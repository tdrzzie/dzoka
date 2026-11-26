"""Query OSV for public locked Maven coordinates; never reads credentials or app data."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import urllib.request

def inventory(lock,fetch):
    dependencies=lock['dependencies']
    queries=[]
    for dep in dependencies:
        group,artifact,version=dep['coordinate'].split(':')
        if not all(re.fullmatch(r'[A-Za-z0-9._+-]+',v) for v in (group,artifact,version)):raise ValueError('Unexpected public coordinate')
        queries.append({'package':{'ecosystem':'Maven','name':group+':'+artifact},'version':version})
    results=[[] for _ in queries];pending=[(i,q) for i,q in enumerate(queries)]
    for _ in range(10):
        response=fetch({'queries':[q for _,q in pending]})
        if not isinstance(response,dict) or not isinstance(response.get('results'),list) or len(response['results'])!=len(pending):raise ValueError('Incomplete advisory response')
        more=[]
        for (index,query),result in zip(pending,response['results']):
            if not isinstance(result,dict) or set(result)-{'vulns','next_page_token'}:raise ValueError('Invalid advisory result')
            for vuln in result.get('vulns',[]):
                if not isinstance(vuln,dict) or not isinstance(vuln.get('id'),str) or not re.fullmatch(r'[A-Za-z0-9._-]{1,120}',vuln['id']):raise ValueError('Invalid advisory identifier')
                results[index].append({'id':vuln['id'],'modified':vuln.get('modified')})
            if result.get('next_page_token'):more.append((index,{**query,'page_token':result['next_page_token']}))
        if not more:return [{'coordinate':d['coordinate'],'scope':d['scope'],'advisories':v} for d,v in zip(dependencies,results)]
        pending=more
    raise ValueError('Advisory pagination exceeds bound')

def fetch(body):
    request=urllib.request.Request('https://api.osv.dev/v1/querybatch',data=json.dumps(body).encode(),headers={'Content-Type':'application/json'})
    class NoRedirect(urllib.request.HTTPRedirectHandler):
        def redirect_request(self,*args):return None
    with urllib.request.build_opener(urllib.request.ProxyHandler({}),NoRedirect()).open(request,timeout=30) as response:
        raw=response.read(8*1024*1024+1)
    if len(raw)>8*1024*1024:raise ValueError('Advisory response exceeds bound')
    return json.loads(raw)

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--lock',type=Path,default=Path('dependencies.lock.json'));parser.add_argument('--output',type=Path,required=True);parser.add_argument('--review-date',required=True);args=parser.parse_args()
    data=args.lock.read_bytes();rows=inventory(json.loads(data),fetch)
    output={'schema':1,'reviewDate':args.review_date,'lockSha256':hashlib.sha256(data).hexdigest(),'source':'https://api.osv.dev/v1/querybatch','dependencies':rows,'limits':['Public OSV exact-coordinate findings only; no guarantee of vulnerability absence','No source, base-image, JDK, Python, configuration or penetration assessment']}
    args.output.write_text(json.dumps(output,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({'packages':len(rows),'packagesWithAdvisories':sum(bool(r['advisories']) for r in rows)}))
