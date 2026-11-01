"""Host guardian controller; intentionally outside the worker Job Object."""
import json
import os
from pathlib import Path
import queue
import secrets
import subprocess
import sys
import threading
import uuid
sys.path.insert(0,str(Path(__file__).resolve().parent))
from worker_process import run_owned
from docker_lifecycle import docker_env,reconcile,preflight
ROOT=Path(__file__).resolve().parents[1]

class Guardian:
    def __init__(self,models,state,identity,timeout=290):
        self.owner=uuid.uuid4().hex;self.key=secrets.token_hex(32);self.state=Path(state).resolve();self.process=None
        env=docker_env();env.update(PYTHONNOUSERSITE='1',PYTHONUTF8='1',DZOKA_GUARDIAN_KEY=self.key,DZOKA_GUARDIAN_MODELS=str(Path(models).resolve()),DZOKA_GUARDIAN_STATE=str(self.state),DZOKA_GUARDIAN_LIFETIME=str(min(330,timeout+30)))
        options={'creationflags':subprocess.CREATE_NO_WINDOW} if os.name=='nt' else {'start_new_session':True}
        self.process=subprocess.Popen([sys.executable,'-I',str(ROOT/'tools/docker_guardian.py'),'--identity',identity,'--owner',self.owner],env=env,cwd=ROOT,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.DEVNULL,close_fds=True,**options)
        ready=queue.Queue(1);process=self.process
        threading.Thread(target=lambda:ready.put(process.stdout.readline(4097)),daemon=True).start()
        try:
            raw=ready.get(timeout=90);value=json.loads(raw)
            if set(value)!={'port'} or type(value['port']) is not int or not 1<=value['port']<=65535:raise ValueError('Guardian startup failed')
            self.port=value['port']
        except Exception:self.close();raise OSError('Docker guardian unavailable') from None
    def environment(self):return {'DZOKA_AI_DOCKER_PORT':str(self.port),'DZOKA_AI_DOCKER_KEY':self.key}
    def close(self):
        if self.process is None:return
        process=self.process;self.process=None
        if process.stdin and not process.stdin.closed:process.stdin.close()
        try:code=process.wait(timeout=60)
        except subprocess.TimeoutExpired:
            process.kill();process.wait(timeout=10);code=1
            # Durable intents survive a stuck guardian. Cleanup is scoped by journal/labels.
            try:preflight();reconcile(self.state)
            except Exception:pass
        finally:
            if process.stdout:process.stdout.close()
        if code:raise OSError('Docker guardian cleanup unavailable')
    def __enter__(self):return self
    def __exit__(self,*_):self.close()

def invoke_owned_docker(command,env,cwd,timeout,identity):
    with Guardian(env['DZOKA_AI_SHONA_SUPERVISED_DIR'],env['DZOKA_AI_DOCKER_STATE_DIR'],identity,timeout) as guardian:
        return run_owned(command,{**env,**guardian.environment()},cwd,timeout)
