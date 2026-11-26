# 0059. Authorized edge observations and recovery transitions

- Date: 2026-11-26
- Status: Implementing the local authorization and observation profile

Use PostgreSQL for investigation grants and bounded worker observations. An independent grant reviewer authorizes one camera and report, a time window, expiry, frame budget and protected evidence digest. A worker claims a lease and publishes only timestamped item sightings or explicit gaps. Each operation rechecks grant and report availability. Observations retain source and processor digests and uncertainty. No raw frames or person identifiers enter the API.

Read confirmed custody history directly from native records as the initial recovery graph projection. Return observed edges separately from descriptive transition counts. The count baseline uses only permitted, completed observations available before the requested cutoff and excludes the target intake. It exposes support counts and an unavailable result for sparse history. It has no release authority.

The portable edge provider now consumes registered frame archives under the [bounded runtime contract](0060-portable-edge-frame-runtime.md). It binds camera, reference appearance, ROI and purpose to a reviewer-approved plan and sends only observations and item crops. Crops require independent review before investigator access.

Use a live SQL graph projection over native custody facts. This avoids a separate stale materialization and makes source availability and temporal cutoff checks apply on each query. Replaying the query rebuilds the same authorized view without generating actions. Baselines count distinct source intakes and expose median observed elapsed time. The standalone transition model retains dataset/configuration identity and rejects evaluation splits that overlap item, institution or chronological scopes.
