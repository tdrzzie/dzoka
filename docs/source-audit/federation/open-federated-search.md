# Source audit: open-federated-search

Status: COMPLETE

Scope: architecture-relevant static review of the listed source ranges, license evidence, deployment entry points and tests/evaluation where present. No upstream tests were executed and no upstream implementation was copied into Dzoka. Unreviewed subsystems and integration acceptance work are called out explicitly.

## Repository identity

https://github.com/dstl/Open-Federated-Search

## Audited branch

master. The exact commit below is authoritative; branch HEAD may move.

## Exact commit SHA

fe13ee670bd12d55d693a1e55f5be03c7d01333c

## Audit date

2026-10-26

## License

Apache-2.0. Root LICENSE text inspected at the pinned commit: Apache-2.0. Dependency, checkpoint and dataset licenses remain separate.

## Files inspected

- [src/uk/gov/dstl/fedsearch/HTTP.java](https://github.com/dstl/Open-Federated-Search/blob/fe13ee670bd12d55d693a1e55f5be03c7d01333c/src/uk/gov/dstl/fedsearch/HTTP.java), lines 1-65; role: implementation.
- [src/uk/gov/dstl/fedsearch/REST.java](https://github.com/dstl/Open-Federated-Search/blob/fe13ee670bd12d55d693a1e55f5be03c7d01333c/src/uk/gov/dstl/fedsearch/REST.java), lines 1-65; role: implementation.
- [LICENSE](https://github.com/dstl/Open-Federated-Search/blob/fe13ee670bd12d55d693a1e55f5be03c7d01333c/LICENSE), lines 1-65; role: license.
- [src/uk/gov/dstl/fedsearch/Hit.java](https://github.com/dstl/Open-Federated-Search/blob/fe13ee670bd12d55d693a1e55f5be03c7d01333c/src/uk/gov/dstl/fedsearch/Hit.java), lines 1-41; role: implementation.
- [src/uk/gov/dstl/fedsearch/GaianHandler.java](https://github.com/dstl/Open-Federated-Search/blob/fe13ee670bd12d55d693a1e55f5be03c7d01333c/src/uk/gov/dstl/fedsearch/GaianHandler.java), lines 1-65; role: implementation.

## Runtime architecture

Legacy Java servlet search interface delegates through GaianDB/JDBC and HTTP wrappers to independently operated search engines.

## Domain model

Hit includes title, URL, source URL, repository name, date, score and hit count; source-engine metadata is retained.

## Storage model

GaianHandler opens a Derby/GaianDB JDBC connection; it does not supply a property database or custody ledger.

## AI and matching implementation

Federated lexical search abstraction; no item perception or visual inference in reviewed classes. Hit.compareTo orders results by score, without independent-node calibration metadata.

## Security model

Servlet authentication/filter deployment was not established by reviewed sources. GaianHandler includes fixed demonstration database credentials; their values are not reproduced in reports.

## API design

@WebServlet /REST exposes distributed search; HTTP helper constructs HttpURLConnection from a supplied URL string. Endpoint/URL policy must be imposed by trusted configuration.

## Event model

Synchronous JDBC/HTTP request/response; no replay-safe recovery domain event or outbox/inbox contract.

## Deployment model

Java servlet container plus separately configured Derby/GaianDB/search engines. No reproducible build/dependency lock or production deployment manifest found in the pinned source tree.

## Test coverage

No executable tests found in the inspected pinned tree. No live servlet/database/search environment executed.

## Strengths

Source-engine attribution and adapter-oriented cross-repository search are useful historical interoperability concepts.

## Weaknesses

Raw-score ordering assumes comparability; getHTTP connects without setting a request-specific connect/read timeout in the reviewed path. Source strings/default connections are unsuitable production governance.

## Security concerns

Add authenticated/allowlisted destinations, SSRF controls, timeouts, safe error responses and least-privilege credentials. Full servlet filters and query construction were not certified.

## Scalability concerns

Blocking HTTP/JDBC plus unbounded response accumulation in the reviewed helper need budgets and peer failure isolation; distributed delegation alone does not bound latency.

## Licensing consequences

Preserve applicable license/copyright/NOTICE obligations for future permitted reuse. Code licensing does not establish permission for every model weight or dataset. No upstream implementation was incorporated into the core.

## Reusable components

Reference adapter/source attribution concepts only; build a modern signed, privacy-aware recovery gateway.

## Do not reuse

Do not adopt raw node-score sorting, credential defaults, unrestricted endpoint URLs or indefinite blocking calls.

## Architecture lessons

A federation abstraction must preserve peer/model/evidence provenance and deadline isolation; search transport is not recovery trust.

## Disposition

REFERENCE. Applies to this declared integration scope. It is not a claim of production readiness, blanket source reuse approval or completion of later integration acceptance tests.

## ADRs triggered

Proposed investigations: node identity/trust; bounded peer fan-out; non-comparable raw scores; local PII and signed privacy-safe recovery messages. This audit supplies evidence for later technical ADRs; it does not finalize the architecture.

## Unresolved questions

Legacy dependency compatibility, full query sanitization, servlet authentication and secure node operations remain unverified; direct runtime adoption is not recommended.
