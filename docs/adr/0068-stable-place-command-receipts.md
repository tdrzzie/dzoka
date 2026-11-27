# 0068: Versioned deterministic place-command hashes with legacy readers

- Status: Accepted implementation decision; execution recorded separately
- Date: 2026-11-27

## Context

The M3 recheck reproduced different persisted request digests across JVMs because V27 hashes JSON from an unordered two-entry map. Same-process retry tests did not establish restart/replica safety.

## Decision

Append V28 with a receipt hash-version column. Existing rows remain version 1 with their original digests and results. New rows explicitly use version 2: domain-separated, fixed-field-order binary encoding, UTF-8 strings with length/null framing, fixed-width integers and exact nullable double bits. Alias order and raw labels remain significant. No injected/global JSON mapper setting influences version 2.

Version 1 readers reconstruct both exact historical outer-map orders, using a private frozen JSON mapper and the explicit original ten-field draft projection. A replay succeeds only if one reconstructed digest equals the stored digest. Version 2 requires its exact canonical digest; unknown versions fail closed. No receipt is rewritten, no changed-body or actor/tenant/revocation check is waived, and no global serialization behavior changes.

## Alternatives

Changing only the hash formula strands old receipts. Ignoring hashes on replay permits changed requests. Globally sorting all JSON can alter unrelated provider/provenance contracts. Updating every stored hash cannot reconstruct input from a digest and would fabricate historical evidence.

## Consequences

Deploy the schema and patched application together under a controlled drain/upgrade: old binaries do not understand version 2 and already have the version 1 map-order defect. Do not route requests to old replicas or downgrade to them after creating version 2 receipts. Patched replicas read both formats; existing hashes, snapshots and keys remain unchanged. Future encoding changes require a new version and explicit reader compatibility. No new dependency is introduced.

## Source-audit evidence

The [confirmed recheck](../testing/m3-post-publication-recheck-2026-11-27.md), [ADR 0067](0067-canonical-places-and-media-reconciliation.md), [tenant boundary](0025-tenant-security.md) and existing JDK/Jackson [adoption](../dependencies/foundation-adoption.md) govern this fix. Source gate passed before implementation; no upstream source/runtime artifact is added.

## Implementation implications

Test independent-process hashing, actual concurrent HTTP replicas and restarted servers against one real database/JWKS, both historical encodings through a populated V27→V28 upgrade, preserved receipt bytes/results, changed bodies, tenant/actor scopes and revocation. Run full provider integration and restore checks before closing M3.

[Execution and source-bound evidence](../testing/m3-retry-fix-2026-11-27.md) record the completed checks separately from this decision.

## Unicode validity refinement (2026-11-27)

The [Unicode recheck](../testing/m3-unicode-recheck-2026-11-27.md) reproduced an accepted lone-surrogate label aliasing a literal question mark before hashing. Place label/alias construction and normalization now reject residual surrogate code points; valid pairs remain accepted and raw labels stay exact. The version 2 text encoder independently uses strict UTF-8 encoding. These are input-validity checks: every previously valid Unicode digest remains byte-identical, both historical readers remain intact, no receipt is rewritten and V28 is unchanged. Invalid historical request text is rejected rather than reconstructed through lossy replacement. No new runtime dependency or format version is introduced. [Repair execution](../testing/m3-unicode-fix-2026-11-27.md) records regression and full-gate evidence.
