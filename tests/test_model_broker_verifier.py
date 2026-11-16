from pathlib import Path
import sys
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'tools'))
import verify_model_broker as verifier


class BrokerVerifierTests(unittest.TestCase):
    def test_protocol_retry_checks_cleanup_and_full_integrity_before_inference(self):
        order = []

        def infer(*args, **kwargs):
            order.append('infer')
            if len(order) == 1:
                raise ValueError('Isolated model protocol unavailable')
            return {'output': 'authored'}

        with patch.object(verifier, 'infer', side_effect=infer), \
                patch.object(verifier, 'state_check', side_effect=lambda *a: order.append('cleanup')), \
                patch.object(verifier, 'verify_bundle', side_effect=lambda *a: order.append('integrity')):
            output, attempts = verifier.checked_inference('bundle', 'receipt', 'state', 'profile', b'authored')
        self.assertEqual({'output': 'authored'}, output)
        self.assertEqual(2, attempts)
        self.assertEqual(['infer', 'cleanup', 'integrity', 'infer'], order)

    def test_integrity_or_cleanup_failure_blocks_retry(self):
        for failing in ('state_check', 'verify_bundle'):
            with patch.object(verifier, 'infer', side_effect=ValueError('Isolated model protocol unavailable')) as infer, \
                    patch.object(verifier, 'state_check') as cleanup, \
                    patch.object(verifier, 'verify_bundle') as integrity:
                (cleanup if failing == 'state_check' else integrity).side_effect = ValueError('Authored integrity failure')
                with self.assertRaisesRegex(ValueError, 'Authored integrity'):
                    verifier.checked_inference('bundle', 'receipt', 'state', 'profile', b'authored')
                self.assertEqual(1, infer.call_count)

    def test_invalid_output_and_unconfirmed_cleanup_are_not_availability_retries(self):
        for error in (ValueError('Wrong model invocation'), OSError('Model cleanup unconfirmed')):
            with patch.object(verifier, 'infer', side_effect=error) as infer, \
                    patch.object(verifier, 'verify_bundle') as integrity, self.assertRaises(type(error)):
                verifier.checked_inference('bundle', 'receipt', 'state', 'profile', b'authored')
            self.assertEqual(1, infer.call_count)
            integrity.assert_not_called()

    def test_repeated_unavailability_is_a_gate_failure(self):
        with patch.object(verifier, 'infer', side_effect=ValueError('Isolated model protocol unavailable')) as infer, \
                patch.object(verifier, 'state_check') as cleanup, \
                patch.object(verifier, 'verify_bundle') as integrity, self.assertRaises(ValueError):
            verifier.checked_inference('bundle', 'receipt', 'state', 'profile', b'authored')
        self.assertEqual(3, infer.call_count)
        self.assertEqual(2, cleanup.call_count)
        self.assertEqual(2, integrity.call_count)
