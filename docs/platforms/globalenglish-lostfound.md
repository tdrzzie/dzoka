# Source Audit — globalenglish01/lostfound

> **Dzoka — The Open Recovery Network**
>
> Source audit of the strongest direct lost-and-found matching implementation identified during Dzoka's initial architecture research.

---

# 1. Audit Metadata

| Field | Value |
|---|---|
| Repository | `https://github.com/globalenglish01/lostfound` |
| Upstream owner | `globalenglish01` |
| Project | Lost & Found Intelligent Matching Platform |
| Audited revision | `6ff8cffe55bfc3e7274124512a69fae746ceb643` |
| Short revision | `6ff8cffe` |
| Audit date | 2026-10-04 |
| License | MIT |
| Copyright | Tigersoft, 2026 |
| Primary language | Python |
| API framework | FastAPI |
| Persistence | PostgreSQL |
| Vector search | pgvector |
| Lexical search | PostgreSQL FTS + `pg_trgm` |
| ORM / database access | SQLAlchemy / psycopg |
| Primary classification | **ADAPT HEAVILY** |
| Direct-code reuse | Permitted subject to MIT notice/attribution requirements |
| Dzoka foundation? | Architectural reference, **not** a repository fork |

---

# 2. Audit Decision

## Classification

```text
ADAPT HEAVILY
```

This is currently the strongest direct lost-and-found implementation identified for several core Dzoka ideas:

- lost/found entity matching rather than generic search;
- hybrid candidate retrieval;
- evidence-oriented scoring;
- hard conflicts;
- missing-evidence semantics;
- model-version-aware embeddings;
- explicit matching runs;
- human decisions;
- explainable candidate evidence;
- category-aware weights;
- private/secret item attributes.

Dzoka should **not** use the repository as its complete application foundation.

Instead:

```text
UPSTREAM IMPLEMENTATION
        ↓
extract proven architectural lessons
        ↓
redesign into Dzoka bounded contexts
        ↓
extend multimodal identity
        ↓
add ownership verification
        ↓
add custody / recovery
        ↓
add federation
        ↓
add edge/CCTV
        ↓
add production trust/security
```

---

# 3. Why This Repository Matters to Dzoka

Many lost-and-found repositories implement:

```text
post item
    ↓
browse list
    ↓
basic keyword search
```

This repository instead treats matching as an **entity-resolution problem**:

> Do a LOST observation and a FOUND observation describe the same physical object?

That framing closely matches Dzoka's core problem.

The repository also makes an important architectural separation between:

```text
retrieval
matching
hard conflicts
ranking
human decision
```

rather than collapsing everything into one AI similarity score.

This directly influenced Dzoka's:

```text
ItemFingerprint
MatchingRun
MatchCandidate
MatchEvidence
ConflictEngine
EvidenceCoverage
MatchConfidence
Ownership Verification boundary
```

---

# 4. Audit Scope

This audit focuses on architecture-relevant implementation.

Primary inspected paths include:

```text
LICENSE

db/schema.sql

config/
├── matching_weights.json
├── conflict_rules.json
└── synonyms.json

backend/app/
├── ai/
│   ├── normalize.py
│   ├── embedding_provider.py
│   └── ...
│
├── matching/
│   ├── retrieval.py
│   ├── conflicts.py
│   ├── features.py
│   ├── scoring.py
│   └── engine.py
│
├── api/
└── repository.py

scripts/
├── bootstrap.py
├── benchmark.py
├── eval_synonyms.py
├── reembed.py
└── reextract.py
```

Supporting repository documentation and tests were also considered where relevant to architecture.

---

# 5. Audit Scope Limit

This is a **targeted architectural source audit**.

It is not:

- a line-by-line audit of every source file;
- a penetration test;
- a formal legal opinion;
- a full dependency-security audit;
- an independent reproduction of every upstream benchmark.

The conclusions below apply to the inspected revision.

---

# 6. Repository Architecture

At the audited revision, the project is structured roughly as:

```text
lostfound/
├── backend/
│   └── app/
│       ├── ai/
│       ├── api/
│       ├── matching/
│       ├── config.py
│       ├── db.py
│       ├── repository.py
│       └── main.py
│
├── config/
│   ├── matching_weights.json
│   ├── conflict_rules.json
│   ├── synonyms.json
│   └── ...
│
├── db/
│   └── schema.sql
│
├── docs/
│
├── domain/
│
├── scripts/
│
├── docker-compose.yml
└── LICENSE
```

The architecture is intentionally compact.

It does not attempt to use distributed microservices for every domain capability.

---

# 7. Runtime Model

Conceptually:

```text
Client
  ↓
FastAPI
  ↓
AI Understanding
  ↓
PostgreSQL
  ├── domain state
  ├── attributes
  ├── FTS
  ├── trigram
  └── pgvector
        ↓
Hybrid Retrieval
        ↓
Conflict Analysis
        ↓
Feature Scoring
        ↓
Candidate Ranking
        ↓
Explanation
        ↓
Human Decision
```

This is a useful demonstration that a sophisticated matching system does not require a large distributed architecture from day one.

---

# 8. Important Architectural Philosophy

The upstream implementation embodies a particularly useful principle:

```text
embedding
→ retrieval signal

structured evidence
→ matching evidence

hard contradiction
→ conflict

ranking
→ ordering

human
→ final physical-return decision
```

Dzoka retains this separation and extends it substantially.

---

# 9. Five-Layer Data Model

`db/schema.sql` explicitly organizes the system into five conceptual layers:

```text
1. Business
2. Master Data
3. AI Understanding
4. AI Retrieval
5. AI Decision
```

This is one of the strongest parts of the upstream design.

---

# 10. Layer 1 — Business

Representative tables include:

```text
item_records
lost_reports
found_reports
return_records
```

The upstream system maintains one general `item_records` table with LOST or FOUND observations.

Dzoka adapts this concept but separates:

