# Source Audit — pirinthaban/findback

> **Dzoka — The Open Recovery Network**
>
> Source audit of FindBack, a Flutter/Firebase lost-and-found platform focused on community recovery, automatic matching, claims, notifications, handover codes, reputation, privacy controls, police verification, and mobile-first interaction.

---

# 1. Audit Metadata

| Field | Value |
|---|---|
| Repository | `https://github.com/pirinthaban/findback` |
| Upstream owner | `pirinthaban` |
| Project | FindBack |
| Audited revision | `fc91563886e4e8da6931bc07dd025fbe235068a2` |
| Short revision | `fc915638` |
| Revision date | 2026-01-17 |
| Audit date | 2026-10-04 |
| Default branch | `main` |
| License | MIT |
| Primary frontend | Flutter |
| Primary backend | Firebase |
| Database | Cloud Firestore |
| Object storage | Firebase Storage / optional Cloudinary concepts |
| Authentication | Firebase Auth |
| Notifications | Firebase Cloud Messaging + local notifications |
| Functions runtime | Node.js 18 / TypeScript |
| Primary classification | **ADAPT SELECTIVELY** |
| Secondary classification | **REFERENCE / REJECT MATCHER AS FOUNDATION** |
| Direct code reuse | Permitted under MIT subject to attribution/license requirements |
| Dzoka foundation? | No |

---

# 2. Executive Decision

FindBack should not become the technical foundation for Dzoka's matching or ownership-verification engine.

However, it contains several useful architectural/product concepts around:

```text
claim notifications
handover codes
push notifications
proof documents
reputation
police review
privacy-aware storage
event-triggered workflows
mobile-first recovery
```

Its strongest reusable lessons are in:

```text
RECOVERY UX
NOTIFICATIONS
CLAIM/PROOF DATA MODEL
HANDOVER CONCEPT
FIREBASE SECURITY RULE THINKING
```

not its current AI matcher.

The source shows several major divergences from the README.

Most importantly:

```text
README:
TensorFlow Lite image similarity
AI matching
production-ready claims
encrypted chat
auto-blur privacy
```

while important active implementation paths contain:

```text
random image similarity
placeholder image embeddings
placeholder claim screen
claim flow bypass
plaintext chat messages
incomplete backend image privacy processing
```

Therefore the correct decision is:

```text
ADAPT SELECTIVELY

REJECT MATCHING IMPLEMENTATION AS FOUNDATION
```

---

# 3. Why FindBack Matters to Dzoka

FindBack attempts to cover a broader recovery lifecycle than simple lost-item listing applications.

Its conceptual flow includes:

```text
REPORT ITEM
    ↓
AUTOMATIC MATCH
    ↓
PUSH NOTIFICATION
    ↓
CLAIM
    ↓
PROOF / VERIFICATION
    ↓
MEETUP
    ↓
HANDOVER CODE
    ↓
COMPLETED CLAIM
    ↓
REPUTATION
```

This is closely related to several Dzoka domains:

```text
Matching
Claim
Verification
Messaging
Custody
Recovery
Notification
Trust
```

---

# 4. Audit Scope

Primary files and paths inspected include:

```text
README.md
LICENSE
CHANGELOG.md
AI_FEATURES_STATUS.md
pubspec.yaml
firebase.json
firestore.rules
storage.rules
.env.example

lib/
├── core/services/
│   ├── matching_service.dart
│   ├── free_ai_service.dart
│   └── notification_service.dart
│
├── features/claims/presentation/screens/
│   └── claim_flow_screen.dart
│
├── features/items/presentation/screens/
│   └── item_detail_screen.dart
│
├── features/chat/
│   ├── data/chat_service.dart
│   └── presentation/screens/
└── core/router/app_router.dart

functions/
├── src/index.ts
├── src/ai/matchingEngine.ts
└── src/triggers/
    ├── onItemCreated.ts
    ├── onClaimCreated.ts
    └── processImageUpload.ts

findback-functions/
└── src/
    ├── ai/faceRecognition.ts
    └── triggers/
        ├── onItemCreated.ts
        ├── onClaimCreated.ts
        └── onClaimUpdated.ts
```

---

# 5. Audit Scope Limit

This is a targeted architecture and source audit.

It is not:

- a complete penetration test;
- a formal legal opinion;
- an independent production deployment;
- a full Firebase billing/scalability assessment;
- an independent accuracy benchmark of every matching component.

---

# 6. License

Unlike several other direct lost-and-found repositories audited for Dzoka, FindBack contains a real root:

```text
LICENSE
```

file.

The project is licensed under:

```text
MIT
```

at the audited revision.

---

# 7. License Decision

This means Dzoka may legally reuse MIT-compatible implementation material if desired, while preserving required notices.

However:

> **License compatibility does not imply architectural suitability.**

Many source paths should still not be copied because the implementation is incomplete, inconsistent, or unsuitable for Dzoka's trust requirements.

---

# 8. Repository Architecture

Conceptually:

```text
Flutter App
    ↓
Firebase Auth
    ↓
Cloud Firestore
    ↓
Firebase Storage
    ↓
Cloud Functions
    ↓
Firebase Cloud Messaging
```

Supporting capabilities include:

```text
Google ML Kit
Google Cloud Vision
optional Gemini code
geolocation
Google Sign-In
local notifications
```

---

# 9. Important Architecture Finding — Two Backend Function Trees

The repository contains:

```text
functions/
```

and:

```text
findback-functions/
```

Both contain Cloud Function implementations.

This is important.

---

# 10. Which Backend Is Actually Configured?

`firebase.json` defines:

```json
{
  "functions": {
    "source": "functions"
  }
}
```

Therefore the default Firebase deployment uses:

```text
functions/
```

not:

```text
findback-functions/
```

---

# 11. Why This Matters

The second tree:

```text
findback-functions/
```

contains more advanced implementations involving:

```text
Gemini
Vision API
face analysis
richer scoring
```

but those should not automatically be described as the default deployed runtime.

The audit therefore distinguishes:

```text
DEFAULT DEPLOYMENT PATH
functions/

ALTERNATE / DUPLICATE IMPLEMENTATION
findback-functions/
```

---

# 12. Documentation Must Identify Runtime Reality

A repository can contain advanced code without that code being:

```text
wired
configured
deployed
reachable
```

Therefore Dzoka source audits must always distinguish:

```text
exists in repository
```

from:

```text
active runtime path
```

---

# 13. README Positioning

The README describes FindBack as:

```text
Production-Ready Lost & Found Ecosystem
```

and marks capabilities such as:

```text
AI image similarity
claim verification
encrypted chat
reputation
auto-blurring
push notifications
```

as implemented.

---

# 14. Changelog Contradiction

`CHANGELOG.md` describes the public release as:

```text
alpha stage
```

and lists several advertised capabilities as planned.

Examples include:

```text
image similarity matching
real phone authentication
push notifications
in-app chat messaging
claim verification
reputation
offline-first architecture
AI-powered auto-matching
```

This is a significant documentation-state inconsistency.

---

# 15. Dzoka Lesson

Never classify source maturity from:

