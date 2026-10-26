# Source audit: ultralytics

Status: COMPLETE

Scope: architecture-relevant static review of the listed source ranges, license evidence, deployment entry points and tests/evaluation where present. No upstream tests were executed and no upstream implementation was copied into Dzoka. Unreviewed subsystems and integration acceptance work are called out explicitly.

## Repository identity

https://github.com/ultralytics/ultralytics

## Audited branch

main. The exact commit below is authoritative; branch HEAD may move.

## Exact commit SHA

739d9c72b74b498fa674a477dc6a8190e2373350

## Audit date

2026-10-26

## License

AGPL-3.0. Root LICENSE text inspected at the pinned commit: AGPL-3.0. Dependency, checkpoint and dataset licenses remain separate.

## Files inspected

- [LICENSE](https://github.com/ultralytics/ultralytics/blob/739d9c72b74b498fa674a477dc6a8190e2373350/LICENSE), lines 1-18, 26-35, 51-60, 204-213; role: license.
- [ultralytics/engine/results.py](https://github.com/ultralytics/ultralytics/blob/739d9c72b74b498fa674a477dc6a8190e2373350/ultralytics/engine/results.py), lines 1-12, 54-63, 331-340, 536-545, 981-990, 1472-1481; role: implementation.
- [ultralytics/engine/predictor.py](https://github.com/ultralytics/ultralytics/blob/739d9c72b74b498fa674a477dc6a8190e2373350/ultralytics/engine/predictor.py), lines 1-12, 267-276, 298-365, 370-410, 429-438, 477-486; role: implementation.
- [ultralytics/trackers/track.py](https://github.com/ultralytics/ultralytics/blob/739d9c72b74b498fa674a477dc6a8190e2373350/ultralytics/trackers/track.py), lines 1-65; role: implementation.
- [docker/Dockerfile-cpu](https://github.com/ultralytics/ultralytics/blob/739d9c72b74b498fa674a477dc6a8190e2373350/docker/Dockerfile-cpu), lines 1-19; role: deployment.
- [tests/test_python.py](https://github.com/ultralytics/ultralytics/blob/739d9c72b74b498fa674a477dc6a8190e2373350/tests/test_python.py), lines 1-12, 56-65, 67-76, 88-97, 104-113, 126-135; role: tests.

## Runtime architecture

Python inference engine handles images/video/streams and registers multiple tracking backends through prediction callbacks.

## Domain model

Results contain boxes/masks/classes/confidence and optional track IDs; these are observations, not verified property identities.

## Storage model

Model artifacts, in-memory prediction/tracker state and optional rendered output files. No recovery-tenancy or immutable custody store.

## AI and matching implementation

stream_inference loads sources, preprocesses, invokes real model inference, postprocesses and yields results. Tracking config selects ByteTrack/BoT-SORT and other implementations. A lock protects predictor state; non-stream calls can accumulate results.

## Security model

Reviewed code is an offline research/library boundary, not an authenticated tenant service. Keep datasets, checkpoints and configuration trusted and isolate execution.

## API design

The inspected entry points are local Python/CLI or tensor calls; no production recovery API or authorization contract is supplied by those paths.

## Event model

No recovery-domain event stream, outbox/inbox or custody lifecycle appears in the reviewed algorithm boundary; these must be supplied by Dzoka.

## Deployment model

Inspected CPU Dockerfile inherits a mutable latest-python upstream image. Root/source headers explicitly use AGPL-3.0; this review establishes no alternate commercial permission.

## Test coverage

Selected test_python tests exercise prefetch failure propagation, dataloader worker caps and deterministic seeds. None executed; these are not detector-accuracy or institutional privacy tests.

## Strengths

Real streamed inference, structured outputs, source lifecycle cleanup and replaceable tracker selection.

## Weaknesses

AGPL obligations conflict with casual incorporation into a permissive core. Broad input-source features and optional output writing widen the operational boundary.

## Security concerns

Only authorized camera/media sources may reach an optional edge adapter. Disable arbitrary URL/device/screenshot inputs, limit artifacts and review any chosen license arrangement explicitly.

## Scalability concerns

Prefer streamed results for long sequences; GPU locks, prefetch, decoder threads and tracking histories require quotas/cancellation. Upstream defaults are not Dzoka SLO evidence.

## Licensing consequences

Copyleft source must not be incorporated into the MIT core without explicit license-architecture acceptance. Provider/process separation is not asserted to eliminate obligations. No source code was incorporated.

## Reusable components

Reference optional detector/tracker provider contracts; retain a permissive default and avoid mandatory Ultralytics imports in the core.

## Do not reuse

AGPL source copying into MIT core without explicit compatibility acceptance, mutable latest images and unrestricted surveillance/source ingestion.

## Architecture lessons

An optional provider is a licensing and deployment boundary; process separation alone must not be asserted to remove license obligations.

## Disposition

REFERENCE. Applies to this declared integration scope. It is not a claim of production readiness, blanket source reuse approval or completion of later integration acceptance tests.

## ADRs triggered

Proposed investigations: permissive detector/OCR defaults; optional provider licensing; trusted configuration/model artifacts; isolated asynchronous inference. This audit supplies evidence for later technical ADRs; it does not finalize the architecture.

## Unresolved questions

Model weights and dataset terms, dependency versions, resource budgets, adversarial cases and accuracy on physical-instance recovery data require integration-specific validation.
