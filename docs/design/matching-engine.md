> **Current architecture authority (2026-10-26):** [Accepted baseline](../architecture/baseline.md) and its linked ADRs govern implementation. This supplied document is preserved below as detailed research/design material. Where examples or diagrams conflict with the baseline, use the baseline; no runtime capability is implied.

# Matching Engine Design

> **Dzoka — The Open Recovery Network**

This document defines the design of Dzoka's matching engine.

The matching engine answers:

> **Which lost and found observations are likely to refer to the same physical object?**

It does **not** answer:

> Who owns the item?

Those are separate concerns.

The matching engine consumes `ItemFingerprint` representations and produces:

```text
MatchCandidate
MatchEvidence
MatchConflict
MatchScore
EvidenceCoverage
MatchConfidence
RecommendedAction
```

The engine is designed for:

- multimodal evidence;
- incomplete reports;
- millions of records;
- same-instance identification;
- structured attributes;
- semantic retrieval;
- image retrieval;
- local visual matching;
- OCR;
- geospatial context;
- uncertain time;
- protected identifiers;
- federation;
- explainability;
- model evolution;
- human review.

---

# 1. Core Matching Principle

Dzoka separates five questions:

```text
Candidate Retrieval
    ↓
"Should we inspect this item?"

Match Scoring
    ↓
"How strongly do the observations agree?"

Evidence Coverage
    ↓
"How much useful evidence is actually available?"

Match Confidence
    ↓
"How trustworthy is the conclusion?"

Ownership Verification
    ↓
"Does this claimant have a legitimate right to recover it?"
```

These MUST remain separate.

---

# 2. Matching Is Not Ownership

A result such as:

```text
MatchScore = 0.96
Confidence = HIGH
```

means:

> the two records are likely to describe the same physical object.

It does not mean:

> the person making the claim owns that object.

Ownership is handled by the Ownership Verification subsystem.

---

# 3. Matching Engine Goals

The engine must:

1. retrieve likely candidates efficiently;
2. support partial evidence;
3. combine heterogeneous evidence safely;
4. distinguish missing evidence from mismatch;
5. detect hard contradictions;
6. compare mass-produced objects at instance level where possible;
7. preserve evidence provenance;
8. provide explanations;
9. support model/version changes;
10. support local and federated matching;
11. avoid O(N²) matching;
12. remain benchmarkable;
13. support human decisions and ML feedback.

---

# 4. Non-Goals

The matching engine does not:

- approve ownership;
- transfer custody;
- release property;
- generate payments;
- expose secret evidence publicly;
- replace domain policy;
- treat one embedding as definitive identity;
- rely on LLM judgment as final truth.

---

# 5. Input Contract

Primary input:

```text
MatchingRequest
├── sourceFingerprintId
├── sourceItemId
├── direction
├── tenantId
├── allowedScopes
├── matchingProfile
├── federationPolicy
├── requestedTopK
├── trigger
└── correlationId
```

Possible directions:

```text
LOST_TO_FOUND
FOUND_TO_LOST
REGISTERED_TO_FOUND
CCTV_TO_ITEM
FEDERATED
```

---

# 6. Output Contract

The engine returns:

```text
MatchingResult
├── runId
├── sourceFingerprintId
├── algorithmVersion
├── configurationVersion
├── candidates[]
├── retrievalMetrics
├── duration
└── warnings[]
```

Each candidate contains:

```text
MatchCandidate
├── candidateId
├── targetItemId
├── targetFingerprintId
├── retrievalEvidence[]
├── featureScores[]
├── conflicts[]
├── retrievalScore
├── matchScore
├── evidenceCoverage
├── matchConfidence
├── confidenceLevel
├── recommendedAction
└── explanation
```

---

# 7. Matching Pipeline

The complete pipeline is:

```text
Source ItemFingerprint
        ↓
Search Eligibility
        ↓
Candidate Retrieval
        ↓
Candidate Union
        ↓
Candidate Deduplication
        ↓
Rank Fusion
        ↓
Cheap Hard-Conflict Checks
        ↓
Feature Extraction
        ↓
Feature Scoring
        ↓
Evidence Reliability
        ↓
Evidence Coverage
        ↓
Heavy Reranking
        ↓
Advanced Conflict Analysis
        ↓
Score Aggregation
        ↓
Confidence Calibration
        ↓
Explanation
        ↓
Policy / Thresholding
        ↓
Persist Candidates
```

---

# 8. Two-Stage Search

Dzoka uses at least two matching stages.

## Stage 1 — Candidate Retrieval

Goal:

> efficiently reduce millions of records to a manageable shortlist.

Uses cheaper indexed methods.

Example:

```text
3,000,000 records
    ↓
300 candidates
```

---

## Stage 2 — Detailed Matching

Goal:

> determine which shortlisted candidates plausibly represent the same physical object.

Uses:

- structured comparisons;
- local visual features;
- multimodal reranking;
- conflict analysis;
- evidence coverage;
- confidence calibration.

Example:

```text
300 candidates
    ↓
50 scored
    ↓
10 heavily reranked
    ↓
top 3 actionable
```

---

# 9. Candidate Retrieval Service

Primary component:

```text
CandidateRetrievalService
```

It orchestrates multiple independent retrievers.

```text
CandidateRetrievalService
├── ExactIdentifierRetriever
├── CategoryRetriever
├── AttributeRetriever
├── LexicalRetriever
├── TrigramRetriever
├── SemanticRetriever
├── GlobalImageRetriever
├── TextToImageRetriever
├── GeoRetriever
├── TemporalRetriever
├── DistinctiveFeatureRetriever
└── FederatedRetriever
```

---

# 10. Retrieval Strategy Contract

Conceptually:

```text
RetrievalStrategy.search(
    sourceFingerprint,
    searchContext,
    retrievalBudget
) -> RetrievalResult
```

`RetrievalResult` contains:

```text
strategy
candidates[]
latency
queryMetadata
modelVersion
warnings
```

---

# 11. Retrieval Candidate

```text
RetrievalCandidate
├── targetItemId
├── targetFingerprintId
├── retrievalRoute
├── rawScore
├── rank
├── sourceEvidence
└── metadata
```

`rawScore` is specific to the retrieval method.

It MUST NOT automatically be compared against scores from different retrieval routes.

---

# 12. Exact Identifier Retrieval

Strong identifiers should be checked early.

Examples:

```text
IMEI
serial number
passport number
enterprise asset ID
registered Dzoka tag
GS1 identifier
```

Normal lookup uses protected hashes.

Example:

```text
source IMEI HMAC
    ↓
indexed lookup
    ↓
exact candidate
```

Exact identifier matches may dramatically increase candidate priority.

---

# 13. Identifier Retrieval Security

The retriever should never require loading plaintext identifiers for normal equality search.

Use:

```text
normalized identifier
    ↓
keyed hash
    ↓
indexed equality lookup
```

---

# 14. Category Retrieval

Retrieve candidates within:

```text
same category
compatible category
related category
```

Example:

```text
LAPTOP_BAG
```

may retrieve:

```text
LAPTOP_BAG
BACKPACK
BAG_UNKNOWN
```

depending on category relationship policy.

---

# 15. Category Compatibility

Use a relationship matrix:

```text
EXACT
COMPATIBLE
POSSIBLE
INCOMPATIBLE
```

Example:

```text
BACKPACK ↔ LAPTOP_BAG
COMPATIBLE

PASSPORT ↔ UMBRELLA
INCOMPATIBLE
```

---

# 16. Attribute Retrieval

Candidate filters may use:

```text
brand
model
colour
material
size
category-specific attributes
```

Do not overfilter on unreliable evidence.

Example:

A low-confidence colour inference should not eliminate an otherwise strong candidate.

---

# 17. Lexical Retrieval

Use PostgreSQL full-text search for:

```text
raw descriptions
normalized descriptions
canonical descriptions
OCR text
safe labels
```

Lexical retrieval is useful for:

```text
brand names
model names
rare textual terms
custom labels
```

---

# 18. Trigram Retrieval

`pg_trgm` supports:

```text
misspellings
partial brand/model names
OCR errors
```

Example:

```text
Lenvo
```

may still retrieve:

```text
Lenovo
```

---

# 19. Semantic Retrieval

Use text embeddings for semantic similarity.

Potential inputs:

```text
normalized description
canonical description
translated description
```

Use multilingual models where practical.

---

# 20. Semantic Retrieval Is Weak Evidence

Semantic retrieval is primarily a **candidate-generation mechanism**.

Example:

```text
"black work laptop bag"
```

being semantically close to:

```text
"dark office backpack"
```

does not prove they are the same physical item.

---

# 21. Global Image Retrieval

Use global image embeddings to retrieve visually similar items.

Default vector provider:

```text
PgVectorProvider
```

Optional scale-out:

```text
QdrantProvider
```

Query must specify:

```text
embedding type
model
version
dimensions
```

---

# 22. Text-to-Image Retrieval

If the source report has text but no image:

```text
"black Lenovo backpack"
```

a compatible multimodal embedding model may retrieve visually relevant candidate images.

This improves text-only lost reports.

---

# 23. Image-to-Text Retrieval

Where the embedding model supports it, a found-item image may retrieve semantically compatible text descriptions.

This should remain secondary to structured and image evidence.

---

# 24. Geospatial Retrieval

Location search may use:

```text
distance
facility hierarchy
route relationship
shared institution
transport network
environment
```

It must not rely only on Haversine distance.

---

# 25. Location Search Example

A bag lost on a bus may later appear at:

```text
bus depot
transport lost-property office
```

which may be geographically far from the original point.

Pure distance filtering could incorrectly remove the correct candidate.

---

# 26. Environment-Aware Search

Location tolerances may differ by environment.

Examples:

```text
OFFICE
small radius

MALL
medium radius

AIRPORT
larger operational radius

BUS ROUTE
route-aware rather than pure radius
```

---

# 27. Temporal Retrieval

Use time windows, not only timestamps.

Example:

```text
lost:
13:30–14:30

found:
14:20
```

is a strong overlap.

---

# 28. Temporal Uncertainty

Natural language may produce wide ranges.

Example:

```text
"last night"
```

may become:

```text
18:00–02:00
```

according to locale/configuration.

Uncertainty should reduce precision but not become mismatch.

---

# 29. Distinctive Feature Retrieval

Where indexed structured distinctive features exist:

```text
yellow sticker
missing rubber foot
engraving
specific tear
```

they may contribute to candidate retrieval.

This should be bounded because free-form distinctive descriptions can be noisy.

---

# 30. Federated Retrieval

Federated candidate retrieval is an optional route.

It must occur only when:

```text
local policy permits
tenant policy permits
peer trust permits
data disclosure policy permits
```

Federated candidates enter the local candidate pipeline as evidence-bearing remote references.

---

# 31. Candidate Retrieval Budget

Each retriever should receive a bounded budget.

Example:

```text
Exact identifier     20
Lexical              100
Semantic             100
Image                100
Geo                   75
Temporal              75
Federation            50
```

These values are configurable and benchmark-driven.

---

# 32. Candidate Union

All retrieval results are combined.

Example:

```text
Candidate A
retrieved by:
- semantic
- image
- geo

Candidate B
retrieved by:
- lexical
- time

Candidate C
retrieved by:
- exact identifier
```

---

# 33. Candidate Deduplication

Deduplicate on authoritative item reference.

For federated candidates use:

```text
sourceNode + remoteReference
```

Do not merge two different remote records simply because their descriptions are similar.

---

# 34. Reciprocal Rank Fusion

A strong default for combining heterogeneous retrieval routes is Reciprocal Rank Fusion.

Concept:

```text
RRF(d) = Σ 1 / (k + rank_i(d))
```

Advantages:

- does not require raw-score comparability;
- rewards candidates appearing in multiple retrieval routes;
- robust across heterogeneous retrievers.

---

# 35. RRF Configuration

Store:

```text
k parameter
enabled retrievers
per-route weighting if used
configuration version
```

Do not hard-code these in application source.

---

# 36. Weighted RRF

Where benchmarks support it:

```text
WeightedRRF(d) =
Σ weight_i / (k + rank_i(d))
```

This allows stronger retrieval methods to contribute more.

---

# 37. Retrieval Score

`retrievalScore` answers:

> how strongly should this candidate proceed to detailed comparison?

It is not the final `matchScore`.

---

# 38. Early Candidate Elimination

Before expensive scoring, eliminate candidates that are clearly impossible.

Examples:

```text
different verified IMEI
incompatible document type
impossible time ordering
explicitly excluded category
```

This saves compute.

---

# 39. Conflict Engine

Component:

```text
ConflictEngine
```

