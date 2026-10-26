package org.dzoka;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.dzoka.fingerprints.ports.*;
import org.dzoka.fingerprints.domain.FingerprintRollout.*;
import org.dzoka.fingerprints.domain.ItemFingerprint.EvidenceState;
import org.dzoka.intake.application.IntakeService;
import org.dzoka.intake.domain.ReportDraft;
import org.junit.jupiter.api.Test;

class LanguageProviderIT extends AiFixture {
    @Override protected void initializeProviders()throws Exception{super.initializeProviders();try(var c=admin()){LanguageJobsIT.profiles(c,A);}}
    private void runWorker(String task)throws Exception{
        var command=new ProcessBuilder("python","tools/language_worker.py","--once");var env=command.environment();
        env.put("DZOKA_AI_CORE_URL",base);env.put("DZOKA_AI_TOKEN",token("inference"));env.put("DZOKA_AI_TENANT",A.toString());env.put("DZOKA_AI_PROFILE",PerceptionFixtures.id(task).toString());
        env.put("DZOKA_AI_LANGUAGE_DIR",Path.of(".audit-cache/language-models").toAbsolutePath().toString());env.put("DZOKA_AI_ALLOW_INSECURE_LOOPBACK","true");
        Path log=Path.of(".test-tmp","language-worker-"+UUID.randomUUID()+".log");command.redirectErrorStream(true).redirectOutput(log.toFile());
        var child=command.start();if(!child.waitFor(140,TimeUnit.SECONDS)){child.destroyForcibly();fail("Language invocation exceeded bound");}assertEquals(0,child.exitValue(),Files.readString(log));
    }
    @Test void actualLicensedGenerationAndRerankingPublishPrivateDerivedEvidenceAndOnlyIntrinsicPins()throws Exception{
        UUID english=report("I lost a black Lenovo bag at Joina City around 2pm.").reportId();
        UUID french=app.getBean(IntakeService.class).submit(who("reporter"),A,null,null,new ReportDraft(ReportDraft.Kind.LOST,"J'ai perdu un sac noir.","fr","UTC",null,null,null,null,null,null,null),"{}".getBytes(),UUID.randomUUID().toString()).reportId();
        var jobs=app.getBean(PerceptionJobs.class);var attributes=jobs.enqueue(who("reporter"),A,english,PerceptionFixtures.id("ATTRIBUTES"),null,null);runWorker("ATTRIBUTES");assertEquals("SUCCEEDED",jobs.read(who("reporter"),A,attributes.id()).state());
        var translation=jobs.enqueue(who("reporter"),A,french,PerceptionFixtures.id("TRANSLATION"),null,null);runWorker("TRANSLATION");assertEquals("SUCCEEDED",jobs.read(who("reporter"),A,translation.id()).state());
        UUID relevant=UUID.randomUUID(),other=UUID.randomUUID();String raw="{\"profileId\":\""+PerceptionFixtures.id("RERANK")+"\",\"candidates\":[{\"id\":\""+other+"\",\"text\":\"A red umbrella\"},{\"id\":\""+relevant+"\",\"text\":\"A black Lenovo travel bag found at Joina City\"}]}";
        var rerank=jobs.rerank(who("reporter"),A,english,raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));runWorker("RERANK");assertEquals("SUCCEEDED",jobs.read(who("reporter"),A,rerank.id()).state());
        var attr=jobs.result(who("reporter"),A,attributes.id()).orElseThrow();var trans=jobs.result(who("reporter"),A,translation.id()).orElseThrow();var ranked=jobs.result(who("reporter"),A,rerank.id()).orElseThrow();
        var fields=(Map<?,?>)attr.evidence().get("fields");assertEquals(5,fields.size());assertTrue(fields.values().stream().anyMatch(v->"AVAILABLE".equals(((Map<?,?>)v).get("state"))));
        assertEquals("Lenovo",((Map<?,?>)fields.get("brand")).get("quote"));assertEquals("black",((Map<?,?>)fields.get("colour")).get("quote"));
        assertEquals("around 2pm",((Map<?,?>)fields.get("time")).get("quote"));assertEquals("UNINTERPRETED",((Map<?,?>)fields.get("time")).get("uncertainty"));
        assertTrue(trans.evidence().get("text").toString().toLowerCase(Locale.ROOT).contains("bag"));assertNotEquals("J'ai perdu un sac noir.",trans.evidence().get("text"));
        var scores=(List<?>)ranked.evidence().get("candidates");assertEquals(2,scores.size());assertEquals(relevant.toString(),((Map<?,?>)scores.getFirst()).get("id"));
        assertThrows(org.dzoka.foundation.application.Failures.NotFound.class,()->jobs.result(who("other"),A,rerank.id()));
        UUID missing=report("I lost a bag.").reportId();var sparse=jobs.enqueue(who("reporter"),A,missing,PerceptionFixtures.id("ATTRIBUTES"),null,null);runWorker("ATTRIBUTES");var unavailable=(Map<?,?>)jobs.result(who("reporter"),A,sparse.id()).orElseThrow().evidence().get("fields");
        for(String field:List.of("brand","colour","location","time"))assertEquals("UNAVAILABLE",((Map<?,?>)unavailable.get(field)).get("state"));
        var plan=new Plan(UUID.randomUUID(),List.of(new Source(english,1),new Source(french,1)),List.of(),10000,"e".repeat(64),List.of(new PerceptionPin(english,attr.id()),new PerceptionPin(french,trans.id())));
        var rollouts=app.getBean(FingerprintRollouts.class);rollouts.create(who("curator"),A,plan);assertEquals(2,rollouts.seal(who("curator"),A,plan.id()).availableSlots());rollouts.activate(who("curator"),A,plan.id(),0,false);
        var fingerprints=app.getBean(FingerprintJobs.class);assertEquals(EvidenceState.AVAILABLE,fingerprints.snapshot(who("reporter"),A,english).coverage().get("attributes"));assertEquals(EvidenceState.AVAILABLE,fingerprints.snapshot(who("reporter"),A,french).coverage().get("translation"));
        assertEquals("SHADOW",jobs.result(who("reporter"),A,rerank.id()).orElseThrow().selection());
        try(var c=admin();var s=c.prepareStatement("SELECT raw_text,language FROM dzoka.report_revisions WHERE tenant_id=? AND report_id=? AND revision=1")){s.setObject(1,A);s.setObject(2,french);try(var r=s.executeQuery()){r.next();assertEquals("J'ai perdu un sac noir.",r.getString(1));assertEquals("fr",r.getString(2));}}
    }
}
