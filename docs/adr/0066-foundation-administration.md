# 0066: Complete foundation administration without a public privilege bootstrap

- Status: Accepted implementation decision; verification recorded separately
- Date: 2026-11-27

## Context

M2 originally delivers organization create/list and manually provisioned memberships. Bounded lists, manual SQL and absent organization lifecycle leave useful local foundation work. Institutional IdP account administration remains external to the resource server.

## Decision

Preserve existing organization responses. Add an authorized cursor page and versioned rename/archive/reactivation API. Changes require current local write permission, an expected version and a durable actor/tenant-scoped idempotency key; state, minimal audit and outbox commit together. Archived organizations remain historical references.

Supply a separate operator command for tenant creation/activation and local membership grant/revocation. It uses only the explicitly provisioned `dzoka_migrator` role, never API credentials or token roles. Input is bounded JSON on stdin; credential files use the existing migration secret contract. Tenant and membership versions plus request hashes guard concurrency and retries. Append-only operator receipts record the database actor and hashed target/input references; private issuer/subject values are not printed or placed in event payloads. Database owners remain privileged; this is not tamper-proof auditing against a hostile owner.

No public route can create tenants, grant memberships or bootstrap itself. OIDC continues to authenticate, local memberships authorize, and provider account enrollment/password/MFA/session administration remains the selected IdP's responsibility. No runtime dependency is added.

## Alternatives

A public generic admin endpoint requires a separate delegation and privilege-escalation policy. Adding an unreviewed identity server or local password database would enlarge the trusted surface. Continuing manual SQL leaves commands without version/idempotency checks or consistent receipts.

## Consequences

Operators can provision/recover a tenant and revoke local access reproducibly. This command is privileged and must run separately from the API. It changes only local authorization, not upstream IdP accounts or already-running transactions. Target identity enrollment, credentials, TLS, key lifecycle and institutional approval still require real deployment evidence.

## Source-audit evidence

[ADR 0002](0002-postgresql-authority.md), [ADR 0025](0025-tenant-security.md) and the [foundation adoption record](../dependencies/foundation-adoption.md) govern authority, tenant scoping and existing JDBC/Jackson inputs. Their canonical source citations and limitations remain unchanged. The mandatory source gate passed before this implementation; no upstream code or new runtime artifact is incorporated.

## Implementation implications

Test cross-tenant and read-only denial, complete pagination, concurrent expected-version conflicts, idempotent archive/reactivation, audit/outbox rollback, operator-only provisioning/revocation, immutable receipts, migration validation and JWT key rotation. Preserve old migrations and add V26.

[Executed local verification and receipts](../testing/m2-implementation-2026-11-27.md) record results separately from target production acceptance.