```text
ItemRecord
LostReport
FoundReport
RecoveryCase
```

more explicitly at the domain level.

---

# 11. `item_records`

Important observed fields include:

```text
id
record_type
status
category_id
brand
model
raw_description
normalized_text
search_vector
created_by
created_at
updated_at
version
```

---

# 12. Raw Description Preservation

One of the most important design choices is:

```text
raw_description
```

is preserved separately from:

```text
normalized_text
```

The implementation comments explicitly state that AI must not overwrite the original description.

Dzoka adopts this as a hard invariant:

> **Raw human input must remain immutable evidence.**

---

# 13. Why Raw Preservation Matters

Suppose a user reports:

```text
"Ndasiya black Lenovo bag muJoina around 2."
```

An AI system might normalize it into:

```text
"black Lenovo backpack lost at Joina City around 14:00"
```

The normalized form is useful.

But it must never replace the original evidence.

Dzoka therefore stores:

```text
raw
normalized
canonical
translated
```

representations separately.

---

# 14. Lost Report Time Modeling

The schema contains:

```text
lost_at
lost_at_start
lost_at_end
```

This is an excellent design choice.

Users often say:

```text
around 2pm
last night
between 7 and 9
```

rather than precise timestamps.

Dzoka adopts and expands this as:

```text
TimeWindow
├── start
├── end
├── precision
├── rawExpression
└── timezone
```

---

# 15. Found Reports

The upstream model includes:

```text
found_at
found_location_id
found_by
storage_location
custody_status
reported_at
```

This demonstrates awareness that found-item custody matters.

However, upstream custody modeling is too shallow for Dzoka.

Dzoka replaces the simple custody field with a dedicated:

```text
CustodyRecord
CustodyEvent
CustodyTransfer
CustodyHold
CustodyIncident
```

domain.

---

# 16. Layer 2 — Master Data

Important upstream master data includes:

```text
item_categories
brands
locations
attribute_definitions
```

This is strongly aligned with Dzoka's requirements.

---

# 17. Category Hierarchy

`item_categories` supports:

```text
parent_id
code
name
level
location_tau_m
time_tau_hours
```

This allows matching behaviour to vary by item category.

Dzoka adapts this principle.

---

# 18. Category-Specific Context

A passport and an umbrella should not necessarily use identical matching assumptions.

For example:

```text
location tolerance
time tolerance
attribute importance
identity evidence
```

may differ by category.

This becomes a first-class `MatchingProfile` concept in Dzoka.

---

# 19. Attribute Definitions

The upstream schema treats attributes as configurable domain data.

Important fields include:

```text
attribute_code
data_type
searchable
matchable
importance_weight
conflict_severity
```

This is much better than hard-coding every possible property into an item table.

Dzoka adopts the same principle.

---

# 20. Layer 3 — AI Understanding

Representative tables:

```text
item_attributes
item_images
ai_analyses
```

This is another strong architectural separation.

AI output is stored as derived evidence rather than pretending to be raw truth.

---

# 21. `item_attributes`

Important fields include:

```text
attribute_code
value_text
value_number
value_boolean
value_json
original_value
source
source_type
confidence
is_secret
```

This influenced Dzoka's evidence model substantially.

---

# 22. Evidence Source

The upstream schema supports sources such as:

```text
USER
AI
ADMIN
OCR
VISION
IMPORT
SYSTEM
```

Dzoka generalizes this into evidence provenance.

---

# 23. Evidence Confidence

Attributes may contain:

```text
confidence
```

This allows the system to distinguish:

```text
user explicitly told us this
```

from:

```text
AI guessed this
```

Dzoka preserves both:

```text
model confidence
```

and:

```text
evidence reliability
```

because they are not the same thing.

---

# 24. Secret Attributes

The upstream project includes:

```text
is_secret
```

on item attributes.

This is highly relevant to ownership verification.

Secret attributes can represent facts that should not appear publicly but may later help prove ownership.

Example:

```text
small sticker underneath
scratch inside case
hidden engraving
```

Dzoka expands this into:

```text
PUBLIC
RESTRICTED
SECRET
```

classification plus explicit disclosure history.

---

# 25. AI Analysis Provenance

`ai_analyses` includes fields such as:

```text
analysis_type
model_provider
model_name
prompt_version
input_hash
result_json
confidence
```

This is exactly the type of model provenance Dzoka requires.

---

# 26. Dzoka Extension to Model Provenance

Dzoka extends the concept into:

```text
ModelRun
├── provider
├── model
├── modelVersion
├── configurationVersion
├── promptVersion
├── inputHash
├── outputSchema
├── latency
├── cost
└── generatedAt
```

---

# 27. Layer 4 — AI Retrieval

Important upstream objects include:

```text
embeddings
matching_runs
```

This separation is architecturally important.

Embeddings are not stored directly inside the main item record.

---

# 28. Embedding Model

The upstream embedding schema contains:

```text
embedding_type
model_provider
model_name
model_version
dimensions
content_text
content_hash
embedding
status
```

This is one of the strongest design decisions in the repository.

---

# 29. Model Version Awareness

The repository explicitly recognizes:

> vectors produced by different embedding models cannot automatically be compared.

Dzoka adopts this as:

```text
EmbeddingCompatibilityKey
=
embeddingType
+ model
+ modelVersion
+ dimensions
+ semanticSpaceVersion
```

---

# 30. Embedding Migration

The upstream design supports statuses such as:

```text
ACTIVE
DEPRECATED
FAILED
PROCESSING
```

and provides re-embedding tooling.

The intended migration model is:

```text
old embeddings remain
    +
new embeddings generated
    ↓
evaluate
    ↓
switch active configuration
```

not:

```text
UPDATE old_vector = new_vector
```

Dzoka adopts this strongly.

---

# 31. Content Hash Deduplication

Embedding inputs can carry:

```text
content_hash
```

