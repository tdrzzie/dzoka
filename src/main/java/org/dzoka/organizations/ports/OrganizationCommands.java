package org.dzoka.organizations.ports;

import java.util.List;
import java.util.UUID;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.organizations.domain.Organization;

public interface OrganizationCommands {
    Organization create(VerifiedIdentity actor, UUID tenant, Organization proposed, String idempotencyKey);
    List<Organization> list(VerifiedIdentity actor, UUID tenant);
}
