import io
from pathlib import Path
import sys
import unittest
from unittest.mock import patch
import urllib.error
from uuid import UUID

sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'tools'))
from matching_maintenance import commands, execute


class MatchingMaintenanceTests(unittest.TestCase):
    def plan(self):
        return {'schemaVersion':1,'origin':'http://127.0.0.1:8080','allowLoopbackHttp':True,
                'assignments':[{'tenant':str(UUID(int=1)),'spaces':[str(UUID(int=2))],'pruneHistory':True}]}

    def test_commands_require_scoped_assignments_and_transport(self):
        plan=self.plan()
        self.assertEqual(['PUT','POST'],[method for method,_ in commands(plan)])
        plan['assignments'] *= 2
        with self.assertRaises(ValueError): commands(plan)
        for origin in ('http://outside.example:8080','https://example.org/@somebody','https://x:password@example.org'):
            plan=self.plan();plan['origin']=origin
            with self.assertRaises(ValueError): commands(plan)

    def test_partial_attempt_stops_without_retry_or_secret_output(self):
        class Response(io.BytesIO): status=200
        jobs=commands(self.plan());token='a'*32
        error=urllib.error.HTTPError(jobs[1][1],503,'private upstream',{},None)
        with patch('matching_maintenance.urllib.request.build_opener') as opener:
            transport=opener.return_value
            transport.open.side_effect=[Response(b'{}'),error]
            self.assertEqual(1,execute(jobs,token))
            self.assertEqual(2,transport.open.call_count)
            self.assertEqual('Bearer '+token,transport.open.call_args.args[0].get_header('Authorization'))