```text
README checkboxes
```

alone.

Inspect:

```text
actual implementation
deployment config
tests
runtime dependencies
```

---

# 16. Data Model

The documented Firestore architecture includes collections such as:

```text
users
items
matches
claims
chats
messages
reports
police_verifications
notifications
audit_logs
```

This is a useful domain decomposition.

---

# 17. Positive Data Separation

The project distinguishes:

```text
Item
Match
Claim
Chat
Report
PoliceVerification
AuditLog
```

rather than trying to place every workflow inside one item document.

That is a good architectural instinct.

---

# 18. Dzoka Extension

Dzoka takes the separation further into:

```text
ItemRecord
LostReport
FoundReport
MatchingRun
MatchCandidate
Claim
VerificationSession
RiskAssessment
RecoveryAuthorization
CustodyRecord
RecoveryEvent
```

---

# 19. Client-Side Matching Architecture

The Flutter application contains:

```text
lib/core/services/matching_service.dart
```

which performs matching against Firestore data.

---

# 20. Candidate Query

The client matcher searches for:

```text
opposite item type
+
same category
```

and limits the query to:

```text
50 candidates
```

---

# 21. Strict Category Gate

The query contains a strict:

```text
category == category
```

condition.

This can eliminate legitimate matches.

Example:

```text
Lost:
category = Electronics

Found:
category = Phone
```

may represent the same object but never reach comparison.

---

# 22. Dzoka Decision

Category should usually be:

```text
retrieval preference
+
matching feature
```

rather than an unconditional destructive gate.

---

# 23. Same-User Exclusion

The Flutter matcher excludes items submitted by the current user.

This is a reasonable default for ordinary lost/found matching.

---

# 24. Search Inputs

The source item comparison text contains roughly:

```text
description
category
location
```

while candidate text includes:

```text
description
title
location
category
OCR extracted text
```

---

# 25. OCR Participation

Including:

```text
extractedText
```

in matching is a useful idea.

OCR may reveal:

```text
brand
serial fragments
labels
names
document numbers
```

which can increase retrieval quality.

---

# 26. OCR Privacy Caveat

Extracted OCR can also contain highly sensitive information.

Examples:

```text
NIC
passport number
phone number
email
bank information
```

Therefore Dzoka must classify OCR-derived evidence before indexing or exposing it.

---

# 27. Active Client Matching Engine

The Flutter matcher calls:

```text
FreeAIService.calculateMatchScore()
```

---

# 28. `FreeAIService`

This service genuinely implements several useful on-device capabilities:

```text
OCR
face detection
NIC pattern detection
passport pattern detection
license number detection
phone extraction
email extraction
color extraction
brand extraction
fuzzy text matching
synonym matching
Tamil translation
geographic distance
```

These are real source capabilities.

---

# 29. Text Similarity

The text matcher uses a combination of:

```text
tokenization
stop-word filtering
Jaccard-like overlap
synonyms
Levenshtein fuzzy matching
```

This is a reasonable lightweight baseline.

---

# 30. Brand Recognition

The implementation extracts brands from a manually maintained brand dictionary.

This is useful for basic structured evidence but should become taxonomy/configuration data rather than remain hard-coded.

---

# 31. Color Recognition

The implementation maps color variants into canonical color groups.

Examples:

```text
navy
azure
cobalt
→ blue
```

This is a useful normalization concept.

---

# 32. Dzoka Adaptation

Dzoka may use similar normalization concepts through:

```text
AttributeNormalizer
TaxonomyProvider
```

rather than hard-coded application maps.

---

# 33. Location Score

The Flutter matcher assigns:

```text
locationScore = 50
```

when geographic coordinates are missing.

This is problematic.

---

# 34. Missing Location Is Not 50% Similarity

Unknown location means:

```text
no geographic evidence
```

not:

```text
50% geographic match
```

---

# 35. Time Score

Likewise:

```text
timeScore = 50
```

when timestamps are unavailable.

Again:

```text
missing evidence
```

is being converted into:

```text
positive evidence
```

---

# 36. Category Score

Category also defaults to:

```text
50
```

when category evidence is missing.

This repeats the same architectural error.

---

# 37. Dzoka Core Invariant Reinforced

FindBack provides another useful negative example supporting:

> **Missing evidence ≠ mismatch, and missing evidence ≠ synthetic similarity.**

Correct Dzoka representation:

```text
available = false
score = null
```

---

# 38. Hard Geographic Rejection

The client matcher rejects candidates when geographic distance exceeds approximately:

```text
150 km
```

when coordinates are available.

---

# 39. Why This Can Fail

An item may legitimately travel.

Example:

```text
lost on inter-city bus
    ↓
bus continues route
    ↓
item found 180 km away
```

The correct candidate would be eliminated.

---

# 40. Dzoka Geographic Model

Dzoka should combine:

```text
distance
facility hierarchy
routes
transport movement
recovery graph
time
institutional transitions
```

rather than relying on one universal maximum radius.

---

# 41. Hard Time Rejection

The Flutter matcher also rejects candidates more than approximately:

```text
365 days
```

apart.

A maximum age may make operational sense, but it should be policy-based rather than hard-coded identity logic.

---

# 42. Matching Uses Creation Time

Several matching paths compare:

```text
createdAt
```

rather than necessarily the actual:

```text
lostAt
foundAt
```

event time.

These are different facts.

---

# 43. Dzoka Time Model

Dzoka uses:

```text
TimeWindow
├── start
├── end
├── precision
├── rawExpression
└── timezone
```

based on the actual observed loss/found event where possible.

---

# 44. No Client Image Matching

Despite README claims, the active Flutter matcher returns:

```text
imageSimilarity = 0
```

from its main `FreeAIService` match result.

Therefore the client matcher does not provide actual image-to-image identity scoring.

---

# 45. Default Cloud Functions Matching

The default Firebase backend contains:

```text
functions/src/triggers/onItemCreated.ts
```

This path is particularly important because it is the function directory configured by `firebase.json`.

---

# 46. Default Candidate Query

It filters candidates by:

```text
opposite type
same category
same district
active status
```

and takes:

```text
50
```

candidates.

---

# 47. Same-District Hard Gate

The additional:

```text
district == item.district
```

filter introduces another false-negative risk.

Items can move between districts through:

```text
transport
couriers
commuting
travel
```

---

# 48. Default Cloud Match Formula

The default Cloud Function computes:

```text
image similarity  = 40%
text similarity   = 30%
location          = 20%
time              = 10%
```

---

# 49. Critical Finding — Image Similarity Is Random

The actual function is:

```text
calculateImageSimilarity(...)
```

and explicitly says:

```text
Placeholder: return random score
```

It returns approximately:

```text
30–80%
```

using:

```text
Math.random()
```

---

# 50. Consequence

Randomness contributes:

```text
40%
```

of the default match score.

Therefore repeated matching of the same two items can produce different results.

---

# 51. This Is Not AI

The default image matcher does not inspect:

```text
pixels
embeddings
features
objects
patches
perceptual hashes
```

It simply generates a random number.

---

# 52. Dzoka Classification

This matching path is:

