# CPU Shona and unmarked source proposals

[ADR 0035](../adr/0035-license-compatible-shona-and-unmarked-spans.md) and the [license review](../dependencies/shona-cpu-adoption.md) govern this separate family. It uses the unchanged 22-wheel Windows generative environment for tokenization and numeric ONNX language identification, and a pinned Linux amd64 CTranslate2/OpenBLAS CPU float32 child. The previous 23-wheel vendor runtime and all old profile identities remain unchanged and local-development-only.

## Provision and verify

Provision the exact 14 artifacts in `ai/shona-cpu-models.lock.json` at their `local` paths under `.audit-cache/shona-cpu-models`. The ten translation files are identical to the earlier reviewed Shona model files; the four LID artifacts add 424,570,582 bytes. No model-supplied Python executes. Weights, Debian archives and source archives are ignored and are not committed.

The optional offline rebuild command `python tools/shona_cpu_build.py` needs the exact three source archives under `.audit-cache/shona-production-review` and 65 Debian files in its `packages` directory, matching the build lock. It creates a new isolated build context and never resolves/downloads/installs host packages. The pinned official base must be cached. Build sources and packages stay in the builder; the final image retains runtime notices. It tags a candidate image, but does not amend the immutable runtime/profile locks. A new image requires explicit re-pinning and verification. Source acquisition/caching and image registry publication are operator steps; this tooling does not certify redistribution compliance or reproducible image identity.

```powershell
python tools/shona_cpu_runtime.py check
python tools/shona_cpu_runtime.py runtime-check
python tools/shona_cpu_runtime.py probe
python tools/shona_cpu_profile.py --tenant TENANT_UUID --profile PROFILE_UUID --profile-name CPU_SN_EN_AUTO
```

The profile command prints a disabled INSERT. An authorized operator must explicitly register/enable a selected profile. Available profiles: `CPU_EN_SN`, `CPU_SN_EN`, `CPU_EN_SN_KEEP`, `CPU_SN_EN_KEEP`, `CPU_SN_EN_AUTO`. No default model/direction, download, compute fallback or activation is installed.

Set the existing private worker variables `DZOKA_AI_CORE_URL`, `DZOKA_AI_TOKEN`, `DZOKA_AI_TENANT`, `DZOKA_AI_PROFILE`, plus `DZOKA_AI_SHONA_CPU_DIR` to the absolute model directory; invoke `python tools/shona_cpu_worker.py --once`. HTTPS is required except explicitly enabled insecure loopback development. The worker needs Docker access to start its owned bounded child; its input and model mount never enter a command argument as text. Do not expose the Docker socket to the model child. Service tokens have tenant-scoped `ai:work`, without DB/S3 credentials.

The child runs non-root, read-only, no network/capabilities/new privileges, 2 CPU, 1536 MiB, 64 PIDs and bounded temporary storage. It accepts one bounded binary token request, uses explicit CPU FLOAT32/beam4/128-input/32-output limits, and exits. A 120-second child timeout removes only its owned container; the worker has the existing 290-second external deadline and durable 300-second job lease. Hash-verified Windows read-share-only handles protect the translation artifacts during loading/inference. Only the three verified model/config/shared-vocabulary files are individually mounted at their resolved locked paths; extra cache files cannot enter the child. The driver independently rejects an effective backend other than CPU float32. The classifier graph is similarly locked and uses the exact reviewed CPU ONNX environment.

## Automatic policy and trust

Only `CPU_SN_EN_AUTO` proposes English spans on original unmarked Shona input. It scans at most 512 UTF-16 units/32 lexical words, tries contiguous whitespace-separated windows of one to three words and retains at most eight spans of 256 units. Each lexical word needs English score ≥0.6 and margin ≥0.15; complete windows need ≥0.85/≥0.2. Tokens containing digits can accompany accepted English words (for example “around 2pm”); they do not independently establish language. These are uncalibrated fixed policy thresholds. Proper names, short/ambiguous words, punctuation, contractions and unseen morphology can be missed or misclassified.

Accepted windows use collision-checked numeric sentinels and strict exact-once/order restoration. `982`, `[[` and `]]` are reserved in automatic mode; caller KEEP syntax uses its distinct explicit profile. Missing/duplicated/reordered sentinels fail the job without falling back to another translation. Missing classifier/model/image/runtime also fails closed.

Output includes `proposedSpans`, `proposalStatus`, `scannedSourceUnits`, `proposalScanTruncated`; every copied span includes exact source/output UTF-16 ranges, quote, `language: en`, raw `modelScore`, `modelMargin` and `AUTO_LANGUAGE_PROPOSAL_UNVERIFIED`. These are model proposals, not user annotations, calibrated confidence, confirmed entities or ownership evidence. Core checks exact leased source/output quotes, bounds/order, status, scores and trust independently of the worker; it does not rerun language identification. Provider/model/runtime/configuration/input hashes and timestamps remain attached through immutable jobs/results/pins.

No accepted window reports `NO_ACCEPTED_WINDOW` plus an empty list, then uses the declared whole-source translation branch. That status does not prove monolingual Shona or a mismatch. Scan coverage is separate from translation token truncation; masked token counts describe the actual model input. Raw reports remain untouched. Explicit KEEP profiles continue to label caller-marked source copying separately.

Nine authored probe rows and the real private-job tests are conformance only. The place-name corruption and “bylack” negative probes remain open quality findings. Representative/native-speaker acceptance, code-switch recall/precision, calibration, load/latency, portable host packaging, deployment registry/TLS/secrets and production operational acceptance remain required; license compatibility alone establishes none of these.
