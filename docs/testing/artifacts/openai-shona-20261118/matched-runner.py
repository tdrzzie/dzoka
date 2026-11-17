"""Reproduce a matched, public Shona reference comparison without loading upstream code."""
import ast
from collections import Counter
import hashlib
import json
from pathlib import Path
from statistics import median
import sys
import time

sys.path[:0] = [str(Path.cwd() / 'tools')]
import openai_connection as client
from model_evaluation import score

repo = Path.cwd()
root = Path.home() / 'DzokaOpenAI'
run = root / 'shona-matched-20261118-v1'
config_path = root / 'settings.json'
sha = client.sha
def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def verified(path, expected):
    raw = path.read_bytes()
    if sha(raw) != expected:
        raise ValueError('Reference or historical result hash mismatch')
    return raw

def read(path):
    return json.loads(path.read_bytes())

def identity(row):
    return tuple(row[k] for k in ('direction','slice','row','inputSha256','referenceSha256'))

def load_cases():
    base = repo / '.audit-cache/public-reference-20261106'
    en = verified(base/'eng_Latn.devtest','612e9fbe87997617c0fa8fa8929654a4f49b728d96738112c2b86ef6a1d78d88').decode().splitlines()
    sn = verified(base/'sna_Latn.devtest','92cb2659b91ef2ad96b88b3816b5837816d29240cbb2ea4197c9ac090f4dd1bb').decode().splitlines()
    cases = []
    def add(direction, slice_name, row, source, target, fragments=()):
        cases.append(dict(direction=direction,slice=slice_name,row=row,source=source,reference=target,expectedOutputFragments=list(fragments),inputSha256=sha(source.encode()),referenceSha256=sha(target.encode())))
    for direction, label, numbers in [('sn-en','development',range(65,73)),('sn-en','additional',range(193,201)),('en-sn','additional',range(193,201))]:
        source,target = (sn,en) if direction == 'sn-en' else (en,sn)
        for n in numbers:
            add(direction,label,n,source[n-1],target[n-1])
    tree = ast.parse((repo/'.test-tmp/afrislm-evaluate-20261116.py').read_bytes())
    authored = next(ast.literal_eval(n.value) for n in tree.body if isinstance(n,ast.Assign) and any(isinstance(x,ast.Name) and x.id=='authored' for x in n.targets))
    for n,(source,target,fragments) in enumerate(authored,1):
        add('sn-en','authored-code-switch',n,source,target,fragments)
    smol = repo/'.audit-cache/smol-reference-20261116'
    for direction,file,expected in [('en-sn','en_sn.jsonl','8726011841a3eb23532ee11385dc4c3106fbe3e026d935150c0b6fcf97a3decf'),('sn-en','sn_en.jsonl','a79080c195dc705c01ce9c4e1b800830d763f5f911c32664b4efe8db30d4053d')]:
        for n,line in enumerate(verified(smol/file,expected).splitlines()[:8],1):
            row=json.loads(line)
            if (row['sl'],row['tl']) != tuple(direction.split('-')):
                raise ValueError('Reference direction mismatch')
            add(direction,'smol-additional',n,row['src'],row['trg'])
    return cases

HISTORIES = {
    'CPU baseline':('DzokaAfriSlmBaseline-20261116-v1',32,None),
    'TranslatePsy 0.8B':('DzokaAfriSlmDiagnostic-20261116-v1',32,'32adff97476b2a76b93e2a721fc68f63429afb5a3d2bc6e2cbc8eb91c88ae4a3'),
    'TranslatePsy 4B':('DzokaAfriSlm4BDiagnostic-20261116-v1',32,'3ed0f9ab218fe5f5cdb90c6455a5455818614cfc8b1313e3b98115b539b1dce5'),
    'MORENA zero-shot':('DzokaMorenaDiagnostic-20261116-v1',48,'1b3c41a080b3959fcd00fe7f8a9212904ef7d00a000a0149cc65e94dd3936f1b'),
    'MORENA three-shot':('DzokaMorenaFewshotDiagnostic-20261116-v2',48,'619e5e5f7812f6acfcb200887e4b5a75485b1d747f3a807c96a420db75fca505'),
}
def historical(cases):
    results={}
    for name,(folder,count,expected) in HISTORIES.items():
        directory=Path.home()/folder
        path=directory/'result.json'
        result=json.loads(verified(path,expected)) if expected else read(path)
        plan_path=directory/'plan.json'
        plan=json.loads(verified(plan_path,result['planSha256']))
        if len(result['rows'])!=count or len(plan['rows'])!=count:
            raise ValueError('Historical population mismatch')
        for i,(row,planned,case) in enumerate(zip(result['rows'],plan['rows'],cases)):
            if identity(row)!=identity(case) or identity(planned)!=identity(case) or row['expectedOutputFragments']!=case['expectedOutputFragments']:
                raise ValueError('Historical case mismatch')
            if row['status'] in ('AVAILABLE','TRUNCATED'):
                value=json.loads(verified(directory/f'{i:03}.json',row['evidenceSha256']))
                if abs(score('cer',value['text'],case['reference'])-row['cer'])>1e-12:
                    raise ValueError('Historical score mismatch')
        results[name]={'rows':result['rows'],'resultSha256':digest(path),'planSha256':result['planSha256']}
    return results

