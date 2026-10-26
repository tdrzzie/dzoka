# Reusable Components and Patterns

> **Dzoka — The Open Recovery Network**
>
> Canonical catalogue of architectural patterns, algorithms, workflows, domain models, infrastructure components, standards, and implementation ideas discovered through Dzoka's source-audit process that are suitable for adoption, adaptation, or architectural reference.

---

# 1. Purpose

Dzoka audits existing systems before finalizing major architecture decisions.

The objective is not to assemble Dzoka by copying repositories.

The objective is to determine:

```text
What already works?
What can be reused safely?
What should be adapted?
What should only influence architecture?
What must Dzoka build itself?
```

This document records the positive side of that process.

Its companion is:

```text
docs/source-audit/findings/rejected-patterns.md
```

Together they define:

```text
WHAT DZOKA SHOULD INHERIT
+
WHAT DZOKA MUST REFUSE TO INHERIT
```

---

# 2. Reuse Classification

Every reusable finding belongs to one of four categories.

| Classification | Meaning |
|---|---|
| `ADOPT` | Use the component/library/standard substantially as provided |
| `ADAPT` | Reimplement or significantly modify the pattern for Dzoka |
| `REFERENCE` | Use as architectural/research inspiration only |
| `BUILD` | Required Dzoka capability with no adequate upstream implementation |

---

# 3. Licensing Rule

Technical usefulness does not imply permission to copy source.

Reuse classification and licensing classification are separate.

Example:

```text
Architectural value:
HIGH

License:
UNRESOLVED
```

means:

```text
ADAPT CONCEPT
DO NOT COPY CODE
```

Dzoka must verify:

```text
repository
revision
license
dependency license
reuse implications
```

before direct adoption.

---

# 4. High-Level Reuse Strategy

The current audit findings suggest:

```text
MATCHING DISCIPLINE
    ← globalenglish/lostfound

VISUAL INSTANCE EVIDENCE
    ← FuricBond
    ← ILIAS
    ← Patchwise Retrieval
    ← OpenCLIP

CLAIMS / INSTITUTIONAL WORKFLOW
    ← Lost-and-Found-Tracking-System

PRIVATE OWNERSHIP CHALLENGES
    ← Malak-Rashad/LostAndFound

QR / MESSAGING / INCENTIVES
    ← Smart Asset Finder

MOBILE / PUSH / HANDOVER UX
    ← FindBack

CCTV SEARCH
    ← CCTV Theft Finder

FEDERATION
    ← Mycel
    ← Open Federated Search

TRACEABILITY
    ← OpenEPCIS
    ← GS1 EPCIS

VECTOR SEARCH
    ← pgvector
    ← Qdrant

VISION
    ← PaddleDetection
    ← PaddleOCR
    ← OpenCLIP
```

Dzoka combines these into one coherent architecture rather than selecting a single upstream application.

---

# 5. Core Architectural Principle

No audited application is sufficient as the complete Dzoka foundation.

The strongest architecture therefore uses:

```text
PROVEN COMPONENTS
+
PROVEN PATTERNS
+
DZOKA-SPECIFIC DOMAIN DESIGN
```

instead of:

```text
fork one lost-and-found repository
+
keep adding features indefinitely
```

---

# 6. Reusable Domain Pattern — Separate Observation from Physical Item

**Classification:** `ADAPT`

Strong upstream systems distinguish lost/found records from the physical object being reasoned about.

Dzoka formalizes:

```text
ItemRecord
    ≠
LostReport
    ≠
FoundReport
```

### Benefit

Multiple reports can describe:

```text
the same physical object
```

without forcing them into one mutable report record.

---

# 7. Reusable Domain Pattern — Separate Match from Claim

**Classification:** `ADAPT`

A match answers:

```text
Could these observations describe the same object?
```

A claim answers:

```text
Does this person assert entitlement to it?
```

Dzoka preserves:

```text
MatchCandidate
    ↓
Claim
```

as separate aggregates.

---

# 8. Reusable Domain Pattern — Separate Claim from Verification

**Classification:** `ADAPT`

The existence of a claim does not prove ownership.

Dzoka therefore uses:

```text
Claim
    ↓
VerificationSession
    ↓
VerificationDecision
```

---

# 9. Reusable Domain Pattern — Separate Verification from Recovery Authorization

**Classification:** `BUILD`

A verified owner may still not be authorized for immediate release.

Dzoka adds:

```text
VerificationDecision
    ↓
RecoveryAuthorization
```

to support:

```text
legal holds
institution policy
authorized representatives
guardian requirements
courier restrictions
competing claims
```

---

# 10. Reusable Domain Pattern — Separate Ownership from Custody

**Classification:** `BUILD`

A finder, institution, courier, or staff member may physically possess an item without owning it.

Dzoka therefore separates:

```text
ownership
entitlement
custody
```

---

# 11. Reusable Component — Raw Input Preservation

**Source influence:** `globalenglish01/lostfound`

**Classification:** `ADAPT`

Preserve:

```text
raw_description
```

separately from:

```text
normalized
translated
canonical
AI-extracted
```

representations.

### Dzoka invariant

AI must never silently overwrite original human evidence.

---

# 12. Reusable Component — Structured Attribute Definitions

**Source influence:** `globalenglish01/lostfound`

**Classification:** `ADAPT`

Instead of hard-coding every property into the item schema, define configurable attributes:

```text
AttributeDefinition
├── code
├── dataType
├── searchable
├── matchable
├── importance
├── conflictSeverity
└── classification
```

---

# 13. Reusable Component — Evidence Provenance

**Source influence:** `globalenglish01/lostfound`

**Classification:** `ADAPT`

Derived attributes should know their origin:

```text
USER
STAFF
VISION
OCR
AI
IMPORT
SYSTEM
INSTITUTION
```

Dzoka expands this with:

```text
confidence
reliability
modelRunId
classification
```

---

# 14. Reusable Component — Secret Attributes

**Sources:**

```text
globalenglish01/lostfound
Malak-Rashad/LostAndFound
```

**Classification:** `ADAPT`

Some object traits should remain:

```text
SECRET
```

so they can later support ownership verification.

Examples:

```text
hidden sticker
engraving
private scratch
interior contents
serial suffix
```

---

# 15. Reusable Component — Data Classification

**Classification:** `BUILD / ADAPT`

Dzoka generalizes upstream secrecy concepts into:

```text
PUBLIC
RESTRICTED
SECRET
```

Every important evidence/media field should have a classification.

---

# 16. Reusable Component — ItemFingerprint

**Classification:** `BUILD`

No audited source provides Dzoka's complete fingerprint abstraction.

However, multiple sources contribute pieces.

Dzoka combines:

```text
ItemFingerprint
├── semantic identity
├── structured attributes
├── normalized text
├── OCR
├── global visual features
├── local visual features
├── distinctive regions
├── perceptual hashes
├── protected identifiers
├── location context
├── temporal context
├── evidence quality
├── model provenance
└── fingerprint version
```

---

# 17. Reusable Component — Global Visual Embeddings

**Sources:**

```text
FuricBond/Lost-And-Found
OpenCLIP
VisualMatch
OpenVisualSearch
```

**Classification:** `ADOPT dependency / ADAPT architecture`

Global embeddings are useful for:

```text
semantic visual retrieval
category similarity
appearance similarity
candidate generation
```

---

# 18. Reusable Library — OpenCLIP

**Repository:** `mlfoundations/open_clip`

**Classification:** `ADOPT`

License:

```text
MIT
```

Recommended use:

```text
ImageEmbeddingProvider
```

with model/version-aware storage.

---

# 19. Reusable Component — Object-Aware Image Cropping

**Source influence:** `FuricBond/Lost-And-Found`

**Classification:** `ADAPT`

Pipeline:

```text
photo
    ↓
object detection
    ↓
candidate target crop
    ↓
visual fingerprint extraction
```

### Dzoka improvement

Do not always select only the highest-confidence object.

Support:

```text
category-aware selection
multiple candidate regions
user selection
segmentation
```

---

# 20. Reusable Component — Perceptual Hashes

**Source influence:** `FuricBond/Lost-And-Found`

**Classification:** `ADAPT`

Use:

```text
pHash
dHash
aHash
```

where useful for:

```text
duplicate detection
near-duplicate evidence
media deduplication
```

Do not use as sole physical identity.

