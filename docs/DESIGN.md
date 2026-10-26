> **Current architecture authority (2026-10-26):** [Accepted baseline](architecture/baseline.md) and its linked ADRs govern implementation. This supplied document is preserved below as detailed research/design material. Where examples or diagrams conflict with the baseline, use the baseline; no runtime capability is implied.

# Dzoka Design

> **Dzoka — The Open Recovery Network**
>
> Implementation design for an open-source, federated, multimodal AI platform for finding, matching, verifying, tracking, and recovering lost physical property.

---

# 1. Purpose

This document defines **how Dzoka is implemented**.

`ARCHITECTURE.md` defines the shape, boundaries, major systems, invariants, and deployment philosophy of Dzoka.

`DESIGN.md` defines the concrete design used to implement that architecture.

This document specifies:

- internal module design;
- application flows;
- service interfaces;
- domain contracts;
- provider abstractions;
- matching algorithms;
- fingerprint construction;
- ownership verification;
- fraud evaluation;
- custody state machines;
- recovery workflows;
- event processing;
- federation protocol behaviour;
- CCTV processing;
- movement intelligence;
- persistence patterns;
- API conventions;
- failure handling;
- testing requirements;
- implementation constraints.

It is intended to be directly useful to:

- human contributors;
- Codex and other coding agents;
- reviewers;
- maintainers;
- security auditors.

---

# 2. Design Objectives

The implementation must make it possible for a user to:

```text
REPORT ONCE
    ↓
Dzoka understands the report
    ↓
constructs an ItemFingerprint
    ↓
searches locally
    ↓
searches trusted federated nodes when permitted
    ↓
ranks likely same-object candidates
    ↓
explains why they match
    ↓
verifies ownership privately
    ↓
coordinates custody and return
    ↓
confirms recovery
    ↓
learns from the outcome
```

The implementation must remain usable:

- on one self-hosted server;
- in a shared SaaS deployment;
- in an institutional deployment;
- in a geographically distributed federation;
- with local AI;
- with cloud AI;
- with or without CCTV;
- with or without courier integration;
- with or without rewards.

---

# 3. Non-Negotiable Design Rules

The following are implementation invariants.

## D-001 — Matching is not ownership

Never write code where:

```text
highSimilarity == ownerVerified
```

These are separate workflows.

---

## D-002 — Missing evidence is not mismatch

Incorrect:

```text
if image == null:
    imageScore = 0
```

Correct:

```text
if image == null:
    imageScore = unavailable
```

Unavailable evidence is excluded from the weighted denominator.

---

## D-003 — Raw evidence is immutable

Never overwrite:

```text
rawDescription
rawClaimAnswer
rawTranscript
rawLocationExpression
```

with normalized AI output.

Store derived representations separately.

---

## D-004 — Hard conflicts override weak similarity

A candidate must not receive a high-confidence result when evidence contains a critical contradiction such as:

```text
different IMEI
different serial
different passport/document identity
impossible chronology
```

---

## D-005 — AI is evidence-producing infrastructure

AI may:

```text
detect
extract
rank
describe
classify
translate
generate challenge suggestions
interpret responses
```

AI may not independently:

```text
approve ownership
release property
transfer custody
release reward
```

---

## D-006 — Every inference is versioned

Every material inference must reference:

```text
provider
model
modelVersion
configurationVersion
promptVersion where applicable
inputHash
generatedAt
```

---

## D-007 — Domain code must not depend on vendors

Core business logic must not contain direct dependencies on:

```text
Meta
Twilio
OpenAI
Anthropic
Qdrant
specific courier
specific payment gateway
```

Vendor code belongs behind adapters.

---

## D-008 — Remote federation records remain remote

Receiving a remote candidate does not make the remote object a local authoritative item.

---

## D-009 — Historical decisions are reproducible

The system must retain enough evidence to reproduce why a candidate, verification, or recovery decision was made.

---

# 4. Repository Structure

Target structure:

```text
dzoka/
│
├── apps/
│   ├── web/
│   ├── core/
│   ├── ai/
│   ├── matching/
│   ├── messaging/
│   ├── federation/
│   └── edge/
│
├── workers/
│   ├── enrichment/
│   ├── matching/
│   ├── notifications/
│   ├── logistics/
│   ├── rewards/
│   ├── retention/
│   └── ml-feedback/
│
├── packages/
│   ├── contracts/
│   ├── event-schemas/
│   ├── federation-protocol/
│   ├── item-fingerprint/
│   └── shared-types/
│
├── infrastructure/
│   ├── docker/
│   ├── helm/
│   ├── migrations/
│   └── monitoring/
│
├── models/
│   ├── manifests/
│   ├── evaluations/
│   └── configs/
│
├── docs/
│   ├── architecture/
│   ├── design/
│   ├── source-audit/
│   ├── adr/
│   ├── api/
│   ├── federation/
│   ├── security/
│   ├── privacy/
│   ├── operations/
│   └── runbooks/
│
├── ARCHITECTURE.md
├── DESIGN.md
├── AGENTS.md
├── STATUS.md
├── ROADMAP.md
├── SECURITY.md
└── README.md
```

---

# 5. Core Backend Design

Recommended:

```text
Java 21+
Spring Boot
```

`dzoka-core` is a modular monolith.

Recommended module pattern:

```text
module/
├── domain/
├── application/
├── ports/
├── adapters/
└── api/
```

Example:

```text
claim/
├── domain/
│   ├── Claim.java
│   ├── ClaimStatus.java
│   └── ClaimEvidence.java
├── application/
│   ├── SubmitClaimService.java
│   └── ReviewClaimService.java
├── ports/
│   ├── ClaimRepository.java
│   └── VerificationPort.java
├── adapters/
│   └── persistence/
└── api/
    └── ClaimController.java
```

---

# 6. Dependency Direction

The intended dependency direction is:

```text
API
 ↓
Application
 ↓
Domain
 ↓
Port
 ↓
Adapter
```

Forbidden:

