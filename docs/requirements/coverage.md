# Requirements coverage

All 100 top-level sections of the supplied specification remain in scope. This is a section-level delivery ledger, not a substitute for future capability acceptance tests. The supplied `docs/dzoka.md` text was compared to the current attachment and is identical after newline normalization. A scoped Java/PostgreSQL foundation now runs; the complete recovery product remains unimplemented beyond the capabilities recorded here.

| Section | Requirement | Current state |
|---|---|---|
| 1 | NON-NEGOTIABLE DEVELOPMENT PRINCIPLES | Governance/source gate/architecture complete; foundation invariants tested; complete product enforcement remains. |
| 2 | REQUIRED SOURCE AUDIT | Complete within declared static scopes: 29 pinned repositories and one edition-pinned standard; 176 evidence records; no pending mandatory audits. |
| 3 | PROJECT IDENTITY | Temporary codename Dzoka retained. |
| 4 | CORE ARCHITECTURAL PRINCIPLE | Specified; implementation and production validation pending. |
| 5 | ITEM FINGERPRINT | First-class current view, immutable text components, source/model/config provenance and explicit modality availability implemented; full multimodal snapshots/migration promotion remain. |
| 6 | MULTIMODAL INTAKE | M3 text/uncertain context and private PNG/JPEG intake implemented with immutable raw/normalized revisions; real-provider verification in STATUS. Audio/video/document/channel providers and production validation remain. |
| 7 | WHATSAPP-FIRST EXPERIENCE | Specified; implementation and production validation pending. |
| 8 | AI PERCEPTION LAYER | Durable private text-inference jobs and one reviewed real local CPU embedding profile implemented; image/OCR/speech/LLM perception and production validation remain. |
| 9 | SEARCH AND MATCHING PIPELINE | Specified; implementation and production validation pending. |
| 10 | MATCH EVIDENCE MODEL | Specified; implementation and production validation pending. |
| 11 | MISSING EVIDENCE RULE | Nullable source context and explicit fingerprint AVAILABLE/PENDING/UNAVAILABLE/FAILED states implemented; scoring-wide coverage/reliability remains. |
| 12 | HARD CONFLICT ENGINE | Specified; implementation and production validation pending. |
| 13 | OWNERSHIP VERIFICATION ENGINE | Specified; implementation and production validation pending. |
| 14 | AI-GENERATED PRIVATE OWNERSHIP CHALLENGES | Specified; implementation and production validation pending. |
| 15 | TRUST AND FRAUD ENGINE | Specified; implementation and production validation pending. |
| 16 | PRIVACY ARCHITECTURE | Specified; implementation and production validation pending. |
| 17 | DATABASE ARCHITECTURE | V1–V4 cover foundation, intake/media and AI jobs/spaces/components/quotas; full recovery schema and spatial/vector extensions remain. |
| 18 | VECTOR SEARCH ABSTRACTION | Specified; implementation and production validation pending. |
| 19 | EVENT-DRIVEN ARCHITECTURE | Transactional outbox/inbox and leased dispatch foundation implemented/tested; live Kafka adapter, scheduling, DLQ and operations remain. |
| 20 | RECOVERY EVENT MODEL | Specified; implementation and production validation pending. |
| 21 | RECOVERY KNOWLEDGE GRAPH | Specified; implementation and production validation pending. |
| 22 | CCTV / EDGE VISION SUBSYSTEM | Specified; implementation and production validation pending. |
| 23 | OBJECT TRACKING | Specified; implementation and production validation pending. |
| 24 | LOST-OBJECT MOVEMENT / RECOVERY PREDICTION | Specified; implementation and production validation pending. |
| 25 | FEDERATION | Specified; implementation and production validation pending. |
| 26 | FEDERATED SEARCH | Specified; implementation and production validation pending. |
| 27 | FEDERATION PRIVACY | Specified; implementation and production validation pending. |
| 28 | MULTI-TENANCY | Organization/intake/media HTTP/SQL isolation, active memberships, tenant-qualified keys and forced row policies implemented/tested; AI/cache/vector/federation surfaces remain. |
| 29 | SELF-HOSTED + SAAS DEPLOYMENT | Specified; implementation and production validation pending. |
| 30 | RECOVERY / CUSTODY ENGINE | Specified; implementation and production validation pending. |
| 31 | SECURE HANDOVER | Specified; implementation and production validation pending. |
| 32 | COURIER ORCHESTRATION | Specified; implementation and production validation pending. |
| 33 | REWARD SYSTEM | Specified; implementation and production validation pending. |
| 34 | QR / NFC ITEM REGISTRATION | Specified; implementation and production validation pending. |
| 35 | DIGITAL ITEM PASSPORT | Specified; implementation and production validation pending. |
| 36 | SECURE ANONYMOUS MESSAGING | Specified; implementation and production validation pending. |
| 37 | NOTIFICATIONS | Specified; implementation and production validation pending. |
| 38 | LOCATION INTELLIGENCE | Raw place labels and optional coordinates/radius preserved; spatial indexing/geocoding/intelligence and production validation remain. |
| 39 | TEMPORAL INTELLIGENCE | Raw time labels, IANA timezone and optional uncertain UTC interval preserved; temporal inference/retrieval and production validation remain. |
| 40 | SEARCH SCALE | Specified; implementation and production validation pending. |
| 41 | MODEL PROVIDER ARCHITECTURE | Private leased Python consumer with a locked local ONNX text profile and strict result boundary implemented; other providers and production isolation remain. |
| 42 | MODEL GOVERNANCE | Specified; implementation and production validation pending. |
| 43 | ML FEEDBACK LOOP | Specified; implementation and production validation pending. |
| 44 | MLFLOW | Specified; implementation and production validation pending. |
| 45 | OBSERVABILITY | Specified; implementation and production validation pending. |
| 46 | SECURITY | Signed JWT, local permissions, safe SQL/input/error boundaries and runtime-role guard implemented; full security assessment/hardening remains. |
| 47 | POLICY ENGINE | Active memberships plus intake retention/quotas and media legal-hold enforcement implemented; full versioned release/legal policy engine remains. |
| 48 | AUTHENTICATION AND IDENTITY | Real JWT resource-server validation and issuer/subject local memberships implemented; production IdP/admin/account lifecycle remains. |
| 49 | API DESIGN | Versioned organization/report/reference/media APIs with explicit auth, bounds, revisions, idempotency and safe errors; broader recovery APIs remain. |
| 50 | FRONTEND | Specified; implementation and production validation pending. |
| 51 | CORE BACKEND | Java 21-bytecode/Spring Boot 4.1.2 core builds and passes foundation tests; remaining domain implementation pending. |
| 52 | SERVICE BOUNDARIES | Specified; implementation and production validation pending. |
| 53 | DOMAIN MODULES | Identity/organization/intake/media modules and tenant/event persistence implemented; most recovery/AI modules remain. |
| 54 | EVENTUAL CONSISTENCY | Outbox/inbox transaction and duplicate/retry handling implemented; full asynchronous projections/provider workflows remain. |
| 55 | AUDITABILITY | Minimal protected organization/report/media audit/outbox writes implemented; complete sensitive-operation/read/export audit remains. |
| 56 | TESTING | 28 Python, 6 Java unit/architecture/configuration and 44 real PostgreSQL/S3/sandbox/model integration tests pass; complete product/security/load suites remain. |
| 57 | BENCHMARKS | Specified; implementation and production validation pending. |
| 58 | DATASETS | Specified; implementation and production validation pending. |
| 59 | ANALYTICS | Specified; implementation and production validation pending. |
| 60 | PREDICTIVE PREVENTION | Specified; implementation and production validation pending. |
| 61 | EDGE / IoT EXTENSIBILITY | Specified; implementation and production validation pending. |
| 62 | DISPOSAL / UNCLAIMED PROPERTY | Specified; implementation and production validation pending. |
| 63 | INTERNATIONALIZATION | Specified; implementation and production validation pending. |
| 64 | ACCESSIBILITY AND LOW-BANDWIDTH OPERATION | Specified; implementation and production validation pending. |
| 65 | OFFLINE / EDGE OPERATION | Specified; implementation and production validation pending. |
| 66 | ARCHITECTURE DOCUMENTATION | Accepted baseline and all 24 required views present; supplied bodies preserved. Implementation/production validation pending. |
| 67 | ADRs | Process ADR 0001, design ADRs 0002–0025 and scoped intake/media/text-inference ADRs 0026–0027 accepted; later artifact adoption remains. |
| 68 | ROOT REPOSITORY DOCUMENTATION | Root governance documents added; ongoing maintenance required. |
| 69 | STATUS.md | Factual STATUS.md added. |
| 70 | PRODUCTION CONFIGURATION | Required DB/OIDC configuration fails closed, separate migration credentials, reviewed local/test runtime; production deployment/config hardening remains. |
| 71 | DATABASE MIGRATIONS | Flyway V1–V4 foundation/intake/media/AI migrations applied, rerun and validated against real PostgreSQL; full schema/upgrade/restore coverage remains. |
| 72 | BACKUP AND RESTORE | Specified; implementation and production validation pending. |
| 73 | HIGH AVAILABILITY | Specified; implementation and production validation pending. |
| 74 | RATE LIMITING AND ABUSE PREVENTION | Specified; implementation and production validation pending. |
| 75 | DATA RETENTION | Bounded tenant report/image retention, legal holds and explicit expiry/purge workers implemented; scheduling, broader policy and production validation remain. |
| 76 | DATA DELETION | Intake tombstones/raw erasure, current preview revocation and actual object purge tested; backup/restore/replica/orphan reconciliation and broader domains remain. |
| 77 | SEARCH PRIVACY | Specified; implementation and production validation pending. |
| 78 | PUBLIC API | Specified; implementation and production validation pending. |
| 79 | CONNECTOR FRAMEWORK | Specified; implementation and production validation pending. |
| 80 | SOCIAL / EXTERNAL SOURCE INGESTION | Specified; implementation and production validation pending. |
| 81 | HUMAN REVIEW OPERATIONS | Specified; implementation and production validation pending. |
| 82 | SYSTEM ADMINISTRATION | Specified; implementation and production validation pending. |
| 83 | FEATURE FLAGS | Specified; implementation and production validation pending. |
| 84 | SAFETY | Specified; implementation and production validation pending. |
| 85 | SYSTEM PERFORMANCE | Specified; implementation and production validation pending. |
| 86 | FAILURE HANDLING | Specified; implementation and production validation pending. |
| 87 | IDEMPOTENCY | Durable actor/tenant/operation idempotency, inbox dedupe and outbox lease tokens tested; external effects and future domains remain. |
| 88 | REPRODUCIBILITY | 106 Maven artifacts, separate 21-wheel/model/tokenizer/source hashes and fixed archive timestamp; expanded build SBOM/cross-runtime reproducibility remains. |
| 89 | LOCAL DEVELOPMENT | Working Java API/migration launchers and native/Docker integration harnesses documented; full product/AI/web development environment remains. |
| 90 | CI/CD | Governance and pinned-Java/digest-PostgreSQL foundation workflows written; local runners pass; remote CI/deployment/security scans not run. |
| 91 | DEPENDENCY GOVERNANCE | Foundation/media/text adoption, 106-JAR and 21-wheel/model integrity/license locks delivered; later models/platforms and expanded advisory/build inventory remain. |
| 92 | LICENSE STRATEGY | MIT core and bounded foundation runtime/license review recorded; unresolved audited grants and later model/data licensing restrictions remain. |
| 93 | NO README-DRIVEN CLAIMS | Canonical findings distinguish source inspection from claims. |
| 94 | IMPLEMENTATION ORDER | M0–M3 and the first M4 asynchronous text slice delivered; remaining M4 modalities/snapshot promotion precede M5. Full scope retained. |
| 95 | DO NOT OVERENGINEER THE WRONG PARTS | Specified; implementation and production validation pending. |
| 96 | SUCCESS CRITERIA | Specified; implementation and production validation pending. |
| 97 | REQUIRED FINAL DOCUMENTATION BEFORE DECLARING PRODUCTION READY | Specified; implementation and production validation pending. |
| 98 | ARCHITECTURAL INVARIANTS | Accepted invariants plus executed foundation acceptance portions recorded; ownership/release/AI/federation enforcement still future. |
| 99 | WORKING STYLE | Autonomous inspection and governance work in progress. |
| 100 | FIRST ACTIONS | Inventory, 30 audits/findings, ADRs/baseline and first Java/PostgreSQL foundation delivered; subsequent product milestones remain. |
