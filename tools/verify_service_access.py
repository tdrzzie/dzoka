"""Read-only Windows SCM preflight for the Dzoka worker service."""

import ctypes
import json
import os
import sys
from ctypes import wintypes
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from windows_service import NAME


class ServiceStatus(ctypes.Structure):
    _fields_ = [(field, wintypes.DWORD) for field in (
        'service_type', 'state', 'controls', 'exit_code', 'service_exit_code',
        'checkpoint', 'wait_hint',
    )]


def probe():
    if os.name != 'nt':
        raise OSError('Windows SCM required')
    api = ctypes.WinDLL('advapi32', use_last_error=True)
    handle = wintypes.HANDLE
    api.OpenSCManagerW.argtypes = [wintypes.LPCWSTR, wintypes.LPCWSTR, wintypes.DWORD]
    api.OpenSCManagerW.restype = handle
    api.OpenServiceW.argtypes = [handle, wintypes.LPCWSTR, wintypes.DWORD]
    api.OpenServiceW.restype = handle
    api.QueryServiceStatus.argtypes = [handle, ctypes.POINTER(ServiceStatus)]
    api.QueryServiceStatus.restype = wintypes.BOOL
    api.CloseServiceHandle.argtypes = [handle]
    api.CloseServiceHandle.restype = wintypes.BOOL

    result = {'service': NAME, 'scope': 'READ_ONLY_CURRENT_TOKEN'}
    create_access = api.OpenSCManagerW(None, None, 0x0002)  # SC_MANAGER_CREATE_SERVICE
    result['createServiceAccess'] = bool(create_access)
    if create_access:
        api.CloseServiceHandle(create_access)
    else:
        result['createServiceWin32Error'] = ctypes.get_last_error()

    manager = api.OpenSCManagerW(None, None, 0x0001)  # SC_MANAGER_CONNECT
    if not manager:
        result['connectWin32Error'] = ctypes.get_last_error()
        return result
    try:
        service = api.OpenServiceW(manager, NAME, 0x0004)  # SERVICE_QUERY_STATUS
        if not service:
            error = ctypes.get_last_error()
            result['serviceQueryWin32Error'] = error
            result['installation'] = 'ABSENT' if error == 1060 else 'UNKNOWN'
            return result
        try:
            result['installation'] = 'PRESENT'
            status = ServiceStatus()
            if api.QueryServiceStatus(service, ctypes.byref(status)):
                result['serviceState'] = status.state
            else:
                result['serviceStatusWin32Error'] = ctypes.get_last_error()
        finally:
            api.CloseServiceHandle(service)
    finally:
        api.CloseServiceHandle(manager)
    return result


if __name__ == '__main__':
    print(json.dumps(probe(), sort_keys=True))
