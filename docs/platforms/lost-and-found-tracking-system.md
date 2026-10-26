# Source Audit — Lost-and-Found-Tracking-System/lost-and-found-system

> **Dzoka — The Open Recovery Network**
>
> Source audit of `Lost-and-Found-Tracking-System/lost-and-found-system`, with emphasis on reporting, institutional submissions, geospatial modeling, ownership claims, claim conflicts, AI matching, fraud detection, administrative review, auditability, and operational workflows.

---

# 1. Audit Metadata

| Field | Value |
|---|---|
| Repository | `https://github.com/Lost-and-Found-Tracking-System/lost-and-found-system` |
| Upstream owner | `Lost-and-Found-Tracking-System` |
| Audited revision | `4ef1493f80590f6fa03d2ad9eded2e70a59ebe58` |
| Short revision | `4ef1493f` |
| Revision date | 2026-03-11 |
| Audit date | 2026-10-04 |
| Default branch | `main` |
| License | MIT |
| Backend | Node.js + Express + TypeScript |
| Frontend | React + Vite |
| Persistence | MongoDB + Mongoose |
| Cache / queue | Redis + Bull |
| Media storage | Cloudinary |
| Authentication | JWT + refresh sessions |
| Password hashing | Argon2 |
| Validation | Zod |
| Geospatial model | GeoJSON + MongoDB `2dsphere` |
| Detection | YOLOv8 via HuggingFace + Roboflow workflow |
| Visual embedding | OpenCLIP |
| Text similarity | custom TF-IDF |
| Notifications | In-app + SendGrid + Fast2SMS |
| Primary classification | **ADAPT WORKFLOW CONCEPTS** |
| Secondary classification | **REFERENCE AI/FRAUD IMPLEMENTATION** |
| Direct code reuse | Permitted under MIT, but unsafe patterns must not be copied |

---

# 2. Executive Decision

This repository is one of the more complete application-level lost-and-found systems audited for Dzoka.

It contains real implementations for:

```text
authentication
roles
item reporting
institutional submissions
campus geospatial zones
drafts
claims
claim conflicts
claim decisions
notifications
audit logs
retention policies
AI configuration
AI matching services
fraud/risk services
```

Unlike many small lost-and-found projects, it attempts to model an actual operational organization rather than only:

```text
post item
browse items
contact finder
```

Its strongest value to Dzoka is therefore:

```text
workflow architecture
+
claim state concepts
+
institutional context
+
geospatial organization
+
admin review
+
fraud signals
+
auditability
```

However, several implementation choices are unsafe or incomplete.

Dzoka should classify the project as:

```text
ADAPT WORKFLOW CONCEPTS
```

rather than:

```text
ADOPT APPLICATION
```

---

# 3. Why This Repository Matters

This source is especially relevant to Dzoka because it models several concepts missing from simpler projects:

```text
Organization
Campus Zone
Ownership Claim
Claim Conflict
Claim Decision
Fraud Assessment
AI Configuration
Audit Log
Data Retention Policy
```

These map closely to several Dzoka bounded contexts.

---

# 4. Audit Scope

Primary inspected files include:

```text
LICENSE
README.md

backend/src/
├── models/
│   ├── items.ts
│   ├── claims.ts
│   ├── ai.ts
│   ├── locations.ts
│   └── audit.ts
│
├── services/
│   ├── itemService.ts
│   ├── embeddingService.ts
│   ├── findImagePairs.ts
│   └── fraudDetectionService.ts
│
├── routes/v1/
│   ├── claims.ts
│   └── admin.ts
│
└── config/
    └── env.ts
```

Repository tests and E2E documentation were also examined for relevant coverage.

---

# 5. Audit Scope Limit

This is a targeted architectural source audit.

It is not:

- a complete penetration test;
- a full dependency audit;
- a legal opinion;
- a complete ML benchmark reproduction;
- a review of every frontend animation or user-interface component.

---

# 6. License

The repository contains a valid root:

```text
LICENSE
```

with an MIT license.

Therefore direct reuse is legally more straightforward than repositories with missing licenses.

Normal MIT requirements still apply, including preservation of required copyright/license notices.

---

# 7. High-Level Architecture

The upstream system resembles:

```text
React Frontend
      ↓
Express REST API
      ↓
Business Services
      ↓
MongoDB
      ↓
Redis / Bull
      ↓
External Providers
├── Cloudinary
├── SendGrid
├── Fast2SMS
├── HuggingFace
└── Roboflow
```

This is a reasonable campus-scale application architecture.

---

# 8. Repository Scope

The application supports several operational domains:

```text
Identity
Items
Organizations
Locations
Claims
Notifications
Administration
AI Matching
Fraud Detection
Audit
Retention
```

This is significantly broader than most direct lost-and-found repositories audited.

---

# 9. Item Model

`backend/src/models/items.ts` defines a reasonably structured item model.

Conceptually:

```text
Item
├── trackingId
├── submissionType
├── submittedBy
├── isAnonymous
├── organizationId
├── authorizationProofId
├── itemAttributes
├── images[]
├── location
├── timeMetadata
├── status
├── admin review
└── aiMetadata
```

---

# 10. Submission Types

Items are classified as:

```text
lost
found
```

This is straightforward and correct.

Dzoka preserves the semantic distinction while separating reports more explicitly into:

```text
LostReport
FoundReport
ItemRecord
RecoveryCase
```

---

# 11. Structured Attributes

The upstream model contains:

```text
category
color
material
size
description
```

This is useful but limited.

Dzoka generalizes this using configurable attribute definitions rather than fixing the entire attribute model to a small schema.

---

# 12. Item Lifecycle

Upstream item statuses include:

```text
draft
pending
submitted
matched
resolved
archived
rejected
```

This is useful because it recognizes:

```text
report creation
administrative approval
matching
resolution
archive
```

as separate steps.

---

# 13. Dzoka Improvement

Dzoka should not make one `Item.status` field carry every workflow concern.

Separate bounded contexts own:

```text
report status
case status
claim status
custody state
recovery state
```

to avoid state explosion.

---

# 14. Tracking IDs

The service generates human-readable IDs such as:

```text
ITEM-<random hex>
```

using cryptographically generated random bytes.

This is a good pattern.

