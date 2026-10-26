# Source Audit — Malak-Rashad/LostAndFound

> **Dzoka — The Open Recovery Network**
>
> Source audit of `Malak-Rashad/LostAndFound`, also presented as **La2eet (لقيت)**, with emphasis on claims, finder-defined private verification questions, human ownership review, post-approval communication, recovery state, authorization boundaries, and lessons for Dzoka's verification architecture.

---

# 1. Audit Metadata

| Field | Value |
|---|---|
| Repository | `https://github.com/Malak-Rashad/LostAndFound` |
| Upstream owner | `Malak-Rashad` |
| Product name | La2eet / لقيت |
| Repository name | LostAndFound |
| Audited revision | `d5d7bfaf043396a32b40c264c647f6456cadb633` |
| Short revision | `d5d7bfaf` |
| Revision date | 2026-07-21 |
| Audit date | 2026-10-04 |
| Default branch | `master` |
| Backend | ASP.NET Core MVC / .NET 9 |
| ORM | Entity Framework Core 9 |
| Database | Microsoft SQL Server |
| Authentication | ASP.NET Core Identity |
| External authentication | Google OAuth |
| Real-time communication | SignalR |
| Email | MailKit / MimeKit |
| Frontend | Razor Views + Bootstrap + JavaScript |
| License | **UNRESOLVED — no root project license found** |
| Primary classification | **REFERENCE** |
| Secondary classification | **ADAPT VERIFICATION CONCEPTS** |
| Direct source reuse | **NO — until licensing is clarified** |

---

# 2. Relationship to `la2eet.md`

This repository is the same source audited in:

```text
docs/source-audit/platforms/la2eet.md
```

The two names refer to the same upstream project:

```text
Repository:
Malak-Rashad/LostAndFound

Product:
La2eet
```

This file uses the **repository identity** as its canonical name:

```text
malak-lostandfound.md
```

To avoid contradictory future audits, Dzoka should ideally designate one file as canonical.

Recommended:

```text
CANONICAL:
docs/source-audit/platforms/malak-lostandfound.md

ALIAS / PRODUCT REFERENCE:
docs/source-audit/platforms/la2eet.md
```

---

# 3. Executive Decision

The repository provides a genuinely useful lost-property verification concept:

```text
finder records facts that are not publicly disclosed
        ↓
claimant answers private questions
        ↓
finder compares claimant answer with expected answer
        ↓
finder approves or rejects claim
```

That concept directly informs Dzoka's:

```text
PrivateEvidence
VerificationChallenge
ChallengeResponse
VerificationSession
VerificationDecision
```

However, several implementation details are unsafe or incomplete.

Most importantly:

```text
ExpectedAnswerHash
```

is only a misleading field name.

The audited implementation does **not** cryptographically hash the expected answer.

Instead:

```text
finder enters expected answer
        ↓
plaintext stored in database
        ↓
plaintext retrieved
        ↓
plaintext displayed to reviewer
```

The source also contains major authorization weaknesses in:

```text
SignalR messaging
mark-as-returned flow
```

Therefore:

```text
PRIMARY DECISION:
REFERENCE

SECONDARY:
ADAPT PRIVATE VERIFICATION WORKFLOW CONCEPTS
```

---

# 4. Why This Repository Matters

Many lost-and-found platforms ask only:

```text
Does the claimant say this item belongs to them?
```

This project introduces a much stronger idea:

> Ask the claimant to demonstrate knowledge about the item that is not available from the public listing.

Examples include:

```text
hidden markings
contents
engraving
private cosmetic damage
case details
lock-screen details
labels
stickers
```

That concept is fundamental to Dzoka's ownership-verification system.

---

# 5. Audit Scope

Important implementation areas inspected include:

```text
README.md

LostAndFound/
├── Models/
│   ├── Item.cs
│   ├── Claim.cs
│   ├── VerificationQuestion.cs
│   └── User.cs
│
├── Models/ViewModels/
│   └── VerificationQuestionViewModel.cs
│
├── Services/
│   ├── ItemService.cs
│   ├── ClaimsService.cs
│   └── ChatService.cs
│
├── Controllers/
│   ├── ItemsController.cs
│   ├── ClaimsController.cs
│   ├── MessagesController.cs
│   └── AuthController.cs
│
├── Repositories/
│   ├── ItemRepository.cs
│   └── ChatRepository.cs
│
├── Hubs/
│   └── ChatHub.cs
│
├── ModelConfigrations/
│   ├── VerificationQuestionConfiguration.cs
│   └── ClaimConfiguration.cs
│
├── Views/Claims/
│   ├── Create.cshtml
│   └── Review.cshtml
│
├── Enums/
│   ├── ClaimStatus.cs
│   ├── ItemType.cs
│   └── ReportStatus.cs
│
└── Program.cs
```

---

# 6. License Finding

At revision:

```text
d5d7bfaf043396a32b40c264c647f6456cadb633
```

no root:

```text
LICENSE
```

file was found.

The presence of licenses belonging to dependencies does not license the application itself.

---

# 7. Dzoka License Classification

Until the author adds or clarifies a project license:

### Permitted for Dzoka

```text
inspect architecture
study workflows
document lessons
independently implement concepts
```

