"""Native interactive-task acceptance and shared real-core test fixture."""
from contextlib import contextmanager
from datetime import datetime,timezone,timedelta
import ctypes
import hashlib
from http.server import BaseHTTPRequestHandler,ThreadingHTTPServer
import json
import os
from pathlib import Path
import subprocess
import sys
import threading
import time
import uuid
sys.path[:0]=[str(Path(__file__).resolve().parent),str(Path(__file__).resolve().parents[1]/'ai')]
import current_setup_worker as worker
from broker_bundle import protect
from service_account import identity
from verify_worker_boundary import private_fixture

def wait(predicate,seconds=360):
    until=time.monotonic()+seconds
    while time.monotonic()<until:
        if predicate():return
        time.sleep(.2)
    raise AssertionError('Interactive acceptance deadline')

def snapshot(config):
    path=config['controlState']/'interactive-status.json'
    return json.loads(path.read_bytes()) if path.exists() else {}

def active(config):
    guard=worker.OwnerLock(config['controlState'],worker.INSTANCE)
    if guard.acquire():guard.close();return False
    return True


def wait_running(config,previous=None,seconds=720,clock=None,pause=None,emit=None):
    """Bound launch/verification readiness; reject stale or terminal generations.

    This diagnostic bound includes scheduler launch and full startup integrity
    verification for every generation. It is not a production startup SLO.
    """
    if type(seconds) is not int or not 1<=seconds<=720:raise ValueError('Bounded readiness wait required')
    clock=clock or time.monotonic;pause=pause or time.sleep;emit=emit or (lambda value:print(value,flush=True))
    started=clock();deadline=started+seconds;observed=None
    while clock()<deadline:
        value=snapshot(config);run_id=value.get('run');new=isinstance(run_id,str) and len(run_id)==32 and all(c in '0123456789abcdef' for c in run_id) and run_id!=previous
        phase=value.get('phase');safe=phase if new and phase in ('STARTING','RUNNING','STOPPED','UNAVAILABLE','CIRCUIT_OPEN') else 'WAITING_FOR_NEW_GENERATION'
        if safe!=observed:emit('Interactive readiness: '+safe);observed=safe
        if new and phase in ('STOPPED','UNAVAILABLE','CIRCUIT_OPEN'):raise AssertionError('New interactive generation became terminal before readiness')
        if new and phase=='RUNNING' and active(config):return round(clock()-started,3)
        pause(.2)
    raise AssertionError('Bounded interactive startup readiness deadline; inspect retained phase history')

@contextmanager
def installed_worker(core,tenant,profile,profile_name,token,readiness=None):
    with private_fixture() as root:
        protect(root)
        for name in ('broker','broker/profiles','broker/docker','service','interactive'):(root/name).mkdir();protect(root/name)
        who=identity();entry={'tenant':tenant,'profile':profile,'profileName':profile_name,'bundle':os.environ['DZOKA_TEST_BROKER_BUNDLE'],'bundleSha256':os.environ['DZOKA_TEST_BROKER_SHA256'],'stateDirectory':str(root/'broker'),'tokenEnvironment':'DZOKA_WORKER_TOKEN_ONE'}
        values={'credentials.json':{'DZOKA_WORKER_TOKEN_ONE':token},'plan.json':{'schemaVersion':1,'coreUrl':core,'allowInsecureLoopback':True,'pollSeconds':1,'backoffMaxSeconds':4,'entries':[entry]},'service.json':{'schemaVersion':3,'accountSid':who['sid'],'integrityRid':who['integrity'],'plan':str(root/'plan.json'),'credentials':str(root/'credentials.json'),'stateDirectory':str(root/'service')},'policy.json':{'schemaVersion':1,'serviceConfig':str(root/'service.json'),'archiveAt':50,'keepTerminal':5},'interactive.json':{'schemaVersion':1,'owner':uuid.uuid4().hex,'accountSid':who['sid'],'integrityRid':who['integrity'],'policy':str(root/'policy.json'),'controlState':str(root/'interactive'),'managementReceipt':os.environ['DZOKA_TEST_MANAGED_SHA256'],'watchdogStart':(datetime.now(timezone.utc)+timedelta(seconds=60)).strftime('%Y-%m-%dT%H:%M:%SZ')}}
        for name,value in values.items():
            path=root/name;path.write_bytes(json.dumps(value).encode());protect(path)
        path=root/'interactive.json';digest=hashlib.sha256(path.read_bytes()).hexdigest();config,path=worker.load_config(path,digest);installed=False
        try:
            worker.task('install',config,path,digest);installed=True
            worker.task('start',config,path,digest)
            elapsed=wait_running(config)
            if readiness is not None:readiness['initialReadyWaitSeconds']=elapsed
            yield config,path,digest,root
        finally:
            if installed:
                worker.stop(config,path,digest)
                wait(lambda:worker.task('inspect',config,path,digest)['state'] not in (2,4),60)
                worker.task('remove',config,path,digest)
            from model_broker import state_check
            from docker_lifecycle import preflight,reconcile
            state_check(root/'broker',entry['bundle'],entry['bundleSha256']);preflight();outcome=reconcile(root/'broker/docker');assert outcome['active']==outcome['unresolved']==0

