import hashlib,json,os,shutil,sys,tempfile,unittest,zipfile
from pathlib import Path
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'ai'))
import shona,shona_integrity,shona_worker,shona_markup
class ShonaTests(unittest.TestCase):
    def test_directed_profiles_are_distinct_role_bound_and_development_only(self):
        profiles=shona.profiles();self.assertEqual(profiles,json.loads((ROOT/'ai/shona-profiles.json').read_bytes()));self.assertEqual({'CT2_EN_SN','CT2_SN_EN','CT2_EN_SN_KEEP','CT2_SN_EN_KEEP'},set(profiles));self.assertEqual(4,len({x['key'] for x in profiles.values()}))
        for p in profiles.values():
            c=p['manifest']['configuration'];self.assertEqual('LOCAL_DEVELOPMENT_ONLY',c['deploymentScope']);self.assertEqual('UNVERIFIED_TRANSLATION',c['trust']);self.assertTrue(all('role' in a for a in p['manifest']['artifacts']))
    def test_missing_development_flag_and_remote_core_fail_before_native_or_network(self):
        with patch.dict(os.environ,{'DZOKA_AI_SHONA_LOCAL_DEVELOPMENT':'false'}),self.assertRaisesRegex(ValueError,'local development'):shona.ShonaProvider('CT2_EN_SN','unused')
        with patch.dict(os.environ,{'DZOKA_AI_SHONA_LOCAL_DEVELOPMENT':'true'}),patch.object(shona_worker,'request') as request,self.assertRaisesRegex(ValueError,'loopback'):shona_worker.run_once('https://example.test','unused','unused','unused','unused')
        request.assert_not_called()
    def test_corrupt_model_fails_before_native_import(self):
        with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as d:
            p=Path(d)/'ct2_en_sn';p.mkdir();(p/'model.bin').write_bytes(b'changed')
            with patch.dict(os.environ,{'DZOKA_AI_SHONA_LOCAL_DEVELOPMENT':'true'}),patch.dict(sys.modules,{'ctranslate2':None}),self.assertRaisesRegex(ValueError,'artifact integrity'):shona.ShonaProvider('CT2_EN_SN',d)
    def test_layered_dll_and_required_notice_tampering_fail_closed(self):
        with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as d:
            root=Path(d);(root/'ai').mkdir();(root/'.audit-cache/shona-wheels').mkdir(parents=True);(root/'ai/generative-runtime.lock.json').write_bytes(b'parent');site=root/'site';site.mkdir();dll=site/'native.dll';dll.write_bytes(b'approved');wheel=root/'.audit-cache/shona-wheels/test.whl'
            with zipfile.ZipFile(wheel,'w') as z:z.writestr('native.dll',b'approved')
            notice=root/'NOTICE';notice.write_bytes(b'notice');lock={'parentRuntimeLockSha256':hashlib.sha256(b'parent').hexdigest(),'notices':[{'path':'NOTICE','sha256':hashlib.sha256(b'notice').hexdigest()}],'additionalWheels':[{'name':'test','version':'1','wheel':'test.whl','sha256':hashlib.sha256(wheel.read_bytes()).hexdigest()}]};(root/'ai/shona-runtime.lock.json').write_bytes(json.dumps(lock).encode())
            with patch.object(shona_integrity,'ROOT',root),patch.object(shona_integrity.sys,'platform','win32'),patch.object(shona_integrity,'verify_base'),patch.object(shona_integrity.sysconfig,'get_paths',return_value={'purelib':str(site)}),patch.object(shona_integrity.importlib.metadata,'version',return_value='1'):
                shona_integrity.verify_runtime();dll.write_bytes(b'changed')
                with self.assertRaisesRegex(ValueError,'Installed layered'):shona_integrity.verify_runtime()
                dll.write_bytes(b'approved');notice.write_bytes(b'changed')
                with self.assertRaisesRegex(ValueError,'notice changed'):shona_integrity.verify_runtime()
    def test_decoder_reports_bounds_without_inventing_eos_or_unknown_coverage(self):
        p=object.__new__(shona.ShonaProvider);p.config={'sourceLanguage':'en','targetLanguage':'sn','beamSize':4};p.source_sp=type('SP',(),{'encode':lambda self,text,out_type:text.split()})();p.target_sp=type('SP',(),{'decode_pieces':lambda self,tokens:' '.join(tokens)})();p.vocab={'word'}
        seen={}
        def run(batch,**kwargs):seen.update(kwargs);seen['batch']=batch;return [type('Result',(),{'hypotheses':[['text']*32]})()]
        p.translator=type('Translator',(),{'translate_batch':staticmethod(run)})();e=p.infer(('word '*200).encode());self.assertEqual(128,e['inputTokenCount']);self.assertTrue(e['inputTruncated'] and e['outputTruncated']);self.assertEqual('OUTPUT_LIMIT',e['finishReason']);self.assertEqual('</s>',seen['batch'][0][-1]);self.assertTrue(seen['return_end_token']);self.assertFalse(seen['replace_unknowns'])
        with self.assertRaisesRegex(ValueError,'Source unavailable'):p.translate('unknown')
    def test_explicit_copy_spans_preserve_utf16_source_and_output_offsets(self):
        source='Ndasiya [[keep:bag 😀]] [[keep:around 2pm]]';masked,spans=shona_markup.protect(source);text,copies=shona_markup.restore('I left 9820 9821.',spans);self.assertEqual('I left bag 😀 around 2pm.',text)
        self.assertEqual('SOURCE_COPY_UNVERIFIED',copies[0]['trust']);self.assertEqual(shona_markup.utf16(source[:source.index('bag')]),copies[0]['sourceStart']);self.assertEqual(shona_markup.utf16('I left bag 😀'),copies[0]['outputEnd'])
    def test_missing_duplicated_reordered_changed_or_embedded_placeholders_fail(self):
        _,spans=shona_markup.protect('[[keep:bag]] [[keep:time]]')
        for output in ['9820','9820 9820 9821','9821 9820','982 0 9821','19820 9821']:
            with self.assertRaises(ValueError):shona_markup.restore(output,spans)
        for source in ['unmarked','[[keep: ]]','[[keep:nested [[keep:bag]]]]','[[keep:bag]','1982 [[keep:bag]]','[[keep:'+('😀'*129)+']]']:
            with self.assertRaises(ValueError):shona_markup.protect(source)
    def test_marker_mode_never_guesses_spans_and_labels_copied_source(self):
        p=object.__new__(shona.ShonaProvider);p.config={'protectionPolicy':'explicit-keep-utf16-v1'};p.translate=lambda source:{'text':'I left 9820.'}
        e=p.infer(b'Ndasiya [[keep:bag]]');self.assertEqual('I left bag.',e['text']);self.assertEqual('ctranslate2-marked-v1',e['representation']);self.assertEqual('bag',e['preservedSpans'][0]['quote'])
        with self.assertRaises(ValueError):p.infer(b'Ndasiya bag')
    def test_mutated_markup_implementation_cannot_reuse_profile_identity(self):
        with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as d:
            root=Path(d);(root/'ai').mkdir();lock=json.loads((ROOT/'ai/shona.lock.json').read_bytes())
            for name in ['ai/shona.lock.json','ai/shona-runtime.lock.json','ai/generative-runtime.lock.json','ai/runtime.lock.json',*lock['sources']]:shutil.copy2(ROOT/name,root/name)
            with (root/'ai/shona_markup.py').open('ab') as output:output.write(b'changed')
            with patch.object(shona,'ROOT',root),self.assertRaisesRegex(ValueError,'implementation changed'):shona.profiles()
    def test_evaluation_fixture_mutation_fails_before_inference(self):
        self.assertEqual(8,len(shona.evaluation()['cases']))
        with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as d:
            root=Path(d);(root/'ai').mkdir();shutil.copy2(ROOT/'ai/shona.lock.json',root/'ai/shona.lock.json');(root/'ai/shona-evaluation.json').write_bytes(b'changed')
            with patch.object(shona,'ROOT',root),self.assertRaisesRegex(ValueError,'fixture changed'):shona.evaluation()
if __name__=='__main__':unittest.main()
