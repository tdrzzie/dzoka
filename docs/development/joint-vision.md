# Joint image/text and region profiles

This extends the private [perception pipeline](perception.md) under [ADR 0030](../adr/0030-joint-vision-and-region-models.md). It does not replace any published MiniLM, ResNet, Harris, OCR or speech profile. The existing reviewed Windows amd64 CPU environment and 21-wheel runtime are required; no new library is installed. See the separate [weight/license adoption](../dependencies/joint-vision-adoption.md).

After explicit artifact review, `python tools/vision_runtime.py fetch` downloads only the eight immutable size/hash-pinned artifacts into ignored `.audit-cache/vision-models`. `check` verifies all artifacts and implementation/runtime identities. `probe --image PATH` executes all four graphs on an authored sanitized RGBA fixture and a synthetic English caption. API startup never downloads or substitutes models. No publisher custom code is executed.

Use `python tools/vision_profile.py --tenant UUID --profile UUID --task JOINT_IMAGE` (or JOINT_TEXT, DETECTION, SEGMENTATION) to print a **disabled** INSERT for operator review. The profile manifest binds artifact revisions/hashes, exact independently authored preprocessing, runtime and implementation. Enabling immutable profiles is an operator database action. Assign distinct profile IDs; the two joint profiles have distinct task keys but the same recomputed shared space key binding **both** encoders and tokenizer. Matching dimension alone never establishes compatibility. Existing text retrieval/index space IDs are not reused.

Image enqueue uses the existing `POST /api/v1/tenants/{tenant}/reports/{report}/perception/image` with `profileId` and current SANITIZED `mediaId`. Joint text enqueue is `POST .../reports/{report}/perception/text` with `profileId`; it reads the current stored normalized report text, preserving the report's exact original raw submission separately. Empty/oversized text is unavailable; no synthetic caption or duplicate text store is created. Requests require owner/report-write authority and share the existing tenant 32-job reservation and 64 retained-job per-report bounds.

Configure the same private worker URL/token/tenant/profile and loopback development exception as the existing worker, plus `DZOKA_AI_VISION_DIR` pointing to the reviewed artifact directory. Run `python tools/vision_worker.py --once`. The launcher removes unrelated environment credentials and starts isolated Python with a bounded deadline. The worker gets only its live lease-bound binary input via the authenticated API, with no database/S3 credentials. Source revision, digest, profile, actor, lease, lifecycle and deadline checks govern completion. TEXT_UTF8 RLS grants only the leased revision to its live worker; image jobs receive no report text. Native process sandbox/egress/resource enforcement on a deployment host remains an operational prerequisite.

Outputs are private append-only derived evidence, initially SHADOW:

| Task | Output and limit |
|---|---|
| JOINT_IMAGE | 768 normalized values and exact paired `spaceKey`, source width/height; shortest-side 224 resize and center crop |
| JOINT_TEXT | Same 768-dimensional space, at most 512 tokenizer tokens, explicit truncation; English profile, at most 8,000 Unicode characters/32,000 UTF-8 bytes |
| DETECTION | Up to 20 score-thresholded COCO numeric classes, source-pixel xyxy boxes, uncalibrated model score, truncation flag; 300-square inference |
| SEGMENTATION | 64×64 source-aligned semantic class grid, canonical row-major `[classId,count]` RLE totaling 4,096 cells; background plus Pascal VOC 20, 256-square inference |

Detection crops and masks are not automatically embedded or used as identities. Segmentation is semantic, not instance segmentation. Scores, categories and empty outputs never authorize ownership/release or act as hard identity contradictions. No face identification is enabled. Explicit component pins and the existing cohort seal/activation policy select outputs into `ItemFingerprint`; `jointImage`, `jointText`, `detection` and `segmentation` coverage stay separate. No joint index, cross-modal ranking/calibration or representative accuracy claim follows from provider conformance.

`python tools/test_intake.py --with-ai --with-perception --with-vision --restore-drill` runs actual graphs through private HTTP/JWT, real S3, sanitizer sandbox and PostgreSQL. Portable runs include text lease/security/idempotency and typed geometry tests but exclude native model execution explicitly. `python tools/verify_local.py` requires all provider artifacts and full inference, plus the audit/architecture/dependency gates and logical restore. Full masks/vectors, source images, WAVs, model binaries and disposable databases remain ignored test data.
