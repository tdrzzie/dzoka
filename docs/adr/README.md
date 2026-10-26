# Architecture decisions

The mandatory source gate passed on 2026-10-26 before technical architecture work. ADR 0001 governs that process. ADRs 0002–0025 accept the design baseline, covering all 23 investigations required by specification section 67 plus tenant/security enforcement. They do not approve unreviewed runtime versions, certify upstreams or imply implemented functionality.

The [architecture baseline](../architecture/baseline.md) is the current implementation authority. Exact upstream revisions and audit links appear in every technical ADR. [Dependency adoption](../architecture/dependency-adoption.md) is a separate pre-installation gate; the [acceptance ledger](../architecture/acceptance.md) distinguishes executed foundation/intake/media portions from remaining system-wide requirements. ADR 0026 records the concrete private-image implementation profile.

| ADR | Decision | Status |
|---|---|---|
| 0001 | [Source evidence gate](0001-source-evidence-gate.md) | Accepted process |
| 0002 | [PostgreSQL is the transactional system of record](0002-postgresql-authority.md) | Accepted design |
| 0003 | [pgvector is the default vector backend](0003-pgvector-default.md) | Accepted design |
| 0004 | [Keep Qdrant behind the vector provider contract](0004-optional-qdrant.md) | Accepted design |
| 0005 | [Use PostGIS for spatial reasoning](0005-postgis-spatial.md) | Accepted design |
| 0006 | [Separate retrieval from scoring](0006-retrieval-scoring.md) | Accepted design |
| 0007 | [Matching and ownership are separate domains](0007-matching-ownership.md) | Accepted design |
| 0008 | [Missing evidence is unavailable](0008-missing-evidence.md) | Accepted design |
| 0009 | [Version every embedding and inference result](0009-model-provenance.md) | Accepted design |
| 0010 | [Combine global and local visual fingerprints](0010-global-local-fingerprints.md) | Accepted design |
| 0011 | [Use replaceable AI provider ports](0011-ai-provider-ports.md) | Accepted design |
| 0012 | [Run expensive AI asynchronously](0012-asynchronous-ai.md) | Accepted design |
| 0013 | [Use Kafka-compatible events with outbox and inbox](0013-durable-events.md) | Accepted design |
| 0014 | [Use a modular Java transactional core](0014-modular-core.md) | Accepted design |
| 0015 | [Separate Python inference from domain authority](0015-python-ai-boundary.md) | Accepted design |
| 0016 | [Default to permissive detector implementations](0016-detector-licensing.md) | Accepted design |
| 0017 | [Expose EPCIS through a compatibility adapter](0017-epcis-adapter.md) | Accepted design |
| 0018 | [Keep blockchain optional](0018-no-default-blockchain.md) | Accepted design |
| 0019 | [Federate independent recovery nodes](0019-federated-nodes.md) | Accepted design |
| 0020 | [Do not compare raw federated scores](0020-federation-score-semantics.md) | Accepted design |
| 0021 | [Keep PII and ownership secrets local](0021-pii-local.md) | Accepted design |
| 0022 | [Authorize release through policy or accountable review](0022-release-authorization.md) | Accepted design |
| 0023 | [Search authorized CCTV from coarse to fine](0023-cctv-coarse-fine.md) | Accepted design |
| 0024 | [Predict recovery state transitions](0024-movement-state-models.md) | Accepted design |
| 0025 | [Enforce tenancy at every boundary](0025-tenant-security.md) | Accepted design |
| 0026 | [Preserve raw intake and publish only isolated image derivatives](0026-private-image-intake.md) | Accepted implementation profile |
| 0027 | [Lease private text inference and append compatible fingerprint components](0027-local-text-inference.md) | Accepted scoped implementation profile |

Changes to invariants require a superseding ADR with evidence and consequences. New records use `NNNN-short-title.md`, status, context, decision, alternatives, consequences, source-audit evidence and implementation implications. No major requirement has been dropped.
