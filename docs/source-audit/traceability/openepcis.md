# Source audit: openepcis

Status: COMPLETE

Scope: architecture-relevant static review of the listed source ranges, license evidence, deployment entry points and tests/evaluation where present. No upstream tests were executed and no upstream implementation was copied into Dzoka. Unreviewed subsystems and integration acceptance work are called out explicitly.

## Repository identity

https://github.com/openepcis/epcis-repository-ce

## Audited branch

main. The exact commit below is authoritative; branch HEAD may move.

## Exact commit SHA

e81a857771c4c4a2d12e62b60a2dbc094551aa6e

## Audit date

2026-10-26

## License

Apache-2.0. Root LICENSE text inspected at the pinned commit: Apache-2.0. Dependency, checkpoint and dataset licenses remain separate.

## Files inspected

- [LICENSE](https://github.com/openepcis/epcis-repository-ce/blob/e81a857771c4c4a2d12e62b60a2dbc094551aa6e/LICENSE), lines 1-65; role: license.
- [modules/quarkus-capture-topology-ce/src/main/java/io/openepcis/capture/kafka/CaptureContextTopology.java](https://github.com/openepcis/epcis-repository-ce/blob/e81a857771c4c4a2d12e62b60a2dbc094551aa6e/modules/quarkus-capture-topology-ce/src/main/java/io/openepcis/capture/kafka/CaptureContextTopology.java), lines 1-65, 175-238, 320-375; role: implementation.
- [modules/openepcis-rest-api-common/src/main/java/io/openepcis/rest/api/common/EventsCaptureApi.java](https://github.com/openepcis/epcis-repository-ce/blob/e81a857771c4c4a2d12e62b60a2dbc094551aa6e/modules/openepcis-rest-api-common/src/main/java/io/openepcis/rest/api/common/EventsCaptureApi.java), lines 1-65, 250-298; role: implementation.
- [docker/docker-compose.yml](https://github.com/openepcis/epcis-repository-ce/blob/e81a857771c4c4a2d12e62b60a2dbc094551aa6e/docker/docker-compose.yml), lines 1-58; role: deployment.
- [parent/pom.xml](https://github.com/openepcis/epcis-repository-ce/blob/e81a857771c4c4a2d12e62b60a2dbc094551aa6e/parent/pom.xml), lines 1-12, 59-68, 73-82; role: deployment.
- [modules/openepcis-generated-events-capture/src/test/java/io/openepcis/test/GenerateEventsTest.java](https://github.com/openepcis/epcis-repository-ce/blob/e81a857771c4c4a2d12e62b60a2dbc094551aa6e/modules/openepcis-generated-events-capture/src/test/java/io/openepcis/test/GenerateEventsTest.java), lines 1-32; role: tests.

## Runtime architecture

Quarkus/Java EPCIS repository modules combine REST contracts with Kafka Streams capture processing and event persistence services.

## Domain model

EPCIS events/documents, capture IDs/job states, validation metadata, event hashes and invalid-event records.

## Storage model

Capture topology invokes repository/persistence services and updates job state in OpenSearch/Elasticsearch. These stores are upstream choices, not a requirement to replace Dzoka PostgreSQL.

## AI and matching implementation

No multimodal item matching in the reviewed boundary. Event parsing, SHA-256 event hashing, validation and persistence orchestration are the reusable concerns.

## Security model

REST interface accepts SecurityIdentity, validation and content-type/error contracts. That signature alone does not prove enforcement of roles, tenant grouping or safe JSON-LD/XML resolution in the deployed handlers.

## API design

Reviewed POST /events accepts JSON/XML/JSON-LD and documents payload/media errors. Adapter compatibility must target tested EPCIS event/header subsets rather than claim blanket standards conformance.

## Event model

Kafka Streams aggregates capture status, validates parsed events and invokes persistence. Rollback handling can remove events for a capture ID after invalid-event discovery; external side-effect atomicity must be evaluated separately.

## Deployment model

Compose has OpenSearch security disabled, a privileged service and plaintext Kafka listeners; this is a development example, not a safe production template. Parent Maven dependency repositories/build chain need independent provisioning review.

## Test coverage

Inspected GenerateEventsTest has launch annotations but its exit-code/response assertions are commented out. No upstream tests executed; these methods do not establish successful capture or conformance.

## Strengths

Concrete capture/job/error contracts, event hash/validation pipeline and asynchronous topology support a useful interoperability adapter.

## Weaknesses

Operational stack and capture semantics are larger than Dzoka needs; weak inspected test assertions and nontransactional external effects require caution.

## Security concerns

Do not copy disabled-security Compose defaults. Restrict document resolution, namespaces, XML parsers and public event access; do not export private verification evidence as EPCIS payload.

## Scalability concerns

Kafka topology separates ingestion/processing, but joins, aggregation state, external async persistence and rollback require failure/replay tests and actual workload measurements.

## Licensing consequences

Preserve applicable license/copyright/NOTICE obligations for future permitted reuse. Code licensing does not establish permission for every model weight or dataset. No upstream implementation was incorporated into the core.

## Reusable components

Adapt standards-facing capture/event mapping and error/provenance contracts through EPCISAdapter; adopt only a tested interoperability subset after ADR.

## Do not reuse

Do not turn every recovery object into EPCIS, replace the authoritative store just to mirror upstream or treat commented assertions as conformance tests.

## Architecture lessons

Keep Dzoka-native recovery events and map an explicit external subset; prove duplicate/rollback behavior across the actual transaction boundary.

## Disposition

ADAPT. Applies to this declared integration scope. It is not a claim of production readiness, blanket source reuse approval or completion of later integration acceptance tests.

## ADRs triggered

Proposed investigations: Dzoka-native recovery events with an explicit EPCIS adapter; optional item identifiers; no default blockchain. This audit supplies evidence for later technical ADRs; it does not finalize the architecture.

## Unresolved questions

Full EPCIS 2.x header/version support, JSON-LD context safety, deployed auth, replay idempotency, restore and dependency licensing remain concrete adapter acceptance work.
