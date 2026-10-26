# 0032: Keep general LLM drafts private and language directions explicit

- Status: Accepted scoped implementation profile
- Date: 2026-10-26

## Context

M4 still lacks a general local LLM provider and wider translation directions. Generated text is untrusted; model language inventories are not measured quality. Published inference profiles and their 21-wheel runtime must remain reproducible.

## Decision

Add separately versioned local CPU profiles for an official Apache-2.0 SmolLM2 instruction model and a MIT M2M100 ONNX export. Review exact artifacts and tokenizer dependencies before installation/execution. A separate virtual environment layers a reviewed SentencePiece wheel over the unchanged base wheel lock; existing environments, modules and profile keys remain unchanged.

LLM requests are bounded single-turn instructions plus the current report as data, preserved in exact raw request bytes and a digest-bound private payload. Independently authored chat formatting escapes untrusted special-token delimiters, preserves the assistant suffix and separately budgets instruction/context tokens. There are no tools, code execution, retrieval or release actions. Every output is an UNTRUSTED_DRAFT with token/finish/truncation provenance. It cannot be selected or pinned into an intrinsic fingerprint, interpreted as verified attributes, or authorize property release. Semantic prompt-injection resistance is not assumed.

Multilingual profiles explicitly bind source and target languages, model/tokenizer, language-token mapping and decoding policy. Only reviewed named directions can run; source hints must agree with the profile, unsupported hints fail closed and missing languages are never silently mapped to another language. M2M100's inventory excludes Shona. Supported-token inventory and successful fixture generation do not establish language/translation accuracy. Every result requires UNVERIFIED_TRANSLATION. Original text, language and temporal uncertainty remain unchanged.

Reuse private perception jobs, tenant quotas, idempotency, leases, deadlines, append-only results and hold-aware erasure. Prompt payload/raw bytes share the retained-input quota using the existing compatibility input table. New heavy profiles have an explicit bounded worker/lease budget without changing older profile budgets. Translation alone may join explicit intrinsic component pins; general drafts remain query-dependent.

## Alternatives

Changing the shipped runtime lock, silently using a failed general model as translation fallback, unconstrained chat roles/tools, default cloud transfer, guessed language aliases or treating fluent drafts as evidence would break reproducibility or decision/privacy boundaries. Restricted/noncommercial model grants are not adopted as permissive defaults.

## Consequences

Local CPU inference is testable without a cloud model account. Broader model quality, approved datasets, host resource/egress isolation, multilingual product acceptance and deployment remain distinct work. Low available memory is a local host constraint, not proof that the implementation requires external inference infrastructure.

## Source-audit evidence

- [Provider contracts](0011-ai-provider-ports.md), [asynchronous inference](0012-asynchronous-ai.md), [private language jobs](0031-private-language-inference.md).
- [Adoption](../dependencies/generative-adoption.md) records exact primary model/dependency grants and execution prerequisites. No publisher inference or conversion implementation is copied.

## Implementation implications

Verify actual model generation, named directions, wrong/unsupported languages, explicit truncation, private raw bytes, role-token escaping, owner/tenant/lease isolation, retries, quota/erasure/outbox behavior, draft pin exclusion, older profile identities and restore. Record negative quality observations and resource limits honestly.
