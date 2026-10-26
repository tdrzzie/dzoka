# 0012: Run expensive AI asynchronously

- Status: Accepted design; implementation and runtime dependency approval remain separate
- Date: 2026-10-26
- Prerequisite: mandatory source audit gate passed before these decisions

## Context

Media decoding, inference and reranking have variable cost and must survive request disconnects and retries.

## Decision

Core commits intake plus job/outbox in one transaction and returns a durable reference. Jobs progress queued, running, succeeded, partial, failed or cancelled with a lease and bounded retry policy. Worker results use job ID, attempt/lease token, tenant, input version and model version; core rejects stale or unauthorized completion.

## Alternatives

Synchronous inference holds request resources and couples reporting to GPU/provider availability.

## Consequences

User-visible progress and partial fingerprints are required. A failed inference does not erase raw intake. Quotas, deadlines and isolated decoders limit resource abuse.

## Source-audit evidence

The specification and [AGENTS.md](../../AGENTS.md) govern invariants and required stack preferences. These static reviews inform the decision; they do not establish benchmarks or full dependency certification.

- [paddleocr](../source-audit/vision/paddleocr.md) — `dab3fe35379033fdcb2d0e9572fac0b36c9a9ebf`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [visualmatch](../source-audit/retrieval/visualmatch.md) — `941c69fa7d82c7fd5f570ad7392833b8d0b83791`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [qdrant](../source-audit/retrieval/qdrant.md) — `222b2e44ab1c3f17e8246396ead6558e45041df7`; inspected ranges and limitations are in the canonical audit and evidence manifest.

## Implementation implications

Crash workers before/after result commit, expire leases, redeliver jobs, cancel work and submit late results; preserve one authorized logical result.

The [architecture baseline](../architecture/baseline.md) defines ownership and execution boundaries. Complete the relevant [dependency adoption review](../architecture/dependency-adoption.md) before installing runtime dependencies. Acceptance scenarios here are requirements, not tests already executed.
