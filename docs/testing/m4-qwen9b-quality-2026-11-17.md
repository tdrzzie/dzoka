# Larger Qwen CPU Shona diagnostic — 2026-11-17

## Reviewed candidate and frozen comparison

[ADR 0048](../adr/0048-larger-qwen-cpu-translation-diagnostic.md) and its [license/runtime review](../dependencies/qwen9b-cpu-review.md) authorize this bounded diagnostic before execution. The ungated Apache-2.0 Qwen3.5 9B Q4_K_M export is pinned to revision `3885219b6810b007914f3a7950a8d1b469d598a5`. The completed 5,680,522,464-byte file verifies SHA-256 `03b74727a860a56338e042c4420bb3f04b2fec5734175f4cb9fa853daf52b7e8`; incomplete/failed earlier downloads are retained separately. No gated Sunbird weights, converter, model-supplied Python or Unsloth library execute.

The independent CPU driver changes only the verified embedding geometry from the 4B diagnostic. Its exact source/build/evaluator/framing/analysis bytes are retained in [artifacts](artifacts/m4-qwen9b-20261117/README.md). The native image is `sha256:9c86ecf855ade68c92aca7fedb993a330456e2442a1695a33aced491c5d839bc`, with actual compiler, link, binary and runtime package inspection. No upstream tests are claimed.

The same original 32 cases are frozen: FLORES Shona→English rows 65–72 and 193–200, English→Shona rows 193–200, plus eight first-party authored code-switch cases. The prior [TranslatePsy/MORENA comparison](m4-african-cpu-quality-2026-11-16.md) retains baseline/candidate identities, licenses, leakage limits and failures. Screens remain eight complete per slice, median CER ≤25%, worst ≤60%, and all 20 authored literal fragments matched. General-domain, previously inspected references and eight synthetic cases do not establish representative semantic approval or unknown training overlap.

## Q4 attempt — resource preflight fails

After the exact 25-phase application gate passes, the queued Q4 evaluator fails its eight-GiB available-memory assertion before creating a private evaluation root or making any model call. A separate bounded probe reports 16,764,338,176 bytes guest total and 6,020,374,528 bytes available; swap is not a reason to exceed the approved budget. Preserve `.test-tmp/qwen9b-evaluation-20261117.log` and `.test-tmp/qwen9b-memory-preflight-20261117.json`. This is zero executed model calls, not 32 measured translation failures or a model-compatibility result. The complete hash-verified Q4 weights remain retained.

## Separately reviewed low-memory attempt — completed

Before download/execution, the ADR amendment separately reviews `Qwen3.5-9B-UD-IQ2_M.gguf` from the same exact export: 3,649,365,216 bytes, SHA-256 `a31b6b4c603ddd66b5496ee065c9dd13b2b6de77dce9e5d59609aef8d39868e7`. The same existing Apache license review applies. A new evaluation plan binds that weight digest to the exact unchanged driver/image; the original Q4 build closure and failed attempt remain unchanged. Lower precision is a quality risk; there is no measured Q4 result to compare it against.

A separate evaluator requires five GiB available and caps both memory and memory-plus-swap at five GiB, retaining two CPUs, original fixed context/prompt/output limits, no network/non-root/read-only execution, dropped capabilities and durable exact-owned cleanup. It uses the same 32 cases and unchanged screens, and a new private result root. No other workloads are stopped, engine settings changed or dependency/converter installed. The full 3,649,365,216-byte weight digest verifies before inference. All 32 planned calls execute with the exact five-GiB/no-swap limits and owned cleanup. The results below determine the candidate disposition.

The actual image passes **16 malformed binary-frame rejections plus one positive valid-frame control**. These model-free native tests use a 128-MiB/no-network container and do not establish weight compatibility or translation quality. Exact sources/result are retained in the artifact manifest.

No model activation or M4 completion follows from downloading weights, passing framing tests or preserving source artifacts.

## Verified low-memory results — reject for rollout

The result contains **24 complete outputs, six truncated and two unavailable**. All 32 owned invocation journals end REMOVED. Model-loading-stage failures occur on Shona→English row 69 and English→Shona row 197, with native exit 1 and the fixed stage-2 diagnostic. That stage covers loading/geometry checks and does not establish the underlying resource/I/O/library cause. No unavailable call is converted to a mismatch or silently replayed.

| Slice | Complete | Median / worst CER | Coverage/fragment result |
|---|---|---|---|
| Shona→English development | 7/8 | 74.1% / 109.0% | One unavailable |
| Shona→English additional | 8/8 | 69.6% / 82.7% | Complete but quality fails |
| English→Shona additional | 1/8 | 39.0% / 39.0% | Six truncated, one unavailable; one score is not eight-case acceptance |
| Authored code-switch | 8/8 | 25.2% / 61.7% | Only 15/20 exact protected fragments |

Every declared slice fails. Of the **16 exact common complete pairs** with the original baseline, five have lower CER and eleven higher; no generalized quality gain is established. CER may exceed 100% when insertions dominate and is not a probability or semantic-accuracy percentage. The earlier 4B/MORENA alternatives remain separately recorded, not overwritten. This quantization's poor results do not establish how unexecuted Q4/full-precision weights would behave, or isolate model size from precision/template/runtime effects.

The private root is `C:\Users\nyash\DzokaQwen9BLowMemoryDiagnostic-20261117-v1`. Plan SHA-256 is `56646ecaedd07d53a7b15b0cb90363a316ee0dcdb95498288e075236e53a6d77`; result SHA-256 is `f4086fc0b673cede60754b89dbf11d6b48271f340d1f46f1afa9057ca1c4dd74`. Exact raw outputs/references remain private. The public artifact manifest retains checked aggregate results and source/build/evaluator bytes. The original analysis helper first rejects the baseline's different nested journal layout; its failed log/source remain retained. The corrected v2 helper independently verifies 32 baseline model removals plus 32 grant removals, and 32 direct removals for each GGUF run, before reporting metrics. No model evidence or result is modified to fix the analyzer.

This candidate is **not registered, activated or promoted** into the application. The current-account worker uses the existing reviewed CPU profile in its actual core integration. [Hosted reference experiments](m4-hosted-shona-pro-2026-11-17.md) remain public-only and also fail general quality screens. The additional Shona-specific publisher adapter has unresolved license/base access; the user confirms no existing HF access. M4 remains open for representative/qualified quality and the remaining deployment requirements.

Further Shona work is deferred by the user on November 17. The final application source later passes the complete 25-phase gate with 190 Python/24 Java unit/96 integration tests and the 35-table/3,223-row restore; see the current-account deployment record. This conformance does not approve the model quality reported here.
