package org.dzoka.media.adapters;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.dzoka.foundation.application.Failures;
import org.dzoka.foundation.application.ContentDigest;
import org.dzoka.media.ports.RasterDecoder;
import org.dzoka.media.sanitizer.*;

public final class DockerRasterDecoder implements RasterDecoder {
    public static final String IMAGE="eclipse-temurin@sha256:da0f0d242bec39e3fe5c1ac4c415840f03be9cef848c243d723166708e02148c";
    private final byte[] code=resource(RasterSanitizer.class);
    private final byte[] validation=resource(CanonicalPng.class);
    private static byte[] resource(Class<?> type){
        try(var input=type.getResourceAsStream(type.getSimpleName()+".class")){if(input==null)throw new IOException("Missing decoder artifact");return input.readAllBytes();}
        catch(IOException e){throw new Failures.Unavailable(e);}
    }
    @Override public String policyVersion(){return "raster-pixels-v1:code="+ContentDigest.sha256(code)+":validation="+ContentDigest.sha256(validation)+":runtime="+IMAGE.substring(IMAGE.indexOf("sha256:"));}
    @Override public Result sanitize(byte[] source){
        Path scratch=null;String container="dzoka-raster-"+UUID.randomUUID();
        try{
            Path root=Path.of(".test-tmp","media-sandbox").toAbsolutePath().normalize();Files.createDirectories(root);
            scratch=Files.createTempDirectory(root,"job-");Path input=Files.createDirectory(scratch.resolve("input")),output=Files.createDirectory(scratch.resolve("output"));
            Files.write(input.resolve("source"),source);
            String user="65534:65534";
            if(Files.getFileStore(scratch).supportsFileAttributeView("unix")){
                int uid=((Number)Files.getAttribute(scratch,"unix:uid")).intValue(),gid=((Number)Files.getAttribute(scratch,"unix:gid")).intValue();
                if(uid==0)throw new IllegalStateException("Run the media worker as an unprivileged operator");
                user=uid+":"+gid;
            }
            Path codeRoot=Files.createDirectory(scratch.resolve("code"));
            Path codePath=codeRoot.resolve("org/dzoka/media/sanitizer/RasterSanitizer.class");Files.createDirectories(codePath.getParent());Files.write(codePath,code);
            var args=new ArrayList<>(List.of("docker","run","--rm","--name",container,"--network","none","--read-only",
                "--cap-drop","ALL","--security-opt","no-new-privileges","--pids-limit","64","--memory","256m","--cpus","1",
                "--user",user,"--tmpfs","/tmp:rw,noexec,nosuid,size=16m",
                "--mount","type=bind,src="+codeRoot+",dst=/code,readonly",
                "--mount","type=bind,src="+input+",dst=/input,readonly",
                "--mount","type=bind,src="+output+",dst=/output",
                "--entrypoint","/opt/java/openjdk/bin/java",IMAGE,"-Xmx128m","-Djava.awt.headless=true",
                "-cp","/code","org.dzoka.media.sanitizer.RasterSanitizer","/input/source","/output/result.png"));
            Process child=new ProcessBuilder(args).redirectOutput(scratch.resolve("result").toFile())
                    .redirectError(scratch.resolve("error").toFile()).start();
            if(!child.waitFor(35,TimeUnit.SECONDS)){child.destroyForcibly();throw new IOException("Decoder deadline exceeded");}
            if(child.exitValue()==2)throw new RasterDecoder.Rejected();
            if(child.exitValue()!=0)throw new IOException("Decoder unavailable");
            Path result=output.resolve("result.png");
            if(Files.isSymbolicLink(result) || Files.size(result)>20*1024*1024 || Files.size(scratch.resolve("result"))>100)throw new IOException("Invalid decoder output");
            String[] facts=Files.readString(scratch.resolve("result")).split(" ");
            if(facts.length!=3)throw new IOException("Invalid decoder output");
            byte[] png=Files.readAllBytes(result);int[] dimensions=CanonicalPng.inspect(png);
            if(dimensions[0]!=Integer.parseInt(facts[0]) || dimensions[1]!=Integer.parseInt(facts[1])
                || !Set.of("image/png","image/jpeg").contains(facts[2]))throw new IOException("Invalid decoder result");
            return new Result(png,dimensions[0],dimensions[1],facts[2]);
        }catch(RasterDecoder.Rejected rejected){throw rejected;}
        catch(Exception error){if(error instanceof InterruptedException)Thread.currentThread().interrupt();throw new Failures.Unavailable(error);}
        finally{
            try{new ProcessBuilder("docker","rm","--force",container).redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD).start().waitFor(10,TimeUnit.SECONDS);}catch(Exception ignored){}
            if(scratch!=null)try{
                Path boundary=Path.of(".test-tmp","media-sandbox").toAbsolutePath().normalize();
                if(!scratch.toAbsolutePath().normalize().startsWith(boundary))throw new IOException("Unsafe scratch path");
                try(var files=Files.walk(scratch)){for(Path p:files.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}
            }catch(IOException error){throw new Failures.Unavailable(error);}
        }
    }
}
