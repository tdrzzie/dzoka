package org.dzoka;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.Instant;
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

class LanguageJobsIT extends AiFixture {
    static void profiles(Connection c,UUID tenant)throws Exception{
        var root=new ObjectMapper().readTree(Files.readString(Path.of("ai/language-profiles.json")));
        for(String task:List.of("ATTRIBUTES","TRANSLATION","RERANK"))try(var s=c.prepareStatement("INSERT INTO dzoka.perception_profiles(tenant_id,id,task,profile_key,manifest,enabled) VALUES(?,?,?,?,CAST(? AS jsonb),true)")){
            s.setObject(1,tenant);s.setObject(2,PerceptionFixtures.id(task));s.setString(3,task);s.setString(4,root.get(task).get("key").asString());s.setString(5,root.get(task).get("manifest").toString());s.executeUpdate();
        }
    }
    @Override protected void initializeProviders()throws Exception{super.initializeProviders();try(var c=admin()){profiles(c,A);profiles(c,B);}}
    private PerceptionJobs perception(){return app.getBean(PerceptionJobs.class);}
    private Lease claim(String task){return perception().claim(who("inference"),A,PerceptionFixtures.id(task)).orElseThrow();}
    private Result result(Lease lease,Map<String,Object> evidence){return new Result(lease.lease(),lease.inputSha256(),lease.profileKey(),Instant.now(),evidence);}
    static byte[] request(UUID candidate,String text){return ("{ \"profileId\":\""+PerceptionFixtures.id("RERANK")+"\", \"candidates\":[{\"id\":\""+candidate+"\",\"text\":\""+text+"\"}] }").getBytes(StandardCharsets.UTF_8);}
    static Map<String,Object> ranked(UUID id){return Map.of("candidates",List.of(Map.of("id",id.toString(),"rawScore",2.5,"inputTokenCount",12,"inputTruncated",false)),"representation","msmarco-raw-logit-v1");}
    private long scalar(String sql)throws Exception{try(var c=admin();var s=c.prepareStatement(sql)){s.setObject(1,A);try(var r=s.executeQuery()){r.next();return r.getLong(1);}}}
    @Test void groundedAttributesArePrivateReplaySafeAndRevisionBound()throws Exception{
        UUID report=report("A black bag").reportId();var jobs=perception();var job=jobs.enqueue(who("reporter"),A,report,PerceptionFixtures.id("ATTRIBUTES"),null,null);var lease=claim("ATTRIBUTES");
        assertEquals("A black bag",new String(jobs.input(who("inference"),A,job.id(),lease.lease()),StandardCharsets.UTF_8));
        assertThrows(Failures.NotFound.class,()->jobs.input(who("wrong-worker"),A,job.id(),lease.lease()));
        var bad=LanguageEvidenceTest.attributes();@SuppressWarnings("unchecked") var fields=(Map<String,Object>)bad.get("fields");fields.put("colour",Map.of("state","AVAILABLE","quote","white","start",2,"end",7,"outputTruncated",false));
        assertThrows(IllegalArgumentException.class,()->jobs.complete(who("inference"),A,job.id(),result(lease,bad)));
        fields.put("colour",Map.of("state","AVAILABLE","quote","black","start",2,"end",7,"outputTruncated",false));var completed=result(lease,bad);
        assertTrue(jobs.complete(who("inference"),A,job.id(),completed));assertTrue(jobs.complete(who("inference"),A,job.id(),completed));assertTrue(jobs.result(who("reporter"),A,job.id()).isPresent());
        assertThrows(Failures.NotFound.class,()->jobs.result(who("other"),A,job.id()));revise(report,1,"A white bag");assertTrue(jobs.result(who("reporter"),A,job.id()).isEmpty());assertFalse(jobs.complete(who("inference"),A,job.id(),completed));
    }
    @Test void rerankRawBytesConcurrentIdempotencyLeaseIsolationAndPinExclusionHold()throws Exception{
        UUID report=report("Black bag").reportId(),candidate=UUID.randomUUID();byte[] raw=request(candidate,"a black travel bag");Set<UUID> ids=new HashSet<>();
        try(var pool=Executors.newFixedThreadPool(3)){var fs=new ArrayList<Future<Job>>();for(int i=0;i<3;i++)fs.add(pool.submit(()->perception().rerank(who("reporter"),A,report,raw)));for(var f:fs)ids.add(f.get(20,TimeUnit.SECONDS).id());}assertEquals(1,ids.size());
        var job=perception().rerank(who("reporter"),A,report,raw);try(var c=admin();var s=c.prepareStatement("SELECT raw_request FROM dzoka.perception_rerank_inputs WHERE tenant_id=? AND job_id=?")){s.setObject(1,A);s.setObject(2,job.id());try(var r=s.executeQuery()){r.next();assertArrayEquals(raw,r.getBytes(1));}}
        assertThrows(Failures.NotFound.class,()->perception().rerank(who("other"),A,report,raw));var lease=claim("RERANK");byte[] input=perception().input(who("inference"),A,job.id(),lease.lease());var payload=new ObjectMapper().readTree(input);assertEquals("Black bag",payload.get("query").asString());assertEquals("a black travel bag",payload.get("candidates").get(0).get("text").asString());
        assertThrows(Failures.NotFound.class,()->perception().input(who("wrong-worker"),A,job.id(),lease.lease()));assertThrows(Failures.NotFound.class,()->perception().input(who("remote"),B,job.id(),lease.lease()));
        int count=app.getBean(TenantTransactions.class).execute(who("wrong-worker"),A,"ai:work",c->{try(var s=c.createStatement();var r=s.executeQuery("SELECT count(*) FROM dzoka.perception_rerank_inputs")){r.next();return r.getInt(1);}});assertEquals(0,count);
        assertThrows(IllegalArgumentException.class,()->perception().complete(who("inference"),A,job.id(),result(lease,ranked(UUID.randomUUID()))));var result=result(lease,ranked(candidate));assertTrue(perception().complete(who("inference"),A,job.id(),result));assertTrue(perception().complete(who("inference"),A,job.id(),result));
        var component=perception().result(who("reporter"),A,job.id()).orElseThrow();var plan=new Plan(UUID.randomUUID(),List.of(new Source(report,1)),List.of(),10000,"a".repeat(64),List.of(new PerceptionPin(report,component.id())));
        assertThrows(Failures.Conflict.class,()->app.getBean(FingerprintRollouts.class).create(who("curator"),A,plan));
        assertEquals(raw.length+input.length,scalar("SELECT raw_bytes FROM dzoka.perception_audio_usage WHERE tenant_id=?"));
        var intake=app.getBean(IntakeService.class);intake.hold(who("reviewer"),A,report,true);
        try(var c=admin();var s=c.prepareStatement("UPDATE dzoka.reports SET expires_at=now()-interval '1 second' WHERE tenant_id=? AND id=?")){s.setObject(1,A);s.setObject(2,report);s.executeUpdate();}
        assertEquals(0,perception().purge(who("purger"),A));intake.hold(who("reviewer"),A,report,false);assertEquals(1,perception().purge(who("purger"),A));assertEquals(0,perception().purge(who("purger"),A));
        assertEquals(0,scalar("SELECT raw_bytes FROM dzoka.perception_audio_usage WHERE tenant_id=?"));assertEquals(0,scalar("SELECT count(*) FROM dzoka.perception_rerank_inputs WHERE tenant_id=?"));
    }
    @Test void directionCandidateBoundsAndChunkedBodiesFailClosed()throws Exception{
        UUID report=report("English source").reportId();assertThrows(IllegalArgumentException.class,()->perception().enqueue(who("reporter"),A,report,PerceptionFixtures.id("TRANSLATION"),null,null));
        assertThrows(IllegalArgumentException.class,()->perception().enqueue(who("reporter"),A,report,PerceptionFixtures.id("RERANK"),null,null));
        assertThrows(IllegalArgumentException.class,()->perception().rerank(who("reporter"),A,report,request(UUID.randomUUID(),"x".repeat(513))));
        assertThrows(IllegalArgumentException.class,()->perception().rerank(who("reporter"),A,report,"{broken".getBytes(StandardCharsets.UTF_8)));
        String duplicate="{\"profileId\":\""+PerceptionFixtures.id("RERANK")+"\",\"profileId\":\""+PerceptionFixtures.id("RERANK")+"\",\"candidates\":[]}";
        assertThrows(IllegalArgumentException.class,()->perception().rerank(who("reporter"),A,report,duplicate.getBytes(StandardCharsets.UTF_8)));
        String body=new String(request(UUID.randomUUID(),"candidate"),StandardCharsets.UTF_8);assertEquals(200,api("POST",A,"/reports/"+report+"/perception/rerank","reporter",body).statusCode());assertEquals(403,api("POST",A,"/reports/"+report+"/perception/rerank","inference",body).statusCode());
        var response=http.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(base+"/api/v1/tenants/"+A+"/reports/"+report+"/perception/rerank")).header("Authorization","Bearer "+token("reporter")).header("Content-Type","application/json").POST(java.net.http.HttpRequest.BodyPublishers.ofInputStream(()->new java.io.ByteArrayInputStream(new byte[16385]))).build(),java.net.http.HttpResponse.BodyHandlers.ofString());assertEquals(413,response.statusCode());
        UUID french=app.getBean(IntakeService.class).submit(who("reporter"),A,null,null,new ReportDraft(ReportDraft.Kind.LOST,"Un sac noir","fr","UTC",null,null,null,null,null,null,null),"{}".getBytes(),UUID.randomUUID().toString()).reportId();
        var job=perception().enqueue(who("reporter"),A,french,PerceptionFixtures.id("TRANSLATION"),null,null);var lease=claim("TRANSLATION");assertTrue(perception().complete(who("inference"),A,job.id(),result(lease,Map.of("text","A black bag","sourceLanguage","fr","targetLanguage","en","inputTokenCount",9,"inputTruncated",false,"outputTruncated",false,"representation","opus-fr-en-greedy-v1"))));
    }
    @Test void rerankPayloadAndQuotaRollbackWithRequestJournal()throws Exception{
        UUID report=report("Atomic rerank").reportId();byte[] raw=request(UUID.randomUUID(),"candidate");
        try(var c=admin();var s=c.createStatement()){s.execute("CREATE OR REPLACE FUNCTION dzoka.fail_language() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF NEW.event_type='perception.requested' THEN RAISE EXCEPTION 'injected'; END IF; RETURN NEW; END $$");s.execute("CREATE TRIGGER fail_language BEFORE INSERT ON dzoka.outbox FOR EACH ROW EXECUTE FUNCTION dzoka.fail_language()");}
        try{assertThrows(Failures.Unavailable.class,()->perception().rerank(who("reporter"),A,report,raw));}
        finally{try(var c=admin();var s=c.createStatement()){s.execute("DROP TRIGGER fail_language ON dzoka.outbox");}}
        assertEquals(0,scalar("SELECT count(*) FROM dzoka.perception_rerank_inputs WHERE tenant_id=?"));assertEquals(0,scalar("SELECT count(*) FROM dzoka.perception_audio_usage WHERE tenant_id=?"));assertEquals(0,scalar("SELECT count(*) FROM dzoka.ai_usage WHERE tenant_id=?"));
    }
}
