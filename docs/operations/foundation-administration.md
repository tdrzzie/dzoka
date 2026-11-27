# Foundation administration

The local M2 operator profile follows [ADR 0066](../adr/0066-foundation-administration.md). It manages Dzoka tenant state and local OIDC membership permissions. Account enrollment, passwords, MFA and upstream session revocation belong to your selected IdP. No new identity server is bundled.

## Operator boundary

Use a separate operator shell with the reviewed JDK in `JAVA_HOME`. Provision the existing `dzoka_migrator` database/schema owner as documented in the [foundation guide](../development/foundation.md). It must not be superuser or bypass RLS. Apply migrations first with `python tools/core.py migrate`. V26 adds owner-only policies for these explicit operator operations; the API remains `dzoka_app` and cannot read operator receipts or grant memberships.

Supply `DZOKA_MIGRATION_URL`, `DZOKA_MIGRATION_USER`, `DZOKA_MIGRATION_PASSWORD`, or their individual `_FILE` variants. A value and file for the same setting conflict. Files stay private and outside Git. The operator launcher strips API/provider/AI/storage and Spring configuration from its child environment. Never run this command in the HTTP API process or give API workers migration credentials.

## Commands and concurrency

Send one UTF-8 JSON document (maximum 16 KiB) to `python tools/core.py admin-foundation` through standard input. The following are synthetic examples; use your deployment's exact verified issuer and subject, never a token or password. Keep operational input files private: issuer/subject identifiers are not public directory information.

Create a tenant, with an operator-chosen UUID and expected version zero:

```json
{"kind":"TENANT","tenant":"aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa","name":"Synthetic institution","active":true,"expectedVersion":0,"idempotencyKey":"tenant-create-example"}
```

Provision a local member within that tenant:

```json
{"kind":"MEMBERSHIP","tenant":"aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa","issuer":"https://identity.example.invalid","subject":"synthetic-member","permissions":["organizations:read","organizations:write"],"active":true,"expectedVersion":0,"idempotencyKey":"membership-create-example"}
```

For an existing tenant or membership, send its current `expectedVersion`, a new logical idempotency key and the desired complete state. The response contains only an opaque operation ID, resulting version and active flag. Empty permissions revoke all local permissions; `active:false` disables membership. Tenant deactivation blocks all its members; the operator can reactivate it. State is preserved rather than deleted. Operator grants are deliberate privileged choices, not permissions inferred from a token role. Review every granted permission against the relevant module contract.

Versions start at one; zero means the entity must not exist. Concurrent changes serialize per tenant and stale versions fail. Identical logical retries return the original receipt; a changed request using the same key conflicts. Successful changes and append-only receipts commit together. A receipt contains database role, hashed target/input and result version/status, without raw issuer, subject, permission list or names. A shared database role identifies the operator credential, not a separately authenticated human; individual human attribution requires deployment-controlled access and external operator audit. Database owners retain authority to alter the schema and are not a tamper-resistant security boundary.

Local HTTP issuer configuration requires the explicit diagnostic `DZOKA_ADMIN_ALLOW_INSECURE_LOOPBACK=true` and only accepts `127.0.0.1`; leave it unset for real deployments. Production issuers require HTTPS. Invalid input, unsafe credentials, version conflicts and database errors produce a fixed failure message and nonzero exit code, without printing parser/SQL/input/credential details. Inspect current versions through an authorized private operator SQL session if the original receipt is unavailable.

Local membership is rechecked on each new request, including idempotent replays. Revocation does not cancel a transaction already authorized and running. For signing-key rotation, publish overlapping keys before using a new key ID; retain overlap until token lifetime/cache requirements expire. Removing an old key is not guaranteed to revoke cached tokens immediately. Use local membership revocation for immediate denial of subsequent requests and follow the selected IdP's emergency rotation process.

## Organization management

Existing organization create/list response shapes remain supported. The legacy list contains up to 100 active organizations. Use these tenant-scoped endpoints for complete traversal and lifecycle:

| Endpoint | Behavior |
|---|---|
| `GET /api/v1/tenants/{tenant}/organizations/page?limit=50&after={uuid}&includeInactive=false` | `items` plus optional `nextCursor`; limit 1–100; omit `after` initially; read permission required |
| `GET /api/v1/tenants/{tenant}/organizations/{id}` | Current state including active/version; retained archived references remain readable to authorized members |
| `PUT /api/v1/tenants/{tenant}/organizations/{id}` | Body `{"name":"Synthetic institution","active":false,"expectedVersion":1}`, plus `Idempotency-Key`; write permission required |

Rename, archive and reactivate use the same versioned PUT. They atomically append audit/outbox and an actor-private retry receipt. Retries return the original response even after later mutations; changed bodies conflict. Archive hides an organization from default discovery without erasing foreign-key or custody history. It does not deactivate the whole tenant or its members. Cursor traversal is a live keyset view, not a snapshot across concurrent mutations. Hard deletion and arbitrary private-data exports are intentionally not administrative defaults.

## Deployment acceptance

This operator command and the resource-server rotation/concurrency tests run locally without an institutional IdP account. Actual IdP enrollment/recovery/MFA configuration, target TLS and secret storage, operator identity attribution, advisory/security assessment, load/HA and institutional acceptance require evidence from the selected deployment. They are not established by a local fixture or by changing a milestone label.
