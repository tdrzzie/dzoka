package org.dzoka;

import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.dzoka.fingerprints.domain.*;
import org.dzoka.fingerprints.ports.FingerprintJobs;
import org.dzoka.foundation.application.*;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.intake.application.IntakeService;
import org.dzoka.intake.domain.*;
import org.junit.jupiter.api.*;
import tools.jackson.databind.ObjectMapper;

class AiProviderIT extends AiFixture {
    @Test void realPythonWorkerClaimsInfersAndPublishesWithOnlyServiceToken() throws Exception {
        UUID report=report("Synthetic blue travel bag").reportId();var job=jobs().enqueue(who("reporter"),A,report,primary);
        var command=new ProcessBuilder("python","tools/ai_worker.py","--once");var env=command.environment();
        env.put("DZOKA_AI_CORE_URL",base);env.put("DZOKA_AI_TOKEN",token("inference"));env.put("DZOKA_AI_TENANT",A.toString());env.put("DZOKA_AI_SPACE",primary.toString());
        env.put("DZOKA_AI_MODEL_DIR",Path.of(".audit-cache/ai-model").toAbsolutePath().toString());env.put("DZOKA_AI_ALLOW_INSECURE_LOOPBACK","true");
        Path log=Path.of(".test-tmp","ai-worker-"+UUID.randomUUID()+".log");command.redirectErrorStream(true).redirectOutput(log.toFile());
        var child=command.start();if(!child.waitFor(90,TimeUnit.SECONDS)){child.destroyForcibly();fail("Worker deadline exceeded");}
        assertEquals(0,child.exitValue(),Files.readString(log));assertEquals("SUCCEEDED",jobs().read(who("reporter"),A,job.id()).state());
        var fingerprint=jobs().current(who("reporter"),A,report).getFirst();assertEquals(384,fingerprint.vector().length);assertEquals(profile.spaceKey(),fingerprint.space().spaceKey());
        assertEquals(1,Arrays.stream(fingerprint.vector()).map(v->v*v).sum(),0.0001);assertTrue(Arrays.stream(fingerprint.vector()).filter(v->Math.abs(v)>0.001).count()>100);
    }
}
