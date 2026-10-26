# Language quality alternatives and Shona adoption

Reviewed 2026-10-27 under [ADR 0033](../adr/0033-language-quality-and-shona.md), before graph download/execution. Approve only the immutable quantized graphs and tokenizer/configuration artifacts selected below for bounded local CPU conformance. Runtime remains the reviewed separate 22-wheel Windows amd64 CPython 3.14 profile; no extra wheel, upstream conversion program, custom Python/Jinja or remote code executes. Earlier locks/profile keys remain unchanged.

| Export | Immutable revision | Separately inspected grant/base |
|---|---|---|
| [Official SmolLM2-360M-Instruct](https://huggingface.co/HuggingFaceTB/SmolLM2-360M-Instruct/tree/a10cc1512eabd3dde888204e902eca88bddb4951) | `a10cc1512eabd3dde888204e902eca88bddb4951` | Explicit Apache-2.0 official export; quantized ONNX; same Llama/chat vocabulary family as the older distinct 135M profile |
| [TigreGotico Shona→English](https://huggingface.co/TigreGotico/opus-mt-sn-en-onnx/tree/71a742161e59bc41dc63374979ffcf6ac1db02db) | `71a742161e59bc41dc63374979ffcf6ac1db02db` | Explicit Apache-2.0 export; Helsinki-NLP/opus-mt-sn-en base separately declares Apache-2.0 at `9fc9d70ed5584666e50ad1d43aa72317bfa6f5e2` |
| [TigreGotico Spanish→English](https://huggingface.co/TigreGotico/opus-mt-es-en-onnx/tree/aa1c7cbd25cee1758155696f099d41cedc7faf6d) | `aa1c7cbd25cee1758155696f099d41cedc7faf6d` | Explicit Apache-2.0 export; base Helsinki-NLP/opus-mt-es-en grant independently inspected at `c96e2c5399ebfae4fc43d9669556b9afa74bb69d` |
| [TigreGotico German→English](https://huggingface.co/TigreGotico/opus-mt-de-en-onnx/tree/7d21e6010a23a7b2c38ace7f3b18e55ec73be4f3) | `7d21e6010a23a7b2c38ace7f3b18e55ec73be4f3` | Explicit Apache-2.0 export; base Helsinki-NLP/opus-mt-de-en grant independently inspected at `1a922f3b32a8e809e17a47d4b32142d8105924e5` |

Primary metadata/cards/configurations are retained under ignored `.audit-cache/language-quality-review`. Exact graph/tokenizer/configuration byte sizes/hashes, base/card evidence and independent implementation identities are pinned in `ai/quality.lock.json` before execution. Downloaded bytes must independently match hashes. Inspect source/target SentencePiece normalization/vocab mapping and EOS/PAD/decoder starts, without importing publisher code. Select separate int8 encoder and full-prefix decoder graphs rather than the much larger merged/cached conversion. This implementation does not inherit exporter's parity/beam-search/benchmark claims.

SmolLM2's inspected 360M configuration has 32 layers, width 960, 15 attention heads / 5 KV heads, head dimension 64 and vocabulary 49,152; fixed independently authored chat framing and 512/64 token bounds remain explicit. The independently authored new profile adds a 1.1 repetition penalty and no-repeat token bigrams, separately from the unchanged older decoder. Marian profiles independently tokenize source with source.spm, map pieces through the exact vocab, append EOS, seed decoder with the configured PAD/start and decode generated IDs through target.spm. Preserve original report bytes/language separately.

Shona base/card benchmarks are historical publisher claims on a different distribution; no local recovery score is inherited. The export card itself reports a wrong translation of a language name, which is a reason to preserve negative findings and require representative native-speaker acceptance. Shona→English is the only adopted Shona direction here. English→Shona candidates inspected include an ungranted non-ONNX distribution and licensed CTranslate2/modified-weight distributions requiring a separate runtime/export review; these are not silently executed or labeled an external infrastructure blocker.

This is bounded artifact/license/configuration review, not a complete training-data permission, vulnerability, reproducible native build or production distribution certification. Existing runtime bundled notices remain required. Complete model redistribution notices and advisory/security review before distribution.

## Exact adoption evidence

Twenty-one selected application artifacts total 733,248,475 bytes; expected graph identities below come from primary LFS metadata and require complete-download verification before loading.

| Profile / role | Expected bytes | SHA-256 |
|---|---|---|
| SMOL360_EN / llm.onnx | 364564671 | `57987a3a24dc34ad2cb5e7e566840ccaece095e35a24ae4fc5b3086c7ddd6918` |
| OPUS_SN_EN / encoder.onnx | 50751244 | `2c815894cab5a9569f1d1917f896d739c9d652024f2e517ba556098554a5f142` |
| OPUS_SN_EN / decoder.onnx | 88883046 | `e31853be3917eb931ca4de85b2b9879a3d07eb8110757a4c2eca3f38148b6bc4` |
| OPUS_ES_EN / encoder.onnx | 52721414 | `968a70f55d0249ac40c0e09861b556bc875ba0fe42f08aca7e2fcb3b5f960454` |
| OPUS_ES_EN / decoder.onnx | 59557443 | `93cf7fcb42a7a17577574206f21069b67402701a0ea7456545d6136eeab36fb7` |
| OPUS_DE_EN / encoder.onnx | 49188618 | `5dfaa76bbbbcf4a0f291024ea498bf6bf8087085b3aa455481e72a5cf0bf7ee9` |
| OPUS_DE_EN / decoder.onnx | 55997038 | `40c5d3e3a58cfb21946a7a260691c97d48560f117fcd38aa3c8b13d188a9e1ae` |

Seven independently inspected primary cards/base grants are pinned with raw-byte SHA-256 under `licenseEvidence` in the lock. Config/tokenizer files, their role mapping and exact revisions/hashes are separately recorded in `files`; tokenizer source/target direction is never inferred by swapping models. No publisher test was executed; native conformance below is independently authored.
