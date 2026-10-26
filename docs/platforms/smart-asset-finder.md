# Source Audit — fredopoku/Smart-Asset-Finder

> **Dzoka — The Open Recovery Network**
>
> Source audit of Smart Asset Finder, with emphasis on QR-assisted recovery, AI text matching, ownership-question scoring, WhatsApp notifications, geolocation, claims, proof uploads, rewards, tags, and payments.

---

# 1. Audit Metadata

| Field | Value |
|---|---|
| Repository | `https://github.com/fredopoku/Smart-Asset-Finder` |
| Upstream owner | `fredopoku` |
| Project | Smart Asset Finder |
| Audited revision | `8ac4484354d23aea42e1a569e2f127a878049b1e` |
| Short revision | `8ac44843` |
| Revision date | 2026-06-23 |
| Audit date | 2026-10-04 |
| Default branch | `main` |
| Primary language | PHP |
| Database | MySQL / MariaDB |
| Frontend | Bootstrap + jQuery |
| Maps | Leaflet + OpenStreetMap / Nominatim |
| AI provider | Anthropic Claude |
| AI fallback | PHP `similar_text()` + keyword heuristic |
| WhatsApp | UltraMsg outbound API |
| Email | PHPMailer |
| QR | `qrcode.js` + server-side tag records |
| Payments | Paystack + manual mobile money/bank flows |
| Primary classification | **REFERENCE ONLY** |
| License | **All rights reserved** |
| Direct code reuse | **PROHIBITED without explicit permission from copyright holder** |

---

# 2. Executive Decision

Smart Asset Finder is architecturally interesting because it attempts to combine:

```text id="x7rnv2"
lost/found reports
+
security questions
+
AI text matching
+
AI ownership scoring
+
QR asset tags
+
GPS scan events
+
WhatsApp
+
email
+
claims
+
proof uploads
+
rewards
+
payments
```

inside one product.

That makes it useful to Dzoka as a **product/workflow reference**.

However:

```text id="mp8uw0"
LICENSE:
ALL RIGHTS RESERVED
```

means Dzoka must not copy or adapt its source code without separate permission.

The correct classification is:

```text id="qyif1t"
REFERENCE ONLY
```

with selected concepts independently reimplemented.

---

# 3. Why This Repository Matters to Dzoka

Most lost-and-found repositories stop at:

```text id="ukgxvi"
report
→ search
→ claim
```

Smart Asset Finder attempts a broader recovery ecosystem:

```text id="fhjtq5"
report
→ AI discovery
→ notifications
→ claim
→ proof
→ admin review
→ QR recovery
→ rewards
→ hardware
→ payments
```

This makes it especially useful when designing Dzoka's:

```text id="q5hzj7"
Messaging
QR/NFC recovery
Claims
Rewards
Payments
Notifications
Item Identity
Recovery incentives
```

domains.

---

# 4. Audit Scope

Primary implementation paths inspected include:

```text id="4xhlnl"
README.md
.env.example
config.php

classes/
├── AiMatcher.php
├── Master.php
├── Whatsapp.php
├── QrScan.php
└── Login.php

items/
└── claim_form.php

admin/claims/
└── view_claim.php

database/
└── lfis_db.sql

rewards.php
shop.php
my-orders.php
tag.php
lost.php
found.php
```

---

# 5. Audit Limit

This audit is focused on architectural and security-relevant implementation.

It is not:

- a full penetration test;
- an exhaustive PHP review;
- a payment-compliance audit;
- a full dependency audit;
- independent verification of Claude model accuracy;
- a legal opinion.

---

# 6. License

The README explicitly states:

```text id="3l0q1y"
All rights reserved by the author
```

Therefore this repository is **not an open-source code reuse source** despite being publicly visible on GitHub.

---

# 7. Dzoka Licensing Decision

Allowed:

```text id="40zc2b"
study concepts
study workflows
document architectural lessons
independently reimplement ideas
```

Not allowed without permission:

```text id="rzw06u"
copy source
port functions
copy templates
copy substantial schema/code
create lightly modified derivatives
```

---

# 8. Architecture

The application is primarily a traditional PHP monolith:

```text id="o3axye"
Browser
   ↓
PHP Pages / AJAX
   ↓
Master.php / service classes
   ↓
MySQL
   ↓
External Providers
├── Claude
├── UltraMsg
├── SMTP
├── Paystack
└── OpenStreetMap
```

---

# 9. Main Product Capabilities

The project implements concepts around:

```text id="hvvfvw"
lost reports
found reports
multiple media
GPS
map browsing
travel mode
security Q&A
claims
AI scoring
QR tags
QR scan telemetry
notifications
email
WhatsApp
rewards
badges
shop/orders
payments
```

---

# 10. Lost / Found Model

`item_list` stores:

```text id="oo5sbs"
item_type
category_id
title
description
serial_number
ownership_notes
contact
location
lat
lng
is_travelling
location_country
date_lost_found
image_path
status
ai_match_score
```

---

# 11. Positive — Serial Number Field

The presence of:

```text id="u80jtb"
serial_number
```

shows awareness that strong object identity fields matter.

However, it is stored directly as ordinary plaintext.

---

# 12. Dzoka Improvement

Dzoka treats sensitive identifiers such as:

```text id="7nbtfu"
IMEI
serial number
passport number
asset identifier
```

as protected evidence.

Recommended representation:

```text id="aaxt1q"
keyed searchable representation
+
encrypted original
+
safe display suffix
```

rather than ordinary searchable plaintext.

---

# 13. Ownership Notes

The source includes:

```text id="mftz0n"
ownership_notes
```

as additional private-ish identification information.

The idea is useful, but Dzoka should not mix secret ownership evidence into general item records.

---

# 14. Dzoka Separation

Use:

```text id="tdlsvw"
PUBLIC ATTRIBUTES
```

separately from:

```text id="ex9rzt"
PRIVATE OWNERSHIP EVIDENCE
```

and:

```text id="z32phj"
SECRET IDENTIFIERS
```

---

# 15. Multi-Media Reports

The source supports several media files per report.

This is directionally valuable because physical-item identification benefits from:

```text id="l0drxc"
multiple angles
distinctive features
different surfaces
damage
labels
```

---

# 16. Dzoka Extension

Dzoka records each:

```text id="ti2um4"
MediaAsset
```

individually with:

```text id="av7mmj"
content hash
classification
derivatives
provenance
model runs
```

rather than treating images as simple upload paths.

