package org.dzoka.rewards;

public interface PaymentProvider extends org.dzoka.fulfillment.FulfillmentGateway {
    record Money(String currency,long minorUnits) {
        public Money {
            if(currency==null || !currency.matches("[A-Z]{3}") || minorUnits<1 || minorUnits>1_000_000_000_000L)
                throw new IllegalArgumentException("Bounded currency and minor units required");
        }
    }
}
