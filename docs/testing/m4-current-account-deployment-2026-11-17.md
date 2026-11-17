# M4 current-account deployment verification — 2026-11-17

## Scope and implemented behavior

[ADR 0047](../adr/0047-current-account-interactive-worker-deployment.md) adds an optional installed Task Scheduler path around the existing protected managed broker on the user's current non-elevated Windows account and Docker Desktop. [Operator instructions](../development/current-account-worker.md) describe immutable staging, private configuration, install/start/inspect/stop/remove and recovery. This implementation does not migrate Docker, create another Windows account, install a global runtime or alter historical model/profile identities. No new Python wheel/Java dependency is introduced.

The direct hidden action uses the separately byte-pinned, valid PSF-signed installed CPython 3.14.7 GUI executable. Source, runtime, current SID/medium integrity, private release/control trees and exact task principal/action/trigger/ACL definitions are checked. Tasks register CREATE-only and initially disabled. Explicit start enables logon and periodic triggers; duplicate launches are excluded. A private configuration-bound launch budget permits one initial plus three replacements, then disables the task before another claim. Stop disables future triggers before draining the current nonce-bound run. Control operations are serialized; removal disables triggers and holds the runtime lock while deleting only the matching inactive task. Operation intents and ambiguous outcomes are retained.

Broker/core inference still uses the existing credential-holding trusted broker and capability-free model process. Credentials remain in private files, never task XML/arguments/status/model environments. The current independent deployment lock is `a58c07201b5dde7824787884ae0219f44177f744d24247639f248627744c8555`; its earlier `7b1ee4e7` bytes remain archived. Parent model/broker/service/management lock identities remain unchanged. Management staging has a new inventory receipt instead of overwriting earlier private releases.

## Completed native acceptance

The final focused installed-task run passes **14 actual native checks**:

- Installed least-privilege interactive task, current-account Docker preflight and authenticated empty-queue claim.
- Duplicate start exclusion, actual competing-control-lock rejection and wrong command/source descriptor denial.
- Exact registered-process forced death, periodic replacement and recovery before resumed claims.
- Generation-bound graceful stop, explicit restart, injected exhausted-budget boundary that disables the task before claims, and confirmed owned task/resource cleanup.

This is executed scheduler/process/ACL/Docker/protocol conformance, not a simulated task definition or a claim of real model completion. Result `.test-tmp/current-setup-0c84da8f1f644478937330b9b6cd3aff.json` has SHA-256 `fd67310ac143ab42e11dcde78418e8d165956eb70099782baeae2be5c2e68760`. Its immutable management release receipt is `6c6caf28467f5cb6a92f91c951cb67a6998d432418684117f489b0c0544c581b`. The preceding twelve-check run also passes, receipt SHA-256 `46dd0c9d997043a1c3c880c59891b905168a567118cda0f9bb9f30f7f3a67183`.

Four focused Python regressions cover configuration authority/type injection, stale stop generations and the restart budget's identity/bounds. The earlier full 182-test Python run passes and the real-core integration class compiles offline. The expanded complete gate adds `CurrentSetupWorkerIT` to the managed-provider matrix; it must prove an actual installed-task Shona lease/input/inference/completion through PostgreSQL/HTTP, private SHADOW evidence, raw-input identity and idempotency. The final gate below executes and passes that actual core test; the empty-queue native fixture alone does not supply it.

## Retained failures and corrections

The initial disabled permission probe registered successfully but the ownership guard expected a custom URI. Task Scheduler normalizes URI to its actual task path. A separate Source marker fixes that assumption; exact UUID/principal/no-op-action verification permitted cleanup of the retained disabled probe. The corrected non-elevated registration/cleanup probe passes.

The first installed-worker release starts and polls successfully, but native `RestartOnFailure` metadata does not restart an externally terminated process within the 180-second test window. Do not infer OS recovery from the XML or invent its root cause. The explicit periodic trigger and bounded budget implement the verified replacement path. A subsequent attempt fails closed because a newly written budget leaf lacks the explicitly protected ACL required by its reader. Protecting budget/status/stop-request temporaries before atomic publication fixes the producer; ACL validation is not relaxed. Failed releases and private fixtures remain retained. Later tests prove the corrected behavior.

The first expanded gate on source `08785aed2c57b1dfc02eeeb8c1b33da16e02e9334638c77ca119aede81f3095e` fails at phase 14 after six successful CPU sample calls: Docker's read-only inspect of the exact pinned image times out after twenty seconds. No container was being created by that failed inspect. The engine subsequently responds to pinned preflight in 0.2 seconds without a restart. Preserve `.test-tmp/local-verification-20261116T183857445272Z/result.json` and its probe log.

