package org.dzoka.fingerprints.domain;
import java.time.Instant;
import java.util.UUID;
public record InferenceJob(UUID id,UUID reportId,int sourceRevision,UUID spaceId,String state,int attempts,String errorCode,Instant deadline) {}