to avoid unnecessary recomputation.

Dzoka adopts content hashing for:

```text
embedding generation
fingerprint generation
media lineage
model runs
```

---

# 32. `matching_runs`

The upstream schema records matching executions separately.

Important fields include:

```text
source_item_id
trigger_type
algorithm_version
embedding_model
retrieval_config
ranking_config
candidate_count
duration_ms
created_at
```

This is excellent for reproducibility.

---

# 33. Matching Run Reproducibility

Without a `MatchingRun`, it becomes difficult to answer:

> Why did this candidate rank highly yesterday but not today?

Dzoka adopts this pattern directly at the design level.

Dzoka's version includes:

```text
MatchingRun
├── sourceFingerprintId
├── algorithmVersion
├── configurationVersion
├── retrievalConfiguration
├── modelVersions
├── rerankerVersion
├── candidateCount
├── duration
└── status
```

---

# 34. Layer 5 — AI Decision

Representative upstream objects include:

```text
match_candidates
match_evidences
match_decisions
```

This is one of the repository's most valuable architectural contributions.

---

# 35. Why Evidence Tables Matter

A weak system stores:

```text
match_score = 94
```

A stronger system stores:

```text
brand matched
model matched
location near
time matched
visual similarity
serial conflict
```

The upstream implementation follows the latter direction.

Dzoka takes this further with `MatchEvidence`.

---

# 36. Matching Pipeline

The audited source implements roughly:

```text
AI understanding
      ↓
candidate retrieval
      ↓
RRF
      ↓
hard constraints
      ↓
feature scoring
      ↓
weighted score
      ↓
bounded LLM analysis
      ↓
explanation
      ↓
human decision
```

This strongly influenced `docs/design/matching-engine.md`.

---

# 37. Retrieval Architecture

`backend/app/matching/retrieval.py` implements multiple retrieval channels.

Important channels include:

```text
structured
keyword / FTS
trigram
vector
```

and image-vector support where available.

---

# 38. Safe Base Pool

An especially good design choice is that the base candidate pool applies only relatively safe filters such as:

```text
record_type
status
```

Category is **not** treated as an absolute retrieval gate.

---

# 39. Why Category Is Not a Hard Gate

AI category extraction can be wrong.

If:

```text
lost item
→ incorrectly classified as WATER_BOTTLE
```

while the correct found record is:

```text
SAKE
```

using category as a mandatory database filter can make the correct match unreachable.

The upstream design instead allows category to influence:

```text
retrieval ranking
feature scoring
```

without necessarily eliminating candidates.

Dzoka adopts this principle.

---

# 40. Dzoka Retrieval Rule

Derived Dzoka rule:

> **AI-inferred attributes should not normally become destructive retrieval gates unless their reliability justifies it.**

Strong verified identifiers are different.

---

# 41. Lexical Search

The upstream implementation uses:

```text
PostgreSQL FTS
```

with:

```text
pg_trgm
```

fallback/support.

This demonstrates that PostgreSQL can handle useful hybrid lexical search before an additional search cluster becomes necessary.

---

# 42. Multilingual Limitation

The implementation itself recognizes limitations of PostgreSQL's simple FTS tokenization for languages such as:

```text
Japanese
Chinese
```

and uses trigram matching as a fallback.

This is useful evidence for Dzoka's search strategy:

```text
PostgreSQL first
OpenSearch optional later
```

rather than mandatory from day one.

---

# 43. Vector Retrieval

Vector retrieval uses:

```text
pgvector
```

with HNSW indexing.

The implementation explicitly filters by model identity during vector comparison.

This prevents mixed embedding spaces from being compared.

---

# 44. Vector Backend Lesson

The repository provides strong evidence that Dzoka can initially use:

```text
PostgreSQL
+
PostGIS
+
pgvector
+
pg_trgm
+
FTS
```

as one operational search foundation.

Dzoka therefore makes Qdrant optional behind:

```text
VectorSearchProvider
```

rather than mandatory.

---

# 45. Reciprocal Rank Fusion

`rrf_fuse()` implements:

```text
RRF(d) = Σ 1 / (k + rank_i(d))
```

with configurable `k`.

This combines multiple retrieval channels without pretending their raw scores share one scale.

Dzoka adopts RRF as a strong default candidate-fusion strategy.

---

# 46. Why RRF Matters

Consider:

```text
semantic similarity = 0.86
FTS rank = 0.17
image cosine = 0.91
```

These values are not naturally comparable.

RRF instead uses:

```text
rank within each retrieval channel
```

which is much safer for heterogeneous candidate generation.

---

# 47. Candidate Multi-Route Evidence

A candidate retrieved through:

```text
semantic
image
geo
```

should generally be prioritized over one appearing only through:

```text
semantic
```

all else being equal.

The upstream RRF architecture captures this naturally.

Dzoka retains the concept.

---

# 48. Hard Conflict Architecture

`backend/app/matching/conflicts.py` implements explicit conflicts separate from similarity scoring.

This is a critical strength.

---

# 49. Conflict Severities

The project models:

```text
CRITICAL
MAJOR
MINOR
```

conflicts.

Dzoka adopts the same conceptual classes.

---

# 50. Critical Identifier Conflicts

Configuration includes strong identity fields such as:

```text
IMEI
serial number
passport number
```

with rejecting rules.

This means:

```text
high semantic similarity
```

cannot automatically overpower:

```text
known exact identifier mismatch
```

---

# 51. Example

```text
Lost:
iPhone 15 Pro
IMEI = X

Found:
iPhone 15 Pro
IMEI = Y
```

Even if:

```text
visual similarity = extremely high
semantic similarity = extremely high
location matches
```

the two devices should not be treated as the same physical unit.

Dzoka makes this a core invariant.

---

# 52. Model Conflicts

The upstream system also handles examples such as:

```text
iPhone 15 Pro
```

versus:

```text
iPhone 15 Pro Max
```

