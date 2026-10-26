package org.dzoka;

import static org.junit.jupiter.api.Assertions.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import org.dzoka.fingerprints.domain.FingerprintRollout.*;
import org.dzoka.fingerprints.ports.FingerprintRollouts;
import org.dzoka.foundation.application.Failures;
import org.dzoka.foundation.persistence.TenantTransactions;
import org.junit.jupiter.api.Test;

class FingerprintRolloutsIT extends AiFixture {
    private FingerprintRollouts rollouts(){return app.getBean(FingerprintRollouts.class);}
    private Plan plan(UUID report,UUID space){return new Plan(UUID.randomUUID(),List.of(new Source(report,1)),List.of(space),10000,"a".repeat(64));}
    private UUID complete(UUID report,UUID space){
        jobs().enqueue(who("reporter"),A,report,space);var input=jobs().claim(who("inference"),A,space).orElseThrow();
        assertTrue(jobs().complete(who("inference"),A,input.jobId(),result(input)));return jobs().current(who("reporter"),A,report).stream()
            .filter(f->f.space().id().equals(space)).findFirst().map(f->f.id()).orElse(null);
    }
    @Test void immutablePlanSealMeasuredCoverageAndAtomicPromotionRollback() throws Exception{
        UUID report=report("Synthetic migration source").reportId();complete(report,primary);
        Plan old=plan(report,primary);var created=rollouts().create(who("curator"),A,old);
        assertEquals(created,rollouts().create(who("curator"),A,old));assertEquals("DRAFT",created.state());
        assertThrows(Failures.Conflict.class,()->rollouts().create(who("curator"),A,new Plan(old.id(),old.sources(),old.spaces(),0,old.evaluationSha256())));
        assertEquals(10000,rollouts().seal(who("curator"),A,old.id()).coverageBps());
        assertEquals(rollouts().seal(who("curator"),A,old.id()),rollouts().read(who("curator"),A,old.id()));
        assertEquals(1,rollouts().activate(who("curator"),A,old.id(),0,false).version());
        assertEquals(1,rollouts().activate(who("curator"),A,old.id(),0,false).version());
        complete(report,alternate);assertEquals(1,jobs().current(who("reporter"),A,report).size()); // shadow remains hidden
        assertEquals(primary,jobs().current(who("reporter"),A,report).getFirst().space().id());
        Plan next=plan(report,alternate);rollouts().create(who("curator"),A,next);rollouts().seal(who("curator"),A,next.id());
        assertEquals(2,rollouts().activate(who("curator"),A,next.id(),1,false).version());
        var snapshot=jobs().snapshot(who("reporter"),A,report);assertEquals(next.id(),snapshot.rolloutId());assertEquals(2,snapshot.activationVersion());
        assertEquals(alternate,snapshot.components().getFirst().space().id());
        assertEquals(3,rollouts().activate(who("curator"),A,old.id(),2,true).version());
        assertEquals(primary,jobs().current(who("reporter"),A,report).getFirst().space().id());
        try(var c=admin();var s=c.prepareStatement("SELECT count(*) FROM dzoka.fingerprint_activations WHERE tenant_id=?")){
            s.setObject(1,A);try(var r=s.executeQuery()){r.next();assertEquals(3,r.getInt(1));}
        }
    }
    @Test void incompleteFailedAndUnavailableInputsHaveDifferentCoverage() throws Exception{
        UUID ready=report("Available").reportId(),missing=report(null).reportId();complete(ready,primary);jobs().enqueue(who("reporter"),A,missing,primary);
        var sources=List.of(new Source(ready,1),new Source(missing,1));
        Plan strict=new Plan(UUID.randomUUID(),sources,List.of(primary),10000,"b".repeat(64));rollouts().create(who("curator"),A,strict);
        assertThrows(Failures.Conflict.class,()->rollouts().seal(who("curator"),A,strict.id()));
        Plan measured=new Plan(UUID.randomUUID(),sources,List.of(primary),5000,"b".repeat(64));rollouts().create(who("curator"),A,measured);
        var sealed=rollouts().seal(who("curator"),A,measured.id());assertEquals(2,sealed.expectedSlots());assertEquals(1,sealed.availableSlots());assertEquals(1,sealed.unavailableSlots());assertEquals(5000,sealed.coverageBps());
        UUID pending=report("Pending").reportId();jobs().enqueue(who("reporter"),A,pending,primary);
        Plan notReady=new Plan(UUID.randomUUID(),List.of(new Source(pending,1)),List.of(primary),0,"c".repeat(64));rollouts().create(who("curator"),A,notReady);
        assertThrows(Failures.Conflict.class,()->rollouts().seal(who("curator"),A,notReady.id()));
        for(int i=0;i<3;i++){var lease=jobs().claim(who("inference"),A,primary).orElseThrow();jobs().fail(who("inference"),A,lease.jobId(),lease.lease(),"PROVIDER_UNAVAILABLE");}
        assertThrows(Failures.Conflict.class,()->rollouts().seal(who("curator"),A,notReady.id()));
    }
    @Test void sourceRevisionErasureDisabledSpaceAndNoncohortReportsNeverFallThrough() throws Exception{
        UUID report=report("Versioned").reportId();complete(report,primary);Plan old=plan(report,primary);
        rollouts().create(who("curator"),A,old);rollouts().seal(who("curator"),A,old.id());rollouts().activate(who("curator"),A,old.id(),0,false);
        UUID outside=report("Outside cohort").reportId();complete(outside,primary);assertTrue(jobs().current(who("reporter"),A,outside).isEmpty());
        assertEquals(org.dzoka.fingerprints.domain.ItemFingerprint.EvidenceState.UNAVAILABLE,jobs().snapshot(who("reporter"),A,outside).coverage().get("text"));
        try(var c=admin();var s=c.prepareStatement("UPDATE dzoka.embedding_spaces SET enabled=false WHERE tenant_id=? AND id=?")){
            s.setObject(1,A);s.setObject(2,primary);s.executeUpdate();
        }
        try{assertTrue(jobs().current(who("reporter"),A,report).isEmpty());assertThrows(Failures.Conflict.class,()->rollouts().seal(who("curator"),A,old.id()));}
        finally{try(var c=admin();var s=c.prepareStatement("UPDATE dzoka.embedding_spaces SET enabled=true WHERE tenant_id=? AND id=?")){s.setObject(1,A);s.setObject(2,primary);s.executeUpdate();}}
        revise(report,1,"Revised source");assertTrue(jobs().current(who("reporter"),A,report).isEmpty());
        assertThrows(Failures.Conflict.class,()->rollouts().seal(who("curator"),A,old.id()));
        try(var c=admin();var s=c.prepareStatement("UPDATE dzoka.reports SET expires_at=now()-interval '1 second' WHERE tenant_id=? AND id=?")){s.setObject(1,A);s.setObject(2,report);s.executeUpdate();}
        assertEquals(1,jobs().purge(who("purger"),A));assertThrows(Failures.Conflict.class,()->rollouts().activate(who("curator"),A,old.id(),1,true));
        assertEquals(0,rollouts().read(who("curator"),A,old.id()).availableSlots());
    }
    @Test void permissionsHttpProjectionAndDatabaseImmutabilityAreEnforced() throws Exception{
        UUID report=report("Private cohort").reportId();complete(report,primary);Plan plan=plan(report,primary);
        assertThrows(Failures.Forbidden.class,()->rollouts().create(who("inference"),A,plan));
        assertThrows(Failures.Conflict.class,()->rollouts().create(who("curator"),B,plan));
        var mapper=new tools.jackson.databind.ObjectMapper();String body=mapper.writeValueAsString(plan);
        assertEquals(0,rollouts().active(who("curator"),A).version());
        assertEquals(403,api("POST",A,"/internal/fingerprints/rollouts","reporter",body).statusCode());
        assertEquals(400,api("POST",A,"/internal/fingerprints/rollouts","curator",body.replace("\"minimumCoverageBps\":10000,","")).statusCode());
        var response=api("POST",A,"/internal/fingerprints/rollouts","curator",body);assertEquals(200,response.statusCode());
        assertFalse(response.body().contains("Private cohort"));assertFalse(response.body().contains("vector"));
        assertEquals(200,api("POST",A,"/internal/fingerprints/rollouts/"+plan.id()+"/seal","curator",null).statusCode());
        assertEquals(400,api("POST",A,"/internal/fingerprints/rollouts/"+plan.id()+"/activate","curator","{\"rollback\":false}").statusCode());
        assertEquals(200,api("POST",A,"/internal/fingerprints/rollouts/"+plan.id()+"/activate","curator","{\"expectedVersion\":0,\"rollback\":false}").statusCode());
        var pointer=api("GET",A,"/internal/fingerprints/rollouts/active","curator",null);assertEquals(200,pointer.statusCode());
        assertTrue(pointer.body().contains(plan.id().toString()));assertTrue(pointer.body().contains("\"version\":1"));
        var tx=app.getBean(TenantTransactions.class);
        assertThrows(Failures.Unavailable.class,()->tx.execute(who("curator"),A,"fingerprints:manage",c->{
            try(var s=c.createStatement()){s.executeUpdate("UPDATE dzoka.embedding_spaces SET enabled=false");}return null;
        }));
        assertThrows(Failures.Unavailable.class,()->tx.execute(who("curator"),A,"fingerprints:manage",c->{
            try(var s=c.createStatement()){s.executeUpdate("UPDATE dzoka.fingerprint_rollouts SET evaluation_sha256='"+"f".repeat(64)+"'");}return null;
        }));
        assertThrows(Failures.Unavailable.class,()->tx.execute(who("curator"),A,"fingerprints:manage",c->{
            try(var s=c.createStatement()){s.executeUpdate("DELETE FROM dzoka.fingerprint_snapshot_slots");}return null;
        }));
        int visible=tx.execute(who("other"),A,"reports:read",c->{try(var s=c.createStatement();var r=s.executeQuery("SELECT count(*) FROM dzoka.fingerprint_snapshot_slots")){r.next();return r.getInt(1);}});
        assertEquals(0,visible);
        assertThrows(Failures.Forbidden.class,()->rollouts().read(who("reporter"),A,plan.id()));
        assertEquals(404,api("GET",B,"/internal/fingerprints/rollouts/"+plan.id(),"curator",null).statusCode());
    }
    @Test void compareAndSetActivationRacesHaveOneWinnerAndUnknownRollbackIsRejected() throws Exception{
        UUID report=report("Concurrent activation").reportId();complete(report,primary);complete(report,alternate);
        Plan one=plan(report,primary),two=plan(report,alternate);
        for(var plan:List.of(one,two)){rollouts().create(who("curator"),A,plan);rollouts().seal(who("curator"),A,plan.id());}
        assertThrows(Failures.Conflict.class,()->rollouts().activate(who("curator"),A,one.id(),0,true));
        try(var pool=Executors.newFixedThreadPool(2)){
            var results=new ArrayList<Future<Boolean>>();
            for(var plan:List.of(one,two))results.add(pool.submit(()->{try{rollouts().activate(who("curator"),A,plan.id(),0,false);return true;}catch(Failures.Conflict expected){return false;}}));
            int successes=0;for(var future:results)if(future.get(20,TimeUnit.SECONDS))successes++;assertEquals(1,successes);
        }
        assertEquals(1,jobs().snapshot(who("reporter"),A,report).activationVersion());
    }
    @Test void activationAndAuditOutboxRollbackTogether() throws Exception{
        UUID report=report("Atomic activation").reportId();complete(report,primary);Plan plan=plan(report,primary);
        rollouts().create(who("curator"),A,plan);rollouts().seal(who("curator"),A,plan.id());
        try(var c=admin();var s=c.createStatement()){
            s.execute("CREATE OR REPLACE FUNCTION dzoka.test_fail_rollout() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF NEW.event_type='fingerprint.rollout.promoted' THEN RAISE EXCEPTION 'injected'; END IF; RETURN NEW; END $$");
            s.execute("CREATE TRIGGER fail_rollout BEFORE INSERT ON dzoka.outbox FOR EACH ROW EXECUTE FUNCTION dzoka.test_fail_rollout()");
        }
        try{assertThrows(Failures.Unavailable.class,()->rollouts().activate(who("curator"),A,plan.id(),0,false));}
        finally{try(var c=admin();var s=c.createStatement()){s.execute("DROP TRIGGER fail_rollout ON dzoka.outbox");}}
        assertNull(jobs().snapshot(who("reporter"),A,report).rolloutId());
        assertEquals(1,rollouts().activate(who("curator"),A,plan.id(),0,false).version());
    }
    @Test void concurrentDisjointCohortSwitchesNeverReturnTornPointerAndComponents() throws Exception{
        UUID a=report("Cohort A").reportId(),b=report("Cohort B").reportId();complete(a,primary);complete(b,primary);
        Plan one=plan(a,primary),two=plan(b,primary);
        for(var plan:List.of(one,two)){rollouts().create(who("curator"),A,plan);rollouts().seal(who("curator"),A,plan.id());}
        rollouts().activate(who("curator"),A,one.id(),0,false);
        try(var pool=Executors.newFixedThreadPool(2)){
            var writer=pool.submit(()->{for(int i=1;i<=12;i++)rollouts().activate(who("curator"),A,(i%2==1?two:one).id(),i,false);});
            var reader=pool.submit(()->{for(int i=0;i<24;i++){
                var snapshot=jobs().snapshot(who("reporter"),A,a);
                assertEquals(snapshot.rolloutId().equals(one.id())?1:0,snapshot.components().size());
            }});
            writer.get(30,TimeUnit.SECONDS);reader.get(30,TimeUnit.SECONDS);
        }
        // A previously active cohort on an obsolete revision cannot be rolled back.
        rollouts().activate(who("curator"),A,two.id(),13,false);revise(a,1,"New cohort A source");
        assertThrows(Failures.Conflict.class,()->rollouts().activate(who("curator"),A,one.id(),14,true));
    }
}
