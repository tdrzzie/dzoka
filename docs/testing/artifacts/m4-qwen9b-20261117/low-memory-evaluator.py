import hashlib,json,os,shutil,struct,subprocess,sys,time,uuid
from pathlib import Path
repo=Path.cwd();sys.path[:0]=[str(repo/'tools'),str(repo/'ai')]
from broker_bundle import protect,copy_checked
from generative_model import locked_model
from model_evaluation import score
import docker_lifecycle as docker
def sha(raw):return hashlib.sha256(raw).hexdigest()
build=json.loads((repo/'.test-tmp/qwen9b-build-20261116.json').read_bytes());image=build['imageId'];docker.preflight()
assert docker.cli(['image','inspect',image,'--format','{{.Id}}']).decode().strip()==image
memory=json.loads(docker.cli(['run','--rm','--pull','never','--network','none','--read-only','--cap-drop','ALL','--security-opt','no-new-privileges','--memory','128m','--pids-limit','16','--entrypoint','python3',image,'-I','-S','-c',"import json;from pathlib import Path;m={x.split(':')[0]:int(x.split()[1])*1024 for x in Path('/proc/meminfo').read_text().splitlines() if x.startswith(('MemAvailable:','MemTotal:'))};print(json.dumps(m))"],timeout=30))
assert memory['MemAvailable']>=5*1024**3,'Insufficient available guest memory; do not change shared workloads'
reference=repo/'.audit-cache/public-reference-20261106'
def member(name,digest):
 raw=(reference/name).read_bytes();assert sha(raw)==digest
 return raw.decode().splitlines()
en=member('eng_Latn.devtest','612e9fbe87997617c0fa8fa8929654a4f49b728d96738112c2b86ef6a1d78d88')
sn=member('sna_Latn.devtest','92cb2659b91ef2ad96b88b3816b5837816d29240cbb2ea4197c9ac090f4dd1bb')
cases=[]
for direction,indices,slice_name in [('sn-en',range(65,73),'development'),('sn-en',range(193,201),'additional'),('en-sn',range(193,201),'additional')]:
 for n in indices:
  source,target=(sn[n-1],en[n-1]) if direction=='sn-en' else (en[n-1],sn[n-1])
  cases.append({'direction':direction,'row':n,'slice':slice_name,'source':source,'reference':target,'expectedOutputFragments':[]})
