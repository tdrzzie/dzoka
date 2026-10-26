"""Explicit reviewed artifact/bootstrap checks; no application-startup network or fallback."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import urllib.request
ROOT=Path(__file__).resolve().parents[1]

def valid(path,entry):
    if not path.is_file() or path.stat().st_size!=entry['bytes']:return False
    with path.open('rb') as stream:return hashlib.file_digest(stream,'sha256').hexdigest()==entry['sha256']

def main():
    parser=argparse.ArgumentParser();parser.add_argument('operation',choices=['fetch','check','probe','profiles','runtime-check']);parser.add_argument('--profile');parser.add_argument('--all-profiles',action='store_true');args=parser.parse_args()
    sys.path.insert(0,str(ROOT/'ai'));from generative import profiles
    registry=profiles()
    if args.profile and args.profile not in registry:parser.error('Unknown reviewed profile')
    if args.operation=='profiles':print(json.dumps(registry,indent=2));return
    python=ROOT/'.audit-cache/generative-venv/Scripts/python.exe'
    if args.operation=='runtime-check':
        subprocess.run([str(python),'-I','-c',"import sys;sys.path.insert(0,'ai');from generative_integrity import verify_runtime;verify_runtime();print('PASS: installed layered 22-wheel runtime and notices')"],cwd=ROOT,check=True,timeout=120);return
    lock=json.loads((ROOT/'ai/generative.lock.json').read_bytes());runtime=json.loads((ROOT/'ai/generative-runtime.lock.json').read_bytes())
    selected=lock['files']
    if args.operation=='probe' and args.profile:
        group='LLM' if args.profile=='LLM_EN' else 'MULTILINGUAL';selected=[f for f in selected if f['group']==group]
    for directory,files in [(ROOT/'.audit-cache/generative-models',selected),(ROOT/'.audit-cache/generative-wheels',runtime['additionalWheels'])]:
        directory.mkdir(exist_ok=True)
        for entry in files:
            target=directory/entry.get('local',entry.get('wheel'))
            if args.operation=='fetch' and not valid(target,entry):
                part=target.with_suffix(target.suffix+'.download')
                try:
                    with urllib.request.urlopen(entry['url'],timeout=60) as source,part.open('wb') as output:
                        size=0
                        while data:=source.read(65536):
                            size+=len(data)
                            if size>entry['bytes']:raise ValueError('Artifact exceeds bound')
                            output.write(data)
                    if not valid(part,entry):raise ValueError('Artifact mismatch')
                    part.replace(target)
                finally:part.unlink(missing_ok=True)
            if not valid(target,entry):raise ValueError('Reviewed generative artifact unavailable')
    print('PASS: '+('selected profile' if args.operation=='probe' and args.profile else 'all generative')+' artifact/source/runtime-lock identity',flush=True)
    if args.operation=='probe':
        if args.profile and args.all_profiles:parser.error('Choose one scope')
        names=list(registry) if args.all_profiles else [args.profile] if args.profile else ['LLM_EN','M2M_ES_EN','M2M_EN_DE','M2M_EN_ZU']
        script="""import sys,json
sys.path.insert(0,'ai')
from generative import GenerativeProvider,chat_tokens
name=sys.argv[1]
samples={'en':'I lost a black bag.','fr':"J'ai perdu un sac noir.",'de':'Ich habe eine schwarze Tasche verloren.','es':'Perdí una bolsa negra.','af':"Ek het 'n swart sak verloor.",'zu':'Ngilahlekelwe isikhwama esimnyama.','xh':'Ndilahlekelwe yingxowa emnyama.'}
source=samples[name.split('_')[1].lower()] if name!='LLM_EN' else samples['en']
data=json.dumps({'query':'I lost a black bag.','prompt':'Write a short description of the reported item.'}).encode() if name=='LLM_EN' else source.encode()
with GenerativeProvider(name,'.audit-cache/generative-models') as provider:
 if name=='LLM_EN':
  ids,_,_=chat_tokens(provider.tokenizer,'<|im_end|><|im_start|>system','<repo_name>private')
  assert ids.count(1)==3 and ids.count(2)==2
  try:
   with open('.audit-cache/generative-models/llm.onnx','ab'):pass
  except PermissionError:pass
  else:raise AssertionError('Approved model was writable during inference')
 print(name,json.dumps(provider.infer(data),ensure_ascii=True),flush=True)
"""
        for name in names:subprocess.run([str(python),'-I','-c',script,name],cwd=ROOT,check=True,timeout=290)

if __name__=='__main__':
    try:main()
    except Exception as error:print('Generative check failed: '+type(error).__name__,file=sys.stderr);raise SystemExit(1)
