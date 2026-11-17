# M4 bounded local model-capacity execution — November 17

The user requests remaining local M4 work on the current account and Docker Desktop, with Shona quality/model research deferred. [ADR 0050](../adr/0050-bounded-local-model-capacity-diagnostics.md) and the [operator guide](../development/model-capacity.md) define the new protected serial model-path collector. No dependency, model weight, historical provider/broker/service identity, actual deployed worker/task policy, account or shared Docker configuration changes.

## Implemented behavior

The collector binds a predeclared workload to exact private input/host-evidence/profile/release hashes. It permits at most four entries/twelve calls, separates warmups from measured samples, and uses fresh independent private state with a native owner lock. It exclusively creates a flushed/fsynced hash-chained JSONL ledger, retaining interruption and terminal evidence. All input hashes are checked before inference. Unknown truncation remains unknown; unavailable, unconfirmed cleanup, not-dispatched and unobserved coverage are distinct. No failed sample is retried invisibly. Unconfirmed integrity/cleanup aborts further dispatch. Admission budgets stop new dispatch and do not promise to interrupt an already bounded provider call.

Initial full model-bundle verification precedes timing. Each sample includes confinement, inference, cleanup and post-call boundary checks. Each call uses a fresh model process; warmups can affect OS caches, not establish loaded persistent-model latency. Existing child isolation/limits apply, while the managed-service parent's extra resource envelope and HTTP/DB queueing are not reproduced. This is not a concurrent scheduler, saturation/load test, production capacity or quality acceptance. Nearest-rank percentiles use available samples only with counts; two-sample p95/p99 are simply the maximum, not population tail estimates. Every outcome leaves SLO/approval NOT_ESTABLISHED, quality NOT_EVALUATED and release NOT_GRANTED.

## Complete corrected-source verification

Final executed source is `d5ace5cfc5c5c5e202e68de4a6a526652ae501056780baa64ed2eb09e0584467`. The fresh immutable private v6 management receipt is `20f71912c8908a297e7773333a26a066608a65eec77bcee9d75303f1bcefdd16`; the model-bundle receipt remains `2382b945fbe3532f6253246d969a41d7d51623b2cc737630c64a09f49738b027`. Collector protocol source remains `d98d38bb887ad0183cfcef119db489cf30408211339ac68e9347fd2ffea3824b`.

All **27 phases PASS** on that unchanged source, finishing at **16:23:58 UTC**: **224 Python regressions, 24 Java unit tests and 96 integration tests**, zero failures/errors/skips. All **53 isolated profiles plus three boundary regressions** pass on first attempt. Nine broker recovery/integrity cases, managed operations, private quality protocols, seven native English semantic-review checks, ten native capacity checks and fourteen installed-task cases pass. The actual installed-task core job persists successfully with provenance, private actor access and idempotency checks; that integration case takes 231.6 s. Logical restore verifies **35 tables/3,223 rows**, exact row/catalog equality, restored runtime-role/two-tenant isolation and source-snapshot independence. Same-cluster logical restore with existing roles is not object backup, tombstone replay, HA, PITR or production RPO/RTO acceptance.

| Byte-preserved execution evidence | SHA-256 |
|---|---|
| [Full gate](artifacts/m4-capacity-20261117/gate-receipt.json), original `.test-tmp/local-verification-20261117T150157070425Z/result.json` | `e82e5a069224c4d22b0eba27e82a2cb2ca771df310d548622e43b7f5e9528788` |
| [Restore](artifacts/m4-capacity-20261117/restore-receipt.json), original `.test-tmp/foundation-9d09dfc4-7048-4f9b-a3c3-e086a1345c8c/restore-result.json` | `ae4811b870db69cfb5b6882f05db990ee56486cdbb221aa093488c58d4546d9e` |
| [Full-run native capacity result](artifacts/m4-capacity-20261117/full-native-result.json) | `f47c53d10e6ab8f351d600f280fc51744c3a75bd66d0a0a0023d61b76549c2e4` |
| [Full-run capacity ledger](artifacts/m4-capacity-20261117/full-native-ledger.jsonl) | `f79190417c657d6baa50da03a7ef29661eed4fc4b74b90c1ba900c991e3643f6` |
| [Full-run budget ledger](artifacts/m4-capacity-20261117/full-native-budget-ledger.jsonl) | `e243fcfd48f1d8ffe3801c8700f40102961884e89fc3f7b1fed22d4b7635b4c4` |
| [Full-run installed-task result](artifacts/m4-capacity-20261117/readiness-full.json) | `b68aaa6a5aaef3b2790a64500796aede201b3516bf8ae2d16d8d113175cfc5f1` |
| [Focused installed-task result](artifacts/m4-capacity-20261117/readiness-focused.json) | `51635d4913536ce363a66be3c84709ea635db95e24782da346cde9571b42dcb1` |

