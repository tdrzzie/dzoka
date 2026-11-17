"""Protected, bounded serial model timings; no SLO, quality or release approval."""
import argparse
from datetime import datetime, timezone
import hashlib
import json
import math
import os
from pathlib import Path
import platform
import sys
import time
sys.path.insert(0,str(Path(__file__).resolve().parent))
from semantic_review import Reader, fields, version, digest, label
from quality_protocols import parse, sequence, sha
from profile_archive import protected, audit_records
from service_host import plain
from broker_bundle import protect, verify_bundle
from model_broker import infer, state_check
from broker_profiles import profiles
from docker_lifecycle import OwnerLock

ALLOWED=frozenset(('ISO_TEXT','ISO_VISION_JOINT_TEXT','ISO_LANGUAGE_ATTRIBUTES',
    'ISO_LANGUAGE_RERANK','ISO_GENERATIVE_LLM_EN','ISO_QUALITY_SMOL360_EN',
    'ISO_PERCEPTION_SPEECH','ISO_REFINEMENT_WHISPER_EN','ISO_REFINEMENT_WHISPER_DISTIL_SMALL_EN'))
OWNER=hashlib.sha256(b'dzoka-serial-capacity-v1').hexdigest()[:32]


def integer(value,minimum,maximum):
    if type(value) is not int or not minimum<=value<=maximum:raise ValueError('Bounded integer required')
    return value


def prepare(path,expected):
    digest(expected);path=Path(path);root=protected(path.parent);audit_records(root);plain(path)
    with path.open('rb') as source:raw=source.read(262145)
    if len(raw)>262144 or sha(raw)!=expected:raise ValueError('Capacity plan changed')
    plan=version(parse(raw),('schemaVersion','scope','hostEvidence','admissionBudgetSeconds','entries'))
    if plan['scope']!='LOCAL_CAPACITY_DIAGNOSTIC':raise ValueError('Local diagnostic scope required')
    integer(plan['admissionBudgetSeconds'],1,1800)
    reader=Reader(root);reader.read(plan['hostEvidence'],32768);registry=profiles();seen=set();calls=0;prepared=[]
    for ordinal,entry in enumerate(sequence(plan['entries'],4)):
        fields(entry,('id','profile','profileKey','input','warmup','samples'));label(entry['id'])
        if entry['id'] in seen:raise ValueError('Duplicate workload entry')
        seen.add(entry['id']);name=entry['profile']
        if not isinstance(name,str) or name not in ALLOWED or name not in registry:raise ValueError('Reviewed non-Shona profile required')
        digest(entry['profileKey'])
        if entry['profileKey']!=registry[name]['key']:raise ValueError('Capacity profile identity changed')
        calls+=integer(entry['warmup'],0,2)+integer(entry['samples'],1,8)
        raw_input=reader.read(entry['input'],2*1024*1024)
        if not raw_input:raise ValueError('Nonempty capacity input required')
        prepared.append((ordinal,entry,raw_input))
    if not prepared or calls>12:raise ValueError('One to twelve explicit calls required')
    return plan,prepared


class Ledger:
    def __init__(self,stream):self.stream=stream;self.previous=None;self.sequence=0
    def write(self,kind,**payload):
        record={'schemaVersion':1,'sequence':self.sequence,'previousSha256':self.previous,'kind':kind,'at':datetime.now(timezone.utc).isoformat(),**payload}
        raw=json.dumps(record,sort_keys=True,separators=(',',':'),allow_nan=False).encode()
        self.stream.write(raw+b'\n');self.stream.flush();os.fsync(self.stream.fileno())
        self.previous=sha(raw);self.sequence+=1


def inspect(raw,expected):
    digest(expected)
    if len(raw)>262144 or sha(raw)!=expected or not raw.endswith(b'\n'):raise ValueError('Exact complete ledger bytes required')
    rows=[];previous=None
    for ordinal,line in enumerate(raw.splitlines()):
        if ordinal>13:raise ValueError('Capacity ledger exceeds event bound')
        row=parse(line)
        if not isinstance(row,dict) or type(row.get('schemaVersion')) is not int or row['schemaVersion']!=1 or type(row.get('sequence')) is not int or row['sequence']!=ordinal or row.get('previousSha256')!=previous:raise ValueError('Capacity ledger chain changed')
        if row.get('kind') not in ('START','CALL','SUMMARY','ABORT') or (ordinal==0)!=(row['kind']=='START') or rows and rows[-1]['kind'] in ('SUMMARY','ABORT'):raise ValueError('Capacity ledger event order')
        previous=sha(line);rows.append(row)
    if not rows:raise ValueError('Capacity ledger required')
    return {'state':rows[-1]['kind'] if rows[-1]['kind'] in ('SUMMARY','ABORT') else 'INCOMPLETE','events':rows}


def quantile(values,fraction):
    return sorted(values)[math.ceil(len(values)*fraction)-1]/1_000_000 if values else None


def elapsed(before):
    value=time.perf_counter_ns()-before
    if value<0:raise ValueError('Monotonic timing unavailable')
    return value


def summarize(prepared,observations):
    result=[]
    for ordinal,entry,_ in prepared:
        row={'entry':ordinal,'profileKey':entry['profileKey'],'inputSha256':entry['input']['sha256']}
        for phase,planned in (('WARMUP',entry['warmup']),('MEASURED',entry['samples'])):
            rows=[r for r in observations if r['entry']==ordinal and r['phase']==phase]
            durations=[r['durationNs'] for r in rows if r['state']=='AVAILABLE']
            row[phase.lower()]={'planned':planned,**{state.lower():sum(r['state']==state for r in rows) for state in ('AVAILABLE','UNAVAILABLE','UNCONFIRMED','NOT_DISPATCHED')},
                'unobserved':planned-len(rows),'availableP50Ms':quantile(durations,.5),'availableP95Ms':quantile(durations,.95),
                'availableP99Ms':quantile(durations,.99),'availableMinMs':min(durations)/1e6 if durations else None,
                'availableMaxMs':max(durations)/1e6 if durations else None}
        result.append(row)
    return result