```text
Domain
 ↓
Spring MVC

Domain
 ↓
Kafka client

Domain
 ↓
Meta SDK

Domain
 ↓
Qdrant SDK
```

---

# 7. Item Model

A physical item is represented by:

```text
ItemRecord
```

Key fields:

```text
itemId
tenantId
status
category
brand
model
registrationState
currentFingerprintId
createdAt
updatedAt
version
```

The item is not the user's report.

---

# 8. Lost and Found Reports

Reports are observations.

## LostReport

```text
LostReport
├── reportId
├── caseId
├── itemId
├── reporterId
├── rawDescription
├── normalizedDescription
├── originalLanguage
├── locationExpression
├── resolvedLocations[]
├── lostTimeWindow
├── media[]
├── channel
├── status
└── provenance
```

## FoundReport

```text
FoundReport
├── reportId
├── caseId
├── itemId
├── finderReference
├── organizationId
├── rawDescription
├── normalizedDescription
├── foundLocation
├── foundTime
├── storageLocation
├── media[]
├── status
└── provenance
```

---

# 9. ItemFingerprint Design

`ItemFingerprint` is the canonical machine-readable identity abstraction.

```text
ItemFingerprint
│
├── identity
│   ├── category
│   ├── subcategory
│   ├── brand
│   ├── model
│   ├── variant
│   ├── colour
│   ├── material
│   └── dimensions
│
├── globalVisual
│   ├── embeddings[]
│   └── perceptualHashes[]
│
├── localVisual
│   ├── patchEmbeddings[]
│   ├── distinctiveRegions[]
│   ├── scratches[]
│   ├── stickers[]
│   ├── cracks[]
│   ├── wearPatterns[]
│   └── textureFeatures[]
│
├── text
│   ├── rawDescription
│   ├── normalizedDescription
│   ├── canonicalDescription
│   ├── embeddings[]
│   └── keywords[]
│
├── extracted
│   ├── ocr[]
│   ├── logos[]
│   ├── labels[]
│   └── structuredAttributes[]
│
├── protectedIdentity
│   ├── serialHashes[]
│   ├── imeiHashes[]
│   ├── documentHashes[]
│   └── secretAttributes[]
│
├── context
│   ├── locations[]
│   ├── timeWindows[]
│   ├── environment
│   ├── route
│   └── provenance
│
└── provenance
    ├── sources[]
    ├── models[]
    ├── version
    └── generatedAt
```

---

# 10. Fingerprint Versioning

Fingerprints are immutable.

If evidence changes:

```text
Fingerprint v1
    ↓
new photograph
    ↓
additional OCR
    ↓
Fingerprint v2
```

Never update v1 in place.

Matching runs reference:

```text
fingerprintId
fingerprintVersion
```

---

# 11. Fingerprint Construction

Conceptual pipeline:

```text
Report
 + Media
 + OCR
 + Vision
 + Embeddings
 + Attributes
 + Protected identifiers
 + Location
 + Time
        ↓
FingerprintAssembler
        ↓
ItemFingerprint
```

`FingerprintAssembler` only assembles evidence.

It does not run models.

---

# 12. AI Service Design

Recommended:

```text
Python
FastAPI
```

Major internal modules:

```text
vision
ocr
speech
translation
embeddings
local_features
attribute_extraction
distinctive_features
challenge_generation
claim_interpretation
privacy
```

---

# 13. AI Provider Contracts

Define provider interfaces.

Example:

```python
class ImageEmbeddingProvider(Protocol):
    def embed_image(self, request: ImageEmbeddingRequest) -> ImageEmbeddingResult:
        ...
```

Equivalent interfaces:

```text
DetectionProvider
SegmentationProvider
OCRProvider
SpeechProvider
TranslationProvider
TextEmbeddingProvider
ImageEmbeddingProvider
LocalFeatureProvider
LLMProvider
```

---

# 14. Model Manifest

Every model available to Dzoka should have a manifest.

Example:

```yaml
id: openclip-vit-b32
task: multimodal_embedding
provider: local
model: ViT-B-32
version: "1"
dimensions: 512
license: MIT-compatible-dependency
device:
  cpu: true
  cuda: true
status: active
```

---

# 15. Global Visual Embeddings

Purpose:

- broad visual similarity;
- cross-modal image/text retrieval;
- initial candidate generation.

Default candidate:

```text
OpenCLIP-compatible provider
```

Do not assume one global embedding uniquely identifies an object.

---

# 16. Local Visual Identity

Dzoka must support same-instance evidence.

Pipeline:

```text
image
 ↓
object crop
 ↓
multi-scale regions / patches
 ↓
patch embeddings
 ↓
salience filtering
 ↓
local visual identity
```

Used to distinguish:

```text
black backpack A
```

from:

```text
black backpack B
```

through details such as:

```text
scratch
sticker
zipper wear
tear
texture
missing component
```

---

# 17. OCR Design

OCR output:

```text
OCRResult
├── text
├── normalizedText
├── boundingBox
├── confidence
├── language
└── modelProvenance
```

Potential secret identifiers discovered via OCR must be classified before public exposure.

---

# 18. Attribute Design

Attributes must track:

```text
name
value
source
confidence
reliability
classification
```

Example:

```text
brand = Lenovo
source = OCR
confidence = .96
```

versus:

```text
brand = Lenovo
source = USER
confidence = 1.0
```

Source type matters.

---

# 19. Matching Engine Overview

Matching is divided into:

```text
Candidate Retrieval
        ↓
Candidate Fusion
        ↓
Hard Conflict Analysis
        ↓
Feature Scoring
        ↓
Heavy Reranking
        ↓
Evidence Coverage
        ↓
Confidence
        ↓
Explanation
```

---

# 20. Candidate Retrieval

Implement:

```text
CandidateRetrievalService
```

with independent retrievers:

```text
ExactIdentifierRetriever
CategoryRetriever
AttributeRetriever
LexicalRetriever
TrigramRetriever
SemanticRetriever
ImageRetriever
TextToImageRetriever
GeoRetriever
TemporalRetriever
DistinctiveFeatureRetriever
FederatedRetriever
```

