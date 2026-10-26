"""Append-only local language alternatives; no implicit default or policy authority."""
from contextlib import ExitStack
import hashlib
import json
import os
from pathlib import Path
from generative import GenerativeProvider,chat_tokens
from perception import canonical,sha
ROOT=Path(__file__).resolve().parents[1]

def profiles():
    lock=json.loads((ROOT/'ai/quality.lock.json').read_bytes())
    if sha((ROOT/'ai/generative-runtime.lock.json').read_bytes())!=lock['runtimeLockSha256']:raise ValueError('Quality runtime identity changed')
    runtime=json.loads((ROOT/'ai/generative-runtime.lock.json').read_bytes())
    if sha((ROOT/'ai/runtime.lock.json').read_bytes())!=runtime['baseRuntimeLockSha256']:raise ValueError('Base runtime identity changed')
    for name,digest in lock['sources'].items():
        if sha((ROOT/name).read_bytes())!=digest:raise ValueError('Quality implementation changed')
    result={}
    for name,config in lock['configuration'].items():
        manifest={'schema':'perception-profile-v1','task':config['task'],'provider':'onnxruntime-cpu','runtimeLockSha256':lock['runtimeLockSha256'],'sources':lock['sources'],'configuration':config,'artifacts':[{k:f[k] for k in ('repository','revision','path','bytes','sha256','role')} for f in lock['files'] if f['group']==name]}
        result[name]={'key':sha(canonical(manifest)),'manifest':manifest}
    return result

def evaluation():
    lock=json.loads((ROOT/'ai/quality.lock.json').read_bytes());raw=(ROOT/'ai/quality-evaluation.json').read_bytes()
    if sha(raw)!=lock['evaluationSha256']:raise ValueError('Evaluation fixture identity changed')
    fixture=json.loads(raw);cases=fixture['cases'];registry=profiles()
    if not cases or len({c['id'] for c in cases})!=len(cases) or any(c['profile'] not in registry for c in cases):raise ValueError('Invalid evaluation scope')
    return fixture

def draft_scores(logits,history,penalty):
    scores=logits.copy()
    for token in set(history):scores[token]=scores[token]/penalty if scores[token]>0 else scores[token]*penalty
    if history:
        for first,second in zip(history,history[1:]):
            if first==history[-1]:scores[second]=-float('inf')
    return scores