Dzoka likewise distinguishes:

```text
internal globally unique ID
```

from:

```text
human-friendly case/item reference
```

---

# 15. Anonymous Reporting

The upstream item model supports:

```text
isAnonymous
```

This is useful.

Dzoka should support different identity levels rather than assuming every report requires a fully registered account.

---

# 16. Institutional Submission

One especially useful upstream feature is:

```text
organizationId
authorizationProofId
```

for organization-submitted items.

This directly supports Dzoka's institutional use cases.

---

# 17. Organization Submission Flow

Conceptually:

```text
user
    ↓
organization
    ↓
authorization proof
    ↓
item submission
```

This is valuable for:

```text
airports
universities
bus operators
hotels
malls
employers
```

---

# 18. Dzoka Extension

Dzoka makes institutional authority more explicit using:

```text
Tenant
Organization
OrganizationUnit
Membership
Delegation
Policy
```

rather than storing only an `organizationId`.

---

# 19. Geographic Modeling

The item location model uses proper:

```text
GeoJSON Point
```

with:

```text
coordinates
zoneId
```

This is considerably stronger than raw latitude/longitude fields alone.

---

# 20. Geospatial Index

The item schema creates:

```text
2dsphere
```

indexing on location.

This is a good production-oriented decision for MongoDB deployments.

---

# 21. Campus Zones

The project also defines:

```text
CampusZone
```

as GeoJSON polygons.

Conceptually:

```text
CampusZone
├── zoneName
├── Polygon
├── active
└── createdBy
```

---

# 22. Zone Validation

During item submission, the service can validate whether a point intersects the selected zone.

Conceptually:

```text
point
    ↓
$geoIntersects
    ↓
campus zone polygon
```

This is a useful institutional feature.

---

# 23. Dzoka Location Lesson

A meaningful location is often not just:

```text
lat = ...
lon = ...
```

but:

```text
Airport
    ↓
Terminal 1
    ↓
Gate A
```

or:

```text
University
    ↓
Library
    ↓
Ground Floor
```

The upstream zone model reinforces Dzoka's hierarchical location architecture.

---

# 24. Dangerous Zone Validation Fallback

`validateZone()` catches errors and returns:

```text
true
```

with a warning when zone validation fails.

Conceptually:

```text
zone validation error
    ↓
allow submission
```

This is permissive.

---

# 25. Dzoka Decision

Whether a validation failure should fail open or fail closed must be policy-dependent.

Do not implement:

```text
validation service failed
→ therefore validation passed
```

as a general pattern.

---

# 26. Draft Submissions

The project stores partially completed reports using:

```text
DraftSubmission
```

with:

```text
userId
partialData
lastSavedAt
```

This is good UX architecture.

---

# 27. Dzoka Reuse

Dzoka can adopt the product concept of resumable reports for:

```text
web
PWA
WhatsApp
kiosk
```

especially where users may have unreliable connectivity.

---

# 28. Claim Model

The claim schema is one of the repository's strongest areas.

Conceptually:

```text
Claim
├── itemId
├── claimantId
├── claimedBy
├── ownershipProofs[]
├── proofScore
├── status
├── aiConfidenceScore
├── confidenceTier
├── submittedAt
├── resolvedAt
├── resolvedBy
├── adminNotes
└── isAdminOverride
```

---

# 29. Claim Statuses

Declared statuses include:

```text
pending
approved
rejected
withdrawn
conflict
```

This is much better than a simple:

```text
claimed = true
```

boolean.

---

# 30. Ownership Proofs

Claims can contain:

```text
ownershipProofs[]
```

which may represent text or image references.

This recognizes that ownership must be demonstrated through evidence.

---

# 31. Dzoka Extension

Dzoka expands this into typed evidence:

```text
ClaimEvidence
├── PHOTO
├── DOCUMENT
├── IDENTIFIER
├── PRIVATE_KNOWLEDGE
├── AUTHORIZATION
├── INSTITUTIONAL
└── OTHER
```

with classification and provenance.

---

# 32. Claim Conflicts

The upstream model explicitly defines:

```text
ClaimConflict
```

containing:

```text
itemId
conflictingClaims[]
detectedAt
resolved
```

This is an important architectural insight.

---

# 33. Dzoka Adoption

Dzoka strongly retains the idea that multiple claims form a separate domain condition.

A serious competing claim should be able to:

```text
freeze recovery
```

until resolution.

---

# 34. Claim Decisions

The repository defines:

```text
ClaimDecision
```

separately from the claim itself.

Conceptually:

```text
ClaimDecision
├── claimId
├── decidedBy
├── decision
├── remarks
├── approvalChain
└── timestamp
```

This is strong audit-oriented design.

---

# 35. Approval Chains

The upstream model includes:

```text
approvalChain[]
```

which can track multiple approvers.

This directly supports Dzoka's future:

```text
dual control
multi-review approval
high-value item policy
```

---

# 36. Archived Claims

The project also defines:

```text
ArchivedClaim
```

with a data snapshot.

This shows awareness that resolved historical workflow state may need to be retained independently.

---

# 37. Dzoka Archive Approach

Dzoka should avoid arbitrary copying of entire mutable documents where possible.

Prefer explicit immutable:

```text
decision records
event history
audit records
```

plus retention-controlled archival snapshots where required.

---

# 38. Claim Submission Workflow

The active claim route:

```text
POST /claims
```

requires authentication.

It creates:

```text
Claim
status = pending
proofScore = 0
confidenceTier = low
```

and emits an audit record.

---

# 39. Positive Audit Pattern

The claim submission creates an explicit:

```text
AuditLog
```

containing:

```text
actor
action
target entity
target ID
metadata
timestamp
```

This is a strong pattern.

---

# 40. Claim Withdrawal

The claim owner may withdraw their claim.

The implementation:

```text
checks claimant ownership
updates status
sets resolvedAt
writes audit log
```

This maps well to Dzoka's claim workflow.

---

# 41. Critical Security Finding — Public Claim Lookup

The route:

```text
GET /claims/:id
```

does not use:

```text
authMiddleware
```

at the audited revision.

It also populates claimant fields including:

```text
profile.fullName
profile.email
```

---

# 42. Security Consequence

