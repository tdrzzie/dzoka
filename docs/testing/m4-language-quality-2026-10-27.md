# M4 language quality and Shona verification

This scoped extension uses Windows amd64 CPython 3.14.7 CPU, the unchanged reviewed separate 22-wheel runtime, Oracle JDK 25.0.4 emitting Java 21 bytecode, native PostgreSQL 18.6 and digest-pinned Docker S3/decoder fixtures. No new runtime wheel or Maven artifact is introduced. Full M4, representative quality/native-speaker acceptance and production readiness remain open.

## Final complete run

`python tools/verify_local.py --pg-bin 'C:\Program Files\PostgreSQL\18\bin' --maven 'D:\Projects\ai\dzoka\.audit-cache\apache-maven-3.9.16\bin\mvn.cmd'` passed all **thirteen** required phases on the final role-bound profiles.

| Evidence | Actual result |
|---|---|
| Mandatory source gate | PASS; all thirty registered bounded reviews remain complete |
| Architecture/preservation | PASS; 53 checked baseline documents, thirteen unchanged supplied bodies |
| Python | 66 tests, zero failures/errors |
| Java | 16 unit / 77 integration tests, zero failures/errors/skips |
| Dependencies/models | Unchanged 106 Maven records and 21-wheel baseline plus reviewed separate 22-wheel environment; earlier providers and all twenty-one new artifacts/source identities pass |
| Native inference | Fifteen controlled comparison rows; four new authenticated worker jobs plus every earlier provider and explicit snapshot activation |
| Logical restore | Exact row/catalog equality across 35 tables / 2,246 synthetic rows, with runtime-role/two-tenant/source-snapshot independence checks |

Started `2026-10-26T22:25:13.779260+00:00`, finished `2026-10-26T22:29:54.598176+00:00` (UTC; local verification date 2026-10-27). Unchanged code/lock input SHA-256 `fdddba2434b5d47574d008e0d174f46e17981abcfb7f3051bfb6c7711cd6c756` before/after the entire run. Ignored evidence `.test-tmp/local-verification-20261026T222513779038Z/result.json`; restore `.test-tmp/foundation-7be7f077-10d9-4a7f-ba7a-b858af12f2eb/restore-result.json`. Task-owned PostgreSQL/S3/decoder fixtures stopped/removed; completed range/partial downloads removed while verified model files remain cached. No mock substitution or omitted provider is presented as full-provider verification. Native Linux/model platform, Docker PostgreSQL rerun and remote CI remain unverified for this extension.

## Adoption and scope

[ADR 0033](../adr/0033-language-quality-and-shona.md) and [adoption evidence](../dependencies/language-quality-adoption.md) record separately inspected explicit Apache-2.0 export/base grants, seven primary card hashes, immutable revisions/configurations and twenty-one selected artifacts (733,248,475 bytes). Independently authored inference uses the official SmolLM2-360M-Instruct quantized graph plus dedicated OPUS-MT Shona/Spanish/German-to-English int8 encoders/decoders. Existing model/runtime locks, providers and profile keys remain unchanged. No publisher conversion code/Jinja/custom Python or upstream test is executed. Actual local fixture execution is separately identified below.

The source-audit gate passed before implementation; the thirty mandatory audit scopes and thirteen supplied research bodies remain unchanged. This separate artifact/dependency review is bounded, not a complete model-training-data permission, reproducible binary build, security/advisory or production distribution certification. Existing SentencePiece bundled notices still apply.

Shona support is one-way `sn→en`, with regional source hints accepted and typed UNVERIFIED_TRANSLATION results. No English→Shona alias, pivot, identity fallback or language detection is added. English→Shona candidates lacked an adopted ONNX export or need separately reviewed conversion/runtime work; this remains local engineering, not an external-infrastructure blocker. Shona attribute extraction/OCR/speech coverage is not established. Original report text/language/uncertainty remain unchanged.

## Controlled diagnostic method

Nine SHA-locked authored synthetic cases in `ai/quality-evaluation.json` cover Spanish/German bag and umbrella sentences, three Shona bag/place/code-switch examples and two English draft instructions. Six cases additionally execute the earlier 135M/M2M100 baselines; no M2M100 Shona baseline is invented. The fifteen expected per-case rows include candidate/baseline profile keys, exact outputs, finish/truncation/token/unknown metadata, exact English reference comparisons where authored, repeated whitespace-token trigram counts and fixture/runtime hashes. No reference or repetition metric is an accuracy/safety/confidence result. Shona examples and English references require native-speaker validation. These controlled fixtures must not be presented as held-out representative benchmarks or proof of broad improvement.

`python tools/quality_runtime.py evaluate` writes private ignored `.test-tmp/quality-evaluation-result.json`. Artifacts and evaluation/source/runtime identities must pass before inference. `--profile NAME` can narrow a reproducible diagnostic. Reports preserve negative findings and outputs reaching token limits. Models are disabled until deliberately registered/enabled; no default silently changes.

## Verification boundaries

