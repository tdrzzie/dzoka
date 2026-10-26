> **Current architecture authority (2026-10-26):** [Accepted baseline](../architecture/baseline.md) and its linked ADRs govern implementation. This supplied document is preserved below as detailed research/design material. Where examples or diagrams conflict with the baseline, use the baseline; no runtime capability is implied.

# Item Fingerprint Design

> **Dzoka — The Open Recovery Network**

This document defines the design of the `ItemFingerprint`, the canonical machine-readable representation Dzoka uses to describe, search for, compare, and reason about a physical object.

The `ItemFingerprint` is central to Dzoka.

It provides the bridge between:

```text
human description
photos
video
voice
OCR
protected identifiers
location
time
institutional context
```

and:

```text
candidate retrieval
same-object matching
ownership verification
federated search
CCTV evidence
recovery intelligence
```

The fingerprint must be:

- multimodal;
- versioned;
- explainable;
- privacy-aware;
- model-aware;
- extensible;
- suitable for partial evidence;
- suitable for federation;
- suitable for local and remote AI providers;
- capable of representing same-instance identity rather than only category similarity.

---

# 1. Core Principle

An `ItemFingerprint` does not attempt to produce one magical identifier or one opaque vector.

A physical object's identity is represented by **multiple independent evidence dimensions**.

Conceptually:

```text
ItemFingerprint
    =
semantic identity
+
structured attributes
+
global visual identity
+
local visual identity
+
textual identity
+
OCR identity
+
protected identifiers
+
location/time context
+
provenance
```

The system must never assume:

```text
one embedding = item identity
```

---

# 2. Why a Fingerprint Is Needed

Consider these three objects:

```text
black Lenovo backpack
black Lenovo backpack
black Lenovo backpack
```

A category model may treat them as almost identical.

Even a global image embedding may place them extremely close together.

But one specific backpack may have:

```text
yellow luggage tag
small tear beside right zipper
missing zipper pull
white paint mark underneath
worn left shoulder strap
```

Those details identify the **instance**, not merely the category.

Dzoka therefore separates:

```text
category identity
```

from:

```text
instance identity
```

---

# 3. Design Goals

The ItemFingerprint design must support:

1. partially described items;
2. multiple photos;
3. old pre-loss photos;
4. found-item photos;
5. images from different angles;
6. poor-quality images;
7. text-only reports;
8. voice-only reports;
9. multilingual reports;
10. OCR;
11. exact protected identifiers;
12. local visual differences;
13. geospatial context;
14. temporal uncertainty;
15. model upgrades;
16. federation;
17. historical reproducibility;
18. privacy-safe projections.

---

# 4. Non-Goals

An ItemFingerprint is not:

- ownership proof;
- a public listing;
- one embedding;
- a blockchain identity;
- a replacement for the original report;
- a replacement for source media;
- guaranteed ground truth;
- a permanent immutable identity across all model versions.

The fingerprint represents the best machine-readable evidence available at a specific point in time.

---

# 5. Canonical Structure

Conceptually:

```text
ItemFingerprint
│
├── metadata
│
├── semanticIdentity
│
├── structuredAttributes
│
├── globalVisualIdentity
│
├── localVisualIdentity
│
├── textIdentity
│
├── ocrIdentity
│
├── protectedIdentity
│
├── contextualIdentity
│
├── provenance
│
└── quality
```

---

# 6. Canonical Data Contract

A language-neutral conceptual contract:

```text
ItemFingerprint
├── fingerprintId
├── itemId
├── tenantId
├── version
├── schemaVersion
├── status
├── semanticIdentity
├── structuredAttributes[]
├── globalVisualFeatures[]
├── localVisualFeatures[]
├── textFeatures[]
├── ocrFeatures[]
├── protectedFeatures[]
├── contextualFeatures
├── provenance[]
├── coverage
├── quality
├── contentHash
├── createdAt
└── supersedesFingerprintId
```

---

# 7. Fingerprint Identity

Every fingerprint must have:

```text
fingerprintId
itemId
version
schemaVersion
```

These are different concepts.

`itemId`

identifies the logical physical object record.

`fingerprintId`

identifies one generated representation of that object.

`version`

identifies the fingerprint generation sequence for that item.

`schemaVersion`

identifies the structure of the fingerprint contract itself.

---

# 8. Example Versioning

```text
Item: LAPTOP-123

Fingerprint v1
    text report only

Fingerprint v2
    + old owner photograph

Fingerprint v3
    + OCR and distinctive feature extraction

Fingerprint v4
    + new embedding model
```

Older fingerprints remain immutable.

---

# 9. Fingerprint Status

Possible statuses:

```text
BUILDING
ACTIVE
SUPERSEDED
FAILED
RETIRED
```

`ACTIVE` means:

> eligible for normal matching under current policy.

`SUPERSEDED` means:

> a newer fingerprint version exists, but the old fingerprint remains available for historical reproducibility.

---

# 10. Semantic Identity

`semanticIdentity` contains broadly interpretable identity fields.

Example:

```text
SemanticIdentity
├── category
├── subcategory
├── brand
├── model
├── variant
├── primaryColour
├── secondaryColours[]
├── material
├── sizeClass
└── objectTypeConfidence
```

Example:

```text
category = BAG
subcategory = BACKPACK
brand = Lenovo
primaryColour = BLACK
material = POLYESTER
```

---

# 11. Category Design

Categories should use canonical identifiers.

Example hierarchy:

```text
ELECTRONICS
├── PHONE
├── LAPTOP
├── TABLET
└── CAMERA

BAGS
├── BACKPACK
├── HANDBAG
├── SUITCASE
└── LAPTOP_BAG
```

A report may initially produce:

```text
category = BAG
confidence = 0.74
```

and later improve to:

