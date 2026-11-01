# Isolated model broker

[ADR 0040](../adr/0040-brokered-model-processes.md) appends 34 isolated profiles for the seven supported worker families. `ai/broker-profiles.json` is the registry; `ai/broker.lock.json` binds runtime boundary sources. Older inference/model/runtime/profile bytes are preserved. Isolated text and paired image/text spaces have new incompatible identities. Registering a profile does not approve its output or activate a fingerprint.

The trusted broker holds one configured tenant service credential and uses fixed core claim/input/complete/fail paths. The model receives an opaque invocation ID, approved profile selector, exact bytes and content digest over bounded anonymous pipes. It receives no tenant/job/lease/token/core URL or DB/S3/Docker authority. The broker checks claimed profile, source length/digest, response invocation/digest/profile and completion acknowledgement. Ambiguous completion retries reuse the exact envelope. Core leases, quotas, authorization, schema validation, retention and activation remain authoritative.

The model is created suspended with atomic Windows Job membership, zero AppContainer capabilities and only three explicitly inherited standard handles. The broker reads back package SID/capabilities/job membership and rejects a loopback exemption before resume. The model checks its credential-free environment and requires direct outbound denial or Windows' missing-capability diagnostic; a timeout alone fails. The independent boundary suite tests reachable loopback/host listeners and private-file denial. This restricts model egress, not arbitrary code under the trusted broker's account. Keep that account and its permitted core endpoint under deployment policy.

Ordinary model work has a 100-second deadline; generative, quality and CPU Shona have 260 seconds. Both fit their respective core leases. The child Job limits committed memory to 8 GiB and processes to eight. Killing the broker closes its job handle and kills the model. CPU Shona's single token-only pipe operation is accepted only for the configured family/artifact group; the broker uses the existing reviewed image and exact-owner Docker Session. The child cannot choose an image, executable, mount, endpoint or group. The container remains non-root, network-disabled, read-only and bounded. Tokens return after confirmed container removal. Daemon resources remain outside Windows Job ownership and need journal reconciliation after broker/service loss.

## Prepare an explicit private bundle

Use the already reviewed installed CPython and cached, hash-locked wheels/models. No fetch/install step occurs. Preparation copies files; it does not use hardlinks. Choose a new absolute directory under an ancestor protected against replacement by other accounts. The working checkout's broad inherited write permissions are unsuitable for a deployment release.

```powershell
python -B tools/broker_bundle.py prepare --bundle C:\DzokaPrivate\release-001
python -B tools/broker_bundle.py check --bundle C:\DzokaPrivate\release-001 --manifest-sha256 <returned-digest>
```

The parent must already satisfy the ACL checks. Preparation protects the new tree and records every file's digest/length, CPython version and broker-lock identity in `bundle.json`. Pin its returned SHA-256 in the plan. Include required model/wheel/runtime notices; do not redistribute artifacts beyond their reviewed terms. Failure leaves incomplete staging for inspection. Never overwrite a published bundle in place. Changes to broker sources require a new lock, registry identity, bundle, registration and explicit rollout.

## Broker plan and service configuration

Use `worker_service.py` schema **3** with the same explicit `accountSid` and `integrityRid` assertions as schema 2. Schema 1/2 retain the historical networked worker path. A schema-3 plan is broker-only:

```json
{
  "schemaVersion": 1,
  "coreUrl": "https://core.example",
  "allowInsecureLoopback": false,
  "pollSeconds": 5,
  "backoffMaxSeconds": 300,
  "entries": [{
    "tenant": "11111111-1111-1111-1111-111111111111",
    "profile": "22222222-2222-2222-2222-222222222222",
    "profileName": "ISO_TEXT",
    "bundle": "C:\\DzokaPrivate\\release-001",
    "bundleSha256": "<actual-manifest-sha256>",
    "stateDirectory": "C:\\DzokaPrivate\\tenant-state",
    "tokenEnvironment": "DZOKA_WORKER_TOKEN_ONE"
  }]
}
```

Create explicit private `profiles` and `docker` children under each entry state. Cross-tenant state paths cannot overlap. Keep service config, credentials, plan and service status outside the model-readable bundle. Credential JSON must contain exactly the plan's references. Model processes receive package-specific read/execute grants only on their immutable bundle. Grant intents precede access changes; startup revokes inactive owned grants using the plan-pinned bundle and derived package SID, then reconciles profiles. Active owners, changed receipts and ambiguous intents block work. Never delete unresolved journals to force startup.

Recovery verifies bundle bytes and refuses broad write access while tracked read-only grants may remain. It then reconciles all configured states and rechecks strict private bundle access before claiming. This limited inspection mode does not relax credential/state/control ACLs, grant network access or accept unknown grants for inference. A standalone bundle `check` remains strict; use the configured startup recovery path for its owned interrupted grants.

Credentials are snapshotted at service startup; rotate through the approved secret/token lifecycle and a checked restart. This implementation does not refresh IdP tokens automatically. The inherited AppContainer journal capacity is 1,000 profile records, including confirmed terminal records. Monitor capacity and retain/receipt-verify terminal history during controlled offline state rotation; do not discard ambiguous intents or claim indefinite unattended retention. Docker terminal archival uses its existing separate tool. Long-running retention automation remains operational engineering.

Generate reviewable **disabled** registration SQL with `tools/broker_profile.py --tenant <uuid> --profile <uuid> --profile-name ISO_TEXT` (or another exact registry selector). Enablement and fingerprint promotion require existing operator authority and evaluation evidence. Polling is serial with per-assignment capped backoff; one invocation failure does not authorize another assignment or fabricate a completed job. Do not run concurrent service instances against one release/state to evade ownership checks.

Run the private release's `tools/worker_service.py` with the reviewed base interpreter using `-I -B`, `--config <protected-schema-3-config>` and `--check`, `--foreground` or `--scm`. `-B` prevents trusted host imports from adding bytecode to the immutable receipt inventory. SCM registration, least-privilege service logon, credentials, daemon ACLs, stop/restart/boot and target-host policy must be validated by the deployment operator. See [service guide](worker-service.md).

## Verification

Set `DZOKA_TEST_BROKER_BUNDLE` to the explicit prepared bundle and `DZOKA_TEST_BROKER_SHA256` to its receipt. `tools/verify_model_broker.py` requires actual inference for every registry entry. `--profile` is an explicitly narrower diagnostic and cannot establish full acceptance. `tools/verify_broker_recovery.py` tests native deadlines, broker death, grant/profile recovery and receipt faults without mutating the real model bundle. The full `tools/verify_local.py` gate requires both phases and `BrokerProviderIT`, in addition to existing real-provider, authorization/concurrency/retention/activation and restore checks. These are synthetic conformance and failure evidence, not representative accuracy or installed-service acceptance.