uses:

```text
ConflictRuleRegistry
```

Rules are versioned and configuration-driven.

---

# 40. Conflict Severity

```text
CRITICAL
MAJOR
MINOR
```

---

# 41. Critical Conflict Examples

```text
verified IMEI mismatch
verified serial mismatch
passport/document-number mismatch
registered tag mismatch
```

Possible behavior:

```text
candidate rejected
```

---

# 42. Major Conflict Examples

```text
brand mismatch
strong model mismatch
incompatible category
impossible chronology
```

Possible behavior:

```text
large confidence penalty
or rejection based on profile
```

---

# 43. Minor Conflict Examples

```text
colour disagreement
material disagreement
size disagreement
```

May be caused by:

```text
bad lighting
human error
AI extraction error
```

and usually should not independently reject a candidate.

---

# 44. Conflict Contract

```text
MatchConflict
├── conflictType
├── field
├── severity
├── sourceValue
├── candidateValue
├── reliability
├── penalty
├── rejecting
└── explanation
```

---

# 45. Feature Extraction

For remaining candidates create comparable evidence pairs.

Example:

```text
source brand ↔ candidate brand
source category ↔ candidate category
source location ↔ candidate location
source image features ↔ candidate image features
```

---

# 46. Feature Scoring Contract

```text
FeatureScore
├── featureType
├── available
├── score
├── reliability
├── configuredWeight
├── effectiveWeight
├── sourceValueSnapshot
├── candidateValueSnapshot
├── relation
├── provenance
└── explanation
```

---

# 47. Missing Evidence Rule

If either side lacks usable evidence:

```text
available = false
score = null
```

Never:

```text
score = 0
```

unless evidence exists and actually contradicts or fails similarity.

---

# 48. Category Scoring

Possible relationships:

```text
EXACT
PARENT_CHILD
SIBLING_COMPATIBLE
WEAKLY_RELATED
INCOMPATIBLE
```

Example conceptual scores:

```text
EXACT              1.00
PARENT_CHILD       0.85
COMPATIBLE         0.70
WEAKLY_RELATED     0.35
INCOMPATIBLE       conflict
```

Exact values belong in configuration.

---

# 49. Attribute Scoring

Attributes should be scored individually.

Example:

```text
brand
model
colour
material
size
strap count
screen size
```

Each attribute has:

```text
importance
source reliability
comparison function
```

---

# 50. Attribute Importance

Example:

For phone:

```text
IMEI       critical
model      high
brand      high
colour     low
```

For umbrella:

```text
colour     medium
pattern    high
handle     high
brand      low
```

Use category-specific profiles.

---

# 51. Attribute Score Aggregation

Concept:

```text
AttributeScore =
Σ(attributeSimilarity × importance × reliability)
───────────────────────────────────────────────
Σ(importance × reliability)
```

using only available attributes.

---

# 52. Semantic Score

Semantic similarity can use cosine similarity or model-specific similarity.

Convert raw similarity into normalized evidence through calibration.

Do not assume:

```text
cosine 0.8
```

means:

```text
80% probability of same item
```

---

# 53. Lexical Score

May combine:

```text
FTS rank
trigram similarity
keyword overlap
rare-token matches
```

Rare identifying terms may receive more value than generic words.

---

# 54. Global Image Score

Compare compatible image embeddings.

When multiple images exist:

```text
source image set
×
candidate image set
```

Do not simply average every pair.

Possible aggregation:

```text
best compatible pairs
top-N mean
view diversity adjustment
```

must be benchmarked.

---

# 55. View-Aware Image Comparison

Where image metadata exists:

```text
front
back
underside
interior
```

prefer semantically compatible views.

Example:

```text
underside ↔ underside
```

may be more useful than:

```text
underside ↔ front
```

---

# 56. Local Visual Score

Use patch/local features for same-instance discrimination.

Pipeline:

```text
source local features
      ↓
nearest compatible candidate local features
      ↓
top correspondences
      ↓
salience weighting
      ↓
consistency filtering
      ↓
LocalVisualScore
```

---

# 57. Local Feature Matching

Each local feature may contain:

```text
embedding
region
scale
salience
distinctive type
```

A strong local correspondence should be weighted more when:

```text
salience high
discriminative score high
feature confidence high
```

---

# 58. Spatial Consistency

Local feature matches may be improved by checking approximate spatial consistency.

Example:

A sticker believed to be on the underside should not match an unrelated visually similar region on the front without penalty.

---

# 59. Distinctive Feature Score

Structured distinctive features may compare:

```text
type
description
location on object
shape
colour
associated embedding
```

Example:

```text
yellow rectangular sticker on underside
```

is stronger than:

```text
yellow colour somewhere
```

---

# 60. OCR Score

OCR comparison may use:

```text
exact text
normalized text
fuzzy match
protected identifier hash
semantic text
```

Examples:

```text
ThinkPad
ABC Corp
custom name label
baggage tag
```

---

# 61. Sensitive OCR

Protected OCR evidence must be compared without exposing plaintext where possible.

Example serial comparison:

```text
HMAC(serial)
```

rather than raw serial display.

---

# 62. Geospatial Score

Inputs may include:

```text
distance
hierarchy
same facility
same route
same vehicle
movement path
environment
```

Conceptual score may combine:

```text
distance plausibility
+
hierarchy relationship
+
route plausibility
```

---

# 63. Distance Decay

Simple environments may use configurable distance decay.

Example:

```text
score = exp(-distance / τ)
```

where:

```text
τ
```

depends on environment.

Do not use one global τ.

---

# 64. Temporal Score

Possible factors:

```text
window overlap
time difference
report delay
environment-specific movement delay
```

Example:

An item found 20 minutes after loss is highly plausible.

An item found three months earlier than it was reportedly lost is not.

---

# 65. Temporal Ordering Conflicts

Some chronology is impossible.

Example:

```text
found_at < manufactured_at
```

or:

```text
found event conclusively occurred before lost event
```

depending on semantics.

These may become hard conflicts.

---

# 66. Provenance Score

Evidence provenance may affect trust.

Example:

```text
verified institutional record
```

may receive higher reliability than:

```text
anonymous unverified user description
```

This affects reliability, not necessarily semantic similarity.

---

# 67. Movement Plausibility Score

Recovery intelligence may provide:

```text
P(candidate_location |
  source_location,
  route,
  institution,
  time,
  item_category)
```