---

# 21. Reusable Component — Local Visual Features

**Sources:**

```text
FuricBond ORB implementation
ILIAS
Patchwise Retrieval
RerankingTransformer
```

**Classification:** `ADAPT`

Physical instance identification benefits from:

```text
local patches
logos
wear patterns
scratches
stickers
damage
spatial correspondences
```

---

# 22. Reusable Research Source — ILIAS

**Classification:** `ADAPT / REFERENCE`

Useful for:

```text
instance-level retrieval
extractor evaluation
FAISS-style research
query expansion
retrieval experiments
```

Particularly relevant to distinguishing:

```text
same product model
```

from:

```text
same physical instance
```

---

# 23. Reusable Research Pattern — Patchwise Retrieval

**Classification:** `REFERENCE`

Use architectural ideas around:

```text
multi-scale patch embeddings
local regions
patch-level matching
```

to strengthen Dzoka's same-instance visual pipeline.

---

# 24. Reusable Research Pattern — Heavy Reranking

**Sources:**

```text
RerankingTransformer
ILIAS
instance-retrieval literature
```

**Classification:** `REFERENCE / ADAPT`

Recommended Dzoka pattern:

```text
cheap candidate retrieval
        ↓
structured scoring
        ↓
bounded expensive reranking
```

rather than applying expensive models to the full corpus.

---

# 25. Reusable Component — OCR

**Repository:** `PaddlePaddle/PaddleOCR`

**Classification:** `ADOPT dependency`

License:

```text
Apache-2.0
```

Use for:

```text
labels
serial fragments
names
document text
asset tags
branding
```

subject to privacy classification.

---

# 26. Reusable Component — Object Detection

**Preferred reference:** `PaddlePaddle/PaddleDetection`

**Classification:** `ADOPT / ADAPT`

License:

```text
Apache-2.0
```

Useful features include:

```text
RT-DETR
object detection
tracking
deployment tooling
MOT support
```

---

# 27. Alternative Detection Provider — Detectron2

**Classification:** `ADOPT optional provider`

License:

```text
Apache-2.0
```

Provide through:

```text
DetectionProvider
```

rather than coupling Dzoka to one framework.

---

# 28. Optional Detector — Ultralytics

**Classification:** `OPTIONAL / LICENSE-CONSCIOUS`

Technically useful, but AGPL implications mean it should not silently become Dzoka's required permissive-core dependency.

---

# 29. Reusable Matching Pattern — Hybrid Candidate Retrieval

**Source influence:** `globalenglish01/lostfound`

**Classification:** `ADAPT HEAVILY`

Use multiple retrieval strategies:

```text
semantic text
lexical
trigram
structured attributes
visual vector
perceptual
identifier
location
```

instead of one candidate-generation method.

---

# 30. Reusable Matching Pattern — Reciprocal Rank Fusion

**Source influence:** `globalenglish01/lostfound`

**Classification:** `ADAPT`

Conceptually:

```text
RRF(d) = Σ 1 / (k + rankᵢ(d))
```

Useful because heterogeneous retrieval scores do not necessarily share comparable scales.

---

# 31. Reusable Matching Pattern — Multi-Route Candidate Strength

**Classification:** `ADAPT`

A candidate discovered through:

```text
visual
+
semantic
+
geo
```

is often more interesting than a candidate discovered through only one weak route.

Retain retrieval-route provenance.

---

# 32. Reusable Matching Component — Hard Conflict Engine

**Source influence:** `globalenglish01/lostfound`

**Classification:** `ADAPT HEAVILY`

Conflicts must remain separate from general similarity.

Potential classes:

```text
CRITICAL
MAJOR
MINOR
```

---

# 33. Reusable Conflict — Protected Identifier Mismatch

**Classification:** `ADAPT`

Examples:

```text
IMEI A vs IMEI B
serial A vs serial B
passport ID A vs passport ID B
```

may eliminate candidates despite high semantic/visual similarity.

---

# 34. Reusable Conflict — Model/Subtype Incompatibility

**Classification:** `ADAPT`

Example:

```text
iPhone 15 Pro
vs
iPhone 15 Pro Max
```

may represent stronger negative evidence than semantic embeddings capture.

---

# 35. Reusable Matching Principle — Missing Evidence Is `NULL`

**Source influence:** `globalenglish01/lostfound`

**Classification:** `ADOPT AS INVARIANT`

Correct:

```text
available = false
score = null
```

not:

```text
score = 0
```

and not:

```text
score = 50
```

---

# 36. Reusable Component — Available-Evidence Normalization

**Classification:** `ADAPT`

Only available features should participate in score normalization.

Conceptually:

```text
Σ(weight × reliability × score)
──────────────────────────────
Σ(weight × reliability)
```

over available evidence.

---

# 37. Reusable Component — Evidence Coverage

**Sources:**

```text
globalenglish01/lostfound
Dzoka synthesis
```

**Classification:** `ADAPT`

Store separately:

```text
MatchScore
EvidenceCoverage
MatchConfidence
```

A sparse high score should not look equivalent to a richly evidenced high score.

---

# 38. Reusable Component — Evidence Thickness

**Source influence:** `globalenglish01/lostfound`

**Classification:** `ADAPT`

One shared attribute should not produce:

```text
100% attribute confidence
```

merely because the one comparable attribute matched.

Evidence quantity matters.

---

# 39. Reusable Component — Reliability-Weighted Evidence

**Classification:** `ADAPT`

Evidence source quality can influence contribution.

For example:

```text
verified serial
staff observation
OCR
vision model
user claim
inferred value
```

may carry different reliability.

Exact values must be calibrated by Dzoka rather than copied.

---

# 40. Reusable Component — Versioned Matching Profiles

**Sources:**

```text
globalenglish01/lostfound
Lost-and-Found-Tracking-System
```

**Classification:** `ADAPT`

Store configurable:

```text
weights
thresholds
category profiles
environment profiles
conflict rules
```

as versioned configuration.

---

# 41. Reusable Component — MatchingRun

**Source influence:** `globalenglish01/lostfound`

**Classification:** `ADAPT HEAVILY`

Every significant match operation should persist:

```text
MatchingRun
├── sourceFingerprintId
├── algorithmVersion
├── configurationVersion
├── modelVersions
├── retrievalConfig
├── candidateCount
├── duration
├── status
└── timestamps
```

---

# 42. Reusable Component — MatchEvidence

**Classification:** `BUILD / ADAPT`

Each candidate should preserve evidence such as:

```text
feature
score
availability
reliability
weight
source
modelRun
contribution
explanation
```

---

# 43. Reusable Component — Human Match Decisions as Feedback

**Source influence:** `globalenglish01/lostfound`

**Classification:** `ADAPT`

Confirmed/rejected candidates become valuable:

```text
positives
hard negatives
evaluation labels
```

for future ranking experiments.

---

# 44. Reusable Evaluation Pattern — Hard Negatives

**Classification:** `ADAPT`

Especially valuable:

```text
same brand
same model
same color
high embedding similarity
different physical item
```

These examples are critical for same-instance matching.

---

# 45. Reusable Storage Component — PostgreSQL

**Classification:** `ADOPT`

PostgreSQL is Dzoka's default authoritative relational data store.

---

# 46. Reusable Extension — PostGIS

**Classification:** `ADOPT operationally`

Use for:

```text
points
polygons
distance
facility boundaries
routes
location hierarchy support
```

---

# 47. Reusable Extension — pgvector

**Repository:** `pgvector/pgvector`

**Classification:** `ADOPT`

Use as Dzoka's default V1 vector-search provider.

Benefits:

```text
one operational database
HNSW
IVFFlat
SQL integration
transactional metadata
```

---

# 48. Reusable Extension — pg_trgm

**Classification:** `ADOPT`

Useful for:

```text
fuzzy lexical search
typos
partial names
multilingual fallback cases
```

---

# 49. Reusable Component — PostgreSQL Full-Text Search

**Classification:** `ADOPT`

Use initially alongside:

```text
pg_trgm
pgvector
PostGIS
```

before introducing a dedicated search cluster unnecessarily.

---

# 50. Optional Vector Backend — Qdrant

**Repository:** `qdrant/qdrant`

**Classification:** `ADOPT OPTIONAL`

License:

```text
Apache-2.0
```

Expose through:

```text
VectorSearchProvider
```

