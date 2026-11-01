"""Windows resource boundary and conservative NTFS deployment checks."""
import ctypes
from ctypes import wintypes as w
import json
import os
from pathlib import Path
import re
import subprocess

ROOT=Path(__file__).resolve().parents[1]
TRUSTED={'S-1-5-18','S-1-5-32-544'}
INSTALLER='S-1-5-80-956008885-3418522649-1831038044-1853292631-2271478464'

def plain(path):
    path=Path(path)
    if not path.is_absolute() or not re.fullmatch(r'[A-Za-z]:',path.drive):raise ValueError('Local absolute deployment path required')
    for part in (path,*path.parents):
        if part.lstat().st_file_attributes & 0x400:raise ValueError('Reparse deployment path')
    return path

def validate_acl(data,private,ancestor=False):
    allowed=TRUSTED|{data['sid']}|({INSTALLER} if not private else set())
    if data['owner'] not in allowed or not data['rules'] or private and not data['protected']:
        raise ValueError('Unprotected deployment ACL')
    allowed.add('S-1-3-4') # OWNER RIGHTS resolves to the already-validated owner.
    # Write data/append/EA/delete-child/attributes/delete/write-DACL/write-owner.
    write=0x2|0x4|0x10|0x40|0x100|0x10000|0x40000|0x80000|0x10000000|0x40000000
    if ancestor:write=0x40|0x10000|0x40000|0x80000|0x10000000|0x40000000
    for rule in data['rules']:
        if not private and rule.get('inheritOnly'):continue
        if not rule['allow']:raise ValueError('Complex deployment ACL requires review')
        if rule['sid'] not in allowed and (private or rule['rights'] & write):
            raise ValueError('Broad deployment access')
    if private and not any(r['sid']==data['sid'] and r['allow'] for r in data['rules']):
        raise ValueError('Explicit service identity access required')

def check_acl(path,private=True,ancestor=False):
    if os.name!='nt':raise OSError('Windows service host required')
    path=plain(path)
    executable=Path(os.environ['SYSTEMROOT'])/'System32/WindowsPowerShell/v1.0/powershell.exe'
    env={k:v for k,v in os.environ.items() if k in {'SYSTEMROOT','WINDIR','PATH','TEMP','TMP'}}
    result=subprocess.run([str(executable),'-NoProfile','-NonInteractive','-ExecutionPolicy','Bypass','-File',str(ROOT/'tools/service_acl.ps1'),'-Target',str(path)],env=env,stdout=subprocess.PIPE,stderr=subprocess.DEVNULL,timeout=20,check=True,creationflags=subprocess.CREATE_NO_WINDOW)
    if len(result.stdout)>65536:raise ValueError('Oversize ACL')
    validate_acl(json.loads(result.stdout),private,ancestor)

def check_tree(path):
    path=plain(path)
    if not path.is_dir():raise ValueError('Deployment runtime directory required')
    executable=Path(os.environ['SYSTEMROOT'])/'System32/WindowsPowerShell/v1.0/powershell.exe'
    env={k:v for k,v in os.environ.items() if k in {'SYSTEMROOT','WINDIR','PATH','TEMP','TMP'}}
    result=subprocess.run([str(executable),'-NoProfile','-NonInteractive','-ExecutionPolicy','Bypass','-File',str(ROOT/'tools/service_acl.ps1'),'-Target',str(path),'-Tree'],env=env,stdout=subprocess.PIPE,stderr=subprocess.DEVNULL,timeout=120,check=True,creationflags=subprocess.CREATE_NO_WINDOW)
    if len(result.stdout)>1024:raise ValueError('Invalid runtime ACL summary')
    value=json.loads(result.stdout)
    if value.get('safe') is not True or type(value.get('scanned')) is not int or not 1<=value['scanned']<=30000:raise ValueError('Writable or unsupported runtime tree')

class HostLimits:
    """Apply before any child starts. Handle must live for the service lifetime."""
    def __init__(self):
        if os.name!='nt':raise OSError('Windows service host required')
        size=ctypes.c_size_t
        class Basic(ctypes.Structure):
            _fields_=[('process_time',ctypes.c_int64),('job_time',ctypes.c_int64),('flags',w.DWORD),('min_ws',size),('max_ws',size),('active',w.DWORD),('affinity',size),('priority',w.DWORD),('scheduling',w.DWORD)]
        class IO(ctypes.Structure):
            _fields_=[(n,ctypes.c_uint64) for n in ('read_ops','write_ops','other_ops','read_bytes','write_bytes','other_bytes')]
        class Extended(ctypes.Structure):
            _fields_=[('basic',Basic),('io',IO),('process_memory',size),('job_memory',size),('peak_process',size),('peak_job',size)]
        class Cpu(ctypes.Structure):_fields_=[('flags',w.DWORD),('rate',w.DWORD)]
        k=ctypes.WinDLL('kernel32',use_last_error=True);self.kernel=k
        k.CreateJobObjectW.argtypes=[ctypes.c_void_p,w.LPCWSTR];k.CreateJobObjectW.restype=w.HANDLE
        k.SetInformationJobObject.argtypes=[w.HANDLE,ctypes.c_int,ctypes.c_void_p,w.DWORD];k.SetInformationJobObject.restype=w.BOOL
        k.QueryInformationJobObject.argtypes=[w.HANDLE,ctypes.c_int,ctypes.c_void_p,w.DWORD,ctypes.c_void_p];k.QueryInformationJobObject.restype=w.BOOL
        k.AssignProcessToJobObject.argtypes=[w.HANDLE,w.HANDLE];k.AssignProcessToJobObject.restype=w.BOOL
        k.GetCurrentProcess.restype=w.HANDLE
        self.handle=k.CreateJobObjectW(None,None)
        if not self.handle:raise OSError('Service resource ownership unavailable')
        limits=Extended();limits.basic.flags=0x2000|0x200|0x8;limits.basic.active=128;limits.job_memory=8*1024**3
        cpu=Cpu(5,5000)
        if not k.SetInformationJobObject(self.handle,9,ctypes.byref(limits),ctypes.sizeof(limits)) or not k.SetInformationJobObject(self.handle,15,ctypes.byref(cpu),ctypes.sizeof(cpu)):
            raise OSError('Service resource limits unavailable')
        actual=Extended();actual_cpu=Cpu()
        if not k.QueryInformationJobObject(self.handle,9,ctypes.byref(actual),ctypes.sizeof(actual),None) or not k.QueryInformationJobObject(self.handle,15,ctypes.byref(actual_cpu),ctypes.sizeof(actual_cpu),None):raise OSError('Service limit verification unavailable')
        if actual.basic.flags!=limits.basic.flags or actual.basic.active!=128 or actual.job_memory!=limits.job_memory or actual_cpu.flags!=5 or actual_cpu.rate!=5000:raise OSError('Service limits differ')
        if not k.AssignProcessToJobObject(self.handle,k.GetCurrentProcess()):raise OSError('Service resource assignment unavailable')
        # Do not close while this process is a member: that would terminate it.
        self.evidence={'memoryBytes':actual.job_memory,'activeProcesses':actual.basic.active,'cpuRate':actual_cpu.rate,'killOnClose':True}
