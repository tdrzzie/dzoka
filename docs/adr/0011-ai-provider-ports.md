# 0011: Use replaceable AI provider ports

- Status: Accepted design; implementation and runtime dependency approval remain separate
- Date: 2026-10-26
- Prerequisite: mandatory source audit gate passed before these decisions

## Context

Cloud and local inference differ in availability, privacy, model artifacts and output shape.

## Decision

Define LLM, OCR, vision, speech, embedding and reranker ports. Every request specifies tenant policy, task, approved artifact/config, sanitized input references, schema and deadline. Return typed success, unavailable, invalid-output, policy-denied or failed results; validate outputs before persistence. Cloud transfer requires tenant permission and minimized data.

## Alternatives

Vendor SDK calls in domain logic or silent heuristic fallbacks obscure provenance and fail privacy policy.

## Consequences

Local/self-hosted operation remains required. Provider conformance and weight licensing are separate from library adoption. Untrusted media text/prompts never become executable commands or policy.

## Source-audit evidence

The specification and [AGENTS.md](../../AGENTS.md) govern invariants and required stack preferences. These static reviews inform the decision; they do not establish benchmarks or full dependency certification.

- [paddleocr](../source-audit/vision/paddleocr.md) — `dab3fe35379033fdcb2d0e9572fac0b36c9a9ebf`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [openclip](../source-audit/retrieval/openclip.md) — `2bcb5adf5090df595a78b56ca9aee7b1c70efc18`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [smart-asset-finder](../source-audit/platforms/smart-asset-finder.md) — `8ac4484354d23aea42e1a569e2f127a878049b1e`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [ai-lost-and-found](../source-audit/platforms/ai-lost-and-found.md) — `577edc3b492f62ac8707fc950230bc04a354e9d8`; inspected ranges and limitations are in the canonical audit and evidence manifest.

## Implementation implications

Verify equivalent contract behavior for local/cloud adapters, schema rejection, deadlines, policy denial, cancellation and explicit degraded mode.

The [architecture baseline](../architecture/baseline.md) defines ownership and execution boundaries. Complete the relevant [dependency adoption review](../architecture/dependency-adoption.md) before installing runtime dependencies. Acceptance scenarios here are requirements, not tests already executed.
