"""Private authenticated leased-job client. Never receives database/storage credentials."""
import argparse
import datetime
import json
import os
from pathlib import Path
import sys
import urllib.parse
import urllib.request
import uuid
sys.path.insert(0,str(Path(__file__).resolve().parent))
from provider import TextEmbeddingProvider,ProviderUnavailable,load_profile,digest

def request(base,token,path,body):
    raw=json.dumps(body,allow_nan=False,separators=(',',':')).encode()
    if len(raw)>16384:raise ProviderUnavailable('Envelope exceeds core limit')
    req=urllib.request.Request(base+path,data=raw,headers={'Authorization':'Bearer '+token,'Content-Type':'application/json'},method='POST')
    # Do not follow redirects with a service token or use environment HTTP proxies.
    class NoRedirect(urllib.request.HTTPRedirectHandler):
        def redirect_request(self,*args,**kwargs):return None
    opener=urllib.request.build_opener(urllib.request.ProxyHandler({}),NoRedirect())
    with opener.open(req,timeout=25) as response:
        data=response.read(65537)
        if len(data)>65536:raise ProviderUnavailable('Core response too large')
        return json.loads(data)

def run_once(base,token,tenant,space_id,directory):
    url=urllib.parse.urlparse(base)
    if url.username or url.password or url.query or url.fragment or url.path not in ('','/') or not url.hostname:
        raise ProviderUnavailable('Invalid core endpoint')
    if url.scheme!='https' and not(url.scheme=='http' and url.hostname in ('127.0.0.1','localhost') and os.environ.get('DZOKA_AI_ALLOW_INSECURE_LOOPBACK')=='true'):
        raise ProviderUnavailable('Core TLS required')
    uuid.UUID(tenant);uuid.UUID(space_id)
    prefix='/api/v1/tenants/'+tenant+'/internal/ai/jobs'
    job=request(base.rstrip('/'),token,prefix+'/claim',{'spaceId':space_id})
    if job is None:return False
    if not isinstance(job,dict):raise ProviderUnavailable('Invalid lease')
    try:
        provider=TextEmbeddingProvider(directory);expected=provider.profile['space']
        if job['space']['spaceKey']!=expected['spaceKey'] or job['inputSha256']!=digest(job['text'].encode()):raise ProviderUnavailable('Input/profile mismatch')
        output=provider.embed(job['text'])
        envelope={'lease':job['lease'],'inputSha256':job['inputSha256'],'spaceKey':expected['spaceKey'],
            'provider':expected['provider'],'outputSchema':'text-embedding-v1','generatedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),**output}
    except (ProviderUnavailable,KeyError,TypeError):
        request(base.rstrip('/'),token,prefix+'/'+str(uuid.UUID(job['jobId']))+'/fail',{'lease':job['lease'],'reason':'PROVIDER_UNAVAILABLE'})
        raise ProviderUnavailable('Inference unavailable; durable job retained') from None
    # Ambiguous completion is safe to resend with this exact envelope; no new inference or timestamp is needed.
    path=prefix+'/'+str(uuid.UUID(job['jobId']))+'/complete'
    for attempt in range(2):
        try:
            if request(base.rstrip('/'),token,path,envelope) is not True:raise ProviderUnavailable('Lease/input revoked')
            return True
        except (TimeoutError,ConnectionError,urllib.error.URLError):
            if attempt:raise ProviderUnavailable('Completion unacknowledged; lease recovery required') from None

def main():
    p=argparse.ArgumentParser();p.add_argument('--once',action='store_true');p.add_argument('--describe-space',action='store_true');args=p.parse_args()
    if args.describe_space:
        print(json.dumps(load_profile()['space'],indent=2));return 0
    if not args.once:p.error('Use --once; scheduling is an explicit operator task')
    try:
        required=[os.environ[k] for k in ['DZOKA_AI_CORE_URL','DZOKA_AI_TOKEN','DZOKA_AI_TENANT','DZOKA_AI_SPACE','DZOKA_AI_MODEL_DIR']]
        processed=run_once(*required);print('Processed one embedding job' if processed else 'No eligible embedding job');return 0
    except Exception:
        print('AI worker unavailable; inspect private configuration and durable job status',file=sys.stderr);return 1

if __name__=='__main__':raise SystemExit(main())
