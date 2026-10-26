# 0001: Require verified source evidence before application implementation

- Status: Accepted (process only)
- Date: 2026-10-26

## Context

The user's specification explicitly requires source-level audits before final architecture or feature implementation. The supplied repository has extensive design and audit prose but lacks a canonical lockfile, executable software and reproducible evidence records. Some notes overlap: `la2eet` and `malak-lostandfound` describe the same upstream repository and revision.

## Decision

Preserve supplied notes. Establish one canonical registry in `docs/source-audit/source-audit.lock.json` and one audit per mandatory source. Distinguish pending, in-progress and complete review. Validate exact revisions, inspected files, evidence hashes and license findings. Application work and final architecture remain gated until all mandatory reviews are complete.

## Alternatives

- Trust supplied prose without reinspection: weak provenance and no enforceable completion boundary.
- Discard the notes and restart: loses useful prior investigation and user work.
- Implement while auditing: contradicts the explicitly requested development order.

## Consequences

The initial deliverable is audit infrastructure and verified research. This does not reduce product scope. Static review is not runtime validation, and a complete audit is not proof of upstream security or production suitability.

## Source-audit evidence

The governing evidence is the user specification sections 1, 2, 94, 98 and 100, plus `docs/source-audit/README.md` sections 2, 18 and 92. Upstream technical conclusions are excluded from this process decision and must cite their own audits later.

## Implementation implications

`python tools/audit_registry.py check` verifies structural integrity; `gate` fails until mandatory audits meet completion requirements. With all 30 mandatory reviews complete, CI runs structural checks, regression tests and the mandatory audit gate on every push and pull request. Passing this gate permits the architecture phase; it is not an application release or production-readiness check.
