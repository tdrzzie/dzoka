# M0 independent revalidation

Rechecked on 2026-10-26 in response to the user's question whether M0 was actually completed or merely marked complete. This is a fresh verification of existing evidence, not a retrospective claim that upstream programs or security tests were executed.

## Verdict

The defined local M0 deliverables are substantively present and verified: governance files, 30 scoped source reviews, pinned evidence, five reconciled findings documents and a functioning audit gate. The COMPLETE flags are supported by actual upstream bytes, not just document status text. At this inspection, the checkout had no Git commit and its files were untracked; publication and remote CI execution were not established. The user subsequently authorized initial commit/publication. This report preserves the inspection-time result; remote CI enforcement still needs an actual workflow result.

## Checks executed

| Check | Actual result |
|---|---|
| Required scope against the original specification section 2 | All 30 audit filenames match; no mandatory source silently removed |
| Canonical review documents | 30 documents, each with all 26 required sections; scoped findings and limitations present; no TODO/TBD/placeholder markers found |
| Registry and mandatory gate | Both passed; COMPLETE 30, PENDING 0, IN_PROGRESS 0, STALE 0 |
| Fresh upstream source downloads | All 175 repository files matched recorded SHA-256, Git blob SHA-1 and original decoded line counts, including the original UTF-16 file |
| GS1 standard | Fresh official edition download matched original SHA-256 and 4219090-byte length |
| Pinned repository root trees | All 29 resolved; the nine unresolved-license projects still have no root LICENSE/COPYING candidate at the recorded revision |
| Narrative spot checks | Hash-matched source corroborated Globalenglish's payload-supplied actor/release boundary, OpenCLIP's optional pretrained/random-init behavior, Mycel's score-agnostic fan-out and Ultralytics' AGPL license |
| Findings and governance artifacts | Five canonical findings documents and root governance policies exist; licensing/dispositions are recorded, including restrictive/unresolved outcomes |
| Regression tests | 32 current Python tests passed: 20 original registry/gate, 4 new live-byte-verifier failure/encoding tests, 5 dependency-lock and 3 AI-profile tests |
| CI configuration | Governance workflow calls registry check, tests and mandatory gate; no remote workflow run verified |
| Git delivery | No HEAD commit exists; no commit, push or remote workflow was performed by this recheck |

All **176/176 artifacts passed**, with **0 mismatches and 0 unverified downloads**. Machine-readable records: [byte verification](verification/m0-bytes-2026-10-26.json) and [root checks](verification/m0-roots-2026-10-26.json). No upstream source contents, credentials or personal data are included in these records.

## What the result does not prove

The existing offline gate validates scope, document structure, pins, manifest hashes and metadata consistency. It does not fetch upstream bytes, establish that every written conclusion is correct, or prove human inspection. The new live check separately revalidates the recorded source bytes; the narrative checks above are samples, not an independent line-by-line re-audit of every subsystem.

M0 completion means bounded static source review. It does not mean upstream test suites ran, all dependencies/model weights were licensed, every repository is secure, benchmarks were reproduced or the product is production-ready. Nine unresolved project grants and one all-rights-reserved finding remain restrictions on reuse; they are completed review outcomes rather than permission to copy. Security contacts/response operations and production assurance remain explicitly deferred work.

## Reproduce

```sh
python tools/audit_registry.py check
python tools/audit_registry.py gate
python -m unittest discover -s tests -v
python tools/verify_source_evidence.py
```

The last command performs read-only public downloads; it installs/runs no upstream code. Its default report is ignored under `.audit-cache`. Network failure yields UNVERIFIED and a nonzero exit, never a successful audit claim. Source-byte mismatch and incorrect line counts also fail. Recheck root trees and narrative claims separately when changing a pin; the existing audit statuses were not altered to manufacture a passing result.
