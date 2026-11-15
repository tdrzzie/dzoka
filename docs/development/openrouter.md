# Optional OpenRouter public-reference connection

The user requested this optional connection for Dzoka. It is independent of local model inference and is disabled by default. It currently accepts public-reference text translation only, not tenant reports, secret ownership evidence, private contact details, uploaded media or core jobs. No runtime dependency is added. [ADR 0043](../adr/0043-versioned-quality-refinements-and-optional-reference-api.md) records the boundary.

On November 15 the local key authenticated successfully and the private connection was enabled with `qwen/qwen3-235b-a22b-2507`, provider allowlist `["Nebius"]` and a 512-token output limit. The provider endpoint listed $0.20 per million input tokens and $0.60 per million output tokens. Live calls confirmed both model and provider identities. The earlier 30B candidate returned unsuitable Shona on the approved greeting diagnostic. The 235B candidate returned `Mhoro, dziko!` for `Hello, world!`. Both receipts remain private. General language quality acceptance requires the separately recorded reference comparison. [Current provider endpoints](https://openrouter.ai/api/v1/models/qwen/qwen3-235b-a22b-2507/endpoints)

Run `python -B tools/openrouter_connection.py setup` once. On this Windows account the files are:

- `C:\Users\nyash\DzokaOpenRouter\credentials.json`: replace only the empty `apiKey` value with your OpenRouter API key.
- `C:\Users\nyash\DzokaOpenRouter\settings.json`: explicit model/provider routing and output budget. Keep `enabled: false` until choosing these settings.

Both files and their directory have owner/SYSTEM/Administrators-only ACLs and remain outside Git. Setup refuses an existing directory rather than overwriting credentials. Do not paste the key into chat or source files. Create/manage it on the [OpenRouter key page](https://openrouter.ai/settings/keys); set an account/key spend limit suitable for the intended use.

`python -B tools/openrouter_connection.py check` makes an authenticated metadata request and prints authentication status without the key, account label or provider body. It works while inference is disabled. `models` lists model identifiers from the bounded catalog. The November 15 authentication and translation calls provide live evidence in addition to the five mocked transport tests.

For an explicitly approved public-reference comparison, set an exact model ID and provider allowlist in settings, then enable it. Choose a model ID that matches the returned identity; unexpected models/providers fail. Place a bounded reference envelope in an owner-private directory:

```json
{
  "classification": "PUBLIC_REFERENCE",
  "task": "TRANSLATION",
  "sourceLanguage": "en",
  "targetLanguage": "fr",
  "text": "Your permitted public reference text",
  "sourceReference": "https://publisher.example/reference",
  "sourceSha256": "<sha256 of exact UTF-8 text>"
}
```

Run `python -B tools/openrouter_connection.py reference --input ABSOLUTE_PRIVATE_ENVELOPE --output NEW_ABSOLUTE_PRIVATE_RESULT`. Public classification is an operator declaration, not automatic permission/PII certification. A durable exclusive attempt file is reserved before a possible paid request; uncertainty is retained and rerunning the same output cannot silently repeat a charge. Results are new private files with source/config hashes, configured/returned provider/model, request identity, timestamps and supplied usage. Raw input remains in the separate envelope. No automatic retry/fallback or model approval follows; output is UNVERIFIED_TRANSLATION and approval NOT_ESTABLISHED.

The returned model string is provider-reported identity. An exact hosted checkpoint digest/server build is unavailable through this client and must not be inferred from a matching model ID; aliases can change behind that identifier. These results do not establish immutable hosted-weight compatibility with local embedding spaces or approved production profiles.

Requests use the fixed official HTTPS origin, no environment proxies or redirects, explicit providers, no fallback, deny-training and per-request ZDR preferences. The [official authentication](https://openrouter.ai/docs/api/reference/authentication), [provider routing](https://openrouter.ai/docs/guides/routing/provider-selection) and [ZDR policy](https://openrouter.ai/docs/guides/features/zdr) were inspected on 2026-11-06. ZDR preferences do not establish an absolute no-retention guarantee: account/guardrail safety-retention settings and documented safety/legal exceptions apply. No private-data egress approval or institutional provider agreement is inferred. OpenRouter's hosted model terms are separate from licenses for self-hosted checkpoints.
