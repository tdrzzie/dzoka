from pathlib import Path
source=Path('.test-tmp/afrislm-evaluate-20261116.py').read_text()
source=source.replace('afrislm-build-20261116.json','morena-build-20261116.json').replace('DzokaAfriSlmDiagnostic-20261116-v1','DzokaMorenaDiagnostic-20261116-v1').replace('afrislm-models-20261116','morena-models-20261116').replace('1082015808','1580378784').replace('b5056635be0c9bb832f0c964dc063bd4b4462e253f9f9d94eebf0aa0960b7798','2b7905b1f0d70efeaa7f95ae1829ffb640dcfce748f9e20e45185359b4933203').replace('afrislm-strict-v1','morena-translation-v1').replace('afrislm-v1','morena-v1').replace('dzoka-afrislm-diagnostic-','dzoka-morena-diagnostic-').replace("'3g'","'4g'").replace('==3*1024**3','==4*1024**3')
extra='''
smol=repo/'.audit-cache/smol-reference-20261116'
for direction,filename,digest in [('en-sn','en_sn.jsonl','8726011841a3eb23532ee11385dc4c3106fbe3e026d935150c0b6fcf97a3decf'),('sn-en','sn_en.jsonl','a79080c195dc705c01ce9c4e1b800830d763f5f911c32664b4efe8db30d4053d')]:
 raw=(smol/filename).read_bytes();assert sha(raw)==digest
 for n,line in enumerate(raw.splitlines()[:8],1):
  row=json.loads(line);assert row['sl']==direction[:2] and row['tl']==direction[3:]
  cases.append({'direction':direction,'row':n,'slice':'smol-additional','source':row['src'],'reference':row['trg'],'expectedOutputFragments':[]})
'''
source=source.replace("root=Path.home()/'DzokaMorenaDiagnostic-20261116-v1'",extra+"\nroot=Path.home()/'DzokaMorenaDiagnostic-20261116-v1'")
exec(compile(source,__file__,'exec'))
