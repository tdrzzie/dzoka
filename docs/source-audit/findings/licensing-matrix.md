# Licensing matrix

License findings are tied to the exact pinned source, not repository metadata or README marketing. All 30 reviews are complete within scope. The MIT core contains no incorporated upstream implementation. Code, model weights, datasets, transitive dependencies and containers require independent license accounting.

| Source | Inspected license outcome | Disposition | Consequence |
|---|---|---|---|
| [globalenglish-lostfound](../platforms/globalenglish-lostfound.md) | MIT | ADAPT | Permissive code grant; retain notices; review weights/data/dependencies separately. |
| [furicbond-lost-and-found](../platforms/furicbond-lost-and-found.md) | UNRESOLVED | REFERENCE | No root project grant found. No copying; independently implement reference contracts. |
| [lost-and-found-tracking-system](../platforms/lost-and-found-tracking-system.md) | MIT | ADAPT | Permissive code grant; retain notices; review weights/data/dependencies separately. |
| [smart-asset-finder](../platforms/smart-asset-finder.md) | ALL_RIGHTS_RESERVED | REFERENCE | Author explicitly reserves rights. No copying without permission. |
| [ai-lost-and-found](../platforms/ai-lost-and-found.md) | UNRESOLVED | REJECT | No root project grant found. No copying; independently implement reference contracts. |
| [malak-lostandfound](../platforms/malak-lostandfound.md) | UNRESOLVED | REFERENCE | No root project grant found. No copying; independently implement reference contracts. |
| [findback](../platforms/findback.md) | MIT | ADAPT | Permissive code grant; retain notices; review weights/data/dependencies separately. |
| [kwasu-lost-found-bot](../platforms/kwasu-lost-found-bot.md) | UNRESOLVED | REFERENCE | No root project grant found. No copying; independently implement reference contracts. |
| [trishthakur-lost-and-found-ai](../platforms/trishthakur-lost-and-found-ai.md) | UNRESOLVED | REJECT | No root project grant found. No copying; independently implement reference contracts. |
| [visualmatch](../retrieval/visualmatch.md) | UNRESOLVED | REFERENCE | No root project grant found. No copying; independently implement reference contracts. |
| [openvisualsearch](../retrieval/openvisualsearch.md) | UNRESOLVED | REFERENCE | No root project grant found. No copying; independently implement reference contracts. |
| [ilias](../retrieval/ilias.md) | MIT | ADAPT | Permissive code grant; retain notices; review weights/data/dependencies separately. |
| [patchwise-retrieval](../retrieval/patchwise-retrieval.md) | UNRESOLVED | REFERENCE | No root project grant found. No copying; independently implement reference contracts. |
| [reranking-transformer](../retrieval/reranking-transformer.md) | GPL-3.0 | REFERENCE | No GPL source incorporation into MIT core without explicit license-architecture acceptance. |
| [openclip](../retrieval/openclip.md) | MIT | ADOPT | Permissive code grant; retain notices; review weights/data/dependencies separately. |
| [pgvector](../retrieval/pgvector.md) | PostgreSQL | ADOPT | Permissive notice obligations; package/container components are separate. |
| [qdrant](../retrieval/qdrant.md) | Apache-2.0 | ADOPT | Retain license/attribution/NOTICE obligations as applicable; review weights/dependencies separately. |
| [paddleocr](../vision/paddleocr.md) | Apache-2.0 | ADOPT | Retain license/attribution/NOTICE obligations as applicable; review weights/dependencies separately. |
| [paddledetection](../vision/paddledetection.md) | Apache-2.0 | ADOPT | Retain license/attribution/NOTICE obligations as applicable; review weights/dependencies separately. |
| [detectron2](../vision/detectron2.md) | Apache-2.0 | REFERENCE | Retain license/attribution/NOTICE obligations as applicable; review weights/dependencies separately. |
| [ultralytics](../vision/ultralytics.md) | AGPL-3.0 | REFERENCE | No assumed commercial grant; no core incorporation; separate deployment is not a blanket exemption. |
| [deepsort](../vision/deepsort.md) | GPL-3.0 | REFERENCE | No GPL source incorporation into MIT core without explicit license-architecture acceptance. |
| [cctv-theft-finder](../cctv/cctv-theft-finder.md) | MIT | ADAPT | Permissive code grant; retain notices; review weights/data/dependencies separately. |
| [trajectron-plus-plus](../cctv/trajectron-plus-plus.md) | MIT | REFERENCE | Permissive code grant; retain notices; review weights/data/dependencies separately. |
| [mycel](../federation/mycel.md) | AGPL-3.0-only | REFERENCE | No core incorporation; network obligations require explicit review, not assumed isolation exemption. |
| [open-federated-search](../federation/open-federated-search.md) | Apache-2.0 | REFERENCE | Retain license/attribution/NOTICE obligations as applicable; review weights/dependencies separately. |
| [openepcis](../traceability/openepcis.md) | Apache-2.0 | ADAPT | Retain license/attribution/NOTICE obligations as applicable; review weights/dependencies separately. |
| [gs1-end-to-end-traceability](../traceability/gs1-end-to-end-traceability.md) | GS1-standard-terms | REFERENCE | Copyright/IP-policy standard, not a software license or patent-clearance certificate. |
| [dpp-did](../traceability/dpp-did.md) | UNRESOLVED | REJECT | No root project grant found. No copying; independently implement reference contracts. |
| [openescrow](../recovery/openescrow.md) | MIT | REFERENCE | Permissive code grant; retain notices; review weights/data/dependencies separately. |

