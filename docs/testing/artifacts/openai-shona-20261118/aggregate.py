"""Offline aggregation for the 2026-11-18 OpenAI Shona comparison."""
import json
from pathlib import Path
import runpy
from statistics import median

directory=Path(__file__).resolve().parent
m=runpy.run_path(str(directory/'matched-runner.py'))
cases=m['load_cases']()
root=m['run']
read,digest,verified,identity=(m[k] for k in ('read','digest','verified','identity'))
plan=read(root/'plan.json')
summary=read(root/'summary.json')
assert summary['planSha256']==digest(root/'plan.json')
assert m['digest'](directory/'matched-runner.py')==plan['runnerSha256']
rows=[read(root/f'{i:03}.observation.json') for i in range(len(cases))]
for i,(case,row) in enumerate(zip(cases,rows)):
    assert identity(case)==identity(row)==identity(plan['cases'][i])
    if row['status'] in ('AVAILABLE','TRUNCATED'):
        result=json.loads(verified(root/f'{i:03}.json',row['evidenceSha256']))
        assert result['inputSha256']==case['inputSha256']
        assert result['configurationSha256']==plan['configurationSha256']
        assert result['clientSourceSha256']==plan['clientSha256']
        assert m['score']('cer',result['text'],case['reference'])==row['cer']
hosted={}
for name,folder,expected in [
 ('Gemini 3 Flash Preview','DzokaGeminiMatched-20261116-v1','57493e6cfb2b25c3960cf9bfa213dd8da692d95a143de1f75fc4a3b3424cc984'),
 ('Qwen 235B','DzokaQwenFlores-20261116-v2','08964e714038a65745ce4ad5b1c9d140ca732f473fe80007784fe9813ae379ec'),
 ('Gemini Pro medium','DzokaGeminiProReference-20261117-v1','564b1e49fae7d6a57946fb3067de552ea276e7337745349a201ea1cbcc309e40'),
 ('Gemini Pro low','DzokaGeminiProLowReference-20261117-v1','faf31d6eee4530fe72b477e16262e17ed2180947aec3c7f8de91ae3eba43ae4e')]:
    other=Path.home()/folder
    report=json.loads(verified(other/'summary.json',expected))
    old_plan=json.loads(verified(other/'plan.json',report['planSha256']))
    key=lambda row:(row['direction'],row['inputSha256'],row['referenceSha256'])
    indexed={key(case):(case,row) for case,row in zip(cases,rows)}
    matches=[]
    for i,row in enumerate(report['rows']):
        if key(row) not in indexed:
            continue
        case,current=indexed[key(row)]
        assert row['row']==case['row']==old_plan['rows'][i]['row']
        if row['status'] in ('AVAILABLE','TRUNCATED'):
            result=json.loads(verified(other/f'result-{i:03}.json',row['resultSha256']))
            assert result['inputSha256']==case['inputSha256']
            assert abs(m['score']('cer',result['text'],case['reference'])-row['cer'])<1e-12
        matches.append((current,row))
    pairs=[(a,b) for a,b in matches if a['status']==b['status']=='AVAILABLE']
    hosted[name]={'summarySha256':expected,'matchedCases':len(matches),'historicalComplete':sum(b['status']=='AVAILABLE' for _,b in matches),
                  'commonComplete':len(pairs),'openaiLowerCer':sum(a['cer']<b['cer'] for a,b in pairs),
                  'equalCer':sum(a['cer']==b['cer'] for a,b in pairs),'openaiHigherCer':sum(a['cer']>b['cer'] for a,b in pairs),
                  'openaiMedianOnPairs':median(a['cer'] for a,_ in pairs) if pairs else None,
                  'historicalMedianOnPairs':median(b['cer'] for _,b in pairs) if pairs else None}
record={'evidence':{'planSha256':digest(root/'plan.json'),'privateSummarySha256':digest(root/'summary.json'),
                    'clientSha256':plan['clientSha256'],'configurationSha256':plan['configurationSha256'],
                    'runnerSha256':plan['runnerSha256'],'scorerSha256':plan['scorerSha256'],
                    'localComparisons':plan['comparisons']},
        'openai':summary,'hostedComparisons':hosted}
with (directory/'summary.json').open('x',encoding='utf-8',newline='\n') as output:
    json.dump(record,output,indent=2,allow_nan=False)
    output.write('\n')
