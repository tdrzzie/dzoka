# Unresolved Gaps

> **Dzoka — The Open Recovery Network**
>
> Canonical catalogue of capabilities, research questions, architectural uncertainties, implementation gaps, validation requirements, and source-audit areas that remain unresolved after the current Dzoka source-audit programme.

---

# 1. Purpose

The source-audit programme has identified many reusable components, useful patterns, and rejected approaches.

It has **not** identified a complete upstream implementation for Dzoka.

This document records the remaining gaps.

It answers:

```text
What does no audited source solve adequately?

What still requires Dzoka-specific design?

What requires additional source audits?

What requires experiments rather than architecture speculation?

Which decisions remain open?

Which risks must be resolved before production?
```

---

# 2. Relationship to Other Findings

This document should be read with:

```text
docs/source-audit/findings/reusable-components.md
docs/source-audit/findings/rejected-patterns.md
docs/source-audit/findings/licensing-matrix.md
docs/source-audit/findings/architecture-inputs.md
```

Their responsibilities are:

```text
reusable-components.md
→ what we can carry forward

rejected-patterns.md
→ what we must not inherit

licensing-matrix.md
→ what we are legally permitted to reuse

architecture-inputs.md
→ what architecture follows

unresolved-gaps.md
→ what still remains unsolved
```

---

# 3. Gap Status

Each unresolved gap should eventually move through:

```text
DISCOVERED
    ↓
RESEARCHING
    ↓
DESIGNED
    ↓
PROTOTYPED
    ↓
VALIDATED
    ↓
IMPLEMENTED
    ↓
PRODUCTION-VALIDATED
```

A gap must not be considered resolved simply because:

```text
a class exists
an API endpoint exists
a README says implemented
a model returned a score
```

---

# 4. Gap Classification

| Classification | Meaning |
|---|---|
| `ARCHITECTURE` | Major system design remains open |
| `DESIGN` | Architecture exists but detailed implementation design is incomplete |
| `ALGORITHM` | Core algorithm/research problem remains unresolved |
| `INFRASTRUCTURE` | Technology/provider choice remains open |
| `SECURITY` | Security mechanism needs further design/validation |
| `PRIVACY` | Privacy implications remain unresolved |
| `FEDERATION` | Cross-node behavior remains incomplete |
| `AI/ML` | Model, evaluation, or governance work remains |
| `OPERATIONS` | Production operation needs design/validation |
| `PRODUCT` | User/workflow behavior remains open |
| `LEGAL/POLICY` | Policy, licensing, regulatory, or governance decision remains |
| `RESEARCH` | Requires empirical investigation |

---

# 5. Overall Conclusion

No audited repository provides the complete combination of:

```text
multimodal item understanding

same-physical-instance retrieval

hard-conflict reasoning

private ownership verification

fraud/risk analysis

formal recovery authorization

physical custody

secure handover

courier orchestration

reward settlement

CCTV edge analysis

movement intelligence

institutional federation

traceability interoperability

privacy-aware AI

multi-tenancy
```

Therefore the remaining work is not merely:

```text
glue existing repositories together
```

Dzoka must build several original architectural layers.

---

# 6. Highest-Priority Unresolved Areas

The largest gaps are currently:

```text
1. same-physical-instance visual matching
2. dynamic private ownership verification
3. calibrated multimodal match confidence
4. complete lost-property federation protocol
5. formal recovery authorization
6. secure physical handover protocol
7. movement intelligence
8. CCTV-to-recovery evidence pipeline
9. cross-institution custody transfer
10. courier/reward integration semantics
11. privacy-aware AI provider governance
12. end-to-end evaluation datasets
```

---

# 7. GAP-001 — Same Physical Instance Recognition

**Classification:** `ALGORITHM / AI/ML`

**Priority:** `CRITICAL`

Existing systems commonly solve:

```text
same category
same product family
similar appearance
```

Dzoka needs:

```text
same physical object
```

These are substantially different problems.

---

# 8. Why This Gap Matters

Two phones can be:

```text
same brand
same model
same colour
same case
```

while being different physical devices.

A semantic image embedding may rank them nearly identically.

Dzoka therefore needs evidence such as:

```text
scratches
damage
stickers
engraving
wear
serial fragments
local geometry
texture
case imperfections
```

---

# 9. Existing Sources Only Partially Address It

Useful inputs exist from:

```text
FuricBond
ILIAS
Patchwise Retrieval
RerankingTransformer
OpenCLIP
```

but no audited source provides a production-ready lost-property same-instance solution.

---

# 10. Required Research

Evaluate combinations of:

```text
global embedding
local patch embeddings
keypoint/local features
perceptual hashes
object-specific fine-grained models
OCR
structured identifiers
```

on real difficult examples.

---

# 11. Key Research Dataset Needed

The evaluation dataset must include:

```text
same item, different angle

same item, poor lighting

same item, damaged later

same item, different background

same product model, different physical item

same colour, same model, same accessories

partial occlusion

cropped object

blurry images

low-resolution images
```

The most important negatives are:

```text
same-model different-instance
```

pairs.

---

# 12. Success Criteria

The final system must demonstrate:

```text
high recall at candidate stage

strong precision after reranking

low false-positive rate on same-model negatives

stable performance across image quality

explainable local evidence
```

---

# 13. GAP-002 — Local Visual Feature Architecture

**Classification:** `DESIGN / AI/ML`

**Priority:** `HIGH`

The architecture acknowledges local features.

The exact production implementation remains open.

Potential approaches include:

```text
multi-scale patch embeddings

keypoint matching

learned local descriptors

region proposals

saliency-guided patches

segmentation-aware instance descriptors
```

---

# 14. Open Questions

```text
Which local descriptor architecture performs best?

How many patches per item?

How are patches selected?

How are multiple images fused?

How are patch embeddings indexed?

Are local features persisted or computed on demand?

How much GPU/CPU cost is acceptable?

Can local evidence run only on top-N candidates?
```

---

# 15. GAP-003 — ItemFingerprint Final Schema

**Classification:** `DESIGN`

**Priority:** `HIGH`

The conceptual schema is defined.

The final database/API representation remains to be frozen.

Open questions include:

```text
fingerprint aggregate granularity

versioning mechanics

visual feature table design

feature compatibility metadata

canonical attribute consensus

how many fingerprint projections are persisted

whether public/federated views are materialized or generated
```

---

# 16. Required Resolution

A complete:

```text
docs/design/item-fingerprint.md
```

exists conceptually, but implementation will still require:

```text
SQL schema
JSON/API schema
event schema
migration strategy
compatibility rules
```

---

# 17. GAP-004 — Multimodal Score Calibration

**Classification:** `AI/ML / RESEARCH`

**Priority:** `CRITICAL`

Dzoka can compute many feature scores.

The unresolved question is:

```text
How should they combine into reliable match confidence?
```

---

# 18. Why Weighted Average Is Not Enough

Evidence may be:

```text
missing
correlated
redundant
unreliable
category-specific
model-specific
```

Example:

```text
global CLIP
local visual model
colour classifier
```

may all derive from the same photograph and are not independent evidence sources.

---

# 19. Required Research

Possible approaches:

```text
hand-tuned profiles
logistic regression
gradient boosting
learning-to-rank
Bayesian models
calibrated reranker
hybrid rules + model
```

---

# 20. Calibration Questions

```text
Should confidence represent P(same physical item)?

Should it instead remain a ranking confidence?

How much evidence coverage is required before showing probability?

Should calibration be per category?

Per environment?

Per model configuration?
```

---

# 21. GAP-005 — Evidence Dependency Modeling

**Classification:** `AI/ML / DESIGN`

**Priority:** `HIGH`

Current architecture recognizes evidence dependence but final implementation remains open.

Example dependencies:

```text
OCR from image
colour from image
CLIP from image
local patches from image
```

must not be naively treated as four fully independent confirmations.

---

# 22. Needed Component

Potential:

```text
EvidenceDependencyGraph
```

or simpler grouped evidence model:

```text
visual-image-1
visual-image-2
user-text
protected-id
location
time
institution-record
```

---

# 23. GAP-006 — Hard Conflict Catalogue

**Classification:** `DESIGN`

**Priority:** `HIGH`

The architecture contains a `ConflictEngine`.

The complete domain catalogue is not yet frozen.

Potential conflicts include:

```text
serial mismatch
IMEI mismatch
asset tag mismatch
passport number mismatch
brand incompatibility
model incompatibility
size impossibility
material incompatibility
chronology impossibility
registered-owner contradiction
```

---

# 24. Open Questions

```text
Which conflicts are CRITICAL?

Which are MAJOR?

Which can be overridden by human review?

Which must permanently reject a match?

How does evidence reliability affect conflict severity?

How are conflicts category-specific?
```

---

# 25. GAP-007 — Dynamic Ownership Challenge Selection

**Classification:** `ALGORITHM / SECURITY`

**Priority:** `CRITICAL`

No audited project provides a complete implementation for:

```text
private evidence
    ↓
safe challenge selection
    ↓
question generation
    ↓
answer evaluation
```

---

# 26. Required Challenge Properties

A good challenge should be:

```text
non-public
discriminative
answerable by legitimate owner
difficult to guess
safe to reveal as a question
supported by reliable evidence
```

---

# 27. Open Algorithm Questions

```text
How is discriminative strength computed?

How is guessability estimated?

How much evidence can a question reveal?

How are challenges rotated?

How are prior disclosure attempts tracked?

How many questions should be asked?

When is a challenge too sensitive to ask?
```

---

# 28. GAP-008 — Semantic Challenge Evaluation

**Classification:** `AI/ML / SECURITY`

**Priority:** `HIGH`

Exact identifiers can be compared deterministically.

Natural-language answers are harder.

Example:

```text
Expected:
small dark-red triangular sticker underneath

Claimant:
there's a little maroon triangle on the bottom
```

The system needs semantic interpretation.

---

# 29. Open Questions

```text
Can local embedding models handle this?

When should an LLM assist?

How are secrets protected from external models?

What confidence is safe?

When must a human review?

How is semantic similarity distinguished from ownership proof?
```

---

# 30. GAP-009 — Verification Evidence Independence

**Classification:** `DESIGN / RESEARCH`

**Priority:** `HIGH`

Verification may combine:

```text
receipt
photo
serial
private question
account history
```

but some evidence sources may be related.

Example:

```text
receipt serial
+
typed serial
```

may both come from the same underlying fact.

---

# 31. Needed Outcome

Verification should understand:

```text
evidence quantity
evidence diversity
independence
strength
```

rather than simply counting evidence items.

---

# 32. GAP-010 — Verification Confidence Calibration

**Classification:** `RESEARCH`

**Priority:** `HIGH`

The architecture distinguishes:

```text
match confidence
```

from:

```text
verification confidence
```

but verification-confidence semantics remain open.

---

# 33. Questions

```text
Should verification confidence be numeric?

Should policy use evidence tiers instead?

Can probabilities be calibrated reliably?

Should human decisions remain categorical?

How should uncertainty be presented to staff?
```

---

# 34. GAP-011 — Entitlement Model Edge Cases

**Classification:** `DESIGN / POLICY`

**Priority:** `HIGH`

The basic entitlement model includes:

```text
owner
guardian
representative
employee
executor
authorized agent
```

Many real cases remain.

---

# 35. Examples

```text
parent collecting child's item

employee collecting company laptop

estate representative collecting deceased person's property

embassy collecting passport

police collecting evidence

courier collecting on behalf of owner

school staff collecting student's item

shared ownership
```

---

# 36. Needed Work

Define:

```text
evidence requirements
delegation model
expiry
revocation
organization authority
minor/guardian relationships
```

---

# 37. GAP-012 — Claim Conflict Adjudication

**Classification:** `DESIGN / PRODUCT`

**Priority:** `HIGH`

The architecture supports `ClaimConflict`.

The actual adjudication process remains to be fully designed.

---

# 38. Questions

```text
Who adjudicates?

Can an institution adjudicate?

What if neither claim is conclusive?

What evidence can be requested?

Can claimants appeal?

When does the item go on indefinite hold?

What happens to reward offers?

How are malicious competing claims penalized?
```

---

# 39. GAP-013 — Recovery Authorization Policy Engine

**Classification:** `DESIGN / SECURITY`

**Priority:** `CRITICAL`

`RecoveryAuthorization` is a Dzoka-specific aggregate.

The full policy language/engine remains unresolved.

---

# 40. Required Inputs

Authorization may depend on:

```text
claim status
verification decision
risk assessment
custody state
institution policy
item category
legal hold
claim conflict
entitlement
handover method
```

---

# 41. Open Decision

Whether initial policy should be:

```text
Java deterministic policy modules
```

or:

```text
OPA
```

or:

```text
hybrid
```

remains open.

---

# 42. GAP-014 — Recovery Authorization Expiry / Revocation

**Classification:** `DESIGN`

**Priority:** `HIGH`

Need rules for:

```text
expiration
revocation
reissue
change of claimant
new conflict
new legal hold
custody change
```

---

# 43. GAP-015 — Secure Handover Protocol

**Classification:** `SECURITY / DESIGN`

**Priority:** `CRITICAL`

Upstream sources provide only partial concepts.

Dzoka still needs to finalize the complete handover protocol.

---

# 44. Protocol Requirements

Potential sequence:

```text
RecoveryAuthorized
    ↓
HandoverCredentialIssued
    ↓
sender presents item
    ↓
recipient authenticates
    ↓
credential verified
    ↓
condition confirmed
    ↓
sender confirmation
    ↓
recipient confirmation
    ↓
CustodyTransfer
```

---

# 45. Open Questions

```text
OTP vs QR vs both?

Must both parties confirm?

How are offline handovers handled?

Can a staff member override?

How are disputes recorded?

How does a courier use the protocol?

How are lockers handled?

What happens if credential leaks?
```

---

# 46. GAP-016 — Offline Handover

**Classification:** `SECURITY / OPERATIONS`

**Priority:** `HIGH`

Institutions may have poor connectivity.

Dzoka needs an offline-safe handover mechanism without allowing replay or conflicting custody transitions.

---

# 47. Possible Research Areas

```text
pre-authorized signed offline token
device-held credential
short-lived offline package
deferred signed event
```

This requires careful threat modeling.

---

# 48. GAP-017 — Cross-Institution Custody Transfer

**Classification:** `FEDERATION / DESIGN`

**Priority:** `CRITICAL`

No audited project provides robust:

```text
institution A
    ↓
institution B
```

physical custody transfer with federated authority.

---

# 49. Required Semantics

Need:

```text
transfer request
authorization
dispatch
in-transit state
receiving confirmation
dispute
timeout
loss in transit
chain of responsibility
```

---

# 50. GAP-018 — Cross-Node Recovery Authorization

**Classification:** `FEDERATION / SECURITY`

**Priority:** `CRITICAL`

Example:

```text
Node A
→ claimant registered

Node B
→ item held
```

Questions:

```text
Which node verifies ownership?

Which policy wins?

How does Node B trust Node A's verification?

Can evidence remain local?

Who issues release authorization?

How are disputes handled?
```

---

# 51. GAP-019 — Dzoka Federation Protocol Specification

**Classification:** `FEDERATION`

**Priority:** `CRITICAL`

The architecture is clear at a high level.

The protocol is not yet fully specified.

---

# 52. Required Protocol Areas

```text
node discovery

node identity

trust establishment

capability discovery

candidate query

candidate response

candidate follow-up

claim initiation

verification coordination

custody transfer

revocation

health

error semantics

rate limits

schema versions
```

---

# 53. GAP-020 — Federation Cryptography

**Classification:** `SECURITY / FEDERATION`

**Priority:** `CRITICAL`

Need final decisions for:

```text
key type
key rotation
signature format
canonical serialization
nonce rules
clock skew
replay windows
revocation
trust anchors
```

---

# 54. GAP-021 — Federation Candidate Privacy

**Classification:** `PRIVACY / FEDERATION`

**Priority:** `CRITICAL`

Need to determine how much item evidence can safely cross nodes.

---

# 55. Questions

```text
Can embeddings be shared?

Can OCR be shared?

Can serial HMACs be compared across nodes?

Should nodes use federation-specific keyed identifiers?

Can local distinctive images leave the node?

What is safe for PUBLIC vs RESTRICTED vs SECRET?
```

---

# 56. GAP-022 — Privacy-Preserving Identifier Matching Across Nodes

**Classification:** `SECURITY / RESEARCH`

**Priority:** `HIGH`

If two nodes need to discover:

```text
same serial
same IMEI
```

without revealing plaintext identifiers, a stronger design may be required.

Possible approaches include:

```text
federation-scoped keyed hashes
blind indexes
secure comparison protocol
trusted broker
```

No final choice exists.

---

# 57. GAP-023 — Federated Score Normalization

**Classification:** `FEDERATION / ALGORITHM`

**Priority:** `HIGH`

Architecture correctly says raw scores are not comparable.

The exact merging/ranking strategy still requires evaluation.

---

# 58. Candidate Strategies

```text
reciprocal rank fusion
round-robin
node confidence tiers
per-node calibrated score mapping
source reliability weighting
```

---

# 59. GAP-024 — Node Trust Scoring

**Classification:** `FEDERATION / SECURITY`

**Priority:** `MEDIUM-HIGH`

Should Dzoka distinguish:

```text
trusted government institution
verified airport node
community node
unknown node
```

in federation?

If so:

```text
How is trust established?
How does trust affect search?
How does it affect verification evidence?
Can trust decay?
```

---

# 60. GAP-025 — Federation Abuse / Spam Protection

**Classification:** `SECURITY / FEDERATION`

**Priority:** `HIGH`

Need protection from:

```text
query flooding
candidate spam
malicious peers
enumeration
data harvesting
replay
resource exhaustion
```

---

# 61. GAP-026 — Federation Discovery

**Classification:** `FEDERATION`

**Priority:** `MEDIUM`

Open questions:

```text
manual peer registration?

directory service?

DNS discovery?

organization registry?

trust federation?

public node registry?
```

---

# 62. GAP-027 — Federation Offline Peers

**Classification:** `FEDERATION / OPERATIONS`

**Priority:** `MEDIUM`

Need policy for:

```text
temporarily offline institution

deferred query

queued federation request

candidate expiration

late candidate arrival
```

---

# 63. GAP-028 — Movement Intelligence Model

**Classification:** `AI/ML / RESEARCH`

**Priority:** `HIGH`

Dzoka's movement model is conceptually different from pedestrian trajectory prediction.

It must learn transitions such as:

```text
bus
→ depot

airport gate
→ security
→ lost property

hotel room
→ housekeeping
→ reception
```

---

# 64. Needed Model Inputs

Potential:

```text
location hierarchy
organization workflows
custody history
item category
time of day
transport route
event sequences
recovery outcomes
```

---

# 65. GAP-029 — Movement Training Data

**Classification:** `AI/ML / DATA`

**Priority:** `HIGH`

There is currently no large Dzoka-specific movement dataset.

Possible initial approaches:

```text
expert-configured transition priors
synthetic workflows
institution-specific transition tables
later learned probabilities
```

---

# 66. GAP-030 — Movement Explainability

**Classification:** `AI/ML`

**Priority:** `MEDIUM`

If Dzoka predicts:

```text
likely moved to central lost-property desk
```

operators should know why.

Potential explanation:

```text
82% of similar items from this terminal
are transferred to desk X within 2 hours
```

---

# 67. GAP-031 — Movement Feedback Loop

**Classification:** `AI/ML`

**Priority:** `MEDIUM`

Successful recoveries should improve transition estimates, but care is needed to avoid:

```text
feedback bias
institution bias
self-reinforcing predictions
```

---

# 68. GAP-032 — CCTV Connector Standard

**Classification:** `INFRASTRUCTURE / EDGE`

**Priority:** `HIGH`

Need to define supported camera/VMS integration boundaries.

Potential standards/protocols:

```text
ONVIF
RTSP
vendor APIs
file upload
VMS export
```

---

# 69. GAP-033 — ONVIF Source Audit

**Classification:** `SOURCE AUDIT`

**Priority:** `HIGH`

An appropriate open-source ONVIF client/server library still requires:

```text
source audit
license review
runtime evaluation
```

---

# 70. GAP-034 — CCTV Item Re-Identification

**Classification:** `AI/ML / RESEARCH`

**Priority:** `HIGH`

Tracking an item inside one camera feed is easier than recognizing it across:

```text
different cameras
different angles
different lighting
partial views
```

No audited project solves this specifically for arbitrary lost objects.

---

# 71. GAP-035 — CCTV Disappearance Detection Generalization

**Classification:** `AI/ML`

**Priority:** `MEDIUM-HIGH`

`cctv-theft-finder` is useful for known fixed positions.

Dzoka must handle broader contexts:

```text
moving luggage
phone on table
bag on seat
item transferred by staff
object carried away
```

---

# 72. GAP-036 — CCTV Privacy Redaction

**Classification:** `PRIVACY / AI`

**Priority:** `CRITICAL`

Need production design for:

```text
face redaction
license plate redaction
document redaction
bystander privacy
```

while retaining enough item evidence.

---

# 73. GAP-037 — CCTV Retention

**Classification:** `PRIVACY / POLICY`

**Priority:** `CRITICAL`

Need explicit rules for:

```text
raw footage
event clips
keyframes
embeddings
track metadata
redacted derivatives
```

---

# 74. GAP-038 — CCTV Authorization

**Classification:** `SECURITY / POLICY`

**Priority:** `CRITICAL`

Who may request CCTV search?

Potential roles:

```text
institution staff
security
police
authorized recovery agent
```

Ordinary users should not gain arbitrary camera-search capability.

---

# 75. GAP-039 — CCTV Audit Trail

**Classification:** `SECURITY`

**Priority:** `HIGH`

Every CCTV search should record:

```text
who requested
case
camera
time range
purpose
result
exported evidence
```

---

# 76. GAP-040 — Edge Offline Operation

**Classification:** `EDGE / OPERATIONS`

**Priority:** `HIGH`

`dzoka-edge` must support:

```text
temporary network loss
queued evidence
local model inference
event replay
clock drift
```

---

# 77. GAP-041 — Edge Device Identity

**Classification:** `SECURITY`

**Priority:** `HIGH`

Need device provisioning and authentication for edge nodes.

Questions:

```text
mTLS?

device certificates?

key rotation?

revocation?

secure bootstrap?
```

---

# 78. GAP-042 — Edge Software Update Model

**Classification:** `OPERATIONS / SECURITY`

**Priority:** `MEDIUM-HIGH`

Need safe deployment/update mechanism for:

```text
edge connectors
models
configuration
camera adapters
```

---

# 79. GAP-043 — Edge Model Distribution

**Classification:** `AI/ML / OPERATIONS`

**Priority:** `MEDIUM`

Need strategy for:

```text
model download
version pinning
rollback
signature verification
hardware compatibility
```

---

# 80. GAP-044 — Speech-to-Text Provider

**Classification:** `INFRASTRUCTURE / AI`

**Priority:** `MEDIUM`

Likely candidate:

```text
faster-whisper
```

but source/license/deployment audit still required.

---

# 81. GAP-045 — Voice Privacy

**Classification:** `PRIVACY`

**Priority:** `MEDIUM`

Need decisions about:

```text
retain original audio?
transcribe then delete?
classification?
external STT?
```

---

# 82. GAP-046 — Translation Provider

**Classification:** `AI / INFRASTRUCTURE`

**Priority:** `MEDIUM`

Dzoka needs multilingual support.

Open questions:

```text
local models
external API
language-specific providers
hybrid
```

---

# 83. GAP-047 — Multilingual Matching

**Classification:** `AI/ML / RESEARCH`

**Priority:** `HIGH`

Example:

```text
lost report in Shona
found report in English
```

must still match.

Need to compare:

```text
translation-first
multilingual embeddings
hybrid
```

---

# 84. GAP-048 — Low-Resource Language Quality

**Classification:** `AI/ML`

**Priority:** `HIGH`

English-centric datasets may not adequately support:

```text
Shona
Ndebele
regional African language variants
```

for:

```text
translation
OCR
speech
semantic embeddings
```

---

# 85. GAP-049 — Local Place Name Resolution

**Classification:** `PRODUCT / DATA`

**Priority:** `MEDIUM`

Users may describe locations informally:

```text
near Copa Cabana
rank by the shops
Gate 3
old terminal
by the bridge
```

Need locality-aware resolution without inventing exact coordinates.

---

# 86. GAP-050 — Route-Aware Movement

**Classification:** `ALGORITHM`

**Priority:** `HIGH`

For transport-related recovery, straight-line distance is insufficient.

Need routing context from:

```text
road
rail
bus
airport
institution route
```

---

# 87. GAP-051 — OSRM vs Valhalla

**Classification:** `INFRASTRUCTURE`

**Priority:** `MEDIUM`

Routing-engine source audit/ADR remains open.

---

# 88. GAP-052 — Messaging Provider Strategy

**Classification:** `INFRASTRUCTURE`

**Priority:** `HIGH`

Architecture has `MessagingProvider`.

Exact initial providers remain open.

Likely channels:

```text
WhatsApp Cloud API
push
email
SMS
```

---

# 89. GAP-053 — WhatsApp Conversational State

**Classification:** `DESIGN`

**Priority:** `HIGH`

Need a robust state model for:

```text
report item
upload media
choose item
submit claim
answer challenge
confirm handover
```

without channel-specific business logic.

---

# 90. GAP-054 — WhatsApp Identity Linking

**Classification:** `SECURITY / PRODUCT`

**Priority:** `HIGH`

Questions:

```text
Does phone number map directly to account?

Can one account have multiple numbers?

How is number ownership verified?

How are shared phones handled?

What about institutional numbers?
```

---

# 91. GAP-055 — SMS Fallback

**Classification:** `PRODUCT / INFRASTRUCTURE`

**Priority:** `MEDIUM`

Need policy for low-bandwidth users without smartphone data.

---

# 92. GAP-056 — Push Provider Strategy

**Classification:** `INFRASTRUCTURE`

**Priority:** `MEDIUM`

Potential:

```text
FCM
APNs
web push
```

Need abstraction details.

---

# 93. GAP-057 — Recovery Conversation Moderation

**Classification:** `PRODUCT / SECURITY`

**Priority:** `MEDIUM-HIGH`

Need controls for:

```text
harassment
fraud
social engineering
spam
inappropriate contact
```

---

# 94. GAP-058 — Safe Contact Disclosure

**Classification:** `PRIVACY`

**Priority:** `HIGH`

When should:

```text
phone
email
address
```

be revealed, if ever?

Need:

```text
consent
policy
audit
revocation where possible
```

---

# 95. GAP-059 — Courier Provider Architecture

**Classification:** `INFRASTRUCTURE / PRODUCT`

**Priority:** `HIGH`

Need provider interface and lifecycle.

Potential operations:

```text
quote
book
cancel
pickup confirmation
tracking
delivery proof
dispute
```

---

# 96. GAP-060 — Courier Identity and Custody

**Classification:** `SECURITY / CUSTODY`

**Priority:** `CRITICAL`

Need to establish:

```text
who physically collected item
whether courier agent is authorized
pickup evidence
delivery evidence
```

---

# 97. GAP-061 — Failed Delivery Semantics

**Classification:** `DESIGN`

**Priority:** `HIGH`

Possible states:

```text
delivery attempted
recipient unavailable
wrong recipient
parcel returned
parcel lost
damaged
```

must map into custody correctly.

---

# 98. GAP-062 — Locker Integration

**Classification:** `FUTURE / DESIGN`

**Priority:** `MEDIUM`

Potential recovery through:

```text
smart lockers
pickup cabinets
institution storage lockers
```

requires its own custody adapter.

---

# 99. GAP-063 — Payment Provider Strategy

**Classification:** `INFRASTRUCTURE`

**Priority:** `MEDIUM-HIGH`

Potential markets require different providers.

Architecture must accommodate:

```text
card
mobile money
bank
wallet
```

without provider-specific domain coupling.

---

# 100. GAP-064 — Reward Escrow

**Classification:** `DESIGN / POLICY`

**Priority:** `MEDIUM-HIGH`

If an owner offers a cash reward:

```text
When is money reserved?

When is it released?

Can owner cancel?

What if claim disputed?

What if courier loses item?
```

---

# 101. GAP-065 — Reward Fraud

**Classification:** `SECURITY`

**Priority:** `HIGH`

Potential attacks:

```text
fake finder + fake owner collusion

repeated QR scans

self-recovery

fake report/recovery

duplicate reward claim
```

Need risk controls.

---

# 102. GAP-066 — Non-Monetary Incentive Design

**Classification:** `PRODUCT`

**Priority:** `LOW-MEDIUM`

Questions:

```text
reputation?
badges?
community rankings?
institution recognition?
```

must be designed carefully to avoid perverse incentives.

---

# 103. GAP-067 — Reputation Semantics

**Classification:** `DESIGN`

**Priority:** `MEDIUM`

Reputation must not become a universal truth score.

