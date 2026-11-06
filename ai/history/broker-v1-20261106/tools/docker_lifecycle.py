"""Fixed local Docker resources, durable non-PII intents and active-owner exclusion."""
from contextlib import ExitStack,contextmanager
from contextvars import ContextVar
from datetime import datetime,timezone
import json
import hashlib
import os
from pathlib import Path
import re
import queue
import threading
import subprocess
import sys
import time
import uuid
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'ai'))
HEX=re.compile(r'^[0-9a-f]{32}$');ID=re.compile(r'^[0-9a-f]{64}$')
LOCAL_HOST=None
class NotStarted(OSError):pass
RUNTIME_BYTES=(ROOT/'ai/shona-cpu-runtime.lock.json').read_bytes()
MODEL_BYTES=(ROOT/'ai/shona-cpu-models.lock.json').read_bytes()
APPROVED_IMAGE=json.loads(RUNTIME_BYTES)['imageId']
APPROVED_FILES=json.loads(MODEL_BYTES)['files']
MANAGED='org.dzoka.supervision';OWNER='org.dzoka.invocation';RESOURCE='org.dzoka.resource'

def require(value):
    if not value:raise ValueError('Invalid Docker supervision state')

def docker_env():
    return {k:v for k,v in os.environ.items() if k in {'PATH','SYSTEMROOT','WINDIR','HOME','TEMP','TMP','USERPROFILE','APPDATA','LOCALAPPDATA'}}

BUDGET=ContextVar('docker-operation-budget',default=None)
@contextmanager
def budget(seconds):
    previous=BUDGET.get();until=time.monotonic()+seconds
    token=BUDGET.set(min(previous,until) if previous is not None else until)
    try:yield
    finally:BUDGET.reset(token)

def cli(args,*,data=None,stop=None,timeout=20,limit=65536):
    if stop is not None and stop.is_set():raise NotStarted('Docker operation cancelled')
    if BUDGET.get() is not None:timeout=min(timeout,BUDGET.get()-time.monotonic())
    if timeout<=0:raise NotStarted('Docker operation budget exhausted')
    options={'creationflags':subprocess.CREATE_NO_WINDOW} if os.name=='nt' else {}
    if LOCAL_HOST is None and args[:2]!=['context','inspect']:raise NotStarted('Local Docker endpoint not selected')
    command=['docker',*(['--host',LOCAL_HOST] if LOCAL_HOST is not None else []),*args]
    try:child=subprocess.Popen(command,stdin=subprocess.PIPE if data is not None else subprocess.DEVNULL,stdout=subprocess.PIPE,stderr=subprocess.DEVNULL,env=docker_env(),**options)
    except OSError:raise NotStarted('Docker client unavailable') from None
    completed=queue.Queue(1)
    def communicate():
        try:output,_=child.communicate(data);completed.put((output,None))
        except Exception as error:completed.put((None,error))
    thread=threading.Thread(target=communicate,daemon=True);thread.start();until=time.monotonic()+timeout
    try:
        while True:
            if (stop is not None and stop.is_set()) or time.monotonic()>=until:raise OSError('Docker operation unavailable')
            try:output,error=completed.get(timeout=min(.2,max(.01,until-time.monotonic())))
            except queue.Empty:continue
            if error is not None or child.returncode:raise OSError('Docker operation unavailable')
            require(len(output)<=limit);return output
    finally:
        if child.poll() is None:child.kill()
        child.wait(timeout=10);thread.join(timeout=10)
        if thread.is_alive():raise OSError('Docker client cleanup unavailable')

def image():return APPROVED_IMAGE

def preflight():
    global LOCAL_HOST
    base=json.loads((ROOT/'ai/shona-cpu.lock.json').read_bytes())
    require(hashlib.sha256(RUNTIME_BYTES).hexdigest()==base['runtimeLockSha256'] and hashlib.sha256(MODEL_BYTES).hexdigest()==base['modelLockSha256'])
    require((ROOT/'ai/shona-cpu-runtime.lock.json').read_bytes()==RUNTIME_BYTES and (ROOT/'ai/shona-cpu-models.lock.json').read_bytes()==MODEL_BYTES)
    require(os.name=='nt') # Existing locked-model host is Windows; no implicit portable runtime.
    endpoint=json.loads(cli(['context','inspect','--format','{{json .Endpoints.docker.Host}}']))
    require(isinstance(endpoint,str) and re.fullmatch(r'npipe:////\./pipe/[A-Za-z0-9_.-]{1,100}',endpoint) is not None)
    LOCAL_HOST=endpoint
    require(cli(['image','inspect',image(),'--format','{{.Id}}']).decode().strip()==image())

