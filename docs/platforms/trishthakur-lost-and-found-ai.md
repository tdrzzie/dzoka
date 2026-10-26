# Source Audit — trishthakur/lost-and-found-ai

> **Dzoka — The Open Recovery Network**
>
> Source audit of `trishthakur/lost-and-found-ai`, an educational Streamlit prototype using OpenAI CLIP through Hugging Face Transformers for text-to-image and image-to-image lost-item retrieval.
>
> The audit focuses on actual implementation rather than README claims, with particular attention to CLIP matching semantics, embedding storage, unsafe deserialization, authentication, authorization, privacy, recovery workflow, data integrity, model provenance, deployment readiness, and relevance to Dzoka.

---

# 1. Audit Metadata

| Field | Value |
|---|---|
| Repository | `https://github.com/trishthakur/lost-and-found-ai` |
| Owner | `trishthakur` |
| Repository | `lost-and-found-ai` |
| Audited branch | `main` |
| Exact audited revision | `8e5fcb6f79595c9d429679ba7868cac4a90e1319` |
| Short revision | `8e5fcb6` |
| Latest audited commit date | 2025-10-28 |
| Audit date | 2026-10-04 |
| Primary language | Python |
| UI framework | Streamlit |
| AI framework | Hugging Face Transformers |
| Model | `openai/clip-vit-base-patch32` |
| ML runtime | PyTorch |
| Metadata storage | CSV |
| Embedding storage | Serialized Python lists inside CSV |
| Image storage | Local filesystem |
| Location configuration | Excel |
| Authentication storage | CSV |
| Project license | **UNRESOLVED** |
| Source classification | **REJECT AS FOUNDATION** |
| Secondary value | **REFERENCE CLIP RETRIEVAL PROTOTYPE** |
| Direct code reuse | **NO** |
| Production suitability | **NO** |

---

# 2. Executive Decision

The repository demonstrates a valid high-level idea:

```text
image / text
    ↓
CLIP
    ↓
shared embedding space
    ↓
cosine similarity
    ↓
candidate item
```

That concept is relevant to Dzoka.

The implementation, however, must **not** become a Dzoka foundation.

The main reasons are:

```text
project license unresolved

unsafe eval() of persisted embedding data

weak unsalted SHA-256 password hashing

password hashes written to logs

credential database committed to repository

owner information committed to repository

authorization role defined but not enforced

single Streamlit process

CSV as transactional database

local filesystem media storage

no concurrency control

no claim model

no ownership verification

no recovery authorization

no custody model

no handover protocol

uncalibrated similarity threshold

one-result retrieval

cross-modal scoring contamination bug

no hard conflicts

no evidence coverage

no model provenance

no model revision pin

no tests

no CI
```

Therefore:

```text
PRIMARY CLASSIFICATION:
REJECT FOUNDATION
```

with:

```text
SECONDARY CLASSIFICATION:
REFERENCE CLIP RETRIEVAL CONCEPT
```

---

# 3. Source Reuse Decision

Dzoka should **not**:

```text
fork this application

copy its authentication implementation

copy its CSV persistence

copy its embedding serialization

copy its matching algorithm

copy its recovery workflow

copy its authorization model
```

Dzoka may use the repository as evidence that:

```text
CLIP-like models are useful for candidate generation
```

but Dzoka already has stronger sources for that capability:

```text
OpenCLIP
ILIAS
VisualMatch
Patchwise Retrieval
FuricBond/Lost-And-Found
```

---

# 4. Repository Structure

The audited root contains approximately:

```text
lost-and-found-ai/
├── README.md
├── lostnfound.py
├── requirements.txt
├── lostnfound.mp4
├── data/
│   ├── locations.xlsx
│   ├── lost_items.csv
│   ├── resolved_items.csv
│   └── users.csv
└── lost_items/
    └── image files
```

---

# 5. Repository Architecture

The application is effectively a:

```text
single-file Streamlit monolith
```

Most behavior lives in:

```text
lostnfound.py
```

including:

```text
model loading
embedding generation
matching
authentication
authorization state
report creation
search
case resolution
file storage
CSV mutation
UI rendering
```

---

# 6. High-Level Runtime

```text
Browser
   ↓
Streamlit
   ↓
lostnfound.py
   ├── Hugging Face CLIP
   ├── CSV files
   ├── local image files
   └── Excel location configuration
```

There is no separate:

```text
API
domain layer
application layer
matching service
AI service
database service
object-storage service
identity provider
```

---

# 7. README Positioning

The README describes the project as:

```text
Automated Lost-Item Retrieval System
```

and states that it provides:

```text
multimodal search

text matching

image matching

CLIP embeddings

staff portal

location management

contact lookup
```

These broad capabilities exist.

However, their implementation maturity is much lower than the names may imply.

---

# 8. README Production Disclaimer

The README explicitly states that the project is:

```text
a prototype developed for educational purposes
```

and recommends stronger security/privacy controls before production.

That disclaimer is appropriate.

The implementation should therefore be evaluated as:

```text
educational proof of concept
```

rather than a production-ready recovery platform.

---

# 9. Project License

No root:

```text
LICENSE
```

file was found at the audited revision.

No verified project license was established.

Therefore:

```text
license = UNRESOLVED
```

---

# 10. Licensing Consequence

Public GitHub visibility is not equivalent to an open-source license.

Dzoka should therefore assume:

```text
directCodeReuseAllowed = false
```

until licensing is clarified.

---

# 11. Bundled Data Licensing

The repository also contains:

```text
item images

location data

lost-item data

resolved-item data

user credential data
```

No clear dataset/media license was found.

Therefore Dzoka must not assume these assets are reusable either.

---

# 12. AI Model Licensing

The application downloads:

```text
openai/clip-vit-base-patch32
```

through Hugging Face Transformers.

The software repository license and model-weight license are separate concerns.

Dzoka must independently audit:

```text
model implementation license

weight license

redistribution terms

commercial use

model revision
```

before shipping model artifacts.

---

# 13. Primary Application File

The core implementation is:

```text
lostnfound.py
```

at approximately:

```text
12.8 KB
```

in the audited revision.

---

# 14. Application Startup

At startup the application:

```text
loads CLIP

selects CUDA if available

creates local directories

creates missing CSV files

loads locations.xlsx

loads users.csv

prints credential-file contents

renders Streamlit UI
```

---

# 15. Model Loading

The application loads:

```python
CLIPModel.from_pretrained("openai/clip-vit-base-patch32")
```

and:

```python
CLIPProcessor.from_pretrained("openai/clip-vit-base-patch32")
```

---

# 16. Positive Pattern — Model Cached

Streamlit:

```text
@st.cache_resource
```

is used so the model is not repeatedly loaded on every UI interaction.

For a prototype, this is sensible.

---

# 17. Device Selection

The application automatically selects:

```text
CUDA
```

when available and otherwise uses:

```text
CPU
```

This is a reasonable prototype deployment pattern.

---

# 18. Model Revision Is Not Pinned

The model is requested only by name:

```text
openai/clip-vit-base-patch32
```

with no explicit:

```text
revision
commit hash
artifact digest
```

---

# 19. Dzoka Model Governance Requirement

Dzoka should record:

```text
provider
repository
model name
exact revision
weight digest
processor version
embedding dimension
normalization
license
```