Each returns:

```text
RetrievalCandidate
├── itemId
├── route
├── rank
├── rawScore
└── evidence
```

---

# 21. Candidate Set Fusion

Raw retrieval scores from different methods are not directly comparable.

Therefore use rank fusion such as:

```text
Reciprocal Rank Fusion
```

Conceptually:

```text
RRF(candidate) =
Σ 1 / (k + rank_i)
```

where each retrieval route contributes its candidate rank.

Alternative fusion methods may be benchmarked.

---

# 22. Candidate Deduplication

The same item may appear through:

```text
lexical
semantic
image
geo
```

Candidate fusion must collapse these into one candidate with multiple retrieval evidence routes.

---

# 23. Hard Conflict Engine

Implement:

```text
ConflictEngine
```

Rule classes:

```text
CRITICAL
MAJOR
MINOR
```

Examples:

### Critical

```text
IMEI mismatch
serial mismatch
passport/document-number mismatch
```

### Major

```text
strong model mismatch
brand mismatch
strong category incompatibility
chronology contradiction
```

### Minor

```text
colour discrepancy
size disagreement
material discrepancy
```

---

# 24. Conflict Evaluation

Concept:

```text
Candidate
   ↓
ConflictRuleRegistry
   ↓
evaluate each known comparable feature
   ↓
MatchConflict[]
```

Critical rejecting conflicts may stop further expensive reranking.

---

# 25. FeatureScore

Canonical feature result:

```text
FeatureScore
├── featureType
├── score
├── available
├── reliability
├── weight
├── sourceLeft
├── sourceRight
├── relation
├── contribution
├── explanation
└── provenance
```

Unavailable:

```text
available = false
score = null
```

---

# 26. Feature Scorers

Implement:

```text
CategoryScorer
AttributeScorer
LocationScorer
TemporalScorer
SemanticScorer
LexicalScorer
GlobalImageScorer
LocalVisualScorer
OCRScorer
DistinctiveFeatureScorer
ProvenanceScorer
MovementPlausibilityScorer
```

---

# 27. Weighted Score

For available features:

```text
effectiveWeight_i =
configuredWeight_i × reliability_i
```

Then:

```text
MatchScore =
Σ(score_i × effectiveWeight_i)
─────────────────────────────
Σ(effectiveWeight_i)
```

Unavailable features are excluded.

---

# 28. Example Weight Profile

Initial default may look like:

```text
category       0.10
attributes     0.25
location       0.15
time           0.10
distinctive    0.15
semantic       0.10
lexical        0.05
global_image   0.05
local_visual   0.05
```

These values are not permanent constants.

They belong in versioned configuration.

Category-specific profiles are permitted.

---

# 29. Category-Specific Matching

Example:

For smartphones:

```text
serial / IMEI
brand
model
visual
```

may dominate.

For umbrellas:

```text
visual
colour
location
time
distinctive features
```

may matter more.

Implement:

```text
WeightProfileRegistry
```

---

# 30. Evidence Reliability

The same apparent feature can have different reliability.

Conceptually:

```text
verified serial
    >
staff-entered identifier
    >
user-entered identifier
    >
high-confidence OCR
    >
vision inference
    >
generic language inference
```

Reliability must be explicit and configurable.

---

# 31. Evidence Coverage

Compute separately from match score.

Possible conceptual formula:

```text
coverage =
availableImportantEvidenceWeight
──────────────────────────────
totalPotentialEvidenceWeight
```

Coverage is used to cap confidence.

Example:

```text
score = 0.95
coverage = 0.22
```

must not become very-high confidence.

---

# 32. Match Confidence

Keep separate:

```text
retrievalScore
matchScore
evidenceCoverage
matchConfidence
```

Possible confidence calculation considers:

```text
match score
coverage
identity-grade evidence
critical conflicts
calibration model
```

---

# 33. Confidence Levels

Example:

```text
VERY_HIGH
HIGH
MEDIUM
LOW
INSUFFICIENT
REJECTED
```

Do not rely on score alone.

---

# 34. Heavy Reranking

Only rerank a bounded candidate set.

Example:

```text
3,000,000 found items
     ↓
indexed retrieval
     ↓
top 300
     ↓
feature scoring
     ↓
top 50
     ↓
local visual / multimodal reranker
     ↓
top 10
```

Never perform expensive pairwise inference against the full corpus.

---

# 35. Reranker Interface

```text
RerankerProvider
```

Input:

```text
queryFingerprint
candidateFingerprint
structuredEvidence
```

Output:

```text
rerankScore
evidence
modelProvenance
```

---

# 36. LLM Role in Matching

LLMs may:

- explain structured evidence;
- normalize unstructured descriptions;
- optionally act as bounded rerankers.

LLMs may not:

- override critical conflicts;
- invent evidence;
- directly verify ownership.

---

# 37. Match Explainability

Every recommended candidate must explain why it was surfaced.

Example:

```text
Category:
backpack ↔ backpack
100%

Brand:
Lenovo ↔ Lenovo
100%

Location:
Joina City ↔ Joina City security desk
94%

Time:
14:00 ± 30m ↔ found 14:23
96%

Global visual:
0.87

Distinctive mark:
yellow underside sticker
96%

Critical conflicts:
none

Coverage:
82%

Confidence:
HIGH
```

---

# 38. Claim Design

A candidate may lead to a claim.

```text
Claim
├── claimId
├── itemId
├── claimantId
├── candidateId
├── status
├── evidence[]
├── verificationId
└── riskAssessmentId
```

---

# 39. Claim State Machine

```text
DRAFT
 ↓
SUBMITTED
 ↓
UNDER_REVIEW
 ↓
VERIFICATION_REQUIRED
 ↓
VERIFYING
 ↓
APPROVED
```

Alternative:

```text
REJECTED
WITHDRAWN
CONFLICT
EXPIRED
```

---

# 40. Ownership Verification Design

Verification combines:

```text
private challenge responses
protected identifiers
receipts
prior photographs
unique hidden attributes
institutional evidence
claim risk
human review
policy
```