def independent(left,right):
    return left!=right and not left.is_relative_to(right) and not right.is_relative_to(left)


def run(plan_path,expected,bundle,receipt,state,target,management_receipt):
    if management_receipt is not None:
        from managed_release import verify
        verify(management_receipt)
    plan,prepared=prepare(plan_path,expected)
    bundle=Path(bundle).resolve(strict=True);state=protected(Path(state)).resolve();target=Path(target);parent=protected(target.parent).resolve();data=Path(plan_path).parent.resolve()
    if not all(independent(a,b) for a,b in ((bundle,state),(bundle,data),(bundle,parent),(state,data),(state,parent))):raise ValueError('Independent private capacity paths required')
    if target.exists():raise ValueError('New capacity ledger required')
    for name in ('profiles','docker'):
        child=protected(state/name)
        if any(child.iterdir()):raise ValueError('Fresh private model state required')
    guard=OwnerLock(state,OWNER)
    if not guard.acquire():raise ValueError('Capacity state already owned')
    try:
        protect(state/(OWNER+'.lock'));verify_bundle(bundle,receipt);state_check(state,bundle,receipt)
        with target.open('xb') as stream:
            protect(target);ledger=Ledger(stream)
            ledger.write('START',at=datetime.now(timezone.utc).isoformat(),scope=plan['scope'],planSha256=expected,
                protocolSourceSha256=sha(Path(__file__).read_bytes()),managementReceiptSha256=management_receipt,
                brokerBundleSha256=receipt,hostEvidenceSha256=plan['hostEvidence']['sha256'],
                pythonVersion=platform.python_version(),architecture=platform.machine(),
                admissionBudgetSeconds=plan['admissionBudgetSeconds'],entryCount=len(prepared),
                outputHashEncoding='SORTED_COMPACT_JSON_UTF8',
                executionIntegrity='PROTECTED_RELEASE_VERIFIED' if management_receipt else 'NOT_ATTESTED',
                scheduling='SERIAL_FRESH_MODEL_PROCESS_PER_CALL',approval='NOT_ESTABLISHED',releaseAuthorization='NOT_GRANTED')
            start=time.perf_counter_ns();observations=[]
            try:
                for ordinal,entry,raw in prepared:
                    for phase,count in (('WARMUP',entry['warmup']),('MEASURED',entry['samples'])):
                        for sample in range(count):
                            row={'entry':ordinal,'phase':phase,'sample':sample,'profileKey':entry['profileKey'],
                                'inputSha256':entry['input']['sha256'],'inputBytes':len(raw),'state':'NOT_DISPATCHED',
                                'durationNs':None,'outputSha256':None,'inputTruncated':None,'outputTruncated':None}
                            if time.perf_counter_ns()-start<plan['admissionBudgetSeconds']*1_000_000_000:
                                before=time.perf_counter_ns();value=None;row['state']='UNAVAILABLE'
                                try:
                                    value=infer(bundle,receipt,state,entry['profile'],raw,checked=True)
                                    if not isinstance(value,dict):raise ValueError('Model output object required')
                                    output=json.dumps(value,sort_keys=True,separators=(',',':'),allow_nan=False).encode()
                                    row.update(state='AVAILABLE',outputSha256=sha(output),
                                        inputTruncated=value.get('inputTruncated') if type(value.get('inputTruncated')) is bool else None,
                                        outputTruncated=value.get('outputTruncated') if type(value.get('outputTruncated')) is bool else None)
                                except Exception:pass
                                try:
                                    state_check(state,bundle,receipt);verify_bundle(bundle,receipt,full=False)
                                except Exception:
                                    row.update(state='UNCONFIRMED',outputSha256=None,inputTruncated=None,outputTruncated=None)
                                    row['durationNs']=elapsed(before);observations.append(row);ledger.write('CALL',**row)
                                    raise ValueError('Capacity integrity or cleanup unconfirmed') from None
                                row['durationNs']=elapsed(before)
                            observations.append(row);ledger.write('CALL',**row)
                ledger.write('SUMMARY',state='COMPLETED',entries=summarize(prepared,observations),
                    calls=len(observations),percentileMethod='NEAREST_RANK_AVAILABLE_ONLY',
                    slo='NOT_ESTABLISHED',quality='NOT_EVALUATED',approval='NOT_ESTABLISHED',releaseAuthorization='NOT_GRANTED')
                return 0
            except Exception:
                ledger.write('ABORT',state='INTEGRITY_OR_EXECUTION_UNCONFIRMED',entries=summarize(prepared,observations),
                    approval='NOT_ESTABLISHED',releaseAuthorization='NOT_GRANTED')
                return 1
    finally:guard.close()


def main():
    parser=argparse.ArgumentParser()
    for name in ('plan','plan-sha256','bundle','bundle-sha256','state','output','management-sha256'):parser.add_argument('--'+name,required=True)
    args=parser.parse_args()
    try:
        code=run(args.plan,args.plan_sha256,args.bundle,args.bundle_sha256,args.state,args.output,args.management_sha256)
        print('Private capacity diagnostic '+('recorded' if code==0 else 'aborted')+'; SLO NOT_ESTABLISHED');return code
    except Exception:
        print('Capacity diagnostic unavailable; retain private evidence and state',file=sys.stderr);return 1


if __name__=='__main__':raise SystemExit(main())
