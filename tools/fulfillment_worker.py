"""Drain a bounded batch through the authenticated Core fulfillment API."""
import argparse
import json
import os
import urllib.request
from urllib.parse import urlsplit
import uuid


def drain(base, tenant, token, limit):
    origin = urlsplit(base)
    if (origin.scheme != "https" and not (origin.scheme == "http" and origin.hostname == "127.0.0.1")
            or origin.username or origin.password or origin.query or origin.fragment or origin.path not in ("", "/")
            or not origin.hostname or not 1 <= limit <= 100 or not isinstance(token, str) or not token
            or len(token) > 16384 or any(ord(c) < 33 for c in token)):
        raise ValueError("Fixed origin, worker credential and bounded batch required")
    tenant = str(uuid.UUID(tenant))

    class NoRedirect(urllib.request.HTTPRedirectHandler):
        def redirect_request(self, *args):
            return None

    opener = urllib.request.build_opener(urllib.request.ProxyHandler({}), NoRedirect())
    worked = 0
    for _ in range(limit):
        request = urllib.request.Request(base.rstrip("/") + f"/api/v1/tenants/{tenant}/fulfillment/dispatch", data=b"{}",
                                         headers={"Authorization": "Bearer " + token, "Content-Type": "application/json"})
        with opener.open(request, timeout=20) as response:
            raw = response.read(4097)
        if len(raw) > 4096:
            raise ValueError("Bounded Core response required")
        result = json.loads(raw)
        if not isinstance(result, dict) or set(result) != {"worked"} or type(result["worked"]) is not bool:
            raise ValueError("Unexpected Core dispatch result")
        if not result["worked"]:
            break
        worked += 1
    return worked


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base", required=True)
    parser.add_argument("--tenant", required=True)
    parser.add_argument("--max-operations", type=int, default=10)
    args = parser.parse_args()
    try:
        print(json.dumps({"worked": drain(args.base, args.tenant, os.environ.get("DZOKA_FULFILLMENT_WORKER_TOKEN"), args.max_operations)}))
    except Exception as error:
        print("Fulfillment worker unavailable: " + type(error).__name__)
        raise SystemExit(1)