```text
REJECT
```

as an implementation foundation.

---

# 53. AI Documentation Is More Honest in One Place

`AI_FEATURES_STATUS.md` explicitly states:

```text
Image Matching:
PARTIALLY IMPLEMENTED
```

and documents the random placeholder.

This is more accurate than the main README.

---

# 54. README Contradiction

README marks:

```text
Image similarity detection (TensorFlow Lite)
```

as completed.

The source does not support that claim.

---

# 55. TensorFlow Dependency Finding

README and AI documentation reference:

```text
tflite_flutter
```

and even state it is already present in `pubspec.yaml`.

At the audited revision:

```text
tflite_flutter
```

is not in the actual `pubspec.yaml`.

---

# 56. Dzoka Lesson

Documentation statements such as:

```text
already in dependencies
```

must be checked against the actual lock/build files.

---

# 57. Placeholder Image Embeddings

The default backend's:

```text
generateImageEmbedding()
```

also generates a random:

```text
128-dimensional vector
```

using `Math.random()`.

---

# 58. Random Embeddings Are Dangerous

Random vectors should never enter:

```text
production search
training datasets
evaluation datasets
persistent fingerprints
```

because they look structurally like real ML data.

---

# 59. Dzoka Rule

Placeholders must be:

```text
explicitly marked
disabled outside tests
```

and must never contaminate production evidence.

---

# 60. Sensitive Image Processing

The default backend's:

```text
processImageUpload.ts
```

claims responsibilities including:

```text
face detection
NIC detection
privacy blurring
image embeddings
```

but those pieces remain:

```text
TODO
```

in that deployment path.

---

# 61. Actual Default Processing

The active implementation mainly records:

```text
imageProcessed = true
image metadata
```

even though the deeper privacy analysis is not actually performed.

---

# 62. Dangerous Status Semantics

Setting:

```text
imageProcessed = true
```

can imply that privacy protection succeeded when it merely recorded metadata.

---

# 63. Dzoka Rule

Processing status must be task-specific.

For example:

```text
OCR_COMPLETED
FACE_REDACTION_COMPLETED
IDENTIFIER_REDACTION_COMPLETED
EMBEDDING_COMPLETED
```

rather than one ambiguous:

```text
processed = true
```

---

# 64. Alternate Backend — `findback-functions`

The repository also contains a richer implementation in:

```text
findback-functions/
```

This code includes:

```text
Gemini
Google Cloud Vision
face landmarks
multimodal prompting
richer match score
```

---

# 65. But It Is Not Default Firebase Source

Again:

```text
firebase.json
→ functions/
```

not:

```text
findback-functions/
```

Therefore this must be described as an alternate code path.

---

# 66. Alternate Match Formula

The richer implementation uses approximately:

```text
text         15%
location     15%
time         10%
image        25%
face         10%
Gemini       25%
```

---

# 67. Gemini Analysis

Gemini is asked whether two reports are:

```text
the SAME physical object
```

using descriptions and up to:

```text
one image per item
```

and asked to output:

```text
score
reasoning
```

---

# 68. LLM Score Limitation

A generative model returning:

```text
score = 87
```

does not establish:

```text
87% calibrated probability
```

that two records describe the same object.

Dzoka should treat this as:

```text
reranker evidence
```

not final confidence.

---

# 69. Dzoka LLM Role

LLMs may:

```text
interpret
rerank
summarize
explain
```

but should not directly own:

```text
identity truth
ownership truth
release authority
```

---

# 70. Face Processing

The alternate backend uses Google Cloud Vision to detect facial landmarks.

It then constructs a numerical vector from normalized landmark coordinates.

---

# 71. Important Qualification

This is not equivalent to a modern learned:

```text
face-recognition embedding
```

such as a dedicated biometric recognition model.

It is a hand-built geometric descriptor.

---

# 72. Vector Dimension Padding

The face similarity code pads shorter vectors with zeros if dimensions differ.

This is another instance of a pattern Dzoka should avoid.

---

# 73. Dzoka Rule

Unexpected embedding dimension mismatch should normally produce:

```text
INCOMPATIBLE_FEATURE
```

rather than silent resizing.

---

# 74. Biometric Privacy Concern

Face recognition is much more sensitive than ordinary object matching.

Dzoka should distinguish:

```text
FACE DETECTION FOR REDACTION
```

from:

```text
BIOMETRIC FACE IDENTIFICATION
```

---

# 75. Dzoka Default

Face detection for privacy protection may be appropriate.

Face recognition/search should **not** be part of the default Dzoka item-recovery pipeline.

It would require its own:

```text
privacy review
legal basis
threat model
ADR
explicit deployment policy
```

---

# 76. Claim Model — Documented Design

README documents a claim containing:

```text
itemId
claimantUserId
itemOwnerId
status
proofDocuments[]
proofAnswers
verificationQuestions
verifiedBy
verificationNotes
meetupLocation
meetupTime
handoverConfirmed
handoverCode
createdAt
completedAt
```

Conceptually, this is a useful schema.

---

# 77. Claim State Model

Claim statuses include:

```text
pending
under_review
verified
rejected
completed
```

This is better than a single boolean.

---

# 78. Strong Claim Concepts

Useful ideas include:

```text
proof documents
proof answers
reviewer
verification notes
meetup
handover state
completed state
```

---

# 79. Critical Finding — Flutter Claim Screen Is Placeholder

Actual:

```text
lib/features/claims/presentation/screens/claim_flow_screen.dart
```

contains only:

```text
Claim Flow for: <itemId>
```

as placeholder UI.

There is no complete claim workflow there.

---

# 80. Actual Main App Claim Behavior

The item detail screen handles the claim action itself.

When a non-owner taps the action, it does **not** create a:

```text
claims
```

document.

---

# 81. What It Actually Creates

It creates:

```text
notification
```

with claim-like metadata and then immediately starts a chat.

Conceptually:

```text
user clicks "This is mine"
    ↓
notification created
    ↓
chat created
```

---

# 82. Claim Architecture Bypass

This bypasses the richer documented:

```text
Claim
Verification
Proof
Handover
```

model.

Therefore:

```text
claim architecture exists in documentation/backend schema
```

but:

```text
main Flutter flow does not consistently use it
```

---

# 83. Dzoka Lesson

A domain model is only real if all user channels invoke it.

Dzoka must prevent:

```text
web → real Claim workflow

WhatsApp → shortcut

mobile → notification-only shortcut
```

---

# 84. One Authoritative `SubmitClaim`

All channels should invoke:

```text
SubmitClaim
```

through the same application/domain logic.

---

# 85. Direct Chat Before Verification

The same item detail screen allows users to initiate chat with the item poster directly.

There is no requirement for:

```text
approved match
verified claim
recovery authorization
```

---

# 86. Privacy / Fraud Implication

A malicious user can potentially contact finders directly and socially engineer item release without going through intended ownership verification.

---

# 87. Dzoka Messaging Rule

Recovery communication should normally require an authorized relationship such as:

```text
active claim
approved conversation membership
assigned recovery case
```

according to policy.

---

# 88. Claim Trigger

If a real:

