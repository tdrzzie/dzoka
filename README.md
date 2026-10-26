# Dzoka

Temporary internal codename for the Federated Multimodal AI Lost Property Recovery Network.

The target is a complete self-hostable, multi-tenant recovery platform: multilingual intake, replaceable AI providers, evidence-based retrieval and matching, private ownership verification, authorized release, custody, messaging, federation, logistics, rewards, and institutional operations.

**Current state: source audits, architecture, the Java/PostgreSQL foundation, report/private-image intake and the first asynchronous text-fingerprint slice are implemented. The complete recovery product is not production-ready.** See [STATUS.md](STATUS.md) for verified progress and remaining production requirements.

## Start here

- [Run and test the foundation](docs/development/foundation.md)
- [Report intake, private images and workers](docs/development/intake-media.md)
- [Private AI jobs and real local text fingerprints](docs/development/ai-text.md)
- [Dependency adoption and artifact lock](docs/dependencies/README.md)
- [Agent and contributor constraints](AGENTS.md)
- [Source audit index](docs/source-audit/INDEX.md)
- [Audit procedure](docs/source-audit/README.md)
- [Accepted architecture baseline](docs/architecture/baseline.md)
- [Architecture decision records](docs/adr/README.md)
- [Delivery milestones and acceptance](docs/architecture/milestones.md)
- [Supplied target architecture](docs/ARCHITECTURE.md)
- [Original project specification](docs/dzoka.md)

The accepted baseline resolves conflicts in preserved supplied design documents. Current scope includes tenant-owned organizations/intake, immutable raw revisions, private image processing, durable AI jobs, a separately locked local text provider and versioned fingerprint components, with membership checks, migrations, idempotency and outbox/inbox. Other modalities, matching and recovery remain targets. Historical research remains in `docs/platforms/` and `docs/findings/`. Current audit evidence is in `docs/source-audit/`; accepted decisions cite completed reviews and exact revisions. New runtime dependencies still require concrete adoption review before installation.

## Reproducible repository checks

Python 3.11+ is sufficient; the audit tooling has no third-party dependencies.

```sh
python tools/audit_registry.py check
python -m unittest discover -s tests -v
python tools/audit_registry.py gate
```

`check` validates registry integrity, paths, evidence hashes and documentation metadata. `gate` additionally fails while any mandatory source audit is incomplete. A passing structural check does not mean the production platform or the source audits are complete.

The mandatory audit gate passes. Build and test the foundation with a reviewed Java 21+ JDK and Docker:

```sh
python tools/maven.py dependency:resolve
python tools/dependency_lock.py
python tools/test_intake.py
```

The full harness creates isolated PostgreSQL/private S3 services and runs the Docker raster sandbox, then stops its own services. [Native verification options](docs/development/intake-media.md) and [migration/API instructions](docs/development/foundation.md) are available. Deployment credentials/IdP configuration have no default fallback. Full product setup, production Compose/Helm, benchmarks and recovery workflows remain outstanding.
