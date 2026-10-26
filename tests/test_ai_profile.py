import copy
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('dzoka_ai_provider',ROOT/'ai/provider.py')
provider=importlib.util.module_from_spec(spec);spec.loader.exec_module(provider)

class AiProfileTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):(ROOT/'.test-tmp').mkdir(exist_ok=True)
    def test_committed_profile_binds_runtime_and_provider_sources(self):
        p=provider.load_profile();self.assertEqual(384,p['space']['dimensions']);self.assertEqual('L2',p['space']['normalization'])
    def test_modified_runtime_or_configuration_fails_before_native_import(self):
        for changed in ('runtime','configuration','space'):
            with self.subTest(changed=changed),tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as d:
                base=Path(d);(base/'ai').mkdir()
                for name in ['runtime.lock.json','model.lock.json','integrity.py']:(base/'ai'/name).write_bytes((ROOT/'ai'/name).read_bytes())
                if changed=='runtime':(base/'ai/runtime.lock.json').write_text('{}')
                else:
                    p=json.loads((base/'ai/model.lock.json').read_text(encoding='utf-8'))
                    if changed=='configuration':p['configuration']['maxTokens']=512
                    else:p['space']['artifactSha256']='0'*64
                    (base/'ai/model.lock.json').write_text(json.dumps(p),encoding='utf-8')
                before=provider.ROOT
                try:
                    provider.ROOT=base
                    with self.assertRaises(provider.ProviderUnavailable):provider.load_profile()
                finally:provider.ROOT=before
    def test_missing_or_corrupt_model_never_activates_a_substitute(self):
        with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as d:
            with self.assertRaises(provider.ProviderUnavailable):provider.TextEmbeddingProvider(d)
            (Path(d)/'model.onnx').write_bytes(b'synthetic corrupt model')
            with self.assertRaises(provider.ProviderUnavailable):provider.TextEmbeddingProvider(d)

if __name__=='__main__':unittest.main()
