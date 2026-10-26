> **Current architecture authority (2026-10-26):** [Accepted baseline](../architecture/baseline.md) and its linked ADRs govern implementation. This supplied document is preserved below as detailed research/design material. Where examples or diagrams conflict with the baseline, use the baseline; no runtime capability is implied.

# Ownership Verification Design

> **Dzoka — The Open Recovery Network**

This document defines how Dzoka verifies that a claimant is legitimately entitled to recover a matched physical item.

The ownership verification subsystem sits **after matching** and **before recovery authorization**.

Its purpose is to answer:

> **Does this claimant have sufficient private, independent, and trustworthy evidence to recover this item?**

It must never reduce ownership to:

```text
high match score
```

or:

```text
LLM says answer looks correct
```

Instead, Dzoka combines:

- private item attributes;
- protected identifiers;
- prior photographs;
- receipts;
- institutional evidence;
- user knowledge;
- AI-generated challenge questions;
- fraud signals;
- deterministic policy;
- human review where required.

The result is an auditable `VerificationDecision`.

---

# 1. Core Principle

The verification system is built around this separation:

```text
MATCHING
"Is this probably the same item?"
        ↓
CLAIM
"This person says the item is theirs."
        ↓
VERIFICATION
"Can they demonstrate legitimate knowledge/evidence?"
        ↓
RECOVERY AUTHORIZATION
"May the item actually be released?"
```

These steps MUST remain separate.

---

# 2. Goals

The verification subsystem must:

1. reduce fraudulent claims;
2. avoid leaking ownership answers to claimants;
3. work even when no serial number exists;
4. support low-value and high-value items;
5. support multiple evidence types;
6. support partial evidence;
7. handle competing claimants;
8. remain explainable;
9. preserve human oversight;
10. protect personal information;
11. support institutional policies;
12. support federated cases;
13. support authorized representatives;
14. remain auditable;
15. produce ML feedback without letting ML become final authority.

---

# 3. Non-Goals

The verification engine does not:

- perform initial item matching;
- decide whether a found item visually resembles a lost item;
- release property directly;
- expose secret attributes;
- assume claimant identity equals legal owner;
- rely solely on LLM judgment;
- require formal receipts for every item;
- require government ID for every item;
- create a surveillance identity system.

---

# 4. Verification Pipeline

The high-level flow is:

```text
Likely Match
    ↓
Claim Submitted
    ↓
Claim Eligibility Check
    ↓
Evidence Inventory
    ↓
Challenge Planning
    ↓
Private Challenge(s)
    ↓
Evidence Evaluation
    ↓
Fraud / Risk Assessment
    ↓
Verification Policy
    ↓
Human Review if Required
    ↓
Verification Decision
    ↓
Recovery Authorization
```

---

# 5. Core Domain Objects

The primary objects are:

```text
Claim
VerificationSession
OwnershipChallenge
ChallengeResponse
VerificationEvidence
RiskAssessment
VerificationDecision
RecoveryEntitlement
```

---

# 6. `VerificationSession`

Canonical structure:

```text
VerificationSession
├── verificationId
├── claimId
├── itemId
├── candidateId
├── tenantId
├── organizationId
├── status
├── policyVersion
├── challengePlanVersion
├── challenges[]
├── evidence[]
├── riskAssessmentId
├── decision
├── startedAt
├── completedAt
└── expiresAt
```

---

# 7. Verification Status

Recommended lifecycle:

```text
PENDING
    ↓
EVIDENCE_ASSESSMENT
    ↓
CHALLENGE_REQUIRED
    ↓
IN_PROGRESS
    ↓
RISK_REVIEW
    ↓
POLICY_EVALUATION
    ↓
HUMAN_REVIEW
    ↓
VERIFIED
```

Alternative terminal states:

```text
REJECTED
INCONCLUSIVE
EXPIRED
CANCELLED
```

---

# 8. Claim Preconditions

A verification session may only begin when:

```text
valid Claim exists
item is recoverable
candidate exists where applicable
claimant is authorized to make claim
case is active
```

Matching confidence may influence the verification path, but matching cannot replace verification.

---

# 9. Verification Evidence Categories

Dzoka should support evidence in several classes.

```text
Knowledge Evidence
Protected Identifier Evidence
Visual Evidence
Documentary Evidence
Possession History Evidence
Institutional Evidence
Digital Item Identity Evidence
Behavioural / Risk Evidence
Human Review Evidence
```

---

# 10. Knowledge Evidence

Knowledge evidence tests facts a legitimate owner is likely to know.

Examples:

```text
contents of bag
hidden scratch
internal sticker
missing component
engraving
phone wallpaper
case accessory
custom modification
unusual wear
```

This is particularly useful for ordinary possessions without formal ownership documents.

---

# 11. Protected Identifier Evidence

Strong identifiers may include:

```text
serial number
IMEI
enterprise asset ID
document number
registered Dzoka tag
GS1-linked identifier
```

These values must remain protected.

Verification should normally compare:

```text
normalized secure representation
```

instead of exposing the stored original.

---

# 12. Documentary Evidence

Possible documents:

```text
purchase receipt
invoice
warranty
insurance document
service record
asset register record
registration document
```

Documents should be treated as evidence, not automatic truth.

They may themselves be fraudulent.

---

# 13. Prior Photograph Evidence

Pre-loss photographs can be very strong.

Examples:

```text
old photo of laptop
photo showing same sticker
photo showing same crack
photo showing same customized cover
```

The system should preserve provenance such as:

```text
uploaded before loss
uploaded during claim
linked registered item
external trusted source
```

Evidence uploaded before the loss/report is usually stronger than evidence uploaded after public listing information became available.

---

# 14. Institutional Evidence

Organizations may hold evidence such as:

```text
asset register
hotel guest record
airport baggage record
university inventory
employer laptop register
bus ticket / seat record
repair/service history
```

Institutional evidence reliability should be explicit.

---

# 15. Recovery Entitlement

Ownership and entitlement must remain distinct.

A claimant may legitimately collect for someone else.

Examples:

