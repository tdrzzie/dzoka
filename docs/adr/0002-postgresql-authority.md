# 0002: PostgreSQL is the transactional system of record

- Status: Accepted design; implementation and runtime dependency approval remain separate
- Date: 2026-10-26
- Prerequisite: mandatory source audit gate passed before these decisions

## Context

The specification requires consistent claims, custody and payments. Audited CSV/document workflows do not establish atomic recovery transitions.

## Decision

Use PostgreSQL for tenant-owned domain state, audit references, durable jobs, outbox/inbox and ledger entries. Core application services own writes; AI workers have no domain-table write credentials. Model aggregates explicitly rather than event-source the entire platform.

## Alternatives

Separate document stores per domain would require distributed coordination; event sourcing every record adds replay and privacy complexity without established need.

## Consequences

A database outage blocks mutations safely. Backups must include relational state and coordinate object/index recovery. Exact server and migration-tool versions require adoption review.

## Source-audit evidence

The specification and [AGENTS.md](../../AGENTS.md) govern invariants and required stack preferences. These static reviews inform the decision; they do not establish benchmarks or full dependency certification.

- [lost-and-found-tracking-system](../source-audit/platforms/lost-and-found-tracking-system.md) — `4ef1493f80590f6fa03d2ad9eded2e70a59ebe58`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [trishthakur-lost-and-found-ai](../source-audit/platforms/trishthakur-lost-and-found-ai.md) — `8e5fcb6f79595c9d429679ba7868cac4a90e1319`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [openepcis](../source-audit/traceability/openepcis.md) — `e81a857771c4c4a2d12e62b60a2dbc094551aa6e`; inspected ranges and limitations are in the canonical audit and evidence manifest.

## Implementation implications

Prove atomic state/outbox commit, rollback, optimistic conflicts, tenant foreign keys and restore consistency using the selected real database.

The [architecture baseline](../architecture/baseline.md) defines ownership and execution boundaries. Complete the relevant [dependency adoption review](../architecture/dependency-adoption.md) before installing runtime dependencies. Acceptance scenarios here are requirements, not tests already executed.
