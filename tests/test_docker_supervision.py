import copy,json,socket,struct,sys,tempfile,threading,unittest,uuid
from pathlib import Path
from unittest.mock import patch
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools'));sys.path.insert(0,str(ROOT/'ai'))
import docker_lifecycle as d
import docker_rpc
import shona_supervised
from shona_cpu import profiles as base_profiles

def intent(owner=None):
 owner=owner or uuid.uuid4().hex;resource=uuid.uuid4().hex
 return {'schemaVersion':1,'owner':owner,'resource':resource,'name':'dzoka-owned-'+owner+'-'+resource,'image':d.image(),'history':[]}
class DockerSupervisionTests(unittest.TestCase):
 def test_cleanup_requires_full_name_labels_and_approved_image(self):
  r=intent();container={'Id':'a'*64,'Name':'/'+r['name'],'Image':r['image'],'Config':{'Image':r['image'],'Labels':{d.MANAGED:'v1',d.OWNER:r['owner'],d.RESOURCE:r['resource']}}};self.assertTrue(d.matches(r,container))
  for field,value in [('Name','/unrelated'),('Image','sha256:other'),('Id','not-an-id')]:
   changed=copy.deepcopy(container);changed[field]=value;self.assertFalse(d.matches(r,changed))
  for label in (d.MANAGED,d.OWNER,d.RESOURCE):
   changed=copy.deepcopy(container);changed['Config']['Labels'][label]='unrelated';self.assertFalse(d.matches(r,changed))
 def test_unknown_create_absence_is_not_a_cleanup_success(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as directory:
   r=d.persist(directory,intent(),'INTENT')
   with patch.object(d,'lookup',return_value=None):
    result,done=d.cleanup(directory,r);self.assertFalse(done);self.assertEqual('INTENT',result['history'][-1]['state'])
    r=d.persist(directory,r,'CREATED');result,done=d.cleanup(directory,r);self.assertTrue(done);self.assertEqual('REMOVED',result['history'][-1]['state'])
 def test_daemon_failure_preserves_unresolved_intent_and_excludes_active_owner(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as directory:
   root=Path(directory);r=d.persist(root,intent(),'INTENT');lock=d.OwnerLock(root,r['owner']);self.assertTrue(lock.acquire())
   try:
    with patch.object(d,'lookup') as lookup:self.assertEqual({'removed':0,'active':1,'unresolved':0},d.reconcile(root));lookup.assert_not_called()
   finally:lock.close()
   with patch.object(d,'lookup',side_effect=OSError('daemon down')):self.assertEqual(1,d.reconcile(root)['unresolved'])
   data=json.loads(next(root.glob('*.json')).read_bytes());self.assertEqual('INTENT',data['history'][-1]['state']);self.assertEqual(set(r),set(data))
 def test_malformed_or_retargeted_journal_cannot_issue_docker_operations(self):
  with tempfile.TemporaryDirectory(dir=ROOT/'.test-tmp') as directory:
   root=Path(directory);r=d.persist(root,intent(),'INTENT');r['name']='unrelated';next(root.glob('*.json')).write_bytes(json.dumps(r).encode())
   with patch.object(d,'cli') as cli,self.assertRaises(ValueError):d.reconcile(root)
   cli.assert_not_called()
 def test_ipc_rejects_oversized_duplicate_or_nonfinite_frames(self):
  for raw in [struct.pack('!I',300001),struct.pack('!I',13)+b'{"x":1,"x":2}',struct.pack('!I',9)+b'{"x":NaN}']:
   a,b=socket.socketpair()
   try:
    a.sendall(raw)
    with self.assertRaises(ValueError):docker_rpc.read_frame(b,300000,1)
   finally:a.close();b.close()
 def test_request_pieces_and_container_group_cannot_be_arbitrary_commands(self):
  session=d.Session(ROOT,ROOT/'.test-tmp',uuid.uuid4().hex,threading.Event())
  with self.assertRaises(ValueError):session.prepare('../../image')
  for pieces in [[],['word'],['word','wrong-eos'],['word','</s>','extra']]:
   with self.assertRaises(ValueError):session.native(pieces)
 def test_supervised_profiles_append_without_reusing_older_keys(self):
  registry=shona_supervised.profiles();base=base_profiles();self.assertEqual(5,len(registry));self.assertFalse({p['key'] for p in registry.values()}&{p['key'] for p in base.values()})
  self.assertEqual(registry,json.loads((ROOT/'ai/shona-supervised-profiles.json').read_bytes()))
  for value in registry.values():
   c=value['manifest']['configuration'];self.assertEqual(base[c['baseProfile']]['key'],c['baseProfileKey']);self.assertEqual('none',c['containerLogs']);self.assertEqual('host-guardian-v1',c['containerLifecycle'])
  with patch.object(shona_supervised,'sha',return_value='0'*64),self.assertRaisesRegex(ValueError,'implementation changed'):shona_supervised.profiles()
if __name__=='__main__':unittest.main()
