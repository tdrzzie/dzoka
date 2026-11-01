# M4 model-worker broker and egress isolation — 2026-11-01

[ADR 0040](../adr/0040-brokered-model-processes.md) and the [operator guide](../development/model-broker.md) govern this extension. The thirty-source audit gate passed before application changes. No runtime dependency, wheel, weight or container image was added. Earlier inference/model/runtime/profile bytes remain preserved; `ai/service.lock.json` evolves as an orchestration lock. New isolated profile identities include source and adopted CPython executable/DLL/stdlib pins.

## Implementation and acceptance scope

| Boundary | Implemented behavior | Evidence required in the final gate |
|---|---|---|
| All seven supported worker families | 34 appended profiles: text 1, perception 4, joint/region vision 4, language 3, generative 13, quality 4, CPU Shona 5 | Actual native inference for every registry entry, exact profile identity and confirmed cleanup |
| Trusted broker/core authority | Fixed claim/input/complete/fail routes; tenant service token stays outside the model; selected profile and exact input digest/length checked | Real HTTP/JWT jobs, wrong-tenant denial, private results, raw-source preservation and existing core retry/revocation/schema/retention tests |
| Model process | Actual package SID/zero capabilities/no loopback exemption and atomic Job membership checked before resume; explicit three-handle/credential-free transfer; outbound native capability denial | Real native execution; independent reachable-listener/private-file controls; malformed/cross-invocation/oversize/trailing/native-capability protocol faults |
| CPU Shona native operation | Only one token operation for the configured artifact group through the existing exact-owner Docker Session; no child-selected image/mount/command/daemon authority | All five CPU profiles, daemon isolation/resource inspection and confirmed removal; restart reconciliation |
| Private release and provenance | Every bundle byte has a receipt; CPython/wheels/source/model identity pinned; notices retained; old and new text/joint spaces incompatible | Source/registry/runtime integrity, same-length corruption/missing/extra/receipt faults and old-inference byte preservation |
| Service schema 3 | Checked account/MIC, protected release/control files, exact credentials, disjoint private tenant state, recover-before-poll, capped backoff and stop | Actual protected foreground service/core jobs plus service ordering, credential/overlap and unresolved-state regressions |
| Broker death/recovery | Kill-on-close model ownership; durable package-grant intent; inactive exact-SID grant revocation and profile reconciliation | Native deadline, malformed request, live-owner refusal, broker death and idempotent orphan recovery |

The model receives an opaque invocation ID, exact bytes/digest and a fixed profile selector. It receives no tenant/job/lease/JWT/core URL, database/storage identity or daemon endpoint. Shared image/text identities remain paired within the new boundary and incompatible with earlier spaces. AI output still cannot approve ownership or release. Rerank scores and general drafts remain private SHADOW outputs; intrinsic components require explicit existing rollout authority.

Each model has eight-process/8-GiB limits. Ordinary inference has a 100-second deadline; generative/quality/CPU Shona have 260 seconds, below the respective core leases. The service's existing outer host limits also apply. These are enforced bounds, not measured production capacity or latency objectives.

## Preliminary findings and corrections

An initial broker import selected the same-named launcher rather than the reviewed private HTTP client. The final broker resolves that source explicitly. A first image fixture contained GDI display-metadata chunks; the narrow provider correctly rejected it. The fixture now matches the real sanitizer's canonical RGBA8 PNG contract, without weakening the model input reader or adding a package.

A loopback attempt to an unserved port timed out and Windows reported no missing-capability diagnosis. It was rejected rather than counted as access denial. Model startup instead requires the actual zero-capability token and host no-exemption query, plus direct/native-diagnosed outbound denial. Independent reachable loopback/host listeners retain the existing paired receiver controls. No production code treats timeout alone as denial. Preliminary partial runs and the interrupted diagnostic run are not final acceptance evidence.

CPython core and standard-library bytes were added to the new profile identity before the frozen run. Private immutable bundles are prepared from existing reviewed local bytes; incomplete/older bundles remain separate diagnostics rather than being mutated into an accepted release.

Review also found that the initial schema-v3 branch skipped the trusted broker host's recursive base-Python ACL check when adopting the model's private runtime. That preflight gap was corrected, with a regression requiring rejection before plan loading when the broker stdlib tree is writable. The interrupted gate was not accepted; the final gate must run against the corrected source and bundle.

The first authenticated text case completed inference but remained PROCESSING because the core's result type allowed only the earlier provider identifier. The new identifier is now an explicit second adoption; unknown providers remain rejected and the registered space's provider must still match. Unit and real-core checks cover the new identifier and attempted substitution into a legacy space.

Interrupted verification exposed a recovery-order defect: a tracked package RX grant made strict bundle privacy fail before its own cleanup. Startup now first verifies exact immutable bytes under a no-broad-write ACL, reconciles every configured state's inactive owned grants, and requires strict private ACLs before any claim. Unknown grants and active owners still block work. The native parent-death drill exercises the ordinary plan-startup path, including repeated recovery; it does not bypass startup with manual journal deletion.

A separate native stdlib probe, under the same actual zero-capability token class, opened the Docker named pipe successfully in the parent and received **Win32 error 5** in the child. A private sibling file was denied and credential environment variables were absent. This ancillary IPC/file observation is distinct from the actual-model conformance phase and is not a model-quality measurement.

## Final unchanged-source verification

