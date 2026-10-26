# 0010: Combine global and local visual fingerprints

- Status: Accepted design; implementation and runtime dependency approval remain separate
- Date: 2026-10-26
- Prerequisite: mandatory source audit gate passed before these decisions

## Context

Global category similarity misses scratches, logos and instance details; local retrieval research supplies useful concepts but no proven recovery accuracy.

## Decision

Store global descriptors and optional object/region/patch descriptors with crop coordinates, source media and model-space keys. Use coarse global retrieval followed by bounded local comparisons. Exact identifiers, OCR, attributes and perceptual hashes remain distinct evidence channels.

## Alternatives

Whole-image-only matching misses instance evidence; storing all patch-pair scores online has unproven cost.

## Consequences

Patch budgets, aggregation and score calibration require consented/synthetic recovery evaluation. Independently implement contracts; no copying unresolved or GPL source.

## Source-audit evidence

The specification and [AGENTS.md](../../AGENTS.md) govern invariants and required stack preferences. These static reviews inform the decision; they do not establish benchmarks or full dependency certification.

- [ilias](../source-audit/retrieval/ilias.md) — `208815da09c31f728ca8427520eb4898c5156273`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [patchwise-retrieval](../source-audit/retrieval/patchwise-retrieval.md) — `d593101957933fcd574649fcb3b0af02c7295c11`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [reranking-transformer](../source-audit/retrieval/reranking-transformer.md) — `81fca3ca9b191950f702ae712daef0400608ae3a`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [furicbond-lost-and-found](../source-audit/platforms/furicbond-lost-and-found.md) — `ebe3974ef0c0096a521836b934661fa6e4f99897`; inspected ranges and limitations are in the canonical audit and evidence manifest.

## Implementation implications

Evaluate same model/color but different instance, background leakage, occlusion, crop changes and bounded memory/latency; record negative results.

The [architecture baseline](../architecture/baseline.md) defines ownership and execution boundaries. Complete the relevant [dependency adoption review](../architecture/dependency-adoption.md) before installing runtime dependencies. Acceptance scenarios here are requirements, not tests already executed.
