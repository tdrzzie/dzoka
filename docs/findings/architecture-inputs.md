# Architecture Inputs

> **Dzoka — The Open Recovery Network**
>
> Consolidated architecture inputs derived from Dzoka's source-audit programme. This document translates upstream implementation findings into explicit Dzoka architectural requirements, constraints, component boundaries, ADR candidates, provider decisions, and design invariants.

---

# 1. Purpose

Source audits are only valuable if their findings materially influence architecture.

This document answers:

```text
What did the audited systems teach us?

What architecture decision does that imply?

Which Dzoka component owns the decision?

Which ideas should be adopted, adapted, rejected, or built?

Which decisions require ADRs?
```

It acts as the bridge between:

```text
docs/source-audit/
```

and:

```text
ARCHITECTURE.md
DESIGN.md
docs/architecture/
docs/design/
adr/
```

---

# 2. Relationship to Other Findings

This file should be read with:

```text
docs/source-audit/findings/reusable-components.md
docs/source-audit/findings/rejected-patterns.md
docs/source-audit/findings/licensing-matrix.md
docs/source-audit/findings/unresolved-gaps.md
```

Their responsibilities are different.

## `reusable-components.md`

Answers:

```text
What is useful?
```

## `rejected-patterns.md`

Answers:

```text
What must Dzoka avoid?
```

## `licensing-matrix.md`

Answers:

```text
What are we legally allowed to reuse?
```

## `architecture-inputs.md`

Answers:

```text
What architecture follows from those findings?
```

---

# 3. Input Classification

Architecture inputs are classified as:

| Classification | Meaning |
|---|---|
| `MANDATORY` | Must shape Dzoka architecture |
| `DEFAULT` | Recommended default unless ADR changes it |
| `OPTIONAL` | Supported through a provider/adapter |
| `FUTURE` | Architecturally accommodated but not mandatory now |
| `RESEARCH` | Influences experiments/evaluation rather than production baseline |
| `PROHIBITED` | Architecture must explicitly prevent it |

---

# 4. Source Influence Overview

The major source influences are:

| Source | Primary architecture input |
|---|---|
| `globalenglish01/lostfound` | matching discipline, evidence modeling, hard conflicts |
| `FuricBond/Lost-And-Found` | multimodal visual matching |
| `Lost-and-Found-Tracking-System` | claims, institutions, risk, audit |
| `Malak-Rashad/LostAndFound` | private ownership challenges |
| `Smart Asset Finder` | QR recovery, WhatsApp, rewards, registered assets |
| `FindBack` | mobile recovery flow, push notifications, handover concepts |
| `ILIAS` | instance-level retrieval |
| `Patchwise Retrieval` | local visual evidence |
| `OpenCLIP` | global image embeddings |
| `pgvector` | default vector retrieval |
| `Qdrant` | optional vector scale-out |
| `PaddleOCR` | OCR |
| `PaddleDetection` | detection/tracking |
| `CCTV Theft Finder` | coarse-to-fine CCTV analysis |
| `Trajectron++` | movement-prediction research |
| `Mycel` | federation patterns |
| `Open Federated Search` | historical federation concepts |
| `OpenEPCIS` | event traceability |
| `GS1` | traceability semantics |
| `OpenEscrow` | dispute/release state-machine ideas |

---

# 5. Architecture Input — Dzoka Must Be a Recovery Network, Not a Listings Application

**Classification:** `MANDATORY`

Most simple upstream projects implement:

```text
post lost item
post found item
search listings
contact person
```

Dzoka must instead model:

```text
Report
    ↓
Fingerprint
    ↓
Matching
    ↓
Claim
    ↓
Verification
    ↓
Recovery Authorization
    ↓
Custody
    ↓
Handover / Logistics
    ↓
Recovery
```

### Architectural consequence

Dzoka requires separate bounded contexts for:

```text
Reporting
Matching
Claims
Verification
Custody
Recovery
Federation
```

---

# 6. Architecture Input — Physical Item Is Not the Report

**Sources:**

```text
globalenglish01/lostfound
multiple direct platform audits
```

**Classification:** `MANDATORY`

Use:

```text
ItemRecord
LostReport
FoundReport
```

as distinct concepts.

### Reason

Several reports may describe:

```text
one physical object
```

while one report may initially be associated with:

```text
multiple candidate objects
```

---

# 7. Architecture Input — ItemFingerprint Must Be a First-Class Domain/AI Boundary

**Sources:**

```text
globalenglish
FuricBond
ILIAS
Patchwise Retrieval
OpenCLIP
PaddleOCR
Dzoka synthesis
```

**Classification:** `MANDATORY`

Dzoka requires a canonical:

```text
ItemFingerprint
```

between report ingestion and matching.

---

# 8. ItemFingerprint Required Evidence Families

The architecture must support:

```text
semantic identity
structured attributes
global vision
local visual regions
distinctive marks
OCR
protected identifiers
time
location
provenance
quality
coverage
```

### Architectural consequence

Fingerprints cannot be represented as:

```text
vector[]
```

alone.

---

# 9. ItemFingerprint Storage Input

**Classification:** `MANDATORY`

Store fingerprint components separately enough to support:

```text
re-embedding
model migration
different dimensions
different modalities
different security classifications
```

### Data architecture

Use normalized relational metadata plus vector columns/providers.

---

# 10. Architecture Input — Raw User Evidence Must Survive AI Processing

**Source influence:** `globalenglish`

**Classification:** `MANDATORY`

Preserve:

```text
raw user description
raw timestamps
raw location phrases
original media
```

alongside:

```text
normalized
translated
AI-extracted
canonical
```

representations.

---

# 11. Architectural Ownership

This requirement affects:

```text
Reporting context
ItemFingerprint
Data architecture
AI architecture
Audit architecture
```

---

# 12. Architecture Input — Matching Must Be Hybrid

**Sources:**

```text
globalenglish
VisualMatch
OpenVisualSearch
ILIAS
pgvector
```

**Classification:** `MANDATORY`

Do not use a single retrieval method.

Candidate discovery should combine:

```text
structured attributes
full-text search
trigram search
semantic text
global image vectors
protected identifier lookup
geospatial evidence
temporal evidence
local visual evidence where appropriate
```

---

# 13. Architecture Input — Retrieval and Scoring Are Separate Stages

**Classification:** `MANDATORY`

Pipeline:

```text
Candidate Retrieval
       ↓
Union / Dedupe
       ↓
Rank Fusion
       ↓
Conflict Checks
       ↓
Feature Computation
       ↓
Reranking
       ↓
Confidence
```

### Reason

Retrieval optimizes:

```text
recall
```

while final ranking optimizes:

```text
precision
```

---

# 14. Architecture Input — Default Search Stack

**Sources:**

```text
pgvector
PostGIS
PostgreSQL FTS
pg_trgm
```

**Classification:** `DEFAULT`

V1 default:

```text
PostgreSQL
├── PostGIS
├── pgvector
├── pg_trgm
└── Full-Text Search
```

---

# 15. Why PostgreSQL Is the Default

It minimizes operational complexity while supporting:

```text
relational state
geospatial queries
semantic retrieval
lexical search
fuzzy search
transactional metadata
```

---

# 16. Architecture Input — Qdrant Must Be Optional

**Source:** `qdrant/qdrant`

**Classification:** `OPTIONAL`

Use:

```text
VectorSearchProvider
```

so:

```text
pgvector
```

can remain default and:

```text
Qdrant
```

can be introduced when justified.

---

# 17. ADR Input — Vector Backend

Potential ADR:

```text
Use pgvector as default vector backend and expose a replaceable VectorSearchProvider
```

---

# 18. Architecture Input — Global Embeddings Are Candidate Signals, Not Physical Identity

**Sources:**

```text
OpenCLIP
FuricBond
ILIAS
Patchwise Retrieval
```

**Classification:** `MANDATORY`

Global embeddings answer:

```text
does this look semantically similar?
```

They do not reliably answer:

```text
is this the exact same physical object?
```

---

# 19. Architectural Consequence — Local Instance Evidence

Dzoka needs a local-instance pipeline supporting:

```text
distinctive regions
patches
logos
damage
scratches
stickers
keypoint-style evidence
```

for top candidates.

---

# 20. Architecture Input — Expensive Visual Reranking Must Be Bounded

**Sources:**

```text
ILIAS
Patchwise Retrieval
RerankingTransformer
```

**Classification:** `DEFAULT`

Do not run expensive local comparison against the entire corpus.

Use:

```text
cheap retrieval
    ↓
top N
    ↓
local visual reranker
```

---

# 21. Architecture Input — Detection Provider Must Be Replaceable

**Sources:**

```text
PaddleDetection
Detectron2
Ultralytics
```

**Classification:** `MANDATORY`

Interface:

```text
DetectionProvider
```

with default candidate:

```text
PaddleDetection
```

---

# 22. ADR Input — Detection Default

Recommended decision:

```text
PaddleDetection as default permissive detector/tracker
Detectron2 as supported alternative
Ultralytics optional due licensing
```

---

