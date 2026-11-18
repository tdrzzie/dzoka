package org.dzoka;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import org.dzoka.fingerprints.domain.FingerprintRollout.Plan;
import org.dzoka.fingerprints.domain.FingerprintRollout.Source;
import org.dzoka.fingerprints.ports.FingerprintRollouts;
import org.dzoka.intake.application.IntakeService;
import org.dzoka.intake.domain.ReportDraft;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;

class MatchingIT extends AiFixture {
    private final ObjectMapper json = new ObjectMapper();

    @Override protected List<String> extraOptions() {
        return List.of("--dzoka.matching.vector-backend="+System.getenv().getOrDefault("DZOKA_TEST_MATCHING_BACKEND","exact"));
    }

    @Override protected void initializeProviders() throws Exception {
        super.initializeProviders();
        membership(A,"matcher","reports:read","reports:read:any","matches:read");
        membership(A,"verifier","reports:read:any","matches:attest-identifiers");
        membership(A,"limited","reports:read","matches:read","fingerprints:manage");
        membership(B,"matcher","reports:read","reports:read:any","matches:read");
        membership(B,"remote-reporter","reports:write");
    }

    private UUID submit(UUID tenant, String actor, ReportDraft.Kind kind, String text) {
        return app.getBean(IntakeService.class).submit(who(actor),tenant,null,null,
                new ReportDraft(kind,text,"en","UTC",null,null,-17.8,31.05,30.0,null,null),
                "{}".getBytes(java.nio.charset.StandardCharsets.UTF_8),UUID.randomUUID().toString()).reportId();
    }

    private String path(UUID report) { return "/internal/matching/reports/"+report; }
    private String assertion(String digest) {
        return "{\"revision\":1,\"keyVersion\":1,\"scopeDigest\":\""+"a".repeat(64)+"\",\"digest\":\""+digest+
                "\",\"attestationId\":\""+UUID.randomUUID()+"\"}";
    }

