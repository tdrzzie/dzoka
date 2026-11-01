"""Independent no-capability Windows process launch for the offline pilot only."""
import ctypes
from ctypes import wintypes as w
import hashlib
import json
import os
from pathlib import Path
import subprocess
import tempfile
import uuid
from docker_lifecycle import OwnerLock
from service_account import identity

PREFIX='Dzoka.Offline.'

def apis():
    if os.name!='nt':raise OSError('Windows AppContainer required')
    k=ctypes.WinDLL('kernel32',use_last_error=True);u=ctypes.WinDLL('userenv',use_last_error=True);a=ctypes.WinDLL('advapi32',use_last_error=True)
    u.CreateAppContainerProfile.argtypes=[w.LPCWSTR,w.LPCWSTR,w.LPCWSTR,ctypes.c_void_p,w.DWORD,ctypes.POINTER(ctypes.c_void_p)];u.CreateAppContainerProfile.restype=ctypes.c_long
    u.DeriveAppContainerSidFromAppContainerName.argtypes=[w.LPCWSTR,ctypes.POINTER(ctypes.c_void_p)];u.DeriveAppContainerSidFromAppContainerName.restype=ctypes.c_long
    u.DeleteAppContainerProfile.argtypes=[w.LPCWSTR];u.DeleteAppContainerProfile.restype=ctypes.c_long
    a.FreeSid.argtypes=[ctypes.c_void_p];a.FreeSid.restype=ctypes.c_void_p
    a.ConvertSidToStringSidW.argtypes=[ctypes.c_void_p,ctypes.POINTER(w.LPWSTR)];a.ConvertSidToStringSidW.restype=w.BOOL
    a.OpenProcessToken.argtypes=[w.HANDLE,w.DWORD,ctypes.POINTER(w.HANDLE)];a.OpenProcessToken.restype=w.BOOL
    a.GetTokenInformation.argtypes=[w.HANDLE,ctypes.c_int,ctypes.c_void_p,w.DWORD,ctypes.POINTER(w.DWORD)];a.GetTokenInformation.restype=w.BOOL
    k.LocalFree.argtypes=[ctypes.c_void_p];k.LocalFree.restype=ctypes.c_void_p
    k.CloseHandle.argtypes=[w.HANDLE];k.CloseHandle.restype=w.BOOL
    return k,u,a

def sid_string(a,k,sid):
    text=w.LPWSTR()
    if not a.ConvertSidToStringSidW(sid,ctypes.byref(text)):raise OSError('Package SID unavailable')
    try:return text.value
    finally:k.LocalFree(ctypes.cast(text,ctypes.c_void_p))

def state_write(path,value):
    temporary=path.with_suffix('.pending')
    with temporary.open('wb') as output:output.write((json.dumps(value,sort_keys=True)+'\n').encode());output.flush();os.fsync(output.fileno())
    os.replace(temporary,path)

def require_no_loopback_exemption(profile):
    class Entry(ctypes.Structure):_fields_=[('sid',ctypes.c_void_p),('attributes',w.DWORD)]
    api=ctypes.WinDLL('FirewallAPI.dll',use_last_error=True);k=profile.k;a=profile.a
    query=api.NetworkIsolationGetAppContainerConfig;query.argtypes=[ctypes.POINTER(w.DWORD),ctypes.POINTER(ctypes.POINTER(Entry))];query.restype=w.DWORD
    a.EqualSid.argtypes=[ctypes.c_void_p,ctypes.c_void_p];a.EqualSid.restype=w.BOOL
    k.GetProcessHeap.restype=w.HANDLE;k.HeapFree.argtypes=[w.HANDLE,w.DWORD,ctypes.c_void_p];k.HeapFree.restype=w.BOOL
    count=w.DWORD();entries=ctypes.POINTER(Entry)()
    if query(ctypes.byref(count),ctypes.byref(entries))!=0:raise OSError('Loopback policy unavailable')
    try:
        if count.value>10000:raise ValueError('Loopback policy exceeds review bound')
        if any(a.EqualSid(entries[index].sid,profile.sid) for index in range(count.value)):raise OSError('Offline package has loopback exemption')
    finally:
        for index in range(count.value):k.HeapFree(k.GetProcessHeap(),0,entries[index].sid)
        if entries:k.HeapFree(k.GetProcessHeap(),0,entries)

