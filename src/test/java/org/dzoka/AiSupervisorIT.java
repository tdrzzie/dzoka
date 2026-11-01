package org.dzoka;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.dzoka.fingerprints.ports.PerceptionJobs;
import org.dzoka.intake.application.IntakeService;
import org.dzoka.intake.domain.ReportDraft;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class AiSupervisorIT extends AiFixture {
    @Override protected void initializeProviders()throws Exception{super.initializeProviders();try(var c=admin()){LanguageJobsIT.profiles(c,A);}}
    Map<String,Object> entry(String family,UUID profile,String directory,String token){return Map.of("family",family,"tenant",A.toString(),"profile",profile.toString(),"modelDirectory",Path.of(directory).toAbsolutePath().toString(),"tokenEnvironment",token);}
    String run(List<Map<String,Object>> entries,int rounds,int expected)throws Exception{
        var codec=new ObjectMapper();Path plan=Path.of(".test-tmp","supervisor-plan-"+UUID.randomUUID()+".json"),log=Path.of(".test-tmp","supervisor-"+UUID.randomUUID()+".log");
        Files.write(plan,codec.writeValueAsBytes(Map.of("schemaVersion",1,"coreUrl",base,"allowInsecureLoopback",true,"pollSeconds",1,"backoffMaxSeconds",4,"entries",entries)));
        var command=new ProcessBuilder("python","tools/ai_supervisor.py","--plan",plan.toString(),"--rounds",Integer.toString(rounds));var env=command.environment();
        String goodToken=token("inference"),remoteToken=token("remote");env.put("DZOKA_WORKER_TOKEN_GOOD",goodToken);env.put("DZOKA_WORKER_TOKEN_REMOTE",remoteToken);
        env.put("DZOKA_AI_TENANT",B.toString());env.put("DZOKA_AI_SPACE",UUID.randomUUID().toString());env.put("DATABASE_URL","unusable inherited credential");
        command.redirectErrorStream(true).redirectOutput(log.toFile());var child=command.start();if(!child.waitFor(260,TimeUnit.SECONDS)){child.destroyForcibly();fail("Supervisor test deadline exceeded");}
        String output=Files.readString(log);assertEquals(expected,child.exitValue(),output);
        for(String secret:List.of(goodToken,remoteToken,A.toString(),B.toString(),"Synthetic private supervisor source","unusable inherited credential"))assertFalse(output.contains(secret));
        return output;
    }
    @Test void twoPollingCyclesPublishRealEmbeddingsWithoutCreatingDuplicateJobs()throws Exception{
        UUID first=report("Synthetic private supervisor source blue travel bag").reportId(),second=report("Synthetic second black laptop bag").reportId();
        var one=jobs().enqueue(who("reporter"),A,first,primary);var two=jobs().enqueue(who("reporter"),A,second,primary);
        String output=run(List.of(entry("text",primary,".audit-cache/ai-model","DZOKA_WORKER_TOKEN_GOOD")),2,0);
        assertTrue(output.contains("\"invocations\": 2"));assertTrue(output.contains("\"successfulInvocations\": 2"));
        for(var job:List.of(one,two)){assertEquals("SUCCEEDED",jobs().read(who("reporter"),A,job.id()).state());var components=jobs().current(who("reporter"),A,job.reportId());assertEquals(1,components.size());assertEquals(profile.spaceKey(),components.getFirst().space().spaceKey());assertEquals(384,components.getFirst().vector().length);}
        assertEquals(one.id(),jobs().enqueue(who("reporter"),A,first,primary).id());
    }
    @Test void wrongTenantCredentialCannotClaimAndDoesNotStarveAHealthyLanguageAssignment()throws Exception{
        UUID report=report("Synthetic private supervisor source blue bag").reportId();var blocked=jobs().enqueue(who("reporter"),A,report,primary);
        UUID french=app.getBean(IntakeService.class).submit(who("reporter"),A,null,null,new ReportDraft(ReportDraft.Kind.LOST,"J'ai perdu un sac noir.","fr","UTC",null,null,null,null,null,null,null),"{}".getBytes(),UUID.randomUUID().toString()).reportId();
        var perception=app.getBean(PerceptionJobs.class);var translation=perception.enqueue(who("reporter"),A,french,PerceptionFixtures.id("TRANSLATION"),null,null);
        String output=run(List.of(entry("text",primary,".audit-cache/ai-model","DZOKA_WORKER_TOKEN_REMOTE"),entry("language",PerceptionFixtures.id("TRANSLATION"),".audit-cache/language-models","DZOKA_WORKER_TOKEN_GOOD")),1,1);
        assertTrue(output.contains("\"failedInvocations\": 1"));assertTrue(output.contains("\"successfulInvocations\": 1"));assertEquals("QUEUED",jobs().read(who("reporter"),A,blocked.id()).state());
        assertEquals("SUCCEEDED",perception.read(who("reporter"),A,translation.id()).state());var evidence=perception.result(who("reporter"),A,translation.id()).orElseThrow().evidence();assertEquals("opus-fr-en-greedy-v1",evidence.get("representation"));assertEquals("fr",evidence.get("sourceLanguage"));assertEquals("en",evidence.get("targetLanguage"));assertFalse(evidence.get("text").toString().isBlank());
    }
}
