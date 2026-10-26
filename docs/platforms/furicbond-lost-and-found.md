# Source Audit — FuricBond/Lost-And-Found

> **Dzoka — The Open Recovery Network**
>
> Source audit of `FuricBond/Lost-And-Found`, with particular focus on its multimodal visual matching pipeline, image preprocessing, location-aware candidate filtering, and mutual-confirmation recovery workflow.

---

# 1. Audit Metadata

| Field | Value |
|---|---|
| Repository | `https://github.com/FuricBond/Lost-And-Found` |
| Upstream owner | `FuricBond` |
| Audited revision | `ebe3974ef0c0096a521836b934661fa6e4f99897` |
| Short revision | `ebe3974e` |
| Revision date | 2026-06-30 |
| Audit date | 2026-10-04 |
| Default branch | `main` |
| Primary language | Python |
| Backend | FastAPI |
| Runtime persistence | SQLite + SQLAlchemy Async |
| Alternative schema included | PostgreSQL + pgvector |
| Vision | Ultralytics YOLOv8 |
| Global image embeddings | OpenCLIP ViT-B/32 |
| Perceptual matching | pHash / dHash / aHash |
| Local features | OpenCV ORB |
| Location | Latitude/longitude + EXIF GPS |
| Authentication | JWT + bcrypt |
| Primary classification | **REFERENCE** |
| Secondary classification | **ADAPT CONCEPTS** |
| License status | **UNRESOLVED** |
| Direct code reuse | **DO NOT COPY until licensing is clarified** |

---

# 2. Executive Decision

The repository contains a real and useful multimodal visual matching implementation.

It is **not** merely a README claiming AI.

The inspected source genuinely implements:

```text
image
  ↓
orientation correction
  ↓
YOLO object detection
  ↓
object crop
  ↓
OpenCLIP embedding
  ↓
perceptual hashes
  ↓
ORB local descriptors
  ↓
location-aware candidate filtering
  ↓
multi-signal scoring
  ↓
ranked candidates
```

It also implements a useful mutual-confirmation workflow:

```text
potential match
    ↓
source user confirms
    +
target user confirms
    ↓
BOTH_CONFIRMED
    ↓
contact details revealed
```

However, Dzoka should classify this repository primarily as:

```text
REFERENCE
```

because the licensing state is not reliable enough for direct reuse.

The README claims MIT and links to `LICENSE`, but at the audited revision the repository root does **not** contain that file.

Therefore:

```text
study architecture
adapt concepts
independently implement
do not copy source
```

until the upstream author provides a valid license.

---

# 3. Why This Repository Matters to Dzoka

This project is particularly relevant because it attempts something most lost-and-found repositories do not:

> identify the same physical object using several complementary visual signals.

Instead of relying only on:

```text
description similarity
```

it combines:

```text
OpenCLIP
pHash
ORB
location
YOLO cropping
```

This directly influenced Dzoka's decision that `ItemFingerprint` should contain multiple visual evidence types rather than one global embedding.

---

# 4. Audit Scope

Primary files inspected:

```text
README.md
requirements.txt

app/
├── config.py
├── db/
│   └── database.py
├── models/
│   ├── item.py
│   ├── image.py
│   ├── embedding.py
│   └── match.py
├── services/
│   ├── image_processing.py
│   └── matching.py
└── api/
    └── routes/
        └── matches.py

scripts/
└── schema.sql

tests/
└── test_api.py
```

Repository root contents were also inspected to verify licensing.

---

# 5. Audit Limit

This is a targeted architecture/source audit.

It is not:

- a full penetration test;
- a complete dependency-license audit;
- a line-by-line review;
- an independent reproduction of ML accuracy;
- a legal opinion.

---

# 6. Licensing Finding

The repository README explicitly states:

```text
MIT License
```

and displays an MIT badge linked to:

```text
LICENSE
```

However, at:

```text
ebe3974ef0c0096a521836b934661fa6e4f99897
```

the repository root does not contain:

```text
LICENSE
```

and fetching that path returns not found.

---

# 7. Licensing Decision

Dzoka must therefore treat the repository as:

```text
LICENSE = UNRESOLVED
```

not:

```text
LICENSE = MIT
```

for reuse purposes.

The README statement alone is insufficient.

---

# 8. Dzoka Licensing Rule

Until clarified:

### Allowed

```text
study architecture
understand algorithms
document behaviour
independently implement concepts
```

### Not allowed

```text
copy source files
copy functions
copy substantial code fragments
port implementation with superficial renaming
```

---

# 9. Dependency Licensing

The project itself also depends on separately licensed packages including:

```text
Ultralytics
OpenCLIP
OpenCV
ImageHash
PyTorch
```

