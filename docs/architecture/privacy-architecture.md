# Privacy, retention and disclosure

Accepted design, 2026-10-26. Implementation is pending. This view follows the [current baseline](baseline.md) and ADRs [0021](../adr/0021-pii-local.md); [0025](../adr/0025-tenant-security.md). The canonical audits linked by those ADRs provide pinned source evidence and scope limits. Runtime/artifact choices must pass [dependency adoption](dependency-adoption.md).

## Data classes

Public discovery is a deliberately limited projection; operational records support staff workflows; restricted-PII stores contacts/identity; secret-verification stores private facts, answers and documents. Derived OCR/embeddings/crops may be sensitive even when not directly readable. Do not publish whole entities or treat vectors as anonymized by default.

Anonymous relay hides contacts until explicit policy-approved disclosure. Model requests use permitted sanitized inputs and tenant-specific cloud policy. Federation exports opaque references and minimized evidence/rank metadata; precise recovery locations and private identifiers stay local unless a separately authorized logistics disclosure needs them. Every disclosure records purpose, recipient, scope, expiration and responsible authority.

## Lifecycle

Raw input is preserved against silent normalization overwrite, not retained forever. Tenant retention categories, legal holds, consent/purpose and regional obligations govern source evidence, answers, media derivatives, vectors, caches, events and feedback. Append-only history uses minimal pseudonymous facts and references; encrypted secrets can expire independently. Corrections append factual history without perpetuating unnecessary private bytes.

Deletion/expiry creates durable tombstones, revokes media/job grants, removes projections and schedules object/version cleanup. Restore procedures reapply deletion/hold policy before making restored data searchable. Backups expire under governed policy; a restored worker/event must not resurrect erased evidence. Model training inclusion/exclusion and artifact retirement require separate data-governance decisions.

## Acceptance

A19, A24, A27 and A29. Inspect every serialized projection and restore path; no real PII is needed in tests. Exact durations/legal requirements are deployment decisions requiring current review.

Acceptance IDs refer to [the implementation acceptance ledger](acceptance.md); none is claimed executed by this document.
