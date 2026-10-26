# 0016: Default to permissive detector implementations

- Status: Accepted design; implementation and runtime dependency approval remain separate
- Date: 2026-10-26
- Prerequisite: mandatory source audit gate passed before these decisions

## Context

PaddleDetection and Detectron2 have permissive code grants; Ultralytics is AGPL and canonical DeepSORT is GPL. Model weights remain separately licensed.

## Decision

Design PaddleDetection as the initial detector provider candidate and Detectron2 as optional. Require separately reviewed artifacts and measured suitability before activation. Do not copy GPL/AGPL/unlicensed tracker or detector source into the MIT core. Optional restrictive providers need a documented distribution/license review; process separation is not automatic exemption.

## Alternatives

Defaulting to a restrictive detector without a license plan conflicts with the requested permissive core; permissive code alone does not clear weights.

## Consequences

Select tracking independently through reviewed interfaces. Do not claim a production detector/tracker exists until artifacts, dependencies and evaluation pass.

## Source-audit evidence

The specification and [AGENTS.md](../../AGENTS.md) govern invariants and required stack preferences. These static reviews inform the decision; they do not establish benchmarks or full dependency certification.

- [paddledetection](../source-audit/vision/paddledetection.md) — `b25522a0f4bde8c80603f3ba5e3472059972e3b5`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [detectron2](../source-audit/vision/detectron2.md) — `b0fe2f0a5bb6496f65246ef35687a386a9f19202`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [ultralytics](../source-audit/vision/ultralytics.md) — `739d9c72b74b498fa674a477dc6a8190e2373350`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [deepsort](../source-audit/vision/deepsort.md) — `f08cf1dc470eeb1cd2add1cbf077d95ac6c48aab`; inspected ranges and limitations are in the canonical audit and evidence manifest.

## Implementation implications

Maintain artifact/license manifest and evaluate actual detection/tracking data, resource limits, malformed media and model-load failures.

The [architecture baseline](../architecture/baseline.md) defines ownership and execution boundaries. Complete the relevant [dependency adoption review](../architecture/dependency-adoption.md) before installing runtime dependencies. Acceptance scenarios here are requirements, not tests already executed.
