"""Bounded local camera-frame search with an appearance provider and explicit gaps.

The portable provider consumes uncompressed P6 camera exports. Compressed video
decoders can supply the same indexed frame port inside their reviewed sandbox.
"""
from dataclasses import dataclass
from datetime import datetime, timezone
from hashlib import sha256
from pathlib import Path
import json
import math
import base64
import subprocess
import uuid


VERSION = "edge-template-grid-v1"
RUNTIME = "python@sha256:d13fa0424035d290decef3d575cea23d1b7d5952cdf429df8f5542c71e961576"
MAX_PIXELS = 320 * 240
MAX_FRAMES = 100


def utc(value):
    result = datetime.fromisoformat(value.replace("Z", "+00:00"))
    if result.tzinfo is None:
        raise ValueError("UTC frame time required")
    return result.astimezone(timezone.utc)


@dataclass(frozen=True)
class Frame:
    time: str
    name: str
    digest: str
    offset: float | None = None


def ppm(raw):
    """Parse one deliberately narrow, bounded, uncompressed raster profile."""
    if len(raw) > MAX_PIXELS * 3 + 64:
        raise ValueError("Frame exceeds pixel budget")
    parts = raw.split(b"\n", 3)
    if len(parts) != 4 or parts[0] != b"P6" or parts[2] != b"255":
        raise ValueError("P6 frame required")
    dimensions = parts[1].split()
    if len(dimensions) != 2 or any(not word.isdigit() for word in dimensions):
        raise ValueError("Invalid dimensions")
    width, height = map(int, dimensions)
    if not 1 <= width <= 320 or not 1 <= height <= 240 or width * height > MAX_PIXELS:
        raise ValueError("Frame dimensions exceed budget")
    if len(parts[3]) != width * height * 3:
        raise ValueError("Frame length mismatch")
    return width, height, parts[3]


class FrameArchive:
    def __init__(self, directory, camera, manifest):
        original = Path(directory)
        if original.is_symlink() or getattr(original.lstat(), "st_file_attributes", 0) & 0x400:
            raise ValueError("Plain archive directory required")
        self.directory = original.resolve(strict=True)
        if not self.directory.is_dir():
            raise ValueError("Local archive directory required")
        if manifest.get("camera") != camera or manifest.get("schema") != 1:
            raise ValueError("Camera binding mismatch")
        entries = manifest.get("frames")
        if not isinstance(entries, list) or not 1 <= len(entries) <= 10000:
            raise ValueError("Bounded frame index required")
        self.frames = []
        self.cache = {}
        previous = None
        for entry in entries:
            if set(entry) not in ({"time", "name", "sha256"}, {"time", "name", "sha256", "offset"}):
                raise ValueError("Invalid frame entry")
            name, digest = entry["name"], entry["sha256"]
            if not isinstance(name, str) or not name or Path(name).name != name or any(c in name for c in "/\\:"):
                raise ValueError("Frame basename required")
            if not isinstance(digest, str) or len(digest) != 64 or any(c not in "0123456789abcdef" for c in digest):
                raise ValueError("Frame digest required")
            observed = utc(entry["time"])
            if previous is not None and observed <= previous:
                raise ValueError("Frame index must be strictly chronological")
            previous = observed
            offset = entry.get("offset")
            if offset is not None and (type(offset) not in (int, float) or not math.isfinite(offset) or not 0 <= offset <= 86400):
                raise ValueError("Bounded clip offset required")
            self.frames.append(Frame(entry["time"], name, digest, offset))

    def read(self, frame, authorize):
        authorize()  # Recheck remotely before each local read.
        cache_key = (frame.digest, frame.offset)
        if cache_key in self.cache:
            return self.cache[cache_key]
        path = self.directory / frame.name
        if not path.exists():
            raise FileNotFoundError("Indexed frame missing")
        if path.is_symlink() or getattr(path.lstat(), "st_file_attributes", 0) & 0x400 or not path.is_file() or path.resolve().parent != self.directory:
            raise ValueError("Frame unavailable")
        if frame.offset is None:
            with path.open("rb") as stream:
                raw = stream.read(MAX_PIXELS * 3 + 65)
            if sha256(raw).hexdigest() != frame.digest:
                raise ValueError("Frame content changed")
            decoded = ppm(raw)
        else:
            if path.stat().st_size > 64 * 1024 * 1024:
                raise ValueError("Clip exceeds size bound")
            with path.open("rb") as stream:
                if __import__("hashlib").file_digest(stream, "sha256").hexdigest() != frame.digest:
                    raise ValueError("Clip content changed")
            decoded = decode_video_frame(path, frame.offset)
        if len(self.cache) < MAX_FRAMES:
            self.cache[cache_key] = decoded
        return decoded


