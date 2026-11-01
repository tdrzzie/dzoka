# M4 Windows worker service, host limits and restart recovery — 2026-11-01

[ADR 0038](../adr/0038-local-worker-service-and-recovery.md) and the [deployment guide](../development/worker-service.md) define the scoped implementation. The mandatory thirty-source gate passed before feature work. No new package, model, image, inference key or migration was adopted; all earlier AI bytes and thirteen supplied document bodies are preserved. This record concerns local Windows service lifecycle/resource/restart behavior, not full M4 or production acceptance.

## Implementation and executed scope

- Separately source-pinned service manifest, existing supervisor-lock binding and strict protected configuration/credential snapshots. Exact token reference names are required; unknown/sibling/DB/cloud credentials are excluded from supervisor input/status. No secret values are accepted in command-line configuration or printed in errors.
- Conservative actual NTFS deployment preflight rejects broad private access, unprotected private DACLs, untrusted owners, writable release/interpreter/runtime files, writable replacement permissions on ancestors and reparse/remote paths. Existing trusted Windows ancestor/OWNER RIGHTS semantics are explicit. Runtime-tree audits include explicit writable child exceptions; they do not assert artifact signatures or account privilege separation.
- An exclusive file lock owns the service instance. Atomic redacted lifecycle status is not a lease/job health indicator. Recovery reconciles every distinct configured Docker-state directory before polling; active or unresolved intents block startup. Core job leases/retry/tenant authorization/result provenance and unverified AI trust are unchanged.
- Independently declared Windows Job Object limits are applied/read back before polling: 8 GiB aggregate committed memory, 128 active host processes, 50% host CPU, kill-on-close and inherited membership. The daemon/VM is outside the Windows job; container limits remain separate. Readback/assignment is executed; this is not a resource-exhaustion/load benchmark or a host network/OS account sandbox.
- Standard-library SCM dispatcher/status/control-handler adapter plus the same foreground lifecycle. Unit execution proves rejection of a non-SCM launch without starting work. Positive SCM registration/control/boot/restart-policy acceptance is unexecuted.

The final gate's ten native service checks passed at `.test-tmp/worker-service-8d54af1863d645f58bfc1120058c4844/result.json`: actual private ACL acceptance, broad reader denial, kernel limit readback, active process exclusion, lock release after process death, recovery after whole service/guardian loss with a previously RUNNING container, idempotent reconciliation, explicit writable runtime-leaf denial, protected foreground startup/poll/stop and writable-release rejection. The whole-owner fixture kills only its task-owned process tree and reconciles only the matching journaled resource. It does not restart Docker or simulate host/daemon outage. Recovery is invoked directly in that fault drill; unit ordering proves the service calls the same recovery path before polling.

The protected foreground fixture stages reviewed code and synthetic configuration in a task-owned private user-profile directory, verifies the actual base interpreter ACL tree and uses a deliberately unavailable private worker executable. It establishes deployment/lifecycle/failure containment, not successful inference or credential authentication. Temporary staging is removed afterward. The full integration's finite service-boundary entry point separately exercises all five actual private supervised CPU Shona profiles within the inherited kernel limits, retaining raw/trust/tenant/replay/removal assertions. It is not a production preflight/SCM bypass.

Eight new regression cases cover ACL profile/ancestor/generic permissions, strict JSON limits, credential/environment exactness, source binding, recovery-before-poll/stop order, unresolved/active recovery denial, instance/stale-status behavior and non-SCM API rejection. These are independently authored tests, not upstream test execution.

## Corrections and rejected evidence

An initial PowerShell ACL inspection was refused by execution policy. The fixed source-pinned read-only script now uses process-scoped policy for that invocation; no global policy changes. Native fixture trials rejected directory-only grants applied to files, unrecognized OWNER RIGHTS and TrustedInstaller ownership, and default Temp ancestors with additional writable principals. File/ancestor semantics were corrected and the private fixture moved under the user profile; shared ACLs were not weakened. Failed diagnostics remain ignored and are not counted as passing tests.

