"""Bounded, authenticated M11 metadata/retention worker; schedule per tenant/identity."""
import argparse
import json
from pathlib import Path
import urllib.request
from urllib.parse import urlsplit
import uuid

ROUTES={'mlflow':'/learning/mlflow/export','activation':'/learning/mlflow/sync-activation','telemetry':'/operations/telemetry/export','learning-purge':'/learning/purge',
        'reports-purge':'/operations/maintenance/reports','fingerprints-purge':'/operations/maintenance/fingerprints',
        'perceptions-purge':'/operations/maintenance/perceptions','history-purge':'/operations/maintenance/history'}

def run(base,tenant,token,action,limit=10):
    origin=urlsplit(base)
    if (not origin.hostname or origin.username or origin.password or origin.query or origin.fragment or origin.path not in ('','/')
            or not (origin.scheme=='https' or origin.scheme=='http' and origin.hostname=='127.0.0.1')
            or action not in ROUTES or not 1<=limit<=100 or not isinstance(token,str) or not token or len(token)>16384 or any(ord(c)<33 for c in token)):
        raise ValueError('Explicit fixed origin, action, identity and bound required')
    tenant=str(uuid.UUID(tenant))
    class NoRedirect(urllib.request.HTTPRedirectHandler):
        def redirect_request(self,*args):return None
    opener=urllib.request.build_opener(urllib.request.ProxyHandler({}),NoRedirect())
    worked=0
    for _ in range(limit):
        request=urllib.request.Request(base.rstrip('/')+'/api/v1/tenants/'+tenant+ROUTES[action],data=b'{}',headers={'Authorization':'Bearer '+token,'Content-Type':'application/json'})
        with opener.open(request,timeout=40) as response:content=response.read(4097)
        if len(content)>4096:raise ValueError('Oversize operational response')
        value=json.loads(content)
        expected='exported' if action in ('mlflow','telemetry') else 'synced' if action=='activation' else 'purged'
        if not isinstance(value,dict) or set(value)!={expected}:raise ValueError('Invalid operational response')
        result=value[expected]
        if expected=='purged' or action=='telemetry':
            if type(result) is not int or not 0<=result<=10000:raise ValueError('Invalid maintenance count')
        elif type(result) is not bool:raise ValueError('Invalid metadata result')
        if not result:break
        worked+=1
        if action=='activation':break
    return worked

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--base',required=True);parser.add_argument('--tenant',required=True)
    parser.add_argument('--token-file',required=True,type=Path);parser.add_argument('--action',required=True,choices=ROUTES)
    parser.add_argument('--max-operations',type=int,default=10);args=parser.parse_args()
    try:
        if args.token_file.stat().st_size>16384:raise ValueError('Invalid credential file')
        print(json.dumps({'worked':run(args.base,args.tenant,args.token_file.read_text().strip(),args.action,args.max_operations)}))
    except Exception as error:
        print('Operational worker unavailable: '+type(error).__name__);raise SystemExit(1)
