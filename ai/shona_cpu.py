"""Reviewed CPU container translations; auto copies remain unverified proposals."""
import json
import struct
import subprocess
import uuid
from contextlib import ExitStack
from pathlib import Path
from perception import canonical,sha
ROOT=Path(__file__).resolve().parents[1]

def profiles():
    lock=json.loads((ROOT/'ai/shona-cpu.lock.json').read_bytes())
    for name,digest in lock['sources'].items():
        if sha((ROOT/name).read_bytes())!=digest:raise ValueError('CPU implementation changed')
    for name in ('modelLock','runtimeLock'):
        if sha((ROOT/lock[name]).read_bytes())!=lock[name+'Sha256']:raise ValueError('CPU lock changed')
    runtime=json.loads((ROOT/lock['runtimeLock']).read_bytes())
    if sha((ROOT/'ai/generative-runtime.lock.json').read_bytes())!=runtime['hostRuntimeLockSha256']:raise ValueError('Host runtime changed')
    for entry in runtime['notices']:
        if sha((ROOT/entry['path']).read_bytes())!=entry['sha256']:raise ValueError('CPU notice changed')
    models=json.loads((ROOT/lock['modelLock']).read_bytes())
    result={}
    for name,config in lock['configuration'].items():
        groups=[config['artifactGroup']]+(['LID'] if config.get('proposalPolicy') else [])
        manifest={'schema':'perception-profile-v1','task':'MULTILINGUAL_TRANSLATION','provider':'ctranslate2-openblas-cpu','runtimeLockSha256':lock['runtimeLockSha256'],'sources':lock['sources'],'configuration':config,'artifacts':[{k:f[k] for k in ('repository','revision','path','bytes','sha256','role')} for f in models['files'] if f['group'] in groups]}
        result[name]={'key':sha(canonical(manifest)),'manifest':manifest}
    return result

def decode_tokens(data):
    if len(data)>33000:raise ValueError('CPU protocol bound')
    position=0
    def number():
        nonlocal position
        if position+4>len(data):raise ValueError('CPU protocol incomplete')
        n=struct.unpack_from('<I',data,position)[0];position+=4;return n
    count=number()
    if not 1<=count<=32:raise ValueError('CPU protocol count')
    tokens=[]
    for _ in range(count):
        size=number()
        if not 1<=size<=1024 or position+size>len(data):raise ValueError('CPU protocol length')
        tokens.append(data[position:position+size].decode('utf-8'));position+=size
    if position!=len(data) or any('\0' in t for t in tokens):raise ValueError('CPU protocol trailing')
    return tokens