---

# 17. Location

The project captures:

```text id="fgsbtd"
human-readable location
latitude
longitude
```

using Leaflet and Nominatim.

This is useful.

---

# 18. Travel Mode

Reports include:

```text id="y77i4v"
is_travelling
location_country
```

which is a particularly interesting product idea.

A user may lose/find an item while:

```text id="ewjszb"
travelling
transiting
passing through
```

rather than near their home.

---

# 19. Dzoka Lesson — Location Is Contextual

The `is_travelling` concept reinforces that physical-item recovery cannot rely only on:

```text id="j5a8oh"
distance from report coordinates
```

Dzoka's recovery graph and movement intelligence should model:

```text id="n33an6"
routes
transport systems
institutions
movement
```

---

# 20. AI Matching

`AiMatcher::findAndNotifyMatches()` implements the automatic matching process.

The actual pipeline is much simpler than the full product description might suggest.

---

# 21. Actual Matching Inputs

The AI matcher compares:

```text id="xyr30l"
found.title
+
found.description
```

against:

```text id="pzmn9x"
lost.title
+
lost.description
```

---

# 22. No Visual AI in Matcher

Despite reports containing images, the inspected matching implementation does **not** use:

```text id="lr61ef"
image embeddings
object detection
OCR
local visual features
```

for lost↔found matching.

---

# 23. No Geographic Score in AI Matcher

The inspected matching path also does not incorporate:

```text id="z9so9u"
latitude
longitude
distance
```

into the actual candidate score.

---

# 24. No Time Score

Beyond a hard report-age window, matching does not perform a proper temporal similarity calculation.

---

# 25. Hard Category Gate

The query only retrieves:

```text id="kngld4"
same category
```

lost items.

This can create false negatives when categorization differs.

---

# 26. Hard 90-Day Gate

Only lost reports created within approximately:

```text id="xmr4ex"
90 days
```

are considered.

Again, this may remove valid matches before evidence scoring.

---

# 27. Dzoka Rejection

Do not use:

```text id="w4x8h9"
category
```

or generic report age as unconditional destructive gates unless policy/evidence justifies it.

---

# 28. Claude Matching

If an API key exists, the matcher sends two item descriptions to Claude and asks:

```text id="17cj11"
How likely are these to be the same physical item?
```

with a requested score from:

```text id="red247"
0–100
```

---

# 29. Numeric Parsing

The response is reduced to a number by removing non-numeric characters.

That number becomes the similarity score.

---

# 30. Dzoka Rejection — LLM Score as Identity Metric

A free-form LLM-generated number is not a calibrated physical identity probability.

Do not treat:

```text id="c28qfy"
Claude says 87
```

as:

```text id="q39rw1"
87% chance same item
```

---

# 31. Local Fallback

Without Claude, the platform uses:

```text id="v6i8r5"
PHP similar_text()
+
keyword overlap bonus
```

---

# 32. Important Capability Qualification

Therefore the platform's:

```text id="u684n8"
AI matching
```

is fundamentally:

```text id="coom9h"
TEXT MATCHING
```

rather than multimodal physical-instance identification.

---

# 33. Match Threshold

Candidates scoring at least roughly:

```text id="j9zix5"
38
```

are retained.

Top:

```text id="b9etst"
3
```

are returned and their owners notified.

---

# 34. Match Persistence Weakness

The best score is written directly to:

```text id="jh6on4"
item_list.ai_match_score
```

on the found item.

This does not preserve:

```text id="iixvbf"
which lost item
algorithm version
model
prompt
candidate list
feature evidence
matching run
```

---

# 35. Dzoka Correction

Use:

```text id="oow9lm"
MatchingRun
MatchCandidate
MatchEvidence
```

rather than one mutable:

```text id="x52ecx"
ai_match_score
```

field.

---

# 36. Candidate Provenance

Dzoka must always be able to answer:

```text id="5y1dn5"
Which two fingerprints were compared?

Using which models?

Using which configuration?

What evidence contributed?

What conflicts existed?

When?
```

---

# 37. AI Match Notifications

One useful product behavior is immediate notification after a possible match.

The platform can notify through:

```text id="9o1ijz"
in-app
email
WhatsApp
```

---

# 38. Dzoka Adoption

Dzoka should preserve the event concept:

```text id="ue1j8m"
HighQualityCandidateDetected
    ↓
NotificationPolicy
    ↓
authorized channels
```

---

# 39. Notification Must Not Imply Ownership

The wording should remain:

```text id="2v3ntd"
possible match
```

rather than:

```text id="801j91"
we found your item
```

until verification occurs.

---

# 40. Fraud / Spam Check

`checkItemReport()` performs:

```text id="g8zkn0"
minimum description length
report-frequency limit
optional Claude moderation
```

---

# 41. Rule-Based Checks

The non-AI checks are straightforward:

```text id="tvp6zl"
too-short description
too many reports in one hour
```

These are sensible abuse controls.

---

# 42. AI Fraud Check

Claude is asked whether a report looks like:

```text id="i4be77"
legitimate
spam
fake
advertisement
gibberish
```

---

# 43. AI Can Block Submission

If Claude replies:

```text id="dxjmwb"
FLAG:...
```

the submission is rejected.

This gives a third-party probabilistic model direct blocking power.

---

# 44. Dzoka Improvement

AI moderation may:

```text id="ys57an"
flag
prioritize
quarantine
request more detail
```

but high-impact automated rejection should have:

```text id="o5kf3q"
policy
review
appeal
```

where appropriate.

---

# 45. Provider Failure Semantics

If the Claude request fails, the helper returns:

```text id="dqu54m"
0
```

For spam checking, a response other than `FLAG:` is treated as acceptable.

Therefore AI moderation generally fails open.

---

# 46. Dzoka Improvement

Provider outcomes should be explicit:

```text id="n46vkp"
SUCCESS
NO_RESULT
FAILED
TIMED_OUT
```

Do not overload numeric zero to represent infrastructure failure.

---

# 47. Security Questions

Lost reports can provide:

```text id="9ni72z"
item_security_qa
```

records.

These contain:

```text id="b2vju6"
question
answer_normalized
sort_order
```

---

# 48. Critical Finding — Answers Are Not Protected

`answer_normalized` is stored as ordinary plaintext.

Normalization consists mainly of:

```text id="l83ybg"
lowercase
strip punctuation
collapse spaces
```

---

# 49. Example

Input:

```text id="76px8p"
Blue Ferrari Sticker!
```

may become:

```text id="4lrf59"
blue ferrari sticker
```

This is normalization.

It is not:

```text id="zbiw8k"
hashing
encryption
protection
```

---

# 50. Dzoka Rejection

Private ownership evidence must not be stored as ordinary normalized plaintext.

---

# 51. Secret Evidence Sent to Claude

`scoreWithClaude()` includes both:

```text id="rd60ve"
owner's expected answer
claimant's submitted answer
```

inside the external model prompt.

---

# 52. Privacy Consequence

Sensitive ownership evidence may leave Dzoka's trust boundary and be sent to a third-party model provider.

Examples could include:

```text id="ednwov"
engraving
serial details
private markings
personal names
contents
```

---

# 53. Dzoka Rule

SECRET ownership evidence must not automatically be sent to an external model.

Use:

```text id="4zmint"
AIProviderInputPolicy
```

and explicit classification.

---

# 54. Possible Local Evaluation

Where possible, exact challenges should use:

```text id="88mrq6"
local deterministic comparison
```

without revealing protected expected answers externally.

---

# 55. Claim Scoring

For each stored security question:

```text id="fcozta"
submitted answer
    ↓
normalize
    ↓
Claude or local comparison
    ↓
0–100 score
```

Scores are averaged.

---

# 56. Score Thresholds

The claim handler behaves roughly as:

```text id="l98tlu"
< 35
→ reject submission

35–69
→ pending

>= 70
→ verification_status = verified
```

---

# 57. Dangerous Language — “AI Verified”

The admin interface labels sufficiently high-scoring claims:

```text id="x0my2p"
AI Verified
```

This is much too strong.

---

# 58. Dzoka Rule

Never equate:

```text id="x5dr2g"
semantic answer similarity
```

with:

```text id="4nntgk"
verified ownership
```

---

# 59. Correct Terminology

Use something like:

```text id="3tu8gs"
challenge_similarity = high
```

or:

```text id="ypd86j"
private_knowledge_evidence = strong
```

not:

```text id="b8hkld"
ownership verified
```

---

# 60. Automatic Low-Score Rejection

Claims scoring below:

```text id="fiw5pg"
35
```

are prevented from being submitted through the ordinary workflow.

This can produce false denial.

---

# 61. Dzoka Improvement

A weak challenge response might:

```text id="iqevow"
increase risk
reduce confidence
request other evidence
route to human review
```

rather than permanently prevent a legitimate claimant from proceeding.

---

# 62. Security-Q&A Wiring Inconsistency

One of the most important findings is architectural.

Security questions are stored only when:

```text id="dmjh20"
item_type === LOST
```

during new public report submission.

---

# 63. Claim Form Behavior

The claim form loads security questions using:

```text id="z6myv6"
item_security_qa
WHERE item_id = claimed item
```

and displays them only when the item being claimed is a FOUND item.

---

# 64. Logical Consequence

A normal found report:

```text id="2me1a7"
does not receive security questions
```

during creation.

Therefore:

```text id="h6jdqp"
claim found item
→ load found item's questions
→ usually none
```

---

# 65. Missing Lost↔Found Verification Link

The system does not clearly connect:

```text id="l28t6q"
lost report's private answers
```

to:

```text id="txjue0"
claim against matched found report
```

inside the inspected claim path.

---

# 66. Dzoka Lesson

Private ownership evidence belongs to the:

```text id="a0e3xi"
claim / verification relationship
```

across corresponding item observations.

It must not depend on both records accidentally sharing one item ID.

---

# 67. Correct Dzoka Flow

```text id="zoy7e6"
LostReport
    ↓
private evidence

FoundReport
    ↓
match candidate

Claim
    ↓
VerificationSession

private LostReport evidence
+
private FoundReport evidence
    ↓
challenge
```

---

# 68. Proof Upload

Claimants may upload:

```text id="h55bk7"
image
PNG
WebP
PDF
```

as proof of ownership.

This is a useful product feature.

---

# 69. Dzoka Adoption

Proof evidence may include:

```text id="iujzc3"
receipt
pre-loss photograph
document
screenshot
purchase record
```

and should be modeled as typed `ClaimEvidence`.

---

# 70. Upload Security Concern

The proof uploader checks MIME type but derives the stored extension from the original filename.

That can be dangerous when uploads are served from an executable web directory.

---

# 71. Dzoka Upload Rule

Never trust:

```text id="q22y9m"
original filename extension
```

to determine executable storage behavior.

Uploads should be:

```text id="v37x24"
content validated
renamed
stored outside executable application paths
served through controlled object storage
```

---

# 72. Admin Claim Review

Admin reviewers can inspect:

```text id="zfp8sa"
claimant identity
message/evidence
AI score
verification status
uploaded proof
admin note
```

before approving/rejecting.

This is useful UX inspiration.

---

# 73. Positive Authority Boundary

Despite the AI score, final claim status is changed by:

```text id="58ed84"
admin action
```

rather than directly by Claude.

That is safer than full automatic release.

---

# 74. But AI Already Filters Claims

The earlier `<35` rejection means AI still has substantial authority before the admin stage.

Dzoka should remove this ambiguity.

---

# 75. Claim Approval

When approved:

```text id="50v926"
claim.status = approved
```

and:

```text id="g6duiy"
item.status = claimed
```

This is better than immediately calling the item physically returned.

---

# 76. Recovery Gap

However, after approval the workflow largely says:

```text id="ckxtsw"
contact finder/admin
arrange return
```

There is no full:

```text id="etbrup"
RecoveryAuthorization
CustodyTransfer
HandoverToken
ReturnConfirmation
```

architecture.

---

# 77. Dzoka Extension

Claim approval must only unlock the next recovery phase.

It must not be equivalent to physical recovery.

---

# 78. WhatsApp Architecture

The project implements outbound WhatsApp through:

```text id="yp633c"
UltraMsg
```

using a static `Whatsapp` class.

---

# 79. Supported Notification Types

The source includes WhatsApp templates for:

```text id="bs63de"
welcome
QR scanned
possible item match
new claim
claim approved
claim rejected
broadcast
```

---

# 80. Strong Product Lesson

Recovery workflows benefit from proactive messaging.

A user should not need to continuously reopen the app to discover:

```text id="9g6syi"
match
claim
handover event
QR scan
```

---

# 81. WhatsApp Is Outbound Only

The inspected implementation does not provide a first-class inbound WhatsApp conversational interface.

