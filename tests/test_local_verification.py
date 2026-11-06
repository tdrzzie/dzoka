import importlib.util
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import hashlib

ROOT=Path(__file__).resolve().parents[1]

def load(name):
    spec=importlib.util.spec_from_file_location(name,ROOT/'tools'/f'{name}.py')
    module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module);return module

restore=load('restore_drill');local=load('verify_local')


class LocalVerificationTests(unittest.TestCase):
    def test_explicit_native_s3_requires_exact_bytes_and_keeps_full_matrix(self):
        baseline=local.integration_command()
        self.assertIn('--with-docker-shona',baseline);self.assertIn('--restore-drill',baseline)
        with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as folder:
            executable=Path(folder)/'weed.exe';executable.write_bytes(b'authored executable fixture, never executed')
            with patch.object(local,'NATIVE_S3_SHA256',hashlib.sha256(executable.read_bytes()).hexdigest()):
                command=local.integration_command('pg','maven',executable)
                self.assertEqual(baseline,command[:len(baseline)])
                self.assertEqual(['--pg-bin','pg','--maven','maven','--native-s3',str(executable.resolve())],command[len(baseline):])
                executable.write_bytes(b'tampered')
                with self.assertRaisesRegex(ValueError,'executable changed'):local.integration_command(native_s3=executable)
            with self.assertRaises(FileNotFoundError):local.integration_command(native_s3=Path(folder)/'missing.exe')
        with self.assertRaises(ValueError):local.integration_command(pg_bin='pg')

    def test_restore_changed_rows_or_policy_cannot_pass(self):
        original={'data':'original','catalog':'rls-enabled'}
        restore.require_equal(original,dict(original),'unchanged')
        for changed in ({**original,'data':'missing'},{**original,'catalog':'rls-disabled'}):
            with self.assertRaisesRegex(RuntimeError,'Restore verification differs'):
                restore.require_equal(original,changed,'injected integrity failure')

    def test_restore_identifiers_reject_sql_fragments(self):
        self.assertEqual('"dzoka_restore_abc"',restore.identifier('dzoka_restore_abc'))
        for text in ('a; DROP DATABASE postgres','../data','a"',''):
            with self.assertRaises(ValueError):restore.identifier(text)

    def test_evidence_changes_when_source_changes_but_not_ignored_bytecode(self):
        (ROOT/'.test-tmp').mkdir(exist_ok=True)
        with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as folder:
            root=Path(folder)
            for directory in ('src','ai','tools','tests'):(root/directory).mkdir()
            for name in ('pom.xml','dependencies.lock.json','AGENTS.md'):(root/name).write_text('fixture')
            code=root/'src/source.java';code.write_text('first')
            before=local.source_digest(root)
            (root/'tools/__pycache__').mkdir();(root/'tools/__pycache__/tool.pyc').write_bytes(b'ignored')
            self.assertEqual(before,local.source_digest(root))
            code.write_text('changed');self.assertNotEqual(before,local.source_digest(root))


if __name__=='__main__':unittest.main()
