# 0031: Ground extracted attributes and keep reranking query-dependent

- Status: Accepted scoped implementation profile
- Date: 2026-10-26

## Context

Structured attributes, translation and reranking remain M4 work. Generated statements are untrusted. Ranking scores depend on the query and cannot become intrinsic item vectors, calibrated confidence, ownership evidence or release authority.

## Decision

Add separate immutable ATTRIBUTES, TRANSLATION and RERANK profiles on the existing reviewed CPU runtime. Use explicitly Apache-2.0 task-specific MiniLM extractive QA for fixed English questions, OPUS-MT Marian for French-to-English translation, and the official Apache-2.0 MS MARCO MiniLM cross-encoder for English bounded reranking. Exact adoption precedes loading weights. Existing provider sources/keys stay intact. The permissively licensed FLAN experiment returned unsuitable field/translation outputs and is retained as negative evidence, not an application backend.

Extraction returns only unique, exact source spans with field-level AVAILABLE/UNAVAILABLE and explicit input/output truncation. Keep source quotation and UTF-16 offsets; no guessed normalization, certainty, identifiers or private ownership answers. Literal support is necessary, not proof that an assigned field is semantically correct. Translation appends a derived output with declared direction and truncation; never replace original text, language or uncertain temporal/location evidence. Translation direction is explicit and must agree with the stored language hint; no implicit language detection or unsupported-language fallback.

Reuse asynchronous private perception jobs, leases, shared quotas and immutable output provenance. ATTRIBUTES/TRANSLATION may be explicitly pinned into fingerprints. Reranking accepts at most eight caller-supplied candidate IDs/texts as a private input envelope governed by the owning report. IDs are opaque client labels, not authority to read another report. Preserve exact request bytes and candidate strings; core creates/digests the exact worker payload including the current query revision. Raw input storage shares the existing bounded retained-input quota and hold-aware erasure. Workers receive no DB/S3 credentials.

Rerank results bind all candidate labels exactly once, sorted raw finite logits, token/truncation metadata and profile identity. No cross-profile score comparison. RERANK components cannot be pinned into intrinsic fingerprint cohorts. Read exact job results through authorized job/result endpoints; later M5 retrieval must construct authorized candidates and fuse independent evidence under its own contract.

## Alternatives

Unlicensed conversions, executing publisher custom code, trusting generated JSON/fields, silently translating unknown languages, scanning private reports using caller IDs, or promoting relevance scores into fingerprints would weaken provenance/privacy and decision separation. A generic chat model or multilingual production-quality claim is not required for this bounded profile.

## Consequences

Local native inference and service-token publication are verifiable without cloud/GPU infrastructure. Fixed prompts, decoding bounds and span grounding form explicit versioned profiles. Missing/hallucinated/ambiguous answers become unavailable. Representative multilingual/instance data and institutional acceptance remain external inputs; broader languages, general LLM behavior, quality/evaluation, indexing and deployment isolation remain engineering work.

## Source-audit evidence

- [AI provider ports](0011-ai-provider-ports.md) and [asynchronous inference](0012-asynchronous-ai.md) retain failure/privacy boundaries.
- [Reranking review](../source-audit/retrieval/reranking-transformer.md), `81fca3ca9b191950f702ae712daef0400608ae3a`: bounded reranking is distinct from calibrated confidence and ownership.
- [Adoption](../dependencies/language-adoption.md) records exact primary grants/artifacts; no upstream implementation is copied.

## Implementation implications

Verify genuine generation and cross-encoder inference; source-span rejection, missing evidence, unsupported direction, malformed candidates, tenant/owner/worker isolation, truncation, raw-input preservation, retry/concurrency/quotas, revision revocation, exact completion, holds/erasure, pin exclusion and restore. Do not infer representative accuracy from synthetic fixtures.
