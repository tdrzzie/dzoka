"""Task-owned Windows ACL/kernel/service recovery probes; no SCM installation."""
import json
import os
from pathlib import Path
import subprocess
import shutil
import sys
import tempfile
import time
import uuid
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'tools'))
from service_host import check_acl,check_tree
from worker_service import verify_release,INSTANCE
from docker_lifecycle import OwnerLock,Session,preflight,reconcile,lookup,cli

def wait(predicate,seconds=20):
    end=time.monotonic()+seconds
    while time.monotonic()<end:
        if predicate():return
        time.sleep(.1)
    raise AssertionError('Service fixture deadline')

def main():
    verify_release()
    if os.name!='nt':raise OSError('Windows service verification required')
    run=ROOT/'.test-tmp'/('worker-service-'+uuid.uuid4().hex);run.mkdir();private=run/'private';private.mkdir();cases=[]
    sid=subprocess.check_output(['whoami','/user','/fo','csv','/nh']).decode().strip().split(',')[-1].strip('"')
    subprocess.run(['icacls',str(private),'/inheritance:r','/grant:r','*'+sid+':(OI)(CI)F','*S-1-5-18:(OI)(CI)F','*S-1-5-32-544:(OI)(CI)F'],check=True,stdout=subprocess.DEVNULL)
    check_acl(private);cases.append('private_ntfs_acl_accepted')
    subprocess.run(['icacls',str(private),'/grant','*S-1-5-11:(RX)'],check=True,stdout=subprocess.DEVNULL)
    try:check_acl(private)
    except ValueError:cases.append('broad_private_acl_rejected')
    else:raise AssertionError('Broad ACL accepted')
    subprocess.run(['icacls',str(private),'/remove:g','*S-1-5-11'],check=True,stdout=subprocess.DEVNULL);check_acl(private)
    # Host resource limits are applied only to a disposable subprocess.
    source="import sys,json;sys.path.insert(0,sys.argv[1]+'/tools');from service_host import HostLimits;limits=HostLimits();print(json.dumps(limits.evidence))"
    result=subprocess.run([sys.executable,'-I','-c',source,str(ROOT)],check=True,stdout=subprocess.PIPE,stderr=subprocess.PIPE,timeout=20)
    limits=json.loads(result.stdout);assert limits=={'memoryBytes':8*1024**3,'activeProcesses':128,'cpuRate':5000,'killOnClose':True};cases.append('real_kernel_resource_limits_readback')
    # A real process holds the fixed service lease, then dies without releasing it.
    ready=run/'ready';source="import sys,time;from pathlib import Path;sys.path.insert(0,sys.argv[1]+'/tools');from docker_lifecycle import OwnerLock;from worker_service import INSTANCE;lock=OwnerLock(Path(sys.argv[2]),INSTANCE);assert lock.acquire();Path(sys.argv[3]).write_bytes(b'ready');time.sleep(120)"
    child=subprocess.Popen([sys.executable,'-I','-c',source,str(ROOT),str(private),str(ready)],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
    try:
        wait(lambda:ready.exists() or child.poll() is not None);assert ready.exists();lock=OwnerLock(private,INSTANCE);assert not lock.acquire();cases.append('live_process_instance_excluded')
        child.kill();child.wait(timeout=10);assert lock.acquire();lock.close();cases.append('crash_releases_instance_lease')
    finally:
        if child.poll() is None:child.kill();child.wait(timeout=10)
    # Lose the entire guardian owner, leaving the daemon resource for startup recovery.
    state=private/'docker';state.mkdir();ready=run/'docker-ready';models=ROOT/'.audit-cache/shona-cpu-models'
    source="""import sys,os,json,time
from pathlib import Path
sys.path.insert(0,sys.argv[1]+'/tools');sys.path.insert(0,sys.argv[1]+'/ai')
from service_host import HostLimits
from docker_owner import Guardian
from ai_supervisor import verify_sources
from docker_rpc import call
from docker_lifecycle import cli,preflight
limits=HostLimits()
with Guardian(Path(sys.argv[2]),Path(sys.argv[3]),verify_sources()) as guardian:
 os.environ.update(guardian.environment());call('prepare',group='CT2_SN_EN')
 record=json.loads(next(Path(sys.argv[3]).glob('*.json')).read_bytes());preflight();cli(['start',record['name']])
 assert json.loads(cli(['inspect',record['name']]))[0]['State']['Running']
 Path(sys.argv[4]).write_bytes(b'ready');time.sleep(120)
"""
    child=subprocess.Popen([sys.executable,'-I','-c',source,str(ROOT),str(models),str(state),str(ready)],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
    try:
        wait(lambda:ready.exists() or child.poll() is not None,90);assert ready.exists(),'Whole-service fixture failed'
        preflight();record=json.loads(next(state.glob('*.json')).read_bytes());assert lookup(record)['State']['Running']
        child.kill();child.wait(timeout=10)
        wait(lambda:reconcile(state)['active']==0,20)
        result=reconcile(state);assert result['unresolved']==0 and lookup(record) is None
        assert json.loads(next(state.glob('*.json')).read_bytes())['history'][-1]['state']=='REMOVED';cases.append('whole_service_and_guardian_loss_running_container_recovered')
        assert reconcile(state)=={'removed':0,'active':0,'unresolved':0};cases.append('restart_reconciliation_idempotent')
    finally:
        if child.poll() is None:child.kill();child.wait(timeout=10)
        preflight();reconcile(state)
    # Stage only task-owned source/configuration in the user's private temp tree.
    # The shared development drive intentionally fails the deployment ACL check.
    with tempfile.TemporaryDirectory(prefix='dzoka-service-',dir=Path.home()) as directory:
        stage=Path(directory)
        def protect(path):
            rights=':(OI)(CI)F' if path.is_dir() else ':F'
            subprocess.run(['icacls',str(path),'/inheritance:r','/grant:r','*'+sid+rights,'*S-1-5-18'+rights,'*S-1-5-32-544'+rights],check=True,stdout=subprocess.DEVNULL)
        protect(stage)
        for folder in ('tools','ai'):
            (stage/folder).mkdir()
            for path in (ROOT/folder).iterdir():
                if path.is_file() and path.suffix in ('.py','.json','.ps1'):shutil.copyfile(path,stage/folder/path.name)
        runtime=stage/'.audit-cache/ai-venv/Scripts';runtime.mkdir(parents=True)
        # Deliberately unavailable executable: lifecycle is tested without a model
        # or inference runtime fallback. Native jobs run in separate actual ITs.
        (runtime/'python.exe').write_bytes(b'not-an-executable')
        check_tree(stage/'.audit-cache/ai-venv')
        subprocess.run(['icacls',str(runtime/'python.exe'),'/grant','*S-1-5-11:(M)'],check=True,stdout=subprocess.DEVNULL)
        try:check_tree(stage/'.audit-cache/ai-venv')
        except ValueError:cases.append('explicit_writable_runtime_leaf_rejected')
        else:raise AssertionError('Writable runtime leaf accepted')
        subprocess.run(['icacls',str(runtime/'python.exe'),'/remove:g','*S-1-5-11'],check=True,stdout=subprocess.DEVNULL)
        state=stage/'state';state.mkdir();protect(state)
        config={'schemaVersion':1,'plan':str(stage/'plan.json'),'credentials':str(stage/'credentials.json'),'stateDirectory':str(state)}
        plan={'schemaVersion':1,'coreUrl':'https://127.0.0.1:1','allowInsecureLoopback':False,'pollSeconds':1,'backoffMaxSeconds':2,'entries':[{'family':'text','tenant':'11111111-1111-1111-1111-111111111111','profile':'22222222-2222-2222-2222-222222222222','modelDirectory':str(ROOT/'.audit-cache/ai-model'),'tokenEnvironment':'DZOKA_WORKER_TOKEN_FIXTURE'}]}
        # Only plan parsing is needed: use an existing directory; no inference claim.
        plan['entries'][0]['modelDirectory']=str(ROOT/'.audit-cache')
        for name,value in [('config.json',config),('plan.json',plan),('credentials.json',{'DZOKA_WORKER_TOKEN_FIXTURE':'synthetic-not-a-core-token'})]:
            path=stage/name;path.write_bytes(json.dumps(value).encode());protect(path)
        # No cached inference environment is copied. A worker is unavailable and
        # polling/stop still behave correctly; actual native jobs have separate ITs.
        source="""import sys,threading
sys.path.insert(0,sys.argv[1]+'/tools')
from worker_service import execute
stop=threading.Event();timer=threading.Timer(3,stop.set)
def started():timer.start()
raise SystemExit(execute(sys.argv[2],stop,started))
"""
        completed=subprocess.run([sys.executable,'-I','-c',source,str(stage),str(stage/'config.json')],stdout=subprocess.PIPE,stderr=subprocess.PIPE,timeout=240)
        if completed.returncode:
            # Only fixed errors/path information; fixture contains synthetic values.
            (run/'foreground-failure.log').write_bytes(completed.stderr)
        assert completed.returncode==0,'Protected foreground deployment failed; inspect ignored fixture log'
        status=json.loads((state/'status.json').read_bytes());assert status['phase']=='STOPPED' and 'synthetic-not-a-core-token' not in json.dumps(status)
        cases.append('protected_foreground_start_poll_unavailable_worker_and_graceful_stop')
        # Deliberately add a writable release ACL and require deployment rejection.
        subprocess.run(['icacls',str(stage/'tools'),'/grant','*S-1-5-11:(M)'],check=True,stdout=subprocess.DEVNULL)
        denied=subprocess.run([sys.executable,'-I',str(stage/'tools/worker_service.py'),'--config',str(stage/'config.json'),'--check'],stdout=subprocess.PIPE,stderr=subprocess.PIPE,timeout=240)
        assert denied.returncode!=0 and b'synthetic-not-a-core-token' not in denied.stderr
        cases.append('writable_release_rejected_before_polling')
    evidence={'status':'PASS','cases':cases,'limits':limits,'scope':'Actual Windows ACLs/kernel locks/limits and local Docker whole-owner recovery; SCM installation/boot/account isolation not executed'}
    (run/'result.json').write_bytes((json.dumps(evidence,indent=2)+'\n').encode());print('PASS: '+str(len(cases))+' native worker service checks; '+str(run/'result.json'))

if __name__=='__main__':main()
