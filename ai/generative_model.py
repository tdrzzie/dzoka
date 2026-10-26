"""Windows read-share-only model handles avoid large Python graph copies and mutable weights."""
import ctypes
from ctypes import wintypes
from contextlib import contextmanager
import hashlib
import msvcrt
import os

@contextmanager
def locked_model(path,entry):
    kernel=ctypes.WinDLL('kernel32',use_last_error=True)
    create=kernel.CreateFileW;create.argtypes=[wintypes.LPCWSTR,wintypes.DWORD,wintypes.DWORD,wintypes.LPVOID,wintypes.DWORD,wintypes.DWORD,wintypes.HANDLE];create.restype=wintypes.HANDLE
    handle=create(str(path.resolve()),0x80000000,1,None,3,0x80,None)
    if handle==ctypes.c_void_p(-1).value:raise OSError(ctypes.get_last_error(),'Approved model read lock unavailable')
    try:fd=msvcrt.open_osfhandle(handle,os.O_RDONLY|os.O_BINARY)
    except Exception:
        kernel.CloseHandle.argtypes=[wintypes.HANDLE];kernel.CloseHandle(handle);raise
    with os.fdopen(fd,'rb') as stream:
        if os.fstat(stream.fileno()).st_size!=entry['bytes'] or hashlib.file_digest(stream,'sha256').hexdigest()!=entry['sha256']:raise ValueError('Model changed before loading')
        yield str(path.resolve())
