"""Separate versioned joint/region profiles; previously published perception identities stay intact."""
import json
import os
from pathlib import Path
from perception import canonical,sha,resize
from perception_formats import png_rgba

ROOT=Path(__file__).resolve().parents[1]
TASKS=('JOINT_IMAGE','JOINT_TEXT','DETECTION','SEGMENTATION')

def profiles():
    lock=json.loads((ROOT/'ai/vision.lock.json').read_bytes())
    if sha((ROOT/'ai/runtime.lock.json').read_bytes())!=lock['runtimeLockSha256']:raise ValueError('Runtime profile changed')
    for name,digest in lock['sources'].items():
        if sha((ROOT/name).read_bytes())!=digest:raise ValueError('Vision implementation changed')
    def artifacts(group):return [{k:f[k] for k in ('repository','revision','path','bytes','sha256')} for f in lock['files'] if f['group']==group]
    shared={'artifacts':artifacts('JOINT'),'configuration':lock['jointSpace'],'sources':lock['sources'],'runtimeLockSha256':lock['runtimeLockSha256']}
    result={}
    for task in TASKS:
        group='JOINT' if task.startswith('JOINT_') else task
        manifest={'schema':'perception-profile-v1','task':task,'provider':'onnxruntime-cpu','runtimeLockSha256':lock['runtimeLockSha256'],
                  'sources':lock['sources'],'configuration':lock['configuration'][task],'artifacts':artifacts(group)}
        if group=='JOINT':manifest['sharedSpace']={'key':sha(canonical(shared)),'definition':shared}
        result[task]={'key':sha(canonical(manifest)),'manifest':manifest}
    return result