Nine repository grants remain unresolved; Smart Asset Finder explicitly reserves rights. Those are completed audit outcomes, not reuse approval. Patchwise's MIT file is confined to bundled ILIAS material and does not license the main project. Canonical DeepSORT is GPL-3.0, resolving the earlier unidentified-source uncertainty. Mycel's manifest specifies AGPL-3.0-only. No alternate commercial Ultralytics grant was established.

GS1 edition 2.0 was downloaded from its official edition URL; the original PDF digest and inspected pages are recorded. Its IP-policy disclaimer is not an open-source license. Reports link to the standard rather than redistribute it. Keep the [earlier matrix](../../findings/licensing-matrix.md) as supplied historical research; current canonical findings take precedence.


## Subsequent local-development bidirectional Shona profile (2026-10-27)

[ADR 0034](../../adr/0034-bidirectional-shona-and-code-switching.md), [separate model/runtime review](../../dependencies/shona-adoption.md) and [execution record](../../testing/m4-shona-2026-10-27.md) add Apache-2.0 English↔Shona int8 models and explicit caller-marked copying. The new CTranslate2 wheel is MIT with separately governed Intel/NVIDIA binaries, so this adoption is local-development-only and does not approve production/SaaS/distribution. Existing 21/22-wheel environments/keys and mandatory thirty-source review scope remain unchanged. Marked outputs expose exact source/output offsets and unverified copy provenance; unmarked qualifier/proper-name failures remain recorded. Permitted representative data/native-speaker acceptance is external; a suitable runtime build, automatic code-switch quality and annotation UX remain local engineering.


## Subsequent CPU Shona and unmarked-span extension (2026-10-27)

[ADR 0035](../../adr/0035-license-compatible-shona-and-unmarked-spans.md), [separate closure review](../../dependencies/shona-cpu-adoption.md) and [operator guide](../../development/shona-cpu.md) and [execution evidence](../../testing/m4-shona-cpu-2026-10-27.md) add a source-built MIT CTranslate2/OpenBLAS CPU alternative and Apache-2.0-plus-notices GlotLID proposals. The new image avoids the preceding vendor-wheel limitation; that old adoption stays local-development-only. Five new immutable profiles retain separate provider/model/runtime/input provenance. Automatic English windows are source-grounded `AUTO_LANGUAGE_PROPOSAL_UNVERIFIED` copies, never caller annotations, calibrated confidence or release authority. The selected unmarked qualifier improves, while place-name and “bylack” errors remain. Mandatory audit scope and supplied bodies are unchanged. Representative/native-speaker acceptance and production hosts/registry/TLS/secrets remain external; broader language quality, portable host packaging, deployment/advisory/load work and annotation UX remain local engineering.
