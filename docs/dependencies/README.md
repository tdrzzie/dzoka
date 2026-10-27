# Runtime adoption records

- [Foundation](foundation-adoption.md): Java/Maven, Spring/security, PostgreSQL/JDBC, Flyway, tests and initial CI runtimes.
- [Intake and private media](intake-media-adoption.md): AWS S3 SDK, private S3 test server, and restricted raster decoder runtime.
- [Local text inference](ai-text-adoption.md): separately locked Python/native wheels, pinned multilingual model/tokenizer and private leased consumer.
- [Local perception](perception-adoption.md): exact licensed ResNet/Paddle/Wav2Vec2 artifacts/dictionaries on the existing CPU runtime, plus independently authored classical local features; original/export terms retained separately.
- [Joint/region profiles](joint-vision-adoption.md): official Apache-2.0 Jina CLIP v1 paired exports and Model Zoo SSD/FCN artifacts with both declared Apache-2.0/MIT obligations, exact immutable hashes and bounded processing.

- [Structured attributes, translation and reranking](language-adoption.md): task-specific Apache-2.0 QA/Marian/official cross-encoder exports, independent native conformance and retained negative FLAN findings.

- [General drafts and multilingual profiles](generative-adoption.md): Apache-2.0 official SmolLM2, explicitly MIT M2M100 export/base and separately reviewed SentencePiece wheel with preserved bundled notices, in a separate 22-wheel environment.

- [Language quality and Shona](language-quality-adoption.md): official larger SmolLM2 and explicit Apache-2.0 dedicated Shona/Spanish/German-to-English ONNX exports on the unchanged separate 22-wheel runtime.

- [Bidirectional Shona and explicit span preservation](shona-adoption.md): Apache-2.0 int8 exports plus a separately reviewed 23-wheel CTranslate2 environment. Bundled vendor terms limit this adoption to local development; production/SaaS/distribution remains a separate gate.

The [artifact lock](../../dependencies.lock.json) records the combined application/test graph and explicit license choices. Dependency hashes and license declarations are not a complete source audit or vulnerability certification. New runtime versions, providers, model weights and production distributions require their own bounded adoption decision before installation.

- [CPU Shona and unmarked proposals](shona-cpu-adoption.md): pinned MIT CTranslate2/OpenBLAS source build, exact Debian closure, unchanged 22-wheel host and Apache-2.0-plus-notices GlotLID. Separate service-license-compatible profiles; deployment and language acceptance remain required.
