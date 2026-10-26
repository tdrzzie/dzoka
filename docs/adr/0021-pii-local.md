# 0021: Keep PII and ownership secrets local

- Status: Accepted design; implementation and runtime dependency approval remain separate
- Date: 2026-10-26
- Prerequisite: mandatory source audit gate passed before these decisions

## Context

Federated recovery requires discoverability without exporting contact details, private challenge answers or unrestricted media.

## Decision

Separate public, operational, restricted-PII and secret-verification projections. Exchange only purpose-approved redacted evidence and opaque references; contact is relayed through the home node. Raw answers/receipts and full identifiers stay encrypted under local policy. Retention, deletion and legal-hold policies apply to every derived projection.

## Alternatives

Replicating everything then hiding fields in the UI leaves multiple uncontrolled copies.

## Consequences

Cross-node physical transfer may require explicitly authorized logistics disclosure, with recipient, purpose, expiry and audit; it is not a general PII export permission. Immutable histories retain minimal pseudonymous facts, not perpetual secret documents.

## Source-audit evidence

The specification and [AGENTS.md](../../AGENTS.md) govern invariants and required stack preferences. These static reviews inform the decision; they do not establish benchmarks or full dependency certification.

- [malak-lostandfound](../source-audit/platforms/malak-lostandfound.md) — `d5d7bfaf043396a32b40c264c647f6456cadb633`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [kwasu-lost-found-bot](../source-audit/platforms/kwasu-lost-found-bot.md) — `e4863739135594d8ce0a541be6029d0bba5ae95c`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [dpp-did](../source-audit/traceability/dpp-did.md) — `44533c27967d406f4eeb5f09c4257b506a0264d8`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [gs1-end-to-end-traceability](../source-audit/traceability/gs1-end-to-end-traceability.md) — `2.0.0 (August 2017)`; inspected ranges and limitations are in the canonical audit and evidence manifest.

## Implementation implications

Inspect API/event/log/index/federation projections for secrets; delete/expire data across indexes, media and caches and test restore suppression.

The [architecture baseline](../architecture/baseline.md) defines ownership and execution boundaries. Complete the relevant [dependency adoption review](../architecture/dependency-adoption.md) before installing runtime dependencies. Acceptance scenarios here are requirements, not tests already executed.
