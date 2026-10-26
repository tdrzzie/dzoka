# 0017: Expose EPCIS through a compatibility adapter

- Status: Accepted design; implementation and runtime dependency approval remain separate
- Date: 2026-10-26
- Prerequisite: mandatory source audit gate passed before these decisions

## Context

OpenEPCIS provides relevant capture/query concepts and GS1 supplies traceability semantics, but neither is an ownership engine.

## Decision

Keep native tenant-scoped recovery/custody events authoritative. Map explicitly supported events to a versioned EPCIS projection via a TraceabilityProvider. Store source event identity, mapping version and external capture receipt. Unsupported event types fail explicitly; no blanket EPCIS conformance claim.

## Alternatives

Making an external EPCIS repository the primary domain database would replace recovery semantics with an interoperability model.

## Consequences

External capture/query is optional and asynchronous. Minimize identifiers/locations and enforce visibility. Select supported EPCIS version/event subset and vocabulary in an adapter contract before implementation.

## Source-audit evidence

The specification and [AGENTS.md](../../AGENTS.md) govern invariants and required stack preferences. These static reviews inform the decision; they do not establish benchmarks or full dependency certification.

- [openepcis](../source-audit/traceability/openepcis.md) — `e81a857771c4c4a2d12e62b60a2dbc094551aa6e`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [gs1-end-to-end-traceability](../source-audit/traceability/gs1-end-to-end-traceability.md) — `2.0.0 (August 2017)`; inspected ranges and limitations are in the canonical audit and evidence manifest.

## Implementation implications

Golden mapping fixtures, duplicate capture, correction events, projection rollback/replay and tenant/private-field filtering must pass.

The [architecture baseline](../architecture/baseline.md) defines ownership and execution boundaries. Complete the relevant [dependency adoption review](../architecture/dependency-adoption.md) before installing runtime dependencies. Acceptance scenarios here are requirements, not tests already executed.
