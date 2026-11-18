import hashlib,json,sys
from pathlib import Path
sys.path.insert(0,'tools')
from matching_calibration import evaluate
digest=lambda s:hashlib.sha256(s.encode()).hexdigest()
rows=[]
for split in ('fit','holdout'):
 for b,(raw,positive) in enumerate(zip((.18,.36,.54,.72,.90),(2,8,14,22,30))):
  for i in range(40):
   key=f'{split}/{b}/{i}'
   rows.append(dict(caseSha256=digest('case/'+key),groupSha256=digest('entity/'+key),
                    split=split,label=i<positive,similarity=raw,coverage=.75,disposition='COMPARABLE'))
 for i in range(5):
  key=f'{split}/sparse/{i}'
  rows.append(dict(caseSha256=digest('case/'+key),groupSha256=digest('entity/'+key),
                   split=split,label=False,similarity=None,coverage=0,disposition='INSUFFICIENT_EVIDENCE'))
plan=dict(schemaVersion=1,datasetSha256=digest('authored-calibration-bin-population-20261118-v1'),
          spaceKey=digest('synthetic-space'),scoringVersion='bounded-evidence-v1',minimumCoverage=.5,cases=rows)
canonical=json.dumps(plan,sort_keys=True,separators=(',',':')).encode()
record=evaluate(plan,hashlib.sha256(canonical).hexdigest())
assert record['approval']=='NOT_ESTABLISHED' and record['fitCount']==200 and record['supportedHoldout']==200
assert record['holdoutBrier']<record['rawScoreBrierOnSameRows']
record['fixture']='AUTHORED_FIXED_PER_BIN_FREQUENCIES'
p=Path('.test-tmp/m5-calibration-holdout-v2.json')
p.write_bytes((json.dumps(record,indent=2)+'\n').encode())
print(json.dumps(record))