Their licenses must be evaluated independently when Dzoka selects equivalent runtime providers.

The repository's own claimed license would not automatically override dependency licenses.

---

# 10. Repository Architecture

The implementation is a compact monolithic FastAPI application:

```text
Browser / CLI
      ↓
FastAPI
      ↓
Application Routes
      ↓
Services
 ├── image processing
 ├── matching
 ├── auth
 └── notification
      ↓
SQLAlchemy
      ↓
SQLite
      ↓
Local image storage
```

This is a useful prototype/reference architecture but not a direct fit for Dzoka's broader production scope.

---

# 11. Runtime Database Reality

One important finding is that the repository contains **two different persistence stories**.

The active application uses:

```text
SQLite
+
aiosqlite
+
JSON-serialized embeddings
```

as shown by:

```text
app/config.py
app/db/database.py
app/models/embedding.py
requirements.txt
```

---

# 12. PostgreSQL Schema Exists Separately

The repository also contains:

```text
scripts/schema.sql
```

which defines:

```text
PostgreSQL
+
pgvector
+
HNSW
```

and stores embeddings as:

```text
vector(512)
```

This is useful architecture material.

However, it is not the persistence backend used by the inspected runtime application.

---

# 13. Important Documentation Divergence

The README describes:

```text
Vector Similarity Search
Top 50
```

and the repository contains a PostgreSQL/pgvector schema.

But the actual inspected matching service:

```text
app/services/matching.py
```

does not issue a pgvector ANN query.

Instead it:

```text
loads metadata-filtered candidate rows
    ↓
deserializes JSON vectors
    ↓
computes cosine similarity in NumPy
```

for each candidate.

---

# 14. Dzoka Lesson

This is exactly why source audits matter.

README architecture:

```text
metadata filter
    ↓
vector search
    ↓
top 50
```

Actual runtime:

```text
metadata SQL query
    ↓
distance filtering in application
    ↓
NumPy cosine across remaining rows
    ↓
weighted ranking
```

Those are not equivalent at production scale.

---

# 15. AI Image Processing Pipeline

`app/services/image_processing.py` implements a genuine image-processing pipeline:

```text
raw image
    ↓
EXIF extraction
    ↓
orientation correction
    ↓
YOLO detection
    ↓
crop main object
    ↓
OpenCLIP embedding
    ↓
pHash / dHash / aHash
    ↓
ORB descriptors
    ↓
thumbnail
```

This is the strongest part of the project for Dzoka.

---

# 16. YOLO Object Detection

The implementation uses:

```text
Ultralytics YOLO
```

and loads a configurable model path.

Default configuration:

```text
yolov8n.pt
```

The detector is lazily loaded and moved to:

```text
CUDA
```

when available, otherwise CPU.

---

# 17. Detection Strategy

When multiple detections exist, the implementation selects:

```text
the detection with the highest confidence
```

and crops it with additional padding.

Conceptually:

```text
image
 ↓
YOLO detections
 ↓
highest confidence detection
 ↓
10% padding
 ↓
cropped object
```

---

# 18. Strength of Object Cropping

This is a good design direction.

Uploaded photographs frequently contain:

```text
table
hands
floor
people
bags
background clutter
```

Cropping the probable target object before producing embeddings can improve retrieval quality.

Dzoka retains this concept.

---

# 19. Object-Crop Weakness

Selecting only:

```text
highest-confidence YOLO detection
```

can be wrong.

Example photograph:

```text
person holding lost handbag
```

YOLO may assign greater confidence to:

```text
person
```

than:

```text
handbag
```

and therefore crop the wrong object.

---

# 20. Dzoka Improvement

Dzoka should support:

```text
category-aware detection
+
multiple candidate crops
+
user-selected target region
+
segmentation where beneficial
```

rather than always selecting the single highest-confidence box.

---

# 21. Detector Fallback

If no object is detected, the upstream implementation returns:

```text
original image
```

rather than failing the entire processing workflow.

That graceful fallback is useful.

---

# 22. OpenCLIP

The inspected code loads:

```text
ViT-B-32
```

using OpenCLIP with:

```text
laion2b_s34b_b79k
```

pretrained weights.

Image embeddings are normalized before storage/comparison.

This is a real OpenCLIP pipeline.

---

# 23. Global Embedding Purpose

Global embeddings help answer:

```text
Does this image broadly resemble the same kind of object?
```

Examples:

```text
black backpack
red wallet
silver laptop
blue headphones
```

This is useful candidate-generation evidence.

---

# 24. Global Embedding Limitation

Global embedding similarity alone is weak for distinguishing:

```text
MacBook A
```

from:

```text
MacBook B
```

when both are visually similar production models.

This project partially addresses that limitation through pHash and ORB.

