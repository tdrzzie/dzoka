"""Actual isolated embedding and private reference-protocol boundary checks."""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import uuid
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'tools'));sys.path.insert(0,str(ROOT/'ai'))
from verify_worker_boundary import private_fixture
from broker_bundle import protect,verify_bundle
from model_broker import infer
from broker_profiles import profiles
from quality_protocols import evaluate


def main():
    bundle=os.environ['DZOKA_TEST_BROKER_BUNDLE'];receipt=os.environ['DZOKA_TEST_BROKER_SHA256']
    verify_bundle(bundle,receipt);cases=[]
    with private_fixture() as root:
        protect(root)
        for name in ('state','state/profiles','state/docker','dataset'):
            (root/name).mkdir();protect(root/name)
        data=b'Authored synthetic blue travel bag.'
        output=infer(bundle,receipt,root/'state','ISO_TEXT',data,checked=True)
        actual=output['vector'];reference=[0.0]*len(actual);reference[0]=1.0
        dataset=root/'dataset'
        def put(name,value):
            raw=json.dumps(value).encode();(dataset/name).write_bytes(raw)
            return {'path':name,'sha256':hashlib.sha256(raw).hexdigest()}
        key=profiles()['ISO_TEXT']['key']
        case={'id':'authored','split':'TEST','slice':'authored','inputSha256':hashlib.sha256(data).hexdigest(),
              'entityGroup':'authored','institutionGroup':'authored','timeGroup':'authored',
              'spaceKey':key,'referenceSpaceKey':key,'artifact':put('actual.json',actual),
              'reference':put('reference.json',reference),'metric':'cosine','parameters':{},
              'projectionSourceSha256':hashlib.sha256(Path(__file__).read_bytes()).hexdigest()}
        manifest={'schemaVersion':1,'scope':'AUTHORED_DIAGNOSTIC',
                  'evidence':put('permission.json',{'origin':'authored synthetic, no user data'}),'cases':[case]}
        path=dataset/'manifest.json';path.write_text(json.dumps(manifest),encoding='utf-8')
        result=evaluate(path)
        assert result['approval']=='NOT_ESTABLISHED' and len(result['rows'])==1
        assert abs(result['rows'][0]['metrics']['cosine']-actual[0])<1e-5
        assert data.decode() not in json.dumps(result)
        cases.append('actual_isolated_embedding_private_metric')
        subprocess.run([sys.executable,'-I','-B',str(ROOT/'tools/quality_protocols.py'),
                        '--dataset',str(path),'--output',str(root/'result.json')],check=True,timeout=120)
        assert json.loads((root/'result.json').read_bytes())['approval']=='NOT_ESTABLISHED'
        cases.append('native_private_cli_new_result')
        # The result is append-only: a second invocation must refuse replacement.
        repeated=subprocess.run([sys.executable,'-I','-B',str(ROOT/'tools/quality_protocols.py'),
                        '--dataset',str(path),'--output',str(root/'result.json')],capture_output=True,timeout=120)
        assert repeated.returncode!=0;cases.append('existing_result_not_replaced')
        subprocess.run(['icacls',str(dataset/'actual.json'),'/grant','*S-1-5-11:(RX)'],check=True,stdout=subprocess.DEVNULL)
        try:
            try:evaluate(path)
            except (ValueError,subprocess.CalledProcessError):cases.append('broad_leaf_read_rejected')
            else:raise AssertionError('Broad leaf read accepted')
        finally:subprocess.run(['icacls',str(dataset/'actual.json'),'/remove:g','*S-1-5-11'],check=True,stdout=subprocess.DEVNULL)
    run=ROOT/'.test-tmp'/('quality-protocols-'+uuid.uuid4().hex);run.mkdir()
    (run/'result.json').write_text(json.dumps({'status':'PASS','cases':cases,'bundleSha256':receipt,
                                             'protocolSourceSha256':result['protocolSourceSha256']},indent=2)+'\n')
    print('PASS: private reference protocols; '+str(run/'result.json'),flush=True)


if __name__=='__main__':main()
