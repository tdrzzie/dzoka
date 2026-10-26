# Authorized coarse-to-fine edge vision

Accepted design, 2026-10-26. Implementation is pending. This view follows the [current baseline](baseline.md) and ADRs [0016](../adr/0016-detector-licensing.md); [0023](../adr/0023-cctv-coarse-fine.md). The canonical audits linked by those ADRs provide pinned source evidence and scope limits. Runtime/artifact choices must pass [dependency adoption](dependency-adoption.md).

## Investigation scope

An InvestigationGrant binds tenant/institution, authorized cameras, purpose, item/case reference, exact or uncertain time interval, actor/reviewer, allowed operations/export fields, retention, resource budget and expiry. Both dispatch and each read/export recheck grant validity. No default facial recognition or unrestricted cross-camera identity tracing.

Video stays within authorized edge storage. Start with time/camera narrowing and coarse frame sampling; run approved detection, then refine candidate windows with local descriptors/tracking as justified. Preserve original video segment references, timestamp conversion, gaps, occlusion and last-seen uncertainty. A disappearance is a hypothesis; it is not theft proof or a custody transfer.

## Worker and evidence lifecycle

Task-scoped credentials allow only approved inputs. Decode/inference is isolated with limits, cancellation and bounded retries. Revocation stops further reads and exports; temporary artifacts expire and previously authorized disclosures remain audit records. Export minimal redacted crops/intervals and provenance, not whole unrestricted video streams. Detector/tracker weights and code require independent license review; restrictive research code is not copied into core.

## Acceptance

A06–A08, A22, A27–A28. Demonstrate grant expiry mid-job, camera/time rejection, cancellation, real gaps/occlusion, resource limits, redaction and deletion. Actual camera accuracy and compute cost remain unmeasured.

Acceptance IDs refer to [the implementation acceptance ledger](acceptance.md); none is claimed executed by this document.
