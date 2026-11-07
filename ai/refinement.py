"""Explicit local alternatives. No network, downloads, fabricated evidence or promotion."""
import hashlib
import json
import os
from pathlib import Path
from perception import canonical,sha,resize
from perception_formats import png_rgba,pcm16

ROOT=Path(__file__).resolve().parents[1]
COCO_IDS=(1,2,3,4,5,6,7,8,9,10,11,13,14,15,16,17,18,19,20,21,22,23,24,25,27,28,31,32,33,34,35,36,37,38,39,40,41,42,43,44,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,67,70,72,73,74,75,76,77,78,79,80,81,82,84,85,86,87,88,89,90)
VOC={1:15,2:2,3:7,4:14,5:1,6:6,7:19,9:4,16:3,17:8,18:12,19:13,20:17,21:10,44:5,62:9,63:18,64:16,67:11,72:20}

def profiles():
    lock=json.loads((ROOT/'ai/refinement.lock.json').read_bytes())
    if sha((ROOT/'ai/generative-runtime.lock.json').read_bytes())!=lock['runtimeLockSha256']:raise ValueError('Refinement runtime changed')
    for name,digest in lock['sources'].items():
        if sha((ROOT/name).read_bytes())!=digest:raise ValueError('Refinement implementation changed')
    result={}
    for name,config in lock['configuration'].items():
        artifacts=[{k:f[k] for k in ('repository','revision','path','bytes','sha256')} for f in lock['files'] if f['group']==config['artifactGroup']]
        manifest={'schema':'perception-profile-v1','task':config['task'],'provider':'onnxruntime-cpu','runtimeLockSha256':lock['runtimeLockSha256'],'sources':lock['sources'],'configuration':config,'artifacts':artifacts}
        result[name]={'key':sha(canonical(manifest)),'manifest':manifest}
    return result

def rle(values):
    runs=[]
    for value in values:
        label=int(value)
        if runs and runs[-1][0]==label:runs[-1][1]+=1
        else:runs.append([label,1])
    return runs

def log_mel(np,audio,filters):
    if audio.ndim!=1 or not 400<=len(audio)<=160000 or not np.isfinite(audio).all() or filters.shape!=(80,201) or not np.isfinite(filters).all():raise ValueError('Bounded speech features required')
    if float(np.max(np.abs(audio)))<1e-5:raise ValueError('Speech signal unavailable')
    padded=np.pad(audio,(0,480000-len(audio)))
    centered=np.pad(padded,(200,200),mode='reflect')
    frames=np.lib.stride_tricks.sliding_window_view(centered,400)[::160][:-1]
    window=(.5-.5*np.cos(2*np.pi*np.arange(400)/400)).astype(np.float32)
    spectrum=np.fft.rfft(frames*window,axis=-1)
    power=(np.abs(spectrum)**2).astype(np.float32).T
    mel=np.log10(np.maximum(filters@power,1e-10))
    mel=np.maximum(mel,float(mel.max())-8)
    return ((mel+4)/4).astype(np.float32)[None]

