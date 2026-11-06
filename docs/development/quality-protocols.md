# Private reference quality protocols

[ADR 0042](../adr/0042-private-reference-quality-protocols.md) adds `tools/quality_protocols.py` to the earlier [text diagnostics](managed-broker.md). It uses no new model or dependency and makes no inference request. An authorized operator supplies projected JSON artifacts and independently established references in one protected directory. Keep datasets within their authorized tenant/institutional scope; never commit private references or source artifacts.

Each manifest has `schemaVersion: 1`, `scope` (`AUTHORED_DIAGNOSTIC` or `REFERENCE_EVALUATION`), `evidence` (basename `path` and SHA-256), and 1–128 `cases`. Each case has these exact fields:

```json
{
  "id": "case-001",
  "split": "TEST",
  "slice": "authored-fixture",
  "inputSha256": "<actual-original-input-sha256>",
  "entityGroup": "authored-item-001",
  "institutionGroup": "authored-institution-001",
  "timeGroup": "authored-time-window-001",
  "spaceKey": "<actual-profile-or-space-key>",
  "referenceSpaceKey": "<same-compatible-profile-or-space-key>",
  "artifact": {"path": "actual.json", "sha256": "<actual-file-sha256>"},
  "reference": {"path": "reference.json", "sha256": "<reference-file-sha256>"},
  "metric": "cosine",
  "parameters": {},
  "projectionSourceSha256": "<reviewed-projection-source-sha256>"
}
```

Keys/hashes must be 64 lowercase hex digits. A projection is a reviewed offline transformation of provider evidence into the metric's data format. Record its actual source hash and retain original provider provenance/artifacts in the authorized evidence store. The evaluator never executes projection code, reconstructs provenance from dimensions, certifies claimed identities or approves the labels. Identical declared keys and dimensions are necessary checks, not model attestation. Cross-profile comparisons require separately defined protocols rather than relabeling incompatible vectors.

| Metric | JSON actual/reference format | Result and limits |
|---|---|---|
| `cosine` | Two nonzero finite numeric arrays, equal length, up to 4,096 dimensions | Cosine; incompatible identities/dimensions and zero vectors fail |
| `exact`, `wer`, `cer` | Strings up to 4,096 Unicode codepoints | Existing NFC/casefold/whitespace normalization; CER counts codepoints; error rates may exceed one |
| `labels` | Unique exact strings, at most 256, up to 128 characters each | Set TP/FP/FN, precision/recall/F1; absent denominators are null. Project structured attribute pairs explicitly, preserving source grounding outside the scorer |
| `ranking` | Actual: unique returned opaque labels; reference: unique relevant labels | Top-one relevance, reciprocal rank and recall within returned list; no calibrated confidence or corpus-wide retrieval claim |
| `boxes` | Arrays of `{ "label": "bag", "box": [0,0,1,1] }`, up to 256 | Class-aware global greedy matching sorted by descending IoU, ties by array index; `parameters` must be `{ "iouThreshold": 0.5 }` with an explicitly selected threshold. Counts are not AP or optimal assignment |
| `mask` | `{ "shape": [height,width], "labels": [...] }`, dense row-major integer class IDs 0–255 | Same geometry, each axis at most 256; pixel agreement and macro IoU/Dice over union of observed classes, including background. No resizing or class remapping is implicit |

All metrics except boxes require empty `parameters`. Masks/boxes must already use the same source geometry and class vocabulary; convert provider RLE/source-pixel boxes through the reviewed projection. Label-set matching is exact and case-sensitive. General generation, hallucination, translation meaning, instance identity, local descriptor matching and domain-specific fairness require additional reference/human protocols; this tool does not replace them.

Splits must be TRAIN, VALIDATION or TEST. Every declared input/entity/institution/time group must stay in one split. This implements a strict held-out-institution/entity/time protocol; it cannot discover undeclared shared items or bad group assignments. Repeated inputs within a split are permitted for explicit separate model cases, with unique case IDs. Slices are counted separately by hash. The tool does not combine training results with test results into an accuracy headline, choose a promotion threshold or infer legal consent from an evidence file.

Use the reviewed checkout for authored development diagnostics. Operational private references require a protected code release, outside shared writable checkout ancestry. Prepare a **new** code-only release using `tools/managed_release.py --target <new-private-management-directory>`; it copies these optional tools and retains exact file hashes in its receipt. Verify that receipt using the release's `managed_release.verify` before running the quality CLI. Never add files to or edit a published release. This does not replace the model bundle, install a service or change existing inference identities.

```powershell
python -I -B C:\DzokaPrivate\management-002\tools\quality_protocols.py --dataset C:\DzokaPrivate\evaluation\manifest.json --output C:\DzokaPrivate\results\run-001.json
```

Dataset/result parent directories and ancestors must pass the existing native private ACL checks. Broadly readable leaves, reparse paths, basename escapes, duplicate/nonfinite JSON, oversized files, changed hashes and cross-split leakage fail closed. Inputs are bounded at 2 MiB each, manifest 256 KiB, evidence 32 KiB. Results omit raw artifacts/reference/labels/group IDs; preserve their hashes and split, hashed slice counts, exact protocol/projection source identities and `approval: NOT_ESTABLISHED`. Keep the result private. Each output path must be new.

`tools/verify_quality_protocols.py` runs a real isolated embedding plus actual private CLI/ACL/overwrite checks. Its authored coordinate reference is a mathematical conformance diagnostic, not a semantic item-match target or benchmark. The expanded full local gate contains 24 phases and includes this check. Representative permitted data, baseline comparisons, human/native-speaker review and accountable thresholds remain necessary before model promotion.

A missing/unavailable provider result must not be projected into an empty label list or blank text to manufacture a scored mismatch. Retain its unavailable coverage in the reviewed evaluation evidence; invalid/missing files, null values and zero vectors fail rather than generating negative similarity. Empty valid predictions and unavailable inference are different states.
