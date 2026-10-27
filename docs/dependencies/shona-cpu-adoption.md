# CPU Shona runtime and unmarked language-span adoption

Reviewed 2026-10-27 under [ADR 0035](../adr/0035-license-compatible-shona-and-unmarked-spans.md). The mandatory source gate passed first. Existing 106 Maven records, 21/22-wheel environments, local-only Windows Shona runtime and prior model/profile keys remain unchanged.

## Model adoption before download/execution

Approve [TigreGotico/glotlid-onnx](https://huggingface.co/TigreGotico/glotlid-onnx/tree/324ea6246b96c997c7eb63089ec5f272dc567b41) quantized ONNX graph/config/vocabulary/labels only. Export explicitly declares Apache-2.0; its base [cis-lmu/glotlid](https://huggingface.co/cis-lmu/glotlid/tree/85cd6716494360367b75f642b5bc78667605d0b4) grants Apache-2.0 plus complete additional notices. Retain that full LICENSE, including training-data limitations, without claiming original-corpus permissions. Source/base cards/license bytes and exact artifacts are independently pinned before execution. The selected graph is 418,935,890 bytes; not the 1.7 GB fp32/original model. Labels include `__label__eng_Latn` and `__label__sna_Latn`.

Inspect the numeric graph contract (int64 feature IDs to 2,102 probabilities), vocabulary order, signed-byte FNV-1a UTF-8 character n-grams, EOS, bucket/count bounds and primary fastText source/license. Independently implement features; no model-supplied Python, conversion program, pickle or training code executes. Publisher parity/accuracy figures are not inherited, and document scores are not calibrated token-level confidence. Use the unchanged reviewed 22-wheel ONNX/SentencePiece host environment.

Re-use the independently hashed Apache-2.0 manancode English↔Shona model/config/tokenizer/vocabulary artifacts from [the preceding adoption](shona-adoption.md), with new float32/container and plain/marked/unmarked profiles. No provider/key or old output is overwritten.

## CPU build dependency review

Source CTranslate2 v4.8.4 independently resolves upstream to `5de572ef60b67a0276c5989ee73d0072808ff4c3` (MIT). Selected source/submodule hashes, API and backend flags are inspected before execution. Disable vendor/GPU/oneDNN/OpenMP/Ruy backends; explicitly select OpenBLAS/CPU float32. Runtime image starts from cached official `python@sha256:593bd06efe90efa80dc4eee3948be7c0fde4134606dd40d8dd8dbcade98e669c` (Linux amd64 Debian bookworm), with exact package/dependency closure to be reviewed and pinned before installation/build. The base image is a platform/build dependency, not a new Python inference stack. Do not install the vendor CTranslate2 Python wheel.

Only archive/package metadata and license texts may be fetched/extracted until that closure review is recorded. Building reviewed source is implementation, not running upstream tests merely to inspect it. Compiler/build tools stay in the builder stage; retain exact runtime library notices and dynamic-link/source availability obligations. License compatibility permits relevant service use under the selected grants; it is not a redistribution/security/reproducible-build certification. Production registry, hosts/TLS/secrets and policy/data acceptance remain separate requirements.

## Exact closure approval before build (2026-10-27)

Approve the exact 65 Debian package archives recorded in `ai/shona-cpu-build.lock.json` for this isolated build; archive hashes are checked before offline installation. Debian copyright metadata was extracted without installing or executing these packages. Build-only GCC/binutils/make GPL tools remain in the builder. CMake and its support libraries retain their Debian notices. No GPL implementation is copied into the core. Runtime OpenBLAS pthread is BSD-3-Clause with full additional file notices, dynamically linked glibc is LGPL-2.1-or-later with BSD additions, libquadmath is LGPL-2.1-or-later, and libgcc/libstdc++/libgfortran carry GPL with GCC Runtime Library Exception 3.1. The independent driver is compiled through the eligible GCC compilation process. Full Debian copyright/common-license files remain in the image. Dynamic library replacement is possible; any redistributed image requires corresponding source availability and license compliance for its exact base/packages (Debian source packages), in addition to notices. This approval covers local build and service-compatible runtime selection, not distribution certification. The pinned base also includes Python and Debian utilities, not just the inference executable.

## Independently inspected source scope

CTranslate2 release branch/tag v4.8.4, upstream verified immutable revision `5de572ef60b67a0276c5989ee73d0072808ff4c3`, reviewed 2026-10-27. Complete source archive/submodule SHA-256 and byte counts are in the build lock; the selected actual source files below were read for public constructors, token bounds, compute selection and backend behavior. In particular: CMake options at 10–21/57 and backend branches 356–523; ReplicaPool CPU/compute constructor at 41–58; Translator public batch overloads at 24–50; model device/requested/effective compute getters at 59–81; GEMM dtype dispatch at 44–78. File lengths below identify the exact hashed source, not a claim of reviewing every line. This is bounded static inspection, not a complete upstream audit. Existing upstream CI YAML was inspected for context; upstream tests were not executed. Native driver conformance and Dzoka tests are separately recorded.

| Actual source file | File line extent; selected declarations inspected | SHA-256 |
|---|---|---|
| `CMakeLists.txt` | 1–921 | `9703dc16bb59179d2ab2af38f58f2e4b02ab04e8528a3c3cec1c01e3f85e5aee` |
| `include/ctranslate2/models/model.h` | 1–254 | `30a75d89dad4c144c3552a83775ea677453a182d9351a4dcdacffdf7d6640bc3` |
| `include/ctranslate2/replica_pool.h` | 1–395 | `d21edef7e4e7ddb61cf611c040028ac1fb7bbf6797823ca2d852011a9bd04b87` |
| `include/ctranslate2/translation.h` | 1–148 | `051a4709eb7ba27f0477bd7acfd02e39edf3f6d1eeebbbbc97d7b99d02d00a5b` |
| `include/ctranslate2/translator.h` | 1–347 | `448a1270d5e190bd4ee6c5b01dc41c378dcfd05d2817ddef3134c15a091c9ea4` |
| `include/ctranslate2/types.h` | 1–68 | `80fda2a3736d8936b4829c5c8e44ed7cbc465a1ade3808671037dda6e81ef02e` |
| `src/ops/gemm.cc` | 1–189 | `6ce76b8fdaee72983a7547be136f801a135242593b81f86e7789e1d447fad5a7` |

The independent language features follow [fastText v0.9.2 dictionary source](https://github.com/facebookresearch/fastText/blob/v0.9.2/src/dictionary.cc) hash/subword/EOS rules (lines 79–110, 145–194) and the pinned export's numeric configuration/reference feature description. No upstream feature implementation is imported or executed. ASCII whitespace, signed UTF-8 hashing and EOS are covered by independently authored regression fixtures; quantized-export parity against original fastText weights was not verified. Inference graph inputs/outputs and probability bounds were exercised directly. The full Apache-2.0-plus-notices GlotLID grant remains authoritative, including data provenance limitations. No downstream copying of publisher accuracy claims.

## Selection and executed build

The existing Windows CTranslate2 wheel remains local-development-only because its actual bundled vendor grants differ from the MIT library. The selected alternative compiles that MIT library with OpenBLAS and custom C++ threading rather than that wheel. The older ONNX Shona→English profile remains available separately but cannot supply the requested reverse direction. Existing M2M100 profiles exclude Shona. No unlicensed model export or original training corpus is adopted.

The offline build succeeded. Linkage of the actual driver and CTranslate2 library resolves OpenBLAS, libc, libstdc++, libgcc, libm, libgfortran and libquadmath; no Intel/NVIDIA/OpenMP libraries appear. `ai/shona-cpu-runtime.lock.json` pins the actual local image identity, host environment parent, build closure and retained notices. Unmodified 22-wheel host verification precedes inference. The current host launcher remains Windows amd64; the inference child is Linux amd64. A portable Linux host environment, signed registry publication, deployment SBOM/CVE review and target-host operational acceptance remain work. Rebuilding does not silently replace the pinned image or prior profiles.

## Final bundled-notice closure check

The final image also retains the actual MIT notices for bundled nlohmann JSON 3.11.2, half 2.2.0, BS thread pool 5.1.0 and fmt; AVX math functions carry MIT/zlib notices and AVX512 declares BSD-2 in the pinned CTranslate2 header. Independently fetched complete upstream grants below complement those embedded source declarations. The initial candidate image lacked several separate bundled notices and was not approved for final use; the final image is rebuilt with this complete retained notice set. This is a corrected closure/notice check, not an assertion that every bundled component had been independently reviewed in the initial candidate. No new upstream implementation is copied into Dzoka.

- `nlohmann/json` revision `bc889afb4c5bf1c0d8ee29ef35eaaf4c8bef8a5d`, `LICENSE.MIT`, SHA-256 `86b998c792894ccb911a1cb7994f7a9652894e7a094c0b5e45be2f553f45cf14`.
- `bshoshany/thread-pool` revision `bd4533f1f70c2b975cbd5769a60d8eaaea1d2233`, `LICENSE.txt`, SHA-256 `712a867cc12ebfcdecf6cea097f5aa7ec5acaa435a8f818c9f370960df35acbf`.
- `JishinMaster/simd_utils` revision `b3f3e45d9fa6356fd1eb5071d361c92c864182f2`, `LICENSE`, SHA-256 `dfaa54b4188ffa0e6298ce7ea27c865d12edcf73d742e2200ce9812db6268c0f`.

BS thread pool attribution: https://github.com/bshoshany/thread-pool . Image redistribution remains subject to all retained notices and the Debian dynamic-library/source obligations described above.
