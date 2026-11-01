import copy,json,os,subprocess,sys,tempfile,threading,time,unittest
from pathlib import Path
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'))
import ai_supervisor as s
from worker_process import run_owned

def plan(directory):
 return {'schemaVersion':1,'coreUrl':'https://core.example.test','allowInsecureLoopback':False,'pollSeconds':2,'backoffMaxSeconds':10,'entries':[{'family':'text','tenant':'11111111-1111-1111-1111-111111111111','profile':'22222222-2222-2222-2222-222222222222','modelDirectory':str(directory),'tokenEnvironment':'DZOKA_WORKER_TOKEN_ONE'}]}
class SupervisorTests(unittest.TestCase):
 def test_plan_fails_closed_on_unknown_fields_duplicates_unsupported_family_and_transport(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as directory:
   path=Path(directory)/'plan.json';valid=plan(directory);path.write_bytes(json.dumps(valid).encode());self.assertEqual(valid,s.load_plan(path)[0])
   for key,value in [('coreUrl','http://remote.test'),('coreUrl','https://secret@core.test'),('coreUrl','https://core.test/path'),('pollSeconds',True),('backoffMaxSeconds',1),('entries',[]),('token','secret')]:
    bad=copy.deepcopy(valid);bad[key]=value;path.write_bytes(json.dumps(bad).encode())
    with self.assertRaises((ValueError,TypeError)):s.load_plan(path)
   for key,value in [('family','shona-vendor'),('family','../../program'),('tokenEnvironment','PATH'),('tenant','not-uuid'),('modelDirectory','relative')]:
    bad=copy.deepcopy(valid);bad['entries'][0][key]=value;path.write_bytes(json.dumps(bad).encode())
    with self.assertRaises(ValueError):s.load_plan(path)
   for raw in ['{"schemaVersion":1,"schemaVersion":1}',json.dumps(valid).replace('"pollSeconds": 2','"pollSeconds": NaN'),' '*32769]:
    path.write_bytes(raw.encode())
    with self.assertRaises(ValueError):s.load_plan(path)
   bad=copy.deepcopy(valid);bad['entries']*=2;path.write_bytes(json.dumps(bad).encode())
   with self.assertRaises(ValueError):s.load_plan(path)
 def test_environment_is_scoped_per_assignment_and_all_credentials_validate_before_work(self):
  p=plan(ROOT);environ={'PATH':'approved','DZOKA_WORKER_TOKEN_ONE':'opaque-one','DZOKA_WORKER_TOKEN_TWO':'opaque-two','DZOKA_AI_TENANT':'wrong','DZOKA_AI_PROFILE':'wrong','DATABASE_URL':'secret-db','AWS_SECRET_ACCESS_KEY':'secret-s3','PYTHONPATH':'untrusted','DZOKA_AI_SHONA_LOCAL_DEVELOPMENT':'true'}
  env=s.child_environment(p,p['entries'][0],environ);self.assertEqual('opaque-one',env['DZOKA_AI_TOKEN']);self.assertEqual(p['entries'][0]['tenant'],env['DZOKA_AI_TENANT']);self.assertNotIn('DZOKA_AI_PROFILE',env)
  for name in ('DATABASE_URL','AWS_SECRET_ACCESS_KEY','PYTHONPATH','DZOKA_WORKER_TOKEN_TWO','DZOKA_AI_SHONA_LOCAL_DEVELOPMENT'):self.assertNotIn(name,env)
  second=copy.deepcopy(p['entries'][0]);second.update(tenant='33333333-3333-3333-3333-333333333333',tokenEnvironment='DZOKA_WORKER_TOKEN_MISSING');p['entries'].append(second)
  with self.assertRaises(ValueError):s.Supervisor(p,environ)
 def test_backoff_is_capped_and_one_failure_does_not_starve_next_assignment(self):
  p=plan(ROOT);second=copy.deepcopy(p['entries'][0]);second['profile']='33333333-3333-3333-3333-333333333333';p['entries'].append(second);clock=[0.];calls=[];codes=iter([1,0,124,0,1,0,0,0])
  def invoke(command,env,cwd,timeout):calls.append((command,env,timeout));return next(codes)
  supervisor=s.Supervisor(p,{'DZOKA_WORKER_TOKEN_ONE':'opaque'},clock=lambda:clock[0],invoke=invoke)
  supervisor.tick();self.assertEqual([4,2],[x['due'] for x in supervisor.states]);supervisor.tick();self.assertEqual(2,len(calls))
  clock[0]=4;supervisor.tick();self.assertEqual(12,supervisor.states[0]['due']);self.assertEqual(1,supervisor.states[0]['deadlines'])
  clock[0]=12;supervisor.tick();self.assertEqual(22,supervisor.states[0]['due']);clock[0]=22;supervisor.tick();self.assertEqual(0,supervisor.states[0]['consecutiveFailures']);self.assertEqual(24,supervisor.states[0]['due'])
  self.assertTrue(all(x[0][-2]=='text' and x[2]==110 for x in calls));self.assertEqual(4,supervisor.states[0]['invocations'])
 def test_stop_does_not_start_a_second_worker_and_status_cannot_echo_exception_secrets(self):
  p=plan(ROOT);p['entries']*=2;stop=threading.Event();events=[]
  def invoke(*_):stop.set();raise OSError('private report and secret token')
  supervisor=s.Supervisor(p,{'DZOKA_WORKER_TOKEN_ONE':'secret'},stop=stop,invoke=invoke,emit=events.append);supervisor.tick()
  self.assertEqual(1,supervisor.states[0]['failedInvocations']);self.assertEqual(0,supervisor.states[1]['invocations']);self.assertNotIn('secret',json.dumps(events));self.assertNotIn('secret',json.dumps(supervisor.snapshot()))
 def test_owned_timeout_kills_actual_descendant_and_never_inherits_child_output(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as directory:
   heartbeat=Path(directory)/'heartbeat';descendant="import pathlib,time; p=pathlib.Path("+repr(str(heartbeat))+ ");\nwhile True: p.write_text(str(time.time()));time.sleep(.02)"
   bootstrap="import sys,subprocess,time;\nif sys.stdin.buffer.read(1)!=b'1':raise SystemExit(2)\nsubprocess.Popen([sys.executable,'-c',"+repr(descendant)+"]);print('private output',flush=True);time.sleep(60)"
   code=run_owned([sys.executable,'-c',bootstrap],os.environ.copy(),ROOT,1.5);self.assertEqual(124,code);self.assertTrue(heartbeat.exists());time.sleep(.2);stamp=heartbeat.stat().st_mtime_ns;time.sleep(.2);self.assertEqual(stamp,heartbeat.stat().st_mtime_ns)
 def test_ownership_failure_prevents_go_byte_and_bootstrap_work(self):
  if os.name!='nt':self.skipTest('Windows ownership failure path')
  with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as directory:
   marker=Path(directory)/'work';bootstrap="import sys,pathlib;sys.stdin.buffer.read(1);pathlib.Path("+repr(str(marker))+").write_text('started')"
   with patch('worker_process.WindowsJob',side_effect=OSError('denied')),self.assertRaises(OSError):run_owned([sys.executable,'-c',bootstrap],os.environ.copy(),ROOT,1)
   self.assertFalse(marker.exists())
 def test_source_mutation_is_rejected_before_any_claim(self):
  identity=s.verify_sources()
  with self.assertRaisesRegex(ValueError,'identity changed'):s.verify_sources('0'*64)
  with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as directory:
   root=Path(directory);(root/'ai').mkdir();(root/'test.py').write_bytes(b'changed');(root/'ai/supervisor.lock.json').write_bytes(json.dumps({'sources':{'test.py':'0'*64}}).encode())
   with patch.object(s,'ROOT',root),self.assertRaisesRegex(ValueError,'implementation changed'):s.verify_sources()

 def test_parent_death_closes_windows_job_and_stops_actual_descendant(self):
  if os.name!='nt':self.skipTest('Windows parent-death ownership contract')
  with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as directory:
   heartbeat=Path(directory)/'heartbeat';descendant="import pathlib,time; p=pathlib.Path("+repr(str(heartbeat))+ ");\nwhile True: p.write_text(str(time.time()));time.sleep(.02)"
   bootstrap="import sys,subprocess,time;sys.stdin.buffer.read(1);subprocess.Popen([sys.executable,'-c',"+repr(descendant)+"]);time.sleep(60)"
   owner="import sys,os;sys.path.insert(0,"+repr(str(ROOT/'tools'))+");from worker_process import run_owned;run_owned([sys.executable,'-c',"+repr(bootstrap)+"],os.environ.copy(),"+repr(str(ROOT))+",60)"
   parent=subprocess.Popen([sys.executable,'-c',owner],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
   try:
    until=time.monotonic()+8
    while not heartbeat.exists() and time.monotonic()<until and parent.poll() is None:time.sleep(.05)
    self.assertTrue(heartbeat.exists());parent.kill();parent.wait(timeout=5);time.sleep(.2);stamp=heartbeat.stat().st_mtime_ns;time.sleep(.2);self.assertEqual(stamp,heartbeat.stat().st_mtime_ns)
   finally:
    if parent.poll() is None:parent.kill();parent.wait(timeout=5)

if __name__=='__main__':unittest.main()
