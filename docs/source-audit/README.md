# Dzoka Source Audit

> **2026-10-26 status correction:** [INDEX.md](INDEX.md) and
> [source-audit.lock.json](source-audit.lock.json) are the canonical current records.
> The original text below is retained as supplied research guidance. Its statements
> about completed audits, classifications and architecture are historical claims;
> only completed canonical records with inspected-file evidence may be relied on.
> All 30 mandatory bounded static reviews are complete, with 176 inspected
> file/artifact records. A scoped Java/PostgreSQL foundation now exists; the full
> product and production deployment remain incomplete. See [STATUS.md](../../STATUS.md).
> Review completion is not runtime certification or unrestricted reuse permission.

> **Dzoka — The Open Recovery Network**

This directory contains the source audits for external open-source projects, standards, research implementations, frameworks, libraries, and architectural references evaluated during the design and implementation of Dzoka.

The purpose of the source audit is not to collect interesting GitHub links.

It exists to answer:

> **What exactly does this upstream project implement, how does it implement it, what can Dzoka safely reuse, what should Dzoka only learn from, and what must Dzoka build itself?**

Dzoka follows a **source-first architecture process**.

Important architectural decisions should not be based only on:

- README claims;
- marketing pages;
- repository descriptions;
- screenshots;
- blog posts;
- AI summaries.

Where an external repository materially influences Dzoka's architecture, the relevant implementation must be inspected.

---

# 1. Why Source Audits Exist

Many repositories describe capabilities that are significantly broader than what their code actually implements.

Examples discovered during Dzoka research included projects claiming:

```text
AI image matching
```

while the implementation actually used:

```text
text similarity
+
placeholder image scores
```

or projects claiming:

```text
secure ownership verification
```

while implementation consisted primarily of:

```text
one fuzzy text comparison
```

A README is therefore useful for orientation but is not sufficient evidence for an architectural decision.

The source audit exists to determine:

```text
README claim
      ↓
actual repository structure
      ↓
actual implementation
      ↓
operational limitations
      ↓
license implications
      ↓
Dzoka decision
```

---

# 2. Architecture Gate

For architecture-relevant external projects, Dzoka uses the following sequence:

```text
DISCOVER
    ↓
AUDIT SOURCE
    ↓
CLASSIFY
    ↓
PIN REVISION
    ↓
DOCUMENT FINDINGS
    ↓
RECORD LICENSE
    ↓
LOCK SOURCE AUDIT
    ↓
ARCHITECTURE DECISION
    ↓
ADR WHERE REQUIRED
    ↓
IMPLEMENT
```

Do not reverse this process.

In particular:

```text
architecture first
↓
look for repositories that justify it later
```

is discouraged.

---

# 3. What Counts as a Source Audit

A source audit is a focused examination of the implementation relevant to a Dzoka architectural decision.

It normally includes:

```text
repository structure
runtime architecture
domain models
important source files
algorithms
storage
APIs
security
testing
deployment
license
known limitations
```

The audit must identify specific code paths where practical.

Example:

```text
backend/app/matching/engine.py
backend/app/matching/retrieval.py
backend/app/matching/scoring.py
```

is stronger evidence than:

```text
"the repository has a matching engine"
```

---

# 4. What a Source Audit Is Not

A source audit does not necessarily mean:

> every line of a large upstream repository was manually reviewed.

For large projects such as:

```text
OpenCLIP
pgvector
Qdrant
PaddleOCR
PaddleDetection
OpenEPCIS
```

the audit should focus on architecture-relevant paths and explicitly state the scope reviewed.

Never claim a complete code audit when only selected subsystems were inspected.

---

# 5. Audit Classifications

Every audited source receives one primary classification.

```text
ADOPT
ADAPT
REFERENCE
REJECT
```

---

# 6. ADOPT

`ADOPT` means:

> Dzoka intends to use the upstream project directly as a dependency, service, standard, extension, or supported infrastructure component.

Examples may include:

```text
OpenCLIP
pgvector
PaddleOCR
```

subject to final implementation decisions.

ADOPT requires:

- acceptable license;
- compatible operational model;
- maintained or sufficiently stable source;
- clear integration boundary;
- known version/revision.

---

# 7. ADAPT

`ADAPT` means:

