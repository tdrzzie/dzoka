# Recovery graph as an authorized projection

Accepted design, 2026-10-26. Implementation is pending. This view follows the [current baseline](baseline.md) and ADRs [0017](../adr/0017-epcis-adapter.md); [0024](../adr/0024-movement-state-models.md). The canonical audits linked by those ADRs provide pinned source evidence and scope limits. Runtime/artifact choices must pass [dependency adoption](dependency-adoption.md).

## Projection model

Derive a graph from confirmed tenant-authorized RecoveryEvents: nodes represent local item/case, canonical place, custodian/organization and workflow state; edges represent observed receipt, transfer, location/time or recovery outcome. Each edge retains source event identity/version, observation interval, provenance and classification. Store initial graph projections in PostgreSQL; a new graph database requires measured need and a separate ADR.

Keep observed, inferred and predicted edges explicitly distinct. An inferred location or likely next custodian cannot become a custody fact through projection refresh. Corrections append/update the authorized projection based on source correction references; source history remains the evidence authority. Cross-node references stay opaque and permission-scoped.

## Queries and learning

Support authorized case history, institutional transfer routes, repeated recovery patterns and next-location hypotheses. Minimize personal nodes; contact/verification secrets never enter general analytics. Materialization is idempotent by source event/projection version with a watermark, repair/rebuild and tombstone processing. A rebuilt graph cannot reissue courier/payment actions.

Confirmed outcome labels feed governed evaluation, with item/institution/time splits and documented consent/purpose. Query results expose uncertainty and source scope rather than unsupported certainty about a person or route.

## Acceptance

A23–A24, A27 and A29: replay, corrections, deletion, provenance traversal, isolation and prediction/observation separation.

Acceptance IDs refer to [the implementation acceptance ledger](acceptance.md); none is claimed executed by this document.
