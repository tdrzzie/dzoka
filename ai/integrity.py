"""Verify installed runtime bytes against the reviewed wheels before importing native inference code."""
import hashlib
import importlib.metadata
import json
from pathlib import Path
import sys
import sysconfig
import zipfile

ROOT=Path(__file__).resolve().parents[1]

def verify_runtime():
    if sys.version_info[:2]!=(3,14):raise ValueError('Reviewed Python 3.14 runtime required')
    lock=json.loads((ROOT/'ai/runtime.lock.json').read_text(encoding='utf-8'))
    site=Path(sysconfig.get_paths()['purelib']).resolve()
    for entry in lock['wheels']:
        if importlib.metadata.version(entry['name'])!=entry['version']:raise ValueError('Runtime version mismatch')
        wheel=ROOT/'.audit-cache/ai-wheels'/entry['wheel']
        if hashlib.sha256(wheel.read_bytes()).hexdigest()!=entry['sha256']:raise ValueError('Wheel checksum mismatch')
        with zipfile.ZipFile(wheel) as bundle:
            for name in bundle.namelist():
                if name.endswith('/') or '.data/' in name or name.endswith('/RECORD'):continue
                target=(site/name).resolve()
                if not target.is_relative_to(site) or not target.is_file() or hashlib.sha256(target.read_bytes()).digest()!=hashlib.sha256(bundle.read(name)).digest():
                    raise ValueError('Installed runtime bytes changed')
