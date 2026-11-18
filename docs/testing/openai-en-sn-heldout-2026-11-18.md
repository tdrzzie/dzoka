# English-to-Shona held-out OpenAI comparison

The previous fidelity prompt regressed on both sampled English-to-Shona cases. The new opt-in prompt targets fluent standard written Zimbabwean Shona, noun-class agreement, idiomatic clause-level translation and preservation of actors, modality, negation and exact identifiers. It applies only from English to Shona.

The paired plan fixes eight FLORES-200 `devtest` rows (241–248, one-based) and eight professional SmolSent English-to-Shona rows (33–40, one-based). The runner verifies the pinned dataset-member hashes, checks exact source and reference content against earlier archived plans and excludes the MORENA context rows in both directions. This is a holdout from the checked Dzoka experiments. Model training exposure is unknown. The [FLORES review](../dependencies/public-reference-data-review.md) and [SmolSent attribution](../dependencies/morena-cpu-review.md#additional-references) cover the licensed reference assets.

The active and candidate prompts use the same `gpt-6.1-terra` model, low reasoning and 1,024 output token ceiling. The runner alternates which prompt goes first and freezes both configurations, the exact source and scorer hashes, and all row identities before any calls. Only public English source sentences and translation instructions go to OpenAI. Target references stay local. No retries or provider fallback are enabled. The existing diagnostic screen is eight complete cases per dataset, median CER at or below 25%, and worst CER at or below 60%. WER is secondary, and neither edit metric adjudicates Shona meaning. The [runner](artifacts/openai-en-sn-heldout-20261118/paired-runner.py) records the exact procedure.


| Dataset | Active prompt median / worst CER | Targeted prompt median / worst CER | Targeted lower / equal / higher CER |
|---|---:|---:|---:|
| FLORES-200 | 53.4% / 81.8% | 56.4% / 80.0% | 5 / 0 / 3 |
| SmolSent | 57.7% / 74.1% | 55.7% / 103.5% | 3 / 1 / 4 |

Both prompts complete all 16 cases without truncation or unavailable calls. Both fail the predeclared screen in each dataset. Across 16 complete pairs the targeted prompt wins eight, ties one and loses seven. Median WER regresses on FLORES from 76.3% to 82.9% and changes from 89.5% to 89.0% on SmolSent. CER can exceed 100% when insertions make the output much longer than the reference. On the worst candidate row, its paraphrase of an economic term changes the phrasing substantially. The technical terminology needs bilingual review before attributing that full edit distance to a meaning error.

The two source sentences with digits also show why surface checks need review. Both prompts preserve 15 in the SmolSent example. In the FLORES example the candidate keeps the digit 3 and the active prompt spells it as “matatu.” Both retain the approximate three-minute timing. These spot checks cover two numbers only.

Keep schema 1 active. The target-language prompt remains an opt-in diagnostic for further development and has no broader quality approval. Any future candidate needs a distinct holdout. The [frozen plan](artifacts/openai-en-sn-heldout-20261118/plan.json) and [observations](artifacts/openai-en-sn-heldout-20261118/summary.json) retain provenance, paired scores and coverage without source or model text. The independent check recomputed all 32 CER/WER scores and verified the provider receipt hashes and exact model, settings and source identities. OpenAI reported 4,082 input and 3,252 output tokens across the 32 calls.

The frozen plan SHA-256 is `b6885d77ec75851d537815accc8801dfcea4e8ed7af69dfbc710ffbc6b58bda9`. The observation SHA-256 is `17e7118a5b81b69672af2d0ee6d446dce4a9baa1ba3299068ae6cc89ae247d73`. Protected input, attempt, output and score files are retained under `C:\Users\nyash\DzokaOpenAI\en-sn-heldout-20261118-v1`.

## Full gate and restore

All 27 phases passed on executed source `d5bc12ec19a0f385d0f3b443b00c5e8c68fcd0379332064cf7d8d39f1275e68c`. The run finished with 236 Python tests, 24 Java unit tests and 97 integration tests, with zero failures, errors or skips. The installed worker recovered its claim and completed all 33 jobs. The logical restore verified 35 tables and 3,686 rows, with row and catalog equality, restored tenant isolation and source snapshot independence. [Gate receipt](artifacts/openai-en-sn-heldout-20261118/gate-receipt.json), [queue receipt](artifacts/openai-en-sn-heldout-20261118/queue-receipt.json), [restore receipt](artifacts/openai-en-sn-heldout-20261118/restore-receipt.json) and [source byte overlay](artifacts/openai-en-sn-heldout-20261118/source-overlay.json).

The first management-release staging command failed before starting a gate. A new private management release staged successfully with receipt `364b4b70333cc5b9ba3a8d0a11e206dea873bfb360875fc04693eac8edbbe472`. Its full gate ran once and passed. The model bundle receipt remains `2382b945fbe3532f6253246d969a41d7d51623b2cc737630c64a09f49738b027`. No application verification bounds or provider tests were relaxed.

The independent hosted quality test and the local gate establish different evidence. English-to-Shona quality and M4 acceptance remain open.
