# Versioned ItemFingerprint contract

Accepted design, 2026-10-26. `ItemFingerprint` now selects immutable text and explicitly pinned vision/local/OCR/speech components through bounded multimodal cohort activation/rollback; [text](../development/ai-text.md), [perception](../development/perception.md), [rollout guide](../development/fingerprint-rollouts.md) and [STATUS](../../STATUS.md) record exact scope. This view follows the [current baseline](baseline.md) and ADRs [0009](../adr/0009-model-provenance.md); [0010](../adr/0010-global-local-fingerprints.md); [0028](../adr/0028-fingerprint-shadow-rollouts.md); [0029](../adr/0029-local-perception-providers.md). New runtime/artifact choices must pass [dependency adoption](dependency-adoption.md).

## Identity and provenance

FingerprintSnapshot has tenant, local item/report references, immutable revision, input-manifest digest, schema version, generated/recorded times and producer/configuration references. Exact raw input remains in restricted source records; normalized text/attributes retain language, confidence/reliability and source spans. A fingerprint is an evidence snapshot, not a canonical ownership identity.

Components include protected identifiers, category/brand/model/color/material/condition, OCR spans, global image descriptors, localized regions/patch descriptors, perceptual hashes, text/speech descriptors, location resolution/uncertainty, temporal interval/timezone and provenance. Every component distinguishes unavailable/failed/not-applicable from observed mismatch. Secrets reside in protected storage with scoped references; public/federation fingerprints are explicit minimized projections.

## Compatibility and lifecycle

EmbeddingSpace binds provider, model, artifact digest/revision, preprocessing version, dimensions, metric and normalization. Same dimensions alone are insufficient. Region descriptors also bind source image/derivative, coordinate system, crop bounds and extraction policy. Configuration/prompt/output schema versions accompany all non-vector inference.

Append snapshots and results; never silently replace history. A versioned active pointer supports shadow re-embedding and rollback. Expiry/deletion revokes access and removes derived vectors/projections; minimal tombstones prevent stale workers/replays restoring erased evidence. A legal hold is explicit and scoped.

V5 implements this for explicit cohorts of at most 100 report revisions/16 text spaces: complete terminal slots, separately measured available coverage, immutable manifests, explicit evaluation approval reference and tenant pointer compare-and-set. All activation/rollback rechecks current sources/spaces/components; after activation, shadow or noncohort results do not leak into current reads. It does not establish model quality, activate image/OCR/speech providers or migrate ANN indexes.

V6 supplies licensed local perception profiles and protected input/jobs/results. V7 adds up to 64 explicit perception component pins to the source cohort, including perception-only plans. Selected manifests bind profile, input and output hashes; source-pixel boxes/descriptor schemas remain attached to their exact profile. Current coverage becomes AVAILABLE only for selected retained/current/enabled perception evidence, never merely because a worker completed. Empty observations are distinct from missing evidence. Representative quality, wider formats/languages, larger cohorts and ANN migration are not established by this bounded profile.

## Acceptance

A05, A07–A10 and A24. Test stale result rejection, region bounds, zero reliability, mixed model spaces and idempotent migration activation.

Acceptance IDs refer to [the implementation acceptance ledger](acceptance.md); only its recorded text/job portions are executed so far.
