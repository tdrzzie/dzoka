# Source audit: paddleocr

Status: COMPLETE

Scope: architecture-relevant static review of the listed source ranges, license evidence, deployment entry points and tests/evaluation where present. No upstream tests were executed and no upstream implementation was copied into Dzoka. Unreviewed subsystems and integration acceptance work are called out explicitly.

## Repository identity

https://github.com/PaddlePaddle/PaddleOCR

## Audited branch

main. The exact commit below is authoritative; branch HEAD may move.

## Exact commit SHA

dab3fe35379033fdcb2d0e9572fac0b36c9a9ebf

## Audit date

2026-10-26

## License

Apache-2.0. Root LICENSE text inspected at the pinned commit: Apache-2.0. Dependency, checkpoint and dataset licenses remain separate.

## Files inspected

- [paddleocr/_pipelines/base.py](https://github.com/PaddlePaddle/PaddleOCR/blob/dab3fe35379033fdcb2d0e9572fac0b36c9a9ebf/paddleocr/_pipelines/base.py), lines 1-114; role: implementation.
- [LICENSE](https://github.com/PaddlePaddle/PaddleOCR/blob/dab3fe35379033fdcb2d0e9572fac0b36c9a9ebf/LICENSE), lines 1-65; role: license.
- [paddleocr/_pipelines/ocr.py](https://github.com/PaddlePaddle/PaddleOCR/blob/dab3fe35379033fdcb2d0e9572fac0b36c9a9ebf/paddleocr/_pipelines/ocr.py), lines 1-12, 158-167, 177-245, 580-589, 591-600; role: implementation.
- [tests/security/test_latexocr_pickle.py](https://github.com/PaddlePaddle/PaddleOCR/blob/dab3fe35379033fdcb2d0e9572fac0b36c9a9ebf/tests/security/test_latexocr_pickle.py), lines 1-12, 70-79, 121-130, 135-138; role: tests.
- [pyproject.toml](https://github.com/PaddlePaddle/PaddleOCR/blob/dab3fe35379033fdcb2d0e9572fac0b36c9a9ebf/pyproject.toml), lines 1-65, 77-86; role: deployment.
- [tests/pipelines/test_ocr.py](https://github.com/PaddlePaddle/PaddleOCR/blob/dab3fe35379033fdcb2d0e9572fac0b36c9a9ebf/tests/pipelines/test_ocr.py), lines 1-12, 50-59, 90-117, 119-128; role: tests.

## Runtime architecture

PaddleOCR Python/CLI wrapper creates a PaddleX OCR pipeline with configurable detection, recognition and document-orientation stages.

## Domain model

Text regions/polygons, recognized strings, confidence thresholds, word boxes, language/model configuration and inference results.

## Storage model

Model/configuration artifacts and per-input OCR results; storage, secret-identifier protection and retention are application responsibilities.

## AI and matching implementation

predict_iter delegates to the configured PaddleX pipeline with detection/recognition thresholds and orientation options. predict materializes that iterator into a list. Pipeline creation raises a RuntimeError for missing dependencies rather than substituting fabricated OCR.

## Security model

Reviewed code is an offline research/library boundary, not an authenticated tenant service. Keep datasets, checkpoints and configuration trusted and isolate execution.

## API design

The inspected entry points are local Python/CLI or tensor calls; no production recovery API or authorization contract is supplied by those paths.

## Event model

No recovery-domain event stream, outbox/inbox or custody lifecycle appears in the reviewed algorithm boundary; these must be supplied by Dzoka.

## Deployment model

pyproject pins the PaddleX major/minor range >=3.7,<3.8 and exposes a paddleocr CLI. PaddleX is a critical transitive inference boundary; its full runtime was not part of this repository review.

## Test coverage

Inspected actual OCR result-shape/language-model tests and a LaTeXOCR malicious-pickle regression test asserting no marker file is created. None executed; the latter is a test of one training-data path, not certification of all deserialization.

## Strengths

Explicit threshold/language controls, iterator interface, dependency failure reporting and source-level security regression coverage.

## Weaknesses

Heavy inference is delegated to PaddleX; OCR text may contain private serial/document values. A library code license does not establish every model-weight/dataset license.

## Security concerns

Untrusted checkpoints/serialized features and expensive media/tensor operations need a restricted worker boundary. No application tenant/ownership controls are inferred from a research implementation.

## Scalability concerns

Use iterator/batched asynchronous workers; predict converts all returned results to a list. Input resolution, document pages and model/device selection need quotas and benchmarks.

## Licensing consequences

Preserve applicable license/copyright/NOTICE obligations for future permitted reuse. Code licensing does not establish permission for every model weight or dataset. No upstream implementation was incorporated into the core.

## Reusable components

Adopt a pinned OCRProvider candidate with local model artifacts, text-region provenance and secret-field classification before indexing or federation.

## Do not reuse

Unbounded documents, automatic public indexing of raw OCR, arbitrary user model/config paths and assuming wrapper review certifies PaddleX or all OCR model families.

## Architecture lessons

OCR output is uncertain sensitive evidence with region/model provenance, not verified identity. Treat the transitive engine and model artifacts as explicit deployment acceptance work.

## Disposition

ADOPT. Applies to this declared integration scope. It is not a claim of production readiness, blanket source reuse approval or completion of later integration acceptance tests.

## ADRs triggered

Proposed investigations: permissive detector/OCR defaults; optional provider licensing; trusted configuration/model artifacts; isolated asynchronous inference. This audit supplies evidence for later technical ADRs; it does not finalize the architecture.

## Unresolved questions

Model weights and dataset terms, dependency versions, resource budgets, adversarial cases and accuracy on physical-instance recovery data require integration-specific validation.
