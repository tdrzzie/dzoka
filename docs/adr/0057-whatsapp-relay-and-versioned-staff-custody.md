# 0057. Signed WhatsApp channel and versioned staff custody

- Date: 2026-11-26
- Status: Implemented for local verification

Bind the signed WhatsApp webhook to a configured tenant, WhatsApp business account and phone-number ID. Verify the HMAC over the original body before parsing. Bound the payload, reject stale events, and persist keyed event identities. Application membership and explicit verified contact consent govern who can create an intake report or join a conversation. Store the destination encrypted and use a separate keyed lookup value. Never include contact addresses, webhook bodies or secrets in public responses or audit events.

The anonymous relay accepts a small catalogue of phrases for an active claim. Only its participants can submit or read participant messages. A moderator can inspect the structured thread, including an abuse flag. Retained messages expire and are deleted in bounded batches. Automatic notices for relay messages and confirmed returns are recorded transactionally and go through the same consent and provider-operation checks as staff notices.

Delivery operations have stable IDs, a bounded retry for explicit throttling, and an unknown state for an ambiguous submission. A signed, recipient-bound provider callback can reconcile an unknown operation. Out-of-order callbacks cannot downgrade a stronger delivery confirmation. Provider delivery updates only the notification projection.

Use the existing recovery tenant and report locks for physical custody mutations. The current custodian can record storage or an incident, or nominate a separate receiving operator. Only that recipient can accept the transfer. Match the expected generation before changing custody. A stale observation is retained for staff review without changing the authoritative location. Changing custody increments the same generation checked by release authorization. Incidents place a hold.

The provider port uses the approved WhatsApp Cloud API message template endpoint. No new runtime dependency is needed. Target operator verification requires an authorized WhatsApp sandbox, approved templates and real desk roles. [WhatsApp webhooks](https://developers.facebook.com/documentation/business-messaging/whatsapp/webhooks/overview), [WhatsApp message API](https://developers.facebook.com/documentation/business-messaging/whatsapp/reference/whatsapp-business-phone-number/message-api).

V19 extends the channel with verified media downloads, durable matching watches, session replies and pickup messages. A media event carries an encrypted work item. The dispatcher accepts only an exact provider origin, binds retrieval to the configured phone-number ID, and checks the signed SHA-256 and size before private quarantine. The isolated raster worker processes photos. Audio and video remain quarantined until there is an approved isolation and codec path.

Search, candidate selection and claims execute under the verified member's permissions. The channel presents evidence categories and scoped references. Match alerts use generic templates, and the encrypted response queue sends session replies inside the inbound session. Raw challenge answers follow the existing private assessment and are never queued for outbound delivery. Pickup messages record requests and acknowledgments, while the existing authorized staff grant and redemption still govern physical return.
