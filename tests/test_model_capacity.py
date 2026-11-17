import copy
import io
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
import model_capacity as c


class CapacityTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp');self.addCleanup(self.temp.cleanup)
        self.root=Path(self.temp.name)
        for name in ('data','bundle','state','state/profiles','state/docker','results'):(self.root/name).mkdir()
        self.data=self.root/'data';self.path=self.data/'plan.json';self.target=self.root/'results/run.jsonl';self.calls=[];self.now=0
        self.put('host.txt',b'Authored host/workload authority fixture; not an SLO.',True)
        self.put('input.txt',b'A private blue bag near the entrance.',True)
        self.plan={'schemaVersion':1,'scope':'LOCAL_CAPACITY_DIAGNOSTIC','hostEvidence':self.descriptor('host.txt'),'admissionBudgetSeconds':1800,
            'entries':[{'id':'private-workload','profile':'ISO_TEXT','profileKey':'1'*64,'input':self.descriptor('input.txt'),'warmup':1,'samples':3}]}
        self.save()
        for name in ('model_capacity.protected','model_capacity.plain','semantic_review.protected','semantic_review.plain','model_evaluation.plain'):
            p=patch(name,side_effect=Path);p.start();self.addCleanup(p.stop)
        for name in ('model_capacity.audit_records','semantic_review.audit_records','model_capacity.protect','model_capacity.verify_bundle','model_capacity.state_check'):
            p=patch(name);p.start();self.addCleanup(p.stop)
        p=patch.object(c,'profiles',return_value={'ISO_TEXT':{'key':'1'*64}});p.start();self.addCleanup(p.stop)
        p=patch.object(c,'infer',side_effect=self.inference);self.infer=p.start();self.addCleanup(p.stop)
        p=patch.object(c.time,'perf_counter_ns',side_effect=lambda:self.now);p.start();self.addCleanup(p.stop)

    def put(self,name,value,raw=False):
        (self.data/name).write_bytes(value if raw else (json.dumps(value)+'\n').encode())

    def descriptor(self,name):return {'path':name,'sha256':c.sha((self.data/name).read_bytes())}

    def save(self):self.put('plan.json',self.plan);return c.sha(self.path.read_bytes())

    def inference(self,*args,**kwargs):
        self.calls.append(args[3]);self.now+=(1000,10,20,30)[len(self.calls)-1]*1_000_000
        if len(self.calls)==4:raise OSError('private failure payload must not be logged')
        return {'embedding':[1.,0.],'inputTruncated':len(self.calls)==3}

    def run_capacity(self,receipt=None):return c.run(self.path,self.save(),self.root/'bundle','2'*64,self.root/'state',self.target,receipt)

    def ledger(self):
        raw=self.target.read_bytes();return c.inspect(raw,c.sha(raw))

    def test_actual_call_samples_exclude_warmup_and_unavailable_from_percentiles(self):
        self.assertEqual(0,self.run_capacity());record=self.ledger();self.assertEqual('SUMMARY',record['state'])
        entry=record['events'][-1]['entries'][0];measured=entry['measured']
        self.assertEqual(4,len(self.calls));self.assertEqual(1,entry['warmup']['available'])
        self.assertEqual((2,1),(measured['available'],measured['unavailable']))
        self.assertEqual((10.,20.),(measured['availableP50Ms'],measured['availableP95Ms']))
        self.assertTrue(record['events'][3]['inputTruncated']);self.assertIsNone(record['events'][1]['outputTruncated'])
        self.assertEqual('NOT_ATTESTED',record['events'][0]['executionIntegrity'])
        for private in ('private-workload','private blue bag','private failure payload','embedding'):self.assertNotIn(private,self.target.read_text())
        self.assertEqual('NOT_ESTABLISHED',record['events'][-1]['approval']);self.assertEqual('NOT_GRANTED',record['events'][-1]['releaseAuthorization'])

    def test_admission_budget_preserves_not_dispatched_coverage_without_retries(self):
        self.plan['admissionBudgetSeconds']=1;self.assertEqual(0,self.run_capacity())
        self.assertEqual(1,len(self.calls));entry=self.ledger()['events'][-1]['entries'][0]
        self.assertEqual(3,entry['measured']['not_dispatched']);self.assertIsNone(entry['measured']['availableP95Ms'])

    def test_cleanup_failure_aborts_and_never_starts_another_call(self):
        with patch.object(c,'state_check',side_effect=[None,OSError('private state detail')]):self.assertEqual(1,self.run_capacity())
        rows=self.ledger()['events'];self.assertEqual('ABORT',rows[-1]['kind']);self.assertEqual('UNCONFIRMED',rows[1]['state']);self.assertIsNone(rows[1]['outputSha256'])
        self.assertEqual(1,len(self.calls));self.assertEqual(3,rows[-1]['entries'][0]['measured']['unobserved'])
        self.assertNotIn('private state detail',self.target.read_text())

    def test_profile_boolean_budgets_bounds_and_shona_rejected_before_inference(self):
        original=copy.deepcopy(self.plan)
        for what in ('key','shona','warmup','samples','budget','calls','extra','empty'):
            self.plan=copy.deepcopy(original);entry=self.plan['entries'][0]
            if what=='key':entry['profileKey']='f'*64
            if what=='shona':entry['profile']='ISO_SHONA_CPU_CPU_SN_EN'
            if what=='warmup':entry['warmup']=True
            if what=='samples':entry['samples']=9
            if what=='budget':self.plan['admissionBudgetSeconds']=False
            if what=='calls':self.plan['entries']=[{**copy.deepcopy(entry),'id':'workload-'+str(i)} for i in range(4)]
            if what=='extra':self.plan['approval']='APPROVED'
            if what=='empty':self.plan['entries']=[]
            with self.subTest(what=what),self.assertRaises(ValueError):self.run_capacity()
        self.infer.assert_not_called();self.assertFalse(self.target.exists())

    def test_all_input_hashes_checked_before_any_inference(self):
        other=copy.deepcopy(self.plan['entries'][0]);other['id']='second';other['input']={'path':'missing.txt','sha256':'f'*64};self.plan['entries'].append(other)
        with self.assertRaises((ValueError,OSError)):self.run_capacity()
        self.infer.assert_not_called();self.assertFalse(self.target.exists())

    def test_manifest_tampering_basename_and_host_evidence_fail_closed(self):
        with self.assertRaises(ValueError):c.prepare(self.path,'f'*64)
        self.plan['hostEvidence']={'path':'../outside','sha256':'0'*64}
        with self.assertRaises(ValueError):self.run_capacity()
        self.infer.assert_not_called()

    def test_receipt_verified_before_private_data_and_never_claimed_by_direct_call(self):
        with patch('managed_release.verify',side_effect=ValueError('fixture')),patch.object(c,'prepare') as prepare:
            with self.assertRaises(ValueError):self.run_capacity('3'*64)
            prepare.assert_not_called()
        with patch('managed_release.verify') as verify:self.assertEqual(0,self.run_capacity('3'*64))
        verify.assert_called_once_with('3'*64)
        self.assertEqual('PROTECTED_RELEASE_VERIFIED',self.ledger()['events'][0]['executionIntegrity'])

    def test_output_exclusive_and_existing_state_not_discarded(self):
        self.target.write_bytes(b'prior evidence')
        with self.assertRaises(ValueError):self.run_capacity()
        self.assertEqual(b'prior evidence',self.target.read_bytes());self.target.unlink()
        (self.root/'state/profiles/prior.json').write_bytes(b'{}')
        with self.assertRaises(ValueError):self.run_capacity()
        self.infer.assert_not_called();self.assertTrue((self.root/'state/profiles/prior.json').exists())

    def test_native_state_owner_collision_denies_dispatch(self):
        guard=c.OwnerLock(self.root/'state',c.OWNER);self.assertTrue(guard.acquire())
        try:
            with self.assertRaises(ValueError):self.run_capacity()
        finally:guard.close()
        self.infer.assert_not_called()

    def test_provenance_chain_detects_changes_and_missing_terminal_stays_incomplete(self):
        self.run_capacity();raw=self.target.read_bytes()
        with self.assertRaises(ValueError):c.inspect(raw,b'0'.decode()*64)
        lines=raw.splitlines(keepends=True);tampered=lines[0]+lines[2]+b''.join(lines[3:])
        with self.assertRaises(ValueError):c.inspect(tampered,c.sha(tampered))
        prefix=b''.join(lines[:-1]);self.assertEqual('INCOMPLETE',c.inspect(prefix,c.sha(prefix))['state'])
        with self.assertRaises(ValueError):c.inspect(prefix[:-1],c.sha(prefix[:-1]))

    def test_initial_bundle_integrity_failure_starts_no_sample_or_ledger(self):
        with patch.object(c,'verify_bundle',side_effect=ValueError('fixture')),self.assertRaises(ValueError):self.run_capacity()
        self.infer.assert_not_called();self.assertFalse(self.target.exists())

    def test_clock_regression_aborts_instead_of_fabricating_zero_latency(self):
        def bad_clock(*args,**kwargs):self.now=-1;return {'embedding':[1.,0.]}
        with patch.object(c,'infer',side_effect=bad_clock):self.assertEqual(1,self.run_capacity())
        record=self.ledger();self.assertEqual('ABORT',record['state'])
        self.assertEqual(0,record['events'][-1]['entries'][0]['measured']['available'])
        self.assertIsNone(record['events'][-1]['entries'][0]['measured']['availableP95Ms'])


if __name__=='__main__':unittest.main()
