import importlib.util
from pathlib import Path
import re
import shutil
import tempfile
import unittest

ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('m1_architecture',ROOT/'tools/verify_architecture.py')
module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)

class ArchitectureTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        (ROOT/'.test-tmp').mkdir(exist_ok=True)
        cls.temp=tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp');cls.fixture=Path(cls.temp.name).resolve()
        assert cls.fixture.is_relative_to((ROOT/'.test-tmp').resolve())
        for folder in ['docs','src','tools']:
            shutil.copytree(ROOT/folder,cls.fixture/folder,ignore=shutil.ignore_patterns('__pycache__'))
        for path in ROOT.glob('*.md'):shutil.copy2(path,cls.fixture/path.name)
        shutil.copy2(ROOT/'.env.example',cls.fixture/'.env.example')
        shutil.copy2(ROOT/'dependencies.lock.json',cls.fixture/'dependencies.lock.json')
    @classmethod
    def tearDownClass(cls):
        assert cls.fixture.is_relative_to((ROOT/'.test-tmp').resolve());cls.temp.cleanup()
    def test_existing_baseline_matches_original_m1_scope(self):
        errors,stats=module.validate(self.fixture);self.assertEqual([],errors)
        self.assertEqual(24,stats['requiredViews']);self.assertEqual(24,stats['baselineAdrs'])
        self.assertEqual(29,stats['acceptanceScenarios']);self.assertEqual(13,stats['preservedBodies'])
    def test_missing_required_view_is_not_complete(self):
        path=self.fixture/'docs/architecture/matching-architecture.md';original=path.read_bytes();path.unlink()
        try:self.assertTrue(any('matching-architecture.md' in e for e in module.validate(self.fixture)[0]))
        finally:path.write_bytes(original)
    def test_unmatched_source_pin_is_not_approved_evidence(self):
        path=self.fixture/'docs/adr/0003-pgvector-default.md';original=path.read_bytes();text=original.decode()
        pin=re.search(r'`([0-9a-f]{40})`',text).group(1);path.write_text(text.replace(pin,'0'*40,1),encoding='utf-8',newline='\n')
        try:self.assertTrue(any('citation pin differs' in e for e in module.validate(self.fixture)[0]))
        finally:path.write_bytes(original)
    def test_supplied_document_body_cannot_be_replaced(self):
        path=self.fixture/'docs/ARCHITECTURE.md';original=path.read_bytes();path.write_bytes(original+b'\nSynthetic body mutation')
        try:self.assertTrue(any('Supplied body changed' in e for e in module.validate(self.fixture)[0]))
        finally:path.write_bytes(original)
    def test_missing_acceptance_scenario_fails(self):
        path=self.fixture/'docs/architecture/acceptance.md';original=path.read_bytes();path.write_bytes(original.replace(b'| A29 |',b'| A99 |',1))
        try:self.assertTrue(any('A01-A29' in e for e in module.validate(self.fixture)[0]))
        finally:path.write_bytes(original)

if __name__=='__main__':unittest.main()