# 23. Architecture Input — OCR Is First-Class Evidence

**Source:** `PaddleOCR`

**Classification:** `MANDATORY`

OCR may contribute:

```text
brand
serial fragment
name
label
document text
institutional asset ID
```

---

# 24. Privacy Consequence of OCR

OCR outputs require classification before:

```text
indexing
federation
display
AI prompts
```

because OCR may extract:

```text
PII
protected identifiers
financial data
identity documents
```

---

# 25. Architecture Input — Protected Identifiers Need Dedicated Handling

**Sources:**

```text
globalenglish
FindBack
Smart Asset Finder
```

**Classification:** `MANDATORY`

Protected identifiers such as:

```text
IMEI
serial
passport number
national ID
institution asset tag
```

must not be ordinary public attributes.

---

# 26. Architectural Component

Dzoka requires something equivalent to:

```text
ProtectedIdentifier
├── type
├── safe display value
├── keyed searchable representation
├── encrypted source value
├── provenance
└── verification status
```

---

# 27. Architecture Input — Hard Conflicts Are Separate from Similarity

**Source influence:** `globalenglish`

**Classification:** `MANDATORY`

The matching architecture must include:

```text
ConflictEngine
```

before final confidence.

---

# 28. Example Hard Conflict

```text
visual score = 0.96
text score = 0.90
serial A != serial B
```

should not simply average into:

```text
high match
```

---

# 29. Architecture Input — Missing Evidence Must Remain Missing

**Sources:**

```text
globalenglish
FindBack negative findings
SmartFind negative findings
```

**Classification:** `MANDATORY`

Canonical semantics:

```text
available = false
score = null
```

---

# 30. Architectural Consequence — Evidence Coverage

Matching must calculate:

```text
EvidenceCoverage
```

separately from:

```text
MatchScore
```

---

# 31. Architecture Input — Confidence Is Not Raw Similarity

**Classification:** `MANDATORY`

Maintain distinct concepts:

```text
retrieval score
feature similarity
match score
evidence coverage
calibrated confidence
```

---

# 32. ADR Input — Confidence Calibration

Potential ADR:

```text
Raw matching scores are not exposed as probabilities unless calibrated against labeled evaluation data.
```

---

# 33. Architecture Input — Matching Must Be Reproducible

**Sources:**

```text
globalenglish
Lost-and-Found-Tracking-System
```

**Classification:** `MANDATORY`

Every run must retain:

```text
algorithmVersion
configurationVersion
modelVersions
candidate policy
feature values
```

---

# 34. Architectural Component — MatchingRun

Required:

```text
MatchingRun
```

with immutable run metadata.

---

# 35. Architecture Input — Matching Configuration Must Be Versioned

**Classification:** `MANDATORY`

Do not bury important weights in source code.

Version:

```text
retriever weights
feature weights
category profiles
environment profiles
thresholds
confidence policies
```

---

# 36. Architecture Input — Category-Specific Matching Profiles

**Classification:** `DEFAULT`

Different objects require different evidence.

Example:

```text
passport:
identifier >> image

umbrella:
appearance + location + time >> identifier
```

---

# 37. Architecture Input — Environment-Specific Location Logic

**Sources:**

```text
globalenglish
Lost-and-Found-Tracking-System
Smart Asset Finder
```

**Classification:** `DEFAULT`

Location decay should depend on environment.

Examples:

```text
airport
bus route
rail station
university
mall
hotel
```

---

# 38. Architecture Input — Hierarchical Location Model

**Sources:**

```text
Lost-and-Found-Tracking-System
SmartFind campus structures
```

**Classification:** `MANDATORY`

Support:

```text
Organization
└── Site
    └── Building
        └── Floor
            └── Zone
```

plus optional geometry.

---

# 39. Architecture Input — PostGIS

**Classification:** `DEFAULT`

Use PostGIS for:

```text
points
polygons
zones
distance
coverage areas
facility boundaries
```

---

# 40. Architecture Input — Movement Intelligence Must Be Separate from Factual Custody

**Sources:**

```text
Trajectron++
CCTV audits
Dzoka synthesis
```

**Classification:** `MANDATORY`

Prediction:

```text
likely next location
```

must never mutate:

```text
current custody
```

---

# 41. Movement Intelligence Architectural Model

Prefer:

```text
state transition probabilities
```

such as:

```text
Airport Gate
→ Security Desk
→ Central Lost Property
```

over generic x/y trajectory prediction where institutional transitions dominate.

---

# 42. Architecture Input — Recovery Graph Is Initially Relational

**Classification:** `DEFAULT`

Use PostgreSQL to model:

```text
locations
transitions
custody events
organizations
routes
```

and derive graph-like projections.

---

# 43. ADR Input — Dedicated Graph Database

Only introduce one if measurement demonstrates PostgreSQL is insufficient.

---

# 44. Architecture Input — Match Is Never Ownership

**Sources:** essentially all claim-oriented audits

**Classification:** `MANDATORY`

Matching can produce:

```text
candidate
```

not:

```text
owner
```

---

# 45. Architecture Input — Ownership Verification Requires a Dedicated Context

**Sources:**

```text
Malak-Rashad/LostAndFound
Lost-and-Found-Tracking-System
Smart Asset Finder
```

**Classification:** `MANDATORY`

Required bounded context:

```text
Verification
```

---

# 46. Architecture Input — Private Evidence Must Exist

**Source influence:** `Malak-Rashad/LostAndFound`

**Classification:** `MANDATORY`

Some evidence should intentionally remain unavailable publicly.

Examples:

```text
interior contents
unique damage
private sticker
engraving
serial suffix
lock-screen detail
```

---

# 47. Architecture Input — Private Evidence Is Part of Fingerprint but Has Separate Projection

**Classification:** `MANDATORY`

`ItemFingerprint` requires:

```text
FullFingerprint
PublicFingerprint
MatchingFingerprint
FederatedFingerprint
VerificationFingerprint
```

or equivalent projections.

---

# 48. Architecture Input — VerificationChallenge Is First-Class

**Classification:** `BUILD`

Required:

```text
VerificationChallenge
```

with:

```text
source
expected evidence reference
difficulty
disclosure risk
attempt policy
expiry
```

---

# 49. Architecture Input — Challenge Generation Requires Disclosure Guard

**Classification:** `MANDATORY`

Do not ask:

```text
What colour is the red hidden sticker?
```

when:

```text
red
```

is the answer.

---

# 50. Architecture Input — Verification Questions May Come from Humans or AI

**Sources:**

```text
Malak-Rashad
Dzoka synthesis
```

**Classification:** `DEFAULT`

Sources may include:

```text
FINDER
STAFF
OWNER
AI
SYSTEM
```

with provenance.

---

# 51. Architecture Input — LLM Can Interpret, Not Decide Ownership

**Classification:** `MANDATORY`

LLM roles:

```text
question wording
semantic answer interpretation
summarization
risk explanation
```

Not:

```text
release authority
ownership authority
```

---

# 52. Architecture Input — Verification Evidence Must Be Typed

**Sources:**

```text
Lost-and-Found-Tracking-System
FindBack
Smart Asset Finder
```

**Classification:** `MANDATORY`

Examples:

```text
PRIVATE_KNOWLEDGE
PROTECTED_IDENTIFIER
PRIOR_PHOTO
RECEIPT
DOCUMENT
INSTITUTIONAL
POSSESSION
AUTHORIZATION
```

---

# 53. Architecture Input — Verification Must Model Entitlement, Not Only Ownership

**Classification:** `MANDATORY`

Possible entitlement:

```text
OWNER
GUARDIAN
REPRESENTATIVE
EMPLOYEE
EXECUTOR
AUTHORIZED_AGENT
```

---

# 54. Architecture Input — ClaimConflict Is First-Class

**Source:** `Lost-and-Found-Tracking-System`

**Classification:** `MANDATORY`

Competing claims must create:

```text
ClaimConflict
```

rather than merely ranking claimants.

---

# 55. Architecture Input — Competing Claims Can Freeze Recovery

**Classification:** `MANDATORY`

A serious unresolved conflict must be able to create:

```text
RecoveryHold
```

---

# 56. Architecture Input — RiskAssessment Is Separate from Verification

**Source influence:** `Lost-and-Found-Tracking-System`

**Classification:** `MANDATORY`

Separate:

```text
Does evidence support entitlement?
```

from:

```text
Does this behavior look risky?
```

---

# 57. Risk Inputs

Potential signals include:

```text
claim history
reused evidence
identifier conflicts
multiple failed challenges
unusual account/device behavior
competing claims
```

---

# 58. Architecture Input — AI Must Not Auto-Award Competing Claims

**Source negative lesson:** `Lost-and-Found-Tracking-System`

**Classification:** `PROHIBITED`

Highest AI score cannot become:

```text
automatic owner
```

---

# 59. Architecture Input — Claims Need a State Machine

**Sources:**

```text
Lost-and-Found-Tracking-System
FindBack
Malak-Rashad
```

**Classification:** `MANDATORY`

State transitions must be explicit and validated.

---

# 60. Architecture Input — Claim Approval Is Not Recovery Completion

