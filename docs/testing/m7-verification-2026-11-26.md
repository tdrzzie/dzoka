# M7 channel, custody and delivery verification

## V18 channel and custody baseline

M7 adds the V18 migration, the WhatsApp webhook and template adapter, the private relay, delivery operations and staff custody. The unchanged provider contract keeps recipient delivery separate from the recovery return authority. [Architecture decision](../adr/0057-whatsapp-relay-and-versioned-staff-custody.md), [operating instructions](../development/channels-and-custody.md).

- The HTTP fixture implements the provider's request and callback shapes on loopback. It exercises handshake, raw-body signatures, wrong phone-number scope, duplicate signed intake, private report creation, scoped location replies with an idempotent revision, structured relay, flag and block, opt-out, and cancelled notice replay after renewed consent.
- Verified delivery reconciliation covers an ambiguous submission, a wrong recipient, duplicate and out-of-order callbacks, and an explicit 429 retry. Delivery does not produce a custody return.
- The custody suite runs two concurrent acceptances. One becomes authoritative and the stale event is retained for review. It verifies idempotent transfer admission, tenant isolation, and staff-only location history. The existing recovery suite verifies that a confirmed return produces a single automatic notification intent in the same transaction.
- PostgreSQL restore compares the complete rows, catalog, policy and trigger definitions. During verification, tuple comparisons inside three policies produced different parenthesization after dump and restore. The migration now uses explicit equality predicates for those same checks.

| Verification | Result | Restore receipt |
| --- | --- | --- |
| Native PostgreSQL, media, AI and vision | 27 Java unit tests and 86 integrations passed | [59 tables, 4,865 rows](artifacts/m7-verification-20261126/native-restore.json) |
| Docker PostgreSQL with pgvector and the matching index | 27 Java unit tests and 20 integrations passed | [59 tables, 2,854 rows](artifacts/m7-verification-20261126/indexed-restore.json) |
| Python tooling and policy suite | 240 tests passed | |

Both restores verified exact row and catalog equality, runtime-role tenant isolation and snapshot independence.

## V19 WhatsApp conversation

- The end-to-end case exercises a claimant with no broad report access. A separate desk actor publishes a revision-specific offer. Signed WhatsApp commands reach search, candidate review, claim creation, a staff-attested ownership challenge and an acknowledgment of the answer. An independent reviewer approves the claim. Requests before approval do not create a pickup message. The desk uses the existing grant and recipient verification to record the return, then the claimant acknowledges that return through WhatsApp.
- The native S3 case verifies signed photo ingestion, content hash rejection, replay and bounded intake of synthetic audio and video as private quarantined objects. The quarantined audio and video cannot be previewed. The existing isolated raster suite covers photo sanitization.
- The match watcher checks comparable evidence from the staff-published catalogue and admits a generic template only once per candidate and lost-report pair. Search and review responses use encrypted session replies. The integration fixture checks that the answer and the staff pickup credential are absent from the channel response.
- An existing signed report can start a search without a V19 watch already present. The search creates the missing watch using the original contact and quote binding.

| Final V19 verification | Result | Restore receipt |
| --- | --- | --- |
| Native PostgreSQL, private S3, isolated raster processing, text and vision providers | 27 Java unit tests and 87 integrations passed | [64 tables, 4,945 rows](artifacts/m7-verification-20261126/v19-native-restore.json) |
| Docker PostgreSQL with pgvector and the matching index | 27 Java unit tests and 21 integrations passed | [64 tables, 2,917 rows](artifacts/m7-verification-20261126/v19-indexed-restore.json) |
| Python tooling and policy suite | 240 tests passed | |

Both restores verified exact row and catalog equality, runtime-role tenant isolation and snapshot independence. The source-audit, architecture and dependency lock checks passed. [Restore receipt hashes](artifacts/m7-verification-20261126/SHA256SUMS).

The loopback fixture uses synthetic credentials and numeric WhatsApp identities. External sandbox acceptance requires a provisioned WhatsApp test business account, approved localized templates, a reachable callback and authorized test recipients. The target site must verify role assignment, scheduler execution and the physical staff handover. Non-phone-number identity handling and isolated audio/video decoding and transcription remain open. Courier tracking depends on M10.
