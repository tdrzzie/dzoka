# 0036: Supervise private AI workers with bounded polling

- Status: Accepted scoped implementation
- Date: 2026-11-01

## Decision

After the mandatory audit gate, add a standard-library-only local supervisor for existing text, perception, vision, language, generative and quality one-job workers. No new runtime dependency, model, profile key or database migration is adopted. Startup-bound source/lock digests reject silent worker hot reload. Explicit immutable-at-start plans name a core endpoint, tenant/profile assignments, local model directories and environment-variable references to service tokens. Never accept arbitrary executable paths/arguments, secret values in plans, or inferred tenant/model defaults.

Use serial round-robin polling with per-assignment capped exponential failure cooldown, bounded successful polling and existing 110/290-second worker deadlines below core leases. An invocation exit of zero means only successful invocation (possibly no eligible work), never a completed-job count. The core remains authoritative for leases, attempts, retry eligibility, idempotent completion, authorization and holds/erasure. No broker publication, queue inspection, job creation, model download or automatic activation is introduced.

A small child bootstrap waits for parent permission before starting a fixed reviewed launcher. On Windows assign it to an unnamed kill-on-close Job Object before permission; normal cleanup, deadline or parent death closes the job and terminates its inherited process tree. No breakaway permission. On POSIX use a new process group and terminate that owned group on cleanup. Failure to establish ownership fails closed before worker launch. Graceful stop stops new invocations and waits for the active bounded invocation. Child output is discarded; logs expose counts/status only, not input text, endpoints, tokens, tenant/profile IDs or arbitrary exceptions.

Shona vendor profiles remain manually invoked local-development-only; CPU Shona also remains manual until its Docker-daemon container lifecycle is integrated with supervisor ownership. Killing host processes alone cannot prove daemon-side cleanup. This explicit exclusion prevents claiming coverage of every provider. Serial head-of-line delay, horizontal fairness, production service management, CPU/memory/egress isolation and Kafka integration remain separate work.

## Evidence and consequences

[ADR 0011](0011-ai-provider-ports.md), [ADR 0012](0012-asynchronous-ai.md) and the governing baseline preserve provider and authority boundaries. Microsoft [Job Objects](https://learn.microsoft.com/en-us/windows/win32/procthread/job-objects) and [extended limits](https://learn.microsoft.com/en-us/windows/win32/api/winnt/ns-winnt-jobobject_extended_limit_information) document the built-in platform API used by an independent ctypes declaration; no upstream implementation is copied. POSIX process-group behavior requires separate host execution before claiming portability. Test actual descendant termination, startup failure, privacy/config rejection, backoff/fairness/stop behavior and real authenticated private jobs. This is local polling, not complete M4 or production orchestration.
