# Architecture acceptance scenarios

Design requirements for implementation. The M2 evidence table below records executed foundation portions; broader paths remain pending. Source-audit checks validate research metadata only. Each scenario must acquire an automated test location/result or a reproducible manual verification record as its milestone lands. Use synthetic/consented fixtures, no real private recovery data.

| ID | Invariant or failure injection | Required outcome | Milestone |
|---|---|---|---|
| A01 | Two tenants guess each other's IDs through HTTP, SQL, vectors, media, cache and jobs | No unauthorized data or mutation; safe denial and minimal audit | foundation, repeated at every adapter |
| A02 | Reuse pooled connection after tenant A, then missing/B context | Transaction-local context resets; missing context denies; B never reads A | foundation |
| A03 | Concurrent same idempotency key, including different request body | One logical mutation/response; body conflict fails | foundation |
| A04 | Crash before/after DB commit and broker acknowledgment | State plus outbox commit together; eventual publication may duplicate without duplicate effects | foundation/events |
| A05 | Raw text/timezone/uncertain place normalizes or later expires | Exact authorized original remains until policy expiry; revisions retain provenance; expiry covers derivatives | intake |
| A06 | Oversize/polyglot/malformed media, decoder crash or scanner outage | Quarantine; bounded isolated failure; no preview/inference grant before sanitation | media |
| A07 | Expired lease, duplicate completion, wrong tenant/model/input version | No stale/cross-tenant activation; durable progress and bounded retry | AI jobs |
| A08 | Missing model/checksum failure/provider returns invalid schema | Explicit unavailable/failed; no random or heuristic production substitute | AI providers |
| A09 | Same-dimension incompatible vectors; partial re-embedding; deletion | Reject incompatible comparison; controlled activation/rollback; no resurrection | fingerprints |
| A10 | No image/all missing/zero reliability/correlated signals | Unavailable differs from mismatch; no divide-by-zero or fabricated similarity; coverage separate | scoring |
| A11 | Strong verified serial conflict with high visual similarity | Block candidate acceptance; uncertain OCR discrepancy remains uncertainty | matching |
| A12 | ANN filters, route timeout and reranking budget | Measure exact-baseline Recall@K; report partial routes and truncation; bounded heavy work | retrieval |
| A13 | Arbitrarily high match score or public registration role=admin | Neither grants ownership, elevated role, contact disclosure or release | auth/verification |
| A14 | Repeated challenge attempts across sessions/actors; publicly disclosed answer | Enforce item/actor disclosure limits; reject unsafe challenge; no secret answer in response/log/event | verification |
| A15 | Competing claims race, hold added, stale verification or policy changed | Only current authorized entitlement succeeds; locked transactional recheck | recovery |
| A16 | Concurrent redemption, expired/wrong audience token, retry | One custody transition; token single use; response idempotent and privacy-safe | handover |
| A17 | Out-of-order delivery callback, unknown courier booking, payment timeout | Reconcile stable operation before retry; delivery not automatic owner receipt; no double booking/settlement | integrations |
| A18 | Payment reserve/release/refund/dispute races and callback replay | Balanced immutable ledger and one eligible settlement; unknown external state reconciled | rewards |
| A19 | Signed federation replay/audience swap/revoked key/partial fan-out | Reject untrusted requests, bounded cost, attributed partial result, no PII/secret export | federation |
| A20 | Peers return incompatible/raw scales or absent calibration | Rank-level discovery only; no invented global probability/approval | federation |
| A21 | EPCIS duplicate capture, mapping correction and replay | One logical projection per source/version; explicit supported subset/privacy policy | traceability |
| A22 | CCTV grant revoked mid-job, gaps/occlusion and unauthorized camera | Cancel/restrict reads, minimize/redact exports, preserve uncertainty; no facial recognition default | edge |
| A23 | Predicted transition conflicts with observed custody | Observed/inferred/predicted edges stay distinct; prediction cannot authorize release | intelligence |
| A24 | Retention expiry/deletion/legal hold; restore old backup | Governed erasure and valid holds across sources/derivatives; tombstones reapplied before serving restored indexes | operations/privacy |
| A25 | DB/broker/AI/OIDC/vector outage and disk/queue exhaustion | Fail closed for authorization, preserve committed work, bounded backpressure and visible degradation | each milestone |
| A26 | Upgrade/rollback, restore and projection rebuild | Reproducible migrations/artifacts; consistent state; rebuild from permitted canonical data without reissuing external effects | operations |
| A27 | Trace/log/metric/event/API export inspection | No raw answers, private contacts, tokens, unredacted documents or secret identifiers | each milestone |
| A28 | Offline transfer conflicts with newer server custody | Pending conflict/review, no silent overwrite or unilateral stale release | custody/edge |
| A29 | Dataset splits leak same item/institution/time or calibration is absent | No accuracy/probability claim; compare baseline, report slices and uncertainty before promotion | evaluation |

