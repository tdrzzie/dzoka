"""Local-development-only directed Shona inference over exact reviewed bytes."""
import json
import os
from pathlib import Path
from perception import canonical,sha
ROOT=Path(__file__).resolve().parents[1]

def profiles():
    lock=json.loads((ROOT/'ai/shona.lock.json').read_bytes())
    if sha((ROOT/'ai/shona-runtime.lock.json').read_bytes())!=lock['runtimeLockSha256']:raise ValueError('Shona runtime changed')
    runtime=json.loads((ROOT/'ai/shona-runtime.lock.json').read_bytes())
    if sha((ROOT/'ai/generative-runtime.lock.json').read_bytes())!=runtime['parentRuntimeLockSha256']:raise ValueError('Parent runtime changed')
    parent=json.loads((ROOT/'ai/generative-runtime.lock.json').read_bytes())
    if sha((ROOT/'ai/runtime.lock.json').read_bytes())!=parent['baseRuntimeLockSha256']:raise ValueError('Base runtime changed')
    if runtime['deploymentScope']!='LOCAL_DEVELOPMENT_ONLY':raise ValueError('Unapproved Shona deployment scope')
    for name,digest in lock['sources'].items():
        if sha((ROOT/name).read_bytes())!=digest:raise ValueError('Shona implementation changed')
    result={}
    for name,config in lock['configuration'].items():
        group=config.get('artifactGroup',name)
        manifest={'schema':'perception-profile-v1','task':'MULTILINGUAL_TRANSLATION','provider':'ctranslate2-cpu','runtimeLockSha256':lock['runtimeLockSha256'],'sources':lock['sources'],'configuration':config,'artifacts':[{k:f[k] for k in ('repository','revision','path','bytes','sha256','role')} for f in lock['files'] if f['group']==group]}
        result[name]={'key':sha(canonical(manifest)),'manifest':manifest}
    return result

def evaluation():
    lock=json.loads((ROOT/'ai/shona.lock.json').read_bytes());raw=(ROOT/'ai/shona-evaluation.json').read_bytes()
    if sha(raw)!=lock['evaluationSha256']:raise ValueError('Shona evaluation fixture changed')
    fixture=json.loads(raw);cases=fixture['cases']
    if not cases or len({c['id'] for c in cases})!=len(cases) or any(c['profile'] not in profiles() for c in cases):raise ValueError('Invalid evaluation scope')
    return fixture

class ShonaProvider:
    def __init__(self,name,directory):
        if os.environ.get('DZOKA_AI_SHONA_LOCAL_DEVELOPMENT')!='true':raise ValueError('Explicit local development required')
        registry=profiles()
        if name not in registry:raise ValueError('Unsupported Shona profile')
        self.profile=registry[name];self.config=self.profile['manifest']['configuration'];files={};group=self.config.get('artifactGroup',name)
        for entry in json.loads((ROOT/'ai/shona.lock.json').read_bytes())['files']:
            if entry['group']!=group:continue
            data=(Path(directory)/entry['local']).read_bytes()
            if len(data)!=entry['bytes'] or sha(data)!=entry['sha256']:raise ValueError('Shona artifact integrity failed')
            files[entry['role']]=data
        from shona_integrity import verify_runtime
        verify_runtime()
        os.environ.update(CT2_USE_MKL='1',HF_HUB_OFFLINE='1',HF_HUB_DISABLE_TELEMETRY='1')
        import ctranslate2
        import sentencepiece as spm
        self.source_sp=spm.SentencePieceProcessor(model_proto=files['source.spm']);self.target_sp=spm.SentencePieceProcessor(model_proto=files['target.spm']);self.vocab=set(json.loads(files['shared_vocabulary.json']))
        self.translator=ctranslate2.Translator('reviewed-'+self.profile['key'],device='cpu',compute_type='int8_float32',inter_threads=1,intra_threads=2,files={k:files[k] for k in ('model.bin','config.json','shared_vocabulary.json')})
        if self.translator.device!='cpu' or self.translator.compute_type!='int8_float32':raise ValueError('Unapproved native backend')
    def close(self):
        if hasattr(self,'translator'):self.translator.unload_model()
    def __enter__(self):return self
    def __exit__(self,*_):self.close()
    def infer(self,data):
        if len(data)>32000:raise ValueError('Source exceeds bound')
        text=data.decode('utf-8')
        if not text.strip() or len(text)>8000 or '\0' in text:raise ValueError('Invalid Shona source')
        if self.config.get('protectionPolicy'):
            from shona_markup import protect,restore
            masked,spans=protect(text);evidence=self.translate(masked);output,copied=restore(evidence['text'],spans)
            if len(output.encode('utf-16-le'))//2>2048:raise ValueError('Preserved translation exceeds bound')
            evidence.update(text=output,representation='ctranslate2-marked-v1',preservedSpans=copied);return evidence
        return self.translate(text)
    def translate(self,text):
        pieces=self.source_sp.encode(text,out_type=str);cut=len(pieces)>127;pieces=pieces[:127];unknown=sum(p not in self.vocab or p=='<unk>' for p in pieces)
        if not pieces or unknown==len(pieces):raise ValueError('Source unavailable')
        tokens=self.translator.translate_batch([[*pieces,'</s>']],beam_size=self.config['beamSize'],num_hypotheses=1,patience=1,length_penalty=1,max_input_length=128,max_decoding_length=32,min_decoding_length=1,disable_unk=True,replace_unknowns=False,return_end_token=True)[0].hypotheses[0]
        ended=bool(tokens) and tokens[-1]=='</s>'
        if not tokens or len(tokens)>32 or not ended and len(tokens)!=32 or '<unk>' in tokens:raise ValueError('Invalid decoding result')
        output=self.target_sp.decode_pieces(tokens[:-1] if ended else tokens).strip()
        if not output or len(output.encode('utf-16-le'))//2>2048:raise ValueError('Translation unavailable')
        return {'text':output,'sourceLanguage':self.config['sourceLanguage'],'targetLanguage':self.config['targetLanguage'],'inputTokenCount':len(pieces)+1,'inputUnknownTokenCount':unknown,'outputTokenCount':len(tokens),'inputTruncated':cut,'outputTruncated':not ended,'finishReason':'EOS' if ended else 'OUTPUT_LIMIT','trust':'UNVERIFIED_TRANSLATION','representation':'ctranslate2-marian-v1'}