> Dzoka intends to reuse substantial architectural ideas or, where licensing allows, selected implementation patterns while redesigning them for Dzoka's domain.

Example:

```text
globalenglish01/lostfound
```

provides particularly strong concepts around:

```text
matching runs
feature evidence
hard conflicts
missing-evidence handling
human decisions
model-versioned embeddings
```

Dzoka does not become a fork of that project.

---

# 8. REFERENCE

`REFERENCE` means:

> the implementation contains useful architectural or algorithmic lessons, but Dzoka should not directly depend on or copy it.

Possible reasons:

```text
incompatible license
missing license
experimental quality
different domain
security weaknesses
obsolete architecture
partial implementation
```

REFERENCE projects are still valuable.

They may influence:

```text
protocol design
workflow design
algorithm selection
state-machine design
evaluation methods
```

---

# 9. REJECT

`REJECT` means:

> the source should not be used as a Dzoka implementation foundation.

Reasons may include:

```text
unsafe implementation
misleading capability claims
serious security flaws
unmaintainable structure
fundamental architectural mismatch
unusable license situation
placeholder functionality
```

A rejected source can still be documented so future contributors do not repeat the evaluation.

---

# 10. Optional Secondary Labels

An audit may additionally use labels such as:

```text
DEPENDENCY
STANDARD
ALGORITHM_REFERENCE
WORKFLOW_REFERENCE
SECURITY_REFERENCE
RESEARCH_REFERENCE
PROTOCOL_REFERENCE
HISTORICAL_REFERENCE
```

The primary classification remains:

```text
ADOPT | ADAPT | REFERENCE | REJECT
```

---

# 11. Licensing Rule

Repository accessibility does not imply permission to reuse code.

Before copying or adapting implementation code, verify the repository's actual license.

Important distinctions:

```text
README says MIT
```

is not necessarily equivalent to:

```text
repository contains a valid MIT LICENSE file
```

If license status is unclear:

```text
classification should normally be REFERENCE
```

until clarified.

---

# 12. Missing License

If no license exists:

> Source code may be publicly visible, but that does not grant general permission to copy, modify, or redistribute it.

Therefore Dzoka should:

```text
study concepts
document behaviour
reimplement independently
```

rather than copy source.

---

# 13. Copyleft Licenses

Projects under licenses such as:

```text
GPL
AGPL
```

may be excellent architectural references.

Their source must not be copied into Dzoka's permissively licensed components unless the project licensing strategy explicitly allows it.

The audit must record this implication.

---

# 14. Standards vs Implementations

Some audits cover standards rather than reusable application code.

Examples:

```text
GS1 EPCIS
GS1 traceability
DID / VC concepts
```

Classify these appropriately.

Example:

```text
ADOPT STANDARD
```

may mean:

> use the interoperability model

rather than:

> run this exact repository in production.

---

# 15. Required Audit Metadata

Every source audit should begin with:

```text
Repository
Upstream owner
Audited revision
Tag/release if applicable
Audit date
License
Primary languages
Audit scope
Classification
```

Example:

```md
## Audit Metadata

- Repository: https://github.com/example/project
- Upstream owner: Example
- Audited revision: abcdef...
- Release/tag: v1.4.2
- Audit date: 2026-10-04
- License: Apache-2.0
- Primary languages: Java, Python
- Classification: REFERENCE
```

---

# 16. Revision Pinning

Audits should identify the exact revision inspected.

Prefer:

```text
full commit SHA
```

rather than:

```text
main
master
latest
```

because upstream code changes.

Example:

```text
abcdef123456789...
```

makes the audit reproducible.

---

# 17. Tags

If a release is audited, record both:

```text
tag
commit SHA
```

where possible.

Do not assume a tag name uniquely describes future repository state if it can be moved upstream.

---

# 18. Source Audit Lock

Pinned source metadata is also recorded in:

```text
docs/source-audit/source-audit.lock.json
```

Conceptual structure:

```json
{
  "schemaVersion": 1,
  "auditedAt": "2026-10-04",
  "sources": [
    {
      "id": "example-project",
      "repository": "https://github.com/example/project",
      "revision": "FULL_COMMIT_SHA",
      "tag": "v1.0.0",
      "license": "Apache-2.0",
      "classification": "REFERENCE",
      "audit": "docs/source-audit/example-project.md"
    }
  ]
}
```

