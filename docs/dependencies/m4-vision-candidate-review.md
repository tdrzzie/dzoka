# M4 vision candidate review: not adopted

Review date: 2026-10-26. Outcome: **DEFERRED**, no weights downloaded/loaded, no runtime provider activated and no dependency/model lock changed.

Inspected the primary Hugging Face publisher metadata/card/configuration for `Xenova/clip-vit-base-patch32` at immutable revision `6ef1ebc8b0766a7a8d11b146462c99cdf74dd22d`. The [versioned card](https://huggingface.co/Xenova/clip-vit-base-patch32/blob/6ef1ebc8b0766a7a8d11b146462c99cdf74dd22d/README.md) links an upstream CLIP model and describes an ONNX conversion, but the inspected card/metadata declare no model license and the file inventory contains no LICENSE. A linked base model's terms alone were not treated as a separately verified grant for this exact converted distribution.

| Inspected metadata | SHA-256 |
|---|---|
| Revision API metadata with LFS records | `17beccb64f5b8c6d37467855bfc37d7b00f2d9cc2d7f28c4035b6e4a08344b57` |
| README.md, 510 bytes | `3fdc3742da5d9b24839ee8d7bd3a1f781768dca4302114b7960a8dc99df91f58` |
| preprocessor_config.json, 520 bytes | `6f638fb9401a6d6296feff533ee7efe657b787c49f954f82f5906b36ef2a1b1f` |
| config.json, 4524 bytes | `493ef57ff783e42d1530c91b53469b7fdf8db8a9c1408e86998fcb7899a4f495` |

The publisher's LFS inventory names `onnx/vision_model_quantized.onnx`, 89,117,001 bytes, SHA-256 `583fd1110a514667812fee7d684952aaf82a99b959760c8d7dca7e0ab9839299`. This is an inspected metadata assertion, **not** a downloaded/independently hashed or executed model. The configuration describes 224-square center crops, RGB conversion, rescaling and channel normalization with a resampling policy. No substitute preprocessing profile was invented or claimed compatible.

The review evidence is cached under ignored `.audit-cache/m4-vision-review`. Next steps: verify the exact conversion distribution's grant, or select/review an explicitly licensed alternative or independently produce a permitted export with reviewed conversion tooling. Clarification from this distributor is an external permission dependency only for this candidate; choosing another cleared artifact, implementing sanitized-media job contracts, and reviewing OCR/speech providers remain locally possible engineering work. No generic claim that vision/OCR/speech needs cloud infrastructure or a GPU follows from this review. This supplementary artifact review does not alter the 30 completed mandatory source audits.
