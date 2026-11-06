"""Read actual process identity; never provision accounts or adjust privileges."""
import ctypes
from ctypes import wintypes as w
import os
import re

SID=re.compile(r'S-1-5-(?:21|80)(?:-[0-9]{1,10}){1,8}\Z')

def validate(snapshot,expected,integrity=0x2000):
    if not isinstance(expected,str) or not SID.fullmatch(expected):raise ValueError('Explicit private service identity required')
    if type(integrity) is not int or integrity not in (0x1000,0x2000,0x3000,0x4000):raise ValueError('Explicit integrity policy required')
    # MIC level and account privileges are distinct: SCM may supply system IL
    # for a non-administrator account. Require the declared level, never infer
    # administrator permission from MIC alone.
    if snapshot['sid']!=expected or snapshot['elevated'] or snapshot['administrator'] or snapshot['integrity']!=integrity:
        raise ValueError('Service account does not satisfy policy')

def identity():
    if os.name!='nt':raise OSError('Windows identity required')
    k=ctypes.WinDLL('kernel32',use_last_error=True);a=ctypes.WinDLL('advapi32',use_last_error=True)
    k.GetCurrentProcess.restype=w.HANDLE;k.CloseHandle.argtypes=[w.HANDLE]
    a.OpenProcessToken.argtypes=[w.HANDLE,w.DWORD,ctypes.POINTER(w.HANDLE)];a.OpenProcessToken.restype=w.BOOL
    a.GetTokenInformation.argtypes=[w.HANDLE,ctypes.c_int,ctypes.c_void_p,w.DWORD,ctypes.POINTER(w.DWORD)];a.GetTokenInformation.restype=w.BOOL
    a.ConvertSidToStringSidW.argtypes=[ctypes.c_void_p,ctypes.POINTER(w.LPWSTR)];a.ConvertSidToStringSidW.restype=w.BOOL
    a.ConvertStringSidToSidW.argtypes=[w.LPCWSTR,ctypes.POINTER(ctypes.c_void_p)];a.ConvertStringSidToSidW.restype=w.BOOL
    a.CheckTokenMembership.argtypes=[w.HANDLE,ctypes.c_void_p,ctypes.POINTER(w.BOOL)];a.CheckTokenMembership.restype=w.BOOL
    a.GetSidSubAuthorityCount.argtypes=[ctypes.c_void_p];a.GetSidSubAuthorityCount.restype=ctypes.POINTER(ctypes.c_ubyte)
    a.GetSidSubAuthority.argtypes=[ctypes.c_void_p,w.DWORD];a.GetSidSubAuthority.restype=ctypes.POINTER(w.DWORD)
    k.LocalFree.argtypes=[ctypes.c_void_p];k.LocalFree.restype=ctypes.c_void_p
    token=w.HANDLE()
    if not a.OpenProcessToken(k.GetCurrentProcess(),8,ctypes.byref(token)):raise OSError('Token unavailable')
    def information(kind):
        size=w.DWORD();a.GetTokenInformation(token,kind,None,0,ctypes.byref(size));buffer=ctypes.create_string_buffer(size.value)
        if not a.GetTokenInformation(token,kind,buffer,size,ctypes.byref(size)):raise OSError('Token information unavailable')
        return buffer
    try:
        user=information(1);sid=ctypes.c_void_p.from_buffer(user).value;text=w.LPWSTR()
        if not a.ConvertSidToStringSidW(sid,ctypes.byref(text)):raise OSError('Token SID unavailable')
        try:name=text.value
        finally:k.LocalFree(ctypes.cast(text,ctypes.c_void_p))
        elevation=w.DWORD.from_buffer(information(20)).value
        label=information(25);label_sid=ctypes.c_void_p.from_buffer(label).value
        integrity=a.GetSidSubAuthority(label_sid,a.GetSidSubAuthorityCount(label_sid)[0]-1)[0]
        admin=ctypes.c_void_p();enabled=w.BOOL()
        if not a.ConvertStringSidToSidW('S-1-5-32-544',ctypes.byref(admin)):raise OSError('Group SID unavailable')
        try:
            if not a.CheckTokenMembership(None,admin,ctypes.byref(enabled)):raise OSError('Membership unavailable')
        finally:k.LocalFree(admin)
        return {'sid':name,'elevated':bool(elevation),'administrator':bool(enabled.value),'integrity':integrity}
    finally:k.CloseHandle(token)
