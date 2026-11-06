# M4 public reference pilot — 2026-11-06

User authorized finding online references. The [data review](../dependencies/public-reference-data-review.md) separates licensed evaluation material from runtime code and weights. No upstream loaders, source code or new runtime dependencies were executed or installed. References were selected before inference; failures remain failures. Private data/provider artifacts are excluded from Git. Public visibility does not establish training independence, lost-property representativeness or human acceptance.

## Translation and speech

Actual isolated inference used immutable bundle receipt `c955b34be6cf201496d4515f1ee633b207d9124a35d43efa653419c833ee29fa`. The private result is `C:\Users\nyash\DzokaPublicReferenceEvaluation-20261106-v1\result.json`, SHA-256 `e5524900d9c0948318f80ac065cbb85af0ee83951f29bfd9819cee587fd465e8`. Its selection, per-case provider evidence, metrics, analysis-source hash and source/license/preprocessing records are retained alongside original notices. Selection SHA-256 `9d3abd5c20fd3178fdd345f56956276e40e2342c920dad87e4b05023593af31d`.

FLORES-200 revision `a6c830c6e1051fb4ac1a44b32358f00463f332bd`, original public archive SHA-256 `b8b0b76783024b85797e5cc75064eb83fc5288b41e9654dabc7be6ae944011f6`, supplied eight aligned devtest language files. The first four rows were used across 18 translation profiles, 72 cases. FSDD revision `26eb9aaf76e81b692f806f9140c2d2777410d7a1` supplied twelve Git-blob-verified WAVs: digits 0 and 7, index 0, all six documented speakers. Deterministic 8-to-16-kHz PCM conversion and both hashes are retained. No demographic metadata was used.

83/84 provider calls returned available evidence. One Zulu-to-English M2M case is unavailable: a separate, explicitly host-only public-input diagnostic identified the existing unknown-generated-vocabulary guard. It remains fail-closed; there is no substitute output. Of 71 available translations, 50 report input or output truncation. None exactly matches its normalized reference. Mean per-case normalized character edit ratios below are diagnostic CER, not semantic translation quality or official FLORES chrF++/spBLEU. Failed cases are excluded from CER and counted separately.

| Profile/direction | Available | Mean CER |
| --- | ---: | ---: |
| M2M English → French / French → English | 4 / 4 | 0.591 / 0.501 |
| M2M English → German / German → English | 4 / 4 | 0.570 / 0.542 |
| M2M English → Spanish / Spanish → English | 4 / 4 | 0.578 / 0.572 |
| M2M English → Afrikaans / Afrikaans → English | 4 / 4 | 0.519 / 0.439 |
| M2M English → Zulu / Zulu → English | 4 / 3 | 0.759 / 0.705 |
| M2M English → Xhosa / Xhosa → English | 4 / 4 | 0.772 / 0.680 |
| Dedicated French → English | 4 | 0.305 |
| OPUS Shona / German / Spanish → English | 4 / 4 / 4 | 0.587 / 0.500 / 0.472 |
| CPU English → Shona / Shona → English | 4 / 4 | 0.584 / 0.614 |

Speech returned 12/12 outputs, only 2/12 normalized exact digits; mean CER 0.446. Narrowband isolated digits differ from intended conversational inputs, but these actual errors cannot be described as passing quality. Unknown training overlap, small sample sizes and truncation limit interpretation. Automatic edit scores cannot certify native-speaker semantic fidelity, general LLM correctness, code-switching or calibrated confidence. No tuning or promotion follows from these test rows.

## Images and the discovered export defect

The first private visual run used the same old bundle and retained all intermediate outputs. Image 1000 completed six profiles. On image 20553, detection, segmentation and image embedding completed, then the valid human caption failed at 18 tokens. An isolated retry failed again. A separately labeled host-only diagnostic reproduced the ONNX attention-bias broadcast error, 16 by 18; neither diagnostic replaced accepted inference or changed the isolation boundary.