### Not permitted as normal OSS reuse

```text
copy application source
port substantial functions
copy views/controllers/services
treat public GitHub visibility as permission
```

Therefore:

```text
DIRECT_CODE_REUSE = false
```

---

# 8. High-Level Architecture

The application follows a conventional ASP.NET Core architecture:

```text
Browser
   ↓
Razor / MVC
   ↓
Controllers
   ↓
Services / Repositories
   ↓
Entity Framework Core
   ↓
SQL Server
```

Supporting systems include:

```text
ASP.NET Identity
Google OAuth
SignalR
MailKit
```

---

# 9. Main Product Domains

The repository covers:

```text
users
authentication
lost/found reports
moderation
search
claims
verification questions
messaging
recovery
administration
```

This makes it more complete than many single-feature lost-and-found demos.

---

# 10. Item Model

The item model includes approximately:

```text
Item
├── ItemId
├── Title
├── Description
├── City
├── Location
├── Status
├── ReportStatus
├── ImageBase64
├── CreatedAt
├── LostOrFoundDate
├── IsResolved
├── UserId
├── VerificationQuestions[]
├── Claims[]
└── Messages[]
```

---

# 11. Report Type vs Recovery State

The item type enum includes:

```text
Lost
Found
Returned
```

This mixes two different semantic dimensions.

`Lost` and `Found` describe:

```text
report perspective
```

while `Returned` describes:

```text
recovery lifecycle
```

---

# 12. Dzoka Correction

Dzoka separates:

```text
ReportType
├── LOST
└── FOUND
```

from:

```text
CaseState
CustodyState
RecoveryState
```

This avoids contradictory combinations and overloaded enums.

---

# 13. Moderation State

The repository separately defines:

```text
Pending
Approved
Rejected
```

for report moderation.

This separation is useful.

For example:

```text
report type = FOUND
moderation state = APPROVED
```

is semantically clean.

---

# 14. Verification Question Model

The key model is:

```text
VerificationQuestion
├── QId
├── QuestionText
├── ExpectedAnswerHash
├── CreatedAt
└── ItemId
```

This is the most Dzoka-relevant part of the repository.

---

# 15. Found Reports Require Verification Questions

When creating a found report, the service requires at least one valid verification question.

Conceptually:

```text
FOUND REPORT
    ↓
PRIVATE VERIFICATION QUESTION REQUIRED
```

This is a strong product decision.

It ensures recovery verification is considered at report creation rather than being added only after someone claims the item.

---

# 16. Private-Knowledge Verification

A finder may create questions such as:

```text
What is engraved inside the ring?

What sticker is underneath the laptop?

What colour is the phone case?

What is inside the front pocket?

What damage exists near the camera?
```

The expected answer remains hidden from the claimant.

---

# 17. Why This Pattern Is Valuable

Suppose the public report says:

```text
Black iPhone found near library.
```

Thousands of users could plausibly claim it.

But a challenge such as:

```text
Describe the lock-screen image.
```

may distinguish the real owner.

That is stronger evidence.

---

# 18. Critical Finding — Expected Answer Is Not Hashed

Despite the property name:

```text
ExpectedAnswerHash
```

the source simply stores the submitted value.

There is no demonstrated:

```text
bcrypt
PBKDF2
Argon2
SHA-256
HMAC
encryption
```

applied before persistence.

---

# 19. Storage Reality

The field is configured as an ordinary:

```text
nvarchar
```

database value.

The finder enters the answer directly through the form.

The same value is later displayed to the reviewer.

Therefore:

```text
ExpectedAnswerHash
```

should be interpreted as:

```text
ExpectedAnswerPlaintext
```

in the audited revision.

---

# 20. Important Audit Lesson

Security-sensitive names do not prove security behavior.

Fields such as:

```text
passwordHash
answerHash
encryptedSecret
```

must be traced through their actual write/read path.

---

# 21. Dzoka Secret Storage

Private expected evidence must be protected according to comparison requirements.

For exact deterministic checks:

```text
normalized value
    ↓
keyed HMAC
```

may be appropriate.

For reviewer-accessible semantic evidence:

```text
envelope encryption
```

may be required.

---

# 22. Why Plain Hashing Is Insufficient

Private answers often have low entropy:

```text
blue
Samsung
three
red
Mickey Mouse
```

A normal unsalted hash is therefore vulnerable to dictionary guessing.

Dzoka must not simply implement:

```text
SHA256(answer)
```

and consider the evidence safe.

---

# 23. Claimant Answer Storage

The claim model stores:

```text
VerificationAnswer
```

as a single string.

Multiple answers are concatenated using:

```text
|
```

as a delimiter.

---

# 24. Example

Conceptually:

```text
blue|AS initials|small crack near camera
```

could represent responses to three questions.

---

# 25. Why Delimited Storage Is Weak

This representation creates problems such as:

```text
delimiter inside answer
question reordering
question deletion
answer-count mismatch
no answer IDs
no per-answer metadata
no individual classification
no per-answer evaluation
```

---

# 26. Dzoka Replacement

Each answer is a first-class entity:

```text
ChallengeResponse
├── id
├── verificationSessionId
├── challengeId
├── rawResponse
├── normalizedResponse
├── interpretedResponse
├── submittedAt
├── evaluation
└── provenance
```

---

# 27. Claim Workflow

The repository implements approximately:

```text
FOUND ITEM
    ↓
finder creates private questions
    ↓
claimant opens claim
    ↓
claimant answers questions
    ↓
claim status = Pending
    ↓
finder reviews
    ↓
Approve / Reject
```

This is a valuable baseline workflow.

---

# 28. Claim Status

The state model contains:

```text
Pending
Approved
Rejected
```

This is adequate for a small application.

Dzoka requires more lifecycle precision.

---

# 29. Dzoka Claim Lifecycle

A richer model may include:

```text
DRAFT
SUBMITTED
VERIFICATION_PENDING
UNDER_REVIEW
CONFLICT
APPROVED
REJECTED
WITHDRAWN
CANCELLED
COMPLETED
```

according to final domain decisions.

---

# 30. Self-Claim Protection

The application correctly blocks:

```text
reporter
→ claim own report
```

This is a useful invariant.

---

# 31. Duplicate Claim Protection

It also checks whether the same user already has a claim for the item.

This reduces simple duplicate/spam claims.

---

# 32. Claim Answer Completeness

If verification questions exist, the claimant must answer them before submission.

This is a sensible workflow requirement.

---

# 33. Human Verification

The application does not automatically decide whether answers are correct.

Instead:

```text
finder sees expected answer
+
claimant response
```

and decides manually.

---

# 34. Important Positive Principle

This means La2eet does **not** make the mistake of assuming:

```text
text similarity
=
ownership
```

Human judgment remains in the loop.

Dzoka should preserve that authority separation.

---

# 35. Dzoka Enhancement

Dzoka may introduce AI for:

```text
normalization
semantic interpretation
evidence summarization
risk detection
challenge generation
```

but:

```text
AI cannot independently authorize physical item release.
```

---

# 36. Claim Review Authorization

The claim review path checks that:

```text
claim.Item.UserId == currentUser.Id
```

before allowing ordinary users to review/approve/reject.

This is a good server-side authorization pattern.

---

# 37. Expected Answer Privacy from Claimant

The claimant form exposes:

```text
QuestionText
```

without intentionally exposing:

```text
ExpectedAnswerHash
```

This is directionally correct.

---

# 38. Challenge Leakage Risk

A private question itself may accidentally reveal the evidence.

Bad:

```text
What colour is the red triangular sticker?
```

This leaks:

```text
red
triangle
sticker
```

before the user answers.

---

# 39. Dzoka Disclosure Guard

Challenge creation must pass through:

```text
DisclosureGuard
```

which verifies that the challenge does not expose:

```text
the expected answer
private attributes
protected identifiers
previously undisclosed evidence
```

---

# 40. Public Information Must Not Become Private Proof

A question should not ask for information already visible in:

```text
public description
public image
public OCR
public attributes
finder comments
```

Such evidence has little ownership value.

---

# 41. Disclosure History

Dzoka should track:

```text
PrivateEvidence
├── disclosureState
├── disclosedAt
├── disclosedTo
├── reason
└── challengeHistory
```

so repeated challenge attempts do not progressively leak the answer.

---

# 42. Attempt Limits

La2eet indirectly limits repeated claims by restricting duplicate user/item claims.

However, it does not implement a full:

```text
challenge-attempt
cooldown
rotation
risk-escalation
```

architecture.

---

# 43. Dzoka Challenge Attempt Model

Possible controls include:

```text
max attempts
cooldown window
account/device correlation
challenge rotation
manual review escalation
fraud scoring
```

---

# 44. Review UX

The review screen presents:

```text
question
expected answer
claimant answer
```

side by side.

This is operationally simple and useful.

---

# 45. Dzoka Reviewer Experience

Dzoka can expand this into:

```text
Challenge
    ↓
Expected protected evidence
    ↓
Claimant response
    ↓
Normalized interpretation
    ↓
Evidence strength
    ↓
Risk signals
    ↓
Reviewer decision
```

---

# 46. Decision Audit Gap

The upstream workflow primarily changes:

```text
ClaimStatus
```

on approval/rejection.

It does not provide the richer decision object Dzoka requires.

---

# 47. Dzoka Verification Decision

Persist:

```text
VerificationDecision
├── reviewerId
├── outcome
├── reason
├── evidenceConsidered
├── riskAssessmentId
├── policyVersion
├── decidedAt
└── overrideMetadata
```

---

# 48. Competing Claims

The repository does not provide a strong first-class:

```text
ClaimConflict
```

workflow.

Dzoka must explicitly support cases where two plausible claimants exist.

---

# 49. Recovery Hold

A serious competing claim may require:

```text
RecoveryHold
```

that prevents:

```text
handover
courier dispatch
reward release
```

until resolved.

---

# 50. Approval Does Not Immediately Mean Returned

One positive property is that approving a claim does not automatically mark the item physically returned.

Instead, approval enables communication.

This distinction is useful.

---

# 51. Verified Chat Concept

The product flow is roughly:

```text
claim approved
    ↓
communication enabled
    ↓
finder + claimant arrange return
```

