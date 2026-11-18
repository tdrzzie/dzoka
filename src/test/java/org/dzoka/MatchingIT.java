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
        membership(A,"second-matcher","reports:read","reports:read:any","matches:read");
        membership(A,"projection-maintainer","reports:read","reports:read:any","matches:maintain-projection");
        membership(A,"history-maintainer","matches:maintain-history");
        membership(A,"verifier","reports:read:any","matches:attest-identifiers");
        membership(A,"limited","reports:read","matches:read","fingerprints:manage");
        membership(B,"matcher","reports:read","reports:read:any","matches:read");
        membership(B,"remote-reporter","reports:write");
        if ("pgvector-indexed".equals(System.getenv("DZOKA_TEST_MATCHING_BACKEND"))) {
            try(var db=admin();var sql=db.createStatement()) {
                String ddl=java.nio.file.Files.readString(java.nio.file.Path.of("tools/m5_install_pgvector_index.sql"));
                int creation=ddl.indexOf("CREATE INDEX CONCURRENTLY");
                assertTrue(creation>=0);
                sql.execute(ddl.substring(0,creation));
                sql.execute(ddl.substring(creation));
            }
        }
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
            var stale=seedQdrant();
            String projection="/internal/matching/projections/"+primary;
            assertEquals(403,api("PUT",A,projection,"matcher",null).statusCode());
            var rebuilt=api("PUT",A,projection,"projection-maintainer",null);
            assertEquals(200,rebuilt.statusCode(),rebuilt.body());
            assertEquals(3,json.readTree(rebuilt.body()).get("activeComponents").asInt());
            assertEquals(200,api("PUT",A,projection,"projection-maintainer",null).statusCode());
            assertFalse(qdrantPointExists(stale.get(0)));
            assertTrue(qdrantPointExists(stale.get(1)));
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
        assertEquals(expected.endsWith("_UNAVAILABLE")?"FAILED":expected.equals("PGVECTOR_HNSW")?"APPROXIMATE":"COMPLETE",route.get("state").asString());
        var incompatible=api("GET",A,path(query)+"/candidates?spaceId="+alternate,"matcher",null);
        assertEquals(200,incompatible.statusCode());
        assertFalse(json.readTree(incompatible.body()).get("candidates").get(0).get("ranks").has("EXACT_VECTOR"));
        UUID runId=UUID.randomUUID();
        String run=path(query)+"/runs/"+runId;
        String request="{\"spaceId\":\""+primary+"\",\"limit\":10}";
        var stored=api("PUT",A,run,"matcher",request);
        assertEquals(200,stored.statusCode(),stored.body());
        assertEquals(json.readTree(stored.body()),json.readTree(api("GET",A,run,"matcher",null).body()));
        assertEquals(json.readTree(stored.body()),json.readTree(api("PUT",A,run,"matcher",request).body()));
        assertEquals(404,api("GET",A,run,"second-matcher",null).statusCode());
        assertEquals(404,api("GET",B,run,"matcher",null).statusCode());
        assertEquals(409,api("PUT",A,run,"matcher",request.replace("10}","9}")).statusCode());
        try(var db=admin();var update=db.prepareStatement("UPDATE dzoka.reports SET deleted_at=clock_timestamp() WHERE tenant_id=? AND id=?")) {
            update.setObject(1,A);update.setObject(2,negative);assertEquals(1,update.executeUpdate());
        }
        assertFalse(api("GET",A,search,"matcher",null).body().contains(negative.toString()));
        assertEquals(404,api("GET",A,run,"matcher",null).statusCode());
        assertEquals(400,api("GET",A,search+"&limit=21","matcher",null).statusCode());
        String prune="/internal/matching/history/prune";
        assertEquals(403,api("POST",A,prune,"matcher",null).statusCode());
        assertEquals(0,json.readTree(api("POST",A,prune,"history-maintainer",null).body()).get("removed").asInt());
        try(var db=admin();var age=db.prepareStatement("UPDATE dzoka.match_search_runs SET created_at=clock_timestamp()-interval '25 hours' WHERE tenant_id=? AND id=?")) {
            age.setObject(1,A);age.setObject(2,runId);assertEquals(1,age.executeUpdate());
        }
        var expired=api("POST",A,prune,"history-maintainer",null);
        assertEquals(200,expired.statusCode(),expired.body());
        assertEquals(1,json.readTree(expired.body()).get("removed").asInt());
        String live=path(query)+"/runs/"+UUID.randomUUID();
        assertEquals(200,api("PUT",A,live,"matcher",request).statusCode());
        if ("pgvector-indexed".equals(System.getenv("DZOKA_TEST_MATCHING_BACKEND"))) {
            addUnindexedReports(positive);
            var overflow=api("GET",A,search,"matcher",null);
            assertEquals(200,overflow.statusCode(),overflow.body());
            var result=json.readTree(overflow.body());
            assertTrue(result.get("partial").asBoolean());
            assertEquals("CAPACITY_EXCEEDED",result.get("routes").get(1).get("state").asString());
            assertEquals("PGVECTOR_HNSW",result.get("routes").get(3).get("reason").asString());
            assertEquals(1,result.get("candidates").size());
            assertEquals(positive.toString(),result.get("candidates").get(0).get("report").asString());
        }
    }

    private List<UUID> seedQdrant() throws Exception {
        var points=new ArrayList<Map<String,Object>>();
        double[] vector=new double[384];vector[0]=1;
        // A stale projection entry can satisfy both payload predicates. The explicit component allowlist must still exclude it.
        UUID stale=UUID.randomUUID(),foreign=UUID.randomUUID();
        points.add(Map.of("id",stale.toString(),"vector",vector,"payload",Map.of("tenant",A.toString(),"space",profile.spaceKey())));
        points.add(Map.of("id",foreign.toString(),"vector",vector,"payload",Map.of("tenant",B.toString(),"space",profile.spaceKey())));
        String path="/collections/dzoka_"+profile.spaceKey();
        qdrantPut(path,Map.of("vectors",Map.of("size",384,"distance","Cosine")));
        qdrantPut(path+"/points?wait=true",Map.of("points",points));
        return List.of(stale,foreign);
    }

    private boolean qdrantPointExists(UUID point) throws Exception {
        var request=java.net.http.HttpRequest.newBuilder(java.net.URI.create(System.getenv("DZOKA_MATCHING_QDRANT_ORIGIN")+
                "/collections/dzoka_"+profile.spaceKey()+"/points/"+point))
                .timeout(java.time.Duration.ofSeconds(5)).header("api-key",System.getenv("DZOKA_MATCHING_QDRANT_API_KEY")).GET().build();
        int status=http.send(request,java.net.http.HttpResponse.BodyHandlers.discarding()).statusCode();
        assertTrue(status==200 || status==404);
        return status==200;
    }

    private void addUnindexedReports(UUID template) throws Exception {
        try(var db=admin();var sql=db.prepareStatement("""
                WITH new_reports AS (
                  INSERT INTO dzoka.reports(tenant_id,id,case_id,item_id,owner_ref,kind,current_revision,expires_at)
                  SELECT r.tenant_id,gen_random_uuid(),r.case_id,r.item_id,r.owner_ref,r.kind,1,r.expires_at
                  FROM dzoka.reports r CROSS JOIN generate_series(1,513) WHERE r.tenant_id=? AND r.id=?
                  RETURNING tenant_id,id,owner_ref
                ) INSERT INTO dzoka.report_revisions(tenant_id,report_id,revision,raw_request,raw_sha256,
                                                      normalization_version,language,timezone,actor_ref)
                SELECT tenant_id,id,1,''::bytea,repeat('a',64),'fixture','en','UTC',owner_ref FROM new_reports
                """)) {
            sql.setObject(1,A);sql.setObject(2,template);assertEquals(513,sql.executeUpdate());
        }
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
