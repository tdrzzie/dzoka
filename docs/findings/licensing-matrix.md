# Licensing Matrix

> **Dzoka — The Open Recovery Network**
>
> Consolidated licensing and reuse matrix for repositories, libraries, standards implementations, and research projects audited or referenced during Dzoka architecture research.

---

# 1. Purpose

Dzoka intentionally audits licensing before source reuse.

A repository being:

```text
public on GitHub
```

does **not** automatically mean:

```text
open source
```

and a repository being technically useful does **not** automatically mean Dzoka may:

```text
copy
modify
redistribute
embed
ship
```

its source.

This document provides one authoritative view of:

```text
repository
license
license confidence
technical role
Dzoka reuse classification
code reuse status
architectural implications
```

---

# 2. Core Rule

Dzoka separates two independent decisions:

```text
TECHNICAL REUSE DECISION
```

and:

```text
LICENSE REUSE DECISION
```

Example:

```text
Technical value:
HIGH

License:
UNRESOLVED

Decision:
REFERENCE / ADAPT CONCEPTS
NO DIRECT CODE REUSE
```

---

# 3. Reuse Classifications

| Classification | Meaning |
|---|---|
| `ADOPT` | May be used substantially as provided |
| `ADAPT` | Concepts or implementation may be modified significantly |
| `REFERENCE` | Study architecture/algorithm; do not make it a primary dependency |
| `REJECT` | Do not use as a Dzoka implementation source |
| `BUILD` | Dzoka must implement the capability itself |
| `OPTIONAL` | Valid but not required in the default architecture |

---

# 4. License Confidence

| Status | Meaning |
|---|---|
| `CONFIRMED` | License text verified in source or official project |
| `CLAIMED` | README or metadata claims a license, but source verification is incomplete |
| `UNRESOLVED` | No adequate project license verified |
| `RESTRICTIVE` | Source is visible but reuse is contractually/copyright restricted |
| `MIXED` | Project/dependencies include materially different license implications |

---

# 5. License Families

Dzoka broadly treats licenses as follows.

## Permissive

Examples:

```text
MIT
Apache-2.0
BSD
```

Generally compatible with direct reuse, subject to notices and dependency obligations.

---

# 6. Weak / Strong Copyleft

Examples:

```text
GPL-3.0
AGPL-3.0
```

These require explicit architectural review before being included in a permissive Dzoka distribution.

They may still be usable as:

```text
separate deployed services
optional integrations
research/reference sources
```

depending on legal/architecture decisions.

---

# 7. No License

If a repository has no valid license:

```text
copyright defaults apply
```

Dzoka must assume:

```text
NO DIRECT CODE REUSE
```

unless the copyright holder grants permission.

---

# 8. All Rights Reserved

Explicit:

```text
All rights reserved
```

means:

```text
REFERENCE ONLY
```

unless separate permission is obtained.

---

# 9. Direct Lost-and-Found Platforms

| Source | License | Confidence | Technical Decision | Direct Code Reuse |
|---|---|---:|---|---|
| `globalenglish01/lostfound` | MIT | Confirmed | `ADAPT HEAVILY` | Yes, subject to MIT notice |
| `FuricBond/Lost-And-Found` | Unresolved | Unresolved | `REFERENCE / ADAPT CONCEPTS` | **No** |
| `Lost-and-Found-Tracking-System/lost-and-found-system` | MIT | Confirmed | `ADAPT` | Yes |
| `Malak-Rashad/LostAndFound` | Unresolved | Unresolved | `REFERENCE / ADAPT VERIFICATION CONCEPTS` | **No** |
| `fredopoku/Smart-Asset-Finder` | All Rights Reserved | Restrictive | `REFERENCE` | **No** |
| `dhanushshetty40/AI-lost-and-found` | README claims MIT; root LICENSE absent | Unresolved | `REFERENCE` | **No** |
| `pirinthaban/findback` | MIT | Confirmed | `ADAPT SELECTIVELY` | Yes |
| `trishthakur/lost-and-found-ai` | Unclear / unresolved | Unresolved | `REJECT FOUNDATION` | **No** |
| `kwasu-lost-found-bot` | Unresolved | Unresolved | `REFERENCE UX` | **No** |

---

# 10. globalenglish01/lostfound

## License

```text
MIT
```

**Status:** `CONFIRMED`

## Dzoka decision

```text
ADAPT HEAVILY
```

## Reusable areas

```text
hybrid retrieval
hard conflicts
evidence coverage
missing evidence semantics
matching provenance
configuration
human decisions
```

## License implication

MIT permits substantial reuse subject to preservation of required notices.

## Dzoka recommendation

Even though direct reuse is allowed, prefer:

```text
architectural adaptation
```

over wholesale fork because Dzoka's domain and runtime architecture differ substantially.

---

# 11. FuricBond/Lost-And-Found

## License

README claims:

```text
MIT
```

but the audited revision lacks the referenced root:

```text
LICENSE
```

file.

## Status

```text
UNRESOLVED
```

## Dzoka decision

```text
REFERENCE
+
ADAPT CONCEPTS
```

## Direct code reuse

```text
NO
```

until licensing is clarified.

## Useful concepts

```text
YOLO object crop
OpenCLIP
pHash
ORB
location
multi-signal visual reasoning
mutual confirmation
```

