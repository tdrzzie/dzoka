# Working on Dzoka

## Scope and existing work

Build toward the complete specification in `docs/dzoka.md` and the user's current instructions. Dzoka is a temporary internal codename. Preserve supplied research and design material; do not overwrite it with generated summaries. Read `STATUS.md`, `docs/source-audit/INDEX.md`, the relevant existing design documents and ADRs before changing behavior.

## Architecture invariants

1. Retrieval, scoring, confidence, ownership verification and recovery authorization are separate decisions.
2. Missing evidence is unavailable, never an automatic mismatch. Track evidence coverage independently.
3. Preserve exact raw input alongside normalization. Keep temporal uncertainty.
4. Hard identity contradictions override aggregate similarity.
5. Embeddings and AI outputs retain provider, model, version, configuration, timestamp and input-content provenance. Append new versions; never silently replace them.
6. AI cannot authorize property release. Deterministic policy or an authorized human must approve it.
7. Public and federation responses must exclude secret ownership evidence and private contact details.
8. Federation preserves local custody of PII and never assumes independent scores are comparable.
9. AI, vectors, messaging, courier and payment integrations sit behind replaceable provider contracts.
10. Heavy AI runs asynchronously. Critical external operations and event consumers are idempotent.
11. Enforce tenant boundaries in authorization, persistence, caches, jobs, media and providers.
12. Custody and recovery history is append-oriented and auditable.
13. CCTV is optional, authorization-bound and privacy-preserving; no default facial recognition.

An invariant departure requires an explicit ADR with evidence and consequences.

## Source audit gate

Before finalizing major architecture or implementing application features, complete every mandatory audit registered in `docs/source-audit/source-audit.lock.json`. Run `python tools/audit_registry.py gate`. `PENDING` and `IN_PROGRESS` entries are not approved evidence. Pinned revisions inherited from notes must be verified upstream; do not invent SHAs or claim downloaded code has been reviewed. Record actual files, line ranges, hashes, branch, revision, date, license evidence, tests inspected and limitations. Never describe static review as executed tests.

Existing `docs/platforms/` and `docs/findings/` documents are supplied research. Canonical audit records must distinguish their claims from independently inspected source. Ambiguous repository identities remain unresolved until verified. Standards need an edition and authoritative source rather than a fabricated Git commit.

## Dependencies and licenses

No new runtime dependency before its architecture and license review. Pin dependencies and maintain lockfiles. Review model weights separately from library licenses. No copying unlicensed, all-rights-reserved, GPL or AGPL source into a permissive core. Preserve required notices for any authorized reuse. Do not execute upstream code or install its dependencies just to inspect it.

## Security

No committed credentials, real PII, raw verification answers or sensitive upstream artifacts. Never print suspected upstream secrets. Fail closed on missing production configuration; test providers must not silently substitute for production inference. Validate and sanitize uploads in isolated workers. Signed webhooks, replay protection, rate limits, least privilege and privacy-aware audit logging are required at relevant boundaries.

## Verification and documentation

For tooling changes run registry checks and meaningful unit tests; test integrity failures and gate bypasses. For application changes, once authorized by the audit gate, test tenant isolation, authorization, concurrency, retries, migrations and invariant behavior as relevant. Do not invent performance or accuracy claims. Maintain STATUS.md with completed work, blocked work, next steps, debt, benchmark state and production gaps. A UI, interface, mock, schema or passing unit test alone never establishes feature completeness or production readiness.
