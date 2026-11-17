package org.dzoka;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.dzoka.fingerprints.ports.PerceptionJobs;
import org.dzoka.foundation.application.Failures;
import org.dzoka.intake.application.IntakeService;
import org.dzoka.intake.domain.ReportDraft;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class CurrentSetupWorkerIT extends AiFixture {
    static final String PROFILE="ISO_SHONA_CPU_CPU_SN_EN";
    @Override protected void initializeProviders() throws Exception {
        super.initializeProviders();
        var value=new ObjectMapper().readTree(Files.readString(Path.of("ai/broker-profiles.json"))).get(PROFILE);
        try(var c=admin();var s=c.prepareStatement("INSERT INTO dzoka.perception_profiles(tenant_id,id,task,profile_key,manifest,enabled) VALUES(?,?,?,?,CAST(? AS jsonb),true)")) {
            s.setObject(1,A);s.setObject(2,PerceptionFixtures.id(PROFILE));s.setString(3,value.get("manifest").get("task").asString());s.setString(4,value.get("key").asString());s.setString(5,value.get("manifest").toString());s.executeUpdate();
        }
    }
    @Test void installedCurrentAccountWorkerCompletesPrivateShonaJob() throws Exception {
        String text="Ndasiya bhegi rangu.";
        UUID report=app.getBean(IntakeService.class).submit(who("reporter"),A,null,null,new ReportDraft(ReportDraft.Kind.LOST,text,"sn","UTC",null,null,null,null,null,null,null),text.getBytes(StandardCharsets.UTF_8),UUID.randomUUID().toString()).reportId();
        var jobs=app.getBean(PerceptionJobs.class);var job=jobs.enqueue(who("reporter"),A,report,PerceptionFixtures.id(PROFILE),null,null);
        String script="""
import json,os,sys
from pathlib import Path
release=Path(os.environ['DZOKA_TEST_MANAGED_RELEASE']);sys.path[:0]=[str(release/'tools'),str(release/'ai')]
from verify_current_setup import installed_worker,wait
with installed_worker(os.environ['DZOKA_CURRENT_TEST_CORE'],os.environ['DZOKA_CURRENT_TEST_TENANT'],os.environ['DZOKA_CURRENT_TEST_PROFILE'],'ISO_SHONA_CPU_CPU_SN_EN',os.environ['DZOKA_CURRENT_TEST_TOKEN']) as (config,path,digest,root):
 def complete():
  state=root/'service/managed-status.json'
  return state.exists() and json.loads(state.read_bytes()).get('completedAcknowledgements',0)>=1
 wait(complete,600)
print('Installed current-account Shona job acknowledged and owned task cleaned up')
""";
        var command=new ProcessBuilder("python","-I","-B","-c",script);var env=command.environment();
        String credential=token("inference",issuer,"dzoka-core",Instant.now().plusSeconds(1200),key);
        env.put("DZOKA_CURRENT_TEST_CORE",base);env.put("DZOKA_CURRENT_TEST_TENANT",A.toString());env.put("DZOKA_CURRENT_TEST_PROFILE",PerceptionFixtures.id(PROFILE).toString());env.put("DZOKA_CURRENT_TEST_TOKEN",credential);
        Path log=Path.of(".test-tmp","current-setup-http-"+UUID.randomUUID()+".log");command.redirectErrorStream(true).redirectOutput(log.toFile());var child=command.start();
        try {
            assertTrue(child.waitFor(1080,TimeUnit.SECONDS),"Installed worker completion deadline");assertEquals(0,child.exitValue(),Files.readString(log));assertFalse(Files.readString(log).contains(credential));
        } finally {if(child.isAlive()){child.destroyForcibly();child.waitFor(10,TimeUnit.SECONDS);}}
        assertEquals("SUCCEEDED",jobs.read(who("reporter"),A,job.id()).state());var result=jobs.result(who("reporter"),A,job.id()).orElseThrow();
        assertEquals("UNVERIFIED_TRANSLATION",result.evidence().get("trust"));assertEquals("SHADOW",result.selection());
        assertEquals(HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))),result.inputSha256());
        assertThrows(Failures.NotFound.class,()->jobs.result(who("other"),A,job.id()));assertEquals(job.id(),jobs.enqueue(who("reporter"),A,report,PerceptionFixtures.id(PROFILE),null,null).id());
    }
}
