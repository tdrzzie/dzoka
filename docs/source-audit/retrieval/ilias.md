# Source audit: ilias

Status: COMPLETE

Scope: architecture-relevant static review of the listed source ranges, license evidence, deployment entry points and tests/evaluation where present. No upstream tests were executed and no upstream implementation was copied into Dzoka. Unreviewed subsystems and integration acceptance work are called out explicitly.

## Repository identity

https://github.com/ilias-vrg/ilias

## Audited branch

main. The exact commit below is authoritative; branch HEAD may move.

## Exact commit SHA

208815da09c31f728ca8427520eb4898c5156273

## Audit date

2026-10-26

## License

MIT. Root LICENSE text inspected at the pinned commit: MIT. Dependency, checkpoint and dataset licenses remain separate.

## Files inspected

- [LICENSE](https://github.com/ilias-vrg/ilias/blob/208815da09c31f728ca8427520eb4898c5156273/LICENSE), lines 1-21; role: license.
- [knn_search/src/searcher.py](https://github.com/ilias-vrg/ilias/blob/208815da09c31f728ca8427520eb4898c5156273/knn_search/src/searcher.py), lines 1-12, 38-55, 75-130, 173-197; role: implementation.
- [feature_extraction/src/model/extractors/openclip.py](https://github.com/ilias-vrg/ilias/blob/208815da09c31f728ca8427520eb4898c5156273/feature_extraction/src/model/extractors/openclip.py), lines 1-65; role: implementation.
- [knn_search/requirements.txt](https://github.com/ilias-vrg/ilias/blob/208815da09c31f728ca8427520eb4898c5156273/knn_search/requirements.txt), lines 1-6; role: deployment.
- [evaluation/evaluate.py](https://github.com/ilias-vrg/ilias/blob/208815da09c31f728ca8427520eb4898c5156273/evaluation/evaluate.py), lines 1-12, 17-35, 57-66; role: evaluation.
- [local_reranking/src/ames.py](https://github.com/ilias-vrg/ilias/blob/208815da09c31f728ca8427520eb4898c5156273/local_reranking/src/ames.py), lines 1-12, 51-96, 117-126, 188-197; role: implementation.

## Runtime architecture

Separate feature extraction, chunked k-NN search, local-feature reranking and dataset evaluation Python programs.

## Domain model

Instance-level image queries/gallery IDs, global/local features, shortlists, model adapters and ground-truth rankings.

## Storage model

Feature datasets, per-query rankings and JSON/pickle evaluation inputs rather than a transactional object registry.

## AI and matching implementation

Searcher calls faiss.knn or knn_gpu over successive feature chunks and merges a ResultHeap, optionally repeating after query expansion. OpenCLIP extractor explicitly supports RN50.openai/head pooling. AMES remaps and concatenates masked local features for attention-based pair comparison.

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

Instance-level evaluation, explicit distance orientation, bounded chunk memory and separate local reranking form useful research contracts.

## Weaknesses

Chunked exact search still touches gallery features; the OpenCLIP adapter is intentionally narrower than the full upstream library. Evaluation can load pickle input.

## Security concerns

Untrusted checkpoints/serialized features and expensive media/tensor operations need a restricted worker boundary. No application tenant/ownership controls are inferred from a research implementation.

## Scalability concerns

Chunking bounds memory, not total gallery comparisons. Query expansion adds another search; shortlist reranking cost depends on local token count.

## Licensing consequences

Preserve applicable license/copyright/NOTICE obligations for future permitted reuse. Code licensing does not establish permission for every model weight or dataset. No upstream implementation was incorporated into the core.

## Reusable components

Adapt evaluation/shortlist/local-feature contracts under MIT notices. Do not adopt the research programs as the online transactional/search service.

## Do not reuse

Untrusted pickle rankings, whole-gallery exact search as default online architecture, unsupported-model assumptions and research metrics as calibrated ownership proof.

## Architecture lessons

Distinguish same-instance retrieval from category recognition and distinguish memory-bounded offline exact search from indexed online serving.

## Disposition

ADAPT. Applies to this declared integration scope. It is not a claim of production readiness, blanket source reuse approval or completion of later integration acceptance tests.

## ADRs triggered

Proposed investigations: global/local fingerprint provenance; retrieval/reranking separation; compatible vector spaces; evidence coverage and model governance. This audit supplies evidence for later technical ADRs; it does not finalize the architecture.

## Unresolved questions

Model weights and dataset terms, dependency versions, resource budgets, adversarial cases and accuracy on physical-instance recovery data require integration-specific validation.
