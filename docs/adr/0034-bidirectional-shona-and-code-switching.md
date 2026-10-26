# 0034: Add bounded bidirectional Shona and explicit code-switch handling

- Status: Accepted scoped local-development profile
- Date: 2026-10-27

## Context

M4 lacks English→Shona. The dedicated Shona→English ONNX profile corrupts English fragments in a mixed-language report. Every earlier profile/output must remain reproducible and unverified translations cannot authorize recovery.

## Decision

Review Apache-2.0 int8 Marian CTranslate2 exports independently from the CTranslate2 runtime. Add separate directed CPU profiles and measure bounded greedy/beam alternatives on authored synthetic inputs before claiming improvements. Retain earlier providers/keys. Reuse private current-source jobs, exact raw reports, quotas, leases, retries, holds/erasure and explicit translation pins. No default language mapping, silent pivot or confidence is added.

Create a separate 23-wheel environment layering one reviewed CTranslate2 wheel over the unchanged 22-wheel environment. Force CPU/int8-float32 and bounded threads/generation; verify all installed bytes and retained notices before imports. Load only exact verified model bytes through the runtime's memory-file API. Bind model/config/vocabulary/tokenizer roles, native runtime, inference sources and decoding configuration directly into each profile identity.

The Windows distribution includes separately governed Intel/NVIDIA binaries. Its adoption is LOCAL_DEVELOPMENT_ONLY; the launcher must require the explicit development flag and a loopback core endpoint. This does not approve production/SaaS/binary redistribution. A suitable CPU-only build or further vendor-term/distribution review remains local engineering, not external infrastructure. No proprietary implementation source is copied into the MIT core. Retained license/notice texts are separate compliance material.

If model-only decoding does not preserve code-switched meaning, support only an explicit separately versioned annotation/preservation policy; never guess English spans, synthesize ownership facts or conceal source copying as model output. Any retained source fragments must be traceable to exact original offsets and exposed as unverified. Keep native-speaker and representative code-switch acceptance open.

## Alternatives

Missing-weight exports, ungranted conversions, guessed Shona language IDs, silently replacing models, unreviewed conversion/training dependencies and claiming corpus benchmarks as Dzoka accuracy are rejected.

## Consequences

Bidirectional local CPU inference is possible without a GPU/cloud account. Runtime vendor terms and deployment isolation require additional work before production. Permitted multilingual/recovery data and native-speaker/institutional acceptance remain external requirements. Full M4 remains open.

## Source-audit evidence

The mandatory thirty-source gate passed before implementation. [Provider contracts](0011-ai-provider-ports.md), [private language jobs](0031-private-language-inference.md), [directed profiles](0032-general-drafts-and-multilingual-profiles.md), [quality alternatives](0033-language-quality-and-shona.md) and [adoption](../dependencies/shona-adoption.md) govern this extension.

## Implementation implications

Verify real bidirectional outputs and code-switch failures/improvements, bounded input/output/beam behavior, exact role/provenance hashes, direction/trust/representation validation, private tenant/owner/lease boundaries, retries/erasure/pins and the complete local gate/restore. Preserve negative findings and distinguish copied fragments from generated text.
