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

class ShonaCpuProviderIT extends AiFixture {
    @Override protected void initializeProviders()throws Exception{super.initializeProviders();try(var c=admin()){ShonaCpuJobsIT.profiles(c,A);}}
    private void runWorker(String name)throws Exception{
        var command=new ProcessBuilder("python","tools/shona_cpu_worker.py","--once");var env=command.environment();
        env.put("DZOKA_AI_CORE_URL",base);env.put("DZOKA_AI_TOKEN",token("inference"));env.put("DZOKA_AI_TENANT",A.toString());env.put("DZOKA_AI_PROFILE",PerceptionFixtures.id(name).toString());env.put("DZOKA_AI_SHONA_CPU_DIR",Path.of(".audit-cache/shona-cpu-models").toAbsolutePath().toString());env.put("DZOKA_AI_ALLOW_INSECURE_LOOPBACK","true");
        Path log=Path.of(".test-tmp","shona-worker-"+UUID.randomUUID()+".log");command.redirectErrorStream(true).redirectOutput(log.toFile());var child=command.start();if(!child.waitFor(320,TimeUnit.SECONDS)){child.destroyForcibly();fail("Generative invocation exceeded bound");}assertEquals(0,child.exitValue(),Files.readString(log));
    }
    private UUID report(String text,String language){return app.getBean(IntakeService.class).submit(who("reporter"),A,null,null,new ReportDraft(ReportDraft.Kind.LOST,text,language,"UTC",null,null,null,null,null,null,null),"{}".getBytes(),UUID.randomUUID().toString()).reportId();}
    @Test void fiveRealCpuDirectedMarkedAndAutomaticProfilesPreservePrivateRawSourceAndActivation()throws Exception{
        var sources=new LinkedHashMap<String,String>();sources.put("CPU_SN_EN_AUTO","Ndasiya black Lenovo bag muJoina around 2pm.");sources.put("CPU_EN_SN","I lost my bag.");sources.put("CPU_SN_EN","Ndasiya black Lenovo bag muJoina around 2pm.");sources.put("CPU_SN_EN_KEEP","Ndasiya [[keep:black Lenovo bag]] mu[[keep:Joina]] [[keep:around 2pm]].");sources.put("CPU_EN_SN_KEEP","I left a black bag in [[keep:Joina City]].");
        var jobs=app.getBean(PerceptionJobs.class);var cohort=new ArrayList<Source>();var pins=new ArrayList<PerceptionPin>();
        for(var entry:sources.entrySet()){
            String language=entry.getKey().startsWith("CPU_EN")?"en":"sn-ZW";UUID report=report(entry.getValue(),language);var job=jobs.enqueue(who("reporter"),A,report,PerceptionFixtures.id(entry.getKey()),null,null);runWorker(entry.getKey());assertEquals("SUCCEEDED",jobs.read(who("reporter"),A,job.id()).state());var result=jobs.result(who("reporter"),A,job.id()).orElseThrow();assertEquals("UNVERIFIED_TRANSLATION",result.evidence().get("trust"));assertFalse(result.evidence().get("text").toString().isBlank());
            if(entry.getKey().endsWith("KEEP")){assertEquals("ctranslate2-marked-v1",result.evidence().get("representation"));assertFalse(((List<?>)result.evidence().get("preservedSpans")).isEmpty());String text=result.evidence().get("text").toString();assertTrue(text.contains("Joina"));if(entry.getKey().equals("CPU_SN_EN_KEEP")){assertTrue(text.contains("black Lenovo bag"));assertTrue(text.contains("around 2pm"));}}
            if(entry.getKey().endsWith("AUTO")){assertEquals("ctranslate2-auto-v1",result.evidence().get("representation"));assertEquals("PROPOSED",result.evidence().get("proposalStatus"));assertTrue(result.evidence().get("text").toString().contains("around 2pm"));}
            cohort.add(new Source(report,1));pins.add(new PerceptionPin(report,result.id()));try(var c=admin();var q=c.prepareStatement("SELECT raw_text,language FROM dzoka.report_revisions WHERE tenant_id=? AND report_id=? AND revision=1")){q.setObject(1,A);q.setObject(2,report);try(var r=q.executeQuery()){r.next();assertEquals(entry.getValue(),r.getString(1));assertEquals(language,r.getString(2));}}
        }
        var plan=new Plan(UUID.randomUUID(),cohort,List.of(),10000,"e".repeat(64),pins);var rollouts=app.getBean(FingerprintRollouts.class);rollouts.create(who("curator"),A,plan);assertEquals(5,rollouts.seal(who("curator"),A,plan.id()).availableSlots());rollouts.activate(who("curator"),A,plan.id(),0,false);assertEquals(EvidenceState.AVAILABLE,app.getBean(FingerprintJobs.class).snapshot(who("reporter"),A,cohort.getFirst().reportId()).coverage().get("translation"));
    }
}