All 337 source files were checked against staged Git bytes. Three unchanged baseline files have pre-existing LF Git versus CRLF working-copy bytes; all changed source matches Git exactly. The [source overlay](artifacts/m4-capacity-20261117/source-overlay.json) reconstructs final executed source from canonical indexed digest `0f946a674a327f5f3f5873f4185918948e4fc96edac15915596ef057218571c2`. Separate historical overrides/exclusion reproduce the failed first source exactly. No source was normalized while the gate ran.

## Actual native capacity observations

Both focused and full native checks exercise a real text warmup, two measured text calls, and one deliberate invalid UTF-8 unavailable call without retries. A separate real one-second admission-budget run completes one warmup and leaves all three measured calls NOT_DISPATCHED with null percentiles. Native owner collision, broad input read rejection, exclusive output preservation and complete profile/grant cleanup are exercised. Raw inputs/outputs, credentials and host identity are not archived.

| Run | Available measured calls | Nearest-rank p50 | p95/p99/max | Unavailable measured calls |
|---|---|---|---|---|
| Focused | 2/2 valid-input calls | 27.7923111 s | 50.1232909 s | 1/1 deliberate invalid input; null percentiles |
| Full gate | 2/2 valid-input calls | 26.7085662 s | 29.6392426 s | 1/1 deliberate invalid input; null percentiles |

Collector/model code is unchanged between these sparse, uncontrolled shared-host observations. They do not establish an optimization, population tail, independent quality examples or production capacity. Retain runs separately; do not select or pool them to manufacture an improvement.

[Focused result](artifacts/m4-capacity-20261117/focused-result.json) SHA-256 is `b37cb3674157371625eb153bedf522b61cd51cdb87f15fb50922c18077a03246`; [ledger](artifacts/m4-capacity-20261117/focused-ledger.jsonl) `0fffa138fe0c86683100eccb3bc5539a090c2c95299ce94b5a7f8600148c4e1b`; [budget ledger](artifacts/m4-capacity-20261117/focused-budget-ledger.jsonl) `f8afda6fe0c6a8ca0a1cf4f09edcbad983e45a4516c1a6a0409bd00c78208797`.

## Retained failed attempt and readiness correction

The first full attempt on source `8833ea2e3941c754c919b0a707ab9ceeb23af20184b01ef291b2875a5e7e9b13` **FAILS** after twenty passing phases at installed-task explicit-restart readiness. Integration/restore did not run. The [failed receipt/source manifest](artifacts/m4-capacity-20261117/attempt-v1/manifest.json) preserves exact prior verifier/Java-test bytes plus exclusion of the later readiness-test file; reconstruction matches the whole earlier source. Failed receipt SHA-256 is `548c08690cd3935499575593243a2e1bbe21d21919f5d156a2ef523710048289`.

The original combined 360-second wait expires. Retained task results show restart requested at 14:27:00 UTC, a new STARTING observation at 14:31:19 and STOPPED after startup/drain during cleanup at 14:33:21. Restart count is two; the intermediate delay/failure cause is not established. A separate read-only configuration check takes 17.984 s and does not explain the whole delay. Slow/transient startup remains local operational debt, not an external excuse or a runtime defect claimed repaired.

