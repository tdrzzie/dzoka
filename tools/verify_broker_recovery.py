"""Native broker deadline/parent-loss/grant recovery and immutable bundle faults."""
import base64
import ctypes
from ctypes import wintypes as w
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import time
import uuid
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'tools'));sys.path.insert(0,str(ROOT/'ai'))
from appcontainer_boundary import Profile
from broker_bundle import grant,revoke,protect,verify_bundle
from broker_profiles import verify
from model_broker import state_check
from model_process import exchange
from verify_worker_boundary import private_fixture

def main():
    bundle=Path(os.environ['DZOKA_TEST_BROKER_BUNDLE']);receipt=os.environ['DZOKA_TEST_BROKER_SHA256']
    verify_bundle(bundle,receipt);cases=[]
    with private_fixture() as root:
        protect(root);state=root/'state';state.mkdir();protect(state)
        for name in ('profiles','docker'):(state/name).mkdir();protect(state/name)
        # Never mutate the real model bundle. A tiny independent receipt proves
        # the verifier rejects same-length corruption, missing and extra files.
        sample=root/'receipt-fixture';sample.mkdir();protect(sample);file=sample/'value';file.write_bytes(b'good')
        manifest={'schemaVersion':1,'pythonVersion':'fixture','brokerLockSha256':verify()[1],'files':{'value':{'bytes':4,'sha256':hashlib.sha256(b'good').hexdigest()}}}
        (sample/'bundle.json').write_bytes(json.dumps(manifest).encode());digest=hashlib.sha256((sample/'bundle.json').read_bytes()).hexdigest()
        # Only this miniature fixture substitutes a tiny runtime inventory. The
        # real bundle above is verified against the committed CPython pins.
        with patch('broker_bundle.verify_sources',return_value=({'hostRuntime':{'pythonVersion':'fixture','files':{}}},verify()[1])):
            verify_bundle(sample,digest)
            for fault in ('corrupt','missing','extra','receipt'):
                if fault=='corrupt':file.write_bytes(b'evil')
                elif fault=='missing':file.unlink()
                elif fault=='extra':file.write_bytes(b'good');(sample/'extra').write_bytes(b'extra')
                try:verify_bundle(sample,'0'*64 if fault=='receipt' else digest)
                except (ValueError,OSError):cases.append('bundle_'+fault+'_rejected')
                else:raise AssertionError('Bundle integrity fault accepted')
        request={'version':1,'id':str(uuid.uuid4()),'profile':'ISO_TEXT','inputSha256':hashlib.sha256(b'blue bag').hexdigest(),'data':base64.b64encode(b'blue bag').decode()}
        for fault in ('invalid-request','deadline'):
            with Profile(state/'profiles') as profile:
                grant(bundle,profile.sid_text)
                try:
                    started=[]
                    try:exchange(profile,bundle,{**request,'version':0} if fault=='invalid-request' else request,lambda *_:(_ for _ in ()).throw(ValueError('Capability denied')),timeout=.01 if fault=='deadline' else 30,started=started.append)
                    except (ValueError,OSError) as error:
                        if not started or str(error)!=('Isolated model deadline exceeded' if fault=='deadline' else 'Isolated model protocol unavailable'):raise
                        cases.append('native_'+fault+'_terminated')
                    else:raise AssertionError('Native fault accepted')
                finally:revoke(bundle,profile.sid_text)
        marker=root/'pid'
        source="""import sys,time
from pathlib import Path
sys.path.insert(0,str(Path(sys.argv[1])/'tools'));sys.path.insert(0,str(Path(sys.argv[1])/'ai'))
import model_broker as broker
original=broker.exchange
def ready(pid):
 Path(sys.argv[5]).write_bytes(str(pid).encode())
 time.sleep(60)
def owned(*args,**kwargs):
 kwargs['started']=ready
 return original(*args,**kwargs)
broker.exchange=owned
broker.infer(sys.argv[2],sys.argv[3],sys.argv[4],'ISO_TEXT',b'blue bag',checked=True)
"""
        owner=subprocess.Popen([sys.executable,'-I','-B','-c',source,str(ROOT),str(bundle),receipt,str(state),str(marker)],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
        handle=None;k=ctypes.WinDLL('kernel32',use_last_error=True)
        k.OpenProcess.argtypes=[w.DWORD,w.BOOL,w.DWORD];k.OpenProcess.restype=w.HANDLE
        k.WaitForSingleObject.argtypes=[w.HANDLE,w.DWORD];k.WaitForSingleObject.restype=w.DWORD;k.CloseHandle.argtypes=[w.HANDLE]
        try:
            deadline=time.monotonic()+90
            while not marker.exists() and owner.poll() is None and time.monotonic()<deadline:time.sleep(.02)
            if not marker.exists():raise AssertionError('Owned model did not start')
            handle=k.OpenProcess(0x100000,False,int(marker.read_bytes()))
            if not handle:raise OSError('Owned child wait handle unavailable')
            try:state_check(state,bundle,receipt)
            except ValueError:cases.append('live_grant_owner_blocks_reconciliation')
            else:raise AssertionError('Active owner reaped')
            owner.kill();owner.wait(timeout=10)
            if k.WaitForSingleObject(handle,10000)!=0:raise AssertionError('Model survived broker death')
            cases.append('broker_death_kills_creation_owned_model')
            # Exercise normal plan startup, not an ad-hoc cleanup bypass: the
            # stale package RX grant must not block its own journal recovery.
            from broker_plan import load_plan
            plan={'schemaVersion':1,'coreUrl':'https://core.invalid','allowInsecureLoopback':False,'pollSeconds':1,'backoffMaxSeconds':4,'entries':[{'tenant':str(uuid.uuid4()),'profile':str(uuid.uuid4()),'profileName':'ISO_TEXT','bundle':str(bundle),'bundleSha256':receipt,'stateDirectory':str(state),'tokenEnvironment':'DZOKA_WORKER_TOKEN_ONE'}]}
            plan_path=root/'plan.json';plan_path.write_bytes(json.dumps(plan).encode());protect(plan_path)
            load_plan(plan_path);load_plan(plan_path)
            if list((state/'profiles').glob('grant-*.json')):raise AssertionError('Grant intent survived confirmed recovery')
            if any(json.loads(p.read_bytes())['state']!='REMOVED' for p in (state/'profiles').glob('offline-*.json')):raise AssertionError('Profile recovery incomplete')
            cases.append('orphan_grant_and_profile_recovered_idempotently')
        finally:
            if owner.poll() is None:owner.kill();owner.wait(timeout=10)
            if handle:k.CloseHandle(handle)
    run=ROOT/'.test-tmp'/('broker-recovery-'+uuid.uuid4().hex);run.mkdir()
    (run/'result.json').write_bytes((json.dumps({'status':'PASS','bundleSha256':receipt,'cases':cases},indent=2)+'\n').encode())
    print('PASS: '+str(len(cases))+' native broker recovery/integrity cases; '+str(run/'result.json'),flush=True)

if __name__=='__main__':main()