as important model conflicts despite high semantic similarity.

This is an important example of why embeddings cannot make the final identity decision.

---

# 53. Colour Conflict Handling

Another good detail is that colour mismatches are treated carefully.

Example:

```text
black
```

versus:

```text
dark grey
```

must not behave like an IMEI conflict.

The upstream system uses colour-family logic.

Dzoka adopts the principle of attribute-specific conflict semantics.

---

# 54. Conflict Aggregation

The upstream conflict system does not simply sum every penalty linearly.

Its strategy applies the strongest penalty fully and discounts additional penalties.

This reduces pathological behaviour where many small disagreements overwhelm otherwise strong evidence.

Dzoka should benchmark its own penalty model rather than copy exact values blindly.

---

# 55. Feature Architecture

`backend/app/matching/features.py` computes eight high-level dimensions:

```text
category
attribute
location
time
distinctive
semantic
keyword
image
```

All are normalized onto a common score range.

---

# 56. Missing Evidence

This is perhaps the most important matching lesson from the repository.

The code explicitly implements:

```text
missing evidence
→ score = None
```

rather than:

```text
missing evidence
→ score = 0
```

---

# 57. Missing Is Not Mismatch

Example:

Lost report:

```text
no photograph
```

Found report:

```text
photograph exists
```

Correct:

```text
image evidence unavailable
```

Incorrect:

```text
image mismatch
```

Dzoka adopts:

> **Missing evidence ≠ mismatch.**

as a system-wide invariant.

---

# 58. Available Evidence Normalization

The scoring formula uses only available evidence in the denominator.

Conceptually:

```text
S =
Σ(wᵢ × rᵢ × sᵢ)
─────────────────
Σ(wᵢ × rᵢ)
```

where the sum only contains available feature dimensions.

This avoids unfairly penalizing sparse reports.

---

# 59. Reliability Weighting

Feature evidence carries reliability.

The configuration distinguishes sources such as:

```text
SERIAL
SYSTEM
VISION
OCR
STAFF
USER
AI
INFERRED
UNCERTAIN
```

with different reliability values.

Dzoka adapts this concept but avoids treating those exact numbers as universal truth.

---

# 60. Dzoka Reliability Design

Dzoka separates:

```text
confidence
```

from:

```text
reliability
```

Example:

```text
Vision model:
"object is black"
confidence = 0.99
```

does not mean:

```text
black colour
```

is highly discriminative evidence of identity.

---

# 61. Attribute-Level Scoring

The upstream implementation scores shared attributes individually.

Examples:

```text
colour
model
serial
brand
material
```

with category-aware importance.

This is superior to a single structured-attribute equality flag.

Dzoka adopts the pattern.

---

# 62. Evidence Thickness

A particularly strong implementation detail is the adjustment for **attribute evidence thickness**.

Suppose two bags only share:

```text
colour = black
```

A naive attribute scorer might return:

```text
100%
```

because the only comparable field matches.

That is dangerously misleading.

The upstream system reduces reliability when only a small amount of attribute evidence was actually compared.

---

# 63. Dzoka Evidence Coverage

This finding directly influenced Dzoka's explicit separation of:

```text
MatchScore
```

from:

```text
EvidenceCoverage
```

and:

```text
MatchConfidence
```

---

# 64. Category Scoring

The implementation supports:

```text
exact category
same parent
related
unrelated
```

rather than simple boolean equality.

It also contains handling for uncertain category extraction.

This is a useful pattern for Dzoka's category taxonomy.

---

# 65. Category Hypotheses

A valuable idea is retaining more than one category candidate when classification is uncertain.

Dzoka develops this further as:

```text
CategoryHypothesis[]
```

rather than always forcing one category value.

---

# 66. Location Scoring

The upstream source supports:

```text
same location
same facility
nearby
same area
```

and geographic distance.

It uses an exponential distance-decay idea where applicable.

---

# 67. Environment-Specific Location Scale

The configuration includes different location tolerances for contexts such as:

```text
airport
station
train
office
shop
stadium
```

This strongly supports Dzoka's decision that:

> There is no universally correct geographic matching radius.

---

# 68. Dzoka Extension of Location

The upstream approach is useful but Dzoka needs more.

Dzoka additionally models:

```text
routes
transport networks
institutional transitions
recovery graph
predicted movement
```

because a lost item can move legitimately far from the original location.

---

# 69. Time Scoring

The implementation uses time windows and distance-decay rather than an absolute equality test.

If a found timestamp falls inside the known loss window:

```text
strong time evidence
```

Otherwise evidence decreases as time separation grows.

This is a good baseline.

---

# 70. Dzoka Temporal Extension

Dzoka preserves:

```text
raw expression
precision
timezone
start
end
```

and may combine time with movement plausibility.

---

# 71. Distinctive Features

The upstream matcher has a distinct `distinctive` dimension.

This recognizes that unique marks can carry more identity value than ordinary attributes.

Examples:

```text
sticker
scratch
mark
damage
```

This directly influenced Dzoka's `DistinctiveFeature` model.

---

# 72. Dzoka Extension — Local Visual Identity

The upstream distinctive feature comparison is largely structured/textual.

Dzoka extends this significantly with:

```text
local patch embeddings
distinctive-region embeddings
multi-scale instance features
scratches
stickers
wear patterns
damage regions
```

based on additional source audits.

---

# 73. Semantic Feature

Semantic similarity is treated as useful evidence but not final authority.

This is exactly the right direction.

Dzoka preserves:

```text
semantic retrieval
```

and:

```text
semantic feature evidence
```

while preventing it from independently establishing same-object confidence.

---

# 74. Keyword Feature

Keyword evidence remains an independent feature.

This is useful because:

```text
rare explicit model string
custom label
brand
specific phrase
```

may contain important lexical identity signals that generic embeddings smooth over.

---

# 75. Image Feature

The upstream scoring system has an image-similarity dimension.

