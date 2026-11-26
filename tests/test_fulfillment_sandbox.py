from pathlib import Path
import json
import sys
import tempfile
import threading
import unittest
import urllib.error
import urllib.request
import uuid
from concurrent.futures import ThreadPoolExecutor

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "tools"))
from fulfillment_sandbox import make_server, database


class FulfillmentSandboxTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.path = Path(self.temporary.name) / "synthetic.sqlite"
        self.token = uuid.uuid4().hex
        self.tenant = str(uuid.uuid4())
        self.server = None
        self.start()

    def start(self, drop=False):
        self.server = make_server(self.path, self.token, drop_first_reply=drop)
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()
        self.origin = "http://127.0.0.1:" + str(self.server.server_port)

    def stop(self):
        self.server.shutdown()
        self.server.server_close()
        self.thread.join(3)

    def tearDown(self):
        self.stop()
        self.temporary.cleanup()

    def exchange(self, path, packet=None, tenant=None, token=None):
        headers = {"Authorization": "Bearer " + (self.token if token is None else token),
                   "Dzoka-Tenant": tenant or self.tenant, "Content-Type": "application/json"}
        if packet and "operation" in packet:
            headers["Idempotency-Key"] = packet["operation"]
        request = urllib.request.Request(self.origin + path, data=None if packet is None else json.dumps(packet).encode(), headers=headers)
        with urllib.request.urlopen(request, timeout=3) as response:
            return json.load(response)

    def payment(self, kind="PAYMENT_RESERVE", payload=None):
        return {"schema": 1, "tenant": self.tenant, "operation": str(uuid.uuid4()), "kind": kind,
                "payload": payload or {"reward": str(uuid.uuid4()), "beneficiaryReference": "a" * 64,
                                       "currency": "USD", "minorUnits": 1000, "feeMinor": 100, "termsSha": "b" * 64}}

    def test_unknown_commit_reconciles_after_restart_without_second_effect(self):
        self.stop()
        with database(self.path) as db:
            db.execute("UPDATE settings SET value=1 WHERE name='drop'")
        self.start()
        packet = self.payment()
        with self.assertRaises((OSError, __import__("http.client", fromlist=["RemoteDisconnected"]).RemoteDisconnected)):
            self.exchange("/v1/operations", packet)
        self.stop()
        self.start()
        result = self.exchange("/v1/operations/" + packet["operation"])
        self.assertEqual("CONFIRMED", result["state"])
        self.assertEqual(result, self.exchange("/v1/operations", packet))
        changed = {**packet, "payload": {**packet["payload"], "minorUnits": 1200}}
        with self.assertRaises(urllib.error.HTTPError) as error:
            self.exchange("/v1/operations", changed)
        self.assertEqual(409, error.exception.code)
        error.exception.close()
        self.assertEqual("NOT_FOUND", self.exchange("/v1/operations/" + packet["operation"], tenant=str(uuid.uuid4()))["state"])
        with database(self.path) as db:
            self.assertEqual(1, db.execute("SELECT count(*) FROM resources").fetchone()[0])

    def test_concurrent_release_and_refund_choose_one_virtual_settlement(self):
        packet = self.payment()
        self.exchange("/v1/operations", packet)
        release = self.payment("PAYMENT_RELEASE", packet["payload"])
        refund = self.payment("PAYMENT_REFUND", packet["payload"])
        with ThreadPoolExecutor(2) as pool:
            results = list(pool.map(lambda body: self.exchange("/v1/operations", body)["state"], [release, refund]))
        self.assertEqual(["CONFIRMED", "REJECTED"], sorted(results))

    def test_integer_money_and_authenticated_transport_are_required(self):
        packet = self.payment()
        packet["payload"]["minorUnits"] = 1.5
        with self.assertRaises(urllib.error.HTTPError) as error:
            self.exchange("/v1/operations", packet)
        self.assertEqual(400, error.exception.code)
        error.exception.close()
        with self.assertRaises(urllib.error.HTTPError) as error:
            self.exchange("/v1/operations/" + packet["operation"], token="wrong")
        self.assertEqual(403, error.exception.code)
        error.exception.close()

    def test_quote_booking_and_cancellation_are_explicit_synthetic_effects(self):
        quote = self.exchange("/v1/quotes", {"schema": 1, "tenant": self.tenant, "deliveryReference": "sandbox-vault"})
        shipment = str(uuid.uuid4())
        packet = {"schema": 1, "tenant": self.tenant, "operation": str(uuid.uuid4()), "kind": "COURIER_BOOK",
                  "payload": {"shipment": shipment, "pickupIntakeReference": str(uuid.uuid4()), "deliveryReference": "sandbox-vault", "quote": quote["reference"]}}
        self.assertEqual("CONFIRMED", self.exchange("/v1/operations", packet)["state"])
        cancel = {**packet, "operation": str(uuid.uuid4()), "kind": "COURIER_CANCEL", "payload": {"shipment": shipment}}
        self.assertEqual("CONFIRMED", self.exchange("/v1/operations", cancel)["state"])

    def test_reversal_requires_completed_virtual_payout(self):
        packet = self.payment()
        self.exchange("/v1/operations", packet)
        release = self.payment("PAYMENT_RELEASE", packet["payload"])
        self.exchange("/v1/operations", release)
        reverse = self.payment("PAYMENT_REVERSAL", packet["payload"])
        self.assertEqual("CONFIRMED", self.exchange("/v1/operations", reverse)["state"])
        refund = self.payment("PAYMENT_REFUND", packet["payload"])
        self.assertEqual("REJECTED", self.exchange("/v1/operations", refund)["state"])
