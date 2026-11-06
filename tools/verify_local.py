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
NATIVE_S3_SHA256='f3a9522f4900408d8e230f98516cd9c75fa90da3cfc96dc0fd9fbed380fc8001'


def integration_command(pg_bin=None,maven=None,native_s3=None):
    if bool(pg_bin)!=bool(maven):raise ValueError('Native PostgreSQL requires explicit Maven')
    command=['tools/test_intake.py','--with-ai','--with-perception','--with-vision','--with-language','--with-generative','--with-quality','--with-shona','--with-shona-cpu','--with-supervisor','--with-broker','--with-managed-broker','--with-docker-shona','--restore-drill']
    if pg_bin:command+=['--pg-bin',str(pg_bin),'--maven',str(maven)]
    if native_s3:
        path=Path(native_s3).resolve(strict=True)
        with path.open('rb') as source:digest=hashlib.file_digest(source,'sha256').hexdigest()
        if digest!=NATIVE_S3_SHA256:raise ValueError('Reviewed native S3 executable changed')
        command+=['--native-s3',str(path)]
    return command


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
    parser.add_argument('--native-s3',help='Explicit reviewed SeaweedFS 4.51 executable; digest checked, no automatic fallback')
    args=parser.parse_args()
    if bool(args.pg_bin)!=bool(args.maven):parser.error('--pg-bin and --maven must be provided together')
    if not all(os.environ.get(k) for k in ('DZOKA_TEST_BROKER_BUNDLE','DZOKA_TEST_BROKER_SHA256')):parser.error('Full verification requires an explicitly prepared private broker bundle and pinned receipt')
    if not all(os.environ.get(k) for k in ('DZOKA_TEST_MANAGED_RELEASE','DZOKA_TEST_MANAGED_SHA256')):parser.error('Full verification requires a private management release and receipt')
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
           ('generative-installed-runtime',['tools/generative_runtime.py','runtime-check']),
           ('quality-artifacts',['tools/quality_runtime.py','check']),
           ('shona-cpu-artifacts',['tools/shona_cpu_runtime.py','check']),
           ('shona-cpu-native-probes',['tools/shona_cpu_runtime.py','probe']),
           ('docker-guardian-lifecycle',['tools/verify_docker_guardian.py']),
           ('worker-service-recovery',['tools/verify_worker_service.py']),
           ('offline-boundary-and-archive',['tools/verify_worker_boundary.py']),
           ('isolated-model-broker',['tools/verify_model_broker.py']),
           ('broker-death-and-integrity',['tools/verify_broker_recovery.py']),
           ('managed-broker-operations',['tools/verify_managed_operations.py']),
           ('private-quality-protocols',['tools/verify_quality_protocols.py']),
           ('shona-artifacts',['tools/shona_runtime.py','check']),
           ('shona-installed-runtime',['tools/shona_runtime.py','runtime-check'])]
    integration=integration_command(args.pg_bin,args.maven,args.native_s3)
    steps.append(('real-provider-integration-and-restore',integration))
    run=ROOT/'.test-tmp'/('local-verification-'+datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ'))
    run.mkdir(parents=True)
    result={'startedAt':datetime.now(timezone.utc).isoformat(),'status':'IN_PROGRESS',
            'brokerBundleSha256':os.environ['DZOKA_TEST_BROKER_SHA256'],
            'managedReleaseSha256':os.environ['DZOKA_TEST_MANAGED_SHA256'],
            's3Backend':'reviewed-native-4.51' if args.native_s3 else 'reviewed-docker-4.51',
            'nativeS3Sha256':NATIVE_S3_SHA256 if args.native_s3 else None,
            'inputSha256':source_digest(ROOT),'phases':[],
            'scope':'Implemented foundation/intake/multimodal/generative slice; not full specification or production acceptance'}
    def save():
        (run/'result.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
    save()
    for name,command in steps:
        print('Local verification: '+name,flush=True);start=time.monotonic()
        # Protected broker service starts add real ACL/runtime checks to the
        # expanded integration suite. This bounds the test suite, not a worker.
        deadline=5400 if name=='real-provider-integration-and-restore' else 1800
        try:
            with (run/(name+'.log')).open('w',encoding='utf-8') as log:
                completed=subprocess.run([sys.executable,*command],cwd=ROOT,env=os.environ.copy(),
                                         stdout=log,stderr=subprocess.STDOUT,timeout=deadline)
            code=completed.returncode
        except (OSError,subprocess.TimeoutExpired):code=-1
        result['phases'].append({'name':name,'exitCode':code,'seconds':round(time.monotonic()-start,3),'timeoutSeconds':deadline})
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