The workflow is:

```text
Claim
 ↓
VerificationSession
 ↓
Challenge / Evidence
 ↓
Evidence evaluation
 ↓
Risk
 ↓
Policy
 ↓
Human review if required
 ↓
VerificationDecision
```

---

# 41. Ownership Challenge Generation

Implement:

```text
ChallengeCandidateSelector
```

Inputs:

```text
private attributes
distinctive features
OCR evidence
protected identifiers
known pre-loss evidence
```

Filters:

```text
must not already be public
must be discriminative
must be safe
must be answerable
must have sufficient evidence confidence
```

---

# 42. Challenge Example

Found item evidence:

```text
yellow sticker underneath
scratch beside USB-C port
missing right rubber foot
serial suffix 9247
```

Possible generated challenge:

```text
"What distinguishing feature is located underneath the device?"
```

Avoid:

```text
"Does it have a yellow sticker underneath?"
```

because that leaks the answer.

---

# 43. Challenge Disclosure Tracking

Every challenge should record what information was revealed.

This prevents later challenges from relying on evidence already disclosed during previous interactions.

---

# 44. Challenge Response Evaluation

Evaluation may combine:

```text
exact comparison
normalized comparison
fuzzy matching
semantic comparison
LLM interpretation
human review
```

The result is:

```text
VerificationEvidence
```

not automatic ownership approval.

---

# 45. Verification Policy

Example conceptual policy:

```text
if critical identifier matches
and no serious contradiction
    → may verify

if several independent private features match
and evidence coverage is strong
    → may verify

if only public attributes match
    → insufficient

if conflicting claim exists
    → human review
```

Policies must be versioned.

---

# 46. Fraud / Risk Design

Implement:

```text
ClaimRiskService
```

Possible signals:

```text
failed claims
claim frequency
multiple concurrent claims
device/account reuse
location impossibility
copied challenge answers
rapid challenge guessing
identity mismatch
known abuse
```

Output:

```text
RiskAssessment
├── score
├── level
├── signals[]
└── recommendedAction
```

---

# 47. Risk Actions

Possible:

```text
ALLOW_STANDARD_FLOW
REQUIRE_EXTRA_EVIDENCE
REQUIRE_HUMAN_REVIEW
BLOCK
```

Risk must remain independent from match score.

---

# 48. Recovery Authorization

Implement:

```text
RecoveryAuthorizationService
```

Inputs:

```text
VerificationDecision
RiskAssessment
CustodyState
OrganizationPolicy
ReviewerDecision
```

Output:

```text
RecoveryAuthorization
```

Only this stage allows physical return workflow to begin.

---

# 49. Custody Design

Custody is append-oriented.

```text
CustodyEvent
├── itemId
├── previousCustodian
├── newCustodian
├── previousLocation
├── newLocation
├── actor
├── reason
├── occurredAt
└── evidence[]
```

---

# 50. Custody States

Possible:

```text
WITH_FINDER
IN_INSTITUTIONAL_CUSTODY
READY_FOR_HANDOVER
IN_TRANSFER
WITH_COURIER
RETURNED
DISPOSED
```

---

# 51. Custody Invariant

The current custody record may be materialized, but history must never be destroyed.

---

# 52. Handover Design

Supported mechanisms:

```text
OTP
QR
single-use token
staff confirmation
courier proof
digital signature
```

Tokens must:

```text
use cryptographically secure randomness
be hashed at rest
expire
be single-use
record attempts
support revocation
```

---

# 53. Courier Design

Define:

```text
CourierProvider
```

Methods:

```text
quote()
createPickup()
createShipment()
trackShipment()
cancelShipment()
handleWebhook()
```

All provider-specific payloads remain in adapters.

---

# 54. Courier State

Canonical Dzoka shipment states:

```text
QUOTED
BOOKED
PICKUP_SCHEDULED
PICKED_UP
IN_TRANSIT
OUT_FOR_DELIVERY
DELIVERED
FAILED
CANCELLED
RETURNED
```

Provider-specific states map to these.

---

# 55. Reward Design

Rewards are optional.

```text
Reward
├── amount
├── currency
├── beneficiary
├── status
├── conditions
└── paymentReference
```

Workflow:

```text
OFFERED
 ↓
AUTHORIZED
 ↓
RESERVED
 ↓
ELIGIBLE
 ↓
RELEASED
```

---

# 56. Reward Rule

Voluntary reward failure must never prevent rightful recovery.

---

# 57. Messaging Design

Canonical inbound message:

```text
InboundMessage
├── channel
├── provider
├── providerMessageId
├── senderReference
├── conversationReference
├── messageType
├── text
├── media[]
├── location
└── receivedAt
```

---

# 58. WhatsApp Conversation Design

Example lost-item conversation:

```text
START
 ↓
detect intent
 ↓
LOST ITEM
 ↓
collect description
 ↓
collect photo optional
 ↓
collect voice optional
 ↓
collect location
 ↓
collect approximate time
 ↓
confirm summary
 ↓
submit report
 ↓
provide case number
```

The conversation should allow natural-language shortcuts.

Example:

```text
"I lost a black Lenovo backpack at Joina around 2."
```

should skip unnecessary question-by-question collection where enough data can be extracted.

---

# 59. Conversation State

Store ephemeral channel state in Redis.

Authoritative report data moves to PostgreSQL once submitted.

---

# 60. Secure Relay

Owner and finder communicate through:

```text
RecoveryConversation
```

not by exposing:

```text
phone
email
home address
```

by default.

---

# 61. Event Processing Design

Authoritative write pattern:

```text
Application Service
 ↓
Domain change
 +
Outbox event
 ↓
single DB transaction
 ↓
commit
 ↓
OutboxPublisher
 ↓
Kafka-compatible broker
```

---

# 62. Consumer Design

All consumers implement:

```text
Inbox / dedup
 ↓
validate schema
 ↓
establish tenant context
 ↓
process
 ↓
persist result
 ↓
acknowledge
```

---

# 63. Delivery Guarantee