This becomes contextual evidence.

It must not override identity conflicts.

---

# 68. Evidence Reliability

Each feature score receives reliability based on:

```text
source type
model confidence
verification state
evidence quality
independence
```

---

# 69. Source Reliability Profile

Example conceptual hierarchy:

```text
VERIFIED_IDENTIFIER
    >
SYSTEM_REGISTERED_IDENTIFIER
    >
STAFF_CONFIRMED
    >
USER_ASSERTED
    >
HIGH_CONFIDENCE_OCR
    >
HIGH_CONFIDENCE_VISION
    >
LLM_INFERENCE
```

Exact values are configurable.

---

# 70. Independence of Evidence

Avoid double-counting strongly correlated derived evidence.

Example:

```text
canonical description
semantic embedding
LLM-generated keywords
```

may all originate from the same raw description.

They should not be treated as fully independent proof.

---

# 71. Evidence Dependency Graph

Where useful, store lineage:

```text
raw description
 ├── normalized description
 ├── canonical description
 │    └── text embedding
 └── keywords
```

The confidence model may account for correlation.

---

# 72. Effective Weight

Conceptually:

```text
effectiveWeight =
configuredWeight
× reliability
× independenceAdjustment
```

---

# 73. Match Score

For available evidence:

```text
MatchScore =
Σ(featureScore × effectiveWeight)
─────────────────────────────────
Σ(effectiveWeight)
```

---

# 74. Match Score Range

Normalize to either:

```text
0.0–1.0
```

internally, with optional percentage representation for UI.

Do not use arbitrary mixed score scales across components.

---

# 75. Evidence Coverage

Coverage measures how much identity-relevant evidence is available.

Concept:

```text
Coverage =
availablePotentialWeight
────────────────────────
totalPotentialWeight
```

---

# 76. Coverage Is Not Score

Example:

```text
Candidate A
score = .97
coverage = .18

Candidate B
score = .91
coverage = .84
```

Candidate B may be more trustworthy.

---

# 77. Identity-Grade Evidence

Coverage should distinguish ordinary evidence from high-value identity evidence.

Examples of identity-grade evidence:

```text
verified serial
IMEI
registered item ID
strong local visual mark
unique engraving
pre-loss photo correspondence
```

Without identity-grade evidence, confidence may be capped.

---

# 78. Confidence Caps

Example conceptual policy:

```text
coverage < .30
→ max LOW

coverage < .55
→ max MEDIUM

no identity-grade evidence
→ max MEDIUM

critical conflict
→ REJECTED
```

Actual thresholds must be versioned and benchmarked.

---

# 79. Evidence Thickness

A candidate with:

```text
brand + colour
```

should not receive the same confidence as one with:

```text
brand
model
local scratch
OCR
location
time
pre-loss photo
```

even if normalized scores are similar.

This concept is captured by:

```text
evidenceCoverage
identityEvidenceStrength
```

---

# 80. Heavy Reranker

The heavy reranker receives a bounded shortlist.

Possible inputs:

```text
global image embeddings
local patch features
structured attributes
OCR
text
location/time
```

---

# 81. Reranker Interface

```text
RerankerProvider.rerank(
    sourceFingerprint,
    candidateFingerprints,
    featureEvidence
) -> RerankResult[]
```

---

# 82. Reranker Types

Possible implementations:

```text
LocalPatchReranker
CrossEncoderTextReranker
MultimodalReranker
TransformerInstanceReranker
LLMEvidenceReranker
```

LLM reranking is optional and bounded.

---

# 83. Reranker Rules

A reranker:

- may adjust ranking;
- may produce additional evidence;
- may not remove critical conflicts;
- may not fabricate unavailable evidence;
- must record model/version provenance.

---

# 84. LLM Evidence Reranking

If used, supply structured evidence rather than unrestricted raw database data.

Example:

```text
Compare these candidate evidence records.
Do not infer missing facts.
Do not override listed hard conflicts.
```

The LLM result remains derived evidence.

---

# 85. Score Fusion with Reranker

Do not blindly average a reranker score with the deterministic score.

Possible designs:

```text
reranker as one feature
```

or:

```text
calibrated second-stage model
```

must be benchmarked.

---

# 86. Match Confidence

`MatchConfidence` estimates trustworthiness of the same-object conclusion.

It may use:

```text
match score
coverage
identity evidence
conflict profile
calibration model
reranker result
evidence diversity
```

---

# 87. Confidence Calibration

Raw similarity is not calibrated probability.

Use:

```text
held-out labeled data
```

to calibrate confidence.

Possible techniques:

```text
Platt scaling
isotonic regression
calibration model
```

where appropriate.

---

# 88. Confidence Level

Canonical levels:

```text
VERY_HIGH
HIGH
MEDIUM
LOW
INSUFFICIENT
REJECTED
```

---

# 89. Recommended Action

The engine may return:

```text
AUTO_NOTIFY
STAFF_REVIEW
REQUEST_MORE_EVIDENCE
FEDERATE_SEARCH
DO_NOT_SURFACE
REJECT
```

This is recommendation, not ownership authorization.

---

# 90. Candidate Thresholding

Example:

```text
VERY_HIGH
→ notify user and staff

HIGH
→ notify / review depending on tenant policy

MEDIUM
→ staff review or request additional evidence

LOW
→ keep internal

REJECTED
→ suppress
```

Tenant-specific policy may vary.

---

# 91. Candidate Persistence

Persist candidates that are:

```text
above durable threshold
human-reviewed
notified
federated
used in verification
```

Do not necessarily persist millions of transient retrieval hits.

---

# 92. Matching Run Persistence

Every matching execution creates:

```text
MatchingRun
```

with:

```text
runId
sourceFingerprint
algorithmVersion
configurationVersion
retrievalConfiguration
rerankerVersion
candidateCount
duration
status
```

---

# 93. Matching Configuration

Version configuration separately from code.

Example:

```yaml
version: matching-v3

retrieval:
  semanticTopK: 100
  imageTopK: 100
  lexicalTopK: 100
  rrfK: 60

scoring:
  profile: default-v2

confidence:
  profile: confidence-v2
```

---

# 94. Weight Profile

Example:

```yaml
default:
  category: 0.10
  attributes: 0.25
  location: 0.15
  time: 0.10
  distinctive: 0.15
  semantic: 0.10
  lexical: 0.05
  global_image: 0.05
  local_visual: 0.05
```