Performance/availability targets, retention durations, legal-policy defaults and calibration thresholds must be selected with deployment context and measured fixtures. No arbitrary latency/accuracy target is presented as achieved. Full production acceptance also requires all 100 specification sections, operational/security evidence and a complete report-to-confirmed-recovery workflow.

## Executed foundation portions (2026-10-26)

[FoundationIT](../../src/test/java/org/dzoka/FoundationIT.java) and [startup test](../../src/test/java/org/dzoka/StartupConfigurationIT.java) exercise real PostgreSQL/JWT/HTTP behavior; [run instructions](../development/foundation.md) state exact scope and runtime. This is not completion of every surface in the broader acceptance rows.

| Acceptance IDs | Verified foundation portion | Still pending |
|---|---|---|
| A01–A02 | HTTP/SQL tenant denial, local membership/role/policy guards, same-backend pooled transaction cleanup | M3 media portions recorded below; cache, AI jobs, vectors, federation, full admin workflows remain |
| A03 | concurrent idempotent creation, different-body conflict, actor/tenant scoping, atomic original response | provider effects and later domain commands |
| A04 | transactional organization/audit/outbox rollback, inbox crash/concurrent replay, lease/ambiguous transport behavior | live Kafka, broker failure/rebalance, DLQ and replay operations |
| A13 | asserted ADMIN token role cannot grant membership or write | similarity/ownership/release workflows not implemented |
| A25 | database connection outage returns 503; packaged app rejects missing configuration | other provider, IdP, broker and storage outage drills |
| A27 | bounded/unknown-field JSON and safe validation/error projections, minimal event/audit payload design | full export/telemetry/privacy assessment and later integrations |

No accuracy, load, recovery, backup/restore or production-readiness result is claimed.

## Executed intake/private-image portions (2026-10-26)

A clean build passed 5 Java unit checks and 36 integration tests on native PostgreSQL 18.6/SeaweedFS 4.51 plus the pinned Docker Temurin decoder. The all-Docker PostgreSQL/S3/decoder route independently passed the same suite, including the strengthened late-write/stale-cleanup acknowledgment case. The host JDK was Oracle 25.0.4 with Java 21 bytecode; remote CI and its exact host JDK are unverified. [IntakeIT](../../src/test/java/org/dzoka/IntakeIT.java), [MediaStoreIT](../../src/test/java/org/dzoka/MediaStoreIT.java), [S3StorageIT](../../src/test/java/org/dzoka/S3StorageIT.java) and [SandboxMediaIT](../../src/test/java/org/dzoka/SandboxMediaIT.java) extend the foundation coverage. [Run instructions](../development/intake-media.md) distinguish the full real-provider suite from explicitly narrower storage/database routes.