Assume:

```text
at-least-once delivery
```

Therefore all critical consumers must be idempotent.

---

# 64. Retry Design

Failures:

```text
RETRYABLE
NON_RETRYABLE
UNKNOWN
```

Retryable failures use:

```text
exponential backoff
+
jitter
+
bounded attempts
```

Permanent failures go to DLQ.

---

# 65. Matching Worker

Flow:

```text
fingerprint.ready
 ↓
validate fingerprint still current
 ↓
invoke matching engine
 ↓
persist MatchingRun
 ↓
persist candidates/evidence
 ↓
matching.completed
```

---

# 66. AI Enrichment Worker

Flow:

```text
item.reported
 ↓
create enrichment tasks
 ↓
run required tasks
 ↓
run optional tasks
 ↓
store derived evidence
 ↓
FingerprintAssembler
 ↓
fingerprint.ready
```

---

# 67. Partial Enrichment

The system must not wait forever for every optional AI task.

Example:

```text
required:
text extraction
text embedding

optional:
OCR
translation
```

Fingerprint creation proceeds when required evidence is ready.

Late optional evidence may create a new fingerprint version.

---

# 68. Federation Design

Federation is a separate trust boundary.

Conceptual flow:

```text
Local Core
 ↓
Federation Policy
 ↓
Safe Search Envelope
 ↓
Local Federation Gateway
 ↓
Remote Federation Gateway
 ↓
Remote Core
 ↓
Remote Matching
 ↓
Safe Candidate Envelope
 ↓
Local reconciliation
```

---

# 69. Federation Node Identity

Each node has:

```text
nodeId
organization
public keys
capabilities
protocol versions
trust state
```

Messages must be attributable to a node identity.

---

# 70. Federation Request Security

Each request should include:

```text
messageId
originNode
targetNode
timestamp
nonce
protocolVersion
signature
payloadHash
```

Validate:

```text
signature
trust state
timestamp tolerance
nonce uniqueness
capability
policy
schema
```

---

# 71. Federated Search Request

A privacy-safe request may contain:

```text
queryId
anonymousFingerprintReference
category
safeAttributes
coarseLocation
timeWindow
approved vector representations
model metadata
requested capabilities
```

Do not include PII unless explicitly authorized.

---

# 72. Federated Candidate

Canonical representation:

```text
FederatedCandidate
├── sourceNode
├── remoteReference
├── safeEvidence[]
├── remoteScore
├── evidenceCoverage
├── algorithmVersion
├── modelVersions[]
└── calibrationMetadata
```

---

# 73. Federation Score Rule

Never perform:

```text
sort candidates by remoteScore
```

across heterogeneous nodes.

Instead:

```text
normalize evidence
 ↓
local/network reranker
 ↓
local confidence
```

---

# 74. Federation Timeouts

Remote searches must use:

```text
parallel fan-out
bounded timeout
per-peer circuit breaker
```

A dead peer must not block the network.

---

# 75. Federation Circuit Breaker

Track:

```text
success
failure
timeout
consecutiveFailures
cooldownUntil
```

Peer states may include:

```text
HEALTHY
DEGRADED
OPEN
HALF_OPEN
```

---

# 76. Federation Protocol Versioning

Nodes advertise:

```text
protocol versions
capabilities
supported fingerprint schemas
supported embedding models where applicable
```

Never assume all nodes upgrade simultaneously.

---

# 77. Traceability Design

Internal canonical event:

```text
RecoveryEvent
```

Fields:

```text
eventId
itemId
eventType
actor
organization
location
occurredAt
reason
source
evidenceReferences
```

---

# 78. EPCIS Adapter

Implement:

```text
EpcisAdapter
```

to translate selected recovery events to/from EPCIS-compatible representations.

EPCIS is interoperability, not the core domain model.

---

# 79. Recovery Graph

Initial implementation derives a logical graph from:

```text
RecoveryEvents
CustodyEvents
Locations
Routes
Organizations
```

Do not add Neo4j unless graph workload justifies it.

---

# 80. Movement Intelligence

Model institutional movement as discrete transitions.

Example:

```text
BUS_SEAT
 ↓
PASSENGER
 ↓
DRIVER
 ↓
DEPOT
 ↓
LOST_PROPERTY_OFFICE
```

---

# 81. RecoveryTransitionModel

Input:

```text
item category
current state
location
organization
route
custodian
time
historical transitions
```

Output:

```text
nextState
nextLocation
probability
expectedDelay
modelVersion
```

Prediction remains separate from factual custody state.

---

# 82. Movement Prediction Uses

Possible uses:

```text
suggest likely depot
prioritize federation nodes
rank search locations
estimate likely transfer delay
highlight unusual custody path
```

---

# 83. CCTV Edge Design

`dzoka-edge` is separately deployable.

Modules:

```text
camera connectors
video retrieval
temporal search
detection
tracking
ReID
privacy
evidence extraction
offline sync
```

---

# 84. CCTV Investigation Flow

```text
authorized case
 ↓
camera/time selection
 ↓
reference object / ROI
 ↓
coarse temporal search
 ↓
hour-level bracket
 ↓
minute-level bracket
 ↓
second-level bracket
 ↓
disappearance window
 ↓
detection/tracking
 ↓
evidence extraction
```

---

# 85. CCTV Data Rule

Prefer central upload of:

```text
object crops
short evidence clips
timestamps
camera ID
track summary
embedding
zone
```

not full archives.

---

# 86. CCTV Privacy

Before upload:

```text
face redaction
plate redaction
privacy zone masking
metadata sanitization
```

where applicable.

---

# 87. Object Tracking Interface

Define:

```text
ObjectTrackerProvider
```

Possible providers:

```text
ByteTrackProvider
DeepSortProvider
```

Tracking is edge-focused.

---

# 88. Registered Items

Users or institutions may pre-register items.

```text
RegisteredItem
├── itemId
├── ownerReference
├── photos[]
├── protectedIdentifiers[]
├── tags[]
└── proofs[]
```

This improves recovery later.

