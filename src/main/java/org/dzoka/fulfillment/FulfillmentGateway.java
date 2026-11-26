package org.dzoka.fulfillment;

import java.util.UUID;

/** Versioned external operation contract. NOT_FOUND guarantees no effect under this stable key. */
public interface FulfillmentGateway {
    enum State { CONFIRMED, REJECTED, UNKNOWN, NOT_FOUND }
    record Result(State state,String reference) {}
    boolean enabled();
    Result send(UUID tenant,UUID operation,String kind,String payload);
    Result reconcile(UUID tenant,UUID operation);
}
