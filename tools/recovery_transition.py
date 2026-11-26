"""Versioned, auditable frequency and elapsed-time recovery baseline."""
import hashlib
import json
import math
from statistics import median
from edge_vision import utc

VERSION = "recovery-transition-frequency-v1"


def digest(value):
    return hashlib.sha256(json.dumps(value, sort_keys=True, separators=(",", ":"), allow_nan=False).encode()).hexdigest()


def validate(rows):
    if not isinstance(rows, list) or not 1 <= len(rows) <= 10000:
        raise ValueError("Bounded, purpose-approved transitions required")
    seen = set()
    for row in rows:
        if set(row) != {"item", "institution", "from", "to", "before", "after", "recorded", "sourceSha"}:
            raise ValueError("Transition schema mismatch")
        for name in ("item", "institution", "from", "to"):
            value = row[name]
            if not isinstance(value, str) or not 1 <= len(value) <= 64 or any(c not in "abcdefghijklmnopqrstuvwxyz0123456789-_" for c in value):
                raise ValueError("Opaque transition labels required")
        if not isinstance(row["sourceSha"], str) or len(row["sourceSha"]) != 64 or any(c not in "0123456789abcdef" for c in row["sourceSha"]):
            raise ValueError("Native evidence digest required")
        if row["sourceSha"] in seen:
            raise ValueError("Duplicate source")
        seen.add(row["sourceSha"])
        if utc(row["after"]) <= utc(row["before"]) or utc(row["recorded"]) < utc(row["after"]):
            raise ValueError("Invalid transition timing")


def train(rows, cutoff, author, purpose_sha):
    validate(rows)
    if not isinstance(author, str) or not author or not isinstance(purpose_sha, str) or len(purpose_sha) != 64:
        raise ValueError("Author and purpose evidence required")
    as_of = utc(cutoff)
    if any(utc(row["recorded"]) > as_of for row in rows):
        raise ValueError("Training data exceeds as-of cutoff")
    groups = {}
    for row in rows:
        key = row["from"]
        selected = groups.setdefault(key, {})
        # One item contributes at most once to a source-state distribution.
        if row["item"] in selected:
            raise ValueError("Repeated item in source-state distribution")
        selected[row["item"]] = row
    states = {}
    for source, values in groups.items():
        destinations = {}
        for row in values.values():
            seconds = (utc(row["after"]) - utc(row["before"])).total_seconds()
            destinations.setdefault(row["to"], []).append(seconds)
        if len(values) >= 3:
            states[source] = [{"state": target, "support": len(times), "frequency": len(times) / len(values),
                               "medianSeconds": median(times)} for target, times in sorted(destinations.items())]
    model = {"version": VERSION, "cutoff": cutoff, "author": author, "purposeSha": purpose_sha,
             "datasetSha": digest(rows), "states": states, "calibration": "UNAVAILABLE"}
    return {**model, "modelSha": digest(model)}


def predict(model, source):
    unsigned = {key: value for key, value in model.items() if key != "modelSha"}
    if model.get("version") != VERSION or digest(unsigned) != model.get("modelSha"):
        raise ValueError("Model integrity failure")
    distribution = model["states"].get(source, [])
    return {"classification": "PREDICTED", "available": bool(distribution), "calibration": "UNAVAILABLE",
            "modelSha": model["modelSha"], "distribution": distribution}


def evaluate(model, train_rows, test_rows):
    validate(train_rows)
    validate(test_rows)
    if model["datasetSha"] != digest(train_rows):
        raise ValueError("Training provenance mismatch")
    for field in ("item", "institution"):
        if {row[field] for row in train_rows} & {row[field] for row in test_rows}:
            raise ValueError("Evaluation split leaks " + field)
    if max(utc(row["recorded"]) for row in train_rows) >= min(utc(row["before"]) for row in test_rows):
        raise ValueError("Evaluation time split leaks future observations")
    available = correct = 0
    errors = []
    for row in test_rows:
        result = predict(model, row["from"])
        if not result["available"]:
            continue
        available += 1
        selected = sorted(result["distribution"], key=lambda value: (-value["frequency"], value["state"]))[0]
        correct += selected["state"] == row["to"]
        if selected["state"] == row["to"]:
            seconds = (utc(row["after"]) - utc(row["before"])).total_seconds()
            errors.append(abs(selected["medianSeconds"] - seconds))
    return {"modelSha": model["modelSha"], "testSha": digest(test_rows), "cases": len(test_rows),
            "available": available, "top1": None if not available else correct / available,
            "medianTimeAbsoluteErrorSeconds": None if not errors else median(errors),
            "calibration": "UNAVAILABLE", "promotion": "REQUIRES_REVIEW"}


def approve(model, evaluation, reviewer, acceptance_sha):
    predict(model, "__integrity_check__")
    if not reviewer or reviewer == model["author"] or evaluation["modelSha"] != model["modelSha"]:
        raise ValueError("Independent review required")
    if not isinstance(acceptance_sha, str) or len(acceptance_sha) != 64:
        raise ValueError("Protected acceptance evidence required")
    return {"modelSha": model["modelSha"], "evaluationSha": digest(evaluation), "reviewer": reviewer,
            "acceptanceSha": acceptance_sha, "state": "APPROVED_FOR_ASSISTED_SEARCH"}


def main():
    import argparse
    from pathlib import Path
    parser = argparse.ArgumentParser()
    parser.add_argument("operation", choices=["train", "evaluate", "predict", "approve"])
    parser.add_argument("--input", required=True)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()
    path = Path(args.input)
    if path.is_symlink() or path.stat().st_size > 8 * 1024 * 1024:
        raise ValueError("Bounded local transition input required")
    request = json.loads(path.read_bytes())
    if args.operation == "train":
        result = train(request["rows"], request["cutoff"], request["author"], request["purposeSha"])
    elif args.operation == "evaluate":
        result = evaluate(request["model"], request["training"], request["test"])
    elif args.operation == "predict":
        result = predict(request["model"], request["source"])
    else:
        result = approve(request["model"], request["evaluation"], request["reviewer"], request["acceptanceSha"])
    with Path(args.output).open("xb") as target:
        target.write((json.dumps(result, sort_keys=True, allow_nan=False) + "\n").encode())


if __name__ == "__main__":
    try:
        main()
    except Exception as error:
        print("Transition operation unavailable: " + type(error).__name__)
        raise SystemExit(1)
