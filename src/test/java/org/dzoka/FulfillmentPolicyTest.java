package org.dzoka;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import org.dzoka.fulfillment.FulfillmentWebhook;
import org.dzoka.passports.LocalItemIdentity;
import org.dzoka.rewards.PaymentProvider.Money;
import org.junit.jupiter.api.Test;

class FulfillmentPolicyTest {
    @Test void moneyAndUnadoptedIdentityProvidersFailClosed() {
        assertThrows(IllegalArgumentException.class,()->new Money("USD",0));
        assertThrows(IllegalArgumentException.class,()->new Money("usd",100));
        assertThrows(IllegalArgumentException.class,()->new Money("USD",Long.MAX_VALUE));
        var provider=new LocalItemIdentity();var id=java.util.UUID.randomUUID();
        assertTrue(provider.identify(id,"INTERNAL_UUID",id.toString()).verified());
        assertThrows(IllegalArgumentException.class,()->provider.identify(id,"DID","did:example:x"));
        assertThrows(IllegalArgumentException.class,()->provider.identify(id,"GS1_DIGITAL_LINK","http://example.invalid/01/12345678901234"));
    }
    @Test void absentOrMalformedWebhookConfigurationCannotAuthenticate() {
        assertFalse(FulfillmentWebhook.verify(null,"0","0".repeat(64),new byte[0],Instant.now()));
        assertFalse(FulfillmentWebhook.verify(new byte[32],"not-a-time","0".repeat(64),new byte[0],Instant.now()));
        assertFalse(FulfillmentWebhook.verify(new byte[32],"999999999999","0".repeat(64),new byte[0],Instant.now()));
    }
    @Test void providerIsDisabledWithoutConfigurationAndInsecureOriginsNeedExplicitFixtureMode() {
        var empty=environment(java.util.Map.of());
        var mapper=new tools.jackson.databind.ObjectMapper();
        assertFalse(new org.dzoka.fulfillment.GatewayHttp(empty,mapper).enabled());
        var insecure=environment(java.util.Map.of("DZOKA_FULFILLMENT_ORIGIN","http://127.0.0.1:12345","DZOKA_FULFILLMENT_TOKEN","fixture"));
        assertThrows(IllegalArgumentException.class,()->new org.dzoka.fulfillment.GatewayHttp(insecure,mapper));
    }
    private static org.springframework.core.env.StandardEnvironment environment(java.util.Map<String,Object> properties) {
        var env=new org.springframework.core.env.StandardEnvironment();
        for(String name:env.getPropertySources().stream().map(org.springframework.core.env.PropertySource::getName).toList())env.getPropertySources().remove(name);
        env.getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("fixture",properties));return env;
    }
}
