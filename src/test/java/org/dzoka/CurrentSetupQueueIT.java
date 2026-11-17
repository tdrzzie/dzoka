package org.dzoka;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.dzoka.fingerprints.domain.EmbeddingSpace;
import org.dzoka.foundation.application.Failures;
import org.dzoka.intake.application.IntakeService;
import org.dzoka.intake.domain.ReportDraft;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/** Actual installed-worker burst, backpressure, post-claim crash and lease recovery.
 * Authored English inputs establish operational behavior, not semantic quality. */
class CurrentSetupQueueIT extends AiFixture {
    EmbeddingSpace isolated;
    final ObjectMapper json=new ObjectMapper();
    @Override protected void initializeProviders() throws Exception {
        super.initializeProviders();
        var n=json.readTree(Files.readString(Path.of("ai/broker-profiles.json"))).get("ISO_TEXT").get("space");
        isolated=new EmbeddingSpace(PerceptionFixtures.id("ISO_TEXT"),n.get("provider").asString(),n.get("model").asString(),n.get("artifactRevision").asString(),n.get("artifactSha256").asString(),n.get("tokenizerSha256").asString(),n.get("preprocessing").asString(),n.get("dimensions").asInt(),n.get("metric").asString(),n.get("normalization").asString(),n.get("configSha256").asString(),n.get("spaceKey").asString());
        insertSpace(A,isolated);insertSpace(B,isolated);
        membership(B,"remote-reporter","reports:read","reports:write");
    }
    UUID enqueue(UUID tenant,UUID report,String subject,int status) throws Exception {
        var response=api("POST",tenant,"/reports/"+report+"/fingerprints",subject,"{\"spaceId\":\""+isolated.id()+"\"}");
        assertEquals(status,response.statusCode(),response.body());
        return status==200?UUID.fromString(json.readTree(response.body()).get("id").asString()):null;
    }
    @Test void fullBurstDrainsAfterActualWorkerCrashWithoutCrossTenantClaims() throws Exception {
        Path control=Files.createDirectory(Path.of(".test-tmp","queue-control-"+UUID.randomUUID())).toAbsolutePath();
        Path log=control.resolve("controller.log");
        var reports=new LinkedHashMap<UUID,UUID>();var started=new LinkedHashMap<UUID,Long>();
        for(int i=0;i<32;i++) {
            UUID report=report("Authored queue fixture: blue travel bag number "+i+"; location uncertain.").reportId();
            long begin=System.nanoTime();UUID id=enqueue(A,report,"reporter",200);reports.put(id,report);started.put(id,begin);
        }
        UUID overflow=report("Authored overflow fixture: green bag; date uncertain.").reportId();
        enqueue(A,overflow,"reporter",409);
        UUID first=reports.keySet().iterator().next();
        assertEquals(first,enqueue(A,reports.get(first),"reporter",200)); // replay reserves nothing, even at capacity
        var draft=new ReportDraft(ReportDraft.Kind.LOST,"Authored other-tenant queue fixture","en","UTC",null,null,null,null,null,null,null);
        UUID remoteReport=app.getBean(IntakeService.class).submit(who("remote-reporter"),B,null,null,draft,"{}".getBytes(),UUID.randomUUID().toString()).reportId();
        UUID remote=enqueue(B,remoteReport,"remote-reporter",200);
        assertEquals(404,api("GET",B,"/ai/jobs/"+first,"remote-reporter",null).statusCode());
        String script="""
import json,os,sys,time
from pathlib import Path
release=Path(os.environ['DZOKA_TEST_MANAGED_RELEASE']);sys.path[:0]=[str(release/'tools'),str(release/'ai')]
from verify_current_setup import installed_worker,wait,kill_owned,wait_running,snapshot
control=Path(os.environ['DZOKA_QUEUE_CONTROL']);timing={}
def alive():
 if (control/'abort').exists():raise AssertionError('Parent aborted queue acceptance; cleaning owned task')
with installed_worker(os.environ['DZOKA_QUEUE_CORE'],os.environ['DZOKA_QUEUE_TENANT'],os.environ['DZOKA_QUEUE_PROFILE'],'ISO_TEXT',os.environ['DZOKA_QUEUE_TOKEN'],timing) as (config,path,digest,root):
 wait(lambda:(alive() is None and (control/'crash-request').exists()),120)
 previous=kill_owned(config,path,digest)
 (control/'crashed').write_text('exact registered generation terminated')
 timing['watchdogReadyWaitSeconds']=wait_running(config,previous,720)
 def complete():
  alive()
  state=root/'service/managed-status.json'
  return state.exists() and json.loads(state.read_bytes()).get('completedAcknowledgements',0)>=33
 wait(complete,4200)
 (control/'controller-result.json').write_text(json.dumps({'readiness':timing,'restartedGeneration':snapshot(config)['run']!=previous,'acknowledgements':json.loads((root/'service/managed-status.json').read_bytes())['completedAcknowledgements']}))
print('Real queue drain and exact owned-task cleanup completed',flush=True)
""";
        var command=new ProcessBuilder("python","-I","-B","-c",script);var env=command.environment();
        String credential=token("inference",issuer,"dzoka-core",Instant.now().plusSeconds(7200),key);
        env.put("DZOKA_QUEUE_CORE",base);env.put("DZOKA_QUEUE_TENANT",A.toString());env.put("DZOKA_QUEUE_PROFILE",isolated.id().toString());env.put("DZOKA_QUEUE_TOKEN",credential);env.put("DZOKA_QUEUE_CONTROL",control.toString());
        command.redirectErrorStream(true).redirectOutput(log.toFile());var child=command.start();
        var completion=new LinkedHashMap<UUID,Double>();boolean requested=false,crashed=false,replenished=false;
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(6000);
        try {
            while(child.isAlive()&&System.nanoTime()<deadline) {
                var firstState=jobs().read(who("reporter"),A,first);
                if(!requested&&firstState.state().equals("PROCESSING")) {
                    assertEquals(1,firstState.attempts());Files.writeString(control.resolve("crash-request"),"claimed",StandardOpenOption.CREATE_NEW);requested=true;
                }
                if(!crashed&&Files.exists(control.resolve("crashed"))) {
                    var retained=jobs().read(who("reporter"),A,first);
                    assertEquals("PROCESSING",retained.state(),"Crash must precede acknowledgement");assertEquals(1,retained.attempts());crashed=true;
                }
                for(UUID id:reports.keySet()) {
                    if(completion.containsKey(id))continue;
                    var state=jobs().read(who("reporter"),A,id);
                    assertTrue(Set.of("QUEUED","PROCESSING","SUCCEEDED").contains(state.state()),"Unexpected queue terminal state: "+state);
                    if(state.state().equals("SUCCEEDED")) {
                        completion.put(id,(System.nanoTime()-started.get(id))/1e9);
                        System.out.println("Real installed queue progress: "+completion.size()+"/33 acknowledged observations");
                    }
                }
                if(!completion.isEmpty()&&!replenished) {
                    long begin=System.nanoTime();UUID id=enqueue(A,overflow,"reporter",200);reports.put(id,overflow);started.put(id,begin);replenished=true;
                }
                Thread.sleep(500);
            }
            assertTrue(child.waitFor(10,TimeUnit.SECONDS),"Bounded real queue controller deadline");
            assertEquals(0,child.exitValue(),Files.readString(log));assertTrue(requested&&crashed&&replenished);
            // A controller failure never becomes a latency sample or an inferred success.
            for(var entry:reports.entrySet()) {
                var state=jobs().read(who("reporter"),A,entry.getKey());assertEquals("SUCCEEDED",state.state());assertEquals(entry.getKey().equals(first)?2:1,state.attempts());
                completion.putIfAbsent(entry.getKey(),(System.nanoTime()-started.get(entry.getKey()))/1e9);
                var components=jobs().current(who("reporter"),A,entry.getValue());assertEquals(1,components.size());var component=components.getFirst();
                assertEquals(isolated.spaceKey(),component.space().spaceKey());assertEquals(384,component.vector().length);assertFalse(component.truncated());
                assertEquals(entry.getKey(),enqueue(A,entry.getValue(),"reporter",200));
                assertThrows(Failures.NotFound.class,()->jobs().current(who("remote-reporter"),B,entry.getValue()));
            }
            assertEquals("QUEUED",jobs().read(who("remote-reporter"),B,remote).state());assertEquals(0,jobs().read(who("remote-reporter"),B,remote).attempts());
            try(var c=admin();var s=c.prepareStatement("SELECT pending FROM dzoka.ai_usage WHERE tenant_id=?")) {
                s.setObject(1,A);try(var r=s.executeQuery()){assertTrue(r.next());assertEquals(0,r.getInt(1));}
                s.setObject(1,B);try(var r=s.executeQuery()){assertTrue(r.next());assertEquals(1,r.getInt(1));}
            }
            assertEquals(33,completion.size());assertFalse(Files.readString(log).contains(credential));
            var receipt=new LinkedHashMap<String,Object>();receipt.put("schemaVersion",1);receipt.put("scope","AUTHORED_CURRENT_ACCOUNT_HTTP_BURST_AND_CRASH_RECOVERY");receipt.put("profileKey",isolated.spaceKey());receipt.put("initialAdmissions",32);receipt.put("capacityRejections",1);receipt.put("replenishedAdmissions",1);receipt.put("completed",33);receipt.put("reclaimedAttempts",2);receipt.put("otherTenantUnclaimed",true);receipt.put("queueToObservedCompletionSeconds",completion.values());receipt.put("observationPollMilliseconds",500);receipt.put("controller",json.readTree(Files.readString(control.resolve("controller-result.json"))));receipt.put("quality","NOT_EVALUATED");receipt.put("productionSlo","NOT_ESTABLISHED");
            Files.writeString(control.resolve("result.json"),json.writeValueAsString(receipt)+"\n",StandardOpenOption.CREATE_NEW);
            System.out.println("Actual queue receipt: "+control.resolve("result.json"));
        } finally {
            if(child.isAlive()) {
                Files.writeString(control.resolve("abort"),"parent verification failed",StandardOpenOption.CREATE_NEW);
                // Allow bounded readiness to finish and the context manager to
                // stop/remove its exact task before any forced controller exit.
                if(!child.waitFor(900,TimeUnit.SECONDS)){child.destroyForcibly();child.waitFor(10,TimeUnit.SECONDS);}
            }
        }
    }
}