Dzoka extends it further through local patch identity.

---

# 25. Perceptual Hashing

The pipeline generates:

```text
pHash
dHash
aHash
```

using `imagehash`.

The matching engine currently uses:

```text
pHash
```

as a matching signal.

---

# 26. pHash Comparison

Similarity is computed from Hamming distance between the 64-bit hashes:

```text
similarity =
1 - hammingDistance / 64
```

This is simple and computationally cheap.

---

# 27. Strength of pHash

Perceptual hashing is useful for:

```text
near-duplicate images
small resize changes
compression changes
minor image transformations
```

It can be useful evidence when an owner and finder happen to have highly similar photographs.

---

# 28. pHash Limitation

pHash is not a robust general same-object-instance solution across:

```text
different angles
different backgrounds
different lighting
partial occlusion
different camera viewpoints
```

It should therefore remain a supporting evidence type rather than an identity oracle.

---

# 29. ORB Features

The implementation extracts:

```text
OpenCV ORB
```

keypoints and descriptors.

Configuration includes up to approximately:

```text
500 features
```

with multiscale detection.

Descriptors and keypoint coordinates are serialized and stored.

---

# 30. ORB Matching

The matcher uses:

```text
BFMatcher
NORM_HAMMING
crossCheck=True
```

and considers descriptor matches with:

```text
distance < 50
```

as good matches.

The score is approximately:

```text
goodMatches
──────────────
minimum descriptor count
```

capped at 1.

---

# 31. Why ORB Is Interesting for Dzoka

ORB introduces local evidence.

Unlike a global embedding, it may detect correspondence around:

```text
logos
stickers
text
scratches
corners
surface patterns
```

This supports Dzoka's conclusion that physical-instance matching should contain local visual evidence.

---

# 32. ORB Limitation

The implementation does not appear to perform robust geometric verification such as:

```text
homography estimation
RANSAC
spatial-consistency filtering
```

before producing the final score.

Therefore unrelated local patterns may generate apparently good descriptor matches.

---

# 33. Dzoka Extension

Dzoka's local-instance design should support:

```text
patch embeddings
+
distinctive-region detection
+
local correspondences
+
spatial consistency
+
structured visual marks
```

rather than relying solely on ORB match count.

---

# 34. Multi-Signal Matching

The matching engine combines four signals:

```text
CLIP        40%
pHash       25%
ORB         20%
location    15%
```

Conceptually:

```text
score =
0.40 × vector
+
0.25 × phash
+
0.20 × orb
+
0.15 × location
```

---

# 35. Strength of Multi-Signal Design

This is substantially better than:

```text
CLIP cosine only
```

because different methods capture different kinds of similarity.

It supports one of Dzoka's major design principles:

> Physical-object identity should be represented by multiple evidence dimensions.

---

# 36. Weakness — Hard-Coded Weights

The weights are embedded directly in:

```text
MatchingEngine.__init__()
```

They are not:

```text
category-specific
versioned
experiment-controlled
calibrated
```

Dzoka must not copy the exact formula.

---

# 37. Dzoka Improvement

Dzoka uses:

```text
MatchingProfile
```

with:

```text
version
category
environment
feature weights
confidence policy
```

instead.

---

# 38. Missing Evidence Problem

A major weakness is that missing visual evidence frequently produces:

```text
0.0
```

for signals such as:

```text
pHash
ORB
```

rather than:

```text
unavailable
```

---

# 39. Why This Matters

If ORB cannot extract descriptors because the photograph is blurred:

Incorrect interpretation:

```text
ORB score = 0
→ evidence says different item
```

Correct interpretation:

```text
ORB evidence unavailable
```

These are not the same.

---

# 40. Dzoka Decision

Dzoka explicitly rejects this behaviour.

The invariant remains:

> **Missing evidence ≠ mismatch.**

Every feature must support:

```text
available = false
score = null
```

when comparison is impossible.

---

# 41. Location Matching

Items store:

```text
latitude
longitude
location_name
```

The matching engine calculates geodesic distance between the lost and found item.

---

# 42. Candidate Radius

Candidates beyond:

```text
radius_km
```

are discarded.

Default:

```text
50 km
```

according to configuration.

---

# 43. Location Score

For candidates inside the radius:

```text
score =
1 - distance / maximumDistance
```

This creates a linear proximity score.

---

# 44. Good Lesson

Location is treated as supporting evidence alongside image similarity.

This is directionally correct.

---

# 45. Major Limitation — Hard Radius Gate

A strict radius can eliminate valid cases.

Example:

```text
item lost on bus
    ↓
bus travels 80 km
    ↓
item found at depot
```

A 50 km filter removes the correct candidate before matching.

---

# 46. Dzoka Improvement

Dzoka should use:

```text
location evidence
+
facility hierarchy
+
routes
+
transport movement
+
institutional transition models
```

rather than treating distance as a universal hard condition.

---

# 47. Time Filtering

The system creates:

```text
event_date ± configured days
```

and excludes candidates outside that window.

Default:

```text
30 days
```

---

# 48. Time Filtering Limitation

A hard symmetric time window is simple but can create false negatives.

Dzoka should use:

```text
TimeWindow
+
temporal plausibility
+
confidence decay
```

with only genuinely impossible chronology treated as a hard conflict.

---

# 49. Same-Category Hard Filter

Candidates must satisfy:

```text
Item.item_type == source_item.item_type
```

This is another potential false-negative source.

---

# 50. Why Category Gating Is Dangerous

Suppose:

```text
owner selects ELECTRONICS
finder selects HEADPHONES
```

for the same wireless headphones.

The correct candidate will never be compared.

---

# 51. Contrast with Dzoka

Dzoka's matching architecture deliberately treats uncertain category as:

```text
retrieval signal
+
scoring feature
```

rather than an unconditional gate.

Strong verified identity fields may be hard filters.

AI/user category labels normally should not.

---

# 52. Multiple Image Support

The product supports multiple images per item.

The README advertises up to:

```text
6 images
```

and the data model supports multiple image records.

This is a good product decision.

---

# 53. Matching Implementation Uses Only One Source Embedding

Despite multiple-image storage, the inspected matching code does:

```text
source_embeddings[0]
```

for:

```text
CLIP
pHash
ORB
```

---

# 54. Candidate Images Are Also Reduced to One

The candidate-building loop uses:

```text
seen_items
```

and keeps only one candidate image per item.

Therefore the matching engine does not fully exploit the multi-view image capability.

---

# 55. Why This Is Important

Suppose the owner provides:

```text
front
back
underside
```

while finder provides:

```text
side
underside
```

The most distinctive match might be:

```text
underside ↔ underside
```

but it can be lost if only the first image is compared.

---

# 56. Dzoka Improvement

Dzoka should keep image-specific evidence and perform bounded multi-view comparison.

Potential design:

```text
source views
    ×
candidate views
    ↓
compatible pair selection
    ↓
best correspondences
    ↓
view-diversity-aware aggregation
```

---

# 57. EXIF Metadata

The image processor extracts EXIF data before image transformations.

It includes support for:

```text
GPS coordinates
```

and orientation.

This is useful provenance.

---

# 58. EXIF GPS

The code parses GPS latitude and longitude where available.

This can provide useful context.

However:

```text
EXIF GPS
```

should not automatically be considered trusted ground truth.

Metadata can be:

```text
missing
wrong
edited
copied
```

---

# 59. Dzoka EXIF Policy

Dzoka should treat EXIF as:

```text
derived contextual evidence
```

with provenance and reliability.

Never as unquestionable location proof.

---

# 60. Match Candidate Model

The engine stores a transparent score breakdown:

```text
vector_similarity
phash_similarity
orb_match_score
location_score
overall_score
distance_km
```

This is a good explainability direction.

---

# 61. Dzoka Extension

Dzoka expands this into:

```text
MatchEvidence[]
```

where each feature additionally stores:

```text
availability
reliability
weight
provenance
relation
contribution
explanation
```

---

# 62. No Explicit Conflict Engine

The project does not provide Dzoka's explicit hard-conflict architecture.

Example:

```text
IMEI A
```

versus:

```text
IMEI B
```

is not modeled as a first-class rejecting conflict within the inspected visual matcher.

---

# 63. Consequence

A generic weighted similarity formula can incorrectly produce:

```text
high visual score
```

even where a strong identifier proves the candidate is wrong.

Dzoka therefore keeps:

```text
ConflictEngine
```

separate from similarity scoring.

---

# 64. No Evidence Coverage Model

The project calculates an overall score but does not implement the full Dzoka concept of:

```text
EvidenceCoverage
```

where missing modalities reduce confidence separately from similarity.

This is a major difference.

---

# 65. No Calibrated Confidence

`overall_score` is essentially a weighted similarity score.

It should not be interpreted as:

```text
probability these are the same physical item
```

Dzoka separates:

```text
MatchScore
```

from:

```text
MatchConfidence
```

and plans calibration on labeled recovery data.

---

# 66. Mutual Confirmation Workflow

`MatchStatus` includes:

```text
PENDING
SOURCE_CONFIRMED
TARGET_CONFIRMED
BOTH_CONFIRMED
REJECTED
EXPIRED
```

This is a useful explicit state machine.

---

# 67. Confirmation Flow

When source user confirms:

```text
SOURCE_CONFIRMED
```

When target user confirms:

```text
TARGET_CONFIRMED
```

When both have confirmed:

```text
BOTH_CONFIRMED
```

and both associated items are marked:

```text
MATCHED
```

---

# 68. Contact Privacy

Before both users confirm:

```text
contact_info = hidden
```

After:

```text
BOTH_CONFIRMED
```

the system returns:

```text
full name
email
phone number
```

of the other user.

---

# 69. Positive Privacy Lesson

Mutual confirmation before revealing contact data is much better than publishing:

```text
finder phone number
owner phone number
```

with the listing.

Dzoka should preserve the principle of delayed disclosure.

---

# 70. Dzoka Improvement — Secure Relay

Dzoka should generally avoid revealing direct contact information even after matching.

Preferred:

```text
RecoveryConversation
```

through which parties communicate using Dzoka.

Contact exposure should require explicit policy/consent.

---

# 71. Confirmation Is Not Ownership Verification

The largest conceptual limitation of the recovery workflow is:

```text
both users click confirm
```

becomes sufficient to reveal contact information and progress the match.

This is not equivalent to verifying ownership.

---

# 72. Example Attack

A fraudulent claimant may see a candidate and simply select:

```text
Confirm
```

The finder might also confirm because the claimant appears plausible.

This creates social-engineering risk.

---

# 73. Dzoka Improvement

Dzoka requires:

```text
MatchCandidate
    ↓
Claim
    ↓
private ownership evidence
    ↓
VerificationDecision
    ↓
RecoveryAuthorization
```

before protected physical recovery.

---

# 74. Mutual Confirmation Still Has Value

The concept remains useful for:

```text
low-risk acknowledgement
finder agreement
owner interest
two-sided handover confirmation
```

but not as the complete trust mechanism.

---

# 75. Match Rejection

Either participant can reject a potential match.

The system stores:

```text
rejected_at
rejected_by
rejection_reason
```

This is useful feedback.

---

# 76. Dzoka ML Feedback

Rejected and confirmed candidates can become useful:

```text
hard negatives
positive match labels
```

for future model evaluation.

Dzoka formalizes this through:

```text
FeedbackRecord
```

rather than relying only on match status.

---

# 77. Storage Design

Images are stored outside database rows.

The runtime model records:

```text
original path
processed path
thumbnail path
```

This is a good pattern.

---

# 78. Dzoka Storage Improvement

Dzoka generalizes this to:

```text
S3 / MinIO
```

with:

```text
MediaAsset
MediaDerivative
lineage
classification
content hashes
signed access
```

rather than server-local paths.

---

# 79. Security Strengths

Useful security patterns include:

```text
JWT authentication
bcrypt password hashing
rate limiting
input validation
match ownership authorization
contact information delayed until confirmation
```

---

# 80. Match Authorization

The match API explicitly verifies that the current user owns either:

```text
source item
```

or:

```text
target item
```

before allowing match inspection or confirmation.

That is a good authorization baseline.

---

# 81. Security Weakness — Default JWT Secret

Configuration contains a default:

```text
change-this-in-production
```

for the JWT signing secret.

This is convenient for development but dangerous if startup does not prohibit it in production.

---

# 82. Dzoka Secret Rule

Dzoka should fail startup in production when:

```text
default credential
weak signing key
missing required secret
```

is detected.

---

# 83. Security Weakness — Debug Default

Configuration defaults:

```text
debug = true
```

Again, reasonable for local development but unsuitable as a production-safe default.

Dzoka production configuration should default toward secure failure.

---

# 84. File Upload Security

The project has:

```text
content-type controls
size controls
```

but Dzoka needs a stronger pipeline including:

```text
MIME sniffing
malware scanning
EXIF handling
metadata sanitization
decompression-bomb protection
safe image re-encoding
private derivative policy
```

---

# 85. Pickle Use

ORB descriptors are serialized with:

```text
pickle
```

and later deserialized by the matcher.

This deserves caution.

---

# 86. Pickle Security Risk

Python pickle is unsafe when applied to untrusted serialized bytes because deserialization can execute arbitrary code.

In this repository, descriptors are internally generated, which reduces exposure.

However, Dzoka should avoid designing durable interchange/storage formats around pickle.

---

# 87. Dzoka Decision

Prefer formats such as:

```text
NumPy safe arrays
binary protobuf
Arrow
explicit numeric serialization
```

depending on the subsystem.

Never deserialize externally controlled pickle payloads.

---

# 88. Scalability Limitation

Runtime matching performs similarity calculations in Python/NumPy after loading metadata-filtered candidate records.

Conceptually:

```text
SQL candidate rows
    ↓
application loop
    ↓
NumPy cosine
    ↓
pHash
    ↓
ORB
```

This will not scale gracefully to very large datasets.

