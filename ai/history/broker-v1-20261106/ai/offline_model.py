"""Fixed model side of the broker. No core client or credential is imported."""
import base64
import hashlib
import importlib
import os
from pathlib import Path
import sys
sys.path.insert(0,str(Path(__file__).resolve().parent))
from broker_profiles import profiles,FAMILIES,MODEL_DIR
from model_wire import read,write,REQUEST_LIMIT,RESULT_LIMIT
ROOT=Path(__file__).resolve().parents[1]

def boundary_check():
    import socket
    import ctypes
    from ctypes import wintypes as w
    firewall=ctypes.WinDLL('FirewallAPI.dll',use_last_error=True)
    diagnose=firewall.NetworkIsolationDiagnoseConnectFailureAndGetInfo
    diagnose.argtypes=[w.LPCWSTR,ctypes.POINTER(w.DWORD)];diagnose.restype=w.DWORD
    # A refusal/timeout alone is not proof of isolation. Windows can silently
    # drop outbound traffic; require its native missing-capability diagnostic.
    # Loopback's authoritative checks are the actual package token and the
    # host's no-exemption query before resume. Reachable-listener negative
    # controls are part of the independent native boundary acceptance suite.
    for address in ('192.0.2.1',):
        with socket.socket(socket.AF_INET,socket.SOCK_STREAM) as connection:
            connection.settimeout(2)
            try:connection.connect((address,9))
            except OSError as error:
                missing=w.DWORD();diagnostic=diagnose(address,ctypes.byref(missing))
                denied=getattr(error,'winerror',None)==10013 or error.errno in (13,10013) or isinstance(error,TimeoutError) and diagnostic==0 and missing.value in (1,2,3)
                if not denied:raise ValueError('Network capability denial unconfirmed') from None
            else:raise ValueError('Network capability available')

def run():
    request=read(sys.stdin.buffer,REQUEST_LIMIT)
    if not isinstance(request,dict) or set(request)!={'version','id','profile','inputSha256','data'} or request['version']!=1:raise ValueError('Invalid model request')
    import uuid
    if str(uuid.UUID(request['id']))!=request['id']:raise ValueError('Invalid invocation')
    if any(k.startswith(('DZOKA_','AWS_','PG')) or k in ('DATABASE_URL','HTTP_PROXY','HTTPS_PROXY') for k in os.environ):raise ValueError('Credential-bearing model environment')
    boundary_check()
    write(sys.stdout.buffer,{'version':1,'id':request['id'],'type':'ready','boundary':'no-capabilities-egress-denied-v1'},RESULT_LIMIT)
    selected=profiles()[request['profile']];raw=base64.b64decode(request['data'],validate=True)
    if len(raw)>20*1024*1024 or hashlib.sha256(raw).hexdigest()!=request['inputSha256']:raise ValueError('Input changed across boundary')
    family=selected['family'];directory=ROOT/'.audit-cache'/MODEL_DIR[family];provider=None
    try:
        if family=='text':
            from provider import TextEmbeddingProvider
            provider=TextEmbeddingProvider(directory);output=provider.embed(raw.decode('utf-8'))
        else:
            module,class_name=FAMILIES[family];cls=getattr(importlib.import_module(module),class_name)
            if family=='shona-cpu':
                class IsolatedCpu(cls):
                    def check_image(self):pass # Host independently checks the pinned image before accepting native RPC.
                    def native(self,pieces):
                        write(sys.stdout.buffer,{'version':1,'id':request['id'],'type':'native','pieces':pieces},RESULT_LIMIT)
                        answer=read(sys.stdin.buffer,RESULT_LIMIT)
                        if not isinstance(answer,dict) or set(answer)!={'version','id','tokens'} or answer['version']!=1 or answer['id']!=request['id']:raise ValueError('Invalid native response')
                        tokens=answer['tokens']
                        if not isinstance(tokens,list) or not 1<=len(tokens)<=32 or any(not isinstance(t,str) or not 1<=len(t.encode())<=1024 or '\0' in t for t in tokens):raise ValueError('Invalid native tokens')
                        return tokens
                cls=IsolatedCpu
            provider=cls(selected['base'],directory)
            provider.profile={'key':selected['key'],'manifest':selected['manifest']}
            output=provider.infer(raw)
        write(sys.stdout.buffer,{'version':1,'id':request['id'],'type':'result','inputSha256':request['inputSha256'],'profileKey':selected['key'],'output':output},RESULT_LIMIT)
    finally:
        if provider is not None and hasattr(provider,'close'):provider.close()

if __name__=='__main__':
    try:run()
    except Exception:raise SystemExit(1)
