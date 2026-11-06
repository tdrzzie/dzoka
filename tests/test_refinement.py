import hashlib
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'ai'))
import refinement

class RefinementTests(unittest.TestCase):
    def test_historical_generation_sources_remain_exact_and_new_keys_are_explicit(self):
        history=ROOT/'ai/history/broker-v1-20261106';lock=json.loads((history/'broker.lock.json').read_bytes())
        for name,digest in lock['sources'].items():self.assertEqual(digest,hashlib.sha256((history/name).read_bytes()).hexdigest())
        import broker_profiles
        old=json.loads((history/'broker-profiles.json').read_bytes());current=broker_profiles.profiles()
        self.assertEqual(34,len(old));self.assertTrue(set(old)<=set(current))
        for name in old:
            self.assertNotEqual(old[name]['key'],current[name]['key'])
            if old[name]['family']!='text':self.assertEqual(old[name]['manifest']['configuration']['baseProfileKey'],current[name]['manifest']['configuration']['baseProfileKey'])

    def test_new_representations_and_budgets_are_not_legacy_aliases(self):
        profiles=refinement.profiles();self.assertEqual(profiles,json.loads((ROOT/'ai/refinement-profiles.json').read_bytes()))
        for value in profiles.values():
            cfg=value['manifest']['configuration']
            if value['manifest']['task']=='MULTILINGUAL_TRANSLATION':
                self.assertEqual((256,128,True),(cfg['maxInputTokens'],cfg['maxOutputTokens'],cfg['suppressUnknownToken']))
                self.assertTrue(cfg['representation'].endswith('-budget-v2'));self.assertIn('delegateProfileKey',cfg)
            else:self.assertTrue(cfg['outputRepresentation'].endswith('-v2'))

    def test_class_mapping_and_rle_are_explicit_not_ordinal_relabeling(self):
        self.assertEqual(80,len(set(refinement.COCO_IDS)));self.assertEqual(44,refinement.COCO_IDS[39]);self.assertEqual(5,refinement.VOC[44])
        self.assertEqual([[0,2],[5,2],[0,1]],refinement.rle([0,0,5,5,0]));self.assertNotIn(27,refinement.VOC)

    def test_corrupt_model_is_rejected_before_runtime_import_or_inference(self):
        with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as folder:
            (Path(folder)/'maskrcnn.onnx').write_bytes(b'bad')
            with patch.dict(sys.modules,{'onnxruntime':None}),self.assertRaisesRegex(ValueError,'artifact changed'):
                refinement.RefinementProvider('MASK_DETECTION',folder)

if __name__=='__main__':unittest.main()
