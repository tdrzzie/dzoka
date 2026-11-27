# M3 Unicode validation and final local verification

2026-11-27. Repairs the [independent Unicode finding](m3-unicode-recheck-2026-11-27.md). **The defined local M3 implementation is complete and verified.** The final full 27-phase run passes on the corrected source set, including real-provider integrations, restore and the final unchanged-source check. Earlier failed runs remain recorded below.

## Implemented behavior

Place names, aliases and directory normalization reject residual UTF-16 surrogate code points. Valid pairs are combined by Java's code-point iterator, so supplementary characters remain valid. Raw combining sequences, literal question marks and U+FFFD remain intact. The framed request encoder additionally uses a strict UTF-8 encoder which throws on malformed text rather than silently replacing it.

No hash version, schema, valid-input byte framing or historical receipt is changed. Version 2 golden digests remain exact for ASCII and supplementary Unicode input; both frozen version 1 map-order readers remain available. Invalid requests are rejected before persistence/retry lookup, including when the key already belongs to a valid receipt. The correction adds no runtime dependency.

[ADR 0068](../adr/0068-stable-place-command-receipts.md) and the [API/upgrade guide](../development/places-and-media-reconciliation.md) define the behavior and rollout. Continue to drain old binaries before V28/patched-replica rollout; no mixed-version/downgrade support is inferred.

## Executed regressions

The new domain regression failed against the previous implementation because malformed input did not throw; [before-fix evidence](artifacts/m3-unicode-fix-20261127/before-fix.json) retains the observation and source/log hashes. The focused fixed run passes **45 Java unit and 13 place/retry integration tests**, with zero failures/errors/skips; [case receipt](artifacts/m3-unicode-fix-20261127/focused-tests.json).

Actual signed HTTP regressions submit lone high/low surrogates and malformed sequences separately in names/aliases, both with fresh keys and as same-key replays, and on updates. They receive HTTP 400 without extra place/revision/receipt/audit/outbox writes. The original valid request remains replayable; an ordinary changed name conflicts with HTTP 409. Valid JSON surrogate-pair escapes and literal supplementary characters return the same durable result, preserving raw combining marks and aliases.

Independent API JVM tests now use supplementary characters for new create/update commands and populated V27 legacy create/update receipts in both historical map orders. Two concurrent replicas and fresh restarted processes replay the original revisions. Stored legacy hashes/version metadata remain unchanged. Tenant/actor/revocation checks remain exercised. Independently generated Python UTF-8/big-endian framing and JSON vectors pin valid supplementary-text hashes alongside the original historical ASCII vectors.

## Full-gate failures investigated

The first whole-gate attempt stopped at a CPU Shona container availability error after two completed public translations. An owned-container diagnostic and the complete nine-case CPU probe replay passed, and the second whole-gate attempt also passed that phase. Its original cause remains unresolved; no model/runtime availability repair or production reliability claim is inferred. The failed attempt is retained.

The second gate passed all 53 isolated profiles, native broker recovery and management checks, then timed out at watchdog readiness. The verifier allowed 720 seconds for initial/explicit startup but only 180 seconds for watchdog recovery including scheduler launch and full startup integrity checks. A retained timing diagnostic observed scheduler launch at 99.732 seconds, actively owned/RUNNING recovery at approximately 319.55 seconds, and all fourteen native lifecycle cases passed with the existing bounded startup allowance. The verifier now consistently uses the 720-second diagnostic bound. Seven readiness unit tests preserve terminal-state rejection, stale-generation denial, active-lock requirements and finite timeout; the slow-readiness test also exercises a previous generation. No worker security policy, restart budget or production SLO is changed.

The new immutable test management release has receipt `7a8d0fe791250532a418411b8cc51db54264f01b6ea44637ff0c1e3f8fc85a26`. The model bundle stays at `2382b945fbe3532f6253246d969a41d7d51623b2cc737630c64a09f49738b027`. Old releases, failed gates and their source inventory remain retained. [First failed attempt](artifacts/m3-unicode-fix-20261127/attempt-1-cpu.json), [second failed attempt](artifacts/m3-unicode-fix-20261127/attempt-2-watchdog.json), [timing diagnostic](artifacts/m3-unicode-fix-20261127/watchdog-timing-diagnostic.json). The final gate is a new full run on the corrected sources/release, rather than combining earlier phase successes into a claimed uninterrupted pass.

## Final full gate and restore

