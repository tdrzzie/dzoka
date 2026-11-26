# M9 implementation and final local verification

M9's bounded edge and transparent recovery-baseline profile now includes the additional local features below. Institutional quality and operational deployment acceptance remain open. The results here do not mark the entire milestone production-complete.

## Implemented behavior

- Core returns possible-disappearance windows derived from persisted chronological point observations. Each window retains both source digests and the processor identity. Unknown gaps, changed processors and interval observations cannot create a window. Existing report visibility, tenant authorization, expiry, revocation and purge checks apply to the result.
- A registered compressed sample can bind an explicit native-pixel `sourceRegion`. The isolated decoder crops before downscaling, keeping the original output, memory, CPU, PID, network and time bounds. Out-of-source regions fail instead of being clamped. Regions enter cache identity and public evaluation provenance. Earlier archives retain whole-frame decoding.
- The exact approved search plan can contain a binary `disclosureMask` matching the reference box. The worker zeros outside pixels before upload. Plans without a mask export metadata only. Explicit malformed masks fail. New plan bytes require reviewer approval. Independent crop review remains necessary because a wrong match or mask can disclose unintended pixels.
- Transition evaluation v2 adds multiclass Brier error, source-state slices, unavailable coverage, unseen destinations and the precise population contributing to elapsed-time error. Reused source evidence and model cutoffs reaching test observations are rejected alongside item, institution and recorded-time leakage.
- A separate original Kaplan–Meier waiting-time baseline supports completed and right-censored observations. It preserves risk sets, tied events, sparse support, unavailable extrapolation and unknown outcomes when follow-up ends before the test horizon. Immutable artifacts retain author, purpose, source dataset, implementation digest, runtime, version, configuration and creation time. It has no automatic promotion or Core custody effect. [Statistical assumptions and scope](../adr/0063-censored-recovery-waiting-baseline.md).

[Edge decision and compatibility](../adr/0062-authorized-source-regions-and-masked-evidence.md), [API and CLI contracts](../development/authorized-edge.md).

## Public camera execution

The previous full-frame exclusions were real resolution limits. An explicit region makes the ball visible in the licensed MEVA `ex056-pick-up-object` clip. With the existing threshold 0.08 and a reference at one second, samples at zero, two and four seconds are present; six and eight seconds are not observed in the original bench region. The derived possible-removal interval is four to six seconds. The source-region annotations and mask are agent-authored approximate development labels. There is no independent annotator or blind holdout claim.

The final unchanged-configuration comparison reruns the earlier bag and bottle scenes and adds the real pickup. All 14 labelled decisions agree: nine present and five not observed. Mean box IoU is 0.875 for the bag and 1.0 for each of the bottle and ball. The bag's occluded label remains unscored. Two full-frame tiny-item examples remain excluded. Another gym pickup example was screened but its exact item could not be labelled confidently through occlusion and publisher overlays; it contributes no score.

The ball samples also execute the actual masked-crop function, retaining crop hashes and permitted/suppressed pixel counts without exporting images. The actual native HTTP worker integration independently verifies zeroed crop-border pixels, retained object pixels, review-gated investigator access and a Core window bounded by its two source observations.

[Final combined manifest](artifacts/m9-completion-20261126/combined-manifest.json), [isolated execution](artifacts/m9-completion-20261126/combined-result.json), [candidate screening](artifacts/m9-completion-20261126/additional-screening.json). Raw footage remains in the ignored audit cache. Source URLs, license hash and Kitware Inc./IARPA attribution are retained in the manifests. MEVA is CC BY 4.0 according to the [publisher](https://mevadata.org/).

## Recovery diagnostics

The waiting-time CLI was run on explicitly invented fixture histories. Two completed observations at 10 and 30 seconds plus one censored at 20 seconds yield risk sets of three, two and one. The estimated fraction still waiting at 15 seconds is two-thirds. The test contains two observable horizon outcomes and one censored too early to score. The observable-horizon Brier error is 0.277778 with two-thirds coverage. This is an arithmetic and protocol diagnostic, not measured recovery performance or calibrated production evidence.

[Diagnostic purpose](artifacts/m9-completion-20261126/purpose.json), [training input](artifacts/m9-completion-20261126/waiting-train-input.json), [final model](artifacts/m9-completion-20261126/final-waiting-model.json), [evaluation input](artifacts/m9-completion-20261126/final-waiting-evaluation-input.json), [evaluation](artifacts/m9-completion-20261126/final-waiting-evaluation.json).

## Verification

The frozen implementation passes 259 Python tests with the actual isolated video test enabled. The native database/API suite passes 30 Java unit tests and 78 integration tests with the isolated worker enabled. Its restore matches 74 tables, 4,671 rows and the full catalog and verifies restored tenant isolation. The source-audit gate, architecture check, 106-artifact dependency check and whitespace check pass. The external model/provider matrix was not repeated for these edge and offline-baseline changes.

[Execution and source receipt](artifacts/m9-completion-20261126/verification.json), [native restore](artifacts/m9-completion-20261126/full-core-restore.json), [artifact hashes](artifacts/m9-completion-20261126/SHA256SUMS).

## Remaining acceptance

Public footage now exercises a real removal, supported source-region decoding and masked crop generation. It does not supply institution-authorized live camera access, an operational disclosure policy or independently reviewed target-site accuracy. Representative motion, viewpoint, scale and privacy quality remain unaccepted; the template provider does not establish learned or general-purpose ReID quality.

Recovery calibration and operational waiting-time validation require permitted confirmed custody histories plus explicit follow-up/censoring observations, item/institution/time holdouts and reviewed cohort/censoring assumptions. Public activity-video labels do not establish these outcomes. The waiting baseline is an offline diagnostic artifact and has not been activated as a Core prediction service or accepted for operational rollout.

Deployment acceptance requires a selected operational tenant/camera archive, its approved grants and identities, scheduling/retention policy and a target-host/site rehearsal. Tests run an actual isolated worker against local Core and PostgreSQL; they are not an institutional deployment. These inputs and acceptance records cannot be replaced by invented online data. M9 remains open for this evidence and rollout rather than being relabelled complete.
