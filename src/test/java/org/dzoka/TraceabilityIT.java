package org.dzoka;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.intake.application.IntakeService;
import org.dzoka.intake.domain.ReportDraft;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class TraceabilityIT extends IntegrationFixture {
    private final ObjectMapper json=new ObjectMapper();
    private final List<JsonNode> captures=new CopyOnWriteArrayList<>();
    private final AtomicInteger response=new AtomicInteger(202);
    private final AtomicInteger polls=new AtomicInteger();
    private HttpServer repository;

    @Override protected void initializeProviders() throws Exception {
        membership(A,"trace-staff","trace:manage","trace:read","trace:dispatch","reports:read","reports:write","recovery:manage","custody:operate");
        membership(B,"trace-staff","trace:manage","trace:read","trace:dispatch");
        repository=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        repository.createContext("/capture",exchange -> {
            try(exchange) {
                if(!"Bearer local-fixture-only".equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
                    exchange.sendResponseHeaders(401,-1);
                    return;
                }
                if("POST".equals(exchange.getRequestMethod())) {
                    if(!"rollback".equals(exchange.getRequestHeaders().getFirst("GS1-Capture-Error-Behaviour"))) {
                        exchange.sendResponseHeaders(400,-1);
                        return;
                    }
                    captures.add(json.readTree(exchange.getRequestBody().readNBytes(16384)));
                    int status=response.get();
                    if(status==202) exchange.getResponseHeaders().set("Location","/capture/job-"+captures.size());
                    exchange.sendResponseHeaders(status,-1);
                } else {
                    int poll=polls.getAndIncrement();
                    byte[] state=json.writeValueAsBytes(Map.of("running",poll==0,"success",poll!=0,"captureErrorBehaviour","rollback"));
                    exchange.sendResponseHeaders(200,state.length);
                    exchange.getResponseBody().write(state);
                }
            }
        });
        repository.start();
    }

    @Override protected List<String> extraOptions() {
        return List.of("--DZOKA_EPCIS_ORIGIN=http://127.0.0.1:"+repository.getAddress().getPort(),
                "--DZOKA_EPCIS_TOKEN=local-fixture-only","--DZOKA_EPCIS_TEST_LOOPBACK=true");
    }
    @AfterAll void closeRepository() { if(repository!=null) repository.stop(0); }

    @Test void nativeStorageExportsOneLogicalProjectionAndAppendOnlyCorrection() throws Exception {
        String privateMarker="Synthetic private intake marker";
        UUID report=app.getBean(IntakeService.class).submit(new VerifiedIdentity(issuer,"trace-staff"),A,null,null,
                new ReportDraft(ReportDraft.Kind.FOUND,privateMarker,"en","UTC",null,null,null,null,null,null,null),
                privateMarker.getBytes(StandardCharsets.UTF_8),UUID.randomUUID().toString()).reportId();
        UUID intake=UUID.randomUUID(),site=UUID.randomUUID(),storage=UUID.randomUUID();
        String recovery="/recovery/intakes/"+intake;
        expect(204,call("PUT",A,recovery,"trace-staff",Map.of("foundReport",report,"site",site)));
        Instant observed=Instant.now().truncatedTo(ChronoUnit.SECONDS);
        UUID destination=UUID.randomUUID();
        expect(200,call("PUT",A,recovery+"/custody/"+storage,"trace-staff",Map.of("action","STORE","expectedGeneration",1,
                "destination",destination,"observedAt",observed.toString(),"evidenceSha","f".repeat(64))));
        String authorization="/traceability/intakes/"+intake+"/authorization";
        String capture="/traceability/intakes/"+intake+"/native/"+storage;
        expect(403,call("PUT",A,capture,"trace-staff",Map.of("kind","STORED","mapping",1)));
        expect(403,call("PUT",A,authorization,"viewer",Map.of("enabled",true)));
        expect(204,call("PUT",A,authorization,"trace-staff",Map.of("enabled",true)));

        var first=expect(202,call("PUT",A,capture,"trace-staff",Map.of("kind","STORED","mapping",1)));
        UUID id=UUID.fromString(first.path("id").asString());
        assertEquals(id.toString(),expect(202,call("PUT",A,capture,"trace-staff",Map.of("kind","STORED","mapping",1))).path("id").asString());
        expect(400,call("PUT",A,capture,"trace-staff",Map.of("kind","INCIDENT","mapping",1)));
        expect(404,call("PUT",A,capture,"trace-staff",Map.of("kind","TRANSFER_ACCEPTED","mapping",1)));
        expect(409,call("PUT",A,capture,"trace-staff",Map.of("kind","STORED","mapping",2,"predecessor",id)));
        expect(404,call("GET",B,"/traceability/events/"+id,"trace-staff",null));
        assertEquals("SUBMITTED",dispatch());
        assertEquals("SUBMITTED",poll(id));
        assertEquals("COMPLETE",poll(id));
        assertEquals(1,captures.size());
        var original=events(captures.getFirst()).path(0);
        assertEquals("ObjectEvent",original.path("type").asString());
        assertEquals("OBSERVE",original.path("action").asString());
        assertEquals(observed.toString(),original.path("eventTime").asString());
        assertEquals("urn:uuid:"+id,original.path("eventID").asString());

        var amended=expect(202,call("PUT",A,capture,"trace-staff",Map.of("kind","STORED","mapping",2,"predecessor",id)));
        UUID correction=UUID.fromString(amended.path("id").asString());
        assertEquals(correction.toString(),expect(202,call("PUT",A,capture,"trace-staff",
                Map.of("kind","STORED","mapping",2,"predecessor",id))).path("id").asString());
        assertEquals("SUBMITTED",dispatch());
        assertEquals("COMPLETE",poll(correction));
        assertEquals(2,captures.size());
        var corrected=events(captures.get(1));
        assertEquals(2,corrected.size());
        var declaration=(tools.jackson.databind.node.ObjectNode)corrected.path(0).deepCopy();
        assertEquals("urn:uuid:"+correction,declaration.remove("errorDeclaration").path("correctiveEventIDs").path(0).asString());
        assertEquals(original,declaration);
        assertEquals("storing",corrected.path(1).path("bizStep").asString());
        assertEquals(original.path("epcList"),corrected.path(1).path("epcList"));
        String published=captures.toString();
        for(String secret:List.of(privateMarker,report.toString(),intake.toString(),storage.toString(),site.toString(),
                destination.toString(),"f".repeat(64),"trace-staff")) assertFalse(published.contains(secret));

        UUID second=UUID.randomUUID();
        expect(200,call("PUT",A,recovery+"/custody/"+second,"trace-staff",Map.of("action","STORE","expectedGeneration",2,
                "destination",UUID.randomUUID(),"observedAt",observed.toString(),"evidenceSha","a".repeat(64))));
        var queued=expect(202,call("POST",A,"/traceability/project-next","trace-staff",null));
        UUID ambiguous=UUID.fromString(queued.path("id").asString());
        expect(204,call("POST",A,"/traceability/project-next","trace-staff",null));
        response.set(503);
        assertEquals("UNKNOWN",dispatch());
        assertEquals("IDLE",dispatch());
        assertEquals(3,captures.size());
        expect(204,call("PUT",A,"/traceability/events/"+ambiguous+"/receipt","trace-staff",
                Map.of("receipt","external-confirmed-job","evidenceSha","a".repeat(64))));
        assertEquals("COMPLETE",dispatch());
        assertEquals(3,captures.size());
        assertEquals(2,count("staff_custody_events",A));
        UUID third=UUID.randomUUID();
        expect(200,call("PUT",A,recovery+"/custody/"+third,"trace-staff",Map.of("action","STORE","expectedGeneration",3,
                "destination",UUID.randomUUID(),"observedAt",observed.toString(),"evidenceSha","b".repeat(64))));
        var revoked=expect(202,call("POST",A,"/traceability/project-next","trace-staff",null)).path("id").asString();
        expect(204,call("PUT",A,authorization,"trace-staff",Map.of("enabled",false)));
        expect(204,call("PUT",A,authorization,"trace-staff",Map.of("enabled",true)));
        assertEquals("CANCELLED",expect(200,call("GET",A,"/traceability/events/"+revoked,"trace-staff",null)).path("state").asString());
        assertEquals("IDLE",dispatch());
        assertEquals(3,captures.size());
        if("true".equals(System.getenv("DZOKA_TEST_EPCIS_EVIDENCE"))) {
            var file=java.nio.file.Files.createTempFile(java.nio.file.Path.of(".test-tmp"),"epcis-fixture-",".json");
            java.nio.file.Files.write(file,json.writeValueAsBytes(captures));
            System.out.println("Synthetic EPCIS documents: "+file.toAbsolutePath());
        }
    }

    private static JsonNode events(JsonNode doc) { return doc.path("epcisBody").path("eventList"); }

    private String poll(UUID id) throws Exception {
        try(var db=admin();var sql=db.prepareStatement("UPDATE dzoka.epcis_projections SET ready_at=clock_timestamp() WHERE tenant_id=? AND id=?")) {
            sql.setObject(1,A);sql.setObject(2,id);sql.executeUpdate();
        }
        return dispatch();
    }
    private String dispatch() throws Exception {
        return expect(200,call("POST",A,"/traceability/dispatch","trace-staff",null)).path("state").asString();
    }
    private HttpResponse<String> call(String method,UUID tenant,String path,String subject,Object body) throws Exception {
        return api(method,tenant,path,subject,body==null?null:json.writeValueAsString(body));
    }
    private JsonNode expect(int status,HttpResponse<String> response) {
        assertEquals(status,response.statusCode(),response.body());
        return response.body().isBlank()?null:json.readTree(response.body());
    }
}
