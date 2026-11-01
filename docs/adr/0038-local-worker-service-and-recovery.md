# 0038: Host bounded workers as an explicit Windows service

- Status: Accepted scoped implementation
- Date: 2026-11-01

## Context

Specification sections 70–73 require explicit deployment configuration, safe startup and recoverability. ADRs 0011–0012 preserve private replaceable asynchronous inference; ADRs 0036–0037 delivered local process/daemon ownership without installed-service acceptance. The mandatory thirty-source gate passed before this feature work. Existing adopted runtime/model licenses and profile identities remain governing; no additional upstream source is copied or installed.

## Decision

Extend ADRs 0036–0037 with a standard-library Windows SCM adapter and the same foreground service lifecycle. No new package, inference identity, model or authority is introduced. Before polling, verify source identities, private deployment/credential/state ACLs and non-writable release/interpreter/runtime trees, reject reparse paths, acquire an exclusive instance lock, establish inherited Windows job memory/process/CPU limits and reconcile every configured Docker state directory. Unresolved intents prevent polling. Retain journals across restarts; never prune Docker or discard uncertainty.

Use a separate service manifest that pins the existing supervisor lock and service implementation. Snapshot a protected credential file containing exactly the plan's environment references. Do not use machine-wide token variables or place secrets in service arguments/status. Stop requests finish the bounded active invocation and prevent further work. Status is an atomic redacted lifecycle snapshot, not job health, and a process lock, not heartbeat age, decides active ownership. Service failures exit nonzero for SCM recovery policy. No automatic install, privileged account creation, global ACL edits or shared-daemon restart occurs.

The service process and all descendants inherit an outer kill-on-close Job Object with fixed 8 GiB committed-memory, 128-process and 50% host CPU limits. Existing inner worker jobs and Docker limits remain. Whole-service death can also kill the guardian; startup reconciliation is therefore mandatory. This bounds resources, not host egress or account privileges. Same-account Docker access remains a privileged boundary; use an accountable dedicated non-administrator identity and reviewed daemon access before production promotion.

## Evidence and limitations

Microsoft's [service dispatcher](https://learn.microsoft.com/en-us/windows/win32/desktop/services/writing-a-service-program-s-main-function), [Job Objects](https://learn.microsoft.com/en-us/windows/win32/procthread/job-objects) and [CPU control](https://learn.microsoft.com/en-us/windows/win32/api/winnt/ns-winnt-jobobject_cpu_rate_control_information) define the independently declared native APIs. Test actual private ACL rejection/acceptance, kernel limit readback, process exclusivity/crash restart, redacted status, stop ordering and Docker recovery after owner/guardian loss. SCM installation needs an administrator and a target service identity; foreground execution does not establish SCM boot, logon or recovery-policy acceptance. The shared writable development checkout must fail deployment preflight. Full M4, production deployment, HA, egress/account separation and quality acceptance remain incomplete.

## Alternatives

A third-party Windows service wrapper would add another runtime/license/artifact adoption and control boundary. Scheduled tasks and an interactive continuous launcher have different control/status contracts; they remain operator options outside the SCM adapter implemented here. Linux service/container orchestration requires a separately reviewed portable host profile and execution evidence. The built-in SCM/Job/NTFS APIs allow this bounded Windows implementation without a new library or image.

## Consequences and implementation implications

Operators must provision protected release/runtime/configuration/state storage and a dedicated identity; preflight cannot supply missing administrator rights, secrets, policy or host isolation. The outer process job means losing the whole service also loses its guardian, so retained exact-ownership journals and startup reconciliation are mandatory. Runtime-tree scans and fixed capacity limits need target load/operational acceptance; they are not an availability or quality SLO. Model/core authority and prior inference provenance remain unchanged. The [execution record](../testing/m4-worker-service-2026-11-01.md) distinguishes real foreground/kernel/Docker/core tests, injected faults and unexecuted SCM/production paths.
