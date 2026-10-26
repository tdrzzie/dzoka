import importlib.util
import json
from pathlib import Path
import shutil
import struct
import sys
import tempfile
import unittest
from unittest.mock import patch
import wave
import io
import zlib

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'ai'))
import perception as provider
from perception_formats import png_rgba,pcm16

def chunk(kind,data):return struct.pack('>I',len(data))+kind+data+struct.pack('>I',zlib.crc32(kind+data)&0xffffffff)
def png(raw,w=1,h=1,color=6):return b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',w,h,8,color,0,0,0))+chunk(b'IDAT',zlib.compress(raw))+chunk(b'IEND',b'')

class PerceptionTests(unittest.TestCase):
    def test_profile_registry_matches_verified_source_and_exact_artifact_configuration(self):
        self.assertEqual(json.loads((ROOT/'ai/perception-profiles.json').read_bytes()),provider.profiles())
        profiles=provider.profiles();self.assertEqual(4,len({p['key'] for p in profiles.values()}))
        self.assertEqual([],profiles['LOCAL_FEATURES']['manifest']['artifacts'])

    def test_source_mutation_cannot_reuse_an_approved_profile_key(self):
        (ROOT/'.test-tmp').mkdir(exist_ok=True)
        with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as folder:
            root=Path(folder);(root/'ai').mkdir()
            lock=json.loads((ROOT/'ai/perception.lock.json').read_bytes())
            for name in ['ai/perception.lock.json','ai/runtime.lock.json',*lock['sources']]:shutil.copy2(ROOT/name,root/name)
            with patch.object(provider,'ROOT',root):self.assertEqual(provider.profiles(),json.loads((ROOT/'ai/perception-profiles.json').read_bytes()))
            with (root/'ai/perception_formats.py').open('ab') as target:target.write(b'\n# unapproved mutation\n')
            with patch.object(provider,'ROOT',root),self.assertRaisesRegex(ValueError,'implementation changed'):provider.profiles()

    def test_corrupt_artifact_is_rejected_before_native_import(self):
        with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as folder:
            (Path(folder)/'vision.onnx').write_bytes(b'unapproved model')
            with patch.dict(sys.modules,{'onnxruntime':None}),self.assertRaisesRegex(ValueError,'integrity'):
                provider.PerceptionProvider('VISION',folder)

    def test_narrow_png_unfilters_previous_row_and_rejects_crc_and_trailing_bytes(self):
        first=bytes([10,20,30,255]);second=bytes([5,6,7,0])
        encoded=png(b'\0'+first+b'\2'+bytes((b-a)&255 for a,b in zip(first,second)),1,2)
        self.assertEqual((1,2,first+second),png_rgba(encoded))
        altered=bytearray(encoded);altered[-1]^=1
        for raw in (bytes(altered),encoded+b'trailing',encoded[:-1],png(b'\0\0\0\0',color=2)):
            with self.assertRaises(ValueError):png_rgba(raw)

    def test_png_decompression_and_dimensions_remain_bounded(self):
        for raw in (png(b'\0'+b'\0'*10000),png(b'\0\0\0\0\0',w=8192,h=8192),png(b'\5\0\0\0\0')):
            with self.assertRaises(ValueError):png_rgba(raw)

    def test_pcm_requires_exact_rate_channels_size_and_no_extra_chunks(self):
        output=io.BytesIO()
        with wave.open(output,'wb') as target:
            target.setnchannels(1);target.setsampwidth(2);target.setframerate(16000);target.writeframes(b'\0\0'*800)
        data=output.getvalue();self.assertEqual(b'\0\0'*800,pcm16(data))
        wrong=bytearray(data);struct.pack_into('<I',wrong,24,8000)
        for raw in (bytes(wrong),data+b'metadata',data[:-1],b'not a wav'):
            with self.assertRaises(ValueError):pcm16(raw)

    def test_private_worker_refuses_unapproved_network_endpoints_before_request(self):
        spec=importlib.util.spec_from_file_location('perception_client',ROOT/'ai/perception_worker.py');client=importlib.util.module_from_spec(spec);spec.loader.exec_module(client)
        with patch.object(client,'request',side_effect=AssertionError('must not send token')):
            for base in ('http://external.invalid','https://user:secret@example.invalid','https://example.invalid/path','https://example.invalid?token=bad'):
                with self.assertRaises(ValueError):client.run_once(base,'synthetic-token','invalid-tenant','invalid-profile','unused')

if __name__=='__main__':unittest.main()
