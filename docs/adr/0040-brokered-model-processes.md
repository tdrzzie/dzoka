# 0040: Broker core access outside isolated model processes

- Status: Accepted; bounded native/core verification passed; quality/deployment acceptance remains open
- Date: 2026-11-01

## Context and decision

After the mandatory source gate, implement the broker required by ADR 0039 for every supported worker family. Reuse the reviewed 22-wheel Windows runtime, existing licensed model artifacts and pinned CPU Shona image. No new library or weight is installed. Preserve every older inference source/manifest/key; append broker-specific profiles and an incompatible text/joint embedding-space identity. Model math and unverified trust are unchanged.

A trusted host broker holds the tenant service credential and performs only its configured core claim/input/complete/fail operations. It validates the claimed profile and input length/digest before passing opaque invocation ID, input digest, approved profile selector and bytes to one capability-free AppContainer. The child receives no tenant/job/lease/token, URL, DB/S3 identity, proxy or Docker endpoint. It loads only the fixed provider adapter from a protected, byte-receipted bundle. Native runtime/model integrity checks still execute inside it.

Use bounded framed anonymous pipes, strict schemas/JSON, absolute invocation deadlines and creation-time kill-on-close job ownership. Read back the actual package SID, zero capabilities and job membership before resume. All child output is untrusted. Only result or the single approved CPU Shona native-token operation is accepted; no child-selected URL, executable, mount, image, profile, tenant or group reaches the host. Shona token requests use the selected profile's approved artifact group and the existing exact-owner Docker Session. Tokens return only after confirmed removal. All other external operations remain core decisions.

Provision an explicit private immutable bundle from existing verified local bytes, including licenses/notices, wheels, source and models. Record every file hash plus the orchestration lock identity. Pin its manifest digest in the plan; reject changed/missing bytes, reparse paths, broad write ACLs, account mismatch or unresolved profile/Docker intents. Do not mutate a bundle in place or use hardlinks to mutable caches. Package SID grants are read/execute only; broker state stays separate and private. No unisolated fallback is offered.

## Alternatives and consequences

Giving network capabilities/core credentials to model processes preserves the original exposure. Monkeypatching HTTP or disabling telemetry alone is not egress isolation. Moving to a new VM/container model runtime would require another artifact/license/platform adoption. Brokered source variants deliberately produce new provenance and space keys, so activation requires explicit reviewed profile registration and fingerprint rollout. Older profiles remain historical compatibility paths, not secure defaults for the new service plan.

## Acceptance and remaining boundaries

Execute actual licensed inference for every registered isolated profile, including image/text joint identity, image/local/OCR/speech, language/LLM/rerank and five CPU Shona variants. Test reachable socket denial, private-file/credential/daemon denial, malformed/oversize/cross-invocation pipe traffic, source/bundle tampering, lease/input/profile/tenant rejection, retries, parent death and exact resource recovery. Preserve raw input, coverage and release authority. Core A07–A09 acceptance and M4 requirements must be reconciled against executed results; synthetic conformance cannot establish representative quality, production service logon, HA or measured SLOs. Native service/account provisioning, institutional policy and representative permitted datasets remain explicit activation requirements.

The [execution record](../testing/m4-model-broker-2026-11-01.md) records the final unchanged-source 22-phase PASS, all 34 native profiles, nine recovery/integrity checks and protected foreground service/core acceptance. Earlier partial runs and preliminary failures are explicitly excluded from final acceptance.