| Check | Executed final result |
|---|---|
| Whole local gate | All 27 phases PASS in the final uninterrupted run; source inputs unchanged |
| Python | 269 tests, zero skips |
| Java unit | 45 tests, zero failures/errors/skips |
| Real-provider integrations | 157 tests, zero failures/errors/skips; PostgreSQL/private native SeaweedFS/pinned Docker and reviewed installed providers |
| M3 HTTP and process cases | 11 PlacesIT plus two independent-process PlaceRetryIT cases; malformed names/aliases reject without writes, valid supplementary input replays, both historical orders survive populated upgrade and replica/restart replay |
| Isolated providers | All 53 profiles and three joint-text boundary cases pass; cleanup confirmed |
| Native deployment/recovery | Fourteen actual installed-task lifecycle cases and nine broker recovery/integrity cases pass; 33-job queue drains after worker crash without cross-tenant claims |
| Full logical restore | 111 tables/8,309 rows; exact rows/effective catalog, tenant/place isolation, private operator journal, unprivileged runtime and snapshot independence |
| Restored receipts | Two version 1 and 58 version 2 receipts, exact table hash/count retained |
| Source/dependencies | Source/registry, architecture and 106 pinned artifact/POM/license checks pass; no new runtime dependency |

[Manifest](artifacts/m3-unicode-fix-20261127/manifest.json), [full gate](artifacts/m3-unicode-fix-20261127/full-gate.json), [JVM cases](artifacts/m3-unicode-fix-20261127/jvm-tests.json), [Python receipt](artifacts/m3-unicode-fix-20261127/python-tests.json), [restore](artifacts/m3-unicode-fix-20261127/restore.json), [deployed-worker cases](artifacts/m3-unicode-fix-20261127/current-account-deployment.json), [queue recovery](artifacts/m3-unicode-fix-20261127/queue-recovery.json), [independent golden vectors](artifacts/m3-unicode-fix-20261127/golden-vectors.json) and [496-input source inventory](artifacts/m3-unicode-fix-20261127/sources.json) bind the executed working tree based on `e50a75f`. Raw/LF hashes distinguish execution bytes from Git newline normalization; [publication byte proof](artifacts/m3-unicode-fix-20261127/source-publication.json) records all 496 actual staged blobs and twelve exact newline overlays separately. Reconstruct the recorded execution bytes in a separate copy, verifying each raw digest and the aggregate gate input digest; do not normalize the historical evidence in place. Child JSON is written as UTF-8/LF. Raw logs, signing keys, credentials and synthetic dumps remain ignored.

The final normal-gate installed-task readiness measurements are 109.177/126.861/131.468 seconds for initial/watchdog/explicit restart. These are diagnostic observations, not production startup SLOs. The earlier 319.55-second recovery and CPU availability failure remain retained; a later pass does not establish a repaired intermittent native-container defect or production reliability.

Model diagnostics establish conformance within their declared scope; they do not confer representative quality, model promotion or production acceptance. No fresh remote CI execution is claimed. The restored object backup manifest is explicitly empty; real object erasure/barrier behavior is covered separately by the S3 integrations.

## Reproduction

Use the reviewed cached artifacts and protected bundle/release preparation in the [broker guide](../development/model-broker.md) and [management guide](../development/managed-broker.md). Prepare a new immutable management release from the final source and use its actual returned receipt; old releases remain immutable. The recorded run uses the receipts above.

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-25.0.4'
$env:DZOKA_TEST_EDGE_SANDBOX='true'
$env:DZOKA_TEST_BROKER_BUNDLE='C:\Users\nyash\DzokaBrokerBundle-20261107-v2'
$env:DZOKA_TEST_BROKER_SHA256='2382b945fbe3532f6253246d969a41d7d51623b2cc737630c64a09f49738b027'
$env:DZOKA_TEST_MANAGED_RELEASE='C:\Users\nyash\DzokaManagedRelease-20261127-m3-unicode'
$env:DZOKA_TEST_MANAGED_SHA256='7a8d0fe791250532a418411b8cc51db54264f01b6ea44637ff0c1e3f8fc85a26'
python -B tools/verify_local.py --pg-bin 'C:\Program Files\PostgreSQL\18\bin' --maven 'D:\Projects\ai\dzoka\.audit-cache\apache-maven-3.9.16\bin\mvn.cmd' --native-s3 'D:\Projects\ai\dzoka\.audit-cache\seaweed-native\weed.exe'
```

No bootstrap, model download or credential publication is part of this run. JDK 25.0.4 targets Java 21; native PostgreSQL 18.6, Maven 3.9.16, reviewed SeaweedFS 4.51 and existing pinned model/raster/container runtimes are used.

## Completion boundary

The defined M3 local text/PNG/JPEG intake, versioned canonical places and independent orphan-media reconciliation profile is the scope. Target supervision/boot, institutional data/roles, target security/privacy/storage configuration, independent adversarial/load/HA assessment, external replicas/backups and legal retention acceptance require deployment evidence. Broader scanner/codec/geofence/transport profiles retain their explicit separate scope. Same-cluster logical restore with pre-existing roles and an explicitly empty object snapshot is not production disaster recovery. No remote CI or production certification is inferred.
