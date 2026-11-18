import hashlib
from pathlib import Path
import sys
import unittest

sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'tools'))
from matching_calibration import evaluate


def digest(value): return hashlib.sha256(str(value).encode()).hexdigest()


class MatchingCalibrationTests(unittest.TestCase):
    def plan(self):
        rows=[]
        for i in range(16):
            rows.append({'caseSha256':digest(i),'groupSha256':digest(i+32),'label':bool(i%2),
                         'split':'fit' if i<8 else 'holdout','similarity':0.12 if i%2==0 else 0.88,
                         'coverage':0.6,'disposition':'COMPARABLE'})
        return {'schemaVersion':1,'datasetSha256':digest('corpus'),'spaceKey':digest('space'),
                'scoringVersion':'bounded-evidence-v1','minimumCoverage':0.3,'cases':rows}

    def test_draft_with_exact_matched_held_out_measurement_and_unsupported_bins(self):
        plan=self.plan()
        plan['cases'][-1]['similarity']=0.5
        result=evaluate(plan,digest('plan'))
        self.assertEqual('NOT_ESTABLISHED',result['approval'])
        self.assertEqual(8,result['holdoutCount'])
        self.assertEqual(7,result['supportedHoldout'])
        self.assertEqual(0.0,result['holdoutBrier'])
        self.assertGreater(result['rawScoreBrierOnSameRows'],0.0)
        self.assertIsNone(result['bins'][2]['observedFrequency'])

    def test_group_leakage_and_insufficient_evidence_never_fit(self):
        plan=self.plan()
        plan['cases'][-1]['groupSha256']=plan['cases'][0]['groupSha256']
        with self.assertRaisesRegex(ValueError,'overlaps'):
            evaluate(plan,digest('plan'))
        plan=self.plan()
        plan['cases'][-1].update(disposition='CONFLICT',similarity=1.0)
        with self.assertRaisesRegex(ValueError,'eight eligible'):
            evaluate(plan,digest('plan'))
