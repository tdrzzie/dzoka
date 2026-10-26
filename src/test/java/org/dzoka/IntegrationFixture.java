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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class IntegrationFixture {
    protected final UUID A = UUID.randomUUID(), B = UUID.randomUUID();
    protected String url, adminUser, adminPassword, issuer, base;
    protected ConfigurableApplicationContext app;
    protected HttpServer jwks;
    protected RSAKey key;
    protected final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    @BeforeAll void start() throws Exception {
        url = required("DZOKA_TEST_ADMIN_URL"); adminUser = required("DZOKA_TEST_ADMIN_USER");
        adminPassword = required("DZOKA_TEST_ADMIN_PASSWORD");
        var flyway = Flyway.configure().dataSource(url, required("DZOKA_TEST_MIGRATION_USER"),
                required("DZOKA_TEST_MIGRATION_PASSWORD")).cleanDisabled(true).load();
        flyway.migrate();
        assertEquals(0, flyway.migrate().migrationsExecuted);
        flyway.validate();
        key = new RSAKeyGenerator(2048).keyID("ephemeral-integration-key").generate();
        jwks = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        byte[] publicKeys = new JWKSet(key.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
        jwks.createContext("/jwks", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, publicKeys.length);
            try (var output = exchange.getResponseBody()) { output.write(publicKeys); }
        });
        jwks.start(); issuer = "http://127.0.0.1:" + jwks.getAddress().getPort();
        try (var c = admin(); var s = c.prepareStatement("INSERT INTO dzoka.tenants(id,name) VALUES (?,?),(?,?)")) {
            s.setObject(1,A); s.setString(2,"Synthetic tenant A"); s.setObject(3,B); s.setString(4,"Synthetic tenant B"); s.executeUpdate();
        }
        membership(A,"alice","organizations:read","organizations:write");
        membership(B,"bob","organizations:read","organizations:write");
        membership(A,"viewer","organizations:read");
        membership(A,"carol","organizations:read","organizations:write");
        membership(A,"worker","events:publish","events:consume");
        try (var c=admin(); var s=c.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS dzoka.test_effects(tenant_id uuid NOT NULL,id uuid NOT NULL, PRIMARY KEY(tenant_id,id))");
            s.execute("ALTER TABLE dzoka.test_effects ENABLE ROW LEVEL SECURITY");
            s.execute("ALTER TABLE dzoka.test_effects FORCE ROW LEVEL SECURITY");
            s.execute("DROP POLICY IF EXISTS test_scope ON dzoka.test_effects");
            s.execute("CREATE POLICY test_scope ON dzoka.test_effects TO dzoka_app USING(tenant_id=dzoka.current_tenant() AND dzoka.permitted('events:consume')) WITH CHECK(tenant_id=dzoka.current_tenant() AND dzoka.permitted('events:consume'))");
            s.execute("GRANT INSERT,SELECT ON dzoka.test_effects TO dzoka_app");
        }
        initializeProviders();
        var options=new ArrayList<String>(List.of(
                "--server.port=0", "--server.address=127.0.0.1", "--spring.main.banner-mode=off",
                "--spring.datasource.url="+url, "--spring.datasource.username=dzoka_app",
                "--spring.datasource.password="+required("DZOKA_TEST_APP_PASSWORD"),
                "--spring.datasource.hikari.maximum-pool-size=6",
                "--dzoka.security.issuer="+issuer, "--dzoka.security.jwk-set-uri="+issuer+"/jwks",
                "--dzoka.security.audience=dzoka-core", "--dzoka.security.allow-insecure-loopback=true"));
        options.addAll(extraOptions());
        app=SpringApplication.run(CoreApplication.class,options.toArray(String[]::new));
        base = "http://127.0.0.1:"+((WebServerApplicationContext)app).getWebServer().getPort();
    }
    @AfterAll void stop() { if(app!=null)app.close(); if(jwks!=null)jwks.stop(0); }
    protected static String required(String name) {
        String value=System.getenv(name);
        if(value==null||value.isBlank())throw new IllegalStateException("Integration environment missing: "+name);
        return value;
    }
    protected Connection admin() throws SQLException { return DriverManager.getConnection(url,adminUser,adminPassword); }
    protected void membership(UUID tenant,String subject,String... permissions) throws SQLException {
        try(var c=admin(); var s=c.prepareStatement("INSERT INTO dzoka.memberships(tenant_id,issuer,subject,permissions) VALUES (?,?,?,?)")) {
            s.setObject(1,tenant);s.setString(2,issuer);s.setString(3,subject);
            s.setArray(4,c.createArrayOf("text",permissions));s.executeUpdate();
        }
    }
    protected String token(String subject,String iss,String audience,Instant expires,RSAKey signing) throws Exception {
        var claims=new JWTClaimsSet.Builder().issuer(iss).subject(subject).audience(audience)
                .issueTime(Date.from(Instant.now().minusSeconds(5)))
                .expirationTime(Date.from(expires)).claim("roles",List.of("ADMIN")).build();
        var jwt=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(),claims);
        jwt.sign(new RSASSASigner(signing));return jwt.serialize();
    }
    protected String token(String subject) throws Exception { return token(subject,issuer,"dzoka-core",Instant.now().plusSeconds(300),key); }
    protected HttpResponse<String> request(String method,UUID tenant,String subject,String body,String idem) throws Exception {
        return rawRequest(method,tenant,subject==null?null:token(subject),body,idem);
    }
    protected HttpResponse<String> rawRequest(String method,UUID tenant,String bearer,String body,String idem) throws Exception {
        var builder=HttpRequest.newBuilder(URI.create(base+"/api/v1/tenants/"+tenant+"/organizations"))
                .timeout(Duration.ofSeconds(15));
        if(bearer!=null)builder.header("Authorization","Bearer "+bearer);
        if(idem!=null)builder.header("Idempotency-Key",idem);
        if(body!=null)builder.header("Content-Type","application/json");
        builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body));
        return http.send(builder.build(),HttpResponse.BodyHandlers.ofString());
    }
    protected long count(String table,UUID tenant) throws SQLException {
        // Table names are test constants, never external input.
        try(var c=admin();var s=c.prepareStatement("SELECT count(*) FROM dzoka."+table+" WHERE tenant_id=?")) {
            s.setObject(1,tenant);try(var r=s.executeQuery()){r.next();return r.getLong(1);}
        }
    }
    protected void initializeProviders() throws Exception {}
    protected List<String> extraOptions(){return List.of();}
}
