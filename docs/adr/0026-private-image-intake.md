# 0026: Preserve raw intake and publish only isolated image derivatives

- Status: Accepted implementation profile; provider/sandbox verification tracked in STATUS.md
- Date: 2026-10-26

## Context

M3 must add useful intake without treating a report as ownership or allowing untrusted uploads into previews/AI. Source review and the accepted architecture require immutable source evidence, tenant-scoped storage, bounded isolated decoding, explicit failure and retention. An object write and database transaction cannot commit atomically.

## Decision

Submission atomically creates distinct case, unresolved item and LOST/FOUND report references. Preserve exact accepted request bytes, original text/language/timezone/location/time labels and interval/position uncertainty in append-only revisions; normalized text has an explicit algorithm version. Keep content revision separate from aggregate event version. Report creation never records custody or ownership.

Stage a durable media intent before S3, use generated immutable tenant/media keys plus content digests, then mark quarantine only after the object write. Retry the same logical intent and reconcile through a leased worker; no source download/preview is exposed. The initial media profile accepts bounded PNG/JPEG only. A trusted core worker invokes a non-root, network-disabled, read-only, resource/time-limited Docker decoder with no service credentials. Canonical PNG output contains decoded pixels and no source metadata. Only current authorized sanitized manifests can be served; unavailable processors produce FAILED work with preserved source, not fabricated success or an image mismatch.

Use the reviewed S3 SDK behind PrivateObjects. Require private canonical-user ACLs, no bucket policy, and no versioning/history for this initial erasure profile; unverifiable storage fails closed. The operator supplies explicit credentials/region/endpoint. The test reference is pinned SeaweedFS; no storage server is bundled as the mandatory production distribution.

Tenant byte reservations, per-report limits, leases and retry budgets bound work. Holds serialize with irreversible cleanup initiation; a hold cannot retroactively restore removed bytes. Deletion/expiry immediately blocks API access, then governed workers erase revisions and delete objects. Durable tombstones/generations prevent late upload/worker results or stale cleanup acknowledgment from resurrecting data. Backups and broader legal-policy rules remain operational hardening work.

## Alternatives

In-process decoding gives untrusted bytes the API's filesystem/network authority. A filesystem-only production adapter would bypass the accepted S3 boundary. Returning presigned original URLs would bypass current quarantine/expiry checks. Pretending that re-encoding is antivirus certification would make an unsupported security claim. Arbitrary document/audio/video acceptance waits for separately reviewed isolated providers.

## Consequences

Media can be explicitly disabled while text intake works; enabled media requires real storage and its supported privacy profile. Decoder outage is observable and never activates originals. Successful storage tests alone do not prove the sandbox or full media policy. Production distribution, malware/content policy, broader formats, retention/backups and deployment sandbox assessment remain outstanding.

## Source-audit evidence

- [Globalenglish](../source-audit/platforms/globalenglish-lostfound.md), `6ff8cffe55bfc3e7274124512a69fae746ceb643`: caller-supplied authority must not determine release/access.
- [KWASU](../source-audit/platforms/kwasu-lost-found-bot.md), `e4863739135594d8ce0a541be6029d0bba5ae95c`: monolithic intake/provider behavior does not establish secure durable integration.
- [Tenant enforcement ADR 0025](0025-tenant-security.md), [PII ADR 0021](0021-pii-local.md), and [runtime adoption evidence](../dependencies/intake-media-adoption.md) govern the concrete implementation. Exact dependency JAR/POM digests are in the committed lock.

## Implementation implications

Test raw-byte preservation, revision concurrency, ownership-independent item references, same/cross-tenant denial, atomic outbox rollback, immutable object writes, unavailable storage/processors, lease expiry, malicious/mismatched images, metadata stripping, legal holds, late completion and actual byte erasure. Report verified and unverified provider paths separately. This milestone does not implement AI matching, ownership verification or property release.
