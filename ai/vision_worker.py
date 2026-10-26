"""Private joint/detector/segmenter consumer using the existing reviewed perception transport."""
import argparse
import datetime
import hashlib
import os
from pathlib import Path
import sys
import urllib.error
import urllib.parse
import uuid
sys.path.insert(0,str(Path(__file__).resolve().parent))
from perception_worker import request
from vision import VisionProvider

def run_once(base,token,tenant,profile,directory):
    uri=urllib.parse.urlparse(base)
    if uri.username or uri.password or uri.query or uri.fragment or uri.path not in ('','/') or not uri.hostname:raise ValueError('Invalid core endpoint')
    if uri.scheme!='https' and not(uri.scheme=='http' and uri.hostname in ('127.0.0.1','localhost') and os.environ.get('DZOKA_AI_ALLOW_INSECURE_LOOPBACK')=='true'):raise ValueError('TLS required')
    uuid.UUID(tenant);uuid.UUID(profile);base=base.rstrip('/');prefix='/api/v1/tenants/'+tenant+'/internal/perception/jobs'
    job=request(base,token,prefix+'/claim',{'profileId':profile})
    if job is None:return False
    id=str(uuid.UUID(job['jobId']));lease=str(uuid.UUID(job['lease']))
    try:
        provider=VisionProvider(job['task'],directory)
        if job['profileKey']!=provider.profile['key']:raise ValueError('Profile mismatch')
        raw=request(base,token,prefix+'/'+id+'/input',{'lease':lease},True)
        if len(raw)!=job['inputBytes'] or hashlib.sha256(raw).hexdigest()!=job['inputSha256']:raise ValueError('Input integrity')
        evidence=provider.infer(raw)
        envelope={'lease':lease,'inputSha256':job['inputSha256'],'profileKey':job['profileKey'],'generatedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'evidence':evidence}
    except Exception:
        request(base,token,prefix+'/'+id+'/fail',{'lease':lease,'reason':'PROVIDER_UNAVAILABLE'});raise ValueError('Vision provider unavailable') from None
    for attempt in range(2):
        try:
            if request(base,token,prefix+'/'+id+'/complete',envelope) is not True:raise ValueError('Lease revoked')
            return True
        except (TimeoutError,ConnectionError,urllib.error.URLError):
            if attempt:raise ValueError('Completion unacknowledged') from None

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--once',action='store_true',required=True);parser.parse_args()
    try:
        run_once(*(os.environ[k] for k in ('DZOKA_AI_CORE_URL','DZOKA_AI_TOKEN','DZOKA_AI_TENANT','DZOKA_AI_PROFILE','DZOKA_AI_VISION_DIR')))
        print('Vision invocation completed')
    except Exception:print('Vision worker unavailable; inspect private configuration and durable status',file=sys.stderr);raise SystemExit(1)
