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

The accepted baseline resolves conflicts in preserved supplied design documents. Current scope includes tenant-owned intake, immutable raw revisions, private image/short-PCM input, durable AI jobs, reviewed local text/vision/local-feature/OCR/English-speech [providers](docs/development/perception.md), [joint image/text, detection and semantic segmentation](docs/development/joint-vision.md), [grounded attributes, translation and private reranking](docs/development/language.md), [private general drafts and directed multilingual profiles](docs/development/generative.md), [language quality alternatives and Shona→English](docs/development/language-quality.md), [local bidirectional Shona and explicit copied spans](docs/development/shona.md), and [bounded multimodal cohort activation/rollback](docs/development/fingerprint-rollouts.md), with membership checks, migrations, idempotency and outbox/inbox. Wider quality/format/language coverage, matching and recovery remain targets. Historical research stays in `docs/platforms/` and `docs/findings/`; current source evidence is in `docs/source-audit/`. Accepted decisions cite complete bounded reviews/pins; new runtime/artifact adoption remains separate.

## Reproducible repository checks

Python 3.11+ is sufficient; the audit tooling has no third-party dependencies.

```sh
python tools/audit_registry.py check
python -m unittest discover -s tests -v
python tools/audit_registry.py gate
python tools/verify_architecture.py
```

`check` validates registry integrity, paths, evidence hashes and documentation metadata. `gate` additionally fails while any mandatory source audit is incomplete. A passing structural check does not mean the production platform or the source audits are complete.

The mandatory audit gate passes. Build and test the foundation with a reviewed Java 21+ JDK and Docker:

```sh
python tools/maven.py dependency:resolve
python tools/dependency_lock.py
python tools/test_intake.py
```

The full harness creates isolated PostgreSQL/private S3 services and runs the Docker raster sandbox, then stops its own services. [Native verification options](docs/development/intake-media.md) and [migration/API instructions](docs/development/foundation.md) are available. Deployment credentials/IdP configuration have no default fallback. Full product setup, production Compose/Helm, benchmarks and recovery workflows remain outstanding.

For all current local gates, reviewed native text/perception providers, real integration and logical restore, run `python tools/verify_local.py` after provisioning the reviewed Windows CPython and [perception artifacts](docs/development/perception.md), [joint/region artifacts](docs/development/joint-vision.md) [language artifacts](docs/development/language.md) and the separate [generative runtime/artifacts](docs/development/generative.md) plus [quality alternatives](docs/development/language-quality.md) and the separate [local-development Shona runtime](docs/development/shona.md). It explicitly requires `--with-ai`/`--with-perception`/`--with-vision`/`--with-language`/`--with-generative`/`--with-quality`/`--with-shona` in its underlying harness, fails on missing phases, and does not install/download model providers or substitute mocks. Reviewed test-container image pulls remain explicit in the harness. The [local operational profile](docs/operations/local-readiness.md) separates external requirements from unfinished engineering and links [incident procedures](docs/runbooks/README.md). Passing it verifies the implemented bounded profiles, not later milestones or production readiness.

The separate [CPU Shona runtime](docs/development/shona-cpu.md) adds source-built OpenBLAS English/Shona inference and unmarked English-span proposals. Full local verification also requires its pinned image/artifacts and `--with-shona-cpu`; license-compatible inference does not establish production or representative language readiness.
