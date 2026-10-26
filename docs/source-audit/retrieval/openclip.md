# Source audit: openclip

Status: COMPLETE

Scope: architecture-relevant static review of the listed source ranges, license evidence, deployment entry points and tests/evaluation where present. No upstream tests were executed and no upstream implementation was copied into Dzoka. Unreviewed subsystems and integration acceptance work are called out explicitly.

## Repository identity

https://github.com/mlfoundations/open_clip

## Audited branch

main. The exact commit below is authoritative; branch HEAD may move.

## Exact commit SHA

2bcb5adf5090df595a78b56ca9aee7b1c70efc18

## Audit date

2026-10-26

## License

MIT. Root LICENSE text inspected at the pinned commit: MIT. Dependency, checkpoint and dataset licenses remain separate.

## Files inspected

- [LICENSE](https://github.com/mlfoundations/open_clip/blob/2bcb5adf5090df595a78b56ca9aee7b1c70efc18/LICENSE), lines 1-23; role: license.
- [src/open_clip/model.py](https://github.com/mlfoundations/open_clip/blob/2bcb5adf5090df595a78b56ca9aee7b1c70efc18/src/open_clip/model.py), lines 1-12, 63-72, 131-140, 201-210, 275-284, 362-371, 484-515; role: implementation.
- [src/open_clip/factory.py](https://github.com/mlfoundations/open_clip/blob/2bcb5adf5090df595a78b56ca9aee7b1c70efc18/src/open_clip/factory.py), lines 1-65, 234-254, 725-780; role: implementation.
- [pyproject.toml](https://github.com/mlfoundations/open_clip/blob/2bcb5adf5090df595a78b56ca9aee7b1c70efc18/pyproject.toml), lines 1-65; role: deployment.
- [tests/test_inference_simple.py](https://github.com/mlfoundations/open_clip/blob/2bcb5adf5090df595a78b56ca9aee7b1c70efc18/tests/test_inference_simple.py), lines 1-40; role: tests.
- [src/open_clip/pretrained.py](https://github.com/mlfoundations/open_clip/blob/2bcb5adf5090df595a78b56ca9aee7b1c70efc18/src/open_clip/pretrained.py), lines 1-12, 130-139, 147-156, 181-190, 192-201, 238-247, 818-863; role: implementation.

## Runtime architecture

PyTorch library builds paired image/text towers, tokenizers, transforms and pretrained checkpoints from model configuration.

## Domain model

Architecture configuration, tokenizer, preprocessing, normalized/non-normalized feature tensors, logits and checkpoint identity.

## Storage model

Model configuration package plus downloaded checkpoint cache. Dzoka must persist model-space identity beside embeddings rather than relying on cache filenames.

## AI and matching implementation

encode_image and encode_text have explicit normalization switches; get_logits normalizes both. Factory load_state_dict defaults to weights_only and supports safetensors. The factory can warn and return random initialization unless require_pretrained is enforced.

## Security model

Reviewed code is an offline research/library boundary, not an authenticated tenant service. Keep datasets, checkpoints and configuration trusted and isolate execution.

## API design

The inspected entry points are local Python/CLI or tensor calls; no production recovery API or authorization contract is supplied by those paths.

## Event model

No recovery-domain event stream, outbox/inbox or custody lifecycle appears in the reviewed algorithm boundary; these must be supplied by Dzoka.

## Deployment model

pyproject requires torch>=2.6 and timm>=1.0.29 at this revision. Local checkpoint provisioning and pinned runtime compatibility are required; a library installation alone does not enable a valid embedding provider.

## Test coverage

Inspected parameterized inference test encodes an image and three texts and asserts output probabilities. Not run; this small test is not same-instance or multilingual recovery validation.

## Strengths

Real paired multimodal inference, consistent transform construction, explicit normalization and safer checkpoint loading defaults.

## Weaknesses

Code license does not license all pretrained weights. Download checksum checks are URL-dependent; not every URL establishes a full content hash. Random/partial initialization paths are inappropriate production defaults.

## Security concerns

Untrusted checkpoints/serialized features and expensive media/tensor operations need a restricted worker boundary. No application tenant/ownership controls are inferred from a research implementation.

## Scalability concerns

Batch/tensor/GPU resources need worker quotas and deployment benchmarking. Model choice changes dimensions and compute; identical dimension alone does not identify a compatible embedding space.

## Licensing consequences

Preserve applicable license/copyright/NOTICE obligations for future permitted reuse. Code licensing does not establish permission for every model weight or dataset. No upstream implementation was incorporated into the core.

## Reusable components

Adopt a pinned library behind ImageEmbeddingProvider/TextEmbeddingProvider with require_pretrained, trusted artifacts, explicit eval/inference mode and complete provenance.

## Do not reuse

Randomly initialized serving, unpinned remote weights, arbitrary checkpoint paths, mixing model/preprocess versions and logits treated as recovery authorization.

## Architecture lessons

Provider startup must prove the expected weights/configuration loaded; code, model weights and dataset licenses are independent review objects.

## Disposition

ADOPT. Applies to this declared integration scope. It is not a claim of production readiness, blanket source reuse approval or completion of later integration acceptance tests.

## ADRs triggered

Proposed investigations: global/local fingerprint provenance; retrieval/reranking separation; compatible vector spaces; evidence coverage and model governance. This audit supplies evidence for later technical ADRs; it does not finalize the architecture.

## Unresolved questions

Model weights and dataset terms, dependency versions, resource budgets, adversarial cases and accuracy on physical-instance recovery data require integration-specific validation.