```text
parent for child
employee for employer
authorized representative
executor
guardian
organization representative
```

Represent:

```text
RecoveryEntitlement
├── claimantId
├── ownerReference
├── entitlementType
├── supportingEvidence[]
├── validityPeriod
├── status
└── verifiedBy
```

---

# 16. Entitlement Types

Possible values:

```text
OWNER
PARENT_OR_GUARDIAN
AUTHORIZED_REPRESENTATIVE
EMPLOYEE_OR_AGENT
ORGANIZATION_CUSTODIAN
EXECUTOR
OTHER
```

---

# 17. Challenge Engine

A core Dzoka capability is:

```text
OwnershipChallengeService
```

Its responsibility is to create private verification questions based on evidence that is:

- discriminative;
- not public;
- sufficiently reliable;
- safe to ask;
- answerable by the legitimate claimant.

---

# 18. Challenge Sources

Challenge candidates may come from:

```text
secret distinctive features
protected identifiers
private OCR
finder-supplied hidden details
institution-supplied hidden details
registered item history
pre-loss photographs
contents
configuration/customization
```

---

# 19. Challenge Candidate

Canonical structure:

```text
ChallengeCandidate
├── evidenceId
├── challengeType
├── discriminativeScore
├── evidenceConfidence
├── sourceReliability
├── disclosureRisk
├── answerability
├── priorDisclosureStatus
├── recommendedTemplate
└── eligibility
```

---

# 20. Challenge Types

Possible types:

```text
PRIVATE_ATTRIBUTE
DISTINCTIVE_VISUAL_FEATURE
SERIAL_FRAGMENT
IMEI_FRAGMENT
CONTENTS
PURCHASE_DETAIL
PRIOR_PHOTO_DETAIL
DEVICE_CONFIGURATION
ACCESSORY_DETAIL
ENGRAVING
INSTITUTIONAL_DETAIL
CUSTOM
```

---

# 21. Challenge Selection Requirements

A challenge may be selected only when:

```text
evidence exists
AND evidence is private
AND confidence is sufficient
AND disclosure risk is acceptable
AND it has not already been disclosed
AND policy permits use
```

---

# 22. Bad Challenge Example

Hidden evidence:

```text
yellow sticker underneath laptop
```

Bad question:

```text
"Does the laptop have a yellow sticker underneath?"
```

This leaks the answer.

---

# 23. Better Challenge Example

```text
"What distinguishing feature is located underneath the device?"
```

This tests claimant knowledge without revealing the fact.

---

# 24. Another Example

Hidden evidence:

```text
missing right rear rubber foot
```

Challenge:

```text
"Is any part of the laptop's base missing or damaged? If so, describe it."
```

---

# 25. Public Disclosure Guard

Before challenge generation, query:

```text
EvidenceDisclosureHistory
```

If the claimant has already seen the evidence publicly or through a prior message, it must not be considered independent secret proof.

---

# 26. Disclosure Sources

Evidence may have been disclosed through:

```text
public listing
candidate preview
staff message
previous challenge
WhatsApp reply
support interaction
federation response
```

Track all relevant disclosure.

---

# 27. Disclosure Record

```text
EvidenceDisclosure
├── evidenceId
├── recipientOrAudience
├── channel
├── disclosureType
├── disclosedAt
├── caseId
└── actor
```

---

# 28. Challenge Generation Roles

Separate:

```text
ChallengeCandidateSelector
ChallengeTemplateRenderer
LLMChallengeGenerator
ChallengeSafetyValidator
DisclosureGuard
```

The LLM must not have sole responsibility for deciding whether evidence is safe to disclose.

---

# 29. LLM Challenge Role

An LLM may transform structured private evidence into natural language.

Input:

```text
feature type
general location on object
safe abstraction
language
```

It should not necessarily receive unrestricted private case data.

---

# 30. LLM Safety Contract

The LLM must be instructed to:

```text
not reveal the answer
not reveal protected identifiers
not invent features
not infer ownership
not ask unnecessary personal questions
```

Its output must pass deterministic validation.

---

# 31. Challenge Safety Validator

Checks:

```text
does question contain answer?
does it reveal serial/IMEI?
does it reveal secret feature directly?
is it overly specific?
is it based on real evidence?
has the feature already been disclosed?
```

Unsafe generated questions are rejected.

---

# 32. Challenge Difficulty

Challenges should be neither:

```text
trivial
```

nor:

```text
unreasonably impossible
```

Example:

Too easy:

```text
"What colour is the black backpack?"
```

Too hard:

```text
"Give the precise millimetre length of a hidden scratch."
```

Good:

```text
"Describe any unusual marking near the charging ports."
```

---

# 33. Challenge Diversity

Do not ask several questions derived from the same evidence.

Example:

```text
scratch exists
scratch colour
scratch location
```

may all stem from one feature.

Prefer independent evidence dimensions.

---

# 34. Challenge Count

The number of challenges should depend on:

```text
item value/risk
evidence quality
match confidence
claim risk
organization policy
competing claims
```

Do not use one fixed number globally.

---

# 35. Risk-Based Verification

Example:

```text
Low-value umbrella
strong private feature
low-risk claimant
→ lightweight verification

High-value laptop
multiple claims
→ identifiers + private features + human review

Passport
→ strict policy and staff-controlled release
```

---

# 36. Claimant Response

Canonical:

```text
ChallengeResponse
├── responseId
├── challengeId
├── claimantId
├── rawResponse
├── submittedAt
├── channel
├── interpretedResponse
├── interpretationProvenance
└── evaluation
```

Always preserve `rawResponse`.

---

# 37. Response Evaluation

Possible evaluation methods:

```text
EXACT
NORMALIZED_EXACT
FUZZY
SEMANTIC
VISUAL
DOCUMENTARY
HUMAN
```

---

# 38. Exact Comparison

Best for:

```text
serial fragments
known number
specific code
registered tag
```

Use protected comparison where possible.

---

# 39. Fuzzy Comparison

Useful for:

```text
brand names
short phrases
minor spelling errors
```

