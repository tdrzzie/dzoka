import hashlib
import json
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "tools"))
from edge_vision import FrameArchive, TemplateTracker, ppm, search


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
