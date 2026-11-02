"""Opt-in serial broker maintenance, credential rotation and redacted counters."""
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
from worker_service import read_json,deployment,INSTANCE
from service_host import plain,check_acl,HostLimits
from service_account import identity,validate
from broker_plan import load_plan
from broker_supervisor import BrokerSupervisor
from broker_profiles import profiles
from model_broker import run_once,state_check
from docker_lifecycle import OwnerLock,preflight,reconcile
from profile_archive import archive,protected
from managed_release import verify as verify_release
from ai_supervisor import unique_pairs

def bounded(path):
    with Path(path).open('rb') as source:raw=source.read(65537)
    if len(raw)>65536:raise ValueError('Management control exceeds bound')
    return raw

class ManagedService:
    def __init__(self,policy,receipt,stop=None):
        self.path=Path(policy);self.receipt=receipt;self.stop=stop or threading.Event();self.lock=None
        self.run=uuid.uuid4().hex;self.sequence=0;self.generation=0;self.digest=None;self.entries=[]
        self.counts={name:0 for name in ('completedAcknowledgements','idleClaims','failedInvocations','pausedCycles','archivedProfiles','retainedProfiles','activeArchiveOwners')}

    def __enter__(self):
        self.release=verify_release(self.receipt);protected(self.path);policy=read_json(self.path)
        if not isinstance(policy,dict) or set(policy)!={'schemaVersion','serviceConfig','archiveAt','keepTerminal'} or type(policy['schemaVersion']) is not int or policy['schemaVersion']!=1 or type(policy['archiveAt']) is not int or not 2<=policy['archiveAt']<=900 or type(policy['keepTerminal']) is not int or not 0<=policy['keepTerminal']<policy['archiveAt']:raise ValueError('Invalid management policy')
        self.policy=policy;self.config_path=protected(policy['serviceConfig']);cfg=read_json(self.config_path)
        if not isinstance(cfg,dict) or set(cfg)!={'schemaVersion','accountSid','integrityRid','plan','credentials','stateDirectory'} or type(cfg['schemaVersion']) is not int or cfg['schemaVersion']!=3:raise ValueError('Managed service requires schema 3')
        who=identity();validate(who,cfg['accountSid'],cfg['integrityRid']);self.account=who['sid']
        for field in ('plan','credentials','stateDirectory'):cfg[field]=protected(cfg[field])
        if not cfg['stateDirectory'].is_dir() or cfg['stateDirectory'] in (cfg['plan'].parent,cfg['credentials'].parent):raise ValueError('Separate management status state required')
        self.config=cfg;self.plan=load_plan(cfg['plan'],preflight=False)
        controls=(self.path,self.config_path,cfg['plan'],cfg['credentials'],cfg['stateDirectory'])
        for entry in self.plan['entries']:
            bundle=Path(entry['bundle']).resolve()
            if any(p.resolve().is_relative_to(bundle) for p in controls):raise ValueError('Private controls overlap model bundle')
            protected(entry['stateDirectory']);protected(Path(entry['stateDirectory'])/'profiles');protected(Path(entry['stateDirectory'])/'docker')
        self.controls={p:hashlib.sha256(bounded(p)).digest() for p in (self.path,self.config_path,cfg['plan'])}
        self.lock=OwnerLock(cfg['stateDirectory'],INSTANCE)
        if not self.lock.acquire():self.lock=None;raise OSError('Service instance active')
        try:
            self.status('RECOVERING');self.maintenance()
            self.config,self.plan,_=deployment(self.config_path)
            directories={Path(e['stateDirectory'])/'docker' for e in self.plan['entries'] if profiles()[e['profileName']]['family']=='shona-cpu'}
            if directories:preflight()
            for directory in directories:
                outcome=reconcile(directory)
                if outcome['active'] or outcome['unresolved']:raise OSError('Native recovery incomplete')
            self.unchanged();self.refresh();self.status('RUNNING');return self
        except BaseException:
            self.status('UNAVAILABLE','STARTUP_FAILED');self.close();raise

    def unchanged(self):
        for path,digest in self.controls.items():
            protected(path)
            if hashlib.sha256(bounded(path)).digest()!=digest:raise ValueError('Management controls changed')

    def refresh(self):
        path=protected(self.config['credentials']);raw=bounded(path)
        values=json.loads(raw,object_pairs_hook=unique_pairs,parse_constant=lambda _:(_ for _ in ()).throw(ValueError('Invalid credential JSON')))
        expected={e['tokenEnvironment'] for e in self.plan['entries']}
        if not isinstance(values,dict) or set(values)!=expected:raise ValueError('Credential references differ')
        candidate=BrokerSupervisor(self.plan,values).entries
        digest=hashlib.sha256(raw).digest()
        if digest!=self.digest:
            previous={(e['tenant'],e['profile']):(token,state) for e,token,state in self.entries}
            for entry,token,state in candidate:
                old=previous.get((entry['tenant'],entry['profile']))
                if old and old[0]==token:state.update(old[1])
            self.entries=candidate;self.digest=digest;self.generation+=1

    def maintenance(self):
        retained=active=0
        for root in sorted({Path(e['stateDirectory'])/'profiles' for e in self.plan['entries']}):
            if len(list(root.glob('offline-*.json')))>=self.policy['archiveAt']:
                outcome=archive(root,self.account,self.policy['keepTerminal'],batch=2000)
                self.counts['archivedProfiles']+=outcome['archived'];active+=outcome['active']
            retained+=len(list(root.glob('offline-*.json')))
        self.counts['retainedProfiles']=retained;self.counts['activeArchiveOwners']=active

    def status(self,phase,reason=None):
        self.sequence+=1
        value={'schemaVersion':1,'run':self.run,'sequence':self.sequence,'at':datetime.now(timezone.utc).isoformat(),'phase':phase,'reason':reason,'credentialGeneration':self.generation,'managementLockSha256':self.release,**self.counts}
        state=self.config['stateDirectory'];temp=state/(self.run+'.managed.tmp')
        with temp.open('xb') as output:output.write((json.dumps(value,sort_keys=True)+'\n').encode());output.flush();os.fsync(output.fileno())
        os.replace(temp,state/'managed-status.json')
        return value

    def step(self,invoke=run_once,clock=time.monotonic):
        if self.stop.is_set():return self.status('STOPPED')
        try:self.unchanged();self.maintenance()
        except Exception:self.counts['pausedCycles']+=1;return self.status('UNAVAILABLE','CONTROLS_OR_MAINTENANCE')
        try:self.refresh()
        except Exception:self.counts['pausedCycles']+=1;return self.status('UNAVAILABLE','CREDENTIALS_INVALID')
        for index in range(len(self.entries)):
            if self.stop.is_set():break
            try:self.refresh()
            except Exception:self.counts['pausedCycles']+=1;return self.status('UNAVAILABLE','CREDENTIALS_INVALID')
            entry,token,state=self.entries[index]
            if state['due']>clock():continue
            # Recheck controls/credentials between invocations, never mid-lease.
            try:self.unchanged()
            except Exception:self.counts['pausedCycles']+=1;return self.status('UNAVAILABLE','CONTROLS_CHANGED')
            previous=os.environ.get('DZOKA_AI_ALLOW_INSECURE_LOOPBACK')
            try:
                if self.plan['allowInsecureLoopback']:os.environ['DZOKA_AI_ALLOW_INSECURE_LOOPBACK']='true'
                else:os.environ.pop('DZOKA_AI_ALLOW_INSECURE_LOOPBACK',None)
                result=invoke(self.plan['coreUrl'],token,entry['tenant'],entry['profile'],entry['profileName'],entry['bundle'],entry['bundleSha256'],entry['stateDirectory'])
                if type(result) is not bool:raise ValueError('Broker acknowledgement type')
                self.counts['completedAcknowledgements' if result else 'idleClaims']+=1;state['failures']=0
            except Exception:self.counts['failedInvocations']+=1;state['failures']=min(16,state['failures']+1)
            finally:
                if previous is None:os.environ.pop('DZOKA_AI_ALLOW_INSECURE_LOOPBACK',None)
                else:os.environ['DZOKA_AI_ALLOW_INSECURE_LOOPBACK']=previous
            state['due']=clock()+min(self.plan['backoffMaxSeconds'],self.plan['pollSeconds']*2**state['failures'])
        return self.status('STOPPED' if self.stop.is_set() else 'RUNNING')

    def close(self):
        if self.lock:self.lock.close();self.lock=None

    def __exit__(self,kind,*_):
        try:self.status('STOPPED' if kind is None else 'UNAVAILABLE',None if kind is None else 'LIFECYCLE_FAILED')
        finally:self.close()

def execute(policy,receipt,stop,started=lambda:None):
    with ManagedService(policy,receipt,stop) as service:
        limits=HostLimits();started()
        while not stop.is_set():service.step();stop.wait(service.plan['pollSeconds'])
    return 0

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--policy',required=True);parser.add_argument('--release-sha256',required=True);mode=parser.add_mutually_exclusive_group(required=True);mode.add_argument('--check',action='store_true');mode.add_argument('--foreground',action='store_true');mode.add_argument('--scm',action='store_true');args=parser.parse_args()
    try:
        if args.check:
            with ManagedService(args.policy,args.release_sha256):pass
            print('PASS: managed deployment and maintenance; no core claims');raise SystemExit(0)
        if args.scm:
            from windows_service import dispatch
            raise SystemExit(dispatch(lambda stop,started:execute(args.policy,args.release_sha256,stop,started)))
        stop=threading.Event()
        for name in ('SIGINT','SIGTERM'):signal.signal(getattr(signal,name),lambda *_:stop.set())
        raise SystemExit(execute(args.policy,args.release_sha256,stop))
    except Exception:print('Managed broker unavailable; inspect protected status and durable core state',file=sys.stderr);raise SystemExit(1)
