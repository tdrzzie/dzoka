"""One explicit local evidence run. Does not claim whole-platform readiness.

No bootstrap/download/install step. The full real-provider run needs the reviewed
cached dependencies/images/native model. Results include source-input hashes;
nonzero/missing phases fail the run rather than silently becoming a narrower suite.
"""
import argparse
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import time

ROOT=Path(__file__).resolve().parents[1]


def source_digest(root):
    paths=[]
    for directory in ('src','ai','tools','tests'):
        paths.extend(p for p in (root/directory).rglob('*') if p.is_file()
                     and '__pycache__' not in p.parts and p.suffix not in ('.pyc','.pyo'))
    for name in ('pom.xml','dependencies.lock.json','AGENTS.md'):
        paths.append(root/name)
    digest=hashlib.sha256()
    for path in sorted(paths):
        digest.update(path.relative_to(root).as_posix().encode()+b'\0')
        digest.update(hashlib.sha256(path.read_bytes()).digest())
    return digest.hexdigest()


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--pg-bin',help='Optional reviewed native PostgreSQL binaries; otherwise Docker')
    parser.add_argument('--maven',help='Required with --pg-bin')
    args=parser.parse_args()
    if bool(args.pg_bin)!=bool(args.maven):parser.error('--pg-bin and --maven must be provided together')
    steps=[('source-gate',['tools/audit_registry.py','gate']),
           ('architecture',['tools/verify_architecture.py']),
           ('python-regressions',['-m','unittest','discover','-s','tests','-q']),
           ('java-artifact-lock',['tools/dependency_lock.py']),
           ('native-model-artifacts',['tools/ai_runtime.py','check']),
           ('native-model-conformance',['tools/ai_runtime.py','probe']),
           ('perception-artifacts',['tools/perception_runtime.py','check']),
           ('joint-region-artifacts',['tools/vision_runtime.py','check']),
           ('language-artifacts',['tools/language_runtime.py','check']),
           ('generative-artifacts',['tools/generative_runtime.py','check']),
           ('generative-installed-runtime',['tools/generative_runtime.py','runtime-check'])]
    integration=['tools/test_intake.py','--with-ai','--with-perception','--with-vision','--with-language','--with-generative','--restore-drill']
    if args.pg_bin:integration+=['--pg-bin',args.pg_bin,'--maven',args.maven]
    steps.append(('real-provider-integration-and-restore',integration))
    run=ROOT/'.test-tmp'/('local-verification-'+datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ'))
    run.mkdir(parents=True)
    result={'startedAt':datetime.now(timezone.utc).isoformat(),'status':'IN_PROGRESS',
            'inputSha256':source_digest(ROOT),'phases':[],
            'scope':'Implemented foundation/intake/multimodal/generative slice; not full specification or production acceptance'}
    def save():
        (run/'result.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
    save()
    for name,command in steps:
        print('Local verification: '+name,flush=True);start=time.monotonic()
        try:
            with (run/(name+'.log')).open('w',encoding='utf-8') as log:
                completed=subprocess.run([sys.executable,*command],cwd=ROOT,env=os.environ.copy(),
                                         stdout=log,stderr=subprocess.STDOUT,timeout=1800)
            code=completed.returncode
        except (OSError,subprocess.TimeoutExpired):code=-1
        result['phases'].append({'name':name,'exitCode':code,'seconds':round(time.monotonic()-start,3)})
        if code:
            result['status']='FAIL';save()
            print('FAIL: '+name+'; inspect ignored log at '+str(run/(name+'.log')),flush=True)
            return 1
        save()
    if source_digest(ROOT)!=result['inputSha256']:
        result['status']='FAIL';result['reason']='Source inputs changed during verification';save();return 1
    result.update(status='PASS',finishedAt=datetime.now(timezone.utc).isoformat());save()
    print('PASS: all explicit local phases; evidence '+str(run/'result.json'),flush=True)
    return 0


if __name__=='__main__':raise SystemExit(main())
