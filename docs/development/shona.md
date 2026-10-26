# Local-development bidirectional Shona and explicit span preservation

[ADR 0034](../adr/0034-bidirectional-shona-and-code-switching.md) and [adoption](../dependencies/shona-adoption.md) add Apache-2.0 int8 model alternatives on a separate CTranslate2 runtime. The Windows wheel's Intel/NVIDIA components retain their own terms. This profile is **LOCAL_DEVELOPMENT_ONLY**; production/SaaS/binary redistribution is not approved. Use a suitable reviewed runtime build or resolve the vendor/distribution terms before deployment. This is unfinished local engineering, not a GPU/cloud infrastructure blocker.

## Setup and run

Provision the exact CTranslate2 wheel in `ai/shona-runtime.lock.json` into `.audit-cache/shona-wheels`, alongside the previously reviewed base/SentencePiece wheel caches. Run `python tools/shona_setup.py --local-development`; it creates `.audit-cache/shona-venv` using exactly 23 offline hash-pinned wheels with no dependency extras. Existing 21/22-wheel environments, locks and model profiles remain unchanged. Ten complete retained notice/license files are hash-checked. Model and wheel binaries stay ignored.

Run `python tools/shona_runtime.py fetch`, then `check` and `runtime-check`. Ten immutable model/config/vocabulary/source-and-target-tokenizer artifacts total 160,646,400 bytes. Model bytes are verified before native imports and supplied through the runtime's memory-file API, with no unreviewed model-path side files. Backend is explicitly CPU/int8_float32 with one translator and two intra-op threads; an unexpected backend fails closed. No model download or inference fallback occurs in the worker.

Generate disabled SQL using `python tools/shona_profile.py --tenant UUID --profile UUID --profile-name CT2_EN_SN`. Review and enable an exact profile deliberately. All four tasks are MULTILINGUAL_TRANSLATION:

| Profile | Direction / behavior |
|---|---|
| CT2_EN_SN | English→Shona, beam four |
| CT2_SN_EN | Shona→English, beam four |
| CT2_EN_SN_KEEP | English→Shona with explicit caller-marked literal copies |
| CT2_SN_EN_KEEP | Shona→English with explicit caller-marked literal copies |

Use the existing `POST /api/v1/tenants/{tenant}/reports/{report}/perception/text` JSON `{profileId}`. The current report language hint must match the source, including regional `en-*`/`sn-*`. This is not language detection or an automatic language/identity/pivot translation. The report retains its exact raw revision and language; inference leases current normalized UTF-8 text.

Set `DZOKA_AI_CORE_URL`, scoped `DZOKA_AI_TOKEN`, `DZOKA_AI_TENANT`, `DZOKA_AI_PROFILE`, `DZOKA_AI_SHONA_DIR` and `DZOKA_AI_SHONA_LOCAL_DEVELOPMENT=true`, then run `python tools/shona_worker.py --once`. The worker requires a loopback core hostname; HTTPS to a remote host is also rejected. HTTP additionally needs the explicit development `DZOKA_AI_ALLOW_INSECURE_LOOPBACK=true` flag. The launcher scrubs unrelated DB/S3 credentials, uses isolated Python and a 290-second bound; durable leases remain 300 seconds. Existing owner/tenant/current-revision RLS, natural idempotency, quotas, retries, deadlines, outbox atomicity, holds/erasure and explicit translation component pins remain enforced.

## Explicit copied spans

Plain models still corrupt the time qualifier in the unmarked code-switched example and can alter proper names. KEEP profiles add a distinct, opt-in preprocessing policy using caller annotations **stored in the report text**:

```text
Ndasiya [[keep:black Lenovo bag]] mu[[keep:Joina]] [[keep:around 2pm]].
I left a black bag in [[keep:Joina City]].
```

These are annotated inputs, not the original unmarked report or automatic English-span detection. Other text providers may see the annotation syntax too; structured annotation metadata/UI remains future work. A new report revision preserves prior raw revisions under the existing intake contract. Choosing a KEEP profile does not add annotations or revise a report automatically.

One to eight nonblank spans, each at most 256 UTF-16 code units, are accepted. No nesting, newline or bracket inside a span is allowed. Malformed/unmarked inputs and source collisions with reserved placeholder prefix `982` fail before queue reservation. The independently authored policy substitutes short numeric sentinels, translates the whole masked sentence and restores a span only when its sentinel occurs exactly once, in source order and outside a larger digit sequence. Alteration, omission, duplication, reordering, unconsumed sentinels or final output over 2,048 UTF-16 units fail closed. A protected span beyond token truncation is not silently copied back.

Every result remains UNVERIFIED_TRANSLATION. Marked results use `ctranslate2-marked-v1` and include `preservedSpans` with exact quote, sourceStart/sourceEnd, outputStart/outputEnd and SOURCE_COPY_UNVERIFIED. Offsets are UTF-16 `[start,end)` in the **leased normalized source** and derived output, not offsets in a potentially differently normalized raw request. The core independently parses the annotations and checks every source/output range before persistence. Literal copying does not verify language, meaning, identity, time or ownership. Original uncertainty such as “around” stays text; no timestamp is inferred.

Input cap is 128 model tokens including explicit source EOS, generation cap 32 including returned EOS. In marked mode, counts/unknowns refer to masked model input and generated placeholder tokens; copied strings are separately exposed through preservedSpans. They are not evidence of model understanding. Truncation and EOS/OUTPUT_LIMIT are explicit. Missing/unknown evidence never becomes a mismatch or release decision. No AI output authorizes property release.

`python tools/shona_runtime.py probe` runs eight authored cases across four profiles. `evaluate` additionally runs the unchanged prior Shona model on three identical unmarked sources and writes ignored `.test-tmp/shona-evaluation-result.json`. Cases are versioned in `ai/shona-evaluation.json`; tampering fails closed. Marked-input results are separately labeled and cannot be counted as automatic code-switch accuracy. [Execution evidence](../testing/m4-shona-2026-10-27.md) preserves outputs, failures, decoding experiments and bounds. `python tools/verify_local.py` now requires fifteen phases and the underlying harness's `--with-shona` in addition to every earlier native provider.

Native-speaker review and permitted representative multilingual/recovery datasets are still required for quality acceptance. Better automatic code-switch/proper-name handling, annotation UX, suitable production runtime packaging and native host isolation remain local engineering. Full M4 and production readiness remain open.
