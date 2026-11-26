"""Verify the real M11 adapter against the reviewed disposable MLflow image."""
import argparse
import json
import os
from pathlib import Path
import socket
import subprocess
import time
import urllib.request
import uuid

ROOT=Path(__file__).resolve().parents[1]
IMAGE='ghcr.io/mlflow/mlflow@sha256:19f9de44741318515fc3a992429f177420c32d40d971a25f6ae4621a1b9c8516'

def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--pg-bin',required=True);parser.add_argument('--maven',required=True);args=parser.parse_args()
    name='dzoka-m11-mlflow-'+uuid.uuid4().hex[:12];folder=ROOT/'.test-tmp'/name;folder.mkdir(parents=True)
    with socket.socket() as s:s.bind(('127.0.0.1',0));port=s.getsockname()[1]
    base=f'http://127.0.0.1:{port}';started=False
    def command(*parts,input=None):return subprocess.run(list(parts),cwd=ROOT,check=True,capture_output=True,text=True,encoding='utf-8',timeout=120,input=input).stdout
    def request(path,body=None):
        req=urllib.request.Request(base+path,data=None if body is None else json.dumps(body).encode(),headers={'Content-Type':'application/json'})
        with urllib.request.urlopen(req,timeout=5) as response:return json.load(response)
    try:
        command('docker','run','-d','--name',name,'--network','bridge','--read-only','--user','65534:65534','--cap-drop','ALL','--security-opt','no-new-privileges','--memory','1536m','--cpus','2','--pids-limit','128','--tmpfs','/tmp:size=256m,mode=1777','-e','MLFLOW_DISABLE_TELEMETRY=true','-e','MPLCONFIGDIR=/tmp/mpl','-p',f'127.0.0.1:{port}:5000',IMAGE,'mlflow','server','--host','0.0.0.0','--port','5000','--backend-store-uri','sqlite:////tmp/mlflow.db','--default-artifact-root','file:///tmp/artifacts','--no-serve-artifacts','--workers','1');started=True
        deadline=time.monotonic()+60
        while True:
            try:experiment=request('/api/2.0/mlflow/experiments/create',{'name':'synthetic-'+uuid.uuid4().hex})['experiment_id'];break
            except (OSError,ValueError):
                if time.monotonic()>deadline:raise RuntimeError('Real MLflow readiness failed')
                time.sleep(.5)
        inventory=command('docker','exec',name,'python','-c',"import importlib.metadata as m,json;print(json.dumps(sorted([{'name':d.metadata['Name'],'version':d.version,'license':d.metadata.get('License-Expression') or d.metadata.get('License')} for d in m.distributions()],key=lambda x:x['name'])))")
        (folder/'packages.json').write_text(inventory,encoding='utf-8')
        env=os.environ.copy();env.update(DZOKA_TEST_MLFLOW_ORIGIN=base,DZOKA_TEST_MLFLOW_EXPERIMENT=experiment,DZOKA_IT_CLASSES='LearningIT,OperationsIT',DZOKA_TEST_OTLP_OUTPUT=str(folder/'otlp.json'))
        with (folder/'integration.log').open('w',encoding='utf-8') as log:
            result=subprocess.run(['python','-B','tools/test_foundation.py','--full','--restore-drill','--pg-bin',args.pg_bin,'--maven',args.maven],env=env,cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,timeout=180)
        if result.returncode:raise RuntimeError('Real MLflow native integration failed; inspect private local test log')
        protobuf_check='''import sys,json,base64
from google.protobuf.json_format import ParseDict
from opentelemetry.proto.collector.trace.v1.trace_service_pb2 import ExportTraceServiceRequest
from opentelemetry.proto.collector.metrics.v1.metrics_service_pb2 import ExportMetricsServiceRequest
from opentelemetry.proto.collector.logs.v1.logs_service_pb2 import ExportLogsServiceRequest
def normalize(value):
 if isinstance(value,dict):
  for key,item in value.items():
   if key in ('traceId','spanId','parentSpanId'):
    raw=bytes.fromhex(item)
    assert len(raw)==(16 if key=='traceId' else 8) and any(raw)
    value[key]=base64.b64encode(raw).decode()
   else:normalize(item)
 elif isinstance(value,list):
  for item in value:normalize(item)
types={'/v1/traces':ExportTraceServiceRequest,'/v1/metrics':ExportMetricsServiceRequest,'/v1/logs':ExportLogsServiceRequest}
signals=json.load(sys.stdin);seen=set()
for signal in signals:
 normalize(signal['payload']);ParseDict(signal['payload'],types[signal['kind']]());seen.add(signal['kind'])
assert seen==set(types)
print(json.dumps({'status':'PASS','signals':sorted(seen),'payloads':len(signals)}))'''
        protobuf=json.loads(command('docker','exec','-i',name,'python','-c',protobuf_check,input=(folder/'otlp.json').read_text()))
        runs=request('/api/2.0/mlflow/runs/search',{'experiment_ids':[experiment],'max_results':10}).get('runs',[])
        models=request('/api/2.0/mlflow/registered-models/search?max_results=10').get('registered_models',[])
        if len(runs)!=1 or len(models)!=1:raise RuntimeError('Real MLflow duplicated or lost metadata')
        model=models[0];tags={t['key']:t['value'] for t in model.get('tags',[])}
        if tags.get('dzoka.native.evidence')!='SUSPENDED':raise RuntimeError('Native revocation was not mirrored')
        evidence={'status':'PASS','mlflow':'3.18.0','image':IMAGE,'runs':len(runs),'registeredModels':len(models),'metrics':len(runs[0].get('data',{}).get('metrics',[])),'nativeActivationMirrored':True,'nativeRevocationMirrored':True,'otlpProtobufValidation':protobuf,'scope':'synthetic digest-only model/dataset metadata; no artifact execution'}
        (folder/'result.json').write_text(json.dumps(evidence,indent=2)+'\n');print(json.dumps(evidence,indent=2));print('Evidence:',folder/'result.json')
    finally:
        if started:
            try:(folder/'server.log').write_text(command('docker','logs',name),encoding='utf-8')
            finally:
                if not name.startswith('dzoka-m11-mlflow-'):raise RuntimeError('Unsafe cleanup name')
                command('docker','rm','-f',name)

if __name__=='__main__':main()
