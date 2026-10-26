"""Regression tests for provenance integrity and the architecture gate."""
import copy
import hashlib
import json
import tempfile
import unittest
from pathlib import Path

from tools.audit_registry import LOCK, SECTIONS, read_json, validate


class RegistryTests(unittest.TestCase):
    def setUp(self):
        temp_root = Path(__file__).resolve().parents[1] / '.test-tmp'
        temp_root.mkdir(exist_ok=True)
        self.temp = tempfile.TemporaryDirectory(dir=temp_root)
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        assert self.root.resolve().is_relative_to(temp_root.resolve())
        self.source = {
            'id': 'example', 'kind': 'repository',
            'repository': 'https://github.com/example/project',
            'branch': 'main', 'revision': 'a' * 40, 'status': 'PENDING',
            'disposition': None, 'license': 'UNVERIFIED',
            'audit': 'docs/source-audit/retrieval/example.md', 'evidence': None,
        }
        self.data = {'schemaVersion': 1, 'requiredSources': ['example'], 'sources': [self.source]}
        self.write('docs/source-audit/scope.json', {'requiredSources': ['example']})
        self.sync()

    def write(self, name, data):
        path = self.root / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')

    def sync(self):
        self.write(str(LOCK), self.data)
        if isinstance(self.source['audit'], str) and '..' not in self.source['audit']:
            path = self.root / self.source['audit']
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(
                f"Status: {self.source['status']}\nRevision: {self.source['revision']}\n"
                + '\n'.join(f'## {s}\n\nReview finding.\n' for s in SECTIONS),
                encoding='utf-8',
            )

    def evidence(self):
        sid = self.source['id']
        self.source['evidence'] = f'docs/source-audit/evidence/{sid}.json'
        evidence = {
            'sourceId': sid, 'repository': self.source['repository'],
            'revision': self.source['revision'],
            'limitations': {'tests': 'No tests in reviewed upstream tree.', 'deployment': 'No deployment files in reviewed tree.'},
            'files': [{
                'path': 'engine.py', 'role': 'implementation', 'lines': [[1, 10]], 'lineCount': 10,
                'sha256': 'b' * 64, 'blobSha': 'c' * 40,
                'url': self.source['repository'] + '/blob/' + self.source['revision'] + '/engine.py',
                'observation': 'Independent implementation inspected.',
            }, {
                'path': 'LICENSE', 'role': 'license', 'lines': [[1, 20]], 'lineCount': 20,
                'sha256': 'd' * 64, 'blobSha': 'e' * 40,
                'url': self.source['repository'] + '/blob/' + self.source['revision'] + '/LICENSE',
                'observation': 'MIT license text inspected.',
            }],
        }
        return evidence

    def save_evidence(self, evidence):
        self.write(self.source['evidence'], evidence)
        raw = (self.root / self.source['evidence']).read_bytes()
        self.source['evidenceSha256'] = hashlib.sha256(raw).hexdigest()
        self.sync()

    def complete(self):
        self.source.update(status='COMPLETE', disposition='REFERENCE', auditedAt='2026-10-26',
                           license='MIT', licenseFinding='Verified root license.')
        self.save_evidence(self.evidence())

    def test_pending_is_valid_but_cannot_open_gate(self):
        self.assertEqual(validate(self.root), [])
        self.assertTrue(any('mandatory audit is PENDING' in e for e in validate(self.root, gate=True)))

    def test_complete_scoped_review_can_open_gate(self):
        self.complete()
        self.assertEqual(validate(self.root, gate=True), [])

    def test_forged_complete_without_evidence_fails(self):
        self.source['status'] = 'COMPLETE'
        self.sync()
        self.assertTrue(any('evidence manifest' in e for e in validate(self.root, gate=True)))

    def test_removed_required_source_fails(self):
        self.data['sources'] = []
        self.sync()
        self.assertIn('example: mandatory source missing from registry', validate(self.root))

    def test_changed_required_scope_fails(self):
        self.data['requiredSources'] = ['different']
        self.sync()
        self.assertTrue(any('differ from scope' in e for e in validate(self.root)))

    def test_duplicate_source_fails(self):
        self.data['sources'].append(copy.deepcopy(self.source))
        self.sync()
        self.assertIn('example: duplicate source ID', validate(self.root))

    def test_unpinned_revision_fails(self):
        self.source['revision'] = 'main'
        self.sync()
        self.assertTrue(any('full lowercase commit SHA' in e for e in validate(self.root)))

    def test_traversal_is_rejected(self):
        self.source['audit'] = '../outside.md'
        self.sync()
        self.assertTrue(any('escapes repository' in e for e in validate(self.root)))

    def test_evidence_tampering_fails(self):
        self.complete()
        path = self.root / self.source['evidence']
        path.write_text(path.read_text(encoding='utf-8') + ' ', encoding='utf-8')
        self.assertIn('example: evidence manifest digest mismatch', validate(self.root))

    def test_wrong_source_identity_fails(self):
        self.complete()
        evidence = read_json(self.root / self.source['evidence'])
        evidence['revision'] = 'f' * 40
        self.save_evidence(evidence)
        self.assertIn('example: evidence identity or revision differs from registry', validate(self.root))

    def test_mutable_evidence_url_fails(self):
        self.complete()
        evidence = read_json(self.root / self.source['evidence'])
        evidence['files'][0]['url'] = 'https://github.com/example/project/blob/main/engine.py'
        self.save_evidence(evidence)
        self.assertTrue(any('URL must use pinned commit' in e for e in validate(self.root)))

    def test_empty_line_range_fails(self):
        self.complete()
        evidence = read_json(self.root / self.source['evidence'])
        evidence['files'][0]['lines'] = [[10, 1]]
        self.save_evidence(evidence)
        self.assertTrue(any('invalid inspected line ranges' in e for e in validate(self.root)))

    def test_missing_review_section_fails(self):
        path = self.root / self.source['audit']
        path.write_text(path.read_text(encoding='utf-8').replace('## Security model', '## Other'), encoding='utf-8')
        self.assertIn('example: missing audit section Security model', validate(self.root))

    def test_document_status_cannot_disagree_with_registry(self):
        path = self.root / self.source['audit']
        path.write_text(path.read_text(encoding='utf-8').replace('Status: PENDING', 'Status: COMPLETE'), encoding='utf-8')
        self.assertIn('example: document status differs from registry', validate(self.root))

    def test_duplicate_json_key_fails(self):
        (self.root / LOCK).write_text('{"schemaVersion":1,"schemaVersion":1}', encoding='utf-8')
        self.assertTrue(any('duplicate JSON key' in e for e in validate(self.root)))

    def test_malformed_json_fails(self):
        (self.root / LOCK).write_text('{', encoding='utf-8')
        self.assertTrue(validate(self.root))

    def test_out_of_bounds_inspection_fails(self):
        self.complete()
        evidence = read_json(self.root / self.source['evidence'])
        evidence['files'][0]['lines'] = [[1, 11]]
        self.save_evidence(evidence)
        self.assertTrue(any('invalid inspected line ranges' in e for e in validate(self.root)))

    def test_wrong_type_status_fails_without_crashing(self):
        self.source['status'] = []
        self.sync()
        self.assertIn('example: invalid status', validate(self.root))

    def test_wrong_type_revision_fails_without_crashing(self):
        self.source['revision'] = {}
        self.sync()
        self.assertTrue(any('full lowercase commit SHA' in e for e in validate(self.root)))

    def test_wrong_type_evidence_files_fails_without_crashing(self):
        self.complete()
        evidence = read_json(self.root / self.source['evidence'])
        evidence['files'] = 42
        self.save_evidence(evidence)
        self.assertIn('example: evidence files must be nonempty array', validate(self.root))


if __name__ == '__main__':
    unittest.main()
