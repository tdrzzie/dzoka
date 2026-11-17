import copy
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
import semantic_review as q
from verify_semantic_review import Fixture


class SemanticReviewTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp');self.addCleanup(self.temp.cleanup)
        self.root=Path(self.temp.name);self.f=Fixture(self.root,{'text':'private baseline proposal'},{'text':'private candidate proposal'},{'baseline':'1'*64,'candidate':'2'*64})
        for target in ('semantic_review.protected','semantic_review.plain','model_evaluation.plain'):
            p=patch(target,side_effect=Path);p.start();self.addCleanup(p.stop)
        p=patch.object(q,'audit_records');p.start();self.addCleanup(p.stop)

    def run_review(self):return q.evaluate(self.f.path,self.f.save())

    def packet(self):self.f.manifest['packet']=self.f.put('packet.json',self.f.packet)

    def review(self,index):self.f.manifest['reviews'][index]=self.f.put('review-'+str(index)+'.json',self.f.reviews[index])

    def adjudication(self):self.f.manifest['adjudications']=[self.f.put('adjudication.json',self.f.adjudication)]

    def test_actual_disagreement_resolution_is_separate_from_coverage_and_approval(self):
        result=self.run_review();text=json.dumps(result)
        self.assertEqual((1,1),(result['adjudicatedDimensions'],result['unresolvedDimensions']))
        self.assertEqual({'available':2,'truncated':0,'unavailable':1,'fullyReviewed':2,'declaredHumanReviewed':0},result['coverage']['baseline'])
        self.assertEqual({'available':2,'truncated':1,'unavailable':0,'fullyReviewed':1,'declaredHumanReviewed':0},result['coverage']['candidate'])
        self.assertEqual('NOT_ESTABLISHED',result['approval']);self.assertEqual('NOT_GRANTED',result['releaseAuthorization'])
        for private in ('private-case','private-authored','private-entity','perhaps on Monday','private baseline proposal','a'*32):self.assertNotIn(private,text)
        self.assertEqual('DECLARED_ADJUDICATION',result['rows'][0]['dimensions']['GROUNDING']['state'])
        self.assertEqual('INCOMPLETE_EVIDENCE_OR_REVIEW',result['rows'][3]['dimensions']['GROUNDING']['state'])

    def test_majority_does_not_silently_adjudicate(self):
        self.f.manifest['adjudications']=[]
        third=copy.deepcopy(self.f.reviews[0]);third['reviewerId']='d'*32
        self.f.manifest['reviews'].append(self.f.put('third.json',third))
        result=self.run_review();self.assertEqual(2,result['unresolvedDimensions']);self.assertEqual(0,result['adjudicatedDimensions'])

    def test_all_artifact_hashes_checked_before_any_review_summary(self):
        (self.root/'candidate.json').write_bytes(b'{"text":"tampered"}')
        with patch.object(q,'prepare_reviews') as review,self.assertRaises(ValueError):self.run_review()
        review.assert_not_called()

    def test_manifest_digest_and_duplicate_json_cannot_be_bypassed(self):
        with self.assertRaises(ValueError):q.evaluate(self.f.path,'0'*64)
        self.f.path.write_bytes(b'{"schemaVersion":1,"schemaVersion":1}')
        with self.assertRaises(ValueError):q.evaluate(self.f.path,q.sha(self.f.path.read_bytes()))

    def test_exact_source_projection_and_declared_split_leakage(self):
        original=copy.deepcopy(self.f.packet)
        for mutate in ('input','source','split','profile','language'):
            self.f.packet=copy.deepcopy(original)
            if mutate=='input':self.f.packet['cases'][0]['inputSha256']='f'*64
            if mutate=='source':self.f.packet['cases'][0]['source']=self.f.put('other.txt',b'A changed source.',True)
            if mutate=='split':self.f.packet['cases'][2]['split']='TRAIN'
            if mutate=='profile':self.f.packet['profiles']['candidate']['profileKey']='1'*64
            if mutate=='language':self.f.packet['language']='fr'
            self.packet()
            with self.subTest(mutate=mutate),self.assertRaises(ValueError):self.run_review()

    def test_changed_prompt_and_groups_cannot_hide_cross_split_source_reuse(self):
        case=self.f.packet['cases'][2];case['split']='TRAIN'
        for key in ('entityGroup','institutionGroup','timeGroup'):case[key]='different-'+key
        request=json.loads((self.root/case['input']['path']).read_bytes())
        request['prompt']='Summarize the reported item.'
        case['input']=self.f.put('changed-prompt.json',request)
        case['inputSha256']=case['input']['sha256'];self.packet()
        with self.assertRaisesRegex(ValueError,'Cross-split source leakage'):self.run_review()

    def test_grounded_attribute_review_binds_the_original_text_bytes(self):
        self.f.packet['task']='GROUNDED_ATTRIBUTES'
        for case in self.f.packet['cases']:
            case['input']=case['source'];case['inputSha256']=case['source']['sha256']
        self.packet()
        for index,review in enumerate(self.f.reviews):review['packetSha256']=self.f.manifest['packet']['sha256'];self.review(index)
        self.f.manifest['adjudications']=[]
        result=self.run_review();self.assertEqual('GROUNDED_ATTRIBUTES',result['task'])
        self.assertTrue(all(row['inputSha256']==q.sha(self.f.source) for row in result['rows']))
        self.f.packet['cases'][0]['input']=self.f.put('wrong-attribute-input.txt',b'Other source',True)
        self.f.packet['cases'][0]['inputSha256']=self.f.packet['cases'][0]['input']['sha256'];self.packet()
        with self.assertRaises(ValueError):self.run_review()

    def test_review_packet_identity_matrix_and_boolean_verdict_rejected(self):
        original=copy.deepcopy(self.f.reviews[1])
        for mutate in ('packet','reviewer','missing','duplicate','verdict','timestamp','authority'):
            self.f.reviews[1]=copy.deepcopy(original);record=self.f.reviews[1]
            if mutate=='packet':record['packetSha256']='f'*64
            if mutate=='reviewer':record['reviewerId']='a'*32
            if mutate=='missing':record['findings'].pop()
            if mutate=='duplicate':record['findings'][1]=copy.deepcopy(record['findings'][0])
            if mutate=='verdict':record['findings'][0]['verdicts']['GROUNDING']=True
            if mutate=='timestamp':record['at']='2026-11-17T00:00:00'
            if mutate=='authority':record['approval']='APPROVED'
            self.review(1)
            with self.subTest(mutate=mutate),self.assertRaises(ValueError):self.run_review()

    def test_missing_review_and_provider_absence_cannot_be_fabricated_as_failure(self):
        self.f.reviews[0]['findings'][2]={'caseId':'private-case-1','role':'baseline','state':'ASSESSED','verdicts':{k:'FAIL' for k in q.DIMENSIONS}}
        self.review(0)
        with self.assertRaises(ValueError):self.run_review()

    def test_stale_self_duplicate_unanimous_and_incomplete_adjudication_rejected(self):
        original=copy.deepcopy(self.f.adjudication)
        for mutate in ('stale','self','duplicate','unanimous','truncated','unreviewed'):
            self.f.adjudication=copy.deepcopy(original);record=self.f.adjudication
            if mutate=='stale':record['reviewSetSha256']='f'*64
            if mutate=='self':record['reviewerId']='a'*32
            if mutate=='duplicate':record['decisions']*=2
            if mutate=='unanimous':record['decisions'][0]['dimension']='PRIVACY'
            if mutate=='truncated':record['decisions'][0].update(caseId='private-case-1',role='candidate')
            if mutate=='unreviewed':record['decisions'][0].update(caseId='private-case-2',role='candidate')
            self.adjudication()
            with self.subTest(mutate=mutate),self.assertRaises(ValueError):self.run_review()

    def test_review_order_is_irrelevant_but_new_round_invalidates_old_adjudication(self):
        first=self.run_review();self.f.manifest['reviews'].reverse();second=self.run_review()
        self.assertEqual(first['reviewSetSha256'],second['reviewSetSha256'])
        self.f.manifest['reviews'].reverse();self.f.reviews[0]['at']='2026-11-17T01:00:00Z';self.review(0)
        with self.assertRaises(ValueError):self.run_review()

    def test_fixture_reviewers_cannot_masquerade_in_reference_scope(self):
        self.f.packet['scope']='REFERENCE_REVIEW';self.packet()
        for index,review in enumerate(self.f.reviews):review['packetSha256']=self.f.manifest['packet']['sha256'];self.review(index)
        with self.assertRaises(ValueError):self.run_review()

    def test_declared_human_coverage_does_not_certify_identity_or_grant_approval(self):
        for index,review in enumerate(self.f.reviews):review['reviewerKind']='HUMAN';self.review(index)
        self.f.manifest['adjudications']=[]
        result=self.run_review();self.assertEqual(2,result['coverage']['baseline']['declaredHumanReviewed'])
        self.assertEqual('DECLARATIONS_ONLY',result['authority']);self.assertEqual('NOT_ESTABLISHED',result['approval'])

    def test_basename_escape_and_aggregate_read_budget(self):
        reader=q.Reader(self.root)
        with self.assertRaises(ValueError):reader.read({'path':'../outside','sha256':'0'*64})
        reader.remaining=1
        with self.assertRaises(ValueError):reader.read(self.f.permission)

    def test_cli_verifies_release_before_reading_data_and_preserves_existing_output(self):
        target=self.root/'output.json';target.write_bytes(b'prior evidence')
        argv=['semantic_review.py','--manifest',str(self.f.path),'--manifest-sha256',self.f.save(),'--management-sha256','0'*64,'--output',str(target)]
        with patch.object(sys,'argv',argv),patch('managed_release.verify',side_effect=ValueError('fixture')),patch.object(q,'evaluate') as evaluate:
            self.assertEqual(1,q.main());evaluate.assert_not_called()
        with patch.object(sys,'argv',argv),patch('managed_release.verify'),patch.object(q,'evaluate') as evaluate:
            self.assertEqual(1,q.main());evaluate.assert_not_called()
        self.assertEqual(b'prior evidence',target.read_bytes())


if __name__=='__main__':unittest.main()
