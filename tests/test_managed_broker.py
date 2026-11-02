import copy
import json
import os
from pathlib import Path
import sys
import tempfile
import threading
import unittest
import uuid
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
import managed_broker as managed

class ManagedBrokerTests(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp');self.root=Path(self.tmp.name)
        self.credentials=self.root/'credentials';self.credentials.write_bytes(b'{"DZOKA_WORKER_TOKEN_ONE":"old-token"}')
        self.service=managed.ManagedService(self.root/'policy','a'*64)
        self.service.release='b'*64;self.service.account='S-1-5-21-1-2-3-1001'
        self.service.config={'credentials':self.credentials,'stateDirectory':self.root}
        entry={'tenant':str(uuid.uuid4()),'profile':str(uuid.uuid4()),'profileName':'ISO_TEXT','bundle':str(self.root/'bundle'),'bundleSha256':'c'*64,'stateDirectory':str(self.root/'state'),'tokenEnvironment':'DZOKA_WORKER_TOKEN_ONE'}
        self.service.plan={'coreUrl':'https://core.example','allowInsecureLoopback':False,'pollSeconds':1,'backoffMaxSeconds':8,'entries':[entry]}
        self.patch=patch.object(managed,'protected',side_effect=Path);self.patch.start()
        self.service.controls={};self.service.policy={'archiveAt':2,'keepTerminal':0}
    def tearDown(self):self.patch.stop();self.tmp.cleanup()

    def test_rotation_wakes_changed_token_and_preserves_other_backoff(self):
        service=self.service;other=copy.deepcopy(service.plan['entries'][0]);other['profile']=str(uuid.uuid4());other['tokenEnvironment']='DZOKA_WORKER_TOKEN_TWO';service.plan['entries'].append(other)
        self.credentials.write_bytes(b'{"DZOKA_WORKER_TOKEN_ONE":"old-token","DZOKA_WORKER_TOKEN_TWO":"stable-token"}')
        service.refresh();service.entries[0][2].update(due=100,failures=2);service.entries[1][2].update(due=200,failures=3)
        self.credentials.write_bytes(b'{"DZOKA_WORKER_TOKEN_ONE":"new-token","DZOKA_WORKER_TOKEN_TWO":"stable-token"}')
        service.refresh();self.assertEqual(2,service.generation);self.assertEqual(0,service.entries[0][2]['due']);self.assertEqual({'due':200,'failures':3},service.entries[1][2])

    def test_invalid_replacement_pauses_without_old_token_fallback(self):
        self.service.refresh();self.credentials.write_bytes(b'{}');calls=[]
        result=self.service.step(invoke=lambda *a:calls.append(a))
        self.assertEqual([],calls);self.assertEqual('UNAVAILABLE',result['phase']);self.assertEqual('CREDENTIALS_INVALID',result['reason'])
        self.assertEqual(1,result['pausedCycles']);self.assertEqual(0,result['completedAcknowledgements'])

    def test_invalid_rotation_between_entries_prevents_second_claim(self):
        other=copy.deepcopy(self.service.plan['entries'][0]);other['profile']=str(uuid.uuid4());self.service.plan['entries'].append(other);calls=[]
        def invoke(*args):calls.append(args);self.credentials.write_bytes(b'{}');return True
        result=self.service.step(invoke=invoke,clock=lambda:0)
        self.assertEqual(1,len(calls));self.assertEqual(1,result['completedAcknowledgements']);self.assertEqual('CREDENTIALS_INVALID',result['reason'])

    def test_real_acknowledgement_idle_failure_and_stop_have_distinct_counters(self):
        outcomes=iter([True,False,ValueError('private failure')])
        def invoke(*_):
            value=next(outcomes)
            if isinstance(value,Exception):raise value
            return value
        for now in (0,2,4):result=self.service.step(invoke=invoke,clock=lambda:now)
        self.assertEqual((1,1,1),(result['completedAcknowledgements'],result['idleClaims'],result['failedInvocations']))
        self.service.stop.set();calls=[];self.assertEqual('STOPPED',self.service.step(invoke=lambda *_:calls.append(1))['phase']);self.assertFalse(calls)
        raw=(self.root/'managed-status.json').read_text(encoding='utf-8')
        for private in ('old-token',self.service.plan['coreUrl'],self.service.plan['entries'][0]['tenant'],'private failure'):self.assertNotIn(private,raw)

    def test_control_change_or_maintenance_failure_blocks_polling(self):
        for method in ('unchanged','maintenance'):
            with patch.object(self.service,method,side_effect=ValueError('unsafe')),patch.object(self.service,'refresh') as refresh:
                self.assertEqual('UNAVAILABLE',self.service.step()['phase']);refresh.assert_not_called()

    def test_rejected_privacy_replacement_cannot_claim(self):
        with patch.object(managed,'protected',side_effect=ValueError('Public credential file')):
            calls=[];self.assertEqual('CREDENTIALS_INVALID',self.service.step(invoke=lambda *_:calls.append(1))['reason']);self.assertFalse(calls)

    def test_stop_during_one_invocation_prevents_next_assignment(self):
        other=copy.deepcopy(self.service.plan['entries'][0]);other['profile']=str(uuid.uuid4());self.service.plan['entries'].append(other);calls=[]
        def invoke(*_):calls.append(1);self.service.stop.set();return True
        self.assertEqual('STOPPED',self.service.step(invoke=invoke)['phase']);self.assertEqual(1,len(calls))

    def test_release_lock_checks_unchanged_base_and_source_bytes(self):
        import managed_release as release
        release.verify_sources()
        # Reject both base-lock substitution and changed management bytes.
        fixture=self.root/'release';(fixture/'ai').mkdir(parents=True);(fixture/'tools').mkdir()
        import hashlib
        for name in ('service','broker'):(fixture/'ai'/f'{name}.lock.json').write_bytes(name.encode())
        source=fixture/'tools/managed.py';source.write_bytes(b'original')
        lock={'baseServiceLockSha256':hashlib.sha256(b'service').hexdigest(),'brokerLockSha256':hashlib.sha256(b'broker').hexdigest(),'sources':{'tools/managed.py':hashlib.sha256(b'original').hexdigest()}}
        (fixture/'ai/managed-service.lock.json').write_text(json.dumps(lock),encoding='utf-8')
        with patch.object(release,'ROOT',fixture),patch.object(release,'verify_base'):
            release.verify_sources()
            source.write_bytes(b'changed')
            with self.assertRaises(ValueError):release.verify_sources()
            source.write_bytes(b'original');(fixture/'ai/broker.lock.json').write_bytes(b'substituted')
            with self.assertRaises(ValueError):release.verify_sources()
