"""Run the fixed English-to-Shona holdout with paired, protected API attempts."""
from collections import Counter
import json
from pathlib import Path
import runpy
from statistics import median
import sys

ARTIFACTS = Path(__file__).resolve().parent
prior = runpy.run_path(str(ARTIFACTS.parent/'openai-shona-20261118/matched-runner.py'))
client, digest, verified, score = (prior[k] for k in ('client','digest','verified','score'))
HOME = Path.home()
ROOT = HOME/'DzokaOpenAI'
RUN = ROOT/'en-sn-heldout-20261118-v1'

def load_cases():
    paths = Path('.audit-cache/public-reference-20261106')
    en = verified(paths/'eng_Latn.devtest','612e9fbe87997617c0fa8fa8929654a4f49b728d96738112c2b86ef6a1d78d88').decode().splitlines()
    sn = verified(paths/'sna_Latn.devtest','92cb2659b91ef2ad96b88b3816b5837816d29240cbb2ea4197c9ac090f4dd1bb').decode().splitlines()
    smol = verified(Path('.audit-cache/smol-reference-20261116/en_sn.jsonl'),'8726011841a3eb23532ee11385dc4c3106fbe3e026d935150c0b6fcf97a3decf')
    rows = [json.loads(line) for line in smol.splitlines()[32:40]]
    if len(rows)!=8 or any((row['sl'],row['tl'])!=('en','sn') for row in rows):
        raise ValueError('SmolSent held-out direction or coverage differs')
    flores_url = 'https://github.com/facebookresearch/flores/tree/a6c830c6e1051fb4ac1a44b32358f00463f332bd/flores200'
    smol_url = 'https://huggingface.co/datasets/google/smol/tree/fdaff3a1a019f89fa30a562a85d7c1d3e9150444'
    cases = [('FLORES',i,en[i-1],sn[i-1],flores_url) for i in range(241,249)]
    cases += [('SmolSent',i,r['src'],r['trg'],smol_url) for i,r in enumerate(rows,33)]
    seen = {r[k] for r in prior['load_cases']() for k in ('inputSha256','referenceSha256')}
    for folder in HOME.glob('Dzoka*'):
        if not any(part in folder.name for part in ('Translator','Flores','Madlad','AfriSlm','Morena','Qwen','Gemini','Shona')):
            continue
        plan_path = folder/'plan.json'
        if plan_path.is_file():
            plan = json.loads(plan_path.read_bytes())
            for row in plan.get('rows',[]):
                if isinstance(row,dict):
                    seen.update(row.get(k) for k in ('inputSha256','referenceSha256'))
    # The existing MORENA context was sampled from SmolSent rows 257–259.
    for name,sha in [('en_sn.jsonl','8726011841a3eb23532ee11385dc4c3106fbe3e026d935150c0b6fcf97a3decf'),
                     ('sn_en.jsonl','a79080c195dc705c01ce9c4e1b800830d763f5f911c32664b4efe8db30d4053d')]:
        raw=verified(Path('.audit-cache/smol-reference-20261116')/name,sha)
        for line in raw.splitlines()[256:259]:
            row=json.loads(line)
            seen.update(client.sha(row[k].encode()) for k in ('src','trg'))
    selected=set()
    for _,_,source,reference,_ in cases:
        hashes={client.sha(v.encode()) for v in (source,reference)}
        if len(hashes)!=2 or hashes & (selected|seen):
            raise ValueError('Held-out content overlaps a checked reference or prior case')
        selected.update(hashes)
    return cases

