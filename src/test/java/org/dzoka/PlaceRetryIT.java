package org.dzoka;

import static org.junit.jupiter.api.Assertions.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.sql.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.places.domain.*;
import org.dzoka.places.adapters.PlaceCommandHash;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/** Separate real API JVMs share only the disposable database and ephemeral JWKS. */
class PlaceRetryIT extends IntegrationFixture {
    private final ObjectMapper json=new ObjectMapper();
    @Override protected void initializeProviders()throws Exception {
        membership(A,"place-manager","places:manage","places:read");membership(A,"other-manager","places:manage","places:read");
        membership(B,"remote-manager","places:manage","places:read");
    }
    private PlaceDraft draft(String name,long version){return new PlaceDraft(name,"FACILITY",null,null,null,null,"Africa/Harare",List.of("Synthetic alias"),true,version);}
    private final class Replica implements AutoCloseable {
        final Process child;final String origin;
        Replica(String databaseUrl)throws Exception {
            int port;try(var socket=new java.net.ServerSocket(0,0,InetAddress.getByName("127.0.0.1"))){port=socket.getLocalPort();}
            origin="http://127.0.0.1:"+port;
            String javaExe=Path.of(System.getProperty("java.home"),"bin",System.getProperty("os.name").startsWith("Windows")?"java.exe":"java").toString();
            var builder=new ProcessBuilder(javaExe,"-cp",System.getProperty("java.class.path"),"org.dzoka.CoreApplication",
                "--server.port="+port,"--server.address=127.0.0.1","--spring.main.banner-mode=off","--logging.level.root=WARN",
                "--dzoka.security.allow-insecure-loopback=true","--spring.datasource.hikari.maximum-pool-size=3");
            var env=builder.environment();env.keySet().removeIf(k->k.startsWith("DZOKA_")||k.startsWith("SPRING_")||k.startsWith("SERVER_")||Set.of("JAVA_TOOL_OPTIONS","JDK_JAVA_OPTIONS","_JAVA_OPTIONS").contains(k));
            env.put("DZOKA_DB_URL",databaseUrl);env.put("DZOKA_DB_USER","dzoka_app");env.put("DZOKA_DB_PASSWORD",required("DZOKA_TEST_APP_PASSWORD"));
            env.put("DZOKA_OIDC_ISSUER",issuer);env.put("DZOKA_OIDC_JWK_SET_URI",issuer+"/jwks");env.put("DZOKA_OIDC_AUDIENCE","dzoka-core");env.put("DZOKA_MEDIA_ENABLED","false");
            Path log=Files.createTempFile(Path.of(".test-tmp"),"place-api-replica-",".log");builder.redirectErrorStream(true).redirectOutput(log.toFile());child=builder.start();
            try {
                long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(60);
                while(System.nanoTime()<deadline && child.isAlive()) {
                    try{if(request(this,"GET",A,"/places","place-manager",null,null).statusCode()==200)return;}catch(java.io.IOException unavailable){}
                    Thread.sleep(100);
                }
                throw new IllegalStateException("Independent API did not become ready; inspect private fixture log");
            } catch(Exception failure){close();throw failure;}
        }
        @Override public void close()throws Exception{child.destroy();if(!child.waitFor(15,TimeUnit.SECONDS)){child.destroyForcibly();assertTrue(child.waitFor(10,TimeUnit.SECONDS));}}
    }
    private HttpResponse<String> request(Replica replica,String method,UUID tenant,String path,String subject,PlaceDraft d,String key)throws Exception {
        var b=HttpRequest.newBuilder(URI.create(replica.origin+"/api/v1/tenants/"+tenant+path)).timeout(Duration.ofSeconds(method.equals("GET")?2:15)).header("Authorization","Bearer "+token(subject));
        if(key!=null)b.header("Idempotency-Key",key);
        if(d!=null)b.header("Content-Type","application/json");
        return http.send(b.method(method,d==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(d))).build(),HttpResponse.BodyHandlers.ofString());
    }
    private PlaceView success(HttpResponse<String> response){assertEquals(200,response.statusCode());return json.readValue(response.body(),PlaceView.class);}
    @Test void realHttpReplicasAndRestartReturnTheSameCreateAndUpdateReceipts()throws Exception {
        long before=count("places",A),events=count("outbox",A);String createKey=UUID.randomUUID().toString(),updateKey=UUID.randomUUID().toString();var d=draft("Replica institution \uD83D\uDE80",0);
        PlaceView created,updated;
        try(var one=new Replica(url);var two=new Replica(url)) {
            assertNotEquals(one.child.pid(),two.child.pid());
            try(var pool=Executors.newFixedThreadPool(2)){
                var a=pool.submit(()->request(one,"POST",A,"/places","place-manager",d,createKey));var b=pool.submit(()->request(two,"POST",A,"/places","place-manager",d,createKey));
                created=success(a.get(15,TimeUnit.SECONDS));assertEquals(created,success(b.get(15,TimeUnit.SECONDS)));
            }
            var change=draft("Renamed replica institution \uD83D\uDE80",1);updated=success(request(two,"PUT",A,"/places/"+created.id(),"place-manager",change,updateKey));
            assertEquals(updated,success(request(one,"PUT",A,"/places/"+created.id(),"place-manager",change,updateKey)));
            assertEquals(409,request(one,"POST",A,"/places","place-manager",draft("Changed replay",0),createKey).statusCode());
        }
        try(var restarted=new Replica(url)) {
            assertEquals(created,success(request(restarted,"POST",A,"/places","place-manager",d,createKey)));
            assertEquals(updated,success(request(restarted,"PUT",A,"/places/"+created.id(),"place-manager",draft("Renamed replica institution \uD83D\uDE80",1),updateKey)));
            assertEquals(403,request(restarted,"POST",B,"/places","place-manager",d,createKey).statusCode());
        }
        assertEquals(before+1,count("places",A));assertEquals(events+2,count("outbox",A));
        try(var c=admin();var s=c.prepareStatement("SELECT request_hash,request_hash_version FROM dzoka.place_commands WHERE tenant_id=? AND actor_ref=? AND key_hash=?")){
            s.setObject(1,A);s.setString(2,new VerifiedIdentity(issuer,"place-manager").reference());s.setString(3,VerifiedIdentity.digest(createKey));
            try(var r=s.executeQuery()){assertTrue(r.next());assertEquals(2,r.getInt(2));assertEquals(PlaceCommandHash.canonical(null,d),r.getString(1));}
        }
    }
    private String legacy(UUID target,PlaceDraft d,boolean draftFirst){var map=new LinkedHashMap<String,Object>();String id=target==null?"CREATE":target.toString();if(draftFirst){map.put("draft",d);map.put("id",id);}else{map.put("id",id);map.put("draft",d);}return VerifiedIdentity.digest(json.writeValueAsString(map));}
    private void grant(Connection c,UUID tenant,String subject)throws Exception {
        try(var s=c.prepareStatement("INSERT INTO dzoka.memberships(tenant_id,issuer,subject,permissions) VALUES (?,?,?,ARRAY['places:manage','places:read'])")){s.setObject(1,tenant);s.setString(2,issuer);s.setString(3,subject);s.executeUpdate();}
    }
    @Test void populatedV27ReceiptsInBothOrdersSurviveUpgradeReplicasRestartAndRevocation()throws Exception {
        String database="dzoka_place_receipt_"+UUID.randomUUID().toString().replace("-","");String oldUrl=url.replace("/dzoka_test","/"+database);
        try(var c=admin();var s=c.createStatement()){s.execute("CREATE DATABASE "+database+" OWNER dzoka_migrator");}
        var d=draft("Legacy institution \uD83D\uDE80",0);var change=draft("Legacy renamed institution \uD83D\uDE80",1);String[] keys={UUID.randomUUID().toString(),UUID.randomUUID().toString()};UUID[] ids={UUID.randomUUID(),UUID.randomUUID()};String[] hashes={legacy(null,d,false),legacy(null,d,true)};
        String[] updateKeys={UUID.randomUUID().toString(),UUID.randomUUID().toString()};String[] updateHashes={legacy(ids[0],change,false),legacy(ids[1],change,true)};
        assertNotEquals(hashes[0],hashes[1]);
        try {
            Flyway.configure().dataSource(oldUrl,required("DZOKA_TEST_MIGRATION_USER"),required("DZOKA_TEST_MIGRATION_PASSWORD")).target("27").load().migrate();
            try(var c=DriverManager.getConnection(oldUrl,adminUser,adminPassword)) {
                c.setAutoCommit(false);
                try(var s=c.prepareStatement("INSERT INTO dzoka.tenants(id,name) VALUES (?,'Legacy A'),(?,'Legacy B')")){s.setObject(1,A);s.setObject(2,B);s.executeUpdate();}
                grant(c,A,"place-manager");grant(c,A,"other-manager");grant(c,B,"remote-manager");
                for(int i=0;i<2;i++) {
                    try(var s=c.prepareStatement("INSERT INTO dzoka.places(tenant_id,id,version,active) VALUES (?,?,1,true)")){s.setObject(1,A);s.setObject(2,ids[i]);s.executeUpdate();}
                    try(var s=c.prepareStatement("INSERT INTO dzoka.place_revisions(tenant_id,place_id,version,name,normalized_name,kind,active,timezone,actor_ref) VALUES (?,?,1,?,'legacy institution','FACILITY',true,'Africa/Harare',?)")){s.setObject(1,A);s.setObject(2,ids[i]);s.setString(3,d.name());s.setString(4,new VerifiedIdentity(issuer,"place-manager").reference());s.executeUpdate();}
                    try(var s=c.prepareStatement("INSERT INTO dzoka.place_aliases(tenant_id,place_id,version,ordinal,raw_label,normalized_label) VALUES (?,?,1,0,'Synthetic alias','synthetic alias')")){s.setObject(1,A);s.setObject(2,ids[i]);s.executeUpdate();}
                    try(var s=c.prepareStatement("INSERT INTO dzoka.place_commands(tenant_id,actor_ref,key_hash,request_hash,place_id,version) VALUES (?,?,?,?,?,1)")){s.setObject(1,A);s.setString(2,new VerifiedIdentity(issuer,"place-manager").reference());s.setString(3,VerifiedIdentity.digest(keys[i]));s.setString(4,hashes[i]);s.setObject(5,ids[i]);s.executeUpdate();}
                    try(var s=c.prepareStatement("UPDATE dzoka.places SET version=2 WHERE tenant_id=? AND id=?")){s.setObject(1,A);s.setObject(2,ids[i]);s.executeUpdate();}
                    try(var s=c.prepareStatement("INSERT INTO dzoka.place_revisions(tenant_id,place_id,version,name,normalized_name,kind,active,timezone,actor_ref) VALUES (?,?,2,?,'legacy renamed institution','FACILITY',true,'Africa/Harare',?)")){s.setObject(1,A);s.setObject(2,ids[i]);s.setString(3,change.name());s.setString(4,new VerifiedIdentity(issuer,"place-manager").reference());s.executeUpdate();}
                    try(var s=c.prepareStatement("INSERT INTO dzoka.place_aliases(tenant_id,place_id,version,ordinal,raw_label,normalized_label) VALUES (?,?,2,0,'Synthetic alias','synthetic alias')")){s.setObject(1,A);s.setObject(2,ids[i]);s.executeUpdate();}
                    try(var s=c.prepareStatement("INSERT INTO dzoka.place_commands(tenant_id,actor_ref,key_hash,request_hash,place_id,version) VALUES (?,?,?,?,?,2)")){s.setObject(1,A);s.setString(2,new VerifiedIdentity(issuer,"place-manager").reference());s.setString(3,VerifiedIdentity.digest(updateKeys[i]));s.setString(4,updateHashes[i]);s.setObject(5,ids[i]);s.executeUpdate();}
                }c.commit();
            }
            var migration=Flyway.configure().dataSource(oldUrl,required("DZOKA_TEST_MIGRATION_USER"),required("DZOKA_TEST_MIGRATION_PASSWORD")).target("28").load();assertEquals(1,migration.migrate().migrationsExecuted);migration.validate();assertEquals(0,migration.migrate().migrationsExecuted);
            try(var one=new Replica(oldUrl);var two=new Replica(oldUrl)) {
                for(int i=0;i<2;i++) {
                    var a=success(request(one,"POST",A,"/places","place-manager",d,keys[i]));var b=success(request(two,"POST",A,"/places","place-manager",d,keys[i]));assertEquals(ids[i],a.id());assertEquals(a,b);
                    assertEquals(409,request(two,"POST",A,"/places","place-manager",draft("Changed legacy",0),keys[i]).statusCode());
                    var updated=success(request(one,"PUT",A,"/places/"+ids[i],"place-manager",change,updateKeys[i]));
                    assertEquals(2,updated.version());assertEquals(change.name(),updated.name());assertEquals(updated,success(request(two,"PUT",A,"/places/"+ids[i],"place-manager",change,updateKeys[i])));
                    assertEquals(409,request(two,"PUT",A,"/places/"+ids[i],"place-manager",draft("Changed legacy update",1),updateKeys[i]).statusCode());
                }
                assertNotEquals(ids[0],success(request(two,"POST",A,"/places","other-manager",d,keys[0])).id());
                assertNotEquals(ids[0],success(request(one,"POST",B,"/places","remote-manager",d,keys[0])).id());
                try(var c=DriverManager.getConnection(oldUrl,adminUser,adminPassword);var s=c.prepareStatement("UPDATE dzoka.memberships SET active=false WHERE tenant_id=? AND issuer=? AND subject='place-manager'")){s.setObject(1,A);s.setString(2,issuer);s.executeUpdate();}
                assertEquals(403,request(two,"POST",A,"/places","place-manager",d,keys[0]).statusCode());
                try(var c=DriverManager.getConnection(oldUrl,adminUser,adminPassword);var s=c.prepareStatement("UPDATE dzoka.memberships SET active=true WHERE tenant_id=? AND issuer=? AND subject='place-manager'")){s.setObject(1,A);s.setString(2,issuer);s.executeUpdate();}
            }
            try(var restarted=new Replica(oldUrl)){for(int i=0;i<2;i++){assertEquals(ids[i],success(request(restarted,"POST",A,"/places","place-manager",d,keys[i])).id());assertEquals(2,success(request(restarted,"PUT",A,"/places/"+ids[i],"place-manager",change,updateKeys[i])).version());}}
            try(var c=DriverManager.getConnection(oldUrl,adminUser,adminPassword);var s=c.prepareStatement("SELECT request_hash,request_hash_version FROM dzoka.place_commands WHERE tenant_id=? AND actor_ref=? AND key_hash=?")) {
                for(int i=0;i<2;i++){s.setObject(1,A);s.setString(2,new VerifiedIdentity(issuer,"place-manager").reference());s.setString(3,VerifiedIdentity.digest(keys[i]));try(var r=s.executeQuery()){assertTrue(r.next());assertEquals(hashes[i],r.getString(1));assertEquals(1,r.getInt(2));}}
                for(int i=0;i<2;i++){s.setObject(1,A);s.setString(2,new VerifiedIdentity(issuer,"place-manager").reference());s.setString(3,VerifiedIdentity.digest(updateKeys[i]));try(var r=s.executeQuery()){assertTrue(r.next());assertEquals(updateHashes[i],r.getString(1));assertEquals(1,r.getInt(2));}}
            }
        } finally {try(var c=admin();var s=c.createStatement()){s.execute("DROP DATABASE "+database+" WITH (FORCE)");}}
        // Retain both valid legacy formats in the main harness database for its real restore drill.
        var places=app.getBean(org.dzoka.places.application.Places.class);var actor=new VerifiedIdentity(issuer,"place-manager");
        for(boolean draftFirst:List.of(false,true)) {
            String retainedKey=UUID.randomUUID().toString();var retainedDraft=draft("Retained legacy restore fixture",0);
            var original=places.save(actor,A,null,retainedDraft,retainedKey);String retainedHash=legacy(null,retainedDraft,draftFirst);
            try(var c=admin();var s=c.prepareStatement("UPDATE dzoka.place_commands SET request_hash=?,request_hash_version=1 WHERE tenant_id=? AND actor_ref=? AND key_hash=?")){s.setString(1,retainedHash);s.setObject(2,A);s.setString(3,actor.reference());s.setString(4,VerifiedIdentity.digest(retainedKey));assertEquals(1,s.executeUpdate());}
            assertEquals(original,places.save(actor,A,null,retainedDraft,retainedKey));
        }
    }
}
