# M2 recheck after the M3 implementation

Checked 2026-11-27 against commit `01e67470ca7128079298edb540b03388ec26a1ed`, the defined M2 deliverable, implementation, migrations and fresh execution. No application code or completion criteria were changed.

**Verdict: M2's defined local transactional foundation and operator administration are implemented, rather than merely marked complete. No missing implementation within that contract was found. Institutional production identity/deployment acceptance remains separate. A historical receipt-publication defect was found and corrected locally; the correction was local at inspection and is included in the subsequent publication changes.**

## Fresh verification

| Check | Result |
|---|---|
| Current Java unit suite | 38 pass, zero failures/errors/skips |
| M2 integrations | 24 pass: FoundationIT 14, FoundationAdministrationIT 9, StartupConfigurationIT 1; zero failures/errors/skips |
| Credential launcher tests | 2 pass |
| Dependency integrity tests | 5 pass, including byte alteration, path escape and unreviewed-license rejection |
| Source/architecture/dependency gates | Registry integrity, source gate, architecture checker and 106 pinned artifacts/POMs/license declarations pass |
| Current logical restore | 111 tables/244 rows; exact row/effective-catalog equality, runtime privileges, private operator receipts, tenant/place isolation and snapshot independence pass |

The integration harness starts a real isolated PostgreSQL database and HTTP application, uses actual RSA/JWKS bearer validation and exercises SQL policies as the non-owner runtime role. Tests cover cross-tenant/role denial, pooled context cleanup, concurrent idempotency and version conflicts, rollback, immutable responses after rename/archive, revocation, outbox lease/replay, inbox duplicate/conflict handling, operator tenant/member lifecycle, private append-only receipts, real operator subprocess execution and JWT key overlap. The populated V25→V26 upgrade remains explicitly targeted at its original scope; the current application run applies all 27 migrations.

Code review checked the organization adapter/service boundaries, FoundationAdmin, runtime database guard, transaction context, JWT configuration, V1/V2/V26 schema/grants and credential launcher. The operator remains a separate privileged database process, not a public membership-grant endpoint. Token roles cannot bootstrap local authority.

[Sanitized receipt](artifacts/m2-post-m3-recheck-20261127/summary.json) binds current source hashes, individual case names, restore results and publication checks. Raw database/object data, credentials, signing keys, XML/logs and configuration remain ignored.

## Receipt-publication correction

The previous M2 manifest's three child artifacts (`jvm-tests.json`, `reproducibility.json`, `restore.json`) match their original Windows working bytes, but their published Git blobs do not match the recorded SHA-256 values. Git normalized CRLF to LF. Comparison confirms the JSON content is identical after newline normalization; this is a byte-publication defect, not invented test data or a newly observed runtime failure.

Added an explicit `-text` rule for that historical receipt directory and retained the existing original bytes of all four JSON files, including its manifest. **No recorded digest or JSON value was regenerated to hide a mismatch.** Real Git filtering checks now produce the same object bytes as `--no-filters`, and the child artifacts match their original recorded digests. This aligns with existing byte-preservation rules for other historical execution receipts. At inspection, published commit `01e6747` still contained normalized artifacts. The correction is included in the subsequent publication changes; Git history identifies their commit. New recheck JSON uses LF bytes directly.

## Scope and limits

The 24 integrations establish the local foundation contract, not every later domain/provider path. Resource-server and signing-key tests use an ephemeral synthetic issuer, not a deployed institutional IdP. Enrollment, MFA/password recovery, upstream session revocation, institutional interoperability and individual operator attribution require the chosen identity/deployment setup. Shared database operator credentials are not tamper-proof human audit identity.

The outbox ambiguous-transport case uses fault injection; no fresh live Kafka test is claimed. Restore is synthetic, logical, same-cluster with roles pre-existing and an explicitly empty object backup manifest. No production TLS/key/secret/security/advisory/load/HA acceptance, fresh whole-provider suite or new binary reproducibility result is claimed. Earlier full integration/reproducibility records remain dated evidence, not results from this narrower rerun.

Reproduction: set `JAVA_HOME` to the reviewed JDK and `DZOKA_IT_CLASSES=FoundationIT,FoundationAdministrationIT,StartupConfigurationIT`, then run the native foundation harness with its documented PostgreSQL/Maven paths and `--full --restore-drill`. See the [foundation guide](../development/foundation.md) and [operator contract](../operations/foundation-administration.md).
