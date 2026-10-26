package org.dzoka.organizations.adapters;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.dzoka.foundation.application.Failures;
import org.dzoka.foundation.persistence.TenantTransactions;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.organizations.domain.Organization;
import org.dzoka.organizations.ports.OrganizationCommands;
import org.springframework.stereotype.Component;

@Component
public final class JdbcOrganizations implements OrganizationCommands {
    private final TenantTransactions transactions;
    public JdbcOrganizations(TenantTransactions transactions) { this.transactions = transactions; }

    @Override public Organization create(VerifiedIdentity actor, UUID tenant, Organization proposed, String key) {
        String keyHash = VerifiedIdentity.digest(key);
        String requestHash = VerifiedIdentity.digest("organizations.create.v1\0" + proposed.name());
        return transactions.execute(actor, tenant, "organizations:write", connection -> {
            try (var insert = connection.prepareStatement("""
                    INSERT INTO dzoka.idempotency (tenant_id, actor_ref, operation, key_hash, request_hash)
                    VALUES (?, ?, 'organizations.create.v1', ?, ?) ON CONFLICT DO NOTHING
                    """)) {
                insert.setObject(1, tenant); insert.setString(2, actor.reference());
                insert.setString(3, keyHash); insert.setString(4, requestHash); insert.executeUpdate();
            }
            try (var find = connection.prepareStatement("""
                    SELECT request_hash, result_id FROM dzoka.idempotency
                    WHERE tenant_id=? AND actor_ref=? AND operation='organizations.create.v1' AND key_hash=?
                    FOR UPDATE
                    """)) {
                find.setObject(1, tenant); find.setString(2, actor.reference()); find.setString(3, keyHash);
                try (var result = find.executeQuery()) {
                    if (!result.next()) throw new Failures.Forbidden();
                    if (!requestHash.equals(result.getString(1))) throw new Failures.Conflict();
                    UUID previous = result.getObject(2, UUID.class);
                    if (previous != null) {
                        try (var lookup = connection.prepareStatement("""
                                SELECT id, name, version FROM dzoka.organizations WHERE tenant_id=? AND id=?
                                """)) {
                            lookup.setObject(1, tenant); lookup.setObject(2, previous);
                            try (var row = lookup.executeQuery()) {
                                if (!row.next()) throw new Failures.Conflict();
                                return new Organization(row.getObject(1, UUID.class), row.getString(2), row.getLong(3));
                            }
                        }
                    }
                }
            }
            try (var insert = connection.prepareStatement("""
                    INSERT INTO dzoka.organizations (tenant_id,id,name,version) VALUES (?,?,?,?)
                    """)) {
                insert.setObject(1, tenant); insert.setObject(2, proposed.id());
                insert.setString(3, proposed.name()); insert.setLong(4, proposed.version()); insert.executeUpdate();
            }
            try (var update = connection.prepareStatement("""
                    UPDATE dzoka.idempotency SET result_id=?
                    WHERE tenant_id=? AND actor_ref=? AND operation='organizations.create.v1' AND key_hash=?
                    """)) {
                update.setObject(1, proposed.id()); update.setObject(2, tenant);
                update.setString(3, actor.reference()); update.setString(4, keyHash);
                if (update.executeUpdate() != 1) throw new Failures.Conflict();
            }
            try (var audit = connection.prepareStatement("""
                    INSERT INTO dzoka.audit_events (tenant_id,id,actor_ref,action,entity_id)
                    VALUES (?, ?, ?, 'organization.created', ?)
                    """)) {
                audit.setObject(1, tenant); audit.setObject(2, UUID.randomUUID());
                audit.setString(3, actor.reference()); audit.setObject(4, proposed.id()); audit.executeUpdate();
            }
            try (var event = connection.prepareStatement("""
                    INSERT INTO dzoka.outbox (tenant_id,id,event_type,aggregate_id,aggregate_version,payload)
                    VALUES (?,?,'organization.created',?,1,jsonb_build_object('organizationId',CAST(? AS text)))
                    """)) {
                event.setObject(1, tenant); event.setObject(2, UUID.randomUUID());
                event.setObject(3, proposed.id()); event.setString(4, proposed.id().toString()); event.executeUpdate();
            }
            return proposed;
        });
    }

    @Override public List<Organization> list(VerifiedIdentity actor, UUID tenant) {
        return transactions.execute(actor, tenant, "organizations:read", connection -> {
            var organizations = new ArrayList<Organization>();
            try (var statement = connection.prepareStatement("""
                    SELECT id,name,version FROM dzoka.organizations WHERE tenant_id=? ORDER BY id LIMIT 100
                    """)) {
                statement.setObject(1, tenant);
                try (var rows = statement.executeQuery()) {
                    while (rows.next()) organizations.add(new Organization(rows.getObject(1, UUID.class),
                            rows.getString(2), rows.getLong(3)));
                }
            }
            return List.copyOf(organizations);
        });
    }
}
