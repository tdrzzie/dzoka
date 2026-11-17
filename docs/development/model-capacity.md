# Bounded local model-path measurements

[ADR 0050](../adr/0050-bounded-local-model-capacity-diagnostics.md) adds `tools/model_capacity.py` using the existing reviewed runtime and providers. It measures serial confined model calls on the current Windows account. It does not implement concurrent/distributed scheduling, HTTP/DB queue measurement, saturation load, production capacity sizing, model-quality acceptance or unattended deployment. Shona profiles are excluded. No dependency/model/profile identity changes.

Create a permitted workload and a private host/workload evidence record. Record CPU/RAM, other workloads, selection/order, input authority, intended operating conditions and measurement limitations there; the result retains its digest, not host names or raw records. All plan/input/evidence files must be in one protected directory with private ancestor/leaf ACLs and no reparse paths. Retain the exact plan digest independently. Every input is hash-checked before inference. The example is a schema, not an achieved requirement:

```json
{
  "schemaVersion": 1,
  "scope": "LOCAL_CAPACITY_DIAGNOSTIC",
  "hostEvidence": {"path": "host.txt", "sha256": "<raw-file-sha256>"},
  "admissionBudgetSeconds": 300,
  "entries": [{
    "id": "private-workload-id",
    "profile": "ISO_TEXT",
    "profileKey": "<exact-existing-registry-key>",
    "input": {"path": "source.txt", "sha256": "<raw-input-sha256>"},
    "warmup": 1,
    "samples": 2
  }]
}
```

Bounds: one to four distinct workload IDs, zero to two warmups and one to eight measured samples per entry, at most twelve total calls; input files at most 2 MiB, host evidence 32 KiB, plan 256 KiB, unique descriptor files 16 MiB. Explicit admission budgets are integer 1–1,800 seconds. New calls cease once that budget is exhausted; in-flight calls still use the existing reviewed provider deadlines. This is not a hard process wall-time budget. Do not use it against live tenant data or concurrent production workers without separately authorized access/capacity planning.

The allowlist is text embeddings, joint text, grounded English attributes, bounded English reranking/drafts and the already-adopted English speech candidates; see `ALLOWED` for exact immutable registry names. Inputs must already use each existing provider's format. There is no adapter execution, model download, profile substitution or heuristic fallback. Invalid input/provider failures are recorded UNAVAILABLE, never fabricated outputs or automatic mismatches. No failed sample is retried implicitly.

Stage a **new** immutable private management release and retain its exact receipt. Keep the unchanged private model bundle/receipt. Provide a fresh protected state directory containing empty protected `profiles` and `docker` children. It must be independent of model bundle, dataset and output directory. The output parent must be protected and independent of the immutable model bundle. A native state-owner lock rejects concurrent capacity invocations. Never clear another worker's state or overwrite a release.

```powershell
python -I -S -B C:\DzokaPrivate\management-new\tools\model_capacity.py --plan C:\DzokaPrivate\capacity-data\plan.json --plan-sha256 <retained-plan-sha256> --bundle C:\DzokaPrivate\model-bundle --bundle-sha256 <retained-bundle-sha256> --state C:\DzokaPrivate\capacity-state-new --output C:\DzokaPrivate\results\capacity-001.jsonl --management-sha256 <retained-management-receipt>
```

The CLI verifies its protected release before reading private data. Initial complete model-bundle verification precedes timing. Each sample includes confinement/grants, model startup/inference, cleanup and post-call state/bundle boundary checks. Every call starts a fresh model process; warmups can affect OS caches but do not establish persistent loaded-model latency. The host may have unrelated workloads; results are specific to recorded conditions and order, without comparisons between unrelated tasks or claims of independent samples.

The existing model child's isolation/resource limits apply. The standalone collector does not install or reproduce the managed service parent's additional job/resource envelope. These measurements cannot establish installed-service latency or capacity, even when the same child profile is selected; those require end-to-end measurements under the selected service's actual limits.

The exclusively created JSONL ledger flushes/fsyncs START, each CALL and SUMMARY/ABORT, preserving evidence before interruption. Each event has a sequence, UTC observation time and previous-event raw JSON SHA-256. Retain the final whole-ledger SHA-256 separately. `inspect(raw, expectedSha256)` checks exact retained bytes, sequence/order and hash chain; a complete-line prefix without terminal event is INCOMPLETE, while a torn line is invalid. Hash chains are not signatures or proof against an authorized editor. There is no resume/overwrite operation: retain failures, reconcile owned state through existing controls, then use a new plan/output/state for another run.

AVAILABLE, UNAVAILABLE, UNCONFIRMED, NOT_DISPATCHED and unobserved counts remain separate. Unconfirmed cleanup/integrity aborts further dispatch and preserves state for inspection. Truncation flags stay null if the provider does not supply them; AVAILABLE means an output was returned, not complete language/quality evidence. Output hashes cover sorted compact UTF-8 JSON, not raw transport bytes. Raw inputs/outputs, labels, exception details and host identity are omitted. Direct library calls without a verified management receipt are NOT_ATTESTED.

Percentiles use nearest rank over **available samples only**, with counts, min/max and warmups reported separately. Unavailable/not-dispatched samples never contribute zero latency. With two observations, p95/p99 are simply the maximum: they do not estimate population tails. Every summary leaves SLO/approval NOT_ESTABLISHED, quality NOT_EVALUATED and release NOT_GRANTED. No model activation or production configuration follows from these measurements.

The full gate includes `tools/verify_model_capacity.py`: actual isolated text warmup/two measured calls, an actual invalid UTF-8 failure, native admission-budget exhaustion, competing-owner rejection, private-leaf read denial, output preservation and confirmed grant/profile cleanup. Authored inputs and this shared host do not establish representative or end-to-end capacity acceptance.