inside a:

```text
ModelManifest
```

or equivalent.

---

# 20. ModelRun

Every production Dzoka AI output should reference:

```text
ModelRun
```

The audited project has no equivalent.

---

# 21. Embedding Dimension

The README identifies the CLIP embedding dimension as:

```text
512
```

for the selected model.

---

# 22. Embedding Creation

When a report is created, the application generates:

```text
image_embedding
```

from the uploaded/captured image.

---

# 23. Stored Representation

The embedding is converted using roughly:

```text
tensor
→ Python list
→ str(...)
→ CSV cell
```

---

# 24. Example Conceptual Storage

```text
Embedding =
"[[0.012, -0.338, ... ]]"
```

---

# 25. Why This Storage Is Weak

CSV string serialization introduces:

```text
large records
slow parsing
no vector indexes
no dimensional constraints
no model compatibility enforcement
no database-level vector operations
```

---

# 26. Unsafe Embedding Deserialization

The most serious implementation finding in the matching path is:

```python
eval(row['Embedding'])
```

---

# 27. `eval()` Risk

`eval()` interprets the stored field as executable Python syntax.

If embedding storage is modified maliciously, a value could become:

```text
executable Python expression
```

instead of merely numeric data.

---

# 28. Security Classification

This is an:

```text
UNSAFE DESERIALIZATION / CODE EXECUTION PATTERN
```

---

# 29. Current Exploitability Qualification

The normal UI generates embedding strings from tensors rather than accepting arbitrary embedding strings directly.

Therefore the current UI does not obviously provide a direct user field for injecting Python into the embedding cell.

However:

```text
CSV modification
malicious import
compromised filesystem
administrative manipulation
future ingestion feature
```

could convert the unsafe construct into code execution.

The pattern should be rejected regardless.

---

# 30. Dzoka Rule

Never use:

```text
eval()
pickle from untrusted input
unsafe arbitrary object deserialization
```

for vector persistence.

---

# 31. Safe Representation

Dzoka should store vectors using:

```text
pgvector

typed binary arrays

Qdrant

safe structured serialization
```

rather than executable text.

---

# 32. Query Modalities

The UI allows users to search using:

```text
Text Description
```

or:

```text
Image
```

---

# 33. CLIP Cross-Modal Value

This is a legitimate strength.

CLIP places:

```text
text
and
images
```

into a shared semantic embedding space.

Therefore a phrase such as:

```text
black backpack
```

can retrieve visually similar images.

This capability is useful for Dzoka candidate generation.

---

# 34. Candidate Generation Role

Dzoka should use global CLIP-style similarity for:

```text
candidate retrieval
```

not for:

```text
ownership
```

and not for:

```text
same-instance proof
```

---

# 35. Critical Query-Embedding Bug

The implementation function:

```text
encode_text_and_image(...)
```

generates **both** a text embedding and image embedding even when only one modality was supplied.

---

# 36. Text-Only Search Behavior

For a text-only query:

```text
text = actual user description
image = synthetic blank 224×224 image
```

The function then generates:

```text
text embedding of actual query
+
image embedding of blank image
```

---

# 37. Image-Only Search Behavior

For an image-only query:

```text
image = actual uploaded image
text = ""
```

It then generates:

```text
image embedding of actual image
+
text embedding of empty string
```

---

# 38. Matching Function Uses Both

The matching function adds:

```text
cosine(text_embedding, stored_image_embedding)
```

and:

```text
cosine(image_embedding, stored_image_embedding)
```

whenever both returned values are non-null.

They always are under normal execution.

---

# 39. Consequence — Text Search

A text query therefore scores:

```text
semantic text similarity
+
similarity to synthetic blank image
```

---

# 40. Consequence — Image Search

An image query scores:

```text
visual image similarity
+
similarity to empty text
```

---

# 41. Why This Is Wrong

A modality that the user did not supply should contribute:

```text
NO EVIDENCE
```

not artificial evidence derived from:

```text
blank image
empty string
```

---

# 42. Dzoka Invariant Reinforced

This source reinforces one of Dzoka's most important rules:

> **Missing evidence must remain missing.**

Correct:

```text
imageFeature.available = false
```

not:

```text
imageFeature = embedding(blank image)
```

---

# 43. Correct Query Design

Text-only:

```text
TextEmbedding(query)
```

Image-only:

```text
ImageEmbedding(image)
```

Text + image:

```text
explicit multimodal fusion
```

with intentional weighting.

---

# 44. Stored Embeddings

Reported items store only:

```text
image embeddings
```

rather than distinct:

```text
image embeddings
text embeddings
attribute embeddings
```

---

# 45. Text Search

Text search compares:

```text
CLIP text embedding
```

against:

```text
stored CLIP image embedding
```

This is a valid CLIP cross-modal operation.

The problem is the extra blank-image contribution.

---

# 46. Image Search

Image search compares:

```text
query image embedding
```

against:

```text
stored image embedding
```

which is also valid.

Again, the empty-text contribution contaminates the score.

---

# 47. Score Accumulation

The system simply adds similarities.

Conceptually:

```text
score =
text_similarity
+
image_similarity
```

---

# 48. No Weighting

There is no explicit:

```text
modality weighting
reliability weighting
coverage adjustment
```

---

# 49. No Evidence Dependency

The system does not model whether multiple features originate from the same source.

---

# 50. No Calibration

The README uses a hard-coded threshold:

```text
0.1
```

---

# 51. Threshold Meaning

There is no empirical evidence in the repository showing that:

```text
score >= 0.1
```

means:

```text
same physical item
```

or any specific probability.

---

# 52. No Calibration Dataset

The repository does not include documented:

```text
positive pairs
hard negatives
calibration split
ROC analysis
precision-recall analysis
```

supporting the threshold.

---

# 53. Score Range Problem

Because multiple similarities may be added, score semantics depend on which query modalities were used.

Therefore the threshold has no stable universal probabilistic meaning.

---

# 54. Dzoka Correction

Maintain:

```text
retrievalScore
featureSimilarity
matchScore
evidenceCoverage
calibratedConfidence
```

as separate concepts.

---

# 55. One Best Match

The system returns only:

```text
best_match
```

and:

```text
best_score
```

---

# 56. No Top-K Candidate Set

It does not preserve:

```text
candidate 1
candidate 2
candidate 3
...
```

---

# 57. Why Top-K Matters

Lost-property matching is uncertain.

The correct physical item may be:

```text
candidate #3
```

even when a semantically similar different object ranks first.

---

# 58. Dzoka Candidate Retrieval

Use:

```text
Recall@K-oriented retrieval
```

followed by:

```text
reranking
conflict detection
verification
```

---

# 59. No Local Instance Matching

The project uses only global CLIP embeddings.

It does not analyze:

```text
scratches
stickers
wear
damage
engraving
local patches
keypoints
```

---

# 60. Same Product vs Same Instance

A global embedding is useful for distinguishing:

```text
backpack
vs
phone
```

or:

```text
black backpack
vs
red shoes
```

It is far weaker at distinguishing:

```text
black backpack A
vs
black backpack B
```

when the products are visually similar.

---

# 61. Dzoka Implication

Global CLIP should be:

```text
RETRIEVAL EVIDENCE
```

