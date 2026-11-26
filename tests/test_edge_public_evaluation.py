import hashlib
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "tools"))
from edge_public_evaluation import checked_file, metrics


class PublicEdgeEvaluationTests(unittest.TestCase):
    def test_abstention_and_uncertain_labels_are_not_true_negatives(self):
        rows = [{"label": label, "observation": {"state": predicted}}
                for label, predicted in [("PRESENT", "UNKNOWN"), ("NOT_OBSERVED", "PRESENT"),
                                         ("UNKNOWN", "NOT_OBSERVED")]]
        result = metrics(rows)
        self.assertEqual(0, result["tn"])
        self.assertEqual(1, result["fp"])
        self.assertEqual(1, result["abstained"])
        self.assertEqual(1, result["uncertainLabels"])
        self.assertEqual(0, result["recallIncludingAbstentions"])
        self.assertEqual(.5, result["decisionCoverage"])

    def test_inputs_cannot_escape_or_change_and_iou_is_measured(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "clip").write_bytes(b"example")
            expected = hashlib.sha256(b"example").hexdigest()
            self.assertEqual(root / "clip", checked_file(root, "clip", expected))
            for name, checksum in [("../clip", expected), ("clip", "0" * 64)]:
                with self.assertRaises(ValueError):
                    checked_file(root, name, checksum)
        result = metrics([{"label": "PRESENT", "box": [0, 0, 2, 2],
                           "observation": {"state": "PRESENT", "box": [1, 0, 2, 2]}}])
        self.assertAlmostEqual(1 / 3, result["meanBoxIoU"])