---

# 89. Dzoka Retrieval Architecture

Dzoka instead uses:

```text
indexed candidate retrieval
    ↓
small shortlist
    ↓
feature comparison
    ↓
expensive reranking
```

with:

```text
pgvector
FTS
pg_trgm
PostGIS
```

as the initial default.

---

# 90. PostgreSQL Schema as Useful Reference

Although it is not the active runtime backend, `scripts/schema.sql` demonstrates a useful future direction:

```text
PostgreSQL
+
pgvector
+
HNSW
```

with fields for:

```text
CLIP vector
pHash
ORB
YOLO metadata
match scores
```

This is useful conceptually.

---

# 91. But Dzoka Should Not Copy the Schema Directly

The schema lacks important Dzoka requirements:

```text
tenant boundaries
model versioning
fingerprint versioning
evidence provenance
hard conflicts
coverage
verification
custody
federation
event outbox
```

---

# 92. Model Provenance Gap

`ImageEmbedding` stores:

```text
embedding_json
phash
ORB
YOLO information
```

but not a sufficient model identity record for long-term reproducibility.

For example, the table does not strongly encode:

```text
OpenCLIP model version
pretrained checkpoint
preprocessing version
YOLO version
fingerprint algorithm version
```

per generated artifact.

---

# 93. Dzoka Improvement

Every derived feature must have:

```text
provider
model
version
configuration
input hash
generatedAt
```

or reference a `ModelRun`.

---

# 94. Fingerprint Gap

This project stores useful visual features but does not have Dzoka's complete:

```text
ItemFingerprint
```

abstraction.

Its visual data is primarily:

```text
image embedding
perceptual hash
ORB
YOLO class
```

---

# 95. Dzoka Fingerprint Extension

Dzoka additionally requires:

```text
semantic identity
structured attributes
text
OCR
protected identifiers
global visual
local patch visual
distinctive features
location/time
quality
provenance
coverage
```

---

# 96. Text Matching Gap

The inspected core matcher is overwhelmingly image-driven.

The item has:

```text
title
description
```

but these do not form a rich hybrid retrieval/scoring path comparable to Dzoka's:

```text
FTS
trigram
semantic text retrieval
structured attributes
OCR
```

---

# 97. Dzoka Lesson

Visual AI should complement—not replace—textual and structured evidence.

The strongest architecture combines:

```text
visual
+
text
+
attributes
+
identifiers
+
location
+
time
```

---

# 98. Test Coverage

The README reports:

```text
17 passed
```

tests.

The inspected test suite covers areas including:

```text
health
registration
authentication
item CRUD
notifications
```

---

# 99. Core Matching Test Gap

Code search of the inspected repository did not identify tests in `tests/` directly exercising:

```text
MatchingEngine
find_matches
CLIP
ORB
image processing
mutual match confirmation
```

at the pinned revision.

That is a significant quality gap for the project's defining AI capability.

---

# 100. Dzoka Testing Requirement

The Dzoka visual matcher must have golden tests for:

```text
same item / different angle
same item / different lighting
same model / different instance
background changes
object crop failure
multiple objects
missing ORB
poor-quality photo
near-duplicate images
multiple views
hard identifier conflict
```

---

# 101. Good Architectural Ideas to Preserve

The most valuable concepts are:

```text
object detection before embedding

global semantic image embedding

perceptual hash as an independent signal

local keypoint features as another signal

location-aware ranking

transparent score breakdown

multiple uploaded images

EXIF extraction

mutual confirmation

contact privacy until agreement
```

---

# 102. Important Concepts Not to Preserve Literally

Do not copy:

```text
same-category hard gate

fixed geographic hard radius

fixed time hard window

missing ORB = zero match

missing pHash = zero match

hard-coded global weights

first image only

one candidate image only

un-calibrated overall score

contact reveal as ownership verification

pickle as durable feature interchange

application-side full candidate cosine loop
```

---

# 103. Influence on `ItemFingerprint`

This audit is one of the reasons Dzoka's fingerprint includes:

```text
globalVisual
localVisual
perceptualHashes
distinctiveRegions
context
```

rather than only:

```text
imageEmbedding
```

---

# 104. Influence on Local Visual Design

The upstream ORB pipeline demonstrates that local image structure can provide complementary evidence.

Dzoka generalizes this concept into:

```text
LocalVisualFeature
├── embedding
├── region
├── scale
├── salience
├── distinctiveType
└── provenance
```

---

# 105. Influence on Perceptual Hash Design

Dzoka may retain perceptual hashes for:

```text
duplicate image detection
near-duplicate evidence
media deduplication
candidate support
```

but not as a primary same-instance identity mechanism.

---

# 106. Influence on Media Processing

Useful processing pattern:

```text
Original Media
    ↓
orientation correction
    ↓
object crop
    ↓
derived visual evidence
```

Dzoka extends this with explicit derivative lineage.

---

# 107. Dzoka Media Derivative Model

Conceptually:

```text
MediaAsset
├── original
├── sanitized
├── object crop
├── thumbnail
├── OCR derivative
├── privacy-redacted derivative
└── model input derivative
```

Every derivative records its source.

---

# 108. Influence on Candidate Matching

The upstream project proves the usefulness of combining heterogeneous visual signals.

Dzoka retains this philosophy but replaces the simple fixed weighted sum with:

```text
retrieval
    ↓
feature evidence
    ↓
reliability
    ↓
coverage
    ↓
conflict engine
    ↓
bounded reranking
    ↓
calibrated confidence
```

---

# 109. Influence on Match Explainability

A user/reviewer should be able to see:

```text
global visual similarity
local feature correspondence
location relation
OCR relation
distinctive features
conflicts
```

instead of receiving:

```text
94% AI match
```

with no explanation.

---

# 110. Influence on Ownership Flow

Mutual confirmation demonstrates that both parties can participate in recovery.

Dzoka adapts that concept into:

```text
claimant confirmation
finder/institution acknowledgement
private verification
two-sided handover confirmation
```

while keeping actual ownership verification separate.

---

# 111. Influence on Privacy

The repository correctly avoids exposing contact data immediately.

Dzoka strengthens that principle further:

```text
identity/contact separation
secure messaging relay
minimal disclosure
verification privacy
```

---

# 112. Comparison with globalenglish01/lostfound

The two projects are useful for different reasons.

`globalenglish01/lostfound` is stronger in:

```text
structured evidence
hybrid retrieval
matching provenance
hard conflicts
missing evidence handling
coverage
configuration
```

`FuricBond/Lost-And-Found` is stronger as a direct example of:

```text
YOLO crop
OpenCLIP
pHash
ORB
GPS
visual multimodal pipeline
mutual confirmation UX
```

Dzoka should combine lessons from both rather than pick one repository wholesale.

---

# 113. Dzoka Synthesis

The resulting Dzoka architecture becomes:

```text
globalenglish lessons
    +
FuricBond visual lessons
    +
ILIAS / Patchwise instance retrieval
    +
OpenCLIP
    +
PaddleDetection
    +
PaddleOCR
        ↓
Dzoka ItemFingerprint
        ↓
Dzoka Matching Engine
```

---

# 114. Adopt

Because the upstream repository license is unresolved:

```text
ADOPT DIRECT CODE:
NONE
```

Individual underlying libraries may be adopted only after their own dependency/source audit.

---

# 115. Adapt Concepts

Dzoka should independently implement concepts inspired by:

```text
object-aware image cropping

multi-signal visual identity

OpenCLIP-style global embeddings

perceptual hash evidence

local keypoint evidence

EXIF provenance

multiple-image upload

transparent visual score components

mutual confirmation states
```

---

# 116. Reference

Use as reference for:

```text
FastAPI AI-service organization
visual preprocessing order
simple visual matching UX
contact privacy
match confirmation workflow
image derivative storage
```

---

# 117. Explicitly Reject

Dzoka should reject:

```text
README license claim as sufficient licensing evidence

category as unconditional matching gate

radius as unconditional matching gate

time range as universal hard gate

missing visual signal = zero similarity

first-image-only matching

one-candidate-image-only matching

fixed universal scoring weights

overall weighted score = confidence

contact reveal = ownership verification

runtime NumPy scan as large-scale vector search

pickle as external/durable trust format
```

---

# 118. Architecture Impact — Visual Fingerprint

Before this source family was inspected, a simplistic design might have used:

```text
one image
    ↓
CLIP embedding
```

This audit reinforces:

```text
image
├── object crop
├── global embedding
├── perceptual hash
├── local visual features
├── detection metadata
└── context
```

as a better starting point.

---

# 119. Architecture Impact — Multiple Views

The limitations of the upstream first-image strategy reinforce Dzoka's requirement to preserve:

```text
media-specific visual features
```

rather than collapsing all imagery prematurely.

---

# 120. Architecture Impact — Missing Evidence

The repository also provides a useful counterexample.

Its missing pHash/ORB handling reinforces why Dzoka needs:

```text
available = false
```

instead of:

```text
score = 0
```

for unavailable comparison channels.

---

# 121. Architecture Impact — Retrieval at Scale

The difference between the README and active runtime implementation reinforces Dzoka's architecture:

```text
indexed retrieval first
```

rather than:

```text
load candidates
+
scan vectors in application memory
```

at production scale.

---

# 122. Architecture Impact — Recovery Trust

