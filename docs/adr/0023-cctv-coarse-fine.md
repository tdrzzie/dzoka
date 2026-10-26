# 0023: Search authorized CCTV from coarse to fine

- Status: Accepted design; implementation and runtime dependency approval remain separate
- Date: 2026-10-26
- Prerequisite: mandatory source audit gate passed before these decisions

## Context

CCTV tracking is optional evidence with gaps and occlusion, not custody or identity proof.

## Decision

Require an investigation grant binding tenant, cameras, purpose, time window, permissions, retention and reviewer. Start with time/camera narrowing, coarse sampling and detection; refine candidate windows with tracking/local evidence. Preserve gaps and uncertain last-seen intervals. Export minimal redacted evidence references and cancel work when grants expire.

## Alternatives

Always-on exhaustive processing and facial recognition exceed the required recovery purpose and create unnecessary cost/privacy risk.

## Consequences

No default facial recognition. Local video stays on authorized edge storage. Temporary access URLs and job budgets are restricted; track disappearance is a hypothesis, not proof of theft or custodian identity.

## Source-audit evidence

The specification and [AGENTS.md](../../AGENTS.md) govern invariants and required stack preferences. These static reviews inform the decision; they do not establish benchmarks or full dependency certification.

- [cctv-theft-finder](../source-audit/cctv/cctv-theft-finder.md) — `c3af22b1d6239424459a0c568f3a575de49572d6`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [deepsort](../source-audit/vision/deepsort.md) — `f08cf1dc470eeb1cd2add1cbf077d95ac6c48aab`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [ultralytics](../source-audit/vision/ultralytics.md) — `739d9c72b74b498fa674a477dc6a8190e2373350`; inspected ranges and limitations are in the canonical audit and evidence manifest.

## Implementation implications

Test camera/time scope, expiry during processing, cancellation, occlusion/gaps, redaction, resource limits and revoked artifact access.

The [architecture baseline](../architecture/baseline.md) defines ownership and execution boundaries. Complete the relevant [dependency adoption review](../architecture/dependency-adoption.md) before installing runtime dependencies. Acceptance scenarios here are requirements, not tests already executed.