```text
category = LAPTOP_BAG
confidence = 0.92
```

---

# 12. Category Uncertainty

Do not force low-confidence classification into fake certainty.

Support:

```text
CategoryHypothesis[]
```

Example:

```text
BACKPACK      0.65
LAPTOP_BAG    0.29
OTHER_BAG     0.06
```

Matching may use the distribution rather than only the top category.

---

# 13. Structured Attributes

Attributes capture search-relevant characteristics.

Canonical representation:

```text
FingerprintAttribute
├── attributeId
├── name
├── normalizedValue
├── rawValue
├── valueType
├── source
├── confidence
├── reliability
├── classification
├── searchable
└── provenance
```

---

# 14. Example Attributes

For a laptop:

```text
brand
model
colour
screen_size
keyboard_layout
charger_type
material
operating_system
```

For a bag:

```text
colour
material
strap_count
zipper_count
brand
approximate_size
pocket_count
```

For a passport:

```text
issuing_country
document_type
colour
```

Sensitive identity fields remain in protected identity rather than normal attributes.

---

# 15. Attribute Sources

Possible sources:

```text
USER
FINDER
STAFF
OCR
VISION
AI
SYSTEM
ASSET_REGISTRY
FEDERATION
CCTV
```

Source type influences reliability.

---

# 16. Attribute Reliability

Example:

```text
brand = Lenovo
source = OCR
confidence = 0.97
```

may be more reliable than:

```text
brand = Lenovo
source = vision inference
confidence = 0.63
```

Matching must consider both:

```text
confidence
```

and:

```text
source reliability
```

---

# 17. Global Visual Identity

Global visual features represent the overall visual appearance of the object.

```text
GlobalVisualFeature
├── mediaId
├── objectCropId
├── embeddingId
├── embeddingType
├── model
├── modelVersion
├── dimensions
├── confidence
├── contentHash
└── provenance
```

---

# 18. Multiple Global Embeddings

A fingerprint may contain multiple visual embeddings.

Example:

```text
front image
back image
side image
interior image
old owner image
finder image
```

Do not average them blindly into one vector unless a documented aggregation algorithm requires it.

---

# 19. Global Embedding Use

Global embeddings are useful for:

- approximate image similarity;
- broad candidate retrieval;
- text-to-image retrieval;
- category/appearance similarity.

They are weaker for:

- near-identical mass-produced items;
- subtle damage;
- stickers;
- scratches;
- tiny visual differences.

That limitation motivates local visual identity.

---

# 20. Local Visual Identity

`localVisualIdentity` represents distinctive regions or patches.

Concept:

```text
LocalVisualFeature
├── featureId
├── mediaId
├── region
├── scale
├── featureType
├── embeddingId
├── salience
├── confidence
├── description
└── provenance
```

---

# 21. Patch Extraction

A local-feature pipeline may perform:

```text
object crop
   ↓
multi-scale patch generation
   ↓
patch embedding
   ↓
salience filtering
   ↓
distinctive region selection
```

Possible scales:

```text
full object
1/2 regions
1/4 regions
local object parts
```

---

# 22. Why Local Features Matter

Example:

Two laptops may both be:

```text
Lenovo ThinkPad
black
14 inch
```

but one contains:

```text
scratch near USB-C
yellow underside sticker
missing rubber foot
```

Local visual features can represent these details.

---

# 23. Distinctive Features

Canonical structure:

```text
DistinctiveFeature
├── type
├── description
├── region
├── visibility
├── confidence
├── discriminativeScore
├── classification
├── source
└── provenance
```

Types may include:

```text
SCRATCH
CRACK
STICKER
TEAR
DENT
DISCOLOURATION
MISSING_COMPONENT
CUSTOM_MARK
ENGRAVING
WEAR_PATTERN
PATCH
OTHER
```

---

# 24. Discriminative Score

Not every feature is equally useful.

Example:

```text
black colour
```

has low discriminative value.

```text
yellow sticker underneath laptop
```

has much higher value.

Therefore features may contain:

```text
discriminativeScore
```

which can influence:

- matching;
- ownership challenge selection;
- evidence confidence.

---

# 25. Visibility Classification

Distinctive features may be classified:

```text
PUBLIC_SAFE
RESTRICTED
SECRET
```

Example:

```text
black outer shell
→ PUBLIC_SAFE

small underside scratch
→ SECRET
```

Secret features are excellent ownership-verification candidates.

---

# 26. Text Identity

Textual features represent descriptions and semantic meaning.

```text
TextIdentity
├── rawDescriptions[]
├── normalizedDescriptions[]
├── canonicalDescriptions[]
├── semanticEmbeddings[]
├── keywords[]
├── languages[]
└── provenance
```

---

# 27. Raw Description

Always retain:

```text
"Ndasiya black Lenovo bag muJoina around 2pm"
```

exactly as received.

Do not replace it with:

```text
"Lost black Lenovo backpack at Joina City at 14:00"
```

The second representation is derived.

---

# 28. Normalized Description

A normalized form may be:

```text
lost black Lenovo bag at Joina City approximately 14:00
```

This improves retrieval.

It must retain provenance to the raw source.

---

# 29. Canonical Description

A canonical machine-friendly description may combine structured attributes.

Example:

```text
black Lenovo backpack,
medium,
polyester,
lost at Joina City,
approximately 14:00
```

The canonical description may be generated specifically for embedding retrieval.

---

# 30. Language Design

Every text representation should carry:

```text
sourceLanguage
targetLanguage where translated
translationProvider
translationVersion
```

Multilingual reports should not require translating all evidence into English before storage.

---

# 31. Text Embeddings

Text embeddings may be generated from:

```text
raw description
normalized description
canonical description
OCR text
distinctive feature description
```

