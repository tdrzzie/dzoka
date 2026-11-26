# M10 logistics, rewards and private item identities

The local M10 gateway profile is implemented. It covers real Core HTTP/JWT/PostgreSQL operations, a bounded API worker, an executable persistent development provider and the workflows below. External courier/payment acceptance remains open. The development provider performs no shipping or money movement.

[API, deployment settings and provider protocol](../development/fulfillment.md), [architecture and dependency review](../adr/0064-fulfillment-ledger-and-private-item-identities.md).

## Implemented scope

| Area | Implemented behavior |
| --- | --- |
| Courier | Private expiring quote, committed quote quota, claimant route consent, idempotent shipment intent, staff-authorized booking, current-custodian physical handoff, tracking/proof digests, cancellation and staff return |
| Authorization | Current native claim/review, report, hold, policy and custody checks before booking and handoff; consent withdrawal blocks new sends/handoff; provider delivery cannot confirm recovery |
| Recovery | Retained early tracking, source-time terminal ordering, explicit claimant receipt, independent receipt reauthorization after a cleared hold, courier custody in native history, staff return without owner recovery |
| Durable providers | Commit before send, stable tenant-scoped keys and body digests, leases, delayed reconciliation of lost/unknown replies, safe NOT_FOUND handling, signed/time-bound/replay-checked callbacks |
| Rewards | Voluntary immutable terms, integer minor units and fees, reserve/release/refund, independent disputes, policy-blocked intent recovery and cancellation only before a known send |
| Ledger | Immutable constrained transfers, balanced debit/credit view, per-reward balance checks, one settlement under concurrency and compensating payout/fee reversal after confirmed external recovery |
| Registration | Private exact descriptions, opaque IDs, QR/NFC locators, token hashes, tag revocation/rotation and deactivation |
| Passports | Private versioned internal UUID, QR/NFC, enterprise-asset and constrained GS1 Digital Link assertions; unadopted DID providers denied |
| Finder contact | Authenticated pseudonymous phrase relay, owner inbox, owner-only private item context, blocking/flagging, persisted guess quotas, message expiry and bounded purge |

A registered reference is not ownership proof. Internal UUID verification means only that the reference is bound to that private registration. The scanner does not create a claim, approve ownership, reveal contacts or authorize property release. Rewards do not gate physical recovery.

## Executed verification

The final implementation passes **264 Python tests, 33 Java unit tests and 93 native integrations**, including **15 M10 scenarios**, with no skipped cases. Restore matches **91 tables and 5,243 rows**, exact catalog equality and tenant isolation. The [machine-readable execution receipt](artifacts/m10-verification-20261126/verification.json) records counts, log hashes, runtime versions and exact source identities. The [scenario results](artifacts/m10-verification-20261126/scenarios.json) name the executed M10 integration cases. The [restore receipt](artifacts/m10-verification-20261126/full-core-restore.json) records the row/catalog results. [Artifact hashes](artifacts/m10-verification-20261126/SHA256SUMS).

The tests use actual Core JWT authentication, tenant-scoped native PostgreSQL, and a real local HTTP courier/payment gateway. A lost receipt is injected after the gateway commits its synthetic operation; Core reconciles without making a second booking or inventing ledger funds. Separate executable-sandbox tests retain virtual effects across server restart, reject changed operation bodies and choose one release/refund under concurrency. The real bounded Python worker uses a scrubbed environment and completes a Core reserve job with only its API token.

The native suite includes the existing recovery, channel, federation, traceability, matching and isolated edge worker integrations. The restore now compares view definitions, ownership, ACLs and security options alongside the existing table/function/policy catalog, including the ledger's security-invoker setting. Source audit, architecture, dependency integrity and whitespace checks are included in the receipt. External model/provider inference matrices were not repeated for this change.

Earlier failures are retained in ignored local test logs. Initial definer permission checks could not see the invoker's membership under forced RLS; narrow current-context metadata policies corrected that. Fixture corrections gave the private-fact observer its existing report-read permission and isolated claimant identities so the existing fraud quota was preserved. The SQLite sandbox now explicitly closes database connections after transactions on Windows. An unavailable test helper was replaced with existing Spring facilities, adding no dependency. These earlier attempts are not counted as passing runs.

## Reproduction

Run the source-audit gate and Python tests. Set `DZOKA_TEST_EDGE_SANDBOX=true` for the real edge decoder regression. Run `tools/test_foundation.py --restore-drill` with the project's pinned Maven cache and an isolated native PostgreSQL installation. `FulfillmentIT` is registered in that native integration suite; `--full` with `DZOKA_IT_CLASSES=FulfillmentIT` runs a focused replay. Each run initializes a disposable cluster and stops it afterwards. See the developer contract for the executable standalone sandbox and worker.

## Acceptance still requiring a target provider/site

The implemented HTTP adapter speaks **Dzoka fulfillment gateway v1**. It does not claim compatibility or certification with a named commercial courier or payment rail. Live acceptance needs a selected provider/bridge and protected sandbox credentials, approved route-vault references, approved reward/fee/reversal terms, configured callbacks and authorized test recipients. No courier or payment provider configuration was present in the inspected environment. Provider selection and target configuration were unavailable during this run.

A target deployment must provision the appropriate service identity, renewable worker tokens, gateway/webhook secrets, dispatch and retention scheduling, and carry out a staffed handoff/receipt and provider reconciliation rehearsal. QR/NFC data and scan routes are implemented; physical printing/programming and reader-device acceptance require the selected hardware/client. Finder contact currently requires an authenticated tenant membership and hides identities/contact details; it is not an unauthenticated public messaging endpoint. DID/blockchain cryptographic adapters remain optional and unadopted.

No real funds were reserved, released or refunded, and no real shipment was booked. Local conformance does not replace target financial, privacy or operational approval. M10's local implementation is delivered; the overall milestone remains open for those provider/site acceptance records.
