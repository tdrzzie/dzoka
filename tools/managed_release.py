"""Separate management-code release; existing model bundles remain immutable."""
import argparse
import hashlib
import json
from pathlib import Path
import sys
sys.path.insert(0,str(Path(__file__).resolve().parent))
from worker_service import verify_release as verify_base,read_json
from broker_bundle import sha,protect,copy_checked
from service_host import plain,check_acl,check_tree

ROOT=Path(__file__).resolve().parents[1]

def verify_sources():
    verify_base();path=ROOT/'ai/managed-service.lock.json';raw=path.read_bytes();lock=json.loads(raw)
    for field,name in [('baseServiceLockSha256','service'),('brokerLockSha256','broker')]:
        if sha(ROOT/('ai/'+name+'.lock.json'))!=lock[field]:raise ValueError('Management base changed')
    for name,digest in lock['sources'].items():
        if sha(ROOT/name)!=digest:raise ValueError('Management source changed')
    return hashlib.sha256(raw).hexdigest()

def prepare(target):
    identity=verify_sources();target=Path(target)
    if not target.is_absolute() or target.exists():raise ValueError('New management release path required')
    plain(target.parent)
    for parent in (target.parent,*target.parent.parents):check_acl(parent,False,True)
    target.mkdir();protect(target)
    for name in ('ai','tools','docs/dependencies'):
        for path in (ROOT/name).rglob('*'):
            if path.is_file() and '__pycache__' not in path.parts and path.suffix not in ('.pyc','.pyo'):
                copy_checked(path,target/path.relative_to(ROOT))
    files={p.relative_to(target).as_posix():{'bytes':p.stat().st_size,'sha256':sha(p)} for p in target.rglob('*') if p.is_file()}
    record={'schemaVersion':1,'managementLockSha256':identity,'files':files}
    (target/'release.json').write_bytes((json.dumps(record,sort_keys=True,indent=2)+'\n').encode());protect(target/'release.json');check_tree(target)
    return sha(target/'release.json')

def verify(expected):
    plain(ROOT);check_acl(ROOT)
    for parent in ROOT.parents:check_acl(parent,False,True)
    if not isinstance(expected,str) or len(expected)!=64 or sha(ROOT/'release.json')!=expected:raise ValueError('Management receipt changed')
    value=read_json(ROOT/'release.json',2*1024*1024)
    if value['schemaVersion']!=1 or value['managementLockSha256']!=verify_sources():raise ValueError('Management identity differs')
    actual={p.relative_to(ROOT).as_posix() for p in ROOT.rglob('*') if p.is_file()}
    if actual!=set(value['files'])|{'release.json'}:raise ValueError('Management inventory differs')
    for name,entry in value['files'].items():
        path=plain(ROOT/name)
        if path.stat().st_size!=entry['bytes'] or sha(path)!=entry['sha256']:raise ValueError('Management bytes changed')
    check_tree(ROOT)
    return value['managementLockSha256']

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--target',required=True);args=parser.parse_args()
    try:print(json.dumps({'releaseSha256':prepare(args.target)}))
    except Exception:print('Management staging failed; retain incomplete release',file=sys.stderr);raise SystemExit(1)
