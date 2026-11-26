package org.dzoka.logistics;

import java.util.UUID;

public interface CourierProvider extends org.dzoka.fulfillment.FulfillmentGateway {
    record Quote(String reference,String currency,long minorUnits) {}
    Quote quote(UUID tenant,String deliveryReference);
}
