package org.dzoka;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.dzoka.fingerprints.domain.Perception.*;
import org.dzoka.fingerprints.ports.PerceptionJobs;
import org.dzoka.foundation.application.Failures;
import org.dzoka.foundation.persistence.TenantTransactions;
import org.dzoka.intake.application.IntakeService;
import org.junit.jupiter.api.Test;

class PerceptionJobsIT extends AiFixture {
    @Override protected void initializeProviders()throws Exception{super.initializeProviders();try(var c=admin()){PerceptionFixtures.profiles(c,A);PerceptionFixtures.profiles(c,B);}}
    private PerceptionJobs perception(){return app.getBean(PerceptionJobs.class);}
    private Job enqueue(UUID report,int variant){return perception().enqueue(who("reporter"),A,report,PerceptionFixtures.id("SPEECH"),null,PerceptionFixtures.audio(variant));}
    private Lease claim(){return perception().claim(who("inference"),A,PerceptionFixtures.id("SPEECH")).orElseThrow();}
    private Result result(Lease lease){return new Result(lease.lease(),lease.inputSha256(),lease.profileKey(),Instant.now(),Map.of("text","Synthetic transcript","samples",8000,"sampleRate",16000,"language","en","representation","wav2vec2-ctc-greedy-v1"));}
    private long scalar(String query)throws Exception{try(var c=admin();var s=c.prepareStatement(query)){s.setObject(1,A);try(var r=s.executeQuery()){r.next();return r.getLong(1);}}}
    @Test void publicPcmIntakeChecksHttpScopeAndBoundsIncludingChunkedBodies()throws Exception{
        UUID report=report("Private audio HTTP").reportId();String path=base+"/api/v1/tenants/"+A+"/reports/"+report+"/perception/audio/"+PerceptionFixtures.id("SPEECH");
        for(String actor:List.of("reporter","other","inference")){
            var response=http.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(path)).header("Authorization","Bearer "+token(actor)).header("Content-Type","audio/wav")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofByteArray(PerceptionFixtures.audio(20))).build(),java.net.http.HttpResponse.BodyHandlers.ofString());
            assertEquals(actor.equals("reporter")?200:actor.equals("other")?404:403,response.statusCode());
            assertFalse(response.body().contains("Private audio HTTP"));assertFalse(response.body().contains("raw_wav"));
        }
        byte[] oversized=new byte[320045];
        var response=http.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(path)).header("Authorization","Bearer "+token("reporter")).header("Content-Type","audio/wav")
            .POST(java.net.http.HttpRequest.BodyPublishers.ofInputStream(()->new java.io.ByteArrayInputStream(oversized))).build(),java.net.http.HttpResponse.BodyHandlers.ofString());
        assertEquals(413,response.statusCode());
        assertEquals(1,scalar("SELECT count(*) FROM dzoka.perception_jobs WHERE tenant_id=?"));
    }
    @Test void naturalConcurrentIdempotencyPrivateLeasesAndRawInputAreEnforced()throws Exception{
        UUID report=report("Audio lease").reportId();var ids=new HashSet<UUID>();
        try(var pool=Executors.newFixedThreadPool(3)){var futures=new ArrayList<Future<Job>>();for(int i=0;i<3;i++)futures.add(pool.submit(()->enqueue(report,1)));for(var f:futures)ids.add(f.get(20,TimeUnit.SECONDS).id());}
        assertEquals(1,ids.size());var queued=enqueue(report,1);assertEquals(1,scalar("SELECT raw_bytes/16044 FROM dzoka.perception_audio_usage WHERE tenant_id=?"));
        assertThrows(Failures.NotFound.class,()->perception().read(who("other"),A,queued.id()));
        assertThrows(Failures.Forbidden.class,()->perception().claim(who("reporter"),A,PerceptionFixtures.id("SPEECH")));
        var lease=claim();assertArrayEquals(PerceptionFixtures.audio(1),perception().input(who("inference"),A,queued.id(),lease.lease()));
        assertThrows(Failures.NotFound.class,()->perception().input(who("wrong-worker"),A,queued.id(),lease.lease()));
        assertThrows(Failures.NotFound.class,()->perception().input(who("remote"),B,queued.id(),lease.lease()));
        assertEquals(403,api("POST",A,"/internal/perception/jobs/claim","reporter","{\"profileId\":\""+PerceptionFixtures.id("SPEECH")+"\"}").statusCode());
        var tx=app.getBean(TenantTransactions.class);
        int unleased=tx.execute(who("wrong-worker"),A,"ai:work",c->{try(var s=c.createStatement();var r=s.executeQuery("SELECT count(*) FROM dzoka.perception_audio")){r.next();return r.getInt(1);}});assertEquals(0,unleased);
    }
    @Test void resultsAreImmutableExactRetrySafeAndStaleInputsCannotPublish()throws Exception{
        UUID report=report("Versioned audio").reportId();var job=enqueue(report,2);var lease=claim();var result=result(lease);
        var bad=new HashMap<>(result.evidence());bad.put("samples",7000);
        assertThrows(IllegalArgumentException.class,()->perception().complete(who("inference"),A,job.id(),new Result(lease.lease(),lease.inputSha256(),lease.profileKey(),result.generatedAt(),bad)));
        assertFalse(perception().complete(who("wrong-worker"),A,job.id(),result));
        assertTrue(perception().complete(who("inference"),A,job.id(),result));assertTrue(perception().complete(who("inference"),A,job.id(),result));
        assertEquals("SHADOW",perception().current(who("reporter"),A,report).getFirst().selection());
        assertEquals(1,scalar("SELECT count(*) FROM dzoka.perception_components WHERE tenant_id=?"));
        revise(report,1,"New report revision");assertTrue(perception().current(who("reporter"),A,report).isEmpty());assertFalse(perception().complete(who("inference"),A,job.id(),result));
        var next=enqueue(report,2);assertNotEquals(job.id(),next.id());var second=claim();revise(report,2,"Third revision");
        assertThrows(Failures.NotFound.class,()->perception().input(who("inference"),A,next.id(),second.lease()));assertFalse(perception().complete(who("inference"),A,next.id(),result(second)));
    }
    @Test void retryBudgetDeadlineAndSharedTenantBackpressureAreBounded()throws Exception{
        for(int i=0;i<31;i++)jobs().enqueue(who("reporter"),A,report("Text queue "+i).reportId(),primary);
        UUID report=report("Shared quota").reportId();var job=enqueue(report,3);assertThrows(Failures.Conflict.class,()->enqueue(report,4));
        for(int i=0;i<3;i++){var lease=claim();assertTrue(perception().fail(who("inference"),A,job.id(),lease.lease(),"PROVIDER_UNAVAILABLE"));}
        assertEquals("FAILED",perception().read(who("reporter"),A,job.id()).state());assertEquals(31,scalar("SELECT pending FROM dzoka.ai_usage WHERE tenant_id=?"));
        var next=enqueue(report,4);try(var c=admin();var s=c.prepareStatement("UPDATE dzoka.perception_jobs SET deadline=now()-interval '1 second' WHERE tenant_id=? AND id=?")){s.setObject(1,A);s.setObject(2,next.id());s.executeUpdate();}
        assertTrue(perception().claim(who("inference"),A,PerceptionFixtures.id("SPEECH")).isEmpty());assertEquals(31,scalar("SELECT pending FROM dzoka.ai_usage WHERE tenant_id=?"));
    }
    @Test void holdAwareErasureRemovesRawAudioAndComponentsAndReleasesStorageOnce()throws Exception{
        UUID report=report("Governed audio").reportId();var job=enqueue(report,5);var lease=claim();var result=result(lease);assertTrue(perception().complete(who("inference"),A,job.id(),result));
        var intake=app.getBean(IntakeService.class);intake.hold(who("reviewer"),A,report,true);
        try(var c=admin();var s=c.prepareStatement("UPDATE dzoka.reports SET expires_at=now()-interval '1 second' WHERE tenant_id=? AND id=?")){s.setObject(1,A);s.setObject(2,report);s.executeUpdate();}
        assertEquals(0,perception().purge(who("purger"),A));assertEquals(16044,scalar("SELECT raw_bytes FROM dzoka.perception_audio_usage WHERE tenant_id=?"));
        intake.hold(who("reviewer"),A,report,false);assertEquals(1,perception().purge(who("purger"),A));assertEquals(0,perception().purge(who("purger"),A));
        assertEquals(0,scalar("SELECT raw_bytes FROM dzoka.perception_audio_usage WHERE tenant_id=?"));assertEquals(0,scalar("SELECT count(*) FROM dzoka.perception_audio WHERE tenant_id=?"));
        assertFalse(perception().complete(who("inference"),A,job.id(),result));
    }
    @Test void rawAndResultWritesRollbackWithAuditOutbox()throws Exception{
        UUID report=report("Atomic audio").reportId();
        try(var c=admin();var s=c.createStatement()){
            s.execute("CREATE OR REPLACE FUNCTION dzoka.test_fail_perception() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF NEW.event_type LIKE 'perception.%' THEN RAISE EXCEPTION 'injected'; END IF; RETURN NEW; END $$");
            s.execute("CREATE TRIGGER fail_perception BEFORE INSERT ON dzoka.outbox FOR EACH ROW EXECUTE FUNCTION dzoka.test_fail_perception()");
        }
        try{assertThrows(Failures.Unavailable.class,()->enqueue(report,6));}
        finally{try(var c=admin();var s=c.createStatement()){s.execute("DROP TRIGGER fail_perception ON dzoka.outbox");}}
        assertEquals(0,scalar("SELECT count(*) FROM dzoka.perception_audio WHERE tenant_id=?"));assertEquals(0,scalar("SELECT count(*) FROM dzoka.perception_audio_usage WHERE tenant_id=?"));
        var job=enqueue(report,6);var lease=claim();
        try(var c=admin();var s=c.createStatement()){s.execute("CREATE TRIGGER fail_perception BEFORE INSERT ON dzoka.outbox FOR EACH ROW EXECUTE FUNCTION dzoka.test_fail_perception()");}
        try{assertThrows(Failures.Unavailable.class,()->perception().complete(who("inference"),A,job.id(),result(lease)));}
        finally{try(var c=admin();var s=c.createStatement()){s.execute("DROP TRIGGER fail_perception ON dzoka.outbox");}}
        assertEquals("PROCESSING",perception().read(who("reporter"),A,job.id()).state());assertEquals(0,scalar("SELECT count(*) FROM dzoka.perception_components WHERE tenant_id=?"));
        assertTrue(perception().complete(who("inference"),A,job.id(),result(lease)));
    }
    @Test void profilesAndInputTypesFailClosedAndLeaseExpiryRevokesRawReads()throws Exception{
        UUID report=report("Strict input").reportId();
        assertThrows(IllegalArgumentException.class,()->perception().enqueue(who("reporter"),A,report,PerceptionFixtures.id("VISION"),null,PerceptionFixtures.audio(7)));
        byte[] malformed=PerceptionFixtures.audio(7);malformed[24]=0;
        assertThrows(IllegalArgumentException.class,()->perception().enqueue(who("reporter"),A,report,PerceptionFixtures.id("SPEECH"),null,malformed));
        var job=enqueue(report,7);var lease=claim();
        try(var c=admin();var s=c.prepareStatement("UPDATE dzoka.perception_jobs SET lease_until=now()-interval '1 second' WHERE tenant_id=? AND id=?")){s.setObject(1,A);s.setObject(2,job.id());s.executeUpdate();}
        assertThrows(Failures.NotFound.class,()->perception().input(who("inference"),A,job.id(),lease.lease()));assertFalse(perception().complete(who("inference"),A,job.id(),result(lease)));
        var replacement=claim();assertNotEquals(lease.lease(),replacement.lease());
        assertThrows(Failures.Unavailable.class,()->app.getBean(TenantTransactions.class).execute(who("inference"),A,"ai:work",c->{try(var s=c.createStatement()){s.executeUpdate("UPDATE dzoka.perception_profiles SET enabled=false");}return null;}));
        String original;
        try(var c=admin();var s=c.prepareStatement("SELECT manifest::text FROM dzoka.perception_profiles WHERE tenant_id=? AND id=?")){
            s.setObject(1,A);s.setObject(2,PerceptionFixtures.id("SPEECH"));try(var r=s.executeQuery()){r.next();original=r.getString(1);}
        }
        try{
            try(var c=admin();var s=c.prepareStatement("UPDATE dzoka.perception_profiles SET manifest=manifest||'{\"unapproved\":true}'::jsonb WHERE tenant_id=? AND id=?")){s.setObject(1,A);s.setObject(2,PerceptionFixtures.id("SPEECH"));s.executeUpdate();}
            assertThrows(Failures.Unavailable.class,()->enqueue(report,9));
        }finally{try(var c=admin();var s=c.prepareStatement("UPDATE dzoka.perception_profiles SET manifest=CAST(? AS jsonb) WHERE tenant_id=? AND id=?")){s.setString(1,original);s.setObject(2,A);s.setObject(3,PerceptionFixtures.id("SPEECH"));s.executeUpdate();}}
    }
    @Test void explicitPerceptionPinsPromoteAtomicallyAndCannotSurviveSourceRevisionOrErasure()throws Exception{
        UUID report=report("Pinned audio").reportId();var job=enqueue(report,8);var lease=claim();assertTrue(perception().complete(who("inference"),A,job.id(),result(lease)));
        var component=perception().current(who("reporter"),A,report).getFirst();var rollouts=app.getBean(org.dzoka.fingerprints.ports.FingerprintRollouts.class);
        var plan=new org.dzoka.fingerprints.domain.FingerprintRollout.Plan(UUID.randomUUID(),List.of(new org.dzoka.fingerprints.domain.FingerprintRollout.Source(report,1)),List.of(),10000,"f".repeat(64),List.of(new org.dzoka.fingerprints.domain.FingerprintRollout.PerceptionPin(report,component.id())));
        rollouts.create(who("curator"),A,plan);assertEquals(1,rollouts.seal(who("curator"),A,plan.id()).expectedSlots());rollouts.activate(who("curator"),A,plan.id(),0,false);
        assertEquals(1,jobs().snapshot(who("reporter"),A,report).perceptionComponents().size());
        assertEquals("ACTIVE",perception().current(who("reporter"),A,report).getFirst().selection());
        revise(report,1,"Revised");assertTrue(jobs().snapshot(who("reporter"),A,report).perceptionComponents().isEmpty());assertThrows(Failures.Conflict.class,()->rollouts.seal(who("curator"),A,plan.id()));
        try(var c=admin();var s=c.prepareStatement("UPDATE dzoka.reports SET expires_at=now()-interval '1 second' WHERE tenant_id=? AND id=?")){s.setObject(1,A);s.setObject(2,report);s.executeUpdate();}
        assertEquals(1,perception().purge(who("purger"),A));assertEquals(0,rollouts.read(who("curator"),A,plan.id()).availableSlots());
    }
}
