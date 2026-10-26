package org.dzoka;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.*;
import org.dzoka.foundation.application.*;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.intake.application.IntakeService;
import org.dzoka.intake.domain.*;
import org.dzoka.media.adapters.JdbcMediaStore;
import org.dzoka.media.ports.MediaStore;
import org.junit.jupiter.api.*;

class MediaStoreIT extends IntegrationFixture {
    @BeforeEach void clearOnlyThisSyntheticTenantMedia() throws Exception {
        try(var c=admin()){
            for(String table:List.of("outbox","media_assets","media_usage"))
                try(var s=c.prepareStatement("DELETE FROM dzoka."+table+" WHERE tenant_id=?"+(table.equals("outbox")?" AND media_id IS NOT NULL":""))){s.setObject(1,A);s.executeUpdate();}
        }
    }
    @Override protected void initializeProviders() throws Exception {
        membership(A,"owner","reports:write","reports:read");membership(A,"other","reports:write","reports:read");
        membership(A,"processor","media:process");membership(A,"purger","media:purge","reports:purge");
        membership(A,"reviewer","reports:hold","reports:read","reports:read:any");
    }
    private VerifiedIdentity who(String sub){return new VerifiedIdentity(issuer,sub);}
    private UUID report(){return app.getBean(IntakeService.class).submit(who("owner"),A,null,null,
        new ReportDraft(ReportDraft.Kind.FOUND,"Synthetic", "en","UTC",null,null,null,null,null,null,null),
        "{}".getBytes(java.nio.charset.StandardCharsets.UTF_8),UUID.randomUUID().toString()).reportId();}
    private MediaStore.Asset prepare(UUID report){return app.getBean(MediaStore.class).prepare(who("owner"),A,report,UUID.randomUUID().toString(),"a".repeat(64),12,"image/png");}
    @Test void uploadIntentIsIdempotentScopedAndQuotaBound() throws Exception {
        var store=app.getBean(MediaStore.class);UUID report=report();String key=UUID.randomUUID().toString();
        var first=store.prepare(who("owner"),A,report,key,"a".repeat(64),12,"image/png");
        assertEquals(first.id(),store.prepare(who("owner"),A,report,key,"a".repeat(64),12,"image/png").id());
        assertThrows(Failures.Conflict.class,()->store.prepare(who("owner"),A,report,key,"b".repeat(64),12,"image/png"));
        assertThrows(Failures.NotFound.class,()->store.read(who("other"),A,first.id()));
        try(var c=admin();var s=c.prepareStatement("UPDATE dzoka.tenants SET media_byte_limit=(SELECT reserved_bytes FROM dzoka.media_usage WHERE tenant_id=?) WHERE id=?")){
            s.setObject(1,A);s.setObject(2,A);s.executeUpdate();
        }
        try{assertThrows(Failures.Conflict.class,()->prepare(report));}
        finally{try(var c=admin();var s=c.prepareStatement("UPDATE dzoka.tenants SET media_byte_limit=524288000 WHERE id=?")){s.setObject(1,A);s.executeUpdate();}}
    }
    @Test void transientOutagePreservesEvidenceAndRequiresExplicitRetry() throws Exception {
        var store=app.getBean(MediaStore.class);var a=prepare(report());store.uploaded(who("owner"),a);
        for(int i=0;i<3;i++){var claim=store.claim(who("processor"),A).orElseThrow();store.fail(who("processor"),claim,"PROCESSOR_UNAVAILABLE",false);}
        assertEquals("FAILED",store.read(who("owner"),A,a.id()).state());assertTrue(store.purgeCandidate(who("purger"),A).isEmpty());
        assertEquals("QUARANTINED",store.retry(who("owner"),A,a.id()).state());
    }
    @Test void staleLeaseCannotActivateAndParentDeletionRevokesLateResult() throws Exception {
        var store=app.getBean(MediaStore.class);UUID report=report();var original=prepare(report);store.uploaded(who("owner"),original);
        var first=store.claim(who("processor"),A).orElseThrow();
        try(var c=admin();var s=c.prepareStatement("UPDATE dzoka.media_assets SET lease_until=now()-interval '1 second' WHERE tenant_id=? AND id=?")){
            s.setObject(1,A);s.setObject(2,first.id());s.executeUpdate();
        }
        assertFalse(store.complete(who("processor"),first,"b".repeat(64),30,2,2,"test-policy"));
        var next=store.claim(who("processor"),A).orElseThrow();assertNotEquals(first.lease(),next.lease());
        app.getBean(IntakeService.class).delete(who("owner"),A,report);
        assertFalse(store.complete(who("processor"),next,"b".repeat(64),30,2,2,"test-policy"));
        assertThrows(Failures.NotFound.class,()->store.read(who("owner"),A,original.id()));
    }
    @Test void rejectedAssetCleanupDoesNotDeleteValidNeighborAndHoldBlocksCleanup() throws Exception {
        var store=app.getBean(MediaStore.class);UUID report=report();var rejected=prepare(report);var neighbor=prepare(report);
        store.uploaded(who("owner"),rejected);var claim=store.claim(who("processor"),A).orElseThrow();
        store.fail(who("processor"),claim,"INVALID_IMAGE",true);
        app.getBean(IntakeService.class).hold(who("reviewer"),A,report,true);
        assertTrue(store.purgeCandidate(who("purger"),A).isEmpty());
        app.getBean(IntakeService.class).hold(who("reviewer"),A,report,false);
        var purge=store.purgeCandidate(who("purger"),A).orElseThrow();assertEquals(rejected.id(),purge.id());
        assertThrows(Failures.Conflict.class,()->app.getBean(IntakeService.class).hold(who("reviewer"),A,report,true));
        assertTrue(store.purged(who("purger"),purge));assertTrue(store.purged(who("purger"),purge));
        assertEquals("UPLOADING",store.read(who("owner"),A,neighbor.id()).state());
    }
}
