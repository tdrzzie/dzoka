# Architecture documentation

The [accepted baseline](baseline.md) and [24 technical ADRs](../adr/README.md) are the current design authority following the completed source audit. The six supplied views retain their complete original bodies with an authority notice; new views cover the remaining documentation areas required by specification section 66. The original root diagram is historical and does not override the baseline.

| View | Status |
|---|---|
| [system context](system-context.md) | Supplied detail retained; current resolution in baseline |
| [container architecture](container-architecture.md) | Supplied detail retained; current resolution in baseline |
| [component architecture](component-architecture.md) | Supplied detail retained; current resolution in baseline |
| [domain architecture](domain-architecture.md) | Supplied detail retained; current resolution in baseline |
| [data architecture](data-architecture.md) | Supplied detail retained; current resolution in baseline |
| [event architecture](event-architecture.md) | Supplied detail retained; current resolution in baseline |
| [AI execution and governance](ai-architecture.md) | Current design view |
| [Retrieval, evidence scoring and confidence](matching-architecture.md) | Current design view |
| [Versioned ItemFingerprint contract](item-fingerprint.md) | Current design view |
| [Private ownership verification](verification-architecture.md) | Current design view |
| [Fraud, policy and accountable trust](trust-architecture.md) | Current design view |
| [Security boundaries and threat model](security-architecture.md) | Current design view |
| [Privacy, retention and disclosure](privacy-architecture.md) | Current design view |
| [Tenant isolation contract](multi-tenancy.md) | Current design view |
| [Independent node federation](federation-architecture.md) | Current design view |
| [Native recovery events and EPCIS compatibility](traceability-architecture.md) | Current design view |
| [Authorized coarse-to-fine edge vision](cctv-edge-architecture.md) | Current design view |
| [Recovery graph as an authorized projection](recovery-graph.md) | Current design view |
| [Recovery state-transition intelligence](movement-intelligence.md) | Current design view |
| [Courier orchestration and reconciliation](logistics-architecture.md) | Current design view |
| [Rewards and ledger-based escrow](rewards-architecture.md) | Current design view |
| [Privacy-aware observability and evaluation](observability.md) | Current design view |
| [Deployment, durability and recovery](deployment-architecture.md) | Current design view |
| [Provider and API integration contracts](integration-architecture.md) | Current design view |

The baseline reconciles store/deployment authority, module ownership, worker permissions, model/evidence semantics and release transactions. [Acceptance scenarios](acceptance.md), [dependency adoption](dependency-adoption.md) and [milestones](milestones.md) define the work needed to turn design into validated software. No deployment, provider implementation, accuracy or production-readiness claim follows from these documents.
