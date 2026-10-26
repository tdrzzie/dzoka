# Source Audit — dhanushshetty40/AI-lost-and-found (SmartFind)

> **Dzoka — The Open Recovery Network**
>
> Source audit of SmartFind, an AI-labelled campus lost-and-found platform, focusing on matching, image similarity, ownership claims, verification, QR recovery, lifecycle tracking, spatial intelligence, model benchmarking, security, privacy, and recovery semantics.

---

# 1. Audit Metadata

| Field | Value |
|---|---|
| Repository | `https://github.com/dhanushshetty40/AI-lost-and-found` |
| Upstream owner | `dhanushshetty40` |
| Product name | SmartFind |
| Audited revision | `577edc3b492f62ac8707fc950230bc04a354e9d8` |
| Short revision | `577edc3b` |
| Revision date | 2026-08-30 |
| Audit date | 2026-10-04 |
| Default branch | `main` |
| Backend | Python + FastAPI |
| ORM | SQLAlchemy |
| Database | SQLite |
| Frontend | React + Vite |
| Authentication | JWT + bcrypt |
| Claimed license | MIT |
| License verification | **UNRESOLVED — README claims MIT but root `LICENSE` file is absent** |
| Primary classification | **REFERENCE ONLY** |
| Direct code reuse | **DO NOT COPY until licensing is clarified** |

---

# 2. Executive Decision

SmartFind should **not** be used as a Dzoka AI or matching foundation.

Its documentation claims:

```text
Sentence Transformers
ResNet-50
CLIP
visual embeddings
HSV histograms
SSIM
multimodal AI
cryptographic verification
verifiable QR codes
empirical benchmarks
```

but the active source does not implement most of those capabilities.

The actual matching implementation is principally:

```text
TF-IDF when available
        OR
difflib/string similarity
+
category
+
brand
+
color
+
location-string similarity
+
date decay
+
synthetic "image score"
```

Most critically:

> **The image matching function does not inspect image pixels at all.**

The repository is still useful as a reference for:

```text
Report
→ Match
→ Claim
→ Review
→ Handover
→ Lifecycle
```

and for some campus-oriented product ideas.

Classification:

```text
PRIMARY:
REFERENCE ONLY
```

---

# 3. Most Important Finding

The function:

```python
calculate_image_similarity(...)
```

does not perform image similarity.

It does not load:

```text
image bytes
pixels
histograms
embeddings
CNN features
CLIP vectors
local features
```

Instead, if both image URLs exist but differ, it computes a score derived from:

```text
text similarity
```

approximately:

```text
max(75, min(95, text_similarity + 10))
```

Therefore the advertised:

```text
Image Embedding Comparison
```

is not an image embedding comparison.

---

# 4. Repository Scope Inspected

Primary paths inspected:

```text
README.md

backend/
├── .env
├── requirements.txt
├── smartfind.db
├── test_phase1.py
├── test_phase2.py
├── test_phase3.py
├── test_phase4.py
├── test_phase5.py
├── test_phase6.py
├── test_phase7.py
│
└── app/
    ├── config.py
    ├── database.py
    ├── deps.py
    ├── main.py
    ├── security.py
    │
    ├── models/
    │   ├── report.py
    │   ├── match.py
    │   └── user.py
    │
    ├── routers/
    │   ├── reports.py
    │   ├── matches.py
    │   ├── claims.py
    │   ├── lifecycle.py
    │   ├── analytics.py
    │   └── experiments.py
    │
    ├── schemas/
    │   ├── report.py
    │   ├── match.py
    │   ├── claim.py
    │   └── qr_lifecycle.py
    │
    └── services/
        ├── ai_matcher.py
        ├── campus_spatial.py
        ├── ml_experiment_service.py
        ├── qr_service.py
        ├── audit_service.py
        └── notification_service.py
```

Frontend structure and dependencies were also inspected at a high level.

---

# 5. Audit Limit

This is an architectural/source audit rather than:

- a penetration test;
- a legal opinion;
- a complete dependency vulnerability scan;
- an independent execution of all 49 claimed tests;
- an independent reproduction of claimed benchmark results.

---

# 6. Licensing Finding

The README prominently declares:

```text
MIT License
```

and includes an MIT badge.

It also says:

```text
see the LICENSE file
```

However, at the audited revision:

```text
LICENSE
```

does not exist at the repository root.

---

# 7. Dzoka Licensing Decision

Treat license status as:

```text
UNRESOLVED
```

until either:

1. an actual MIT license text is committed; or
2. the repository author explicitly clarifies the license.

Therefore:

```text
ADOPT SOURCE CODE:
NO
```

---

# 8. High-Level Architecture

SmartFind uses:

```text
React / Vite
      ↓
FastAPI
      ↓
SQLAlchemy
      ↓
SQLite
```

with application services for:

```text
matching
spatial resolution
QR generation
ML experiments
notifications
audit
```

---

# 9. Positive Architectural Separation

One useful property is that the repository separates:

```text
Report
AIMatch
Claim
Notification
AuditLog
```

into distinct persisted concepts.

That is directionally much better than systems where everything is stored on one item row.

---

# 10. Report Domain

A report contains:

```text
id
user_id
report_type
item_name
category
description
brand
color
distinctive_features
additional_info
location
incident_date
incident_time
image_url
status
timestamps
```

---

# 11. Report Type

The system distinguishes:

```text
LOST
FOUND
```

which aligns with Dzoka's principle that reports represent observations/perspectives.

---

# 12. Report Status

The model comments list statuses such as:

```text
ACTIVE
RESOLVED
MATCHED
CLAIMED
CLOSED
```

This begins to model lifecycle, but later code mixes these meanings incorrectly.

---

# 13. Matching Model

`AIMatch` stores:

```text
lost_report_id
found_report_id

overall_score
confidence_level

text_score
image_score
location_score
date_score
attribute_score

explanation
status
timestamps
```

This structure is one of the repository's stronger ideas.

---

# 14. Dzoka Lesson — Preserve Feature Breakdown

Dzoka should similarly persist:

```text
overall assessment
+
individual evidence contributions
```

rather than only one opaque score.

For example:

