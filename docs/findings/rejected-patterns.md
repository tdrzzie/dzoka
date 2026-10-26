# Rejected Patterns

> **Dzoka — The Open Recovery Network**
>
> This document records architectural, security, privacy, AI, data, matching, verification, federation, and recovery patterns discovered during Dzoka source audits that **must not be reproduced in Dzoka** without an explicit ADR overturning the decision.

---

# 1. Purpose

Dzoka audits open-source systems not only to discover what should be reused, but also to identify patterns that appear attractive yet are:

- technically unsound;
- insecure;
- misleading;
- privacy-invasive;
- difficult to scale;
- difficult to audit;
- likely to cause false recoveries;
- likely to produce false ownership conclusions;
- unsuitable for federation;
- inconsistent with Dzoka's architecture.

This file is the canonical catalogue of those rejected patterns.

---

# 2. Status Meaning

Each pattern is assigned one of the following dispositions.

| Status | Meaning |
|---|---|
| `REJECT` | Must not be used in Dzoka |
| `REJECT-BY-DEFAULT` | May only be introduced through an explicit ADR and strong justification |
| `REPLACE` | Underlying requirement is valid, but the implementation pattern must be replaced |
| `TEST-ONLY` | Acceptable only in isolated test/demo environments |
| `QUARANTINE` | May exist behind an experimental interface but cannot influence authoritative recovery decisions |

---

# 3. Core Dzoka Invariants

Every rejected pattern in this document follows from the following architectural invariants.

1. **Matching is not ownership.**
2. **Missing evidence is not a mismatch.**
3. **Missing evidence is not synthetic positive evidence.**
4. **Raw user input must remain recoverable.**
5. **Hard identity conflicts may override similarity.**
6. **Every embedding and model output must be version-aware.**
7. **AI may support decisions but may not independently authorize release.**
8. **Ownership evidence is private by default.**
9. **Federation must minimize unnecessary PII.**
10. **Raw scores from independent nodes are not automatically comparable.**
11. **AI providers must remain replaceable.**
12. **Vector search backends must remain replaceable.**
13. **Messaging, courier, and payment providers must remain replaceable.**
14. **Heavy AI work should be asynchronous.**
15. **Critical workflows must be idempotent.**
16. **Tenant isolation must be enforced end to end.**
17. **CCTV processing must be authorization- and privacy-bound.**
18. **Recovery history must remain auditable.**
19. **Major reuse decisions require source audit first.**
20. **Blockchain is not a default system dependency.**

---

# 4. Classification

Rejected patterns are grouped into:

```text
AI / ML
Matching
Retrieval
Item Fingerprinting
Verification
Claims
Recovery / Custody
Security
Privacy
Data
Events
Federation
Messaging
CCTV / Edge
Rewards / Payments
Architecture
Operations
Documentation / Source Reuse
```

---

# 5. AI / ML Rejected Patterns

## RP-AI-001 — Random Scores Masquerading as AI

**Status:** `REJECT`

Do not produce random values and expose them as:

- image similarity;
- confidence;
- embeddings;
- fraud scores;
- identity scores;
- ownership scores.

Rejected example:

```text
imageSimilarity = random(30, 80)
```

or:

```text
embedding = randomVector(128)
```

### Why rejected

Random values may accidentally:

- rank candidates;
- cross notification thresholds;
- contaminate datasets;
- be mistaken for model output;
- influence verification;
- appear statistically meaningful.

### Required replacement

If a modality is unavailable:

```text
available = false
score = null
```

Never:

```text
score = random()
```

---

# 6. RP-AI-002 — Synthetic Modality Evidence

**Status:** `REJECT`

Do not invent feature evidence merely because the system expects every modality to produce a score.

Examples:

```text
no image model
→ image score = 50

no GPS
→ location score = 50

no timestamp
→ time score = 50
```

### Correct representation

```text
feature = location
available = false
score = null
reason = "No usable coordinates"
```

The aggregator must calculate:

```text
evidence coverage
```

separately from:

```text
evidence agreement
```

---

# 7. RP-AI-003 — Unversioned Embeddings

**Status:** `REJECT`

Never persist a vector without knowing at minimum:

```text
provider
model
modelVersion
dimension
embeddingType
contentHash
generatedAt
```

### Why rejected

Embeddings from different models or versions may not share the same vector space.

Comparing them may produce meaningless similarity values.

---

# 8. RP-AI-004 — Silent Vector Dimension Repair

**Status:** `REJECT`

Do not:

```text
pad vectors with zeroes
truncate vectors
resize vectors
```

just so cosine similarity can be computed.

### Required behavior

Return:

```text
INCOMPATIBLE_EMBEDDING
```

and trigger regeneration or compatible-model routing.

---

# 9. RP-AI-005 — Global CLIP Vector as Physical Identity

**Status:** `REJECT`

A single semantic image embedding must not be treated as sufficient evidence that two photographs depict the same physical object.

Global embeddings are useful for:

```text
same category
similar appearance
semantic retrieval
```

but physical-instance matching often requires:

```text
local patches
logos
scratches
stickers
damage
serial fragments
OCR
perceptual signatures
distinctive regions
```

---

# 10. RP-AI-006 — LLM Score as Calibrated Probability

**Status:** `REJECT`

Do not ask an LLM:

```text
"What is the probability these are the same item?"
```

and interpret:

```text
87
```

as:

```text
87% calibrated probability
```

### Acceptable use

LLMs may provide:

```text
semantic comparison
reasoning features
structured extraction
reranking evidence
challenge generation
explanation
```

but the output must not automatically become calibrated confidence.

---

# 11. RP-AI-007 — LLM as Ownership Authority

**Status:** `REJECT`

An LLM must never independently decide:

```text
claimant owns item
```

or:

```text
release item to claimant
```

### Required architecture

```text
AI evidence
    ↓
Verification policy
    ↓
Risk checks
    ↓
Human / deterministic authority
    ↓
Recovery authorization
```

---

# 12. RP-AI-008 — LLM as Custody Authority

**Status:** `REJECT`

Do not allow an AI agent to execute:

```text
CustodyTransfer
RecoveryAuthorized
RewardReleased
```

solely because its reasoning output is positive.

---

# 13. RP-AI-009 — Undocumented Model Output

**Status:** `REJECT`

Every AI run affecting search, matching, verification, or risk must preserve model provenance.

