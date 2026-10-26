# Source audit: detectron2

Status: COMPLETE

Scope: architecture-relevant static review of the listed source ranges, license evidence, deployment entry points and tests/evaluation where present. No upstream tests were executed and no upstream implementation was copied into Dzoka. Unreviewed subsystems and integration acceptance work are called out explicitly.

## Repository identity

https://github.com/facebookresearch/detectron2

## Audited branch

main. The exact commit below is authoritative; branch HEAD may move.

## Exact commit SHA

b0fe2f0a5bb6496f65246ef35687a386a9f19202

## Audit date

2026-10-26

## License

Apache-2.0. Root LICENSE text inspected at the pinned commit: Apache-2.0. Dependency, checkpoint and dataset licenses remain separate.

## Files inspected

- [LICENSE](https://github.com/facebookresearch/detectron2/blob/b0fe2f0a5bb6496f65246ef35687a386a9f19202/LICENSE), lines 1-65; role: license.
- [detectron2/engine/defaults.py](https://github.com/facebookresearch/detectron2/blob/b0fe2f0a5bb6496f65246ef35687a386a9f19202/detectron2/engine/defaults.py), lines 1-12, 313-322, 325-354, 520-529, 630-639, 654-663; role: implementation.
- [detectron2/checkpoint/detection_checkpoint.py](https://github.com/facebookresearch/detectron2/blob/b0fe2f0a5bb6496f65246ef35687a386a9f19202/detectron2/checkpoint/detection_checkpoint.py), lines 1-12, 33-42, 71-80, 87-96, 100-109; role: implementation.
- [tests/structures/test_instances.py](https://github.com/facebookresearch/detectron2/blob/b0fe2f0a5bb6496f65246ef35687a386a9f19202/tests/structures/test_instances.py), lines 1-12, 29-38, 43-52, 74-83, 85-94, 116-125; role: tests.
- [detectron2/structures/instances.py](https://github.com/facebookresearch/detectron2/blob/b0fe2f0a5bb6496f65246ef35687a386a9f19202/detectron2/structures/instances.py), lines 1-12, 76-85, 160-177; role: implementation.
- [setup.py](https://github.com/facebookresearch/detectron2/blob/b0fe2f0a5bb6496f65246ef35687a386a9f19202/setup.py), lines 1-30, 51-60; role: implementation.

## Runtime architecture

PyTorch/C++/CUDA detection and segmentation library with DefaultPredictor convenience inference and training helpers.

## Domain model

Instances carry equal-length boxes, masks, labels and score fields tied to image geometry.

## Storage model

Trusted model checkpoints/configs and tensor output; no transactional property data or custody persistence.

## AI and matching implementation

DefaultPredictor clones config, builds/evals a model, loads weights, applies configured RGB/BGR and resizing transforms, then invokes the model. DetectionCheckpointer supports legacy pickle and torch checkpoint formats.

## Security model

Reviewed code is an offline research/library boundary, not an authenticated tenant service. Keep datasets, checkpoints and configuration trusted and isolate execution.

## API design

The inspected entry points are local Python/CLI or tensor calls; no production recovery API or authorization contract is supplied by those paths.

## Event model

No recovery-domain event stream, outbox/inbox or custody lifecycle appears in the reviewed algorithm boundary; these must be supplied by Dzoka.

## Deployment model

setup.py builds native extensions with PyTorch/CUDA/ROCm-sensitive configuration. A compatible compiler/runtime image is necessary; generic Python requirements are insufficient.

## Test coverage

Inspected Instances indexing/TorchScript field-access tests, not a full detector benchmark or security suite. None executed.

## Strengths

Structured detection/segmentation results and clear preprocessing/inference boundary, with permissive code license.

## Weaknesses

Native extension compatibility and legacy checkpoint deserialization add operational/security cost. DefaultPredictor explicitly describes convenience behavior, not a stable production service contract.

## Security concerns

Only trusted checkpoints, especially .pkl/.pyth, may reach the loader. Restrict image decoding, configs and model paths; tenant authorization belongs outside the library.

## Scalability concerns

The reviewed predictor is a one-image convenience path; batching/GPU sharing and inference quotas need a dedicated worker design and measurement.

## Licensing consequences

Preserve applicable license/copyright/NOTICE obligations for future permitted reuse. Code licensing does not establish permission for every model weight or dataset. No upstream implementation was incorporated into the core.

## Reusable components

Reference/optional alternative detector adapter after a concrete model/deployment need; do not make it an additional mandatory engine alongside the default.

## Do not reuse

Untrusted pickle weights, direct coupling to the predictor convenience class and detection score as ownership confidence.

## Architecture lessons

Standardize detector output contracts and model/preprocessing provenance before selecting optional native model runtimes.

## Disposition

REFERENCE. Applies to this declared integration scope. It is not a claim of production readiness, blanket source reuse approval or completion of later integration acceptance tests.

## ADRs triggered

Proposed investigations: permissive detector/OCR defaults; optional provider licensing; trusted configuration/model artifacts; isolated asynchronous inference. This audit supplies evidence for later technical ADRs; it does not finalize the architecture.

## Unresolved questions

Model weights and dataset terms, dependency versions, resource budgets, adversarial cases and accuracy on physical-instance recovery data require integration-specific validation.
