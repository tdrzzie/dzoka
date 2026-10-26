# Shadow fingerprint cohorts and controlled activation

M4's bounded cohort lifecycle initially extended the [real text provider](ai-text.md); V7 now selects explicitly pinned [local perception](perception.md) as well. [ADR 0028](../adr/0028-fingerprint-shadow-rollouts.md) records authority/tradeoffs; [ADR 0029](../adr/0029-local-perception-providers.md) adds concrete licensed providers and pins. Activation introduces no runtime dependency or quality claim. Larger-cohort/ANN migration and representative quality remain separate work.

V6/V7 now extend this lifecycle to the [licensed local perception profiles](perception.md). Optional `perceptionPins` declare exact `(reportId,componentId)` pairs, at most 64 within the source cohort; duplicates and foreign report/component/revision pins fail. With nonempty perception pins, `spaces` may be empty. All declared pins must already be retained successful results in enabled, hash-consistent profiles; no latest-result inference is performed. Available perception pins join the planned text-slot denominator. Sealing freezes exact component/profile/input/output hashes, and the same atomic promotion/rollback selects text and perception together. Purging a component cascades its selected slot while its declared UUID remains a nonreactivatable tombstone. This is bounded multimodal activation, not representative quality or ANN migration.

## Provision and plan

Apply Flyway V5 through the separate migration process. Grant an accountable operator `fingerprints:manage` in the intended tenant; it does not grant private report text, inference work or release authority. Inference workers with only `ai:work` cannot seal/promote evidence. Retain a real evaluation/approval record outside this API and supply its SHA-256. The service checks this reference's syntax, not the truth or statistical quality of an evaluation.

Create an explicit cohort with 1–100 distinct report IDs and their exact current source revisions, 1–16 distinct registered enabled text spaces, an explicit minimum available-coverage threshold in basis points and an evaluation-record digest. Sort order does not change plan identity. Caller-selected rollout UUID plus exact plan content makes creation idempotent; changing content under an existing ID conflicts. The API does not silently select the first 100 reports or claim coverage of the whole tenant.

All paths below begin `/api/v1/tenants/{tenant}/internal/fingerprints/rollouts` and require a verified bearer token plus local `fingerprints:manage` membership.

| Method/path | Behavior |
|---|---|
| `POST /` | Create an immutable plan; returns cohort counts and DRAFT state |
| `GET /active` | Read current rollout ID/version for compare-and-set; null ID/version zero before first activation |
| `GET /{id}` | Read plan/evaluation digests, state, expected/available/unavailable slot counts and coverage threshold |
| `POST /{id}/seal` | Freeze complete per-report snapshot manifests/component references; idempotent if still valid |
| `POST /{id}/activate` | Explicit `expectedVersion` and `rollback`; atomic pointer/history/audit/outbox commit or conflict |

Example plan (replace all UUIDs/revision and digest with the actual reviewed cohort/record):

```json
{
  "id":"11111111-1111-4111-8111-111111111111",
  "sources":[{"reportId":"22222222-2222-4222-8222-222222222222","revision":1}],
  "spaces":["33333333-3333-4333-8333-333333333333"],
  "minimumCoverageBps":10000,
  "evaluationSha256":"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
}
```

The example digest is synthetic documentation, not evidence. Missing/duplicate/oversized cohorts, spaces or explicit policy fields fail validation. Source revisions must still be current, unexpired and undeleted. Spaces must remain enabled; runtime cannot edit their metadata or enabled state. PostgreSQL SHARE locks use a narrow `UPDATE(enabled)` privilege with an unconditional restrictive `WITH CHECK(false)` policy; this permits locking but refuses every actual value update, including curator updates.

## Seal and measure

Each planned report/space is one expected slot. All slots must be terminal: SUCCEEDED with a retained immutable component or explicit UNAVAILABLE input. Missing/queued/processing/failed jobs do not pass sealing even when the available-coverage threshold is zero. Available coverage is `available slots / all planned slots`; it is separate from unavailable count, similarity and calibrated confidence. A 50-percent threshold does not permit pending work to disappear from the denominator. The threshold is a caller's explicit approval policy, not a hard-coded quality recommendation.

Snapshot SHA-256 binds tenant, report/revision, exact compatible space keys, normalized-input digests, explicit availability and immutable component IDs. Component references must match the same tenant/report/space through composite foreign keys. Sealing, manifests and audit/outbox append atomically; no ordinary runtime grant permits editing/deleting a frozen manifest or its slots.

## Promote and roll back

The initial activation uses `{"expectedVersion":0,"rollback":false}`. Each successful transition increments one tenant-scoped pointer version. Concurrent incompatible transitions with the same expected version have one winner. Retrying the same acknowledged transition by the same actor returns the existing version; a stale caller cannot move the pointer. Activation rechecks the entire cohort's source/space/retention validity and sealed component/manifest identity.

Use `rollback:true` with the current expected version and an older **previously activated** rollout ID. An arbitrary never-active sealed candidate cannot masquerade as rollback. Rollback performs the same current-source checks; it cannot restore vectors for revised/deleted/expired sources, erased components or disabled spaces. Tenant/report locks serialize current reads and transitions, preventing torn pointer/component projections. This bounded profile trades per-tenant read concurrency for clear consistency; it is not a throughput result.

After the first activation, current fingerprint APIs expose only the active cohort's retained, enabled, current-revision components and return `rolloutId`/`activationVersion`. Completed shadow components remain hidden. A report outside the active cohort or on a newer revision returns unavailable evidence, rather than falling through to legacy or shadow results. Before the first activation, the earlier enabled-component view is preserved for compatibility. There is no automatic return to legacy mode or automatic shadow promotion.

## Erasure, recovery and scope

Existing holds and report deletion/expiry remain authoritative. Purging a component cascades its snapshot slot reference; the retained snapshot/activation metadata is minimal history, not permission to resurrect content. Such a cohort no longer satisfies manifest/slot validation and cannot be reactivated. Restoring databases still requires the independent deletion/hold journal barrier in [local operations](../operations/local-readiness.md).

The original V5 text profile remains compatible. V7 additionally supports explicitly pinned vision/local/OCR/speech evidence and its source-pixel/processing provenance; [provider limits](perception.md) apply. Quality datasets, wider formats/languages, larger-cohort scheduling, ANN projection migration and operational deployment remain work. Real native provider output can be sealed/activated locally; synthetic lifecycle outputs test contracts only. No fingerprint transition authorizes property release.
