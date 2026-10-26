# Source audit: reranking-transformer

Status: COMPLETE

Scope: architecture-relevant static review of the listed source ranges, license evidence, deployment entry points and tests/evaluation where present. No upstream tests were executed and no upstream implementation was copied into Dzoka. Unreviewed subsystems and integration acceptance work are called out explicitly.

## Repository identity

https://github.com/uvavision/RerankingTransformer

## Audited branch

main. The exact commit below is authoritative; branch HEAD may move.

## Exact commit SHA

81fca3ca9b191950f702ae712daef0400608ae3a

## Audit date

2026-10-26

## License

GPL-3.0. Root LICENSE text inspected at the pinned commit: GPL-3.0. Dependency, checkpoint and dataset licenses remain separate.

## Files inspected

- [RRT_GLD/models/transformer.py](https://github.com/uvavision/RerankingTransformer/blob/81fca3ca9b191950f702ae712daef0400608ae3a/RRT_GLD/models/transformer.py), lines 1-12, 29-38, 69-78, 84-93, 98-107; role: implementation.
- [RRT_GLD/models/matcher.py](https://github.com/uvavision/RerankingTransformer/blob/81fca3ca9b191950f702ae712daef0400608ae3a/RRT_GLD/models/matcher.py), lines 1-65; role: implementation.
- [LICENSE](https://github.com/uvavision/RerankingTransformer/blob/81fca3ca9b191950f702ae712daef0400608ae3a/LICENSE), lines 1-26, 39-48, 216-225; role: license.
- [requirements.txt](https://github.com/uvavision/RerankingTransformer/blob/81fca3ca9b191950f702ae712daef0400608ae3a/requirements.txt), lines 1-65; role: deployment.
- [RRT_SOP/eval_rerank.py](https://github.com/uvavision/RerankingTransformer/blob/81fca3ca9b191950f702ae712daef0400608ae3a/RRT_SOP/eval_rerank.py), lines 1-65; role: evaluation.

## Runtime architecture

PyTorch/Sacred retrieval experiments with separate global features, cached neighbor lists and learned pair reranking.

## Domain model

Source/target global and local descriptors, scale/segment encodings, masks, pair logits and benchmark neighbor ranks.

## Storage model

Model checkpoints, cached pickle nearest-neighbor indices and experiment configurations; no durable recovery-domain persistence.

## AI and matching implementation

MatchERT remaps global vectors, normalizes global/local descriptors, adds learned scale/segment tokens and concatenates both item feature sequences for a Transformer classifier. eval_rerank loads a checkpoint and cached shortlist before evaluation.

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

Concrete learned interaction between global/local pair evidence after retrieval, rather than an opaque keyword score.

## Weaknesses

GPL root and third-party DETR-derived file notices require attribution/license tracing. Old pinned dependencies and serialized checkpoints are not deployable-service governance.

## Security concerns

Untrusted checkpoints/serialized features and expensive media/tensor operations need a restricted worker boundary. No application tenant/ownership controls are inferred from a research implementation.

## Scalability concerns

Attention cost grows with combined descriptor tokens; a bounded candidate shortlist is essential. Cached benchmarks do not measure end-to-end online latency.

## Licensing consequences

Copyleft source must not be incorporated into the MIT core without explicit license-architecture acceptance. Provider/process separation is not asserted to eliminate obligations. No source code was incorporated.

## Reusable components

Reference coarse retrieval followed by global/local pair reranking; no GPL source incorporation into the MIT core.

## Do not reuse

Do not copy GPL implementation or notices away, accept untrusted pickle/checkpoints, run a heavy pair model over the full corpus or equate pair logits with ownership.

## Architecture lessons

Expose heavy reranking as a replaceable, budgeted provider with its own model version and evaluation contract.

## Disposition

REFERENCE. Applies to this declared integration scope. It is not a claim of production readiness, blanket source reuse approval or completion of later integration acceptance tests.

## ADRs triggered

Proposed investigations: global/local fingerprint provenance; retrieval/reranking separation; compatible vector spaces; evidence coverage and model governance. This audit supplies evidence for later technical ADRs; it does not finalize the architecture.

## Unresolved questions

Model weights and dataset terms, dependency versions, resource budgets, adversarial cases and accuracy on physical-instance recovery data require integration-specific validation.
