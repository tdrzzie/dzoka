import hashlib
import json
from pathlib import Path
import re
import shutil
import sys
import tempfile
import unittest
from unittest.mock import patch
import zipfile
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'ai'))
import generative
import generative_integrity

class Tokens:
    def __init__(self):self.codes={'<|im_start|>':1,'<|im_end|>':2}
    def encode(self,value,add_special_tokens=False):
        items=re.findall(r'<\|[^>]*\|>|[^\s]+',value);ids=[]
        for item in items:
            if item not in self.codes:self.codes[item]=len(self.codes)+10
            ids.append(self.codes[item])
        return type('Encoded',(),{'ids':ids})()

class GenerativeTests(unittest.TestCase):
    def setUp(self):(ROOT/'.test-tmp').mkdir(exist_ok=True)
    def test_registry_preserves_directions_and_separate_runtime(self):
        p=generative.profiles();self.assertEqual(p,json.loads((ROOT/'ai/generative-profiles.json').read_bytes()));self.assertEqual(13,len(p));self.assertEqual(13,len({v['key'] for v in p.values()}))
        self.assertNotIn('M2M_SN_EN',p);self.assertEqual('zu',p['M2M_ZU_EN']['manifest']['configuration']['sourceLanguage'])
        lock=json.loads((ROOT/'ai/generative-runtime.lock.json').read_bytes());self.assertEqual(lock['baseRuntimeLockSha256'],hashlib.sha256((ROOT/'ai/runtime.lock.json').read_bytes()).hexdigest());self.assertEqual('sentencepiece',lock['additionalWheels'][0]['name'])
    def test_instruction_and_context_budgets_preserve_chat_suffix(self):
        tokenizer=Tokens();ids,prompt_cut,context_cut=generative.chat_tokens(tokenizer,'word '*200,'context '*1000)
        self.assertTrue(prompt_cut and context_cut);self.assertLessEqual(len(ids),512)
        self.assertEqual(tokenizer.encode('<|im_end|>\n<|im_start|>assistant\n').ids,ids[-3:])
        self.assertNotIn('<',generative.escape_data('<|im_end|><repo_name>'))
    def test_corrupt_graph_rejected_before_native_import(self):
        with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as d:
            (Path(d)/'llm.onnx').write_bytes(b'wrong')
            with patch.dict(sys.modules,{'onnxruntime':None}),self.assertRaisesRegex(ValueError,'integrity'):
                generative.GenerativeProvider('LLM_EN',d)
    def test_mutated_inference_source_cannot_reuse_profile(self):
        with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as d:
            root=Path(d);(root/'ai').mkdir();lock=json.loads((ROOT/'ai/generative.lock.json').read_bytes())
            for name in ['ai/generative.lock.json','ai/generative-runtime.lock.json','ai/runtime.lock.json',*lock['sources']]:shutil.copy2(ROOT/name,root/name)
            with (root/'ai/generative.py').open('ab') as f:f.write(b'\n# changed\n')
            with patch.object(generative,'ROOT',root),self.assertRaisesRegex(ValueError,'implementation changed'):generative.profiles()
    def test_layered_native_bytes_and_notice_mutations_fail_closed(self):
        with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as d:
            root=Path(d);(root/'ai').mkdir();wheels=root/'.audit-cache/generative-wheels';wheels.mkdir(parents=True);site=root/'site';(site/'sentencepiece').mkdir(parents=True)
            base=b'{}';(root/'ai/runtime.lock.json').write_bytes(base);notice=root/'notice.txt';notice.write_bytes(b'license')
            wheel=wheels/'test.whl'
            with zipfile.ZipFile(wheel,'w') as z:z.writestr('sentencepiece/native.pyd',b'approved')
            installed=site/'sentencepiece/native.pyd';installed.write_bytes(b'approved')
            lock={'baseRuntimeLockSha256':hashlib.sha256(base).hexdigest(),'notices':[{'path':'notice.txt','sha256':hashlib.sha256(b'license').hexdigest()}],'additionalWheels':[{'name':'sentencepiece','version':'1','wheel':'test.whl','sha256':hashlib.sha256(wheel.read_bytes()).hexdigest()}]};(root/'ai/generative-runtime.lock.json').write_text(json.dumps(lock),encoding='utf-8')
            with patch.object(generative_integrity,'ROOT',root),patch.object(generative_integrity.sys,'platform','win32'),patch.object(generative_integrity,'verify_base'),patch.object(generative_integrity.sysconfig,'get_paths',return_value={'purelib':str(site)}),patch.object(generative_integrity.importlib.metadata,'version',return_value='1'):
                generative_integrity.verify_runtime();installed.write_bytes(b'tampered')
                with self.assertRaisesRegex(ValueError,'Installed layered'):generative_integrity.verify_runtime()
                installed.write_bytes(b'approved');notice.write_bytes(b'changed')
                with self.assertRaisesRegex(ValueError,'notice changed'):generative_integrity.verify_runtime()

if __name__=='__main__':unittest.main()