A caller who obtains or guesses a valid claim identifier may be able to retrieve claimant information without authentication.

That creates a potential:

```text
PII disclosure
```

risk.

---

# 43. Dzoka Decision

Explicitly reject this pattern.

Claim detail endpoints must require authorization based on:

```text
claimant
authorized representative
assigned reviewer
organization
tenant
federation policy
```

---

# 44. Claim Enumeration Protection

Dzoka should also ensure public errors do not reveal:

```text
whether a claim exists
who made it
what evidence exists
```

to unauthorized users.

---

# 45. Administrative Claim Review

The admin router is protected globally by:

```text
authMiddleware
+
requireAdmin
```

and accepts:

```text
admin
delegated_admin
```

roles.

This is a sound baseline.

---

# 46. Admin Claim Queue

Administrators can query claims and inspect:

```text
claim
item
claimant profile
status
```

for review.

This maps well to Dzoka's human-review queue.

---

# 47. Admin Decision Endpoint

Administrators can:

```text
approve
reject
```

claims through:

```text
PUT /admin/claims/:id/decision
```

with required remarks.

This is a good explicit decision workflow.

---

# 48. Decision Audit

Approval/rejection creates:

```text
AuditLog
```

with action:

```text
claim_approved
claim_rejected
```

and reviewer metadata.

This is a strong reusable concept.

---

# 49. Approval Notification

The system notifies the claimant after a decision.

This provides a complete operational path:

```text
claim
    ↓
review
    ↓
decision
    ↓
audit
    ↓
notification
```

---

# 50. Major Boundary Problem

When a claim is approved, the upstream implementation immediately updates the item to:

```text
resolved
```

Conceptually:

```text
claim approved
    ↓
item resolved
```

This collapses several real-world states.

---

# 51. Why This Is Wrong for Dzoka

An approved ownership claim does not mean:

```text
item physically returned
```

The item may still be:

```text
in institution
waiting for pickup
with courier
under hold
```

---

# 52. Dzoka Correction

Dzoka requires:

```text
Claim APPROVED
    ↓
RecoveryAuthorization
    ↓
Custody/Handover
    ↓
RecoveryCompleted
```

Only actual completed recovery should mark the case/item recovered.

---

# 53. AI Match Schema

The repository defines:

```text
AiMatch
```

with:

```text
lostItemId
foundItemId
similarityScore
featureBreakdown
status
generatedAt
```

---

# 54. Feature Breakdown

The intended match features are:

```text
text
image
location
time
```

This is a good explainability baseline.

---

# 55. AI Configuration

The repository defines versioned:

```text
AiConfiguration
```

with:

```text
version
thresholds
weights
enabled
updatedBy
updatedAt
```

This is a valuable architectural pattern.

---

# 56. Config Versioning

Keeping configuration revisions as separate records instead of silently editing weights is good.

Dzoka strongly adopts this principle.

Every matching decision must be attributable to:

```text
algorithm version
model versions
configuration version
```

---

# 57. AI Decision Versions

The repository also defines:

```text
AiDecisionVersion
```

for previous states and rollback history.

This demonstrates useful concern for explainability and administrative changes.

---

# 58. AI Pipeline

`embeddingService.ts` describes a three-tier pipeline:

```text
YOLO detection
    ↓
OpenCLIP
    ↓
TF-IDF
```

and then saves results into item metadata.

---

# 59. YOLO Architecture

The service invokes several object-detection models.

Examples include specialist models for:

```text
personal items
electronics
computer equipment
stationery
```

---

# 60. Multi-Detector Aggregation

Several detectors are run concurrently.

Their detections are combined and then deduplicated using:

```text
IoU
+
label similarity
```

This is an interesting architectural concept.

---

# 61. Dzoka Lesson

Different item categories may benefit from specialist perception providers.

Dzoka should allow:

```text
DetectionProvider
```

or model routing rather than requiring one detector to handle every domain.

---

# 62. Detection Deduplication

The upstream system sorts by confidence and removes overlapping detections when:

```text
IoU > threshold
AND
labels considered similar
```

This is a reasonable ensemble post-processing technique.

---

# 63. Label Synonyms

The service recognizes simple synonym groups such as:

```text
phone
mobile_phone
smartphone

laptop
notebook
computer

bag
handbag
backpack
```

This is useful but should become taxonomy-driven rather than hard-coded in Dzoka.

---

# 64. OpenCLIP

The service calls a HuggingFace-hosted OpenCLIP model for image embeddings.

This gives the project real multimodal visual capability.

---

# 65. Model Provider Abstraction Gap

The provider calls are embedded directly into application service code.

Dzoka instead requires:

```text
ImageEmbeddingProvider
DetectionProvider
```

interfaces so model/provider changes do not change business logic.

---

# 66. Critical Security Finding — Embedded Provider Credential

`embeddingService.ts` contains a fallback Roboflow credential directly in source.

Conceptually:

```text
process.env.ROBOFLOW_API_KEY
||
hard-coded credential
```

This is a serious secret-management anti-pattern.

---

# 67. Dzoka Decision

Never copy embedded credentials from the source.

Dzoka must use:

```text
secret manager
environment injection
workload identity
```

as appropriate.

Production startup should fail where a required provider credential is missing.

---

# 68. Credential Incident Rule

Any apparent committed credential should be assumed compromised.

The correct response is:

```text
revoke
rotate
remove from source
remove from history where possible
```

not merely:

```text
move same key to .env
```

---

# 69. HuggingFace Configuration

The HuggingFace credential correctly defaults to:

```text
empty
```

rather than a committed fallback secret.

That is a better pattern.

---

# 70. TF-IDF Implementation

The repository includes a custom TF-IDF-style textual similarity function.

It:

```text
normalizes text
removes stop words
computes term frequency
uses manually assigned IDF values
normalizes vector
```

---

# 71. Important Qualification

This is not conventional corpus-derived TF-IDF.

Its `calculateIDF()` uses manually assigned values such as:

```text
common term → low weight
distinctive term → high weight
other term → medium weight
```

---

# 72. Dzoka Interpretation

This is better described as:

```text
heuristic weighted bag-of-words
```

than a statistically trained TF-IDF system.

It is useful for a prototype but should not be treated as sophisticated semantic retrieval.