**Sources:**

```text
Lost-and-Found-Tracking-System negative finding
SmartFind negative finding
FindBack negative finding
```

**Classification:** `MANDATORY`

Sequence:

```text
ClaimApproved
    ↓
RecoveryAuthorization
    ↓
Custody/Handover
    ↓
RecoveryCompleted
```

---

# 61. Architecture Input — RecoveryAuthorization Must Exist

**Classification:** `BUILD`

No audited source adequately models this.

Required aggregate:

```text
RecoveryAuthorization
```

---

# 62. RecoveryAuthorization Purpose

It answers:

```text
Who may receive the item?
Which claim/verification supports that?
Who authorized release?
Under which policy?
When does authorization expire?
Are there holds?
```

---

# 63. Architecture Input — Custody Must Be a Separate Bounded Context

**Classification:** `MANDATORY`

Audits repeatedly reveal the danger of conflating:

```text
approved claim
returned item
current possession
```

---

# 64. Architecture Input — Append-Only Custody Events

**Classification:** `MANDATORY`

Every physical transition should create a:

```text
CustodyEvent
```

---

# 65. Architecture Input — Current Custody Is a Projection

Use:

```text
CustodyRecord
```

for current state and:

```text
CustodyEvent[]
```

as immutable history.

---

# 66. Architecture Input — Handover Credentials Must Be Secure

**Sources:**

```text
FindBack
SmartFind
```

**Classification:** `MANDATORY`

Required properties:

```text
cryptographically random
hashed/verifier stored
single-use
expiring
attempt-limited
revocable
recovery-bound
recipient-bound where possible
```

---

# 67. Architecture Input — Handover Credential Issued Only After Recovery Authorization

**Source negative lesson:** `FindBack`

**Classification:** `MANDATORY`

Do not issue on:

```text
ClaimSubmitted
```

---

# 68. Architecture Input — Two-Sided Handover May Be Required

**Classification:** `DEFAULT`

Depending on risk level:

```text
sender confirmation
+
recipient confirmation
```

may both be required.

---

# 69. Architecture Input — Courier Booking Does Not Equal Custody Transfer

**Classification:** `MANDATORY`

Custody changes only when actual pickup occurs.

---

# 70. Architecture Input — Courier Integration Must Be Provider-Based

**Classification:** `BUILD`

Use:

```text
LogisticsProvider
```

for:

```text
quote
book
track
cancel
proof of delivery
```

---

# 71. Architecture Input — QR Recovery Is Useful but Optional

**Sources:**

```text
Smart Asset Finder
FindBack
```

**Classification:** `OPTIONAL / DEFAULT FEATURE`

QR/NFC can accelerate recovery of registered items.

---

# 72. Architecture Input — Tag Identity Must Not Reveal Owner PII

**Classification:** `MANDATORY`

A tag should resolve to:

```text
Dzoka recovery workflow
```

not expose:

```text
name
phone
home address
```

---

# 73. Architecture Input — QR Scan Is Evidence, Not Location Truth

**Classification:** `MANDATORY`

Record scan as:

```text
SightingEvidence
```

with trust level.

---

# 74. Architecture Input — ItemIdentityProvider

**Classification:** `BUILD`

Support:

```text
QR
NFC
RFID
BLE
serial
institutional tag
future DPP/DID
```

through an abstraction.

---

# 75. Architecture Input — Registered Assets

**Source:** `Smart Asset Finder`

**Classification:** `OPTIONAL`

Allow users/organizations to register objects before loss.

This creates stronger:

```text
pre-loss fingerprint
ownership evidence
identity
```

---

# 76. Architecture Input — Messaging Must Be Provider-Independent

**Sources:**

```text
Smart Asset Finder
FindBack
kwasu bot
```

**Classification:** `MANDATORY`

Use:

```text
dzoka-messaging
```

with provider adapters.

---

# 77. Architecture Input — WhatsApp Is a First-Class Interaction Channel

**Classification:** `DEFAULT`

Not merely outbound notifications.

Support where feasible:

```text
report
search
claim
verification
recovery
```

through conversational flows.

---

# 78. Architecture Input — RecoveryConversation Must Be Domain-Authorized

**Sources:**

```text
Malak negative findings
FindBack negative findings
```

**Classification:** `MANDATORY`

Conversation access requires a valid recovery relationship.

---

# 79. Architecture Input — Sender Identity Must Be Server-Derived

**Classification:** `MANDATORY`

Never trust:

```text
senderId
```

from the client as authoritative identity.

---

# 80. Architecture Input — Contact Relay Before Direct Disclosure

**Classification:** `DEFAULT`

Prefer:

```text
secure relay
```

over exposing finder/owner contact data immediately.

---

# 81. Architecture Input — Push Notifications

**Source:** `FindBack`

**Classification:** `DEFAULT`

Push is valuable for time-sensitive recovery events.

---

# 82. Architecture Input — Notification Delivery Must Be Durable

**Classification:** `MANDATORY`

Model:

```text
Notification
NotificationAttempt
```

rather than fire-and-forget provider calls.

---

# 83. Architecture Input — Notification Work Must Be Asynchronous

**Classification:** `DEFAULT`

Flow:

```text
domain event
    ↓
outbox
    ↓
messaging worker
    ↓
provider
```

---

# 84. Architecture Input — Rewards Must Be Downstream of Verified Recovery

**Sources:**

```text
Smart Asset Finder
FindBack
OpenEscrow
```

**Classification:** `MANDATORY`

Default:

```text
RecoveryCompleted
    ↓
RewardEligibility
    ↓
RewardRelease
```

---

# 85. Architecture Input — Reward Ledger

**Classification:** `BUILD`

Use append-oriented:

```text
RewardLedgerEntry
```

plus derived balance.

---

# 86. Architecture Input — Reward and Payment Are Separate Contexts

**Classification:** `MANDATORY`

Reward determines:

```text
what someone is entitled to
```

Payment determines:

```text
how money moves
```

---

# 87. Architecture Input — PaymentProvider Abstraction

**Classification:** `BUILD`

Possible providers vary by market and institution.

---

# 88. Architecture Input — Server-Authoritative Pricing

**Source negative lesson:** Smart Asset Finder

**Classification:** `MANDATORY`

Never trust client-supplied:

```text
price
cost
currency
reward value
```

---

# 89. Architecture Input — Institutions Are First-Class

**Source:** `Lost-and-Found-Tracking-System`

**Classification:** `MANDATORY`

Dzoka is not purely peer-to-peer.

Model:

```text
Tenant
Organization
OrganizationUnit
Membership
Delegation
```

---

# 90. Architecture Input — Multi-Tenancy Must Be End-to-End

**Classification:** `MANDATORY`

Tenant boundaries affect:

```text
database
vectors
media
events
cache
audit
federation
```

---

# 91. Architecture Input — Organization and Custodian Are Separate

A staff member may report an item while the organization physically controls it.

This distinction must exist.

---

# 92. Architecture Input — Institutional Policies Vary

**Classification:** `MANDATORY`

Different institutions may define:

```text
retention
claim evidence
release approvals
privacy
handover procedures
```

Use policy-driven architecture.

---

# 93. Architecture Input — Policy Engine Boundary

**Classification:** `DEFAULT / OPTIONAL IMPLEMENTATION`

Policy should be explicit.

Potential implementation:

```text
internal deterministic policy
+
optional OPA
```

---

# 94. Architecture Input — Audit Is First-Class

**Sources:**

```text
Lost-and-Found-Tracking-System
FindBack
```

**Classification:** `MANDATORY`

Important actions require durable audit records.

---

# 95. Audit Examples

```text
claim submitted
claim reviewed
verification evidence accessed
verification decided
recovery authorized
custody transferred
federation trust changed
model configuration changed
```

---

# 96. Architecture Input — Audit Must Not Blindly Copy Whole Secret Objects

**Source negative lesson:** FindBack

**Classification:** `MANDATORY`

Audit metadata must be classified and minimized.

---

# 97. Architecture Input — Retention Policy Must Be Explicit

**Source:** `Lost-and-Found-Tracking-System`

**Classification:** `MANDATORY`

Required concepts:

```text
retention class
delete
anonymize
archive
legal hold
```

---

# 98. Architecture Input — Derived Data Must Follow Source Retention

**Classification:** `MANDATORY`

Deleting/expiring source evidence may require deletion of:

```text
OCR
embeddings
thumbnails
search documents
AI artifacts
```

---

# 99. Architecture Input — Federation Must Be a Real Node Protocol

**Sources:**

```text
Mycel
Open Federated Search
```

**Classification:** `MANDATORY`

Federation does not mean:

```text
many institutions in one database
```

---

# 100. Federation Node Requirements

Each node must support:

```text
node identity
trusted keys
capabilities
endpoint
organization identity
trust state
```

---

# 101. Architecture Input — Federation Gateway

**Classification:** `MANDATORY`

External federation communication flows through:

```text
dzoka-federation
```

not directly into:

```text
database
Kafka
internal modules
```

---

# 102. Architecture Input — Federation Messages Must Be Signed

