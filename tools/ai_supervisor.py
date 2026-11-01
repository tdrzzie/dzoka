"""Explicit private-worker polling; core leases/results retain all authority."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import signal
import subprocess
import sys
import threading
import time
import urllib.parse
import uuid
sys.path.insert(0,str(Path(__file__).resolve().parent))
from worker_process import run_owned
ROOT=Path(__file__).resolve().parents[1]
# script, model directory variable, profile/space variable, complete child-tree deadline
FAMILIES={
 'text':('ai_worker.py','DZOKA_AI_MODEL_DIR','DZOKA_AI_SPACE',110),
 'perception':('perception_worker.py','DZOKA_AI_PERCEPTION_DIR','DZOKA_AI_PROFILE',110),
 'vision':('vision_worker.py','DZOKA_AI_VISION_DIR','DZOKA_AI_PROFILE',110),
 'language':('language_worker.py','DZOKA_AI_LANGUAGE_DIR','DZOKA_AI_PROFILE',110),
 'generative':('generative_worker.py','DZOKA_AI_GENERATIVE_DIR','DZOKA_AI_PROFILE',290),
 'quality':('quality_worker.py','DZOKA_AI_QUALITY_DIR','DZOKA_AI_PROFILE',290)}
SAFE_ENV={'PATH','SYSTEMROOT','WINDIR','TEMP','TMP','HOME','LANG','LC_ALL','SSL_CERT_FILE','SSL_CERT_DIR'}

def require(ok):
    if not ok:raise ValueError('Invalid private worker plan')

def verify_sources(expected=None):
    raw=(ROOT/'ai/supervisor.lock.json').read_bytes();identity=hashlib.sha256(raw).hexdigest()
    if expected is not None and identity!=expected:raise ValueError('Supervisor identity changed')
    lock=json.loads(raw)
    for name,digest in lock['sources'].items():
        if hashlib.sha256((ROOT/name).read_bytes()).hexdigest()!=digest:raise ValueError('Supervisor implementation changed')
    return identity

def unique_pairs(pairs):
    result={}
    for key,value in pairs:
        require(key not in result);result[key]=value
    return result

def load_plan(path):
    with Path(path).open('rb') as source:raw=source.read(32769)
    require(len(raw)<=32768)
    plan=json.loads(raw,object_pairs_hook=unique_pairs,parse_constant=lambda _:require(False))
    require(isinstance(plan,dict) and set(plan)=={'schemaVersion','coreUrl','allowInsecureLoopback','pollSeconds','backoffMaxSeconds','entries'})
    require(type(plan['schemaVersion']) is int and plan['schemaVersion']==1)
    require(isinstance(plan['coreUrl'],str) and len(plan['coreUrl'])<=2048 and all(ord(c)>32 and ord(c)<127 for c in plan['coreUrl']))
    uri=urllib.parse.urlparse(plan['coreUrl'])
    require(bool(uri.hostname) and not(uri.username or uri.password or uri.query or uri.fragment) and uri.path in ('','/') and '\\' not in plan['coreUrl'])
    require(type(plan['allowInsecureLoopback']) is bool)
    require(uri.scheme=='https' or uri.scheme=='http' and uri.hostname in ('127.0.0.1','localhost') and plan['allowInsecureLoopback'])
    require(type(plan['pollSeconds']) is int and 1<=plan['pollSeconds']<=300)
    require(type(plan['backoffMaxSeconds']) is int and plan['pollSeconds']<=plan['backoffMaxSeconds']<=3600)
    require(isinstance(plan['entries'],list) and 1<=len(plan['entries'])<=16)
    seen=set()
    for entry in plan['entries']:
        require(isinstance(entry,dict) and set(entry)=={'family','tenant','profile','modelDirectory','tokenEnvironment'})
        require(isinstance(entry['family'],str) and entry['family'] in FAMILIES)
        for field in ('tenant','profile'):require(isinstance(entry[field],str) and str(uuid.UUID(entry[field]))==entry[field])
        require(isinstance(entry['modelDirectory'],str) and len(entry['modelDirectory'])<=2048 and Path(entry['modelDirectory']).is_absolute() and Path(entry['modelDirectory']).is_dir())
        require(isinstance(entry['tokenEnvironment'],str) and re.fullmatch(r'DZOKA_WORKER_TOKEN_[A-Z0-9_]{1,48}',entry['tokenEnvironment']) is not None)
        key=(entry['tenant'],entry['family'],entry['profile']);require(key not in seen);seen.add(key)
    return plan,hashlib.sha256(raw).hexdigest()

def child_environment(plan,entry,environ):
    token=environ.get(entry['tokenEnvironment'],'')
    require(isinstance(token,str) and 1<=len(token)<=16384 and all(32<ord(c)<127 for c in token))
    family=FAMILIES[entry['family']]
    env={k:v for k,v in environ.items() if k in SAFE_ENV}
    env.update(PYTHONNOUSERSITE='1',PYTHONUTF8='1',DZOKA_AI_CORE_URL=plan['coreUrl'],DZOKA_AI_TOKEN=token,DZOKA_AI_TENANT=entry['tenant'])
    env[family[1]]=str(Path(entry['modelDirectory']).resolve());env[family[2]]=entry['profile']
    if plan['allowInsecureLoopback']:env['DZOKA_AI_ALLOW_INSECURE_LOOPBACK']='true'
    return env

class Supervisor:
    def __init__(self,plan,environ,stop=None,clock=time.monotonic,invoke=run_owned,emit=None):
        self.identity=verify_sources()
        self.plan=plan;self.stop=stop if stop is not None else threading.Event();self.clock=clock;self.invoke=invoke;self.emit=emit
        # Validate every credential before launching any worker; snapshot instead of inheriting mutable environment.
        self.environments=[child_environment(plan,e,environ) for e in plan['entries']]
        self.states=[{'invocations':0,'successfulInvocations':0,'failedInvocations':0,'deadlines':0,'consecutiveFailures':0,'due':0.0} for _ in plan['entries']]
    def tick(self):
        launched=0
        for index,(entry,state,env) in enumerate(zip(self.plan['entries'],self.states,self.environments)):
            if self.stop.is_set():break
            if self.clock()<state['due']:continue
            command=[sys.executable,'-I',str(ROOT/'tools/ai_supervisor_child.py'),entry['family'],self.identity]
            try:code=self.invoke(command,env,ROOT,FAMILIES[entry['family']][3])
            except (OSError,subprocess.SubprocessError):code=125
            launched+=1;state['invocations']+=1
            if code==0:state['successfulInvocations']+=1;state['consecutiveFailures']=0
            else:
                state['failedInvocations']+=1;state['consecutiveFailures']=min(16,state['consecutiveFailures']+1)
                if code==124:state['deadlines']+=1
            delay=min(self.plan['backoffMaxSeconds'],self.plan['pollSeconds']*(2**state['consecutiveFailures']))
            state['due']=self.clock()+delay
            if self.emit:self.emit({'event':'WORKER_INVOCATION','entry':index,'outcome':'OK' if code==0 else 'DEADLINE' if code==124 else 'UNAVAILABLE','cooldownSeconds':delay})
        return launched
    def snapshot(self):
        return {'entries':[{k:v for k,v in s.items() if k!='due'} for s in self.states]}
    def run(self,rounds=None):
        completed=0
        while not self.stop.is_set() and (rounds is None or completed<rounds):
            if self.tick():completed+=1
            if rounds is not None and completed>=rounds:break
            delay=max(.05,min(s['due'] for s in self.states)-self.clock())
            self.stop.wait(min(delay,1))
        return self.snapshot()

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--plan',required=True);mode=parser.add_mutually_exclusive_group(required=True);mode.add_argument('--check',action='store_true');mode.add_argument('--continuous',action='store_true');mode.add_argument('--rounds',type=int);args=parser.parse_args()
    if args.rounds is not None and not 1<=args.rounds<=1000:parser.error('rounds must be 1..1000')
    try:
        verify_sources();plan,digest=load_plan(args.plan);supervisor=Supervisor(plan,os.environ,emit=lambda event:print(json.dumps(event),flush=True))
        if args.check:print('PASS: private worker plan/source identity; no jobs claimed');return 0
        for name in ('SIGINT','SIGTERM'):
            if hasattr(signal,name):signal.signal(getattr(signal,name),lambda *_:supervisor.stop.set())
        print(json.dumps({'event':'SUPERVISOR_STARTED','planSha256':digest,'assignments':len(plan['entries'])}),flush=True)
        result=supervisor.run(args.rounds);print(json.dumps({'event':'SUPERVISOR_STOPPED',**result}),flush=True)
        return 1 if any(s['failedInvocations'] for s in result['entries']) else 0
    except Exception:print('Private worker supervisor unavailable; inspect configuration and durable status',file=sys.stderr);return 1
if __name__=='__main__':raise SystemExit(main())
