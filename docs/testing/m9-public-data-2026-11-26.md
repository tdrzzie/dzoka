# M9 public camera evaluation

The publisher provides MEVA surveillance video and activity annotations under CC BY 4.0. Its public S3 bucket permits downloads without an AWS account. The abandoned-package example `examples/videos/ex000-abandon-package.mp4` was downloaded on 2026-11-26 together with the publisher's license. Attribution belongs to Kitware Inc. and IARPA. [Dataset and license](https://mevadata.org/), [public download registry](https://registry.opendata.aws/mevadata/).

The example is 4,216,365 bytes. Dzoka's pinned isolated video decoder successfully extracted the samples at offsets zero and two seconds within its supported frame dimensions. The receipt records exact source and license hashes, attribution, frame dimensions and output hashes. The original clip and license are retained in the ignored local audit cache. [Source and execution receipt](artifacts/m9-public-data-20261126/meva-source-and-decoder.json).

## Implemented evaluation and corrections

`tools/edge_public_evaluation.py` verifies local clip and license hashes, checks the immutable edge runtime, decodes bounded samples in the pinned FFmpeg container and runs the actual isolated reference tracker. It records source, reference, frame, evaluator, provider, configuration and runtime identities without exporting pixels. It preserves uncertain labels and provider abstentions separately from positive and negative decisions. No dependency, model weight or threshold changed.

Three product defects were corrected. A cached digest could conceal a missing or modified indexed file. Every read now checks the actual file and digest before using decoded pixels. Unknown observations no longer generate a possible-disappearance window. Coarse-to-fine reads can revisit earlier timestamps, so continuity is now calculated from chronological adjacent observations rather than inspection order. Regression tests cover missing cached input, cached-file mutation, uncertain gaps and backward refinement. The new provider source hash creates a new processor profile; historical receipts remain unchanged.

The [frozen expanded manifest](artifacts/m9-public-data-20261126/evaluation-manifest.json) evaluates two camera scenes. The abandoned bag has three present and three not-observed fixed-region labels, with one uncertain occluded sample excluded from scoring. All six labelled decisions agree, and mean box IoU is 0.875. A stationary orange bottle in the independent indoor object-transfer example remains present on three samples despite background activity, with mean box IoU 1.0. [Actual isolated execution](artifacts/m9-public-data-20261126/expanded-evaluation-result.json).

The pickup and set-down examples in that manifest were inspected and excluded because the item was too small or occluded for reliable manual item-box labels at the supported decode resolution. A further pickup view has the same limitation. [Screening record](artifacts/m9-public-data-20261126/screening.json). Exclusions are coverage limits. The earlier one-scene execution is retained with its [original manifest](artifacts/m9-public-data-20261126/initial-evaluation-manifest.json) and [result](artifacts/m9-public-data-20261126/evaluation-result.json).

The agent authored approximate item boxes by visual inspection. These are development diagnostics selected after exploratory execution. Reference offsets are disjoint from scored offsets, but this is not a blind holdout or an independent annotation review. Publisher annotation overlays remain in the videos. The negative labels mean the reference is not visible within the fixed region. They establish neither global absence nor a confirmed property transfer. There is no scored real removal event in these clips, and the crop disclosure policy has not received representative privacy acceptance. The existing authored removal and reviewer-gated crop integrations retain their narrower scope.

## Reproduction

Download the clip and license URLs in the manifest to an ignored directory using the specified local basenames. The evaluator refuses altered content. With the pinned Docker images already installed, run from the checkout, choosing a new output path for every execution.

```powershell
python -B tools/edge_public_evaluation.py --manifest docs/testing/artifacts/m9-public-data-20261126/evaluation-manifest.json --inputs .audit-cache/m9-public-review --output .test-tmp/public-camera-replay.json
```

All 250 Python tests pass with the real isolated compressed-video test enabled. The focused native run passes 28 Java unit tests and three Edge integrations, including the actual isolated worker, and restores 74 tables with 99 synthetic rows. The broad native database/API run passes 28 Java unit tests and 78 integrations and restores 74 tables with 4,671 rows, exact catalog equality and tenant-isolation checks. This run does not repeat the external model/provider matrix. Source-audit, architecture and dependency checks also pass. [Verification receipt](artifacts/m9-public-data-20261126/verification.json), [focused restore](artifacts/m9-public-data-20261126/focused-edge-restore.json), [broad restore](artifacts/m9-public-data-20261126/full-core-restore.json), [artifact hashes](artifacts/m9-public-data-20261126/SHA256SUMS).

## Remaining work

Larger unoverlaid item tracks, real removal events, independent annotations, representative quality thresholds, difficult motion/occlusion and privacy-redaction testing remain local engineering and evaluation work. The two successful scenes do not close M9 quality acceptance. A learned tracker or higher-resolution profile needs its own architecture, license and runtime review.

Institution-specific deployment acceptance requires access to the selected camera host and its operational authorization. Recovery-transition calibration needs confirmed custody/return histories appropriate to the target institutions. Public surveillance activity labels support camera diagnostics but do not supply those operational facts. These deployment and outcome requirements remain external dependencies.