| Acceptance IDs | Verified M3 portion | Still pending |
|---|---|---|
| A01, A03 | Same/cross-tenant owner denial, staff/service scopes, case/item references, exact original idempotent receipt, concurrent revision conflict | Cache/vector/AI/federation and later commands |
| A04–A05 | Exact wire bytes and decomposed Unicode preserved separately from normalization; uncertainty retained; immutable revisions; injected outbox rollback; expiry and no resurrection | Broader channels and restored projections |
| A06 | Actual private S3 quarantine/anonymous denial, 5 MiB limit, immutable conditional PUT/digest checks; real isolated PNG/JPEG canonicalization/metadata removal; spoofed/truncated/oversized-dimension rejection; no premature preview | Broader formats, malware/content policy, deployment sandbox assessment and adversarial corpus |
| A07, A25 | Media lease expiry/stale completion, three-attempt budget and explicit retry; actual refused S3 connection preserves intent and exact retry recovers | AI job/model contracts, automatic worker scheduling and larger outage drills |
| A24 | Holds block expiry/deletion/cleanup; cleanup initiation rejects too-late holds; actual source/derivative deletion, reservation release and stale cleanup acknowledgment handling | Backup erasure, restore tombstone replay, remote replica/lifecycle reconciliation |
| A27 | Preview requires current authorized sanitized manifest; no source URLs/object keys; no-store/nosniff/CSP; minimal audit/event payloads | Comprehensive telemetry/export/privacy/security assessment |

These are bounded working intake/media portions, not completion of every system-wide acceptance row or a claim of ownership/recovery authorization.

## Executed asynchronous text/fingerprint portions (2026-10-26)

The M4-inclusive run passed 6 Java unit checks and 44 integration tests with actual PostgreSQL/S3/raster sandbox and the reviewed native Python/ONNX text provider. [AiJobsIT](../../src/test/java/org/dzoka/AiJobsIT.java) contains seven lifecycle/security/transaction scenarios; [AiProviderIT](../../src/test/java/org/dzoka/AiProviderIT.java) executes real HTTP claim–Python inference–core completion using only the service token. The eight AI scenarios also pass against native PostgreSQL. [Run guide](../development/ai-text.md) records the supported native platform, artifact locks and explicit CI contract scope. The Python tooling suite now has 28 passing tests.

| Acceptance IDs | Verified M4 text portion | Still pending |
|---|---|---|
| A01, A03–A04 | Owner/service isolation, wrong tenant/worker rejection, natural concurrent revision/space idempotency, atomic job/audit/outbox rollback | Other modalities, broker orchestration and broader authority surfaces |
| A07 | Expired/wrong lease and revised input cannot publish; exact completion replay is safe; finite normalized output and schema/provenance validation | Image/region task contracts and complete distributed scheduling |
| A08 | Reviewed real nonconstant CPU model execution, model/tokenizer/installed-wheel integrity, corrupt/missing artifacts fail before activation; no vectors on unavailable input/provider | Other native platforms/providers, broader adversarial artifact corpus and quality evaluation |
| A09 | Full space key includes artifact/tokenizer/preprocessing/runtime/configuration; same dimensions do not establish compatibility; immutable history/current revision separation | Bounded text-cohort activation recorded below; multimodal/larger-cohort and index migration remain |
| A24–A25 | Hold-aware component erasure, cancelled stale work, tenant-wide atomic queue reservations across actors, three-attempt/deadline bounds and exactly-once reservation release | Backup/replica/index erasure, production resource/egress limits, queue reconciliation and outage drills |
| A27 | Private normalized input only on a service lease; no DB/S3 authority for the Python client; safe environment/redirect/proxy behavior; no text/vector/token event payload | Comprehensive telemetry/privacy/security assessment |

`ItemFingerprint` is currently a view over immutable text components with explicit modality availability. It is not a complete multimodal snapshot, an accuracy/calibration result, proof of ownership or authority to release property.

## Executed local database restore portion (2026-10-26)

[restore_drill.py](../../tools/restore_drill.py) runs after a passing application suite, with API/worker contexts stopped. The Docker full-provider run passed 6 Java unit checks and 44 integration tests, then restored all 20 domain/public tables (729 synthetic rows). Complete row sets and Flyway history match; checked table/function owners and grants, column grants, forced row policies and constraints match. Runtime-role missing-context and wrong-actor reads deny; tenant A/B each see only their own synthetic organization; changing the source afterward cannot mutate the restored snapshot. The current local tool regression suite contains 40 passing tests. [Reproduction and limits](../operations/local-readiness.md) apply.

