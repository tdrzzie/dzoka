import importlib.util
import json
from pathlib import Path
import shutil
import unittest
import uuid

ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('dependency_lock',ROOT/'tools/dependency_lock.py')
lock=importlib.util.module_from_spec(spec)
spec.loader.exec_module(lock)

class DependencyLockTests(unittest.TestCase):
    def setUp(self):
        self.root=ROOT/'.test-tmp'/('dependency-lock-'+str(uuid.uuid4()))
        self.repo=self.root/'.audit-cache/m2'
        self.repo.mkdir(parents=True)
        (self.root/'pom.xml').write_text('synthetic declaration')
        (self.repo/'fixture.jar').write_bytes(b'synthetic artifact')
        (self.repo/'fixture.pom').write_bytes(b'synthetic license descriptor')
        self.data={'schemaVersion':1,'pomSha256':lock.sha(self.root/'pom.xml'),'dependencies':[
            {'coordinate':'synthetic:fixture:1','licenses':['MIT'],'selectedLicense':'MIT','jar':'fixture.jar',
             'jarSha256':lock.sha(self.repo/'fixture.jar'),'pom':'fixture.pom',
             'pomSha256':lock.sha(self.repo/'fixture.pom'),
             'licenseEvidence':{'path':'fixture.pom','sha256':lock.sha(self.repo/'fixture.pom')}}]}
        self.save()
    def save(self):
        (self.root/'dependencies.lock.json').write_text(json.dumps(self.data))
    def tearDown(self):
        # UUID fixture path verified inside this repository's test workspace.
        self.assertTrue(self.root.resolve().is_relative_to((ROOT/'.test-tmp').resolve()))
        shutil.rmtree(self.root)
    def test_altered_artifact_is_rejected(self):
        self.assertEqual(1,lock.check(self.root))
        (self.repo/'fixture.jar').write_bytes(b'changed bytes')
        with self.assertRaises(ValueError):lock.check(self.root)
    def test_changed_declaration_requires_review(self):
        (self.root/'pom.xml').write_text('new unreviewed dependency')
        with self.assertRaises(ValueError):lock.check(self.root)
    def test_artifact_path_cannot_escape_repository(self):
        outside=self.root/'outside.jar';outside.write_bytes(b'outside')
        self.data['dependencies'][0]['jar']='../../outside.jar'
        self.data['dependencies'][0]['jarSha256']=lock.sha(outside);self.save()
        with self.assertRaises(ValueError):lock.check(self.root)
    def test_license_evidence_and_duplicate_identity_are_checked(self):
        self.data['dependencies'][0]['licenseEvidence']['sha256']='0'*64;self.save()
        with self.assertRaises(ValueError):lock.check(self.root)
        self.data['dependencies'][0]['licenseEvidence']['sha256']=lock.sha(self.repo/'fixture.pom')
        self.data['dependencies'].append(self.data['dependencies'][0].copy());self.save()
        with self.assertRaises(ValueError):lock.check(self.root)

    def test_unreviewed_license_cannot_be_selected(self):
        with self.assertRaises(ValueError):lock.selected_license(['AGPL-3.0-only'])
        self.assertEqual('EPL-2.0',lock.selected_license(['EPL-2.0','LGPL-2.1-only']))

if __name__=='__main__':unittest.main()
