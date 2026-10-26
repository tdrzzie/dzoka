# Source audit: findback

Status: COMPLETE

Scope: architecture-relevant static review of the listed source ranges, license evidence, deployment entry points and tests/evaluation where present. No upstream tests were executed and no upstream implementation was copied into Dzoka. Unreviewed subsystems and integration acceptance work are called out explicitly.

## Repository identity

https://github.com/pirinthaban/findback

## Audited branch

main. The exact commit below is authoritative; branch HEAD may move.

## Exact commit SHA

fc91563886e4e8da6931bc07dd025fbe235068a2

## Audit date

2026-10-26

## License

MIT. Root LICENSE text inspected at the pinned commit: MIT. Dependency, checkpoint and dataset licenses remain separate.

## Files inspected

- [LICENSE](https://github.com/pirinthaban/findback/blob/fc91563886e4e8da6931bc07dd025fbe235068a2/LICENSE), lines 1-21; role: license.
- [functions/src/triggers/onClaimCreated.ts](https://github.com/pirinthaban/findback/blob/fc91563886e4e8da6931bc07dd025fbe235068a2/functions/src/triggers/onClaimCreated.ts), lines 1-65, 111-114; role: implementation.
- [functions/src/ai/matchingEngine.ts](https://github.com/pirinthaban/findback/blob/fc91563886e4e8da6931bc07dd025fbe235068a2/functions/src/ai/matchingEngine.ts), lines 1-12, 15-24, 158-167, 250-259, 280-289, 298-307, 537-554, 575-610; role: implementation.
- [firestore.rules](https://github.com/pirinthaban/findback/blob/fc91563886e4e8da6931bc07dd025fbe235068a2/firestore.rules), lines 1-65; role: implementation.
- [functions/package.json](https://github.com/pirinthaban/findback/blob/fc91563886e4e8da6931bc07dd025fbe235068a2/functions/package.json), lines 1-36; role: deployment.
- [test/widget_test.dart](https://github.com/pirinthaban/findback/blob/fc91563886e4e8da6931bc07dd025fbe235068a2/test/widget_test.dart), lines 1-27; role: tests.

## Runtime architecture

Flutter/Firebase application with TypeScript Cloud Functions and Firestore rules. Two function-source trees exist; reviewed functions/ paths must not be assumed to be the sole deployed bundle.

## Domain model

Items, users/roles, claims, notifications and handover codes; onClaimCreated changes an item to claimed at submission time.

## Storage model

Firestore documents, security rules and server-side Admin SDK access. Client rule protections do not constrain privileged Cloud Function writes.

## AI and matching implementation

matchingEngine supports text/geo/time/category features and cosine of precomputed image embeddings. Without embeddings but with images it returns fixed 40; missing images return zero. findMatches restricts category and takes the first 50 queried records before ranking.

## Security model

Rules derive admin/police from users.role, but the inspected users create/update rules permit owners without the comment-promised role/reputation field restrictions. This undermines trust in mutable profile roles.

## API design

Firebase triggers/callable matching plus client document APIs. onClaimCreated generates and persists a handover code before ownership review.

## Event model

Firestore onCreate triggers update status and send notifications. Sequential writes/FCM calls lack a demonstrated transactional outbox or deduplication contract in the reviewed trigger.

## Deployment model

functions/package.json deploys Firebase functions with Node 18; managed services are required. No self-hosted alternative was demonstrated by the reviewed package.

## Test coverage

The inspected Flutter test pumps a hard-coded text widget rather than the actual app or Firestore rules. No upstream tests executed; rules-emulator and webhook replay tests remain necessary.

## Strengths

Claim notifications and mobile handover interaction ideas are concrete and easy to separate from the matching implementation.

## Weaknesses

Fixed image score, absent-image zero, arbitrary first-50 candidate cap and premature claimed status/handover-code generation.

## Security concerns

Owner-writable role-bearing profiles, non-cryptographic Math.random handover codes and plaintext code storage are unsuitable recovery authorization controls.

## Scalability concerns

Category filtering may exclude misclassified true items; limiting the query before ranking can suppress relevant records. Managed backend use does not prove global matching scale.

## Licensing consequences

Preserve applicable license/copyright/NOTICE obligations for future permitted reuse. Code licensing does not establish permission for every model weight or dataset. No upstream implementation was incorporated into the core.

## Reusable components

Adapt notification/handover UX concepts after independent secure design. MIT code reuse still requires notices and security review; no source imported here.

## Do not reuse

Do not copy placeholder image scoring, owner-controlled trusted roles, claim-submission release codes or undeduplicated privileged trigger side effects.

## Architecture lessons

Release codes follow verified ownership and authorized release. Rules must enforce allowed field changes rather than rely on comments.

## Disposition

ADAPT. Applies to this declared integration scope. It is not a claim of production readiness, blanket source reuse approval or completion of later integration acceptance tests.

## ADRs triggered

Proposed investigations: private ownership evidence; trusted role assignment; deterministic release authorization; transaction/idempotency boundaries. This audit supplies evidence for later technical ADRs; it does not finalize the architecture.

## Unresolved questions

Actual deployed function tree, full claim rules, token expiry/redeem logic, storage rules and duplicate-trigger behavior need integration validation; this review grants no deployment security certification.
