package org.dzoka;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.*;
import java.sql.*;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.foundation.application.Failures;
import org.dzoka.places.application.Places;
import org.dzoka.places.domain.*;
import org.dzoka.intake.application.IntakeService;
import org.dzoka.intake.domain.ReportDraft;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
class PlacesIT extends IntegrationFixture {
    @Override protected void initializeProviders()throws Exception{
        membership(A,"place-manager","places:manage","places:read","reports:write","reports:read");
        membership(A,"place-reader","places:read");membership(B,"remote-place-manager","places:manage","places:read","reports:write","reports:read");
    }
    private Places places(){return app.getBean(Places.class);}
    private VerifiedIdentity who(String subject){return new VerifiedIdentity(issuer,subject);}
    private PlaceDraft draft(String name,UUID parent,boolean active,long version,List<String> aliases){return new PlaceDraft(name,"FACILITY",parent,null,null,null,"Africa/Harare",aliases,active,version);}
    private PlaceView create(String name,UUID parent){return places().save(who("place-manager"),A,null,draft(name,parent,true,0,List.of()),UUID.randomUUID().toString());}
    @Test void rawAliasesAmbiguityAndTenantBoundariesAreRealHttpContracts()throws Exception{
        var json=app.getBean(ObjectMapper.class);var body=draft("  Cafe\u0301  ",null,true,0,List.of("  CHITORO  "));
        var response=api("POST",A,"/places","place-manager",json.writeValueAsString(body));assertEquals(200,response.statusCode());var p=json.readValue(response.body(),PlaceView.class);
        places().save(who("place-manager"),A,null,draft("Another institution",null,true,0,List.of("chitoro")),UUID.randomUUID().toString());
        var found=api("GET",A,"/places?label=CHITORO&limit=1","place-reader",null);assertEquals(200,found.statusCode());var page=json.readTree(found.body());assertEquals(1,page.get("items").size());assertFalse(page.get("nextCursor").isNull());
        var second=api("GET",A,"/places?label=chitoro&limit=1&after="+page.get("nextCursor").asText(),"place-reader",null);assertEquals(1,json.readTree(second.body()).get("items").size());
        assertEquals("  Cafe\u0301  ",p.name());assertEquals(List.of("  CHITORO  "),p.aliases());
        assertEquals(403,api("POST",A,"/places","place-reader",json.writeValueAsString(body)).statusCode());
        assertEquals(403,api("GET",B,"/places/"+p.id(),"place-reader",null).statusCode());
        assertEquals(404,api("GET",B,"/places/"+p.id(),"remote-place-manager",null).statusCode());
        assertEquals(400,api("GET",A,"/places?limit=101","place-reader",null).statusCode());
        try(var c=admin();var s=c.prepareStatement("SELECT normalized_name,normalization_version FROM dzoka.place_revisions WHERE tenant_id=? AND place_id=?")){s.setObject(1,A);s.setObject(2,p.id());try(var r=s.executeQuery()){assertTrue(r.next());assertEquals("café",r.getString(1));assertEquals("unicode-nfc-whitespace-rootcase-v1",r.getString(2));}}
    }
    @Test void historyArchiveAndVersionedRetriesCannotSilentlyReplacePlaceEvidence()throws Exception{
        String key=UUID.randomUUID().toString();var d=draft("Original building",null,true,0,List.of("Old label"));var initial=places().save(who("place-manager"),A,null,d,key);
        var changed=places().save(who("place-manager"),A,initial.id(),draft("Renamed building",null,true,1,List.of("New label")),UUID.randomUUID().toString());assertEquals(2,changed.version());
        assertEquals(initial,places().save(who("place-manager"),A,null,d,key));assertEquals(initial,places().get(who("place-reader"),A,initial.id(),1L));
        assertThrows(Failures.Conflict.class,()->places().save(who("place-manager"),A,null,draft("Changed body",null,true,0,List.of()),key));
        assertThrows(Failures.Conflict.class,()->places().save(who("place-manager"),A,initial.id(),draft("Stale",null,true,1,List.of()),UUID.randomUUID().toString()));
        places().save(who("place-manager"),A,initial.id(),draft("Renamed building",null,false,2,List.of()),UUID.randomUUID().toString());
        assertTrue(places().page(who("place-reader"),A,null,100,"Renamed building",false).items().isEmpty());
        assertEquals(1,places().page(who("place-reader"),A,null,100,"Renamed building",true).items().size());
        places().save(who("place-manager"),A,initial.id(),draft("Reactivated",null,true,3,List.of()),UUID.randomUUID().toString());
    }
    @Test void concurrentCyclesAndDuplicateCommandsRespectOneTenantForest()throws Exception{
        var one=create("Hierarchy one",null);var two=create("Hierarchy two",null);
        try(var pool=Executors.newFixedThreadPool(2)){
            var a=pool.submit(()->{try{places().save(who("place-manager"),A,one.id(),draft("Hierarchy one",two.id(),true,1,List.of()),UUID.randomUUID().toString());return true;}catch(Failures.Conflict conflict){return false;}});
            var b=pool.submit(()->{try{places().save(who("place-manager"),A,two.id(),draft("Hierarchy two",one.id(),true,1,List.of()),UUID.randomUUID().toString());return true;}catch(Failures.Conflict conflict){return false;}});
            assertNotEquals(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS));
            assertTrue(places().ancestors(who("place-reader"),A,one.id(),null).size()<=2);
            String key=UUID.randomUUID().toString();var d=draft("Duplicate root",null,true,0,List.of());
            var x=pool.submit(()->places().save(who("place-manager"),A,null,d,key));var y=pool.submit(()->places().save(who("place-manager"),A,null,d,key));assertEquals(x.get(),y.get());
        }
    }
    @Test void reparentingRetainsHistoricalAncestorChainAndRejectsCrossTenantParents()throws Exception{
        var root=create("Root",null);var alternative=create("Alternative",null);var floor=create("Floor",root.id());var room=create("Room",floor.id());
        assertEquals(List.of(room.id(),floor.id(),root.id()),places().ancestors(who("place-reader"),A,room.id(),1L).stream().map(PlaceView::id).toList());
        places().save(who("place-manager"),A,floor.id(),draft("Floor",alternative.id(),true,1,List.of()),UUID.randomUUID().toString());
        assertEquals(alternative.id(),places().ancestors(who("place-reader"),A,room.id(),null).getLast().id());
        assertEquals(root.id(),places().ancestors(who("place-reader"),A,room.id(),1L).getLast().id());
        assertThrows(Failures.Conflict.class,()->places().save(who("place-manager"),A,alternative.id(),draft("Alternative",null,false,1,List.of()),UUID.randomUUID().toString()));
        var remote=places().save(who("remote-place-manager"),B,null,draft("Remote",null,true,0,List.of()),UUID.randomUUID().toString());
        assertThrows(Failures.NotFound.class,()->create("Invalid parent",remote.id()));
    }
    @Test void maximumDepthAndSubtreeMovesAreBounded()throws Exception{
        PlaceView p=create("Depth root",null);UUID root=p.id();
        for(int i=1;i<32;i++)p=create("Depth "+i,p.id());
        UUID leaf=p.id();assertEquals(32,places().ancestors(who("place-reader"),A,leaf,null).size());
        assertThrows(Failures.Conflict.class,()->create("Too deep",leaf));
        var external=create("Other root",null);
        assertThrows(Failures.Conflict.class,()->places().save(who("place-manager"),A,root,draft("Moved subtree",external.id(),true,1,List.of()),UUID.randomUUID().toString()));
    }
    @Test void reportReferencesAreVersionedAndNeverRewriteRawLocation()throws Exception{
        var place=create("Canonical institution",null);var intake=app.getBean(IntakeService.class);
        var d=new ReportDraft(ReportDraft.Kind.LOST,"Synthetic bag","en","Africa/Harare","near CHITORO?","yesterday",null,null,null,null,null,null,place.id(),1L);
        byte[] raw=app.getBean(ObjectMapper.class).writeValueAsBytes(d);var report=intake.submit(who("place-manager"),A,null,null,d,raw,UUID.randomUUID().toString());
        places().save(who("place-manager"),A,place.id(),draft("Renamed institution",null,true,1,List.of()),UUID.randomUUID().toString());
        var retained=intake.get(who("place-manager"),A,report.reportId(),null);assertEquals("near CHITORO?",retained.rawLocation());assertEquals(place.id(),retained.placeId());assertEquals(1L,retained.placeVersion());
        assertThrows(Failures.Conflict.class,()->intake.submit(who("place-manager"),A,null,null,d,raw,UUID.randomUUID().toString()));
        var foreign=new ReportDraft(ReportDraft.Kind.LOST,"Other","en","UTC",null,null,null,null,null,null,null,null,place.id(),2L);
        assertThrows(Failures.NotFound.class,()->intake.submit(who("remote-place-manager"),B,null,null,foreign,raw,UUID.randomUUID().toString()));
    }
    @Test void outboxFailureRollsBackPlaceAliasesReceiptAndAudit()throws Exception{
        long before=count("places",A),receipts=count("place_commands",A),audits=count("audit_events",A);String key=UUID.randomUUID().toString();var d=draft("Rollback place",null,true,0,List.of("Raw alias"));
        try(var c=admin();var s=c.createStatement()){s.execute("CREATE FUNCTION dzoka.test_place_failure() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'Injected'; END $$");s.execute("CREATE TRIGGER place_failure BEFORE INSERT ON dzoka.outbox FOR EACH ROW EXECUTE FUNCTION dzoka.test_place_failure()");}
        try{assertThrows(Failures.Unavailable.class,()->places().save(who("place-manager"),A,null,d,key));}
        finally{try(var c=admin();var s=c.createStatement()){s.execute("DROP TRIGGER place_failure ON dzoka.outbox");}}
        assertEquals(before,count("places",A));assertEquals(receipts,count("place_commands",A));assertEquals(audits,count("audit_events",A));assertEquals(1,places().save(who("place-manager"),A,null,d,key).version());
    }
    @Test void populatedV26UpgradeRetainsOldOutboxAndReleasedMediaTombstones()throws Exception {
        String database="dzoka_m3_upgrade_"+UUID.randomUUID().toString().replace("-","");String upgradeUrl=url.replace("/dzoka_test","/"+database);
        try(var c=admin();var s=c.createStatement()){s.execute("CREATE DATABASE "+database+" OWNER dzoka_migrator");}
        try {
            org.flywaydb.core.Flyway.configure().dataSource(upgradeUrl,required("DZOKA_TEST_MIGRATION_USER"),required("DZOKA_TEST_MIGRATION_PASSWORD")).target("26").load().migrate();
            UUID tenant=UUID.randomUUID(),organization=UUID.randomUUID(),report=UUID.randomUUID(),item=UUID.randomUUID(),caseId=UUID.randomUUID(),asset=UUID.randomUUID();String hash="a".repeat(64);
            try(var c=DriverManager.getConnection(upgradeUrl,adminUser,adminPassword)) {
                try(var s=c.prepareStatement("INSERT INTO dzoka.tenants(id,name) VALUES (?,'Synthetic migration tenant')")){s.setObject(1,tenant);s.executeUpdate();}
                try(var s=c.prepareStatement("INSERT INTO dzoka.organizations(tenant_id,id,name,version) VALUES (?,?,'Synthetic migration organization',1)")){s.setObject(1,tenant);s.setObject(2,organization);s.executeUpdate();}
                try(var s=c.prepareStatement("INSERT INTO dzoka.outbox(tenant_id,id,event_type,aggregate_id,aggregate_version,payload) VALUES (?,?,'organization.created',?,1,'{}')")){s.setObject(1,tenant);s.setObject(2,UUID.randomUUID());s.setObject(3,organization);s.executeUpdate();}
                try(var s=c.prepareStatement("INSERT INTO dzoka.recovery_cases(tenant_id,id) VALUES (?,?)")){s.setObject(1,tenant);s.setObject(2,caseId);s.executeUpdate();}
                try(var s=c.prepareStatement("INSERT INTO dzoka.items(tenant_id,id,case_id) VALUES (?,?,?)")){s.setObject(1,tenant);s.setObject(2,item);s.setObject(3,caseId);s.executeUpdate();}
                try(var s=c.prepareStatement("INSERT INTO dzoka.reports(tenant_id,id,case_id,item_id,owner_ref,kind,current_revision,expires_at,deleted_at) VALUES (?,?,?,?,?,'FOUND',1,now(),now())")){s.setObject(1,tenant);s.setObject(2,report);s.setObject(3,caseId);s.setObject(4,item);s.setString(5,hash);s.executeUpdate();}
                try(var s=c.prepareStatement("INSERT INTO dzoka.media_assets(tenant_id,id,report_id,owner_ref,upload_key_hash,source_sha256,source_bytes,declared_type,source_key,derivative_key,state,expires_at,reservation_released) VALUES (?,?,?,?,?,?,12,'image/png',?,?,'DELETED',now(),true)")){s.setObject(1,tenant);s.setObject(2,asset);s.setObject(3,report);s.setString(4,hash);s.setString(5,hash);s.setString(6,hash);s.setString(7,tenant+"/"+asset+"/source");s.setString(8,tenant+"/"+asset+"/"+UUID.randomUUID());s.executeUpdate();}
            }
            var migration=org.flywaydb.core.Flyway.configure().dataSource(upgradeUrl,required("DZOKA_TEST_MIGRATION_USER"),required("DZOKA_TEST_MIGRATION_PASSWORD")).target("27").load();assertEquals(1,migration.migrate().migrationsExecuted);migration.validate();assertEquals(0,migration.migrate().migrationsExecuted);
            try(var c=DriverManager.getConnection(upgradeUrl,adminUser,adminPassword);var s=c.createStatement()) {
                try(var r=s.executeQuery("SELECT count(*) FROM dzoka.outbox")){r.next();assertEquals(1,r.getInt(1));}
                try(var r=s.executeQuery("SELECT reservation_released,cleanup_checked_at,source_key FROM dzoka.media_assets")){assertTrue(r.next());assertTrue(r.getBoolean(1));assertNull(r.getObject(2));assertEquals(tenant+"/"+asset+"/source",r.getString(3));}
            }
        } finally {try(var c=admin();var s=c.createStatement()){s.execute("DROP DATABASE "+database);}}
    }
    @Test void databaseForeignKeysImmutabilityAndRevokedReplaysEnforceIsolation()throws Exception {
        String key=UUID.randomUUID().toString();var d=draft("Protected version",null,true,0,List.of("Original alias"));var local=places().save(who("place-manager"),A,null,d,key);
        var remote=places().save(who("remote-place-manager"),B,null,draft("Remote parent",null,true,0,List.of()),UUID.randomUUID().toString());
        try(var c=admin();var s=c.prepareStatement("UPDATE dzoka.places SET parent_id=? WHERE tenant_id=? AND id=?")){s.setObject(1,remote.id());s.setObject(2,A);s.setObject(3,local.id());assertThrows(SQLException.class,s::executeUpdate);}
        assertThrows(Failures.Unavailable.class,()->app.getBean(org.dzoka.foundation.persistence.TenantTransactions.class).execute(who("place-manager"),A,"places:manage",c->{try(var s=c.prepareStatement("UPDATE dzoka.place_revisions SET name='Overwritten' WHERE tenant_id=? AND place_id=?")){s.setObject(1,A);s.setObject(2,local.id());s.executeUpdate();}return null;}));
        try(var c=admin();var s=c.prepareStatement("UPDATE dzoka.memberships SET active=false WHERE tenant_id=? AND issuer=? AND subject='place-manager'")){s.setObject(1,A);s.setString(2,issuer);s.executeUpdate();}
        try{assertThrows(Failures.Forbidden.class,()->places().save(who("place-manager"),A,null,d,key));}
        finally{try(var c=admin();var s=c.prepareStatement("UPDATE dzoka.memberships SET active=true WHERE tenant_id=? AND issuer=? AND subject='place-manager'")){s.setObject(1,A);s.setString(2,issuer);s.executeUpdate();}}
    }
}
