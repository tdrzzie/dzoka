# Rewards and ledger-based escrow

Accepted design, 2026-10-26. The M10 voluntary reward and gateway profile is implemented; see its [contract](../development/fulfillment.md) and [verification](../testing/m10-verification-2026-11-26.md). This view follows the [current baseline](baseline.md) and ADRs [0018](../adr/0018-no-default-blockchain.md); [0022](../adr/0022-release-authorization.md). The canonical audits linked by those ADRs provide pinned source evidence and scope limits. Runtime/artifact choices must pass [dependency adoption](dependency-adoption.md).

## Financial boundary

Rewards are optional and independent of matching/ownership decisions. Store terms, beneficiary, amount/currency, consent, eligibility and policy version. A double-entry ledger records reserve, release, refund, fee and dispute movements as balanced immutable entries with unique logical operation references. No direct client balance mutation or float-based money calculation.

PaymentProvider encapsulates authorization/capture/refund/payout and reconciliation. External funds state and internal ledger state are separate; persist intent and stable operation keys before calls, verify callbacks and reconcile unknown outcomes before retrying non-idempotent effects. A ledger reservation is not proof that an external transfer settled.

## Settlement policy

Release requires the current reward terms and confirmed eligible recovery outcome with no applicable dispute/hold, under a serialized logical settlement operation. Physical item release is never conditional on a model selecting a beneficiary. Corrections use reversal/compensating entries, not edited history. Custody disputes and chargebacks retain auditable reasons and authorized review.

Normal payment rails are the default; no blockchain is required. Any optional chain provider needs separate key custody, privacy, irreversibility and license review. Payment regulatory obligations and concrete terms need current deployment-specific review before live money operations.

## Acceptance

A15–A18, A24 and A27: local execution covers balance conservation, immutable transfers, duplicate callbacks, release/refund races, unknown reserves, disputes and compensating payout/fee reversals. The integration uses the versioned gateway contract with synthetic HTTP payment effects. Real payment-rail and target financial acceptance remain open.

Acceptance IDs refer to [the implementation acceptance ledger](acceptance.md); none is claimed executed by this document.