not:

```text
PHYSICAL IDENTITY
```

---

# 62. No Hard Conflicts

The system does not consider identifiers such as:

```text
serial number
IMEI
asset tag
engraving
```

---

# 63. Consequence

A visually similar object can win even if a strong identifier proves it is different.

---

# 64. Dzoka Conflict Engine

Dzoka requires:

```text
ConflictEngine
```

where strong contradictions may override similarity.

---

# 65. No Structured Attributes

The project does not model:

```text
brand
model
colour
material
size
condition
distinctive marks
```

as structured match features.

---

# 66. No OCR

The application does not extract text from:

```text
labels
stickers
serial numbers
documents
```

---

# 67. No Geospatial Matching

Location is represented as:

```text
Campus - Building
```

but does not affect similarity ranking.

---

# 68. No Temporal Matching

Report timestamp is stored but not used in matching.

---

# 69. No Movement Context

There is no:

```text
route
transition
likely next location
```

model.

---

# 70. No Evidence Coverage

A candidate receives one score regardless of how much evidence exists.

---

# 71. No Explanation

Users are not shown:

```text
why the candidate matched
which features agreed
which features disagreed
```

---

# 72. Match Result UX

If the best score passes the threshold, the user sees approximately:

```text
You can collect the item at: <location>
```

and a location contact email.

---

# 73. Critical Trust Problem

This jumps directly from:

```text
CLIP similarity
```

to:

```text
collection instruction
```

---

# 74. Architectural Violation

For Dzoka:

```text
Match ≠ Ownership
```

and:

```text
Match ≠ RecoveryAuthorization
```

---

# 75. Safer Result Language

Dzoka should say something like:

```text
A possible match was found.
Ownership has not yet been verified.
```

not:

```text
You can collect the item.
```

---

# 76. No Claim

The application does not create a:

```text
Claim
```

when someone believes a candidate is theirs.

---

# 77. No Ownership Verification

There is no:

```text
private challenge
receipt proof
prior photo comparison
serial verification
entitlement check
```

---

# 78. No Risk Assessment

There is no:

```text
claim fraud detection
claim history
attempt tracking
risk score
```

---

# 79. No Competing Claims

The system cannot formally handle:

```text
two people claiming the same object
```

---

# 80. No Recovery Authorization

There is no separate decision answering:

```text
Is this claimant authorized to receive the item?
```

---

# 81. No Custody Model

The application stores a location but not:

```text
physical custodian
custody history
transfer events
holds
```

---

# 82. No Handover Protocol

There is no:

```text
handover credential
OTP
QR
two-sided confirmation
recipient authentication
```

---

# 83. Case Resolution

Authenticated staff can enter:

```text
owner_details
```

and click:

```text
Resolve
```

---

# 84. Resolution Effect

The application:

```text
copies item from lost_items.csv
to resolved_items.csv
```

and removes it from the active CSV.

---

# 85. Resolution Is Not Verification

The system accepts arbitrary free-text:

```text
Owner Details
```

before resolution.

No evidence is required.

---

# 86. Resolution Is Not Custody Transfer

The system does not prove:

```text
physical item was actually handed over
```

---

# 87. Status Mutation

On resolution:

```text
Status = Found
```

is written.

---

# 88. Semantic Ambiguity

The application begins with:

```text
Status = Lost
```

then after resolution sets:

```text
Status = Found
```

This mixes:

```text
item state
case state
recovery outcome
```

into one field.

---

# 89. README / UI Semantic Drift

The README says the system allows:

```text
staff to report found items
```

and:

```text
users to search for lost belongings
```

---

# 90. Implemented UI

The ingest screen is labelled:

```text
Report a Lost Item
```

---

# 91. Stored State

The newly created record is stored with:

```text
Status = Lost
```

---

# 92. Architectural Ambiguity

It is therefore unclear whether the database represents:

```text
lost items
```

or:

```text
found objects available for claimant search
```

---

# 93. Why This Matters

Domain terminology must be precise.

Dzoka explicitly distinguishes:

```text
LostReport
FoundReport
ItemRecord
MatchCandidate
Claim
Recovery
```

---

# 94. No Report Type Separation

The repository does not cleanly distinguish:

```text
lost observation
```

from:

```text
found observation
```

---

# 95. Dzoka Improvement

Use separate semantic commands:

```text
ReportLostItem
ReportFoundItem
```

while linking both to:

```text
ItemRecord
```

only through explicit identity-resolution logic.

---

# 96. Authentication Storage

Staff identities are stored in:

```text
data/users.csv
```

---

# 97. Credential Repository Exposure

The audited repository commits:

```text
users.csv
```

including at least one password hash.

The actual credential values should not be reproduced in documentation.

---

# 98. Password Hashing

Passwords are hashed using:

```text
SHA-256(password)
```

---

# 99. No Salt

The implementation does not use:

```text
unique password salt
```

---

# 100. No Password KDF

It does not use a password-hardening algorithm such as:

```text
Argon2
bcrypt
scrypt
PBKDF2
```

---

# 101. Security Consequence

Fast unsalted SHA-256 makes offline password guessing substantially easier than a dedicated password KDF.

This matters even more because the hash database is committed publicly.

---

# 102. Critical Logging Finding

During login the application prints:

```text
stored password hash
```

and:

```text
input password hash
```

to the application logs.

---

# 103. Startup Credential Logging

A function called:

```text
print_users_file()
```

prints the contents of the user credential file at startup.

---

# 104. Security Classification

This is an unacceptable production authentication pattern.

---

# 105. Dzoka Rule

Never log:

```text
passwords
password hashes
authentication secrets
OTP values
handover secrets
```

---

# 106. Role Field

The credential CSV contains:

```text
Is_Admin
```

---

# 107. Admin Function

The code defines:

```text
is_admin(username)
```

---

# 108. Authorization Finding

The actual case-resolution workflow does **not** call:

```text
is_admin(...)
```

before showing resolution functionality.

---

# 109. Effective Authorization

Any credential record that successfully passes:

```text
check_password(...)
```

becomes able to access the resolution screen.

---

# 110. Dead Authorization Logic

The existence of:

```text
Is_Admin
```

does not create an authorization boundary if the value is never enforced.

---

# 111. Dzoka Lesson

Authorization must be:

```text
server-enforced
resource-aware
testable
```

not merely represented as an unused field.

---

# 112. Authentication Session

Login state is stored in:

```text
st.session_state.logged_in
```

---

# 113. Prototype Suitability

This is adequate for a simple Streamlit prototype.

It is not a production identity architecture.

---

# 114. No MFA

No:

```text
MFA
step-up authentication
```

exists.

---

# 115. No Brute-Force Protection

There is no visible:

```text
rate limiting
account lockout
progressive delay
```

for login attempts.

---

# 116. No Password Reset

There is no account recovery lifecycle.

---

# 117. No Credential Rotation

No mechanism is present for managed credential lifecycle.

---

# 118. Dzoka Identity Architecture

Use an:

```text
OIDC-compatible identity provider
```

rather than implementing passwords in application CSV files.

---

# 119. Data Storage

Canonical item data is stored in:

```text
CSV files
```

---

# 120. Active Cases

Active records live in:

```text
data/lost_items.csv
```

---

# 121. Resolved Cases

