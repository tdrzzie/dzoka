"""Layered native runtime identity; older environments and locks remain untouched."""
import hashlib
import importlib.metadata
import json
from pathlib import Path
import sysconfig
import sys
import zipfile
from generative_integrity import verify_runtime as verify_base
ROOT=Path(__file__).resolve().parents[1]

def verify_runtime():
    if sys.platform!='win32':raise ValueError('Reviewed Windows Shona local-development runtime required')
    lock=json.loads((ROOT/'ai/shona-runtime.lock.json').read_bytes())
    if hashlib.sha256((ROOT/'ai/generative-runtime.lock.json').read_bytes()).hexdigest()!=lock['parentRuntimeLockSha256']:raise ValueError('Base runtime changed')
    for entry in lock['notices']:
        if hashlib.sha256((ROOT/entry['path']).read_bytes()).hexdigest()!=entry['sha256']:raise ValueError('Required notice changed')
    verify_base();site=Path(sysconfig.get_paths()['purelib']).resolve()
    for entry in lock['additionalWheels']:
        wheel=ROOT/'.audit-cache/shona-wheels'/entry['wheel']
        if importlib.metadata.version(entry['name'])!=entry['version'] or hashlib.sha256(wheel.read_bytes()).hexdigest()!=entry['sha256']:raise ValueError('Layered runtime mismatch')
        with zipfile.ZipFile(wheel) as bundle:
            for name in bundle.namelist():
                if name.endswith('/') or '.data/' in name or name.endswith('/RECORD'):continue
                target=(site/name).resolve()
                if not target.is_relative_to(site) or not target.is_file() or hashlib.sha256(target.read_bytes()).digest()!=hashlib.sha256(bundle.read(name)).digest():raise ValueError('Installed layered runtime changed')
