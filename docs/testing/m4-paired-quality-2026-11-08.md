# M4 paired quality and target host assessment

The paired evaluator binds two exact profile keys to a plan digest retained separately from the observations. It checks the same input and reference for both profiles and distinguishes complete, truncated and unavailable outputs. Per-split and per-slice coverage is explicit. It runs in the protected management release and grants no promotion or recovery authority. [Protocol](../development/quality-protocols.md)

## Shona-to-English decision

Eight fixed one-based FLORES-200 `devtest` rows (65–72) were selected before either profile ran. The existing publisher archive and both aligned member hashes were reverified. These are general-domain CC BY-SA 4.0 references with unknown training overlap. The selection is separate from previously reported rows and does not establish training disjointness or coverage of lost-property terminology and code-switching. Raw inputs, references and model outputs remain in the private evaluation directory. [Data review](../dependencies/public-reference-data-review.md)

The diagnostic screen was fixed before inference and required eight complete cases, median character error rate (CER) at or below 25%, and no individual complete case above 60% CER. This screen can reject a candidate. Semantic promotion additionally requires evidence about meaning, protected spans and omissions in the intended Dzoka setting.

| Pinned profile | Complete | Truncated | Unavailable | Median CER on its complete cases | Worst CER on its complete cases |
|---|---:|---:|---:|---:|---:|
| CPU Shona baseline | 4/8 | 4/8 | 0/8 | 44.4% | 61.0% |
| Refined Marian candidate | 8/8 | 0/8 | 0/8 | 56.9% | 76.7% |

On the four complete matched pairs, median CER was 44.4% for CPU Shona and 50.7% for refined Marian. All four baseline truncations reached its 32-token output limit, with no input truncation. Refined Marian reached EOS in every case. Both profiles failed the declared screen and remain unapproved for general Shona-to-English rollout. A longer CPU output budget would require a versioned candidate and a fresh comparison. A change to coverage alone would leave meaning and fidelity to verify.

| Identity | SHA-256 |
|---|---|
| FLORES-200 archive | `b8b0b76783024b85797e5cc75064eb83fc5288b41e9654dabc7be6ae944011f6` |
| Frozen comparison plan | `b49bf6662f88a6be51af1ace8fee6ce5bb0fa765b2a9e5a7f74e3492ecfceae0` |
| Dataset and screen evidence | `54a149d902c786c580d7e33ac45ade3148a56e3deb83496639dc71013ad77692` |
| Private result | `aaf74b95a983c9c851bf80c736f60725787c80db328020f9b958f4545af38ee4` |
| Management release receipt | `7694c427deb58fcd5ac4c0f1df9ec6793bb1b1daae96c5bf5c1530b9aa88139e` |
| Existing model release receipt | `2382b945fbe3532f6253246d969a41d7d51623b2cc737630c64a09f49738b027` |

The private result identifies CPU Shona `f9a8e470c4151c583f6ee73c9fbc2b84b6c9b558545ef7e54c9319ce87e63e44` and refined Marian `de2dab2a29aaccbde609ad79ad7c264760eb2d84316bcc3ab66b7204ce4dfbc0`. It retains the projection source hash, observation file identities and individual scores. All 16 calls used the existing exact model bundle via the native isolated broker. The source profiles and model artifacts retain their November 7 identities.

### CPU output limit diagnostic

A separate offline build reused the reviewed CPU source archives, packages, base image and model files. The diagnostic driver changed only the decoding and output frame token ceilings from 32 to 96. The existing driver SHA-256 is `d862903068ce9b75ce8512035a49192b2aa2205cb295a2069ea938fb93f06a3f`. The diagnostic driver SHA-256 is `826e2cfac95e389e48c2b4f5a087d9a4c451e0d6e53c388e44685dd480620dfa` and its image ID is `sha256:150a67f23db3f7838ec1958286f3c1dc559d593a3428d8d5083289647ee772dd`. It ran in network-disabled, resource-bounded containers with read-only model mounts and was kept outside the provider registry.

| FLORES slice | Limit | Complete | Truncated | Unavailable | Median CER on complete cases |
|---|---:|---:|---:|---:|---:|
| Development, rows 65–72 | 32 | 4/8 | 4/8 | 0/8 | 44.4% |
| Development, rows 65–72 | 96 | 7/8 | 0/8 | 1/8 | 57.0% |
| Additional, rows 81–88 | 32 | 5/8 | 3/8 | 0/8 | 48.3% |
| Additional, rows 81–88 | 96 | 8/8 | 0/8 | 0/8 | 52.6% |

On the five additional cases both decoders completed, their output text was identical. The 96-token decoder's worst additional CER was 64.2%. It cleared the coverage screen and missed both error tolerances. The unavailable development case was a Docker invocation timeout. A separate same-image replay completed on both decoders at 57.9% CER. The original timeout remains in the results. The diagnostic established higher completion coverage. The quality result leaves the 96-token build outside rollout.

The frozen plan SHA-256 is `044ff3b1b94d0a7526ceefe7902fefcc73c5dda9be839799d2455f52f3ad0d62`. The original result is `2a27aff7fcfcb21488ce369af8f2d0cfb8fb233e9366f9c22a9d545b8e4d6954`. The focused replay result is `15cdbf3df7332230c01067b892093da0c43c9aa4f0f40ecab64e7f122cb45127`. Private records retain exact source, build, image and observation identities.

## Verification and installation status