```text
text evidence
visual evidence
OCR evidence
local-instance evidence
identifier evidence
geo evidence
time evidence
attribute evidence
hard conflicts
coverage
```

---

# 15. But Score Breakdown Must Represent Real Evidence

SmartFind's structure is useful.

Its current values are not always trustworthy.

The key rule for Dzoka is:

> A named score must correspond to evidence actually observed for that modality.

---

# 16. Text Similarity

The matcher first tries:

```text
TF-IDF
+
cosine similarity
```

if scikit-learn imports successfully.

---

# 17. Dependency Mismatch

`backend/requirements.txt` does **not** list:

```text
scikit-learn
```

Therefore a clean installation from the supplied requirements may fall back to:

```text
difflib.SequenceMatcher
+
token overlap
```

instead.

---

# 18. Dzoka Lesson

An optional dependency silently changing the algorithm is dangerous when match scores affect decisions.

Model/runtime capability must be explicit.

---

# 19. Desired Dzoka Pattern

Record:

```text
matcher implementation
model/version
dependency/runtime version
configuration
```

with every matching run.

---

# 20. Attribute Similarity

SmartFind scores:

```text
category
brand
color
```

using manually assigned weights.

Attribute weights inside the attribute score are roughly:

```text
category 50%
brand    30%
color    20%
```

---

# 21. Missing Brand Is Treated as Evidence

When both brands are missing, the implementation assigns:

```text
70
```

for brand similarity.

When one brand is missing:

```text
50
```

is assigned.

---

# 22. Missing Color Is Also Treated as Evidence

When color evidence is missing:

```text
60
```

is assigned.

---

# 23. Dzoka Rejection

This violates one of Dzoka's fundamental rules:

> **Missing evidence ≠ matching evidence.**

Missing brand should not magically become:

```text
70% brand compatibility
```

---

# 24. Correct Dzoka Representation

Use:

```text
brand:
  available: false
  similarity: null
```

rather than:

```text
brand_similarity: 70
```

---

# 25. Location Similarity

The matcher's location score works mostly from:

```text
location strings
```

using:

```text
token overlap
SequenceMatcher
```

---

# 26. Missing Location Is Given 30

If a location is absent:

```text
location_score = 30
```

Again, unavailable evidence contributes positive score.

---

# 27. Dzoka Correction

Unknown location should be:

```text
available = false
```

not:

```text
weak match = 30
```

---

# 28. Date Similarity

Dates use exponential decay:

```text
100 × e^(-0.25 × days_difference)
```

with a floor of:

```text
5
```

---

# 29. Invalid Date Fallback

If date parsing fails, the system returns:

```text
50
```

rather than marking temporal evidence unavailable.

This again violates:

```text
missing evidence ≠ partial match
```

---

# 30. Image Similarity — Actual Implementation

The image function takes:

```text
img_url1
img_url2
text_sim
```

It never opens either image.

---

# 31. Same URL

If:

```text
img_url1 == img_url2
```

it assigns:

```text
100
```

---

# 32. Different Existing URLs

If both URLs exist but are different, it computes approximately:

```text
text_similarity + 10
```

clamped between:

```text
75
and
95
```

---

# 33. Major Consequence

Two entirely different photographs can receive at least:

```text
75%
```

image similarity merely because both reports contain an image URL.

---

# 34. No-Image Case

If neither report has an image:

```text
image_score = text_score × 0.85
```

---

# 35. One-Image Case

If only one side has an image:

```text
image_score = text_score × 0.75
```

---

# 36. Dzoka Rejection

This pattern is unacceptable for Dzoka.

An image feature score must never be fabricated from textual evidence.

---

# 37. Correct Dzoka Semantics

If visual comparison cannot run:

```text
visualEvidence.available = false
visualEvidence.score = null
```

The scoring engine should recompute using available evidence and evidence coverage.

---

# 38. False Explainability

When both reports contain images, SmartFind adds an explanation similar to:

```text
Image Embedding Comparison:
Visual features and color histograms evaluated.
```

But neither embeddings nor histograms are actually calculated by this function.

---

# 39. Critical Explainability Rule

Dzoka's explanation system must never claim an algorithm ran when it did not.

Explainability itself must be:

```text
evidence-backed
```

---

# 40. Claimed Multimodal Formula

SmartFind computes:

```text
30% text
25% image
20% attributes
15% location
10% date
```

---

# 41. Formula Is Structurally Useful

A decomposable fusion architecture is directionally useful.

However, because some component scores are synthetic, the overall score becomes misleading.

---

# 42. Dzoka Matching Improvement

Dzoka uses:

```text
candidate retrieval
        ↓
evidence extraction
        ↓
feature-level comparison
        ↓
hard conflicts
        ↓
availability-aware weighting
        ↓
heavy reranking
        ↓
evidence coverage
        ↓
confidence/calibration
        ↓
explanation
```

---

# 43. Confidence Levels

SmartFind maps:

```text
>=75 → HIGH
>=45 → MEDIUM
else → LOW
```

---

# 44. No Calibration

These thresholds are arbitrary application thresholds.

There is no demonstrated calibration mapping:

```text
score
→ actual probability of same physical item
```

---

# 45. Dzoka Rule

Do not describe an arbitrary similarity score as:

```text
probability
confidence of ownership
```

unless it is empirically calibrated for that meaning.

---

# 46. Candidate Generation

A report is compared against all:

```text
ACTIVE
```

reports of the opposite type.

This is simple exhaustive candidate comparison.

---

# 47. Scalability

For a small campus demo, this is manageable.

At larger scale, repeated:

```text
LOST × FOUND
```

comparisons become expensive.

---

# 48. Dzoka Difference

Dzoka performs:

```text
retrieval first
```

using indexes such as:

```text
pgvector
FTS
trigram
geo
attributes
```

before heavier reranking.

---

# 49. Match Persistence Threshold

SmartFind persists candidates when:

```text
overall_score >= 35
```

This is an application threshold rather than a calibrated retrieval threshold.

---

# 50. Match Updates

Existing match records are overwritten with new scores.

The old scoring state is not retained.

---

# 51. Dzoka Decision

Every significant matching execution should be represented by:

```text
MatchingRun
```

with immutable or reconstructable candidate evidence.

---

# 52. No Model Provenance