**PASS: all 22 phases, 132 Python regressions, 22 Java unit tests and 92 integration tests**, zero failures/errors/skips. All 34 isolated profiles execute, all nine new native broker recovery/integrity checks pass, and the three new real-core broker scenarios pass alongside every earlier provider. The new core cases execute 12 successful model jobs through checked protected foreground service starts, reject a wrong-tenant claim and an isolated-provider substitution into a legacy space, preserve private/raw source, and select explicit intrinsic pins while drafts/reranking stay SHADOW. Logical restore matches **35 tables/3,076 synthetic rows**, exact rows/catalog and tenant/runtime-role/snapshot independence.

- Full result: `.test-tmp/local-verification-20261101T192437747523Z/result.json`.
- Full result SHA-256: `00a8ef98f76bd96092b03f12e7b33b4e2721394ecfbb31b8b5946c09dc0992aa`.
- Frozen repository source/lock SHA-256 before and after: `20efef878f7dec0fee668f78337e5ad62eef10d85ca8bb5786d47fffdcba9374`.
- Native recovery record: `.test-tmp/broker-recovery-076354516052411c85376b197ca1db9b/result.json`, SHA-256 `6d0b4535ee06615a87f4df164efda7d7bc223a4ddd193d1f27c01a4eb371d3d7`.
- Restore record: `.test-tmp/foundation-03ed6804-3df1-47cd-a008-50d3d9201f72/restore-result.json`, SHA-256 `fc7f836804c599e42a74d33bc917172999a1b711483ff2958bb90d0df736d3ff`.
- Started: `2026-11-01T19:24:37.747725+00:00`; finished: `2026-11-01T20:24:36.631664+00:00`.

| Phase | Exit | Seconds |
|---|---:|---:|
| source-gate | 0 | 1.261 |
| architecture | 0 | 0.71 |
| python-regressions | 0 | 7.64 |
| java-artifact-lock | 0 | 6.616 |
| native-model-artifacts | 0 | 0.864 |
| native-model-conformance | 0 | 26.891 |
| perception-artifacts | 0 | 1.174 |
| joint-region-artifacts | 0 | 1.068 |
| language-artifacts | 0 | 0.772 |
| generative-artifacts | 0 | 1.229 |
| generative-installed-runtime | 0 | 16.47 |
| quality-artifacts | 0 | 1.299 |
| shona-cpu-artifacts | 0 | 0.928 |
| shona-cpu-native-probes | 0 | 46.944 |
| docker-guardian-lifecycle | 0 | 20.01 |
| worker-service-recovery | 0 | 144.803 |
| offline-boundary-and-archive | 0 | 22.673 |
| isolated-model-broker | 0 | 875.256 |
| broker-death-and-integrity | 0 | 168.213 |
| shona-artifacts | 0 | 0.597 |
| shona-installed-runtime | 0 | 30.075 |
| real-provider-integration-and-restore | 0 | 2218.902 |

The combined integration/restore harness has a 5,400-second verification deadline to include repeated protected service startups; other phases retain 1,800 seconds. Production model deadlines remain 100/260 seconds. These execution durations are not production latency, capacity or accuracy benchmarks. No remote CI, installed SCM, upstream-suite or whole-system disaster-recovery result is claimed.

The final run's isolated-model phase passed **all 34 registry entries**, with full-set selection and confirmed profile/container cleanup. Its record is `.test-tmp/model-broker-919b0cdec20343b2b74ecc84b1f178b2/result.json`, SHA-256 `00c962475e127cf34e695e1b54dc08937289dce5efa8625596112b407a5ade32`, started `2026-11-01T19:30:23.476760+00:00`, finished `2026-11-01T19:44:17.232488+00:00`. The worker bundle receipt is `c955b34be6cf201496d4515f1ee633b207d9124a35d43efa653419c833ee29fa`. This is a worker release; the top-level verification tool runs against the separately hashed repository source.

Actual authored-fixture observations include OCR `BLUE BAG 742`, speech `BLUE TRAVEL BAG NUMBER SEVEN FOR TWO` (retaining its recognition error), English→Shona `Ndakarasikirwa nebhegi rangu.`, and automatic mixed-input translation `I left the black Lenovo bag in Joina around 2pm.` These observations do not establish representative accuracy or erase the earlier name/meaning/repetition failures.

## M4 acceptance and remaining work

| Category | Remaining requirement | Why this run cannot establish it |
|---|---|---|
| External data/acceptance | Permitted representative instance, language, speech and query/candidate datasets; accountable quality thresholds and native-speaker Shona/code-switch review | Authored synthetic conformance is not representative evaluation; prior translation/name/repetition errors remain recorded |
| Target administrator/account | Provisioned least-privilege service logon, daemon ACLs, trusted-broker core-egress policy, installed SCM start/stop/restart/boot acceptance | This environment's create-service access probe returns Win32 error 5; no account/firewall/service installation is performed |
| External deployment | Target hosts/registry/secret and IdP ownership, TLS, live rotation, production topology and approved SLO/RPO/RTO | Local foreground execution and local restore do not exercise those systems |
| Further local engineering | Wider formats/languages/learned-local or instance models where evaluation requires them, quality improvements, larger cohorts/index migration, parallel/distributed scheduling, portable hosts, operational metrics and broader host/daemon/backup failure tests | These remain engineering work; they must not be mislabeled as unavailable infrastructure |

The locally supported provider/broker slice is verified here. Full M4 acceptance and production readiness remain gated by the stated quality and deployment requirements. No requirement from the complete specification is dropped, and no later product milestone is implemented by this change.

The optional staged whitespace check reports one extra final blank line in `tests/test_model_broker.py`. The accepted source bytes are retained exactly; this is a formatting finding, separate from the passing source gate, runtime integrity and executed test results.
