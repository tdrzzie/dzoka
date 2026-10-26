# Source Audit — Malak-Rashad/LostAndFound (La2eet)

> **Dzoka — The Open Recovery Network**
>
> Source audit of **La2eet (لقيت)**, an Egyptian lost-and-found platform built with ASP.NET Core, with particular focus on private ownership-verification questions, claims, human verification, post-approval communication, report moderation, and recovery workflow.

---

# 1. Audit Metadata

| Field | Value |
|---|---|
| Repository | `https://github.com/Malak-Rashad/LostAndFound` |
| Upstream owner | `Malak-Rashad` |
| Product name | La2eet / لقيت |
| Audited revision | `d5d7bfaf043396a32b40c264c647f6456cadb633` |
| Short revision | `d5d7bfaf` |
| Revision date | 2026-07-21 |
| Audit date | 2026-10-04 |
| Default branch | `master` |
| License | **UNRESOLVED — no root project license found** |
| Backend | ASP.NET Core MVC (.NET 9) |
| ORM | Entity Framework Core 9 |
| Database | Microsoft SQL Server |
| Authentication | ASP.NET Core Identity |
| External identity | Google OAuth |
| Real-time messaging | SignalR |
| Frontend | Razor + Bootstrap + JavaScript |
| Email | MailKit / MimeKit |
| Primary classification | **REFERENCE** |
| Secondary classification | **ADAPT VERIFICATION WORKFLOW CONCEPTS** |
| Direct code reuse | **DO NOT COPY until project licensing is clarified** |

---

# 2. Executive Decision

La2eet provides one of the strongest direct examples identified so far of:

```text
finder defines private knowledge
        ↓
claimant answers questions
        ↓
finder compares expected vs supplied answers
        ↓
finder approves/rejects claim
```

That concept is highly relevant to Dzoka's ownership-verification architecture.

However, the implementation has several major weaknesses.

Most importantly:

```text
ExpectedAnswerHash
```

is **not actually hashed**.

The expected answer is:

```text
entered as plaintext
        ↓
stored as plaintext
        ↓
retrieved as plaintext
        ↓
displayed to finder as plaintext
```

Likewise claimant responses are stored as one plaintext string.

The repository also has significant server-side authorization weaknesses around chat and marking items returned.

Therefore:

```text
PRIMARY:
REFERENCE

SECONDARY:
ADAPT VERIFICATION WORKFLOW CONCEPTS
```

---

# 3. Why La2eet Matters to Dzoka

Most basic lost-and-found platforms ask:

```text
Does this claimant say the item is theirs?
```

La2eet asks something better:

> Can the claimant demonstrate knowledge about the object that should not be obvious from the public listing?

That idea directly supports Dzoka's:

```text
PrivateEvidence
VerificationChallenge
ChallengeResponse
VerificationSession
DisclosureGuard
VerificationDecision
```

design.

---

# 4. Audit Scope

Primary implementation inspected:

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
├── Enums/
│   ├── ClaimStatus.cs
│   ├── ItemType.cs
│   └── ReportStatus.cs
│
├── Views/Claims/
│   ├── Create.cshtml
│   └── Review.cshtml
│
└── Program.cs
```

The root repository contents were also inspected for license verification.

---

# 5. Audit Scope Limit

This is a focused architectural audit.

It is not:

- a complete security penetration test;
- a formal legal opinion;
- a review of every controller/view;
- an exhaustive .NET dependency audit;
- proof that the deployed demo matches the pinned source exactly.

---

# 6. Licensing Finding

At the pinned revision:

```text
d5d7bfaf043396a32b40c264c647f6456cadb633
```

the repository root contains no:

```text
LICENSE
```

file.

A direct request for that file returns not found.

Third-party frontend libraries may contain their own licenses, but those do not license the La2eet application itself.

---

# 7. Dzoka Licensing Decision

Until licensing is clarified:

### Allowed

```text
study the implementation
extract architectural lessons
document workflows
independently reimplement concepts
```

### Do not

```text
copy source files
port substantial source code
copy functions with renamed variables
treat the project as MIT/open-source merely because it is public
```

---

# 8. Product Architecture

La2eet uses a traditional server-rendered architecture:

```text
Browser
   ↓
ASP.NET Core MVC
   ↓
Controllers
   ↓
Services / Repositories
   ↓
Entity Framework Core
   ↓
SQL Server
```

with additional:

```text
ASP.NET Identity
SignalR
MailKit
Google OAuth
```

---

# 9. Domain Scope

The project includes:

```text
Accounts
Reports
Moderation
Search
Claims
Verification Questions
Messaging
Recovery
Profiles
Administration
```

This is a reasonably complete consumer lost-and-found workflow.

---

# 10. Item Model

`Item.cs` contains:

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

# 11. Item Type

The item state/type enum combines:

```text
Lost
Found
Returned
```

This is simple but semantically overloaded.

`Returned` is not the same type of concept as:

```text
Lost
Found
```

The first two describe report perspective.

The latter describes recovery lifecycle.

---

# 12. Dzoka Improvement

Dzoka separates:

```text
ReportType
├── LOST
└── FOUND
```

from:

```text
RecoveryState
```

and:

```text
CustodyState
```

so one enum does not mix distinct concerns.

---

# 13. Report Moderation

Reports separately carry:

```text
ReportStatus
├── Pending
├── Approved
└── Rejected
```

This is a useful distinction.

A report may be:

```text
FOUND
```

while moderation state is:

```text
APPROVED
```

These are different dimensions.

---

# 14. Dzoka Adoption

Dzoka should preserve separate:

```text
domain state
moderation state
```

where moderation is required.

---

# 15. Verification Questions

Found reports require at least one:

```text
VerificationQuestion
```

The model contains:

```text
VerificationQuestion
├── QId
├── QuestionText
├── ExpectedAnswerHash
├── CreatedAt
└── ItemId
```

This is the project's strongest Dzoka-relevant idea.

---

# 16. Finder-Defined Private Evidence

When creating a FOUND report, the finder provides questions such as:

```text
What colour is the phone case?

