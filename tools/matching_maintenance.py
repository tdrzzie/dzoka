"""One bounded pass of projection reconciliation and history expiry through the core API."""
import argparse
import json
from pathlib import Path
import re
import urllib.error
import urllib.parse
import urllib.request
from uuid import UUID

from openrouter_connection import NoRedirect, parse, private, read_private, sha


def uuid_text(value):
    if not isinstance(value,str) or str(UUID(value))!=value:
        raise ValueError('Canonical UUID required')
    return value


def commands(plan):
    if not isinstance(plan,dict) or set(plan)!={'schemaVersion','origin','allowLoopbackHttp','assignments'}:
        raise ValueError('Invalid matching maintenance plan')
    if type(plan['schemaVersion']) is not int or plan['schemaVersion']!=1 or type(plan['allowLoopbackHttp']) is not bool:
        raise ValueError('Matching maintenance policy required')
    uri=urllib.parse.urlsplit(plan['origin'])
    if not uri.hostname or uri.username or uri.password or uri.path or uri.query or uri.fragment or '\\' in plan['origin']:
        raise ValueError('Core origin required')
    if uri.scheme!='https' and not(uri.scheme=='http' and uri.hostname=='127.0.0.1' and uri.port and plan['allowLoopbackHttp']):
        raise ValueError('HTTPS or explicit local loopback required')
    rows=plan['assignments']
    if not isinstance(rows,list) or not 1<=len(rows)<=8:
        raise ValueError('Bounded tenant assignments required')
    seen=set()
    jobs=[]
    for row in rows:
        if not isinstance(row,dict) or set(row)!={'tenant','spaces','pruneHistory'} or type(row['pruneHistory']) is not bool:
            raise ValueError('Invalid maintenance assignment')
        tenant=uuid_text(row['tenant'])
        if tenant in seen or not isinstance(row['spaces'],list) or len(row['spaces'])>8:
            raise ValueError('Unique bounded tenant assignments required')
        seen.add(tenant)
        namespaces=[uuid_text(value) for value in row['spaces']]
        if len(set(namespaces))!=len(namespaces):
            raise ValueError('Duplicate space assignment')
        prefix=uri.geturl()+'/api/v1/tenants/'+tenant+'/internal/matching'
        jobs.extend(('PUT',prefix+'/projections/'+space) for space in namespaces)
        if row['pruneHistory']:
            jobs.append(('POST',prefix+'/history/prune'))
    if not 1<=len(jobs)<=16:
        raise ValueError('Bounded maintenance commands required')
    return jobs


def execute(jobs, token):
    if not isinstance(token,str) or not re.fullmatch(r'[A-Za-z0-9._~-]{32,8192}',token):
        raise ValueError('Core authorization token required')
    opener=urllib.request.build_opener(urllib.request.ProxyHandler({}),NoRedirect())
    complete=0
    for method,url in jobs:
        request=urllib.request.Request(url,data=b'',method=method,headers={'Authorization':'Bearer '+token})
        try:
            with opener.open(request,timeout=65) as response:
                if response.status!=200 or len(response.read(4097))>4096:
                    break
        except urllib.error.HTTPError as error:
            error.close()
            break
        except (OSError,ValueError):
            break
        complete+=1
    return complete


def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--plan',type=Path,required=True)
    parser.add_argument('--sha256',required=True)
    parser.add_argument('--credentials',type=Path,required=True)
    args=parser.parse_args()
    try:
        with private(args.plan).open('rb') as source:
            raw=source.read(32769)
        if len(raw)>32768 or sha(raw)!=args.sha256:
            raise ValueError('Plan digest differs')
        jobs=commands(parse(raw))
        credentials=read_private(args.credentials,16384)
        if not isinstance(credentials,dict) or set(credentials)!={'token'}:
            raise ValueError('Invalid private credentials')
        completed=execute(jobs,credentials['token'])
        print(json.dumps({'completed':completed,'planned':len(jobs),'state':'COMPLETE' if completed==len(jobs) else 'INCOMPLETE'}))
        return 0 if completed==len(jobs) else 1
    except (ValueError,TypeError,OSError):
        print('Matching maintenance unavailable. Inspect protected configuration.')
        return 1


if __name__=='__main__':
    raise SystemExit(main())