Required minimum:

```text
modelRunId
provider
model
version
configuration
inputHash
startedAt
completedAt
status
outputSchemaVersion
```

---

# 14. RP-AI-010 — AI Placeholder Enabled in Production

**Status:** `REJECT`

Placeholder methods may exist during development but must not silently operate in production.

Examples:

```text
TODO embedding
random score
fixed score
fake OCR
fake face match
```

### Required approach

```text
capability status = DISABLED
```

until a real provider is configured.

---

# 15. RP-AI-011 — Model Output Without Availability Metadata

**Status:** `REJECT`

A score alone is insufficient.

Use:

```text
{
  "available": true,
  "score": 0.82,
  "modelRunId": "...",
  "quality": "...",
  "reason": null
}
```

not:

```text
{
  "score": 82
}
```

---

# 16. RP-AI-012 — AI Explanation Without Evidence Trace

**Status:** `REJECT`

Do not generate explanations that cannot be connected to underlying evidence.

A statement such as:

```text
"The items are likely identical because the scratches match."
```

must not be shown unless scratch-level evidence actually participated in the comparison.

---

# 17. Matching Rejected Patterns

## RP-MAT-001 — Matching Equals Ownership

**Status:** `REJECT`

```text
High Match Score
≠
Ownership
```

A match says:

```text
these reports may describe the same physical item
```

A claim says:

```text
this person asserts entitlement to that item
```

These are different questions.

---

# 18. RP-MAT-002 — Missing Evidence Equals Mismatch

**Status:** `REJECT`

If one report has no brand, do not score:

```text
brand mismatch
```

If one has no location, do not score:

```text
geographic disagreement
```

Unknown and contradictory are different states.

---

# 19. RP-MAT-003 — Missing Evidence Equals Neutral 50

**Status:** `REJECT`

Do not assign arbitrary midpoint scores when a feature is absent.

Rejected:

```text
if no location:
    locationScore = 50
```

Correct:

```text
locationAvailable = false
locationScore = null
```

---

# 20. RP-MAT-004 — Similarity-Only Matching

**Status:** `REJECT`

Do not calculate:

```text
finalScore = weightedAverage(all similarities)
```

without checking contradictions.

### Example

```text
visual similarity = 95
text similarity = 90
serial number mismatch = true
```

A serial-number conflict may invalidate the candidate regardless of average similarity.

---

# 21. RP-MAT-005 — No Hard Conflict Layer

**Status:** `REJECT`

Strong contradictions must be modeled separately.

Examples:

```text
serial mismatch
IMEI mismatch
registration mismatch
distinct owner engraving mismatch
impossible chronology
incompatible item subtype
```

---

# 22. RP-MAT-006 — Exact Category as Universal Candidate Gate

**Status:** `REJECT`

Do not require:

```text
lost.category == found.category
```

before comparison.

User categorization is imperfect.

Example:

```text
Electronics
Phone
Mobile Device
iPhone
```

may refer to the same item.

Category should generally be:

```text
retrieval signal
+
matching feature
```

rather than absolute gate.

---

# 23. RP-MAT-007 — Same District as Universal Candidate Gate

**Status:** `REJECT`

Items move.

Rejected rule:

```text
candidate.district == report.district
```

as a universal requirement.

Items can travel through:

```text
buses
airports
trains
couriers
commuting
institutional transfer
```

---

# 24. RP-MAT-008 — Fixed Geographic Radius as Identity Rule

**Status:** `REJECT`

Do not globally reject a candidate because:

```text
distance > 100 km
```

or another arbitrary value.

Distance should be interpreted with:

```text
transport context
elapsed time
route knowledge
institutional transitions
category
movement model
```

---

# 25. RP-MAT-009 — Creation Time as Loss/Found Time

**Status:** `REJECT`

Do not treat:

```text
report.createdAt
```

as equivalent to:

```text
item lostAt
item foundAt
```

A report may be submitted hours or weeks later.

---

# 26. RP-MAT-010 — One Unversioned Weighted Formula Forever

**Status:** `REJECT`

Do not embed unexplained constants such as:

```text
image = 40%
text = 30%
location = 20%
time = 10%
```

into source permanently.

Matching configuration must be versioned.

---

# 27. RP-MAT-011 — Score Without Evidence Coverage

**Status:** `REJECT`

Two candidates with:

```text
score = 82
```

may have radically different evidence.

Example:

```text
Candidate A
5/5 modalities available

Candidate B
1/5 modalities available
```

The result must preserve coverage.

---

# 28. RP-MAT-012 — Match Percentage Presented as Probability

**Status:** `REJECT`

Do not display:

```text
92% probability this is your item
```

unless the number has been calibrated to mean exactly that.

Prefer:

```text
Match score: 92
Match tier: Very strong
```

until calibrated probability exists.

---

# 29. RP-MAT-013 — Mirrored Match Records as Canonical State

**Status:** `REJECT`

Do not store:

```text
A → B
B → A
```

as separate authoritative match entities.

They can diverge.

Store:

```text
one MatchCandidate
```

and create projections for each user.

---

# 30. RP-MAT-014 — Match Results Mutable by Clients

**Status:** `REJECT`

Users may update:

```text
viewed
dismissed
feedback
```

but must not alter:

```text
score
feature evidence
conflicts
model provenance
candidate identity
```

---

# 31. Retrieval Rejected Patterns

## RP-RET-001 — Linear Scan as Production Retrieval Architecture

**Status:** `REJECT-BY-DEFAULT`

Fetching hundreds or thousands of items and comparing all pairs is not the production retrieval architecture.

Use:

```text
FTS
trigram
vector ANN
geospatial filtering
structured indexes
candidate fusion
```

---

# 32. RP-RET-002 — Vector Search Only

**Status:** `REJECT`

Vector similarity alone should not be the candidate-generation system.

Dzoka requires hybrid retrieval:

```text
semantic
lexical
geospatial
temporal
identifier
perceptual
structured attribute
```

---

# 33. RP-RET-003 — Raw Independent Node Score Sorting

**Status:** `REJECT`

Federated nodes may use different:

```text
models
weights
thresholds
datasets
calibration
```

Therefore:

```text
Node A score 0.91
Node B score 0.82
```

does not imply A is better.

---

# 34. RP-RET-004 — Assuming Remote Scores Share Local Meaning

**Status:** `REJECT`

Remote candidate scores must retain:

