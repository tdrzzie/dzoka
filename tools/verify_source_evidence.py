"""Read-only live revalidation of pinned audit bytes. No upstream code is executed or installed."""
import argparse
from concurrent.futures import ThreadPoolExecutor,as_completed
from datetime import datetime,timezone
import hashlib
import json
from pathlib import Path
import urllib.parse
import urllib.request

ROOT=Path(__file__).resolve().parents[1]

def verify(source,file):
    result={'sourceId':source['id'],'path':file['path'],'expectedSha256':file['sha256']}
    if source['kind']=='repository':
        repo=source['repository'].removeprefix('https://github.com/')
        url='https://raw.githubusercontent.com/'+repo+'/'+source['revision']+'/'+urllib.parse.quote(file['path'],safe='/')
    else:url=file['url']
    result['url']=url
    try:
        req=urllib.request.Request(url,headers={'User-Agent':'Dzoka-audit-evidence-revalidation/1'})
        with urllib.request.urlopen(req,timeout=45) as response:
            data=response.read(20_000_001)
            if len(data)>20_000_000:raise ValueError('Evidence exceeds download bound')
        result['bytes']=len(data);result['actualSha256']=hashlib.sha256(data).hexdigest()
        result['sha256Match']=result['actualSha256']==file['sha256']
        if source['kind']=='repository':
            blob=hashlib.sha1(b'blob '+str(len(data)).encode()+b'\0'+data).hexdigest()
            result['actualBlobSha']=blob;result['blobMatch']=blob==file['blobSha']
            text=data.decode('utf-16' if data.startswith((b'\xff\xfe',b'\xfe\xff')) else 'utf-8-sig')
            result['actualLineCount']=len(text.splitlines());result['lineCountMatch']=result['actualLineCount']==file['lineCount']
        else:result['byteLengthMatch']=len(data)==file['byteLength']
        result['status']='PASS' if all(v for k,v in result.items() if k.endswith('Match')) else 'MISMATCH'
    except Exception as error:
        # Never print source contents, response bodies or suspected upstream credentials.
        result['status']='UNVERIFIED';result['errorType']=type(error).__name__
        if hasattr(error,'code'):result['httpStatus']=error.code
    return result

def main():
    p=argparse.ArgumentParser();p.add_argument('--output',default='.audit-cache/m0-live-verification.json');p.add_argument('--workers',type=int,default=4);args=p.parse_args()
    if args.workers<1 or args.workers>8:p.error('Use 1–8 download workers')
    output=(ROOT/args.output).resolve()
    if not output.is_relative_to(ROOT):p.error('Output must stay inside this workspace')
    lock=json.loads((ROOT/'docs/source-audit/source-audit.lock.json').read_text(encoding='utf-8'))
    tasks=[(s,f) for s in lock['sources'] for f in json.loads((ROOT/s['evidence']).read_text(encoding='utf-8'))['files']]
    results=[]
    with ThreadPoolExecutor(max_workers=args.workers) as pool:
        pending=[pool.submit(verify,*pair) for pair in tasks]
        for future in as_completed(pending):
            r=future.result();results.append(r)
            if r['status']!='PASS':print(r['status'],r['sourceId'],r['path'],r.get('errorType','digest/line mismatch'),flush=True)
            if len(results)%20==0:print(f'Checked {len(results)}/{len(tasks)} pinned artifacts',flush=True)
    report={'verifiedAt':datetime.now(timezone.utc).isoformat(),'registrySha256':hashlib.sha256((ROOT/'docs/source-audit/source-audit.lock.json').read_bytes()).hexdigest(),
        'scope':'Original bytes at recorded immutable revisions; not upstream tests, security certification or proof of every narrative conclusion',
        'total':len(results),'passed':sum(r['status']=='PASS' for r in results),'mismatched':sum(r['status']=='MISMATCH' for r in results),
        'unverified':sum(r['status']=='UNVERIFIED' for r in results),'files':sorted(results,key=lambda r:(r['sourceId'],r['path']))}
    output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8',newline='\n')
    print('Evidence byte verification:',{k:report[k] for k in ['total','passed','mismatched','unverified']},flush=True)
    return 0 if report['passed']==report['total'] else 1

if __name__=='__main__':raise SystemExit(main())
