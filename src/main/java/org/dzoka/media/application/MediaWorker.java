package org.dzoka.media.application;
import java.util.UUID;
import org.dzoka.foundation.application.*;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.media.ports.*;

/** Trusted core worker; untrusted image processing occurs only in the separate restricted child. */
public final class MediaWorker {
    private final MediaStore store;private final PrivateObjects objects;private final RasterDecoder decoder;
    public MediaWorker(MediaStore store,PrivateObjects objects,RasterDecoder decoder){this.store=store;this.objects=objects;this.decoder=decoder;}
    public boolean processOne(VerifiedIdentity worker,UUID tenant){
        var claimed=store.claim(worker,tenant);if(claimed.isEmpty())return false;var a=claimed.get();
        try{
            byte[] source=objects.get(a.sourceKey(),a.sourceSha(),a.sourceBytes(),5*1024*1024);
            var result=decoder.sanitize(source);
            if(!result.sourceType().equals(a.declaredType()))throw new RasterDecoder.Rejected();
            String digest=ContentDigest.sha256(result.png());
            objects.put(a.derivativeKey(),result.png(),digest,"image/png");
            store.complete(worker,a,digest,result.png().length,result.width(),result.height(),decoder.policyVersion());
        }catch(RasterDecoder.Rejected rejected){store.fail(worker,a,"INVALID_IMAGE",true);}
        catch(Failures.Unavailable unavailable){store.fail(worker,a,"PROCESSOR_UNAVAILABLE",false);}
        return true;
    }
    public boolean purgeOne(VerifiedIdentity worker,UUID tenant){
        var candidate=store.purgeCandidate(worker,tenant);if(candidate.isEmpty())return false;var a=candidate.get();
        objects.delete(a.sourceKey());objects.delete(a.derivativeKey());store.purged(worker,a);return true;
    }
}
