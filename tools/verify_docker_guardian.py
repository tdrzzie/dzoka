"""Real task-owned Docker lifecycle checks; never restart/prune the daemon."""
import json
import os
from pathlib import Path
import subprocess
import sys
import time
import uuid
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'tools'));sys.path.insert(0,str(ROOT/'ai'))
from ai_supervisor import verify_sources
from docker_owner import Guardian
from docker_lifecycle import Session,OwnerLock,cli,image,preflight,reconcile,lookup
from docker_rpc import call
from shona_supervised import profiles

def wait_for(predicate,seconds):
    until=time.monotonic()+seconds
    while time.monotonic()<until:
        if predicate():return
        time.sleep(.2)
    raise AssertionError('Owned Docker cleanup deadline exceeded')

def main():
    identity=verify_sources();profiles();preflight();models=(ROOT/'.audit-cache/shona-cpu-models').resolve()
    run=ROOT/'.test-tmp'/('docker-guardian-'+uuid.uuid4().hex);run.mkdir();decoy='dzoka-guardian-decoy-'+uuid.uuid4().hex;decoy_id=None;cases=[]
    try:
        decoy_id=cli(['create','--pull','never','--name',decoy,'--network','none',image()]).decode().strip()
        state=run/'orphan';state.mkdir();owner=uuid.uuid4().hex;lock=OwnerLock(state,owner);assert lock.acquire();session=Session(models,state,owner,__import__('threading').Event())
        try:
            session.prepare('CT2_EN_SN');assert reconcile(state)['active']==1;cases.append('active_owner_excluded')
            lock.close();assert reconcile(state)['removed']==1;assert reconcile(state)=={'removed':0,'active':0,'unresolved':0};cases.append('orphan_reconciled_idempotently')
        finally:
            lock.close();session.stack.close();reconcile(state)
        assert json.loads(cli(['inspect',decoy_id]))[0]['Name']=='/'+decoy;cases.append('unrelated_same_image_preserved')
        state=run/'deadline';state.mkdir()
        with Guardian(models,state,identity) as guardian:
            os.environ.update(guardian.environment());correct=os.environ['DZOKA_AI_DOCKER_KEY'];os.environ['DZOKA_AI_DOCKER_KEY']='0'*64
            try:
                try:call('prepare',group='CT2_EN_SN');raise AssertionError('Bad IPC key accepted')
                except ValueError:pass
                assert not list(state.glob('*.json'));cases.append('unauthorized_ipc_cannot_create')
            finally:os.environ['DZOKA_AI_DOCKER_KEY']=correct
            call('prepare',group='CT2_EN_SN');path=next(state.glob('*.json'))
            wait_for(lambda:json.loads(path.read_bytes())['history'][-1]['state']=='REMOVED',45);cases.append('prepared_container_deadline_removed')
        state=run/'parent-death';state.mkdir();ready=run/'ready.json'
        source="""import os,sys,json,time
from pathlib import Path
sys.path.insert(0,sys.argv[1]+'/tools');sys.path.insert(0,sys.argv[1]+'/ai')
from docker_owner import Guardian
from docker_rpc import call
with Guardian(Path(sys.argv[2]),Path(sys.argv[3]),sys.argv[5]) as g:
 os.environ.update(g.environment());call('prepare',group='CT2_SN_EN')
 record=json.loads(next(Path(sys.argv[3]).glob('*.json')).read_bytes());Path(sys.argv[4]).write_text(json.dumps({'name':record['name']}));time.sleep(120)
"""
        options={'creationflags':subprocess.CREATE_NO_WINDOW} if os.name=='nt' else {}
        parent=subprocess.Popen([sys.executable,'-I','-c',source,str(ROOT),str(models),str(state),str(ready),identity],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL,**options)
        try:
            wait_for(lambda:ready.exists() or parent.poll() is not None,90);assert ready.exists(),'Guardian fixture failed before resource preparation'
            parent.kill();parent.wait(timeout=10);path=next(state.glob('*.json'))
            wait_for(lambda:json.loads(path.read_bytes())['history'][-1]['state']=='REMOVED',45)
            wait_for(lambda:reconcile(state)['active']==0,10);cases.append('parent_death_removed_daemon_resource')
        finally:
            if parent.poll() is None:parent.kill();parent.wait(timeout=10)
        assert json.loads(cli(['inspect',decoy_id]))[0]['Name']=='/'+decoy
        result={'status':'PASS','cases':cases,'scope':'Windows/local Linux Docker lifecycle; no daemon restart, cross-host or production claim'}
        (run/'result.json').write_bytes((json.dumps(result,indent=2)+'\n').encode());print('PASS: '+str(len(cases))+' real owned Docker checks; '+str(run/'result.json'),flush=True)
    finally:
        if decoy_id:cli(['rm','-f',decoy_id])
        for state in run.iterdir():
            if state.is_dir():reconcile(state)
if __name__=='__main__':main()
