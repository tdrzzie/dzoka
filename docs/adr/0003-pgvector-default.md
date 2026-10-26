# 0003: pgvector is the default vector backend

- Status: Accepted design; implementation and runtime dependency approval remain separate
- Date: 2026-10-26
- Prerequisite: mandatory source audit gate passed before these decisions

## Context

The specification chooses a PostgreSQL-first deployment; retrieval must preserve tenant and model-space boundaries.

## Decision

Implement PgVectorProvider as the default production vector adapter. Store vector provenance in authoritative fingerprint records; partition or separate indexes by compatible space/dimension. Explicitly request the metric appropriate to that space. ANN candidate counts are retrieval hints, never ownership decisions.

## Alternatives

A mandatory separate vector cluster increases operations; exact-only scans can be a small-data baseline but need measured limits.

## Consequences

Filtered ANN recall and maintenance must be measured against an exact baseline. Never silently truncate a recent-record pool or call an index timeout no matches.

## Source-audit evidence

The specification and [AGENTS.md](../../AGENTS.md) govern invariants and required stack preferences. These static reviews inform the decision; they do not establish benchmarks or full dependency certification.

- [pgvector](../source-audit/retrieval/pgvector.md) — `5db3087b27a4fd095e7398f4b0f2706bf6d54bd3`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [globalenglish-lostfound](../source-audit/platforms/globalenglish-lostfound.md) — `6ff8cffe55bfc3e7274124512a69fae746ceb643`; inspected ranges and limitations are in the canonical audit and evidence manifest.

## Implementation implications

Measure tenant-filtered Recall@K versus exact search, reject dimension/space mismatches, and verify index rebuild/deletion and timeout reporting.

The [architecture baseline](../architecture/baseline.md) defines ownership and execution boundaries. Complete the relevant [dependency adoption review](../architecture/dependency-adoption.md) before installing runtime dependencies. Acceptance scenarios here are requirements, not tests already executed.