class OwnerLock:
    def __init__(self,root,owner):
        require(HEX.fullmatch(owner) is not None);self.root=Path(root).resolve();self.owner=owner;self.stream=None
    def acquire(self):
        path=self.root/(self.owner+'.lock');require(not path.is_symlink());self.stream=path.open('a+b')
        try:
            self.stream.seek(0,2)
            if self.stream.tell()==0:self.stream.write(b'0');self.stream.flush()
            self.stream.seek(0)
            if os.name=='nt':
                import msvcrt
                msvcrt.locking(self.stream.fileno(),msvcrt.LK_NBLCK,1)
            else:
                import fcntl
                fcntl.flock(self.stream,fcntl.LOCK_EX|fcntl.LOCK_NB)
            return True
        except OSError:self.stream.close();self.stream=None;return False
    def close(self):
        if self.stream:
            self.stream.seek(0)
            if os.name=='nt':
                import msvcrt
                msvcrt.locking(self.stream.fileno(),msvcrt.LK_UNLCK,1)
            else:
                import fcntl
                fcntl.flock(self.stream,fcntl.LOCK_UN)
            self.stream.close();self.stream=None

def validate_record(record):
    require(isinstance(record,dict) and set(record)=={'schemaVersion','owner','resource','name','image','history'})
    require(type(record['schemaVersion']) is int and record['schemaVersion']==1 and HEX.fullmatch(record['owner']) and HEX.fullmatch(record['resource']))
    require(record['name']=='dzoka-owned-'+record['owner']+'-'+record['resource'] and record['image']==image())
    require(isinstance(record['history'],list) and 1<=len(record['history'])<=64)
    for event in record['history']:require(isinstance(event,dict) and set(event)=={'state','at'} and event['state'] in {'INTENT','CREATED','STARTED','CREATE_UNKNOWN','NOT_CREATED','CLEANUP_UNAVAILABLE','REMOVED'} and isinstance(event['at'],str))
    return record

def persist(root,record,state):
    record={**record,'history':[*record.get('history',[]),{'state':state,'at':datetime.now(timezone.utc).isoformat()}]};validate_record(record)
    root=Path(root).resolve();path=root/(record['name']+'.json');require(not path.is_symlink())
    temporary=root/(record['name']+'.'+uuid.uuid4().hex+'.pending')
    try:
        with temporary.open('xb') as output:output.write((json.dumps(record,sort_keys=True)+'\n').encode());output.flush();os.fsync(output.fileno())
        temporary.replace(path)
    finally:temporary.unlink(missing_ok=True)
    return record

def matches(record,container):
    labels=container.get('Config',{}).get('Labels') or {}
    return (container.get('Name')=='/'+record['name'] and container.get('Image')==record['image'] and container.get('Config',{}).get('Image')==record['image'] and labels.get(MANAGED)=='v1' and labels.get(OWNER)==record['owner'] and labels.get(RESOURCE)==record['resource'] and ID.fullmatch(container.get('Id','')) is not None)

def lookup(record):
    ids=cli(['container','ls','-a','--no-trunc','--filter','name=^/'+record['name']+'$','--format','{{.ID}}']).decode().splitlines()
    require(len(ids)<=1 and all(ID.fullmatch(i) for i in ids))
    if not ids:return None
    containers=json.loads(cli(['container','inspect',ids[0]]));require(isinstance(containers,list) and len(containers)==1)
    require(matches(record,containers[0]));return containers[0]

def cleanup(root,record):
    with budget(30):return _cleanup(root,record)

def _cleanup(root,record):
    validate_record(record)
    if record['history'][-1]['state']=='REMOVED':return record,True
    container=lookup(record)
    if container:
        cli(['container','rm','-f',container['Id']]);require(lookup(record) is None)
        return persist(root,record,'REMOVED'),True
    # An unconfirmed create can arrive later. Absence now is not completion evidence.
    confirmed=any(e['state'] in ('CREATED','STARTED','NOT_CREATED') for e in record['history'])
    return (persist(root,record,'REMOVED'),True) if confirmed else (record,False)

def reconcile(root):
    with budget(30):return _reconcile(root)

def _reconcile(root):
    root=Path(root).resolve();require(root.is_dir());paths=list(root.glob('dzoka-owned-*.json'));require(len(paths)<=10000)
    result={'removed':0,'active':0,'unresolved':0}
    for path in paths:
        require(not path.is_symlink() and path.stat().st_size<=32768)
        record=validate_record(json.loads(path.read_bytes()));require(path.name==record['name']+'.json')
        if record['history'][-1]['state']=='REMOVED':continue
        lock=OwnerLock(root,record['owner'])
        if not lock.acquire():result['active']+=1;continue
        try:
            try:_,complete=cleanup(root,record)
            except (OSError,ValueError,subprocess.SubprocessError):complete=False
            result['removed' if complete else 'unresolved']+=1
        finally:lock.close()
    return result

