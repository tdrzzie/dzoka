# 0065. Governed feedback, analytics and operational controls

- Date: 2026-11-26
- Status: Accepted; implementation verified in the bounded current-account profile

## Context

Capture minimal native claim/recovery feedback transactionally. Approval or rejection is an operational label, not automatic ground truth about a candidate. Independent adjudication and purpose-approved tenant opt-in govern learning use. Freeze dataset membership, adjudication version, declared institution/item/time split and source digests. Revisions, withdrawal, expiry and deletion invalidate future evaluation/promotion rather than silently replacing labels.

## Decision

Store versioned experiment/provider/model/configuration/evaluation identities and paired shadow results. Compute bounded ranking, coverage and probability diagnostics separately from ownership/recovery safety. Independent approval and the existing fingerprint rollout compare-and-swap lifecycle govern activation and rollback. Production requires an explicit governing tenant policy; older conformance fixtures use a named compatibility exception. Revoked or unavailable evidence fails matching closed until a valid authorized rollback. No experiment or MLflow response grants ownership or property release. Matching datasets require paired identity evidence; independent native ownership/fraud/recovery outcome audits remain a separate source-bound analytics decision.

Provide optional fixed-origin MLflow REST metadata export and OTLP/HTTP JSON telemetry using existing JDK/Spring dependencies. Export only allowlisted opaque digests, bounded metrics and fixed route categories. No raw URLs, query strings, contacts, answers, documents or actor/tenant metric labels. Bounded asynchronous telemetry must not make request success depend on a collector. Preserve durable audit separately. The implementation follows the [MLflow REST API](https://mlflow.org/docs/latest/api_reference/rest-api.html) and [OTLP specification](https://opentelemetry.io/docs/specs/otlp/); no upstream implementation is copied.

Authorized aggregate analytics use current-tenant facts with suppressed small groups and explicit denominators. Retention, restore barriers, erasure manifests and maintenance remain tenant-scoped. A restored node must replay authoritative post-backup erasures before serving that tenant; valid holds cannot be silently overridden.

Container and Helm profiles separate bootstrap/migration credentials from the non-owner API. Pin base images, use read-only non-root containers, mount secrets, provide probes and explicit network policy. Local deployment/restore/outage checks are distinct from target HA, RPO/RTO and representative quality acceptance. Existing open milestone acceptance is not closed by these manifests.

## Alternatives

Do not learn directly from a high score, an unreviewed rejection or operational approval. Do not use a mutable remote model alias as native authority. Reuse bounded original JDK adapters instead of adding a full SDK/server dependency to the core. The optional real server is reviewed separately for synthetic conformance.

## Consequences

Source/consent/label changes can suspend a governed active model and require a valid explicit rollback or newly reviewed evidence. Diagnostics may be dropped while authoritative operations remain durable. Human audits remain attestations with provenance, not automatically proven truth. Larger deployments still require target infrastructure and permitted outcomes.

## Source-audit evidence

The mandatory [registry](../source-audit/INDEX.md) remains the pre-implementation evidence gate. This change adopts no upstream implementation source. Exact runtime/server artifacts, primary license evidence and limitations are in [M11 adoption](../dependencies/m11-operations-adoption.md).

## Implementation implications

V25, the learning and operations modules, actual PostgreSQL/S3/MLflow and Compose rehearsals, and the bounded worker/backup tools implement this decision. [Execution and remaining acceptance](../testing/m11-verification-2026-11-26.md) and the [runbook](../operations/m11-runbook.md) govern claims; no production acceptance is implied by design status.