    @Test void realHttpSearchEnforcesVisibilityAndVerifiedConflictDespiteExactVector() throws Exception {
        UUID query=submit(A,"reporter",ReportDraft.Kind.LOST,"blue backpack near terminus");
        UUID positive=submit(A,"other",ReportDraft.Kind.FOUND,"blue backpack near terminus");
        UUID negative=submit(A,"other",ReportDraft.Kind.FOUND,"blue backpack near terminus");
        UUID remote=submit(B,"remote-reporter",ReportDraft.Kind.FOUND,"blue backpack near terminus");
        String secret="c".repeat(64);
        for (UUID id : List.of(query,positive,negative)) {
            assertEquals(204,api("POST",A,path(id)+"/identifier-attestations","verifier",assertion(id.equals(negative)?"b".repeat(64):secret)).statusCode());
            jobs().enqueue(who(id.equals(query)?"reporter":"other"),A,id,primary);
            var lease=jobs().claim(who("inference"),A,primary).orElseThrow();
            assertEquals(id,lease.reportId());
            assertTrue(jobs().complete(who("inference"),A,lease.jobId(),result(lease)));
        }
        var rollouts=app.getBean(FingerprintRollouts.class);
        var plan=new Plan(UUID.randomUUID(),List.of(new Source(query,1),new Source(positive,1),new Source(negative,1)),List.of(primary),10000,"a".repeat(64));
        rollouts.create(who("curator"),A,plan);
        rollouts.seal(who("curator"),A,plan.id());
        rollouts.activate(who("curator"),A,plan.id(),0,false);
        String search=path(query)+"/candidates?spaceId="+primary;
        if ("qdrant".equals(System.getenv("DZOKA_TEST_MATCHING_BACKEND"))) {
            var missing=api("GET",A,search,"matcher",null);
            assertEquals(200,missing.statusCode());
            var partial=json.readTree(missing.body());
            assertEquals("FAILED",partial.get("routes").get(3).get("state").asString());
            assertEquals(positive.toString(),partial.get("candidates").get(0).get("report").asString());
            seedQdrant(positive,negative);
        }
        assertEquals(403,api("GET",A,search,"reporter",null).statusCode());
        assertEquals(404,api("GET",B,search,"matcher",null).statusCode());
        assertEquals(404,api("GET",A,search,"limited",null).statusCode());
        assertEquals(403,api("POST",A,path(query)+"/identifier-attestations","matcher",assertion(secret)).statusCode());
        assertEquals(409,api("POST",A,path(query)+"/identifier-attestations","verifier",assertion(secret)).statusCode());
        var response=api("GET",A,search,"matcher",null);
        assertEquals(200,response.statusCode(),response.body());
        assertFalse(response.body().contains(secret));
        assertFalse(response.body().contains("blue backpack"));
        assertFalse(response.body().contains(remote.toString()));
        var candidates=json.readTree(response.body()).get("candidates");
        assertEquals(2,candidates.size());
        assertEquals(positive.toString(),candidates.get(0).get("report").asString());
        assertEquals("CONFLICT",candidates.get(1).get("score").get("disposition").asString());
        var route=json.readTree(response.body()).get("routes").get(3);
        String expected=System.getenv().getOrDefault("DZOKA_TEST_MATCHING_ROUTE","EXACT_ROUTE");
        assertEquals(expected,route.get("reason").asString());
        assertEquals(!expected.endsWith("_UNAVAILABLE"),candidates.get(1).get("ranks").has("EXACT_VECTOR"));
        assertEquals(expected.endsWith("_UNAVAILABLE")?"FAILED":"COMPLETE",route.get("state").asString());
        var incompatible=api("GET",A,path(query)+"/candidates?spaceId="+alternate,"matcher",null);
        assertEquals(200,incompatible.statusCode());
        assertFalse(json.readTree(incompatible.body()).get("candidates").get(0).get("ranks").has("EXACT_VECTOR"));
        try(var db=admin();var update=db.prepareStatement("UPDATE dzoka.reports SET deleted_at=clock_timestamp() WHERE tenant_id=? AND id=?")) {
            update.setObject(1,A);update.setObject(2,negative);assertEquals(1,update.executeUpdate());
        }
        assertFalse(api("GET",A,search,"matcher",null).body().contains(negative.toString()));
        assertEquals(400,api("GET",A,search+"&limit=21","matcher",null).statusCode());
    }

    private void seedQdrant(UUID... reports) throws Exception {
        var points=new ArrayList<Map<String,Object>>();
        double[] vector=new double[384];vector[0]=1;
        for (UUID report:reports) {
            var component=jobs().current(who("other"),A,report).getFirst();
            points.add(Map.of("id",component.id().toString(),"vector",vector,
                    "payload",Map.of("tenant",A.toString(),"space",profile.spaceKey())));
        }
        // A stale projection entry can satisfy both payload predicates. The explicit component allowlist must still exclude it.
        points.add(Map.of("id",UUID.randomUUID().toString(),"vector",vector,
                "payload",Map.of("tenant",A.toString(),"space",profile.spaceKey())));
        String path="/collections/dzoka_"+profile.spaceKey();
        qdrantPut(path,Map.of("vectors",Map.of("size",384,"distance","Cosine")));
        qdrantPut(path+"/points?wait=true",Map.of("points",points));
    }

    private void qdrantPut(String path, Map<String,?> value) throws Exception {
        var req=java.net.http.HttpRequest.newBuilder(java.net.URI.create(System.getenv("DZOKA_MATCHING_QDRANT_ORIGIN")+path))
                .timeout(java.time.Duration.ofSeconds(8))
                .header("api-key",System.getenv("DZOKA_MATCHING_QDRANT_API_KEY"))
                .header("Content-Type","application/json")
                .PUT(java.net.http.HttpRequest.BodyPublishers.ofByteArray(json.writeValueAsBytes(value))).build();
        assertEquals(200,http.send(req,java.net.http.HttpResponse.BodyHandlers.discarding()).statusCode());
    }
}
