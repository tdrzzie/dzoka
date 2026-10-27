"""Bounded independent numeric features and unverified English span proposals."""
import json
import re
from contextlib import ExitStack

def hash_ngram(text):
    value=2166136261
    for byte in text.encode('utf-8'):
        value=((value^(byte if byte<128 else byte-256))*16777619)&0xffffffff
    return value

def features(text,vocabulary):
    ids=[]
    for word in [*filter(None,re.split(r'[ \t\n\v\f\r]+',text)),'</s>']:
        if word in vocabulary:ids.append(vocabulary[word])
        if word=='</s>':continue
        wrapped='<'+word+'>'
        for start in range(len(wrapped)):
            for size in range(2,6):
                if start+size<=len(wrapped):ids.append(634361+hash_ngram(wrapped[start:start+size])%1000000)
    if not ids or len(ids)>12000:raise ValueError('Language features unavailable')
    return ids

class LanguageIdentifier:
    def __init__(self,files,directory):
        from generative_model import locked_model
        import onnxruntime as ort
        self.stack=ExitStack()
        try:
            paths={entry['role']:self.stack.enter_context(locked_model(directory/entry['local'],entry)) for entry in files}
            config=json.loads(open(paths['lid-config.json'],'rb').read())
            if config!={'dim':256,'minn':2,'maxn':5,'bucket':1000000,'wordNgrams':1,'nwords':634361,'nlabels':2102}:raise ValueError('LID configuration')
            words=open(paths['lid-vocab.txt'],'r',encoding='utf-8').read().splitlines();self.words={w:i for i,w in enumerate(words)}
            self.labels=json.loads(open(paths['lid-labels.json'],'rb').read())
            if len(words)!=634361 or len(self.words)!=len(words) or len(self.labels)!=2102:raise ValueError('LID vocabulary')
            options=ort.SessionOptions();options.intra_op_num_threads=2;options.inter_op_num_threads=1;options.enable_mem_pattern=False
            self.session=ort.InferenceSession(paths['lid.onnx'],sess_options=options,providers=['CPUExecutionProvider'])
            if self.session.get_providers()!=['CPUExecutionProvider']:raise ValueError('LID backend')
        except Exception:self.stack.close();raise
    def close(self):
        self.session=None;self.stack.close()
    def score(self,text):
        import numpy as np
        result=self.session.run(['probs'],{'input_ids':np.asarray(features(text,self.words),dtype=np.int64)})[0].reshape(-1)
        if result.shape!=(2102,) or not np.isfinite(result).all() or np.min(result)<0 or np.max(result)>1 or abs(float(result.sum())-1)>.01:raise ValueError('LID probabilities unavailable')
        index=self.labels.index('__label__eng_Latn');score=float(result[index]);competitor=float(np.max(np.delete(result,index)))
        return score,score-competitor
    def propose(self,text):
        # At most 32 words and 512 UTF-16 units. Thresholds are policy, not calibration.
        prefix=text.encode('utf-16-le')[:1024].decode('utf-16-le',errors='ignore')
        all_words=list(re.finditer(r'[^\W_]+',prefix));words=all_words[:32];candidates=[]
        scores=[self.score(m.group()) if not any(c.isdigit() for c in m.group()) else None for m in words]
        for i,word in enumerate(words):
            for end in range(i+1,min(i+3,len(words))+1):
                group=words[i:end]
                if any(not text[a.end():b.start()].isspace() for a,b in zip(group,group[1:])):continue
                accepted=[s for s in scores[i:end] if s is not None]
                if not accepted or any(s<.6 or m<.15 for s,m in accepted):continue
                quote=text[word.start():group[-1].end()];score,margin=self.score(quote)
                if score>=.85 and margin>=.2:candidates.append((word.start(),group[-1].end(),score,margin))
        selected=[];previous=0
        for start,end,score,margin in sorted(candidates,key=lambda c:(c[0],-(c[1]-c[0]))):
            if start<previous or len(selected)>=8:continue
            quote=text[start:end]
            if len(quote.encode('utf-16-le'))//2>256:continue
            selected.append({'start':start,'end':end,'quote':quote,'modelScore':score,'modelMargin':margin});previous=end
        scanned=words[-1].end() if len(all_words)>32 else len(prefix)
        return selected,len(text[:scanned].encode('utf-16-le'))//2,scanned<len(text)
