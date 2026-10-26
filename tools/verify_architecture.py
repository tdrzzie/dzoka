"""Reproduce M1 document/pin/preservation checks; not a proof of architecture correctness."""
import hashlib
import json
from pathlib import Path
import re

ROOT=Path(__file__).resolve().parents[1]
ADR_STEMS=('postgresql-authority','pgvector-default','optional-qdrant','postgis-spatial','retrieval-scoring',
    'matching-ownership','missing-evidence','model-provenance','global-local-fingerprints','ai-provider-ports',
    'asynchronous-ai','durable-events','modular-core','python-ai-boundary','detector-licensing','epcis-adapter',
    'no-default-blockchain','federated-nodes','federation-score-semantics','pii-local','release-authorization',
    'cctv-coarse-fine','movement-state-models','tenant-security')
SECTIONS=('Context','Decision','Alternatives','Consequences','Source-audit evidence','Implementation implications')

def validate(root):
    root=Path(root).resolve();errors=[];stats={}
    def read(relative):
        try:return (root/relative).read_text(encoding='utf-8')
        except (OSError,UnicodeError):errors.append('Missing/unreadable '+str(relative));return ''
    spec=read('docs/dzoka.md')
    try:area=spec.split('# 66. ARCHITECTURE DOCUMENTATION',1)[1].split('# 67. ADRs',1)[0]
    except IndexError:errors.append('Original architecture requirements missing');area=''
    names=re.findall(r'([a-z][a-z-]+\.md)',area)
    if len(names)!=24 or len(set(names))!=24:errors.append('Expected 24 distinct required architecture views')
    documents={Path('docs/architecture')/name:read(Path('docs/architecture')/name) for name in names}
    stats['requiredViews']=len(names)
    try:registry=json.loads(read('docs/source-audit/source-audit.lock.json'));sources={s['audit']:s for s in registry['sources']}
    except (ValueError,KeyError):errors.append('Source audit registry unavailable');sources={}
    stats['baselineAdrs']=0;stats['sourceCitations']=0
    for number,stem in enumerate(ADR_STEMS,2):
        path=Path('docs/adr')/f'{number:04d}-{stem}.md';text=read(path);documents[path]=text
        if text:stats['baselineAdrs']+=1
        if not re.search(r'^- Status:\s*Accepted',text,re.M):errors.append(str(path)+': accepted design status missing')
        for section in SECTIONS:
            match=re.search(r'^## '+re.escape(section)+r'\s*\n(.*?)(?=^## |\Z)',text,re.M|re.S)
            if not match or not match.group(1).strip():errors.append(str(path)+': missing/empty '+section)
        evidence=text.split('## Source-audit evidence',1)[-1].split('## Implementation implications',1)[0]
        citations=0
        for line in evidence.splitlines():
            for target in re.findall(r'\]\(([^)]+)\)',line):
                full=(root/path.parent/target).resolve()
                if not full.is_relative_to(root):errors.append(str(path)+': evidence escapes workspace');continue
                source=sources.get(full.relative_to(root).as_posix())
                if source is None:continue
                citations+=1;stats['sourceCitations']+=1
                if source['status']!='COMPLETE':errors.append(str(path)+': incomplete audit '+source['id'])
                pin=source.get('revision') or source.get('edition')
                if not pin or pin not in line:errors.append(str(path)+': citation pin differs for '+source['id'])
        if not citations:errors.append(str(path)+': no registered source evidence')
    try:
        adr_area=spec.split('# 67. ADRs',1)[1].split('\n# ',1)[0]
        blocks=re.findall(r'```text\s*\n(.*?)```',adr_area,re.S)
        investigations=next(block for block in blocks if 'Use PostgreSQL as primary system of record' in block)
        count=len([line for line in investigations.splitlines() if line.strip()])
        stats['requiredInvestigations']=count
        if count!=23:errors.append('Expected all 23 original ADR investigations')
    except (IndexError,StopIteration):errors.append('Original ADR investigation requirements missing')
    acceptance=read('docs/architecture/acceptance.md');documents[Path('docs/architecture/acceptance.md')]=acceptance
    design=acceptance.split('## Executed foundation portions',1)[0]
    ids=re.findall(r'^\| (A\d{2}) \|',design,re.M)
    if ids!=[f'A{i:02d}' for i in range(1,30)]:errors.append('Acceptance ledger must define A01-A29 exactly once in order')
    stats['acceptanceScenarios']=len(ids)
    prefixes={}
    try:
        preservation=json.loads(read('docs/architecture/preserved-source-manifest.json'))['files']
        for item in preservation:
            path=item['path'];full=(root/path).resolve()
            if not full.is_relative_to(root):errors.append('Preservation path escapes workspace');continue
            try:
                data=full.read_bytes();prefix=item['prefixBytes'];prefixes[path]=data[:prefix].decode('utf-8')
                if hashlib.sha256(data[prefix:]).hexdigest()!=item['originalSha256']:errors.append('Supplied body changed: '+path)
            except (OSError,UnicodeError):errors.append('Preserved document missing/unreadable: '+path)
        stats['preservedBodies']=len(preservation)
        if len(preservation)!=13:errors.append('Expected 13 preserved supplied bodies')
    except (ValueError,KeyError):errors.append('Preservation manifest invalid')
    for name in ['baseline.md','README.md','dependency-adoption.md','milestones.md']:
        path=Path('docs/architecture')/name;documents[path]=read(path)
    for path,text in documents.items():
        # Historical bodies are research, not the current link/acceptance authority.
        text=prefixes.get(path.as_posix(),text)
        if not text.strip():errors.append('Empty architecture document: '+str(path))
        if re.search(r'\b(?:TODO|TBD|INSERT HERE)\b',text):errors.append('Unresolved placeholder: '+str(path))
        for target in re.findall(r'\]\(([^)]+)\)',text):
            if '://' in target or target.startswith('#'):continue
            if not (root/path.parent/target.split('#')[0]).resolve().exists():errors.append(str(path)+': broken local link '+target)
        for aid in re.findall(r'\bA\d{2}\b',text):
            if aid not in ids:errors.append(str(path)+': undefined acceptance ID '+aid)
    stats['documentsChecked']=len(documents)
    return errors,stats

if __name__=='__main__':
    errors,stats=validate(ROOT)
    for error in errors:print(error)
    print(('FAIL' if errors else 'PASS')+': M1 structure/pin/preservation checks '+json.dumps(stats,sort_keys=True))
    raise SystemExit(1 if errors else 0)
