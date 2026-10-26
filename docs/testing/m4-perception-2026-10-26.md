# M4 local perception and multimodal activation verification

Executed 2026-10-26. This records actual bounded local provider execution and invariant tests, not universal perception quality, production readiness or a remote CI result.

## Final native execution

`python tools/verify_local.py --pg-bin 'C:\Program Files\PostgreSQL\18\bin' --maven 'D:\Projects\ai\dzoka\.audit-cache\apache-maven-3.9.16\bin\mvn.cmd'` passed all **eight** phases:

| Evidence | Observed result |
|---|---|
| Mandatory source gate | PASS, all 30 bounded mandatory reviews remain complete |
| M1 architecture/preservation | PASS, 53 checked documents and 13 unchanged supplied bodies |
| Python tests | 47 pass, including seven new profile/source/codec/private-client regressions |
| Dependency integrity | Existing 106 JAR/POM/license records and 21-wheel native graph unchanged/verified |
| Existing text model | Exact model/tokenizer/source integrity and real native inference pass |
| Perception artifacts | All six downloaded weight/dictionary files independently match locked size/SHA-256; runtime/provider/format source and profile registry identity pass |
| Java/full-provider acceptance | 8 unit checks and 60 integration tests, zero failures/errors/skips |
| Restore | 34 tables/1,498 synthetic rows plus checked catalog/owner/grant/policy/constraint equality; restored missing-context/tenant isolation and snapshot independence pass |

Host JDK: Oracle 25.0.4 compiling Java 21 bytecode. PostgreSQL: native 18.6. S3 and image decoder: previously reviewed digest-pinned disposable Docker servers/processes. Native inference: reviewed Windows amd64 CPython 3.14.7, ONNX Runtime CPU 1.31.0/NumPy runtime. No new wheel or Maven dependency was installed. No copied upstream implementation, pickle, remote inference or runtime model download was used.

Final ignored run: `.test-tmp/local-verification-20261026T191621665828Z/result.json`, started `2026-10-26T19:16:21.666017+00:00`, completed `2026-10-26T19:18:52.714439+00:00`. Code/lock input SHA-256: `4ea07d1fe17adc44f807db39f1ce7e435ea6da1a00e1e91fe3d778569659a909`. Restore evidence: `.test-tmp/foundation-74a2c3e6-1383-4619-b8e9-f61bbe307621/restore-result.json`. Diagnostics are ignored synthetic data, not committed backups or real-person recordings.

## Independent Docker PostgreSQL route

The same final code/lock digest independently passed `python tools/verify_local.py` using the digest-pinned Linux PostgreSQL container, private S3/decoder containers and the same Windows Java/native-Python consumers. All eight phases, 8 unit/60 integration tests, 47 Python tests and the 34-table/1,498-row restore passed again, without failures/errors/skips. This verifies the supported Docker database route, **not** a Linux native-model runtime or remote CI.

Ignored run: `.test-tmp/local-verification-20261026T192150092592Z/result.json`, started `2026-10-26T19:21:50.093709+00:00`, completed `2026-10-26T19:25:21.062864+00:00`. Restore: `.test-tmp/restore-3b34554a-80df-40a6-8aac-7781d8b3db76.json`. Both native and Docker runs stopped/removed their own services afterward; existing installations/data were not used as test targets.

## Provider execution, not substituted outputs

[PerceptionProviderIT](../../src/test/java/org/dzoka/PerceptionProviderIT.java) uploads an authored label image, verifies raw/quarantined input is refused, sanitizes it in the actual JRE sandbox, then runs all four private Python providers through actual JWT/HTTP claim/input/completion. The consumer has only the scoped service token/configuration, no DB/S3 credentials. Real outputs are appended, explicitly pinned into a perception-only cohort, sealed and atomically selected into ItemFingerprint.

- Vision: the fixed licensed ResNet graph yields a nonconstant, normalized 1,000-value signature. This is its own profile, not CLIP-compatible or a validated instance-retrieval embedding.
- Local features: independently authored Harris/patch extraction yields nonempty, normalized descriptors in bounded source-pixel boxes. It is explicitly classical, not a learned-feature substitute.
- OCR: actual official Paddle detector/recognizer/dictionary returns `BLUE BAG 742` on the synthetic printed label.
- Speech: the installed Windows speech synthesizer creates an authored PCM fixture for “blue travel bag number seven four two.” Actual English CTC returned `BLUE TRAVEL BAG NUMBER SEVEN FOR TWO`; the four/for error is preserved. The test checks an observed stable phrase, not an invented perfect transcript or multilingual score.

Fixtures demonstrate conformance only. No representative dataset, WER/accuracy, Recall@K, calibration, load, latency or recovery-safety benchmark is claimed. Empty detections/transcripts remain empty successful observations; absent/failed/unsupported evidence is never fabricated as a mismatch.

## Security, lifecycle and failure evidence

[PerceptionJobsIT](../../src/test/java/org/dzoka/PerceptionJobsIT.java) has eight real PostgreSQL/JWT/HTTP scenarios: concurrent natural idempotency; tenant/actor/private raw lease isolation; public WAV authorization and chunked oversize rejection; source/lease/revision revocation; typed output validation and exact replay; three-attempt/deadline bounds; shared text/perception tenant quota; raw-audio reservation/actual hold-aware erasure; injected request/completion outbox rollback; runtime profile immutability and stored-manifest/key tamper rejection; explicit pin activation and source/erasure invalidation. Related schema/codec/domain tests cover malformed headers, CRC, bounded decompression, nonfinite output, coordinate/sample bounds, changed provider source and corrupt artifacts rejected before native imports.

V6 raw audio is exactly preserved in lease-scoped restricted rows; it is not placed in ordinary job status, events or logs. V7 plans pin exact component IDs and profile/input/output hashes; a completed job alone does not activate evidence. Curator authority is separate from inference authority. Source changes, disabled/inconsistent profiles, media revocation or actual erasure cannot be repaired by blindly rolling back an old cohort. Existing text/intake/media/foundation regressions remain included. Restored data is never exposed to an API/publisher/worker by the drill.

## Reproduce and limits

Follow [perception setup/profiles](../development/perception.md) and [artifact adoption](../dependencies/perception-adoption.md), then use the command above or the supported Docker PostgreSQL route `python tools/verify_local.py`. The default portable CI command runs lifecycle contracts and explicitly omits native text/perception unless flags request them; this is not Linux native-model approval.

Supported scope: private sanitized PNG input, strict short PCM16 mono 16kHz WAV, named ResNet/classical/Paddle/English-CTC profiles, 100-report/16-text-space cohorts plus 64 explicit perception pins. Wider formats/languages, representative quality/evaluation, learned local/joint-image-text alternatives where warranted, larger-cohort/ANN migration, native deployment memory/CPU/egress enforcement, complete redistribution notices/advisory scans and independent backup deletion-journal/replica recovery remain separate work. Permitted representative datasets and institutional production requirements are genuine external inputs; a cloud inference account/GPU is not required for the delivered local implementation.
