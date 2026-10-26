# M1 independent revalidation

Checked on 2026-10-26 against the original specification, the actual design documents and registered source evidence. No milestone status was used as proof of completion.

## Verdict and scope

M1's defined architecture-baseline deliverables are present and substantiated. This is a completed design milestone, not a fully implemented/validated production architecture. All 24 required view files exist, but six are explicitly preserved supplied research with authority notices; their conflicts are resolved by the accepted baseline. They must not be presented as 24 independently rewritten, fully current implementation specifications.

The review found no missing required M1 view or minimum ADR investigation. It confirmed substantive decisions, alternatives, consequences and source traceability, while recognizing that the views are high-level contracts. Concrete federation cryptography/canonicalization, full client/session design, EPCIS subset, production policy defaults, SLO/RPO/RTO values and deployment/runtime artifacts remain explicit decisions before their respective implementation or production milestones.

## Reproduced evidence

| Requirement | Checked result |
|---|---|
| Specification section 66 | All 24 required architecture paths exist: six preserved views and 18 new current design views |
| Specification section 67 | All 23 minimum investigations map to ADRs 0002–0024; ADR 0025 adds tenancy/security |
| Required ADR contents | All 24 M1 technical ADRs have accepted status, context, decision, alternatives, consequences, source-audit evidence and implementation implications |
| Source traceability | All 74 cited source references in those ADRs match COMPLETE canonical audits and their exact registered pins |
| Governing baseline | Defines module/data authority, tenant and worker boundaries, raw/derived evidence, critical release transactions, event/provider failures, federation privacy and implementation sequence |
| Acceptance ledger | A01–A29 are defined once in the design table; later executed portions are recorded separately |
| Dependency adoption | A separate pre-installation gate exists; accepting a design does not approve unspecified runtime/model artifacts |
| Original preservation | All 13 supplied architecture/design bodies match the original inventory hashes; only authority prefixes were added |
| Structural/local-link checks | 53 M1 documents checked; no missing required content, unmatched citation pin, undefined acceptance reference or broken current local link found |
| Regressions | 37 Python tests passed, including five new M1 checks for valid baseline, missing view, incorrect source pin, altered supplied body and missing acceptance scenario |
| Prerequisite | Mandatory source audit gate passes; M0's separately recorded 176-artifact revalidation remains applicable to the unchanged pins |

The original preservation metadata is now reproducible from [preserved-source-manifest.json](preserved-source-manifest.json), rather than depending only on the ignored inventory cache. These are the pre-existing inventory hashes, not replacement hashes computed to approve altered bodies.

## Minimum investigation mapping

| Specification investigation | ADR |
|---|---|
| PostgreSQL system of record | [0002](../adr/0002-postgresql-authority.md) |
| Initial pgvector backend | [0003](../adr/0003-pgvector-default.md) |
| Optional Qdrant abstraction | [0004](../adr/0004-optional-qdrant.md) |
| PostGIS spatial reasoning | [0005](../adr/0005-postgis-spatial.md) |
| Retrieval separate from scoring | [0006](../adr/0006-retrieval-scoring.md) |
| Matching separate from verification | [0007](../adr/0007-matching-ownership.md) |
| Missing evidence is not mismatch | [0008](../adr/0008-missing-evidence.md) |
| Embedding/model provenance | [0009](../adr/0009-model-provenance.md) |
| Global and local fingerprints | [0010](../adr/0010-global-local-fingerprints.md) |
| AI provider abstraction | [0011](../adr/0011-ai-provider-ports.md) |
| Asynchronous processing | [0012](../adr/0012-asynchronous-ai.md) |
| Kafka-compatible events | [0013](../adr/0013-durable-events.md) |
| Modular transactional core | [0014](../adr/0014-modular-core.md) |
| Separate Python inference | [0015](../adr/0015-python-ai-boundary.md) |
| Permissive detector default | [0016](../adr/0016-detector-licensing.md) |
| EPCIS compatibility adapter | [0017](../adr/0017-epcis-adapter.md) |
| No default blockchain | [0018](../adr/0018-no-default-blockchain.md) |
| Federated nodes | [0019](../adr/0019-federated-nodes.md) |
| Incomparable federation scores | [0020](../adr/0020-federation-score-semantics.md) |
| Local PII authority | [0021](../adr/0021-pii-local.md) |
| Policy/human recovery approval | [0022](../adr/0022-release-authorization.md) |
| Coarse-to-fine CCTV | [0023](../adr/0023-cctv-coarse-fine.md) |
| Recovery state-transition models | [0024](../adr/0024-movement-state-models.md) |

## Semantic review and limitations

Read the governing baseline, all 24 M1 ADR decision/alternative/consequence sections and the current domain/security/privacy/integration views. The design consistently separates retrieval, evidence, ownership and release; keeps PostgreSQL authoritative; confines Python to derived proposals; binds evidence to compatible model/input versions; and uses durable operations/reconciliation instead of assuming exactly-once external effects. Conflicting historical microservice/Qdrant-primary/direct-AI-write descriptions are explicitly superseded in the baseline.

This is a local review, not an independent expert sign-off, formal proof, architecture benchmark or remote CI result. Automated checks establish structure, references and preservation, not semantic correctness. Mermaid source and the governing diagram were inspected as text; a fresh rendering of every historical diagram was not performed. Most A01–A29 system-wide scenarios still require later implementation tests. Section 97's full operational documents/runbooks and production validation belong to release readiness, not a claim made by M1.

The latest inspected GitHub workflows were blocked before job execution; no successful remote run of these new M1 checks is claimed. This report/checker and its tests are local changes until separately committed/published.

## Reproduce

```sh
python tools/audit_registry.py gate
python tools/verify_architecture.py
python -m unittest discover -s tests -v
```

The architecture checker reads local documents only and installs no dependencies. Its mutation tests operate on disposable copies, preserving the user's supplied originals. Human review remains necessary when decisions, requirements or source pins change.
