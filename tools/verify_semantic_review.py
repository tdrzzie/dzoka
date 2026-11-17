"""Native private English inference/review protocol conformance, not quality approval."""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import uuid
ROOT=Path(__file__).resolve().parents[1]
sys.path[:0]=[str(ROOT/'tools'),str(ROOT/'ai')]
from broker_bundle import protect
from semantic_review import DIMENSIONS, ROLES, evaluate, sha


class Fixture:
    """Explicit authored judgments; never names actual humans or their qualifications."""
    def __init__(self,root,baseline,candidate,keys):
        self.root=Path(root)
        self.source=b'I reported a blue bag near the entrance, perhaps on Monday.'
        self.permission=self.put('authority.txt',b'AUTHORED PROTOCOL FIXTURE. No human review, quality acceptance or real user data.',True)
        source=self.put('source.txt',self.source,True);reference=self.put('reference.json',{'criteria':'Preserve source facts and uncertainty; make no ownership/release claim.'})
        request=self.put('input.json',{'prompt':'Describe the reported item in one sentence.','query':self.source.decode()})
        base=self.put('baseline.json',baseline);cand=self.put('candidate.json',candidate)
        profiles={role:{'profileKey':keys[role],'projectionSourceSha256':sha(Path(__file__).read_bytes())} for role in ROLES}
        cases=[]
        for index in range(3):
            cases.append({'id':'private-case-'+str(index),'split':'TEST','slice':'private-authored-slice',
                          'inputSha256':request['sha256'],'entityGroup':'private-entity','institutionGroup':'private-institution','timeGroup':'private-time',
                          'input':request,'source':source,'reference':reference,'baseline':{'state':'UNAVAILABLE' if index==1 else 'AVAILABLE','artifact':None if index==1 else base},
                          'candidate':{'state':'TRUNCATED' if index==1 else 'AVAILABLE','artifact':cand}})
        self.packet={'schemaVersion':1,'scope':'AUTHORED_DIAGNOSTIC','language':'en','task':'ENGLISH_DRAFT','evidence':self.permission,'profiles':profiles,'cases':cases}
        packet=self.put('packet.json',self.packet);self.reviews=[]
        for index,who in enumerate(('a'*32,'b'*32)):
            findings=[]
            for case_index,case in enumerate(cases):
                for role in ROLES:
                    state='PROVIDER_UNAVAILABLE' if case[role]['state']=='UNAVAILABLE' else 'ASSESSED'
                    if index==1 and case_index==2 and role=='candidate':state='UNREVIEWED'
                    verdicts={dimension:'PASS' for dimension in DIMENSIONS} if state=='ASSESSED' else None
                    if index==1 and case_index==0:
                        verdicts['GROUNDING' if role=='baseline' else 'UNCERTAINTY']='FAIL' if role=='baseline' else 'UNCERTAIN'
                    findings.append({'caseId':case['id'],'role':role,'state':state,'verdicts':verdicts})
            self.reviews.append({'schemaVersion':1,'packetSha256':packet['sha256'],'reviewerId':who,'reviewerKind':'AUTHORED_FIXTURE',
                                 'authorityEvidence':self.permission,'at':'2026-11-17T00:00:00Z','findings':findings})
        reviews=[self.put('review-'+str(i)+'.json',review) for i,review in enumerate(self.reviews)]
        review_set=sha(json.dumps(sorted(v['sha256'] for v in reviews),separators=(',',':')).encode())
        self.adjudication={'schemaVersion':1,'packetSha256':packet['sha256'],'reviewSetSha256':review_set,'reviewerId':'c'*32,'reviewerKind':'AUTHORED_FIXTURE',
                           'authorityEvidence':self.permission,'at':'2026-11-17T00:00:00Z',
                           'decisions':[{'caseId':cases[0]['id'],'role':'baseline','dimension':'GROUNDING','verdict':'UNCERTAIN','rationaleEvidence':self.permission}]}
        self.manifest={'schemaVersion':1,'packet':packet,'reviews':reviews,'adjudications':[self.put('adjudication.json',self.adjudication)]}
        self.path=self.root/'manifest.json';self.save()

    def put(self,name,value,raw=False):
        content=value if raw else (json.dumps(value,allow_nan=False)+'\n').encode()
        (self.root/name).write_bytes(content)
        return {'path':name,'sha256':sha(content)}

    def save(self):
        self.put(self.path.name,self.manifest)
        return sha(self.path.read_bytes())


