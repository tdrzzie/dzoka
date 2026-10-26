# Contributing

Read [AGENTS.md](AGENTS.md), [STATUS.md](STATUS.md) and the source audit index before making changes. Keep changes scoped and independently reviewable. Preserve original research and attribution.

For audit contributions, pin the inspected commit, identify the branch and source files, verify the actual license text, document both implementation and limitations, and update the registry. Complete every applicable section; use an explicit scope limitation rather than guessing. License uncertainty forbids source reuse but does not prevent documenting the uncertainty.

Run `python tools/audit_registry.py check` and `python -m unittest discover -s tests -v`. Run `python tools/audit_registry.py gate` before proposing architecture finalization or application implementation; all 30 required records are now complete and the gate is mandatory in CI. Include relevant checks and unresolved limitations in change descriptions. Never submit production secrets or personal recovery records.

For foundation changes, verify the dependency lock and run the real PostgreSQL/JWT integration harness described in [the run instructions](docs/development/foundation.md). Do not substitute an in-memory database or mocked decoder for tenant/authentication evidence. New runtime artifacts require adoption review before updating `dependencies.lock.json`.
