# Retrieval, evidence scoring and confidence

Accepted design, 2026-10-26. Implementation is pending. This view follows the [current baseline](baseline.md) and ADRs [0006](../adr/0006-retrieval-scoring.md); [0008](../adr/0008-missing-evidence.md); [0010](../adr/0010-global-local-fingerprints.md). The canonical audits linked by those ADRs provide pinned source evidence and scope limits. Runtime/artifact choices must pass [dependency adoption](dependency-adoption.md).

## Pipeline

Authorize query and visibility scope; run exact protected identifier, lexical/trigram, compatible-vector and spatial/time candidate routes with independent budgets/deadlines. Fuse ranks and deduplicate canonical references, preserving route ranks and truncation. Reauthorize references, compare reliable conflicts, compute comparable evidence signals and apply bounded local/reranker work. Persist immutable candidate inputs and scoring/configuration versions.

Each signal has availability state, reason, similarity, reliability, applicable weight, independence group and source provenance. Missing/failed inputs are not mismatch. Available-evidence similarity normalizes only comparable reliable evidence; all-missing gives unavailable. Coverage is a separate value. Derived signals from one source do not become independent ownership proofs. Hard conflicts require trustworthy comparable identifiers; uncertain OCR does not qualify automatically.

## Result contract

Return candidate reference, route/rank provenance, observed supporting/conflicting signals, coverage, scoring version, optional valid calibration reference and explicit partial/unavailable reasons. Never describe heuristic rank as probability. Candidate generation cannot approve claims, reveal private answers or issue a handover code.

Pgvector is default; Qdrant remains an optional adapter with the same scope/model-space contract. Approximate retrieval must be measured against exact filtered baselines. Index staleness/deletion watermarks are visible and cannot resurrect deleted/private reports.

## Acceptance

A09–A13, A20 and A29; compare Recall@K before/after tenant/category filters, same-category instance negatives, sparse evidence, provider failure and bounded reranking.

Acceptance IDs refer to [the implementation acceptance ledger](acceptance.md); none is claimed executed by this document.
