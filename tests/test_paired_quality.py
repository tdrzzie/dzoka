import copy
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'tools'))
import paired_quality as q


class PairedQualityTests(unittest.TestCase):
    def setUp(self):
        directory = tempfile.TemporaryDirectory(dir=ROOT / '.test-tmp')
        self.addCleanup(directory.cleanup)
        self.root = Path(directory.name)
        self.baseline = 'a' * 64
        self.candidate = 'b' * 64
        self.reference = self.put('reference.json', 'red bag')
        self.plan = {
            'schemaVersion': 1, 'scope': 'AUTHORED_DIAGNOSTIC',
            'evidence': self.put('evidence.json', {'scope': 'authored'}),
            'profiles': {role: {'profileKey': key, 'projectionSourceSha256': 'd' * 64}
                         for role, key in zip(q.ROLES, (self.baseline, self.candidate))},
            'cases': [],
        }
        self.observations = {}
        self.add_case('private-complete', 'TEST', '1', 'AVAILABLE', 'AVAILABLE')
        self.add_case('private-incomplete', 'TEST', '2', 'UNAVAILABLE', 'TRUNCATED')

    def put(self, name, value):
        raw = json.dumps(value).encode()
        (self.root / name).write_bytes(raw)
        return {'path': name, 'sha256': q.sha(raw)}

    def add_case(self, name, split, token, baseline_state, candidate_state):
        digest = token * 64
        self.plan['cases'].append({'id': name, 'split': split, 'slice': 'private-slice',
                                  'inputSha256': digest, 'entityGroup': 'private-item-' + token,
                                  'institutionGroup': 'private-institution-' + token, 'timeGroup': 'private-time-' + token,
                                  'reference': self.reference, 'metric': 'wer', 'parameters': {}})
        self.observations[name] = {
            role: {'profileKey': key, 'inputSha256': digest, 'state': state,
                   'artifact': None if state == 'UNAVAILABLE' else self.put(name + '-' + role + '.json', text)}
            for role, key, state, text in zip(q.ROLES, (self.baseline, self.candidate),
                                              (baseline_state, candidate_state), ('blue bag', 'red bag'))
        }

    def evaluate(self, expected_plan=None):
        plan = self.put('plan.json', self.plan)
        self.put('observations.json', {'plan': plan, 'observations': self.observations})
        with patch.object(q, 'protected', side_effect=Path), patch.object(q, 'audit_records'):
            return q.evaluate(self.root / 'observations.json', expected_plan or plan['sha256'])

    def test_paired_complete_and_unavailable_are_distinct(self):
        self.add_case('private-validation', 'VALIDATION', '3', 'AVAILABLE', 'AVAILABLE')
        result = self.evaluate()
        self.assertEqual('NOT_ESTABLISHED', result['approval'])
        first, second, _ = result['rows']
        self.assertTrue(first['pairedComplete'])
        self.assertEqual(0.5, first['baseline']['metrics']['value'])
        self.assertEqual(0, first['candidate']['metrics']['value'])
        self.assertFalse(second['pairedComplete'])
        self.assertEqual({'state': 'UNAVAILABLE'}, second['baseline'])
        self.assertEqual('TRUNCATED', second['candidate']['state'])
        test, validation = result['coverage']
        self.assertEqual(('TEST', 1, 1, 1), (test['split'], test['pairedComplete'],
                                            test['baseline']['UNAVAILABLE'], test['candidate']['TRUNCATED']))
        self.assertEqual(('VALIDATION', 1), (validation['split'], validation['pairedComplete']))
        for private in ('private-', 'red bag', 'blue bag', 'reference.json'):
            self.assertNotIn(private, json.dumps(result))

    def test_missing_extra_or_unmatched_outputs_fail_before_scoring(self):
        with patch.object(q, 'measure') as metric, self.assertRaisesRegex(ValueError, 'retained digest'):
            self.evaluate('a' * 64)
        metric.assert_not_called()
        original = copy.deepcopy(self.observations)
        for mutation in (
            lambda observed: observed.pop('private-incomplete'),
            lambda observed: observed.update(extra=original['private-complete']),
            lambda observed: observed['private-incomplete']['candidate'].update(inputSha256='3' * 64),
            lambda observed: observed['private-incomplete']['candidate'].update(profileKey=self.baseline),
            lambda observed: observed['private-incomplete']['baseline'].update(state='AVAILABLE'),
            lambda observed: observed['private-incomplete']['candidate']['artifact'].update(sha256='f' * 64),
        ):
            self.observations = copy.deepcopy(original)
            mutation(self.observations)
            with patch.object(q, 'measure') as metric, self.assertRaises(ValueError):
                self.evaluate()
            metric.assert_not_called()

    def test_declared_groups_cannot_leak_and_vectors_need_their_own_protocol(self):
        original = copy.deepcopy(self.plan)
        for field in q.GROUPS:
            self.plan = copy.deepcopy(original)
            self.plan['cases'][1].update(split='TRAIN', **{field: self.plan['cases'][0][field]})
            with self.assertRaisesRegex(ValueError, 'Cross-split'):
                self.evaluate()
        self.plan = original
        self.plan['cases'][0]['metric'] = 'cosine'
        with self.assertRaisesRegex(ValueError, 'non-vector'):
            self.evaluate()