**Classification:** `MANDATORY`

Use:

```text
digital signatures
timestamp
nonce/message ID
replay protection
```

---

# 103. Architecture Input — Peer Timeouts

**Source:** `Mycel`

**Classification:** `MANDATORY`

Every peer request must have bounded latency.

---

# 104. Architecture Input — Circuit Breakers

**Source:** `Mycel`

**Classification:** `DEFAULT`

Repeatedly failing peers should temporarily stop receiving synchronous fanout.

---

# 105. Architecture Input — Remote Scores Are Non-Comparable

**Sources:**

```text
Mycel
Dzoka federation synthesis
```

**Classification:** `MANDATORY`

Do not:

```text
ORDER BY remoteRawScore DESC
```

across independent nodes.

---

# 106. Architecture Input — Federated Candidate Merge Uses Rank/Source-Aware Fusion

**Classification:** `MANDATORY`

Possible approaches:

```text
round-robin
RRF
source-aware ranking
confidence categories
```

---

# 107. Architecture Input — Federation Must Minimize PII

**Classification:** `MANDATORY`

Initial cross-node queries should share the minimum representation required to identify candidates.

---

# 108. Architecture Input — Holding Node Retains Release Authority

**Classification:** `MANDATORY`

In federated recovery:

```text
item-holding node
```

remains authoritative for:

```text
custody
verification policy
release
```

unless explicit cross-node policy says otherwise.

---

# 109. Architecture Input — Native RecoveryEvent + EPCIS Adapter

**Sources:**

```text
OpenEPCIS
GS1
```

**Classification:** `DEFAULT`

Dzoka should retain its own domain event:

```text
RecoveryEvent
```

and provide:

```text
EPCISAdapter
```

for interoperability.

---

# 110. Why EPCIS Is an Adapter

EPCIS is useful for:

```text
capture
query
traceability
external integration
```

but Dzoka has domain semantics such as:

```text
Claim
VerificationSession
RiskAssessment
```

that do not map cleanly to native EPCIS concepts.

---

# 111. Architecture Input — CCTV Processing Should Be Edge-First

**Source:** `CCTV Theft Finder`

**Classification:** `DEFAULT`

Avoid uploading continuous raw video centrally when unnecessary.

---

# 112. Architecture Input — CCTV Search Should Be Coarse-to-Fine

**Classification:** `MANDATORY`

Pipeline:

```text
large temporal interval
    ↓
coarse disappearance detection
    ↓
narrow interval
    ↓
fine frame analysis
```

---

# 113. Architecture Input — Occlusion Is Not Disappearance

**Classification:** `MANDATORY`

CCTV workflows need rechecks before declaring the item gone.

---

# 114. Architecture Input — CCTV Evidence Must Be Policy-Bound

**Classification:** `MANDATORY`

Require:

```text
authorized camera source
purpose
retention
operator permissions
case binding
```

---

# 115. Architecture Input — Face Detection vs Face Recognition

**Source lesson:** FindBack

**Classification:** `MANDATORY`

Separate:

```text
detect face to redact
```

from:

```text
identify person
```

The latter is not part of default Dzoka architecture.

---

# 116. Architecture Input — CCTV Outputs Do Not Automatically Become Custody State

**Classification:** `MANDATORY`

Video can generate:

```text
ObservationEvidence
```

not:

```text
CustodyTransfer
```

without policy/validation.

---

# 117. Architecture Input — Edge Runtime Must Be Replaceable and Offline-Tolerant

**Classification:** `DEFAULT`

Required component:

```text
dzoka-edge
```

for:

```text
CCTV
local models
sensor integrations
offline buffering
```

---

# 118. Architecture Input — AI Runtime Separate from Transactional Core

**Classification:** `MANDATORY`

Architecture:

```text
Java/Spring Boot
→ transactional domain

Python/FastAPI
→ AI/ML
```

---

# 119. Reason for Runtime Separation

Different concerns:

```text
JVM:
transactions
domain state
security
workflow

Python:
models
GPU
vision
NLP
scientific tooling
```

---

# 120. Architecture Input — Core Begins as Modular Monolith

**Classification:** `DEFAULT`

Use:

```text
dzoka-core
```

as a modular monolith.

---

# 121. Why Not Full Microservices

Audits show the recovery domain is heavily transactional.

Examples:

```text
claim approval
recovery authorization
custody transfer
holds
```

benefit from strong local consistency.

---

# 122. Architecture Input — Matching Is a Separate Logical Boundary

**Classification:** `MANDATORY`

Component:

```text
dzoka-matching
```

may initially share deployment resources but should have explicit interfaces.

---

# 123. Architecture Input — Messaging Is Separate

Component:

```text
dzoka-messaging
```

owns:

```text
providers
inbound webhooks
outbound delivery
conversation adapters
```

---

# 124. Architecture Input — Federation Is Separate

Component:

```text
dzoka-federation
```

owns:

```text
node identity
peer trust
protocol
remote search
remote verification coordination
```

---

# 125. Architecture Input — Workers Are Explicit

**Classification:** `DEFAULT`

Background work includes:

```text
fingerprint enrichment
matching
notifications
retention
federation fanout
courier polling
reward processing
```

---

# 126. Architecture Input — Event Backbone

**Classification:** `DEFAULT`

Use a:

```text
Kafka-compatible broker
```

for durable integration events.

---

# 127. ADR Input — Kafka vs Redpanda

Still requires final source audit/ADR.

---

# 128. Architecture Input — Transactional Outbox

**Classification:** `MANDATORY`

Core transaction:

```text
domain state update
+
outbox event
```

must commit atomically.

---

# 129. Architecture Input — Inbox / Deduplication

**Classification:** `MANDATORY`

Consumers must tolerate duplicate event delivery.

---

# 130. Architecture Input — No Fantasy Exactly-Once

**Classification:** `MANDATORY`

Assume:

```text
at-least-once delivery
+
idempotent processing
```

---

# 131. Architecture Input — Event Envelope

**Classification:** `MANDATORY`

All integration events should include:

```text
eventId
eventType
schemaVersion
occurredAt
producer
tenantId
aggregateType
aggregateId
aggregateVersion
correlationId
causationId
classification
payload
```

---

# 132. Architecture Input — Sensitive Media Never Travels in Kafka Payloads

**Classification:** `MANDATORY`

Use references.

Do not embed:

```text
image
receipt
passport scan
video
```

in integration events.

---

# 133. Architecture Input — Object Storage

**Classification:** `DEFAULT`

Use:

```text
S3 / MinIO
```

for binary media.

---

# 134. Architecture Input — Signed Uploads

**Classification:** `DEFAULT`

Clients should upload large media directly through:

```text
pre-signed upload flow
```

where appropriate.

---

# 135. Architecture Input — Redis Is Ephemeral

**Classification:** `MANDATORY`

Redis may contain:

```text
cache
temporary sessions
OTP state
rate limits
locks
```

but not authoritative recovery state.

---

# 136. Architecture Input — OIDC-Compatible Identity

**Classification:** `DEFAULT`

Prefer an external identity provider architecture.

Candidate:

```text
Keycloak-compatible
```

---

# 137. ADR Input — Identity Provider

Requires exact provider decision and deployment ADR.

---

# 138. Architecture Input — Resource Authorization Is Separate from Authentication

**Sources:** negative findings from multiple direct platforms

**Classification:** `MANDATORY`

Authentication:

```text
Who are you?
```

Authorization:

```text
May you perform this action on this resource?
```

---

# 139. Architecture Input — UI Visibility Is Never an Authorization Boundary

**Classification:** `MANDATORY`

Every sensitive endpoint must independently validate policy.

---

# 140. Architecture Input — Explicit Application Commands

**Classification:** `DEFAULT`

Prefer:

```text
ApproveClaim
PlaceCustodyHold
AuthorizeRecovery
ConfirmHandover
```

over:

```text
PATCH /resource
{ status: ... }
```

---

# 141. Architecture Input — Security-Sensitive IDs Derived from Principal

**Classification:** `MANDATORY`

Fields such as:

```text
actorId
reviewerId
senderId
tenantId
```

must normally come from trusted context.

---

# 142. Architecture Input — Public DTOs Must Be Privacy-Safe

**Sources:** SmartFind public contact leak

**Classification:** `MANDATORY`

Use distinct projections:

```text
PublicItemView
OwnerItemView
ReviewerItemView
FederatedItemView
```

---

# 143. Architecture Input — Proof Media Must Be Private

**Sources:** SmartFind proof-upload weakness

**Classification:** `MANDATORY`

Ownership evidence should be in private object storage with:

```text
short-lived signed access
authorization
audit
```

---

# 144. Architecture Input — Privacy Processing Produces Separate Derivatives

**Classification:** `MANDATORY`

Maintain:

```text
original
sanitized
thumbnail
OCR input
AI derivative
```

as separate media assets.

---

# 145. Architecture Input — Public Clients Receive Sanitized Derivatives

**Classification:** `DEFAULT`

Do not expose sensitive originals simply because a blurred version also exists.

---

