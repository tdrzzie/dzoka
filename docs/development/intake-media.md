# Report intake and private images

M3 extends the [foundation](foundation.md) with tenant-scoped cases, unresolved items, LOST/FOUND reports, immutable evidence revisions and private PNG/JPEG processing. A report records an observation; it grants neither ownership nor custody. [ADR 0026](../adr/0026-private-image-intake.md) defines this implementation profile. See [STATUS.md](../../STATUS.md) for executed verification and remaining work.

## Setup

Build with the reviewed dependencies and run the separate migration process described in the foundation guide. Migration V3 adds the intake/media schema; V1/V2 are unchanged. Provision active local memberships through an authorized operator. Owner operations use `reports:read` and `reports:write`; reading or changing someone else's report additionally requires `reports:read:any` or `reports:write:any`, respectively. Holding/unholding uses `reports:hold`. Service workers use separate memberships with `media:process`, or both `reports:purge` and `media:purge` for erasure. A token role cannot grant these permissions.

Text intake works with `DZOKA_MEDIA_ENABLED=false`. Media operations then return explicit 503 unavailability. To enable images, provision a private S3-compatible bucket and set the `DZOKA_MEDIA_*` environment settings in [.env.example](../../.env.example). Use HTTPS outside explicitly enabled loopback tests, explicit region/credentials, no anonymous/group ACL grants, no bucket policy and a bucket that has never enabled versioning. Startup verifies this supported profile and fails closed when it cannot do so. The API requires object read/write/delete and bucket ACL/versioning/policy inspection permissions; the adapter never creates a bucket or returns a storage URL. Identity grants, encryption, storage administration and preventing configuration drift are operator responsibilities. Review a production storage deployment separately; the pinned SeaweedFS server is a disposable test reference.

## API

All paths below begin `/api/v1/tenants/{tenant}` and require a signed bearer token plus current local membership. JSON is bounded to 16 KiB, including chunked requests; unknown fields are rejected.

| Method and suffix | Input and behavior |
|---|---|
| `POST /reports` | `Idempotency-Key`; report JSON below. Atomically returns `caseId`, `itemId`, `reportId`, `revision` and appends minimal audit/outbox records. |
| `GET /reports/{report}` | Current authorized private revision and context. |
| `GET /reports/{report}/revisions/{revision}` | Specific authorized immutable revision. |
| `POST /reports/{report}/revisions` | Same JSON and idempotency header; integer `If-Match` content revision. A stale revision returns 409; kind is immutable. |
| `GET /cases/{case}` or `/items/{item}` | Authorized reference with OPEN case or UNRESOLVED identity; no ownership/custody inference. |
| `DELETE /reports/{report}` | Tombstones the report; immediately blocks reads/previews. A legal hold returns 409. |
| `PUT /reports/{report}/hold` | `{"held":true}` or false; restricted hold permission. A hold cannot begin after irreversible media cleanup has started. |
| `POST /reports/{report}/media` | Raw `image/png` or `image/jpeg` body, at most 5 MiB, and idempotency header. Returns private metadata in QUARANTINED state after durable storage. |
| `GET /media/{media}` | Authorized processing state/size/dimensions/expiry/rejection code; no object keys or source URL. |
| `POST /media/{media}/retry` | Explicit retry of FAILED work; preserves cumulative attempts. |
| `GET /media/{media}/preview` | Only a current SANITIZED canonical PNG; `no-store`, `nosniff` and restrictive CSP. Other processing states return 409. |

Example report body (null evidence is permitted and remains unavailable):

```json
{"kind":"LOST","rawText":"Synthetic blue bag","language":"en","timezone":"Africa/Harare","rawLocation":"Near the entrance","rawTime":"Yesterday afternoon","latitude":null,"longitude":null,"uncertaintyMeters":null,"occurredAfter":null,"occurredBefore":null}
```