def kill_owned(config,path,digest):
    """Test-only crash: hold the process handle before validating identity."""
    worker.task('inspect',config,path,digest);before=snapshot(config);assert before['phase']=='RUNNING' and active(config)
    from ctypes import wintypes as w
    k=ctypes.WinDLL('kernel32',use_last_error=True);k.OpenProcess.argtypes=[w.DWORD,w.BOOL,w.DWORD];k.OpenProcess.restype=w.HANDLE;k.CloseHandle.argtypes=[w.HANDLE]
    k.QueryFullProcessImageNameW.argtypes=[w.HANDLE,w.DWORD,w.LPWSTR,ctypes.POINTER(w.DWORD)];k.QueryFullProcessImageNameW.restype=w.BOOL
    k.TerminateProcess.argtypes=[w.HANDLE,w.UINT];k.TerminateProcess.restype=w.BOOL;k.WaitForSingleObject.argtypes=[w.HANDLE,w.DWORD];k.WaitForSingleObject.restype=w.DWORD
    handle=k.OpenProcess(0x1000|0x100000|1,False,before['pid']);assert handle
    try:
        buffer=ctypes.create_unicode_buffer(32768);length=w.DWORD(len(buffer));assert k.QueryFullProcessImageNameW(handle,0,buffer,ctypes.byref(length))
        assert Path(buffer.value).resolve()==(Path(sys.base_prefix)/'pythonw.exe').resolve()
        # Holding the handle prevents PID reuse while checking this exact command.
        command="(Get-CimInstance Win32_Process -Filter 'ProcessId = "+str(before['pid'])+"').CommandLine"
        output=subprocess.check_output(['powershell.exe','-NoProfile','-NonInteractive','-Command',command],timeout=20,creationflags=subprocess.CREATE_NO_WINDOW).decode().strip()
        descriptor=worker.descriptor(config,path,digest)
        assert str(path) in output and str(worker.ROOT/'tools/current_setup_worker.py') in output and digest in output
        assert snapshot(config)['run']==before['run'] and active(config)
        assert k.TerminateProcess(handle,1) and k.WaitForSingleObject(handle,10000)==0
    finally:k.CloseHandle(handle)
    return before['run']

