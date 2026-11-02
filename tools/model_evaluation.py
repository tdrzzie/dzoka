"""Private, hash-bound text-output diagnostics; no production promotion decision."""
import argparse
from datetime import datetime,timezone
import hashlib
import json
from pathlib import Path
import sys
import unicodedata
sys.path.insert(0,str(Path(__file__).resolve().parent))
from worker_service import read_json
from profile_archive import protected,audit_records
from service_host import plain
from broker_bundle import verify_bundle
from broker_profiles import profiles
from model_broker import infer
from ai_supervisor import unique_pairs

def digest(raw):return hashlib.sha256(raw).hexdigest()
def units(text,characters=False):
    text=' '.join(unicodedata.normalize('NFC',text).casefold().split())
    return list(text) if characters else text.split()
def distance(left,right):
    row=list(range(len(right)+1))
    for i,a in enumerate(left,1):
        current=[i]
        for j,b in enumerate(right,1):current.append(min(row[j]+1,current[j-1]+1,row[j-1]+int(a!=b)))
        row=current
    return row[-1]
def score(metric,actual,reference):
    if not isinstance(actual,str) or not isinstance(reference,str) or max(len(actual),len(reference))>4096:raise ValueError('Bounded text metric required')
    if metric=='exact':return float(units(actual)==units(reference))
    if metric not in ('wer','cer'):raise ValueError('Unsupported text metric')
    expected=units(reference,metric=='cer');observed=units(actual,metric=='cer')
    if not expected:raise ValueError('Nonempty reference required')
    return distance(expected,observed)/len(expected)

def file_bytes(root,name,expected,limit):
    if not isinstance(name,str) or not name or Path(name).name!=name or any(c in name for c in ('/','\\',':')):raise ValueError('Dataset basename required')
    path=plain(root/name)
    with path.open('rb') as source:raw=source.read(limit+1)
    if len(raw)>limit or digest(raw)!=expected:raise ValueError('Dataset content changed')
    return raw

def evaluate(dataset,bundle,receipt,state):
    root=protected(Path(dataset).parent);audit_records(root);manifest_path=plain(dataset)
    with manifest_path.open('rb') as source:raw=source.read(65537)
    if len(raw)>65536:raise ValueError('Dataset manifest exceeds bound')
    value=json.loads(raw,object_pairs_hook=unique_pairs,parse_constant=lambda _:(_ for _ in ()).throw(ValueError('Invalid dataset JSON')));registry=profiles()
    if not isinstance(value,dict) or set(value)!={'schemaVersion','scope','evidence','cases'} or type(value['schemaVersion']) is not int or value['schemaVersion']!=1 or value['scope'] not in ('AUTHORED_DIAGNOSTIC','REFERENCE_EVALUATION') or not isinstance(value['cases'],list) or not 1<=len(value['cases'])<=128:raise ValueError('Invalid evaluation manifest')
    evidence=value['evidence']
    if not isinstance(evidence,dict) or set(evidence)!={'path','sha256'}:raise ValueError('Dataset evidence required')
    file_bytes(root,evidence['path'],evidence['sha256'],32768)
    prepared=[];seen=set()
    for item in value['cases']:
        if not isinstance(item,dict) or set(item)!={'profile','path','sha256','reference','metric'} or item['profile'] not in registry or item['metric'] not in ('exact','wer','cer'):raise ValueError('Invalid evaluation case')
        if registry[item['profile']].get('manifest',{}).get('task') not in ('OCR','SPEECH','TRANSLATION','MULTILINGUAL_TRANSLATION'):raise ValueError('Text-reference task required')
        score(item['metric'],item['reference'],item['reference'])
        data=file_bytes(root,item['path'],item['sha256'],20*1024*1024)
        key=(item['profile'],item['sha256'])
        if key in seen:raise ValueError('Duplicate evaluation input/profile')
        seen.add(key);prepared.append((item,data))
    verify_bundle(bundle,receipt);rows=[]
    for index,(item,data) in enumerate(prepared):
        output=infer(bundle,receipt,state,item['profile'],data,checked=True)
        rows.append({'case':index,'profileKey':registry[item['profile']]['key'],'inputSha256':item['sha256'],'referenceSha256':digest(item['reference'].encode()),'outputSha256':digest(output['text'].encode()),'metric':item['metric'],'value':score(item['metric'],output['text'],item['reference'])})
    return {'schemaVersion':1,'at':datetime.now(timezone.utc).isoformat(),'datasetSha256':digest(raw),'evidenceSha256':evidence['sha256'],'bundleSha256':receipt,'evaluationSourceSha256':digest(Path(__file__).read_bytes()),'scope':value['scope'],'approval':'NOT_ESTABLISHED','normalization':'Unicode NFC/casefold/whitespace; CER counts Unicode codepoints','rows':rows}

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--dataset',required=True);parser.add_argument('--bundle',required=True);parser.add_argument('--bundle-sha256',required=True);parser.add_argument('--state',required=True);parser.add_argument('--output',required=True);args=parser.parse_args()
    try:
        from managed_release import verify_sources
        verify_sources();result=evaluate(args.dataset,args.bundle,args.bundle_sha256,args.state)
        target=Path(args.output);protected(target.parent)
        if target.exists():raise ValueError('New evaluation result required')
        with target.open('xb') as output:output.write((json.dumps(result,indent=2)+'\n').encode())
        print('Evaluation completed; evidence is private and does not approve promotion')
    except Exception:print('Evaluation unavailable; no quality approval established',file=sys.stderr);raise SystemExit(1)