---

# 12. Lost-and-Found-Tracking-System

## License

```text
MIT
```

## Status

```text
CONFIRMED
```

## Dzoka decision

```text
ADAPT
```

## Useful areas

```text
claims
claim conflicts
admin decisions
GeoJSON
organizations
fraud signals
audit
retention
```

## Direct reuse

Permitted subject to MIT requirements.

## Caveat

Do not reuse insecure patterns such as:

```text
embedded provider credential fallback
public claim PII route
AI auto-award behavior
schema drift
```

License permission does not override technical/security rejection.

---

# 13. Malak-Rashad/LostAndFound / La2eet

## Repository

```text
Malak-Rashad/LostAndFound
```

## Product alias

```text
La2eet
```

## License

No valid root project license verified.

## Status

```text
UNRESOLVED
```

## Dzoka decision

```text
REFERENCE
+
ADAPT VERIFICATION CONCEPTS
```

## Direct source reuse

```text
NO
```

## Useful concept

```text
finder-defined private ownership questions
```

## Important note

`la2eet.md` and `malak-lostandfound.md` refer to the same upstream repository and should not be treated as separate licensed sources.

---

# 14. Smart Asset Finder

## Repository

```text
fredopoku/Smart-Asset-Finder
```

## License

```text
All rights reserved
```

## Status

```text
RESTRICTIVE
```

## Dzoka decision

```text
REFERENCE ONLY
```

## Direct reuse

```text
PROHIBITED
```

without explicit permission from the copyright holder.

## Reference areas

```text
QR recovery
WhatsApp notifications
rewards
registered assets
hardware ecosystem
proof upload
```

---

# 15. AI Lost and Found / SmartFind

## Repository

```text
dhanushshetty40/AI-lost-and-found
```

## License claim

README:

```text
MIT
```

## Verified source

Root:

```text
LICENSE
```

not present at audited revision.

## Status

```text
UNRESOLVED
```

## Dzoka decision

```text
REFERENCE ONLY
```

## Direct code reuse

```text
NO
```

until licensing is clarified.

## Additional reason not to adopt

Technical implementation contains substantial gaps between README claims and actual AI behavior.

---

# 16. FindBack

## Repository

```text
pirinthaban/findback
```

## License

```text
MIT
```

## Status

```text
CONFIRMED
```

## Dzoka decision

```text
ADAPT SELECTIVELY
```

## Reusable concepts

```text
FCM notifications
proof storage ideas
handover concept
reputation
institutional review
mobile UX
```

## Avoid despite permissive license

```text
random image matching
random embeddings
Math.random handover codes
permissive claim state mutation
self-editable trust state
```

---

# 17. Retrieval / Instance Matching Projects

| Source | License | Confidence | Dzoka Decision | Direct Code Reuse |
|---|---|---:|---|---|
| `soham-k-sinha/visualmatch` | Unresolved | Unresolved | `REFERENCE` | No |
| `GolffyCoding/OpenVisualSearch` | Unresolved | Unresolved | `REFERENCE` | No |
| `ilias-vrg/ilias` | MIT | Confirmed | `ADAPT / REFERENCE` | Yes |
| `kaist-ami/Patchwise-Retrieval` | Unresolved | Unresolved | `REFERENCE` | No |
| `uvavision/RerankingTransformer` | GPL-3.0 | Confirmed | `REFERENCE` | License review required |
| `mlfoundations/open_clip` | MIT | Confirmed | `ADOPT` | Yes |

---

# 18. VisualMatch

## License

No adequately verified project license.

## Status

```text
UNRESOLVED
```

## Decision

```text
REFERENCE
```

Useful for:

```text
image retrieval
text-image search
composed retrieval
OpenCLIP + FAISS concepts
```

No direct source copying.

---

# 19. OpenVisualSearch

## License

Unresolved at audit time.

## Decision

```text
REFERENCE
```

Useful architecture ideas:

```text
Go service
HNSW
gRPC
HTTP
ONNX CLIP
hybrid search
```

Direct source reuse should wait for license clarification.

---

# 20. ILIAS

## License

```text
MIT
```

## Status

```text
CONFIRMED
```

## Decision

```text
ADAPT / REFERENCE
```

Useful for:

```text
instance retrieval
feature extractors
FAISS experiments
query expansion
evaluation
```

Direct reuse permitted under MIT.

---

# 21. Patchwise Retrieval

## License

No adequately verified license during audit.

## Status

```text
UNRESOLVED
```

## Decision

```text
REFERENCE
```

Useful for:

```text
multi-scale patch representations
local-instance matching
```

Do not copy implementation.

---

# 22. RerankingTransformer

## License

```text
GPL-3.0
```

## Status

```text
CONFIRMED
```

## Decision

```text
REFERENCE
```

## Direct inclusion in permissive Dzoka core

```text
NOT BY DEFAULT
```

Requires explicit legal/architectural review.

Potential options:

```text
study algorithm
independently implement concepts
separate compatible deployment if justified
```

---

# 23. OpenCLIP

## Repository

```text
mlfoundations/open_clip
```

## License

```text
MIT
```

## Status

```text
CONFIRMED
```

## Dzoka decision

```text
ADOPT
```

Recommended as a default-compatible visual embedding provider foundation.

---

# 24. Vector Search / Databases