This is conceptually appropriate.

---

# 52. Critical Finding — Chat Authorization Is Weak

The implementation does not securely enforce the approved relationship throughout the messaging backend.

The UI may hide/show chat based on claim state, but the server-side SignalR layer has important gaps.

---

# 53. Client-Supplied Sender Identity

`ChatHub.SendMessage()` accepts fields including:

```text
senderId
receiverId
itemId
message
```

from the client.

The server does not reliably derive authoritative:

```text
senderId
```

from the authenticated SignalR principal.

---

# 54. Dzoka Security Rule

Never trust:

```text
senderId
userId
tenantId
role
```

supplied by the client when the authenticated server context can determine them.

---

# 55. Approved-Relationship Check Missing

The inspected hub does not robustly verify:

```text
approved claim exists
```

between the sender, receiver, and item before persisting/sending the message.

---

# 56. Global SignalR Broadcast

The implementation uses:

```text
Clients.All
```

for message broadcasting.

That is inappropriate for a private recovery conversation.

---

# 57. Dzoka Messaging Model

Use:

```text
AuthenticatedPrincipal
        ↓
ConversationMembershipPolicy
        ↓
RecoveryConversation
        ↓
authorized participants only
```

---

# 58. RecoveryConversation

A Dzoka recovery conversation should contain:

```text
conversationId
caseId
claimId
members
membership roles
createdAt
closedAt
policy
```

and every message must be authorized against it.

---

# 59. Contact Privacy

The conversation layer should allow parties to coordinate recovery without automatically exposing:

```text
phone
email
home address
```

unless explicit policy/consent allows disclosure.

---

# 60. Mark-As-Returned Flow

The application includes a server route that eventually calls:

```text
MarkItemAsReturned()
```

which performs approximately:

```text
Status = Returned
IsResolved = true
```

---

# 61. Critical Authorization Finding

The inspected endpoint does not sufficiently verify:

```text
authorized actor
approved claim
custody holder
handover evidence
```

before changing recovery state.

---

# 62. UI Authorization Is Not Security

Even if only certain users see:

```text
Mark Returned
```

in the UI, the server endpoint must independently validate the actor.

Dzoka adopts this as a formal anti-pattern:

```text
UI authorization ≠ server authorization
```

---

# 63. Dzoka Recovery Authority

Sensitive commands such as:

```text
ApproveClaim
AuthorizeRecovery
TransferCustody
ConfirmHandover
CompleteRecovery
ReleaseReward
```

must each enforce server-side policy.

---

# 64. Recovery State Is Too Shallow

The repository effectively moves from:

```text
Claim Approved
→ Communication
→ Returned
```

without a formal:

```text
recovery authorization
handover
custody transfer
recipient confirmation
```

domain.

---

# 65. Dzoka Recovery Sequence

```text
Claim Approved
        ↓
VerificationDecision
        ↓
RecoveryAuthorization
        ↓
Handover / Courier
        ↓
CustodyTransfer
        ↓
RecipientConfirmation
        ↓
RecoveryCompleted
```

---

# 66. `IsResolved` Duplication

The model uses both:

```text
ItemType.Returned
```

and:

```text
IsResolved
```

to represent overlapping recovery meaning.

This can create inconsistent state.

---

# 67. Dzoka Improvement

Persist authoritative state once and derive convenience projections such as:

```text
isResolved
```

when possible.

---

# 68. Search

The application supports ordinary search/filtering over:

```text
keywords
status
city
date
```

This is useful baseline product functionality.

---

# 69. Similar-Item Suggestions

The repository's simple similarity path uses:

```text
title
description
city
shared words
```

to produce candidate items.

---

# 70. Capability Qualification

This is not an AI retrieval engine.

It is closer to:

```text
same city
+
word overlap
```

---

# 71. In-Memory Search

Parts of the implementation transition to:

```text
AsEnumerable()
```

before further filtering.

This can cause work to move from the database into application memory.

That does not scale well.

---

# 72. Dzoka Search Difference

Dzoka uses indexed candidate retrieval:

```text
PostgreSQL FTS
pg_trgm
pgvector
PostGIS
protected identifiers
```

before heavier matching.

---

# 73. Image Storage

Images are represented as:

```text
ImageBase64
```

inside the relational model.

This is convenient for a small application but not appropriate for Dzoka's media architecture.

---

# 74. Dzoka Media Storage

Use:

```text
S3 / MinIO
```

for binary assets and:

```text
PostgreSQL
```

for metadata, lineage, classification, and references.

---

# 75. Claims Service Duplication

The project has a:

```text
ClaimsService
```

but significant claim logic also exists directly in:

```text
ClaimsController
```

---

# 76. Risk of Duplicated Business Rules

Different execution paths can eventually diverge.

For Dzoka:

```text
web
WhatsApp
API
kiosk
federation
```

must not each reimplement claim rules independently.

---

# 77. Dzoka Application Layer Rule

Channels invoke the same authoritative use case:

```text
SubmitClaim
ReviewClaim
ApproveClaim
RejectClaim
```

rather than duplicating business logic.

---

# 78. Verification Representation Divergence

Different code paths construct/store verification responses differently.

