# Native recovery events and EPCIS compatibility

Accepted design, 2026-10-26. Implementation is pending. This view follows the [current baseline](baseline.md) and ADRs [0017](../adr/0017-epcis-adapter.md). The canonical audits linked by those ADRs provide pinned source evidence and scope limits. Runtime/artifact choices must pass [dependency adoption](dependency-adoption.md).

## Native authority

RecoveryEvent records tenant, local event/item references, event type, observed/recorded times and uncertainty, location, actor/custodian, previous/new custody, source/evidence, sequence and correction reference. Physical receipt, storage, transfer, dispatch, delivery, owner return, archive and disposal are distinct facts. Matching/verification state is not silently converted into physical custody.

An EPCIS adapter consumes permitted events from an outbox/projection stream. A mapping contract must select the supported EPCIS edition, event types, vocabulary, identifier/location transforms, capture/query operations, privacy rules and correction semantics before coding. Unsupported mappings fail explicitly and retain the native fact. No blanket GS1/EPCIS conformance claim is made.

## Reliability and disclosure

Capture identity includes source event ID and mapping version; retries preserve identity and external receipt. Mapping revisions append corrected projections under explicit policy instead of inventing a new native transfer. External repository outage cannot block recording local custody. Export only authorized institution/event scope, minimize locations/identifiers and record the recipient/purpose.

GS1 concepts inform event dimensions and traceability responsibilities; an identifier or recorded event does not prove legal ownership. OpenEPCIS is a reference/adapter candidate, not a replacement for the domain store or permission engine.

## Acceptance

A21, A24, A27 and A28: golden mappings, duplicate capture, correction replay, unknown event type, authorized query filtering and external outage.

Acceptance IDs refer to [the implementation acceptance ledger](acceptance.md); none is claimed executed by this document.
