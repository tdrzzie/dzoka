# 0025: Enforce tenancy at every boundary

- Status: Accepted design; implementation and runtime dependency approval remain separate
- Date: 2026-10-26
- Prerequisite: mandatory source audit gate passed before these decisions

## Context

Audited platform authorization weaknesses and index/library scopes show that a tenant field alone is insufficient.

## Decision

Resolve tenant from authenticated membership/service grants, never trust a body/header without authorization. Use tenant-qualified keys/FKs, repository predicates and PostgreSQL row policies as defense in depth. Application roles cannot bypass row policies. Set transaction-local tenant context and clear pooled state. Scope object grants, index filters, caches, jobs and provider credentials; core reauthorizes results.

## Alternatives

UI-only filtering and unqualified primary-key lookups permit cross-tenant reads/writes; schema-per-tenant everywhere adds unproven migration cost.

## Consequences

Administrative jobs use explicit audited per-tenant grants; missing tenant context fails closed. OIDC handles authentication, local membership/policy handles authorization. Exact IdP, JWT algorithms, cryptography and secrets tooling await adoption review.

## Source-audit evidence

The specification and [AGENTS.md](../../AGENTS.md) govern invariants and required stack preferences. These static reviews inform the decision; they do not establish benchmarks or full dependency certification.

- [ai-lost-and-found](../source-audit/platforms/ai-lost-and-found.md) — `577edc3b492f62ac8707fc950230bc04a354e9d8`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [findback](../source-audit/platforms/findback.md) — `fc91563886e4e8da6931bc07dd025fbe235068a2`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [qdrant](../source-audit/retrieval/qdrant.md) — `222b2e44ab1c3f17e8246396ead6558e45041df7`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [malak-lostandfound](../source-audit/platforms/malak-lostandfound.md) — `d5d7bfaf043396a32b40c264c647f6456cadb633`; inspected ranges and limitations are in the canonical audit and evidence manifest.

## Implementation implications

Run two-tenant adversarial tests across HTTP, SQL, reused connections, jobs, media URLs, caches, vectors, federation and support/admin workflows.

The [architecture baseline](../architecture/baseline.md) defines ownership and execution boundaries. Complete the relevant [dependency adoption review](../architecture/dependency-adoption.md) before installing runtime dependencies. Acceptance scenarios here are requirements, not tests already executed.
