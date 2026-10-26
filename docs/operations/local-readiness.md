# Local verification and deployment boundary

2026-10-26. Operational profile for the **implemented foundation, private image/short-PCM intake, local text/perception providers and bounded multimodal activation**. It does not enable later product modules or approve production images. The [baseline](../architecture/baseline.md), [runtime adoptions](../dependencies/README.md) and [status](../../STATUS.md) govern scope.

## Reproduce the local evidence

With the previously reviewed Java, Python, Maven dependencies, model/wheels and Docker available:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-25.0.4'
python tools/verify_local.py
```

This requires every phase: source gate, M1 requirements/preservation, Python regressions, Maven integrity, text-native artifact/conformance, perception, joint/region and language artifact identities, generative artifacts/installed-runtime checks, full actual-provider integration and database restore (thirteen phases, including quality-alternative artifacts). The full integration now includes the four earlier perception profiles plus paired Jina encoders, SSD detection, FCN semantic segmentation, grounded QA attributes, French→English Marian translation, private cross-encoder reranking, the larger repetition-controlled draft model, dedicated Shona/Spanish/German→English alternatives and explicit multimodal activation; [setup/limits](../development/perception.md) apply. A missing provider, failed phase or changed source fails the run; no portable-contract fallback. Results/logs stay in ignored `.test-tmp/local-verification-*/`, binding code/lock inputs to SHA-256. Logs and synthetic archives are diagnostics, not material to publish.

For a reviewed native PostgreSQL route, add `--pg-bin 'C:\Program Files\PostgreSQL\18\bin' --maven 'D:\Projects\ai\dzoka\.audit-cache\apache-maven-3.9.16\bin\mvn.cmd'`. Docker is still required for the real decoder and, unless separately selected through the intake harness, private S3. Individual suites remain documented in the [foundation](../development/foundation.md), [intake](../development/intake-media.md) and [AI](../development/ai-text.md) guides.

## Concrete current runtime and security profile

| Boundary | Selected implemented profile | Activation requirement |
|---|---|---|
| Java core | Spring Boot 4.1.2, Java 21 bytecode; local JDK 25.0.4 | Verify all 106 locked JAR/POM/license artifacts; compile/package before separate migrations |
| Authoritative database | PostgreSQL 18.6; Flyway V1–V10 | Separate migrator and non-owner/non-BYPASSRLS `dzoka_app`; no API-owned migration credentials |
| API authentication/session | Stateless RS256 bearer JWT; exact issuer, audience, expiry and subject; local active membership | HTTPS issuer/JWKS outside explicit loopback tests; no cookie login/session or token-role authorization |
| Private media | Explicitly enabled S3 profile, never-versioned private bucket, bounded PNG/JPEG | HTTPS outside loopback; bucket policy/ACL/versioning checks; no public object URLs |
| Decoder | Reviewed digest-pinned external Temurin child | Only trusted sanitation worker has Docker access; child non-root, no network/credentials/socket, bounded resources |
| Private text worker | Windows amd64 CPython 3.14.7; 21 locked wheels; quantized MiniLM ONNX/tokenizer | Check exact profile/model/runtime bytes; private service `ai:work` membership; no DB/S3 credentials |
| Private perception worker | Same reviewed CPU runtime; pinned ResNet/Paddle/English CTC plus independently authored Harris patches | Exact six weight/dictionary and source/profile checks; leased sanitized PNG/strict PCM only; no DB/S3 credentials, redirects, proxy or runtime downloads |
| Private joint/region worker | Same reviewed CPU runtime; Jina CLIP v1 paired encoders, SSD-MobileNetV1 and FCN-ResNet50 | Eight exact artifact checks and shared-space binding; leased sanitized PNG or current report text; [setup and bounded scope](../development/joint-vision.md) |
| Private quality worker | Unchanged separate 22-wheel runtime; larger official LLM and three dedicated Marian directions | Shona/Spanish/German→English; explicit untrusted results, named profiles and reproducible comparisons; [setup/limits](../development/language-quality.md) |
| Private generative worker | Separate reviewed 22-wheel runtime, SmolLM2 and M2M100 | English untrusted drafts and twelve explicit translation directions; known meaning errors, no Shona; [setup and limits](../development/generative.md) |
| Private language worker | Same reviewed runtime; pinned QA/Marian/cross-encoder graphs | Thirteen artifacts; grounded quoted attributes, explicit French→English direction, private caller-supplied rerank envelopes and raw-logit separation; [language setup](../development/language.md) |
| Worker scheduling | Explicit bounded commands, operator-controlled | No hidden daemon, Kafka adapter or scheduler exists; see procedures below |
| HTTP ingress | API requires authentication; test harness binds loopback | A real deployment must configure TLS ingress, rate limits, network isolation and secret delivery; no production ingress artifact is approved by this local profile |

The actual implementation already resolves the current API's algorithm/session and runtime selections. A future browser/PWA authentication flow still needs its own client/CSRF/token-storage design; describing the API's stateless bearer mode does not implement browser login. Federation signing and EPCIS mappings are different protocols and remain local implementation work before their adapters, not infrastructure blockers.

## Start, maintain and stop the implemented slice

1. Verify the source gate and artifact locks. Provision an explicitly selected database, separate roles and tenant memberships using the foundation guide. Supply secrets through process environments; `.env.example` is documentation and is not automatically loaded.
2. Run `python tools/core.py migrate` in the migration-only environment. A failed checksum/migration stops rollout; never edit applied migrations or disable row policies to proceed.
3. Configure the API-only environment and run `python tools/core.py serve`. The launcher strips migration credentials. Startup validates runtime role/policies before accepting traffic. The test harness uses real JWT/HTTP requests as its readiness check; there is no separate anonymous health/metrics endpoint.
4. Enable media only after the supported bucket and reviewed decoder are available. On a separate trusted worker identity, invoke `python tools/core.py sanitize-media` in bounded batches. A provider failure leaves durable intent; do not serve source/quarantined bytes as a fallback.
5. Register a disabled reviewed embedding space with `python tools/ai_space.py`, inspect the SQL and enable it only after artifact checks. With an `ai:work` service token, run `python tools/ai_worker.py --once`. It claims at most the documented scope and does not install/download models.
6. Under separately granted erasure identities, run `python tools/core.py expire-intake`, `python tools/core.py purge-ai` and `python tools/core.py purge-media`. Repeat bounded batches until no eligible work remains. Preserve holds and check actual object deletion, not only a database state flag. A deployment scheduler and independent orphan reconciliation remain implementation work.
7. To stop, stop worker invocations first, then the API. Let transactions finish and leases expire. Never acknowledge unconfirmed external effects or delete pending outbox/jobs to make shutdown appear clean. The disposable harness stops/removes only its own services; these instructions do not stop unrelated installations.

## Restore evidence and release barrier

`python tools/test_intake.py --with-ai --restore-drill` runs a real PostgreSQL custom-format `pg_dump`/`pg_restore` after all application contexts have stopped. The target is a new disposable database; original owners, grants and row policies are retained. The drill compares every domain/public table's complete synthetic row set, Flyway history, owners, table/column/function grants, policy definitions and constraints. It then tests missing-context denial, separate tenant A/B visibility, wrong-actor denial, a non-owner/non-bypass runtime role, and independence from subsequent source changes. Evidence stores row counts and hashes, never row contents.

This is a **same-cluster logical database restore**, with roles already provisioned. It is not a role/key disaster recovery, physical/PITR restore, object-store backup, HA/failover, replica erasure, tombstone replay or production RPO/RTO measurement. No restored API, publisher or worker is started. Restored data must remain isolated until post-backup deletions/revocations/holds have been reconciled from an independently retained authoritative journal, object availability/permissions checked and pending external operations reconciled. That journal/replay/release barrier is still unimplemented; the drill does not waive it. Such code can be built/tested locally; actual backup storage, keys, replica topology and failover evidence require the chosen deployment.

## Work classification

| Work | Can be done locally? | What actually needs outside access |
|---|---|---|
| M4 local profiles and wider quality/format/language/larger-cohort work | Named local text/vision/local/OCR/speech and explicit multimodal pins are delivered; further engineering remains local | Permitted representative data/institutional acceptance for quality claims; no GPU/cloud account required for delivered profiles |
| Retrieval/scoring, verification/release, custody and other product modules | Yes | Institutional/legal policy and representative workflow acceptance before real use |
| Federation envelope/signing/key lifecycle, EPCIS subset, two-node tests | Yes | Real independent peer trust, keys and interoperability sign-off for live federation |
| Compose/Helm, reviewed deployment images, scheduling, dashboards, scanners, backup/tombstone replay | Yes | Exact target topology/secret backend; live deployment and cross-host failure exercises |
| API OIDC/JWT/RLS/S3/native-model contract verification | Yes; current real local harness | Real IdP service-account lifecycle, key rotation, tenant onboarding and deployment egress policy |
| Messaging, payment and courier adapters, signed callbacks/reconciliation | Local ports and simulations/sandbox clients can be built | Provider accounts/credentials, webhook-reachable TLS domain, provider sandbox/live certification |
| Retention defaults, SLO/RPO/RTO and capacity objectives | Draft configuration and measurements locally | Accountable institutional requirements and production workload/topology acceptance; do not invent legal defaults or achieved objectives |
| CCTV/recovery-model quality and multilingual calibration | Synthetic conformance locally | Consented camera/data access, lawful grants and representative independently evaluated datasets |
| GitHub Actions execution | Workflow code locally | Restore GitHub account ability to start jobs, then run remotely; local tests cannot prove remote execution |

These distinctions prevent unfinished local engineering from being labeled “waiting for infrastructure.” All later milestones remain in scope. See [operational incident procedures](../runbooks/README.md) for current containment actions and activation limits.
