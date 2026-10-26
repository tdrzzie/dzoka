package org.dzoka.fingerprints.domain;
import java.time.Instant;
import java.util.*;

public final class Perception {
    private Perception(){}
    public record Job(UUID id,UUID reportId,int sourceRevision,UUID profileId,String task,String state,int attempts,String errorCode,Instant deadline){}
    public record Lease(UUID jobId,UUID lease,UUID profileId,String task,String profileKey,String inputType,String inputSha256,int inputBytes,Instant leaseUntil){}
    public record Result(UUID lease,String inputSha256,String profileKey,Instant generatedAt,Map<String,Object> evidence){
        public Result{Objects.requireNonNull(lease);Objects.requireNonNull(generatedAt);evidence=Map.copyOf(evidence);
            if(inputSha256==null||profileKey==null||!inputSha256.matches("[a-f0-9]{64}")||!profileKey.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Invalid provenance");}
    }
    public record Component(UUID id,UUID reportId,int sourceRevision,String inputSha256,UUID profileId,String profileKey,String task,
            Instant generatedAt,Instant receivedAt,Map<String,Object> profileManifest,Map<String,Object> evidence,String selection){}
}