authored=[
 ('Ndasiya black Lenovo bag muJoina around 2pm.','I left a black Lenovo bag at Joina around 2pm.',['Lenovo','Joina','around 2pm']),
 ('Ndarasa blue Samsung phone paAvondale yesterday.','I lost a blue Samsung phone at Avondale yesterday.',['Samsung','Avondale','yesterday']),
 ('Ndakawana red backpack paMbare bus terminus at 6am.','I found a red backpack at Mbare bus terminus at 6am.',['Mbare','6am']),
 ('Handina kurasa wallet yangu; ndine black bag only.','I did not lose my wallet; I only have a black bag.',['not','black']),
 ('Ndakasiya silver HP laptop near Borrowdale maybe on Monday.','I left a silver HP laptop near Borrowdale maybe on Monday.',['HP','Borrowdale','maybe','Monday']),
 ('Ndakawana umbrella yered paEastgate around noon.','I found a red umbrella at Eastgate around noon.',['Eastgate','around noon']),
 ('Ndiri kutsvaga green suitcase ine Adidas logo.','I am looking for a green suitcase with an Adidas logo.',['Adidas','green']),
 ('Bag racho rine ZX-742 label, asi handizivi owner.','The bag has a ZX-742 label, but I do not know the owner.',['ZX-742','not']),
]
cases += [{'direction':'sn-en','row':n+1,'slice':'authored-code-switch','source':source,'reference':target,'expectedOutputFragments':critical} for n,(source,target,critical) in enumerate(authored)]
root=Path.home()/'DzokaQwen9BLowMemoryDiagnostic-20261117-v1';root.mkdir();protect(root)
for child in ('state','models'):(root/child).mkdir();protect(root/child)
model=root/'models/model.gguf';entry={'bytes':3649365216,'sha256':'a31b6b4c603ddd66b5496ee065c9dd13b2b6de77dce9e5d59609aef8d39868e7'}
copy_checked(repo/'.audit-cache/qwen9b-lowmem-models-20261117/model.gguf',model,entry);protect(model)
selection=[{k:v for k,v in row.items() if k not in ('source','reference')}|{'inputSha256':sha(row['source'].encode()),'referenceSha256':sha(row['reference'].encode())} for row in cases]
plan={'scope':'Public licensed FLORES + separate authored code-switch regression, unknown training overlap; not native-speaker or representative approval','build':build,'model':entry,'rows':selection,'screen':{'requireAllAvailable':True,'minimumCompletePerDirectionSlice':8,'maxMedianCer':0.25,'maxCaseCer':0.60,'authoredExpectedOutputFragmentsMustAllMatchExactly':True},'promptVersion':'qwen9b-strict-v1','promptTokenCeiling':1536,'outputTokenCeiling':256,'weightQuantization':'UD-IQ2_M','memoryBytes':5*1024**3,'memoryPlusSwapBytes':5*1024**3,'guestMemoryPreflight':memory,'scriptSha256':sha(Path(__file__).read_bytes())}
(root/'plan.json').write_text(json.dumps(plan,indent=2)+'\n');rows=[]
def save():
 target=root/'result.json';temporary=root/'result.pending';temporary.write_text(json.dumps({'planSha256':sha((root/'plan.json').read_bytes()),'rows':rows},indent=2)+'\n');os.replace(temporary,target)
def persist(record,status):
 record['history'].append({'state':status,'at':time.time()});target=root/'state'/(record['owner']+'.json');temporary=target.with_suffix('.pending')
 with temporary.open('w') as output:json.dump(record,output);output.flush();os.fsync(output.fileno())
 os.replace(temporary,target)
def lookup(record):
 ids=docker.cli(['container','ls','-a','--no-trunc','--filter','name=^/'+record['name']+'$','--format','{{.ID}}']).decode().splitlines()
 assert len(ids)<=1 and all(len(x)==64 and all(c in '0123456789abcdef' for c in x) for x in ids)
 if not ids:return None
 value=json.loads(docker.cli(['container','inspect',ids[0]]));assert len(value)==1
 container=value[0];labels=container['Config']['Labels'];assert container['Name']=='/'+record['name'] and container['Image']==image and container['Config']['Image']==image and labels.get('org.dzoka.diagnostic')=='qwen9b-v1' and labels.get('org.dzoka.invocation')==record['owner']
 return container