This supplies a bounded A26 logical database restore result and A01–A02 restored row-policy checks. It does **not** complete A24/A26: no restored API/publisher starts, no external effects are replayed, roles/keys already exist in the same cluster, and object/replica/key recovery, independently journaled deletion/hold replay, physical/PITR restore and HA still require implementation and drills. No production RPO/RTO or capacity claim follows from it.

The native PostgreSQL route independently passed the same full-provider 6-unit/44-integration suite and 20-table/729-row restore. Both routes use random synthetic credentials and leave production services untouched; native restore clients explicitly use their isolated cluster's selected port. [Verification record](../testing/local-readiness-2026-10-26.md) records both executions.

## Executed bounded text-cohort lifecycle (2026-10-26)

V5 and [FingerprintRolloutsIT](../../src/test/java/org/dzoka/FingerprintRolloutsIT.java) add seven real PostgreSQL/JWT/HTTP scenarios. The final native full-provider run passed **7 Java unit/51 integration tests**, plus 40 Python tests and expanded 27-table/1,001-row logical restore. [Execution record](../testing/m4-rollouts-2026-10-26.md) distinguishes the earlier Docker result and final native changes. Real model output was sealed/promoted unchanged in AiProviderIT.

| Acceptance IDs | Verified portion | Still pending |
|---|---|---|
| A01–A04, A27 | Separate curator authority; tenant/source scope, no neighbor slots; plan/transition retry identity; transactional pointer/history/audit/outbox; no text/vector in management API | All later authority surfaces and full telemetry/export assessment |
| A07–A09 | Immutable cohorts/manifests/component pins; terminal-slot completeness; separately measured available/unavailable coverage; hidden shadow results; exact versioned promotion and rollback | Image/local/OCR/speech and region contracts, representative quality evaluation, larger cohorts and index migration |
| A09, A24 | Revised/expired/deleted source, disabled model or erased component blocks reactivation; concurrent disjoint-cohort reads never tear selected components/pointer; no fallback for noncohort/new revision | Independent backup tombstone replay and full replica/index erasure |

The bounded profile is at most 100 explicit report revisions/16 text spaces. An evaluation-record hash is not independently verified accuracy evidence. No full A09/A24 or whole-M4 completion is claimed.

## Executed local perception and multimodal pins (2026-10-26)

V6/V7 add actual licensed vision/Paddle OCR/English CTC and independently authored classical local features, private sanitized/PCM input leases, shared queue/raw quotas and explicit multimodal pin activation. The final native full-provider run passed **8 Java unit/60 integration tests and 47 Python regressions**; logical restore matched **34 tables/1,498 synthetic rows**. [Execution record](../testing/m4-perception-2026-10-26.md) distinguishes actual inference from synthetic lifecycle assertions and records source/runtime/artifact identity.

| Acceptance IDs | Newly executed portion | Still open |
|---|---|---|
| A01–A04, A07, A25, A27 | Protected raw/derivative leases, actor/tenant/revision/expiry guards, public WAV/chunked bounds, shared 32-job quota, storage reservation, retry/deadline, exact idempotency, atomic request/result/outbox and safe projections | Production scheduling/resource/egress/rate-limit and full telemetry/privacy assessment |
| A08–A09 | Real four-provider output, exact checksummed manifests/source keys, tamper/corruption failure before activation; pixel/sample/finite shape bounds; immutable component pins selected atomically into ItemFingerprint | Representative quality/language/format coverage, learned/joint-space alternatives, larger cohorts/ANN index migration |
| A24 | Holds preserve raw/output; expiry actually erases PCM/derived rows and releases storage once; erased selected references cascade and cannot reactivate | Independent backup deletion/hold journal, object/replica/key recovery and release of real restores |

ResNet signatures are not CLIP; Harris patches are not learned descriptors; Paddle's bounded axis-aligned output is not blanket upstream equivalence; English CTC is not universal speech. Synthetic label/voice conformance is not a benchmark, ownership verification or production readiness.
