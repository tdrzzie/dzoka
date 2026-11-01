"""Native no-capability pilot, identity policy and real terminal-journal archive."""
import hashlib
import json
import os
from pathlib import Path
import shutil
import socket
import subprocess
import sys
import tempfile
import threading
import time
import uuid
import zipfile
from unittest.mock import patch
from contextlib import contextmanager
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from appcontainer_boundary import Profile,launch,recover
from service_account import identity,validate
from journal_archive import archive,verify
from docker_lifecycle import preflight,Session,OwnerLock,reconcile

@contextmanager
def private_fixture():
    root=Path(tempfile.mkdtemp(prefix='dzoka-offline-',dir=Path.home())).resolve()
    assert root.parent==Path.home().resolve() and root.name.startswith('dzoka-offline-')
    try:yield root
    except BaseException:
        # Profile creation/deletion can have an unresolved outcome. Never erase
        # its ownership metadata merely because a verification process failed.
        print('Retained private offline fixture: '+str(root),flush=True);raise
    else:
        assert root.resolve()==root and root.parent==Path.home().resolve() and root.name.startswith('dzoka-offline-') and not root.is_symlink() and not root.stat().st_file_attributes&0x400
        shutil.rmtree(root)

def stage_python(target):
    base=Path(sys.base_prefix);hashes={}
    for name in ('python.exe','python314.dll','python3.dll','vcruntime140.dll','vcruntime140_1.dll','LICENSE.txt'):
        source=base/name;shutil.copyfile(source,target/name);hashes[name]=hashlib.sha256(source.read_bytes()).hexdigest()
        assert hashlib.sha256((target/name).read_bytes()).hexdigest()==hashes[name]
    shutil.copytree(base/'DLLs',target/'DLLs',ignore=shutil.ignore_patterns('__pycache__'))
    for source in (base/'DLLs').rglob('*'):
        if source.is_file() and '__pycache__' not in source.parts:
            name='DLLs/'+source.relative_to(base/'DLLs').as_posix();hashes[name]=hashlib.sha256(source.read_bytes()).hexdigest()
            assert hashlib.sha256((target/name).read_bytes()).hexdigest()==hashes[name]
    with zipfile.ZipFile(target/'python314.zip','w',compression=zipfile.ZIP_STORED) as bundle:
        for source in (base/'Lib').rglob('*.py'):
            relative=source.relative_to(base/'Lib')
            if any(p in {'site-packages','test','tests','__pycache__','tkinter','idlelib','ensurepip','turtledemo'} for p in relative.parts):continue
            bundle.write(source,relative.as_posix())
    (target/'python314._pth').write_bytes(b'python314.zip\nDLLs\n.\n')
    shutil.copyfile(ROOT/'tools/offline_probe.py',target/'offline_probe.py')
    hashes['stdlibZipSha256']=hashlib.sha256((target/'python314.zip').read_bytes()).hexdigest()
    return hashes