Need to decide:

```text
what events increase it?
what lowers it?
how does it affect policy?
does it expire?
is it tenant-specific?
```

---

# 104. GAP-068 — Institutional Policy Model

**Classification:** `POLICY / DESIGN`

**Priority:** `CRITICAL`

Different institutions may have different:

```text
retention
claim requirements
handover approvals
document rules
police involvement
reward rules
```

Need a configurable policy architecture.

---

# 105. GAP-069 — Policy Versioning

**Classification:** `DESIGN`

**Priority:** `HIGH`

Historical decisions need the policy version that applied at decision time.

---

# 106. GAP-070 — OPA Adoption

**Classification:** `INFRASTRUCTURE / ADR`

**Priority:** `MEDIUM`

Need dedicated audit/ADR before making OPA a required component.

---

# 107. GAP-071 — Identity Provider

**Classification:** `INFRASTRUCTURE`

**Priority:** `HIGH`

Keycloak-compatible architecture is favored.

Exact solution still requires:

```text
audit
deployment design
realm/tenant mapping
role mapping
MFA
service accounts
```

---

# 108. GAP-072 — Tenant Identity Strategy

**Classification:** `SECURITY / DESIGN`

**Priority:** `HIGH`

Questions:

```text
one realm or multiple?

organization membership claims?

tenant switching?

cross-tenant staff?

federation identity?
```

---

# 109. GAP-073 — Public / Anonymous Users

**Classification:** `SECURITY / PRODUCT`

**Priority:** `HIGH`

Need policy for:

```text
anonymous finder
anonymous report
temporary recovery session
later account linking
```

---

# 110. GAP-074 — Anonymous Abuse Prevention

**Classification:** `SECURITY`

**Priority:** `HIGH`

Need:

```text
rate limiting
risk scoring
captcha/device checks where appropriate
content moderation
```

without making reporting inaccessible.

---

# 111. GAP-075 — Multi-Tenant PostgreSQL Strategy

**Classification:** `DATA / SECURITY`

**Priority:** `HIGH`

Options include:

```text
tenant_id columns

row-level security

schema per tenant

database per large tenant

hybrid
```

Need ADR.

---

# 112. GAP-076 — Tenant Vector Isolation

**Classification:** `SECURITY / DATA`

**Priority:** `CRITICAL`

Vector retrieval must never leak candidates across unauthorized tenant boundaries.

---

# 113. GAP-077 — Tenant Media Isolation

**Classification:** `SECURITY`

**Priority:** `CRITICAL`

Object storage keys, signed URLs, and policies must enforce tenant boundaries.

---

# 114. GAP-078 — Cross-Tenant Federation vs Local Search

**Classification:** `FEDERATION / SECURITY`

**Priority:** `HIGH`

Need clear distinction between:

```text
local multi-tenant search
```

and:

```text
federated remote search
```

---

# 115. GAP-079 — Event Serialization Format

**Classification:** `INFRASTRUCTURE / ADR`

**Priority:** `MEDIUM`

Candidates:

```text
JSON Schema
Avro
Protobuf
```

Need to evaluate:

```text
schema evolution
Java/Python/TS tooling
human readability
registry support
```

---

# 116. GAP-080 — Kafka vs Redpanda

**Classification:** `INFRASTRUCTURE`

**Priority:** `HIGH`

Requires dedicated source/operational audit.

Evaluate:

```text
license
resource use
Kafka compatibility
operational complexity
single-node dev
HA
Kubernetes
self-hosting
```

---

# 117. GAP-081 — NATS

**Classification:** `INFRASTRUCTURE`

**Priority:** `MEDIUM`

Could be compared to Kafka-compatible approaches for lighter deployments, but architecture currently favors durable Kafka semantics.

Needs audit if seriously considered.

---

# 118. GAP-082 — Brokerless Small Deployment

**Classification:** `DEPLOYMENT`

**Priority:** `MEDIUM`

Question:

```text
Should the smallest Dzoka deployment still require Kafka/Redpanda?
```

Potential alternatives:

```text
PostgreSQL outbox worker only
embedded dev profile
broker optional
```

Need packaging decision.

---

# 119. GAP-083 — Object Storage Default

**Classification:** `INFRASTRUCTURE`

**Priority:** `HIGH`

Need audit/ADR for:

```text
MinIO
cloud S3
other S3-compatible provider
```

---

# 120. GAP-084 — Redis Requirement

**Classification:** `INFRASTRUCTURE`

**Priority:** `MEDIUM`

Need to distinguish:

```text
required production dependency
```

from:

```text
optional performance component
```

---

# 121. GAP-085 — Distributed Lock Strategy

**Classification:** `INFRASTRUCTURE / DATA`

**Priority:** `MEDIUM`

Need to determine when:

```text
database locking
Redis lock
Kafka ordering
```

is appropriate.

---

# 122. GAP-086 — MLflow Adoption

**Classification:** `MLOPS`

**Priority:** `MEDIUM`

Need to determine whether MLflow should be:

```text
required
optional
replaced
```

for model registry/experiment tracking.

---

# 123. GAP-087 — Model Serving Strategy

**Classification:** `AI/ML / INFRASTRUCTURE`

**Priority:** `HIGH`

Questions:

```text
models loaded directly in FastAPI?

dedicated serving runtime?

Triton?

ONNX Runtime?

Torch?

Paddle inference server?
```

Likely provider-specific.

---

# 124. GAP-088 — GPU Scheduling

**Classification:** `OPERATIONS`

**Priority:** `MEDIUM-HIGH`

Large deployments need:

```text
GPU worker pools
CPU fallback
batching
queue priority
capacity control
```

---

# 125. GAP-089 — AI Job Priorities

**Classification:** `OPERATIONS`

**Priority:** `MEDIUM`

Examples:

```text
interactive claim verification
> new report enrichment
> historical re-embedding
```

Need scheduling policy.

---

# 126. GAP-090 — AI Cost Governance

**Classification:** `OPERATIONS / AI`

**Priority:** `MEDIUM`

Especially for cloud providers:

```text
per-tenant budgets
rate limits
provider fallback
cost metrics
```

---

# 127. GAP-091 — LLM Provider Strategy

**Classification:** `AI / INFRASTRUCTURE`

**Priority:** `HIGH`

Need a provider model supporting:

```text
local LLM
cloud LLM
tenant-selected provider
no-LLM mode
```

---

# 128. GAP-092 — LLM Evaluation

**Classification:** `AI/ML`

**Priority:** `HIGH`

Tasks may include:

```text
attribute extraction
challenge wording
semantic interpretation
explanation
```

Each needs its own evaluation dataset and failure criteria.

---

# 129. GAP-093 — Prompt Versioning Implementation

**Classification:** `MLOPS`

**Priority:** `MEDIUM`

Prompt versions must be traceable.

Need storage strategy:

```text
code
database
prompt registry
model configuration
```

---

# 130. GAP-094 — Model Security

**Classification:** `SECURITY / AI`

**Priority:** `HIGH`

Need protections against:

```text
malicious model files
supply-chain attacks
unsafe deserialization
tampered weights
```

---

# 131. GAP-095 — Model Artifact Signing

**Classification:** `SECURITY / MLOPS`

**Priority:** `MEDIUM-HIGH`

Especially for edge model distribution.

---

# 132. GAP-096 — Image Moderation

**Classification:** `AI / SECURITY`

**Priority:** `MEDIUM`

Users can upload arbitrary content.

Need policy for:

```text
malware in files
unsafe content
irrelevant images
illegal content
```

without overblocking legitimate recovery evidence.

---

# 133. GAP-097 — Document Upload Security

**Classification:** `SECURITY`

**Priority:** `HIGH`

Need:

```text
MIME validation
magic bytes
malware scanning
safe rendering
PDF handling
metadata stripping
```

---

# 134. GAP-098 — Image Metadata Privacy

**Classification:** `PRIVACY`

**Priority:** `MEDIUM`

EXIF may expose:

```text
GPS
device
capture time
```

Need policy:

```text
strip publicly
retain privately?
```

---

# 135. GAP-099 — Media Authenticity

**Classification:** `SECURITY / RESEARCH`

**Priority:** `MEDIUM`

Questions:

```text
Should evidence capture upload provenance?

Should EXIF be trusted?

Can AI-generated/manipulated images affect ownership verification?

Do we need authenticity checks?
```

---

# 136. GAP-100 — Duplicate Media Detection

**Classification:** `AI / DATA`

**Priority:** `MEDIUM`

Need robust media duplicate/near-duplicate detection for:

```text
fraud
duplicate reports
storage efficiency
```

---

# 137. GAP-101 — Synthetic / Generated Evidence Detection

**Classification:** `AI / SECURITY`

**Priority:** `LOW-MEDIUM initially`

Generative AI may produce fake:

```text
receipts
item photos
damage evidence
```

Need future fraud strategy.

---

# 138. GAP-102 — Fraud Engine Architecture

**Classification:** `SECURITY / AI`

**Priority:** `HIGH`

Current design has `RiskAssessment`.

Detailed fraud engine architecture remains open.

---

# 139. Fraud Categories

Potential:

```text
claim fraud
finder fraud
reward fraud
collusion
identity fraud
report spam
institution abuse
federation abuse
```

---

# 140. GAP-103 — Device / Session Risk Signals

**Classification:** `SECURITY / PRIVACY`

**Priority:** `MEDIUM`

Need balance between:

```text
fraud detection
```

and:

```text
privacy
```

for device/network signals.

---

# 141. GAP-104 — Fraud Feedback Loop

**Classification:** `AI/ML`

**Priority:** `MEDIUM`

Confirmed fraudulent claims can become labels, but bias and wrongful labeling must be controlled.

---

# 142. GAP-105 — Appeals

**Classification:** `PRODUCT / POLICY`

**Priority:** `HIGH`

Need formal appeal flow for:

```text
claim rejected
fraud flag
verification failed
institution denied release
```

---

# 143. GAP-106 — Manual Override Governance

**Classification:** `SECURITY / POLICY`

**Priority:** `HIGH`

Admins may need override.

Every override must capture:

```text
actor
reason
original decision
new decision
policy
timestamp
```

Possibly dual control for sensitive cases.

---

# 144. GAP-107 — Dual-Control Policy

**Classification:** `SECURITY`

**Priority:** `MEDIUM-HIGH`

Need define which items require:

```text
two reviewers
```

Examples:

```text
passport
high-value item
legal hold release
```

---

# 145. GAP-108 — Legal Hold Semantics

**Classification:** `POLICY / DATA`

**Priority:** `HIGH`

Need rules for:

```text
placing hold
renewing
releasing
legal authority
retention extension
blocking recovery
```

---

# 146. GAP-109 — Police / Authority Integration

**Classification:** `PRODUCT / POLICY`

**Priority:** `MEDIUM`

FindBack suggests police roles.

Dzoka should generalize to:

```text
ExternalAuthority
```

but the exact workflow remains open.

---

# 147. GAP-110 — High-Risk Documents

**Classification:** `POLICY / SECURITY`

**Priority:** `HIGH`

Items such as:

```text
passport
national ID
bank card
government credential
```

may require specialized recovery flows.

---

# 148. GAP-111 — Bank Card Handling

**Classification:** `POLICY`

**Priority:** `MEDIUM`

Question:

```text
Should bank cards ever be returned through ordinary claimant workflow?
```

Potentially institution/provider policy may require destruction or bank handoff.

---

# 149. GAP-112 — Keys / Access Cards

**Classification:** `SECURITY / PRODUCT`

**Priority:** `MEDIUM`

Keys may expose:

```text
home
vehicle
office
```

Need privacy-aware handling.

---

# 150. GAP-113 — Medicine / Sensitive Property

**Classification:** `POLICY`

**Priority:** `LOW-MEDIUM`

Certain categories may have health/safety implications and require special policy.

---

# 151. GAP-114 — Weapons / Dangerous Items

**Classification:** `POLICY / SAFETY`

**Priority:** `HIGH`

A lost weapon or hazardous item cannot use the normal recovery flow.

Need category-specific escalation and authority handling.

---

# 152. GAP-115 — Child / Minor Property

**Classification:** `POLICY / ENTITLEMENT`

**Priority:** `MEDIUM`

Need guardian workflows.

---

# 153. GAP-116 — Corporate Assets

**Classification:** `PRODUCT / ENTITLEMENT`

**Priority:** `HIGH`

Need organization asset registry integration and employee/agent collection authority.

---

# 154. GAP-117 — Asset Register Integration

**Classification:** `INTEGRATION`

**Priority:** `MEDIUM`

Potential integrations:

```text
CMDB
ERP
inventory
device management
```

Need provider architecture.

---

# 155. GAP-118 — Digital Item Registration

**Classification:** `PRODUCT`

**Priority:** `MEDIUM`

Need UX and security design for pre-registering assets without encouraging excessive sensitive-data collection.

---

# 156. GAP-119 — DID / VC Item Identity

**Classification:** `FUTURE`

**Priority:** `LOW-MEDIUM`

Still research-only.

Questions:

```text
What actual recovery problem does DID solve better?

Who issues credentials?

How is key recovery handled?

How does this work for ordinary consumers?
```

---

# 157. GAP-120 — EPCIS Mapping

**Classification:** `DESIGN / INTEGRATION`

**Priority:** `MEDIUM`

Need actual mapping specification:

```text
RecoveryEvent
→ EPCIS ObjectEvent / AggregationEvent / TransformationEvent?
```

depending on semantics.

---

# 158. GAP-121 — GS1 Identifier Strategy

**Classification:** `STANDARDS`

**Priority:** `LOW-MEDIUM`

Not all lost items possess GS1 identifiers.

Need optional support only.

---

# 159. GAP-122 — RecoveryEvent Schema

**Classification:** `DESIGN`

**Priority:** `HIGH`

Native Dzoka `RecoveryEvent` needs final schema and event types.

---

# 160. GAP-123 — Event Schema Format

**Classification:** `INFRASTRUCTURE`

**Priority:** `MEDIUM`

Still open:

```text
JSON Schema
Avro
Protobuf
```

---

# 161. GAP-124 — Event Schema Registry

**Classification:** `INFRASTRUCTURE`

**Priority:** `MEDIUM`

Need decide:

```text
Confluent-compatible registry?
Apicurio?
repository-based schemas?
```

depending broker choice.

---

# 162. GAP-125 — Event Replay Governance

**Classification:** `OPERATIONS`

**Priority:** `HIGH`

Replay can cause:

```text
duplicate notifications
duplicate reward releases
wrong custody changes
```

Need replay policy and safe consumer modes.

---

# 163. GAP-126 — DLQ Operations

