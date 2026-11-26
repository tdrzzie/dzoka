package org.dzoka;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.intake.application.IntakeService;
import org.dzoka.intake.domain.ReportDraft;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.dzoka.foundation.application.ContentDigest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ChannelsIT extends S3Fixture {
    private static final String ACCOUNT="123456", PHONE="789012", OWNER="12025550107", FINDER="12025550108";
    private final String secret=UUID.randomUUID().toString().replace("-","");
    private final ObjectMapper json=new ObjectMapper();
    private final AtomicInteger sends=new AtomicInteger(), response=new AtomicInteger(200);
    private final AtomicInteger downloads=new AtomicInteger();
    private final java.util.concurrent.atomic.AtomicReference<String> lastReply=new java.util.concurrent.atomic.AtomicReference<>();
    private byte[] photo;
    private HttpServer graph;

    @Override protected void initializeProviders() throws Exception {
        super.initializeProviders();
        photo=png();
        membership(A,"registrar","reports:write","reports:read","recovery:manage","channels:admin","custody:operate","channels:use");
        membership(A,"claimant","claims:submit","reports:read","reports:write","matches:read","channels:use");
        membership(A,"observer","reports:read:any","recovery:verify");
        membership(A,"desk","recovery:handover");
        membership(A,"moderator","channels:moderate","recovery:review","reports:read:any");
        membership(A,"dispatch","channels:service");
        membership(A,"receiver","custody:operate","channels:use");
        membership(B,"receiver","custody:operate");
        try(var db=admin();var sql=db.prepareStatement("""
                INSERT INTO dzoka.memberships(tenant_id,issuer,subject,permissions) VALUES (?,'urn:dzoka:channel','whatsapp',ARRAY['channels:service'])
                """)) { sql.setObject(1,A);sql.executeUpdate(); }
        graph=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        graph.createContext("/v99.0/"+PHONE+"/messages",exchange -> {
            var packet=json.readTree(exchange.getRequestBody().readNBytes(16384));
            int call=sends.incrementAndGet();
            boolean shape=exchange.getRequestMethod().equals("POST")
                    && packet.path("messaging_product").asString().equals("whatsapp")
                    && List.of(OWNER,FINDER).contains(packet.path("to").asString())
                    && packet.path("biz_opaque_callback_data").asString().length()==36
                    && (packet.path("type").asString().equals("template") && packet.path("template").path("name").asString().startsWith("dzoka_")
                    || packet.path("type").asString().equals("text") && !packet.path("text").path("body").asString().isBlank());
            lastReply.set(packet.path("text").path("body").asString(packet.path("template").path("name").asString()));
            byte[] output=("{\"messages\":[{\"id\":\"wamid.test"+call+"\"}]}").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(shape?response.get():400,output.length);
            try(var stream=exchange.getResponseBody()) { stream.write(output); }
        });
        mediaRoute("123456789","image/png",photo);
        mediaRoute("123456790","audio/ogg",new byte[]{'O','g','g','S',1,2});
        mediaRoute("123456791","video/mp4",new byte[]{0,0,0,8,'f','t','y','p'});
        graph.start();
    }
    private void mediaRoute(String id,String type,byte[] bytes) {
        graph.createContext("/v99.0/"+id,exchange -> {
            boolean shape=exchange.getRequestMethod().equals("GET") && ("phone_number_id="+PHONE).equals(exchange.getRequestURI().getQuery());
            byte[] output=json.writeValueAsBytes(Map.of("id",id,"mime_type",type,"file_size",bytes.length,
                    "url","http://127.0.0.1:"+graph.getAddress().getPort()+"/fixture/"+id));
            exchange.sendResponseHeaders(shape?200:400,output.length);
            try(var stream=exchange.getResponseBody()) { stream.write(output); }
        });
        graph.createContext("/fixture/"+id,exchange -> {
            downloads.incrementAndGet();
            exchange.sendResponseHeaders(200,bytes.length);
            try(var stream=exchange.getResponseBody()) { stream.write(bytes); }
        });
    }
    @AfterAll void stopGraph() { if(graph!=null) graph.stop(0); }

    @Override protected boolean needsS3() { return false; }

    @Override protected List<String> extraOptions() {
        var options=new java.util.ArrayList<>(super.extraOptions());
        options.addAll(List.of("--dzoka.recovery.hmac-key="+UUID.randomUUID().toString().replace("-","")+secret,
                "--DZOKA_WHATSAPP_TENANT="+A,"--DZOKA_WHATSAPP_ACCOUNT="+ACCOUNT,
                "--DZOKA_WHATSAPP_PHONE_ID="+PHONE,"--DZOKA_WHATSAPP_VERIFY_TOKEN="+secret,
                "--DZOKA_WHATSAPP_APP_SECRET="+secret,"--DZOKA_WHATSAPP_CONTACT_KEY="+secret+secret,
                "--DZOKA_WHATSAPP_ACCESS_TOKEN=synthetic-loopback-only","--DZOKA_WHATSAPP_GRAPH_VERSION=v99.0",
                "--DZOKA_WHATSAPP_TEST_LOOPBACK=true","--DZOKA_WHATSAPP_GRAPH_ORIGIN=http://127.0.0.1:"+graph.getAddress().getPort()));
        return options;
    }

    @Test @Order(1) void signedConversationRelayAndDeliveryHaveIndependentPrivateState() throws Exception {
        String handshake="/api/v1/hooks/whatsapp?hub.mode=subscribe&hub.verify_token="+secret+"&hub.challenge=531";
        var result=http.send(HttpRequest.newBuilder(URI.create(base+handshake)).GET().build(),HttpResponse.BodyHandlers.ofString());
        assertEquals(200,result.statusCode());assertEquals("531",result.body());
        UUID contact=UUID.randomUUID();
        expect(400,put("/channels/contacts/"+contact,"registrar",Map.of("subject","claimant","address",OWNER)));
        expect(204,put("/channels/contacts/"+contact,"registrar",Map.of("subject","claimant","address",OWNER,"consentConfirmed",true)));
        expect(204,put("/channels/contacts/"+UUID.randomUUID(),"registrar",Map.of("subject","registrar","address",FINDER,"consentConfirmed",true)));
        long reports=count("reports",A);
        String inbound=event("messages",List.of(inbound("wamid.inbound-1","LOST green field notebook")),PHONE);
        expect(403,hook(inbound,false));
        expect(403,hook(event("messages",List.of(inbound("wamid.inbound-2","LOST another notebook")),"123991"),true));
        expect(200,hook(inbound,true));
        expect(200,hook(inbound,true));
        assertEquals(reports+1,count("reports",A));
        assertEquals(1,count("channel_operations",A));
        assertEquals("SENT",dispatch());
        assertEquals("IDLE",dispatch());
        assertEquals(1,sends.get());

        UUID id=UUID.randomUUID();
        expect(200,put("/channels/notices/"+id,"registrar",Map.of("contact",contact)));
        expect(404,api("GET",A,"/channels/notices/"+id,"receiver",null));
        response.set(503);
        assertEquals("UNKNOWN",dispatch());
        response.set(200);
        assertEquals("IDLE",dispatch());
        assertEquals(2,sends.get());
        expect(200,hook(event("statuses",List.of(delivery(id,"delivered",FINDER)),PHONE),true));
        assertEquals("UNKNOWN",state(id));
        var delivered=event("statuses",List.of(delivery(id,"delivered",OWNER)),PHONE);
        expect(200,hook(delivered,true));expect(200,hook(delivered,true));
        expect(200,hook(event("statuses",List.of(delivery(id,"sent",OWNER),delivery(id,"failed",OWNER)),PHONE),true));
        assertEquals("DELIVERED",state(id));
        assertEquals(0,count("recovery_returns",A));
        var notices=body(api("GET",A,"/channels/notices","claimant",null));
        assertEquals(2,notices.size());
        assertFalse(notices.toString().contains(OWNER));
        UUID retry=UUID.randomUUID();
        expect(200,put("/channels/notices/"+retry,"registrar",Map.of("contact",contact)));
        response.set(429);
        assertEquals("QUEUED",dispatch());
        assertEquals("IDLE",dispatch());
        response.set(200);
        try(var db=admin();var sql=db.prepareStatement("""
                UPDATE dzoka.channel_operations SET ready_at=clock_timestamp()-interval '1 second' WHERE tenant_id=? AND id=?
                """)) { sql.setObject(1,A);sql.setObject(2,retry);sql.executeUpdate(); }
        assertEquals("SENT",dispatch());
        var location=event("messages",List.of(Map.of("id","wamid.location","from",OWNER,"timestamp",epoch(),"type","location",
                "context",Map.of("id","wamid.inbound-1"),"location",Map.of("latitude",-17.83,"longitude",31.05))),PHONE);
        expect(200,hook(location,true));expect(200,hook(location,true));
        try(var db=admin();var sql=db.prepareStatement("""
                SELECT v.raw_text,v.latitude,v.longitude,v.uncertainty_meters FROM dzoka.report_revisions v
                WHERE v.tenant_id=? AND v.revision=2 AND v.raw_text='green field notebook'
                """)) {
            sql.setObject(1,A);
            try(var rows=sql.executeQuery()) {
                assertTrue(rows.next());assertEquals(-17.83,rows.getDouble(2));assertEquals(31.05,rows.getDouble(3));
                assertEquals(200000,rows.getDouble(4));assertFalse(rows.next());
            }
        }
        assertEquals("SENT",dispatch());

        expect(200,put("/recovery/policies/1","registrar",Map.of("dualControl",false,"allowRepresentatives",false)));
        UUID intake=enroll();
        UUID claim=UUID.randomUUID(), thread=UUID.randomUUID();
        expect(200,put("/recovery/intakes/"+intake+"/claims/"+claim,"claimant",Map.of("capacity","OWNER")));
        expect(204,put("/channels/relays/"+thread,"moderator",Map.of("claim",claim)));
        expect(204,put("/channels/relays/"+thread,"moderator",Map.of("claim",claim)));
        var relay=event("messages",List.of(inbound("wamid.inbound-relay","RELAY "+thread+" REQUEST_UPDATE")),PHONE);
        expect(200,hook(relay,true));expect(200,hook(relay,true));
        var threadView=api("GET",A,"/channels/relays/"+thread,"registrar",null);
        assertEquals("REQUEST_UPDATE",body(threadView).path("messages").path(0).path("phrase").asString());
        assertFalse(threadView.body().contains(OWNER));assertFalse(threadView.body().contains("claimant"));
        assertEquals("SENT",dispatch());
        expect(204,put("/channels/relays/"+thread+"/messages/"+UUID.randomUUID(),"registrar",Map.of("phrase","THANK_YOU")));
        expect(204,put("/channels/relays/"+thread+"/block","registrar",Map.of("abuse",true)));
        assertEquals("IDLE",dispatch());
        assertTrue(body(api("GET",A,"/channels/relays/"+thread+"/moderation","moderator",null)).path("flagged").asBoolean());
        expect(404,put("/channels/relays/"+thread+"/messages/"+UUID.randomUUID(),"claimant",Map.of("phrase","THANK_YOU")));
        UUID queued=UUID.randomUUID();
        expect(200,put("/channels/notices/"+queued,"registrar",Map.of("contact",contact)));
        expect(200,hook(event("messages",List.of(inbound("wamid.stop","STOP")),PHONE),true));
        assertEquals("CANCELLED",state(queued));
        assertEquals("CANCELLED",body(put("/channels/notices/"+UUID.randomUUID(),"registrar",Map.of("contact",contact))).path("state").asString());
        expect(204,put("/channels/contacts/"+contact+"/consent","registrar",Map.of("enabled",true)));
        assertEquals("CANCELLED",state(queued));
        assertEquals("IDLE",dispatch());
        try(var db=admin();var query=db.prepareStatement("SELECT route,address_key FROM dzoka.channel_contacts WHERE tenant_id=?")) {
            query.setObject(1,A);
            try(var rows=query.executeQuery()) {
                while(rows.next()) assertFalse(rows.getString(1).contains(OWNER) || rows.getString(1).contains(FINDER) || rows.getString(2).equals(OWNER));
            }
        }
    }

    @Test @Order(2) void quotedMediaMatchingChallengeAndStaffedPickup() throws Exception {
        String candidateWords="maroon violin with curved wooden bridge";
        var found=app.getBean(IntakeService.class).submit(new VerifiedIdentity(issuer,"registrar"),A,null,null,
                new ReportDraft(ReportDraft.Kind.FOUND,candidateWords,"en","UTC",null,null,null,null,null,null,null),
                "{}".getBytes(StandardCharsets.UTF_8),UUID.randomUUID().toString());
        UUID intake=UUID.randomUUID();
        String path="/recovery/intakes/"+intake;
        expect(204,put(path,"registrar",Map.of("foundReport",found.reportId(),"site",UUID.randomUUID())));
        expect(403,put("/channels/match-offers/"+found.reportId(),"claimant",Map.of("revision",1,"summary",candidateWords,"enabled",true)));
        expect(204,put("/channels/match-offers/"+found.reportId(),"registrar",Map.of("revision",1,"summary",candidateWords,"enabled",true)));
        String answer="Synthetic violet stitch beneath the handle";
        expect(204,put(path+"/facts/"+UUID.randomUUID(),"observer",Map.of("kind","HIDDEN_MARK",
                "sourceSha","b".repeat(64),"answer",answer,"privateConfirmed",true)));

        String original="wamid.conversation-original";
        expect(200,hook(event("messages",List.of(inbound(original,"LOST "+candidateWords)),PHONE),true));
        assertEquals("SENT",dispatch());
        if(s3!=null) {
            var image=Map.of("id","123456789","mime_type","image/png","sha256",ContentDigest.sha256(photo));
            var attachment=Map.of("id","wamid.conversation-image","from",OWNER,"timestamp",epoch(),"type","image",
                    "context",Map.of("id",original),"image",image);
            expect(403,hook(event("messages",List.of(attachment),PHONE),false));
            expect(200,hook(event("messages",List.of(attachment),PHONE),true));
            expect(200,hook(event("messages",List.of(attachment),PHONE),true));
            assertEquals(0,downloads.get());
            assertEquals("SENT",dispatch());
            assertEquals(1,downloads.get());
            assertEquals(1,count("media_assets",A));
            assertTrue(lastReply.get().startsWith("Photo received"));
            expect(200,hook(event("messages",List.of(Map.of("id","wamid.bad-hash","from",OWNER,"timestamp",epoch(),
                    "type","image","context",Map.of("id",original),"image",
                    Map.of("id","123456789","mime_type","image/png","sha256","a".repeat(64)))),PHONE),true));
            assertEquals("SENT",dispatch());
            assertEquals(1,count("media_assets",A));
            assertEquals("dzoka_unsupported",lastReply.get());
            checkQuarantinedAttachment(original,"audio","audio/ogg","123456790",new byte[]{'O','g','g','S',1,2});
            checkQuarantinedAttachment(original,"video","video/mp4","123456791",new byte[]{0,0,0,8,'f','t','y','p'});
            assertEquals(3,count("media_assets",A));
            try(var db=admin();var query=db.prepareStatement("""
                    SELECT id,state FROM dzoka.media_assets WHERE tenant_id=? AND declared_type NOT LIKE 'image/%'
                    """)) {
                query.setObject(1,A);
                try(var rows=query.executeQuery()) {
                    while(rows.next()) {
                        assertEquals("QUARANTINED",rows.getString(2));
                        expect(409,api("GET",A,"/media/"+rows.getObject(1)+"/preview","claimant",null));
                    }
                }
            }
        }
        try(var db=admin();var sql=db.prepareStatement("UPDATE dzoka.channel_match_watches SET due_at=clock_timestamp()-interval '1 second' WHERE tenant_id=?")) {
            sql.setObject(1,A);sql.executeUpdate();
        }
        var alerts=List.of(dispatch()+":"+lastReply.get(),dispatch()+":"+lastReply.get());
        assertTrue(alerts.contains("SENT:dzoka_match_alert"),alerts.toString());
        assertEquals(1,count("channel_match_notifications",A));
        // Simulate an existing V18 report with its signed inbox anchor and no V19 watch.
        try(var db=admin();var sql=db.prepareStatement("""
                DELETE FROM dzoka.channel_match_watches WHERE tenant_id=? AND report_id IN (
                  SELECT report_id FROM dzoka.channel_match_notifications WHERE tenant_id=?)
                """)) {
            sql.setObject(1,A);sql.setObject(2,A);assertEquals(1,sql.executeUpdate());
        }
        String search=chat("wamid.search",original,"SEARCH");
        assertTrue(search.startsWith("1 possible candidate"),search);
        String run=search.split("REVIEW ")[1].split(" ")[0];
        assertTrue(chat("wamid.review",original,"REVIEW "+run+" 1").startsWith("Candidate 1"));
        String claimMessage=chat("wamid.claim",original,"CLAIM "+run+" 1");
        assertTrue(claimMessage.startsWith("Claim PENDING"),claimMessage);
        String claim=claimMessage.split("CHALLENGE ")[1].replace(".","").split(" ")[1];
        String ids=intake+" "+claim;
        String challenge=chat("wamid.challenge",null,"CHALLENGE "+ids);
        assertTrue(challenge.startsWith("Describe a distinctive mark"),challenge);
        String answerCommand=challenge.split("Send ANSWER ")[1].split(" followed by")[0];
        String session=answerCommand.split(" ")[2];
        assertTrue(chat("wamid.answer",null,"ANSWER "+ids+" "+session+" "+answer).startsWith("Your answer was recorded"));
        assertFalse(lastReply.get().contains(answer));
        assertEquals("Your claim is waiting for review.",chat("wamid.pickup-before",null,"PICKUP "+ids));
        assertEquals(0,count("pickup_messages",A));
        String claimPath=path+"/claims/"+claim;
        expect(200,put(claimPath+"/reviews/"+UUID.randomUUID(),"moderator",Map.of("session",session,"decision","APPROVE",
                "reason","INDEPENDENT_EVIDENCE","evidenceSha","c".repeat(64))));
        assertTrue(chat("wamid.pickup",null,"PICKUP "+ids).startsWith("Your pickup request was recorded"));
        assertEquals(1,count("pickup_messages",A));
        assertEquals(0,count("recovery_returns",A));
        assertEquals("REQUEST",body(api("GET",A,path+"/pickup-messages","desk",null)).path(0).path("kind").asString());

        UUID grant=UUID.randomUUID();
        var issued=body(put(claimPath+"/grants/"+grant,"moderator",Map.of("audienceRef",new VerifiedIdentity(issuer,"desk").reference())));
        String token=issued.path("token").asString();
        expect(200,put(path+"/returns/"+UUID.randomUUID(),"desk",Map.of("grant",grant,"token",token,
                "recipientRef",new VerifiedIdentity(issuer,"claimant").reference(),"confirmationSha","d".repeat(64))));
        assertEquals("SENT",dispatch());
        assertEquals("Your confirmation of the recorded return was saved.",chat("wamid.confirm",null,"CONFIRM "+ids));
        assertEquals(2,count("pickup_messages",A));
        assertEquals(1,count("recovery_returns",A));
        assertFalse(lastReply.get().contains(token));

    }

    private void checkQuarantinedAttachment(String original,String kind,String type,String asset,byte[] bytes) throws Exception {
        var metadata=Map.of("id",asset,"mime_type",type,"sha256",ContentDigest.sha256(bytes));
        var packet=Map.of("id","wamid."+kind+"-fixture","from",OWNER,"timestamp",epoch(),"type",kind,
                "context",Map.of("id",original),kind,metadata);
        expect(200,hook(event("messages",List.of(packet),PHONE),true));
        assertEquals("SENT",dispatch());
        assertTrue(lastReply.get().startsWith("Attachment received in private quarantine"));
    }

    private String chat(String id,String quoted,String text) throws Exception {
        var inbound=new java.util.HashMap<>(inbound(id,text));
        if(quoted!=null) inbound.put("context",Map.of("id",quoted));
        String webhook=event("messages",List.of(inbound),PHONE);
        expect(200,hook(webhook,true));
        expect(200,hook(webhook,true));
        assertEquals("SENT",dispatch());
        return lastReply.get();
    }

    @Test @Order(3) void custodySerializesAcceptanceAndRetainsStaleOfflineConflict() throws Exception {
        UUID intake=enroll();
        String path="/recovery/intakes/"+intake+"/custody";
        UUID destination=UUID.randomUUID(), requested=UUID.randomUUID();
        String seen=Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
        Map<String,Object> request=Map.of("action","REQUEST_TRANSFER","expectedGeneration",1,"destination",destination,
                "recipientRef",new VerifiedIdentity(issuer,"receiver").reference(),"observedAt",seen,"evidenceSha","a".repeat(64));
        assertEquals("TRANSFER_REQUESTED",body(put(path+"/"+requested,"registrar",request)).path("outcome").asString());
        assertEquals("TRANSFER_REQUESTED",body(put(path+"/"+requested,"registrar",request)).path("outcome").asString());
        Map<String,Object> accept=Map.of("action","ACCEPT_TRANSFER","expectedGeneration",1,"destination",destination,
                "requestId",requested,"observedAt",seen,"evidenceSha","b".repeat(64));
        try(var pool=Executors.newFixedThreadPool(2)) {
            var first=pool.submit(() -> body(put(path+"/"+UUID.randomUUID(),"receiver",accept)).path("outcome").asString());
            var second=pool.submit(() -> body(put(path+"/"+UUID.randomUUID(),"receiver",accept)).path("outcome").asString());
            assertEquals(java.util.Set.of("TRANSFER_ACCEPTED","OFFLINE_CONFLICT"),
                    java.util.Set.of(first.get(20,TimeUnit.SECONDS),second.get(20,TimeUnit.SECONDS)));
        }
        var projection=body(api("GET",A,path,"receiver",null));
        assertEquals(2,projection.path("generation").asInt());
        assertEquals(destination.toString(),projection.path("site").asString());
        expect(404,api("GET",B,path,"receiver",null));
        expect(403,api("GET",A,path,"claimant",null));
    }

    private UUID enroll() throws Exception {
        var result=app.getBean(IntakeService.class).submit(new VerifiedIdentity(issuer,"registrar"),A,null,null,
                new ReportDraft(ReportDraft.Kind.FOUND,"synthetic scarf","en","UTC",null,null,null,null,null,null,null),
                "{}".getBytes(StandardCharsets.UTF_8),UUID.randomUUID().toString());
        UUID intake=UUID.randomUUID();
        expect(204,put("/recovery/intakes/"+intake,"registrar",Map.of("foundReport",result.reportId(),"site",UUID.randomUUID())));
        return intake;
    }
    private Map<String,Object> inbound(String id,String text) {
        return Map.of("id",id,"from",OWNER,"timestamp",epoch(),"type","text","text",Map.of("body",text));
    }
    private Map<String,Object> delivery(UUID op,String state,String recipient) {
        return Map.of("id","wamid.test2","status",state,"recipient_id",recipient,"timestamp",epoch(),"biz_opaque_callback_data",op);
    }
    private static String epoch() { return Long.toString(Instant.now().getEpochSecond()); }
    private String event(String key,List<?> items,String phone) {
        return json.writeValueAsString(Map.of("object","whatsapp_business_account","entry",List.of(Map.of("id",ACCOUNT,
                "changes",List.of(Map.of("field","messages","value",Map.of("messaging_product","whatsapp",
                        "metadata",Map.of("phone_number_id",phone),key,items)))))));
    }
    private HttpResponse<String> hook(String payload,boolean valid) throws Exception {
        var mac=Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
        var signature=HexFormat.of().formatHex(mac.doFinal((valid?payload:payload+" ").getBytes(StandardCharsets.UTF_8)));
        return http.send(HttpRequest.newBuilder(URI.create(base+"/api/v1/hooks/whatsapp"))
                .header("Content-Type","application/json").header("X-Hub-Signature-256","sha256="+signature)
                .POST(HttpRequest.BodyPublishers.ofString(payload)).build(),HttpResponse.BodyHandlers.ofString());
    }
    private String dispatch() throws Exception { return body(api("POST",A,"/channels/dispatch","dispatch",null)).path("state").asString(); }
    private String state(UUID id) throws Exception { return body(api("GET",A,"/channels/notices/"+id,"claimant",null)).path("state").asString(); }
    private HttpResponse<String> put(String path,String actor,Object value) throws Exception {
        return api("PUT",A,path,actor,json.writeValueAsString(value));
    }
    private JsonNode body(HttpResponse<String> response) { expect(200,response);return json.readTree(response.body()); }
    private static void expect(int status,HttpResponse<String> response) { assertEquals(status,response.statusCode(),response.body()); }
}