Image absence returns unavailable rather than zero.

This is correct.

---

# 76. Dzoka Image Extension

Dzoka does not stop at one image cosine.

It requires:

```text
global embeddings
+
multiple image views
+
local patches
+
distinctive regions
+
OCR
```

to improve physical-instance discrimination.

---

# 77. Final Scoring

The upstream formula conceptually includes:

```text
available-evidence weighted score
-
conflict penalty
+
bounded evidence bonus
```

The evidence bonus is capped.

This prevents many weak positive signals from trivially driving the score to maximum.

---

# 78. Configurable Weights

Weights live in configuration rather than being deeply hard-coded in algorithm logic.

Examples include profiles for:

```text
smartphone
wallet
umbrella
passport
laptop
jewelry
```

This is a strong design.

Dzoka adopts versioned `MatchingProfile` configuration.

---

# 79. Why Dzoka Does Not Copy the Exact Weights

Values such as:

```text
attribute = .34
location = .13
```

are upstream calibration choices.

They should not become Dzoka truth.

Dzoka must tune profiles using:

```text
Dzoka datasets
hard negatives
recovery outcomes
calibration experiments
```

---

# 80. Evidence Coverage Caps

The upstream configuration includes confidence-level constraints when evidence coverage is low.

It also prevents weak semantic/keyword evidence from independently producing strong recommendation levels.

This is an excellent safety pattern.

---

# 81. Dzoka Confidence Architecture

Dzoka formalizes the distinction:

```text
RetrievalScore
MatchScore
EvidenceCoverage
IdentityEvidenceStrength
MatchConfidence
ConfidenceLevel
```

rather than using one score for every purpose.

---

# 82. Matching Engine Composition

`backend/app/matching/engine.py` separates:

```text
match_pair()
```

from:

```text
run_matching()
```

The pair comparison is intentionally closer to a pure computation, while orchestration handles persistence and retrieval.

This is a good testability pattern.

---

# 83. Pipeline Orchestration

The engine performs:

```text
retrieve candidates
    ↓
score bounded shortlist
    ↓
sort
    ↓
run heavier analysis on top candidates
    ↓
persist useful candidates
```

This reinforces Dzoka's two-stage matching architecture.

---

# 84. Bounded Heavy Processing

Only a limited top candidate set receives the more expensive LLM analysis.

This avoids:

```text
expensive inference × entire corpus
```

Dzoka generalizes this to:

```text
cheap retrieval
    ↓
structured scoring
    ↓
bounded local visual reranking
    ↓
optional multimodal/LLM reranking
```

---

# 85. LLM Role

A particularly strong upstream design decision is:

```text
LLM does not overwrite algorithm_score
```

The LLM can:

```text
analyze evidence
explain
recommend more conservative action
```

but cannot simply replace the deterministic score.

Dzoka adopts this principle.

---

# 86. Dzoka AI Authority Rule

Dzoka extends the upstream idea:

```text
AI may produce evidence
AI may rank
AI may explain
AI may suggest
```

but:

```text
AI cannot independently authorize property release
```

---

# 87. Secret Filtering Before LLM

The upstream `_llm_view()` explicitly avoids forwarding attributes marked secret.

That is a strong privacy pattern.

Dzoka generalizes this into explicit AI disclosure policy:

```text
AIProviderInputPolicy
```

with:

```text
PUBLIC
RESTRICTED
SECRET
```

classification.

---

# 88. Data Minimization Lesson

Do not simply pass:

```text
entire database object
```

to an LLM.

Build a minimum authorized AI view.

This becomes a Dzoka-wide rule.

---

# 89. Human Decision

The upstream project retains human confirmation.

Its matching system does not claim that an embedding automatically authorizes return.

This is one of the reasons it is a stronger reference than many other repositories examined.

---

# 90. Dzoka Extension to Human Authority

Dzoka separates human/business decisions into:

```text
MatchDecision
Claim
VerificationDecision
RecoveryAuthorization
CustodyTransfer
```

rather than one generic human confirmation step.

---

# 91. Human Feedback

Upstream match decisions can serve as future learning data.

This is a good design because confirmed/rejected matches provide:

```text
positive examples
hard negatives
```

for future ranking evaluation.

Dzoka adopts a dedicated:

```text
MLFeedback
```

bounded context.

---

# 92. Hard Negatives

A particularly useful future dataset consists of:

```text
same brand
same model
same colour
high embedding similarity
but different physical object
```

These are far more useful for Dzoka than easy negatives.

---

# 93. Embedding Providers

`embedding_provider.py` uses a provider abstraction.

Observed types include concepts such as:

```text
hashing
local sentence transformer
ONNX multilingual embedding
OpenAI-compatible provider
```

This proves the matching engine need not be hard-wired to one vendor.

---

# 94. Dzoka Provider Principle

Dzoka generalizes this into separate provider contracts:

```text
TextEmbeddingProvider
ImageEmbeddingProvider
OCRProvider
DetectionProvider
LLMProvider
VectorSearchProvider
```

---

# 95. Important Upstream Weakness — Dimension Fitting

The inspected embedding provider contains a helper that can:

```text
pad vectors with zeroes
or
truncate vectors
```

to match configured dimensions.

This is convenient operationally, but Dzoka should **not** copy this behaviour as the default production approach.

---

# 96. Why Dimension Padding/Truncation Is Dangerous

Changing vector length by blind truncation or zero-padding may alter embedding semantics or hide configuration errors.

Dzoka should instead require:

```text
provider-reported dimension
=
configured expected dimension
```

and fail explicitly on incompatibility unless a model-specific projection is intentionally defined.

---

# 97. Dzoka Decision — Reject Silent Vector Resizing

Dzoka explicitly rejects:

```text
embedding has wrong dimension
    ↓
silently resize
```

Preferred:

```text
embedding has wrong dimension
    ↓
INCOMPATIBLE_EMBEDDING
```

