"""Explicit private bundle preparation from already reviewed bytes; no downloads."""
import argparse
import hashlib
import json
import os
from pathlib import Path,PurePosixPath
import shutil
import subprocess
import sys
import zipfile
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'ai'))
from broker_profiles import MODEL_DIR,verify as verify_sources
from service_host import plain,check_acl,check_tree
from service_account import identity,validate

def sha(path):
    with Path(path).open('rb') as source:return hashlib.file_digest(source,'sha256').hexdigest()
def protect(path):
    who=identity();validate(who,who['sid'],who['integrity'])
    rights=':(OI)(CI)F' if path.is_dir() else ':F'
    subprocess.run(['icacls',str(path),'/inheritance:r','/grant:r','*'+who['sid']+rights,'*S-1-5-18'+rights,'*S-1-5-32-544'+rights],check=True,stdout=subprocess.DEVNULL)
def copy_checked(source,target,expected=None):
    source=Path(source);target.parent.mkdir(parents=True,exist_ok=True)
    if source.is_symlink() or getattr(source.stat(),'st_file_attributes',0)&0x400:raise ValueError('Reparse source')
    shutil.copyfile(source,target)
    digest=sha(target)
    if digest!=sha(source) or expected and (digest!=expected['sha256'] or target.stat().st_size!=expected['bytes']):raise ValueError('Bundle artifact changed')
def prepare(target):
    _,lock_sha=verify_sources();target=Path(target)
    if not target.is_absolute() or target.exists():raise ValueError('New absolute bundle directory required')
    plain(target.parent)
    for parent in (target.parent,*target.parent.parents):check_acl(parent,False,True)
    target.mkdir();protect(target)
    # Failed preparations are deliberately retained, never published READY.
    for folder in ('ai','tools','docs/dependencies'):
        source=ROOT/folder
        for path in source.rglob('*'):
            if not path.is_file() or '__pycache__' in path.parts or path.suffix in ('.pyc','.pyo'):continue
            copy_checked(path,target/path.relative_to(ROOT))
    runtime=target/'runtime';runtime.mkdir()
    from verify_worker_boundary import stage_python
    stage_python(runtime)
    (runtime/'offline_probe.py').unlink()
    (runtime/'python314._pth').write_bytes(b'python314.zip\nDLLs\nLib/site-packages\n../ai\n../tools\n.\n')
    host=json.loads((ROOT/'ai/broker.lock.json').read_bytes())['hostRuntime']
    if sys.version.split()[0]!=host['pythonVersion']:raise ValueError('Reviewed host Python changed')
    for name,entry in host['files'].items():
        path=target/name
        if path.stat().st_size!=entry['bytes'] or sha(path)!=entry['sha256']:raise ValueError('Reviewed host runtime bytes changed')
    site=runtime/'Lib/site-packages';site.mkdir(parents=True)
    for filename,cache,field in [('runtime.lock.json','ai-wheels','wheels'),('generative-runtime.lock.json','generative-wheels','additionalWheels')]:
        for entry in json.loads((ROOT/'ai'/filename).read_bytes())[field]:
            wheel=ROOT/'.audit-cache'/cache/entry['wheel']
            if sha(wheel)!=entry['sha256']:raise ValueError('Reviewed wheel changed')
            copy_checked(wheel,target/'.audit-cache'/cache/entry['wheel'])
            with zipfile.ZipFile(wheel) as bundle:
                for name in bundle.namelist():
                    if name.endswith('/') or '.data/' in name:continue
                    parts=PurePosixPath(name)
                    if parts.is_absolute() or '..' in parts.parts or '\\' in name or ':' in name:raise ValueError('Unsafe reviewed wheel path')
                    destination=site.joinpath(*parts.parts);destination.parent.mkdir(parents=True,exist_ok=True)
                    with destination.open('xb') as output:output.write(bundle.read(name))
    locks={'text':'model.lock.json','perception':'perception.lock.json','vision':'vision.lock.json','language':'language.lock.json','generative':'generative.lock.json','quality':'quality.lock.json','shona-cpu':'shona-cpu-models.lock.json'}
    for family,lock in locks.items():
        for entry in json.loads((ROOT/'ai'/lock).read_bytes())['files']:
            copy_checked(ROOT/'.audit-cache'/MODEL_DIR[family]/entry['local'],target/'.audit-cache'/MODEL_DIR[family]/entry['local'],entry)
    files={}
    for path in target.rglob('*'):
        if path.is_file():files[path.relative_to(target).as_posix()]={'bytes':path.stat().st_size,'sha256':sha(path)}
    manifest={'schemaVersion':1,'brokerLockSha256':lock_sha,'pythonVersion':sys.version.split()[0],'files':files}
    (target/'bundle.json').write_bytes((json.dumps(manifest,sort_keys=True,indent=2)+'\n').encode())
    protect(target/'bundle.json');check_tree(target)
    return sha(target/'bundle.json')

def verify_bundle(target,expected,full=True,pending_grants=False):
    # Inactive owned RX grants may survive broker death. Recovery may inspect
    # immutable bytes under a no-broad-write ACL, then must recheck strict
    # privacy after exact journal/SID reconciliation and before any claim.
    target=plain(target);check_acl(target,private=not pending_grants)
    for parent in target.parents:check_acl(parent,False,True)
    if not isinstance(expected,str) or len(expected)!=64 or sha(target/'bundle.json')!=expected:raise ValueError('Bundle receipt changed')
    manifest=json.loads((target/'bundle.json').read_bytes());lock,lock_sha=verify_sources()
    if manifest['schemaVersion']!=1 or manifest['brokerLockSha256']!=lock_sha:raise ValueError('Bundle source version differs')
    if manifest.get('pythonVersion')!=lock['hostRuntime']['pythonVersion'] or any(manifest['files'].get(n)!=v for n,v in lock['hostRuntime']['files'].items()):raise ValueError('Bundle host runtime identity differs')
    actual={p.relative_to(target).as_posix() for p in target.rglob('*') if p.is_file()}
    if actual!=set(manifest['files'])|{'bundle.json'}:raise ValueError('Bundle inventory differs')
    for name,entry in manifest['files'].items():
        path=plain(target/name)
        if path.stat().st_size!=entry['bytes'] or full and sha(path)!=entry['sha256']:raise ValueError('Bundle bytes changed')
    return manifest

def grant(target,sid):
    # Grants only to the private immutable bundle, never service state/secrets.
    if not sid.startswith('S-1-15-2-') or any(c not in 'S0123456789-' for c in sid):raise ValueError('Invalid package SID')
    subprocess.run(['icacls',str(target),'/grant','*'+sid+':(OI)(CI)RX','/T'],check=True,stdout=subprocess.DEVNULL,timeout=120)
def revoke(target,sid):
    subprocess.run(['icacls',str(target),'/remove:g','*'+sid,'/T'],check=True,stdout=subprocess.DEVNULL,timeout=120)

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('operation',choices=['prepare','check']);parser.add_argument('--bundle',required=True);parser.add_argument('--manifest-sha256');args=parser.parse_args()
    try:
        if args.operation=='prepare':print(json.dumps({'manifestSha256':prepare(Path(args.bundle))}))
        else:verify_bundle(Path(args.bundle),args.manifest_sha256);print('PASS: explicit private broker bundle')
    except Exception:print('Broker bundle unavailable; retain incomplete staging for inspection',file=sys.stderr);raise
