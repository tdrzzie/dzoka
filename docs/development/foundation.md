# Running and verifying the foundation

This guide describes the M2 transactional foundation: organization creation/listing through verified JWT identity, local membership permissions, tenant-scoped SQL, idempotency, minimal audit and outbox. M3 adds [report intake and private images](intake-media.md); claim/recovery workflows and production AI remain future work.

## Build and dependency integrity

Use a reviewed Java 21+ JDK and Python 3.11+. Set `JAVA_HOME` to that JDK. The [adoption record](../dependencies/foundation-adoption.md) records tested runtimes and terms. Maven 3.9.16 is bootstrapped under `.audit-cache` with a pinned SHA-512; dependencies stay in a workspace-local repository.

```sh
python tools/audit_registry.py gate
python tools/maven.py dependency:resolve
python tools/dependency_lock.py
python tools/maven.py test
python -m unittest discover -s tests -v
```

`mvnw.cmd` on Windows and `sh mvnw` on Unix call the same launcher. The committed [dependency lock](../../dependencies.lock.json) covers 106 exact runtime/test JARs, POMs and inherited license evidence after M3 adoption, with explicit selected license alternatives. `--write` is a maintainer operation after adoption review, not the normal way to fix a mismatch. Regenerate `.audit-cache/dependency-tree.json` with `python tools/maven.py dependency:tree -DoutputType=json -DoutputFile=.audit-cache/dependency-tree.json` before an approved lock update. Full advisory scanning and a complete build-tool SBOM remain release work.

## Real integration verification

Docker route, using a reviewed Linux amd64 image by immutable digest:

```sh
python tools/test_foundation_container.py
```

Native PostgreSQL route:

```powershell
python tools/test_foundation.py --pg-bin 'C:\Program Files\PostgreSQL\18\bin' --maven 'D:\Projects\ai\dzoka\.audit-cache\apache-maven-3.9.16\bin\mvn.cmd'
```

Adjust absolute paths to your checkout/installations. On Unix run the native harness as an unprivileged user with a reviewed PostgreSQL binary directory and the cached Maven `bin/mvn` launcher. Both routes generate random credentials and a fresh isolated database, bind only loopback, run `verify`, and stop their own database. Migrations execute as a separate non-superuser database owner; only the test controller uses a superuser for provisioning/fault injection, while HTTP runs as non-owner `dzoka_app`. Docker removes its task-owned container/anonymous volume; native data stays in ignored `.test-tmp` for diagnostics. They never mount or read an existing server data directory. Failsafe integration tests fail when their database environment is missing; they are not silently skipped.

Tests use real RSA signatures, a local JWKS HTTP endpoint, the actual Spring bearer-token decoder, a running HTTP server and PostgreSQL row policies. No JWT-decoder mock or in-memory substitute database is used. The event transport fault-injection fixture tests ambiguous publication/retry semantics; it is not a live Kafka adapter or broker test.

## Provisioning a development or institutional database

Use an explicitly selected empty database. Provision separate migration/owner and runtime roles through an authorized operator. For example, in `psql` as an administrator:

```sql
CREATE ROLE dzoka_migrator LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOBYPASSRLS;
CREATE ROLE dzoka_app LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOBYPASSRLS;
\password dzoka_migrator
\password dzoka_app
CREATE DATABASE dzoka OWNER dzoka_migrator;
```

Use interactive passwords or a secret manager; never paste credentials into tracked files. Runtime must be exactly `dzoka_app`, must not own/inherit the schema/tables, cannot bypass RLS and receives no migration/admin membership. Runtime startup rejects unsafe roles or missing/disabled/unenforced row policies. The migration role is privileged for this database and belongs only in the operational migration process.

Populate `DZOKA_MIGRATION_URL`, `DZOKA_MIGRATION_USER` and `DZOKA_MIGRATION_PASSWORD` in the migration process environment, then run:

```sh
python tools/core.py migrate
```

Flyway applies checksum-validated versioned migrations; clean is disabled. API startup never runs privileged migrations. Add changes as a new migration rather than editing an applied one. M2 does not install PostGIS/pgvector; concrete compatible extension artifacts are reviewed before their retrieval/spatial milestone.

An authorized operator provisions tenant/membership rows in the selected database. `tenants` stores a UUID/name/active flag. `memberships` binds that tenant to exact OIDC `issuer` and `subject`, explicit permissions and an active flag. Grant `organizations:read`/`organizations:write` only to intended operators; service grants separately use `events:publish`/`events:consume`. No public route creates memberships or accepts client-chosen roles. Full identity-provider administration/organization management UI is future work.

## Start the API

Configure the process environment using the names in [.env.example](../../.env.example). It is documentation, not an automatically loaded dotenv file. Database and OIDC settings have no working fallback credentials/provider. Use TLS-verified database connections outside local development. Required OIDC issuer/JWKS URLs use HTTPS; explicit insecure-loopback configuration is reserved for the integration harness. The API trusts only RS256 tokens with valid signature, issuer, audience, expiry and nonempty subject. Authentication identifies the caller; local active membership controls permissions. An `ADMIN` token claim cannot grant local access.

After building the JAR with an integration harness or `python tools/maven.py package`:

```sh
python tools/core.py serve
```

The `core.py` launcher strips migration environment settings from the API child process, and strips runtime DB/OIDC settings from the migration child. The API process receives runtime database credentials and OIDC configuration; its eager database guard runs before the server accepts traffic. Supply a bearer token from the configured IdP; there is no demo login or plaintext password table.

| Endpoint | Requirement | Behavior |
|---|---|---|
| `POST /api/v1/tenants/{tenantId}/organizations` | local `organizations:write`, `Idempotency-Key` header, JSON `{"name":"Synthetic institution"}` | Atomically creates organization, audit, outbox and idempotency result; returns 200 with `id`, `name`, `version` |
| `GET /api/v1/tenants/{tenantId}/organizations` | local `organizations:read` | Returns at most 100 tenant-owned organizations ordered by ID; full pagination/management UI is not delivered |

Keys contain 8–128 ASCII letters/digits or `._:-`, scoped to tenant, verified actor and operation; only digests are stored. Same key/body replays the original result; a different body returns 409. Revoked membership is rechecked on replay. Unknown fields, invalid names/keys and malformed JSON return 400; JSON bodies over 16 KiB, including chunked bodies, return 413. Missing/invalid bearer tokens return 401, unauthorized membership/actions 403, and database unavailability 503. Error projections do not return SQL, parser payloads or secrets.

Outbox claims use expiring lease tokens; stale acknowledgment fails and ambiguous delivery can repeat the same event ID. Inbox records and local effects commit together; duplicate identical events do not repeat effects and changed envelopes with the same identity conflict. There is deliberately no default fake transport or automatically started publisher. A reviewed Kafka adapter, worker scheduling/DLQ/reconciliation and full operational packaging follow later.

## Acceptance scope

[FoundationIT](../../src/test/java/org/dzoka/FoundationIT.java) covers the foundation portions of A01–A04 (HTTP/SQL scope, same-connection cleanup, concurrent idempotency, atomic outbox and inbox replay), A13 (role assertions cannot grant access), A25 (database outage) and A27 (safe input/error projections). Domain/module/security-configuration tests cover value/UUID invariants and dependency boundaries. These do not cover future media/cache/vector/federation paths, matching/ownership release, full IdP account lifecycle or all outages in those acceptance rows. Real production OIDC integration, advisory/security assessment, backup/restore and load results remain outstanding.
