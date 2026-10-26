package org.dzoka.media.adapters;
import java.net.URI;
import java.time.Duration;
import java.util.*;
import org.dzoka.foundation.application.*;
import org.dzoka.media.ports.PrivateObjects;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

public final class S3PrivateObjects implements PrivateObjects {
    private final S3Client client;private final String bucket;
    public S3PrivateObjects(String endpoint,String region,String bucket,String access,String secret,boolean local){
        URI uri=URI.create(endpoint);
        if(uri.getUserInfo()!=null || uri.getQuery()!=null || uri.getFragment()!=null || uri.getHost()==null
                || !("https".equals(uri.getScheme()) || (local && "http".equals(uri.getScheme())
                && Set.of("localhost","127.0.0.1").contains(uri.getHost())))
                || bucket==null || !bucket.matches("[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]")
                || access==null || access.isBlank() || secret==null || secret.isBlank())throw new IllegalArgumentException("Invalid private storage configuration");
        this.bucket=bucket;
        client=S3Client.builder().endpointOverride(uri).region(Region.of(region)).forcePathStyle(true)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(access,secret)))
                .httpClientBuilder(UrlConnectionHttpClient.builder().connectionTimeout(Duration.ofSeconds(3)).socketTimeout(Duration.ofSeconds(10)))
                .overrideConfiguration(c->c.apiCallTimeout(Duration.ofSeconds(20)).apiCallAttemptTimeout(Duration.ofSeconds(12))).build();
    }
    @Override public boolean enabled(){return true;}
    public void verifyPrivateBucket(){
        try{
            var acl=client.getBucketAcl(b->b.bucket(bucket));
            if(acl.grants().stream().anyMatch(g->g.grantee()==null || g.grantee().type()!=Type.CANONICAL_USER))
                throw new IllegalStateException("Bucket ACL is not private");
            String versioning=client.getBucketVersioning(b->b.bucket(bucket)).statusAsString();
            if(versioning!=null && !versioning.isBlank())throw new IllegalStateException("Media bucket must never have versioning enabled");
            try{var policy=client.getBucketPolicy(b->b.bucket(bucket));
                if(policy.policy()!=null && !policy.policy().isBlank())throw new IllegalStateException("Use identity grants rather than bucket policies for this media profile");
            }catch(S3Exception absent){if(absent.statusCode()!=404)throw absent;}
        }catch(RuntimeException e){throw new Failures.Unavailable(e);}
    }
    private static void key(String key){if(key==null || !key.matches("[a-f0-9-]{36}/[a-f0-9-]{36}/(source|[a-f0-9-]{36})"))throw new IllegalArgumentException("Invalid generated key");}
    @Override public void put(String key,byte[] content,String digest,String type){
        key(key);
        try{client.putObject(PutObjectRequest.builder().bucket(bucket).key(key).contentType(type)
                    .metadata(Map.of("sha256",digest)).checksumSHA256(Base64.getEncoder().encodeToString(HexFormat.of().parseHex(digest)))
                    .ifNoneMatch("*").build(),RequestBody.fromBytes(content));}
        catch(S3Exception e){
            if(e.statusCode()==412){get(key,digest,content.length,content.length);return;}
            throw new Failures.Unavailable(e);
        }catch(RuntimeException e){throw new Failures.Unavailable(e);}
    }
    @Override public byte[] get(String key,String digest,long expected,int maximum){
        key(key);
        if(expected<1 || expected>maximum)throw new Failures.Unavailable(new IllegalStateException("Invalid object manifest"));
        try(var stream=client.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build())){
            if(stream.response().contentLength()!=expected)throw new IllegalStateException("Object length mismatch");
            byte[] bytes=stream.readNBytes(maximum+1);
            if(bytes.length!=expected || !ContentDigest.sha256(bytes).equals(digest))throw new IllegalStateException("Object integrity mismatch");
            return bytes;
        }catch(Exception error){throw new Failures.Unavailable(error);}
    }
    @Override public void delete(String key){
        if(key==null)return;key(key);
        try{client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());}
        catch(RuntimeException error){throw new Failures.Unavailable(error);}
    }
    @Override public void close(){client.close();}
}