```text
source node
score type
score label
algorithm version
calibration metadata
```

when available.

---

# 35. RP-RET-005 — Destructive Early Filtering Without Recall Analysis

**Status:** `REJECT`

Early filters must not eliminate valid recovery paths simply for convenience.

Every hard filter should answer:

```text
Can a correct candidate exist outside this condition?
```

If yes, it is probably a ranking feature rather than a hard gate.

---

# 36. Item Fingerprint Rejected Patterns

## RP-FP-001 — ItemFingerprint as One Vector

**Status:** `REJECT`

Dzoka `ItemFingerprint` is not:

```text
embedding[]
```

It is a composite evidence structure.

---

# 37. RP-FP-002 — Fingerprint Without Provenance

**Status:** `REJECT`

Every derived fingerprint feature must be traceable to:

```text
source media
model
version
derivation run
timestamp
```

---

# 38. RP-FP-003 — Overwriting Raw User Description

**Status:** `REJECT`

Do not replace:

```text
rawDescription
```

with AI-corrected text.

Store separately:

```text
rawDescription
normalizedDescription
translatedDescription
structuredAttributes
```

---

# 39. RP-FP-004 — Public Storage of Full Identifiers

**Status:** `REJECT`

Do not expose or normally index plaintext:

```text
IMEI
serial
passport number
national ID
account number
```

---

# 40. RP-FP-005 — Unsalted Plain Hash for Low-Entropy IDs

**Status:** `REJECT`

Plain hashing is often inadequate for identifiers with predictable search spaces.

Prefer appropriately designed:

```text
keyed hash
```

plus encrypted original where required.

---

# 41. RP-FP-006 — No Fingerprint Versioning

**Status:** `REJECT`

An item may be reprocessed as:

```text
models improve
OCR improves
new media appears
taxonomy changes
```

Fingerprint versions must coexist where required.

---

# 42. Verification Rejected Patterns

## RP-VER-001 — Claimant Sees All Verification Secrets

**Status:** `REJECT`

Do not expose all hidden item attributes to a claimant before verification.

Otherwise the claimant can simply repeat them.

---

# 43. RP-VER-002 — Public Security Questions

**Status:** `REJECT`

Verification questions should be derived from:

```text
private evidence
non-public item traits
protected identifiers
```

not information already visible on the listing.

---

# 44. RP-VER-003 — LLM Sole Verifier

**Status:** `REJECT`

An LLM may interpret answers but cannot alone approve ownership.

---

# 45. RP-VER-004 — Match Score Used as Ownership Score

**Status:** `REJECT`

Evidence that:

```text
Report A == Report B
```

does not prove:

```text
Claimant owns Item
```

---

# 46. RP-VER-005 — One Proof Type for Every Item

**Status:** `REJECT`

Verification should vary by category.

Examples:

```text
phone
→ IMEI / lock-screen detail / account evidence

wallet
→ hidden contents

luggage
→ interior contents / label / travel evidence

document
→ identity verification

institution asset
→ asset register
```

---

# 47. RP-VER-006 — Missing Proof Equals Automatic Rejection

**Status:** `REJECT`

Some legitimate owners may lack receipts or serial numbers.

Verification should combine independent evidence rather than depend on a single universal proof type.

---

# 48. RP-VER-007 — Verification Evidence Stored Publicly

**Status:** `REJECT`

Proof-of-ownership evidence is at minimum:

```text
RESTRICTED
```

and frequently:

```text
SECRET
```

---

# 49. RP-VER-008 — Verification Evidence Mutable After Submission

**Status:** `REJECT`

Evidence may need corrections or withdrawal, but should not silently disappear after review begins.

Use versioning or explicit events.

---

# 50. RP-VER-009 — AI-Generated Challenge Without Leakage Check

**Status:** `REJECT`

An AI-generated verification question must not reveal the expected answer.

Bad:

```text
"Was your bag's hidden tag blue?"
```

if blue was secret.

Better:

```text
"Describe any tag or marking inside the bag."
```

---

# 51. Claims Rejected Patterns

## RP-CLM-001 — Notification as Claim

**Status:** `REJECT`

A notification saying:

```text
User X says this is theirs
```

is not a canonical Claim.

Claims require a durable aggregate/state machine.

---

# 52. RP-CLM-002 — Claim Creation Changes Item to Claimed

**Status:** `REJECT`

A claim represents an assertion.

It does not prove entitlement.

Correct:

```text
ClaimSubmitted
```

not:

```text
Item.claimed = true
```

---

# 53. RP-CLM-003 — Claimant Can Approve Own Claim

**Status:** `REJECT`

Claimants may:

```text
submit
withdraw
add evidence
respond to challenge
```

They must not:

```text
verify
approve
authorize release
complete recovery
```

---

# 54. RP-CLM-004 — Generic Client Update on Claim Record

**Status:** `REJECT`

Avoid APIs such as:

```text
PUT /claim/{id}
{
  "status": "completed"
}
```

Use explicit commands:

```text
SubmitClaim
SubmitEvidence
AnswerChallenge
ApproveClaim
RejectClaim
WithdrawClaim
```

---

# 55. RP-CLM-005 — Status Enum Without Transition Enforcement

**Status:** `REJECT`

Having:

```text
pending
verified
completed
```

in a schema does not enforce the workflow.

Transitions must be authoritative.

---

# 56. RP-CLM-006 — One Active Claim Blocks All Others Immediately

**Status:** `REJECT`

The mere existence of one claim should not necessarily prevent competing legitimate claims.

Conflict resolution may be required.

---

# 57. Recovery / Custody Rejected Patterns

## RP-REC-001 — Handover Credential on Claim Creation

**Status:** `REJECT`

Rejected:

```text
ClaimSubmitted
    ↓
Generate handover code
```

Required:

```text
ClaimApproved
    ↓
RecoveryAuthorized
    ↓
Generate handover credential
```

---

# 58. RP-REC-002 — `Math.random()` for Handover Credentials

**Status:** `REJECT`

Security-sensitive credentials require a cryptographically secure random source.

---

# 59. RP-REC-003 — Plaintext Handover Token Storage

**Status:** `REJECT`

Persist a secure verifier/hash rather than the recoverable secret whenever possible.

---

# 60. RP-REC-004 — Permanent Handover Code

**Status:** `REJECT`

Handover credentials require:

```text
expiry
attempt limits
single-use
revocation
scope binding
```

---

# 61. RP-REC-005 — One Boolean `handoverConfirmed`

**Status:** `REJECT`

A boolean cannot represent the full custody transition.

Prefer:

```text
CustodyTransfer
├── fromParty
├── toParty
├── verifiedBy
├── occurredAt
├── location
├── evidence
├── credentialId
└── status
```

---

# 62. RP-REC-006 — Delivery Equals Recovery

**Status:** `REJECT`

Courier status:

```text
DELIVERED
```

does not necessarily mean:

```text
RecoveryConfirmed
```

Recipient confirmation may still be required.

---

# 63. RP-REC-007 — Ownership Equals Custody

**Status:** `REJECT`

A finder may have custody without ownership.

An institution may hold custody without ownership.

A courier may temporarily have custody.

Model separately.

---

# 64. RP-REC-008 — Custody Equals Entitlement

**Status:** `REJECT`

Physical possession does not determine who is entitled to recovery.

---

# 65. RP-REC-009 — Recovery Without Immutable History

**Status:** `REJECT`

Do not simply update:

```text
currentHolder = X
```

without preserving prior custody transitions.

---

# 66. RP-REC-010 — Blockchain Required for Recovery

**Status:** `REJECT-BY-DEFAULT`

Blockchain is not required to establish an auditable custody history.

Default:

```text
signed application events
append-oriented audit
transactional database
optional EPCIS
```

---

# 67. Security Rejected Patterns

## RP-SEC-001 — Client-Editable Role

**Status:** `REJECT`

Users must not be able to self-promote:

```text
citizen → admin
citizen → police
```

through generic profile updates.

---

# 68. RP-SEC-002 — Client-Editable Reputation

**Status:** `REJECT`

Reputation must be derived from authoritative events.

---

# 69. RP-SEC-003 — UI-Only Authorization

**Status:** `REJECT`

Hiding a button is not an authorization control.

Authorization must be enforced server-side.

---

# 70. RP-SEC-004 — Comment-Only Security

**Status:** `REJECT`

Code comments such as:

```text
users cannot change role
```

mean nothing unless executable policy enforces them.

---

# 71. RP-SEC-005 — Broad Field-Level Mutation

**Status:** `REJECT`

Do not allow a user to update an entire object merely because they own it.

Different fields carry different authority.

---

# 72. RP-SEC-006 — Authenticated Means Trusted

**Status:** `REJECT`

Authentication answers:

```text
Who are you?
```

not:

```text
Are you allowed to do this?
```

---

# 73. RP-SEC-007 — Client-Created System Notification

**Status:** `REJECT`

Users must not be able to generate arbitrary trusted-looking system notifications.

---

# 74. RP-SEC-008 — Unrestricted Chat Creation

**Status:** `REJECT`

Knowing another user's ID must not be sufficient to establish a recovery conversation.

Conversation creation must be bound to:

```text
case
claim
match
authorized relationship
```

---

# 75. RP-SEC-009 — Client-Supplied Ownership Identity Trusted

**Status:** `REJECT`

Do not accept:

```text
itemOwnerId
verifiedBy
approvedBy
tenantId
```

from a client without deriving or validating authority server-side.

---

# 76. RP-SEC-010 — Secret in Source Fallback

**Status:** `REJECT`

Patterns such as:

```text
process.env.API_KEY || "real-secret-here"
```

are forbidden.

The Lost-and-Found-Tracking-System audit reinforced this risk with a secret-like fallback.

---

# 77. RP-SEC-011 — Production Credential in Example Configuration

**Status:** `REJECT`

Example configuration must contain only placeholders.

---

# 78. RP-SEC-012 — Security Token Without Scope

**Status:** `REJECT`

Tokens must be scoped to what they authorize.

A handover token must not become a generic account/authentication token.

---

# 79. RP-SEC-013 — No Attempt Limits on Verification Credentials

**Status:** `REJECT`

OTP/challenge/handover verification requires rate limiting and bounded attempts.

---

# 80. RP-SEC-014 — No Replay Protection

**Status:** `REJECT`

Security-sensitive actions must defend against reused:

```text
webhooks
tokens
federation messages
handover credentials
```

---

# 81. RP-SEC-015 — Face Search Enabled by Default

**Status:** `REJECT-BY-DEFAULT`

Face detection for redaction and biometric identification are different risk classes.

Biometric identification requires its own:

```text
ADR
legal basis
privacy assessment
policy
retention rules
```

---

# 82. Privacy Rejected Patterns

## RP-PRV-001 — Public Original Media as Default

**Status:** `REJECT`

When sanitization exists, public clients should receive sanitized derivatives, not secret originals.

---

# 83. RP-PRV-002 — Privacy Processing Status as Single Boolean

**Status:** `REJECT`

Avoid:

```text
imageProcessed = true
```

when this could mean only metadata extraction succeeded.

Use task-specific statuses.

---

# 84. RP-PRV-003 — Sensitive Original Available Despite Blurred Derivative

**Status:** `REJECT`

Creating:

```text
blurred.jpg
```

does not protect privacy if users can still retrieve:

```text
original.jpg
```

without authorization.

---

# 85. RP-PRV-004 — Full PII in Search Index

**Status:** `REJECT`

Search should expose only the minimum data required for retrieval.

---

# 86. RP-PRV-005 — Full Private Evidence in Broker Events

**Status:** `REJECT`

Do not publish:

```text
identity scans
receipts
full serials
IMEI
challenge answers
private photographs
```

into the event bus.

Publish references and safe metadata.

---

# 87. RP-PRV-006 — Full Document Snapshot in Audit Log

**Status:** `REJECT`

Blindly copying entire before/after payloads can duplicate secrets into long-retention audit storage.

---

# 88. RP-PRV-007 — Exact Finder Location Publicly Exposed

**Status:** `REJECT`

Precise location may expose:

```text
finder
home
workplace
custody location
```

Use controlled precision and disclosure policy.

---

# 89. RP-PRV-008 — Full Identity Numbers in Public Listings

**Status:** `REJECT`

Examples:

```text
passport number
national ID
IMEI
bank card number
```

must never be publicly rendered.

---

# 90. RP-PRV-009 — Encryption-at-Rest Called E2EE

**Status:** `REJECT`

Cloud provider encryption and TLS are not end-to-end encryption.

---

