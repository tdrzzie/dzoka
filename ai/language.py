"""Offline bounded generation and cross-encoder ranking; no generated policy or fallback facts."""
import json
import os
import re
from pathlib import Path
from perception import canonical,sha

ROOT=Path(__file__).resolve().parents[1]
TASKS=('ATTRIBUTES','TRANSLATION','RERANK')
ATTRIBUTE_PROMPT='{question}'
TRANSLATION_PROMPT='{source}'
QUESTIONS={'category':'What item was lost or found?','brand':'What is the brand of the item?',
           'colour':'What colour is the item?','location':'Where was the item lost or found?',
           'time':'When was the item lost or found?'}

def profiles():
    lock=json.loads((ROOT/'ai/language.lock.json').read_bytes())
    if sha((ROOT/'ai/runtime.lock.json').read_bytes())!=lock['runtimeLockSha256']:raise ValueError('Runtime identity changed')
    for name,digest in lock['sources'].items():
        if sha((ROOT/name).read_bytes())!=digest:raise ValueError('Language implementation changed')
    result={}
    for task in TASKS:
        group={'RERANK':'RERANK','ATTRIBUTES':'QA','TRANSLATION':'MARIAN'}[task]
        manifest={'schema':'perception-profile-v1','task':task,'provider':'onnxruntime-cpu','runtimeLockSha256':lock['runtimeLockSha256'],
                  'sources':lock['sources'],'configuration':lock['configuration'][task],
                  'artifacts':[{k:f[k] for k in ('repository','revision','path','bytes','sha256')} for f in lock['files'] if f['group']==group]}
        result[task]={'key':sha(canonical(manifest)),'manifest':manifest}
    return result

