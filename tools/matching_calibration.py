"""Fit and evaluate an offline matching calibration draft from labeled scores."""
import argparse
import json
from datetime import datetime, timezone
import math
from pathlib import Path
import re

from openrouter_connection import private, parse, sha
from openai_connection import write_private


def valid_hash(value):
    return isinstance(value,str) and re.fullmatch(r'[0-9a-f]{64}',value) is not None


def probability(value):
    return type(value) in (int,float) and math.isfinite(value) and 0<=value<=1


def evaluate(plan, plan_sha):
    fields={'schemaVersion','datasetSha256','spaceKey','scoringVersion','minimumCoverage','cases'}
    if not isinstance(plan,dict) or set(plan)!=fields or type(plan['schemaVersion']) is not int or plan['schemaVersion']!=1:
        raise ValueError('Bound labeled calibration plan required')
    if not all(valid_hash(value) for value in (plan_sha,plan['datasetSha256'],plan['spaceKey'])) or plan['scoringVersion']!='bounded-evidence-v1':
        raise ValueError('Unsupported matching identity')
    if not probability(plan['minimumCoverage']) or not isinstance(plan['cases'],list) or not 1<=len(plan['cases'])<=2048:
        raise ValueError('Invalid calibration bounds')
    cases=set()
    groups={}
    eligible={'fit':[],'holdout':[]}
    exclusions={'fit':0,'holdout':0}
    for row in plan['cases']:
        if not isinstance(row,dict) or set(row)!={'caseSha256','groupSha256','split','label','similarity','coverage','disposition'}:
            raise ValueError('Invalid labeled case')
        split=row['split']
        if split not in ('fit','holdout') or type(row['label']) is not bool or not probability(row['coverage']):
            raise ValueError('Invalid label or split')
        if not valid_hash(row['caseSha256']) or not valid_hash(row['groupSha256']) or row['caseSha256'] in cases:
            raise ValueError('Duplicate or unbound label')
        cases.add(row['caseSha256'])
        if groups.setdefault(row['groupSha256'],split)!=split:
            raise ValueError('Entity group overlaps fit and holdout')
        status=row['disposition']
        if status not in ('COMPARABLE','CONFLICT','INSUFFICIENT_EVIDENCE'):
            raise ValueError('Unknown disposition')
        similarity=row['similarity']
        if status=='COMPARABLE' and not probability(similarity):
            raise ValueError('Comparable score missing')
        if status!='COMPARABLE' or row['coverage']<plan['minimumCoverage']:
            exclusions[split]+=1
        else:
            eligible[split].append((similarity,row['label']))

    for split in eligible.values():
        if len(split)<8 or {label for _,label in split}!={False,True}:
            raise ValueError('Both outcome classes and at least eight eligible cases per split required')
    # Fixed-width bins preserve empty support instead of extrapolating.
    def bin_for(similarity): return min(4,int(similarity*5))
    fit=[[] for _ in range(5)]
    for value,label in eligible['fit']:
        fit[bin_for(value)].append(label)
    bins=[{'fitCount':len(labels),'observedFrequency':sum(labels)/len(labels) if labels else None} for labels in fit]
    pairs=[(bins[bin_for(value)]['observedFrequency'],value,label) for value,label in eligible['holdout']]
    supported=[(estimate,value,label) for estimate,value,label in pairs if estimate is not None]
    brier=lambda values: sum((estimate-label)**2 for estimate,label in values)/len(values) if values else None
    return {'schemaVersion':1,'state':'DRAFT_EVALUATED','approval':'NOT_ESTABLISHED','planSha256':plan_sha,
            'datasetSha256':plan['datasetSha256'],'scoringVersion':plan['scoringVersion'],'spaceKey':plan['spaceKey'],
            'method':'fixed-five-bins-v1','minimumCoverage':plan['minimumCoverage'],
            'fitCount':len(eligible['fit']),'holdoutCount':len(pairs),'supportedHoldout':len(supported),
            'excluded':exclusions,'bins':bins,
            'holdoutBrier':brier([(estimate,label) for estimate,_,label in supported]),
            'rawScoreBrierOnSameRows':brier([(value,label) for _,value,label in supported])}


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--plan',type=Path,required=True)
    parser.add_argument('--sha256',required=True)
    parser.add_argument('--output',type=Path,required=True)
    args=parser.parse_args()
    try:
        private(args.output.parent)
        with private(args.plan).open('rb') as stream:
            raw=stream.read(1048577)
        if len(raw)>1048576 or sha(raw)!=args.sha256:
            raise ValueError('Calibration plan identity differs')
        result=evaluate(parse(raw),args.sha256)
        result['recordedAt']=datetime.now(timezone.utc).isoformat()
        write_private(args.output,result)
    except (ValueError,TypeError,KeyError,OverflowError,OSError):
        parser.exit(1,'Calibration draft unavailable. Verify the plan identity and private paths.\n')


if __name__=='__main__':
    main()
