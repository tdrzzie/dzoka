# M4 live reference routing and installed service verification

The existing OpenRouter credential authenticated successfully on November 15. The private connection is enabled with `qwen/qwen3-235b-a22b-2507`, explicit provider `Nebius`, a 512-token output cap, disabled provider fallback, denied training and requested zero retention. The provider endpoint listed $0.20 per million input tokens and $0.60 per million output tokens. The original 30B route was superseded after its greeting diagnostic returned unsuitable Shona. The 235B route returned `Mhoro, dziko!` for the user-approved public `Hello, world!` test. Both model and provider were checked against the configured identities. [Provider prices](https://openrouter.ai/api/v1/models/qwen/qwen3-235b-a22b-2507/endpoints) · [Provider policies](https://openrouter.ai/providers)

The first combined enable/test request was rejected by automatic approval review because the authored loss/location payload had unclear sensitivity. It did not run. Configuration was enabled separately, and the user explicitly approved the public greeting request. Subsequent corpus inputs were hash-verified, previously licensed public FLORES-200 references. No tenant source or private ownership evidence was used.

## Public Shona reference results

The selected hosted route translated the same development rows 65–72 and additional rows 81–88 from the pinned FLORES-200 Shona and English members. The selection and diagnostic screen were recorded before inference. Every request had its own exclusive attempt and result files. [Existing data license and hash review](../dependencies/public-reference-data-review.md)

| Slice | Complete | Median CER | Worst CER |
|---|---:|---:|---:|
| Development | 8/8 | 55.6% | 93.2% |
| Additional | 8/8 | 69.1% | 95.3% |

These results fail the existing 25% median and 60% per-case diagnostic ceilings. Exact reference character agreement cannot independently establish semantic accuracy. The hosted route is enabled for public reference work and has no Shona rollout approval. The provider-reported identifier does not attest an immutable checkpoint digest. Public data may overlap model training.

Private summary `C:\Users\nyash\DzokaOpenRouterFlores-20261115-v1\summary.json` has SHA-256 `448fba9706562297af446741c7797d8be444f548e41c8441ec021f2207fa1b9d`. The 30B smoke receipt is `0caf76761435c4de9dc29cd849a9bd83f05cc4aa8ce88e697506820afdc2a444`; the 235B smoke receipt is `aad4322122e8f7d40e9517c966fa8c7e26a72aab3d5ace7767394820ead5bdee`. Keys and raw corpus data remain outside Git.

## Installed adapter and account

Windows UAC elevation succeeded for the read-only SCM probe. A temporary demand-start `DzokaWorkers` service then used a protected staging directory, the actual byte-checked `windows_service.py` adapter and `service_account.py`, and a small acceptance host. This host exercised the SCM dispatcher and controls without polling core jobs. The service ran as `NT SERVICE\DzokaWorkers` and was removed after the checks. The private staging and failed-run evidence were retained.

The first staging attempt assigned the virtual SID before registering the service, and Windows rejected the unmapped SID. The next attempt registered the account first and completed start, stop and crash restart, but the token's elevation flag triggered the existing policy. Setting SCM required privileges to `SeChangeNotifyPrivilege` yielded a non-elevated, non-administrator token with the exact service SID and declared high integrity RID 12288. Three launches, normal stop and automatic restart after exit code 7 passed. No account-policy check was relaxed. [Windows service privileges](https://learn.microsoft.com/en-us/windows/win32/services/service-security-and-access-rights)

The actual Docker executable is in the interactive user's per-user installation. An initial diagnostic incorrectly assumed a machine-wide executable location and recorded `FileNotFoundError`. The corrected native test opened the exact `dockerDesktopLinuxEngine` named pipe from the restricted service token and received Win32 error 5. The pipe and shared Docker configuration were unchanged. This leaves machine-wide daemon provisioning and access, full core/inference under the installed service, core egress policy and boot-before-login verification open. [Docker Windows permissions](https://docs.docker.com/desktop/setup/install/windows-permission-requirements/)

| Receipt | SHA-256 |
|---|---|
| Initial staging failure | `fdce387f00106fa22886565e741b603c24edbf9edd3141a39a659a8f6005a7e0` |
| Initial token policy rejection | `5704f8f986836bcc8c077e8a46d098aa0d366760975845f1fd8f8fd77581dd2d` |
| Restricted-account lifecycle pass | `a04ee2003865af1fca9ce6abb07152c99d2e430465b9e3e6426e251ce54b7720` |
| Lifecycle pass and exact daemon denial | `51fdee70db9cbf7214e32bc958d94eb24a2940155cf6f5b694c5ddebf41bfb0f` |

The source and model identities for the passing November 8 24-phase gate still match the current implementation. Its 169 Python, 24 Java unit and 95 integration tests and exact 35-table/3,189-row restore remain the applicable full local evidence. November 15 adds live provider and installed adapter evidence. Full M4 acceptance remains open for the recorded model-quality and complete deployment requirements.
