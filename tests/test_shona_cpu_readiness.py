import subprocess
from pathlib import Path
import sys
import unittest
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'tools'))
from shona_cpu_runtime import image_readiness

class CpuReadinessTests(unittest.TestCase):
    command=['docker','image','inspect','sha256:'+'a'*64,'--format','{{.Id}}']
    def test_timeout_rechecks_integrity_before_next_inspect(self):
        events=[]
        def check():
            events.append('inspect')
            if len(events)==1:raise subprocess.TimeoutExpired(self.command,20)
        self.assertEqual(2,image_readiness(check,lambda:events.append('integrity'),self.command))
        self.assertEqual(['inspect','integrity','inspect'],events)
    def test_integrity_failure_prevents_retry(self):
        calls=[]
        def check():calls.append('inspect');raise subprocess.TimeoutExpired(self.command,20)
        def integrity():raise ValueError('changed model')
        with self.assertRaisesRegex(ValueError,'changed model'):image_readiness(check,integrity,self.command)
        self.assertEqual(['inspect'],calls)
    def test_other_commands_and_non_timeout_errors_propagate(self):
        for error in [subprocess.TimeoutExpired(['docker','run','unapproved'],20),ValueError('wrong image'),subprocess.CalledProcessError(1,self.command)]:
            calls=[]
            def check():calls.append('inspect');raise error
            with self.subTest(error=type(error).__name__),self.assertRaises(type(error)):image_readiness(check,lambda:self.fail('Unexpected retry'),self.command)
            self.assertEqual(['inspect'],calls)
    def test_three_timeouts_fail_with_retained_attempts(self):
        calls=[];refresh=[]
        def check():raise subprocess.TimeoutExpired(self.command,20)
        with self.assertRaises(subprocess.TimeoutExpired):image_readiness(check,lambda:refresh.append(True),self.command,calls.append)
        self.assertEqual([1,2,3],calls);self.assertEqual(2,len(refresh))
