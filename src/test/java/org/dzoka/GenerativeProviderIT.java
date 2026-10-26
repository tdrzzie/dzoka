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

class GenerativeProviderIT extends AiFixture {
    @Override protected void initializeProviders()throws Exception{super.initializeProviders();try(var c=admin()){GenerativeJobsIT.profiles(c,A);}}
    private void runWorker(String name)throws Exception{
        var command=new ProcessBuilder("python","tools/generative_worker.py","--once");var env=command.environment();
        env.put("DZOKA_AI_CORE_URL",base);env.put("DZOKA_AI_TOKEN",token("inference"));env.put("DZOKA_AI_TENANT",A.toString());env.put("DZOKA_AI_PROFILE",PerceptionFixtures.id(name).toString());env.put("DZOKA_AI_GENERATIVE_DIR",Path.of(".audit-cache/generative-models").toAbsolutePath().toString());env.put("DZOKA_AI_ALLOW_INSECURE_LOOPBACK","true");
        Path log=Path.of(".test-tmp","generative-worker-"+UUID.randomUUID()+".log");command.redirectErrorStream(true).redirectOutput(log.toFile());var child=command.start();if(!child.waitFor(320,TimeUnit.SECONDS)){child.destroyForcibly();fail("Generative invocation exceeded bound");}assertEquals(0,child.exitValue(),Files.readString(log));
    }
    private UUID report(String text,String language){return app.getBean(IntakeService.class).submit(who("reporter"),A,null,null,new ReportDraft(ReportDraft.Kind.LOST,text,language,"UTC",null,null,null,null,null,null,null),"{}".getBytes(),UUID.randomUUID().toString()).reportId();}
    @Test void realGeneralDraftAndFourDirectedTranslationJobsPreserveAuthorityAndSelection()throws Exception{
        UUID english=report("I lost a black bag.","en"),spanish=report("Perdí una bolsa negra.","es"),zulu=report("Ngilahlekelwe isikhwama esimnyama.","zu");var jobs=app.getBean(PerceptionJobs.class);
        var draft=jobs.generate(who("reporter"),A,english,GenerativeJobsIT.prompt("Describe the reported item in one sentence."));runWorker("LLM_EN");assertEquals("SUCCEEDED",jobs.read(who("reporter"),A,draft.id()).state());var generated=jobs.result(who("reporter"),A,draft.id()).orElseThrow();assertEquals("UNTRUSTED_DRAFT",generated.evidence().get("trust"));assertFalse(generated.evidence().get("text").toString().isBlank());assertEquals("SHADOW",generated.selection());
        var pins=new ArrayList<PerceptionPin>();
        for(var entry:List.of(Map.entry("M2M_ES_EN",spanish),Map.entry("M2M_EN_DE",english),Map.entry("M2M_EN_ZU",english),Map.entry("M2M_ZU_EN",zulu))){
            var job=jobs.enqueue(who("reporter"),A,entry.getValue(),PerceptionFixtures.id(entry.getKey()),null,null);runWorker(entry.getKey());assertEquals("SUCCEEDED",jobs.read(who("reporter"),A,job.id()).state());var output=jobs.result(who("reporter"),A,job.id()).orElseThrow();assertFalse(output.evidence().get("text").toString().isBlank());String[] parts=entry.getKey().split("_");assertEquals(parts[1].toLowerCase(Locale.ROOT),output.evidence().get("sourceLanguage"));assertEquals(parts[2].toLowerCase(Locale.ROOT),output.evidence().get("targetLanguage"));pins.add(new PerceptionPin(entry.getValue(),output.id()));
        }
        var plan=new Plan(UUID.randomUUID(),List.of(new Source(english,1),new Source(spanish,1),new Source(zulu,1)),List.of(),10000,"e".repeat(64),pins);var rollouts=app.getBean(FingerprintRollouts.class);rollouts.create(who("curator"),A,plan);assertEquals(4,rollouts.seal(who("curator"),A,plan.id()).availableSlots());rollouts.activate(who("curator"),A,plan.id(),0,false);
        var snapshot=app.getBean(FingerprintJobs.class).snapshot(who("reporter"),A,english);assertEquals(EvidenceState.AVAILABLE,snapshot.coverage().get("translation"));assertEquals(2,snapshot.perceptionComponents().size());assertTrue(snapshot.perceptionComponents().stream().noneMatch(c->c.task().equals("LLM")));assertEquals("SHADOW",jobs.result(who("reporter"),A,draft.id()).orElseThrow().selection());
        try(var c=admin();var s=c.prepareStatement("SELECT raw_text,language FROM dzoka.report_revisions WHERE tenant_id=? AND report_id=? AND revision=1")){s.setObject(1,A);s.setObject(2,zulu);try(var r=s.executeQuery()){r.next();assertEquals("Ngilahlekelwe isikhwama esimnyama.",r.getString(1));assertEquals("zu",r.getString(2));}}
    }
}