---

# 98. Search and Persistence Integration

The upstream system keeps:

```text
relational records
structured attributes
full-text search
trigram search
vectors
matching state
```

inside PostgreSQL.

This has major operational advantages for smaller installations.

---

# 99. Dzoka Database Decision Influenced by This Repository

This source was a major input into the Dzoka decision:

```text
DEFAULT

PostgreSQL
+ PostGIS
+ pgvector
+ pg_trgm
+ FTS
```

rather than requiring:

```text
PostgreSQL
+ Elasticsearch
+ Qdrant
```

from the beginning.

---

# 100. What Dzoka Adds Around PostgreSQL

Dzoka additionally requires:

```text
PostGIS
tenant isolation
transactional outbox
event processing
media/object storage
federation
model governance
backup/PITR
```

because its operational scope is much larger.

---

# 101. Upstream Transaction Model

Because the upstream system is compact, it can store several related records transactionally in PostgreSQL without an event-driven distributed architecture.

That is reasonable for its scale and scope.

---

# 102. Why Dzoka Still Uses Events

Dzoka additionally has:

```text
AI workers
messaging
federation
CCTV edge
couriers
rewards
notifications
ML pipelines
```

which require asynchronous processing.

Therefore Dzoka uses:

```text
transactional PostgreSQL
+
outbox
+
Kafka-compatible broker
```

for distributed workflows.

This is not a criticism of the upstream design; Dzoka has a broader system boundary.

---

# 103. API Surface

The upstream application exposes API concepts for:

```text
lost reports
found reports
search
AI extraction
matches
rematching
explanations
human match decisions
secret questions
secret verification
return
admin metrics
training pairs
configuration reload
```

This demonstrates an unusually complete matching-oriented API for a relatively compact lost-and-found project.

---

# 104. Secret Questions

The project includes APIs around secret questions and secret-attribute verification.

This is especially relevant to Dzoka.

It confirms that:

```text
private item attributes
```

can be useful in a real recovery workflow.

---

# 105. Where Dzoka Goes Beyond Secret Questions

The upstream implementation is not the complete Dzoka ownership-verification design.

Dzoka additionally needs:

```text
automatic private evidence selection
disclosure history
safe challenge generation
challenge leakage validation
attempt limiting
semantic response interpretation
receipts
pre-loss photos
entitlement
competing claims
fraud assessment
policy
human review
federated private verification
```

---

# 106. Return Workflow

The upstream API conceptually separates:

```text
match
```

from:

```text
return
```

which is directionally correct.

However, Dzoka requires a much deeper recovery domain.

---

# 107. Dzoka Recovery Extension

Dzoka introduces:

```text
Claim
VerificationSession
RecoveryAuthorization
CustodyRecord
CustodyTransfer
Handover
Shipment
Reward
RecoveryEvent
```

---

# 108. Testing Philosophy

The upstream project has tests for important matching invariants.

Examples of the types of invariants protected include:

```text
high semantic similarity cannot overpower model conflict
missing data is not conflict
colour families are not over-penalized
semantic similarity alone is insufficient
thin attribute evidence should not create full confidence
```

This is exactly the kind of regression testing Dzoka needs.

---

# 109. Dzoka Test Lesson

Architecture invariants should be executable tests.

Examples:

```text
missing evidence != mismatch

critical IMEI conflict rejects candidate

high semantic score cannot bypass critical conflict

low coverage caps confidence

AI cannot authorize release

federated remote score cannot be compared blindly
```

---

# 110. Evaluation Tooling

The upstream repository includes scripts for:

```text
corpus generation
synonym evaluation
benchmarking
re-embedding
re-extraction
```

This demonstrates another important principle:

> Matching quality must be measured, not inferred from demos.

---

# 111. Dzoka Evaluation Extension

Dzoka requires evaluation across:

```text
text
images
local-instance features
multilingual inputs
OCR
protected identifiers
location
time
federation
verification
recovery outcomes
```

---

# 112. Upstream Benchmark Interpretation

Upstream benchmark results are useful evidence of implementation maturity.

They should not be treated as proof that the same accuracy will apply to:

```text
Zimbabwe
airports
bus systems
universities
malls
African multilingual inputs
real-world noisy photography
```

Dzoka must create its own datasets.

---

# 113. Multilingual Lessons

The upstream implementation pays meaningful attention to:

```text
Japanese
Chinese
English
```

synonyms and multilingual embeddings.

This is relevant to Dzoka because real deployment should support multilingual and code-switched reporting.

---

# 114. Dzoka Language Requirements

Dzoka will need to handle combinations such as:

```text
English
Shona
Ndebele
regional languages
code switching
```

and later broader federation languages.

Therefore language normalization must remain replaceable and benchmarked.

---

# 115. Configuration-Driven Matching

Important matching behaviours live in configuration:

```text
weights
reliability
conflict rules
category profiles
environment distance parameters
```

rather than being entirely hard-coded.

Dzoka adopts versioned configuration.

---

# 116. Hot Configuration Risk

Dynamic configuration is useful, but Dzoka must preserve:

```text
which configuration produced each decision
```

Therefore configuration updates cannot simply overwrite historical meaning.

Every matching run records the exact configuration version.

---

# 117. Security Strengths

Notable good practices from inspected architecture include:

```text
secret attribute concept
LLM data minimization
human decision boundary
provider abstractions
model provenance
raw evidence preservation
```

---

# 118. Security Gaps Relative to Dzoka

The upstream project's scope does not provide Dzoka's full requirements for:

```text
OIDC
RBAC/ABAC
multi-tenancy
tenant isolation
federation signatures
private challenge disclosure history
custody authorization
courier webhooks
payment security
legal holds
security auditing
fine-grained encryption
```

Dzoka must build these independently.

---

# 119. Multi-Tenancy

The audited project is not a sufficient reference implementation for Dzoka's full SaaS/institutional multi-tenancy requirements.

