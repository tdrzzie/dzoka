# Runtime dependency adoption gate

Architecture design is accepted in [ADRs](../adr/README.md); runtime dependency installation remains a separate obligation under [AGENTS.md](../../AGENTS.md). The architecture milestone installed no runtime dependencies. The subsequent [foundation adoption record](../dependencies/foundation-adoption.md) now approves specific M2 versions/terms, with exact artifacts in the [dependency lock](../../dependencies.lock.json). Rows below remain the checklist for any component/version not covered by that record; a generic product name is not approval.

Before each dependency enters a build or deployment: record exact version/revision and immutable artifact digest, authoritative implementation/license evidence, required transitive/NOTICE obligations, runtime compatibility, supported configuration, known advisories at review time, minimal permissions and replacement/removal plan. Pin reproducible builds and image digests; do not use `latest`. Model weights/datasets need separate grants, checksums and provenance. A code-library grant does not clear a checkpoint or dataset.

| Adoption group | Accepted design constraint | Required evidence before installation |
|---|---|---|
| JDK and build tool | Java 21+; modular core | distribution/version/license, wrapper and artifact verification, reproducible build, supported CI runtime |
| Spring Boot/security and transitive libraries | domain independent of framework; authenticated core | supported release/BOM, OAuth resource-server behavior, issuer/audience validation, license/advisory inventory |
| PostgreSQL, JDBC, migration runner | authoritative tenant-scoped data | compatible exact versions, license, transactional migrations, row-policy behavior, backup/restore |
| PostGIS, pgvector, pg_trgm | spatial, vector and lexical retrieval | extension/server compatibility and license; reviewed pgvector audit pin informs but does not choose a released binary |
| Kafka-compatible broker/client | outbox/inbox and replay | exact distribution/client terms, auth/TLS, retention/order/rebalance semantics and operational compatibility |
| OIDC provider/client | identity separate from membership | terms, key rotation, issuer/audience/algorithm behavior, tenant access mapping and account lifecycle |
| Object storage and SDK | private quarantined media | implementation/distribution terms including copyleft if applicable, encryption/access grants and deletion/versioning semantics; do not assume every S3-compatible server is permissive |
| Python/FastAPI and inference engines | isolated versioned workers | Python/library compatibility, native dependencies, locked hashes, artifact and weight terms |
| Paddle/OpenCLIP providers | permissive-code candidates | selected artifact/checkpoint terms, transitive engines, digest verification and conformance/evaluation |
| Qdrant adapter | optional deployment, required eventual adapter | image/client versions and terms, authenticated filter contract, projection repair |
| Web/PWA build | accessible multilingual low-bandwidth client | framework/toolchain terms and pins, auth/session storage, dependency inventory |
| Observability and MLflow | no sensitive telemetry; governed evaluation | exporter/backend/model registry versions, terms, PII filters and retention |
| Crypto, key/secrets storage, media scanning | fail-closed security boundaries | approved algorithms/libraries, canonicalization, key rotation, scanner/decoder sandbox and update policy |
| Messaging/courier/payment integrations | replaceable idempotent operations | service contracts/API version, webhook verification, idempotency/reconciliation and permitted data transfer |

The 30 mandatory audits remain complete. This gate covers additional concrete dependencies and artifacts at adoption time; it does not reopen or misrepresent those source reviews. Do not execute upstream projects just to inspect them. First review direct dependencies needed for the database/auth/tenant milestone; review later adapters before their implementation rather than installing every prospective service now.

An adoption record must include decision, evidence links/revisions, license obligations, pin/digest, compatibility checks, open risks and a clear approved/deferred/rejected outcome. Any unsupported/security-sensitive assumption remains open until demonstrated. No runtime version approval is implied by a product name in the architecture.
