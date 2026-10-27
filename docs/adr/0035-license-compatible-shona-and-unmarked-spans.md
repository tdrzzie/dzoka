# 0035: Build a CPU Shona runtime and retain unverified language-span proposals

- Status: Accepted bounded implementation profile; dependency build approval is separately recorded
- Date: 2026-10-27

## Context

The preceding Windows CTranslate2 wheel is local-development-only because of bundled vendor terms. Unmarked mixed-language reports can lose English words/qualifiers; caller KEEP annotations are explicit but do not solve unmarked input. Source text, uncertainty and ownership decisions must remain separate from AI proposals.

## Decision

Build the reviewed immutable CTranslate2 C++ source with Intel MKL, oneDNN, CUDA, cuDNN, HIP, Ruy and OpenMP disabled; use reviewed OpenBLAS and explicit CPU float32. A small independently authored bounded binary-protocol driver replaces the vendor Python wheel. Pin base image, source/submodule archives, package versions/hashes, build flags, driver and resulting image identity. Review the exact build/runtime license closure before installing/building it. Retain source/dependency notices and separate dynamic-runtime obligations. This removes the earlier vendor-SDK adoption limit; it does not establish whole-system production readiness.

Reuse exact Apache-2.0 English↔Shona weights, private current-source jobs, quotas/leases/retries/holds/erasure and explicit translation pins. Keep every older profile/runtime unchanged. Run the CPU container without network, as non-root, read-only, with dropped capabilities, no new privileges and bounded memory/CPU/pids/time. Mount only reviewed model files read-only while holding verified source handles. Container-host/daemon deployment and production registry/host acceptance remain separate gates.

Approve the explicit Apache-2.0 ONNX GlotLID export and original Apache-2.0-plus-notices grant for bounded local classification after complete artifact checks. Independently implement the reviewed fastText feature contract; never execute publisher feature/conversion code. Model inventory includes English and Shona labels. Document-level language scores are not calibrated code-switch confidence.

A distinct unmarked Shona→English profile may propose bounded nonoverlapping English windows, retain exact source/output spans and raw classifier scores/provenance, and preserve those fragments through the declared translation policy. Use explicit score/margin/coverage thresholds and fail closed on missing classifier/runtime/artifacts or lost/duplicated/reordered placeholders. No accepted window means unavailable span proposals, not an identity mismatch or verified monolingual text. Expose empty proposal coverage separately; never invent manual annotations or imply user confirmation. Source copying/language guesses remain unverified and cannot authorize release. Retain plain and caller-marked profiles separately.

## Alternatives

Reusing the restricted Windows wheel in production, silently copying guessed English fragments, hard-coded recovery-word rewrites, unlicensed conversions, noncommercial model defaults and treating synthetic examples as representative accuracy are rejected. Broader models/training and human-validated datasets remain possible later work.

## Consequences

A license-compatible CPU path can be built and exercised locally without external inference services. Automatic span detection may be wrong; proposals and negative findings remain explicit. Native-speaker/representative acceptance, host/registry operations, distribution obligations, advisory/security validation and full M4 remain open.

## Source-audit evidence

The thirty-source gate passed before implementation. [Provider contracts](0011-ai-provider-ports.md), [language alternatives](0033-language-quality-and-shona.md), [local Shona](0034-bidirectional-shona-and-code-switching.md) and [adoption](../dependencies/shona-cpu-adoption.md) govern this profile.

## Implementation implications

Verify source/dependency/image integrity and absence of vendor backends, real CPU inference, classifier Unicode hashing/features, conservative proposal coverage, exact copy provenance and negatives. Exercise private tenant/lease/source checks, replay/retention/pins and the complete local gate/restore. Do not equate license compatibility or a passing fixture with production/language quality acceptance.
