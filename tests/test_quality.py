import hashlib
import json
from pathlib import Path
import shutil
import sys
import tempfile
import unittest
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'ai'))
import quality

class QualityTests(unittest.TestCase):
    def test_registry_adds_only_explicit_separate_profiles(self):
        p=quality.profiles();self.assertEqual(p,json.loads((ROOT/'ai/quality-profiles.json').read_bytes()));self.assertEqual({'SMOL360_EN','OPUS_SN_EN','OPUS_ES_EN','OPUS_DE_EN'},set(p));self.assertEqual(4,len({x['key'] for x in p.values()}))
        c=p['OPUS_SN_EN']['manifest']['configuration'];self.assertEqual(('sn','en','UNVERIFIED_TRANSLATION'),(c['sourceLanguage'],c['targetLanguage'],c['trust']))
        old=json.loads((ROOT/'ai/generative-profiles.json').read_bytes());self.assertTrue(set(x['key'] for x in p.values()).isdisjoint(x['key'] for x in old.values()))
    def test_repetition_policy_penalizes_both_logit_signs_without_mutating_raw_scores(self):
        raw=[4.0,3.0,-1.0];adjusted=quality.draft_scores(raw,[0,2],2);self.assertEqual([4.0,3.0,-1.0],raw);self.assertEqual([2.0,3.0,-2.0],adjusted);self.assertEqual(1,max(range(3),key=adjusted.__getitem__))
        adjusted=quality.draft_scores([-1.0,-1.5],[0],2);self.assertEqual(1,max(range(2),key=adjusted.__getitem__))
    def test_generated_bigram_repetition_is_blocked_without_masking_other_continuations(self):
        adjusted=quality.draft_scores([0.0,1.0,2.0,3.0],[1,3,1],1.1);self.assertEqual(-float('inf'),adjusted[3]);self.assertEqual(2,max(range(4),key=adjusted.__getitem__));self.assertEqual([0,1],quality.draft_scores([0,1],[],1.1))
    def test_corrupt_graph_fails_before_native_import(self):
        (ROOT/'.test-tmp').mkdir(exist_ok=True)
        with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as d:
            (Path(d)/'smol360_en-llm.onnx').write_bytes(b'changed')
            with patch.dict(sys.modules,{'onnxruntime':None}),self.assertRaisesRegex(ValueError,'artifact integrity'):quality.QualityProvider('SMOL360_EN',d)
    def test_implementation_and_runtime_changes_cannot_reuse_identity(self):
        with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as d:
            root=Path(d);(root/'ai').mkdir();lock=json.loads((ROOT/'ai/quality.lock.json').read_bytes())
            for name in ['ai/quality.lock.json','ai/generative-runtime.lock.json','ai/runtime.lock.json',*lock['sources']]:shutil.copy2(ROOT/name,root/name)
            with (root/'ai/quality.py').open('ab') as f:f.write(b'changed')
            with patch.object(quality,'ROOT',root),self.assertRaisesRegex(ValueError,'implementation changed'):quality.profiles()
            shutil.copy2(ROOT/'ai/quality.py',root/'ai/quality.py');(root/'ai/runtime.lock.json').write_bytes(b'changed')
            with patch.object(quality,'ROOT',root),self.assertRaisesRegex(ValueError,'Base runtime identity'):quality.profiles()
    def test_swapped_source_target_tokenizer_roles_change_profile_identity(self):
        with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as d:
            root=Path(d);(root/'ai').mkdir();lock=json.loads((ROOT/'ai/quality.lock.json').read_bytes())
            for name in ['ai/quality.lock.json','ai/generative-runtime.lock.json','ai/runtime.lock.json',*lock['sources']]:shutil.copy2(ROOT/name,root/name)
            original=quality.profiles()['OPUS_SN_EN']['key']
            for entry in lock['files']:
                if entry['group']=='OPUS_SN_EN' and entry['role'] in ('source.spm','target.spm'):entry['role']='target.spm' if entry['role']=='source.spm' else 'source.spm'
            (root/'ai/quality.lock.json').write_bytes(json.dumps(lock).encode())
            with patch.object(quality,'ROOT',root):self.assertNotEqual(original,quality.profiles()['OPUS_SN_EN']['key'])
    def test_evaluation_scope_and_fixture_identity_are_explicit(self):
        lock=json.loads((ROOT/'ai/quality.lock.json').read_bytes());data=(ROOT/'ai/quality-evaluation.json').read_bytes();self.assertEqual(hashlib.sha256(data).hexdigest(),lock['evaluationSha256']);fixture=json.loads(data)
        self.assertIn('synthetic',fixture['scope']);self.assertEqual(9,len(fixture['cases']));self.assertEqual(9,len({c['id'] for c in fixture['cases']}));self.assertTrue(all(c['profile'] in quality.profiles() for c in fixture['cases']))
        self.assertEqual(fixture,quality.evaluation())
        with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as d:
            root=Path(d);(root/'ai').mkdir();shutil.copy2(ROOT/'ai/quality.lock.json',root/'ai/quality.lock.json');(root/'ai/quality-evaluation.json').write_bytes(data+b'changed')
            with patch.object(quality,'ROOT',root),self.assertRaisesRegex(ValueError,'Evaluation fixture identity'):quality.evaluation()
        self.assertTrue(all(c.get('baseline') is None for c in fixture['cases'] if c['profile']=='OPUS_SN_EN'));self.assertNotEqual(hashlib.sha256(data+b'changed').hexdigest(),lock['evaluationSha256'])

if __name__=='__main__':unittest.main()
