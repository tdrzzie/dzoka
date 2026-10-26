# 0009: Version every embedding and inference result

- Status: Accepted design; implementation and runtime dependency approval remain separate
- Date: 2026-10-26
- Prerequisite: mandatory source audit gate passed before these decisions

## Context

Omitted model versions, partial initialization and index refresh behavior can silently mix incompatible representations.

## Decision

EmbeddingSpace includes provider, model identifier, immutable artifact digest/revision, preprocessing version, dimensions, metric and normalization. Inference records also include input digest, configuration/prompt/output-schema versions, generated time and source region. Append new results; active pointers switch only after validation. Reject incompatible spaces, even if dimensions match.

## Alternatives

Overwriting vectors or comparing by dimensions alone makes results irreproducible.

## Consequences

Re-embedding is a resumable migration with coverage, shadow evaluation, activation and rollback. No configured/pretrained artifact means explicit unavailable, never random production inference.

## Source-audit evidence

The specification and [AGENTS.md](../../AGENTS.md) govern invariants and required stack preferences. These static reviews inform the decision; they do not establish benchmarks or full dependency certification.

- [openclip](../source-audit/retrieval/openclip.md) — `2bcb5adf5090df595a78b56ca9aee7b1c70efc18`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [openvisualsearch](../source-audit/retrieval/openvisualsearch.md) — `134dbb376bd61eac77bc388ef3531edd87fd5362`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [globalenglish-lostfound](../source-audit/platforms/globalenglish-lostfound.md) — `6ff8cffe55bfc3e7274124512a69fae746ceb643`; inspected ranges and limitations are in the canonical audit and evidence manifest.

## Implementation implications

Test same-dimensional incompatible spaces, partial re-embedding, stale results, deleted inputs, artifact checksum failure and activation rollback.

The [architecture baseline](../architecture/baseline.md) defines ownership and execution boundaries. Complete the relevant [dependency adoption review](../architecture/dependency-adoption.md) before installing runtime dependencies. Acceptance scenarios here are requirements, not tests already executed.