---

# 73. Text Vector Consistency Concern

The embedding function creates vocabulary from each description independently.

That means two stored dense vectors may not necessarily represent identical term dimensions in the same order.

---

# 74. Consequence

Direct cosine comparison between independently generated vectors is unsafe unless their vocabulary bases are aligned.

The project also implements direct pairwise TF-IDF comparison separately, which correctly builds a common term set.

---

# 75. Dzoka Decision

Do not store ad-hoc TF-IDF arrays without:

```text
shared vocabulary
vectorizer version
feature mapping
```

Dzoka's semantic text retrieval should primarily use:

```text
FTS
trigram
versioned text embeddings
```

with properly defined spaces.

---

# 76. In-Memory “FAISS” Index

The source defines a class named:

```text
FAISSIndex
```

but it is not actual FAISS.

It stores vectors in a JavaScript:

```text
Map
```

and performs:

```text
linear cosine scan
```

over all entries.

---

# 77. Important Audit Finding

This is a naming issue that could mislead architectural interpretation.

Conceptually:

```text
called FAISS
≠
uses FAISS
```

---

# 78. Dzoka Decision

Do not treat this repository as evidence of scalable FAISS indexing.

Dzoka's vector retrieval remains:

```text
pgvector by default
Qdrant optional
```

behind `VectorSearchProvider`.

---

# 79. Image Pair Matching

`findImagePairs.ts` matches lost items to found items using:

```text
OpenCLIP
TF-IDF vector
YOLO class overlap
```

---

# 80. Pair Score

The inspected pairing function uses approximately:

```text
image embedding = 50%
text = 30%
class/object = 20%
```

in one code path.

---

# 81. Hard Category Gate

Candidates are queried with:

```text
found.category
=
lost.category
```

This is risky.

---

# 82. Dzoka Rejection

Do not allow a potentially incorrect category classification to permanently eliminate the correct candidate.

Category should normally be:

```text
retrieval preference
+
feature
```

not an unconditional gate.

---

# 83. Full Collection Scan

The matching service loads all eligible found items within the same category and compares them in application code.

This is workable for small campus datasets.

It is not the architecture Dzoka should use for large federated deployments.

---

# 84. Location Scoring

The broader embedding service implements location similarity using Haversine distance and discrete score bands.

Example logic roughly follows:

```text
< 100m
→ very strong

< 500m
→ strong

< 1km
→ medium

< 5km
→ weak

otherwise
→ zero
```

---

# 85. Good Location Principle

Physical distance should influence match plausibility.

This is a valid idea.

---

# 86. Limitation

Static distance bands do not understand:

```text
airport movement
bus travel
train movement
courier transfer
institutional routing
```

Dzoka therefore extends this through recovery graph and movement intelligence.

---

# 87. Time Scoring

The source models chronology explicitly.

It checks that a found item should generally not have been found before the corresponding loss.

This is useful.

---

# 88. Time Decay

The system gives stronger scores to:

```text
same day
within 3 days
within week
within month
```

and low but non-zero score for older events.

This is better than one absolute time cutoff.

---

# 89. Dzoka Extension

Dzoka preserves:

```text
chronology constraints
+
time-window evidence
```

but uses richer temporal precision and movement context.

---

# 90. Missing Evidence Weakness

Several upstream similarity functions return:

```text
0
```

when vectors or data are missing.

For Dzoka this is unacceptable as a generic rule.

---

# 91. Dzoka Invariant

Again:

> **Missing evidence ≠ mismatch.**

Use:

```text
available = false
score = null
```

rather than silently making unavailable evidence negative.

---

# 92. AI Match Status

`AiMatch` supports:

```text
accepted
rejected
overridden
```

This is useful for evaluation and feedback.

---

# 93. Dzoka ML Feedback

Dzoka should retain:

```text
system recommendation
human decision
override
reason
```

as distinct records.

This is important training/evaluation data.

---

# 94. Fraud Detection Service

The repository includes one of the more ambitious fraud/competing-claim implementations seen in the direct platform audits.

It evaluates:

```text
proof similarity
proof quality
claim history
similar item claims
competing claimant scores
```

---

# 95. Proof Quality Heuristics

Proof quality considers factors such as:

```text
proof length
specific terminology
image/URL evidence
```

and keywords such as:

```text
serial
receipt
scratch
sticker
engraving
purchase
damage
```

---

# 96. Useful Insight

A claimant who says:

```text
"It's mine."
```

should not receive the same evidence quality as someone who provides:

```text
serial
old photo
hidden scratch
receipt
```

The general principle is valid.

---

# 97. Major Limitation

Keyword presence alone does not prove evidence quality.

A fraudster can type:

```text
"I have the receipt and know the serial."
```

without proving either.

Dzoka therefore models actual evidence objects rather than keyword scores.

---

# 98. Claim History

The fraud service analyzes historical claim behavior.

Potentially useful signals include:

```text
previous rejections
many claims
same-category claims
```

This maps well to Dzoka's fraud architecture.

---

# 99. Important Fairness Requirement

Historical behaviour must never become:

```text
user had past rejected claim
→ automatically fraudulent
```

Signals require:

```text
explainability
policy
appealability
calibration
```

---

# 100. Claim Confidence

The upstream service combines:

```text
proof similarity
proof quality
history
```

into one final score.

This is useful as a risk heuristic.

It must not become ownership truth.

---

# 101. Competing Claim Evaluation

The service ranks multiple claims on the same item and compares:

```text
top claimant
runner-up
```

It checks:

```text
minimum winner score
minimum winner gap
critical fraud flags
```

---

# 102. Valuable Concept

The repository correctly recognizes that multiple claims require comparative evaluation rather than evaluating each claim completely in isolation.

Dzoka adapts that concept.

---

# 103. Dangerous Auto-Award Logic

The fraud service is explicitly designed to:

```text
award best claim
```

automatically when score/gap thresholds permit.

This is too strong for Dzoka.

---

# 104. Dzoka Rejection

Never implement:

```text
highest AI claim score
    ↓
automatic ownership
```

or:

```text
highest AI claim score
    ↓
automatic item release
```

AI may prioritize review.

It does not own the final authorization boundary.

---

# 105. Correct Dzoka Pattern

