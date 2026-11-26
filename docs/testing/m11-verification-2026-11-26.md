# M11 current-account verification

2026-11-26. The current-account operational implementation is delivered. **Overall M11 production acceptance remains open** because the milestone requires all-specification acceptance, including representative qualified outcomes and target infrastructure/site evidence still open or deferred in earlier milestones. This record distinguishes implemented behavior and executed checks from that production gate.

## Implemented scope

| Capability | Concrete behavior |
|---|---|
| Feedback | Native claim/recovery snapshots and participant-declared real search candidates; immutable provenance, duplicate-source rejection, independent revisioned adjudication |
| Datasets | Purpose-approved opt-in; exact source revisions; immutable manifests; paired candidate requirement; item/pair/institution/time split checks; correction, expiry, opt-out and revocation invalidate future use; valid holds preserve expired membership |
| Evaluation/registry | Exact bounded TEST cohort, paired baseline, explicit unavailable coverage, ranking/probability diagnostics, independent thresholds and approval; native sealed activation/CAS/rollback preserved |
| Production governance | Default denies model activation without an explicit governing policy; existing governed evidence is rechecked for search. Earlier milestone fixtures explicitly enable legacy compatibility; M11 tests use the production default |
| Outcome audits | Separate source-bound independent ownership/fraud/attribution attestations; claimant and original decision reviewers cannot audit their own decision; corrections append; audit cannot mutate release/custody |
| Analytics | Tenant/day-created available cohort, allowlisted categories, coarse location/time groups, known denominators and small-group suppression; ownership decision FPR, adjudicated fraud and attributed recovery remain separate from matching quality |
| MLflow | Optional real REST run/dataset/model metadata, immutable model references, native activation/rollback mirror and suspended evidence; stable reconciliation after uncertain sends and backend-bound export IDs |
| OpenTelemetry | Bounded asynchronous HTTP/JSON traces, metrics and fixed-message logs; HTTP/core spans, persisted event/job context, completed background lifecycle export; no source input, raw URL/query, contact, token or actor/tenant metric labels |
| Maintenance | Tenant/actor UTC-day report-write budgets; explicit maintenance endpoints and bounded authenticated worker; source/derived expiry and legal-hold protection |
| Restore | Protected backup integrity manifest; exact database restore; tenant barrier waits for in-flight transactions; authoritative later erasures replay before canonical/object cleanup and explicitly reviewed reopening |
| Deployment | Packaged nonroot/read-only core, separate DB/migration/app credentials, digest-pinned Compose and Helm profiles, probes, resource bounds and scoped network policy; compatibility-gated upgrades |

Implementation is in V25 and the learning/operations modules, with existing native adapters extended at their boundaries. No new Java runtime dependency is added. [ADR 0065](../adr/0065-governed-feedback-and-operational-boundaries.md), [runtime adoption](../dependencies/m11-operations-adoption.md) and the [runbook](../operations/m11-runbook.md) define the supported contracts and operation commands.

## Executed verification

Final receipts are under [artifacts/m11-verification-20261126](artifacts/m11-verification-20261126). The manifest binds implementation/configuration bytes and evidence hashes; fixture credentials, private configuration, database/object bytes and test signing keys remain outside Git.

| Check | Result and practical limit |
|---|---|
| Source/architecture/dependencies | Mandatory source audit and registry integrity, M1 preservation/architecture checks and 106 pinned Maven artifacts/licenses; no new mandatory audit bypass |
| Python | 267 checks, including real isolated video plus backup tampering, advisory pagination and operational-worker protocol checks |
| Java unit | 36 checks, including missing prediction coverage, bounded/redirected transport and diagnostic context cleanup |
| Core/S3/raster | 111 integrations pass with zero failures/errors/skips, using real private SeaweedFS and isolated raster decoding. Native model contracts are not all-profile model inference or representative quality acceptance |
| Database restore | Exact rows/catalog, ownership/RLS, role and tenant isolation across 105 tables and 5,937 rows. Same-cluster logical restore with roles preprovisioned; no production PITR/RPO/RTO claim |
| Real private objects | Restored older synthetic image bytes stay behind a closed tenant barrier; reopening fails before canonical erasure and again before acknowledged object deletion; bytes become unavailable and journal survives cleanup |
| Real MLflow | Reviewed 3.18.0 server: one run, one registered model, 11 metrics, native activation and suspended revocation mirrored; no artifact execution or private source transfer |
| OTLP schema | Actual emitted trace, metric and log JSON parsed against installed generated OpenTelemetry protobuf schemas, with the specified hexadecimal-ID normalization; not a target collector deployment/SLO claim |
| Compose | Two packaged APIs, valid signed JWT/local membership, 12 concurrent identical creates yield one mutation across replicas, anonymous denial, surviving replica, API restart, DB outage 503, DB restart with committed data unchanged, inspected isolation settings |
| Diagnostic read load | 100 authenticated reads, concurrency six, across two actual replicas; receipt records median 33.73 ms, p95 175.86 ms and p99 184.79 ms during this single diagnostic run. One synthetic organization on one host is not representative capacity acceptance |
| Helm | Strict lint/render and rejection of missing required configuration, mutable image reference, shared migration/app secret and single-replica cluster profile. No target-cluster install/CNI enforcement claimed |
| Advisory screen | OSV reported no advisories for the 106 exact Maven coordinates. This is not a source, container/JRE/Python, configuration or penetration-assessment clearance |