`AIMatch` does not persist:

```text
model version
algorithm version
weight configuration
code revision
embedding model
prompt version
feature extractor version
```

---

# 53. Dzoka Requirement

A score without provenance cannot be reliably reproduced.

---

# 54. Match Trigger Authorization Finding

README describes:

```text
POST /api/matches/run
Access: Admin
```

However the source requires only:

```text
get_current_user
```

not:

```text
get_current_admin_user
```

---

# 55. Consequence

Any authenticated user can trigger:

```text
system-wide matching
```

at the audited revision.

---

# 56. Dzoka Security Rule

Administrative or computationally expensive operations require explicit server-side authorization.

---

# 57. Match Confirmation Authorization — Critical

The endpoint:

```text
POST /api/matches/{id}/confirm
```

does not verify that the caller:

```text
owns either report
is an assigned reviewer
is an admin
```

---

# 58. Consequence

Any authenticated user who knows a match ID can potentially:

```text
CONFIRM
or
REJECT
```

that match.

---

# 59. Match Confirmation Changes Reports

Confirming a match also sets both associated reports to:

```text
MATCHED
```

Therefore this authorization weakness changes authoritative workflow state.

---

# 60. Dzoka Rejection

Never allow a generic authenticated user to mutate someone else's match decision.

---

# 61. Match Detail Authorization — Critical

`GET /api/matches/{match_id}` retrieves a match for any authenticated user.

No ownership/admin relationship check is performed.

---

# 62. Potential Exposure

The response contains both:

```text
lost_report
found_report
```

and their details.

This can expose information about reports unrelated to the caller.

---

# 63. Public Report Privacy — Critical

The report API is even more serious.

`ReportResponse` contains:

```text
OwnerInfo
├── id
├── full_name
├── email
└── phone
```

---

# 64. Public Report Endpoint

`GET /api/reports/{report_id}` does not require authentication.

Therefore public report responses can include the reporting user's:

```text
name
email
phone
```

---

# 65. List Endpoint

The report list API also permits unauthenticated access.

The same response schema includes owner information.

---

# 66. Documentation Contradiction

README claims:

```text
Privacy-Guarded Contact Release
```

and says contact data becomes available after successful verification.

The API representation undermines that claim by including owner contact details in public report responses.

---

# 67. Dzoka Rule

Never rely on:

```text
contact_released = false
```

while a different API projection exposes the same contact information.

---

# 68. Projection-Based Privacy

Dzoka needs separate DTOs/projections such as:

```text
PublicReportView
ReviewerReportView
OwnerReportView
RecoveryContactView
```

---

# 69. Ownership Claim Model

A claim stores:

```text
claimant
report
verification_question
verification_answer
proof_image
verification_score
status
contact_released
owner_notes
```

---

# 70. Positive Claim Concepts

Useful concepts include:

```text
separate Claim entity
one claimant
claim status
proof evidence
review
contact-release state
```

These are worth referencing.

---

# 71. Claim Self-Claim Protection

The source correctly blocks:

```text
report owner
→ claim own found report
```

---

# 72. Duplicate Claim Protection

An existing:

```text
PENDING
APPROVED
COMPLETED
```

claim blocks another active claim for the same user/report.

This is a reasonable baseline.

---

# 73. Claimed Hidden Verification

README describes:

```text
hidden security questions
```

and:

```text
cryptographic ownership verification
```

The actual claim implementation is very different.

---

# 74. Generated Verification Question

The source creates a generic question:

```text
Please describe any distinctive markings,
serial numbers, wallpapers,
or proof of ownership...
```

There is no corresponding secret expected answer model.

---

# 75. Actual Verification Target

The claimant's answer is compared against:

```text
report.item_name
report.description
report.distinctive_features
report.color
report.brand
```

---

# 76. This Is Not Private Knowledge Verification

Most or all of that information may already be present in the report itself.

Therefore a claimant may be rewarded for repeating public report information.

---

# 77. Dzoka Rejection

Ownership verification must not compare the claimant primarily against:

```text
public listing text
```

---

# 78. Correct Dzoka Model

Ownership verification should compare against:

```text
private evidence
protected identifiers
prior-owner evidence
non-public finder observations
receipts
historical media
institutional records
```

as policy permits.

---

# 79. Verification Algorithm

The score combines:

```text
SequenceMatcher
+
token overlap
```

against the report text.

---

# 80. Minimum Score Problem

The function returns at least:

```text
25
```

for any non-empty answer because of:

```text
max(25.0, score)
```

---

# 81. Consequence

A completely poor non-empty response still receives a positive ownership score.

---

# 82. Dzoka Decision

Do not impose artificial positive floors on evidence quality.

---

# 83. Verification Score Is Not Ownership Confidence

SmartFind exposes a percentage-like:

```text
verification_score
```

which can look authoritative.

It is simply text similarity against the report.

---

# 84. Dzoka Terminology

If such a metric exists, name it according to what it actually measures:

```text
textEvidenceSimilarity
```

not:

```text
ownershipConfidence
```

---

# 85. Claim Proof Upload

The platform supports:

```text
JPEG
PNG
WebP
PDF
```

ownership proof uploads.

That is a useful recovery feature.

---

# 86. Upload Validation Weakness

Proof upload validation checks:

```text
filename extension
```

rather than performing strong content/magic-byte validation.

---

# 87. Static Proof Exposure

The backend mounts:

```text
/uploads
```

through an unauthenticated static-file handler.

Claim proof documents are stored in that directory.

---

# 88. Consequence

If a proof URL becomes known or guessed/leaked, the content is served without claim-level authorization.

This is inappropriate for:

```text
receipts
identity evidence
ownership photos
documents
```

---

# 89. Dzoka Rule

SECRET/RESTRICTED evidence must never be stored behind a generic public static directory.

---

# 90. Correct Dzoka Proof Flow

Use:

```text
private object storage
        ↓
authorization
        ↓
short-lived signed access
        ↓
audit
```

---

# 91. Claim Review Authorization

`verify_claim()` correctly requires:

```text
report owner
OR
admin
```

to approve/reject a claim.

This is a positive authorization pattern.

---

# 92. Contact Release

Approval sets:

```text
contact_released = true
```

