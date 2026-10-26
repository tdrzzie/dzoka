# Source audit: openvisualsearch

Status: COMPLETE

Scope: Encoder selection, inference/fallback semantics, search filters, API-key middleware, startup, smoke test and Compose; segment/WAL internals and full gateway registration not reviewed.

Completion means bounded static review, not executed tests or production certification. No upstream source copied into Dzoka.

## Repository identity

https://github.com/GolffyCoding/OpenVisualSearch

## Audited branch

main. Pin is a discovery revision, not a guarantee of current HEAD.

## Exact commit SHA

134dbb376bd61eac77bc388ef3531edd87fd5362

## Audit date

2026-10-26

## License

UNRESOLVED. The complete non-truncated tree has no root LICENSE/COPYING. Dependency licenses do not license this application.

## Files inspected

- [internal/embedding/clip/clip.go](https://github.com/GolffyCoding/OpenVisualSearch/blob/134dbb376bd61eac77bc388ef3531edd87fd5362/internal/embedding/clip/clip.go), lines [[1, 125]]: Actual ONNX sessions, inference calls, mutex-protected shared runtime and normalized outputs.
- [internal/embedding/simple/simple.go](https://github.com/GolffyCoding/OpenVisualSearch/blob/134dbb376bd61eac77bc388ef3531edd87fd5362/internal/embedding/simple/simple.go), lines [[1, 110]]: Block-average image colors and feature-hashed text bags; padding/truncation does not create a shared semantic space.
- [internal/search/pipeline.go](https://github.com/GolffyCoding/OpenVisualSearch/blob/134dbb376bd61eac77bc388ef3531edd87fd5362/internal/search/pipeline.go), lines [[1, 242]]: Top-K search capped at 500 candidates before metadata filtering; fallback image errors return successful zero vectors.
- [internal/auth/auth.go](https://github.com/GolffyCoding/OpenVisualSearch/blob/134dbb376bd61eac77bc388ef3531edd87fd5362/internal/auth/auth.go), lines [[1, 28]]: Empty API key bypasses middleware authentication; otherwise a shared bearer token is compared.
- [internal/api/rest/handler.go](https://github.com/GolffyCoding/OpenVisualSearch/blob/134dbb376bd61eac77bc388ef3531edd87fd5362/internal/api/rest/handler.go), lines [[1, 80]]: Versioned search/product routes; top_k defaults to 20 when zero.
- [cmd/ovs-server/main.go](https://github.com/GolffyCoding/OpenVisualSearch/blob/134dbb376bd61eac77bc388ef3531edd87fd5362/cmd/ovs-server/main.go), lines [[1, 110]]: Encoder initialization warns and continues with content-based fallback when CLIP fails.
- [internal/embedding/clip/smoke_test.go](https://github.com/GolffyCoding/OpenVisualSearch/blob/134dbb376bd61eac77bc388ef3531edd87fd5362/internal/embedding/clip/smoke_test.go), lines [[1, 109]]: CLIP semantic and image/text smoke assertions; tests skip if vision model is absent.
- [docker-compose.yml](https://github.com/GolffyCoding/OpenVisualSearch/blob/134dbb376bd61eac77bc388ef3531edd87fd5362/docker-compose.yml), lines [[1, 43]]: Three local nodes expose HTTP/gRPC/metrics ports and bind data/configuration volumes.

## Runtime architecture

Go server constructs a scheduler, local segment manager, HNSW index, optional CLIP encoder, cluster discovery, metrics server and gateway. Startup continues with a warning when enabled CLIP cannot initialize.

## Domain model

ProductRecord and search Result contain ID, name, category, price, metadata and vectors. No lost/found, private verification, custody or ownership domain demonstrated in these paths.

## Storage model

Pipeline coordinates segment persistence with HNSW updates, and Compose mounts per-node data directories. WAL, segment recovery, consistency and deletion durability were not inspected; names and constructor calls are not proof of those properties.

## AI and matching implementation

There is real ONNX CLIP inference and tokenization plumbing. If no encoder is active, images become block-average colors and text becomes feature-hashed words. Those fallback vectors are not a shared cross-modal semantic space. In the fallback image path, decode failures return a zero vector and nil error. Hybrid search linearly blends image and text vectors.

## Security model

Reviewed API-key middleware deliberately allows all requests when configuration is empty, otherwise compares one shared bearer token. Full gateway middleware attachment was not inspected; this does not establish either universal protection or universal exposure.

## API design

REST handler registers /api/v1 image, text and hybrid searches, product operations, metrics and health. Search accepts client metadata filters and top_k; zero is defaulted, while upper/lower bound enforcement is not established in reviewed handler ranges.

## Event model

Startup schedules local work and handles shutdown. No recovery event envelope, transactional outbox, replay-safe consumer or custody event model in the reviewed paths. WAL internals were not inferred as an application event bus.

## Deployment model

Compose describes three local nodes with HTTP, gRPC and metric ports, cluster seeds and bind-mounted configuration/data. No production identity, TLS, durable cluster recovery or backup validation performed.

## Test coverage

The inspected CLIP smoke test checks cat/kitten versus car and a synthetic color-square cross-modal comparison. It skips when the vision model is absent. Tests were not run; no instance-level recovery benchmark or authorization test result is established.

## Strengths

Actual inference calls exist; encoder is injected through a plugin interface; dimension mismatch is checked on indexing; startup and smoke tests expose useful provider lifecycle boundaries.

## Weaknesses

Startup fallback changes capability while retaining the same search pipeline. A successful response can contain a zero vector after invalid fallback imagery. Generic metadata does not establish embedding-space version provenance.

## Security concerns

Empty-key fail-open policy is inappropriate for protected institutional APIs. Client filters are not tenant authorization. Full upload size, decoder sandbox, gateway identity and secret handling review remain outside this scope.

## Scalability concerns

Candidate count is capped at 500 before metadata filtering, so selective filters can underfill results. The shared CLIP runtime uses a mutex. Three Compose nodes do not demonstrate distributed durability or linear scaling; no latency or recall measurements were run.

## Licensing consequences

REFERENCE only. Do not copy project code without verified permission. ONNX runtime, models and transitive dependencies need independent review even if a project license is later clarified.

## Reusable components

Reference provider lifecycle, preprocessing, semantic smoke-test ideas and explicit observability needs. Independently implement Dzoka contracts; no upstream code copied.

## Do not reuse

Do not replace learned cross-modal embeddings with color and text hashes under the same capability claim. Do not suppress image decode errors, use client metadata as tenancy enforcement or assume padding aligns vector spaces.

## Architecture lessons

Inference capability and degraded mode must be explicit, versioned and visible to callers. Invalid media is a processing error. Distinct encoders require distinct model-space identities and compatible retrieval predicates. Filtered recall must be measured.

## Disposition

REFERENCE. Bounded static review complete; not approved for code reuse or as a production recovery foundation.

## ADRs triggered

Proposed: explicit provider degradation, embedding-space compatibility and filter-aware candidate recall. No runtime ADR accepted before the mandatory audit gate.

## Unresolved questions

Full gateway middleware installation, segment/WAL correctness, HNSW implementation, cluster trust, model weights/licensing, safe decoder limits and all failure/concurrency behavior require separate targeted review. This audit does not assert those components are correct or absent.