Category-specific profiles may override these.

---

# 95. Conflict Rules Configuration

Example:

```yaml
rules:
  imei:
    mismatchSeverity: CRITICAL
    reject: true

  serial:
    mismatchSeverity: CRITICAL
    reject: true

  brand:
    mismatchSeverity: MAJOR

  colour:
    mismatchSeverity: MINOR
```

---

# 96. Environment Configuration

Example:

```yaml
location:
  office:
    distanceTauMeters: 200

  mall:
    distanceTauMeters: 500

  airport:
    distanceTauMeters: 5000

  station:
    distanceTauMeters: 800
```

---

# 97. Match Evidence Record

Persist evidence as first-class data.

```text
MatchEvidence
├── candidateId
├── featureType
├── sourceValueSnapshot
├── candidateValueSnapshot
├── score
├── reliability
├── configuredWeight
├── effectiveWeight
├── contribution
├── available
├── sourceEvidenceIds[]
├── modelRunIds[]
└── explanation
```

---

# 98. Evidence Snapshot

Historical evidence should preserve what was compared at the time.

If later data changes, the old decision remains reproducible.

Sensitive values must be:

```text
masked
hashed
encrypted
```

as appropriate.

---

# 99. Explainability Service

Implement:

```text
MatchExplanationService
```

using structured evidence.

It should produce:

```text
strongest supporting evidence
weakest evidence
conflicts
missing evidence
coverage
reason for confidence
```

---

# 100. Human Explanation Example

```text
Likely match because:

• Same Lenovo ThinkPad T14 model.
• Found 23 minutes after the reported loss.
• Found at the destination depot for the reported bus route.
• Old owner photograph and found-item photograph share a distinctive scratch near the USB-C port.
• OCR detects matching custom asset text.
• No conflicting protected identifiers were found.

Evidence coverage: 86%
Match confidence: HIGH
```

---

# 101. Explanation Rules

Explanations must never reveal:

```text
full secret serial
private verification answers
hidden ownership-challenge details
restricted finder data
```

---

# 102. Public vs Staff Explanation

Maintain projections:

```text
PublicMatchExplanation
StaffMatchExplanation
VerificationMatchExplanation
```

Different audiences receive different evidence detail.

---

# 103. Federated Matching

Federation introduces non-comparable local scores.

Remote response may contain:

```text
localScore = .94
```

but Dzoka MUST NOT interpret `.94` as globally calibrated.

---

# 104. Federated Evidence Normalization

Convert remote evidence into canonical forms:

```text
category relation
attribute evidence
OCR evidence
vector similarity if compatible
geo/time context
coverage
model metadata
```

Then locally rerank.

---

# 105. Compatible Federated Embeddings

If nodes share compatible embedding models:

```text
same model
same version
same dimensions
same semantic space
```

vector comparisons may occur directly.

Otherwise use:

```text
remote retrieval only
+
safe structured evidence
```

and locally calibrated reasoning.

---

# 106. Federated Candidate Pipeline

```text
remote candidates
    ↓
source attribution
    ↓
schema validation
    ↓
safe-evidence normalization
    ↓
compatibility checks
    ↓
local feature scoring
    ↓
local confidence
```

---

# 107. Federation Source Attribution

Every remote result must preserve:

```text
sourceNode
remoteReference
remoteAlgorithmVersion
remoteModelVersions
receivedAt
```

---

# 108. Federated Candidate Deduplication

Two nodes may report the same physical item after custody transfer.

Do not automatically merge.

Possible signals:

```text
shared traceability reference
custody transfer reference
same protected identifier
federated recovery event
```

may permit linking.

---

# 109. CCTV Matching

CCTV evidence may enter matching as:

```text
CCTVObjectFingerprint
```

with:

```text
low-resolution embedding
track metadata
camera
time
location
```

---

# 110. CCTV Matching Constraints

CCTV imagery is often:

```text
blurred
occluded
low-resolution
poorly lit
```

Therefore its visual reliability may be lower than uploaded photographs.

Use context strongly.

---

# 111. Registered Item Matching

Registered possessions may provide:

```text
multiple pre-loss photos
serial identifiers
stable structured attributes
QR/NFC identity
```

These should improve retrieval and confidence.

---

# 112. Duplicate Report Detection

The matching engine may also support detection of duplicate reports.

Example:

```text
same user
same item
same day
same images
```

This is related but should be explicitly classified:

```text
DUPLICATE_REPORT
```

rather than normal lost-found match.

---

# 113. Candidate Lifecycle

```text
GENERATED
 ↓
RECOMMENDED
 ↓
UNDER_REVIEW
 ↓
CONFIRMED
```

Alternative states:

```text
REJECTED
STALE
SUPERSEDED
```

---

# 114. Candidate Staleness

A candidate may become stale when:

```text
source fingerprint superseded
candidate fingerprint superseded
item already recovered
new critical conflict discovered
```

Do not silently delete it.

Mark state.

---

# 115. Re-Matching Trigger

Potential triggers:

```text
new fingerprint version
new found item
new lost report
new protected identifier
federation peer online
new custody/location event
model/config promotion
manual retry
```

---

# 116. Incremental Matching

Do not rerun full corpus search unnecessarily.

Example:

When one found item arrives:

```text
match new item against eligible lost corpus
```

rather than recalculating every existing item pair.

---

# 117. Search Eligibility

Candidates should satisfy base eligibility.

Example:

```text
not archived/disposed unless policy permits
compatible report direction
same tenant or federation-authorized scope
within allowed retention
not explicitly excluded
```

---

# 118. Tenant Isolation

Local matching MUST include tenant policy.

Cross-tenant matching occurs only through explicitly configured sharing/federation.

Never accidentally query all tenants because they share one pgvector index.

---

# 119. Vector Tenant Filter

Vector requests must include:

```text
tenantId
```

or explicitly authorized cross-tenant federation scope.

---

# 120. Privacy-Minimized Matching

Candidate retrieval should access only needed fields.

Do not load:

```text
identity documents
private claim answers
finder contact information
```

during routine matching.

---

# 121. Search Backend Abstraction

Define:

```text
VectorSearchProvider
```

Implementations:

```text
PgVectorProvider
QdrantProvider
```

Matching code should consume a stable contract.

---

# 122. Vector Search Request

Concept:

```text
VectorSearchRequest
├── tenantId
├── vector
├── embeddingType
├── model
├── modelVersion
├── topK
├── filters
└── scoreThreshold
```