and the report status becomes:

```text
CLAIMED
```

Conceptually, separating approval from final recovery is good.

---

# 93. But Contact Was Already Public

Because public report responses can contain the reporter's email and phone, the contact-release flag does not create a reliable privacy boundary.

---

# 94. Claim Completion — Critical Authorization Bug

The endpoint:

```text
POST /api/claims/{claim_id}/complete
```

requires authentication.

However, it does **not** verify that the current user is:

```text
claimant
finder
report owner
admin
```

---

# 95. Consequence

Any authenticated user who knows an approved claim ID can potentially mark it:

```text
COMPLETED
```

and the report:

```text
RESOLVED
```

---

# 96. Reputation Side Effect

That endpoint also awards:

```text
+10 reputation
```

to the report owner/finder.

Therefore the authorization bug also affects incentive state.

---

# 97. Lifecycle Architecture

SmartFind exposes a seven-step lifecycle:

```text
REPORTED
MATCH_DETECTED
CLAIM_SUBMITTED
VERIFICATION
APPROVED
RETURNED
CLOSED
```

This is a useful product concept.

---

# 98. Dzoka Adoption

A visual recovery timeline is valuable.

However, the timeline should be a projection of real domain events—not inferred loosely from status flags.

---

# 99. Critical Lifecycle Semantic Bug

SmartFind defines:

```python
is_returned = report.status in ["RESOLVED", "CLAIMED", "CLOSED"]
```

Therefore:

```text
CLAIMED
```

is considered equivalent to:

```text
physical item returned
```

---

# 100. Consequence

Immediately after claim approval:

```text
report.status = CLAIMED
```

the lifecycle may display:

```text
Physical Item Handover Completed
Case Closed
```

before a physical handover actually happened.

---

# 101. Dzoka Invariant

These states must remain separate:

```text
CLAIM APPROVED
≠
RECOVERY AUTHORIZED
≠
CUSTODY TRANSFERRED
≠
RECOVERY COMPLETED
```

---

# 102. Handover OTP Claim

The lifecycle response advertises:

```text
handover_otp_enabled = true
```

and the handover request contains:

```text
handover_otp
```

---

# 103. Critical Finding — OTP Is Not Validated

`confirm_item_handover()` receives the request payload but does not validate:

```text
handover_otp
```

at all.

---

# 104. Test Confirms This Behavior

The lifecycle test sends an arbitrary OTP-like value and expects:

```text
200 OK
```

There is no corresponding OTP generation/verification step demonstrated in the audited implementation.

---

# 105. Dzoka Rejection

Never expose:

```text
handover OTP enabled
```

unless the protocol actually includes:

```text
generation
secure storage
expiry
attempt limits
verification
single use
audit
```

---

# 106. Handover Authorization Weakness

The lifecycle handover endpoint allows:

```text
report owner
first claim's claimant
admin
```

to confirm handover.

---

# 107. Approval Is Not Required

The code does not require the selected claim to be:

```text
APPROVED
```

before the claimant can qualify as a handover actor.

---

# 108. Consequence

A claimant associated with the selected claim may potentially resolve the report even if the claim has not gone through proper approval.

---

# 109. First-Claim Selection Problem

The lifecycle code effectively selects:

```text
claims[0]
```

as the active claim.

There is no robust policy for:

```text
multiple claimants
competing claims
approved claim selection
```

---

# 110. Dzoka Requirement

Recovery must be bound to an explicit:

```text
RecoveryAuthorization
```

referencing the specific:

```text
approved Claim
VerificationDecision
entitled recipient
```

---

# 111. Repeatable Handover Reward

The lifecycle endpoint does not first reject an already-resolved report.

It awards:

```text
+15 reputation
```

when called.

---

# 112. Idempotency Risk

Repeated valid calls could potentially award reputation repeatedly.

Recovery completion must be idempotent.

---

# 113. Two Competing Completion Workflows

SmartFind has at least two completion mechanisms:

```text
POST /claims/{id}/complete
```

and:

```text
POST /reports/{id}/handover-confirm
```

---

# 114. Different Reward Semantics

One awards approximately:

```text
+10
```

to the finder.

The other awards:

```text
+15
```

to participants.

This means recovery truth and reward behavior are duplicated across endpoints.

---

# 115. Dzoka Rule

There must be one authoritative recovery-completion command.

Other channels invoke that command rather than reimplementing recovery logic.

---

# 116. QR Architecture

SmartFind generates:

```text
item tag code
QR image
verification URL
```

for reports.

This is a useful product direction.

---

# 117. Claimed Cryptographic QR

README describes:

```text
cryptographic
verifiable QR codes
```

The actual QR scheme does not provide cryptographic verification.

---

# 118. Tag Code

Tag codes are deterministic:

```text
SF-2026-L0012
SF-2026-F0012
```

derived from:

```text
report ID
report type
fixed year prefix
```

---

# 119. No Signature

No:

```text
HMAC
digital signature
nonce
signed claim
cryptographic proof
```

is attached to the tag.

---

# 120. QR Payload

The service constructs a richer `qr_payload`, but the generated QR itself receives only:

```text
verification_url
```

through `qr.add_data()`.

---

# 121. Missing QR Dependencies

The service optionally imports:

```text
qrcode
PIL
```

but neither dependency is listed in the inspected:

```text
requirements.txt
```

---

# 122. Clean-Install Behavior

If those packages are absent:

```text
QR_AVAILABLE = False
```

and the application returns an SVG fallback.

---

# 123. Fallback Is Not a Real QR Code

The fallback SVG is a QR-styled graphic made from rectangles and text.

It does not encode the verification URL as a real QR symbol.

---

# 124. Dzoka Lesson

Graceful fallback must preserve functionality.

A decorative QR placeholder should not be presented as a functional QR code.

---

# 125. QR Identity Principle

The concept remains useful:

```text
physical tag
→ digital recovery route
```

but Dzoka should implement it using cryptographically strong opaque identifiers or signed tokens according to threat model.

---

# 126. QR Is Not Ownership

A scanned tag proves, at best:

```text
knowledge/possession of tag data
```

It does not prove:

```text
ownership
custody entitlement
```

---

# 127. Campus Spatial Intelligence

