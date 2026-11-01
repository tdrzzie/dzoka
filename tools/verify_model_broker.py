"""Real isolated conformance for every appended profile; no model bootstrap."""
import argparse
from datetime import datetime,timezone
import hashlib
import json
import os
from pathlib import Path
import sys
import struct
import time
import uuid

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'tools'));sys.path.insert(0,str(ROOT/'ai'))
from broker_bundle import verify_bundle,protect
from broker_profiles import profiles
from model_broker import infer,state_check
from docker_lifecycle import reconcile
from verify_worker_boundary import private_fixture
from perception_fixture import generate

SAMPLES={'en':'I lost a black bag.','fr':"J'ai perdu un sac noir.",'de':'Ich habe eine schwarze Tasche verloren.',
         'es':'Perdí una bolsa negra.','af':"Ek het 'n swart sak verloor.",
         'zu':'Ngilahlekelwe isikhwama esimnyama.','xh':'Ndilahlekelwe yingxowa emnyama.',
         'sn':'Ndarasa bhegi rangu.'}

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--profile',help='Explicit narrower diagnostic, never full acceptance')
    args=parser.parse_args();bundle=Path(os.environ['DZOKA_TEST_BROKER_BUNDLE']);receipt=os.environ['DZOKA_TEST_BROKER_SHA256']
    verify_bundle(bundle,receipt);registry=profiles()
    if registry!=json.loads((ROOT/'ai/broker-profiles.json').read_bytes()):raise ValueError('Isolated registry changed')
    run=ROOT/'.test-tmp'/('model-broker-'+uuid.uuid4().hex);run.mkdir()
    audio=run/'speech.wav';generate(audio)
    # Authored PNG using the installed Windows drawing API; no extra wheel.
    import subprocess
    image=run/'label.png'
    script="Add-Type -AssemblyName System.Drawing;$im=New-Object System.Drawing.Bitmap 512,128;$g=[System.Drawing.Graphics]::FromImage($im);$g.Clear([System.Drawing.Color]::White);$font=New-Object System.Drawing.Font 'Arial',36;$g.DrawString('BLUE BAG 742',$font,[System.Drawing.Brushes]::Black,20,35);$im.Save('"+str(image).replace("'","''")+"',[System.Drawing.Imaging.ImageFormat]::Png);$font.Dispose();$g.Dispose();$im.Dispose()"
    subprocess.run(['powershell.exe','-NoProfile','-Command',script],check=True,timeout=30,capture_output=True)
    # GDI adds ancillary display metadata. Match the actual sanitizer's narrow
    # RGBA8 input contract without weakening the model-side raster reader.
    data=image.read_bytes();position=8;canonical=data[:8]
    while position<len(data):
        size=struct.unpack_from('>I',data,position)[0];kind=data[position+4:position+8]
        if kind in (b'IHDR',b'IDAT',b'IEND'):canonical+=data[position:position+size+12]
        position+=size+12
    from perception_formats import png_rgba
    png_rgba(canonical);image.write_bytes(canonical)
    result={'scope':'All isolated profiles, authored synthetic conformance; not representative quality',
            'bundleSha256':receipt,'startedAt':datetime.now(timezone.utc).isoformat(),'status':'IN_PROGRESS','rows':[]}
    target=run/'result.json'
    def save():target.write_bytes((json.dumps(result,sort_keys=True,indent=2)+'\n').encode())
    save()
    with private_fixture() as state:
        protect(state)
        for name in ('profiles','docker'):(state/name).mkdir();protect(state/name)
        try:
            for name,profile in registry.items():
                if args.profile and name!=args.profile:continue
                family=profile['family'];task=profile.get('manifest',{}).get('task')
                if task in ('VISION','LOCAL_FEATURES','OCR','JOINT_IMAGE','DETECTION','SEGMENTATION'):raw=image.read_bytes()
                elif task=='SPEECH':raw=audio.read_bytes()
                elif task=='RERANK':raw=json.dumps({'query':SAMPLES['en'],'candidates':[{'id':str(uuid.uuid4()),'text':'a black travel bag'},{'id':str(uuid.uuid4()),'text':'a red umbrella'}]}).encode()
                elif task=='LLM':raw=json.dumps({'prompt':'Describe the reported item in one sentence.','query':SAMPLES['en']}).encode()
                elif family=='shona-cpu':
                    from shona_cpu_runtime import SAMPLES as cpu_samples
                    raw=cpu_samples[profile['base']][0].encode()
                else:raw=SAMPLES[profile.get('manifest',{}).get('configuration',{}).get('sourceLanguage','fr' if task=='TRANSLATION' else 'en')].encode()
                start=time.monotonic();output=infer(bundle,receipt,state,name,raw,checked=True)
                row={'profile':name,'profileKey':profile['key'],'seconds':round(time.monotonic()-start,3),
                     'inputSha256':hashlib.sha256(raw).hexdigest(),'evidenceSha256':hashlib.sha256(json.dumps(output,sort_keys=True).encode()).hexdigest(),
                     'outputFields':sorted(output),'boundary':'actual token zero capabilities, no loopback exemption; documentation-network native capability denial'}
                if 'text' in output:row['syntheticText']=output['text']
                if 'vector' in output:row['dimensions']=len(output['vector'])
                result['rows'].append(row);save();print('PASS: '+name,flush=True)
            if not result['rows']:raise ValueError('No selected isolated profiles')
            state_check(state);resources=reconcile(state/'docker')
            if resources['active'] or resources['unresolved']:raise ValueError('Unresolved native resources')
            if any(json.loads(p.read_bytes())['state']!='REMOVED' for p in (state/'profiles').glob('offline-*.json')):raise ValueError('Unresolved model profiles')
            result.update(status='PASS',finishedAt=datetime.now(timezone.utc).isoformat(),profileCount=len(result['rows']),fullProfileSet=not bool(args.profile));save()
        except BaseException:
            result['status']='FAIL';save();raise
    print('PASS: isolated native inference and confirmed cleanup; '+str(target),flush=True)

if __name__=='__main__':main()