The native-probe harness now retries only that exact read-only image-inspection timeout, at most three attempts, rechecking reviewed runtime/source/model integrity before another inspect. Wrong commands, non-timeout failures, changed bytes and terminal timeouts propagate. Actual inference is never repeated by this helper. Four focused regressions verify these boundaries; a fresh actual nine-sample CPU conformance run passes. Historical production inference sources/profiles are unchanged.

## Expanded full gate — PASS

The complete **25-phase** gate passes on source `18ad8ad9124eb753dc74389a02d9b57097cfce0044cd326e56c327125b73cc32`, private model bundle `2382b945fbe3532f6253246d969a41d7d51623b2cc737630c64a09f49738b027` and management release `3a2b69d632c7f84a85852f70f13393e0331ae5e16efcb940c291329ff0482945`. It runs 07:53:24–09:04:50 UTC on November 17. All phases exit zero, and the final source identity matches the initial identity.

- **186 Python regressions**, **24 Java unit tests**, **96 integration tests**; zero reported failures/errors/skips in the Java totals.
- All 53 isolated profiles plus three boundary regressions, nine broker recovery/integrity cases, managed operations and fourteen installed-task checks pass.
- The real installed-task Shona HTTP job passes, alongside credential rotation/archival, direct providers and the existing Docker supervisor.
- Actual dump/restore passes for **35 tables and 3,223 rows**, with row/catalog equality, restored tenant isolation and source-snapshot independence.

The reviewed native PostgreSQL 18, Maven 3.9.16/JDK 25.0.4 and explicitly digest-pinned native S3 server are used. No required phase is skipped or substituted. This verifies the implemented local scope, not representative quality or production readiness.

Retained ignored evidence:

| Record | SHA-256 |
|---|---|
| `.test-tmp/local-verification-20261117T075324721458Z/result.json` | `096281825d550c67114588a3f8a433218bab6dc93afe97b95821b9f435467b35` |
| `.test-tmp/foundation-ac9aa7b8-1c33-431d-9a73-596120c9a774/restore-result.json` | `31b38e3ae37070fa1c4f01e44c624227768fc8b6098c3811590057819342cdb9` |
| `.test-tmp/current-setup-393ad43e23e74f099c82a3897ad2b004.json` | `fd67310ac143ab42e11dcde78418e8d165956eb70099782baeae2be5c2e68760` |

## Remaining boundaries

Actual sign-out/sign-in and host boot are unexecuted; configured logon XML and demand-run execution do not prove either. They interrupt this account/session and other workloads and require a concrete maintenance window. Before-login Docker access, target broker network policy and production identity/retention/SLO decisions remain separate acceptance requirements. No machine-wide engine migration, shared workload shutdown or host reboot occurred.

General Shona quality still fails the previous declared screens. [ADR 0048](../adr/0048-larger-qwen-cpu-translation-diagnostic.md) reviews a larger ungated model with complete Q4 weight hashing and native image inspection. After the gate passes, its eight-GiB availability preflight fails before model calls. A separately reviewed lower-memory quantization is downloading for the same frozen comparison; [quality execution record](m4-qwen9b-quality-2026-11-17.md) retains these distinct outcomes. No incomplete weights execute or receive activation. The GitHub billing lock remains an independently observed remote CI startup blocker. M4 remains open.

## OpenRouter connection recheck

On November 17, `python -B tools/openrouter_connection.py check` returns `authenticated: true` and `inferenceEnabled: true`. This authenticated metadata-only request transmits no report text and prints no key. Existing Gemini/Google public-reference routing and its prior quality failures remain unchanged; this check is not a new completion or production/private-data approval.

## Real installed-task core result

In the active expanded gate, `CurrentSetupWorkerIT` passes one test with zero failures, errors or skips (223.9 seconds). A real installed hidden task on the current account claims and completes the protected CPU Shona job through PostgreSQL/HTTP. Assertions verify SUCCEEDED, UNVERIFIED_TRANSLATION, SHADOW selection, exact original UTF-8 source digest, denied other-actor result access and idempotent enqueue; the helper confirms acknowledgement and exact task/resource cleanup. `DockerShonaSupervisorIT` also passes its one actual test (61.54 seconds). The final whole-source totals and restore are recorded above.

## Post-gate cleanup correction — final rerun required

Review finds that startup can leave the monitor unstarted, while unconditional cleanup attempts to join it. Two new regressions both fail with `RuntimeError: cannot join thread before it is started`, masking an exhausted-budget return or the original startup error. Guarding the join by actual thread identity and always closing the owner lock fixes both; all six focused worker tests pass. Exact prior adapter/lock/script bytes are retained in `artifacts/m4-current-account-20261117-v1/` and the old immutable management release. The new separate interactive lock is `a58c07201b5dde7824787884ae0219f44177f744d24247639f248627744c8555`. Historical model/broker/service/management identities are unchanged. The passing 18ad8ad9 source gate above remains historical; the final worker/reference-client changes require a new immutable management release and complete 25-phase rerun after model comparisons finish.

