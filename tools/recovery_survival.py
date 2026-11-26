"""Transparent waiting-time baseline with explicit right-censored observations.

This offline artifact never changes custody or authorizes model deployment.
"""
import argparse
from datetime import datetime, timezone
import hashlib
import json
import math
from pathlib import Path
import platform

from edge_vision import utc
from recovery_transition import digest, evidence_digest, validate

VERSION = "recovery-wait-kaplan-meier-v1"


def censored(rows):
    if not isinstance(rows, list) or len(rows) > 10000:
        raise ValueError("Bounded censored observations required")
    converted = []
    for row in rows:
        if set(row) != {"item", "institution", "from", "before", "through", "recorded", "sourceSha"}:
            raise ValueError("Censored observation schema mismatch")
        converted.append({key: value for key, value in row.items() if key != "through"}
                         | {"to": "censored", "after": row["through"]})
    if converted:
        validate(converted)


def dataset(completed, incomplete):
    if not isinstance(completed, list) or len(completed) > 10000:
        raise ValueError("Bounded completed transitions required")
    if completed:
        validate(completed)
    censored(incomplete)
    rows = completed + incomplete
    if not 1 <= len(rows) <= 10000 or len({row["sourceSha"] for row in rows}) != len(rows):
        raise ValueError("Distinct bounded source evidence required")
    if len({(row["item"], row["from"]) for row in rows}) != len(rows):
        raise ValueError("Repeated item within source state")
    return rows


def train(completed, incomplete, cutoff, author, purpose_sha):
    rows = dataset(completed, incomplete)
    if not isinstance(author, str) or not author or not evidence_digest(purpose_sha):
        raise ValueError("Author and purpose evidence required")
    if any(utc(row["recorded"]) > utc(cutoff) for row in rows):
        raise ValueError("Training exceeds as-of cutoff")
    groups = {}
    for row in rows:
        group = groups.setdefault(row["from"], {})
        if row["item"] in group:
            raise ValueError("Repeated item within source state")
        end = row.get("after", row.get("through"))
        group[row["item"]] = ((utc(end) - utc(row["before"])).total_seconds(), "after" in row)
    states = {}
    for state, group in sorted(groups.items()):
        if len(group) < 3:
            continue
        counts = {}
        for time, event in group.values():
            bucket = counts.setdefault(time, [0, 0])
            bucket[0 if event else 1] += 1
        at_risk, survival = len(group), 1.0
        curve = []
        for time, (events, censor_count) in sorted(counts.items()):
            # Events at a tied time use the risk set before censor withdrawals.
            survival *= 1 - events / at_risk
            curve.append({"seconds": time, "atRisk": at_risk, "events": events,
                          "censored": censor_count, "survival": survival})
            at_risk -= events + censor_count
        states[state] = {"support": len(group), "curve": curve}
    model = {"version": VERSION, "cutoff": cutoff, "author": author, "purposeSha": purpose_sha,
             "datasetSha": digest({"completed": completed, "censored": incomplete}), "states": states,
             "provider": "DZOKA_ORIGINAL_STDLIB", "createdAt": datetime.now(timezone.utc).isoformat(),
             "implementationSha": hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
             "runtime": "CPython-" + platform.python_version(),
             "configuration": {"minimumDistinctItems": 3, "extrapolation": "UNAVAILABLE",
                               "ties": "EVENTS_BEFORE_CENSOR_WITHDRAWAL"},
             "calibration": "UNAVAILABLE", "rollout": "REQUIRES_INDEPENDENT_REVIEW"}
    return model | {"modelSha": digest(model)}


def predict(model, source, horizon):
    if model.get("version") != VERSION or digest({k: v for k, v in model.items() if k != "modelSha"}) != model.get("modelSha"):
        raise ValueError("Waiting model integrity failure")
    if type(horizon) not in (int, float) or not math.isfinite(horizon) or horizon < 0:
        raise ValueError("Nonnegative finite horizon required")
    state = model["states"].get(source)
    available = state is not None and horizon <= state["curve"][-1]["seconds"]
    probability = None
    if available:
        probability = 1.0
        for point in state["curve"]:
            if point["seconds"] <= horizon:
                probability = point["survival"]
    return {"classification": "PREDICTED", "modelSha": model["modelSha"], "available": available,
            "horizonSeconds": horizon, "remainingWithoutTransition": probability,
            "calibration": "UNAVAILABLE", "scope": "POOLED_SOURCE_STATE_WAITING_TIME"}


def evaluate(model, training, training_censored, test, test_censored, horizon):
    source = dataset(training, training_censored)
    target = dataset(test, test_censored)
    predict(model, "__integrity__", horizon)
    if model["datasetSha"] != digest({"completed": training, "censored": training_censored}):
        raise ValueError("Training provenance mismatch")
    for field in ("item", "institution", "sourceSha"):
        if {row[field] for row in source} & {row[field] for row in target}:
            raise ValueError("Waiting evaluation leaks " + field)
    first_test = min(utc(row["before"]) for row in target)
    if max(utc(row["recorded"]) for row in source) >= first_test or utc(model["cutoff"]) >= first_test:
        raise ValueError("Waiting evaluation leaks future evidence")
    errors = []
    unresolved = unavailable = 0
    predictions = {}
    for row in target:
        duration = (utc(row.get("after", row.get("through"))) - utc(row["before"])).total_seconds()
        if "through" in row and duration < horizon:
            unresolved += 1
            continue
        if row["from"] not in predictions:
            predictions[row["from"]] = predict(model, row["from"], horizon)
        result = predictions[row["from"]]
        if not result["available"]:
            unavailable += 1
            continue
        truth = duration > horizon if "after" in row else True
        errors.append((result["remainingWithoutTransition"] - truth) ** 2)
    return {"modelSha": model["modelSha"], "testSha": digest({"completed": test, "censored": test_censored}),
            "horizonSeconds": horizon, "cases": len(target), "scored": len(errors),
            "censoredBeforeHorizon": unresolved, "unavailableModelCases": unavailable,
            "coverage": len(errors) / len(target), "observableHorizonBrier": sum(errors) / len(errors) if errors else None,
            "metricScope": "OBSERVABLE_HORIZON_OUTCOMES_ONLY_NOT_POPULATION_CALIBRATION",
            "calibration": "UNAVAILABLE", "promotion": "REQUIRES_INDEPENDENT_REVIEW"}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("operation", choices=["train", "predict", "evaluate"])
    parser.add_argument("--input", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if args.input.is_symlink() or getattr(args.input.lstat(), "st_file_attributes", 0) & 0x400 or args.input.stat().st_size > 8 * 1024 * 1024:
        raise ValueError("Bounded plain local input required")
    request = json.loads(args.input.read_bytes())
    if args.operation == "train":
        result = train(request["completed"], request["censored"], request["cutoff"], request["author"], request["purposeSha"])
    elif args.operation == "predict":
        result = predict(request["model"], request["source"], request["horizonSeconds"])
    else:
        result = evaluate(request["model"], request["training"], request["trainingCensored"], request["test"],
                          request["testCensored"], request["horizonSeconds"])
    with args.output.open("x", encoding="utf-8") as stream:
        json.dump(result, stream, sort_keys=True, allow_nan=False)
        stream.write("\n")


if __name__ == "__main__":
    try:
        main()
    except Exception as error:
        print("Waiting-time operation unavailable: " + type(error).__name__)
        raise SystemExit(1)
