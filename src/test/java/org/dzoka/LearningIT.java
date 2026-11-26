package org.dzoka;

import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.*;
import org.dzoka.learning.*;
import org.dzoka.operations.Operations;
import org.dzoka.foundation.application.Failures;
import org.dzoka.fingerprints.domain.FingerprintRollout.*;
import org.dzoka.fingerprints.ports.FingerprintRollouts;
import org.dzoka.intake.application.IntakeService;
import org.dzoka.intake.domain.ReportDraft;
import org.dzoka.matching.ports.MatchingUseCases;
import org.junit.jupiter.api.Test;

class LearningIT extends AiFixture {
    @Override protected boolean legacyRolloutFixture(){return false;}
    private static final String SHA="d".repeat(64);
    private com.sun.net.httpserver.HttpServer tracking;
    private final java.util.concurrent.atomic.AtomicInteger creates=new java.util.concurrent.atomic.AtomicInteger();
    private final List<String> exported=new java.util.concurrent.CopyOnWriteArrayList<>();
    @Override protected List<String> extraOptions(){return List.of("--dzoka.mlflow.origin=http://127.0.0.1:"+tracking.getAddress().getPort(),"--dzoka.otlp.origin=http://127.0.0.1:"+tracking.getAddress().getPort(),"--dzoka.mlflow.experiment-id=1","--dzoka.operations.allow-insecure-loopback=true");}
    @org.junit.jupiter.api.AfterAll void stopTracking(){if(tracking!=null)tracking.stop(0);}
    @Override protected void initializeProviders() throws Exception {
        super.initializeProviders();
        membership(A,"learner","learning:manage","learning:review","feedback:submit","reports:read","reports:read:any","reports:write","matches:read","fingerprints:manage");
        membership(A,"independent","learning:review","operations:read","analytics:read");
        membership(A,"restorer","retention:recover","reports:purge","operations:read");
        membership(B,"learner","learning:manage","learning:review","operations:read");
        membership(A,"diagnostics","telemetry:export");
        membership(A,"retainer","learning:maintain");
        tracking=com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);
        tracking.createContext("/api/2.0/mlflow/",e->{
            exported.add(new String(e.getRequestBody().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));String path=e.getRequestURI().getPath();String body="{}";int status=200;
            if(path.endsWith("runs/search"))body=creates.get()==0?"{\"runs\":[]}":"{\"runs\":[{\"info\":{\"run_id\":\"retained-run\"}}]}";
            if(path.endsWith("runs/create")){creates.incrementAndGet();status=503;}
            if(path.endsWith("model-versions/search"))body="{\"model_versions\":[]}";
            if(path.endsWith("model-versions/create"))body="{\"model_version\":{\"version\":\"1\"}}";
            byte[] bytes=body.getBytes();e.getResponseHeaders().set("Content-Type","application/json");e.sendResponseHeaders(status,bytes.length);try(var out=e.getResponseBody()){out.write(bytes);}
        });tracking.start();
        tracking.createContext("/v1/",e->{exported.add(new String(e.getRequestBody().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));e.sendResponseHeaders(200,2);try(var out=e.getResponseBody()){out.write("{}".getBytes());}});
    }
    private UUID source(String text,ReportDraft.Kind kind){return app.getBean(IntakeService.class).submit(who("learner"),A,null,null,new ReportDraft(kind,text,"en","UTC",null,null,null,null,null,null,null),text.getBytes(),UUID.randomUUID().toString()).reportId();}
    private record Pair(UUID report,UUID feedback,UUID adjudication) {}
    private Pair feedback(boolean positive,String group) {
        UUID lost=source("synthetic "+group+" bag",ReportDraft.Kind.LOST),found=source("synthetic "+group+" bag",ReportDraft.Kind.FOUND),run=UUID.randomUUID(),id=UUID.randomUUID(),adjudication=UUID.randomUUID();
        var matching=app.getBean(MatchingUseCases.class);assertTrue(matching.recordSearch(who("learner"),A,lost,null,20,null,run).candidates().stream().anyMatch(c->c.report().equals(found)));
        var learning=app.getBean(Learning.class);var declaration=new Learning.Declaration(run,found,positive);
        var recorded=learning.declare(who("learner"),A,id,declaration);assertEquals(recorded,learning.declare(who("learner"),A,id,declaration));
        assertThrows(Failures.Conflict.class,()->learning.declare(who("learner"),A,UUID.randomUUID(),declaration));
        assertThrows(Failures.Conflict.class,()->learning.declare(who("learner"),A,id,new Learning.Declaration(run,found,!positive)));
        var label=new Learning.Adjudication(id,1,positive?"POSITIVE":"NEGATIVE",Learning.hash(group),SHA);
        assertThrows(Failures.Forbidden.class,()->learning.adjudicate(who("learner"),A,adjudication,label));
        learning.adjudicate(who("independent"),A,adjudication,label);learning.adjudicate(who("independent"),A,adjudication,label);
        return new Pair(lost,id,adjudication);
    }
    @Test void governedLearningAndRestoreAreEnforcedByRealTransactionsAndHttp() throws Exception {
        var learning=app.getBean(Learning.class);var operations=app.getBean(Operations.class);var rollouts=app.getBean(FingerprintRollouts.class);
        UUID modelReport=report("Synthetic rollout fixture").reportId();jobs().enqueue(who("reporter"),A,modelReport,primary);var lease=jobs().claim(who("inference"),A,primary).orElseThrow();assertTrue(jobs().complete(who("inference"),A,lease.jobId(),result(lease)));
        assertEquals(1,app.getBean(org.dzoka.operations.JobDiagnostics.class).export(who("diagnostics"),A));assertEquals(0,app.getBean(org.dzoka.operations.JobDiagnostics.class).export(who("diagnostics"),A));
        assertThrows(Failures.Forbidden.class,()->app.getBean(org.dzoka.operations.JobDiagnostics.class).export(who("learner"),A));
        var plan=new Plan(UUID.randomUUID(),List.of(new Source(modelReport,1)),List.of(primary),10000,SHA);rollouts.create(who("learner"),A,plan);rollouts.seal(who("learner"),A,plan.id());
        assertThrows(Failures.Conflict.class,()->rollouts.activate(who("learner"),A,plan.id(),0,false));
        Pair train=feedback(true,"training"),positive=feedback(true,"heldout-positive"),negative=feedback(false,"heldout-negative");
        UUID dataset=UUID.randomUUID();var cases=List.of(new Learning.Case(train.adjudication(),"TRAIN"),new Learning.Case(positive.adjudication(),"TEST"),new Learning.Case(negative.adjudication(),"TEST"));
        assertThrows(Failures.Conflict.class,()->learning.dataset(who("learner"),A,dataset,new Learning.Dataset(cases,SHA)));
        learning.policy(who("independent"),A,new Learning.Policy(1,true,true,30,SHA));
        assertEquals(400,api("PUT",A,"/learning/policy","independent","{\"version\":2,\"retentionDays\":30,\"purposeSha\":\""+SHA+"\"}").statusCode());
        assertEquals(400,api("PUT",A,"/learning/feedback/"+UUID.randomUUID(),"learner","{\"run\":\""+UUID.randomUUID()+"\",\"candidate\":\""+UUID.randomUUID()+"\"}").statusCode());
        var view=learning.dataset(who("learner"),A,dataset,new Learning.Dataset(cases,SHA));assertEquals(3,view.cases());
        assertEquals(view,learning.dataset(who("learner"),A,dataset,new Learning.Dataset(cases,SHA)));
        assertThrows(Failures.Conflict.class,()->learning.dataset(who("learner"),B,dataset,new Learning.Dataset(cases,SHA)));
        assertThrows(Failures.Conflict.class,()->learning.dataset(who("learner"),A,UUID.randomUUID(),new Learning.Dataset(List.of(new Learning.Case(train.adjudication(),"TEST"),new Learning.Case(positive.adjudication(),"TRAIN")),SHA)));
        assertThrows(Failures.Conflict.class,()->rollouts.activate(who("learner"),A,plan.id(),0,false));
        UUID experiment=UUID.randomUUID();var run=new Learning.Experiment(dataset,plan.id(),profile.provider(),profile.artifactRevision(),profile.artifactSha256(),profile.configSha256(),SHA,5,List.of(new Evaluation.Result(positive.feedback(),2,1,0.95),new Evaluation.Result(negative.feedback(),0,0,0.05)));
        assertEquals(1.0,learning.experiment(who("learner"),A,experiment,run).metrics().recallAtK());
        var mlflow=app.getBean(Mlflow.class);
        assertThrows(Failures.Unavailable.class,()->mlflow.export(who("learner"),A)); // Remote create persisted, reply lost.
        assertTrue(mlflow.export(who("learner"),A));assertFalse(mlflow.export(who("learner"),A));assertEquals(1,creates.get());
        assertFalse(String.join("",exported).contains("heldout-positive"));assertFalse(String.join("",exported).contains(positive.feedback().toString()));
        var approval=new Learning.Approval(SHA,1,1,0.01);
        assertThrows(Failures.Forbidden.class,()->learning.approve(who("learner"),A,experiment,approval));
        assertThrows(Failures.Conflict.class,()->learning.approve(who("independent"),A,experiment,new Learning.Approval(SHA,1,1,0.001)));
        learning.approve(who("independent"),A,experiment,approval);assertEquals(1,rollouts.activate(who("learner"),A,plan.id(),0,false).version());assertTrue(mlflow.syncActivation(who("learner"),A));
        assertEquals(403,api("GET",A,"/learning/feedback","viewer",null).statusCode());
        var response=api("GET",A,"/learning/feedback","independent",null);assertEquals(200,response.statusCode());assertFalse(response.body().contains("heldout-positive bag"));
        assertEquals(200,api("GET",A,"/operations/status","independent",null).statusCode());
        var analytics=operations.analytics(who("independent"),A,Instant.now().minusSeconds(3600));assertTrue(analytics.get("verifiedRecoveryRate").isNull());assertTrue(analytics.get("ownershipFalsePositiveRate").isNull());
        learning.revoke(who("independent"),A,"EXPERIMENT",experiment,SHA);learning.revoke(who("independent"),A,"EXPERIMENT",experiment,SHA);
        assertFalse(mlflow.syncActivation(who("learner"),A));assertTrue(String.join("",exported).contains("SUSPENDED"));
        assertThrows(Failures.Conflict.class,()->app.getBean(MatchingUseCases.class).search(who("learner"),A,positive.report(),null,5,null));
        assertFalse(operations.status(who("independent"),A).get("modelEvidenceAvailable").asBoolean());
        if(System.getenv("DZOKA_TEST_MLFLOW_ORIGIN")!=null){
            var environment=new org.springframework.core.env.StandardEnvironment();environment.getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("reviewed-real-mlflow",Map.of("dzoka.mlflow.origin",System.getenv("DZOKA_TEST_MLFLOW_ORIGIN"),"dzoka.mlflow.experiment-id",System.getenv("DZOKA_TEST_MLFLOW_EXPERIMENT"),"dzoka.operations.allow-insecure-loopback",true)));
            var real=new Mlflow(app.getBean(org.dzoka.foundation.persistence.TenantTransactions.class),new tools.jackson.databind.ObjectMapper(),environment);UUID realExperiment=UUID.randomUUID();
            learning.experiment(who("learner"),A,realExperiment,run);assertTrue(real.export(who("learner"),A));assertFalse(real.export(who("learner"),A));
            learning.approve(who("independent"),A,realExperiment,approval);assertTrue(real.syncActivation(who("learner"),A));
            learning.revoke(who("independent"),A,"EXPERIMENT",realExperiment,SHA);assertFalse(real.syncActivation(who("learner"),A));
        }
        operations.close(who("restorer"),A,SHA);
        assertThrows(Failures.Conflict.class,()->source("Blocked during restore",ReportDraft.Kind.LOST));
        var emptyReplay=new Operations.Replay(SHA,List.of(new Operations.Erasure(UUID.randomUUID(),2)));String manifest=operations.replay(who("restorer"),A,emptyReplay);
        assertEquals(manifest,operations.replay(who("restorer"),A,emptyReplay));
        assertThrows(Failures.Conflict.class,()->operations.open(who("restorer"),A,new Operations.Open("f".repeat(64),SHA,SHA)));
        operations.open(who("restorer"),A,new Operations.Open(manifest,SHA,SHA));assertNotNull(source("Reopened",ReportDraft.Kind.LOST));
        app.getBean(IntakeService.class).submit(who("learner"),A,positive.report(),1,new ReportDraft(ReportDraft.Kind.LOST,"Changed","en","UTC",null,null,null,null,null,null,null),"changed".getBytes(),UUID.randomUUID().toString());
        assertThrows(Failures.Conflict.class,()->learning.experiment(who("independent"),A,experiment));
        assertEquals(0,learning.purge(who("retainer"),A));
        var intake=app.getBean(IntakeService.class);intake.hold(who("reviewer"),A,positive.report(),true);
        try(var db=admin();var statement=db.prepareStatement("UPDATE dzoka.learning_datasets SET expires_at=now()-interval '1 second' WHERE tenant_id=? AND id=?")){statement.setObject(1,A);statement.setObject(2,dataset);statement.executeUpdate();}
        assertEquals(0,learning.purge(who("retainer"),A));intake.hold(who("reviewer"),A,positive.report(),false);assertEquals(3,learning.purge(who("retainer"),A));assertEquals(0,learning.purge(who("retainer"),A));
        learning.policy(who("independent"),A,new Learning.Policy(2,false,false,30,SHA));
        assertFalse(operations.status(who("independent"),A).get("modelEvidenceAvailable").asBoolean());assertFalse(mlflow.syncActivation(who("learner"),A));
        assertThrows(Failures.Conflict.class,()->app.getBean(MatchingUseCases.class).search(who("learner"),A,modelReport,null,5,null));
    }
}
