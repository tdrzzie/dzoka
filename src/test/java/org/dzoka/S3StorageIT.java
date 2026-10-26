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