| Source | License | Confidence | Dzoka Decision | Role |
|---|---|---:|---|---|
| `pgvector/pgvector` | PostgreSQL-style permissive license | Confirmed | `ADOPT` | Default vector search |
| `qdrant/qdrant` | Apache-2.0 | Confirmed | `OPTIONAL ADOPT` | Scale-out vector backend |
| PostgreSQL | PostgreSQL License | Confirmed | `ADOPT` | Primary database |
| PostGIS | GPL-2.0-or-later | Confirmed | `ADOPT operationally` | Geospatial extension |

---

# 25. pgvector

## License

Permissive PostgreSQL-family license.

## Decision

```text
ADOPT
```

Recommended Dzoka V1 vector backend.

Reasons:

```text
PostgreSQL integration
HNSW
IVFFlat
transactional metadata proximity
lower operational complexity
```

---

# 26. Qdrant

## License

```text
Apache-2.0
```

## Decision

```text
OPTIONAL ADOPT
```

Use behind:

```text
VectorSearchProvider
```

when scale or vector-specific operational needs justify it.

---

# 27. PostgreSQL

## License

```text
PostgreSQL License
```

permissive.

## Decision

```text
ADOPT
```

Dzoka authoritative transactional store.

---

# 28. PostGIS

## License

```text
GPL
```

PostGIS is commonly deployed as a PostgreSQL extension.

## Decision

```text
ADOPT OPERATIONALLY
```

### Important distinction

Using PostGIS as infrastructure is different from copying PostGIS source into Dzoka.

Dzoka does not redistribute a modified embedded PostGIS codebase as part of the application core unless separately reviewed.

---

# 29. Vision / OCR Projects

| Source | License | Confidence | Dzoka Decision |
|---|---|---:|---|
| `PaddlePaddle/PaddleOCR` | Apache-2.0 | Confirmed | `ADOPT` |
| `PaddlePaddle/PaddleDetection` | Apache-2.0 | Confirmed | `ADOPT` |
| `facebookresearch/detectron2` | Apache-2.0 | Confirmed | `OPTIONAL ADOPT` |
| `ultralytics/ultralytics` | AGPL-3.0 / commercial options | Confirmed | `OPTIONAL / LICENSE-CONSCIOUS` |
| DeepSORT source audited | Unresolved | Unresolved | `REFERENCE` |

---

# 30. PaddleOCR

## License

```text
Apache-2.0
```

## Decision

```text
ADOPT
```

Recommended for OCR.

---

# 31. PaddleDetection

## License

```text
Apache-2.0
```

## Decision

```text
ADOPT
```

Recommended default candidate for:

```text
object detection
tracking
RT-DETR
MOT
```

---

# 32. Detectron2

## License

```text
Apache-2.0
```

## Decision

```text
OPTIONAL ADOPT
```

Suitable as an alternative provider.

---

# 33. Ultralytics

## License

Current open-source distribution is AGPL-based, with commercial licensing options available separately.

## Decision

```text
OPTIONAL
```

Do not make it a mandatory Dzoka core dependency without an explicit license/architecture decision.

Prefer permissively licensed defaults where technically adequate.

---

# 34. DeepSORT Source

The inspected DeepSORT repository had unresolved licensing.

## Decision

```text
REFERENCE
```

Use tracking concepts, not unlicensed source.

---

# 35. CCTV / Movement Projects

| Source | License | Confidence | Dzoka Decision |
|---|---|---:|---|
| `daanishmufti/cctv-theft-finder` | MIT | Confirmed | `ADAPT` |
| `StanfordASL/Trajectron-plus-plus` | MIT | Confirmed | `REFERENCE` |

---

# 36. CCTV Theft Finder

## License

```text
MIT
```

## Decision

```text
ADAPT
```

Strong reusable concepts:

```text
coarse-to-fine temporal search
empty-spot calibration
occlusion checks
event clips
```

---

# 37. Trajectron++

## License

```text
MIT
```

## Decision

```text
REFERENCE
```

Useful for probabilistic movement research.

Not a direct Dzoka movement-intelligence implementation because lost-property movement is often institutional/discrete rather than pedestrian trajectory prediction.

---

# 38. Federation Projects

| Source | License | Confidence | Dzoka Decision |
|---|---|---:|---|
| `splch/mycel` | AGPL-3.0 | Confirmed | `REFERENCE / ADAPT CONCEPTS` |
| `dstl/Open-Federated-Search` | Apache-2.0 | Confirmed | `REFERENCE` |

---

# 39. Mycel

## License

```text
AGPL-3.0
```

## Decision

```text
REFERENCE
+
ADAPT CONCEPTS
```

Strong ideas:

```text
peer fan-out
timeouts
circuit breakers
allowlists
source attribution
round-robin / normalized merging
```

## Direct core inclusion

Not by default.

AGPL implications require explicit review.

---

# 40. Open Federated Search

## License

```text
Apache-2.0
```

## Status

Confirmed.

## Project status

Archived.

## Decision

```text
REFERENCE
```

Useful historical federation concepts, but not suitable as the direct modern Dzoka foundation.

---

# 41. Traceability Projects / Standards

