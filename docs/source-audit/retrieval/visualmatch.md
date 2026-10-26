# Source audit: visualmatch

Status: COMPLETE

Scope: architecture-relevant static review of the listed source ranges, license evidence, deployment entry points and tests/evaluation where present. No upstream tests were executed and no upstream implementation was copied into Dzoka. Unreviewed subsystems and integration acceptance work are called out explicitly.

## Repository identity

https://github.com/soham-k-sinha/visualmatch

## Audited branch

main. The exact commit below is authoritative; branch HEAD may move.

## Exact commit SHA

941c69fa7d82c7fd5f570ad7392833b8d0b83791

## Audit date

2026-10-26

## License

UNRESOLVED. No root project LICENSE/COPYING in the complete pinned tree; missing licensing is a completed audit finding, not permission to copy.

## Files inspected

- [src/embed.py](https://github.com/soham-k-sinha/visualmatch/blob/941c69fa7d82c7fd5f570ad7392833b8d0b83791/src/embed.py), lines 1-65; role: implementation.
- [src/search.py](https://github.com/soham-k-sinha/visualmatch/blob/941c69fa7d82c7fd5f570ad7392833b8d0b83791/src/search.py), lines 1-65; role: implementation.
- [src/rerank.py](https://github.com/soham-k-sinha/visualmatch/blob/941c69fa7d82c7fd5f570ad7392833b8d0b83791/src/rerank.py), lines 1-12, 40-49, 67-76; role: implementation.
- [api/app.py](https://github.com/soham-k-sinha/visualmatch/blob/941c69fa7d82c7fd5f570ad7392833b8d0b83791/api/app.py), lines 1-51; role: implementation.
- [requirements.txt](https://github.com/soham-k-sinha/visualmatch/blob/941c69fa7d82c7fd5f570ad7392833b8d0b83791/requirements.txt), lines 1-65; role: deployment.
- [src/evaluate.py](https://github.com/soham-k-sinha/visualmatch/blob/941c69fa7d82c7fd5f570ad7392833b8d0b83791/src/evaluate.py), lines 1-65; role: evaluation.

## Runtime architecture

Offline OpenCLIP/FAISS retrieval with a FastAPI wrapper and a separate cross-model reranker.

## Domain model

Fashion dataset item IDs, category/color metadata, global image/text vectors and ranked retrieval results; no claim or custody entities.

## Storage model

NumPy embedding matrix, JSON metadata and model-specific FAISS index paths. Explicit model choices exist but no per-artifact immutable model/preprocessing provenance contract was established.

## AI and matching implementation

embed.py performs actual OpenCLIP inference and normalization on DeepFashion images. SearchEngine encodes queries and uses FAISS; Reranker re-encodes a shortlist with a stronger model. Evaluation implements Recall@K, reciprocal rank and NDCG.

## Security model

FastAPI wrapper has wildcard CORS and no auth dependency in inspected routes. Upload handlers read complete media into memory and decode it directly.

## API design

Text/image/composed search endpoints return top-10 results. api/app.py constructs SearchEngine() without the required model_name argument visible in search.py, a static startup mismatch; no runtime test was performed.

## Event model

No recovery-domain event stream, outbox/inbox or custody lifecycle appears in the reviewed algorithm boundary; these must be supplied by Dzoka.

## Deployment model

Requirements are a large pinned environment snapshot. API startup consistency and missing model/index artifacts must be fixed before any service use.

## Test coverage

Inspected evaluation code is recorded in the evidence manifest. It was not executed and does not establish unit-test coverage, deployment security or Dzoka retrieval accuracy.

## Strengths

Clear retrieval/reranking separation, actual embeddings and explicit evaluation metrics.

## Weaknesses

Wrapper/constructor mismatch; FAISS metadata indexing does not guard negative sentinel indices in the reviewed result loop; dataset identity is not ownership.

## Security concerns

Untrusted checkpoints/serialized features and expensive media/tensor operations need a restricted worker boundary. No application tenant/ownership controls are inferred from a research implementation.

## Scalability concerns

FAISS can retrieve bounded neighbors, while shortlist re-encoding adds model cost. The offline dataset pipeline and top-K outputs are not a production throughput benchmark.

## Licensing consequences

No direct project-code reuse permitted by this audit. Study concepts and independently implement Dzoka contracts; seek explicit permission before any future copying.

## Reusable components

Reference cross-model reranking and evaluation concepts, independently implemented behind Dzoka providers.

## Do not reuse

No project code copying without license. Exclude unauthenticated raw-upload service, constructor mismatch and dataset-level scores presented as ownership confidence.

## Architecture lessons

Retrieval and heavy reranking should have explicit separate budgets and consistent model/index configuration; smoke-test service entry points, not just notebooks.

## Disposition

REFERENCE. Applies to this declared integration scope. It is not a claim of production readiness, blanket source reuse approval or completion of later integration acceptance tests.

## ADRs triggered

Proposed investigations: global/local fingerprint provenance; retrieval/reranking separation; compatible vector spaces; evidence coverage and model governance. This audit supplies evidence for later technical ADRs; it does not finalize the architecture.

## Unresolved questions

Model weights and dataset terms, dependency versions, resource budgets, adversarial cases and accuracy on physical-instance recovery data require integration-specific validation.