def main():
    cases=[];readiness={};tenant=str(uuid.uuid4());profile=str(uuid.uuid4());calls=[];token='synthetic-task-scope-token'
    class Handler(BaseHTTPRequestHandler):
        def log_message(self,*_):pass
        def do_POST(self):
            assert self.path=='/api/v1/tenants/'+tenant+'/internal/perception/jobs/claim'
            assert self.headers.get('Authorization')=='Bearer '+token
            length=int(self.headers['Content-Length']);assert length<4096;body=json.loads(self.rfile.read(length));assert body=={'profileId':profile}
            calls.append(time.monotonic());self.send_response(204);self.end_headers()
    server=ThreadingHTTPServer(('127.0.0.1',0),Handler);thread=threading.Thread(target=server.serve_forever,daemon=True);thread.start()
    try:
        with installed_worker('http://127.0.0.1:'+str(server.server_port),tenant,profile,'ISO_SHONA_CPU_CPU_SN_EN',token,readiness) as (config,path,digest,root):
            wait(lambda:bool(calls));cases+=['installed_least_privilege_interactive_task','current_account_docker_preflight','actual_authenticated_idle_claim']
            run_id=snapshot(config)['run'];worker.task('start',config,path,digest);time.sleep(2);assert snapshot(config)['run']==run_id;cases.append('scheduler_duplicate_start_excluded')
            guard=worker.OwnerLock(config['controlState'],worker.OPERATIONS);assert guard.acquire()
            try:
                try:worker.task('inspect',config,path,digest)
                except OSError:cases.append('actual_control_lock_excludes_competing_operation')
                else:raise AssertionError('Concurrent control operation accepted')
            finally:guard.close()
            original=worker.descriptor
            for field,value in [('arguments',' --unapproved'),('source','0')]:
                def wrong(*args):
                    result=original(*args);result[field]+=value;return result
                worker.descriptor=wrong
                try:
                    try:worker.task('inspect',config,path,digest)
                    except OSError:cases.append('wrong_'+field+'_descriptor_rejected')
                    else:raise AssertionError('Wrong task descriptor accepted')
                finally:worker.descriptor=original
            previous=kill_owned(config,path,digest);cases.append('exact_registered_worker_crash_injected')
            readiness['watchdogRecoveryWaitSeconds']=wait_running(config,previous)
            cases.append('bounded_periodic_worker_restart_verified')
            baseline=len(calls);wait(lambda:len(calls)>baseline);cases.append('restart_recovers_then_resumes_claims')
            worker.stop(config,path,digest);assert snapshot(config)['phase']=='STOPPED';cases.append('graceful_generation_bound_stop')
            stopped_run=snapshot(config)['run']
            wait(lambda:worker.task('inspect',config,path,digest)['state'] not in (2,4),60)
            worker.task('start',config,path,digest);readiness['explicitRestartReadyWaitSeconds']=wait_running(config,stopped_run);cases.append('explicit_restart_after_graceful_stop')
            # Inject the budget boundary, not four fabricated successful runs.
            worker.write_budget(config['controlState'],digest,4);kill_owned(config,path,digest)
            wait(lambda:snapshot(config).get('phase')=='CIRCUIT_OPEN' and not active(config),180)
            assert worker.task('inspect',config,path,digest)['enabled'] is False
            baseline=len(calls);time.sleep(2);assert len(calls)==baseline;cases.append('exhausted_budget_disables_task_before_claim')
        cases.append('exact_task_and_owned_resource_cleanup')
    finally:server.shutdown();server.server_close();thread.join(5)
    result=Path.cwd()/'.test-tmp'/('current-setup-'+uuid.uuid4().hex+'.json');result.write_text(json.dumps({'cases':cases,'readinessWaits':readiness,'startupWaitBoundSeconds':720,'scope':'Actual installed interactive task, empty-queue authenticated protocol; diagnostic readiness waits, not production startup SLO; real model/core jobs are separate integration tests; not boot-before-login'},indent=2)+'\n');print('PASS: current-account interactive deployment; '+str(result),flush=True)

if __name__=='__main__':
    release=Path(os.environ['DZOKA_TEST_MANAGED_RELEASE']).resolve()
    if Path(__file__).resolve().parents[1]!=release:
        subprocess.run([sys.executable,'-I','-B',str(release/'tools/verify_current_setup.py')],check=True,timeout=2400)
    else:main()