for scale-out deployments.

---

# 51. Reusable Provider Pattern — VectorSearchProvider

**Classification:** `BUILD`

Core matching must not depend directly on pgvector or Qdrant.

Interface concept:

```text
VectorSearchProvider
├── index()
├── search()
├── delete()
├── health()
└── capabilities()
```

---

# 52. Reusable Provider Pattern — AI Providers

**Classification:** `BUILD`

Recommended ports include:

```text
DetectionProvider
OCRProvider
ImageEmbeddingProvider
TextEmbeddingProvider
SpeechToTextProvider
TranslationProvider
LLMProvider
```

---

# 53. Reusable Component — ModelRun

**Sources:**

```text
globalenglish01/lostfound
Dzoka architecture synthesis
```

**Classification:** `ADAPT`

All significant AI outputs reference:

```text
ModelRun
├── provider
├── model
├── version
├── configuration
├── promptVersion
├── inputHash
├── outputSchema
├── latency
├── cost
└── status
```

---

# 54. Reusable Pattern — Side-by-Side Model Migration

**Source influence:** `globalenglish01/lostfound`

**Classification:** `ADAPT`

Do not overwrite existing embeddings immediately.

Instead:

```text
old model vectors
+
new model vectors
    ↓
evaluate
    ↓
switch active configuration
```

---

# 55. Reusable Ownership Pattern — Finder-Defined Private Questions

**Sources:**

```text
Malak-Rashad/LostAndFound
Smart Asset Finder
```

**Classification:** `ADAPT`

Finder/staff knowledge about:

```text
hidden marks
contents
stickers
damage
engraving
```

can become private ownership evidence.

---

# 56. Reusable Ownership Pattern — Private Evidence Inventory

**Classification:** `BUILD`

Dzoka generalizes private questions into:

```text
PrivateEvidence
├── type
├── value
├── classification
├── source
├── reliability
├── disclosureState
└── provenance
```

---

# 57. Reusable Component — VerificationChallenge

**Classification:** `BUILD`

Conceptually:

```text
VerificationChallenge
├── type
├── prompt
├── evidenceReference
├── discriminativeStrength
├── disclosureRisk
├── attemptPolicy
└── expiry
```

---

# 58. Reusable Component — Disclosure Guard

**Classification:** `BUILD`

Before issuing a challenge, verify that it does not leak:

```text
expected answer
private identifier
previously undisclosed evidence
```

---

# 59. Reusable Ownership Pattern — Human Review

**Sources:**

```text
Malak-Rashad/LostAndFound
Lost-and-Found-Tracking-System
Smart Asset Finder
```

**Classification:** `ADAPT`

Human judgment remains an important authority for ambiguous/high-risk cases.

---

# 60. Reusable Component — Typed Claim Evidence

**Sources:**

```text
Lost-and-Found-Tracking-System
FindBack
Smart Asset Finder
```

**Classification:** `ADAPT`

Possible types:

```text
PRIOR_PHOTO
RECEIPT
DOCUMENT
IDENTIFIER
PRIVATE_KNOWLEDGE
AUTHORIZATION
INSTITUTIONAL_RECORD
OTHER
```

---

# 61. Reusable Component — Claim

**Sources:**

```text
Lost-and-Found-Tracking-System
FindBack
Malak-Rashad/LostAndFound
```

**Classification:** `ADAPT`

Canonical fields should include:

```text
claimant
target item/report
status
submitted evidence
timestamps
decision references
```

---

# 62. Reusable Component — ClaimConflict

**Source influence:** `Lost-and-Found-Tracking-System`

**Classification:** `ADAPT`

Multiple plausible claimants should create an explicit:

```text
ClaimConflict
```

rather than be hidden inside one claim score.

---

# 63. Reusable Component — Approval Chain

**Source influence:** `Lost-and-Found-Tracking-System`

**Classification:** `ADAPT`

High-value or institutional items may require:

```text
reviewer A
    ↓
reviewer B
    ↓
release authorization
```

rather than one click.

---

# 64. Reusable Component — Fraud / Risk Signals

**Source influence:** `Lost-and-Found-Tracking-System`

**Classification:** `ADAPT`

Useful signals include:

```text
claim history
multiple unrelated claims
reused evidence
competing claims
failed challenges
identifier conflicts
```

but these belong in:

```text
RiskAssessment
```

not ownership truth.

---

# 65. Reusable Component — RiskAssessment

**Classification:** `BUILD`

Separate:

```text
ownership evidence
```

from:

```text
risk signals
```

so a risky claimant is not automatically treated as non-owner and vice versa.

---

# 66. Reusable Pattern — Institutional Organizations

**Source influence:** `Lost-and-Found-Tracking-System`

**Classification:** `ADAPT`

Useful concepts include:

```text
Organization
organization-submitted report
authorization proof
institutional reviewer
```

---

# 67. Reusable Component — Hierarchical Location

**Sources:**

```text
Lost-and-Found-Tracking-System
SmartFind campus model
globalenglish location hierarchy ideas
```

**Classification:** `ADAPT`

Represent:

```text
Organization
└── Site
    └── Building
        └── Floor
            └── Zone
                └── RecoveryPoint
```

where appropriate.

---

# 68. Reusable Component — GeoJSON / Polygon Zones

**Source influence:** `Lost-and-Found-Tracking-System`

**Classification:** `ADAPT`

Useful for:

```text
airport terminals
campus zones
mall areas
stations
event venues
```

Dzoka implements equivalent geometry using PostGIS.

---

# 69. Reusable Pattern — Draft Reports

**Source influence:** `Lost-and-Found-Tracking-System`

**Classification:** `ADAPT`

Support resumable report creation, especially for:

```text
PWA
WhatsApp
unstable connectivity
kiosks
```

---

# 70. Reusable Pattern — QR Item Identity

**Sources:**

```text
Smart Asset Finder
FindBack
```

**Classification:** `ADAPT`

Registered items may have:

```text
QR
NFC
RFID
BLE
```

identities that let a finder begin recovery without searching manually.

---

# 71. Reusable Pattern — Anonymous Finder Relay

**Source influence:** `Smart Asset Finder`

**Classification:** `ADAPT`

A person scanning/finding an item should be able to report it without necessarily registering a full Dzoka account.

Flow:

```text
scan tag
    ↓
privacy-safe recovery page
    ↓
submit sighting / message
    ↓
owner or custodian notified
```

---

# 72. Reusable Component — ItemIdentityProvider

**Classification:** `BUILD`

Abstraction:

```text
ItemIdentityProvider
├── QR
├── NFC
├── RFID
├── BLE
├── manufacturer serial
├── institutional tag
└── future digital passport
```

---

# 73. Reusable Pattern — QR Scan as Evidence, Not Truth

**Classification:** `ADAPT`

A scan can become:

```text
SightingEvidence
```

with:

```text
time
approximate location
device context
tag identity
```

but not automatically:

```text
verified custody
```

---

# 74. Reusable Pattern — Push Notifications

**Source influence:** `FindBack`

**Classification:** `ADAPT`

Push notifications are useful for:

```text
candidate found
claim submitted
claim reviewed
message
handover ready
recovery completed
```

---

# 75. Reusable Pattern — Multi-Channel Notifications

**Source influence:** `Smart Asset Finder`

**Classification:** `ADAPT`

Support:

```text
WhatsApp
push
email
SMS
in-app
```

behind one messaging architecture.

---

# 76. Reusable Component — MessagingProvider

**Classification:** `BUILD`

Provider abstraction:

```text
MessagingProvider
├── WhatsApp
├── Push
├── SMS
├── Email
└── future providers
```

---

# 77. Reusable Component — RecoveryConversation

**Sources:**

```text
Malak-Rashad/LostAndFound
FindBack
```

**Classification:** `BUILD / ADAPT`

Enable secure communication between authorized recovery participants without immediately exposing private contact details.

---

# 78. Reusable Pattern — Delayed Contact Disclosure

**Sources:**

```text
FuricBond
Malak-Rashad
Smart Asset Finder
```

**Classification:** `ADAPT`

Contact information should not be public by default.

Prefer:

```text
secure relay
```

and disclose direct contact only through explicit policy/consent.

---

# 79. Reusable Component — Handover Credential

**Sources:**

```text
FindBack
SmartFind conceptual lifecycle
```

**Classification:** `BUILD`

