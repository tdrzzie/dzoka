import json,struct,sys,unittest
from pathlib import Path
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'ai'))
import shona_cpu,shona_lid
class ShonaCpuTests(unittest.TestCase):
    def test_profiles_bind_new_cpu_model_runtime_and_source_identity(self):
        registry=shona_cpu.profiles();self.assertEqual(5,len(registry));self.assertEqual(registry,json.loads((ROOT/'ai/shona-cpu-profiles.json').read_bytes()));self.assertEqual(5,len({p['key'] for p in registry.values()}))
        for p in registry.values():self.assertEqual('float32',p['manifest']['configuration']['computeType']);self.assertNotEqual('LOCAL_DEVELOPMENT_ONLY',p['manifest']['configuration']['deploymentScope'])
    def test_hash_features_use_signed_utf8_and_eos_without_subwords(self):
        self.assertEqual(2166136261,shona_lid.hash_ngram(''));self.assertEqual(0xe40c292c,shona_lid.hash_ngram('a'))
        expected=2166136261
        for b in (0xffffffc3,0xffffffa9):expected=((expected^b)*16777619)&0xffffffff
        self.assertEqual(expected,shona_lid.hash_ngram('\u00e9'));self.assertEqual([0],shona_lid.features('',{'</s>':0}));self.assertEqual(shona_lid.features('a b',{'</s>':0}),shona_lid.features('a\t\nb',{'</s>':0}))
    def test_binary_protocol_rejects_trailing_truncated_oversize_and_invalid_utf8(self):
        valid=struct.pack('<II',1,4)+b'</s>';self.assertEqual(['</s>'],shona_cpu.decode_tokens(valid))
        for raw in [b'',valid+b'x',valid[:-1],struct.pack('<I',33),struct.pack('<II',1,1025),struct.pack('<II',1,1)+b'\xff']:
            with self.assertRaises((ValueError,UnicodeDecodeError)):shona_cpu.decode_tokens(raw)
    def test_conservative_window_policy_respects_punctuation_bounds_and_coverage(self):
        p=object.__new__(shona_lid.LanguageIdentifier);p.score=lambda text:(.99,.98) if text.startswith('around') else (.1,-.5)
        proposals,scan,cut=p.propose('Ndasiya around 2pm.');self.assertEqual(['around 2pm'],[s['quote'] for s in proposals]);self.assertEqual(19,scan);self.assertFalse(cut)
        self.assertEqual(['around'],[s['quote'] for s in p.propose('around, 2pm.')[0]])
        self.assertEqual([],p.propose('Ndarasa bhegi rangu.')[0]);self.assertTrue(p.propose('word '*40)[2])
    def test_auto_copy_retains_original_offsets_and_has_distinct_trust(self):
        p=object.__new__(shona_cpu.ShonaCpuProvider);p.config={'proposalPolicy':'glotlid-english-window-v1'};p.translate=lambda text:{'text':'I left 9820.'};p.lid=type('LID',(),{'propose':lambda self,text:([{'start':8,'end':14,'quote':'around','modelScore':.99,'modelMargin':.98}],15,False)})()
        e=p.infer(b'Ndasiya around.');self.assertEqual('I left around.',e['text']);self.assertEqual(8,e['proposedSpans'][0]['sourceStart']);self.assertEqual('AUTO_LANGUAGE_PROPOSAL_UNVERIFIED',e['proposedSpans'][0]['trust'])
        with self.assertRaises(ValueError):p.infer(b'[[keep:around]]')
        p.translate=lambda text:{'text':'I left.'}
        with self.assertRaises(ValueError):p.infer(b'Ndasiya around.')

    def test_missing_or_different_image_fails_without_substitution(self):
        p=object.__new__(shona_cpu.ShonaCpuProvider);p.image='sha256:approved'
        with patch.object(shona_cpu.subprocess,'run',return_value=type('Run',(),{'stdout':b'sha256:other'})()),self.assertRaisesRegex(ValueError,'image unavailable'):p.check_image()
    def test_cpu_timeout_removes_only_its_owned_container(self):
        import subprocess
        p=object.__new__(shona_cpu.ShonaCpuProvider);p.image='sha256:approved';p.native_files={name:'approved/'+name for name in ('model.bin','config.json','shared_vocabulary.json')}
        with patch.object(shona_cpu.subprocess,'run',side_effect=[subprocess.TimeoutExpired('docker',120),None]) as run,self.assertRaisesRegex(ValueError,'deadline'):p.native(['word','</s>'])
        command=run.call_args_list[0].args[0];cleanup=run.call_args_list[1].args[0];self.assertEqual(['docker','rm','-f'],cleanup[:3]);self.assertEqual(command[command.index('--name')+1],cleanup[3]);self.assertIn('--read-only',command);self.assertIn('none',command);self.assertEqual(3,command.count('--mount'));self.assertFalse(any('target=/model,readonly' in arg for arg in command))

    def test_closed_provider_cannot_infer_after_releasing_verified_handles(self):
        from contextlib import ExitStack
        p=object.__new__(shona_cpu.ShonaCpuProvider);p.stack=ExitStack();p.lid=None;p.close()
        with self.assertRaisesRegex(ValueError,'provider closed'):p.infer(b'word')
        with self.assertRaisesRegex(ValueError,'provider closed'):p.native(['word','</s>'])

if __name__=='__main__':unittest.main()