```text
FraudRiskAssessment
        +
VerificationEvidence
        +
Policy
        +
Human Review where required
        ↓
VerificationDecision
        ↓
separate RecoveryAuthorization
```

---

# 106. Dangerous Suspicion Treatment

The competing-claim processor can mark non-winning claims suspicious based partly on:

```text
distance from winner's score
```

This is risky.

---

# 107. Why Losing Does Not Equal Fraud

Suppose:

```text
Claim A = legitimate owner with limited evidence
Claim B = highly convincing fraudulent claimant
```

The fact that A scores below B does not make A suspicious.

Likewise several family members might legitimately claim the same item.

---

# 108. Dzoka Principle

```text
non-winning claimant
≠
fraudulent claimant
```

Fraud needs independent indicators.

---

# 109. Schema-Service Inconsistency

The claim schema declares statuses:

```text
pending
approved
rejected
withdrawn
conflict
```

but the fraud service writes:

```text
suspicious
```

---

# 110. Why This Matters

This creates divergence between:

```text
declared domain state machine
```

and:

```text
service behavior
```

Depending on update validation behavior, this can lead to inconsistent state or hidden runtime problems.

---

# 111. Dzoka Rule

Every workflow state must belong to one explicit state machine.

Unknown state transitions must fail.

---

# 112. Fraud Risk Schema Drift

The fraud service also writes:

```text
fraudRisk
```

into claims, but the inspected claim schema does not formally define that object.

This is another domain/schema mismatch.

---

# 113. Dzoka Improvement

Risk should be a separate aggregate/reference:

```text
RiskAssessment
```

not arbitrary additional fields injected into claims.

---

# 114. AI Service Integration Status

The README explicitly states that:

```text
AI Matching
Fraud Detection
```

services exist but still require API/frontend integration.

This is an important qualification.

---

# 115. Claim Route Does Not Invoke Fraud Service

The active claim-submission route creates:

```text
proofScore = 0
confidenceTier = low
```

and saves the claim.

It does not invoke the fraud-analysis workflow in that route.

---

# 116. Consequence

The repository contains:

```text
fraud architecture/code
```

but the complete end-to-end claim path is not fully wired.

Dzoka should not describe this source as having a fully integrated fraud claim pipeline.

---

# 117. Similar AI Integration Gap

The README also states that AI matching backend services exist while route/frontend exposure is pending.

Again:

```text
implemented service
≠
fully integrated user workflow
```

---

# 118. Audit Value

This distinction is precisely why Dzoka's source audit records:

```text
IMPLEMENTED
PARTIAL
PLANNED
```

rather than trusting feature tables alone.

---

# 119. Admin AI Configuration

Admins can inspect and update AI configuration.

A new configuration version is created, and older configurations are disabled.

This is a strong operational pattern.

---

# 120. Audit of Config Changes

Configuration changes create audit records containing:

```text
version
thresholds
weights
actor
timestamp
```

This is excellent provenance.

---

# 121. Important Configuration Weakness

The stored configuration supports weights for:

```text
text
image
location
time
```

while some matching code paths use separate hard-coded weight sets.

This creates possible configuration drift.

---

# 122. Dzoka Rule

A matching run must explicitly persist:

```text
the exact effective configuration
```

used.

No hidden second weight set should exist in service code.

---

# 123. “AutoApprove” Naming Risk

The AI configuration uses a threshold called:

```text
autoApprove
```

This is too ambiguous for a real recovery platform.

---

# 124. Dzoka Naming

Prefer terms such as:

```text
highConfidenceCandidateThreshold
```

if the threshold only concerns match ranking.

Never imply that AI match confidence automatically approves:

```text
ownership
claim
recovery
```

---

# 125. Audit Logging

The repository has a dedicated:

```text
AuditLog
```

model.

Fields include:

```text
actorId
action
targetEntity
targetId
metadata
timestamp
```

This is a good baseline.

---

# 126. Dzoka Audit Extension

Dzoka additionally needs:

```text
tenantId
organizationId
correlationId
request context
security context
before/after where permitted
data classification
```

for sensitive operations.

---

# 127. Data Retention Policy

The repository defines:

```text
DataRetentionPolicy
```

with:

```text
entity
retentionDays
action
```

and supports:

```text
delete
anonymize
```

This is a strong architectural concept.

---

# 128. Dzoka Extension

Dzoka's retention model also needs:

```text
legal hold
data class
jurisdiction
media derivatives
AI artifacts
audit retention
federated replicas
```

---

# 129. Authentication Architecture

The README and source architecture include:

```text
JWT access tokens
refresh tokens
session tracking
Argon2 passwords
OTP
```

which is stronger than many small open-source lost-and-found systems.

---

# 130. Dzoka Identity Decision

Dzoka should still use:

```text
OIDC-compatible identity
```

rather than copying bespoke authentication wholesale.

A Keycloak-compatible approach remains preferable for multi-tenant institutional deployment.

---

# 131. Roles

The upstream application supports:

```text
student
faculty
visitor
admin
delegated_admin
```

This is useful for the campus domain.

---

# 132. Dzoka Authorization Extension

Dzoka requires more general:

```text
RBAC
+
resource scope
+
tenant
+
organization unit
+
case assignment
+
policy
```

rather than application-specific fixed roles alone.

---

# 133. Notifications

The application includes:

```text
in-app
email
SMS
```

notifications.

This is useful operationally.

---

# 134. Dzoka Extension

Dzoka additionally treats:

```text
WhatsApp
```

as a first-class experience channel rather than simply another notification provider.

---

# 135. Queue Architecture

The project uses:

```text
Bull
+
Redis
```

for background work.

This is appropriate for its Node.js application.

---

# 136. Dzoka Difference

Dzoka's broader distributed architecture requires:

```text
Kafka-compatible integration events
```

for durable cross-container workflows.

Redis remains useful for:

```text
cache
OTP
locks
temporary sessions
```

---

# 137. Cloudinary

The application uses Cloudinary for uploaded media.

This is convenient for SaaS.

Dzoka requires a storage abstraction so self-hosted deployments can use:

```text
MinIO
S3
```

without a mandatory cloud-media vendor.

---

# 138. AI Provider Failure

The HuggingFace and Roboflow functions often handle provider errors by returning:

