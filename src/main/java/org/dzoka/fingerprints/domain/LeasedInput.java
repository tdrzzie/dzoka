package org.dzoka.fingerprints.domain;
import java.time.Instant;
import java.util.UUID;
/** Private worker-only projection; never included in an event or ordinary job-status response. */
public record LeasedInput(UUID jobId,UUID lease,Instant leaseUntil,UUID reportId,int sourceRevision,String inputSha256,String text,EmbeddingSpace space) {}
