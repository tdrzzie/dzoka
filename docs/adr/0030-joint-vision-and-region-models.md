# 0030: Bind both joint encoders and bounded region-model evidence

- Status: Accepted scoped implementation profile
- Date: 2026-10-26

## Context

ResNet class signatures do not supply a joint text/image space; generic visual signatures do not establish object boxes or semantic masks. New models must not silently change already published profile keys or treat equal dimensions as compatibility.

## Decision

Add separately versioned local ONNX profiles for paired Jina CLIP v1 image/text embeddings, SSD-MobileNet detection and FCN-ResNet50 semantic segmentation. Keep the existing text/perception provider sources, locks and profiles intact. Use the same reviewed CPU/NumPy/Tokenizers runtime; no new dependency or remote model code. [Adoption](../dependencies/joint-vision-adoption.md) records exact artifact grants and pins before execution.

JointSpace binds both encoder artifacts, exact serialized tokenizer, image/text processing, dimensions, metric/normalization, runtime and adapter source identity. Image/text profile keys remain task-specific; their shared-space definition/key must match, and the core recomputes both manifest and shared-space hashes. A dimension-only or model-name comparison is insufficient. Results declare finite unit vectors and the shared key; text declares truncation/token count. This does not implement unrestricted search or calibrate similarity.

Reuse private perception jobs, quotas, retries, input authorization and explicit multimodal pins. Joint text reads only the claimed current normalized report revision through a lease-bound byte endpoint; no extra raw-text copy or public input is created. Detection and semantic segmentation require the existing sanitized private PNG boundary. Detection is bounded scored source-pixel boxes with an explicit COCO class vocabulary. Segmentation is a bounded row-major run-length semantic class mask covering the source image with an explicit grid/vocabulary; it is not instance identity or facial recognition. The core checks geometry, label/count ranges, complete mask coverage and finite outputs before immutable append/activation.

## Alternatives

Separate unbound encoder versions can fabricate joint compatibility. Undeclared converted weights, arbitrary model URLs, pickle/custom remote code, unbounded full-resolution masks or source replacement would weaken licensing, provenance and resource controls. Reusing ResNet class logits as if they were CLIP text/image embeddings is rejected.

## Consequences

Local CPU conformance and real service-token publication/activation are testable here. Custom bounded interpolation/resolution/token limits form an explicit distinct profile; publisher benchmark numbers are not inherited. All wider quality/language/retrieval/calibration, deployment OS isolation and production acceptance remain separate gates. Semantic masks and detection scores cannot authorize release or establish ownership.

## Source-audit evidence

- [OpenCLIP](../source-audit/retrieval/openclip.md), `2bcb5adf5090df595a78b56ca9aee7b1c70efc18`: complete model/preprocessing identity and explicit activation.
- [PaddleDetection](../source-audit/vision/paddledetection.md), `b25522a0f4bde8c80603f3ba5e3472059972e3b5`: bounded versioned detector outputs and separate model grants.
- [ADR 0029](0029-local-perception-providers.md) retains private inputs, immutable evidence and policy separation. No upstream implementation code is copied.

## Implementation implications

Test both real encoders, shared-key tampering, incompatible spaces with identical dimensions, finite normalized shape, source geometry/RLE completeness, wrong input/task/tenant/lease, text revision/expiry revocation, exact completion replay, rollback/erasure and restore. Preserve all older profile identities and source-audit/supplied bodies.