---

# 123. pgvector Design

Default V1:

```text
PostgreSQL + pgvector
```

Advantages:

- joins with domain data;
- operational simplicity;
- transaction compatibility;
- filters;
- mature indexing.

---

# 124. Qdrant Design

Optional when:

```text
vector volume very large
multi-vector search needed
independent vector scaling needed
```

PostgreSQL remains authoritative.

---

# 125. Search Index Synchronization

For external vector backends:

```text
embedding.created
    ↓
VectorIndexWorker
    ↓
Qdrant
```

Track:

```text
PENDING
INDEXED
FAILED
```

in PostgreSQL.

---

# 126. Search Failure Handling

If Qdrant is unavailable and pgvector fallback is configured:

```text
Qdrant search fails
    ↓
PgVectorProvider
```

The fallback must be observable.

Do not silently change result behavior.

---

# 127. Matching API

Internal endpoints may include:

```text
POST /internal/v1/matching/run
GET  /internal/v1/matching/runs/{runId}
GET  /internal/v1/matches/{candidateId}
POST /internal/v1/matches/{candidateId}/review
POST /internal/v1/matching/compare
```

---

# 128. Matching Run Request

Example:

```json
{
  "sourceFingerprintId": "019...",
  "direction": "LOST_TO_FOUND",
  "matchingProfile": "default-v3",
  "topK": 20,
  "includeFederation": true
}
```

---

# 129. Matching Response

Example:

```json
{
  "runId": "019...",
  "status": "COMPLETED",
  "candidates": [
    {
      "candidateId": "019...",
      "matchScore": 0.93,
      "evidenceCoverage": 0.84,
      "confidenceLevel": "HIGH"
    }
  ]
}
```

Detailed evidence may require a separate authorized endpoint.

---

# 130. Matching Worker

Default production flow is asynchronous.

```text
fingerprint.ready
    ↓
MatchingWorker
    ↓
MatchingEngine
    ↓
MatchingRun persisted
    ↓
candidate.created
```

---

# 131. Interactive Search

Some user searches may require synchronous retrieval.

Use only cheaper stages synchronously.

Example:

```text
structured + lexical + vector retrieval
```

Heavy local-feature reranking may continue asynchronously.

---

# 132. Search Result Progress

UI may show:

```text
Searching local records...
Checking visual candidates...
Checking partner network...
```

without blocking the original report creation request.

---

# 133. Matching Run Status

```text
QUEUED
RUNNING
PARTIAL
COMPLETED
FAILED
CANCELLED
STALE
```

---

# 134. Partial Results

If federation times out but local matching succeeds:

```text
status = PARTIAL
```

Return local candidates and record the federation warning.

---

# 135. Match Cancellation

Long-running runs may be cancelled when:

```text
case closed
item recovered
new fingerprint supersedes source
manual cancellation
```

---

# 136. Idempotency

Repeated request for the same:

```text
source fingerprint
matching profile
algorithm version
search scope
```

may reuse an existing completed run where policy permits.

---

# 137. Matching Run Key

Possible deterministic key derived from:

```text
fingerprintId
configurationVersion
searchScopeHash
algorithmVersion
```

This can avoid accidental duplicate work.

---

# 138. Historical Matching

Old runs remain immutable.

A new configuration creates a new run rather than changing old scores.

---

# 139. Human Match Decisions

Authorized reviewers may record:

```text
CONFIRMED
REJECTED
DEFERRED
```

with reason.

These decisions become ML feedback.

---

# 140. Human Override

A reviewer may override a non-critical automated recommendation if policy permits.

Critical identifier conflicts require stronger override authorization and audit.

---

# 141. Override Record

Store:

```text
actor
reason
previous recommendation
new decision
policy
timestamp
```

---

# 142. ML Feedback

Confirmed/rejected match decisions become:

```text
FeedbackRecord
```

for:

```text
ranking evaluation
calibration
hard-negative mining
weight optimization
```

---

# 143. Hard-Negative Mining

Rejected candidates with high initial similarity are especially valuable.

Example:

```text
same model
same colour
similar image
different physical item
```

These improve same-instance discrimination.

---

# 144. Evaluation Datasets

Maintain versioned datasets for:

```text
text-only reports
image-only reports
multimodal reports
hard negatives
identifier conflicts
geo/time ambiguity
federated candidates
CCTV candidates
```

---

# 145. Retrieval Metrics

Track:

```text
Recall@10
Recall@50
Recall@100
candidate count
retrieval latency
per-route recall
```

The correct match cannot be reranked if retrieval never finds it.

---

# 146. Ranking Metrics

Track:

```text
MRR
Precision@K
NDCG
Top-1 accuracy
Top-3 accuracy
```

---

# 147. Safety Metrics

Track:

```text
false-match rate
critical-conflict escape rate
high-confidence false-match rate
```

These matter more than generic similarity accuracy.

---

# 148. Coverage Metrics

Track recovery/matching performance by:

```text
coverage bands
```

Example:

```text
0-.25
.25-.50
.50-.75
.75-1.0
```

This reveals how robust the engine is under sparse evidence.

---

# 149. Calibration Metrics

Use:

```text
reliability diagrams
expected calibration error
Brier score
```

where appropriate.

Confidence should mean something measurable.

---

# 150. Business Metrics

Ultimately track:

```text
candidate → claim conversion
candidate → verified match conversion
verified recovery rate
median time to recovery
false-match operational incidents
```

---

# 151. Benchmark Profiles

At minimum:

```text
SMALL
10k records

MEDIUM
1m records

LARGE
10m+ records
```

Use synthetic and realistic datasets.

---

# 152. Performance Targets

The architecture should aim for:

```text
cheap candidate retrieval
interactive

heavy reranking
bounded and asynchronous where necessary
```

Exact latency SLOs belong in performance/deployment documentation.

---

# 153. Scaling Strategy

Scale independently:

```text
retrieval workers
reranking workers
vector backend
GPU workers
federation workers
```

---

# 154. Batch Matching

For newly imported institutional records:

```text
batch
 ↓
fingerprint generation
 ↓
bounded matching jobs
```

Use backpressure.

Do not flood Kafka/GPU workers without limits.

---

# 155. Matching Priority

Possible priorities:

```text
ACTIVE_OWNER_SEARCH
INSTITUTIONAL_NEW_FOUND_ITEM
BACKGROUND_REINDEX
MODEL_REEVALUATION
```