| Source | License / status | Dzoka Decision |
|---|---|---|
| `openepcis/epcis-repository-ce` | Apache-2.0 | `ADOPT STANDARD / OPTIONAL ADAPTER` |
| GS1 End-to-End Traceability | Standard/reference materials | `STANDARD REFERENCE` |
| `Jorge-Lopes/dpp-did` | Unresolved | `REFERENCE` |

---

# 42. OpenEPCIS

## License

```text
Apache-2.0
```

## Decision

```text
ADOPT STANDARD
OPTIONAL IMPLEMENTATION / ADAPTER
```

Dzoka should support EPCIS through:

```text
EPCISAdapter
```

without forcing native domain semantics to become EPCIS.

---

# 43. GS1 Standards

Standards documentation is not the same as a software dependency.

Dzoka may implement compatibility with:

```text
EPCIS 2.0
GS1 traceability concepts
```

subject to applicable standards terms and trademark/identifier requirements.

---

# 44. DPP / DID Experiment

## Repository

```text
Jorge-Lopes/dpp-did
```

## License

Unresolved at audit time.

## Decision

```text
REFERENCE
```

Useful for future:

```text
DID
VC
digital item identity
digital product passport
```

Do not copy code until licensing is clarified.

---

# 45. Recovery / Escrow

| Source | License | Confidence | Dzoka Decision |
|---|---|---:|---|
| `omslice/OpenEscrow` | MIT | Confirmed | `REFERENCE` |

---

# 46. OpenEscrow

## License

```text
MIT
```

## Decision

```text
REFERENCE
```

Reusable concepts:

```text
state machine
evidence
dispute
release
```

Dzoka does not adopt blockchain as the default recovery/reward architecture.

---

# 47. Direct Platform Alias Handling

The following files/sources require special care:

```text
la2eet
malak-lostandfound
```

They refer to the same repository:

```text
Malak-Rashad/LostAndFound
```

The licensing matrix therefore records the repository only once as the canonical source.

Recommended canonical ID:

```text
malak-lostandfound
```

with:

```text
productAlias = La2eet
```

---

# 48. Current License Risk Categories

## Green — Direct Reuse Generally Permitted

Subject to notices and normal dependency review:

```text
globalenglish/lostfound
Lost-and-Found-Tracking-System
FindBack
ILIAS
OpenCLIP
pgvector
Qdrant
PaddleOCR
PaddleDetection
Detectron2
CCTV Theft Finder
Trajectron++
Open Federated Search
OpenEPCIS
OpenEscrow
```

---

# 49. Yellow — Explicit Copyleft / Architecture Review Required

```text
Mycel
RerankingTransformer
Ultralytics
PostGIS source-level redistribution considerations
```

These are not automatically prohibited.

They require an explicit decision about:

```text
deployment boundary
distribution
modification
linking
network use
source obligations
commercial licensing
```

---

# 50. Red — No Direct Code Reuse

Current examples:

```text
FuricBond/Lost-And-Found
Malak-Rashad/LostAndFound
Smart Asset Finder
AI-lost-and-found / SmartFind
VisualMatch
OpenVisualSearch
Patchwise Retrieval
kwasu-lost-found-bot
trishthakur/lost-and-found-ai
dpp-did
DeepSORT audited source
```

Reason:

```text
license unresolved
or
all rights reserved
```

---

# 51. Important Distinction — Algorithm vs Source Code

Copyright generally protects implementation expression, not broad technical ideas.

Therefore Dzoka may independently implement concepts learned from restrictive/unlicensed projects such as:

```text
private challenge workflow
QR finder relay
patchwise retrieval
circuit breaker architecture
multi-signal matching
```

provided Dzoka does not copy protected implementation expression.

---

# 52. Independent Reimplementation Standard

When adapting concepts from an unlicensed/restrictive repository:

```text
describe concept in source-audit docs
derive Dzoka-specific requirements
design Dzoka API/domain model independently
write new implementation from Dzoka design
avoid copying source structure/function bodies
```

---

# 53. Dependency License Rule

A repository's project license does not automatically resolve the licenses of all dependencies.

Example:

```text
MIT application
+
AGPL dependency
```

may still create obligations depending on use.

Therefore dependencies require separate review where materially relevant.

---

# 54. Model License Rule

AI code license and AI model-weight license are separate.

Dzoka must verify:

```text
library license
model weights license
dataset/use restrictions
commercial-use terms
redistribution restrictions
```

before shipping a model.

---

# 55. AI Model Registry Licensing Metadata

Each production model registration should eventually contain:

```text
provider
model
version
codeLicense
weightsLicense
commercialUseAllowed
redistributionAllowed
attributionRequirements
sourceUrl
reviewedAt
```

---

# 56. SaaS vs Distribution

License obligations may differ depending on whether Dzoka is:

```text
self-hosted software distribution
```

or:

```text
central SaaS
```

AGPL is especially relevant for network-served modified software.

Therefore deployment model must be considered during license review.

---

# 57. Dzoka Core License Strategy

The preferred Dzoka core should avoid accidentally inheriting incompatible obligations from optional dependencies.

Recommended principle:

```text
PERMISSIVE CORE
+
OPTIONAL LICENSE-AWARE ADAPTERS
```

where feasible.

---

# 58. Preferred Default Dependency Policy

Where two technically adequate alternatives exist:

```text
permissive
vs
strong copyleft
```