**Classification:** `OPERATIONS`

**Priority:** `MEDIUM`

Need:

```text
inspection
repair
retry
quarantine
audit
```

workflow.

---

# 164. GAP-127 — Outbox Cleanup

**Classification:** `OPERATIONS`

**Priority:** `LOW-MEDIUM`

Need retention/archival policy for published outbox rows.

---

# 165. GAP-128 — Process Manager Persistence

**Classification:** `DESIGN`

**Priority:** `HIGH`

Need design for durable orchestration of:

```text
fingerprint build
verification
recovery
courier
federation
```

---

# 166. GAP-129 — Saga Compensation Rules

**Classification:** `DESIGN`

**Priority:** `HIGH`

Examples:

```text
courier booking succeeds
payment fails

reward reserved
recovery cancelled

federated transfer started
receiving node unavailable
```

Need compensation policy.

---

# 167. GAP-130 — Idempotency Storage

**Classification:** `DATA`

**Priority:** `MEDIUM`

Need decide where/how long to keep:

```text
API idempotency keys
webhook IDs
event inbox IDs
```

---

# 168. GAP-131 — API Versioning

**Classification:** `ARCHITECTURE`

**Priority:** `MEDIUM`

Need strategy for:

```text
public REST APIs
federation APIs
webhooks
SDKs
```

---

# 169. GAP-132 — API Schema Style

**Classification:** `DESIGN`

**Priority:** `MEDIUM`

Need decide:

```text
OpenAPI-first?
code-first?
```

and compatibility requirements.

---

# 170. GAP-133 — GraphQL

**Classification:** `ARCHITECTURE`

**Priority:** `LOW`

No strong reason currently established.

Should not be added without a clear use case.

---

# 171. GAP-134 — External Developer Access

**Classification:** `PRODUCT / SECURITY`

**Priority:** `MEDIUM`

Need:

```text
API clients
scopes
developer keys/OAuth
rate limits
sandbox
```

---

# 172. GAP-135 — Webhook Subscription Platform

**Classification:** `INTEGRATION`

**Priority:** `MEDIUM`

Institutions may want:

```text
match notifications
custody updates
recovery completion
```

via webhook.

Need subscription/security design.

---

# 173. GAP-136 — PWA Offline Model

**Classification:** `PRODUCT / DATA`

**Priority:** `MEDIUM`

Need determine what can be safely stored offline:

```text
draft reports
media queue
public matches
```

but not:

```text
secret evidence
sensitive proof
```

without strong protection.

---

# 174. GAP-137 — Mobile App Strategy

**Classification:** `PRODUCT`

**Priority:** `MEDIUM`

Architecture includes Web/PWA as baseline.

Need determine when native/Flutter/React Native app is justified.

---

# 175. GAP-138 — Kiosk Security

**Classification:** `SECURITY / PRODUCT`

**Priority:** `MEDIUM`

Shared public kiosks require:

```text
session reset
no residual PII
temporary uploads
privacy screen
limited permissions
```

---

# 176. GAP-139 — Accessibility Standard

**Classification:** `PRODUCT`

**Priority:** `MEDIUM`

Need decide target compliance level, likely aligned to modern WCAG standards.

---

# 177. GAP-140 — Media Compression Policy

**Classification:** `PRODUCT / INFRASTRUCTURE`

**Priority:** `MEDIUM`

Need balance:

```text
low bandwidth
```

against:

```text
visual matching detail
OCR accuracy
```

---

# 178. GAP-141 — Image Quality Scoring

**Classification:** `AI/ML`

**Priority:** `MEDIUM`

Need automatically identify:

```text
blur
underexposure
tiny object
occlusion
bad crop
```

and request better capture where useful.

---

# 179. GAP-142 — Best-Photo Selection

**Classification:** `AI/ML`

**Priority:** `MEDIUM`

For many images, select strongest candidates for:

```text
retrieval
OCR
public display
```

without discarding other evidence.

---

# 180. GAP-143 — Multi-Image Fusion

**Classification:** `AI/ML`

**Priority:** `HIGH`

How should:

```text
5 photos of one item
```

be represented?

Options:

```text
separate embeddings
pooled embedding
attention-based fusion
best-view selection
multi-vector retrieval
```

---

# 181. GAP-144 — Multi-Image Candidate Comparison

**Classification:** `AI/ML`

**Priority:** `HIGH`

Need efficient:

```text
N source images × M candidate images
```

comparison strategy.

---

# 182. GAP-145 — Object Crop Selection

**Classification:** `AI/ML`

**Priority:** `HIGH`

Photos may contain:

```text
item
person
table
bag
background objects
```

Need robust item-region selection.

---

# 183. GAP-146 — Segmentation

**Classification:** `AI/ML`

**Priority:** `MEDIUM`

Should Dzoka add:

```text
SAM-like segmentation
```

or equivalent for object isolation?

Needs benchmark against simpler detection crops.

---

# 184. GAP-147 — Product Recognition

**Classification:** `AI/ML`

**Priority:** `MEDIUM`

Recognizing:

```text
iPhone 15 Pro
Nike Air Max...
```

could improve matching, but model/provider remains open.

---

# 185. GAP-148 — Fine-Grained Attribute Extraction

**Classification:** `AI/ML`

**Priority:** `HIGH`

Need reliable extraction of:

```text
colour
material
brand
model
shape
size
condition
distinctive marks
```

from text/media.

---

# 186. GAP-149 — Attribute Ontology

**Classification:** `DATA / DOMAIN`

**Priority:** `HIGH`

Need canonical category/attribute ontology.

Questions:

```text
How many categories?

Who maintains them?

Can tenants extend them?

How are translations handled?

How are categories versioned?
```

---

# 187. GAP-150 — Category Mapping

**Classification:** `DATA / AI`

**Priority:** `HIGH`

Need map user descriptions such as:

```text
phone
smartphone
mobile
iPhone
electronics
```

without destructive hard gates.

---

# 188. GAP-151 — Tenant-Specific Taxonomy Extensions

**Classification:** `MULTI-TENANCY`

**Priority:** `MEDIUM`

Airports and universities may have different useful categories.

Need extension model without breaking federation.

---

# 189. GAP-152 — Federation Taxonomy Mapping

**Classification:** `FEDERATION`

**Priority:** `HIGH`

Different nodes may classify the same item differently.

Need canonical/fuzzy taxonomy mapping across nodes.

---

# 190. GAP-153 — Identifier Type Registry

**Classification:** `DATA`

**Priority:** `MEDIUM`

Need maintain types:

```text
IMEI
serial
VIN?
passport
national ID
asset tag
custom organization ID
```

with validation and privacy rules.

---

# 191. GAP-154 — Identifier Verification

**Classification:** `SECURITY / INTEGRATION`

**Priority:** `MEDIUM`

Should some identifiers be verified through external registries?

Example:

```text
institution inventory
device MDM
vehicle registry
```

No universal design exists.

---

# 192. GAP-155 — Search Query Understanding

**Classification:** `AI/ML`

**Priority:** `MEDIUM`

Users may search conversationally:

```text
black bag I left in kombi yesterday
```

Need parse:

```text
item
colour
transport context
time
```

without requiring structured forms.

---

# 193. GAP-156 — Search Personalization

**Classification:** `PRODUCT`

**Priority:** `LOW`

Likely unnecessary initially.

Should not compromise objective matching or privacy.

---

# 194. GAP-157 — Duplicate Report Detection

**Classification:** `MATCHING`

**Priority:** `HIGH`

Need distinguish:

```text
same user reposting same loss
```

from:

```text
multiple legitimate reports about same physical item
```

---

# 195. GAP-158 — Report Merge Policy

**Classification:** `DOMAIN`

**Priority:** `MEDIUM`

If duplicate reports exist:

```text
merge?
link?
retain all?
```

Need audit-friendly semantics.

---

# 196. GAP-159 — Item Identity Resolution

**Classification:** `DOMAIN / MATCHING`

**Priority:** `HIGH`

When do multiple reports become linked to one canonical:

```text
ItemRecord
```

?

Need threshold/human decision process.

---

# 197. GAP-160 — Item Record Split

**Classification:** `DOMAIN`

**Priority:** `MEDIUM`

If reports incorrectly merged, Dzoka needs safe:

```text
split canonical item
```

operation preserving history.

---

# 198. GAP-161 — Re-Matching After Fingerprint Changes

**Classification:** `MATCHING`

**Priority:** `HIGH`

New evidence may require candidate recalculation.

Need incremental strategy:

```text
new image
new OCR
new serial
updated time/location
```

---

# 199. GAP-162 — Candidate Staleness

**Classification:** `MATCHING`

**Priority:** `MEDIUM`

When should old matches become:

```text
STALE
```

after:

```text
model changes
fingerprint changes
item resolved
```

---

# 200. GAP-163 — Matching Run Cost Controls

**Classification:** `OPERATIONS`

**Priority:** `MEDIUM`

Need quotas/budgets for expensive:

```text
federation
local reranking
LLM
CCTV
```

---

# 201. GAP-164 — Real-Time vs Batch Matching

**Classification:** `MATCHING`

**Priority:** `MEDIUM`

Need strategy combining:

```text
interactive matching
background exhaustive matching
periodic rematching
```

---

# 202. GAP-165 — Large Corpus Scaling

**Classification:** `PERFORMANCE`

**Priority:** `MEDIUM-HIGH`

Need validate architecture at:

```text
10k
100k
1m
10m+
```

item observations.

---

# 203. GAP-166 — pgvector Scaling Threshold

**Classification:** `PERFORMANCE`

**Priority:** `MEDIUM`

Need measured criteria for switching/adding Qdrant.

Not:

```text
because Qdrant is more specialized
```

but based on benchmark evidence.

---

# 204. GAP-167 — PostGIS Scale Testing

**Classification:** `PERFORMANCE`

**Priority:** `MEDIUM`

Need benchmark spatial candidate generation under realistic load.

---

# 205. GAP-168 — Full-Text Search Scaling

**Classification:** `PERFORMANCE`

**Priority:** `LOW-MEDIUM`

Need determine whether PostgreSQL FTS remains enough or if a dedicated search engine becomes justified.

---

# 206. GAP-169 — Dedicated Search Engine

**Classification:** `INFRASTRUCTURE`

**Priority:** `LOW`

No current evidence requires Elasticsearch/OpenSearch.

Keep as future option only.

---

# 207. GAP-170 — Backup Architecture

**Classification:** `OPERATIONS`

**Priority:** `HIGH`

Need finalized procedures for:

```text
PostgreSQL
MinIO/S3
configuration
federation keys
model metadata
```

---

# 208. GAP-171 — Point-in-Time Recovery

**Classification:** `OPERATIONS`

**Priority:** `HIGH`

Need support for database PITR in serious deployments.

---

# 209. GAP-172 — Object Storage Versioning

**Classification:** `OPERATIONS`

**Priority:** `MEDIUM`

Need decide retention/versioning strategy for:

```text
proof
custody evidence
public media
```

---

# 210. GAP-173 — Disaster Recovery Objectives

**Classification:** `OPERATIONS`

**Priority:** `HIGH`

Need define:

```text
RPO
RTO
```

for deployment profiles.

---

# 211. GAP-174 — Federation Key Backup

**Classification:** `SECURITY / OPERATIONS`

**Priority:** `CRITICAL`

Node identity keys require:

```text
secure backup
rotation
revocation
disaster recovery
```

---

# 212. GAP-175 — Audit Integrity

**Classification:** `SECURITY`

**Priority:** `HIGH`

Need decide whether append-only database audit is sufficient or whether stronger:

```text
hash chaining
signed audit segments
WORM storage
```

is justified for high-security deployments.

---

# 213. GAP-176 — Tamper-Evident Recovery History

**Classification:** `SECURITY / RESEARCH`

**Priority:** `MEDIUM`

Could use:

```text
hash chains
periodic signed roots
```

without blockchain.

Needs threat model first.

---

# 214. GAP-177 — Clock Synchronization

**Classification:** `OPERATIONS`

**Priority:** `MEDIUM`

Federation, CCTV, custody, and signatures depend on reasonably synchronized time.

Need policy for:

```text
NTP
clock skew
offline devices
occurredAt vs recordedAt
```

---

# 215. GAP-178 — Backdated Events

**Classification:** `DOMAIN / SECURITY`

**Priority:** `HIGH`

Offline and institutional workflows may record events after the fact.

Need strict rules for:

```text
who may backdate
how much
evidence
audit
```

---

# 216. GAP-179 — Data Residency

**Classification:** `PRIVACY / FEDERATION`

**Priority:** `HIGH`

Self-hosted institutions may require:

```text
data stays in country
images never leave node
```

Need federation architecture capable of this.

---

# 217. GAP-180 — Data Sovereignty Policy

**Classification:** `POLICY`

**Priority:** `HIGH`

Need tenant/node settings for:

```text
allowed regions
external providers
model providers
federation data classes
```

---

# 218. GAP-181 — AI Provider Residency

**Classification:** `PRIVACY`

**Priority:** `HIGH`

External AI may violate institutional data residency.

Need:

```text
provider locality policy
local-model fallback
```

---

# 219. GAP-182 — Cross-Border Federation

**Classification:** `LEGAL/POLICY`

**Priority:** `MEDIUM-HIGH`

Need privacy/legal review before federating personal data internationally.

---

# 220. GAP-183 — Consent

**Classification:** `PRIVACY`

**Priority:** `HIGH`

Need define where consent is required for:

```text
contact sharing
CCTV
biometrics
location
marketing/rewards
external AI providers
```

---

# 221. GAP-184 — Legitimate Purpose / Institutional Authority

**Classification:** `LEGAL/POLICY`

**Priority:** `HIGH`

Some processing may rely on institutional authority/policy rather than user consent.

Must be deployment-specific.

---

# 222. GAP-185 — Privacy Export / Access Request

**Classification:** `PRIVACY`

**Priority:** `MEDIUM`

Need mechanism for users to access their personal data where applicable.

---

# 223. GAP-186 — Right-to-Delete Semantics

**Classification:** `PRIVACY`

**Priority:** `HIGH`

Deletion is complicated by:

```text
audit
legal holds
claim disputes
federated copies
ML datasets
```

Need precise policy.

---

# 224. GAP-187 — ML Dataset Eligibility

**Classification:** `PRIVACY / MLOPS`

**Priority:** `CRITICAL`

Not every recovery record should automatically become training data.

Need:

```text
consent/policy
anonymization
dataset eligibility
retention
```

---

# 225. GAP-188 — Training on User Images

**Classification:** `PRIVACY / LEGAL`

