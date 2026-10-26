# Source audit: ai-lost-and-found

Status: COMPLETE

Scope: architecture-relevant static review of the listed source ranges, license evidence, deployment entry points and tests/evaluation where present. No upstream tests were executed and no upstream implementation was copied into Dzoka. Unreviewed subsystems and integration acceptance work are called out explicitly.

## Repository identity

https://github.com/dhanushshetty40/AI-lost-and-found

## Audited branch

main. The exact commit below is authoritative; branch HEAD may move.

## Exact commit SHA

577edc3b492f62ac8707fc950230bc04a354e9d8

## Audit date

2026-10-26

## License

UNRESOLVED. No root project LICENSE/COPYING in the complete pinned tree; missing licensing is a completed audit finding, not permission to copy.

## Files inspected

- [backend/app/routers/claims.py](https://github.com/dhanushshetty40/AI-lost-and-found/blob/577edc3b492f62ac8707fc950230bc04a354e9d8/backend/app/routers/claims.py), lines 1-12, 27-36, 86-95, 225-244; role: implementation.
- [backend/app/services/ai_matcher.py](https://github.com/dhanushshetty40/AI-lost-and-found/blob/577edc3b492f62ac8707fc950230bc04a354e9d8/backend/app/services/ai_matcher.py), lines 1-12, 14-23, 29-38, 50-59, 61-70, 75-84, 95-165, 185-243; role: implementation.
- [backend/app/models/report.py](https://github.com/dhanushshetty40/AI-lost-and-found/blob/577edc3b492f62ac8707fc950230bc04a354e9d8/backend/app/models/report.py), lines 1-65; role: implementation.
- [backend/requirements.txt](https://github.com/dhanushshetty40/AI-lost-and-found/blob/577edc3b492f62ac8707fc950230bc04a354e9d8/backend/requirements.txt), lines 1-11; role: deployment.
- [backend/test_phase2.py](https://github.com/dhanushshetty40/AI-lost-and-found/blob/577edc3b492f62ac8707fc950230bc04a354e9d8/backend/test_phase2.py), lines 1-65; role: tests.
- [backend/app/routers/auth.py](https://github.com/dhanushshetty40/AI-lost-and-found/blob/577edc3b492f62ac8707fc950230bc04a354e9d8/backend/app/routers/auth.py), lines 1-65; role: implementation.
- [backend/app/schemas/user.py](https://github.com/dhanushshetty40/AI-lost-and-found/blob/577edc3b492f62ac8707fc950230bc04a354e9d8/backend/app/schemas/user.py), lines 1-34; role: implementation.

## Runtime architecture

FastAPI/SQLAlchemy backend with JWT/password helper calls and optional sklearn text similarity. Requirements provide an application environment but no evidence of a production AI service.

## Domain model

Report and Claim records include description, brand/color, image URL, verification question/answer, score, review state and a contact_released flag.

## Storage model

Relational ORM tables persist claim answers as text and mutable status/contact flags. Incident date/time are strings and timestamps use naive datetime.utcnow defaults.

## AI and matching implementation

ai_matcher.py uses TF-IDF or difflib for text. calculate_image_similarity never opens images: different nonempty URLs receive a text-derived score constrained to 75-95. Generated explanation nevertheless describes embedding/color evaluation. Confidence is a score threshold.

## Security model

Public register accepts UserCreate.role; the schema exposes arbitrary role text, and auth.py explicitly accepts ADMIN. This is a source-level self-registration privilege-assignment defect, not a remotely tested exploit.

## API design

Authenticated user/claim endpoints and finder/admin-style approval paths exist. Claim approval sets contact_released and report status; verification similarity compares answers to descriptive report text.

## Event model

In-process notification helper calls accompany state mutations. No durable event bus/outbox/inbox guarantee established in reviewed files.

## Deployment model

backend/requirements.txt lists lower-bounded FastAPI/SQLAlchemy/JWT dependencies. Inspected phase test creates/drops schema; that must not become production migration practice.

## Test coverage

Inspected reporting TestClient setup, seeded synthetic logins and report-creation assertions. Tests mutate a local database and were not executed; no model accuracy or production authorization result established.

## Strengths

Reports and claims are explicit entities and readable tests expose intended flows; useful as a negative example of capability claims.

## Weaknesses

Visual score is fabricated from text, missing date/location inputs receive heuristic baselines, and confidence is conflated with aggregate score.

## Security concerns

Client-selectable ADMIN role, plaintext claim-answer storage and contact release require redesign. Do not rely on an AI label or seeded demos as security evidence.

## Scalability concerns

Pairwise text fitting and synchronous score computation lack an indexed retrieval stage in the inspected implementation.

## Licensing consequences

No direct project-code reuse permitted by this audit. Study concepts and independently implement Dzoka contracts; seek explicit permission before any future copying.

## Reusable components

Reference the documented failure modes and independently specified report/claim workflow only; reject as the platform or matcher foundation.

## Do not reuse

No source copying without license clarification. Exclude pseudo-image scores, invented embedding explanations, role assignment from registration and confidence-by-score thresholds.

## Architecture lessons

Validate provider capabilities through actual inference calls; enforce privileged role assignment server-side; explanations must reflect evidence actually computed.

## Disposition

REJECT. Applies to this declared integration scope. It is not a claim of production readiness, blanket source reuse approval or completion of later integration acceptance tests.

## ADRs triggered

Proposed investigations: private ownership evidence; trusted role assignment; deterministic release authorization; transaction/idempotency boundaries. This audit supplies evidence for later technical ADRs; it does not finalize the architecture.

## Unresolved questions

No root license found. Other media/lifecycle modules and all dependency/model terms were not comprehensively audited; they are not implicitly approved.