prefer the permissive default unless the stronger-copyleft option provides substantial unique value.

---

# 59. Optional Copyleft Service Pattern

Where justified, a strong-copyleft tool may be deployed as:

```text
separate optional service
```

with clear licensing documentation.

This is an architecture/legal decision, not an automatic loophole.

An ADR/legal review is still required.

---

# 60. License Metadata in Source Audit Lock

Each entry in:

```text
docs/source-audit/source-audit.lock.json
```

should contain at least:

```json
{
  "repository": "...",
  "revision": "...",
  "license": "...",
  "licenseConfidence": "...",
  "classification": "...",
  "audit": "...",
  "auditedAt": "..."
}
```

---

# 61. Suggested License Status Values

Canonical values:

```text
MIT
APACHE-2.0
GPL-3.0
AGPL-3.0
POSTGRESQL
ALL_RIGHTS_RESERVED
UNRESOLVED
OTHER
```

---

# 62. Suggested License Confidence Values

```text
CONFIRMED
CLAIMED
UNRESOLVED
```

Do not overload the license field with confidence.

---

# 63. Suggested Reuse Values

```text
ADOPT
ADAPT
REFERENCE
REJECT
OPTIONAL
```

Multiple classifications may be represented explicitly where needed.

---

# 64. Example — Confirmed Permissive Entry

```json
{
  "id": "openclip",
  "repository": "https://github.com/mlfoundations/open_clip",
  "license": "MIT",
  "licenseConfidence": "CONFIRMED",
  "classification": "ADOPT"
}
```

---

# 65. Example — README Claim But Missing License

```json
{
  "id": "ai-lost-and-found",
  "repository": "https://github.com/dhanushshetty40/AI-lost-and-found",
  "license": "UNRESOLVED",
  "claimedLicense": "MIT",
  "licenseConfidence": "UNRESOLVED",
  "classification": "REFERENCE",
  "directCodeReuseAllowed": false
}
```

---

# 66. Example — Restrictive Source

```json
{
  "id": "smart-asset-finder",
  "repository": "https://github.com/fredopoku/Smart-Asset-Finder",
  "license": "ALL_RIGHTS_RESERVED",
  "licenseConfidence": "CONFIRMED",
  "classification": "REFERENCE",
  "directCodeReuseAllowed": false
}
```

---

# 67. Example — Copyleft Reference

```json
{
  "id": "mycel",
  "repository": "https://github.com/splch/mycel",
  "license": "AGPL-3.0",
  "licenseConfidence": "CONFIRMED",
  "classification": "REFERENCE",
  "requiresExplicitLicenseADR": true
}
```

---

# 68. License Review Before Pull Request

Any PR introducing a new external dependency should answer:

```text
What is the license?

Was the license verified?

Are model weights separately licensed?

Are transitive dependencies material?

Are we modifying the dependency?

Are we distributing it?

Is it server-side only?

Does it affect Dzoka's intended project license?

Are notices required?
```

---

# 69. License Review Before Source Copying

Before copying even a small upstream implementation:

```text
repository pinned?
license verified?
copyright notice known?
source attribution required?
modifications tracked?
code genuinely worth copying?
```

If not, implement independently.

---

# 70. Source Header / Notice Requirements

Where source is copied from a permissive project, retain required:

```text
copyright notice
license notice
attribution
NOTICE contents
```

as applicable.

---

# 71. Apache-2.0 Specific Considerations

For Apache-2.0 sources, Dzoka should preserve:

```text
license
notices
required attribution
```

and respect patent-related provisions.

---

# 72. MIT Specific Considerations

For MIT sources, preserve the license notice in substantial copies/distributions.

---

# 73. GPL / AGPL Specific Considerations

Do not casually copy GPL/AGPL implementation code into a permissively licensed Dzoka core.

An explicit licensing decision must determine:

```text
compatibility
distribution model
source obligations
network obligations
architecture boundary
```

---

# 74. No-License Repositories

When no license exists:

```text
REFERENCE CONCEPTS
```

is the safe default.

A repository owner may later add a license.

If so:

```text
re-audit
pin new SHA
update matrix
```

before reuse.

---

# 75. License Change Trigger

Re-audit a source when:

```text
LICENSE added
LICENSE removed
license changed
commercial license introduced
dependency license changes
model license changes
project ownership changes
```

---

# 76. Technical Decision Is Still Independent

Example:

```text
FindBack
MIT
```

does not mean:

```text
adopt random image matcher
```

Technical quality remains independently evaluated.

---

# 77. Conversely, Restrictive Source May Still Be Architecturally Valuable

Example:

```text
Smart Asset Finder
All Rights Reserved
```

still contributes useful concepts:

```text
QR recovery
anonymous finder
WhatsApp notifications
reward ledger
```

through independent reimplementation.

---

# 78. Canonical Matrix — Platform Sources

