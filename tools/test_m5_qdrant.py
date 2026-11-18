"""Disposable loopback Qdrant and real PostgreSQL HTTP matching verification."""
import argparse
import json
import os
from pathlib import Path
import secrets
import subprocess
import sys
import time
import urllib.request
import uuid

IMAGE='qdrant/qdrant@sha256:2d1b0bfd0a961275481ea09400fc98008216ae54e1b52ed7d24520d63050c644'
ROOT=Path(__file__).resolve().parents[1]

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--pg-bin',required=True)
    parser.add_argument('--maven',required=True)
    args=parser.parse_args()
    name='dzoka-m5-qdrant-'+uuid.uuid4().hex
    key=secrets.token_urlsafe(32)
    env={**os.environ,'QDRANT__SERVICE__API_KEY':key,'QDRANT__TELEMETRY_DISABLED':'true'}

    def docker(*command):
        return subprocess.run(['docker',*command],env=env,capture_output=True,text=True,check=True,timeout=180).stdout

    try:
        docker('run','--detach','--pull','never','--name',name,'--publish','127.0.0.1::6333',
               '--env','QDRANT__SERVICE__API_KEY','--env','QDRANT__TELEMETRY_DISABLED',IMAGE)
        binding=json.loads(docker('inspect',name))[0]['NetworkSettings']['Ports']['6333/tcp'][0]
        if binding['HostIp']!='127.0.0.1':
            raise RuntimeError('Qdrant test binding is not loopback')
        origin='http://127.0.0.1:'+binding['HostPort']
        opener=urllib.request.build_opener(urllib.request.ProxyHandler({}))
        request=urllib.request.Request(origin+'/collections',headers={'api-key':key})
        for _ in range(60):
            try:
                with opener.open(request,timeout=2) as response:
                    if response.status==200:
                        break
            except OSError:
                time.sleep(0.5)
        else:
            raise RuntimeError('Qdrant test startup failed')
        env.update(DZOKA_MATCHING_QDRANT_ORIGIN=origin,DZOKA_MATCHING_QDRANT_API_KEY=key,
                   DZOKA_TEST_MATCHING_BACKEND='qdrant',DZOKA_TEST_MATCHING_ROUTE='QDRANT_EXACT',
                   DZOKA_IT_CLASSES='FoundationIT,MatchingIT')
        return subprocess.run([sys.executable,'-B','tools/test_foundation.py','--full','--restore-drill',
                               '--pg-bin',args.pg_bin,'--maven',args.maven],env=env,cwd=ROOT).returncode
    finally:
        cleanup=subprocess.run(['docker','rm','--force','--volumes',name],env=env,capture_output=True,timeout=60)
        if cleanup.returncode!=0 and b'No such container' not in cleanup.stderr:
            raise RuntimeError('Test Qdrant cleanup needs attention')
        print('Disposable Qdrant container cleanup verified.',flush=True)

if __name__=='__main__':
    raise SystemExit(main())
