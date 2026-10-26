# Local vision, local features, OCR and speech

The bounded M4 perception profiles use the existing reviewed Windows amd64 CPython 3.14.7/ONNX Runtime CPU environment. They require no GPU or external inference account. [Adoption evidence](../dependencies/perception-adoption.md), [ADR 0029](../adr/0029-local-perception-providers.md), `ai/perception.lock.json` and `ai/perception-profiles.json` identify exact terms, artifacts, processing and source/runtime hashes. They do not establish recovery accuracy or complete multilingual coverage.

## Profiles and honest limits

| Task | Implemented local profile | Output/limit |
|---|---|---|
| VISION | Apache-2.0 Model Zoo ResNet18-v1-7 | Fixed RGB/center-crop normalized 1,000-class-logit signature; its own compatible profile, not CLIP or validated instance retrieval |
| LOCAL_FEATURES | Independently authored Harris/patch baseline | At most 16 source-pixel boxes, each with normalized 16-value patch descriptor; explicitly classical, not a learned-feature fallback |
| OCR | Official Apache-2.0 Paddle PP-OCRv6 small detector/recognizer/dictionary | At most 16 axis-aligned regions with pixel bounds, text and uncalibrated model scores; bounded BGR/resize/CTC profile, not full upstream polygon-pipeline equivalence |
| SPEECH | Wav2Vec2 base 960h ONNX export; preserve upstream Apache-2.0 plus exporter MIT obligations | English greedy CTC, mono PCM16 16kHz, 400–160,000 samples (0.025–10 seconds), at most 2,048 characters; no claim of multilingual or word-timestamp support |

Empty detection/features/transcript are successful empty observations, not invented mismatches. Confidence/quality/calibration remain separate from model scores and modality availability. OCR/transcript text is untrusted data and cannot execute instructions or approve release.

## Bootstrap and register

Use the reviewed text-runtime installation first; no new wheel is installed. Then:

```powershell
python tools/perception_runtime.py fetch
python tools/perception_runtime.py check
python tools/perception_runtime.py profiles
python tools/perception_profile.py --tenant '11111111-1111-4111-8111-111111111111' --profile '22222222-2222-4222-8222-222222222222' --task OCR
```

Replace synthetic UUIDs with the intended tenant/profile. The generator prints a **disabled** INSERT, never connects/enables anything, and includes the exact reviewed manifest/key. An authorized database operator provisions/enables it after integrity/conformance approval. Every input/output provenance binds source revision, exact input digest, provider/task, artifact revisions/digests, implementation/runtime/configuration and generated/received times. A code, artifact, processing or runtime change requires a new reviewed profile/version; do not rewrite old rows/vectors or normal checks to hide a mismatch. The earlier undeclared CLIP conversion stays deferred; its grant is not needed to run these alternatives.

## Input, jobs and private consumer

Migrate V6/V7 through the separate migrator. Ordinary owners need `reports:write`/`reports:read`; existing explicit `:any` permissions handle separately authorized staff. Provision a private service identity with `ai:work`. Curators separately need `fingerprints:manage`; workers cannot promote their output.

Paths below start `/api/v1/tenants/{tenant}` and require signed bearer authentication plus current local membership.

| Method/path | Contract |
|---|---|
| `POST /reports/{report}/perception/image` | JSON `profileId`, `mediaId`; source must be a current private SANITIZED PNG derivative, not raw/quarantined or caller URL |
| `POST /reports/{report}/perception/audio/{profile}` | `Content-Type: audio/wav`; exact bounded canonical 44-byte RIFF/fmt/data PCM16 mono 16kHz header/body; no compressed codec, arbitrary chunks or metadata |
| `GET /perception/jobs/{job}` | Safe state/attempt/deadline/profile/task projection; no raw bytes/transcript/vector/token |
| `GET /reports/{report}/perception/components` | Authorized current-source derived components with full provenance and explicit SHADOW/ACTIVE selection |
| `POST /internal/perception/jobs/claim` | `ai:work`, JSON profileId; one private 120-second lease |
| `POST /internal/perception/jobs/{job}/input` | Exact lease JSON; no-store bounded binary input after current source/lease checks before and after core object access |
| `POST /internal/perception/jobs/{job}/complete` | Exact lease/input/profile/time/evidence envelope; finite typed output, model-space identity and pixel/sample bounds checked; atomic append/job/audit/outbox |
| `POST /internal/perception/jobs/{job}/fail` | Exact lease and approved safe reason; bounded retry, no fabricated evidence |