What sticker is on the back?

What name appears inside the wallet?

What scratch is on the laptop?
```

and supplies the expected answers.

---

# 17. At Least One Question

`ItemService.CreateItem()` explicitly rejects a found-item report if there are no valid verification questions.

Conceptually:

```text
FOUND item
    ↓
private verification question required
```

This is a strong trust-oriented product decision.

---

# 18. Why This Is Valuable

A public listing might reveal:

```text
black iPhone
found in Cairo
```

Thousands of people could plausibly claim that.

A private question might ask:

```text
What picture is on the lock screen?
```

That provides far stronger ownership evidence.

---

# 19. Critical Finding — `ExpectedAnswerHash` Is Not a Hash

Despite the name:

```text
ExpectedAnswerHash
```

the implementation performs no cryptographic hashing.

`ItemService.cs` effectively performs:

```text
ExpectedAnswerHash =
    submitted ExpectedAnswerHash
```

unchanged.

---

# 20. Creation Form Confirms This

`Views/Items/Create.cshtml` directly binds a normal text input to:

```text
Questions[...].ExpectedAnswerHash
```

The user types the correct answer into this form.

---

# 21. Database Confirms This

The schema stores it simply as:

```text
nvarchar(255)
```

There is no evidence of:

```text
SHA-256
HMAC
PBKDF2
Argon2
bcrypt
encryption
```

being applied to the expected answer.

---

# 22. Review Screen Confirms This

The review page renders:

```text
@q.ExpectedAnswerHash
```

as:

> the correct answer

next to the claimant's submitted response.

Therefore:

```text
ExpectedAnswerHash
```

is actually:

```text
ExpectedAnswerPlaintext
```

in the audited implementation.

---

# 23. Dzoka Lesson — Never Trust Security Names

A field named:

```text
passwordHash
secretHash
answerHash
```

does not prove that hashing occurs.

Source behaviour must be inspected.

This is exactly the purpose of the source audit.

---

# 24. Dzoka Storage Decision

Private expected answers must not be stored in ordinary plaintext.

Depending on challenge type, Dzoka may use:

```text
keyed hash / HMAC
```

for deterministic normalized comparison and/or:

```text
envelope-encrypted secret
```

when authorized human review requires access.

---

# 25. Why Plain Unsalted Hashing Is Also Weak

Many expected answers have low entropy:

```text
black
blue
Samsung
three
Mickey Mouse
```

A plain SHA-256 hash would therefore be vulnerable to dictionary guessing.

Dzoka should not simply replace plaintext with:

```text
SHA256(answer)
```

and consider the problem solved.

---

# 26. Better Dzoka Pattern

For exact challenge types:

```text
normalize
    ↓
keyed HMAC
```

may allow comparison without exposing the answer.

Where semantic/human comparison is necessary:

```text
encrypted expected evidence
```

may also be retained under strict authorization.

---

# 27. Claimant Answers

`Claim.cs` stores:

```text
VerificationAnswer
```

as a single string.

Claim creation builds the value by joining answers with:

```text
|
```

---

# 28. Example Storage

Conceptually:

```text
black|Samsung|small crack near camera
```

may be stored in one field.

The review view splits that string back on:

```text
|
```

and assumes answer ordering matches question ordering.

---

# 29. Structural Weakness

This is fragile.

Problems include:

```text
answer contains "|"
questions reordered
question removed
answer count differs
max field length exceeded
no per-answer metadata
```

---

# 30. Dzoka Correction

Responses should be first-class records:

```text
ChallengeResponse
├── responseId
├── sessionId
├── challengeId
├── rawResponse
├── normalizedResponse
├── interpretedResponse
├── submittedAt
├── source
└── evaluation
```

---

# 31. Question-to-Answer Identity

Each answer must reference:

```text
challengeId
```

explicitly.

Do not rely on:

```text
array position
```

or:

```text
string order
```

for verification evidence.

---

# 32. Claim Workflow

The upstream flow is:

```text
Found Report
    ↓
Finder defines questions
    ↓
Claimant submits claim
    ↓
Claimant answers all questions
    ↓
Pending
    ↓
Finder reviews
    ↓
Approved / Rejected
```

This is a genuinely useful workflow reference.

---

# 33. Claim State Machine

The status enum contains:

```text
Pending
Approved
Rejected
```

This is intentionally simple.

---

# 34. Dzoka Extension

Dzoka needs:

```text
DRAFT
SUBMITTED
ELIGIBILITY_REVIEW
VERIFICATION_PENDING
UNDER_REVIEW
CONFLICT
APPROVED
REJECTED
WITHDRAWN
CANCELLED
```

or equivalent domain-controlled states.

---

# 35. One Claim Per User/Item

The controller checks whether the current user has already submitted a claim for the item.

Conceptually:

```text
same user
+
same item
→ second claim blocked
```

This is a useful basic abuse control.

---

# 36. Finder Cannot Claim Own Report

The controller also blocks:

```text
item.UserId == claimant.UserId
```

This is another reasonable invariant.

---

# 37. Dzoka Adoption

Dzoka should maintain equivalent invariants while allowing explicit administrative exceptions where required.

---

# 38. Answer Completeness

If the found item has private questions:

```text
every question must be answered
```

before submission.

This is reasonable.

---

# 39. No Automatic Verification

The implementation does **not** automatically compare claimant responses with expected answers.

Instead:

```text
finder manually reviews
```

the two values side by side.

---

# 40. Important Interpretation

La2eet's architecture is therefore:

```text
PRIVATE QUESTION
        ↓
HUMAN COMPARISON
```

not:

```text
PRIVATE QUESTION
        ↓
CRYPTOGRAPHIC MATCH
```

and not:

```text
PRIVATE QUESTION
        ↓
