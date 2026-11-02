"""Real SDK metadata/ACL archival and private management release acceptance."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import uuid
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'tools'))

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--native-result');args=parser.parse_args()
    release=Path(os.environ['DZOKA_TEST_MANAGED_RELEASE']).resolve();receipt=os.environ['DZOKA_TEST_MANAGED_SHA256']
    if args.native_result is None:
        result=ROOT/'.test-tmp'/('managed-operations-'+uuid.uuid4().hex);result.mkdir()
        subprocess.run([sys.executable,'-I','-B',str(release/'tools/verify_managed_operations.py'),'--native-result',str(result/'result.json')],check=True,timeout=600)
        print('PASS: native managed operations; '+str(result/'result.json'),flush=True);return
    if ROOT!=release:raise ValueError('Private management release required')
    from managed_release import verify
    from profile_archive import archive,verify as verify_archive,protected
    from appcontainer_boundary import Profile
    from broker_bundle import protect
    from docker_lifecycle import OwnerLock
    from service_account import identity
    from verify_worker_boundary import private_fixture
    verify(receipt);account=identity()['sid'];cases=[]
    with private_fixture() as root:
        protect(root);state=root/'profiles';state.mkdir();protect(state);records=[]
        for _ in range(3):
            with Profile(state) as profile:pass
            records.append((profile.path,profile.owner,profile.path.read_bytes()))
        # The producer's inherited private leaf ACL is accepted. A broad read
        # exception on one leaf must instead fail before any acknowledgement.
        path=records[0][0]
        subprocess.run(['icacls',str(path),'/grant','*S-1-5-11:(RX)'],check=True,stdout=subprocess.DEVNULL)
        try:
            try:archive(state,account,0)
            except ValueError:cases.append('broad_leaf_read_denied')
            else:raise AssertionError('Broad leaf ACL accepted')
            assert path.exists()
        finally:subprocess.run(['icacls',str(path),'/remove:g','*S-1-5-11'],check=True,stdout=subprocess.DEVNULL)
        guard=OwnerLock(state,records[0][1]);assert guard.acquire()
        grant=state/('grant-'+records[1][1]+'.json');grant.write_bytes(b'{}')
        try:
            with Profile(state) as live:
                try:archive(state,account,0,after_receipt=lambda:(_ for _ in ()).throw(RuntimeError('crash seam')))
                except RuntimeError:cases.append('receipt_before_acknowledgement_crash')
                else:raise AssertionError('Fault seam not reached')
                outcome=archive(state,account,0)
                assert outcome=={'archived':1,'active':1,'retained':2},outcome
                assert live.path.exists() and records[0][0].exists() and records[1][0].exists()
                cases+=['actual_live_sdk_profile_retained','actual_owner_lock_excluded','pending_grant_record_retained']
        finally:guard.close();grant.unlink()
        outcome=archive(state,account,0);assert outcome['archived']==3,outcome
        assert verify_archive(state/'archive',account)==4
        for path,_,raw in records:assert (state/'archive'/path.name).read_bytes()==raw
        assert not list(state.glob('offline-*.json'))
        cases+=['four_actual_sdk_terminal_records_archived','inherited_private_leaf_supported','exact_bytes_and_receipts_verified']
        assert archive(state,account,0)['archived']==0;cases.append('native_retry_idempotent')
        from model_evaluation import evaluate,digest
        dataset=root/'dataset';dataset.mkdir();protect(dataset)
        (dataset/'source.txt').write_bytes("J'ai perdu un sac noir.".encode())
        (dataset/'evidence.txt').write_bytes(b'Authored synthetic diagnostic. Not representative quality or legal approval.')
        manifest={'schemaVersion':1,'scope':'AUTHORED_DIAGNOSTIC','evidence':{'path':'evidence.txt','sha256':digest((dataset/'evidence.txt').read_bytes())},'cases':[{'profile':'ISO_LANGUAGE_TRANSLATION','path':'source.txt','sha256':digest((dataset/'source.txt').read_bytes()),'reference':'I lost a black bag.','metric':'exact'}]}
        (dataset/'manifest.json').write_bytes(json.dumps(manifest).encode())
        inference=root/'inference';inference.mkdir();protect(inference)
        for name in ('profiles','docker'):(inference/name).mkdir();protect(inference/name)
        diagnostic=evaluate(dataset/'manifest.json',os.environ['DZOKA_TEST_BROKER_BUNDLE'],os.environ['DZOKA_TEST_BROKER_SHA256'],inference)
        assert diagnostic['approval']=='NOT_ESTABLISHED' and len(diagnostic['rows'])==1
        assert 'I lost a black bag.' not in json.dumps(diagnostic)
        cases.append('real_isolated_translation_private_reference_diagnostic')
    Path(args.native_result).write_bytes((json.dumps({'status':'PASS','releaseSha256':receipt,'cases':cases},indent=2)+'\n').encode())

if __name__=='__main__':main()
