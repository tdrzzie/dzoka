# Remaining gaps after mandatory source review

All 30 mandatory audits are complete within their declared static scopes: 29 pinned repositories and one edition-pinned standard. No mandatory record is pending. License uncertainty is a recorded outcome, not permission to copy and not a reason to pretend the source was unreviewed. See [INDEX.md](../INDEX.md).

The architecture milestone is recorded in the [accepted baseline](../../architecture/baseline.md) and [technical ADRs](../../adr/README.md). The foundation, M3 report/private images and first M4 durable text-inference/fingerprint slice are implemented. Concrete runtime/model adoption and real-provider verification are tracked in [STATUS.md](../../../STATUS.md), [intake](../../development/intake-media.md) and [AI run instructions](../../development/ai-text.md). Broader multimodal/AI/recovery capabilities and production hardening remain. Completed source review does not prove upstream security or replace the following work.

| Gap | Evidence / limitation | Required resolution |
|---|---|---|
| Model/data/dependency licensing | Code grants do not cover all artifacts; nine project grants unresolved and one all-rights-reserved | Concrete bill of materials, weight/dataset terms and independent reference-contract implementation; no unauthorized copying |
| Actual runtime dependencies | Mandatory list does not include all framework/infrastructure versions | Targeted adoption review for Spring/security, PostgreSQL/PostGIS, selected Kafka distribution, OIDC, object storage, observability and AI transitive engines |
| ItemFingerprint/model spaces | Missing version predicates, incompatible coordinates and hidden fallbacks | Complete provenance/compatibility keys, append/versioned re-embedding and explicit failure contracts |
| Matching/calibration | Heuristic scores/research metrics are not recovery accuracy | Consented/synthetic instance data, hard negatives, Recall@K/MRR/NDCG, false-match analysis and calibrated coverage/confidence |
| Private ownership challenges | No source integrates evidence-supported selection, safe disclosure and robust verification | Challenge schema, encryption, attempt/disclosure limits, risk policy and human review |
| Trusted release | Upstream score-driven approvals, client trust fields or premature codes | Atomic authorized claim/custody/reward/handover transitions with concurrency/replay tests |
| Tenant isolation | Libraries/single-institution apps do not supply all boundaries | DB/query/cache/job/media/provider/federation authorization and adversarial isolation tests |
| Durable messaging/events | TwiML and document triggers do not ensure exactly-once effects | Signed webhooks, inbox/outbox, idempotency, correlation/causation, retry/DLQ/replay and degradation |
| Recovery federation | Web search/shard protocols do not exchange private recovery evidence safely | Signed/versioned canonical evidence, trust/quotas, replay protection, revocation, calibration and PII-local routing |
| EPCIS subset | Interfaces/topology are not blanket conformance or passing tests | Native mapping, supported versions/types, privacy projection and duplicate/rollback tests |
| Authorized CCTV | Local algorithms/tracks are not secure institutional investigations | Grants, isolation, input limits, redaction, protected manifests, UTC/uncertain windows, cancellation and real-camera evaluation |
| State-transition learning | Continuous trajectories have a different target | Confirmed labels, leakage-resistant evaluation, next-state calibration and controlled promotion |
| Courier/rewards | Rental escrow is not recovery logistics/payment operations | Provider contracts, deterministic ledger, signed callbacks, duplicate booking/payment prevention and reconciliation |
| Operations/security | No upstream execution, dynamic assessment, deployment or benchmark performed | Integration/security/load tests, migrations, restore/HA/failure drills, observability and runbooks before production |

Individual reports record narrower exclusions: checkpoint/native-code security, complete router/rules/filter enforcement, remote provider contracts, decoder paths and transitive licenses. Resolve these at integration adoption; a bounded review is not blanket approval.

2026-10-26 adoption update: separately reviewed immutable [joint/region artifacts](../../dependencies/joint-vision-adoption.md) supply local Jina CLIP v1 paired encoders, SSD detection and FCN semantic segmentation on the existing reviewed CPU runtime. This does not resolve grants for previously ambiguous distributions or authorize copying their implementation. Paired-space identity, private typed outputs and explicit cohort selection are implemented under [ADR 0030](../../adr/0030-joint-vision-and-region-models.md); representative quality, cross-modal retrieval/indexing, instance segmentation, redistribution notices and deployment acceptance remain distinct work. [STATUS.md](../../../STATUS.md) records executed conformance and external prerequisites.

All 100 sections remain in [requirements coverage](../../requirements/coverage.md). The [earlier gap catalogue](../../findings/unresolved-gaps.md) remains intact for detailed planning, with assumptions subject to current evidence and ADRs.

2026-10-26 language adoption update: independently reviewed Apache-2.0 task-specific QA, Marian French→English and official MS MARCO cross-encoder exports now supply bounded private language profiles under [ADR 0031](../../adr/0031-private-language-inference.md). Exact grants/bytes and rejected FLAN observations are in [adoption](../../dependencies/language-adoption.md). This closes the named local provider slice, not general LLM quality, representative multilingual/instance acceptance, server-side candidate retrieval/fusion or deployment isolation. No previously unresolved source grant is silently approved.
