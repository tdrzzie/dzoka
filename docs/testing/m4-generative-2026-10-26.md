# M4 general drafts and broader language verification

Executed 2026-10-26 on Windows amd64 CPython 3.14.7 CPU, Oracle JDK 25.0.4 emitting Java 21 bytecode, native PostgreSQL 18.6 and digest-pinned Docker S3/decoder fixtures. This records executable bounded providers and invariant conformance, with known quality failures. It does not establish representative accuracy, full M4, Linux native-model adoption, successful remote CI or production readiness.

## Final complete run

`python tools/verify_local.py --pg-bin 'C:\Program Files\PostgreSQL\18\bin' --maven 'D:\Projects\ai\dzoka\.audit-cache\apache-maven-3.9.16\bin\mvn.cmd'` passed all **twelve** required phases.

| Evidence | Executed result |
|---|---|
| Mandatory source gate | PASS; all thirty bounded mandatory reviews complete |
| Architecture/preservation | PASS; 53 baseline documents, thirteen supplied bodies unchanged |
| Python | 59 tests; zero failures/errors |
| Java | 15 unit / 74 integration tests; zero failures/errors/skips |
| Artifact/runtime integrity | 106 JAR/POM/license records, unchanged 21-wheel baseline plus separate reviewed 22-wheel runtime; all old/new model identities and installed native bytes/notices verified |
| Real providers | Earlier text/perception/joint/region/language providers plus five new generative HTTP-worker jobs; explicit snapshot selection and private inputs |
| Logical restore | 35 tables / 2,056 synthetic rows exactly equal, with catalog/runtime-role/two-tenant/source-snapshot independence checks |

Started `2026-10-26T21:35:32.264140+00:00`, finished `2026-10-26T21:44:33.197235+00:00`. Code/lock input SHA-256 remained `ff7041742f49f18a552ff8e366a2edc9e9603678b5fbdd7ffbedd44fafd2acbe` before/after the complete run. Ignored evidence: `.test-tmp/local-verification-20261026T213532263546Z/result.json`; restore `.test-tmp/foundation-ddf7c198-b9e4-40bb-a3db-e2040074e2bb/restore-result.json`. Task-owned PostgreSQL/S3 fixtures stopped/removed. No mock substitution or skipped provider became passing evidence.

## Native adoption and observed outputs

[Adoption](../dependencies/generative-adoption.md) records the official Apache-2.0 SmolLM2-135M-Instruct export and explicitly MIT M2M100 export/base, exact revisions/hashes and configuration/tokenizer inspection. Thirteen artifacts (902,012,169 bytes) are independently verified; no publisher Python/Jinja/conversion/pickle code executes. A separate environment reuses the exact reviewed 21 base wheels and adds one reviewed SentencePiece wheel. Five bundled copyright/license texts are preserved and hash-checked. Existing runtime locks, model profiles and 106 Maven records are unchanged. Binary distribution hashes and bounded native source/license review do not establish a reproducible rebuild or advisory/security certification.

`python tools/generative_runtime.py probe --all-profiles` executed all thirteen profiles, independently loading native graphs and verifying all installed runtime bytes before imports. Ignored output: `.test-tmp/generative-all-probes.log`. The LLM probe additionally checked actual chat special-token boundaries against a hostile token-string instruction/report and denied opening its held model for append without performing a write.

For the English bag sentence and independently authored localized equivalents:

| Direction | Observed output / limitation |
|---|---|
| English LLM draft | `I lost a black bag.`; 74 input / 7 output tokens, EOS |
| English→French | `J’ai perdu une valise noire.`; bag narrows to suitcase |
| French→English | `I lost a black bag.` |
| English→German | `Ich habe eine schwarze Tasche verloren.` |
| German→English | `I lost a black pocket.`; wrong noun |
| English→Spanish | `Perdieron una bolsa negra.`; changes person/number |
| Spanish→English | `I lost a black pocket.`; wrong noun |
| English→Afrikaans | `Ek het 'n swart tas verloor.`; no native-speaker acceptance |
| Afrikaans→English | `I lost a black bag.` |
| English→Zulu | `Ngaba umkhosi emaphakathi.`; divergent result, no native-speaker acceptance |
| Zulu→English | `I have a black shade.`; meaning error |
| English→Xhosa | `Ngaba umzoko lula.`; divergent result, no native-speaker acceptance |
| Xhosa→English | `I was given a black shade.`; meaning error |

All named translation probes ended at EOS with zero unknown input tokens; zero unknown tokens plainly does not imply semantic correctness. Results require `UNVERIFIED_TRANSLATION` and retain original source language/text. No unsupported Shona or other vocabulary direction is enabled. A separate SmolLM2 general prompt asking what a backpack is produced repetition and reached its 64-token bound, correctly reporting OUTPUT_LIMIT/outputTruncated. Earlier development's overly restrictive system instruction caused a refusal; the adopted helpful draft instruction produced the bag description above. These are retained negative observations, not aggregate quality scores or native-speaker validation.

The full service test executes five real worker jobs through authenticated HTTP leases: one English general draft plus Spanish→English, English→German, English→Zulu and Zulu→English. It checks typed output/provenance, exact source preservation, four explicit translation pins, sealed activation/translation availability and exclusion of the LLM from selected intrinsic components. It asserts nonblank output and controls, not correctness of the translations above. The twelve direct direction probes are additional evidence, not twelve more integration tests.

## Contract verification and remaining gates

Four new portable real-PostgreSQL scenarios exercise exact prompt/raw retention, concurrent natural idempotency, private lease/RLS/tenant boundaries, 300-second leases, strict body/direction validation, shared pending/storage quotas, stale-lease retry exhaustion, injected outbox rollback, holds/actual erasure and query-dependent selection denial. Three new Java unit tests cover typed draft/translation limits/trust/direction/coverage rejection and intrinsic LLM exclusion. Five new Python tests cover layered profile/source/runtime integrity, bounded/escaped formatting, corrupt artifacts and installed native-byte/notice tampering. Native inference tests are separately identified; static primary license/config review is not described as upstream test execution.

The independent development portable run passed 61 integration tests and 15 Java unit tests. The final complete twelve-phase run below governs the final code. The existing full harness exercises every earlier real provider, S3 and actual decoder isolation, plus native models and logical restore. Task model files, raw inputs and diagnostics remain ignored; no real PII or credential is published.

Representative permitted multilingual/recovery/query data, native-speaker validation, institutional retention/release policies and production hosts/IdP are external prerequisites. Model improvement, a separately licensed Shona alternative, broader language/format/context support, evaluation tooling, host CPU/memory/filesystem/egress enforcement and remaining product milestones are unfinished local engineering. Remote CI is unverified; earlier account-level Actions startup failures remain historical evidence. No new Docker PostgreSQL or Linux native-inference result is claimed. Restore is a same-cluster logical drill, not object backup, independent tombstone replay, HA/PITR or approved RPO/RTO.

Additional actual-model long-input probes passed at 512 LLM input / 64 output tokens and 128 multilingual input / 32 output tokens. The LLM reported instruction/context truncation separately; both reported input/output truncation and OUTPUT_LIMIT. Diagnostic: `.test-tmp/generative-bound-probe.log`. An initial probe instruction exceeded 1,024 characters and was correctly rejected before generation; corrected within-character-bound input exercised token truncation. These are supplementary probes, not additional automated test counts.
