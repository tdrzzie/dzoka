package org.dzoka;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.dzoka.fingerprints.domain.EmbeddingSpace;
import org.dzoka.fingerprints.domain.FingerprintRollout.*;
import org.dzoka.fingerprints.ports.*;
import org.dzoka.foundation.application.Failures;
import org.dzoka.intake.application.IntakeService;
import org.dzoka.intake.domain.ReportDraft;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class BrokerProviderIT extends AiFixture {
    EmbeddingSpace isolated;
    @Override protected void initializeProviders()throws Exception {
        super.initializeProviders();
        var root=new ObjectMapper().readTree(Files.readString(Path.of("ai/broker-profiles.json")));
        var n=root.get("ISO_TEXT").get("space");
        isolated=new EmbeddingSpace(PerceptionFixtures.id("ISO_TEXT"),n.get("provider").asString(),n.get("model").asString(),n.get("artifactRevision").asString(),n.get("artifactSha256").asString(),n.get("tokenizerSha256").asString(),n.get("preprocessing").asString(),n.get("dimensions").asInt(),n.get("metric").asString(),n.get("normalization").asString(),n.get("configSha256").asString(),n.get("spaceKey").asString());
        insertSpace(A,isolated);
        try(var c=admin()) {
            for(var property:root.properties()) {
                String name=property.getKey();if(name.equals("ISO_TEXT"))continue;
                var value=property.getValue();
                try(var s=c.prepareStatement("INSERT INTO dzoka.perception_profiles(tenant_id,id,task,profile_key,manifest,enabled) VALUES(?,?,?,?,CAST(? AS jsonb),true)")) {
                    s.setObject(1,A);s.setObject(2,PerceptionFixtures.id(name));s.setString(3,value.get("manifest").get("task").asString());s.setString(4,value.get("key").asString());s.setString(5,value.get("manifest").toString());s.executeUpdate();
                }
            }
        }
    }
    void run(String name,String subject,int expected)throws Exception {
        // The private synthetic state is task-owned and removed only after a
        // successful invocation has confirmed native/profile cleanup.
        String script="""
import os,sys
from pathlib import Path
sys.path.insert(0,'tools');sys.path.insert(0,'ai')
from verify_worker_boundary import private_fixture
from broker_bundle import protect
from model_broker import run_once
with private_fixture() as state:
 protect(state)
 for name in ('profiles','docker'):
  (state/name).mkdir();protect(state/name)
 run_once(os.environ['DZOKA_AI_CORE_URL'],os.environ['DZOKA_AI_TOKEN'],os.environ['DZOKA_AI_TENANT'],os.environ['DZOKA_AI_PROFILE'],os.environ['DZOKA_AI_BROKER_PROFILE'],os.environ['DZOKA_TEST_BROKER_BUNDLE'],os.environ['DZOKA_TEST_BROKER_SHA256'],state)
print('Broker test invocation completed')
""";
        if(expected==0)script="""
import os,sys,json,threading
from pathlib import Path
bundle=Path(os.environ['DZOKA_TEST_BROKER_BUNDLE'])
sys.path.insert(0,str(bundle/'tools'));sys.path.insert(0,str(bundle/'ai'))
from verify_worker_boundary import private_fixture
from broker_bundle import protect
from service_account import identity
from worker_service import execute
from broker_supervisor import BrokerSupervisor
stop=threading.Event();original=BrokerSupervisor.tick
def once(self):
 try:original(self)
 finally:stop.set()
BrokerSupervisor.tick=once
with private_fixture() as root:
 protect(root)
 for name in ('broker','broker/profiles','broker/docker','service'):
  (root/name).mkdir();protect(root/name)
 entry={'tenant':os.environ['DZOKA_AI_TENANT'],'profile':os.environ['DZOKA_AI_PROFILE'],'profileName':os.environ['DZOKA_AI_BROKER_PROFILE'],'bundle':str(bundle),'bundleSha256':os.environ['DZOKA_TEST_BROKER_SHA256'],'stateDirectory':str(root/'broker'),'tokenEnvironment':'DZOKA_WORKER_TOKEN_ONE'}
 who=identity()
 values={'plan.json':{'schemaVersion':1,'coreUrl':os.environ['DZOKA_AI_CORE_URL'],'allowInsecureLoopback':True,'pollSeconds':1,'backoffMaxSeconds':4,'entries':[entry]},'credentials.json':{'DZOKA_WORKER_TOKEN_ONE':os.environ['DZOKA_AI_TOKEN']},'service.json':{'schemaVersion':3,'plan':str(root/'plan.json'),'credentials':str(root/'credentials.json'),'stateDirectory':str(root/'service'),'accountSid':who['sid'],'integrityRid':who['integrity']}}
 for name,value in values.items():
  path=root/name;path.write_bytes(json.dumps(value).encode());protect(path)
 execute(root/'service.json',stop,lambda:None,require_account=True)
 assert json.loads((root/'service/status.json').read_bytes())['phase']=='STOPPED'
print('Checked broker service invocation completed')
""";
        // Cover protected startup and the six-minute helper window with a test-only token.
        var command=new ProcessBuilder("python","-I","-B","-c",script);var env=command.environment();String credential=token(subject,issuer,"dzoka-core",java.time.Instant.now().plusSeconds(1200),key);
        env.put("DZOKA_AI_CORE_URL",base);env.put("DZOKA_AI_TOKEN",credential);env.put("DZOKA_AI_TENANT",A.toString());env.put("DZOKA_AI_PROFILE",PerceptionFixtures.id(name).toString());env.put("DZOKA_AI_BROKER_PROFILE",name);env.put("DZOKA_AI_ALLOW_INSECURE_LOOPBACK","true");env.put("DATABASE_URL","synthetic forbidden inherited credential");
        Path log=Path.of(".test-tmp","broker-http-"+UUID.randomUUID()+".log");command.redirectErrorStream(true).redirectOutput(log.toFile());var child=command.start();if(!child.waitFor(360,TimeUnit.SECONDS)){child.destroyForcibly();fail("Isolated broker deadline exceeded");}
        String output=Files.readString(log);assertEquals(expected,child.exitValue(),output);assertFalse(output.contains(credential));assertFalse(output.contains("synthetic forbidden inherited credential"));
    }
    UUID report(String text,String language) {
        return app.getBean(IntakeService.class).submit(who("reporter"),A,null,null,new ReportDraft(ReportDraft.Kind.LOST,text,language,"UTC",null,null,null,null,null,null,null),text.getBytes(java.nio.charset.StandardCharsets.UTF_8),UUID.randomUUID().toString()).reportId();
    }
    @Test void refinedTranslationAndSpeechCompleteWithExplicitNewRepresentations()throws Exception {
        var jobs=app.getBean(PerceptionJobs.class);
        String translation="ISO_REFINEMENT_OPUS_ES_EN",speech="ISO_REFINEMENT_WHISPER_EN";
        UUID spanish=report("Perdí una bolsa negra.","es"),english=report("Synthetic authored speech reference").reportId();
        var translated=jobs.enqueue(who("reporter"),A,spanish,PerceptionFixtures.id(translation),null,null);
        run(translation,"inference",0);
        var result=jobs.result(who("reporter"),A,translated.id()).orElseThrow();
        assertEquals("marian-budget-v2",result.evidence().get("representation"));assertEquals("UNVERIFIED_TRANSLATION",result.evidence().get("trust"));assertEquals("SHADOW",result.selection());
        byte[] raw=Files.readAllBytes(Path.of(required("DZOKA_TEST_SPEECH_WAV")));
        var spoken=jobs.enqueue(who("reporter"),A,english,PerceptionFixtures.id(speech),null,raw);run(speech,"inference",0);
        var transcription=jobs.result(who("reporter"),A,spoken.id()).orElseThrow();
        assertEquals("whisper-en-greedy-v2",transcription.evidence().get("representation"));assertEquals("SHADOW",transcription.selection());
        assertThrows(Failures.NotFound.class,()->jobs.result(who("other"),A,spoken.id()));
        assertEquals(spoken.id(),jobs.enqueue(who("reporter"),A,english,PerceptionFixtures.id(speech),null,raw).id());
    }
    @Test void distilSpeechCompletesAsPrivateVersionedEvidence() throws Exception {
        String name = "ISO_REFINEMENT_WHISPER_DISTIL_SMALL_EN";
        byte[] audio = Files.readAllBytes(Path.of(required("DZOKA_TEST_SPEECH_WAV")));
        UUID reportId = report("Synthetic authored speech reference").reportId();
        var jobs = app.getBean(PerceptionJobs.class);
        var job = jobs.enqueue(who("reporter"), A, reportId, PerceptionFixtures.id(name), null, audio);
        run(name, "inference", 0);
        assertEquals("SUCCEEDED", jobs.read(who("reporter"), A, job.id()).state());
        var result = jobs.result(who("reporter"), A, job.id()).orElseThrow();
        var profile = new ObjectMapper().readTree(Files.readString(Path.of("ai/broker-profiles.json"))).get(name);
        assertEquals(profile.get("key").asString(), result.profileKey());
        assertEquals(HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(audio)), result.inputSha256());
        assertEquals("whisper-en-greedy-v2", result.evidence().get("representation"));
        assertFalse(result.evidence().get("text").toString().isBlank());
        assertEquals("SHADOW", result.selection());
        assertThrows(Failures.NotFound.class, () -> jobs.result(who("other"), A, job.id()));
    }

    @Test void actualIsolatedTextHasNewSpaceAndWrongTenantCannotClaim()throws Exception {
        UUID report=report("Synthetic private blue travel bag").reportId();var job=jobs().enqueue(who("reporter"),A,report,isolated.id());
        run("ISO_TEXT","remote",1);assertEquals("QUEUED",jobs().read(who("reporter"),A,job.id()).state());
        run("ISO_TEXT","inference",0);assertEquals("SUCCEEDED",jobs().read(who("reporter"),A,job.id()).state());
        var component=jobs().current(who("reporter"),A,report).getFirst();assertEquals(isolated.spaceKey(),component.space().spaceKey());assertNotEquals(profile.spaceKey(),component.space().spaceKey());assertEquals(384,component.vector().length);
        assertEquals(job.id(),jobs().enqueue(who("reporter"),A,report,isolated.id()).id());assertThrows(Failures.NotFound.class,()->jobs().read(who("other"),A,job.id()));
        UUID another=report("Synthetic incompatible provider test").reportId();var legacy=jobs().enqueue(who("reporter"),A,another,primary);var lease=jobs().claim(who("inference"),A,primary).orElseThrow();
        var wrong=new org.dzoka.fingerprints.domain.EmbeddingResult(lease.lease(),lease.inputSha256(),profile.spaceKey(),isolated.provider(),"text-embedding-v1",java.time.Instant.now(),component.vector(),9,false);
        assertThrows(IllegalArgumentException.class,()->jobs().complete(who("inference"),A,legacy.id(),wrong));assertTrue(jobs().current(who("reporter"),A,another).isEmpty());
    }
    @Test void actualIsolatedLanguageAndJointTextCanActivateWhilePrivateRerankingRemainsShadow()throws Exception {
        var jobs=app.getBean(PerceptionJobs.class);UUID english=report("A black travel bag with a silver buckle and a large front pocket, reported beside the main entrance.").reportId(),french=report("J'ai perdu un sac noir.","fr");var pins=new ArrayList<PerceptionPin>();
        for(var entry:List.of(Map.entry("ISO_LANGUAGE_ATTRIBUTES",english),Map.entry("ISO_LANGUAGE_TRANSLATION",french),Map.entry("ISO_VISION_JOINT_TEXT",english))) {
            var job=jobs.enqueue(who("reporter"),A,entry.getValue(),PerceptionFixtures.id(entry.getKey()),null,null);run(entry.getKey(),"inference",0);assertEquals("SUCCEEDED",jobs.read(who("reporter"),A,job.id()).state());var component=jobs.result(who("reporter"),A,job.id()).orElseThrow();pins.add(new PerceptionPin(entry.getValue(),component.id()));assertThrows(Failures.NotFound.class,()->jobs.result(who("other"),A,job.id()));
        }
        assertTrue(((Number)jobs.result(who("reporter"),A,jobs.enqueue(who("reporter"),A,english,PerceptionFixtures.id("ISO_VISION_JOINT_TEXT"),null,null).id()).orElseThrow().evidence().get("tokenCount")).intValue()>16);
        UUID candidate=UUID.randomUUID();byte[] raw=new ObjectMapper().writeValueAsBytes(Map.of("profileId",PerceptionFixtures.id("ISO_LANGUAGE_RERANK"),"candidates",List.of(Map.of("id",candidate,"text","a black travel bag"))));
        var rerank=jobs.rerank(who("reporter"),A,english,raw);run("ISO_LANGUAGE_RERANK","inference",0);assertEquals("SHADOW",jobs.result(who("reporter"),A,rerank.id()).orElseThrow().selection());
        var plan=new Plan(UUID.randomUUID(),List.of(new Source(english,1),new Source(french,1)),List.of(),10000,"d".repeat(64),pins);var rollouts=app.getBean(FingerprintRollouts.class);rollouts.create(who("curator"),A,plan);assertEquals(3,rollouts.seal(who("curator"),A,plan.id()).availableSlots());rollouts.activate(who("curator"),A,plan.id(),0,false);
        assertEquals(2,this.jobs().snapshot(who("reporter"),A,english).perceptionComponents().size());
    }
    @Test void actualIsolatedDraftsAndCpuShonaPreserveUntrustedEvidenceAndRawSource()throws Exception {
        var jobs=app.getBean(PerceptionJobs.class);UUID english=report("I lost a black bag.").reportId();
        for(String name:List.of("ISO_GENERATIVE_LLM_EN","ISO_QUALITY_SMOL360_EN")) {
            byte[] raw=new ObjectMapper().writeValueAsBytes(Map.of("profileId",PerceptionFixtures.id(name),"prompt","Describe the reported item in one sentence."));
            var job=jobs.generate(who("reporter"),A,english,raw);run(name,"inference",0);var component=jobs.result(who("reporter"),A,job.id()).orElseThrow();assertEquals("UNTRUSTED_DRAFT",component.evidence().get("trust"));assertEquals("SHADOW",component.selection());
        }
        for(String name:List.of("CPU_EN_SN","CPU_SN_EN","CPU_EN_SN_KEEP","CPU_SN_EN_KEEP","CPU_SN_EN_AUTO")) {
            String text=switch(name) {case "CPU_EN_SN"->"I lost my bag.";case "CPU_EN_SN_KEEP"->"I left a black bag in [[keep:Joina City]].";case "CPU_SN_EN_KEEP"->"Ndasiya [[keep:black Lenovo bag]] mu[[keep:Joina]] [[keep:around 2pm]].";default->"Ndasiya black Lenovo bag muJoina around 2pm.";};
            UUID report=report(text,name.startsWith("CPU_EN")?"en":"sn-ZW");String isolatedName="ISO_SHONA_CPU_"+name;var job=jobs.enqueue(who("reporter"),A,report,PerceptionFixtures.id(isolatedName),null,null);run(isolatedName,"inference",0);var component=jobs.result(who("reporter"),A,job.id()).orElseThrow();assertEquals("UNVERIFIED_TRANSLATION",component.evidence().get("trust"));assertFalse(component.evidence().get("text").toString().isBlank());
            try(var c=admin();var s=c.prepareStatement("SELECT raw_text FROM dzoka.report_revisions WHERE tenant_id=? AND report_id=? AND revision=1")){s.setObject(1,A);s.setObject(2,report);try(var r=s.executeQuery()){r.next();assertEquals(text,r.getString(1));}}
        }
    }
}