That is another sign that persistence semantics are not centralized strongly enough.

---

# 79. Dzoka Rule

Equivalent evidence must have one canonical domain representation regardless of:

```text
web
mobile
WhatsApp
API
```

entry channel.

---

# 80. Authentication

The repository uses:

```text
ASP.NET Core Identity
```

which is a solid foundation compared with custom password handling.

---

# 81. Password Policy

The configuration requires characteristics such as:

```text
minimum length
uppercase
lowercase
digit
non-alphanumeric
```

and unique email.

---

# 82. Lockout Mismatch

Identity configuration contains failed-login lockout settings.

However, the active login call uses:

```text
lockoutOnFailure: false
```

for password sign-in.

Therefore the configured lockout behavior is not necessarily enforced in that login path.

---

# 83. Dzoka Lesson

Security configuration is only real when the full execution path enforces it.

---

# 84. Email Confirmation

The registration workflow generates an identity confirmation token and sends it through email.

The login flow then manually blocks unconfirmed accounts.

This is a useful security concept.

---

# 85. Centralized Policy Preferred

The repository partly enforces confirmation in application code.

Dzoka should prefer identity-policy enforcement that cannot accidentally be omitted by alternate login flows.

---

# 86. Google OAuth

Google authentication is configured only when both provider credentials exist.

This is a reasonable optional-provider pattern.

---

# 87. Private Evidence Classification

Expected answers and claimant responses should not be ordinary report data.

In Dzoka they should normally be:

```text
SECRET
```

or similarly restricted.

---

# 88. Examples of Sensitive Verification Evidence

Potential answers may reveal:

```text
serial number fragments
private labels
bag contents
lock-screen content
engraving
unique damage
names
personal information
```

---

# 89. Access Policy

Secret evidence should be visible only to authorized components/users such as:

```text
verification engine
assigned reviewer
policy-authorized administrator
```

---

# 90. Logging Rule

Never send private expected answers or claimant secrets into normal:

```text
application logs
analytics
notifications
trace attributes
```

---

# 91. Human-Generated Questions

La2eet relies on the finder to create the verification questions.

This is useful because humans may observe details that automated perception misses.

---

# 92. Challenge Quality Problem

A finder can create a weak question:

```text
What colour is the phone?
```

when the public image already reveals the colour.

The platform does not model question quality.

---

# 93. Dzoka Challenge Architecture

```text
PrivateEvidenceInventory
        ↓
ChallengeCandidateGenerator
        ↓
DisclosureGuard
        ↓
GuessabilityAssessment
        ↓
DiscriminativeStrengthScorer
        ↓
ChallengeSelection
```

---

# 94. Challenge Sources

Dzoka can support questions from:

```text
FINDER
STAFF
AI
REGISTERED_OWNER
SYSTEM
```

with provenance.

---

# 95. Challenge Strength

Potential metadata:

```text
discriminativeStrength
guessability
sensitivity
reliability
disclosureRisk
```

---

# 96. Evidence Strength Example

```text
generic colour
→ weak

unique scratch
→ medium

serial suffix
→ strong

pre-loss image showing same unique damage
→ very strong
```

---

# 97. Multiple Evidence Types

La2eet primarily focuses on private text questions.

Dzoka combines:

```text
private knowledge
registered identifiers
pre-loss photographs
receipts
documents
institutional records
account history
```

where appropriate.

---

# 98. Verification Is Not Release Authorization

Even when ownership appears established:

```text
verification
```

must not automatically equal:

```text
release authorization
```

---

# 99. Example

A genuine owner may still face:

```text
legal hold
institutional hold
competing representative
guardian requirement
courier restriction
```

Therefore Dzoka separates:

```text
VerificationDecision
```

from:

```text
RecoveryAuthorization
```

---

# 100. Ownership vs Entitlement

Dzoka additionally needs to support:

```text
owner
    ↓ authorizes
representative
    ↓ collects
item
```

The collector and owner may be different persons.

---

# 101. Reporter vs Custodian

The repository generally assumes that the person reporting the found object controls the item.

Institutional Dzoka deployments cannot make that assumption.

---

# 102. Example

```text
airport employee
→ submits report

airport lost-property office
→ holds physical custody
```

Therefore:

```text
reporter ≠ custodian
```

must be modeled.

---

# 103. Reviewer vs Release Authority

Likewise:

```text
claim reviewer
```

may not have the authority to:

```text
physically release item
```

These are separate roles in Dzoka.

---

# 104. Fraud Architecture

The repository does not contain a comprehensive fraud/risk engine.

Dzoka requires a dedicated risk context.

---

# 105. Example Risk Signals

```text
many unrelated claims
repeated failed challenges
reused proof documents
account/device correlation
identifier conflict
suspicious timing
claim collusion
```

---

# 106. Evidence Coverage

The project does not calculate an explicit:

```text
EvidenceCoverage
```

because verification is primarily manual.

Dzoka needs coverage when combining heterogeneous evidence.

---

# 107. Example

A result based on:

```text
1 weak answer
```

must not look equivalent to one based on:

```text
serial suffix
+
receipt
+
pre-loss image
+
private challenge
```

even if a score coincidentally matches.

---

