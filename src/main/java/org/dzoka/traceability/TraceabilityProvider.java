package org.dzoka.traceability;

import java.util.UUID;

public interface TraceabilityProvider {
    enum State { SUBMITTED, RUNNING, COMPLETE, REJECTED, RETRY, UNKNOWN }
    record Result(State state,String receipt) {}
    boolean enabled();
    String repository();
    Result capture(UUID id,byte[] document);
    Result status(String receipt);
}
