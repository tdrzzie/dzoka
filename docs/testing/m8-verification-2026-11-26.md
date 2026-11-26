# M8 federation and EPCIS verification

M8 implements signed discovery and an EPCIS observation profile. [Protocol and mapping](../adr/0058-federation-wire-and-epcis-projection.md), [operating instructions](../development/federation-and-epcis.md).

## Executed local scope

- Federation tests start two Core instances with independent PostgreSQL databases and signing keys. The fixture provisions bilateral trust and tests capability negotiation, publication, private-field exclusion, signature binding, replay, audience substitution, expiry, withdrawal, key rotation, partial timeouts and circuit state. A separate signed peer response carrying an unauthorized score exercises strict response validation.
- Traceability tests create native custody observations through HTTP. The capture fixture verifies one logical projection per source and mapping, the error declaration, repository-job confirmation, uncertain-send reconciliation, revocation and tenant isolation. The mapping leaves the native record intact.
- Three emitted observation and correction documents passed schema and format validation against the official EPCIS 2.0.0 Draft 7 schema. Schema SHA-256 `cfabd8620cc4dd67a53e6aa397bc26379f8b05bb9147c8a37a60bbec69a62572`. [Official schema](https://ref.gs1.org/standards/epcis/2.0.0/epcis-json-schema.json), [schema validation receipt](artifacts/m8-verification-20261126/epcis-schema-validation.json).

| Local run | Result | Restore |
| --- | --- | --- |
| Native PostgreSQL 18.6 and Core APIs | 28 Java unit tests and 75 integration tests passed | [70 tables, 4,598 rows](artifacts/m8-verification-20261126/native-restore.json) |
| Docker PostgreSQL 18.7 with pgvector and indexed matching | 23 focused integration tests passed | [70 tables, 2,965 rows](artifacts/m8-verification-20261126/indexed-restore.json) |
| Python tools and policy checks | 240 tests passed | |

Both restores verified exact row and catalog equality, runtime-role tenant isolation and snapshot independence. The source-audit, architecture and dependency lock gates passed. The native test record covers the database and Core APIs. An earlier expanded run passed the M8 tests before it was interrupted during optional native vision verification. [Artifact hashes](artifacts/m8-verification-20261126/SHA256SUMS).

## External acceptance

The federation fixture uses loopback transport and synthetic institution identities. Deployment acceptance requires two participating institutions, protected production keys and trust exchange, verified TLS ingress and egress, and an approved publication policy. EPCIS requires an institutional repository, authorized service credentials, a scheduled projector and dispatcher, and an approved disclosure and correction policy. Cross-node physical custody transfer and secure follow-up contact remain separate workflows.