Embedding type must identify which representation was used.

---

# 32. OCR Identity

OCR evidence is treated separately because visible text may strongly identify an item.

```text
OCRFeature
├── text
├── normalizedText
├── boundingBox
├── confidence
├── language
├── classification
├── mediaId
├── modelRunId
└── provenance
```

---

# 33. OCR Examples

Potential useful OCR:

```text
Lenovo
ThinkPad
Property of ABC Corp
serial suffix
custom name label
flight/baggage tag
registration number
```

OCR text may be more discriminative than general visual similarity.

---

# 34. Sensitive OCR

OCR may accidentally discover:

```text
passport number
phone number
email
employee ID
serial
IMEI
```

Therefore OCR output must pass through sensitive-entity classification before entering public-safe representations.

---

# 35. Protected Identity

Protected identity contains strong or sensitive identifiers.

```text
ProtectedIdentity
├── serialIdentifiers[]
├── imeiIdentifiers[]
├── documentIdentifiers[]
├── enterpriseAssetIds[]
├── secretAttributes[]
└── ownershipEvidenceReferences[]
```

These features are not public.

---

# 36. Protected Identifier Structure

```text
ProtectedIdentifier
├── type
├── normalizedHash
├── encryptedValue
├── maskedValue
├── source
├── verificationState
├── reliability
└── provenance
```

Example:

```text
type = IMEI
maskedValue = ***********9247
normalizedHash = HMAC(...)
```

---

# 37. Identifier Normalization

Before hashing:

```text
trim
case normalization
remove non-semantic separators
validate format
```

Example:

```text
SN-123 456
```

and:

```text
sn123456
```

may normalize identically where appropriate.

Normalization depends on identifier type.

---

# 38. Identifier Hashing

Use:

```text
HMAC(secret, normalizedIdentifier)
```

or another approved keyed cryptographic mechanism.

Do not use:

```text
SHA256(identifier)
```

alone for low-entropy identifiers.

---

# 39. Identifier Comparison

Exact comparison:

```text
query normalized
 ↓
HMAC
 ↓
indexed lookup
```

The plaintext value does not need to be loaded during normal matching.

---

# 40. Contextual Identity

Context provides evidence about where and when the object may exist.

```text
ContextualIdentity
├── locationEvidence[]
├── timeEvidence[]
├── routeEvidence[]
├── environment
├── custodyContext
└── provenance
```

---

# 41. Location Evidence

```text
LocationEvidence
├── locationId
├── locationType
├── geoPoint
├── radius
├── hierarchyPath
├── confidence
├── source
└── observedAt
```

Possible sources:

```text
USER
GPS
STAFF
CCTV
ORGANIZATION
ROUTE
FEDERATION
SYSTEM
```

---

# 42. Location Uncertainty

A user may say:

```text
"somewhere around Joina"
```

This should not become:

```text
exact latitude/longitude
```

Instead:

```text
location = Joina City area
radius = approximate
confidence = medium
```

---

# 43. Time Evidence

```text
TimeEvidence
├── start
├── end
├── precision
├── rawExpression
├── source
└── confidence
```

Example:

```text
rawExpression = "around 2pm"
start = 13:30
end = 14:30
precision = APPROXIMATE
```

---

# 44. Route Context

Transport environments may use:

```text
RouteEvidence
├── routeId
├── vehicleId
├── stopIds[]
├── direction
├── confidence
└── provenance
```

This can improve matching beyond simple geo-distance.

---

# 45. Environment Context

Possible values:

```text
AIRPORT
BUS
TRAIN
MALL
UNIVERSITY
HOTEL
HOSPITAL
OFFICE
EVENT
STREET
OTHER
```

Matching rules may vary by environment.

Example:

```text
airport location tolerance
```

may be larger than:

```text
office room tolerance
```

---

# 46. Fingerprint Provenance

Every fingerprint must record which evidence contributed.

```text
FingerprintProvenance
├── sourceType
├── sourceId
├── evidenceId
├── modelRunId
├── processor
├── contentHash
└── addedAt
```

---

# 47. Model Provenance

Canonical:

```text
ModelProvenance
├── provider
├── model
├── version
├── configurationVersion
├── dimensions
├── promptVersion
├── generatedAt
└── inputHash
```

---

# 48. Coverage

Fingerprint coverage describes how much useful identifying evidence is available.

Conceptually:

```text
FingerprintCoverage
├── semantic
├── structured
├── globalVisual
├── localVisual
├── textual
├── ocr
├── protectedIdentity
├── location
├── temporal
└── overall
```

---

# 49. Coverage Example

```text
semantic        1.00
structured      0.72
global visual   1.00
local visual    0.68
text            0.90
OCR             0.00
protected ID    0.00
location        0.90
time            0.80

overall         0.71
```

OCR absence is not a mismatch.

It simply contributes no available OCR evidence.

---

# 50. Fingerprint Quality

Quality differs from coverage.

A fingerprint may have many fields but poor evidence quality.

Example:

```text
FingerprintQuality
├── sourceQuality
├── visualQuality
├── textQuality
├── identifierQuality
├── contextualQuality
└── overall
```

---

# 51. Image Quality

Possible signals:

```text
resolution
blur
occlusion
lighting
object coverage
view angle
compression artifacts
```

Low-quality images may still be usable but receive lower reliability.

---

# 52. Evidence Confidence vs Reliability

These concepts are different.

`confidence`

means:

> how certain was the producing model/source about this observation?

`reliability`

means:

> how trustworthy is this evidence type/source for identity matching?

Example:

```text
vision predicts colour black
confidence = 0.99
```

but colour may still have low identity reliability because many objects are black.

---

# 53. Content Hash

A fingerprint should have a deterministic content hash over its canonical evidence representation.

Use cases:

```text
duplicate-generation detection
cache
audit
reprocessing
reproducibility
```

The hash must not include fields such as:

```text
generatedAt
```

that change without changing fingerprint meaning.

---

# 54. Fingerprint Generation Trigger

Potential triggers:

```text
report submitted
media sanitized
new image uploaded
OCR completed
new protected identifier verified
new model deployed
manual evidence added
federated evidence imported where allowed
```

Not every trigger must generate immediately.

The fingerprint coordinator decides whether meaningful evidence changed.

---

# 55. Fingerprint Build State

Suggested task states:

```text
PENDING
WAITING_FOR_REQUIRED_EVIDENCE
BUILDING
READY
FAILED
SUPERSEDED
```

---

# 56. Required vs Optional Evidence

The required evidence depends on what exists.

Example:

Text-only report:

```text
required:
- normalized text
- semantic embedding

optional:
- translation
```

Photo-only found item:

```text
required:
- object crop
- global image embedding

optional:
- OCR
- local patches
- attribute extraction
```

Dzoka must support partial fingerprints.

---

# 57. Fingerprint Assembler

Pseudo-interface:

```text
FingerprintAssembler.build(
    ItemRecord item,
    List<SourceEvidence> sourceEvidence,
    List<DerivedEvidence> derivedEvidence,
    FingerprintBuildPolicy policy
) -> ItemFingerprint
```

The assembler does not perform inference.

---

# 58. Build Policy

`FingerprintBuildPolicy` may determine:

```text
which evidence is eligible
minimum confidence
required features
allowed secret attributes
active model versions
deduplication rules
```

Policies should be versioned.

---

# 59. Evidence Deduplication

Multiple AI processes may produce equivalent evidence.

Example:

```text
brand = Lenovo from OCR
brand = Lenovo from vision
brand = Lenovo from user
```

Do not simply discard duplicates.

Preserve sources while deriving a canonical attribute.

---

# 60. Canonical Attribute Resolution

Example:

```text
USER: Lenovo
OCR: Lenovo
VISION: Lenovo
```

Canonical:

```text
brand = Lenovo
confidence = very high
supportingSources = [USER, OCR, VISION]
```

If:

```text
USER: Lenovo
OCR: Dell
VISION: Lenovo
```

do not silently pick one.

Record the conflict/uncertainty.

---

# 61. Attribute Consensus

A conceptual aggregator may consider:

```text
source reliability
model confidence
number of independent sources
agreement
```

Output:

```text
CanonicalAttribute
├── value
├── confidence
├── supportingEvidence[]
├── conflictingEvidence[]
└── resolutionMethod
```

---

# 62. Multiple Photographs

The fingerprint should maintain photo-specific features.

Do not discard which embedding came from which image.

Example:

```text
Image A
front

Image B
underside

Image C
side
```

This is critical for ownership verification and local feature reasoning.

---

# 63. Pre-Loss Photos

Old owner photographs can be particularly strong evidence.

Mark provenance:

```text
PRE_LOSS_OWNER_PHOTO
```

This may have different reliability from:

```text
POST_REPORT_USER_UPLOAD
```

depending on policy.

---

# 64. Found-Item Photos

Found-item photographs should preserve:

```text
finder source
institution source
capture time
capture location
```

where known.

---

# 65. CCTV Fingerprints

CCTV-derived object evidence may form a temporary fingerprint or contribute to an existing fingerprint.

Example:

```text
CCTVObjectFingerprint
├── global embedding
├── local embedding
├── category
├── camera
├── timestamp
├── track
└── image crop
```

CCTV confidence may be reduced due to quality.

---

# 66. Fingerprint Types

Potential type field:

```text
LOST_REPORT
FOUND_REPORT
REGISTERED_ITEM
CCTV_OBSERVATION
FEDERATED_REMOTE
```

These describe provenance/use rather than changing the fundamental schema.

---

# 67. Registered Item Fingerprint

A pre-registered item may have richer evidence:

```text
serial
receipts
multiple photos
tags
known marks
ownership provenance
```

This can dramatically increase later match confidence.

---

# 68. Remote Federation Fingerprint

Federation should not transmit the entire internal fingerprint by default.

Create:

```text
FederatedFingerprintProjection
```

containing only policy-approved evidence.

---

# 69. Federation Projection

Potential structure:

```text
FederatedFingerprintProjection
├── anonymousFingerprintId
├── schemaVersion
├── category
├── safeAttributes[]
├── approximateLocation
├── timeWindow
├── permittedEmbeddings[]
├── embeddingMetadata[]
└── nodeId
```

Excluded by default:

```text
owner identity
phone
email
full serial
full IMEI
receipts
private challenge attributes
raw private photos
```

---

# 70. Embedding Sharing Policy

Embeddings must not automatically be treated as harmless.

They are derived from potentially private inputs.

Federation policy decides whether a particular vector may leave the node.

---

# 71. Fingerprint Projections

Maintain different projections:

```text
FullFingerprint
MatchingFingerprint
PublicFingerprint
FederatedFingerprint
VerificationFingerprint
```

---

# 72. Public Fingerprint

May expose:

```text
category
general colour
safe description
safe image
broad location
approximate time
```

Must not expose:

```text
secret marks
serial
IMEI
private OCR
ownership evidence
```

---

# 73. Matching Fingerprint

Contains the evidence necessary for the matching engine.

It may include restricted information if policy permits internal use.

---

# 74. Verification Fingerprint

May expose hidden attributes to the challenge engine.

Access must be tightly controlled and audited.

---

# 75. Fingerprint Storage Model

Recommended normalized storage:

```text
item_fingerprints
fingerprint_attributes
fingerprint_embeddings
fingerprint_local_features
fingerprint_ocr_features
fingerprint_distinctive_features
fingerprint_context
fingerprint_protected_features
fingerprint_provenance
```

