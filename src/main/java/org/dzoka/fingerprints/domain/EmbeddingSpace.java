package org.dzoka.fingerprints.domain;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Dimensions alone never establish compatibility. Every preprocessing/runtime profile is a distinct space. */
public record EmbeddingSpace(UUID id,String provider,String model,String artifactRevision,String artifactSha256,
        String tokenizerSha256,String preprocessing,int dimensions,String metric,String normalization,String configSha256,String spaceKey) {
    public EmbeddingSpace {
        Objects.requireNonNull(id);
        for(String s:List.of(provider,model,artifactRevision,preprocessing,metric,normalization))
            if(s.isBlank() || s.length()>200 || s.contains("\n"))throw new IllegalArgumentException("Invalid embedding space");
        for(String s:List.of(artifactSha256,tokenizerSha256,configSha256,spaceKey))
            if(!s.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Invalid artifact digest");
        if(dimensions<1 || dimensions>1024 || !metric.equals("COSINE") || !normalization.equals("L2"))throw new IllegalArgumentException("Unsupported space");
        if(!spaceKey.equals(key(provider,model,artifactRevision,artifactSha256,tokenizerSha256,preprocessing,dimensions,metric,normalization,configSha256)))
            throw new IllegalArgumentException("Space identity mismatch");
    }
    public static String key(String provider,String model,String revision,String artifact,String tokenizer,String preprocessing,
            int dimensions,String metric,String normalization,String config){
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(String.join("\n",provider,model,revision,artifact,tokenizer,preprocessing,
            Integer.toString(dimensions),metric,normalization,config).getBytes(StandardCharsets.UTF_8)));}
        catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
    public boolean compatibleWith(EmbeddingSpace other){return spaceKey.equals(other.spaceKey);}
}