class ShonaCpuProvider:
    def __init__(self,name,directory):
        self.stack=ExitStack();self.lid=None;self.closed=False
        try:
            self.profile=profiles()[name];self.config=self.profile['manifest']['configuration'];self.directory=Path(directory).resolve()
            runtime=json.loads((ROOT/'ai/shona-cpu-runtime.lock.json').read_bytes());self.image=runtime['imageId']
            from generative_integrity import verify_runtime
            verify_runtime()
            self.check_image()
            from generative_model import locked_model
            files={};self.native_files={}
            entries=json.loads((ROOT/'ai/shona-cpu-models.lock.json').read_bytes())['files']
            for entry in entries:
                if entry['group']==self.config['artifactGroup']:
                    locked=self.stack.enter_context(locked_model(self.directory/entry['local'],entry));self.native_files[entry['role']]=locked
                    if entry['role']!='model.bin':files[entry['role']]=Path(locked).read_bytes()
            import sentencepiece as spm
            self.source_sp=spm.SentencePieceProcessor(model_proto=files['source.spm']);self.target_sp=spm.SentencePieceProcessor(model_proto=files['target.spm']);self.vocab=set(json.loads(files['shared_vocabulary.json']))
            if self.config.get('proposalPolicy'):
                from shona_lid import LanguageIdentifier
                self.lid=LanguageIdentifier([e for e in entries if e['group']=='LID'],self.directory)
        except Exception:self.close();raise
    def check_image(self):
        result=subprocess.run(['docker','image','inspect',self.image,'--format','{{.Id}}'],capture_output=True,timeout=20,check=True)
        if result.stdout.decode().strip()!=self.image:raise ValueError('CPU image unavailable')
    def close(self):
        self.closed=True
        if self.lid is not None:self.lid.close();self.lid=None
        self.stack.close()
    def __enter__(self):return self
    def __exit__(self,*_):self.close()
    def native(self,pieces):
        if getattr(self,'closed',False):raise ValueError('CPU provider closed')
        data=struct.pack('<I',len(pieces))
        for piece in pieces:
            raw=piece.encode();
            if not 1<=len(raw)<=1024:raise ValueError('CPU token bound')
            data+=struct.pack('<I',len(raw))+raw
        name='dzoka-shona-'+uuid.uuid4().hex
        command=['docker','run','--rm','--name',name,'--network','none','--read-only','--cap-drop','ALL','--security-opt','no-new-privileges','--pids-limit','64','--memory','1536m','--cpus','2','--tmpfs','/tmp:rw,noexec,nosuid,size=16m','-i']
        for role in ('model.bin','config.json','shared_vocabulary.json'):
            if any(c in self.native_files[role] for c in (',','\r','\n')):raise ValueError('Unsupported mount path')
            command+=['--mount','type=bind,source='+self.native_files[role]+',target=/model/'+role+',readonly']
        command.append(self.image)
        try:
            run=subprocess.run(command,input=data,capture_output=True,timeout=120)
            if run.returncode:raise ValueError('CPU translation unavailable')
            return decode_tokens(run.stdout)
        except subprocess.TimeoutExpired:raise ValueError('CPU deadline exceeded') from None
        finally:
            # Owned invocation only; also cleans up after CLI timeout.
            subprocess.run(['docker','rm','-f',name],capture_output=True,timeout=20)
    def translate(self,text):
        pieces=self.source_sp.encode(text,out_type=str);cut=len(pieces)>127;pieces=pieces[:127];unknown=sum(p not in self.vocab or p=='<unk>' for p in pieces)
        if not pieces or unknown==len(pieces):raise ValueError('Source unavailable')
        tokens=self.native([*pieces,'</s>']);ended=tokens[-1]=='</s>'
        if '<unk>' in tokens or '</s>' in tokens[:-1] or not ended and len(tokens)!=32:raise ValueError('CPU decoding result')
        output=self.target_sp.decode_pieces(tokens[:-1] if ended else tokens).strip()
        if not output or len(output.encode('utf-16-le'))//2>2048:raise ValueError('Translation unavailable')
        return {'text':output,'sourceLanguage':self.config['sourceLanguage'],'targetLanguage':self.config['targetLanguage'],'inputTokenCount':len(pieces)+1,'inputUnknownTokenCount':unknown,'outputTokenCount':len(tokens),'inputTruncated':cut,'outputTruncated':not ended,'finishReason':'EOS' if ended else 'OUTPUT_LIMIT','trust':'UNVERIFIED_TRANSLATION','representation':'ctranslate2-marian-v1'}
    def infer(self,data):
        if getattr(self,'closed',False):raise ValueError('CPU provider closed')
        if len(data)>32000:raise ValueError('Source exceeds bound')
        text=data.decode('utf-8')
        if not text.strip() or len(text)>8000 or '\0' in text:raise ValueError('Invalid source')
        from shona_markup import protect,restore,utf16
        if self.config.get('proposalPolicy'):
            if '982' in text or '[[' in text or ']]' in text:raise ValueError('Reserved auto source')
            proposals,scanned,truncated=self.lid.propose(text);spans=[];parts=[];position=0
            for i,p in enumerate(proposals):
                sentinel='982'+str(i);parts.extend([text[position:p['start']],sentinel]);position=p['end'];spans.append({'sentinel':sentinel,'quote':p['quote'],'sourceStart':utf16(text[:p['start']]),'sourceEnd':utf16(text[:p['end']])})
            parts.append(text[position:]);e=self.translate(''.join(parts));output,copies=restore(e['text'],spans)
            for copy,p in zip(copies,proposals):copy.update(trust='AUTO_LANGUAGE_PROPOSAL_UNVERIFIED',language='en',modelScore=p['modelScore'],modelMargin=p['modelMargin'])
            e.update(text=output,representation='ctranslate2-auto-v1',proposedSpans=copies,proposalStatus='PROPOSED' if copies else 'NO_ACCEPTED_WINDOW',scannedSourceUnits=scanned,proposalScanTruncated=truncated)
        elif self.config.get('protectionPolicy'):
            masked,spans=protect(text);e=self.translate(masked);output,copies=restore(e['text'],spans);e.update(text=output,representation='ctranslate2-marked-v1',preservedSpans=copies)
        else:e=self.translate(text)
        if utf16(e['text'])>2048:raise ValueError('Restored translation exceeds bound')
        return e
