# 0028: Seal fingerprint cohorts before atomic activation

- Status: Accepted scoped implementation profile
- Date: 2026-10-26

## Context

The first text slice appends compatible components but exposes all enabled results. Re-embedding must not silently change a serving fingerprint or mix a partially migrated cohort. A successful inference is not evidence of model quality.

## Decision

Introduce immutable rollout plans naming an explicit bounded cohort of current report revisions and exact embedding spaces. Separate `fingerprints:manage` authority can create and seal plans; inference identities cannot activate evidence. Sealing freezes per-report snapshots, component references, explicit availability and a content digest. Required spaces must have terminal success or explicit unavailable input; pending/failed work cannot count as evaluated evidence. Measure available components against all planned slots, independently of similarity. The caller explicitly supplies the minimum coverage basis points and an evaluation-record SHA-256; this is an accountable approval reference, not machine-certified accuracy.

One tenant-scoped pointer activates the whole sealed cohort atomically with compare-and-set version checks and append-only activation history. Before every promotion/rollback recheck all source revisions/lifecycle, enabled exact spaces and retained components. In managed mode, reports outside the active cohort or on newer revisions have unavailable evidence; never fall through to unapproved shadow components. Historical legacy current views remain only until the first explicit activation. Cohorts are at most 100 reports and 16 spaces, with 100 percent of required slots terminal and an explicit available-coverage threshold. Rollback can reactivate an older valid sealed cohort, never stale or erased inputs.

Tenant advisory locks precede sorted report locks, matching the existing job locking order. Read a report's selected components in one transaction. Retention deletes component references with their components; invalidated snapshots cannot be reactivated. Audit/events contain operation references and versions, never vectors or text. No release authority follows from fingerprint activation.

## Alternatives

Toggling model flags is emergency artifact authorization, not controlled migration. Automatically exposing every completed result mixes old/new cohorts. Overwriting vectors destroys provenance. Global quality/coverage claims without a defined cohort are misleading.

## Consequences

This implements bounded cohort activation, not an ANN index migration, dataset quality evaluator, multimodal provider or unlimited tenant-wide rollout scheduler. Large tenants use a future reviewed larger-cohort orchestration design rather than silently truncating coverage. Evaluation provenance must be retained by an accountable operator. No new runtime dependencies or model artifacts are adopted here.

## Source-audit evidence

- [OpenCLIP](../source-audit/retrieval/openclip.md), `2bcb5adf5090df595a78b56ca9aee7b1c70efc18`: preprocessing/artifact compatibility.
- [OpenVisualSearch](../source-audit/retrieval/openvisualsearch.md), `134dbb376bd61eac77bc388ef3531edd87fd5362`: coherent derived representation versions.
- Existing [provenance ADR](0009-model-provenance.md) and [text profile](0027-local-text-inference.md) retain authority. Independently written code; no upstream source reuse.

## Implementation implications

Verify missing/failed/pending coverage, source/space/retention revocation, wrong tenant/role, immutable pins, transactional audit rollback, concurrent activation, idempotent create/seal, explicit rollback and no shadow leakage. Exercise migrations, current HTTP views and restore with real PostgreSQL.
