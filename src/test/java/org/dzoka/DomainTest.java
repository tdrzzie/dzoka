package org.dzoka;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.dzoka.identity.domain.VerifiedIdentity;
import org.dzoka.organizations.domain.Organization;
import org.junit.jupiter.api.Test;

class DomainTest {
    @Test void identifiersAndNamesHaveExplicitInvariants() {
        assertThrows(IllegalArgumentException.class,()->Organization.create(" "));
        assertThrows(IllegalArgumentException.class,()->Organization.create("line\nbreak"));
        assertThrows(IllegalArgumentException.class,()->Organization.create("x".repeat(161)));
        assertThrows(IllegalArgumentException.class,()->new VerifiedIdentity("issuer",""));
        assertNotEquals(new VerifiedIdentity("a","bc").reference(),new VerifiedIdentity("ab","c").reference());
        Set<UUID> ids=new HashSet<>();
        for(int i=0;i<10000;i++){UUID id=Organization.create("Synthetic").id();assertEquals(4,id.version());assertTrue(ids.add(id));}
    }
}
