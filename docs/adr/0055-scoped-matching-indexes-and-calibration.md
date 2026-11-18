# 0055. Scope indexed matching and calibration to current evidence

- Status: Implemented with local conformance. Target rollout and representative acceptance remain open.
- Date: 2026-11-18

The exact pool remains the reference for a visible set of at most 512 opposite-kind reports. Above this limit, independently query the PostgreSQL full-text GIN, latitude, time-range, declared category and protected-identifier indexes. Apply live tenant, actor, report-kind, revision and category checks before the shortlist limit, and check the recorded revisions again before scoring. The lexical shortlist uses PostgreSQL tokenization and relevance before exact application scoring, so report it as approximate. Spatial/time uses the authoritative uncertainty and interval comparison. Category is a declaration on the report revision and is useful for retrieval and filtering only. Missing categories remain absent.

Obtain vector candidates independently of the unindexed report pool. Preserve the reviewed 384-dimensional HNSW production path and the bounded portable, pgvector exact and Qdrant paths for active text spaces. Compare image/image and image/text evidence only among compatible 768-dimensional pins in the current multimodal rollout. An image must have a live sanitized derivative with matching input provenance. Use the best comparable pair per candidate from at most 64 pins. Combine related lexical and embedding signals within one evidence group. Report contradictory verified serials even when visual similarity is high.

Scope client search IDs to tenant and actor. Account for retained runs independently of report visibility, so superseding a report cannot bypass the cap. The immutable history payload keeps opaque component references. A replay with a retired joint pin or expired media fails closed. Matching confidence displayed on replay is subject to current calibration status.

Accept an offline calibration proposal and a separate hash-bound approval from a different authorized principal. Approvals and revocations are append-only. Match the current tenant, space identity, activation version, source kind, scoring policy and vector backend, and require reviewed coverage, fit support and expiry. The runtime offers estimates only for comparable evidence in the supported unfiltered text-space search population. Revocation or expiry of the latest review cannot reactivate an earlier approval. Authored and public catalog diagnostics do not approve a deployment probability. Match results have no ownership or release authority.

No JVM runtime dependency or model weights are added. See the [M5 execution record](../testing/m5-verification-2026-11-18.md).