Resolved records live in:

```text
data/resolved_items.csv
```

---

# 122. Authentication

Accounts live in:

```text
data/users.csv
```

---

# 123. Location Configuration

Locations live in:

```text
data/locations.xlsx
```

---

# 124. Image Storage

Images live under:

```text
lost_items/
```

on the local filesystem.

---

# 125. CSV Is Not a Transactional Database

The application repeatedly performs:

```text
read entire CSV
modify DataFrame
rewrite entire CSV
```

---

# 126. Concurrency Risk

If two Streamlit sessions modify the same CSV concurrently:

```text
both read old file
both modify
one writes
other writes
```

resulting in possible:

```text
lost updates
```

---

# 127. No Row Locking

There is no:

```text
locking
optimistic version
transaction
```

around case changes.

---

# 128. Resolution Consistency Risk

Resolution consists of separate operations:

```text
write item to resolved_items.csv
```

then:

```text
remove item from lost_items.csv
```

---

# 129. Partial Failure

If the process fails between those writes, the same item may remain:

```text
active
+
resolved
```

simultaneously.

---

# 130. No Atomic Transaction

The operation is not atomic.

---

# 131. Dzoka Requirement

A recovery state transition should happen inside a database transaction.

For example:

```text
validate state
update state
append event
write outbox
COMMIT
```

---

# 132. File-Based Embedding Index

Every search loads:

```text
lost_items.csv
```

and iterates through records.

---

# 133. Complexity

Candidate search is approximately:

```text
O(number of items)
```

per query.

---

# 134. No Vector Index

There is no:

```text
HNSW
IVFFlat
FAISS production index
pgvector index
Qdrant
```

in the implemented runtime.

---

# 135. README FAISS Section

The README proposes FAISS as:

```text
production scaling guidance
```

---

# 136. Important Distinction

FAISS is:

```text
DOCUMENTED AS POSSIBLE FUTURE SCALING
```

but is not part of the actual audited application.

---

# 137. Dzoka Vector Default

Dzoka's stronger architecture remains:

```text
PostgreSQL
+
pgvector
```

with optional:

```text
Qdrant
```

at scale.

---

# 138. Images Committed to Git

The repository includes many images under:

```text
lost_items/
```

---

# 139. Data Provenance Concern

Some image filenames appear to contain:

```text
dataset-style identifiers
personal-looking names
```

No clear provenance or licensing file for the media was identified.

---

# 140. Dzoka Rule

User-submitted recovery media should not normally be committed into:

```text
Git history
```

---

# 141. Object Storage

Use:

```text
S3 / MinIO
```

or equivalent.

---

# 142. Public Repository Contains Resolved Records

The audited repository includes:

```text
resolved_items.csv
```

with populated:

```text
Owner Details
```

records.

---

# 143. Privacy Concern

Some committed owner-detail fields contain personal-looking information.

Those values are intentionally not reproduced here.

---

# 144. Severity

Committing recovery records containing personal data into a public source repository is a serious privacy anti-pattern.

---

# 145. Git History Problem

Removing the file in a future commit does not necessarily remove the data from prior Git history.

---

# 146. Dzoka Rule

Production/test datasets containing real personal information must never be casually committed into the public source repository.

---

# 147. Data Separation

Dzoka should separate:

```text
source code

synthetic test fixtures

production data

evaluation datasets

private recovery evidence
```

---

# 148. Synthetic Fixtures

Public repositories should use:

```text
clearly synthetic data
```

for examples and tests.

---

# 149. Owner Details Field

The schema contains:

```text
Owner Details
```

as free text.

---

# 150. Data Classification Problem

This single field could contain:

```text
name
phone
email
student ID
address
other PII
```

without schema constraints.

---

# 151. Dzoka Replacement

Use typed entities and classified evidence.

For example:

```text
Claimant
ContactPoint
IdentityEvidence
EntitlementEvidence
```

rather than one unrestricted string.

---

# 152. No Data Classification

There is no distinction among:

```text
PUBLIC
RESTRICTED
SECRET
```

data.

---

# 153. No Retention Policy

The repository contains no automated lifecycle for:

```text
old reports
resolved records
images
credentials
owner data
embeddings
```

---

# 154. No Legal Hold

There is no concept of:

```text
retention override
legal hold
```

---

# 155. No Data Deletion Propagation

Deleting a record does not account for:

```text
image
embedding
Git history
logs
derived data
```

---

# 156. Location Model

The system uses an Excel sheet containing:

```text
Campus
Building
Contact Email
```

---

# 157. Positive Pattern — Hierarchical Location

Even though simple, the design recognizes:

```text
Campus
    ↓
Building
```

rather than one completely unstructured location string.

---

# 158. Dzoka Adaptation

Dzoka should generalize:

```text
Organization
└── Site
    └── Building
        └── Floor
            └── Zone
```

---

# 159. Location Contact Mapping

The system maps:

```text
location
→ contact email
```

---

# 160. Useful Institutional Concept

A recovery location may have its own:

```text
recovery desk
contact point
custodian
```

This concept is worth retaining.

---

# 161. But Contact Is Not Custody

A building email is not sufficient to model:

```text
who actually possesses the item
```

---

# 162. Dzoka Custodian

Use explicit:

```text
Custodian
CustodyLocation
CustodyRecord
```

---

# 163. Report Image Requirement

The reporting UI requires an image.

---

# 164. Product Limitation

Users who:

```text
have no prior image
have low bandwidth
use accessibility tooling
```

cannot create a report through this path.

---

# 165. Dzoka Policy

Images should strongly improve evidence coverage but should not universally be mandatory.

---

# 166. Webcam Capture

The project supports:

```text
camera capture
```

through Streamlit.

This is a good prototype UX concept.

---

# 167. Dzoka Reuse

Retain:

```text
camera capture
upload
multi-view capture
```

as channel features.

---

# 168. Filename Generation

Uploaded images use:

```text
timestamp + original filename
```

for storage.

---

# 169. Filename Problems

This can expose:

```text
original filenames
names
device-generated metadata
```

and does not guarantee globally unique object identity.

---

# 170. Dzoka Media Identity

Generate:

```text
opaque mediaId
```

separately from:

```text
originalFilename
```

---

# 171. Content Hashing

The repository does not calculate an image content hash.

---

# 172. Dzoka Benefit

Content hashes help:

```text
deduplication
lineage
integrity
AI input tracking
```

---

# 173. File Validation

The Streamlit uploader filters extensions:

```text
png
jpg
jpeg
```

---

# 174. Extension Filtering Is Not Full Validation

Production media processing should also validate:

```text
magic bytes
decode success
size
dimensions
metadata
malware
```

---

# 175. EXIF Privacy

No explicit EXIF metadata handling is present.

---

# 176. Dzoka Privacy

Uploaded images may contain:

```text
GPS
capture timestamps
device information
```

which require explicit policy.

---

# 177. No Image Derivatives

The same local image acts as:

```text
original
display image
matching input
```

---

# 178. Dzoka Media Architecture

Separate:

```text
ORIGINAL
SANITIZED
THUMBNAIL
AI_INPUT
OCR_INPUT
PUBLIC
```

derivatives with lineage.

---

# 179. Requirements File

The repository contains a large:

```text
requirements.txt
```