The core alone holds S3 credentials. Raw audio is preserved exactly in a restricted tenant/source table; the worker checks/decodes only the narrow PCM format and normalizes it asynchronously. Images are decoded/canonicalized in the existing non-root/network-disabled/resource-limited JRE sandbox before they may enter a perception job. The private Python reader accepts only bounded canonical RGBA8 noninterlaced PNG, validates CRC/filtered raster lengths and bounds decompression. No general image/audio codec or remote model code is added to the API.

Natural idempotency binds tenant, report revision, profile, selected media (or audio bytes) and exact input digest. Both text and perception use one tenant-wide 32-pending-job reservation ledger. Perception caps retained noncancelled jobs at 64 per report; raw audio also has an independent 32-MiB tenant reservation. Three attempts, 120-second leases and one-day deadlines bound outages. Storage reservations release only when exact raw rows are actually erased; queue reservations release once on terminal outcomes. Failed jobs do not silently reset budgets. Scheduling remains an explicit operator task.

Configure only `DZOKA_AI_CORE_URL`, `DZOKA_AI_TOKEN`, `DZOKA_AI_TENANT`, `DZOKA_AI_PROFILE`, `DZOKA_AI_PERCEPTION_DIR` and optional explicit loopback-test flag, then run:

```powershell
python tools/perception_worker.py --once
```

The launcher strips DB/S3/migration credentials and user Python paths. The HTTP client follows no redirects/proxies, requires HTTPS except explicit loopback tests, and bounds bodies/time. The child has a 110-second process deadline; native sessions use CPU only/two sequential threads. Missing/corrupt artifacts or unsupported inputs remain failed/unavailable; there is no downloader, heuristic global-model fallback or cloud substitution. Deployment-level OS memory/CPU/egress isolation still needs the target configuration; this local host process is not a production sandbox certification.

## Select exact multimodal snapshots

Fresh results are SHADOW. Extend a [rollout plan](fingerprint-rollouts.md) with `perceptionPins`, for example:

```json
"perceptionPins":[{"reportId":"33333333-3333-4333-8333-333333333333","componentId":"44444444-4444-4444-8444-444444444444"}]
```

Every pin must belong to a declared report/revision and be a retained success in an enabled exact profile. Up to 64 explicit pins join the text-slot denominator. A perception-only cohort may use an empty `spaces` list, but must have nonempty pins. There is no automatic latest-image/result choice. Sealed manifests bind exact input/profile/output hashes; existing compare-and-set promotion/rollback atomically selects the pinned text/perception evidence. Current ItemFingerprint includes only ACTIVE perception components and corresponding modality availability. Unknown/noncohort/new-revision evidence never falls through to unapproved results.

Delete/expire/hold rules remain authoritative. Run `python tools/core.py purge-ai` with a separately granted `reports:purge` maintenance identity; it now erases perception components/raw WAV, cancels jobs and releases actual raw/queue reservations, respecting holds. Erased components cascade selected snapshot references; old declared UUID pins remain minimal tombstones and cannot reactivate missing data. Independent backup/replica/deletion-journal reconciliation remains required before releasing a real restore.

## Verify

```powershell
python -m unittest discover -s tests -q
python tools/test_intake.py --with-ai --with-perception --restore-drill
```

`--with-perception` verifies exact artifacts, generates an entirely synthetic English WAV with the installed Windows speech synthesizer (test fixture only, no recording/voice-weight redistribution), then exercises all four real providers through HTTP/JWT/S3/decoder boundaries. No native fixture/provider is silently replaced if unavailable. Default portable CI runs lifecycle/validation contracts and explicitly excludes native models; other native platforms require adoption/conformance review. `tools/verify_local.py` now requests all these provider checks by default. See [STATUS](../../STATUS.md) for actual counts/results, not assumptions from this guide.