**Priority:** `CRITICAL`

Need explicit governance before training Dzoka models on user-submitted media.

---

# 226. GAP-189 — Synthetic Training Data

**Classification:** `AI/ML`

**Priority:** `MEDIUM`

Synthetic data may help bootstrap but must not replace real-world validation.

---

# 227. GAP-190 — Benchmark Dataset Governance

**Classification:** `AI/ML / LEGAL`

**Priority:** `HIGH`

Need versioned dataset cards describing:

```text
source
license
consent
splits
bias
categories
hard negatives
```

---

# 228. GAP-191 — Evaluation Metrics

**Classification:** `AI/ML`

**Priority:** `HIGH`

Need final metric suite.

Candidate retrieval:

```text
Recall@K
MRR
nDCG
```

Matching:

```text
precision
recall
F1
ROC-AUC where appropriate
PR-AUC
false match rate
false non-match rate
```

Calibration:

```text
ECE
Brier score
calibration curves
```

Recovery:

```text
time to recovery
successful recovery rate
false release rate
```

---

# 229. GAP-192 — Safety Metric: False Release Rate

**Classification:** `PRODUCT / SAFETY`

**Priority:** `CRITICAL`

Dzoka needs a business safety metric:

```text
wrong item released to wrong claimant
```

This may be more important than raw model accuracy.

---

# 230. GAP-193 — Recovery Latency Metric

**Classification:** `PRODUCT`

**Priority:** `HIGH`

Measure:

```text
report → first candidate
report → verified match
match → claim
claim → recovery
```

---

# 231. GAP-194 — Federation Effectiveness Metrics

**Classification:** `PRODUCT / FEDERATION`

**Priority:** `MEDIUM`

Measure:

```text
matches discovered only through federation
remote-node latency
remote candidate quality
cross-node recovery rate
```

---

# 232. GAP-195 — CCTV Effectiveness Metrics

**Classification:** `AI/ML`

**Priority:** `MEDIUM`

Measure:

```text
disappearance localization accuracy
search time reduction
false event rate
operator review time
```

---

# 233. GAP-196 — Ownership Challenge Effectiveness Metrics

**Classification:** `SECURITY / RESEARCH`

**Priority:** `HIGH`

Measure:

```text
legitimate claimant pass rate
fraudulent claimant pass rate
challenge abandonment
information leakage
human review rate
```

---

# 234. GAP-197 — User Trust UX

**Classification:** `PRODUCT`

**Priority:** `MEDIUM`

Need clearly explain:

```text
why candidate suggested
what AI means
what verification is doing
what data is private
```

without overwhelming users.

---

# 235. GAP-198 — Explainability UX

**Classification:** `PRODUCT / AI`

**Priority:** `HIGH`

Need separate explanations for:

```text
candidate retrieval
match evidence
conflicts
verification decision
risk review
```

---

# 236. GAP-199 — Avoiding Overconfidence in UI

**Classification:** `PRODUCT`

**Priority:** `HIGH`

Need language that avoids:

```text
"92% yours"
```

when actual meaning is a ranking score.

---

# 237. GAP-200 — Human Reviewer Tooling

**Classification:** `PRODUCT`

**Priority:** `HIGH`

Reviewers need:

```text
side-by-side images
local feature evidence
OCR comparison
conflicts
private challenge responses
risk indicators
audit history
```

---

# 238. GAP-201 — Reviewer Cognitive Load

**Classification:** `PRODUCT / RESEARCH`

**Priority:** `MEDIUM`

Too much evidence may reduce decision quality.

Need UI evaluation.

---

# 239. GAP-202 — Human Override Bias

**Classification:** `AI/ML / PRODUCT`

**Priority:** `MEDIUM`

If AI shows:

```text
95%
```

first, reviewers may anchor on it.

Need careful decision UX.

---

# 240. GAP-203 — Blind Review Modes

**Classification:** `RESEARCH`

**Priority:** `LOW-MEDIUM`

Potentially hide AI score initially in high-risk cases to reduce automation bias.

Requires testing.

---

# 241. GAP-204 — Model Bias Across Item Categories

**Classification:** `AI/ML`

**Priority:** `HIGH`

Visual models may perform differently on:

```text
phones
bags
keys
documents
clothing
jewelry
```

Need category-level evaluation.

---

# 242. GAP-205 — Model Bias Across Regions

**Classification:** `AI/ML`

**Priority:** `MEDIUM-HIGH`

Imported model datasets may underrepresent:

```text
African product brands
local IDs
local transport
local languages
```

---

# 243. GAP-206 — Local Brand Dictionary

**Classification:** `DATA`

**Priority:** `MEDIUM`

Need local product/brand taxonomy without endless hard-coded lists.

---

# 244. GAP-207 — OCR for Local IDs

**Classification:** `AI/ML / PRIVACY`

**Priority:** `HIGH`

Need evaluate OCR extraction quality for regional documents while ensuring redaction/privacy.

---

# 245. GAP-208 — Document-Specific Recovery

**Classification:** `PRODUCT`

**Priority:** `HIGH`

Passports/IDs may require different flows from generic objects.

Need dedicated category policies.

---

# 246. GAP-209 — Cash / Wallet Contents

**Classification:** `VERIFICATION / PRODUCT`

**Priority:** `MEDIUM`

Wallet verification can use hidden contents but can also expose sensitive financial data.

Needs safe challenge design.

---

# 247. GAP-210 — Key Recovery Verification

**Classification:** `SECURITY`

**Priority:** `MEDIUM`

Keys often lack unique identifying attributes.

Need stronger contextual verification.

---

# 248. GAP-211 — Clothing / Generic Objects

**Classification:** `MATCHING`

**Priority:** `HIGH`

Generic objects may have weak identity evidence.

Need honest low-confidence handling rather than forced matches.

---

# 249. GAP-212 — Unmatchable Items

**Classification:** `PRODUCT`

**Priority:** `HIGH`

Dzoka must support:

```text
insufficient evidence
```

as a legitimate outcome.

The system should not manufacture a match.

---

# 250. GAP-213 — No-Owner Recovery

**Classification:** `PRODUCT / POLICY`

**Priority:** `MEDIUM`

What happens when an item is never claimed?

Need:

```text
retention
donation
disposal
authority transfer
```

rules.

---

# 251. GAP-214 — Abandoned Claims

**Classification:** `PRODUCT`

**Priority:** `MEDIUM`

Need expiry and reactivation rules.

---

# 252. GAP-215 — Long-Term Unresolved Cases

**Classification:** `OPERATIONS`

**Priority:** `MEDIUM`

Need archival and rematching strategy.

---

# 253. GAP-216 — Automatic Re-Matching

**Classification:** `MATCHING`

**Priority:** `MEDIUM`

Should old unresolved items be periodically re-run when:

```text
new models
new data
new federation peers
```

become available?

Likely yes, with cost controls.

---

# 254. GAP-217 — Search Freshness

**Classification:** `MATCHING`

**Priority:** `MEDIUM`

Need define time horizons by category without destructive global 90/365-day rules.

---

# 255. GAP-218 — Archive Search

**Classification:** `PRODUCT`

**Priority:** `LOW-MEDIUM`

Should archived items remain searchable?

Likely policy/category dependent.

---

# 256. GAP-219 — Legal Evidence Preservation

**Classification:** `POLICY`

**Priority:** `HIGH`

If a case becomes part of:

```text
police investigation
insurance claim
legal dispute
```

evidence preservation rules change.

---

# 257. GAP-220 — Insurance Integration

**Classification:** `FUTURE / INTEGRATION`

**Priority:** `LOW`

Potential future API for insurers.

No immediate requirement.

---

# 258. GAP-221 — Manufacturer Integration

**Classification:** `FUTURE`

**Priority:** `LOW`

Could allow registered product identities.

Needs ecosystem adoption.

---

# 259. GAP-222 — Airline / Transport Integrations

**Classification:** `INTEGRATION`

**Priority:** `MEDIUM-HIGH`

Potentially high-value Dzoka use case.

Need source audits of:

```text
airline baggage systems
transport lost property APIs
```

where available.

---

# 260. GAP-223 — Airport Deployment Model

**Classification:** `PRODUCT / ARCHITECTURE`

**Priority:** `MEDIUM`

Need specialized:

```text
terminal zones
security boundaries
baggage context
staff roles
federation
```

profile.

---

# 261. GAP-224 — University Deployment Model

**Classification:** `PRODUCT`

**Priority:** `MEDIUM`

Potentially useful initial institutional profile.

---

# 262. GAP-225 — Public City Deployment

**Classification:** `PRODUCT`

**Priority:** `MEDIUM`

Need broader public/community moderation and abuse controls than institutional deployments.

---

# 263. GAP-226 — National Federation Governance

**Classification:** `GOVERNANCE / FEDERATION`

**Priority:** `FUTURE-HIGH`

If Dzoka becomes national infrastructure:

```text
Who operates root directory?

Who certifies nodes?

Who sets protocol policy?

Who resolves node disputes?
```

---

# 264. GAP-227 — Federation Governance Without Central Monopolization

**Classification:** `GOVERNANCE`

**Priority:** `FUTURE`

Need balance:

```text
interoperability
trust
decentralized operation
```

---

# 265. GAP-228 — Node Certification

**Classification:** `GOVERNANCE / SECURITY`

**Priority:** `MEDIUM`

Potential levels:

```text
community
verified organization
regulated institution
government
```

Need clear semantics.

---

# 266. GAP-229 — Node Revocation

**Classification:** `FEDERATION`

**Priority:** `HIGH`

Need mechanisms to revoke compromised/malicious nodes.

---

# 267. GAP-230 — Protocol Version Negotiation

**Classification:** `FEDERATION`

**Priority:** `HIGH`

Nodes may run different Dzoka versions.

Need:

```text
protocol version
supported schemas
capabilities
fallback
```

---

# 268. GAP-231 — Federated Schema Evolution

**Classification:** `FEDERATION`

**Priority:** `HIGH`

Must preserve interoperability through upgrades.

---

# 269. GAP-232 — Federated Query Budget

**Classification:** `OPERATIONS`

**Priority:** `MEDIUM`

Avoid querying:

```text
every peer
for every report
forever
```

Need:

```text
peer selection
query budget
retry schedule
```

---

# 270. GAP-233 — Federation Search Routing

**Classification:** `ALGORITHM`

**Priority:** `MEDIUM`

Could route queries based on:

```text
geography
institution type
transport routes
capabilities
historical movement
```

rather than fanout to every peer.

---

# 271. GAP-234 — Federated Movement Intelligence

**Classification:** `RESEARCH`

**Priority:** `FUTURE`

Cross-node item movement may improve search routing.

Needs strong privacy controls.

---

# 272. GAP-235 — Federation Reputation

**Classification:** `SECURITY`

**Priority:** `FUTURE`

Should peer reliability affect candidate handling?

Requires caution to avoid opaque trust scoring.

---

# 273. GAP-236 — Emergency Trust Bootstrap

**Classification:** `FEDERATION`

**Priority:** `LOW-MEDIUM`

How does a new institution join quickly without weakening trust?

---

# 274. GAP-237 — Source Audit — faster-whisper

**Classification:** `SOURCE AUDIT`

**Priority:** `MEDIUM`

Required before STT lock-in.

---

# 275. GAP-238 — Source Audit — Kafka

**Classification:** `SOURCE AUDIT`

**Priority:** `HIGH`

Need deployment/resource/license review.

---

# 276. GAP-239 — Source Audit — Redpanda

**Classification:** `SOURCE AUDIT`

**Priority:** `HIGH`

Compare against Kafka.

---

# 277. GAP-240 — Source Audit — NATS

**Classification:** `SOURCE AUDIT`

**Priority:** `MEDIUM`

Only if broker-light architecture remains under consideration.

---

# 278. GAP-241 — Source Audit — Keycloak

**Classification:** `SOURCE AUDIT`

**Priority:** `HIGH`

Need evaluate:

```text
multi-tenancy
OIDC
service accounts
MFA
deployment
```

---

# 279. GAP-242 — Source Audit — OPA

**Classification:** `SOURCE AUDIT`

**Priority:** `MEDIUM`

Need evaluate policy complexity vs operational cost.

---

# 280. GAP-243 — Source Audit — MinIO

**Classification:** `SOURCE AUDIT`

**Priority:** `HIGH`

Need verify licensing/current distribution/deployment model before default adoption.

---

# 281. GAP-244 — Source Audit — Redis

**Classification:** `SOURCE AUDIT`

**Priority:** `HIGH`

Current Redis licensing/product landscape should be explicitly reviewed before final default selection.

Potential alternative compatibility must also be considered.

---

# 282. GAP-245 — Source Audit — MLflow

**Classification:** `SOURCE AUDIT`

**Priority:** `MEDIUM`

---

# 283. GAP-246 — Source Audit — OpenTelemetry Stack

**Classification:** `SOURCE AUDIT`

**Priority:** `MEDIUM`

Projects:

```text
OpenTelemetry
Prometheus
Grafana
Loki
Tempo
```

require final dependency/deployment review.

---

# 284. GAP-247 — Source Audit — OSRM

**Classification:** `SOURCE AUDIT`

**Priority:** `MEDIUM`

---

# 285. GAP-248 — Source Audit — Valhalla

**Classification:** `SOURCE AUDIT`

**Priority:** `MEDIUM`

Compare routing suitability.

---

# 286. GAP-249 — Source Audit — ONVIF Tooling

**Classification:** `SOURCE AUDIT`

**Priority:** `HIGH`

---

# 287. GAP-250 — Source Audit — WhatsApp Integration Libraries

**Classification:** `SOURCE AUDIT`

**Priority:** `MEDIUM`

Prefer official Cloud API patterns where possible.

---

# 288. GAP-251 — Source Audit — Map Components

**Classification:** `SOURCE AUDIT`

**Priority:** `LOW-MEDIUM`

Need final selection for:

```text
web map
mobile map
geocoder
tile provider
```

---

# 289. GAP-252 — Source Audit — Malware Scanning

**Classification:** `SOURCE AUDIT`

**Priority:** `MEDIUM`

Potential:

```text
ClamAV
other scanning service
```

---

# 290. GAP-253 — Source Audit — Secret Manager

**Classification:** `SOURCE AUDIT`

**Priority:** `MEDIUM`

Self-hosted and Kubernetes deployments need clear secret management options.

---

# 291. GAP-254 — Source Audit — Backup Tooling

**Classification:** `SOURCE AUDIT`

**Priority:** `MEDIUM`

Evaluate:

```text
PostgreSQL backup
S3 backup
Kubernetes backup
```

