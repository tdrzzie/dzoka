# Source audit: patchwise-retrieval

Status: COMPLETE

Scope: architecture-relevant static review of the listed source ranges, license evidence, deployment entry points and tests/evaluation where present. No upstream tests were executed and no upstream implementation was copied into Dzoka. Unreviewed subsystems and integration acceptance work are called out explicitly.

## Repository identity

https://github.com/kaist-ami/Patchwise-Retrieval

## Audited branch

main. The exact commit below is authoritative; branch HEAD may move.

## Exact commit SHA

d593101957933fcd574649fcb3b0af02c7295c11

## Audit date

2026-10-26

## License

UNRESOLVED. No root project LICENSE/COPYING in the complete pinned tree; missing licensing is a completed audit finding, not permission to copy. The bundled ilias/LICENSE applies to ILIAS material and does not establish permission for the main project.

## Files inspected

- [src/eval_retrieval.py](https://github.com/kaist-ami/Patchwise-Retrieval/blob/d593101957933fcd574649fcb3b0af02c7295c11/src/eval_retrieval.py), lines 1-8; role: evaluation.
- [src/pyramid_embedding.py](https://github.com/kaist-ami/Patchwise-Retrieval/blob/d593101957933fcd574649fcb3b0af02c7295c11/src/pyramid_embedding.py), lines 1-21, 50-88, 101-110, 115-150; role: implementation.
- [src/retrieval.py](https://github.com/kaist-ami/Patchwise-Retrieval/blob/d593101957933fcd574649fcb3b0af02c7295c11/src/retrieval.py), lines 1-12, 54-63, 193-202, 338-347, 350-478; role: implementation.
- [ilias/LICENSE](https://github.com/kaist-ami/Patchwise-Retrieval/blob/d593101957933fcd574649fcb3b0af02c7295c11/ilias/LICENSE), lines 1-21; role: license.
- [src/metrics.py](https://github.com/kaist-ami/Patchwise-Retrieval/blob/d593101957933fcd574649fcb3b0af02c7295c11/src/metrics.py), lines 1-65; role: evaluation.
- [requirements.txt](https://github.com/kaist-ami/Patchwise-Retrieval/blob/d593101957933fcd574649fcb3b0af02c7295c11/requirements.txt), lines 1-12; role: deployment.

## Runtime architecture

Offline PyTorch regional retrieval evaluator with multi-scale patch extraction and optional FAISS PQ/IVFPQ indexing.

## Domain model

Patch features and locations, image IDs, query/gallery mappings and localization-aware rankings.

## Storage model

Cached feature/index artifacts and local ILIAS query/gallery metadata. This is not a database abstraction or tenant-scoped case store.

## AI and matching implementation

PatchEmbeddingModule unfolds overlapping crops, resizes and encodes them, then normalizes each feature. Pyramid code repeats across scales. Retrieval maps patch indices to images and takes maximal patch similarity; a direct path computes query-by-gallery matrix products.

## Security model

Reviewed code is an offline research/library boundary, not an authenticated tenant service. Keep datasets, checkpoints and configuration trusted and isolate execution.

## API design

The inspected entry points are local Python/CLI or tensor calls; no production recovery API or authorization contract is supplied by those paths.

## Event model

No recovery-domain event stream, outbox/inbox or custody lifecycle appears in the reviewed algorithm boundary; these must be supplied by Dzoka.

## Deployment model

Reviewed dependency files describe an offline Python environment. GPU/native runtime compatibility, reproducible images and model artifact provisioning need separate integration validation.

## Test coverage

Inspected evaluation code is recorded in the evidence manifest. It was not executed and does not establish unit-test coverage, deployment security or Dzoka retrieval accuracy.

## Strengths

Retains regional evidence/locations, supports compression experiments and implements mAP/Recall-style evaluation independently of a UI.

## Weaknesses

The only inspected license is ilias/LICENSE for the bundled ILIAS material, not a root project grant. CUDA events/autocast assumptions and max-patch aggregation require careful calibration.

## Security concerns

Untrusted checkpoints/serialized features and expensive media/tensor operations need a restricted worker boundary. No application tenant/ownership controls are inferred from a research implementation.

## Scalability concerns

Patch multiplication increases vector count and memory. Direct full-gallery matrix products and broad sorting are offline evaluation patterns, not bounded online retrieval.

## Licensing consequences

No direct project-code reuse permitted by this audit. Study concepts and independently implement Dzoka contracts; seek explicit permission before any future copying.

## Reusable components

Reference multi-scale regional identity and patch provenance; independently implement provider contracts without copying unlicensed project code.

## Do not reuse

Do not propagate the nested MIT license to the main project, adopt dense all-gallery evaluation as online search or treat one strong patch as ownership proof.

## Architecture lessons

Keep patch-to-image/region provenance and evaluate repeated-texture hard negatives. Local evidence complements rather than replaces global identity.

## Disposition

REFERENCE. Applies to this declared integration scope. It is not a claim of production readiness, blanket source reuse approval or completion of later integration acceptance tests.

## ADRs triggered

Proposed investigations: global/local fingerprint provenance; retrieval/reranking separation; compatible vector spaces; evidence coverage and model governance. This audit supplies evidence for later technical ADRs; it does not finalize the architecture.

## Unresolved questions

Model weights and dataset terms, dependency versions, resource budgets, adversarial cases and accuracy on physical-instance recovery data require integration-specific validation.
