"""Replay twelve declared diagnostic cases with the explicit fidelity prompt."""
import json
from pathlib import Path
import runpy
from statistics import median

directory=Path(__file__).resolve().parent
m=runpy.run_path(str(directory/'matched-runner.py'))
client=m['client']
root=m['root']
baseline=m['run']
run=root/'shona-fidelity-20261118-v1'
baseline_plan=m['read'](baseline/'plan.json')
legacy=client.settings(root/'settings.json')
assert client.sha(json.dumps(legacy,sort_keys=True).encode())==baseline_plan['configurationSha256']
settings={**legacy,'schemaVersion':2,'promptVersion':client.FIDELITY_PROMPT}
selected=[0,12,16,17,*range(24,32)]
cases=m['load_cases']()
for i in selected:
    assert m['identity'](cases[i])==m['identity'](baseline_plan['cases'][i])
    assert m['read'](baseline/f'{i:03}.observation.json')['status']=='AVAILABLE'
run.mkdir()
client.protect(run)
config=root/'settings-fidelity-reference-20261118.json'
client.write_private(config,settings)
setup={'scope':'PUBLIC_REFERENCE_ONLY','selectedOrdinals':selected,'baselinePlanSha256':m['digest'](baseline/'plan.json'),
       'configurationSha256':client.sha(json.dumps(settings,sort_keys=True).encode()),
       'clientSha256':m['digest'](Path('tools/openai_connection.py')),
       'runnerSha256':m['digest'](Path(__file__)),
       'comparison':'Development replay of two inspected SN-EN errors, two EN-SN regressions, and the entire authored code-switch slice.'}
client.write_private(run/'plan.json',setup)
rows=[]
for i in selected:
    case=cases[i]
    prior=m['read'](baseline/f'{i:03}.observation.json')
    client.write_private(run/f'{i:03}.attempt.json',{'state':'MAY_REQUIRE_RECONCILIATION','ordinal':i,'planSha256':m['digest'](run/'plan.json')})
    row={'ordinal':i,'direction':case['direction'],'slice':case['slice'],'row':case['row'],'beforeCer':prior['cer'],'status':'UNAVAILABLE'}
    try:
        value=client.reference(config,baseline/f'{i:03}.input.json')
        assert value['configurationSha256']==setup['configurationSha256'] and value['clientSourceSha256']==setup['clientSha256']
        assert value['inputSha256']==case['inputSha256'] and value['promptVersion']==client.FIDELITY_PROMPT
        output=run/f'{i:03}.json'
        client.write_private(output,value)
        row.update(status='TRUNCATED' if value['outputTruncated'] else 'AVAILABLE',afterCer=m['score']('cer',value['text'],case['reference']),
                   literalMatched=sum(fragment in value['text'] for fragment in case['expectedOutputFragments']),
                   beforeLiteralMatched=sum(prior['literalMatches']),usage=value['usage'],evidenceSha256=m['digest'](output))
    except client.ReferenceUnavailable as error:
        row.update(code=error.code,httpStatus=error.status)
    client.write_private(run/f'{i:03}.observation.json',row)
    rows.append(row)
    print(f'{i:03} {row["status"]}',flush=True)
    if row.get('httpStatus') in (401,403,429):
        break
pairs=[row for row in rows if row['status']=='AVAILABLE']
switch=[row for row in pairs if row['slice']=='authored-code-switch']
summary={'planSha256':m['digest'](run/'plan.json'),'selected':len(selected),'observed':len(rows),'complete':len(pairs),'rows':rows,
         'lowerCer':sum(r['afterCer']<r['beforeCer'] for r in pairs),'equalCer':sum(r['afterCer']==r['beforeCer'] for r in pairs),
         'higherCer':sum(r['afterCer']>r['beforeCer'] for r in pairs),
         'codeSwitch':{'matchedComplete':len(switch),'beforeMedianCer':median(r['beforeCer'] for r in switch) if switch else None,
                       'afterMedianCer':median(r['afterCer'] for r in switch) if switch else None,
                       'beforeLiteralMatched':sum(r['beforeLiteralMatched'] for r in switch),'afterLiteralMatched':sum(r['literalMatched'] for r in switch)},
         'qualityApproval':'NOT_ESTABLISHED'}
client.write_private(run/'summary.json',summary)
print(json.dumps(summary),flush=True)