components.

---

# 292. GAP-255 — Deployment Packaging

**Classification:** `OPERATIONS`

**Priority:** `HIGH`

Need production package set:

```text
Docker Compose
Helm
environment templates
migration jobs
initial bootstrap
```

---

# 293. GAP-256 — First-Run Bootstrap

**Classification:** `OPERATIONS / SECURITY`

**Priority:** `HIGH`

Need secure setup for:

```text
first admin
tenant
keys
object storage
broker
identity provider
```

No known default passwords.

---

# 294. GAP-257 — Federation Bootstrap

**Classification:** `OPERATIONS`

**Priority:** `HIGH`

Need explicit ceremony/process for first peer.

---

# 295. GAP-258 — Configuration Management

**Classification:** `OPERATIONS`

**Priority:** `HIGH`

Need versioned config for:

```text
matching profiles
policies
providers
federation peers
retention
```

---

# 296. GAP-259 — Configuration Audit

**Classification:** `SECURITY`

**Priority:** `HIGH`

Changes to high-impact configuration must be audited.

---

# 297. GAP-260 — Feature Flags

**Classification:** `OPERATIONS`

**Priority:** `MEDIUM`

Useful for:

```text
new model
new matching feature
federation capability
```

but flags must not bypass security invariants.

---

# 298. GAP-261 — Shadow Model Deployment

**Classification:** `MLOPS`

**Priority:** `MEDIUM`

Need support for:

```text
new model runs
but does not affect user decisions
```

for evaluation.

---

# 299. GAP-262 — A/B Testing Safety

**Classification:** `MLOPS / PRODUCT`

**Priority:** `MEDIUM`

High-risk decisions such as ownership/release should not be casually A/B tested.

---

# 300. GAP-263 — Model Rollback

**Classification:** `MLOPS`

**Priority:** `HIGH`

Need quick rollback after degraded matching performance.

---

# 301. GAP-264 — Re-Embedding Strategy

**Classification:** `MLOPS`

**Priority:** `HIGH`

Need:

```text
batch size
priority
model coexistence
storage cost
cutover
rollback
```

---

# 302. GAP-265 — Re-OCR Strategy

**Classification:** `MLOPS`

**Priority:** `MEDIUM`

As OCR improves, historical evidence may be regenerated.

Need lineage/versioning.

---

# 303. GAP-266 — Migration Across Vector Providers

**Classification:** `OPERATIONS`

**Priority:** `LOW-MEDIUM`

If moving:

```text
pgvector → Qdrant
```

need dual-write/backfill/cutover design.

---

# 304. GAP-267 — Matching Config Rollout

**Classification:** `MLOPS`

**Priority:** `MEDIUM`

New weights/profiles need:

```text
validation
versioning
shadow comparison
rollback
```

---

# 305. GAP-268 — Category Profile Governance

**Classification:** `MLOPS / DOMAIN`

**Priority:** `MEDIUM`

Who may change matching weights for:

```text
passport
phone
bag
```

?

Need controlled governance.

---

# 306. GAP-269 — Human Decision Feedback Quality

**Classification:** `AI/ML`

**Priority:** `MEDIUM`

Human feedback may itself be incorrect.

Training labels need:

```text
confidence
review status
provenance
```

---

# 307. GAP-270 — Ground Truth Definition

**Classification:** `RESEARCH`

**Priority:** `CRITICAL`

What constitutes true confirmation that two reports are the same physical item?

Strong labels may require:

```text
successful verified recovery
matching serial
custody evidence
```

not simply reviewer opinion.

---

# 308. GAP-271 — Negative Ground Truth

**Classification:** `RESEARCH`

**Priority:** `HIGH`

How do we know two similar items are definitely different?

Could use:

```text
different verified serial
different recovered owners
controlled dataset
```

---

# 309. GAP-272 — Evaluation Leakage

**Classification:** `AI/ML`

**Priority:** `HIGH`

Need prevent:

```text
same image
same report
same object
```

appearing in train and test in misleading ways.

---

# 310. GAP-273 — Temporal Evaluation

**Classification:** `AI/ML`

**Priority:** `MEDIUM`

Models should be evaluated against future data, not only random splits.

---

# 311. GAP-274 — Federation Evaluation Dataset

**Classification:** `RESEARCH`

**Priority:** `MEDIUM`

Need simulated or real multi-node scenarios to test:

```text
score incomparability
peer failures
late candidates
privacy
```

---

# 312. GAP-275 — End-to-End Recovery Simulation

**Classification:** `TESTING`

**Priority:** `HIGH`

Need test environment simulating:

```text
loss
finding
matching
claim
verification
handover
courier
recovery
reward
```

---

# 313. GAP-276 — Chaos Testing

**Classification:** `OPERATIONS`

**Priority:** `MEDIUM`

Inject failures:

```text
broker down
AI down
federation peer down
storage down
courier timeout
duplicate event
```

---

# 314. GAP-277 — Security Penetration Testing

**Classification:** `SECURITY`

**Priority:** `HIGH`

Before production:

```text
API auth
tenant boundaries
media access
handover tokens
federation signatures
claim authorization
```

need dedicated testing.

---

# 315. GAP-278 — Threat Model

**Classification:** `SECURITY`

**Priority:** `CRITICAL`

A formal Dzoka threat model remains required.

Actors include:

```text
fraudulent claimant
malicious finder
malicious employee
compromised node
malicious federation peer
attacker scraping listings
stolen recovery token
insider administrator
```

---

# 316. GAP-279 — STRIDE / Abuse Cases

**Classification:** `SECURITY`

**Priority:** `HIGH`

Threat analysis should cover each bounded context.

---

# 317. GAP-280 — Insider Threat

**Classification:** `SECURITY`

**Priority:** `HIGH`

Institution staff may abuse access to:

```text
secret evidence
contact details
custody
CCTV
```

Need least privilege and audit.

---

# 318. GAP-281 — Privileged Access Management

**Classification:** `SECURITY`

**Priority:** `MEDIUM-HIGH`

Need stronger controls for:

```text
super admins
security admins
database admins
```

in large deployments.

---

# 319. GAP-282 — Break-Glass Access

**Classification:** `SECURITY`

**Priority:** `MEDIUM`

Emergency access should be:

```text
time-limited
reason-required
audited
```

---

# 320. GAP-283 — Secret Evidence Encryption Key Model

**Classification:** `SECURITY`

**Priority:** `CRITICAL`

Need decide:

```text
application-level encryption
KMS
per-tenant keys
key rotation
envelope encryption
```

---

# 321. GAP-284 — Searchable Encryption / Blind Indexing

**Classification:** `SECURITY / RESEARCH`

**Priority:** `HIGH`

Needed for equality matching protected identifiers.

Need a clear cryptographic design.

---

# 322. GAP-285 — Key Rotation

**Classification:** `SECURITY`

**Priority:** `HIGH`

Applies to:

```text
JWT/OIDC keys
federation keys
encryption keys
blind-index keys
webhook secrets
```

---

# 323. GAP-286 — Key Loss Recovery

**Classification:** `SECURITY / OPERATIONS`

**Priority:** `CRITICAL`

Need safe strategy that does not permanently destroy encrypted ownership evidence unexpectedly.

---

# 324. GAP-287 — Secure Deletion

**Classification:** `SECURITY / PRIVACY`

**Priority:** `MEDIUM`

Cloud/object storage semantics complicate physical secure deletion.

Need policy-level approach.

---

# 325. GAP-288 — Secret Leakage in Observability

**Classification:** `SECURITY`

**Priority:** `HIGH`

Need structured redaction for:

```text
logs
traces
metrics labels
error reports
```

---

# 326. GAP-289 — Secret Leakage in AI Errors

**Classification:** `SECURITY`

**Priority:** `HIGH`

AI-provider error payloads must not leak secret prompts/evidence into logs.

---

# 327. GAP-290 — PII in Correlation Data

**Classification:** `PRIVACY`

**Priority:** `MEDIUM`

Avoid using:

```text
phone
email
national ID
```

as trace IDs or Kafka keys.

---

# 328. GAP-291 — Case Export

**Classification:** `PRODUCT / SECURITY`

**Priority:** `MEDIUM`

Institutions may need case export.

Need:

```text
redaction
audit
format
access control
```

---

# 329. GAP-292 — Evidence Export for Authorities

**Classification:** `POLICY`

**Priority:** `MEDIUM`

Need controlled export workflow rather than arbitrary database access.

---

# 330. GAP-293 — Public Search Exposure

**Classification:** `PRIVACY / PRODUCT`

**Priority:** `HIGH`

How much detail should public lost/found search expose?

Need balance:

```text
discoverability
vs
ownership-proof secrecy
```

---

# 331. GAP-294 — Public Image Redaction

**Classification:** `PRIVACY`

**Priority:** `HIGH`

Need automatically protect:

```text
identity docs
faces
addresses
phone numbers
serials
```

where required.

---

# 332. GAP-295 — Public OCR Redaction

**Classification:** `PRIVACY`

**Priority:** `HIGH`

OCR may reveal secrets even when the user did not type them.

---

# 333. GAP-296 — Image-Derived Secret Evidence

**Classification:** `VERIFICATION / PRIVACY`

**Priority:** `HIGH`

Some visual evidence may need to remain hidden publicly to preserve verification value.

---

# 334. GAP-297 — Finder Secret Evidence Quality

**Classification:** `PRODUCT`

**Priority:** `MEDIUM`

Finders may not know what constitutes a useful private trait.

Need UX guidance.

---

# 335. GAP-298 — Automated Evidence Suggestions

**Classification:** `AI/ML`

**Priority:** `MEDIUM`

AI could suggest:

```text
hide this serial from public view
use this scratch as private verification evidence
```

but needs careful validation.

---

# 336. GAP-299 — Dynamic Public/Private Projection

**Classification:** `DESIGN`

**Priority:** `HIGH`

Need ensure the same evidence never accidentally appears in:

```text
public listing
```

after being designated:

```text
verification secret
```

---

# 337. GAP-300 — Disclosure History

**Classification:** `SECURITY`

**Priority:** `HIGH`

Need record when secret evidence was disclosed through:

```text
challenge
message
staff action
export
```

---

# 338. GAP-301 — Challenge Information Leakage Across Attempts

**Classification:** `SECURITY`

**Priority:** `CRITICAL`

Multiple failed challenges can cumulatively reveal the answer.

Need challenge rotation and disclosure budget.

---

# 339. GAP-302 — Challenge Brute Force

**Classification:** `SECURITY`

**Priority:** `CRITICAL`

Need:

```text
attempt limit
rate limit
cooldown
risk escalation
```

---

# 340. GAP-303 — Cross-Account Challenge Brute Force

**Classification:** `SECURITY`

**Priority:** `HIGH`

Attackers may create multiple accounts.

Need device/network/case-level limits subject to privacy constraints.

---

# 341. GAP-304 — Social Engineering Risk

**Classification:** `SECURITY / PRODUCT`

**Priority:** `HIGH`

Fraudsters may pressure finders through chat.

Recovery conversation UX should warn users not to bypass verification.

---

# 342. GAP-305 — Direct Finder-to-Owner Recovery Policy

**Classification:** `PRODUCT / SECURITY`

**Priority:** `HIGH`

When may Dzoka permit:

```text
finder
→ owner
```

direct handover rather than institutional custody?

Need configurable risk policy.

---

# 343. GAP-306 — High-Value Threshold

**Classification:** `POLICY`

**Priority:** `MEDIUM`

Should value affect:

```text
verification strength
handover
dual control
```

?

Need avoid unreliable self-reported valuation.

---

# 344. GAP-307 — Unknown Item Value

**Classification:** `POLICY`

**Priority:** `MEDIUM`

Risk cannot depend solely on declared monetary value.

---

# 345. GAP-308 — Category Risk Profiles

**Classification:** `POLICY`

**Priority:** `HIGH`

Examples:

```text
passport
keys
bank card
high-value electronics
ordinary clothing
```

need distinct policy.

---

# 346. GAP-309 — Condition Tracking

**Classification:** `CUSTODY`

**Priority:** `MEDIUM`

Need decide when to capture:

```text
condition at intake
condition before handover
damage claims
```

---

# 347. GAP-310 — Condition Evidence Authenticity

**Classification:** `CUSTODY / SECURITY`

**Priority:** `LOW-MEDIUM`

Photos can document condition but may be disputed.

Need timestamp/provenance.

---

# 348. GAP-311 — Lost From Custody Incident Handling

**Classification:** `CUSTODY`

**Priority:** `HIGH`

Architecture includes this state.

Operational process still needs design:

```text
incident investigation
notifications
liability
recovery restart
```

---

# 349. GAP-312 — Damaged in Custody

**Classification:** `CUSTODY / POLICY`

**Priority:** `MEDIUM`

Need incident handling and evidence.

---

# 350. GAP-313 — Disposal Workflow

**Classification:** `CUSTODY / POLICY`

**Priority:** `MEDIUM`

Need authorization, retention expiry, audit, and category-specific rules.

---

# 351. GAP-314 — Donation Workflow

**Classification:** `FUTURE / POLICY`

**Priority:** `LOW`

Potential for unclaimed property.

---

# 352. GAP-315 — Transfer to Authority

**Classification:** `CUSTODY`

**Priority:** `MEDIUM`

Need flow for:

```text
police
embassy
bank
issuer
transport authority
```

---

# 353. GAP-316 — Chain-of-Custody Evidence Standard

**Classification:** `CUSTODY`

**Priority:** `HIGH`

Need final evidence requirements by transfer type.

---

# 354. GAP-317 — Signature Capture

**Classification:** `PRODUCT / LEGAL`

**Priority:** `MEDIUM`

Should physical handovers capture signatures?

Need optional provider/input type.

---

# 355. GAP-318 — QR/NFC Handover

**Classification:** `PRODUCT`

**Priority:** `MEDIUM`

Could improve staff workflows but must not become identity proof by itself.

---

# 356. GAP-319 — Locker Authentication

**Classification:** `FUTURE`

**Priority:** `LOW-MEDIUM`

Possible:

```text
OTP
QR
NFC
```

with locker APIs.

---

# 357. GAP-320 — IoT Sensor Evidence

**Classification:** `FUTURE`

**Priority:** `LOW`

BLE/UWB trackers could produce sightings.

Need trust and privacy design.

---

# 358. GAP-321 — BLE Item Identity

**Classification:** `FUTURE / SECURITY`

**Priority:** `LOW`

Broadcast identifiers may create tracking/privacy concerns.

---

# 359. GAP-322 — NFC Tag Cloning

