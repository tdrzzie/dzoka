# Rejected patterns

These are source-level findings or identified consequences of inspected code, not remotely tested exploits. Exact paths/ranges are in the linked reports. No upstream application was executed.

| Pattern | Source evidence | Dzoka constraint |
|---|---|---|
| Fabricate visual scores/explanations | [SmartFind](../platforms/ai-lost-and-found.md), image score derived from text; [FindBack](../platforms/findback.md), fixed 40 fallback | Unavailable/failed evidence stays unavailable; explanations describe actual computation |
| Turn zero reliability into full reliability | [Globalenglish](../platforms/globalenglish-lostfound.md), scoring.py | Explicit null/zero handling and validated numeric reliability |
| Approve ownership or infer fraud solely from rank | [Tracking System](../platforms/lost-and-found-tracking-system.md), processCompetingClaims/awardClaim | Separate MatchScore, ClaimRiskScore, ownership and release policy |
| Give full verification credit when no questions exist | [Smart Asset Finder](../platforms/smart-asset-finder.md), scoreClaimAnswers | Absence of verification is not success |
| Allow privileged self-registration or writable trusted roles | [SmartFind](../platforms/ai-lost-and-found.md), schema/register; [FindBack](../platforms/findback.md), users rules | Server-controlled privileged roles and field-change rules |
| Issue release codes on claim submission | [FindBack](../platforms/findback.md), onClaimCreated | Codes follow ownership approval and deterministic release policy |
| Treat request-supplied actors or report ownership as release authority | [Globalenglish](../platforms/globalenglish-lostfound.md), return route; [Malak](../platforms/malak-lostandfound.md), review | Authenticated actor plus institutional policy and atomic custody transaction |
| Infer crypto from a field named Hash | [Malak](../platforms/malak-lostandfound.md), model/config | Verify actual transformation, keying and access policy |
| Count replica functions/constants or unrelated widgets as coverage | [Tracking System](../platforms/lost-and-found-tracking-system.md), auth test; [FindBack](../platforms/findback.md), widget test | Exercise real middleware/rules/components |
| Count commented assertions as conformance validation | [OpenEPCIS](../traceability/openepcis.md), GenerateEventsTest | Assert capture/auth/duplicate/rollback outcomes |
| Evaluate serialized data as code | [Trishthakur](../platforms/trishthakur-lost-and-found-ai.md), eval; [FuricBond](../platforms/furicbond-lost-and-found.md), pickle | Safe typed formats, trusted artifacts and isolated workers |
| Accept executable preprocessing configuration from users | [PaddleDetection](../vision/paddledetection.md), eval(op_type) | Deployment-controlled operator allowlists |
| Log credentials or private message content | [Trishthakur](../platforms/trishthakur-lost-and-found-ai.md), user-table print; [KWASU](../platforms/kwasu-lost-found-bot.md), debug logs | Privacy-aware structured logs |
| Treat Math.random codes as ownership proof | [FindBack](../platforms/findback.md), [KWASU](../platforms/kwasu-lost-found-bot.md) | Cryptographic, expiring, single-use tokens and atomic consumption |
| Trust webhook sender fields before provider authentication | [KWASU](../platforms/kwasu-lost-found-bot.md), /whatsapp | Signature validation, replay/idempotency and durable conversation state |
| Use CSV files as authoritative recovery/credential persistence | [Trishthakur](../platforms/trishthakur-lost-and-found-ai.md) | Transactions, migrations and append-oriented history |
| Serve random weights or incompatible handcrafted fallbacks | [OpenCLIP](../retrieval/openclip.md), require_pretrained; [OpenVisualSearch](../retrieval/openvisualsearch.md), fallback | Prove expected weights loaded or expose explicit degraded capability/model space |
| Compare incompatible coordinates or omit model version | [Tracking System](../platforms/lost-and-found-tracking-system.md), per-document TF-IDF; [Globalenglish](../platforms/globalenglish-lostfound.md), vector predicate | Complete compatible model/preprocessing identity end to end |
| Bound K only after arbitrarily truncating recall | [Globalenglish](../platforms/globalenglish-lostfound.md), newest pool; [FindBack](../platforms/findback.md), first 50 | Measure candidate recall under tenant/model/history filters |
| Copy offline whole-gallery evaluation into online search | [ILIAS](../retrieval/ilias.md), exact chunks; [Patchwise](../retrieval/patchwise-retrieval.md), dense matrix | Indexed retrieval before bounded expensive processing |
| Compare independent raw federation scores | [Open Federated Search](../federation/open-federated-search.md), Hit.compareTo | Canonical evidence reconciliation; [Mycel](../federation/mycel.md) is a score-agnostic counterexample |
| Treat transport authentication as complete recovery federation policy | [Mycel](../federation/mycel.md), peer allowlist | Scoped evidence access, message signatures/replay policy, revocation and PII minimization |
| Claim whole-archive logarithmic CCTV processing | [CCTV](../cctv/cctv-theft-finder.md), day/night enumeration | Bound coarse samples, seek/decode cost and cancellation; preserve uncertainty |
| Treat unsigned local hashes as immutable custody | [CCTV](../cctv/cctv-theft-finder.md), manifests | Protect/sign manifests and authenticate append-only audit actors |
| Disable TLS or initialize production identity from constants | [DPP/DID](../traceability/dpp-did.md), server | Secure key lifecycle, verified transport and tested authorization |
| Adopt insecure development deployment defaults | [OpenEPCIS](../traceability/openepcis.md), security-disabled privileged Compose; [pgvector](../retrieval/pgvector.md), actual build ref | Provenanced builds and explicit production security/network configuration |
| Infer project permission from readable code or nested licenses | [Patchwise](../retrieval/patchwise-retrieval.md), bundled ILIAS license; unresolved platforms | Verify scope of the grant; no unauthorized copying |
| Require blockchain because an escrow reference uses it | [OpenEscrow](../recovery/openescrow.md) | Normal payment/ledger adapters by default; optional implementations need ADR |

The [earlier catalogue](../../findings/rejected-patterns.md) remains intact. Additional historical claims are not automatically promoted.
