# M5 retrieval, evidence scoring and lifecycle verification

The exact pool remains the reference implementation. In real PostgreSQL and HTTP tests, the system crossed the 512-report bound and returned candidates from independent lexical, spatial/time, category, protected-identifier and active vector routes. The same-category negative with no comparable evidence remained unscored. Category filtering excluded the wrong category and a foreign tenant. The vector path was exercised with native exact, pgvector exact, pgvector HNSW and Qdrant. Local calibration approval was tested separately from the offline evaluator.

An actual pinned joint image/text run used sanitized media and an explicit active rollout, and matched a text query against the image. Shadow evidence remained unavailable. A verified serial contradiction held even when image/text evidence retrieved the candidate. Expiring the image invalidated its live match and its recorded replay. The authored unit test also vetoed a contradiction at joint cosine 1.0.

The private history API replays unchanged inputs and binds category filters to recorded runs. An independently authenticated actor can use the same client run UUID in their own namespace. A superseded report no longer exposes its recorded result, and those retained runs still consume capacity until expiry. The restore drill compares data, Flyway history, grants, policies, constraints, indexes, and extension versions.

## Recall

The authored pgvector fixture contains 1,200 vectors. Ten independent queries use verified HNSW plans for tenant, space, active state and optional category filters. All 76 expected neighbors overlap the exact baseline, with zero filter violations. [Synthetic recall receipt](artifacts/m5-verification-20261118/synthetic-index-recall.json).

The public catalog proxy uses the pinned multilingual MiniLM text encoder and pgvector 0.8.7. It compares a single English title per listing with independently supplied multilingual titles for 36 listings across shoes, chairs and handbags. The 593-candidate gallery includes a phone-case distractor slice. The diagnostic assigns synthetic tenant scopes and compares the exact cosine baseline with an actual HNSW plan in each scope. Duplicate listing IDs, identical English titles and shared main image IDs are excluded before sampling. Gallery IDs and titles are omitted from the receipt.

| Scope | HNSW overlap at 1 / 5 / 10 | Exact listing hits at 1 / 5 / 10 | HNSW listing hits at 1 / 5 / 10 |
|---|---|---|---|
| Tenant | 36/36, 179/180, 359/360 | 16, 32, 34 out of 36 | 16, 33, 34 out of 36 |
| Tenant and category | 36/36, 180/180, 360/360 | 16, 33, 34 out of 36 | 16, 33, 34 out of 36 |

No scope violations appeared. Listing identity can differ from physical-instance identity, and catalog titles differ from short, noisy lost-and-found reports. These figures measure the proxy corpus, with seven query languages and uneven language counts. The tool uses a temporary table with the same vector dimension and metric as the reviewed text index. Target latency and HNSW scale remain to be measured. [Catalog recall and calibration receipt](artifacts/m5-verification-20261118/catalog-recall-and-calibration.json).

The source is the publisher's Amazon Berkeley Objects metadata shard `listings/metadata/listings_0.json.gz`, SHA-256 `c5e7723f4a4afa3da4f35c497ab30ac0f8ee5b3e473ec0ffa897511e127e9324`. Data credit goes to Amazon.com. Dataset and benchmark construction credit goes to Matthieu Guillaumin, Thomas Dideriksen, Kenan Deng, Himanshu Arora, Arnab Dhua, Xi (Brian) Zhang, Tomas Yago-Vicente, Jasmine Collins, Shubham Goel and Jitendra Malik. The diagnostic samples and normalizes text and derives vectors and aggregate statistics. [Publisher release and attribution](https://amazon-berkeley-objects.s3.amazonaws.com/index.html), [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/), [reproduction script](../../tools/m5_catalog_recall.py).

## Calibration

The manually authored fixture measures Brier 0.174 versus 0.200 for raw similarity on 200 supported holdout rows, with 200 fit rows. The independent catalog proxy uses one listing positive and four mined same-category negatives per query. It has 95 fit pairs, 85 supported holdout pairs, zero gallery identity overlap and holdout Brier 0.157 versus 0.396 for raw cosine. Its controlled mining fixes prevalence and the proxy measures vector-only scores. These draft evaluations leave live confidence uncalibrated.

The HTTP suite records a synthetic calibration draft, blocks self-approval, rejects an insufficient measured threshold, activates only a matching scope, and removes a recorded candidate's displayed estimate on revocation. Conflict candidates and incompatible spaces retain uncalibrated confidence. Review evidence and deployment labels remain external inputs.

## Integration, upgrade and restore

| Configuration | Integration tests | Restored tables and rows | Receipt |
|---|---:|---|---|
| Native PostgreSQL, SeaweedFS, Docker sanitizer, real text and vision models | 81 | 42 tables, 4,542 rows | [Core and vision](artifacts/m5-verification-20261118/core-and-vision-restore.json) |
| pgvector HNSW | 15 | 42 tables, 2,531 rows | [Indexed](artifacts/m5-verification-20261118/indexed-restore.json) |
| pgvector exact | 15 | 42 tables, 2,531 rows | [PostgreSQL exact](artifacts/m5-verification-20261118/pgvector-exact-restore.json) |
| Native PostgreSQL with detached Qdrant | 15 | 42 tables, 2,531 rows | [Qdrant](artifacts/m5-verification-20261118/qdrant-restore.json) |

All four passes include 27 Java unit tests, with zero failures or skips in the listed suites. The source audit, architecture baseline, 106 pinned Java artifacts and 240 Python regressions also pass. A separate populated V12 ledger upgrade applies the real forward SQL, preserves the older payload and client ID, backfills quota, and denies another actor. [Upgrade receipt](artifacts/m5-verification-20261118/legacy-history-upgrade.json), [upgrade fixture](artifacts/m5-verification-20261118/legacy-history-upgrade.py).

Another pinned Qdrant instance accepted a query with its read-only key and returned 403 on an attempted write with the same key. Its separate writer credential provisioned the collection and upserted a point. Anonymous reads were denied. [Qdrant credential test](artifacts/m5-verification-20261118/qdrant-credential-separation.json).

## Remaining acceptance

Production cutover needs the target database and Core identity configuration, an approved projection maintenance schedule and credentials for the target tenant, and an independent restore rehearsal on that deployment. Representative acceptance needs consented or appropriately licensed identity-labeled loss/recovery cases, coverage across deployed languages and modalities, independent calibration review, and an operating threshold. The active cohort is bounded to 100 reports and 64 multimodal pins. Text HNSW supports 384 dimensions, and the joint route uses bounded exact comparisons within the activated cohort. This milestone's local evidence supports those bounds. Production-scale benchmarks and confidence remain open.

The search for a public acceptance corpus turned up the LostNet CC BY 4.0 release. Its published technical description specifies category labels for classification. It does not document independently adjudicated lost/found physical-item pairs or representative non-matches. [LostNet paper](https://pmc.ncbi.nlm.nih.gov/articles/PMC11524469/), [publisher dataset metadata](https://api.figshare.com/v2/articles/24826740/versions/2).
