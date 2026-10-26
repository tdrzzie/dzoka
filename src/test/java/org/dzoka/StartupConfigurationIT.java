package org.dzoka;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class StartupConfigurationIT {
    @Test void packagedApplicationCannotStartWithoutRequiredConfiguration() throws Exception {
        Path java=Path.of(System.getProperty("java.home"),"bin",
                System.getProperty("os.name").startsWith("Windows")?"java.exe":"java");
        Path log=Path.of(".test-tmp","startup-"+UUID.randomUUID()+".log");
        Files.createDirectories(log.getParent());
        var builder=new ProcessBuilder(java.toString(),"-jar","target/dzoka-core-0.1.0-SNAPSHOT.jar","--server.port=0")
                .redirectErrorStream(true).redirectOutput(log.toFile());
        builder.environment().keySet().removeIf(k->k.startsWith("DZOKA_")||k.startsWith("SPRING_")||k.startsWith("SERVER_"));
        Process child=builder.start();
        try {
            assertTrue(child.waitFor(30,TimeUnit.SECONDS),"Unconfigured application did not stop");
            assertNotEquals(0,child.exitValue());
            assertTrue(Files.readString(log).contains("Application run failed"));
        } finally {if(child.isAlive()){child.destroyForcibly();child.waitFor(5,TimeUnit.SECONDS);}}
    }
}
