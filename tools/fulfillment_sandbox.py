"""Explicit loopback-only development gateway. No real funds or shipments.

Persists synthetic quotes, virtual reservations and idempotent operations in
SQLite so unknown outcomes can be reconciled across process restarts.
"""
import argparse
from contextlib import contextmanager
from hashlib import sha256
import hmac
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
import os
from pathlib import Path
import re
import sqlite3
import time
import uuid


def encode(value):
    return json.dumps(value, sort_keys=True, separators=(",", ":"), allow_nan=False).encode()


@contextmanager
def database(path):
    db = sqlite3.connect(path, timeout=5)
    try:
        yield db
        db.commit()
    except BaseException:
        db.rollback()
        raise
    finally:
        db.close()


def make_server(path, token, port=0, drop_first_reply=False):
    path = Path(path).resolve()
    if not isinstance(token, str) or len(token) < 24 or any(c.isspace() for c in token):
        raise ValueError("Private sandbox token required")
    path.parent.mkdir(parents=True, exist_ok=True)
    with database(path) as db:
        db.executescript("""
        CREATE TABLE IF NOT EXISTS operations(tenant TEXT, id TEXT, input_sha TEXT, result TEXT, PRIMARY KEY(tenant,id));
        CREATE TABLE IF NOT EXISTS quotes(tenant TEXT, id TEXT, delivery TEXT, expires REAL, PRIMARY KEY(tenant,id));
        CREATE TABLE IF NOT EXISTS resources(tenant TEXT, kind TEXT, id TEXT, state TEXT, payload TEXT, PRIMARY KEY(tenant,kind,id));
        CREATE TABLE IF NOT EXISTS settings(name TEXT PRIMARY KEY,value INTEGER);
        """)
        db.execute("INSERT OR IGNORE INTO settings VALUES('drop',?)", (int(drop_first_reply),))

    class Handler(BaseHTTPRequestHandler):
        def log_message(self, *args):
            pass

        def reply(self, status, value):
            raw = encode(value)
            self.send_response(status)
            self.send_header("Content-Type", "application/json")
            self.send_header("Cache-Control", "no-store")
            self.send_header("Content-Length", str(len(raw)))
            self.end_headers()
            self.wfile.write(raw)

        def authenticate(self):
            if not hmac.compare_digest(self.headers.get("Authorization", ""), "Bearer " + token):
                self.reply(403, {"error": "sandbox authentication required"})
                return False
            return True

        def packet(self):
            length = int(self.headers.get("Content-Length", "0"))
            if not 1 <= length <= 16384:
                raise ValueError("Bounded request required")
            return json.loads(self.rfile.read(length))

        def do_GET(self):
            if not self.authenticate():
                return
            try:
                operation = str(uuid.UUID(self.path.removeprefix("/v1/operations/")))
                tenant = str(uuid.UUID(self.headers["Dzoka-Tenant"]))
                with database(path) as db:
                    found = db.execute("SELECT result FROM operations WHERE tenant=? AND id=?", (tenant, operation)).fetchone()
                self.reply(200, json.loads(found[0]) if found else
                           {"schema": 1, "tenant": tenant, "operation": operation, "state": "NOT_FOUND", "reference": None})
            except (ValueError, KeyError, TypeError):
                self.reply(400, {"error": "invalid sandbox lookup"})

        def do_POST(self):
            if not self.authenticate():
                return
            self.connection.settimeout(3)
            try:
                packet = self.packet()
                tenant = str(uuid.UUID(packet["tenant"]))
                if packet["schema"] != 1 or tenant != self.headers.get("Dzoka-Tenant"):
                    raise ValueError("Scope mismatch")
                with database(path) as db:
                    db.execute("BEGIN IMMEDIATE")
                    if self.path == "/v1/quotes":
                        if set(packet) != {"schema", "tenant", "deliveryReference"} or not re.fullmatch(r"[A-Za-z0-9_-]{1,80}", packet["deliveryReference"]):
                            raise ValueError("Opaque delivery reference required")
                        reference = "sandbox-quote-" + uuid.uuid4().hex
                        db.execute("INSERT INTO quotes VALUES(?,?,?,?)", (tenant, reference, packet["deliveryReference"], time.time() + 300))
                        db.commit()
                        self.reply(200, {"reference": reference, "currency": "USD", "minorUnits": 250})
                        return
                    if self.path != "/v1/operations" or set(packet) != {"schema", "tenant", "operation", "kind", "payload"}:
                        raise ValueError("Versioned sandbox operation required")
                    operation = str(uuid.UUID(packet["operation"]))
                    if self.headers.get("Idempotency-Key") != operation:
                        raise ValueError("Stable operation key required")
                    checksum = sha256(encode(packet)).hexdigest()
                    old = db.execute("SELECT input_sha,result FROM operations WHERE tenant=? AND id=?", (tenant, operation)).fetchone()
                    if old:
                        if old[0] != checksum:
                            self.reply(409, {"error": "different operation input"})
                        else:
                            self.reply(200, json.loads(old[1]))
                        return
                    accepted = apply(db, tenant, packet["kind"], packet["payload"])
                    result = {"schema": 1, "tenant": tenant, "operation": operation,
                              "state": "CONFIRMED" if accepted else "REJECTED", "reference": "sandbox-" + operation}
                    db.execute("INSERT INTO operations VALUES(?,?,?,?)", (tenant, operation, checksum, encode(result).decode()))
                    drop = db.execute("SELECT value FROM settings WHERE name='drop'").fetchone()[0]
                    if drop:
                        db.execute("UPDATE settings SET value=0 WHERE name='drop'")
                    db.commit()
                if drop:
                    self.close_connection = True
                    return  # The synthetic effect committed, but the caller gets no receipt.
                self.reply(200, result)
            except (ValueError, KeyError, TypeError, OverflowError, json.JSONDecodeError):
                self.reply(400, {"error": "invalid sandbox operation"})

    server = ThreadingHTTPServer(("127.0.0.1", port), Handler)
    server.daemon_threads = True
    return server