The concept is valuable even where upstream implementations are insecure.

Dzoka should implement:

```text
HandoverCredential
├── cryptographically random secret
├── stored verifier/hash
├── expiry
├── attempt limit
├── single-use
├── recovery binding
├── recipient binding
└── revocation
```

---

# 80. Reusable Component — Custody State Machine

**Classification:** `BUILD`

Upstream repositories demonstrate the need but none provide the full architecture.

Conceptually:

```text
FOUND
    ↓
IN_CUSTODY
    ↓
HELD
    ↓
AUTHORIZED_FOR_RELEASE
    ↓
IN_HANDOVER
    ↓
TRANSFERRED
    ↓
RECOVERY_CONFIRMED
```

with domain-specific states as finalized.

---

# 81. Reusable Component — CustodyEvent

**Classification:** `BUILD`

Append physical custody history:

```text
received
stored
moved
placed on hold
released
given to courier
delivered
returned
```

---

# 82. Reusable Component — RecoveryEvent

**Sources:**

```text
OpenEPCIS
GS1 traceability
Dzoka synthesis
```

**Classification:** `BUILD with standard adapter`

Canonical Dzoka history:

```text
RecoveryEvent
```

with optional mapping to EPCIS.

---

# 83. Reusable Standard — EPCIS 2.0

**Repository influence:** `openepcis/epcis-repository-ce`

**Classification:** `ADOPT STANDARD / OPTIONAL ADAPTER`

Useful for:

```text
capture
query
traceability
event semantics
external integrations
```

Do not force Dzoka's entire domain model to become EPCIS.

---

# 84. Reusable Component — EPCISAdapter

**Classification:** `BUILD`

Map:

```text
Dzoka RecoveryEvent
↔
EPCIS Event
```

for organizations requiring standards-based traceability.

---

# 85. Reusable Federation Pattern — Independent Nodes

**Sources:**

```text
Mycel
Open Federated Search
```

**Classification:** `ADAPT`

Dzoka federation should support institutions operating independent nodes while cooperating through secure search/query protocols.

---

# 86. Reusable Federation Component — Node Identity

**Classification:** `BUILD / ADAPT`

Each Dzoka node needs:

```text
nodeId
public keys
endpoint
capabilities
trust state
organization
```

---

# 87. Reusable Federation Component — Peer Allowlist / Trust

**Source influence:** `Mycel`

**Classification:** `ADAPT`

Peers should be explicitly trusted rather than accepted solely because they have a URL.

---

# 88. Reusable Federation Pattern — Per-Peer Timeouts

**Source influence:** `Mycel`

**Classification:** `ADAPT`

One slow institution must not stall the whole search.

---

# 89. Reusable Federation Pattern — Circuit Breakers

**Source influence:** `Mycel`

**Classification:** `ADAPT`

Temporarily stop sending traffic to consistently failing peers.

---

# 90. Reusable Federation Pattern — Source Attribution

**Classification:** `ADAPT`

Every remote candidate retains:

```text
sourceNode
remoteCandidateId
remoteScoreLabel
receivedAt
```

---

# 91. Reusable Federation Pattern — Rank Fusion

**Source influence:** `Mycel`

**Classification:** `ADAPT`

Because node scores are not necessarily comparable, merge candidates using:

```text
round-robin
rank fusion
source-aware ranking
```

rather than naive raw-score sorting.

---

# 92. Reusable Federation Component — Capability Discovery

**Classification:** `ADAPT`

A node should advertise capabilities such as:

```text
text search
visual search
private verification
EPCIS
courier integration
supported categories
```

---

# 93. Reusable Federation Boundary — Gateway

**Classification:** `BUILD`

External federation traffic enters through:

```text
dzoka-federation
```

rather than directly accessing:

```text
database
internal Kafka
internal services
```

---

# 94. Reusable Pattern — Signed Federation Messages

**Classification:** `BUILD`

Use authenticated:

```text
signatures
timestamps
nonces
replay protection
```

for important cross-node operations.

---

# 95. Reusable CCTV Pattern — Coarse-to-Fine Search

**Source:** `cctv-theft-finder`

**Classification:** `ADAPT`

Search disappearance events progressively:

```text
day/night
    ↓
hour
    ↓
minute
    ↓
second
```

instead of brute-forcing every frame.

---

# 96. Reusable CCTV Pattern — Empty-Spot Calibration

**Source influence:** `cctv-theft-finder`

**Classification:** `ADAPT`

Use reference observations to determine when an item is:

```text
present
occluded
gone
```

in a known monitored location.

---

# 97. Reusable CCTV Pattern — Occlusion Rechecks

**Classification:** `ADAPT`

Temporary occlusion should not immediately be interpreted as disappearance.

---

# 98. Reusable CCTV Pattern — Event Clips

**Classification:** `ADAPT`

Instead of centralizing full continuous footage, retain/upload:

```text
event clip
key frames
track metadata
timestamps
```

where policy permits.

---

# 99. Reusable Edge Pattern — Local Processing

**Classification:** `BUILD / ADAPT`

CCTV and heavy sensor processing should preferably occur at:

```text
edge
```

with central Dzoka receiving selected evidence.

---

# 100. Reusable Movement Intelligence Pattern — State Transitions

**Classification:** `BUILD`

For recovery networks, useful movement intelligence is often:

```text
FOUND_AT_GATE
→ SECURITY_DESK
→ CENTRAL_LOST_PROPERTY
→ COURIER
→ OWNER
```

rather than raw x/y trajectory prediction.

---

# 101. Reusable Research Source — Trajectron++

**Classification:** `REFERENCE`

Useful for understanding probabilistic movement prediction techniques, but not a direct lost-property movement model.

---

# 102. Reusable Component — Recovery Graph

**Classification:** `BUILD`

Represent relationships among:

```text
items
locations
organizations
custodians
routes
reports
events
```

initially using PostgreSQL relational projections.

---

# 103. Reusable Pattern — Relational Graph First

**Classification:** `ADAPT`

Do not introduce a dedicated graph database until evidence shows PostgreSQL projections are insufficient.

---

# 104. Reusable Reward Pattern — Append-Oriented Ledger

**Sources:**

```text
Smart Asset Finder
OpenEscrow conceptual lessons
```

**Classification:** `ADAPT`

Store:

```text
RewardLedgerEntry
```

rather than only a mutable:

```text
reward_balance
```

---

# 105. Reusable Reward Pattern — Reward After Recovery

**Classification:** `BUILD`

Default flow:

```text
RecoveryCompleted
    ↓
RewardEligibility
    ↓
RewardRelease
```

rather than rewarding merely:

```text
match
claim
QR scan
```

---

# 106. Reusable Reward Component — Reward Policy

**Classification:** `BUILD`

Allow deployment-specific policy:

```text
fixed reward
owner-offered reward
points
recognition
no reward
```

---

# 107. Reusable State-Machine Inspiration — OpenEscrow

**Classification:** `REFERENCE`

Useful concepts include:

```text
state transition
evidence
dispute
release
```

without adopting blockchain as default architecture.

---

# 108. Reusable Payment Pattern — Provider Abstraction

**Classification:** `BUILD`

Use:

```text
PaymentProvider
```

for:

```text
authorize
verify
refund
settle
```

rather than embedding payment-provider semantics into the reward domain.

---

# 109. Reusable Product Pattern — Registered Assets

**Source influence:** `Smart Asset Finder`

**Classification:** `ADAPT`

Users may optionally register an item before loss:

```text
Register
    ↓
Fingerprint
    ↓
QR/NFC identity
    ↓
Future recovery
```

This can dramatically reduce recovery time.

---

# 110. Reusable Product Pattern — Digital Item Passport

**Classification:** `FUTURE / OPTIONAL`

Possible future support:

```text
manufacturer identity
QR
NFC
institutional asset ID
DID / VC
digital product passport
```

behind replaceable providers.

---

# 111. Reusable DID/VC Research

**Source influence:** `dpp-did`

**Classification:** `REFERENCE`

Useful for future verifiable digital-item identity.

Not mandatory for ordinary Dzoka operation.

---

# 112. Reusable Component — Multi-Tenancy

**Classification:** `BUILD`

Institutional workflows across:

```text
airports
universities
malls
hotels
transport operators
government
companies
```

require explicit tenant isolation.

---

# 113. Reusable Tenant Rule — Tenant IDs Everywhere Important