Must not be used for strong identifiers where exactness matters.

---

# 40. Semantic Comparison

Useful for free-text answers.

Expected evidence:

```text
yellow sticker under laptop
```

Claimant:

```text
"there is a small yellow label underneath"
```

Semantic interpretation may correctly identify agreement.

---

# 41. Semantic Evaluation Limit

The semantic model does not decide ownership.

It outputs:

```text
ResponseInterpretationEvidence
```

such as:

```text
CONSISTENT
PARTIALLY_CONSISTENT
INCONSISTENT
UNCERTAIN
```

with confidence.

---

# 42. Response Interpretation Contract

```text
ResponseInterpretation
├── normalizedMeaning
├── relation
├── score
├── confidence
├── modelProvenance
├── supportingEvidence
└── warnings[]
```

---

# 43. Human Evaluation

Some responses require human judgment.

Examples:

```text
receipt image
old family photograph
custom engraving description
complex authorization letter
```

Use:

```text
HumanReviewRequired
```

rather than forcing an AI answer.

---

# 44. Verification Evidence

All evidence is converted into a canonical structure.

```text
VerificationEvidence
├── evidenceId
├── verificationId
├── type
├── source
├── result
├── score
├── reliability
├── confidence
├── independenceGroup
├── classification
├── provenance
└── explanation
```

---

# 45. Evidence Results

Possible:

```text
STRONG_SUPPORT
SUPPORT
WEAK_SUPPORT
NEUTRAL
WEAK_CONFLICT
CONFLICT
CRITICAL_CONFLICT
UNAVAILABLE
```

---

# 46. Independence Groups

Avoid double-counting evidence from one root source.

Example:

```text
receipt OCR
receipt text extraction
receipt serial detection
```

all come from the same receipt.

Assign:

```text
independenceGroup = receipt_123
```

so the policy knows they are correlated.

---

# 47. Evidence Reliability

Possible conceptual hierarchy:

```text
trusted registered identifier
    >
trusted institutional record
    >
pre-loss registered evidence
    >
staff-confirmed private observation
    >
private claimant knowledge
    >
OCR
    >
vision inference
    >
generic LLM interpretation
```

Exact values are configurable.

---

# 48. Verification Evidence Categories

A useful grouping is:

```text
IDENTIFIER
PRIVATE_KNOWLEDGE
VISUAL_PROVENANCE
DOCUMENT
INSTITUTIONAL
ENTITLEMENT
BEHAVIOURAL
HUMAN_REVIEW
```

---

# 49. Verification Score

Dzoka may compute an internal evidence score.

Conceptually:

```text
VerificationEvidenceScore =
Σ(resultScore × reliability × independenceAdjustment × weight)
──────────────────────────────────────────────────────────────
Σ(available effective weights)
```

But this score alone MUST NOT be the final decision.

---

# 50. Verification Coverage

Track:

```text
verificationCoverage
```

separately.

Example:

```text
score = .96
coverage = .20
```

should not produce high certainty based on one weak answer.

---

# 51. Identity-Grade Verification Evidence

Examples:

```text
verified full serial
verified IMEI
pre-registered ownership record
strong pre-loss same-instance photo
multiple independent private features
```

Policy may require at least one identity-grade evidence source for certain item classes.

---

# 52. Critical Verification Conflicts

Examples:

```text
claimant gives wrong verified serial
submitted receipt serial contradicts item
institutional ownership record names another owner
claimant's alleged pre-loss photo is demonstrably created after public disclosure
```

These can block verification.

---

# 53. Verification Decision Engine

Implement:

```text
VerificationDecisionService
```

It consumes:

```text
VerificationEvidence[]
RiskAssessment
VerificationPolicy
HumanReviewOutcome
EntitlementEvidence
```

and produces:

```text
VerificationDecision
```

---

# 54. Verification Decision

Canonical:

```text
VerificationDecision
├── verificationId
├── outcome
├── confidence
├── evidenceCoverage
├── policyVersion
├── supportingEvidenceIds[]
├── conflictingEvidenceIds[]
├── riskAssessmentId
├── humanReviewer
├── reasonCodes[]
├── explanation
└── decidedAt
```

---

# 55. Decision Outcomes

```text
VERIFIED
REJECTED
INCONCLUSIVE
```

Do not overload with dozens of final result states.

Workflow status carries process detail.

---

# 56. Reason Codes

Examples:

```text
STRONG_IDENTIFIER_MATCH
MULTIPLE_PRIVATE_FEATURES_MATCH
PRE_LOSS_PHOTO_MATCH
DOCUMENTARY_EVIDENCE_CONFIRMED
INSTITUTIONAL_RECORD_CONFIRMED
INSUFFICIENT_PRIVATE_EVIDENCE
CRITICAL_IDENTIFIER_CONFLICT
HIGH_FRAUD_RISK
COMPETING_CLAIM
HUMAN_REVIEW_APPROVED
HUMAN_REVIEW_REJECTED
```

---

# 57. Verification Policy

Policies are tenant/organization aware.

Examples:

```text
electronics over threshold
→ require identity-grade evidence

passport
→ staff verification mandatory

low-value clothing
→ simplified challenge workflow

organization asset
→ institutional asset register required
```

---

# 58. Policy Versioning

Every decision records:

```text
policyVersion
```

Old decisions remain reproducible after policies change.

---

# 59. Policy Evaluation

Conceptually:

```text
VerificationPolicy.evaluate(
    item,
    claim,
    evidenceSummary,
    riskAssessment,
    organizationContext
)
```

returns:

```text
APPROVE
REJECT
REQUIRE_MORE_EVIDENCE
REQUIRE_HUMAN_REVIEW
```

---

# 60. LLM Boundary

LLMs may:

```text
generate safe question wording
interpret free-form answers
summarize evidence
assist reviewer explanation
```

LLMs may not:

```text
return final VERIFIED decision
override critical conflict
bypass policy
authorize recovery
```

---

# 61. Fraud Integration

Verification invokes:

```text
ClaimRiskService
```

Signals may include:

