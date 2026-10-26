package org.dzoka.events.application;

import java.time.Duration;
import java.util.UUID;
import org.dzoka.events.ports.Outbox;
import org.dzoka.events.ports.EventTransport;
import org.dzoka.identity.domain.VerifiedIdentity;

/** Explicitly constructed worker; no default fake transport or automatic publisher. */
public final class OutboxDispatcher {
    private final Outbox outbox;
    private final EventTransport transport;
    public OutboxDispatcher(Outbox outbox, EventTransport transport) {
        this.outbox = outbox; this.transport = transport;
    }
    public boolean dispatchOne(VerifiedIdentity worker, UUID tenant) throws Exception {
        var claimed = outbox.claim(worker, tenant, Duration.ofSeconds(30));
        if (claimed.isEmpty()) return false;
        var delivery = claimed.get();
        transport.publish(delivery.event()); // outside the transaction; an ambiguous failure leaves the lease.
        return outbox.acknowledge(worker, delivery);
    }
}