with exact versions.

---

# 180. Positive Aspect

Using:

```text
==
```

versions is more deterministic than unconstrained dependencies.

---

# 181. Dependency Bloat

The file contains many libraries not visibly required by `lostnfound.py`.

Examples include classes of dependencies for:

```text
web frameworks
Google Cloud
database access
plotting
TensorFlow-related tooling
Streamlit extensions
```

that are not central to the audited runtime.

---

# 182. Risk

Unnecessary dependencies increase:

```text
installation size
build time
dependency conflicts
security surface
license surface
```

---

# 183. Dzoka Dependency Rule

Each runtime image should include only dependencies required by that workload.

---

# 184. Python Version Claim

The README says:

```text
Python 3.8+
```

while the requirements file pins a modern dependency set.

That compatibility claim should be verified automatically rather than assumed.

---

# 185. Dzoka Runtime Pinning

Specify:

```text
exact supported Python versions
```

through:

```text
pyproject
container image
CI matrix
```

---

# 186. No Container

No root:

```text
Dockerfile
```

was present in the audited repository.

---

# 187. README Cloud Build Guidance

The README describes a GCP/Cloud Run production direction.

The actual repository does not include a complete deployment artifact implementing that architecture.

---

# 188. Local Filesystem Problem in Cloud Deployment

The current runtime writes:

```text
images
CSV state
```

to local disk.

Serverless/container filesystems may be:

```text
ephemeral
non-shared
```

across replicas.

---

# 189. Production Scaling Mismatch

Multiple replicas would each have different:

```text
local CSV
local images
```

unless storage was externalized.

---

# 190. Dzoka Rule

Authoritative production state must not depend on local application-container filesystems.

---

# 191. No Database Migrations

There is no migration system.

---

# 192. Schema Evolution

Changing a CSV schema requires ad hoc code/data handling.

---

# 193. Dzoka Requirement

Use versioned migrations for:

```text
PostgreSQL schema
event schemas
configuration
```

---

# 194. No Event Architecture

All processing occurs synchronously inside the Streamlit application.

---

# 195. No Asynchronous Enrichment

There is no:

```text
media uploaded
    ↓
background embedding worker
```

pipeline.

---

# 196. Model Latency

CLIP processing occurs directly during user interaction.

For a small prototype this is acceptable.

At scale, Dzoka should use asynchronous workers for heavy enrichment.

---

# 197. No Rematching

The project does not automatically re-run unresolved cases when:

```text
new item added

model changes

new evidence added
```

---

# 198. Dzoka Event Flow

```text
ReportCreated
    ↓
FingerprintBuildRequested
    ↓
FingerprintReady
    ↓
MatchingRequested
    ↓
MatchCandidateCreated
```

---

# 199. No Model Migration

Changing CLIP model requires regenerating stored embeddings manually.

---

# 200. No Embedding Version

The CSV does not record:

```text
embedding model
revision
dimension
normalization
```

per vector.

---

# 201. Dangerous Model Upgrade Scenario

Suppose embeddings were originally generated by:

```text
model A
```

and the application later loads:

```text
model B
```

The code has no compatibility guard preventing vectors from being compared.

---

# 202. Dzoka Invariant

Only compatible vectors may be compared.

Compatibility includes:

```text
model
revision
dimension
embedding space
normalization
```

---

# 203. No Feature Provenance

There is no metadata explaining:

```text
which image
which model
which preprocessing
```

generated an embedding.

---

# 204. No AI Failure State

`encode_text_and_image()` catches errors and returns:

```text
None, None
```

while showing a Streamlit error.

---

# 205. Reporting Behavior

If embedding generation fails during reporting:

```text
image_embedding_str = None
```

may still be written to the CSV.

---

# 206. Partial Data

The system has no explicit:

```text
FingerprintStatus
```

to indicate whether AI enrichment succeeded.

---

# 207. Dzoka Improvement

Use:

```text
PARTIAL
READY
DEGRADED
FAILED
```

fingerprint states.

---

# 208. No Model Metrics

There is no telemetry for:

```text
inference latency
failure rate
GPU utilization
embedding throughput
```

---

# 209. No Evaluation Suite

The repository does not include automated evaluation measuring:

```text
Recall@K
MRR
precision
false match rate
```

---

# 210. No Hard Negatives

There is no dedicated evaluation for:

```text
same product model
different physical instance
```

---

# 211. Why This Matters Most to Dzoka

A lost-property system can appear impressive when tested with:

```text
phone
vs
backpack
vs
keys
```

but fail badly when tested with:

```text
black iPhone A
vs
black iPhone B
```

---

# 212. Dzoka Evaluation Requirement

Same-instance testing must include:

```text
visually near-identical negatives
```

---

# 213. No Confidence Calibration

The number returned by cosine similarity is treated directly as decision evidence.

---

# 214. Dzoka Principle

Similarity is not probability.

---

# 215. No Category-Specific Threshold

All objects share the same threshold.

---

# 216. Problem

Matching requirements for:

```text
keys
passport
laptop
bag
charger
```

are materially different.

---

# 217. Dzoka Profiles

Use:

```text
category-specific matching profiles
```

where evidence supports them.

---

# 218. No Environment-Specific Matching

Campus/building context is not incorporated into score.

---

# 219. Dzoka Improvement

Environment can influence:

```text
location relevance
time relevance
movement expectations
```

without becoming a destructive hard filter.

---

# 220. No Search Explanation

A user sees the match but not:

```text
image similarity
text similarity
location agreement
time agreement
```

---

# 221. Dzoka Explainability

Candidate explanation should show safe evidence such as:

```text
visual appearance consistent
category consistent
location plausible
time plausible
```

without revealing private verification data.

---

# 222. Authentication Claim in README

The README describes:

```text
Staff Portal: Secure authentication
```

---

# 223. Audit Assessment

Authentication exists.

However, calling it:

```text
secure
```

is not supported by the implementation because of:

```text
unsalted SHA-256

public credential hashes

hash logging

credential-file logging

no rate limiting

unused admin authorization
```

---

# 224. Classification

Therefore:

```text
STAFF AUTHENTICATION:
IMPLEMENTED
```

but:

```text
PRODUCTION-SECURE AUTHENTICATION:
NOT IMPLEMENTED
```

---

# 225. Multimodal Search Claim

```text
IMPLEMENTED
```

but with important algorithmic defects.

---

# 226. Text Search

```text
IMPLEMENTED
```

through CLIP cross-modal similarity.

---

# 227. Image Search

```text
IMPLEMENTED
```

through CLIP visual similarity.

---

# 228. Multimodal Fusion

```text
PARTIAL / INCORRECT
```

because missing modalities are replaced by synthetic blank inputs.

---

# 229. Embedding-Based Matching

```text
IMPLEMENTED
```

but:

```text
linear CSV scan
unsafe eval
uncalibrated threshold
single candidate
```

---

# 230. Location Management

```text
IMPLEMENTED
```

as:

```text
Campus → Building → Contact Email
```

---

# 231. Location Matching

```text
NOT IMPLEMENTED
```

---

# 232. Staff Case Resolution

```text
IMPLEMENTED
```

as a basic administrative operation.

---

# 233. Ownership Verification

```text
NOT IMPLEMENTED
```

---

