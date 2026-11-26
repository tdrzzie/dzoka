# Authorized edge observation API

Paths are under `/api/v1/tenants/{tenant}/edge`. Use JWT membership with the stated permissions. Every caller also needs the existing report visibility required by its role.

A reviewer with `edge:review` uses `PUT /grants/{id}` to approve a report, one opaque camera ID, an investigator reference, an interval of at most 24 hours, expiry within seven days, a budget of at most 100 observations and a digest referencing protected purpose and authorization evidence. The investigator and reviewer must be different. A grant is immutable except for revocation through `DELETE /grants/{id}`.

The investigator with `edge:investigate` queues `PUT /jobs/{id}` with the grant ID. Each grant admits one job, with up to three lease attempts. An authorized worker with `edge:process` claims one job through `POST /claim`. The lease specifies the exact camera, interval and observation budget. The worker submits `PUT /jobs/{id}/result` with the lease token and ordered observations. Each observation has an uncertain `after`/`before` interval, a `gap` flag and source and processor SHA-256 digests. Raw images, people, coordinates and URLs are absent from this protocol.

The investigator reads the result through `GET /jobs/{id}`. Revoked, expired or unavailable reports deny access. Schedule `POST /purge` using the worker identity to clear result payloads for revoked or expired grants in batches of 100. Edge storage requires its own retention policy.

With `intelligence:read` and existing custody permissions, use `GET /intakes/{intake}/history?cutoff=...` to inspect observed and unconfirmed native events as of a past UTC cutoff. Use `GET /intakes/{intake}/baseline?site=...&cutoff=...` for distinct-item transfer counts excluding the target intake. Support below three independent intakes returns unavailable. These counts are descriptive and have no release authority.

## Register and search a local frame archive

Export a chronological camera frame index as schema 1 JSON with `camera` and `frames`. Each frame has `time`, a local basename in `name` and its `sha256`. P6 frames support up to 320 by 240 pixels. For a compressed clip, add `offset` in seconds to each indexed sample and bind the clip SHA-256. Clips are limited to 64 MiB. The pinned isolated FFmpeg provider seeks to the indexed sample and scales within the supported pixel bounds. Footage stays local. Register the exact index digest using `PUT /cameras/{id}` with `archiveSha` and `enabled`. A changed archive needs a new camera registration identity so an earlier approval cannot silently expand.

The grant evidence digest binds an exact JSON plan with `camera`, `referenceSha`, `referenceBox`, `searchBox`, `threshold` and `purpose: "LOST_PROPERTY_SEARCH"`. Boxes are integer `[x,y,width,height]` regions. A distinguishing reference crop must be at most 64 by 64 pixels. The detector is an original template appearance baseline, with explicit ambiguity and occlusion uncertainty. It reidentifies the same reference appearance locally and tracks continuity between observations.

Run `python tools/edge_worker.py` with `--base`, `--tenant`, `--archive`, `--manifest`, `--reference` and `--plan`. Supply the worker token in `DZOKA_EDGE_TOKEN`. It claims one job, rechecks `POST /jobs/{id}/access` before each read and export, and invokes the pinned Docker runtime with no network and bounded resources. Coarse sampling never assumes monotonic disappearance. Candidate intervals are refined within the observation budget. Unseen appearance and missing frames have separate statuses. Metadata contains source, processor and runtime provenance.

The worker uploads at most four item crops, excluding surrounding pixels, through `PUT /jobs/{job}/evidence/{id}`. The reviewer inspects `GET /jobs/{job}/evidence/{id}/review` and approves with `PUT /jobs/{job}/evidence/{id}/approval` and a protected review digest in `evidenceSha`. Investigator downloads through `GET /jobs/{job}/evidence/{id}` require that approval and a current grant. Revocation denies reads and scheduled purging deletes stored pixels. The portable profile exports still crops and event intervals.

## Evaluate transition estimates

`tools/recovery_transition.py` supports `train`, `evaluate`, `predict` and `approve` with explicit JSON input and exclusive output files. Training rows identify opaque item/institution/state labels, source digests and observed/recorded timestamps. Train and test must be disjoint by item and institution, with test observation windows after training recording. The model returns state frequencies and median elapsed seconds, with unavailable sparse states and unavailable calibration. Independent approval references a protected acceptance record. Select a previous immutable model artifact to roll back. These operations produce assisted-search estimates.

The SQL history query rebuilds the current graph directly from native facts and applies report availability on each read. It includes storage, accepted transfers, incidents and owner returns. Unconfirmed transfer requests and offline conflicts retain their classification. Camera codecs outside the pinned decoder's supported formats, learned detection/ReID providers, representative quality, institutional disclosure policy and deployment acceptance need separate verification.
