package org.dzoka.organizations.ports;

import java.util.List;
import java.util.UUID;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.organizations.domain.Organization;
import org.dzoka.organizations.domain.OrganizationState;

public interface OrganizationCommands {
    Organization create(VerifiedIdentity actor, UUID tenant, Organization proposed, String idempotencyKey);
    List<Organization> list(VerifiedIdentity actor, UUID tenant);
    record Page(List<OrganizationState> items, UUID nextCursor) {}
    Page page(VerifiedIdentity actor, UUID tenant, UUID after, int limit, boolean includeInactive);
    OrganizationState get(VerifiedIdentity actor, UUID tenant, UUID id);
    OrganizationState update(VerifiedIdentity actor, UUID tenant, UUID id, String name, boolean active, long expectedVersion, String key);
}
