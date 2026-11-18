"""Exercise the actual forward migrations against a populated V12 ledger."""
import hashlib
import json
import os
from pathlib import Path
import secrets
import subprocess
import sys
import time
import uuid

ROOT = Path(__file__).resolve().parents[4]
sys.path.insert(0, str(ROOT / 'tools'))
from test_foundation_container import PGVECTOR_DIAGNOSTIC

TENANT, REPORT, RUN = (f'00000000-0000-0000-0000-00000000000{i}' for i in range(1,4))
ACTOR = hashlib.sha256(b'https://upgrade.invalid\0matcher').hexdigest()

SEED = f"""
INSERT INTO dzoka.tenants(id,name) VALUES ('{TENANT}','Synthetic upgrade');
INSERT INTO dzoka.memberships(tenant_id,issuer,subject,permissions)
  VALUES ('{TENANT}','https://upgrade.invalid','matcher',ARRAY['matches:read','reports:read']);
INSERT INTO dzoka.recovery_cases(tenant_id,id) VALUES ('{TENANT}','{REPORT}');
INSERT INTO dzoka.items(tenant_id,id,case_id) VALUES ('{TENANT}','{REPORT}','{REPORT}');
INSERT INTO dzoka.reports(tenant_id,id,case_id,item_id,owner_ref,kind,current_revision,expires_at)
  VALUES ('{TENANT}','{REPORT}','{REPORT}','{REPORT}','{ACTOR}','LOST',1,now()+interval '1 day');
INSERT INTO dzoka.report_revisions(tenant_id,report_id,revision,raw_request,raw_sha256,normalization_version,language,timezone,actor_ref)
  VALUES ('{TENANT}','{REPORT}',1,''::bytea,repeat('a',64),'fixture','en','UTC','{ACTOR}');
INSERT INTO dzoka.match_search_runs(tenant_id,id,actor_ref,report_id,revision,requested_limit,candidate_count,summary)
  VALUES ('{TENANT}','{RUN}','{ACTOR}','{REPORT}',1,10,0,'{{"scoringVersion":"bounded-evidence-v1"}}');
"""

CHECK = f"""
BEGIN;
SET LOCAL ROLE dzoka_app;
SET LOCAL dzoka.tenant_id='{TENANT}';
SET LOCAL dzoka.issuer='https://upgrade.invalid';
SET LOCAL dzoka.subject='matcher';
SELECT json_build_object('clientIdPreserved', r.client_id=r.id, 'quotaRestored', q.id=r.id,
  'payloadPreserved',r.summary->>'scoringVersion'='bounded-evidence-v1', 'categoryAbsent',r.requested_category IS NULL)
FROM dzoka.match_search_runs r JOIN dzoka.match_search_quota q ON (q.tenant_id,q.id)=(r.tenant_id,r.id)
WHERE r.client_id='{RUN}';
SET LOCAL dzoka.subject='other';
SELECT count(*) FROM dzoka.match_search_runs;
ROLLBACK;
"""


def main():
    name = 'dzoka-m5-upgrade-'+uuid.uuid4().hex
    password = secrets.token_urlsafe(32)
    env = {**os.environ,'POSTGRES_PASSWORD':password,'PGPASSWORD':password,'PGCLIENTENCODING':'UTF8'}

    def docker(*args, **options):
        return subprocess.run(['docker',*args],env=env,text=True,encoding='utf-8',capture_output=True,
                              timeout=180,check=True,**options).stdout

    def sql(body):
        return docker('exec','-i','--env','PGPASSWORD','--env','PGCLIENTENCODING',name,'psql',
                      '-h','127.0.0.1','-U','postgres','-X','-qAt','-v','ON_ERROR_STOP=1',input=body)

    try:
        docker('run','--detach','--pull','never','--network','none','--name',name,
               '--env','POSTGRES_PASSWORD',PGVECTOR_DIAGNOSTIC)
        for _ in range(80):
            ready = subprocess.run(['docker','exec',name,'pg_isready','-U','postgres','-h','127.0.0.1'],
                                   capture_output=True,timeout=4)
            if ready.returncode==0:
                break
            time.sleep(0.5)
        else:
            raise RuntimeError('Disposable PostgreSQL unavailable')
        sql('CREATE ROLE dzoka_app NOBYPASSRLS; CREATE ROLE dzoka_migrator NOBYPASSRLS; '
            'ALTER SCHEMA public OWNER TO dzoka_migrator; ALTER DATABASE postgres OWNER TO dzoka_migrator;')
        migrations = sorted((ROOT/'src/main/resources/db/migration').glob('V*__*.sql'),
                            key=lambda path:int(path.name.split('__')[0][1:]))
        for migration in migrations:
            version = int(migration.name.split('__')[0][1:])
            if version>16:
                break
            sql('BEGIN; SET LOCAL ROLE dzoka_migrator;\n'+migration.read_text(encoding='utf-8')+'\nCOMMIT;')
            if version==12:
                sql(SEED)
        payload, inaccessible = sql(CHECK).splitlines()
        result = json.loads(payload)
        if not all(result.values()) or inaccessible!='0':
            raise RuntimeError('Legacy matching history failed migration or isolation')
        result.update(status='PASS',scope='SYNTHETIC_V12_TO_V16_SQL',differentActorDenied=True,
                      migrations={path.name:hashlib.sha256(path.read_bytes()).hexdigest()
                                  for path in migrations[:16]})
        print(json.dumps(result,indent=2))
    finally:
        removed = subprocess.run(['docker','rm','--force','--volumes',name],env=env,capture_output=True,timeout=60)
        if removed.returncode!=0 and b'No such container' not in removed.stderr:
            raise RuntimeError('Disposable upgrade database cleanup needs attention')


if __name__=='__main__':
    main()
