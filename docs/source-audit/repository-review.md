# Existing repository review

Inspection date: 2026-10-26. See [inventory](repository-inventory.json) for supplied file hashes, sizes, line counts and headings. The inventory captures the supplied tree before generated audit/governance reports. It is a structural inventory plus targeted document review, not a claim that every paragraph of research was manually verified.

The initial Git branch had no commits. All supplied files were untracked. There was no README, AGENTS, STATUS, ADR directory, build manifest, runtime source, migration, test suite or CI pipeline. Nothing was reset, staged or committed.

Supplied material includes the exact specification in `docs/dzoka.md`, broad architecture/design proposals, six architecture documents, five workflow/domain design documents, ten platform research documents and five large findings catalogues. `starter` is a proposed audit-directory tree. The ten platform documents describe nine unique sources because La2eet and Malak LostAndFound share one repository and revision.

The source-audit README describes completed research and preferred components but had no lockfile or canonical per-source records beneath it. Those claims are not reproducible approval evidence by themselves. The current status notice, canonical index and evidence manifests distinguish fresh source inspection from supplied assertions.

The supplied `ARCHITECTURE.png` was visually inspected. It depicts client, institution, edge and federation inputs; an API gateway; a domain service grid; asynchronous workers; Kafka-compatible events; data stores; supporting outbox/inbox/replay services; and observability. It labels Qdrant as primary with pgvector as fallback and refers to domain microservices. The current specification instead requires pgvector initially and a modular monolith for the transactional core. Neither drawing labels nor existing prose override the current requirement; final topology must follow completed audits and ADRs. The picture is retained unchanged.

Existing prose is useful target design material, not evidence that the pictured capabilities exist. No application feature was found or marked complete. The entire feature scope is retained in [requirements coverage](../requirements/coverage.md).
