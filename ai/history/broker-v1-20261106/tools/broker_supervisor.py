"""Serial broker scheduling; source/core authorization remain independent."""
import os
from pathlib import Path
import threading
import time
from model_broker import run_once

class BrokerSupervisor:
    def __init__(self,plan,environ,stop=None):
        self.plan=plan;self.stop=stop or threading.Event();self.entries=[]
        for entry in plan['entries']:
            token=environ.get(entry['tokenEnvironment'])
            if not isinstance(token,str) or not 1<=len(token)<=16384 or any(ord(c)<=32 or ord(c)>126 for c in token):raise ValueError('Broker credential unavailable')
            self.entries.append((entry,token,{'due':0.,'failures':0}))
    def tick(self):
        for entry,token,state in self.entries:
            if self.stop.is_set():break
            if time.monotonic()<state['due']:continue
            previous=os.environ.get('DZOKA_AI_ALLOW_INSECURE_LOOPBACK')
            try:
                if self.plan['allowInsecureLoopback']:os.environ['DZOKA_AI_ALLOW_INSECURE_LOOPBACK']='true'
                else:os.environ.pop('DZOKA_AI_ALLOW_INSECURE_LOOPBACK',None)
                run_once(self.plan['coreUrl'],token,entry['tenant'],entry['profile'],entry['profileName'],entry['bundle'],entry['bundleSha256'],entry['stateDirectory']);state['failures']=0
            except Exception:state['failures']=min(16,state['failures']+1)
            finally:
                if previous is None:os.environ.pop('DZOKA_AI_ALLOW_INSECURE_LOOPBACK',None)
                else:os.environ['DZOKA_AI_ALLOW_INSECURE_LOOPBACK']=previous
            state['due']=time.monotonic()+min(self.plan['backoffMaxSeconds'],self.plan['pollSeconds']*2**state['failures'])
