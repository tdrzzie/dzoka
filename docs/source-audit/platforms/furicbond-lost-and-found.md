# Source audit: furicbond-lost-and-found

Status: COMPLETE

Scope: architecture-relevant static review of the listed source ranges, license evidence, deployment entry points and tests/evaluation where present. No upstream tests were executed and no upstream implementation was copied into Dzoka. Unreviewed subsystems and integration acceptance work are called out explicitly.

## Repository identity

https://github.com/FuricBond/Lost-And-Found

## Audited branch

main. The exact commit below is authoritative; branch HEAD may move.

## Exact commit SHA

ebe3974ef0c0096a521836b934661fa6e4f99897

## Audit date

2026-10-26

## License

UNRESOLVED. No root project LICENSE/COPYING in the complete pinned tree; missing licensing is a completed audit finding, not permission to copy.

## Files inspected

- [app/models/item.py](https://github.com/FuricBond/Lost-And-Found/blob/ebe3974ef0c0096a521836b934661fa6e4f99897/app/models/item.py), lines 1-65; role: implementation.
- [app/services/matching.py](https://github.com/FuricBond/Lost-And-Found/blob/ebe3974ef0c0096a521836b934661fa6e4f99897/app/services/matching.py), lines 1-12, 52-61, 65-74, 79-104, 130-238, 285-365; role: implementation.
- [app/services/image_processing.py](https://github.com/FuricBond/Lost-And-Found/blob/ebe3974ef0c0096a521836b934661fa6e4f99897/app/services/image_processing.py), lines 1-65, 85-120, 300-345, 412-445; role: implementation.
- [Dockerfile](https://github.com/FuricBond/Lost-And-Found/blob/ebe3974ef0c0096a521836b934661fa6e4f99897/Dockerfile), lines 1-58; role: deployment.
- [app/api/routes/matches.py](https://github.com/FuricBond/Lost-And-Found/blob/ebe3974ef0c0096a521836b934661fa6e4f99897/app/api/routes/matches.py), lines 1-12, 128-137, 233-242, 252-335, 411-420; role: implementation.
- [tests/test_api.py](https://github.com/FuricBond/Lost-And-Found/blob/ebe3974ef0c0096a521836b934661fa6e4f99897/tests/test_api.py), lines 1-12, 14-23, 34-43, 53-62, 66-75, 78-87; role: tests.

## Runtime architecture

FastAPI with async SQLAlchemy; actual vision processing uses lazy OpenCLIP and Ultralytics models. The inspected application is a single backend rather than a federation node.

## Domain model

Item type/status, user-owned lost/found listings, images, embeddings and bilateral match confirmations.

## Storage model

Item is an ORM entity with a string UUID explicitly intended for SQLite. Matching loads image/embedding rows and JSON vectors; a separate SQL schema exists in the tree but was not treated as proof of active PostgreSQL use.

## AI and matching implementation

matching.py combines NumPy cosine, pHash, ORB and geographic distance with fixed weights. image_processing.py constructs ViT-B-32 and an Ultralytics detector, runs ORB and serializes descriptors with pickle. This is real visual processing; candidate reranking is not restricted to an ANN shortlist in the inspected loop.

## Security model

confirm_match requires get_current_user and checks ownership of one of the two reports. Bilateral confirmation is a contact-sharing step, not private proof of legal ownership.

## API design

Versioned match confirmation POST endpoint; source/target confirmation timestamps and status updates are explicit. An unrelated authenticated user is rejected in the inspected handler.

## Event model

Notifications and match state mutations occur in the application workflow. No durable outbox, inbox or replay guarantee was established by the inspected paths.

## Deployment model

Inspected multistage Python 3.11 Dockerfile builds wheels, installs native image libraries, runs as appuser and starts uvicorn. Base tags are mutable and this is not an HA/backup validation.

## Test coverage

Inspected async HTTP tests for health, registration, duplicate-email rejection and invalid inputs. They were not executed; matcher accuracy, concurrency and tenant isolation were not measured.

## Strengths

Real complementary visual signals, image preprocessing, authenticated report-owner confirmation and a non-root runtime image.

## Weaknesses

Failed or missing local evidence becomes numeric zero in helper paths; fixed weights do not distinguish evidence coverage. Raw NumPy scoring is applied across the filtered candidate list.

## Security concerns

Pickle descriptors require trusted storage; retained EXIF can include sensitive location. No institutional tenant boundary or private ownership-policy enforcement was demonstrated.

## Scalability concerns

The ORM candidate query materializes rows, then performs distance and multi-signal scoring in Python before sorting. Top-K output does not bound all preceding work.

## Licensing consequences

No direct project-code reuse permitted by this audit. Study concepts and independently implement Dzoka contracts; seek explicit permission before any future copying.

## Reusable components

Reference global/local visual evidence and bilateral workflow concepts; independently implement licensed Dzoka contracts.

## Do not reuse

No project source reuse without a verified license. Exclude hardwired Ultralytics, untrusted pickle loading, fixed missing-evidence zeros and contact disclosure as proof of ownership.

## Architecture lessons

Keep cheap indexed retrieval before bounded local-feature reranking, and separate matching, contact relay and recovery authorization.

## Disposition

REFERENCE. Applies to this declared integration scope. It is not a claim of production readiness, blanket source reuse approval or completion of later integration acceptance tests.

## ADRs triggered

Proposed investigations: private ownership evidence; trusted role assignment; deterministic release authorization; transaction/idempotency boundaries. This audit supplies evidence for later technical ADRs; it does not finalize the architecture.

## Unresolved questions

No root license found in the pinned non-truncated tree. Full media routes, model-weight terms, migrations, provider concurrency and complete transaction boundaries need integration-specific review.
