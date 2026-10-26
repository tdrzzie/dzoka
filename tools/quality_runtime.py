"""Explicit quality-artifact provisioning and fixture inference; no worker downloads."""
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
from quality import profiles,evaluation
from generative_runtime import valid

def main():
    parser=argparse.ArgumentParser();parser.add_argument('operation',choices=['fetch','check','profiles','probe','evaluate']);parser.add_argument('--profile');parser.add_argument('--output',default='.test-tmp/quality-evaluation-result.json');args=parser.parse_args();registry=profiles()
    if args.profile and args.profile not in registry:parser.error('Unknown reviewed profile')
    if args.operation=='profiles':print(json.dumps(registry,indent=2));return
    lock=json.loads((ROOT/'ai/quality.lock.json').read_bytes());directory=ROOT/'.audit-cache/quality-models';directory.mkdir(exist_ok=True)
    for entry in lock['files']:
        if args.profile and entry['group']!=args.profile:continue
        target=directory/entry['local']
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
        if not valid(target,entry):raise ValueError('Reviewed quality artifact unavailable')
    print('PASS: selected quality artifact/source/runtime identity',flush=True)
    if args.operation not in ('probe','evaluate'):return
    fixture=evaluation();names=[args.profile] if args.profile else list(registry);python=ROOT/'.audit-cache/generative-venv/Scripts/python.exe';rows=[]
    script="""import json,sys
sys.path.insert(0,'ai')
from quality import QualityProvider
from generative import GenerativeProvider
name=sys.argv[1];baseline=sys.argv[2]=='baseline'
cls=GenerativeProvider if baseline else QualityProvider
directory='.audit-cache/generative-models' if baseline else '.audit-cache/quality-models'
with cls(name,directory) as provider:
 for line in sys.stdin:
  case=json.loads(line);payload=json.dumps({'prompt':case['prompt'],'query':case['source']}).encode() if 'prompt' in case else case['source'].encode()
  print(json.dumps({'id':case['id'],'profile':name,'profileKey':provider.profile['key'],'baseline':baseline,'evidence':provider.infer(payload)}),flush=True)
"""
    for name in names:
        cases=[c for c in fixture['cases'] if c['profile']==name]
        scopes=[(name,False,cases)]
        if args.operation=='evaluate':
            for baseline in sorted({c['baseline'] for c in cases if c.get('baseline')}):scopes.append((baseline,True,[c for c in cases if c.get('baseline')==baseline]))
        for selected,baseline,subset in scopes:
            if not subset:continue
            run=subprocess.run([str(python),'-I','-c',script,selected,'baseline' if baseline else 'candidate'],input=''.join(json.dumps(c)+'\n' for c in subset),text=True,encoding='utf-8',capture_output=True,cwd=ROOT,check=True,timeout=290)
            for line in run.stdout.splitlines():
                row=json.loads(line);case=next(c for c in subset if c['id']==row['id']);text=row['evidence']['text'];words=text.lower().split();triples=[tuple(words[i:i+3]) for i in range(max(0,len(words)-2))]
                row['exactReferenceMatch']=text in case.get('references',[]) if case.get('references') else None;row['repeatedTrigrams']=len(triples)-len(set(triples));rows.append(row);print(json.dumps(row,ensure_ascii=True),flush=True)
    result={'schemaVersion':1,'generatedAt':datetime.now(timezone.utc).isoformat(),'scope':'Authored synthetic diagnostics; not representative accuracy or native-speaker acceptance','fixtureSha256':lock['evaluationSha256'],'runtimeLockSha256':lock['runtimeLockSha256'],'rows':rows}
    target=Path(args.output);target.parent.mkdir(parents=True,exist_ok=True);target.write_text(json.dumps(result,indent=2,ensure_ascii=False)+'\n',encoding='utf-8');print('Saved '+str(target),flush=True)

if __name__=='__main__':main()