Active recovery work should outrank offline benchmarking.

---

# 156. Observability

Trace stages:

```text
matching.run
matching.retrieve.category
matching.retrieve.lexical
matching.retrieve.semantic
matching.retrieve.image
matching.rank-fusion
matching.conflicts
matching.features
matching.rerank
matching.confidence
```

---

# 157. Metrics

Examples:

```text
dzoka_matching_runs_total
dzoka_matching_run_duration_seconds
dzoka_matching_candidates_retrieved
dzoka_matching_candidates_reranked
dzoka_matching_conflicts_total
dzoka_matching_high_confidence_total
dzoka_matching_retrieval_route_latency_seconds
```

---

# 158. Logging

Structured logs may contain:

```text
runId
fingerprintId
candidate count
algorithm version
configuration version
duration
```

Do not log secret evidence values.

---

# 159. Failure Types

Canonical errors:

```text
SOURCE_FINGERPRINT_NOT_FOUND
SOURCE_FINGERPRINT_STALE
VECTOR_BACKEND_UNAVAILABLE
INCOMPATIBLE_EMBEDDING
MATCHING_CONFIGURATION_INVALID
RERANKER_UNAVAILABLE
FEDERATION_PARTIAL_FAILURE
MATCHING_TIMEOUT
```

---

# 160. Graceful Degradation

Examples:

```text
local patch model unavailable
→ continue without local visual evidence
→ coverage/confidence adjusted

federation unavailable
→ local results returned

Qdrant unavailable
→ optional pgvector fallback

LLM explanation unavailable
→ structured deterministic explanation
```

---

# 161. Required vs Optional Matching Features

Matching profile defines required capabilities.

Example:

```text
required:
structured retrieval
lexical retrieval
semantic retrieval

optional:
local visual reranking
federation
movement score
LLM explanation
```

Missing optional services must not break the run.

---

# 162. Security

Matching service must enforce:

```text
tenant scope
classification policy
service authentication
authorized federation scope
protected identifier handling
audit for sensitive searches
```

---

# 163. Sensitive Search Audit

Exact searches for:

```text
IMEI
serial
passport/document ID
```

may require audit because they can expose whether a protected identifier exists.

---

# 164. Rate Limiting

Protect public/manual matching endpoints from:

```text
enumeration
bulk scraping
candidate probing
```

Background trusted workers have separate quotas.

---

# 165. Search Privacy Attack

Attackers may attempt:

```text
submit many fake lost reports
observe candidate outputs
infer hidden attributes
```

Mitigations include:

```text
limited public candidate detail
rate limiting
claim requirements
private challenge separation
risk detection
```

---

# 166. Public Candidate Projection

Public user may receive:

```text
general category
safe image
broad found area
approximate date
high-level reason
```

Not:

```text
secret attributes
serial
finder details
full internal scores
conflict rules
```

---

# 167. Staff Candidate Projection

Authorized staff may see:

```text
detailed match evidence
confidence
coverage
conflicts
algorithm provenance
```

subject to policy.

---

# 168. Model Version Compatibility

Matching should reject direct vector comparison when:

```text
model/version spaces incompatible
```

Possible fallback:

```text
recompute embedding
use other features
use lexical/structured retrieval
```

---

# 169. Model Migration

During migration:

```text
Model A vectors
+
Model B vectors
```

may coexist.

Matching configuration decides which model/version is active.

Do not mix vectors silently.

---

# 170. Shadow Matching

A candidate model/configuration may run in shadow.

```text
production matcher
→ user-facing result

shadow matcher
→ evaluation only
```

Compare outcomes without affecting recovery.

---

# 171. A/B Experimentation

If experimentation is used, it must not compromise safety.

Critical conflict rules remain consistent.

Experiment assignment and configuration must be recorded.

---

# 172. Source Audit Influence

Important audited influences include:

```text
globalenglish01/lostfound
    → retrieval vs scoring separation
    → missing != mismatch
    → hard conflicts
    → configurable weights
    → evidence coverage
    → model-aware embeddings

ILIAS
    → instance-level retrieval evaluation
    → query expansion concepts

Patchwise Retrieval
    → local patch representations

RerankingTransformer
    → global/local reranking concepts

OpenCLIP
    → global multimodal representation

pgvector
    → default ANN retrieval

Qdrant
    → scale-out vector provider

Mycel
    → remote score non-comparability
    → federation source attribution
```

Dzoka's final matching architecture is its own composition and domain design.

---

# 173. Anti-Patterns

Do not implement:

```text
score =
0.5 * textSimilarity +
0.5 * imageSimilarity
```

as the complete matcher.

Do not implement:

```text
if no image:
    imageScore = 0
```

Do not implement:

```text
sort all federation candidates by remote score
```

Do not implement:

```text
LLM says same item → confirmed
```

Do not implement:

```text
compare every lost item against every found item
```

Do not implement:

```text
high CLIP similarity = ownership verified
```

---

# 174. Example Matching Case

Lost item:

```text
black Lenovo ThinkPad T14
lost around 14:00
Joina City
old owner photo
scratch near USB-C
```

Found item:

```text
black Lenovo ThinkPad T14
found 14:23
Joina City security office
front/underside photos
```

Evidence:

```text
category          1.00
brand             1.00
model             1.00
colour             .98
time               .96
location           .94
semantic           .89
global image       .87
local scratch      .96
OCR                .92

conflicts:
none
```

Coverage:

```text
.86
```

Possible result:

```text
MatchScore = .93
Confidence = HIGH
RecommendedAction = AUTO_NOTIFY
```

This is still not ownership verification.

---

# 175. Example Sparse Match

Lost:

```text
blue umbrella
Avondale
yesterday
```

Found:

```text
blue umbrella
Avondale
same day
```

Evidence:

```text
category = strong
colour = strong
location = strong
time = moderate
```

But:

```text
no image
no distinctive features
no identifiers
```

Possible:

```text
MatchScore = .91
Coverage = .32
Confidence = MEDIUM
```

The score is high, but evidence is weak.

---

# 176. Example Critical Conflict

Lost:

```text
Samsung Galaxy S25
IMEI hash = A
```

Found:

```text
Samsung Galaxy S25
IMEI hash = B
```

Even if:

```text
image similarity = .97
location = same
time = same
```

result:

```text
CRITICAL IMEI CONFLICT
Confidence = REJECTED
```