# 91. RP-PRV-010 — Claiming E2EE Without Client-Side Cryptography

**Status:** `REJECT`

Do not claim E2EE unless:

```text
client keys
key exchange
client encryption
ciphertext storage
client decryption
```

are actually implemented.

---

# 92. RP-PRV-011 — Biometric Data Retained Without Explicit Policy

**Status:** `REJECT`

Face landmarks, descriptors, or biometric embeddings require explicit retention and purpose limitations.

---

# 93. RP-PRV-012 — Proof Evidence Deletable After Review Without Trace

**Status:** `REJECT`

Use versioning, retention state, or explicit withdrawal rather than silent destruction.

---

# 94. Data Rejected Patterns

## RP-DAT-001 — One Giant Item Document

**Status:** `REJECT`

Do not combine:

```text
item
reports
claims
matches
verification
custody
messages
```

into one mutable document.

---

# 95. RP-DAT-002 — Item Equals Report

**Status:** `REJECT`

The physical item is not equivalent to a user's report of it.

---

# 96. RP-DAT-003 — Report Equals Fingerprint

**Status:** `REJECT`

Reports contain human evidence.

Fingerprints contain derived structured/AI evidence.

---

# 97. RP-DAT-004 — Match Equals Claim

**Status:** `REJECT`

A candidate match does not constitute an ownership claim.

---

# 98. RP-DAT-005 — Claim Equals Recovery

**Status:** `REJECT`

A claim may be:

```text
pending
invalid
fraudulent
withdrawn
rejected
```

---

# 99. RP-DAT-006 — Generic Last-Write-Wins Across Offline Nodes

**Status:** `REJECT`

Recovery state has semantic conflicts.

Example:

```text
Node A: item transferred to courier
Node B: item released to claimant
```

cannot be resolved by whichever timestamp is later.

---

# 100. RP-DAT-007 — Mutable Custody History

**Status:** `REJECT`

Historical custody transitions must be append-oriented.

---

# 101. RP-DAT-008 — Mutable AI Decision Evidence

**Status:** `REJECT`

A later model run must not overwrite the evidence that supported an earlier human decision.

---

# 102. RP-DAT-009 — JSONB for Every Domain Object

**Status:** `REJECT`

JSONB is useful for flexible metadata.

It should not replace strongly modeled searchable fields or relational constraints.

---

# 103. RP-DAT-010 — Permanent Derived Data Without Regeneration Metadata

**Status:** `REJECT`

Embeddings, OCR, thumbnails, translations, and search documents are derived data.

They must retain lineage and regeneration capability.

---

# 104. RP-DAT-011 — Plaintext Search Index of Protected IDs

**Status:** `REJECT`

Use controlled searchable representations.

---

# 105. RP-DAT-012 — Duplicate Canonical State for Convenience

**Status:** `REJECT`

Read performance should use projections rather than multiple independently mutable authoritative copies.

---

# 106. Event Architecture Rejected Patterns

## RP-EVT-001 — Database Update Followed by Best-Effort Publish

**Status:** `REJECT`

Rejected:

```text
commit DB
publish Kafka
```

without an outbox.

If publish fails, state and events diverge.

---

# 107. RP-EVT-002 — Event Publish Before Transaction Commit

**Status:** `REJECT`

Consumers may act on data that later rolls back.

---

# 108. RP-EVT-003 — Exactly-Once End-to-End Assumption

**Status:** `REJECT`

Dzoka assumes:

```text
at-least-once delivery
+
idempotent consumers
```

not magical global exactly-once semantics.

---

# 109. RP-EVT-004 — Consumer Without Idempotency

**Status:** `REJECT`

Duplicate delivery must not:

```text
send duplicate reward
double transfer custody
double notify courier
double increment reputation
```

---

# 110. RP-EVT-005 — Global Ordering Assumption

**Status:** `REJECT`

Kafka-like systems do not provide universal ordering across all partitions.

Ordering must be scoped to the relevant key.

---

# 111. RP-EVT-006 — Secret Data in Event Payload

**Status:** `REJECT`

Events should generally reference sensitive resources rather than embedding them.

---

# 112. RP-EVT-007 — Broker as Authoritative Audit Store

**Status:** `REJECT`

Kafka retention is not Dzoka's permanent business/audit history.

---

# 113. RP-EVT-008 — Command Presented as Event

**Status:** `REJECT`

Distinguish:

```text
MatchRequested
```

from:

```text
MatchCompleted
```

Request and fact have different semantics.

---

# 114. RP-EVT-009 — Infinite Retry

**Status:** `REJECT`

Poison messages must eventually reach:

```text
DLQ
manual review
```

rather than consume resources forever.

---

# 115. RP-EVT-010 — Retrying Permanent Failures

**Status:** `REJECT`

Do not retry forever on conditions such as:

```text
schema invalid
authorization denied
resource permanently deleted
```

---

# 116. Federation Rejected Patterns

## RP-FED-001 — Central Database Shared by All Institutions

**Status:** `REJECT` as federation model

Dzoka federation means independently operated nodes can cooperate.

It is not merely:

```text
multiple institutions
→ same database
```

---

# 117. RP-FED-002 — Raw Score Global Sort

**Status:** `REJECT`

Different nodes may produce incomparable scores.

Use:

```text
rank fusion
source-aware ordering
confidence bands
node metadata
```

---

# 118. RP-FED-003 — Full Record Replication by Default

**Status:** `REJECT`

Federation should minimize data sharing.

Prefer:

```text
safe search representation
candidate summary
capabilities
authorized follow-up
```

---

# 119. RP-FED-004 — Peer Trust by URL Alone

**Status:** `REJECT`

Federated peers require authenticated identity.

---

# 120. RP-FED-005 — Unsigned Federation Messages

**Status:** `REJECT`

Important cross-node messages require integrity and replay protection.

---

# 121. RP-FED-006 — No Circuit Breaker

**Status:** `REJECT`

One slow peer must not stall an entire federated search.

---

# 122. RP-FED-007 — No Per-Peer Timeout

**Status:** `REJECT`

Federation operates across unreliable networks.

---

# 123. RP-FED-008 — Remote Candidate Becomes Local Truth

**Status:** `REJECT`

Remote candidate evidence remains:

```text
remote evidence
```

until appropriately verified.

---

# 124. RP-FED-009 — Federation Bus Exposed Directly to Internal Kafka

