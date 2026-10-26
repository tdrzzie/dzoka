# Source audit: lost-and-found-tracking-system

Status: COMPLETE

Scope: architecture-relevant static review of the listed source ranges, license evidence, deployment entry points and tests/evaluation where present. No upstream tests were executed and no upstream implementation was copied into Dzoka. Unreviewed subsystems and integration acceptance work are called out explicitly.

## Repository identity

https://github.com/Lost-and-Found-Tracking-System/lost-and-found-system

## Audited branch

main. The exact commit below is authoritative; branch HEAD may move.

## Exact commit SHA

4ef1493f80590f6fa03d2ad9eded2e70a59ebe58

## Audit date

2026-10-26

## License

MIT. Root LICENSE text inspected at the pinned commit: MIT. Dependency, checkpoint and dataset licenses remain separate.

## Files inspected

- [LICENSE](https://github.com/Lost-and-Found-Tracking-System/lost-and-found-system/blob/4ef1493f80590f6fa03d2ad9eded2e70a59ebe58/LICENSE), lines 1-21; role: license.
- [backend/src/services/embeddingService.ts](https://github.com/Lost-and-Found-Tracking-System/lost-and-found-system/blob/4ef1493f80590f6fa03d2ad9eded2e70a59ebe58/backend/src/services/embeddingService.ts), lines 1-12, 56-65, 73-82, 112-121, 395-434, 493-542, 552-561, 988-1042; role: implementation.
- [backend/src/services/findImagePairs.ts](https://github.com/Lost-and-Found-Tracking-System/lost-and-found-system/blob/4ef1493f80590f6fa03d2ad9eded2e70a59ebe58/backend/src/services/findImagePairs.ts), lines 1-12, 41-50, 127-136, 185-202, 205-214; role: implementation.
- [backend/src/routes/v1/claims.ts](https://github.com/Lost-and-Found-Tracking-System/lost-and-found-system/blob/4ef1493f80590f6fa03d2ad9eded2e70a59ebe58/backend/src/routes/v1/claims.ts), lines 1-81; role: implementation.
- [backend/src/services/fraudDetectionService.ts](https://github.com/Lost-and-Found-Tracking-System/lost-and-found-system/blob/4ef1493f80590f6fa03d2ad9eded2e70a59ebe58/backend/src/services/fraudDetectionService.ts), lines 1-12, 52-61, 78-87, 364-373, 497-579; role: implementation.
- [backend/src/models/claims.ts](https://github.com/Lost-and-Found-Tracking-System/lost-and-found-system/blob/4ef1493f80590f6fa03d2ad9eded2e70a59ebe58/backend/src/models/claims.ts), lines 1-65; role: implementation.
- [backend/package.json](https://github.com/Lost-and-Found-Tracking-System/lost-and-found-system/blob/4ef1493f80590f6fa03d2ad9eded2e70a59ebe58/backend/package.json), lines 1-54; role: deployment.
- [backend/tests/unit/middleware/auth.test.ts](https://github.com/Lost-and-Found-Tracking-System/lost-and-found-system/blob/4ef1493f80590f6fa03d2ad9eded2e70a59ebe58/backend/tests/unit/middleware/auth.test.ts), lines 1-65; role: tests.

## Runtime architecture

Express/TypeScript backend with Mongoose, Redis/Bull dependencies and remote Hugging Face vision calls; React frontend is outside this bounded backend review.

## Domain model

Claims, proof scores, confidence tiers, conflict groups, decision records and audit logs are separate models, but risk scoring can mutate approval status.

## Storage model

Mongoose claim schemas index item, claimant and status; ownership proofs are strings. Embedding service stores vectors and vocabulary-related structures on item metadata.

## AI and matching implementation

embedImageWithOpenCLIP makes a real remote inference request and returns [] on failure. embedTextWithTFIDF builds a per-description sorted vocabulary: positional cosine of independently constructed vectors is not a stable shared text space. calculateItemSimilarity uses fixed weighted dimensions.

## Security model

Claim submission derives claimant from authenticated req.user. The inspected GET /:id lacks a route-level auth middleware and populates claimant name/email; parent-router protection was not inspected, so deployment-wide exposure is not asserted.

## API design

Versioned claim routes include submit, user-specific listing and withdrawal. Claim and audit writes are sequential; a shared transaction was not established in the inspected route.

## Event model

Bull and notification dependencies exist, but package presence is not proof of durable/replay-safe processing. Inspected fraud processing directly changes multiple claim records.

## Deployment model

backend/package.json supplies TypeScript build, start, lint and Vitest commands; MongoDB/Redis/cloud-media dependencies increase operational boundaries. No deployed system was started.

## Test coverage

The inspected auth.test.ts reimplements token parsing inside tests and asserts constants; it does not invoke the production middleware. Other integration tests exist in the tree but were not run or used to claim security coverage.

## Strengths

Explicit claim/conflict/decision models, authenticated claimant derivation and real image embedding integration.

## Weaknesses

Missing vectors score as zero. Independent text vocabularies invalidate naive positional comparisons. Fraud processing selects a winner and calls awardClaim to set approved automatically.

## Security concerns

AI-derived comparative rank must not authorize ownership. Review the claim-read authentication chain, private proof disclosure, remote image URL fetch SSRF boundaries and provider timeouts before any adaptation.

## Scalability concerns

Pair-matching services and item similarity calculations do per-pair work; their presence does not establish bounded indexed retrieval or network-scale recall.

## Licensing consequences

Preserve applicable license/copyright/NOTICE obligations for future permitted reuse. Code licensing does not establish permission for every model weight or dataset. No upstream implementation was incorporated into the core.

## Reusable components

Adapt claim-conflict and human-review record concepts under independent Dzoka policy. MIT permits reuse with notices, but unsafe implementations are excluded.

## Do not reuse

Automatic awardClaim approval, risk derived merely from losing a score competition, per-document vector-coordinate comparison and isolated replica middleware tests.

## Architecture lessons

Keep ClaimRiskScore separate from match/ownership confidence; test the actual authorization middleware and transaction boundaries.

## Disposition

ADAPT. Applies to this declared integration scope. It is not a claim of production readiness, blanket source reuse approval or completion of later integration acceptance tests.

## ADRs triggered

Proposed investigations: private ownership evidence; trusted role assignment; deterministic release authorization; transaction/idempotency boundaries. This audit supplies evidence for later technical ADRs; it does not finalize the architecture.

## Unresolved questions

Full mount-level authorization, queue consumers, upload isolation, transactions and model/data licenses remain integration validation questions. No production safety or accuracy claim follows from this review.
