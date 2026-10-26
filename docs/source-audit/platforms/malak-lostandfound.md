# Source audit: malak-lostandfound

Status: COMPLETE

Scope: architecture-relevant static review of the listed source ranges, license evidence, deployment entry points and tests/evaluation where present. No upstream tests were executed and no upstream implementation was copied into Dzoka. Unreviewed subsystems and integration acceptance work are called out explicitly.

## Repository identity

https://github.com/Malak-Rashad/LostAndFound

## Audited branch

master. The exact commit below is authoritative; branch HEAD may move.

## Exact commit SHA

d5d7bfaf043396a32b40c264c647f6456cadb633

## Audit date

2026-10-26

## License

UNRESOLVED. No root project LICENSE/COPYING in the complete pinned tree; missing licensing is a completed audit finding, not permission to copy.

## Files inspected

- [LostAndFound/Services/ClaimsService.cs](https://github.com/Malak-Rashad/LostAndFound/blob/d5d7bfaf043396a32b40c264c647f6456cadb633/LostAndFound/Services/ClaimsService.cs), lines 1-65; role: implementation.
- [LostAndFound/Controllers/ClaimsController.cs](https://github.com/Malak-Rashad/LostAndFound/blob/d5d7bfaf043396a32b40c264c647f6456cadb633/LostAndFound/Controllers/ClaimsController.cs), lines 1-183; role: implementation.
- [LostAndFound/Models/VerificationQuestion.cs](https://github.com/Malak-Rashad/LostAndFound/blob/d5d7bfaf043396a32b40c264c647f6456cadb633/LostAndFound/Models/VerificationQuestion.cs), lines 1-17; role: implementation.
- [LostAndFound/LostAndFound.csproj](https://github.com/Malak-Rashad/LostAndFound/blob/d5d7bfaf043396a32b40c264c647f6456cadb633/LostAndFound/LostAndFound.csproj), lines 1-22; role: deployment.
- [LostAndFound/Program.cs](https://github.com/Malak-Rashad/LostAndFound/blob/d5d7bfaf043396a32b40c264c647f6456cadb633/LostAndFound/Program.cs), lines 1-65; role: implementation.
- [LostAndFound/ModelConfigrations/VerificationQuestionConfiguration.cs](https://github.com/Malak-Rashad/LostAndFound/blob/d5d7bfaf043396a32b40c264c647f6456cadb633/LostAndFound/ModelConfigrations/VerificationQuestionConfiguration.cs), lines 1-31; role: implementation.

## Runtime architecture

ASP.NET Core MVC/.NET 9 with EF Core SQL Server, ASP.NET Identity and SignalR services registered in Program.cs.

## Domain model

Items have finder-defined VerificationQuestions; Claims collect answers and move through human approval/rejection.

## Storage model

ExpectedAnswerHash is a string field name, not proof of hashing. EF configuration maps it to a length-limited column; submitted VerificationAnswer is a joined plaintext string. SQL Server time defaults/local application timestamps need normalization.

## AI and matching implementation

No inference or calibrated matcher appears in reviewed claim paths. Human comparison of private questions is the relevant workflow; a property named Hash must not be treated as cryptographic evidence.

## Security model

ClaimsController has [Authorize]; Approve/Reject and Review require the current user to own the reported item. POST actions lack local antiforgery attributes; global antiforgery enforcement was not established by the reviewed registration.

## API design

MVC Create/Approve/Reject/Review actions. CreatePost derives the user through UserManager, prevents self-claim and checks existing claims before insertion.

## Event model

EF SaveChanges drives workflow; SignalR is registered but not a durable recovery-event/outbox implementation in reviewed paths.

## Deployment model

The .csproj pins .NET 9 dependency versions and Program.cs configures SQL Server, password policy, lockout and cookies. Full server secrets/deployment assets were not downloaded.

## Test coverage

No executable application test project found in the pinned tree. A QA documentation file exists; it was not represented as executed tests.

## Strengths

Finder-private questions, authenticated per-item human review and separation of claimant from item reporter.

## Weaknesses

Hash-named field does not establish cryptographic storage; delimited answers lose structure, and check-then-insert duplicates need database/concurrency protection.

## Security concerns

Private answers need encryption/access logging and disclosure limits. Review CSRF globally; human item-reporter approval alone may not meet institutional release policy.

## Scalability concerns

Synchronous EF queries and in-memory ordering occur in claim listing; no institutional multi-tenancy, claim race test or throughput measurement established.

## Licensing consequences

No direct project-code reuse permitted by this audit. Study concepts and independently implement Dzoka contracts; seek explicit permission before any future copying.

## Reusable components

Reference private challenge and finder-review interaction concepts. La2eet is the same source, not independent corroboration.

## Do not reuse

No code copying without project license. Exclude naming-based crypto claims, plaintext joined secrets and untested approval concurrency.

## Architecture lessons

Store challenge definitions, private expected evidence and answers separately, and bind human review to deterministic institutional release policy.

## Disposition

REFERENCE. Applies to this declared integration scope. It is not a claim of production readiness, blanket source reuse approval or completion of later integration acceptance tests.

## ADRs triggered

Proposed investigations: private ownership evidence; trusted role assignment; deterministic release authorization; transaction/idempotency boundaries. This audit supplies evidence for later technical ADRs; it does not finalize the architecture.

## Unresolved questions

No root license; full item-creation hashing flow, message authorization, CSRF configuration and database constraints require targeted integration work if considered further.
