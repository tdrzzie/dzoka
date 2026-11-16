# M4 language alternatives and native recovery verification

Execution date November 16, 2026. Continue on the user's selected interactive Windows account and existing Docker Desktop. No Docker migration, uninstall, daemon replacement or shared workload cleanup was performed. M4 acceptance remains governed by the [checklist](m4-acceptance-checklist.md); failed diagnostics do not become approved providers.

## Reference-client fixes and actual requests

The initial enabled default was Qwen3 through Nebius. Its protected configuration and API key remain outside Git; the matched comparison below subsequently selects Gemini 3 Flash Preview for public-reference work. The Gemini configurations are explicit public-reference comparisons, without tenant inputs, fallback providers or automatic paid retries. The client records named-language prompt/source identities, safe timeout/network/HTTP failure categories and exclusive attempt receipts. Schema 2 accepts a bounded transport timeout and an explicit null temperature. The latter omits the parameter rather than weakening strict provider routing.

A fresh November 16 `openrouter_connection.py check` returned `authenticated: true` and `inferenceEnabled: true`; no key or account body was printed. This metadata check is separate from paid inference and quality acceptance.

The Gemini 3.8 route originally returned HTTP 404 on all sixteen requests. Its supported-parameter metadata did not include temperature. One explicitly parameter-free request completed. The subsequent fixed configuration omits temperature, uses Google only, a 1,024-token output cap and a 180-second transport timeout. An HTTP-200 error envelope is now recognized as a provider error with safe numeric code metadata; upstream bodies and credentials are excluded from failure receipts. Completed text retains truncation flags and approval NOT_ESTABLISHED.

Licensed FLORES-200 references are attributed to NLLB Team (2022), CC-BY-SA-4.0, as reviewed in the [data record](../dependencies/public-reference-data-review.md). English and Shona member SHA-256 values are respectively `612e9fbe87997617c0fa8fa8929654a4f49b728d96738112c2b86ef6a1d78d88` and `92cb2659b91ef2ad96b88b3816b5837816d29240cbb2ea4197c9ac090f4dd1bb`. Public benchmark training overlap is unknown. Development rows 65–72 were previously seen; additional rows differ between the two models and cannot support a paired cross-model claim.

| Model / slice | Original complete / requested | Original median CER | Complete after separate operator replays | Reconciled median / worst CER |
|---|---:|---:|---:|---:|
| Gemini 3 Flash Preview, development 65–72 | 7/8 | 25.0% | 8/8 | 27.0% / 64.1% |
| Gemini 3 Flash Preview, additional 81–88 | 6/8 | 37.5% | 8/8 | 38.4% / 61.3% |
| Gemini 3.8 Flash, development 65–72 | 7/8 | 32.0% | 8/8 | 25.3% / 50.5% |
| Gemini 3.8 Flash, additional 97–104 | 6/8 | 49.4% | 8/8 | 41.7% / 83.8% |

No completed case was marked truncated. Original unavailability remains 3/16 for each fixed configuration; a later successful request does not erase it. The 3 Flash failures were transport/network errors. The three 3.8 failures were retained under their original client failure categories; the new safe envelope classifier applies to subsequent requests. Replays are distinct requests and may incur separate charges. Neither reconciled model meets the predeclared eight-complete, median CER ≤25%, worst CER ≤60% screen on both slices. CER is a reference-string diagnostic, not a native-speaker semantic judgement or rollout approval.

Private 3 Flash original summary SHA-256 is `509fbad56bb97d89f253bfc1af1c55cc47ad71510651b523117ae780090e3330`. Separate replay hashes for rows 66, 85 and 86 are `5018837d6339da1e8238bd86e0f1e49ec6abe5359be677b2bbf95889f3c2e0c0`, `68502d85a3d61d9204b64bbd7dbe509d28025cb9efe28d94e3a8c3220582b5dc` and `faf49fe317347b7c0560ecf6a68009f06badea840a91413d0a41abbec462e65f`.

Private 3.8 original summary SHA-256 is `9b87487ccd4877724f28b7f5fa41fc1e8b76951133e61011719a0251d613f729`. November 16 replay hashes for rows 70, 97 and 100 are `fab3147fcd6bc5a888691f063b9e9b56107104a61532fac938ba812c3220696b`, `4c3a9a76d1dc311ff2ea97c148121a3d2c3b3153dd6f1bb5811f978fa924f02c` and `9d7c9ed96da963b087f2127e88f0d93a731797a89cd8539585dd14d748ce1dec`.

