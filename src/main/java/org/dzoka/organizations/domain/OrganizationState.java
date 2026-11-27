package org.dzoka.organizations.domain;

import java.util.UUID;

public record OrganizationState(UUID id, String name, long version, boolean active) {
    public OrganizationState { new Organization(id,name,version); }
}