def decode_video_frame(path, offset):
    root = Path(__file__).resolve().parents[1]
    lock = json.loads((root / "ai/edge-video.lock.json").read_bytes())
    image = lock["imageId"]
    if not isinstance(image, str) or not image.startswith("sha256:") or len(image) != 71:
        raise ValueError("Pinned decoder image required")
    name = "dzoka-edge-video-" + uuid.uuid4().hex
    command = ["docker", "run", "--rm", "--name", name, "--network", "none", "--read-only",
               "--user", "65534:65534", "--cap-drop", "ALL", "--security-opt", "no-new-privileges",
               "--memory", "256m", "--cpus", "1", "--pids-limit", "32",
               "--mount", f"type=bind,src={path},dst=/input.clip,readonly", image,
               "-nostdin", "-v", "error", "-threads", "1", "-ss", str(offset), "-i", "/input.clip",
               "-frames:v", "1", "-an", "-sn", "-vf", "scale=w='min(320,iw)':h='min(240,ih)':force_original_aspect_ratio=decrease",
               "-f", "image2pipe", "-vcodec", "ppm", "-"]
    try:
        result = subprocess.run(command, capture_output=True, timeout=15)
        if result.returncode or len(result.stdout) > MAX_PIXELS * 3 + 64:
            raise ValueError("Video frame unavailable")
        return ppm(result.stdout)
    finally:
        subprocess.run(["docker", "rm", "--force", name], capture_output=True, timeout=10)


