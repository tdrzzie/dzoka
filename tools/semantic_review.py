"""Private English semantic-review evidence; never model/release approval."""
import argparse
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import re
import sys
sys.path.insert(0,str(Path(__file__).resolve().parent))
from quality_protocols import parse, sequence, sha
from model_evaluation import file_bytes
from profile_archive import protected, audit_records
from service_host import plain
from broker_bundle import protect

ROLES=('baseline','candidate')
DIMENSIONS=('GROUNDING','UNCERTAINTY','PRIVACY','SAFETY')
VERDICTS=('PASS','FAIL','UNCERTAIN')
KINDS=('HUMAN','AUTOMATED','AUTHORED_FIXTURE')


def fields(value,expected):
    if not isinstance(value,dict) or set(value)!=set(expected):raise ValueError('Review fields')
    return value


def version(value,expected):
    fields(value,expected)
    if type(value['schemaVersion']) is not int or value['schemaVersion']!=1:raise ValueError('Review version')
    return value


def digest(value):
    if not isinstance(value,str) or not re.fullmatch('[a-f0-9]{64}',value):raise ValueError('Exact digest required')
    return value


def label(value):
    if not isinstance(value,str) or not 1<=len(value)<=128 or any(ord(c)<32 for c in value):raise ValueError('Bounded private label')
    return value


def timestamp(value):
    if not isinstance(value,str) or not re.fullmatch(r'\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,6})?Z',value):raise ValueError('UTC review timestamp required')
    datetime.fromisoformat(value)


class Reader:
    def __init__(self,root):self.root=root;self.cache={};self.remaining=16*1024*1024

    def read(self,description,limit=262144):
        fields(description,('path','sha256'));digest(description['sha256'])
        name=description['path']
        if not isinstance(name,str):raise ValueError('Review basename required')
        if name in self.cache:
            raw=self.cache[name]
            if len(raw)>limit or sha(raw)!=description['sha256']:raise ValueError('Conflicting review artifact')
            return raw
        raw=file_bytes(self.root,name,description['sha256'],limit)
        self.remaining-=len(raw)
        if self.remaining<0:raise ValueError('Review aggregate exceeds bound')
        self.cache[name]=raw
        return raw


def actor(record,reader,scope):
    if not isinstance(record['reviewerId'],str) or not re.fullmatch('[a-f0-9]{32}',record['reviewerId']):raise ValueError('Opaque reviewer identity required')
    if record['reviewerKind'] not in KINDS or (scope=='REFERENCE_REVIEW' and record['reviewerKind']=='AUTHORED_FIXTURE'):raise ValueError('Declared reviewer kind')
    timestamp(record['at']);reader.read(record['authorityEvidence'],32768)
    return {'reviewerSha256':sha(record['reviewerId'].encode()),'kind':record['reviewerKind'],
            'authorityEvidenceSha256':record['authorityEvidence']['sha256'],'declaredAt':record['at']}


def prepare_packet(packet,reader):
    version(packet,('schemaVersion','scope','language','task','evidence','profiles','cases'))
    if packet['scope'] not in ('AUTHORED_DIAGNOSTIC','REFERENCE_REVIEW') or packet['language']!='en' or packet['task'] not in ('ENGLISH_DRAFT','GROUNDED_ATTRIBUTES'):raise ValueError('Supported English review scope required')
    reader.read(packet['evidence'],32768);fields(packet['profiles'],ROLES)
    for profile in packet['profiles'].values():
        fields(profile,('profileKey','projectionSourceSha256'))
        digest(profile['profileKey']);digest(profile['projectionSourceSha256'])
    if packet['profiles']['baseline']['profileKey']==packet['profiles']['candidate']['profileKey']:raise ValueError('Distinct compared profiles required')
    cases=sequence(packet['cases'],128)
    if not cases:raise ValueError('Review cases required')
    identities=set();groups={key:{} for key in ('inputSha256','entityGroup','institutionGroup','timeGroup')}
    result={};source_splits={}
    for ordinal,case in enumerate(cases):
        fields(case,('id','split','slice','inputSha256','entityGroup','institutionGroup','timeGroup','input','source','reference',*ROLES))
        for key in ('id','slice','entityGroup','institutionGroup','timeGroup'):label(case[key])
        digest(case['inputSha256'])
        if case['id'] in identities or case['split'] not in ('TRAIN','VALIDATION','TEST'):raise ValueError('Duplicate case or split')
        identities.add(case['id'])
        for key,owners in groups.items():
            group=case[key]
            if group in owners and owners[group]!=case['split']:raise ValueError('Cross-split review leakage')
            owners[group]=case['split']
        source=reader.read(case['source'],16384);raw_input=reader.read(case['input'],32768)
        if sha(raw_input)!=case['inputSha256'] or not 1<=len(source.decode('utf-8'))<=4096 or b'\0' in source:raise ValueError('Exact bounded source identity required')
        source_sha=sha(source)
        if source_sha in source_splits and source_splits[source_sha]!=case['split']:raise ValueError('Cross-split source leakage')
        source_splits[source_sha]=case['split']
        if packet['task']=='ENGLISH_DRAFT':
            request=fields(parse(raw_input),('prompt','query'))
            if any(not isinstance(request[k],str) or not request[k] or '\0' in request[k] for k in request) or len(request['prompt'])>1024 or request['query'].encode('utf-8')!=source:raise ValueError('Draft source projection differs from raw request')
        elif raw_input!=source:raise ValueError('Attribute source differs from raw input')
        parse(reader.read(case['reference'],65536))
        for role in ROLES:
            observation=fields(case[role],('state','artifact'))
            if observation['state']=='UNAVAILABLE':
                if observation['artifact'] is not None:raise ValueError('Unavailable evidence has no artifact')
            elif observation['state'] in ('AVAILABLE','TRUNCATED'):parse(reader.read(observation['artifact'],65536))
            else:raise ValueError('Explicit provider coverage required')
        result[case['id']]=(ordinal,case)
    return result