def main():
    from verify_worker_boundary import private_fixture
    from model_broker import infer,state_check
    from broker_profiles import profiles
    from docker_lifecycle import preflight,reconcile
    release=Path(os.environ['DZOKA_TEST_MANAGED_RELEASE']);receipt=os.environ['DZOKA_TEST_MANAGED_SHA256']
    assert (release/'tools/semantic_review.py').read_bytes()==(ROOT/'tools/semantic_review.py').read_bytes()
    bundle=os.environ['DZOKA_TEST_BROKER_BUNDLE'];bundle_sha=os.environ['DZOKA_TEST_BROKER_SHA256'];cases=[]
    with private_fixture() as root:
        protect(root)
        for name in ('state','state/profiles','state/docker','data','actual','results'):(root/name).mkdir();protect(root/name)
        raw=(json.dumps({'prompt':'Describe the reported item in one sentence.','query':'I reported a blue bag near the entrance, perhaps on Monday.'})+'\n').encode()
        names={'baseline':'ISO_GENERATIVE_LLM_EN','candidate':'ISO_QUALITY_SMOL360_EN'}
        evidence={role:infer(bundle,bundle_sha,root/'state',name,raw,checked=True) for role,name in names.items()}
        # Keep authored disagreement fixtures separate from actual provider state.
        fixture=Fixture(root/'data',{'text':'Authored baseline proposal.'},{'text':'Authored candidate proposal.'},
                        {role:sha(('authored-review-'+role+'-not-a-model').encode()) for role in ROLES})
        for path in (root/'data').iterdir():protect(path)
        expected=fixture.save();protect(fixture.path)
        target=root/'results/review.json'
        command=[sys.executable,'-I','-B',str(release/'tools/semantic_review.py'),'--manifest',str(fixture.path),'--manifest-sha256',expected,
                 '--management-sha256',receipt,'--output',str(target)]
        subprocess.run(command,check=True,timeout=180);result=json.loads(target.read_bytes());original=target.read_bytes()
        assert result['adjudicatedDimensions']==result['unresolvedDimensions']==1
        assert result['coverage']['baseline']=={'available':2,'truncated':0,'unavailable':1,'fullyReviewed':2,'declaredHumanReviewed':0}
        assert result['coverage']['candidate']=={'available':2,'truncated':1,'unavailable':0,'fullyReviewed':1,'declaredHumanReviewed':0}
        assert result['approval']=='NOT_ESTABLISHED' and result['releaseAuthorization']=='NOT_GRANTED'
        assert result['managementReceiptSha256']==receipt and result['executionIntegrity']=='PROTECTED_RELEASE_VERIFIED'
        assert all(text not in target.read_text() for text in ('private-case','private-authored','private-entity','perhaps on Monday','a'*32))
        cases+=['protected_receipt_verified_cli','review_disagreement_adjudication_and_incomplete_coverage','redacted_result_never_grants_approval']
        actual=Fixture(root/'actual',evidence['baseline'],evidence['candidate'],{role:profiles()[name]['key'] for role,name in names.items()})
        assert (root/'actual/input.json').read_bytes()==raw
        actual.packet['cases']=actual.packet['cases'][:1]
        for role in ROLES:
            actual.packet['cases'][0][role]['state']='TRUNCATED' if evidence[role]['inputTruncated'] or evidence[role]['outputTruncated'] else 'AVAILABLE'
        actual.manifest['packet']=actual.put('packet.json',actual.packet)
        for index,review in enumerate(actual.reviews):
            review['packetSha256']=actual.manifest['packet']['sha256'];review['findings']=review['findings'][:2]
            for finding in review['findings']:finding.update(state='UNREVIEWED',verdicts=None)
            actual.manifest['reviews'][index]=actual.put('review-'+str(index)+'.json',review)
        actual.manifest['adjudications']=[];actual_sha=actual.save()
        for path in (root/'actual').iterdir():protect(path)
        actual_target=root/'results/actual-review.json'
        actual_command=[sys.executable,'-I','-B',str(release/'tools/semantic_review.py'),'--manifest',str(actual.path),'--manifest-sha256',actual_sha,
                        '--management-sha256',receipt,'--output',str(actual_target)]
        subprocess.run(actual_command,check=True,timeout=180);actual_result=json.loads(actual_target.read_bytes())
        for row in actual_result['rows']:
            assert row['inputSha256']==sha(raw) and row['artifactSha256']==actual.packet['cases'][0][row['role']]['artifact']['sha256']
            assert row['profileKey']==profiles()[names[row['role']]]['key'] and row['providerState']==actual.packet['cases'][0][row['role']]['state']
            assert row['assessedReviewers']==0
        cases.append('actual_isolated_english_outputs_preserve_input_profile_truncation_and_unreviewed_state')
        assert subprocess.run(command,capture_output=True,timeout=180).returncode!=0 and target.read_bytes()==original
        cases.append('exclusive_result_preserves_prior_evidence')
        subprocess.run(['icacls',str(root/'data/baseline.json'),'/grant','*S-1-5-11:(RX)'],check=True,stdout=subprocess.DEVNULL)
        try:
            bad=command[:-1]+[str(root/'results/broad.json')]
            assert subprocess.run(bad,capture_output=True,timeout=180).returncode!=0 and not (root/'results/broad.json').exists()
            cases.append('native_broad_leaf_read_rejected')
        finally:subprocess.run(['icacls',str(root/'data/baseline.json'),'/remove:g','*S-1-5-11'],check=True,stdout=subprocess.DEVNULL)
        state_check(root/'state',bundle,bundle_sha);preflight();resources=reconcile(root/'state/docker');assert resources['active']==resources['unresolved']==0
    cases.append('owned_inference_resources_cleaned')
    path=ROOT/'.test-tmp'/('semantic-review-'+uuid.uuid4().hex+'.json')
    path.write_text(json.dumps({'status':'PASS','cases':cases,'managementReceiptSha256':receipt,'protocolSourceSha256':result['protocolSourceSha256'],
                                'scope':'Actual English inference and authored review-protocol conformance; no human quality approval'},indent=2)+'\n',encoding='utf-8')
    print('PASS: private English semantic review; '+str(path),flush=True)


if __name__=='__main__':main()
