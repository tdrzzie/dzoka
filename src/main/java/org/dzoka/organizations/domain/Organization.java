package org.dzoka.organizations.domain;

import java.util.UUID;

public record Organization(UUID id, String name, long version) {
    public Organization {
        if (id == null || name == null || name.isBlank() || name.length() > 160
                || name.codePoints().anyMatch(Character::isISOControl) || version < 1)
            throw new IllegalArgumentException("Invalid organization");
    }
    public static Organization create(String name) { return new Organization(UUID.randomUUID(), name, 1); }
}
