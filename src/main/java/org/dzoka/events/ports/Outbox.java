package org.dzoka.events.ports;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.dzoka.events.domain.IntegrationEvent;
import org.dzoka.identity.domain.VerifiedIdentity;

public interface Outbox {
    record Delivery(IntegrationEvent event, UUID leaseToken) {}
    Optional<Delivery> claim(VerifiedIdentity worker, UUID tenant, Duration lease);
    boolean acknowledge(VerifiedIdentity worker, Delivery delivery);
}