class VisionProvider:
    def __init__(self,task,directory):
        if task not in TASKS:raise ValueError('Unsupported task')
        self.task=task;self.profile=profiles()[task];group='JOINT' if task.startswith('JOINT_') else task
        self.model={'JOINT_IMAGE':'joint-image.onnx','JOINT_TEXT':'joint-text.onnx','DETECTION':'detector.onnx','SEGMENTATION':'segmenter.onnx'}[task]
        files={};lock=json.loads((ROOT/'ai/vision.lock.json').read_bytes())
        for f in lock['files']:
            if f['group']!=group:continue
            data=(Path(directory)/f['local']).read_bytes()
            if len(data)!=f['bytes'] or sha(data)!=f['sha256']:raise ValueError('Vision artifact integrity failed')
            if not f['local'].endswith('.onnx') or f['local']==self.model:files[f['local']]=data
        os.environ['ORT_DISABLE_TELEMETRY']='1';os.environ['HF_HUB_OFFLINE']='1';os.environ['HF_HUB_DISABLE_TELEMETRY']='1'
        from integrity import verify_runtime
        verify_runtime()
        import numpy as np
        import onnxruntime as ort
        self.np=np;ort.disable_telemetry_events();options=ort.SessionOptions();options.intra_op_num_threads=2;options.inter_op_num_threads=1
        options.execution_mode=ort.ExecutionMode.ORT_SEQUENTIAL
        self.session=ort.InferenceSession(files[self.model],sess_options=options,providers=['CPUExecutionProvider']);self.session.disable_fallback()
        if self.session.get_providers()!=['CPUExecutionProvider']:raise ValueError('Unsupported execution provider')
        if task=='JOINT_TEXT':
            from tokenizers import Tokenizer
            self.tokenizer=Tokenizer.from_str(files['joint-tokenizer.json'].decode());self.tokenizer.enable_truncation(max_length=512)

    def run(self,inputs):
        required={i.name for i in self.session.get_inputs()}
        if not required<=inputs.keys():raise ValueError('Unexpected graph inputs')
        outputs=self.session.run(None,{k:v for k,v in inputs.items() if k in required})
        if not all(self.np.isfinite(v).all() for v in outputs):raise ValueError('Nonfinite graph output')
        return dict(zip((o.name for o in self.session.get_outputs()),outputs))

    def joint(self,output):
        np=self.np;vector=output.reshape(-1).astype(np.float64)
        if vector.shape!=(768,):raise ValueError('Unsupported joint output')
        norm=float(np.linalg.norm(vector))
        if not np.isfinite(norm) or norm<=0:raise ValueError('Invalid joint vector')
        return {'vector':(vector/norm).tolist(),'spaceKey':self.profile['manifest']['sharedSpace']['key'],'representation':'joint-embedding-v1'}

    def infer(self,data):
        np=self.np
        if self.task=='JOINT_TEXT':
            if len(data)>32000:raise ValueError('Text exceeds bound')
            text=data.decode('utf-8')
            if not text.strip() or len(text)>8000:raise ValueError('Text input unavailable')
            encoded=self.tokenizer.encode(text)
            output=self.run({'input_ids':np.array([encoded.ids],dtype=np.int64),'attention_mask':np.array([encoded.attention_mask],dtype=np.int64),'token_type_ids':np.array([encoded.type_ids],dtype=np.int64)})
            return {**self.joint(output['text_embeds']),'tokenCount':len(encoded.ids),'truncated':bool(encoded.overflowing)}
        width,height,pixels=png_rgba(data);rgba=np.frombuffer(pixels,dtype=np.uint8).reshape(height,width,4).astype(np.float32)
        image=rgba[:,:,:3]*(rgba[:,:,3:4]/255)+255*(1-rgba[:,:,3:4]/255)
        if self.task=='JOINT_IMAGE':
            scale=224/min(height,width);h,w=round(height*scale),round(width*scale)
            if max(h,w)>4096:raise ValueError('Unsupported image aspect ratio')
            image=resize(np,image,h,w);y=(h-224)//2;x=(w-224)//2;image=image[y:y+224,x:x+224]/255
            tensor=((image-np.array([.48145466,.4578275,.40821073],np.float32))/np.array([.26862954,.26130258,.27577711],np.float32)).transpose(2,0,1)[None]
            result=self.joint(self.run({'pixel_values':tensor})['image_embeds'])
        elif self.task=='DETECTION':result=self.detect(image)
        else:result=self.segment(image)
        return {'width':width,'height':height,**result}

    def detect(self,image):
        np=self.np;h,w=image.shape[:2];tensor=np.rint(resize(np,image,300,300)).clip(0,255).astype(np.uint8)[None]
        outputs=self.run({'inputs':tensor});count=float(outputs['num_detections'].reshape(-1)[0])
        boxes=outputs['detection_boxes'].reshape(-1,4);scores=outputs['detection_scores'].reshape(-1);labels=outputs['detection_classes'].reshape(-1)
        if count!=int(count) or count<0 or count>min(len(boxes),len(scores),len(labels),1000):raise ValueError('Invalid detection count')
        detections=[]
        for i in np.argsort(-scores[:int(count)]):
            score=float(scores[i]);label=float(labels[i])
            if not 0<=score<=1 or label!=int(label) or not 1<=label<=90:raise ValueError('Invalid detection output')
            if score<.5:continue
            y0,x0,y1,x1=np.clip(boxes[i],0,1);box=[int(np.floor(x0*w)),int(np.floor(y0*h)),int(np.ceil(x1*w)),int(np.ceil(y1*h))]
            if box[0]>=box[2] or box[1]>=box[3]:continue
            detections.append({'box':box,'classId':int(label),'modelScore':score})
        return {'detections':detections[:20],'truncated':len(detections)>20,'vocabulary':'coco-90','representation':'ssd-coco-boxes-v1'}

    def segment(self,image):
        np=self.np;image=resize(np,image,256,256)/255
        tensor=((image-np.array([.485,.456,.406],np.float32))/np.array([.229,.224,.225],np.float32)).transpose(2,0,1)[None]
        output=self.run({'input':tensor})['out']
        if output.shape!=(1,21,256,256):raise ValueError('Unexpected segmentation output')
        labels=output[0].argmax(axis=0);indices=np.arange(64)*4+2;grid=labels[indices[:,None],indices[None,:]].reshape(-1)
        runs=[]
        for value in grid:
            label=int(value)
            if runs and runs[-1][0]==label:runs[-1][1]+=1
            else:runs.append([label,1])
        return {'maskWidth':64,'maskHeight':64,'runs':runs,'vocabulary':'pascal-voc-21','representation':'semantic-mask-rle-v1'}