AI OWNERSHIP DECISION
```

---

# 41. Positive Authority Boundary

Human review is actually a positive property.

The system does not pretend one text-similarity score can establish ownership.

That principle should survive in Dzoka.

---

# 42. Dzoka Extension — Automated Assistance

Dzoka may add:

```text
normalization
semantic interpretation
exact comparison
evidence weighting
AI assistance
```

but:

```text
AI
```

must not become the sole release authority.

---

# 43. Finder Authorization

When approving/rejecting a claim, the controller verifies:

```text
claim.Item.UserId == currentUser.Id
```

This correctly prevents arbitrary users from approving claims on someone else's report.

---

# 44. Claim Review Authorization

The review route performs the same ownership check.

Only the report owner can inspect the claim review screen.

This is a good authorization pattern.

---

# 45. Private Expected Answer Exposure

Although expected answers are plaintext in storage, they are not intentionally shown to the claimant through the claim form.

The claimant receives:

```text
QuestionText
```

not:

```text
ExpectedAnswerHash
```

This is directionally correct.

---

# 46. Question Leakage Risk

Even the question itself can reveal the answer if poorly written.

Bad challenge:

```text
What colour is the red sticker on the back?
```

The question itself reveals:

```text
red sticker
```

---

# 47. Dzoka Challenge Leakage Guard

Dzoka's challenge generator should evaluate:

```text
Does the question disclose the evidence?
```

before presenting it.

This is a core `DisclosureGuard` responsibility.

---

# 48. Public Listing Leakage

A good private challenge should not ask about something already disclosed in:

```text
public description
public images
public OCR
finder comments
previous challenge
```

---

# 49. Dzoka Disclosure History

Each private evidence element should track whether it has already been revealed.

Conceptually:

```text
PrivateEvidence
├── disclosureState
├── disclosedAt
├── disclosedTo
├── disclosureReason
└── challengeHistory
```

---

# 50. Repeated Challenge Problem

La2eet does not implement a strong disclosure-history architecture.

If a claimant can repeatedly see the same questions or obtain information through other channels, challenge strength can degrade.

---

# 51. Dzoka Improvement

Challenge selection must consider:

```text
previous attempts
previous disclosures
claimant identity
competing claims
question entropy
evidence sensitivity
```

---

# 52. Attempt Limiting

La2eet blocks a second claim by the same account for the same item.

That indirectly limits repeated attempts.

However, it does not constitute a full challenge-attempt security architecture.

---

# 53. Dzoka Attempt Controls

Dzoka requires:

```text
attempt count
cooldown
risk escalation
account/device correlation
challenge rotation
lockout
manual review
```

as appropriate.

---

# 54. Claim Review Screen

The finder sees:

```text
question
expected answer
claimant answer
```

side by side.

This is simple and operationally effective.

---

# 55. Dzoka Reviewer UX Lesson

Human reviewers should receive structured evidence comparison rather than raw database data.

Example:

```text
Challenge:
What is engraved inside?

Expected:
N.M.

Claimant:
NM

Interpretation:
likely equivalent

Risk:
low

Disclosure:
secret
```

---

# 56. Human Judgment Must Remain Auditable

La2eet changes only:

```text
ClaimStatus
```

when the finder approves or rejects.

Dzoka should additionally store:

```text
VerificationDecision
├── reviewer
├── decision
├── reason
├── evidence considered
├── policy version
├── timestamp
└── override information
```

---

# 57. Decision Reason Gap

The upstream approve/reject action does not require an explicit reviewer reason.

That weakens later auditability.

---

# 58. Dzoka Rule

Sensitive ownership decisions should record:

```text
why
```

not only:

```text
what
```

---

# 59. Competing Claims Gap

La2eet supports multiple claims generally but does not provide Dzoka's explicit:

```text
ClaimConflict
```

aggregate and conflict resolution workflow.

---

# 60. Dzoka Improvement

A serious competing claim should be able to:

```text
place recovery hold
```

and prevent release until adjudicated.

---

# 61. Approval Does Not Immediately Mark Item Returned

Unlike some other audited repositories, La2eet does not immediately mark the item returned when a claim is approved.

Instead approval enables communication.

This is directionally better.

---

# 62. Post-Approval Chat Concept

README describes:

```text
claim approved
    ↓
verified chat
    ↓
finder + claimant arrange return
```

This is a useful product concept.

---

# 63. Dzoka Equivalent

Dzoka similarly needs:

```text
RecoveryConversation
```

that can safely connect parties without necessarily exposing:

```text
phone
email
home address
```

---

# 64. Critical Finding — “Verified Chat” Is Not Securely Enforced

The UI may only offer chat after an approved claim.

However, inspected backend enforcement is insufficient.

This is a major trust-boundary flaw.

---

# 65. `MessagesController`

The controller itself does not carry a class-level:

```text
[Authorize]
```

attribute.

Its `Index()` assumes a current user exists, but server-side authentication is not explicitly required there.

---

# 66. More Serious — Direct Chat Construction

`ChatService.GetMessagesPage()` accepts:

```text
itemId
receiverId
```

and can construct a new chat view for those supplied IDs even when no existing approved relationship was verified.

---

# 67. Most Serious — SignalR Sender Spoofing

`ChatHub.SendMessage()` accepts:

```text
senderId
receiverId
itemId
message
```

directly from the client.

It does not derive:

```text
senderId
```

from the authenticated SignalR principal.

---

# 68. Consequence

A malicious client could potentially supply another user's ID as:

```text
senderId
```

unless other unseen infrastructure prevents it.

This violates a fundamental authentication rule:

> Never trust the client to tell the server who the authenticated user is.

---

# 69. No Approved-Claim Check in Hub

The inspected `SendMessage()` code does not verify:

```text
approved claim exists
```

between the sender/receiver for that item.

---

# 70. No Relationship Authorization

It checks only:

```text
item exists
item is not resolved
```

before persisting a message.

That is insufficient.

---

# 71. Broadcast to All Clients

Messages are sent using:

```text
Clients.All
```

rather than a private:

```text
user
group
conversation
```

channel.

That is another serious privacy concern.

---

# 72. Dzoka Decision — Reject Completely

Do not copy this messaging authorization architecture.

Dzoka messaging must enforce:

```text
authenticated actor
+
authorized conversation membership
+
case/recovery relationship
+
tenant boundary
```

on every message.

---

# 73. Dzoka Messaging Pattern

Conceptually:

```text
authenticated principal
       ↓
