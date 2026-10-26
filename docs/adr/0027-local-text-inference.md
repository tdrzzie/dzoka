# 0027: Lease private text inference and append compatible fingerprint components

- Status: Accepted scoped implementation profile
- Date: 2026-10-26

## Context

M4 needs real inference without making Python a domain authority or treating unavailable modalities as negative evidence. Models with identical vector dimensions may have incompatible artifacts, tokenization, quantization or pooling. Retries must not overwrite history or publish results for revised/deleted inputs.

## Decision

The Java core owns tenant-scoped durable TEXT_EMBEDDING jobs, bounded queues, deadlines, attempt budgets and 120-second leases. Natural idempotency binds report revision and embedding space. Private Python consumers authenticate with a service bearer token carrying local `ai:work` membership; they receive only the normalized text of the claimed current report revision. No database/object-store credentials, arbitrary model URLs, cloud inference or release APIs are provided.

EmbeddingSpace binds provider, model/revision, model and tokenizer SHA-256, preprocessing, dimensions, metric, normalization and configuration digest. Configuration additionally binds reviewed runtime and provider/integrity source hashes. Validate finite normalized vector shape, exact provenance, lease owner/token/expiry and current source before an append-only component and minimal audit/outbox commit. A repeated identical completion is idempotent; changed or stale completion cannot overwrite it. Report revision changes remove older components from the current projection; immutable history remains until governed erasure.

Approve only the reviewed local quantized multilingual MiniLM CPU text profile in [adoption evidence](../dependencies/ai-text-adoption.md). No pickle, remote model code, runtime download or fallback vectors. Missing input is UNAVAILABLE; provider failure retains durable work up to a bounded budget and then FAILED. Each approved space remains separate; registration/enabling is an explicit operator action. This delivers the text component, not image/OCR/global-local fingerprints or a completed shadow-promotion/rollback migration system.

Erasure uses a trusted core maintenance process with `reports:purge`; legal holds prevent component deletion. Current read/completion paths recheck source expiry/deletion. Operators must run raw/media/AI erasure together and reconcile backups/projections before restored data is served.

## Alternatives

In-process Java native inference would couple model and API runtime trust. Python SQL access would bypass core authorization. Dimension-only compatibility, unreviewed artifact downloads and heuristic substitutes would violate provenance and explicit-unavailable invariants. The first slice uses a private leased consumer rather than adding a FastAPI server with unused public endpoints.

## Consequences

Real model conformance is separate from synthetic lifecycle tests. The reviewed native wheel profile is Windows CPython 3.14; other platforms require their own adoption/conformance. The publisher's multilingual label and nonconstant outputs are not Shona/Ndebele coverage, calibrated similarity, ownership evidence or a recovery accuracy result. Remaining M4 modalities, migration activation/rollback, distributed scheduling and production resource/advisory assessment remain explicit work.

## Source-audit evidence

- [OpenCLIP](../source-audit/retrieval/openclip.md), `2bcb5adf5090df595a78b56ca9aee7b1c70efc18`: artifact/preprocessing compatibility is distinct from dimensions.
- [OpenVisualSearch](../source-audit/retrieval/openvisualsearch.md), `134dbb376bd61eac77bc388ef3531edd87fd5362`: derived representations need coherent versions and input provenance.
- [ADR 0009](0009-model-provenance.md), [ADR 0015](0015-python-ai-boundary.md) and the separate adoption record govern this independently written provider. No upstream inference implementation was copied into the core.

## Implementation implications

Test transactional enqueue/results, concurrent idempotency/quotas, private worker scope, stale/wrong-tenant/worker/result metadata, same-dimension incompatible spaces, deadlines, retry exhaustion, missing/corrupt artifacts, real inference, revision changes and hold/deletion erasure. Keep matching, ownership and release authority outside this component.
