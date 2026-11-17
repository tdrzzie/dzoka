import hashlib,json,statistics,sys
from pathlib import Path
repo=Path.cwd();sys.path.insert(0,str(repo/'tools'));from model_evaluation import score
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
roots=[('FLASH',Path.home()/'DzokaGeminiMatched-20261116-v1'),('PRO_DEFAULT',Path.home()/'DzokaGeminiProReference-20261117-v1'),('PRO_LOW',Path.home()/'DzokaGeminiProLowReference-20261117-v1')]
summary={};sets={};identity=lambda r:(r['direction'],r['slice'],r['row'],r['inputSha256'],r['referenceSha256'])
for label,root in roots:
 plan=json.loads((root/'plan.json').read_bytes());result=json.loads((root/'summary.json').read_bytes());assert result['planSha256']==sha(root/'plan.json');rows=result['rows'];assert len(rows)==(20 if label=='FLASH' else 32);sets[label]={identity(r):r for r in rows};assert len(sets[label])==len(rows)
 for index,row in enumerate(rows):
  assert (root/f'result-{index:03d}.json.attempt.json').is_file()
  for key in ('direction','slice','row'):assert row[key]==plan['rows'][index][key]
  if 'resultSha256' in row:
   path=root/f'result-{index:03d}.json';assert row['resultSha256']==sha(path);value=json.loads(path.read_bytes());assert value['inputSha256']==row['inputSha256'] and value['configuredModel']==value['returnedModel'] and value['upstreamProvider']=='Google'
   assert value['scope']=='PUBLIC_REFERENCE_ONLY' and value['trust']=='UNVERIFIED_TRANSLATION' and value['approval']=='NOT_ESTABLISHED'
   if label!='FLASH':assert value['clientSourceSha256']==plan['clientSourceSha256'] and value['configurationSha256']==plan['configurationSha256']
 stats={}
 for direction,slice_name in [('sn-en','development'),('sn-en','additional'),('en-sn','additional'),('sn-en','authored-code-switch')]:
  group=[r for r in rows if (r['direction'],r['slice'])==(direction,slice_name)]
  if not group:continue
  complete=[r for r in group if r['status']=='AVAILABLE'];values=[r['cer'] for r in complete]
  fragments=sum(len(r.get('expectedOutputFragments',[])) for r in group);matched=sum(sum(r.get('expectedOutputFragmentMatches',[])) for r in complete)
  stats[direction+'/'+slice_name]={'planned':len(group),'complete':len(complete),'medianCer':statistics.median(values) if values else None,'worstCer':max(values) if values else None,'literalMatched':matched,'literalTotal':fragments,'screenPass':len(complete)>=8 and statistics.median(values)<=0.25 and max(values)<=0.60 and matched==fragments}
 summary[label]={'planSha256':sha(root/'plan.json'),'resultSha256':sha(root/'summary.json'),'counts':{s:sum(r['status']==s for r in rows) for s in ['AVAILABLE','TRUNCATED','UNAVAILABLE']},'slices':stats,'usage':{k:sum(r.get('usage',{}).get(k,0) for r in rows) for k in ['prompt_tokens','completion_tokens','total_tokens']}}
for baseline,candidate in [('FLASH','PRO_DEFAULT'),('FLASH','PRO_LOW'),('PRO_DEFAULT','PRO_LOW')]:
 left,right=sets[baseline],sets[candidate];common=left.keys()&right.keys();pairs=[(left[k],right[k]) for k in sorted(common) if left[k]['status']==right[k]['status']=='AVAILABLE'];summary[candidate]['matchedAgainst'+baseline]={'sameInputReferenceCases':len(common),'completePairs':len(pairs),'better':sum(b['cer']<a['cer'] for a,b in pairs),'ties':sum(b['cer']==a['cer'] for a,b in pairs),'worse':sum(b['cer']>a['cer'] for a,b in pairs)}
Path('.test-tmp/hosted-pro-summary-20261117.json').write_text(json.dumps(summary,indent=2)+'\n',encoding='utf-8');print(json.dumps(summary,indent=2))
