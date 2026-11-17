import copy
import hashlib
import json
from pathlib import Path
import sys
import tempfile
import unittest
import urllib.error
from unittest.mock import patch

ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
import openrouter_connection as client

class OpenRouterConnectionTests(unittest.TestCase):
    def setUp(self):
        self.config={'schemaVersion':1,'enabled':True,'model':'example/dated-model','providers':['Example'],'maxOutputTokens':256,'requireZdr':True,'referenceOnly':True}
        text='A public authored black bag example.'
        self.source={'classification':'PUBLIC_REFERENCE','task':'TRANSLATION','sourceLanguage':'en','targetLanguage':'fr','text':text,'sourceReference':'https://example.org/authored','sourceSha256':hashlib.sha256(text.encode()).hexdigest()}
        self.response={'id':'request-one','model':'example/dated-model','provider':'Example','choices':[{'finish_reason':'stop','message':{'content':'Sac noir.'}}],'usage':{'prompt_tokens':12,'completion_tokens':4}}

    def test_explicit_routing_untrusted_provenance_and_no_key_in_result(self):
        def read(path,*args):return self.config if str(path)=='settings' else self.source
        with patch.object(client,'read_private',side_effect=read),patch.object(client,'key',return_value='synthetic-credential-never-returned'),patch.object(client,'exchange',return_value=self.response) as send:
            result=client.reference('settings','source')
        self.assertEqual('NOT_ESTABLISHED',result['approval']);self.assertEqual('UNVERIFIED_TRANSLATION',result['trust']);self.assertNotIn('synthetic-credential',json.dumps(result))
        body=send.call_args.args[2];self.assertEqual({'only':['Example'],'allow_fallbacks':False,'require_parameters':True,'data_collection':'deny','zdr':True},body['provider'])
        self.assertEqual(self.source['sourceSha256'],result['inputSha256'])
        self.assertIn('English (en) to French (fr)',body['messages'][0]['content'])
        self.assertEqual('named-languages-v2',result['promptVersion'])
        self.assertEqual(hashlib.sha256(body['messages'][0]['content'].encode()).hexdigest(),result['promptSha256'])

    def test_disabled_missing_model_private_input_and_hash_tamper_never_send(self):
        for field,value in [('enabled',False),('model',None),('providers',[]),('requireZdr',False),('referenceOnly',False)]:
            bad={**self.config,field:value}
            with patch.object(client,'read_private',return_value=bad),patch.object(client,'exchange') as send,self.assertRaises(ValueError):
                client.reference('settings','source')
            send.assert_not_called()
        for field,value in [('classification','TENANT_PRIVATE'),('sourceSha256','0'*64),('sourceReference','file:///private')]:
            bad={**self.source,field:value}
            with patch.object(client,'read_private',side_effect=lambda p,*a:self.config if str(p)=='settings' else bad),patch.object(client,'exchange') as send,self.assertRaises(ValueError):client.reference('settings','source')
            send.assert_not_called()

    def test_unexpected_provider_model_tool_and_usage_fail_closed(self):
        for field,value in [('provider','Unapproved'),('model','other/model'),('usage',{'completion_tokens':True}),('choices',[{'finish_reason':'stop','message':{'content':'x','tool_calls':[{}]}}])]:
            bad={**self.response,field:value}
            with patch.object(client,'read_private',side_effect=lambda p,*a:self.config if str(p)=='settings' else self.source),patch.object(client,'key',return_value='synthetic-key'),patch.object(client,'exchange',return_value=bad),self.assertRaises(ValueError):client.reference('settings','source')

    def test_no_redirect_and_no_arbitrary_routes_or_nonfinite_duplicate_json(self):
        with self.assertRaises(ValueError):client.NoRedirect().redirect_request(None,None,302,'',{},'https://evil.example')
        with self.assertRaises(ValueError):client.exchange('/unapproved','synthetic-key')
        for raw in ['{"a":1,"a":2}','{"a":NaN}']:
            with self.assertRaises(ValueError):client.parse(raw)

    def test_uncertain_paid_attempt_prevents_repeated_transport(self):
        with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as folder:
            path=Path(folder)/'result.json';argv=['openrouter_connection.py','reference','--input','public.json','--output',str(path)]
            with patch.object(sys,'argv',argv),patch.object(client,'private',side_effect=Path),patch.object(client,'reference',side_effect=ValueError('uncertain')) as invoke:
                self.assertEqual(1,client.main());self.assertEqual(1,client.main());self.assertEqual(1,invoke.call_count)
            self.assertTrue(Path(str(path)+'.attempt.json').exists());self.assertFalse(path.exists())
            failure=json.loads(Path(str(path)+'.failure.json').read_bytes())
            self.assertEqual('VALIDATION_OR_LOCAL_FAILURE',failure['code'])
            self.assertNotIn('uncertain',json.dumps(failure))

    def test_transport_failures_have_safe_codes_and_never_repeat_requests(self):
        for error,code,status in (
            (TimeoutError('synthetic private body'),'TRANSPORT_TIMEOUT',None),
            (urllib.error.HTTPError('https://example.org/private',429,'private body',{},None),'HTTP_ERROR',429),
            (urllib.error.URLError('private body'),'NETWORK_UNAVAILABLE',None),
        ):
            with patch.object(client.urllib.request,'build_opener') as factory:
                opener=factory.return_value
                opener.open.side_effect=error
                with self.assertRaises(client.ReferenceUnavailable) as raised:
                    client.exchange('/chat/completions','synthetic-key',{},timeout=180)
                self.assertEqual(code,raised.exception.code)
                self.assertEqual(status,raised.exception.status)
                self.assertNotIn('private',str(raised.exception))
                self.assertEqual(1,opener.open.call_count)

    def test_http_success_with_provider_error_preserves_only_numeric_diagnostics(self):
        for value,expected in ((502,502),(True,None),('sensitive-body',None)):
            with patch.object(client.urllib.request,'build_opener') as factory:
                response=factory.return_value.open.return_value.__enter__.return_value
                response.status=200
                response.read.return_value=json.dumps({'error':{'code':value,'message':'sensitive-provider-body'}}).encode()
                with self.assertRaises(client.ReferenceUnavailable) as raised:
                    client.exchange('/chat/completions','synthetic-key',{})
            self.assertEqual('PROVIDER_ERROR_ENVELOPE',raised.exception.code)
            self.assertEqual(200,raised.exception.status)
            self.assertEqual(expected,raised.exception.api_code)
            self.assertNotIn('sensitive',str(raised.exception))

    def test_failure_receipt_distinguishes_http_and_provider_status(self):
        with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as folder:
            path=Path(folder)/'result.json'
            argv=['openrouter_connection.py','reference','--input','public.json','--output',str(path)]
            error=client.ReferenceUnavailable('PROVIDER_ERROR_ENVELOPE',200,502)
            with patch.object(sys,'argv',argv),patch.object(client,'private',side_effect=Path),patch.object(client,'reference',side_effect=error):
                self.assertEqual(1,client.main())
            failure=json.loads(Path(str(path)+'.failure.json').read_bytes())
            self.assertEqual(200,failure['httpStatus'])
            self.assertEqual(502,failure['apiErrorCode'])
            self.assertFalse(path.exists())

    def test_version_two_timeout_preserves_legacy_settings_and_bounds(self):
        with patch.object(client,'read_private',return_value=self.config):
            self.assertEqual(1,client.settings('settings')['schemaVersion'])
        for value in (9,181,True,None):
            config={**self.config,'schemaVersion':2,'transportTimeoutSeconds':value}
            with patch.object(client,'read_private',return_value=config),self.assertRaises(ValueError):
                client.settings('settings')
        config={**self.config,'schemaVersion':2,'transportTimeoutSeconds':180}
        with patch.object(client,'read_private',side_effect=lambda p,*a:config if str(p)=='settings' else self.source),patch.object(client,'key',return_value='synthetic'),patch.object(client,'exchange',return_value=self.response) as send:
            client.reference('settings','source')
        self.assertEqual(180,send.call_args.kwargs['timeout'])

    def test_explicit_temperature_omission_keeps_strict_provider_parameters(self):
        config={**self.config,'schemaVersion':2,'transportTimeoutSeconds':180,'temperature':None}
        with patch.object(client,'read_private',side_effect=lambda p,*a:config if str(p)=='settings' else self.source),patch.object(client,'key',return_value='synthetic'),patch.object(client,'exchange',return_value=self.response) as send:
            client.reference('settings','source')
        body=send.call_args.args[2]
        self.assertNotIn('temperature',body)
        self.assertTrue(body['provider']['require_parameters'])
        for value in (True,-1,3,float('nan')):
            with patch.object(client,'read_private',return_value={**config,'temperature':value}),self.assertRaises(ValueError):
                client.settings('settings')

    def test_reasoning_effort_is_opt_in_bounded_and_preserves_private_input_denial(self):
        base={**self.config,'schemaVersion':2,'transportTimeoutSeconds':180}
        configurations=[base,{**base,'reasoningEffort':'low'}]
        bodies=[];digests=[]
        for config in configurations:
            with patch.object(client,'read_private',side_effect=lambda p,*a:config if str(p)=='settings' else self.source),patch.object(client,'key',return_value='synthetic'),patch.object(client,'exchange',return_value=self.response) as send:
                result=client.reference('settings','source')
            bodies.append(send.call_args.args[2]);digests.append(result['configurationSha256'])
        self.assertNotIn('reasoning',bodies[0])
        self.assertEqual({'effort':'low','exclude':True},bodies[1]['reasoning'])
        self.assertEqual(bodies[0]['max_tokens'],bodies[1]['max_tokens'])
        self.assertEqual(bodies[0]['provider'],bodies[1]['provider'])
        self.assertNotEqual(digests[0],digests[1])
        private_source={**self.source,'classification':'TENANT_PRIVATE'}
        with patch.object(client,'read_private',side_effect=lambda p,*a:configurations[1] if str(p)=='settings' else private_source),patch.object(client,'exchange') as send,self.assertRaises(ValueError):
            client.reference('settings','source')
        send.assert_not_called()

    def test_reasoning_effort_rejects_unreviewed_values_and_legacy_schema(self):
        for value in (True,None,[],{},'none','LOW','low\n',1024):
            with self.subTest(value=value),patch.object(client,'read_private',return_value={**self.config,'schemaVersion':2,'transportTimeoutSeconds':180,'reasoningEffort':value}),self.assertRaises(ValueError):
                client.settings('settings')
        with patch.object(client,'read_private',return_value={**self.config,'reasoningEffort':'low'}),self.assertRaises(ValueError):
            client.settings('settings')

if __name__=='__main__':unittest.main()
