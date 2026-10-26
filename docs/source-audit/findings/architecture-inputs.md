# Evidence for the architecture phase

All mandatory source reviews are complete within their declared scopes. These remain audit inputs; the resulting accepted design is recorded in [technical ADRs](../../adr/README.md) and the [architecture baseline](../../architecture/baseline.md). Audit recommendations are not implemented application capabilities or approval of concrete runtime artifacts. Process ADR 0001 and the user's invariants remain binding.

| Decision area | Evidence | Implication / required validation |
|---|---|---|
| Transactional authority and tenancy | [Tracking System](../platforms/lost-and-found-tracking-system.md), [Malak](../platforms/malak-lostandfound.md), [Trishthakur](../platforms/trishthakur-lost-and-found-ai.md) | Retain requested PostgreSQL authority; design tenant keys, transactions, concurrency, migrations and authorization rather than inherit sequential document/CSV writes |
| Java/Spring modular core | No audited platform implements the complete network | Retain user stack preference; audits are not a Spring-vs-Python benchmark. Review concrete framework/security dependencies before adoption |
| Initial/optional vectors | [pgvector](../retrieval/pgvector.md), [Qdrant](../retrieval/qdrant.md) | Provider abstraction, compatible model-space keys, authenticated queries and filtered-recall/maintenance/restore tests; PostgreSQL remains authoritative |
| Retrieval versus reranking | [Globalenglish](../platforms/globalenglish-lostfound.md), [VisualMatch](../retrieval/visualmatch.md), [ILIAS](../retrieval/ilias.md) | Independent candidate routes/fusion and bounded heavy scoring; measure Recall@K before and after filters |
| Global/local identity | [ILIAS](../retrieval/ilias.md), [Patchwise](../retrieval/patchwise-retrieval.md), [Reranking Transformer](../retrieval/reranking-transformer.md), [FuricBond](../platforms/furicbond-lost-and-found.md) | Retain patch/region provenance and model metadata; evaluate same-category/different-instance negatives and storage/compute budgets |
| Confidence and missing evidence | [Globalenglish](../platforms/globalenglish-lostfound.md), [SmartFind](../platforms/ai-lost-and-found.md), [FindBack](../platforms/findback.md) | Available-evidence normalization, independent coverage and calibrated confidence; no invented visual signals or weak evidence double counting |
| Model governance | [OpenCLIP](../retrieval/openclip.md), [OpenVisualSearch](../retrieval/openvisualsearch.md), [Tracking System](../platforms/lost-and-found-tracking-system.md) | Require proven pretrained artifacts, complete model/version/preprocess/dimension keys, explicit degraded modes and append/versioned re-embedding |
| Perception defaults | [PaddleOCR](../vision/paddleocr.md), [PaddleDetection](../vision/paddledetection.md), [Detectron2](../vision/detectron2.md), [Ultralytics](../vision/ultralytics.md) | Permissive default providers with controlled configuration/artifacts; isolate asynchronous workers; review weights and transitive engines |
| Ownership/fraud/release | [Malak](../platforms/malak-lostandfound.md), [Smart Asset Finder](../platforms/smart-asset-finder.md), [Tracking System](../platforms/lost-and-found-tracking-system.md), [SmartFind](../platforms/ai-lost-and-found.md) | Private evidence/disclosure budgets, server-assigned roles and deterministic/human release policy; relative risk/rank cannot approve ownership |
| WhatsApp-first workflows | [KWASU](../platforms/kwasu-lost-found-bot.md), [Smart Asset Finder](../platforms/smart-asset-finder.md) | Authenticated inbound messages, durable conversations/raw input, anonymous relay and idempotent delivery; outbound notification alone is insufficient |
| Event backbone | [OpenEPCIS](../traceability/openepcis.md), platform trigger/write behavior | Kafka-compatible events with transactional outbox/inbox, retries/DLQ/replay; broker delivery does not ensure exactly-once external effects |
| Federation | [Mycel](../federation/mycel.md), [Open Federated Search](../federation/open-federated-search.md) | Node identity/trust, bounded fan-out, quotas, timeouts/breakers, source attribution and canonical evidence; raw scores remain incomparable |
| Traceability/custody | [GS1](../traceability/gs1-end-to-end-traceability.md), [OpenEPCIS](../traceability/openepcis.md) | Native append-oriented recovery events and explicit tested EPCIS subset; neither traceability nor identifiers prove ownership |
| Optional passports | [DPP/DID](../traceability/dpp-did.md) | UUID/QR/NFC remain viable without DID. Optional provider needs secure keys, resolution policy and revocation |
| Authorized edge/CCTV | [CCTV](../cctv/cctv-theft-finder.md), [DeepSORT](../vision/deepsort.md), [Ultralytics](../vision/ultralytics.md) | Investigation grants, gap/occlusion uncertainty, quotas/cancellation/redaction and minimal export; no default facial recognition |
| Recovery intelligence | [Trajectron++](../cctv/trajectron-plus-plus.md) | Build next-custodian/location-state prediction from confirmed events, not generic continuous trajectories |
| Rewards | [OpenEscrow](../recovery/openescrow.md) | Payment/ledger reserve-release-refund-dispute phases, deterministic recovery prerequisites and reconciliation; blockchain not required |

