# 0008: Missing evidence is unavailable

- Status: Accepted design; implementation and runtime dependency approval remain separate
- Date: 2026-10-26
- Prerequisite: mandatory source audit gate passed before these decisions

## Context

Fixed visual scores and zero-valued missing inputs produce misleading rankings; sparse reports remain valid intake.

## Decision

Represent each signal as available, unavailable, failed or not-applicable, with reason, reliability and provenance. For available comparable signals compute sum(weight * reliability * similarity) / sum(weight * reliability); if denominator is zero the aggregate is unavailable. Preserve reliability zero. Report coverage separately against the applicable policy weight budget.

## Alternatives

Replacing absence with zero penalizes missing data; replacing it with a constant invents evidence.

## Consequences

Scoring is not probability. Confidence stays uncalibrated until a validated calibration artifact applies. Independent evidence groups prevent multiple derivatives of one image being counted as independent proof.

## Source-audit evidence

The specification and [AGENTS.md](../../AGENTS.md) govern invariants and required stack preferences. These static reviews inform the decision; they do not establish benchmarks or full dependency certification.

- [globalenglish-lostfound](../source-audit/platforms/globalenglish-lostfound.md) — `6ff8cffe55bfc3e7274124512a69fae746ceb643`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [findback](../source-audit/platforms/findback.md) — `fc91563886e4e8da6931bc07dd025fbe235068a2`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [ai-lost-and-found](../source-audit/platforms/ai-lost-and-found.md) — `577edc3b492f62ac8707fc950230bc04a354e9d8`; inspected ranges and limitations are in the canonical audit and evidence manifest.

## Implementation implications

Check all-missing, zero reliability, missing images, failed providers, correlated evidence and equal similarity with unequal coverage.

The [architecture baseline](../architecture/baseline.md) defines ownership and execution boundaries. Complete the relevant [dependency adoption review](../architecture/dependency-adoption.md) before installing runtime dependencies. Acceptance scenarios here are requirements, not tests already executed.
