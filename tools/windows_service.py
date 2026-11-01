"""Independent standard-library SCM declarations; installation is operator-owned."""
import ctypes
from ctypes import wintypes as w
import os
import threading

NAME='DzokaWorkers'
def dispatch(run):
    if os.name!='nt':raise OSError('Windows SCM required')
    api=ctypes.WinDLL('advapi32',use_last_error=True)
    class Status(ctypes.Structure):
        _fields_=[(n,w.DWORD) for n in ('kind','state','accepted','error','specific','checkpoint','hint')]
    Handler=ctypes.WINFUNCTYPE(w.DWORD,w.DWORD,w.DWORD,ctypes.c_void_p,ctypes.c_void_p)
    Main=ctypes.WINFUNCTYPE(None,w.DWORD,ctypes.POINTER(w.LPWSTR))
    class Entry(ctypes.Structure):_fields_=[('name',w.LPWSTR),('main',Main)]
    api.RegisterServiceCtrlHandlerExW.argtypes=[w.LPCWSTR,Handler,ctypes.c_void_p];api.RegisterServiceCtrlHandlerExW.restype=w.HANDLE
    api.SetServiceStatus.argtypes=[w.HANDLE,ctypes.POINTER(Status)];api.SetServiceStatus.restype=w.BOOL
    api.StartServiceCtrlDispatcherW.argtypes=[ctypes.POINTER(Entry)];api.StartServiceCtrlDispatcherW.restype=w.BOOL
    stop=threading.Event();handle=None;result=[1];mutex=threading.Lock();checkpoint=[0]
    def report(state,error=0):
        with mutex:
            checkpoint[0]=checkpoint[0]+1 if state in (2,3) else 0
            status=Status(0x10,state,5 if state==4 else 0,error,1 if error==1066 else 0,checkpoint[0],600000 if state in (2,3) else 0)
            if not api.SetServiceStatus(handle,ctypes.byref(status)):stop.set();raise OSError('SCM status unavailable')
    @Handler
    def control(code,*_):
        try:
            if code in (1,5):stop.set();report(3)
            return 0
        except Exception:return 1
    @Main
    def service_main(*_):
        nonlocal handle
        handle=api.RegisterServiceCtrlHandlerExW(NAME,control,None)
        if not handle:return
        try:
            report(2);result[0]=run(stop,lambda:report(4) if not stop.is_set() else report(3))
        except Exception:result[0]=1
        finally:report(1,1066 if result[0] else 0)
    table=(Entry*2)(Entry(NAME,service_main),Entry(None,Main()))
    if not api.StartServiceCtrlDispatcherW(table):raise OSError('SCM dispatcher unavailable')
    return result[0]
