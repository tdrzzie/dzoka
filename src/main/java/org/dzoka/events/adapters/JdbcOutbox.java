package org.dzoka.events.adapters;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.dzoka.events.domain.IntegrationEvent;
import org.dzoka.foundation.persistence.TenantTransactions;
import org.dzoka.events.ports.Outbox;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.springframework.stereotype.Component;

@Component
public final class JdbcOutbox implements Outbox {
    private final TenantTransactions transactions;
    public JdbcOutbox(TenantTransactions transactions) { this.transactions = transactions; }
    public Optional<Delivery> claim(VerifiedIdentity worker, UUID tenant, Duration lease) {
        if (lease.isNegative() || lease.isZero() || lease.compareTo(Duration.ofMinutes(5)) > 0)
            throw new IllegalArgumentException("Lease out of range");
        return transactions.execute(worker, tenant, "events:publish", c -> {
            try (var s = c.prepareStatement("""
                    WITH next AS (
                      SELECT id FROM dzoka.outbox
                      WHERE tenant_id=? AND published_at IS NULL
                        AND (lease_until IS NULL OR lease_until < clock_timestamp())
                      ORDER BY occurred_at,id FOR UPDATE SKIP LOCKED LIMIT 1
                    )
                    UPDATE dzoka.outbox o SET lease_token=?,
                      lease_until=clock_timestamp()+(? * interval '1 millisecond'),attempts=attempts+1
                    FROM next WHERE o.tenant_id=? AND o.id=next.id
                    RETURNING o.*
                    """)) {
                UUID token = UUID.randomUUID();
                s.setObject(1, tenant); s.setObject(2, token); s.setLong(3, lease.toMillis()); s.setObject(4, tenant);
                try (var r = s.executeQuery()) {
                    if (!r.next()) return Optional.empty();
                    var event = new IntegrationEvent(tenant, r.getObject("id", UUID.class),
                            r.getString("event_type"), r.getInt("schema_version"),
                            r.getObject("aggregate_id", UUID.class), r.getLong("aggregate_version"),
                            r.getObject("correlation_id", UUID.class), r.getObject("causation_id", UUID.class),
                            r.getTimestamp("occurred_at").toInstant(), r.getString("payload"));
                    return Optional.of(new Delivery(event, token));
                }
            }
        });
    }
    public boolean acknowledge(VerifiedIdentity worker, Delivery delivery) {
        return transactions.execute(worker, delivery.event().tenantId(), "events:publish", c -> {
            try (var s = c.prepareStatement("""
                    UPDATE dzoka.outbox SET published_at=clock_timestamp(),lease_token=NULL,lease_until=NULL
                    WHERE tenant_id=? AND id=? AND lease_token=? AND lease_until>=clock_timestamp()
                      AND published_at IS NULL
                    """)) {
                s.setObject(1, delivery.event().tenantId()); s.setObject(2, delivery.event().id());
                s.setObject(3, delivery.leaseToken()); return s.executeUpdate() == 1;
            }
        });
    }
}
