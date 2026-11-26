# Courier orchestration and reconciliation

Accepted design, 2026-10-26. The local M10 gateway profile is implemented; see its [API contract](../development/fulfillment.md) and [verification](../testing/m10-verification-2026-11-26.md). This view follows the [current baseline](baseline.md) and ADRs [0022](../adr/0022-release-authorization.md); [0018](../adr/0018-no-default-blockchain.md). The canonical audits linked by those ADRs provide pinned source evidence and scope limits. Runtime/artifact choices must pass [dependency adoption](dependency-adoption.md).

## Authority and operation lifecycle

Logistics owns shipment intent, provider operation, tracking events and reconciliation; custody owns possession and recovery owns release authorization. Booking requires policy-approved current authorization, address consent and an eligible custodian. Dispatch rechecks authorization/holds and records transfer to the courier. Booking a shipment alone never proves physical pickup.

ProviderOperation uses a stable tenant-scoped logical key and request digest. Persist pending intent before the network call. Record submitted/confirmed/failed/unknown outcomes and provider references. An ambiguous timeout enters reconciliation; do not blindly create a second shipment. Signed, replay-checked callbacks are matched to authorized operations and mapped through legal transitions; event order may differ from arrival order.

## Custody and privacy

Share only the address/contact subset needed by the selected courier under explicit policy. Pickup, damage, loss, attempted delivery and receipt carry evidence references and append events. Delivery status is not automatically owner confirmation; disputes/holds can prevent final recovery or reward release. Cancellation/refund and compensation are durable operations with separate provider outcomes.

## Acceptance

A15–A18 and A27–A28. Local execution covers duplicate booking, lost replies and reconciliation, early/out-of-order/replayed callbacks, held/revoked authorization, consent withdrawal, staff return, minimal route references and owner receipt. The executable provider is an explicit development sandbox, not a vendor certification. Institution/provider deployment acceptance remains open.

Acceptance IDs refer to [the implementation acceptance ledger](acceptance.md); none is claimed executed by this document.
