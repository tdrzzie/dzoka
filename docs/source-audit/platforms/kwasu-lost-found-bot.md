# Source audit: kwasu-lost-found-bot

Status: COMPLETE

Scope: architecture-relevant static review of the listed source ranges, license evidence, deployment entry points and tests/evaluation where present. No upstream tests were executed and no upstream implementation was copied into Dzoka. Unreviewed subsystems and integration acceptance work are called out explicitly.

## Repository identity

https://github.com/zhuluh247/kwasu-lost-found-bot

## Audited branch

main. The exact commit below is authoritative; branch HEAD may move.

## Exact commit SHA

e4863739135594d8ce0a541be6029d0bba5ae95c

## Audit date

2026-10-26

## License

UNRESOLVED. No root project LICENSE/COPYING in the complete pinned tree; missing licensing is a completed audit finding, not permission to copy.

## Files inspected

- [package.json](https://github.com/zhuluh247/kwasu-lost-found-bot/blob/e4863739135594d8ce0a541be6029d0bba5ae95c/package.json), lines 1-15; role: deployment.
- [index.js](https://github.com/zhuluh247/kwasu-lost-found-bot/blob/e4863739135594d8ce0a541be6029d0bba5ae95c/index.js), lines 1-12, 19-60, 117-193, 221-270, 438-447, 505-535, 629-638; role: implementation.

## Runtime architecture

Single Express/Node file integrates Twilio WhatsApp and Firebase client SDK Realtime Database; conversational state is externalized to database paths.

## Domain model

Lost/found reports, reporter identity, management codes, conversation steps and claimed/recovered flags.

## Storage model

Reads reports and user-state subtrees from Firebase; media handling uses axios and Base64-style storage paths in the source. No immutable custody log or tenant schema was established.

## AI and matching implementation

findMatchingFoundItems is substring keyword counting, with an image-presence bonus. It declares matchScore as const then attempts += when imagery exists; that branch throws and the broad catch returns an empty result.

## Security model

handleMarkItem checks both reporter and a management code, but webhook identity is read from req.body.From. No signature-validation middleware is visible on the inspected /whatsapp route.

## API design

POST /whatsapp consumes Twilio-form fields and emits TwiML. Report-management commands are distinct from proof of ownership.

## Event model

Database mutations and TwiML replies occur inside request handlers; timer-based state cleanup is not a durable job/replay system.

## Deployment model

package.json starts index.js and lists Express/Twilio/Firebase/dotenv; code requires axios but the package does not declare it. Reproducibility is incomplete.

## Test coverage

No executable tests found in the four-entry pinned repository tree; no upstream code executed.

## Strengths

WhatsApp menus, resumable conversation intent and separate report-management commands show useful channel UX.

## Weaknesses

Keyword search scans all reports; an image branch mutates a const; Math.random produces report codes. Debug logs include sender and message content.

## Security concerns

Authenticate Twilio webhooks before trusting sender identity; avoid logging private messages and using management codes as ownership proof. Media fetch needs URL/provider allowlists and size/MIME enforcement.

## Scalability concerns

Full reports-subtree reads for searching and code lookup grow with all reports, and Base64 media inflates realtime-database payloads.

## Licensing consequences

No direct project-code reuse permitted by this audit. Study concepts and independently implement Dzoka contracts; seek explicit permission before any future copying.

## Reusable components

Reference conversational intake and report-management UX. Build provider-independent messaging and private relay separately.

## Do not reuse

No project source copying without license. Exclude unverified webhook identity, public contact sharing, image-presence bonuses, missing dependency declarations and scan-all search.

## Architecture lessons

Channel identity, conversation state, report administration and ownership verification are separate trust boundaries.

## Disposition

REFERENCE. Applies to this declared integration scope. It is not a claim of production readiness, blanket source reuse approval or completion of later integration acceptance tests.

## ADRs triggered

Proposed investigations: private ownership evidence; trusted role assignment; deterministic release authorization; transaction/idempotency boundaries. This audit supplies evidence for later technical ADRs; it does not finalize the architecture.

## Unresolved questions

No root license. Firebase rules and real deployment are not present in the reviewed tree; no assurance of backend auth, retention or rate limits can be inferred.