A serialized JSON representation may additionally exist for transport/export.

---

# 76. `item_fingerprints`

Conceptual fields:

```text
id
item_id
tenant_id
version
schema_version
status
coverage
quality
content_hash
supersedes_fingerprint_id
created_at
```

Constraint:

```text
UNIQUE(item_id, version)
```

---

# 77. Embedding Storage

Canonical record:

```text
EmbeddingRecord
├── embeddingId
├── entityType
├── entityId
├── embeddingType
├── provider
├── model
├── modelVersion
├── dimensions
├── vector
├── contentHash
├── status
└── createdAt
```

---

# 78. Embedding Type

Examples:

```text
TEXT_RAW
TEXT_NORMALIZED
TEXT_CANONICAL
IMAGE_GLOBAL
IMAGE_PATCH
OCR_TEXT
DISTINCTIVE_REGION
CCTV_OBJECT
```

---

# 79. Vector Compatibility Key

Create a compatibility key conceptually from:

```text
embeddingType
model
modelVersion
dimensions
semanticSpaceVersion
```

Only compatible vectors may be directly compared.

---

# 80. Re-Embedding

When a new model is introduced:

```text
old embeddings remain
new embeddings generated
new fingerprint version or compatible feature revision generated
benchmark performed
active matching config switched
```

Never overwrite the old vectors.

---

# 81. Model Migration

Recommended sequence:

```text
Model A active
 ↓
Model B registered
 ↓
historical re-embedding batch
 ↓
offline benchmark
 ↓
shadow matching
 ↓
comparison
 ↓
promotion
 ↓
Model B active
 ↓
Model A eventually retired
```

---

# 82. Matching Use of Fingerprint

Retrieval may use:

```text
exact identifiers
structured attributes
search text
global image vectors
geo/time
```

Reranking may additionally use:

```text
local patch features
distinctive features
OCR
movement plausibility
```

---

# 83. Match Evidence from Fingerprints

Matching must convert comparisons into explicit evidence.

Example:

```text
Feature:
BRAND

Lost:
Lenovo

Found:
Lenovo

Score:
1.0

Reliability:
0.95

Source:
USER ↔ OCR
```

The fingerprint is therefore an evidence source, not merely an opaque object.

---

# 84. Fingerprint and Conflict Engine

Strong fields may generate hard conflicts.

Example:

```text
Lost IMEI HMAC = X
Found IMEI HMAC = Y
X != Y
```

produces:

```text
CRITICAL_IDENTIFIER_CONFLICT
```

The fingerprint must preserve enough information for the conflict engine without revealing plaintext.

---

# 85. Fingerprint and Ownership Challenges

Secret distinctive features may be used to generate ownership challenges.

Example feature:

```text
type = STICKER
description = yellow sticker on underside
classification = SECRET
discriminativeScore = 0.94
```

Challenge engine may transform this into:

```text
"What distinguishing feature is located underneath the device?"
```

---

# 86. Challenge Safety

A feature cannot be used as a secret challenge if it has already been publicly disclosed.

Track disclosure history.

---

# 87. Disclosure Record

Concept:

```text
EvidenceDisclosure
├── evidenceId
├── audience
├── channel
├── disclosedAt
└── disclosureType
```

Challenge selection queries this history.

---

# 88. Fingerprint and Fraud

Fingerprints may support fraud detection.

Examples:

```text
same claimant repeatedly claims unrelated items
same uploaded "proof" reused across cases
claim-provided photograph does not correspond to pre-loss timeline
```

Fraud logic remains separate from matching.

---

# 89. Fingerprint and Recovery Graph

Fingerprints identify items.

Recovery events identify movement.

Do not embed changing custody history inside the identity fingerprint itself.

Instead reference current/historical context separately.

---

# 90. Mutable vs Immutable Context

Identity-like evidence:

```text
scratch
serial
brand
model
```

is relatively stable.

Context:

```text
current location
custodian
case state
```

changes frequently.

The fingerprint should represent observation context, not become the authoritative live custody object.

---

# 91. Context Snapshot

A fingerprint may store context as of generation time.

Example:

```text
contextSnapshot.generatedAt
```

Later custody changes do not mutate that historical fingerprint.

---

# 92. Fingerprint Build Events

Relevant events:

```text
fingerprint.build.requested
fingerprint.build.started
fingerprint.ready
fingerprint.failed
fingerprint.superseded
```

---

# 93. Fingerprint Ready Event

Example payload:

```json
{
  "fingerprintId": "019...",
  "itemId": "019...",
  "fingerprintVersion": 3,
  "schemaVersion": 1,
  "coverage": 0.82,
  "contentHash": "sha256:..."
}
```

Do not publish the complete fingerprint into broad Kafka topics.

---

# 94. Fingerprint Build Idempotency

For the same:

```text
item
build policy
source evidence set
model configuration
```

a repeated build request should either:

- return the already existing equivalent fingerprint;
- or safely create only one fingerprint version.

Use the content hash/build key to prevent accidental duplicates.

---

# 95. Fingerprint Content Hash Inputs

May include:

```text
source evidence IDs
source content hashes
canonical attributes
model versions
build policy version
fingerprint schema version
```

Do not include unrelated volatile metadata.

---

# 96. Error Handling

Possible errors:

```text
NO_USABLE_EVIDENCE
REQUIRED_MODEL_UNAVAILABLE
INVALID_SOURCE_EVIDENCE
INCOMPATIBLE_EMBEDDING
PROTECTED_DATA_POLICY_VIOLATION
FINGERPRINT_BUILD_FAILED
```

---

# 97. Partial Failure

If OCR fails but image embedding succeeds:

```text
fingerprint may still become READY
```