Persist:

```text
tenantId
organizationId
```

where domain ownership requires them.

Potential PostgreSQL RLS may provide defense in depth.

---

# 114. Reusable Component — Policy Engine Boundary

**Classification:** `BUILD / OPTIONAL OPA`

Complex release, privacy, federation, and institutional rules should be represented through:

```text
Policy
```

rather than scattered `if` statements.

OPA may be evaluated as an implementation option.

---

# 115. Reusable Identity Pattern — OIDC

**Classification:** `ADOPT STANDARD`

Dzoka should avoid building all authentication from scratch.

Use an:

```text
OIDC-compatible IdP
```

such as Keycloak-compatible infrastructure.

---

# 116. Reusable Security Pattern — Server-Derived Actor Identity

**Classification:** `ADOPT AS INVARIANT`

Never trust client-supplied:

```text
senderId
reviewerId
approvedBy
tenantId
```

when server authentication can determine it.

---

# 117. Reusable Security Pattern — Explicit Commands

**Classification:** `ADAPT`

Prefer:

```text
ApproveClaim
RejectClaim
TransferCustody
ConfirmHandover
```

over:

```text
PATCH object
{
  status: ...
}
```

for authoritative state transitions.

---

# 118. Reusable Security Pattern — Idempotency Keys

**Classification:** `BUILD`

Critical actions require idempotency.

Examples:

```text
SubmitClaim
CreateShipment
ReleaseReward
ConfirmRecovery
ProcessWebhook
```

---

# 119. Reusable Security Pattern — HMAC/Keyed Search for Low-Entropy IDs

**Classification:** `BUILD`

Where protected identifiers need equality search, prefer keyed representations rather than plain unsalted hashes.

---

# 120. Reusable Data Pattern — Object Storage for Binary Media

**Classification:** `ADOPT`

Use:

```text
S3 / MinIO
```

for:

```text
images
videos
receipts
claim evidence
CCTV clips
generated derivatives
```

---

# 121. Reusable Data Pattern — Media Metadata in PostgreSQL

**Classification:** `ADAPT`

Store:

```text
mediaId
objectKey
contentType
hash
size
classification
owner
lineage
status
```

in authoritative relational storage.

---

# 122. Reusable Component — Media Derivatives

**Classification:** `BUILD`

Represent:

```text
original
sanitized
thumbnail
object crop
OCR input
redacted
AI input
```

with lineage.

---

# 123. Reusable Component — Content Hashing

**Source influence:** `globalenglish01/lostfound`

**Classification:** `ADAPT`

Useful for:

```text
media deduplication
embedding deduplication
AI input identity
lineage
```

---

# 124. Reusable Data Pattern — Unknown Is Explicit

**Classification:** `ADOPT AS INVARIANT`

Do not overload:

```text
empty
false
0
```

to represent unknown.

Use:

```text
NULL / UNKNOWN / unavailable
```

explicitly.

---

# 125. Reusable Data Pattern — Derived vs Canonical Data

**Classification:** `BUILD`

Canonical examples:

```text
reports
claims
custody transfers
verification decisions
```

Derived examples:

```text
embeddings
OCR
search vectors
thumbnails
candidate rankings
graph projections
```

Derived data should generally be rebuildable.

---

# 126. Reusable Data Pattern — Immutable Decision Snapshots

**Classification:** `ADAPT`

Historical decisions should retain the evidence/configuration that existed at decision time.

Do not allow model upgrades to rewrite decision history.

---

# 127. Reusable Data Pattern — Legal Holds

**Classification:** `BUILD`

Retention/deletion logic must support:

```text
legal hold
institutional hold
investigation
```

overrides.

---

# 128. Reusable Event Pattern — Transactional Outbox

**Classification:** `ADOPT ARCHITECTURAL PATTERN`

Transactional state and integration events should commit together through:

```text
business transaction
+
outbox record
```

---

# 129. Reusable Event Pattern — Inbox / Deduplication

**Classification:** `ADAPT`

Critical consumers should track received event IDs or idempotency keys.

---

# 130. Reusable Event Pattern — At-Least-Once + Idempotency

**Classification:** `ADOPT AS DELIVERY MODEL`

Assume duplicates may occur.

Design consumers accordingly.

---

# 131. Reusable Event Pattern — Correlation and Causation

**Classification:** `ADAPT`

Every distributed workflow should retain:

```text
correlationId
causationId
traceId
```

where appropriate.

---

# 132. Reusable Event Component — Event Envelope

**Classification:** `BUILD`

Recommended fields:

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
metadata
```

---

# 133. Reusable Event Pattern — Retry + DLQ

**Classification:** `ADOPT`

Use:

```text
bounded retries
exponential backoff
jitter
DLQ
manual replay
```

rather than infinite retries.

---

# 134. Reusable Event Pattern — Process Managers / Sagas

**Classification:** `BUILD`

Useful for:

```text
EnrichmentProcess
MatchingProcess
VerificationProcess
RecoveryProcess
CourierProcess
RewardReleaseProcess
FederatedSearchProcess
```

---

# 135. Reusable Event Pattern — No Distributed Global Transaction

**Classification:** `ADAPT`

Multi-step external workflows should use:

```text
state machines
process managers
compensation
idempotency
```

rather than pretending external providers share one ACID transaction.

---

# 136. Reusable Queue/Streaming Infrastructure — Kafka-Compatible Broker

**Classification:** `ADOPT CATEGORY`

Use a Kafka-compatible event backbone.

Exact:

```text
Kafka
Redpanda
```

choice remains subject to source audit/ADR.

---

# 137. Reusable Cache — Redis

**Classification:** `ADOPT`

Appropriate for:

```text
OTP
temporary sessions
locks
short-lived cache
idempotency helpers
rate-limit state
```

not authoritative recovery state.

---

# 138. Reusable Observability Standard — OpenTelemetry

**Classification:** `ADOPT`

Use for:

```text
distributed traces
metrics context
correlation
```

---

# 139. Reusable Observability Stack

**Classification:** `ADOPT / DEPLOYMENT OPTION`

Suggested components:

```text
Prometheus
Grafana
Loki
Tempo
OpenTelemetry
```

---

# 140. Reusable ML Governance — MLflow

**Classification:** `OPTIONAL ADOPT`

Useful for:

```text
experiments
model registration
metrics
artifact lineage
```

where the AI team requires it.

---

# 141. Reusable Deployment Pattern — Docker Compose

**Classification:** `ADOPT`

Suitable for:

```text
local development
demo
small institutional deployment
```

---

# 142. Reusable Deployment Pattern — Kubernetes / Helm

**Classification:** `ADOPT OPTIONAL`

Suitable when:

```text
scale
availability
GPU workloads
multiple institutions
```

justify orchestration complexity.

---

# 143. Reusable Core Architecture — Modular Monolith

**Classification:** `ADOPT`

Dzoka's transactional core should initially remain:

```text
Java 21+
Spring Boot
modular monolith
```

with strong bounded-context modules.

---

# 144. Why Modular Monolith Is Reusable

The audited repositories reinforce that much core recovery logic is:

```text
transactional
stateful
highly connected
```

Splitting it prematurely into many distributed services creates unnecessary consistency problems.

---

# 145. Reusable Separation — AI Runtime

**Classification:** `ADOPT`

AI workloads belong in a separate:

```text
Python + FastAPI
```

runtime where:

```text
ML dependencies
GPU
model loading
Python ecosystem
```

are natural.

---

# 146. Reusable Separation — Matching Runtime

**Classification:** `BUILD`

Matching has different scaling/performance characteristics and deserves its own logical/deployable boundary:

```text
dzoka-matching
```

even if initially co-deployed with AI.

---

# 147. Reusable Separation — Messaging Gateway

**Classification:** `BUILD`

All channel/vendor integrations flow through:

```text
dzoka-messaging
```

rather than entering domain logic directly.

---

# 148. Reusable Separation — Federation Gateway

**Classification:** `BUILD`

External peer communication flows through:

```text
dzoka-federation
```

with node authentication, privacy controls, and protocol translation.

---

# 149. Reusable Separation — Edge Runtime

**Classification:** `BUILD`

CCTV/sensor operations use:

```text
dzoka-edge
```

for privacy, hardware access, bandwidth, and offline tolerance.

---

# 150. Reusable Worker Architecture

**Classification:** `BUILD`

Background workloads include:

```text
AI enrichment
matching
notifications
federation
courier
reward
retention
ML feedback
```

---

# 151. Reusable API Pattern — Signed Media Uploads

**Classification:** `ADAPT`

Clients should upload large media directly to object storage using controlled signed upload flows rather than routing large binaries through every application service.

---

# 152. Reusable API Pattern — Privacy-Safe Projections

**Classification:** `BUILD`

Use separate representations such as:

```text
PublicItemView
ClaimantItemView
ReviewerItemView
CustodianItemView
FederatedCandidateView
```

rather than one universal DTO.

---

# 153. Reusable Product Pattern — WhatsApp-First Recovery

**Sources:**

```text
kwasu-lost-found-bot
Smart Asset Finder
```

**Classification:** `ADAPT`

WhatsApp should be a full interaction channel for:

```text
reporting
search
claims
verification
notifications
recovery
```

not merely outbound alerts.

---

# 154. Reusable Product Pattern — Voice Input

**Classification:** `BUILD`

Useful for users with:

```text
low literacy
limited typing
accessibility needs
```

through speech-to-text and structured follow-up questions.

---

# 155. Reusable Product Pattern — Kiosk

**Classification:** `BUILD`

Useful for:

```text
airports
universities
malls
stations
government institutions
```

where public users may not have an account/device.

---

# 156. Reusable Product Pattern — Progressive Reporting

**Classification:** `ADAPT`

Do not force a user to complete every field before a report exists.

Support:

```text
initial report
    ↓
