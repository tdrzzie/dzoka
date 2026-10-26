# 0022: Authorize release through policy or accountable review

- Status: Accepted design; implementation and runtime dependency approval remain separate
- Date: 2026-10-26
- Prerequisite: mandatory source audit gate passed before these decisions

## Context

Similarity-driven approval, client-trusted roles and premature codes are unacceptable property-release boundaries.

## Decision

RecoveryAuthorization records item, approved claim, verification decision/version, policy version, actor/approval, hold checks, allowed action and expiry. Revalidate under an item-level transaction lock when issuing and redeeming handover tokens. Tokens are cryptographically random, purpose/audience-bound, stored as keyed digests and single use. Default sensitive/ambiguous policies require human review.

## Alternatives

Minting a code on claim creation or treating one successful answer as unrestricted release allows races and replay.

## Consequences

Only one active release entitlement per local item. Custody, recovery, token consumption, audit and outbox commit atomically; payment/courier side effects follow durable commands. Holds/revocation invalidate unused authorization.

## Source-audit evidence

The specification and [AGENTS.md](../../AGENTS.md) govern invariants and required stack preferences. These static reviews inform the decision; they do not establish benchmarks or full dependency certification.

- [findback](../source-audit/platforms/findback.md) — `fc91563886e4e8da6931bc07dd025fbe235068a2`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [globalenglish-lostfound](../source-audit/platforms/globalenglish-lostfound.md) — `6ff8cffe55bfc3e7274124512a69fae746ceb643`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [ai-lost-and-found](../source-audit/platforms/ai-lost-and-found.md) — `577edc3b492f62ac8707fc950230bc04a354e9d8`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [smart-asset-finder](../source-audit/platforms/smart-asset-finder.md) — `8ac4484354d23aea42e1a569e2f127a878049b1e`; inspected ranges and limitations are in the canonical audit and evidence manifest.

## Implementation implications

Race competing claims and duplicate redemption, expire/revoke tokens, change policy/holds and submit stale verification; exactly one authorized transition may commit.

The [architecture baseline](../architecture/baseline.md) defines ownership and execution boundaries. Complete the relevant [dependency adoption review](../architecture/dependency-adoption.md) before installing runtime dependencies. Acceptance scenarios here are requirements, not tests already executed.