**Classification:** `SECURITY`

**Priority:** `LOW-MEDIUM`

If NFC becomes authorization-relevant, clone resistance matters.

By default tag identity should remain a routing signal, not ownership proof.

---

# 360. GAP-323 — QR Enumeration

**Classification:** `SECURITY`

**Priority:** `HIGH`

Registered-item identifiers must be opaque enough to prevent scraping/enumeration.

---

# 361. GAP-324 — Registered Asset Ownership Transfer

**Classification:** `PRODUCT`

**Priority:** `MEDIUM`

If a user sells a registered device:

```text
How does Dzoka transfer registration?
How are prior recovery secrets revoked?
```

---

# 362. GAP-325 — Shared Assets

**Classification:** `PRODUCT`

**Priority:** `MEDIUM`

Examples:

```text
family vehicle keys
company equipment
shared luggage
```

Need multiple authorized owners/custodians.

---

# 363. GAP-326 — Asset Registration Fraud

**Classification:** `SECURITY`

**Priority:** `MEDIUM`

Someone could pre-register an item they do not own.

Registration alone must not become unquestioned ownership truth.

---

# 364. GAP-327 — Pre-Loss Registration Confidence

**Classification:** `VERIFICATION`

**Priority:** `MEDIUM`

Need determine how strongly different registration evidence counts.

---

# 365. GAP-328 — Trusted Institutional Registration

**Classification:** `INTEGRATION`

**Priority:** `MEDIUM`

Institutional asset registers may provide stronger provenance.

---

# 366. GAP-329 — Manufacturer-Signed Item Identity

**Classification:** `FUTURE`

**Priority:** `LOW`

Potentially valuable if ecosystem exists.

---

# 367. GAP-330 — Cross-Platform Import

**Classification:** `INTEGRATION`

**Priority:** `MEDIUM`

Institutions may migrate from existing lost-property systems.

Need:

```text
import adapters
data mapping
provenance
```

---

# 368. GAP-331 — Duplicate Imports

**Classification:** `DATA`

**Priority:** `MEDIUM`

Imported and native Dzoka reports may describe same items.

Need deduplication.

---

# 369. GAP-332 — Legacy Data Quality

**Classification:** `DATA`

**Priority:** `MEDIUM`

Imported systems may lack:

```text
timestamps
coordinates
private evidence
consistent categories
```

Need explicit low-quality provenance.

---

# 370. GAP-333 — Data Migration Validation

**Classification:** `OPERATIONS`

**Priority:** `MEDIUM`

Need reconciliation tooling.

---

# 371. GAP-334 — Public API Rate Limits

**Classification:** `SECURITY`

**Priority:** `HIGH`

Search APIs could be abused for enumeration/scraping.

---

# 372. GAP-335 — Search Enumeration Resistance

**Classification:** `PRIVACY`

**Priority:** `HIGH`

Attackers should not be able to enumerate:

```text
all passports
all phones
all lost wallets
```

with sensitive metadata.

---

# 373. GAP-336 — Identifier Search Abuse

**Classification:** `SECURITY`

**Priority:** `CRITICAL`

Protected identifier endpoints require very strict access.

---

# 374. GAP-337 — Candidate Privacy

**Classification:** `PRIVACY`

**Priority:** `HIGH`

A candidate suggestion should reveal only enough information for legitimate workflow.

---

# 375. GAP-338 — False Candidate Notification Privacy

**Classification:** `PRIVACY`

**Priority:** `MEDIUM`

A wrong candidate notification should not leak another user's private information.

---

# 376. GAP-339 — Cross-User Media Exposure

**Classification:** `SECURITY`

**Priority:** `HIGH`

Media access must be policy-scoped even when a match exists.

---

# 377. GAP-340 — Match Explanation Privacy

**Classification:** `PRIVACY`

**Priority:** `HIGH`

Explanations must not reveal:

```text
secret serial
hidden marks
private OCR
```

to unauthorized users.

---

# 378. GAP-341 — Federated Explanation Privacy

**Classification:** `FEDERATION / PRIVACY`

**Priority:** `HIGH`

Remote nodes may return evidence reasons without exposing secret data.

Need safe explanation vocabulary.

---

# 379. GAP-342 — Human Reviewer Privacy Boundaries

**Classification:** `PRIVACY`

**Priority:** `HIGH`

Reviewers should see only evidence necessary for their task.

---

# 380. GAP-343 — Staff Segregation of Duties

**Classification:** `SECURITY`

**Priority:** `MEDIUM-HIGH`

Potentially separate:

```text
matcher reviewer
claim reviewer
custody officer
release officer
admin
```

for sensitive institutions.

---

# 381. GAP-344 — Organization Delegation

**Classification:** `AUTHORIZATION`

**Priority:** `HIGH`

Need fine-grained organizational roles and delegated authority.

---

# 382. GAP-345 — Temporary Staff Access

**Classification:** `AUTHORIZATION`

**Priority:** `MEDIUM`

Staff access may be:

```text
shift-based
temporary
location-bound
```

---

# 383. GAP-346 — Location-Based Authorization

**Classification:** `SECURITY`

**Priority:** `MEDIUM`

Staff at Site A may not be authorized for Site B.

---

# 384. GAP-347 — Central Admin vs Tenant Admin

**Classification:** `MULTI-TENANCY`

**Priority:** `HIGH`

Need strong separation between platform and tenant authority.

---

# 385. GAP-348 — Support Access

**Classification:** `SECURITY`

**Priority:** `MEDIUM`

Platform support may need diagnostic access without exposing secret evidence.

---

# 386. GAP-349 — Customer-Managed Encryption Keys

**Classification:** `FUTURE / SECURITY`

**Priority:** `LOW-MEDIUM`

Large institutions may demand tenant-managed keys.

Architecture should not make this impossible.

---

# 387. GAP-350 — Air-Gapped Deployment

**Classification:** `FUTURE / DEPLOYMENT`

**Priority:** `LOW`

Some government/security deployments may require no external providers.

Need optional local-only stack eventually.

---

# 388. GAP-351 — Local-Only AI Profile

**Classification:** `DEPLOYMENT / AI`

**Priority:** `MEDIUM`

Need model/provider set that works with:

```text
no cloud AI
```

for privacy-sensitive deployments.

---

# 389. GAP-352 — Hardware Requirements

**Classification:** `OPERATIONS`

**Priority:** `MEDIUM`

Need realistic sizing for:

```text
small CPU-only deployment
GPU institution
large federation node
edge device
```

---

# 390. GAP-353 — Performance Sizing

**Classification:** `OPERATIONS`

**Priority:** `HIGH`

Need benchmark:

```text
reports/day
media size
matching jobs
concurrent users
federation requests
```

---

# 391. GAP-354 — Storage Growth Modeling

**Classification:** `OPERATIONS`

**Priority:** `HIGH`

Media can dominate cost.

Need retention + storage projections.

---

# 392. GAP-355 — Vector Storage Growth

**Classification:** `OPERATIONS`

**Priority:** `MEDIUM`

Multiple embeddings/model versions multiply storage.

Need capacity planning.

---

# 393. GAP-356 — Re-Embedding Compute Cost

**Classification:** `OPERATIONS`

**Priority:** `MEDIUM`

Need estimate full-corpus model migrations.

---

# 394. GAP-357 — CCTV Compute Cost

**Classification:** `OPERATIONS`

**Priority:** `HIGH`

Continuous video can overwhelm GPU capacity.

Edge filtering is critical but needs benchmarks.

---

# 395. GAP-358 — Autoscaling Signals

**Classification:** `OPERATIONS`

**Priority:** `MEDIUM`

AI workers may scale on:

```text
queue depth
GPU utilization
job age
```

---

# 396. GAP-359 — Priority Inversion

**Classification:** `OPERATIONS`

**Priority:** `LOW-MEDIUM`

Historical reprocessing must not starve urgent recovery tasks.

---

# 397. GAP-360 — Backpressure

**Classification:** `OPERATIONS`

**Priority:** `HIGH`

Need defined behavior when:

```text
AI queue overloaded
broker lagging
federation peers slow
```

---

# 398. GAP-361 — User Feedback During Async Processing

**Classification:** `PRODUCT`

**Priority:** `MEDIUM`

Need status UX:

```text
report saved
analyzing images
searching
additional candidates may appear
```

without fake certainty.

---

# 399. GAP-362 — Partial Fingerprints

**Classification:** `DESIGN`

**Priority:** `HIGH`

Matching may begin before all enrichment tasks finish.

Need versioned partial fingerprint semantics.

---

# 400. GAP-363 — Required vs Optional Enrichment Tasks

**Classification:** `DESIGN`

**Priority:** `HIGH`

Some tasks may be:

```text
required
optional
category-dependent
```

Need formal build policy.

---

# 401. GAP-364 — Fingerprint Completion State

**Classification:** `DESIGN`

**Priority:** `MEDIUM`

Need distinguish:

```text
PARTIAL
READY
DEGRADED
FAILED
```

without preventing useful early matching.

---

# 402. GAP-365 — Enrichment Retry Policy

**Classification:** `OPERATIONS`

**Priority:** `MEDIUM`

Need decide when failed OCR/model jobs retry vs permanently fail.

---

# 403. GAP-366 — Manual Attribute Correction

**Classification:** `PRODUCT`

**Priority:** `MEDIUM`

Users/staff may correct AI attributes.

Need retain:

```text
AI value
human override
provenance
```

---

# 404. GAP-367 — Attribute Consensus

**Classification:** `DESIGN`

**Priority:** `HIGH`

If:

```text
user says navy
vision says black
staff says dark blue
```

how does canonical value form?

Need consensus rules.

---

# 405. GAP-368 — Contradictory Evidence Preservation

**Classification:** `DATA`

**Priority:** `HIGH`

Do not erase conflicts simply to create one canonical attribute.

---

# 406. GAP-369 — Model Confidence vs Evidence Reliability

**Classification:** `AI/ML`

**Priority:** `HIGH`

A model may be highly confident and still unreliable for a certain category/domain.

Need reliability metadata.

---

# 407. GAP-370 — Model Performance by Category

**Classification:** `MLOPS`

**Priority:** `HIGH`

Need category-specific metrics.

---

# 408. GAP-371 — Model Performance Drift

**Classification:** `MLOPS`

**Priority:** `MEDIUM`

Monitor over time.

---

# 409. GAP-372 — Dataset Drift

**Classification:** `MLOPS`

**Priority:** `MEDIUM`

Products, camera quality, user behavior change.

---

# 410. GAP-373 — Tenant-Specific Model Adaptation

**Classification:** `FUTURE / MLOPS`

**Priority:** `LOW-MEDIUM`

Airports may benefit from different model configuration than universities.

Need avoid fragmented model sprawl.

---

# 411. GAP-374 — Fine-Tuning Governance

**Classification:** `MLOPS`

**Priority:** `MEDIUM`

Who can fine-tune?

On what data?

How is approval managed?

---

# 412. GAP-375 — Human Approval of Model Promotion

**Classification:** `MLOPS`

**Priority:** `HIGH`

New model/config should not auto-promote solely on one metric.

---

# 413. GAP-376 — Rollback of Matching Configuration

**Classification:** `MLOPS`

**Priority:** `HIGH`

Config rollback must be as deliberate as model rollback.

---

# 414. GAP-377 — False Match Incident Process

**Classification:** `OPERATIONS / SAFETY`

**Priority:** `CRITICAL`

If Dzoka contributes to wrong-item recovery:

```text
How is incident reported?
How is case frozen?
How are parties contacted?
How is evidence preserved?
How are models/config reviewed?
```

---

# 415. GAP-378 — Wrongful Release Incident

**Classification:** `SAFETY / SECURITY`

**Priority:** `CRITICAL`

Requires formal incident runbook.

---

# 416. GAP-379 — Lost-in-Custody Incident

**Classification:** `CUSTODY / OPERATIONS`

**Priority:** `CRITICAL`

Requires separate runbook.

---

# 417. GAP-380 — Compromised Federation Node Incident

**Classification:** `SECURITY`

**Priority:** `CRITICAL`

Need:

```text
revoke key
disable peer
review exchanged data
notify affected nodes
```

---

# 418. GAP-381 — Model Compromise Incident

**Classification:** `SECURITY / MLOPS`

**Priority:** `HIGH`

Need disable/rollback/replace model.

---

# 419. GAP-382 — Provider Outage Runbooks

**Classification:** `OPERATIONS`

**Priority:** `HIGH`

Required for:

```text
identity
database
object storage
broker
AI
messaging
federation
```

---

# 420. GAP-383 — Database Failover

**Classification:** `OPERATIONS`

**Priority:** `HIGH`

Large deployments need tested HA architecture.

---

# 421. GAP-384 — Object Storage Failover

**Classification:** `OPERATIONS`

**Priority:** `MEDIUM-HIGH`

Need failure behavior if proof/media temporarily unavailable.

---

# 422. GAP-385 — Broker Recovery

**Classification:** `OPERATIONS`

**Priority:** `HIGH`

Need validate outbox catch-up and idempotent replay.

---

# 423. GAP-386 — Identity Provider Outage

**Classification:** `OPERATIONS`

**Priority:** `HIGH`

Need determine cached sessions vs new-login behavior.

---

# 424. GAP-387 — Clock Failure

**Classification:** `OPERATIONS`

**Priority:** `MEDIUM`

Security credentials and federation signatures depend on time.

---

# 425. GAP-388 — Observability Cardinality

**Classification:** `OPERATIONS`

**Priority:** `LOW-MEDIUM`

Avoid high-cardinality metrics from:

```text
item IDs
user IDs
```

---

# 426. GAP-389 — Sensitive Trace Data

**Classification:** `PRIVACY`

**Priority:** `HIGH`

Trace attributes must not contain secret evidence.

---

# 427. GAP-390 — Audit Search

**Classification:** `OPERATIONS / SECURITY`

**Priority:** `MEDIUM`

Need reviewer/admin audit-query tools without exposing unnecessary secrets.

---

# 428. GAP-391 — Audit Retention

**Classification:** `POLICY`

**Priority:** `HIGH`

May vary by institution and event type.

---

# 429. GAP-392 — Audit Export

**Classification:** `SECURITY`

**Priority:** `MEDIUM`

Need tamper-aware, access-controlled exports.

---

# 430. GAP-393 — Audit Tamper Detection

**Classification:** `SECURITY`

**Priority:** `MEDIUM`

Potential hash chaining remains unresolved.

---

# 431. GAP-394 — Full Legal/Regulatory Review

**Classification:** `LEGAL/POLICY`

**Priority:** `BEFORE PRODUCTION`