ConversationMembershipPolicy
       ↓
authorized RecoveryConversation
       ↓
message persisted
       ↓
send only to conversation group
```

---

# 74. Server-Derived Identity

Dzoka must derive:

```text
senderId
```

from the authenticated principal.

Never accept authoritative sender identity from request payload.

---

# 75. Critical Finding — Mark Returned Authorization

`MessagesController.MarkAsReturned()` accepts:

```text
itemId
```

and delegates directly to:

```text
ChatService.MarkItemAsReturned()
```

---

# 76. Repository Behaviour

`ChatRepository.MarkItemAsReturned()` simply performs:

```text
item.Status = Returned
item.IsResolved = true
```

and saves.

It does not check:

```text
who called
approved claim
current custody
recovery authorization
handover evidence
```

---

# 77. Endpoint-Level Problem

The inspected `MarkAsReturned` endpoint does not derive a current authorized actor before performing the state change.

This is a critical authorization weakness.

---

# 78. UI Authorization Is Not Security

The UI calculates:

```text
CanMarkReturned
```

for selected users.

That does **not** secure the server endpoint.

A user interface hiding a button is not an authorization boundary.

---

# 79. Dzoka Invariant

Every sensitive server operation must independently authorize the actor.

Especially:

```text
approve claim
create handover
transfer custody
mark returned
release reward
```

---

# 80. Recovery Completion

Dzoka therefore rejects:

```text
POST itemId
→ IsResolved = true
```

as a complete recovery model.

---

# 81. Correct Dzoka Flow

```text
Claim Approved
    ↓
RecoveryAuthorization
    ↓
Handover / Logistics
    ↓
Custody Transfer
    ↓
Recipient Confirmation
    ↓
RecoveryCompleted
```

---

# 82. Chat Closure Concept

The repository closes conversations when an item becomes resolved.

That is a useful UX concept.

Dzoka can adapt:

```text
recovery completed
→ conversation becomes read-only / closed
```

while preserving audit history.

---

# 83. Item Resolution Data

The upstream model uses:

```text
IsResolved
```

plus:

```text
ItemType.Returned
```

which duplicates recovery meaning.

This can drift.

---

# 84. Dzoka Decision

Use one authoritative recovery state machine and derive projections such as:

```text
isResolved
```

rather than persisting multiple overlapping truth fields unnecessarily.

---

# 85. Search Architecture

La2eet supports ordinary browsing by:

```text
keyword
status
city
date
```

This is useful product functionality.

---

# 86. Similar Item Suggestions

`ItemRepository.GetSimilarItems()` creates a word list from:

```text
title
description
```

then:

```text
loads items
filters same city
tests substring overlap
takes first few
```

---

# 87. Important Qualification

The “similar items” capability is not an AI matching engine.

It is essentially:

```text
same city
+
shared words
```

---

# 88. Dzoka Decision

Use La2eet as a verification/workflow reference.

Do **not** use it as a matching-engine reference.

---

# 89. In-Memory Filtering

The implementation calls:

```text
AsEnumerable()
```

before parts of similar-item filtering.

That can move filtering into application memory.

This is unsuitable for large datasets.

---

# 90. Images in SQL

The item stores:

```text
ImageBase64
```

inside the SQL record.

This is convenient for a student/small project but not a strong production media architecture.

---

# 91. Dzoka Media Decision

Media belongs in:

```text
S3 / MinIO
```

with metadata stored in PostgreSQL.

---

# 92. Claims Service Architecture

The project contains a dedicated:

```text
ClaimsService
```

using repositories/unit-of-work.

That is directionally good.

---

# 93. Controller Duplication

However, `ClaimsController` independently reimplements much of the same:

```text
claim creation
validation
approval
rejection
```

logic rather than consistently delegating to `ClaimsService`.

---

# 94. Architecture Consequence

Two business-logic paths can drift.

For example:

```text
service logic
≠
controller logic
```

over time.

---

# 95. Dzoka Rule

Business invariants belong in application/domain services, not duplicated across:

```text
HTTP
WhatsApp
kiosk
federation
```

controllers.

Every channel must invoke the same use case.

---

# 96. One Verification Representation

The service path formats responses as:

```text
QuestionText: answer
```

while the controller path stores only:

```text
answer|answer
```

This is another example of duplicated logic producing inconsistent persistence formats.

---

# 97. Dzoka Decision

Never store semantically equivalent evidence in incompatible formats depending on entry path.

Use one domain representation.

---

# 98. Authentication Foundation

The project uses:

```text
ASP.NET Core Identity
```

which is significantly stronger than hand-written password storage.

---

# 99. Password Policy

Configuration requires:

```text
minimum length 8
digit
uppercase
lowercase
non-alphanumeric
```

and unique email.

This is a reasonable baseline.

---

# 100. Lockout Configuration

The Identity configuration defines:

```text
5 failed attempts
→ 5 minute lockout
```

but password login calls:

```text
PasswordSignInAsync(... lockoutOnFailure: false)
```

---

# 101. Documentation Divergence

The README describes lockout after repeated failures.

The active login path disables lockout-on-failure for that sign-in call.

Therefore the implemented behaviour does not clearly match the README claim.

---

# 102. Dzoka Lesson

Security configuration must be validated end-to-end.

Having:

```text
lockout settings
```

does not matter if login code bypasses them.

---

# 103. Email Confirmation

The registration flow generates an ASP.NET Identity email-confirmation token and emails a confirmation link.

The login action manually refuses unconfirmed users.

This is a positive account-security pattern.

---

# 104. Configuration Divergence

`Program.cs` itself sets:

```text
RequireConfirmedEmail = false
```

while the login controller manually checks `EmailConfirmed`.

This works for that login path but spreads the invariant across layers.

---

# 105. Dzoka Identity Lesson

Authentication requirements should preferably be enforced centrally rather than relying on every login path remembering the same checks.

---

# 106. Google Authentication

Google OAuth is enabled only when both:

```text
ClientId
ClientSecret
```

exist.

This is a clean optional-provider pattern.

---

# 107. Secret Handling

The repository uses a configuration template rather than committing the primary application settings file.

That is better than embedding provider credentials directly.

---

# 108. Verification Answer Classification

La2eet currently treats expected/claimant answers as ordinary database strings.

Dzoka must classify them as:

```text
SECRET
```

or similarly highly restricted evidence.

---

# 109. Why Verification Answers Are Sensitive

A challenge answer can reveal:

```text
device unlock characteristics
engraving
private markings
personal names
contents of wallet/bag
unique damage
```

which should not be broadly visible to staff or logs.

---

# 110. Dzoka Access Control

Access to secret verification evidence should be limited to:

```text
verification engine
authorized reviewer
policy-authorized support
```

with audit logging.

---

# 111. Logging Rule

Never place:

```text
expected private answers
claimant answers
protected serial numbers
```

in ordinary application logs.

---

# 112. Question Creation Trust

La2eet relies entirely on the finder to create good questions.

This is simple and valuable, but quality varies.

---

# 113. Weak Questions

Finder may choose:

```text
What colour is it?
```

for an object whose public image clearly shows the colour.

Such a challenge has almost zero private evidentiary value.

---

# 114. Dzoka Dynamic Challenge Engine

Dzoka therefore extends the idea with:

```text
PrivateEvidenceInventory
        ↓