The first full attempt was explicitly superseded when review identified the need to protect interpreter/runtime leaves before worker integrity checks. Its record is FAIL, not accepted verification; only its orchestrator was stopped and its already-running bounded provider probe completed independently. A subsequent attempt completed all 89 integration tests and restore, but its frozen record was superseded by a credential-unit portability correction: the test now isolates Windows/cached-runtime prerequisites instead of depending on them in Ubuntu governance CI. Its changed-input record is not accepted full verification. Only the final unchanged-source PASS below is accepted full verification.


## Final frozen-source verification

**PASS: all 19 phases, 107 Python tests, 21 Java unit tests and 89 integration tests**, zero failures/errors/skips. All five real supervised CPU Shona profiles pass under the service's actual inherited host limits through `verify_service_supervisor.py`; this finite test entry point uses the existing supervisor plan/core path, not SCM registration. Existing native providers, core tenant/retry/private-source invariants and multimodal activation still pass. Restore matches **35 tables/2,867 synthetic rows**, exact rows/catalog, tenant/runtime-role isolation and source-snapshot independence.

- Full record: `.test-tmp/local-verification-20261101T072528902943Z/result.json`, SHA-256 `3102d9d2f7f8d4e89a2b7f190d7703bcbd0de659ff24136545bd4cb51aa7a5bd`.
- Unchanged source/lock SHA-256 before and after: `b705541ede4f56aa3ffaec2e808dbadace6ee40001b9e64b3e3743edb1634f5a`.
- Started `2026-11-01T07:25:28.910950+00:00`; finished `2026-11-01T07:44:13.817368+00:00`.
- Restore record: `.test-tmp/foundation-d6878ac1-6eda-40a6-9067-34a8e3cc15ae/restore-result.json`.
- Full command: `python tools/verify_local.py --pg-bin 'C:\Program Files\PostgreSQL\18\bin' --maven 'D:\Projects\ai\dzoka\.audit-cache\apache-maven-3.9.16\bin\mvn.cmd'`, with reviewed `JAVA_HOME` set.
- Native service phase: `.test-tmp/worker-service-8d54af1863d645f58bfc1120058c4844/result.json` (ten actual checks). All private fixture logs/archives remain ignored. No upstream suites, representative quality/load result or remote CI result is claimed.

| Phase | Exit | Seconds |
|---|---:|---:|
| source-gate | 0 | 1.082 |
| architecture | 0 | 0.621 |
| python-regressions | 0 | 5.938 |
| java-artifact-lock | 0 | 1.487 |
| native-model-artifacts | 0 | 0.469 |
| native-model-conformance | 0 | 4.311 |
| perception-artifacts | 0 | 3.169 |
| joint-region-artifacts | 0 | 1.089 |
| language-artifacts | 0 | 0.787 |
| generative-artifacts | 0 | 1.093 |
| generative-installed-runtime | 0 | 33.948 |
| quality-artifacts | 0 | 1.453 |
| shona-cpu-artifacts | 0 | 0.916 |
| shona-cpu-native-probes | 0 | 89.058 |
| docker-guardian-lifecycle | 0 | 31.82 |
| worker-service-recovery | 0 | 132.895 |
| shona-artifacts | 0 | 3.015 |
| shona-installed-runtime | 0 | 34.445 |
| real-provider-integration-and-restore | 0 | 772.787 |

## Limits and activation requirements

A read-only create-service capability probe returned Win32 error 5 (`.test-tmp/worker-service-scm-access.json`). No SCM service/account, machine startup policy, shared ACL, global execution policy or shared-daemon restart was installed/changed. Activation needs a local administrator token, selected dedicated non-administrator identity/log-on-as-service rights, a protected provisioned release/runtime/configuration/state path and service-logon Docker access validation. These are local permissions/deployment prerequisites, not an external infrastructure certificate.

Same-account Docker privilege and host egress remain unisolated. SCM positive handshake/control/boot/failure actions, service-account/daemon ACL policy, distribution/archival/capacity, live daemon/host failure, Linux/remote deployment and system-wide HA/disaster recovery remain local engineering. Production hosts/registry/TLS/IdP/secret custody and accountable institutional policy require external resources/decisions. Representative consented Shona/native-speaker quality data is still needed; known model meaning/place-name/code-switch errors remain and AI cannot authorize release. Remote CI is unverified. Full M4 remains incomplete.