# 108. No AI Ownership Decision

One positive property is that La2eet does not put an AI model in control of ownership decisions.

Dzoka should retain:

```text
AI = assistant
```

not:

```text
AI = release authority
```

---

# 109. No Federation

The repository is centralized.

It does not provide:

```text
node identity
peer trust
cross-institution matching
federated claims
signed requests
```

---

# 110. No Custody State Machine

A complete chain of physical custody is absent.

Dzoka requires:

```text
custodian
location
transfer event
acceptance
release
handover
```

---

# 111. No Handover Credential

No robust:

```text
OTP
QR handover token
signed release credential
two-sided confirmation
```

was identified for recovery.

---

# 112. Recovery Audit

Dzoka needs immutable records answering:

```text
Who authorized release?
Who physically had the item?
Who handed it over?
Who received it?
When?
Where?
Under which claim?
```

---

# 113. Strongest Architecture Ideas

The strongest ideas worth retaining are:

```text
private verification questions

private evidence created at found-report time

mandatory verification question for found objects

claimant must answer before review

human ownership review

one claim per account/item

claim-owner authorization

claim approval separate from immediate return

post-approval communication concept
```

---

# 114. Weakest Architecture Areas

Major weaknesses include:

```text
unresolved license

plaintext expected answers

delimited claimant answers

no disclosure tracking

no challenge-quality model

no attempt model

no typed response evidence

no detailed decision audit

weak SignalR authorization

client-controlled sender identity

global SignalR broadcasting

weak returned-state authorization

shallow recovery/custody lifecycle

Base64 relational image storage
```

---

# 115. Security Findings — High/Critical

```text
project license unresolved

ExpectedAnswerHash is plaintext

client-supplied SignalR sender identity

missing approved-relationship enforcement in chat hub

Clients.All private-message broadcast

weak mark-returned authorization
```

---

# 116. Security Findings — Medium

```text
plaintext claimant responses

no private-evidence disclosure audit

login lockout configuration mismatch

duplicated claim business logic
```

---

# 117. What Dzoka Should Adopt Directly

Because licensing is unresolved:

```text
DIRECT CODE ADOPTION:
NONE
```

---

# 118. What Dzoka Should Adapt Conceptually

Independently implement:

```text
finder-defined private questions

mandatory private verification for suitable found reports

challenge-response ownership workflow

human review

claim restrictions

post-approval recovery conversation
```

---

# 119. What Dzoka Should Reference

Reference the project for:

```text
consumer ownership-verification UX

Arabic/RTL lost-and-found UX

claim comparison UI

human review interaction

post-claim communication concept
```

---

# 120. Explicitly Reject

Do not reproduce:

```text
plaintext ExpectedAnswerHash

delimiter-concatenated responses

array-position answer binding

UI-only authorization

client-supplied authenticated identity

Clients.All private messaging

mark-returned without entitlement checks

Base64 media storage

word-overlap similarity as primary matching
```

---

# 121. Dzoka Mapping — Verification Question

Upstream:

```text
VerificationQuestion
├── QuestionText
└── ExpectedAnswerHash
```

Dzoka:

```text
VerificationChallenge
├── challengeId
├── verificationSessionId
├── type
├── prompt
├── evidenceReference
├── protectedExpectedEvidence
├── source
├── disclosureRisk
├── discriminativeStrength
├── attemptsAllowed
├── status
├── createdAt
└── expiresAt
```

---

# 122. Dzoka Mapping — Challenge Response

Upstream:

```text
Claim.VerificationAnswer
```

Dzoka:

```text
ChallengeResponse
├── responseId
├── challengeId
├── rawResponse
├── normalizedResponse
├── interpretedResponse
├── submittedAt
├── comparisonResult
├── confidence
└── riskSignals
```

---

# 123. Dzoka Mapping — Verification Decision

Upstream:

```text
finder
→ approve / reject
```

Dzoka:

```text
evidence
    ↓
verification engine
    ↓
risk assessment
    ↓
policy
    ↓
authorized human review
    ↓
VerificationDecision
```

---

# 124. Dzoka Mapping — Recovery

Upstream:

```text
claim approved
    ↓
chat
    ↓
mark returned
```

Dzoka:

```text
claim approved
    ↓
VerificationDecision
    ↓
RecoveryAuthorization
    ↓
handover / logistics
    ↓
CustodyTransfer
    ↓
RecipientConfirmation
    ↓
RecoveryCompleted
```

---

# 125. Dzoka Mapping — Messaging

Upstream:

```text
SignalR user-to-user chat
```

Dzoka:

```text
RecoveryConversation
├── authenticated participants
├── claim/case scope
├── membership policy
├── contact privacy
├── moderation
├── audit
└── channel/provider adapters
```

---

# 126. ItemFingerprint Influence

This repository reinforces that an `ItemFingerprint` may contain information that must not be publicly searchable.

Therefore Dzoka separates:

```text
PUBLIC evidence
RESTRICTED evidence
SECRET ownership evidence
```

---

# 127. Example

Found phone:

```text
Public:
black smartphone
Apple-like device
found at station

Secret:
specific crack near rear camera
private wallpaper clue
serial suffix
case interior marking
```

