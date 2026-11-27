package org.dzoka;
import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import org.dzoka.foundation.application.*;
import org.dzoka.intake.application.IntakeService;
import org.dzoka.media.application.MediaService;
import org.dzoka.media.adapters.S3PrivateObjects;
import org.dzoka.media.domain.MediaView;
import org.dzoka.media.ports.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class S3StorageIT extends S3Fixture {
    private void maintenanceCycle()throws Exception {
        var javaExe=java.nio.file.Path.of(System.getProperty("java.home"),"bin",System.getProperty("os.name").startsWith("Windows")?"java.exe":"java").toString();
        var builder=new ProcessBuilder(javaExe,"-cp",System.getProperty("java.class.path"),"org.dzoka.bootstrap.MediaWorkerMain","maintain");
        var env=builder.environment();env.keySet().removeIf(k->k.startsWith("DZOKA_")||k.startsWith("SPRING_"));
        env.put("DZOKA_DB_URL",url);env.put("DZOKA_DB_USER","dzoka_app");env.put("DZOKA_DB_PASSWORD",required("DZOKA_TEST_APP_PASSWORD"));
        env.put("DZOKA_WORKER_ISSUER",issuer);env.put("DZOKA_WORKER_SUBJECT","maintainer");env.put("DZOKA_WORKER_TENANT",A.toString());env.put("DZOKA_WORKER_LIMIT","2");env.put("DZOKA_WORKER_CYCLES","1");
        env.put("DZOKA_MEDIA_ENDPOINT",endpoint);env.put("DZOKA_MEDIA_REGION","us-east-1");env.put("DZOKA_MEDIA_BUCKET",bucket);env.put("DZOKA_MEDIA_ACCESS_KEY",access);env.put("DZOKA_MEDIA_SECRET_KEY",secret);env.put("DZOKA_MEDIA_ALLOW_INSECURE_LOOPBACK","true");
        var log=java.nio.file.Files.createTempFile(java.nio.file.Path.of(".test-tmp"),"media-maintenance-",".log");builder.redirectErrorStream(true).redirectOutput(log.toFile());
        Process child=builder.start();try{assertTrue(child.waitFor(60,java.util.concurrent.TimeUnit.SECONDS));assertEquals(0,child.exitValue());}finally{if(child.isAlive())child.destroyForcibly();}
    }
    @Test void actualBoundedMaintenanceProcessSanitizesAndReconcilesPrivateMedia()throws Exception {
        membership(A,"maintainer","reports:purge","media:purge","media:process");UUID report=report();var response=upload(report,png(),"image/png",UUID.randomUUID().toString());assertEquals(200,response.statusCode());
        UUID id=app.getBean(ObjectMapper.class).readValue(response.body(),MediaView.class).id();maintenanceCycle();assertEquals("SANITIZED",app.getBean(MediaStore.class).read(who("owner"),A,id).state());
        app.getBean(IntakeService.class).delete(who("owner"),A,report);maintenanceCycle();
        assertEquals(404,media(A,id,"owner","/preview").statusCode());assertFalse(worker().purgeOne(who("purger"),A));
    }
    private void makeReconciliationDue(UUID id)throws Exception {
        try(var c=admin();var s=c.prepareStatement("UPDATE dzoka.media_assets SET cleanup_checked_at=clock_timestamp()-interval '16 minutes' WHERE tenant_id=? AND id=?")){s.setObject(1,A);s.setObject(2,id);s.executeUpdate();}
    }
    @Test void unacknowledgedLateSourceAndDerivativeWritesAreIndependentlyErased()throws Exception {
        UUID report=report();byte[] bytes=png();var response=upload(report,bytes,"image/png",UUID.randomUUID().toString());assertEquals(200,response.statusCode());
        UUID id=app.getBean(ObjectMapper.class).readValue(response.body(),MediaView.class).id();var store=app.getBean(MediaStore.class);var objects=app.getBean(PrivateObjects.class);var asset=store.claim(who("processor"),A).orElseThrow();
        app.getBean(IntakeService.class).delete(who("owner"),A,report);
        var first=store.purgeCandidate(who("purger"),A).orElseThrow();objects.delete(first.sourceKey());objects.delete(first.derivativeKey());assertTrue(store.purged(who("purger"),first));
        assertFalse(worker().purgeOne(who("purger"),A));
        // Both PUTs finish after acknowledged erasure, and neither completion callback runs.
        objects.put(asset.sourceKey(),bytes,asset.sourceSha(),"application/octet-stream");
        String sha=ContentDigest.sha256(bytes);objects.put(asset.derivativeKey(),bytes,sha,"image/png");
        assertEquals(404,media(A,id,"owner","/preview").statusCode());makeReconciliationDue(id);
        assertTrue(worker().purgeOne(who("purger"),A));
        assertThrows(Failures.Unavailable.class,()->objects.get(asset.sourceKey(),asset.sourceSha(),bytes.length,5*1024*1024));
        assertThrows(Failures.Unavailable.class,()->objects.get(asset.derivativeKey(),sha,bytes.length,20*1024*1024));
        assertFalse(store.purged(who("purger"),first));assertFalse(worker().purgeOne(who("purger"),A));
        try(var c=admin();var s=c.prepareStatement("SELECT reserved_bytes FROM dzoka.media_usage WHERE tenant_id=?")){s.setObject(1,A);try(var r=s.executeQuery()){r.next();assertEquals(0,r.getLong(1));}}
    }
    @Test void revokedWriterCannotPreventIndependentLateWriteReconciliation()throws Exception {
        UUID report=report();byte[] bytes=png();assertEquals(200,upload(report,bytes,"image/png",UUID.randomUUID().toString()).statusCode());
        var store=app.getBean(MediaStore.class);var asset=store.claim(who("processor"),A).orElseThrow();var objects=app.getBean(PrivateObjects.class);
        app.getBean(IntakeService.class).delete(who("owner"),A,report);assertTrue(worker().purgeOne(who("purger"),A));
        objects.put(asset.derivativeKey(),bytes,ContentDigest.sha256(bytes),"image/png");
        try(var c=admin();var s=c.prepareStatement("UPDATE dzoka.memberships SET active=false WHERE tenant_id=? AND issuer=? AND subject='processor'")){s.setObject(1,A);s.setString(2,issuer);s.executeUpdate();}
        try {
            assertThrows(Failures.Forbidden.class,()->store.complete(who("processor"),asset,ContentDigest.sha256(bytes),bytes.length,8,7,"diagnostic"));
            makeReconciliationDue(asset.id());assertTrue(worker().purgeOne(who("purger"),A));
            assertThrows(Failures.Unavailable.class,()->objects.get(asset.derivativeKey(),ContentDigest.sha256(bytes),bytes.length,20*1024*1024));
        } finally {try(var c=admin();var s=c.prepareStatement("UPDATE dzoka.memberships SET active=true WHERE tenant_id=? AND issuer=? AND subject='processor'")){s.setObject(1,A);s.setString(2,issuer);s.executeUpdate();}}
    }
    @Test void crashedCleanupLeaseCanBeReclaimedWithoutDoubleQuotaRelease()throws Exception {
        UUID report=report();assertEquals(200,upload(report,png(),"image/png",UUID.randomUUID().toString()).statusCode());app.getBean(IntakeService.class).delete(who("owner"),A,report);
        var store=app.getBean(MediaStore.class);var first=store.purgeCandidate(who("purger"),A).orElseThrow();assertTrue(store.purgeCandidate(who("purger"),A).isEmpty());
        app.getBean(PrivateObjects.class).delete(first.sourceKey()); // process dies before acknowledgment
        try(var c=admin();var s=c.prepareStatement("UPDATE dzoka.media_assets SET cleanup_until=clock_timestamp()-interval '1 second' WHERE tenant_id=? AND id=?")){s.setObject(1,A);s.setObject(2,first.id());s.executeUpdate();}
        var next=store.purgeCandidate(who("purger"),A).orElseThrow();assertNotEquals(first.cleanupToken(),next.cleanupToken());assertFalse(store.purged(who("purger"),first));
        var objects=app.getBean(PrivateObjects.class);objects.delete(next.sourceKey());objects.delete(next.derivativeKey());assertTrue(store.purged(who("purger"),next));assertTrue(store.purged(who("purger"),next));
        assertFalse(worker().purgeOne(who("purger"),A));
    }
    @Test void restoredPrivateObjectCannotReopenBeforePostBackupErasureIsApplied() throws Exception {
        membership(A,"restore-worker","retention:recover","operations:read","reports:purge","media:purge");
        UUID report=report();byte[] original=png();var response=upload(report,original,"image/png",UUID.randomUUID().toString());assertEquals(200,response.statusCode());
        UUID id=app.getBean(ObjectMapper.class).readValue(response.body(),MediaView.class).id();var asset=app.getBean(MediaStore.class).read(who("owner"),A,id);var objects=app.getBean(PrivateObjects.class);
        byte[] backup=objects.get(asset.sourceKey(),asset.sourceSha(),asset.sourceBytes(),5*1024*1024);assertArrayEquals(original,backup);
        var operations=app.getBean(org.dzoka.operations.Operations.class);String evidence="e".repeat(64);operations.close(who("restore-worker"),A,evidence);
        String manifest=operations.replay(who("restore-worker"),A,new org.dzoka.operations.Operations.Replay(evidence,List.of(new org.dzoka.operations.Operations.Erasure(report,10))));
        // Restore an older real S3 object snapshot while tenant serving remains closed.
        objects.delete(asset.sourceKey());objects.put(asset.sourceKey(),backup,asset.sourceSha(),"image/png");
        var reviews=new org.dzoka.operations.Operations.Open(manifest,evidence,evidence);
        assertThrows(Failures.Conflict.class,()->operations.open(who("restore-worker"),A,reviews));
        app.getBean(org.dzoka.intake.ports.Reports.class).expire(who("restore-worker"),A);
        assertThrows(Failures.Conflict.class,()->operations.open(who("restore-worker"),A,reviews));
        assertTrue(worker().purgeOne(who("restore-worker"),A));
        assertThrows(Failures.Unavailable.class,()->objects.get(asset.sourceKey(),asset.sourceSha(),asset.sourceBytes(),5*1024*1024));
        operations.open(who("restore-worker"),A,reviews);assertEquals(404,media(A,id,"owner","/preview").statusCode());
        assertTrue(operations.erasures(who("restore-worker"),A,null).stream().anyMatch(e->e.report().equals(report)));
    }
    @Test void realPrivateUploadIsIdempotentAndQuarantineCannotBePreviewed() throws Exception {
        UUID report=report();byte[] png=png();String key=UUID.randomUUID().toString();
        var first=upload(report,png,"image/png",key);assertEquals(200,first.statusCode(),new String(first.body()));
        var view=app.getBean(ObjectMapper.class).readValue(first.body(),MediaView.class);assertEquals("QUARANTINED",view.state());
        assertArrayEquals(first.body(),upload(report,png,"image/png",key).body());
        assertEquals(409,upload(report,new byte[]{1,2,3},"image/png",key).statusCode());
        assertEquals(409,media(A,view.id(),"owner","/preview").statusCode());
        assertEquals(404,media(A,view.id(),"other","").statusCode());assertEquals(404,media(B,view.id(),"remote","").statusCode());
        var a=app.getBean(MediaStore.class).read(who("owner"),A,view.id());
        assertArrayEquals(png,app.getBean(PrivateObjects.class).get(a.sourceKey(),a.sourceSha(),a.sourceBytes(),5*1024*1024));
        assertEquals(403,http.send(HttpRequest.newBuilder(URI.create(endpoint+"/"+bucket+"/"+a.sourceKey())).GET().build(),HttpResponse.BodyHandlers.discarding()).statusCode());
        assertFalse(new String(first.body()).contains(a.sourceKey()));
    }
    @Test void objectManifestAndConditionalWritesDetectTampering() throws Exception {
        UUID report=report();var response=upload(report,png(),"image/png",UUID.randomUUID().toString());
        var id=app.getBean(ObjectMapper.class).readValue(response.body(),MediaView.class).id();var a=app.getBean(MediaStore.class).read(who("owner"),A,id);
        var objects=app.getBean(PrivateObjects.class);
        assertThrows(Failures.Unavailable.class,()->objects.get(a.sourceKey(),"0".repeat(64),a.sourceBytes(),5*1024*1024));
        assertThrows(Failures.Unavailable.class,()->objects.put(a.sourceKey(),new byte[]{1,2},ContentDigest.sha256(new byte[]{1,2}),"application/octet-stream"));
        assertArrayEquals(png(),objects.get(a.sourceKey(),a.sourceSha(),a.sourceBytes(),5*1024*1024));
    }
    @Test void lateCompletionCannotPublishAndActualObjectsArePurgedAfterDeletion() throws Exception {
        UUID report=report();var response=upload(report,png(),"image/png",UUID.randomUUID().toString());
        var id=app.getBean(ObjectMapper.class).readValue(response.body(),MediaView.class).id();var store=app.getBean(MediaStore.class);var a=store.claim(who("processor"),A).orElseThrow();
        byte[] derivative=png();String sha=ContentDigest.sha256(derivative);app.getBean(PrivateObjects.class).put(a.derivativeKey(),derivative,sha,"image/png");
        app.getBean(IntakeService.class).delete(who("owner"),A,report);
        assertFalse(store.complete(who("processor"),a,sha,derivative.length,8,7,"test-negative-completion"));
        assertEquals(404,media(A,id,"owner","/preview").statusCode());
        var firstCleanup=store.purgeCandidate(who("purger"),A).orElseThrow();var objects=app.getBean(PrivateObjects.class);
        objects.delete(a.sourceKey());objects.delete(a.derivativeKey());
        // A PUT that was already in flight can finish after cleanup; its completion must requeue erasure.
        objects.put(a.sourceKey(),png(),a.sourceSha(),"application/octet-stream");store.uploaded(who("owner"),a);
        assertFalse(store.purged(who("purger"),firstCleanup));assertTrue(worker().purgeOne(who("purger"),A));
        assertThrows(Failures.Unavailable.class,()->app.getBean(PrivateObjects.class).get(a.sourceKey(),a.sourceSha(),a.sourceBytes(),5*1024*1024));
        assertThrows(Failures.Unavailable.class,()->objects.get(a.derivativeKey(),sha,derivative.length,20*1024*1024));
        assertFalse(worker().purgeOne(who("purger"),A));
    }
    @Test void realConnectionFailurePreservesIntentAndExactRetryRecovers() throws Exception {
        UUID report=report();String key=UUID.randomUUID().toString();byte[] bytes=png();
        try(var unavailable=new S3PrivateObjects("http://127.0.0.1:1","us-east-1",bucket,access,secret,true)){
            var service=new MediaService(app.getBean(MediaStore.class),unavailable);
            assertThrows(Failures.Unavailable.class,()->service.upload(who("owner"),A,report,key,bytes,"image/png"));
        }
        assertEquals(200,upload(report,bytes,"image/png",key).statusCode());
    }
    @Test void uploadSizeAndUnsupportedFormatsFailBeforePublishing() throws Exception {
        UUID report=report();assertEquals(413,upload(report,new byte[5*1024*1024+1],"image/png",UUID.randomUUID().toString()).statusCode());
        assertEquals(415,upload(report,"<svg/>".getBytes(),"image/svg+xml",UUID.randomUUID().toString()).statusCode());
    }
}