The repository contains a `campus_spatial` service with nine predefined campus locations.

Examples include:

```text
library
engineering
science
cafeteria
sports
student activity centre
security
parking
bus/transit
```

---

# 128. Useful Bounded-Space Concept

For:

```text
campuses
airports
malls
hospitals
office complexes
```

an institutional place ontology is genuinely useful.

Dzoka should retain this idea.

---

# 129. But It Is Not Geographic Resolution

The coordinates are:

```text
x/y percentages
```

on a predefined vector campus map.

They are not actual geographic coordinates.

---

# 130. Fuzzy Resolver

Free text is compared against:

```text
hard-coded keywords
location names
```

using string similarity.

This is acceptable for a campus demo.

---

# 131. Dangerous Fallback

Unknown locations may be assigned to one of the predefined buildings using:

```text
hash(location) % number_of_locations
```

---

# 132. Consequence

A completely unknown location may be mapped to an arbitrary campus building.

That invented location can then appear authoritative.

---

# 133. Dzoka Rejection

Never invent a physical location merely because location resolution failed.

Use:

```text
resolutionStatus = UNRESOLVED
```

---

# 134. Coordinate Jitter

SmartFind applies deterministic random jitter so multiple items do not overlap visually.

This is fine for visualization.

It must never become factual location evidence.

---

# 135. Dzoka Separation

Store separately:

```text
actual reported location
resolved place
map-display offset
```

---

# 136. Benchmark Claims

README presents detailed metrics for:

```text
TF-IDF
FastText
MiniLM
HSV/SSIM
ResNet-50
CLIP
SmartFind multimodal ensemble
```

---

# 137. Actual Benchmark Endpoint

`get_benchmark_metrics()` returns those metrics as:

```text
hard-coded Python dictionaries
```

---

# 138. No Benchmark Computation

The endpoint does not:

```text
load dataset
run models
generate predictions
calculate precision
calculate recall
calculate F1
calculate MRR
```

---

# 139. Dataset Reproducibility

The repository root does not contain an inspected:

```text
CLF-Bench-500
```

dataset or a reproducible evaluation pipeline producing those tables.

---

# 140. Audit Classification of Metrics

Therefore the values should be treated as:

```text
DOCUMENTED / STATIC CLAIMS
```

not verified empirical results.

---

# 141. “Live Sentence Transformer” Finding

The live experiment route includes a model labelled:

```text
Sentence-Transformers (all-MiniLM-L6-v2 Dense Vectors)
```

---

# 142. Actual Implementation

It does not load MiniLM.

Instead it combines:

```text
TF-IDF score
SequenceMatcher score
Jaccard token overlap
constant adjustment
```

---

# 143. Explicit Simulation

The source comment itself describes this as:

```text
Dense Semantic Transformer Emulation
```

---

# 144. Artificial Latency

The implementation also adds a fixed value to the measured time to simulate transformer latency.

Therefore reported latency does not represent transformer inference.

---

# 145. Dzoka Rule

Never label:

```text
heuristic emulation
```

as:

```text
actual model execution
```

in production or evaluation reports.

---

# 146. README vs Source — Sentence Transformers

README:

```text
Sentence-Transformers all-MiniLM-L6-v2
```

Actual matcher:

```text
not loaded
```

---

# 147. README vs Source — CLIP

README:

```text
CLIP ViT-B/32
```

Actual matcher:

```text
not loaded
```

---

# 148. README vs Source — ResNet-50

README:

```text
ResNet-50 embeddings
```

Actual matcher:

```text
not loaded
```

---

# 149. README vs Source — HSV / SSIM

README:

```text
HSV histogram + SSIM
```

Actual matcher:

```text
not implemented
```

---

# 150. README vs Source — Visual Embeddings

README:

```text
image embeddings
```

Actual active matching implementation:

```text
synthetic image score from text score / URL presence
```

---

# 151. README vs Source — Cryptographic Ownership Verification

README:

```text
cryptographic ownership verification
```

Actual implementation:

```text
string similarity against report text
```

---

# 152. README vs Source — Handover OTP

README/lifecycle API:

```text
handover OTP enabled
```

Actual implementation:

```text
OTP value ignored
```

---

# 153. README vs Source — Match Run Access

README:

```text
Admin
```

Actual endpoint:

```text
any authenticated user
```

---

# 154. README vs Source — Contact Privacy

README:

```text
contact released after verification
```

Actual report API:

```text
public response can expose reporter email/phone
```

---

# 155. README vs Source — MIT License

README:

```text
MIT
```

Repository root:

```text
LICENSE file absent
```

---

# 156. Test Suite

The repository contains:

```text
test_phase1.py
...
test_phase7.py
```

and the README claims:

```text
49 / 49 tests passing
```

---

# 157. Audit Qualification

Those tests were not independently executed during this audit.

More importantly, passing tests do not prove claimed algorithms exist.

---

# 158. Image Test Weakness

The matching test uses the same:

```text
image URL
```

for the high-match pair.

The image function therefore gives:

```text
100
```

without loading the image.

---

# 159. Handover Test Weakness

The test sends an OTP-like value to:

```text
handover-confirm
```

and verifies successful completion.

It does not test an incorrect OTP because the implementation does not validate OTPs.

---

# 160. Test Philosophy Lesson

Tests can confirm implementation consistency while still validating incorrect or incomplete semantics.

Dzoka tests must validate security and domain invariants, not merely endpoint success.

---

# 161. Authentication

Positive implementation choices include:

```text
bcrypt
JWT expiry
active-user checks
role field
dependency-based authentication
```

---

# 162. Password Hashing

bcrypt uses:

```text
12 rounds
```

which is a reasonable baseline.

---

# 163. Predictable JWT Fallback Secret

`config.py` includes a hard-coded default:

```text
SECRET_KEY
```

if no environment variable is provided.

A known fallback signing secret is unsafe for production.

---

# 164. Committed `.env`

The repository also contains:

```text
backend/.env
```

with runtime configuration keys including:

```text
SECRET_KEY
DATABASE_URL
```

Exact values should not be propagated.

Runtime `.env` files should normally not be committed.

---

# 165. Dzoka Secret Rule

Production secrets must come from:

```text
secret manager
deployment environment
encrypted secret mechanism
```

and never from repository defaults.

---

# 166. Default Accounts

Startup automatically seeds:

```text
admin@smartfind.com
user@smartfind.com
```

with documented default passwords.

---

# 167. Production Risk

Known administrator credentials must not exist in production startup behavior.

---

# 168. CORS

The application configures:

```text
allow_origins = ["*"]
allow_credentials = true
allow_methods = ["*"]
allow_headers = ["*"]
```

This is far too permissive as a production default.

---

# 169. Dzoka CORS Rule

Allowed origins must be environment-specific and explicit.

---

# 170. Database

The application defaults to:

```text
SQLite
```

which is perfectly reasonable for a student/demo application.

It is not Dzoka's production multi-node persistence architecture.

---

# 171. Schema Management

The backend invokes:

```text
Base.metadata.create_all(...)
```

at startup.

No production migration framework was identified in the inspected dependency set.

---

# 172. Dzoka Difference

Dzoka requires controlled migrations:

```text
Flyway / Liquibase
```

for the Java transactional core.

---

# 173. Committed Database Files

The repository contains:

```text
smartfind.db
test_phase3.db
test_phase4.db
test_phase5.db
test_phase6.db
test_phase7.db
```

Runtime/test database artifacts generally should not be committed to a production repository.

---

# 174. Audit Logging

SmartFind includes:

```text
AuditLog
```

which is a positive concept.

---

# 175. Audit Service Weakness

The audit helper defaults the source IP to:

```text
127.0.0.1
```

rather than deriving trustworthy request context.

It is therefore not a complete security audit architecture.

---

# 176. Dzoka Audit Requirements

Audit records should capture:

```text
actor
tenant
action
resource
decision
timestamp
correlation
request context
policy
reason
result
```

without leaking secrets.

---

# 177. Notifications

SmartFind has a persistent notification model.

This is a useful concept for:

```text
match alerts
claims
approvals
rejections
reputation
```

---

# 178. Dzoka Extension

Notifications should be generated asynchronously from domain/integration events.

---

# 179. Reputation

The system rewards successful return activity with reputation points.

The concept can encourage positive recovery participation.

---

# 180. But Reputation Must Be Derived from Verified Events

Because handover completion currently has authorization/idempotency weaknesses, reputation can inherit those weaknesses.

Dzoka must make:

```text
Reward / Reputation
```

downstream of authoritative recovery events.

---

# 181. No Hard Conflict Engine

SmartFind does not provide Dzoka's explicit:

```text
ConflictEngine
```

For example:

```text
serial number mismatch
```

does not automatically override superficial visual/text similarity.

---

# 182. No Evidence Coverage

There is no explicit measure of:

```text
how much evidence was actually available
```

when a score was produced.

This is particularly harmful because missing modalities receive fabricated positive values.

---

# 183. No Local-Instance Visual Matching

The platform does not implement:

```text
local patch embeddings
distinctive region matching
keypoint matching
perceptual hashing
ReID
```

---

# 184. No OCR

OCR is not part of the active match pipeline.

Therefore:

```text
labels
serial text
engraving
document text
```

are not independently extracted from images.

---

# 185. No Protected-Identifier Matching

There is no architecture for securely comparing:

```text
IMEI
serial number
asset tag
identity-document identifier
```

without public exposure.

---

# 186. No Vector Retrieval

The system does not use:

```text
pgvector
FAISS
Qdrant
HNSW
```

for candidate retrieval.

---

# 187. No Federation

SmartFind is a single-instance campus application.

It does not implement:

```text
institutional peers
signed federation
capability exchange
federated queries
cross-node claims
```

---

# 188. No Real Custody Model

Although it presents a lifecycle timeline, it does not model:

```text
current custodian
custody location
custody transfer
custody acceptance
release authority
```

as a formal aggregate.

---

# 189. No Recovery Authorization

Claim approval leads directly toward handover.

There is no explicit:

```text
RecoveryAuthorization
```

aggregate separating ownership/entitlement from physical release.

---

# 190. No Competing Claim Resolution

Multiple claims can exist, but no robust:

```text
ClaimConflict
RecoveryHold
Adjudication
```

workflow was identified.

---

# 191. No True Handover Credential

The schema contains an OTP field, but no usable OTP protocol exists.

Therefore the project does not provide a true handover credential implementation.

---

# 192. Strongest Reusable Concepts

Despite the problems, several architectural ideas are useful:

```text
separate Match entity

separate Claim entity

feature-score breakdown

human-readable match explanation

automatic matching after report creation

match lifecycle status

claim verification workflow

proof upload UX

seven-stage recovery timeline

campus place ontology

QR recovery concept

reputation concept

audit entity

notification entity
```

---

# 193. What Dzoka Should Reference

Use SmartFind as a reference for:

```text
campus-oriented UX

match comparison screens

score-breakdown presentation

lifecycle visualization

admin analytics concepts

bounded-location ontology

QR tag UX

claim dashboards
```

---

# 194. What Dzoka Must Not Use as AI Reference

Do not use this repository as the technical basis for:

```text
visual retrieval
CLIP
ResNet
instance recognition
multimodal embeddings
model evaluation
calibrated matching
ownership AI
```

---

# 195. What Dzoka Must Explicitly Reject

Reject:

```text
image similarity derived from text

missing evidence given arbitrary positive scores

fake visual explanations

hard-coded benchmark metrics presented as empirical computation

transformer emulation presented as transformer execution

arbitrary simulated model latency

public owner contact information

unprotected claim proof files

unvalidated handover OTP

match confirmation without authorization

claim completion without authorization

repeatable reputation award

CLAIMED = RETURNED

predictable JWT fallback secret

known production-style default admin credentials

hash-based invented locations
```

---

# 196. Dzoka Mapping — Matching

SmartFind:

```text
Report
   ↓
Compare every opposite report
   ↓
weighted heuristic
   ↓
AIMatch
```

Dzoka:

```text
Report
   ↓
ItemFingerprint
   ↓
multi-strategy candidate retrieval
   ↓
rank fusion
   ↓
hard conflict evaluation
   ↓
feature scoring
   ↓
local-instance reranking
   ↓
coverage
   ↓
confidence/calibration
   ↓
MatchCandidate
```

