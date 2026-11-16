# 0045. Evaluate a small African translation model on a fixed CPU boundary

- Status: Accepted model diagnostic plan; native build closure pending
- Date: 2026-11-16

## Context and decision

The full local 24-phase gate passes on source `1f52b0682dee3899cba9fa0c185e0861cd03f5f1ae2cd4c50a8dd0bcd1403b57`. The licensed MADLAD INT8 alternative executes but fails the predeclared Shona quality screen. Hosted Gemini improves matched public-reference scores against Qwen, while local quality and semantic acceptance remain open. The user explicitly requested continued implementation through failures.

Review `qvac/TranslatePsy-AfriSLM-0.8B-Q8-GGUF` at `55220bcc74bed219ad9db9ab378f6aae31850974` for a separate bounded local diagnostic. The publisher/export explicitly grants Apache-2.0 for its weights and lists Shona. Its base is Qwen3.5-0.8B, separately pinned and licensed. Preserve the teacher/synthetic-data noncommercial-license disclosures; this decision authorizes neither use nor redistribution of those datasets or teacher weights. Exact evidence and limitations are in the [model review](../dependencies/afrislm-cpu-review.md). Published benchmark claims are not executed Dzoka evidence.

Permit downloading the exact 1,082,015,808-byte GGUF only, requiring full SHA-256 `b5056635be0c9bb832f0c964dc063bd4b4462e253f9f9d94eebf0aa0960b7798` before execution. Do not run model-supplied Python, conversion code or dependency installers. Retain all prior artifacts, profiles and immutable releases.

The proposed runtime is a separately source-pinned llama.cpp CPU library with an independently authored bounded C++ driver. Keep common utilities, tools, server, UI, subprocess support, optional downloads, GPU/vendor backends and upstream tests disabled. Reuse the reviewed offline Debian build packages and base; no Python inference dependency is added. Complete and record the selected source/license/build closure before building or running it. Containers must have no network, a read-only root, dropped capabilities, no new privileges and explicit CPU/memory/process/time limits, with only the verified model mounted. Source/output counts and truncation must remain explicit; malformed or unavailable inference fails closed.

Freeze development and additional translation cases and tolerances before inference. Report directions separately, preserve unavailability/truncation and unknown training overlap, and compare exact matching source/reference bytes. Native compatibility or an upstream score cannot establish semantic accuracy or promote a profile. Production integration requires a separate accepted runtime closure and versioned profile/evidence contracts if the diagnostic merits adoption.

## Alternatives and consequences

Do not dequantize a larger model into the existing Marian boundary or relabel GGUF evidence as CTranslate2. Keep the existing CPU providers available under their historical identities. The smaller translation-specialized model may fit this host, but compute support, output framing, memory and quality remain unverified at this decision. No change to tenant authority, secret ownership evidence, fingerprint activation, recovery authorization or property release follows from this diagnostic.