def apply(db, tenant, kind, payload):
    if kind == "COURIER_BOOK":
        if set(payload) != {"shipment", "pickupIntakeReference", "deliveryReference", "quote"}:
            raise ValueError()
        entity = str(uuid.UUID(payload["shipment"]))
        uuid.UUID(payload["pickupIntakeReference"])
        quote = db.execute("SELECT 1 FROM quotes WHERE tenant=? AND id=? AND delivery=? AND expires>?",
                           (tenant, payload["quote"], payload["deliveryReference"], time.time())).fetchone()
        if not quote:
            return False
        old = db.execute("SELECT 1 FROM resources WHERE tenant=? AND kind='COURIER' AND id=?", (tenant, entity)).fetchone()
        if old:
            return False
        db.execute("INSERT INTO resources VALUES(?,?,?,?,?)", (tenant, "COURIER", entity, "BOOKED", encode(payload).decode()))
        return True
    if kind == "COURIER_CANCEL":
        if set(payload) != {"shipment"}:
            raise ValueError()
        return db.execute("UPDATE resources SET state='CANCELLED' WHERE tenant=? AND kind='COURIER' AND id=? AND state='BOOKED'",
                          (tenant, str(uuid.UUID(payload["shipment"])))).rowcount == 1
    if kind not in {"PAYMENT_RESERVE", "PAYMENT_RELEASE", "PAYMENT_REFUND", "PAYMENT_REVERSAL"}:
        raise ValueError()
    if set(payload) != {"reward", "beneficiaryReference", "currency", "minorUnits", "feeMinor", "termsSha"}:
        raise ValueError()
    entity = str(uuid.UUID(payload["reward"]))
    if (type(payload["minorUnits"]) is not int or type(payload["feeMinor"]) is not int
            or not 1 <= payload["minorUnits"] <= 10**12 or not 0 <= payload["feeMinor"] < payload["minorUnits"]
            or not re.fullmatch(r"[A-Z]{3}", payload["currency"])
            or any(not re.fullmatch(r"[0-9a-f]{64}", payload[name]) for name in ("termsSha", "beneficiaryReference"))):
        raise ValueError()
    old = db.execute("SELECT state,payload FROM resources WHERE tenant=? AND kind='PAYMENT' AND id=?", (tenant, entity)).fetchone()
    serialized = encode(payload).decode()
    if kind == "PAYMENT_RESERVE":
        if old:
            return False
        db.execute("INSERT INTO resources VALUES(?,?,?,?,?)", (tenant, "PAYMENT", entity, "RESERVED", serialized))
        return True
    required = "RELEASED" if kind == "PAYMENT_REVERSAL" else "RESERVED"
    if not old or old[0] != required or old[1] != serialized:
        return False
    db.execute("UPDATE resources SET state=? WHERE tenant=? AND kind='PAYMENT' AND id=?",
               ("REVERSED" if kind == "PAYMENT_REVERSAL" else "RELEASED" if kind == "PAYMENT_RELEASE" else "REFUNDED", tenant, entity))
    return True


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--state", type=Path, required=True)
    parser.add_argument("--port", type=int, default=0)
    parser.add_argument("--drop-first-reply", action="store_true")
    args = parser.parse_args()
    server = make_server(args.state, os.environ.get("DZOKA_SANDBOX_TOKEN"), args.port, args.drop_first_reply)
    print(json.dumps({"mode": "TEST_ONLY_NO_MONEY_OR_SHIPPING", "origin": "http://127.0.0.1:" + str(server.server_port)}), flush=True)
    try:
        server.serve_forever()
    finally:
        server.server_close()
