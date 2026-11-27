# 0067: Versioned places and recurring media tombstone reconciliation

- Status: Accepted implementation decision; execution recorded separately
- Date: 2026-11-27

## Context

The M3 recheck found raw location/coordinate support without canonical places, and no independent cleanup after a late object PUT loses its database acknowledgment. Neither gap requires external infrastructure to implement.

## Decision

Create a tenant-scoped place catalogue with immutable name/type/alias/coordinate/timezone revisions, current pointers and a bounded acyclic parent hierarchy. Preserve raw labels beside versioned Unicode/whitespace/case normalization. Alias ambiguity returns multiple candidates. Separate read/manage permissions, optimistic versions, actor-scoped retry receipts and atomic audit/outbox protect administrative changes. Serialize hierarchy changes per tenant; disallow cycles, excessive depth and archiving an active parent with active children. Reports optionally pin an exact place revision without rewriting raw location text; absent links remain unavailable. Place resolution does not grant custody or ownership.

All application-generated source/derivative object keys already have durable media intents before PUT. Retain these tombstones and periodically revisit deleted/rejected assets even after reservation release. Add exclusive expiring cleanup leases and a persisted last-checked time. Repeated deletes are idempotent, stale acknowledgments cannot release quota twice, and holds are checked before cleanup initiation. Reconciliation is bounded and repeats on later worker invocations; an acknowledgment is not treated as permanent proof that an in-flight PUT cannot complete later.

No runtime dependency is added. The supported media format boundary remains text/PNG/JPEG; broader scanner/format adoption is a separate explicit profile, not a silent fallback.

## Alternatives

Treating labels/GPS as canonical identity drops hierarchy and ambiguity. Updating place rows in place loses report provenance. Auto-selecting an alias fabricates certainty. Deleting tombstones or trusting only completion callbacks cannot recover unacknowledged late writes. Listing an entire shared bucket risks unrelated objects and is unnecessary for keys already recorded before writes.

## Consequences

Current and historical place references remain inspectable under tenant permissions. Tree and alias queries have explicit limits. Coordinates stay optional and uncertain; no inferred geofence or route is invented. Reconciliation requires repeated worker scheduling and accessible storage; its interval is not a guaranteed production erasure SLO. Unknown objects outside application-owned intent keys and external replicas/backups remain operator governance. No live deployment or production acceptance follows from a local test.

## Source-audit evidence

[ADR 0002](0002-postgresql-authority.md), [ADR 0025](0025-tenant-security.md), [ADR 0026](0026-private-image-intake.md), the [baseline](../architecture/baseline.md) and [existing adoption](../dependencies/intake-media-adoption.md) govern this implementation. Their canonical citations, license scope and limits remain unchanged. The source gate passed before implementation; no upstream source or new dependency is incorporated.

## Implementation implications

Append V27; preserve applied migrations. Test cross-tenant reads/FKs, role denial, raw labels and alias ambiguity, concurrent cycles/versioned retries, historical report references, real S3 late writes with no acknowledgment, lease takeover, holds, quota release, migration/restore and full regression. Record remaining target scheduling/security/retention acceptance honestly.

[Executed completion record](../testing/m3-implementation-2026-11-27.md) records results and deployment limits.
