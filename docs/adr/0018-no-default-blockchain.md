# 0018: Keep blockchain optional

- Status: Accepted design; implementation and runtime dependency approval remain separate
- Date: 2026-10-26
- Prerequisite: mandatory source audit gate passed before these decisions

## Context

Rental escrow phases and DID demos do not establish a need for recovery operations on chain.

## Decision

Use internal double-entry reward accounting and replaceable payment rails. Registered items use opaque UUID references with QR/NFC lookup and access policy; a scanned identifier proves reference possession only. DID/blockchain adapters are optional and require explicit key custody, revocation, privacy and license review.

## Alternatives

Default smart contracts add irreversible disclosure and key/transaction risks without solving ownership verification.

## Consequences

Ledger reserve, release, refund and dispute remain transactional and auditable. A provider receipt is reconciled independently of recovery success.

## Source-audit evidence

The specification and [AGENTS.md](../../AGENTS.md) govern invariants and required stack preferences. These static reviews inform the decision; they do not establish benchmarks or full dependency certification.

- [openescrow](../source-audit/recovery/openescrow.md) — `abebc3b6543a975e7ec5943e9bdd53cea6dc168a`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [dpp-did](../source-audit/traceability/dpp-did.md) — `44533c27967d406f4eeb5f09c4257b506a0264d8`; inspected ranges and limitations are in the canonical audit and evidence manifest.

## Implementation implications

Prove balanced entries, no double release, idempotent refunds, ambiguous payment reconciliation and QR possession never approving ownership.

The [architecture baseline](../architecture/baseline.md) defines ownership and execution boundaries. Complete the relevant [dependency adoption review](../architecture/dependency-adoption.md) before installing runtime dependencies. Acceptance scenarios here are requirements, not tests already executed.
