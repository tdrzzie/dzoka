import json
from pathlib import Path
import sys
import tempfile
import unittest
import uuid
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
import profile_archive as archive
from appcontainer_boundary import PREFIX
from docker_lifecycle import OwnerLock

ACCOUNT='S-1-5-21-1-2-3-1001'

class ProfileArchiveTests(unittest.TestCase):
    def setUp(self):
        self.tmp=tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp');self.root=Path(self.tmp.name)
        self.patches=[patch.object(archive,'protected',side_effect=Path),patch.object(archive,'plain',side_effect=Path),patch.object(archive,'protect'),patch.object(archive,'audit_records')]
        for p in self.patches:p.start()
    def tearDown(self):
        for p in self.patches:p.stop()
        self.tmp.cleanup()
    def record(self,state='REMOVED',owner=None):
        owner=owner or uuid.uuid4().hex
        value={'schemaVersion':1,'accountSid':ACCOUNT,'owner':owner,'name':PREFIX+owner,'state':state}
        path=self.root/('offline-'+owner+'.json');path.write_bytes((json.dumps(value,indent=2)+'\n').encode());return path,value

    def test_exact_archive_retry_after_crash_and_capacity_reclaimed(self):
        path,_=self.record();raw=path.read_bytes()
        with self.assertRaisesRegex(RuntimeError,'crash'):archive.archive(self.root,ACCOUNT,0,after_receipt=lambda:(_ for _ in ()).throw(RuntimeError('crash')))
        self.assertEqual(raw,path.read_bytes());self.assertEqual(raw,(self.root/'archive'/path.name).read_bytes())
        self.assertEqual(1,archive.archive(self.root,ACCOUNT,0)['archived']);self.assertFalse(path.exists())
        self.assertEqual(1,archive.verify(self.root/'archive',ACCOUNT));self.assertEqual(0,archive.archive(self.root,ACCOUNT,0)['archived'])

    def test_over_historical_capacity_terminal_records_are_preserved_and_reclaimed(self):
        for _ in range(1001):self.record()
        result=archive.archive(self.root,ACCOUNT,keep=3,batch=2000)
        self.assertEqual(998,result['archived']);self.assertEqual(3,len(list(self.root.glob('offline-*.json'))));self.assertEqual(998,archive.verify(self.root/'archive',ACCOUNT))

    def test_active_unresolved_and_grant_bearing_records_cannot_be_discarded(self):
        terminal,value=self.record();lock=OwnerLock(self.root,value['owner']);self.assertTrue(lock.acquire())
        try:
            for state in ('INTENT','CREATED'):self.record(state)
            grant,value=self.record();(self.root/('grant-'+value['owner']+'.json')).write_bytes(b'{}')
            result=archive.archive(self.root,ACCOUNT,0)
            self.assertEqual({'archived':0,'active':1,'retained':3},result);self.assertTrue(terminal.exists());self.assertTrue(grant.exists())
        finally:lock.close()

    def test_foreign_malformed_duplicate_and_retargeted_records_fail_before_acknowledgement(self):
        for kind in ('foreign','name','duplicate','version'):
            with self.subTest(kind=kind):
                path,value=self.record()
                if kind=='foreign':value['accountSid']='S-1-5-21-4-5-6-1002'
                if kind=='name':value['name']=PREFIX+uuid.uuid4().hex
                if kind=='version':value['schemaVersion']=True
                raw=json.dumps(value).encode()
                if kind=='duplicate':raw=raw[:-1]+b',"state":"REMOVED"}'
                path.write_bytes(raw)
                with self.assertRaises(ValueError):archive.archive(self.root,ACCOUNT,0)
                self.assertTrue(path.exists());path.unlink()

    def test_corrupt_archive_or_receipt_blocks_new_archival(self):
        path,_=self.record();archive.archive(self.root,ACCOUNT,0)
        proof=self.root/'archive'/(path.stem+'.receipt.json');proof.write_bytes(b'{}')
        another,_=self.record()
        with self.assertRaises(ValueError):archive.archive(self.root,ACCOUNT,0)
        with self.assertRaises(ValueError):archive.verify(self.root/'archive',ACCOUNT)
        self.assertTrue(another.exists())

    def test_incomplete_copy_can_retry_only_against_identical_live_record(self):
        path,_=self.record();target=self.root/'archive';target.mkdir();(target/path.name).write_bytes(path.read_bytes())
        self.assertEqual(1,archive.archive(self.root,ACCOUNT,0)['archived'])
        orphan,_=self.record();(target/orphan.name).write_bytes(orphan.read_bytes());orphan.unlink()
        with self.assertRaises(ValueError):archive.archive(self.root,ACCOUNT,0)

    def test_exclusive_archiver_and_conflicting_copy_retain_live_bytes(self):
        path,_=self.record();guard=OwnerLock(self.root,archive.ARCHIVER);self.assertTrue(guard.acquire())
        try:
            with self.assertRaises(OSError):archive.archive(self.root,ACCOUNT,0)
        finally:guard.close()
        target=self.root/'archive';target.mkdir();(target/path.name).write_bytes(b'wrong')
        with self.assertRaises(ValueError):archive.archive(self.root,ACCOUNT,0)
        self.assertTrue(path.exists())

    def test_privacy_audit_failure_prevents_any_archive_write(self):
        path,_=self.record()
        with patch.object(archive,'audit_records',side_effect=ValueError('Broad read access')):
            with self.assertRaises(ValueError):archive.archive(self.root,ACCOUNT,0)
        self.assertTrue(path.exists());self.assertFalse((self.root/'archive').exists())