def recover(state):
    k,u,a=apis();state=Path(state);result={'removed':0,'active':0,'unresolved':0}
    account=identity()['sid']
    paths=list(state.glob('offline-*.json'))
    if len(paths)>1000:raise ValueError('Pilot profile state capacity')
    for path in paths:
        if path.is_symlink() or getattr(path.lstat(),'st_file_attributes',0)&0x400 or path.stat().st_size>4096:raise ValueError('Invalid pilot state')
        from worker_service import read_json
        value=read_json(path,4096);owner=value.get('owner')
        if not isinstance(owner,str) or len(owner)!=32 or any(c not in '0123456789abcdef' for c in owner) or set(value)!={'schemaVersion','accountSid','owner','name','state'} or type(value['schemaVersion']) is not int or value['schemaVersion']!=1 or value['accountSid']!=account or value['name']!=PREFIX+owner or path.name!='offline-'+owner+'.json' or value['state'] not in ('INTENT','CREATED','REMOVED'):raise ValueError('Invalid pilot ownership')
        if value['state']=='REMOVED':continue
        lock=OwnerLock(state,owner)
        if not lock.acquire():result['active']+=1;continue
        try:
            if u.DeleteAppContainerProfile(value['name'])!=0:raise OSError('Profile removal unresolved')
            # Interrupted creation has no confirmed outcome. Do not certify an
            # absent profile if creation might still complete after owner loss.
            if value['state']=='INTENT':result['unresolved']+=1;continue
            state_write(path,{**value,'state':'REMOVED'});result['removed']+=1
        finally:lock.close()
    return result

class Profile:
    def __init__(self,state):
        self.state=Path(state);self.owner=uuid.uuid4().hex;self.name=PREFIX+self.owner;self.lock=OwnerLock(self.state,self.owner);self.sid=None
        self.k,self.u,self.a=apis();self.path=self.state/('offline-'+self.owner+'.json')
        self.account=identity()['sid']
    def value(self,state):return {'schemaVersion':1,'accountSid':self.account,'owner':self.owner,'name':self.name,'state':state}
    def __enter__(self):
        if not self.lock.acquire():raise OSError('Profile ownership unavailable')
        state_write(self.path,self.value('INTENT'))
        sid=ctypes.c_void_p()
        if self.u.CreateAppContainerProfile(self.name,self.name,'Dzoka offline capability pilot',None,0,ctypes.byref(sid))!=0:
            self.lock.close();raise OSError('AppContainer creation unavailable')
        self.sid=sid;self.sid_text=sid_string(self.a,self.k,sid)
        state_write(self.path,self.value('CREATED'));return self
    def __exit__(self,*_):
        try:
            if self.u.DeleteAppContainerProfile(self.name)!=0:raise OSError('Profile cleanup unavailable')
            state_write(self.path,self.value('REMOVED'))
        finally:
            if self.sid:self.a.FreeSid(self.sid)
            self.lock.close()

class KillJob:
    def __init__(self,k):
        size=ctypes.c_size_t
        class Basic(ctypes.Structure):_fields_=[('pt',ctypes.c_int64),('jt',ctypes.c_int64),('flags',w.DWORD),('min',size),('max',size),('active',w.DWORD),('affinity',size),('priority',w.DWORD),('scheduling',w.DWORD)]
        class Io(ctypes.Structure):_fields_=[(name,ctypes.c_uint64) for name in ('ro','wo','oo','rb','wb','ob')]
        class Limits(ctypes.Structure):_fields_=[('basic',Basic),('io',Io),('pm',size),('jm',size),('pp',size),('pj',size)]
        self.k=k;k.CreateJobObjectW.argtypes=[ctypes.c_void_p,w.LPCWSTR];k.CreateJobObjectW.restype=w.HANDLE
        k.SetInformationJobObject.argtypes=[w.HANDLE,ctypes.c_int,ctypes.c_void_p,w.DWORD];k.SetInformationJobObject.restype=w.BOOL
        self.handle=k.CreateJobObjectW(None,None)
        if not self.handle:raise OSError('Offline job unavailable')
        limits=Limits();limits.basic.flags=0x2000|0x8|0x200;limits.basic.active=8;limits.jm=512*1024**2
        if not k.SetInformationJobObject(self.handle,9,ctypes.byref(limits),ctypes.sizeof(limits)):
            self.close();raise OSError('Offline limits unavailable')
    def close(self):
        if self.handle:self.k.CloseHandle(self.handle);self.handle=None

