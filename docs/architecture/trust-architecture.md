# Fraud, policy and accountable trust

Accepted design, 2026-10-26. Implementation is pending. This view follows the [current baseline](baseline.md) and ADRs [0007](../adr/0007-matching-ownership.md); [0022](../adr/0022-release-authorization.md); [0025](../adr/0025-tenant-security.md). The canonical audits linked by those ADRs provide pinned source evidence and scope limits. Runtime/artifact choices must pass [dependency adoption](dependency-adoption.md).

## Risk is separate from entitlement

Fraud assessments record actor/claim/item scope, signal provenance, observed interval, policy/model versions, reason codes and expiry. Duplicate attempts, inconsistent identifiers, suspicious coordination and disclosure abuse may require hold/review; absence of a fraud signal does not establish ownership. Avoid making irreversible allegations from predictive signals or CCTV gaps.

Policy is versioned and tenant-scoped. Permissions distinguish claimant, finder, custodian, reviewer, organization administrator, integration and platform operator. Public registration cannot assign privileged roles. Platform operations privileges do not automatically include verification-secret access. Overrides require explicit authority, reason and minimal audit; critical holds cannot be bypassed by an unrelated generic admin flag.

## Recovery authorization

Bind an authorization to current item/claim, verification version, permitted action/audience, policy version, approval actor and expiry. Recheck membership, holds, competing claims and physical custody while locking the item recovery record. One active release entitlement is allowed; new facts may revoke it. No AI or matching score issues authorization.

Risk and policy services can explain safe next actions without exposing detection internals or private answers. Decisions need appeals/review workflows and access-controlled audit history. Exact policy thresholds and regional legal defaults remain deployment configuration work, not invented universal rules.

## Acceptance

A13–A16, A27. Test permission revocation, administrative separation, newly introduced holds, dual-review requirements and deterministic policy reproduction.

Acceptance IDs refer to [the implementation acceptance ledger](acceptance.md); none is claimed executed by this document.
