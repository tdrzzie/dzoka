# Provider and API integration contracts

Accepted design, 2026-10-26. Implementation is pending. This view follows the [current baseline](baseline.md) and ADRs [0011](../adr/0011-ai-provider-ports.md); [0013](../adr/0013-durable-events.md); [0025](../adr/0025-tenant-security.md). The canonical audits linked by those ADRs provide pinned source evidence and scope limits. Runtime/artifact choices must pass [dependency adoption](dependency-adoption.md).

## Ports and operations

Keep VectorSearchProvider, SpatialQueryProvider, AI task providers, ObjectStore, MessagingProvider, NotificationProvider, CourierProvider, PaymentProvider, TraceabilityProvider and FederationTransport behind application-owned contracts. Adapter responses carry provider reference/version, safe errors, status/freshness and correlation rather than vendor exceptions leaking into domain policy.

Mutating external calls use durable ProviderOperation identity, tenant/purpose scope, request digest, deadline, attempts and reconciliation status. Persist intent before calling; reconcile unknown effects. Retries/backoff are bounded; DLQ/replay retains operation identity. Provider credentials are tenant-scoped secret references, never user-controlled arbitrary endpoints/keys in domain commands.

## Inbound APIs

Version public and internal contracts with explicit request/response projections, pagination/limits, validation and privacy-safe errors. Core checks current identity/membership/resource scope even for internal callers. Webhook verification uses the provider's reviewed raw-body signature/authentication protocol, replay/timestamp checks and stable callback identity before mapping data. Provider tenant/role fields are lookup hints, not authority.

Frontend supports multilingual accessible low-bandwidth/PWA workflows, durable user-visible job state and safe retries. Offline drafts/custody intents synchronize with version/conflict checks; they never imply an offline unilateral release permission. Real local/self-hosted adapters are required where specified; mocks do not satisfy production provider delivery.

## Acceptance

A01–A03, A07–A08, A17–A21 and A25–A28. Contract tests plus real sandbox/local implementation evidence are required before declaring an integration complete.

Acceptance IDs refer to [the implementation acceptance ledger](acceptance.md); none is claimed executed by this document.
