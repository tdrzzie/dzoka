package org.dzoka.events.domain;

import java.time.Instant;
import java.util.UUID;

public record IntegrationEvent(UUID tenantId, UUID id, String type, int schemaVersion,
        UUID aggregateId, long aggregateVersion, UUID correlationId, UUID causationId,
        Instant occurredAt, String payload) {}
