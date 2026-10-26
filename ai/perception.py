"""Independently implemented offline CPU profiles; all outputs remain derived evidence."""
import hashlib
import json
import math
import os
from pathlib import Path
from perception_formats import png_rgba,pcm16

ROOT=Path(__file__).resolve().parents[1]
TASKS=('VISION','LOCAL_FEATURES','OCR','SPEECH')

def sha(data):return hashlib.sha256(data).hexdigest()
def canonical(value):return json.dumps(value,sort_keys=True,separators=(',',':'),ensure_ascii=True).encode()

def profiles():
    lock=json.loads((ROOT/'ai/perception.lock.json').read_bytes())
    if sha((ROOT/'ai/runtime.lock.json').read_bytes())!=lock['runtimeLockSha256']:raise ValueError('Runtime lock changed')
    for name,digest in lock['sources'].items():
        if sha((ROOT/name).read_bytes())!=digest:raise ValueError('Perception implementation changed')
    result={}
    for task in TASKS:
        manifest={'schema':'perception-profile-v1','task':task,'provider':'numpy-harris' if task=='LOCAL_FEATURES' else 'onnxruntime-cpu',
                  'runtimeLockSha256':lock['runtimeLockSha256'],'sources':lock['sources'],'configuration':lock['configuration'][task],
                  'artifacts':[{k:f[k] for k in ('repository','revision','path','bytes','sha256')} for f in lock['files'] if f['task']==task]}
        result[task]={'key':sha(canonical(manifest)),'manifest':manifest}
    return result


def resize(np,image,height,width):
    """Fixed half-pixel bilinear interpolation, independently authored."""
    h,w=image.shape[:2];ys=np.clip((np.arange(height)+.5)*h/height-.5,0,h-1);xs=np.clip((np.arange(width)+.5)*w/width-.5,0,w-1)
    y0=ys.astype(np.int32);x0=xs.astype(np.int32);y1=np.minimum(y0+1,h-1);x1=np.minimum(x0+1,w-1)
    wy=(ys-y0)[:,None,None];wx=(xs-x0)[None,:,None]
    return ((image[y0[:,None],x0[None,:]]*(1-wx)+image[y0[:,None],x1[None,:]]*wx)*(1-wy)
            +(image[y1[:,None],x0[None,:]]*(1-wx)+image[y1[:,None],x1[None,:]]*wx)*wy).astype(np.float32)


