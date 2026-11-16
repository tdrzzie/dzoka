# M4 African CPU translation diagnostics — 2026-11-16

## Verified integration and restore baseline

The full serial application gate completed all **24 phases**, with **178 Python tests, 24 Java unit tests and 95 integration tests**, zero failures/errors/skips. All 53 isolated model profiles and three boundary regressions completed, with 56 calls/56 attempts. Nine broker recovery/integrity checks pass, including protected HTTP Shona completion after an injected post-claim failure and reclaim. Exact PostgreSQL restore matches **35 tables and 3,189 rows**, catalog/runtime-role/two-tenant/snapshot-independence checks. Receipt/source identities and earlier failed attempts remain in the [integration record](m4-language-alternatives-2026-11-16.md). No phase was replaced by these standalone diagnostics.

Application/tool/test source digest remains `1f52b0682dee3899cba9fa0c185e0861cd03f5f1ae2cd4c50a8dd0bcd1403b57`. Diagnostic source/build archives below are evidence artifacts, not newly registered application profiles. No core inference source, provider identity, migration, private model bundle or management release changes in this diagnostic work. The earlier complete gate is applicable to those unchanged application bytes; it does not prove these candidates' HTTP/broker integration.

## TranslatePsy: real CPU execution and matched comparison

[ADR 0045](../adr/0045-small-african-translation-cpu-diagnostic.md), [license/runtime review](../dependencies/afrislm-cpu-review.md) and retained notices precede build/execution. Both complete GGUF files match their full publisher LFS SHA-256. The separately pinned llama.cpp CPU library is built offline with independently authored fixed drivers; no model-supplied Python, converter, dependency installer, server, tools or upstream test suite is run. Actual image/compiler/link/package inspections verify CPU flags, binary hashes, no missing dynamic libraries and the reviewed package versions. First-party executed driver/Dockerfile bytes and image/closure identities are retained in [artifact manifest](artifacts/m4-language-20261116/manifest.json).

The 0.8B Q8 and 4B Q4 containers run as non-root with no network, read-only root/model, dropped capabilities, no new privileges, disabled core dumps/log driver, 64-process limit, two CPUs and respectively three/six GiB memory ceilings. The fixed driver uses 2,048 context, at most 1,536 prompt tokens and 256 output tokens, greedy sampling, CPU only. Private owned create/cleanup journals are persisted before operations, image/labels/configuration are read back, positive container listings distinguish absence from lookup failure, and confirmed removal precedes accepting each result. Exact verified model handles stay read-locked throughout inference. These are diagnostic containment checks; production broker integration is not claimed.

Freeze 32 exact source/reference pairs before execution: FLORES-200 development SN→EN rows 65–72; additional SN→EN and EN→SN rows 193–200; eight separately authored mixed-language regression sentences. FLORES file/license provenance is retained in [public reference review](../dependencies/public-reference-data-review.md). Unknown training overlap and prior exposure remain explicit. Compare the same bytes through the existing isolated CPU baseline (AUTO for the authored code-switch cases). Full baseline bundle verification and profile/Docker reconciliation pass before/after and between cases.

The predeclared screen requires all eight complete per direction/slice, median CER ≤25%, worst-case CER ≤60%, and every predeclared authored literal fragment to match. CER measures reference-string error, not semantic correctness; do not interpret different paraphrases as necessarily wrong or a low score as faithful. Downloaded corpus references and generated outputs stay in ignored caches/protected stores. The archived first-party evaluator contains the eight independently authored synthetic pairs; these are not real reports or private ownership answers.

| Slice | Baseline complete / 8 | Baseline median / worst CER on complete | 0.8B complete / 8 | 0.8B median / worst | 4B complete / 8 | 4B median / worst |
|---|---:|---:|---:|---:|---:|---:|
| Development SN→EN | 4 | 44.4% / 61.0% | 8 | 29.7% / 59.4% | 8 | 27.9% / 53.0% |
| Additional SN→EN | 4 | 52.4% / 68.2% | 8 | 49.6% / 66.7% | 8 | 47.3% / 59.1% |
| Additional EN→SN | 4 | 58.5% / 77.3% | 8 | 52.5% / 74.9% | 8 | 49.2% / 77.2% |
| Authored code-switch | 8 | 11.9% / 52.0% | 8 | 6.7% / 14.0% | 8 | 7.6% / 32.6% |

The baseline has 20 complete and 12 truncated calls; both candidates have **32/32 complete**, with no unavailability/truncation. The baseline medians above use fewer complete rows and therefore are not full matched-population improvement estimates. Across the **20 common complete pairs**, 0.8B has lower CER on 15, higher on five; 4B has lower on 16, equal on one, higher on three. Preserve truncation separately rather than assigning it zero error.

