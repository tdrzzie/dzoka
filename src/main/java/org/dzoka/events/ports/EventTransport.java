package org.dzoka.events.ports;

import org.dzoka.events.domain.IntegrationEvent;

/** A successful return is a durable transport acknowledgment. May deliver more than once. */
@FunctionalInterface
public interface EventTransport { void publish(IntegrationEvent event) throws Exception; }
