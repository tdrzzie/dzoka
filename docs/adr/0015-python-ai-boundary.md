# 0015: Separate Python inference from domain authority

- Status: Accepted design; implementation and runtime dependency approval remain separate
- Date: 2026-10-26
- Prerequisite: mandatory source audit gate passed before these decisions

## Context

Audited inference libraries use Python/native accelerators and have different runtime and hardware requirements from transactional workflows.

## Decision

Use Python/FastAPI for inference and expensive reranking with private authenticated endpoints or leased job consumers. Workers may read authorized sanitized media and submit derived result envelopes. They cannot approve ownership, mutate custody, release money or directly write core tables.

## Alternatives

Embedding native Python inference in Java increases runtime coupling; allowing workers full database credentials makes AI a domain authority.

## Consequences

Service identities carry task-scoped grants. Job references are reauthorized at execution and completion. Optional edge/federation deployments do not imply splitting every module.

## Source-audit evidence

The specification and [AGENTS.md](../../AGENTS.md) govern invariants and required stack preferences. These static reviews inform the decision; they do not establish benchmarks or full dependency certification.

- [paddleocr](../source-audit/vision/paddleocr.md) — `dab3fe35379033fdcb2d0e9572fac0b36c9a9ebf`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [paddledetection](../source-audit/vision/paddledetection.md) — `b25522a0f4bde8c80603f3ba5e3472059972e3b5`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [openclip](../source-audit/retrieval/openclip.md) — `2bcb5adf5090df595a78b56ca9aee7b1c70efc18`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [ilias](../source-audit/retrieval/ilias.md) — `208815da09c31f728ca8427520eb4898c5156273`; inspected ranges and limitations are in the canonical audit and evidence manifest.

## Implementation implications

Prove workers cannot read another tenant media or invoke release APIs, and reject stale, cross-tenant or unexpected result schemas.

The [architecture baseline](../architecture/baseline.md) defines ownership and execution boundaries. Complete the relevant [dependency adoption review](../architecture/dependency-adoption.md) before installing runtime dependencies. Acceptance scenarios here are requirements, not tests already executed.
