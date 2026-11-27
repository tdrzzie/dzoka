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
    public org.dzoka.organizations.domain.OrganizationState get(VerifiedIdentity actor, UUID tenant, UUID id) { return commands.get(actor,tenant,id); }
    public OrganizationCommands.Page page(VerifiedIdentity actor, UUID tenant, UUID after, int limit, boolean includeInactive) {
        if(limit<1 || limit>100)throw new IllegalArgumentException("Invalid page size");
        return commands.page(actor,tenant,after,limit,includeInactive);
    }
    public org.dzoka.organizations.domain.OrganizationState update(VerifiedIdentity actor, UUID tenant, UUID id,
            String name, boolean active, long expectedVersion, String key) {
        if(id==null || expectedVersion<1 || key==null || !key.matches("[A-Za-z0-9._:-]{8,128}"))throw new IllegalArgumentException("Invalid update");
        new Organization(id,name,expectedVersion);
        return commands.update(actor,tenant,id,name,active,expectedVersion,key);
    }
}
