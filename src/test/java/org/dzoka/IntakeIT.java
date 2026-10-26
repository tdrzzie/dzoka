package org.dzoka;
import static org.junit.jupiter.api.Assertions.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import org.dzoka.foundation.persistence.TenantTransactions;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.intake.application.IntakeService;
import org.dzoka.intake.domain.*;
import org.junit.jupiter.api.*;
import tools.jackson.databind.ObjectMapper;

class IntakeIT extends IntegrationFixture {
    @Override protected void initializeProviders() throws Exception {
        membership(A,"reporter","reports:write","reports:read");
        membership(A,"other","reports:write","reports:read");
        membership(B,"remote","reports:write","reports:read");
        membership(A,"reviewer","reports:write","reports:read","reports:read:any","reports:write:any","reports:hold");
        membership(A,"purger","reports:purge","media:purge");
    }
    protected String draft(String raw){
        return "{\"kind\":\"LOST\",\"rawText\":"+new ObjectMapper().writeValueAsString(raw)+",\"language\":\"sn\",\"timezone\":\"Africa/Harare\",\"rawLocation\":\"pamusika?\",\"rawTime\":\"yesterday evening\",\"occurredAfter\":\"2026-10-24T16:00:00Z\",\"occurredBefore\":\"2026-10-25T20:00:00Z\"}";
    }
    protected HttpResponse<String> intake(String method,UUID tenant,String suffix,String subject,String body,String key,Integer expected) throws Exception {
        var b=HttpRequest.newBuilder(URI.create(base+"/api/v1/tenants/"+tenant+"/reports"+suffix)).timeout(java.time.Duration.ofSeconds(20));
        b.header("Authorization","Bearer "+token(subject));
        if(key!=null)b.header("Idempotency-Key",key);if(expected!=null)b.header("If-Match",expected.toString());
        if(body!=null)b.header("Content-Type","application/json");
        return http.send(b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
    }
    protected ReportReceipt create(String subject,String body) throws Exception {
        var response=intake("POST",A,"",subject,body,UUID.randomUUID().toString(),null);assertEquals(200,response.statusCode(),response.body());
        return app.getBean(ObjectMapper.class).readValue(response.body(),ReportReceipt.class);
    }
    @Test void rawBytesUnicodeAndUncertainContextArePreservedSeparately() throws Exception {
        String text="  Cafe\u0301\n  nhema  ",body=draft(text);var receipt=create("reporter",body);
        var response=intake("GET",A,"/"+receipt.reportId(),"reporter",null,null,null);assertEquals(200,response.statusCode());
        var view=app.getBean(ObjectMapper.class).readValue(response.body(),ReportView.class);
        assertEquals(text,view.rawText());assertEquals("Caf\u00e9 nhema",view.normalizedText());assertNull(view.latitude());
        assertEquals("Africa/Harare",view.timezone());assertEquals("pamusika?",view.rawLocation());assertNotEquals(view.occurredAfter(),view.occurredBefore());
        try(var c=admin();var s=c.prepareStatement("SELECT raw_request,raw_sha256 FROM dzoka.report_revisions WHERE tenant_id=? AND report_id=?")){
            s.setObject(1,A);s.setObject(2,receipt.reportId());try(var r=s.executeQuery()){assertTrue(r.next());assertArrayEquals(body.getBytes(StandardCharsets.UTF_8),r.getBytes(1));}
        }
        assertNotEquals(receipt.caseId(),receipt.itemId());assertNotEquals(receipt.itemId(),receipt.reportId());
    }
    @Test void sameTenantAndCrossTenantIdsDoNotExposeReports() throws Exception {
        var r=create("reporter",draft("Private synthetic description"));
        assertEquals(404,intake("GET",A,"/"+r.reportId(),"other",null,null,null).statusCode());
        assertEquals(404,intake("GET",B,"/"+r.reportId(),"remote",null,null,null).statusCode());
        assertEquals(200,intake("GET",A,"/"+r.reportId(),"reviewer",null,null,null).statusCode());
        assertEquals(403,intake("POST",A,"", "stranger",draft("No permission"),UUID.randomUUID().toString(),null).statusCode());
    }
    @Test void revisionsAreAppendOnlyAndStaleUpdatesConflict() throws Exception {
        var r=create("reporter",draft("original"));String changed=draft("new description"),key=UUID.randomUUID().toString();
        var v2=intake("POST",A,"/"+r.reportId()+"/revisions","reporter",changed,key,1);assertEquals(200,v2.statusCode());
        assertEquals(v2.body(),intake("POST",A,"/"+r.reportId()+"/revisions","reporter",changed,key,1).body());
        assertEquals(409,intake("POST",A,"/"+r.reportId()+"/revisions","reporter",draft("stale"),UUID.randomUUID().toString(),1).statusCode());
        assertTrue(intake("GET",A,"/"+r.reportId()+"/revisions/1","reporter",null,null,null).body().contains("original"));
        var tx=app.getBean(TenantTransactions.class);
        assertThrows(org.dzoka.foundation.application.Failures.Unavailable.class,()->tx.execute(new VerifiedIdentity(issuer,"reporter"),A,"reports:write",c->{
            try(var s=c.createStatement()){s.executeUpdate("UPDATE dzoka.report_revisions SET raw_text='overwritten'");}return null;
        }));
    }
    @Test void concurrentCreationRetainsOneLogicalResult() throws Exception {
        String key=UUID.randomUUID().toString(),body=draft("Concurrent intake fixture");long before=count("reports",A),events=count("outbox",A);
        Set<String> results=new HashSet<>();
        try(var pool=Executors.newFixedThreadPool(4)){
            var pending=new ArrayList<Future<HttpResponse<String>>>();
            for(int i=0;i<4;i++)pending.add(pool.submit(()->intake("POST",A,"","reporter",body,key,null)));
            for(var p:pending){var r=p.get(25,TimeUnit.SECONDS);assertEquals(200,r.statusCode());results.add(r.body());}
        }
        assertEquals(1,results.size());assertEquals(before+1,count("reports",A));assertEquals(events+1,count("outbox",A));
        assertEquals(409,intake("POST",A,"","reporter",draft("different body"),key,null).statusCode());
    }
    @Test void competingRevisionsCannotOverwriteOneAnother() throws Exception {
        var created=create("reporter",draft("original"));
        try(var pool=Executors.newFixedThreadPool(2)){
            var one=pool.submit(()->intake("POST",A,"/"+created.reportId()+"/revisions","reporter",draft("revision one"),UUID.randomUUID().toString(),1));
            var two=pool.submit(()->intake("POST",A,"/"+created.reportId()+"/revisions","reporter",draft("revision two"),UUID.randomUUID().toString(),1));
            assertEquals(Set.of(200,409),Set.of(one.get(20,TimeUnit.SECONDS).statusCode(),two.get(20,TimeUnit.SECONDS).statusCode()));
        }
        assertTrue(intake("GET",A,"/"+created.reportId()+"/revisions/1","reporter",null,null,null).body().contains("original"));
    }
    @Test void caseAndItemReferencesRemainUnresolvedAndOwnerScoped() throws Exception {
        var created=create("reporter",draft("found observation").replace("LOST","FOUND"));
        for(String path:List.of("/cases/"+created.caseId(),"/items/"+created.itemId())){
            for(String subject:List.of("reporter","other")){
                var response=http.send(HttpRequest.newBuilder(URI.create(base+"/api/v1/tenants/"+A+path))
                    .header("Authorization","Bearer "+token(subject)).GET().build(),HttpResponse.BodyHandlers.ofString());
                assertEquals(subject.equals("reporter")?200:404,response.statusCode());
                if(subject.equals("reporter"))assertTrue(response.body().contains("UNRESOLVED"));
            }
        }
    }
    @Test void invalidPositionTimeZoneAndRoleFieldsFailBeforePersistence() throws Exception {
        long before=count("reports",A);
        for(String body:List.of(draft("bad").replace("Africa/Harare","Not/AZone"),
                draft("bad").replace("2026-10-24T16:00:00Z","2026-10-26T16:00:00Z"),
                draft("bad").replace("{","{\"latitude\":-17.8,"),draft("bad").replace("{","{\"role\":\"ADMIN\",")))
            assertEquals(400,intake("POST",A,"","reporter",body,UUID.randomUUID().toString(),null).statusCode());
        assertEquals(before,count("reports",A));
    }
    @Test void reportCommitRollsBackWhenOutboxFails() throws Exception {
        long before=count("reports",A),revisions=count("report_revisions",A),cases=count("recovery_cases",A);
        try(var c=admin();var s=c.createStatement()){
            s.execute("CREATE OR REPLACE FUNCTION dzoka.test_fail_intake() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF NEW.event_type='report.submitted' THEN RAISE EXCEPTION 'injected'; END IF; RETURN NEW; END $$");
            s.execute("CREATE TRIGGER fail_intake BEFORE INSERT ON dzoka.outbox FOR EACH ROW EXECUTE FUNCTION dzoka.test_fail_intake()");
        }
        try{assertEquals(503,intake("POST",A,"","reporter",draft("rollback"),UUID.randomUUID().toString(),null).statusCode());}
        finally{try(var c=admin();var s=c.createStatement()){s.execute("DROP TRIGGER fail_intake ON dzoka.outbox");}}
        assertEquals(before,count("reports",A));assertEquals(revisions,count("report_revisions",A));assertEquals(cases,count("recovery_cases",A));
    }
    @Test void legalHoldExpiryErasureAndRetryDoNotResurrectRawEvidence() throws Exception {
        String key=UUID.randomUUID().toString(),body=draft("Retention fixture");var created=intake("POST",A,"","reporter",body,key,null);
        var report=app.getBean(ObjectMapper.class).readValue(created.body(),ReportReceipt.class).reportId();
        assertEquals(403,intake("PUT",A,"/"+report+"/hold","reporter","{\"held\":true}",null,null).statusCode());
        assertEquals(200,intake("PUT",A,"/"+report+"/hold","reviewer","{\"held\":true}",null,null).statusCode());
        assertEquals(409,intake("DELETE",A,"/"+report,"reporter",null,null,null).statusCode());
        try(var c=admin();var s=c.prepareStatement("UPDATE dzoka.reports SET expires_at=now()-interval '1 second' WHERE tenant_id=? AND id=?")){
            s.setObject(1,A);s.setObject(2,report);s.executeUpdate();
        }
        var service=app.getBean(IntakeService.class);var purger=new VerifiedIdentity(issuer,"purger");
        assertEquals(0,service.expire(purger,A));
        assertEquals(200,intake("PUT",A,"/"+report+"/hold","reviewer","{\"held\":false}",null,null).statusCode());
        assertEquals(1,service.expire(purger,A));assertEquals(0,service.expire(purger,A));
        assertEquals(404,intake("GET",A,"/"+report,"reporter",null,null,null).statusCode());
        assertNotEquals(200,intake("POST",A,"","reporter",body,key,null).statusCode());
        try(var c=admin();var s=c.prepareStatement("SELECT count(*) FROM dzoka.report_revisions WHERE tenant_id=? AND report_id=?")){
            s.setObject(1,A);s.setObject(2,report);try(var r=s.executeQuery()){r.next();assertEquals(0,r.getInt(1));}
        }
    }
}
