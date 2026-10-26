# Source audit: pgvector

Status: COMPLETE

Scope: PostgreSQL vector SQL contract, HNSW iterative scan implementation, filtered-scan test and extension/container packaging; not all native code, access methods or PostgreSQL security.

Completion means bounded static source review, not a full security audit, executed tests, benchmark or production certification. No upstream implementation copied into Dzoka.

## Repository identity

https://github.com/pgvector/pgvector

## Audited branch

master. Revision pinned below; this record does not assert the pin remains branch HEAD.

## Exact commit SHA

5db3087b27a4fd095e7398f4b0f2706bf6d54bd3

## Audit date

2026-10-26

## License

PostgreSQL. Root LICENSE inspected; see evidence manifest. Code and model-weight licensing are separate.

## Files inspected

- [LICENSE](https://github.com/pgvector/pgvector/blob/5db3087b27a4fd095e7398f4b0f2706bf6d54bd3/LICENSE): lines 1–20. Permissive PostgreSQL-family notice verified.
- [src/hnswscan.c](https://github.com/pgvector/pgvector/blob/5db3087b27a4fd095e7398f4b0f2706bf6d54bd3/src/hnswscan.c): lines 1–100, 201–350. Dimension checks, layered search, MVCC requirement, scan memory/tuple budgets and ordering behavior.
- [sql/vector.sql](https://github.com/pgvector/pgvector/blob/5db3087b27a4fd095e7398f4b0f2706bf6d54bd3/sql/vector.sql): lines 1–100. SQL vector type and strict immutable distance/dimension functions.
- [test/t/043_hnsw_iterative_scan.pl](https://github.com/pgvector/pgvector/blob/5db3087b27a4fd095e7398f4b0f2706bf6d54bd3/test/t/043_hnsw_iterative_scan.pl): lines 1–59. Synthetic filtered HNSW count tests with scan tuple and memory limits.
- [vector.control](https://github.com/pgvector/pgvector/blob/5db3087b27a4fd095e7398f4b0f2706bf6d54bd3/vector.control): lines 1–4. Extension reports version 0.8.7 at this commit.
- [Dockerfile](https://github.com/pgvector/pgvector/blob/5db3087b27a4fd095e7398f4b0f2706bf6d54bd3/Dockerfile): lines 1–23. Build downloads tag v0.8.7 rather than automatically using the audit commit.

## Runtime architecture

Native PostgreSQL extension registering vector types, distance functions and index access methods. Reviewed HNSW code executes inside PostgreSQL and requires MVCC-compliant snapshots.

## Domain model

Generic vectors, distances and index scans; no item, tenant, ownership or recovery semantics. Dzoka must supply those models and authorization boundaries.

## Storage model

Vectors live in PostgreSQL relations and HNSW index pages. The inspected scan coordinates with vacuum through shared page locks and checks dimensions against the index.

## AI and matching implementation

HNSW traverses upper layers and searches the ground layer; iterative scanning resumes from discarded candidates. It retrieves neighbors, does not generate embeddings or establish same-instance identity or ownership.

## Security model

SQL execution and data access operate within PostgreSQL. No application identity/tenant authorization is provided by these reviewed extension functions; PostgreSQL roles, RLS and application authorization require separate design and tests.

## API design

SQL vector type plus L2, inner product, cosine, L1, dimension and normalization functions. No standalone REST service in this integration boundary.

## Event model

No recovery or application event semantics in reviewed paths. WAL/recovery correctness and event outbox design are separate concerns; WAL implementation was not audited here.

## Deployment model

Extension control and SQL installation packaged into PostgreSQL. Upstream Dockerfile fetches v0.8.7; build provenance must pin both extension release/commit and base-image digest before production use.

## Test coverage

Inspected a PostgreSQL::Test::Cluster test with 100,000 synthetic vectors and filtered queries under different iterative scan budgets. Not executed; no Dzoka workload or tenant-isolation benchmark established.

## Strengths

SQL integration, explicit dimension checks, scan work/memory controls, MVCC-aware scan behavior and reproducible test harness make it a strong default-provider candidate.

## Weaknesses

Approximate retrieval and filtered-result yield depend on search settings. The reviewed test measures counts under a synthetic filter, not physical-object accuracy. No independent SQL planner/recall benchmark was run.

## Security concerns

Native extension code shares the database process; trusted installation and patch governance matter. This selected-file review does not certify memory safety, RLS behavior or all dependency security.

## Scalability concerns

Tuple and memory limits can terminate expansion; strict ordering may skip out-of-order candidates. Benchmark recall, latency and memory with tenant/model filters and realistic cardinalities before adopting a configuration.

## Licensing consequences

Root license grants permissive use with required notices. Package/container components require their own review. No upstream C code copied into Dzoka.

## Reusable components

ADOPT as a version-pinned extension behind VectorSearchProvider after the audit gate and ADR. Use tenant and model-space predicates; retain Qdrant as an independently reviewed optional provider.

## Do not reuse

Do not expose extension internals as the business API, compare incompatible model spaces or mistake ANN similarity for verified ownership. Do not build a purported audit-commit image using an unrelated mutable source ref.

## Architecture lessons

PostgreSQL vector retrieval can keep transactional metadata and vectors together. Filtering and work budgets are retrieval-quality decisions that must be evaluated, not assumed safe from a top-K interface.

## Disposition

ADOPT. Applies only to the declared review scope; runtime adoption still requires the project-wide audit gate and relevant ADRs.

## ADRs triggered

Proposed: pgvector default, vector provider abstraction and bounded model-aware retrieval. Version selection and PostGIS coexistence still require integration tests and final ADR.

## Unresolved questions

IVFFlat, sparse/half vector paths, all native input validation, PostgreSQL/PostGIS version matrix, backup/restore, index builds/vacuum under load, multi-tenant recall and image digest selection remain outside scope.
