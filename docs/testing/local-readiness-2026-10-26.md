# Local readiness execution record

Date: 2026-10-26. Scope: implemented tenant/organization foundation, report/private-image intake and private asynchronous text-fingerprint slice. This is actual execution evidence, not an assertion that future milestones or production readiness are complete. No application behavior or runtime dependency was added in this verification work.

## Results

| Check | Result |
|---|---|
| Mandatory source audit gate | PASS, 30 complete mandatory reviews |
| Architecture requirements/pins/preservation | PASS, 53 documents, 24 views, 24 baseline ADRs, 74 source citations, 29 scenarios, 13 original bodies |
| Python regressions | 40 passed, including three new restore/evidence checks |
| Java artifact integrity | All 106 locked artifacts/POM/license records passed |
| Native inference artifact checks | 21 locked wheels and reviewed ONNX/tokenizer/source profile passed |
| Real native-model probe | PASS, nonconstant 384-dimensional synthetic outputs; no quality/accuracy claim |
| Full Docker PG/S3/decoder + native Python model | 6 Java unit and 44 integration tests; zero failures/errors/skips |
| Full native PG + Docker S3/decoder + native Python model | Independently 6 unit and 44 integration tests; zero failures/errors/skips |
| Logical restore, each database route | 20 tables, 729 synthetic rows; all complete row sets/migration history and checked catalog metadata identical |
| Restored access controls | Missing context and wrong actor denied; tenant A/B separate; runtime role non-owner/non-bypass |
| Snapshot independence | Subsequent source mutation does not change restored rows |
| Cleanup | Isolated native cluster stopped; task-owned Docker services removed |

PostgreSQL was 18.6; the host Oracle JDK was 25.0.4 compiling Java 21 bytecode; native Python was Windows amd64 CPython 3.14.7. S3/decoder and container PostgreSQL use the immutable images already recorded in their adoption reviews. No new runtime dependency was installed or approved. Docker harness image pulls remain restricted to those reviewed digests.

## Execution identity

The local runner hashes code, tests, provider/tool sources, POM/dependency/model/runtime locks and AGENTS.md before and after its phases. It fails if these inputs change. This is a code/lock digest, not a Git commit or hash of every document. Ignored diagnostics are available at:

- Docker run: `.test-tmp/local-verification-20261026T181442753067Z/result.json`, completed 18:16:09 UTC; code/lock SHA-256 `7be245a2d1ff3f27c83b85ea9195662465a0a4c3eb2f8052a5804c6d0b3bd428`.
- Docker restore: `.test-tmp/restore-bfe413a9-c5a9-4988-822c-4d8f2e87e4df.json`.
- Final native run: `.test-tmp/local-verification-20261026T181810938283Z/result.json`, completed 18:19:27 UTC; code/lock SHA-256 `ccc3ee81aa3635522d38f6489305ee93876cf160e0e4f88c3ec9c3dd43cf120d`.
- Native restore: `.test-tmp/foundation-7110a5fe-cd1d-42b1-b87c-68a9a5ae839c/restore-result.json`.

The new drill initially exposed Windows default-encoding and native missing-port harness defects. Both were fixed and complete runs repeated before recording acceptance. The initial default-port attempt failed authentication before any query ran. Successful native commands explicitly select the harness-owned cluster port. Historical failed runs remain diagnostics and are not counted as passing phases.

## Reproduce

After the reviewed prerequisites in [local operations](../operations/local-readiness.md):

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-25.0.4'
python tools/verify_local.py
python tools/verify_local.py --pg-bin 'C:\Program Files\PostgreSQL\18\bin' --maven 'D:\Projects\ai\dzoka\.audit-cache\apache-maven-3.9.16\bin\mvn.cmd'
```

The two commands are independent supported database routes, not a requirement to run both repeatedly after documentation-only edits. The default CI suite remains portable and excludes native inference; its new restore flag does not approve a Linux model runtime. Remote CI execution is still blocked/unverified.

## Limits

The restore is custom-format logical `pg_dump`/`pg_restore` into a fresh database in the same disposable cluster, with roles already present. It compares table/function owners and grants, column grants, row-policy definitions/flags and constraints alongside every row. It does not test physical/PITR recovery, a new cluster/role/key disaster, object backups, replica erasure, independent deletion/hold journal replay, live broker/provider replay, cross-host failover or the release of restored data to traffic. No restored API, publisher or worker is started. All restored synthetic data remains in the isolated test environment until teardown.

The [external prerequisites and unfinished local work](../../STATUS.md) remain explicit. The [13 runbook procedures](../runbooks/README.md) are scoped operational guidance, not claims that their future runtime controls or all production drills have been delivered. All 100 original specification sections remain in scope.
