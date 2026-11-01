# Account, offline egress and recovery boundary

[ADR 0039](../adr/0039-offline-account-boundary-and-journal-archive.md) extends the [service host](worker-service.md). No model, image, runtime library, inference source/profile key or database migration changes. Existing API workers remain networked; the new AppContainer path is a fixed standard-library isolation pilot, not a production model launcher.

## Checked service identity

Use service schema v2 with an operator-provisioned `accountSid` and `integrityRid`. The actual process token must match both. Shared LocalSystem/LocalService/NetworkService identities, enabled Administrators and elevation are rejected. Supported declared SID forms are local/domain user and virtual-service identities. Low/medium/high/system MIC are explicit allowed RIDs; Windows service MIC is distinct from account privileges. A non-administrator system-integrity token is accepted only when that level was explicitly declared. This does not create an account, grant logon rights, inspect all privileges or certify daemon access. SCM mode refuses legacy v1; existing v1 foreground behavior is retained as a development compatibility path.

A dedicated identity, required-privilege/daemon ACL review, administrator access for registration, protected release/runtime/state provisioning and real service-logon/control/boot tests remain prerequisites. Current SCM create-service access is denied; no machine account/firewall/service installation is performed by these tools.

## Offline AppContainer pilot

`python tools/verify_worker_boundary.py` stages byte-checked copies of the adopted Python host and its license/stdlib/DLLs in a task-owned private user-profile directory. Its fixed probe has no model, core JWT, DB/S3 credential, proxy environment or download path. It creates a unique per-user AppContainer profile with zero capabilities and no loopback exemption; only the fixture runtime gets package-SID read/execute access. Windows dual-principal access then independently denies a private sibling file despite the same underlying user SID.

The launcher explicitly inherits two standard I/O handles only. It attaches an unnamed kill-on-close Job Object **at process creation** using the Windows job-list attribute, starts suspended, reads back the actual package SID/zero capabilities/job membership, then resumes. There is no create-then-assign parent-crash gap. The pilot is bounded to eight Windows processes, 512 MiB committed memory, a finite request/response and a deadline. The fixed script computes a SHA-256 over exact Unicode/NFD/astral UTF-8 input. These bounds are a pilot capacity profile, not a model sizing, hostile-code containment or load benchmark.

Egress tests use only task-owned listeners. The parent connects before and after the child attempts, drains each baseline connection and checks no child connection arrived. Host-interface failures require the Windows missing-capability diagnostic; loopback diagnostics exclude that scope, so the test independently verifies the package has no loopback exemption, its actual token is capability-free, and paired receiver controls pass. A client timeout alone never passes. The recorded `directOrCapabilityDenial` can be false for loopback while the separate no-exemption/receiver checks pass; it must not be reported as a Winsock permission error. IPv4 loopback and a local host interface are observed on this host. IPv6/UDP/DNS/public-internet and hostile-kernel escape acceptance are not established. No policy, exemption or global firewall setting is modified.

Existing model workers need private core HTTP and CPU Shona guardian loopback IPC. They cannot simply run inside the zero-network pilot. A reviewed credential-holding broker, bounded anonymous-pipe request/result contract, sanitized input staging, model/runtime packaging, per-tenant authorization and separately keyed inference adoption remain local implementation before that boundary protects model execution. Ordinary networked workers still share the service's host account/network rights. Existing Linux CPU inference children retain their independently verified Docker network-none restrictions.

## Profile recovery

```powershell
python tools/appcontainer_boundary.py --recover-state ABSOLUTE_PROTECTED_PROFILE_STATE
```

Use the same Windows user that owns the profiles. Recovery checks exact version/account/name/UUID metadata in the selected protected directory, skips active locks, and removes only that user's exact known-created owned profiles. A mismatched account, malformed/reparse record or unresolved creation blocks acceptance. An interrupted INTENT is retained as unresolved even if deletion currently reports absence; no unknown-outcome bypass is offered. Confirmed cleanup is idempotent. No global profile enumeration/prune occurs.

The native fixture retains its private staging directory and ownership metadata on failure. Reconcile its printed `profiles` path before any manual cleanup; do not erase uncertain intents to resume a test. Successful fixtures remove only their verified task-owned staging paths after resource cleanup, and retain terminal diagnostic records under ignored `.test-tmp`. Native parent-death tests observe the exact child process terminating after its owner dies, then reconcile its abandoned profile. This does not establish host-power-loss, profile-store corruption or another user's recovery.

## Terminal Docker journal archival

```powershell
python tools/journal_archive.py archive --state ABSOLUTE_PROTECTED_DOCKER_STATE
python tools/journal_archive.py verify --state ABSOLUTE_PROTECTED_DOCKER_STATE
```

Archival is a separate explicit operator action with no Docker calls. It acquires an exclusive archiver lock and each inactive owner's lock, validates the current approved CPU image/name/record shape and moves only confirmed REMOVED journals out of the active glob. Active records are skipped; uncertain/nonterminal records are retained. It writes exact original bytes to `archive`, flushes them, writes and flushes a hash/length/name receipt, rechecks the original and archive, then removes only the duplicate active-directory copy. All lifecycle history bytes remain unchanged. No model, tenant, input text or secret is added.

A crash after receipt creation leaves both copies; retry verifies identical bytes and acknowledges once. A partial/conflicting copy, corrupt/missing receipt, malformed record or capacity violation stops archival and preserves source evidence for investigation. Never overwrite or delete a conflicting archive to force a pass. `verify` checks every archive journal/receipt pair; it is not signing, tamper-proof storage, backup replication or a power-loss durability certificate. Active-directory capacity remains 10,000; archive capacity is bounded to 100,000 journal/receipt pairs. Rotation/storage/ACL/backup/retention policy and coordinated restore are operator/local engineering work; no history is automatically expired.

See the [execution record](../testing/m4-account-egress-recovery-2026-11-01.md) for accepted source hashes, actual native observations, rejected preliminary trials and remaining work.