The independent development portable run passed 63 PostgreSQL integration tests and 16 Java unit tests; 63 Python tests passed separately. New portable cases exercise exact private Shona input/current-source leases, regional hints/wrong directions, tenant/worker/result isolation, replay, manifest-bound representation/trust/unknown-count validation, explicit translation activation and larger-draft exclusion. Existing suites cover shared quotas/concurrency/retry/erasure/outbox/holds; no new persistence/queue mechanism is introduced. Seven new Python tests cover exact distinct registries, corrupt artifact rejection before native imports, source/base-runtime mutation and actual evaluation-loader tampering, sign-aware repetition control/no-repeat bigrams and source/target tokenizer-role swaps.

The complete gate also requires every earlier real provider and four new authenticated service-worker jobs: larger English draft plus Shona/Spanish/German→English, typed untrusted results, unchanged Shona raw source/language, three explicit translation pins/sealing/activation and draft SHADOW isolation. These verify actual inference and lifecycle, not the semantic correctness of every output. Final complete-run counts/digest/restore and observed quality are recorded after execution below.

Permitted representative multilingual/recovery/query data, native-speaker review, institutional policy and production hosts/identity services are external prerequisites. Better language/LLM models or decoding, English→Shona conversion/runtime adoption, evaluation tooling and host resource/filesystem/egress enforcement remain locally buildable work. Remote CI and Linux native-model adoption remain unverified; prior remote account-startup failures remain historical evidence. Logical restore does not establish object backup, independent tombstone replay, HA/PITR or approved RPO/RTO.

## Observed final comparisons

Final evaluation produced fifteen rows at `2026-10-26T22:25:53.120427+00:00`, fixture SHA-256 `bc13e0ada741ac23534f0368017985dd2e45aef608ff4e982a7ce969b48dc122`; ignored complete report `.test-tmp/quality-evaluation-result.json`, console `.test-tmp/quality-final-evaluation.log`. All named profiles executed real CPU inference.

| Case | Prior profile output | New profile output |
|---|---|---|
| es-bag | I lost a black pocket. | I lost a black bag. |
| es-umbrella | I found a red paraguay. | I found a red umbrella. |
| de-bag | I lost a black pocket. | I lost a black bag. |
| de-umbrella | I found a red rainwave. | I found a red umbrella. |
| sn-bag | No prior Shona profile | I lost my bag. |
| sn-place | No prior Shona profile | I left a black bag in Joina City. |
| sn-code-switch | No prior Shona profile | I left lack Lenovo bag in Joina aund 2pm. |
| llm-backpack | Repetitive explanation; 64 tokens / OUTPUT_LIMIT, eighteen repeated trigrams | A backpack is a bag that you carry your belongings in. |
| llm-bag | I lost a black bag. | I lost a black bag. |

All four Spanish/German authored English references match the new dedicated outputs; none match the older M2M100 outputs on these same four sentences. This is a controlled diagnostic result on four selected examples, not an accuracy percentage, representative evaluation or justification for enabling all language directions. Two simple Shona examples match authored English references; the code-switched example corrupts “black” and “around” to “lack” and “aund”. The qualifier failure is preserved; the source report/temporal uncertainty is never overwritten or canonically inferred from that translation. All translation examples had zero unknown input tokens and EOS, illustrating that neither guarantees meaning.

The larger LLM initially ran with ordinary greedy decoding and produced a circular thirty-token answer listing “a backpack” repeatedly, despite zero repeated whitespace trigrams. That negative probe is retained under `.test-tmp/quality-llm-evaluation.json`. The new explicitly versioned policy uses a 1.1 sign-aware repetition penalty on generated token history and bans repeated generated-token bigrams. Its backpack answer ends at thirteen tokens/EOS; the bag draft remains “I lost a black bag.” Both models still return UNTRUSTED_DRAFT. Zero repeated trigrams is insufficient as a semantic-quality metric; the first failed larger-model probe demonstrates this directly. No quality/safety/calibration claim follows from these two instructions. The earlier greedy 360M probe is development evidence, not the final registered profile.

Supplementary real-model long-input probes reached 512 LLM input / 64 output tokens and 128 Shona input / 32 output tokens with explicit input/output truncation and OUTPUT_LIMIT; instruction/context truncation remain separate for LLM. Diagnostic `.test-tmp/quality-bound-probe.log`. These development probes are additional evidence, not extra automated test counts. The final complete gate below governs the final source/profile bytes.

Slow approved transfers were completed using bounded HTTP ranges, preserving partial downloaded bytes and independently verifying each complete artifact against its exact expected SHA-256 before loading. A stopped sequential task-owned downloader is not a passing fetch run; the final complete artifact checks and native execution are the evidence. No model graph or raw diagnostic is committed.

Final review bound artifact roles directly into the four new profile manifests and added a source/target tokenizer-swap regression. An earlier complete thirteen-phase development run passed before that strengthening; the full gate and fifteen-row comparison were repeated against the final role-bound identities. The governing final run is recorded below. Earlier provider/profile identities remain unchanged.