```text
failed challenge attempts
rapid guesses
multiple unrelated claims
same proof reused across cases
account/device abuse
impossible geography
identity conflicts
known fraud patterns
```

---

# 62. Risk Assessment

```text
RiskAssessment
├── riskScore
├── riskLevel
├── signals[]
├── recommendedAction
└── modelRuleVersions
```

---

# 63. Risk Levels

```text
LOW
MEDIUM
HIGH
CRITICAL
```

---

# 64. Risk Actions

```text
ALLOW_STANDARD_FLOW
REQUIRE_ADDITIONAL_EVIDENCE
REQUIRE_HUMAN_REVIEW
BLOCK
```

Risk is never mixed directly into match score.

---

# 65. Challenge Brute-Force Protection

Controls:

```text
attempt limits
cooldowns
rate limits
challenge rotation policy
session expiry
risk escalation
```

Do not allow unlimited guesses at hidden item attributes.

---

# 66. Challenge Attempt History

Store:

```text
challengeId
attemptNumber
submittedAt
result
riskImpact
```

Raw answers may have stricter retention.

---

# 67. Information Leakage Protection

After a failed challenge, do not tell the claimant:

```text
"Wrong. The correct answer was yellow sticker."
```

Use:

```text
"The response could not be verified."
```

Detailed reasons remain internal.

---

# 68. Claim Enumeration Protection

Public interfaces must not reveal whether a specific secret identifier exists in Dzoka.

Example attack:

```text
"Does serial ABC123 exist?"
```

must not produce a useful public oracle.

---

# 69. Receipt Verification

Receipt evidence pipeline:

```text
upload
 ↓
sanitization
 ↓
OCR
 ↓
merchant/date/item extraction
 ↓
serial/model extraction where present
 ↓
consistency checks
 ↓
VerificationEvidence
```

---

# 70. Receipt Limitations

A receipt may demonstrate purchase of:

```text
a model
```

without proving possession of:

```text
this exact physical unit
```

unless a unique identifier is present.

Policy must account for that distinction.

---

# 71. Prior Photo Verification

Pipeline:

```text
claimant photograph
 ↓
metadata/provenance analysis
 ↓
object fingerprint
 ↓
same-instance comparison
 ↓
private feature comparison
 ↓
VerificationEvidence
```

---

# 72. Photo Timing

Photo timing can matter.

Evidence may be stronger if:

```text
known to predate report/public listing
```

than if:

```text
uploaded after candidate details were disclosed
```

Never blindly trust EXIF as proof; metadata can be manipulated.

---

# 73. Photo Provenance Classes

Possible:

```text
REGISTERED_PRE_LOSS
TRUSTED_EXTERNAL_PRE_LOSS
USER_ASSERTED_PRE_LOSS
POST_CLAIM_UPLOAD
UNKNOWN
```

---

# 74. Document Verification

Documents such as government ID may verify:

```text
claimant identity
```

but not necessarily:

```text
item ownership
```

Do not confuse identity verification with ownership evidence.

---

# 75. Claimant Identity vs Ownership

Example:

```text
passport confirms Nyasha is Nyasha
```

It does not prove:

```text
the black backpack belongs to Nyasha
```

Verification policy must combine the correct evidence types.

---

# 76. Organization-Owned Items

For corporate assets:

```text
claimant
+
organization membership
+
asset register
+
authorization
```

may establish entitlement.

---

# 77. Child-Owned Items

For minors, a guardian may establish entitlement through:

```text
guardian relationship
+
private item evidence
+
policy
```

---

# 78. Competing Claims

An item may have:

```text
Claim A
Claim B
Claim C
```

simultaneously.

Dzoka must not auto-release merely because one claimant completes a weak verification first.

---

# 79. Claim Conflict

Represent:

```text
ClaimConflict
├── itemId
├── claims[]
├── detectedAt
├── severity
├── status
└── resolution
```

---

# 80. Competing Claim Policy

Possible behavior:

```text
multiple serious claims
→ freeze recovery authorization
→ require human review
```

---

# 81. Claim Comparison

Reviewers should see a privacy-controlled comparison such as:

```text
Claim A
- verified serial fragment
- private scratch correct
- old photograph consistent

Claim B
- public colour correct
- general description correct
- private challenge wrong
```

---

# 82. Reviewer Interface

Human reviewers need:

```text
candidate match evidence
claim evidence
private challenge results
risk signals
conflicts
policy requirement
entitlement information
```

Do not show irrelevant personal data.

---

# 83. Reviewer Actions

Possible:

```text
APPROVE
REJECT
REQUEST_MORE_EVIDENCE
ESCALATE
```

Every action requires:

```text
reason
actor
timestamp
```

---

# 84. Reviewer Override

Overrides should be explicit.

Store:

```text
automated recommendation
human outcome
override reason
policy authority
```

High-risk overrides may require a second reviewer.

---

# 85. Dual Control

Organizations may configure dual approval for:

```text
high-value items
identity documents
weapons-sensitive property
large rewards
competing claims
```

Dzoka should support:

```text
minimumApprovals
```

in policy.

---

# 86. Verification Explainability

Internal explanation example:

```text
Ownership verified because:

• claimant supplied a serial suffix matching protected item evidence;
• claimant independently described a hidden yellow underside sticker;
• a pre-loss photograph contains the same scratch pattern;
• no ownership conflicts were detected;
• claim risk is LOW;
• policy v4 requirements were satisfied.
```

---

# 87. Claimant-Facing Explanation

Public explanation should reveal less:

```text
"Your ownership evidence has been verified."
```

or:

```text
"We need additional evidence before the claim can be approved."
```

Do not expose security rules.

---

# 88. Rejection Explanation

Do not reveal hidden answers.

Possible public message:

```text
"The evidence provided was not sufficient to verify the claim."
```

Internal record may be much more detailed.

---

# 89. Verification Confidence

Keep:

```text
verificationConfidence
```

separate from:

```text
matchConfidence
```

They represent different hypotheses.

---

# 90. Confidence Example

```text
MatchConfidence = HIGH
VerificationConfidence = LOW
```

