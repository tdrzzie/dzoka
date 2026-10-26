package org.dzoka;
import java.net.URI;
import java.util.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.http.*;
import javax.imageio.ImageIO;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.intake.application.IntakeService;
import org.dzoka.intake.domain.*;
import org.dzoka.media.application.MediaWorker;
import org.dzoka.media.adapters.DockerRasterDecoder;
import org.dzoka.media.ports.*;
import org.junit.jupiter.api.*;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

abstract class S3Fixture extends IntegrationFixture {
    protected String endpoint,bucket,access,secret;protected S3Client s3;
    @Override protected void initializeProviders() throws Exception {
        endpoint=required("DZOKA_TEST_S3_ENDPOINT");access=required("DZOKA_TEST_S3_ACCESS_KEY");secret=required("DZOKA_TEST_S3_SECRET_KEY");bucket="dzoka-"+UUID.randomUUID();
        s3=S3Client.builder().endpointOverride(URI.create(endpoint)).region(Region.US_EAST_1).forcePathStyle(true)
            .httpClientBuilder(UrlConnectionHttpClient.builder())
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(access,secret))).build();
        s3.createBucket(b->b.bucket(bucket));
        membership(A,"owner","reports:write","reports:read");membership(A,"other","reports:write","reports:read");
        membership(B,"remote","reports:write","reports:read");membership(A,"processor","media:process");
        membership(A,"purger","media:purge","reports:purge");membership(A,"reviewer","reports:read","reports:read:any","reports:hold");
    }
    @Override protected List<String> extraOptions(){return List.of("--dzoka.media.enabled=true","--dzoka.media.endpoint="+endpoint,
        "--dzoka.media.region=us-east-1","--dzoka.media.bucket="+bucket,"--dzoka.media.access-key="+access,
        "--dzoka.media.secret-key="+secret,"--dzoka.media.allow-insecure-loopback=true");}
    @AfterAll void closeS3(){if(s3!=null)s3.close();}
    @BeforeEach void clearOnlySyntheticMedia() throws Exception {
        var objects=app.getBean(PrivateObjects.class);
        try(var c=admin();var s=c.prepareStatement("SELECT source_key,derivative_key FROM dzoka.media_assets WHERE tenant_id=?")){
            s.setObject(1,A);try(var r=s.executeQuery()){while(r.next()){objects.delete(r.getString(1));objects.delete(r.getString(2));}}
        }
        try(var c=admin()){
            for(String table:List.of("outbox","media_assets","media_usage"))
                try(var s=c.prepareStatement("DELETE FROM dzoka."+table+" WHERE tenant_id=?"+(table.equals("outbox")?" AND media_id IS NOT NULL":""))){s.setObject(1,A);s.executeUpdate();}
        }
    }
    protected VerifiedIdentity who(String subject){return new VerifiedIdentity(issuer,subject);}
    protected UUID report(){return app.getBean(IntakeService.class).submit(who("owner"),A,null,null,
        new ReportDraft(ReportDraft.Kind.FOUND,"Synthetic media fixture","en","UTC",null,null,null,null,null,null,null),
        "{}".getBytes(java.nio.charset.StandardCharsets.UTF_8),UUID.randomUUID().toString()).reportId();}
    protected byte[] png() throws Exception {
        var output=new ByteArrayOutputStream();var image=new BufferedImage(8,7,BufferedImage.TYPE_INT_RGB);
        image.setRGB(2,2,0x12ab34);ImageIO.write(image,"png",output);return output.toByteArray();
    }
    protected HttpResponse<byte[]> upload(UUID report,byte[] bytes,String type,String key) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(base+"/api/v1/tenants/"+A+"/reports/"+report+"/media"))
            .timeout(java.time.Duration.ofSeconds(30)).header("Authorization","Bearer "+token("owner"))
            .header("Content-Type",type).header("Idempotency-Key",key).POST(HttpRequest.BodyPublishers.ofByteArray(bytes)).build(),HttpResponse.BodyHandlers.ofByteArray());
    }
    protected HttpResponse<byte[]> media(UUID tenant,UUID id,String subject,String suffix) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(base+"/api/v1/tenants/"+tenant+"/media/"+id+suffix))
            .header("Authorization","Bearer "+token(subject)).GET().build(),HttpResponse.BodyHandlers.ofByteArray());
    }
    protected MediaWorker worker(){return new MediaWorker(app.getBean(MediaStore.class),app.getBean(PrivateObjects.class),new DockerRasterDecoder());}
}