Authored literal-fragment coverage is baseline 17/20 and both candidates 18/20. The two missed candidate fragments are exact `not` checks where contractions occur; do not relabel those literal checks as passed or infer a semantic negation error solely from their failure. The unchanged strict screen still fails. Independently inspected development output also changes the meaning of a congressional/FBI passage; this is not merely a reference spelling issue. No unrestricted Shona quality approval or activation is justified. The small authored regression improvement is useful, but does not establish representative lost-property accuracy.

## Exact private receipt identities

| Run | Plan SHA-256 | Result SHA-256 |
|---|---|---|
| `DzokaAfriSlmBaseline-20261116-v1` | `af3701f018d424fc3689a2efd3fdbff8ef110070113eeafec793646cdd226e94` | `319c856b1e9fa97d043eb7efd1dc936f85f75fb798838dc6a398179bcfdb1abb` |
| `DzokaAfriSlmDiagnostic-20261116-v1` | `a9491ca648c772cbf8c8727fab6e36e61827053e185002c48c77e816e242c276` | `32adff97476b2a76b93e2a721fc68f63429afb5a3d2bc6e2cbc8eb91c88ae4a3` |
| `DzokaAfriSlm4BDiagnostic-20261116-v1` | `81d1da323f4147d485c2d4c1ca47f5f7fefccdc0bd0ccdc037d38e261f29a4f9` | `3ed0f9ab218fe5f5cdb90c6455a5455818614cfc8b1313e3b98115b539b1dce5` |

An independent aggregation verifies all result→plan hashes, exact selected input/reference hashes, each available evidence-file hash and exact matching populations before deriving the numbers. Original per-case counts, token scopes, EOS/truncation, elapsed times and raw-output digests remain in protected per-user stores.

All four built native drivers also pass **64 malformed-frame checks**, sixteen each, in constrained containers with no model mounted. Empty/partial frames, invalid direction/length/EOF, NUL/control bytes, invalid/overlong/surrogate UTF-8 and role/thinking delimiters are rejected at stage 1 with no output, before model loading. Four valid-frame positive controls reach the model loader and then fail closed at stage 2 because no model is mounted. The three-shot driver receives its valid hash-locked context mount in this strengthened run, so missing context cannot explain rejection. Each owned test container is confirmed removed. The [exact executed test and result](artifacts/m4-language-20261116/framing-result.json) and earlier no-context test are retained with the driver/evaluator source archive. These are additional native boundary checks, not additions to the full gate's Python count and not model quality or application integration tests.

## Retained failures and limitations

The first staging proposal referred to three GCC runtime archives actually inherited from the pinned base. It failed before building; the corrected runtime stage installs only the reviewed libc archives and records inherited packages explicitly. The next attempt built the CPU library but the driver used obsolete loading fields; compiler rejection is retained. The corrected driver uses the actual pinned loading/lazy-mode API. Their distinct failed closure/driver hashes and the exact correction are retained in the adoption review; neither failed attempt is reported as successful inference.

These candidates do not change the intermittent generic isolated-text protocol startup debt recorded with the full gate. No root cause is invented from a later passing retry. Nor do they establish native-speaker semantic review, target-service/boot/egress acceptance, approved institutional datasets/retention policy, load/SLO results or production readiness.

Registry integrity and the mandatory source gate pass. A later restricted-command rerun of the twenty registry tests failed while creating/removing temporary fixtures with Win32 access-denied errors (twenty cases, forty setup/cleanup errors, no exercised assertions). The same unchanged tests rerun in the authorized local test environment pass 20/20; the full application's earlier source-bound gate remains separate. No source fix or successful assertion execution is claimed for the restricted attempt.

