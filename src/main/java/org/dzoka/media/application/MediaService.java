package org.dzoka.media.application;
import java.util.UUID;
import org.dzoka.foundation.application.*;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.media.domain.MediaView;
import org.dzoka.media.ports.*;
import org.springframework.stereotype.Service;

@Service
public final class MediaService {
    private final MediaStore store;private final PrivateObjects objects;
    public MediaService(MediaStore store,PrivateObjects objects){this.store=store;this.objects=objects;}
    public void authorizeUpload(VerifiedIdentity actor,UUID tenant,UUID report,String key){
        if(key==null || !key.matches("[A-Za-z0-9._:-]{8,128}"))throw new IllegalArgumentException("Invalid upload key");
        store.authorizeUpload(actor,tenant,report);requireStorage();
    }
    private void requireStorage(){if(!objects.enabled())throw new Failures.Unavailable(new IllegalStateException("Media storage disabled"));}
    public MediaView upload(VerifiedIdentity actor,UUID tenant,UUID report,String key,byte[] bytes,String type){
        authorizeUpload(actor,tenant,report,key);
        if(bytes.length==0 || bytes.length>5*1024*1024)throw new IllegalArgumentException("Invalid upload size");
        var a=store.prepare(actor,tenant,report,key,ContentDigest.sha256(bytes),bytes.length,type);
        if(a.state().equals("UPLOADING") || a.state().equals("FAILED")){
            objects.put(a.sourceKey(),bytes,a.sourceSha(),"application/octet-stream");a=store.uploaded(actor,a);
            if(a==null)throw new Failures.NotFound();
        }
        return a.view();
    }
    public MediaView read(VerifiedIdentity actor,UUID tenant,UUID media){return store.read(actor,tenant,media).view();}
    public MediaView retry(VerifiedIdentity actor,UUID tenant,UUID media){requireStorage();return store.retry(actor,tenant,media).view();}
    public byte[] preview(VerifiedIdentity actor,UUID tenant,UUID media){
        var a=store.read(actor,tenant,media);
        if(!a.state().equals("SANITIZED"))throw new Failures.Conflict();requireStorage();
        byte[] result=objects.get(a.derivativeKey(),a.derivativeSha(),a.derivativeBytes(),20*1024*1024);
        var current=store.read(actor,tenant,media);
        if(!current.state().equals("SANITIZED") || !a.derivativeSha().equals(current.derivativeSha()))throw new Failures.NotFound();
        return result;
    }
}