```text
claims/{claimId}
```

document is created, the Cloud Function:

```text
onClaimCreated
```

runs.

---

# 89. Item Becomes Claimed Immediately

On any new claim, the function updates the item to:

```text
status = claimed
```

before verifying ownership.

This is too strong.

---

# 90. Claim Submission ≠ Item Claimed

The existence of a claim means:

```text
someone asserts entitlement
```

not:

```text
the item has a valid claimant
```

---

# 91. Dzoka Correction

A new claim should not mutate authoritative item recovery status.

Instead:

```text
claim.submitted
```

is recorded independently.

---

# 92. Handover Code Generation

The claim trigger generates a six-digit handover code immediately after claim creation.

This concept is useful.

The implementation is not.

---

# 93. Critical Finding — Non-Cryptographic Handover Code

The code uses:

```text
Math.random()
```

to generate the handover code.

This is not a cryptographically secure random generator.

---

# 94. Dzoka Rejection

Never use:

```text
Math.random()
```

for:

```text
OTP
handover credential
password reset token
recovery token
authentication code
```

---

# 95. Correct Dzoka Handover Credential

Use a cryptographically secure source and store:

```text
token hash
expiry
attempt count
usedAt
revokedAt
recoveryId
recipient binding
```

---

# 96. Handover Code Generated Too Early

The code is issued on:

```text
CLAIM CREATED
```

rather than after:

```text
Claim Approved
+
Recovery Authorized
```

---

# 97. Correct Lifecycle

```text
Claim Submitted
    ↓
Verification
    ↓
Approved
    ↓
RecoveryAuthorization
    ↓
HandoverCredential issued
```

---

# 98. No Verified Handover Validation Path Found

The audit found documentation/data fields for:

```text
handoverCode
handoverConfirmed
```

and generation logic.

A complete corresponding validation protocol was not identified in the inspected active implementation.

---

# 99. Therefore Handover Classification

The handover capability is:

```text
PARTIAL
```

rather than:

```text
production-complete
```

---

# 100. Why It Still Matters

The conceptual presence of:

```text
claim
meetup
handover code
confirmation
completed state
```

is useful.

This is one reason FindBack remains a good recovery-workflow reference.

---

# 101. Dzoka Custody Extension

Dzoka transforms the simple handover concept into:

```text
RecoveryAuthorization
        ↓
Handover
        ↓
Credential
        ↓
Credential Validation
        ↓
CustodyTransfer
        ↓
Recipient Confirmation
        ↓
RecoveryCompleted
```

---

# 102. Push Notifications

FindBack contains actual notification architecture using:

```text
Firebase Cloud Messaging
```

in Cloud Functions.

This is one of the stronger practical parts of the project.

---

# 103. Match Notifications

High-confidence match candidates can trigger:

```text
Potential Match Found
```

push notifications to lost-item and found-item users.

---

# 104. Claim Notification

When a claim document is created, the owner can receive:

```text
New Claim Request
```

through FCM.

---

# 105. Positive Event Pattern

Conceptually:

```text
business event
    ↓
notification
```

is a good pattern.

---

# 106. Dzoka Difference

Dzoka should not couple business logic directly to FCM.

Instead:

```text
domain/integration event
        ↓
Messaging Worker
        ↓
NotificationPolicy
        ↓
FCM / WhatsApp / SMS / Email
```

---

# 107. Notification Delivery State

FindBack also stores notifications in Firestore.

This supports:

```text
in-app notification center
read/unread state
```

which is useful.

---

# 108. Dzoka Notification Model

Dzoka additionally needs:

```text
Notification
NotificationAttempt
provider message ID
retry
failure reason
delivery status
```

---

# 109. Reputation

FindBack updates reputation when claims transition to:

```text
completed
```

---

# 110. Reputation Updates

The event adds roughly:

```text
owner:
+50 reputation
+1 returned item

claimant:
+30 reputation
```

This is a useful event-driven trust concept.

---

# 111. Good Principle

Reputation should be based on:

```text
completed recovery outcomes
```

rather than self-declared actions.

---

# 112. But Recovery Completion Must Be Authoritative

If clients can improperly transition claims to:

```text
completed
```

then reputation becomes gameable.

This leads directly to one of the most important Firestore-rule findings.

---

# 113. Firestore Security Rules

The repository contains relatively detailed:

```text
firestore.rules
```

This is much better than leaving Firestore broadly open.

There are, however, several critical authorization weaknesses.

---

# 114. Positive Rule Concepts

Good concepts include:

```text
authenticated access checks
owner checks
admin roles
police roles
participant-based message access
restricted audit logs
claim participant reads
```

---

# 115. Critical Finding — Users Can Modify Their Own Role/Reputation

The comment states:

```text
Users can update their own profile
except role and reputation
```

but the actual rule is essentially:

```text
allow update
if request.auth.uid == userId
```

There is no field-difference restriction protecting:

```text
role
reputation
```

---

# 116. Consequence

A user may potentially attempt to self-modify:

```text
role = admin
```

or:

```text
reputation = 1000
```

if no other control blocks it.

This is a critical authorization/integrity flaw.

---

# 117. Dzoka Rule

Sensitive fields must be protected by server-side/domain authorization.

Never rely on comments or UI hiding.

---

# 118. Claim Update Rule

Claims may be updated by:

```text
claimant
item owner
police
```

without sufficiently restricting which fields each role may modify.

---

# 119. Potential Claim Authority Problem

A claimant might potentially modify fields such as:

```text
status
verifiedBy
verificationNotes
itemOwnerId
handoverCode
```

depending on the submitted Firestore update.

---

# 120. Dzoka Rejection

Clients must not have generic mutation rights over authoritative claim documents.

Use commands such as:

```text
SubmitClaimEvidence
WithdrawClaim
ReviewClaim
ApproveClaim
RejectClaim
```

with server-authoritative state transitions.

---

# 121. Claim State Machine Must Be Enforced

Firestore rules should not permit:

```text
pending
→ completed
```

merely because the claimant owns the record.

---

# 122. Match Updates

Users involved in matches can update their match records.

This may be acceptable for limited:

```text
viewed
dismissed
```

state, but should not permit mutation of authoritative matching evidence or scores.

---

# 123. Match Data Immutability

Separate:

```text
user interaction state
```

from:

```text
matching evidence
```

so clients cannot rewrite model output.

---

# 124. Chat Creation Rule

An authenticated user can create a chat if:

```text
they are one of exactly two participants
```

and item fields exist.

It does not require that the second participant:

```text
owns the item
has matched
has approved a claim
```

---

# 125. Consequence

If another user's ID is known, unauthorized conversation creation may be possible.

---

# 126. Dzoka Conversation Policy

Creation must check an actual recovery relationship:

```text
case
claim
match
staff assignment
```

rather than only participant array membership.

---

# 127. Message Sender Validation

A positive detail:

Firestore message creation requires:

```text
senderId == request.auth.uid
```

and the authenticated user must belong to the chat.

This is stronger than the messaging authorization seen in La2eet.

---

# 128. Notifications Creation Weakness

The rules allow:

```text
any authenticated user
```