def stats(rows):
    complete=[r for r in rows if r['status']=='AVAILABLE']
    return {'counts':dict(Counter(r['status'] for r in rows)),
            'medianCer':median(r['cer'] for r in complete) if complete else None,
            'worstCer':max((r['cer'] for r in complete),default=None),
            'literalMatched':sum(sum(r.get('literalMatches',r.get('expectedOutputFragmentMatches',[]))) for r in complete)}

def evaluate():
    cases=load_cases()
    comparisons=historical(cases)
    config=client.settings(config_path)
    if not config['enabled'] or config['model']!=client.MODEL:
        raise ValueError('Unexpected OpenAI configuration')
    config_digest=sha(json.dumps(config,sort_keys=True).encode())
    client_digest=digest(repo/'tools/openai_connection.py')
    run.mkdir()
    client.protect(run)
    plan={'scope':'PUBLIC_REFERENCE_ONLY','cases':[dict(zip(('direction','slice','row','inputSha256','referenceSha256'),identity(c))) for c in cases],
          'configuredModel':config['model'],'configurationSha256':config_digest,'clientSha256':client_digest,
          'runnerSha256':digest(Path(__file__)),'scorerSha256':digest(repo/'tools/model_evaluation.py'),
          'screen':{'completePerSlice':8,'maximumMedianCer':.25,'maximumCaseCer':.60,'codeSwitchLiteralMatches':20},
          'comparisons':{n:{k:v for k,v in r.items() if k!='rows'} for n,r in comparisons.items()}}
    client.write_private(run/'plan.json',plan)
    rows=[]
    blocked=False
    for i,case in enumerate(cases):
        row={**plan['cases'][i],'status':'NOT_DISPATCHED'}
        if not blocked:
            source,target=case['direction'].split('-')
            urls={'authored-code-switch':'https://github.com/tdrzzie/dzoka/blob/643d76e/docs/testing/m4-african-cpu-quality-2026-11-16.md',
                  'smol-additional':'https://huggingface.co/datasets/google/smol/tree/fdaff3a1a019f89fa30a562a85d7c1d3e9150444'}
            envelope={'classification':'PUBLIC_REFERENCE','task':'TRANSLATION','sourceLanguage':source,'targetLanguage':target,
                      'text':case['source'],'sourceSha256':case['inputSha256'],
                      'sourceReference':urls.get(case['slice'],'https://github.com/facebookresearch/flores/tree/a6c830c6e1051fb4ac1a44b32358f00463f332bd/flores200')}
            input_path=run/f'{i:03}.input.json'
            client.write_private(input_path,envelope)
            client.write_private(run/f'{i:03}.attempt.json',{'state':'MAY_REQUIRE_RECONCILIATION','case':i,'planSha256':digest(run/'plan.json')})
            try:
                if sha(json.dumps(client.settings(config_path),sort_keys=True).encode())!=config_digest:
                    raise ValueError('Configuration changed')
                value=client.reference(config_path,input_path)
                if value['configurationSha256']!=config_digest or value['clientSourceSha256']!=client_digest:
                    raise ValueError('Returned provenance differs')
                receipt=run/f'{i:03}.json'
                client.write_private(receipt,value)
                row.update(status='TRUNCATED' if value['outputTruncated'] else 'AVAILABLE',
                           cer=score('cer',value['text'],case['reference']),evidenceSha256=digest(receipt),
                           literalMatches=[fragment in value['text'] for fragment in case['expectedOutputFragments']],usage=value['usage'])
            except client.ReferenceUnavailable as error:
                row.update(status='UNAVAILABLE',code=error.code,httpStatus=error.status)
                blocked=error.status in (401,403,429)
        client.write_private(run/f'{i:03}.observation.json',row)
        rows.append(row)
        print(f'{i+1}/48 {row["status"]} {row.get("httpStatus","")}',flush=True)
    summary={'planSha256':digest(run/'plan.json'),'overall':stats(rows),'slices':{},'pairs':{},
             'usage':{k:sum(r.get('usage',{}).get(k,0) for r in rows) for k in ('input_tokens','output_tokens','total_tokens')},
             'qualityApproval':'NOT_ESTABLISHED'}
    groups=list(dict.fromkeys((r['direction'],r['slice']) for r in rows))
    for direction,label in groups:
        group=[r for r in rows if (r['direction'],r['slice'])==(direction,label)]
        result=stats(group)
        result['screenPassed']=result['counts'].get('AVAILABLE')==8 and result['medianCer']<=.25 and result['worstCer']<=.60 and (label!='authored-code-switch' or result['literalMatched']==20)
        summary['slices'][direction+'/'+label]=result
    for name,evidence in comparisons.items():
        historical_rows=evidence['rows']
        pairs=[(a,b) for a,b in zip(rows,historical_rows) if a['status']==b['status']=='AVAILABLE']
        summary['pairs'][name]={'count':len(pairs),'lowerCer':sum(a['cer']<b['cer'] for a,b in pairs),
            'ties':sum(a['cer']==b['cer'] for a,b in pairs),'higherCer':sum(a['cer']>b['cer'] for a,b in pairs),
            'slices':{direction+'/'+label:stats([r for r in historical_rows if (r['direction'],r['slice'])==(direction,label)]) for direction,label in groups if any((r['direction'],r['slice'])==(direction,label) for r in historical_rows)}}
    client.write_private(run/'summary.json',summary)
    print(json.dumps(summary),flush=True)

if __name__=='__main__':
    evaluate()