It is essentially:

```text id="i7k72a"
application event
→ WhatsApp notification
```

---

# 82. Dzoka Difference

WhatsApp should be a first-class Dzoka experience:

```text id="2dscnd"
report
search
claim
verification
notifications
handover
```

can all potentially occur through WhatsApp.

---

# 83. Provider Coupling

The source is directly tied to:

```text id="gm3trb"
UltraMsg
```

rather than an abstract messaging provider.

---

# 84. Dzoka Provider Design

Use:

```text id="mqv6ba"
MessagingProvider
├── WhatsAppCloudProvider
├── alternative provider
└── future adapters
```

---

# 85. Silent WhatsApp Failure

If credentials are absent:

```text id="2s2027"
send()
→ false
```

and many calling workflows continue.

Graceful degradation is useful, but delivery outcome must be observable.

---

# 86. Dzoka Notification Attempt

Record:

```text id="6k6isa"
NotificationAttempt
├── provider
├── status
├── providerMessageId
├── attempts
├── error
└── timestamps
```

---

# 87. QR Tags

The application generates user-associated QR tags using cryptographically random bytes.

Conceptually:

```text id="yfnwrb"
random_bytes(5)
→ 10-character hexadecimal tag
```

---

# 88. Positive QR Concept

QR tags allow:

```text id="sbrzj0"
finder
```

to notify:

```text id="ml66re"
owner
```

without requiring the finder to have an account.

This strongly aligns with Dzoka.

---

# 89. Anonymous Finder Experience

The QR landing page allows a finder to provide:

```text id="7n7ryq"
name
contact
location
message
```

and notify the registered owner.

This is a useful low-friction recovery flow.

---

# 90. Owner Privacy

The landing page does not simply expose owner contact information publicly.

Instead:

```text id="wuq323"
finder submits information
→ system notifies owner
```

This is a good privacy direction.

---

# 91. Dzoka Improvement — Secure Relay

Dzoka should go further and avoid unnecessarily revealing:

```text id="2i9j07"
finder contact
owner contact
```

to either party.

Use:

```text id="icb31e"
RecoveryConversation
```

and controlled contact disclosure.

---

# 92. QR Scan Telemetry

`QrScan.php` records:

```text id="y11r3v"
item_id
lat
lng
location_label
IP address
user agent
timestamp
```

---

# 93. Useful Recovery Signal

A genuine scan can be valuable evidence of:

```text id="7e58ar"
possible item sighting
```

and may help an owner locate the item.

---

# 94. Critical Trust Limitation

The endpoint accepts:

```text id="ic0jgm"
item_id
lat
lng
label
```

from the client.

There is no cryptographic proof that:

```text id="z4lf9l"
an actual QR tag was scanned
```

or:

```text id="brsh3j"
GPS coordinates are genuine
```

---

# 95. Dzoka Rule

A scan event should be classified as:

```text id="glhyql"
reported observation
```

not:

```text id="p4ctxv"
verified physical location
```

unless stronger device/tag trust exists.

---

# 96. QR Location Must Not Become Custody

A QR scan at:

```text id="hku51f"
Harare CBD
```

does not prove the item is currently in custody there.

It is only:

```text id="r53xui"
location evidence
```

---

# 97. QR Scan Reward Abuse

If the scanner is logged in, the endpoint awards:

```text id="rv75yr"
5 points
```

for a scan.

The inspected endpoint does not enforce a strong uniqueness/idempotency rule for rewards.

---

# 98. Potential Abuse

A user may repeatedly trigger the scan endpoint for:

```text id="wi4m4a"
same item
```

and repeatedly generate point transactions.

---

# 99. Dzoka Reward Rule

Every reward-producing recovery event must be:

```text id="z63qx2"
idempotent
validated
policy-controlled
abuse-resistant
```

---

# 100. Reward Ledger

The platform maintains:

```text id="ranz5z"
point_transactions
```

with:

```text id="s2f9s1"
user_id
points
action
description
reference_id
created_at
```

This is a useful pattern.

---

# 101. Append-Oriented Reward History

The transaction log is stronger than simply maintaining:

```text id="f4lyjo"
user.points
```

alone.

Dzoka similarly should maintain:

```text id="slsbkx"
RewardLedgerEntry
```

alongside a projected balance.

---

# 102. Badges

The project includes:

```text id="2b43av"
badges
user_badges
```

based on activity thresholds.

This is primarily gamification rather than core recovery infrastructure.

---

# 103. Dzoka Position on Gamification

Badges can encourage:

```text id="v9j7hc"
community participation
```

but must not incentivize abuse such as:

```text id="2ptupj"
fake scans
fake reports
manufactured claims
```

---

# 104. Return Rewards Advertised

The rewards UI advertises points for:

```text id="4gqkdp"
item returned
verified item return
```

---

# 105. Implementation Qualification

In the inspected source, `item_returned` is referenced in reward/badge statistics and UI rules, but no corresponding reward trigger was identified in `Master.php`.

Therefore:

```text id="y2bxmb"
RETURN REWARD:
DESIGNED / REFERENCED
BUT NOT CLEARLY WIRED END-TO-END
```

at the audited revision.

---

# 106. Dzoka Audit Lesson

A reward listed in UI/documentation is not necessarily an implemented recovery event.

Always trace:

```text id="0nt1j4"
business event
→ ledger entry
→ balance
```

in actual source.

---

# 107. Reward Redemption

The endpoint accepts client-supplied:

```text id="y2d9dq"
cost
reward description
```

then checks whether the user has enough points.

---

# 108. Critical Reward Integrity Issue

The server trusts the client-supplied cost.

A malicious client could potentially submit:

```text id="kl31n9"
reward = expensive reward
cost = 1
```

instead of the UI's intended price.

---

# 109. Dzoka Rule

Never trust:

```text id="tvbv1f"
reward name
reward price
reward amount
```

from the client.

The client should submit:

```text id="f73xgb"
rewardId
```

and the server must resolve authoritative cost/policy.

---

# 110. Redemption Race Condition

The workflow performs:

```text id="kqme86"
read balance
    ↓
check balance
    ↓
deduct
    ↓
log
```

without one clear transactional conditional update.

Concurrent redemptions could therefore produce inconsistent outcomes.

---

# 111. Dzoka Reward Transaction

Use an atomic operation such as:

```text id="wv731o"
UPDATE balance
WHERE balance >= cost
```

within a transaction, plus a unique redemption idempotency key.

