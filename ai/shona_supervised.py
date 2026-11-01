"""Append supervised CPU profiles; preserve all earlier model/profile identities."""
import json
from pathlib import Path
from perception import canonical,sha
from shona_cpu import ShonaCpuProvider,profiles as base_profiles
from docker_rpc import call
ROOT=Path(__file__).resolve().parents[1]

def profiles():
    lock=json.loads((ROOT/'ai/shona-supervised.lock.json').read_bytes());base=base_profiles();result={}
    for path,digest in lock['sources'].items():
        if sha((ROOT/path).read_bytes())!=digest:raise ValueError('Supervised implementation changed')
    for name,config in lock['configuration'].items():
        old=base[config['baseProfile']]
        if old['key']!=config['baseProfileKey']:raise ValueError('Base CPU profile changed')
        manifest={**old['manifest'],'provider':'ctranslate2-openblas-supervised-cpu','sources':{**old['manifest']['sources'],**lock['sources']},'configuration':config}
        result[name]={'key':sha(canonical(manifest)),'manifest':manifest}
    return result

class SupervisedShonaProvider(ShonaCpuProvider):
    def __init__(self,name,directory):
        selected=profiles()[name];config=selected['manifest']['configuration']
        super().__init__(config['baseProfile'],directory)
        if self.profile['key']!=config['baseProfileKey']:self.close();raise ValueError('Base profile mismatch')
        self.profile=selected;self.config=config
    def check_image(self):
        if call('check').get('imageId')!=self.image:raise ValueError('Guardian image mismatch')
    def native(self,pieces):
        if getattr(self,'closed',False):raise ValueError('CPU provider closed')
        call('prepare',group=self.config['artifactGroup'])
        tokens=call('native',pieces=pieces).get('tokens')
        if not isinstance(tokens,list) or not 1<=len(tokens)<=32 or any(not isinstance(t,str) or not 1<=len(t.encode())<=1024 or '\0' in t for t in tokens):raise ValueError('Guardian token result unavailable')
        return tokens