Dzoka requires tenant-aware:

```text
database access
vectors
object storage
events
caches
federation
authorization
```

---

# 120. Federation

The upstream repository is fundamentally local.

It does not provide Dzoka's required:

```text
peer identities
node trust
signed protocol
federated candidate envelopes
privacy minimization
capability discovery
circuit breakers
cross-node verification
```

Federation remains a separate Dzoka subsystem.

---

# 121. CCTV

The audited repository does not provide Dzoka's complete CCTV/edge architecture.

Dzoka therefore draws CCTV design from other source audits.

---

# 122. Recovery Movement Intelligence

The upstream location/time model is useful for matching.

It does not implement Dzoka's:

```text
RecoveryTransitionModel
```

for predicting institutional movement.

That remains a BUILD capability.

---

# 123. Item Fingerprint Gap

The upstream project has many of the ingredients of an `ItemFingerprint`:

```text
attributes
text
images
embeddings
secret attributes
location
time
provenance
```

but does not provide the complete Dzoka abstraction.

---

# 124. Dzoka ItemFingerprint Extension

Dzoka combines:

```text
semantic identity
structured attributes
global visual embeddings
local patch features
distinctive regions
OCR
protected identity
location/time
provenance
model metadata
quality
coverage
```

into a versioned canonical evidence model.

---

# 125. Same-Instance Visual Gap

The upstream global image capability is useful.

However, same-model physical objects often require:

```text
local visual identity
```

such as:

```text
scratches
stickers
wear patterns
damage
patch-level correspondences
```

This is why Dzoka also audits:

```text
ILIAS
Patchwise Retrieval
RerankingTransformer
OpenCLIP
```

---

# 126. Match Confidence Gap

The upstream architecture already includes score levels and coverage constraints.

Dzoka further formalizes:

```text
MatchScore
EvidenceCoverage
IdentityEvidenceStrength
MatchConfidence
ConfidenceCalibration
```

to avoid conflating normalized score with probability.

---

# 127. Calibration Requirement

Dzoka should not assume:

```text
score = 93
```

means:

```text
93% probability same item
```

True probabilistic confidence requires calibration against labeled data.

---

# 128. Ownership Boundary

The upstream repository correctly keeps humans involved in item return.

Dzoka takes a stronger domain approach:

```text
Match
    ≠
Claim
    ≠
Verification
    ≠
Recovery Authorization
    ≠
Custody Transfer
```

---

# 129. Architecture Strength Summary

The strongest reusable architectural lessons are:

```text
entity matching framing

five-layer data separation

raw-description preservation

derived evidence provenance

attribute-level source/confidence

secret attributes

model-versioned embeddings

content-hash deduplication

matching-run persistence

hybrid retrieval

RRF

hard conflicts

missing != mismatch

available-evidence normalization

category-aware weights

environment-aware location scoring

evidence thickness

bounded heavy analysis

LLM cannot override deterministic score

human decisions as feedback
```

---

# 130. Weakness Summary

The main limitations relative to Dzoka are:

```text
single-system scope

no full federation

limited ownership verification

limited custody model

no recovery authorization domain

no courier orchestration

no reward/payment orchestration

no edge/CCTV subsystem

no institutional movement prediction

no full multi-tenancy architecture

no local patch instance matching

limited production security architecture

no distributed asynchronous workflow architecture
```

These are largely scope differences rather than reasons to reject the project.

---

# 131. Specific Pattern to Reject — Silent Vector Dimension Fitting

Do not copy:

```text
truncate vector
or
pad vector
```

merely to satisfy database dimension configuration.

Dzoka must treat dimension mismatch as an explicit compatibility error unless a mathematically defined projection exists.

---

# 132. Specific Pattern to Avoid — Fixed Universal Reliability

The upstream reliability values are useful examples.

Do not treat values like:

```text
USER = .85
AI = .70
OCR = .90
```

as universal constants.

Dzoka should calibrate them using its own evidence and deployment environment.

---

# 133. Specific Pattern to Avoid — Generic Global Location Decay Alone

Straight-line distance is useful but insufficient for many recovery environments.

Dzoka must support:

```text
facility hierarchy
routes
transit movement
institutional transitions
recovery graph
```

---

# 134. Specific Pattern to Extend — Secret Questions

Do not merely expose a static:

```text
secret question
```

system.

Dzoka needs:

```text
private evidence inventory
    ↓
challenge candidate scoring
    ↓
disclosure check
    ↓
safe challenge generation
    ↓
attempt controls
    ↓
multi-evidence verification
```

---

# 135. Adopt

Dzoka may directly adopt or depend on mature foundations demonstrated by this architecture where separately audited:

```text
PostgreSQL
pgvector
pg_trgm
```

The repository itself is not planned as a runtime dependency.

---

# 136. Adapt

Dzoka should heavily adapt:

```text
entity-resolution framing

five-layer evidence separation

raw input preservation

structured attribute evidence

secret attributes

AI analysis provenance

version-aware embeddings

matching run persistence

hybrid retrieval

RRF fusion

feature scoring

hard conflict architecture

missing-evidence semantics

reliability weighting

evidence thickness

category-specific profiles

environment-specific location behaviour

LLM-as-evidence-analyst boundary

human match decisions
```

---

# 137. Reference

Use as reference for:

```text
multilingual normalization
provider abstractions
benchmark tooling
model migration
search configuration
API workflow
```

while implementing Dzoka-specific contracts independently.

---

# 138. Explicitly Do Not Copy Blindly

Do not blindly copy:

```text
exact matching weights

exact confidence thresholds

exact reliability constants

single-node domain boundaries

simple custody_status model

vector dimension padding/truncation

deployment assumptions

upstream benchmark claims as Dzoka performance claims
```

---

# 139. Dzoka Mapping — Data Model

