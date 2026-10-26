# Source audit: deepsort

Status: COMPLETE

Scope: architecture-relevant static review of the listed source ranges, license evidence, deployment entry points and tests/evaluation where present. No upstream tests were executed and no upstream implementation was copied into Dzoka. Unreviewed subsystems and integration acceptance work are called out explicitly.

## Repository identity

https://github.com/nwojke/deep_sort

## Audited branch

master. The exact commit below is authoritative; branch HEAD may move.

## Exact commit SHA

f08cf1dc470eeb1cd2add1cbf077d95ac6c48aab

## Audit date

2026-10-26

## License

GPL-3.0. Root LICENSE text inspected at the pinned commit: GPL-3.0. Dependency, checkpoint and dataset licenses remain separate.

## Files inspected

- [deep_sort/tracker.py](https://github.com/nwojke/deep_sort/blob/f08cf1dc470eeb1cd2add1cbf077d95ac6c48aab/deep_sort/tracker.py), lines 1-12, 48-138; role: implementation.
- [deep_sort/nn_matching.py](https://github.com/nwojke/deep_sort/blob/f08cf1dc470eeb1cd2add1cbf077d95ac6c48aab/deep_sort/nn_matching.py), lines 1-12, 29-38, 76-85, 90-99, 105-114, 126-177; role: implementation.
- [LICENSE](https://github.com/nwojke/deep_sort/blob/f08cf1dc470eeb1cd2add1cbf077d95ac6c48aab/LICENSE), lines 1-26, 39-48, 216-225; role: license.
- [evaluate_motchallenge.py](https://github.com/nwojke/deep_sort/blob/f08cf1dc470eeb1cd2add1cbf077d95ac6c48aab/evaluate_motchallenge.py), lines 1-12, 31-40, 52-54; role: evaluation.
- [deep_sort/track.py](https://github.com/nwojke/deep_sort/blob/f08cf1dc470eeb1cd2add1cbf077d95ac6c48aab/deep_sort/track.py), lines 1-12, 110-119, 124-133; role: implementation.
- [requirements.txt](https://github.com/nwojke/deep_sort/blob/f08cf1dc470eeb1cd2add1cbf077d95ac6c48aab/requirements.txt), lines 1-3; role: deployment.

## Runtime architecture

Canonical nwojke/deep_sort NumPy/SciPy/OpenCV tracking code with an offline MOTChallenge runner.

## Domain model

Tentative/confirmed/deleted tracks, Kalman state, appearance feature galleries, track IDs and unmatched detections.

## Storage model

Per-track in-memory samples; optional appearance budgets prune old samples and remove inactive targets. MOT evaluation writes local sequence outputs.

## AI and matching implementation

Tracker predicts state, runs appearance-cost matching with Kalman gating, then IoU matching for remaining tracks. The distance metric selects nearest cosine/Euclidean feature samples; feature history is updated after association.

## Security model

Reviewed code is an offline research/library boundary, not an authenticated tenant service. Keep datasets, checkpoints and configuration trusted and isolate execution.

## API design

The inspected entry points are local Python/CLI or tensor calls; no production recovery API or authorization contract is supplied by those paths.

## Event model

No recovery-domain event stream, outbox/inbox or custody lifecycle appears in the reviewed algorithm boundary; these must be supplied by Dzoka.

## Deployment model

Simple unpinned requirements for NumPy/OpenCV/SciPy; appearance descriptor/model generation is a separate boundary and was not certified here.

## Test coverage

Inspected MOTChallenge evaluation CLI and thresholds/budget arguments. No standalone unit-test suite found in the pinned tree; no evaluation executed.

## Strengths

Clear motion/appearance association and explicit track lifecycle/sample-budget concepts.

## Weaknesses

The actual root license is GPL-3.0, resolving the supplied unspecified-license note. Track continuity is uncertain and cannot prove object ownership or cross-camera identity.

## Security concerns

Untrusted checkpoints/serialized features and expensive media/tensor operations need a restricted worker boundary. No application tenant/ownership controls are inferred from a research implementation.

## Scalability concerns

Association costs grow with active tracks/detections and sample history; enforce scene/time bounds and appearance budgets at the authorized edge.

## Licensing consequences

Copyleft source must not be incorporated into the MIT core without explicit license-architecture acceptance. Provider/process separation is not asserted to eliminate obligations. No source code was incorporated.

## Reusable components

Reference tracker lifecycle and association concepts only; independently implement or select a license-compatible provider.

## Do not reuse

No GPL tracker code copying into the MIT core. Exclude track ID as persistent physical identity and unbounded/person-centric cross-camera tracking.

## Architecture lessons

Tracking remains an authorized observation subsystem with explicit uncertainty, bounded retention and a separate licensing review.

## Disposition

REFERENCE. Applies to this declared integration scope. It is not a claim of production readiness, blanket source reuse approval or completion of later integration acceptance tests.

## ADRs triggered

Proposed investigations: permissive detector/OCR defaults; optional provider licensing; trusted configuration/model artifacts; isolated asynchronous inference. This audit supplies evidence for later technical ADRs; it does not finalize the architecture.

## Unresolved questions

Model weights and dataset terms, dependency versions, resource budgets, adversarial cases and accuracy on physical-instance recovery data require integration-specific validation.
