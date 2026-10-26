# Recovery state-transition intelligence

Accepted design, 2026-10-26. Implementation is pending. This view follows the [current baseline](baseline.md) and ADRs [0024](../adr/0024-movement-state-models.md). The canonical audits linked by those ADRs provide pinned source evidence and scope limits. Runtime/artifact choices must pass [dependency adoption](dependency-adoption.md).

## Target and baseline

Predict next location/custodian/workflow state and time-to-transition from permitted confirmed recovery events. Start with transparent transition-frequency and elapsed-time baselines conditioned on supported institution/context; return distributions and uncertainty. Continuous pedestrian trajectories are a different target and are not imported as proof of recovery effectiveness.

Features bind an as-of time and exclude future knowledge, later custody and post-outcome evidence. Labels come from confirmed receipts/transfers/recovery outcomes, not model guesses. Keep observed/inferred/predicted graph edges separate. Sparse or unseen contexts return low coverage/unavailable rather than an overconfident destination.

## Governance

Train on purpose-approved de-identified data; split by item, institution and time to limit leakage. Evaluate ranking, probability calibration, time-to-event/censoring behavior, subgroup performance and comparison to frequency baselines. Promotion records dataset/model/config versions, evaluation artifact, reviewer and rollback. Tenant sharing is opt-in under policy and does not require exporting PII.

Predictions may prioritize authorized search or staff review. They cannot identify a thief, establish possession, change custody or authorize release. Monitoring tracks drift/coverage and can deactivate a model without losing source facts.

## Acceptance

A23, A27 and A29, including future-data leakage fixtures, sparse histories, unseen institutions, temporal shift and deterministic rollback. No recovery prediction accuracy has been measured.

Acceptance IDs refer to [the implementation acceptance ledger](acceptance.md); none is claimed executed by this document.
