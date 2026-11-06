"""Explicit broker-only plans; no executable, URL or capability chosen by models."""
import json
import re
import uuid
from pathlib import Path
from ai_supervisor import load_plan as legacy_load,unique_pairs
from broker_bundle import verify_bundle
from model_broker import state_check
from broker_profiles import profiles

def load_plan(path,preflight=True):
    with Path(path).open('rb') as source:raw=source.read(65537)
    if len(raw)>65536:raise ValueError('Broker plan exceeds bound')
    plan=json.loads(raw,object_pairs_hook=unique_pairs,parse_constant=lambda _:(_ for _ in ()).throw(ValueError('Invalid broker JSON')))
    if not isinstance(plan,dict) or set(plan)!={'schemaVersion','coreUrl','allowInsecureLoopback','pollSeconds','backoffMaxSeconds','entries'} or type(plan['schemaVersion']) is not int or plan['schemaVersion']!=1:raise ValueError('Invalid broker plan')
    import urllib.parse
    uri=urllib.parse.urlparse(plan['coreUrl'])
    if not uri.hostname or uri.username or uri.password or uri.query or uri.fragment or uri.path not in ('','/') or '\\' in plan['coreUrl'] or any(ord(c)<=32 or ord(c)>126 for c in plan['coreUrl']):raise ValueError('Invalid broker endpoint')
    if type(plan['allowInsecureLoopback']) is not bool or not(uri.scheme=='https' or uri.scheme=='http' and uri.hostname in ('127.0.0.1','localhost') and plan['allowInsecureLoopback']):raise ValueError('Broker TLS required')
    if type(plan['pollSeconds']) is not int or not 1<=plan['pollSeconds']<=300 or type(plan['backoffMaxSeconds']) is not int or not plan['pollSeconds']<=plan['backoffMaxSeconds']<=3600:raise ValueError('Invalid broker polling')
    if not isinstance(plan['entries'],list) or not 1<=len(plan['entries'])<=32:raise ValueError('Invalid broker entries')
    registry=profiles();seen=set();states={};bundles=set()
    for entry in plan['entries']:
        if not isinstance(entry,dict) or set(entry)!={'tenant','profile','profileName','bundle','bundleSha256','stateDirectory','tokenEnvironment'}:raise ValueError('Invalid broker assignment')
        for key in ('tenant','profile'):
            if not isinstance(entry[key],str) or str(uuid.UUID(entry[key]))!=entry[key]:raise ValueError('Invalid broker identifier')
        if entry['profileName'] not in registry or not isinstance(entry['tokenEnvironment'],str) or not re.fullmatch('DZOKA_WORKER_TOKEN_[A-Z0-9_]{1,48}',entry['tokenEnvironment']):raise ValueError('Invalid broker selector')
        key=(entry['tenant'],entry['profile'])
        if key in seen:raise ValueError('Duplicate broker assignment')
        seen.add(key)
        state=str(Path(entry['stateDirectory']).resolve())
        for other,tenant in states.items():
            if tenant!=entry['tenant'] and (Path(state).is_relative_to(other) or Path(other).is_relative_to(state)):raise ValueError('Cross-tenant broker state reuse')
        states[state]=entry['tenant']
        if not Path(entry['bundle']).is_absolute() or not Path(entry['stateDirectory']).is_absolute() or not re.fullmatch('[a-f0-9]{64}',entry['bundleSha256']):raise ValueError('Explicit broker paths/digest required')
        bundle=Path(entry['bundle']).resolve()
        if bundle.is_relative_to(state) or Path(state).is_relative_to(bundle):raise ValueError('Model and private state overlap')
        if preflight:
            pair=(entry['bundle'],entry['bundleSha256'])
            if pair not in bundles:verify_bundle(*pair,pending_grants=True);bundles.add(pair)
    if preflight:
        # Reconcile every configured state before strict privacy. Otherwise an
        # inactive grant from a later assignment could block its own recovery.
        for entry in plan['entries']:state_check(entry['stateDirectory'],entry['bundle'],entry['bundleSha256'])
        for pair in bundles:verify_bundle(*pair,full=False)
    return plan