---

# 89. QR/NFC Design

Public tag payload must not contain owner PII.

Use:

```text
opaque tag ID
```

which resolves through Dzoka.

Flow:

```text
scan
 ↓
Dzoka endpoint
 ↓
anonymous finder workflow
```

---

# 90. Item Identity Provider

Define:

```text
ItemIdentityProvider
```

Implementations may include:

```text
InternalIdentityProvider
QrIdentityProvider
NfcIdentityProvider
Gs1IdentityProvider
DidIdentityProvider
EnterpriseAssetIdentityProvider
```

---

# 91. API Design

All external APIs use:

```text
/api/v1/...
```

Response models are DTOs.

Do not expose domain entities or ORM objects directly.

---

# 92. API Error Shape

Canonical shape:

```json
{
  "error": {
    "code": "CLAIM_NOT_ALLOWED",
    "message": "The claim cannot be submitted in the current state.",
    "requestId": "..."
  }
}
```

Do not expose stack traces.

---

# 93. Pagination

Use stable pagination.

Prefer:

```text
cursor pagination
```

for large dynamic collections.

Offset pagination is acceptable for small administrative views.

---

# 94. Idempotency API

Critical POST operations support:

```text
Idempotency-Key
```

Examples:

```text
claim submission
custody transfer
courier booking
handover confirmation
reward release
```

---

# 95. Optimistic Concurrency

State-changing APIs may use:

```text
version
ETag / If-Match
```

to prevent lost updates.

---

# 96. Public Search Design

Public search exposes only safe projection fields.

Example:

```text
publicReference
category
safeDescription
broadLocation
approximateDate
safeImage
```

Never public:

```text
full serial
IMEI
finder contact
secret attributes
verification answers
exact custody room
```

---

# 97. Sensitive Data Access

Use:

```text
SensitiveDataAccessService
```

for decrypting or revealing high-risk fields.

Every access records:

```text
actor
resource
purpose
time
correlationId
```

---

# 98. Secret Identifier Matching

Exact secret identifiers use a keyed hash.

Concept:

```text
normalize(identifier)
 ↓
HMAC(secretKey, normalized)
 ↓
indexed equality comparison
```

Do not use unsalted SHA-256 for low-entropy secrets.

---

# 99. Encryption

Use envelope encryption for fields requiring reversible protection.

Stored record:

```text
ciphertext
keyId
encryptionVersion
```

Keys live outside PostgreSQL.

---

# 100. Media Upload Design

Preferred:

```text
client requests upload authorization
 ↓
core validates request
 ↓
signed object-storage URL
 ↓
client uploads directly
 ↓
media.uploaded event
 ↓
sanitization
```

Avoid routing huge files through application memory.

---

# 101. Media Sanitization

Pipeline:

```text
MIME verification
size validation
malware scan
metadata extraction
EXIF removal
privacy detection
redaction
safe derivative generation
```

---

# 102. Storage Design

Primary:

```text
PostgreSQL
PostGIS
pgvector
S3/MinIO
Redis
Kafka-compatible broker
```

Optional:

```text
Qdrant
MLflow
```

---

# 103. Vector Backend Interface

Define:

```text
VectorSearchProvider
```

Methods conceptually:

```text
index()
delete()
search()
searchBatch()
health()
```

Implement:

```text
PgVectorProvider
QdrantProvider
```

---

# 104. Vector Compatibility

Search request must identify:

```text
embeddingType
model
modelVersion
dimensions
```

Never search across incompatible vector spaces.

---

# 105. Search Projection

Maintain a denormalized search projection with safe searchable fields.

Example:

```text
ItemSearchProjection
├── itemId
├── tenantId
├── category
├── brand
├── model
├── safeAttributes
├── textDocument
├── location
├── timeWindow
├── status
└── fingerprintId
```

---

# 106. PostgreSQL Search

Use:

```text
FTS
pg_trgm
PostGIS
pgvector
```

before adding additional search engines.

OpenSearch remains optional.

---

# 107. Multi-Tenancy

Every tenant-owned query must be tenant scoped.

Do not trust:

```text
frontend filtering
```

for isolation.

Enforce through:

```text
authorization
repository predicates
constraints
vector filters
object namespace
cache key namespace
```

---

# 108. Row-Level Security

PostgreSQL RLS may be used as defense in depth.

It does not replace application policy.

---

# 109. Policy Design

Policy types include:

```text
ACCESS
VERIFICATION
RECOVERY
CUSTODY
FEDERATION
RETENTION
CCTV
REWARD
DATA_DISCLOSURE
```

Policies are versioned.

---

# 110. Policy Engine

Define:

```text
PolicyEnginePort
```

Implementations:

```text
InternalPolicyEngine
OpaPolicyAdapter
```

OPA is optional.

---

# 111. Observability

All services emit:

```text
structured logs
metrics
traces
```

through OpenTelemetry.

---

# 112. Correlation

Every user operation should propagate:

```text
requestId
correlationId
traceId
```

through:

```text
HTTP
events
workers
federation
external integrations
```

---

# 113. Important Metrics

Examples:

```text
dzoka_reports_created_total
dzoka_fingerprints_generated_total
dzoka_matching_duration_seconds
dzoka_candidates_generated_total
dzoka_claims_submitted_total
dzoka_ownership_verified_total
dzoka_recoveries_completed_total
dzoka_recovery_duration_seconds
dzoka_federation_query_duration_seconds
dzoka_ai_inference_duration_seconds
```

---

# 114. Audit Design

Audit is append-oriented.

Audit:

```text
who
did what
to which resource
for what purpose
when
from where
```

Do not dump entire secret objects into audit payloads.

---

# 115. Outbox Design

Authoritative transaction:

```text
BEGIN
 update aggregate
 insert outbox event
COMMIT
```

Publisher later sends it to Kafka.

This is required for critical domain events.

---

# 116. Inbox Design

Critical consumers store:

```text
consumerName
eventId
processedAt
status
```

with uniqueness:

```text
(consumerName, eventId)
```

---

