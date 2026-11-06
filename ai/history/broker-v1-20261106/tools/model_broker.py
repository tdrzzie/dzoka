"""Trusted claim/input/result broker. Models have no service/network authority."""
import argparse
import base64
import datetime
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import sys
import threading
import urllib.parse
import urllib.error
import uuid
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'ai'));sys.path.insert(0,str(ROOT/'tools'))
from broker_profiles import profiles,MODEL_DIR
# tools/perception_worker.py is a launcher with the same basename. Resolve the
# reviewed HTTP client explicitly, regardless of caller sys.path/module cache.
_spec=importlib.util.spec_from_file_location('_broker_http',ROOT/'ai/perception_worker.py')
_http=importlib.util.module_from_spec(_spec);_spec.loader.exec_module(_http)
request=_http.request
from broker_bundle import verify_bundle,grant,revoke
from appcontainer_boundary import Profile,recover,apis,sid_string,state_write,PREFIX
from model_process import exchange
from docker_lifecycle import OwnerLock,Session,preflight,reconcile
from service_host import plain,check_acl

def state_check(state,bundle=None,receipt=None):
    state=plain(state);check_acl(state)
    for parent in state.parents:check_acl(parent,False,True)
    for name in ('profiles','docker'):
        child=state/name
        if not child.is_dir():raise ValueError('Explicit protected broker state children required')
        check_acl(child)
    # Grant intent precedes icacls. A killed broker can leave package RX on the
    # bundle; reconcile only the plan-pinned bundle and derived owned SID.
    for path in (state/'profiles').glob('grant-*.json'):
        from worker_service import read_json
        value=read_json(plain(path),4096);owner=value.get('owner')
        if not isinstance(owner,str) or len(owner)!=32 or any(c not in '0123456789abcdef' for c in owner) or set(value)!={'schemaVersion','owner','sid','bundleSha256'} or value['schemaVersion']!=1 or path.name!='grant-'+owner+'.json':raise ValueError('Invalid bundle grant intent')
        if bundle is None or value['bundleSha256']!=receipt:raise ValueError('Plan bundle required for grant recovery')
        guard=OwnerLock(state/'profiles',owner)
        if not guard.acquire():raise ValueError('Active bundle grant owner')
        try:
            import ctypes
            k,u,a=apis();sid=ctypes.c_void_p()
            if u.DeriveAppContainerSidFromAppContainerName(PREFIX+owner,ctypes.byref(sid))!=0:raise ValueError('Grant SID unavailable')
            try:
                if sid_string(a,k,sid)!=value['sid']:raise ValueError('Grant SID changed')
            finally:a.FreeSid(sid)
            revoke(bundle,value['sid']);path.unlink()
        finally:guard.close()
    result=recover(state/'profiles')
    if result['unresolved'] or result['active']:raise ValueError('Outstanding model profile')
    return state

def infer(bundle,receipt,state,name,raw,checked=False):
    selected=profiles()[name]
    bundle=Path(bundle).resolve();state=Path(state).resolve()
    if bundle==state or bundle.is_relative_to(state) or state.is_relative_to(bundle):raise ValueError('Broker state and model bundle must be disjoint')
    if not checked:verify_bundle(bundle,receipt,pending_grants=True)
    state=state_check(state,bundle,receipt);family=selected['family'];session=None;lock=None
    if not checked:verify_bundle(bundle,receipt,full=False)
    invocation=str(uuid.uuid4());digest=hashlib.sha256(raw).hexdigest()
    if len(raw)>20*1024*1024:raise ValueError('Source exceeds model boundary')
    if family=='shona-cpu':
        preflight();result=reconcile(state/'docker')
        if result['active'] or result['unresolved']:raise ValueError('Outstanding native resource')
    with Profile(state/'profiles') as profile:
        grant_intent=state/'profiles'/('grant-'+profile.owner+'.json')
        state_write(grant_intent,{'schemaVersion':1,'owner':profile.owner,'sid':profile.sid_text,'bundleSha256':receipt})
        try:
            grant(bundle,profile.sid_text)
            def native(pieces,stop):
                nonlocal session,lock
                if family!='shona-cpu' or session is not None:raise ValueError('Native capability denied')
                owner=uuid.uuid4().hex;lock=OwnerLock(state/'docker',owner)
                if not lock.acquire():raise OSError('Native ownership unavailable')
                session=Session(Path(bundle)/'.audit-cache'/MODEL_DIR[family],state/'docker',owner,stop)
                session.prepare(selected['manifest']['configuration']['artifactGroup'])
                return session.native(pieces)
            timeout=260 if family in ('generative','quality','shona-cpu') else 100
            result=exchange(profile,bundle,{'version':1,'id':invocation,'profile':name,'inputSha256':digest,'data':base64.b64encode(raw).decode()},native,timeout=timeout)
            if result['profileKey']!=selected['key']:raise ValueError('Isolated profile changed')
            if not isinstance(result['output'],dict):raise ValueError('Invalid isolated evidence')
            return result['output']
        finally:
            complete=True
            try:
                if session:complete=session.close()
            finally:
                if lock:lock.close()
                revoke(bundle,profile.sid_text)
                grant_intent.unlink()
            if not complete:raise OSError('Native cleanup unconfirmed')