**Status:** `REJECT`

Untrusted external federation should pass through the federation gateway/protocol boundary.

---

# 125. RP-FED-010 — PII Broadcasting

**Status:** `REJECT`

Do not broadcast claimant or finder identities to every node merely to discover a match.

---

# 126. Messaging Rejected Patterns

## RP-MSG-001 — Messaging Provider as Domain Model

**Status:** `REJECT`

Do not model Dzoka around:

```text
WhatsAppMessage
FCMMessage
TwilioMessage
```

at the domain level.

Model:

```text
Conversation
Message
Notification
DeliveryAttempt
```

then adapt to providers.

---

# 127. RP-MSG-002 — Channel-Specific Business Logic

**Status:** `REJECT`

WhatsApp must not have a different ownership workflow from web or mobile.

All channels invoke the same application commands.

---

# 128. RP-MSG-003 — Conversation Without Recovery Context

**Status:** `REJECT`

A user should not normally open arbitrary conversations with another user solely from a visible listing.

---

# 129. RP-MSG-004 — Raw Vendor Webhook as Domain Event

**Status:** `REJECT`

Normalize provider webhooks into Dzoka commands/events first.

---

# 130. RP-MSG-005 — No Webhook Signature Verification

**Status:** `REJECT`

Inbound provider webhooks require validation where supported.

---

# 131. RP-MSG-006 — Provider Message ID as Primary Domain Identity

**Status:** `REJECT`

External IDs belong in integration metadata, not as Dzoka's canonical identity.

---

# 132. RP-MSG-007 — User Phone Number as Conversation Identity

**Status:** `REJECT`

Phone number is a channel identifier, not Dzoka's canonical user identity.

---

# 133. CCTV / Edge Rejected Patterns

## RP-CCTV-001 — Upload All CCTV Continuously to Central Cloud

**Status:** `REJECT-BY-DEFAULT`

This is:

```text
privacy-heavy
bandwidth-heavy
storage-heavy
operationally expensive
```

Dzoka should prefer edge processing and upload only relevant evidence where possible.

---

# 134. RP-CCTV-002 — Full-Day Frame-by-Frame Search

**Status:** `REJECT`

Use coarse-to-fine temporal search where possible.

The CCTV Theft Finder audit demonstrated the usefulness of:

```text
day/night
→ hour
→ minute
→ second
```

narrowing.

---

# 135. RP-CCTV-003 — Movement Prediction as Factual Custody

**Status:** `REJECT`

Predicted movement must never appear as:

```text
Item is at Station B
```

unless observed.

Represent:

```text
predicted next location
probability
model version
generatedAt
```

---

# 136. RP-CCTV-004 — Human Face Identification as Default CCTV Capability

**Status:** `REJECT-BY-DEFAULT`

Dzoka is an item-recovery network, not a general biometric surveillance system.

---

# 137. RP-CCTV-005 — Infinite Raw CCTV Retention

**Status:** `REJECT`

Raw footage should obey short, explicit retention policies unless legally preserved.

---

# 138. RP-CCTV-006 — Edge Node Trusted as Central Authority

**Status:** `REJECT`

Edge evidence requires provenance and may require central verification.

---

# 139. Rewards / Payments Rejected Patterns

## RP-RWD-001 — Blockchain Escrow as Default

**Status:** `REJECT-BY-DEFAULT`

The OpenEscrow audit showed useful state-machine ideas, but Dzoka does not require blockchain to manage rewards.

---

# 140. RP-RWD-002 — Reward Released on Match

**Status:** `REJECT`

A match is not a completed recovery.

---

# 141. RP-RWD-003 — Reward Released on Claim Approval

**Status:** `REJECT`

The item may still fail to reach the claimant.

Default release should follow confirmed recovery according to policy.

---

# 142. RP-RWD-004 — Floating-Point Money

**Status:** `REJECT`

Money should use:

```text
decimal / integer minor units
+
currency
```

not binary floating point.

---

# 143. RP-RWD-005 — Payment Provider State as Dzoka Ledger

**Status:** `REJECT`

Provider responses must be reconciled into Dzoka's own reward/payment ledger.

---

# 144. Architecture Rejected Patterns

## RP-ARC-001 — Microservices by Default

**Status:** `REJECT`

Dzoka does not split every bounded context into an independent service merely because microservices appear scalable.

Default core:

```text
modular monolith
```

with separate containers only where justified by:

```text
scaling
runtime
hardware
trust boundary
deployment lifecycle
failure isolation
```

---

# 145. RP-ARC-002 — Monolith Containing Heavy AI

**Status:** `REJECT`

GPU/ML workloads should not be tightly embedded inside the Java transactional core.

---

# 146. RP-ARC-003 — AI Provider Hard-Coded into Domain

**Status:** `REJECT`

No domain code should fundamentally depend on:

```text
Gemini
OpenAI
Claude
TensorFlow Serving
```

as its identity.

Use provider ports.

---

# 147. RP-ARC-004 — Vector Database Hard-Coded into Domain

**Status:** `REJECT`

Dzoka uses a:

```text
VectorSearchProvider
```

boundary.

Default V1 may be pgvector, with Qdrant as optional scale-out.

---

# 148. RP-ARC-005 — Graph Database Mandatory on Day One

**Status:** `REJECT`

The recovery graph should initially be represented relationally unless measured requirements justify a specialized graph database.

---

# 149. RP-ARC-006 — Separate Database Per Feature Without Need

**Status:** `REJECT`

Adding technologies solely because they are fashionable increases operational complexity.

---

# 150. RP-ARC-007 — Kafka Across Every Internal Function Call

**Status:** `REJECT`

Synchronous domain operations should stay synchronous where transactional consistency matters.

Events are not a replacement for function/module boundaries.

---

# 151. RP-ARC-008 — Direct Database Access Across Containers

**Status:** `REJECT-BY-DEFAULT`

Containers should communicate through defined ownership boundaries rather than reaching into another service's private tables.

The modular core may share a transactional database internally; external deployables should not casually bypass it.

---

# 152. RP-ARC-009 — One Service Per Domain Noun

**Status:** `REJECT`

Bounded contexts do not automatically imply deployable services.

---

# 153. RP-ARC-010 — Provider Lock-In Hidden in Core

**Status:** `REJECT`

Avoid direct assumptions that Dzoka requires:

```text
Firebase
AWS
Google Cloud
Twilio
Cloudinary
Qdrant
```

