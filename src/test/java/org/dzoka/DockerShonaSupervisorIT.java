package org.dzoka;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.dzoka.fingerprints.ports.*;
import org.dzoka.foundation.application.Failures;
import org.dzoka.intake.application.IntakeService;
import org.dzoka.intake.domain.ReportDraft;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
class DockerShonaSupervisorIT extends AiFixture {
    @Override @SuppressWarnings("unchecked") protected void initializeProviders()throws Exception{
        super.initializeProviders();var codec=new ObjectMapper();Map<String,Map<String,Object>> profiles=codec.readValue(Files.readAllBytes(Path.of("ai/shona-supervised-profiles.json")),Map.class);
        try(var c=admin()){
            for(var entry:profiles.entrySet())try(var s=c.prepareStatement("INSERT INTO dzoka.perception_profiles(tenant_id,id,task,profile_key,manifest,enabled) VALUES(?,?,?,?,CAST(? AS jsonb),true)")){
                var value=entry.getValue();var manifest=(Map<String,Object>)value.get("manifest");s.setObject(1,A);s.setObject(2,PerceptionFixtures.id(entry.getKey()));s.setString(3,(String)manifest.get("task"));s.setString(4,(String)value.get("key"));s.setString(5,codec.writeValueAsString(manifest));s.executeUpdate();
            }
        }
    }
    @Test void fiveRealSupervisedShonaProfilesPublishPrivateGroundedEvidenceAndRemoveTheirContainers()throws Exception{
        var sources=new LinkedHashMap<String,String>();sources.put("SUP_CPU_EN_SN","I lost my bag.");sources.put("SUP_CPU_SN_EN","Ndarasa bhegi rangu.");sources.put("SUP_CPU_EN_SN_KEEP","I left a black bag in [[keep:Joina City]].");sources.put("SUP_CPU_SN_EN_KEEP","Ndasiya [[keep:black Lenovo bag]] mu[[keep:Joina]] [[keep:around 2pm]].");sources.put("SUP_CPU_SN_EN_AUTO","Ndasiya black Lenovo bag muJoina around 2pm.");
        var jobs=app.getBean(PerceptionJobs.class);var queued=new LinkedHashMap<String,UUID>();var entries=new ArrayList<Map<String,Object>>();Path state=Files.createDirectory(Path.of(".test-tmp","docker-supervisor-state-"+UUID.randomUUID())).toAbsolutePath();
        for(var entry:sources.entrySet()){
            String language=entry.getKey().contains("_EN_SN")?"en":"sn-ZW";
            UUID report=app.getBean(IntakeService.class).submit(who("reporter"),A,null,null,new ReportDraft(ReportDraft.Kind.LOST,entry.getValue(),language,"UTC",null,null,null,null,null,null,null),"{}".getBytes(),UUID.randomUUID().toString()).reportId();
            var job=jobs.enqueue(who("reporter"),A,report,PerceptionFixtures.id(entry.getKey()),null,null);queued.put(entry.getKey(),job.id());assertEquals(job.id(),jobs.enqueue(who("reporter"),A,report,PerceptionFixtures.id(entry.getKey()),null,null).id());
            entries.add(Map.of("family","shona-cpu","tenant",A.toString(),"profile",PerceptionFixtures.id(entry.getKey()).toString(),"modelDirectory",Path.of(".audit-cache/shona-cpu-models").toAbsolutePath().toString(),"tokenEnvironment","DZOKA_WORKER_TOKEN_SHONA","dockerStateDirectory",state.toString()));
        }
        var codec=new ObjectMapper();Path plan=Path.of(".test-tmp","docker-supervisor-plan-"+UUID.randomUUID()+".json"),log=Path.of(".test-tmp","docker-supervisor-"+UUID.randomUUID()+".log");
        Files.write(plan,codec.writeValueAsBytes(Map.of("schemaVersion",1,"coreUrl",base,"allowInsecureLoopback",true,"pollSeconds",1,"backoffMaxSeconds",4,"entries",entries)));
        var command=new ProcessBuilder("python","tools/verify_service_supervisor.py","--plan",plan.toString(),"--rounds","1");String secret=token("inference");command.environment().put("DZOKA_WORKER_TOKEN_SHONA",secret);command.environment().put("DZOKA_AI_DOCKER_KEY","inherited-untrusted-key");command.redirectErrorStream(true).redirectOutput(log.toFile());var child=command.start();
        if(!child.waitFor(1600,TimeUnit.SECONDS)){child.destroyForcibly();fail("Supervised Shona invocation budget exceeded");}String output=Files.readString(log);assertEquals(0,child.exitValue(),output);assertFalse(output.contains(secret));assertFalse(output.contains(A.toString()));assertFalse(output.contains("black Lenovo"));
        for(var entry:queued.entrySet()){
            assertEquals("SUCCEEDED",jobs.read(who("reporter"),A,entry.getValue()).state());var result=jobs.result(who("reporter"),A,entry.getValue()).orElseThrow();assertEquals("UNVERIFIED_TRANSLATION",result.evidence().get("trust"));assertFalse(result.evidence().get("text").toString().isBlank());assertThrows(Failures.NotFound.class,()->jobs.result(who("other"),A,entry.getValue()));
            if(entry.getKey().endsWith("KEEP"))assertTrue(result.evidence().get("text").toString().contains("Joina"));
            if(entry.getKey().endsWith("AUTO")){assertEquals("PROPOSED",result.evidence().get("proposalStatus"));assertTrue(result.evidence().get("text").toString().contains("around 2pm"));assertEquals("AUTO_LANGUAGE_PROPOSAL_UNVERIFIED",((Map<?,?>)((List<?>)result.evidence().get("proposedSpans")).getFirst()).get("trust"));}
            try(var c=admin();var q=c.prepareStatement("SELECT raw_text FROM dzoka.report_revisions WHERE tenant_id=? AND report_id=? AND revision=1")){q.setObject(1,A);q.setObject(2,result.reportId());try(var r=q.executeQuery()){assertTrue(r.next());assertEquals(sources.get(entry.getKey()),r.getString(1));}}
        }
        int records=0;try(var files=Files.list(state)){for(Path file:files.filter(p->p.toString().endsWith(".json")).toList()){var record=codec.readTree(Files.readAllBytes(file));var history=record.get("history");assertEquals("REMOVED",history.get(history.size()-1).get("state").asString());records++;}}
        assertEquals(5,records);
    }
}
