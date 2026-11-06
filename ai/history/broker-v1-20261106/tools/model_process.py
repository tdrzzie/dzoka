"""Capability-free model process, atomic job ownership and bounded duplex pipes."""
import ctypes
from ctypes import wintypes as w
import os
from pathlib import Path
import queue
import subprocess
import sys
import threading
import time
from appcontainer_boundary import require_no_loopback_exemption,sid_string
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'ai'))
from model_wire import conversation

class ModelJob:
    def __init__(self,k):
        size=ctypes.c_size_t
        class Basic(ctypes.Structure):_fields_=[('pt',ctypes.c_int64),('jt',ctypes.c_int64),('flags',w.DWORD),('min',size),('max',size),('active',w.DWORD),('affinity',size),('priority',w.DWORD),('scheduling',w.DWORD)]
        class Io(ctypes.Structure):_fields_=[(n,ctypes.c_uint64) for n in ('ro','wo','oo','rb','wb','ob')]
        class Limits(ctypes.Structure):_fields_=[('basic',Basic),('io',Io),('pm',size),('jm',size),('pp',size),('pj',size)]
        self.k=k;k.CreateJobObjectW.argtypes=[ctypes.c_void_p,w.LPCWSTR];k.CreateJobObjectW.restype=w.HANDLE
        k.SetInformationJobObject.argtypes=[w.HANDLE,ctypes.c_int,ctypes.c_void_p,w.DWORD];k.SetInformationJobObject.restype=w.BOOL
        self.handle=k.CreateJobObjectW(None,None)
        if not self.handle:raise OSError('Model ownership unavailable')
        limits=Limits();limits.basic.flags=0x2000|0x8|0x200;limits.basic.active=8;limits.jm=8*1024**3
        if not k.SetInformationJobObject(self.handle,9,ctypes.byref(limits),ctypes.sizeof(limits)):
            self.close();raise OSError('Model limits unavailable')
    def close(self):
        if self.handle:self.k.CloseHandle(self.handle);self.handle=None

def exchange(profile,bundle,request,native,timeout=260,started=lambda _:None):
    k,u,a=profile.k,profile.u,profile.a;require_no_loopback_exemption(profile)
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
    k.GetExitCodeProcess.argtypes=[w.HANDLE,ctypes.POINTER(w.DWORD)];k.GetExitCodeProcess.restype=w.BOOL
    k.IsProcessInJob.argtypes=[w.HANDLE,w.HANDLE,ctypes.POINTER(w.BOOL)];k.IsProcessInJob.restype=w.BOOL
    size=ctypes.c_size_t();k.InitializeProcThreadAttributeList(None,3,0,ctypes.byref(size));attributes=ctypes.create_string_buffer(size.value)
    if not k.InitializeProcThreadAttributeList(attributes,3,0,ctypes.byref(size)):raise OSError('Model attributes unavailable')
    job=ModelJob(k);process=Process();stop=threading.Event();finished=queue.Queue(1);thread=None
    inbound_r,inbound_w=os.pipe();outbound_r,outbound_w=os.pipe();null=os.open(os.devnull,os.O_WRONLY)
    import msvcrt
    handles=(w.HANDLE*3)(*(msvcrt.get_osfhandle(fd) for fd in (inbound_r,outbound_w,null)))
    for handle in handles:os.set_handle_inheritable(handle,True)
    parent_in=os.fdopen(inbound_w,'wb',buffering=0);parent_out=os.fdopen(outbound_r,'rb',buffering=0)
    child_fds=[inbound_r,outbound_w,null]
    try:
        caps=Capabilities(profile.sid,None,0,0);jobs=(w.HANDLE*1)(job.handle)
        for key,value in ((0x20009,caps),(0x20002,handles),(0x2000d,jobs)):
            if not k.UpdateProcThreadAttribute(attributes,0,key,ctypes.byref(value),ctypes.sizeof(value),None,None):raise OSError('Model attribute rejected')
        startup=Extended();startup.startup.cb=ctypes.sizeof(startup);startup.startup.flags=0x100;startup.startup.stdin=handles[0];startup.startup.stdout=handles[1];startup.startup.stderr=handles[2];startup.attributes=ctypes.addressof(attributes)
        executable=Path(bundle)/'runtime/python.exe';command=ctypes.create_unicode_buffer(subprocess.list2cmdline([str(executable),'-I','-S','-B',str(Path(bundle)/'ai/offline_model.py')]))
        safe={key:os.environ[key] for key in ('SYSTEMROOT','WINDIR','LOCALAPPDATA','TEMP','TMP')}
        environment=ctypes.create_unicode_buffer('\0'.join(key+'='+value for key,value in sorted(safe.items()))+'\0\0')
        if not k.CreateProcessW(str(executable),command,None,None,True,0x80000|0x400|4|0x8000000,environment,str(bundle),ctypes.byref(startup),ctypes.byref(process)):raise OSError('Isolated model launch unavailable')
        for fd in child_fds:os.close(fd)
        child_fds=[]
        member=w.BOOL()
        if not k.IsProcessInJob(process.process,job.handle,ctypes.byref(member)) or not member.value:raise OSError('Model job ownership failed')
        token=w.HANDLE()
        if not a.OpenProcessToken(process.process,8,ctypes.byref(token)):raise OSError('Model token unavailable')
        def information(kind):
            size=w.DWORD();a.GetTokenInformation(token,kind,None,0,ctypes.byref(size));data=ctypes.create_string_buffer(size.value)
            if not a.GetTokenInformation(token,kind,data,size,ctypes.byref(size)):raise OSError('Model token query failed')
            return data
        try:
            if w.DWORD.from_buffer(information(29)).value!=1 or w.DWORD.from_buffer(information(30)).value!=0:raise OSError('Model token is not isolated')
            package=information(31)
            if sid_string(a,k,ctypes.c_void_p.from_buffer(package).value)!=profile.sid_text:raise OSError('Model package differs')
        finally:k.CloseHandle(token)
        if k.ResumeThread(process.thread)==0xffffffff:raise OSError('Model resume failed')
        started(process.pid)
        def transport():
            try:
                value=conversation(parent_out,parent_in,request,lambda pieces:native(pieces,stop))
                finished.put((value,None))
            except Exception as error:finished.put((None,error))
        thread=threading.Thread(target=transport,daemon=True);thread.start();deadline=time.monotonic()+timeout
        while time.monotonic()<deadline:
            try:value,error=finished.get(timeout=.2)
            except queue.Empty:continue
            if error is not None:raise ValueError('Isolated model protocol unavailable') from None
            if k.WaitForSingleObject(process.process,10000)!=0:raise OSError('Model failed to exit')
            code=w.DWORD()
            if not k.GetExitCodeProcess(process.process,ctypes.byref(code)) or code.value:raise OSError('Isolated inference failed')
            return value
        raise OSError('Isolated model deadline exceeded')
    finally:
        stop.set();job.close()
        if process.process:
            stopped=k.WaitForSingleObject(process.process,10000);k.CloseHandle(process.process)
            if stopped!=0:raise OSError('Model cleanup unconfirmed')
        if process.thread:k.CloseHandle(process.thread)
        for fd in child_fds:os.close(fd)
        if thread:thread.join(timeout=40)
        parent_in.close();parent_out.close();k.DeleteProcThreadAttributeList(attributes)
        if thread and thread.is_alive():raise OSError('Model transport cleanup unconfirmed')
