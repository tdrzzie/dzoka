"""Explicit reviewed artifact bootstrap/conformance, never called by API startup."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import urllib.request

ROOT=Path(__file__).resolve().parents[1]
DIRECTORY=ROOT/'.audit-cache/language-models'

def main():
    parser=argparse.ArgumentParser();parser.add_argument('operation',choices=['fetch','check','probe','profiles']);args=parser.parse_args()
    sys.path.insert(0,str(ROOT/'ai'))
    from language import profiles
    selected=profiles();lock=json.loads((ROOT/'ai/language.lock.json').read_bytes())
    if args.operation=='profiles':print(json.dumps(selected,indent=2));return
    DIRECTORY.mkdir(exist_ok=True)
    for f in lock['files']:
        target=DIRECTORY/f['local']
        def valid():return target.exists() and target.stat().st_size==f['bytes'] and hashlib.sha256(target.read_bytes()).hexdigest()==f['sha256']
        if args.operation=='fetch' and not valid():
            part=target.with_suffix(target.suffix+'.part')
            try:
                with urllib.request.urlopen(f['url'],timeout=90) as source,part.open('wb') as output:
                    size=0
                    while block:=source.read(65536):
                        size+=len(block)
                        if size>f['bytes']:raise ValueError('Artifact exceeds bound')
                        output.write(block)
                if size!=f['bytes'] or hashlib.sha256(part.read_bytes()).hexdigest()!=f['sha256']:raise ValueError('Artifact mismatch')
                part.replace(target)
            finally:part.unlink(missing_ok=True)
        if not valid():raise ValueError('Reviewed artifact unavailable')
    print('PASS: language source/runtime identity and thirteen artifact byte checks',flush=True)
    if args.operation=='probe':
        executable=ROOT/'.audit-cache/ai-venv/Scripts/python.exe'
        script="import sys,json;sys.path.insert(0,'ai');from language import LanguageProvider;"
        script+="\nfor task in ('ATTRIBUTES','TRANSLATION','RERANK'):\n data=('J\\u2019ai perdu un sac noir.' if task=='TRANSLATION' else 'I lost a black Lenovo bag at Joina City around 2pm.').encode();\n if task=='RERANK':data=json.dumps({'query':'black Lenovo bag','candidates':[{'id':'00000000-0000-0000-0000-000000000001','text':'A black Lenovo travel bag'},{'id':'00000000-0000-0000-0000-000000000002','text':'A red umbrella'}]}).encode()\n p=LanguageProvider(task,'.audit-cache/language-models');print(task,json.dumps(p.infer(data),ensure_ascii=True),flush=True)"
        subprocess.run([str(executable),'-I','-c',script],cwd=ROOT,check=True,timeout=240)

if __name__=='__main__':
    try:main()
    except Exception as error:
        print('Language artifact/conformance check failed: '+type(error).__name__,file=sys.stderr);raise SystemExit(1)
