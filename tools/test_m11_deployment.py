"""Actual packaged two-replica Compose rehearsal. Fresh task-owned resources only."""
import argparse
from concurrent.futures import ThreadPoolExecutor
import json
import ipaddress
import os
from pathlib import Path
import secrets
import socket
import math
import statistics
import subprocess
import time
import urllib.error
import urllib.request
import uuid

ROOT=Path(__file__).resolve().parents[1]

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--java-home',required=True,type=Path)
    args=parser.parse_args()
    identity=uuid.uuid4().hex[:12];project='dzoka-m11-'+identity
    run=ROOT/'.test-tmp'/project;run.mkdir(parents=True)
    env=os.environ.copy()
    def command(*parts,input=None):
        result=subprocess.run([str(p) for p in parts],cwd=ROOT,env=env,input=input,capture_output=True,text=True,encoding='utf-8',timeout=180)
        # Do not echo argv, configuration, SQL, headers, provider output or secrets on failure.
        if result.returncode:
            (run/'command-failure.log').write_text(result.stdout+'\n'+result.stderr,encoding='utf-8')
            raise RuntimeError('Deployment command failed: '+str(parts[0])+'; private diagnostic retained in '+str(run/'command-failure.log'))
        return result.stdout.strip()
    def port():
        with socket.socket() as s:s.bind(('127.0.0.1',0));return s.getsockname()[1]
    ports=[port(),port()]
    # This host may have exhausted Docker's automatic pools. Choose only from an explicit
    # private range after checking Docker IPAM and host routes; never reconfigure old networks.
    used=[];networks=command('docker','network','ls','-q').splitlines()
    for start in range(0,len(networks),20):
        for network in json.loads(command('docker','network','inspect',*networks[start:start+20])):
            for entry in network.get('IPAM',{}).get('Config') or []:
                if entry.get('Subnet'):used.append(ipaddress.ip_network(entry['Subnet'],strict=False))
    if os.name=='nt':
        prefixes=json.loads(command('powershell.exe','-NoProfile','-Command',"ConvertTo-Json -Compress -InputObject @(Get-NetRoute -AddressFamily IPv4 | Select-Object -ExpandProperty DestinationPrefix)"))
        used.extend(ipaddress.ip_network(p,strict=False) for p in prefixes if p!='0.0.0.0/0')
    subnet=next((s for s in ipaddress.ip_network('10.240.0.0/12').subnets(new_prefix=28) if all(s.version!=n.version or not s.overlaps(n) for n in used)),None)
    if subnet is None:raise RuntimeError('No nonoverlapping test subnet; supply a network plan before deployment')
    db=run/'db-secrets';config=run/'app-config';fixtures=run/'fixtures'
    for folder in (db,config,fixtures):folder.mkdir()
    passwords={name:secrets.token_urlsafe(32) for name in ('control','migrator','app')}
    for name,value in passwords.items():(db/name).write_text(value,encoding='utf-8')
    (config/'spring.datasource.password').write_text(passwords['app'],encoding='utf-8')
    (config/'dzoka.security.allow-insecure-loopback').write_text('true',encoding='utf-8')
    # Explicitly test-only issuer uses the same loopback URI inside each API container.
    env.update(DZOKA_CORE_IMAGE='dzoka-m11-local:'+identity,DZOKA_DB_SECRETS_DIR=str(db),DZOKA_APP_CONFIG_DIR=str(config),
        DZOKA_OIDC_ISSUER='http://127.0.0.1:19090',DZOKA_OIDC_JWK_SET_URI='http://127.0.0.1:19090/jwks',DZOKA_OIDC_AUDIENCE='dzoka-core',
        DZOKA_NETWORK_SUBNET=str(subnet),
        DZOKA_HTTP_PORT=str(ports[0]),DZOKA_SECOND_HTTP_PORT=str(ports[1]))
    command(args.java_home/'bin/javac.exe','--release','21','-cp',ROOT/'target/core-layer/BOOT-INF/lib/*','-d',fixtures,ROOT/'tools/deployment/ProbeIssuer.java')
    command(args.java_home/'bin/java.exe','-cp',str(fixtures)+os.pathsep+str(ROOT/'target/core-layer/BOOT-INF/lib/*'),'ProbeIssuer','init',fixtures)
    # Override contains a fixture mount only, never production defaults or a live user's credentials.
    override=run/'fixtures.compose.json'
    override.write_text(json.dumps({'services':{s:{'volumes':[{'type':'bind','source':str(fixtures),'target':'/fixtures','read_only':True}]} for s in ('core','core-secondary')}}),encoding='utf-8')
    compose=['docker','compose','-p',project,'-f',str(ROOT/'deployment/compose.yaml'),'-f',str(override),'--profile','replicas']
    started=False
    def call(method,path,which=0,body=None,key=None,auth=True):
        headers={'Content-Type':'application/json'}
        if auth:headers['Authorization']='Bearer '+(fixtures/'test-token').read_text()
        if key:headers['Idempotency-Key']=key
        request=urllib.request.Request(f'http://127.0.0.1:{ports[which]}'+path,data=None if body is None else json.dumps(body).encode(),headers=headers,method=method)
        try:
            with urllib.request.urlopen(request,timeout=12) as response:return response.status,json.load(response)
        except urllib.error.HTTPError as error:return error.code,{}
    def wait_ready(which):
        deadline=time.monotonic()+60
        while time.monotonic()<deadline:
            try:
                if call('GET','/health/ready',which,auth=False)[0]==200:return
            except (OSError,ValueError):pass
            time.sleep(.5)
        raise RuntimeError('Deployment readiness deadline exceeded')
    def container(service):return command(*compose,'ps','-q',service)
    def issuer(service):command('docker','exec','-d',container(service),'java','-cp','/fixtures:/app/BOOT-INF/lib/*','ProbeIssuer','serve','/fixtures')
    try:
        command(*compose,'config','--quiet')
        command('docker','build','-f','deployment/core.Dockerfile','-t',env['DZOKA_CORE_IMAGE'],'.')
        started=True;command(*compose,'up','-d')
        for which in range(2):wait_ready(which)
        for service in ('core','core-secondary'):issuer(service)
        tenant=str(uuid.uuid4())
        sql=f"INSERT INTO dzoka.tenants(id,name) VALUES('{tenant}','Synthetic deployment');\nINSERT INTO dzoka.memberships(tenant_id,issuer,subject,permissions) VALUES('{tenant}','http://127.0.0.1:19090','deployment',ARRAY['organizations:read','organizations:write']);"
        command('docker','exec','-i',container('db'),'psql','-X','-U','dzoka_control','-d','dzoka','-v','ON_ERROR_STOP=1',input=sql)
        path='/api/v1/tenants/'+tenant+'/organizations';key=str(uuid.uuid4());began=time.monotonic()
        for which in range(2):
            deadline=time.monotonic()+10
            while call('GET',path,which)[0]!=200:
                if time.monotonic()>deadline:raise RuntimeError('Deployment test issuer unavailable')
                time.sleep(.2)
        with ThreadPoolExecutor(max_workers=6) as workers:
            responses=list(workers.map(lambda n:call('POST',path,n%2,{'name':'Synthetic replica race'},key),range(12)))
        if any(code!=200 for code,_ in responses) or len({json.dumps(body,sort_keys=True) for _,body in responses})!=1:raise RuntimeError('Cross-replica idempotency failed')
        elapsed=time.monotonic()-began
        def measured(n):
            start=time.perf_counter();code,_=call('GET',path,n%2)
            if code!=200:raise RuntimeError('Diagnostic load request failed')
            return time.perf_counter()-start
        load_start=time.perf_counter()
        with ThreadPoolExecutor(max_workers=6) as workers:latencies=sorted(workers.map(measured,range(100)))
        load={'scope':'100 authenticated reads of one synthetic organization across two replicas','concurrency':6,'requests':100,'elapsedSeconds':round(time.perf_counter()-load_start,3),'medianMs':round(statistics.median(latencies)*1000,2),'p95Ms':round(latencies[math.ceil(.95*len(latencies))-1]*1000,2),'p99Ms':round(latencies[math.ceil(.99*len(latencies))-1]*1000,2)}
        if call('GET',path,1)[0]!=200 or call('GET',path,auth=False)[0]!=401:raise RuntimeError('Deployment authorization failed')
        command(*compose,'stop','core')
        if call('GET',path,1)[0]!=200:raise RuntimeError('Surviving replica failed')
        command(*compose,'start','core');wait_ready(0);issuer('core')
        deadline=time.monotonic()+10
        while call('GET',path,0)[0]!=200:
            if time.monotonic()>deadline:raise RuntimeError('Restarted replica failed')
            time.sleep(.2)
        expected_rows=call('GET',path,1)[1]
        command(*compose,'stop','db')
        if call('GET','/health/ready',auth=False)[0]!=503 or call('GET',path,1)[0]!=503:raise RuntimeError('Database outage did not fail closed')
        command(*compose,'start','db')
        for which in range(2):wait_ready(which)
        if any(call('GET',path,n)!=(200,expected_rows) for n in range(2)):raise RuntimeError('Database recovery failed or committed data changed')
        settings=json.loads(command('docker','inspect',container('core')))[0]
        if settings['Config']['User']!='10001:10001' or not settings['HostConfig']['ReadonlyRootfs'] or 'ALL' not in settings['HostConfig']['CapDrop']:raise RuntimeError('Container isolation mismatch')
        result={'status':'PASS','profile':'ISOLATED_CURRENT_ACCOUNT_COMPOSE','replicas':2,'concurrentIdenticalRequests':12,'logicalCreates':1,'raceElapsedSeconds':round(elapsed,3),'anonymousDenied':True,'survivingReplica':True,'coreRestart':True,'databaseOutage503':True,'databaseRecovery':True,'nonrootReadonlyCapDrop':True,'coreImageId':command('docker','image','inspect',env['DZOKA_CORE_IMAGE'],'--format','{{.Id}}'),'limits':['ephemeral loopback test issuer','single-host database; no HA/PITR claim','synthetic small race; no capacity/SLO claim','no external providers or funds']}
        result['diagnosticReadLoad']=load
        (run/'result.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result,indent=2));print('Evidence:',run/'result.json')
    finally:
        if started:
            if not project.startswith('dzoka-m11-') or len(identity)!=12:raise RuntimeError('Unsafe cleanup project')
            try:(run/'services.log').write_text(command(*compose,'logs','--no-color'),encoding='utf-8')
            finally:command(*compose,'down','--volumes','--remove-orphans')

if __name__=='__main__':main()
