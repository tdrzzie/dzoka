import hashlib
import json
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "tools"))
from edge_vision import Frame, FrameArchive, TemplateTracker, ppm, search, redacted_crop, disclosure_mask, source_region


def image(present):
    width, height = 24, 18
    pixels = bytearray([220] * width * height * 3)
    if present:
        for y in range(4, 10):
            for x in range(4, 10):
                offset = (y * width + x) * 3
                pixels[offset:offset + 3] = bytes(((x * 31) % 255, (y * 29) % 255, (x * y * 11) % 255))
    return f"P6\n{width} {height}\n255\n".encode() + pixels


class EdgeVisionTests(unittest.TestCase):
    def test_source_region_requires_bounded_integer_coordinates(self):
        self.assertEqual((1, 2, 30, 40), source_region([1, 2, 30, 40]))
        for region in [None, [], [-1, 0, 10, 10], [0, 0, 1, 20], [0, 0, 10000, 10000],
                       [0, 0, "10", 10], [True, 0, 10, 10]]:
            with self.assertRaises(ValueError):
                source_region(region)

    def test_redaction_preserves_only_approved_pixels_before_export(self):
        frame = ppm(image(True))
        mask = ["000000", "011110", "011110", "011110", "011110", "000000"]
        crop = redacted_crop(frame, [4, 4, 6, 6], mask)
        for y in range(6):
            for x in range(6):
                value = crop[(y * 6 + x) * 3:(y * 6 + x + 1) * 3]
                source = ((y + 4) * frame[0] + x + 4) * 3
                self.assertEqual(frame[2][source:source + 3] if mask[y][x] == "1" else b"\0\0\0", value)
        for bad in [None, [], ["0" * 6] * 6, ["x" * 6] * 6, ["1" * 5] * 6]:
            with self.assertRaises(ValueError):
                disclosure_mask(bad, [6, 6])
        with self.assertRaises(ValueError):
            redacted_crop(frame, [23, 17, 6, 6], mask)

    def test_unknown_gap_does_not_create_disappearance_or_continue_track(self):
        with tempfile.TemporaryDirectory() as directory:
            raw = image(True)
            (Path(directory) / "frame.ppm").write_bytes(raw)
            entries = [{"time": f"2026-11-26T12:00:0{i}Z", "name": "missing.ppm" if i == 1 else "frame.ppm",
                        "sha256": hashlib.sha256(raw).hexdigest()} for i in range(3)]
            archive = FrameArchive(directory, "camera", {"schema": 1, "camera": "camera", "frames": entries})
            result = search(archive, TemplateTracker(raw, [4, 4, 6, 6]), entries[0]["time"], entries[-1]["time"],
                            [0, 0, 24, 18], 3, lambda: None)
            self.assertEqual([], result["windows"])
            self.assertEqual("UNKNOWN", result["observations"][1]["state"])
            self.assertFalse(result["observations"][2]["continued"])

    def test_backward_refinement_cannot_claim_continuity_across_absence(self):
        class Provider:
            profile = "test"
            def detect(self, frame, roi):
                state = "PRESENT" if frame[0] == 7 else "NOT_OBSERVED"
                return {"state": state, **({"continued": True} if state == "PRESENT" else {})}
        class Archive:
            frames = [Frame(f"2026-11-26T12:00:{i:02}Z", str(i), "0" * 64) for i in range(19)]
            def read(self, frame, authorize):
                return (int(frame.name), 1, b"")
        archive = Archive()
        result = search(archive, Provider(), archive.frames[0].time, archive.frames[-1].time,
                        [0, 0, 1, 1], 19, lambda: None)
        present = [row for row in result["observations"] if row["state"] == "PRESENT"]
        self.assertEqual(1, len(present))
        self.assertFalse(present[0]["continued"])

    def test_coarse_refinement_records_uncertain_disappearance_and_hashes(self):
        with tempfile.TemporaryDirectory() as directory:
            frames = []
            for index in range(17):
                raw = image(index < 8)
                name = f"frame-{index}.ppm"
                (Path(directory) / name).write_bytes(raw)
                frames.append({"time": f"2026-11-26T12:00:{index:02}Z", "name": name, "sha256": hashlib.sha256(raw).hexdigest()})
            archive = FrameArchive(directory, "camera", {"schema": 1, "camera": "camera", "frames": frames})
            provider = TemplateTracker(image(True), [4, 4, 6, 6], 0.04)
            calls = []
            result = search(archive, provider, frames[0]["time"], frames[-1]["time"], [0, 0, 24, 18], 13, lambda: calls.append(1))
            self.assertLessEqual(len(result["observations"]), 13)
            self.assertEqual(len(result["observations"]) + 1, len(calls))
            self.assertTrue(result["windows"])
            self.assertEqual("POSSIBLE_DISAPPEARANCE", result["windows"][0]["hypothesis"])
            self.assertTrue(any(value["state"] == "NOT_OBSERVED" for value in result["observations"]))
            self.assertNotIn("pixels", json.dumps(result))

    def test_revocation_stops_before_read(self):
        with tempfile.TemporaryDirectory() as directory:
            raw = image(True)
            (Path(directory) / "frame.ppm").write_bytes(raw)
            manifest = {"schema": 1, "camera": "camera", "frames": [{"time": "2026-11-26T12:00:00Z",
                        "name": "frame.ppm", "sha256": hashlib.sha256(raw).hexdigest()}]}
            archive = FrameArchive(directory, "camera", manifest)
            def revoked():
                raise PermissionError("revoked")
            with self.assertRaises(PermissionError):
                archive.read(archive.frames[0], revoked)
            with self.assertRaises(ValueError):
                FrameArchive(directory, "other-camera", manifest)
            archive.read(archive.frames[0], lambda: None)
            (Path(directory) / "frame.ppm").write_bytes(image(False))
            with self.assertRaises(ValueError):
                archive.read(archive.frames[0], lambda: None)

    def test_malformed_frames_and_outside_regions_fail_closed(self):
        with self.assertRaises(ValueError):
            ppm(b"P6\n999999 999999\n255\n")
        provider = TemplateTracker(image(True), [4, 4, 6, 6])
        with self.assertRaises(ValueError):
            provider.detect(ppm(image(True)), [0, 0, 1000, 1000])
        with self.assertRaises(ValueError):
            TemplateTracker(image(False), [4, 4, 6, 6])
