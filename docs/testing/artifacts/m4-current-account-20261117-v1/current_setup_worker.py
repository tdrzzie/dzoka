"""Opt-in current-account logon deployment. No secrets in Task Scheduler."""
import argparse
from contextlib import suppress
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import threading
import time
import uuid
sys.path.insert(0,str(Path(__file__).resolve().parent))
sys.path.insert(1,str(Path(__file__).resolve().parents[1]/'ai'))
from worker_service import read_json
from service_account import identity,validate
from service_host import plain,check_acl,check_tree
from broker_bundle import sha,protect
from profile_archive import protected
from docker_lifecycle import OwnerLock
ROOT=Path(__file__).resolve().parents[1]
INSTANCE='00000000000000000000000000000047'
OPERATIONS='00000000000000000000000000000048'

def verify_sources():
    path=ROOT/'ai/interactive-deployment.lock.json';lock=read_json(path)
    if sha(ROOT/'ai/managed-service.lock.json')!=lock['managementLockSha256']:raise ValueError('Management source identity changed')
    for name,digest in lock['sources'].items():
        if sha(ROOT/name)!=digest:raise ValueError('Interactive deployment source changed')
    return lock,sha(path)

def validate_config(config):
    fields={'schemaVersion','owner','accountSid','integrityRid','policy','controlState','managementReceipt','watchdogStart'}
    if not isinstance(config,dict) or set(config)!=fields or type(config['schemaVersion']) is not int or config['schemaVersion']!=1 or not re.fullmatch('[0-9a-f]{32}',str(config['owner'])) or not re.fullmatch('[0-9a-f]{64}',str(config['managementReceipt'])) or type(config['integrityRid']) is not int or config['integrityRid']!=8192:raise ValueError('Invalid interactive deployment')
    for key in ('policy','controlState'):
        if not isinstance(config[key],str) or any(c in config[key] for c in ('"','\r','\n','\0')):raise ValueError('Invalid control path')
    from datetime import datetime
    if not isinstance(config['watchdogStart'],str) or not re.fullmatch(r'\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z',config['watchdogStart']):raise ValueError('Explicit watchdog start required')
    datetime.fromisoformat(config['watchdogStart'])
    return config

def load_config(path,expected):
    if not re.fullmatch('[0-9a-f]{64}',str(expected)):raise ValueError('Explicit configuration receipt required')
    lock,_=verify_sources();path=protected(path)
    if sha(path)!=expected:raise ValueError('Interactive configuration changed')
    config=validate_config(read_json(path));validate(identity(),config['accountSid'],config['integrityRid'])
    for field in ('policy','controlState'):config[field]=protected(config[field])
    if not config['controlState'].is_dir() or config['controlState']==config['policy'].parent:raise ValueError('Separate control state required')
    from managed_release import verify
    verify(config['managementReceipt'])
    host=Path(sys.base_prefix);check_tree(host)
    for filename,entry in lock['hostFiles'].items():
        file=plain(host/filename)
        if file.stat().st_size!=entry['bytes'] or sha(file)!=entry['sha256']:raise ValueError('Interactive host runtime changed')
    from broker_plan import load_plan
    policy=read_json(config['policy']);service=read_json(protected(policy['serviceConfig']));plan=load_plan(protected(service['plan']),preflight=False)
    for entry in plan['entries']:
        bundle=Path(entry['bundle']).resolve()
        if any(p.resolve().is_relative_to(bundle) for p in (path,config['controlState'],ROOT)):raise ValueError('Interactive controls overlap model bundle')
    return config,path

def descriptor(config,path,digest):
    return {'name':'Dzoka-Interactive-'+config['owner'],'sid':config['accountSid'],'source':'urn:dzoka:interactive:'+digest,'executable':str(Path(sys.base_prefix)/'pythonw.exe'),'arguments':subprocess.list2cmdline(['-I','-S','-B',str(ROOT/'tools/current_setup_worker.py'),'run','--config',str(path),'--sha256',digest]),'workingDirectory':str(ROOT),'watchdogStart':config['watchdogStart']}

def task(operation,config,path,digest):
    guard=OwnerLock(config['controlState'],OPERATIONS)
    if not guard.acquire():raise OSError('Another interactive control operation is active')
    try:return task_locked(operation,config,path,digest)
    finally:guard.close()

def task_locked(operation,config,path,digest):
    if operation=='start':
        state=task_locked('inspect',config,path,digest)
        if state['state'] not in (2,4):write_budget(config['controlState'],digest,0)
    runtime_guard=None
    if operation=='remove':
        task_locked('disable',config,path,digest)
        runtime_guard=OwnerLock(config['controlState'],INSTANCE)
        if not runtime_guard.acquire():raise OSError('Stop the active worker before removal')
    try:return task_request(operation,config,path,digest)
    finally:
        if runtime_guard:runtime_guard.close()

def task_request(operation,config,path,digest):
    if len(list(config['controlState'].glob('task-request-*.json')))>=1000:raise ValueError('Interactive operation journal capacity reached')
    request=config['controlState']/('task-request-'+uuid.uuid4().hex+'.json')
    with request.open('xb') as stream:stream.write(json.dumps(descriptor(config,path,digest)).encode());stream.flush();os.fsync(stream.fileno())
    protect(request)
    env={k:v for k,v in os.environ.items() if k in {'PATH','SYSTEMROOT','WINDIR','TEMP','TMP'}}
    try:
        run=subprocess.run([str(Path(os.environ['SYSTEMROOT'])/'System32/WindowsPowerShell/v1.0/powershell.exe'),'-NoProfile','-NonInteractive','-ExecutionPolicy','Bypass','-File',str(ROOT/'tools/current_task.ps1'),'-Operation',operation,'-Request',str(request)],capture_output=True,timeout=60,env=env,creationflags=subprocess.CREATE_NO_WINDOW)
        if run.returncode or len(run.stdout)>4096:raise OSError('Interactive task operation unavailable')
        value=json.loads(run.stdout)
        if not isinstance(value,dict) or value.get('schemaVersion')!=1:raise ValueError('Invalid task reply')
        with request.with_suffix('.result.json').open('xb') as output:output.write(json.dumps(value).encode());output.flush();os.fsync(output.fileno())
        return value
    except BaseException:
        # Keep the intent; an exception is not evidence registration failed.
        raise

