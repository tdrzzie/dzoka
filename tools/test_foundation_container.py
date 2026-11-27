"""Disposable, digest-pinned PostgreSQL for CI/developers with Docker. No host mounts."""
import json
import argparse
import os
from pathlib import Path
import secrets
import subprocess
import sys
import time
import uuid

ROOT = Path(__file__).resolve().parents[1]
IMAGE = 'postgres@sha256:9770878138a2e83abcb10856d30cc3f24dae0d317bb956db6ab9159de6c3c80d'
PGVECTOR_DIAGNOSTIC = 'pgvector/pgvector@sha256:a5b06db72c44033b10fde7f303aad2a543e87a314dd6c2a680848067defd3760'

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--full',action='store_true')
    parser.add_argument('--restore-drill',action='store_true')
    parser.add_argument('--with-pgvector',action='store_true',help='Use the pinned disposable matching conformance image')
    parser.add_argument('--matching-index',action='store_true',help='Exercise the actual authoritative expression index')
    args=parser.parse_args()
    if args.matching_index and not args.with_pgvector:parser.error('--matching-index requires --with-pgvector')
    name='dzoka-foundation-'+str(uuid.uuid4())
    password,app_password=secrets.token_urlsafe(32),secrets.token_urlsafe(32)
    migration_password=secrets.token_urlsafe(32)
    env=os.environ.copy()
    env.update(POSTGRES_PASSWORD=password,POSTGRES_USER='dzoka_control',POSTGRES_DB='postgres',
               POSTGRES_INITDB_ARGS='--auth-host=scram-sha-256 --auth-local=scram-sha-256',PGPASSWORD=password,PGCLIENTENCODING='UTF8',
               DZOKA_TEST_ADMIN_USER='dzoka_control',DZOKA_TEST_ADMIN_PASSWORD=password,
               DZOKA_TEST_APP_PASSWORD=app_password,DZOKA_TEST_MIGRATION_USER='dzoka_migrator',
               DZOKA_TEST_MIGRATION_PASSWORD=migration_password)
    def docker(*args,**kwargs):
        return subprocess.run(['docker',*args],env=env,cwd=ROOT,check=True,capture_output=True,
                              text=True,encoding='utf-8',timeout=180,**kwargs)
    created=False
    try:
        # No persistent volumes or user directories are mounted. Only loopback is published.
        created=True
        docker('run','--detach','--name',name,'--publish','127.0.0.1::5432',
               '--env','POSTGRES_PASSWORD','--env','POSTGRES_USER','--env','POSTGRES_DB',
               '--env','POSTGRES_INITDB_ARGS',PGVECTOR_DIAGNOSTIC if args.with_pgvector else IMAGE)
        for _ in range(60):
            ready=subprocess.run(['docker','exec',name,'pg_isready','-h','127.0.0.1','-U','dzoka_control'],
                                 env=env,capture_output=True,timeout=10)
            if ready.returncode==0:break
            time.sleep(0.5)
        else:raise RuntimeError('Test database readiness timeout')
        binding=json.loads(docker('inspect',name).stdout)[0]['NetworkSettings']['Ports']['5432/tcp'][0]
        env['DZOKA_TEST_ADMIN_URL']='jdbc:postgresql://127.0.0.1:'+binding['HostPort']+'/dzoka_test'
        docker('exec','-i','--env','PGPASSWORD',name,'psql','-h','127.0.0.1','-U','dzoka_control','-d','postgres',
               '-v','ON_ERROR_STOP=1',input=f"CREATE ROLE dzoka_app LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOBYPASSRLS PASSWORD '{app_password}';\n"
               f"CREATE ROLE dzoka_migrator LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOBYPASSRLS PASSWORD '{migration_password}';\n"
               "CREATE DATABASE dzoka_test OWNER dzoka_migrator;\n")
        if args.with_pgvector:
            docker('exec','-i','--env','PGPASSWORD',name,'psql','-h','127.0.0.1','-U','dzoka_control','-d','dzoka_test',
                   '-v','ON_ERROR_STOP=1',input="CREATE EXTENSION vector WITH SCHEMA public VERSION '0.8.7';\n")
            env.update(DZOKA_TEST_MATCHING_BACKEND='pgvector',DZOKA_TEST_MATCHING_ROUTE='PGVECTOR_EXACT')
            if args.matching_index:
                env.update(DZOKA_TEST_MATCHING_BACKEND='pgvector-indexed',DZOKA_TEST_MATCHING_ROUTE='PGVECTOR_HNSW')
            result=docker('exec','-i','--env','PGPASSWORD',name,'psql','-X','-qAt','-h','127.0.0.1',
                          '-U','dzoka_control','-d','dzoka_test','-v','ON_ERROR_STOP=1',
                          input=(ROOT/'tools/m5_pgvector_recall.sql').read_text(encoding='utf-8'))
            recall=json.loads(result.stdout)
            if (recall['exactCount'],recall['indexCount'],recall['overlap'],recall['filterViolations'])!=(10,10,10,0):
                raise RuntimeError('Scoped HNSW recall fixture differs from exact baseline')
            if len(recall['additionalQueries'])!=9 or sum(
                    query['category'] is not None for query in recall['additionalQueries'])!=3 or any(
                    not x['scoped'] or x['found']!=x['k'] or x['overlap']!=x['k'] for x in recall['additionalQueries']):
                raise RuntimeError('Multi-query filtered recall differs from exact baseline')
            output=ROOT/'.test-tmp'/('pgvector-recall-'+str(uuid.uuid4())+'.json')
            output.parent.mkdir(exist_ok=True)
            output.write_text(json.dumps(recall,indent=2)+'\n',encoding='utf-8')
            print('Synthetic pgvector recall evidence: '+str(output),flush=True)
        print('Digest-pinned disposable PostgreSQL ready; random test credentials, loopback only.',flush=True)
        maven=[sys.executable,str(ROOT/'tools/maven.py'),'verify']
        if not args.full:maven.append('-Dit.test=FoundationIT,FoundationAdministrationIT,PlacesIT,IntakeIT,MediaStoreIT,AiJobsIT,FingerprintRolloutsIT,MatchingIT,RecoveryIT,ChannelsIT,FederationIT,TraceabilityIT,EdgeIT,PerceptionJobsIT,VisionJobsIT,LanguageJobsIT,GenerativeJobsIT,QualityJobsIT,ShonaJobsIT,StartupConfigurationIT')
        elif env.get('DZOKA_IT_CLASSES'):maven.append('-Dit.test='+env['DZOKA_IT_CLASSES'])
        result=subprocess.run(maven,cwd=ROOT,env=env)
        if result.returncode==0 and args.restore_drill:
            from restore_drill import drill
            def command(tool,*arguments,**kwargs):
                return docker('exec','-i','--env','PGPASSWORD','--env','PGCLIENTENCODING',name,tool,*arguments,**kwargs)
            evidence=drill(command,'/tmp/synthetic-backup.dump')
            output=ROOT/'.test-tmp'/('restore-'+str(uuid.uuid4())+'.json')
            output.parent.mkdir(exist_ok=True)
            output.write_text(json.dumps(evidence,indent=2)+'\n',encoding='utf-8')
            print('Synthetic restore evidence: '+str(output),flush=True)
        return result.returncode
    except (RuntimeError,subprocess.SubprocessError) as error:
        print('Container integration runner failed: '+type(error).__name__,file=sys.stderr)
        if isinstance(error,subprocess.CalledProcessError):print(error.stderr or '',file=sys.stderr)
        return 1
    finally:
        if created:
            cleanup=subprocess.run(['docker','rm','--force','--volumes',name],env=env,
                                   capture_output=True,text=True,timeout=60)
            if cleanup.returncode==0:
                print('Task-owned test container and its anonymous volumes removed.',flush=True)
            elif 'No such container' not in cleanup.stderr:
                print('Test container cleanup needs attention: '+name,file=sys.stderr)

if __name__=='__main__':raise SystemExit(main())
