# Source audit: paddledetection

Status: COMPLETE

Scope: architecture-relevant static review of the listed source ranges, license evidence, deployment entry points and tests/evaluation where present. No upstream tests were executed and no upstream implementation was copied into Dzoka. Unreviewed subsystems and integration acceptance work are called out explicitly.

## Repository identity

https://github.com/PaddlePaddle/PaddleDetection

## Audited branch

release/2.9. The exact commit below is authoritative; branch HEAD may move.

## Exact commit SHA

b25522a0f4bde8c80603f3ba5e3472059972e3b5

## Audit date

2026-10-26

## License

Apache-2.0. Root LICENSE text inspected at the pinned commit: Apache-2.0. Dependency, checkpoint and dataset licenses remain separate.

## Files inspected

- [LICENSE](https://github.com/PaddlePaddle/PaddleDetection/blob/b25522a0f4bde8c80603f3ba5e3472059972e3b5/LICENSE), lines 1-65; role: license.
- [deploy/python/preprocess.py](https://github.com/PaddlePaddle/PaddleDetection/blob/b25522a0f4bde8c80603f3ba5e3472059972e3b5/deploy/python/preprocess.py), lines 1-12, 139-148, 199-208, 402-411; role: implementation.
- [deploy/python/infer.py](https://github.com/PaddlePaddle/PaddleDetection/blob/b25522a0f4bde8c80603f3ba5e3472059972e3b5/deploy/python/infer.py), lines 1-12, 137-146, 159-168, 184-251, 369-378; role: implementation.
- [ppdet/modeling/tests/test_architectures.py](https://github.com/PaddlePaddle/PaddleDetection/blob/b25522a0f4bde8c80603f3ba5e3472059972e3b5/ppdet/modeling/tests/test_architectures.py), lines 1-65; role: tests.
- [requirements.txt](https://github.com/PaddlePaddle/PaddleDetection/blob/b25522a0f4bde8c80603f3ba5e3472059972e3b5/requirements.txt), lines 1-26; role: deployment.
- [ppdet/modeling/architectures/yolo.py](https://github.com/PaddlePaddle/PaddleDetection/blob/b25522a0f4bde8c80603f3ba5e3472059972e3b5/ppdet/modeling/architectures/yolo.py), lines 1-65, 86-95; role: implementation.

## Runtime architecture

Paddle detector architectures with a local Python inference/export deployment runner and configurable native predictor.

## Domain model

Boxes/class scores, box counts, optional masks, image transforms and per-stage timing; no property ownership domain.

## Storage model

Exported models, preprocessing configuration and inference arrays; the library supplies no recovery record database.

## AI and matching implementation

infer.py calls predictor.run and copies native output handles to NumPy boxes/counts/masks. Preprocessing dynamically constructs operators with eval(op_type), so model configuration is executable-trust input. Reviewed YOLO architecture uses registered backbone/neck/head components.

## Security model

Reviewed code is an offline research/library boundary, not an authenticated tenant service. Keep datasets, checkpoints and configuration trusted and isolate execution.

## API design

The inspected entry points are local Python/CLI or tensor calls; no production recovery API or authorization contract is supplied by those paths.

## Event model

No recovery-domain event stream, outbox/inbox or custody lifecycle appears in the reviewed algorithm boundary; these must be supplied by Dzoka.

## Deployment model

requirements limits NumPy<2 and OpenCV<=4.6.0 at this pin, plus native/scientific dependencies. Native Paddle/device compatibility and exported model packaging must be tested separately.

## Test coverage

Architecture tests load concrete configs and construct Trainer in test mode. They were not executed and do not measure detection accuracy, safe input handling or privacy redaction.

## Strengths

Actual detection/mask inference, exported-model serving path and permissive code license make a plausible default detector foundation.

## Weaknesses

Trusted-config eval, older constrained dependencies and a broad model ecosystem require a narrow adapter and maintained deployment environment.

## Security concerns

Do not accept model/preprocessing configuration from reports or federation peers. Parse uploads in isolated workers; validate model artifacts, image dimensions and operator allowlists.

## Scalability concerns

Batch/image-slice paths exist, but runtime buffers and output arrays need bounds. Do not run expensive detection synchronously in the report API or on every video frame across an unbounded archive.

## Licensing consequences

Preserve applicable license/copyright/NOTICE obligations for future permitted reuse. Code licensing does not establish permission for every model weight or dataset. No upstream implementation was incorporated into the core.

## Reusable components

Adopt a pinned default DetectionProvider candidate using controlled models/operator configuration; keep segmentation/tracking capabilities separately declared.

## Do not reuse

Publicly supplied eval configuration, untrusted model artifacts, coupled provider business logic and detector labels presented as instance identity.

## Architecture lessons

Permissive code licensing does not remove configuration execution risk. Freeze/validate the inference configuration at deployment boundaries.

## Disposition

ADOPT. Applies to this declared integration scope. It is not a claim of production readiness, blanket source reuse approval or completion of later integration acceptance tests.

## ADRs triggered

Proposed investigations: permissive detector/OCR defaults; optional provider licensing; trusted configuration/model artifacts; isolated asynchronous inference. This audit supplies evidence for later technical ADRs; it does not finalize the architecture.

## Unresolved questions

Model weights and dataset terms, dependency versions, resource budgets, adversarial cases and accuracy on physical-instance recovery data require integration-specific validation.
