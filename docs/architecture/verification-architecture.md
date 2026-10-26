# Private ownership verification

Accepted design, 2026-10-26. Implementation is pending. This view follows the [current baseline](baseline.md) and ADRs [0007](../adr/0007-matching-ownership.md); [0021](../adr/0021-pii-local.md); [0022](../adr/0022-release-authorization.md). The canonical audits linked by those ADRs provide pinned source evidence and scope limits. Runtime/artifact choices must pass [dependency adoption](dependency-adoption.md).

## Evidence and challenges

A verification session binds tenant, claim, item, evidence snapshot, policy version and expiration. Evaluate knowledge, protected identifiers, prior photos, documents, institution records and representative entitlement without equating account identity or QR possession with ownership. Matching only supplies a possible item reference.

Challenge generation selects evidence-backed facts not already publicly disclosed. Deterministic validators enforce source provenance, disclosure budget, independence, difficulty and safe wording; an LLM suggestion cannot bypass them. Attempts are limited across sessions by actor/item as well as session, with fraud controls that do not expose the expected answer. Encrypted evidence and keyed exact-match digests replace raw/unkeyed low-entropy answer storage as appropriate.

## Decision boundary

Outcomes are passed, failed, inconclusive or review-required, with reason codes, evidence references and reproducible policy/configuration versions. Insufficient evidence remains inconclusive rather than becoming a fabricated pass/fail. Conflicting claimants/holds route to accountable review. Human decisions record actor, authority and justification; sensitive cases can require dual control under policy.

A successful assessment is an input to current release authorization, not a custody transfer. Revoked/expired assessments, policy changes or new conflicts invalidate pending authorization as required. Claimants receive privacy-safe next actions, never finder secrets or internal fraud details.

## Acceptance

A13–A16 and A27; verify challenge leakage, repeated guessing, stale sessions, role spoofing, representative entitlement and concurrent claims.

Acceptance IDs refer to [the implementation acceptance ledger](acceptance.md); none is claimed executed by this document.
