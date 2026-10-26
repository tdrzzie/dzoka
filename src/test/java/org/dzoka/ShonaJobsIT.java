package org.dzoka;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.dzoka.fingerprints.domain.Perception.*;
import org.dzoka.fingerprints.domain.FingerprintRollout.*;
import org.dzoka.fingerprints.ports.*;
import org.dzoka.foundation.application.Failures;
import org.dzoka.intake.application.IntakeService;
import org.dzoka.intake.domain.ReportDraft;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
class ShonaJobsIT extends AiFixture {
    @SuppressWarnings("unchecked") static void profiles(Connection c,UUID tenant)throws Exception{
        var codec=new ObjectMapper();Map<String,Map<String,Object>> root=codec.readValue(Files.readAllBytes(Path.of("ai/shona-profiles.json")),Map.class);
        for(var entry:root.entrySet())try(var s=c.prepareStatement("INSERT INTO dzoka.perception_profiles(tenant_id,id,task,profile_key,manifest,enabled) VALUES(?,?,?,?,CAST(? AS jsonb),true)")){
            var value=entry.getValue();var manifest=(Map<String,Object>)value.get("manifest");s.setObject(1,tenant);s.setObject(2,PerceptionFixtures.id(entry.getKey()));s.setString(3,(String)manifest.get("task"));s.setString(4,(String)value.get("key"));s.setString(5,codec.writeValueAsString(manifest));s.executeUpdate();
        }
    }
    @Override protected void initializeProviders()throws Exception{super.initializeProviders();try(var c=admin()){profiles(c,A);profiles(c,B);}}
    UUID source(String text,String language){return app.getBean(IntakeService.class).submit(who("reporter"),A,null,null,new ReportDraft(ReportDraft.Kind.LOST,text,language,"UTC",null,null,null,null,null,null,null),text.getBytes(StandardCharsets.UTF_8),UUID.randomUUID().toString()).reportId();}
    @Test void englishToShonaUsesPrivateCurrentSourceAndExplicitDirection()throws Exception{
        var jobs=app.getBean(PerceptionJobs.class);String raw="I lost a black bag.";UUID report=source(raw,"en-ZW");var job=jobs.enqueue(who("reporter"),A,report,PerceptionFixtures.id("CT2_EN_SN"),null,null);assertEquals(job.id(),jobs.enqueue(who("reporter"),A,report,PerceptionFixtures.id("CT2_EN_SN"),null,null).id());var lease=jobs.claim(who("inference"),A,PerceptionFixtures.id("CT2_EN_SN")).orElseThrow();assertArrayEquals(raw.getBytes(StandardCharsets.UTF_8),jobs.input(who("inference"),A,job.id(),lease.lease()));assertThrows(Failures.NotFound.class,()->jobs.input(who("wrong-worker"),A,job.id(),lease.lease()));assertThrows(Failures.NotFound.class,()->jobs.input(who("remote"),B,job.id(),lease.lease()));
        var e=GenerativeEvidenceTest.translation("en","sn");e.put("text","Ndarasa bhegi dema.");e.put("representation","ctranslate2-marian-v1");var result=new Result(lease.lease(),lease.inputSha256(),lease.profileKey(),Instant.now(),e);assertTrue(jobs.complete(who("inference"),A,job.id(),result));assertTrue(jobs.complete(who("inference"),A,job.id(),result));var component=jobs.result(who("reporter"),A,job.id()).orElseThrow();assertEquals("UNVERIFIED_TRANSLATION",component.evidence().get("trust"));assertThrows(Failures.NotFound.class,()->jobs.result(who("other"),A,job.id()));
        var plan=new Plan(UUID.randomUUID(),List.of(new Source(report,1)),List.of(),10000,"e".repeat(64),List.of(new PerceptionPin(report,component.id())));var rollouts=app.getBean(FingerprintRollouts.class);rollouts.create(who("curator"),A,plan);assertEquals(1,rollouts.seal(who("curator"),A,plan.id()).availableSlots());rollouts.activate(who("curator"),A,plan.id(),0,false);assertEquals("sn",app.getBean(FingerprintJobs.class).snapshot(who("reporter"),A,report).perceptionComponents().getFirst().evidence().get("targetLanguage"));
        assertThrows(IllegalArgumentException.class,()->jobs.enqueue(who("reporter"),A,report,PerceptionFixtures.id("CT2_SN_EN"),null,null));
    }
    @Test void shonaCodeSwitchRawSourceAndPrivateReplayRemainExact()throws Exception{
        var jobs=app.getBean(PerceptionJobs.class);String raw="Ndasiya black Lenovo bag muJoina around 2pm.";UUID report=source(raw,"sn");var job=jobs.enqueue(who("reporter"),A,report,PerceptionFixtures.id("CT2_SN_EN"),null,null);var lease=jobs.claim(who("inference"),A,PerceptionFixtures.id("CT2_SN_EN")).orElseThrow();assertArrayEquals(raw.getBytes(StandardCharsets.UTF_8),jobs.input(who("inference"),A,job.id(),lease.lease()));var e=GenerativeEvidenceTest.translation("sn","en");e.put("representation","marian-source-target-v1");assertThrows(IllegalArgumentException.class,()->jobs.complete(who("inference"),A,job.id(),new Result(lease.lease(),lease.inputSha256(),lease.profileKey(),Instant.now(),e)));e.put("representation","ctranslate2-marian-v1");assertTrue(jobs.complete(who("inference"),A,job.id(),new Result(lease.lease(),lease.inputSha256(),lease.profileKey(),Instant.now(),e)));assertThrows(IllegalArgumentException.class,()->jobs.enqueue(who("reporter"),A,source("Bhegi","sn"),PerceptionFixtures.id("CT2_EN_SN"),null,null));
    }
    @Test void markedCopiesMustMatchLeasedSourceAndCannotGainTrust()throws Exception{
        var jobs=app.getBean(PerceptionJobs.class);String raw="Ndasiya [[keep:bag]]";UUID report=source(raw,"sn");var job=jobs.enqueue(who("reporter"),A,report,PerceptionFixtures.id("CT2_SN_EN_KEEP"),null,null);var lease=jobs.claim(who("inference"),A,PerceptionFixtures.id("CT2_SN_EN_KEEP")).orElseThrow();assertArrayEquals(raw.getBytes(StandardCharsets.UTF_8),jobs.input(who("inference"),A,job.id(),lease.lease()));
        var e=ShonaEvidenceTest.evidence(raw,"bag","I left bag");var copies=new HashMap<String,Object>((Map<String,Object>)((List<?>)e.get("preservedSpans")).getFirst());copies.put("quote","other report");e.put("preservedSpans",List.of(copies));assertThrows(IllegalArgumentException.class,()->jobs.complete(who("inference"),A,job.id(),new Result(lease.lease(),lease.inputSha256(),lease.profileKey(),Instant.now(),e)));
        var good=ShonaEvidenceTest.evidence(raw,"bag","I left bag");assertTrue(jobs.complete(who("inference"),A,job.id(),new Result(lease.lease(),lease.inputSha256(),lease.profileKey(),Instant.now(),good)));assertThrows(IllegalArgumentException.class,()->jobs.enqueue(who("reporter"),A,source("unmarked","sn"),PerceptionFixtures.id("CT2_SN_EN_KEEP"),null,null));assertThrows(IllegalArgumentException.class,()->jobs.enqueue(who("reporter"),A,source("[[keep:outer [[keep:inner]]]]","sn"),PerceptionFixtures.id("CT2_SN_EN_KEEP"),null,null));
    }

}
