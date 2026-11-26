package org.dzoka;

import static org.junit.jupiter.api.Assertions.*;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.foundation.persistence.TenantTransactions;
import org.dzoka.intake.application.IntakeService;
import org.dzoka.intake.domain.ReportDraft;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class RecoveryIT extends IntegrationFixture {
    private static final String PRIVATE="angular violet enamel";
    private static final String PUBLIC="Green striped travel pouch";
    private final ObjectMapper json=new ObjectMapper();
    private UUID tenant;

    @Override protected List<String> extraOptions() {
        byte[] bytes=new byte[32];
        new java.security.SecureRandom().nextBytes(bytes);
        return List.of("--dzoka.recovery.hmac-key="+java.util.HexFormat.of().formatHex(bytes));
    }

    @Override protected void initializeProviders() throws Exception {
        membership(B,"owner","claims:submit");
    }

    @BeforeEach void createTenant() throws Exception {
        tenant=UUID.randomUUID();
        try (var db=admin();var sql=db.prepareStatement("INSERT INTO dzoka.tenants(id,name) VALUES (?,'Synthetic recovery')")) {
            sql.setObject(1,tenant);
            sql.executeUpdate();
        }
        membership(tenant,"registrar","reports:write","reports:hold","recovery:manage");
        membership(tenant,"observer","reports:read:any","recovery:verify","recovery:review");
        membership(tenant,"first-reviewer","recovery:review");
        membership(tenant,"second-reviewer","recovery:review");
        membership(tenant,"owner","claims:submit","recovery:review");
        membership(tenant,"other","claims:submit");
        membership(tenant,"guesser","claims:submit");
        membership(tenant,"desk","recovery:handover");
        membership(tenant,"wrong-desk","recovery:handover");
    }

    @Test void competingClaimsIndependentReviewAndConcurrentPickupCommitOnePrivateReturn() throws Exception {
        policy(1,true,true);
        var item=enroll();
        String owner=claim(item,"owner","REPRESENTATIVE");
        String other=claim(item,"other","OWNER");
        assertEquals(404,api("GET",tenant,owner,"other",null).statusCode());
        assertEquals(404,api("GET",B,owner,"owner",null).statusCode());
        assertEquals(403,api("GET",tenant,"/reports/"+item.report(),"owner",null).statusCode());
        var transactions=app.getBean(TenantTransactions.class);
        assertEquals(0,transactions.execute(identity("owner"),B,"claims:submit",db -> {
            try(var sql=db.prepareStatement("SELECT count(*) FROM dzoka.recovery_report_status(?)")) {
                sql.setObject(1,item.id());
                try(var rows=sql.executeQuery()) { rows.next();return rows.getInt(1); }
            }
        }).intValue());
        assertEquals(403,api("GET",tenant,owner+"/review-context","owner",null).statusCode());
        assertEquals(identity("owner").reference(),ok(api("GET",tenant,owner+"/review-context","first-reviewer",null))
                .get("claimantRef").asString());
        UUID fact=UUID.randomUUID();
        assertEquals(400,post(item.path()+"/facts/"+fact,"observer",fact(PUBLIC,true)).statusCode());
        assertEquals(400,post(item.path()+"/facts/"+fact,"observer",fact(PRIVATE,false)).statusCode());
        assertEquals(403,post(item.path()+"/facts/"+fact,"other",fact(PRIVATE,true)).statusCode());
        assertEquals(204,post(item.path()+"/facts/"+fact,"observer",fact(PRIVATE,true)).statusCode());
        assertEquals(204,post(item.path()+"/facts/"+fact,"observer",fact(PRIVATE,true)).statusCode());
        UUID session=pass(owner,"owner");
        UUID competingSession=pass(other,"other");
        var review=review(session,"APPROVE","INDEPENDENT_EVIDENCE",'b','e');
        assertEquals(403,post(owner+"/reviews/"+UUID.randomUUID(),"owner",review).statusCode());
        assertEquals(403,post(owner+"/reviews/"+UUID.randomUUID(),"observer",review).statusCode());
        try (var executor=Executors.newFixedThreadPool(2)) {
            var left=executor.submit(() -> post(owner+"/reviews/"+UUID.randomUUID(),"first-reviewer",review));
            var right=executor.submit(() -> post(other+"/reviews/"+UUID.randomUUID(),"second-reviewer",
                    review(competingSession,"APPROVE","INDEPENDENT_EVIDENCE",'c',null)));
            assertEquals(409,left.get(25,TimeUnit.SECONDS).statusCode());
            assertEquals(409,right.get(25,TimeUnit.SECONDS).statusCode());
        }
        expectState("WITHDRAWN",post(other+"/withdrawal","other",null));
        assertEquals(409,post(owner+"/reviews/"+UUID.randomUUID(),"first-reviewer",
                review(session,"APPROVE","INDEPENDENT_EVIDENCE",'b',null)).statusCode());
        expectState("PENDING",post(owner+"/reviews/"+UUID.randomUUID(),"first-reviewer",review));
        expectState("PENDING",post(owner+"/reviews/"+UUID.randomUUID(),"second-reviewer",review));
        expectState("APPROVED",post(owner+"/reviews/"+UUID.randomUUID(),"second-reviewer",
                review(session,"APPROVE","INDEPENDENT_EVIDENCE",'c','e')));
        UUID grant=UUID.randomUUID();
        var grantResponse=post(owner+"/grants/"+grant,"first-reviewer",audience("desk"));
        var grantBody=ok(grantResponse);
        assertTrue(grantResponse.headers().firstValue("cache-control").orElseThrow().contains("no-store"));
        assertEquals(grantBody,ok(post(owner+"/grants/"+grant,"first-reviewer",audience("desk"))));
        String token=grantBody.get("token").asString();
        String context=item.path()+"/grants/"+grant+"/pickup-context";
        String tokenBody=json.writeValueAsString(Map.of("token",token));
        assertEquals(409,api("POST",tenant,context,"wrong-desk",tokenBody).statusCode());
        var preview=api("POST",tenant,context,"desk",tokenBody);
        assertEquals(identity("owner").reference(),ok(preview).get("recipientRef").asString());
        assertFalse(preview.body().contains(token));
        var request=presentation(grant,token,"owner",'d');
        assertEquals(409,post(item.path()+"/returns/"+UUID.randomUUID(),"desk",presentation(grant,token,"other",'d')).statusCode());
        assertEquals(403,post(item.path()+"/returns/"+UUID.randomUUID(),"owner",request).statusCode());
        assertEquals(409,post(item.path()+"/returns/"+UUID.randomUUID(),"wrong-desk",request).statusCode());
        try (var executor=Executors.newFixedThreadPool(2)) {
            String pickup=item.path()+"/returns/"+UUID.randomUUID();
            String concurrent=item.path()+"/returns/"+UUID.randomUUID();
            var first=executor.submit(() -> post(pickup,"desk",request));
            var second=executor.submit(() -> post(concurrent,"desk",request));
            var left=first.get(25,TimeUnit.SECONDS);
            var right=second.get(25,TimeUnit.SECONDS);
            assertEquals(java.util.Set.of(200,409),java.util.Set.of(left.statusCode(),right.statusCode()));
            var winner=left.statusCode()==200?left:right;
            var winnerPath=left.statusCode()==200?pickup:concurrent;
            assertEquals(ok(winner),ok(post(winnerPath,"desk",request)));
            assertEquals(409,post(winnerPath,"desk",presentation(grant,token,"owner",'f')).statusCode());
            assertFalse(winner.body().contains(token));
            assertFalse(winner.body().contains(PRIVATE));
            for (int i=0;i<21;i++) {
                assertEquals(409,post(item.path()+"/returns/"+UUID.randomUUID(),"desk",
                        presentation(grant,"A".repeat(43),"owner",'d')).statusCode());
            }
            assertEquals(ok(winner),ok(post(winnerPath,"desk",request)));
        }
        expectState("RETURNED",api("GET",tenant,owner,"owner",null));
        assertEquals(1,count("recovery_returns",tenant));
        assertEquals(1,count("channel_alerts",tenant));
        assertEquals(1,countRecoveryOutbox(item.report()));
        try (var db=admin();var sql=db.prepareStatement("""
                SELECT count(*) FROM dzoka.recovery_activity WHERE tenant_id=? AND event='REDEEM_THROTTLED'
                """)) {
            sql.setObject(1,tenant);
            try (var rows=sql.executeQuery()) { assertTrue(rows.next());assertTrue(rows.getLong(1)>0); }
        }
        try (var db=admin(); var sql=db.prepareStatement("""
                SELECT action::text FROM (
                  SELECT to_jsonb(a) AS action FROM dzoka.recovery_activity a WHERE tenant_id=?
                  UNION ALL SELECT to_jsonb(e) FROM dzoka.outbox e WHERE tenant_id=?
                  UNION ALL SELECT to_jsonb(f) FROM dzoka.private_recovery_facts f WHERE tenant_id=?
                  UNION ALL SELECT to_jsonb(g) FROM dzoka.handover_grants g WHERE tenant_id=?
                ) rows
                """)) {
            for (int i=1;i<=4;i++) sql.setObject(i,tenant);
            try (var rows=sql.executeQuery()) {
                while (rows.next()) {
                    String stored=rows.getString(1);
                    assertFalse(stored.contains(PRIVATE) || stored.contains("angularvioletenamel") || stored.contains(token));
                }
            }
        }
    }

    @Test void disclosedFactsCannotValidateExistingSessionsAndItemBudgetSpansActors() throws Exception {
        policy(1,false,false);
        var item=enroll();
        String owner=claim(item,"owner","OWNER");
        assertEquals("INCONCLUSIVE",ok(post(owner+"/sessions/"+UUID.randomUUID(),"owner",null)).get("status").asString());
        var disclosed=UUID.randomUUID();
        assertEquals(204,post(item.path()+"/facts/"+disclosed,"observer",fact(PRIVATE,true)).statusCode());
        UUID earlier=challenge(owner,"owner");
        String withdrawal=item.path()+"/facts/"+disclosed+"/withdrawal";
        assertEquals(403,post(withdrawal,"owner",Map.of("reason","DISCLOSED")).statusCode());
        assertEquals(204,post(withdrawal,"observer",Map.of("reason","DISCLOSED")).statusCode());
        assertEquals(404,post(answerPath(owner,earlier),"owner",Map.of("answer",PRIVATE)).statusCode());
        assertEquals("INCONCLUSIVE",ok(post(owner+"/sessions/"+UUID.randomUUID(),"owner",null)).get("status").asString());
        assertEquals(409,post(item.path()+"/facts/"+UUID.randomUUID(),"observer",fact(PRIVATE,true)).statusCode());
        assertEquals(204,post(item.path()+"/facts/"+UUID.randomUUID(),"observer",fact("spiral copper clasp",true)).statusCode());
        for (String actor:List.of("owner","other","guesser")) {
            String subject=actor.equals("owner")?owner:claim(item,actor,"OWNER");
            for (int i=0;i<(actor.equals("guesser")?2:3);i++) {
                var session=challenge(subject,actor);
                expectOutcome("FAILED",post(answerPath(subject,session),actor,Map.of("answer","wrong hidden mark")));
            }
        }
        membership(tenant,"new-claimant","claims:submit");
        String blocked=claim(item,"new-claimant","OWNER");
        assertEquals(409,post(blocked+"/sessions/"+UUID.randomUUID(),"new-claimant",null).statusCode());
        var risk=ok(api("GET",tenant,blocked+"/review-context","first-reviewer",null)).get("risk");
        assertEquals(8,risk.get("itemFailures").asInt());
        assertEquals(3,risk.get("competingClaims").asInt());
        assertTrue(risk.get("score").asInt()>0);
        expectState("REJECTED",post(blocked+"/reviews/"+UUID.randomUUID(),"first-reviewer",
                review(null,"REJECT","FRAUD",'f',null)));
    }

    @Test void budgetsAndStaleSnapshotsFailClosedAfterHoldsPolicyAndExpiry() throws Exception {
        policy(1,false,false);
        var first=enroll();
        attest(first);
        String guess=claim(first,"guesser","OWNER");
        for (int i=0;i<3;i++) {
            var session=challenge(guess,"guesser");
            expectOutcome("FAILED",post(answerPath(guess,session),"guesser",Map.of("answer","invented answer")));
            expectOutcome("FAILED",post(answerPath(guess,session),"guesser",Map.of("answer",PRIVATE)));
        }
        assertEquals(409,post(guess+"/sessions/"+UUID.randomUUID(),"guesser",null).statusCode());
        var second=enroll();
        attest(second);
        String elsewhere=claim(second,"guesser","OWNER");
        assertEquals(409,post(elsewhere+"/sessions/"+UUID.randomUUID(),"guesser",null).statusCode());
        expectState("WITHDRAWN",post(elsewhere+"/withdrawal","guesser",null));
        String claim=claim(second,"other","OWNER");
        UUID assessment=pass(claim,"other");
        assertEquals(200,api("PUT",tenant,"/reports/"+second.report()+"/hold","registrar","{\"held\":true}").statusCode());
        assertEquals(200,api("PUT",tenant,"/reports/"+second.report()+"/hold","registrar","{\"held\":false}").statusCode());
        assertEquals(409,post(claim+"/reviews/"+UUID.randomUUID(),"first-reviewer",
                review(assessment,"APPROVE","INDEPENDENT_EVIDENCE",'a',null)).statusCode());
        UUID current=pass(claim,"other");
        expectState("APPROVED",post(claim+"/reviews/"+UUID.randomUUID(),"first-reviewer",
                review(current,"APPROVE","INDEPENDENT_EVIDENCE",'a',null)));
        UUID grant=UUID.randomUUID();
        String token=ok(post(claim+"/grants/"+grant,"first-reviewer",audience("desk"))).get("token").asString();
        assertEquals(204,post(second.path()+"/hold","registrar",Map.of("blocked",true)).statusCode());
        assertEquals(409,post(second.path()+"/returns/"+UUID.randomUUID(),"desk",presentation(grant,token,"other",'d')).statusCode());
        assertEquals(204,post(second.path()+"/hold","registrar",Map.of("blocked",false)).statusCode());
        assertEquals(409,post(second.path()+"/returns/"+UUID.randomUUID(),"desk",presentation(grant,token,"other",'d')).statusCode());
        policy(2,true,false);
        assertEquals(409,post(second.path()+"/returns/"+UUID.randomUUID(),"desk",
                presentation(grant,token,"other",'d')).statusCode());
        expectState("REVOKED",post(claim+"/revocation","registrar",Map.of("reason","FRAUD")));
        var third=enroll();
        attest(third);
        String alternate=claim(third,"other","OWNER");
        UUID newest=pass(alternate,"other");
        expectState("PENDING",post(alternate+"/reviews/"+UUID.randomUUID(),"first-reviewer",
                review(newest,"APPROVE","INDEPENDENT_EVIDENCE",'a',null)));
        expectState("APPROVED",post(alternate+"/reviews/"+UUID.randomUUID(),"second-reviewer",
                review(newest,"APPROVE","INDEPENDENT_EVIDENCE",'b',null)));
        UUID expired=UUID.randomUUID();
        String expiredToken=ok(post(alternate+"/grants/"+expired,"first-reviewer",audience("desk"))).get("token").asString();
        try (var db=admin();var sql=db.prepareStatement("""
                UPDATE dzoka.handover_grants SET expires_at=clock_timestamp()-interval '1 second' WHERE tenant_id=? AND id=?
                """)) {
            sql.setObject(1,tenant);sql.setObject(2,expired);assertEquals(1,sql.executeUpdate());
        }
        assertEquals(409,post(third.path()+"/returns/"+UUID.randomUUID(),"desk",
                presentation(expired,expiredToken,"other",'a')).statusCode());
    }

    private record Item(UUID id,UUID report,String path) {}
    private Item enroll() throws Exception {
        var report=app.getBean(IntakeService.class).submit(identity("registrar"),tenant,null,null,
                new ReportDraft(ReportDraft.Kind.FOUND,PUBLIC,"en","UTC",null,null,null,null,null,null,null),
                "{}".getBytes(java.nio.charset.StandardCharsets.UTF_8),UUID.randomUUID().toString());
        UUID id=UUID.randomUUID();
        String path="/recovery/intakes/"+id;
        var response=post(path,"registrar",Map.of("foundReport",report.reportId(),"site",UUID.randomUUID()));
        assertEquals(204,response.statusCode(),response.body());
        return new Item(id,report.reportId(),path);
    }
    private void policy(int version,boolean dual,boolean representatives) throws Exception {
        ok(post("/recovery/policies/"+version,"registrar",Map.of("dualControl",dual,"allowRepresentatives",representatives)));
    }
    private void attest(Item item) throws Exception {
        var r=post(item.path()+"/facts/"+UUID.randomUUID(),"observer",fact(PRIVATE,true));
        assertEquals(204,r.statusCode(),r.body());
    }
    private String claim(Item item,String actor,String capacity) throws Exception {
        String path=item.path()+"/claims/"+UUID.randomUUID();
        expectState("PENDING",post(path,actor,Map.of("capacity",capacity)));
        return path;
    }
    private UUID challenge(String claim,String actor) throws Exception {
        UUID session=UUID.randomUUID();
        var response=post(claim+"/sessions/"+session,actor,null);
        var data=ok(response);
        assertFalse(response.body().contains(PRIVATE) || response.body().contains("answer_mac"));
        assertEquals(session.toString(),data.get("id").asString());
        return session;
    }
    private UUID pass(String claim,String actor) throws Exception {
        UUID session=challenge(claim,actor);
        expectOutcome("PASSED",post(answerPath(claim,session),actor,Map.of("answer","Angular VIOLET enamel!")));
        return session;
    }
    private long countRecoveryOutbox(UUID report) throws Exception {
        try (var db=admin();var sql=db.prepareStatement("""
                SELECT count(*) FROM dzoka.outbox WHERE tenant_id=? AND report_id=? AND event_type='recovery.returned'
                """)) {
            sql.setObject(1,tenant);sql.setObject(2,report);
            try (var rows=sql.executeQuery()) { rows.next();return rows.getLong(1); }
        }
    }
    private Map<String,String> audience(String actor) { return Map.of("audienceRef",identity(actor).reference()); }
    private Map<String,Object> presentation(UUID grant,String token,String recipient,char proof) {
        return Map.of("grant",grant,"token",token,"recipientRef",identity(recipient).reference(),"confirmationSha",digest(proof));
    }
    private static Map<String,Object> fact(String answer,boolean confirmed) {
        return Map.of("kind","HIDDEN_MARK","sourceSha",digest('b'),"answer",answer,"privateConfirmed",confirmed);
    }
    private static Map<String,Object> review(UUID session,String decision,String reason,char evidence,Character entitlement) {
        var values=new java.util.LinkedHashMap<String,Object>();
        values.put("session",session);values.put("decision",decision);values.put("reason",reason);
        values.put("evidenceSha",digest(evidence));values.put("entitlementSha",entitlement==null?null:digest(entitlement));
        return values;
    }
    private static String answerPath(String claim,UUID session) { return claim+"/sessions/"+session+"/answer"; }
    private static String digest(char letter) { return String.valueOf(letter).repeat(64); }
    private VerifiedIdentity identity(String actor) { return new VerifiedIdentity(issuer,actor); }
    private HttpResponse<String> post(String path,String actor,Object body) throws Exception {
        return api("PUT",tenant,path,actor,body==null?null:json.writeValueAsString(body));
    }
    private JsonNode ok(HttpResponse<String> response) {
        assertEquals(200,response.statusCode(),response.body());
        return json.readTree(response.body());
    }
    private void expectState(String state,HttpResponse<String> response) { assertEquals(state,ok(response).get("state").asString()); }
    private void expectOutcome(String outcome,HttpResponse<String> response) { assertEquals(outcome,ok(response).get("outcome").asString()); }
}