The lock file is the machine-readable index of audited upstream revisions.

---

# 19. Audit Template

Use the following structure where applicable:

```md
# Source Audit — <Project>

## 1. Audit Metadata

- Repository:
- Upstream owner:
- Audited revision:
- Release/tag:
- Audit date:
- License:
- Primary languages:
- Audit scope:
- Classification:

## 2. Why This Source Is Relevant

## 3. Repository Structure

## 4. Runtime Architecture

## 5. Domain / Data Model

## 6. Important Algorithms and Code Paths

## 7. APIs and Integrations

## 8. Persistence and Search

## 9. AI / ML Components

## 10. Security and Privacy

## 11. Reliability and Scalability

## 12. Testing and CI/CD

## 13. Deployment and Operations

## 14. Strengths

## 15. Weaknesses / Risks

## 16. Licensing Implications

## 17. What Dzoka Will Reuse

### Adopt

### Adapt

### Reference

### Explicitly Do Not Copy

## 18. Impact on Dzoka Architecture

## 19. Decision
```

Not every section must be equally long.

The important requirement is that architectural conclusions are grounded in inspected implementation.

---

# 20. Evidence Quality

Prefer evidence in this order:

```text
actual source code
    ↓
tests
    ↓
configuration
    ↓
deployment files
    ↓
official technical documentation
    ↓
README
    ↓
repository description
```

README claims should be verified against implementation for architecture-critical capabilities.

---

# 21. Tests as Architectural Evidence

Tests often reveal intended behaviour better than documentation.

Inspect relevant tests for:

```text
authentication
matching
claims
failure handling
federation
database behaviour
model compatibility
```

when those capabilities materially influence Dzoka.

---

# 22. Configuration as Architectural Evidence

Configuration files may reveal:

```text
actual weights
thresholds
providers
ports
storage backends
security defaults
```

that are absent from README documentation.

Important architecture-relevant config should be included in the audit.

---

# 23. Deployment Evidence

Where infrastructure matters, inspect:

```text
Dockerfiles
Compose files
Helm charts
Kubernetes manifests
environment templates
CI pipelines
```

Do not assume a repository is production-ready because it runs locally.

---

# 24. Security Review Scope

A source audit is not automatically a complete professional security audit.

However, architecture-relevant security issues should be documented.

Examples:

```text
hard-coded credentials
weak password hashing
secret leakage
unsafe eval
missing authorization
insecure tokens
PII exposure
unvalidated webhooks
```

---

# 25. Security Findings Affect Classification

A useful implementation may still be classified:

```text
REFERENCE
```

rather than:

```text
ADOPT
```

if its security design is unsuitable.

---

# 26. Capability Claims

For important capabilities, audits should explicitly distinguish:

```text
CLAIMED
IMPLEMENTED
PARTIAL
PLACEHOLDER
ABSENT
```

Example:

```text
Image matching
README: CLAIMED
Source: PLACEHOLDER
Decision: do not adopt matcher
```

---

# 27. Dzoka Reuse Categories

Within each audit, explicitly describe:

### Adopt

Things Dzoka may use directly.

### Adapt

Concepts Dzoka will redesign into its own architecture.

### Reference

Patterns worth studying.

### Do Not Copy

Implementation or practices specifically excluded.

---

# 28. Example

```text
Repository:
Example Lost & Found

ADOPT
- none

ADAPT
- claim lifecycle
- handover workflow

REFERENCE
- notification UX

DO NOT COPY
- password hashing
- embedded provider secret
- naive matching score
```

This is more useful than merely calling a repository "good."

---

# 29. Architecture Impact

Every meaningful audit should answer:

> What changed in Dzoka because we inspected this source?

Example:

```text
Before audit:
Qdrant required.

After audit:
PostgreSQL + pgvector becomes the default vector backend,
with Qdrant behind VectorSearchProvider for scale-out.
```

That is an architectural finding.

---

# 30. Architecture Decisions Must Remain Dzoka Decisions

Source audits inform Dzoka.

They do not automatically define Dzoka.

Avoid:

```text
upstream does X
therefore Dzoka must do X
```

Instead:

```text
upstream demonstrates X
+
other evidence
+
Dzoka requirements
↓
Dzoka architecture decision
```

---

# 31. Source Diversity

