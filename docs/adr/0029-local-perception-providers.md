# 0029: Run bounded licensed perception through private leases

- Status: Accepted scoped implementation profile
- Date: 2026-10-26

## Context

M4 needs executable image, local-feature, OCR and speech providers without unverified conversion grants, cloud dependencies or broad media credentials. Existing text jobs/spaces remain compatible and unchanged. Provider completion is derived evidence, never ownership/release authority.

## Decision

Adopt reviewed ONNX ResNet18 visual signatures, official PaddlePaddle PP-OCRv6 detection/recognition and a reviewed Wav2Vec2 export; preserve all upstream/export license obligations separately. Implement independently authored bounded Harris/patch local descriptors as an explicitly named classical baseline, not a learned-feature fallback. Reuse the locked CPU ONNX/NumPy/PyYAML runtime; no new Python/Maven dependency. Each profile binds all artifacts, configuration, implementation and runtime hashes and is explicitly provisioned disabled until checked.

Use a separate durable perception lane with the same tenant-wide AI reservation ledger/advisory lock, three attempts, 120-second leases, one-day deadlines and immutable results. Input is either an existing private sanitized PNG manifest or a strictly bounded PCM16 mono 16kHz WAV. WAV intake preserves exact accepted bytes in a restricted tenant/source table; it accepts no compressed codec or metadata chunks. Heavy parsing/inference occurs in the private worker, which has no DB/S3 credentials. A lease-bound authenticated binary-input endpoint rechecks current report revision, media manifest, expiry and worker identity before and after object read. There are no arbitrary URLs or runtime downloads.

Outputs carry source/profile/input/time provenance and typed finite vectors, pixel-bounded local descriptors/OCR spans, or bounded CTC transcripts/uncertainty. Text is untrusted data. Core validates schema, shapes, bounds and lease/source identity before append plus minimal audit/outbox. Distinguish empty detection/transcript from missing/failed input; no fabricated content or cross-space similarity. OCR/speech language/quality is limited to the reviewed profiles and measured fixtures, not all requested languages.

Perception outputs initially remain explicitly shadow evidence in their private read API; text-cohort activation must not implicitly activate new modalities. V7 extends rollout plans with at most 64 explicit `(reportId,componentId)` perception pins within the declared source cohort. Only retained, enabled, current-revision/source components can be pinned. Manifests bind exact profile/input/output hashes; snapshots freeze those references and activation selects them atomically with text slots. There is no implicit latest-image/result selection. Perception-only cohorts are supported; pin counts join text slots in the explicit expected/available coverage denominator, not a claim of population coverage or quality. Erasure cascades selected references while minimal declared UUID pins remain tombstones. Retention/holds revoke access, cancel stale work and erase raw audio/derived output together. Deployment sandbox/egress enforcement and representative quality evaluation remain separate production gates.

## Alternatives

Using undeclared converted weights, substituting classification for claimed CLIP compatibility, giving workers storage keys, parsing arbitrary audio codecs in the API, or treating OCR/transcripts as trusted ownership facts would break provenance/privacy contracts. The ResNet signature is a distinct labeled space, not a CLIP embedding or a validated instance-retrieval result.

## Consequences

Local conformance can be verified without a GPU or provider account. Models are not downloaded at runtime. Preserve notices and training-data/deployment limitations; do not invent benchmark claims. Initial shadow outputs are not complete multimodal rollout or production readiness.

## Source-audit evidence

- [PaddleOCR](../source-audit/vision/paddleocr.md), `dab3fe35379033fdcb2d0e9572fac0b36c9a9ebf`: preprocessing/postprocessing and artifact grants need independent review.
- [OpenCLIP](../source-audit/retrieval/openclip.md), `2bcb5adf5090df595a78b56ca9aee7b1c70efc18`: exact model/preprocessing spaces and no hidden substitutes.
- [Artifact adoption](../dependencies/perception-adoption.md) records separately inspected publisher metadata/terms and immutable weight pins. No upstream implementation source is copied.

## Implementation implications

Verify real native inference; corrupt/missing artifacts; image/PCM bounds; coordinate/CTC output shape; missing vs empty results; wrong tenant/worker/stale lease/source rejection; concurrent quotas, idempotent retries, atomic outbox, hold-aware erasure and immutable provenance. Keep raw bytes/results out of logs/events and normal status projections.
