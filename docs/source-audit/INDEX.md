# Canonical source audit index

As of 2026-10-26: all 30 mandatory sources have completed bounded static reviews; none remain pending. The scoped Java/PostgreSQL foundation is now implemented; full product delivery remains in progress. See [STATUS.md](../../STATUS.md) for verification results and remaining work.

The reviews cover 29 pinned repository revisions and the GS1 Global Traceability Standard, release 2.0 (August 2017). Branch names identify discovery branches; pinned commits are not guaranteed to remain HEAD. [Discovery records](discovery.json) retain the upstream trees and root license paths. DeepSORT uses the canonical `nwojke/deep_sort` implementation; its GPL-3.0 license was inspected. The GS1 artifact digest, edition and selected page scope are recorded in its audit.

The supplied `docs/platforms/` and `docs/findings/` remain intact as historical research. La2eet and Malak LostAndFound are one source and revision, not independent confirmations. Current conclusions are in the [five findings documents](findings/architecture-inputs.md).

| Source | Status | Disposition | Revision / edition |
|---|---|---|---|
| [globalenglish-lostfound](platforms/globalenglish-lostfound.md) | COMPLETE | ADAPT | 6ff8cffe55bfc3e7274124512a69fae746ceb643 |
| [furicbond-lost-and-found](platforms/furicbond-lost-and-found.md) | COMPLETE | REFERENCE | ebe3974ef0c0096a521836b934661fa6e4f99897 |
| [lost-and-found-tracking-system](platforms/lost-and-found-tracking-system.md) | COMPLETE | ADAPT | 4ef1493f80590f6fa03d2ad9eded2e70a59ebe58 |
| [smart-asset-finder](platforms/smart-asset-finder.md) | COMPLETE | REFERENCE | 8ac4484354d23aea42e1a569e2f127a878049b1e |
| [ai-lost-and-found](platforms/ai-lost-and-found.md) | COMPLETE | REJECT | 577edc3b492f62ac8707fc950230bc04a354e9d8 |
| [malak-lostandfound](platforms/malak-lostandfound.md) | COMPLETE | REFERENCE | d5d7bfaf043396a32b40c264c647f6456cadb633 |
| [findback](platforms/findback.md) | COMPLETE | ADAPT | fc91563886e4e8da6931bc07dd025fbe235068a2 |
| [kwasu-lost-found-bot](platforms/kwasu-lost-found-bot.md) | COMPLETE | REFERENCE | e4863739135594d8ce0a541be6029d0bba5ae95c |
| [trishthakur-lost-and-found-ai](platforms/trishthakur-lost-and-found-ai.md) | COMPLETE | REJECT | 8e5fcb6f79595c9d429679ba7868cac4a90e1319 |
| [visualmatch](retrieval/visualmatch.md) | COMPLETE | REFERENCE | 941c69fa7d82c7fd5f570ad7392833b8d0b83791 |
| [openvisualsearch](retrieval/openvisualsearch.md) | COMPLETE | REFERENCE | 134dbb376bd61eac77bc388ef3531edd87fd5362 |
| [ilias](retrieval/ilias.md) | COMPLETE | ADAPT | 208815da09c31f728ca8427520eb4898c5156273 |
| [patchwise-retrieval](retrieval/patchwise-retrieval.md) | COMPLETE | REFERENCE | d593101957933fcd574649fcb3b0af02c7295c11 |
| [reranking-transformer](retrieval/reranking-transformer.md) | COMPLETE | REFERENCE | 81fca3ca9b191950f702ae712daef0400608ae3a |
| [openclip](retrieval/openclip.md) | COMPLETE | ADOPT | 2bcb5adf5090df595a78b56ca9aee7b1c70efc18 |
| [pgvector](retrieval/pgvector.md) | COMPLETE | ADOPT | 5db3087b27a4fd095e7398f4b0f2706bf6d54bd3 |
| [qdrant](retrieval/qdrant.md) | COMPLETE | ADOPT | 222b2e44ab1c3f17e8246396ead6558e45041df7 |
| [paddleocr](vision/paddleocr.md) | COMPLETE | ADOPT | dab3fe35379033fdcb2d0e9572fac0b36c9a9ebf |
| [paddledetection](vision/paddledetection.md) | COMPLETE | ADOPT | b25522a0f4bde8c80603f3ba5e3472059972e3b5 |
| [detectron2](vision/detectron2.md) | COMPLETE | REFERENCE | b0fe2f0a5bb6496f65246ef35687a386a9f19202 |
| [ultralytics](vision/ultralytics.md) | COMPLETE | REFERENCE | 739d9c72b74b498fa674a477dc6a8190e2373350 |
| [deepsort](vision/deepsort.md) | COMPLETE | REFERENCE | f08cf1dc470eeb1cd2add1cbf077d95ac6c48aab |
| [cctv-theft-finder](cctv/cctv-theft-finder.md) | COMPLETE | ADAPT | c3af22b1d6239424459a0c568f3a575de49572d6 |
| [trajectron-plus-plus](cctv/trajectron-plus-plus.md) | COMPLETE | REFERENCE | 1031c7bd1a444273af378c1ec1dcca907ba59830 |
| [mycel](federation/mycel.md) | COMPLETE | REFERENCE | 07a671d03c4fbb68f459d24af262a3dec563c913 |
| [open-federated-search](federation/open-federated-search.md) | COMPLETE | REFERENCE | fe13ee670bd12d55d693a1e55f5be03c7d01333c |
| [openepcis](traceability/openepcis.md) | COMPLETE | ADAPT | e81a857771c4c4a2d12e62b60a2dbc094551aa6e |
| [gs1-end-to-end-traceability](traceability/gs1-end-to-end-traceability.md) | COMPLETE | REFERENCE | 2.0.0 (August 2017) |
| [dpp-did](traceability/dpp-did.md) | COMPLETE | REJECT | 44533c27967d406f4eeb5f09c4257b506a0264d8 |
| [openescrow](recovery/openescrow.md) | COMPLETE | REFERENCE | abebc3b6543a975e7ec5943e9bdd53cea6dc168a |

Evidence manifests contain 176 inspected file/artifact records with immutable URLs, ranges, Git blob IDs where applicable and SHA-256 digests. Source content was checked against pinned blobs; the GS1 digest covers the original PDF. `COMPLETE` means the declared source scope was reviewed; it does not certify runtime behavior, dependencies, weights or production suitability. Upstream tests were inspected where recorded but not executed.

Disposition totals: ADOPT 5, ADAPT 6, REFERENCE 16, REJECT 3. Nine unresolved project licenses and one all-rights-reserved project restrict reuse; completed review does not grant permission to copy. See the [licensing matrix](findings/licensing-matrix.md).

Run `python tools/audit_registry.py check`, `summary` and `gate`. The mandatory gate is enforced in governance CI and must pass before architecture finalization or application implementation.