def launch(profile,runtime,request,timeout=20,started=lambda _:None):
    """Fixed probe, zero capabilities, three standard handles only, suspended ownership."""
    runtime=Path(runtime);k,u,a=profile.k,profile.u,profile.a
    require_no_loopback_exemption(profile)
    class Startup(ctypes.Structure):
        _fields_=[('cb',w.DWORD),('reserved',w.LPWSTR),('desktop',w.LPWSTR),('title',w.LPWSTR),('x',w.DWORD),('y',w.DWORD),('xs',w.DWORD),('ys',w.DWORD),('xc',w.DWORD),('yc',w.DWORD),('fill',w.DWORD),('flags',w.DWORD),('show',w.WORD),('reserved2',w.WORD),('bytes2',ctypes.c_void_p),('stdin',w.HANDLE),('stdout',w.HANDLE),('stderr',w.HANDLE)]
    class Extended(ctypes.Structure):_fields_=[('startup',Startup),('attributes',ctypes.c_void_p)]
    class Process(ctypes.Structure):_fields_=[('process',w.HANDLE),('thread',w.HANDLE),('pid',w.DWORD),('tid',w.DWORD)]
    class Capabilities(ctypes.Structure):_fields_=[('sid',ctypes.c_void_p),('capabilities',ctypes.c_void_p),('count',w.DWORD),('reserved',w.DWORD)]
    k.InitializeProcThreadAttributeList.argtypes=[ctypes.c_void_p,w.DWORD,w.DWORD,ctypes.POINTER(ctypes.c_size_t)];k.InitializeProcThreadAttributeList.restype=w.BOOL
    k.UpdateProcThreadAttribute.argtypes=[ctypes.c_void_p,w.DWORD,ctypes.c_size_t,ctypes.c_void_p,ctypes.c_size_t,ctypes.c_void_p,ctypes.c_void_p];k.UpdateProcThreadAttribute.restype=w.BOOL
    k.DeleteProcThreadAttributeList.argtypes=[ctypes.c_void_p]
    k.CreateProcessW.argtypes=[w.LPCWSTR,w.LPWSTR,ctypes.c_void_p,ctypes.c_void_p,w.BOOL,w.DWORD,ctypes.c_void_p,w.LPCWSTR,ctypes.POINTER(Extended),ctypes.POINTER(Process)];k.CreateProcessW.restype=w.BOOL
    k.ResumeThread.argtypes=[w.HANDLE];k.ResumeThread.restype=w.DWORD
    k.WaitForSingleObject.argtypes=[w.HANDLE,w.DWORD];k.WaitForSingleObject.restype=w.DWORD
    k.TerminateProcess.argtypes=[w.HANDLE,w.UINT];k.GetExitCodeProcess.argtypes=[w.HANDLE,ctypes.POINTER(w.DWORD)]
    raw=json.dumps(request).encode()
    if len(raw)>16384:raise ValueError('Offline request too large')
    size=ctypes.c_size_t();k.InitializeProcThreadAttributeList(None,3,0,ctypes.byref(size));attributes=ctypes.create_string_buffer(size.value)
    if not k.InitializeProcThreadAttributeList(attributes,3,0,ctypes.byref(size)):raise OSError('Process attributes unavailable')
    import msvcrt
    process=Process();job=KillJob(k)
    with tempfile.TemporaryFile() as incoming,tempfile.TemporaryFile() as outgoing:
        incoming.write(raw);incoming.seek(0)
        handles=(w.HANDLE*2)(msvcrt.get_osfhandle(incoming.fileno()),msvcrt.get_osfhandle(outgoing.fileno()))
        for handle in handles:os.set_handle_inheritable(handle,True)
        capabilities=Capabilities(profile.sid,None,0,0)
        jobs=(w.HANDLE*1)(job.handle)
        try:
            if not k.UpdateProcThreadAttribute(attributes,0,0x20009,ctypes.byref(capabilities),ctypes.sizeof(capabilities),None,None) or not k.UpdateProcThreadAttribute(attributes,0,0x20002,handles,ctypes.sizeof(handles),None,None) or not k.UpdateProcThreadAttribute(attributes,0,0x2000d,jobs,ctypes.sizeof(jobs),None,None):raise OSError('Offline attributes unavailable')
            startup=Extended();startup.startup.cb=ctypes.sizeof(startup);startup.startup.flags=0x100;startup.startup.stdin=handles[0];startup.startup.stdout=handles[1];startup.startup.stderr=handles[1];startup.attributes=ctypes.addressof(attributes)
            executable=runtime/'python.exe';command=ctypes.create_unicode_buffer(subprocess.list2cmdline([str(executable),'-I','-S',str(runtime/'offline_probe.py')]))
            safe={key:os.environ[key] for key in ('SYSTEMROOT','WINDIR','LOCALAPPDATA','TEMP','TMP')}
            environment=ctypes.create_unicode_buffer('\0'.join(key+'='+value for key,value in sorted(safe.items()))+'\0\0')
            if not k.CreateProcessW(str(executable),command,None,None,True,0x80000|0x400|0x4|0x8000000,environment,str(runtime),ctypes.byref(startup),ctypes.byref(process)):raise OSError('Offline process creation unavailable: '+str(ctypes.get_last_error()))
            k.IsProcessInJob.argtypes=[w.HANDLE,w.HANDLE,ctypes.POINTER(w.BOOL)];k.IsProcessInJob.restype=w.BOOL;member=w.BOOL()
            if not k.IsProcessInJob(process.process,job.handle,ctypes.byref(member)) or not member.value:raise OSError('Offline process ownership unavailable')
            token=w.HANDLE()
            if not a.OpenProcessToken(process.process,8,ctypes.byref(token)):raise OSError('Offline token unavailable')
            def info(kind):
                size=w.DWORD();a.GetTokenInformation(token,kind,None,0,ctypes.byref(size));data=ctypes.create_string_buffer(size.value)
                if not a.GetTokenInformation(token,kind,data,size,ctypes.byref(size)):raise OSError('Offline token query failed')
                return data
            try:
                if w.DWORD.from_buffer(info(29)).value!=1 or w.DWORD.from_buffer(info(30)).value!=0:raise OSError('Offline token is not capability-free')
                package=info(31)
                if sid_string(a,k,ctypes.c_void_p.from_buffer(package).value)!=profile.sid_text:raise OSError('Offline package differs')
            finally:k.CloseHandle(token)
            if k.ResumeThread(process.thread)==0xffffffff:raise OSError('Offline resume failed')
            started(process.pid)
            if k.WaitForSingleObject(process.process,int(timeout*1000))!=0:raise OSError('Offline deadline exceeded')
            exit_code=w.DWORD();k.GetExitCodeProcess(process.process,ctypes.byref(exit_code));outgoing.seek(0);output=outgoing.read(32769)
            if exit_code.value or len(output)>32768:raise OSError('Offline probe failed: '+str(exit_code.value)+' '+output[:400].decode(errors='replace'))
            return json.loads(output)
        finally:
            if job:job.close()
            elif process.process:k.TerminateProcess(process.process,1)
            if process.process:
                stopped=k.WaitForSingleObject(process.process,10000);k.CloseHandle(process.process)
                if stopped!=0:raise OSError('Offline process cleanup unconfirmed')
            if process.thread:k.CloseHandle(process.thread)
            for handle in handles:os.set_handle_inheritable(handle,False)
            k.DeleteProcThreadAttributeList(attributes)

if __name__=='__main__':
    import argparse
    from worker_service import verify_release
    from service_host import plain,check_acl
    parser=argparse.ArgumentParser();parser.add_argument('--recover-state',required=True);args=parser.parse_args()
    try:
        verify_release();state=plain(args.recover_state);check_acl(state)
        for parent in state.parents:check_acl(parent,False,True)
        result=recover(state);print(json.dumps(result));raise SystemExit(1 if result['unresolved'] else 0)
    except Exception:print('Offline profile recovery unavailable; retain protected state');raise SystemExit(1)
