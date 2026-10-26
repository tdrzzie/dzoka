"""Explicit bootstrap/checks for the reviewed Windows CPython inference profile; never runs at API startup."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import urllib.request
import venv

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'ai'))
from provider import load_profile,ProviderUnavailable

def download(url,path,digest,expected=None):
    if path.is_file() and hashlib.sha256(path.read_bytes()).hexdigest()==digest:return
    path.parent.mkdir(parents=True,exist_ok=True);partial=path.with_name(path.name+'.part')
    try:
        with urllib.request.urlopen(url,timeout=60) as source,partial.open('wb') as target:
            size=0
            while chunk:=source.read(1024*1024):
                size+=len(chunk)
                if size>(expected or 150_000_000):raise ValueError('Artifact exceeds bound')
                target.write(chunk)
        if expected is not None and partial.stat().st_size!=expected:raise ValueError('Artifact size mismatch')
        if hashlib.sha256(partial.read_bytes()).hexdigest()!=digest:raise ValueError('Artifact checksum mismatch')
        partial.replace(path)
    finally:partial.unlink(missing_ok=True)

def check():
    profile=load_profile();runtime=json.loads((ROOT/'ai/runtime.lock.json').read_text(encoding='utf-8'))
    for wheel in runtime['wheels']:
        path=ROOT/'.audit-cache/ai-wheels'/wheel['wheel']
        if hashlib.sha256(path.read_bytes()).hexdigest()!=wheel['sha256']:raise ValueError('Wheel checksum mismatch')
    for file in profile['files']:
        path=ROOT/'.audit-cache/ai-model'/file['local']
        if path.stat().st_size!=file['bytes'] or hashlib.sha256(path.read_bytes()).hexdigest()!=file['sha256']:raise ValueError('Model checksum mismatch')
    print('PASS: reviewed source/profile, 21 wheels and model/tokenizer bytes')

def main():
    p=argparse.ArgumentParser();p.add_argument('operation',choices=['fetch','install','check','probe']);args=p.parse_args()
    profile=load_profile();runtime=json.loads((ROOT/'ai/runtime.lock.json').read_text(encoding='utf-8'))
    if args.operation in ('fetch','install') and (os.name!='nt' or sys.version_info[:2]!=(3,14)):
        raise ValueError('This native-wheel profile is reviewed for Windows CPython 3.14; review another platform explicitly')
    if args.operation=='fetch':
        for wheel in runtime['wheels']:
            download(wheel['url'],ROOT/'.audit-cache/ai-wheels'/wheel['wheel'],wheel['sha256'])
        for file in profile['files']:
            url='https://huggingface.co/'+profile['space']['model']+'/resolve/'+profile['space']['artifactRevision']+'/'+file['path']
            download(url,ROOT/'.audit-cache/ai-model'/file['local'],file['sha256'],file['bytes'])
    check()
    if args.operation=='install':
        destination=ROOT/'.audit-cache/ai-venv'
        venv.EnvBuilder(with_pip=True).create(destination)
        subprocess.run([str(destination/'Scripts/python.exe'),'-m','pip','install','--no-index','--find-links',str(ROOT/'.audit-cache/ai-wheels'),
            '--require-hashes','--only-binary=:all:','-r',str(ROOT/'ai/requirements.lock.txt')],check=True)
    if args.operation=='probe':
        binary=ROOT/'.audit-cache/ai-venv'/('Scripts/python.exe' if os.name=='nt' else 'bin/python')
        script="import sys;sys.path.insert(0,'ai');from provider import TextEmbeddingProvider;p=TextEmbeddingProvider('.audit-cache/ai-model');a=p.embed('Synthetic blue bag');b=p.embed('Un sac bleu');assert len(a['vector'])==384 and a['vector']!=b['vector'];print('PASS: real nonconstant multilingual-profile inference (not an accuracy benchmark)')"
        subprocess.run([str(binary),'-I','-c',script],cwd=ROOT,check=True)

if __name__=='__main__':
    try:main()
    except Exception:
        print('AI runtime check/bootstrap failed; review artifact/runtime configuration',file=sys.stderr);raise SystemExit(1)