to function.

---

# 154. Operations Rejected Patterns

## RP-OPS-001 — No Dead-Letter Queue

**Status:** `REJECT`

Asynchronous processing needs failure isolation.

---

# 155. RP-OPS-002 — Silent Failure Returning Empty Results

**Status:** `REJECT`

Patterns such as:

```text
catch exception
return []
```

can disguise broken matching as "no matches".

Errors should be observable.

---

# 156. RP-OPS-003 — Marking Work Complete Before Work Completes

**Status:** `REJECT`

Example:

```text
imageProcessed = true
```

before OCR/redaction/embedding genuinely succeed.

---

# 157. RP-OPS-004 — No Model Failure State

**Status:** `REJECT`

AI tasks need states such as:

```text
PENDING
RUNNING
SUCCEEDED
FAILED
SKIPPED
EXPIRED
```

---

# 158. RP-OPS-005 — No Correlation IDs

**Status:** `REJECT`

Distributed recovery workflows require end-to-end traceability.

---

# 159. RP-OPS-006 — No Outbox Monitoring

**Status:** `REJECT`

Outbox backlog must be observable.

---

# 160. RP-OPS-007 — No Consumer Lag Monitoring

**Status:** `REJECT`

Event processing latency affects matching and recovery experience.

---

# 161. RP-OPS-008 — Logs as Audit History

**Status:** `REJECT`

Application logs are not authoritative business audit records.

---

# 162. RP-OPS-009 — Production State Based on README

**Status:** `REJECT`

A README statement such as:

```text
production ready
```

does not prove production readiness.

---

# 163. Documentation / Source Reuse Rejected Patterns

## RP-SRC-001 — Architecture from README Alone

**Status:** `REJECT`

Source audits must inspect actual implementation.

---

# 164. RP-SRC-002 — Reuse Before License Verification

**Status:** `REJECT`

No source should be adopted or copied until its license is confirmed.

---

# 165. RP-SRC-003 — README License Claim Without License File

**Status:** `REJECT` for direct copying

Several audited repositories claim permissive licensing in README while lacking a clear root license.

Until resolved:

```text
REFERENCE ONLY
```

---

# 166. RP-SRC-004 — Copying "All Rights Reserved" Source

**Status:** `REJECT`

Architecture ideas may be independently learned from, but source code must not be copied without permission.

---

# 167. RP-SRC-005 — AGPL Code Into Permissive Core Without Decision

**Status:** `REJECT-BY-DEFAULT`

Examples from audits include AGPL-licensed components.

Use may be appropriate operationally or as separate services, but requires explicit license architecture consideration.

---

# 168. RP-SRC-006 — GPL Component Copied Into Core Without License Analysis

**Status:** `REJECT-BY-DEFAULT`

Algorithms may be studied conceptually, but direct incorporation requires license review.

---

# 169. RP-SRC-007 — Unpinned Upstream Dependency Audit

**Status:** `REJECT`

Audits must identify an exact:

```text
commit SHA
```

not merely:

```text
main
latest
```

---

# 170. RP-SRC-008 — "Exists in Repository" Equals "Used in Runtime"

**Status:** `REJECT`

FindBack demonstrated why this is dangerous.

The repository may contain multiple implementations, but deployment configuration determines what actually runs.

---

# 171. RP-SRC-009 — README Checkbox Equals Implemented

**Status:** `REJECT`

Always compare claims against:

```text
source
dependencies
configuration
tests
deployment
```

---

# 172. RP-SRC-010 — Documentation Security Claim Accepted Without Source

**Status:** `REJECT`

Claims such as:

```text
encrypted
E2EE
secure
AI-powered
fraud detection
```

must be verified.

---

# 173. RP-SRC-011 — Source Audit Without Files Inspected

**Status:** `REJECT`

Every source audit must record important inspected paths.

---

# 174. RP-SRC-012 — Source Audit Without Final Reuse Classification

**Status:** `REJECT`

Every audited repository must end with one or more of:

```text
ADOPT
ADAPT
REFERENCE
REJECT
```

---

# 175. Specific Patterns Observed Across Audited Repositories

## globalenglish01/lostfound

Primarily a positive reference.

It reinforces rejection of:

```text
missing evidence = mismatch
similarity-only matching
model-unaware vectors
raw-description destruction
```

---

# 176. FuricBond/Lost-And-Found

Useful retrieval concepts, but the audit reinforced:

```text
README license claims without confirmed root license
```

as unsuitable for direct code reuse until resolved.

---

# 177. Lost-and-Found-Tracking-System

Useful claims/fraud workflow.

Reject:

```text
secret-like hard-coded fallback credentials
```

and any pattern where production secrets can be committed as defaults.

---

# 178. trishthakur/lost-and-found-ai

Reject as architectural foundation due to patterns including:

```text
unsafe eval
weak password handling
toy persistence
prototype-level architecture
unclear licensing
```

---

# 179. Smart Asset Finder

Useful ideas around:

```text
QR
WhatsApp
rewards
```

but source licensing is restrictive.

Therefore reject:

```text
direct code reuse
```

without permission.

---

# 180. Malak-Rashad/LostAndFound

Useful private-question workflow.

Reject direct code reuse until licensing is resolved.

Also avoid static verification questions becoming predictable across claims.

---

# 181. FindBack

FindBack contributed several explicit rejected patterns:

```text
random image similarity

random image embeddings

missing evidence = score 50

strict category filtering

strict district filtering

claim workflow bypass

claim automatically marks item claimed

Math.random handover code

handover code generated before authorization

self-editable role/reputation

broad client claim mutation

client-created notifications

chat creation without trusted relationship

plaintext messaging marketed as E2EE

incomplete server-side redaction

full before/after audit snapshots

duplicate canonical match records
```

---

# 182. VisualMatch / OpenVisualSearch

Useful retrieval ideas.

Reject relying on projects with unresolved/no license for direct code reuse.

Also reject treating generic visual similarity as sufficient physical-instance identity.

---

# 183. ILIAS / Patchwise Retrieval

These reinforce that local-instance retrieval needs stronger evidence than global semantic embeddings.

Do not collapse patch evidence back into a single unexplained score without retaining provenance.

---

# 184. RerankingTransformer

GPL licensing means direct integration into a permissive Dzoka core requires explicit license analysis.

Default use:

