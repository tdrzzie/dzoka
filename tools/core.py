"""Launch the built core or the separate privileged migration entry point."""
import argparse
import os
from pathlib import Path
import shutil
import subprocess
from dependency_lock import check

ROOT=Path(__file__).resolve().parents[1]

def main():
    parser=argparse.ArgumentParser();parser.add_argument('operation',choices=['serve','migrate','sanitize-media','purge-media','expire-intake','purge-ai']);args=parser.parse_args()
    check(ROOT)
    java=(Path(os.environ['JAVA_HOME'])/'bin'/('java.exe' if os.name=='nt' else 'java')) if os.environ.get('JAVA_HOME') else shutil.which('java')
    if not java:raise SystemExit('Reviewed Java 21+ runtime is required')
    if args.operation=='serve':
        command=[str(java),'-jar',str(ROOT/'target/dzoka-core-0.1.0-SNAPSHOT.jar')]
        child_environment={k:v for k,v in os.environ.items() if not k.startswith('DZOKA_MIGRATION_')}
    else:
        import json
        lock=json.loads((ROOT/'dependencies.lock.json').read_text())
        paths=[str(ROOT/'target/classes')]+[str(ROOT/'.audit-cache/m2'/d['jar']) for d in lock['dependencies'] if d['scope']!='test']
        if args.operation=='migrate':
            command=[str(java),'-cp',os.pathsep.join(paths),'org.dzoka.foundation.persistence.MigrateDatabase']
            child_environment={k:v for k,v in os.environ.items() if not k.startswith(('DZOKA_DB_','DZOKA_OIDC_'))}
        elif args.operation=='purge-ai':
            command=[str(java),'-cp',os.pathsep.join(paths),'org.dzoka.bootstrap.AiMaintenanceMain']
            child_environment={k:v for k,v in os.environ.items() if not k.startswith(('DZOKA_MIGRATION_','DZOKA_OIDC_','DZOKA_MEDIA_','DZOKA_AI_'))}
        else:
            operation={'sanitize-media':'sanitize','purge-media':'purge','expire-intake':'expire'}[args.operation]
            command=[str(java),'-cp',os.pathsep.join(paths),'org.dzoka.bootstrap.MediaWorkerMain',operation]
            child_environment={k:v for k,v in os.environ.items() if not k.startswith(('DZOKA_MIGRATION_','DZOKA_OIDC_'))}
    return subprocess.run(command,cwd=ROOT,env=child_environment).returncode

if __name__=='__main__':raise SystemExit(main())
