# M4 bounded worker supervision — 2026-11-01

[ADR 0036](../adr/0036-bounded-private-worker-supervision.md) and [operator guide](../development/worker-supervision.md) govern this local implementation. No new dependency, model, runtime/profile identity, database migration or authority surface is introduced. The thirty-source gate passed before feature work. This is not full M4 or production acceptance.

## Implemented and tested scope

- Strict at-start private plan parsing: byte/assignment/time bounds, duplicate JSON/schema/assignment rejection, explicit tenant/profile/family/model-directory/token references, HTTPS or explicit development loopback and complete secret validation before launching any assignment.
- Six fixed existing launchers: text, perception, vision, language, generative and quality. No executable/argument paths supplied by a plan, no model defaults/downloads, no arbitrary inherited AI/DB/cloud/Python-path credentials. Startup digest binds the declared supervisor/bootstrap/launcher/worker-transport source inventory; model workers retain their own integrity contracts. Code upgrades require an explicit stop/review/re-pin/restart, not plan hot reload.
- Serial eligible passes and per-assignment cooldown with capped exponential failure backoff/reset. A failed assignment does not skip later eligible ones. Graceful stop starts no further invocation and allows the active bounded call to finish. Counts mean invocations, not jobs, queue drainage or health.
- Actual Windows process-tree tests: timeout stops a real descendant heartbeat; killing the supervisor parent stops its descendant through kernel job ownership. Ownership failure stops the waiting bootstrap before work. Descendant creation is gated until after Job Object assignment. No upstream process-management implementation is copied or executed.
- Redacted status, no worker stdout/stderr forwarding, no raw exception data. Unit tests inject a secret-bearing exception and poisoned/sibling environments; status and child environment exclude those values.
- Real supervised integration cases are included in the full gate: two rounds consume two durable text jobs with actual vectors and natural replay identity; a wrong-tenant credential cannot claim a queued text job and does not prevent a healthy language assignment publishing its real private unverified translation. Exact source/profile/core result checks remain authoritative.

The actual supervised native paths exercised are text and language. Other declared family launchers continue to execute through their existing native conformance suites; this is not a claim that every model/profile combination was independently run through the new supervisor. Source identity and shared dispatch/process ownership cover the six configured families. Shona vendor/CPU scheduling is deliberately excluded until its separate daemon/local-development constraints are addressed.

## Verification

Final unchanged-source verification **PASS**: all **17 phases**, **92 Python tests**, **21 Java unit tests** and **88 integration tests**, zero failures/errors/skips. Eight new Python process/plan/security scenarios and two new real supervised HTTP/native scenarios pass alongside every existing provider. Logical restore matches **35 tables/2,777 synthetic rows**, exact row/catalog equality, runtime-role/two-tenant isolation and source-snapshot independence. Existing 49 AI source/model/runtime/profile files remain byte-identical to the previous commit; the new orchestration inventory pins 15 source files.

- Full record: `.test-tmp/local-verification-20261101T051148425874Z/result.json`, SHA-256 `38127c739d2c697ebe13e99d831f06b2c43c6c1d98b712160475502ababd843a`.
- Frozen source/lock input SHA-256: `3c36a9b1f04c94009106f225e7935e9a71c23fce6afc5b35eef0759b1ff7bdd3` before and after execution.
- Started `2026-11-01T05:11:48.426651+00:00`; finished `2026-11-01T05:27:28.710390+00:00`.
- Restore record: `.test-tmp/foundation-3a3ddc9e-34ee-4ee3-a8aa-1d7db33ab96c/restore-result.json`.
- Documented example plan passed `--check` with a synthetic environment token, without a core/network call or job claim. This verifies configuration/source validation, not credential or model readiness.
- Private diagnostics/artifacts remain ignored. The complete command was `python tools/verify_local.py --pg-bin 'C:\Program Files\PostgreSQL\18\bin' --maven 'D:\Projects\ai\dzoka\.audit-cache\apache-maven-3.9.16\bin\mvn.cmd'`, with reviewed `JAVA_HOME` set.

| Phase | Exit | Seconds |
|---|---:|---:|
| source-gate | 0 | 0.353 |
| architecture | 0 | 0.256 |
| python-regressions | 0 | 12.045 |
| java-artifact-lock | 0 | 5.372 |
| native-model-artifacts | 0 | 2.639 |
| native-model-conformance | 0 | 11.867 |
| perception-artifacts | 0 | 2.578 |
| joint-region-artifacts | 0 | 1.197 |
| language-artifacts | 0 | 0.922 |
| generative-artifacts | 0 | 2.039 |
| generative-installed-runtime | 0 | 5.024 |
| quality-artifacts | 0 | 1.531 |
| shona-cpu-artifacts | 0 | 2.092 |
| shona-cpu-native-probes | 0 | 229.421 |
| shona-artifacts | 0 | 0.935 |
| shona-installed-runtime | 0 | 5.106 |
| real-provider-integration-and-restore | 0 | 656.337 |

The first full run rejected an incorrect integration assertion: the older French translation schema has `opus-fr-en-greedy-v1` representation and explicit `fr`/`en` direction, not the newer multilingual trust field. Both real supervisory behaviors had executed successfully, but that run is FAIL and is not accepted verification. The assertion was corrected to the existing representation/direction/output contract; no model/provider evidence schema or profile key was changed. The final PASS record above supersedes that failed run. The 92-test Python regression run already passed, including eight new supervisor tests; these are independently authored tests, not upstream test-suite execution. Unprivileged Java and one Python child launch returned Access Denied and were not counted as accepted verification; the permission-approved native runs passed without skips.

## Limits and next work

Windows ownership and native localhost behavior are exercised. POSIX cleanup code has not been executed here; abrupt POSIX parent-death cleanup remains unresolved. No Windows service installation, distributed parallel scheduling/fairness, production CPU/memory/egress enforcement, periodic health/alerting, Kafka/outbox transport or supervised Docker ownership is delivered. These remain local engineering work. Operator tokens/identities, production hosts/TLS/secrets/service ownership and institutional policy acceptance require real external resources or decisions. Representative multilingual data/native-speaker review remains required for language quality; this scheduling change does not repair the recorded model errors. Remote CI is unverified by local runs. AI cannot authorize release.