---

# 112. Reward Fulfilment

Redemptions create an inquiry so an administrator can manually fulfill them.

This is simple and operationally practical.

---

# 113. Dzoka Extension

Model explicitly:

```text id="dcdvjd"
Reward
RewardRedemption
RewardFulfilment
RewardLedgerEntry
```

rather than repurposing generic support inquiries.

---

# 114. Reward vs Finder Incentive

Smart Asset Finder's points system is general gamification.

Dzoka additionally needs explicit recovery reward concepts such as:

```text id="5yx5qc"
owner-offered reward
finder eligibility
release conditions
dispute
payment status
```

---

# 115. Rewards Must Follow Recovery Truth

Reward release should depend on authoritative:

```text id="gq50yb"
RecoveryCompleted
```

or other policy-defined events.

Not:

```text id="bfn1ij"
claim approved
```

or:

```text id="h24e51"
QR scanned
```

alone.

---

# 116. Shop / Hardware

The project includes a shop for:

```text id="1ae4ke"
QR stickers
GPS trackers
BLE tags
```

This is strategically interesting.

---

# 117. Dzoka Item Identity Ecosystem

Dzoka could eventually support:

```text id="j7yrwm"
QR
NFC
RFID
BLE
UWB
digital item passport
```

through a provider/device abstraction.

---

# 118. Hardware Is Optional

Dzoka must continue to work for ordinary untagged objects.

Tags improve recovery.

They must not become a platform requirement.

---

# 119. Paystack

The project implements Paystack verification for card payments.

The server calls:

```text id="cih66k"
transaction/verify/{reference}
```

using its Paystack secret.

This verification step is directionally correct.

---

# 120. Payment Amount Check

The server compares:

```text id="7ccy6x"
Paystack paid amount
```

against an expected amount.

Again, this is directionally correct.

---

# 121. Critical Payment Pricing Issue

However, the expected price is calculated using client-supplied:

```text id="1zaeok"
sku
product
unit_price
qty
```

rather than retrieving authoritative product pricing from the server-side catalog.

---

# 122. Why This Matters

A client could potentially alter:

```text id="nqo4me"
unit_price
```

and both:

```text id="o5hj68"
payment amount
server expected amount
```

could then reflect the tampered value.

---

# 123. Dzoka Payment Rule

Never trust:

```text id="ck2z2e"
unit price
fee
reward amount
currency
```

from the browser.

Submit:

```text id="i4sp2g"
product/reward/provider ID
```

and resolve pricing server-side.

---

# 124. Development Payment Bypass

If the configured Paystack secret is absent or looks like a placeholder, the verification function enters a development mode and assumes payment succeeded.

---

# 125. Production Risk

Development payment bypasses must be impossible in a production environment.

Dzoka should fail startup or payment operations if:

```text id="91mhgp"
production
+
payment provider unavailable
```

rather than silently entering fake-payment mode.

---

# 126. Recovery Payments vs Shop Payments

The source's Paystack implementation concerns the hardware shop.

It is not a complete reference for:

```text id="nveuln"
finder reward escrow
courier payment
recovery reimbursement
```

---

# 127. Dzoka Payment Abstraction

Use:

```text id="3bfc96"
PaymentProvider
```

with:

```text id="bpadfk"
payment authorization
verification
webhooks
idempotency
refund
settlement
```

separate from the reward domain.

---

# 128. Contact Privacy

The README says contact details are shared only with a verified owner.

The actual claim/recovery architecture should be interpreted carefully because there is no complete verification/custody boundary.

---

# 129. QR Finder Contact

In the QR tag workflow, the finder provides:

```text id="c5sr5i"
name
phone/email
```

which is then forwarded to the owner through notifications.

---

# 130. Dzoka Improvement

Prefer:

```text id="vrtnjf"
anonymous/relayed communication
```

where possible.

Allow contact disclosure only with:

```text id="02ppmk"
consent
policy
```

---

# 131. Notification Architecture

The repository is strong as a UX reference for multi-channel notifications.

Events include:

```text id="y82arm"
account registration
QR scan
AI candidate
new claim
claim approved
claim rejected
broadcast
```

---

# 132. Dzoka Event Mapping

Potential Dzoka events:

```text id="0k1u83"
match.candidate-promoted
claim.submitted
claim.approved
verification.completed
qr.sighting-reported
recovery.ready-for-handover
recovery.completed
reward.released
```

should feed the messaging subsystem.

---

# 133. Notifications Must Be Asynchronous

The inspected PHP implementation frequently performs email/WhatsApp calls inline during business operations.

Dzoka should avoid making:

```text id="jodhmh"
claim submission
```

depend on synchronous third-party notification latency.

---

# 134. Correct Dzoka Pattern

```text id="yexos3"
business transaction
    ↓
outbox event
    ↓
messaging worker
    ↓
email / WhatsApp
```

---

# 135. Password Security

The public user flow uses:

```text id="ld5esk"
password_hash()
PASSWORD_BCRYPT
cost 12
```

which is a reasonable baseline.

---

# 136. Session Security

The source includes:

```text id="7ir9ws"
HttpOnly
SameSite=Lax
secure in production
strict session mode
session regeneration
```

which are useful security practices.

---

# 137. CSRF

The application generates a cryptographically random CSRF token and checks POST requests routed through the relevant dispatcher.

This is a positive implementation feature.

---

# 138. But Endpoint Coverage Matters

Standalone endpoints such as:

```text id="kr3w0k"
QrScan.php
```

must be individually assessed.

Security middleware in one dispatcher does not automatically protect independent entrypoints.

---

# 139. Default Admin Credentials

README documents a default administrator:

```text id="mxj57a"
admin
admin123
```

and instructs users to change it.

This is unsafe as a production default.

---

# 140. Dzoka Rule

Production deployments must never start with:

```text id="qwwjm7"
known default privileged credentials
```

---

# 141. Account Verification

The project implements email-verification tokens using:

```text id="19g8z1"
random_bytes()
```

which is good.

---

# 142. Password Reset

Reset tokens are also generated cryptographically and expire.

This is directionally sound.

---

# 143. Password Reset Token Storage

Reset tokens appear stored directly in the database.

Dzoka should preferably store:

```text id="3ab36r"
hash(token)
```

rather than bearer reset tokens in plaintext at rest.

---

# 144. QR Code Entropy

QR codes use:

```text id="4xu964"
40 bits
```

of random value from `random_bytes(5)`.