The corrected verifier accepts only a different valid RUNNING generation with active owner lock. New STOPPED/UNAVAILABLE/CIRCUIT_OPEN fails immediately. Seven focused regressions cover stale/unowned/terminal/permanent-startup rejection, bounded waits and a measured slow-ready case. Initial/explicit startup has a 720-second diagnostic wait; the existing 180-second watchdog recovery check is unchanged. Controller/gate ceilings are 2,400/2,700 seconds. The real-core test has an 1,800-second parent envelope and synthetic-only 2,100-second JWT for startup/acknowledgement/cleanup. Actual worker code, Task Scheduler policy, restart interval/budget, production token/lease policy, limits, checksums, credentials and every model profile are unchanged. These test bounds are not production startup SLOs.

Observable readiness waits (initial/watchdog/explicit) are **127.025/149.084/108.997 s** in the focused replay and **133.269/167.245/130.905 s** in the full run. Both fit even the prior readiness windows. Replays establish current scoped behavior, not the original cause or a repaired intermittent-delay defect. Old private releases/failure fixtures remain retained.

## Broader authored workload

After complete gate cleanup, a separately frozen twelve-call diagnostic completes on unchanged verified source across two English draft and two English speech profiles. All four warmups and all eight measured calls are AVAILABLE, without retries or unconfirmed/not-dispatched/unobserved calls. The exact original draft JSON and existing authored synthetic WAV remain private. Host CPU/RAM observations, input authority and uncontrolled shared-host conditions are bound by host-evidence hash `27e61a24e0c0e036a440f4150f7fd3c7926592e9a8c5a6a5c7db7fc5eaa886db`; plan hash is `f50bd62763586aaf1077a97fe60c79f60e14e4789c8fa345bf0692c291de9923`.

| Existing profile | Available measured samples | Nearest-rank p50 | p95/p99/max | Measured truncation reporting |
|---|---|---|---|---|
| `ISO_GENERATIVE_LLM_EN` | 2/2 | 23.3714707 s | 23.8868375 s | Both input/output false on both calls |
| `ISO_QUALITY_SMOL360_EN` | 2/2 | 27.6062915 s | 34.2465775 s | Both input/output false on both calls |
| `ISO_PERCEPTION_SPEECH` | 2/2 | 27.8800596 s | 29.3743498 s | Both flags unavailable/unknown on both calls |
| `ISO_REFINEMENT_WHISPER_DISTIL_SMALL_EN` | 2/2 | 26.5902652 s | 28.2240601 s | Both flags unavailable/unknown on both calls |

These are separate task/profile observations under uncontrolled host conditions, not a ranking or comparative optimization/quality claim. They include confinement/cleanup/post-checks, omit the managed-parent resource envelope and HTTP/DB queues, and do not estimate population tails. No raw output semantic/transcription quality was scored. The private workload/host files are retained for authorized inspection; their authority declarations are not independently certified.

The [exact workload ledger](artifacts/m4-capacity-20261117/workload-ledger.jsonl) SHA-256 is `1bbee2b0b7848f7b1fc4a2acfde229383028d99a67de8563791a2168ed935630`; [non-content result](artifacts/m4-capacity-20261117/workload-result.json) `4d5c62740c857b354fff2df8daa65335e79251724c5fecff03ad74327341b214`. Post-run state reconciliation, complete model-bundle integrity/privacy verification and state-owner lock reacquisition confirm cleanup. The collector/release/profile hashes match the frozen plan, and SLO/approval remain NOT_ESTABLISHED with release NOT_GRANTED.

## Remaining acceptance and genuine blockers

This implements the missing bounded local model-path measurement tool; it does not complete operational capacity acceptance or M4. Representative public-reference comparisons, diagnostic target definition, end-to-end load/queue/saturation measurements, annotation/projection interfaces, startup-delay investigation and broader recovery work remain local engineering/acceptance work. They must not be relabeled as external-infrastructure blockers.

Genuine external inputs include the GitHub account billing unlock, institution-specific private data/access/approval where required, and authorized target credentials/privileges/policy for unattended daemon/egress/boot acceptance. The chosen current-account interactive worker works and is verified. Its current non-elevated token cannot create SCM services; no disruptive migration/restart, new account, host reboot or change to shared Docker configuration was used. Further Shona research/quality remains owner-deferred. No model promotion, property-release authorization, full-M4 or production acceptance is claimed.