ChallengeCandidateGenerator
        ↓
DisclosureGuard
        ↓
DiscriminativeStrengthScorer
        ↓
ChallengeSelection
```

---

# 115. Finder Questions Still Useful

Dzoka should not remove human-generated questions.

Finder/staff may know details the AI cannot detect.

Therefore Dzoka can combine:

```text
finder-provided challenge
AI-generated challenge
institution-provided challenge
registered-owner secret
```

---

# 116. Challenge Source Provenance

Every challenge should record:

```text
FINDER
STAFF
AI
REGISTERED_OWNER
SYSTEM
```

as its source.

---

# 117. Challenge Quality

Each challenge can carry:

```text
discriminative strength
guessability
disclosure risk
reliability
sensitivity
```

which La2eet does not model.

---

# 118. Low-Entropy Questions

Example:

```text
What colour is the case?
```

may have only a handful of likely answers.

It should not carry the same ownership weight as:

```text
What are the last four digits of the device serial?
```

---

# 119. Protected Identifiers

Dzoka additionally supports:

```text
serial
IMEI
passport number
asset tag
```

using protected comparison mechanisms.

La2eet does not provide this.

---

# 120. Multiple Evidence Types

La2eet's verification is primarily:

```text
private knowledge questions
```

Dzoka must combine:

```text
private knowledge
prior photos
purchase receipts
registered identifiers
documents
institutional records
account/device history
```

where appropriate.

---

# 121. Verification Is Not Claim Approval Alone

La2eet combines:

```text
finder review
→ claim approved
```

into one direct step.

Dzoka separates:

```text
VerificationDecision
```

from:

```text
ClaimDecision
```

and from:

```text
RecoveryAuthorization
```

where policy requires it.

---

# 122. Why Separation Matters

A claimant may prove:

```text
ownership
```

but still not be immediately eligible for physical release.

Examples:

```text
legal hold
police hold
competing representative
courier restriction
minor/guardian requirement
```

---

# 123. Authorization vs Entitlement

Dzoka additionally models cases where the collector is:

```text
not the owner
```

but is legitimately authorized.

Example:

```text
owner
    ↓ authorizes
family member
    ↓ collects
