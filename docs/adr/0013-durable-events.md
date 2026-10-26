# 0013: Use Kafka-compatible events with outbox and inbox

- Status: Accepted design; implementation and runtime dependency approval remain separate
- Date: 2026-10-26
- Prerequisite: mandatory source audit gate passed before these decisions

## Context

Core state and broker publication cannot be assumed to commit together. Upstream event topology does not prove exactly-once external effects.

## Decision

Use a Kafka-compatible production broker, transactional database outbox and consumer inbox keyed by consumer, tenant and event ID. Persist aggregate sequence and correlation/causation. Partition by tenant plus aggregate reference; check aggregate version rather than assuming total ordering. Broker distribution/client versions remain adoption decisions.

## Alternatives

Direct publish after a domain commit can lose events; broker-only exactly-once claims do not cover payment or courier APIs.

## Consequences

Publisher may deliver duplicates. Consumers commit inbox with local effects; external effects use durable operation keys and reconciliation. DLQ payloads exclude secrets. Replay never regenerates a new external operation key.

## Source-audit evidence

The specification and [AGENTS.md](../../AGENTS.md) govern invariants and required stack preferences. These static reviews inform the decision; they do not establish benchmarks or full dependency certification.

- [openepcis](../source-audit/traceability/openepcis.md) — `e81a857771c4c4a2d12e62b60a2dbc094551aa6e`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [findback](../source-audit/platforms/findback.md) — `fc91563886e4e8da6931bc07dd025fbe235068a2`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [kwasu-lost-found-bot](../source-audit/platforms/kwasu-lost-found-bot.md) — `e4863739135594d8ce0a541be6029d0bba5ae95c`; inspected ranges and limitations are in the canonical audit and evidence manifest.

## Implementation implications

Inject failures around DB commit, publish acknowledgment, inbox commit and external callback; test duplicates, poison messages, ordering gaps and replay.

The [architecture baseline](../architecture/baseline.md) defines ownership and execution boundaries. Complete the relevant [dependency adoption review](../architecture/dependency-adoption.md) before installing runtime dependencies. Acceptance scenarios here are requirements, not tests already executed.