class QualityProvider(GenerativeProvider):
    def __init__(self,name,directory):
        registry=profiles()
        if name not in registry:raise ValueError('Unsupported quality profile')
        self.name=name;self.profile=registry[name];self.task=self.profile['manifest']['task'];self.config=self.profile['manifest']['configuration'];files={};models={}
        for entry in json.loads((ROOT/'ai/quality.lock.json').read_bytes())['files']:
            if entry['group']!=name:continue
            path=Path(directory)/entry['local']
            if path.suffix=='.onnx':
                with path.open('rb') as stream:
                    if os.fstat(stream.fileno()).st_size!=entry['bytes'] or hashlib.file_digest(stream,'sha256').hexdigest()!=entry['sha256']:raise ValueError('Quality artifact integrity failed')
                models[entry['role']]=(path,entry)
            else:
                data=path.read_bytes()
                if len(data)!=entry['bytes'] or sha(data)!=entry['sha256']:raise ValueError('Quality artifact integrity failed')
                files[entry['role']]=data
        from generative_integrity import verify_runtime
        verify_runtime()
        import numpy as np
        import onnxruntime as ort
        os.environ.update(ORT_DISABLE_TELEMETRY='1',HF_HUB_OFFLINE='1',HF_HUB_DISABLE_TELEMETRY='1');ort.disable_telemetry_events()
        from generative_model import locked_model
        self.np=np;self._locks=ExitStack();self.sessions={};options=ort.SessionOptions();options.intra_op_num_threads=2;options.inter_op_num_threads=1;options.execution_mode=ort.ExecutionMode.ORT_SEQUENTIAL
        for role,(path,entry) in models.items():
            session=ort.InferenceSession(self._locks.enter_context(locked_model(path,entry)),sess_options=options,providers=['CPUExecutionProvider']);session.disable_fallback()
            if session.get_providers()!=['CPUExecutionProvider']:raise ValueError('Unsupported execution provider')
            self.sessions[role]=session
        self.model_config=json.loads(files['config.json'])
        if self.task=='LLM':
            from tokenizers import Tokenizer
            self.tokenizer=Tokenizer.from_str(files['tokenizer.json'].decode('utf-8'));self.tokenizer.no_padding();self.tokenizer.no_truncation()
        else:
            import sentencepiece as spm
            self.source_sp=spm.SentencePieceProcessor(model_proto=files['source.spm']);self.target_sp=spm.SentencePieceProcessor(model_proto=files['target.spm']);self.vocab=json.loads(files['vocab.json']);self.inverse={value:key for key,value in self.vocab.items()}
            if len(self.inverse)!=len(self.vocab) or len(self.vocab)!=self.model_config['vocab_size']:raise ValueError('Invalid Marian vocabulary')

    def draft(self,prompt,source):
        np=self.np;ids,instruction_cut,context_cut=chat_tokens(self.tokenizer,prompt,source);session=self.sessions['llm.onnx'];config=self.model_config
        past={i.name:np.zeros((1,config['num_key_value_heads'],0,config['hidden_size']//config['num_attention_heads']),np.float32) for i in session.get_inputs() if i.name.startswith('past_key_values.')}
        outputs=[o.name for o in session.get_outputs()];generated=[];current=ids;offset=0;ended=False
        for _ in range(64):
            result=self.run('llm.onnx',{'input_ids':np.array([current],np.int64),'attention_mask':np.ones((1,offset+len(current)),np.int64),'position_ids':np.arange(offset,offset+len(current),dtype=np.int64)[None],**past},outputs);logits=result['logits']
            if logits.shape!=(1,len(current),49152):raise ValueError('Invalid draft shape')
            token=int(np.argmax(draft_scores(logits[0,-1],generated,self.config['repetitionPenalty'])));generated.append(token)
            if token in (0,2):ended=True;break
            past={name:result[name.replace('past_key_values.','present.')] for name in past};offset+=len(current);current=[token]
        text=self.tokenizer.decode(generated,skip_special_tokens=True).strip()
        if not text or len(text.encode('utf-16-le'))//2>4096:raise ValueError('Draft unavailable')
        return {'text':text,'inputTokenCount':len(ids),'outputTokenCount':len(generated),'inputTruncated':instruction_cut or context_cut,'instructionTruncated':instruction_cut,'contextTruncated':context_cut,'outputTruncated':not ended,'finishReason':'EOS' if ended else 'OUTPUT_LIMIT','trust':'UNTRUSTED_DRAFT','representation':'smollm2-draft-v1'}

    def translate(self,source):
        np=self.np;c=self.model_config;eos=c['eos_token_id'];pad=c['pad_token_id'];unk=self.vocab['<unk>']
        pieces=self.source_sp.encode(source,out_type=str);cut=len(pieces)>127;tokens=[self.vocab.get(piece,unk) for piece in pieces[:127]]
        if not tokens or all(token==unk for token in tokens):raise ValueError('Source tokens unavailable')
        ids=[*tokens,eos];mask=np.ones((1,len(ids)),np.int64)
        hidden=self.run('encoder.onnx',{'input_ids':np.array([ids],np.int64),'attention_mask':mask},['last_hidden_state'])['last_hidden_state']
        generated=[c['decoder_start_token_id']];ended=False
        for _ in range(32):
            logits=self.run('decoder.onnx',{'input_ids':np.array([generated],np.int64),'encoder_hidden_states':hidden,'encoder_attention_mask':mask},['logits'])['logits']
            if logits.shape!=(1,len(generated),c['vocab_size']):raise ValueError('Invalid Marian shape')
            last=logits[0,-1].copy();last[pad]=-np.inf;token=int(np.argmax(last));generated.append(token)
            if token==eos:ended=True;break
            if token==unk or token not in self.inverse:raise ValueError('Generated vocabulary unavailable')
        text=self.target_sp.decode_pieces([self.inverse[token] for token in generated[1:] if token!=eos]).strip()
        if not text or len(text.encode('utf-16-le'))//2>2048:raise ValueError('Translation unavailable')
        return {'text':text,'sourceLanguage':self.config['sourceLanguage'],'targetLanguage':self.config['targetLanguage'],'inputTokenCount':len(ids),'inputUnknownTokenCount':tokens.count(unk),'outputTokenCount':len(generated)-1,'inputTruncated':cut,'outputTruncated':not ended,'finishReason':'EOS' if ended else 'OUTPUT_LIMIT','trust':'UNVERIFIED_TRANSLATION','representation':'marian-source-target-v1'}