item
```

La2eet does not model this distinction.

---

# 124. Report Owner ≠ Custodian

La2eet assumes the person who posted the found report is effectively the party controlling return.

For institutional Dzoka deployments:

```text
reporter
≠
custodian
```

may be common.

---

# 125. Example

```text
employee reports item
```

but:

```text
Airport Lost Property Office
```

is the legal/physical custodian.

Dzoka therefore separates reporter and custodian.

---

# 126. Verification Authority ≠ Custody Authority

Likewise:

```text
person reviewing claim
```

need not be:

```text
person authorized to physically release item
```

Dzoka treats these as separate roles.

---

# 127. No Fraud/Risk Engine

La2eet does not provide a substantial fraud-scoring architecture.

Dzoka builds risk separately.

---

# 128. Useful Fraud Signals Missing

Examples include:

```text
many unrelated claims
reused evidence
device/account correlation
multiple failed private challenges
receipt reuse
serial conflicts
identity anomalies
```

---

# 129. No Evidence Coverage

La2eet does not calculate:

```text
EvidenceCoverage
```

because verification is fundamentally finder judgment.

Dzoka needs explicit coverage when multiple evidence classes are combined.

---

# 130. No AI Verification

This is not necessarily a weakness.

It avoids unsafe over-automation.

Dzoka should treat AI as:

```text
verification assistant
```

not:

```text
ownership authority
```

---

# 131. No Federation

La2eet is a centralized application.

It does not provide:

```text
peer nodes
signed federation
cross-institution claims
federated verification
```

---

# 132. No Custody State Machine

The project jumps roughly from:

```text
claim approved
```

to:

```text
chat
```

then:

```text
mark returned
```

There is no robust chain of custody.

---

# 133. Dzoka Extension

Dzoka requires:

```text
current custodian
transfer authorization
handover evidence
courier custody
recipient confirmation
```

---

# 134. No Handover Token

The project does not implement a secure:

```text
OTP
QR
handover credential
```

for physical release.

Dzoka does.

---

# 135. No Recovery Authorization Aggregate

There is no explicit:

```text
RecoveryAuthorization
```

between claim approval and physical return.

Dzoka introduces this deliberately.

---

# 136. No Audit Trail for Verification Decisions

The basic claim status changes are persisted, but the source does not provide Dzoka's full immutable decision/audit history around ownership review.

---

# 137. Dzoka Verification Audit

Each decision should capture:

```text
reviewer
evidence
result
reason
policy
risk
timestamp
override
```

---

# 138. No Disclosure Audit

La2eet does not track:

```text
who saw expected answer
when
why
```

even though the answer is sensitive.

Dzoka should.

---

# 139. No Model Provenance Requirement

Not relevant to La2eet's current human verification workflow, but Dzoka's AI-generated challenges must retain:

```text
model
version
prompt
input evidence
output
```

provenance.

---

# 140. Tests

Repository search did not identify dedicated automated tests for:

```text
ClaimsController
VerificationQuestion
ChatHub
MarkAsReturned
```

at the audited revision.

---

# 141. Why This Matters

These are some of the platform's most security-sensitive workflows.

Dzoka needs explicit tests covering them.

---

# 142. Required Dzoka Verification Tests

Examples:

```text
correct answer
near-equivalent answer
wrong answer
missing answer
leaked public answer
repeated attempt
question changed after claim
multiple claimants
competing claims
reviewer unauthorized
```

---

# 143. Required Messaging Security Tests

Examples:

```text
unapproved claimant cannot chat
random user cannot chat
sender ID spoof rejected
cross-tenant chat rejected
resolved conversation rejects new messages
messages not broadcast to unrelated users
```

---

# 144. Required Recovery Tests

Examples:

```text
unauthorized actor cannot mark returned
approved claim alone cannot complete custody
handover token required where configured
hold blocks recovery
duplicate completion is idempotent
```

---

# 145. Architecture Strength Summary

The strongest concepts in La2eet are:

```text
finder-defined private questions

mandatory question for found reports

claimant supplies answers before claim review

finder manually reviews evidence

claimant cannot claim own report

one claim per account/item

report-owner authorization for claim decisions

post-approval communication concept

report moderation

Identity-based authentication
```

---

# 146. Architecture Weakness Summary

Major weaknesses include:

```text
no project license

"hash" is plaintext

claim answers stored as one delimited plaintext field

no disclosure tracking

no challenge quality model

no challenge attempt model

no typed claim evidence

no verification-decision audit

weak chat authorization

client-supplied SignalR sender identity

Clients.All chat broadcast

mark-returned endpoint lacks authoritative actor checks

recovery state too shallow

images stored as Base64 in SQL

simple keyword similarity only
```

---

# 147. Security Finding Severity

### Critical / High

```text
unresolved project license for code reuse

client-controlled chat sender identity

lack of server-side approved-claim validation in ChatHub

global SignalR broadcast

mark-returned server authorization weakness

plaintext private expected answers
```

### Medium

```text
plaintext claimant answers

no disclosure audit

lockout documentation/configuration mismatch

business logic duplicated between controller/service
```

---

# 148. What Dzoka Should Adopt

Because the project license is unresolved:

```text
DIRECT CODE ADOPTION:
NONE
```

until license clarification.

---

# 149. What Dzoka Should Adapt Conceptually

Independently implement:

```text
finder-defined private verification questions

mandatory private evidence for appropriate found items

claimant challenge-response workflow

human comparison of private evidence

one claim per user/item

report-owner claim review

verified recovery conversation concept

moderation workflow
```

---

# 150. What Dzoka Should Reference

Use La2eet as a reference for:

```text
simple consumer ownership-verification UX
Arabic/RTL lost-property UX
claim review presentation
post-claim communication flow
ASP.NET Identity deployment pattern
```

---

# 151. Explicitly Do Not Copy

Do not copy:

```text
plaintext ExpectedAnswerHash

plaintext delimited VerificationAnswer

array-position answer association

UI-only chat authorization

client-controlled senderId

Clients.All message delivery

MarkAsReturned without actor authorization

claim approval as sufficient recovery trust

Base64 media storage in relational item row

in-memory substring "similar items" architecture
```

---

# 152. Dzoka Mapping — Verification Question

Upstream:

```text
VerificationQuestion
├── QuestionText
└── ExpectedAnswerHash
```

Dzoka:

```text
VerificationChallenge
├── ChallengeId
├── VerificationSessionId
├── Type
├── Prompt
├── EvidenceReference
├── ProtectedExpectedEvidence
├── Source
├── DisclosureRisk
├── DiscriminativeStrength
├── AttemptsAllowed
├── Status
├── CreatedAt
└── ExpiresAt
```

---

# 153. Dzoka Mapping — Answer

Upstream:

```text
Claim.VerificationAnswer
```

Dzoka:

```text
ChallengeResponse
├── ChallengeId
├── RawResponse
├── NormalizedResponse
├── Interpretation
├── ComparisonResult
├── Confidence
├── SubmittedAt
└── RiskSignals
```

---

# 154. Dzoka Mapping — Decision

Upstream:

```text
Finder
→ Approve / Reject
```

Dzoka:

```text
Evidence
    ↓
Verification Engine
    ↓
Risk Assessment
    ↓
Policy
    ↓
Human Review when required
    ↓
VerificationDecision
```

---

# 155. Dzoka Mapping — Recovery

Upstream:

```text
Claim Approved
    ↓
Chat
    ↓
Mark Returned
```

Dzoka:

```text
Claim Approved
    ↓
RecoveryAuthorization
    ↓
Handover / Logistics
    ↓
CustodyTransfer
    ↓
RecipientConfirmation
    ↓
