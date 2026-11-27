# M3 local implementation completion

2026-11-27. Completes the two local gaps in the [substantive recheck](m3-verification-2026-11-27.md): canonical places/aliases/hierarchy and independent cleanup after unacknowledged late media writes. The documented text/PNG/JPEG backend profile is implemented and locally verified; this is not production/site or all-format certification.

## Delivered implementation

V27 adds tenant-qualified place heads and immutable name/type/alias/coordinate/timezone revisions, separately versioned normalization, bounded cursor/name/alias APIs, historical reads and ancestor traversal. Shared aliases return candidates without automatic selection. Per-tenant serialization prevents cycles; subtree/depth, active-ancestor and archive rules protect hierarchy consistency. Versioned changes, actor-private retry receipts, minimal audit and outbox commit together. Reports optionally pin exact place revisions through the locations application interface and tenant-qualified FKs, preserving raw request/location evidence.

Media reconciliation revisits retained source/derivative intent keys after deletion/expiry/rejection, including already released quota and old tombstones with no check timestamp. Separate 120-second cleanup leases/tokens/generations prevent concurrent takeover and stale acknowledgments from releasing quota twice. Successful checks defer recurring eligibility for 15 minutes. Holds serialize before cleanup initiation. The combined `maintain-media` process runs bounded expiry/sanitization/purge, with explicit finite or repeating cycles, storage privacy rechecks and retry behavior. No service is silently installed.

[ADR 0067](../adr/0067-canonical-places-and-media-reconciliation.md), [API/worker guide](../development/places-and-media-reconciliation.md) and updated [media guide](../development/intake-media.md) define the contracts. Earlier migrations and all dependency/model/image pins remain unchanged; no runtime dependency or upstream implementation source is added.

## Executed verification

| Check | Result |
|---|---|
| Governance/dependencies | Registry integrity, source gate, architecture checker and 106 artifact/POM/license checks pass |
| Python | **269 tests pass, zero skips**, including the pinned video runtime |
| Java unit | **38 pass**, including place normalization/input invariants and module boundaries |
| Full real-provider integrations | **134 pass, zero failures/errors/skips**, on native PostgreSQL/private SeaweedFS/pinned Docker; includes **36 directly relevant M3 cases** across intake, places, media store, S3 and sandbox classes |
| Full logical restore | **111 tables/6,443 rows**: exact row/effective-catalog equality, runtime privileges, tenant isolation, private operator journal and snapshot independence |
| Additional places/restore | Nine PlacesIT cases pass again; **111 tables/330 rows** restore with same-alias ambiguity, missing/cross-tenant denial and pinned parent revisions |
| Maintenance process | Actual finite subprocess sanitizes private PNG media and subsequently erases deleted media using explicit runtime worker scope |

Real-S3 regressions PUT both source and derivative bytes **after acknowledged deletion without calling either callback**, then verify an independent due sweep removes both. Another revokes the original processor grant before its failed callback; a separate purger still erases the late derivative. Cleanup crash/lease takeover rejects old acknowledgments without double quota release. Existing holds, neighbor isolation, anonymous denial, malformed-image and stale-generation cases still pass.

Place tests exercise HTTP/JWT/SQL for raw Unicode labels, alias ambiguity/pagination, reciprocal concurrent moves, retries, archive/reactivation and history. They enforce 32-level depth/subtree bounds, tenant FKs, immutable revisions, revoked replays, report pins and outbox-failure rollback. A populated V26→V27 upgrade retains old outbox/media tombstones and validates no-op reapplication. M2's existing V25→V26 test now explicitly targets its original V26 scope instead of implicitly expecting only one future migration forever.

The initial unit attempt exposed a timezone exception classification mismatch; the domain boundary was corrected, and the final full run passes. No known failing M3 check is waived. Finite maintenance was executed; continuous target supervision/boot acceptance is separate.

## Receipts and reproduction

[Manifest](artifacts/m3-implementation-20261127/manifest.json), [JVM cases](artifacts/m3-implementation-20261127/jvm-tests.json), [full restore](artifacts/m3-implementation-20261127/full-restore.json) and [place restore](artifacts/m3-implementation-20261127/place-restore.json) bind source/evidence and exact scope. Raw execution and separately derived LF-normalized source hashes distinguish Windows bytes from Git normalization. Receipt JSON uses canonical LF bytes. Latest unit/PlacesIT XML hashes reflect the additional place-restore replay; application/test sources did not change between runs. The enhanced checker was added after the full restore and then executed against the additional restore. Their datasets/results are not conflated.

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-25.0.4'
$env:DZOKA_TEST_EDGE_SANDBOX='true'
python -B -m unittest discover -s tests -v
python -B tools/test_intake.py --native-s3 'D:\Projects\ai\dzoka\.audit-cache\seaweed-native\weed.exe' --pg-bin 'C:\Program Files\PostgreSQL\18\bin' --maven 'D:\Projects\ai\dzoka\.audit-cache\apache-maven-3.9.16\bin\mvn.cmd' --restore-drill
```

For the additional focused restore, set `DZOKA_IT_CLASSES=PlacesIT` and run `tools/test_foundation.py` with those PostgreSQL/Maven paths and `--full --restore-drill`. This is explicitly narrower than the full real-provider route. Raw logs, signing keys, credentials, operational configuration and synthetic DB/object dumps remain ignored.

## Completion boundary

The requested local M3 backend implementation is complete. Polygon/geofence scoring, transport intelligence and additional document/audio/video scanner profiles remain broader product/provider work; neither a place kind nor the raster sanitizer implies those capabilities. No antivirus/all-format certification is claimed.

Unattended host supervision/boot, institutional place data/roles, target encryption/privacy configuration, independent security/adversarial/load/HA assessment, external replicas/backups and legal retention acceptance require the selected deployment's evidence. The resweep interval is not a guaranteed erasure SLO under backlog/outage. Retain tombstones and known intent keys; unknown shared-bucket objects need operator governance. Logical restores use synthetic data, same-cluster pre-existing roles and an explicitly empty object backup manifest. Real S3 erasure/barrier tests are separate from object-store disaster recovery. No fresh remote-CI or all-profile model inference/quality result is claimed.
