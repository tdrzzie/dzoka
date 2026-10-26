# Operational procedures and activation limits

2026-10-26. These procedures cover the current foundation/intake/text/perception slice and safe containment for later adapters. They are not evidence that unimplemented services, automated alerting or production recovery have been exercised. Use synthetic data for drills. Record safe incident/tenant/operation references and timestamps; keep tokens, contacts, answers and raw media out of diagnostic tickets. [Local readiness](../operations/local-readiness.md) documents executable verification and the deployment boundary.

## Database restore

Stop API ingress, publishers and worker scheduling. Restore into an isolated target with no outbound effects; do not overwrite the live database. Keep the reviewed schema/artifacts, role provisioning and decryption/key availability under separate operator control. Preserve owners, grants and forced RLS. Compare migrations, domain/event/job rows, constraints and permissions; test missing-context/two-tenant denial using runtime credentials. The executable synthetic drill is `python tools/test_intake.py --with-ai --restore-drill`.

Before serving a real restore, reconcile deletions, revocations and holds newer than the snapshot; purge source/derived objects and indexes as required; reconcile pending external operations by stable identity. The independent journal/replay/release barrier and object-backup restore remain unimplemented: a successful database-only drill must not release restored private data to traffic. Record measured recovery time/loss only for the exact topology actually exercised.

## Event replay

Stop the affected publisher/consumer and identify the bounded tenant, consumer and event range. Preserve the original event ID, schema, aggregate version, correlation and logical provider-operation key. Verify source facts and inbox deduplication; do not clear inbox records, invent fresh event IDs or mark unknown external effects successful. Reconcile external receipts before retrying effects. Resume a bounded batch, inspect duplicate/conflict outcomes and audit the replay request. Outbox/inbox transactional contracts are tested; a live Kafka transport, DLQ/replay operator command and broker recovery drill are still required before activating this procedure against a broker.

## Stuck AI jobs

Check the tenant's authorized private job status, source revision, attempts, deadline and space activation. Check the artifact/runtime probe and worker service membership without printing the token or input. Restart the private consumer only after correcting the cause; expired 120-second leases can be reclaimed through the normal claim API, up to three attempts and the one-day deadline. Do not extend leases, reset attempts or change model/space metadata directly to force success. A disabled/corrupt model stays unavailable. Stop retrying terminal work; preserve safe reason codes and use a new reviewed space/revision only when semantically warranted. Quotas are tenant-wide; deleting other actors' jobs is not a remedy.

## Federation peer compromise

Before federation exists, keep it disabled. Once implemented, immediately revoke the peer/key trust and stop fan-out/export to it; invalidate in-flight grants and preserve minimized receipt/replay evidence. Reauthorize affected results under local policy; no peer statement can approve local release. Rotate trust through an authenticated independent channel and retest replay/audience/revocation/partition cases before resuming. Peer registry/revocation and a real two-node drill are M8 local implementation obligations, not delivered controls.

## Lost signing key

Stop signing with the affected key and revoke trust for its key ID; do not generate a same-ID replacement or accept unsigned fallback. For the current API, authentication keys belong to the configured IdP: stop/revoke affected local memberships or tenant access while the IdP rotates keys and tokens, and verify rejected old signatures plus accepted authorized new signatures. Test cache/expiry behavior against the actual IdP before reopening. Federation/application signing-key issuance, archival verification and emergency revocation require their own implementation and external trust ceremony.

## Object-store incident

Stop upload/sanitization workers if bucket privacy or credentials are uncertain; block media-serving ingress until revalidated. Keep text intake available only if its independent controls are sound. Confirm bucket ACL/policy/versioning and authorized immutable object digest/length checks. Preserve manifests and durable failed/pending intents; do not preview raw/quarantined objects or replace missing data with another object. Correct provider configuration, rerun actual S3/sandbox checks and retry bounded intent. Reconcile orphan/late writes and tombstones before closing; independent bucket reconciliation and replica/backup cleanup remain outstanding.

## Payment reconciliation

