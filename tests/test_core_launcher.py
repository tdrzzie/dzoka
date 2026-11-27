"""Credential separation for the privileged foundation operator process."""
import importlib.util
import os
from pathlib import Path
import sys
import unittest
from unittest.mock import patch

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'tools'))
spec=importlib.util.spec_from_file_location('foundation_core_launcher',ROOT/'tools/core.py')
launcher=importlib.util.module_from_spec(spec);spec.loader.exec_module(launcher)

class CoreLauncherTests(unittest.TestCase):
    def test_operator_receives_only_migration_credentials_and_explicit_test_flag(self):
        environment={'JAVA_HOME':'C:/reviewed-java','PATH':'synthetic-path','DZOKA_MIGRATION_URL':'synthetic-db',
            'DZOKA_MIGRATION_PASSWORD_FILE':'synthetic-migration-file','DZOKA_DB_PASSWORD':'runtime-placeholder',
            'DZOKA_AI_TOKEN':'provider-placeholder','DZOKA_MEDIA_SECRET_KEY':'storage-placeholder',
            'SPRING_DATASOURCE_PASSWORD':'spring-placeholder','DZOKA_ADMIN_ALLOW_INSECURE_LOOPBACK':'true'}
        with patch.dict(os.environ,environment,clear=True),patch.object(sys,'argv',['core.py','admin-foundation']),patch.object(launcher,'check'),patch.object(launcher.subprocess,'run') as run:
            run.return_value.returncode=0
            self.assertEqual(0,launcher.main())
            args=run.call_args
            self.assertEqual('org.dzoka.foundation.persistence.FoundationAdmin',args.args[0][-1])
            child=args.kwargs['env']
            self.assertEqual('synthetic-migration-file',child['DZOKA_MIGRATION_PASSWORD_FILE'])
            self.assertEqual('true',child['DZOKA_ADMIN_ALLOW_INSECURE_LOOPBACK'])
            for key in ['DZOKA_DB_PASSWORD','DZOKA_AI_TOKEN','DZOKA_MEDIA_SECRET_KEY','SPRING_DATASOURCE_PASSWORD']:self.assertNotIn(key,child)

    def test_api_never_receives_migration_secrets(self):
        with patch.dict(os.environ,{'JAVA_HOME':'C:/reviewed-java','DZOKA_MIGRATION_PASSWORD_FILE':'synthetic-file'},clear=True),patch.object(sys,'argv',['core.py','serve']),patch.object(launcher,'check'),patch.object(launcher.subprocess,'run') as run:
            run.return_value.returncode=0
            launcher.main()
            self.assertNotIn('DZOKA_MIGRATION_PASSWORD_FILE',run.call_args.kwargs['env'])

if __name__=='__main__':unittest.main()