# 117. Exactly-Once Rule

Do not design around magical end-to-end exactly-once processing.

Use:

```text
at-least-once
+
idempotency
+
constraints
+
inbox/outbox
```

---

# 118. External Webhooks

Flow:

```text
webhook
 ↓
signature validation
 ↓
replay protection
 ↓
deduplication
 ↓
schema validation
 ↓
normalization
 ↓
application service
 ↓
domain event
```

---

# 119. Payment Webhook Rule

A payment provider saying:

```text
success
```

does not directly cause:

```text
RewardReleased
```

The Reward domain confirms state and policy first.

---

# 120. Courier Webhook Rule

Courier:

```text
DELIVERED
```

is normalized and validated before Dzoka transitions shipment state.

---

# 121. Failure Design

All external failures must be categorized.

```text
RetryableExternalFailure
PermanentExternalFailure
PolicyFailure
ValidationFailure
ConflictFailure
SecurityFailure
```

---

# 122. Graceful Degradation

Examples:

```text
AI unavailable
→ accept report

WhatsApp unavailable
→ web still works

Qdrant unavailable
→ pgvector path may continue

federation unavailable
→ local search works

courier unavailable
→ pickup available

MLflow unavailable
→ inference continues
```

---

# 123. Feature Flags

Flags may include:

```text
FEDERATION
CCTV
AI_CHALLENGES
REWARDS
COURIER
MOVEMENT_INTELLIGENCE
EXPERIMENTAL_RERANKER
```

Feature flags do not bypass authorization.

---

# 124. Testing Strategy

Each module requires:

```text
unit tests
integration tests
contract tests
```

Critical workflows also require:

```text
end-to-end tests
security tests
failure tests
concurrency tests
```

---

# 125. Matching Golden Dataset

Maintain deterministic cases including:

```text
same item, different angle
same item, different lighting
same category, different instance
missing image
missing description
serial mismatch
strong visual similarity but wrong model
nearby location
far location with plausible transport route
distinctive sticker match
```

---

# 126. Verification Golden Dataset

Include:

```text
correct hidden feature
wrong hidden feature
partially correct answer
publicly guessable answer
correct receipt
wrong serial
multiple competing claimants
brute-force attempts
```

---

# 127. Federation Tests

At minimum:

```text
2-node search
3-node search
untrusted node rejection
signature failure
replay attack
timeout
circuit breaker
schema incompatibility
privacy field filtering
remote score calibration
```

---

# 128. Edge Tests

Include:

```text
object present
object disappears
temporary occlusion
lighting change
camera interruption
wrong reference object
offline edge sync
privacy redaction
```

---

# 129. Security Tests

Mandatory:

```text
cross-tenant access
secret-data disclosure
IDOR
replayed handover token
replayed webhook
federation spoofing
upload malware
MIME confusion
SSRF
rate-limit bypass
brute-force challenge attempts
```

---

# 130. Provider Contract Tests

Every interchangeable provider must pass common tests.

Examples:

```text
VectorSearchProviderContractTest
MessagingProviderContractTest
CourierProviderContractTest
PaymentProviderContractTest
OCRProviderContractTest
ImageEmbeddingProviderContractTest
```

---

# 131. Database Migration Design

Use an explicit migration tool.

Production must never use automatic destructive schema updates.

Use expand-and-contract:

```text
add
 ↓
deploy compatible code
 ↓
backfill
 ↓
switch
 ↓
verify
 ↓
remove old field later
```

---

# 132. Background Reprocessing

Operations such as:

```text
reembedding
reindexing
fingerprint regeneration
analytics rebuild
```

must:

```text
run in batches
support checkpointing
be restartable
not overwrite historical results
```

---

# 133. Model Promotion

Workflow:

```text
candidate model
 ↓
offline evaluation
 ↓
benchmark comparison
 ↓
shadow mode
 ↓
review
 ↓
promotion
```

Promotion changes future model selection.

It does not rewrite history.

---

# 134. ML Feedback

Ground-truth events:

```text
MATCH_CONFIRMED
MATCH_REJECTED
CLAIM_VERIFIED
CLAIM_REJECTED
RECOVERY_SUCCESS
RECOVERY_FAILED
```

feed versioned datasets.

---

# 135. Main ML Metrics

Track:

```text
Recall@10
Recall@50
Precision@K
MRR
NDCG
false match rate
candidate recall
verification false-positive rate
verified recovery rate
median time to recovery
```

Primary business outcome:

```text
verified successful recovery
```

---

# 136. Low-Bandwidth Design

Support:

```text
compressed images
resumable uploads
PWA
WhatsApp
asynchronous processing
small API payloads
offline institutional workflows
```

---

# 137. Localization

Store:

```text
raw source language
normalized canonical representation
translated representation
```

Do not discard original language.

---

# 138. Time Design

Persist timestamps in:

```text
UTC
```

Display using user/tenant timezone.

Approximate time remains a range.

---

# 139. Public IDs

Internal primary IDs may be UUIDv7.

Human-visible IDs should look like:

```text
DZK-2026-4J7Q8P
```

Never require users to communicate UUIDs.

---

# 140. No Giant Aggregate

Do not implement:

```text
Case
├── reports
├── item
├── fingerprint
├── all matches
├── all claims
├── custody
├── messages
├── shipment
└── reward
```

as one transactional ORM aggregate.

Use bounded aggregates connected by IDs/events.

---

# 141. No Giant Microservice Explosion

Do not create:

```text
ClaimService deployment
ItemService deployment
ReportService deployment
CustodyService deployment
...
```

solely because they are separate domain modules.

Keep them inside `dzoka-core` unless independent scaling/trust/runtime justifies extraction.

---

# 142. Production Deployment Design

Small installation:

```text
Reverse Proxy
Dzoka Web
Dzoka Core
Dzoka AI
Dzoka Matching
Dzoka Workers
PostgreSQL
Redis
MinIO
Kafka-compatible broker
```

Large deployment adds:

```text
Kubernetes
HA PostgreSQL
Kafka cluster
GPU worker pools
optional Qdrant
distributed object storage
full observability
```