---

# 197. Dzoka Mapping — Image Evidence

SmartFind:

```text
image URL exists?
       ↓
derive score from text
```

Dzoka:

```text
MediaAsset
    ↓
detection/crop
    ↓
global embedding
    ↓
local patch features
    ↓
perceptual evidence
    ↓
OCR
    ↓
model-aware visual comparison
```

---

# 198. Dzoka Mapping — Missing Evidence

SmartFind:

```text
missing brand → 50/70
missing color → 60
missing location → 30
bad date → 50
no image → text-derived image score
```

Dzoka:

```text
missing
→ unavailable
→ no similarity contribution
→ evidence coverage reduced
```

This distinction is fundamental.

---

# 199. Dzoka Mapping — Verification

SmartFind:

```text
claimant answer
    ↓
compare to report's public text
    ↓
verification percentage
```

Dzoka:

```text
private evidence inventory
        ↓
safe challenge
        ↓
claimant response
        ↓
typed evidence evaluation
        ↓
risk
        ↓
policy
        ↓
human review
        ↓
VerificationDecision
```

---

# 200. Dzoka Mapping — Handover

SmartFind:

```text
handover endpoint
    ↓
status RESOLVED
    ↓
reputation
```

Dzoka:

```text
approved Claim
        ↓
RecoveryAuthorization
        ↓
handover credential
        ↓
credential verification
        ↓
CustodyTransfer
        ↓
recipient confirmation
        ↓
RecoveryCompleted
        ↓
reward/reputation
```

---

# 201. Dzoka Mapping — QR

SmartFind:

```text
report ID
→ deterministic tag
→ report URL
```

Dzoka:

```text
ItemIdentity
    ↓
opaque/signed resolver identity
    ↓
privacy-safe landing flow
    ↓
RecoverySession
```

---

# 202. Dzoka Mapping — Location

SmartFind:

```text
free text
→ hard-coded campus landmark
→ artificial display coordinates
```

Dzoka:

```text
raw user location
+
institution location hierarchy
+
optional geocoding
+
actual geometry where available
+
resolution confidence
```

---

# 203. Good Campus Architecture Idea

The bounded campus dictionary is useful enough to retain conceptually.

A Dzoka institutional node could maintain:

```text
Organization
└── Sites
    └── Buildings
        └── Floors
            └── Zones
                └── RecoveryPoints
```

---

# 204. Location Must Remain Evidence-Aware

A resolver result should carry:

```text
EXACT
RESOLVED
APPROXIMATE
AMBIGUOUS
UNRESOLVED
```

rather than always producing a place.

---

# 205. Source Claims vs Implementation Summary

| Claimed capability | Audited implementation |
|---|---|
| TF-IDF | Implemented if scikit-learn happens to be installed |
| Sentence Transformers | Not actually loaded |
| CLIP | Not implemented in active matcher |
| ResNet-50 | Not implemented |
| HSV histogram | Not implemented |
| SSIM | Not implemented |
| Image embeddings | Not implemented |
| Multimodal visual matching | Synthetic image score |
| Spatial matching | String similarity / bounded campus resolver |
| Temporal decay | Implemented |
| Attribute matching | Implemented heuristically |
| Explainability | Implemented, but some explanations overstate evidence |
| Ownership verification | Public-text similarity + human approval |
| Cryptographic verification | Not demonstrated |
| QR generation | Optional dependency; fallback is decorative |
| Cryptographic QR | Not implemented |
| Handover OTP | Field exists; validation absent |
| Lifecycle | Implemented as derived projection |
| Benchmark endpoint | Hard-coded metrics |
| MiniLM live experiment | Heuristic simulation |
| Admin-only match run | Not enforced |
| Privacy-gated contacts | Contradicted by public report schema |
| Claim proof protection | Files served from public static mount |

---

# 206. Security Severity Summary

## Critical / High

```text
public exposure of reporter email/phone

match decision mutation without ownership/admin authorization

claim completion without actor authorization

handover OTP not validated

pending/unapproved claimant may qualify for lifecycle handover

repeatable handover reputation risk

private proof documents served from static uploads

known fallback JWT signing secret

default administrator credentials
```

## Medium

```text
system-wide match execution by any authenticated user

extension-only upload validation

committed .env

broad CORS

committed SQLite databases

audit IP defaulting to localhost

missing production migration tooling
```

---

# 207. AI Integrity Severity Summary

## Critical

```text
image score is not image similarity

explanation claims embeddings/histograms were evaluated when they were not

benchmark metrics are constants

transformer model comparison is simulated
```

## High

```text
missing evidence contributes fabricated positive similarity

requirements do not include scikit-learn

no model provenance

arbitrary confidence thresholds
```

---

# 208. Positive Security Patterns

The repository nevertheless demonstrates some good practices:

```text
bcrypt password hashing

JWT expiry

active-user enforcement

role-based admin dependency exists

owner/admin checks on report edits

owner/admin checks on claim approval

randomized uploaded filenames

authenticated report image upload

separate AuditLog entity
```

These are worth retaining conceptually.

---

# 209. Important Negative Lesson — Names Do Not Prove Capability

A function called:

```text
compute_multimodal_match
```

does not prove actual multimodal ML.

A field called:

```text
verification_score
```

does not prove ownership verification.

A flag called:

```text
handover_otp_enabled
```

does not prove OTP enforcement.

A route called:

```text
benchmark
```

does not prove benchmarks were computed.

---

# 210. Source Audit Value

This repository is an excellent example of why Dzoka's source-audit process is mandatory.

Without inspecting source, we might have assumed SmartFind provided:

```text
CLIP
ResNet
MiniLM
true image similarity
cryptographic verification
validated handover OTP
reproducible ML benchmarks
```

It does not.

---

# 211. Influence on Dzoka `item-fingerprint.md`

This audit strongly reinforces that a true `ItemFingerprint` cannot consist only of:

```text
item name
description
brand
color
image URL
```

It needs actual computed evidence.

---

# 212. Required Dzoka Visual Fingerprint

At minimum:

```text
global visual embedding
local distinctive-region embeddings
perceptual signatures
OCR observations
detected attributes
model provenance
```