to create notification records.

This could permit:

```text
notification spoofing
spam
fake system messages
```

unless application conventions prevent it.

---

# 129. Dzoka Rule

System notifications should normally be created by:

```text
trusted backend worker
```

rather than arbitrary clients.

---

# 130. Audit Log Rules

Clients cannot:

```text
create
update
delete
```

audit logs.

Only admins may read them.

This is a good pattern.

---

# 131. Storage Rules

FindBack provides dedicated:

```text
storage.rules
```

for:

```text
item images
blurred images
proof documents
avatars
chat media
report evidence
```

This is a useful storage-domain separation.

---

# 132. Proof Document Privacy

Proof documents are restricted to a narrower audience than public item images.

That is a positive design direction.

---

# 133. Comment / Rule Mismatch

The rule comment says proof documents are readable by:

```text
uploader
item owner
police
```

but the actual rule grants:

```text
uploader
admin
police/admin role
```

without a clear item-owner check.

Again:

```text
comment ≠ enforcement
```

---

# 134. Proof Deletion

The uploader can delete their proof document.

There is no clear restriction based on:

```text
claim submitted
under review
legal hold
```

---

# 135. Evidence Integrity Problem

A claimant could potentially remove evidence after submission/review.

Dzoka should preserve submitted evidence according to retention policy.

---

# 136. Dzoka Evidence Rule

After claim submission:

```text
evidence becomes immutable
```

or changes are represented by:

```text
new evidence version
withdrawal event
```

rather than silent deletion.

---

# 137. Chat Media Delete Rule

For chat media, the delete rule is effectively:

```text
allow delete if authenticated
```

without checking the authenticated user is a chat participant or uploader.

This is a serious storage authorization flaw.

---

# 138. Dzoka Rule

Deleting restricted media must require:

```text
ownership
conversation membership
policy
```

and preserve required audit history.

---

# 139. Original Item Images

Item images are readable by:

```text
any authenticated user
```

according to storage rules.

---

# 140. Privacy Conflict

README claims public privacy mechanisms including:

```text
blurred images
NIC redaction
face redaction
```

but the default backend's automatic redaction path is incomplete.

If original images contain sensitive data and are readable by all authenticated users, privacy protections may be bypassed.

---

# 141. Dzoka Media Projection

Users should normally access:

```text
public/sanitized derivative
```

rather than the sensitive original.

Original access should require explicit authorization.

---

# 142. Chat Encryption Claim

README and privacy documentation repeatedly describe chat as:

```text
end-to-end encrypted
AES-256
```

---

# 143. Actual Chat Storage

The inspected Flutter chat implementation writes:

```text
text: message
senderId
timestamp
isRead
```

directly into Firestore.

No encryption/decryption layer was identified around the message body.

---

# 144. Audit Conclusion — E2EE Not Demonstrated

The audited source does not demonstrate actual application-level:

```text
end-to-end encryption
```

of chat messages.

Firebase TLS and encryption at rest are not the same as E2EE.

---

# 145. Dzoka Documentation Rule

Never claim:

```text
E2EE
```

unless:

```text
client-side key generation
key exchange
message encryption
ciphertext storage
client-side decryption
```

are genuinely implemented and threat-modeled.

---

# 146. Communication Is Still Access-Controlled

Despite the E2EE claim mismatch, Firestore participant rules do provide access controls around chat messages.

That remains useful, but it should be accurately described as:

```text
access-controlled Firebase messaging
```

rather than E2EE.

---

# 147. Audit Logging Implementation

`functions/src/index.ts` creates audit logs for changes to:

```text
users
items
claims
police_verifications
```

---

# 148. Full Before/After Snapshot

The audit log stores:

```text
before
after
```

document objects.

This improves traceability.

It also introduces privacy/security risk.

---

# 149. Secret Duplication Risk

Blindly copying whole documents into audit logs can duplicate:

```text
PII
private evidence
phone numbers
verification information
sensitive identifiers
```

into a long-lived audit store.

---

# 150. Dzoka Audit Principle

Audit should record:

```text
who
what
when
why
result
selected changed fields
```

without automatically duplicating every secret payload.

---

# 151. Retention Classes

Audit data may require longer retention than operational data.

Therefore secret evidence should not accidentally gain:

```text
audit-log retention
```

simply because a record changed.

---

# 152. User Deletion Cleanup

FindBack has an authentication deletion trigger that removes:

```text
user document
user items
user messages
```

---

# 153. Cleanup Is Incomplete

It does not clearly delete/reconcile all possible user-related:

```text
claims
matches
chats
storage objects
proof documents
notifications
face records
audit records
```

---

# 154. Dzoka Data Deletion

Deletion requires a complete domain-aware workflow covering:

```text
canonical records
derived data
media
embeddings
caches
federated copies
analytics
retention holds
```

---

# 155. Police Verification

The data model includes:

```text
police_verifications
```

and Firestore roles for:

```text
police
admin
```

This is an interesting institutional verification concept.

---

# 156. Dzoka Generalization

Instead of hard-coding only police, Dzoka should support:

```text
VerificationAuthority
```

such as:

```text
police
airport security
university security
employer
transport operator
government agency
```

depending on deployment.

---

# 157. Role-Based Verification

High-value or sensitive item categories may require stronger verification policy.

Examples:

```text
passport
national identity document
high-value electronics
institutional asset
```

---

# 158. Dzoka Policy Example

```text
if item.category == PASSPORT:
    require identity validation
    require authorized institutional reviewer
    prohibit casual finder-to-owner handover
```

---

# 159. Positive Proof Document Separation

The repository stores claim proof documents under:

```text
proof_documents/
```

rather than putting them in ordinary public item-image paths.

This is worth adapting.

---

# 160. Dzoka Storage Classes

Dzoka should similarly separate:

```text
public media
restricted evidence
secret verification media
custody evidence
```

with different access policies.

---

# 161. Match Notifications

A useful UX feature is notifying both:

```text
lost-item reporter
found-item reporter
```

about high-quality candidates.

---

# 162. Privacy-Safe Wording

Notifications should say:

```text
Potential match found
```

not:

```text
We found your item
```

until ownership is verified.

---

# 163. Match Score Presentation

FindBack displays percentage-like match scores.

Because the underlying scores are heuristic and sometimes random, these percentages can look more probabilistic than they are.

---

# 164. Dzoka Rule

Only label a value:

```text
confidence %
```

if its meaning has been calibrated and documented.

Otherwise prefer:

```text
match score
match tier
candidate rank
```

---

# 165. Duplicate Match Records

The Flutter matcher writes two mirrored match records:

```text
A → B
B → A
```

for the same relationship.

This simplifies per-user reads.

It also duplicates canonical state.

---

# 166. Risk

Two match records can drift:

```text
status differs
score differs
one deleted
one updated
```

---

# 167. Dzoka Design

Store one authoritative:

```text
MatchCandidate
```

and build per-user read projections as necessary.

---

# 168. Match Schema Inconsistency

`saveMatches()` stores fields such as:

```text
itemId
matchedItemId
```

while `getSavedMatches()` queries for:

