# Deployment, durability and recovery

Accepted design, 2026-10-26. Implementation is pending. This view follows the [current baseline](baseline.md) and ADRs [0002](../adr/0002-postgresql-authority.md); [0013](../adr/0013-durable-events.md); [0014](../adr/0014-modular-core.md). The canonical audits linked by those ADRs provide pinned source evidence and scope limits. Runtime/artifact choices must pass [dependency adoption](dependency-adoption.md).

## Profiles

Small self-hosted nodes target Docker Compose with core API/worker roles, web, Python workers, PostgreSQL extensions, private object storage, Kafka-compatible events and a reviewed OIDC provider. Kubernetes/Helm supports larger deployments; development does not require it. Federation/edge, GPUs, Qdrant and Redis are optional profiles, not prerequisites for every installation. These manifests are future work. The [current local runtime/operations profile](../operations/local-readiness.md) records exact implemented runtime/authentication choices, executable real-provider verification and a database logical-restore drill; it does not approve production images or deploy the later modules.

Pin runtime/build artifacts and image digests after adoption review. Fail closed on missing production secrets, authentication, tenant policy or model configuration; test providers require explicit test profiles. Keep database, broker, storage and inference private; expose only authenticated gateways with least-privilege network policy. Migration credentials are separate from application roles; readiness does not imply inference/models or all providers are usable.

## Recovery and HA

HA planning covers core stateless replicas, PostgreSQL failover, broker durability, private object replication and inference capacity. Backup restore must preserve domain/outbox consistency and coordinate object manifests, policy/key availability and deletion tombstones. Vector/graph projections rebuild from permitted canonical state, not from arbitrary old private snapshots. Replays preserve external operation identity and do not reissue completed shipments/payments.

Choose RPO/RTO and capacity from deployment needs, then test failover/restore, schema upgrades/rollback and queue exhaustion. Bounded backpressure prevents disk/queue growth from turning a provider outage into data loss. Maintenance/reconciliation runbooks remain required before release.

## Acceptance

A24–A28 plus reproducible clean deployment, no default credentials, key rotation, restore/failover drills and observed capacity measurements.

Acceptance IDs refer to [the implementation acceptance ledger](acceptance.md); none is claimed executed by this document.