```text
REFERENCE
```

---

# 185. OpenCLIP

Useful dependency.

Still reject:

```text
one OpenCLIP embedding = ItemFingerprint
```

---

# 186. DeepSORT

Useful tracking reference.

Reject treating generic multi-object tracking as sufficient item identity or ownership evidence.

---

# 187. CCTV Theft Finder

Positive source for coarse-to-fine temporal video search.

Reject:

```text
brute-force every frame centrally
```

when more efficient temporal localization is possible.

---

# 188. Trajectron++

Useful research reference.

Reject treating human trajectory prediction assumptions as automatically suitable for lost-property institutional movement.

---

# 189. Mycel

Strong federation ideas.

Its audit reinforces rejecting:

```text
raw global score sorting
no peer timeouts
no circuit breakers
no source attribution
```

AGPL also means its source should not be copied casually into a permissive core.

---

# 190. Open Federated Search

Useful historical architecture reference.

Reject using an archived older platform as the foundation purely because it implements federation terminology.

---

# 191. OpenEPCIS

Useful traceability implementation.

Reject forcing all Dzoka domain semantics directly into EPCIS concepts.

Dzoka maintains its own:

```text
RecoveryEvent
```

model and maps through an adapter.

---

# 192. DPP / DID Experiments

Useful future identity concepts.

Reject making DID/VC mandatory for ordinary lost-property recovery.

---

# 193. OpenEscrow

Useful state-machine inspiration.

Reject blockchain as the default reward or escrow mechanism.

---

# 194. Pgvector / Qdrant

Both useful.

Reject requiring Qdrant from the first deployment when PostgreSQL + pgvector adequately covers the initial system.

---

# 195. Ultralytics

Useful technically, but AGPL licensing should not silently become a mandatory Dzoka core dependency.

Prefer permissive defaults where possible.

---

# 196. Security Gate Checklist

Before approving any implementation, ask:

```text
Can the client modify an authoritative state directly?

Can a user modify their role?

Can a user modify reputation?

Can a claimant approve themselves?

Can anyone create trusted system notifications?

Can private evidence be read by unintended users?

Can a secret token be guessed or replayed?

Can proof disappear after review?

Can one tenant access another tenant's data?

Can a federation peer impersonate another node?
```

If yes, implementation must not ship.

---

# 197. Matching Gate Checklist

Before accepting any matcher, ask:

```text
Does missing evidence become a fake score?

Are hard conflicts represented?

Are model versions preserved?

Can different embedding spaces be accidentally compared?

Is candidate recall destroyed by category/location gates?

Is event time confused with report creation time?

Does score mean ranking or probability?

Is evidence coverage visible?

Can the same pair get different scores due to randomness?
```

---

# 198. AI Gate Checklist

Before accepting an AI subsystem, ask:

```text
What model generated this?

Which version?

What input?

Can the result be reproduced?

What happens when the model is unavailable?

Does AI control a legal/security/business authority?

Are model outputs separated from ground truth?

Can human reviewers see the evidence?

Can the model output leak private verification evidence?
```

---

# 199. Privacy Gate Checklist

Before accepting a media/evidence workflow, ask:

```text
Who can see the original?

Who can see the sanitized derivative?

Does OCR contain PII?

Are identifiers searchable safely?

Are audit records duplicating secrets?

Are biometric features being generated?

What is retained?

For how long?

Can derived artifacts be deleted with the source?
```

---

# 200. Federation Gate Checklist

Before accepting federation logic, ask:

```text
How is the node authenticated?

How is a message signed?

How is replay prevented?

How much PII is shared?

Are remote scores comparable?

What happens if a peer is slow?

What happens if a peer is malicious?

Can a remote record become local truth without verification?

Can a node revoke another node's trust?
```

---

# 201. Recovery Gate Checklist

Before a physical item can leave custody, Dzoka must be able to answer:

```text
Which item is this?

Who currently holds it?

Who is entitled to collect it?

How was that entitlement verified?

Who authorized the release?

What credential authorizes this handover?

Has the credential expired?

Has it already been used?

Who received the item?

Was the transfer recorded?
```

If these cannot be answered, release must not be treated as a completed Dzoka recovery.

---

# 202. Patterns Allowed Only in Tests

The following may appear only in dedicated test/demo code:

```text
random match scores
fake embeddings
mock ownership decisions
static OTP values
fake courier status
fake payment success
synthetic CCTV detections
hard-coded demo users
```

They must be:

```text
clearly isolated
clearly named
impossible to activate accidentally in production
```

---

# 203. Never Cross This Boundary

The following states require trusted authority:

```text
ClaimApproved
VerificationPassed
RecoveryAuthorized
CustodyTransferred
RecoveryCompleted
RewardReleased
FederationNodeTrusted
```

No UI client, uncontrolled AI model, external webhook, or untrusted node may set them directly.

---

# 204. Source Audit Rule

Whenever an upstream repository introduces a useful capability, Dzoka asks two questions:

```text
What should we learn from this?

What should we refuse to inherit from this?
```

Both answers must be recorded.

---

# 205. Exceptions

A rejected pattern may only be reconsidered through an ADR containing:

```text
context
specific rejected pattern
reason for reconsideration
alternatives
security impact
privacy impact
operational impact
licensing impact
migration strategy
rollback strategy
final decision
```

---

# 206. Definition of Rejection

`REJECT` does not mean:

```text
the upstream project is bad
```

It means:

```text
this pattern does not satisfy Dzoka's requirements
```

A project may still provide valuable:

```text
algorithms
UX ideas
data models
research
workflow concepts
testing ideas
```

while containing patterns Dzoka intentionally avoids.

---

# 207. Final Rule

Dzoka must never trade recoverability, privacy, ownership integrity, or custody safety for the appearance of AI sophistication.

The hierarchy is:

```text
CORRECTNESS
    ↓
TRUST
    ↓
SECURITY
    ↓
PRIVACY
    ↓
AUDITABILITY
    ↓
RECOVERY SUCCESS
    ↓
PERFORMANCE
    ↓
CONVENIENCE
    ↓
NOVELTY
```

The most important rule remains:

> **A system that confidently returns the wrong physical item to the wrong person is worse than a system that admits uncertainty.**

Dzoka therefore rejects any design that hides uncertainty, fabricates evidence, weakens ownership verification, bypasses custody authority, or turns AI confidence into unquestioned truth.