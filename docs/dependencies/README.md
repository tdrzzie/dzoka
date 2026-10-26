# Runtime adoption records

- [Foundation](foundation-adoption.md): Java/Maven, Spring/security, PostgreSQL/JDBC, Flyway, tests and initial CI runtimes.
- [Intake and private media](intake-media-adoption.md): AWS S3 SDK, private S3 test server, and restricted raster decoder runtime.
- [Local text inference](ai-text-adoption.md): separately locked Python/native wheels, pinned multilingual model/tokenizer and private leased consumer.

The [artifact lock](../../dependencies.lock.json) records the combined application/test graph and explicit license choices. Dependency hashes and license declarations are not a complete source audit or vulnerability certification. New runtime versions, providers, model weights and production distributions require their own bounded adoption decision before installation.