means:

> likely same object, but claimant has not demonstrated ownership.

---

# 91. Another Example

```text
MatchConfidence = MEDIUM
VerificationConfidence = HIGH
```

may occur if a claimant provides a verified serial identifier.

Strong ownership evidence can coexist with a weaker original multimodal match.

---

# 92. Recovery Authorization Boundary

A `VERIFIED` decision still does not directly mutate custody.

Flow:

```text
OwnershipVerified
    ↓
RecoveryAuthorizationService
    ↓
organization policy
custody state
fraud state
legal hold
competing claim state
    ↓
RecoveryAuthorized
```

---

# 93. Recovery Blockers

Even after ownership verification, recovery may be blocked by:

```text
legal hold
active competing claim
police/institution restriction
custody discrepancy
unpaid mandatory statutory process where lawful
item unavailable
security incident
```

---

# 94. Verification API

Internal/user endpoints may include:

```text
POST /api/v1/claims
GET  /api/v1/claims/{claimId}

GET  /api/v1/verifications/{verificationId}
POST /api/v1/verifications/{verificationId}/responses
POST /api/v1/verifications/{verificationId}/evidence

POST /api/v1/staff/verifications/{verificationId}/review
```

Never expose expected answers through claimant APIs.

---

# 95. Challenge API Response

Claimant sees:

```json
{
  "challengeId": "019...",
  "question": "Describe any distinguishing feature underneath the device.",
  "expiresAt": "...",
  "attemptsRemaining": 2
}
```

Not:

```json
{
  "expectedAnswer": "yellow sticker"
}
```

---

# 96. Verification Evidence API

Uploads should use the same protected media pipeline as other Dzoka evidence.

Evidence types may include:

```text
PHOTO
DOCUMENT
TEXT
IDENTIFIER
AUTHORIZATION_DOCUMENT
```

---

# 97. Verification Event Flow

Representative events:

```text
claim.submitted
    ↓
verification.requested
    ↓
verification.started
    ↓
challenge.issued
    ↓
challenge.response-submitted
    ↓
verification.evidence-evaluated
    ↓
verification.risk-assessed
    ↓
verification.review-requested
    ↓
ownership.verified
```

Alternative:

```text
ownership.rejected
verification.inconclusive
verification.expired
```

---

# 98. Event Privacy

Do not publish:

```text
raw challenge answers
passport numbers
full serials
receipts
```

in broad events.

Publish references:

```text
verificationEvidenceId
challengeResponseId
```

---

# 99. Verification Persistence

Representative tables:

```text
verification_sessions
ownership_challenges
challenge_responses
verification_evidence
verification_decisions
evidence_disclosures
claim_conflicts
review_actions
```

---

# 100. `ownership_challenges`

Possible fields:

```text
id
verification_id
challenge_type
question
expected_evidence_reference
protected_expected_hash
classification
discriminative_score
disclosure_risk
generated_by
model_run_id
status
expires_at
created_at
```

---

# 101. Expected Answer Storage

Depending on challenge type:

Exact secret:

```text
HMAC / keyed hash
```

Free-form evidence:

```text
encrypted reference
```

Visual challenge:

```text
source evidence reference
```

Avoid unnecessary plaintext duplication.

---

# 102. `challenge_responses`

Possible:

```text
id
challenge_id
claimant_id
encrypted_raw_response
interpreted_response
evaluation_result
score
model_run_id
submitted_at
```

Retention should be stricter than ordinary public data.

---

# 103. `verification_evidence`

Possible:

```text
id
verification_id
evidence_type
source
result
score
reliability
confidence
independence_group
classification
source_reference
model_run_id
created_at
```

---

# 104. `verification_decisions`

Append-oriented:

```text
id
verification_id
outcome
confidence
coverage
policy_version
risk_assessment_id
reviewer_id
reason_codes
explanation
decided_at
```

Do not overwrite historical decisions.

---

# 105. Expiration

Verification sessions should expire.

Reasons:

```text
private questions should not stay active indefinitely
claims may become stale
risk changes
custody may change
```

Policy controls duration.

---

# 106. Session Restart

An expired session may create:

```text
new VerificationSession
```

rather than mutating the historical expired session.

---

# 107. Evidence Freshness

Some evidence may have freshness rules.

Example:

```text
one-time authorization letter
```

may expire.

A registered serial number may not.

---

# 108. Privacy Classification

Typical:

```text
challenge question
→ RESTRICTED

expected hidden evidence
→ SECRET

raw claimant answer
→ SECRET

verification outcome
→ RESTRICTED

high-level claimant notification
→ PUBLIC-to-that-user
```

---

# 109. Data Minimization

Do not request more evidence than needed.

Example:

A user should not be asked to upload national ID for a low-value umbrella if two strong private item features are sufficient.

---

# 110. High-Sensitivity Documents

Identity documents should:

```text
use protected upload
have restricted access
have short configurable retention
record every view
never appear in logs
```

---

# 111. Sensitive Access Audit

Viewing:

```text
receipt
identity document
full serial
private photo
raw challenge answer
```

must generate a sensitive access audit event where policy requires.

---

# 112. Verification and Federation

In a federated case, the node holding the item should normally retain authority over:

```text
secret found-item evidence
claim release
custody
```

The claimant's home node may coordinate the claim.

---

# 113. Federated Verification Pattern

Possible flow:

```text
Claimant Node
    ↓
verification request
    ↓
Holding Node
    ↓
private challenge generated locally
    ↓
safe challenge sent
    ↓
claimant response
    ↓
holding node evaluates private evidence
    ↓
verification result
```

Secret expected answers remain at the holding node.

---

# 114. Federated Privacy

The holding node should not need to transmit:

```text
hidden scratch
serial
private finder note
```

to the requesting node.

Instead it can return:

```text
challenge
evaluation result
verification outcome
```

according to policy.

---

# 115. Federated Verification Result

Canonical:

```text
FederatedVerificationResult
├── remoteVerificationId
├── outcome
├── confidenceClass
├── policyVersion
├── authorizedBy
├── evidenceSummarySafe
└── signature
```

