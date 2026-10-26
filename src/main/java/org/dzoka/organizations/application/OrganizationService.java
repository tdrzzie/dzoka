package org.dzoka.organizations.application;

import java.util.List;
import java.util.UUID;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.organizations.domain.Organization;
import org.dzoka.organizations.ports.OrganizationCommands;
import org.springframework.stereotype.Service;

@Service
public final class OrganizationService {
    private final OrganizationCommands commands;
    public OrganizationService(OrganizationCommands commands) { this.commands = commands; }
    public Organization create(VerifiedIdentity actor, UUID tenant, String name, String key) {
        if (key == null || !key.matches("[A-Za-z0-9._:-]{8,128}"))
            throw new IllegalArgumentException("Invalid idempotency key");
        return commands.create(actor, tenant, Organization.create(name), key);
    }
    public List<Organization> list(VerifiedIdentity actor, UUID tenant) { return commands.list(actor, tenant); }
}
