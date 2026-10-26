"""Explicit reviewed layered environment bootstrap, never called by application startup."""
import hashlib
import json
from pathlib import Path
import subprocess
import sys
ROOT=Path(__file__).resolve().parents[1]

def main():
    lock=json.loads((ROOT/'ai/generative-runtime.lock.json').read_bytes())
    if sys.version_info[:2]!=(3,14) or sys.platform!='win32':raise ValueError('Reviewed Windows Python 3.14 required')
    if hashlib.sha256((ROOT/'ai/runtime.lock.json').read_bytes()).hexdigest()!=lock['baseRuntimeLockSha256']:raise ValueError('Base runtime changed')
    for entry in lock['notices']:
        if hashlib.sha256((ROOT/entry['path']).read_bytes()).hexdigest()!=entry['sha256']:raise ValueError('Missing license notice')
    entries=[(ROOT/'.audit-cache/ai-wheels',x) for x in json.loads((ROOT/'ai/runtime.lock.json').read_bytes())['wheels']]+[(ROOT/'.audit-cache/generative-wheels',x) for x in lock['additionalWheels']]
    for directory,entry in entries:
        if hashlib.sha256((directory/entry['wheel']).read_bytes()).hexdigest()!=entry['sha256']:raise ValueError('Reviewed wheel missing or changed')
    env=ROOT/'.audit-cache/generative-venv';python=env/'Scripts/python.exe'
    if not python.exists():subprocess.run([sys.executable,'-m','venv',str(env)],check=True,timeout=120)
    requirements=ROOT/'.test-tmp/generative-requirements.txt';requirements.parent.mkdir(exist_ok=True)
    requirements.write_text(''.join(e['name']+'=='+e['version']+' --hash=sha256:'+e['sha256']+'\n' for _,e in entries),encoding='utf-8')
    subprocess.run([str(python),'-m','pip','install','--no-index','--no-deps','--require-hashes','--find-links',str(ROOT/'.audit-cache/ai-wheels'),'--find-links',str(ROOT/'.audit-cache/generative-wheels'),'-r',str(requirements)],check=True,timeout=300)
    subprocess.run([str(python),'-I','-c',"import sys;sys.path.insert(0,'ai');from generative_integrity import verify_runtime;verify_runtime();print('PASS: separate 22-wheel runtime bytes and notices')"],cwd=ROOT,check=True,timeout=120)

if __name__=='__main__':main()