| ID | Source | License | Confidence | Reuse | Direct Copy |
|---|---|---|---|---|---|
| `globalenglish-lostfound` | `globalenglish01/lostfound` | MIT | Confirmed | Adapt heavily | Yes |
| `furicbond-lost-and-found` | `FuricBond/Lost-And-Found` | Unresolved | Unresolved | Reference / adapt concepts | No |
| `lost-and-found-tracking-system` | `Lost-and-Found-Tracking-System/lost-and-found-system` | MIT | Confirmed | Adapt | Yes |
| `malak-lostandfound` | `Malak-Rashad/LostAndFound` | Unresolved | Unresolved | Reference / adapt concepts | No |
| `smart-asset-finder` | `fredopoku/Smart-Asset-Finder` | All Rights Reserved | Confirmed | Reference | No |
| `ai-lost-and-found` | `dhanushshetty40/AI-lost-and-found` | Unresolved; README claims MIT | Unresolved | Reference | No |
| `findback` | `pirinthaban/findback` | MIT | Confirmed | Adapt selectively | Yes |
| `kwasu-lost-found-bot` | `zhuluh247/kwasu-lost-found-bot` | Unresolved | Unresolved | Reference | No |
| `trishthakur-lost-and-found-ai` | `trishthakur/lost-and-found-ai` | Unresolved | Unresolved | Reject foundation | No |

---

# 79. Canonical Matrix — Retrieval

| ID | Source | License | Confidence | Reuse | Direct Copy |
|---|---|---|---|---|---|
| `visualmatch` | `soham-k-sinha/visualmatch` | Unresolved | Unresolved | Reference | No |
| `openvisualsearch` | `GolffyCoding/OpenVisualSearch` | Unresolved | Unresolved | Reference | No |
| `ilias` | `ilias-vrg/ilias` | MIT | Confirmed | Adapt / reference | Yes |
| `patchwise-retrieval` | `kaist-ami/Patchwise-Retrieval` | Unresolved | Unresolved | Reference | No |
| `reranking-transformer` | `uvavision/RerankingTransformer` | GPL-3.0 | Confirmed | Reference | Explicit review |
| `openclip` | `mlfoundations/open_clip` | MIT | Confirmed | Adopt | Yes |
| `pgvector` | `pgvector/pgvector` | Permissive | Confirmed | Adopt | Yes |
| `qdrant` | `qdrant/qdrant` | Apache-2.0 | Confirmed | Optional adopt | Yes |

---

# 80. Canonical Matrix — Vision

| ID | Source | License | Confidence | Reuse |
|---|---|---|---|---|
| `paddleocr` | `PaddlePaddle/PaddleOCR` | Apache-2.0 | Confirmed | Adopt |
| `paddledetection` | `PaddlePaddle/PaddleDetection` | Apache-2.0 | Confirmed | Adopt |
| `detectron2` | `facebookresearch/detectron2` | Apache-2.0 | Confirmed | Optional adopt |
| `ultralytics` | `ultralytics/ultralytics` | AGPL-3.0 / commercial | Confirmed | Optional / license-conscious |
| `deepsort` | audited DeepSORT source | Unresolved | Unresolved | Reference |

---

# 81. Canonical Matrix — CCTV / Movement

| ID | Source | License | Confidence | Reuse |
|---|---|---|---|---|
| `cctv-theft-finder` | `daanishmufti/cctv-theft-finder` | MIT | Confirmed | Adapt |
| `trajectron-plus-plus` | `StanfordASL/Trajectron-plus-plus` | MIT | Confirmed | Reference |

---

# 82. Canonical Matrix — Federation

| ID | Source | License | Confidence | Reuse |
|---|---|---|---|---|
| `mycel` | `splch/mycel` | AGPL-3.0 | Confirmed | Reference / adapt concepts |
| `open-federated-search` | `dstl/Open-Federated-Search` | Apache-2.0 | Confirmed | Reference |

---

# 83. Canonical Matrix — Traceability

| ID | Source | License | Confidence | Reuse |
|---|---|---|---|---|
| `openepcis` | `openepcis/epcis-repository-ce` | Apache-2.0 | Confirmed | Adopt standard / optional adapter |
| `gs1-end-to-end-traceability` | GS1 reference material | Standards terms | N/A | Standard reference |
| `dpp-did` | `Jorge-Lopes/dpp-did` | Unresolved | Unresolved | Reference |

---

# 84. Canonical Matrix — Recovery

| ID | Source | License | Confidence | Reuse |
|---|---|---|---|---|
| `openescrow` | `omslice/OpenEscrow` | MIT | Confirmed | Reference |

---

# 85. Recommended Core Dependency Zone

Strong default candidates for Dzoka core/runtime:

```text
PostgreSQL
PostGIS
pgvector
PaddleOCR
PaddleDetection
OpenCLIP
OpenTelemetry
```

with their respective licensing obligations documented.

---

# 86. Optional Dependency Zone

Potential optional infrastructure:

```text
Qdrant
Detectron2
MLflow
OPA
Kafka / Redpanda
Keycloak
```

Each should have its own audit/ADR before final lock-in where material.

---

# 87. Restricted / Research Zone

Keep these behind explicit license/design review:

```text
Mycel
RerankingTransformer
Ultralytics
unlicensed research repositories
```

---

# 88. Dzoka Source Reuse Workflow

```text
Find candidate source
        ↓
Pin exact revision
        ↓
Inspect source
        ↓
Verify license
        ↓
Inspect dependency licenses
        ↓
Classify technical usefulness
        ↓
Classify legal reuse
        ↓
Record audit
        ↓
Record lock entry
        ↓
ADR if architectural dependency
        ↓
Reuse / adapt / reference / reject
```

---

# 89. No Automatic Dependency Adoption

