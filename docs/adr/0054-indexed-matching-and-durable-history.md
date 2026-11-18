# 0054. Index the authoritative components and govern matching history

- Status: Implemented with local conformance. Target acceptance remains open.
- Date: 2026-11-18

Reuse the source-reviewed extension and image identities from ADR 0053. Index the existing authoritative 384-dimension active-compatible text component type with pgvector HNSW. PostgreSQL owns insert and delete maintenance. The query uses current report access, opposite-kind, tenant, exact space and activation filters, and re-reads the shortlisted evidence when it cannot load the whole exact pool. The route explicitly reports approximation and capacity. Native exact remains the portable reference. Unsupported production spaces need separately reviewed index adoption and coverage measurements.

Qdrant remains a detached projection. Use the existing rollout advisory lock to serialize the authorized per-tenant and per-space reconciler with activation. Upsert the expected active component IDs before deleting stale points in the exact tenant and space. Core rechecks the current component allowlist before searching even if reconciliation is interrupted. Operators schedule repeatable reconciliation and retain authority for collection provisioning, retention and backup. No matching operation trusts Qdrant output as identity evidence.

Persist optional private, append-oriented run summaries and separate ordered candidate records with report revision foreign keys. Use one transaction, bind run UUID and request, and require current source and candidate visibility on replay. Keep a 24-hour read window and authorized bounded expiry pruning. Deleting an input revision cascades the relevant records. Keep raw source text, vectors, protected identifier digests, and ownership authority outside the ledger. No new binaries or JVM dependencies are adopted.

The authored recall and calibration fixtures verify behavior, not population accuracy or policy approval. See [execution results](../testing/m5-lifecycle-2026-11-18.md).