| Upstream | Dzoka |
|---|---|
| `item_records` | `ItemRecord` + reports |
| `lost_reports` | `LostReport` |
| `found_reports` | `FoundReport` |
| `item_attributes` | normalized evidence / fingerprint attributes |
| `item_images` | `MediaAsset` + derivatives |
| `ai_analyses` | `ModelRun` + derived evidence |
| `embeddings` | model-versioned `EmbeddingRecord` |
| `matching_runs` | `MatchingRun` |
| `match_candidates` | `MatchCandidate` |
| `match_evidences` | `MatchEvidence` |
| `match_decisions` | `MatchDecision` / ML feedback |

---

# 140. Dzoka Mapping — Matching

```text
Upstream
Hybrid Retrieval
    ↓
Hard Constraints
    ↓
8 Feature Dimensions
    ↓
Score
    ↓
LLM Explanation
    ↓
Human Decision
```

becomes:

```text
Dzoka
Candidate Retrieval
    ↓
Multi-route Rank Fusion
    ↓
Cheap Hard Conflicts
    ↓
Multimodal Feature Evidence
    ↓
Evidence Reliability
    ↓
Evidence Coverage
    ↓
Bounded Local/Multimodal Reranking
    ↓
Advanced Conflict Analysis
    ↓
Score Aggregation
    ↓
Confidence Calibration
    ↓
Explainability
    ↓
Claim / Verification
```

---

# 141. Dzoka Mapping — Verification

Upstream:

```text
secret attributes
secret questions
human return
```

Dzoka:

```text
Private Evidence Inventory
    ↓
Disclosure Guard
    ↓
Challenge Candidate Selector
    ↓
Safe Challenge Generator
    ↓
Response Interpretation
    ↓
Protected Identifier Evidence
    ↓
Prior Photo / Document Evidence
    ↓
Fraud / Risk
    ↓
Policy
    ↓
Human Review
    ↓
VerificationDecision
```

---

# 142. Dzoka Mapping — Persistence

The upstream repository strongly supports the design decision that PostgreSQL can initially own:

```text
domain state
structured attributes
full-text index
trigram index
vectors
match evidence
```

Dzoka additionally adds:

```text
PostGIS
transactional outbox
inbox
tenant controls
object storage
Kafka-compatible events
```

---

# 143. ADRs Influenced

This audit should be referenced by ADRs concerning:

```text
PostgreSQL as authoritative store

pgvector as default vector backend

vector provider abstraction

matching architecture

model-versioned embeddings

missing evidence semantics

hard conflicts

AI decision boundaries
```

---

# 144. Architecture Documents Influenced

This audit directly informs:

```text
ARCHITECTURE.md

DESIGN.md

docs/architecture/data-architecture.md
docs/architecture/event-architecture.md
docs/architecture/ai-architecture.md
docs/architecture/security-architecture.md

docs/design/item-fingerprint.md
docs/design/matching-engine.md
docs/design/ownership-verification.md
docs/design/claim-workflow.md
```

---

# 145. Source Audit Lock Entry

Recommended entry in:

```text
docs/source-audit/source-audit.lock.json
```

```json
{
  "id": "globalenglish-lostfound",
  "category": "platforms",
  "repository": "https://github.com/globalenglish01/lostfound",
  "revision": "6ff8cffe55bfc3e7274124512a69fae746ceb643",
  "license": "MIT",
  "classification": "ADAPT",
  "audit": "docs/source-audit/platforms/globalenglish-lostfound.md",
  "auditedAt": "2026-10-04"
}
```

---

# 146. Re-Audit Triggers

Re-audit this repository if upstream materially changes:

```text
matching algorithm

schema

vector backend

embedding model lifecycle

ownership verification

licensing

security model

evaluation framework
```

Also re-audit before directly porting substantial source code beyond isolated MIT-licensed patterns.

---

# 147. Final Assessment

`globalenglish01/lostfound` is the strongest direct architectural reference identified so far for Dzoka's core matching domain.

Its most valuable contribution is not a particular model.

It is the discipline of treating lost-property matching as:

```text
ENTITY RESOLUTION
      ↓
MULTIPLE RETRIEVAL CHANNELS
      ↓
STRUCTURED EVIDENCE
      ↓
HARD CONFLICTS
      ↓
AVAILABLE-EVIDENCE NORMALIZATION
      ↓
EXPLAINABLE SCORING
      ↓
HUMAN DECISION
```

That is substantially stronger than the common pattern:

```text
description
    ↓
embedding
    ↓
cosine similarity
    ↓
"94% match"
```

Dzoka should preserve those strengths while extending the architecture with:

```text
ItemFingerprint
local visual instance identity
OCR
protected identifiers
calibrated confidence
ownership verification
fraud/risk
claims
custody
recovery authorization
courier logistics
rewards
federation
CCTV edge
recovery graph
movement intelligence
multi-tenancy
production security
event-driven processing
```

---

# 148. Final Decision

```text
PRIMARY CLASSIFICATION:
ADAPT HEAVILY
```

### Adopt directly

```text
No complete upstream application layer.
Use separately audited underlying dependencies where appropriate.
```

### Adapt heavily

```text
data/evidence separation
hybrid retrieval
RRF
conflict engine
feature scoring
missing-evidence semantics
evidence thickness
model provenance
matching-run persistence
configuration-driven ranking
human-feedback architecture
```

### Reference

```text
multilingual normalization
provider architecture
benchmarking
model migration
secret-question workflow
```

### Build in Dzoka

```text
complete ItemFingerprint
same-instance local visual model
ownership verification engine
claim/risk/entitlement workflow
custody
recovery authorization
federation
edge/CCTV
recovery graph
movement intelligence
logistics/rewards
production trust architecture
```

The guiding conclusion is:

> **Use this repository as evidence that explainable, structured, model-aware entity matching is a strong foundation for lost-property recovery — then build Dzoka's much broader trust, federation, physical-custody, and recovery architecture around that foundation.**