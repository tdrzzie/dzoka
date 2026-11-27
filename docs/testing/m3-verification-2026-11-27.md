# M3 substantive revalidation

Subsequent implementation on this date closes the two identified local gaps. See the [M3 completion record](m3-implementation-2026-11-27.md) for V27 code, real crash/no-callback regressions and new verification/restore receipts. The inspection below describes the pre-extension implementation and is retained as historical evidence.

Checked 2026-11-27 against the [milestone deliverable](../architecture/milestones.md), specification sections 6/38/94, [governing baseline](../architecture/baseline.md), [ADR 0026](../adr/0026-private-image-intake.md), [adoption record](../dependencies/intake-media-adoption.md), actual intake/media implementation and executed tests. A status label was not used as evidence.

**Verdict: the bounded text/PNG/JPEG intake implementation is real and freshly verified. The whole M3 deliverable is not fully closed: canonical place foundations and independent orphan-media reconciliation remain local engineering, in addition to broader format and target deployment acceptance.** The previous milestone row said “scoped slice delivered”; interpreting that as full completion would overstate its evidence.

## Implemented and checked

| Responsibility | Code and behavior |
|---|---|
| Cases/items/reports | Separate tenant-qualified references; LOST/FOUND observation does not grant ownership or physical custody |
| Exact evidence | Accepted wire bytes/hash and original text/labels remain separate from versioned NFC/whitespace normalization; immutable revisions and stale-update conflicts |
| Uncertainty | Raw location/time labels, language, timezone, optional coordinate uncertainty and ordered temporal intervals; missing coordinates are not fabricated |
| Private media | Durable upload intent before external PUT; immutable conditional writes, bounded length/hash verification, quarantine and current authorization checks |
| Isolation/canonicalization | Reviewed digest-pinned Docker JRE; no network, nonroot, read-only root/input/code, dropped capabilities and resource/deadline limits; fresh PNG pixels and parent-side metadata-free output validation |
| Failure/retry | Leases, stale completion denial, retry budget, explicit failure, refused-storage recovery and no source preview fallback |
| Retention hooks | Holds, tombstones, raw revision erasure, actual source/derivative deletion, one-time reservation release and cleanup generations when late completion is acknowledged |
| Later restore controls | Current S3 test also exercises M11 restore barrier/tombstone replay and actual deletion before reopening; this supersedes the October guide's generic “restore replay pending” statement |

Fresh Java results: **36 unit tests and 120 full integrations passed, zero failures/errors/skips**. The four directly relevant M3 classes contribute **22 integrations**: IntakeIT 9, MediaStoreIT 4, S3StorageIT 6 and SandboxMediaIT 3. The larger total includes foundation and later milestone regression coverage; it must not be described as 120 independent M3 requirements.

Tests execute actual PostgreSQL, private SeaweedFS HTTP/S3, RSA/JWKS authentication and the isolated Docker raster decoder. They cover owner/staff/cross-tenant denial, concurrent create/revisions, rollback, malformed/spoofed/truncated/large images, PNG metadata removal, JPEG canonicalization, unavailable storage, holds and actual erasure. The selected native SeaweedFS executable was independently rehashed to the adopted `f3a9522f4900408d8e230f98516cd9c75fa90da3cfc96dc0fd9fbed380fc8001`. Source gate, registry integrity, architecture checker and 106 dependency artifact checks pass. No application code was changed for this review.

## Remaining local work

### Canonical places are not implemented by raw coordinates

The M3 deliverable includes places. The baseline assigns the locations module canonical places, aliases and geofences, while specification section 38 also requires hierarchy. Current ReportDraft/JdbcReports and migrations retain raw location strings, coordinates and uncertainty. Inspection found no canonical place/alias/hierarchy persistence or administration in the application/migrations. This is an implementation gap, not a missing external infrastructure credential. Raw input preservation and spatial indexes do not establish that capability. Broader route/network intelligence can remain separately staged without dropping the foundational place requirement.

### Unacknowledged late object writes need reconciliation

Static failure-path review confirms the limitation already documented in [the media guide](../development/intake-media.md). MediaService writes a source object before `store.uploaded`; MediaWorker writes a derivative before `store.complete`. A late write after purge can therefore be followed by process failure or grant revocation before its database acknowledgment.

`JdbcMediaStore.purged` clears `purge_pending` and sets `reservation_released=true`. Subsequent purge selection requires pending cleanup or an unreleased reservation on a deleted report. An unacknowledged late write does not restore either condition, so repeating the existing bounded purge command does not independently rediscover that object. Current objects remain inaccessible through report/preview authorization, but physical byte erasure is not assured for this failure path. Independent object/tombstone reconciliation or a safe recurring tombstone sweep can be implemented/tested locally.

The existing real-S3 late-write test calls `store.uploaded` after its simulated late PUT and verifies cleanup is requeued. It does not simulate a crash before that acknowledgment. This finding is based on inspected control flow and test coverage, **not a newly executed crash reproducer**. Passing the suite does not close it.

## Wider scope and external acceptance

The raster profile accepts PNG/JPEG, not arbitrary document/SVG/audio/video intake. Later speech, channels and edge implementations have their own bounded contracts; they do not automatically expand this uploader. Wider format/scanner support requires additional reviewed code/artifacts and local engineering. Antivirus/content-policy certification, adversarial corpus assessment and the target sandbox/storage/security configuration remain separate acceptance work.

Operators must schedule and supervise media sanitization/expiry/purge with appropriate private credentials. Target storage encryption/configuration drift, replicas/backups, retention/legal policy, real operational load, failover and remote CI were not certified by this check. The logical restore uses synthetic data and existing same-cluster roles; its object snapshot is explicitly empty, with actual S3 tombstone behavior exercised separately by integration tests. It is not full object-store disaster recovery.

## Reproduction

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-25.0.4'
$env:DZOKA_TEST_EDGE_SANDBOX='true'
python -B tools/test_intake.py --native-s3 'D:\Projects\ai\dzoka\.audit-cache\seaweed-native\weed.exe' --pg-bin 'C:\Program Files\PostgreSQL\18\bin' --maven 'D:\Projects\ai\dzoka\.audit-cache\apache-maven-3.9.16\bin\mvn.cmd' --restore-drill
```

The final restore passes **107 tables/6,096 rows**, exact effective-catalog and row equality, runtime privilege/tenant isolation, private operator journal checks and source-snapshot independence. The [sanitized receipt](artifacts/m3-recheck-20261127/summary.json) binds commit, source bytes, suite results, direct M3 case names and restore hashes. Raw logs, ephemeral credentials, signing keys and synthetic database/object dumps remain ignored.
