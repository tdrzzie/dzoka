# 0019: Federate independent recovery nodes

- Status: Accepted design; implementation and runtime dependency approval remain separate
- Date: 2026-10-26
- Prerequisite: mandatory source audit gate passed before these decisions

## Context

Institutions retain local policy, custody and data. Search federation concepts supply transport patterns rather than recovery trust.

## Decision

Federation gateway exchanges versioned minimized requests/results between allowlisted node identities. Use authenticated transport plus signed canonical envelopes with key ID, issuer/audience, request ID, issued/expiry times and payload digest. Enforce replay checks, revocation, bounded fan-out, deadlines, quotas and cancellation. Specify concrete cryptography/canonicalization before protocol code.

## Alternatives

Replicating private databases/shards across nodes weakens local authority; public unauthenticated fan-out enables enumeration and resource abuse.

## Consequences

Each node can refuse a query and is authoritative for its local evidence and custody. Trust is policy-scoped and revocable, not transitive. Remote approval never directly mutates local release state.

## Source-audit evidence

The specification and [AGENTS.md](../../AGENTS.md) govern invariants and required stack preferences. These static reviews inform the decision; they do not establish benchmarks or full dependency certification.

- [mycel](../source-audit/federation/mycel.md) — `07a671d03c4fbb68f459d24af262a3dec563c913`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [open-federated-search](../source-audit/federation/open-federated-search.md) — `fe13ee670bd12d55d693a1e55f5be03c7d01333c`; inspected ranges and limitations are in the canonical audit and evidence manifest.

## Implementation implications

Two-node tests cover replay, audience substitution, key rotation/revocation, partial timeout, quota exhaustion and unauthorized field requests.

The [architecture baseline](../architecture/baseline.md) defines ownership and execution boundaries. Complete the relevant [dependency adoption review](../architecture/dependency-adoption.md) before installing runtime dependencies. Acceptance scenarios here are requirements, not tests already executed.
