# Source audit: dpp-did

Status: COMPLETE

Scope: architecture-relevant static review of the listed source ranges, license evidence, deployment entry points and tests/evaluation where present. No upstream tests were executed and no upstream implementation was copied into Dzoka. Unreviewed subsystems and integration acceptance work are called out explicitly.

## Repository identity

https://github.com/Jorge-Lopes/dpp-did

## Audited branch

main. The exact commit below is authoritative; branch HEAD may move.

## Exact commit SHA

44533c27967d406f4eeb5f09c4257b506a0264d8

## Audit date

2026-10-26

## License

UNRESOLVED. No root project LICENSE/COPYING in the complete pinned tree; missing licensing is a completed audit finding, not permission to copy.

## Files inspected

- [wallet/src/utils/vc.js](https://github.com/Jorge-Lopes/dpp-did/blob/44533c27967d406f4eeb5f09c4257b506a0264d8/wallet/src/utils/vc.js), lines 1-5; role: implementation.
- [wallet/src/utils/did.js](https://github.com/Jorge-Lopes/dpp-did/blob/44533c27967d406f4eeb5f09c4257b506a0264d8/wallet/src/utils/did.js), lines 1-42; role: implementation.
- [wallet/src/utils/storage.js](https://github.com/Jorge-Lopes/dpp-did/blob/44533c27967d406f4eeb5f09c4257b506a0264d8/wallet/src/utils/storage.js), lines 1-34; role: implementation.
- [wallet/test/test.js](https://github.com/Jorge-Lopes/dpp-did/blob/44533c27967d406f4eeb5f09c4257b506a0264d8/wallet/test/test.js), lines 1-68; role: tests.
- [wallet/src/server.js](https://github.com/Jorge-Lopes/dpp-did/blob/44533c27967d406f4eeb5f09c4257b506a0264d8/wallet/src/server.js), lines 1-65; role: implementation.
- [docker-compose.yaml](https://github.com/Jorge-Lopes/dpp-did/blob/44533c27967d406f4eeb5f09c4257b506a0264d8/docker-compose.yaml), lines 1-32; role: deployment.

## Runtime architecture

Node/Express business/client wallet demonstration uses Digital Bazaar DID/VC libraries and local document-serving services.

## Domain model

Wallet DIDs, product DIDs, credential issuance and service endpoints referencing credential documents.

## Storage model

storageManagement writes JSON to concatenated filesystem paths. Credential paths derive productReference from productEndpoint.slice(-1), which can collide for multi-digit identifiers.

## AI and matching implementation

No perception/matching inference. vc.js delegates issuance to the VC library; did.js constructs/resolves did:key and did:web documents.

## Security model

wallet server globally disables TLS certificate verification and initializes wallets from a constant seed. isBusiness uses !ROLE === Business, an incorrect precedence/type comparison, and response helper returns do not stop callers.

## API design

Express /init and /product endpoints operate a process-global wallet. There is no demonstrated caller authentication/tenant authorization in the inspected server boundary.

## Event model

Filesystem document updates/credential references are not immutable signed custody transitions or a verified recovery ledger.

## Deployment model

Compose launches two wallets and a document server sharing a local data volume. This is a demonstration topology, not secure institutional key management.

## Test coverage

Inspected happy-path wallet/product/credential tests use a deterministic synthetic seed and stubbed documents. They are not authorization, TLS or adversarial DID verification tests; none executed.

## Strengths

Small examples illustrate optional product identifiers, credential references and delegation to established DID/VC libraries.

## Weaknesses

No root project license, constant key seed, incorrect role guard, disabled TLS verification and fragile filesystem identity mapping.

## Security concerns

Never import the committed development certificate/key artifacts, accept arbitrary resolver URLs or expose shared wallet state as ownership proof. Keys must be generated and protected per node/tenant policy.

## Scalability concerns

Process-global state/local JSON writes lack demonstrated concurrency/tenant safety or transactional registry updates.

## Licensing consequences

No direct project-code reuse permitted by this audit. Study concepts and independently implement Dzoka contracts; seek explicit permission before any future copying.

## Reusable components

Reference optional digital passport/DID concepts independently; reject the demonstration server as a security or identity foundation.

## Do not reuse

No unlicensed source copying. Exclude constant seeds, global TLS bypass, broken role helpers, unvalidated filesystem paths and DID possession treated as property ownership.

## Architecture lessons

Persistent item identifiers are optional interoperability handles, not a universal ownership or custody trust anchor.

## Disposition

REJECT. Applies to this declared integration scope. It is not a claim of production readiness, blanket source reuse approval or completion of later integration acceptance tests.

## ADRs triggered

Proposed investigations: Dzoka-native recovery events with an explicit EPCIS adapter; optional item identifiers; no default blockchain. This audit supplies evidence for later technical ADRs; it does not finalize the architecture.

## Unresolved questions

External DID/VC library/weight-free dependency terms, resolver SSRF controls, revocation/rotation, path containment and credential-verification semantics require a separate provider design.
