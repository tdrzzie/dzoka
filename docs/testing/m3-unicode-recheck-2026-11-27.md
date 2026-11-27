# M3 independent Unicode recheck

**Historical finding, now fixed:** the [verified repair](m3-unicode-fix-2026-11-27.md) rejects malformed Unicode and preserves valid hashes/receipts. All 27 final phases, 45 unit/157 integrations and 111-table/8,309-row restore pass. The original source/HTTP observations below remain evidence for commit `e50a75f`; the historical probe is expected to reject the malformed request on the patched implementation.

2026-11-27, commit `e50a75f`. **Verdict: substantial local implementation and real execution evidence, but local M3 is not fully complete. A newly reproduced changed-body retry defect remains in the version 2 encoder.** This supersedes the unrestricted local completion verdict in the [retry closure](m3-retry-fix-2026-11-27.md); its valid-input restart/replica and historical-receipt successes remain valid.

## Confirmed local defect

`PlaceDraft.label` accepts unpaired UTF-16 surrogates: its checks cover blanks, length and ISO controls, but not malformed Unicode. Jackson deserializes the JSON name `"\ud800"` into such a Java string. `PlaceCommandHash.text` then calls `String.getBytes(UTF_8)`, which replaces this malformed input with the same bytes as a literal question mark. The two distinct accepted drafts produce exactly the same framed input and request digest. This is a lossy encoding collision before hashing, not a break of SHA-256.

Actual signed HTTP calls through the current `CoreApplication` in a fresh, loopback-only PostgreSQL fixture reproduced:

| Request | Observed |
|---|---|
| Create place named `?`, new idempotency key | HTTP 200 |
| Retry same key/actor/tenant, change name to escaped lone surrogate `\ud800` | HTTP 200, exactly the original response |
| Retry same key, ordinary different name | HTTP 409 |
| Number of places added | One |

The malformed changed body should be rejected, rather than accepted as an identical retry. No tenant/authentication bypass, additional mutation or property-release effect is established. The same encoder is used for labels/aliases; only the name case was executed through HTTP here.

[Sanitized observation and source hashes](artifacts/m3-unicode-recheck-20261127/probe.json), [JSON/domain/hash probe](artifacts/m3-unicode-recheck-20261127/M3UnicodeHashProbe.java), [HTTP probe](artifacts/m3-unicode-recheck-20261127/UnicodePlaceHttpProbe.java), [fresh-cluster runner](artifacts/m3-unicode-recheck-20261127/run_unicode_place_probe.py). The direct probe produces equal digest `161c7f0868af7d9a79049beea9c5da43936ce3f77c3d7123134afd2af2dc7a4b` for distinct names and `matches(2, ...)` returns true. The HTTP probe compiles against the actual fixture/application classes and uses the existing harness to provision and stop a separate disposable cluster; its runner replaces only the harness Maven invocation with the probe JVM. No existing database, model provider or real user data is accessed. Raw logs, credentials and ephemeral signing keys remain ignored.

## Checks and evidence integrity

Published/current source hashes and child-artifact byte hashes from the prior closure were checked independently: all 400 LF-normalized source hashes match. Registry/source, architecture and 106 dependency artifact/POM/license checks pass. The prior full suite genuinely executed 269 Python/42 Java unit/136 integrations and a 111-table/6,486-row restore; it does not contain this malformed-Unicode regression. Its local-completion conclusion was too broad despite genuine test execution.

Fresh execution passes **42 Java unit tests and 135/136 integrations**, zero failures/errors and one explicit optional video skip because `DZOKA_TEST_EDGE_SANDBOX` was omitted. That omitted case then passes in a **separate three-case EdgeIT replay with the sandbox flag enabled and zero skips**; this is not represented as one uninterrupted zero-skip run. Full logical restore passes **111 tables/6,474 rows**, exact rows/effective catalog, tenant/place isolation, private operator journal and snapshot independence, preserving two version 1 and 56 version 2 place-command receipts. The smaller restored row count reflects the omitted edge case in that main dataset; the separate replay is not folded into the restored dataset.

[Fresh manifest](artifacts/m3-unicode-recheck-20261127/manifest.json), [main JVM cases](artifacts/m3-unicode-recheck-20261127/jvm-tests.json), [explicit video replay](artifacts/m3-unicode-recheck-20261127/edge-replay.json) and [restore](artifacts/m3-unicode-recheck-20261127/restore.json) record exact counts, source/artifact hashes and limits. Python's 269-case result above is the prior source-matched run; it was not rerun for this audit. No new all-profile model or remote CI result is claimed.

## Reproducing the HTTP probe

Use the reviewed JDK, existing `target/classes` and `target/test-classes` from the normal Maven verification, and the exact dependency lock. Copy `UnicodePlaceHttpProbe.java` and `run_unicode_place_probe.py` from the linked artifacts into ignored `.test-tmp/`, then run `python -B .test-tmp/run_unicode_place_probe.py` from the repository root. The runner compiles only the probe into `.test-tmp/unicode-probe-classes`, starts its own synthetic PostgreSQL cluster using the reviewed installed binaries and cleans up that cluster. Do not publish raw logs or keys. The direct JSON/hash probe can run in source-file mode with the application classes and non-test locked JARs.

## Required local closure

Reject malformed Unicode at all place label/alias entry points or make the framed encoder fail strictly instead of replacing malformed code units. Preserve the version 2 digest for every already valid Unicode input and retain both version 1 readers. Add actual JSON/HTTP regressions for lone high/low surrogates in names and aliases, legitimate supplementary characters, changed-body denial and unchanged historical receipt/restart behavior. Re-run relevant integration/restore checks before restoring local completion. No production dependency is needed for this fix.

Target supervision/boot, institutional configuration, independent security/adversarial/load/HA, replica/backup/legal and broader codec/scanner/geofence/transport acceptance remain separately scoped. They are not the reason for reopening local M3. No application behavior was changed during this audit.
