import hashlib
import json
from pathlib import Path
import sys
import tempfile
import unittest
import uuid
from unittest.mock import patch
from types import SimpleNamespace
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
from service_account import validate
from journal_archive import archive,verify,receipt
from docker_lifecycle import OwnerLock,persist,image

class BoundaryTests(unittest.TestCase):
    def test_expected_identity_rejects_elevated_admin_wrong_user_and_shared_accounts(self):
        value={'sid':'S-1-5-21-1-2-3-1001','elevated':False,'administrator':False,'integrity':0x2000}
        validate(value,value['sid'])
        validate({**value,'integrity':0x4000},value['sid'],0x4000)
        with self.assertRaises(ValueError):validate(value,value['sid'],True)
        for key,item in [('sid','S-1-5-21-1-2-3-1002'),('elevated',True),('administrator',True),('integrity',0x3000)]:
            with self.assertRaises(ValueError):validate({**value,key:item},value['sid'])
        for expected in ('S-1-5-18','S-1-5-19','S-1-5-20','invalid'):
            with self.assertRaises(ValueError):validate({**value,'sid':expected},expected)

    def test_unconfirmed_profile_intent_remains_unresolved_even_after_absence_cleanup(self):
        from appcontainer_boundary import PREFIX,recover,state_write
        with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as directory:
            root=Path(directory);owner=uuid.uuid4().hex;path=root/('offline-'+owner+'.json')
            value={'schemaVersion':1,'accountSid':'S-1-5-21-1-2-3-1001','owner':owner,'name':PREFIX+owner,'state':'INTENT'};state_write(path,value)
            with patch('appcontainer_boundary.apis',return_value=(None,SimpleNamespace(DeleteAppContainerProfile=lambda _:0),None)),patch('appcontainer_boundary.identity',return_value={'sid':value['accountSid']}):
                self.assertEqual({'removed':0,'active':0,'unresolved':1},recover(root))
            with patch('appcontainer_boundary.apis',return_value=(None,None,None)),patch('appcontainer_boundary.identity',return_value={'sid':'S-1-5-21-1-2-3-1002'}):
                with self.assertRaises(ValueError):recover(root)
            self.assertEqual(value,json.loads(path.read_bytes()))

    def record(self,root,state='REMOVED'):
        owner=uuid.uuid4().hex;resource=uuid.uuid4().hex
        value={'schemaVersion':1,'owner':owner,'resource':resource,'name':'dzoka-owned-'+owner+'-'+resource,'image':image(),'history':[]}
        value=persist(root,value,'INTENT')
        return persist(root,value,state)

    def test_archive_preserves_exact_bytes_and_idempotency_and_retains_uncertainty(self):
        with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as directory:
            root=Path(directory);record=self.record(root);pending=self.record(root,'CREATE_UNKNOWN');path=root/(record['name']+'.json');raw=path.read_bytes()
            self.assertEqual({'archived':1,'active':0,'retained':1},archive(root));self.assertFalse(path.exists());self.assertTrue((root/(pending['name']+'.json')).exists())
            self.assertEqual(raw,(root/'archive'/path.name).read_bytes());self.assertEqual(1,verify(root/'archive'));self.assertEqual(0,archive(root)['archived'])

    def test_crash_after_receipt_keeps_both_and_retry_verifies_before_acknowledgement(self):
        with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as directory:
            root=Path(directory);record=self.record(root);path=root/(record['name']+'.json');raw=path.read_bytes()
            def crash():raise OSError('simulated crash')
            with self.assertRaises(OSError):archive(root,crash)
            self.assertEqual(raw,path.read_bytes());self.assertEqual(1,verify(root/'archive'));self.assertEqual(1,archive(root)['archived']);self.assertEqual(1,verify(root/'archive'))

    def test_conflicting_archive_and_corrupt_receipt_do_not_erase_source(self):
        with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as directory:
            root=Path(directory);record=self.record(root);path=root/(record['name']+'.json');target=root/'archive';target.mkdir();(target/path.name).write_bytes(b'conflicting')
            with self.assertRaises(ValueError):archive(root)
            self.assertTrue(path.exists());(target/path.name).write_bytes(path.read_bytes());(target/(record['name']+'.receipt.json')).write_bytes(b'wrong')
            with self.assertRaises(ValueError):archive(root)
            with self.assertRaises(ValueError):verify(target)
            self.assertTrue(path.exists())

    def test_active_owner_is_skipped_and_concurrent_archiver_is_rejected(self):
        from journal_archive import ARCHIVER
        with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as directory:
            root=Path(directory);record=self.record(root);lock=OwnerLock(root,record['owner']);self.assertTrue(lock.acquire())
            try:self.assertEqual(1,archive(root)['active'])
            finally:lock.close()
            lock=OwnerLock(root,ARCHIVER);self.assertTrue(lock.acquire())
            try:
                with self.assertRaises(OSError):archive(root)
            finally:lock.close()

    def test_nonterminal_and_duplicate_or_retargeted_records_cannot_enter_archive(self):
        with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as directory:
            root=Path(directory);record=self.record(root,'CREATED');path=root/(record['name']+'.json');raw=path.read_bytes();self.assertEqual(1,archive(root)['retained'])
            path.write_bytes(raw.replace(b'"schemaVersion": 1',b'"schemaVersion": 1, "schemaVersion": 1'))
            with self.assertRaises(ValueError):archive(root)
            path.write_bytes(raw);path.rename(root/'dzoka-owned-forged.json')
            with self.assertRaises(ValueError):archive(root)

if __name__=='__main__':unittest.main()
