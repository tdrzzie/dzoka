"""Explicit service lifecycle. No installation, secret output or inference fallback."""
import argparse
from datetime import datetime,timezone
import hashlib
import json
import os
from pathlib import Path
import signal
import sys
import threading
import time
import uuid
sys.path.insert(0,str(Path(__file__).resolve().parent))
from ai_supervisor import Supervisor,load_plan,verify_sources,unique_pairs,child_environment
from docker_lifecycle import OwnerLock,preflight,reconcile
from service_host import check_acl,check_tree,plain,HostLimits
ROOT=Path(__file__).resolve().parents[1]
INSTANCE='00000000000000000000000000000001'

def read_json(path,limit=65536):
    with Path(path).open('rb') as stream:raw=stream.read(limit+1)
    if len(raw)>limit:raise ValueError('Deployment file too large')
    return json.loads(raw,object_pairs_hook=unique_pairs,parse_constant=lambda _:(_ for _ in ()).throw(ValueError('Invalid JSON')))

def verify_release():
    lock=read_json(ROOT/'ai/service.lock.json')
    if lock['supervisorLockSha256']!=verify_sources():raise ValueError('Service supervisor changed')
    for name,digest in lock['sources'].items():
        if hashlib.sha256((ROOT/name).read_bytes()).hexdigest()!=digest:raise ValueError('Service source changed')
    return lock

def deployment(path):
    checked=set()
    def acl(target,private=True,ancestor=False):
        key=(str(target),private,ancestor)
        if key not in checked:check_acl(target,private,ancestor);checked.add(key)
    verify_release();path=plain(path);acl(path);acl(path.parent)
    config=read_json(path)
    if set(config)!={'schemaVersion','plan','credentials','stateDirectory'} or type(config['schemaVersion']) is not int or config['schemaVersion']!=1:raise ValueError('Invalid service configuration')
    for field in ('plan','credentials','stateDirectory'):
        config[field]=plain(config[field]);acl(config[field]);acl(config[field].parent)
    if not config['stateDirectory'].is_dir() or config['stateDirectory'] in (config['plan'].parent,config['credentials'].parent):raise ValueError('Separate service state required')
    # Parent directories can replace a protected file/directory; check their write access too.
    for target in (path,*config.values()):
        if isinstance(target,Path):
            for parent in target.parents:acl(parent,private=False,ancestor=True)
    inventory=verify_release()['sources']
    code=[ROOT/'ai/service.lock.json',ROOT/'ai/supervisor.lock.json',*(ROOT/n for n in inventory),*(ROOT/n for n in read_json(ROOT/'ai/supervisor.lock.json')['sources'])]
    for target in sorted(set(code)|{ROOT,ROOT/'tools',ROOT/'ai',Path(sys.executable)}):
        acl(target,private=False)
        for parent in target.parents:acl(parent,private=False,ancestor=True)
    plan,_=load_plan(config['plan']);credentials=read_json(config['credentials'])
    runtimes={ROOT/'.audit-cache'/('generative-venv' if e['family'] in ('generative','quality','shona-cpu') else 'ai-venv') for e in plan['entries']}
    # A replaceable interpreter/site package could execute before model integrity
    # checks. Protect all runtime files, including explicit child ACL exceptions.
    for runtime in sorted(runtimes|{Path(sys.base_prefix)}):
        if not runtime.is_dir():raise ValueError('Installed service runtime required')
        check_tree(runtime)
        for parent in runtime.parents:acl(parent,private=False,ancestor=True)
    for runtime in runtimes:acl(runtime/'Scripts/python.exe',private=False)
    expected={entry['tokenEnvironment'] for entry in plan['entries']}
    if not isinstance(credentials,dict) or set(credentials)!=expected:raise ValueError('Credential references differ')
    # Never inherit arbitrary service environment credentials into the supervisor.
    env={k:v for k,v in os.environ.items() if k in {'PATH','SYSTEMROOT','WINDIR','TEMP','TMP','HOME','LANG','SSL_CERT_FILE','SSL_CERT_DIR'}}
    env.update(credentials)
    for entry in plan['entries']:
        child_environment(plan,entry,env)
        if entry['family']=='shona-cpu':
            acl(entry['dockerStateDirectory'])
            for parent in Path(entry['dockerStateDirectory']).parents:acl(parent,private=False,ancestor=True)
    return config,plan,env

class ServiceLoop:
    def __init__(self,config,plan,environ,stop=None,supervisor=Supervisor,recover=reconcile,prepare=preflight):
        self.config=config;self.plan=plan;self.environ=environ;self.stop=stop or threading.Event();self.factory=supervisor;self.recover=recover;self.prepare=prepare
        self.run_id=uuid.uuid4().hex;self.sequence=0
    def status(self,phase):
        self.sequence+=1
        value={'schemaVersion':1,'run':self.run_id,'sequence':self.sequence,'phase':phase,'at':datetime.now(timezone.utc).isoformat()}
        state=self.config['stateDirectory'];temporary=state/(self.run_id+'.tmp')
        with temporary.open('wb') as stream:stream.write((json.dumps(value)+'\n').encode());stream.flush();os.fsync(stream.fileno())
        os.replace(temporary,state/'status.json')
    def run(self,started=lambda:None):
        lock=OwnerLock(self.config['stateDirectory'],INSTANCE)
        if not lock.acquire():raise OSError('Service instance already active')
        try:
            self.status('RECOVERING')
            directories=sorted({e['dockerStateDirectory'] for e in self.plan['entries'] if e['family']=='shona-cpu'})
            if directories:self.prepare()
            for directory in directories:
                result=self.recover(Path(directory))
                if result['unresolved'] or result['active']:raise OSError('Docker recovery incomplete')
            worker=self.factory(self.plan,self.environ,stop=self.stop)
            self.status('RUNNING');started()
            while not self.stop.is_set():
                worker.tick();self.status('RUNNING');self.stop.wait(1)
            self.status('STOPPED');return 0
        except Exception:
            self.status('UNAVAILABLE');raise
        finally:lock.close()

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--config',required=True);mode=parser.add_mutually_exclusive_group(required=True);mode.add_argument('--check',action='store_true');mode.add_argument('--foreground',action='store_true');mode.add_argument('--scm',action='store_true');args=parser.parse_args()
    try:
        if args.scm:
            from windows_service import dispatch
            return dispatch(lambda stop,started:execute(args.config,stop,started))
        config,plan,env=deployment(args.config)
        if args.check:print('PASS: protected service deployment/source/configuration; no jobs claimed');return 0
        limits=HostLimits();stop=threading.Event()
        for name in ('SIGINT','SIGTERM'):signal.signal(getattr(signal,name),lambda *_:stop.set())
        return ServiceLoop(config,plan,env,stop).run()
    except Exception:print('Worker service unavailable; inspect protected deployment and durable state',file=sys.stderr);return 1

def execute(path,stop,started):
    config,plan,env=deployment(path);limits=HostLimits()
    return ServiceLoop(config,plan,env,stop).run(started)

if __name__=='__main__':raise SystemExit(main())
