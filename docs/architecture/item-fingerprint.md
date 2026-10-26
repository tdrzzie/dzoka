# Versioned ItemFingerprint contract

Accepted design, 2026-10-26. A first-class `ItemFingerprint` current view and immutable text components are implemented in the first M4 slice; [run guide](../development/ai-text.md) and [STATUS](../../STATUS.md) distinguish this from full multimodal snapshots and promotion/rollback. This view follows the [current baseline](baseline.md) and ADRs [0009](../adr/0009-model-provenance.md); [0010](../adr/0010-global-local-fingerprints.md). New runtime/artifact choices must pass [dependency adoption](dependency-adoption.md).

## Identity and provenance

FingerprintSnapshot has tenant, local item/report references, immutable revision, input-manifest digest, schema version, generated/recorded times and producer/configuration references. Exact raw input remains in restricted source records; normalized text/attributes retain language, confidence/reliability and source spans. A fingerprint is an evidence snapshot, not a canonical ownership identity.

Components include protected identifiers, category/brand/model/color/material/condition, OCR spans, global image descriptors, localized regions/patch descriptors, perceptual hashes, text/speech descriptors, location resolution/uncertainty, temporal interval/timezone and provenance. Every component distinguishes unavailable/failed/not-applicable from observed mismatch. Secrets reside in protected storage with scoped references; public/federation fingerprints are explicit minimized projections.

## Compatibility and lifecycle

EmbeddingSpace binds provider, model, artifact digest/revision, preprocessing version, dimensions, metric and normalization. Same dimensions alone are insufficient. Region descriptors also bind source image/derivative, coordinate system, crop bounds and extraction policy. Configuration/prompt/output schema versions accompany all non-vector inference.

Append snapshots and results; never silently replace history. A versioned active pointer supports shadow re-embedding and rollback. Expiry/deletion revokes access and removes derived vectors/projections; minimal tombstones prevent stale workers/replays restoring erased evidence. A legal hold is explicit and scoped.

## Acceptance

A05, A07–A10 and A24. Test stale result rejection, region bounds, zero reliability, mixed model spaces and idempotent migration activation.

Acceptance IDs refer to [the implementation acceptance ledger](acceptance.md); only its recorded text/job portions are executed so far.
