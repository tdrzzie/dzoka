# Runtime adoption records

- [Foundation](foundation-adoption.md): Java/Maven, Spring/security, PostgreSQL/JDBC, Flyway, tests and initial CI runtimes.
- [Intake and private media](intake-media-adoption.md): AWS S3 SDK, private S3 test server, and restricted raster decoder runtime.
- [Local text inference](ai-text-adoption.md): separately locked Python/native wheels, pinned multilingual model/tokenizer and private leased consumer.
- [Local perception](perception-adoption.md): exact licensed ResNet/Paddle/Wav2Vec2 artifacts/dictionaries on the existing CPU runtime, plus independently authored classical local features; original/export terms retained separately.
- [Joint/region profiles](joint-vision-adoption.md): official Apache-2.0 Jina CLIP v1 paired exports and Model Zoo SSD/FCN artifacts with both declared Apache-2.0/MIT obligations, exact immutable hashes and bounded processing.

The [artifact lock](../../dependencies.lock.json) records the combined application/test graph and explicit license choices. Dependency hashes and license declarations are not a complete source audit or vulnerability certification. New runtime versions, providers, model weights and production distributions require their own bounded adoption decision before installation.