Important architectural decisions should ideally be informed by more than one source where mature alternatives exist.

Example matching research used lessons from:

```text
globalenglish01/lostfound
OpenCLIP
ILIAS
Patchwise Retrieval
RerankingTransformer
pgvector
Qdrant
```

rather than treating one repository as the complete solution.

---

# 32. Current Audit Families

Dzoka's source audits are grouped conceptually into the following families.

---

# 33. Direct Lost-and-Found Systems

These help evaluate:

```text
reporting
claims
matching
verification
recovery
notifications
```

Current sources include:

```text
FuricBond/Lost-And-Found

globalenglish01/lostfound

Lost-and-Found-Tracking-System/lost-and-found-system

trishthakur/lost-and-found-ai

AI-lost-and-found / SmartFind

La2eet

Smart Asset Finder

FindBack

KWASU Lost & Found Bot
```

---

# 34. Visual Retrieval and Same-Instance Matching

These inform:

```text
global embeddings
local features
instance retrieval
reranking
```

Sources include:

```text
OpenCLIP

VisualMatch

OpenVisualSearch

ILIAS

Patchwise Retrieval

RerankingTransformer
```

---

# 35. Data and Search Infrastructure

Sources include:

```text
pgvector
Qdrant
PostGIS
```

These inform:

```text
vector retrieval
spatial retrieval
search-provider abstraction
```

---

# 36. Perception

Sources include:

```text
PaddleOCR
PaddleDetection
Detectron2
Ultralytics
```

These inform:

```text
OCR
detection
segmentation
tracking
```

---

# 37. CCTV and Tracking

Sources include:

```text
CCTV Theft Finder
DeepSORT
```

They inform:

```text
coarse-to-fine disappearance search
tracking
ReID
edge evidence extraction
```

---

# 38. Federation

Sources include:

```text
Mycel
Open Federated Search
```

These inform:

```text
fan-out
peer trust
timeouts
circuit breakers
remote attribution
score-comparability concerns
```

---

# 39. Traceability and Identity

Sources include:

```text
OpenEPCIS
GS1 Traceability
DPP/DID research implementations
```

These inform:

```text
physical-object events
interoperability
optional persistent product identity
```

---

# 40. Movement Intelligence

Sources include:

```text
Trajectron++
```

but Dzoka's main movement problem is different.

Dzoka primarily models:

```text
institutional state transitions
```

rather than continuous pedestrian trajectories.

Example:

```text
bus
→ driver
→ depot
→ lost-property office
```

Therefore continuous trajectory models are research references, not the core recovery-transition design.

---

# 41. Rewards / Escrow

Sources include:

```text
OpenEscrow
```

which informs:

```text
state-machine
evidence
dispute
release
```

concepts.

Dzoka does not require blockchain for reward handling.

---

# 42. Current Strongest Architectural Sources

At the time this README was written, the strongest influences include:

```text
1. globalenglish01/lostfound
   domain/matching architecture

2. ILIAS
   instance-level retrieval research

3. Patchwise Retrieval
   local visual identity ideas

4. OpenCLIP
   multimodal representation

5. PostgreSQL + pgvector + PostGIS
   default search/data foundation

6. CCTV Theft Finder
   disappearance-search architecture

7. OpenEPCIS
   physical-object event semantics

8. Mycel
   federation patterns

9. PaddleOCR
   OCR

10. PaddleDetection
    permissive perception/tracking foundation

11. Lost-and-Found-Tracking-System
    claim/recovery workflow ideas

12. FindBack
    handover workflow ideas

13. La2eet
    private verification questions

14. KWASU Lost & Found Bot
    WhatsApp-first interaction reference
```

This list is descriptive, not a permanent ranking.

The individual audit files and lock file are authoritative.

---

# 43. Important Current Conclusions

The audit process has already changed Dzoka's architecture in several important ways.

---

# 44. PostgreSQL First for Search

Earlier architecture considered Qdrant as mandatory.

Source audit showed that Dzoka can initially combine:

```text
PostgreSQL
├── relational state
├── PostGIS
├── FTS
├── pg_trgm
└── pgvector
```

behind:

```text
VectorSearchProvider
```

with:

```text
PgVectorProvider
QdrantProvider
```

This reduces operational complexity without closing the scale-out path.