---

# 128. Public vs Private Evidence

A critical design principle becomes:

> **The system may know more about the object than it is permitted to reveal.**

That knowledge can later support ownership verification.

---

# 129. Challenge Generation Example

Private evidence:

```text
small triangular sticker underneath laptop
```

Good challenge:

```text
Is there any marking or sticker on the underside?
If so, describe it.
```

Bad challenge:

```text
What colour is the triangular sticker underneath the laptop?
```

The second leaks too much information.

---

# 130. Exact vs Semantic Challenges

Dzoka should distinguish challenges requiring:

```text
exact comparison
```

from those requiring:

```text
semantic interpretation
```

---

# 131. Exact Example

```text
What are the final four characters of the serial?
```

Possible evaluation:

```text
normalize
→ keyed comparison
```

---

# 132. Semantic Example

```text
Describe any unusual marking on the underside.
```

This may require:

```text
structured interpretation
+
human review
```

---

# 133. LLM Role

An LLM may help interpret:

```text
"tiny dark-red triangle underneath"
```

against protected evidence like:

```text
"small crimson triangular sticker on bottom panel"
```

but it must not independently make the ownership or release decision.

---

# 134. Verification Evidence Composition

A strong Dzoka verification may combine:

```text
private challenge
+
serial suffix
+
pre-loss photograph
+
receipt
+
risk assessment
```

rather than depending on a single answer.

---

# 135. Privacy Architecture Influence

This repository directly supports the need for:

```text
PublicItemProjection
RestrictedItemProjection
VerificationEvidenceProjection
```

rather than returning one universal representation of an item.

---

# 136. Data Architecture Influence

Relevant Dzoka tables/entities include:

```text
verification_sessions
verification_challenges
challenge_expected_evidence
challenge_responses
challenge_disclosures
verification_decisions
```

---

# 137. Security Architecture Influence

Private verification evidence requires:

```text
encryption
strict roles
audited access
retention policy
minimum disclosure
rate limiting
```

---

# 138. Claim Workflow Influence

The repository validates the architecture:

```text
MATCH
  ≠
OWNERSHIP
```

and:

```text
MatchCandidate
    ↓
Claim
    ↓
Verification
```

before recovery.

---

# 139. Messaging Architecture Influence

The implementation also gives Dzoka a major negative lesson:

```text
button visibility
≠
authorization
```

Every recovery conversation must be authorized server-side.

---

# 140. Custody Architecture Influence

Similarly:

```text
"Mark Returned" UI
≠
evidence of physical return
```

Custody/recovery must be modeled as business state, not merely a button action.

---

# 141. Comparison — `globalenglish01/lostfound`

`globalenglish01/lostfound` is much stronger for:

```text
matching
hard conflicts
evidence coverage
model provenance
retrieval
```

Malak-Rashad/LostAndFound is more useful for:

```text
private ownership questions
human review
claim-response UX
```

---

# 142. Comparison — Smart Asset Finder

Smart Asset Finder is stronger for:

```text
QR ecosystem
WhatsApp notifications
rewards
hardware tags
```

Malak-Rashad/LostAndFound provides a cleaner conceptual private-question workflow.

---

# 143. Comparison — AI-lost-and-found / SmartFind

SmartFind attempts automated verification using text similarity against report information.

Malak-Rashad's approach is conceptually safer because:

```text
private expected answer
+
human comparison
```

is a better trust primitive than:

```text
public description similarity
→ ownership percentage
```

even though its secret storage remains insecure.

---

# 144. Dzoka Source Synthesis

```text
globalenglish
→ evidence-aware matching

FuricBond
→ multimodal visual retrieval ideas

Lost-and-Found-Tracking-System
→ institutional claims/risk

Malak-Rashad/LostAndFound
→ private challenge verification

Smart Asset Finder
→ QR / notifications / rewards

AI-lost-and-found
→ workflow presentation + anti-pattern lessons
```

---

# 145. Relevant Dzoka Documents

This audit should influence:

```text
DESIGN.md

docs/design/item-fingerprint.md
docs/design/ownership-verification.md
docs/design/claim-workflow.md
docs/design/custody-state-machine.md

docs/architecture/data-architecture.md
docs/architecture/security-architecture.md
docs/architecture/privacy-architecture.md
docs/architecture/verification-architecture.md
docs/architecture/whatsapp-architecture.md
```

---

# 146. Potential ADRs

Possible ADRs informed by this repository:

```text
protect private expected verification evidence

separate matching from ownership verification

support finder-authored verification challenges

track private-evidence disclosure

server-enforce recovery conversation membership

separate claim approval from recovery authorization

require authoritative custody transition for return completion
```

---

# 147. Source Claims vs Implementation

## Verification questions

Claim:

```text
finder can define verification questions
```

Result:

```text
IMPLEMENTED
```

---

## Claimant response review

Claim:

```text
finder compares responses
```

Result:

```text
IMPLEMENTED
```

---

## Expected answer hash

Name implies:

```text
hashed answer
```

Result:

```text
NOT HASHED
PLAINTEXT
```

---

## Verified chat

Product concept:

```text
chat after claim approval
```

Result:

```text
UI/workflow exists
BUT server authorization is insufficient
```

