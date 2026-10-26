package org.dzoka;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.dzoka.fingerprints.domain.FingerprintRollout.*;
import org.dzoka.fingerprints.domain.ItemFingerprint.EvidenceState;
import org.dzoka.fingerprints.ports.*;
import org.dzoka.intake.application.IntakeService;
import org.dzoka.intake.domain.ReportDraft;
import org.junit.jupiter.api.Test;

class QualityProviderIT extends AiFixture {
    @Override protected void initializeProviders()throws Exception{super.initializeProviders();try(var c=admin()){QualityJobsIT.profiles(c,A);}}
    private void runWorker(String name)throws Exception{
        var command=new ProcessBuilder("python","tools/quality_worker.py","--once");var env=command.environment();
        env.put("DZOKA_AI_CORE_URL",base);env.put("DZOKA_AI_TOKEN",token("inference"));env.put("DZOKA_AI_TENANT",A.toString());env.put("DZOKA_AI_PROFILE",PerceptionFixtures.id(name).toString());env.put("DZOKA_AI_QUALITY_DIR",Path.of(".audit-cache/quality-models").toAbsolutePath().toString());env.put("DZOKA_AI_ALLOW_INSECURE_LOOPBACK","true");
        Path log=Path.of(".test-tmp","quality-worker-"+UUID.randomUUID()+".log");command.redirectErrorStream(true).redirectOutput(log.toFile());var child=command.start();if(!child.waitFor(320,TimeUnit.SECONDS)){child.destroyForcibly();fail("Generative invocation exceeded bound");}assertEquals(0,child.exitValue(),Files.readString(log));
    }
    private UUID report(String text,String language){return app.getBean(IntakeService.class).submit(who("reporter"),A,null,null,new ReportDraft(ReportDraft.Kind.LOST,text,language,"UTC",null,null,null,null,null,null,null),"{}".getBytes(),UUID.randomUUID().toString()).reportId();}
    @Test void fourRealAlternativesPreserveOriginalShonaAndTypedSelection()throws Exception{
        String shona="Ndarasa bhegi rangu.";UUID english=report("I lost a black bag.","en"),sn=report(shona,"sn-ZW"),es=report("Perdí una bolsa negra.","es"),de=report("Ich habe eine schwarze Tasche verloren.","de");var jobs=app.getBean(PerceptionJobs.class);
        byte[] prompt=new tools.jackson.databind.ObjectMapper().writeValueAsBytes(Map.of("profileId",PerceptionFixtures.id("SMOL360_EN"),"prompt","Describe the reported item in one sentence."));var draft=jobs.generate(who("reporter"),A,english,prompt);runWorker("SMOL360_EN");var generated=jobs.result(who("reporter"),A,draft.id()).orElseThrow();assertEquals("UNTRUSTED_DRAFT",generated.evidence().get("trust"));assertEquals("SHADOW",generated.selection());
        var pins=new ArrayList<PerceptionPin>();
        for(var entry:List.of(Map.entry("OPUS_SN_EN",sn),Map.entry("OPUS_ES_EN",es),Map.entry("OPUS_DE_EN",de))){
            var job=jobs.enqueue(who("reporter"),A,entry.getValue(),PerceptionFixtures.id(entry.getKey()),null,null);runWorker(entry.getKey());assertEquals("SUCCEEDED",jobs.read(who("reporter"),A,job.id()).state());var result=jobs.result(who("reporter"),A,job.id()).orElseThrow();assertEquals("UNVERIFIED_TRANSLATION",result.evidence().get("trust"));assertEquals("marian-source-target-v1",result.evidence().get("representation"));assertEquals("en",result.evidence().get("targetLanguage"));assertFalse(result.evidence().get("text").toString().isBlank());pins.add(new PerceptionPin(entry.getValue(),result.id()));
        }
        var plan=new Plan(UUID.randomUUID(),List.of(new Source(sn,1),new Source(es,1),new Source(de,1)),List.of(),10000,"e".repeat(64),pins);var rollouts=app.getBean(FingerprintRollouts.class);rollouts.create(who("curator"),A,plan);assertEquals(3,rollouts.seal(who("curator"),A,plan.id()).availableSlots());rollouts.activate(who("curator"),A,plan.id(),0,false);var snapshot=app.getBean(FingerprintJobs.class).snapshot(who("reporter"),A,sn);assertEquals(EvidenceState.AVAILABLE,snapshot.coverage().get("translation"));assertEquals(1,snapshot.perceptionComponents().size());assertEquals("SHADOW",jobs.result(who("reporter"),A,draft.id()).orElseThrow().selection());
        try(var c=admin();var q=c.prepareStatement("SELECT raw_text,language FROM dzoka.report_revisions WHERE tenant_id=? AND report_id=? AND revision=1")){q.setObject(1,A);q.setObject(2,sn);try(var r=q.executeQuery()){r.next();assertEquals(shona,r.getString(1));assertEquals("sn-ZW",r.getString(2));}}
    }
}
