"""Loss-averse terminal journal archival. No Docker operations or retention deletion."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import sys
import uuid
sys.path.insert(0,str(Path(__file__).resolve().parent))
from docker_lifecycle import OwnerLock,validate_record

ARCHIVER='00000000000000000000000000000002'

def ordinary(path):
    if path.is_symlink() or getattr(path.lstat(),'st_file_attributes',0)&0x400:raise ValueError('Reparse archive path')

def load(path):
    ordinary(path)
    with path.open('rb') as source:raw=source.read(32769)
    if len(raw)>32768:raise ValueError('Oversize journal')
    def pairs(values):
        result={}
        for key,value in values:
            if key in result:raise ValueError('Duplicate journal field')
            result[key]=value
        return result
    record=validate_record(json.loads(raw,object_pairs_hook=pairs))
    if record['name']+'.json'!=path.name:raise ValueError('Journal name differs')
    return raw,record

def durable_new(path,raw):
    if path.exists():
        ordinary(path)
        with path.open('rb') as stream:existing=stream.read(len(raw)+1)
        if existing!=raw:raise ValueError('Conflicting archive bytes')
        return
    with path.open('xb') as stream:stream.write(raw);stream.flush();os.fsync(stream.fileno())

def receipt(name,raw):
    return (json.dumps({'schemaVersion':1,'name':name,'bytes':len(raw),'sha256':hashlib.sha256(raw).hexdigest()},sort_keys=True)+'\n').encode()

def verify(root):
    root=Path(root);ordinary(root);paths=list(root.glob('dzoka-owned-*.json'))
    if len(paths)>200000:raise ValueError('Archive capacity exceeded')
    count=0
    for path in paths:
        if path.name.endswith('.receipt.json'):
            if not path.with_name(path.name.removesuffix('.receipt.json')+'.json').exists():raise ValueError('Receipt has no journal')
            continue
        raw,record=load(path)
        if record['history'][-1]['state']!='REMOVED':raise ValueError('Archive contains unresolved journal')
        proof=path.with_name(record['name']+'.receipt.json');ordinary(proof)
        if proof.read_bytes()!=receipt(record['name'],raw):raise ValueError('Archive integrity mismatch')
        count+=1
    return count

def archive(root,after_receipt=lambda:None):
    root=Path(root)
    if not root.is_absolute() or not root.is_dir():raise ValueError('Explicit journal directory required')
    ordinary(root);lease=OwnerLock(root,ARCHIVER)
    if not lease.acquire():raise OSError('Archival already active')
    try:
        target=root/'archive'
        if not target.exists():target.mkdir()
        ordinary(target)
        paths=list(root.glob('dzoka-owned-*.json'))
        if len(paths)>10000:raise ValueError('Active directory capacity exceeded')
        archive_entries=len(list(target.glob('dzoka-owned-*.json')))
        if archive_entries>200000:raise ValueError('Archive capacity exceeded')
        result={'archived':0,'active':0,'retained':0}
        for path in paths:
            raw,record=load(path)
            if record['history'][-1]['state']!='REMOVED':result['retained']+=1;continue
            lock=OwnerLock(root,record['owner'])
            if not lock.acquire():result['active']+=1;continue
            try:
                current,record=load(path)
                if current!=raw:raise ValueError('Journal changed')
                proof=target/(record['name']+'.receipt.json')
                new_entries=int(not (target/path.name).exists())+int(not proof.exists())
                if archive_entries+new_entries>200000:raise ValueError('Archive capacity exceeded')
                durable_new(target/path.name,raw)
                durable_new(proof,receipt(record['name'],raw));archive_entries+=new_entries
                after_receipt() # Fault-injection seam: both copies survive a crash here.
                if load(path)[0]!=raw:raise ValueError('Journal changed before archive acknowledgement')
                if load(target/path.name)[0]!=raw or proof.read_bytes()!=receipt(record['name'],raw):raise ValueError('Archive changed before acknowledgement')
                path.unlink();result['archived']+=1
            finally:lock.close()
        return result
    finally:lease.close()

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('operation',choices=['archive','verify']);parser.add_argument('--state',required=True);args=parser.parse_args()
    try:
        from worker_service import verify_release
        from service_host import check_acl,plain
        verify_release();root=plain(args.state);check_acl(root)
        for parent in root.parents:check_acl(parent,False,True)
        if args.operation=='archive':print(json.dumps(archive(root)))
        else:print(json.dumps({'verified':verify(root/'archive')}))
    except Exception:print('Journal archive unavailable; retain all copies and inspect protected state',file=sys.stderr);raise SystemExit(1)
