# M4 joint embeddings and region-model verification

Executed 2026-10-26 on the reviewed Windows amd64 CPython 3.14.7 CPU runtime, Oracle JDK 25.0.4 emitting Java 21 bytecode, native PostgreSQL 18.6 and digest-pinned Docker S3/decoder processes. This verifies the bounded implementation, not representative quality or production readiness. Earlier [perception evidence](m4-perception-2026-10-26.md) remains historical evidence for unchanged profiles.

## Final run

`python tools/verify_local.py --pg-bin 'C:\Program Files\PostgreSQL\18\bin' --maven 'D:\Projects\ai\dzoka\.audit-cache\apache-maven-3.9.16\bin\mvn.cmd'` passed all **nine** mandatory phases:

| Check | Actual result |
|---|---|
| Mandatory source gate | PASS; 30 complete bounded reviews |
| Architecture/preservation | PASS; 53 documents, 13 supplied bodies unchanged |
| Python | 50 tests pass, including paired-space identity, changed source and corrupt-model rejection before native imports |
| Java/native dependencies | Existing 106 JAR/POM/license records and 21-wheel runtime pass; no new runtime dependency |
| Earlier model profiles | Existing text native conformance and six perception artifact/dictionary checks pass; real earlier providers remain in integration |
| New artifacts | Eight downloaded files, 397,294,177 bytes, independently match exact locked size/SHA-256; implementation/runtime identities and generated profile registry match |
| Java checks | 9 unit tests and 64 integration tests; zero failures/errors/skips |
| Database restore | Exact rows/catalog match across 34 tables/1,640 synthetic rows; restored runtime-role/tenant isolation and source-snapshot independence pass |

Ignored result: `.test-tmp/local-verification-20261026T195512850414Z/result.json`, started `2026-10-26T19:55:12.851176+00:00`, finished `2026-10-26T20:00:25.662919+00:00`. Unchanged code/lock input SHA-256: `5afcc17ebc1517b00b34e630cc9c134a1e257be022bfe3177ad4076f9e29d6d9`. Restore: `.test-tmp/foundation-48ae1546-0144-4b70-b8ec-81faf88b968e/restore-result.json`. Task-owned services were stopped/removed. Model binaries, inputs, private synthetic logs and database backups stay ignored.

This final expanded slice was verified on the **native PostgreSQL route**. Earlier Docker PostgreSQL evidence belongs to the preceding slice; it is not presented as a rerun of these new profiles. No Linux native-model approval, remote GitHub CI or cross-JDK result is claimed.

## Actual inference and protected activation

[VisionProviderIT](../../src/test/java/org/dzoka/VisionProviderIT.java) uploads an authored `BLUE BAG 742` label image, refuses quarantine inference and sanitizes it through the actual isolated raster child. Four real Python invocations then claim private service-token HTTP leases, validate exact input/profile identity, run the approved graphs and publish immutable outputs through core validation. The consumer has no DB/S3 credentials. All four start SHADOW; an independently authorized curator explicitly pins their IDs, seals the cohort and activates them into `ItemFingerprint` with separate modality coverage.

- Jina v1 image and text graphs yield distinct finite normalized 768-value vectors with the same recomputed shared space key. Each profile binds both quantized encoders, tokenizer/configuration, declared processing and runtime/source identity. The text encoder consumes the current stored report revision. These vectors are distinct from MiniLM 384D and ResNet 1000D; no automatic compatibility or retrieval calibration is inferred.
- Actual SSD inference on the authored label yields no boxes above the declared 0.5 threshold. The graph's actual input/output names (`inputs`, `detection_boxes`, `detection_classes`, `detection_scores`, `num_detections`) differ from the card's older example; the adapter uses the inspected export names. Positive/invalid source-pixel boxes are tested separately with synthetic domain fixtures. No object-detection accuracy claim follows from empty-output conformance.
- Actual FCN inference yields a 64×64 all-background grid encoded as `[[0,4096]]` for that label. The core validates canonical run lengths, vocabulary and complete grid coverage. This is semantic segmentation, not instance segmentation or a demonstrated foreground-quality result.

[VisionJobsIT](../../src/test/java/org/dzoka/VisionJobsIT.java) exercises concurrent natural text-job idempotency, authorized HTTP enqueue, wrong actor/tenant/worker denial, direct unleased-text RLS, private exact UTF-8 input, source revision and lease revocation, exact completion replay, disabled profiles, incompatible same-dimension space keys and independently rehashed manifests with broken shared definitions. [VisionEvidenceTest](../../src/test/java/org/dzoka/VisionEvidenceTest.java) rejects out-of-bounds geometry, invalid labels, incomplete masks and noncanonical/zero runs. Existing perception/rollout tests continue to cover shared quotas/retries/deadlines, atomic outbox, retention/holds/actual erasure, pin invalidation and rollback.

## Corrected verification attempts

An initial direct database harness invocation used `--full` without the S3-provisioning parent and failed four media-class setups for missing `DZOKA_TEST_S3_ENDPOINT`; it was not a passing full run. The first correctly provisioned combined run passed 63 integration tests but hit S3 HTTP 500 on the last fixture's first upload, before new inference. Increasing the disposable SeaweedFS allocation from four to sixteen bounded volumes eliminated that failure; the final complete run above passes. The harness now retains ignored private S3 diagnostics before removing its container. Earlier failed run: `.test-tmp/local-verification-20261026T195050920431Z/result.json`. No test was skipped or replaced with mock inference to obtain the final result.

## Remaining work and external prerequisites

[Adoption](../dependencies/joint-vision-adoption.md), [ADR 0030](../adr/0030-joint-vision-and-region-models.md) and [operator guide](../development/joint-vision.md) define exact scope. The Model Zoo cards' Apache-2.0 metadata and MIT body declarations are both retained; complete redistribution notices remain required before shipping model binaries. No model binaries or upstream implementation were copied into the core.

Representative object/instance and multilingual evaluation requires permitted datasets and accountable acceptance criteria. Production identity/hosts/TLS/secrets, remote CI account availability and institutional privacy/retention policy are external inputs. Cross-modal retrieval/ANN indexing, larger cohorts, richer attributes/LLM/translation/reranking, learned local features, broader formats/languages, instance segmentation if required by later workflows, native OS resource/egress isolation, advisory/notice tooling and recovery/deployment automation remain **local engineering**, not cloud/GPU blockers. No accuracy, latency, throughput, ownership or property-release claim follows from this run. Full M4 and production acceptance remain open.
