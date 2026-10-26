package org.dzoka.media.domain;
import java.time.Instant;
import java.util.UUID;
public record MediaView(UUID id,UUID reportId,String state,long sourceBytes,Integer width,Integer height,
        Instant expiresAt,String rejectionCode) {}