with locked_model(model,entry) as model_path:
 for ordinal,case in enumerate(cases):
  owner=uuid.uuid4().hex;record={'owner':owner,'name':'dzoka-afrislm4b-diagnostic-'+owner,'image':image,'history':[]};lease=docker.OwnerLock(root/'state',owner);assert lease.acquire();persist(record,'INTENT');confirmed=False
  row={**selection[ordinal],'status':'UNAVAILABLE'};start=time.monotonic()
  try:
   command=['container','create','--pull','never','--name',record['name'],'--label','org.dzoka.diagnostic=qwen9b-v1','--label','org.dzoka.invocation='+owner,'--log-driver','none','--ulimit','core=0:0','--network','none','--read-only','--cap-drop','ALL','--security-opt','no-new-privileges','--pids-limit','64','--memory','5g','--memory-swap','5g','--cpus','2','--tmpfs','/tmp:rw,noexec,nosuid,size=16m','-i','--mount','type=bind,source='+model_path+',target=/model/model.gguf,readonly',image]
   try:identifier=docker.cli(command).decode().strip()
   except docker.NotStarted:persist(record,'NOT_CREATED');confirmed=True;raise
   except Exception:persist(record,'CREATE_UNKNOWN');raise
   assert len(identifier)==64;confirmed=True;persist(record,'CREATED');container=lookup(record);assert container['Id']==identifier
   config=container['HostConfig'];assert container['Config']['User']=='65532:65532' and config['NetworkMode']=='none' and config['ReadonlyRootfs'] and not config['Privileged'] and config['LogConfig']['Type']=='none' and config['Memory']==5*1024**3 and config['MemorySwap']==5*1024**3 and config['NanoCpus']==2000000000 and config['PidsLimit']==64 and 'ALL' in config['CapDrop'] and 'no-new-privileges' in config['SecurityOpt']
   assert any(u['Name']=='core' and u['Soft']==u['Hard']==0 for u in config['Ulimits'])
   assert len(container['Mounts'])==1 and container['Mounts'][0]['Destination']=='/model/model.gguf' and not container['Mounts'][0]['RW'] and container['Mounts'][0]['Type']=='bind'
   data=case['source'].encode();frame=struct.pack('<II',0 if case['direction']=='en-sn' else 1,len(data))+data;persist(record,'STARTED')
   result=subprocess.run(['docker','--host',docker.LOCAL_HOST,'container','start','--attach','--interactive',identifier],input=frame,capture_output=True,timeout=300,env=docker.docker_env(),creationflags=subprocess.CREATE_NO_WINDOW)
   (root/f'{ordinal:03d}.stderr').write_bytes(result.stderr);assert len(result.stderr)<=65536
   container=lookup(record);assert container and not container['State']['Running'];row['containerExitCode']=container['State']['ExitCode']
   if result.returncode or container['State']['ExitCode']:raise RuntimeError('Native unavailable')
   raw=result.stdout;assert 24<len(raw)<=8216
   magic,source_count,prompt_count,output_count,ended,length=struct.unpack('<6I',raw[:24]);assert magic==0x314b5a44 and 1<=source_count<=8192 and 1<=prompt_count<=1536 and 1<=output_count<=256 and ended in (0,1) and (ended or output_count==256) and 1<=length<=8192 and len(raw)==24+length
   text=raw[24:].decode().strip();assert text and '\0' not in text and len(text.encode('utf-16-le'))//2<=2048 and '<|' not in text and '<think>' not in text and '</think>' not in text
   evidence={'text':text,'sourceTokenCount':source_count,'promptTokenCount':prompt_count,'outputTokenCount':output_count,'inputTruncated':False,'outputTruncated':not ended,'imageId':image,'rawOutputSha256':sha(raw),'trust':'UNVERIFIED_TRANSLATION'}
   path=root/f'{ordinal:03d}.json'
   with path.open('x') as output:json.dump(evidence,output,ensure_ascii=True,indent=2)
   row.update(status='AVAILABLE' if ended else 'TRUNCATED',cer=score('cer',text,case['reference']),evidenceSha256=sha(path.read_bytes()),expectedOutputFragmentMatches=[value in text for value in case['expectedOutputFragments']])
  except Exception as error:row['errorType']=type(error).__name__
  finally:
   try:
    existing=lookup(record)
    if existing:docker.cli(['container','rm','-f',existing['Id']]);assert lookup(record) is None;confirmed=True
    assert confirmed,'Uncertain create cannot be cleared by an empty lookup'
    persist(record,'REMOVED')
   except Exception:persist(record,'CLEANUP_UNAVAILABLE');raise
   finally:lease.close()
  row['seconds']=round(time.monotonic()-start,2);rows.append(row);save();print(ordinal,case['direction'],case['row'],row['status'],row.get('cer'),row.get('errorType'),flush=True)
  if ordinal==0 and row['status']=='UNAVAILABLE':raise SystemExit('Initial compatibility failed; private evidence retained')
print('COMPLETED',sha((root/'result.json').read_bytes()),flush=True)
