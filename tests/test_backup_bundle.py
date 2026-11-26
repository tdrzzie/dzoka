import json
from pathlib import Path
import tempfile
import unittest
from tools.backup_bundle import seal, verify, path_in

class BackupBundleTests(unittest.TestCase):
    def test_missing_classes_tampering_and_untrusted_manifest_fail(self):
        with tempfile.TemporaryDirectory() as folder:
            root=Path(folder)
            inventory=[{'name':k+'.fixture','kind':k} for k in ('postgres','objects','configuration','tombstones')]
            for e in inventory: (root/e['name']).write_bytes(b'SYNTHETIC BACKUP')
            with self.assertRaises(ValueError): seal(root,inventory[:3],'a'*64,'b'*64)
            sha=seal(root,inventory,'a'*64,'b'*64)
            self.assertEqual(4,verify(root,sha)['verifiedEntries'])
            with self.assertRaises(ValueError): verify(root,'0'*64)
            (root/'objects.fixture').write_bytes(b'tampered')
            with self.assertRaises(ValueError): verify(root,sha)
            with self.assertRaises(ValueError): path_in(root,'../outside')
            with self.assertRaises(ValueError): path_in(root,'C:/outside')
            with self.assertRaises(FileExistsError): seal(root,inventory,'a'*64,'b'*64)
