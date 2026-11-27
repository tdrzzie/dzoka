# M2 foundation revalidation

Subsequent work on this date adds the [completed local administration extension](m2-implementation-2026-11-27.md): pagination/lifecycle, immutable retries, tenant/member operator commands and expanded verification. The inspection below records the pre-extension code and its original scope.

Checked 2026-11-27 against the defined [M2 deliverable](../architecture/milestones.md), [foundation contract](../development/foundation.md), [adoption record](../dependencies/foundation-adoption.md), implementation and executed tests. A completion label was not treated as evidence.

**Verdict: the scoped M2 transactional foundation is implemented and passes fresh local verification. Production identity administration, complete organization management and target deployment/security acceptance are not established by this milestone.**

## Code and behavior checked

| M2 responsibility | Actual implementation and fresh evidence |
|---|---|
| Domain/build foundation | Organization and verified-identity invariants; module import boundary checks; reviewed Maven/JDK inputs and pinned dependency inventory |
| Database and migrations | V1 creates tenant, membership, organization, idempotency, audit, outbox and inbox tables with tenant keys, grants and forced RLS; V2 binds idempotency/audit to verified actor. The fresh current-checkout run applies and validates all 25 migrations and verifies a second migration is a no-op |
| Authentication and local policy | Actual Spring RS256 bearer decoder against ephemeral RSA/JWKS HTTP fixture; issuer/audience/expiry/signature checks, unsigned/malformed token denial, local active membership and tenant checks; asserted ADMIN roles cannot grant access |
| Tenant/role isolation | Transaction-local SQL context, explicit predicates and RLS; same pooled backend reused without prior tenant context; cross-tenant reads/inserts and runtime privilege escalation denied; migration/superuser credentials rejected by runtime guard |
| Idempotency and atomic mutation | Six concurrent HTTP requests produce one organization/audit/outbox mutation; changed body conflicts; actor/tenant scopes differ; injected outbox failure rolls back the command and retry succeeds |
| Durable event foundation | Concurrent inbox delivery applies one local effect; crash rollback permits retry; changed envelope conflicts; expired outbox lease rejects stale acknowledgment; ambiguous publication retries the same event identity |
| Failure/input boundaries | Database login outage returns unavailable; oversized and unknown-field JSON rejected; packaged application refuses startup without required configuration |

These are executable implementations, rather than interfaces or mocked database behavior. The event transport fault injection is deliberately not a live Kafka test. The external identity fixture verifies resource-server behavior, rather than institutional account lifecycle or production OIDC interoperability.

## Executed checks

- `python -B tools/audit_registry.py gate`: PASS.
- `python -B tools/dependency_lock.py`: PASS, 106 exact artifacts/POMs/license declarations.
- `python -B -m unittest discover -s tests -p test_dependency_lock.py -v`: five passed, including modified bytes, path escape, declaration changes and unreviewed license rejection.
- Native PostgreSQL harness, with `JAVA_HOME=C:\Program Files\Java\jdk-25.0.4` and `DZOKA_IT_CLASSES=FoundationIT,StartupConfigurationIT`: Maven `verify` PASS; **36 Java unit tests plus 15 foundation integrations, zero failures/errors/skips**. The unit total covers the current codebase; three of these suites directly cover M2 domain/module/security configuration. The isolated database was stopped by the harness.

Reproduction command (PowerShell; set the two environment variables above first):

```powershell
python -B tools/test_foundation.py --pg-bin 'C:\Program Files\PostgreSQL\18\bin' --maven 'D:\Projects\ai\dzoka\.audit-cache\apache-maven-3.9.16\bin\mvn.cmd' --full
```

Here `--full` enables the harness's explicit `DZOKA_IT_CLASSES` selection. Only the two named foundation integration classes ran; this is not the full M11/media/provider suite. [Sanitized receipt](artifacts/m2-recheck-20261127/summary.json) records source hashes, base commit, test names/counts and raw-report digests. Raw XML/logs and ephemeral credentials remain ignored. The first attempt using PATH Java stalled before Maven output and was interrupted; it contributes no passing result. The explicit reviewed JDK retry completed successfully. No system Java configuration was changed.

## Limits and remaining work

No missing implementation was found within the explicitly scoped M2 contract. Organization delivery is create/list (bounded to 100), with operator-provisioned memberships; full management/pagination and IdP account administration are not delivered by that contract. Live broker distribution, broader domain workflows and later isolation surfaces belong to later milestone records and are not certified by this rerun. Current migration validation is not an independent replay of the historical M2 checkout.

Production IdP configuration/lifecycle, institutional roles, external TLS/key rotation, target security/advisory assessment, load/HA and deployment acceptance require their own evidence. Dependency byte/license integrity does not establish vulnerability absence or byte-for-byte reproducible output; no clean-room binary reproduction was performed here. No fresh remote CI, restore, media, AI or external-provider test is claimed. Historical “future work” in October guides must be read alongside later ADRs and milestone evidence, rather than interpreted as today's complete backlog. No application code or milestone scope was changed for this check.
