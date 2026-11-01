"""Owned process trees. Bootstrap must wait for the go byte before spawning work."""
import ctypes
from ctypes import wintypes
import os
import signal
import subprocess

class WindowsJob:
    def __init__(self,process):
        size=ctypes.c_size_t;large=ctypes.c_int64;dword=wintypes.DWORD
        class Basic(ctypes.Structure):
            _fields_=[('process_time',large),('job_time',large),('flags',dword),('min_ws',size),('max_ws',size),('active',dword),('affinity',size),('priority',dword),('scheduling',dword)]
        class IO(ctypes.Structure):
            _fields_=[(n,ctypes.c_uint64) for n in ('read_ops','write_ops','other_ops','read_bytes','write_bytes','other_bytes')]
        class Extended(ctypes.Structure):
            _fields_=[('basic',Basic),('io',IO),('process_memory',size),('job_memory',size),('peak_process',size),('peak_job',size)]
        self.kernel=ctypes.WinDLL('kernel32',use_last_error=True)
        self.kernel.CreateJobObjectW.argtypes=[ctypes.c_void_p,wintypes.LPCWSTR];self.kernel.CreateJobObjectW.restype=wintypes.HANDLE
        self.kernel.SetInformationJobObject.argtypes=[wintypes.HANDLE,ctypes.c_int,ctypes.c_void_p,dword];self.kernel.SetInformationJobObject.restype=wintypes.BOOL
        self.kernel.OpenProcess.argtypes=[dword,wintypes.BOOL,dword];self.kernel.OpenProcess.restype=wintypes.HANDLE
        self.kernel.AssignProcessToJobObject.argtypes=[wintypes.HANDLE,wintypes.HANDLE];self.kernel.AssignProcessToJobObject.restype=wintypes.BOOL
        self.kernel.CloseHandle.argtypes=[wintypes.HANDLE];self.kernel.CloseHandle.restype=wintypes.BOOL
        self.handle=self.kernel.CreateJobObjectW(None,None)
        if not self.handle:raise OSError('Owned worker job unavailable')
        handle=None
        try:
            limits=Extended();limits.basic.flags=0x2000 # JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE
            if not self.kernel.SetInformationJobObject(self.handle,9,ctypes.byref(limits),ctypes.sizeof(limits)):raise OSError('Worker limits unavailable')
            handle=self.kernel.OpenProcess(0x100|0x1,False,process.pid) # SET_QUOTA | TERMINATE
            if not handle or not self.kernel.AssignProcessToJobObject(self.handle,handle):raise OSError('Worker ownership unavailable')
        except Exception:self.close();raise
        finally:
            if handle:self.kernel.CloseHandle(handle)
    def close(self):
        if self.handle:
            self.kernel.CloseHandle(self.handle);self.handle=None

def run_owned(command,env,cwd,timeout):
    """No inherited stdin/output; caller supplies a bootstrap awaiting one go byte."""
    options={'creationflags':subprocess.CREATE_NO_WINDOW} if os.name=='nt' else {'start_new_session':True}
    process=subprocess.Popen(command,cwd=cwd,env=env,stdin=subprocess.PIPE,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL,close_fds=True,**options)
    job=None;owned=False
    try:
        if os.name=='nt':job=WindowsJob(process)
        owned=True
        process.stdin.write(b'1');process.stdin.close()
        try:return process.wait(timeout=timeout)
        except subprocess.TimeoutExpired:return 124
    finally:
        if process.stdin and not process.stdin.closed:process.stdin.close()
        if job:job.close()
        elif os.name!='nt' and owned:
            try:os.killpg(process.pid,signal.SIGKILL)
            except ProcessLookupError:pass
        if process.poll() is None:process.kill()
        process.wait(timeout=10)
