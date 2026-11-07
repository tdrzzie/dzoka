# Windows worker service deployment and recovery

[ADR 0038](../adr/0038-local-worker-service-and-recovery.md) adds an explicit service host around the seven-family [supervisor](worker-supervision.md). No model, inference source/profile, image, runtime library or database migration changes. This is a Windows/local Docker deployment slice; vendor Shona remains excluded. Production and full M4 acceptance remain open.

## Protected deployment

Use an accountable dedicated non-administrator service identity, a protected reviewed release, separately protected configuration/credential files, a private service-state directory and stable private Docker-state directories. Do not deploy directly from a shared writable checkout. The current development drive grants Authenticated Users Modify; deployment preflight rejects it. The host must have the reviewed Python/native environments, exact source/model/runtime locks and existing pinned images. No install/download fallback is provided.

The service manifest has exactly these fields (paths are examples, never secret values):

```json
{
  "schemaVersion": 2,
  "accountSid": "S-1-5-21-111111111-222222222-333333333-1001",
  "integrityRid": 8192,
  "plan": "C:\\Dzoka\\private\\plan.json",
  "credentials": "C:\\Dzoka\\private\\credentials.json",
  "stateDirectory": "C:\\Dzoka\\state"
}
```

Replace the synthetic SID with the accountable target identity and explicitly select its measured/approved integrity RID (4096 low, 8192 medium, 12288 high, 16384 system). SCM mode requires v2 and exact actual-token matching; shared built-in service identities and enabled administrator/elevated tokens are rejected. Integrity is not an account privilege: system-service MIC can differ from interactive MIC without making the account LocalSystem. A changed level fails instead of being silently accepted. This is an identity assertion, not a complete privilege inventory or account provisioning. Legacy v1 is retained only as the existing foreground development path and cannot activate SCM.

The plan retains the existing schema, explicit tenant/profile assignments and `tokenEnvironment` references. The credentials file is a private JSON object containing exactly those reference names and operator-provisioned token values. Never commit it, include it in status/arguments, or use machine-wide environment variables. Startup snapshots it; rotation requires a controlled stop, replacement and restart. Core expiry/revocation rules still apply. State must be separate from the plan/credential directory and retained across restarts. The service does not register profiles, mint tokens, create jobs or authorize release.

Protect each private directory with an explicit DACL granting only the service SID, SYSTEM and Administrators, with inheritance disabled. Protect files with file grants (`F`), directories with inheritable grants (`(OI)(CI)F`); do not apply directory-only grants to files. Existing explicit grants must be reviewed/removed by their accountable owner. Do not recursively change a shared checkout or a drive's ACL to make a check pass. OWNER RIGHTS is evaluated only after validating the actual owner. Deployment rejects complex deny rules for separate review; it is a deliberately conservative accepted ACL profile, not a general Windows effective-access calculator.

Source files, release directories and the Python executable reject broad write access. Windows TrustedInstaller ownership is accepted for public release/ancestor checks, with its fixed well-known SID; private credentials/state still require the narrower owner set. Parent checks reject effective delete-child/delete/write-DACL/write-owner permissions that could replace protected paths; inherit-only rules do not grant effective ancestor access. Private ACLs additionally exclude unrelated readers/inheritance. Paths must be local drive-absolute, exist, and contain no reparse components. The selected installed virtual environments and base Python tree are also recursively audited (30,000-entry bound per tree), including explicit writable leaf ACLs and reparse points, because their code runs before model checks. This checks access policy, not executable/library signatures or runtime artifact identity; existing runtime/model conformance still supplies the latter. Checks do not certify antivirus, host egress or arbitrary same-account code. The fixed hash-checked ACL script runs with process-scoped PowerShell `ExecutionPolicy Bypass`; no machine/user execution policy is changed. Policy is not an OS privilege boundary.

```powershell
C:\Python314\python.exe -I C:\Dzoka\release\tools\worker_service.py --config C:\Dzoka\private\service.json --check
C:\Python314\python.exe -I C:\Dzoka\release\tools\worker_service.py --config C:\Dzoka\private\service.json --foreground
```

`--check` validates protected files/configuration/source identities and credential shape; it does not authenticate credentials, load every model, apply resource limits or claim jobs. Foreground and SCM modes share the same checked lifecycle. Foreground Ctrl+C requests graceful stop. The active worker invocation remains bounded; no new invocation starts after stop. Host shutdown policy can terminate a service earlier, so retain restart reconciliation even when graceful stop is configured. Do not interpret invocation success or a RUNNING status as completed jobs or model readiness.

