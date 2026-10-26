# Source audit: globalenglish-lostfound

Status: COMPLETE

Scope: Matching/scoring/conflict contracts, retrieval SQL, persistence schema, image provider, recovery API boundary and selected matcher tests; no full dependency or runtime security audit.

Completion means bounded static source review, not a full security audit, executed tests, benchmark or production certification. No upstream implementation copied into Dzoka.

## Repository identity

https://github.com/globalenglish01/lostfound

## Audited branch

main. Revision pinned below; this record does not assert the pin remains branch HEAD.

## Exact commit SHA

6ff8cffe55bfc3e7274124512a69fae746ceb643

## Audit date

2026-10-26

## License

MIT. Root LICENSE inspected; see evidence manifest. Code and model-weight licensing are separate.

## Files inspected

- [LICENSE](https://github.com/globalenglish01/lostfound/blob/6ff8cffe55bfc3e7274124512a69fae746ceb643/LICENSE): lines 1–21. MIT notice verified.
- [backend/app/main.py](https://github.com/globalenglish01/lostfound/blob/6ff8cffe55bfc3e7274124512a69fae746ceb643/backend/app/main.py): lines 1–52. Router registration and wildcard CORS; no application-level authentication middleware declared here.
- [backend/app/matching/scoring.py](https://github.com/globalenglish01/lostfound/blob/6ff8cffe55bfc3e7274124512a69fae746ceb643/backend/app/matching/scoring.py): lines 1–261. Available-evidence normalization, confidence heuristic, level caps and evidence flattening.
- [backend/app/matching/conflicts.py](https://github.com/globalenglish01/lostfound/blob/6ff8cffe55bfc3e7274124512a69fae746ceb643/backend/app/matching/conflicts.py): lines 1–176. Missing attributes skip conflict checks; explicit rejection and graded penalties.
- [backend/app/matching/retrieval.py](https://github.com/globalenglish01/lostfound/blob/6ff8cffe55bfc3e7274124512a69fae746ceb643/backend/app/matching/retrieval.py): lines 1–245. Independent channels and RRF; newest-record pool; model name but not model version in vector predicate.
- [db/schema.sql](https://github.com/globalenglish01/lostfound/blob/6ff8cffe55bfc3e7274124512a69fae746ceb643/db/schema.sql): lines 1–145, 166–357. Raw/normalized descriptions, model-versioned embeddings, candidates, decisions, returns and audit tables.
- [backend/app/api/matches.py](https://github.com/globalenglish01/lostfound/blob/6ff8cffe55bfc3e7274124512a69fae746ceb643/backend/app/api/matches.py): lines 1–190, 200–217. Decision and return handlers accept actor fields from request payloads; return handler updates status without checking verification result.
- [backend/app/ai/image_provider.py](https://github.com/globalenglish01/lostfound/blob/6ff8cffe55bfc3e7274124512a69fae746ceb643/backend/app/ai/image_provider.py): lines 1–97. Explicit disabled provider errors; fixed-dimensional fitting of image and text vectors.
- [backend/tests/test_matching.py](https://github.com/globalenglish01/lostfound/blob/6ff8cffe55bfc3e7274124512a69fae746ceb643/backend/tests/test_matching.py): lines 1–135. Synthetic positive, missing-image, model and IMEI conflict tests inspected, not executed.
- [docker-compose.yml](https://github.com/globalenglish01/lostfound/blob/6ff8cffe55bfc3e7274124512a69fae746ceb643/docker-compose.yml): lines 1–63. API plus pgvector PostgreSQL, local images/model volumes and development credentials.

## Runtime architecture

FastAPI application with SQLAlchemy sessions, PostgreSQL hybrid search and local embedding providers. Inspected rematch endpoint calls matching synchronously; no event worker demonstrated in the reviewed paths.

## Domain model

Unified LOST/FOUND item records with raw descriptions, separate reports, candidates, evidence, human decisions and return records. Matching decisions and returns are separate records, but this alone does not enforce ownership authorization.

## Storage model

PostgreSQL with vector and pg_trgm, FTS and trigram indexes, VECTOR(1536), HNSW, AI-analysis provenance and model/version uniqueness. Local image and model-cache volumes in Compose. No tenant key in the reviewed schema.

## AI and matching implementation

RRF combines structured, lexical and vector routes, followed by evidence scoring and conflict penalties. Missing scores are omitted from normalization. Confidence is a coverage/reliability heuristic, not a calibrated probability. Disabled images explicitly raise. The lexical route uses ts_rank_cd and a trigram fallback; calling its score BM25 is misleading.

## Security model

No authentication dependencies appear in inspected match/decision/return handler signatures or router registration. Actor and role are supplied in payloads. This boundary is unsuitable for exposed institutional use without independent authentication and authorization enforcement.

## API design

Unversioned /api/items/... matching and return routes, persisted evidence output and LLM-assisted explanation. Reviewed return handler inserts submitted verification data then sets RETURNED without checking an authorization decision.

## Event model

Matching-run and audit tables provide historical records, not a durable event backbone or transactional outbox. No inbox/replay contract was established in the inspected paths.

## Deployment model

Compose runs API and PostgreSQL with persistent volumes. Mutable image tags and built-in development database credentials are unsuitable deployment defaults. No HA or restore validation performed.

## Test coverage

Inspected the first 135 lines of the synthetic matcher unit suite: positive case, missing-image behavior, color family and hard model/IMEI conflicts. No upstream test execution or coverage measurement; no tenant or return-authorization assurance.

## Strengths

Concrete evidence records, explicit critical contradictions, independent retrieval ranks, raw input preservation and model-versioned embedding schema. These are useful domain contracts to redesign independently.

## Weaknesses

Scoring uses reliability-or-1.0, so zero reliability becomes full reliability; confidence also filters zero reliability. Vector SQL filters model_name but omits model_version. Fixed-dimensional padding must not become an implicit compatibility guarantee.

## Security concerns

Request-supplied actor identity, no verified release authorization at the reviewed route boundary, unrestricted evidence endpoint access in those signatures, wildcard CORS and no demonstrated tenant isolation. Treat these as source-level boundary findings rather than a remotely tested exploit.

## Scalability concerns

Semantic and lexical queries operate over a newest-record bounded pool; older relevant items can be excluded before retrieval. Category is a separate channel, which avoids one AI category error becoming an absolute gate. Pool limits and HNSW do not establish million-record recall or latency.

## Licensing consequences

MIT source permits reuse subject to notices, but no code was copied. Adapting contracts does not approve transitive dependencies, model weights, insecure endpoints or production deployment.

## Reusable components

ADAPT evidence/coverage separation, critical-conflict semantics, per-run provenance and rank fusion concepts. Independently implement tenant-aware and version-aware contracts against Dzoka requirements.

## Do not reuse

Return handler authorization pattern; caller-supplied trusted actor identity; reliability truthiness fallback; model-name-only vector comparisons; recency-truncated search pool; developer Compose secrets and broad CORS.

## Architecture lessons

Keep retrieval rank, match score, evidence coverage, calibrated confidence, ownership and release separate. Bind vector compatibility to provider/model/version/dimensions/preprocessing. Do not let weak category evidence count as proof of ownership.

## Disposition

ADAPT. Applies only to the declared review scope; runtime adoption still requires the project-wide audit gate and relevant ADRs.

## ADRs triggered

Proposed: retrieval/scoring separation; missing evidence; hard conflicts; model-version compatibility; server-authorized release and tenant isolation. No runtime architecture ADR accepted while mandatory audits remain pending.

## Unresolved questions

Full engine/repository transaction behavior, raw identifier encryption, migration safety, complete provider fallbacks, dataset provenance, dependency licensing, runtime throughput and exhaustive auth review remain outside this bounded review. Re-audit before importing those subsystems.
