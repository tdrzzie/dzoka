"""Offline CPU inference using only the explicitly reviewed artifact/profile. No default model."""
import hashlib
import json
import math
import os
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]

class ProviderUnavailable(Exception):
    pass

def digest(data):return hashlib.sha256(data).hexdigest()

def load_profile():
    lock=json.loads((ROOT/'ai/model.lock.json').read_text(encoding='utf-8'))
    if digest((ROOT/'ai/runtime.lock.json').read_bytes())!=lock['configuration']['runtimeLockSha256']:
        raise ProviderUnavailable('Runtime profile changed')
    if digest(Path(__file__).read_bytes())!=lock['configuration']['providerSourceSha256']:
        raise ProviderUnavailable('Provider implementation changed')
    if digest((ROOT/'ai/integrity.py').read_bytes())!=lock['configuration']['integritySourceSha256']:
        raise ProviderUnavailable('Integrity implementation changed')
    config=digest(json.dumps(lock['configuration'],sort_keys=True,separators=(',',':')).encode())
    if config!=lock['space']['configSha256']:raise ProviderUnavailable('Configuration identity mismatch')
    s=lock['space']
    identity='\n'.join(str(s[k]) for k in ['provider','model','artifactRevision','artifactSha256','tokenizerSha256','preprocessing','dimensions','metric','normalization','configSha256'])
    if digest(identity.encode())!=s['spaceKey']:raise ProviderUnavailable('Space identity mismatch')
    return lock

class TextEmbeddingProvider:
    def __init__(self,directory):
        try:
            self.profile=load_profile();directory=Path(directory)
            values={}
            for file in self.profile['files']:
                data=(directory/file['local']).read_bytes()
                if len(data)!=file['bytes'] or digest(data)!=file['sha256']:raise ProviderUnavailable('Artifact checksum mismatch')
                values[file['local']]=data
            os.environ['TOKENIZERS_PARALLELISM']='false'
            os.environ['HF_HUB_OFFLINE']='1';os.environ['HF_HUB_DISABLE_TELEMETRY']='1';os.environ['ORT_DISABLE_TELEMETRY']='1'
            from integrity import verify_runtime
            verify_runtime()
            import onnxruntime as ort
            import numpy as np
            from tokenizers import Tokenizer
            ort.disable_telemetry_events()
            if ort.__version__!='1.31.0' or np.__version__!='2.5.4':raise ProviderUnavailable('Runtime version mismatch')
            options=ort.SessionOptions();options.intra_op_num_threads=2;options.inter_op_num_threads=1
            options.execution_mode=ort.ExecutionMode.ORT_SEQUENTIAL
            self.session=ort.InferenceSession(values['model.onnx'],sess_options=options,providers=['CPUExecutionProvider'])
            self.session.disable_fallback()
            if self.session.get_providers()!=['CPUExecutionProvider']:raise ProviderUnavailable('Unexpected execution provider')
            self.tokenizer=Tokenizer.from_str(values['tokenizer.json'].decode('utf-8'))
            self.tokenizer.enable_truncation(max_length=128)
            self.np=np
        except ProviderUnavailable:raise
        except Exception:raise ProviderUnavailable('Local inference unavailable') from None

    def embed(self,text):
        if not isinstance(text,str) or not text.strip() or len(text)>8000:raise ProviderUnavailable('Input unavailable')
        try:
            encoding=self.tokenizer.encode(text)
            if encoding.overflowing:truncated=True
            else:truncated=False
            np=self.np
            inputs={'input_ids':np.array([encoding.ids],dtype=np.int64),
                'attention_mask':np.array([encoding.attention_mask],dtype=np.int64),
                'token_type_ids':np.array([encoding.type_ids],dtype=np.int64)}
            required={i.name for i in self.session.get_inputs()}
            if not required<=inputs.keys():raise ProviderUnavailable('Unsupported model input')
            output=self.session.run(None,{k:v for k,v in inputs.items() if k in required})[0]
            if output.shape!=(1,len(encoding.ids),384):raise ProviderUnavailable('Unexpected model output')
            mask=inputs['attention_mask'][...,None].astype(np.float64)
            pooled=(output.astype(np.float64)*mask).sum(axis=1)[0]/max(float(mask.sum()),1)
            norm=float(np.linalg.norm(pooled))
            if not math.isfinite(norm) or norm<=0:raise ProviderUnavailable('Invalid model output')
            vector=(pooled/norm).tolist()
            if len(vector)!=384 or not all(math.isfinite(v) for v in vector):raise ProviderUnavailable('Invalid model output')
            return {'vector':vector,'tokenCount':len(encoding.ids),'truncated':truncated}
        except ProviderUnavailable:raise
        except Exception:raise ProviderUnavailable('Local inference failed') from None