```text
[]
```

rather than failing the whole service.

This can be useful graceful degradation.

---

# 139. But Empty Results Need Provenance

A downstream system must distinguish:

```text
model saw no object
```

from:

```text
provider failed
```

Those are not equivalent.

---

# 140. Dzoka Rule

Derived evidence should have statuses such as:

```text
SUCCESS
NO_RESULT
FAILED
TIMED_OUT
INCOMPATIBLE
```

rather than collapsing all outcomes into an empty array.

---

# 141. Model Provenance Gap

The item AI metadata can hold:

```text
detectedObjects
primaryClass
embedding
textEmbedding
```

but the inspected item schema does not strongly encode:

```text
model provider
model version
preprocessing version
generatedAt
input hash
```

for each derived artifact.

---

# 142. Dzoka Correction

Every embedding or AI feature must be version-aware.

```text
EmbeddingRecord
├── provider
├── model
├── version
├── dimensions
├── inputHash
└── status
```

---

# 143. Embeddings Inside Item Documents

The service writes vectors into:

```text
item.aiMetadata.embedding
```

This tightly couples large derived AI artifacts to the domain item document.

---

# 144. Dzoka Decision

Store embeddings separately from canonical item state.

Reasons:

```text
multiple models
re-embedding
retention
indexing
migration
scale
provenance
```

---

# 145. Multiple Model Coexistence

The upstream item model effectively assumes one active embedding representation.

Dzoka must support:

```text
model A
model B
old version
new version
```

coexisting during evaluation/migration.

---

# 146. Item Fingerprint Gap

The upstream project has useful ingredients:

```text
attributes
image embedding
detected objects
text representation
location
time
```

but no complete versioned fingerprint abstraction.

---

# 147. Dzoka ItemFingerprint

Dzoka combines:

```text
structured attributes
semantic text
OCR
global vision
local vision
distinctive marks
protected identifiers
location
time
provenance
quality
model metadata
```

into a versioned fingerprint.

---

# 148. Hard Identity Conflict Gap

The inspected AI matching architecture does not provide the same explicit conflict system Dzoka requires.

Example:

```text
verified serial A
vs
verified serial B
```

must be able to invalidate a candidate independently of visual similarity.

---

# 149. Evidence Coverage Gap

The project does not provide a robust separation between:

```text
similarity score
```

and:

```text
how much evidence was actually available
```

Dzoka therefore maintains:

```text
MatchScore
EvidenceCoverage
MatchConfidence
```

separately.

---

# 150. Confidence Tier

Claims have:

```text
full
partial
low
```

confidence tiers.

This is directionally useful.

---

# 151. Dzoka Difference

Confidence classes should be derived from explicit evidence/policy and calibrated data.

They should not simply reflect one opaque AI score.

---

# 152. Organization Context

The project's campus orientation demonstrates a valuable truth:

> Lost-property recovery often happens inside institutions, not only between individual users.

This is strategically important for Dzoka.

---

# 153. Institutional Dzoka Use Cases

The same architecture generalizes to:

```text
university
airport
bus operator
railway
hotel
mall
hospital
company
event venue
government office
```

---

# 154. Institutional Nodes

Dzoka extends the organization idea into independently deployable:

```text
Dzoka Node
```

with:

```text
local policy
local custody
local verification
federation
```

---

# 155. Testing

The project has significantly more test infrastructure than many direct lost-and-found repositories.

The repository includes:

```text
Vitest
Supertest
mongodb-memory-server
React Testing Library
Playwright
```

and E2E scenarios.

---

# 156. Claim UI Testing

The E2E suite covers administrative claim pages and claim-related UI behaviour.

That is a positive operational sign.

---

# 157. AI/Fraud Unit Test Gap

Code search did not identify dedicated tests directly targeting:

```text
embeddingService
findImagePairs
fraudDetectionService
```

at the pinned revision.

That is a significant gap because these are high-risk decision components.

---

# 158. Dzoka Requirement

High-risk subsystems need explicit tests for:

```text
duplicate claims
competing claims
false fraud signals
missing evidence
provider failure
model mismatch
configuration changes
hard identity conflicts
```

---

# 159. Positive Design — Admin Approval

Despite AI scoring, the integrated claim UI currently includes:

```text
admin review
```

which is much safer than fully trusting match scores.

Dzoka preserves human review as a core control.

---

# 160. Contradictory AI Path

However, the separate fraud service also contains:

```text
auto-award
```

logic.

This creates inconsistent authority boundaries in the repository.

---

# 161. Dzoka Lesson

One subsystem should not believe:

```text
AI can award
```

while another believes:

```text
admin decides
```

The authority model must be globally explicit.

---

# 162. Dzoka Authority Model

```text
AI
→ evidence / recommendation

Policy
→ requirements

Human
→ approval when required

RecoveryAuthorization
→ release permission
```

No ambiguity.

---

# 163. Positive Design — Claim Conflict Object

The explicit conflict model is worth carrying forward.

Dzoka's richer equivalent includes:

```text
ClaimConflict
├── claims[]
├── severity
├── type
├── status
├── reviewer
└── resolution
```

---

# 164. Positive Design — Administrative Remarks

Requiring remarks for claim decisions is valuable.

A decision should answer:

```text
who
what
why
when
```

not just:

```text
approved = true
```

---

# 165. Positive Design — Retention Awareness

The presence of archival and retention concepts is notable.

Lost-and-found systems accumulate:

```text
PII
images
claims
location
audit
```

so indefinite retention is undesirable.

---

# 166. Security Strengths

Useful security practices include:

```text
Argon2
JWT access/refresh model
session records
admin middleware
Zod validation
audit logs
OTP TTL
rate-limit architecture
role checks
```

---

# 167. Security Weaknesses

Important findings include:

```text
hard-coded Roboflow fallback credential

unauthenticated claim-detail endpoint

claimant email exposed by that endpoint

AI/fraud schema drift

provider failures collapsed into empty output

validation fail-open behaviour
```

These must not be inherited.

---

# 168. Operational Weakness — Partial AI Integration

AI matching and fraud services are described by upstream itself as not completely connected to the product workflow.

Therefore this repository should not be treated as a fully production-integrated AI recovery platform.

---

