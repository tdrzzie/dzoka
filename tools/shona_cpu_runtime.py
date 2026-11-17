"""Explicit reviewed artifact checks and real native CPU conformance probes."""
import argparse,hashlib,json,subprocess,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'ai'))
from shona_cpu import profiles
SAMPLES={'CPU_EN_SN':['I lost my bag.','I left a black bag in Joina City.'],'CPU_SN_EN':['Ndarasa bhegi rangu.','Ndasiya black Lenovo bag muJoina around 2pm.'],'CPU_EN_SN_KEEP':['I left a black bag in [[keep:Joina City]].'],'CPU_SN_EN_KEEP':['Ndasiya [[keep:black Lenovo bag]] mu[[keep:Joina]] [[keep:around 2pm]].'],'CPU_SN_EN_AUTO':['Ndasiya black Lenovo bag muJoina around 2pm.','Ndarasa bhegi rangu.','Ndasiya my black laptop bag muJoina.']}

def image_readiness(check,refresh,command,record=lambda _:None):
    """Retry only a timed-out exact read-only inspect; never repeat inference."""
    for attempt in range(1,4):
        try:check()
        except subprocess.TimeoutExpired as error:
            if error.cmd!=command:raise
            record(attempt)
            if attempt==3:raise
            refresh()
        else:return attempt

def check_artifacts():
    registry=profiles()
    if registry!=json.loads((ROOT/'ai/shona-cpu-profiles.json').read_bytes()):raise ValueError('CPU profile registry changed')
    for entry in json.loads((ROOT/'ai/shona-cpu-models.lock.json').read_bytes())['files']:
        target=ROOT/'.audit-cache/shona-cpu-models'/entry['local']
        with target.open('rb') as source:digest=hashlib.file_digest(source,'sha256').hexdigest()
        if target.stat().st_size!=entry['bytes'] or digest!=entry['sha256']:raise ValueError('CPU model artifact changed')
    return registry

def main():
    parser=argparse.ArgumentParser();parser.add_argument('operation',choices=['check','profiles','probe','runtime-check']);args=parser.parse_args();registry=profiles()
    if registry!=json.loads((ROOT/'ai/shona-cpu-profiles.json').read_bytes()):raise ValueError('CPU profile registry changed')
    if args.operation=='profiles':print(json.dumps(registry,indent=2));return
    check_artifacts()
    print('PASS: all 14 CPU translation/LID artifacts and five profile identities',flush=True)
    if args.operation=='check':return
    script="""import json,sys
sys.path[:0]=['ai','tools']
from shona_cpu import ShonaCpuProvider
from shona_cpu_runtime import image_readiness,check_artifacts
from generative_integrity import verify_runtime
verify_runtime()
class ProbeProvider(ShonaCpuProvider):
 def check_image(self):
  expected=json.load(open('ai/shona-cpu-runtime.lock.json'))['imageId']
  if self.image!=expected:raise ValueError('Pinned probe image changed')
  def refresh():verify_runtime();check_artifacts()
  image_readiness(lambda:ShonaCpuProvider.check_image(self),refresh,['docker','image','inspect',expected,'--format','{{.Id}}'],lambda attempt:print(json.dumps({'phase':'IMAGE_INSPECT_TIMEOUT','attempt':attempt}),file=sys.stderr,flush=True))
if sys.argv[1]=='runtime-check':
 p=object.__new__(ProbeProvider);p.image=json.load(open('ai/shona-cpu-runtime.lock.json'))['imageId'];p.check_image();print('PASS: reviewed 22-wheel host and exact CPU container image')
else:
 with ProbeProvider(sys.argv[1],'.audit-cache/shona-cpu-models') as p:
  for source in json.loads(sys.argv[2]):
   e=p.infer(source.encode());print(json.dumps({'profile':sys.argv[1],'profileKey':p.profile['key'],'source':source,'evidence':e}),flush=True)
"""
    names=['runtime-check'] if args.operation=='runtime-check' else list(SAMPLES)
    rows=[]
    for name in names:
        run=subprocess.run([str(ROOT/'.audit-cache/generative-venv/Scripts/python.exe'),'-I','-c',script,name,json.dumps(SAMPLES.get(name,[]))],capture_output=True,text=True,encoding='utf-8',cwd=ROOT,timeout=290)
        if run.returncode:raise ValueError('CPU native probe failed; private diagnostics: '+run.stderr[-2000:])
        for line in run.stderr.splitlines():
            try:event=json.loads(line)
            except ValueError:continue
            if isinstance(event,dict) and set(event)=={'phase','attempt'} and event['phase']=='IMAGE_INSPECT_TIMEOUT' and type(event['attempt']) is int and 1<=event['attempt']<=3:print(json.dumps(event),file=sys.stderr,flush=True)
        print(run.stdout,end='',flush=True)
        if name!='runtime-check':rows.extend(json.loads(line) for line in run.stdout.splitlines())
    if rows:(ROOT/'.test-tmp/shona-cpu-probe.json').write_text(json.dumps(rows,indent=2)+'\n',encoding='utf-8')
if __name__=='__main__':main()
