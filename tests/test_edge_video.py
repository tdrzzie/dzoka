import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "tools"))
from edge_vision import FrameArchive, TemplateTracker, search
from test_edge_vision import image

ROOT = Path(__file__).resolve().parents[1]


@unittest.skipUnless(os.environ.get("DZOKA_TEST_EDGE_SANDBOX") == "true", "requires the pinned isolated video runtime")
class EdgeVideoTests(unittest.TestCase):
    def test_real_compressed_clip_stays_local_and_decodes_only_indexed_samples(self):
        lock = json.loads((ROOT / "ai/edge-video.lock.json").read_bytes())
        with tempfile.TemporaryDirectory(dir=ROOT / ".test-tmp", prefix="edge-video-") as temporary:
            directory = Path(temporary).resolve()
            for index in range(3):
                (directory / f"frame-{index}.ppm").write_bytes(image(index < 2))
            command = ["docker", "run", "--rm", "--network", "none", "--read-only", "--user", "65534:65534",
                       "--cap-drop", "ALL", "--security-opt", "no-new-privileges", "--memory", "256m", "--cpus", "1",
                       "--pids-limit", "32", "--mount", f"type=bind,src={directory},dst=/data",
                       lock["imageId"], "-nostdin", "-v", "error", "-framerate", "1", "-i", "/data/frame-%d.ppm",
                       "-c:v", "ffv1", "-pix_fmt", "bgr0", "/data/clip.mkv"]
            subprocess.run(command, check=True, capture_output=True, timeout=30)
            digest = hashlib.sha256((directory / "clip.mkv").read_bytes()).hexdigest()
            entries = [{"time": f"2026-11-26T12:00:0{i}Z", "name": "clip.mkv", "sha256": digest, "offset": i}
                       for i in range(3)]
            archive = FrameArchive(directory, "camera", {"schema": 1, "camera": "camera", "frames": entries})
            reference = (directory / "frame-0.ppm").read_bytes()
            result = search(archive, TemplateTracker(reference, [4, 4, 6, 6], 0.04),
                            entries[0]["time"], entries[-1]["time"], [0, 0, 24, 18], 3, lambda: None)
            self.assertEqual(["PRESENT", "PRESENT", "NOT_OBSERVED"], [row["state"] for row in result["observations"]])
            self.assertEqual(1, len(result["windows"]))
            self.assertTrue((directory / "clip.mkv").exists())
            self.assertNotIn("pixels", json.dumps(result))
