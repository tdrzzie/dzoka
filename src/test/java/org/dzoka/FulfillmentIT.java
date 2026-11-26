package org.dzoka;

import static org.junit.jupiter.api.Assertions.*;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.dzoka.foundation.application.*;
import org.dzoka.foundation.persistence.TenantTransactions;
import org.dzoka.fulfillment.*;
import org.dzoka.logistics.Logistics;
import org.dzoka.rewards.Rewards;
import org.dzoka.passports.Passports;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.intake.application.IntakeService;
import org.dzoka.intake.domain.ReportDraft;
import org.dzoka.recovery.ports.RecoveryOperations;
import org.dzoka.recovery.domain.Recovery.*;
import org.junit.jupiter.api.*;
import tools.jackson.databind.ObjectMapper;

class FulfillmentIT extends IntegrationFixture {
    private final ObjectMapper json=new ObjectMapper();
    private HttpServer sandbox;
    private final byte[] key=new byte[32];
    private final String gatewayToken=UUID.randomUUID().toString();
    private final Map<UUID,Map<String,Object>> external=new ConcurrentHashMap<>();
    private final AtomicBoolean loseNextReply=new AtomicBoolean();
    private final AtomicInteger creates=new AtomicInteger(),queries=new AtomicInteger();
    private Logistics logistics;private Rewards rewards;private FulfillmentOperations operations;private RecoveryOperations recovery;
    private String ownerSubject;
    private record Item(UUID intake,UUID claim) {}
    private VerifiedIdentity who(String name){return new VerifiedIdentity(issuer,name.equals("owner") && ownerSubject!=null?ownerSubject:name);}
    private static String sha(String value){return ContentDigest.sha256(value.getBytes(StandardCharsets.UTF_8));}
    @Override protected void initializeProviders() throws Exception {
        membership(A,"custodian","reports:write","recovery:manage","logistics:manage","logistics:use");
        membership(A,"observer","recovery:verify","reports:read:any");membership(A,"reviewer","recovery:review","rewards:review","logistics:manage");
        membership(A,"owner","claims:submit","logistics:use","rewards:use","passports:use");
        membership(A,"finder","rewards:use","passports:use","logistics:use");
        membership(A,"dispatcher","fulfillment:process");membership(A,"desk","recovery:handover");
        membership(A,"privacy-worker","passports:maintain");
        membership(A,"analyst","intelligence:read","custody:operate");
        membership(B,"owner","logistics:use","rewards:use","passports:use");
        membership(B,"finder","passports:use");
        new java.security.SecureRandom().nextBytes(key);
        sandbox=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        sandbox.createContext("/v1/quotes",exchange -> {
            if(!("Bearer "+gatewayToken).equals(exchange.getRequestHeaders().getFirst("Authorization"))){exchange.sendResponseHeaders(403,-1);exchange.close();return;}
            exchange.getRequestBody().readAllBytes();
            byte[] body=json.writeValueAsBytes(Map.of("reference","quote-fixture","currency","USD","minorUnits",250));
            exchange.sendResponseHeaders(200,body.length);try(var out=exchange.getResponseBody()){out.write(body);}
        });
        sandbox.createContext("/v1/operations",exchange -> {
            if(!("Bearer "+gatewayToken).equals(exchange.getRequestHeaders().getFirst("Authorization"))){exchange.sendResponseHeaders(403,-1);exchange.close();return;}
            UUID id;UUID tenant=A;Map<String,Object> result;
            if(exchange.getRequestMethod().equals("POST")) {
                var request=json.readTree(exchange.getRequestBody().readAllBytes());
                id=UUID.fromString(request.path("operation").asString());tenant=UUID.fromString(request.path("tenant").asString());
                if(!id.toString().equals(exchange.getRequestHeaders().getFirst("Idempotency-Key")))throw new AssertionError();
                result=new HashMap<>();result.put("schema",1);result.put("tenant",tenant);result.put("operation",id);result.put("state","CONFIRMED");result.put("reference","receipt-"+id);
                if(external.putIfAbsent(id,result)==null)creates.incrementAndGet();
                result=external.get(id);
                if(loseNextReply.getAndSet(false)){exchange.sendResponseHeaders(503,-1);exchange.close();return;}
            } else {
                queries.incrementAndGet();id=UUID.fromString(exchange.getRequestURI().getPath().substring("/v1/operations/".length()));
                result=external.get(id);
                if(result==null){result=new HashMap<>();result.put("schema",1);result.put("tenant",tenant);result.put("operation",id);result.put("state","NOT_FOUND");result.put("reference",null);}
            }
            byte[] body=json.writeValueAsBytes(result);exchange.sendResponseHeaders(200,body.length);try(var out=exchange.getResponseBody()){out.write(body);}
        });
        sandbox.start();
    }
    @Override protected List<String> extraOptions(){return List.of("--DZOKA_FULFILLMENT_ORIGIN=http://127.0.0.1:"+sandbox.getAddress().getPort(),"--DZOKA_FULFILLMENT_TEST_LOOPBACK=true","--DZOKA_FULFILLMENT_TOKEN="+gatewayToken,
            "--DZOKA_FULFILLMENT_WEBHOOK_KEY="+HexFormat.of().formatHex(key),"--DZOKA_FULFILLMENT_TENANT="+A,"--DZOKA_FULFILLMENT_ISSUER="+issuer,"--DZOKA_FULFILLMENT_SUBJECT=dispatcher","--dzoka.recovery.hmac-key="+HexFormat.of().formatHex(key));}
    @BeforeEach void services() throws Exception {
        ownerSubject="owner-"+UUID.randomUUID();
        membership(A,ownerSubject,"claims:submit","logistics:use","rewards:use","passports:use");
        membership(B,ownerSubject,"logistics:use","rewards:use","passports:use");
        logistics=app.getBean(Logistics.class);rewards=app.getBean(Rewards.class);operations=app.getBean(FulfillmentOperations.class);recovery=app.getBean(RecoveryOperations.class);
    }
    @AfterAll void stopSandbox(){if(sandbox!=null)sandbox.stop(0);}
    private Item item() {
        recovery.policy(who("custodian"),A,1,false,false);
        UUID report=app.getBean(IntakeService.class).submit(who("custodian"),A,null,null,new ReportDraft(ReportDraft.Kind.FOUND,"Synthetic M10 travel container","en","UTC",null,null,null,null,null,null,null),new byte[]{'{','}'},UUID.randomUUID().toString()).reportId();
        UUID intake=UUID.randomUUID(),claim=UUID.randomUUID(),session=UUID.randomUUID();
        recovery.enroll(who("custodian"),A,intake,report,UUID.randomUUID());
        recovery.fact(who("observer"),A,intake,UUID.randomUUID(),FactKind.HIDDEN_MARK,sha("fixture-"+intake),"angular violet enamel",true);
        recovery.submit(who("owner"),A,intake,claim,Capacity.OWNER);
        recovery.challenge(who("owner"),A,intake,claim,session);
        assertEquals("PASSED",recovery.answer(who("owner"),A,intake,claim,session,"angular violet enamel").outcome());
        assertEquals("APPROVED",recovery.review(who("reviewer"),A,intake,claim,UUID.randomUUID(),new Review(session,Decision.APPROVE,Reason.INDEPENDENT_EVIDENCE,sha("review-"+intake),null)).state());
        return new Item(intake,claim);
    }
    private UUID shipment(Item item) {
        var quote=logistics.quote(who("owner"),A,"vault-fixture-route");UUID id=UUID.randomUUID();
        var request=new Logistics.Request(item.intake(),item.claim(),quote.id(),"vault-fixture-route",sha("consent-"+id));
        logistics.request(who("owner"),A,id,request);assertEquals("REQUESTED",logistics.request(who("owner"),A,id,request).state());return id;
    }
    private UUID operation(UUID entity,String kind){return UUID.nameUUIDFromBytes((A+":"+entity+":"+kind).getBytes(StandardCharsets.US_ASCII));}
    private void due(UUID entity) throws Exception {try(var db=admin();var q=db.prepareStatement("UPDATE dzoka.fulfillment_operations SET next_at=clock_timestamp()-interval '1 second',lease_until=NULL WHERE tenant_id=? AND entity_id=?")){q.setObject(1,A);q.setObject(2,entity);q.executeUpdate();}}
    private UUID reward(Item item) {
        UUID id=UUID.randomUUID();var terms=new Rewards.Terms(item.intake(),item.claim(),who("finder").reference(),"USD",1000,100,sha("terms-"+id),1);
        rewards.offer(who("owner"),A,id,terms);assertEquals("RESERVE_PENDING",rewards.offer(who("owner"),A,id,terms).state());return id;
    }
    private void deskReturn(Item item) {
        var grant=recovery.issue(who("reviewer"),A,item.intake(),item.claim(),UUID.randomUUID(),who("desk").reference());
        assertTrue(recovery.redeem(who("desk"),A,item.intake(),grant.id(),UUID.randomUUID(),grant.token(),who("owner").reference(),sha("receipt-"+item.intake())).accepted());
    }
    private FulfillmentOperations.Callback tracking(UUID shipment,String status,UUID event) {
        var op=operation(shipment,"COURIER_BOOK");
        return new FulfillmentOperations.Callback(A,event,op,"CONFIRMED","receipt-"+op,status,sha("pod-"+event),Instant.now());
    }
    private HttpResponse<String> callback(FulfillmentOperations.Callback callback,String timestamp,boolean valid) throws Exception {
        byte[] raw=json.writeValueAsBytes(callback);var mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key,"HmacSHA256"));mac.update((timestamp+"\n").getBytes(StandardCharsets.US_ASCII));
        String signature=valid?HexFormat.of().formatHex(mac.doFinal(raw)):"0".repeat(64);
        return http.send(HttpRequest.newBuilder(URI.create(base+"/api/v1/hooks/fulfillment")).header("Content-Type","application/json").header("X-Dzoka-Timestamp",timestamp).header("X-Dzoka-Signature",signature).POST(HttpRequest.BodyPublishers.ofByteArray(raw)).build(),HttpResponse.BodyHandlers.ofString());
    }
    @Test void lostBookingReplyReconcilesOnceAndDeliveryNeedsOwnerReceipt() throws Exception {
        Item item=item();UUID id=shipment(item);int before=creates.get();
        logistics.book(who("custodian"),A,id);loseNextReply.set(true);
        assertTrue(operations.dispatch(who("dispatcher"),A));assertEquals("BOOKING",logistics.read(who("owner"),A,id).state());
        due(id);int priorQueries=queries.get();assertTrue(operations.dispatch(who("dispatcher"),A));
        assertEquals("BOOKED",logistics.read(who("owner"),A,id).state());assertEquals(before+1,creates.get());assertTrue(queries.get()>priorQueries);
        assertThrows(Failures.NotFound.class,()->logistics.read(who("owner"),B,id));
        assertThrows(Failures.NotFound.class,()->logistics.read(who("finder"),A,id));
        logistics.handoff(who("custodian"),A,id,sha("courier"),sha("handoff"));
        assertEquals("DISPATCHED",logistics.handoff(who("custodian"),A,id,sha("courier"),sha("handoff")).state());
        var delivery=tracking(id,"DELIVERED",UUID.randomUUID());String now=Long.toString(Instant.now().getEpochSecond());
        assertEquals(200,callback(delivery,now,true).statusCode());assertEquals(200,callback(delivery,now,true).statusCode());
        assertEquals(200,callback(tracking(id,"IN_TRANSIT",UUID.randomUUID()),now,true).statusCode());
        assertEquals("DELIVERED",logistics.read(who("owner"),A,id).state());
        try(var db=admin();var q=db.prepareStatement("SELECT returned FROM dzoka.recovery_intakes WHERE tenant_id=? AND id=?")){q.setObject(1,A);q.setObject(2,item.intake());try(var row=q.executeQuery()){assertTrue(row.next());assertFalse(row.getBoolean(1));}}
        assertEquals("OWNER_CONFIRMED",logistics.confirm(who("owner"),A,id,sha("owner-receipt")).state());
        assertEquals("OWNER_CONFIRMED",logistics.confirm(who("owner"),A,id,sha("owner-receipt")).state());
        assertThrows(Failures.Conflict.class,()->logistics.confirm(who("owner"),A,id,sha("different")));
        var graph=app.getBean(org.dzoka.edge.RecoveryIntelligence.class).history(who("analyst"),A,item.intake(),Instant.now());
        assertEquals(Set.of("COURIER_DISPATCHED","OWNER_RETURN"),graph.stream().map(org.dzoka.edge.RecoveryIntelligence.Edge::kind).collect(java.util.stream.Collectors.toSet()));
        assertTrue(graph.stream().allMatch(e->e.classification().equals("OBSERVED")));
    }
    @Test void holdOrRevocationPreventsNewBookingAndPhysicalDispatch() throws Exception {
        Item item=item();UUID id=shipment(item);logistics.book(who("custodian"),A,id);
        recovery.hold(who("custodian"),A,item.intake(),true);int before=creates.get();
        operations.dispatch(who("dispatcher"),A);assertEquals(before,creates.get());assertEquals("FAILED",logistics.read(who("owner"),A,id).state());
        recovery.hold(who("custodian"),A,item.intake(),false);
        Item second=item();UUID next=shipment(second);logistics.book(who("custodian"),A,next);operations.dispatch(who("dispatcher"),A);
        recovery.revoke(who("custodian"),A,second.intake(),second.claim(),Reason.CONFLICT);
        assertThrows(Failures.Conflict.class,()->logistics.handoff(who("custodian"),A,next,sha("courier"),sha("handoff")));
    }
    @Test void immutableBalancedEscrowAllowsOneReleaseOrRefundUnderRace() throws Exception {
        Item item=item();UUID id=reward(item);operations.dispatch(who("dispatcher"),A);
        assertEquals("RESERVED",rewards.read(who("owner"),A,id).state());
        assertThrows(Failures.Conflict.class,()->rewards.settle(who("owner"),A,id,false));
        deskReturn(item);rewards.confirm(who("owner"),A,id,sha("recovered"));
        try(var executor=Executors.newFixedThreadPool(2)) {
            var release=executor.submit(()->{try{rewards.settle(who("owner"),A,id,false);return true;}catch(Failures.Conflict expected){return false;}});
            var refund=executor.submit(()->{try{rewards.settle(who("owner"),A,id,true);return true;}catch(Failures.Conflict expected){return false;}});
            assertNotEquals(release.get(10,TimeUnit.SECONDS),refund.get(10,TimeUnit.SECONDS));
        }
        operations.dispatch(who("dispatcher"),A);
        var entries=rewards.ledger(who("owner"),A,id);assertEquals(0,entries.stream().mapToLong(Rewards.Entry::amount).sum());
        assertEquals(0,entries.stream().filter(e->e.account().equals("ESCROW")).mapToLong(Rewards.Entry::amount).sum());
        assertTrue(Set.of("RELEASED","REFUNDED").contains(rewards.read(who("owner"),A,id).state()));
        var tx=app.getBean(TenantTransactions.class);
        assertThrows(Failures.Unavailable.class,()->tx.execute(who("dispatcher"),A,"fulfillment:process",db->Sql.write(db,"UPDATE dzoka.reward_transfers SET minor_units=1 WHERE reward_id=?",id)));
        assertThrows(Failures.NotFound.class,()->rewards.read(who("owner"),B,id));
    }
    @Test void disputeFreezesFundsAndRecoveryDoesNotDependOnReward() throws Exception {
        Item item=item();UUID id=reward(item);operations.dispatch(who("dispatcher"),A);
        rewards.dispute(who("finder"),A,id,sha("dispute"),false);
        assertThrows(Failures.Conflict.class,()->rewards.settle(who("owner"),A,id,true));
        deskReturn(item);rewards.dispute(who("reviewer"),A,id,sha("resolution"),true);
        rewards.settle(who("owner"),A,id,true);operations.dispatch(who("dispatcher"),A);
        assertEquals("REFUNDED",rewards.read(who("owner"),A,id).state());
    }
    @Test void callbackSignatureExpiryReplayAndChangedPayloadAreEnforced() throws Exception {
        UUID id=shipment(item());logistics.book(who("custodian"),A,id);operations.dispatch(who("dispatcher"),A);
        var c=tracking(id,null,UUID.randomUUID());String now=Long.toString(Instant.now().getEpochSecond());
        assertEquals(403,callback(c,now,false).statusCode());assertEquals(403,callback(c,Long.toString(Instant.now().minusSeconds(600).getEpochSecond()),true).statusCode());
        assertEquals(200,callback(c,now,true).statusCode());
        var changed=new FulfillmentOperations.Callback(c.tenant(),c.event(),c.operation(),c.state(),c.reference(),null,sha("changed"),c.occurredAt());
        assertEquals(409,callback(changed,now,true).statusCode());
    }
    @Test void registeredTagsKeepOwnerPrivateAndNeverCreateOwnershipEvidence() throws Exception {
        var passports=app.getBean(Passports.class);UUID item=UUID.randomUUID(),tag=UUID.randomUUID(),thread=UUID.randomUUID();
        passports.register(who("owner"),A,item,"Private registry description");
        byte[] random=new byte[32];new java.security.SecureRandom().nextBytes(random);String token=Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        passports.tag(who("owner"),A,item,tag,new Passports.Tag("QR",token));
        long claims=count("ownership_claims",A);
        var opened=passports.scan(who("finder"),A,thread,Passports.locator(A,token));
        assertTrue(opened.messages().isEmpty());assertTrue(passports.items(who("finder"),A).isEmpty());
        var response=api("GET",A,"/passports/threads/"+thread,"finder",null);
        assertEquals(200,response.statusCode());assertFalse(response.body().contains(who("owner").reference()));assertFalse(response.body().contains("Private registry"));
        passports.message(who("finder"),A,thread,UUID.randomUUID(),"FOUND_ITEM");
        assertEquals(1,passports.thread(who("owner"),A,thread).messages().size());
        assertTrue(passports.threads(who("owner"),A).stream().anyMatch(t->item.equals(t.registeredItem()) && t.privateDescription().equals("Private registry description")));
        assertNull(opened.registeredItem());assertNull(opened.privateDescription());
        assertEquals(400,api("PUT",A,"/passports/threads/"+thread+"/messages/"+UUID.randomUUID(),"finder","{\"phrase\":\"my phone number\"}").statusCode());
        assertEquals(claims,count("ownership_claims",A));
        passports.revokeTag(who("owner"),A,item,tag);
        assertThrows(Failures.NotFound.class,()->passports.thread(who("finder"),A,thread));
        assertThrows(Failures.NotFound.class,()->passports.scan(who("finder"),B,UUID.randomUUID(),token));
        assertThrows(Failures.NotFound.class,()->passports.scan(who("finder"),A,UUID.randomUUID(),token));
    }
    @Test void earlyTrackingSurvivesHandoffAndHeldReceiptNeedsIndependentReview() throws Exception {
        UUID id=shipment(item());logistics.book(who("custodian"),A,id);operations.dispatch(who("dispatcher"),A);
        var early=tracking(id,"DELIVERED",UUID.randomUUID());String now=Long.toString(Instant.now().getEpochSecond());
        assertEquals(200,callback(early,now,true).statusCode());assertEquals("BOOKED",logistics.read(who("owner"),A,id).state());
        var dispatched=logistics.handoff(who("custodian"),A,id,sha("courier"),sha("dispatch"));
        assertEquals("DELIVERED",dispatched.state());
        try(var db=admin();var query=db.prepareStatement("UPDATE dzoka.reports SET version=version+1 WHERE tenant_id=? AND id=(SELECT report_id FROM dzoka.recovery_intakes WHERE tenant_id=? AND id=?)")) {
            query.setObject(1,A);query.setObject(2,A);query.setObject(3,dispatched.intake());query.executeUpdate();
        }
        assertThrows(Failures.Conflict.class,()->logistics.confirm(who("owner"),A,id,sha("receipt")));
        logistics.reviewReceipt(who("reviewer"),A,id,UUID.randomUUID(),sha("current-report-review"));
        recovery.hold(who("custodian"),A,dispatched.intake(),true);
        assertThrows(Failures.Conflict.class,()->logistics.confirm(who("owner"),A,id,sha("receipt")));
        recovery.hold(who("custodian"),A,dispatched.intake(),false);
        assertThrows(Failures.Conflict.class,()->logistics.confirm(who("owner"),A,id,sha("receipt")));
        logistics.reviewReceipt(who("reviewer"),A,id,UUID.randomUUID(),sha("independent-receipt-review"));
        assertEquals("OWNER_CONFIRMED",logistics.confirm(who("owner"),A,id,sha("receipt")).state());
    }
    @Test void courierReturnToStaffDoesNotBecomeOwnerRecoveryOrRewardEligibility() throws Exception {
        Item item=item();UUID reward=reward(item);operations.dispatch(who("dispatcher"),A);
        UUID id=shipment(item);logistics.book(who("custodian"),A,id);operations.dispatch(who("dispatcher"),A);
        logistics.handoff(who("custodian"),A,id,sha("courier"),sha("dispatch"));
        assertEquals(200,callback(tracking(id,"RETURNED_TO_SENDER",UUID.randomUUID()),Long.toString(Instant.now().getEpochSecond()),true).statusCode());
        assertEquals("CANCELLED",logistics.staffReceipt(who("custodian"),A,id,sha("staff-receipt")).state());
        assertThrows(Failures.Conflict.class,()->rewards.confirm(who("owner"),A,reward,sha("not-owner-receipt")));
        assertEquals("PENDING",recovery.read(who("owner"),A,item.intake(),item.claim()).state());
        assertEquals(200,callback(tracking(id,"DELIVERED",UUID.randomUUID()),Long.toString(Instant.now().getEpochSecond()),true).statusCode());
        assertEquals("CANCELLED",logistics.read(who("owner"),A,id).state());
        rewards.settle(who("owner"),A,reward,true);operations.dispatch(who("dispatcher"),A);
    }
    @Test void unknownPaymentReconcilesWithoutInventingLedgerFundsAndCancelIsDurable() throws Exception {
        Item item=item();UUID id=reward(item);loseNextReply.set(true);operations.dispatch(who("dispatcher"),A);
        assertTrue(rewards.ledger(who("owner"),A,id).isEmpty());due(id);operations.dispatch(who("dispatcher"),A);
        assertEquals("RESERVED",rewards.read(who("owner"),A,id).state());assertEquals(2,rewards.ledger(who("owner"),A,id).size());
        UUID shipment=shipment(item);logistics.book(who("custodian"),A,shipment);operations.dispatch(who("dispatcher"),A);
        logistics.cancel(who("custodian"),A,shipment);operations.dispatch(who("dispatcher"),A);
        assertEquals("CANCELLED",logistics.read(who("owner"),A,shipment).state());
        assertThrows(Failures.Conflict.class,()->logistics.handoff(who("custodian"),A,shipment,sha("courier"),sha("dispatch")));
    }
    @Test void currencyDecimalsAndFailedTagGuessesCannotBypassBoundaryControls() throws Exception {
        Item item=item();String body=json.writeValueAsString(new Rewards.Terms(item.intake(),item.claim(),who("finder").reference(),"USD",1000,100,sha("terms"),1));
        body=body.replace("\"minorUnits\":1000","\"minorUnits\":1000.5");
        assertEquals(400,api("PUT",A,"/rewards/"+UUID.randomUUID(),ownerSubject,body).statusCode());
        var passports=app.getBean(Passports.class);String name="scan-"+UUID.randomUUID();membership(A,name,"passports:use");
        byte[] random=new byte[32];new java.security.SecureRandom().nextBytes(random);String missing=Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        for(int i=0;i<60;i++)assertThrows(Failures.NotFound.class,()->passports.scan(who(name),A,UUID.randomUUID(),missing));
        assertThrows(Failures.Conflict.class,()->passports.scan(who(name),A,UUID.randomUUID(),missing));
    }
    @Test void passportAssertionsStayPrivateAndExpiredRelayMessagesArePurged() throws Exception {
        var p=app.getBean(Passports.class);UUID item=UUID.randomUUID();p.register(who("owner"),A,item,"Private registration");
        assertTrue(p.passport(who("owner"),A,item,UUID.randomUUID(),new Passports.Identity("INTERNAL_UUID",item.toString(),sha("identity"))).verified());
        assertFalse(p.passport(who("owner"),A,item,UUID.randomUUID(),new Passports.Identity("ENTERPRISE_ASSET","inventory-42",sha("identity"))).verified());
        assertEquals(2,p.identities(who("owner"),A,item).size());
        assertThrows(Failures.NotFound.class,()->p.identities(who("finder"),A,item));
        assertThrows(IllegalArgumentException.class,()->p.passport(who("owner"),A,item,UUID.randomUUID(),new Passports.Identity("DID","did:example:unreviewed",sha("identity"))));
        byte[] random=new byte[32];new java.security.SecureRandom().nextBytes(random);String token=Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        p.tag(who("owner"),A,item,UUID.randomUUID(),new Passports.Tag("NFC",token));UUID thread=UUID.randomUUID();p.scan(who("finder"),A,thread,token);
        assertEquals(1,p.tags(who("owner"),A,item).size());
        p.message(who("finder"),A,thread,UUID.randomUUID(),"FOUND_ITEM");p.moderate(who("owner"),A,thread,true);
        assertThrows(Failures.Conflict.class,()->p.message(who("finder"),A,thread,UUID.randomUUID(),"THANK_YOU"));
        try(var db=admin();var q=db.prepareStatement("UPDATE dzoka.tag_threads SET expires_at=clock_timestamp()-interval '1 second' WHERE tenant_id=? AND id=?")){q.setObject(1,A);q.setObject(2,thread);q.executeUpdate();}
        assertThrows(Failures.NotFound.class,()->p.thread(who("owner"),A,thread));assertEquals(1,p.purge(who("privacy-worker"),A));
    }
    @Test void postSettlementDisputeUsesCompensatingEntriesAndDoesNotRewriteHistory() throws Exception {
        Item item=item();UUID id=reward(item);operations.dispatch(who("dispatcher"),A);
        deskReturn(item);rewards.confirm(who("owner"),A,id,sha("recovered"));rewards.settle(who("owner"),A,id,false);operations.dispatch(who("dispatcher"),A);
        assertEquals("RELEASED",rewards.read(who("owner"),A,id).state());var original=rewards.ledger(who("owner"),A,id);
        rewards.dispute(who("finder"),A,id,sha("dispute"),false);
        rewards.reverse(who("reviewer"),A,id,sha("reversal-review"));operations.dispatch(who("dispatcher"),A);
        assertEquals("REVERSED",rewards.read(who("owner"),A,id).state());var entries=rewards.ledger(who("owner"),A,id);
        assertEquals(original.size()+4,entries.size());assertTrue(entries.containsAll(original));
        assertEquals(0,entries.stream().mapToLong(Rewards.Entry::amount).sum());
        assertEquals(1000,entries.stream().filter(e->e.account().equals("PAYER")).mapToLong(Rewards.Entry::amount).sum());
        assertEquals(0,entries.stream().filter(e->e.account().equals("BENEFICIARY") || e.account().equals("FEE")).mapToLong(Rewards.Entry::amount).sum());
    }
    @Test void withdrawnAddressConsentStopsDispatchAndTheActualWorkerDrainsCoreJobs() throws Exception {
        Item item=item();UUID shipment=shipment(item);logistics.book(who("custodian"),A,shipment);
        logistics.withdrawConsent(who("owner"),A,shipment,sha("withdrawn"));int before=creates.get();operations.dispatch(who("dispatcher"),A);
        assertEquals(before,creates.get());assertEquals("FAILED",logistics.read(who("owner"),A,shipment).state());
        UUID reward=reward(item);
        var command=new ProcessBuilder("python","-B","tools/fulfillment_worker.py","--base",base,"--tenant",A.toString(),"--max-operations","5");
        var inherited=new HashMap<>(command.environment());command.environment().clear();
        for(String name:List.of("SystemRoot","PATH","Path","PATHEXT","TEMP","TMP"))if(inherited.containsKey(name))command.environment().put(name,inherited.get(name));
        command.environment().put("DZOKA_FULFILLMENT_WORKER_TOKEN",token("dispatcher"));
        var log=java.nio.file.Files.createTempFile(java.nio.file.Path.of(".test-tmp"),"fulfillment-worker-",".log");
        var process=command.redirectErrorStream(true).redirectOutput(log.toFile()).start();assertTrue(process.waitFor(30,TimeUnit.SECONDS));
        assertEquals(0,process.exitValue(),java.nio.file.Files.readString(log));assertEquals("RESERVED",rewards.read(who("owner"),A,reward).state());
    }
    @Test void unsentDisputedPaymentResumesAfterReviewAndUnknownPaymentCannotBeCancelled() throws Exception {
        Item item=item();UUID id=reward(item);operations.dispatch(who("dispatcher"),A);deskReturn(item);rewards.confirm(who("owner"),A,id,sha("receipt"));
        rewards.settle(who("owner"),A,id,false);rewards.dispute(who("finder"),A,id,sha("dispute"),false);int before=creates.get();
        operations.dispatch(who("dispatcher"),A);assertEquals(before,creates.get());
        rewards.dispute(who("reviewer"),A,id,sha("review"),true);due(id);operations.dispatch(who("dispatcher"),A);
        assertEquals("RELEASED",rewards.read(who("owner"),A,id).state());
        UUID unknown=reward(item());loseNextReply.set(true);operations.dispatch(who("dispatcher"),A);
        assertThrows(Failures.Conflict.class,()->rewards.cancelIntent(who("owner"),A,unknown,sha("cancel")));
        due(unknown);operations.dispatch(who("dispatcher"),A);rewards.settle(who("owner"),A,unknown,true);operations.dispatch(who("dispatcher"),A);
        UUID unsent=reward(item());assertEquals("FAILED",rewards.cancelIntent(who("owner"),A,unsent,sha("cancel-unsent")).state());
    }
    @Test void courierQuoteBudgetIsCommittedBeforeExternalRequests() throws Exception {
        String caller="quote-"+UUID.randomUUID();membership(A,caller,"logistics:use");
        for(int i=0;i<30;i++)assertNotNull(logistics.quote(who(caller),A,"sandbox-vault"));
        assertThrows(Failures.Conflict.class,()->logistics.quote(who(caller),A,"sandbox-vault"));
    }
}
