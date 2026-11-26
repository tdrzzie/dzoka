package org.dzoka;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.sql.DriverManager;
import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.dzoka.foundation.application.ContentDigest;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.intake.application.IntakeService;
import org.dzoka.intake.domain.ReportDraft;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class FederationIT extends IntegrationFixture {
    private final ObjectMapper json=new ObjectMapper();
    private final String remoteDb="dzoka_peer_"+UUID.randomUUID().toString().replace("-","");
    private final String month=YearMonth.now(java.time.ZoneOffset.UTC).toString();
    private KeyPair alpha,beta,rotated;
    private HttpServer slow;
    private ConfigurableApplicationContext remote;
    private String remoteBase;
    private boolean created;

    @Override protected void initializeProviders() throws Exception {
        KeyPairGenerator generator=KeyPairGenerator.getInstance("Ed25519");
        alpha=generator.generateKeyPair();beta=generator.generateKeyPair();rotated=generator.generateKeyPair();
        membership(A,"federation-admin","federation:admin","federation:search","reports:write","reports:read");
        service(url,A);
        try(var db=admin();var ddl=db.createStatement()) {
            ddl.execute("CREATE DATABASE "+remoteDb+" OWNER dzoka_migrator");
            created=true;
        }
        String remoteUrl=url.substring(0,url.lastIndexOf('/')+1)+remoteDb;
        Flyway.configure().dataSource(remoteUrl,required("DZOKA_TEST_MIGRATION_USER"),required("DZOKA_TEST_MIGRATION_PASSWORD"))
                .cleanDisabled(true).load().migrate();
        try(var db=DriverManager.getConnection(remoteUrl,adminUser,adminPassword);
            var tenant=db.prepareStatement("INSERT INTO dzoka.tenants(id,name) VALUES (?,'Remote synthetic institution')");
            var grant=db.prepareStatement("""
                    INSERT INTO dzoka.memberships(tenant_id,issuer,subject,permissions)
                    VALUES (?,?,'federation-admin',ARRAY['federation:admin','federation:search','reports:write','reports:read'])
                    """)) {
            tenant.setObject(1,B);tenant.executeUpdate();
            grant.setObject(1,B);grant.setString(2,issuer);grant.executeUpdate();
        }
        service(remoteUrl,B);
        var options=new ArrayList<>(settings(B,"beta",beta));
        options.addAll(List.of("--server.port=0","--server.address=127.0.0.1","--spring.main.banner-mode=off",
                "--spring.datasource.url="+remoteUrl,"--spring.datasource.username=dzoka_app",
                "--spring.datasource.password="+required("DZOKA_TEST_APP_PASSWORD"),
                "--dzoka.security.issuer="+issuer,"--dzoka.security.jwk-set-uri="+issuer+"/jwks",
                "--dzoka.security.audience=dzoka-core","--dzoka.security.allow-insecure-loopback=true"));
        remote=SpringApplication.run(CoreApplication.class,options.toArray(String[]::new));
        remoteBase="http://127.0.0.1:"+((WebServerApplicationContext)remote).getWebServer().getPort();
        slow=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        slow.setExecutor(Thread::startVirtualThread);
        slow.createContext("/api/v1/federation/peer",exchange -> {
            try {
                var packet=json.readTree(exchange.getRequestBody().readNBytes(16384));
                if("incompatible".equals(packet.path("audience").asString())) {
                    var remotePayload=Map.<String,Object>of("policy","coarse-published-v1","candidates",List.of(
                            Map.of("reference",UUID.randomUUID(),"rank",1,"freshness",month,"score",0.92)));
                    var response=wire(beta,"current","SEARCH_RESPONSE","incompatible","alpha",
                            UUID.fromString(packet.path("request").asString()),remotePayload,Instant.now().getEpochSecond());
                    byte[] reply=json.writeValueAsBytes(response);
                    exchange.sendResponseHeaders(200,reply.length);
                    exchange.getResponseBody().write(reply);
                } else {
                    Thread.sleep(2200);
                    exchange.sendResponseHeaders(503,-1);
                }
            }
            catch(InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            catch(java.security.GeneralSecurityException invalid) { exchange.sendResponseHeaders(500,-1); }
            finally { exchange.close(); }
        });
        slow.start();
    }

    private void service(String dbUrl,UUID tenant) throws Exception {
        try(var db=DriverManager.getConnection(dbUrl,adminUser,adminPassword);
            var sql=db.prepareStatement("""
                    INSERT INTO dzoka.memberships(tenant_id,issuer,subject,permissions)
                    VALUES (?,'urn:dzoka:federation','node',ARRAY['federation:service'])
                    """)) {
            sql.setObject(1,tenant);sql.executeUpdate();
        }
    }

    @Override protected List<String> extraOptions() {
        var options=new ArrayList<>(settings(A,"alpha",alpha));
        String fixture="http://127.0.0.1:"+slow.getAddress().getPort();
        options.add("--DZOKA_FEDERATION_ORIGINS=beta="+remoteBase+",slow="+fixture+",incompatible="+fixture);
        return options;
    }

    private static List<String> settings(UUID tenant,String node,KeyPair key) {
        return List.of("--DZOKA_FEDERATION_TENANT="+tenant,"--DZOKA_FEDERATION_NODE="+node,
                "--DZOKA_FEDERATION_KEY_ID=current","--DZOKA_FEDERATION_PRIVATE_KEY="+encoded(key.getPrivate().getEncoded()),
                "--DZOKA_FEDERATION_TEST_LOOPBACK=true");
    }

    @AfterAll void stopPeer() throws Exception {
        if(slow!=null) slow.stop(0);
        if(remote!=null) remote.close();
        if(created) try(var db=admin();var ddl=db.createStatement()) { ddl.execute("DROP DATABASE "+remoteDb); }
    }

    @Test void independentDatabaseFederationRejectsReplayAndLeaksNoReportContent() throws Exception {
        configure(base,A,"beta",beta);
        configure(base,A,"slow",beta);
        configure(base,A,"incompatible",beta);
        configure(remoteBase,B,"alpha",alpha);
        String secret="Private detail marker never export";
        UUID released=report(secret);
        UUID unpublished=report("Another private source");
        String offer="/offers/"+released;
        expect(204,tenant(remoteBase,B,"PUT",offer,Map.of("revision",1,"category","BAG","country","ZW","month",month,"active",true)));
        expect(403,api("POST",A,"/federation/search","viewer",json.writeValueAsString(Map.of("peers",List.of("beta"),"query",query()))));
        JsonNode caps=expect(200,tenant(base,A,"POST","/peers/beta/capabilities",null));
        assertEquals(1,caps.path("versions").path(0).asInt());

        long start=System.nanoTime();
        JsonNode partial=expect(200,tenant(base,A,"POST","/search",Map.of("peers",List.of("beta","slow"),"query",query())));
        assertTrue(System.nanoTime()-start<5_000_000_000L);
        assertTrue(partial.path("partial").asBoolean());
        assertEquals("COMPLETE",partial.path("peers").path(0).path("state").asString());
        assertEquals("TIMEOUT",partial.path("peers").path(1).path("state").asString());
        assertEquals(1,partial.path("hits").size());
        assertEquals("beta",partial.path("hits").path(0).path("peer").asString());
        assertEquals(1,partial.path("hits").path(0).path("peerRank").asInt());
        assertFalse(partial.toString().contains("score") || partial.toString().contains(secret));
        assertFalse(partial.toString().contains(released.toString()) || partial.toString().contains(unpublished.toString()));
        assertEquals("UNCALIBRATED",partial.path("hits").path(0).path("calibration").asString());
        var ranked=expect(200,tenant(base,A,"POST","/search",Map.of("peers",List.of("beta","incompatible"),"query",query())));
        assertEquals("UNAVAILABLE",ranked.path("peers").path(1).path("state").asString());
        assertEquals(1,ranked.path("hits").size());
        assertFalse(ranked.toString().contains("0.92"));

        UUID request=UUID.randomUUID();
        var signed=sign(alpha,"current","beta",request,query(),Instant.now().getEpochSecond());
        expect(200,peer(signed));
        expect(409,peer(signed));
        expect(403,peer(sign(alpha,"current","other",UUID.randomUUID(),query(),Instant.now().getEpochSecond())));
        expect(403,peer(sign(alpha,"current","beta",UUID.randomUUID(),query(),Instant.now().minusSeconds(90).getEpochSecond())));
        var modified=new LinkedHashMap<>(sign(alpha,"current","beta",UUID.randomUUID(),query(),Instant.now().getEpochSecond()));
        modified.put("payload",encoded(json.writeValueAsBytes(Map.of("category","WALLET","country","ZW","month",month,"limit",1))));
        expect(403,peer(modified));
        var excessive=new LinkedHashMap<>(query());excessive.put("include","phone");
        expect(400,peer(sign(alpha,"current","beta",UUID.randomUUID(),excessive,Instant.now().getEpochSecond())));
        var unsupported=new LinkedHashMap<>(signed);unsupported.put("version",2);
        expect(400,peer(unsupported));

        expect(204,tenant(remoteBase,B,"PUT","/peers/alpha/keys/next",Map.of("publicKey",encoded(rotated.getPublic().getEncoded()))));
        expect(204,tenant(remoteBase,B,"DELETE","/peers/alpha/keys/current",null));
        expect(403,peer(sign(alpha,"current","beta",UUID.randomUUID(),query(),Instant.now().getEpochSecond())));
        expect(200,peer(sign(rotated,"next","beta",UUID.randomUUID(),query(),Instant.now().getEpochSecond())));
        assertEquals("REFUSED",expect(200,tenant(base,A,"POST","/search",Map.of("peers",List.of("beta"),"query",query())))
                .path("peers").path(0).path("state").asString());
        expect(204,tenant(remoteBase,B,"PUT",offer,Map.of("revision",1,"active",false)));
        var gone=expect(200,peer(sign(rotated,"next","beta",UUID.randomUUID(),query(),Instant.now().getEpochSecond())));
        var decoded=json.readTree(Base64.getUrlDecoder().decode(gone.path("payload").asString()));
        assertEquals(0,decoded.path("candidates").size());

        // The same node budget survives key rotation. Emit requests until the signed window closes.
        int allowed=0;
        boolean limited=false;
        for(int i=0;i<31;i++) {
            var requestResult=peer(sign(rotated,"next","beta",UUID.randomUUID(),query(),Instant.now().getEpochSecond()));
            if(requestResult.statusCode()==409) { limited=true;break; }
            expect(200,requestResult);
            allowed++;
        }
        assertTrue(limited && allowed>0 && allowed<30);

        for(int i=0;i<2;i++) expect(200,tenant(base,A,"POST","/search",Map.of("peers",List.of("slow"),"query",query())));
        assertEquals("CIRCUIT_OPEN",expect(200,tenant(base,A,"POST","/search",Map.of("peers",List.of("slow"),"query",query())))
                .path("peers").path(0).path("state").asString());
        assertTrue(count("federation_traffic",A)>0);
        assertEquals(0,count("federation_offers",A));
    }

    private UUID report(String description) {
        return remote.getBean(IntakeService.class).submit(new VerifiedIdentity(issuer,"federation-admin"),B,null,null,
                new ReportDraft(ReportDraft.Kind.FOUND,description,"en","UTC",null,null,null,null,null,null,null),
                description.getBytes(StandardCharsets.UTF_8),UUID.randomUUID().toString()).reportId();
    }

    private Map<String,Object> query() { return Map.of("category","BAG","country","ZW","month",month,"limit",2); }

    private void configure(String origin,UUID tenant,String peer,KeyPair key) throws Exception {
        expect(204,tenant(origin,tenant,"PUT","/peers/"+peer,
                Map.of("inbound",true,"outbound",true,"active",true,"minVersion",1,"maxVersion",1)));
        expect(204,tenant(origin,tenant,"PUT","/peers/"+peer+"/keys/current",Map.of("publicKey",encoded(key.getPublic().getEncoded()))));
    }

    private Map<String,Object> sign(KeyPair pair,String keyId,String audience,UUID request,Map<String,Object> payload,long issued) throws Exception {
        return wire(pair,keyId,"SEARCH_REQUEST","alpha",audience,request,payload,issued);
    }

    private Map<String,Object> wire(KeyPair pair,String keyId,String operation,String from,String audience,UUID request,
                                    Map<String,Object> payload,long issued) throws java.security.GeneralSecurityException {
        byte[] bytes=json.writeValueAsBytes(payload);
        var signer=Signature.getInstance("Ed25519");
        signer.initSign(pair.getPrivate());
        long expires=issued+30;
        signer.update(String.join("\n","dzoka-federation","1",operation,from,audience,keyId,request.toString(),
                Long.toString(issued),Long.toString(expires),ContentDigest.sha256(bytes)).getBytes(StandardCharsets.US_ASCII));
        return Map.of("version",1,"operation",operation,"issuer",from,"audience",audience,"keyId",keyId,
                "request",request,"issued",issued,"expires",expires,"payload",encoded(bytes),"signature",encoded(signer.sign()));
    }

    private HttpResponse<String> tenant(String origin,UUID scope,String method,String path,Object body) throws Exception {
        var request=HttpRequest.newBuilder(URI.create(origin+"/api/v1/tenants/"+scope+"/federation"+path))
                .header("Authorization","Bearer "+token("federation-admin")).header("Content-Type","application/json")
                .timeout(java.time.Duration.ofSeconds(8)).method(method,body==null?HttpRequest.BodyPublishers.noBody()
                        :HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();
        return http.send(request,HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> peer(Map<String,Object> wire) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(remoteBase+"/api/v1/federation/peer"))
                .header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(wire)))
                .build(),HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode expect(int status,HttpResponse<String> response) {
        assertEquals(status,response.statusCode(),response.body());
        return response.body().isBlank()?null:json.readTree(response.body());
    }

    private static String encoded(byte[] value) { return Base64.getUrlEncoder().withoutPadding().encodeToString(value); }
}
