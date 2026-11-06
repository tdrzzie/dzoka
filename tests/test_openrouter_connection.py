import copy
import hashlib
import json
from pathlib import Path
import sys
import tempfile
import unittest
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

if __name__=='__main__':unittest.main()
