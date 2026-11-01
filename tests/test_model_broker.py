import copy
import io
import json
import struct
import sys
import tempfile
import unittest
import uuid
from pathlib import Path
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'tools'))
sys.path.insert(0, str(ROOT / 'ai'))
import model_wire as wire
import model_broker as broker
from broker_plan import load_plan
from broker_supervisor import BrokerSupervisor


class BrokerTests(unittest.TestCase):
    def test_wire_rejects_duplicate_nonfinite_truncated_and_oversized_frames(self):
        for raw in (b'{"x":1,"x":2}', b'{"x":NaN}', b'{"x":Infinity}'):
            with self.assertRaises(ValueError):
                wire.read(io.BytesIO(struct.pack('<I',len(raw))+raw),100)
        for raw in (b'', b'\x01', struct.pack('<I',0), struct.pack('<I',101), struct.pack('<I',2)+b'{'):
            with self.assertRaises(ValueError):wire.read(io.BytesIO(raw),100)
        with self.assertRaises(ValueError):wire.encode({'x':float('nan')},100)

    def test_wire_handles_partial_reads_and_writes(self):
        class Short(io.BytesIO):
            def read(self,n=-1):return super().read(min(n,2))
            def write(self,data):return super().write(data[:2])
        stream=Short();value={'text':'Shona: ndarasa bhegi','n':[1,2]}
        wire.write(stream,value,1024);stream.seek(0)
        self.assertEqual(value,wire.read(stream,1024))

    def transcript(self,packets,native=lambda _:['token']):
        request={'version':1,'id':'invocation','inputSha256':'a'*64}
        stream=io.BytesIO(b''.join(wire.encode(p,wire.RESULT_LIMIT) for p in packets))
        return wire.conversation(stream,io.BytesIO(),request,native)

    def packets(self):
        return ({'version':1,'id':'invocation','type':'ready','boundary':'no-capabilities-egress-denied-v1'},
                {'version':1,'id':'invocation','type':'result','inputSha256':'a'*64,'profileKey':'b'*64,'output':{}})

    def test_protocol_rejects_missing_duplicate_or_incorrect_boundary_proof(self):
        ready,result=self.packets();self.transcript([ready,result])
        for packets in ([result],[ready,ready,result],[{**ready,'boundary':'trust-me'},result]):
            with self.assertRaises(ValueError):self.transcript(packets)

    def test_protocol_rejects_cross_invocation_wrong_input_unknown_operation_and_trailing_frames(self):
        ready,result=self.packets()
        for changed in ({**result,'id':'another'},{**result,'inputSha256':'c'*64},{**result,'type':'fetch-url'},{**result,'url':'https://untrusted.example'}):
            with self.assertRaises(ValueError):self.transcript([ready,changed])
        with self.assertRaises(ValueError):self.transcript([ready,result,result])

    def test_protocol_limits_native_call_and_host_can_deny_capability(self):
        ready,result=self.packets();native={'version':1,'id':'invocation','type':'native','pieces':['test']}
        self.transcript([ready,native,result])
        with self.assertRaises(ValueError):self.transcript([ready,native,native,result])
        with self.assertRaises(ValueError):self.transcript([ready,{**native,'image':'unapproved'},result])
        def denied(_):raise ValueError('Non-CPU capability denied')
        with self.assertRaises(ValueError):self.transcript([ready,native,result],denied)

    def plan(self):
        return {'schemaVersion':1,'coreUrl':'https://core.example','allowInsecureLoopback':False,
                'pollSeconds':1,'backoffMaxSeconds':8,'entries':[{
                    'tenant':str(uuid.uuid4()),'profile':str(uuid.uuid4()),'profileName':'ISO_TEXT',
                    'bundle':str(ROOT/'.test-tmp/bundle'),'bundleSha256':'a'*64,
                    'stateDirectory':str(ROOT/'.test-tmp/private-state'),'tokenEnvironment':'DZOKA_WORKER_TOKEN_ONE'}]}

    def load(self,value):
        with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as tmp:
            path=Path(tmp)/'plan.json';path.write_bytes(json.dumps(value).encode())
            return load_plan(path,preflight=False)

    def test_plan_rejects_cross_tenant_state_overlap_and_duplicate_assignment(self):
        plan=self.plan();self.load(plan)
        plan['entries'].append(copy.deepcopy(plan['entries'][0]))
        with self.assertRaises(ValueError):self.load(plan)
        plan['entries'][1]['tenant']=str(uuid.uuid4())
        plan['entries'][1]['stateDirectory']+= '/nested'
        with self.assertRaises(ValueError):self.load(plan)

    def test_plan_requires_tls_fixed_profile_absolute_paths_and_disjoint_state(self):
        for field,value in [('coreUrl','http://core.example'),('coreUrl','https://user@core.example'),('coreUrl','https://core.example/other'),('allowInsecureLoopback',1),('pollSeconds',True)]:
            plan=self.plan();plan[field]=value
            with self.assertRaises(ValueError):self.load(plan)
        for field,value in [('bundle','relative'),('profileName','CPU_SN_EN'),('bundleSha256','0'*63),('tokenEnvironment','DATABASE_URL')]:
            plan=self.plan();plan['entries'][0][field]=value
            with self.assertRaises(ValueError):self.load(plan)
        plan=self.plan();plan['entries'][0]['stateDirectory']=plan['entries'][0]['bundle']
        with self.assertRaises(ValueError):self.load(plan)

    def test_plan_recovers_all_states_before_strict_bundle_privacy_and_never_claims(self):
        plan=self.plan();other=copy.deepcopy(plan['entries'][0]);other['tenant']=str(uuid.uuid4());other['stateDirectory']+='-other';plan['entries'].append(other)
        events=[]
        with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as tmp:
            path=Path(tmp)/'plan.json';path.write_bytes(json.dumps(plan).encode())
            with patch('broker_plan.verify_bundle',side_effect=lambda *_,**kw:events.append('inspect' if kw.get('pending_grants') else 'strict')),patch('broker_plan.state_check',side_effect=lambda *_:events.append('recover')),patch.object(broker,'request') as request:
                load_plan(path);request.assert_not_called()
        self.assertEqual(['inspect','recover','recover','strict'],events)

    def test_profile_identity_is_appended_and_joint_space_remains_paired(self):
        from broker_profiles import profiles
        from provider import load_profile
        values=profiles();self.assertEqual(34,len(values))
        self.assertNotEqual(values['ISO_TEXT']['key'],load_profile()['space']['spaceKey'])
        one=values['ISO_VISION_JOINT_IMAGE']['manifest']['sharedSpace']
        self.assertEqual(one,values['ISO_VISION_JOINT_TEXT']['manifest']['sharedSpace'])
        for value in values.values():
            if value['family']!='text':self.assertNotEqual(value['key'],value['manifest']['configuration']['baseProfileKey'])

    def test_supervisor_validates_all_tokens_before_any_claim_and_stops(self):
        plan=self.plan()
        with self.assertRaises(ValueError):BrokerSupervisor(plan,{})
        worker=BrokerSupervisor(plan,{'DZOKA_WORKER_TOKEN_ONE':'synthetic-token'})
        worker.stop.set()
        with patch('broker_supervisor.run_once') as run:worker.tick();run.assert_not_called()

    def test_supervisor_failure_does_not_starve_other_assignment(self):
        plan=self.plan();other=copy.deepcopy(plan['entries'][0]);other['profile']=str(uuid.uuid4());plan['entries'].append(other)
        worker=BrokerSupervisor(plan,{'DZOKA_WORKER_TOKEN_ONE':'synthetic-token'})
        with patch('broker_supervisor.run_once',side_effect=[ValueError('private'),False]) as run:worker.tick();self.assertEqual(2,run.call_count)
        self.assertEqual([1,0],[e[2]['failures'] for e in worker.entries])

    def test_bundle_failure_precedes_claim(self):
        entry=self.plan()['entries'][0]
        with patch.object(broker,'verify_bundle',side_effect=ValueError('tamper')),patch.object(broker,'request') as request:
            with self.assertRaises(ValueError):broker.run_once('https://core.example','synthetic-token',entry['tenant'],entry['profile'],'ISO_TEXT',entry['bundle'],entry['bundleSha256'],entry['stateDirectory'])
            request.assert_not_called()

    def test_claim_digest_mismatch_fails_without_model_execution(self):
        entry=self.plan()['entries'][0]
        from broker_profiles import profiles
        job={'jobId':str(uuid.uuid4()),'lease':str(uuid.uuid4()),'text':'synthetic private input',
             'space':{'spaceKey':profiles()['ISO_TEXT']['key']},'inputSha256':'0'*64}
        with patch.object(broker,'verify_bundle'),patch.object(broker,'state_check'),patch.object(broker,'infer') as infer,patch.object(broker,'request',side_effect=[job,True]) as request:
            with self.assertRaisesRegex(ValueError,'Isolated inference unavailable'):
                broker.run_once('https://core.example','synthetic-token',entry['tenant'],entry['profile'],'ISO_TEXT',entry['bundle'],entry['bundleSha256'],entry['stateDirectory'])
            infer.assert_not_called();self.assertTrue(request.call_args.args[2].endswith('/fail'))
            self.assertEqual({'lease':job['lease'],'reason':'PROVIDER_UNAVAILABLE'},request.call_args.args[3])

    def test_ambiguous_completion_replays_identical_envelope_without_new_inference(self):
        import hashlib
        entry=self.plan()['entries'][0]
        from broker_profiles import profiles
        job={'jobId':str(uuid.uuid4()),'lease':str(uuid.uuid4()),'text':'synthetic private input',
             'space':{'spaceKey':profiles()['ISO_TEXT']['key']},'inputSha256':hashlib.sha256(b'synthetic private input').hexdigest()}
        with patch.object(broker,'verify_bundle'),patch.object(broker,'state_check'),patch.object(broker,'infer',return_value={'vector':[1.0],'tokenCount':3,'truncated':False}) as infer,patch.object(broker,'request',side_effect=[job,TimeoutError(),True]) as request:
            self.assertTrue(broker.run_once('https://core.example','synthetic-token',entry['tenant'],entry['profile'],'ISO_TEXT',entry['bundle'],entry['bundleSha256'],entry['stateDirectory']))
            self.assertEqual(1,infer.call_count)
            self.assertIs(request.call_args_list[1].args[3],request.call_args_list[2].args[3])

