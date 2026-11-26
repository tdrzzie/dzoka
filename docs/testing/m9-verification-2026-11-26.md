# M9 edge and recovery-intelligence verification

The authorization profile implements reviewer-approved investigation grants, bounded leased observation jobs, provenance, gaps, revocation checks and result retention. A native custody history query distinguishes authoritative observations from unconfirmed events. A descriptive transition baseline uses a past cutoff, excludes the target intake and requires three distinct source intakes. [Decision](../adr/0059-authorized-edge-observations.md), [API](../development/authorized-edge.md).

The integration suite exercises independent reviewer grants, idempotent submission, cross-tenant denial, revocation during a lease, cancelled completion, result scope, budget enforcement, gaps, result purging and unavailable sparse baselines. Existing recovery and channel tests run alongside it.

The local run passed 28 Java unit tests and eight focused integration tests. The final availability-filter change also passed the two M9 integration tests. The Python tooling suite passed all 240 tests. Source-audit, architecture, dependency integrity and whitespace checks passed. The recovery/channel run restored all 72 tables with exact row/catalog equality and runtime-role tenant isolation.

[Recovery/channel restore](artifacts/m9-verification-20261126/recovery-channel-restore.json), [final edge restore](artifacts/m9-verification-20261126/final-edge-restore.json), [receipt hashes](artifacts/m9-verification-20261126/SHA256SUMS).

## Portable edge profile

The final expanded run passed **246 Python tests, 28 Java unit tests and 78 Core integration tests**, with the real isolated worker enabled. This includes actual compressed-video extraction, largest supported crop upload through HTTP, reviewer-gated access, camera and plan hash binding, source/runtime integrity checks and lease renewal. The final full Core restore matched 74 tables and their complete row/catalog snapshots. The focused frame-worker restore independently matched the same 74-table schema. [Full Core restore](artifacts/m9-verification-20261126/full-core-restore.json), [frame-worker restore](artifacts/m9-verification-20261126/frame-worker-restore.json), [authored transition diagnostic](artifacts/m9-verification-20261126/transition-diagnostic.json).

The expanded implementation registers immutable camera archive manifests and binds reference appearance, ROI, camera and purpose to exact approved plan bytes. An authenticated worker claims a real Core job and rechecks access before each frame and export. It invokes the pinned Python container without networking, with an unprivileged UID, memory/CPU/PID limits and a per-frame wall-clock deadline.

The end-to-end integration runs the actual worker against Core HTTP and three authored P6 camera frames. It verifies present and not-observed states, crop ingestion and metadata completion. Independent tests cover reviewer approval before crop access, revocation and pixel deletion, and reject duplicate jobs that would multiply a grant budget. Missing observations stay distinct from not-observed appearance.

The native graph is a live projection. Each query rebuilds its authorized view from native facts and availability checks, with cutoff filtering. Three synthetic intakes verify distinct-item support, an observed 30-second median transition and target-intake exclusion. The transition model tests reject tampering, future training rows, overlapping item/institution splits and self-approval.

## Supported inputs and external acceptance

The camera profile consumes indexed P6 exports and bounded compressed clips through an isolated FFmpeg provider, yielding frames within 320 by 240 pixels. A real synthetic lossless clip test verifies indexed sampling and possible disappearance while preserving the local clip. The detector uses original bounded reference-appearance matching. The evidence profile exports item crops and uncertain event intervals. Institutional acceptance requires permitted camera archives, representative item/institution/time splits, camera and redaction-policy approval, quality thresholds and target deployment validation. Authored fixtures establish local execution and invariant behavior.
