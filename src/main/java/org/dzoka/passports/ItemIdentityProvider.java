package org.dzoka.passports;

import java.util.UUID;

public interface ItemIdentityProvider {
    record Assertion(String provider,String version,String reference,boolean verified) {}
    Assertion identify(UUID registeredItem,String provider,String reference);
}
