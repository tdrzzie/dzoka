# 0004: Keep Qdrant behind the vector provider contract

- Status: Accepted design; implementation and runtime dependency approval remain separate
- Date: 2026-10-26
- Prerequisite: mandatory source audit gate passed before these decisions

## Context

Specialized deployments may need an independently scaled index while PostgreSQL remains authoritative.

## Decision

Define VectorSearchProvider upsert, remove and query contracts with tenant, visibility, model-space, vector, filters, limit and deadline. QdrantProvider is a required eventual adapter, optional at deployment. Results contain canonical references, raw metric/direction and index watermark. Core reauthorizes returned references.

## Alternatives

Exposing vendor clients in matching logic would make backend migration and isolation difficult; dual synchronous writes would create partial-commit ambiguity.

## Consequences

Asynchronous projections use version checks, deletion tombstones and repair/rebuild jobs. Qdrant authentication/configuration remains an integration obligation.

## Source-audit evidence

The specification and [AGENTS.md](../../AGENTS.md) govern invariants and required stack preferences. These static reviews inform the decision; they do not establish benchmarks or full dependency certification.

- [qdrant](../source-audit/retrieval/qdrant.md) — `222b2e44ab1c3f17e8246396ead6558e45041df7`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [pgvector](../source-audit/retrieval/pgvector.md) — `5db3087b27a4fd095e7398f4b0f2706bf6d54bd3`; inspected ranges and limitations are in the canonical audit and evidence manifest.

## Implementation implications

Run the same provider contract suite against both backends: isolation, stale/tombstoned results, retries, incompatible spaces and index outage.

The [architecture baseline](../architecture/baseline.md) defines ownership and execution boundaries. Complete the relevant [dependency adoption review](../architecture/dependency-adoption.md) before installing runtime dependencies. Acceptance scenarios here are requirements, not tests already executed.
