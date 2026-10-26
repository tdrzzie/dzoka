# Privacy-aware observability and evaluation

Accepted design, 2026-10-26. Implementation is pending. This view follows the [current baseline](baseline.md) and ADRs [0013](../adr/0013-durable-events.md); [0021](../adr/0021-pii-local.md). The canonical audits linked by those ADRs provide pinned source evidence and scope limits. Runtime/artifact choices must pass [dependency adoption](dependency-adoption.md).

## Operational signals

Propagate correlation/causation and trace context across intake, outbox, jobs, provider operations and callbacks. Structured logs contain safe operation references, reason/status codes, timings and version references, not raw evidence, answers, tokens, contacts or documents. Security audit is a controlled durable domain record, distinct from sampled diagnostic telemetry.

Measure queue age/backlog, outbox lag, lease expiry/retries, DLQ rate, provider latency/error/cost, index watermark, retrieval route coverage, verification review load and reconciliation age. Avoid unbounded item/user/tenant identifiers as metric labels; provide authorized drill-down through restricted records. Alerts need explicit runbook actions and privacy-safe context.

## Evaluation and model registry

Version datasets, consent/purpose, splits, feature/model/configuration, calibration and outcome labels. MLflow is the requested registry/evaluation integration pending concrete adoption review. Promotion uses approved evidence, not a training run finishing successfully. Separate candidate recall/ranking, confidence calibration, verification safety and confirmed recovery outcomes; none substitutes for the others.

Define service objectives and alert thresholds from actual deployment requirements and measurements. No latency, uptime or accuracy target is claimed achieved. Operator dashboards must respect tenant and secret-access scopes.

## Acceptance

A24–A29, telemetry export inspection, trace continuity during retries and an outage exercise proving alerts are actionable without exposing private data.

Acceptance IDs refer to [the implementation acceptance ledger](acceptance.md); none is claimed executed by this document.
