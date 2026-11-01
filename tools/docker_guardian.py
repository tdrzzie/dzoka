"""Independent guardian. Only its parent owns the control-pipe writer."""
import argparse
import json
import os
from pathlib import Path
import secrets
import socket
import sys
import threading
import time
sys.path.insert(0,str(Path(__file__).resolve().parent))
from ai_supervisor import verify_sources
from docker_lifecycle import OwnerLock,Session,preflight,reconcile,image,HEX
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'ai'))
from docker_rpc import read_frame,send_frame

def serve(models,state,owner,key,lifetime):
    stop=threading.Event();lock=OwnerLock(state,owner)
    if not lock.acquire():raise ValueError('Guardian ownership unavailable')
    session=Session(models,state,owner,stop);complete=False;timer=None
    try:
        preflight();result=reconcile(state)
        if result['unresolved']:raise ValueError('Unresolved Docker intent')
        def parent_watch():
            # No data is expected: EOF is normal stop or parent death.
            os.read(sys.stdin.fileno(),1);stop.set()
        threading.Thread(target=parent_watch,daemon=True).start()
        timer=threading.Timer(lifetime,stop.set);timer.daemon=True;timer.start()
        with socket.socket() as server:
            server.bind(('127.0.0.1',0));server.listen(2);server.settimeout(.2)
            print(json.dumps({'port':server.getsockname()[1]}),flush=True)
            until=time.monotonic()+lifetime
            while not stop.is_set() and time.monotonic()<until:
                if session.prepared_at is not None and session.record['history'][-1]['state']!='REMOVED' and time.monotonic()-session.prepared_at>10:break
                try:connection,_=server.accept()
                except socket.timeout:continue
                with connection:
                    try:
                        request=read_frame(connection,300000,5)
                        if not isinstance(request,dict) or not isinstance(request.get('key'),str) or not secrets.compare_digest(request['key'],key):raise ValueError('Denied')
                        op=request.get('operation')
                        if op=='check' and set(request)=={'key','operation'}:response={'ok':True,'imageId':image()}
                        elif op=='prepare' and set(request)=={'key','operation','group'}:session.prepare(request['group']);response={'ok':True}
                        elif op=='native' and set(request)=={'key','operation','pieces'}:response={'ok':True,'tokens':session.native(request['pieces'])}
                        else:raise ValueError('Unsupported guardian request')
                    except Exception:response={'ok':False}
                    try:send_frame(connection,response,100000)
                    except (OSError,ValueError):stop.set()
        complete=session.close()
    finally:
        try:complete=session.close() and complete
        finally:
            if timer:timer.cancel()
            lock.close()
    return 0 if complete else 1

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--identity',required=True);parser.add_argument('--owner',required=True);args=parser.parse_args()
    try:
        verify_sources(args.identity)
        key=os.environ['DZOKA_GUARDIAN_KEY'];lifetime=int(os.environ['DZOKA_GUARDIAN_LIFETIME'])
        if not HEX.fullmatch(args.owner) or len(key)!=64 or any(c not in '0123456789abcdef' for c in key) or not 1<=lifetime<=330:raise ValueError('Guardian configuration')
        raise SystemExit(serve(Path(os.environ['DZOKA_GUARDIAN_MODELS']),Path(os.environ['DZOKA_GUARDIAN_STATE']),args.owner,key,lifetime))
    except Exception:raise SystemExit(1)
