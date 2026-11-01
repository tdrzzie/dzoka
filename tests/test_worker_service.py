import copy
import json
import os
import sys
import tempfile
import threading
import unittest
from pathlib import Path
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'tools'))
import worker_service as service
from service_host import validate_acl

class WorkerServiceTests(unittest.TestCase):
    def test_private_acl_rejects_broad_access_unprotected_owner_and_complex_rules(self):
        valid={'sid':'S-1-5-21-123','owner':'S-1-5-21-123','protected':True,'rules':[{'sid':'S-1-5-21-123','allow':True,'rights':2032127}]}
        validate_acl(valid,True)
        owner_rights=copy.deepcopy(valid);owner_rights['rules'].append({'sid':'S-1-3-4','allow':True,'rights':2032127});validate_acl(owner_rights,True)
        from service_host import INSTALLER
        installer=copy.deepcopy(valid);installer['owner']=INSTALLER;validate_acl(installer,False,True)
        with self.assertRaises(ValueError):validate_acl(installer,True)
        for field,value in [('protected',False),('owner','S-1-1-0'),('rules',[])]:
            bad=copy.deepcopy(valid);bad[field]=value
            with self.assertRaises(ValueError):validate_acl(bad,True)
        for rule in [{'sid':'S-1-1-0','allow':True,'rights':1},{'sid':valid['sid'],'allow':False,'rights':1}]:
            bad=copy.deepcopy(valid);bad['rules'].append(rule)
            with self.assertRaises(ValueError):validate_acl(bad,True)
        public=copy.deepcopy(valid);public['rules'].append({'sid':'S-1-1-0','allow':True,'rights':0x120089});validate_acl(public,False)
        public['rules'][-1]['rights']|=2
        with self.assertRaises(ValueError):validate_acl(public,False)
        public['rules'][-1]['inheritOnly']=True;validate_acl(public,False)
        public['rules'][-1]['inheritOnly']=False;public['rules'][-1]['rights']=4;validate_acl(public,False,ancestor=True)
        public['rules'][-1]['rights']=0x40
        with self.assertRaises(ValueError):validate_acl(public,False,ancestor=True)
        public['rules'][-1]['rights']=0x10000000
        with self.assertRaises(ValueError):validate_acl(public,False)

    def test_unknown_json_duplicates_and_oversize_fail(self):
        with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as directory:
            path=Path(directory)/'config'
            for raw in (b'{"x":1,"x":2}',b'{"x":NaN}',b' '*65537):
                path.write_bytes(raw)
                with self.assertRaises(ValueError):service.read_json(path)

    def test_recovery_precedes_polling_and_stop_does_not_claim_again(self):
        with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as directory:
            state=Path(directory);events=[];stop=threading.Event()
            class Worker:
                def __init__(self,*_,**__):events.append('init')
                def tick(self):events.append('tick');stop.set()
            loop=service.ServiceLoop({'stateDirectory':state},{'entries':[{'family':'shona-cpu','dockerStateDirectory':directory}]},{'secret':'not-status'},stop,Worker,lambda p:(events.append('recover') or {'unresolved':0,'active':0}),lambda:events.append('prepare'))
            self.assertEqual(0,loop.run(lambda:events.append('started')))
            self.assertEqual(['prepare','recover','init','started','tick'],events)
            status=json.loads((state/'status.json').read_bytes());self.assertEqual('STOPPED',status['phase']);self.assertNotIn('secret',json.dumps(status));self.assertNotIn('not-status',json.dumps(status))

    def test_unresolved_or_active_recovery_blocks_polling_and_preserves_diagnostic(self):
        with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as directory:
            state=Path(directory)
            for result in ({'unresolved':1,'active':0},{'unresolved':0,'active':1}):
                with patch.object(service,'Supervisor') as factory:
                    loop=service.ServiceLoop({'stateDirectory':state},{'entries':[{'family':'shona-cpu','dockerStateDirectory':directory}]},{},supervisor=factory,recover=lambda _:result,prepare=lambda:None)
                    with self.assertRaises(OSError):loop.run()
                    factory.assert_not_called();self.assertEqual('UNAVAILABLE',json.loads((state/'status.json').read_bytes())['phase'])

    def test_exclusive_instance_and_crash_restart_uses_lock_not_status(self):
        with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as directory:
            state=Path(directory);lock=service.OwnerLock(state,service.INSTANCE);self.assertTrue(lock.acquire())
            loop=service.ServiceLoop({'stateDirectory':state},{'entries':[]},{},stop=threading.Event())
            try:
                with self.assertRaises(OSError):loop.run()
                self.assertFalse((state/'status.json').exists())
            finally:lock.close()
            # A stale RUNNING status is not an ownership lease.
            (state/'status.json').write_bytes(b'{"phase":"RUNNING"}')
            stop=threading.Event();stop.set()
            class Worker:
                def __init__(self,*_,**__):pass
            self.assertEqual(0,service.ServiceLoop({'stateDirectory':state},{'entries':[]},{},stop,Worker).run())

    def test_service_release_rejects_changed_source_and_supervisor_identity(self):
        service.verify_release()
        with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as directory:
            root=Path(directory);(root/'ai').mkdir();(root/'ai/service.lock.json').write_bytes(json.dumps({'supervisorLockSha256':'wrong','sources':{}}).encode())
            with patch.object(service,'ROOT',root),self.assertRaises(ValueError):service.verify_release()
            (root/'changed').write_bytes(b'modified')
            (root/'ai/service.lock.json').write_bytes(json.dumps({'supervisorLockSha256':service.verify_sources(),'sources':{'changed':'0'*64}}).encode())
            with patch.object(service,'ROOT',root),self.assertRaises(ValueError):service.verify_release()

    def test_credentials_are_exact_private_references_and_environment_is_scrubbed(self):
        with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as directory:
            root=Path(directory);state=root/'state';state.mkdir()
            config={'schemaVersion':1,'plan':str(root/'plan.json'),'credentials':str(root/'credentials.json'),'stateDirectory':str(state)}
            plan={'schemaVersion':1,'coreUrl':'https://core.example.test','allowInsecureLoopback':False,'pollSeconds':1,'backoffMaxSeconds':2,'entries':[{'family':'text','tenant':'11111111-1111-1111-1111-111111111111','profile':'22222222-2222-2222-2222-222222222222','modelDirectory':directory,'tokenEnvironment':'DZOKA_WORKER_TOKEN_ONE'}]}
            (root/'service.json').write_bytes(json.dumps(config).encode());(root/'plan.json').write_bytes(json.dumps(plan).encode())
            # This contract test needs neither Windows ACLs nor cached runtimes.
            # Actual path/ACL/runtime checks are exercised by the native phase.
            with patch.object(service,'plain',side_effect=Path),patch.object(Path,'is_dir',return_value=True),patch.object(service,'check_acl'),patch.object(service,'check_tree'),patch.dict(os.environ,{'DATABASE_URL':'private-db','DZOKA_WORKER_TOKEN_OTHER':'sibling'}):
                for credentials in ({},{'DZOKA_WORKER_TOKEN_ONE':'valid','extra':'secret'},{'DZOKA_WORKER_TOKEN_ONE':'invalid token'}):
                    (root/'credentials.json').write_bytes(json.dumps(credentials).encode())
                    with self.assertRaises(ValueError):service.deployment(root/'service.json')
                (root/'credentials.json').write_bytes(b'{"DZOKA_WORKER_TOKEN_ONE":"synthetic-valid"}')
                _,_,env=service.deployment(root/'service.json')
                self.assertEqual('synthetic-valid',env['DZOKA_WORKER_TOKEN_ONE']);self.assertNotIn('DATABASE_URL',env);self.assertNotIn('DZOKA_WORKER_TOKEN_OTHER',env)

    def test_scm_dispatch_rejects_non_scm_launch_without_executing_worker(self):
        if os.name!='nt':self.skipTest('Windows SCM contract')
        from windows_service import dispatch
        calls=[]
        with self.assertRaises(OSError):dispatch(lambda *_:calls.append('work'))
        self.assertEqual([],calls)

    def test_scm_requires_checked_account_schema_before_applying_limits_or_polling(self):
        with patch.object(service,'deployment',return_value=({'schemaVersion':1},{},{})),patch.object(service,'HostLimits') as limits:
            with self.assertRaises(ValueError):service.execute('unused',threading.Event(),lambda:None,require_account=True)
            limits.assert_not_called()

if __name__=='__main__':unittest.main()