# 234. Claims

```text
NOT IMPLEMENTED
```

---

# 235. Competing Claims

```text
NOT IMPLEMENTED
```

---

# 236. Fraud Detection

```text
NOT IMPLEMENTED
```

---

# 237. Recovery Authorization

```text
NOT IMPLEMENTED
```

---

# 238. Custody

```text
NOT IMPLEMENTED
```

---

# 239. Secure Handover

```text
NOT IMPLEMENTED
```

---

# 240. Audit Trail

```text
NOT IMPLEMENTED
```

---

# 241. Federation

```text
NOT IMPLEMENTED
```

---

# 242. Notifications

```text
NOT IMPLEMENTED
```

---

# 243. Traceability

```text
NOT IMPLEMENTED
```

---

# 244. Tests

```text
NOT FOUND
```

---

# 245. CI/CD

```text
NOT FOUND
```

---

# 246. Production Observability

```text
NOT FOUND
```

---

# 247. What This Repository Does Well

For a small educational prototype, the repository demonstrates:

```text
CLIP image embeddings

text-to-image retrieval

image-to-image retrieval

camera capture

image upload

simple institutional locations

simple staff interface

model GPU/CPU selection
```

---

# 248. Most Valuable Dzoka Lesson

The strongest reusable concept is:

```text
TEXT
   ┐
   ├──► SHARED VISUAL-SEMANTIC SPACE
IMAGE
   ┘
```

for:

```text
candidate generation
```

---

# 249. But Dzoka Already Has Better Foundation

Instead of reusing this source, Dzoka should use:

```text
OpenCLIP
```

directly through:

```text
ImageEmbeddingProvider
```

and potentially:

```text
TextEmbeddingProvider
```

---

# 250. Reusable Product Concept — Image Search

Users should be able to submit:

```text
a previous photo of their lost object
```

to improve candidate retrieval.

---

# 251. Reusable Product Concept — Text Search

Users should also be able to search with:

```text
natural-language description
```

without requiring a photo.

---

# 252. Reusable Product Concept — Camera Capture

Direct camera input is useful for:

```text
finder reporting
kiosk reporting
mobile/PWA
```

---

# 253. Reusable Product Concept — Institutional Pickup Location

An institution can associate items with:

```text
recovery points
```

and contact details.

Dzoka should formalize this through:

```text
RecoveryPoint
```

rather than spreadsheets.

---

# 254. Rejected Pattern — `eval()` Persistence

```text
serialized data
    ↓
eval()
```

must be permanently rejected.

---

# 255. Rejected Pattern — SHA-256 Passwords

```text
SHA256(password)
```

must not be used for Dzoka password storage.

---

# 256. Rejected Pattern — Password Hash Logging

Never log credential hashes.

---

# 257. Rejected Pattern — Credential Database in Git

Never commit live credential stores into the source repository.

---

# 258. Rejected Pattern — Real Recovery Records in Git

Never commit potentially real:

```text
owner data
recovery records
user media
```

to a public source repository.

---

# 259. Rejected Pattern — CSV as Authoritative Multi-User State

CSV is suitable for:

```text
small experiments
fixtures
exports
```

not Dzoka transactional production state.

---

# 260. Rejected Pattern — Synthetic Missing Modality

Do not convert:

```text
missing image
```

into:

```text
blank image embedding
```

---

# 261. Rejected Pattern — Arbitrary Similarity Threshold

Do not assume:

```text
cosine >= 0.1
```

means same physical object.

---

# 262. Rejected Pattern — Best Candidate Equals Match

The first-ranked candidate is still:

```text
a hypothesis
```

---

# 263. Rejected Pattern — Match Enables Collection

Never permit:

```text
AI similarity
→ collect item
```

---

# 264. Rejected Pattern — Free-Text Owner Proof

```text
owner_details = "..."
```

does not constitute ownership verification.

---

# 265. Rejected Pattern — Resolution by Data Movement

Moving a row from:

```text
active.csv
```

to:

```text
resolved.csv
```

is not sufficient recovery semantics.

---

# 266. Rejected Pattern — Local Filesystem Authoritative Media

Production deployments require durable object storage.

---

# 267. Rejected Pattern — Model Name Without Revision

Production AI models should be version-pinned.

---

# 268. Rejected Pattern — One Embedding per Item as Complete Identity

An item fingerprint requires more than one global vector.

---

# 269. Dzoka Mapping — Report

Upstream:

```text
image
+
campus
+
building
    ↓
CSV row
```

Dzoka:

```text
Report
    ↓
MediaAsset
    ↓
Observation
    ↓
ItemFingerprint
```

---

# 270. Dzoka Mapping — Fingerprint

Upstream:

```text
ItemFingerprint
≈
CLIP image vector
```

Dzoka:

```text
ItemFingerprint
├── structured attributes
├── semantic text
├── global visual vectors
├── local visual evidence
├── OCR
├── protected identifiers
├── time
├── location
├── provenance
├── quality
└── coverage
```

---

# 271. Dzoka Mapping — Search

Upstream:

```text
query
    ↓
CLIP
    ↓
scan every CSV row
    ↓
best candidate
```

Dzoka:

```text
query
    ↓
multiple retrievers
    ↓
candidate union
    ↓
rank fusion
    ↓
conflicts
    ↓
feature scoring
    ↓
reranking
    ↓
confidence
```

---

# 272. Dzoka Mapping — Vector Storage

Upstream:

```text
CSV string
```

Dzoka:

```text
VectorSearchProvider
    ├── pgvector
    └── optional Qdrant
```

---

# 273. Dzoka Mapping — CLIP

Upstream:

```text
transformers CLIP
```

Dzoka:

```text
ImageEmbeddingProvider
```

default candidate:

```text
OpenCLIP-compatible model
```

---

# 274. Dzoka Mapping — Missing Modality

Upstream:

```text
missing image
→ blank image
```

Dzoka:

```text
missing image
→ available = false
```

---

# 275. Dzoka Mapping — Match Result

Upstream:

```text
score >= threshold
    ↓
show pickup location
```

Dzoka:

```text
candidate
    ↓
claim
    ↓
verification
    ↓
risk
    ↓
policy
    ↓
recovery authorization
```

---

# 276. Dzoka Mapping — Staff Login

Upstream:

```text
users.csv
+
SHA-256
```

Dzoka:

```text
OIDC Identity Provider
```

---

# 277. Dzoka Mapping — Authorization

Upstream:

```text
logged_in == true
```

Dzoka:

```text
principal
+
tenant
+
resource
+
role
+
policy
```

---

# 278. Dzoka Mapping — Locations

Upstream:

```text
Campus
→ Building
```

Dzoka:

```text
Organization
→ Site
→ Building
→ Floor
→ Zone
→ RecoveryPoint
```

---

# 279. Dzoka Mapping — Resolution

Upstream:

```text
enter owner details
→ Resolve
```

Dzoka:

```text
Claim
→ Verification
→ RecoveryAuthorization
→ CustodyTransfer
→ RecoveryCompleted
```

---

# 280. Dzoka Mapping — Media

Upstream:

```text
local directory
```

Dzoka:

```text
S3 / MinIO
+
MediaAsset metadata
```

---

# 281. Dzoka Mapping — State

Upstream:

