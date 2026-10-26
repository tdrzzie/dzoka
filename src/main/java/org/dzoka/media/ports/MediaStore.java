package org.dzoka.media.ports;
import java.time.Instant;
import java.util.*;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.media.domain.MediaView;
public interface MediaStore {
    record Asset(UUID tenant,UUID id,UUID report,String state,String sourceKey,String sourceSha,long sourceBytes,
            String declaredType,String derivativeKey,String derivativeSha,long derivativeBytes,Integer width,Integer height,
            Instant expires,String rejection,UUID lease,long purgeVersion) {
        public MediaView view(){return new MediaView(id,report,state,sourceBytes,width,height,expires,rejection);}
    }
    void authorizeUpload(VerifiedIdentity actor,UUID tenant,UUID report);
    Asset prepare(VerifiedIdentity actor,UUID tenant,UUID report,String key,String sha,int size,String declaredType);
    Asset uploaded(VerifiedIdentity actor,Asset asset);
    Asset read(VerifiedIdentity actor,UUID tenant,UUID id);
    Asset retry(VerifiedIdentity actor,UUID tenant,UUID id);
    Optional<Asset> claim(VerifiedIdentity worker,UUID tenant);
    boolean complete(VerifiedIdentity worker,Asset asset,String sha,int bytes,int width,int height,String policy);
    void fail(VerifiedIdentity worker,Asset asset,String reason,boolean permanent);
    Optional<Asset> purgeCandidate(VerifiedIdentity worker,UUID tenant);
    boolean purged(VerifiedIdentity worker,Asset asset);
}
