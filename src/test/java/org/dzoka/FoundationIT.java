package org.dzoka;

import static org.junit.jupiter.api.Assertions.*;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.*;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.dzoka.events.adapters.*;
import org.dzoka.events.application.OutboxDispatcher;
import org.dzoka.foundation.application.Failures;
import org.dzoka.foundation.persistence.*;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.organizations.application.OrganizationService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import javax.sql.DataSource;

class FoundationIT extends IntegrationFixture {
    @Test void signaturesIssuerAudienceExpiryAndAnonymousFailClosed() throws Exception {
        assertEquals(401,request("GET",A,null,null,null).statusCode());
        for(String bearer:List.of("malformed",token("alice",issuer+"/wrong","dzoka-core",Instant.now().plusSeconds(300),key),
                token("alice",issuer,"other-api",Instant.now().plusSeconds(300),key),
                token("alice",issuer,"dzoka-core",Instant.now().minusSeconds(120),key),
                token("alice",issuer,"dzoka-core",Instant.now().plusSeconds(300),new RSAKeyGenerator(2048).generate())))
            assertEquals(401,rawRequest("GET",A,bearer,null,null).statusCode());
        assertEquals(200,request("GET",A,"alice",null,null).statusCode());
    }
    @Test void tokenAdminRoleCannotGrantMembershipOrWrite() throws Exception {
        assertEquals(403,request("GET",A,"stranger",null,null).statusCode());
        assertEquals(403,request("POST",A,"viewer","{\"name\":\"No privilege\"}","viewer-key").statusCode());
        assertEquals(403,request("GET",B,"alice",null,null).statusCode());
    }
    @Test void missingRequiredClaimsAndUnsignedTokensAreRejected() throws Exception {
        var claims=new JWTClaimsSet.Builder().issuer(issuer).subject("alice").audience("dzoka-core").build();
        var missingExpiry=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(),claims);
        missingExpiry.sign(new RSASSASigner(key));
        assertEquals(401,rawRequest("GET",A,missingExpiry.serialize(),null,null).statusCode());
        var noAudience=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(),
                new JWTClaimsSet.Builder().issuer(issuer).subject("alice").expirationTime(Date.from(Instant.now().plusSeconds(300))).build());
        noAudience.sign(new RSASSASigner(key));
        assertEquals(401,rawRequest("GET",A,noAudience.serialize(),null,null).statusCode());
        assertEquals(401,rawRequest("GET",A,new PlainJWT(claims).serialize(),null,null).statusCode());
    }
    @Test void invalidAndOversizedJsonFailsSafely() throws Exception {
        assertEquals(400,request("POST",A,"alice","{\"name\":\"Name\",\"role\":\"ADMIN\"}",UUID.randomUUID().toString()).statusCode());
        assertEquals(400,request("POST",A,"alice","{\"name\":\" \"}",UUID.randomUUID().toString()).statusCode());
        assertEquals(413,request("POST",A,"alice","{\"name\":\""+"x".repeat(20000)+"\"}",UUID.randomUUID().toString()).statusCode());
        var chunked=HttpRequest.newBuilder(URI.create(base+"/api/v1/tenants/"+A+"/organizations"))
                .timeout(Duration.ofSeconds(15)).header("Authorization","Bearer "+token("alice"))
                .header("Content-Type","application/json").header("Idempotency-Key",UUID.randomUUID().toString())
                .POST(HttpRequest.BodyPublishers.ofInputStream(()->new java.io.ByteArrayInputStream(
                        ("{\"name\":\""+"x".repeat(20000)+"\"}").getBytes(StandardCharsets.UTF_8)))).build();
        assertEquals(413,http.send(chunked,HttpResponse.BodyHandlers.ofString()).statusCode());
    }
    @Test void runtimeRejectsMigrationOrSuperuserCredentials() throws Exception {
        var privileged=new org.springframework.jdbc.datasource.DriverManagerDataSource(url,adminUser,adminPassword);
        assertThrows(IllegalStateException.class,()->new RuntimeDatabaseGuard(privileged).afterPropertiesSet());
        var owner=new org.springframework.jdbc.datasource.DriverManagerDataSource(url,
                required("DZOKA_TEST_MIGRATION_USER"),required("DZOKA_TEST_MIGRATION_PASSWORD"));
        assertThrows(IllegalStateException.class,()->new RuntimeDatabaseGuard(owner).afterPropertiesSet());
    }
    @Test void tenantApiIsolationAndRevokedMembership() throws Exception {
        String name="Only-A-"+UUID.randomUUID();
        assertEquals(200,request("POST",A,"alice","{\"name\":\""+name+"\"}",UUID.randomUUID().toString()).statusCode());
        assertFalse(request("GET",B,"bob",null,null).body().contains(name));
        membership(A,"revoked","organizations:read");
        try(var c=admin();var s=c.prepareStatement("UPDATE dzoka.memberships SET active=false WHERE tenant_id=? AND subject='revoked'")) {
            s.setObject(1,A);s.executeUpdate();
        }
        assertEquals(403,request("GET",A,"revoked",null,null).statusCode());
        try(var c=admin();var s=c.prepareStatement("UPDATE dzoka.tenants SET active=false WHERE id=?")){
            s.setObject(1,A);s.executeUpdate();
        }
        try {assertEquals(403,request("GET",A,"alice",null,null).statusCode());}
        finally {try(var c=admin();var s=c.prepareStatement("UPDATE dzoka.tenants SET active=true WHERE id=?")){
            s.setObject(1,A);s.executeUpdate();
        }}
    }
    @Test void idempotencyIsBoundToVerifiedActorAtTheDatabaseBoundary() throws Exception {
        String key=UUID.randomUUID().toString(),body="{\"name\":\"Actor scope fixture\"}";
        var alice=request("POST",A,"alice",body,key);var carol=request("POST",A,"carol",body,key);
        assertEquals(200,alice.statusCode());assertEquals(200,carol.statusCode());assertNotEquals(alice.body(),carol.body());
        var tx=app.getBean(TenantTransactions.class);var actor=new VerifiedIdentity(issuer,"carol");
        tx.execute(actor,A,"organizations:write",c->{
            try(var s=c.prepareStatement("SELECT count(*) FROM dzoka.idempotency WHERE actor_ref=?")){
                s.setString(1,new VerifiedIdentity(issuer,"alice").reference());
                try(var r=s.executeQuery()){r.next();assertEquals(0,r.getInt(1));}
            }
            try(var s=c.createStatement();var r=s.executeQuery("SELECT dzoka.current_actor_ref()")){
                r.next();assertEquals(actor.reference(),r.getString(1));
            }
            return null;
        });
    }
    @Test void idempotentConcurrencyCommitsOneOrganizationAuditAndOutbox() throws Exception {
        long organizations=count("organizations",A),audits=count("audit_events",A),events=count("outbox",A);
        String idem=UUID.randomUUID().toString(),body="{\"name\":\"Concurrent synthetic organization\"}";
        try(var executor=Executors.newFixedThreadPool(6)) {
            var jobs=new ArrayList<Future<HttpResponse<String>>>();
            for(int i=0;i<6;i++)jobs.add(executor.submit(()->request("POST",A,"alice",body,idem)));
            Set<String> responses=new HashSet<>();
            for(var job:jobs){var response=job.get(20,TimeUnit.SECONDS);assertEquals(200,response.statusCode());responses.add(response.body());}
            assertEquals(1,responses.size());
        }
        assertEquals(organizations+1,count("organizations",A));assertEquals(audits+1,count("audit_events",A));assertEquals(events+1,count("outbox",A));
        assertEquals(409,request("POST",A,"alice","{\"name\":\"Different body\"}",idem).statusCode());
        assertEquals(200,request("POST",B,"bob",body,idem).statusCode());
    }
    @Test void rowPoliciesFailClosedAndPooledContextDoesNotLeak() throws Exception {
        var config=new com.zaxxer.hikari.HikariConfig();
        config.setJdbcUrl(url);config.setUsername("dzoka_app");config.setPassword(required("DZOKA_TEST_APP_PASSWORD"));
        config.setMaximumPoolSize(1);
        try(var isolatedPool=new com.zaxxer.hikari.HikariDataSource(config)) {
        var tx=new TenantTransactions(isolatedPool);
        int[] backend={0};
        tx.execute(new VerifiedIdentity(issuer,"alice"),A,"organizations:read",c->{
            try(var s=c.createStatement();var r=s.executeQuery("SELECT pg_backend_pid()")){r.next();backend[0]=r.getInt(1);}
            try(var s=c.prepareStatement("SELECT count(*) FROM dzoka.organizations WHERE tenant_id=?")){
                s.setObject(1,B);try(var r=s.executeQuery()){r.next();assertEquals(0,r.getInt(1));}
            }
            return null;
        });
        try(var c=isolatedPool.getConnection();var s=c.createStatement()) {
            try(var r=s.executeQuery("SELECT pg_backend_pid()")){r.next();assertEquals(backend[0],r.getInt(1));}
            try(var r=s.executeQuery("SELECT count(*) FROM dzoka.organizations")){r.next();assertEquals(0,r.getInt(1));}
            assertThrows(SQLException.class,()->s.executeUpdate("INSERT INTO dzoka.organizations VALUES ('"+A+"','"+UUID.randomUUID()+"','Denied',1,now())"));
            assertThrows(SQLException.class,()->s.executeUpdate("UPDATE dzoka.memberships SET permissions=ARRAY['organizations:write']"));
            assertThrows(SQLException.class,()->s.execute("SET ROLE dzoka_migrator"));
            assertThrows(SQLException.class,()->s.execute("ALTER TABLE dzoka.organizations DISABLE ROW LEVEL SECURITY"));
            assertThrows(SQLException.class,()->s.execute("DELETE FROM dzoka.audit_events"));
        }
        }
        assertEquals(200,request("GET",B,"bob",null,null).statusCode());
    }
    @Test void inboxConcurrentRedeliveryAppliesOneEffect() throws Exception {
        var inbox=app.getBean(JdbcInbox.class);var worker=new VerifiedIdentity(issuer,"worker");
        UUID event=UUID.randomUUID(),effect=UUID.randomUUID();long before=count("test_effects",A);
        try(var executor=Executors.newFixedThreadPool(6)) {
            var start=new CountDownLatch(1);var results=new ArrayList<Future<Boolean>>();
            for(int i=0;i<6;i++)results.add(executor.submit(()->{
                start.await();return inbox.process(worker,A,"concurrent.consumer",event,"same-envelope",c->{
                    try(var s=c.prepareStatement("INSERT INTO dzoka.test_effects VALUES (?,?)")){
                        s.setObject(1,A);s.setObject(2,effect);s.executeUpdate();}return null;
                });
            }));
            start.countDown();int applied=0;
            for(var result:results)if(result.get(15,TimeUnit.SECONDS))applied++;
            assertEquals(1,applied);
        }
        assertEquals(before+1,count("test_effects",A));
    }
    @Test void failedOutboxInsertRollsBackEntireCommandAndRetrySucceeds() throws Exception {
        String idem=UUID.randomUUID().toString();
        long before=count("organizations",A),audit=count("audit_events",A);
        try(var c=admin();var s=c.createStatement()) {
            s.execute("CREATE FUNCTION dzoka.test_fail_event() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'Injected event failure'; END $$");
            s.execute("CREATE TRIGGER test_failure BEFORE INSERT ON dzoka.outbox FOR EACH ROW EXECUTE FUNCTION dzoka.test_fail_event()");
        }
        try {assertEquals(503,request("POST",A,"alice","{\"name\":\"Rollback fixture\"}",idem).statusCode());}
        finally {try(var c=admin();var s=c.createStatement()){s.execute("DROP TRIGGER test_failure ON dzoka.outbox");}}
        assertEquals(before,count("organizations",A));assertEquals(audit,count("audit_events",A));
        assertEquals(200,request("POST",A,"alice","{\"name\":\"Rollback fixture\"}",idem).statusCode());
    }
    @Test void inboxDuplicatesConflictAndCrashRollbackShareOneTransaction() throws Exception {
        var inbox=app.getBean(JdbcInbox.class);var worker=new VerifiedIdentity(issuer,"worker");UUID event=UUID.randomUUID(),effect=UUID.randomUUID();
        TenantTransactions.Work<Void> write=c->{try(var s=c.prepareStatement("INSERT INTO dzoka.test_effects VALUES (?,?)")){s.setObject(1,A);s.setObject(2,effect);s.executeUpdate();}return null;};
        assertThrows(IllegalStateException.class,()->inbox.process(worker,A,"test.consumer",event,"envelope",c->{write.run(c);throw new IllegalStateException("crash before commit");}));
        assertTrue(inbox.process(worker,A,"test.consumer",event,"envelope",write));
        assertFalse(inbox.process(worker,A,"test.consumer",event,"envelope",c->{fail("duplicate effect");return null;}));
        assertThrows(Failures.Conflict.class,()->inbox.process(worker,A,"test.consumer",event,"changed envelope",write));
        assertThrows(Failures.Forbidden.class,()->inbox.process(worker,B,"test.consumer",event,"envelope",write));
    }
    @Test void outboxExpiredLeaseRejectsStaleAckAndAmbiguousPublishCanRedeliver() throws Exception {
        app.getBean(OrganizationService.class).create(new VerifiedIdentity(issuer,"alice"),A,"Outbox fixture",UUID.randomUUID().toString());
        app.getBean(OrganizationService.class).create(new VerifiedIdentity(issuer,"alice"),A,"Second outbox fixture",UUID.randomUUID().toString());
        var outbox=app.getBean(JdbcOutbox.class);var worker=new VerifiedIdentity(issuer,"worker");
        var first=outbox.claim(worker,A,Duration.ofSeconds(30)).orElseThrow();
        try(var c=admin();var s=c.prepareStatement("UPDATE dzoka.outbox SET lease_until=now()-interval '1 second' WHERE tenant_id=? AND id=?")){
            s.setObject(1,A);s.setObject(2,first.event().id());s.executeUpdate();
        }
        var second=outbox.claim(worker,A,Duration.ofSeconds(30)).orElseThrow();
        assertEquals(first.event().id(),second.event().id());assertNotEquals(first.leaseToken(),second.leaseToken());
        assertFalse(outbox.acknowledge(worker,first));assertTrue(outbox.acknowledge(worker,second));
        assertFalse(outbox.acknowledge(worker,second));
        var received=new ArrayList<UUID>();
        var dispatcher=new OutboxDispatcher(outbox,e->{received.add(e.id());throw new java.io.IOException("ambiguous transport failure");});
        assertThrows(java.io.IOException.class,()->dispatcher.dispatchOne(worker,A));
        assertEquals(1,received.size());
        try(var c=admin();var s=c.prepareStatement("UPDATE dzoka.outbox SET lease_until=now()-interval '1 second' WHERE id=?")){s.setObject(1,received.getFirst());s.executeUpdate();}
        var retried=outbox.claim(worker,A,Duration.ofSeconds(30)).orElseThrow();
        assertEquals(received.getFirst(),retried.event().id());assertTrue(outbox.acknowledge(worker,retried));
    }
    @Test void databaseConnectionFailureDoesNotProduceSuccess() throws Exception {
        try(var c=admin();var s=c.createStatement()){s.execute("ALTER ROLE dzoka_app NOLOGIN");}
        try {
            app.getBean(com.zaxxer.hikari.HikariDataSource.class).getHikariPoolMXBean().softEvictConnections();
            assertEquals(503,request("GET",A,"alice",null,null).statusCode());
        } finally {try(var c=admin();var s=c.createStatement()){s.execute("ALTER ROLE dzoka_app LOGIN");}}
    }
}
