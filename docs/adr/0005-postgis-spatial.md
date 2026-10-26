# 0005: Use PostGIS for spatial reasoning

- Status: Accepted design; implementation and runtime dependency approval remain separate
- Date: 2026-10-26
- Prerequisite: mandatory source audit gate passed before these decisions

## Context

The user requires PostGIS and uncertain location/time reasoning. The mandatory audits do not independently review a PostGIS implementation.

## Decision

Use PostGIS through a SpatialQueryProvider for coordinate/geofence and distance retrieval. Retain original place text, resolved place reference, geometry, CRS, uncertainty region and provenance. Use appropriate geographic distance semantics; never treat unknown location as zero distance.

## Alternatives

Text equality loses aliases and uncertainty; a new graph/geospatial database is unnecessary before workload evidence.

## Consequences

This accepts the requested design, not an unaudited extension binary. Exact PostgreSQL/PostGIS compatibility, license and spatial query behavior remain pre-install review items.

## Source-audit evidence

The specification and [AGENTS.md](../../AGENTS.md) govern invariants and required stack preferences. These static reviews inform the decision; they do not establish benchmarks or full dependency certification.

- [globalenglish-lostfound](../source-audit/platforms/globalenglish-lostfound.md) — `6ff8cffe55bfc3e7274124512a69fae746ceb643`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [openepcis](../source-audit/traceability/openepcis.md) — `e81a857771c4c4a2d12e62b60a2dbc094551aa6e`; inspected ranges and limitations are in the canonical audit and evidence manifest.

## Implementation implications

Test aliases, coordinate order, boundary cases, uncertain regions, absent positions, distance units and impossible chronology without conflating uncertainty with conflict.

The [architecture baseline](../architecture/baseline.md) defines ownership and execution boundaries. Complete the relevant [dependency adoption review](../architecture/dependency-adoption.md) before installing runtime dependencies. Acceptance scenarios here are requirements, not tests already executed.
