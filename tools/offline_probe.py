"""Fixed isolated conformance probe; not a model provider or network client."""
import hashlib
import json
from pathlib import Path
import socket
import sys
import time
import os
import ctypes
from ctypes import wintypes as w

request=json.loads(sys.stdin.buffer.read(16385))
if request.get('wait'):time.sleep(60)
denied=[];errors=[]
firewall=ctypes.WinDLL('FirewallAPI.dll',use_last_error=True)
diagnose=firewall.NetworkIsolationDiagnoseConnectFailureAndGetInfo
diagnose.argtypes=[w.LPCWSTR,ctypes.POINTER(w.DWORD)];diagnose.restype=w.DWORD
for host,port in request['endpoints']:
    with socket.socket(socket.AF_INET6 if ':' in host else socket.AF_INET,socket.SOCK_STREAM) as client:
        client.settimeout(2)
        try:client.connect((host,port))
        except OSError as error:
            missing=w.DWORD();diagnostic=diagnose(host,ctypes.byref(missing))
            denied.append(getattr(error,'winerror',None)==10013 or error.errno in (13,10013) or isinstance(error,TimeoutError) and diagnostic==0 and missing.value in (1,2,3))
            errors.append({'errno':error.errno,'winerror':getattr(error,'winerror',None),'timeout':isinstance(error,TimeoutError),'diagnostic':diagnostic,'missingCapability':missing.value})
        else:denied.append(False);errors.append(None)
try:Path(request['privateFile']).read_bytes()
except PermissionError:file_denied=True
else:file_denied=False
print(json.dumps({'credentialEnvironmentPresent':any(name.startswith(('DZOKA_','AWS_','PG')) or name in ('DATABASE_URL','HTTP_PROXY','HTTPS_PROXY') for name in os.environ),'directOrCapabilityDenial':denied,'networkErrors':errors,'privateFileDenied':file_denied,'payloadSha256':hashlib.sha256(request['payload'].encode()).hexdigest(),'rawBytes':len(request['payload'].encode())}),flush=True)
