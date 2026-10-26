# Source audit: smart-asset-finder

Status: COMPLETE

Scope: architecture-relevant static review of the listed source ranges, license evidence, deployment entry points and tests/evaluation where present. No upstream tests were executed and no upstream implementation was copied into Dzoka. Unreviewed subsystems and integration acceptance work are called out explicitly.

## Repository identity

https://github.com/fredopoku/Smart-Asset-Finder

## Audited branch

main. The exact commit below is authoritative; branch HEAD may move.

## Exact commit SHA

8ac4484354d23aea42e1a569e2f127a878049b1e

## Audit date

2026-10-26

## License

ALL_RIGHTS_RESERVED. README lines 276-278 explicitly reserve all rights; no root permissive license found. No code reuse permission established.

## Files inspected

- [classes/QrScan.php](https://github.com/fredopoku/Smart-Asset-Finder/blob/8ac4484354d23aea42e1a569e2f127a878049b1e/classes/QrScan.php), lines 1-65; role: implementation.
- [classes/Whatsapp.php](https://github.com/fredopoku/Smart-Asset-Finder/blob/8ac4484354d23aea42e1a569e2f127a878049b1e/classes/Whatsapp.php), lines 1-65; role: implementation.
- [classes/AiMatcher.php](https://github.com/fredopoku/Smart-Asset-Finder/blob/8ac4484354d23aea42e1a569e2f127a878049b1e/classes/AiMatcher.php), lines 1-65, 72-125, 158-205, 251-260; role: implementation.
- [README.md](https://github.com/fredopoku/Smart-Asset-Finder/blob/8ac4484354d23aea42e1a569e2f127a878049b1e/README.md), lines 1-65, 157-166, 268-278; role: license.
- [classes/Login.php](https://github.com/fredopoku/Smart-Asset-Finder/blob/8ac4484354d23aea42e1a569e2f127a878049b1e/classes/Login.php), lines 1-65; role: implementation.

## Runtime architecture

PHP application with mysqli queries, session-based login, synchronous Claude-assisted matching and an outbound UltraMsg helper.

## Domain model

Items, security Q&A, claims, QR sightings, notifications and points are visible in the reviewed classes and table queries.

## Storage model

AiMatcher queries item_list and item_security_qa; expected normalized answers are read as text. QrScan inserts coordinates, IP and user agent in qr_scans. Production/sample database contents were intentionally not downloaded.

## AI and matching implementation

AiMatcher chooses Claude or local text similarity based on configuration. scoreClaimAnswers returns 100 when there are no questions, which is absence of verification rather than proof. No visual embedding implementation was established in this class.

## Security model

Login uses prepared queries, password_verify and session regeneration. QrScan accepts client coordinates and proxy-related headers; these are untrusted observations, not verified item movements.

## API design

PHP request handlers and an outbound WhatsApp message helper. Whatsapp.php is notification transport, not a complete inbound conversational WhatsApp intake channel.

## Event model

Notifications are inserted in SQL and external messages sent through synchronous helper calls; no transactional outbox/replay contract established.

## Deployment model

Reviewed PHP entry points require shared config.php and a configured database. No reproducible production container or application lockfile was inspected; README deployment prose is orientation only.

## Test coverage

No application test suite found in the pinned tree after excluding bundled vendor material. No tests executed.

## Strengths

Prepared-query examples, explicit claim questions, QR-sighting records and reusable product workflow ideas.

## Weaknesses

No-question verification returns full score; missing messaging credentials silently skip delivery. Scores are not calibrated ownership confidence.

## Security concerns

Expected answers, exact scan locations and finder-contact templates require privacy boundaries. Client-forwarded IP headers need trusted-proxy policy; QR observations must not be treated as established custody.

## Scalability concerns

Per-item Claude requests and synchronous messaging have provider cost/timeout implications; no bounded candidate-retrieval benchmark was observed.

## Licensing consequences

No direct project-code reuse permitted by this audit. Study concepts and independently implement Dzoka contracts; seek explicit permission before any future copying.

## Reusable components

Reference QR relay, claim-question and notification workflow ideas only. README lines 276-278 explicitly reserve rights.

## Do not reuse

No copying project code. Exclude treating absent questions as verified ownership, direct contact disclosure and silent provider failure.

## Architecture lessons

Missing verification evidence is not successful verification. Messaging needs inbound workflow state, authenticated webhooks and durable delivery status.

## Disposition

REFERENCE. Applies to this declared integration scope. It is not a claim of production readiness, blanket source reuse approval or completion of later integration acceptance tests.

## ADRs triggered

Proposed investigations: private ownership evidence; trusted role assignment; deterministic release authorization; transaction/idempotency boundaries. This audit supplies evidence for later technical ADRs; it does not finalize the architecture.

## Unresolved questions

No root open-source license. Full CSRF enforcement, payment reconciliation, private data exposure and transaction/idempotency semantics were not certified by this bounded review.