---

# 45. Missing Is Not Mismatch

The matching design now explicitly preserves:

```text
missing evidence
≠
negative evidence
```

This is a core Dzoka invariant.

---

# 46. Conflict Engine

Strong identifiers and contradictory evidence are modeled separately from weighted similarity.

Example:

```text
same model
same image appearance
same location
```

cannot silently overpower:

```text
verified IMEI mismatch
```

---

# 47. Evidence Coverage

A high normalized score based on little evidence is not treated as high confidence.

Dzoka therefore separates:

```text
MatchScore
EvidenceCoverage
MatchConfidence
```

---

# 48. Local Visual Identity

Source audit confirmed that global CLIP-like representations alone are insufficient for many same-instance cases.

Dzoka therefore includes:

```text
global visual embeddings
+
local patch features
+
distinctive regions
+
structured marks/defects
```

---

# 49. Federation Score Calibration

Independent nodes may use different:

```text
models
weights
candidate pools
calibration
```

Therefore:

```text
Node A score .92
```

is not automatically comparable to:

```text
Node B score .92
```

Dzoka preserves remote scores but performs local/network normalization and reranking.

---

# 50. CCTV Search

Rather than continuously processing months of raw footage, source audit identified a stronger pattern:

```text
rough last-known interval
    ↓
coarse search
    ↓
hours
    ↓
minutes
    ↓
seconds
    ↓
short event window
    ↓
tracking / ReID
```

This informs `dzoka-edge`.

---

# 51. Ownership Verification

No audited repository provided the complete Dzoka approach to:

```text
private evidence discovery
+
safe challenge generation
+
disclosure tracking
+
multi-evidence evaluation
+
fraud/risk
+
policy
+
human review
```

This remains a Dzoka-specific subsystem.

---

# 52. EPCIS

Dzoka should not invent every physical-object event concept from scratch.

The system therefore uses:

```text
Dzoka-native RecoveryEvents
```

with an:

```text
EPCISAdapter
```

for interoperability where appropriate.

---

# 53. Blockchain

The audit found no strong reason to make blockchain part of the core platform.

Dzoka prefers:

```text
append-only records
signatures
hash chaining
immutable archives
```

and may revisit DLT only if a concrete federation trust problem justifies it.

---

# 54. Audit Directory

Expected structure:

```text
docs/
└── source-audit/
    ├── README.md
    │
    ├── furicbond-lost-and-found.md
    ├── globalenglish-lostfound.md
    ├── lost-and-found-tracking-system.md
    ├── trishthakur-lost-and-found-ai.md
    ├── smartfind.md
    ├── la2eet.md
    ├── smart-asset-finder.md
    ├── kwasu-lost-found-bot.md
    ├── findback.md
    │
    ├── visualmatch.md
    ├── openvisualsearch.md
    ├── ilias.md
    ├── patchwise-retrieval.md
    ├── reranking-transformer.md
    ├── openclip.md
    │
    ├── pgvector.md
    ├── qdrant.md
    ├── postgis.md
    │
    ├── paddleocr.md
    ├── paddle-detection.md
    ├── detectron2.md
    │
    ├── object-tracking.md
    ├── cctv-theft-finder.md
    │
    ├── open-federated-search.md
    ├── mycel.md
    │
    ├── openepcis.md
    ├── gs1-traceability.md
    ├── dpp-did.md
    │
    ├── trajectory-prediction.md
    ├── escrow-rewards.md
    │
    ├── identity-policy.md
    ├── event-streaming.md
    ├── logistics-rewards.md
    │
    └── source-audit.lock.json
```

The exact files may evolve as more foundations are audited.

---

# 55. Infrastructure Audits

Architecture-critical infrastructure should also be audited when a meaningful choice exists.

Expected future/continued audits may include:

```text
Kafka
Redpanda
NATS

Keycloak

OPA

MinIO

Redis

MLflow

OpenTelemetry

Prometheus
Grafana
Loki

faster-whisper / Whisper

routing / courier / dispatch foundations
```

Do not assume a familiar tool automatically belongs in Dzoka.

---

# 56. Audit Before Mandatory Dependency

Before making a technology mandatory in the architecture, answer:

```text
Why this project?

What problem does it solve?

Why not the alternatives?

What does its actual implementation support?

What operational complexity does it add?

What license applies?

Can the capability remain behind an abstraction?
```