RecoveryCompleted
```

---

# 156. Dzoka Mapping — Communication

Upstream:

```text
SignalR direct chat
```

Dzoka:

```text
RecoveryConversation
├── authorized members
├── tenant/case scope
├── identity protection
├── audit
├── moderation
└── provider/channel adapters
```

---

# 157. Influence on Ownership Verification

This repository directly reinforces Dzoka's decision that verification can use:

```text
non-public physical knowledge
```

as strong evidence.

Examples:

```text
hidden marking
case detail
internal content
damage
engraving
screen detail
```

---

# 158. Where Dzoka Goes Further

La2eet:

```text
finder manually invents question
```

Dzoka:

```text
Private Evidence Inventory
       ↓
human + AI challenge candidates
       ↓
disclosure filtering
       ↓
guessability filtering
       ↓
strength ranking
       ↓
safe challenge
```

---

# 159. Influence on ItemFingerprint

Certain private facts extracted during found-item processing should enter:

```text
ItemFingerprint.privateEvidence
```

without becoming public searchable attributes.

---

# 160. Example

Found phone image reveals:

```text
small crack beside rear camera
```

Public fingerprint may expose:

```text
black smartphone
```

Private fingerprint retains:

```text
crack at upper-left rear camera edge
```

for verification.

---

# 161. Private Evidence Must Not Improve Public Guessability

Do not expose the private evidence merely because it improves matching.

Separate:

```text
searchable evidence
```

from:

```text
ownership evidence
```

---

# 162. Verification Question Generation

Possible Dzoka flow:

```text
FoundItemFingerprint
        ↓
private distinctive features
        ↓
remove publicly disclosed traits
        ↓
rank by discriminative value
        ↓
generate challenge
        ↓
validate no answer leakage
        ↓
issue to claimant
```

---

# 163. Example Good Challenge

Private evidence:

```text
small triangular sticker under laptop
```

Challenge:

```text
Is there any marking or sticker on the underside?
If yes, describe it.
```

Do not ask:

```text
What colour is the triangular sticker under the laptop?
```

because that leaks too much.

---

# 164. Exact vs Semantic Answers

Some questions support exact comparison:

```text
last four serial digits
```

Others require semantic interpretation:

```text
describe the sticker
```

Dzoka must distinguish these challenge types.

---

# 165. Exact Challenge

Possible evaluation:

```text
normalize
→ keyed comparison
```

with no need to expose expected plaintext.

---

# 166. Semantic Challenge

Possible:

```text
claimant response
    ↓
structured interpretation
    ↓
compare with protected evidence
    ↓
human review if ambiguous
```

---

# 167. LLM Role

An LLM may interpret:

```text
"tiny red triangle underneath"
```

against protected evidence describing:

```text
"small triangular crimson sticker on underside"
```

but the LLM must not independently approve ownership.

---

# 168. Multi-Evidence Verification

One private answer should not necessarily determine recovery.

Dzoka combines:

```text
challenge answers
prior image evidence
identifier evidence
document evidence
fraud risk
claim context
```

---

# 169. Evidence Strength

Private evidence should carry different strengths.

Example:

```text
case colour
→ weak

unique scratch
→ medium

serial suffix
→ strong

pre-loss photo with same unique damage
→ very strong
```

---

# 170. Influence on Privacy Architecture

This audit reinforces a critical Dzoka principle:

> The system may need to know more about an item than it is allowed to reveal publicly.

Therefore:

```text
ItemFingerprint
```

must support different projections:

```text
PUBLIC
RESTRICTED
SECRET
```

---

# 171. Influence on Data Architecture

Verification information should be stored separately from general public report fields.

Potential tables:

```text
verification_sessions
verification_challenges
challenge_expected_evidence
challenge_responses
verification_decisions
challenge_disclosures
```

---

# 172. Influence on Security Architecture

Private challenge evidence must support:

```text
encryption
restricted DB roles
audit
minimum disclosure
retention
attempt controls
```

---

# 173. Influence on Claim Workflow

This repository strongly validates the sequence:

```text
Match
    ↓
Claim
    ↓
Private Verification
```

rather than:

```text
Match
    ↓
Return Item
```

---

# 174. Influence on Messaging Architecture

The implementation also provides a useful negative lesson:

> Unlocking a chat button in the UI does not establish authorization.

Dzoka must enforce messaging access at the server/domain boundary.

---

# 175. Influence on Custody Architecture

Likewise:

> Showing a “mark returned” button only to certain users does not safely implement recovery.

Physical-return state must be guarded by the custody/recovery domain.

---

# 176. Comparison with globalenglish01/lostfound

`globalenglish01/lostfound` is far stronger for:

```text
matching
hard conflicts
evidence coverage
model provenance
retrieval
```

La2eet is valuable specifically for:

```text
private ownership questions
finder review
claimant response UX
post-approval communication concept
```

---

# 177. Comparison with Lost-and-Found-Tracking-System

`lost-and-found-system` is stronger for:

```text
claim administration
fraud/risk ideas
institutional workflow
GeoJSON
audit
```

La2eet provides the clearer:

```text
finder-owned private challenge
```

pattern.

---

# 178. Dzoka Synthesis

Together:

```text
globalenglish
    → matching evidence discipline

FuricBond
    → multimodal visual evidence

Lost-and-Found-Tracking-System
    → claims / risk / institutions

La2eet
    → private challenge workflow
```

form complementary architectural inputs.

---

# 179. Relevant Dzoka Documents

This audit should directly influence:

```text
DESIGN.md

docs/design/item-fingerprint.md
docs/design/matching-engine.md
docs/design/ownership-verification.md
docs/design/claim-workflow.md
docs/design/custody-state-machine.md

docs/architecture/security-architecture.md
docs/architecture/privacy-architecture.md
docs/architecture/data-architecture.md
docs/architecture/whatsapp-architecture.md
```

---

# 180. ADRs Potentially Influenced

Potential ADRs include:

```text
protect private expected verification evidence