progressive enrichment
    ↓
additional photos
    ↓
identifier
    ↓
private evidence
```

---

# 157. Reusable Product Pattern — Multi-View Capture

**Sources:**

```text
FuricBond
Dzoka visual synthesis
```

**Classification:** `ADAPT`

Encourage:

```text
front
back
side
underside
distinctive feature
label/serial
```

views.

---

# 158. Reusable Product Pattern — Recovery Timeline

**Sources:**

```text
SmartFind
FindBack
```

**Classification:** `ADAPT`

Show users a real projected timeline derived from domain events:

```text
Reported
Potential Match
Claim Submitted
Verification
Authorized
Ready for Handover
Transferred
Recovered
```

---

# 159. Reusable Product Pattern — Recovery Reputation

**Source influence:** `FindBack`

**Classification:** `ADAPT`

Trust/reputation may be derived from verified outcomes such as:

```text
successful return
helpful finder contribution
verified institutional actions
```

not merely app activity.

---

# 160. Reusable Product Pattern — Community Recognition

**Source influence:** `Smart Asset Finder`

**Classification:** `OPTIONAL ADAPT`

Badges/recognition may encourage useful participation, provided they do not create incentives for fraud.

---

# 161. Reusable Product Pattern — Travel Context

**Source influence:** `Smart Asset Finder`

**Classification:** `ADAPT`

Reports should capture context such as:

```text
travelling
transit
airport
bus
rail
hotel
```

because recovery movement differs from static local loss.

---

# 162. Reusable Component — TimeWindow

**Sources:**

```text
globalenglish01/lostfound
Dzoka synthesis
```

**Classification:** `ADAPT`

Model uncertain user time descriptions as:

```text
TimeWindow
├── start
├── end
├── precision
├── timezone
└── rawExpression
```

---

# 163. Reusable Matching Pattern — Environment-Specific Geo Decay

**Source influence:** `globalenglish01/lostfound`

**Classification:** `ADAPT`

Location relevance differs by:

```text
airport
station
office
stadium
mall
bus route
```

Use environment-aware matching profiles.

---

# 164. Reusable Matching Pattern — Category-Specific Profiles

**Classification:** `ADAPT`

A passport and umbrella need different weighting.

Example:

```text
passport:
identifier evidence very high

umbrella:
appearance + location/time higher
```

---

# 165. Reusable Pattern — Category Hypotheses

**Classification:** `BUILD / ADAPT`

When classification is uncertain, preserve multiple plausible categories rather than forcing one value too early.

---

# 166. Reusable Pattern — Bounding Expensive AI

**Sources:**

```text
globalenglish01/lostfound
retrieval/reranking audits
```

**Classification:** `ADOPT AS PRINCIPLE`

Use heavy models only for a bounded candidate shortlist.

---

# 167. Reusable Pattern — AI Data Minimization

**Source influence:** `globalenglish01/lostfound`

**Classification:** `ADAPT`

Before calling an AI provider, build the smallest permitted input view.

Do not send entire domain entities blindly.

---

# 168. Reusable Component — AIProviderInputPolicy

**Classification:** `BUILD`

Determine which fields:

```text
PUBLIC
RESTRICTED
SECRET
```

may go to which provider/model.

---

# 169. Reusable Pattern — Human Feedback for Model Evaluation

**Classification:** `ADAPT`

Capture:

```text
accepted match
rejected match
successful recovery
failed verification
human override
```

as structured ML feedback.

---

# 170. Reusable Component — MLFeedback

**Classification:** `BUILD`

Suggested fields:

```text
sourceDecision
candidate
label
reason
reviewer
datasetEligibility
createdAt
```

---

# 171. Reusable Pattern — Evaluation Before Model Promotion

**Classification:** `BUILD`

A new matcher/model should be evaluated against:

```text
labeled positives
hard negatives
multilingual samples
poor images
same-model different-instance pairs
```

before becoming active.

---

# 172. Reusable Pattern — Model Coexistence

**Classification:** `ADAPT`

Support:

```text
CURRENT
CANDIDATE
DEPRECATED
```

models simultaneously during migration/testing.

---

# 173. Reusable Pattern — Search Backend Coexistence

**Classification:** `BUILD`

Allow PgVector and Qdrant providers to coexist during migrations or scale tests.

---

# 174. Reusable Pattern — Observability for Matching

**Classification:** `BUILD`

Measure:

```text
candidate retrieval latency
candidate count
feature availability
reranker latency
model failure rate
match precision/recall
coverage distribution
```

---

# 175. Reusable Pattern — Observability for Events

**Classification:** `BUILD`

Monitor:

```text
outbox backlog
consumer lag
retry rate
DLQ size
processing duration
event failure
```

---

# 176. Reusable Pattern — Audit Separate from Logs

**Classification:** `BUILD`

Application logs are operational.

Audit history is business/security evidence.

Keep them distinct.

---

# 177. Reusable Pattern — Append-Oriented Audit

**Sources:**

```text
Lost-and-Found-Tracking-System
FindBack
```

**Classification:** `ADAPT`

Important actions should produce durable audit records.

Examples:

```text
claim submitted
claim reviewed
verification decision
custody transfer
configuration changed
node trust changed
```

---

# 178. Reusable Data Governance Pattern — Retention Policy

**Source influence:** `Lost-and-Found-Tracking-System`

**Classification:** `ADAPT`

Data classes should define:

```text
retention
archive
anonymization
deletion
legal hold
```

---

# 179. Reusable Pattern — Data Deletion Propagation

**Classification:** `BUILD`

Deletion may need to propagate to:

```text
media
derived images
OCR
embeddings
search indexes
cache
federated replicas
```

unless retention/legal rules prevent it.

---

# 180. Reusable Pattern — Human-Readable References

**Sources:** several audited platforms

**Classification:** `ADAPT`

Use internal UUIDs plus user-facing references such as:

```text
DZ-2026-...
```

or equivalent.

---

# 181. Reusable ID Strategy — UUIDv7 Candidate

**Classification:** `ADR REQUIRED`

UUIDv7 is a strong candidate for internal identifiers because it provides:

```text
global uniqueness
rough temporal ordering
```

Final decision should be captured in an ADR.

---

# 182. Reusable Pattern — Rebuildable Search Projection

**Classification:** `BUILD`

Search documents and vector indexes should be treated as derived projections that can be regenerated from canonical state.

---

# 183. Reusable Pattern — Human Review Queues

**Sources:**

```text
Lost-and-Found-Tracking-System
Smart Asset Finder
Malak-Rashad
```

**Classification:** `ADAPT`

Queues may include:

```text
ambiguous matches
competing claims
high-risk claims
identity documents
custody exceptions
```

---

# 184. Reusable Pattern — Dual Control for Sensitive Items

**Classification:** `BUILD`

High-risk recovery may require two separate approvals.

Useful for:

```text
passports
high-value electronics
government documents
institutional assets
```

---

# 185. Reusable Pattern — Anonymous Reporting

**Sources:** several consumer lost/found implementations

**Classification:** `ADAPT`

Allow limited anonymous/public reporting where policy permits, while restricting sensitive workflows.

---

# 186. Reusable Pattern — Proactive Registration

**Classification:** `ADAPT`

Users can register important assets before loss.

This creates stronger:

```text
pre-loss fingerprint
ownership evidence
serial linkage
tag identity
```

later.

---

# 187. Reusable Pattern — Pre-Loss Evidence

**Classification:** `BUILD`

Pre-loss data can be especially valuable for ownership verification:

```text
photos
serial
receipt
hidden features
purchase date
```

---

# 188. Reusable Component — Protected Identifier Registry

**Classification:** `BUILD`

Store protected item identifiers separately with safe lookup mechanisms.

---

# 189. Reusable Pattern — Evidence Disclosure State

**Classification:** `BUILD`

Private evidence should track whether it has already been disclosed during:

```text
public listing
claim challenge
staff communication
previous claim
```

---

# 190. Reusable Pattern — Challenge Rotation

**Classification:** `BUILD`

Do not repeatedly ask the same private question after failed attempts.

---

# 191. Reusable Pattern — Evidence Entropy / Guessability

**Classification:** `BUILD`

Challenge strength should consider whether an answer is easy to guess.

Example:

```text
What color?
```

is weaker than:

```text
Describe the unusual mark inside the case.
```

---

# 192. Reusable Pattern — Strong Identifier Evidence

**Classification:** `ADAPT`

Strong identity fields may have disproportionate importance.

Examples:

```text
serial
IMEI
asset tag
registration
```

but must be protected.

---

# 193. Reusable Pattern — Evidence Reliability Separate from Confidence

**Classification:** `BUILD`

Example:

```text
vision confidence = 0.99
```

does not mean:

```text
identity reliability = 0.99
```

These are different concepts.

---

# 194. Reusable Pattern — Confidence Calibration

**Classification:** `BUILD`

If Dzoka presents actual probabilities, calibrate them using labeled data.

Possible methods may include:

```text
Platt scaling
isotonic regression
calibration curves
```

subject to model/evaluation design.

---

# 195. Reusable Pattern — Remote Candidate Safe Snapshot

**Classification:** `BUILD`

Federated candidates should be stored locally only as the minimum safe snapshot required for workflow continuity, with expiry where appropriate.

---

# 196. Reusable Pattern — Data Sovereignty

**Classification:** `BUILD`

Federated deployment must support organizations or jurisdictions where:

```text
raw data must remain local
```

while safe candidate discovery still works.

---

# 197. Reusable Pattern — Optional Self-Hosting

**Classification:** `BUILD`

Dzoka should support:

```text
self-hosted institution
central SaaS
federated institution network
hybrid deployment
```

---

# 198. Reusable Pattern — Capability Degradation

**Classification:** `BUILD`

Dzoka should continue functioning when optional systems are unavailable.

Examples:

```text
Qdrant down
→ pgvector fallback where configured

