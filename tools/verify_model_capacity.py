"""Actual non-Shona model timing/availability and protected capacity CLI checks."""
import copy
import json
import os
from pathlib import Path
import subprocess
import sys
import uuid
ROOT=Path(__file__).resolve().parents[1];sys.path[:0]=[str(ROOT/'tools'),str(ROOT/'ai')]
from broker_bundle import protect
from broker_profiles import profiles
from model_capacity import OWNER, inspect, sha
from docker_lifecycle import OwnerLock
from verify_worker_boundary import private_fixture


def main():
    release=Path(os.environ['DZOKA_TEST_MANAGED_RELEASE']);receipt=os.environ['DZOKA_TEST_MANAGED_SHA256']
    assert (release/'tools/model_capacity.py').read_bytes()==(ROOT/'tools/model_capacity.py').read_bytes()
    bundle=os.environ['DZOKA_TEST_BROKER_BUNDLE'];bundle_sha=os.environ['DZOKA_TEST_BROKER_SHA256'];checks=[]
    destination=ROOT/'.test-tmp'/('model-capacity-'+uuid.uuid4().hex);destination.mkdir()
    with private_fixture() as root:
        protect(root)
        for name in ('data','results','state','state/profiles','state/docker','second','second/profiles','second/docker'):
            (root/name).mkdir();protect(root/name)
        data=root/'data'
        def put(name,raw):
            path=data/name;path.write_bytes(raw);protect(path);return {'path':name,'sha256':sha(raw)}
        evidence=put('host.txt',b'Authored local shared-host diagnostic. Existing workloads uncontrolled. No production SLO, hardware certification or quality acceptance.')
        text=put('source.txt',b'A blue bag was reported near the entrance, perhaps on Monday.')
        invalid=put('invalid.txt',b'\xff')
        entry={'id':'private-valid-workload','profile':'ISO_TEXT','profileKey':profiles()['ISO_TEXT']['key'],'input':text,'warmup':1,'samples':2}
        bad={**entry,'id':'private-invalid-utf8','input':invalid,'warmup':0,'samples':1}
        plan={'schemaVersion':1,'scope':'LOCAL_CAPACITY_DIAGNOSTIC','hostEvidence':evidence,'admissionBudgetSeconds':1800,'entries':[entry,bad]}
        plan_desc=put('plan.json',(json.dumps(plan)+'\n').encode())
        target=root/'results/complete.jsonl'
        def command(plan_file,plan_sha,state,output):
            return [sys.executable,'-I','-S','-B',str(release/'tools/model_capacity.py'),'--plan',str(plan_file),'--plan-sha256',plan_sha,
                '--bundle',bundle,'--bundle-sha256',bundle_sha,'--state',str(state),'--output',str(output),'--management-sha256',receipt]
        invocation=command(data/'plan.json',plan_desc['sha256'],root/'state',target)
        result=subprocess.run(invocation,capture_output=True,timeout=1200)
        (root/'results/invocation.log').write_bytes(result.stdout+result.stderr);protect(root/'results/invocation.log')
        assert result.returncode==0,'Capacity diagnostic failed; private execution evidence retained'
        raw=target.read_bytes();record=inspect(raw,sha(raw));events=record['events'];summary=events[-1]
        assert record['state']=='SUMMARY' and summary['calls']==4
        assert events[0]['managementReceiptSha256']==receipt and events[0]['executionIntegrity']=='PROTECTED_RELEASE_VERIFIED'
        assert events[0]['planSha256']==plan_desc['sha256'] and events[0]['brokerBundleSha256']==bundle_sha
        first,second=summary['entries'];assert first['warmup']['available']==1 and first['measured']['available']==2 and second['measured']['unavailable']==1
        assert first['measured']['availableP95Ms'] is not None and second['measured']['availableP95Ms'] is None
        assert all(r['outputSha256'] is not None for r in events[1:4]) and events[4]['outputSha256'] is None
        assert summary['slo']==summary['approval']=='NOT_ESTABLISHED' and summary['releaseAuthorization']=='NOT_GRANTED'
        for private in (b'private-valid-workload',b'near the entrance',b'private-invalid-utf8',b'embedding'):assert private not in raw
        (destination/'ledger.jsonl').write_bytes(raw)
        checks+=['actual_isolated_text_warmup_and_two_measured_calls','actual_invalid_utf8_is_unavailable_not_retried','receipt_profile_input_and_output_provenance',
            'available_only_percentiles_and_separate_coverage','redacted_flushed_hash_chain_never_grants_slo_or_release']
        assert subprocess.run(invocation,capture_output=True,timeout=180).returncode!=0 and target.read_bytes()==raw
        checks.append('exclusive_output_preserves_previous_execution')
        subprocess.run(['icacls',str(data/'source.txt'),'/grant','*S-1-5-11:(RX)'],check=True,stdout=subprocess.DEVNULL)
        try:
            bad_target=root/'results/broad.jsonl'
            assert subprocess.run(command(data/'plan.json',plan_desc['sha256'],root/'second',bad_target),capture_output=True,timeout=180).returncode!=0 and not bad_target.exists()
        finally:subprocess.run(['icacls',str(data/'source.txt'),'/remove:g','*S-1-5-11'],check=True,stdout=subprocess.DEVNULL)
        checks.append('native_broad_input_read_rejected_before_dispatch')
        guard=OwnerLock(root/'second',OWNER);assert guard.acquire();protect(root/'second'/(OWNER+'.lock'))
        try:
            collision=root/'results/collision.jsonl'
            assert subprocess.run(command(data/'plan.json',plan_desc['sha256'],root/'second',collision),capture_output=True,timeout=180).returncode!=0 and not collision.exists()
        finally:guard.close()
        checks.append('actual_state_owner_lock_excludes_competing_dispatch')
        budget=copy.deepcopy(plan);budget['admissionBudgetSeconds']=1;budget_desc=put('budget-plan.json',(json.dumps(budget)+'\n').encode())
        budget_target=root/'results/budget.jsonl'
        assert subprocess.run(command(data/'budget-plan.json',budget_desc['sha256'],root/'second',budget_target),capture_output=True,timeout=600).returncode==0
        budget_raw=budget_target.read_bytes();budget_result=inspect(budget_raw,sha(budget_raw));budget_summary=budget_result['events'][-1]
        assert budget_summary['entries'][0]['warmup']['available']==1
        assert sum(e['measured']['not_dispatched'] for e in budget_summary['entries'])==3
        assert all(e['measured']['availableP95Ms'] is None for e in budget_summary['entries'])
        (destination/'budget-ledger.jsonl').write_bytes(budget_raw);checks.append('real_call_exhausts_admission_budget_without_false_zero_latency')
        from model_broker import state_check
        state_check(root/'state',bundle,bundle_sha);state_check(root/'second',bundle,bundle_sha)
        from broker_bundle import verify_bundle
        verify_bundle(bundle,bundle_sha)
    checks.append('owned_profiles_and_bundle_grants_confirmed_clean')
    result={'status':'PASS','checks':checks,'managementReceiptSha256':receipt,'protocolSourceSha256':sha((ROOT/'tools/model_capacity.py').read_bytes()),
        'ledgerSha256':sha(raw),'budgetLedgerSha256':sha(budget_raw),'measuredEntries':summary['entries'],
        'scope':'Serial fresh-process model path on a shared host; authored inputs; no HTTP/DB queue, saturation, quality or production SLO acceptance'}
    (destination/'result.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
    print('PASS: native bounded model capacity diagnostic; '+str(destination/'result.json'),flush=True)


if __name__=='__main__':main()