---

# 116. Federated Trust

A remote verification result is accepted only if:

```text
node trusted
signature valid
protocol supported
policy permits delegation
```

---

# 117. Verification and Registered Items

Registered possessions may contain:

```text
pre-loss photos
serial
receipt
ownership record
QR/NFC tag
```

This allows stronger and often simpler verification.

---

# 118. Registered Item Shortcut

A registered item still should not automatically bypass all verification.

Example:

Account compromise could allow fraudulent collection.

Policy may require:

```text
authenticated account
+
registered identity
+
handover confirmation
```

---

# 119. Verification and QR/NFC

Scanning a registered tag can establish:

```text
item identity
```

but not automatically:

```text
claimant identity
```

Both must be considered.

---

# 120. Verification and Digital Credentials

Future `ItemIdentityProvider` implementations may supply:

```text
signed ownership credentials
verifiable credentials
enterprise asset credentials
```

These become another evidence class.

They do not replace policy.

---

# 121. Risk of Social Engineering

A malicious claimant may learn public details from:

```text
listing
social media
finder posts
staff
```

Therefore challenges should prioritize evidence that was never public.

---

# 122. Adaptive Challenges

If a response fails:

```text
do not immediately reveal why
```

Policy may:

```text
ask another independent challenge
request stronger evidence
escalate to staff
block further attempts
```

---

# 123. Challenge Rotation

Do not continuously cycle through every secret attribute until the claimant guesses one.

Set:

```text
maximum challenge count
maximum attempts
```

per policy.

---

# 124. Challenge Abuse Detection

Signals:

```text
many challenge attempts
many items claimed
rapid submissions
same answer pattern
multiple accounts same device
```

feed Fraud & Trust.

---

# 125. Verification Under Weak Evidence

Some items genuinely have no strong hidden details.

Example:

```text
plain blue umbrella
```

Possible evidence:

```text
loss time
loss location
brand
specific handle
claimant's historical report timing
finder's non-public observations
```

Outcome may remain:

```text
INCONCLUSIVE
```

requiring staff judgment.

Dzoka must not fabricate certainty.

---

# 126. Low-Value Simplified Policy

An organization may configure:

```text
LOW_VALUE_SIMPLIFIED
```

for common low-risk items.

Example:

```text
two independent private details
+
low claim risk
+
no competing claim
```

may be sufficient.

---

# 127. High-Value Policy

Example:

```text
HIGH_VALUE_ELECTRONICS
```

may require:

```text
identity-grade evidence
+
private challenge
+
staff approval
```

---

# 128. Identity Document Policy

Documents such as passports should use strict release policy.

Potential requirements:

```text
staff review
claimant identity validation
document-specific hidden detail
institution policy
possibly government/authority process
```

---

# 129. Verification Decision Pseudocode

Conceptually:

```text
function evaluateVerification(session):

    evidence = loadVerificationEvidence(session)

    risk = assessClaimRisk(session.claim)

    conflicts = findCriticalVerificationConflicts(evidence)

    if conflicts.containsCritical():
        return REJECTED

    summary =
        aggregateIndependentEvidence(evidence)

    policy =
        loadPolicy(
            tenant,
            organization,
            itemCategory,
            itemRiskClass
        )

    policyResult =
        policy.evaluate(
            summary,
            risk,
            claimContext
        )

    if policyResult.requiresMoreEvidence:
        return IN_PROGRESS

    if policyResult.requiresHumanReview:
        return HUMAN_REVIEW

    if policyResult.approve:
        return VERIFIED

    if policyResult.reject:
        return REJECTED

    return INCONCLUSIVE
```

---

# 130. Evidence Aggregation Pseudocode

```text
for evidence in evidenceSet:

    if evidence.result == UNAVAILABLE:
        continue

    effectiveWeight =
        policyWeight(evidence.type)
        × evidence.reliability
        × independenceAdjustment(evidence)

    accumulate(
        evidence.score,
        effectiveWeight
    )

coverage =
    availableWeight / expectedRelevantWeight
```

Critical rules remain outside the aggregate score.

---

# 131. Verification Decision Must Be Reproducible

Retain:

```text
claim snapshot
candidate reference
fingerprint versions
challenge definitions
challenge responses
evidence evaluations
risk version
policy version
model versions
human review
decision
```

---

# 132. Human Review Queue

Possible priority factors:

```text
high-value item
competing claim
high fraud risk
identity document
critical evidence conflict
staff escalation
federated dispute
```

---

# 133. Review SLA

Organizations may configure review targets.

SLA misses should be visible operationally but must not automatically approve a claim.

---

# 134. Appeals

Optional institutional workflow:

```text
REJECTED
    ↓
APPEAL_REQUESTED
    ↓
SECOND_REVIEW
    ↓
UPHELD / OVERTURNED
```

If implemented, the original decision remains immutable.

---

# 135. Evidence Reuse

Evidence supplied for one claim should not automatically be reused for another claimant or case without policy authorization.

---

# 136. Evidence Deduplication

The same uploaded receipt reused across unrelated claims should be detectable.

Use:

```text
content hashes
document fingerprints
provenance
```

as fraud signals.

---

# 137. Verification Metrics

Track:

```text
verification sessions
verification success rate
inconclusive rate
human review rate
challenge failure rate
average evidence count
verification duration
competing claim rate
```

---

# 138. Safety Metrics

Important:

```text
verified false-owner rate
fraudulent claim approval rate
critical-conflict override rate
challenge leakage incidents
```

These are more important than simply maximizing automated verification rate.

---

# 139. Challenge Metrics

Track:

```text
challenge discriminative success
average attempts
public-disclosure rejection count
challenge type effectiveness
false rejection rate
```

---

# 140. Human Review Metrics

Track:

```text
automatic recommendation vs reviewer decision
override rate
review duration
reviewer disagreement
```

These can expose weak policy/model areas.

---

# 141. ML Feedback

Outcomes may generate:

