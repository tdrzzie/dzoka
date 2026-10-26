"""Run Java integration tests against a fresh loopback-only PostgreSQL cluster.

Uses installed reviewed binaries; never reads/mutates an existing server data directory.
All state and random credentials remain in ignored .test-tmp. No recursive deletion.
"""
import argparse
import json
import os
from pathlib import Path
import secrets
import socket
import subprocess
import sys
import uuid

ROOT = Path(__file__).resolve().parents[1]

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--pg-bin', default=os.environ.get('DZOKA_PG_BIN'))
    parser.add_argument('--maven', default=os.environ.get('DZOKA_MAVEN'))
    parser.add_argument('--full',action='store_true',help='Include live S3/sandbox tests; requires the intake infrastructure environment')
    parser.add_argument('--restore-drill',action='store_true',help='After passing tests, verify a real logical restore in this disposable cluster')
    args = parser.parse_args()
    if not args.pg_bin or not args.maven:
        parser.error('--pg-bin and --maven (or DZOKA_PG_BIN / DZOKA_MAVEN) are required')
    pg = Path(args.pg_bin).resolve()
    executable = '.exe' if os.name == 'nt' else ''
    run = ROOT / '.test-tmp' / ('foundation-' + str(uuid.uuid4()))
    run.mkdir(parents=True)
    data = run / 'data'
    password = secrets.token_urlsafe(32)
    app_password = secrets.token_urlsafe(32)
    migration_password = secrets.token_urlsafe(32)
    pwfile = run / 'password'
    pwfile.write_text(password)
    if os.name != 'nt': pwfile.chmod(0o600)
    with socket.socket() as reservation:
        reservation.bind(('127.0.0.1', 0))
        port = reservation.getsockname()[1]
    env = os.environ.copy()
    env.update(PGPASSWORD=password, PGCLIENTENCODING='UTF8', DZOKA_TEST_ADMIN_URL=f'jdbc:postgresql://127.0.0.1:{port}/dzoka_test',
               DZOKA_TEST_ADMIN_USER='dzoka_control', DZOKA_TEST_ADMIN_PASSWORD=password,
               DZOKA_TEST_APP_PASSWORD=app_password, DZOKA_TEST_MIGRATION_USER='dzoka_migrator',
               DZOKA_TEST_MIGRATION_PASSWORD=migration_password)
    def command(name, *arguments, **kwargs):
        # A background postgres child must not inherit a PIPE held open after pg_ctl exits.
        if name == 'pg_ctl':
            with (run / 'control.log').open('a', encoding='utf-8') as log:
                return subprocess.run([str(pg / (name + executable)), *arguments], env=env, cwd=ROOT,
                                      check=True, stdout=log, stderr=subprocess.STDOUT,
                                      text=True, timeout=60, **kwargs)
        return subprocess.run([str(pg / (name + executable)), *arguments], env=env, cwd=ROOT,
                              check=True, capture_output=True, text=True, encoding='utf-8', timeout=60, **kwargs)
    started = False
    try:
        command('initdb', '-D', str(data), '-U', 'dzoka_control', '--pwfile', str(pwfile),
                '--auth-host=scram-sha-256', '--auth-local=scram-sha-256', '--encoding=UTF8', '--no-locale')
        pwfile.unlink()
        # Socket directories are irrelevant on Windows; on Unix put them in this private run dir.
        options = f'-h 127.0.0.1 -p {port}'
        if os.name != 'nt': options += f' -k "{run}"'
        command('pg_ctl', '-D', str(data), '-l', str(run / 'postgres.log'), '-o', options, '-w', 'start')
        started = True
        # Generated password contains only token_urlsafe alphabet; no user text enters this SQL.
        command('psql', '-h', '127.0.0.1', '-p', str(port), '-U', 'dzoka_control', '-d', 'postgres',
                '-v', 'ON_ERROR_STOP=1', input=f"CREATE ROLE dzoka_app LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOBYPASSRLS PASSWORD '{app_password}';\n"
                f"CREATE ROLE dzoka_migrator LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOBYPASSRLS PASSWORD '{migration_password}';\n"
                "CREATE DATABASE dzoka_test OWNER dzoka_migrator;\n")
        print('Fresh loopback PostgreSQL cluster ready; existing installations untouched.', flush=True)
        maven = [args.maven, '-B', '-ntp', '-Dmaven.repo.local=.audit-cache/m2', 'verify']
        if not args.full:
            maven.append('-Dit.test=FoundationIT,IntakeIT,MediaStoreIT,AiJobsIT,FingerprintRolloutsIT,PerceptionJobsIT,StartupConfigurationIT')
            print('Explicit database/API suite; live S3/sandbox tests require the full intake harness.',flush=True)
        elif env.get('DZOKA_IT_CLASSES'):
            maven.append('-Dit.test='+env['DZOKA_IT_CLASSES'])
        result = subprocess.run(maven, env=env, cwd=ROOT)
        if result.returncode == 0 and args.restore_drill:
            from restore_drill import drill
            def restore_command(tool,*arguments,**kwargs):
                # Never let PostgreSQL clients fall back to a host default port.
                return command(tool,'-p',str(port),*arguments,**kwargs)
            evidence=drill(restore_command,run/'synthetic-backup.dump')
            (run/'restore-result.json').write_text(json.dumps(evidence,indent=2)+'\n',encoding='utf-8')
            print('Synthetic restore evidence: '+str(run/'restore-result.json'),flush=True)
        return result.returncode
    except subprocess.CalledProcessError as error:
        # initdb/pg_ctl output has no credentials; never echo the role-provisioning SQL.
        print('Foundation runner failed:', Path(error.cmd[0]).name, file=sys.stderr)
        print(error.stderr or '', file=sys.stderr)
        return 1
    finally:
        if pwfile.exists(): pwfile.unlink()
        if started or (data/'postmaster.pid').exists():
            command('pg_ctl', '-D', str(data), '-m', 'fast', '-w', 'stop')
            print('Isolated test PostgreSQL stopped.', flush=True)

if __name__ == '__main__':
    raise SystemExit(main())
