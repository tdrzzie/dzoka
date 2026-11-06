"""Private reference protocols; metrics never approve a model or property release."""
import argparse
from datetime import datetime, timezone
import hashlib
import json
import math
from pathlib import Path
import re
import sys
sys.path.insert(0, str(Path(__file__).resolve().parent))
from ai_supervisor import unique_pairs
from model_evaluation import score as text_score, file_bytes
from profile_archive import protected, audit_records
from service_host import plain


def sha(raw): return hashlib.sha256(raw).hexdigest()
def parse(raw):
    return json.loads(raw, object_pairs_hook=unique_pairs,
                      parse_constant=lambda _: (_ for _ in ()).throw(ValueError('Nonfinite JSON')))
def number(x):
    if type(x) not in (int, float) or not math.isfinite(x): raise ValueError('Finite number required')
    return x
def ratio(a, b): return a / b if b else None
def sequence(x, bound=65536):
    if not isinstance(x, list) or len(x) > bound: raise ValueError('Bounded list required')
    return x
def labels(x):
    x=sequence(x, 256)
    if any(not isinstance(v,str) or not 1<=len(v)<=128 for v in x) or len(set(x))!=len(x):
        raise ValueError('Unique bounded labels required')
    return x
def counts(tp, fp, fn):
    return {'truePositive':tp,'falsePositive':fp,'falseNegative':fn,
            'precision':ratio(tp,tp+fp),'recall':ratio(tp,tp+fn),'f1':ratio(2*tp,2*tp+fp+fn)}
def boxes(x):
    for item in sequence(x,256):
        if not isinstance(item,dict) or set(item)!={'label','box'}: raise ValueError('Box fields')
        labels([item['label']]);b=sequence(item['box'],4)
        if len(b)!=4 or any(not 0<=number(v)<=1 for v in b) or b[0]>=b[2] or b[1]>=b[3]:
            raise ValueError('Normalized positive-area box required')
    return x
def iou(a,b):
    intersection=max(0,min(a[2],b[2])-max(a[0],b[0]))*max(0,min(a[3],b[3])-max(a[1],b[1]))
    return intersection/((a[2]-a[0])*(a[3]-a[1])+(b[2]-b[0])*(b[3]-b[1])-intersection)


def measure(metric, actual, reference, parameters):
    if not isinstance(parameters,dict): raise ValueError('Parameters object required')
    if metric=='boxes':
        if set(parameters)!={'iouThreshold'} or not 0<number(parameters['iouThreshold'])<=1:
            raise ValueError('Explicit positive IoU threshold required')
        actual=boxes(actual);reference=boxes(reference)
        # Deterministic global greedy overlap matching, not AP or optimal assignment.
        pairs=sorted((-iou(a['box'],b['box']),i,j) for i,a in enumerate(actual)
                     for j,b in enumerate(reference) if a['label']==b['label'])
        used_a=set();used_b=set()
        for negative,i,j in pairs:
            if -negative>=parameters['iouThreshold'] and i not in used_a and j not in used_b:
                used_a.add(i);used_b.add(j)
        return counts(len(used_a),len(actual)-len(used_a),len(reference)-len(used_b))
    if parameters: raise ValueError('Unexpected parameters')
    if metric in ('exact','wer','cer'): return {'value':text_score(metric,actual,reference)}
    if metric=='cosine':
        a=sequence(actual,4096);b=sequence(reference,4096)
        if not a or len(a)!=len(b): raise ValueError('Compatible nonempty vectors required')
        a=[number(x) for x in a];b=[number(x) for x in b]
        # Scale first to avoid overflow on otherwise finite inputs.
        sa=max(map(abs,a));sb=max(map(abs,b))
        if not sa or not sb: raise ValueError('Zero vector is unavailable')
        a=[v/sa for v in a];b=[v/sb for v in b]
        return {'cosine':max(-1,min(1,sum(x*y for x,y in zip(a,b))/math.sqrt(sum(x*x for x in a)*sum(x*x for x in b))))}
    if metric=='labels':
        a=set(labels(actual));b=set(labels(reference));return counts(len(a&b),len(a-b),len(b-a))
    if metric=='ranking':
        a=labels(actual);b=set(labels(reference))
        if not a or not b: raise ValueError('Nonempty ranking and relevant labels required')
        return {'top1Relevant':int(a[0] in b),'recallInReturnedList':len(set(a)&b)/len(b),
                'reciprocalRank':next((1/(i+1) for i,x in enumerate(a) if x in b),0)}
    if metric=='mask':
        for x in (actual,reference):
            if not isinstance(x,dict) or set(x)!={'shape','labels'}: raise ValueError('Mask fields')
            shape=sequence(x['shape'],2)
            if len(shape)!=2 or any(type(v) is not int or not 1<=v<=256 for v in shape): raise ValueError('Mask shape')
            pixels=sequence(x['labels'])
            if len(pixels)!=math.prod(shape) or any(type(v) is not int or not 0<=v<=255 for v in pixels): raise ValueError('Mask labels')
        if actual['shape']!=reference['shape']: raise ValueError('Mask geometry differs')
        a=actual['labels'];b=reference['labels'];classes=sorted(set(a)|set(b));ious=[];dice=[]
        for label in classes:
            intersection=sum(x==label and y==label for x,y in zip(a,b));na=a.count(label);nb=b.count(label)
            ious.append(intersection/(na+nb-intersection));dice.append(2*intersection/(na+nb))
        return {'pixelAgreement':sum(x==y for x,y in zip(a,b))/len(a),'macroIou':sum(ious)/len(ious),
                'macroDice':sum(dice)/len(dice),'classCount':len(classes)}
    raise ValueError('Unsupported protocol; generative semantics require accountable human review')


