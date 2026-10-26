package org.dzoka;

import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.dzoka.fingerprints.domain.*;
import org.dzoka.fingerprints.ports.FingerprintJobs;
import org.dzoka.foundation.application.*;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.intake.application.IntakeService;
import org.dzoka.intake.domain.*;
import org.junit.jupiter.api.*;
import tools.jackson.databind.ObjectMapper;

class AiJobsIT extends AiFixture {
    @Test void naturalIdempotencyAndConcurrentRequestsCreateOneJob() throws Exception {
        UUID report=report("Synthetic concurrent bag").reportId();
        try(var pool=Executors.newFixedThreadPool(3)){
            var tasks=new ArrayList<Future<InferenceJob>>();for(int i=0;i<3;i++)tasks.add(pool.submit(()->jobs().enqueue(who("reporter"),A,report,primary)));
            var ids=new HashSet<UUID>();for(var f:tasks)ids.add(f.get(20,TimeUnit.SECONDS).id());assertEquals(1,ids.size());
        }
        var first=jobs().enqueue(who("reporter"),A,report,primary);assertEquals("QUEUED",first.state());
        var snapshot=api("GET",A,"/reports/"+report+"/fingerprints","reporter",null);assertEquals(200,snapshot.statusCode());
        assertTrue(snapshot.body().contains("\"schemaVersion\":1"));assertTrue(snapshot.body().contains("\"image\":\"UNAVAILABLE\""));
        assertTrue(snapshot.body().contains("\"text\":\"PENDING\""));
        assertThrows(Failures.NotFound.class,()->jobs().read(who("other"),A,first.id()));
        assertEquals(403,api("POST",A,"/internal/ai/jobs/claim","reporter","{\"spaceId\":\""+primary+"\"}").statusCode());
        assertEquals(403,api("GET",A,"/reports/"+report,"inference",null).statusCode());
        assertEquals(403,api("POST",A,"/organizations","inference","{\"name\":\"Synthetic forbidden mutation\"}").statusCode());
        assertEquals(400,api("POST",A,"/internal/ai/jobs/"+first.id()+"/fail","inference","{\"lease\":\""+UUID.randomUUID()+"\",\"reason\":null}").statusCode());
    }
    @Test void inputRevocationExpiredLeasesWrongWorkerAndCrossTenantCannotActivate() throws Exception {
        UUID report=report("Old input").reportId();var queued=jobs().enqueue(who("reporter"),A,report,primary);var first=jobs().claim(who("inference"),A,primary).orElseThrow();
        assertFalse(jobs().complete(who("wrong-worker"),A,queued.id(),result(first)));
        assertFalse(jobs().complete(who("remote"),B,queued.id(),result(first)));
        try(var c=admin();var s=c.prepareStatement("UPDATE dzoka.ai_jobs SET lease_until=now()-interval '1 second' WHERE tenant_id=? AND id=?")){
            s.setObject(1,A);s.setObject(2,queued.id());s.executeUpdate();
        }
        assertFalse(jobs().complete(who("inference"),A,queued.id(),result(first)));
        var second=jobs().claim(who("inference"),A,primary).orElseThrow();assertNotEquals(first.lease(),second.lease());
        revise(report,1,"New input");assertFalse(jobs().complete(who("inference"),A,queued.id(),result(second)));
        assertTrue(jobs().current(who("reporter"),A,report).isEmpty());assertTrue(jobs().claim(who("inference"),A,primary).isEmpty());
        assertEquals("OBSOLETE",jobs().read(who("reporter"),A,queued.id()).state());
    }
    @Test void completionIsImmutableIdempotentAndOldRevisionsStaySeparate() throws Exception {
        UUID report=report("Version one").reportId();var job=jobs().enqueue(who("reporter"),A,report,primary);var input=jobs().claim(who("inference"),A,primary).orElseThrow();var result=result(input);
        assertTrue(jobs().complete(who("inference"),A,job.id(),result));assertTrue(jobs().complete(who("inference"),A,job.id(),result));
        var current=jobs().current(who("reporter"),A,report);assertEquals(1,current.size());assertEquals(profile.spaceKey(),current.getFirst().space().spaceKey());
        assertFalse(jobs().complete(who("inference"),A,job.id(),new EmbeddingResult(input.lease(),input.inputSha256(),profile.spaceKey(),profile.provider(),"text-embedding-v1",Instant.now(),result.vector(),5,false)));
        revise(report,1,"Version two");assertTrue(jobs().current(who("reporter"),A,report).isEmpty());
        var next=jobs().enqueue(who("reporter"),A,report,primary);assertNotEquals(job.id(),next.id());assertEquals(2,next.sourceRevision());
        try(var c=admin();var s=c.prepareStatement("SELECT count(*) FROM dzoka.fingerprint_components WHERE tenant_id=? AND report_id=?")){
            s.setObject(1,A);s.setObject(2,report);try(var r=s.executeQuery()){r.next();assertEquals(1,r.getInt(1));}
        }
    }
    @Test void invalidProvenanceUnavailableInputAndRetryBudgetAreExplicit() throws Exception {
        assertEquals("UNAVAILABLE",jobs().enqueue(who("reporter"),A,report(null).reportId(),primary).state());
        UUID report=report("Available input").reportId();assertThrows(Failures.Unavailable.class,()->jobs().enqueue(who("reporter"),A,report,UUID.randomUUID()));
        var job=jobs().enqueue(who("reporter"),A,report,primary);var input=jobs().claim(who("inference"),A,primary).orElseThrow();var good=result(input);
        assertThrows(IllegalArgumentException.class,()->jobs().complete(who("inference"),A,job.id(),new EmbeddingResult(input.lease(),input.inputSha256(),"f".repeat(64),profile.provider(),"text-embedding-v1",Instant.now(),good.vector(),5,false)));
        assertTrue(jobs().fail(who("inference"),A,job.id(),input.lease(),"PROVIDER_UNAVAILABLE"));
        for(int i=0;i<2;i++){var lease=jobs().claim(who("inference"),A,primary).orElseThrow();assertTrue(jobs().fail(who("inference"),A,job.id(),lease.lease(),"PROVIDER_UNAVAILABLE"));}
        assertTrue(jobs().claim(who("inference"),A,primary).isEmpty());assertEquals("FAILED",jobs().read(who("reporter"),A,job.id()).state());
        assertTrue(jobs().current(who("reporter"),A,report).isEmpty());
    }
    @Test void deletionAndHoldControlErasureAndLateCompletion() throws Exception {
        UUID report=report("Retention input").reportId();var job=jobs().enqueue(who("reporter"),A,report,primary);var input=jobs().claim(who("inference"),A,primary).orElseThrow();
        assertTrue(jobs().complete(who("inference"),A,job.id(),result(input)));var intake=app.getBean(IntakeService.class);intake.hold(who("reviewer"),A,report,true);
        try(var c=admin();var s=c.prepareStatement("UPDATE dzoka.reports SET expires_at=now()-interval '1 second' WHERE tenant_id=? AND id=?")){s.setObject(1,A);s.setObject(2,report);s.executeUpdate();}
        assertEquals(0,jobs().purge(who("purger"),A));intake.hold(who("reviewer"),A,report,false);
        assertEquals(1,jobs().purge(who("purger"),A));assertEquals(0,jobs().purge(who("purger"),A));
        assertFalse(jobs().complete(who("inference"),A,job.id(),result(input)));
        assertThrows(Failures.NotFound.class,()->jobs().current(who("reporter"),A,report));
    }
    @Test void jobAndAuditOutboxCreationAreOneTransaction() throws Exception {
        UUID report=report("Atomic job fixture").reportId();
        try(var c=admin();var s=c.createStatement()){
            s.execute("CREATE OR REPLACE FUNCTION dzoka.test_fail_ai() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF NEW.event_type='fingerprint.requested' THEN RAISE EXCEPTION 'injected'; END IF; RETURN NEW; END $$");
            s.execute("CREATE TRIGGER fail_ai BEFORE INSERT ON dzoka.outbox FOR EACH ROW EXECUTE FUNCTION dzoka.test_fail_ai()");
        }
        try{assertThrows(Failures.Unavailable.class,()->jobs().enqueue(who("reporter"),A,report,primary));}
        finally{try(var c=admin();var s=c.createStatement()){s.execute("DROP TRIGGER fail_ai ON dzoka.outbox");}}
        try(var c=admin();var s=c.prepareStatement("SELECT count(*) FROM dzoka.ai_jobs WHERE tenant_id=? AND report_id=?")){
            s.setObject(1,A);s.setObject(2,report);try(var r=s.executeQuery()){r.next();assertEquals(0,r.getInt(1));}
        }
    }
    @Test void concurrentRequestsRespectTenantBackpressureAndDeadline() throws Exception {
        for(int i=0;i<31;i++)jobs().enqueue(who("reporter"),A,report("Synthetic queued item "+i).reportId(),primary);
        UUID one=report("Boundary one").reportId(),two=app.getBean(IntakeService.class).submit(who("other"),A,null,null,
            new ReportDraft(ReportDraft.Kind.LOST,"Boundary other actor","en","UTC",null,null,null,null,null,null,null),"{}".getBytes(),UUID.randomUUID().toString()).reportId();
        try(var pool=Executors.newFixedThreadPool(2)){
            var calls=new ArrayList<Future<Boolean>>();
            for(UUID id:List.of(one,two))calls.add(pool.submit(()->{try{jobs().enqueue(who(id.equals(one)?"reporter":"other"),A,id,primary);return true;}catch(Failures.Conflict expected){return false;}}));
            int successes=0;for(var c:calls)if(c.get(20,TimeUnit.SECONDS))successes++;assertEquals(1,successes);
        }
        try(var c=admin();var s=c.prepareStatement("UPDATE dzoka.ai_jobs SET deadline=now()-interval '1 second' WHERE tenant_id=?")){s.setObject(1,A);s.executeUpdate();}
        assertTrue(jobs().claim(who("inference"),A,primary).isEmpty());
        try(var c=admin();var s=c.prepareStatement("SELECT pending FROM dzoka.ai_usage WHERE tenant_id=?")){
            s.setObject(1,A);try(var r=s.executeQuery()){r.next();assertEquals(0,r.getInt(1));}
        }
    }
}
