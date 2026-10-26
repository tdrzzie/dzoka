import hashlib
import importlib.util
import io
from pathlib import Path
import unittest
from unittest.mock import patch
import urllib.error

ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('source_bytes',ROOT/'tools/verify_source_evidence.py')
module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)

class SourceEvidenceTests(unittest.TestCase):
    source={'id':'synthetic','kind':'repository','repository':'https://github.com/example/project','revision':'a'*40}
    def manifest(self,data,line_count):
        return {'path':'source.py','sha256':hashlib.sha256(data).hexdigest(),
            'blobSha':hashlib.sha1(b'blob '+str(len(data)).encode()+b'\0'+data).hexdigest(),'lineCount':line_count}
    def test_original_bytes_and_git_blob_match(self):
        data='Synthetic café\nsecond line\n'.encode()
        with patch.object(module.urllib.request,'urlopen',return_value=io.BytesIO(data)):
            self.assertEqual('PASS',module.verify(self.source,self.manifest(data,2))['status'])
    def test_utf16_is_hashed_before_decoding(self):
        data='Synthetic UTF-16\nsecond line\n'.encode('utf-16')
        with patch.object(module.urllib.request,'urlopen',return_value=io.BytesIO(data)):
            result=module.verify(self.source,self.manifest(data,2));self.assertEqual('PASS',result['status']);self.assertTrue(result['blobMatch'])
    def test_changed_bytes_or_line_count_never_pass(self):
        expected=self.manifest(b'original\n',1)
        with patch.object(module.urllib.request,'urlopen',return_value=io.BytesIO(b'changed\n')):
            self.assertEqual('MISMATCH',module.verify(self.source,expected)['status'])
        with patch.object(module.urllib.request,'urlopen',return_value=io.BytesIO(b'original\n')):
            expected['lineCount']=2;self.assertEqual('MISMATCH',module.verify(self.source,expected)['status'])
    def test_network_failure_is_unverified_not_complete(self):
        with patch.object(module.urllib.request,'urlopen',side_effect=urllib.error.URLError('synthetic network failure')):
            result=module.verify(self.source,self.manifest(b'original',1));self.assertEqual('UNVERIFIED',result['status']);self.assertEqual('URLError',result['errorType'])

if __name__=='__main__':unittest.main()