## Matched hosted-route comparison and selection

To check the corrected named-language prompt on the cheap enabled route, freeze exactly the same twenty source/reference cases as the MADLAD plan before further requests. Qwen/Nebius uses the original 512-token configuration; Gemini 3 Flash Preview/Google uses its previously tested 1,024-token configuration and 180-second timeout. Model and budget are jointly compared configurations, not a controlled budget-only or prompt-only experiment. All forty new calls completed without truncation or replay. These calls do not erase earlier Gemini unavailability or establish an availability SLO.

| Slice | Complete pairs | Qwen / Gemini median CER | Qwen / Gemini worst CER | Lower / higher Gemini error |
|---|---:|---:|---:|---:|
| Shona→English development 65–72 | 8 | 48.7% / 24.0% | 82.0% / 65.0% | 7 / 1 |
| Shona→English additional 145–152 | 8 | 60.8% / 46.0% | 90.5% / 61.9% | 7 / 1 |
| English→Shona additional probes 145–148 | 4 | 57.8% / 34.3% | 63.6% / 50.7% | 4 / 0 |

Gemini's observed reference error is lower on eighteen of twenty complete pairs, with no ties. Both eight-case configurations still fail the screen: Gemini exceeds the development worst-case ceiling and both additional ceilings. The four English→Shona probes cannot satisfy the eight-case coverage requirement. Development cases were previously inspected, additional cases have unknown training overlap, and hosted aliases do not attest immutable server builds. No native-speaker semantic or representative lost-property acceptance follows.

Private Qwen plan/result hashes are `ed7b501ce63b931942995dd80133ca56efb8b43453d2a6de3246c5e76b8c5dc5` / `08964e714038a65745ce4ad5b1c9d140ca732f473fe80007784fe9813ae379ec`. Gemini plan/result hashes are `b2e125baca896b68eeed2b2dd3ce2704faee60aa291dd5f9b16b211ae9575ae7` / `57493e6cfb2b25c3960cf9bfa213dd8da692d95a143de1f75fc4a3b3424cc984`. The scoring check independently requires matching direction, row, slice and exact input/reference hashes across all four configurations.

Under the user's existing authority to select/configure OpenRouter, selected Gemini 3 Flash Preview through Google for **public-reference work only**. Preserved the exact Qwen settings as a new private snapshot before atomically replacing `settings.json`; the credential file is untouched. The prior/new configuration-file byte SHA-256 values are `f96ca80eccc07786547e7114205af2cb56c05130f139a9302d2cced00723ce2b` / `a1880ff6fc7b0f9e039404f7c6d4e3bdd14c1cc57382bb793f8d627d0cd343ce`. These are file digests, distinct from the canonical configuration digest in inference receipts. The retained private dated decision has SHA-256 `d1da827d0b4ddf58bded7a6b3f2a856d1b6a0e26871fb08e90781dae366cec16`. Post-selection authentication again returned authenticated/enabled true. No local model profile, core job route, fingerprint activation or tenant-data authorization changes. This is a comparative reference-route choice; quality approval remains NOT_ESTABLISHED.

Google's standard endpoint currently lists $0.50/M input and $3/M output tokens, with different flex/priority prices. Provider-name allowlisting does not select a specific tier. No total charge or cost guarantee is inferred from token counts. [Official endpoint metadata, checked November 16](https://openrouter.ai/api/v1/models/google/gemini-3-flash-preview/endpoints)

## Licensed MADLAD alternative

The complete INT8 export and tokenizer were hash-verified before execution. The [model review](../dependencies/madlad-cpu-review.md) distinguishes the publisher's Apache-2.0 weight license from the export/conversion claims. The first OpenBLAS image failed before translation; its private failed result remains retained. Inspection showed that the chosen compute mode needs an INT8 backend. [ADR 0044](../adr/0044-madlad-int8-cpu-diagnostic.md) separately reviews the exact Ruy/cpuinfo/clog revisions and licenses before execution. No upstream training/converter Python code or upstream tests were executed.