---

## Item returned

Feature exists:

```text
mark returned
```

Result:

```text
IMPLEMENTED
BUT insufficient authoritative authorization/custody checks
```

---

# 148. Key Positive Findings

```text
private verification question concept

found reports require verification questions

human review remains authoritative

claim owner authorization

duplicate/self-claim protections

approval separated from immediate returned status

ASP.NET Identity foundation

email confirmation

optional Google OAuth
```

---

# 149. Key Negative Findings

```text
license unresolved

plaintext expected answers

plaintext claimant answers

no typed response model

no private-evidence disclosure history

no challenge quality score

no strong competing-claim flow

no custody aggregate

no recovery authorization aggregate

weak SignalR authorization

client-supplied sender identity

global message broadcast

weak returned-state authorization
```

---

# 150. Recommended `source-audit.lock.json` Entry

```json
{
  "id": "malak-lostandfound",
  "category": "platforms",
  "repository": "https://github.com/Malak-Rashad/LostAndFound",
  "revision": "d5d7bfaf043396a32b40c264c647f6456cadb633",
  "license": "UNRESOLVED",
  "licenseNote": "No root project LICENSE file found at audited revision.",
  "classification": "REFERENCE",
  "secondaryClassification": "ADAPT_VERIFICATION_CONCEPTS",
  "productAlias": "La2eet",
  "audit": "docs/source-audit/platforms/malak-lostandfound.md",
  "auditedAt": "2026-10-04"
}
```

---

# 151. Duplicate Lock Entry Warning

Do **not** maintain two independently pinned entries for:

```text
la2eet
```

and:

```text
malak-lostandfound
```

if they both point to the exact same repository.

Prefer one canonical lock identity.

Recommended:

```text
id = malak-lostandfound
productAlias = La2eet
```

This prevents:

```text
duplicate audits
different SHAs
different license classifications
contradictory findings
```

for the same source.

---

# 152. Re-Audit Triggers

Re-audit if upstream:

```text
adds a project license

protects expected answers cryptographically

adds typed verification responses

adds challenge attempt limits

adds disclosure tracking

adds competing-claim adjudication

fixes SignalR identity/authorization

uses private conversation groups

adds strong returned-state authorization

introduces custody transfers

adds handover credentials

adds verification/audit tests
```

---

# 153. Final Classification

```text
PRIMARY:
REFERENCE
```

Secondary:

```text
ADAPT_VERIFICATION_CONCEPTS
```

No direct code adoption until licensing is resolved.

---

# 154. Core Concept Worth Preserving

The most important contribution from this repository is:

```text
PUBLIC ITEM INFORMATION
          ≠
ALL KNOWN ITEM INFORMATION
```

Some characteristics should intentionally remain hidden so they can later become ownership evidence.

---

# 155. Dzoka Evolution

Upstream:

```text
Finder
  ↓
creates private question
  ↓
stores expected answer
  ↓
Claimant answers
  ↓
Finder compares
  ↓
Approve / Reject
```

Dzoka:

```text
ITEM OBSERVATION
      ↓
PRIVATE EVIDENCE INVENTORY
      ↓
CLASSIFICATION
      ↓
DISCLOSURE ANALYSIS
      ↓
CHALLENGE CANDIDATE GENERATION
      ↓
DISCRIMINATIVE STRENGTH
      ↓
SAFE CHALLENGE
      ↓
CLAIMANT RESPONSE
      ↓
EXACT / SEMANTIC EVALUATION
      ↓
FRAUD / RISK ASSESSMENT
      ↓
POLICY
      ↓
AUTHORIZED HUMAN REVIEW
      ↓
VERIFICATION DECISION
      ↓
RECOVERY AUTHORIZATION
      ↓
CUSTODY / HANDOVER
      ↓
RECOVERY COMPLETION
```

---

# 156. Final Assessment

`Malak-Rashad/LostAndFound` is **not a suitable Dzoka code foundation**, particularly because its license is unresolved and several security boundaries are too weak.

Its architectural value is concentrated in one excellent idea:

> **A finder can retain non-public facts about a found object and use those facts later to challenge someone claiming ownership.**

Dzoka should preserve and significantly harden that concept.

The final Dzoka model becomes:

```text
FOUND OBJECT
      ↓
ITEM FINGERPRINT
      ├── PUBLIC EVIDENCE
      ├── RESTRICTED EVIDENCE
      └── SECRET OWNERSHIP EVIDENCE
                     ↓
             VerificationSession
                     ↓
             Safe Challenge
                     ↓
             Claimant Response
                     ↓
             Evidence Evaluation
                     ↓
             Fraud / Risk
                     ↓
             Policy
                     ↓
             Human Decision
                     ↓
             RecoveryAuthorization
                     ↓
             Secure Custody Handover
```

The main conclusion is:

> **Malak-Rashad/LostAndFound gives Dzoka a strong private-challenge ownership-verification concept, but Dzoka must replace plaintext secrets, position-based response storage, UI-level trust, weak messaging authorization, and shallow recovery state with protected evidence, typed challenge responses, disclosure controls, server-enforced authorization, explicit recovery authorization, and a formal custody state machine.**