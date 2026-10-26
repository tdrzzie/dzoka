package org.dzoka.foundation.security;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class SecurityConfigurationTest {
    @Test void insecureOrAmbiguousIdentityConfigurationFailsClosed() {
        assertThrows(IllegalArgumentException.class,()->SecurityConfiguration.requireEndpoint("http://127.0.0.1/issuer",false));
        assertThrows(IllegalArgumentException.class,()->SecurityConfiguration.requireEndpoint("http://example.com/issuer",true));
        assertThrows(IllegalArgumentException.class,()->SecurityConfiguration.requireEndpoint("https://user:password@example.com/issuer",false));
        assertThrows(IllegalArgumentException.class,()->SecurityConfiguration.requireEndpoint("https://example.com/#fragment",false));
        assertDoesNotThrow(()->SecurityConfiguration.requireEndpoint("https://idp.example/issuer",false));
        assertThrows(IllegalArgumentException.class,()->new SecurityConfiguration().jwtDecoder("https://idp.example","https://idp.example/jwks","",false));
    }
}
