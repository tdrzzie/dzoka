# Penpot screen register

Design receipt dated 2026-11-27. Dzoka remains a temporary internal codename.
This records the original 26-screen pass. The later [screen and flow inventory](screen-flow-inventory.md) is the current coverage/checkpoint authority and includes additional saved concepts, partial flows and remaining work.
This new register records the Penpot design scope; supplied research/design documents remain authoritative within their stated scope and are preserved.

## Design location and foundations

- Connected Penpot VPS file: **Dzoka**, file ID `456d79de-921a-81c4-8009-02637f9b3b01`.
- Pages: **Design language**, **Your recovery**, **Institution workspace**.
- Mobile boards: **390 × 844**. Staff desktop boards: **1440 × 1000**.
- Palette: forest `#123D30`, primary `#235C45`, lime `#DCEF8B`, background `#F5F6F1`, muted text `#596D61`.
- Typography: Manrope headings and Inter body text.
- Foundations include 14 native color tokens and four reusable action components.
- All displayed people, cases, items, counts and operational records are fictional sample data.

## Your recovery: mobile screens

| Screen | Design scope |
|---|---|
| Home | Recovery entry points, active case and reporting actions |
| Report lost item | Original item description and optional evidence intake |
| Place and time | Location and time context with uncertainty |
| Report submitted | Case acknowledgement and asynchronous progress |
| Possible matches | Safe candidate discovery without ownership approval |
| Candidate detail | Minimized candidate information and claim entry |
| Private ownership check | Restricted verification step |
| Recovery progress | Distinct claim, review and recovery stages |
| Collection pass | Expiring collection credential concept and staff check |
| Finder report | Found-item reporting journey |
| Private messages | Contact-preserving recovery communication |
| My items | Private registered-item surface |
| Finder handover | Handover coordination separate from physical custody acceptance |
| Back together | Return outcome and confirmation concept |

## Institution workspace: desktop screens

| Screen | Design scope |
|---|---|
| Institution overview | Scoped work queues and recovery overview |
| Found inventory | Institutional holdings and operational statuses |
| Found-item intake | Public report and protected observations |
| Match review | Candidate evidence, conflicts and coverage |
| Independent claim review | Accountable human assessment and release boundary |
| Custody and transfers | Current possession, acceptance and append-oriented history |
| Trusted network | Attributed peer discovery and trust status |
| Authorised investigation | Purpose-bound investigation and restricted evidence |
| Recovery analytics | Illustrative operational reporting |
| Workspace administration | Institutional administration and configuration concepts |
| Collection desk | Staff collection checks and handover action |
| Return receipt | Recorded return and auditable outcome |

## Decisions carried into the screens

- Retrieval, similarity, confidence, ownership verification and release authorization remain separate decisions. A candidate is a possible match.
- Evidence coverage is distinct from similarity; missing evidence is unavailable. Reliable identity contradictions take precedence over aggregate similarity.
- Release requires an independent authorized human reviewer under the current local recovery decision. AI does not authorize property release.
- Public views omit private contact data, secret ownership facts, protected identifiers and precise secure storage. Private verification does not reveal expected answers.
- Federation discovery stays restricted to the approved minimized projection and peer attribution. Independent peer ranks are not globally comparable confidence.
- Custody records physical possession independently of claims, booking and delivery. Transfers need acceptance; history is append-oriented, and holds block release.
- Registration and QR possession do not establish ownership. Courier delivery does not by itself establish confirmed owner return. Rewards do not grant release authority.
- Investigations require scoped authorization and privacy-preserving evidence access; no default facial recognition is implied.

## Evidence and limitations

The source audit gate was executed on 2026-11-27 with `python tools/audit_registry.py gate` and returned `PASS: mandatory source audit gate`.
This confirms the registered audit prerequisite only; it does not validate the UI, a deployed product or institutional acceptance.

These are screen concepts with 180 native navigation interactions, including report/claim, finder, staff review and handover paths. Forms do not submit data, and QR graphics are nonfunctional. Pending review remains distinct from the separately illustrated approved-collection state.
Native file validation returned no errors. Text and table containment checks across all 26 screens found no remaining overflow after corrections. Selected native PNG exports were visually inspected: home, lost report, private verification, collection pass, institutional overview and independent claim review. These bounded checks do not establish accessibility conformance or usability acceptance.
Muted text on the pale surface has a calculated sRGB contrast ratio of 4.78:1; this single token-pair check is not a complete accessibility audit. A named design version was saved as `Dzoka · 26-screen design concept · 2026-11-27`.
Remaining design work includes additional breakpoints, complete alternate/error states, form interactions, user research and keyboard/screen-reader evaluation. Authentication, system-wide administration and detailed commercial logistics/payment interfaces need separate design coverage.
No runtime implementation, accessibility conformance, security enforcement, representative accuracy, operational performance or production readiness follows from these designs.
Implementation must continue to follow [the specification](../dzoka.md), [accepted baseline](../architecture/baseline.md), applicable ADRs and [current project status](../../STATUS.md).
