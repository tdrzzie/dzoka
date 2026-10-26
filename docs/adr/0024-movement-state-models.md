# 0024: Predict recovery state transitions

- Status: Accepted design; implementation and runtime dependency approval remain separate
- Date: 2026-10-26
- Prerequisite: mandatory source audit gate passed before these decisions

## Context

Trajectron++ forecasts continuous trajectories; recovery decisions concern next location, custodian and time-to-event.

## Decision

Build a tenant-authorized recovery graph from confirmed append-oriented events. Start with explainable transition-frequency/time baselines; later models predict next location/custodian/state distributions with uncertainty. Keep observed, inferred and predicted edges distinct. Predictions guide authorized search only.

## Alternatives

Directly applying pedestrian trajectory models to institutional custody invents labels and a mismatched target.

## Consequences

Train only on permitted de-identified outcomes; split by item, institution and time to prevent leakage. Promotion requires calibration, slice evaluation and rollback; no prediction authorizes release or assigns guilt.

## Source-audit evidence

The specification and [AGENTS.md](../../AGENTS.md) govern invariants and required stack preferences. These static reviews inform the decision; they do not establish benchmarks or full dependency certification.

- [trajectron-plus-plus](../source-audit/cctv/trajectron-plus-plus.md) — `1031c7bd1a444273af378c1ec1dcca907ba59830`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [gs1-end-to-end-traceability](../source-audit/traceability/gs1-end-to-end-traceability.md) — `2.0.0 (August 2017)`; inspected ranges and limitations are in the canonical audit and evidence manifest.

## Implementation implications

Compare to frequency baselines, test unseen institutions/time shifts and sparse histories, and verify predictions never become confirmed custody events.

The [architecture baseline](../architecture/baseline.md) defines ownership and execution boundaries. Complete the relevant [dependency adoption review](../architecture/dependency-adoption.md) before installing runtime dependencies. Acceptance scenarios here are requirements, not tests already executed.