separate ownership verification from matching

server-enforce recovery conversation membership

server-enforce recovery completion

retain finder-defined challenges alongside AI challenges

track challenge disclosure state
```

---

# 181. Recommended Lock Entry

Add to:

```text
docs/source-audit/source-audit.lock.json
```

```json
{
  "id": "la2eet",
  "category": "platforms",
  "repository": "https://github.com/Malak-Rashad/LostAndFound",
  "revision": "d5d7bfaf043396a32b40c264c647f6456cadb633",
  "license": "UNRESOLVED",
  "licenseNote": "No root project LICENSE file found at audited revision.",
  "classification": "REFERENCE",
  "secondaryClassification": "ADAPT_VERIFICATION_CONCEPTS",
  "audit": "docs/source-audit/platforms/la2eet.md",
  "auditedAt": "2026-10-04"
}
```

---

# 182. Re-Audit Triggers

Re-audit if upstream:

```text
adds a project license

actually hashes/encrypts expected answers

adds automatic challenge comparison

adds challenge attempt limits

adds disclosure tracking

adds competing-claim handling

fixes SignalR authorization

adds server-side recovery authorization

adds custody/handover state

adds verification/audit tests
```

---

# 183. Source Claims vs Actual Implementation

## README claim

```text
verification questions
```

### Actual

```text
IMPLEMENTED
```

---

## README claim

```text
finder reviews claimant answers
```

### Actual

```text
IMPLEMENTED
```

---

## Apparent field claim

```text
ExpectedAnswerHash
```

### Actual

```text
PLAINTEXT
```

---

## README claim

```text
Verified Chat
```

### Actual

```text
UI FLOW EXISTS
SERVER AUTHORIZATION IS INSUFFICIENT
```

---

## README claim

```text
users can mark item returned
```

### Actual

```text
IMPLEMENTED
BUT SERVER-SIDE AUTHORIZATION IS TOO WEAK
```

---

# 184. Security Findings Summary

## Positive

```text
ASP.NET Core Identity

password policy

email confirmation flow

finder-only claim review

finder-only approve/reject checks

no expected answer intentionally exposed in claimant form

one claim per account/item
```

## Serious weaknesses

```text
private expected answer stored plaintext

claimant answers stored plaintext

project license missing

chat sender identity controlled by client

no approved-claim enforcement inside ChatHub

messages broadcast with Clients.All

MarkAsReturned lacks proper actor authorization

verification decisions lack detailed audit trail
```

---

# 185. Verification Findings Summary

## Strong concept

```text
finder creates private question
claimant answers
finder manually compares
```

## Weak implementation

```text
plaintext secret
no hash
no encryption
no typed response records
no disclosure state
no automated comparison
no strength scoring
no challenge quality validation
```

---

# 186. Recovery Findings Summary

## Good

```text
approval does not instantly set returned
post-approval communication exists
```

## Weak

```text
no RecoveryAuthorization
no custody transition
no handover credential
weak server authorization
simple IsResolved boolean
```

---

# 187. Final Architecture Lesson

La2eet demonstrates a powerful idea:

```text
PUBLIC DESCRIPTION
        ≠
ALL KNOWLEDGE ABOUT ITEM
```

Some item facts should remain hidden so they can later serve as proof.

That principle becomes:

```text
PUBLIC FINGERPRINT
        +
PRIVATE OWNERSHIP EVIDENCE
```

inside Dzoka.

---

# 188. Dzoka Evolution

La2eet:

```text
Finder writes secret question
        ↓
Stores expected answer
        ↓
Claimant answers
        ↓
Finder compares
        ↓
Approve / Reject
```

Dzoka evolves this into:

```text
PRIVATE EVIDENCE DISCOVERY
        ↓
CLASSIFICATION
        ↓
DISCLOSURE CHECK
        ↓
CHALLENGE SELECTION
        ↓
SAFE QUESTION GENERATION
        ↓
CLAIMANT RESPONSE
        ↓
EXACT / SEMANTIC EVALUATION
        ↓
FRAUD / RISK
        ↓
POLICY
        ↓
HUMAN REVIEW WHERE REQUIRED
        ↓
VERIFICATION DECISION
        ↓
RECOVERY AUTHORIZATION
```

---

# 189. Final Decision

```text
PRIMARY CLASSIFICATION:
REFERENCE
```

because the project has no verified root license.

Secondary:

```text
ADAPT VERIFICATION WORKFLOW CONCEPTS
```

### Strong concepts to independently implement

```text
finder-defined private questions

mandatory private verification evidence

claimant challenge responses

human ownership review

claim-per-item restriction

approved-claim communication concept
```

### Extend substantially

```text
secret storage
challenge selection
challenge quality
disclosure tracking
attempt controls
fraud/risk
decision audit
messaging authorization
recovery authorization
custody/handover
```

### Explicitly reject

```text
ExpectedAnswerHash stored as plaintext

claim responses concatenated into one string

position-based answer mapping

UI-only security

client-supplied sender identity

global chat broadcasts

unauthorized return-state mutation

plaintext secret evidence in ordinary database fields
```

---

# 190. Final Assessment

La2eet should **not** become a Dzoka code foundation.

Its value is more important than that.

It demonstrates a domain concept that many other lost-and-found projects miss:

> **The finder may possess private facts about the physical object that can later be used to challenge a claimant without revealing those facts publicly.**

Dzoka should retain that idea and harden it dramatically.

The resulting Dzoka model becomes:

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
             Risk + Policy
                     ↓
             Human Decision
                     ↓
             RecoveryAuthorization
                     ↓
             Secure Custody Handover
```

The core lesson is:

> **La2eet gives Dzoka an excellent ownership-verification idea, but Dzoka must replace its plaintext secrets, UI-level trust assumptions, and shallow recovery controls with cryptographically protected evidence, server-enforced authorization, auditable decisions, and a real custody/recovery state machine.**