## Final-source rerun and current-account boundary

All 190 Python regressions and the source-audit gate pass after the two cleanup and two reasoning-control regressions are added. The new protected release is `C:\Users\nyash\DzokaManagedRelease-20261117-v2`, receipt `bbf72202112542c2953e30b7d0efc8956883f989c4da1f9d21ed25d4c2d72b8f`. Source `64bb1f5c77d23de455466c699aa1a6a8992e7f2aa2dbdba2fdce291d394781ac` is under a fresh complete gate at `.test-tmp/local-verification-20261117T101507440652Z/result.json`; the subsequent final success/restore is recorded below. Local diagnostic math and all its owned container cleanup finish before this gate starts.

A November 17 read-only token/SCM-access probe confirms elevated=false, administrator=false, integrity=8192, service-creation permission denied with Win32 5. No service/account/engine is changed by that probe. The current-account installed interactive route remains the selected local target; before-login operation and host broker egress acceptance remain separate. The user now defers Shona quality work while continuing the remaining M4 work; existing-provider regressions remain in the full gate.

## Final corrected-source acceptance — PASS

The fresh full gate completes **10:15:07–11:21:15 UTC on November 17** (12:15–13:21 Harare), with all **25 phases exiting zero** and the final source digest equal to the starting `64bb1f5c77d23de455466c699aa1a6a8992e7f2aa2dbdba2fdce291d394781ac`. This verifies the corrected code, rather than substituting the earlier 18ad8ad9 pass.

- **190 Python regressions, 24 Java unit tests, 96 integration tests**; Java totals have zero failures, errors or skips.
- All **53 isolated profiles plus three boundary regressions** pass with 56 calls/56 attempts and no native retry.
- All **14 native installed-task cases** pass against the new `a58c0720` adapter lock and protected `bbf72202` management release. `CurrentSetupWorkerIT` passes its actual core job/acknowledgement/privacy/idempotency/cleanup assertions (192.3 seconds).
- Managed credential replacement/invalid-credential pause/archival, existing supervisors and real providers pass.
- Actual restore matches **35 tables and 3,223 rows**, row/catalog equality, restored tenant isolation and source-snapshot independence.

| Final retained record | SHA-256 |
|---|---|
| `.test-tmp/local-verification-20261117T101507440652Z/result.json` | `5caf0d5dd76938a129cfe9642992d41dc5c694342cdfcf2200ca91e45cd993ce` |
| `.test-tmp/foundation-491a13f6-dc00-4111-936d-999ece9763ec/restore-result.json` | `c2f9325771a367f3d93cc10f51ae839a3b4e45c866260648108c1ec046b8a6ce` |
| `.test-tmp/model-broker-a52c63220dcf4360b3576830727fa755/result.json` | `4ce1a66e1d213b09e1a720580e33785f3c9bd546d505fc5e6129bc6317ee766a` |
| `.test-tmp/current-setup-21c1af1674734e5aa48325da12b29780.json` | `fd67310ac143ab42e11dcde78418e8d165956eb70099782baeae2be5c2e68760` |

The historical source overlay reproduces the entire earlier 18ad8ad9 source digest exactly from final source and five byte-verified overrides; earlier test success and failures remain independently attributable. New/changed source and artifact bytes are also compared to staged Git blobs, with credential-pattern and whitespace checks passing before publication. Caches, model binaries, raw references/results and private configuration remain outside Git.

After the final client change, a fresh metadata-only OpenRouter check returns `authenticated: true` and `inferenceEnabled: true`. It transmits no language/report input. The user defers further Shona quality work; its unapproved diagnostics remain historical, while existing-provider regression checks remain intact. No host reboot/logout, global Docker/WSL change or new account is performed. Actual logon/boot-before-login, unattended daemon/SCM/core-egress policy and representative quality/privacy acceptance remain unverified; current logged-on deployment conformance does not certify them.

## Publication follow-up

The verified implementation/evidence is committed and pushed as `2add54421e90b0e423d940139d8059d61300a867`. The [Core foundation run 44073740201](https://github.com/tdrzzie/dzoka/actions/runs/44073740201) and [Repository governance run 44073740083](https://github.com/tdrzzie/dzoka/actions/runs/44073740083) both complete with failure and **zero executed steps**. Public job annotations for `134006255397` and `134006254986` identify the account billing lock as the startup failure. The governance runner-label migration notice is not that error's cause. The connector supplies runs/jobs; its unsupported annotation endpoint is read through a credential-free public GitHub API request, retaining only the safe failure/notice fields.

This follow-up changes documentation only; the application source remains exactly the fully verified `64bb1f5c` identity. Remote CI remains blocked, not passed. Billing/account changes, unattended deployment/egress acceptance and deferred quality are not silently waived by publishing the local result.
