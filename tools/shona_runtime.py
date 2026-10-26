"""Explicit Shona-artifact provisioning and fixture inference; no worker downloads."""
import argparse
from datetime import datetime,timezone
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import urllib.request
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'ai'))
from shona import profiles,evaluation
from generative_runtime import valid

def main():
    parser=argparse.ArgumentParser();parser.add_argument('operation',choices=['fetch','check','profiles','probe','runtime-check','evaluate']);parser.add_argument('--profile');args=parser.parse_args();registry=profiles()
    if args.profile and args.profile not in registry:parser.error('Unknown reviewed profile')
    if args.operation=='profiles':print(json.dumps(registry,indent=2));return
    if args.operation=='runtime-check':
        subprocess.run([str(ROOT/'.audit-cache/shona-venv/Scripts/python.exe'),'-I','-c',"import sys;sys.path.insert(0,'ai');from shona_integrity import verify_runtime;verify_runtime();print('PASS: installed 23-wheel runtime and notices')"],cwd=ROOT,check=True,timeout=120);return
    lock=json.loads((ROOT/'ai/shona.lock.json').read_bytes());directory=ROOT/'.audit-cache/shona-models';directory.mkdir(exist_ok=True)
    for entry in lock['files']:
        if args.profile and entry['group']!=registry[args.profile]['manifest']['configuration'].get('artifactGroup',args.profile):continue
        target=directory/entry['local'];target.parent.mkdir(parents=True,exist_ok=True)
        if args.operation=='fetch' and not valid(target,entry):
            part=target.with_suffix(target.suffix+'.download')
            try:
                with urllib.request.urlopen(entry['url'],timeout=90) as source,part.open('wb') as output:
                    size=0
                    while data:=source.read(65536):
                        size+=len(data)
                        if size>entry['bytes']:raise ValueError('Artifact exceeds bound')
                        output.write(data)
                if not valid(part,entry):raise ValueError('Artifact mismatch')
                part.replace(target);print('Verified '+entry['local'],flush=True)
            finally:part.unlink(missing_ok=True)
        if not valid(target,entry):raise ValueError('Reviewed Shona artifact unavailable')
    print('PASS: selected Shona artifact/source/runtime identity',flush=True)
    if args.operation=='evaluate':
        fixture=evaluation();rows=[]
        script="""import json,sys,os
sys.path.insert(0,'ai');os.environ['DZOKA_AI_SHONA_LOCAL_DEVELOPMENT']='true'
from shona import ShonaProvider
from quality import QualityProvider
baseline=sys.argv[2]=='baseline';cls=QualityProvider if baseline else ShonaProvider;directory='.audit-cache/quality-models' if baseline else '.audit-cache/shona-models'
with cls(sys.argv[1],directory) as p:
 for line in sys.stdin:
  case=json.loads(line);print(json.dumps({'id':case['id'],'profile':sys.argv[1],'profileKey':p.profile['key'],'baseline':baseline,'source':case['source'],'evidence':p.infer(case['source'].encode())}),flush=True)
"""
        for name in ([args.profile] if args.profile else list(registry)):
            cases=[c for c in fixture['cases'] if c['profile']==name];scopes=[(name,False,cases)]
            for baseline in sorted({c['baseline'] for c in cases if c.get('baseline')}):scopes.append((baseline,True,[c for c in cases if c.get('baseline')==baseline]))
            for selected,baseline,subset in scopes:
                if not subset:continue
                python=ROOT/('.audit-cache/generative-venv/Scripts/python.exe' if baseline else '.audit-cache/shona-venv/Scripts/python.exe')
                run=subprocess.run([str(python),'-I','-c',script,selected,'baseline' if baseline else 'candidate'],input=''.join(json.dumps(c)+'\n' for c in subset),text=True,encoding='utf-8',capture_output=True,cwd=ROOT,check=True,timeout=290)
                for line in run.stdout.splitlines():row=json.loads(line);rows.append(row);print(json.dumps(row),flush=True)
        target=ROOT/'.test-tmp/shona-evaluation-result.json';target.write_text(json.dumps({'schemaVersion':1,'generatedAt':datetime.now(timezone.utc).isoformat(),'scope':fixture['scope'],'fixtureSha256':lock['evaluationSha256'],'runtimeLockSha256':lock['runtimeLockSha256'],'rows':rows},indent=2)+'\n',encoding='utf-8');print('Saved '+str(target));return
    if args.operation!='probe':return
    names=[args.profile] if args.profile else list(registry)
    script="""import json,sys,os
sys.path.insert(0,'ai')
from shona import ShonaProvider
os.environ['DZOKA_AI_SHONA_LOCAL_DEVELOPMENT']='true'
name=sys.argv[1]
samples={'CT2_EN_SN':['I lost my bag.','I left a black bag in Joina City.','I found a red umbrella.'],'CT2_SN_EN':['Ndarasa bhegi rangu.','Ndasiya bhegi dema muJoina City.','Ndasiya black Lenovo bag muJoina around 2pm.'],'CT2_SN_EN_KEEP':['Ndasiya [[keep:black Lenovo bag]] mu[[keep:Joina]] [[keep:around 2pm]].'],'CT2_EN_SN_KEEP':['I left a black bag in [[keep:Joina City]].']}[name]
with ShonaProvider(name,'.audit-cache/shona-models') as p:
 for source in samples:print(json.dumps({'profile':name,'profileKey':p.profile['key'],'source':source,'evidence':p.infer(source.encode())}),flush=True)
"""
    for name in names:subprocess.run([str(ROOT/'.audit-cache/shona-venv/Scripts/python.exe'),'-I','-c',script,name],cwd=ROOT,check=True,timeout=290)

if __name__=='__main__':main()
