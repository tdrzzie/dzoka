# Private asynchronous text fingerprints

This is the first working M4 provider: durable core jobs and one local multilingual-profile text embedding model. It extends [intake](intake-media.md). The [cohort lifecycle](fingerprint-rollouts.md) now controls text and explicitly pinned [local perception](perception.md) snapshots. This guide remains scoped to text; provider limits, wider language/format quality, larger-cohort/index migration and retrieval are separate. [ADR 0027](../adr/0027-local-text-inference.md) and [adoption evidence](../dependencies/ai-text-adoption.md) define the supported text profile.

## Reviewed runtime and model

The Java dependency graph remains 106 artifacts. Python is a separate workspace-local environment; the current native profile is reviewed/tested on Windows amd64 CPython 3.14.7. `ai/runtime.lock.json` records 21 exact wheels, source URLs, hashes, selected terms and notice digests; `ai/requirements.lock.txt` pins all packages and publisher wheel hashes. Other platforms need a separate native-wheel review rather than silently using a different binary. No Python dependency is needed to run the core API or Java lifecycle tests.

With the reviewed Python installed:

```powershell
python tools/ai_runtime.py fetch
python tools/ai_runtime.py install
python tools/ai_runtime.py check
python tools/ai_runtime.py probe
```

`fetch` downloads only reviewed immutable model files and locked wheel URLs, verifies SHA-256 and never loads a model. `install` uses local wheels only, `--require-hashes` and no source builds in `.audit-cache/ai-venv`. Runtime loading verifies installed package files against original wheel bytes before native imports. Inference reads verified model/tokenizer bytes, never pickle/remote code, and never downloads a model. The source/config/runtime hash identity in `ai/model.lock.json` must be reviewed and regenerated as a new space when those inputs change. Normal dependency checks must not rewrite locks to hide a mismatch.

The provider uses the publisher's quantized MiniLM ONNX export and exact serialized tokenizer, at most 128 tokens, mask-aware mean pooling and explicit L2 normalization to 384 finite dimensions. CPU sequential execution uses two intra-op threads; telemetry/HF runtime downloads are disabled. Truncation and token count are returned as provenance. Real nonconstant synthetic outputs establish executable conformance only; no retrieval quality, language coverage or calibrated confidence is claimed.

## Provision an approved space

Apply V4 with the separate migration process. An authorized database operator provisions an `embedding_spaces` row for the intended tenant; runtime API/worker roles cannot insert or edit spaces. Inspect the exact fields with:

```powershell
python tools/ai_worker.py --describe-space
python tools/ai_space.py --tenant 'your-tenant-uuid' --space 'new-space-uuid'
```

Replace the placeholders with actual UUIDs. The SQL generator prints a disabled INSERT and a commented activation command; it never connects to a database. Use the exact profile values unchanged for all model/preprocessing/space identity columns. Keep `enabled=false` until the artifact/runtime review and local probe pass; an operator may then enable the chosen row. Disabling a space blocks new claims/completion and removes it from the current component projection. This switch is artifact authorization, not a measured re-embedding rollout approval.

Grant ordinary reporters `reports:read`/`reports:write`, with the existing explicit staff `:any` permissions when needed. Provision a separate service membership with only `ai:work`; JWT role claims grant no local permissions. Production IdP service-account/token acquisition is operator integration, not a fake token generator in the worker.

## API and job lifecycle

All suffixes begin `/api/v1/tenants/{tenant}` and use bearer authentication.

| Method and suffix | Behavior |
|---|---|
| `POST /reports/{report}/fingerprints` | JSON `{"spaceId":"approved-space-uuid"}`. Requires report write authority; current source revision plus space is a natural idempotency key. Returns durable job status. |
| `GET /ai/jobs/{job}` | Report read authority; private status, attempts, source revision, deadline and safe error code, never input text/token. |
| `GET /reports/{report}/fingerprints` | Report read authority; an `ItemFingerprint` current view with schema/source revision, immutable components in separate approved spaces and explicit AVAILABLE/PENDING/UNAVAILABLE/FAILED coverage. Image/OCR/local-feature/speech components remain UNAVAILABLE. No implicit cross-space similarity or ownership decision. |
| `POST /internal/ai/jobs/claim` | Service `ai:work`; JSON space ID; returns one private leased normalized-text input, or JSON null. |
| `POST /internal/ai/jobs/{job}/complete` | Service `ai:work`; exact lease, input/space/provider/schema, generated time, normalized finite vector, token count and truncation envelope. Only a current matching lease/source can append; exact acknowledged replay is safe. |
| `POST /internal/ai/jobs/{job}/fail` | Service `ai:work`; exact lease plus whitelisted failure reason. Preserves work for bounded retry, never fabricates a component. |

Each tenant permits 32 queued/processing jobs through an atomic reservation ledger shared across actors, with consistent tenant-first locking; jobs expire after one day. Terminal transitions release each reservation once. Claims lease for 120 seconds and consume one of three attempts. Stale/deleted/revised inputs become OBSOLETE; late completion is refused immediately even before cleanup. Missing text produces UNAVAILABLE. Exhausted retries/deadlines produce FAILED. Successful completion, component insertion, report event-version increment and minimal audit/outbox are transactional. Input text, vectors, tokens and private artifact paths never enter events. Completed historical components are immutable and excluded from a newer report revision's current view. Once a tenant explicitly activates a [sealed cohort](fingerprint-rollouts.md), only its pinned current-source components are exposed; later completions remain shadow evidence until approved activation.

## Run the private consumer and erasure

Set `DZOKA_AI_CORE_URL` (HTTPS), `DZOKA_AI_TOKEN`, `DZOKA_AI_TENANT`, `DZOKA_AI_SPACE` and `DZOKA_AI_MODEL_DIR` for the chosen reviewed profile. Then run:

```powershell
python tools/ai_worker.py --once
```

The launcher strips DB/S3/migration/OIDC credentials from the child, ignores user-site/PYTHONPATH settings, and passes only the service's AI configuration and basic runtime environment. The client follows no redirects, uses no environment proxy, and bounds responses/requests/timeouts. Plain HTTP is accepted only on loopback with explicit `DZOKA_AI_ALLOW_INSECURE_LOOPBACK=true` for local tests. It has no database driver, storage credentials or ownership/release command. The trusted reviewed model runs locally; deployment-level memory/CPU/process isolation and egress restrictions remain operator hardening work. There is no hidden scheduler or default provider.

After deletion/expiry, schedule `python tools/core.py expire-intake`, `python tools/core.py purge-ai` and the media purge command as applicable. AI purge uses trusted core DB credentials and the separately provisioned `DZOKA_WORKER_*` identity with `reports:purge`; it deletes derived components and cancels jobs in bounded batches, respecting holds. Workers with only `ai:work` cannot invoke that maintenance authority. Backup/replica/index erasure and restored-tombstone replay remain operational work.

## Verification

```powershell
python -m unittest discover -s tests -v
python tools/test_intake.py --with-ai
```

`--with-ai` requires the reviewed local runtime/model and exercises the real HTTP claim–Python inference–completion path alongside PostgreSQL, JWT, S3 and Docker decoder tests. Without it the harness explicitly runs core/media and AI lifecycle contracts, excluding native inference. CI uses that portable contract scope; a CI file is not a remote result or a native model-platform approval. See [STATUS.md](../../STATUS.md) for actual counts and runtimes.
