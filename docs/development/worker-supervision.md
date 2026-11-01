# Bounded private worker supervision

[ADR 0036](../adr/0036-bounded-private-worker-supervision.md) governs this standard-library-only local polling supervisor. It invokes the existing reviewed one-job workers, without changing their model/runtime/profile identities. Core queues, tenant authorization, leases, retries, quotas and result validation remain authoritative. It does not create jobs, activate evidence, publish outbox events or replace the Kafka-compatible architecture.

Copy the [synthetic example](worker-supervisor.example.json) to a private operator location, then explicitly configure the actual core endpoint, tenant UUID, enabled space/profile UUID, absolute model directory and token environment-variable reference for each assignment. Do not store token values in the plan or commit real operational plans. A plan supports 1–16 assignments, a 1–300-second successful polling interval and capped failure cooldown up to 3,600 seconds. Duplicate tenant/family/profile assignments, unknown fields, duplicate JSON keys, unsupported families, relative/missing directories and missing/invalid token references fail before any child starts. All assignments' secrets validate before launching the first worker.

Supply service tokens through `DZOKA_WORKER_TOKEN_...` environment variables using the operator's secret mechanism. Tokens need the existing tenant-scoped `ai:work` membership; workers receive no database/S3 authority. HTTPS is required. HTTP is accepted only for explicitly enabled `127.0.0.1`/`localhost` development. No environment HTTP proxy or inherited `DZOKA_AI_*` overrides are forwarded. The child receives only the selected assignment's token/tenant/selector/directory and a small platform/TLS environment allowlist; sibling tokens and DB/cloud/Python-path variables are excluded.

```powershell
python tools/ai_supervisor.py --plan PRIVATE_PLAN_PATH --check
python tools/ai_supervisor.py --plan PRIVATE_PLAN_PATH --rounds 2
python tools/ai_supervisor.py --plan PRIVATE_PLAN_PATH --continuous
```

`--check` verifies configuration, secret presence/shape and implementation identity; it makes no network call and does not prove credentials, model availability or inference quality. `--rounds N` performs N eligible passes through the assignments, respecting cooldowns between passes (1–1,000). `--continuous` polls until stopped. Plans and tokens are snapshotted at startup; restart to change them. Child invocations bind the startup lock digest and reject changes in the declared supervisor/bootstrap/launcher/worker-transport source inventory or lock. Provider implementations retain their own integrity contracts; review and restart for code upgrades.

| Family | Existing launcher | Model directory variable | Selector | Deadline |
|---|---|---|---|---:|
| text | `tools/ai_worker.py` | `DZOKA_AI_MODEL_DIR` | space UUID | 110 s |
| perception | `tools/perception_worker.py` | `DZOKA_AI_PERCEPTION_DIR` | profile UUID | 110 s |
| vision | `tools/vision_worker.py` | `DZOKA_AI_VISION_DIR` | profile UUID | 110 s |
| language | `tools/language_worker.py` | `DZOKA_AI_LANGUAGE_DIR` | profile UUID | 110 s |
| generative | `tools/generative_worker.py` | `DZOKA_AI_GENERATIVE_DIR` | profile UUID | 290 s |
| quality | `tools/quality_worker.py` | `DZOKA_AI_QUALITY_DIR` | profile UUID | 290 s |

No arbitrary executable, arguments or guessed provider are accepted. Shona vendor/CPU families remain outside this supervisor. The vendor profile keeps its local-development limitation; CPU Shona's Docker-daemon child needs separately integrated ownership/cleanup before unattended supervision. The one-way ONNX Shona profile in the quality family is separate and remains supported under that family's existing contract.

One invocation per eligible assignment runs serially; another eligible assignment still runs if an earlier one fails. Each failure doubles that assignment's interval up to the configured cap; success resets it. Startup, HTTP, native-provider and deadline failures cannot become a model fallback or implicit success. The core retains its own retry budget and idempotent completion. `OK` means only that the one-job command exited successfully; it may have found no eligible job. Counts are **worker invocations**, never completed-job, health, accuracy or queue-drained metrics. Use authorized core status/result reads to establish actual outcomes.

The worker bootstrap waits for a go byte. On Windows it is assigned to an unnamed kill-on-close Job Object before permission to launch the fixed worker. A deadline, normal cleanup or supervisor-process death closes the sole owned job handle and terminates the inherited process tree. Ownership failure kills the waiting bootstrap before any work. No breakaway permission is granted. The POSIX path starts a new session and kills its own process group on cleanup; it has not been executed on a Linux host here, and abrupt POSIX parent-death cleanup is not established. Windows kernel APIs are declared independently through the existing Python ctypes library; no new library/toolchain is installed.

SIGINT/SIGTERM requests stop new invocations and let the active invocation finish within its deadline. This is not durable service installation; Windows console Ctrl-C and normal OS signal delivery depend on the launching service. Forced termination is covered by Windows job ownership, not a graceful completion guarantee. Any unacknowledged/terminated inference recovers through core lease expiry and existing retry/retention rules. The supervisor does not complete or release leases itself.

Worker stdout/stderr is discarded. Console JSON contains startup plan hash, assignment count, numeric assignment index, coarse outcome/cooldown and invocation counts only; it never deliberately emits private text, tokens, tenant/profile identifiers, model paths or exception details. Logs still require operator retention/access policy. Any failed invocation makes a bounded run return nonzero, even if later invocations succeed; continuous operation reports failures without stopping the other assignments.

Remaining local engineering includes parallel/distributed fairness and capacity controls, periodic health/metrics, service deployment/restart/rotation, OS resource/egress isolation, supervised Docker ownership and Kafka/outbox integration. Production hosts, institutional policy/secrets/TLS/IdP ownership and representative language/model acceptance remain external requirements. Passing synthetic/native conformance does not establish whole-M4 or production readiness.