Mandatory reviews do not cover every prospective runtime dependency. Concrete PostgreSQL/PostGIS, Kafka distribution, Spring/security, object storage, OIDC, OpenTelemetry and transitive AI runtime versions still need targeted adoption/license review. No technology choice is justified solely by upstream implementation language or popularity.

Technical ADRs must document alternatives, evidence, consequences, implementation implications and acceptance tests. [Unresolved gaps](unresolved-gaps.md) separates completed source review from remaining design, experiments and production validation.


## Subsequent bounded generative implementation (2026-10-26)

[ADR 0032](../../adr/0032-general-drafts-and-multilingual-profiles.md), [separate model/runtime adoption](../../dependencies/generative-adoption.md) and [execution evidence](../../testing/m4-generative-2026-10-26.md) add private English general drafts and twelve explicit M2M100 translation directions. This separate artifact/dependency review does not expand the thirty mandatory source audits or certify upstream tests. Known translation meaning errors and LLM repetition remain recorded; Shona, representative quality acceptance, native host isolation and full M4 remain open. General drafts cannot become intrinsic fingerprints or release authority.


## Subsequent language-quality and Shona extension (2026-10-27)

[ADR 0033](../../adr/0033-language-quality-and-shona.md) and [separate artifact adoption](../../dependencies/language-quality-adoption.md) append a larger official draft model and dedicated Shona/Spanish/German-to-English exports. These reuse the reviewed 22-wheel environment and preserve older keys. [Execution evidence](../../testing/m4-language-quality-2026-10-27.md) distinguishes controlled synthetic comparisons from representative/native-speaker acceptance. One-way Shona support is not language detection, bidirectional support or verified translation. English→Shona/further quality remains local engineering; permitted data and institutional acceptance remain external. Mandatory thirty-source audit scope and supplied research bodies remain unchanged.


## Subsequent local-development bidirectional Shona profile (2026-10-27)

[ADR 0034](../../adr/0034-bidirectional-shona-and-code-switching.md), [separate model/runtime review](../../dependencies/shona-adoption.md) and [execution record](../../testing/m4-shona-2026-10-27.md) add Apache-2.0 English↔Shona int8 models and explicit caller-marked copying. The new CTranslate2 wheel is MIT with separately governed Intel/NVIDIA binaries, so this adoption is local-development-only and does not approve production/SaaS/distribution. Existing 21/22-wheel environments/keys and mandatory thirty-source review scope remain unchanged. Marked outputs expose exact source/output offsets and unverified copy provenance; unmarked qualifier/proper-name failures remain recorded. Permitted representative data/native-speaker acceptance is external; a suitable runtime build, automatic code-switch quality and annotation UX remain local engineering.
