package org.dzoka;

import static org.junit.jupiter.api.Assertions.*;
import java.sql.*;
import java.net.URI;
import java.net.http.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.dzoka.foundation.persistence.FoundationAdmin;
import org.dzoka.foundation.application.Failures;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.organizations.application.OrganizationService;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import tools.jackson.databind.ObjectMapper;

class FoundationAdministrationIT extends IntegrationFixture {
    private final ObjectMapper json=new ObjectMapper();
    private HttpResponse<String> update(UUID tenant,UUID id,String subject,String name,boolean active,long version,String key) throws Exception {
        var body=json.writeValueAsString(Map.of("name",name,"active",active,"expectedVersion",version));
        var request=HttpRequest.newBuilder(URI.create(base+"/api/v1/tenants/"+tenant+"/organizations/"+id))
                .timeout(java.time.Duration.ofSeconds(15)).header("Authorization","Bearer "+token(subject))
                .header("Content-Type","application/json").header("Idempotency-Key",key).PUT(HttpRequest.BodyPublishers.ofString(body)).build();
        return http.send(request,HttpResponse.BodyHandlers.ofString());
    }
    private UUID create(String name) {
        return app.getBean(OrganizationService.class).create(new VerifiedIdentity(issuer,"alice"),A,name,UUID.randomUUID().toString()).id();
    }
    private Connection operator() throws Exception {
        return DriverManager.getConnection(url,required("DZOKA_TEST_MIGRATION_USER"),required("DZOKA_TEST_MIGRATION_PASSWORD"));
    }
    private FoundationAdmin.Command member(UUID tenant,String subject,boolean active,long version,String key) {
        return new FoundationAdmin.Command("MEMBERSHIP",tenant,null,issuer,subject,List.of("organizations:read"),active,version,key);
    }
    @Test void cursorPagesReachBeyondOldCapWithoutCrossTenantRows() throws Exception {
        try(var c=admin();var s=c.prepareStatement("INSERT INTO dzoka.organizations(tenant_id,id,name,version) VALUES (?,?,?,1)")) {
            for(int i=0;i<113;i++){s.setObject(1,A);s.setObject(2,UUID.randomUUID());s.setString(3,"Synthetic page "+i);s.addBatch();}s.executeBatch();
        }
        long expected;try(var c=admin();var s=c.prepareStatement("SELECT count(*) FROM dzoka.organizations WHERE tenant_id=? AND active")){s.setObject(1,A);try(var r=s.executeQuery()){r.next();expected=r.getLong(1);}}
        Set<String> ids=new HashSet<>();String cursor="";
        do {
            var response=api("GET",A,"/organizations/page?limit=17"+(cursor.isEmpty()?"":"&after="+cursor),"alice",null);
            assertEquals(200,response.statusCode());var page=json.readTree(response.body());
            assertTrue(page.get("items").size()<=17);
            for(var item:page.get("items"))assertTrue(ids.add(item.get("id").asText()));
            cursor=page.get("nextCursor").isNull()?"":page.get("nextCursor").asText();
        }while(!cursor.isEmpty());
        assertEquals(expected,ids.size());
        assertEquals(403,api("GET",B,"/organizations/page?limit=17","alice",null).statusCode());
        assertEquals(400,api("GET",A,"/organizations/page?limit=101","alice",null).statusCode());
        assertEquals(400,api("GET",A,"/organizations/page?after=bad","alice",null).statusCode());
    }
    @Test void archiveReactivationAndHistoricalCreateReplayPreserveReferences() throws Exception {
        String createKey=UUID.randomUUID().toString();String body="{\"name\":\"Original institution\"}";
        var original=request("POST",A,"alice",body,createKey);UUID id=UUID.fromString(json.readTree(original.body()).get("id").asText());
        assertEquals(403,update(A,id,"viewer","Denied",false,1,UUID.randomUUID().toString()).statusCode());
        assertEquals(403,update(B,id,"alice","Denied",false,1,UUID.randomUUID().toString()).statusCode());
        assertEquals(404,update(B,id,"bob","Denied",false,1,UUID.randomUUID().toString()).statusCode());
        String updateKey=UUID.randomUUID().toString();var archived=update(A,id,"alice","Renamed institution",false,1,updateKey);
        assertEquals(200,archived.statusCode());
        assertFalse(request("GET",A,"alice",null,null).body().contains(id.toString()));
        assertTrue(api("GET",A,"/organizations/"+id,"alice",null).body().contains("\"active\":false"));
        assertEquals(archived.body(),update(A,id,"alice","Renamed institution",false,1,updateKey).body());
        assertEquals(409,update(A,id,"alice","Different",false,1,updateKey).statusCode());
        assertEquals(original.body(),request("POST",A,"alice",body,createKey).body());
        assertEquals(200,update(A,id,"alice","Restored institution",true,2,UUID.randomUUID().toString()).statusCode());
        assertEquals(archived.body(),update(A,id,"alice","Renamed institution",false,1,updateKey).body());
    }
    @Test void concurrentVersionsAndDuplicateKeysProduceOneLogicalChange() throws Exception {
        UUID id=create("Concurrent updates");long audits=count("audit_events",A),outbox=count("outbox",A);String key=UUID.randomUUID().toString();
        try(var executor=Executors.newFixedThreadPool(6)) {
            var jobs=new ArrayList<Future<HttpResponse<String>>>();
            for(int i=0;i<6;i++)jobs.add(executor.submit(()->update(A,id,"alice","One version",false,1,key)));
            var results=new HashSet<String>();for(var job:jobs){var r=job.get(20,TimeUnit.SECONDS);assertEquals(200,r.statusCode());results.add(r.body());}assertEquals(1,results.size());
            var first=executor.submit(()->update(A,id,"alice","Winner one",true,2,UUID.randomUUID().toString()));
            var second=executor.submit(()->update(A,id,"alice","Winner two",true,2,UUID.randomUUID().toString()));
            assertEquals(Set.of(200,409),Set.of(first.get().statusCode(),second.get().statusCode()));
        }
        assertEquals(audits+2,count("audit_events",A));assertEquals(outbox+2,count("outbox",A));
        try(var c=admin();var s=c.prepareStatement("UPDATE dzoka.memberships SET active=false WHERE tenant_id=? AND issuer=? AND subject='alice'")) {
            s.setObject(1,A);s.setString(2,issuer);s.executeUpdate();
        }
        try{assertEquals(403,update(A,id,"alice","One version",false,1,key).statusCode());}
        finally{try(var c=admin();var s=c.prepareStatement("UPDATE dzoka.memberships SET active=true WHERE tenant_id=? AND issuer=? AND subject='alice'")){s.setObject(1,A);s.setString(2,issuer);s.executeUpdate();}}
    }
    @Test void updateFailureRollsBackStateReceiptAuditAndEvent() throws Exception {
        UUID id=create("Before rollback");long audits=count("audit_events",A),receipts=count("organization_commands",A);String key=UUID.randomUUID().toString();
        try(var c=admin();var s=c.createStatement()) {
            s.execute("CREATE FUNCTION dzoka.test_fail_org_update() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'Injected failure'; END $$");
            s.execute("CREATE TRIGGER test_org_failure BEFORE INSERT ON dzoka.outbox FOR EACH ROW EXECUTE FUNCTION dzoka.test_fail_org_update()");
        }
        try{assertEquals(503,update(A,id,"alice","After rollback",false,1,key).statusCode());}
        finally{try(var c=admin();var s=c.createStatement()){s.execute("DROP TRIGGER test_org_failure ON dzoka.outbox");}}
        assertEquals(audits,count("audit_events",A));assertEquals(receipts,count("organization_commands",A));
        assertEquals(200,update(A,id,"alice","After rollback",false,1,key).statusCode());
    }
    @Test void operatorProvisionsAndRevokesWithImmutableVersionedReceipts() throws Exception {
        String subject="operator-provisioned-"+UUID.randomUUID();String key=UUID.randomUUID().toString();var command=member(A,subject,true,0,key);
        try(var c=operator()) {
            var receipt=FoundationAdmin.execute(c,command,true);assertEquals(1,receipt.version());
            assertEquals(receipt,FoundationAdmin.execute(c,command,true));
            assertThrows(Failures.Conflict.class,()->FoundationAdmin.execute(c,member(A,subject,false,0,key),true));
            assertThrows(Failures.Conflict.class,()->FoundationAdmin.execute(c,member(A,subject,false,0,UUID.randomUUID().toString()),true));
            assertEquals(200,request("GET",A,subject,null,null).statusCode());
            assertEquals(403,request("GET",B,subject,null,null).statusCode());
            assertEquals(2,FoundationAdmin.execute(c,member(A,subject,false,1,UUID.randomUUID().toString()),true).version());
            assertEquals(403,request("GET",A,subject,null,null).statusCode());
            assertThrows(SQLException.class,()->{try(var s=c.prepareStatement("DELETE FROM dzoka.foundation_admin_operations WHERE tenant_id=?")){s.setObject(1,A);s.executeUpdate();}});
        }
        try(var c=DriverManager.getConnection(url,"dzoka_app",required("DZOKA_TEST_APP_PASSWORD"))) {
            assertThrows(Failures.Forbidden.class,()->FoundationAdmin.execute(c,command,true));
            assertThrows(SQLException.class,()->{try(var s=c.createStatement()){s.executeQuery("SELECT * FROM dzoka.foundation_admin_operations");}});
        }
    }
    @Test void operatorReceiptFailureRollsBackMembershipAndCanRetry() throws Exception {
        var command=member(A,"rollback-"+UUID.randomUUID(),true,0,UUID.randomUUID().toString());
        try(var c=admin();var s=c.createStatement()) {
            s.execute("CREATE TRIGGER test_operator_failure BEFORE INSERT ON dzoka.foundation_admin_operations FOR EACH ROW EXECUTE FUNCTION dzoka.reject_foundation_receipt_change()");
        }
        try(var c=operator()){assertThrows(SQLException.class,()->FoundationAdmin.execute(c,command,true));}
        finally{try(var c=admin();var s=c.createStatement()){s.execute("DROP TRIGGER test_operator_failure ON dzoka.foundation_admin_operations");}}
        assertEquals(403,request("GET",A,command.subject(),null,null).statusCode());
        try(var c=operator()){assertEquals(1,FoundationAdmin.execute(c,command,true).version());}
    }
    @Test void realOperatorProcessCreatesAndDisablesTenantWithoutApiAuthority() throws Exception {
        UUID tenant=UUID.randomUUID();String key=UUID.randomUUID().toString();
        var command=new FoundationAdmin.Command("TENANT",tenant,"Synthetic operator tenant",null,null,null,true,0L,key);
        var javaExe=java.nio.file.Path.of(System.getProperty("java.home"),"bin",System.getProperty("os.name").startsWith("Windows")?"java.exe":"java").toString();
        var builder=new ProcessBuilder(javaExe,"-cp",System.getProperty("java.class.path"),FoundationAdmin.class.getName());
        builder.environment().put("DZOKA_MIGRATION_URL",url);builder.environment().put("DZOKA_MIGRATION_USER",required("DZOKA_TEST_MIGRATION_USER"));builder.environment().put("DZOKA_MIGRATION_PASSWORD",required("DZOKA_TEST_MIGRATION_PASSWORD"));
        Process child=builder.start();try {
            try(var input=child.getOutputStream()){input.write(json.writeValueAsBytes(command));}
            assertTrue(child.waitFor(20,TimeUnit.SECONDS));assertEquals(0,child.exitValue());
            var result=json.readTree(child.getInputStream().readAllBytes());assertEquals(1,result.get("version").asInt());assertFalse(result.has("name"));
        } finally {if(child.isAlive())child.destroyForcibly();}
        try(var c=operator()) {
            assertEquals(2,FoundationAdmin.execute(c,new FoundationAdmin.Command("TENANT",tenant,"Disabled synthetic tenant",null,null,null,false,1L,UUID.randomUUID().toString()),false).version());
            assertEquals(1,FoundationAdmin.execute(c,member(tenant,"new-member",true,0,UUID.randomUUID().toString()),true).version());
            assertEquals(403,request("GET",tenant,"new-member",null,null).statusCode());
            FoundationAdmin.execute(c,new FoundationAdmin.Command("TENANT",tenant,"Enabled synthetic tenant",null,null,null,true,2L,UUID.randomUUID().toString()),false);
            assertEquals(200,request("GET",tenant,"new-member",null,null).statusCode());
        }
    }
    @Test void signingKeyRotationSupportsOverlapAndRejectsUnknownKeys() throws Exception {
        var old=key;String oldToken=token("alice");var next=new RSAKeyGenerator(2048).keyID("rotated-fixture-key").generate();
        jwks.removeContext("/jwks");byte[] keys=new JWKSet(List.of(old.toPublicJWK(),next.toPublicJWK())).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        jwks.createContext("/jwks",exchange->{exchange.sendResponseHeaders(200,keys.length);try(var output=exchange.getResponseBody()){output.write(keys);}});
        key=next;
        try {
            assertEquals(200,request("GET",A,"alice",null,null).statusCode());
            assertEquals(200,rawRequest("GET",A,oldToken,null,null).statusCode());
            var unknown=new RSAKeyGenerator(2048).keyID("unpublished-fixture-key").generate();key=unknown;
            assertEquals(401,rawRequest("GET",A,token("alice",issuer,"dzoka-core",Instant.now().plusSeconds(300),unknown),null,null).statusCode());
        } finally {key=old;}
    }
    @Test void populatedV25UpgradeBackfillsOriginalIdempotencyResponses() throws Exception {
        String database="dzoka_m2_upgrade_"+UUID.randomUUID().toString().replace("-","");
        String upgradeUrl=url.replace("/dzoka_test","/"+database);
        try(var c=admin();var s=c.createStatement()){s.execute("CREATE DATABASE "+database+" OWNER dzoka_migrator");}
        try {
            org.flywaydb.core.Flyway.configure().dataSource(upgradeUrl,required("DZOKA_TEST_MIGRATION_USER"),required("DZOKA_TEST_MIGRATION_PASSWORD")).target("25").load().migrate();
            UUID tenant=UUID.randomUUID(),organization=UUID.randomUUID();String hash=VerifiedIdentity.digest("synthetic-upgrade");
            try(var c=DriverManager.getConnection(upgradeUrl,adminUser,adminPassword)) {
                try(var s=c.prepareStatement("INSERT INTO dzoka.tenants(id,name) VALUES (?,'Synthetic upgrade tenant')")){s.setObject(1,tenant);s.executeUpdate();}
                try(var s=c.prepareStatement("INSERT INTO dzoka.organizations(tenant_id,id,name,version) VALUES (?,?,'Original upgrade institution',1)")){s.setObject(1,tenant);s.setObject(2,organization);s.executeUpdate();}
                try(var s=c.prepareStatement("INSERT INTO dzoka.idempotency(tenant_id,actor_ref,operation,key_hash,request_hash,result_id) VALUES (?,?,'organizations.create.v1',?,?,?)")) {
                    s.setObject(1,tenant);s.setString(2,hash);s.setString(3,hash);s.setString(4,hash);s.setObject(5,organization);s.executeUpdate();
                }
            }
            var migration=org.flywaydb.core.Flyway.configure().dataSource(upgradeUrl,required("DZOKA_TEST_MIGRATION_USER"),required("DZOKA_TEST_MIGRATION_PASSWORD")).target("26").load();
            assertEquals(1,migration.migrate().migrationsExecuted);migration.validate();assertEquals(0,migration.migrate().migrationsExecuted);
            try(var c=DriverManager.getConnection(upgradeUrl,adminUser,adminPassword);var s=c.prepareStatement("SELECT result_name,result_version FROM dzoka.idempotency WHERE tenant_id=?")) {
                s.setObject(1,tenant);try(var r=s.executeQuery()){assertTrue(r.next());assertEquals("Original upgrade institution",r.getString(1));assertEquals(1,r.getLong(2));}
            }
        } finally {try(var c=admin();var s=c.createStatement()){s.execute("DROP DATABASE "+database);}}
    }
}
