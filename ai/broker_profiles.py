"""Appended isolated profiles. Earlier provider identities remain immutable."""
import importlib
import json
from pathlib import Path
from perception import canonical,sha
ROOT=Path(__file__).resolve().parents[1]
FAMILIES={'perception':('perception','PerceptionProvider'),'vision':('vision','VisionProvider'),'language':('language','LanguageProvider'),'generative':('generative','GenerativeProvider'),'quality':('quality','QualityProvider'),'shona-cpu':('shona_cpu','ShonaCpuProvider'),'refinement':('refinement','RefinementProvider')}
MODEL_DIR={'text':'ai-model','perception':'perception-models','vision':'vision-models','language':'language-models','generative':'generative-models','quality':'quality-models','shona-cpu':'shona-cpu-models','refinement':'refinement-models'}

def verify():
    raw=(ROOT/'ai/broker.lock.json').read_bytes();lock=json.loads(raw)
    for name,digest in lock['sources'].items():
        if sha((ROOT/name).read_bytes())!=digest:raise ValueError('Broker implementation changed')
    return lock,sha(raw)

def profiles():
    lock,identity=verify();result={};boundary={'protocol':'model-pipe-v1','execution':'windows-appcontainer-no-capabilities','brokerLockSha256':identity,'runtime':'reviewed-22-wheel','nativeCpu':'owned-fixed-session-v1'}
    from provider import load_profile
    text=load_profile()['space'];config=sha(canonical({'baseSpaceKey':text['spaceKey'],'boundary':boundary}))
    space={**text,'provider':'onnxruntime-appcontainer-cpu','configSha256':config}
    fields=['provider','model','artifactRevision','artifactSha256','tokenizerSha256','preprocessing','dimensions','metric','normalization','configSha256']
    space['spaceKey']=sha('\n'.join(str(space[k]) for k in fields).encode())
    result['ISO_TEXT']={'family':'text','base':'TEXT','space':space,'key':space['spaceKey'],'boundary':boundary}
    for family,(module,_) in FAMILIES.items():
        for name,base in importlib.import_module(module).profiles().items():
            manifest={**base['manifest'],'provider':base['manifest']['provider']+'-appcontainer','sources':{**base['manifest']['sources'],**lock['sources']},'configuration':{**base['manifest']['configuration'],'baseProfileKey':base['key'],'brokerBoundary':boundary}}
            if 'sharedSpace' in manifest:
                definition={**manifest['sharedSpace']['definition'],'brokerBoundary':boundary}
                manifest['sharedSpace']={'key':sha(canonical(definition)),'definition':definition}
            key='ISO_'+family.replace('-','_').upper()+'_'+name
            result[key]={'family':family,'base':name,'key':sha(canonical(manifest)),'manifest':manifest}
    return result