The working offline-built image is `sha256:c42a79d484cdd70f385149ae5c5a7fec64e2b3fb9f6a1f4c5e0dcbf7b54af6f0`. It requires effective CPU INT8_FLOAT32, with 256 input and 128 output tokens. Driver SHA-256 is `163496fa555ef098ef133e0a1af0659606f52b9f4ee7b9756d12404568dec85c`; Dockerfile SHA-256 is `81bdae9a8880b0ca28ac4eb9c73737e65b690f2a1c4ef29051535a691eeaf622`. Runtime containers have no network, a read-only root, dropped capabilities, no new privileges, a 64-process ceiling, two CPUs and six GiB memory. Each mounts only three verified model files and is removed after the call. A snapshot observed about 3.2 GiB memory; that observation is not a capacity or throughput benchmark.

The frozen plan SHA-256 is `aa6d3d1951f3090cb9967dac75f67d1f05d0e4d1b822cc6e0dfb981ec29b591e`. It selects Shona→English development rows 65–72, new additional rows 145–152, and four English→Shona probes 145–148. Source/reference hashes, actual text evidence hashes, availability and truncation remain private. The comparison uses the same frozen twenty cases and existing isolated CPU profiles; a truncated baseline output is not counted as a complete translation.

All twenty MADLAD calls completed without input/output truncation or unavailable calls. All twenty baseline calls returned evidence, with nine truncated. Both diagnostics completed their owned-resource cleanup. Result SHA-256 values are `8452d468efccdfc3381ca16353169d10a588c90e0e4f64239fb613f1383ef39e` (MADLAD) and `e121c857c8e6ad75d84729f140a41ddd9db20d79124ca198dff5c169e277f4fe` (baseline). Baseline plan SHA-256 is `e772bf0a45340c53073768395053a6d1b9bf9c7637976f4c168ade5621595139`.

| Slice | Baseline complete / truncated | Baseline complete-only median / worst CER | MADLAD complete / truncated | MADLAD median / worst CER |
|---|---:|---:|---:|---:|
| Shona→English development | 4 / 4 | 44.4% / 61.0% | 8 / 0 | 35.6% / 63.2% |
| Shona→English additional | 4 / 4 | 49.1% / 57.0% | 8 / 0 | 46.4% / 90.5% |
| English→Shona additional probes | 3 / 1 | 62.5% / 64.2% | 4 / 0 | 86.6% / 146.6% |

Complete-only medians have different denominators and are not a paired improvement estimate. The common plan preserves exact pair identities for further review. CER can exceed 100% because insertions add errors. Four English→Shona probes are smaller than the eight-case diagnostic screen and supply no acceptance claim. The predeclared MADLAD screen requires all available, median CER ≤25% and worst CER ≤60%; both eight-case Shona→English slices fail. The candidate solves bounded native INT8 compatibility and the observed truncation, but its quality does not justify provider adoption. Do not register or activate it as a quality fix.

On cases where **both** models completed, the matched comparison is:

| Slice | Complete pairs | Baseline / MADLAD median CER | Lower / equal / higher MADLAD error |
|---|---:|---:|---:|
| Shona→English development | 4 | 44.4% / 35.6% | 3 / 0 / 1 |
| Shona→English additional | 4 | 49.1% / 53.7% | 1 / 1 / 2 |
| English→Shona additional probes | 3 | 62.5% / 100.8% | 1 / 0 / 2 |

Thus the development improvement does not generalize to these additional complete pairs. The nine baseline-truncated pairs remain in the coverage record rather than being used as complete-quality wins. This result supports rejecting broad adoption of this particular model/export/decoding configuration; it does not prove all MADLAD configurations or larger models fail.

## Gate failures and recovery scope

The earlier gate was explicitly interrupted for a source correction. Its termination left a temporary model-package read grant, which the normal exact journal recovery removed. The following gate rejected that grant; the check was preserved. Two subsequent runs stopped at the existing generic isolated-protocol-unavailable exception on different profiles. Twelve focused alternating calls of those profiles subsequently passed. This does not establish the intermittent fault's root cause.

