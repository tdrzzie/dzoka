package org.dzoka;

import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.dzoka.fingerprints.domain.*;
import org.dzoka.fingerprints.ports.FingerprintJobs;
import org.dzoka.foundation.application.*;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.intake.application.IntakeService;
import org.dzoka.intake.domain.*;
import org.junit.jupiter.api.*;
import tools.jackson.databind.ObjectMapper;

abstract class AiFixture extends IntegrationFixture {
    protected final UUID primary=UUID.randomUUID(),alternate=UUID.randomUUID();protected EmbeddingSpace profile;
    @Override protected void initializeProviders() throws Exception {
        membership(A,"reporter","reports:read","reports:write");membership(A,"other","reports:read","reports:write");
        membership(A,"inference","ai:work");membership(A,"wrong-worker","ai:work");membership(B,"remote","ai:work","reports:read");
        membership(A,"purger","reports:purge");membership(A,"reviewer","reports:hold","reports:read","reports:read:any");
        membership(A,"curator","fingerprints:manage");membership(B,"curator","fingerprints:manage");
        var n=new ObjectMapper().readTree(Files.readString(Path.of("ai/model.lock.json"))).get("space");
        profile=new EmbeddingSpace(primary,n.get("provider").asString(),n.get("model").asString(),n.get("artifactRevision").asString(),n.get("artifactSha256").asString(),
            n.get("tokenizerSha256").asString(),n.get("preprocessing").asString(),n.get("dimensions").asInt(),n.get("metric").asString(),n.get("normalization").asString(),n.get("configSha256").asString(),n.get("spaceKey").asString());
        insertSpace(A,profile);
        var other=new EmbeddingSpace(alternate,profile.provider(),profile.model(),profile.artifactRevision(),profile.artifactSha256(),profile.tokenizerSha256(),profile.preprocessing(),384,"COSINE","L2","b".repeat(64),
            EmbeddingSpace.key(profile.provider(),profile.model(),profile.artifactRevision(),profile.artifactSha256(),profile.tokenizerSha256(),profile.preprocessing(),384,"COSINE","L2","b".repeat(64)));
        insertSpace(A,other);insertSpace(B,profile);
    }
    protected void insertSpace(UUID tenant,EmbeddingSpace s) throws Exception {
        try(var c=admin();var q=c.prepareStatement("INSERT INTO dzoka.embedding_spaces(tenant_id,id,provider,model,artifact_revision,artifact_sha256,tokenizer_sha256,preprocessing,dimensions,metric,normalization,config_sha256,space_key,enabled) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,true)")){
            q.setObject(1,tenant);q.setObject(2,s.id());q.setString(3,s.provider());q.setString(4,s.model());q.setString(5,s.artifactRevision());q.setString(6,s.artifactSha256());q.setString(7,s.tokenizerSha256());
            q.setString(8,s.preprocessing());q.setInt(9,s.dimensions());q.setString(10,s.metric());q.setString(11,s.normalization());q.setString(12,s.configSha256());q.setString(13,s.spaceKey());q.executeUpdate();
        }
    }
    @BeforeEach void cleanSyntheticJobs() throws Exception {
        try(var c=admin()){
            for(String table:List.of("fingerprint_perception_slots","fingerprint_rollout_perception_pins","perception_components","perception_audio","perception_jobs","perception_audio_usage",
                    "fingerprint_activations","fingerprint_active","fingerprint_snapshot_slots","fingerprint_snapshots",
                    "fingerprint_rollout_spaces","fingerprint_rollout_sources","fingerprint_rollouts","fingerprint_components","ai_jobs","ai_usage"))
                try(var s=c.prepareStatement("DELETE FROM dzoka."+table+" WHERE tenant_id=?")){s.setObject(1,A);s.executeUpdate();}
        }
    }
    protected VerifiedIdentity who(String subject){return new VerifiedIdentity(issuer,subject);}
    protected FingerprintJobs jobs(){return app.getBean(FingerprintJobs.class);}
    protected ReportReceipt report(String text){return app.getBean(IntakeService.class).submit(who("reporter"),A,null,null,
        new ReportDraft(ReportDraft.Kind.LOST,text,"en","UTC",null,null,null,null,null,null,null),"{}".getBytes(),UUID.randomUUID().toString());}
    protected void revise(UUID report,int revision,String text){app.getBean(IntakeService.class).submit(who("reporter"),A,report,revision,
        new ReportDraft(ReportDraft.Kind.LOST,text,"en","UTC",null,null,null,null,null,null,null),text.getBytes(),UUID.randomUUID().toString());}
    protected EmbeddingResult result(LeasedInput input){double[] vector=new double[384];vector[0]=1;
        return new EmbeddingResult(input.lease(),input.inputSha256(),input.space().spaceKey(),"onnxruntime-cpu","text-embedding-v1",Instant.now(),vector,5,false);}
    protected HttpResponse<String> api(String method,UUID tenant,String path,String subject,String body) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create(base+"/api/v1/tenants/"+tenant+path)).header("Authorization","Bearer "+token(subject)).header("Idempotency-Key",UUID.randomUUID().toString())
            .header("Content-Type","application/json").method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
    }
}
