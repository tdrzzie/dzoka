"""Private one-job perception consumer; exact bounded requests, no cloud/DB/S3 authority."""
import argparse
import datetime
import hashlib
import json
import os
from pathlib import Path
import sys
import urllib.error
import urllib.parse
import urllib.request
import uuid
sys.path.insert(0,str(Path(__file__).resolve().parent))
from perception import PerceptionProvider

class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self,*args,**kwargs):return None

def request(base,token,path,body,binary=False):
    data=json.dumps(body,allow_nan=False,separators=(',',':'),ensure_ascii=True).encode()
    if len(data)>65536:raise ValueError('Perception envelope exceeds bound')
    req=urllib.request.Request(base+path,data=data,headers={'Authorization':'Bearer '+token,'Content-Type':'application/json'},method='POST')
    opener=urllib.request.build_opener(urllib.request.ProxyHandler({}),NoRedirect())
    limit=20*1024*1024 if binary else 65536
    with opener.open(req,timeout=30) as response:
        data=response.read(limit+1)
        if len(data)>limit:raise ValueError('Input exceeds bound')
        return data if binary else json.loads(data)

def run_once(base,token,tenant,profile,directory):
    uri=urllib.parse.urlparse(base)
    if uri.username or uri.password or uri.query or uri.fragment or uri.path not in ('','/') or not uri.hostname:raise ValueError('Invalid endpoint')
    if uri.scheme!='https' and not(uri.scheme=='http' and uri.hostname in ('127.0.0.1','localhost') and os.environ.get('DZOKA_AI_ALLOW_INSECURE_LOOPBACK')=='true'):raise ValueError('TLS required')
    uuid.UUID(tenant);uuid.UUID(profile);base=base.rstrip('/');prefix='/api/v1/tenants/'+tenant+'/internal/perception/jobs'
    job=request(base,token,prefix+'/claim',{'profileId':profile})
    if job is None:return False
    id=str(uuid.UUID(job['jobId']));lease=str(uuid.UUID(job['lease']))
    try:
        provider=PerceptionProvider(job['task'],directory)
        if provider.profile['key']!=job['profileKey']:raise ValueError('Profile mismatch')
        data=request(base,token,prefix+'/'+id+'/input',{'lease':lease},True)
        if len(data)!=job['inputBytes'] or hashlib.sha256(data).hexdigest()!=job['inputSha256']:raise ValueError('Input mismatch')
        evidence=provider.infer(data)
        envelope={'lease':lease,'inputSha256':job['inputSha256'],'profileKey':job['profileKey'],
                  'generatedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'evidence':evidence}
    except Exception:
        request(base,token,prefix+'/'+id+'/fail',{'lease':lease,'reason':'PROVIDER_UNAVAILABLE'})
        raise ValueError('Provider unavailable; bounded durable retry retained') from None
    for attempt in range(2):
        try:
            if request(base,token,prefix+'/'+id+'/complete',envelope) is not True:raise ValueError('Lease revoked')
            return True
        except (TimeoutError,ConnectionError,urllib.error.URLError):
            if attempt:raise ValueError('Completion unacknowledged') from None

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--once',action='store_true',required=True);parser.parse_args()
    try:
        run_once(*(os.environ[k] for k in ('DZOKA_AI_CORE_URL','DZOKA_AI_TOKEN','DZOKA_AI_TENANT','DZOKA_AI_PROFILE','DZOKA_AI_PERCEPTION_DIR')))
        print('Perception invocation completed');return 0
    except Exception:
        print('Perception worker unavailable; inspect private configuration and safe durable status',file=sys.stderr);return 1

if __name__=='__main__':raise SystemExit(main())