def translation(provider,kind,config,source):
    np=provider.np;limit=config['maxInputTokens'];maximum=config['maxOutputTokens'];m2m=kind=='m2m'
    pieces=(provider.sp if m2m else provider.source_sp).encode(source,out_type=str)
    unknown=3 if m2m else provider.vocab['<unk>'];structural=2 if m2m else 1
    tokens=[provider.vocab.get(piece,unknown) for piece in pieces[:limit-structural]]
    if not tokens or all(t==unknown for t in tokens):raise ValueError('Source tokens unavailable')
    if m2m:
        ids=[provider.lang['__'+config['sourceLanguage']+'__'],*tokens,2];generated=[2,provider.lang['__'+config['targetLanguage']+'__']];encoder='multi-encoder.onnx';decoder='multi-decoder.onnx';eos=2;banned=[0,1,3,*provider.lang.values()]
    else:
        c=provider.model_config;ids=[*tokens,c['eos_token_id']];generated=[c['decoder_start_token_id']];encoder='encoder.onnx';decoder='decoder.onnx';eos=c['eos_token_id'];banned=[c['pad_token_id'],unknown]
    mask=np.ones((1,len(ids)),np.int64);hidden=provider.run(encoder,{'input_ids':np.array([ids],np.int64),'attention_mask':mask},['last_hidden_state'])['last_hidden_state'];prefix=len(generated);ended=False
    for _ in range(maximum):
        logits=provider.run(decoder,{'input_ids':np.array([generated],np.int64),'encoder_hidden_states':hidden,'encoder_attention_mask':mask},['logits'])['logits']
        expected=provider.model_config['vocab_size']
        if logits.shape!=(1,len(generated),expected):raise ValueError('Invalid translation shape')
        scores=logits[0,-1].copy();scores[banned]=-np.inf
        if not np.isfinite(scores).any():raise ValueError('Generated vocabulary unavailable')
        token=int(np.argmax(scores));generated.append(token)
        if token==eos:ended=True;break
        if token not in provider.inverse:raise ValueError('Generated vocabulary unavailable')
    sp=provider.sp if m2m else provider.target_sp
    text=sp.decode_pieces([provider.inverse[t] for t in generated[prefix:] if t!=eos]).strip()
    if not text or '\0' in text or len(text.encode('utf-16-le'))//2>2048:raise ValueError('Translation unavailable')
    return {'text':text,'sourceLanguage':config['sourceLanguage'],'targetLanguage':config['targetLanguage'],'inputTokenCount':len(ids),'inputUnknownTokenCount':tokens.count(unknown),'outputTokenCount':len(generated)-prefix,'inputTruncated':len(pieces)>limit-structural,'outputTruncated':not ended,'finishReason':'EOS' if ended else 'OUTPUT_LIMIT','trust':'UNVERIFIED_TRANSLATION','representation':config['representation']}

