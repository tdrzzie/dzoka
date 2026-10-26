# Security boundaries and threat model

Accepted design, 2026-10-26. Implementation is pending. This view follows the [current baseline](baseline.md) and ADRs [0021](../adr/0021-pii-local.md); [0022](../adr/0022-release-authorization.md); [0025](../adr/0025-tenant-security.md). The canonical audits linked by those ADRs provide pinned source evidence and scope limits. Runtime/artifact choices must pass [dependency adoption](dependency-adoption.md).

## Trust boundaries

Untrusted public clients, messaging webhooks, partner callbacks, uploads, peer nodes, model output and edge devices cross distinct authenticated interfaces. Treat all supplied IDs, URLs, roles, tenant fields, MIME labels and AI text as untrusted. Validate schemas and authorization on every command/read; parameterize persistence and prevent arbitrary URL/network access in connectors and model/media workers.

Use OIDC authentication with verified issuer/audience/algorithm and local membership/policy checks. Service identities have narrow task scopes. Browser auth/session and CSRF handling depend on the reviewed client/session design; never rely on CORS as authorization. Separate migration, application, publisher and worker credentials; secrets stay in a reviewed secret/key service, not logs or provider request text.

## Primary abuse cases

| Threat | Required control |
|---|---|
| Cross-tenant ID guessing or pooled context leak | tenant-qualified keys/FKs, repository scope, row policies and missing-context denial |
| Secret-answer enumeration or media leak | explicit projections, item/actor attempt budgets, short-lived purpose grants |
| Race/replay releases | item transaction lock, current policy/hold checks, keyed single-use tokens |
| Forged webhook/peer message | raw-body verification, audience/expiry, durable replay suppression and key revocation |
| Malicious decoder/model/prompt input | quarantine, isolated bounded decoders, approved checksummed artifacts, output schema validation |
| Unknown external effect after timeout | stable operation key and reconciliation before non-idempotent retry |

Cryptography/library pins, concrete signature canonicalization and key rotation must be reviewed before implementation. Metrics/audit redact secrets. Run dependency/security scans, adversarial tests and restore/incident drills before production.

## Acceptance

A01–A03, A06–A08, A13–A19, A24–A28; no security guarantee is inferred from architecture documents alone.

Acceptance IDs refer to [the implementation acceptance ledger](acceptance.md); none is claimed executed by this document.
