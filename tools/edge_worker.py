"""One leased edge job. API credentials and camera archive stay outside the sandbox."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import urllib.error
import urllib.request
from urllib.parse import urlsplit
import uuid
import base64

from edge_vision import FrameArchive, IsolatedTemplateTracker, search, disclosure_mask, redacted_crop


def api(base, token, method, path, body=None):
    endpoint = urlsplit(base)
    if endpoint.scheme != "https" and not (endpoint.scheme == "http" and endpoint.hostname == "127.0.0.1"):
        raise ValueError("TLS or loopback API required")
    if endpoint.username or endpoint.password or endpoint.query or endpoint.fragment or endpoint.path not in ("", "/"):
        raise ValueError("API origin required")
    class NoRedirect(urllib.request.HTTPRedirectHandler):
        def redirect_request(self, *args):
            return None
    opener = urllib.request.build_opener(urllib.request.ProxyHandler({}), NoRedirect())
    payload = None if body is None else json.dumps(body).encode()
    request = urllib.request.Request(base.rstrip("/") + path, data=payload, method=method,
                                     headers={"Authorization": "Bearer " + token, "Content-Type": "application/json"})
    with opener.open(request, timeout=10) as response:
        raw = response.read(16385)
        if len(raw) > 16384:
            raise ValueError("API response exceeds limit")
        return None if not raw else json.loads(raw)


def read(path, maximum):
    path = Path(path)
    if path.is_symlink() or getattr(path.lstat(), "st_file_attributes", 0) & 0x400:
        raise ValueError("Plain local file required")
    with path.open("rb") as source:
        raw = source.read(maximum + 1)
    if len(raw) > maximum:
        raise ValueError("Input exceeds bound")
    return raw


def run_once(base, token, tenant, archive_path, manifest_path, reference_path, plan_path, output):
    from edge_runtime import check
    check()
    tenant = str(uuid.UUID(tenant))
    root = f"/api/v1/tenants/{tenant}/edge"
    lease = api(base, token, "POST", root + "/claim")
    if lease is None:
        return False
    manifest_raw = read(manifest_path, 2 * 1024 * 1024)
    plan_raw = read(plan_path, 16384)
    if hashlib.sha256(manifest_raw).hexdigest() != lease["archiveSha"]:
        raise ValueError("Registered archive changed")
    if hashlib.sha256(plan_raw).hexdigest() != lease["authorizationSha"]:
        raise ValueError("Reviewer-approved search plan changed")
    plan = json.loads(plan_raw)
    required = {"camera", "referenceSha", "referenceBox", "searchBox", "threshold", "purpose"}
    if set(plan) not in (required, required | {"disclosureMask"}):
        raise ValueError("Invalid search plan")
    if plan["camera"] != lease["camera"] or plan["purpose"] != "LOST_PROPERTY_SEARCH":
        raise ValueError("Plan outside grant")
    reference = read(reference_path, 320 * 240 * 3 + 64)
    if hashlib.sha256(reference).hexdigest() != plan["referenceSha"]:
        raise ValueError("Reference object changed")
    archive = FrameArchive(archive_path, lease["camera"], json.loads(manifest_raw))
    provider = IsolatedTemplateTracker(reference, plan["referenceBox"], plan["threshold"])
    mask = plan.get("disclosureMask")
    if "disclosureMask" in plan:
        disclosure_mask(mask, plan["referenceBox"][2:])
    job = str(uuid.UUID(lease["job"]))

    def authorize():
        current = api(base, token, "POST", root + f"/jobs/{job}/access", {"token": lease["token"]})
        if current != lease:
            raise ValueError("Lease authorization changed")

    result = search(archive, provider, lease["start"], lease["end"], plan["searchBox"], lease["budget"], authorize)
    evidence = []
    for observation in result["observations"]:
        if mask is None or observation["state"] != "PRESENT" or len(evidence) >= 4:
            continue
        frame = next(frame for frame in archive.frames if frame.time == observation["time"])
        width, height, pixels = archive.read(frame, authorize)
        x, y, crop_width, crop_height = observation["box"]
        cropped = redacted_crop((width, height, pixels), observation["box"], mask)
        evidence_id = str(uuid.uuid5(uuid.UUID(job), frame.digest + ":" + frame.time))
        api(base, token, "PUT", root + f"/jobs/{job}/evidence/{evidence_id}",
            {"token": lease["token"], "width": crop_width, "height": crop_height, "pixels": base64.b64encode(cropped).decode()})
        evidence.append(evidence_id)
    result["evidence"] = evidence
    observations = [{"after": value["time"], "before": value["time"], "gap": value["state"] == "UNKNOWN",
                     "sourceSha": value["sourceSha"], "processorSha": result["profile"], "status": value["state"]}
                    for value in result["observations"]]
    authorize()
    accepted = api(base, token, "PUT", root + f"/jobs/{job}/result", {"token": lease["token"], "observations": observations})
    if not accepted["accepted"]:
        raise ValueError("Result invalidated by revocation")
    # The evidence contains intervals and provenance, without original pixels.
    if output:
        destination = Path(output)
        with destination.open("xb") as target:
            os.chmod(destination, 0o600)
            target.write(json.dumps({"job": job, **result}, sort_keys=True).encode())
    return True


def main():
    parser = argparse.ArgumentParser()
    for name in ("base", "tenant", "archive", "manifest", "reference", "plan"):
        parser.add_argument("--" + name, required=True)
    parser.add_argument("--output")
    args = parser.parse_args()
    run_once(args.base, os.environ["DZOKA_EDGE_TOKEN"], args.tenant, args.archive, args.manifest,
             args.reference, args.plan, args.output)


if __name__ == "__main__":
    try:
        main()
    except urllib.error.HTTPError as error:
        print("Edge API denied operation with HTTP " + str(error.code))
        raise SystemExit(1)
    except Exception as error:
        print("Edge job unavailable: " + type(error).__name__)
        raise SystemExit(1)