# 146. Architecture Input — Data Classification Must Follow Evidence

**Classification:** `MANDATORY`

Classification should propagate into:

```text
media
OCR
vectors
events
federation
AI prompts
logs
exports
```

---

# 147. Architecture Input — AI Provider Input Policy

**Classification:** `BUILD`

Required:

```text
AIProviderInputPolicy
```

to decide whether:

```text
PUBLIC
RESTRICTED
SECRET
```

data can leave local trust boundaries.

---

# 148. Architecture Input — Secret Ownership Evidence Should Prefer Local/Deterministic Evaluation

**Classification:** `DEFAULT`

Do not automatically send expected secret answers to external LLM APIs.

---

# 149. Architecture Input — Provider Abstraction Everywhere External

Required abstractions include:

```text
DetectionProvider
OCRProvider
ImageEmbeddingProvider
TextEmbeddingProvider
LLMProvider
VectorSearchProvider
MessagingProvider
PaymentProvider
LogisticsProvider
ItemIdentityProvider
```

---

# 150. Why Provider Abstraction Is Mandatory

Source audits show frequent direct coupling to:

```text
UltraMsg
Cloudinary
Firebase
Claude
HuggingFace
Roboflow
```

which creates lock-in and hard-to-test business logic.

---

# 151. Architecture Input — ModelRun Is Canonical

**Classification:** `MANDATORY`

Every AI-derived artifact should reference:

```text
ModelRun
```

---

# 152. ModelRun Minimum Fields

```text
provider
model
version
configuration
input hash
output schema
startedAt
completedAt
status
```

---

# 153. Architecture Input — Model Migration Must Support Coexistence

**Source:** `globalenglish`

**Classification:** `MANDATORY`

Do not overwrite all embeddings immediately.

Allow:

```text
CURRENT
CANDIDATE
DEPRECATED
```

models during transitions.

---

# 154. Architecture Input — ML Feedback

**Classification:** `DEFAULT`

Capture:

```text
accepted match
rejected match
successful recovery
failed claim
human override
```

as structured labels where allowed.

---

# 155. Architecture Input — Evaluation Must Include Hard Negatives

**Classification:** `MANDATORY`

Examples:

```text
same phone model
same colour
same case type
different physical device
```

---

# 156. Architecture Input — Observability

**Classification:** `MANDATORY`

Use:

```text
OpenTelemetry
Prometheus
Grafana
Loki
Tempo
```

or equivalent.

---

# 157. Required Matching Metrics

Track:

```text
retrieval latency
candidate count
reranker latency
feature availability
coverage
model failures
precision
recall
MRR / ranking metrics
```

---

# 158. Required Event Metrics

Track:

```text
outbox backlog
consumer lag
retry count
DLQ size
processing latency
```

---

# 159. Architecture Input — Audit and Logs Are Different

**Classification:** `MANDATORY`

Logs:

```text
operational diagnostics
```

Audit:

```text
business/security evidence
```

---

# 160. Architecture Input — Multi-Channel UX Must Share One Domain

**Sources:** duplicated/bypassed claim workflows found in upstream apps

**Classification:** `MANDATORY`

Web, mobile, WhatsApp, kiosk and API must call the same application use cases.

---

# 161. Architecture Input — Progressive Reporting

**Classification:** `DEFAULT`

Allow incomplete reports and enrich asynchronously.

Useful for:

```text
mobile
WhatsApp
low bandwidth
kiosk
```

---

# 162. Architecture Input — Heavy AI Is Async

**Classification:** `DEFAULT`

Report creation should not wait on:

```text
OCR
vision
embeddings
federated matching
reranking
```

unless an interactive path specifically requires it.

---

# 163. Architecture Input — Fast Interactive Path + Background Deep Path

Recommended:

```text
quick initial candidate retrieval
        ↓
background enrichment/reranking
        ↓
candidate updates
```

---

# 164. Architecture Input — Capability Degradation

**Classification:** `MANDATORY`

If an optional provider is down:

```text
core reporting must continue
```

where possible.

Examples:

```text
LLM down
→ deterministic path continues

WhatsApp down
→ other channels continue

Qdrant down
→ pgvector provider where configured

remote federation node down
→ local search continues
```

---

# 165. Architecture Input — Explicit Capability Status

**Classification:** `MANDATORY`

For major subsystems record:

```text
IMPLEMENTED
PARTIAL
EXPERIMENTAL
DISABLED
PLACEHOLDER
PLANNED
```

This is directly informed by source-audit discrepancies.

---

# 166. Architecture Input — No Placeholder AI in Production

**Classification:** `PROHIBITED`

Random/fake:

```text
scores
embeddings
OCR
matches
```

must be impossible to activate in production accidentally.

---

# 167. Architecture Input — Source Audit Is a Governance Gate

**Classification:** `MANDATORY PROCESS`

Before a major dependency becomes architectural:

```text
source audit
license check
exact SHA
security review
reuse classification
ADR where necessary
```

---

# 168. Architecture Input — Exact Revision Pinning

**Classification:** `MANDATORY PROCESS`

All audited repositories must be pinned in:

```text
docs/source-audit/source-audit.lock.json
```

---

# 169. Architecture Input — Licensing Can Affect Deployment Boundaries

**Source:** licensing matrix

**Classification:** `MANDATORY`

Examples:

```text
AGPL dependency
GPL library
all-rights-reserved repository
```

may require:

```text
alternative component
optional service
independent reimplementation
```

---

# 170. Preferred Licensing Architecture

```text
PERMISSIVE DZOKA CORE
+
PERMISSIVE DEFAULT PROVIDERS
+
OPTIONAL LICENSE-AWARE ADAPTERS
```

where feasible.

---

# 171. Architecture Input — Default AI Foundation

Current preferred baseline:

```text
PaddleDetection
PaddleOCR
OpenCLIP
Dzoka local-instance pipeline
```

---

# 172. Architecture Input — Default Storage Foundation

```text
PostgreSQL
PostGIS
pgvector
pg_trgm
FTS
MinIO/S3
Redis
```

---

# 173. Architecture Input — Default Runtime Foundation

```text
Next.js + TypeScript
Java 21+ + Spring Boot
Python + FastAPI
Kafka-compatible events
```

---

# 174. Architecture Input — Deployment Profiles

Dzoka should support:

```text
Developer
Small Self-Hosted
Institution
Large HA
Federated Network
Edge
```

---

# 175. Small Deployment Profile

Potentially:

```text
Docker Compose
PostgreSQL
Redis
MinIO
core
AI
web
workers
```

with optional broker profile depending on packaging decisions.

---

# 176. Large Deployment Profile

Potentially:

```text
Kubernetes
Helm
HA PostgreSQL
Kafka-compatible broker
object storage
AI/GPU workers
autoscaling
observability stack
```

---

# 177. Architecture Input — Self-Hosted and SaaS Must Use the Same Domain Model

**Classification:** `MANDATORY`

Do not create a reduced self-hosted architecture that cannot federate or a SaaS-only domain model that assumes one operator.

---

# 178. Architecture Input — Federation Must Be Optional

A single institution must be able to run Dzoka without connecting to federation.

Federation adds:

```text
reach
```

not basic operability.

---

# 179. Architecture Input — Blockchain Is Optional, Not Baseline

**Sources:**

```text
OpenEscrow
DID/DPP research
```

**Classification:** `DEFAULT REJECTION`

Do not require:

```text
wallet
token
smart contract
chain
```

for normal lost-property recovery.

---

# 180. Architecture Input — DID/VC Is Future Provider Capability

**Classification:** `FUTURE`

Architect:

```text
ItemIdentityProvider
CredentialProvider
```

so DID/VC can be added later.

---

# 181. Architecture Input — Standards Integration Through Adapters

**Classification:** `MANDATORY`

Examples:

```text
EPCIS
DID
courier APIs
payment APIs
WhatsApp
```

must not shape Dzoka's core domain objects directly.

---

# 182. Source-to-Architecture Mapping — globalenglish01/lostfound

Primary inputs:

```text
raw evidence preservation
attribute provenance
secret attributes
hybrid retrieval
RRF
hard conflicts
missing != mismatch
evidence coverage
versioned models
human decisions
```

### Affected Dzoka components

```text
ItemFingerprint
Matching Engine
Data Architecture
AI Architecture
Model Governance
```

### Reuse

```text
ADAPT HEAVILY
```

---

# 183. Source-to-Architecture Mapping — FuricBond

Primary inputs:

```text
object-aware cropping
OpenCLIP
pHash
ORB
multi-signal visual matching
mutual confirmation
```

### Affected components

```text
Perception
ItemFingerprint
Matching
Verification UX
```

### Reuse

```text
REFERENCE / ADAPT CONCEPTS
```

---

# 184. Source-to-Architecture Mapping — Lost-and-Found-Tracking-System

Primary inputs:

```text
organizations
GeoJSON zones
claim conflicts
claim decisions
approval chains
fraud signals
audit
retention
AI configuration
```

### Affected components

```text
Multi-Tenancy
Claims
Risk
Audit
Data Governance
Location
```

### Reuse