```text
CLAIM_VERIFIED
CLAIM_REJECTED
CHALLENGE_SUCCESS
CHALLENGE_FAILURE
FRAUD_CONFIRMED
FRAUD_CLEARED
```

for model evaluation.

---

# 142. ML Safety

Do not automatically train on every reviewer action as unquestionable truth.

Feedback records should contain:

```text
label source
review confidence
appeal outcome
ground-truth quality
```

---

# 143. Observability

Useful traces:

```text
verification.start
verification.challenge-plan
verification.challenge-generate
verification.response-evaluate
verification.risk-assess
verification.policy-evaluate
verification.human-review
verification.decide
```

---

# 144. Metrics

Examples:

```text
dzoka_verification_started_total
dzoka_verification_verified_total
dzoka_verification_rejected_total
dzoka_verification_inconclusive_total
dzoka_verification_duration_seconds
dzoka_challenge_attempts_total
dzoka_verification_human_review_total
```

---

# 145. Logging

Safe structured logging may include:

```text
verificationId
claimId
status
policyVersion
riskLevel
decision
duration
```

Do not log:

```text
raw answers
serials
receipts
identity documents
secret feature descriptions
```

---

# 146. Failure Modes

Canonical failures:

```text
CHALLENGE_GENERATION_FAILED
CHALLENGE_UNSAFE
EVIDENCE_PROCESSING_FAILED
POLICY_UNAVAILABLE
RISK_ENGINE_UNAVAILABLE
HUMAN_REVIEW_REQUIRED
VERIFICATION_EXPIRED
EVIDENCE_CORRUPTED
```

---

# 147. Graceful Degradation

If LLM challenge generation fails:

```text
use deterministic templates
```

If semantic answer evaluation fails:

```text
human review
```

If risk model fails:

```text
rule-based fallback or manual review
```

Never silently auto-approve.

---

# 148. Verification Without AI

Dzoka must support a fully deterministic/manual verification path.

Example:

```text
private question
exact serial fragment
receipt
staff approval
```

AI improves usability but is not mandatory for trust correctness.

---

# 149. Security Invariants

### VERIFY-SEC-001

Expected challenge evidence is never exposed to claimant APIs.

### VERIFY-SEC-002

Raw challenge answers are secret.

### VERIFY-SEC-003

A failed challenge never reveals the correct answer.

### VERIFY-SEC-004

Critical identifier mismatches cannot be ignored by an LLM.

### VERIFY-SEC-005

Every secret evidence access is authorized.

### VERIFY-SEC-006

Challenge attempts are rate-limited.

### VERIFY-SEC-007

Competing serious claims can block release.

### VERIFY-SEC-008

Recovery authorization is separate from verification.

---

# 150. Domain Invariants

### VERIFY-001

A verification session must reference a valid claim.

### VERIFY-002

A challenge must reference actual evidence.

### VERIFY-003

An AI-generated question must pass deterministic safety validation.

### VERIFY-004

Publicly disclosed evidence cannot count as fully private challenge evidence.

### VERIFY-005

A verification decision records exact policy version.

### VERIFY-006

Historical decisions are immutable.

### VERIFY-007

Evidence conflicts are preserved, not hidden by aggregate scores.

### VERIFY-008

Verification outcome must not mutate custody directly.

### VERIFY-009

Remote verification results require trusted federation provenance.

### VERIFY-010

Authorized representation is modeled explicitly rather than pretending every claimant is the owner.

---

# 151. Example — Laptop

Found evidence:

```text
Lenovo ThinkPad T14
black
yellow sticker underneath
scratch beside USB-C
serial suffix 9247
```

Claimant provides:

```text
correct model
correct serial suffix
describes yellow label underneath
old photograph showing same scratch
```

Risk:

```text
LOW
```

Policy:

```text
high-value electronics requires:
- identity-grade evidence
- one private detail
- no critical conflict
```

Result:

```text
VERIFIED
```

---

# 152. Example — Fraudulent Laptop Claim

Claimant knows:

```text
black Lenovo
found at Joina
```

because this is public.

Challenge:

```text
"What distinguishing feature is underneath the laptop?"
```

Claimant answers incorrectly three times.

Risk signals:

```text
multiple failed attempts
multiple other item claims
```

Result:

```text
REJECTED
```

and potentially:

```text
HIGH risk
```

---

# 153. Example — Plain Umbrella

Item:

```text
blue umbrella
black curved handle
small tear near edge
```

No serial or receipt.

Claimant:

```text
correctly identifies curved handle
describes small tear
gives plausible loss time/location
```

Policy for low-value property permits:

```text
two independent private details
+
low risk
```

Result may be:

```text
VERIFIED
```

without government ID.

---

# 154. Example — Competing Claims

Claim A:

```text
correct colour
correct public brand
wrong hidden feature
```

Claim B:

```text
correct hidden feature
old photo
serial fragment
```

System:

```text
freezes release
flags conflict
staff compares claims
```

Likely outcome:

```text
Claim B VERIFIED
Claim A REJECTED
```

---

# 155. Example — Parent Collecting for Child

Item belongs to minor.

Parent provides:

```text
child relationship evidence
private item feature
matching school/institution context
```

Verification proves:

```text
RecoveryEntitlement
```

rather than falsely recording the parent as the owner.

---

# 156. Example — Organization Laptop

Employee claims company laptop.

Evidence:

```text
company asset register
employee membership
asset serial
manager authorization
```

Outcome:

```text
employee authorized representative
organization remains owner
```

---

# 157. Federation Example

Airport node finds laptop.

Owner uses home-region Dzoka node.

The airport node keeps:

```text
serial
secret scratch
finder details
```

locally.

It sends challenge:

```text
"Describe any unusual marking near the charging ports."
```

Owner replies through their local node.

Airport evaluates privately and returns:

```text
VERIFIED
```

signed by the trusted airport node.

The secret evidence never needs to leave the airport node.

---

# 158. Source Audit Influence

This design incorporates lessons from audited repositories:

