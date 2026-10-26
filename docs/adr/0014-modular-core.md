# 0014: Use a modular Java transactional core

- Status: Accepted design; implementation and runtime dependency approval remain separate
- Date: 2026-10-26
- Prerequisite: mandatory source audit gate passed before these decisions

## Context

No audited application implements the complete recovery network; the user requests Java 21+ and Spring Boot with explicit domains.

## Decision

Use one Java/Spring deployable core with package-owned domain, application, ports and adapters. Domains call public application interfaces, not other repositories. Keep pure domain rules independent of Spring, JPA and vendor SDKs. Worker roles can run the same codebase with distinct credentials and scaling.

## Alternatives

A microservice per entity creates distributed transactions; a single undifferentiated CRUD service loses authority boundaries.

## Consequences

Enforce acyclic module dependencies and aggregate ownership in the first code milestone. Java/Spring exact versions, build tool and dependency licenses require targeted review before adding build dependencies.

## Source-audit evidence

The specification and [AGENTS.md](../../AGENTS.md) govern invariants and required stack preferences. These static reviews inform the decision; they do not establish benchmarks or full dependency certification.

- [lost-and-found-tracking-system](../source-audit/platforms/lost-and-found-tracking-system.md) — `4ef1493f80590f6fa03d2ad9eded2e70a59ebe58`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [malak-lostandfound](../source-audit/platforms/malak-lostandfound.md) — `d5d7bfaf043396a32b40c264c647f6456cadb633`; inspected ranges and limitations are in the canonical audit and evidence manifest.
- [openepcis](../source-audit/traceability/openepcis.md) — `e81a857771c4c4a2d12e62b60a2dbc094551aa6e`; inspected ranges and limitations are in the canonical audit and evidence manifest.

## Implementation implications

Architecture checks reject forbidden module dependencies; transaction tests prove critical recovery invariants and module APIs reauthorize commands.

The [architecture baseline](../architecture/baseline.md) defines ownership and execution boundaries. Complete the relevant [dependency adoption review](../architecture/dependency-adoption.md) before installing runtime dependencies. Acceptance scenarios here are requirements, not tests already executed.