A source appearing in:

```text
reusable-components.md
```

does not automatically mean it should be added to:

```text
pom.xml
requirements.txt
package.json
Docker image
```

The architecture must still justify it.

---

# 90. No Automatic Code Copy

Similarly:

```text
MIT
```

does not mean:

```text
copy as much as possible
```

Dzoka should preserve architectural coherence and minimize inherited technical debt.

---

# 91. Attribution Register

If Dzoka eventually incorporates source code directly, maintain a repository-level attribution document such as:

```text
THIRD_PARTY_NOTICES.md
```

or equivalent.

Potential contents:

```text
component
upstream repository
copyright
license
version/SHA
files reused
modifications
```

---

# 92. Container Image Licensing

License review should include container images.

Example:

```text
FROM some/vendor/image
```

may introduce terms different from the application code.

---

# 93. Operating-System Packages

Normal distribution packages may carry:

```text
GPL
LGPL
BSD
Apache
MIT
```

licenses.

These should be handled through standard dependency/SBOM processes rather than manually duplicated into every source audit.

---

# 94. SBOM

Production releases should generate a Software Bill of Materials.

Recommended formats include:

```text
CycloneDX
SPDX
```

---

# 95. Automated License Scanning

CI should eventually run dependency-license tooling to detect unexpected changes.

Possible checks:

```text
new dependency introduced
license changed
unknown license
forbidden license
```

---

# 96. License Policy File

Dzoka may later define machine-readable categories such as:

```text
allowed
review-required
prohibited
unknown
```

for dependency automation.

---

# 97. Example Policy

```text
MIT
Apache-2.0
BSD
PostgreSQL
→ normally allowed

GPL
AGPL
SSPL-like
custom commercial
→ review required

unlicensed
all rights reserved
→ direct reuse prohibited
```

This is a project policy, not universal legal advice.

---

# 98. Model/Dataset SBOM

AI introduces additional non-code artifacts.

Dzoka should eventually track:

```text
models
weights
tokenizers
datasets
evaluation corpora
```

alongside software dependencies.

---

# 99. Data License Matters

Even if model code is permissively licensed, training/evaluation data may have restrictions.

This is particularly relevant if Dzoka later trains on:

```text
recovery images
public product images
CCTV
receipts
documents
```

---

# 100. Community Contribution Rule

Contributors should certify that submitted code is:

```text
their own
properly licensed
or based on compatible upstream source
```

Dzoka may adopt a DCO or contributor agreement depending on final project governance.

---

# 101. Copy/Paste From Stack Overflow or Blogs

External snippets also have copyright/license considerations.

Do not treat:

```text
found on internet
```

as equivalent to reusable public-domain code.

---

# 102. AI-Generated Code

AI-assisted implementation should still avoid intentionally reproducing non-compatible source and should be reviewed like any other contribution.

The resulting Dzoka code must comply with Dzoka's licensing policy.

---

# 103. Future Dzoka License

The final Dzoka project license should be selected explicitly through an ADR/governance decision.

Potential considerations:

```text
community contribution
commercial adoption
institutional self-hosting
SaaS competition
copyleft preferences
compatibility with adopted dependencies
```

---

# 104. License Decision Must Precede Distribution

Before the first formal public release:

```text
choose Dzoka project license
generate notices
generate SBOM
review dependencies
review model licenses
review assets/fonts/icons
```

---

# 105. Fonts / Icons / Media

Not all licensing risks come from source code.

Review:

```text
fonts
icons
stock images
map tiles
sample datasets
logos
model weights
```

before public distribution.

---

# 106. Map Provider Terms

If Dzoka uses services such as:

```text
OpenStreetMap tile servers
Google Maps
Mapbox
```

API/service terms must be evaluated separately from open-source library licenses.

---

# 107. WhatsApp / Messaging Providers

Provider SDK licenses are only one part of the picture.

Also consider:

```text
platform terms
message template policies
data processing terms
rate limits
commercial restrictions
```

---

# 108. Payment Providers

Similarly, payment integration requires both:

```text
SDK license
```

and:

```text
provider contractual/compliance requirements
```

These are not interchangeable.

---

# 109. Federation Standards

When using:

```text
GS1
EPCIS
DID
VC
```

check both software implementation licenses and standard-specific terms where relevant.

---

# 110. Re-Audit Requirements

A license matrix entry must be reconsidered when:

```text
source revision materially changes
license changes
dependency changes
project forks
repository archived
copyright ownership changes
commercial edition introduced
```

---

# 111. High-Priority Remaining License Audits

Before final infrastructure lock-in, audit licenses and architecture for:

```text
faster-whisper
Kafka
Redpanda
NATS
Keycloak
OPA
MinIO
Redis
MLflow
Prometheus
Grafana
Loki
Tempo
ONVIF libraries
OSRM / Valhalla
```

---

# 112. High-Priority AI License Audits

Before models are locked:

```text
chosen OpenCLIP weights
Paddle model weights
speech model weights
translation models
rerankers
LLMs
```

must each receive model-specific license review.

---

# 113. High-Priority Frontend License Audits

Before distribution:

```text
Next.js
React
component libraries
icons
map libraries
PWA libraries
```

should be included in automated dependency review.

---

# 114. Canonical Rule for Unresolved Sources

