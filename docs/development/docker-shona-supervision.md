# Docker and CPU Shona supervision

[ADR 0037](../adr/0037-owned-docker-shona-supervision.md) extends [bounded supervision](worker-supervision.md). It adds a `shona-cpu` assignment and five **new** supervised profiles, without replacing any older profile, source, model, image or runtime-lock identity. The old CPU worker remains available separately; the vendor wheel stays local-development-only and is excluded. No new runtime package, image build or weight download is needed.

## Explicit setup

Provision the existing [CPU Shona artifacts/runtime](shona-cpu.md) and pinned image. Use a protected, persistent state directory for resource intents; create it explicitly and reuse it across restarts. It must be separate from the model directory. The example below uses an ignored local test folder; a deployment needs accountable storage/ACL/retention ownership. Never erase or relocate pending/unknown intents just to resume work.

Available appended names are `SUP_CPU_EN_SN`, `SUP_CPU_SN_EN`, `SUP_CPU_EN_SN_KEEP`, `SUP_CPU_SN_EN_KEEP`, `SUP_CPU_SN_EN_AUTO`. Each binds the exact prior base profile key, runtime/image/artifacts and newly hashed guardian/IPC implementation. Their translation/KEEP/automatic output schemas and unverified trust markers are unchanged. These are not quality upgrades or calibrated language detection.

```powershell
python tools/shona_supervised_runtime.py check
python tools/shona_supervised_profile.py --tenant TENANT_UUID --profile PROFILE_UUID --profile-name SUP_CPU_SN_EN_AUTO
python tools/ai_supervisor.py --plan PRIVATE_PLAN_PATH --check
python tools/ai_supervisor.py --plan PRIVATE_PLAN_PATH --continuous
```

The profile tool emits a disabled INSERT. An authorized operator must register/enable the specific profile. Add a `shona-cpu` entry to the existing plan with `tenant`, `profile`, absolute `modelDirectory`, `tokenEnvironment` and an additional absolute `dockerStateDirectory`. The child model variable is `DZOKA_AI_SHONA_SUPERVISED_DIR`; the operator still supplies only a tenant-scoped core service-token environment reference, not a token value in JSON. The [synthetic example](docker-shona-supervisor.example.json) has no real identifiers or secrets. Plans still reject unknown fields, arbitrary commands, guessed providers and inherited AI overrides. Source upgrades need explicit review/re-pin/restart.

## Ownership and privacy

The supervisor starts a host guardian **before** the worker process tree. It is outside the worker's Windows kill-on-close Job Object and receives no core JWT, DB or S3 credentials. A separate control pipe has only one parent writer. Closing it after success, failure or parent-only death signals cancellation; the surviving guardian cleans up its daemon resource. Killing the guardian too, host power loss or Docker unavailability requires the journal/reconciliation path instead. This is not a separate installed HA service. Lifecycle ownership does not revoke the host account's Docker privileges: worker and guardian still run under the operator account. Production account/pipe-ACL and host isolation policy remain separate requirements.

The worker reaches only an authenticated bounded loopback IPC service with a fresh per-invocation key. The permitted operations are checking the fixed image, preparing one approved model container, and executing one bounded token request. No worker-selected executable, image, entrypoint, Docker endpoint, mount path or generic Docker API is accepted. The daemon is restricted to a validated local Windows named pipe, captured explicitly for subsequent Docker calls; changing the global Docker context cannot redirect an active invocation to another host. Remote daemons and Linux hosts are not supported by this reviewed host profile.

The guardian snapshots the approved image/model metadata, verifies file hashes and holds read-share-only model handles until cleanup. Only model/config/shared-vocabulary files are individually mounted read-only. It creates and journals the container before start, then enforces a 10-second prepared-resource lease and a 120-second native deadline. The worker tree retains its 290-second deadline under the core's 300-second lease; guardian startup/teardown are separately bounded. A guardian lifetime also handles a stalled parent. Docker-client I/O runs off the cancellation thread, so blocked stdin cannot prevent that thread from killing its own client and attempting removal.

Children retain the pinned non-root CPU image, no network/capabilities/new privileges, read-only root, two CPUs, 1,536 MiB memory, 64 PIDs and bounded temporary storage. Container logging and core dumps are disabled. The guardian verifies the created settings before start. No Docker socket, service token or private input file is mounted. Model token data travels only in local IPC/stdin/response memory; it is absent from command arguments and resource journals. Host memory/disk/endpoint and service-account policy still require production review.

A successful native RPC returns tokens only after confirmed removal. Failure, timeout or lost parent cancels/removes the exact owned container; no silent model/runtime fallback occurs. The five core job results remain private, lease/input/profile-bound unverified translations or unverified copied/proposed spans. AI cannot authorize release. The supervisor still reports **invocations**, not completed jobs or health; use authorized core status/results for outcomes.

## Reconciliation and failure handling

```powershell
python tools/shona_supervised_runtime.py reconcile --state ABSOLUTE_STATE_DIRECTORY
```

The journal stores opaque resource/owner IDs, approved image and append-only lifecycle states/timestamps, never input tokens/text, service secrets, tenant identifiers or private model paths. Writes are atomic and flushed before creation. An exclusive owner lock prevents another guardian/reaper from removing active work. Recovery reads only the explicitly selected directory and requires exact journal name, invocation/resource labels and image reference before removal. It never prunes Docker, deletes by prefix, or touches another user's/unrelated containers.

Daemon inspection failure is unavailable, not evidence of absence. Unknown/late create outcomes retain their intent; mere current absence cannot mark them removed. Unresolved orphan intents block new guarded invocations until safely reconciled or investigated by an accountable operator. The tool does not offer a bypass that simply discards uncertainty. Normal confirmed resources reconcile idempotently; active owners are skipped. Reconciliation and cleanup have operation budgets and retain unresolved state on budget/daemon failure. Keep the state directory protected and stable, reconcile after losing both parent and guardian, and archive only confirmed terminal history under a reviewed policy. State-directory capacity/retention and cross-host recovery are not automatically managed.

## Verification and remaining work

The full local gate includes real preparation expiry, parent-only death, active-owner exclusion, orphan recovery, unrelated same-image preservation and IPC-denial checks. Unit fault injection tests unavailable-daemon/unknown-create behavior without restarting the shared Docker daemon. Real core integration exercises all five appended profiles under supervision. The execution record distinguishes actual execution from mocked fault paths. No daemon-wide restart/failover, Linux-host, service-installation, distributed scheduling or representative Shona quality result follows from these tests. Production host/secret/TLS/IdP/policy ownership and permitted representative/native-speaker data remain required.