---

# 57. Provider Abstraction Rule

Where multiple viable implementations exist, the source audit should help define an abstraction rather than hard-wire one vendor.

Examples:

```text
VectorSearchProvider
├── PgVectorProvider
└── QdrantProvider
```

```text
DetectionProvider
├── PaddleDetectionProvider
├── Detectron2Provider
└── optional UltralyticsProvider
```

```text
MessagingProvider
├── WhatsAppCloudProvider
├── TwilioProvider
└── other adapters
```

---

# 58. Source Audit vs ADR

These documents serve different purposes.

`source-audit/*.md` answers:

> What did the upstream project actually implement, and what did we learn?

ADR answers:

> Which architectural choice did Dzoka make, and why?

Example:

```text
source audit:
pgvector supports the vector capabilities Dzoka requires initially

source audit:
Qdrant supports independent scale-out vector infrastructure

ADR:
Use pgvector as the default V1 vector backend behind VectorSearchProvider
```

---

# 59. Source Audit vs Architecture

The source audit informs:

```text
ARCHITECTURE.md
docs/architecture/*
DESIGN.md
docs/design/*
```

Those documents describe Dzoka.

The source audit describes upstream evidence used in reaching Dzoka's design.

---

# 60. Source Audit vs Implementation

Codex must not blindly port upstream source.

Before implementing an externally influenced component:

```text
read the relevant source audit
    ↓
check classification
    ↓
check license
    ↓
read relevant ADR
    ↓
implement Dzoka contract
```

---

# 61. Codex Rules

Coding agents working on Dzoka MUST:

1. read this README before using upstream source;
2. inspect the relevant individual audit;
3. respect its classification;
4. respect licensing notes;
5. use the pinned revision when validating architectural assumptions;
6. never copy code from `REFERENCE` or `REJECT` sources unless a later explicit licensing decision allows it;
7. document any new upstream project that materially changes architecture;
8. update `source-audit.lock.json` when a source revision is formally audited;
9. create/update an ADR when the audit changes a major architecture choice.

---

# 62. Adding a New Source

When evaluating a new repository:

```text
1. record repository URL
2. inspect repository metadata
3. establish license
4. pin commit
5. inspect relevant tree
6. inspect implementation paths
7. inspect relevant tests/config
8. document weaknesses/security
9. classify
10. document Dzoka reuse
11. update source-audit.lock.json
```

---

# 63. Updating an Existing Audit

If upstream changes materially:

```text
old audit revision
    ↓
new upstream revision
    ↓
targeted re-audit
    ↓
update findings
    ↓
update lock
```

Do not silently change the pinned commit.

---

# 64. Audit Diff

Where useful, record:

```text
Previous revision:
...

Current revision:
...

Material changes:
...
```

This is especially useful for dependencies under active development.

---

# 65. Stale Audits

An audit may become stale if:

```text
major upstream rewrite
license change
security issue
new architecture
repository archival
important release
```

Mark:

```text
STALE
```

or re-audit before relying on it for new architecture decisions.

---

# 66. Archived Projects

Archived projects can still be useful as:

```text
REFERENCE
HISTORICAL_REFERENCE
STANDARD_REFERENCE
```

but should rarely become new runtime dependencies without strong justification.

---

# 67. Repository Health

Audit may record:

```text
maintenance activity
release frequency
issue activity
CI status
test quality
```

but repository popularity should not substitute for technical fit.

---

# 68. Stars Are Not Architecture

Do not select a dependency because it has:

```text
50k GitHub stars
```

without inspecting whether it actually solves Dzoka's requirements.

Likewise, a small repository may contain a highly useful algorithmic pattern.

---

# 69. Security Red Flags

Explicitly record findings such as:

```text
hard-coded API keys
plaintext secrets
unsafe eval
weak cryptography
insecure password storage
PII leakage
unauthorized contact disclosure
fake/random AI scoring
unvalidated input
```

These may materially change the classification.

---

# 70. Placeholder Detection

Look for implementation patterns such as:

```text
Math.random()
fixed similarity score
always-return-success
TODO replacing model inference
mock provider enabled in production path
```

A feature backed by placeholders must not be described as production capability.

---

# 71. AI Capability Verification

