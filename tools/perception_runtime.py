"""Explicit reviewed artifact bootstrap/conformance, never called by API startup."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import urllib.request

ROOT=Path(__file__).resolve().parents[1]
DIRECTORY=ROOT/'.audit-cache/perception-models'

def main():
    parser=argparse.ArgumentParser();parser.add_argument('operation',choices=['fetch','check','probe','profiles']);parser.add_argument('--image');parser.add_argument('--audio');args=parser.parse_args()
    sys.path.insert(0,str(ROOT/'ai'))
    from perception import profiles
    selected=profiles();lock=json.loads((ROOT/'ai/perception.lock.json').read_bytes())
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
                    while block:=source.read(1024*1024):
                        size+=len(block)
                        if size>f['bytes']:raise ValueError('Artifact exceeds bound')
                        output.write(block)
                if size!=f['bytes'] or hashlib.sha256(part.read_bytes()).hexdigest()!=f['sha256']:raise ValueError('Artifact mismatch')
                part.replace(target)
            finally:part.unlink(missing_ok=True)
        if not valid():raise ValueError('Reviewed artifact unavailable')
    print('PASS: perception source/runtime identity and six artifact/dictionary byte checks',flush=True)
    if args.operation=='probe':
        if not args.image or not args.audio:parser.error('probe requires --image and --audio synthetic fixture paths')
        executable=ROOT/'.audit-cache/ai-venv/Scripts/python.exe'
        script="import sys,json;from pathlib import Path;sys.path.insert(0,'ai');from perception import PerceptionProvider;" 
        script+="\nfor task in ('VISION','LOCAL_FEATURES','OCR','SPEECH'):\n p=PerceptionProvider(task,'.audit-cache/perception-models');data=Path(sys.argv[2] if task=='SPEECH' else sys.argv[1]).read_bytes();r=p.infer(data);print(task,json.dumps(r,ensure_ascii=True)[:1500],flush=True)"
        subprocess.run([str(executable),'-I','-c',script,args.image,args.audio],cwd=ROOT,check=True,timeout=240)

if __name__=='__main__':
    try:main()
    except Exception as error:
        print('Perception artifact/conformance check failed: '+type(error).__name__,file=sys.stderr);raise SystemExit(1)
