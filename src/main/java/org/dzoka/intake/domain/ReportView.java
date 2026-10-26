package org.dzoka.intake.domain;
import java.time.Instant;
import java.util.UUID;
public record ReportView(UUID caseId, UUID itemId, UUID reportId, int revision, ReportDraft.Kind kind,
        String rawText, String normalizedText, String normalizationVersion, String language, String timezone,
        String rawLocation, String rawTime, Double latitude, Double longitude, Double uncertaintyMeters,
        Instant occurredAfter, Instant occurredBefore, Instant expiresAt, boolean legalHold) {}