with:

```text
OCR unavailable
```

if OCR was optional.

---

# 98. Required Failure

If the only usable photo cannot be decoded and no text exists:

```text
status = FAILED
```

or:

```text
WAITING_FOR_EVIDENCE
```

depending on workflow.

---

# 99. Quality Thresholds

Fingerprint build policy may reject extremely low-quality derived features.

Example:

```text
OCR confidence < threshold
```

may be stored for audit but excluded from matching.

---

# 100. Evidence Eligibility

Each evidence record may contain:

```text
eligibleForMatching
eligibleForPublicDisplay
eligibleForFederation
eligibleForVerification
```

These decisions may derive from policy.

---

# 101. Fingerprint API

Internal API examples:

```text
GET /internal/v1/items/{itemId}/fingerprints/current
GET /internal/v1/fingerprints/{fingerprintId}
POST /internal/v1/items/{itemId}/fingerprints/build
POST /internal/v1/items/{itemId}/fingerprints/rebuild
GET /internal/v1/fingerprints/{fingerprintId}/matching-view
GET /internal/v1/fingerprints/{fingerprintId}/federation-view
```

Public APIs should never return the full internal fingerprint.

---

# 102. Build Request

Example:

```json
{
  "itemId": "019...",
  "reason": "NEW_MEDIA",
  "policyVersion": "fingerprint-policy-v1"
}
```

Model selection is resolved through current model governance configuration rather than caller-supplied arbitrary models.

---

# 103. Fingerprint Service

Core-side responsibilities:

```text
request build
track current fingerprint
retrieve historical fingerprint
control visibility
publish fingerprint-ready event
```

AI-side responsibilities:

```text
generate derived evidence
```

Matching-side responsibilities:

```text
consume matching projection
```

---

# 104. Separation of Responsibilities

Incorrect:

```text
FingerprintService
    runs OCR
    runs OpenCLIP
    searches pgvector
    decides match
```

Correct:

```text
AI
→ evidence

Fingerprint
→ canonical representation

Matching
→ comparison
```

---

# 105. Fingerprint Schema Evolution

The fingerprint contract will evolve.

Example:

```text
schemaVersion 1
global embeddings

schemaVersion 2
local region features

schemaVersion 3
new provenance structure
```

Old fingerprints remain readable.

---

# 106. Schema Compatibility

Prefer additive evolution.

Consumers should tolerate unknown optional fields.

Breaking semantic changes require new schema versions and migration logic.

---

# 107. Historical Fingerprints

Never delete a historical fingerprint referenced by:

```text
MatchingRun
MatchDecision
VerificationDecision
audit
```

until retention policy explicitly permits it.

---

# 108. Fingerprint Retention

Derived fingerprint data may have retention different from source evidence.

Example:

If private source media is deleted, policy determines whether:

```text
derived embedding
```

may remain.

The safe default for privacy-sensitive evidence is to delete dependent derived representations when their legal basis disappears.

---

# 109. Fingerprint Deletion Graph

Deletion may cascade logically:

```text
Source Media
 ↓
Object Crop
 ↓
Embedding
 ↓
Local Features
 ↓
Fingerprint projection
```

But historical decision evidence may require protected archival.

Deletion policy must reconcile:

```text
privacy
auditability
legal hold
```

---

# 110. Evaluation Dataset

Fingerprint quality should be evaluated using datasets containing:

```text
same item, different angle
same item, different lighting
same model, different physical item
partial occlusion
old vs new photograph
damaged item
item inside clutter
text-only description
multilingual description
OCR-rich object
no identifiers
```

---

# 111. Hard Negative Dataset

Particularly important:

```text
two identical black iPhones
two identical Lenovo laptops
two identical school backpacks
two passports from same country
```

The evaluation must test same-category, same-model, different-instance negatives.

---

# 112. Fingerprint Metrics

Useful metrics:

```text
fingerprint build success rate
average build latency
coverage distribution
global embedding availability
local feature availability
OCR availability
identifier availability
rebuild rate
model-version distribution
```

---

# 113. Matching Evaluation Metrics

Fingerprint utility is ultimately measured through matching:

```text
candidate Recall@K
same-instance Recall@K
hard-negative false-match rate
MRR
Precision@K
```

---

# 114. Challenge Utility Metrics

Secret distinctive features may also be evaluated by:

```text
challenge discriminative success
challenge rejection rate
challenge public-leak rate
verification contribution
```

---

# 115. Observability

Fingerprint generation should produce spans such as:

```text
fingerprint.build
fingerprint.attributes.resolve
fingerprint.visual.aggregate
fingerprint.local-features.aggregate
fingerprint.protected-identity.resolve
```

---

# 116. Logs

Structured logs may include:

```text
fingerprintId
itemId
version
coverage
buildReason
duration
```

Do not log secret feature values.

---

# 117. Metrics

Examples:

```text
dzoka_fingerprint_build_total
dzoka_fingerprint_build_failed_total
dzoka_fingerprint_build_duration_seconds
dzoka_fingerprint_coverage
dzoka_fingerprint_local_features_count
```

---

# 118. Security Rules

The fingerprint subsystem must never:

1. expose secret identifiers through public projections;
2. log raw protected identifiers;
3. publish full fingerprints into public events;
4. allow one tenant to retrieve another tenant's fingerprint;
5. permit arbitrary model execution based on user input;
6. treat AI-derived identifiers as verified identifiers without validation;
7. expose hidden ownership-challenge evidence to claimants.

---

# 119. Model Trust

Model output must be treated as untrusted structured input.

Validate:

```text
schema
field lengths
allowed enum values
confidence ranges
embedding dimensions
model identity
```

before persistence.

---

# 120. Example Complete Fingerprint

Conceptual example:

```yaml
fingerprintId: fp_019...
itemId: item_019...
version: 3
schemaVersion: 1
status: ACTIVE

semanticIdentity:
  category: LAPTOP
  brand: Lenovo
  model: ThinkPad T14
  primaryColour: BLACK

structuredAttributes:
  - name: keyboard_layout
    value: US
    source: VISION
    confidence: 0.81

globalVisualFeatures:
  - mediaId: media_front
    embeddingId: emb_global_1
    model: ViT-B-32
    modelVersion: "1"

localVisualFeatures:
  - type: SCRATCH
    region: right_usb_c_area
    description: scratch beside USB-C port
    discriminativeScore: 0.91
    classification: SECRET

  - type: STICKER
    region: underside
    description: yellow rectangular sticker
    discriminativeScore: 0.95
    classification: SECRET

textIdentity:
  rawDescriptions:
    - "Black Lenovo laptop I left on the bus."

  canonicalDescription:
    "black Lenovo ThinkPad T14 laptop"

ocrFeatures:
  - text: "ThinkPad"
    confidence: 0.98

protectedIdentity:
  serialIdentifiers:
    - maskedValue: "*******9247"
      normalizedHash: "..."
      verificationState: USER_ASSERTED

contextualIdentity:
  locationEvidence:
    - locationId: bus_route_42
      confidence: 0.82

  timeEvidence:
    - start: "2026-10-04T12:30:00Z"
      end: "2026-10-04T13:30:00Z"
      precision: APPROXIMATE

coverage:
  semantic: 1.0
  structured: 0.72
  globalVisual: 1.0
  localVisual: 0.84
  text: 1.0
  ocr: 0.75
  protectedIdentity: 0.5
  location: 0.8
  temporal: 0.8
  overall: 0.82

contentHash: "sha256:..."

createdAt: "..."
```

---

# 121. Example Minimal Fingerprint

A valid fingerprint may be much smaller.

User says:

```text
"I lost my blue umbrella at Avondale."
```

Possible fingerprint:

```yaml
semanticIdentity:
  category: UMBRELLA
  primaryColour: BLUE

textIdentity:
  rawDescription:
    "I lost my blue umbrella at Avondale."

contextualIdentity:
  location:
    Avondale

coverage:
  overall: 0.34
```

Dzoka still searches.

The lower coverage naturally limits confidence.

---

# 122. Example Strong Identifier Fingerprint

A phone report containing IMEI:

```text
category = PHONE
brand = Samsung
model = Galaxy S25
IMEI hash = X
```

A found phone with matching verified IMEI may achieve strong identity evidence even without good photographs.

---

# 123. Example Conflict

Lost:

```text
IMEI hash = X
```

Found:

```text
IMEI hash = Y
```

The fingerprint comparison should create:

```text
CRITICAL IMEI CONFLICT
```

even if:

```text
colour matches
model matches
image similarity is high
location matches
```

---

# 124. Example Missing Evidence

Lost item:

```text
no image
```

Found item:

```text
image exists
```

Result:

```text
globalVisualScore = unavailable
```

not:

```text
globalVisualScore = 0
```

---

# 125. Example Multiple Visual Views

Owner provides:

```text
front photo
inside photo
side photo
```

Finder provides:

```text
front photo
bottom photo
```

Matching should find the best compatible evidence pairs rather than averaging all image comparisons indiscriminately.

---

# 126. Pairwise Visual Comparison

Possible design:

```text
for each compatible lost visual feature:
    compare against compatible found features

retain:
    best score
    top-N score distribution
    view diversity
```

The exact algorithm should be benchmarked.

---

# 127. Local Patch Comparison

For local features:

```text
query patches
    ↓
candidate patch search
    ↓
top local correspondences
    ↓
spatial/semantic consistency
    ↓
local visual score
```

This is expected to improve same-instance discrimination.

---

# 128. Distinctive Feature Comparison

Example:

```text
Lost:
small tear on lower right corner

Found:
tear on lower right corner
```

This evidence should typically receive higher identity importance than:

```text
colour = black
```

---

# 129. Provenance Comparison

Evidence can be stronger when independently supported.

Example:

```text
brand Lenovo
```

supported by:

```text
USER
OCR
VISION
```

should be more robust than an unsupported single inference.

---

# 130. Fingerprint Builder Pseudocode

Conceptually:

```text
function buildFingerprint(itemId):

    sources = loadEligibleSourceEvidence(itemId)
    derived = loadEligibleDerivedEvidence(itemId)

    validateTenantScope()

    canonicalIdentity =
        resolveSemanticIdentity(sources, derived)

    attributes =
        resolveAttributes(sources, derived)

    globalVisual =
        selectCompatibleGlobalVisualFeatures(derived)

    localVisual =
        selectDistinctiveLocalFeatures(derived)

    text =
        buildTextIdentity(sources, derived)

    ocr =
        classifyAndFilterOCR(derived)

    protectedIdentity =
        buildProtectedIdentity(sources, derived)

    context =
        resolveContext(sources, derived)

    coverage =
        calculateCoverage(...)

    quality =
        calculateQuality(...)

    contentHash =
        calculateCanonicalHash(...)

    if equivalentFingerprintExists(contentHash):
        return existingFingerprint

    persistImmutableFingerprint()

    publish fingerprint.ready
```

---

# 131. Fingerprint Comparison Is Not Fingerprint Building

Keep separate:

```text
FingerprintBuilder
```

and:

```text
FingerprintComparator / MatchingEngine
```

The builder describes one item.

The matcher compares two or more items.

---

# 132. Fingerprint Business Rules

The subsystem should enforce:

### FP-001

A fingerprint must belong to exactly one local `ItemRecord`.

### FP-002

A fingerprint version is immutable after activation.

