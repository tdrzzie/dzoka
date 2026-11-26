"""Offline logical-restore drill for harness-owned synthetic databases only.

The caller supplies reviewed PostgreSQL commands in its disposable cluster. No
production connection option, role dump, external transport or service startup.
"""
import hashlib
import json
import re
import uuid
from pathlib import Path
import shutil


def require_equal(before, after, label):
    if before != after:
        raise RuntimeError('Restore verification differs: ' + label)


def identifier(value):
    if not re.fullmatch(r'[a-z_][a-z_0-9]*', value):
        raise ValueError('Unexpected test database identifier')
    return '"' + value + '"'


def drill(command, archive):
    """command(tool, *args, input=None) must capture output, never echo SQL/data.

    Run only after successful integration tests, with all API/workers stopped.
    archive is an ignored native path or a path inside the disposable container.
    """
    source = 'dzoka_test'
    restored = 'dzoka_restore_' + uuid.uuid4().hex

    def sql(database, query, role='dzoka_control'):
        return command('psql', '-X', '-q', '-A', '-t', '-h', '127.0.0.1',
                       '-U', 'dzoka_control', '-d', database, '-v', 'ON_ERROR_STOP=1',
                       input=('SET ROLE ' + identifier(role) + ';\n' + query)).stdout.strip()

    def snapshot(database):
        tables = json.loads(sql(database, "SELECT coalesce(json_agg(tablename ORDER BY tablename),'[]') "
                                "FROM pg_tables WHERE schemaname IN ('dzoka','public');"))
        hashes = {}
        # Include Flyway's public history and all domain/worker/event rows. Never
        # persist rows; only hashes/counts enter the diagnostic result.
        for schema in ('dzoka', 'public'):
            names = json.loads(sql(database, "SELECT coalesce(json_agg(tablename ORDER BY tablename),'[]') "
                                   f"FROM pg_tables WHERE schemaname='{schema}';"))
            for name in names:
                rows = sql(database, f'SELECT coalesce(jsonb_agg(to_jsonb(t) ORDER BY to_jsonb(t)::text), '
                           f"'[]'::jsonb) FROM {identifier(schema)}.{identifier(name)} t;")
                hashes[schema + '.' + name] = {'sha256': hashlib.sha256(rows.encode()).hexdigest(),
                                             'rows': len(json.loads(rows))}
        if not tables or not hashes.get('public.flyway_schema_history', {}).get('rows'):
            raise RuntimeError('Restore drill requires a migrated, populated test database')
        catalog = sql(database, """
SELECT jsonb_build_object(
 'tables',(SELECT jsonb_agg(to_jsonb(x) ORDER BY x.relname) FROM
   (SELECT c.relname,pg_get_userbyid(c.relowner) owner,c.relrowsecurity,c.relforcerowsecurity,c.relacl
    FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
    WHERE n.nspname='dzoka' AND c.relkind IN ('r','p')) x),
 'policies',(SELECT jsonb_agg(to_jsonb(x) ORDER BY x.tablename,x.policyname) FROM
   (SELECT * FROM pg_policies WHERE schemaname='dzoka') x),
 'constraints',(SELECT jsonb_agg(to_jsonb(x) ORDER BY x.table_name,x.conname) FROM
   (SELECT c.relname table_name,k.conname,pg_get_constraintdef(k.oid) definition,k.convalidated
    FROM pg_constraint k JOIN pg_class c ON c.oid=k.conrelid
    JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='dzoka') x),
 'indexes',(SELECT jsonb_agg(to_jsonb(x) ORDER BY x.relname) FROM
   (SELECT c.relname,pg_get_userbyid(c.relowner) owner,pg_get_indexdef(c.oid) definition,i.indisready,i.indisvalid
    FROM pg_index i JOIN pg_class c ON c.oid=i.indexrelid
    JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='dzoka') x),
 'triggers',(SELECT jsonb_agg(to_jsonb(x) ORDER BY x.table_name,x.trigger_name) FROM
   (SELECT c.relname table_name,t.tgname trigger_name,t.tgenabled,pg_get_triggerdef(t.oid) definition
    FROM pg_trigger t JOIN pg_class c ON c.oid=t.tgrelid JOIN pg_namespace n ON n.oid=c.relnamespace
    WHERE n.nspname='dzoka' AND NOT t.tgisinternal) x),
 'extensions',(SELECT jsonb_agg(to_jsonb(x) ORDER BY x.extname) FROM
   (SELECT e.extname,e.extversion,n.nspname FROM pg_extension e JOIN pg_namespace n ON n.oid=e.extnamespace) x),
 'functions',(SELECT jsonb_agg(to_jsonb(x) ORDER BY x.signature) FROM
   (SELECT p.oid::regprocedure::text signature,pg_get_userbyid(p.proowner) owner,p.proacl,
           pg_get_functiondef(p.oid) definition
    FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace WHERE n.nspname='dzoka') x),
 'views',(SELECT jsonb_agg(to_jsonb(x) ORDER BY x.relname) FROM
   (SELECT c.relname,pg_get_userbyid(c.relowner) owner,c.relacl,c.reloptions,pg_get_viewdef(c.oid,true) definition
    FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='dzoka' AND c.relkind='v') x),
 'columns',(SELECT jsonb_agg(to_jsonb(x) ORDER BY x.relname,x.attname) FROM
   (SELECT c.relname,a.attname,a.attacl FROM pg_attribute a JOIN pg_class c ON c.oid=a.attrelid
    JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='dzoka' AND a.attnum>0 AND NOT a.attisdropped) x)
);""")
        return {'tables': hashes, 'catalogSha256': hashlib.sha256(catalog.encode()).hexdigest()}, json.loads(catalog)

    # Explicit fixtures prove restored authorization, rather than relying on
    # whichever fixtures happen to survive the application integration suite.
    a, b, org_a, org_b = (str(uuid.uuid4()) for _ in range(4))
    sql(source, f"""
BEGIN;
INSERT INTO dzoka.tenants(id,name) VALUES ('{a}','Synthetic restore A'),('{b}','Synthetic restore B');
INSERT INTO dzoka.memberships(tenant_id,issuer,subject,permissions) VALUES
 ('{a}','https://restore.invalid','alice',ARRAY['organizations:read','organizations:write']),
 ('{b}','https://restore.invalid','bob',ARRAY['organizations:read','organizations:write']);
INSERT INTO dzoka.organizations(tenant_id,id,name,version) VALUES
 ('{a}','{org_a}','Synthetic restore A',1),('{b}','{org_b}','Synthetic restore B',1);
COMMIT;
""")
    before, source_catalog = snapshot(source)
    command('pg_dump', '-h', '127.0.0.1', '-U', 'dzoka_control', '-d', source,
            '--format=custom', '--file=' + str(archive))
    bundle = None
    if Path(archive).is_file():
        from backup_bundle import seal, verify
        staged = Path(archive).parent / 'backup-checkpoint'
        staged.mkdir()
        shutil.copyfile(archive, staged/'postgres.dump')
        (staged/'objects.json').write_text(json.dumps({'scope':'SYNTHETIC_EMPTY_OBJECT_SNAPSHOT','objects':[]}),encoding='utf-8')
        (staged/'configuration.json').write_text(json.dumps({'schemaMigrations':25,'runtimeRole':'dzoka_app','scope':'SYNTHETIC_RESTORE'}),encoding='utf-8')
        (staged/'tombstones.json').write_text(sql(source,"SELECT coalesce(jsonb_agg(to_jsonb(j) ORDER BY tenant_id,report_id),'[]') FROM dzoka.erasure_journal j;"),encoding='utf-8')
        inventory=[{'kind':kind,'name':name} for kind,name in [('postgres','postgres.dump'),('objects','objects.json'),('configuration','configuration.json'),('tombstones','tombstones.json')]]
        checkpoint=hashlib.sha256(json.dumps(before,sort_keys=True).encode()).hexdigest()
        manifest=seal(staged,inventory,checkpoint,hashlib.sha256(b'SYNTHETIC_TEST_ROLES_PRE_EXIST').hexdigest())
        bundle=verify(staged,manifest)
    # New empty DB; original owner and ACLs intentionally retained. Never use
    # --no-owner/--no-acl to make a failing restore appear to work.
    sql('postgres', f'CREATE DATABASE {identifier(restored)} OWNER dzoka_migrator;')
    command('pg_restore', '-h', '127.0.0.1', '-U', 'dzoka_control', '-d', restored,
            '--exit-on-error', '--single-transaction', str(archive))
    after, restored_catalog = snapshot(restored)
    differing_tables = sorted(name for name in before['tables'].keys() | after['tables'].keys()
                              if before['tables'].get(name) != after['tables'].get(name))
    differing_catalog = sorted(section for section in source_catalog.keys() | restored_catalog.keys()
                               if source_catalog.get(section) != restored_catalog.get(section))
    require_equal([], differing_tables, 'row hashes in ' + ', '.join(differing_tables))
    require_equal([], differing_catalog, 'catalog sections ' + ', '.join(differing_catalog))

    def visible(tenant=None, subject=None):
        context = ''
        if tenant:
            context = (f"SELECT set_config('dzoka.tenant_id','{tenant}',true);"
                       "SELECT set_config('dzoka.issuer','https://restore.invalid',true);"
                       f"SELECT set_config('dzoka.subject','{subject}',true);")
        result = sql(restored, 'BEGIN;' + context +
                     "SELECT 'VISIBLE:'||coalesce(string_agg(id::text,',' ORDER BY id),'') "
                     'FROM dzoka.organizations;ROLLBACK;', role='dzoka_app')
        return [line.removeprefix('VISIBLE:') for line in result.splitlines()
                if line.startswith('VISIBLE:')]

    require_equal([''], visible(), 'missing context denies restored rows')
    require_equal([org_a], visible(a, 'alice'), 'tenant A restored scope')
    require_equal([org_b], visible(b, 'bob'), 'tenant B restored scope')
    require_equal([''], visible(b, 'alice'), 'cross-tenant restored actor denied')
    unsafe = sql(restored, "SELECT count(*) FROM pg_roles WHERE rolname='dzoka_app' "
                 "AND (rolsuper OR rolbypassrls OR rolcreaterole OR rolcreatedb);"
                 "SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace "
                 "WHERE n.nspname='dzoka' AND pg_get_userbyid(c.relowner)='dzoka_app';")
    require_equal(['0','0'], unsafe.splitlines(), 'runtime role remains unprivileged/non-owner')
    # Prove this is an independent snapshot, not a connection back to source.
    sql(source, f"UPDATE dzoka.organizations SET name='Synthetic changed after backup' WHERE id='{org_a}';")
    require_equal(after, snapshot(restored)[0], 'source changes cannot mutate restored snapshot')
    result = {'status': 'PASS', 'tables': len(after['tables']),
              'rows': sum(t['rows'] for t in after['tables'].values()),
              'rowAndCatalogEquality': True, 'restoredTenantIsolation': True,
              'sourceSnapshotIndependence': True, 'backupBundle': bundle, 'snapshot': after,
              'limits': ['same-cluster logical restore; roles pre-exist',
                         'object snapshot is explicitly empty; tombstone replay is separately integration-tested; no external effects',
                         'not production RPO/RTO, HA or point-in-time recovery']}
    print('PASS: logical restore, exact rows/catalog, runtime-role and two-tenant isolation (' +
          str(result['tables']) + ' tables)', flush=True)
    return result
