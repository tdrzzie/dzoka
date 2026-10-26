package org.dzoka.fingerprints.domain;
import java.time.Instant;
import java.util.UUID;
public record FingerprintComponent(UUID id,UUID itemId,UUID reportId,int sourceRevision,String inputSha256,EmbeddingSpace space,
        String outputSchema,Instant generatedAt,Instant receivedAt,double[] vector,int tokenCount,boolean truncated) {
    public FingerprintComponent {vector=vector.clone();}
    @Override public double[] vector(){return vector.clone();}
}
