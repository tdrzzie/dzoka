# 0007: Matching and ownership are separate domains

- Status: Accepted design; implementation and runtime dependency approval remain separate
- Date: 2026-10-26
- Prerequisite: mandatory source audit gate passed before these decisions

## Context

Audited systems sometimes approve claims using similarity or fixed scores. Object similarity cannot establish entitlement.

## Decision

Matching owns candidates and evidence snapshots. Claims owns assertions of entitlement. Verification owns private evidence evaluation. Recovery owns release authorization. No matching result or AI response changes a claim to approved or mints a handover token.

## Alternatives

A single confidence threshold for match and release permits category similarity to authorize theft.

## Consequences

More explicit workflow states and user steps are needed for ambiguous evidence. A claimant may challenge a candidate without receiving secret finder information.

## Source-audit evidence

The specification and [AGENTS.md](../../AGENTS.md) govern invariants and required stack preferences. These static reviews inform the decision; they do not establish benchmarks or full dependency certification.

- [lost-and-found-tracking-system](../source-audit/platforms/lost-and-found-tracking-system.md) — `4ef1493f80590f6fa03d2ad9eded2e70a59ebe58`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [findback](../source-audit/platforms/findback.md) — `fc91563886e4e8da6931bc07dd025fbe235068a2`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [smart-asset-finder](../source-audit/platforms/smart-asset-finder.md) — `8ac4484354d23aea42e1a569e2f127a878049b1e`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [malak-lostandfound](../source-audit/platforms/malak-lostandfound.md) — `d5d7bfaf043396a32b40c264c647f6456cadb633`; inspected ranges and limitations are in the canonical audit and evidence manifest.

## Implementation implications

Show that arbitrarily high match scores cannot approve claims, reveal contacts, dispatch items or release rewards.

The [architecture baseline](../architecture/baseline.md) defines ownership and execution boundaries. Complete the relevant [dependency adoption review](../architecture/dependency-adoption.md) before installing runtime dependencies. Acceptance scenarios here are requirements, not tests already executed.
