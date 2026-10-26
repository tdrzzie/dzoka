package org.dzoka.events.adapters;

import java.util.UUID;
import org.dzoka.foundation.application.Failures;
import org.dzoka.foundation.persistence.TenantTransactions;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.springframework.stereotype.Component;

@Component
public final class JdbcInbox {
    private final TenantTransactions transactions;
    public JdbcInbox(TenantTransactions transactions) { this.transactions = transactions; }
    /** Effect must only perform local DB work on the supplied transaction, never external calls. */
    public boolean process(VerifiedIdentity worker, UUID tenant, String consumer, UUID eventId,
            String canonicalEnvelope, TenantTransactions.Work<Void> effect) {
        if (consumer == null || !consumer.matches("[a-z0-9.-]{1,100}") || eventId == null
                || canonicalEnvelope == null || canonicalEnvelope.length() > 65536)
            throw new IllegalArgumentException("Invalid inbox envelope");
        String digest = VerifiedIdentity.digest(canonicalEnvelope);
        return transactions.execute(worker, tenant, "events:consume", c -> {
            int inserted;
            try (var s = c.prepareStatement("""
                    INSERT INTO dzoka.inbox (tenant_id,consumer,event_id,payload_hash)
                    VALUES (?,?,?,?) ON CONFLICT DO NOTHING
                    """)) {
                s.setObject(1, tenant); s.setString(2, consumer); s.setObject(3, eventId);
                s.setString(4, digest); inserted = s.executeUpdate();
            }
            if (inserted == 0) {
                try (var s = c.prepareStatement("""
                        SELECT payload_hash FROM dzoka.inbox WHERE tenant_id=? AND consumer=? AND event_id=?
                        """)) {
                    s.setObject(1, tenant); s.setString(2, consumer); s.setObject(3, eventId);
                    try (var r = s.executeQuery()) {
                        if (!r.next() || !digest.equals(r.getString(1))) throw new Failures.Conflict();
                    }
                }
                return false;
            }
            effect.run(c);
            return true;
        });
    }
}