# 169. Match Workflow Gap

The repository does not implement Dzoka's complete:

```text
candidate generation
    ↓
match evidence
    ↓
claim
    ↓
private verification
    ↓
risk
    ↓
recovery authorization
```

chain.

---

# 170. Custody Gap

There is no production-grade physical chain-of-custody architecture comparable to Dzoka's:

```text
CustodyRecord
CustodyEvent
CustodyTransfer
CustodyHold
CustodyIncident
```

---

# 171. Handover Gap

README references handover letters/PDF generation, but that is not equivalent to cryptographically controlled physical handover.

Dzoka requires:

```text
handover credential
transfer evidence
custody transition
recipient confirmation
```

---

# 172. Federation Gap

The project is institution/campus-oriented but not federated.

It lacks:

```text
node identity
peer trust
signed requests
cross-node search
remote verification
federated custody
```

---

# 173. CCTV Gap

The application does not provide Dzoka's edge/CCTV architecture.

That capability remains sourced from separate audits.

---

# 174. Movement Intelligence Gap

Its GeoJSON model is useful for current location, but it does not model:

```text
probable next recovery location
```

based on institutional movement.

Dzoka builds this separately.

---

# 175. What Dzoka Should Adopt

Because the repository is MIT, selected implementation patterns can legally be reused where technically appropriate.

High-value candidates include:

```text
GeoJSON Point modeling
zone polygon modeling
2dsphere indexing
audit record structure
configuration versioning concepts
claim conflict structure
admin decision flow
retention-policy concepts
```

Code reuse should still be evaluated against Dzoka's Java/Python architecture.

---

# 176. What Dzoka Should Adapt

Dzoka should adapt:

```text
institutional item submissions

authorization evidence

claim lifecycle

claim conflict detection

approval chains

admin review

fraud signal architecture

AI configuration history

geospatial zones

draft reporting

audit logging

retention policy
```

---

# 177. What Dzoka Should Reference

Use as conceptual reference for:

```text
multi-detector orchestration

object-detection ensemble

TF-IDF prototype

OpenCLIP provider calls

campus-specific workflows

notification orchestration
```

---

# 178. What Dzoka Must Reject

Explicitly reject:

```text
embedded API credentials

public claim PII endpoint

AI auto-award of ownership

losing claim = suspicious

claim approval = item recovered

undeclared workflow statuses

fraud fields outside domain schema

category hard gating

per-item ad-hoc TF-IDF vector spaces

fake/in-memory "FAISS" naming

missing signal = zero mismatch

provider error = no detection

validation error = automatically valid
```

---

# 179. Comparison with globalenglish01/lostfound

`globalenglish01/lostfound` remains stronger for:

```text
matching semantics
hard conflicts
evidence coverage
missing evidence
model versioning
retrieval architecture
```

This repository is stronger in:

```text
claim workflows
organizational operations
admin workflows
roles
fraud concepts
GeoJSON
audit
retention
```

---

# 180. Comparison with FuricBond/Lost-And-Found

FuricBond is stronger as a compact visual-instance matching example:

```text
YOLO crop
CLIP
pHash
ORB
```

This repository is stronger in:

```text
organizational workflow
claims
risk
admin
audit
GeoJSON zones
```

---

# 181. Dzoka Synthesis

The architectural synthesis becomes:

```text
globalenglish
    → evidence/matching discipline

FuricBond
    → visual multi-signal ideas

Lost-and-Found-Tracking-System
    → institutional workflow
    → claims
    → fraud signals
    → geospatial zones
    → admin/audit
```

combined with specialist retrieval, federation, traceability, and CCTV sources.

---

# 182. Mapping to Dzoka Domains

| Upstream | Dzoka |
|---|---|
| `items` | `ItemRecord`, `LostReport`, `FoundReport` |
| `CampusZone` | `Location`, `LocationHierarchy`, spatial boundaries |
| `organizationId` | `Organization` / `OrganizationUnit` |
| `claims` | `Claim` |
| `claim_conflicts` | `ClaimConflict` |
| `claim_decisions` | `ClaimReview` / `VerificationDecision` |
| `ai_matches` | `MatchCandidate` / `MatchingRun` |
| `ai_configurations` | versioned `MatchingProfile` |
| fraud service | `RiskAssessment` |
| `audit_logs` | `AuditEvent` |
| retention policies | Dzoka data-governance policy |

---

# 183. Mapping — Claim Flow

Upstream:

```text
submit claim
    ↓
pending
    ↓
admin review
    ↓
approved / rejected
    ↓
item resolved
```

Dzoka:

```text
submit claim
    ↓
eligibility
    ↓
verification
    ↓
fraud/risk
    ↓
conflict check
    ↓
entitlement
    ↓
human/policy review
    ↓
claim approved
    ↓
recovery authorization
    ↓
custody transfer
    ↓
recovery completed
```

---

# 184. Mapping — Fraud

Upstream:

```text
proof similarity
+
proof quality
+
history
+
competing claims
    ↓
claim score
```

Dzoka:

```text
independent fraud signals
+
verification evidence
+
claim history
+
device/account abuse
+
document reuse
+
competing claims
    ↓
RiskAssessment
```

Risk remains separate from ownership evidence.

---

# 185. Mapping — AI Matching

Upstream:

```text
YOLO
+
OpenCLIP
+
TF-IDF
+
location
+
time
```

Dzoka:

```text
structured attributes
+
FTS / trigram
+
semantic text
+
global visual
+
local visual
+
OCR
+
protected identifiers
+
location
+
time
+
hard conflicts
+
coverage
+
confidence calibration
```

---

# 186. Influence on Claim Workflow

This source directly supports Dzoka's decisions to include:

```text
Claim
ClaimConflict
ClaimReview
ClaimDecision history
withdrawal
admin review
audit
```

---

# 187. Influence on Fraud Architecture

The source demonstrates the usefulness of:

```text
claim history
proof-quality assessment
repeated claims
competing claimant evaluation
```

but also demonstrates why fraud must not become:

```text
automatic ownership selection
```

---

# 188. Influence on Location Architecture

The GeoJSON model strongly reinforces:

```text
structured location
+
institutional zones
+
spatial indexes
```

as first-class data.