def grounded(source,answer,truncated):
    """One whole source span only. No generated or normalized identifier is accepted."""
    missing={'state':'UNAVAILABLE','reason':'NO_GROUNDED_SPAN','outputTruncated':truncated}
    answer=answer.strip()
    if truncated or not answer or len(answer)>128:return missing
    matches=list(re.finditer(r'(?<!\w)'+re.escape(answer)+r'(?!\w)',source,re.IGNORECASE))
    if len(matches)!=1:return missing
    match=matches[0];start,end=match.span()
    return {'state':'AVAILABLE','quote':source[start:end],'start':len(source[:start].encode('utf-16-le'))//2,
            'end':len(source[:end].encode('utf-16-le'))//2,'outputTruncated':False}

COLOURS={'black','white','blue','green','red','yellow','orange','purple','pink','brown','grey','gray','silver','gold','beige','navy','cream'}
MODIFIERS={'light','dark','pale','bright','metallic','off'}
TEMPORAL=r'\b(?:morning|afternoon|evening|night|noon|midnight|today|yesterday|tomorrow|week|month|year|monday|tuesday|wednesday|thursday|friday|saturday|sunday)\b'
LOCATION=r'\b(?:at|in|near|inside|outside|by|from|around)\s+(?:(?:the|a|an)\s+)?'
QUALIFIER=r'\b(?:around|about|approximately|roughly|before|after|between)\s+$'
def valid_field(field,answer):
    if field=='time':return bool(re.search(r'\d',answer) or re.search(TEMPORAL,answer,re.IGNORECASE))
    if field=='colour':
        words=re.split(r'[\s/-]+',answer.lower().strip())
        return bool(set(words)&COLOURS) and all(word in COLOURS|MODIFIERS for word in words)
    return True

class LanguageProvider:
    def __init__(self,task,directory):
        if task not in TASKS:raise ValueError('Unsupported language task')
        self.task=task;self.profile=profiles()[task];group={'RERANK':'RERANK','ATTRIBUTES':'QA','TRANSLATION':'MARIAN'}[task];files={}
        for f in json.loads((ROOT/'ai/language.lock.json').read_bytes())['files']:
            if f['group']!=group:continue
            data=(Path(directory)/f['local']).read_bytes()
            if len(data)!=f['bytes'] or sha(data)!=f['sha256']:raise ValueError('Language artifact integrity failed')
            files[f['local']]=data
        os.environ.update(ORT_DISABLE_TELEMETRY='1',HF_HUB_OFFLINE='1',HF_HUB_DISABLE_TELEMETRY='1')
        from integrity import verify_runtime
        verify_runtime()
        import numpy as np
        import onnxruntime as ort
        from tokenizers import Tokenizer
        self.np=np;ort.disable_telemetry_events();options=ort.SessionOptions();options.intra_op_num_threads=2;options.inter_op_num_threads=1;options.execution_mode=ort.ExecutionMode.ORT_SEQUENTIAL
        self.sessions={}
        names={'RERANK':('reranker',),'ATTRIBUTES':('extractor',),'TRANSLATION':('encoder','decoder')}[task]
        for name in names:
            session=ort.InferenceSession(files[('marian-' if task=='TRANSLATION' else '')+name+'.onnx'],sess_options=options,providers=['CPUExecutionProvider']);session.disable_fallback()
            if session.get_providers()!=['CPUExecutionProvider']:raise ValueError('Unsupported provider')
            self.sessions[name]=session
        self.config=json.loads(files[{'RERANK':'rerank-config.json','ATTRIBUTES':'qa-config.json','TRANSLATION':'marian-config.json'}[task]])
        self.tokenizer=Tokenizer.from_str(files[{'RERANK':'rerank-tokenizer.json','ATTRIBUTES':'qa-tokenizer.json','TRANSLATION':'marian-tokenizer.json'}[task]].decode('utf-8'))
        self.tokenizer.no_padding()
        self.tokenizer.enable_truncation(max_length=512 if task=='RERANK' else 256,strategy='only_second' if task=='ATTRIBUTES' else 'longest_first')

    def run(self,name,inputs,output):
        session=self.sessions[name];required={i.name for i in session.get_inputs()}
        if not required<=inputs.keys():raise ValueError('Unexpected language graph inputs')
        multiple=isinstance(output,list);names=output if multiple else [output]
        values=session.run(names,{k:v for k,v in inputs.items() if k in required})
        if not all(self.np.isfinite(value).all() for value in values):raise ValueError('Nonfinite language graph output')
        return dict(zip(names,values)) if multiple else values[0]

    def generate(self,source,maximum):
        np=self.np;encoded=self.tokenizer.encode(source);ids=np.array([encoded.ids],np.int64);mask=np.array([encoded.attention_mask],np.int64)
        hidden=self.run('encoder',{'input_ids':ids,'attention_mask':mask},'last_hidden_state');generated=[self.config['decoder_start_token_id']];ended=False
        heads=self.config['decoder_attention_heads'];width=self.config['d_model']//heads
        empty={i.name:np.zeros((1,heads,0,width),np.float32) for i in self.sessions['decoder'].get_inputs() if i.name.startswith('past_key_values.')}
        for _ in range(maximum):
            logits=self.run('decoder',{'input_ids':np.array([generated],np.int64),'encoder_hidden_states':hidden,'encoder_attention_mask':mask,'use_cache_branch':np.array([False]),**empty},'logits')
            if logits.shape!=(1,len(generated),self.config['vocab_size']):raise ValueError('Invalid generation shape')
            last=logits[0,-1].copy();last[self.config['pad_token_id']]=-np.inf;token=int(np.argmax(last));generated.append(token)
            if token==self.config['eos_token_id']:ended=True;break
        return self.tokenizer.decode(generated,skip_special_tokens=True).strip(),len(encoded.ids),bool(encoded.overflowing),not ended

    def extract(self,source,question,field):
        np=self.np;encoded=self.tokenizer.encode(question,source)
        outputs=self.run('extractor',{'input_ids':np.array([encoded.ids],np.int64),'attention_mask':np.array([encoded.attention_mask],np.int64),'token_type_ids':np.array([encoded.type_ids],np.int64)},['start_logits','end_logits'])
        if any(value.shape!=(1,len(encoded.ids)) for value in outputs.values()):raise ValueError('Invalid span shape')
        starts=outputs['start_logits'][0];ends=outputs['end_logits'][0];best=None;score=float(starts[0]+ends[0])
        for i,sequence in enumerate(encoded.sequence_ids):
            if sequence!=1 or encoded.offsets[i][1]<=encoded.offsets[i][0]:continue
            for j in range(i,min(i+16,len(encoded.ids))):
                if encoded.sequence_ids[j]!=1:break
                value=float(starts[i]+ends[j])
                if value>score:best=(encoded.offsets[i][0],encoded.offsets[j][1]);score=value
        if best and field=='location' and not(re.search(LOCATION+'$',source[:best[0]],re.IGNORECASE) or re.match(LOCATION,source[best[0]:best[1]],re.IGNORECASE)):best=None
        if best and field=='time':
            qualifier=re.search(QUALIFIER,source[:best[0]],re.IGNORECASE)
            while qualifier:
                best=(qualifier.start(),best[1]);qualifier=re.search(QUALIFIER,source[:best[0]],re.IGNORECASE)
        answer=source[best[0]:best[1]] if best else ''
        value=grounded(source,answer,False) if valid_field(field,answer) else grounded(source,'',False)
        if field=='time' and value['state']=='AVAILABLE':value['uncertainty']='UNINTERPRETED'
        return value,len(encoded.ids),bool(encoded.overflowing)

    def infer(self,data):
        if self.task=='RERANK':return self.rerank(data)
        if len(data)>32000:raise ValueError('Text exceeds bound')
        source=data.decode('utf-8')
        if not source.strip() or len(source)>8000:raise ValueError('Text unavailable')
        if self.task=='TRANSLATION':
            answer,count,truncated,output=self.generate(TRANSLATION_PROMPT.format(source=source),64)
            if not answer or len(answer.encode('utf-16-le'))//2>2048:raise ValueError('Translation unavailable')
            return {'text':answer,'sourceLanguage':'fr','targetLanguage':'en','inputTokenCount':count,'inputTruncated':truncated,'outputTruncated':output,'representation':'opus-fr-en-greedy-v1'}
        fields={};count=0;truncated=False
        for field,question in QUESTIONS.items():
            value,tokens,cut=self.extract(source,question,field)
            fields[field]=value;count=max(count,tokens);truncated|=cut
        return {'fields':fields,'inputTokenCount':count,'inputTruncated':truncated,'representation':'qa-source-spans-v1'}

    def rerank(self,data):
        if len(data)>65536:raise ValueError('Rerank payload exceeds bound')
        body=json.loads(data);query=body['query'];candidates=body['candidates'];seen=set();results=[];np=self.np
        if not isinstance(query,str) or not query.strip() or len(query)>8000 or not 1<=len(candidates)<=8:raise ValueError('Invalid rerank input')
        for candidate in candidates:
            id=candidate['id'];text=candidate['text']
            if id in seen or not isinstance(text,str) or not text.strip() or len(text)>512:raise ValueError('Invalid candidate')
            seen.add(id);encoded=self.tokenizer.encode(query,text)
            logits=self.run('reranker',{'input_ids':np.array([encoded.ids],np.int64),'attention_mask':np.array([encoded.attention_mask],np.int64),'token_type_ids':np.array([encoded.type_ids],np.int64)},'logits')
            if logits.shape!=(1,1) or abs(float(logits[0,0]))>1000:raise ValueError('Invalid rerank output')
            results.append({'id':id,'rawScore':float(logits[0,0]),'inputTokenCount':len(encoded.ids),'inputTruncated':bool(encoded.overflowing)})
        results.sort(key=lambda x:(-x['rawScore'],x['id']))
        return {'candidates':results,'representation':'msmarco-raw-logit-v1'}
