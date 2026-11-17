# Matched Shona translation comparison with OpenAI

November 18, 2026. The configured direct OpenAI route used GPT-6.1 Terra, low reasoning and a 1,024-token output limit per request. The plan froze 48 public or authored synthetic cases before the first call. All 48 returned complete translations with no truncations or unavailable responses. Provider-reported usage was 3,834 input and 3,068 output tokens. The run sent only source text and translation instructions. The local references and archived model outputs were used for scoring on the host.

The selected cases match the existing FLORES-200, authored code-switching and SmolSent comparisons by exact source and reference hashes. The eight SmolSent pairs are represented in both directions. Character error rate (CER) uses the repository scorer with NFC, case folding and collapsed whitespace. The existing screen requires eight complete results, median CER at or below 25%, and worst-case CER at or below 60% per slice. The authored slice also requires all 20 exact fragments. These reused public and synthetic cases have unknown overlap with model training. See [FLORES-200 data review](../dependencies/public-reference-data-review.md), [SmolSent attribution and review](../dependencies/morena-cpu-review.md#additional-references), and [earlier local results](m4-african-cpu-quality-2026-11-16.md).

| Slice | Complete | Median CER | Worst CER |
|---|---:|---:|---:|
| FLORES Shona → English development | 8/8 | 29.4% | 60.9% |
| FLORES Shona → English additional | 8/8 | 35.4% | 62.6% |
| FLORES English → Shona additional | 8/8 | 51.7% | 80.1% |
| Authored Shona/English → English | 8/8 | 3.9% | 14.3% |
| SmolSent English → Shona | 8/8 | 67.2% | 94.8% |
| SmolSent Shona → English | 8/8 | 41.6% | 68.6% |

All five general translation slices miss the thresholds. The code-switching slice passes both CER thresholds but matches 16 of 20 literal fragments. Review of the four literal misses found two equivalent time formats and two equivalent negative contractions. Keep the original exact-fragment score for historical comparability. The separate review accounts only for these four observed matches and does not assess overall semantic quality. In one FLORES case both prompts soften the Shona obligation to English “should,” where the reference uses “must.” A separate apparent death inference was a reference mismatch: the Shona source explicitly refers to the deceased, while the English reference says “victim.” This illustrates why high CER alone cannot identify a meaning error.

| Archived comparator | Common complete pairs | OpenAI lower / equal / higher CER |
|---|---:|---:|
| Isolated CPU baseline | 20 | 17 / 1 / 2 |
| TranslatePsy 0.8B | 32 | 22 / 1 / 9 |
| TranslatePsy 4B | 32 | 21 / 1 / 10 |
| MORENA zero-shot | 48 | 46 / 0 / 2 |
| MORENA three-shot | 48 | 31 / 1 / 16 |
| Gemini 3 Flash Preview | 8 | 2 / 2 / 4 |
| Qwen 235B | 8 | 7 / 1 / 0 |
| Gemini Pro medium | 23 | 11 / 4 / 8 |
| Gemini Pro low | 30 | 10 / 5 / 15 |

Comparators use their original configurations. Truncated and unavailable rows are excluded from the paired CER counts and preserved in the underlying evidence. These results compare the observed configurations and do not isolate a model effect. Raw reference text, response text and credentials remain outside Git. The [aggregate evidence](artifacts/openai-shona-20261118/summary.json) records coverage, source and plan identities. The [runner](artifacts/openai-shona-20261118/matched-runner.py) and [offline aggregator](artifacts/openai-shona-20261118/aggregate.py) record how the comparisons were made.

## Fidelity prompt development replay

The optional schema 2 connection settings select a distinct Shona prompt addressing mixed input, modality, actor roles, negation, named entities and unsupported inferences. The replay freezes 12 existing cases before calls: two inspected Shona-to-English fidelity failures, two English-to-Shona regressions and all eight authored cases. The same model, token limit, transport settings and scorer apply. This selected replay assesses a candidate prompt on previously seen inputs. It supplies no held-out evidence for wider adoption.

All 12 replays complete, with five lower, four equal and three higher CERs than the initial prompt. In the eight code-switch cases, median CER drops from 3.9% to 0.9% and literal matches rise from 16/20 to 19/20. The remaining literal miss is the equivalent contraction “don't know.” Worst CER remains 14.3%. Both sampled English-to-Shona rows regress (50.7% to 53.1% and 39.5% to 48.6%). The Shona obligation still reads “should” in the revised output. Leave the fidelity prompt opt-in and retain schema 1 as the active configuration. The code-switch gain does not establish broader quality or remove the need for bilingual review of the evaluation cases. [Replay results](artifacts/openai-shona-20261118/fidelity-summary.json) and [replay procedure](artifacts/openai-shona-20261118/fidelity-replay.py).

The replay used 1,801 input and 509 output tokens. Its private plan SHA-256 is `f13904eb79f4bb0520b8ea8c41b5d14811d1594471d4684ae150651e6176bb83`. Its private summary and archived copy share SHA-256 `189c96db2faed01b4060569ffeb9d095402975970f4b52e449f739a703613e8c`. The historical runner requires the locally reviewed FLORES and SmolSent cache members, the authored fixture literal in the original evaluator, and the prior private run directories. It parses the authored fixtures as data and verifies their content hashes against the archived plans. The exact first-run [client source](artifacts/openai-shona-20261118/openai_connection-v1.py) is retained separately.

The mandatory source audit, architecture check and all 235 Python regressions pass, including the new prompt selection and provenance check. The prior 27-phase gate and restore remain historical evidence for the source tested then. This optional reference client work did not rerun that full provider and restore gate. The representative Shona acceptance and M4 remain open.