def prepare_reviews(descriptions,reader,packet_sha,packet,cases):
    descriptions=sequence(descriptions,8)
    if len(descriptions)<2:raise ValueError('At least two reviewers required')
    reviewers={};observations={};proofs=[]
    expected={(case_id,role) for case_id in cases for role in ROLES}
    for description in descriptions:
        record=version(parse(reader.read(description)),('schemaVersion','packetSha256','reviewerId','reviewerKind','authorityEvidence','at','findings'))
        if record['packetSha256']!=packet_sha:raise ValueError('Review belongs to a different packet')
        metadata=actor(record,reader,packet['scope']);who=record['reviewerId']
        if who in reviewers:raise ValueError('Duplicate reviewer')
        reviewers[who]=metadata;proofs.append(description['sha256']);found={}
        for finding in sequence(record['findings'],256):
            fields(finding,('caseId','role','state','verdicts'));key=(label(finding['caseId']),finding['role'])
            if finding['role'] not in ROLES or key not in expected or key in found:raise ValueError('Duplicate or unknown reviewed observation')
            provider=cases[key[0]][1][key[1]]['state'];state=finding['state']
            if provider=='UNAVAILABLE':
                if state!='PROVIDER_UNAVAILABLE' or finding['verdicts'] is not None:raise ValueError('Provider absence is not a semantic failure')
            elif state=='ASSESSED':
                fields(finding['verdicts'],DIMENSIONS)
                if any(value not in VERDICTS for value in finding['verdicts'].values()):raise ValueError('Explicit semantic judgment required')
            elif state!='UNREVIEWED' or finding['verdicts'] is not None:raise ValueError('Missing review is separate coverage')
            found[key]=finding
        if set(found)!=expected:raise ValueError('Incomplete reviewer matrix; declare unreviewed observations')
        observations[who]=found
    # Order is not review authority; every adjudication binds the same exact set.
    return reviewers,observations,sha(json.dumps(sorted(proofs),separators=(',',':')).encode())


def prepare_adjudications(descriptions,reader,packet_sha,review_set,packet,cases,reviewers,observations):
    result={};actors=[];seen=set()
    for description in sequence(descriptions,128):
        record=version(parse(reader.read(description)),('schemaVersion','packetSha256','reviewSetSha256','reviewerId','reviewerKind','authorityEvidence','at','decisions'))
        if record['packetSha256']!=packet_sha or record['reviewSetSha256']!=review_set:raise ValueError('Stale adjudication evidence')
        metadata=actor(record,reader,packet['scope']);who=record['reviewerId']
        if who in reviewers or who in seen:raise ValueError('Distinct adjudicator required')
        seen.add(who);actors.append(metadata)
        decisions=sequence(record['decisions'],1024)
        if not decisions:raise ValueError('Adjudication decisions required')
        for decision in decisions:
            fields(decision,('caseId','role','dimension','verdict','rationaleEvidence'))
            key=(label(decision['caseId']),decision['role']);dimension=decision['dimension']
            if key[0] not in cases or key[1] not in ROLES or dimension not in DIMENSIONS or decision['verdict'] not in VERDICTS:raise ValueError('Unknown adjudication target')
            ratings=[rows[key] for rows in observations.values()]
            if cases[key[0]][1][key[1]]['state']!='AVAILABLE' or any(r['state']!='ASSESSED' for r in ratings):raise ValueError('Cannot adjudicate incomplete evidence')
            if len({r['verdicts'][dimension] for r in ratings})<2:raise ValueError('Adjudication requires a disagreement')
            identity=(*key,dimension)
            if identity in result:raise ValueError('Duplicate adjudication')
            reader.read(decision['rationaleEvidence'],32768)
            result[identity]={'verdict':decision['verdict'],'reviewerSha256':metadata['reviewerSha256'],
                              'reviewerKind':metadata['kind'],'rationaleEvidenceSha256':decision['rationaleEvidence']['sha256']}
    return result,actors


