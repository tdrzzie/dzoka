from pathlib import Path
import sys
import unittest
from unittest.mock import patch
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'tools'))
import verify_current_setup as v


class ReadinessVerifierTests(unittest.TestCase):
    def setUp(self):self.now=0
    def clock(self):return self.now
    def pause(self,_):self.now+=100
    def wait(self,**kwargs):return v.wait_running({},clock=self.clock,pause=self.pause,emit=lambda _:None,**kwargs)

    def test_new_owned_running_generation_is_required(self):
        rows=[{'phase':'RUNNING','run':'a'*32},{'phase':'STARTING','run':'b'*32},{'phase':'RUNNING','run':'b'*32}]
        with patch.object(v,'snapshot',side_effect=rows),patch.object(v,'active',return_value=True):self.assertEqual(200,self.wait(previous='a'*32))

    def test_terminal_new_generation_fails_immediately(self):
        for phase in ('STOPPED','UNAVAILABLE','CIRCUIT_OPEN'):
            with self.subTest(phase=phase),patch.object(v,'snapshot',return_value={'phase':phase,'run':'b'*32}),patch.object(v,'active') as active:
                with self.assertRaisesRegex(AssertionError,'terminal'):self.wait(previous='a'*32)
                active.assert_not_called()

    def test_stale_terminal_record_is_not_a_failure_for_new_launch(self):
        rows=[{'phase':'UNAVAILABLE','run':'a'*32},{'phase':'RUNNING','run':'b'*32}]
        with patch.object(v,'snapshot',side_effect=rows),patch.object(v,'active',return_value=True):self.assertEqual(100,self.wait(previous='a'*32))

    def test_running_status_without_active_ownership_cannot_pass(self):
        with patch.object(v,'snapshot',return_value={'phase':'RUNNING','run':'b'*32}),patch.object(v,'active',return_value=False):
            with self.assertRaisesRegex(AssertionError,'deadline'):self.wait(seconds=180)

    def test_invalid_generation_cannot_pass_and_wait_is_bounded(self):
        with patch.object(v,'snapshot',return_value={'phase':'RUNNING','run':'invalid'}),patch.object(v,'active') as active:
            with self.assertRaisesRegex(AssertionError,'deadline'):self.wait(seconds=180)
            active.assert_not_called()
        for seconds in (True,0,721,-1):
            with self.subTest(seconds=seconds),self.assertRaises(ValueError):self.wait(seconds=seconds)

    def test_slow_verification_window_is_measured_without_weakening_readiness(self):
        rows=[{}, {'phase':'STARTING','run':'b'*32},{'phase':'STARTING','run':'b'*32},{'phase':'STARTING','run':'b'*32},{'phase':'RUNNING','run':'b'*32}]
        with patch.object(v,'snapshot',side_effect=rows),patch.object(v,'active',return_value=True):self.assertEqual(400,self.wait())

    def test_permanent_starting_never_becomes_a_success(self):
        with patch.object(v,'snapshot',return_value={'phase':'STARTING','run':'b'*32}),patch.object(v,'active') as active:
            with self.assertRaisesRegex(AssertionError,'deadline'):self.wait()
            active.assert_not_called()


if __name__=='__main__':unittest.main()