class RefinementProvider:
    def __init__(self,name,directory):
        self.profile=profiles()[name];self.config=self.profile['manifest']['configuration'];self.task=self.profile['manifest']['task'];self.sessions={};self.delegate=None
        files={};lock=json.loads((ROOT/'ai/refinement.lock.json').read_bytes())
        for entry in lock['files']:
            if entry['group']!=self.config['artifactGroup']:continue
            path=Path(directory)/entry['local']
            with path.open('rb') as source:
                if os.fstat(source.fileno()).st_size != entry['bytes']:
                    raise ValueError('Refinement artifact changed')
                if self.task == 'MULTILINGUAL_TRANSLATION':
                    digest = hashlib.file_digest(source, 'sha256').hexdigest()
                else:
                    payload = source.read(entry['bytes'] + 1)
                    if len(payload) != entry['bytes']:
                        raise ValueError('Refinement artifact changed')
                    digest = sha(payload)
                    files[entry['local']] = payload
                if digest != entry['sha256']:
                    raise ValueError('Refinement artifact changed')
        if self.task=='MULTILINGUAL_TRANSLATION':
            if self.config['baseFamily']=='generative':
                from generative import GenerativeProvider
                self.delegate=GenerativeProvider(self.config['baseProfile'],directory)
            else:
                from quality import QualityProvider
                self.delegate=QualityProvider(self.config['baseProfile'],directory)
            if self.delegate.profile['key']!=self.config['delegateProfileKey']:
                self.delegate.close();raise ValueError('Translation base profile changed')
            self.np=self.delegate.np;return
        from generative_integrity import verify_runtime
        verify_runtime()
        import numpy as np
        import onnxruntime as ort
        self.np=np;os.environ.update(ORT_DISABLE_TELEMETRY='1',HF_HUB_OFFLINE='1',HF_HUB_DISABLE_TELEMETRY='1');ort.disable_telemetry_events()
        options=ort.SessionOptions();options.intra_op_num_threads=2;options.inter_op_num_threads=1;options.execution_mode=ort.ExecutionMode.ORT_SEQUENTIAL
        models = {'maskrcnn': 'maskrcnn.onnx'}
        if self.task == 'SPEECH':
            prefix = self.config.get('speechArtifactPrefix', 'whisper')
            models = {f'whisper-{role}': f'{prefix}-{role}.onnx' for role in ('encoder', 'decoder')}
        for model, filename in models.items():
            session=ort.InferenceSession(files[filename],sess_options=options,providers=['CPUExecutionProvider']);session.disable_fallback()
            if session.get_providers()!=['CPUExecutionProvider']:raise ValueError('Unsupported refinement runtime')
            self.sessions[model]=session
        if self.task=='SPEECH':
            from tokenizers import Tokenizer
            self.tokenizer=Tokenizer.from_str(files[f'{prefix}-tokenizer.json'].decode());self.tokenizer.no_truncation();self.tokenizer.no_padding()
            self.generation=json.loads(files[f'{prefix}-generation.json']);self.model_config=json.loads(files[f'{prefix}-config.json'])
            import io
            with np.load(io.BytesIO(files['whisper-mel.npz']),allow_pickle=False) as archive:self.filters=archive['mel_80'].copy()

    def close(self):
        self.sessions.clear()
        if self.delegate is not None:self.delegate.close();self.delegate=None

    def run(self,name,inputs):
        session=self.sessions[name];required={x.name for x in session.get_inputs()}
        if not required<=inputs.keys():raise ValueError('Refinement graph inputs unavailable')
        outputs=session.run(None,{k:v for k,v in inputs.items() if k in required})
        if not all(self.np.isfinite(x).all() for x in outputs):raise ValueError('Nonfinite refinement output')
        return dict(zip((x.name for x in session.get_outputs()),outputs))

    def infer(self,raw):
        if self.task=='MULTILINGUAL_TRANSLATION':
            if len(raw)>32000:raise ValueError('Text exceeds bound')
            source=raw.decode('utf-8')
            if not source.strip() or '\0' in source or len(source)>8000:raise ValueError('Source unavailable')
            return translation(self.delegate,self.config['decoderKind'],self.config,source)
        if self.task == 'SPEECH':
            if self.config.get('decoding') == 'cached-greedy-validated-cache-v1':
                from whisper_speech import transcribe
                return transcribe(self, raw, log_mel)
            return self.speech(raw)
        width,height,pixels=png_rgba(raw);np=self.np;rgba=np.frombuffer(pixels,np.uint8).reshape(height,width,4).astype(np.float32);image=rgba[:,:,:3]*(rgba[:,:,3:4]/255)+255*(1-rgba[:,:,3:4]/255)
        scale=min(800/min(height,width),1333/max(height,width));h,w=int(height*scale),int(width*scale)
        if min(h,w)<32:raise ValueError('Unsupported region aspect ratio')
        tensor=resize(np,image,h,w)[:,:,::-1]-np.array([102.9801,115.9465,122.7717],np.float32)
        padded=np.zeros((3,((h+31)//32)*32,((w+31)//32)*32),np.float32);padded[:,:h,:w]=tensor.transpose(2,0,1)
        out=self.run('maskrcnn',{'image':padded});boxes=out['6568'];labels=out['6570'];scores=out['6572'];masks=out['6887'];n=len(scores)
        if not 0<=n<=1000 or boxes.shape!=(n,4) or labels.shape!=(n,) or masks.shape!=(n,1,28,28):raise ValueError('Invalid region graph shape')
        proposals=[]
        for i in np.argsort(-scores,kind='stable'):
            score=float(scores[i]);label=int(labels[i])
            if not 0<=score<=1.000001 or float(labels[i])!=label or not 1<=label<=80:raise ValueError('Invalid region class/score')
            if score<.5:continue
            b=boxes[i].astype(np.float64);b[[0,2]]=np.clip(b[[0,2]],0,w);b[[1,3]]=np.clip(b[[1,3]],0,h)
            if b[0]>=b[2] or b[1]>=b[3]:continue
            proposals.append((COCO_IDS[label-1],min(score,1),b,masks[i,0]))
        kept=proposals[:20]
        if self.task=='DETECTION':
            values=[]
            for label,score,b,_ in kept:
                box=[max(0,int(np.floor(b[0]*width/w))),max(0,int(np.floor(b[1]*height/h))),min(width,int(np.ceil(b[2]*width/w))),min(height,int(np.ceil(b[3]*height/h)))]
                if box[0]<box[2] and box[1]<box[3]:values.append({'box':box,'classId':label,'modelScore':score})
            return {'width':width,'height':height,'detections':values,'truncated':len(proposals)>20,'vocabulary':'coco-90','representation':'maskrcnn-coco-boxes-v2'}
        # Same source sample locations as the historical FCN's 4j+2 grid.
        gx,gy=np.meshgrid((np.arange(64)*4+2.5)/256*w,(np.arange(64)*4+2.5)/256*h);grid=np.zeros((64,64),np.int64)
        for label,score,b,mask in kept:
            if label not in VOC:continue
            if float(mask.min())<0 or float(mask.max())>1.000001:raise ValueError('Invalid ROI probabilities')
            x=(gx-b[0])/(b[2]-b[0])*28-.5;y=(gy-b[1])/(b[3]-b[1])*28-.5
            x0=np.floor(x).astype(np.int64);y0=np.floor(y).astype(np.int64);fx=x-x0;fy=y-y0
            probability=np.zeros((64,64),np.float32)
            for dx,dy,weight in [(0,0,(1-fx)*(1-fy)),(1,0,fx*(1-fy)),(0,1,(1-fx)*fy),(1,1,fx*fy)]:
                xx=x0+dx;yy=y0+dy;valid=(xx>=0)&(xx<28)&(yy>=0)&(yy<28)
                probability+=np.where(valid,mask[np.clip(yy,0,27),np.clip(xx,0,27)]*weight,0).astype(np.float32)
            selected=(gx>=b[0])&(gx<=b[2])&(gy>=b[1])&(gy<=b[3])&(probability>=.5)&(grid==0);grid[selected]=VOC[label]
        return {'width':width,'height':height,'maskWidth':64,'maskHeight':64,'runs':rle(grid.reshape(-1)),'vocabulary':'pascal-voc-21','representation':'maskrcnn-voc-union-rle-v2'}

    def speech(self,raw):
        np=self.np;pcm=pcm16(raw);samples=len(pcm)//2;audio=np.frombuffer(pcm,dtype='<i2').astype(np.float32)/32768
        features=log_mel(np,audio,self.filters);hidden=self.run('whisper-encoder',{'input_features':features})['last_hidden_state']
        if hidden.shape!=(1,1500,384):raise ValueError('Invalid speech encoder shape')
        c=self.generation;current=[c['decoder_start_token_id'],c['no_timestamps_token_id']];past={x.name:np.zeros((1,6,0,64),np.float32) for x in self.sessions['whisper-decoder'].get_inputs() if x.name.startswith('past_key_values.')};generated=[];ended=False
        for step in range(self.config['maxOutputTokens']):
            values=self.run('whisper-decoder',{'input_ids':np.array([current],np.int64),'encoder_hidden_states':hidden,'use_cache_branch':np.array([step>0]),**past});logits=values['logits']
            if logits.shape!=(1,len(current),51864):raise ValueError('Invalid speech decoder shape')
            scores=logits[0,-1].copy();scores[c['suppress_tokens']]=-np.inf;scores[c['no_timestamps_token_id']:]=-np.inf
            if not step:scores[c['begin_suppress_tokens']]=-np.inf
            if not np.isfinite(scores).any():raise ValueError('Speech tokens unavailable')
            token=int(np.argmax(scores));generated.append(token)
            if token==c['eos_token_id']:ended=True;break
            for name in past:
                value=values[name.replace('past_key_values.','present.')]
                if value.size:past[name]=value
            current=[token]
        if not ended:raise ValueError('Speech output limit reached')
        text=self.tokenizer.decode(generated,skip_special_tokens=True).strip()
        if not text or '\0' in text or len(text)>2048:raise ValueError('Speech output unavailable')
        return {'text':text,'samples':samples,'sampleRate':16000,'language':'en','representation':'whisper-en-greedy-v2'}