def run_once(base,token,tenant,profile_id,name,bundle,receipt,state):
    uri=urllib.parse.urlparse(base)
    if uri.username or uri.password or uri.query or uri.fragment or uri.path not in ('','/') or not uri.hostname or '\\' in base:raise ValueError('Invalid core URL')
    if uri.scheme!='https' and not(uri.scheme=='http' and uri.hostname in ('127.0.0.1','localhost') and os.environ.get('DZOKA_AI_ALLOW_INSECURE_LOOPBACK')=='true'):raise ValueError('Core TLS required')
    tenant=str(uuid.UUID(tenant));profile_id=str(uuid.UUID(profile_id));selected=profiles()[name];text=selected['family']=='text'
    verify_bundle(bundle,receipt,pending_grants=True);state_check(state,bundle,receipt);verify_bundle(bundle,receipt,full=False)
    if selected['family']=='shona-cpu':
        preflight();pending=reconcile(Path(state)/'docker')
        if pending['active'] or pending['unresolved']:raise ValueError('Native recovery required before claim')
    prefix='/api/v1/tenants/'+tenant+'/internal/'+('ai' if text else 'perception')+'/jobs';base=base.rstrip('/')
    job=request(base,token,prefix+'/claim',{'spaceId' if text else 'profileId':profile_id})
    if job is None:return False
    job_id=str(uuid.UUID(job['jobId']));lease=str(uuid.UUID(job['lease']))
    try:
        if text:
            if job['space']['spaceKey']!=selected['key']:raise ValueError('Wrong claimed space')
            raw=job['text'].encode()
        else:
            if job['profileKey']!=selected['key'] or job['task']!=selected['manifest']['task']:raise ValueError('Wrong claimed profile')
            raw=request(base,token,prefix+'/'+job_id+'/input',{'lease':lease},True)
            if len(raw)!=job['inputBytes']:raise ValueError('Claimed source length changed')
        if hashlib.sha256(raw).hexdigest()!=job['inputSha256']:raise ValueError('Claimed source digest changed')
        output=infer(bundle,receipt,state,name,raw,checked=True)
        envelope={'lease':lease,'inputSha256':job['inputSha256'],'generatedAt':datetime.datetime.now(datetime.timezone.utc).isoformat()}
        if text:envelope.update(spaceKey=selected['key'],provider=selected['space']['provider'],outputSchema='text-embedding-v1',**output)
        else:envelope.update(profileKey=selected['key'],evidence=output)
    except Exception:
        request(base,token,prefix+'/'+job_id+'/fail',{'lease':lease,'reason':'PROVIDER_UNAVAILABLE'});raise ValueError('Isolated inference unavailable') from None
    for attempt in range(2):
        try:
            if request(base,token,prefix+'/'+job_id+'/complete',envelope) is not True:raise ValueError('Lease revoked')
            return True
        except (TimeoutError,ConnectionError,urllib.error.URLError):
            if attempt:raise ValueError('Completion acknowledgement unavailable') from None

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--once',action='store_true',required=True);parser.parse_args()
    try:
        run_once(*(os.environ[k] for k in ('DZOKA_AI_CORE_URL','DZOKA_AI_TOKEN','DZOKA_AI_TENANT','DZOKA_AI_PROFILE','DZOKA_AI_BROKER_PROFILE','DZOKA_AI_BUNDLE','DZOKA_AI_BUNDLE_SHA256','DZOKA_AI_BROKER_STATE')))
        print('Broker invocation completed')
    except Exception:print('Model broker unavailable; inspect protected configuration and durable job state',file=sys.stderr);raise SystemExit(1)
