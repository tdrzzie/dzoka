package org.dzoka.organizations.adapters;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.dzoka.foundation.application.Failures;
import org.dzoka.foundation.persistence.TenantTransactions;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.organizations.domain.Organization;
import org.dzoka.organizations.domain.OrganizationState;
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
                    SELECT request_hash, result_id, result_name, result_version FROM dzoka.idempotency
                    WHERE tenant_id=? AND actor_ref=? AND operation='organizations.create.v1' AND key_hash=?
                    FOR UPDATE
                    """)) {
                find.setObject(1, tenant); find.setString(2, actor.reference()); find.setString(3, keyHash);
                try (var result = find.executeQuery()) {
                    if (!result.next()) throw new Failures.Forbidden();
                    if (!requestHash.equals(result.getString(1))) throw new Failures.Conflict();
                    UUID previous = result.getObject(2, UUID.class);
                    if (previous != null) {
                        return new Organization(previous,result.getString(3),result.getLong(4));
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
                    UPDATE dzoka.idempotency SET result_id=?,result_name=?,result_version=?
                    WHERE tenant_id=? AND actor_ref=? AND operation='organizations.create.v1' AND key_hash=?
                    """)) {
                update.setObject(1, proposed.id());update.setString(2,proposed.name());update.setLong(3,proposed.version());update.setObject(4, tenant);
                update.setString(5, actor.reference()); update.setString(6, keyHash);
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
                    SELECT id,name,version FROM dzoka.organizations WHERE tenant_id=? AND active ORDER BY id LIMIT 100
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

    @Override public OrganizationState get(VerifiedIdentity actor,UUID tenant,UUID id) {
        return transactions.execute(actor,tenant,"organizations:read",c->{
            try(var s=c.prepareStatement("SELECT id,name,version,active FROM dzoka.organizations WHERE tenant_id=? AND id=?")) {
                s.setObject(1,tenant);s.setObject(2,id);
                try(var r=s.executeQuery()){if(!r.next())throw new Failures.NotFound();return new OrganizationState(r.getObject(1,UUID.class),r.getString(2),r.getLong(3),r.getBoolean(4));}
            }
        });
    }
    @Override public Page page(VerifiedIdentity actor, UUID tenant, UUID after, int limit, boolean includeInactive) {
        return transactions.execute(actor,tenant,"organizations:read",c->{
            var rows=new ArrayList<OrganizationState>();
            try(var s=c.prepareStatement("SELECT id,name,version,active FROM dzoka.organizations WHERE tenant_id=? AND (? OR active) AND (?::uuid IS NULL OR id>?) ORDER BY id LIMIT ?")) {
                s.setObject(1,tenant);s.setBoolean(2,includeInactive);s.setObject(3,after);s.setObject(4,after);s.setInt(5,limit+1);
                try(var r=s.executeQuery()){while(r.next())rows.add(new OrganizationState(r.getObject(1,UUID.class),r.getString(2),r.getLong(3),r.getBoolean(4)));}
            }
            boolean more=rows.size()>limit;if(more)rows.removeLast();
            return new Page(List.copyOf(rows),more?rows.getLast().id():null);
        });
    }

    @Override public OrganizationState update(VerifiedIdentity actor, UUID tenant, UUID id, String name, boolean active, long expectedVersion, String key) {
        String keyHash=VerifiedIdentity.digest(key);
        String requestHash=VerifiedIdentity.digest("organization.update.v1\0"+id+"\0"+expectedVersion+"\0"+active+"\0"+name);
        return transactions.execute(actor,tenant,"organizations:write",c->{
            try(var lock=c.prepareStatement("SELECT pg_advisory_xact_lock(hashtextextended(?,26))")) {
                lock.setString(1,tenant+":"+actor.reference()+":"+keyHash);lock.execute();
            }
            try(var s=c.prepareStatement("SELECT request_hash,organization_id,result_name,result_version,result_active FROM dzoka.organization_commands WHERE tenant_id=? AND actor_ref=? AND key_hash=?")) {
                s.setObject(1,tenant);s.setString(2,actor.reference());s.setString(3,keyHash);
                try(var r=s.executeQuery()){if(r.next()) {
                    if(!requestHash.equals(r.getString(1)))throw new Failures.Conflict();
                    return new OrganizationState(r.getObject(2,UUID.class),r.getString(3),r.getLong(4),r.getBoolean(5));
                }}
            }
            long next;
            try(var s=c.prepareStatement("SELECT version FROM dzoka.organizations WHERE tenant_id=? AND id=? FOR UPDATE")) {
                s.setObject(1,tenant);s.setObject(2,id);
                try(var r=s.executeQuery()){if(!r.next())throw new Failures.NotFound();if(r.getLong(1)!=expectedVersion)throw new Failures.Conflict();next=Math.addExact(expectedVersion,1);}
            }
            try(var s=c.prepareStatement("UPDATE dzoka.organizations SET name=?,active=?,version=? WHERE tenant_id=? AND id=?")) {
                s.setString(1,name);s.setBoolean(2,active);s.setLong(3,next);s.setObject(4,tenant);s.setObject(5,id);s.executeUpdate();
            }
            try(var s=c.prepareStatement("INSERT INTO dzoka.organization_commands(tenant_id,actor_ref,key_hash,request_hash,organization_id,result_name,result_version,result_active) VALUES (?,?,?,?,?,?,?,?)")) {
                s.setObject(1,tenant);s.setString(2,actor.reference());s.setString(3,keyHash);s.setString(4,requestHash);s.setObject(5,id);s.setString(6,name);s.setLong(7,next);s.setBoolean(8,active);s.executeUpdate();
            }
            try(var s=c.prepareStatement("INSERT INTO dzoka.audit_events(tenant_id,id,actor_ref,action,entity_id) VALUES (?,?,?,'organization.updated',?)")) {
                s.setObject(1,tenant);s.setObject(2,UUID.randomUUID());s.setString(3,actor.reference());s.setObject(4,id);s.executeUpdate();
            }
            try(var s=c.prepareStatement("INSERT INTO dzoka.outbox(tenant_id,id,event_type,aggregate_id,aggregate_version,payload) VALUES (?,?,'organization.updated',?,?,jsonb_build_object('organizationId',CAST(? AS text)))")) {
                s.setObject(1,tenant);s.setObject(2,UUID.randomUUID());s.setObject(3,id);s.setLong(4,next);s.setString(5,id.toString());s.executeUpdate();
            }
            return new OrganizationState(id,name,next,active);
        });
    }
}
