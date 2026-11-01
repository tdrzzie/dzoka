# M4 account assertions, offline boundary and journal recovery — 2026-11-01

[ADR 0039](../adr/0039-offline-account-boundary-and-journal-archive.md) and the [operator guide](../development/account-egress-recovery.md) govern this scoped extension. The mandatory thirty-source gate passed before feature work. No new package/model/image/inference identity or migration is adopted; the existing reviewed Python host is copied only into temporary native fixtures with its license and byte records. Earlier inference bytes/keys/notices and all thirteen supplied bodies remain preserved. The service orchestration lock explicitly evolves.

## Implemented and directly exercised

- Service schema v2 declares an exact account SID and integrity RID. Actual token checks reject wrong users/levels, enabled Administrators/elevation and shared built-in service identities. MIC and account privileges are separate: an explicitly declared system MIC does not imply LocalSystem membership. SCM activation requires v2; legacy v1 remains the existing foreground development path. The protected native service fixture executes v2. Account creation, all-privilege inventory, daemon ACLs and positive SCM logon/control/boot remain unexecuted.
- A fixed standard-library AppContainer pilot runs under the actual package SID with zero capabilities, no loopback exemption, explicit standard handles/environment and package read/execute access only to its staged runtime. Token/job membership is read back before resume. The job is attached atomically at creation (8 processes/512 MiB/kill-on-close); deadline and actual launcher-parent death terminate the exact child. No plain-process fallback is permitted.
- The parent proves IPv4 loopback/local-interface listeners reachable before/after the child's attempts, drains controls and independently checks no child connection arrived. Host-interface failure is paired with a native missing-capability diagnostic. The Windows diagnostic excludes loopback, so that path instead requires no-exemption policy, actual package token, a timeout and paired receiver controls. Timeout alone is never acceptance. `directOrCapabilityDenial` can be false for loopback: it must not be reported as a Winsock permission error. No firewall rule, capability or exemption is enabled.
- A private sibling fixture is actually denied while exact UTF-8 bytes including composed/decomposed and astral characters are hashed successfully. Poisoned parent credential/proxy variables do not appear in the child. No core JWT/DB/S3 access or model download is offered.
- Exact per-user profile state/UUID/account checks, active-owner exclusion, confirmed profile cleanup, owner-death reconciliation and idempotent recovery execute. Unconfirmed INTENT absence remains unresolved; wrong-account and malformed metadata cannot authorize cleanup. Failed private fixtures retain ownership metadata; successful cleanup checks the exact task-owned path before deletion.
- A real guarded CPU container reaches confirmed removal, then its entire journal is archived byte-for-byte with a flushed hash/length/name receipt. Reconciliation sees no active duplicate. Independently authored fault tests cover crash after receipt, exact retry, corrupt/conflicting copies, active-owner/archiver exclusion, nonterminal retention and retargeted/duplicate metadata rejection. Archive integrity does not establish signing, backup replication or power-loss durability.

The native offline phase has **13 executed checks**, distinct from the existing ten protected-service checks and six guardian checks. Its fixed stdlib probe is not a model provider. The full provider suite continues to test the previous actual models independently; no existing networked worker is silently rerouted through the offline pilot.

## Preliminary failures and corrections

The first offline launch returned Win32 error 203 because the minimal child environment omitted Windows profile-location variables. Only required non-secret Windows location variables were added. Initial socket assertions rejected timeout outcomes; missing-capability diagnostics plus independent listener observations and no-exemption policy now distinguish the measured loopback behavior from unreachable endpoints. Atomic creation-time job membership closes the suspended-child owner-death gap before this implementation is accepted. These early native trials are not counted as passing verification.

One preliminary run of the older process-tree unit test encountered WinError 32 while deleting its heartbeat fixture. Its cause is unconfirmed; the existing inference/process sources were not changed or the test skipped. Complete subsequent regressions pass. Only the final unchanged-source full PASS below is accepted evidence.

## Final unchanged-source verification

**PASS: all 20 phases, 115 Python tests, 21 Java unit tests and 89 integration tests**, zero failures/errors/skips. Thirteen new actual account/offline/recovery checks pass alongside the existing ten service and six guardian checks. Every prior native provider and core tenant/authorization/retry/private-source/activation scenario still passes. Restore matches **35 tables/2,867 synthetic rows**, exact rows/catalog and tenant/runtime-role/snapshot independence.

- Full result: `.test-tmp/local-verification-20261101T081623686309Z/result.json`.
- Result SHA-256: `ca060c5f34a659cc7c557b9cfc099f6557207ee9f4d4eb8a2d97b1e0a8537067`.
- Frozen source/lock SHA-256 before and after: `af0e336b47539f2edab3332a090ceb1f34ae0a04008be7d327a90236ea1afb4c`.
- Native phase record: `.test-tmp/worker-boundary-10cbfaeecae945b6b2d12607e2b02336/result.json`.
- Restore record: `.test-tmp/foundation-c3e60ac3-d1b8-4a9e-bf4e-9c218888e980/restore-result.json`.
- Started: `2026-11-01T08:16:23.687552+00:00`.
- Finished: `2026-11-01T08:37:57.268173+00:00`.

| Phase | Exit | Seconds |
|---|---:|---:|
| source-gate | 0 | 0.337 |
| architecture | 0 | 0.273 |
| python-regressions | 0 | 6.198 |
| java-artifact-lock | 0 | 10.04 |
| native-model-artifacts | 0 | 1.318 |
| native-model-conformance | 0 | 36.331 |
| perception-artifacts | 0 | 1.425 |
| joint-region-artifacts | 0 | 0.996 |
| language-artifacts | 0 | 0.93 |
| generative-artifacts | 0 | 1.665 |
| generative-installed-runtime | 0 | 20.257 |
| quality-artifacts | 0 | 1.137 |
| shona-cpu-artifacts | 0 | 0.941 |
| shona-cpu-native-probes | 0 | 146.422 |
| docker-guardian-lifecycle | 0 | 36.213 |
| worker-service-recovery | 0 | 171.004 |
| offline-boundary-and-archive | 0 | 15.266 |
| shona-artifacts | 0 | 0.844 |
| shona-installed-runtime | 0 | 29.0 |
| real-provider-integration-and-restore | 0 | 810.018 |

Command: the reviewed `JAVA_HOME` plus `python tools/verify_local.py --pg-bin` and `--maven` native paths from the existing service execution guide. Logs, synthetic backup, profile metadata and copied runtime bytes remain private/ignored. No remote CI, upstream-suite, representative model-quality, load or full-system HA result is claimed.

## Remaining scope and blockers

Existing model workers still use the prior host account/network boundary. A credential-holding broker, bounded pipe/typed messages, sanitized input/model staging, native runtime packaging and newly keyed inference adoption are local engineering before AppContainer protects model execution. IPv6/UDP/DNS/public-internet behavior, hostile-code/OS escape acceptance and model-specific capacity are not demonstrated by the IPv4 fixtures. Existing Linux inference children retain their separate Docker network-none limits.

The current read-only SCM probe returns Win32 error 5 (`.test-tmp/account-egress-scm-access.json`). Current SCM installation needs a local administrator token, a selected dedicated identity and logon rights; source/config checks do not provide them. Enabled-privilege/daemon policy, service-logon/control/boot acceptance, archival capacity/retention/backup ownership, live daemon/host/power loss, broader restore and distributed HA remain local engineering/activation requirements. Real target infrastructure/TLS/IdP/secrets/policy ownership and consented representative/native-speaker datasets require external resources/decisions. Language quality, AI trust and release authority are unchanged; remote CI is unverified. Full M4 and production readiness remain incomplete.