LLM down
→ deterministic matching continues

WhatsApp down
→ other notifications continue

remote nodes down
→ local recovery continues
```

---

# 199. Reusable Pattern — Provider Failure Visibility

**Classification:** `BUILD`

A failed AI/provider operation should produce:

```text
FAILED
TIMED_OUT
RATE_LIMITED
```

not silently become:

```text
no result
```

---

# 200. Reusable Pattern — Capability Status

**Classification:** `BUILD`

For major subsystems use explicit implementation states:

```text
IMPLEMENTED
PARTIAL
EXPERIMENTAL
PLACEHOLDER
DISABLED
PLANNED
```

This avoids README/source divergence.

---

# 201. Reusable Pattern — Source Audit Before Architectural Adoption

**Classification:** `MANDATORY PROCESS`

Before adding a major upstream dependency or architectural foundation:

```text
inspect actual source
verify license
pin commit
identify runtime path
inspect security
inspect tests
classify reuse
```

---

# 202. Reusable Pattern — Exact Commit Pinning

**Classification:** `MANDATORY PROCESS`

Every source audit must pin:

```text
full SHA
```

in:

```text
docs/source-audit/source-audit.lock.json
```

---

# 203. Reusable Pattern — Files Inspected

**Classification:** `MANDATORY PROCESS`

Every audit must record which implementation paths were actually examined.

---

# 204. Reusable Pattern — Runtime vs Repository Distinction

**Source lesson:** FindBack

**Classification:** `MANDATORY PROCESS`

When multiple implementations exist, identify:

```text
which code actually runs
```

based on:

```text
deployment configuration
imports
build configuration
entrypoints
```

---

# 205. Reusable Pattern — Documentation Claims Verification

**Classification:** `MANDATORY PROCESS`

For claimed capabilities such as:

```text
AI
encryption
E2EE
fraud detection
biometrics
vector search
```

trace the implementation before treating the capability as real.

---

# 206. Source-by-Source Reuse Summary

| Source | Main reusable contribution | Classification |
|---|---|---|
| `globalenglish01/lostfound` | matching/evidence architecture | `ADAPT HEAVILY` |
| `FuricBond/Lost-And-Found` | multimodal visual concepts | `REFERENCE / ADAPT CONCEPTS` |
| `Lost-and-Found-Tracking-System` | claims, institutional workflow, risk | `ADAPT` |
| `Malak-Rashad/LostAndFound` | private challenge verification | `REFERENCE / ADAPT CONCEPTS` |
| `Smart Asset Finder` | QR, WhatsApp, incentives | `REFERENCE` |
| `FindBack` | recovery UX, push, handover concepts | `ADAPT SELECTIVELY` |
| `ILIAS` | instance retrieval research | `ADAPT / REFERENCE` |
| `Patchwise Retrieval` | local patch retrieval | `REFERENCE` |
| `OpenCLIP` | visual embedding foundation | `ADOPT` |
| `pgvector` | default V1 vector search | `ADOPT` |
| `Qdrant` | optional scale-out vectors | `ADOPT OPTIONAL` |
| `PaddleOCR` | OCR | `ADOPT` |
| `PaddleDetection` | detection/tracking | `ADOPT` |
| `CCTV Theft Finder` | coarse-to-fine CCTV search | `ADAPT` |
| `Trajectron++` | movement research | `REFERENCE` |
| `Mycel` | federation architecture | `REFERENCE / ADAPT` |
| `Open Federated Search` | historical federation concepts | `REFERENCE` |
| `OpenEPCIS` | traceability/EPCIS | `ADOPT STANDARD / ADAPTER` |
| `GS1 Traceability` | traceability semantics | `STANDARD REFERENCE` |
| `OpenEscrow` | escrow/state-machine ideas | `REFERENCE` |

---

# 207. Recommended V1 Adopted Infrastructure

Based on current audits, the strongest default stack is:

```text
PostgreSQL
├── PostGIS
├── pgvector
├── pg_trgm
└── Full-Text Search

MinIO / S3

Redis

Kafka-compatible broker

OpenTelemetry

Prometheus
Grafana
Loki
Tempo
```

Optional:

```text
Qdrant
MLflow
OPA
```

subject to deployment needs and ADRs.

---

# 208. Recommended V1 AI Foundation

```text
PaddleDetection
    ↓
object detection / region extraction

PaddleOCR
    ↓
text evidence

OpenCLIP
    ↓
global visual embeddings

local-instance feature pipeline
    ↓
Dzoka-specific / research-derived

LLMProvider
    ↓
optional interpretation / reranking / challenges
```

---

# 209. Recommended V1 Matching Foundation

```text
ItemFingerprint
       ↓
Candidate Retrieval
├── FTS
├── trigram
├── pgvector
├── geo
├── attributes
└── identifiers
       ↓
Rank Fusion
       ↓
Hard Conflict Engine
       ↓
Feature Evidence
       ↓
Reliability
       ↓
Evidence Coverage
       ↓
Bounded Heavy Reranking
       ↓
Confidence / Calibration
       ↓
Explainable MatchCandidate
```

---

# 210. Recommended Verification Foundation

```text
Claim
    ↓
Private Evidence Inventory
    ↓
VerificationSession
    ↓
Challenge Selection
    ↓
Disclosure Guard
    ↓
