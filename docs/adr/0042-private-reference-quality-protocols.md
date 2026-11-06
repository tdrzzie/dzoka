# 0042: Evaluate explicit private references without granting promotion authority

- Status: Accepted scoped implementation; representative acceptance remains required
- Date: 2026-11-06

After the mandatory source gate, extend the offline evaluation tooling using the existing standard library and private-file boundary. This evaluator adoption changes no inference source, model, wheel, image, registry or service lock. The subsequently discovered Jina export defect and explicitly versioned replacement are handled in a [separate model adoption](../dependencies/joint-text-export-v2-adoption.md). Keep deployed releases immutable. The new evaluator operates on operator-projected, hash-bound JSON artifacts, not on arbitrary executable adapters or live tenant database queries.

Require explicit identical model/profile/space identities for actual/reference data, input hashes and projection-source hashes. Reject duplicate case IDs and declared input/entity/institution/time groups shared across TRAIN, VALIDATION and TEST. Validate every file hash and split before computing metrics. These declarations are auditable inputs, not independent discovery of hidden leakage, consent certification or cryptographic model attestation.

Provide bounded cosine comparison, normalized text exact/WER/CER, exact label-set precision/recall, ranking top-one/reciprocal-rank/returned-list recall, class-aware greedy box overlap counts, and dense semantic-mask agreement/macro IoU/Dice. Box matching uses an explicit positive threshold and normalized positive-area coordinates, without confidence-based AP claims. Empty denominators are unavailable; zero vectors and incompatible mask geometries fail. General drafts and semantic translation correctness still require accountable human protocols and reference data.

Results retain content/source hashes, split and hashed slice identity, individual metrics and slice counts. They omit raw artifacts, references, labels and grouping metadata. New files are created exclusively; existing results cannot be overwritten. Every result leaves approval NOT_ESTABLISHED. No policy, fingerprint activation, calibrated confidence or ownership/release decision follows from a metric. Representative quality thresholds and deployment/institutional sign-off remain explicit acceptance work.

Native verification must use a real isolated embedding, protected private files, actual broad-read rejection and refusal to overwrite an existing result. Authored mathematical vectors/masks/regions verify protocols only; self-consistency and synthetic conformance never establish item identification or language accuracy.
