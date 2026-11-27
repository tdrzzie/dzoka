# Canonical places and recurring media reconciliation

The M3 extension follows [ADR 0067](../adr/0067-canonical-places-and-media-reconciliation.md) and appends migration V27. Existing migrations and provider/dependency pins remain unchanged. Run migrations separately from the API as documented in the [foundation guide](foundation.md).

## Place catalogue

Provision explicit local `places:read` and/or `places:manage` membership permissions through the [operator command](../operations/foundation-administration.md). Managing does not imply the separate API read permission; grant both to directory administrators. Tenant context and RLS apply to every table, reference and retry receipt. Public registration and token roles cannot grant these permissions.

Paths below start `/api/v1/tenants/{tenant}/places` and require verified identity/current membership. JSON bodies share the 16 KiB input limit and reject unknown fields:

| Method/path | Contract |
|---|---|
| `POST /` | Create with `expectedVersion:0` and `Idempotency-Key`; returns an opaque generated ID and immutable version |
| `PUT /{id}` | Rename, change aliases/context, reparent, archive or reactivate with the current expected version and a new logical key |
| `GET /?after={uuid}&limit=50&label={encoded-label}&includeInactive=false` | Bounded current keyset page, `items` and `nextCursor`; exact normalized name/alias lookup; omit optional parameters initially |
| `GET /{id}` | Current revision |
| `GET /{id}/revisions/{version}` | Historical immutable revision |
| `GET /{id}/ancestors` | Current chain, starting at the requested place and ending at the root |
| `GET /{id}/ancestors?version={version}` | Historical chain using the exact parent revisions recorded at that revision |

Example synthetic create body:

```json
{"name":"Joina City example","kind":"FACILITY","parentId":null,"latitude":-17.83,"longitude":31.05,"uncertaintyMeters":50,"timezone":"Africa/Harare","aliases":["Pachitoro example"],"active":true,"expectedVersion":0}
```

Kinds are COUNTRY, REGION, CITY, FACILITY, FLOOR, ROOM, ENTRANCE, STATION, TERMINAL, PLATFORM, ROUTE, VEHICLE, DEPOT and OTHER. A kind describes a place; it does not implement transport routing or inferred movement. Coordinates and uncertainty must either all be absent or all be present. Timezone is optional; provided values must be valid IANA zone IDs. Names/aliases retain exact accepted labels; separately stored normalization uses NFC, whitespace folding and Locale.ROOT case conversion with an explicit algorithm version. No transliteration or AI resolution is performed.

At most 32 aliases of 160 characters and 100 results per page are accepted. Equivalent aliases within one revision conflict; the same alias may legitimately refer to several places. Follow `nextCursor` to see ambiguity; no candidate is automatically selected. Queries are live views, not multi-request snapshots.

Hierarchy depth, including the leaf, is at most 32. Per-tenant serialization prevents concurrent reciprocal reparenting from creating cycles. Moves check the whole descendant depth; cross-tenant parent references fail. An active place requires active ancestors, and an active parent with active children cannot be archived. Archive children first or explicitly reparent them. Historical revisions remain accessible to authorized readers. No hard deletion API is exposed.

Each change commits pointer/revision/aliases, actor-private retry receipt, minimal audit and outbox together. Same key/body returns the original revision after later changes; a changed request conflicts. Membership is rechecked before replay. Labels and coordinates are not included in outbox payloads. Place names are operational metadata, not proof of custody, identity or ownership.

## Report linkage

Report JSON may additionally contain `placeId` and `placeVersion`; both are required together or both absent. The selected revision and all current ancestors must be active, and the version must be current at submission. The locations application interface validates under the report transaction's shared hierarchy lock, and tenant-qualified foreign keys retain exact historical references.

The report still preserves the exact raw JSON and `rawLocation`. No place name, coordinates or language inference overwrites them. Report reads return the pinned pair; subsequent place renames/moves do not rewrite old report revisions. Historical parent traversal follows the parent revisions recorded when that place revision was created; it is distinct from current ancestor traversal. A place revision can explicitly capture a newer parent snapshot when needed. Without an explicit link, canonical place evidence is unavailable. Staff or clients can inspect ambiguous alias results and submit a selected ID/version; this implementation does not guess the user's intent.

## Independent cleanup recovery

Durable media intents retain both application-generated source/derivative keys before external writes. After deletion/expiry/rejection, the purge worker revisits these retained tombstones even when quota was already released and no completion callback arrives. Legacy released tombstones with no sweep timestamp are eligible immediately. Successful checks defer recurring resweep for 15 minutes; immediate pending cleanup remains eligible sooner. A backlogged or unavailable service can take longer: this interval is not a certified erasure deadline.

Cleanup has its own random token, 120-second lease and generation. Concurrent workers skip a live lease; a crashed attempt is reclaimable. Stale acknowledgment cannot clear a newer generation or release quota again. Only successful deletion and matching acknowledgment advance `cleanup_checked_at`; storage/DB failure leaves work recoverable. Holds are serialized before cleanup initiation, and already-started erasure cannot be retroactively undone by a hold. Rejected assets can be reconciled without deleting active neighboring assets.

This specifically fixes source/derivative PUTs finishing after acknowledged erasure when the original writer crashes or loses its grant before its callback. It does not depend on resurrecting that writer's membership. Reads remain denied by the report tombstone throughout. Cleanup uses recorded intent keys, not arbitrary shared-bucket listing; unrelated/unknown bucket objects and external replicas remain operator responsibilities. Tombstones must not be discarded while future reconciliation is required.

## Recurring worker on the current setup

The existing `purge-media`, `sanitize-media` and `expire-intake` commands remain available. A new combined mode runs bounded expiry, sanitization and reconciliation each cycle:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-25.0.4'
$env:DZOKA_WORKER_LIMIT='10'
$env:DZOKA_WORKER_CYCLES='0'
$env:DZOKA_WORKER_INTERVAL_SECONDS='60'
python tools/core.py maintain-media
```

Supply private runtime DB, reviewed private S3 and explicit worker issuer/subject/tenant settings from the [media guide](intake-media.md). The identity needs `reports:purge`, `media:purge` and `media:process`. The process runs under the chosen operator account, never migration credentials. It invokes the isolated Docker decoder, which receives no service credentials. The environment example is documentation; it is not automatically loaded.

Cycles default to one; zero repeats until stopped; finite values up to 10,000 are supported. Interval is 15–3,600 seconds. Each cycle expires at most 100 reports and processes/purges at most the configured 1–100 operations per action. Storage privacy is rechecked each cycle. Transient in-cycle unavailability is reported and retried on later cycles; failed one-shot or unresolved final cycles exit nonzero. Invalid configuration/permissions fail closed. No service/task is silently installed. Use the selected host's supervisor/scheduler and restart policy for unattended operation; verify it on the target host. No claim of boot/HA acceptance follows from the finite local process test.

The supported generic upload profile remains PNG/JPEG with isolated canonicalization, not antivirus certification or arbitrary document/audio/video processing. Other modality providers retain their own reviewed contracts. Polygon/geofence scoring, transport networks, target replica/backup erasure and institutional privacy/security/retention acceptance remain explicit broader product/rollout work.