```text
ADAPT
```

---

# 185. Source-to-Architecture Mapping — Malak-Rashad/LostAndFound

Primary inputs:

```text
private finder questions
human comparison
claim review
communication after approval
```

### Affected components

```text
Verification
Claims
Messaging
Privacy
```

### Reuse

```text
REFERENCE / ADAPT CONCEPTS
```

---

# 186. Source-to-Architecture Mapping — Smart Asset Finder

Primary inputs:

```text
QR tags
anonymous finder
WhatsApp alerts
registered items
travel context
rewards
hardware ecosystem
```

### Affected components

```text
Item Identity
Messaging
Rewards
Location
Product UX
```

### Reuse

```text
REFERENCE
```

---

# 187. Source-to-Architecture Mapping — FindBack

Primary inputs:

```text
push notifications
claim/proof structure
handover credential idea
police verification
reputation
mobile UX
```

### Affected components

```text
Notifications
Claims
Verification
Recovery
Trust
```

### Reuse

```text
ADAPT SELECTIVELY
```

---

# 188. Source-to-Architecture Mapping — SmartFind

Primary value:

```text
workflow presentation
score breakdown concept
campus location ontology
negative implementation lessons
```

### Affected components

```text
Matching explainability
Campus location
Security tests
Source audit governance
```

### Reuse

```text
REFERENCE
```

---

# 189. Source-to-Architecture Mapping — ILIAS

Primary inputs:

```text
instance retrieval
feature extractor evaluation
query expansion
retrieval research
```

### Affected components

```text
Matching
Visual Reranking
Evaluation
```

---

# 190. Source-to-Architecture Mapping — Patchwise Retrieval

Primary input:

```text
local multi-scale patch embeddings
```

### Affected components

```text
ItemFingerprint
Visual Reranking
```

---

# 191. Source-to-Architecture Mapping — OpenCLIP

Primary input:

```text
global visual embeddings
```

### Component

```text
ImageEmbeddingProvider
```

### Reuse

```text
ADOPT
```

---

# 192. Source-to-Architecture Mapping — pgvector

Primary input:

```text
vector search inside PostgreSQL
```

### Component

```text
VectorSearchProvider
```

### Reuse

```text
ADOPT DEFAULT
```

---

# 193. Source-to-Architecture Mapping — Qdrant

Primary input:

```text
specialized vector retrieval at scale
```

### Component

```text
VectorSearchProvider alternative
```

### Reuse

```text
OPTIONAL
```

---

# 194. Source-to-Architecture Mapping — PaddleOCR

Primary input:

```text
OCR
```

### Component

```text
OCRProvider
```

### Reuse

```text
ADOPT
```

---

# 195. Source-to-Architecture Mapping — PaddleDetection

Primary inputs:

```text
detection
tracking
RT-DETR
MOT
```

### Components

```text
DetectionProvider
Edge vision
Media enrichment
```

---

# 196. Source-to-Architecture Mapping — CCTV Theft Finder

Primary inputs:

```text
coarse-to-fine disappearance search
empty-spot calibration
occlusion handling
clips
```

### Component

```text
dzoka-edge
CCTV analysis
```

---

# 197. Source-to-Architecture Mapping — Trajectron++

Primary input:

```text
probabilistic movement modeling research
```

### Component

```text
Movement Intelligence
```

### Reuse

```text
RESEARCH
```

---

# 198. Source-to-Architecture Mapping — Mycel

Primary inputs:

```text
peer fanout
timeouts
circuit breakers
allowlists
source attribution
rank-aware merging
```

### Component

```text
dzoka-federation
```

### Reuse

```text
REFERENCE / ADAPT CONCEPTS
```

---

# 199. Source-to-Architecture Mapping — Open Federated Search

Primary value:

```text
federation concepts
historical design reference
```

### Reuse

```text
REFERENCE
```

---

# 200. Source-to-Architecture Mapping — OpenEPCIS

Primary input:

```text
capture/query traceability standard implementation
```

### Component

```text
EPCISAdapter
```

---

# 201. Source-to-Architecture Mapping — OpenEscrow

Primary inputs:

```text
dispute state
evidence
release state machine
```

### Components

```text
Reward
Recovery
Dispute
```

### Reuse

```text
REFERENCE
```

---

# 202. Architecture Decisions Already Strongly Supported by Audits

The following decisions now have significant source-audit support:

```text
PostgreSQL as authoritative DB

PostGIS for geometry

pgvector as default vector provider

Qdrant optional

OpenCLIP-compatible image embeddings

PaddleOCR

PaddleDetection

Java modular transactional core

Python AI runtime

hybrid retrieval

hard conflict engine

private ownership challenges

claim conflict model

formal custody state machine

event-driven async processing

provider abstractions

federation gateway

native RecoveryEvent + EPCIS adapter
```

---

# 203. Architecture Decisions Still Requiring Dedicated Audit / ADR

The following remain unresolved enough to deserve explicit review:

```text
Kafka vs Redpanda

Keycloak exact deployment pattern

OPA adoption

MinIO exact role vs cloud S3

Redis topology

MLflow adoption

STT provider / faster-whisper

translation provider

LLM provider strategy

local-instance visual model choice

search schema format

event serialization format

UUID strategy

graph database threshold

courier provider architecture

payment provider architecture

OSRM vs Valhalla
```

---

# 204. Candidate ADR — PostgreSQL

```text
ADR:
Use PostgreSQL as Dzoka's authoritative operational database.
```

### Inputs

```text
strong relational workflows
PostGIS
pgvector
FTS
transactional outbox
low operational complexity
```

---

# 205. Candidate ADR — pgvector

```text
ADR:
Use pgvector as the default V1 vector backend.
```

### Inputs

```text
good integration
lower operational burden
Qdrant remains provider option
```

---

# 206. Candidate ADR — Modular Monolith

```text
ADR:
Use a Spring Boot modular monolith for transactional core domains.
```

### Inputs

```text
strong transactional relationships
avoid premature distributed consistency
separate AI/edge/federation where boundaries justify it
```

---

# 207. Candidate ADR — AI Runtime

```text
ADR:
Use Python/FastAPI for AI and model-serving workloads.
```

---

# 208. Candidate ADR — OpenCLIP

```text
ADR:
Use OpenCLIP-compatible embeddings as the default global image representation.
```

with explicit recognition that local-instance evidence is separately required.

---

# 209. Candidate ADR — PaddleOCR

```text
ADR:
Use PaddleOCR as the default OCR provider.
```

---

# 210. Candidate ADR — PaddleDetection

```text
ADR:
Use PaddleDetection as default detector/tracker provider.
```

---

# 211. Candidate ADR — Kafka-Compatible Broker

```text
ADR:
Use Kafka-compatible durable messaging for integration events.
```

Exact product remains to be locked.

---

# 212. Candidate ADR — Transactional Outbox

```text
ADR:
All core integration events originate through a transactional outbox.
```

---

# 213. Candidate ADR — Matching/Ownership Boundary

```text
ADR:
Matching may never directly authorize ownership or item release.
```

This should be treated almost as an architectural invariant rather than optional implementation preference.

---

# 214. Candidate ADR — Protected Evidence

```text
ADR:
Private ownership evidence is classified separately from public matching evidence.
```

---

# 215. Candidate ADR — Recovery Authorization

```text
ADR:
Claim approval does not directly release an item; RecoveryAuthorization is required.
```

---

# 216. Candidate ADR — Custody

```text
ADR:
Physical custody is modeled through append-only CustodyEvents and a current-state projection.
```

---

# 217. Candidate ADR — Federation Score Semantics

```text
ADR:
Raw match scores from independent federation nodes are never assumed globally comparable.
```

---

# 218. Candidate ADR — EPCIS

```text
ADR:
Dzoka uses native RecoveryEvent semantics and integrates EPCIS through an adapter.
```

---

# 219. Candidate ADR — Blockchain

```text
ADR:
Blockchain is not part of Dzoka's default recovery architecture.
```

---

# 220. Candidate ADR — Graph Storage

```text
ADR:
Recovery graph is implemented relationally first.
```

---

# 221. Candidate ADR — Face Recognition

```text
ADR:
Biometric face identification is excluded from the default Dzoka recovery pipeline.
```

Face detection/redaction remains separately allowed.

---

# 222. Candidate ADR — Handover Credentials

```text
ADR:
Handover credentials must be cryptographically generated, single-use, expiring, and issued only after recovery authorization.
```

---

# 223. Candidate ADR — Provider Abstraction

```text
ADR:
External providers are accessed through Dzoka-owned ports/interfaces.
```

---

# 224. Architecture Inputs by Dzoka Container

## `dzoka-web`

Inputs:

```text
PWA
progressive reporting
privacy-safe public projections
recovery timeline
low-bandwidth UX
multi-view capture
```

---

# 225. `dzoka-core`

Inputs:

```text
ItemRecord
Reports
Claims
Verification decisions
RecoveryAuthorization
Custody
Organizations
Policy
Audit
Outbox
```

---

# 226. `dzoka-ai`

Inputs:

```text
OCR
detection
embeddings
attribute extraction
translation
speech
LLM assistance
model provenance
```

---

# 227. `dzoka-matching`

Inputs:

```text
retrieval
rank fusion
conflicts
feature scoring
coverage
reranking
confidence
explanations
```

---

# 228. `dzoka-messaging`

Inputs:

```text
WhatsApp
push
email
SMS
conversation adapters
delivery attempts
```

---

# 229. `dzoka-federation`

Inputs:

```text
node identity
peer trust
signatures
federated queries
timeouts
circuit breakers
remote candidate normalization
```

---

# 230. `dzoka-edge`

Inputs:

```text
CCTV
local models
sensor adapters
offline buffers
privacy processing
```

---

# 231. `dzoka-workers`

Inputs:

```text
AI enrichment
matching jobs
notifications
retention
federation jobs
courier polling
reward processing
re-embedding
```

---

# 232. Architecture Inputs by Data Store

## PostgreSQL

Owns:

```text
canonical transactional state
claims
verification metadata
custody
organizations
audits
outbox
model metadata
matching metadata
```

---

# 233. PostGIS

Owns:

```text
geometries
zones
locations
spatial operations
```

---

# 234. pgvector

Owns default:

```text
versioned vector indexes
```

under provider abstraction.

---

# 235. Object Storage

Owns:

```text
images
videos
receipts
proof
CCTV clips
sanitized derivatives
```

---

# 236. Redis

Owns temporary:

```text
cache
OTP state
short locks
rate limits
ephemeral sessions
```

---

# 237. Kafka-Compatible Broker

Carries:

```text
integration events
background jobs/events
process coordination
```

not canonical business truth.

---

# 238. Architecture Inputs by Security Boundary

## Public Internet Boundary

Must expose:

```text
privacy-safe DTOs
rate limits
strong auth where required
signed upload workflows
```

---

# 239. AI Provider Boundary

Must apply:

```text
data minimization
classification checks
provider policy
redaction
```

---

# 240. Federation Boundary

Must apply:

```text
node auth
signatures
replay defense
schema validation
privacy minimization
```

---

# 241. Edge Boundary

Must apply:

```text
device identity
local authorization
buffer security
evidence provenance
```

---

# 242. Payment / Logistics Boundary

Must apply:

```text
webhook verification
idempotency
provider abstraction
reconciliation
```

---

# 243. Architecture Inputs by Domain Invariant

The audits collectively support the following permanent invariants.

---

# 244. Invariant — Matching ≠ Ownership

No match score authorizes item release.

---

# 245. Invariant — Claim ≠ Ownership

A claim is an assertion.

---

# 246. Invariant — Verification ≠ Custody

Verification establishes entitlement confidence, not physical location.

---

# 247. Invariant — Custody ≠ Ownership

Possession is not ownership.

---

# 248. Invariant — Claim Approval ≠ Recovery

Recovery requires actual custody/handover completion.

---

# 249. Invariant — Missing Evidence ≠ Mismatch

Unknown must remain unknown.

---

# 250. Invariant — Missing Evidence ≠ Synthetic Evidence

Do not fabricate scores.

---

# 251. Invariant — AI Output ≠ Ground Truth

Every AI output retains provenance and uncertainty.

---

# 252. Invariant — Prediction ≠ Fact

Movement predictions never overwrite observed state.

---

# 253. Invariant — Remote Candidate ≠ Local Truth

Federated evidence remains source-attributed.

---

# 254. Invariant — UI Permission ≠ Authorization

Server/domain policy decides.

---

# 255. Invariant — Notification ≠ Domain Event

Notifications are user communication derived from domain events.

---

# 256. Invariant — Chat ≠ Verification

Communication cannot substitute for ownership evidence.

---

# 257. Invariant — QR Scan ≠ Custody

A scan is an observation.

---

# 258. Invariant — Courier Delivery ≠ Recovery Confirmation

Recipient confirmation/policy may still be required.

---

# 259. Invariant — Reward ≠ Recovery Decision

Rewards consume recovery truth; they do not determine it.

---

# 260. Invariant — Broker ≠ Audit Database

Kafka-like infrastructure is not permanent business history.

---

# 261. Invariant — Search Index ≠ Canonical Store

Search projections are rebuildable.

---

# 262. Invariant — Model Upgrade ≠ Historical Rewrite

Past decisions retain past model/config evidence.

---

# 263. Invariant — Provider Failure ≠ Negative Evidence

A failed AI call means:

```text
FAILED
```

not:

```text
NO_MATCH
```

---

# 264. Invariant — Data Classification Follows Derived Data

An OCR/vector/thumbnail derived from secret evidence may itself remain sensitive.

---

# 265. Architecture Inputs for Testing

Audits reveal several mandatory test classes.

---

# 266. Matching Tests

Must verify:

```text
missing evidence behavior
hard conflicts
deterministic scoring
embedding version compatibility
candidate recall
coverage
reranker bounds
```

---

# 267. Verification Tests

Must verify:

```text
secret answer isolation
challenge leakage
attempt controls
human authorization
competing claims
AI non-authority
```

---

# 268. Claims Tests

Must verify:

```text
valid transitions
duplicate submissions
withdrawal
conflicts
unauthorized decisions
```

---

# 269. Custody Tests

Must verify:

```text
version conflicts
holds
handover token expiry
single-use behavior
unauthorized transfers
offline conflicts
```

---

# 270. Federation Tests

Must verify:

```text
signature verification
replay rejection
peer timeout
circuit breaker
privacy minimization
remote score handling
```

---

# 271. Security Tests

Must verify:

```text
cross-tenant isolation
actor identity derivation
role escalation prevention
proof evidence access
public DTO redaction
```

---

# 272. Provider Failure Tests

Must simulate:

```text
OCR unavailable
LLM unavailable
vector provider unavailable
WhatsApp unavailable
courier unavailable
remote peer unavailable
```

---

# 273. Architecture Inputs for Documentation

Each major design document should show:

```text
invariants
ownership
interfaces
failure modes
security boundaries
source-audit influences
```

---

# 274. Architecture Inputs for AGENTS.md

Coding agents must be instructed that they may not:

```text
bypass state machines
invent provider results
use placeholder AI in production
merge claim and recovery state
expose private evidence
copy unlicensed source
```

---

# 275. Architecture Inputs for STATUS.md

Capability status must be explicit:

```text
IMPLEMENTED
PARTIAL
EXPERIMENTAL
PLACEHOLDER
PLANNED
BLOCKED
```

---

# 276. Architecture Inputs for ROADMAP.md

Roadmap stages should follow architectural dependencies.

Example:

```text
foundation
→ reporting
→ fingerprint
→ matching
→ claims
→ verification
→ recovery
→ custody
→ messaging
→ federation
→ CCTV
→ advanced movement intelligence
```

without reducing any required final capability to vague “future work.”

---

# 277. Architecture Inputs for SECURITY.md

Must explicitly document:

```text
private evidence
protected identifiers
tenant isolation
handover credentials
federation trust
provider webhooks
media authorization
AI data boundaries
```

---

# 278. Architecture Inputs for Privacy Documentation

Required topics:

```text
public vs restricted vs secret evidence
biometric policy
CCTV retention
media derivatives
OCR PII
federation minimization
AI-provider disclosure
retention/deletion
```

---

# 279. Architecture Inputs for Model Governance

Required concepts:

```text
model registry
model versions
input/output provenance
evaluation
promotion
rollback
re-embedding
shadow tests
```

---

# 280. Architecture Inputs for Operations

Required operational tooling:

```text
backup
restore
DLQ replay
outbox monitoring
re-embedding jobs
failed-provider recovery
federation peer disable
model rollback
credential rotation
```

---

# 281. Architecture Inputs for Disaster Recovery

Canonical state should be recoverable from:

```text
PostgreSQL backups
object storage backups
configuration
model metadata
```

Derived:

```text
vectors
search projections
caches
```

should be rebuildable.

---

# 282. Architecture Inputs for Offline Operation

Offline-capable clients/edge nodes must not use generic:

```text
last-write-wins
```

for:

```text
claims
custody
recovery
```

---

# 283. Offline Sync Input

Conflicts require domain semantics.

Example:

```text
one node says custody moved to courier
another says release occurred locally
```

requires review, not timestamp overwrite.

---

# 284. Architecture Inputs for Low-Bandwidth Environments

Dzoka should support:

```text
compressed media
progressive upload
resumable reporting
WhatsApp
deferred AI
server-side processing
```

---

# 285. Architecture Inputs for Localization

Support:

```text
multilingual text
voice
local identifiers
local place names
local payment/logistics providers
```

without changing domain semantics.

---

# 286. Architecture Input — Multilingual Normalization

Text should preserve:

```text
raw language
detected language
translated representation
normalized representation
```

with provenance.

---

# 287. Architecture Input — Voice as Evidence Source

Voice transcription should become:

```text
derived evidence
```

with original audio retained only according to policy.

---

# 288. Architecture Inputs for Accessibility

Multiple channels should support:

```text
low literacy
screen readers
voice entry
large-text kiosk modes
```