```text
La2eet
    → private finder-defined verification questions

Smart Asset Finder
    → claim-answer scoring concept
      but Dzoka does not rely on LLM score alone

AI-lost-and-found / SmartFind
    → claim proof workflow concepts

Lost-and-Found-Tracking-System
    → claims, conflicts and human approval patterns

globalenglish01/lostfound
    → secret attributes
    → deterministic conflict/evidence thinking

FindBack
    → handover/recovery codes after approved claims
```

No audited project provided the complete Dzoka design of:

```text
AI-selected non-public distinguishing evidence
+
safe challenge generation
+
disclosure tracking
+
multi-evidence verification
+
risk
+
deterministic policy
+
human authorization
```

That integrated subsystem is a Dzoka-specific design.

---

# 159. Anti-Patterns

Do not implement:

```text
match score > 90%
→ owner verified
```

Do not implement:

```text
LLM response says "looks correct"
→ owner verified
```

Do not implement:

```text
public listing contains scratch
→ ask claimant about scratch as secret proof
```

Do not implement:

```text
unlimited ownership guesses
```

Do not implement:

```text
receipt uploaded
→ automatic owner
```

Do not implement:

```text
government ID uploaded
→ automatic item ownership
```

Do not implement:

```text
wrong serial ignored because photo looks similar
```

Do not expose expected answers to the frontend.

---

# 160. Implementation Components

Recommended components:

```text
ClaimEligibilityService

VerificationSessionService

VerificationEvidenceService

ChallengeCandidateSelector
ChallengePlanService
ChallengeGenerationService
ChallengeSafetyValidator
DisclosureGuard

ChallengeResponseService
ResponseInterpretationService

DocumentEvidenceService
PriorPhotoEvidenceService
ProtectedIdentifierEvidenceService
InstitutionalEvidenceService
EntitlementService

ClaimRiskService

VerificationPolicyService
VerificationDecisionService

VerificationReviewService
ClaimConflictService
```

---

# 161. Internal Component Flow

```text
Claim
 ↓
ClaimEligibilityService
 ↓
VerificationSessionService
 ↓
VerificationEvidenceService
 ↓
ChallengePlanService
 ↓
ChallengeGenerationService
 ↓
ChallengeResponseService
 ↓
ResponseInterpretationService
 ↓
ClaimRiskService
 ↓
VerificationPolicyService
 ↓
VerificationReviewService when required
 ↓
VerificationDecisionService
```

---

# 162. Implementation Sequence

Recommended order:

```text
1. VerificationSession domain
2. evidence contracts
3. deterministic private questions
4. protected identifier comparison
5. challenge response lifecycle
6. verification policy engine
7. human review
8. claim risk integration
9. disclosure tracking
10. AI challenge wording
11. semantic answer interpretation
12. prior-photo verification
13. document evidence processing
14. entitlement
15. competing claims
16. federated verification
17. calibration and ML feedback
```

This is sequencing, not scope reduction.

---

# 163. Testing Requirements

Unit tests:

```text
challenge selection
disclosure rejection
exact secret comparison
semantic response mapping
evidence independence
critical conflicts
policy evaluation
session expiry
```

Integration tests:

```text
claim → challenge → verified
claim → wrong answers → rejected
receipt processing
prior-photo verification
human review
federated verification
```

---

# 164. Security Tests

Mandatory:

```text
expected answer not returned by API
expected answer absent from logs
challenge brute force blocked
cross-tenant evidence access denied
secret evidence federation leak blocked
replayed challenge submission handled
unauthorized reviewer denied
```

---

# 165. Golden Verification Dataset

Include cases such as:

```text
correct hidden scratch
semantically equivalent description
wrong hidden feature
guessed public detail
correct serial
wrong serial
receipt with matching serial
receipt with matching model only
valid pre-loss photo
post-disclosure photo
multiple independent private details
competing claimant
authorized representative
```

---

# 166. Production Readiness

Ownership verification is not production-ready until it supports:

```text
private evidence
disclosure tracking
protected identifiers
bounded attempts
risk
policy
human review
auditing
tenant isolation
federation privacy
failure handling
security tests
```

A chatbot asking one question is not sufficient.

---

# 167. Relationship to Matching Engine

Matching produces:

```text
MatchCandidate
```

Verification consumes that candidate but does not inherit its conclusion blindly.

A high match can still fail ownership verification.

---

# 168. Relationship to ItemFingerprint

The verification engine may use:

```text
secret distinctive features
protected identifiers
pre-loss evidence
```

from the `ItemFingerprint`.

It must access these through a restricted verification projection.

---

# 169. Relationship to Fraud Engine

Verification generates useful fraud signals.

Fraud results may require:

```text
additional challenge
human review
block
```

but risk score alone should not prove or disprove ownership.

---

# 170. Relationship to Recovery Authorization

Verification ends with:

```text
VerificationDecision
```

Recovery begins only when:

```text
RecoveryAuthorizationService
```

accepts that decision together with:

```text
custody
policy
risk
competing claims
legal restrictions
```

---

# 171. Final Design Principle

Dzoka ownership verification is fundamentally a **private evidence protocol**.

It should establish entitlement without giving attackers a checklist of correct answers.

The design therefore follows:

```text
LIKELY ITEM MATCH
      ↓
PRIVATE EVIDENCE INVENTORY
      ↓
NON-PUBLIC CHALLENGE SELECTION
      ↓
SAFE QUESTION GENERATION
      ↓
CLAIMANT RESPONSE
      ↓
MULTI-EVIDENCE EVALUATION
      ↓
FRAUD / RISK
      ↓
DETERMINISTIC POLICY
      ↓
HUMAN REVIEW WHERE REQUIRED
      ↓
VERIFICATION DECISION
      ↓
SEPARATE RECOVERY AUTHORIZATION
```

The subsystem must always be able to answer:

```text
Which private evidence supported the claim?

Was that evidence already public?

How reliable was it?

Were there contradictory identifiers?

Which model interpreted the answer?

Which policy version was applied?

Was human review required?

Why was the claimant verified or rejected?

Who is actually entitled to recover the item?
```

That is the standard required for Dzoka to move from “AI found something similar” to **trusted real-world property recovery**.