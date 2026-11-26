# Operate federation and EPCIS

## Configure and trust a node

Set `DZOKA_FEDERATION_TENANT` to the tenant served by this deployment. Set `DZOKA_FEDERATION_NODE` and `DZOKA_FEDERATION_KEY_ID` to lowercase identifiers. Inject the Ed25519 PKCS#8 private key as unpadded base64url via `DZOKA_FEDERATION_PRIVATE_KEY` from the deployment secret manager. Keep that key local. Give `urn:dzoka:federation` / `node` active `federation:service` membership in the selected tenant. The peer endpoint is `POST /api/v1/federation/peer`. Configure HTTPS and enforce body and connection deadlines at the deployment ingress.

Set `DZOKA_FEDERATION_ORIGINS` to a comma-separated allowlist such as `university=https://recovery.example.org`. Each origin is a TLS-validated, fixed endpoint. Keep the loopback flag unset outside tests. The network policy should permit only the approved peer origins and other required dependencies.

Tenant operator paths are under `/api/v1/tenants/{tenant}/federation`.

1. With `federation:admin`, use `PUT /peers/{peer}` to select `active`, `inbound`, `outbound`, `minVersion` and `maxVersion`. Set both version bounds to `1` for this implementation. Append a verified X.509 SubjectPublicKeyInfo Ed25519 public key via `PUT /peers/{peer}/keys/{key}` with `publicKey` encoded as unpadded base64url. Verify fingerprints through an authenticated out-of-band channel.
2. Use `POST /peers/{peer}/capabilities` with `federation:search` to check the bilateral protocol. When rotating a key, grant the new public key on each peer, switch the sender's key and configuration, verify signed exchanges, and revoke the old key via `DELETE /peers/{peer}/keys/{key}`. Disable an affected relationship before restoring trust after suspected compromise.
3. An operator with `federation:admin` and local report-read permission can publish an available found report via `PUT /offers/{report}` with the current `revision`, approved `category`, ISO country code in `country`, calendar `month` in `YYYY-MM` format, and `active: true`. Revisions, holds, expiration and deletion invalidate discovery. Set `active: false` to withdraw. Category, country and month are chosen for authorized coarse discovery. The protocol has no free-text or precise locations.
4. Send `POST /search` with `peers` containing one to four peer names and a `query` containing `category`, `country`, `month` and `limit` of at most ten per peer. Use each hit's origin and rank for discovery. The response reports partial availability and an uncalibrated evidence policy. Recontact and any verification or release use the respective institution's authorized process.
5. Use `GET /peers` for health and circuit state. Each peer has separate 30-request-per-minute inbound and outbound windows. Three failures open a one-minute circuit. Schedule authenticated `POST /prune` to remove bounded batches of audit entries older than 30 days. Signatures and replay controls apply to capability queries as well as searches.

The signed header and decoded payload hashing rules are specified in the protocol ADR. Exchange only directly with registered peers. [Wire protocol and trust](../adr/0058-federation-wire-and-epcis-projection.md).

## Export authorized custody observations

Set `DZOKA_EPCIS_ORIGIN` to the approved TLS repository origin and provide `DZOKA_EPCIS_TOKEN` through the deployment secret manager. The adapter uses `/capture` and `/capture/{captureID}` at that origin. Local native custody and projection remain available when the external repository is absent.

Paths are under `/api/v1/tenants/{tenant}/traceability`.

1. A `trace:manage` operator sets consent for an intake via `PUT /intakes/{intake}/authorization` with `enabled`. Revocation cancels queued captures. Retain evidence for captures already submitted.
2. A scoped projector with `trace:manage` and appropriate custody/read permissions calls `POST /project-next` to queue one supported native event recorded after authorization. A deliberate backfill can call `PUT /intakes/{intake}/native/{event}` with an explicit `kind` and `mapping`. Supported kinds are `STORED`, `TRANSFER_ACCEPTED` and `RETURNED`. Select mapping 2 for new captures. Repeating the same native event and mapping returns the same projection.
3. A `trace:dispatch` worker calls `POST /dispatch`. Each call claims one submission or job poll. `SUBMITTED` means the repository issued an asynchronous job receipt. `COMPLETE` means that job later reported success. A capped retry handles explicit throttling. Ambiguous submissions become `UNKNOWN`. An operator with both `trace:manage` and `trace:dispatch` can provide a separately verified `receipt` and `evidenceSha` through `PUT /events/{id}/receipt` to resume polling without repeating the capture.
4. To correct a completed mapping 1 projection, project the same native event with `mapping: 2` and its projection ID as `predecessor`. The new capture contains the prior event with an EPCIS error declaration plus the replacement event. Both retain the original native event time and opaque object identifier.
5. With `trace:read`, use `GET /intakes/{intake}/events` or `GET /events/{id}` for the local projections and capture state. The export profile excludes native person, site, report and evidence identifiers.

This adapter covers a subset of the EPCIS 2.0.0 capture and job interfaces. Institutional repository interoperability, deployment key custody and policy approval require their own evidence. [GS1 EPCIS artefacts](https://ref.gs1.org/standards/epcis/2.0.0/artefacts).