Dzoka may touch:

```text
PII
identity documents
location
CCTV
biometrics
payments
cross-border data
```

Deployment-specific legal review will be necessary.

---

# 432. GAP-395 — Zimbabwe Deployment Policy

**Classification:** `LEGAL/POLICY`

**Priority:** `BEFORE LOCAL PRODUCTION`

A Zimbabwe production deployment needs specific privacy, payments, communications, CCTV, and institutional policy review.

---

# 433. GAP-396 — Multi-Jurisdiction Architecture

**Classification:** `LEGAL / FEDERATION`

**Priority:** `HIGH FOR FEDERATION`

Federation must not assume one jurisdiction.

---

# 434. GAP-397 — Law-Enforcement Requests

**Classification:** `POLICY`

**Priority:** `MEDIUM`

Need controlled process for lawful requests and disclosure audit.

---

# 435. GAP-398 — Data Breach Response

**Classification:** `SECURITY / POLICY`

**Priority:** `HIGH`

Need incident response process.

---

# 436. GAP-399 — Privacy Notice / Consent UX

**Classification:** `PRODUCT / LEGAL`

**Priority:** `BEFORE PRODUCTION`

Need clear explanation of:

```text
matching
AI
location
media
federation
retention
```

---

# 437. GAP-400 — Source Audit Completeness

**Classification:** `SOURCE AUDIT`

**Priority:** `ONGOING`

The current audit set is strong but not final.

Additional repositories may reveal better implementations before specific components are locked.

---

# 438. Gaps That Are Architectural Novelty

Several gaps are not merely missing libraries.

They are likely areas where Dzoka itself must contribute original system design.

These include:

```text
ItemFingerprint

dynamic private verification

RecoveryAuthorization

formal lost-property custody model

lost-property federation protocol

cross-node custody transfer

recovery movement intelligence

privacy-aware multimodal federation
```

---

# 439. Gaps That Should Not Be Solved With More Dependencies

Some gaps require domain design rather than another library.

Examples:

```text
claim conflict

recovery authorization

custody

handover policy

entitlement

verification disclosure

reward eligibility
```

Adding another framework will not solve these semantics.

---

# 440. Gaps That Require Empirical Evaluation

These cannot be decided reliably from architecture documents alone:

```text
local visual model

feature weights

calibration

candidate K

patch count

geo decay

time decay

LLM usefulness

multilingual embeddings

movement model
```

They require datasets and experiments.

---

# 441. Gaps That Require Security Threat Modeling

```text
verification challenge

handover token

cross-node identifier comparison

anonymous reporting

QR identity

CCTV

edge devices

federation keys

reward/payment
```

---

# 442. Gaps That Require Policy Decisions

```text
high-risk categories

legal holds

document recovery

direct finder handover

reward rules

retention

institution authority

appeals
```

---

# 443. Gaps That Require Operational Validation

```text
broker

HA PostgreSQL

object storage

edge fleet

GPU workers

backup/restore

federation failover

re-embedding
```

---

# 444. Gaps That Can Be Deferred Without Architectural Rework

The following can remain optional if correct abstractions exist:

```text
Qdrant

DID / VC

graph database

smart lockers

BLE / UWB

blockchain escrow

advanced reputation

manufacturer integrations
```

---

# 445. Gaps That Cannot Be Deferred to "Future Work"

The following are core correctness requirements and must have real designs before Dzoka can be considered production-complete:

```text
same-instance matching strategy

private ownership verification

claim conflicts

RecoveryAuthorization

custody state machine

secure handover

tenant isolation

protected identifiers

event idempotency

privacy projections

federation security

recovery audit
```

---

# 446. Build-vs-Research Matrix

| Capability | Current status |
|---|---|
| PostgreSQL persistence | Reuse mature technology |
| Vector search | Reuse mature technology |
| OCR | Reuse mature technology |
| Detection | Reuse mature technology |
| Global embeddings | Reuse mature technology |
| Local-instance matching | **Research + build** |
| ItemFingerprint | **Build** |
| Matching orchestration | **Build** |
| Hard conflict engine | **Build** |
| Dynamic ownership verification | **Build + research** |
| Claim conflict | **Build** |
| RecoveryAuthorization | **Build** |
| Custody state machine | **Build** |
| Secure handover | **Build** |
| Messaging adapters | Reuse APIs, **build abstraction** |
| Federation protocol | **Build** |
| EPCIS integration | Reuse standard, **build adapter** |
| Movement intelligence | **Research + build** |
| CCTV temporal search | Adapt + build |
| Rewards | Adapt + build |
| Courier integration | **Build abstraction** |

---

# 447. Priority Tier 0 — Correctness Before Public Recovery

Must resolve before users can safely rely on Dzoka for item release:

```text
ownership verification
RecoveryAuthorization
custody
handover credential
authorization
tenant isolation
private evidence security
hard conflicts
```

---

# 448. Priority Tier 1 — Matching Quality

Resolve before serious matching claims:

```text
instance-level visual pipeline
multimodal calibration
evidence coverage
hard-negative dataset
attribute ontology
protected identifier matching
```

---

# 449. Priority Tier 2 — Institutional Production

Resolve for serious institutional deployment:

```text
OIDC
multi-tenancy
policy versioning
audit
retention
backup
HA
organization hierarchy
role delegation
```

---

# 450. Priority Tier 3 — Federation

Resolve before cross-node production:

```text
protocol
node identity
signatures
privacy minimization
score fusion
cross-node claim
cross-node custody
```

---

# 451. Priority Tier 4 — Advanced Intelligence

```text
movement prediction
advanced CCTV
LLM reranking
model learning
route prediction
```

These add intelligence but must not be allowed to block correctness of the foundational recovery workflow.

---

# 452. Priority Tier 5 — Ecosystem Extensions

```text
DID
manufacturer integrations
smart lockers
BLE/UWB
blockchain adapters
```

Optional extensions.

---

# 453. Recommended Next Source Audits

Before infrastructure is fully locked:

```text
Kafka
Redpanda
Keycloak
MinIO
Redis / compatible alternatives
OPA
faster-whisper
MLflow
OpenTelemetry stack
OSRM
Valhalla
ONVIF tooling
```

---

# 454. Recommended Next Research Spike — Same-Instance Vision

Build an offline experiment using:

```text
OpenCLIP global retrieval
+
local patch features
+
OCR
+
pHash
```

against a deliberately difficult same-model dataset.

Measure:

```text
Recall@K
MRR
precision
false match rate
```

---

# 455. Recommended Next Research Spike — Verification

Create synthetic/private-evidence test cases and evaluate:

```text
exact comparison
fuzzy comparison
local semantic embeddings
LLM semantic interpretation
human decision
```

without exposing expected secrets externally.

---

# 456. Recommended Next Research Spike — Federation

Build three mock nodes:

```text
Node A
Node B
Node C
```

with intentionally different local scoring scales.

Test:

```text
fanout
timeout
circuit breaker
RRF
privacy-safe candidates
```

---

# 457. Recommended Next Research Spike — Movement

Create a synthetic institution graph:

```text
Gate
→ Security
→ Lost Property
→ Courier
→ Owner
```

and compare:

```text
expert transition priors
Markov model
sequence model
```

before considering complex trajectory models.

---

# 458. Recommended Next Research Spike — CCTV

Build a small controlled dataset with:

```text
object present
temporary occlusion
object removed
person carries object
camera lighting changes
```

and validate coarse-to-fine disappearance search.

---

# 459. Recommended Next Security Spike — Handover

Threat-model:

```text
stolen OTP
screenshot QR
brute-force
replay
wrong recipient
offline handover
insider misuse
```

before finalizing the protocol.

---

# 460. Recommended Next Security Spike — Protected Identifiers

Prototype:

```text
encrypted original
+
keyed searchable representation
```

and evaluate:

```text
rotation
tenant isolation
federation compatibility
```

---

# 461. Recommended Next Product Spike — WhatsApp

Prototype full:

```text
report
→ media
→ candidate
→ claim
→ verification
```

conversation using the same backend use cases as web.

The goal is to prove the channel does not create alternate domain logic.

---

# 462. Recommended Next Operations Spike — Small Deployment

Run the intended small stack on a modest VPS and measure:

```text
RAM
CPU
disk
vector search
media upload
AI queues
broker overhead
```

before declaring all infrastructure mandatory.

---

# 463. Definition of Gap Resolution

A gap is not resolved when:

```text
implementation exists
```

alone.

Resolution should normally include:

```text
documented design
security implications
tests
observability
failure handling
license review
operational strategy
```

where relevant.

---

# 464. Gap Closure Template

When closing a gap, record:

```text
Gap ID:
Decision:
Evidence:
Architecture affected:
Design document:
ADR:
Implementation:
Tests:
Operational validation:
Remaining limitations:
Closed at:
```

---

# 465. Gap Reopening

A resolved gap may be reopened when:

```text
new source audit changes assumptions
new threat emerges
model performance degrades
new deployment profile appears
regulation changes
scale invalidates design
```

---

# 466. Source Audits and Gap Closure

Future source audits may change a gap from:

```text
BUILD
```

to:

```text
ADOPT
```

or:

```text
ADAPT
```

if a mature suitable project is discovered.

This is why source audits remain ongoing.

---

# 467. Avoiding Premature Closure

Do not close a gap based on:

```text
README claim
marketing page
single demo
one successful test case
benchmark copied from upstream
```

Use evidence.

---

# 468. Avoiding Endless Research

Conversely, Dzoka should not remain permanently in architecture mode.

A gap should move to implementation when:

```text
risk is understood
interfaces are clear
reasonable default exists
experiments support decision
```

Perfection is not required.

Correct boundaries are.

---

# 469. Architecture Stability Rule

Open implementation choices must not destabilize fixed domain invariants.

Example:

```text
local visual model can change
```

without changing:

```text
Matching ≠ Ownership
```

---

# 470. Provider Stability Rule

Open provider choices should sit behind interfaces.

Example:

```text
Kafka ↔ Redpanda
```

should not change:

```text
Domain Event
Transactional Outbox
Idempotent Consumer
```

semantics.

---

# 471. Model Stability Rule

Model replacement should not change business authority.

Example:

```text
OpenCLIP model changes
```

must not suddenly allow:

```text
AI directly authorizes release
```

---

# 472. Federation Stability Rule

Protocol evolution must preserve:

```text
node identity
source attribution
privacy
holding-node authority
score non-comparability
```

unless a deliberate protocol ADR changes them.

---

# 473. Recovery Stability Rule

No unresolved AI, provider, or federation question should weaken:

```text
Claim
Verification
RecoveryAuthorization
Custody
Handover
```

boundaries.

---

# 474. Final Unresolved Technical Core

The deepest remaining technical problem is:

```text
Can Dzoka reliably infer that two imperfect observations
refer to the exact same physical object,
while preserving uncertainty and explaining why?
```

This requires:

```text
multimodal evidence
instance-level vision
conflicts
coverage
calibration
```

---

# 475. Final Unresolved Trust Core

The deepest trust problem is:

```text
Can Dzoka determine that a claimant is genuinely entitled
to receive a specific physical object without publicly revealing
the evidence that proves ownership?
```

This requires:

```text
private evidence
safe challenges
risk
policy
human authority
```

---

# 476. Final Unresolved Recovery Core

The deepest recovery problem is:

```text
Can Dzoka prove that the right physical object
was transferred from the correct custodian
to the authorized recipient?
```

This requires:

```text
RecoveryAuthorization
secure handover
custody events
recipient confirmation
audit
```

---

# 477. Final Unresolved Federation Core

The deepest federation problem is:

```text
Can independent institutions discover and recover the same item
without sharing unnecessary sensitive data or trusting each other's
internal scoring systems blindly?
```

This requires:

```text
node trust
signed protocol
privacy-safe search
source-aware ranking
cross-node verification
cross-node custody
```

---

# 478. Final Unresolved Intelligence Core

The deepest intelligence problem is:

```text
Can Dzoka predict where an item is likely to move next
without confusing prediction with factual observation?
```

This requires:

```text
recovery graph
transition history
institution workflows
probabilistic modeling
strict prediction/fact separation
```

---

# 479. What the Audits Have Already Resolved

Despite the unresolved gaps, the audits have resolved several major questions.

Dzoka now has strong evidence for:

```text
PostgreSQL authoritative storage

PostGIS

pgvector default vectors

OpenCLIP global embeddings

PaddleOCR

PaddleDetection

hybrid retrieval

hard conflict handling

missing-evidence semantics

private challenge concept

claim conflict modeling

event-driven async architecture

formal custody

federation gateway

EPCIS as adapter

no blockchain default
```

---

# 480. What the Audits Have Proven We Must Build

Current source audits strongly indicate that Dzoka must own:

```text
ItemFingerprint

Matching Engine orchestration

dynamic ownership verification

RecoveryAuthorization

Custody State Machine

secure handover protocol

lost-property federation protocol

recovery movement intelligence
```

These are not adequately solved by the audited upstream projects.

---

# 481. Why This Is Valuable

An unresolved-gap list prevents two dangerous assumptions.

First:

```text
"there is probably a GitHub project that already solves it"
```

Second:

```text
"we already discussed it, therefore it is implemented"
```

Neither is safe.

---

# 482. Final Principle

Dzoka should not hide uncertainty in architecture any more than it hides uncertainty in matching.

If a capability is unresolved, say:

```text
UNRESOLVED
```

If a model is experimental, say:

```text
EXPERIMENTAL
```

If a provider has not been audited, say:

```text
NOT YET LOCKED
```

The same principle that governs Dzoka's matching engine should govern its engineering process:

> **Unknown is a valid state. Unknown must not be converted into false certainty.**

---

# 483. Final Conclusion

The current audits establish that Dzoka's challenge is no longer finding enough open-source projects.

The real task is now integrating mature foundations around a set of difficult domain problems that remain genuinely unsolved.

The remaining chain is:

```text
REAL INSTANCE IDENTITY
        ↓
TRUSTWORTHY MATCH
        ↓
PRIVATE OWNERSHIP PROOF
        ↓
ENTITLEMENT
        ↓
RECOVERY AUTHORIZATION
        ↓
PHYSICAL CUSTODY
        ↓
SECURE HANDOVER
        ↓
FEDERATED RECOVERY
        ↓
LEARNING
```

The goal of future research, ADRs, designs, and implementations is to close these gaps **without weakening the invariants already established by the source audits**.

The governing rule is:

> **Dzoka should reuse what the ecosystem already solves well, research what remains scientifically uncertain, and explicitly build the trust and recovery semantics that no audited upstream project currently provides.**