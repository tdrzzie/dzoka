# AI execution and governance

Accepted design, 2026-10-26. M4's text, vision/local-feature/OCR/English-speech providers and bounded multimodal cohort activation are implemented; [text ADR 0027](../adr/0027-local-text-inference.md), [rollout ADR 0028](../adr/0028-fingerprint-shadow-rollouts.md), [perception ADR 0029](../adr/0029-local-perception-providers.md), [perception run guide](../development/perception.md) and [STATUS](../../STATUS.md) record profiles and executed scope. Wider language/format coverage, representative quality evaluation, deployment isolation and larger-cohort/index migration remain separate work. This view follows the [current baseline](baseline.md) and ADRs [0011](../adr/0011-ai-provider-ports.md); [0012](../adr/0012-asynchronous-ai.md); [0015](../adr/0015-python-ai-boundary.md). New runtime/artifact choices must pass [dependency adoption](dependency-adoption.md).

## Request and result boundaries

Core commits a job binding tenant, report/media revision, sanitized input references, task, model artifact/configuration, schema and deadline. A private worker leases that job with a task-scoped identity. Results include job/attempt token, input digest/version, provider, artifact digest, preprocessing/prompt/configuration versions, generated time, output schema and structured evidence. Core validates the lease, permissions, compatible shape and current input before appending results; workers do not write domain tables.

## Provider behavior

LLM, embedding, OCR, speech, vision and reranker adapters return typed success/unavailable/failed/policy-denied/invalid-output states. No hidden random model or text heuristic substitutes for missing image inference. Local execution remains supported; cloud transfer needs a tenant-approved minimized projection. Untrusted OCR/transcript/prompt content is data and cannot alter policy or invoke tools.

Artifact activation requires license/checksum review, conformance, evaluation and explicit configuration. Re-embedding appends versions and switches active references after coverage/shadow checks; cancellation, lease expiry and deleted inputs prevent stale activation. Per-tenant/GPU quotas, bounded media decoding and job budgets prevent resource exhaustion.

## Acceptance

Implement A07–A09, A25 and A27. Demonstrate real inference using reviewed artifacts; mocked outputs establish contract tests only. MLflow/evaluation artifact versions do not grant release authority.

Acceptance IDs refer to [the implementation acceptance ledger](acceptance.md); its executed portions do not complete every AI capability described here.
