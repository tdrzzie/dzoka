# 0033: Append language alternatives and measure their limits

- Status: Accepted scoped implementation profile
- Date: 2026-10-27

## Context

The preceding M4 profiles run locally but show translation meaning errors and repetitive general drafts. M2M100 has no Shona token. A broader vocabulary or model publisher score cannot substitute for actual local output and representative evaluation.

## Decision

Append separately keyed official Apache-2.0 SmolLM2-360M-Instruct and explicit Apache-2.0 OPUS-MT ONNX profiles for Shona, Spanish and German into English. Review immutable export and base-model grants/configuration separately. Bind encoder/decoder/source-tokenizer/target-tokenizer roles directly into profile identity so a swapped role cannot reuse a key. Reuse the exact approved 22-wheel Windows runtime, safe model-file handles and private jobs. Preserve every earlier model/runtime/profile key and output version. No new runtime dependency or publisher code is adopted. Bind a 1.1 generated-token repetition penalty and no-repeat bigrams to the new larger draft profile; preserve the initial circular greedy output as a negative finding.

Shona support is explicitly one-way sn→en, with source hints and typed UNVERIFIED_TRANSLATION outputs. Do not pretend that M2M100 supports sn, reinterpret another language token or silently pivot/fall back. English→Shona needs its own reviewed executable export/runtime and verification. No automatic language detection is added.

General drafts remain UNTRUSTED_DRAFT and cannot become intrinsic fingerprints or release authority. Translation retains raw source and explicit unknown-token/truncation provenance. Dedicated Marian source/eos formatting has one structural token, distinct from M2M100's two. Bind representation to the manifest and validate it in the core.

Add a reproducible, versioned authored synthetic evaluation with exact inputs/references, per-case outputs and baseline/candidate identities. English reference comparisons and repetition/limit observations are diagnostic, not calibrated accuracy or native-speaker acceptance. Preserve negative findings and never select a default just because a single fixture improves.

## Alternatives

Restricted noncommercial models, unlicensed exports, replacing published keys, guessed Shona mappings, unreviewed conversion runtimes and claiming fluent outputs are verified facts are rejected. Larger models, improved decoding or training remain possible after separate adoption/measurement.

## Consequences

Local CPU implementation can improve specific observed errors without external inference infrastructure. Permitted representative multilingual/recovery data, native-speaker review and institutional acceptance remain external requirements. Host isolation, longer/broader profiles and further quality engineering remain local work. Full M4 and production readiness remain open.

## Source-audit evidence

The mandatory thirty-source gate passed before implementation. [Provider contracts](0011-ai-provider-ports.md), [private language jobs](0031-private-language-inference.md), [private drafts/directions](0032-general-drafts-and-multilingual-profiles.md) and [new adoption evidence](../dependencies/language-quality-adoption.md) govern this scoped extension. Primary model/config/license inspection is not upstream test execution.

## Implementation implications

Verify native outputs and bounds, manifest/representation identity, exact source preservation, unsupported directions, tenant/lease/result isolation, explicit pins, draft exclusion, regression evaluation integrity and the complete local gate/restore. Keep binaries/raw private diagnostics ignored.
