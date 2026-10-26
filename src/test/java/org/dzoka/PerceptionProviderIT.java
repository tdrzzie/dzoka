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
import org.dzoka.media.application.MediaService;
import org.junit.jupiter.api.Test;

class PerceptionProviderIT extends S3Fixture {
    @Override protected void initializeProviders()throws Exception{
        super.initializeProviders();membership(A,"inference","ai:work");membership(A,"wrong-worker","ai:work");
        membership(A,"curator","fingerprints:manage");
        try(var c=admin()){PerceptionFixtures.profiles(c,A);}
    }
    private void runWorker(UUID profile)throws Exception{
        var command=new ProcessBuilder("python","tools/perception_worker.py","--once");var env=command.environment();
        env.put("DZOKA_AI_CORE_URL",base);env.put("DZOKA_AI_TOKEN",token("inference"));env.put("DZOKA_AI_TENANT",A.toString());env.put("DZOKA_AI_PROFILE",profile.toString());
        env.put("DZOKA_AI_PERCEPTION_DIR",Path.of(".audit-cache/perception-models").toAbsolutePath().toString());env.put("DZOKA_AI_ALLOW_INSECURE_LOOPBACK","true");
        Path log=Path.of(".test-tmp","perception-worker-"+UUID.randomUUID()+".log");command.redirectErrorStream(true).redirectOutput(log.toFile());
        var child=command.start();if(!child.waitFor(140,TimeUnit.SECONDS)){child.destroyForcibly();fail("Perception worker exceeded bounded invocation");}
        assertEquals(0,child.exitValue(),Files.readString(log));
    }
    @Test void actualLicensedProvidersConsumeOnlyPrivateSanitizedOrPcmLeasesAndAppendShadowEvidence()throws Exception{
        UUID report=report();var image=new BufferedImage(512,128,BufferedImage.TYPE_INT_ARGB);var graphics=image.createGraphics();
        graphics.setColor(Color.WHITE);graphics.fillRect(0,0,512,128);graphics.setColor(Color.BLACK);graphics.setFont(new Font("Dialog",Font.BOLD,40));graphics.drawString("BLUE BAG 742",35,78);graphics.dispose();
        var encoded=new ByteArrayOutputStream();ImageIO.write(image,"png",encoded);
        var media=app.getBean(MediaService.class).upload(who("owner"),A,report,UUID.randomUUID().toString(),encoded.toByteArray(),"image/png");
        var jobs=app.getBean(PerceptionJobs.class);
        assertThrows(org.dzoka.foundation.application.Failures.NotFound.class,()->jobs.enqueue(who("owner"),A,report,PerceptionFixtures.id("VISION"),media.id(),null));
        assertTrue(worker().processOne(who("processor"),A));
        for(String task:java.util.List.of("VISION","LOCAL_FEATURES","OCR")){
            var job=jobs.enqueue(who("owner"),A,report,PerceptionFixtures.id(task),media.id(),null);runWorker(PerceptionFixtures.id(task));assertEquals("SUCCEEDED",jobs.read(who("owner"),A,job.id()).state());
        }
        byte[] audio=Files.readAllBytes(Path.of(required("DZOKA_TEST_SPEECH_WAV")));
        var speech=jobs.enqueue(who("owner"),A,report,PerceptionFixtures.id("SPEECH"),null,audio);runWorker(PerceptionFixtures.id("SPEECH"));assertEquals("SUCCEEDED",jobs.read(who("owner"),A,speech.id()).state());
        var components=jobs.current(who("owner"),A,report);assertEquals(4,components.size());
        for(var component:components){assertEquals("SHADOW",component.selection());assertNotNull(component.profileManifest().get("artifacts"));}
        var visual=components.stream().filter(c->c.task().equals("VISION")).findFirst().orElseThrow();assertEquals(1000,((java.util.List<?>)visual.evidence().get("vector")).size());
        var local=components.stream().filter(c->c.task().equals("LOCAL_FEATURES")).findFirst().orElseThrow();assertFalse(((java.util.List<?>)local.evidence().get("points")).isEmpty());
        var ocr=components.stream().filter(c->c.task().equals("OCR")).findFirst().orElseThrow();assertTrue(ocr.evidence().get("text").toString().contains("BLUE BAG 742"));
        var transcript=components.stream().filter(c->c.task().equals("SPEECH")).findFirst().orElseThrow();assertTrue(transcript.evidence().get("text").toString().contains("BLUE TRAVEL BAG"));
        assertThrows(org.dzoka.foundation.application.Failures.NotFound.class,()->jobs.current(who("other"),A,report));
        var rollouts=app.getBean(org.dzoka.fingerprints.ports.FingerprintRollouts.class);
        var plan=new org.dzoka.fingerprints.domain.FingerprintRollout.Plan(UUID.randomUUID(),java.util.List.of(new org.dzoka.fingerprints.domain.FingerprintRollout.Source(report,1)),java.util.List.of(),10000,"e".repeat(64),
            components.stream().map(c->new org.dzoka.fingerprints.domain.FingerprintRollout.PerceptionPin(report,c.id())).toList());
        rollouts.create(who("curator"),A,plan);assertEquals(4,rollouts.seal(who("curator"),A,plan.id()).availableSlots());rollouts.activate(who("curator"),A,plan.id(),0,false);
        var selected=app.getBean(org.dzoka.fingerprints.ports.FingerprintJobs.class).snapshot(who("owner"),A,report);
        assertEquals(4,selected.perceptionComponents().size());assertEquals(plan.id(),selected.rolloutId());
        for(String modality:java.util.List.of("image","localFeatures","ocr","speech"))assertEquals(org.dzoka.fingerprints.domain.ItemFingerprint.EvidenceState.AVAILABLE,selected.coverage().get(modality));
    }
}
