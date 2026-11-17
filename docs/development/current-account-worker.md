# Worker deployment on the current Windows account

This opt-in path keeps the existing Docker Desktop installation and uses the logged-on account. It does not install SCM, store a Windows password, change Docker/WSL configuration or establish boot-before-login operation. [ADR 0047](../adr/0047-current-account-interactive-worker-deployment.md) records its separate source/runtime decision. The historical SCM path remains available under its own deployment requirements.

Use an immutable protected management release prepared with `tools/managed_release.py`, the existing private model bundle and a valid [managed broker policy](managed-broker.md). The reviewed installed PSF Python 3.14.7 GUI executable and console/DLL/license bytes must match `ai/interactive-deployment.lock.json`; runtime/code/control trees must pass the existing NTFS/reparse checks. Launch the control CLI from that protected release, using the reviewed installed console Python. A shared writable checkout intentionally fails deployment checks.

Create a protected configuration file, separate from model-readable bundles, with exactly these fields:

```json
{
  "schemaVersion": 1,
  "owner": "<32 lowercase hexadecimal characters, unique to this task>",
  "accountSid": "<actual current account SID>",
  "integrityRid": 8192,
  "policy": "C:\\private\\policy.json",
  "controlState": "C:\\private\\interactive-state",
  "managementReceipt": "<exact new management release.json SHA-256>",
  "watchdogStart": "<UTC YYYY-MM-DDTHH:MM:SSZ, initially about one minute ahead>"
}
```

The policy points to the existing schema-3 plan and private credential file. Do not put a token or password in this configuration. Protect the file and dedicated control-state directory with the existing `broker_bundle.protect` helper; verify their ancestors as described in the management guide. Retain the configuration's exact SHA-256. The task action contains only the pinned code/configuration paths and digest. Each task name is `Dzoka-Interactive-<owner>`; registration uses CREATE only, never overwrite.

From the protected release, invoke:

```powershell
python -I -S -B tools/current_setup_worker.py install --config C:\private\interactive.json --sha256 <configuration-sha256>
python -I -S -B tools/current_setup_worker.py inspect --config C:\private\interactive.json --sha256 <configuration-sha256>
python -I -S -B tools/current_setup_worker.py start --config C:\private\interactive.json --sha256 <configuration-sha256>
python -I -S -B tools/current_setup_worker.py stop --config C:\private\interactive.json --sha256 <configuration-sha256>
python -I -S -B tools/current_setup_worker.py remove --config C:\private\interactive.json --sha256 <configuration-sha256>
```

Install creates a disabled task. Explicit start enables its current-user logon and one-minute recovery triggers and resets its private configuration-bound launch budget. IgnoreNew plus the existing process lock excludes overlapping workers. A crash consumes another launch when the time trigger starts a replacement; after one initial and three replacement attempts, the next launch disables the task and records `CIRCUIT_OPEN` before core claims. Repair the cause and explicitly start again. Do not infer recovery from Task Scheduler's `RestartOnFailure` setting: an actual killed-process test failed that expectation, prompting the explicit watchdog.

Stop first disables future triggers, then publishes an atomically protected request for the current run nonce. It drains the bounded active invocation and confirms the matching STOPPED receipt and released kernel lock. A stale request cannot stop a different run. Remove disables triggers and holds the runtime lock while deleting only a fully matching inactive task; serialized control operations prevent start/remove races. Changed source/principal/command/trigger/ACL/configuration or ambiguous state fails closed. Restore the original protected configuration/definition before normal controls; never use a name-prefix deletion or bypass task ownership checks.

`interactive-status.json` is private redacted lifecycle metadata, including run/PID/time and deployment-lock digest. It is not model health or completed-job evidence. The managed service's separate counters and core durable results remain authoritative. Keep operation intents, receipts, budgets and failed fixtures. A status timestamp alone does not prove a live worker; the kernel instance lock supplies liveness.

Native verification installs temporary owned tasks, exercises hidden execution, Docker access, authenticated empty-queue polling, descriptor tampering, control concurrency, forced process death, periodic replacement, graceful stop, explicit restart and budget exhaustion, then removes only confirmed owned resources. A separate Java integration test completes a real protected Shona lease through PostgreSQL/HTTP under the installed task and checks private SHADOW evidence/idempotency. These tests do not simulate or certify an actual logout/relogin or host boot. Such tests interrupt this account/session and require an approved maintenance window; production identity/egress/privacy/language acceptance remains separate.