The gates exercise tenant/authorization failures, independent review, immutable replay, source drift, missing evidence, split leakage, governance revocation/policy-removal status, cross-tenant restore denial, quotas under race, closed restore/in-flight work, legal holds, known outcome denominators, collector/DB/MLflow failure and actual old-object cleanup. A scripted MLflow lost-reply fixture supplements the real server test; the fixture alone is not presented as a real-server result. Qualified outcome labels, institutional grouping and review evidence remain human attestations, not automatically proven facts.

Resolved failures included stale report/history column bindings, maintenance-trigger RLS context, missing tenant predicates on the new restore role (fixed with explicit predicates plus restrictive RLS and cross-tenant read/update/replay regressions), Compose quoting/mount syntax, the host's exhausted automatic network pools, disposable issuer startup timing and the difference between media state and acknowledged cleanup flags. Existing Docker networks/workloads were not pruned or reconfigured. The rehearsal selects an explicit nonoverlapping task subnet and removes only its own resources.

## Acceptance ledger

| Acceptance | Executed current-account portion | Remaining target evidence |
|---|---|---|
| A01–A03 | Native tenant/RLS/JWT denial and replay, independent outcome/learning roles, concurrent report budget and two-replica idempotency | Institution identity/provisioning/privacy policy |
| A24 | Source/derived legal holds, expired dataset membership, restore closure/replay, real private-object resurrection blocked until acknowledged cleanup | Coordinated target snapshots, protected key/config escrow, target deletion propagation and independent recovery rehearsal |
| A25 | DB/collector/MLflow failures, bounded diagnostic queue/response/batches, surviving API replica and DB recovery without replacing authority | Target broker/IdP/storage/network/disk/fencing failover and workload-specific objectives |
| A26 | Fresh V1–V25 migration, separate migrator, exact database restore, explicit replay and descriptive native registry mirror | Exact target rolling-compatible releases, multi-host failover/PITR, external-effect reconciliation after target restore |
| A27 | Actual OTLP payload privacy assertions/protobuf parsing, no-store projections and digest-only real MLflow export | Target collectors, ingress, operators, retention and deployed telemetry/security/privacy assessment |
| A29 | Source/pair/item/institution/time cohort checks, independent approval, missing-evidence coverage, known-denominator outcome corrections and fail-closed governance | Permitted representative cohorts, qualified reviewers, accepted thresholds and model/matching/recovery calibration |

## Genuine external acceptance dependencies

1. Permitted representative institutional item/claim/recovery/fraud/attribution histories and qualified independent review. Public licensed model/reference datasets and invented fixtures do not establish who owned a real item, whether a real release was safe, or whether an institutional fraud/attribution label is true.
2. The actual target identity, domain/TLS, ingress/egress, protected release/config/key storage, legal retention and operations policy. Current-account Docker works; no machine-wide migration or unattended-account requirement is imposed on this selected local profile.
3. A chosen target cluster/CNI and multi-host database/broker/object/worker infrastructure, with fencing, replication, key recovery and measured recovery/availability objectives. Local Compose restart and Helm rendering do not prove that target behavior.
4. The earlier milestones' explicit deferred representative quality, staffed/camera/federation/EPCIS and commercial-provider/site acceptance. M11 does not quietly mark those complete or resume deferred Shona/model work.
5. GitHub account billing. The latest parent-commit [Core foundation run](https://github.com/tdrzzie/dzoka/actions/runs/45869600740) and [governance run](https://github.com/tdrzzie/dzoka/actions/runs/45869600778) were prevented from starting because the account is locked for billing. No remote M11 result is claimed.

Broader optional engineering, such as additional model/client profiles, internal third-party SDK spans, portable unattended packaging and additional deployment topologies, remains separately scoped engineering rather than an external blocker. The local evidence above supports its stated current-account profile; it does not certify universal production readiness.

## Reproduce

Use the reviewed JDK 25.0.4 (Java 21 target), Maven 3.9.16, native PostgreSQL 18.6, reviewed SeaweedFS executable and current-account Docker:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-25.0.4'
$env:DZOKA_TEST_EDGE_SANDBOX='true'
python -B tools/audit_registry.py gate
python -B tools/verify_architecture.py
python -B tools/dependency_lock.py
python -B -m unittest discover -s tests -q
python -B tools/test_intake.py --native-s3 'D:\Projects\ai\dzoka\.audit-cache\seaweed-native\weed.exe' --pg-bin 'C:\Program Files\PostgreSQL\18\bin' --maven 'D:\Projects\ai\dzoka\.audit-cache\apache-maven-3.9.16\bin\mvn.cmd' --restore-drill
python -B tools/test_m11_mlflow.py --pg-bin 'C:\Program Files\PostgreSQL\18\bin' --maven 'D:\Projects\ai\dzoka\.audit-cache\apache-maven-3.9.16\bin\mvn.cmd'
python -B tools/prepare_core_image.py
python -B tools/test_m11_deployment.py --java-home 'C:\Program Files\Java\jdk-25.0.4'
```

Prepare a fresh generated image layer before repeating its extraction; do not overwrite an unknown directory. The MLflow/server and deployment harnesses use only task-owned disposable resources and synthetic credentials. The runbook covers actual secrets, policy provisioning, retention, alerts, backup/restore, native model changes and target deployment.
