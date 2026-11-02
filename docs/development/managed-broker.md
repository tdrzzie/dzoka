# Managed broker operations

[ADR 0041](../adr/0041-managed-broker-operations.md) adds opt-in operations around the existing schema-3 broker. The 34 inference profiles, model/runtime locks and prior source bytes remain unchanged. Management code has a separate `ai/managed-service.lock.json` and private release receipt. The management release contains code/notices, and is separate from the immutable model bundle and private control/state directories.

Prepare a new protected management release from the checkout:

```powershell
python -B tools/managed_release.py --target C:\DzokaPrivate\management-001
```

The parent must already pass replacement/ACL checks. Retain the returned `releaseSha256`. Run the prepared release with the reviewed interpreter and `-I -B`; never mutate it in place or place credentials in it. The existing [model bundle](model-broker.md) and schema-3 configuration remain required.

A protected management policy refers to that configuration:

```json
{
  "schemaVersion": 1,
  "serviceConfig": "C:\\DzokaPrivate\\controls\\service.json",
  "archiveAt": 500,
  "keepTerminal": 64
}
```

`archiveAt` is 2–900; `keepTerminal` is below that threshold. The original 1,000-live-record limit remains unchanged. Before startup recovery and between polls, maintenance receipts and archives terminal records when the threshold is reached. Retained terminal records are chosen in deterministic UUID order, not chronological recency. Active owners, unconfirmed states and pending grants remain in the active directory. An archive holds at most 100,000 records; live maintenance accepts at most 2,000 records to permit recovery above the historical limit. Reaching bounds or encountering uncertainty pauses work rather than expiring history. Owner lock files are retained. Storage/export/retention policy still requires operator ownership.

```powershell
C:\Python314\python.exe -I -B C:\DzokaPrivate\management-001\tools\managed_broker.py --policy C:\DzokaPrivate\controls\managed.json --release-sha256 <actual-release-sha256> --check
C:\Python314\python.exe -I -B C:\DzokaPrivate\management-001\tools\managed_broker.py --policy C:\DzokaPrivate\controls\managed.json --release-sha256 <actual-release-sha256> --foreground
```

`--check` performs maintenance and checked recovery but makes no core claims. `--scm` uses the existing dispatcher and still requires administrator installation, selected service identity/logon rights and target-host acceptance. The original instance lock is shared with the older service entry point; two versions cannot poll concurrently against the same service state.

Credential JSON must contain exactly the configured token references. Rotate it through an atomic protected replacement at the same path. The replacement and parent must pass the private ACL checks, including explicit DACL protection for control files. Validate every reference before accepting a generation. Invalid/insecure/oversize/duplicate JSON pauses claims without using the previous credential. A corrected replacement resumes work; changed tokens wake their assignments, while other assignments keep their backoff. Tokens are re-read between invocations, not during an existing lease. This does not implement IdP token issuance/refresh/revocation. Policy, plan or service-configuration changes require restart/revalidation.

`managed-status.json` in the private service-state directory records only opaque run/sequence/time, management source identity, phase/reason, accepted credential generation and aggregate counters. `completedAcknowledgements` means the existing broker received a successful core completion acknowledgement; it is not an ownership/quality decision. `failedInvocations` does not imply the durable job failed. Idle polls, paused cycles, archived profiles, remaining records and excluded archive owners are distinct. Snapshots do not replace core audit, history or liveness checks. No token hashes, IDs, private paths, inputs or outputs appear.

For explicit offline profile maintenance, run the prepared release's `tools/profile_archive.py archive --state <protected-profiles-directory> --keep 64`, or `verify --state <protected-profiles-directory>`. The archiver performs no SDK deletion or Docker operation. Exact journal bytes and flushed hash/length receipts precede removal of the active copy. Retries accept identical copies; conflicts, orphaned archive files, wrong account/name, broad leaf ACLs and reparse paths fail closed. Private inherited producer-file ACLs are accepted only beneath a protected root after a complete effective-rule audit. Never erase an uncertain record or conflicting archive to force a pass.

## Private reference diagnostics

`tools/model_evaluation.py` supplies independently authored normalized exact, word-error and character-error metrics for OCR/speech/directed translation text. General drafts, embeddings, boxes and masks need other evaluation protocols. It runs the existing isolated model boundary with an explicit bundle receipt/state directory. No model/runtime dependency is downloaded or installed.

Use a protected dataset directory with private top-level files and a manifest:

```json
{
  "schemaVersion": 1,
  "scope": "AUTHORED_DIAGNOSTIC",
  "evidence": {"path": "permissions.txt", "sha256": "<actual-evidence-sha256>"},
  "cases": [{
    "profile": "ISO_LANGUAGE_TRANSLATION",
    "path": "source.txt",
    "sha256": "<actual-input-sha256>",
    "reference": "I lost a black bag.",
    "metric": "exact"
  }]
}
```

`REFERENCE_EVALUATION` is also accepted as a declared scope; documentary evidence is not legal/consent certification or representative-dataset approval. Cases are bounded to 128, basenames cannot escape the directory, duplicate input/profile cases are rejected and every evidence/input hash is verified before inference. References/output are bounded to 4,096 Unicode codepoints. Normalization is NFC, case folding and collapsed whitespace; CER counts Unicode codepoints, not UTF-16 units. Edit error rates can exceed one because insertions count. These metrics are not semantic correctness, identity verification or calibrated confidence.

Run with explicit `--dataset`, `--bundle`, `--bundle-sha256`, `--state` and a new private `--output` path. Results contain hashes/provenance and individual metric values, omitting raw input, reference and generated text. Every result sets `approval: NOT_ESTABLISHED`. Aggregate/sliced quality evaluation, leakage controls, representative labels and native-speaker review remain required before model promotion.

Full local verification now also requires `DZOKA_TEST_MANAGED_RELEASE` and `DZOKA_TEST_MANAGED_SHA256`, alongside the two model-bundle variables. The native managed phase exercises actual SDK records/ACLs and one authored isolated translation diagnostic. The real-core scenario exercises wrong-tenant refusal, valid credential replacement, malformed-file pause, resumed real inference and automatic archival. The [execution record](../testing/m4-managed-operations-2026-11-02.md) records results and remaining scope.