```text
lostItemId
foundItemId
```

This indicates schema drift between writing and reading paths.

---

# 169. Dzoka Rule

Contracts for persisted events/entities should be schema-tested.

---

# 170. Offline-First Claim

README advertises:

```text
offline-first
```

but the changelog explicitly lists offline-first architecture as planned for a later release.

---

# 171. Dzoka Lesson

Again:

```text
feature description
≠
implemented capability
```

---

# 172. Notification Maturity

The repository has stronger notification implementation than its older changelog suggests.

FCM dependencies and Cloud Function notification code are present.

Therefore some originally planned features were partially implemented later.

This reinforces the need to audit the pinned revision rather than rely on one historical document alone.

---

# 173. Face Detection vs Face Recognition

FindBack contains meaningful face-detection code.

This can help privacy redaction.

The alternate backend additionally attempts face-based search.

These should be treated as separate capabilities.

---

# 174. Dzoka Decision

### Potentially appropriate

```text
detect face
→ redact face
```

### High-risk / not default

```text
detect face
→ identify person / search database
```

---

# 175. NIC OCR

The Flutter service recognizes Sri Lankan NIC formats from OCR.

This is useful as an example of:

```text
region-specific protected identifier extraction
```

---

# 176. Dzoka Localization Lesson

Different countries may require different identifier detectors:

```text
Zimbabwe national ID
passport
IMEI
institution asset tag
serial
```

These should be provider/rule modules rather than one universal regex.

---

# 177. Exact Identifier Match Bonus

FindBack awards a large bonus when NIC values match.

The underlying idea is valid:

```text
strong identifiers
```

can carry much more identity information than generic text.

---

# 178. Missing Conflict Side

However, a mismatch should also be modeled explicitly.

Example:

```text
NIC A
vs
NIC B
```

should not merely fail to receive the bonus.

It may represent a:

```text
CRITICAL CONFLICT
```

---

# 179. Dzoka Improvement

Protected identifier logic belongs in:

```text
ConflictEngine
```

as well as normal feature scoring.

---

# 180. Matching Quality Summary

### Useful

```text
text normalization
fuzzy matching
synonyms
OCR
brand extraction
color normalization
location
time
protected-identifier awareness
```

### Weak

```text
strict category gates
strict district gates
default missing scores
hard radius
createdAt instead of incident time
heuristic confidence
```

### Reject

```text
random image similarity
random image embeddings
```

---

# 181. Claim Quality Summary

### Useful concepts

```text
Claim entity
proof documents
verification questions
police/admin review
meetup
handover code
completion
```

### Runtime weaknesses

```text
claim screen placeholder
main app does not create Claim
notification used instead
chat starts immediately
item marked claimed upon raw claim creation
claim update rules too permissive
```

---

# 182. Handover Quality Summary

### Useful concept

```text
six-digit recovery handover credential
```

### Weak implementation

```text
Math.random
plaintext
issued too early
no expiry
no attempts
no hash
no clear validation
no clear single-use control
```

---

# 183. Messaging Quality Summary

### Useful

```text
participant access rules
sender ID validation in Firestore
FCM notifications
in-app messages
```

### Weak

```text
chat creation not tied to approved claim
E2EE claims unsupported by inspected source
```

---

# 184. Security Rules Summary

### Good

```text
authenticated access patterns
owner checks
police/admin concepts
message participant checks
audit write protection
proof-document separation
default deny in Storage
```

### Dangerous

```text
user self-update can affect role/reputation
claim updates too broad
notifications can be client-created
chat creation too permissive
chat media deletion too permissive
proof evidence deletable after submission
original item media broadly readable
```

---

# 185. Privacy Summary

### Good concepts

```text
blurred derivatives
private proof paths
OCR detection
face detection
data deletion trigger
privacy documentation
```

### Weaknesses

```text
backend blur implementation incomplete
original media readable to authenticated users
audit snapshots may duplicate secrets
user cleanup incomplete
biometric face search code exists
E2EE documentation overstates implementation
```

---

# 186. What Dzoka Should Adopt

Because FindBack uses MIT, direct reuse is legally possible where technically suitable.

Strong candidates for selective adoption/adaptation include concepts around:

```text
event-triggered notifications
Firebase-style participant authorization ideas
proof media separation
append-oriented audit concepts
reputation after completed recovery
police/institution reviewer concepts
```

Actual code should still be adapted to Dzoka's Java/Python architecture rather than copied wholesale.

---

# 187. What Dzoka Should Adapt

High-value concepts to adapt:

```text
claim lifecycle model

proof document support

verification questions

institutional/police review

push notification triggers

handover credential concept

meetup/handover metadata

reputation after confirmed recovery

audit records

sensitive-media storage separation

mobile-first recovery interaction
```

---

# 188. What Dzoka Should Reference

Reference FindBack for:

```text
Flutter lost/found UX

Firebase event-trigger patterns

Sri Lanka localization

mobile OCR/privacy ideas

notification UX

role-driven institutional verification

community reputation
```

---

# 189. What Dzoka Should Explicitly Reject

Do not inherit:

```text
random image similarity

random production-like embeddings

missing evidence = score 50

strict category candidate gate

strict district candidate gate

hard-coded universal geographic cutoff

creation timestamp as incident timestamp

Math.random handover codes

handover code at claim creation

client-authoritative claim transitions

self-modifiable role/reputation

client-created system notifications

immediate direct chat before verification

E2EE claims without E2EE implementation

whole-document audit snapshots containing secrets
```

---

# 190. Source Claims vs Actual Implementation

| Capability | Source Audit Result |
|---|---|
| MIT license | **Implemented / verified** |
| Flutter app | **Implemented** |
| Firebase Auth / Firestore | **Implemented** |
| FCM notifications | **Implemented in backend paths** |
| OCR | **Implemented client-side with ML Kit** |
| Face detection | **Implemented in selected paths** |
| NIC extraction | **Implemented** |
| Text matching | **Implemented** |
| Location matching | **Implemented** |
| Time scoring | **Implemented** |
| Image similarity via TensorFlow Lite | **Not implemented in active paths** |
| Default backend image similarity | **Random placeholder** |
| Image embeddings | **Placeholder/random in default backend** |
| Auto-blur server workflow | **Incomplete in default deployed function path** |
| Claim schema | **Documented** |
| Full Flutter claim workflow | **Placeholder / bypassed** |
| Claim FCM notification | **Implemented when claim doc exists** |
| Handover code generation | **Implemented but insecure** |
| Handover-code validation | **Not demonstrated in audited active flow** |
| Encrypted chat | **Access-controlled messaging exists; E2EE not demonstrated** |
| Police verification model | **Documented/rule support exists** |
| Reputation on completed claim | **Implemented in Cloud Functions** |
| Offline-first | **README claim; changelog indicates planned/incomplete** |

---

# 191. Comparison with globalenglish01/lostfound

`globalenglish01/lostfound` is much stronger for:

```text
matching semantics
hard conflicts
missing evidence
model provenance
retrieval
coverage
```

FindBack is more useful for:

```text
mobile UX
notifications
handover concepts
claim schema
community trust/reputation
institutional verification
```

