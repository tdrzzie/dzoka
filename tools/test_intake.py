"""Real private-S3 plus PostgreSQL integration harness. Full mode also requires Docker raster sandbox."""
import argparse
import hashlib
import http.client
import json
import os
from pathlib import Path
import secrets
import socket
import subprocess
import sys
import time
import urllib.error
import urllib.request
import uuid

ROOT=Path(__file__).resolve().parents[1]
S3_IMAGE='chrislusf/seaweedfs@sha256:3a522c379599e163433a636e75022630848b63a74f08ea37bf06f8d776e9a707'
RASTER_IMAGE='eclipse-temurin@sha256:da0f0d242bec39e3fe5c1ac4c415840f03be9cef848c243d723166708e02148c'

def free_ports(count):
    sockets=[]
    try:
        for _ in range(count):
            s=socket.socket();s.bind(('127.0.0.1',0));sockets.append(s)
        return [s.getsockname()[1] for s in sockets]
    finally:
        for s in sockets:s.close()

def main():
    p=argparse.ArgumentParser();p.add_argument('--native-s3',help='Reviewed native weed executable')
    p.add_argument('--pg-bin');p.add_argument('--maven');p.add_argument('--storage-only',action='store_true',help='Explicitly excludes Docker sandbox tests; never a production decoder fallback')
    p.add_argument('--with-ai',action='store_true',help='Also run the reviewed native Python/model provider; requires its installed artifact lock')
    p.add_argument('--with-perception',action='store_true',help='Also run licensed native vision/local/OCR/speech profiles and synthetic voice fixture')
    p.add_argument('--with-vision',action='store_true',help='Run reviewed joint image/text, detection and semantic segmentation models')
    p.add_argument('--with-language',action='store_true',help='Run reviewed native extraction, translation and cross-encoder reranking')
    p.add_argument('--with-shona',action='store_true',help='Run reviewed local-development bidirectional Shona/explicit-copy providers')
    p.add_argument('--with-quality',action='store_true',help='Run reviewed larger draft and dedicated Shona/Spanish/German translators')
    p.add_argument('--with-generative',action='store_true',help='Run reviewed general draft and multilingual profiles in the separate native runtime')
    p.add_argument('--restore-drill',action='store_true',help='Verify database logical restore after the full passing suite')
    args=p.parse_args();run=ROOT/'.test-tmp'/('intake-'+str(uuid.uuid4()));run.mkdir(parents=True)
    access,secret=secrets.token_hex(12),secrets.token_urlsafe(32)
    credentials=run/'s3.json';credentials.write_text(json.dumps({'identities':[{'name':'synthetic-test','credentials':[{'accessKey':access,'secretKey':secret}],
        'actions':['Admin','Read','Write','List','Tagging']}]}))
    if os.name!='nt':credentials.chmod(0o600)
    env=os.environ.copy();env.update(DZOKA_TEST_S3_ACCESS_KEY=access,DZOKA_TEST_S3_SECRET_KEY=secret)
    container='dzoka-intake-s3-'+str(uuid.uuid4());child=None;created=False;log=None
    def docker(*a,**kw):return subprocess.run(['docker',*a],env=env,cwd=ROOT,check=True,capture_output=True,text=True,timeout=180,**kw)
    def pull(image):
        # Cold image downloads need a separate bounded deadline and visible progress.
        subprocess.run(['docker','pull',image],env=env,cwd=ROOT,check=True,timeout=600)
    try:
        if not args.storage_only:pull(RASTER_IMAGE)
        if not args.native_s3:pull(S3_IMAGE)
        ports=free_ports(10)
        # Each fixture bucket can allocate its own collection volumes. Keep the
        # disposable capacity bounded while allowing all four provider fixtures.
        flags=['mini','-master.telemetry=false','-s3.port.iceberg=0','-s3.port.lance=0','-admin.port.legacy=0','-admin.ui=false','-volume.max=16','-master.volumeSizeLimitMB=32']
        if args.native_s3:
            binary=Path(args.native_s3).resolve()
            # Reviewed archive hash is checked by bootstrap/operator before executable selection.
            for name,port in zip(['master.port','master.port.grpc','volume.port','volume.port.grpc','filer.port','filer.port.grpc','s3.port','s3.port.grpc','admin.port','admin.port.grpc'],ports):flags.append(f'-{name}={port}')
            flags+=['-ip=127.0.0.1','-ip.bind=127.0.0.1',f'-dir={run}',f'-s3.config={credentials}']
            log=(run/'s3.log').open('w');child=subprocess.Popen([str(binary),*flags],cwd=run,env=env,stdout=log,stderr=subprocess.STDOUT)
            endpoint='http://127.0.0.1:'+str(ports[6])
        else:
            created=True
            docker('run','--detach','--name',container,'--publish','127.0.0.1::8333',
                '--mount',f'type=bind,src={credentials},dst=/config/s3.json,readonly',S3_IMAGE,
                *flags,'-ip=127.0.0.1','-dir=/data','-s3.config=/config/s3.json')
            binding=json.loads(docker('inspect',container).stdout)[0]['NetworkSettings']['Ports']['8333/tcp'][0]
            endpoint='http://127.0.0.1:'+binding['HostPort']
        for _ in range(120):
            try:
                urllib.request.urlopen(endpoint,timeout=1).close()
                raise RuntimeError('S3 incorrectly permits anonymous access')
            except urllib.error.HTTPError as e:
                if e.code==403:break
                if e.code not in (500,503):raise
            except (urllib.error.URLError,TimeoutError,ConnectionError,http.client.RemoteDisconnected):pass
            if child is not None and child.poll() is not None:raise RuntimeError('Native S3 stopped; inspect ignored test log')
            time.sleep(0.5)
        else:raise RuntimeError('Authenticated S3 readiness timeout')
        env['DZOKA_TEST_S3_ENDPOINT']=endpoint
        classes=['FoundationIT','IntakeIT','MediaStoreIT','S3StorageIT','AiJobsIT','FingerprintRolloutsIT','PerceptionJobsIT','VisionJobsIT','LanguageJobsIT','GenerativeJobsIT','QualityJobsIT','ShonaJobsIT','StartupConfigurationIT']
        if not args.storage_only:classes.append('SandboxMediaIT')
        if args.with_ai:
            subprocess.run([sys.executable,str(ROOT/'tools/ai_runtime.py'),'check'],cwd=ROOT,env=env,check=True)
            classes.append('AiProviderIT')
        else:print('Explicit core/media/AI-contract suite: native model inference requires --with-ai.',flush=True)
        if args.with_perception:
            if args.storage_only:raise ValueError('Native image providers require the real sanitizer, not storage-only mode')
            subprocess.run([sys.executable,str(ROOT/'tools/perception_runtime.py'),'check'],cwd=ROOT,env=env,check=True)
            audio=run/'synthetic-speech.wav'
            subprocess.run([sys.executable,str(ROOT/'tools/perception_fixture.py'),'--output',str(audio)],cwd=ROOT,env=env,check=True)
            env['DZOKA_TEST_SPEECH_WAV']=str(audio);classes.append('PerceptionProviderIT')
        else:print('Explicit portable perception lifecycle scope; native vision/OCR/speech require --with-perception.',flush=True)
        if args.with_vision:
            if args.storage_only:raise ValueError('Vision models require real image sanitizer')
            subprocess.run([sys.executable,str(ROOT/'tools/vision_runtime.py'),'check'],cwd=ROOT,env=env,check=True)
            classes.append('VisionProviderIT')
        if args.with_language:
            subprocess.run([sys.executable,str(ROOT/'tools/language_runtime.py'),'check'],cwd=ROOT,env=env,check=True)
            classes.append('LanguageProviderIT')
        if args.with_shona:
            subprocess.run([sys.executable,str(ROOT/'tools/shona_runtime.py'),'check'],cwd=ROOT,env=env,check=True)
            subprocess.run([sys.executable,str(ROOT/'tools/shona_runtime.py'),'runtime-check'],cwd=ROOT,env=env,check=True)
            classes.append('ShonaProviderIT')
        else:print('Portable Shona lifecycle scope; native inference requires --with-shona and the reviewed local-development runtime.',flush=True)
        if args.with_quality:
            subprocess.run([sys.executable,str(ROOT/'tools/quality_runtime.py'),'check'],cwd=ROOT,env=env,check=True)
            subprocess.run([sys.executable,str(ROOT/'tools/generative_runtime.py'),'runtime-check'],cwd=ROOT,env=env,check=True)
            classes.append('QualityProviderIT')
        else:print('Portable quality lifecycle scope; native alternatives require --with-quality.',flush=True)
        if args.with_generative:
            subprocess.run([sys.executable,str(ROOT/'tools/generative_runtime.py'),'check'],cwd=ROOT,env=env,check=True)
            subprocess.run([sys.executable,str(ROOT/'tools/generative_runtime.py'),'runtime-check'],cwd=ROOT,env=env,check=True)
            classes.append('GenerativeProviderIT')
        else:print('Portable generative lifecycle scope; native general/multilingual inference requires --with-generative.',flush=True)
        env['DZOKA_IT_CLASSES']=','.join(classes)
        if args.storage_only:
            print('Explicit storage-only verification: Docker decoder tests excluded, no decoder fallback.',flush=True)
        command=[sys.executable,str(ROOT/('tools/test_foundation.py' if args.pg_bin else 'tools/test_foundation_container.py')),'--full']
        if args.restore_drill:command.append('--restore-drill')
        if args.pg_bin:
            if not args.maven:raise RuntimeError('--maven is required with --pg-bin')
            command+=['--pg-bin',args.pg_bin,'--maven',args.maven]
        return subprocess.run(command,cwd=ROOT,env=env).returncode
    finally:
        if child is not None:
            child.terminate()
            try:child.wait(timeout=15)
            except subprocess.TimeoutExpired:child.kill();child.wait(timeout=5)
        if log is not None:log.close()
        if created:
            # Private synthetic diagnostics stay ignored; never print credentials.
            try:
                with (run/'s3.log').open('w',encoding='utf-8') as output:
                    subprocess.run(['docker','logs',container],stdout=output,stderr=subprocess.STDOUT,timeout=30)
            finally:subprocess.run(['docker','rm','--force','--volumes',container],capture_output=True,timeout=60)
        credentials.unlink(missing_ok=True)

if __name__=='__main__':raise SystemExit(main())
