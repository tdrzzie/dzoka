# Separate hosted Shona reference comparison — 2026-11-17

## Review before requests

The existing [public-reference contract](../development/openrouter.md) and ADR 0043 permit bounded public translation diagnostics. Test `google/gemini-3.1-pro-preview`, provider allowlist `["Google"]`, in a separate private configuration: 1,024 output tokens, omitted temperature, 180-second transport timeout, deny-training/ZDR preferences, no fallback and no automatic retry. Keep the active Gemini Flash configuration and key unchanged. This uses a hosted proprietary service under its existing provider terms, not self-hosted Apache-licensed weights or an immutable checkpoint.

The [official endpoint catalog](https://openrouter.ai/api/v1/models/google/gemini-3.1-pro-preview/endpoints), read before requests on November 17, identifies the route and tier-dependent pricing. Its largest listed short-prompt rates are $3.60/million input and $21.60/million completion tokens. Thirty-two 1,024-token budgets therefore imply roughly $0.71 for capped completion tokens, plus a small input charge at those observed rates. This estimate is not a billing guarantee or account spend limit; service tiers are not pinned by the provider-name allowlist. The catalog snapshot is retained privately. No commercial/private-data agreement or zero-retention guarantee is inferred from routing preferences.

Freeze the exact original 32 FLORES/authored cases and their input/reference hashes used by the local comparison. Only licensed public references and the eight already-public first-party synthetic sentences may leave the host; no tenant reports, ownership evidence, contact details or local model outputs are sent. Retain the same eight-complete-per-slice, median CER ≤25%, worst CER ≤60% and 20/20 authored literal-fragment screens. Existing local and earlier hosted results remain separate matched baselines; compare only exact common cases and report coverage independently.

Reserve each private attempt before a potentially paid request, retain errors without replay, validate returned model/provider identities and record configuration/client/prompt/source/output hashes, timestamps and usage. Stop after an unavailable first compatibility case rather than making 31 more incompatible requests. Any later replay needs a distinct attempt/result identity. No production approval or active-route change follows automatically from this experiment.

## Frozen execution plan

The plan/configuration/evaluator must be frozen before the first request. Record actual coverage, safe failure categories, exact matched populations and quality disposition after completion. A live response alone does not close M4 or establish qualified semantic review.

## Original run completed; low-effort run reviewed before requests

The original run executes all 32 single requests: **23 complete, nine truncated, zero unavailable**. Its result SHA-256 is `564b1e49fae7d6a57946fb3067de552ea276e7337745349a201ea1cbcc309e40`. Do not replay or replace these outputs. The client bytes used by this run and the earlier full gate are preserved at SHA-256 `40f09f7af8ff13815f63ee8a8494c19609a7e8a7ab646bcd04f4b334399ca1e1` in the artifact manifest.

The official model catalog identifies mandatory reasoning, default medium, and supported low/medium/high. [ADR 0043](../adr/0043-versioned-quality-refinements-and-optional-reference-api.md) adds the explicit opt-in request control; all twelve focused contract tests pass, including invalid-value rejection, unchanged legacy payload, fixed output cap, provenance change and private-input denial. A separately frozen low-effort configuration/evaluator may now send at most 32 more single public/synthetic requests on the exact same cases and unchanged screens. Keep Google, 1,024 total output tokens, omitted temperature, original privacy/routing rules, no retry/fallback and the active default unchanged. Exclude returned reasoning text; this does not exclude its cost or guarantee a precise allocation.

Use a new private root and bind the new configuration/client/evaluator digests. Compare exact complete pairs and report original truncation separately. This is a prospective parameter experiment, not permission to combine differently configured successes into one fabricated complete run or a promotion decision. The final application gate must rerun on the changed source after local model math finishes.

## Verified results

All result/plan hashes, returned model/provider identities, configuration/client/source binding and reserved attempts are checked. Both configurations use the same 32 local-comparison cases, but their prompts/runtime differ from the local driver. These are joint configurations, not an isolated model-size experiment.

| Slice | Default complete / median / worst CER | Low complete / median / worst CER |
|---|---|---|
| Shona→English development | 7/8 / 31.8% / 56.4% | 6/8 / 37.3% / 52.6% |
| Shona→English additional | 5/8 / 40.2% / 63.6% | 8/8 / 40.1% / 62.7% |
| English→Shona additional | 3/8 / 39.0% / 54.5% | 8/8 / 48.9% / 64.5% |
| Eight authored code-switch cases | 8/8 / 0.0% / 20.0% | 8/8 / 0.0% / 4.3% |

The original run has 23 complete/nine truncated/zero unavailable. Low effort has **30 complete/zero truncated/two unavailable**. Median/worst calculations include only complete cases, so different coverage cannot establish a paired quality gain. On the exact **21 common complete pairs**, low has six lower CER, nine ties and six higher. Reported completion usage is 25,724 tokens for the original 32 responses and 15,998 for low's 30 responses; the two failed requests have unknown chargeable usage. Do not infer a matched billing reduction.

Default preserves all 20 authored literal fragments and passes that synthetic slice. Low preserves 19/20 under the unchanged exact-literal checker; its missed `not` is a contraction in English output. That lexical observation is not qualified Shona semantic approval. Every general slice still fails the declared screen. Twenty-four directional FLORES cases include reverse directions from shared underlying rows; they are not 24 independent entities or representative lost-property samples. The eight authored cases are synthetic and previously inspected.

The earlier Gemini Flash comparison uses additional rows 145–152 (Shona→English) and 145–148 (English→Shona), whereas these runs use 193–200. Only **eight development cases** overlap exactly. Original Pro has seven common complete Flash pairs: one lower error, one tie, five higher. Low has six: three lower and three higher. Do not claim twenty matched cases or compare the additional-slice medians as if their inputs matched. Keep the active cheaper Flash route unchanged; these results do not establish superior general quality for Pro.

| Private record | Plan SHA-256 | Result SHA-256 |
|---|---|---|
| `DzokaGeminiProReference-20261117-v1` | `c7b6755d2211702a2ea9c98a705f5d48070b7716574a652f03ce9dce28810794` | `564b1e49fae7d6a57946fb3067de552ea276e7337745349a201ea1cbcc309e40` |
| `DzokaGeminiProLowReference-20261117-v1` | `bc6b183e326104d2d1829add1e565e963f51752d2e21a61b8a373c78985a1f25` | `faf31d6eee4530fe72b477e16262e17ed2180947aec3c7f8de91ae3eba43ae4e` |

Private configuration file SHA-256 values are `ae3e1765c1f4b74f3ab1b28fce61c57a033afca563d949855cb2cd7cc55d77ab` (original) and `2e1f31ed123fdc8b65ecc70910826cd203b93350c0f5b2ec4491670c74fce599` (low); canonical configuration digests are separately retained in plans/results. The new exact client SHA-256 is `f26e9198b6a87c7150e909aeb830521ddf238f082b0ca00fe8738502e52b0c42`. Exact evaluator/client/analysis artifacts are retained in the [manifest](artifacts/m4-qwen9b-20261117/manifest.json). Hosted alias and provider identity do not supply immutable checkpoint/server bytes.

## Separate deliberate failure replay

The two original low-effort failures are `NETWORK_UNAVAILABLE` (development row 68) and `INVALID_PROVIDER_RESPONSE` (row 69). They remain unavailable and potentially chargeable in their original ledger. An explicitly separate, single-request-per-case replay uses the unchanged low configuration, source hashes and 1,024-token cap under `DzokaGeminiProLowReplay-20261117-v1`. Both replay outputs complete, with CER 22.0% and 16.7%. This verifies a subsequent response, not an established transient-fault root cause, automatic retry policy or availability SLO. No original output is replaced and no mixed-run 32/32 success is claimed.

The replay plan/result hashes are `e66fb67b640ed7a1646795c07b92c6cb0701b6facd3d054ca43e2a43bcacc816` / `2497910c439c28d15ce411b3b80f0c16c2d98f4b8ff5413effa7b4b4929c6d86`. The original primary result hash is independently unchanged afterward. M4 quality acceptance remains open; the final corrected source passes the complete gate recorded in the current-account deployment record.

Further Shona work is deferred by the user on November 17. The final application source later passes the complete 25-phase gate with 190 Python/24 Java unit/96 integration tests and the 35-table/3,223-row restore; see the current-account deployment record. This conformance does not approve the model quality reported here.
