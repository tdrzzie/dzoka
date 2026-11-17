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
import openai_connection as client

class OpenAIConnectionTests(unittest.TestCase):
    def setUp(self):
        self.config={'schemaVersion':1,'enabled':True,'model':client.MODEL,'maxOutputTokens':1024,'transportTimeoutSeconds':180,'referenceOnly':True,'store':False,'reasoningEffort':'low'}
        self.source={'classification':'PUBLIC_REFERENCE','task':'TRANSLATION','sourceLanguage':'en','targetLanguage':'sn','text':'Hello, world!','sourceReference':'https://en.wikipedia.org/wiki/Hello_world','sourceSha256':hashlib.sha256(b'Hello, world!').hexdigest()}
        self.response={'id':'resp_test','model':client.MODEL,'status':'completed','output':[{'type':'message','role':'assistant','content':[{'type':'output_text','text':'Mhoro, nyika!'}]}],'usage':{'input_tokens':40,'output_tokens':5,'total_tokens':45}}
    def read(self,path,*args):return self.config if str(path)=='settings' else self.source
    def run_reference(self,response=None):
        with patch.object(client,'read_private',side_effect=self.read),patch.object(client,'key',return_value='never-return-this-secret'),patch.object(client,'exchange',return_value=response or self.response) as send:
            return client.reference('settings','input'),send.call_args
    def test_provenance_and_no_tools_storage_or_secret(self):
        result,call=self.run_reference();body=call.args[2]
        self.assertFalse(body['store']);self.assertFalse(body['stream']);self.assertEqual([],body['tools']);self.assertEqual('disabled',body['truncation'])
        self.assertEqual(self.source['sourceSha256'],result['inputSha256']);self.assertEqual('NOT_ESTABLISHED',result['approval']);self.assertEqual('NOT_ATTESTED',result['zeroDataRetention'])
        self.assertNotIn('never-return-this-secret',json.dumps(result));self.assertEqual('Mhoro, nyika!',result['text']);self.assertFalse(result['outputTruncated'])
    def test_private_tampered_or_unsupported_input_never_sends(self):
        for field,value in [('classification','TENANT_PRIVATE'),('sourceSha256','0'*64),('sourceReference','file:///secret'),('targetLanguage','invalid'),('text','x'*8001)]:
            with self.subTest(field=field):
                original=self.source;self.source={**original,field:value}
                with patch.object(client,'read_private',side_effect=self.read),patch.object(client,'exchange') as send,self.assertRaises(ValueError):client.reference('settings','input')
                send.assert_not_called();self.source=original
    def test_disabled_and_invalid_settings_never_send(self):
        for field,value in [('enabled',False),('store',True),('referenceOnly',False),('schemaVersion',True),('transportTimeoutSeconds',181),('maxOutputTokens',100000),('model','https://evil'),('reasoningEffort','arbitrary')]:
            original=self.config;self.config={**original,field:value}
            with patch.object(client,'read_private',side_effect=self.read),patch.object(client,'exchange') as send,self.assertRaises(ValueError):client.reference('settings','input')
            send.assert_not_called();self.config=original
    def test_refusals_tool_calls_wrong_model_and_usage_rejected(self):
        changes=[('model','unapproved'),('status','failed'),('usage',{'input_tokens':True}),('output',[{'type':'function_call'}]),('output',[{'type':'message','role':'assistant','content':[{'type':'refusal','refusal':'no'}]}])]
        for field,value in changes:
            response={**self.response,field:value}
            with self.assertRaises(ValueError):self.run_reference(response)
    def test_partial_output_retains_truncation_without_claiming_completion(self):
        response={**self.response,'status':'incomplete','incomplete_details':{'reason':'max_output_tokens'}}
        result,_=self.run_reference(response);self.assertTrue(result['outputTruncated']);self.assertEqual('incomplete',result['status'])
        response['incomplete_details']['reason']='content_filter'
        with self.assertRaises(ValueError):self.run_reference(response)
    def test_opt_in_shona_prompt_keeps_limits_and_records_distinct_provenance(self):
        legacy,old_call=self.run_reference()
        self.config.update(schemaVersion=2,promptVersion=client.FIDELITY_PROMPT)
        candidate,new_call=self.run_reference()
        old_body=old_call.args[2];new_body=new_call.args[2].copy()
        self.assertNotEqual(old_body['instructions'],new_body.pop('instructions'))
        self.assertEqual({k:v for k,v in old_body.items() if k!='instructions'},new_body)
        self.assertEqual(client.LEGACY_PROMPT,legacy['promptVersion'])
        self.assertEqual(client.FIDELITY_PROMPT,candidate['promptVersion'])
        for digest in ('configurationSha256','promptSha256'):
            self.assertNotEqual(legacy[digest],candidate[digest])
        self.source['targetLanguage']='fr'
        with self.assertRaises(ValueError):self.run_reference()
        self.config['promptVersion']='unknown'
        with self.assertRaises(ValueError):self.run_reference()
    def test_fixed_routes_json_and_redirect_guards(self):
        for route in ('https://evil','/responses/other','/models/../secret'):
            with self.assertRaises(ValueError):client.exchange(route,'synthetic')
        with self.assertRaises(ValueError):client.NoRedirect().redirect_request(None,None,302,'',{},'https://evil')
        for raw in ('{"x":1,"x":2}','{"x":NaN}'):
            with self.assertRaises(ValueError):client.parse(raw)
    def test_http_error_is_safe_and_not_retried(self):
        error=urllib.error.HTTPError(client.ORIGIN+'/responses',401,'private provider body',{},None)
        with patch.object(client.urllib.request,'build_opener') as make:
            make.return_value.open.side_effect=error
            with self.assertRaises(client.ReferenceUnavailable) as caught:client.exchange('/responses','synthetic-key',{})
            self.assertEqual(401,caught.exception.status);self.assertEqual('HTTP_ERROR',str(caught.exception));self.assertEqual(1,make.return_value.open.call_count)
    def test_exclusive_attempt_blocks_accidental_paid_retry(self):
        with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as folder:
            output=Path(folder)/'result.json'
            with patch.object(sys,'argv',['openai_connection.py','reference','--input','input','--output',str(output)]),patch.object(client,'protect'),patch.object(client,'private',side_effect=Path),patch.object(client,'reference',side_effect=ValueError('private error')) as invoke:
                self.assertEqual(1,client.main());self.assertEqual(1,client.main());self.assertEqual(1,invoke.call_count)
            failure=Path(str(output)+'.failure.json').read_text();self.assertNotIn('private error',failure)
    def test_setup_never_overwrites_existing_directory(self):
        with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as folder:
            p=Path(folder)/'credentials.json';p.write_text('untouched')
            with self.assertRaises(ValueError):client.setup(Path(folder)/'settings.json')
            self.assertEqual('untouched',p.read_text())
    def test_output_acl_precedes_content_and_creation_is_exclusive(self):
        with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as folder:
            path=Path(folder)/'result.json'
            def protect(p):self.assertEqual(b'',p.read_bytes())
            with patch.object(client,'protect',side_effect=protect) as guard:
                client.write_private(path,{'text':'untrusted output'});guard.assert_called_once_with(path)
                with self.assertRaises(FileExistsError):client.write_private(path,{})

if __name__=='__main__':unittest.main()
