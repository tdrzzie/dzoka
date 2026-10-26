# Independent node federation

Accepted design, 2026-10-26. Implementation is pending. This view follows the [current baseline](baseline.md) and ADRs [0019](../adr/0019-federated-nodes.md); [0020](../adr/0020-federation-score-semantics.md); [0021](../adr/0021-pii-local.md). The canonical audits linked by those ADRs provide pinned source evidence and scope limits. Runtime/artifact choices must pass [dependency adoption](dependency-adoption.md).

## Protocol boundary

Maintain a local registry of peer identity/key history, allowed purposes/scopes, supported versions, quotas, calibration metadata and revocation. Trust is explicit and non-transitive. Authenticated transport plus signed versioned canonical envelopes binds issuer, audience, key ID, payload digest, request ID and issued/expiry times. Concrete canonicalization/algorithm interoperability is a pre-code protocol review item.

Queries include purpose, minimized evidence, supported model spaces, limit/deadline and permitted response fields. Validate schema, peer scope and replay before fan-out. Bound concurrency/bytes/time/cost and record per-peer completion, refusal or timeout. Do not forward private answers/contact details or expose a public enumeration oracle.

Responses contain opaque origin references, ranks, permitted evidence summaries, freshness/coverage, model/policy/calibration versions and explicit partial state. Rank fusion supports discovery; heterogeneous raw floats are never global confidence. Remote evidence/assessment does not override local release policy. Relay verification/contact through the node holding the secret evidence.

## Cross-node custody

Transfer uses a shared operation ID and local pending/accepted/committed/disputed states with evidence receipts. The current custodian authorizes dispatch; receiving authority confirms receipt under its own policy. Partition/timeout leaves an explicit pending or disputed operation, never two independently final owner releases. Reconcile signed receipts and append corrections; do not overwrite local custody with a remote result.

## Acceptance

A19–A20 and A28, including two-node partitions, key rotation/revocation, replay, cancellation, schema downgrade and PII projection inspection.

Acceptance IDs refer to [the implementation acceptance ledger](acceptance.md); none is claimed executed by this document.