where feasible.

---

# 289. Architecture Input — Kiosks Must Not Become Trusted Actors Automatically

Kiosk location/device may provide context but user identity/authority still requires explicit verification where needed.

---

# 290. Architecture Input — Public Developers/API Consumers

External API access should be scoped through:

```text
OAuth/OIDC
API client identity
tenant scope
rate limits
```

not shared public credentials.

---

# 291. Architecture Input — API Idempotency

Critical external commands should accept idempotency keys.

Examples:

```text
submit report
submit claim
create shipment
process payment
confirm handover
```

---

# 292. Architecture Input — Webhook Normalization

Vendor callbacks should become normalized Dzoka events/commands.

Do not expose domain modules directly to arbitrary vendor schemas.

---

# 293. Architecture Input — Webhook Security

Where providers support signatures:

```text
verify them
```

and include replay defense where appropriate.

---

# 294. Architecture Input — Secrets Management

No production secret fallback values.

Use:

```text
environment
container secret
Kubernetes Secret / external secret manager
```

depending on deployment.

---

# 295. Architecture Input — Production Startup Must Fail Safely

If a security-critical provider is required and misconfigured:

```text
fail configuration
```

rather than silently use test credentials or bypass verification.

---

# 296. Architecture Input — Development Bypasses Must Be Structurally Isolated

Demo:

```text
fake payment
fake AI
static OTP
```

must not be switchable accidentally in production.

---

# 297. Architecture Input — Source Audit Findings Become Tests

Every serious upstream anti-pattern should produce either:

```text
architecture invariant
test
lint/static rule
review checklist
```

where practical.

---

# 298. Architecture Input — Rejected Patterns Become Coding Constraints

Examples:

```text
no Math.random for security tokens
no raw secret evidence in events
no public proof media
no score 50 for missing feature
no direct claim status patch
```

---

# 299. Architecture Input — Licensing Matrix Becomes Dependency Policy

Dependencies marked:

```text
UNRESOLVED
ALL_RIGHTS_RESERVED
```

must never silently enter production source.

---

# 300. Architecture Input — SBOM

Production builds should eventually produce:

```text
CycloneDX
or
SPDX
```

software bills of materials.

---

# 301. Architecture Input — AI Artifact Inventory

Future SBOM-like inventory should include:

```text
model weights
tokenizers
datasets
prompts
evaluation sets
```

where relevant.

---

# 302. Final Architecture Baseline

The source audits currently support the following baseline:

```text
Clients
├── Web/PWA
├── Mobile
├── WhatsApp
├── Kiosk
└── API
      │
      ▼
API / Application Boundary
      │
      ├───────────────┐
      ▼               ▼
dzoka-core       dzoka-messaging
      │
      ├───────────────┐
      ▼               ▼
dzoka-matching   dzoka-federation
      │
      ▼
dzoka-ai
      │
      ▼
AI Providers
      │
      └───────────────┐
                      ▼
                  dzoka-edge
```

with infrastructure:

```text
PostgreSQL
├── PostGIS
├── pgvector
├── pg_trgm
└── FTS

S3 / MinIO
Redis
Kafka-compatible broker
OpenTelemetry stack
```

---

# 303. Final Domain Pipeline

```text
REPORT
   ↓
MEDIA
   ↓
PERCEPTION
   ↓
ITEM FINGERPRINT
   ↓
HYBRID RETRIEVAL
   ↓
RANK FUSION
   ↓
HARD CONFLICTS
   ↓
FEATURE EVIDENCE
   ↓
COVERAGE + RERANK
   ↓
MATCH CANDIDATE
   ↓
CLAIM
   ↓
PRIVATE VERIFICATION
   ↓
RISK
   ↓
POLICY
   ↓
CLAIM DECISION
   ↓
RECOVERY AUTHORIZATION
   ↓
CUSTODY
   ↓
HANDOVER / LOGISTICS
   ↓
RECOVERY
   ↓
REWARD / LEARNING / TRACEABILITY
```

---

# 304. Final Federation Pipeline

```text
LOCAL QUERY
    ↓
LOCAL MATCHING
    +
FEDERATED QUERY
    ↓
TRUSTED PEERS
    ↓
REMOTE CANDIDATES
    ↓
SOURCE-AWARE FUSION
    ↓
LOCAL REVIEW
    ↓
CROSS-NODE CLAIM / VERIFICATION
    ↓
HOLDING NODE RELEASE POLICY
```

---

# 305. Final AI Pipeline

```text
MEDIA
   ↓
DETECTION
   ↓
OCR
   ↓
GLOBAL EMBEDDING
   ↓
LOCAL VISUAL EVIDENCE
   ↓
ATTRIBUTE EXTRACTION
   ↓
ITEM FINGERPRINT
```

with:

```text
LLM
```

limited to appropriate tasks such as:

```text
interpretation
reranking
challenge wording
summarization
```

not authoritative recovery decisions.

---

# 306. Final Recovery Pipeline

```text
ClaimApproved
    ↓
VerificationSatisfied
    ↓
RiskPolicySatisfied
    ↓
RecoveryAuthorized
    ↓
HandoverCredential
    ↓
Physical Transfer
    ↓
CustodyEvent
    ↓
RecipientConfirmation
    ↓
RecoveryCompleted
```

---

# 307. Final Trust Pipeline

```text
Authentication
    ↓
Authorization
    ↓
Evidence
    ↓
Verification
    ↓
Risk
    ↓
Policy
    ↓
Human Authority where required
    ↓
Recovery Authorization
```

---

# 308. Architecture Inputs That Are Now Non-Negotiable

The current audits make the following requirements non-negotiable:

```text
Matching ≠ ownership

Claim ≠ ownership

Claim approval ≠ physical recovery

Missing evidence ≠ mismatch

Missing evidence ≠ fake score

AI cannot authorize release

Private ownership evidence is non-public

Protected identifiers require special storage

Hard conflicts can override similarity

Every model output has provenance

Recovery requires explicit custody semantics

Handover credentials use secure randomness

Federated node scores are non-comparable by default

Provider integrations remain replaceable

Critical workflows are idempotent

Tenant isolation applies everywhere

Source audit precedes major source reuse
```

---

# 309. Architecture Inputs Still Open for Change

These remain implementation choices rather than fixed invariants:

```text
Kafka vs Redpanda
JSON Schema vs Avro vs Protobuf
Keycloak deployment details
OPA adoption
MLflow adoption
Qdrant activation threshold
local-instance visual model
STT implementation
translation provider
routing engine
exact Kubernetes topology
```

---

# 310. Future Source Audit Input Procedure

Every future audit should update this file only if it creates a material architecture input.

The sequence is:

```text
source audit
    ↓
finding
    ↓
reusable/rejected classification
    ↓
licensing status
    ↓
architecture implication
    ↓
architecture-inputs.md
    ↓
ADR / architecture doc / design doc
```

---

# 311. Avoiding Architecture Drift

When implementation work conflicts with this document:

```text
implementation should not silently win
```

Instead:

```text
review finding
review architecture
create ADR if changing decision
update affected docs
then implement
```

---

# 312. Traceability Requirement

Major architecture decisions should be traceable backward to:

```text
source findings
requirements
security needs
operational constraints
```

and forward to:

```text
design
code
tests
deployment
```

---

# 313. Final Architecture Principle

The audits show that no single upstream repository solves Dzoka.

The strongest source-based architecture therefore comes from combining proven ideas without inheriting their weaknesses.

The pattern is:

```text
GLOBALENGLISH
→ evidence-aware matching

FURICBOND / ILIAS / PATCHWISE
→ instance-level visual identity

LOST-AND-FOUND-TRACKING-SYSTEM
→ institutional claims and risk

MALAK / LA2EET
→ private ownership challenges

SMART ASSET FINDER
→ QR, messaging and incentives

FINDBACK
→ mobile recovery and notifications

CCTV THEFT FINDER
→ edge temporal search

MYCEL
→ federation resilience

OPENEPCIS
→ traceability interoperability
```

combined through Dzoka-owned domain semantics.

---

# 314. Final Conclusion

The source-audit programme leads to one clear architectural position:

> **Dzoka should reuse infrastructure aggressively, reuse patterns selectively, preserve evidence faithfully, and own the domain semantics that determine identity, ownership, authorization, custody, recovery, and trust.**

The architecture must therefore remain:

```text
evidence-aware
provider-independent
privacy-aware
federation-ready
auditable
model-versioned
tenant-isolated
recovery-centric
```

and must never degrade into:

```text
listing app
+
one AI similarity score
+
claim button
+
contact exchange
```

The architectural chain that now governs Dzoka is:

```text
OBSERVATION
    ↓
EVIDENCE
    ↓
FINGERPRINT
    ↓
MATCH
    ↓
CLAIM
    ↓
VERIFY
    ↓
AUTHORIZE
    ↓
TRANSFER
    ↓
RECOVER
    ↓
LEARN
    ↓
FEDERATE
```

Every major Dzoka implementation decision should remain consistent with that chain unless an explicit ADR intentionally changes it.