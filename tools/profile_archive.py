"""Receipt-preserving terminal AppContainer metadata archival; no SDK deletion."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import sys
import subprocess
sys.path.insert(0,str(Path(__file__).resolve().parent))
from appcontainer_boundary import PREFIX
from docker_lifecycle import OwnerLock
from journal_archive import durable_new
from service_account import identity,validate,SID
from worker_service import read_json
from service_host import plain,check_acl
from broker_bundle import protect

ARCHIVER='00000000000000000000000000000003'
MAX_LIVE=2000
MAX_ARCHIVE=100000

def protected(path):
    path=plain(path);check_acl(path)
    for parent in path.parents:check_acl(parent,False,True)
    return path

def load(path,account):
    path=plain(path)
    with path.open('rb') as source:raw=source.read(4097)
    if len(raw)>4096:raise ValueError('Profile record exceeds bound')
    # Use the existing duplicate/nonfinite-rejecting deployment parser.
    value=read_json(path,4096);owner=value.get('owner') if isinstance(value,dict) else None
    if not isinstance(owner,str) or not re.fullmatch('[a-f0-9]{32}',owner) or set(value)!={'schemaVersion','accountSid','owner','name','state'} or type(value['schemaVersion']) is not int or value['schemaVersion']!=1 or value['accountSid']!=account or not SID.fullmatch(account) or value['name']!=PREFIX+owner or path.name!='offline-'+owner+'.json' or value['state'] not in ('INTENT','CREATED','REMOVED'):
        raise ValueError('Invalid profile ownership')
    if json.loads(raw)!=value:raise ValueError('Profile record changed during read')
    return raw,value

def audit_records(root):
    executable=Path(os.environ['SYSTEMROOT'])/'System32/WindowsPowerShell/v1.0/powershell.exe'
    script=Path(__file__).with_name('managed_state_acl.ps1')
    env={k:v for k,v in os.environ.items() if k in {'SYSTEMROOT','WINDIR','PATH','TEMP','TMP'}}
    result=subprocess.run([str(executable),'-NoProfile','-NonInteractive','-ExecutionPolicy','Bypass','-File',str(script),'-Target',str(root)],env=env,stdout=subprocess.PIPE,stderr=subprocess.DEVNULL,check=True,timeout=120,creationflags=subprocess.CREATE_NO_WINDOW)
    if len(result.stdout)>1024 or json.loads(result.stdout).get('safe') is not True:raise ValueError('State leaf ACL is not private')

def receipt(name,raw):
    return (json.dumps({'schemaVersion':1,'name':name,'bytes':len(raw),'sha256':hashlib.sha256(raw).hexdigest()},sort_keys=True)+'\n').encode()

def verify(target,account):
    target=protected(target);audit_records(target);paths=list(target.iterdir())
    if len(paths)>MAX_ARCHIVE*2:raise ValueError('Profile archive capacity')
    journals={p.name:p for p in paths if re.fullmatch('offline-[a-f0-9]{32}\\.json',p.name)}
    if {p.name for p in paths}!=set(journals)|{name.removesuffix('.json')+'.receipt.json' for name in journals}:raise ValueError('Unexpected or orphaned archive files')
    for name,path in journals.items():
        raw,value=load(path,account)
        proof=path.with_name(name.removesuffix('.json')+'.receipt.json');plain(proof)
        with proof.open('rb') as source:content=source.read(1025)
        if value['state']!='REMOVED' or content!=receipt(name,raw):raise ValueError('Profile archive integrity')
    return len(journals)

def archive(root,account,keep=64,batch=1000,after_receipt=lambda:None):
    if type(keep) is not int or not 0<=keep<=900 or type(batch) is not int or not 1<=batch<=2000:raise ValueError('Invalid profile archival policy')
    root=protected(root);audit_records(root);lease=OwnerLock(root,ARCHIVER)
    if not lease.acquire():raise OSError('Profile archiver active')
    try:
        target=root/'archive'
        if not target.exists():
            target.mkdir();plain(target);protect(target)
        protected(target)
        audit_records(target)
        entries=list(target.iterdir())
        if len(entries)>MAX_ARCHIVE*2:raise ValueError('Profile archive capacity')
        paths=sorted(root.glob('offline-*.json'))
        if len(paths)>MAX_LIVE:raise ValueError('Profile live-state capacity')
        live={p.name:p for p in paths}
        for entry in entries:
            if not re.fullmatch('offline-[a-f0-9]{32}(?:\\.receipt)?\\.json',entry.name):raise ValueError('Unexpected archive entry')
            name=entry.name.replace('.receipt.json','.json');journal=target/name;proof=target/(name.removesuffix('.json')+'.receipt.json')
            if journal.exists():
                archived,value=load(journal,account)
                if value['state']!='REMOVED':raise ValueError('Unresolved archive record')
            elif name in live:archived,value=load(live[name],account)
            else:raise ValueError('Orphan archive receipt')
            if proof.exists():
                plain(proof)
                if proof.read_bytes()!=receipt(name,archived):raise ValueError('Conflicting archive receipt')
            elif name not in live or load(live[name],account)[0]!=archived:raise ValueError('Orphan archive record')
        result={'archived':0,'active':0,'retained':0}
        terminals=[]
        for path in paths:
            raw,value=load(path,account)
            if value['state']!='REMOVED' or (root/('grant-'+value['owner']+'.json')).exists():result['retained']+=1
            else:terminals.append((path,raw,value))
        # Deterministic UUID order; this is not a claim of chronological recency.
        selected=terminals[:max(0,len(terminals)-keep)][:batch]
        result['retained']+=len(terminals)-len(selected)
        for path,raw,value in selected:
            lock=OwnerLock(root,value['owner'])
            if not lock.acquire():result['active']+=1;continue
            try:
                current,now=load(path,account)
                if current!=raw or now['state']!='REMOVED' or (root/('grant-'+now['owner']+'.json')).exists():raise ValueError('Profile changed before archival')
                destination=target/path.name;proof=target/(path.stem+'.receipt.json')
                additional=int(not destination.exists())+int(not proof.exists())
                if len(entries)+additional>MAX_ARCHIVE*2:raise ValueError('Profile archive capacity')
                durable_new(destination,raw);durable_new(proof,receipt(path.name,raw))
                entries.extend([None]*additional)
                after_receipt()
                if load(path,account)[0]!=raw or load(destination,account)[0]!=raw:raise ValueError('Profile changed before acknowledgement')
                plain(proof)
                if proof.read_bytes()!=receipt(path.name,raw):raise ValueError('Profile receipt changed')
                path.unlink();result['archived']+=1
            finally:lock.close()
        return result
    finally:lease.close()

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('operation',choices=['archive','verify']);parser.add_argument('--state',required=True);parser.add_argument('--keep',type=int,default=64);args=parser.parse_args()
    try:
        from managed_release import verify_sources
        verify_sources()
        who=identity();validate(who,who['sid'],who['integrity'])
        value=archive(args.state,who['sid'],args.keep) if args.operation=='archive' else {'verified':verify(Path(args.state)/'archive',who['sid'])}
        print(json.dumps(value))
    except Exception:print('Profile archive unavailable; retain evidence and inspect protected state',file=sys.stderr);raise SystemExit(1)
