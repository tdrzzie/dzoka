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
        membership(A,"quota-matcher","reports:read","reports:read:any","matches:read");
        membership(A,"calibration-author","matches:propose-calibration","matches:review-calibration","reports:read","reports:read:any");
        membership(A,"calibration-reviewer","matches:review-calibration","reports:read","reports:read:any");
        membership(B,"calibration-reviewer","matches:review-calibration","reports:read","reports:read:any");
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
        return submit(tenant,actor,kind,text,-17.8,31.05,30.0);
    }

    private UUID submit(UUID tenant,String actor,ReportDraft.Kind kind,String text,
                        Double latitude,Double longitude,Double uncertainty) {
        return app.getBean(IntakeService.class).submit(who(actor),tenant,null,null,
                new ReportDraft(kind,text,"en","UTC",null,null,latitude,longitude,uncertainty,null,null),
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
        if (!expected.endsWith("_UNAVAILABLE")) verifyCalibration(query,search,positive,negative,expected);
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
        try(var db=admin();var age=db.prepareStatement("UPDATE dzoka.match_search_runs SET created_at=clock_timestamp()-interval '25 hours' WHERE tenant_id=? AND client_id=?")) {
            age.setObject(1,A);age.setObject(2,runId);assertEquals(1,age.executeUpdate());
        }
        var expired=api("POST",A,prune,"history-maintainer",null);
        assertEquals(200,expired.statusCode(),expired.body());
        assertEquals(1,json.readTree(expired.body()).get("removed").asInt());
        String live=path(query)+"/runs/"+UUID.randomUUID();
        assertEquals(200,api("PUT",A,live,"matcher",request).statusCode());
        assertEquals(200,api("PUT",A,live,"second-matcher",request.replace("10}","9}")).statusCode());
        assertEquals(200,api("PUT",A,live,"matcher",request).statusCode());
        UUID lexical=submit(A,"other",ReportDraft.Kind.FOUND,"blue backpack",null,null,null);
        UUID context=submit(A,"other",ReportDraft.Kind.FOUND,"thermos");
        addUnindexedReports(positive);
        var overflow=api("GET",A,search,"matcher",null);
        assertEquals(200,overflow.statusCode(),overflow.body());
        var result=json.readTree(overflow.body());
        assertTrue(result.get("partial").asBoolean());
        assertEquals("APPROXIMATE",result.get("routes").get(1).get("state").asString(),overflow.body());
        assertEquals("SPATIAL_TIME_INDEX",result.get("routes").get(2).get("reason").asString(),overflow.body());
        assertEquals(expected,result.get("routes").get(3).get("reason").asString(),overflow.body());
        var found=new java.util.HashMap<String,JsonNode>();
        result.get("candidates").forEach(candidate -> found.put(candidate.get("report").asString(),candidate.get("ranks")));
        assertEquals(java.util.Set.of(positive.toString(),lexical.toString(),context.toString()),found.keySet());
        assertEquals(1,found.get(lexical.toString()).size());
        assertTrue(found.get(lexical.toString()).has("LEXICAL"));
        assertEquals(1,found.get(context.toString()).size());
        assertTrue(found.get(context.toString()).has("SPATIAL_TIME"));
        verifyCategoryScope();
        verifyTemporalScope();
        verifyHistoryAccounting(query);
    }

    private void verifyHistoryAccounting(UUID activeReport) throws Exception {
        UUID stale=submit(A,"reporter",ReportDraft.Kind.LOST,"initial report");
        UUID remembered;
        try(var db=admin();var seed=db.prepareStatement("""
                WITH rows AS (
                  INSERT INTO dzoka.match_search_runs
                    (tenant_id,id,client_id,actor_ref,report_id,revision,requested_limit,candidate_count,summary)
                  SELECT ?,gen_random_uuid(),gen_random_uuid(),?,?,1,1,0,'{}'::jsonb FROM generate_series(1,100)
                  RETURNING tenant_id,id,actor_ref,client_id
                ), tracked AS (
                  INSERT INTO dzoka.match_search_quota(tenant_id,id,actor_ref) SELECT tenant_id,id,actor_ref FROM rows
                ) SELECT client_id FROM rows LIMIT 1
                """)) {
            seed.setObject(1,A);seed.setString(2,who("quota-matcher").reference());seed.setObject(3,stale);
            try(var ids=seed.executeQuery()) { assertTrue(ids.next());remembered=ids.getObject(1,UUID.class); }
        }
        app.getBean(IntakeService.class).submit(who("reporter"),A,stale,1,
                new ReportDraft(ReportDraft.Kind.LOST,"superseding report","en","UTC",null,null,null,null,null,null,null),
                "{}".getBytes(java.nio.charset.StandardCharsets.UTF_8),UUID.randomUUID().toString());
        assertEquals(404,api("GET",A,path(stale)+"/runs/"+remembered,"quota-matcher",null).statusCode());
        String run=path(activeReport)+"/runs/"+UUID.randomUUID();
        assertEquals(409,api("PUT",A,run,"quota-matcher","{\"limit\":2}").statusCode());
        try(var db=admin();var expire=db.prepareStatement("""
                UPDATE dzoka.match_search_runs SET created_at=now()-interval '25 hours' WHERE tenant_id=? AND client_id=?
                """)) {
            expire.setObject(1,A);expire.setObject(2,remembered);assertEquals(1,expire.executeUpdate());
        }
        assertEquals(200,api("PUT",A,run,"quota-matcher","{\"limit\":2}").statusCode());
    }

    private void verifyTemporalScope() throws Exception {
        var when=java.time.Instant.parse("2026-10-01T10:15:00.453Z");
        UUID source=submitTimed(ReportDraft.Kind.LOST,when);
        UUID close=submitTimed(ReportDraft.Kind.FOUND,when.plusSeconds(22*3600));
        UUID far=submitTimed(ReportDraft.Kind.FOUND,when.plusSeconds(24*3600));
        var response=api("GET",A,path(source)+"/candidates","matcher",null);
        assertEquals(200,response.statusCode(),response.body());
        var result=json.readTree(response.body());
        assertEquals("SPATIAL_TIME_INDEX",result.get("routes").get(2).get("reason").asString());
        assertEquals(1,result.get("candidates").size());
        assertEquals(close.toString(),result.get("candidates").get(0).get("report").asString());
        assertFalse(response.body().contains(far.toString()));
    }

    private UUID submitTimed(ReportDraft.Kind kind,java.time.Instant when) {
        return app.getBean(IntakeService.class).submit(who("reporter"),A,null,null,
                new ReportDraft(kind,null,"en","UTC",null,null,null,null,null,when,when),
                "{}".getBytes(java.nio.charset.StandardCharsets.UTF_8),UUID.randomUUID().toString()).reportId();
    }

    private void verifyCategoryScope() throws Exception {
        UUID source=submitCategorized(A,"reporter",ReportDraft.Kind.LOST,"green folding umbrella","UMBRELLA");
        UUID relevant=submitCategorized(A,"other",ReportDraft.Kind.FOUND,"green folding umbrella","UMBRELLA");
        UUID sameCategory=submitCategorized(A,"other",ReportDraft.Kind.FOUND,null,"UMBRELLA");
        UUID wrongCategory=submitCategorized(A,"other",ReportDraft.Kind.FOUND,"green folding umbrella","BAG");
        UUID anotherTenant=submitCategorized(B,"remote-reporter",ReportDraft.Kind.FOUND,"green folding umbrella","UMBRELLA");
        String endpoint=path(source)+"/candidates?category=UMBRELLA";
        var filtered=api("GET",A,endpoint,"matcher",null);
        assertEquals(200,filtered.statusCode(),filtered.body());
        var rows=json.readTree(filtered.body()).get("candidates");
        assertEquals(2,rows.size(),filtered.body());
        assertEquals(relevant.toString(),rows.get(0).get("report").asString());
        assertEquals(sameCategory.toString(),rows.get(1).get("report").asString());
        assertEquals("INSUFFICIENT_EVIDENCE",rows.get(1).get("score").get("disposition").asString());
        assertFalse(filtered.body().contains(wrongCategory.toString()));
        assertFalse(filtered.body().contains(anotherTenant.toString()));
        assertEquals(400,api("GET",A,endpoint.replace("UMBRELLA","Umbrella"),"matcher",null).statusCode());
        addUnindexedReports(relevant,"UMBRELLA");
        var overflow=api("GET",A,endpoint,"matcher",null);
        assertEquals(200,overflow.statusCode(),overflow.body());
        var result=json.readTree(overflow.body());
        assertEquals(relevant.toString(),result.get("candidates").get(0).get("report").asString());
        assertEquals("CATEGORY_INDEX_ROUTE_LIMIT",result.get("routes").get(4).get("reason").asString());
        assertFalse(overflow.body().contains(wrongCategory.toString()));
        var run=path(source)+"/runs/"+UUID.randomUUID();
        assertEquals(200,api("PUT",A,run,"matcher","{\"limit\":3,\"category\":\"UMBRELLA\"}").statusCode());
        assertEquals(409,api("PUT",A,run,"matcher","{\"limit\":3,\"category\":\"BAG\"}").statusCode());
    }

    private UUID submitCategorized(UUID tenant,String actor,ReportDraft.Kind kind,String text,String category) {
        return app.getBean(IntakeService.class).submit(who(actor),tenant,null,null,
                new ReportDraft(kind,text,"en","UTC",null,null,null,null,null,null,null,category),
                "{}".getBytes(java.nio.charset.StandardCharsets.UTF_8),UUID.randomUUID().toString()).reportId();
    }

    private void verifyCalibration(UUID query,String search,UUID positive,UUID negative,String backend) throws Exception {
        String path="/internal/matching/calibrations/"+UUID.randomUUID();
        var draft=Map.ofEntries(Map.entry("spaceId",primary),Map.entry("spaceKey",profile.spaceKey()),
                Map.entry("activationVersion",1),Map.entry("sourceKind","LOST"),Map.entry("scoringVersion","bounded-evidence-v1"),
                Map.entry("vectorBackend",backend),
                Map.entry("planSha256","e".repeat(64)),Map.entry("datasetSha256","f".repeat(64)),
                Map.entry("minimumCoverage",0.25),Map.entry("holdoutCount",20),Map.entry("supportedHoldout",20),
                Map.entry("holdoutBrier",0.12),Map.entry("rawScoreBrierOnSameRows",0.20),
                Map.entry("bins",java.util.Collections.nCopies(5,Map.of("fitCount",10,"observedFrequency",0.84))));
        String body=json.writeValueAsString(draft);
        assertEquals(403,api("PUT",A,path,"matcher",body).statusCode());
        var proposal=api("PUT",A,path,"calibration-author",body);
        assertEquals(204,proposal.statusCode(),proposal.body());
        assertEquals(204,api("PUT",A,path,"calibration-author",body).statusCode());
        assertEquals(404,api("GET",B,path,"calibration-reviewer",null).statusCode());
        assertEquals(200,api("GET",A,path,"calibration-reviewer",null).statusCode());
        assertTrue(json.readTree(api("GET",A,search,"matcher",null).body()).get("candidates").get(0).get("confidence").isNull());
        String approval=json.writeValueAsString(Map.of("reviewSha256","d".repeat(64),"maximumBrier",0.14,
                "minimumBinSupport",3,"validHours",1));
        assertEquals(403,api("PUT",A,path+"/approval","calibration-author",approval).statusCode());
        assertEquals(409,api("PUT",A,path+"/approval","calibration-reviewer",approval.replace("0.14","0.11")).statusCode());
        var approved=api("PUT",A,path+"/approval","calibration-reviewer",approval);
        assertEquals(204,approved.statusCode(),approved.body());
        assertEquals(204,api("PUT",A,path+"/approval","calibration-reviewer",approval).statusCode());
        String run=path(query)+"/runs/"+UUID.randomUUID();
        var recorded=api("PUT",A,run,"matcher","{\"spaceId\":\""+primary+"\",\"limit\":10}");
        assertEquals(200,recorded.statusCode(),recorded.body());
        var candidates=json.readTree(recorded.body()).get("candidates");
        assertEquals(positive.toString(),candidates.get(0).get("report").asString());
        assertEquals(0.84,candidates.get(0).get("confidence").get("probability").asDouble());
        assertEquals(negative.toString(),candidates.get(1).get("report").asString());
        assertTrue(candidates.get(1).get("confidence").isNull());
        assertEquals(json.readTree(recorded.body()),json.readTree(api("GET",A,run,"matcher",null).body()));
        assertTrue(json.readTree(api("GET",A,search.replace(primary.toString(),alternate.toString()),"matcher",null)
                .body()).get("candidates").get(0).get("confidence").isNull());
        String revocation="{\"reviewSha256\":\""+"c".repeat(64)+"\"}";
        assertEquals(403,api("PUT",A,path+"/revocation","matcher",revocation).statusCode());
        assertEquals(204,api("PUT",A,path+"/revocation","calibration-reviewer",revocation).statusCode());
        assertTrue(json.readTree(api("GET",A,search,"matcher",null).body()).get("candidates").get(0).get("confidence").isNull());
        assertTrue(json.readTree(api("GET",A,run,"matcher",null).body()).get("candidates").get(0).get("confidence").isNull());
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
        addUnindexedReports(template,null);
    }

    private void addUnindexedReports(UUID template,String category) throws Exception {
        try(var db=admin();var sql=db.prepareStatement("""
                WITH new_reports AS (
                  INSERT INTO dzoka.reports(tenant_id,id,case_id,item_id,owner_ref,kind,current_revision,expires_at)
                  SELECT r.tenant_id,gen_random_uuid(),r.case_id,r.item_id,r.owner_ref,r.kind,1,r.expires_at
                  FROM dzoka.reports r CROSS JOIN generate_series(1,513) WHERE r.tenant_id=? AND r.id=?
                  RETURNING tenant_id,id,owner_ref
                ) INSERT INTO dzoka.report_revisions(tenant_id,report_id,revision,raw_request,raw_sha256,
                                                      normalization_version,language,timezone,actor_ref,category)
                SELECT tenant_id,id,1,''::bytea,repeat('a',64),'fixture','en','UTC',owner_ref,? FROM new_reports
                """)) {
            sql.setObject(1,A);sql.setObject(2,template);sql.setString(3,category);assertEquals(513,sql.executeUpdate());
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