The [publisher documents the export defect and correction](https://huggingface.co/jinaai/jina-clip-v1/discussions/22). The [replacement adoption review](../dependencies/joint-text-export-v2-adoption.md) pins the full-precision text graph and explicitly changes the joint pair's identity. Historical private outputs and the failed old run remain intact. New-version native/public-caption verification is recorded separately; earlier image/text vectors are never silently compared with the replacement space.

The corrected run completed all twelve actual isolated calls (six profiles per image), including the exact formerly failing caption without truncation. Private result `C:\Users\nyash\DzokaPublicVisualEvaluation-20261106-v2\result.json`, SHA-256 `05184f3548175f65008c2f621d1f7ed8d35845cb4720301faaae30d29167b3fb`; analysis source SHA-256 `454aca4165641e7af516b44a0d91b59effeb2eb76e44e86511581d27b0fe1990`. Bundle receipt is `24920f7773b455fe0a73af7becaefddd841d01a1584920463b1a5006cbd9961a`. Both encoders report new shared key `ee92174788ddd7e7fa321e574634ec09878dd4d5cc8e72b9f42d9ce80f78e0a0`. [Machine-readable provenance](m4-public-reference-provenance.json) retains source/data hashes without raw examples, speaker-demographic metadata or private contact details.

| COCO image | Boxes: TP / FP / FN at class-aware IoU 0.5 | Box F1 | Semantic target | Foreground IoU | Macro IoU | Matched-caption cosine |
| --- | --- | ---: | --- | ---: | ---: | ---: |
| 1000 | 10 / 0 / 7 | 0.741 | Person | 0.778 | 0.803 | 0.192 |
| 20553 | 0 / 3 / 11 | 0 | Bottle | 0 | 0.496 | 0.155 |

Masks compare a binary target-class union with independently authored noncrowd COCO polygons at the native coarse grid's half-pixel coordinates `(4j+2.5)/256 * original_dimension`. Other classes are background. This is semantic, not instance, evaluation. Bottle pixel agreement is 0.992 despite zero target overlap because background dominates; it must not be described as successful segmentation. Boxes use all referenced classes and global greedy matching, not AP. None of these metrics is an approved threshold or probability. ResNet and classical-local outputs were available, but no independently labeled same-item/hard-negative reference was supplied for them, so no identity quality is claimed. The unchanged non-joint profile identities keep the earlier language/speech observations applicable; there is no need to rerun them merely because this separately versioned pair changed.

A separate append-only `caption-matrix.json` (SHA-256 `8fa261466c3d0a141ede32e659fac0b672e10c7ad22f207c5e3be2a0b1ae356b`) compares the retained vectors after the run, using verified private evaluation code and original provider hashes. For image rows 1000/20553 and caption columns 1000/20553, cosine is `[[0.191654, -0.099527], [0.059700, 0.155241]]`. Each matched caption ranks higher in this two-image set. This is post-hoc descriptive caption alignment, not a held-out threshold, corpus retrieval recall, calibrated probability or same-physical-item evaluation. Original run results remain unchanged.

## Hash-bound protocol execution

The verified private management release's actual `quality_protocols.py` CLI was subsequently applied to 91 projected comparisons from the retained outputs: 83 available text CER cases, two box cases, two masks and four image/caption cosine pairs. No model was rerun and no old result was changed. Text results exactly match the earlier pilot; projection assertions also match the retained reference-box/mask hashes. The original unavailable case 38 remains separately recorded and unscored.

Private artifact root `C:\Users\nyash\DzokaPublicProtocolEvaluation-20261106-v1` retains original-provider/source hashes, references, projections, dataset/evidence files and projection code. Result SHA-256 `ad09077218b26f9eebca17554024e8103f8d755fca7c07bcf5146843295408e9`; projection-source SHA-256 `c05f30beda98bce4c0b6697f1fef43dea2faaa2fb7a97dd91c99e0a4b449ead7`. All hashes and declared groups were checked before metrics. Every case is TEST: unspecified institution/time and unknown model-training overlap are explicit limitations, so the declared-group check is not evidence of training independence. This is actual public-reference execution of the bounded private protocol, with approval still NOT_ESTABLISHED; mathematical/permission checks and substantive human acceptance remain distinct.

## Acceptance

Public references now provide independently authored comparison evidence and expose real limitations. They do not close M4 quality acceptance. Larger permissioned domain cohorts, declared leakage/slices, held-out thresholds, human/native-speaker review, learned/instance coverage where required and accountable institutional sign-off remain outstanding under the [checklist](m4-acceptance-checklist.md). Administrator-backed installed service identity, boot/restart, daemon/core egress and target topology acceptance are separate. A read-only service-manager create-right check returned Win32 error 5 in this environment; no service was installed.

Additional public Shona code-switch leads were checked during verification; the [license/access review](../dependencies/public-reference-data-review.md) records exact candidate revisions, inspected hashes, noncommercial restrictions, a manual gate/HTTP 401 and an unresolved grant. No candidate corpus or code was adopted and no gated file was bypassed. FLORES pure-language sentences and synthetic code-switch fixtures cannot stand in for independently approved mixed-language semantic acceptance.
