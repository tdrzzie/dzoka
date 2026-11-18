package org.dzoka;
import static org.junit.jupiter.api.Assertions.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import org.dzoka.fingerprints.ports.PerceptionJobs;
import org.dzoka.fingerprints.domain.FingerprintRollout.*;
import org.dzoka.intake.application.IntakeService;
import org.dzoka.intake.domain.ReportDraft;
import org.dzoka.media.application.MediaService;
import org.junit.jupiter.api.Test;

class VisionProviderIT extends S3Fixture {
    @Override protected void initializeProviders()throws Exception{
        super.initializeProviders();membership(A,"inference","ai:work");membership(A,"wrong-worker","ai:work");
        membership(A,"curator","fingerprints:manage");
        membership(A,"matcher","matches:read","reports:read","reports:read:any","matches:attest-identifiers");
        try(var c=admin()){VisionJobsIT.profiles(c,A);}
    }
    private void runWorker(UUID profile)throws Exception{
        var command=new ProcessBuilder("python","tools/vision_worker.py","--once");var env=command.environment();
        env.put("DZOKA_AI_CORE_URL",base);env.put("DZOKA_AI_TOKEN",token("inference"));env.put("DZOKA_AI_TENANT",A.toString());env.put("DZOKA_AI_PROFILE",profile.toString());
        env.put("DZOKA_AI_VISION_DIR",Path.of(".audit-cache/vision-models").toAbsolutePath().toString());env.put("DZOKA_AI_ALLOW_INSECURE_LOOPBACK","true");
        Path log=Path.of(".test-tmp","vision-worker-"+UUID.randomUUID()+".log");command.redirectErrorStream(true).redirectOutput(log.toFile());
        var child=command.start();if(!child.waitFor(140,TimeUnit.SECONDS)){child.destroyForcibly();fail("Perception worker exceeded bounded invocation");}
        assertEquals(0,child.exitValue(),Files.readString(log));
    }
    @Test void actualJointAndRegionModelsConsumePrivateLeasesAndActivateExplicitPins()throws Exception{
        UUID report=report();var image=new BufferedImage(512,128,BufferedImage.TYPE_INT_ARGB);var graphics=image.createGraphics();
        graphics.setColor(Color.WHITE);graphics.fillRect(0,0,512,128);graphics.setColor(Color.BLACK);graphics.setFont(new Font("Dialog",Font.BOLD,40));graphics.drawString("BLUE BAG 742",35,78);graphics.dispose();
        var encoded=new ByteArrayOutputStream();ImageIO.write(image,"png",encoded);
        var media=app.getBean(MediaService.class).upload(who("owner"),A,report,UUID.randomUUID().toString(),encoded.toByteArray(),"image/png");
        var jobs=app.getBean(PerceptionJobs.class);
        assertThrows(org.dzoka.foundation.application.Failures.NotFound.class,()->jobs.enqueue(who("owner"),A,report,PerceptionFixtures.id("JOINT_IMAGE"),media.id(),null));
        assertTrue(worker().processOne(who("processor"),A));
        for(String task:java.util.List.of("JOINT_IMAGE","DETECTION","SEGMENTATION")){
            var job=jobs.enqueue(who("owner"),A,report,PerceptionFixtures.id(task),media.id(),null);runWorker(PerceptionFixtures.id(task));assertEquals("SUCCEEDED",jobs.read(who("owner"),A,job.id()).state());
        }
        var textJob=jobs.enqueue(who("owner"),A,report,PerceptionFixtures.id("JOINT_TEXT"),null,null);runWorker(PerceptionFixtures.id("JOINT_TEXT"));assertEquals("SUCCEEDED",jobs.read(who("owner"),A,textJob.id()).state());
        var components=jobs.current(who("owner"),A,report);assertEquals(4,components.size());
        for(var component:components){assertEquals("SHADOW",component.selection());assertNotNull(component.profileManifest().get("artifacts"));}
        var imageVector=components.stream().filter(c->c.task().equals("JOINT_IMAGE")).findFirst().orElseThrow();
        var textVector=components.stream().filter(c->c.task().equals("JOINT_TEXT")).findFirst().orElseThrow();
        assertEquals(768,((java.util.List<?>)imageVector.evidence().get("vector")).size());assertEquals(768,((java.util.List<?>)textVector.evidence().get("vector")).size());
        assertEquals(imageVector.evidence().get("spaceKey"),textVector.evidence().get("spaceKey"));assertNotEquals(imageVector.evidence().get("vector"),textVector.evidence().get("vector"));
        var segment=components.stream().filter(c->c.task().equals("SEGMENTATION")).findFirst().orElseThrow();assertEquals(64,((Number)segment.evidence().get("maskWidth")).intValue());
        assertThrows(org.dzoka.foundation.application.Failures.NotFound.class,()->jobs.current(who("other"),A,report));
        UUID lost=app.getBean(IntakeService.class).submit(who("owner"),A,null,null,
                new ReportDraft(ReportDraft.Kind.LOST,"blue travel backpack","en","UTC",null,null,null,null,null,null,null),
                "{}".getBytes(java.nio.charset.StandardCharsets.UTF_8),UUID.randomUUID().toString()).reportId();
        var lostJob=jobs.enqueue(who("owner"),A,lost,PerceptionFixtures.id("JOINT_TEXT"),null,null);
        runWorker(PerceptionFixtures.id("JOINT_TEXT"));
        var lostText=jobs.result(who("owner"),A,lostJob.id()).orElseThrow();
        String matching="/internal/matching/reports/"+lost;
        var json=app.getBean(tools.jackson.databind.ObjectMapper.class);
        assertTrue(json.readTree(api("GET",A,matching+"/candidates","matcher",null).body()).get("candidates").isEmpty());
        var rollouts=app.getBean(org.dzoka.fingerprints.ports.FingerprintRollouts.class);
        var pins=new ArrayList<>(components.stream().map(c->new PerceptionPin(report,c.id())).toList());
        pins.add(new PerceptionPin(lost,lostText.id()));
        var plan=new Plan(UUID.randomUUID(),java.util.List.of(new Source(report,1),new Source(lost,1)),
                java.util.List.of(),10000,"e".repeat(64),pins);
        rollouts.create(who("curator"),A,plan);assertEquals(5,rollouts.seal(who("curator"),A,plan.id()).availableSlots());rollouts.activate(who("curator"),A,plan.id(),0,false);
        var selected=app.getBean(org.dzoka.fingerprints.ports.FingerprintJobs.class).snapshot(who("owner"),A,report);
        assertEquals(4,selected.perceptionComponents().size());assertEquals(plan.id(),selected.rolloutId());
        for(String modality:java.util.List.of("jointImage","jointText","detection","segmentation"))assertEquals(org.dzoka.fingerprints.domain.ItemFingerprint.EvidenceState.AVAILABLE,selected.coverage().get(modality));
        for(UUID id:java.util.List.of(lost,report)) {
            String attestation=json.writeValueAsString(Map.of("revision",1,"keyVersion",1,"scopeDigest","a".repeat(64),
                    "digest",(id.equals(lost)?"b":"c").repeat(64),"attestationId",UUID.randomUUID()));
            assertEquals(204,api("POST",A,"/internal/matching/reports/"+id+"/identifier-attestations","matcher",attestation).statusCode());
        }
        String run=matching+"/runs/"+UUID.randomUUID();
        var matched=api("PUT",A,run,"matcher","{\"limit\":5}");
        assertEquals(200,matched.statusCode(),matched.body());
        var candidate=json.readTree(matched.body()).get("candidates").get(0);
        assertEquals("CONFLICT",candidate.get("score").get("disposition").asString());
        assertEquals(1,candidate.get("ranks").get("JOINT_VECTOR").asInt());
        assertEquals(lostText.id().toString(),candidate.get("jointEvidence").get("sourceComponent").asString());
        assertEquals(imageVector.id().toString(),candidate.get("jointEvidence").get("candidateComponent").asString());
        assertFalse(matched.body().contains("\"vector\":"));
        assertEquals(200,api("GET",A,run,"matcher",null).statusCode());
        try(var db=admin();var expire=db.prepareStatement("UPDATE dzoka.media_assets SET expires_at=now()-interval '1 second' WHERE tenant_id=? AND id=?")) {
            expire.setObject(1,A);expire.setObject(2,media.id());assertEquals(1,expire.executeUpdate());
        }
        assertEquals(404,api("GET",A,run,"matcher",null).statusCode());
        assertTrue(json.readTree(api("GET",A,matching+"/candidates","matcher",null).body()).get("candidates").isEmpty());
    }
}
