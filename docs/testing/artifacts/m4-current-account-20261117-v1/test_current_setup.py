import copy
from pathlib import Path
import sys
import unittest
from unittest.mock import patch
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'tools'))
from current_setup_worker import validate_config,stop_record
import current_setup_worker as worker

class InteractiveDeploymentTests(unittest.TestCase):
    def config(self):return {'schemaVersion':1,'owner':'a'*32,'accountSid':'S-1-5-21-1-2-3-1001','integrityRid':8192,'policy':'C:\\private\\policy.json','controlState':'C:\\private\\state','managementReceipt':'b'*64,'watchdogStart':'2026-11-16T00:00:00Z'}
    def test_unreviewed_configuration_authority_rejected(self):
        for field,value in [('schemaVersion',True),('owner','../other'),('integrityRid',16384),('integrityRid',True),('managementReceipt','b'*63),('policy','C:\\private\\x" --evil'),('controlState','C:\\private\nstate')]:
            with self.subTest(field=field,value=value):
                config=self.config();config[field]=value
                with self.assertRaises(ValueError):validate_config(config)
        config=self.config();config['credentials']='do not allow a token in task configuration'
        with self.assertRaises(ValueError):validate_config(config)
    def test_stale_stop_never_stops_new_generation(self):
        for record in [{'schemaVersion':1,'run':'a'*32},{'schemaVersion':True,'run':'b'*32},{'schemaVersion':1,'run':'b'*32,'command':'kill'}]:
            with self.subTest(record=record):
                with self.assertRaises(ValueError):stop_record(record,'b'*32)
        stop_record({'schemaVersion':1,'run':'b'*32},'b'*32)
    def test_restart_budget_rejects_changed_authority_and_boolean_counts(self):
        base={'schemaVersion':1,'configurationSha256':'a'*64,'attempts':0}
        for field,value in [('configurationSha256','b'*64),('attempts',True),('attempts',-1),('attempts',5),('schemaVersion',True)]:
            record=copy.deepcopy(base);record[field]=value
            with self.subTest(field=field,value=value),patch.object(worker,'protected',side_effect=lambda p:p),patch.object(worker,'read_json',return_value=record),patch.object(worker,'write_budget') as write:
                with self.assertRaises(ValueError):worker.consume_budget(Path('state'),'a'*64)
                write.assert_not_called()
    def test_restart_budget_caps_initial_plus_three_replacements(self):
        for count in range(5):
            with self.subTest(count=count),patch.object(worker,'protected',side_effect=lambda p:p),patch.object(worker,'read_json',return_value={'schemaVersion':1,'configurationSha256':'a'*64,'attempts':count}),patch.object(worker,'write_budget') as write:
                self.assertEqual(count<4,worker.consume_budget(Path('state'),'a'*64))
                if count<4:write.assert_called_once_with(Path('state'),'a'*64,count+1)
                else:write.assert_not_called()
