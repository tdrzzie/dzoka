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
        try(var c=admin()){
            PerceptionFixtures.profiles(c,A);
            var profiles=new tools.jackson.databind.ObjectMapper().readTree(Files.readString(Path.of("ai/refinement-profiles.json")));
            for(String name:java.util.List.of("MASK_DETECTION","MASK_SEGMENTATION"))try(var s=c.prepareStatement("INSERT INTO dzoka.perception_profiles(tenant_id,id,task,profile_key,manifest,enabled) VALUES(?,?,?,?,CAST(? AS jsonb),true)")){
                var value=profiles.get(name);s.setObject(1,A);s.setObject(2,PerceptionFixtures.id(name));s.setString(3,value.get("manifest").get("task").asString());s.setString(4,value.get("key").asString());s.setString(5,value.get("manifest").toString());s.executeUpdate();
            }
        }
    }
    private void runWorker(UUID profile)throws Exception{
        boolean refined=java.util.List.of("MASK_DETECTION","MASK_SEGMENTATION").stream().anyMatch(n->PerceptionFixtures.id(n).equals(profile));
        var command=refined?new ProcessBuilder(Path.of(".audit-cache/generative-venv/Scripts/python.exe").toAbsolutePath().toString(),"-I","ai/refinement_worker.py","--once"):new ProcessBuilder("python","tools/perception_worker.py","--once");var env=command.environment();
        if(refined)env.keySet().removeIf(k->!java.util.Set.of("PATH","SYSTEMROOT","WINDIR","TEMP","TMP","HOME","LANG","LC_ALL","SSL_CERT_FILE","SSL_CERT_DIR").contains(k.toUpperCase(java.util.Locale.ROOT)));
        env.put("DZOKA_AI_CORE_URL",base);env.put("DZOKA_AI_TOKEN",token("inference"));env.put("DZOKA_AI_TENANT",A.toString());env.put("DZOKA_AI_PROFILE",profile.toString());
        env.put("DZOKA_AI_PERCEPTION_DIR",Path.of(".audit-cache/perception-models").toAbsolutePath().toString());env.put("DZOKA_AI_ALLOW_INSECURE_LOOPBACK","true");
        if(refined)env.put("DZOKA_AI_REFINEMENT_DIR",Path.of(".audit-cache/refinement-models").toAbsolutePath().toString());
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
        for(String name:java.util.List.of("MASK_DETECTION","MASK_SEGMENTATION")){
            var job=jobs.enqueue(who("owner"),A,report,PerceptionFixtures.id(name),media.id(),null);runWorker(PerceptionFixtures.id(name));
            assertEquals("SUCCEEDED",jobs.read(who("owner"),A,job.id()).state());var refined=jobs.result(who("owner"),A,job.id()).orElseThrow();
            assertEquals(name.equals("MASK_DETECTION")?"maskrcnn-coco-boxes-v2":"maskrcnn-voc-union-rle-v2",refined.evidence().get("representation"));assertEquals("SHADOW",refined.selection());
            assertThrows(org.dzoka.foundation.application.Failures.NotFound.class,()->jobs.result(who("other"),A,job.id()));
        }
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