class PerceptionProvider:
    def __init__(self,task,directory):
        if task not in TASKS:raise ValueError('Unsupported task')
        self.task=task;self.profile=profiles()[task];self.sessions={};self.directory=Path(directory)
        lock=json.loads((ROOT/'ai/perception.lock.json').read_bytes());files={}
        for f in lock['files']:
            if f['task']!=task:continue
            data=(self.directory/f['local']).read_bytes()
            if len(data)!=f['bytes'] or sha(data)!=f['sha256']:raise ValueError('Artifact integrity failed')
            files[f['local']]=data
        from integrity import verify_runtime
        verify_runtime()
        import numpy as np
        self.np=np
        if task!='LOCAL_FEATURES':
            import onnxruntime as ort
            ort.disable_telemetry_events();options=ort.SessionOptions();options.intra_op_num_threads=2;options.inter_op_num_threads=1
            options.execution_mode=ort.ExecutionMode.ORT_SEQUENTIAL
            for name,data in files.items():
                if not name.endswith('.onnx'):continue
                session=ort.InferenceSession(data,sess_options=options,providers=['CPUExecutionProvider']);session.disable_fallback()
                if session.get_providers()!=['CPUExecutionProvider']:raise ValueError('Unexpected execution provider')
                self.sessions[name]=session
        if task=='OCR':
            import yaml
            settings=yaml.safe_load(files['ocr-rec.yml'])['PostProcess']
            self.characters=['']+settings['character_dict']+[' ']
        if task=='SPEECH':self.vocabulary={int(v):k for k,v in json.loads(files['speech-vocab.json']).items()}

    def infer(self,data):
        np=self.np
        if self.task=='SPEECH':return self.speech(data)
        width,height,pixels=png_rgba(data);rgba=np.frombuffer(pixels,dtype=np.uint8).reshape(height,width,4).astype(np.float32)
        alpha=rgba[:,:,3:4]/255;image=rgba[:,:,:3]*alpha+255*(1-alpha)
        if self.task=='VISION':result=self.vision(image)
        elif self.task=='LOCAL_FEATURES':result=self.local(image)
        else:result=self.ocr(image)
        return {'width':width,'height':height,**result}

    def run(self,name,tensor):
        session=self.sessions[name];inputs=session.get_inputs()
        if len(inputs)!=1:raise ValueError('Unsupported graph input')
        result=session.run(None,{inputs[0].name:tensor.astype(self.np.float32)})[0]
        if not self.np.isfinite(result).all():raise ValueError('Nonfinite inference')
        return result

    def vision(self,image):
        np=self.np;h,w=image.shape[:2];scale=256/min(h,w);rh,rw=round(h*scale),round(w*scale)
        # Bound extreme aspect ratios before allocating a resized image.
        if rh>4096 or rw>4096:raise ValueError('Unsupported vision aspect ratio')
        image=resize(np,image,rh,rw);y=(rh-224)//2;x=(rw-224)//2;image=image[y:y+224,x:x+224]/255
        tensor=((image-np.array([.485,.456,.406],np.float32))/np.array([.229,.224,.225],np.float32)).transpose(2,0,1)[None]
        vector=self.run('vision.onnx',tensor).reshape(-1).astype(np.float64)
        if vector.shape!=(1000,):raise ValueError('Unexpected vision output')
        norm=float(np.linalg.norm(vector))
        if norm<=0:raise ValueError('Invalid visual signature')
        return {'vector':(vector/norm).tolist(),'representation':'resnet18-logits-l2-v1'}

    def local(self,image):
        np=self.np;h,w=image.shape[:2];scale=min(1,512/max(h,w));small=resize(np,image,max(1,round(h*scale)),max(1,round(w*scale)))
        gray=small.mean(axis=2)/255
        if min(gray.shape)<20:return {'points':[],'representation':'harris-patch4x4-v1'}
        gy,gx=np.gradient(gray)
        def box(a):
            p=np.pad(a,1,mode='edge');return sum(p[y:y+a.shape[0],x:x+a.shape[1]] for y in range(3) for x in range(3))/9
        a,b,c=box(gx*gx),box(gy*gy),box(gx*gy);score=a*b-c*c-.04*(a+b)**2
        score[:8]=0;score[-8:]=0;score[:,:8]=0;score[:,-8:]=0;best=float(score.max());points=[]
        if best<=1e-10:return {'points':points,'representation':'harris-patch4x4-v1'}
        for _ in range(16):
            y,x=np.unravel_index(np.argmax(score),score.shape)
            if score[y,x]<best*.01:break
            score[max(0,y-8):y+9,max(0,x-8):x+9]=0
            patch=gray[y-8:y+8,x-8:x+8].reshape(4,4,4,4).mean(axis=(1,3)).reshape(-1).astype(np.float64)
            patch-=patch.mean();norm=float(np.linalg.norm(patch))
            if norm<=1e-10:continue
            x0=max(0,int((x-8)/scale));y0=max(0,int((y-8)/scale));x1=min(w,int((x+8)/scale));y1=min(h,int((y+8)/scale))
            points.append({'box':[x0,y0,x1,y1],'descriptor':(patch/norm).tolist()})
        return {'points':points,'representation':'harris-patch4x4-v1'}

    def ocr(self,image):
        np=self.np;h,w=image.shape[:2];scale=min(1,736/max(h,w));rh=max(32,round(h*scale/32)*32);rw=max(32,round(w*scale/32)*32)
        resized=resize(np,image,rh,rw)[:,:,::-1]/255
        tensor=((resized-np.array([.485,.456,.406],np.float32))/np.array([.229,.224,.225],np.float32)).transpose(2,0,1)[None]
        probability=self.run('ocr-det.onnx',tensor).squeeze()
        if probability.shape!=(rh,rw):raise ValueError('Unexpected detector shape')
        # Bounded axis-aligned proposals, not claimed equivalent to polygon DB postprocessing.
        mask=probability>.2;ph=(-rh)%4;pw=(-rw)%4
        coarse=np.pad(mask,((0,ph),(0,pw))).reshape((rh+ph)//4,4,(rw+pw)//4,4).max(axis=(1,3))
        seen=set();boxes=[];ch,cw=coarse.shape
        for yy,xx in zip(*np.nonzero(coarse)):
            start=(int(yy),int(xx))
            if start in seen:continue
            stack=[start];seen.add(start);cells=[]
            while stack:
                y,x=stack.pop();cells.append((y,x))
                for ny,nx in ((y-1,x),(y+1,x),(y,x-1),(y,x+1)):
                    if 0<=ny<ch and 0<=nx<cw and coarse[ny,nx] and (ny,nx) not in seen:seen.add((ny,nx));stack.append((ny,nx))
            if len(cells)<3:continue
            ys,xs=zip(*cells);x0=max(0,(min(xs)*4-4)*w//rw);y0=max(0,(min(ys)*4-4)*h//rh)
            x1=min(w,((max(xs)+1)*4+4)*w//rw);y1=min(h,((max(ys)+1)*4+4)*h//rh)
            boxes.append((x0,y0,x1,y1))
            if len(boxes)>=16:break
        regions=[]
        for x0,y0,x1,y1 in sorted(boxes,key=lambda b:(b[1],b[0])):
            if x1<=x0 or y1<=y0:continue
            width=max(160,min(640,math.ceil((x1-x0)/(y1-y0)*48/32)*32));crop=resize(np,image[y0:y1,x0:x1],48,width)[:,:,::-1]
            output=self.run('ocr-rec.onnx',((crop/127.5-1).transpose(2,0,1))[None])
            if output.ndim!=3 or output.shape[0]!=1 or output.shape[2]!=len(self.characters):raise ValueError('Unexpected recognizer dictionary/shape')
            ids=output[0].argmax(axis=-1);values=output[0].max(axis=-1);text=[];scores=[];last=-1
            for index,value in zip(ids,values):
                index=int(index)
                if index and index!=last:text.append(self.characters[index]);scores.append(float(value))
                last=index
            value=''.join(text)[:256]
            if value:regions.append({'box':[x0,y0,x1,y1],'text':value,'modelScore':max(0,min(1,sum(scores)/len(scores)))})
        return {'regions':regions,'text':'\n'.join(r['text'] for r in regions)[:2048],'representation':'ppocr-v6-axis-ctc-v1'}

    def speech(self,data):
        np=self.np;raw=pcm16(data);wave=np.frombuffer(raw,dtype='<i2').astype(np.float32)/32768
        tensor=((wave-wave.mean())/np.sqrt(wave.var()+1e-7))[None]
        logits=self.run('speech.onnx',tensor)
        if logits.ndim!=3 or logits.shape[0]!=1 or logits.shape[2]!=len(self.vocabulary):raise ValueError('Unexpected speech logits')
        ids=logits[0].argmax(axis=-1);text=[];last=-1
        for index in ids:
            index=int(index)
            if index!=last and index not in (0,1,2,3):text.append(self.vocabulary[index])
            last=index
        return {'text':''.join(text).replace('|',' ').strip()[:2048],'samples':len(wave),'sampleRate':16000,'language':'en','representation':'wav2vec2-ctc-greedy-v1'}
