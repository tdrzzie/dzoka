# Tenant isolation contract

Accepted design, 2026-10-26. Implementation is pending. This view follows the [current baseline](baseline.md) and ADRs [0025](../adr/0025-tenant-security.md). The canonical audits linked by those ADRs provide pinned source evidence and scope limits. Runtime/artifact choices must pass [dependency adoption](dependency-adoption.md).

## Trusted context

TenantContext carries authenticated principal/service, authorized tenant, membership/grant, purpose and permission scope. A hostname/path/header can select a requested tenant but cannot authorize it. Recheck membership and policy for sensitive actions. Background work stores original scope and uses a current task grant rather than deserializing trusted authority from arbitrary message payloads.

Persistence uses tenant-qualified identifiers/FKs, repository predicates and row policies. Application roles cannot bypass policies; migrations use separate credentials. Set tenant context transaction-locally so connection-pool reuse cannot leak prior state. Missing context denies queries. Global administrative queries use distinct audited interfaces and do not gain blanket secret access.

## Other isolation surfaces

Object keys and grants, cache keys, index filters, embeddings, inbox/outbox keys, idempotency records, jobs and provider credentials all include tenant scope. Provider results are reauthorized after retrieval. Limits cover per-tenant concurrency, storage, inference spend, fan-out and attempt/disclosure budget so shared infrastructure does not permit noisy-neighbor abuse.

Cross-tenant institutional cooperation goes through explicit policy/consent and minimized federation-style contracts; it is not a repository query without the tenant predicate. A self-hosted single-tenant node uses the same isolation paths to avoid a separate insecure code path.

## Acceptance

A01–A03, A07, A12, A19 and A27. Use two unrelated tenants plus a revoked administrator/service to test every interface, pooled SQL context and index/cache/job boundary.

Acceptance IDs refer to [the implementation acceptance ledger](acceptance.md); none is claimed executed by this document.