class TemplateTracker:
    """Original fixed-grid appearance matcher behind the edge provider port."""
    def __init__(self, reference, box, threshold=0.08):
        width, height, pixels = ppm(reference)
        x, y, w, h = box
        if any(type(value) is not int for value in box) or min(x, y) < 0 or min(w, h) < 2:
            raise ValueError("Invalid reference ROI")
        if x + w > width or y + h > height or w > 64 or h > 64:
            raise ValueError("Reference ROI outside bounds")
        if not math.isfinite(threshold) or not 0 < threshold < 0.25:
            raise ValueError("Invalid appearance threshold")
        self.samples = [(dx, dy, tuple(pixels[((y + dy) * width + x + dx) * 3:((y + dy) * width + x + dx) * 3 + 3]))
                        for dy in range(0, h, max(1, h // 8))
                        for dx in range(0, w, max(1, w // 8))]
        colors = {value for _, _, value in self.samples}
        if len(colors) < 3:
            raise ValueError("Reference lacks distinguishing appearance")
        self.size = (w, h)
        self.threshold = threshold
        self.last = None
        self.profile = sha256(json.dumps({
            "version": VERSION, "reference": sha256(reference).hexdigest(),
            "roi": box, "threshold": threshold, "sourceSha": sha256(Path(__file__).read_bytes()).hexdigest(),
            "runtime": RUNTIME,
        }, sort_keys=True).encode()).hexdigest()

    def detect(self, frame, roi):
        width, height, pixels = frame
        left, top, rw, rh = roi
        w, h = self.size
        if min(left, top) < 0 or min(rw, rh) < 1 or left + rw > width or top + rh > height:
            raise ValueError("Search ROI outside frame")
        if w > rw or h > rh:
            return {"state": "UNKNOWN", "reason": "ROI_TOO_SMALL"}
        # At most 512 candidate locations. Exact position is deliberately uncertain.
        stride = max(1, math.ceil(math.sqrt((rw - w + 1) * (rh - h + 1) / 512)))
        scored = []
        for y in range(top, top + rh - h + 1, stride):
            for x in range(left, left + rw - w + 1, stride):
                error = sum(abs(pixels[(y + dy) * width * 3 + (x + dx) * 3 + channel] - color[channel])
                            for dx, dy, color in self.samples for channel in range(3))
                scored.append((error / (len(self.samples) * 3 * 255), x, y))
        scored.sort()
        best, x, y = scored[0]
        if best > self.threshold:
            self.last = None
            return {"state": "NOT_OBSERVED", "reason": "OCCLUSION_OR_ABSENCE"}
        ambiguous = any(error <= best + 0.01 and (abs(cx - x) >= w or abs(cy - y) >= h)
                        for error, cx, cy in scored[1:])
        if ambiguous:
            self.last = None
            return {"state": "UNKNOWN", "reason": "AMBIGUOUS_APPEARANCE"}
        prior = self.last
        self.last = (x, y, w, h)
        return {"state": "PRESENT", "box": list(self.last), "continued": prior is not None,
                "pixelUncertainty": stride, "reason": "REFERENCE_APPEARANCE"}


class IsolatedTemplateTracker:
    IMAGE = RUNTIME

    def __init__(self, reference, box, threshold=0.08):
        provider = TemplateTracker(reference, box, threshold)
        self.reference = base64.b64encode(reference).decode()
        self.box, self.threshold, self.profile = box, threshold, provider.profile
        self.last = None

    def detect(self, frame, roi):
        width, height, pixels = frame
        name = "dzoka-edge-" + uuid.uuid4().hex
        source = Path(__file__).resolve()
        packet = json.dumps({
            "reference": self.reference, "box": self.box, "threshold": self.threshold,
            "width": width, "height": height, "pixels": base64.b64encode(pixels).decode(), "roi": roi,
        }).encode()
        command = ["docker", "run", "--rm", "-i", "--name", name, "--network", "none", "--read-only",
                   "--cap-drop", "ALL", "--security-opt", "no-new-privileges", "--user", "65534:65534",
                   "--memory", "128m", "--cpus", "1", "--pids-limit", "32",
                   "--mount", f"type=bind,src={source},dst=/edge_vision.py,readonly",
                   "--entrypoint", "python3", self.IMAGE, "-I", "-S", "/edge_vision.py"]
        try:
            result = subprocess.run(command, input=packet, capture_output=True, timeout=15)
            if result.returncode != 0 or len(result.stdout) > 4096:
                raise ValueError("Isolated edge provider failed")
            output = json.loads(result.stdout)
            if output.get("profile") != self.profile:
                raise ValueError("Edge profile changed")
            observation = output["observation"]
            if observation["state"] == "PRESENT":
                observation["continued"] = self.last is not None
                observation["track"] = self.profile[:32]
                self.last = observation["box"]
            else:
                self.last = None
            return observation
        finally:
            subprocess.run(["docker", "rm", "--force", name], capture_output=True, timeout=10)


def search(archive, provider, start, end, roi, budget, authorize):
    if not 1 <= budget <= MAX_FRAMES:
        raise ValueError("Frame budget outside grant")
    start, end = utc(start), utc(end)
    if start >= end:
        raise ValueError("Invalid search window")
    frames = [frame for frame in archive.frames if start <= utc(frame.time) <= end]
    if not frames:
        return {"profile": provider.profile, "state": "UNAVAILABLE", "observations": [], "windows": []}
    sampled = {}

    def inspect(index):
        frame = frames[index]
        try:
            raster = archive.read(frame, authorize)
        except FileNotFoundError:
            observation = {"state": "UNKNOWN", "reason": "FRAME_GAP"}
        else:
            observation = provider.detect(raster, roi)
        sampled[index] = {"time": frame.time, "sourceSha": frame.digest, **observation}

    # Coarse sampling never assumes object presence is monotonic.
    coarse_count = min(len(frames), budget, 9)
    indexes = sorted({round(i * (len(frames) - 1) / max(1, coarse_count - 1)) for i in range(coarse_count)})
    for index in indexes:
        inspect(index)
    while len(sampled) < min(budget, len(frames)):
        ordered = sorted(sampled)
        intervals = [(right - left, left, right) for left, right in zip(ordered, ordered[1:])
                     if right - left > 1 and (sampled[left]["state"] != sampled[right]["state"]
                                              or sampled[left]["state"] != "PRESENT")]
        if not intervals:
            break
        _, left, right = max(intervals)
        inspect((left + right) // 2)
    authorize()  # A revoked grant invalidates the result before export.
    ordered = sorted(sampled)
    windows = [{"after": frames[left].time, "before": frames[right].time, "hypothesis": "POSSIBLE_DISAPPEARANCE"}
               for left, right in zip(ordered, ordered[1:])
               if sampled[left]["state"] == "PRESENT" and sampled[right]["state"] != "PRESENT"]
    return {"profile": provider.profile, "state": "COMPLETE", "coverage": len(sampled) / len(frames),
            "observations": [sampled[index] for index in ordered], "windows": windows}


if __name__ == "__main__":
    import sys
    try:
        raw = sys.stdin.buffer.read(700001)
        if len(raw) > 700000:
            raise ValueError("Input exceeds budget")
        request = json.loads(raw)
        reference = base64.b64decode(request["reference"], validate=True)
        provider = TemplateTracker(reference, request["box"], request["threshold"])
        pixels = base64.b64decode(request["pixels"], validate=True)
        width, height = request["width"], request["height"]
        ppm(f"P6\n{width} {height}\n255\n".encode() + pixels)
        observation = provider.detect((width, height, pixels), request["roi"])
        print(json.dumps({"profile": provider.profile, "observation": observation}))
    except Exception:
        raise SystemExit(2)
