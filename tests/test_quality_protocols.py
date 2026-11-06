import copy
import hashlib
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
import quality_protocols as q


class QualityProtocolTests(unittest.TestCase):
    def test_vectors_require_compatible_nonzero_finite_inputs(self):
        self.assertEqual(1,q.measure('cosine',[1e300,0],[2e300,0],{})['cosine'])
        self.assertEqual(-1,q.measure('cosine',[1,0],[-1,0],{})['cosine'])
        for a,b in [([0],[1]),([True],[1]),([float('inf')],[1]),([1],[1,2])]:
            with self.assertRaises(ValueError):q.measure('cosine',a,b,{})

    def test_duplicate_detections_count_as_false_positives(self):
        box={'label':'bag','box':[0,0,1,1]}
        result=q.measure('boxes',[box,box],[box],{'iouThreshold':0.5})
        self.assertEqual((1,1,0),(result['truePositive'],result['falsePositive'],result['falseNegative']))
        self.assertEqual(0.5,result['precision'])
        self.assertIsNone(q.measure('boxes',[],[],{'iouThreshold':0.5})['recall'])
        for threshold in (0,True,float('nan')):
            with self.assertRaises(ValueError):q.measure('boxes',[],[],{'iouThreshold':threshold})
        with self.assertRaises(ValueError):q.measure('boxes',[{'label':'bag','box':[1,0,0,1]}],[],{'iouThreshold':0.5})

    def test_masks_geometry_class_macro_and_label_bounds(self):
        a={'shape':[1,2],'labels':[0,1]};b={'shape':[1,2],'labels':[0,0]}
        r=q.measure('mask',a,b,{})
        self.assertEqual(0.5,r['pixelAgreement']);self.assertEqual(0.25,r['macroIou'])
        self.assertAlmostEqual(1/3,r['macroDice'])
        with self.assertRaises(ValueError):q.measure('mask',a,{'shape':[2,1],'labels':[0,1]},{})
        with self.assertRaises(ValueError):q.measure('mask',a,{'shape':[1,2],'labels':[0,True]},{})

    def test_set_and_ranking_metrics_preserve_missing_evidence(self):
        self.assertIsNone(q.measure('labels',[],[],{})['f1'])
        self.assertEqual(1/2,q.measure('ranking',['x','a'],['a'],{})['reciprocalRank'])
        self.assertEqual(0,q.measure('ranking',['x'],['a'],{})['recallInReturnedList'])
        with self.assertRaises(ValueError):q.measure('ranking',['a','a'],['a'],{})
        with self.assertRaises(ValueError):q.measure('generative','answer','reference',{})

    def test_all_split_groups_and_hashes_checked_before_metrics(self):
        with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as directory:
            root=Path(directory)
            def put(name,value):
                raw=json.dumps(value).encode();(root/name).write_bytes(raw)
                return {'path':name,'sha256':q.sha(raw)}
            evidence=put('permission.json',{'scope':'authored'})
            case={'id':'private-id','split':'TRAIN','slice':'private-slice','inputSha256':'a'*64,
                  'entityGroup':'private-entity','institutionGroup':'private-institution','timeGroup':'private-time',
                  'spaceKey':'b'*64,'referenceSpaceKey':'b'*64,'artifact':put('actual.json',[1,0]),
                  'reference':put('reference.json',[1,0]),'metric':'cosine','parameters':{},'projectionSourceSha256':'c'*64}
            dataset={'schemaVersion':1,'scope':'AUTHORED_DIAGNOSTIC','evidence':evidence,'cases':[case]}
            path=root/'manifest.json'
            def run(value):path.write_text(json.dumps(value));return q.evaluate(path)
            with patch.object(q,'protected',side_effect=Path),patch.object(q,'audit_records'):
                result=run(dataset);raw=json.dumps(result)
                self.assertEqual('NOT_ESTABLISHED',result['approval'])
                for private in ('private-id','private-slice','private-entity','private-institution','private-time','[1, 0]'):
                    self.assertNotIn(private,raw)
                for field in ('inputSha256','entityGroup','institutionGroup','timeGroup'):
                    other=copy.deepcopy(case);other.update(id='other',split='TEST',inputSha256='d'*64,entityGroup='e',institutionGroup='i',timeGroup='t');other[field]=case[field]
                    bad=copy.deepcopy(dataset);bad['cases'].append(other)
                    with patch.object(q,'measure') as metric:
                        with self.assertRaises(ValueError):run(bad)
                        metric.assert_not_called()
                for field,value in [('referenceSpaceKey','d'*64),('id',None)]:
                    bad=copy.deepcopy(dataset);bad['cases'][0][field]=value
                    with self.assertRaises(ValueError):run(bad)
                bad=copy.deepcopy(dataset);bad['cases'][0]['artifact']['path']='../escape'
                with self.assertRaises(ValueError):run(bad)
                (root/'actual.json').write_bytes(b'[0,1]')
                with patch.object(q,'measure') as metric:
                    with self.assertRaises(ValueError):run(dataset)
                    metric.assert_not_called()
                path.write_bytes(b'{"scope":1,"scope":2}')
                with self.assertRaises(ValueError):q.evaluate(path)