```text
CSV files
```

Dzoka:

```text
PostgreSQL
+
transactions
```

---

# 282. Dzoka Mapping — Matching Failure

Upstream errors may be surfaced through UI-level exceptions.

Dzoka requires explicit run state:

```text
MatchingRun
├── RUNNING
├── COMPLETED
├── FAILED
├── TIMED_OUT
└── CANCELLED
```

---

# 283. Dzoka Mapping — Model Failure

Use:

```text
ModelRun
```

rather than silently storing missing embeddings.

---

# 284. Source Influence on `item-fingerprint.md`

This audit reinforces that:

```text
one CLIP vector
≠
ItemFingerprint
```

---

# 285. Source Influence on `matching-engine.md`

It reinforces:

```text
global CLIP = candidate-generation signal
```

not final identity.

---

# 286. Source Influence on `ownership-verification.md`

The direct pickup path demonstrates why candidate discovery must be separated from ownership verification.

---

# 287. Source Influence on `claim-workflow.md`

The absence of Claims shows why a retrieval result must not directly initiate recovery.

---

# 288. Source Influence on `custody-state-machine.md`

A location string is not custody.

---

# 289. Source Influence on `rejected-patterns.md`

Ensure it includes:

```text
eval() on persisted data

unsalted SHA-256 passwords

credential hashes in logs

credentials committed to Git

real recovery PII in source repository

synthetic blank modalities

raw similarity treated as confidence

AI match directly exposing pickup instructions

CSV as authoritative concurrent state
```

---

# 290. Source Influence on `reusable-components.md`

Retain only high-level concepts:

```text
CLIP cross-modal candidate generation

text-to-image search

image-to-image search

camera capture

institutional location hierarchy
```

---

# 291. Source Influence on `architecture-inputs.md`

This source strongly supports:

```text
global embeddings are retrieval features,
not physical identity
```

---

# 292. Source Influence on `unresolved-gaps.md`

It reinforces the unresolved need for:

```text
same-instance recognition

multi-image fusion

local visual features

score calibration

hard-negative evaluation
```

---

# 293. Source Influence on Security Architecture

The credential implementation demonstrates why authentication should be delegated to:

```text
OIDC-compatible infrastructure
```

and not rebuilt casually inside application code.

---

# 294. Source Influence on Privacy Architecture

The committed resolved records demonstrate that:

```text
source repository
```

must never become a convenient storage location for production recovery data.

---

# 295. Source Influence on Model Governance

Model names alone are insufficient.

Dzoka must pin:

```text
model revision
processor
embedding schema
```

and preserve them with each derived vector.

---

# 296. Source Influence on Data Architecture

Embeddings must not be opaque executable strings inside business records.

---

# 297. Candidate ADR — CLIP Role

```text
ADR:
CLIP/OpenCLIP-style global embeddings are used for candidate retrieval,
not final same-instance identity determination.
```

---

# 298. Candidate ADR — Missing Modalities

```text
ADR:
Unavailable evidence is represented explicitly as unavailable and is never replaced with synthetic blank inputs for production scoring.
```

---

# 299. Candidate ADR — Vector Persistence

```text
ADR:
Production embeddings are stored through VectorSearchProvider using typed vector storage, never executable text serialization.
```

---

# 300. Candidate ADR — Authentication

```text
ADR:
Dzoka does not maintain production passwords in application CSV/database logic and delegates authentication to an OIDC-compatible identity provider.
```

---

# 301. Candidate ADR — Recovery Boundary

```text
ADR:
A retrieval or matching result cannot directly instruct the user to collect an item.
```

---

# 302. Candidate ADR — Production Data

```text
ADR:
Production recovery records and user media are prohibited from the public source repository.
```

---

# 303. Security Severity Summary

## Critical / High

```text
unsafe eval() of persisted embedding data

publicly committed password hash database

password hashes written to logs

credential database dumped to logs

weak unsalted password hashing

potentially personal resolved-case data committed publicly
```

---

# 304. Matching Severity Summary

## High

```text
missing modalities replaced with synthetic blank inputs

uncalibrated threshold

only best candidate returned

no hard conflicts

no evidence coverage

no same-instance features

candidate result jumps directly to collection instructions
```

---

# 305. Data Severity Summary

## High

```text
CSV transactional state

multi-step non-atomic resolution

no concurrency control

local filesystem media

vectors serialized as text

no vector/model version metadata
```

---

# 306. Privacy Severity Summary

## High

```text
owner-detail records committed to repository

user images committed to repository

credentials committed to repository

no data classification

no retention rules

no media privacy transformations
```

---

# 307. Operational Severity Summary

```text
no CI

no tests

no production database

no object storage integration in runtime

no migrations

no event processing

no backups

no health/readiness design

no observability
```

---

# 308. Positive Technical Findings

The following concepts remain valid:

```text
CLIP shared image/text embedding space

cosine similarity

camera-based reporting

GPU/CPU model selection

institutional location selection

location contact directory
```

---

# 309. But Do Not Reuse the Matching Function

Dzoka should not port:

```text
find_best_match()
```

because it combines several unsuitable assumptions.

---

# 310. Better Dzoka Search Flow

```text
Text Query / Image Query
        ↓
QueryFingerprint
        ↓
Retriever Set
├── semantic text
├── global image
├── lexical
├── attributes
├── geo
├── time
└── identifiers
        ↓
Candidate Union
        ↓
RRF
        ↓
Conflict Engine
        ↓
Evidence Features
        ↓
Local Instance Reranker
        ↓
Confidence / Explanation
```

---

# 311. Better Dzoka CLIP Handling

Text query:

```text
text_embedding
```

Image query:

```text
image_embedding
```

Text + image:

```text
both explicitly available
```

Missing modality:

```text
NULL
```

---

# 312. Example Feature Contract

```text
FeatureScore {
  feature: "GLOBAL_VISUAL",
  available: true,
  score: 0.82,
  modelVersion: "...",
  reliability: 0.71
}
```

Missing:

```text
FeatureScore {
  feature: "GLOBAL_VISUAL",
  available: false,
  score: null
}
```

---

# 313. Better Model Storage

```text
embeddingId
fingerprintId
modality
modelId
modelRevision
dimension
vector
createdAt
inputHash
```

---

# 314. Better Location Model

Instead of:

```text
"Main Campus - Library"
```

only, use:

```text
Location
├── organizationId
├── siteId
├── buildingId
├── floorId
├── zoneId
├── geometry
└── rawDescription
```

---

# 315. Better Case Resolution

Upstream:

```text
owner text
→ click resolve
```

Dzoka:

```text
ClaimApproved
    ↓
VerificationSatisfied
    ↓
RiskPolicySatisfied
    ↓
RecoveryAuthorized
    ↓
Handover
    ↓
CustodyTransfer
    ↓
RecoveryConfirmed
```

---

# 316. Matching vs Recovery

This repository is a strong example of why these must remain separate.

The application treats:

```text
semantic similarity
```

as sufficient to tell the user where to collect an item.

Dzoka must explicitly reject that shortcut.

---

# 317. Physical Identity Problem

Suppose the database contains ten visually identical:

```text
white phone chargers
```

A CLIP model may rank one of them highly.

That does not answer:

```text
which charger belongs to this claimant?
```

---