---

# 177. Example Federation Match

Local node:

```text
black backpack
airport terminal
08:00
```

Remote transport node returns:

```text
localScore = .95
category = backpack
safe image vector
found at bus depot
09:15
```

Dzoka does not trust `.95`.

Instead:

```text
validate model compatibility
compare vector locally if possible
score route/time plausibility
evaluate structured evidence
derive local match score
derive local confidence
```

---

# 178. Example Retrieval Fusion

Candidate A ranks:

```text
semantic #3
image #1
geo #6
time #4
```

Candidate B ranks:

```text
semantic #1
```

RRF may correctly prioritize Candidate A because independent retrieval routes support it.

---

# 179. Matching Engine Pseudocode

Conceptually:

```text
function match(request):

    source = loadFingerprint(request.sourceFingerprintId)

    ensureEligible(source)

    profile = loadMatchingProfile(request.matchingProfile)

    retrievalResults =
        runRetrieversInParallel(source, profile)

    candidates =
        unionAndDeduplicate(retrievalResults)

    candidates =
        rankFuse(candidates)

    candidates =
        applyCheapConflictChecks(candidates)

    candidates =
        truncateToDetailedScoringBudget(candidates)

    for candidate in candidates:

        target = loadFingerprint(candidate.targetFingerprintId)

        features =
            scoreFeatures(source, target, profile)

        conflicts =
            evaluateConflicts(source, target, profile)

        if conflicts.containsRejectingCritical():
            persistRejectedCandidate()
            continue

        coverage =
            calculateEvidenceCoverage(features)

        baseScore =
            aggregateFeatureScores(features)

        if candidateEligibleForHeavyRerank():
            rerankEvidence =
                heavyRerank(source, target, features)

        score =
            combineConfiguredEvidence(
                baseScore,
                rerankEvidence
            )

        confidence =
            calibrateConfidence(
                score,
                coverage,
                conflicts,
                identityEvidence
            )

        explanation =
            explainStructuredEvidence(
                features,
                conflicts,
                coverage,
                confidence
            )

        persistCandidate(...)

    return rankedResults()
```

---

# 180. Matching Decision Pipeline

Final candidate ordering should consider:

```text
confidence level
then calibrated confidence
then match score
then evidence coverage
```

rather than only raw score.

Exact ordering should be benchmarked.

---

# 181. Business Rules

### MATCH-001

Matching does not establish ownership.

### MATCH-002

Unavailable evidence is excluded from scoring.

### MATCH-003

Critical identifier conflicts may reject candidates.

### MATCH-004

Every candidate must reference exact fingerprint versions.

### MATCH-005

Every model-based feature must preserve model provenance.

### MATCH-006

Remote node scores are not globally comparable.

### MATCH-007

Heavy reranking operates on bounded candidate sets.

### MATCH-008

Historical matching runs are immutable.

### MATCH-009

Public explanations may not expose secret evidence.

### MATCH-010

A high score with low evidence coverage must not become high confidence without calibration/policy support.

---

# 182. Implementation Sequence

Recommended order:

```text
1. MatchingRun domain contract
2. retrieval strategy interface
3. structured/category retrieval
4. lexical/trigram retrieval
5. pgvector semantic retrieval
6. global image retrieval
7. rank fusion
8. conflict engine
9. feature scoring framework
10. evidence coverage
11. confidence rules
12. explainability
13. local visual scoring
14. heavy reranking
15. federation normalization
16. movement plausibility
17. calibration
18. shadow experimentation
19. large-scale benchmark tooling
```

This is sequencing, not scope reduction.

---

# 183. Test Requirements

Unit tests:

```text
RRF
missing evidence
weight normalization
conflict rules
coverage
confidence caps
category scoring
temporal overlap
geo scoring
```

Integration tests:

```text
pgvector retrieval
FTS retrieval
trigram retrieval
PostGIS filtering
Qdrant provider contract
federation normalization
```

---

# 184. Golden Matching Tests

Mandatory cases:

```text
same item / different angle
same item / different lighting
same model / different instance
same colour only
text-only lost report
image-only found report
OCR-rich item
serial exact match
serial critical mismatch
far location but plausible route
same location but impossible time
federated remote candidate
```

---

# 185. Load Tests

Measure:

```text
retrieval latency
candidate count
vector query latency
reranker throughput
worker lag
PostgreSQL load
Qdrant optional load
```

under realistic corpus sizes.

---

# 186. Production Readiness

The matching engine is production-ready only when it has:

```text
versioned config
reproducible MatchingRun
candidate evidence
hard conflict rules
coverage
confidence
explainability
benchmarks
tenant isolation
provider failure handling
observability
security tests
```

A working cosine-similarity endpoint is not sufficient.

---

# 187. Relationship to ItemFingerprint

`ItemFingerprint` answers:

> What evidence describes this physical object?

Matching answers:

> How strongly does this evidence correspond to another observed object?

The matching engine must not reimplement fingerprint construction.

---

# 188. Relationship to Ownership Verification

Matching outputs:

```text
Likely same object
```

Ownership Verification consumes a candidate and asks:

```text
Does this claimant know/provide private evidence consistent with legitimate ownership or entitlement?
```

They must remain separate services and state machines.

---

# 189. Relationship to Recovery

Only after:

```text
match
+
claim
+
verification
+
risk
+
policy
```

may recovery authorization occur.

Matching itself cannot release property.

---

# 190. Final Design Principle

The Dzoka matching engine is not a similarity calculator.

It is an **evidence-fusion system for same-physical-object hypothesis testing under uncertainty**.

It must answer:

```text
How did we find this candidate?

Which independent evidence supports it?

Which evidence contradicts it?

Which evidence is missing?

How reliable is each source?

How much evidence do we actually have?

Which models produced the derived evidence?

How confident should we be?

Can a human understand why it was recommended?
```

The final reasoning chain is:

```text
MULTIPLE RETRIEVAL ROUTES
          ↓
CANDIDATE FUSION
          ↓
HARD CONFLICTS
          ↓
MULTIMODAL FEATURE EVIDENCE
          ↓
RELIABILITY
          ↓
COVERAGE
          ↓
BOUNDED HEAVY RERANKING
          ↓
CALIBRATED CONFIDENCE
          ↓
EXPLAINABLE MATCH CANDIDATE
```

That design allows Dzoka to recover real physical objects reliably without reducing identity to one opaque AI similarity score.