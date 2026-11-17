# Private English semantic-review evidence

This offline M4 tool records independent declared reviews and explicit disagreement resolution for `ENGLISH_DRAFT` and `GROUNDED_ATTRIBUTES`. It complements numeric reference metrics. It does not judge language meaning, authenticate a reviewer, certify permission/qualification, choose thresholds, activate a model or authorize property release. [ADR 0049](../adr/0049-private-semantic-review-evidence.md) records the decision. Shona work remains deferred.

Prepare all inputs in one protected private directory. Keep names, reviewer qualifications/authority, dataset permission and rationale in private evidence files. Use random 32-character lowercase hexadecimal reviewer identifiers rather than names/email addresses. Each descriptor is `{ "path": "basename.json", "sha256": "<exact raw-file digest>" }`; paths cannot escape the directory. Retain the manifest digest independently before evaluation.

The manifest has exactly `schemaVersion: 1`, `packet`, `reviews` (two to eight file descriptors), and `adjudications` (zero to 128 descriptors). Every listed file is hash-checked. The packet contains:

| Field | Contract |
|---|---|
| `schemaVersion`, `scope` | Version 1; `AUTHORED_DIAGNOSTIC` or `REFERENCE_REVIEW` |
| `language`, `task` | `en`; `ENGLISH_DRAFT` or `GROUNDED_ATTRIBUTES` |
| `evidence` | Private dataset/permission/reference-construction declaration descriptor |
| `profiles` | Exactly `baseline` and `candidate`, each with distinct `profileKey` and `projectionSourceSha256` digests |
| `cases` | One to 128 case objects described below |

Each case has `id`, `split` (TRAIN/VALIDATION/TEST), `slice`, `inputSha256`, `entityGroup`, `institutionGroup`, `timeGroup`, `input`, `source`, `reference`, `baseline` and `candidate`. Source/input/entity/institution/time groups cannot cross splits. Repeated input within a split is permitted for explicit diagnostic observations; it is not independent sampling. The tool cannot discover undeclared shared entities, training overlap or dishonest grouping.

`input` preserves exact raw provider input bytes and must hash to `inputSha256`. For drafts it is the existing JSON request with exactly `prompt` and `query`; the reviewable UTF-8 `source` file must equal the query bytes without normalization. For grounded attributes, raw input and source bytes must match. Sources are bounded to 4,096 codepoints and draft instructions to 1,024. References and projected artifacts are bounded valid JSON, not executable adapters. The projection-source digest and packet bind the operator's declared mapping; hashes do not independently attest that a projection came from a model.

Each baseline/candidate observation has exactly `state` and `artifact`. AVAILABLE and TRUNCATED require a descriptor; UNAVAILABLE requires null. Do not turn unavailable inference into an empty proposal or a semantic failure.

Each review file has `schemaVersion`, `packetSha256`, `reviewerId`, `reviewerKind`, `authorityEvidence`, `at` and `findings`. Kinds are HUMAN, AUTOMATED or AUTHORED_FIXTURE; fixture kinds are rejected in reference-review scope. All identities/kinds/authority are declarations. A UTC timestamp uses `YYYY-MM-DDTHH:MM:SSZ`, optionally fractional seconds. Findings must explicitly cover every case and role exactly once:

```json
{
  "caseId": "private-case-id",
  "role": "candidate",
  "state": "ASSESSED",
  "verdicts": {
    "GROUNDING": "PASS",
    "UNCERTAINTY": "UNCERTAIN",
    "PRIVACY": "PASS",
    "SAFETY": "PASS"
  }
}
```

Allowed verdicts are PASS, FAIL or UNCERTAIN. Use UNREVIEWED with null verdicts for missing assessment. Provider-unavailable observations require PROVIDER_UNAVAILABLE with null verdicts. Review observed truncated content if useful, but it cannot become complete reviewed evidence or an adjudication target. Reviewers must inspect exact source/proposal/reference data: for grounding, check unsupported/omitted/misbound facts; for uncertainty, preserve negation, time/location qualifications and absence of evidence; for privacy and safety, check disclosure and unsupported guidance. This protocol records judgments; it does not make them true.

Retain the reviewer's case-specific notes and source/proposal span annotations alongside their authority declaration in the private `authorityEvidence` file. Record omissions, invented attributes, misbound spans and the reason for each FAIL/UNCERTAIN finding there, using the packet's case identifiers and exact source text offsets. This file's digest binds those notes to the review. The collector does not parse their prose or certify span correctness; reviewers/adjudicators must inspect them. An unexplained PASS matrix alone is insufficient quality-acceptance evidence.

No majority vote resolves disagreement. An adjudication file has `schemaVersion`, `packetSha256`, `reviewSetSha256`, a different `reviewerId`, `reviewerKind`, `authorityEvidence`, `at` and `decisions`. Compute the review-set digest as SHA-256 of UTF-8 compact JSON encoding of the **sorted review-file SHA-256 list** (`separators=(',', ':')`). A changed review or new review round invalidates earlier adjudications. Each decision has `caseId`, `role`, `dimension`, `verdict` and `rationaleEvidence`. It must address an actual disagreement with complete inference and all reviewers assessed. Duplicate, stale, self-adjudicated, unanimous or incomplete targets fail closed. Revised rounds use new manifests/results; keep prior evidence.

Run from a newly staged immutable management release, retaining its exact receipt:

```powershell
python -I -S -B C:\DzokaPrivate\management\tools\semantic_review.py --manifest C:\DzokaPrivate\review\manifest.json --manifest-sha256 <retained-manifest-sha256> --management-sha256 <release-json-sha256> --output C:\DzokaPrivate\results\review-001.json
```

The CLI verifies the entire protected code-release inventory before reading private data, rejects broadly readable/reparse paths, and creates a new protected output exclusively. Limits are 256 KiB for packet/manifest/review/adjudication files, 64 KiB for projected artifacts/references, 32 KiB for authority/rationale evidence, and 16 MiB for unique descriptor files plus the bounded manifest. No network/provider/database calls occur in the collector.

Results retain manifest/packet/review-set/release/protocol/source/projection hashes and timestamp, provider/review coverage, hashed reviewer/slice identities and per-dimension agreement/unresolved/adjudicated states. They omit raw texts, labels, case/group names and reviewer identities. `declaredHumanReviewed` only counts two declared HUMAN assessments; it certifies neither identity nor qualification. Direct library evaluation is marked NOT_ATTESTED; operational CLI results bind the verified management receipt. Every result leaves `approval: NOT_ESTABLISHED` and `releaseAuthorization: NOT_GRANTED`. Accountable quality approval and deterministic/human release policy remain separate.

The native verifier uses two actual isolated English draft models, preserving their original input/profile/artifact/truncation and unreviewed state in a separate packet. Authored proposal/reviewer fixtures separately test disagreements and adjudication. No fixture is represented as a real human's review or production quality approval. The full local gate contains 26 phases, including this verification before final integration/restore. Native fixture directories are removed after assertions; their conformance summary is not a retained real-review acceptance packet. Operational review inputs/results must be retained under the authorized evidence policy.