When a repository claims AI functionality, inspect:

```text
actual model
actual provider
preprocessing
inference call
postprocessing
fallback path
evaluation
```

Do not assume a file named:

```text
ai_matcher.py
```

contains meaningful AI matching.

---

# 72. Fallback Behaviour

Audits must identify silent fallbacks.

Example:

```text
real CLIP encoder unavailable
→ average colour encoder
```

may be acceptable in tests but dangerous if used silently in production.

Dzoka providers should generally:

```text
fail explicitly
```

or expose degraded capability clearly.

---

# 73. Security Secrets

Any upstream source containing apparent secrets must be treated carefully.

Do not copy:

```text
API keys
tokens
credentials
connection strings
private endpoints
```

into Dzoka.

Report the pattern, not the secret value.

---

# 74. Benchmarks

Upstream benchmark claims must be contextualized.

Example:

```text
7/7 image Top-1
```

on a synthetic 7-item dataset is useful implementation evidence but is not proof of production accuracy.

Record:

```text
dataset
size
metric
conditions
limitations
```

---

# 75. Research Code

Research repositories may provide excellent algorithms while lacking:

```text
API security
multi-tenancy
operational tooling
deployment
failure handling
```

Classification will often be:

```text
REFERENCE
```

or:

```text
ADAPT
```

rather than ADOPT.

---

# 76. Prototype Code

A prototype can demonstrate:

```text
algorithm feasibility
UI concept
interaction design
```

without being production-ready.

Dzoka must not inherit prototype shortcuts accidentally.

---

# 77. Standards Compliance

When a repository claims compliance with:

```text
EPCIS
DID
VC
GS1
```

identify:

```text
which version
which subset
where implemented
```

where relevant.

---

# 78. Privacy Findings

Audit should consider:

```text
public/private data separation
PII handling
media exposure
contact information
secret identifiers
retention
```

because lost-property systems naturally contain sensitive information.

---

# 79. Operational Findings

For infrastructure dependencies consider:

```text
single-node operation
HA mode
storage
backup
recovery
resource requirements
observability
upgrade complexity
```

---

# 80. Dzoka Architecture Principles Derived from Audits

Source audits should reinforce, not violate, these current principles:

```text
PostgreSQL first
provider abstractions
event-driven expensive work
matching ≠ ownership
missing ≠ mismatch
evidence provenance
versioned model outputs
hard conflicts
local + global visual identity
privacy-safe federation
human-authorized recovery
append-oriented custody/traceability
```

---

# 81. Current Source Classification Snapshot

The following summarizes current direction.

This section is informational; individual audit files and the lock are authoritative.

| Source | Current Direction |
|---|---|
| `globalenglish01/lostfound` | ADAPT heavily |
| `FuricBond/Lost-And-Found` | REFERENCE / ADAPT concepts |
| `lost-and-found-system` | ADAPT workflow concepts |
| `trishthakur/lost-and-found-ai` | REJECT as foundation |
| SmartFind | REFERENCE |
| La2eet | REFERENCE verification pattern |
| Smart Asset Finder | REFERENCE only |
| FindBack | ADAPT recovery patterns |
| KWASU Lost & Found Bot | REFERENCE WhatsApp UX |
| OpenCLIP | ADOPT dependency |
| ILIAS | ADAPT research/evaluation ideas |
| Patchwise Retrieval | REFERENCE algorithm |
| RerankingTransformer | REFERENCE algorithm |
| VisualMatch | REFERENCE |
| OpenVisualSearch | REFERENCE |
| pgvector | ADOPT |
| Qdrant | ADOPT as optional provider |
| PostGIS | ADOPT/use extension |
| PaddleOCR | ADOPT |
| PaddleDetection | strong ADOPT/default candidate |
| Detectron2 | alternative provider |
| CCTV Theft Finder | ADAPT architecture |
| DeepSORT reference | REFERENCE |
| Mycel | REFERENCE federation architecture |
| Open Federated Search | REFERENCE |
| OpenEPCIS | ADOPT standard/integration foundation |
| GS1 Traceability | STANDARD REFERENCE |
| DPP/DID implementations | REFERENCE |
| Trajectron++ | RESEARCH REFERENCE |
| OpenEscrow | REFERENCE workflow/state-machine ideas |

---

# 82. Build vs Reuse

