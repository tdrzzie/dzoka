import hashlib,json,os,subprocess,sys
from pathlib import Path
repo=Path.cwd();sys.path.insert(0,str(repo/'tools'))
from broker_bundle import protect
from openrouter_connection import settings
from model_evaluation import score
sha=lambda raw:hashlib.sha256(raw).hexdigest()
reference=repo/'.audit-cache/public-reference-20261106'
def member(name,digest):
 raw=(reference/name).read_bytes();assert sha(raw)==digest
 return raw.decode().splitlines()
en=member('eng_Latn.devtest','612e9fbe87997617c0fa8fa8929654a4f49b728d96738112c2b86ef6a1d78d88')
sn=member('sna_Latn.devtest','92cb2659b91ef2ad96b88b3816b5837816d29240cbb2ea4197c9ac090f4dd1bb')
cases=[]
for direction,indices,slice_name in [('sn-en',range(65,73),'development'),('sn-en',range(193,201),'additional'),('en-sn',range(193,201),'additional')]:
 for n in indices:
  source,target=(sn[n-1],en[n-1]) if direction=='sn-en' else (en[n-1],sn[n-1])
  cases.append({'direction':direction,'row':n,'slice':slice_name,'source':source,'reference':target,'expectedOutputFragments':[]})
authored=[
 ('Ndasiya black Lenovo bag muJoina around 2pm.','I left a black Lenovo bag at Joina around 2pm.',['Lenovo','Joina','around 2pm']),
 ('Ndarasa blue Samsung phone paAvondale yesterday.','I lost a blue Samsung phone at Avondale yesterday.',['Samsung','Avondale','yesterday']),
 ('Ndakawana red backpack paMbare bus terminus at 6am.','I found a red backpack at Mbare bus terminus at 6am.',['Mbare','6am']),
 ('Handina kurasa wallet yangu; ndine black bag only.','I did not lose my wallet; I only have a black bag.',['not','black']),
 ('Ndakasiya silver HP laptop near Borrowdale maybe on Monday.','I left a silver HP laptop near Borrowdale maybe on Monday.',['HP','Borrowdale','maybe','Monday']),
 ('Ndakawana umbrella yered paEastgate around noon.','I found a red umbrella at Eastgate around noon.',['Eastgate','around noon']),
 ('Ndiri kutsvaga green suitcase ine Adidas logo.','I am looking for a green suitcase with an Adidas logo.',['Adidas','green']),
 ('Bag racho rine ZX-742 label, asi handizivi owner.','The bag has a ZX-742 label, but I do not know the owner.',['ZX-742','not']),
]
cases += [{'direction':'sn-en','row':n+1,'slice':'authored-code-switch','source':source,'reference':target,'expectedOutputFragments':critical} for n,(source,target,critical) in enumerate(authored)]
config=Path.home()/'DzokaOpenRouter/settings-gemini-pro-low-reference-20261117.json'
value=settings(config);assert value['enabled'] and value['model']=='google/gemini-3.1-pro-preview' and value['providers']==['Google'] and value['maxOutputTokens']==1024 and value['temperature'] is None and value['reasoningEffort']=='low'
prior=Path.home()/'DzokaQwen9BLowMemoryDiagnostic-20261117-v1/plan.json';prior_plan=json.loads(prior.read_bytes())
selection=[{k:v for k,v in row.items() if k not in ('source','reference')}|{'inputSha256':sha(row['source'].encode()),'referenceSha256':sha(row['reference'].encode())} for row in cases]
assert selection==prior_plan['rows'] and len(cases)==32
root=Path.home()/'DzokaGeminiProLowReference-20261117-v1';root.mkdir();protect(root)
plan={'scope':'PUBLIC_REFERENCE_ONLY; licensed FLORES and previously public first-party synthetic cases, no tenant data','sourcePlanSha256':sha(prior.read_bytes()),'configurationSha256':sha(json.dumps(value,sort_keys=True).encode()),'configurationFileSha256':sha(config.read_bytes()),'model':value['model'],'providers':value['providers'],'rows':selection,'screen':prior_plan['screen'],'endpointCatalogSha256':sha((repo/'.audit-cache/qwen9b-review-20261116/gemini-pro-endpoints-20261117.json').read_bytes()),'scriptSha256':sha(Path(__file__).read_bytes()),'clientSourceSha256':sha((repo/'tools/openrouter_connection.py').read_bytes()),'promptVersion':'named-languages-v2','maximumCalls':32,'outputTokenCeilingPerCall':1024,'noAutomaticRetryOrFallback':True,'requestedReasoningEffort':'low'}
(root/'plan.json').write_text(json.dumps(plan,indent=2)+'\n',encoding='utf-8');rows=[]
for index,case in enumerate(cases):
 envelope={'classification':'PUBLIC_REFERENCE','task':'TRANSLATION','sourceLanguage':'sn' if case['direction']=='sn-en' else 'en','targetLanguage':'en' if case['direction']=='sn-en' else 'sn','text':case['source'],'sourceReference':'https://github.com/tdrzzie/dzoka/blob/cf763023846ba4d632c340194f28bed5739d3c68/docs/testing/artifacts/m4-language-20261116/afrislm4b-evaluate-20261116.py' if case['slice']=='authored-code-switch' else 'https://dl.fbaipublicfiles.com/nllb/flores200_dataset.tar.gz','sourceSha256':selection[index]['inputSha256']}
 input_path=root/f'input-{index:03d}.json';input_path.write_text(json.dumps(envelope)+'\n',encoding='utf-8');protect(input_path);output=root/f'result-{index:03d}.json';row={**selection[index],'status':'UNAVAILABLE'}
 try:
  run=subprocess.run([sys.executable,'-I','-B',str(repo/'tools/openrouter_connection.py'),'reference','--config',str(config),'--input',str(input_path),'--output',str(output)],capture_output=True,timeout=220)
  if run.returncode==0:
   result=json.loads(output.read_bytes());assert result['configuredModel']==result['returnedModel']==value['model'] and result['upstreamProvider']=='Google' and result['inputSha256']==row['inputSha256'] and result['configurationSha256']==plan['configurationSha256'] and result['clientSourceSha256']==plan['clientSourceSha256']
   row.update(status='TRUNCATED' if result['outputTruncated'] else 'AVAILABLE',cer=score('cer',result['text'],case['reference']),resultSha256=sha(output.read_bytes()),expectedOutputFragmentMatches=[x in result['text'] for x in case['expectedOutputFragments']],usage=result['usage'])
 except Exception as error:row['errorType']=type(error).__name__
 failure=Path(str(output)+'.failure.json')
 if failure.exists():row['failure']=json.loads(failure.read_bytes())
 rows.append(row);temporary=root/'summary.pending';temporary.write_text(json.dumps({'planSha256':sha((root/'plan.json').read_bytes()),'rows':rows},indent=2)+'\n',encoding='utf-8');os.replace(temporary,root/'summary.json');print(index,case['direction'],case['row'],row['status'],row.get('cer'),row.get('failure',{}).get('category'),flush=True)
 if index==0 and row['status']=='UNAVAILABLE':raise SystemExit('First hosted compatibility request unavailable; no replay and remaining calls deferred')
print('COMPLETED',sha((root/'summary.json').read_bytes()),flush=True)