def evaluate(path,expected):
    digest(expected);path=Path(path);root=protected(path.parent);audit_records(root);plain(path)
    with path.open('rb') as source:raw=source.read(262145)
    if len(raw)>262144 or sha(raw)!=expected:raise ValueError('Review manifest changed')
    manifest=version(parse(raw),('schemaVersion','packet','reviews','adjudications'));reader=Reader(root)
    packet_raw=reader.read(manifest['packet']);packet=parse(packet_raw);packet_sha=sha(packet_raw)
    cases=prepare_packet(packet,reader)
    reviewers,observations,review_set=prepare_reviews(manifest['reviews'],reader,packet_sha,packet,cases)
    adjudications,adjudicators=prepare_adjudications(manifest['adjudications'],reader,packet_sha,review_set,packet,cases,reviewers,observations)
    rows=[];coverage={role:{'available':0,'truncated':0,'unavailable':0,'fullyReviewed':0,'declaredHumanReviewed':0} for role in ROLES}
    unresolved=adjudicated=0
    for case_id,(ordinal,case) in cases.items():
        for role in ROLES:
            provider=case[role]['state'];coverage[role][provider.lower()]+=1
            ratings=[values[(case_id,role)] for values in observations.values()]
            complete=provider=='AVAILABLE' and all(r['state']=='ASSESSED' for r in ratings)
            human=sum(reviewers[who]['kind']=='HUMAN' and values[(case_id,role)]['state']=='ASSESSED' for who,values in observations.items())
            coverage[role]['fullyReviewed']+=int(complete)
            coverage[role]['declaredHumanReviewed']+=int(provider=='AVAILABLE' and human>=2)
            resolutions={}
            for dimension in DIMENSIONS:
                resolution={'state':'INCOMPLETE_EVIDENCE_OR_REVIEW','verdict':None}
                if complete:
                    values={r['verdicts'][dimension] for r in ratings}
                    if len(values)==1:resolution={'state':'AGREEMENT','verdict':next(iter(values))}
                    elif (case_id,role,dimension) in adjudications:
                        resolution={'state':'DECLARED_ADJUDICATION',**adjudications[(case_id,role,dimension)]};adjudicated+=1
                    else:resolution={'state':'UNRESOLVED_DISAGREEMENT','verdict':None};unresolved+=1
                resolutions[dimension]=resolution
            rows.append({'case':ordinal,'split':case['split'],'sliceSha256':sha(case['slice'].encode()),'role':role,
                         'profileKey':packet['profiles'][role]['profileKey'],'projectionSourceSha256':packet['profiles'][role]['projectionSourceSha256'],
                         'inputSha256':case['inputSha256'],'referenceSha256':case['reference']['sha256'],
                         'artifactSha256':case[role]['artifact']['sha256'] if case[role]['artifact'] else None,
                         'providerState':provider,'assessedReviewers':sum(r['state']=='ASSESSED' for r in ratings),'dimensions':resolutions})
    return {'schemaVersion':1,'at':datetime.now(timezone.utc).isoformat(),'scope':packet['scope'],'task':packet['task'],
            'manifestSha256':sha(raw),'packetSha256':packet_sha,'reviewSetSha256':review_set,'evidenceSha256':packet['evidence']['sha256'],
            'protocolSourceSha256':sha(Path(__file__).read_bytes()),'fileProtocolSourceSha256':sha(Path(__file__).with_name('model_evaluation.py').read_bytes()),
            'managementReceiptSha256':None,'executionIntegrity':'NOT_ATTESTED',
            'reviewers':list(reviewers.values()),'adjudicators':adjudicators,'coverage':coverage,'rows':rows,
            'unresolvedDimensions':unresolved,'adjudicatedDimensions':adjudicated,'authority':'DECLARATIONS_ONLY',
            'approval':'NOT_ESTABLISHED','releaseAuthorization':'NOT_GRANTED','leakageCheck':'DECLARED_GROUPS_DISJOINT'}


def main():
    parser=argparse.ArgumentParser();parser.add_argument('--manifest',required=True);parser.add_argument('--manifest-sha256',required=True);parser.add_argument('--management-sha256',required=True);parser.add_argument('--output',required=True);args=parser.parse_args()
    try:
        from managed_release import verify
        verify(args.management_sha256)
        target=Path(args.output);protected(target.parent)
        if target.exists():raise ValueError('New review output required')
        result=evaluate(args.manifest,args.manifest_sha256)
        result.update(managementReceiptSha256=args.management_sha256,executionIntegrity='PROTECTED_RELEASE_VERIFIED')
        with target.open('xb') as output:output.write((json.dumps(result,allow_nan=False,indent=2)+'\n').encode());output.flush();os.fsync(output.fileno())
        protect(target);print('Private semantic review recorded; approval NOT_ESTABLISHED');return 0
    except Exception:
        print('Semantic review unavailable; inspect private evidence without sharing it',file=sys.stderr);return 1


if __name__=='__main__':raise SystemExit(main())
