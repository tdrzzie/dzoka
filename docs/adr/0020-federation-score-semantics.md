# 0020: Do not compare raw federated scores

- Status: Accepted design; implementation and runtime dependency approval remain separate
- Date: 2026-10-26
- Prerequisite: mandatory source audit gate passed before these decisions

## Context

Audited search merges scores that need not share model space, labels or calibration.

## Decision

Remote responses expose origin, rank, permitted evidence summary, coverage, model/policy/calibration versions and freshness. Use rank-level fusion for discovery. A probability is usable only when an approved calibration artifact applies to the population and evidence regime. Uncalibrated results remain labeled uncalibrated.

## Alternatives

Sorting arbitrary node floats as one global confidence ranking gives false precision.

## Consequences

Local matching may request authorized compatible evidence for local evaluation; absent compatibility stays unknown. Never average conflicting calibrated and uncalibrated estimates.

## Source-audit evidence

The specification and [AGENTS.md](../../AGENTS.md) govern invariants and required stack preferences. These static reviews inform the decision; they do not establish benchmarks or full dependency certification.

- [open-federated-search](../source-audit/federation/open-federated-search.md) — `fe13ee670bd12d55d693a1e55f5be03c7d01333c`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [mycel](../source-audit/federation/mycel.md) — `07a671d03c4fbb68f459d24af262a3dec563c913`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [visualmatch](../source-audit/retrieval/visualmatch.md) — `941c69fa7d82c7fd5f570ad7392833b8d0b83791`; inspected ranges and limitations are in the canonical audit and evidence manifest.

## Implementation implications

Check heterogeneous score ranges, reversed metrics, absent calibration, drift, stale results and federated origin attribution.

The [architecture baseline](../architecture/baseline.md) defines ownership and execution boundaries. Complete the relevant [dependency adoption review](../architecture/dependency-adoption.md) before installing runtime dependencies. Acceptance scenarios here are requirements, not tests already executed.