def status(state,run,phase):
    from datetime import datetime,timezone
    value={'schemaVersion':1,'run':run,'phase':phase,'at':datetime.now(timezone.utc).isoformat(),'pid':os.getpid(),'deploymentLockSha256':verify_sources()[1]}
    temporary=state/(run+'.interactive.tmp')
    with temporary.open('xb') as stream:stream.write(json.dumps(value).encode());stream.flush();os.fsync(stream.fileno())
    protect(temporary)
    os.replace(temporary,state/'interactive-status.json')

def stop_record(value,run):
    if not isinstance(value,dict) or set(value)!={'schemaVersion','run'} or type(value['schemaVersion']) is not int or value['schemaVersion']!=1 or value['run']!=run:raise ValueError('Wrong stop generation')

def write_budget(state,digest,attempts):
    path=state/('budget-'+uuid.uuid4().hex+'.tmp')
    with path.open('xb') as output:output.write(json.dumps({'schemaVersion':1,'configurationSha256':digest,'attempts':attempts}).encode());output.flush();os.fsync(output.fileno())
    protect(path)
    os.replace(path,state/'restart-budget.json')

def consume_budget(state,digest):
    value=read_json(protected(state/'restart-budget.json'),4096)
    if not isinstance(value,dict) or set(value)!={'schemaVersion','configurationSha256','attempts'} or type(value['schemaVersion']) is not int or value['schemaVersion']!=1 or value['configurationSha256']!=digest or type(value['attempts']) is not int or not 0<=value['attempts']<=4:raise ValueError('Invalid restart budget')
    if value['attempts']==4:return False
    write_budget(state,digest,value['attempts']+1);return True

def run(config,control_path,control_digest):
    state=config['controlState'];guard=OwnerLock(state,INSTANCE)
    if not guard.acquire():raise OSError('Interactive worker already active')
    run_id=uuid.uuid4().hex;stop=threading.Event();failed=threading.Event();finished=threading.Event()
    def monitor():
        path=state/('stop-'+run_id+'.json')
        while not finished.wait(.2):
            try:
                if sha(plain(control_path))!=control_digest:raise ValueError('Interactive controls changed')
                if path.exists():protected(path);stop_record(read_json(path,4096),run_id);stop.set();return
            except Exception:failed.set();stop.set();return
    thread=threading.Thread(target=monitor,daemon=True)
    try:
        if not consume_budget(state,control_digest):
            task('disable',config,control_path,control_digest);status(state,run_id,'CIRCUIT_OPEN');return 1
        status(state,run_id,'STARTING');thread.start()
        from managed_broker import execute
        execute(config['policy'],config['managementReceipt'],stop,lambda:status(state,run_id,'RUNNING'))
        if failed.is_set():raise ValueError('Invalid interactive stop request')
        status(state,run_id,'STOPPED');return 0
    except BaseException:
        with suppress(Exception):status(state,run_id,'UNAVAILABLE')
        raise
    finally:finished.set();thread.join(2);guard.close()

def stop(config,path,digest,timeout=360):
    # Disable future time/logon launches before draining the current run.
    task('disable',config,path,digest)
    state=config['controlState'];guard=OwnerLock(state,INSTANCE)
    if guard.acquire():guard.close();return {'stopped':True,'wasActive':False}
    value=read_json(protected(state/'interactive-status.json'),4096);run_id=value.get('run')
    if not re.fullmatch('[0-9a-f]{32}',str(run_id)):raise ValueError('Invalid run identity')
    path=state/('stop-'+run_id+'.json')
    temporary=state/('stop-request-'+uuid.uuid4().hex+'.tmp')
    with temporary.open('xb') as stream:stream.write(json.dumps({'schemaVersion':1,'run':run_id}).encode());stream.flush();os.fsync(stream.fileno())
    protect(temporary)
    try:
        temporary.rename(path)
    except FileExistsError:
        stop_record(read_json(protected(path),4096),run_id);temporary.unlink()
    deadline=time.monotonic()+timeout
    while time.monotonic()<deadline:
        if guard.acquire():
            guard.close();last=read_json(protected(state/'interactive-status.json'),4096)
            if last.get('run')!=run_id or last.get('phase')!='STOPPED':raise OSError('Graceful stop unconfirmed')
            return {'stopped':True,'wasActive':True}
        time.sleep(.2)
    raise OSError('Bounded graceful stop unavailable')

def main():
    parser=argparse.ArgumentParser();parser.add_argument('operation',choices=['install','inspect','start','stop','remove','run']);parser.add_argument('--config',required=True);parser.add_argument('--sha256',required=True);args=parser.parse_args()
    try:
        config,path=load_config(args.config,args.sha256)
        if args.operation=='run':return run(config,path,args.sha256)
        if args.operation=='stop':value=stop(config,path,args.sha256)
        else:value=task(args.operation,config,path,args.sha256)
        if sys.stdout is not None:print(json.dumps(value))
        return 0
    except Exception:
        if sys.stderr is not None:print('Interactive worker unavailable; inspect protected deployment state',file=sys.stderr)
        return 1

if __name__=='__main__':raise SystemExit(main())
