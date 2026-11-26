import copy
from datetime import datetime, timedelta, timezone
from pathlib import Path
import sys
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "tools"))
from recovery_survival import train, predict, evaluate


def row(index, duration, incomplete=False, test=False):
    start = datetime(2026, 11, 3 if test else 1, 12, tzinfo=timezone.utc)
    return {"item": f"item-{index}", "institution": "institution-b" if test else "institution-a", "from": "bus",
            "before": start.isoformat(), "recorded": (start + timedelta(seconds=120)).isoformat(),
            "sourceSha": f"{index:064x}",
            **({"through": (start + timedelta(seconds=duration)).isoformat()} if incomplete else
               {"to": "depot", "after": (start + timedelta(seconds=duration)).isoformat()})}


class RecoverySurvivalTests(unittest.TestCase):
    def model(self):
        completed, incomplete = [row(1, 10), row(2, 30)], [row(3, 20, True)]
        return train(completed, incomplete, "2026-11-02T00:00:00Z", "author", "a" * 64), completed, incomplete

    def test_risk_sets_keep_censored_items_until_withdrawal_and_do_not_extrapolate(self):
        model, _, _ = self.model()
        curve = model["states"]["bus"]["curve"]
        self.assertEqual([3, 2, 1], [point["atRisk"] for point in curve])
        self.assertAlmostEqual(2 / 3, predict(model, "bus", 15)["remainingWithoutTransition"])
        self.assertEqual(0, predict(model, "bus", 30)["remainingWithoutTransition"])
        self.assertFalse(predict(model, "bus", 31)["available"])
        self.assertFalse(predict(model, "new-state", 15)["available"])
        changed = copy.deepcopy(model)
        changed["states"] = {}
        with self.assertRaises(ValueError):
            predict(changed, "bus", 15)

    def test_censoring_before_horizon_is_unknown_not_a_failed_return(self):
        model, completed, incomplete = self.model()
        result = evaluate(model, completed, incomplete, [row(11, 10, test=True), row(12, 20, test=True)],
                          [row(13, 5, True, True)], 15)
        self.assertEqual(1, result["censoredBeforeHorizon"])
        self.assertEqual(2, result["scored"])
        self.assertAlmostEqual(5 / 18, result["observableHorizonBrier"])
        self.assertEqual(2 / 3, result["coverage"])
        self.assertEqual("UNAVAILABLE", result["calibration"])

    def test_future_cutoffs_and_item_institution_or_source_leakage_fail(self):
        model, completed, incomplete = self.model()
        for field in ("item", "institution", "sourceSha"):
            leaked = row(11, 10, test=True)
            leaked[field] = completed[0][field]
            with self.assertRaises(ValueError):
                evaluate(model, completed, incomplete, [leaked], [], 15)
        future = train(completed, incomplete, "2026-11-04T00:00:00Z", "author", "a" * 64)
        with self.assertRaises(ValueError):
            evaluate(future, completed, incomplete, [row(11, 10, test=True)], [], 15)
        with self.assertRaises(ValueError):
            train(completed, incomplete, "2026-10-01T00:00:00Z", "author", "a" * 64)

    def test_tied_events_use_risk_set_before_censoring_and_sparse_support_is_unavailable(self):
        model = train([row(1, 20), row(2, 30)], [row(3, 20, True)],
                      "2026-11-02T00:00:00Z", "author", "a" * 64)
        self.assertAlmostEqual(2 / 3, predict(model, "bus", 20)["remainingWithoutTransition"])
        sparse = train([row(1, 20)], [], "2026-11-02T00:00:00Z", "author", "a" * 64)
        self.assertFalse(predict(sparse, "bus", 15)["available"])
        with self.assertRaises(ValueError):
            predict(model, "bus", float("nan"))

    def test_repeated_items_and_source_hashes_cannot_multiply_support(self):
        for field in ("item", "sourceSha"):
            duplicate = row(3, 20, True)
            duplicate[field] = row(1, 10)[field]
            with self.assertRaises(ValueError):
                train([row(1, 10)], [duplicate], "2026-11-02T00:00:00Z", "author", "a" * 64)
        model, completed, incomplete = self.model()
        first, repeated = row(11, 10, test=True), row(12, 20, test=True)
        repeated["item"] = first["item"]
        with self.assertRaises(ValueError):
            evaluate(model, completed, incomplete, [first, repeated], [], 15)
