# Source audit: trajectron-plus-plus

Status: COMPLETE

Scope: architecture-relevant static review of the listed source ranges, license evidence, deployment entry points and tests/evaluation where present. No upstream tests were executed and no upstream implementation was copied into Dzoka. Unreviewed subsystems and integration acceptance work are called out explicitly.

## Repository identity

https://github.com/StanfordASL/Trajectron-plus-plus

## Audited branch

master. The exact commit below is authoritative; branch HEAD may move.

## Exact commit SHA

1031c7bd1a444273af378c1ec1dcca907ba59830

## Audit date

2026-10-26

## License

MIT. Root LICENSE text inspected at the pinned commit: MIT. Dependency, checkpoint and dataset licenses remain separate.

## Files inspected

- [LICENSE](https://github.com/StanfordASL/Trajectron-plus-plus/blob/1031c7bd1a444273af378c1ec1dcca907ba59830/LICENSE), lines 1-21; role: license.
- [trajectron/model/trajectron.py](https://github.com/StanfordASL/Trajectron-plus-plus/blob/1031c7bd1a444273af378c1ec1dcca907ba59830/trajectron/model/trajectron.py), lines 1-65, 129-195; role: implementation.
- [trajectron/model/mgcvae.py](https://github.com/StanfordASL/Trajectron-plus-plus/blob/1031c7bd1a444273af378c1ec1dcca907ba59830/trajectron/model/mgcvae.py), lines 1-12, 510-519, 531-540, 613-622, 656-665, 686-695; role: implementation.
- [trajectron/model/model_registrar.py](https://github.com/StanfordASL/Trajectron-plus-plus/blob/1031c7bd1a444273af378c1ec1dcca907ba59830/trajectron/model/model_registrar.py), lines 1-75; role: implementation.
- [requirements.txt](https://github.com/StanfordASL/Trajectron-plus-plus/blob/1031c7bd1a444273af378c1ec1dcca907ba59830/requirements.txt), lines 1-20; role: deployment.
- [trajectron/test/test_data_structures.py](https://github.com/StanfordASL/Trajectron-plus-plus/blob/1031c7bd1a444273af378c1ec1dcca907ba59830/trajectron/test/test_data_structures.py), lines 1-20, 36-48; role: tests.

## Runtime architecture

Research PyTorch graph-conditioned multimodal trajectory prediction with per-node-type CVAE models.

## Domain model

Scenes, node types, history/future states, neighbors, robot trajectory, maps and predicted trajectory samples.

## Storage model

Dataset/scene structures, hyperparameter configs and model-registrar .pt files, not institutional custody-state transitions.

## AI and matching implementation

Trajectron.predict constructs node history/neighborhood batches and calls a MultimodalGenerativeCVAE for trajectory samples. The registrar loads torch objects from checkpoints; history and future encoders use recurrent representations.

## Security model

Reviewed code is an offline research/library boundary, not an authenticated tenant service. Keep datasets, checkpoints and configuration trusted and isolate execution.

## API design

The inspected entry points are local Python/CLI or tensor calls; no production recovery API or authorization contract is supplied by those paths.

## Event model

No recovery-domain event stream, outbox/inbox or custody lifecycle appears in the reviewed algorithm boundary; these must be supplied by Dzoka.

## Deployment model

Requirements pin old torch 1.4/TensorFlow 1.14/NumPy 1.16-era dependencies. Do not install this environment into the platform runtime without a modernization and compatibility study.

## Test coverage

Inspected array/header indexing tests, not prediction correctness or calibrated recovery routing. They import a data module whose availability must be checked in execution; none run.

## Strengths

Useful probabilistic/history-conditioned prediction research and separation of scene/model state.

## Weaknesses

Continuous trajectory inputs do not represent institutional movements such as bus-to-driver-to-depot. Old full-object checkpoint loading and dated dependencies limit direct deployment.

## Security concerns

Untrusted checkpoints/serialized features and expensive media/tensor operations need a restricted worker boundary. No application tenant/ownership controls are inferred from a research implementation.

## Scalability concerns

Scene-graph neighborhood size, prediction horizon and sample count drive compute; this is not evidence for scalable cross-institution recovery prediction.

## Licensing consequences

Preserve applicable license/copyright/NOTICE obligations for future permitted reuse. Code licensing does not establish permission for every model weight or dataset. No upstream implementation was incorporated into the core.

## Reusable components

Reference uncertainty and historical-conditioning concepts; build RecoveryTransitionModel on confirmed institutional event transitions.

## Do not reuse

Do not substitute x/y trajectory models for custody-state prediction, load untrusted torch objects or reuse private person trajectories for recovery training without authorization.

## Architecture lessons

The target variable must match the domain: predict next custodian/location state from confirmed recoveries, not generic agent motion.

## Disposition

REFERENCE. Applies to this declared integration scope. It is not a claim of production readiness, blanket source reuse approval or completion of later integration acceptance tests.

## ADRs triggered

Proposed investigations: authorized optional edge processing; state-transition recovery intelligence versus continuous trajectory prediction. This audit supplies evidence for later technical ADRs; it does not finalize the architecture.

## Unresolved questions

Model weights and dataset terms, dependency versions, resource budgets, adversarial cases and accuracy on physical-instance recovery data require integration-specific validation.
