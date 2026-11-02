package org.dzoka;

import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.dzoka.fingerprints.domain.EmbeddingSpace;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class ManagedBrokerIT extends AiFixture {
    EmbeddingSpace isolated;
    @Override protected void initializeProviders()throws Exception {
        super.initializeProviders();var n=new ObjectMapper().readTree(Files.readString(Path.of("ai/broker-profiles.json"))).get("ISO_TEXT").get("space");
        isolated=new EmbeddingSpace(PerceptionFixtures.id("MANAGED_ISO_TEXT"),n.get("provider").asString(),n.get("model").asString(),n.get("artifactRevision").asString(),n.get("artifactSha256").asString(),n.get("tokenizerSha256").asString(),n.get("preprocessing").asString(),n.get("dimensions").asInt(),n.get("metric").asString(),n.get("normalization").asString(),n.get("configSha256").asString(),n.get("spaceKey").asString());insertSpace(A,isolated);
    }
    String line(Process child,BufferedReader reader)throws Exception {
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(450);
        while(!reader.ready()&&child.isAlive()&&System.nanoTime()<end)Thread.sleep(20);
        assertTrue(reader.ready(),"Managed helper did not report bounded progress");return reader.readLine();
    }
    @Test void realTokenReplacementPausesInvalidCredentialsAndArchivesModelHistory()throws Exception {
        var first=jobs().enqueue(who("reporter"),A,report("Synthetic managed blue bag").reportId(),isolated.id());
        String script="""
import os,sys,json
from pathlib import Path
release=Path(os.environ['DZOKA_TEST_MANAGED_RELEASE']);sys.path.insert(0,str(release/'tools'));sys.path.insert(0,str(release/'ai'))
from verify_worker_boundary import private_fixture
from broker_bundle import protect
from service_account import identity
from service_host import HostLimits
from managed_broker import ManagedService
from profile_archive import verify
def emit(value):print(json.dumps(value),flush=True)
with private_fixture() as root:
 protect(root)
 for name in ('broker','broker/profiles','broker/docker','service'):
  (root/name).mkdir();protect(root/name)
 credential=root/'credentials.json'
 def replace(value):
  temporary=root/'rotation.tmp';temporary.write_bytes(json.dumps(value).encode());protect(temporary);os.replace(temporary,credential)
 replace({'DZOKA_WORKER_TOKEN_ONE':os.environ['DZOKA_TEST_BAD_TOKEN']})
 entry={'tenant':os.environ['DZOKA_TEST_TENANT'],'profile':os.environ['DZOKA_TEST_PROFILE'],'profileName':'ISO_TEXT','bundle':os.environ['DZOKA_TEST_BROKER_BUNDLE'],'bundleSha256':os.environ['DZOKA_TEST_BROKER_SHA256'],'stateDirectory':str(root/'broker'),'tokenEnvironment':'DZOKA_WORKER_TOKEN_ONE'}
 who=identity();values={'plan.json':{'schemaVersion':1,'coreUrl':os.environ['DZOKA_TEST_CORE'],'allowInsecureLoopback':True,'pollSeconds':1,'backoffMaxSeconds':4,'entries':[entry]},'service.json':{'schemaVersion':3,'accountSid':who['sid'],'integrityRid':who['integrity'],'plan':str(root/'plan.json'),'credentials':str(credential),'stateDirectory':str(root/'service')},'policy.json':{'schemaVersion':1,'serviceConfig':str(root/'service.json'),'archiveAt':2,'keepTerminal':0}}
 for name,value in values.items():
  path=root/name;path.write_bytes(json.dumps(value).encode());protect(path)
 with ManagedService(root/'policy.json',os.environ['DZOKA_TEST_MANAGED_SHA256']) as service:
  limits=HostLimits();emit(service.step())
  assert sys.stdin.readline().strip()=='rotate'
  replace({'DZOKA_WORKER_TOKEN_ONE':os.environ['DZOKA_TEST_GOOD_TOKEN']});emit(service.step())
  assert sys.stdin.readline().strip()=='invalidate'
  replace({});emit(service.step())
  assert sys.stdin.readline().strip()=='restore'
  replace({'DZOKA_WORKER_TOKEN_ONE':os.environ['DZOKA_TEST_GOOD_TOKEN']});emit(service.step())
  emit(service.step());assert verify(root/'broker/profiles/archive',who['sid'])==2
""";
        String good=token("inference",issuer,"dzoka-core",Instant.now().plusSeconds(1200),key),bad=token("remote",issuer,"dzoka-core",Instant.now().plusSeconds(1200),key);
        var command=new ProcessBuilder("python","-I","-B","-c",script);var env=command.environment();env.put("DZOKA_TEST_GOOD_TOKEN",good);env.put("DZOKA_TEST_BAD_TOKEN",bad);env.put("DZOKA_TEST_TENANT",A.toString());env.put("DZOKA_TEST_PROFILE",isolated.id().toString());env.put("DZOKA_TEST_CORE",base);
        Path log=Path.of(".test-tmp","managed-core-"+UUID.randomUUID()+".log");command.redirectError(log.toFile());var child=command.start();var codec=new ObjectMapper();
        try(var reader=child.inputReader();var writer=child.outputWriter()) {
            var denied=codec.readTree(line(child,reader));assertEquals(1,denied.get("failedInvocations").asInt());assertEquals("QUEUED",jobs().read(who("reporter"),A,first.id()).state());
            writer.write("rotate\n");writer.flush();var accepted=codec.readTree(line(child,reader));assertEquals(1,accepted.get("completedAcknowledgements").asInt());assertEquals(2,accepted.get("credentialGeneration").asInt());assertEquals("SUCCEEDED",jobs().read(who("reporter"),A,first.id()).state());
            var second=jobs().enqueue(who("reporter"),A,report("Synthetic managed black laptop bag").reportId(),isolated.id());writer.write("invalidate\n");writer.flush();var paused=codec.readTree(line(child,reader));assertEquals("CREDENTIALS_INVALID",paused.get("reason").asString());assertEquals("QUEUED",jobs().read(who("reporter"),A,second.id()).state());
            writer.write("restore\n");writer.flush();var restored=codec.readTree(line(child,reader));assertEquals(2,restored.get("completedAcknowledgements").asInt());assertEquals("SUCCEEDED",jobs().read(who("reporter"),A,second.id()).state());
            var maintained=codec.readTree(line(child,reader));assertEquals(2,maintained.get("archivedProfiles").asInt());assertTrue(maintained.get("idleClaims").asInt()>=1);
            String output=denied.toString()+accepted+paused+restored+maintained;for(String secret:List.of(good,bad,A.toString(),first.id().toString(),"Synthetic managed"))assertFalse(output.contains(secret));
            assertTrue(child.waitFor(60,TimeUnit.SECONDS));assertEquals(0,child.exitValue(),Files.readString(log));
        } finally {if(child.isAlive()){child.destroyForcibly();child.waitFor(10,TimeUnit.SECONDS);}}
    }
}