### FP-003

A fingerprint must preserve provenance for derived evidence.

### FP-004

An embedding must identify its model and version.

### FP-005

Protected evidence cannot appear in public projections.

### FP-006

A fingerprint may be valid with incomplete evidence.

### FP-007

Missing evidence is never materialized as a zero similarity.

### FP-008

Historical fingerprints referenced by decisions cannot be silently rewritten.

### FP-009

Remote federated fingerprints remain projections, not authoritative local fingerprints.

### FP-010

AI-derived evidence cannot automatically upgrade a protected identifier to `VERIFIED`.

---

# 133. Source Audit Influence

This design incorporates source-level lessons from audited projects.

Important influences include:

```text
globalenglish01/lostfound
    → structured attributes
    → model-aware embeddings
    → raw input preservation
    → evidence provenance
    → missing != mismatch

OpenCLIP
    → multimodal global representation

ILIAS
    → instance-level retrieval concepts

Patchwise Retrieval
    → multi-scale local visual representation

CCTV Theft Finder
    → visual evidence from bounded CCTV investigations

OpenEPCIS
    → provenance/observation thinking
```

The Dzoka `ItemFingerprint` itself is a project-specific domain abstraction.

External inspiration does not imply direct code reuse.

---

# 134. Design Constraints

Do not implement the ItemFingerprint as:

```text
one JSON blob
```

with no normalized searchable evidence.

Do not implement it as:

```text
one CLIP vector
```

Do not make:

```text
serial
IMEI
private marks
```

public.

Do not overwrite embeddings during model upgrades.

Do not discard evidence provenance.

Do not fabricate missing fields.

Do not force every item to have the same evidence modalities.

---

# 135. Implementation Sequence

Recommended implementation order:

```text
1. fingerprint domain contracts
2. source/derived evidence contracts
3. semantic identity
4. structured attributes
5. text identity
6. global embeddings
7. OCR
8. protected identifiers
9. context
10. coverage/quality
11. immutable versioning
12. fingerprint projections
13. local visual features
14. distinctive feature extraction
15. federation projection
16. ownership-challenge integration
17. model migration tooling
18. benchmark and re-embedding tooling
```

This order is implementation sequencing, not scope reduction.

All capabilities remain part of the production target.

---

# 136. Testing Requirements

Unit tests must cover:

```text
canonical attribute resolution
missing evidence
conflicting attributes
coverage calculation
fingerprint immutability
content hashing
projection filtering
identifier protection
model-version compatibility
```

Integration tests must cover:

```text
build from text-only report
build from image-only found report
build from multimodal report
new evidence creates new version
model upgrade coexists with old vectors
public projection strips secret data
federation projection respects policy
```

---

# 137. Security Tests

Test that:

```text
public API cannot retrieve secret feature
claimant cannot inspect expected challenge evidence
cross-tenant fingerprint access fails
logs do not contain full identifiers
federation view excludes prohibited fields
old signed URLs cannot expose fingerprint media
```

---

# 138. Benchmark Requirements

Maintain benchmarks for:

```text
global visual retrieval
local patch retrieval
same-instance matching
same-model hard negatives
text-to-image retrieval
OCR-assisted matching
identifier-assisted matching
partial fingerprints
```

No model should be promoted based solely on generic public benchmark scores.

Evaluate against Dzoka-specific recovery tasks.

---

# 139. Operational Tooling

Eventually provide commands/jobs such as:

```text
fingerprint rebuild --item <id>
fingerprint inspect --item <id>
fingerprint compare <id-a> <id-b>
fingerprint reembed --model <version>
fingerprint coverage-report
fingerprint validate
```

Administrative tools must respect authorization and secret-data policy.

---

# 140. Relationship to Matching Engine

The next stage receives a fingerprint and asks:

```text
Which known item observations deserve comparison?
```

The fingerprint provides evidence.

The matching engine determines relationships.

Conceptually:

```text
ItemFingerprint
      ↓
Candidate Retrieval
      ↓
Feature Comparison
      ↓
Match Evidence
      ↓
Confidence
```

---

# 141. Relationship to Ownership Verification

After a candidate match:

```text
ItemFingerprint
      ↓
hidden/private features
      ↓
ChallengeCandidateSelector
      ↓
OwnershipChallenge
```

Only explicitly eligible secret evidence may be used.

---

# 142. Relationship to Federation

The local fingerprint is transformed:

```text
FullFingerprint
      ↓
FederationDisclosurePolicy
      ↓
FederatedFingerprintProjection
      ↓
Remote Node
```

Federation never receives unrestricted local evidence by default.

---

# 143. Relationship to Recovery Intelligence

Fingerprints describe identity.

Recovery intelligence describes likely movement.

Do not combine them into one ML object.

The matching engine may use recovery predictions as contextual evidence, but the ItemFingerprint remains primarily an identity/evidence representation.

---

# 144. Final Design Principle

Dzoka must treat physical-object identity as a collection of evidence, not a single number.

The fingerprint must answer:

```text
WHAT kind of object is this?

WHAT does it look like globally?

WHAT makes this exact instance distinctive?

WHAT text or labels are visible?

WHAT strong protected identifiers exist?

WHERE and WHEN was it observed?

WHERE did each piece of evidence come from?

HOW reliable is each piece of evidence?

WHICH model produced each derived feature?

WHAT may safely be shared publicly or federated?
```

The desired outcome is:

```text
Human evidence
     ↓
Multimodal evidence graph
     ↓
Versioned ItemFingerprint
     ↓
Fast candidate retrieval
     ↓
Same-instance reasoning
     ↓
Explainable match evidence
```

The `ItemFingerprint` is therefore not merely a data structure.

It is Dzoka's **canonical evidence model for representing the identity of a physical object under uncertainty**.