Payments are not implemented and must stay disabled. For a future ambiguous timeout, freeze the bounded operation/settlement, retain its logical key and query the provider receipt before retrying. Authenticate callbacks and deduplicate them; balance immutable ledger entries and distinguish reserved/submitted/confirmed/refunded/disputed/unknown outcomes. Never settle from a match score or delivery notification alone. Require payment adapter, ledger race tests and real provider sandbox reconciliation before activation.

## Courier reconciliation

Courier booking is not implemented and must stay disabled. For a future unknown booking, retain the original operation key, query provider status and prevent a second booking until resolved. Authenticate and sequence callbacks; delivery does not itself confirm authorized owner receipt. Append custody corrections/disputes rather than overwriting facts. Require provider sandbox, duplicate/out-of-order callback and lost-response tests before live dispatch.

## Privacy deletion request

Authenticate and authorize the request without collecting extra secret evidence. Resolve tenant scope and valid legal holds through authorized policy. Tombstone the report through its DELETE API; reads/previews must deny immediately. Use separately granted maintenance identities for `expire-intake`, `purge-ai` and `purge-media`; repeat bounded batches, verify actual object deletion and reservation release, and retain only permitted minimal audit facts. Do not remove holds or claim backup/replica/index erasure from a source-row deletion. Register and reconcile deletion across every destination and restored snapshot; the independent restore journal remains a prerequisite for production closure.

## Leaked credentials

Contain affected ingress/worker roles and revoke the credential at its issuer/provider/database. Never paste suspected values into logs or this repository. Rotate scoped credentials, update only their authorized secret delivery and restart affected processes; ensure old credentials no longer authenticate. A stolen SQL runtime credential is not contained by transaction context alone: inspect all reachable tenant data and writes, not only one caller's declared tenant. Preserve minimal access/audit evidence and apply institutional notification/response policy. Production secret service, audit retention and accountable contacts must be established before release.

## Model rollback

V7 extends the same controlled rollout to explicit perception pins: verify the retained source/profile/input/output identities and current epoch before rollback. A completed vision/OCR/speech job remains SHADOW until explicitly selected. The core recomputes profile keys from stored manifests and rejects tampering; never repair a mismatch by editing an existing approved row or suppressing its checks. Wider model/format/language quality and deployment isolation remain separate from successful cohort rollback. [Provider instructions](../development/perception.md) apply.

Stop the affected worker; an authorized database operator can set the exact tenant/embedding-space row to `enabled=false`. That blocks new claims/completion and excludes components from current reads. Keep immutable provenance/history subject to retention; never overwrite vectors or treat equal dimensions as compatibility. A previously reviewed space can be enabled only with its exact artifact/configuration and permitted current-source components. For delivered text cohorts, a separate `fingerprints:manage` operator can read `/internal/fingerprints/rollouts/active`, then POST a previously activated valid cohort's `/activate` with the current `expectedVersion` and `rollback:true`; [exact paths and checks](../development/fingerprint-rollouts.md) apply. Revised/expired/deleted inputs, disabled spaces or erased components reject rollback. Emergency model disabling remains distinct from cohort activation. Multimodal/larger-cohort/index rollout and quality evaluation remain work. Re-run artifact and real inference checks before resuming.

## False-positive matching incident

Matching/recovery are not implemented. When they are, suspend affected automatic candidate promotion and any release relying on disputed evidence; existing custody remains factual. Preserve compatible-space, signal, coverage, hard-contradiction, policy and calibration versions without disclosing private answers. Reassess under accountable human/policy authority, fix the bounded defect and evaluate hard negatives with leakage-resistant fixtures. A similarity threshold change must not silently rewrite verification or authorize release. No accuracy or calibrated probability claim is established by current text inference.

## Tenant data isolation incident

Block the affected API/provider/export path, stop corresponding workers and revoke compromised memberships/credentials. Preserve minimal incident evidence with restricted access. Identify affected rows, object grants, jobs and destinations under a trusted operator; do not trust a client-supplied tenant filter as the investigation boundary. Verify forced RLS, non-owner roles, tenant-qualified references, pooled connection reset and downstream isolation. Correct and rerun real two-tenant adversarial tests plus the affected adapter before reopening. Current DB/HTTP/media/AI paths have bounded tests; future cache/vector/federation surfaces need equivalent tests when implemented. Follow accountable incident/notification policy before closing.