def main():
    from worker_service import verify_release
    verify_release();who=identity();validate(who,who['sid'],who['integrity']);cases=['actual_non_elevated_expected_identity_accepted']
    try:validate(who,'S-1-5-21-1-2-3-1002')
    except ValueError:cases.append('different_service_identity_rejected')
    else:raise AssertionError('Identity mismatch accepted')
    run=ROOT/'.test-tmp'/('worker-boundary-'+uuid.uuid4().hex);run.mkdir()
    listeners=[]
    with private_fixture() as root:
        runtime=root/'runtime';runtime.mkdir();private=root/'private';private.mkdir();secret=private/'credential.fixture';secret.write_bytes(b'synthetic-private-fixture');profiles=root/'profiles';profiles.mkdir()
        sid=who['sid']
        subprocess.run(['icacls',str(root),'/inheritance:r','/grant:r','*'+sid+':(OI)(CI)F','*S-1-5-18:(OI)(CI)F','*S-1-5-32-544:(OI)(CI)F'],check=True,stdout=subprocess.DEVNULL)
        subprocess.run(['icacls',str(profiles),'/inheritance:r','/grant:r','*'+sid+':(OI)(CI)F','*S-1-5-18:(OI)(CI)F','*S-1-5-32-544:(OI)(CI)F'],check=True,stdout=subprocess.DEVNULL)
        hashes=stage_python(runtime)
        # Both listeners are demonstrably reachable outside the sandbox. Host
        # interface is optional only when none exists; at least IPv4 loopback required.
        endpoints=[]
        hosts=['127.0.0.1']
        candidates={item[4][0] for item in socket.getaddrinfo(socket.gethostname(),None,socket.AF_INET)}
        hosts+=sorted(h for h in candidates if not h.startswith('127.'))[:1]
        for host in hosts:
            server=socket.socket();server.bind((host,0));server.listen(4);listeners.append(server);endpoint=(host,server.getsockname()[1]);endpoints.append(endpoint)
            with socket.create_connection(endpoint,timeout=2):
                connection,_=server.accept();connection.close()
        with Profile(profiles) as profile:
            subprocess.run(['icacls',str(runtime),'/grant','*'+profile.sid_text+':(OI)(CI)RX','/T'],check=True,stdout=subprocess.DEVNULL)
            assert recover(profiles)['active']==1;cases.append('active_package_profile_excluded_from_recovery')
            payload='Ndasiya bhegi rangu around 2pm. caf\u00e9 e\u0301 \U0001f9f3'
            request={'endpoints':endpoints,'privateFile':str(secret),'payload':payload}
            with patch.dict(os.environ,{'DZOKA_AI_TOKEN':'synthetic-parent-token','DATABASE_URL':'synthetic-parent-db','HTTPS_PROXY':'synthetic-parent-proxy'}):
                result=launch(profile,runtime,request)
            assert not result['credentialEnvironmentPresent'];cases.append('parent_credentials_and_proxy_environment_excluded')
            # A timeout alone is never accepted. Require an independent listener
            # negative observation bracketed by successful parent connections.
            for server,endpoint in zip(listeners,endpoints):
                server.settimeout(.3)
                try:connection,_=server.accept()
                except TimeoutError:pass
                else:connection.close();raise AssertionError('Isolated connection reached listener')
                with socket.create_connection(endpoint,timeout=2):
                    connection,_=server.accept();connection.close()
            assert len(result['directOrCapabilityDenial'])==len(endpoints),result
            for (host,_),denied,error in zip(endpoints,result['directOrCapabilityDenial'],result['networkErrors']):
                # The Windows capability diagnostic excludes loopback. For that
                # endpoint require independently checked no-exemption policy,
                # verified package token, timeout AND paired receiver controls.
                assert denied or host=='127.0.0.1' and error and error['timeout'] and error['diagnostic']==0 and error['missingCapability']==0,result
            assert result['privateFileDenied'] and result['payloadSha256']==hashlib.sha256(payload.encode()).hexdigest(),result
            cases+=['actual_package_token_and_zero_capabilities_verified','no_loopback_exemption_and_paired_receiver_controls_pass','private_file_denied_and_offline_utf8_hash_computed']
            if len(endpoints)>1:cases.append('reachable_host_interface_egress_denied')
            try:launch(profile,runtime,{**request,'wait':True},timeout=.4)
            except OSError as error:
                assert str(error)=='Offline deadline exceeded',str(error);cases.append('suspended_owned_process_timeout_terminated')
            else:raise AssertionError('Deadline did not stop child')
        assert all(json.loads(p.read_bytes())['state']=='REMOVED' for p in profiles.glob('*.json'));cases.append('owned_package_profile_removed_after_exit')
        # Abandoned profile (no child) simulates a lost owner before next startup.
        orphan=Profile(profiles);orphan.__enter__();orphan.a.FreeSid(orphan.sid);orphan.sid=None;orphan.lock.close()
        assert recover(profiles)['removed']==1 and recover(profiles)=={'removed':0,'active':0,'unresolved':0};cases.append('inactive_package_profile_reconciled_idempotently')
        ready=root/'child-ready.json'
        source="""import sys,json,subprocess
from pathlib import Path
sys.path.insert(0,sys.argv[1]+'/tools')
from appcontainer_boundary import Profile,launch
with Profile(Path(sys.argv[2])) as profile:
 subprocess.run(['icacls',sys.argv[3],'/grant','*'+profile.sid_text+':(OI)(CI)RX','/T'],check=True,stdout=subprocess.DEVNULL)
 launch(profile,Path(sys.argv[3]),{'wait':True},timeout=120,started=lambda pid:Path(sys.argv[4]).write_bytes(json.dumps({'pid':pid}).encode()))
"""
        helper=subprocess.Popen([sys.executable,'-I','-c',source,str(ROOT),str(profiles),str(runtime),str(ready)],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL,creationflags=subprocess.CREATE_NO_WINDOW)
        try:
            end=time.monotonic()+30
            while not ready.exists() and helper.poll() is None and time.monotonic()<end:time.sleep(.1)
            assert ready.exists(),'Parent-death fixture did not start owned child'
            pid=json.loads(ready.read_bytes())['pid']
            import ctypes
            from ctypes import wintypes as w
            k=ctypes.WinDLL('kernel32',use_last_error=True);k.OpenProcess.argtypes=[w.DWORD,w.BOOL,w.DWORD];k.OpenProcess.restype=w.HANDLE;k.CloseHandle.argtypes=[w.HANDLE];k.WaitForSingleObject.argtypes=[w.HANDLE,w.DWORD];k.WaitForSingleObject.restype=w.DWORD
            handle=k.OpenProcess(0x100000,False,pid);assert handle,'Cannot observe task child'
            try:
                helper.kill();helper.wait(timeout=10);assert k.WaitForSingleObject(handle,10000)==0,'Child survived owner death'
            finally:k.CloseHandle(handle)
            assert recover(profiles)['removed']==1;cases.append('atomic_job_membership_parent_death_and_profile_recovery_pass')
        finally:
            if helper.poll() is None:helper.kill();helper.wait(timeout=10)
            recover(profiles)
        shutil.copytree(profiles,run/'profiles')
    for listener in listeners:listener.close()
    # Actual guarded container -> confirmed terminal history -> exact archived bytes.
    state=run/'docker';state.mkdir();preflight();owner=uuid.uuid4().hex;lease=OwnerLock(state,owner);assert lease.acquire();session=Session(ROOT/'.audit-cache/shona-cpu-models',state,owner,threading.Event())
    try:session.prepare('CT2_SN_EN');assert session.close()
    finally:session.stack.close();lease.close()
    journal=next(state.glob('dzoka-owned-*.json'));raw=journal.read_bytes();assert archive(state)['archived']==1 and verify(state/'archive')==1
    assert (state/'archive'/journal.name).read_bytes()==raw and not journal.exists() and reconcile(state)=={'removed':0,'active':0,'unresolved':0}
    cases.append('real_confirmed_docker_journal_archived_without_history_change')
    evidence={'status':'PASS','cases':cases,'endpointsTested':len(endpoints),'networkObservations':result,'pythonVersion':sys.version.split()[0],'runtimeCopy':hashes,'scope':'Windows AppContainer stdlib pilot; existing networked model workers are not isolated by this pilot; no machine firewall/account changes'}
    (run/'result.json').write_bytes((json.dumps(evidence,indent=2)+'\n').encode());print('PASS: '+str(len(cases))+' account/offline/recovery checks; '+str(run/'result.json'))

if __name__=='__main__':main()
