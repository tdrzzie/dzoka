# 0039: Check service identity and prove an offline Windows boundary

- Status: Accepted scoped implementation
- Date: 2026-11-01

## Context and decision

The thirty-source gate passed before feature work. ADRs 0037–0038 retain same-account/network privilege and terminal-journal capacity gaps. Use existing Windows APIs and reviewed local Python bytes without new packages, images, models or inference-profile changes.

Add explicit service configuration v2 with an expected account SID and explicit integrity RID. Check the actual token and require both matches; reject shared built-in service identities and enabled Administrators/elevation. MIC level is distinct from account privileges: SCM may supply system integrity for a non-administrator account, so it must be explicitly declared rather than inferred as administrator membership. SCM activation requires v2; v1 remains a documented foreground development compatibility path. This validates identity, not password provisioning, service logon rights or daemon ACL policy.

Implement a separately keyed, no-capability AppContainer pilot using a private byte-recorded copy of the adopted Python host and fixed standard-library probe. Attach a bounded kill-on-close Job Object atomically through the process attribute job list at creation, then launch suspended, verify the actual package token and zero capabilities, verify job membership before resume, inherit only explicit standard handles and an environment without service credentials. Grant read/execute only to its own fixture runtime; do not grant network capabilities, loopback exemptions, Docker access or broad host file permissions. Journal exact per-user profile creation and remove/reconcile only inactive owned profiles. Real socket/private-file denial is required; configuration alone is not acceptance. Loopback requires a native no-exemption check and receiver controls; host-interface failure additionally requires native missing-capability diagnostics. A timeout alone is not acceptance. Existing networked model workers are not silently routed through this pilot: their private HTTP and guardian loopback transports need a reviewed broker/new inference profiles before adoption.

Add recoverable archival of confirmed terminal Docker journals under owner locks. Preserve exact history bytes and a hash receipt before removing an active-directory copy; retries verify both sides. Refuse active, unresolved, malformed, reparse or conflicting records. Never archive/discard uncertainty or prune Docker. Archive integrity is not a cryptographic signature or backup/HA substitute.

## Evidence and alternatives

Microsoft documents [AppContainer launch and dual-principal access](https://learn.microsoft.com/en-us/windows/win32/secauthz/implementing-an-appcontainer), [process attributes](https://learn.microsoft.com/en-us/windows/win32/api/processthreadsapi/nf-processthreadsapi-updateprocthreadattribute) and [per-user profile deletion](https://learn.microsoft.com/en-us/windows/win32/api/userenv/nf-userenv-deleteappcontainerprofile). Native ABI declarations are independently authored. Machine firewall/account installation requires administrator access unavailable here; a restricted ordinary token alone does not establish network isolation. A container/VM host migration needs its own runtime/provider adoption and provenance.

## Consequences and acceptance

Run actual positive/negative identity checks, AppContainer token/capability readback, TCP loopback and host-interface denial against reachable task-owned listeners, private-file denial, process termination/profile recovery and archive conflict/retry/active-owner integrity cases. Standard-library execution is an isolation pilot, not native-model or all-host sandbox acceptance. Full worker broker integration, dedicated-account/daemon/SCM activation, egress policy for networked clients, daemon/host loss, archival storage/backup policy, load and representative model acceptance remain open. No AI ownership/release authority changes.
