package org.dzoka;

import static org.junit.jupiter.api.Assertions.*;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.dzoka.operations.*;
import org.dzoka.foundation.application.Failures;
import org.dzoka.foundation.persistence.TenantTransactions;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.intake.application.IntakeService;
import org.dzoka.intake.domain.ReportDraft;
import org.dzoka.intake.ports.Reports;
import org.dzoka.events.adapters.JdbcOutbox;
import org.junit.jupiter.api.*;

class OperationsIT extends IntegrationFixture {
    private com.sun.net.httpserver.HttpServer collector;private final List<String> exports=new CopyOnWriteArrayList<>();private final AtomicBoolean failing=new AtomicBoolean();
    private static final String SHA="e".repeat(64);
    private final UUID analyticsTenant=UUID.randomUUID();
    private VerifiedIdentity who(String actor){return new VerifiedIdentity(issuer,actor);}
    @Override protected void initializeProviders() throws Exception {
        membership(A,"operator","retention:manage","retention:recover","operations:read","reports:purge");
        membership(A,"writer","reports:write","reports:read");membership(A,"writer2","reports:write","reports:read");
        membership(B,"remote-writer","reports:write","reports:read");
        try(var c=admin();var s=c.prepareStatement("INSERT INTO dzoka.tenants(id,name) VALUES(?,'Synthetic analytics')")){s.setObject(1,analyticsTenant);s.executeUpdate();}
        membership(analyticsTenant,"analyst","analytics:read","reports:write");
        collector=com.sun.net.httpserver.HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        collector.createContext("/v1/",e->{exports.add(e.getRequestURI().getPath()+new String(e.getRequestBody().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));byte[] body="{}".getBytes();e.getResponseHeaders().set("Content-Type","application/json");e.sendResponseHeaders(failing.get()?503:200,body.length);try(var out=e.getResponseBody()){out.write(body);}});collector.start();
    }
    @Override protected List<String> extraOptions(){return List.of("--dzoka.otlp.origin=http://127.0.0.1:"+collector.getAddress().getPort(),"--dzoka.operations.allow-insecure-loopback=true");}
    @AfterAll void stopCollector(){if(collector!=null)collector.stop(0);}
    @Test void analyticsSuppressesSmallGroupsAndExportsOnlyApprovedCoarseFields() {
        var operations=app.getBean(Operations.class);var intake=app.getBean(IntakeService.class);Instant when=Instant.parse("2026-11-25T12:01:00Z");
        for(int i=0;i<6;i++)intake.submit(who("analyst"),analyticsTenant,null,null,new ReportDraft(ReportDraft.Kind.LOST,"NEVER_EXPORT_TEXT","en","UTC","NEVER_EXPORT_LOCATION",null,-17.8,31.05,25.0,when,when.plusSeconds(10),"BAG"),"synthetic".getBytes(),UUID.randomUUID().toString());
        var result=operations.analytics(who("analyst"),analyticsTenant,Instant.now().minus(Duration.ofDays(1)));
        assertEquals(6,result.get("reports").asInt());assertEquals(1,result.get("commonCategories").size());assertEquals(1,result.get("commonLossAreas").size());assertEquals(1,result.get("commonLossUtcHours").size());assertTrue(result.get("ownershipFalsePositiveRate").isNull());assertFalse(result.toString().contains("NEVER_EXPORT"));assertFalse(result.toString().contains("31.05"));
    }
    private UUID report(String actor,String key){return app.getBean(IntakeService.class).submit(who(actor),A,null,null,new ReportDraft(ReportDraft.Kind.LOST,"Synthetic sensitive phrase","en","UTC",null,null,null,null,null,null,null),"fixture".getBytes(),key).reportId();}
    @Test void quotasRestoreErasureAndInFlightWorkAreEnforced()throws Exception {
        var operations=app.getBean(Operations.class);operations.policy(who("operator"),A,new Operations.Policy(2,2,3,SHA));
        UUID foreign=app.getBean(IntakeService.class).submit(who("remote-writer"),B,null,null,new ReportDraft(ReportDraft.Kind.LOST,"Foreign private fixture","en","UTC",null,null,null,null,null,null,null),"foreign".getBytes(),UUID.randomUUID().toString()).reportId();
        var pool=Executors.newFixedThreadPool(3);String key=UUID.randomUUID().toString();UUID first=report("writer",key);assertEquals(first,report("writer",key));
        var one=pool.submit(()->{try{report("writer",UUID.randomUUID().toString());return true;}catch(Failures.RateLimited limited){return false;}});
        var two=pool.submit(()->{try{report("writer",UUID.randomUUID().toString());return true;}catch(Failures.RateLimited limited){return false;}});
        assertNotEquals(one.get(10,TimeUnit.SECONDS),two.get(10,TimeUnit.SECONDS));report("writer2",UUID.randomUUID().toString());assertThrows(Failures.RateLimited.class,()->report("writer2",UUID.randomUUID().toString()));
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);var tx=app.getBean(TenantTransactions.class);
        assertEquals(0,tx.execute(who("operator"),A,"retention:recover",db->{try(var s=db.prepareStatement("SELECT count(*) FROM dzoka.reports WHERE id=?")){s.setObject(1,foreign);try(var r=s.executeQuery()){r.next();return r.getInt(1);}}}).intValue());
        assertEquals(0,tx.execute(who("operator"),A,"retention:recover",db->{try(var s=db.prepareStatement("UPDATE dzoka.reports SET deleted_at=clock_timestamp(),version=version+1 WHERE id=?")){s.setObject(1,foreign);return s.executeUpdate();}}).intValue());
        var inflight=pool.submit(()->tx.execute(who("writer"),A,"reports:read",db->{entered.countDown();try{if(!release.await(3,TimeUnit.SECONDS))throw new IllegalStateException();}catch(InterruptedException interrupted){throw new IllegalStateException(interrupted);}return true;}));
        assertTrue(entered.await(3,TimeUnit.SECONDS));var closing=pool.submit(()->operations.close(who("operator"),A,SHA));
        Thread.sleep(150);assertFalse(closing.isDone());release.countDown();assertTrue(inflight.get(5,TimeUnit.SECONDS));closing.get(5,TimeUnit.SECONDS);
        assertThrows(Failures.Conflict.class,()->report("writer",UUID.randomUUID().toString()));
        String manifest=operations.replay(who("operator"),A,new Operations.Replay(SHA,List.of(new Operations.Erasure(first,10),new Operations.Erasure(foreign,10))));
        assertThrows(Failures.Conflict.class,()->operations.open(who("operator"),A,new Operations.Open(manifest,SHA,SHA)));
        var purged=api("POST",A,"/operations/maintenance/reports","operator",null);assertEquals(200,purged.statusCode());assertTrue(purged.body().contains("\"purged\":1"));
        assertTrue(operations.erasures(who("operator"),A,null).stream().anyMatch(e->e.report().equals(first)&&e.version()>=10));
        operations.open(who("operator"),A,new Operations.Open(manifest,SHA,SHA));pool.shutdownNow();
        try(var c=admin();var s=c.prepareStatement("SELECT erased_at IS NOT NULL FROM dzoka.reports WHERE tenant_id=? AND id=?")){s.setObject(1,A);s.setObject(2,first);try(var r=s.executeQuery()){r.next();assertTrue(r.getBoolean(1));}}
        try(var c=admin();var s=c.prepareStatement("SELECT version,deleted_at FROM dzoka.reports WHERE tenant_id=? AND id=?")){s.setObject(1,B);s.setObject(2,foreign);try(var r=s.executeQuery()){assertTrue(r.next());assertEquals(1,r.getLong(1));assertNull(r.getObject(2));}}
        assertEquals(403,api("GET",B,"/operations/status","operator",null).statusCode());assertEquals(403,api("PUT",A,"/operations/policy","viewer","{\"reportRetentionDays\":2,\"dailyActorLimit\":2,\"dailyTenantLimit\":3,\"evidenceSha\":\""+SHA+"\"}").statusCode());
    }
    @Test void telemetryIsPrivateBoundedAndDoesNotBreakRequestsDuringCollectorOutage()throws Exception {
        String parent="00-"+"1".repeat(32)+"-"+"2".repeat(16)+"-01";
        var response=http.send(HttpRequest.newBuilder(URI.create(base+"/api/v1/tenants/"+A+"/organizations?secret=DO_NOT_EXPORT"))
            .header("Authorization","Bearer "+token("alice")).header("traceparent",parent).POST(HttpRequest.BodyPublishers.ofString("{\"name\":\"DO_NOT_EXPORT\"}")).header("Content-Type","application/json").header("Idempotency-Key",UUID.randomUUID().toString()).build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(200,response.statusCode());assertEquals("1".repeat(32),response.headers().firstValue("X-Trace-Id").orElseThrow());
        var outbox=app.getBean(JdbcOutbox.class).claim(who("worker"),A,Duration.ofSeconds(30)).orElseThrow();assertNotNull(outbox.event().traceparent());assertTrue(outbox.event().traceparent().startsWith("00-"+"1".repeat(32)));
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);while(exports.stream().noneMatch(e->e.startsWith("/v1/logs"))&&System.nanoTime()<deadline)Thread.sleep(20);
        String observed=String.join("",exports);assertTrue(observed.contains("resourceSpans"));assertTrue(observed.contains("resourceMetrics"));assertTrue(observed.contains("resourceLogs"));assertFalse(observed.contains("DO_NOT_EXPORT"));assertFalse(observed.contains(A.toString()));assertFalse(observed.contains("Bearer"));
        if(System.getenv("DZOKA_TEST_OTLP_OUTPUT")!=null){var json=new tools.jackson.databind.ObjectMapper();var signals=exports.stream().map(e->{int start=e.indexOf('{');return Map.of("kind",e.substring(0,start),"payload",json.readTree(e.substring(start)));}).toList();java.nio.file.Files.writeString(java.nio.file.Path.of(System.getenv("DZOKA_TEST_OTLP_OUTPUT")),json.writeValueAsString(signals));}
        failing.set(true);assertEquals(200,http.send(HttpRequest.newBuilder(URI.create(base+"/health/ready")).build(),HttpResponse.BodyHandlers.ofString()).statusCode());
        var telemetry=app.getBean(Telemetry.class);deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);while(telemetry.counters().get("telemetryExportFailures")==0&&System.nanoTime()<deadline)Thread.sleep(20);assertTrue(telemetry.counters().get("telemetryExportFailures")>0);
        failing.set(false);assertNull(TraceContext.current());assertNotEquals("0".repeat(32),telemetry.context("00-"+"0".repeat(32)+"-"+"0".repeat(16)+"-01").trace());
    }
}
