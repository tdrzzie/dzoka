# Screen and flow inventory

Repository-derived concept design inventory · 2026-11-27 · Dzoka is a temporary codename.

## Current coverage

**130 screen/state concepts designed; none of the named concepts are missing.** Live board evidence verifies all 130 application frames. Two additional boards contain the recovery decision map and clickable 26-flow catalogue.

**Flows: 15 designed concept flows, 11 partial.** Partial flows have explicit remaining detail below; screen presence is not full workflow completion. 807 native interaction sources are recorded across application screens and maps.

[Open Dzoka flow map](https://penpot.nyasham.dev/?screen=workspace&team-id=a5efc7a3-ef74-80db-8008-f02c367e6621&file-id=456d79de-921a-81c4-8009-02637f9b3b01&page-id=46d4ce92-2766-80d6-8009-0277f03d8174) · [JSON inventory](screen-flow-inventory.json) · [Native verification receipt](penpot-verification-2026-11-27.json)

## Status semantics

Designed screens are editable named concepts with live board evidence. A designed flow has actors, guarded branches, outcomes and connected primary views/catalogue access. Partial flows have named views but incomplete interaction/report/responsive detail. Neither status establishes implementation, complete UX specification, accessibility conformance, security enforcement or production readiness.

The before column preserves the initial audit: 24 designed and 2 partial existing concepts; 96 initial proposed additions, followed by eight critical variants. The user reconnected after an earlier interrupted checkpoint. The two missing variants and original relay correction are now saved. Source authority remains the specification, baseline, current STATUS and ADRs 0056–0058/0062/0064/0065. Supplied research remains preserved.

## Screen/state register

| ID | View | Page | Before | Current | Board evidence |
|---|---|---|---|---|---|
| M01 | 01 · Home | 01 · Your recovery | designed | designed | `3885c71d-2bc7-8052-8009-026565a41470` |
| M02 | 02 · Report a lost item | 01 · Your recovery | designed | designed | `3885c71d-2bc7-8052-8009-026566a3b5fa` |
| M03 | 03 · Place and time | 01 · Your recovery | designed | designed | `3885c71d-2bc7-8052-8009-0265674187fb` |
| M04 | 04 · Report submitted | 01 · Your recovery | designed | designed | `3885c71d-2bc7-8052-8009-0265680aeb15` |
| M05 | 05 · Possible matches | 01 · Your recovery | designed | designed | `3885c71d-2bc7-8052-8009-0265ad957c5d` |
| M06 | 06 · Candidate detail | 01 · Your recovery | designed | designed | `3885c71d-2bc7-8052-8009-0265af456eef` |
| M07 | 07 · Private ownership check | 01 · Your recovery | designed | designed | `3885c71d-2bc7-8052-8009-0265aff60b44` |
| M08 | 08 · Recovery progress | 01 · Your recovery | designed | designed | `3885c71d-2bc7-8052-8009-0265b08dfdca` |
| M09 | 09 · Collection pass | 01 · Your recovery | designed | designed | `3885c71d-2bc7-8052-8009-0265fe0199b4` |
| M10 | 10 · Finder report | 01 · Your recovery | designed | designed | `3885c71d-2bc7-8052-8009-0266020e7c28` |
| M11 | 11 · Private messages | 01 · Your recovery | partial | designed | `3885c71d-2bc7-8052-8009-026603150fb3` |
| M12 | 12 · My items | 01 · Your recovery | designed | designed | `3885c71d-2bc7-8052-8009-0266048a68bc` |
| M13 | 13 · Finder handover | 01 · Your recovery | designed | designed | `3885c71d-2bc7-8052-8009-0266a7875bb4` |
| M14 | 14 · Back together | 01 · Your recovery | designed | designed | `3885c71d-2bc7-8052-8009-0266a9ad8229` |
| D01 | 01 · Institution overview | 02 · Institution workspace | designed | designed | `3885c71d-2bc7-8052-8009-0266df49b06a` |
| D02 | 02 · Found inventory | 02 · Institution workspace | designed | designed | `3885c71d-2bc7-8052-8009-0266e39e4ea2` |
| D03 | 03 · Found-item intake | 02 · Institution workspace | designed | designed | `3885c71d-2bc7-8052-8009-0266e75bc0a6` |
| D04 | 04 · Match review | 02 · Institution workspace | designed | designed | `3885c71d-2bc7-8052-8009-02672a0f4faf` |
| D05 | 05 · Independent claim review | 02 · Institution workspace | designed | designed | `3885c71d-2bc7-8052-8009-02672f9e6994` |
| D06 | 06 · Custody & transfers | 02 · Institution workspace | designed | designed | `3885c71d-2bc7-8052-8009-0267340adc9e` |
| D07 | 07 · Trusted network | 02 · Institution workspace | designed | designed | `3885c71d-2bc7-8052-8009-0267960b13fd` |
| D08 | 08 · Authorised investigation | 02 · Institution workspace | designed | designed | `3885c71d-2bc7-8052-8009-02679dc62314` |
| D09 | 09 · Recovery analytics | 02 · Institution workspace | designed | designed | `3885c71d-2bc7-8052-8009-0267a3a46f04` |
| D10 | 10 · Workspace administration | 02 · Institution workspace | partial | designed | `3885c71d-2bc7-8052-8009-026802666291` |
| D11 | 11 · Collection desk | 02 · Institution workspace | designed | designed | `3885c71d-2bc7-8052-8009-02680c6ca5a9` |
| D12 | 12 · Return receipt | 02 · Institution workspace | designed | designed | `3885c71d-2bc7-8052-8009-026814a95cd2` |
| A01 | Welcome to Dzoka | 03 · Access & preferences | missing | designed | `3885c71d-2bc7-8052-8009-026e291218ea` |
| A02 | Sign in securely | 03 · Access & preferences | missing | designed | `3885c71d-2bc7-8052-8009-026e29e7a370` |
| A03 | Check your sign-in | 03 · Access & preferences | missing | designed | `3885c71d-2bc7-8052-8009-026e2a91b403` |
| A04 | Recover account access | 03 · Access & preferences | missing | designed | `3885c71d-2bc7-8052-8009-026e2b54759f` |
| A05 | Choose your workspace | 03 · Access & preferences | missing | designed | `3885c71d-2bc7-8052-8009-026e2be9975c` |
| A06 | Your profile | 03 · Access & preferences | missing | designed | `3885c71d-2bc7-8052-8009-026e2ca63feb` |
| A07 | Updates, your way | 03 · Access & preferences | missing | designed | `3885c71d-2bc7-8052-8009-026e2d6c168d` |
| A08 | Privacy & your data | 03 · Access & preferences | missing | designed | `3885c71d-2bc7-8052-8009-026e2e36f43d` |
| A09 | Review deletion request | 03 · Access & preferences | missing | designed | `3885c71d-2bc7-8052-8009-026e2f3f9b8c` |
| A10 | Language & accessibility | 03 · Access & preferences | missing | designed | `3885c71d-2bc7-8052-8009-026e302cdfcf` |
| C01 | My reports | 04 · Reports & claim states | missing | designed | `3885c71d-2bc7-8052-8009-026e53407785` |
| C02 | Your backpack report | 04 · Reports & claim states | missing | designed | `3885c71d-2bc7-8052-8009-026e53dec8bb` |
| C03 | Update your report | 04 · Reports & claim states | missing | designed | `3885c71d-2bc7-8052-8009-026e5483ae8e` |
| C04 | No matches yet | 04 · Reports & claim states | missing | designed | `3885c71d-2bc7-8052-8009-026e550ebd39` |
| C05 | Part of search is delayed | 04 · Reports & claim states | missing | designed | `3885c71d-2bc7-8052-8009-026e55b208d6` |
| C06 | A little more evidence | 04 · Reports & claim states | missing | designed | `3885c71d-2bc7-8052-8009-026e565a3841` |
| C07 | Your claim is under review | 04 · Reports & claim states | missing | designed | `3885c71d-2bc7-8052-8009-026e56f986a5` |
| C08 | More review is needed | 04 · Reports & claim states | missing | designed | `3885c71d-2bc7-8052-8009-026e579e7196` |
| C09 | Claim decision | 04 · Reports & claim states | missing | designed | `3885c71d-2bc7-8052-8009-026e584931e3` |
| C10 | Withdraw this claim? | 04 · Reports & claim states | missing | designed | `3885c71d-2bc7-8052-8009-026e59075ec2` |
| C11 | Private recovery messages | 04 · Reports & claim states | missing | designed | `3885c71d-2bc7-8052-8009-026e59cfbde4` |
| C12 | Collecting for someone | 04 · Reports & claim states | missing | designed | `3885c71d-2bc7-8052-8009-026e5a91c24e` |
| C13 | This pass has expired | 04 · Reports & claim states | missing | designed | `3885c71d-2bc7-8052-8009-026e5b592224` |
| C14 | Draft saved offline | 04 · Reports & claim states | missing | designed | `3885c71d-2bc7-8052-8009-026e5c4c0fab` |
| L01 | Choose how it comes back | 05 · Delivery rewards & passports | missing | designed | `3885c71d-2bc7-8052-8009-026e7c74077f` |
| L02 | Delivery details & consent | 05 · Delivery rewards & passports | missing | designed | `3885c71d-2bc7-8052-8009-026e7d58c539` |
| L03 | Review delivery quote | 05 · Delivery rewards & passports | missing | designed | `3885c71d-2bc7-8052-8009-026e7e00879a` |
| L04 | Booking needs a check | 05 · Delivery rewards & passports | missing | designed | `3885c71d-2bc7-8052-8009-026e7ea9c15b` |
| L05 | Follow your item | 05 · Delivery rewards & passports | missing | designed | `3885c71d-2bc7-8052-8009-026e7f45a063` |
| L06 | Confirm you received it | 05 · Delivery rewards & passports | missing | designed | `3885c71d-2bc7-8052-8009-026e7feeffd1` |
| L07 | Recovery confirmed | 05 · Delivery rewards & passports | missing | designed | `3885c71d-2bc7-8052-8009-026e809bd3d1` |
| L08 | Offer a voluntary reward | 05 · Delivery rewards & passports | missing | designed | `3885c71d-2bc7-8052-8009-026e8166b860` |
| L09 | Reward & payment status | 05 · Delivery rewards & passports | missing | designed | `3885c71d-2bc7-8052-8009-026e8237e3d5` |
| L10 | Reward dispute | 05 · Delivery rewards & passports | missing | designed | `3885c71d-2bc7-8052-8009-026e830a4642` |
| L11 | Register an item | 05 · Delivery rewards & passports | missing | designed | `3885c71d-2bc7-8052-8009-026e83d5641f` |
| L12 | Your item passport | 05 · Delivery rewards & passports | missing | designed | `3885c71d-2bc7-8052-8009-026e84a512b6` |
| L13 | Tags & finder contact | 05 · Delivery rewards & passports | missing | designed | `3885c71d-2bc7-8052-8009-026e85791960` |
| L14 | A finder can reach the team | 05 · Delivery rewards & passports | missing | designed | `3885c71d-2bc7-8052-8009-026e864ee39d` |
| S01 | Review work, in one place | 06 · Staff decisions & exceptions | missing | designed | `3885c71d-2bc7-8052-8009-026ef1a5fc10` |
| S02 | Resolve competing claims | 06 · Staff decisions & exceptions | missing | designed | `3885c71d-2bc7-8052-8009-026ef4edd291` |
| S03 | Risk & active holds | 06 · Staff decisions & exceptions | missing | designed | `3885c71d-2bc7-8052-8009-026ef766bff0` |
| S04 | Sensitive evidence access | 06 · Staff decisions & exceptions | missing | designed | `3885c71d-2bc7-8052-8009-026ef9a8d4a5` |
| S05 | Protected evidence & challenge | 06 · Staff decisions & exceptions | missing | designed | `3885c71d-2bc7-8052-8009-026efc5d48d8` |
| S06 | Accept a custody transfer | 06 · Staff decisions & exceptions | missing | designed | `3885c71d-2bc7-8052-8009-026f000855dc` |
| S07 | Review an offline conflict | 06 · Staff decisions & exceptions | missing | designed | `3885c71d-2bc7-8052-8009-026f03609f74` |
| S08 | Custody incident & hold | 06 · Staff decisions & exceptions | missing | designed | `3885c71d-2bc7-8052-8009-026f06424da4` |
| S09 | Unclaimed property queue | 06 · Staff decisions & exceptions | missing | designed | `3885c71d-2bc7-8052-8009-026f0904f43f` |
| S10 | Approve a disposition | 06 · Staff decisions & exceptions | missing | designed | `3885c71d-2bc7-8052-8009-026f0c55cf71` |
| S11 | Shipment & receipt exceptions | 06 · Staff decisions & exceptions | missing | designed | `3885c71d-2bc7-8052-8009-026f0ff23a37` |
| S12 | Reauthorise an owner receipt | 06 · Staff decisions & exceptions | missing | designed | `3885c71d-2bc7-8052-8009-026f13c32d0d` |
| S13 | Payment dispute review | 06 · Staff decisions & exceptions | missing | designed | `3885c71d-2bc7-8052-8009-026f17958441` |
| S14 | Reward ledger & reconciliation | 06 · Staff decisions & exceptions | missing | designed | `3885c71d-2bc7-8052-8009-026f1b7b8a2e` |
| S15 | Approve investigation scope | 06 · Staff decisions & exceptions | missing | designed | `3885c71d-2bc7-8052-8009-026f2057d535` |
| S16 | Review a masked item crop | 06 · Staff decisions & exceptions | missing | designed | `3885c71d-2bc7-8052-8009-026f24101464` |
| S17 | Relay moderation & abuse | 06 · Staff decisions & exceptions | missing | designed | `3885c71d-2bc7-8052-8009-026f27c9c283` |
| S18 | Federation dispute review | 06 · Staff decisions & exceptions | missing | designed | `3885c71d-2bc7-8052-8009-026f2c77ac34` |
| O01 | People & permissions | 07 · Organisation administration | missing | designed | `3885c71d-2bc7-8052-8009-026fbcac3281` |
| O02 | Member & service access | 07 · Organisation administration | missing | designed | `3885c71d-2bc7-8052-8009-026fbfa0be9a` |
| O03 | Sites & collection policy | 07 · Organisation administration | missing | designed | `3885c71d-2bc7-8052-8009-026fc23808ec` |
| O04 | Policy versions & holds | 07 · Organisation administration | missing | designed | `3885c71d-2bc7-8052-8009-026fc48dc224` |
| O05 | Retention & deletion requests | 07 · Organisation administration | missing | designed | `3885c71d-2bc7-8052-8009-026fc6c362e9` |
| O06 | Privacy request detail | 07 · Organisation administration | missing | designed | `3885c71d-2bc7-8052-8009-026fc9419a6f` |
| O07 | Integrations & delivery | 07 · Organisation administration | missing | designed | `3885c71d-2bc7-8052-8009-026fcb733a2f` |
| O08 | Notification & webhook operation | 07 · Organisation administration | missing | designed | `3885c71d-2bc7-8052-8009-027013d71ea1` |
| O09 | Trusted peers & keys | 07 · Organisation administration | missing | designed | `3885c71d-2bc7-8052-8009-027017c940e2` |
| O10 | Peer trust & publication | 07 · Organisation administration | missing | designed | `3885c71d-2bc7-8052-8009-02701c1b9bec` |
| O11 | Audit & traceability | 07 · Organisation administration | missing | designed | `3885c71d-2bc7-8052-8009-02701f7d3318` |
| O12 | Event provenance & correction | 07 · Organisation administration | missing | designed | `3885c71d-2bc7-8052-8009-0270223e6b92` |
| O13 | Permitted source ingestion | 07 · Organisation administration | missing | designed | `3885c71d-2bc7-8052-8009-027024962177` |
| O14 | API clients & notification templates | 07 · Organisation administration | missing | designed | `3885c71d-2bc7-8052-8009-0270278b5b5a` |
| P01 | Tenants & node health | 08 · Platform & model operations | missing | designed | `3885c71d-2bc7-8052-8009-027065765863` |
| P02 | Node readiness & restore | 08 · Platform & model operations | missing | designed | `3885c71d-2bc7-8052-8009-027067d8da0b` |
| P03 | Jobs & provider operations | 08 · Platform & model operations | missing | designed | `3885c71d-2bc7-8052-8009-027069ec320c` |
| P04 | Artifact & input provenance | 08 · Platform & model operations | missing | designed | `3885c71d-2bc7-8052-8009-02706c509372` |
| P05 | Datasets & independent labels | 08 · Platform & model operations | missing | designed | `3885c71d-2bc7-8052-8009-02706ee70208` |
| P06 | Evaluation & calibration | 08 · Platform & model operations | missing | designed | `3885c71d-2bc7-8052-8009-027072c991a2` |
| P07 | Approve a model version | 08 · Platform & model operations | missing | designed | `3885c71d-2bc7-8052-8009-0270b6615cbe` |
| P08 | Activation & rollback | 08 · Platform & model operations | missing | designed | `3885c71d-2bc7-8052-8009-0270b9cddf24` |
| P09 | Providers & controlled features | 08 · Platform & model operations | missing | designed | `3885c71d-2bc7-8052-8009-0270bd72e07f` |
| P10 | Quotas & platform audit | 08 · Platform & model operations | missing | designed | `3885c71d-2bc7-8052-8009-0270c0315b46` |
| P11 | Recovery paths & uncertainty | 08 · Platform & model operations | missing | designed | `3885c71d-2bc7-8052-8009-0270c3c85477` |
| P12 | Prevention & hardware consent | 08 · Platform & model operations | missing | designed | `3885c71d-2bc7-8052-8009-0270c7481afe` |
| E01 | Upload needs attention | 09 · Recovery & access states | missing | designed | `3885c71d-2bc7-8052-8009-02710d9d902f` |
| E02 | Media held for review | 09 · Recovery & access states | missing | designed | `3885c71d-2bc7-8052-8009-02710e34bbb2` |
| E03 | A required detail is missing | 09 · Recovery & access states | missing | designed | `3885c71d-2bc7-8052-8009-02710eca8415` |
| E04 | Please wait before trying again | 09 · Recovery & access states | missing | designed | `3885c71d-2bc7-8052-8009-02710f636ea2` |
| E05 | This workspace is restricted | 09 · Recovery & access states | missing | designed | `3885c71d-2bc7-8052-8009-027110002dd5` |
| E06 | Your session has ended | 09 · Recovery & access states | missing | designed | `3885c71d-2bc7-8052-8009-027110c6e8b3` |
| E07 | Collection is on hold | 09 · Recovery & access states | missing | designed | `3885c71d-2bc7-8052-8009-027111a3b768` |
| E08 | Connection restored | 09 · Recovery & access states | missing | designed | `3885c71d-2bc7-8052-8009-02711253ddfd` |
| R01 | Report a lost item · desktop | 10 · Responsive adaptations | missing | designed | `3885c71d-2bc7-8052-8009-027139d199eb` |
| R02 | Ownership check · desktop | 10 · Responsive adaptations | missing | designed | `3885c71d-2bc7-8052-8009-02713b58eec3` |
| R03 | Staff inventory · mobile | 10 · Responsive adaptations | missing | designed | `3885c71d-2bc7-8052-8009-02713d5e2bae` |
| R04 | Custody acceptance · mobile | 10 · Responsive adaptations | missing | designed | `3885c71d-2bc7-8052-8009-02713e2d4ef8` |
| R05 | Collection desk · tablet | 10 · Responsive adaptations | missing | designed | `3885c71d-2bc7-8052-8009-02713eef61e5` |
| R06 | Overview · tablet | 10 · Responsive adaptations | missing | designed | `3885c71d-2bc7-8052-8009-02714096a663` |
| C15 | This candidate has a conflict | 04 · Reports & claim states | missing | designed | `46d4ce92-2766-80d6-8009-02768d8ac162` |
| L15 | Payment outcome is pending | 05 · Delivery rewards & passports | missing | designed | `3885c71d-2bc7-8052-8009-02728911fd0f` |
| L16 | Refund or reversal status | 05 · Delivery rewards & passports | missing | designed | `3885c71d-2bc7-8052-8009-02728a388ef2` |
| L17 | Cancel this delivery request? | 05 · Delivery rewards & passports | missing | designed | `3885c71d-2bc7-8052-8009-02728b892e5e` |
| L18 | This tag is unavailable | 05 · Delivery rewards & passports | missing | designed | `3885c71d-2bc7-8052-8009-02728cc2161a` |
| L19 | Revoke or rotate a tag | 05 · Delivery rewards & passports | missing | designed | `3885c71d-2bc7-8052-8009-02728e10cd60` |
| L20 | Confirm voluntary reward terms | 05 · Delivery rewards & passports | missing | designed | `3885c71d-2bc7-8052-8009-02728f728920` |
| S19 | Pickup attempt denied | 06 · Staff decisions & exceptions | missing | designed | `46d4ce92-2766-80d6-8009-027667d4364f` |

## Flow inventory

Screen membership is not a sequential walk through every state. The guarded branches below determine the path. Role transitions in the prototype do not grant live authority.

| ID | Flow | Before | Current | Screen/state IDs |
|---|---|---|---|---|
| F01 | Access, identity & workspace | missing | designed | A01, A02, A03, A04, A05, A06, A10, E05, E06 |
| F02 | Lost report & revision | partial | designed | M01, M02, M03, M04, C01, C02, C03, C14, E01, E02, E03, E08 |
| F03 | Search & partial availability | partial | designed | M05, M06, C04, C05, D04, C15 |
| F04 | Private ownership & representative claim | partial | designed | M07, M08, C06, C07, C08, C09, C10, C12, E04 |
| F05 | Approval, collection & return | partial | designed | M09, M14, C13, E07, D05, D11, D12, S19 |
| F06 | Finder intake & staffed acceptance | partial | designed | M10, M13, D03, S06, S07 |
| F07 | Private relay & moderation | partial | designed | M11, C11, S17 |
| F08 | Notification consent & delivery | missing | partial | A07, O07, O08, O14 |
| F09 | Courier quote, consent & reconciliation | missing | partial | L01, L02, L03, L04, L05, S11, L17 |
| F10 | Owner receipt & transit changes | missing | designed | L05, L06, L07, S11, S12 |
| F11 | Voluntary reward & financial dispute | missing | partial | L08, L09, L10, S13, S14, L15, L16, L20 |
| F12 | Registration, tags & finder relay | partial | partial | M12, L11, L12, L13, L14, L18, L19 |
| F13 | Staff queue, evidence & risk | partial | designed | D01, D02, S01, S02, S03, S04, S05 |
| F14 | Custody, stale updates & incident holds | partial | designed | D06, S06, S07, S08 |
| F15 | Unclaimed property disposition | missing | partial | S09, S10 |
| F16 | Scoped investigation & crop access | partial | partial | D08, S15, S16 |
| F17 | Trusted network & dispute | partial | designed | D07, O09, O10, S18 |
| F18 | Audit, EPCIS & correction | missing | designed | O11, O12 |
| F19 | Organisation access & policy | partial | partial | D10, O01, O02, O03, O04 |
| F20 | Privacy, retention & deletion | missing | designed | A08, A09, O05, O06 |
| F21 | Connectors, sources & API clients | missing | partial | O07, O08, O13, O14 |
| F22 | Platform, restore & operation failures | missing | designed | P01, P02, P03, P09, P10 |
| F23 | Governed data, evaluation & rollout | missing | partial | P04, P05, P06, P07, P08 |
| F24 | Analytics, movement & optional prevention | partial | partial | D09, P11, P12 |
| F25 | Responsive access & critical workflows | missing | partial | R01, R02, R03, R04, R05, R06 |
| F26 | Shared recovery and access failures | missing | designed | E01, E02, E03, E04, E05, E06, E07, E08 |

## Actors, decisions and outcomes

### F01 · Access, identity & workspace

**Actors:** Member / identity provider.

**Outcome:** Authenticated workspace selected; authentication is not ownership.

- Confirmed identity → A05; current membership determines access.
- Denied workspace → E05; expired session → E06; account recovery → A04.

**Boundary:** Identity provider handles sign-in and recovery; membership separately gates workspace access.

### F02 · Lost report & revision

**Actors:** Claimant / finder.

**Outcome:** Report saved with original input and revision history.

- Valid submission → M04; missing detail → E03.
- Offline → C14; interrupted upload → E01; quarantine → E02; restored connection → E08.

**Boundary:** Original observations and revisions retained; offline draft is not a server submission.

### F03 · Search & partial availability

**Actors:** Claimant / match reviewer.

**Outcome:** A candidate is reviewed or safely excluded.

- Available candidates → M05/M06; no candidates → C04; partial routes → C05.
- Reliable identity contradiction → C15; unavailable evidence is not mismatch.

**Boundary:** Unavailable search routes are not empty; coverage and contradictions are independent.

### F04 · Private ownership & representative claim

**Actors:** Claimant / independent reviewer.

**Outcome:** Evidence assessed; entitlement and release remain separate.

- Private submission → C07; representative evidence → C12; more proof → C06.
- Inconclusive → C08; not approved → C09; withdrawal → C10; attempt limit → E04.

**Boundary:** Staff-attested evidence, attempt budgets and independent review; no disclosed answers.

### F05 · Approval, collection & return

**Actors:** Independent reviewer / collection desk.

**Outcome:** One authorised physical return is recorded.

- Independent approval and current policy permit M09; pending review cannot issue a pass.
- Expired pass → C13; active hold → E07; wrong/used/invalid pass → S19; actual recipient and item check → D11/D12.

**Boundary:** Approval, token, actual recipient, custody and owner return remain separate checks.

### F06 · Finder intake & staffed acceptance

**Actors:** Finder / intake custodian / receiver.

**Outcome:** Physical possession is explicitly accepted.

- Finder report → M10/M13; staff intake → D03.
- Only nominated receiver accepts → S06; stale generation → S07; report alone never transfers custody.

**Boundary:** Finder retains possession until an authorised receiving operator accepts physical custody.

### F07 · Private relay & moderation

**Actors:** Claim participants / moderator.

**Outcome:** Structured communication without contact disclosure.

- Only REQUEST_UPDATE, READY_TO_ARRANGE_PICKUP and THANK_YOU in current claim relay → M11/C11.
- Flag/block and scoped abuse review → S17; no arbitrary private answers sent outbound.

**Boundary:** Current runtime is phrase-only; block, flag and retention controls apply.

### F08 · Notification consent & delivery

**Actors:** Member / notification operator.

**Outcome:** Consented channel operation reconciled independently of custody.

- Explicit channel consent → A07; availability → O07.
- Unknown send/callback outcome → O08; signed replay-checked delivery cannot change custody.

**Boundary:** Consent and provider operation state are separate from custody.

**Remaining design:** Channel-specific template editing, localisation previews and delivery-failure interaction details remain partial.

### F09 · Courier quote, consent & reconciliation

**Actors:** Claimant / logistics staff / provider.

**Outcome:** Consented delivery request is authorised or reconciled.

- Private route consent → L02; quote review → L03; current staff authority required before dispatch.
- Unknown booking → L04; cancellation → L17; provider facts cannot release property.

**Boundary:** Provider-vault route consent; booking does not change custody; unknown sends reconcile.

**Remaining design:** Dedicated staff booking/dispatch forms, route/quote failure branches and cancellation completion details need further design.

### F10 · Owner receipt & transit changes

**Actors:** Claimant / independent logistics reviewer.

**Outcome:** Actual owner receipt recorded under current authority.

- Provider tracking → L05; actual receipt confirmation → L06/L07.
- Changed policy/hold in transit → S11/S12; delivery alone never confirms owner return.

**Boundary:** Delivery does not confirm receipt; policy/hold changes require current independent authority.

### F11 · Voluntary reward & financial dispute

**Actors:** Payer / beneficiary / independent financial reviewer.

**Outcome:** Voluntary funds reconciled without gating property recovery.

- Immutable terms → L20; unknown payment → L15; dispute → L10/S13.
- Ledger evidence → S14; refund/reversal → L16; no unconfirmed or fabricated recovered funds.

**Boundary:** Payment never gates recovery; immutable terms and independently reviewed reconciliation.

**Remaining design:** Payment consent, declined/resolved settlement variants and detailed independent dispute/reversal decisions need further design.

### F12 · Registration, tags & finder relay

**Actors:** Registration owner / authenticated finder.

**Outcome:** Limited private contact through a revocable capability.

- Private item → L11/L12; tag management → L13/L19; unavailable tag → L18.
- Finder phrase relay → L14; a tag scan never creates a claim or ownership authority.

**Boundary:** A tag scan neither establishes ownership nor creates release authority.

**Remaining design:** Tag issuance/printing/NFC-writing and complete rotation/revocation interaction details remain partial.

### F13 · Staff queue, evidence & risk

**Actors:** Assigned staff / restricted evidence reviewer.

**Outcome:** Versioned evidence and risk reviewed within a task.

- Queue → S01; competing claims → S02; risk/holds → S03.
- Scoped evidence access → S04; staff-attested safe challenge → S05; expected answer stays secret.

**Boundary:** Task-scoped private evidence, separate risk, competing claims and recorded reasons.

### F14 · Custody, stale updates & incident holds

**Actors:** Current custodian / nominated receiver.

**Outcome:** Current generation and append-oriented custody history retained.

- Transfer acceptance → S06; stale update → S07.
- Incident → S08 creates a hold; no stale or offline observation overwrites custody.

**Boundary:** Receiver acceptance, generation check, append-only incident and current hold.

### F15 · Unclaimed property disposition

**Actors:** Retention operator / authorised approver.

**Outcome:** A policy-permitted disposition is audited.

- Retention queue → S09; legal hold blocks terminal actions.
- Current eligibility, reason and recipient evidence are required for S10.

**Boundary:** Retention and legal hold checked before an audited policy-authorised terminal action.

**Remaining design:** Institution-specific disposition eligibility, recipient evidence and confirmation forms remain partial.

### F16 · Scoped investigation & crop access

**Actors:** Institution investigator / independent privacy reviewer.

**Outcome:** Only scoped approved observations/crops become visible.

- Exact purpose, camera, time, expiry and budget → S15.
- Masked crop review → S16; source scene → D08; no default facial recognition.

**Boundary:** Camera/time/purpose/expiry/budget grant, local footage and independent crop review.

**Remaining design:** Interactive source-region/mask authoring, grant revocation and purge controls remain partial.

### F17 · Trusted network & dispute

**Actors:** Local reviewer / tenant trust administrator.

**Outcome:** Attributed remote discovery with local custody preserved.

- Peer-local ranks and explicit availability → D07.
- Bilateral trust/key controls → O09/O10; stale or withdrawn offers → S18.

**Boundary:** Coarse attributed projection and peer-local ranks; physical custody stays local.

### F18 · Audit, EPCIS & correction

**Actors:** Audit/traceability operator.

**Outcome:** Native facts retained independently of EPCIS delivery.

- History → O11; mapping/source/correction → O12.
- 202 submission is not completed capture; corrections append rather than replace originals.

**Boundary:** Native custody fact is authoritative; asynchronous submission and correction retain originals.

### F19 · Organisation access & policy

**Actors:** Organisation administrator / independent policy reviewer.

**Outcome:** Least-privilege membership and versioned local policy.

- Members/scopes → O01/O02; sites/pickup policy → O03.
- Policy/hold change → O04 requires current approval checks; tenant scope remains explicit.

**Boundary:** Least privilege, independent reviewer, versioned local policies and site scope.

**Remaining design:** Detailed invitation, role-permission matrix and policy-change impact/confirmation interactions remain partial.

### F20 · Privacy, retention & deletion

**Actors:** Member / privacy officer / maintenance operator.

**Outcome:** Permitted erasure propagated with hold-aware minimal audit.

- Request → A08/A09; retention queue → O05; detailed scope → O06.
- Legal holds block incompatible deletion; derivatives and post-backup tombstones stay in scope.

**Boundary:** Class-specific retention, holds and propagated erasure; minimal audit facts retained.

### F21 · Connectors, sources & API clients

**Actors:** Tenant connector administrator / external provider.

**Outcome:** Approved origins, scopes and signed operations retained.

- Availability and callback operation → O07/O08.
- Permitted source terms/provenance → O13; scoped client/template configuration → O14; secrets are not printed.

**Boundary:** Approved origins/terms, secret references, signed callbacks and scoped clients.

**Remaining design:** Connector/API scope setup, localised template editing and detailed permitted-ingestion controls remain partial.

### F22 · Platform, restore & operation failures

**Actors:** System operator / tenant-authorised service.

**Outcome:** Operational health without blanket tenant evidence access.

- Tenant/node readiness → P01/P02; jobs → P03.
- Restore erasures must replay before serving; provider/quotas/audit → P09/P10; ambiguous sends reconcile.

**Boundary:** System metadata does not grant tenant evidence access; restore barriers precede serving.

### F23 · Governed data, evaluation & rollout

**Actors:** Data curator / independent adjudicator / model approver.

**Outcome:** Versioned evidence governs evaluation and activation.

- Input/model provenance → P04; independent labels/frozen splits → P05.
- Evaluation/calibration → P06; incomplete quality blocks P07; only valid explicit rollback → P08.

**Boundary:** Versioned input/model provenance, independent labels/approval, scoped calibration and valid rollback.

**Remaining design:** Detailed dataset/adjudication editing, evaluation charts, approval forms and rollback confirmation interactions remain partial.

### F24 · Analytics, movement & optional prevention

**Actors:** Tenant analyst / governed prediction operator.

**Outcome:** Aggregates and uncertain inference stay distinguishable from custody.

- Tenant operational summary → D09; observed/inferred pathways → P11.
- Insufficient support stays unavailable; optional hardware/consent → P12, disabled until adopted.

**Boundary:** Tenant aggregates and uncertainty; inferred transitions do not establish possession.

**Remaining design:** Analytics report families, denominators/suppression controls and prevention-provider consent details remain partial.

### F25 · Responsive access & critical workflows

**Actors:** Claimant / staff across viewports.

**Outcome:** The same privacy and authority boundaries survive layout changes.

- Desktop reporting/verification → R01/R02; mobile staff inventory/acceptance → R03/R04.
- Tablet collection/overview → R05/R06; full breakpoint and accessibility acceptance remain open.

**Boundary:** Representative desktop, tablet and mobile adaptations; complete breakpoint testing remains separate.

**Remaining design:** Six representative adaptations exist; full breakpoint, keyboard/screen-reader and localisation design remain partial.

### F26 · Shared recovery and access failures

**Actors:** Member / staff in a recoverable failure.

**Outcome:** Draft, availability and critical-operation outcomes stay distinct.

- Upload/validation/quarantine → E01/E02/E03; throttling → E04.
- Access/session → E05/E06; hold → E07; reconnection → E08 without replaying critical actions.

**Boundary:** Explicit validation, quarantine, throttling, expiry, denial and sync outcomes.

## Remaining detail

All 130 named views exist. These are the specifically partial flow areas; they are not hidden by the screen count.

- F08: Channel-specific template editing, localisation previews and delivery-failure interaction details remain partial.
- F09: Dedicated staff booking/dispatch forms, route/quote failure branches and cancellation completion details need further design.
- F11: Payment consent, declined/resolved settlement variants and detailed independent dispute/reversal decisions need further design.
- F12: Tag issuance/printing/NFC-writing and complete rotation/revocation interaction details remain partial.
- F15: Institution-specific disposition eligibility, recipient evidence and confirmation forms remain partial.
- F16: Interactive source-region/mask authoring, grant revocation and purge controls remain partial.
- F19: Detailed invitation, role-permission matrix and policy-change impact/confirmation interactions remain partial.
- F21: Connector/API scope setup, localised template editing and detailed permitted-ingestion controls remain partial.
- F23: Detailed dataset/adjudication editing, evaluation charts, approval forms and rollback confirmation interactions remain partial.
- F24: Analytics report families, denominators/suppression controls and prevention-provider consent details remain partial.
- F25: Six representative adaptations exist; full breakpoint, keyboard/screen-reader and localisation design remain partial.
- Evaluate complete keyboard/screen-reader, localisation, role/version/hold, offline and concurrent-operation behavior during implementation and usability testing.

## Specification traceability

All 100 specification headings were enumerated at their actual source line. A UI mapping is traceability, not implementation or complete requirement acceptance. Engineering/governance requirements are explicitly distinguished.

| Section | Requirement | Source line | Flow families / disposition |
|---|---|---|---|
| 1 | NON-NEGOTIABLE DEVELOPMENT PRINCIPLES | 15 | Engineering/governance requirement; no standalone application screen inferred |
| 2 | REQUIRED SOURCE AUDIT | 63 | Engineering/governance requirement; no standalone application screen inferred |
| 3 | PROJECT IDENTITY | 160 | F01, F02, F06, F19, F22 |
| 4 | CORE ARCHITECTURAL PRINCIPLE | 212 | F03, F04, F05 |
| 5 | ITEM FINGERPRINT | 242 | F02, F03, F23 |
| 6 | MULTIMODAL INTAKE | 330 | F02, F06 |
| 7 | WHATSAPP-FIRST EXPERIENCE | 380 | F07, F08 |
| 8 | AI PERCEPTION LAYER | 417 | F02, F23 |
| 9 | SEARCH AND MATCHING PIPELINE | 464 | F03 |
| 10 | MATCH EVIDENCE MODEL | 517 | F03 |
| 11 | MISSING EVIDENCE RULE | 567 | F03, F04 |
| 12 | HARD CONFLICT ENGINE | 593 | F03 |
| 13 | OWNERSHIP VERIFICATION ENGINE | 636 | F04 |
| 14 | AI-GENERATED PRIVATE OWNERSHIP CHALLENGES | 666 | F04, F13 |
| 15 | TRUST AND FRAUD ENGINE | 703 | F13 |
| 16 | PRIVACY ARCHITECTURE | 736 | F07, F20 |
| 17 | DATABASE ARCHITECTURE | 795 | Engineering/governance requirement; no standalone application screen inferred |
| 18 | VECTOR SEARCH ABSTRACTION | 863 | F22, F23 |
| 19 | EVENT-DRIVEN ARCHITECTURE | 886 | F18, F22 |
| 20 | RECOVERY EVENT MODEL | 938 | F05, F14, F18 |
| 21 | RECOVERY KNOWLEDGE GRAPH | 967 | F24 |
| 22 | CCTV / EDGE VISION SUBSYSTEM | 1016 | F16 |
| 23 | OBJECT TRACKING | 1072 | F16 |
| 24 | LOST-OBJECT MOVEMENT / RECOVERY PREDICTION | 1092 | F24 |
| 25 | FEDERATION | 1149 | F17 |
| 26 | FEDERATED SEARCH | 1196 | F17 |
| 27 | FEDERATION PRIVACY | 1231 | F17, F20 |
| 28 | MULTI-TENANCY | 1264 | F01, F19, F22 |
| 29 | SELF-HOSTED + SAAS DEPLOYMENT | 1289 | F22 |
| 30 | RECOVERY / CUSTODY ENGINE | 1317 | F06, F14, F15 |
| 31 | SECURE HANDOVER | 1353 | F05 |
| 32 | COURIER ORCHESTRATION | 1372 | F09, F10 |
| 33 | REWARD SYSTEM | 1399 | F11 |
| 34 | QR / NFC ITEM REGISTRATION | 1434 | F12 |
| 35 | DIGITAL ITEM PASSPORT | 1454 | F12 |
| 36 | SECURE ANONYMOUS MESSAGING | 1483 | F07 |
| 37 | NOTIFICATIONS | 1501 | F08 |
| 38 | LOCATION INTELLIGENCE | 1530 | F02, F03, F24 |
| 39 | TEMPORAL INTELLIGENCE | 1573 | F02, F03, F24 |
| 40 | SEARCH SCALE | 1594 | F03, F22 |
| 41 | MODEL PROVIDER ARCHITECTURE | 1616 | F22, F23 |
| 42 | MODEL GOVERNANCE | 1650 | F23 |
| 43 | ML FEEDBACK LOOP | 1670 | F23 |
| 44 | MLFLOW | 1720 | F23 |
| 45 | OBSERVABILITY | 1736 | F22 |
| 46 | SECURITY | 1774 | F01, F19, F20, F26 |
| 47 | POLICY ENGINE | 1814 | F19 |
| 48 | AUTHENTICATION AND IDENTITY | 1829 | F01, F19 |
| 49 | API DESIGN | 1846 | F21 |
| 50 | FRONTEND | 1876 | F01, F02, F06, F19, F22, F25 |
| 51 | CORE BACKEND | 1913 | Engineering/governance requirement; no standalone application screen inferred |
| 52 | SERVICE BOUNDARIES | 1948 | Engineering/governance requirement; no standalone application screen inferred |
| 53 | DOMAIN MODULES | 1969 | Engineering/governance requirement; no standalone application screen inferred |
| 54 | EVENTUAL CONSISTENCY | 2005 | F09, F14, F18, F26 |
| 55 | AUDITABILITY | 2033 | F18 |
| 56 | TESTING | 2052 | Engineering/governance requirement; no standalone application screen inferred |
| 57 | BENCHMARKS | 2079 | F23 |
| 58 | DATASETS | 2101 | F23 |
| 59 | ANALYTICS | 2119 | F24 |
| 60 | PREDICTIVE PREVENTION | 2142 | F24 |
| 61 | EDGE / IoT EXTENSIBILITY | 2158 | F24 |
| 62 | DISPOSAL / UNCLAIMED PROPERTY | 2173 | F15 |
| 63 | INTERNATIONALIZATION | 2194 | F01, F25 |
| 64 | ACCESSIBILITY AND LOW-BANDWIDTH OPERATION | 2214 | F02, F25, F26 |
| 65 | OFFLINE / EDGE OPERATION | 2234 | F14, F22, F26 |
| 66 | ARCHITECTURE DOCUMENTATION | 2250 | Engineering/governance requirement; no standalone application screen inferred |
| 67 | ADRs | 2286 | Engineering/governance requirement; no standalone application screen inferred |
| 68 | ROOT REPOSITORY DOCUMENTATION | 2337 | Engineering/governance requirement; no standalone application screen inferred |
| 69 | STATUS.md | 2364 | Engineering/governance requirement; no standalone application screen inferred |
| 70 | PRODUCTION CONFIGURATION | 2383 | F22 |
| 71 | DATABASE MIGRATIONS | 2400 | Engineering/governance requirement; no standalone application screen inferred |
| 72 | BACKUP AND RESTORE | 2415 | F22 |
| 73 | HIGH AVAILABILITY | 2429 | F22 |
| 74 | RATE LIMITING AND ABUSE PREVENTION | 2446 | F01, F07, F26 |
| 75 | DATA RETENTION | 2465 | F20 |
| 76 | DATA DELETION | 2485 | F20, F22 |
| 77 | SEARCH PRIVACY | 2502 | F03, F04 |
| 78 | PUBLIC API | 2514 | F21 |
| 79 | CONNECTOR FRAMEWORK | 2533 | F21 |
| 80 | SOCIAL / EXTERNAL SOURCE INGESTION | 2558 | F21 |
| 81 | HUMAN REVIEW OPERATIONS | 2576 | F13, F14, F15, F16, F17 |
| 82 | SYSTEM ADMINISTRATION | 2593 | F19, F22, F23 |
| 83 | FEATURE FLAGS | 2613 | F22, F24 |
| 84 | SAFETY | 2627 | F16, F20 |
| 85 | SYSTEM PERFORMANCE | 2641 | F22, F23 |
| 86 | FAILURE HANDLING | 2660 | F26 |
| 87 | IDEMPOTENCY | 2680 | F05, F09, F11, F14, F22 |
| 88 | REPRODUCIBILITY | 2697 | Engineering/governance requirement; no standalone application screen inferred |
| 89 | LOCAL DEVELOPMENT | 2718 | Engineering/governance requirement; no standalone application screen inferred |
| 90 | CI/CD | 2728 | Engineering/governance requirement; no standalone application screen inferred |
| 91 | DEPENDENCY GOVERNANCE | 2750 | Engineering/governance requirement; no standalone application screen inferred |
| 92 | LICENSE STRATEGY | 2764 | Engineering/governance requirement; no standalone application screen inferred |
| 93 | NO README-DRIVEN CLAIMS | 2780 | Engineering/governance requirement; no standalone application screen inferred |
| 94 | IMPLEMENTATION ORDER | 2804 | Engineering/governance requirement; no standalone application screen inferred |
| 95 | DO NOT OVERENGINEER THE WRONG PARTS | 2851 | Engineering/governance requirement; no standalone application screen inferred |
| 96 | SUCCESS CRITERIA | 2875 | F02, F03, F04, F05, F09, F11, F16, F17, F23, F24 |
| 97 | REQUIRED FINAL DOCUMENTATION BEFORE DECLARING PRODUCTION READY | 2953 | Engineering/governance requirement; no standalone application screen inferred |
| 98 | ARCHITECTURAL INVARIANTS | 2992 | F03, F04, F05, F07, F10, F11, F14, F16, F17, F20, F23 |
| 99 | WORKING STYLE | 3019 | Engineering/governance requirement; no standalone application screen inferred |
| 100 | FIRST ACTIONS | 3047 | Engineering/governance requirement; no standalone application screen inferred |

## Verification and limitations

- Live native validation returned no errors. Text/table containment checks across all application and map pages found no overflow. Representative PNG exports were visually inspected; all views were not individually pixel-audited.
- The saved-file read contains 130 application concepts, one foundation board and two map boards. The catalogue indexes every one of the 130 state IDs. The receipt records per-page dimensions, board IDs and interaction counts.
- Primary/section navigation is connected; new mobile back controls use history. Cross-page links open viewer tabs. The catalogue is also a review guide for alternate state and role handoffs.
- Original M11 now uses REQUEST_UPDATE, READY_TO_ARRANGE_PICKUP and THANK_YOU display phrases. Expected verification answers, contacts and token values are not exposed.
- All data and amounts are synthetic. Forms, QR graphics, payments, booking and release actions remain nonfunctional concepts. Provider availability and production acceptance are separate.
- Six responsive adaptations exist. Remaining flow-detail, localisation, usability, keyboard/screen-reader and role/version/hold combinations remain explicitly open.
- A named saveVersion call completed; a later optional version-list read was unavailable after the tab suspended. No snapshot-list verification is claimed.
- Concurrent repository implementation changes outside these design documents were left untouched.

[STATUS](../../STATUS.md), [baseline](../architecture/baseline.md), the specification and governing ADRs continue to define runtime and production acceptance. JSON section fragments mean specification section numbers, not HTML anchors.
