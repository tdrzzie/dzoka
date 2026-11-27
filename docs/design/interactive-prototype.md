# Interactive prototype

Created in Penpot on 2026-11-27.

[Open prototype launcher](https://penpot.nyasham.dev/?screen=viewer&file-id=456d79de-921a-81c4-8009-02637f9b3b01&page-id=01c1c9a5-459f-804b-8009-028a30190730&frame-id=01c1c9a5-459f-804b-8009-028a54c3c6ba&section=interactions&zoom=fit) · [Start recovery](https://penpot.nyasham.dev/?screen=viewer&file-id=456d79de-921a-81c4-8009-02637f9b3b01&page-id=01c1c9a5-459f-804b-8009-0288142c2967&frame-id=01c1c9a5-459f-804b-8009-0288d6ec44cf&section=interactions&zoom=fit) · [Native interaction receipt](interactive-prototype.json)

The launcher provides 26 flow entry points. The continuous recovery page contains 27 preset screen states and seven native overlays. The canonical 215-view library is preserved; prototype copies and the synthetic evidence variant are recorded separately.

## How to try it

1. Select **Start the recovery journey**.
2. Report a lost item, continue to place/time and submit. The native confirmation offers a complete sample, a validation error, or **Keep editing**.
3. Open a candidate and start a private ownership claim. Select the private-response field, choose **Use private sample evidence**, then submit the completed state.
4. Claim submission ends in **under review**. It does not generate a collection pass.
5. Select **Scenarios** to inspect a separately labelled approved-review example, identity conflict, expired pass, unknown booking, authorised receipt or completed collection.
6. Use the launcher cards to open focused staff, courier, rewards, tags, administration, investigation, model and analytics sessions. Click result rows to inspect the associated branches.

Forms use preset sample states. Arbitrary typing, real uploads, device writes and provider calls are outside this Penpot prototype. Scenario controls are reviewer tools, not product authorization.

## Interaction additions

- Same-page recovery navigation, back routes, sample evidence entry, confirmation, cancellation, message feedback and alternate outcomes.
- Seven recovery overlays: scenarios, report submission, sample evidence, claim confirmation, owner receipt, queued message and conversation safety.
- Thirty-seven new result-row links across notifications, courier, rewards, access requests, connectors, model governance and analytics.
- Native confirmation overlays with explicit close controls for relevant focused flows; simulated NFC and investigation result choosers.
- A compact desktop launcher with 26 clickable flow cards.

## Verification

The actual recovery graph contains 104 navigation/overlay edges. All 27 preset states are reachable from home; there are zero invalid local destinations. All 26 launcher targets resolve to existing native boards. Native file validation reports zero errors.

Live Chrome viewer checks passed for report navigation, confirmation/cancel with preserved form, successful report submission, candidate/private evidence/claim submission through pending review, and scenario switching to an expired pass. Representative exports and the live viewer were visually inspected.

Presentation checks exposed imported text and canvas-offset problems. Imported SVG text was replaced by native Penpot text, native button labels restored, and illustration coordinates corrected relative to their original boards. The source library remains intact.

Focused branches were checked by their native interaction records and geometry; not every focused-flow path was manually clicked. See the [native receipt](interactive-prototype.json) for actual board IDs, destinations, scope and save label.

Prototype status does not establish runtime authorization, provider acceptance, institutional policy, accessibility conformance, performance or production readiness.