This may be adequate for some low-risk identification use cases but should be formally threat-modeled before becoming a security credential.

---

# 145. QR Tag ≠ Authorization Token

In Dzoka:

```text id="dkcp22"
item tag
```

should identify or route to an item.

It should not automatically grant:

```text id="uab1h5"
ownership
custody access
private data
recovery authorization
```

---

# 146. QR Identity vs Handover Credential

Separate:

```text id="mx5hkd"
ItemIdentifier
```

from:

```text id="p673tl"
HandoverCredential
```

They solve different problems.

---

# 147. QR Scan Privacy

The source records:

```text id="80envc"
IP address
user agent
GPS
```

for scans.

This is potentially sensitive telemetry.

---

# 148. Dzoka Privacy Requirement

Scan telemetry requires:

```text id="nd7n73"
purpose limitation
retention policy
visibility rules
consent/legal basis where applicable
```

---

# 149. Exact GPS Exposure

Do not automatically expose:

```text id="5u08l2"
scanner exact GPS
```

to every owner or user.

Use policy and potentially reduced precision.

---

# 150. Matching and QR Should Remain Separate

A QR scan can provide:

```text id="mzp9x6"
direct item identity
```

whereas lost/found matching performs:

```text id="rr0kvp"
probabilistic identity resolution
```

These should converge into recovery workflows without being conflated.

---

# 151. AI Model Provenance Gap

The model is hard-coded as:

```text id="a0l1kr"
claude-haiku-4-5-20251001
```

but match results are not stored with comprehensive:

```text id="tfu826"
model
prompt version
input hash
configuration
provider response
```

provenance.

---

# 152. Dzoka Requirement

Every AI-assisted match or verification result should reference:

```text id="k2uk0h"
ModelRun
```

---

# 153. Prompt-Version Gap

Claude prompts are embedded directly inside PHP methods.

Changes to source change decision behavior without explicit:

```text id="nroyxs"
promptVersion
```

tracking.

---

# 154. Dzoka Rule

High-impact prompts should be versioned and traceable like model configuration.

---

# 155. LLM Injection / Data Boundary

User-provided title, description, and challenge answers are interpolated directly into prompts.

This is another reason LLM output must remain advisory.

---

# 156. Dzoka LLM Input Handling

Treat all user content as untrusted data and use:

```text id="pmux4p"
structured prompts
output schemas
bounded tools
post-validation
```

where LLMs are involved.

---

# 157. No Evidence Coverage

Smart Asset Finder does not provide Dzoka's explicit:

```text id="j67mcb"
EvidenceCoverage
```

model.

A score can therefore look numerically strong even when based on limited evidence.

---

# 158. No Hard Conflict Engine

There is no explicit mechanism ensuring:

```text id="x2unqg"
verified serial mismatch
```

overrides:

```text id="e5ipfe"
high description similarity
```

---

# 159. Dzoka Correction

Keep:

```text id="oxh0g6"
ConflictEngine
```

separate from general similarity.

---

# 160. No Calibrated Confidence

The source uses AI-generated/heuristic scores such as:

```text id="ygzfpp"
AI Ownership Score 82%
```

but they are not calibrated probabilities.

---

# 161. Dzoka UI Rule

Avoid displaying:

```text id="mtitxq"
82%
```

in a way that users naturally interpret as:

```text id="z2rbaz"
82% probability owner
```

unless calibration actually supports that interpretation.

---

# 162. No Matching Run

No explicit immutable `MatchingRun` was identified for the text match process.

This limits reproducibility.

---

# 163. No Candidate History

Only the current best score appears to be stored on the found item.

Future model changes could therefore make historical decisions difficult to reconstruct.

---

# 164. No Federation

The system is centralized.

It does not implement:

```text id="w9txll"
peer nodes
cross-institution search
node trust
signed requests
federated claim verification
```

---

# 165. No Custody Model

The platform has claim/item statuses but no complete physical chain-of-custody state machine.

---

# 166. No Courier Recovery Architecture

The shop has order delivery.

That should not be mistaken for:

```text id="vb0bcn"
lost-item recovery logistics
```

---

# 167. No Recovery Handover Protocol

There is no robust:

```text id="z8v29f"
handover OTP
handover QR
two-sided confirmation
custody transfer
```

for the recovered item itself.

---

# 168. No EPCIS / Traceability

The platform does not offer standards-based physical recovery traceability.

Dzoka handles that separately.

---

# 169. What Smart Asset Finder Does Best

The source is strongest as a reference for combining:

```text id="yzmgl5"
consumer reporting
QR tags
maps
notifications
WhatsApp
rewards
hardware
claims
payments
```

into a coherent product experience.

---

# 170. What It Does Not Do Best

It is not a strong source for:

```text id="ol0jbd"
multimodal matching
same-instance retrieval
ownership verification security
custody
federation
calibrated AI
event-driven architecture
```

---

# 171. Comparison with globalenglish01/lostfound

`globalenglish01/lostfound` is substantially stronger for:

```text id="65wwnn"
matching architecture
hard conflicts
evidence coverage
missing evidence
model/version provenance
```

Smart Asset Finder is stronger as inspiration for:

```text id="wq3y5n"
QR ecosystem
WhatsApp notifications
rewards
hardware commerce
consumer recovery UX
```

---

# 172. Comparison with FuricBond/Lost-And-Found

FuricBond provides much stronger visual matching concepts:

```text id="6zh8nk"
YOLO
OpenCLIP
pHash
ORB
```

Smart Asset Finder's matching is primarily textual.

---

# 173. Comparison with La2eet

La2eet provides a clearer:

```text id="cofwtk"
finder-private-question
→ claimant answer
→ human comparison
```

verification workflow.

Smart Asset Finder adds:

```text id="xbn2es"
LLM scoring
proof files
admin review
```

but its private-answer handling and workflow linkage are weaker.

---

# 174. Comparison with Lost-and-Found-Tracking-System

`lost-and-found-system` is stronger for:

```text id="j2utl8"
claims
claim conflicts
fraud architecture
institutional operation
audit
```

Smart Asset Finder is stronger for:

```text id="pshzkd"
QR tags
WhatsApp
reward gamification
hardware ordering
```

---

# 175. Dzoka Synthesis

Useful combined lessons become:

```text id="34kspa"
globalenglish
→ matching discipline

FuricBond
→ visual identity

La2eet
→ private challenges

Lost-and-Found-Tracking-System
→ institutional claims/risk

Smart Asset Finder
→ QR + messaging + incentives + hardware ecosystem
```

