# Source audit: trishthakur-lost-and-found-ai

Status: COMPLETE

Scope: architecture-relevant static review of the listed source ranges, license evidence, deployment entry points and tests/evaluation where present. No upstream tests were executed and no upstream implementation was copied into Dzoka. Unreviewed subsystems and integration acceptance work are called out explicitly.

## Repository identity

https://github.com/trishthakur/lost-and-found-ai

## Audited branch

main. The exact commit below is authoritative; branch HEAD may move.

## Exact commit SHA

8e5fcb6f79595c9d429679ba7868cac4a90e1319

## Audit date

2026-10-26

## License

UNRESOLVED. No root project LICENSE/COPYING in the complete pinned tree; missing licensing is a completed audit finding, not permission to copy.

## Files inspected

- [requirements.txt](https://github.com/trishthakur/lost-and-found-ai/blob/8e5fcb6f79595c9d429679ba7868cac4a90e1319/requirements.txt), lines 1-65; role: deployment.
- [lostnfound.py](https://github.com/trishthakur/lost-and-found-ai/blob/8e5fcb6f79595c9d429679ba7868cac4a90e1319/lostnfound.py), lines 1-56, 64-100, 114-123, 133-160; role: implementation.

## Runtime architecture

Single Streamlit process loads Hugging Face CLIP/PyTorch and reads/writes local tabular files.

## Domain model

Lost-item rows, serialized embeddings, login/admin flags and resolved rows with owner details.

## Storage model

CSV for records/users/embeddings, Excel for locations and local media. Resolving a case writes separate CSV files without a transaction.

## AI and matching implementation

Actual CLIPModel.from_pretrained inference generates text/image embeddings and cosine retrieval. find_best_match uses eval on serialized embedding cells, and absent images are substituted with a blank image rather than explicitly unavailable evidence.

## Security model

Passwords are unsalted SHA-256 in CSV; startup prints the entire user table. UI role checks are not a service authorization policy.

## API design

Streamlit UI paths share process/local filesystem state; no documented versioned recovery REST API in the reviewed file.

## Event model

Case resolution directly rewrites CSV state; no append-only custody events, outbox or replay handling.

## Deployment model

A large pinned requirements snapshot exists, but no production process isolation, migration or multi-user durability guarantees were established.

## Test coverage

No test source found in the pinned tree. Requirements alone are not tests; none executed.

## Strengths

Real text-to-image/image-to-image CLIP retrieval is distinguishable from placeholder AI claims.

## Weaknesses

CSV serialization, eval deserialization, unversioned embeddings and non-transactional case resolution are unsuitable infrastructure foundations.

## Security concerns

eval on data can execute code; weak password storage and full credential-table logging expose identities. Production records and credential data files were not downloaded.

## Scalability concerns

Each search iterates tabular records and reconstructs tensors; local whole-file writes have race and data-loss risks.

## Licensing consequences

No direct project-code reuse permitted by this audit. Study concepts and independently implement Dzoka contracts; seek explicit permission before any future copying.

## Reusable components

Reference the cross-modal retrieval concept only; reject this application as a platform foundation.

## Do not reuse

No project source copying without license. Exclude eval, unsalted password hashing, logging credential tables, mutable CSV authority and blank-image imputation.

## Architecture lessons

Research inference can be real while the surrounding storage and security are unsafe. Treat embeddings as typed, versioned artifacts and input as data.

## Disposition

REJECT. Applies to this declared integration scope. It is not a claim of production readiness, blanket source reuse approval or completion of later integration acceptance tests.

## ADRs triggered

Proposed investigations: private ownership evidence; trusted role assignment; deterministic release authorization; transaction/idempotency boundaries. This audit supplies evidence for later technical ADRs; it does not finalize the architecture.

## Unresolved questions

No root license. Full UI state/race behavior, model-weight terms and uploaded-media handling were not certified; no production reuse is recommended.
