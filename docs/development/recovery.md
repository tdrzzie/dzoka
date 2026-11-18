# Private claims and staffed pickup

Configure `DZOKA_RECOVERY_HMAC_KEY` from the target secret manager as a stable, randomly generated 256-bit key encoded as 64 lowercase hexadecimal characters. Recovery operations involving answers and pickup tokens fail closed when the key is unavailable. Key replacement requires withdrawing old facts, cancelling unused authorizations, and enrolling fresh observations. Keep the key, source evidence and confirmation records in the institution's restricted custody.

Provision tenant-scoped permissions through verified memberships.

| Role permission | Purpose |
|---|---|
| `recovery:manage` | Enroll a found report the custodian can read, set policy versions and holds, revoke approved claims |
| `recovery:verify` with scoped report read | Check observation provenance, register facts and withdraw facts that become disclosed |
| `claims:submit` | Submit a claim by opaque intake reference, view or withdraw one's pending claim, request and answer a private challenge |
| `recovery:review` | View the claimant's protected account reference, record independent reviews and issue an eligible pickup token |
| `recovery:handover` | Present the grant at its designated desk, verify the recipient and confirm return |

Paths below are relative to `/api/v1/tenants/{tenant}/recovery`. Use random UUIDs for the path resources. Provide explicit boolean choices.

1. Use `PUT /policies/{version}` with `dualControl` and `allowRepresentatives`. Start at version 1 and publish subsequent versions in order. A new version invalidates existing verification and unused releases. Keep dual control enabled for sensitive or ambiguous cases.
2. Use `PUT /intakes/{intake}` with an active and authorized `foundReport` and a `site` reference. Share only the opaque intake reference with the claimant. The actor is the recorded custodian.
3. Use `PUT /intakes/{intake}/facts/{fact}` with `kind`, `sourceSha`, `answer` and `privateConfirmed`. Allowed kinds are `HIDDEN_MARK`, `PRIVATE_ACCESSORY` and `PRIVATE_CONTENT`. Inspect the actual local evidence and public disclosures before confirming privacy. Store only a hash reference to the provenance. Use `PUT .../facts/{fact}/withdrawal` with `DISCLOSED` or `WITHDRAWN` if the fact becomes unsafe.
4. Use `PUT /intakes/{intake}/claims/{claim}` with `capacity` of `OWNER` or `REPRESENTATIVE`. The claimant uses `PUT .../sessions/{session}` and `PUT .../sessions/{session}/answer` with the submitted `answer`. Each answer resolves one session. Sessions expire after ten minutes. An inconclusive result leaves the claim pending. Issuance and failure budgets cover both the actor and the item over 24 hours.
5. Use `GET .../claims/{claim}/review-context` to associate the opaque claimant reference with independently reviewed private records. The context shows recent assessment failures, competing claims and current holds. Use `PUT .../reviews/{review}` with `session`, `decision`, `reason`, `evidenceSha` and optional `entitlementSha`. Approval requires `INDEPENDENT_EVIDENCE`, a live passed session and evidence reviewed outside the challenge itself. Representatives require an entitlement reference bound to the claimant. Dual control requires two separate reviewers and separate corroboration on the same entitlement. Reject or resolve competing claims through independent review. A manager can place an item hold or revoke the existing entitlement.
6. Use `PUT .../claims/{claim}/grants/{grant}` with the assigned desk's `audienceRef`. Deliver the returned token through the institution's private channel. The token expires within five minutes or at the end of the verification session, whichever comes first. Keep it out of URLs, logs and shared messages.
7. At pickup, call `POST /intakes/{intake}/grants/{grant}/pickup-context` with the presented `token`. Only the authorized audience can see the eligible site and opaque recipient reference. Confirm the recipient's verified account and the site through the desk's institution procedure. Use `PUT /intakes/{intake}/returns/{request}` with `grant`, `token`, the confirmed `recipientRef`, and `confirmationSha`. Repeat the same request UUID and presentation after a transport interruption. One return and one outbox event may commit for the item.

The release path rechecks active intake and report holds, expiry, current report revision, claim competition, eligible evidence, and current policy. Lifting a hold leaves prior verification stale. To recover from a stale entitlement, revoke it, refresh the facts under the current report as needed, then obtain a fresh claim, challenge and review. No list endpoint exposes item owners or private evidence.