## Windows SCM activation

The independently declared standard-library SCM adapter uses service name `DzokaWorkers`. Its service command is the fully quoted absolute Python executable, `-I`, the fully quoted absolute `worker_service.py`, `--config`, the fully quoted private manifest and `--scm`. Do not invoke `python worker_service.py --continuous` as an SCM executable; SCM requires its dispatcher and status/control-handler handshake.

The SCM adapter is implemented, but only its non-SCM rejection path is exercised here; foreground execution does not validate its positive registration/control handshake. An administrator must register that exact command with an explicitly selected least-privilege service identity and log-on-as-service rights, configure demand/automatic startup and a bounded restart policy, and verify the resulting account, command, DACL and recovery configuration. Never accept the SCM default LocalSystem account as an implicit deployment choice. Use institutional account provisioning/secret handling rather than embedding account passwords in shell commands. Start the service, inspect its protected status/core results, request stop through SCM and verify boot, account permissions and configured restart behavior (including failure actions for non-crash exits) before promotion. Do not install a second instance pointing at another copy of the same state merely to bypass a lock.

Run `python -B tools/verify_service_access.py` from the checkout for a read-only preflight. It reports the current token's SCM creation access and the query result for `DzokaWorkers`. On 2026-11-07 the local host returned error 5 for service creation access and error 1060 for the absent service. SCM installation, start, stop, boot and restart recovery require an administrator token and a provisioned service identity. Verify Docker daemon access under that service logon on the target host. Production service-account and daemon isolation, TLS, identity, secrets and institutional ownership remain activation requirements. [SCM access rights](https://learn.microsoft.com/en-us/windows/win32/services/service-security-and-access-rights) · [Service query API](https://learn.microsoft.com/en-us/windows/win32/api/winsvc/nf-winsvc-openservicew)

## Limits, status and recovery

Before polling, the service acquires a fixed exclusive instance file lock, verifies its release/source lock and inherits an outer Windows Job Object with read-back-verified limits: 8 GiB aggregate committed memory, 128 active processes, 50% host CPU and kill-on-close. These host limits cover member Windows processes; the Docker daemon/VM is outside the job and its containers retain their separately verified Docker limits. Existing worker Job Objects and Docker non-root/no-network/read-only/resource restrictions remain. Failure to establish the outer boundary prevents work. This is a fixed local capacity profile, not a measured production sizing result. CPU percentage is across the host, not a reserved core count. The service and workers still share one account; Job Objects do not remove Docker privilege or enforce host network egress.

Startup reconciles every distinct configured Docker-state directory **before polling**. Active owners or unresolved/ambiguous intents block service startup. Scoped cleanup checks exact journal/name/image/labels and never prunes Docker. Terminal journals remain; no auto-archive/discard-uncertainty option is introduced. Each later Shona invocation also retains guardian preflight/reconciliation. If the entire service dies, the outer job kills its guardian/host descendants too; daemon containers can survive until the next explicit/startup reconciliation. Loss of the daemon/host requires availability to return before cleanup can be confirmed. This is restart recovery on one host, not HA or cross-host failover.

The atomic `status.json` contains only opaque run ID, sequence, UTC timestamp and RECOVERING/RUNNING/STOPPED/UNAVAILABLE. It has no credentials, tenant/report IDs, private inputs, model paths or raw exceptions. It is a replaceable operational snapshot; durable resource histories and core audit/results retain authority. A stale RUNNING file never proves liveness and never overrides the kernel lock. SCM exit status is nonzero on startup/recovery/lifecycle failure; worker invocation failures retain the supervisor's capped retries/cooldown and do not falsely report completed jobs. Status can remain unchanged during one bounded invocation. Alerting, retention/archival, distributed fairness/capacity, live daemon restart and host-loss drills, Linux/remote deployment and full system disaster recovery remain separate engineering.

The [execution record](../testing/m4-worker-service-2026-11-01.md) contains final gate evidence, negative findings and the exact tested deployment boundary.

[Account/egress and recovery extension](account-egress-recovery.md) records the earlier offline pilot and terminal-journal archival. [Model broker schema 3](model-broker.md) adds actual isolated model execution for seven supported families under new profile/space identities. Use the explicit schema-v3 broker path for that boundary; historical schema-1/2 networked workers and older profiles remain unchanged compatibility paths. SCM accepts checked schema 2 or 3, and still requires target-account/daemon/boot acceptance. Run a prepared private release with `-I -B` to preserve its byte receipt inventory.
