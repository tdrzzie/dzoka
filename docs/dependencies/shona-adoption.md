# Bidirectional Shona model and runtime adoption

Reviewed 2026-10-27 before installation/model execution under [ADR 0034](../adr/0034-bidirectional-shona-and-code-switching.md). Approve the selected immutable Apache-2.0 int8 model/configuration/tokenizer/vocabulary bytes for bounded local conformance. Runtime is explicitly LOCAL_DEVELOPMENT_ONLY; no production/SaaS or binary-redistribution approval is implied.

- English→Shona: [manancode/opus-mt-en-sn-ctranslate2-android](https://huggingface.co/manancode/opus-mt-en-sn-ctranslate2-android/tree/06794e0ec31c3f79287b57fa64edaaf52121bcff), explicit Apache-2.0 export. Base [Helsinki-NLP/opus-mt-en-sn](https://huggingface.co/Helsinki-NLP/opus-mt-en-sn/tree/7313e969b1783060740e1ea49cf5ad0876e4792e) separately declares Apache-2.0.
- Shona→English: [manancode/opus-mt-sn-en-ctranslate2-android](https://huggingface.co/manancode/opus-mt-sn-en-ctranslate2-android/tree/2ee5c6ef9d65bec9ccb177f904d2cc2aff78a243), explicit Apache-2.0 export, with the independently inspected Apache-2.0 Helsinki base at `9fc9d70ed5584666e50ad1d43aa72317bfa6f5e2`.
- Inspected gaudi English→Shona exports omit model.bin; no missing weights are treated as runnable. Sams200's separate CC-BY-4.0 conversion was not selected. Ungranted Candle exports and unreviewed conversions remain unadopted. No model conversion/training program, model-supplied Python/Jinja or pickle load is run; the reviewed library package imports as installed.

CTranslate2 `4.8.4`, exact regular-GIL Windows CPython 3.14 amd64 wheel, 20,638,792 bytes, SHA-256 `4b322215f3b8eaab1db45b9e6aebfb8ddfe1336651e15a569c90826bfdb4b136`. METADATA SHA-256 `c1bb7364dbb3e9f8c8af2de5331a6f85e5ed00ad0d583ae2212917c74234a86c`. Dependencies numpy/PyYAML are already pinned in the base graph. No converter dependency extras are installed. Source tag v4.8.4 resolves to `5de572ef60b67a0276c5989ee73d0072808ff4c3`. Inspected MIT root, native/Python entry points, CMake, Windows build preparation and static DLL version metadata without executing upstream tests/converters.

The wheel omits legal text files and includes `ctranslate2.dll`, Intel `libiomp5md.dll` (20250910), and NVIDIA `cudnn64_9.dll` (9.10.2.21). The inspected build uses static MKL/oneDNN, spdlog/fmt, cpu_features, pybind11 and CUDA/Thrust headers. Retained complete upstream notices are hash-bound under `docs/dependencies/notices/shona/`: MIT CTranslate2/spdlog/cxxopts, Apache cpu_features/oneDNN and combined third-party terms, BSD pybind11, CCCL/Thrust's combined terms, and the Intel package's actual license/third-party texts. These are compliance notices, not copied implementation source. Review is bounded to published build pins/distribution hashes, not reproducible binary provenance or security certification.

The actual Intel OpenMP package uses the Developer Tools August 2024 agreement, including internal development and service/distribution restrictions; the oneMKL simplified grant is distinct and does not relicense every bundled binary. NVIDIA cuDNN 9.10.2 has its separate SDK grant and redistribution conditions. Primary [Intel terms](https://www.intel.com/content/www/us/en/developer/articles/license/end-user-license-agreement.html) and [cuDNN terms](https://docs.nvidia.com/deeplearning/cudnn/backend/v9.10.2/reference/eula.html) were inspected; cuDNN HTML snapshot SHA-256 `f3763a5ae20aa135e6f0186ac64c22f61c049e5ef5b81122ea06dd4b9ce58707` remains in ignored review cache. Local CPU development/test installation is approved; public-service deployment/redistribution requires a suitable build or separate vendor review. No GPU path or fallback is approved. The worker requires a development flag and loopback endpoint. Models remain Apache-2.0; runtime vendor components retain their own terms.

Exact model artifact roles/sizes/hashes, card evidence and implementation/runtime identities are pinned separately before execution. Earlier 106 Maven records, 21/22-wheel locks/environments and all model profiles remain unchanged. Complete model redistribution notices, advisories, permitted representative data, native-speaker acceptance and host isolation before production.

The selected OpenMP DLL independently matches the inspected Intel OpenMP 2025.3.0 wheel byte-for-byte. Model configuration has add_source_eos=false; the independently authored profile explicitly appends one EOS. Beam four/length penalty one, exact returned EOS and bounded generation are explicit. Optional KEEP profiles use caller-authored markers and exact short-sentinel restoration, with independent core source/output checks; they are not automatic language detection or semantic verification. Four profile identities preserve the distinct plain/marked directions.

## Inspected runtime source evidence

At immutable tag v4.8.4 / revision `5de572ef60b67a0276c5989ee73d0072808ff4c3`, review cache preserves these complete-file digests; inspected ranges are bounded below. No upstream test suite was executed.

| File | Inspected scope | Complete-file SHA-256 |
|---|---|---|
| LICENSE | full grant | `54aa79d9fe3c09e67a16dcd95b9e88676405a6ec174efda31036983cf7672ecb` |
| .gitmodules | full pinned dependency declarations | `5c91d8a3f142c0354e96a7a5122c78a2a1b6f735045aa4da74c474f55b6cedac` |
| CMakeLists.txt | 1–20 and selected backend/vendor grep lines; not a complete implementation review | `9703dc16bb59179d2ab2af38f58f2e4b02ab04e8528a3c3cec1c01e3f85e5aee` |
| python/setup.py | dependency/package/entry-point declarations | `844669b766580e5670bfe647fde68fdddb5cc643d7e164e56848e2dc8407aa2c` |
| python/ctranslate2/__init__.py | 1–60 native DLL loading/import boundary | `b98159455a98805e03821a0e9979f9bbd6fe7343e7dd30d5366c314eb71e4ed1` |
| python/tools/prepare_build_environment_windows.sh | full Windows build preparation/linked-library recipe | `413d8cec729bf4348939e5d8ce6f76195814e26e35cd406dff6100703f2a8115` |
