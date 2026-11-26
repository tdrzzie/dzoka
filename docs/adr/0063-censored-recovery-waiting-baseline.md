# 0063. Transparent censored waiting-time baseline

- Date: 2026-11-26
- Status: Accepted for offline diagnostics

Completed transfers alone cannot measure how long unresolved cases remain in a state. Add an original standard-library Kaplan–Meier product-limit baseline, separately versioned as `recovery-wait-kaplan-meier-v1`. No third-party implementation, runtime dependency or model weight is copied. The statistical method is described by [NIST](https://itl.nist.gov/div898/handbook/apr/section2/apr215.htm).

Purpose-approved rows distinguish a completed transition from an observation that remains without a transition through an explicit time. Keep one observation per item/source-state, source-content digests, recorded times and an as-of cutoff. Require at least three distinct items per supported source state. Compute risk sets before censor withdrawals at tied event times. The model assumes comparable source-state cohorts and non-informative right censoring. It cannot infer why a case was no longer observed. These assumptions need review on operational data.

Return the estimated fraction still without a transition at a horizon within observed follow-up. Unsupported states and extrapolation beyond the last follow-up are unavailable. This waiting estimate is separate from the completed-transition destination frequencies. It has no location, possession or release authority.

Evaluation requires disjoint items, institutions and source evidence, training recording and model cutoff before test observation. A test case censored before the requested horizon has an unknown horizon outcome and remains unscored. Report that count and available-case coverage explicitly. The reported Brier error covers observable horizon outcomes only; it is not an inverse-probability-weighted population score or calibration acceptance. Preserve model/dataset/evaluation hashes and exclusive artifact outputs. No automatic promotion or Core rollout path is added. Existing transition model versions remain intact.
