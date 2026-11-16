import hashlib,json,sys,time,uuid
from pathlib import Path
sys.path.insert(0,str(Path.cwd()/'tools'));import docker_lifecycle as d
from broker_bundle import protect
sys.path.insert(0,str(Path.cwd()/'ai'));from generative_model import locked_model
d.preflight();root=Path('.test-tmp/language-driver-framing-v2-20261116');root.mkdir();protect(root);results=[]
code=r'''import json,struct,subprocess
def frame(text,direction=0,size=None):return struct.pack('<II',direction,len(text) if size is None else size)+text
cases=[b'',b'\x00',struct.pack('<I',0),frame(b'a',2),frame(b'',0),frame(b'x',0,8193),frame(b'x',0,2),frame(b'x')+b'x',frame(b'\x00'),frame(b'\xff'),frame(b'\xed\xa0\x80'),frame(b'\xc0\xaf'),frame(b'<|im_start|>'),frame(b'<think>'),frame(b'</think>'),frame(b'\x01')]
for data in cases:
 result=subprocess.run(['/usr/local/bin/dzoka-afri-translate'],input=data,capture_output=True,timeout=5)
 assert result.returncode==1 and result.stdout==b'' and result.stderr==b'CPU translation unavailable stage 1\n'
positive=subprocess.run(['/usr/local/bin/dzoka-afri-translate'],input=frame(b'public test'),capture_output=True,timeout=5)
assert positive.returncode==1 and positive.stdout==b'' and positive.stderr==b'CPU translation unavailable stage 2\n'
print(json.dumps({'validFrameReachedModelLoader':True,'invalidFrames':len(cases),'allRejectedBeforeModelLoad':True,'noOutput':True}))
'''
for variant in ['afrislm','afrislm4b','morena','morena-fewshot']:
 image=json.loads(Path(f'.test-tmp/{variant}-build-20261116.json').read_bytes())['imageId'];owner=uuid.uuid4().hex;name='dzoka-language-framing-'+owner;record={'owner':owner,'name':name,'image':image,'state':'INTENT'};path=root/(owner+'.json');lease=d.OwnerLock(root,owner);assert lease.acquire();confirmed=False
 def save(state):
  record['state']=state;path.write_text(json.dumps(record,indent=2)+'\n')
 def lookup():
  ids=d.cli(['container','ls','-a','--no-trunc','--filter','name=^/'+name+'$','--format','{{.ID}}']).decode().splitlines();assert len(ids)<=1
  if not ids:return None
  value=json.loads(d.cli(['container','inspect',ids[0]]))[0];assert value['Name']=='/'+name and value['Image']==image and value['Config']['Labels'].get('org.dzoka.driver-test')==owner;return value
 save('INTENT')
 asset_guard=None;mount=[]
 if variant=='morena-fewshot':
  asset=Path.home()/'DzokaMorenaFewshotAssets-20261116-v1/examples.bin';asset_guard=locked_model(asset,{'bytes':1136,'sha256':'e9b7b046b61d7500ed1aa83305171549572f037d78a8e9af2a41f739e86f6125'});asset_path=asset_guard.__enter__();mount=['--mount','type=bind,source='+asset_path+',target=/model/examples.bin,readonly']
 try:
  try:cid=d.cli(['container','create','--pull','never','--name',name,'--label','org.dzoka.driver-test='+owner,'--network','none','--read-only','--cap-drop','ALL','--security-opt','no-new-privileges','--log-driver','none','--ulimit','core=0:0','--memory','128m','--cpus','1','--pids-limit','32','--entrypoint','python3',*mount,image,'-I','-S','-c',code]).decode().strip()
  except d.NotStarted:confirmed=True;save('NOT_CREATED');raise
  except Exception:save('CREATE_UNKNOWN');raise
  confirmed=True;save('CREATED');value=lookup();assert value['Id']==cid and value['Config']['User']=='65532:65532' and value['HostConfig']['NetworkMode']=='none' and value['HostConfig']['ReadonlyRootfs'] and (not value['Mounts'] if not mount else len(value['Mounts'])==1 and value['Mounts'][0]['Source']==asset_path and value['Mounts'][0]['Destination']=='/model/examples.bin' and not value['Mounts'][0]['RW'])
  raw=d.cli(['container','start','--attach',cid],timeout=60);value=lookup();assert not value['State']['Running'] and value['State']['ExitCode']==0;output=json.loads(raw);assert output['validFrameReachedModelLoader'];assert output['invalidFrames']==16 and output['allRejectedBeforeModelLoad'] and output['noOutput'];results.append({'variant':variant,'imageId':image,**output});print(variant,'PASS 16 malformed frames',flush=True)
 finally:
  value=lookup()
  if value:d.cli(['container','rm','-f',value['Id']]);assert lookup() is None;confirmed=True
  assert confirmed;save('REMOVED');lease.close()
  if asset_guard:asset_guard.__exit__(None,None,None)
Path('.test-tmp/language-driver-framing-v2-result-20261116.json').write_text(json.dumps({'results':results,'cases':sum(x['invalidFrames'] for x in results),'scope':'Actual native binary malformed-frame rejection; not model quality, upstream tests or application integration'},indent=2)+'\n')
