# M2 local implementation completion

2026-11-27. Completes the remaining local foundation administration work identified after the [M2 recheck](m2-verification-2026-11-27.md). This extends the original create/list and manual-provisioning slice with working backend/operator functionality. It does not claim a complete frontend or institutional production acceptance.

## Implemented behavior

| Capability | Implemented contract |
|---|---|
| Organization traversal | Authorized bounded UUID cursor pages beyond the legacy 100-row cap; explicit archived inclusion and individual lookup; live traversal rather than a cross-request snapshot |
| Organization lifecycle | Rename, archive and reactivate with expected versions; no hard deletion or loss of historical references |
| Durable retries | Actor/tenant-scoped immutable update responses; old creation responses also remain stable after rename/archive; conflicting bodies fail and revoked membership cannot replay |
| Atomic mutation | Organization state, retry receipt, audit and outbox share one transaction; injected event failure rolls back all of them |
| Tenant provisioning | Separate operator command creates, renames, disables and reactivates tenants under expected-version/idempotency checks |
| Local membership lifecycle | Operator-only explicit issuer/subject grants, complete permission replacement and revocation; no public privilege bootstrap or trusted ADMIN token role |
| Operator evidence | Append-only private receipts bind database actor, hashed input/target and result; credential-holding database owners remain privileged |
| Credential boundaries | Operator receives migration credentials, excluding API/provider/storage/Spring settings; API child excludes migration secrets; bounded strict JSON stdin and fixed safe failures |
| Migration | Appended V26, preserving V1–V25; explicit RLS/column grants and private operator journal; existing populated V25 create receipts backfill before lifecycle changes |
| OIDC rotation | Real RSA/JWKS resource-server test accepts old/new overlap and rejects unpublished keys; local revocation remains separate from cached signing-key lifetime |

[ADR 0066](../adr/0066-foundation-administration.md) and the [operator/API guide](../operations/foundation-administration.md) define authority, commands and limitations. No runtime dependency, upstream source, model or credential is added. Existing 106-artifact dependency pins remain unchanged.

## Verification

- Source gate, registry integrity, architecture checker and 106 dependency byte/POM/license checks pass.
- Python suite: 269 discovered, 268 passed and one explicitly skipped video-runtime case; that exact skipped case subsequently passed using the existing pinned Docker runtime. Two new launcher tests verify privileged child credential separation. No unresolved Python failure remains.
- Java unit suite: 36 passed, zero failures/errors/skips.
- Full native PostgreSQL/private SeaweedFS/pinned Docker raster/video-contract suite: **120 integrations passed, zero failures/errors/skips**. It includes 14 existing foundation cases, nine new administration cases and the unconfigured packaged-startup case. Native AI model inference/representative quality were not rerun.
- The earlier database/API-only run passed 107 integrations with one explicitly skipped sandbox case, then restored 107 tables/5,814 rows with exact rows, catalog and tenant isolation. The final full run supersedes that narrower integration selection.
- Populated upgrade coverage applies V1–V25, seeds an existing creation receipt, applies only V26, validates and confirms a second migration is a no-op. Old response name/version survive the upgrade.
- Two fresh source copies built offline using the shared pinned Maven cache and reviewed JDK produce identical executable JARs: **40,751,523 bytes**, SHA-256 `456278744413fb51cf74f1ced515e5cdb0c28d5711360985575973c80c07211c`. This is same-host/JDK reproducibility, not a cross-platform or independently provisioned build certification.

The final full restore passes **107 tables/6,096 rows**, exact row/effective-catalog equality, restored tenant isolation, source-snapshot independence, verified backup bundle and the operator journal permission fault check. [Sanitized source/test manifest](artifacts/m2-implementation-20261127/manifest.json), [JVM test cases](artifacts/m2-implementation-20261127/jvm-tests.json), [restore receipt](artifacts/m2-implementation-20261127/restore.json) and [reproducibility receipt](artifacts/m2-implementation-20261127/reproducibility.json) bind the executed scope. Raw logs/XML, SQL/data dumps, generated test credentials, signing keys and operational configuration stay ignored.

Reproduce the full local integration/restore route with the reviewed installed runtimes:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-25.0.4'
$env:DZOKA_TEST_EDGE_SANDBOX='true'
python -B tools/test_intake.py --native-s3 'D:\Projects\ai\dzoka\.audit-cache\seaweed-native\weed.exe' --pg-bin 'C:\Program Files\PostgreSQL\18\bin' --maven 'D:\Projects\ai\dzoka\.audit-cache\apache-maven-3.9.16\bin\mvn.cmd' --restore-drill
```

Run `python -B -m unittest discover -s tests -v` with that sandbox flag and engine access to include the real video case. For binary reproduction, copy only `pom.xml` and `src/main` into two distinct empty workspace directories and run the pinned Maven executable with `-o`, the same absolute `-Dmaven.repo.local` cache, `-Dmaven.test.skip=true package` in each. Compare the complete executable JAR SHA-256; do not copy compiled classes between builds. This packaging check is separate from the test run.

## Restore failure and correction

The first administration-only integration run passed 23 cases, but its restore checker rejected table catalog equality. Explicitly revoking public/runtime access on the new private operator journal produces an explicit owner-only ACL; PostgreSQL dump/restore can represent the same permissions using its implicit default ACL. The checker now expands effective ACL entries with `aclexplode`/`acldefault`, preserving grantor, grantee, privilege and grantability, alongside unchanged ownership/RLS/policy/constraint/index/trigger checks.

This is not a bypass: the drill deliberately grants restored journal SELECT to the runtime role and requires the catalog comparison to detect the difference, then revokes it and requires complete equality again. It also verifies the operator journal has no runtime SELECT privilege. Backup configuration metadata now records the actual successful migration version instead of a stale literal 25. Both changes are exercised against the real PostgreSQL restore.

## Remaining deployment acceptance

The defined M2 local backend/operator implementation is complete. Its API is a resource server, not a new identity provider. Institutional enrollment, MFA/password/account recovery, upstream session revocation and actual provider interoperability require the selected IdP and permitted accounts. Target TLS/secret storage, individually attributable operator access, independent advisory/security assessment, load/HA and institutional acceptance still require deployment evidence. These are not proven by fixtures, the operator credential or this same-cluster logical restore. Existing later-milestone production/quality deferrals remain unchanged; no real PII, live account administration or existing deployment was modified in this work.
