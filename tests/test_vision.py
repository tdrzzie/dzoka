import json
from pathlib import Path
import shutil
import sys
import tempfile
import unittest
from unittest.mock import patch

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'ai'))
import vision

class VisionTests(unittest.TestCase):
    def test_pair_shares_exact_space_but_profiles_remain_distinct(self):
        profiles=vision.profiles()
        self.assertEqual(profiles,json.loads((ROOT/'ai/vision-profiles.json').read_bytes()))
        self.assertEqual(4,len({p['key'] for p in profiles.values()}))
        self.assertEqual(profiles['JOINT_IMAGE']['manifest']['sharedSpace'],profiles['JOINT_TEXT']['manifest']['sharedSpace'])
        shared=profiles['JOINT_IMAGE']['manifest']['sharedSpace']
        self.assertEqual(shared['key'],vision.sha(vision.canonical(shared['definition'])))
        self.assertEqual(768,shared['definition']['configuration']['dimensions'])

    def test_runtime_or_source_change_cannot_reuse_published_identity(self):
        with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as directory:
            root=Path(directory);(root/'ai').mkdir()
            lock=json.loads((ROOT/'ai/vision.lock.json').read_bytes())
            for name in ['ai/vision.lock.json','ai/runtime.lock.json',*lock['sources']]:shutil.copy2(ROOT/name,root/name)
            with patch.object(vision,'ROOT',root):self.assertEqual(vision.profiles(),json.loads((ROOT/'ai/vision-profiles.json').read_bytes()))
            with (root/'ai/vision.py').open('ab') as output:output.write(b'\n# changed\n')
            with patch.object(vision,'ROOT',root),self.assertRaisesRegex(ValueError,'implementation changed'):vision.profiles()

    def test_corrupt_model_fails_before_native_imports(self):
        with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as directory:
            (Path(directory)/'detector.onnx').write_bytes(b'invalid model')
            with patch.dict(sys.modules,{'onnxruntime':None}),self.assertRaisesRegex(ValueError,'integrity'):
                vision.VisionProvider('DETECTION',directory)

if __name__=='__main__':unittest.main()
