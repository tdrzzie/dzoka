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
    def test_probability_errors_unseen_targets_and_unavailable_states_keep_coverage(self):
        data = rows()
        model = train(data, "2026-11-02T00:00:00Z", "author", "a" * 64)
        test = []
        for i, (source, target) in enumerate([("bus", "depot"), ("bus", "other"), ("unknown", "depot")]):
            test.append({"item": f"test-{i}", "institution": "institution-b", "from": source, "to": target,
                         "before": "2026-11-03T12:00:00Z", "after": "2026-11-03T12:01:00Z",
                         "recorded": "2026-11-03T12:02:00Z", "sourceSha": f"{i + 10:064x}"})
        result = evaluate(model, data, test)
        self.assertEqual(2, result["available"])
        self.assertEqual(2 / 3, result["coverage"])
        self.assertEqual(.5, result["top1"])
        self.assertEqual(1, result["multiclassBrier"])
        self.assertEqual(1, result["unseenDestinationCases"])
        self.assertIsNone(result["bySourceState"]["unknown"]["multiclassBrier"])
        self.assertEqual(1, result["timeErrorCases"])
        self.assertEqual("UNAVAILABLE_COMPLETED_TRANSITIONS_ONLY", result["censoringEvaluation"])

    def test_relabelled_source_and_future_model_cutoff_cannot_evade_split(self):
        data = rows()
        model = train(data, "2026-11-02T00:00:00Z", "author", "a" * 64)
        test = [{**data[0], "item": "new", "institution": "new", "before": "2026-11-03T12:00:00Z",
                 "after": "2026-11-03T12:01:00Z", "recorded": "2026-11-03T12:02:00Z"}]
        with self.assertRaisesRegex(ValueError, "source evidence"):
            evaluate(model, data, test)
        test[0]["sourceSha"] = "f" * 64
        future = train(data, "2026-11-04T00:00:00Z", "author", "a" * 64)
        with self.assertRaisesRegex(ValueError, "cutoff"):
            evaluate(future, data, test)
        with self.assertRaises(ValueError):
            train(data, "2026-11-02T00:00:00Z", "author", "z" * 64)

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
