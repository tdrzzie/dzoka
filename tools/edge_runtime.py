"""Verify the optional immutable edge image and implementation manifest."""
import hashlib
import json
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]


def check():
    lock = json.loads((ROOT / "ai/edge-runtime.lock.json").read_bytes())
    if lock["schema"] != 1 or lock["scope"] != "PORTABLE_P6_EDGE":
        raise ValueError("Unsupported edge runtime profile")
    for name, expected in lock["sources"].items():
        path = ROOT / name
        if path.is_symlink() or hashlib.sha256(path.read_bytes()).hexdigest() != expected:
            raise ValueError("Edge implementation digest mismatch")
    result = subprocess.run(["docker", "image", "inspect", lock["image"]], check=True, capture_output=True, timeout=15)
    images = json.loads(result.stdout)
    if len(images) != 1 or images[0]["Os"] != "linux" or images[0]["Architecture"] != "amd64":
        raise ValueError("Unsupported edge runtime platform")
    if lock["image"] not in images[0]["RepoDigests"]:
        raise ValueError("Edge runtime identity mismatch")
    return lock["image"]


if __name__ == "__main__":
    check()
    print("PASS: immutable edge image and source manifest")
