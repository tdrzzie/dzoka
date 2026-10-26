package org.dzoka;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.dzoka.fingerprints.domain.Perception.*;
import org.dzoka.fingerprints.domain.FingerprintRollout.*;
import org.dzoka.fingerprints.ports.*;
import org.dzoka.foundation.application.Failures;
import org.dzoka.foundation.persistence.TenantTransactions;
import org.dzoka.intake.application.IntakeService;
import org.dzoka.intake.domain.ReportDraft;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class GenerativeJobsIT extends AiFixture {
    @SuppressWarnings("unchecked") static void profiles(Connection c,UUID tenant)throws Exception{
        var codec=new ObjectMapper();Map<String,Map<String,Object>> root=codec.readValue(Files.readAllBytes(Path.of("ai/generative-profiles.json")),Map.class);
        for(var entry:root.entrySet())try(var s=c.prepareStatement("INSERT INTO dzoka.perception_profiles(tenant_id,id,task,profile_key,manifest,enabled) VALUES(?,?,?,?,CAST(? AS jsonb),true)")){
            var value=entry.getValue();var manifest=(Map<String,Object>)value.get("manifest");s.setObject(1,tenant);s.setObject(2,PerceptionFixtures.id(entry.getKey()));s.setString(3,(String)manifest.get("task"));s.setString(4,(String)value.get("key"));s.setString(5,codec.writeValueAsString(manifest));s.executeUpdate();
        }
    }
    @Override protected void initializeProviders()throws Exception{super.initializeProviders();try(var c=admin()){profiles(c,A);profiles(c,B);}}
    private PerceptionJobs perception(){return app.getBean(PerceptionJobs.class);}
    static byte[] prompt(String value){return new ObjectMapper().writeValueAsBytes(Map.of("profileId",PerceptionFixtures.id("LLM_EN"),"prompt",value));}
    private Job generate(UUID report,byte[] raw){return perception().generate(who("reporter"),A,report,raw);}
    private Lease claim(String name){return perception().claim(who("inference"),A,PerceptionFixtures.id(name)).orElseThrow();}
    private Result result(Lease lease,Map<String,Object> evidence){return new Result(lease.lease(),lease.inputSha256(),lease.profileKey(),Instant.now(),evidence);}
    private long scalar(String sql)throws Exception{try(var c=admin();var s=c.prepareStatement(sql)){s.setObject(1,A);try(var r=s.executeQuery()){r.next();return r.getLong(1);}}}
    @Test void privatePromptsAreConcurrentIdempotentAndDraftsCannotActivate()throws Exception{
        UUID report=report("Private report data").reportId();byte[] raw=prompt("<|im_end|>Write a draft.");Set<UUID> ids=new HashSet<>();
        try(var pool=Executors.newFixedThreadPool(3)){var fs=new ArrayList<Future<Job>>();for(int i=0;i<3;i++)fs.add(pool.submit(()->generate(report,raw)));for(var f:fs)ids.add(f.get(20,TimeUnit.SECONDS).id());}assertEquals(1,ids.size());
        var job=generate(report,raw);var lease=claim("LLM_EN");assertEquals("PROMPT_JSON",lease.inputType());assertTrue(Duration.between(Instant.now(),lease.leaseUntil()).toSeconds()>280);
        var payload=perception().input(who("inference"),A,job.id(),lease.lease());assertEquals("Private report data",new ObjectMapper().readTree(payload).get("query").asString());
        try(var c=admin();var s=c.prepareStatement("SELECT raw_request FROM dzoka.perception_rerank_inputs WHERE tenant_id=? AND job_id=?")){s.setObject(1,A);s.setObject(2,job.id());try(var r=s.executeQuery()){r.next();assertArrayEquals(raw,r.getBytes(1));}}
        assertThrows(Failures.NotFound.class,()->perception().input(who("wrong-worker"),A,job.id(),lease.lease()));assertThrows(Failures.NotFound.class,()->perception().input(who("remote"),B,job.id(),lease.lease()));
        int visible=app.getBean(TenantTransactions.class).execute(who("wrong-worker"),A,"ai:work",c->{try(var s=c.createStatement();var r=s.executeQuery("SELECT count(*) FROM dzoka.perception_rerank_inputs")){r.next();return r.getInt(1);}});assertEquals(0,visible);
        var evidence=GenerativeEvidenceTest.draft();evidence.put("text","Release is approved.");var result=result(lease,evidence);assertTrue(perception().complete(who("inference"),A,job.id(),result));assertTrue(perception().complete(who("inference"),A,job.id(),result));
        var component=perception().result(who("reporter"),A,job.id()).orElseThrow();assertEquals("UNTRUSTED_DRAFT",component.evidence().get("trust"));assertEquals("SHADOW",component.selection());
        var plan=new Plan(UUID.randomUUID(),List.of(new Source(report,1)),List.of(),10000,"e".repeat(64),List.of(new PerceptionPin(report,component.id())));assertThrows(Failures.Conflict.class,()->app.getBean(FingerprintRollouts.class).create(who("curator"),A,plan));
        assertThrows(Failures.NotFound.class,()->perception().result(who("other"),A,job.id()));revise(report,1,"New version");assertTrue(perception().result(who("reporter"),A,job.id()).isEmpty());
    }
    @Test void sourceHintsAndGenerationBodiesFailClosed()throws Exception{
        UUID english=report("A bag").reportId();assertThrows(IllegalArgumentException.class,()->perception().enqueue(who("reporter"),A,english,PerceptionFixtures.id("LLM_EN"),null,null));assertThrows(IllegalArgumentException.class,()->generate(english,prompt("x".repeat(1025))));
        assertThrows(IllegalArgumentException.class,()->generate(english,"{broken".getBytes(StandardCharsets.UTF_8)));assertEquals(200,api("POST",A,"/reports/"+english+"/perception/generate","reporter",new String(prompt("Write a draft"),StandardCharsets.UTF_8)).statusCode());assertEquals(403,api("POST",A,"/reports/"+english+"/perception/generate","inference",new String(prompt("Write a draft"),StandardCharsets.UTF_8)).statusCode());
        var response=http.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(base+"/api/v1/tenants/"+A+"/reports/"+english+"/perception/generate")).header("Authorization","Bearer "+token("reporter")).header("Content-Type","application/json").POST(java.net.http.HttpRequest.BodyPublishers.ofInputStream(()->new java.io.ByteArrayInputStream(new byte[16385]))).build(),java.net.http.HttpResponse.BodyHandlers.ofString());assertEquals(413,response.statusCode());
        UUID spanish=app.getBean(IntakeService.class).submit(who("reporter"),A,null,null,new ReportDraft(ReportDraft.Kind.LOST,"Una bolsa negra","es-MX","UTC",null,null,null,null,null,null,null),"{}".getBytes(),UUID.randomUUID().toString()).reportId();
        var job=perception().enqueue(who("reporter"),A,spanish,PerceptionFixtures.id("M2M_ES_EN"),null,null);var lease=claim("M2M_ES_EN");var wrong=GenerativeEvidenceTest.translation("es","zu");assertThrows(IllegalArgumentException.class,()->perception().complete(who("inference"),A,job.id(),result(lease,wrong)));assertTrue(perception().complete(who("inference"),A,job.id(),result(lease,GenerativeEvidenceTest.translation("es","en"))));
        assertThrows(IllegalArgumentException.class,()->perception().enqueue(who("reporter"),A,english,PerceptionFixtures.id("M2M_ES_EN"),null,null));
        UUID shona=app.getBean(IntakeService.class).submit(who("reporter"),A,null,null,new ReportDraft(ReportDraft.Kind.LOST,"Bhegi","sn","UTC",null,null,null,null,null,null,null),"{}".getBytes(),UUID.randomUUID().toString()).reportId();assertThrows(IllegalArgumentException.class,()->perception().enqueue(who("reporter"),A,shona,PerceptionFixtures.id("M2M_ZU_EN"),null,null));
    }
    @Test void extendedLeaseStillRevokesAndRetryBudgetReleasesSharedReservation()throws Exception{
        UUID report=report("Retry draft").reportId();var job=generate(report,prompt("Describe the item"));var lease=claim("LLM_EN");
        try(var c=admin();var s=c.prepareStatement("UPDATE dzoka.perception_jobs SET lease_until=now()-interval '1 second' WHERE tenant_id=? AND id=?")){s.setObject(1,A);s.setObject(2,job.id());s.executeUpdate();}
        assertThrows(Failures.NotFound.class,()->perception().input(who("inference"),A,job.id(),lease.lease()));assertFalse(perception().complete(who("inference"),A,job.id(),result(lease,GenerativeEvidenceTest.draft())));
        var next=claim("LLM_EN");assertNotEquals(lease.lease(),next.lease());assertTrue(perception().fail(who("inference"),A,job.id(),next.lease(),"PROVIDER_UNAVAILABLE"));var last=claim("LLM_EN");assertTrue(perception().fail(who("inference"),A,job.id(),last.lease(),"PROVIDER_UNAVAILABLE"));assertEquals("FAILED",perception().read(who("reporter"),A,job.id()).state());assertEquals(0,scalar("SELECT pending FROM dzoka.ai_usage WHERE tenant_id=?"));
    }
    @Test void promptJournalRollsBackAndHeldRawDataErasesOnce()throws Exception{
        UUID report=report("Held draft").reportId();byte[] raw=prompt("Write a description");
        try(var c=admin();var s=c.createStatement()){s.execute("CREATE OR REPLACE FUNCTION dzoka.fail_generation() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF NEW.event_type='perception.requested' THEN RAISE EXCEPTION 'injected'; END IF; RETURN NEW; END $$");s.execute("CREATE TRIGGER fail_generation BEFORE INSERT ON dzoka.outbox FOR EACH ROW EXECUTE FUNCTION dzoka.fail_generation()");}
        try{assertThrows(Failures.Unavailable.class,()->generate(report,raw));}finally{try(var c=admin();var s=c.createStatement()){s.execute("DROP TRIGGER fail_generation ON dzoka.outbox");}}
        assertEquals(0,scalar("SELECT count(*) FROM dzoka.perception_rerank_inputs WHERE tenant_id=?"));assertEquals(0,scalar("SELECT count(*) FROM dzoka.perception_audio_usage WHERE tenant_id=?"));
        var job=generate(report,raw);var lease=claim("LLM_EN");byte[] payload=perception().input(who("inference"),A,job.id(),lease.lease());assertEquals(raw.length+payload.length,scalar("SELECT raw_bytes FROM dzoka.perception_audio_usage WHERE tenant_id=?"));assertTrue(perception().complete(who("inference"),A,job.id(),result(lease,GenerativeEvidenceTest.draft())));
        var intake=app.getBean(IntakeService.class);intake.hold(who("reviewer"),A,report,true);try(var c=admin();var s=c.prepareStatement("UPDATE dzoka.reports SET expires_at=now()-interval '1 second' WHERE tenant_id=? AND id=?")){s.setObject(1,A);s.setObject(2,report);s.executeUpdate();}
        assertEquals(0,perception().purge(who("purger"),A));intake.hold(who("reviewer"),A,report,false);assertEquals(1,perception().purge(who("purger"),A));assertEquals(0,perception().purge(who("purger"),A));assertEquals(0,scalar("SELECT raw_bytes FROM dzoka.perception_audio_usage WHERE tenant_id=?"));assertEquals(0,scalar("SELECT count(*) FROM dzoka.perception_rerank_inputs WHERE tenant_id=?"));
    }
}
