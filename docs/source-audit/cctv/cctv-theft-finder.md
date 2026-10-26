# Source audit: cctv-theft-finder

Status: COMPLETE

Scope: Temporal search, baseline presence verifier, selected local API boundaries, evidence persistence, synthetic search tests and Python dependency deployment entry point; not detector models, all upload paths or a full privacy/security review.

Completion means bounded static source review, not a full security audit, executed tests, benchmark or production certification. No upstream implementation copied into Dzoka.

## Repository identity

https://github.com/daanishmufti/cctv-theft-finder

## Audited branch

main. Revision pinned below; this record does not assert the pin remains branch HEAD.

## Exact commit SHA

c3af22b1d6239424459a0c568f3a575de49572d6

## Audit date

2026-10-26

## License

MIT. Root LICENSE inspected; see evidence manifest. Code and model-weight licensing are separate.

## Files inspected

- [LICENSE](https://github.com/daanishmufti/cctv-theft-finder/blob/c3af22b1d6239424459a0c568f3a575de49572d6/LICENSE): lines 1–20. MIT notice verified.
- [lastseen/search.py](https://github.com/daanishmufti/cctv-theft-finder/blob/c3af22b1d6239424459a0c568f3a575de49572d6/lastseen/search.py): lines 1–130, 211–387. Day/night sampling, refinement, gap/occlusion states and optional binary strategy.
- [lastseen/presence.py](https://github.com/daanishmufti/cctv-theft-finder/blob/c3af22b1d6239424459a0c568f3a575de49572d6/lastseen/presence.py): lines 1–130. Intensity/edge ZNCC template matching, scene recognition and moved-object fallback.
- [lastseen/api.py](https://github.com/daanishmufti/cctv-theft-finder/blob/c3af22b1d6239424459a0c568f3a575de49572d6/lastseen/api.py): lines 1–95, 171–240. Local FastAPI, in-memory job registry, camera deletion and client-supplied user field.
- [lastseen/evidence.py](https://github.com/daanishmufti/cctv-theft-finder/blob/c3af22b1d6239424459a0c568f3a575de49572d6/lastseen/evidence.py): lines 1–86. Filesystem cases, SHA-256 manifests, SQLite audit table and local naive timestamps.
- [tests/test_search_logic.py](https://github.com/daanishmufti/cctv-theft-finder/blob/c3af22b1d6239424459a0c568f3a575de49572d6/tests/test_search_logic.py): lines 1–130. Synthetic world tests distinguish missing, occluded, moved and unavailable footage; frame budget assertions are synthetic.
- [requirements.txt](https://github.com/daanishmufti/cctv-theft-finder/blob/c3af22b1d6239424459a0c568f3a575de49572d6/requirements.txt): lines 1–15. Unbounded minimum dependencies; optional learned components commented out.

## Runtime architecture

Local FastAPI/dashboard process with frame grabber, recording index, per-job progress and threaded execution. Core presence matching can run without a learned detector; this is an edge algorithm reference rather than a production investigation service.

## Domain model

Camera/ROI/reference observations, samples, level brackets, search results and case evidence. Presence states include PRESENT, MISSING, UNCERTAIN and NO_FOOTAGE; no recovery ownership model.

## Storage model

Case files and JSON manifests on local disk plus SQLite audit rows. Case path resolution checks containment. Hash manifests detect content changes only if the manifest itself remains trusted; no external signature or immutable store established.

## AI and matching implementation

Baseline verifier combines normalized grayscale and edge template correlation. Search samples day/night slots, narrows hours/minutes/seconds, can calibrate scene-specific thresholds and optionally bisect a bracket. This is not automatic proof of theft or human identity.

## Security model

Reviewed local API declares no authentication dependency on camera deletion or search; SearchIn accepts a user string. Local filesystem containment is useful but insufficient for institutional authorization and per-case consent.

## API design

Unversioned /api/cameras, /api/upload, /api/search and /api/detect routes. Camera coverage, ROI and reference checks appear in reviewed search handler. In-memory dictionaries track progress; durable cancellation/restart semantics not established.

## Event model

SQLite audit actions and progress callbacks describe local processing. No durable domain event bus, outbox/inbox or federation envelope in reviewed paths.

## Deployment model

Python module/uvicorn entry point, OpenCV/PyAV/NumPy/FastAPI and local storage. Requirements use lower bounds rather than a reproducible environment lock. No production container/HA validation performed.

## Test coverage

Inspected fake-world search tests for bracket refinement, borrowed/returned objects, occlusions, calibration and synthetic frame budgets. None executed; these do not establish real-camera accuracy or performance.

## Strengths

Separates absent footage and ambiguous scenes from a missing object; preserves last-present/first-missing uncertainty; separates cheap temporal search from heavier downstream detection.

## Weaknesses

Template matching is viewpoint/ROI sensitive. Reviewed algorithm has heuristic calibration and optional bisection; bisection requires care with non-monotonic presence and gaps. Model capability cannot be inferred from optional dependency comments.

## Security concerns

Local unauthenticated destructive API, caller-supplied audit identity, unsandboxed media processing boundary and mutable local evidence manifest are unsuitable as a complete institutional chain of custody. Full upload/path traversal review remains outside scope.

## Scalability concerns

Coarse stage enumerates day/night slots across the interval, so the entire procedure is not simply O(log N). Synthetic frame counts exclude seek/decode costs. Threaded jobs and in-memory tracking require resource quotas, persistence and cancellation for production.

## Licensing consequences

MIT code can be reused with notice, but no implementation copied here. Optional detector weights and all native/media dependencies require separate licensing and security review.

## Reusable components

ADAPT uncertainty-aware coarse-to-fine investigation and explicit gap states. Implement independently behind an authorization-bound EdgeVisionNode, retaining source media locally and exporting only redacted necessary evidence.

## Do not reuse

Do not expose local camera APIs unchanged, accept audit actor identity from the request body, call disappearance proof of theft, promise logarithmic whole-archive complexity or treat unsigned local hashes as tamper-proof custody.

## Architecture lessons

Model absent observations separately from negative observations. Preserve evidence windows rather than fabricate exact disappearance times. Enforce authorization, quotas, cancellation and privacy redaction around the algorithm.

## Disposition

ADAPT. Applies only to the declared review scope; runtime adoption still requires the project-wide audit gate and relevant ADRs.

## ADRs triggered

Proposed: optional edge deployment, coarse-to-fine investigation with uncertainty, signed evidence provenance and explicit investigation authorization. No generalized surveillance or default face recognition.

## Unresolved questions

Full _confirm/_until_empty and decoder behavior, model download provenance, authorized VMS access, media sandboxing, redaction, cancellation, retention, timezone conversion and performance on non-monotonic real footage require further targeted audit and integration work.