Whenever:

```text
license = UNRESOLVED
```

the default is:

```text
study
document
independently implement
```

not:

```text
copy first
fix license later
```

---

# 115. Canonical Rule for Restrictive Sources

Whenever:

```text
license = ALL_RIGHTS_RESERVED
```

or equivalent:

```text
directCodeReuseAllowed = false
```

unless explicit authorization is obtained.

---

# 116. Canonical Rule for Copyleft Sources

Whenever:

```text
GPL
AGPL
```

appears:

```text
requiresLicenseADR = true
```

before incorporation into the Dzoka distribution architecture.

---

# 117. Canonical Rule for Permissive Sources

Permissive licenses allow reuse, but Dzoka must still:

```text
retain required notices
respect trademarks
review dependency/model licenses
assess technical suitability
```

---

# 118. Licensing Is Part of Architecture

Licensing can influence:

```text
service boundaries
default providers
deployment topology
distribution model
plugin architecture
dependency selection
```

Therefore it must be decided during architecture—not after implementation.

---

# 119. Current Default Licensing Strategy

The preferred current direction is:

```text
PERMISSIVE DZOKA CORE
        +
PERMISSIVE DEFAULT DEPENDENCIES
        +
OPTIONAL LICENSE-AWARE PROVIDERS
```

where technically feasible.

---

# 120. Current Default AI Direction

Prefer default AI components with permissive code licensing:

```text
PaddleOCR
PaddleDetection
OpenCLIP
```

while treating model weights as separately auditable artifacts.

---

# 121. Current Default Retrieval Direction

Prefer:

```text
PostgreSQL
PostGIS
pgvector
FTS
pg_trgm
```

as the default stack.

Optional:

```text
Qdrant
```

when scale justifies it.

---

# 122. Current Default Federation Direction

Do not directly embed Mycel code into Dzoka core merely because its architecture is useful.

Instead:

```text
study Mycel
adapt concepts
build Dzoka federation protocol
```

under Dzoka's chosen project license.

---

# 123. Current Default Verification Direction

Do not copy unlicensed private-question implementations.

Build:

```text
VerificationSession
VerificationChallenge
ChallengeResponse
DisclosureGuard
VerificationDecision
```

independently from Dzoka's design documents.

---

# 124. Current Default Recovery Direction

OpenEscrow provides conceptual state-machine lessons, but Dzoka should independently implement:

```text
RecoveryAuthorization
CustodyTransfer
HandoverCredential
RewardRelease
```

without mandatory blockchain dependency.

---

# 125. Current Default CCTV Direction

Adapt MIT-compatible CCTV temporal-search ideas where useful, while keeping privacy and edge architecture Dzoka-specific.

---

# 126. Matrix Maintenance Ownership

This document should be updated whenever:

```text
new source audit completed
license reclassified
source adopted
dependency removed
new provider selected
```

---

# 127. Conflict Resolution Rule

If this file conflicts with an individual source-audit document:

```text
re-check pinned source
```

and fix both.

Do not silently pick whichever license statement is more convenient.

---

# 128. README Is Not Final License Authority

A README statement is supporting evidence.

Prefer:

```text
root LICENSE file
official project licensing page
package metadata backed by source
copyright holder statement
```

where possible.

---

# 129. Source Audit Lock Is the Pinning Authority

This matrix describes policy and current findings.

Exact audited revisions belong in:

```text
docs/source-audit/source-audit.lock.json
```

---

# 130. Final License Matrix Principles

Dzoka follows these rules:

```text
1. Verify before copying.
2. Pin before auditing.
3. Separate technical value from legal permission.
4. Prefer permissive defaults.
5. Treat model licenses separately.
6. Treat datasets separately.
7. Explicitly review copyleft.
8. Never copy unlicensed source.
9. Preserve required notices.
10. Re-audit when licensing changes.
```

---

# 131. Final Summary

The current source-audit landscape can be summarized as:

```text
DIRECTLY REUSABLE / PERMISSIVE
──────────────────────────────
globalenglish/lostfound
Lost-and-Found-Tracking-System
FindBack
ILIAS
OpenCLIP
pgvector
Qdrant
PaddleOCR
PaddleDetection
Detectron2
CCTV Theft Finder
Trajectron++
Open Federated Search
OpenEPCIS
OpenEscrow

REFERENCE / REIMPLEMENT
───────────────────────
FuricBond
Malak / La2eet
AI-lost-and-found
VisualMatch
OpenVisualSearch
Patchwise Retrieval
DPP-DID
DeepSORT audited source
kwasu-lost-found-bot
trishthakur lost-and-found-ai

RESTRICTIVE
───────────
Smart Asset Finder

EXPLICIT COPYLEFT REVIEW
────────────────────────
Mycel
RerankingTransformer
Ultralytics
```

---

# 132. Final Principle

Dzoka should be aggressive about learning from excellent systems and conservative about inheriting licensing risk.

The rule is:

> **If the concept is useful but the license is unclear, keep the idea and rewrite the implementation from Dzoka's own design.**

The strongest Dzoka architecture is therefore not the one that copies the most code.

It is the one that combines:

```text
verified licensing
+
technical quality
+
clean architecture
+
replaceable dependencies
+
clear provenance
```

into a system that can safely remain open, extensible, and distributable over the long term.