The verifier now allows at most three attempts only for that exact legacy protocol-unavailable exception. Before each retry it reconciles the owned state and performs full bundle integrity/ACL verification. Unconfirmed cleanup, integrity changes, unrelated parent-visible validation errors and exhausted attempts fail the gate. That legacy category hides the underlying conversation failure and can include malformed wire output or native callback errors, not just EOF. Both normal profile and Jina boundary results retain attempt counts. The production worker's existing durable job retry path is unchanged. A passing result with retries establishes bounded recovery, not first-call availability, an innocuous failure cause or a repaired underlying pipe implementation.

The interrupted and failed November 15 receipts remain retained at the following paths beneath `.test-tmp/`: `local-verification-20261115T093334999301Z`, `local-verification-20261115T094830570574Z`, `local-verification-20261115T095458728123Z` and `local-verification-20261115T103852439435Z`. Their result hashes are respectively `c691966c82fdf4f703e8d34c0040eb331971758fe766b786196042457e1336dd`, `23243d08fb5f35c70a5c00a49d0055d67e84c2b2767dd967c531d0c7004d73b6`, `b1b7451c48b5a10cdf124e9a3bdbd40804bb6753b2fa7d056b07043107470cbb` and `5c23a9f85080841725758729dc536c97ee9f7b17eecf8406a48f54eaadde51f8`.

The first November 16 gate, `local-verification-20261116T130912293896Z`, failed on ISO_TEXT after all three protocol attempts. Its result SHA-256 is `61d4cdefdfc228ff162d1cf2c1cc70e2f9d6176f8655b6c8e6d1725c8a1df3cb`. That run overlapped the final diagnostic comparisons; concurrency is a limitation, not an established root cause. After both comparisons completed, three instrumented text calls passed, followed by the standard uninstrumented focused verifier. A new full run executes serially. The failed original run remains failed.

## Completed full verification

The serial run `local-verification-20261116T133038750512Z` passed all **24 phases**, finishing at `2026-11-16T14:50:41.258148+00:00`. Its unchanged application/tool/test source digest is `1f52b0682dee3899cba9fa0c185e0861cd03f5f1ae2cd4c50a8dd0bcd1403b57`; result SHA-256 is `f5baf22d3ad9088c288756964ad5ef865a950eeeb10ec4422da203560c4be3ae`. All **178 Python tests, 24 Java unit tests and 95 integration tests** pass, with zero errors/failures/skips. Fresh XML timestamps and aggregate counts were checked against the run's start, rather than reusing older reports.

All **53 isolated profiles and three Jina boundary regressions** passed on their first attempt in this serial run: 56 calls, 56 attempts, no native retry. The native result SHA-256 is `93b25c2b8b8c17126b5f6f19e515c6007a21a885ac7c19db44725508cb55c7cd`. Nine broker recovery/integrity cases, managed operations and private quality protocols also pass. The real core integration verifies protected broker job completion, injected post-claim Shona retry, wrong-tenant denial, immutable versioned evidence and shadow/activation behavior. Earlier failures remain retained; this success supplies no root-cause diagnosis or first-call availability SLO.

The pinned model bundle receipt remains `2382b945fbe3532f6253246d969a41d7d51623b2cc737630c64a09f49738b027`; the new code-only management release receipt is `f69793beb0efe5924e4533d235c09374a41e2123d30f7c6f3ddb67c51ea5e4fd`. Existing model/profile identities are unchanged. Explicit hash-verified native SeaweedFS 4.51, native PostgreSQL and real Docker decoder/CPU inference were exercised. No required phase/provider/restore test was skipped.

Logical restore passed exact row/catalog equality for **35 tables and 3,189 synthetic rows**, restored runtime-role/two-tenant isolation and source-snapshot independence. Restore result SHA-256 is `db3abbd89a22926da1c0502671df98cf875e0216b32483a586e51a7e7c690ce7`. This is a same-cluster logical restore with pre-existing roles, not object/key recovery, tombstone replay, PITR, HA or an achieved production RPO/RTO. The task-owned PostgreSQL cluster stopped after verification.

M4 remains open for the recorded quality and unattended target-deployment acceptance. The current interactive setup works and was retained. Native translation quality/model improvement and unexplained intermittent protocol failures remain local engineering debt; qualified semantic/representative approval and target-host identity/network/boot evidence additionally need their actual resources. A passing full conformance gate does not approve these quality candidates or complete the production platform.