---

# 176. Dzoka Mapping — QR

Smart Asset Finder:

```text id="pwq2pm"
QR Tag
→ Finder Form
→ Owner Notification
```

Dzoka:

```text id="jhthbj"
ItemIdentity
    ↓
QR/NFC Resolver
    ↓
Anonymous Recovery Session
    ↓
Secure Relay
    ↓
Evidence / Location
    ↓
Recovery Workflow
```

---

# 177. Dzoka Mapping — WhatsApp

Smart Asset Finder:

```text id="p9xdgb"
Event
→ UltraMsg
→ outbound notification
```

Dzoka:

```text id="9xilkg"
Messaging Gateway
├── inbound WhatsApp
├── outbound WhatsApp
├── conversation state
├── interactive flows
├── provider abstraction
└── delivery receipts
```

---

# 178. Dzoka Mapping — Rewards

Smart Asset Finder:

```text id="dvg6bz"
Action
→ points
→ badge
→ redemption
```

Dzoka:

```text id="8f0hda"
Verified Recovery Event
        ↓
Reward Policy
        ↓
RewardLedger
        ↓
Eligibility
        ↓
RewardRelease
        ↓
PaymentProvider
```

---

# 179. Dzoka Mapping — Ownership

Smart Asset Finder:

```text id="28t5g2"
security answers
→ AI score
→ admin review
```

Dzoka:

```text id="7sfz53"
typed evidence
+
private challenges
+
identifiers
+
prior media
+
risk
+
policy
+
human review
→ VerificationDecision
```

---

# 180. Dzoka Mapping — AI Matching

Smart Asset Finder:

```text id="6vnzj4"
same category
+
last 90 days
+
description text
→ Claude score
```

Dzoka:

```text id="k5hh91"
hybrid retrieval
+
global visual
+
local visual
+
OCR
+
text
+
attributes
+
location
+
time
+
protected identifiers
+
hard conflicts
+
coverage
→ calibrated match confidence
```

---

# 181. Architecture Documents Influenced

This audit should inform:

```text id="v2qaqp"
docs/architecture/whatsapp-architecture.md
docs/architecture/logistics-architecture.md
docs/architecture/rewards-architecture.md
docs/architecture/integration-architecture.md
docs/architecture/privacy-architecture.md
docs/architecture/security-architecture.md

docs/design/ownership-verification.md
docs/design/claim-workflow.md
docs/design/custody-state-machine.md
docs/design/item-fingerprint.md
```

---

# 182. Potential ADRs Influenced

Potential decisions include:

```text id="4nzs22"
QR as optional item identity mechanism

anonymous finder relay

MessagingProvider abstraction

rewards tied to verified recovery events

server-authoritative reward pricing

server-authoritative payment pricing

secret ownership evidence cannot be sent to external AI by default
```

---

# 183. What Dzoka Should Adopt Directly

Because the source is all-rights-reserved:

```text id="v9skk9"
DIRECT SOURCE ADOPTION:
NONE
```

---

# 184. What Dzoka Should Adapt Independently

Concepts worth independently implementing:

```text id="ccwm80"
QR asset identity

no-account finder flow

QR sighting events

proactive owner notifications

WhatsApp notification events

claim proof upload

reward ledger

community incentives

hardware identity ecosystem

travel context

multi-channel recovery UX
```

---

# 185. What Dzoka Should Reference

Reference the source for:

```text id="me19gg"
consumer QR UX

map UX

reward UX

WhatsApp message templates

hardware-tag product ideas

claim review presentation

multi-channel notifications
```

---

# 186. Explicitly Reject — Matching

Do not copy:

```text id="kihymp"
same-category hard gate

90-day universal hard gate

LLM-generated number as identity confidence

single mutable AI match score

text-only matching marketed as complete AI identity matching
```

---

# 187. Explicitly Reject — Verification

Do not copy:

```text id="izh6io"
plaintext normalized expected answers

external LLM access to secret ownership evidence by default

AI Verified ownership label

automatic rejection based only on semantic answer score

lost/found security-question linkage inconsistency
```

---

# 188. Explicitly Reject — QR

Do not assume:

```text id="2m4b7y"
QR endpoint call
=
physical scan
```

or:

```text id="1f2r1i"
client GPS
=
verified location
```

---

# 189. Explicitly Reject — Rewards

Do not copy:

```text id="t8rmes"
client-supplied reward cost

repeatable scan rewards without idempotency

non-transactional redemption balance check

generic inquiry as reward fulfilment state
```

---

# 190. Explicitly Reject — Payments

Do not copy:

```text id="s3a9us"
client-supplied authoritative unit price

production payment bypass

shop payment logic as recovery escrow
```

---

# 191. Explicitly Reject — Secrets

Do not expose:

```text id="ty7rq9"
serial numbers
security answers
precise finder location
finder contact
```

outside explicitly authorized projections.

---

# 192. Positive Security Patterns

The project does contain several good security habits:

```text id="466vi7"
prepared statements
bcrypt passwords
session regeneration
HttpOnly cookies
SameSite cookies
CSRF token
login/registration rate limiting
random verification tokens
admin guards
MIME checks
```

These are worth recognizing even though the overall platform needs stronger trust architecture.

---

# 193. Significant Security Risks

Key risks identified:

```text id="7v8lzb"
all-rights-reserved licensing

plaintext secret Q&A

private evidence sent to third-party AI

LLM score used as ownership signal

QR sightings not cryptographically trustworthy

reward farming potential

client-authoritative redemption cost

client-authoritative shop pricing

development payment bypass

proof file extension handling risk

known default admin credentials
```

---

# 194. AI Findings Summary

### Implemented

```text id="ayjlag"
Claude text lost↔found scoring

Claude challenge-answer scoring

Claude spam/fake report classifier

PHP text-similarity fallback
```

### Not implemented in core matcher

```text id="bf039y"
visual matching

OCR

local features

image embeddings

geo ranking

temporal scoring

multimodal fusion
```

---

# 195. QR Findings Summary

### Strong ideas

```text id="by9jzk"
random item tags

anonymous finder route

owner privacy

owner notifications

location sightings

scan history
```

### Weaknesses

```text id="dyy0ha"
client-provided GPS

scan event not cryptographically proven

IP/device telemetry privacy

scan reward farming risk

no custody proof
```

---

# 196. Rewards Findings Summary