After all audits, each Dzoka capability should fall into one of:

```text
ADOPT
ADAPT
REFERENCE
BUILD
```

`BUILD` means:

> no audited source adequately implements the required Dzoka capability.

Important current BUILD areas include:

```text
Dzoka ItemFingerprint abstraction

cross-institution lost-property federation protocol

AI-selected private ownership challenges

challenge disclosure tracking

recovery-transition intelligence

integrated recovery authorization

privacy-safe federated verification

complete courier/reward orchestration
```

These may still use audited foundations.

---

# 83. Novelty Rule

Dzoka should not claim novelty for technologies that already exist.

Examples:

```text
CLIP embeddings
vector search
OCR
Kafka
object detection
```

are not novel Dzoka inventions.

Dzoka's distinct value is in the domain architecture and integration of these capabilities into a recovery network.

---

# 84. Source Attribution

When source code is directly reused under an allowed license, preserve all legally required:

```text
copyright notices
license notices
attribution
```

according to that license.

Source audits are not a substitute for legal compliance.

---

# 85. No Copy-by-Paraphrase

For sources with incompatible or absent licensing, do not:

```text
read source
rename variables
slightly rewrite code
paste into Dzoka
```

That is still derived copying.

Use clean architectural understanding and independent implementation.

---

# 86. Independent Reimplementation

For REFERENCE sources:

```text
study behaviour
document concept
close upstream source
implement against Dzoka's own contract
write independent tests
```

where clean-room discipline is appropriate.

---

# 87. Audit Traceability in Code

Where an implementation was heavily informed by an audit, documentation may reference:

```text
docs/source-audit/<source>.md
```

Do not clutter production code with unnecessary upstream commentary unless attribution/license requires it.

---

# 88. Audit Traceability in ADRs

ADRs should reference relevant audits.

Example:

```text
Decision:
Use PostgreSQL + pgvector by default.

Evidence:
- docs/source-audit/globalenglish-lostfound.md
- docs/source-audit/pgvector.md
- docs/source-audit/qdrant.md
```

---

# 89. Pull Request Expectations

A PR introducing a new major external dependency should answer:

```text
Has it been source-audited?

What license does it use?

Why is it needed?

Why can existing providers not solve this?

Does it change ARCHITECTURE.md?

Does it require an ADR?
```

---

# 90. Dependency Upgrade Expectations

Routine patch/minor library upgrades do not always require a full source re-audit.

Re-audit when an upgrade materially changes:

```text
architecture
license
security
runtime behaviour
storage
protocol
API contract
```

---

# 91. Source Audit Status

An audit may carry status:

```text
IN_PROGRESS
COMPLETE
STALE
REPLACED
```

Only `COMPLETE` audits should be considered fully approved architectural evidence.

---

# 92. Minimum Definition of Complete

An audit is complete when:

```text
repository pinned
license recorded
relevant implementation inspected
important risks documented
reuse classification assigned
architecture impact stated
lock entry created
```

---

# 93. Audit Quality Standard

A useful audit should allow another engineer to understand:

```text
what this source really does

where that behaviour lives

what it does well

where it fails

what Dzoka is borrowing

what Dzoka deliberately rejects
```

without repeating the initial investigation from scratch.

---

# 94. Review Questions

Before accepting an audit, ask:

```text
Did we inspect source rather than only README?

Did we identify exact code paths?

Did we inspect relevant tests/config?

Did we verify license?

Did we identify fallbacks/placeholders?

Did we document security weaknesses?

Did we distinguish concept reuse from code reuse?

Did the audit materially explain the architecture decision?
```

---

# 95. Final Source-Audit Principle

The purpose of this directory can be summarized as:

```text
DON'T TRUST CLAIMS
      ↓
INSPECT IMPLEMENTATION
      ↓
UNDERSTAND LIMITATIONS
      ↓
UNDERSTAND LICENSE
      ↓
EXTRACT THE RIGHT LESSONS
      ↓
MAKE A DZOKA DECISION
```

Dzoka should neither reinvent mature engineering unnecessarily nor become a patchwork of copied repositories.

The desired outcome is:

> **Understand the best available open-source implementations deeply enough to adopt what is mature, adapt what is useful, reject what is unsafe, and deliberately build the pieces that make Dzoka unique.**