where appropriate.

---

# 213. Influence on Dzoka `matching-engine.md`

This audit reinforces:

```text
missing evidence ≠ mismatch
missing evidence ≠ synthetic similarity
```

and:

```text
every score must be traceable to real evidence
```

---

# 214. Influence on Dzoka `ownership-verification.md`

This audit reinforces that:

```text
public report similarity
```

must never be treated as strong ownership evidence.

---

# 215. Influence on Dzoka `claim-workflow.md`

Claim approval should be separate from:

```text
contact release
recovery authorization
custody
handover
case closure
```

---

# 216. Influence on Dzoka `custody-state-machine.md`

The `CLAIMED = RETURNED` bug demonstrates exactly why Dzoka needs a formal custody state machine.

---

# 217. Relevant Architecture Documents

This audit should influence:

```text
docs/architecture/ai-architecture.md
docs/architecture/item-fingerprint.md
docs/architecture/matching-architecture.md
docs/architecture/verification-architecture.md
docs/architecture/security-architecture.md
docs/architecture/privacy-architecture.md
docs/architecture/recovery-graph.md
docs/architecture/movement-intelligence.md
docs/architecture/data-architecture.md
docs/architecture/observability.md
```

---

# 218. Potential ADRs

Possible ADRs influenced by this audit:

```text
missing evidence represented explicitly

model provenance required for AI evidence

no synthetic modality scores

private proof media requires authenticated object access

handover credential must be server-validated

one authoritative recovery completion command

public API uses privacy-safe projections

benchmarks must be reproducible from versioned datasets
```

---

# 219. What Dzoka Should Adopt Directly

Until licensing is clarified:

```text
DIRECT CODE ADOPTION:
NONE
```

---

# 220. What Dzoka Should Adapt Conceptually

Independently implement the useful concepts:

```text
Report → Match → Claim workflow

persisted match-feature breakdown

human-readable explanations

automatic matching after new reports

explicit match decision state

claim proof evidence

bounded institutional place ontology

recovery lifecycle timeline

QR-linked recovery identity

notifications

reputation / positive recovery incentives
```

---

# 221. What Dzoka Should Reference Only

Reference SmartFind for:

```text
campus-focused product UX

analytics/dashboard presentation

lifecycle presentation

match-detail UI

admin workflow ideas

educational architecture decomposition
```

---

# 222. Explicitly Do Not Reuse

Do not reproduce:

```text
synthetic image scoring

fabricated visual XAI

hard-coded benchmark results

MiniLM emulation

simulated inference latency

positive default scores for missing evidence

public owner contacts

unprotected proof files

unvalidated OTP handover

unrestricted match confirmation

unauthorized claim completion

duplicate recovery completion paths

arbitrary location assignment
```

---

# 223. Recommended Source-Audit Lock Entry

Add:

```json
{
  "id": "ai-lost-and-found",
  "category": "platforms",
  "repository": "https://github.com/dhanushshetty40/AI-lost-and-found",
  "revision": "577edc3b492f62ac8707fc950230bc04a354e9d8",
  "license": "UNRESOLVED",
  "licenseNote": "README declares MIT but no root LICENSE file exists at audited revision.",
  "classification": "REFERENCE",
  "audit": "docs/source-audit/platforms/ai-lost-and-found.md",
  "auditedAt": "2026-10-04"
}
```

---

# 224. Re-Audit Triggers

Re-audit if upstream:

```text
adds actual LICENSE text

adds real CLIP/OpenCLIP matching

adds image pixel processing

adds local-instance visual features

adds actual sentence-transformer execution

publishes reproducible benchmark dataset/scripts

fixes public contact leakage

protects proof evidence

fixes match authorization

fixes claim completion authorization

implements real OTP generation/validation

adds recovery idempotency

replaces SQLite demo architecture

adds model/version provenance
```

---

# 225. Final Decision

```text
CLASSIFICATION:
REFERENCE ONLY
```

SmartFind should **not** influence Dzoka's core AI implementation.

It should influence selected:

```text
workflow
UX
domain decomposition
campus/institution concepts
```

only.

---

# 226. Final Architecture Lesson

SmartFind exposes one of the most important principles for Dzoka's entire architecture:

> **A system must not manufacture evidence merely to keep every score populated.**

The wrong pattern is:

```text
no image
→ invent image score from text

no brand
→ assign 70

invalid date
→ assign 50

unknown location
→ invent campus location
```

The Dzoka pattern is:

```text
UNKNOWN
≠
MATCH

MISSING
≠
MISMATCH

MISSING
≠
SYNTHETIC SCORE
```

Instead:

```text
evidence unavailable
        ↓
exclude feature from comparison
        ↓
reduce evidence coverage
        ↓
reflect uncertainty honestly
```

---

# 227. Final Dzoka Synthesis

SmartFind:

```text
LOST REPORT
     ↓
heuristic "multimodal" scoring
     ↓
AI Match
     ↓
Claim
     ↓
text similarity verification
     ↓
approval
     ↓
handover endpoint
     ↓
resolved
```

Dzoka:

```text
LOST / FOUND OBSERVATION
          ↓
REAL ITEM FINGERPRINT
          ↓
HYBRID RETRIEVAL
          ↓
REAL MODALITY EVIDENCE
          ↓
HARD CONFLICTS
          ↓
EVIDENCE COVERAGE
          ↓
MATCH CANDIDATE
          ↓
CLAIM
          ↓
PRIVATE OWNERSHIP EVIDENCE
          ↓
VERIFICATION + RISK + POLICY
          ↓
RECOVERY AUTHORIZATION
          ↓
SECURE HANDOVER CREDENTIAL
          ↓
CUSTODY TRANSFER
          ↓
RECOVERY CONFIRMATION
          ↓
AUDIT / REPUTATION / LEARNING
```

The central conclusion is:

> **SmartFind is useful as a workflow and campus-product reference, but its advertised multimodal AI, visual matching, cryptographic verification, QR verification, and benchmark capabilities are substantially stronger in documentation than in the audited source. Dzoka should borrow the domain ideas—not the AI implementation—and should enforce a strict rule that every score, explanation, verification result, and recovery transition must be backed by real, traceable evidence.**