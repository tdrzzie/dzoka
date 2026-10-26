# Language quality alternatives and Shona

[ADR 0033](../adr/0033-language-quality-and-shona.md) and [artifact adoption](../dependencies/language-quality-adoption.md) append four profiles to the existing private-job system. These are explicitly selected alternatives; no default or historical profile/output is replaced.

## Bootstrap and run

Provision the separately reviewed [22-wheel environment](generative.md) first. No new Python/Maven dependency is introduced. Run `python tools/quality_runtime.py fetch`, then `check`. Artifact roles are bound directly into each profile manifest; swapping source/target tokenizers changes its identity. Twenty-one exact immutable artifacts are required in ignored `.audit-cache/quality-models`; no worker downloads a model. Verify the installed environment using `python tools/generative_runtime.py runtime-check`. Runtime/platform/notices/source/artifact identities fail closed before native inference; approved graph paths remain held against write/delete while loaded.

`python tools/quality_profile.py --tenant UUID --profile UUID --profile-name OPUS_SN_EN` prints disabled SQL only. Review and enable a profile deliberately. Available names:

| Profile | Task / direction | Limits |
|---|---|---|
| SMOL360_EN | Private English LLM draft, official SmolLM2-360M-Instruct | 512 input tokens, instruction cap 160, 64 generated tokens |
| OPUS_SN_EN | Shona→English, dedicated Marian OPUS-MT | 128 input tokens, 32 generated tokens |
| OPUS_ES_EN | Spanish→English, dedicated Marian OPUS-MT | Same |
| OPUS_DE_EN | German→English, dedicated Marian OPUS-MT | Same |

Use the existing `/api/v1/tenants/{tenant}/reports/{report}/perception/generate` endpoint for LLM JSON `{profileId,prompt}`; it captures the current report and exact original request privately. Use `.../perception/text` JSON `{profileId}` for translation. Source hints must agree, including regional `sn-ZW`; English input cannot silently become a Shona job. Only one Shona translation direction is implemented; attribute extraction, OCR and speech retain their earlier named language bounds. There is no language detection or English→Shona fallback, pivot or guessed M2M language code.

Set `DZOKA_AI_CORE_URL`, scoped `DZOKA_AI_TOKEN`, `DZOKA_AI_TENANT`, `DZOKA_AI_PROFILE`, `DZOKA_AI_QUALITY_DIR` and, only for local development, the explicit insecure-loopback exception. Run `python tools/quality_worker.py --once`. The launcher uses the reviewed generative environment, isolated Python and a 290-second process bound with 300-second durable leases. Unrelated DB/S3 credentials are scrubbed. Tenant/owner/live-lease/current-revision checks, shared queue/storage quotas, natural idempotency, retry/deadline/outbox atomicity, holds and erasure remain enforced.

The larger LLM uses the same independently authored escaped fixed chat framing and typed `smollm2-draft-v1` schema with a distinct artifact/source/config profile key. Every result requires `UNTRUSTED_DRAFT`; it remains query-dependent SHADOW and cannot join intrinsic fingerprints. The new decoding policy applies a sign-aware 1.1 repetition penalty and prohibits repeated generated-token bigrams; it is bound to the new profile. Larger size or repetition controls are not safety, factuality or quality guarantees.

Dedicated Marian tokenizes with the pinned source SentencePiece model, maps through its exact vocabulary, appends one EOS, seeds its full-prefix decoder with the configured start/PAD and decodes through target SentencePiece. It suppresses PAD, fails on generated unknown IDs and retains unknown-input counts/truncation. No publisher inference/beam/template code executes. `marian-source-target-v1` is distinct from M2M100's two-structural-token representation and is core-validated against the profile. Results require `UNVERIFIED_TRANSLATION`; original report text/language and temporal uncertainty remain unchanged. Explicit translation pins may join reviewed snapshots with availability coverage, which does not establish semantic correctness.

## Reproducible diagnostics

`python tools/quality_runtime.py probe` executes the four profiles on the versioned synthetic cases. `evaluate` additionally executes the older 135M/M2M100 baselines on the same cases and writes ignored `.test-tmp/quality-evaluation-result.json` (override using `--output`). `--profile NAME` narrows either operation. The fixture bytes are SHA-locked and mutation/duplicate case/scope errors fail closed. Reports retain each exact profile key, output, finish/token/unknown/truncation metadata, fixture/runtime hashes, exact English reference comparison where authored and repeated-trigram counts. Reference matching and repetition counts are narrow diagnostics; neither is calibrated language accuracy. No Shona baseline is invented for M2M100.

Nine authored synthetic cases cover bag/umbrella translations, Shona bag/place/code-switching and two general draft instructions. The Shona examples and English references need native-speaker validation. Do not tune an acceptance threshold on these examples or treat publisher corpus scores as Dzoka quality. [Execution evidence](../testing/m4-language-quality-2026-10-27.md) records actual gains and failures.

The complete local gate now requires thirteen phases and all earlier providers plus `--with-quality` native jobs in its underlying harness. Portable tests verify contracts/security only and label native inference omitted. English→Shona and further translation/speech/model/decoding quality work remain local engineering; permitted representative multilingual/recovery data, native-speaker acceptance, institutional policy and production infrastructure are external prerequisites. Full M4 and production readiness remain open.