Claimant Response
    ↓
Evidence Evaluation
    ↓
RiskAssessment
    ↓
Policy
    ↓
Human Review where required
    ↓
VerificationDecision
```

---

# 211. Recommended Recovery Foundation

```text
VerificationDecision
        ↓
RecoveryAuthorization
        ↓
Custody State
        ↓
Handover Credential
        ↓
Credential Verification
        ↓
CustodyTransfer
        ↓
RecipientConfirmation
        ↓
RecoveryCompleted
        ↓
Reward / ML Feedback
```

---

# 212. Recommended Federation Foundation

```text
Local Node
    ↓
Federation Gateway
    ↓
Signed Query
    ↓
Trusted Peers
    ↓
Per-Peer Timeout
    ↓
Candidate Responses
    ↓
Source Attribution
    ↓
Rank Fusion
    ↓
Local Review / Verification
```

---

# 213. Recommended CCTV Foundation

```text
VMS / ONVIF
    ↓
Edge Connector
    ↓
Coarse-to-Fine Search
    ↓
Detection / Tracking
    ↓
Item Re-identification
    ↓
Relevant Evidence Clip
    ↓
Privacy Redaction
    ↓
Central Recovery Evidence
```

---

# 214. Recommended Messaging Foundation

```text
Channel
├── Web
├── Mobile
├── WhatsApp
├── SMS
├── Email
└── Kiosk
        ↓
Messaging Gateway
        ↓
Normalized command
        ↓
Dzoka application/domain
```

Responses flow through provider adapters.

---

# 215. Recommended Observability Foundation

Every major operation should expose:

```text
trace
metrics
structured logs
audit where required
```

with:

```text
correlationId
tenantId
caseId
matchingRunId
modelRunId
```

as appropriate.

---

# 216. Recommended Testing Foundation

Source audits strongly support tests around architectural invariants.

Required categories include:

```text
domain state-machine tests
matching invariant tests
security authorization tests
privacy projection tests
provider failure tests
event duplicate/replay tests
federation trust tests
custody/handover tests
AI/model provenance tests
```

---

# 217. Required Matching Regression Tests

Examples:

```text
missing image ≠ mismatch

serial conflict overrides visual similarity

same model different physical item remains hard negative

low evidence coverage caps confidence

different embedding versions are not compared

category disagreement does not automatically remove candidate
```

---

# 218. Required Verification Regression Tests

Examples:

```text
publicly disclosed answer has reduced challenge value

claimant cannot see expected evidence

unauthorized reviewer cannot inspect secrets

challenge retries obey policy

AI cannot authorize release

competing claims place recovery hold
```

---

# 219. Required Recovery Regression Tests

Examples:

```text
claim approval does not mark recovered

handover credential requires authorization

expired credential rejected

used credential rejected

duplicate recovery completion is idempotent

custody history remains immutable
```

---

# 220. Required Federation Regression Tests

Examples:

```text
unsigned peer message rejected

replayed message rejected

slow peer does not stall query

remote score not compared as local probability

untrusted peer denied

PII minimized
```

---

# 221. Build vs Reuse Summary

Dzoka should **adopt** mature infrastructure where possible:

```text
PostgreSQL
PostGIS
pgvector
OpenCLIP
PaddleOCR
PaddleDetection
Qdrant optionally
OpenTelemetry
```

Dzoka should **adapt** proven architectural patterns:

```text
hybrid retrieval
RRF
hard conflicts
private challenges
claim conflicts
GeoJSON-style institutional zones
FCM-style notifications
QR recovery
coarse-to-fine CCTV search
federation timeouts/circuit breakers
```

Dzoka must **build** the integration layer that makes these a coherent recovery network:

```text
ItemFingerprint
Matching Engine
Verification Engine
RecoveryAuthorization
Custody State Machine
Federation Protocol
Recovery Graph
Movement Intelligence
WhatsApp-first orchestration
Reward orchestration
```

---

# 222. What No Upstream Source Solves Completely

Current source audits have not identified one mature upstream implementation covering all of:

```text
multimodal same-instance matching

private dynamic ownership challenges

claim conflicts

formal custody

secure handover

institutional federation

movement prediction

CCTV edge

QR/NFC identity

courier orchestration

reward release

privacy-aware AI

multi-tenancy
```

This is precisely where Dzoka's architectural novelty lies.

---

# 223. Dzoka's Main Integration Advantage

Individually, the audited projects demonstrate parts of the problem.

Dzoka's value is the integration:

```text
PERCEPTION
    ↓
ITEM FINGERPRINT
    ↓
MATCHING
    ↓
CLAIM
    ↓
VERIFICATION
    ↓
TRUST
    ↓
RECOVERY AUTHORIZATION
    ↓
CUSTODY
    ↓
LOGISTICS
    ↓
RECOVERY
    ↓
LEARNING
    ↓
FEDERATION
```

---

# 224. Decision Rule for Future Audits

When a new repository is audited, ask:

### Can we adopt it?

```text
Is the license compatible?
Is the implementation mature?
Does it solve the problem cleanly?
Is the security model acceptable?
```

### Should we adapt it?

```text
Is the concept strong but implementation unsuitable?
```

### Should we only reference it?

```text
Is it research, restrictive-license source, old technology,
or architecture inspiration?
```

### Must Dzoka build instead?

```text
Does no suitable implementation exist?
```

---

# 225. Reusable Component Acceptance Checklist

Before adding something to this file as reusable:

```text
actual source inspected
exact commit pinned
license checked
runtime path confirmed
security reviewed
privacy reviewed
tests reviewed where relevant
limitations recorded
reuse classification assigned
Dzoka adaptation identified
```

---

# 226. Final Reuse Principle

Dzoka should avoid two extremes.

Do not:

```text
reinvent mature infrastructure unnecessarily
```

but also do not:

```text
copy an upstream application simply because it already has
"lost and found" in its name
```

The correct approach is:

```text
ADOPT mature foundations

ADAPT proven patterns

REFERENCE good research

BUILD Dzoka's unique trust/recovery layer
```

---

# 227. Final Architecture Synthesis

The strongest reusable architecture emerging from the audits is:

```text
                         CHANNELS
          Web / Mobile / WhatsApp / Kiosk / API
                              │
                              ▼
                     EXPERIENCE LAYER
                              │
                              ▼
                         REPORTING
                              │
                              ▼
                     MEDIA + PERCEPTION
             Detection / OCR / Embeddings
                              │
                              ▼
                     ITEM FINGERPRINT
                              │
                              ▼
                     HYBRID RETRIEVAL
              Text / Vector / Geo / IDs
                              │
                              ▼
                        RANK FUSION
                              │
                              ▼
                     CONFLICT ENGINE
                              │
                              ▼
                      FEATURE EVIDENCE
                              │
                              ▼
             COVERAGE + RERANK + CONFIDENCE
                              │
                              ▼
                      MATCH CANDIDATE
                              │
                              ▼
                            CLAIM
                              │
                              ▼
                 PRIVATE VERIFICATION
                              │
                              ▼
                    RISK + POLICY + HUMAN
                              │
                              ▼
                  RECOVERY AUTHORIZATION
                              │
                              ▼
                     CUSTODY / LOGISTICS
                              │
                              ▼
                   RECOVERY COMPLETION
                              │
                              ├──────────────► REWARD
                              │
                              ├──────────────► ML FEEDBACK
                              │
                              ├──────────────► TRACEABILITY
                              │
                              └──────────────► FEDERATION LEARNING
```

---

# 228. Final Conclusion

The source audits show that Dzoka does **not** need to invent every technical primitive.

Strong reusable foundations already exist for:

```text
vector search
OCR
object detection
visual embeddings
hybrid retrieval
traceability
federation patterns
notifications
claims
QR interaction
CCTV temporal search
```

What remains uniquely important is how Dzoka combines them.

The central reuse rule is:

> **Reuse mature infrastructure, adapt proven ideas, preserve evidence provenance, and build Dzoka's trust, verification, custody, and federation semantics explicitly rather than inheriting them accidentally from unrelated applications.**

The architecture should therefore optimize not for maximum code reuse, but for:

```text
correct reuse
+
safe reuse
+
licensed reuse
+
explainable reuse
+
replaceable reuse
```

That is the foundation on which Dzoka can become a real open recovery network rather than another lost-and-found listing application.