def evaluate(path):
    path=Path(path);root=protected(path.parent);audit_records(root);plain(path)
    with path.open('rb') as source: raw=source.read(262145)
    if len(raw)>262144: raise ValueError('Manifest exceeds bound')
    manifest=parse(raw)
    if not isinstance(manifest,dict) or set(manifest)!={'schemaVersion','scope','evidence','cases'} or type(manifest['schemaVersion']) is not int or manifest['schemaVersion']!=1 or manifest['scope'] not in ('AUTHORED_DIAGNOSTIC','REFERENCE_EVALUATION'):
        raise ValueError('Manifest fields')
    evidence=manifest['evidence']
    if not isinstance(evidence,dict) or set(evidence)!={'path','sha256'}: raise ValueError('Evidence fields')
    file_bytes(root,evidence['path'],evidence['sha256'],32768)
    cases=sequence(manifest['cases'],128)
    if not cases: raise ValueError('Cases required')
    groups={k:{} for k in ('inputSha256','entityGroup','institutionGroup','timeGroup')};seen=set();prepared=[]
    fields={'id','split','slice','inputSha256','entityGroup','institutionGroup','timeGroup','spaceKey','referenceSpaceKey','artifact','reference','metric','parameters','projectionSourceSha256'}
    for case in cases:
        if not isinstance(case,dict) or set(case)!=fields: raise ValueError('Case fields')
        for field in ('id','slice','entityGroup','institutionGroup','timeGroup'):
            if not isinstance(case[field],str) or not 1<=len(case[field])<=128: raise ValueError('Bounded private metadata')
        for field in ('inputSha256','spaceKey','referenceSpaceKey','projectionSourceSha256'):
            if not isinstance(case[field],str) or not re.fullmatch('[a-f0-9]{64}',case[field]): raise ValueError('Content identity required')
        if case['spaceKey']!=case['referenceSpaceKey']: raise ValueError('Incompatible profile/space identities')
        if case['id'] in seen or case['split'] not in ('TRAIN','VALIDATION','TEST'): raise ValueError('Duplicate case or invalid split')
        seen.add(case['id'])
        for field,owners in groups.items():
            group=case[field]
            if group in owners and owners[group]!=case['split']: raise ValueError('Cross-split leakage')
            owners[group]=case['split']
        values=[];digests=[]
        for field in ('artifact','reference'):
            item=case[field]
            if not isinstance(item,dict) or set(item)!={'path','sha256'}: raise ValueError('Artifact fields')
            content=file_bytes(root,item['path'],item['sha256'],2*1024*1024)
            values.append(parse(content));digests.append(sha(content))
        prepared.append((case,values,digests))
    # Complete hashes/split validation before any metric computation.
    rows=[];slices={}
    for ordinal,(case,values,digests) in enumerate(prepared):
        metrics=measure(case['metric'],*values,case['parameters'])
        key=sha(case['slice'].encode());slices[key]=slices.get(key,0)+1
        rows.append({'case':ordinal,'split':case['split'],'sliceSha256':key,'spaceKey':case['spaceKey'],
                     'inputSha256':case['inputSha256'],'artifactSha256':digests[0],'referenceSha256':digests[1],
                     'projectionSourceSha256':case['projectionSourceSha256'],'metric':case['metric'],'metrics':metrics})
    return {'schemaVersion':1,'at':datetime.now(timezone.utc).isoformat(),'scope':manifest['scope'],
            'datasetSha256':sha(raw),'evidenceSha256':evidence['sha256'],
            'protocolSourceSha256':sha(Path(__file__).read_bytes()),
            'textProtocolSourceSha256':sha(Path(__file__).with_name('model_evaluation.py').read_bytes()),
            'approval':'NOT_ESTABLISHED','leakageCheck':'DECLARED_GROUPS_DISJOINT','sliceCounts':slices,'rows':rows}


if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--dataset',required=True);parser.add_argument('--output',required=True);args=parser.parse_args()
    try:
        result=evaluate(args.dataset);target=Path(args.output);protected(target.parent)
        with target.open('xb') as output: output.write((json.dumps(result,allow_nan=False,indent=2)+'\n').encode())
        print('Private reference evaluation recorded; approval NOT_ESTABLISHED')
    except Exception:
        print('Reference evaluation unavailable; retain private evidence',file=sys.stderr);raise SystemExit(1)
