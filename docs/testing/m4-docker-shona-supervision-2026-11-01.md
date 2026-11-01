# M4 Docker and CPU Shona supervision — 2026-11-01

[ADR 0037](../adr/0037-owned-docker-shona-supervision.md) and the [operator guide](../development/docker-shona-supervision.md) govern this scoped implementation. The mandatory thirty-source gate passed before feature work. No new model, image, runtime library or database migration was adopted. Five separately keyed profiles extend the existing reviewed CPU Shona runtime; previous inference sources, keys, model/runtime locks and supplied research bodies are preserved. Vendor Shona remains local-development-only and excluded from supervision.

## Executed verification

Final unchanged-source gate **PASS: 18 phases, 99 Python tests, 21 Java unit tests and 89 integration tests**, zero failures/errors/skips. Real native PostgreSQL 18.6, SeaweedFS, the existing sandbox/native providers and pinned Linux CPU image ran on the Windows host. Logical restore matches **35 tables/2,867 synthetic rows**, exact row/catalog equality, tenant/runtime-role isolation and source-snapshot independence. This is synthetic conformance, not a representative quality or performance benchmark.

- Full record: `.test-tmp/local-verification-20261101T055730355998Z/result.json`, SHA-256 `feda9f3b33a201b1465752e106b1d913e5ae6240a7544c1e95c8cb013844aba5`.
- Frozen source/lock SHA-256 before and after: `e5502b7fe9f0547a3ba9c16bec842db30afdb4d2c667191adb9ea195af46c09f`.
- Started `2026-11-01T05:57:30.364996+00:00`; finished `2026-11-01T06:07:30.763940+00:00`.
- Restore record: `.test-tmp/foundation-39b6573a-3752-4ca7-9e01-1446b96e11d0/restore-result.json`.
- Command: `python tools/verify_local.py --pg-bin 'C:\Program Files\PostgreSQL\18\bin' --maven 'D:\Projects\ai\dzoka\.audit-cache\apache-maven-3.9.16\bin\mvn.cmd'`, with reviewed `JAVA_HOME` set. Private diagnostics and synthetic archives remain ignored.

| Phase | Exit | Seconds |
|---|---:|---:|
| source-gate | 0 | 0.324 |
| architecture | 0 | 0.233 |
| python-regressions | 0 | 5.644 |
| java-artifact-lock | 0 | 7.345 |
| native-model-artifacts | 0 | 1.086 |
| native-model-conformance | 0 | 26.73 |
| perception-artifacts | 0 | 0.932 |
| joint-region-artifacts | 0 | 1.691 |
| language-artifacts | 0 | 1.358 |
| generative-artifacts | 0 | 1.685 |
| generative-installed-runtime | 0 | 6.008 |
| quality-artifacts | 0 | 1.292 |
| shona-cpu-artifacts | 0 | 0.836 |
| shona-cpu-native-probes | 0 | 83.724 |
| docker-guardian-lifecycle | 0 | 20.56 |
| shona-artifacts | 0 | 0.476 |
| shona-installed-runtime | 0 | 14.063 |
| real-provider-integration-and-restore | 0 | 425.828 |

`DockerShonaSupervisorIT` executes all five appended profiles through the real supervisor/guardian/core HTTP path: plain English→Shona, plain Shona→English, both caller-marked copy directions, and conservative unmarked English-window proposals. It verifies successful private results, exact raw input preservation, immutable replay identity, unverified translation/proposal trust, retained marked names/selected time qualifier, cross-actor denial, redacted output and all five journals ending REMOVED. Earlier provider, tenant, retry, activation and process-tree suites still pass. No model accuracy improvement is claimed.

The tracked `docker-guardian-lifecycle` phase passes six actual daemon checks: active-owner exclusion, idempotent orphan reconciliation, unrelated same-image preservation, unauthorized IPC denial, prepared-container deadline removal and parent-only-death removal. The last two tracked fixtures use CREATED containers. Record: `.test-tmp/docker-guardian-a1a343f146164e8990b67f7771121b21/result.json`.

A separate manual supplemental drill starts the same approved image detached (waiting for stdin, without private input), confirms RUNNING, then separately forces owner-parent death and prepared-lease expiry. Both exact resources are confirmed removed. Record: `.test-tmp/docker-running-drill-e0848d642e114808974f3536e5a9cfa9/result.json`. These two checks supplement the gate; they are not extra tracked phases/tests. They do not simulate daemon outage or host loss.

Seven new unit scenarios cover exact ownership tuples, unknown-create uncertainty, unavailable-daemon preservation/active locks, malformed journal rejection, bounded strict IPC JSON, fixed operations/token bounds and new profile/source identities. Unavailable-daemon faults are injected; the shared daemon was not restarted. Native lifecycle execution first found a buffered-stdin watcher causing abnormal guardian shutdown after removal; switching the EOF watcher to raw OS reads resolved it before the final frozen run. An earlier ad hoc console diagnostic hit Windows console encoding while printing Unicode tokens; that diagnostic was not accepted as a passing test. The final real profile jobs and full gate above pass.

## Remaining local work and external requirements

Lifecycle ownership is scoped to the Windows host and a captured local Linux Docker named-pipe endpoint. The guardian survives parent-only death, owns bounded creation/start/removal and retains durable uncertain outcomes. Losing parent and guardian together, daemon/host outage or an ambiguous late create requires later scoped reconciliation. Unknown absence never becomes successful cleanup; unresolved intents block new guarded work. No shared-daemon restart, cross-host/HA, remote daemon or Linux-host behavior was verified. Worker and guardian use the same operator account; authenticated IPC is not OS privilege separation.

Local engineering still includes installed service ownership, OS account/pipe-ACL isolation, daemon-wide/cross-host recovery, journal archival/capacity policy, distributed fairness/capacity, deployment/SBOM/advisory/load work and portable host validation. Production hosts/registry/TLS/IdP/secrets, accountable institutional policy and permitted representative/native-speaker datasets require external resources or decisions. Remote CI remains unverified. Existing Shona meaning/place-name/code-switch errors, including the recorded “bylack” output, remain; translation/copy proposals stay unverified and cannot authorize release. Full M4 and production readiness remain incomplete.
