# M3 post-publication recheck

**Historical finding, now repaired:** the [verified correction](m3-retry-fix-2026-11-27.md) closes this defect with versioned hashes, legacy compatibility, actual restart/replica tests and full restore. The original reproduction below remains unchanged evidence of the earlier failure.

Checked 2026-11-27 at commit `20791be6760405d724b0d812f035039756eb35a2`. The current implementation, M3 scope/ADR, existing tests, published receipt integrity and fresh real-provider execution were inspected. No application behavior was changed for this review.

**Verdict: M3 is implemented substantially, but its local completion claim is too strong. A reproduced cross-process place-command hash defect remains; M3 should not be considered fully closed until deterministic hashing, legacy-receipt compatibility and restart/replica regression coverage are verified.** This is a local engineering defect, not an external infrastructure blocker.

## Confirmed finding: place retry identity depends on JVM map order

[JdbcPlaces](../../src/main/java/org/dzoka/places/adapters/JdbcPlaces.java), line 45, hashes Jackson serialization of `Map.of("id", ..., "draft", d)`. Line 49 compares that digest with the durable command receipt and raises Conflict when it differs. No local canonical key order is specified and inspection found no repository mapper customizer enabling map-key sorting.

A source-file probe executed the exact hash expression with identical public PlaceDraft input in separate reviewed JVM processes. The follow-up probe uses Spring Boot's Jackson auto-configuration and the repository's application properties, rather than assuming a bare mapper behaves like the application. **Eight separate JVMs produced two distinct digests; map-key sorting was false in every run.** Both digest values and the complete reproduction source are in the [probe result](artifacts/m3-post-publication-recheck-20261127/spring-result.json) and [source](artifacts/m3-post-publication-recheck-20261127/SpringPlaceRetryHashProbe.java).

Consequently, the same body/key/actor/tenant can hash differently after restart or when routed to another replica. Under the inspected persisted-hash comparison, a legitimate replay then conflicts instead of returning its original receipt. The existing duplicate-command test runs inside one JVM and does not establish restart-safe hashing. This is a failure of the already-defined durable retry contract, not an added production-only criterion.

The hashing behavior was executed, not inferred from API documentation. A separate full HTTP/SQL restart/replica replay was **not** executed in this review; its conflict outcome follows from the reproduced digest difference and inspected equality branch. No unauthorized write, tenant leak or release-authority bypass is claimed.

## Fresh checks that do pass

- **38 Java unit tests and 134 full integrations pass**, zero failures/errors/skips, with actual PostgreSQL/private SeaweedFS/pinned Docker raster/video runtime.
- Restore passes **111 tables/6,452 rows**, exact row/effective-catalog equality, restored tenant/place/alias isolation, pinned parent revisions, runtime privileges and snapshot independence. This fresh run uses the enhanced checker in the full dataset.
- Source gate, 106 dependency artifact/POM/license checks and architecture checks pass.
- Published M3 child-artifact hashes match their recorded digests. All recorded LF-normalized source hashes match current committed code. This finding is not another receipt line-ending defect.

[Sanitized summary](artifacts/m3-post-publication-recheck-20261127/summary.json) and [restore record](artifacts/m3-post-publication-recheck-20261127/restore.json) bind commit, inspected source, probe and verification scope. Existing success cases confirm raw/revision preservation, place hierarchy/alias behavior, tenancy, holds, image isolation and independent orphan-media reconciliation. They do not waive the newly reproduced failure. The previous [completion record](m3-implementation-2026-11-27.md) remains historical evidence; its completion verdict is superseded here.

## Required closure

Use an explicit, versioned canonical request encoding locally for place commands. Preserve legitimate retries against both historical map-order encodings; merely changing the digest formula would strand existing receipts. Add independent-process/replica regression coverage proving identical requests replay, changed bodies conflict, historical receipts remain usable, and tenant/actor/revocation checks still apply. Re-run the relevant integration/restore gates and update completion only after that passes.

Reproduce the probe with the reviewed JDK and a classpath containing `target/classes` plus the non-test JARs in `dependencies.lock.json`; execute the linked Java source repeatedly in distinct JVMs and compare its `PROBE` digest output. The accompanying result records actual executions. Raw logs, credentials, keys and synthetic DB/object dumps remain ignored.

Production/site supervision, institutional data/roles, security/adversarial/load/HA, other codec/scanner profiles and replica/backup/legal acceptance remain separately scoped as before. They are not the reason for the revised local verdict. No new runtime dependency, model call or application fix was introduced here.
