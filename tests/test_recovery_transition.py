import copy
import sys
from pathlib import Path
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "tools"))
from recovery_transition import train, predict, evaluate, approve


def rows():
    return [{"item": f"item-{i}", "institution": "institution-a", "from": "bus", "to": "depot",
             "before": "2026-11-01T12:00:00Z", "after": "2026-11-01T12:01:00Z",
             "recorded": "2026-11-01T12:02:00Z", "sourceSha": f"{i:064x}"} for i in range(3)]


class RecoveryTransitionTests(unittest.TestCase):
    def test_versioned_frequency_and_elapsed_time_preserve_sparse_uncertainty(self):
        data = rows()
        model = train(data, "2026-11-02T00:00:00Z", "author", "a" * 64)
        result = predict(model, "bus")
        self.assertEqual("PREDICTED", result["classification"])
        self.assertEqual(60, result["distribution"][0]["medianSeconds"])
        self.assertFalse(predict(model, "unknown")["available"])
        self.assertEqual("UNAVAILABLE", result["calibration"])
        changed = copy.deepcopy(model)
        changed["states"] = {}
        with self.assertRaises(ValueError):
            predict(changed, "bus")

    def test_split_leakage_future_information_and_self_approval_fail(self):
        data = rows()
        model = train(data, "2026-11-02T00:00:00Z", "author", "a" * 64)
        with self.assertRaises(ValueError):
            evaluate(model, data, data)
        with self.assertRaises(ValueError):
            train(data, "2026-10-01T00:00:00Z", "author", "a" * 64)
        test = [{"item": "other", "institution": "institution-b", "from": "bus", "to": "depot",
                 "before": "2026-11-03T12:00:00Z", "after": "2026-11-03T12:01:00Z",
                 "recorded": "2026-11-03T12:02:00Z", "sourceSha": "b" * 64}]
        evaluation = evaluate(model, data, test)
        self.assertEqual(1.0, evaluation["top1"])
        with self.assertRaises(ValueError):
            approve(model, evaluation, "author", "c" * 64)
        self.assertEqual("APPROVED_FOR_ASSISTED_SEARCH", approve(model, evaluation, "reviewer", "c" * 64)["state"])