The mutual confirmation feature is useful, but its limitations reinforce Dzoka's separation:

```text
Match
≠
Ownership
≠
Recovery Authorization
```

---

# 123. Relevant Dzoka Documents

This audit directly influences:

```text
DESIGN.md

docs/design/item-fingerprint.md
docs/design/matching-engine.md
docs/design/ownership-verification.md
docs/design/claim-workflow.md

docs/architecture/ai-architecture.md
docs/architecture/data-architecture.md
docs/architecture/security-architecture.md
docs/architecture/privacy-architecture.md
```

---

# 124. Recommended Source-Audit Lock Entry

```json
{
  "id": "furicbond-lost-and-found",
  "category": "platforms",
  "repository": "https://github.com/FuricBond/Lost-And-Found",
  "revision": "ebe3974ef0c0096a521836b934661fa6e4f99897",
  "license": "UNRESOLVED",
  "licenseNote": "README claims MIT but LICENSE file is absent at audited revision.",
  "classification": "REFERENCE",
  "secondaryClassification": "ADAPT_CONCEPTS",
  "audit": "docs/source-audit/platforms/furicbond-lost-and-found.md",
  "auditedAt": "2026-10-04"
}
```

---

# 125. Re-Audit Triggers

Re-audit if upstream:

```text
adds a valid LICENSE

changes image matcher

adds actual pgvector runtime retrieval

adds model/version provenance

adds multimodal text matching

adds ownership verification

changes Ultralytics dependency/licensing approach

adds strong matching tests
```

The licensing trigger is especially important because a valid upstream license could change the reuse classification.

---

# 126. Source Audit Summary

## What the repository genuinely implements

```text
YOLO object detection
OpenCLIP image embeddings
perceptual hashes
ORB descriptors
EXIF GPS parsing
location-aware matching
time filtering
multi-signal visual score
mutual confirmation
contact privacy before confirmation
JWT authentication
```

## What it does not provide sufficiently for Dzoka

```text
full multimodal ItemFingerprint

text/attribute hybrid matching

missing-evidence normalization

hard identity conflicts

evidence coverage

calibrated confidence

multi-view visual matching

production vector retrieval

ownership verification

claims and entitlement

custody

federation

CCTV edge processing

movement intelligence

multi-tenancy

production-grade model provenance
```

---

# 127. Final Assessment

`FuricBond/Lost-And-Found` is a valuable visual-matching reference because its AI pipeline is substantially real.

The most important implementation chain is:

```text
RAW PHOTO
   ↓
EXIF / ORIENTATION
   ↓
YOLO CROP
   ↓
OPENCLIP GLOBAL IDENTITY
   ↓
PERCEPTUAL HASH
   ↓
ORB LOCAL STRUCTURE
   ↓
LOCATION
   ↓
COMPOSITE MATCH SCORE
```

That is a useful improvement over a one-vector matcher.

However, the implementation still treats same-object matching too much as a fixed weighted similarity calculation and lacks the broader evidence and trust architecture Dzoka requires.

Dzoka therefore takes the visual idea and evolves it into:

```text
OBJECT DETECTION
      ↓
MULTIPLE OBJECT VIEWS
      ↓
GLOBAL VISUAL FEATURES
      ↓
LOCAL PATCH FEATURES
      ↓
DISTINCTIVE REGIONS
      ↓
PERCEPTUAL HASHES
      ↓
OCR / ATTRIBUTES
      ↓
LOCATION / TIME
      ↓
HARD CONFLICTS
      ↓
EVIDENCE COVERAGE
      ↓
CALIBRATED MATCH CONFIDENCE
      ↓
SEPARATE OWNERSHIP VERIFICATION
```

---

# 128. Final Decision

```text
PRIMARY CLASSIFICATION:
REFERENCE
```

because:

```text
README claims MIT
BUT
LICENSE file is absent
```

at the audited revision.

Secondary decision:

```text
ADAPT CONCEPTS
```

### Strong concepts to carry into Dzoka

```text
YOLO-assisted object isolation
OpenCLIP visual semantics
perceptual image hashes
local feature evidence
EXIF provenance
multi-signal visual reasoning
multiple image capture
mutual user acknowledgement
delayed contact disclosure
```

### Concepts to redesign

```text
candidate filtering
multi-view comparison
visual score fusion
location/time modelling
missing evidence
confidence
privacy communication
recovery confirmation
```

### Do not copy

```text
source implementation
```

until the repository's licensing is actually clarified.

The guiding conclusion is:

> **FuricBond/Lost-And-Found is useful evidence that lost-property matching benefits from combining global vision, local visual structure, perceptual similarity, and context—but Dzoka must implement those ideas inside a stronger evidence, licensing, verification, and recovery architecture.**