The mandatory source and M1 architecture gates passed. All 169 Python tests and six native private protocol checks passed. The native checks include a real isolated embedding, paired coverage, exclusive output and refusal of a broadened input ACL. Focused result `.test-tmp/quality-protocols-ce79b5f8581a49e3820a64c61755dfb1/result.json` has SHA-256 `a3fad46908d2b49e07d4e19bc500edbdcd4c240ee858e7267481150380276b1b`. The first focused test invocation used the restricted tool sandbox and Windows denied access to its private temporary directories. The normal-token rerun passed all cases.

The first full run on source SHA-256 `e92b5afc657507ea778c345ced5aed5b9b47180a7777f91bed553aa1c9d754bb` passed the audit, architecture and Python gates, native service recovery, all 53 isolated profiles and broker death/integrity. It stopped in managed operations when the isolated translation diagnostic reported a protocol connection failure. Integration and restore did not run. The failed receipt `.test-tmp/local-verification-20261108T060913701190Z/result.json` has SHA-256 `a7d295b64875cefbc269e039bf3ae892774562be4cad260711a064e929e88f64`. The old and new management release inventories differ only in the evaluator, its native verification harness and the management lock. The translation/broker/runtime source and exact model release are identical.

The focused managed-operations retry passed all ten native cases on the same releases. Receipt `.test-tmp/managed-operations-ac90897dc78c41f8bdeb5f67b950a7ef/result.json` has SHA-256 `d20ba7392ab078128e2958d9beea51f87e38744ffac02db93dbc3f5d1b3f772f`. The unchanged-source full retry stopped earlier when the isolated English LLM case reported a protocol connection failure. The translation and reranking profiles passed in that run. Receipt `.test-tmp/local-verification-20261108T063954432576Z/result.json` has SHA-256 `4ad3d05a0108d0b340221219f72e1f20b9fab7a8642633c082ca325b164e4dfe`. The cause of the connection failures remains unconfirmed.

The focused isolated LLM retry also passed with confirmed cleanup. Receipt `.test-tmp/model-broker-019e4b379e5a453ba0a75bddb1455f5a/result.json` has SHA-256 `ac9a27cd574be38f6b66d5718f3301e35db8c2ddd73934030a364afe8705e8a2`. The third full run passed all 23 prerequisites, including the previously failed checks and the new evaluator. Java unit tests passed 24/24. Integration recorded 94 passes and one error when a CPU Shona job had no persisted result after one protected broker poll. Restore did not run. The failed receipt `.test-tmp/local-verification-20261108T070741023657Z/result.json` has SHA-256 `0f85d3851a5cb9c74115ef35535f48d60c1c8f370d5dd355eae08f4243bab9b3`. At that point, the November 7 run was the latest full pass.

The five exact CPU Shona inputs from the affected test each produced nonempty `UNVERIFIED_TRANSLATION` evidence through their pinned native isolated profiles on a direct replay. The replay did not exercise service polling or HTTP persistence. Receipt `.test-tmp/m4-paired-shona-integration-fixtures.log` has SHA-256 `731830a85193743f23b7bfd68e293fdcde94d27cb213fa2a749eecde1881962d`. The direct replay could not resolve the service-to-core failure.

The integration fixture formerly ended the protected service after a single poll. It now observes the real supervisor and waits for a successful completion or up to three actual polls under existing backoff. One CPU Shona job deliberately fails after claim to verify that the real failure endpoint requeues it, a later poll claims it, and actual inference persists its private result. The test checks the attempt count and logs the sanitized error type on failure. Provider identities, retries, authorization, leases and production configuration retain their prior behavior.

All 24 local verification phases passed on source SHA-256 `cfb1ed87a98a4bde906f0a4d709ef813633e84a59b708c093d88e271005e84aa`. The run had 169 Python tests, 24 Java unit tests, 95 integration tests, all 53 isolated profiles and six native private protocol checks. The restore matched all 35 tables and 3,189 rows with catalog, tenant and snapshot checks.

| Passing receipt | SHA-256 |
|---|---|
| `.test-tmp/local-verification-20261108T090402987594Z/result.json` | `bb7f56988fb4777b765e3dc25b015e6d6a489b756577652f19d4350c23eff9b2` |
| `.test-tmp/model-broker-ff7c7814fafd40bbbd2a30cdfd8b5ea3/result.json` | `8c09d422bd3269fe27d35b17e3250745dc0862fc8a4e4a2c88a990568b16a85a` |
| `.test-tmp/broker-http-8d8f3089-4392-4d14-a744-145ae080b5e2.log` | `9700de5d7c189e96ca3ab4c6ffcc3edf66632d7e244aef694d9905019b559143` |
| `.test-tmp/foundation-7aaf1057-eb6c-4151-8790-6d8490228988/restore-result.json` | `0296472cff32ad9414574f27aacf0addb49a1574749e4f79a5ae05da4c17f9b9` |

SCM preflight returned Win32 5 for creation access and Win32 1060 for service absence. The current Windows account is a member of local Administrators and has a filtered token with the Administrators group deny-only. Use the current account as elevated installer/operator and qualify a dedicated worker identity. The local candidate `NT SERVICE\DzokaWorkers` has a deterministic SID confirmed by `sc.exe showsid` and remains inactive. The observed Docker daemon uses the interactive WSL2 backend. Installed-service lifecycle, access to the daemon and core egress under the service token, and boot before interactive login still require administrator-backed target execution. [Worker service deployment](../development/worker-service.md)