---

# 143. Offline Institutional Node

A node must still support:

```text
local reports
local search
local claims
local verification
local custody
local recovery
```

without federation connectivity.

Federation queues resume later.

---

# 144. Disaster Recovery Design

Authoritative priority:

```text
1. PostgreSQL
2. private/secret object storage
3. federation identity keys
4. configuration
5. financial reconciliation
6. public derivatives
7. event backlog
8. rebuildable indexes
9. cache
```

---

# 145. Runbook Requirements

The production repository must eventually contain runbooks for:

```text
database restore
object-store restore
event replay
DLQ recovery
stuck enrichment
model rollback
Qdrant rebuild
federation key compromise
peer revocation
false-match incident
privacy deletion
cross-tenant incident
courier reconciliation
reward/payment reconciliation
CCTV privacy incident
```

---

# 146. Coding Agent Rules

Codex and other agents must:

1. read `ARCHITECTURE.md`;
2. read `DESIGN.md`;
3. read relevant `docs/architecture/*`;
4. read relevant ADRs;
5. inspect `docs/source-audit/*` before reusing upstream concepts;
6. preserve architectural invariants;
7. write tests with implementation;
8. update documentation when contracts change;
9. update `STATUS.md`;
10. never introduce undocumented architecture changes.

---

# 147. Prohibited Shortcuts

Do not:

```text
use one opaque AI score
use one CLIP vector as complete item identity
treat match as ownership
treat LLM output as authority
use Math.random for security tokens
hard-code provider secrets
store passwords with plain SHA256
store base64 media in database records
expose finder contact details publicly
return secret attributes in public search
store AI vectors without model version
compare incompatible embeddings
compare federation scores blindly
run O(N²) matching
make report submission wait for all AI
copy unlicensed source code
make blockchain mandatory
make Qdrant mandatory
make Kubernetes mandatory
```

---

# 148. End-to-End Reference Implementation

A complete successful workflow should execute roughly as follows.

```text
1. User sends WhatsApp voice note:
   "Ndasiya black Lenovo bag muJoina around 2pm."

2. Messaging adapter validates webhook.

3. Inbound message is normalized.

4. Speech transcription runs.

5. Language is detected.

6. Structured extraction identifies:
   LOST
   BAG
   Lenovo
   black
   Joina City
   approximately 14:00.

7. Raw voice and transcript remain stored.

8. User uploads an old photograph.

9. Media is validated and sanitized.

10. Vision extracts:
    object crop
    category
    global embedding
    patch embeddings
    distinctive features.

11. OCR runs where useful.

12. FingerprintAssembler creates ItemFingerprint v1.

13. Candidate retrieval runs:
    category
    lexical
    semantic
    image
    local visual
    geo
    time.

14. Rank fusion builds a shortlist.

15. ConflictEngine eliminates impossible candidates.

16. Feature scoring calculates structured evidence.

17. Local visual reranker compares top candidates.

18. EvidenceCoverageService calculates evidence thickness.

19. Confidence service produces calibrated confidence.

20. No sufficiently strong local result exists.

21. Federation policy permits search.

22. Safe fingerprint envelope is produced.

23. Federation gateway fans out to trusted peers.

24. Transport operator node finds a similar bag.

25. Remote safe evidence returns.

26. Local Dzoka normalizes and reranks it.

27. Candidate becomes HIGH confidence.

28. User receives WhatsApp notification.

29. User submits claim.

30. Ownership challenge generator selects a hidden feature.

31. User answers.

32. Claim answer interpretation produces evidence.

33. Verification policy combines:
    challenge
    old photograph
    hidden attributes
    risk.

34. Staff review is requested if required.

35. Ownership is verified.

36. RecoveryAuthorization is created.

37. Courier is requested.

38. Custody transfer to courier is recorded.

39. Shipment updates arrive through verified webhooks.

40. Owner receives one-time handover token.

41. Courier completes handover.

42. Dzoka confirms recovery.

43. Optional reward becomes eligible.

44. Payment provider releases reward.

45. Case closes.

46. RecoveryEvent history is finalized.

47. Match decision becomes ML feedback.

48. Custody path becomes a recovery transition observation.

49. Recovery intelligence learns:
    route → depot → lost property desk.

50. Future cases use the improved evidence.
```

---

# 149. Design Completion Criteria

A Dzoka feature is not complete because:

```text
UI exists
endpoint returns 200
model runs
```

A feature is complete when appropriate aspects of the following exist:

```text
domain model
application service
authorization
persistence
events
failure handling
idempotency
observability
tests
documentation
security review
migration
operational support
```

---

# 150. Design Philosophy

Dzoka intentionally separates:

```text
PERCEPTION
What does the object look like?

RETRIEVAL
What candidates should we inspect?

MATCHING
Which observations may refer to the same object?

CONFIDENCE
How strong is the evidence?

VERIFICATION
Does this claimant have ownership evidence?

AUTHORIZATION
May this item be released?

CUSTODY
Who physically controls it?

RECOVERY
How does it get back?

LEARNING
What does the successful or failed outcome teach us?
```

No single AI model should own all of those questions.

---

# 151. Final Design Principle

The implementation should preserve this complete chain:

```text
RAW HUMAN EVIDENCE
        ↓
STRUCTURED EVIDENCE
        ↓
ITEM FINGERPRINT
        ↓
CANDIDATE RETRIEVAL
        ↓
MATCH EVIDENCE
        ↓
MATCH CONFIDENCE
        ↓
OWNERSHIP CLAIM
        ↓
VERIFICATION EVIDENCE
        ↓
RECOVERY AUTHORIZATION
        ↓
CUSTODY / LOGISTICS
        ↓
CONFIRMED PHYSICAL RETURN
        ↓
GROUND TRUTH
        ↓
BETTER FUTURE RECOVERY
```

Dzoka succeeds only when it can turn an uncertain report about a missing physical object into a **verified, private, auditable, secure physical recovery** while preserving enough provenance to explain and improve every important decision along the way.