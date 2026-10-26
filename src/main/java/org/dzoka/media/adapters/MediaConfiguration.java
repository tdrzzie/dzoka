package org.dzoka.media.adapters;
import org.dzoka.foundation.application.Failures;
import org.dzoka.media.ports.PrivateObjects;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

@Configuration
public class MediaConfiguration {
    @Bean public PrivateObjects privateObjects(Environment e){
        if(!e.getProperty("dzoka.media.enabled",Boolean.class,false))return new PrivateObjects(){
            public boolean enabled(){return false;}
            public void put(String k,byte[] c,String s,String t){throw unavailable();}
            public byte[] get(String k,String s,long n,int max){throw unavailable();}
            public void delete(String k){throw unavailable();}
            private Failures.Unavailable unavailable(){return new Failures.Unavailable(new IllegalStateException("Media storage disabled"));}
        };
        var objects=new S3PrivateObjects(e.getRequiredProperty("dzoka.media.endpoint"),e.getRequiredProperty("dzoka.media.region"),
                e.getRequiredProperty("dzoka.media.bucket"),e.getRequiredProperty("dzoka.media.access-key"),e.getRequiredProperty("dzoka.media.secret-key"),
                e.getProperty("dzoka.media.allow-insecure-loopback",Boolean.class,false));
        try{objects.verifyPrivateBucket();return objects;}catch(RuntimeException error){objects.close();throw error;}
    }
}