# 318. Dzoka Solution

Candidate generation:

```text
CLIP
```

followed by:

```text
structured details
local visual evidence
location/time
private traits
protected identifiers
ownership verification
```

---

# 319. Why This Repository Is Still Useful

A weak foundation can still produce a strong architecture lesson.

This project demonstrates exactly how easily an AI lost-and-found prototype can become:

```text
image
→ embedding
→ cosine similarity
→ "match"
```

while omitting the hard parts:

```text
identity
trust
ownership
custody
recovery
```

---

# 320. Dzoka's Differentiation

Dzoka is not merely:

```text
CLIP + database
```

It is:

```text
multimodal perception
+
evidence-aware matching
+
private ownership verification
+
authorization
+
custody
+
recovery
+
federation
```

---

# 321. Source-vs-Claim Summary

| Capability | Audit result |
|---|---|
| Streamlit application | Implemented |
| Image reporting | Implemented |
| Camera input | Implemented |
| CLIP embeddings | Implemented |
| Text→image search | Implemented, flawed by synthetic image contribution |
| Image→image search | Implemented, flawed by synthetic text contribution |
| Cosine similarity | Implemented |
| Vector database | **Not implemented** |
| FAISS | README suggestion only |
| Secure authentication | **Not production-secure** |
| Admin authorization | Defined but not enforced |
| Claims | Not implemented |
| Ownership verification | Not implemented |
| Fraud detection | Not implemented |
| Custody | Not implemented |
| Recovery authorization | Not implemented |
| Secure handover | Not implemented |
| Audit trail | Not implemented |
| Federation | Not implemented |
| Tests | Not found |
| CI/CD | Not found |
| Project license | Unresolved |

---

# 322. Reuse Classification

## `ADOPT`

```text
NONE FROM THIS REPOSITORY DIRECTLY
```

Dzoka should adopt OpenCLIP from its proper upstream project instead.

---

# 323. `ADAPT`

Conceptually:

```text
text-to-image candidate search

image-to-image candidate search

camera capture

institutional location selector
```

---

# 324. `REFERENCE`

Use this repository as a reference for:

```text
small CLIP lost-and-found prototype
```

and as a useful example of why production recovery requires more than semantic similarity.

---

# 325. `REJECT`

Reject as Dzoka foundation because of:

```text
licensing uncertainty

unsafe deserialization

security weaknesses

privacy weaknesses

weak domain model

weak storage model

weak matching semantics

lack of production architecture
```

---

# 326. Recommended `source-audit.lock.json` Entry

```json
{
  "id": "trishthakur-lost-and-found-ai",
  "category": "platforms",
  "repository": "https://github.com/trishthakur/lost-and-found-ai",
  "revision": "8e5fcb6f79595c9d429679ba7868cac4a90e1319",
  "license": "UNRESOLVED",
  "licenseConfidence": "UNRESOLVED",
  "classification": "REJECT",
  "secondaryClassification": "REFERENCE_CLIP_RETRIEVAL_CONCEPT",
  "directCodeReuseAllowed": false,
  "audit": "docs/source-audit/platforms/trishthakur-lost-and-found-ai.md",
  "auditedAt": "2026-10-04"
}
```

---

# 327. Re-Audit Triggers

Re-audit if upstream:

```text
adds an explicit license

removes eval()

moves to real vector storage

fixes missing-modality scoring

introduces calibrated thresholds

adds top-K retrieval

adds local-instance features

adds proper password hashing

removes credential data from repository

removes owner PII from repository history

introduces proper authorization

adds claims

adds verification

adds recovery authorization

adds custody

adds tests

adds CI/CD

pins model revisions
```

---

# 328. Immediate Upstream Security Improvements

If maintaining this project, highest-priority changes are:

```text
1. Remove eval() from embedding loading.

2. Treat committed credential hashes as exposed.

3. Replace SHA-256 password hashing with a dedicated password KDF.

4. Stop logging password hashes.

5. Stop printing users.csv.

6. Remove potentially personal recovery records from the public repository.

7. Separate production data from source control.

8. Enforce actual role-based authorization.

9. Add login rate limiting.

10. Add privacy controls around item/owner data.
```

---

# 329. Immediate Matching Improvements

```text
1. Do not create blank-image embeddings for text-only queries.

2. Do not create empty-text evidence for image-only queries.

3. Return top-K candidates.

4. Separate retrieval score from confidence.

5. Calibrate thresholds against a labeled dataset.

6. Add category, location, and time evidence.

7. Add same-instance local visual evidence.

8. Add hard conflicts.

9. Version all embeddings.

10. Preserve model provenance.
```

---

# 330. Immediate Data Improvements

```text
1. Replace CSV authoritative storage with a transactional database.

2. Replace local images with object storage.

3. Use typed vector storage.

4. Add schema migrations.

5. Add atomic resolution transactions.

6. Add optimistic concurrency.

7. Add audit events.

8. Add retention policies.
```

---

# 331. Immediate Product Improvements

Instead of:

```text
match
→ "collect item here"
```

use:

```text
possible candidate
→ claim
→ verification
→ authorization
→ handover
```

---

# 332. Final Architecture Lesson

The repository represents the easiest version of the AI lost-and-found problem:

```text
photo
    ↓
CLIP
    ↓
cosine similarity
    ↓
best result
```

Dzoka needs to solve the much harder version:

```text
observation
    ↓
multimodal fingerprint
    ↓
high-recall retrieval
    ↓
same-instance reasoning
    ↓
hard conflicts
    ↓
evidence coverage
    ↓
calibrated candidate confidence
    ↓
claim
    ↓
private ownership verification
    ↓
recovery authorization
    ↓
custody
    ↓
secure handover
```

---

# 333. Final Source Decision

```text
SOURCE:
trishthakur/lost-and-found-ai

LICENSE:
UNRESOLVED

FOUNDATION:
REJECT

DIRECT CODE REUSE:
NO

TECHNICAL VALUE:
LIMITED

RESEARCH VALUE:
MODERATE

MAIN POSITIVE:
Demonstrates CLIP text/image candidate retrieval

MAIN NEGATIVE:
Treats global semantic similarity as much more meaningful than it is for physical-item recovery
```

---

# 334. Final Conclusion

`trishthakur/lost-and-found-ai` is a useful educational demonstration of multimodal retrieval, but it should **not** influence Dzoka's production architecture beyond the basic observation that CLIP-style text/image embeddings are useful for broad candidate discovery.

Its strongest concept is:

```text
TEXT / IMAGE
    ↓
CLIP EMBEDDING
    ↓
CANDIDATE RETRIEVAL
```

Its most important warning is:

```text
SEMANTIC SIMILARITY
≠
SAME PHYSICAL INSTANCE
≠
OWNERSHIP
≠
AUTHORIZATION TO COLLECT
```

Dzoka should therefore preserve the good part:

```text
multimodal retrieval
```

while replacing everything around it with:

```text
typed evidence

safe vector storage

model provenance

hybrid retrieval

local-instance matching

hard conflicts

evidence coverage

calibration

claims

private verification

recovery authorization

custody

secure handover

privacy

audit

federation
```

The governing architectural lesson is:

> **CLIP can help Dzoka decide where to look. It must never decide who owns an item or who is allowed to receive it.**