---

# 192. Comparison with FuricBond/Lost-And-Found

FuricBond provides much stronger actual visual evidence:

```text
YOLO
OpenCLIP
pHash
ORB
```

FindBack's default image matching is a placeholder.

---

# 193. Comparison with Malak-Rashad/LostAndFound

Malak-Rashad provides a stronger actual private-question workflow.

FindBack documents richer:

```text
proof documents
security questions
police verification
handover code
```

but its main Flutter claim flow is not wired to that complete model.

---

# 194. Comparison with Lost-and-Found-Tracking-System

`lost-and-found-system` is stronger for:

```text
claim administration
claim conflicts
fraud concepts
admin decisions
audit
```

FindBack contributes more around:

```text
mobile
FCM
handover code
reputation
police roles
```

---

# 195. Comparison with Smart Asset Finder

Smart Asset Finder is stronger as a reference for:

```text
QR
WhatsApp
hardware tags
reward economy
```

FindBack is stronger for:

```text
Firebase-native mobile workflows
push notifications
structured security rules
claim handover concept
```

---

# 196. Dzoka Source Synthesis

FindBack contributes:

```text
RECOVERY UX
FCM / PUSH PATTERNS
HANDOVER CREDENTIAL IDEA
PROOF MEDIA
REPUTATION
INSTITUTION REVIEW
```

while other audited sources contribute:

```text
globalenglish
→ matching discipline

FuricBond
→ multimodal vision

Malak/La2eet
→ private challenges

Lost-and-Found-Tracking-System
→ claim/risk workflow

Smart Asset Finder
→ QR / WhatsApp / incentives
```

---

# 197. Dzoka Mapping — Matching

FindBack:

```text
Firestore candidate query
    ↓
heuristic scoring
    ↓
optional/random image component
    ↓
match document
```

Dzoka:

```text
ItemFingerprint
    ↓
hybrid indexed retrieval
    ↓
rank fusion
    ↓
hard conflicts
    ↓
multimodal feature comparison
    ↓
local-instance reranking
    ↓
evidence coverage
    ↓
confidence calibration
    ↓
MatchCandidate
```

---

# 198. Dzoka Mapping — Claim

FindBack conceptual model:

```text
Claim
├── proofDocuments
├── proofAnswers
├── verifiedBy
├── handoverCode
└── status
```

Dzoka:

```text
Claim
    ↓
ClaimEvidence
    ↓
VerificationSession
    ↓
RiskAssessment
    ↓
ClaimConflict
    ↓
Entitlement
    ↓
ClaimDecision
```

---

# 199. Dzoka Mapping — Handover

FindBack:

```text
claim created
    ↓
handover code generated
```

Dzoka:

```text
claim approved
    ↓
verification satisfied
    ↓
recovery authorized
    ↓
handover credential generated
    ↓
credential verified
    ↓
custody transfer
```

---

# 200. Dzoka Mapping — Reputation

FindBack:

```text
claim completed
    ↓
reputation increment
```

Dzoka:

```text
RecoveryCompleted
    ↓
Reward/Reputation Policy
    ↓
eligible contribution
    ↓
ledger entry
```

---

# 201. Dzoka Mapping — Notifications

FindBack:

```text
Firestore trigger
→ FCM
```

Dzoka:

```text
Domain Event
    ↓
Transactional Outbox
    ↓
Kafka-compatible broker
    ↓
Messaging Worker
    ↓
FCM / WhatsApp / SMS / Email
```

---

# 202. Dzoka Mapping — Privacy

FindBack:

```text
original media
blurred media
proof documents
```

Dzoka:

```text
MediaAsset
├── ORIGINAL / SECRET
├── SANITIZED / RESTRICTED
├── PUBLIC DERIVATIVE
├── OCR DERIVATIVE
└── VERIFICATION EVIDENCE
```

---

# 203. Architecture Documents Influenced

This audit should inform:

```text
docs/architecture/ai-architecture.md
docs/architecture/security-architecture.md
docs/architecture/privacy-architecture.md
docs/architecture/whatsapp-architecture.md
docs/architecture/data-architecture.md
docs/architecture/event-architecture.md
docs/architecture/integration-architecture.md
docs/architecture/rewards-architecture.md

docs/design/item-fingerprint.md
docs/design/matching-engine.md
docs/design/claim-workflow.md
docs/design/ownership-verification.md
docs/design/custody-state-machine.md
```

---

# 204. Potential ADRs Influenced

Possible ADRs include:

```text
use cryptographically secure handover credentials

generate handover credentials only after recovery authorization

separate reputation from client-controlled state

private proof media requires restricted object access

client channels cannot update authoritative claim state directly

missing evidence is unavailable, not neutral 50

face recognition is excluded from default recovery pipeline

public/sanitized media separate from original media
```

---

# 205. Security Anti-Pattern — Client Authority

FindBack strongly reinforces a general Dzoka rule:

```text
Firebase/Frontend ownership
≠
domain authorization
```

Even with strong backend-as-a-service rules, high-value recovery state should be changed through explicit server-authoritative use cases.

---

# 206. Security Anti-Pattern — Comments vs Enforcement

Examples in FindBack include comments claiming:

```text
role/reputation protected
proof visible to item owner
```

while actual rules do not completely enforce the comments.

Therefore:

> **Security documentation is not a control. The executable policy is the control.**

---

# 207. AI Anti-Pattern — Random Evidence

The strongest negative lesson from this repository is:

```text
random score
```

must never masquerade as:

```text
AI evidence
```

---

# 208. Why Random Image Matching Is Especially Dangerous

Because image similarity carries:

```text
40%
```

of one default score, random data can move candidates across notification thresholds.

This can generate false:

```text
high-confidence match
```

notifications.

---

# 209. Dzoka Anti-Pattern Definition

Add to:

```text
docs/source-audit/findings/rejected-patterns.md
```

the rule:

### Synthetic Modality Evidence

Never manufacture a feature value merely because a modality is unavailable.

Examples:

```text
random image score
constant image score
text-derived image score
default location score 50
default time score 50
```

Correct representation:

```text
available = false
score = null
```

---

# 210. Recovery Anti-Pattern — Credential Before Authorization

FindBack also demonstrates:

```text
ClaimSubmitted
    ↓
HandoverCodeIssued
```

as a dangerous ordering.

Dzoka requires:

```text
ClaimApproved
+
RecoveryAuthorized
    ↓
HandoverCredentialIssued
```

---

# 211. Trust Anti-Pattern — Self-Editable Reputation

Reputation should never be a client-editable profile attribute.

It must be:

```text
server-derived projection
```

from trusted events.

---

# 212. Role Anti-Pattern — Self-Editable Role

Similarly:

```text
role
```

must never be freely mutable by the user who owns the profile record.

---

# 213. Audit Anti-Pattern — Secret Replication

Do not blindly archive entire:

```text
before
after
```

objects when those objects may contain secrets.

Audit data itself requires a data-classification model.

---

# 214. Positive Pattern — Trigger-Based Workflow

FindBack does demonstrate a useful event-like pattern:

```text
new item
→ matching

new claim
→ notification

completed claim
→ reputation
```

Dzoka preserves this pattern using a more explicit durable event architecture.

---

# 215. Positive Pattern — Separate Proof Storage

The project also demonstrates that claim proof media deserves stronger storage rules than ordinary item imagery.

Dzoka adopts this strongly.

---

# 216. Positive Pattern — Multi-Stakeholder Recovery

The product includes concepts for:

```text
citizen
police
university admin
admin
```

This reinforces that recovery is a multi-stakeholder process rather than only peer-to-peer matching.

---

# 217. Positive Pattern — Recovery Completion Drives Trust

Reputation changes after:

```text
completed claim
```

rather than simply posting many items.

This is directionally good because trust should come from verified outcomes.

---

# 218. Positive Pattern — FCM Push

Push alerts for:

```text
high-quality match
new claim
messages
```

are important because recovery is time-sensitive.

Dzoka should support these through a provider abstraction.

---

# 219. Testing Requirement Derived from FindBack

Dzoka must include tests that specifically ensure:

```text
no random match inputs in production

same evidence produces deterministic candidate scoring

missing evidence does not get default positive value

claimant cannot approve own claim

claimant cannot complete recovery

user cannot edit role

user cannot edit reputation

handover token uses secure randomness

handover code cannot be issued before authorization

proof evidence cannot be deleted after submission without policy

notification records cannot be spoofed by clients
```

---

# 220. Model Governance Requirement

Every actual AI/ML feature must have:

```text
provider
model
version
configuration
input hash
output
status
generatedAt
```

This prevents placeholder/random values from being confused with model output.

---

# 221. Feature Status Requirement

Dzoka documentation should classify major capabilities as:

```text
IMPLEMENTED
PARTIAL
PLACEHOLDER
PLANNED
DISABLED
```

rather than relying only on checkbox lists.

---

# 222. Deployment Reality Requirement

Architecture audits should always identify:

```text
which code path is actually deployed
```

when multiple parallel implementations exist.

FindBack's:

```text
functions/
findback-functions/
```

structure is a strong example.

---

# 223. Recommended Source-Audit Lock Entry

Add to:

```text
docs/source-audit/source-audit.lock.json
```

```json
{
  "id": "findback",
  "category": "platforms",
  "repository": "https://github.com/pirinthaban/findback",
  "revision": "fc91563886e4e8da6931bc07dd025fbe235068a2",
  "license": "MIT",
  "classification": "ADAPT",
  "secondaryClassification": "REFERENCE",
  "decisionNote": "Adapt recovery, notification, proof, reputation and handover concepts. Reject current matching implementation as a foundation.",
  "audit": "docs/source-audit/platforms/findback.md",
  "auditedAt": "2026-10-04"
}
```

---

# 224. Re-Audit Triggers

Re-audit FindBack if upstream:

```text
replaces random image similarity with real embeddings

adds TFLite dependency and actual model inference

unifies functions/ and findback-functions/

implements full claim UI

routes claims through claims collection consistently

adds handover-code validation

switches handover code to cryptographic randomness

adds expiry / attempt / single-use controls

tightens Firestore claim transitions

prevents users from editing role/reputation

implements actual chat E2EE

completes server-side media redaction

changes proof-document access rules
```

---

# 225. Final Classification

```text
PRIMARY:
ADAPT SELECTIVELY
```

Specifically:

```text
ADAPT:
recovery/handover concepts
FCM notification patterns
claim/proof schema ideas
institutional review
reputation-after-recovery
storage separation
```

and:

```text
REJECT:
matching implementation as Dzoka foundation
```

---

# 226. Adopt Directly

No complete FindBack subsystem should be adopted as a Dzoka runtime dependency.

Selective MIT-compatible implementation ideas may be reused where appropriate, but Dzoka uses a different core architecture.

---

# 227. Adapt

Strong concepts to adapt:

```text
match notifications

claim notifications

proof documents

verification questions

institutional/police review

meetup metadata

handover credentials

reputation after confirmed recovery

event-triggered processing

privacy-separated media paths

mobile-first recovery flows
```

---

# 228. Reference

Use as reference for:

```text
Flutter UX

Firebase authorization patterns

push notification UX

Sri Lankan localization

OCR-based identifier discovery

mobile privacy warnings

community trust UX
```

---

# 229. Explicitly Reject

```text
random image match scores

random image embeddings

synthetic modality evidence

strict category/district retrieval gates

default missing evidence score = 50

Math.random security codes

handover code before verification

generic client claim mutation

client-editable role/reputation

claim flow that bypasses Claim aggregate

chat without recovery relationship

E2EE claims unsupported by source

unbounded full-payload audit replication
```

---

# 230. Final Assessment

FindBack is a useful source because it attempts something broader than simple lost/found search:

```text
MATCHING
+
CLAIMS
+
PROOF
+
NOTIFICATIONS
+
TRUST
+
HANDOVER
+
INSTITUTIONAL VERIFICATION
```

Its strongest contribution to Dzoka is the recovery workflow thinking around those components.

Its biggest weakness is that several marketed capabilities are more mature in documentation than they are in the default deployed implementation.

The most important example is:

```text
README:
AI image similarity

SOURCE:
Math.random() image score
```

That is exactly the kind of problem Dzoka's mandatory source-audit process exists to catch.

The useful architecture can therefore be distilled as:

```text
ITEM REPORTED
      ↓
MATCH EVENT
      ↓
USER NOTIFIED
      ↓
CLAIM
      ↓
PROOF / REVIEW
      ↓
RECOVERY AUTHORIZATION
      ↓
SECURE HANDOVER CREDENTIAL
      ↓
CUSTODY TRANSFER
      ↓
RECOVERY COMPLETED
      ↓
REPUTATION / LEARNING
```

with Dzoka replacing the weak implementation details with:

```text
REAL MULTIMODAL EVIDENCE
+
EXPLICIT MISSING-EVIDENCE SEMANTICS
+
HARD CONFLICTS
+
PRIVATE VERIFICATION
+
SERVER-AUTHORITATIVE CLAIM STATES
+
CRYPTOGRAPHIC HANDOVER TOKENS
+
FORMAL CUSTODY
+
AUDITABLE RECOVERY
```

---

# 231. Final Dzoka Lesson

FindBack reinforces three major Dzoka rules:

### 1. Never manufacture evidence.

```text
No visual model?
→ visual evidence unavailable

NOT
→ random 57%
```

### 2. Never issue recovery credentials before authorization.

```text
Claim created
≠
ownership verified
≠
release authorized
```

### 3. Never let client storage rules define business authority loosely.

```text
authenticated user
≠
authorized verifier

record owner
≠
allowed to edit every field

claimant
≠
allowed to complete recovery
```

The central conclusion is:

> **FindBack is a valuable MIT-licensed reference for mobile recovery UX, push notifications, proof documents, institutional verification, reputation, and handover concepts—but Dzoka should reject its current matcher, random AI placeholders, permissive claim-state controls, and insecure handover-code implementation, and rebuild those capabilities around real evidence, explicit domain authority, secure credentials, and formal custody.**