Dzoka implements the equivalent using PostgreSQL/PostGIS.

---

# 189. Influence on Multi-Tenancy

The upstream `organizationId` field demonstrates a useful institutional requirement, but it is not sufficient multi-tenancy architecture.

Dzoka must enforce tenant boundaries in:

```text
queries
events
vectors
media
cache
audit
federation
```

---

# 190. Influence on Audit Architecture

This repository is strong evidence that actions such as:

```text
claim submit
claim withdraw
claim approve
item approve
role change
AI config update
```

should produce durable audit records.

Dzoka adopts that principle.

---

# 191. Influence on Data Governance

The upstream retention-policy model is an important architectural reminder that:

```text
operational data
≠
keep forever
```

Dzoka therefore formalizes:

```text
retention classes
legal holds
anonymization
deletion
archive
```

---

# 192. Influence on AI Governance

Versioned AI configuration plus audit records informs Dzoka's:

```text
MatchingConfigurationVersion
ModelRun
EvaluationRun
PolicyVersion
```

approach.

---

# 193. Architecture Documents Influenced

This source should be referenced by:

```text
ARCHITECTURE.md
DESIGN.md

docs/architecture/data-architecture.md
docs/architecture/security-architecture.md
docs/architecture/privacy-architecture.md
docs/architecture/multi-tenancy.md
docs/architecture/ai-architecture.md

docs/design/matching-engine.md
docs/design/ownership-verification.md
docs/design/claim-workflow.md
docs/design/custody-state-machine.md
```

---

# 194. ADRs Potentially Influenced

Possible ADRs include:

```text
use PostGIS for spatial location modeling

separate claims from recovery

separate risk from verification

version matching configuration

use explicit audit log

use explicit data-retention policy

organization-aware reports
```

---

# 195. Recommended Lock Entry

Add to:

```text
docs/source-audit/source-audit.lock.json
```

```json
{
  "id": "lost-and-found-tracking-system",
  "category": "platforms",
  "repository": "https://github.com/Lost-and-Found-Tracking-System/lost-and-found-system",
  "revision": "4ef1493f80590f6fa03d2ad9eded2e70a59ebe58",
  "license": "MIT",
  "classification": "ADAPT",
  "secondaryClassification": "REFERENCE",
  "audit": "docs/source-audit/platforms/lost-and-found-tracking-system.md",
  "auditedAt": "2026-10-04"
}
```

---

# 196. Re-Audit Triggers

Re-audit if upstream materially changes:

```text
AI matching integration

fraud-service integration

claim schema

claim authorization

Roboflow credential handling

ownership verification

physical handover

custody

federation

matching storage/indexing

AI model provenance
```

Particular attention should be paid if the current partial AI/fraud services become fully wired into claim decisions.

---

# 197. Security Findings Summary

## Positive

```text
MIT license

Argon2 passwords

JWT + sessions

admin middleware

audit logging

validation

OTP architecture

roles
```

## Negative

```text
hard-coded provider credential fallback

claim-detail route missing auth

claimant email exposed by that route

schema/service status mismatch

undeclared fraudRisk fields

permissive validation fallback
```

---

# 198. AI Findings Summary

## Real capabilities

```text
YOLO model calls
OpenCLIP API embedding
text similarity
location scoring
time scoring
feature breakdown
configurable AI configuration
```

## Weaknesses

```text
partial product integration

hard-coded candidate rules

ad-hoc TF-IDF vocabulary

application-side scans

fake "FAISS" naming

limited model provenance

missing evidence treated as zero

multiple inconsistent weight systems
```

---

# 199. Claims Findings Summary

## Strong

```text
Claim aggregate
withdrawal
claim conflicts
claim decisions
approval chain
admin review
audit
```

## Weak

```text
public claim-detail endpoint

AI auto-award logic

winner-relative suspicion

approval immediately resolves item

fraud schema drift
```

---

# 200. Final Assessment

`Lost-and-Found-Tracking-System/lost-and-found-system` is valuable not because it provides the best matching algorithm, but because it attempts to model a **real institutional lost-property operation**.

Its strongest architectural chain is:

```text
USER / ORGANIZATION
        ↓
ITEM REPORT
        ↓
GEOJSON LOCATION
        ↓
ADMIN REVIEW
        ↓
AI MATCHING
        ↓
OWNERSHIP CLAIM
        ↓
CLAIM CONFLICT
        ↓
FRAUD / RISK ANALYSIS
        ↓
ADMIN DECISION
        ↓
AUDIT / NOTIFICATION
```

That is much closer to Dzoka's operational domain than a simple lost-item marketplace.

However, several trust boundaries remain too weak.

Dzoka must transform the upstream approach into:

```text
REPORT
    ↓
ITEM FINGERPRINT
    ↓
MATCH CANDIDATES
    ↓
CLAIM
    ↓
PRIVATE OWNERSHIP VERIFICATION
    ↓
FRAUD / RISK ASSESSMENT
    ↓
COMPETING CLAIM RESOLUTION
    ↓
ENTITLEMENT
    ↓
CLAIM APPROVAL
    ↓
RECOVERY AUTHORIZATION
    ↓
CUSTODY TRANSFER
    ↓
RETURN CONFIRMATION
```

---

# 201. Final Decision

```text
PRIMARY CLASSIFICATION:
ADAPT WORKFLOW CONCEPTS
```

### Strong concepts to adapt

```text
institutional submissions

GeoJSON locations

zone polygons

claim lifecycle

claim conflicts

claim decisions

approval chains

admin review

audit logs

data retention

AI configuration versioning

claim-history fraud signals
```

### AI concepts to reference

```text
multi-model detection
OpenCLIP
text similarity
location/time feature breakdown
competing-claim analysis
```

### Explicitly reject

```text
hard-coded provider secrets

public claim PII lookup

AI auto-award

losing claimant = suspicious

claim approval = physical recovery

schema/state drift

unversioned embeddings

category hard filtering

missing evidence = zero

fake FAISS abstraction

provider failure = negative evidence
```

The central lesson for Dzoka is:

> **This repository is a strong source for institutional workflows, claims, geospatial organization, fraud signals, and auditing—but Dzoka must impose much stricter boundaries between matching, ownership verification, risk, authorization, custody, and physical recovery.**