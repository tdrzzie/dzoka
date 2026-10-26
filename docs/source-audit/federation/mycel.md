# Source audit: mycel

Status: COMPLETE

Scope: architecture-relevant static review of the listed source ranges, license evidence, deployment entry points and tests/evaluation where present. No upstream tests were executed and no upstream implementation was copied into Dzoka. Unreviewed subsystems and integration acceptance work are called out explicitly.

## Repository identity

https://github.com/splch/mycel

## Audited branch

main. The exact commit below is authoritative; branch HEAD may move.

## Exact commit SHA

07a671d03c4fbb68f459d24af262a3dec563c913

## Audit date

2026-10-26

## License

AGPL-3.0-only. Root LICENSE text inspected at the pinned commit: AGPL-3.0-only. Dependency, checkpoint and dataset licenses remain separate.

## Files inspected

- [src/net/proto.rs](https://github.com/splch/mycel/blob/07a671d03c4fbb68f459d24af262a3dec563c913/src/net/proto.rs), lines 1-65, 94-120; role: implementation.
- [LICENSE](https://github.com/splch/mycel/blob/07a671d03c4fbb68f459d24af262a3dec563c913/LICENSE), lines 1-18, 20-29, 80-89; role: license.
- [src/search/fanout.rs](https://github.com/splch/mycel/blob/07a671d03c4fbb68f459d24af262a3dec563c913/src/search/fanout.rs), lines 1-20, 47-65, 87-96, 103-112, 145-242, 280-307; role: implementation.
- [src/config.rs](https://github.com/splch/mycel/blob/07a671d03c4fbb68f459d24af262a3dec563c913/src/config.rs), lines 1-12, 19-28, 52-61, 95-104, 112-121, 202-211; role: implementation.
- [Cargo.toml](https://github.com/splch/mycel/blob/07a671d03c4fbb68f459d24af262a3dec563c913/Cargo.toml), lines 1-46; role: deployment.
- [tests/federation.rs](https://github.com/splch/mycel/blob/07a671d03c4fbb68f459d24af262a3dec563c913/tests/federation.rs), lines 1-65; role: tests.
- [src/net/endpoint.rs](https://github.com/splch/mycel/blob/07a671d03c4fbb68f459d24af262a3dec563c913/src/net/endpoint.rs), lines 1-12, 160-205, 221-230, 275-284, 374-383; role: implementation.

## Runtime architecture

Rust crawler/search node with Tokio, iroh QUIC endpoints, peer fan-out and optional shard synchronization.

## Domain model

Node identities, allowlisted peers, search hits, circuit-breaker state, versioned query/sync frames and source attribution.

## Storage model

Cargo dependencies and endpoint queries indicate local SQLite, Tantivy and shard artifacts. This review covers federation boundaries, not database/index/WARC correctness.

## AI and matching implementation

Federated text search rather than item perception. Merge interleaves local and peer results and deduplicates URLs; it deliberately does not globally compare raw node scores.

## Security model

handle_conn checks the authenticated remote endpoint ID against an allowlist after QUIC connection establishment. ALPN versions and stream/frame/query/result limits are explicit. Transport-authenticated peers do not imply per-tenant authorization or a signed recovery-message protocol.

## API design

Length-prefixed JSON query/sync frames over versioned ALPNs; source attribution is applied by the requester from the dialed peer, not accepted as an arbitrary hit field.

## Event model

Peer success/failure updates local breaker state and tracing. Shard synchronization is not an ownership/custody event ledger or a transactional recovery outbox.

## Deployment model

Cargo manifest declares Rust 2024 and AGPL-3.0-only. Federation defaults are disabled; configuration can use relays/discovery or explicit empty-preset addresses. No new Dzoka deployment dependency is approved here.

## Test coverage

Inspected loopback federation integration-test setup and actual merge assertions proving interleaving, deduplication and limits. Not executed; source test intent is not a network security result.

## Strengths

Concrete timeouts, per-peer failure handling, provenance from authenticated peers, wire limits and non-comparable-score handling.

## Weaknesses

Protocol is for web search/shards, not private physical-property evidence, claims, key rotation or recovery authorization. A global peer allowlist is coarser than Dzoka policy.

## Security concerns

Do not federate original PII/media or synchronize private shards by analogy. Add scoped trust, schema/payload minimization, message integrity/replay policy and key revocation for the recovery protocol.

## Scalability concerns

Parallel peer tasks have hard timeouts and bounded replies; shared connection-pool locks, fan-out cardinality, request quotas and circuit-breaker behavior still need realistic failure/load testing.

## Licensing consequences

Copyleft source must not be incorporated into the MIT core without explicit license-architecture acceptance. Provider/process separation is not asserted to eliminate obligations. No source code was incorporated.

## Reusable components

Reference peer identity, failure isolation, score-agnostic fusion and attribution concepts; independently implement Dzoka federation contracts.

## Do not reuse

No AGPL source incorporation into MIT core. Exclude public-web crawl assumptions, whole-shard private-data replication and uncalibrated global score sorting.

## Architecture lessons

Federate necessary evidence with authenticated attribution and bounded work; trust is explicit and local records remain authoritative.

## Disposition

REFERENCE. Applies to this declared integration scope. It is not a claim of production readiness, blanket source reuse approval or completion of later integration acceptance tests.

## ADRs triggered

Proposed investigations: node identity/trust; bounded peer fan-out; non-comparable raw scores; local PII and signed privacy-safe recovery messages. This audit supplies evidence for later technical ADRs; it does not finalize the architecture.

## Unresolved questions

Full iroh identity/key persistence, replays across retries, peer revocation, shard validation and privacy-preserving query negotiation are integration/design work, not certified by this review.
