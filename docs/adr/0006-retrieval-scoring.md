# 0006: Separate retrieval from scoring

- Status: Accepted design; implementation and runtime dependency approval remain separate
- Date: 2026-10-26
- Prerequisite: mandatory source audit gate passed before these decisions

## Context

Research retrieval/reranking pipelines and platform shortcuts show that candidate recall and detailed identity scoring are different responsibilities.

## Decision

Retrieve bounded candidates independently from identifiers, lexical/trigram text, compatible vectors and spatial/temporal context; deduplicate and fuse ranks with versioned budgets. Apply reliable hard conflicts and detailed evidence scoring before optional bounded reranking. Persist route ranks, truncation and failures.

## Alternatives

One opaque full-corpus model score hides recall failures and makes heavy comparisons unbounded.

## Consequences

A missing retrieval route means partial results. Validated strong identifiers can retrieve outside approximate semantic routes but still undergo authorization and conflict checks.

## Source-audit evidence

The specification and [AGENTS.md](../../AGENTS.md) govern invariants and required stack preferences. These static reviews inform the decision; they do not establish benchmarks or full dependency certification.

- [visualmatch](../source-audit/retrieval/visualmatch.md) — `941c69fa7d82c7fd5f570ad7392833b8d0b83791`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [ilias](../source-audit/retrieval/ilias.md) — `208815da09c31f728ca8427520eb4898c5156273`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [globalenglish-lostfound](../source-audit/platforms/globalenglish-lostfound.md) — `6ff8cffe55bfc3e7274124512a69fae746ceb643`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [lost-and-found-tracking-system](../source-audit/platforms/lost-and-found-tracking-system.md) — `4ef1493f80590f6fa03d2ad9eded2e70a59ebe58`; inspected ranges and limitations are in the canonical audit and evidence manifest.

## Implementation implications

Test per-route recall, same-category hard negatives, filtered recall, route outage and bounded reranking; no self-generated visual claims.

The [architecture baseline](../architecture/baseline.md) defines ownership and execution boundaries. Complete the relevant [dependency adoption review](../architecture/dependency-adoption.md) before installing runtime dependencies. Acceptance scenarios here are requirements, not tests already executed.