### Strong ideas

```text id="a0n9sz"
append-oriented points log

balance projection

badges

redemption UX

community incentives
```

### Weaknesses

```text id="5noa72"
client chooses cost

race-prone redemption

weak idempotency

return rewards not clearly wired

no explicit reward escrow/release
```

---

# 197. WhatsApp Findings Summary

### Strong

```text id="6ne2bm"
real outbound integration

match alerts

claim alerts

QR alerts

approval/rejection alerts
```

### Weak

```text id="cf6lpa"
UltraMsg-specific coupling

no inbound workflow

no durable delivery state

mostly synchronous

provider failures easily disappear
```

---

# 198. Claim Findings Summary

### Strong

```text id="wi0an7"
claim model

proof upload

security questions

admin review

review notes

notification flow
```

### Weak

```text id="oxlqbm"
question relationship bug

AI terminology too strong

low-score auto rejection

no competing-claim domain

no recovery authorization

no custody/handover
```

---

# 199. Product Lessons

Smart Asset Finder demonstrates that a recovery network can extend beyond:

```text id="iz4wod"
search listings
```

into:

```text id="m4ql0m"
physical tags
notifications
community participation
hardware
rewards
recovery discovery
```

This is strategically useful for Dzoka.

---

# 200. Strong Dzoka Opportunity — Registered Assets

The QR design suggests a valuable optional Dzoka capability:

```text id="rsvfsb"
register valuable item
    ↓
assign QR/NFC identity
    ↓
item becomes recoverable before it is ever lost
```

---

# 201. Proactive Recovery Model

Normal lost-and-found:

```text id="8o0ewb"
LOSE
→ REPORT
→ SEARCH
```

registered Dzoka asset:

```text id="vez8pm"
REGISTER
→ TAG
→ LOSE
→ FINDER SCANS
→ RECOVERY WORKFLOW
```

This can drastically shorten recovery time.

---

# 202. Digital Item Passport Direction

The same abstraction may later support:

```text id="b8k5hk"
QR
NFC
manufacturer serial
institutional asset ID
digital product passport
```

behind:

```text id="eshym8"
ItemIdentityProvider
```

---

# 203. But Avoid Public Ownership Leakage

A QR tag should not expose:

```text id="g77hji"
owner name
phone
home address
email
```

to anyone who scans it.

---

# 204. Safe QR Response

A scanner might see:

```text id="jtv183"
This item is registered with Dzoka.

Would you like to report that you found it?
```

rather than:

```text id="60nbqg"
This belongs to John Smith, phone...
```

---

# 205. Recovery Incentive Opportunity

Smart Asset Finder also reinforces that finders can be motivated through:

```text id="3i9y5g"
recognition
points
rewards
community reputation
```

---

# 206. Dzoka Must Prevent Perverse Incentives

Incentives must not encourage:

```text id="4rsacg"
keeping found objects
fake scans
fake sightings
fake reports
collusion
self-recovery fraud
```

---

# 207. Reward Eligibility

Reward eligibility may require:

```text id="kpw28k"
verified finder contribution
+
confirmed recovery
+
no fraud hold
+
reward policy
```

---

# 208. Source Audit Lock Entry

Recommended entry:

```json id="dt8z1v"
{
  "id": "smart-asset-finder",
  "category": "platforms",
  "repository": "https://github.com/fredopoku/Smart-Asset-Finder",
  "revision": "8ac4484354d23aea42e1a569e2f127a878049b1e",
  "license": "ALL_RIGHTS_RESERVED",
  "classification": "REFERENCE",
  "audit": "docs/source-audit/platforms/smart-asset-finder.md",
  "auditedAt": "2026-10-04"
}
```

---

# 209. Re-Audit Triggers

Re-audit if upstream:

```text id="ydsd6t"
adds an open-source license

changes security-question architecture

adds real multimodal matching

protects private answers cryptographically

changes QR scan trust model

adds proper recovery handover

adds courier recovery

implements transactional reward redemption

changes payment verification/pricing

adds multi-tenancy
```

---

# 210. Final Classification

```text id="zwot8y"
PRIMARY:
REFERENCE ONLY
```

because:

```text id="a68uga"
ALL RIGHTS RESERVED
```

---

# 211. Most Valuable Ideas

Carry forward conceptually:

```text id="pa3uqr"
QR-first item recovery

anonymous finder workflow

registered/pre-tagged assets

WhatsApp recovery notifications

multi-channel notifications

claim proof uploads

travel context

reward ledger concepts

community incentives

hardware identity ecosystem
```

---

# 212. Most Important Negative Lessons

Do not inherit:

```text id="j1uj2e"
LLM score = ownership confidence

normalized plaintext = protected secret

client GPS = verified physical truth

QR request = verified scan

UI reward price = trusted reward price

client product price = trusted payment price

claim approved = enough for physical recovery
```

---

# 213. Final Assessment

Smart Asset Finder is not the repository from which Dzoka should derive its core matching or verification engine.

Its strongest contribution is elsewhere.

It demonstrates how a lost-property system can extend into a **physical recovery ecosystem**:

```text id="lxvd9n"
ITEM
  ↓
REGISTER / REPORT
  ↓
QR IDENTITY
  ↓
FINDER INTERACTION
  ↓
LOCATION SIGNAL
  ↓
OWNER NOTIFICATION
  ↓
CLAIM
  ↓
PROOF
  ↓
REVIEW
  ↓
COMMUNICATION
  ↓
REWARD / INCENTIVE
```

Dzoka should take that broader product idea and place it inside much stronger trust boundaries:

```text id="ywg3vu"
ITEM IDENTITY
      ↓
ANONYMOUS FINDER RELAY
      ↓
UNTRUSTED SIGHTING EVIDENCE
      ↓
MATCHING
      ↓
CLAIM
      ↓
PRIVATE VERIFICATION
      ↓
RISK
      ↓
RECOVERY AUTHORIZATION
      ↓
CUSTODY HANDOVER
      ↓
CONFIRMED RECOVERY
      ↓
REWARD ELIGIBILITY
      ↓
REWARD RELEASE
```

The central conclusion is:

> **Smart Asset Finder is a valuable product reference for QR-assisted recovery, WhatsApp notifications, registered assets, proof uploads, and incentives—but Dzoka must independently implement those ideas with protected ownership evidence, server-authoritative payments and rewards, trustworthy event semantics, and strict separation between sightings, matching, verification, custody, and recovery.**