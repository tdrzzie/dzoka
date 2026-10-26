# Source audit: qdrant

Status: COMPLETE

Scope: architecture-relevant static review of the listed source ranges, license evidence, deployment entry points and tests/evaluation where present. No upstream tests were executed and no upstream implementation was copied into Dzoka. Unreviewed subsystems and integration acceptance work are called out explicitly.

## Repository identity

https://github.com/qdrant/qdrant

## Audited branch

master. The exact commit below is authoritative; branch HEAD may move.

## Exact commit SHA

222b2e44ab1c3f17e8246396ead6558e45041df7

## Audit date

2026-10-26

## License

Apache-2.0. Root LICENSE text inspected at the pinned commit: Apache-2.0. Dependency, checkpoint and dataset licenses remain separate.

## Files inspected

- [lib/storage/src/rbac/auth.rs](https://github.com/qdrant/qdrant/blob/222b2e44ab1c3f17e8246396ead6558e45041df7/lib/storage/src/rbac/auth.rs), lines 1-139; role: implementation.
- [LICENSE](https://github.com/qdrant/qdrant/blob/222b2e44ab1c3f17e8246396ead6558e45041df7/LICENSE), lines 1-65; role: license.
- [src/actix/auth.rs](https://github.com/qdrant/qdrant/blob/222b2e44ab1c3f17e8246396ead6558e45041df7/src/actix/auth.rs), lines 1-65, 86-165, 218-256; role: implementation.
- [lib/collection/src/collection_manager/segments_searcher.rs](https://github.com/qdrant/qdrant/blob/222b2e44ab1c3f17e8246396ead6558e45041df7/lib/collection/src/collection_manager/segments_searcher.rs), lines 1-12, 178-187, 206-215, 219-228, 247-256, 268-277; role: implementation.
- [config/production.yaml](https://github.com/qdrant/qdrant/blob/222b2e44ab1c3f17e8246396ead6558e45041df7/config/production.yaml), lines 1-7; role: deployment.
- [tests/consensus_tests/auth_tests/test_jwt_access.py](https://github.com/qdrant/qdrant/blob/222b2e44ab1c3f17e8246396ead6558e45041df7/tests/consensus_tests/auth_tests/test_jwt_access.py), lines 1-12, 654-663, 674-683, 704-713, 740-749, 752-761; role: tests.
- [Dockerfile](https://github.com/qdrant/qdrant/blob/222b2e44ab1c3f17e8246396ead6558e45041df7/Dockerfile), lines 1-12, 63-72, 75-84; role: deployment.

## Runtime architecture

Rust vector service with Actix HTTP authentication, RBAC storage access and segmented search orchestration.

## Domain model

Collection-scoped access, vector-search requests, segment query context and audit events; no ownership/custody domain.

## Storage model

Segmented collection persistence is orchestrated by the reviewed searcher. Full WAL, snapshot, consensus and native index implementation were outside this provider-boundary review.

## AI and matching implementation

Vector retrieval infrastructure, not an embedding generator or confidence model. Query-context work propagates timeout and stopping guards and runs blocking segment work separately.

## Security model

Actix middleware validates request keys/JWTs via AuthKeys, with explicit route whitelists. Auth wraps global/collection access checks with optional audit logging; this is not automatic Dzoka tenant isolation.

## API design

HTTP handlers receive request Auth context; a synchronous manage-access extractor is designed to reject unauthorized snapshot uploads before reading their body. Full gRPC authorization registration was not audited here.

## Event model

Structured access audit events are conditional and internal auth can bypass that logging. Recovery outbox/inbox semantics remain Dzoka responsibilities.

## Deployment model

Production YAML binds HTTP to 0.0.0.0:6333; inspected multistage Dockerfile has external build-tool/base dependencies. Enable authenticated private networking/TLS explicitly; the config fragment alone is not secure deployment proof.

## Test coverage

JWT access test includes REST endpoint coverage checks and real HTTP status verification helpers. Inspected statically, not executed; no Dzoka tenant/filter recall or durability result established.

## Strengths

Explicit collection/global authorization boundary, audit metadata, deadline/cancellation plumbing and substantial service-oriented test machinery.

## Weaknesses

ACL whitelists, optional audit settings and direct/unlogged access require consistent use. Full distributed state, query filter enforcement and image builds were not certified.

## Security concerns

Never expose a vector endpoint or management key to public clients. Enforce tenant/model filters in trusted adapters and restrict forwarded-header trust and snapshot upload privileges.

## Scalability concerns

Segment-level parallelism, cancellation and remaining-time accounting are implemented, but filtered recall and collection/shard strategy require real workload benchmarks.

## Licensing consequences

Preserve applicable license/copyright/NOTICE obligations for future permitted reuse. Code licensing does not establish permission for every model weight or dataset. No upstream implementation was incorporated into the core.

## Reusable components

Optional vector provider candidate behind VectorSearchProvider; retain PostgreSQL as authoritative record and pgvector as initial default, subject to ADR.

## Do not reuse

Business logic coupled to backend score semantics, client-supplied tenant filters, disabled production auth/audit or broad management credentials.

## Architecture lessons

A vector service needs independent auth, tenant/model partitioning, cancellation and replication reconciliation; adding it does not remove transactional-core responsibilities.

## Disposition

ADOPT. Applies to this declared integration scope. It is not a claim of production readiness, blanket source reuse approval or completion of later integration acceptance tests.

## ADRs triggered

Proposed investigations: global/local fingerprint provenance; retrieval/reranking separation; compatible vector spaces; evidence coverage and model governance. This audit supplies evidence for later technical ADRs; it does not finalize the architecture.

## Unresolved questions

Tenant-filter integration tests, complete JWT/key configuration, snapshot restore, consensus failure, index recall and runtime compatibility remain production acceptance work.
