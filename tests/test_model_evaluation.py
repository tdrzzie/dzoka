import hashlib
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
import model_evaluation as evaluation

class EvaluationTests(unittest.TestCase):
    def test_edit_metrics_normalization_and_reference_bounds(self):
        self.assertEqual(1,evaluation.score('exact','CAFÉ  BAG','cafe\u0301 bag'))
        self.assertEqual(.25,evaluation.score('wer','blue bag number two','blue bag number four'))
        self.assertEqual(1/3,evaluation.score('cer','cat','cut'))
        self.assertEqual(0,evaluation.score('wer','same words','same words'))
        for metric,actual,expected in [('bleu','a','a'),('cer','a',''),('exact','a'*4097,'a')]:
            with self.assertRaises(ValueError):evaluation.score(metric,actual,expected)
    def test_hash_bound_result_excludes_raw_source_reference_and_output(self):
        with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as tmp:
            root=Path(tmp);source=b'private synthetic source';(root/'input').write_bytes(source);(root/'evidence').write_bytes(b'authored diagnostic')
            case={'profile':'TEST','path':'input','sha256':hashlib.sha256(source).hexdigest(),'reference':'private expected result','metric':'exact'}
            value={'schemaVersion':1,'scope':'AUTHORED_DIAGNOSTIC','evidence':{'path':'evidence','sha256':hashlib.sha256(b'authored diagnostic').hexdigest()},'cases':[case]};path=root/'dataset.json';path.write_bytes(json.dumps(value).encode())
            with patch.object(evaluation,'protected',side_effect=Path),patch.object(evaluation,'audit_records'),patch.object(evaluation,'plain',side_effect=Path),patch.object(evaluation,'profiles',return_value={'TEST':{'key':'a'*64,'manifest':{'task':'TRANSLATION'}}}),patch.object(evaluation,'verify_bundle'),patch.object(evaluation,'infer',return_value={'text':'private expected result'}) as infer:
                result=evaluation.evaluate(path,'bundle','b'*64,'state');self.assertEqual(1,result['rows'][0]['value']);self.assertEqual('NOT_ESTABLISHED',result['approval']);self.assertNotIn('private',json.dumps(result))
                for mutation in ('tamper','duplicate','escape'):
                    candidate=json.loads(json.dumps(value))
                    if mutation=='tamper':candidate['cases'][0]['sha256']='0'*64
                    if mutation=='duplicate':candidate['cases'].append(case)
                    if mutation=='escape':candidate['cases'][0]['path']='../outside'
                    path.write_bytes(json.dumps(candidate).encode());infer.reset_mock()
                    with self.assertRaises(ValueError):evaluation.evaluate(path,'bundle','b'*64,'state')
                    infer.assert_not_called()