The accepted request bytes and raw labels are stored separately from versioned NFC/whitespace-normalized text. Language and a valid IANA timezone are required. Position is either entirely absent or includes latitude, longitude and nonnegative uncertainty (at most 200 km); zero coordinates are valid. UTC interval endpoints may be absent and must be ordered when both exist. Same actor/tenant/operation/key and identical request bytes replay the original receipt; a changed body returns 409. Missing, expired, deleted or unauthorized report/media resources return 404 within an authenticated tenant context. Invalid requests return 400, oversized input 413, unsupported media type 415 and unavailable storage 503.

## Processing and erasure

Run a separately operated trusted worker with runtime DB credentials and explicit `DZOKA_WORKER_ISSUER`, `DZOKA_WORKER_SUBJECT` and `DZOKA_WORKER_TENANT`. Its membership is checked from the database. `DZOKA_WORKER_LIMIT` bounds each media invocation to 1–100 operations (default 10). With the built classes and verified dependency cache present:

```sh
python tools/core.py sanitize-media
python tools/core.py expire-intake
python tools/core.py purge-media
```

Sanitization additionally requires Docker on the trusted worker host and the exact reviewed JRE image in [adoption evidence](../dependencies/intake-media-adoption.md). Run the Unix worker as an unprivileged operator. The API needs no Docker socket. The child decoder receives only staged code/input and disposable output directories: no credentials/socket, no network, non-root, read-only root, dropped capabilities, and CPU/memory/PID/time limits. It checks dimensions before decoding (8192 maximum per dimension, four million pixels), rejects malformed/APNG/trailing PNG content, copies pixels into a new PNG (maximum 20 MiB), then the parent validates its bounded metadata-free structure. Original objects are never previewed. This is format isolation/canonicalization, not antivirus certification or support for SVG, PDF, audio or video.

Leases last 120 seconds. The worker can reconcile an UPLOADING intent after 30 seconds; same-key immutable PUT also recovers interrupted storage writes. Transient failures retry to a budget of three, then become FAILED with source preserved for explicit retry. Invalid content becomes REJECTED and eligible for governed cleanup. Only a matching current lease can activate a derivative; late results cannot reactivate deleted/expired reports.

Default tenant retention is 30 days (operator-configurable 1–365); the byte reservation is 25 MiB per asset, ten active assets per report and 500 MiB per tenant by default. The reservation covers the maximum source plus derivative. Processing does not run inside a database transaction. Holds serialize against cleanup initiation. Expiry erases raw revisions and leaves minimal tombstones; object purge deletes actual source/derivative keys and releases reservations exactly once. A completion that observes a late write increments the cleanup generation so an older acknowledgment cannot suppress another purge. Operators must schedule/retry these bounded commands; no scheduler is silently installed. A process crash or membership revocation between a late external write and its database acknowledgment can leave inaccessible orphan bytes; independent bucket/tombstone reconciliation remains required. Backup erasure, restore tombstone replay, replica/lifecycle reconciliation and deployment-specific retention policy also remain production work.

## Real verification

Full Docker route (real PostgreSQL, private S3 and isolated decoder):

```sh
python tools/test_intake.py
```

Native S3/PostgreSQL plus the same Docker decoder:

```powershell
python tools/test_intake.py --native-s3 'D:\Projects\ai\dzoka\.audit-cache\seaweed-native\weed.exe' --pg-bin 'C:\Program Files\PostgreSQL\18\bin' --maven 'D:\Projects\ai\dzoka\.audit-cache\apache-maven-3.9.16\bin\mvn.cmd'
```

Verify the native release archive SHA-256 in the adoption record before selecting that executable. Both routes create synthetic data/random credentials and stop their own services; diagnostics remain ignored. `--storage-only` explicitly excludes decoder tests and is never evidence of sandbox verification. The foundation harness without `--full` explicitly runs only database/API tests. Full tests fail if a required real provider is absent; no test provider substitutes for production storage/decoding. CI is configured for the full Docker route; a workflow file alone is not a remote execution result.
