package org.dzoka;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class ModuleBoundaryTest {
    @Test void domainHasNoFrameworkAndApplicationDoesNotImportAdapters() throws Exception {
        var imports = Pattern.compile("^import (?:static )?([^;]+);", Pattern.MULTILINE);
        try(var sources=Files.walk(Path.of("src/main/java/org/dzoka"))) {
            for(var path:sources.filter(p->p.toString().endsWith(".java")).toList()) {
                String normalized=path.toString().replace('\\','/');
                var matcher=imports.matcher(Files.readString(path));
                while(matcher.find()) {
                    String dependency=matcher.group(1);
                    if(normalized.contains("/domain/"))
                        assertTrue(dependency.startsWith("java."),()->path+" imports non-JDK dependency "+dependency);
                    if(normalized.contains("/application/"))
                        assertFalse(dependency.startsWith("org.dzoka.")&&dependency.contains(".adapters."),
                                ()->path+" depends on adapter "+dependency);
                    for(String owner:java.util.List.of("organizations","events","intake","media","identity","fingerprints")) {
                        if(!normalized.contains("/"+owner+"/"))continue;
                        assertFalse(dependency.startsWith("org.dzoka.")&&!dependency.startsWith("org.dzoka."+owner+".")
                                        &&dependency.contains(".adapters."),()->path+" reaches another module adapter");
                    }
                }
            }
        }
    }
}
