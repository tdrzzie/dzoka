"""Reproduce a hash-bound public-camera diagnostic without exporting pixels.

Download licensed inputs separately. This evaluator never fetches URLs, changes
provider thresholds, approves a deployment, or uses uncertain labels as negatives.
"""
import argparse
from datetime import datetime, timezone
from hashlib import sha256
import json
import math
from pathlib import Path

from edge_vision import IsolatedTemplateTracker, decode_video_frame, disclosure_mask, redacted_crop
from edge_runtime import check


def digest(path):
    with path.open("rb") as stream:
        return __import__("hashlib").file_digest(stream, "sha256").hexdigest()


def checked_file(directory, name, expected):
    if not isinstance(name, str) or Path(name).name != name or any(c in name for c in "/\\:"):
        raise ValueError("Plain input basename required")
    path = directory / name
    if path.is_symlink() or getattr(path.lstat(), "st_file_attributes", 0) & 0x400 or not path.is_file():
        raise ValueError("Plain input file required")
    if path.stat().st_size > 64 * 1024 * 1024 or digest(path) != expected:
        raise ValueError("Input digest or size mismatch")
    return path


def iou(left, right):
    x = max(0, min(left[0] + left[2], right[0] + right[2]) - max(left[0], right[0]))
    y = max(0, min(left[1] + left[3], right[1] + right[3]) - max(left[1], right[1]))
    intersection = x * y
    return intersection / (left[2] * left[3] + right[2] * right[3] - intersection)


def metrics(rows):
    counts = {key: 0 for key in ("tp", "fp", "tn", "fn", "abstained", "uncertainLabels")}
    overlaps = []
    for row in rows:
        label, predicted = row["label"], row["observation"]["state"]
        if label == "UNKNOWN":
            counts["uncertainLabels"] += 1
            continue
        if predicted == "UNKNOWN":
            counts["abstained"] += 1
        elif label == "PRESENT":
            counts["tp" if predicted == "PRESENT" else "fn"] += 1
        else:
            counts["fp" if predicted == "PRESENT" else "tn"] += 1
        if label == predicted == "PRESENT":
            overlaps.append(iou(row["box"], row["observation"]["box"]))
    scored = len(rows) - counts["uncertainLabels"]
    return {**counts, "labelled": scored, "decisionCoverage": (scored - counts["abstained"]) / scored if scored else None,
            "recallIncludingAbstentions": counts["tp"] / sum(row["label"] == "PRESENT" for row in rows)
            if any(row["label"] == "PRESENT" for row in rows) else None,
            "meanBoxIoU": sum(overlaps) / len(overlaps) if overlaps else None}


def evaluate(manifest_path, directory):
    raw = manifest_path.read_bytes()
    manifest = json.loads(raw)
    if manifest["schema"] != 1 or not 1 <= len(manifest["clips"]) <= 10:
        raise ValueError("Bounded evaluation manifest required")
    checked_file(directory, manifest["license"]["file"], manifest["license"]["sha256"])
    check()
    results = []
    for clip in manifest["clips"]:
        path = checked_file(directory, clip["file"], clip["sha256"])
        if "excluded" in clip:
            results.append({"id": clip["id"], "sourceSha": clip["sha256"], "excluded": clip["excluded"]})
            continue
        samples = clip["samples"]
        offsets = [row["offset"] for row in samples]
        if not 1 <= len(samples) <= 50 or offsets != sorted(set(offsets)) or clip["referenceOffset"] in offsets:
            raise ValueError("Distinct chronological evaluation samples required")
        if any(type(t) not in (int, float) or not math.isfinite(t) or not 0 <= t <= 86400
               for t in offsets + [clip["referenceOffset"]]):
            raise ValueError("Bounded offsets required")
        for sample in samples:
            if sample["label"] not in ("PRESENT", "NOT_OBSERVED", "UNKNOWN"):
                raise ValueError("Explicit availability label required")
            if sample["label"] == "PRESENT":
                box = sample["box"]
                if len(box) != 4 or any(type(v) is not int for v in box) or min(box[:2]) < 0 or min(box[2:]) <= 0:
                    raise ValueError("Positive label needs a valid box")
        width, height, pixels = decode_video_frame(path, clip["referenceOffset"], clip.get("sourceRegion"))
        reference = f"P6\n{width} {height}\n255\n".encode() + pixels
        provider = IsolatedTemplateTracker(reference, clip["referenceBox"], clip["threshold"])
        mask = clip.get("disclosureMask")
        if "disclosureMask" in clip:
            disclosure_mask(mask, clip["referenceBox"][2:])
        rows = []
        for sample in samples:
            frame = decode_video_frame(path, sample["offset"], clip.get("sourceRegion"))
            observation = provider.detect(frame, clip["roi"])
            row = {**sample, "frameSha": sha256(frame[2]).hexdigest(), "observation": observation}
            if mask is not None and observation["state"] == "PRESENT":
                cropped = redacted_crop(frame, observation["box"], mask)
                row["maskedCrop"] = {"sha256": sha256(cropped).hexdigest(),
                                     "suppressedPixels": sum(line.count("0") for line in mask),
                                     "permittedPixels": sum(line.count("1") for line in mask)}
            rows.append(row)
        intervals = [{"afterOffset": left["offset"], "beforeOffset": right["offset"],
                      "lastSeenFrameSha": left["frameSha"], "notObservedFrameSha": right["frameSha"],
                      "hypothesis": "POSSIBLE_DISAPPEARANCE"}
                     for left, right in zip(rows, rows[1:])
                     if left["observation"]["state"] == "PRESENT" and right["observation"]["state"] == "NOT_OBSERVED"]
        results.append({"id": clip["id"], "sourceSha": clip["sha256"], "sourceRegion": clip.get("sourceRegion"), "profile": provider.profile,
                        "referenceSha": sha256(reference).hexdigest(), "metrics": metrics(rows), "samples": rows,
                        "possibleDisappearanceIntervalsSeconds": intervals})
    root = Path(__file__).resolve().parents[1]
    return {"schema": 1, "executedAt": datetime.now(timezone.utc).isoformat(),
            "scope": "MANUALLY_LABELLED_PUBLIC_EXAMPLE_DIAGNOSTIC", "manifestSha": sha256(raw).hexdigest(),
            "evaluatorSha": digest(Path(__file__)), "providerSha": digest(root / "tools/edge_vision.py"),
            "runtimeLockSha": digest(root / "ai/edge-runtime.lock.json"),
            "decoderLockSha": digest(root / "ai/edge-video.lock.json"), "clips": results}


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", required=True, type=Path)
    parser.add_argument("--inputs", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    result = evaluate(args.manifest, args.inputs.resolve(strict=True))
    with args.output.open("x", encoding="utf-8") as stream:
        json.dump(result, stream, indent=2)
        stream.write("\n")
    print("PASS: isolated public-camera diagnostic recorded")