class Session:
    def __init__(self,models,state,owner,stop):
        self.models=Path(models).resolve();self.state=Path(state).resolve();require(self.models.is_dir() and self.state.is_dir() and self.models!=self.state)
        self.owner=owner;self.stop=stop;self.stack=ExitStack();self.record=None;self.used=False;self.prepared_at=None;self.preparation_timer=None
    def prepare(self,group):
        require(not self.used and group in ('CT2_EN_SN','CT2_SN_EN'));self.used=True
        from generative_model import locked_model
        entries=[e for e in APPROVED_FILES if e['group']==group and e['role'] in ('model.bin','config.json','shared_vocabulary.json')]
        require(len(entries)==3)
        files={e['role']:self.stack.enter_context(locked_model(self.models/e['local'],e)) for e in entries}
        resource=uuid.uuid4().hex;record={'schemaVersion':1,'owner':self.owner,'resource':resource,'name':'dzoka-owned-'+self.owner+'-'+resource,'image':image(),'history':[]}
        self.record=persist(self.state,record,'INTENT')
        args=['create','--pull','never','--name',record['name'],'--label',MANAGED+'=v1','--label',OWNER+'='+self.owner,'--label',RESOURCE+'='+resource,'--log-driver','none','--ulimit','core=0:0','--network','none','--read-only','--cap-drop','ALL','--security-opt','no-new-privileges','--pids-limit','64','--memory','1536m','--cpus','2','--tmpfs','/tmp:rw,noexec,nosuid,size=16m','-i']
        for role,path in files.items():
            require(not any(c in path for c in (',','\r','\n')));args+=['--mount','type=bind,source='+path+',target=/model/'+role+',readonly']
        try:
            identifier=cli([*args,image()],stop=self.stop).decode().strip();require(ID.fullmatch(identifier) is not None)
            self.record=persist(self.state,self.record,'CREATED');container=lookup(self.record);require(container['Id']==identifier)
            config=container['HostConfig'];require(container['Config']['User']=='65532:65532' and config['NetworkMode']=='none' and config['ReadonlyRootfs'] and not config['Privileged'] and config['LogConfig']['Type']=='none' and config['Memory']==1536*1024*1024 and config['NanoCpus']==2000000000 and config['PidsLimit']==64 and 'ALL' in config['CapDrop'] and 'no-new-privileges' in config['SecurityOpt'])
            require(any(u['Name']=='core' and u['Soft']==0 and u['Hard']==0 for u in config['Ulimits']))
            require(len(container['Mounts'])==3 and {m['Destination'] for m in container['Mounts']}=={'/model/model.bin','/model/config.json','/model/shared_vocabulary.json'} and all(not m['RW'] and m['Type']=='bind' for m in container['Mounts']))
            self.prepared_at=time.monotonic()
            import threading
            self.preparation_timer=threading.Timer(10,self.stop.set);self.preparation_timer.daemon=True;self.preparation_timer.start()
        except NotStarted:
            self.record=persist(self.state,self.record,'NOT_CREATED');raise
        except Exception:
            self.record=persist(self.state,self.record,'CREATE_UNKNOWN');raise
    def native(self,pieces):
        import struct
        require(self.record is not None and self.record['history'][-1]['state']=='CREATED' and self.prepared_at is not None and time.monotonic()-self.prepared_at<=10)
        if self.preparation_timer:self.preparation_timer.cancel()
        require(isinstance(pieces,list) and 2<=len(pieces)<=128 and pieces[-1]=='</s>')
        raw=struct.pack('<I',len(pieces))
        for piece in pieces:
            require(isinstance(piece,str) and '\0' not in piece);data=piece.encode();require(1<=len(data)<=1024);raw+=struct.pack('<I',len(data))+data
        self.record=persist(self.state,self.record,'STARTED')
        try:
            output=cli(['start','--attach','--interactive',self.record['name']],data=raw,stop=self.stop,timeout=120,limit=33000)
            from shona_cpu import decode_tokens
            return decode_tokens(output)
        finally:
            if not self.close():raise OSError('Docker cleanup unconfirmed')
    def close(self):
        if self.preparation_timer:self.preparation_timer.cancel()
        complete=True
        try:
            if self.record is not None:
                try:self.record,complete=cleanup(self.state,self.record)
                except (OSError,ValueError,subprocess.SubprocessError):
                    complete=False
                    if len(self.record['history'])<64:self.record=persist(self.state,self.record,'CLEANUP_UNAVAILABLE')
        finally:self.stack.close()
        return complete