def run():
    cases=load_cases()
    baseline_path=ROOT/'settings.json'
    baseline=client.settings(baseline_path)
    if baseline['schemaVersion']!=1 or baseline['model']!=client.MODEL or not baseline['enabled']:
        raise ValueError('Expected active schema 1 configuration')
    new={**baseline,'schemaVersion':2,'promptVersion':client.TARGET_SHONA_PROMPT}
    candidate_path=ROOT/'settings-english-to-shona-v3-reference-20261118.json'
    RUN.mkdir()
    client.protect(RUN)
    client.write_private(candidate_path,new)
    hashes={name:client.sha(json.dumps(v,sort_keys=True).encode()) for name,v in [('baseline',baseline),('candidate',new)]}
    plan={'scope':'PUBLIC_REFERENCE_ONLY','direction':'en-sn','model':baseline['model'],'maximumCalls':32,
          'outputTokenCeiling':baseline['maxOutputTokens'],'configurationSha256':hashes,
          'runnerSha256':digest(Path(__file__)),'clientSha256':digest(Path('tools/openai_connection.py')),
          'scorerSha256':digest(Path('tools/model_evaluation.py')),
          'caseManifest':[{'dataset':d,'row':i,'inputSha256':client.sha(x.encode()),'referenceSha256':client.sha(y.encode())} for d,i,x,y,_ in cases],
          'screen':{'completePerDataset':8,'maxMedianCer':0.25,'maxWorstCer':0.60},
          'order':'Alternate the first-tested prompt by case ordinal'}
    client.write_private(RUN/'plan.json',plan)
    plan_hash=digest(RUN/'plan.json')
    observations=[]
    halted=False
    for ordinal,(_,_,source,reference,url) in enumerate(cases):
        identity=plan['caseManifest'][ordinal]
        public=RUN/f'{ordinal:02}.input.json'
        client.write_private(public,{'classification':'PUBLIC_REFERENCE','task':'TRANSLATION','sourceLanguage':'en','targetLanguage':'sn',
                                     'text':source,'sourceReference':url,'sourceSha256':identity['inputSha256']})
        order=[('baseline',baseline_path),('candidate',candidate_path)]
        if ordinal%2:
            order.reverse()
        for name,config in order:
            stem=f'{ordinal:02}.{name}'
            row={**identity,'configuration':name,'status':'NOT_DISPATCHED'}
            if not halted:
                if client.sha(json.dumps(client.settings(config),sort_keys=True).encode())!=hashes[name]:
                    raise ValueError('Configuration drift before dispatch')
                client.write_private(RUN/f'{stem}.attempt.json',{'state':'MAY_REQUIRE_RECONCILIATION','planSha256':plan_hash,'ordinal':ordinal,'configuration':name})
                try:
                    v=client.reference(config,public)
                    if v['inputSha256']!=identity['inputSha256'] or v['configurationSha256']!=hashes[name] or v['clientSourceSha256']!=plan['clientSha256']:
                        raise ValueError('Result provenance drift')
                    receipt=RUN/f'{stem}.result.json'
                    client.write_private(receipt,v)
                    row.update(status='TRUNCATED' if v['outputTruncated'] else 'AVAILABLE',
                               cer=score('cer',v['text'],reference),wer=score('wer',v['text'],reference),
                               evidenceSha256=digest(receipt),usage=v['usage'])
                except client.ReferenceUnavailable as e:
                    row.update(status='UNAVAILABLE',code=e.code,httpStatus=e.status)
                    halted=e.status in (401,403,429)
            client.write_private(RUN/f'{stem}.observation.json',row)
            observations.append(row)
            print(f'{len(observations)}/32 {name} {row["status"]}',flush=True)
    summary={'planSha256':plan_hash,'qualityApproval':'NOT_ESTABLISHED','slices':{},'observations':observations}
    for dataset in ('FLORES','SmolSent'):
        by_config={}
        for name in hashes:
            group=[r for r in observations if r['dataset']==dataset and r['configuration']==name]
            complete=[r for r in group if r['status']=='AVAILABLE']
            by_config[name]={r['row']:r for r in complete}
            stats={'counts':dict(Counter(r['status'] for r in group)),
                   'medianCer':median(r['cer'] for r in complete) if complete else None,
                   'worstCer':max((r['cer'] for r in complete),default=None),
                   'medianWer':median(r['wer'] for r in complete) if complete else None}
            stats['screenPassed']=len(complete)==8 and stats['medianCer']<=.25 and stats['worstCer']<=.60
            summary['slices'].setdefault(dataset,{})[name]=stats
        b,c=by_config['baseline'],by_config['candidate']
        pairs=[(b[i]['cer'],c[i]['cer']) for i in sorted(b.keys()&c.keys())]
        summary['slices'][dataset]['pairs']={'complete':len(pairs),'candidateLower':sum(new<old for old,new in pairs),
                                            'equal':sum(new==old for old,new in pairs),'candidateHigher':sum(new>old for old,new in pairs)}
    summary['usage']={k:sum(r.get('usage',{}).get(k,0) for r in observations) for k in ('input_tokens','output_tokens','total_tokens')}
    client.write_private(RUN/'summary.json',summary)

if __name__=='__main__':
    if sys.argv[1:]==['--check-selection']:
        print(f'{len(load_cases())} held-out cases match pinned sources and pass checked overlap exclusions')
    elif not sys.argv[1:]:
        run()
    else:
        raise SystemExit('Unsupported arguments')
