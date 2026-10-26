package org.dzoka.fingerprints.domain;
import java.time.Instant;
import java.util.*;

public record EmbeddingResult(UUID lease,String inputSha256,String spaceKey,String provider,String outputSchema,
        Instant generatedAt,double[] vector,int tokenCount,boolean truncated) {
    public EmbeddingResult {
        Objects.requireNonNull(lease);Objects.requireNonNull(generatedAt);
        if(inputSha256==null || !inputSha256.matches("[a-f0-9]{64}") || spaceKey==null || !spaceKey.matches("[a-f0-9]{64}")
                || provider==null || !provider.equals("onnxruntime-cpu") || !"text-embedding-v1".equals(outputSchema)
                || tokenCount<1 || tokenCount>128 || vector==null || vector.length<1 || vector.length>1024)throw new IllegalArgumentException("Invalid result");
        vector=vector.clone();double sum=0;
        for(double v:vector){if(!Double.isFinite(v))throw new IllegalArgumentException("Nonfinite vector");sum+=v*v;}
        if(Math.abs(sum-1)>0.0001)throw new IllegalArgumentException("Vector is not L2 normalized");
    }
    @Override public double[] vector(){return vector.clone();}
    public String canonical(){return lease+"\n"+inputSha256+"\n"+spaceKey+"\n"+provider+"\n"+outputSchema+"\n"+generatedAt+"\n"+Arrays.toString(vector)+"\n"+tokenCount+"\n"+truncated;}
}
