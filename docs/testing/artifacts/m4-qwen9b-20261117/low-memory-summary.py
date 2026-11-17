import hashlib,json,statistics
from pathlib import Path
roots=[Path.home()/name for name in ['DzokaAfriSlmBaseline-20261116-v1','DzokaAfriSlmDiagnostic-20261116-v1','DzokaAfriSlm4BDiagnostic-20261116-v1','DzokaQwen9BLowMemoryDiagnostic-20261117-v1']]
sets=[];summary={}
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
for root in roots:
 plan=json.loads((root/'plan.json').read_bytes());result=json.loads((root/'result.json').read_bytes());assert result['planSha256']==sha(root/'plan.json');rows=result['rows'];assert len(rows)==32;sets.append(rows)
 for ordinal,row in enumerate(rows):
  assert all(row[k]==plan['rows'][ordinal][k] for k in ['direction','slice','row','inputSha256','referenceSha256'])
  if 'evidenceSha256' in row:assert row['evidenceSha256']==sha(root/f'{ordinal:03d}.json')
 journals=list((root/'state').glob('*.json'))
 assert len(journals)==32
 for journal in journals:
  value=json.loads(journal.read_bytes());assert value['history'][-1]['state']=='REMOVED'
 stats={}
 for direction,slice_name in [('sn-en','development'),('sn-en','additional'),('en-sn','additional'),('sn-en','authored-code-switch')]:
  group=[r for r in rows if r['direction']==direction and r['slice']==slice_name];complete=[r for r in group if r['status']=='AVAILABLE'];values=[r['cer'] for r in complete]
  stats[direction+'/'+slice_name]={'total':len(group),'complete':len(complete),'medianCer':statistics.median(values) if values else None,'worstCer':max(values) if values else None,'literalFragmentsMatched':sum(sum(r.get('expectedOutputFragmentMatches',[])) for r in complete),'literalFragmentsTotal':sum(len(r['expectedOutputFragments']) for r in group)}
 summary[root.name]={'planSha256':sha(root/'plan.json'),'resultSha256':sha(root/'result.json'),'counts':{status:sum(r['status']==status for r in rows) for status in ['AVAILABLE','TRUNCATED','UNAVAILABLE']},'slices':stats}
baseline=sets[0]
for root,rows in zip(roots[1:],sets[1:]):
 pairs=[]
 for b,c in zip(baseline,rows):
  assert all(b[k]==c[k] for k in ['direction','slice','row','inputSha256','referenceSha256'])
  if b['status']==c['status']=='AVAILABLE':pairs.append((b,c))
 summary[root.name]['matchedComplete']={'count':len(pairs),'better':sum(c['cer']<b['cer'] for b,c in pairs),'ties':sum(c['cer']==b['cer'] for b,c in pairs),'worse':sum(c['cer']>b['cer'] for b,c in pairs)}
Path('.test-tmp/qwen9b-lowmem-summary-20261117.json').write_text(json.dumps(summary,indent=2)+'\n');print(json.dumps(summary,indent=2))