Remote execution is independently rechecked for published application checkpoint `724fb31431fe472de4e3c5afe7d3195eaac21f0c`: [governance run](https://github.com/tdrzzie/dzoka/actions/runs/43891369648) and [foundation run](https://github.com/tdrzzie/dzoka/actions/runs/43891369761) both fail before any job steps. Public check annotations explicitly identify an account billing lock. The GitHub connector reads runs/jobs but rejects the annotations endpoint; a credential-free public API read verifies the exact cause. No remote assertions ran and no workflow/code fix or automatic rerun can resolve the account lock. Local verification remains available. The unrelated `ubuntu-latest` migration notice is not identified as the failure cause.

## MORENA follow-up — both configurations executed

[ADR 0046](../adr/0046-morena-shona-cpu-diagnostic.md) and [model/runtime review](../dependencies/morena-cpu-review.md) precede the separate Apache-2.0 MORENA Q8 diagnostic and attributed CC-BY-4.0 professional SmolSent references. The complete 1,580,378,784-byte weight file matches `2b7905b1f0d70efeaa7f95ae1829ffb640dcfce748f9e20e45185359b4933203`. Actual native image/CPU-library/compiler/link/package inspection passes. The four-GiB/two-CPU containers retain the same non-root/no-network/read-only/capability/process/deadline/output constraints. The fixed first-party reserved-token prompt differs from Qwen's; no upstream Python/template/converter or tool call executes.

Freeze the original 32 exact cases plus SmolSent rows 1–8 per direction before inference. The two SmolSent files are reverse pairs: sixteen directional calls do not mean sixteen independent bilingual pairs. For the separate three-shot version, freeze rows 257–259 per direction as context, exclude them and exact content duplicates from every test input/reference, and retain their attribution, complete hashes and binary-framing transformation. The parent hash-locks the model and context throughout inference and verifies exactly the two constant read-only mounts. Neither context rows nor outputs become their own test reference.

Both configurations complete **48/48**, without unavailable or truncated calls, with every owned container confirmed removed. Zero-shot outputs repeat instructions on inspected cases. The separate three-shot prompt improves CER on **45/48 exact matched pairs**, worsens three and ties none. This compares jointly different prompt/context configurations; it does not reproduce the publisher benchmark or establish semantic quality.

| Slice, eight complete each | Zero-shot median / worst CER | Three-shot median / worst CER |
|---|---:|---:|
| Development SN→EN | 78.4% / 120.4% | 35.2% / 85.0% |
| Additional SN→EN | 86.8% / 110.4% | 52.8% / 82.3% |
| Additional EN→SN | 68.2% / 106.0% | 56.4% / 73.8% |
| Authored code-switch | 121.3% / 158.0% | 4.5% / 36.0% |
| SmolSent EN→SN | 117.1% / 146.8% | 53.1% / 83.1% |
| SmolSent SN→EN | 111.6% / 147.0% | 58.7% / 81.9% |

CER can exceed 100% when generated insertions exceed reference length. Three-shot reduces the authored code-switch median against baseline 11.9% and TranslatePsy 0.8B 6.7%, while its worst case is worse than 0.8B's 14.0%. Literal coverage is 19/20 for each MORENA configuration; the three-shot missed `not` fragment is a `don’t` contraction, not independently established negation loss. Preserve the failed literal guard. The synthetic `Ndarasa` example also leaves dropped/lost/discarded interpretation unresolved; these eight authored references cannot establish native-speaker, representative recovery quality.

Every general translation slice still fails the unchanged screen. An inspected three-shot development output removes the instruction prefix but changes investigative actors/roles relative to the licensed reference. This is not solved by post-hoc prefix stripping, relaxing CER thresholds, relabeling synthetic cases as representative or promoting a model after a passing runtime test. **Do not adopt these diagnostics as unrestricted production translation profiles.** No new application provider, model-worker broker family, fingerprint activation or production Shona approval is claimed.

| Private run | Plan SHA-256 | Result SHA-256 |
|---|---|---|
| `DzokaMorenaDiagnostic-20261116-v1` | `637ce6f68c5f403686edc14956fcd59b6c3ebb827fcd683a794395cd081aa318` | `1b3c41a080b3959fcd00fe7f8a9212904ef7d00a000a0149cc65e94dd3936f1b` |
| `DzokaMorenaFewshotDiagnostic-20261116-v2` | `e2ba9f08e9a9fe5ca872b811b83a50968e1c8af4257d7bd09fca46f852ec19f5` | `619e5e5f7812f6acfcb200887e4b5a75485b1d747f3a807c96a420db75fca505` |

Final aggregation verifies all 96 output/plan hashes, every matching source/reference identity, all 96 terminal cleanup journals, the exact original 32-case population and the unchanged private context asset/manifest hashes. Heavy inference runs serially; the queued three-shot run starts only after the prior exact 48-row receipt and confirmed cleanup. The OpenRouter key authenticates again and reference inference is enabled; these local comparisons send no references to a hosted inference service.

## Remaining acceptance

The requested integration and restore checks are complete and passing on the recorded application bytes. Local model work improves truncation and authored code-switch diagnostics, but general Shona meaning/error requirements remain unfinished engineering, not merely an external-infrastructure excuse. Further model/domain-adaptation choices need separately reviewed versions and meaningful comparisons. Qualified bilingual recovery references/review, accountable target deployment/egress/boot acceptance and resolution of the GitHub account billing lock remain acceptance prerequisites. M4 is **OPEN**; no status edit turns the failed quality results into completion.
