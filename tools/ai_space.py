"""Print reviewable SQL for an authorized operator; never connects to or changes a database."""
import argparse
from pathlib import Path
import sys
import uuid
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'ai'))
from provider import load_profile

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--tenant',type=uuid.UUID,required=True);p.add_argument('--space',type=uuid.UUID,required=True);args=p.parse_args()
    s=load_profile()['space']
    fields={'tenant_id':str(args.tenant),'id':str(args.space),'provider':s['provider'],'model':s['model'],
        'artifact_revision':s['artifactRevision'],'artifact_sha256':s['artifactSha256'],'tokenizer_sha256':s['tokenizerSha256'],
        'preprocessing':s['preprocessing'],'dimensions':s['dimensions'],'metric':s['metric'],'normalization':s['normalization'],
        'config_sha256':s['configSha256'],'space_key':s['spaceKey']}
    values=[str(v) if isinstance(v,int) else "'"+v.replace("'","''")+"'" for v in fields.values()]
    print('-- Creates a disabled space. Review/probe before explicit operator activation.')
    print('INSERT INTO dzoka.embedding_spaces('+','.join(fields)+',enabled) VALUES('+','.join(values)+',false);')
    print("-- After adoption/probe: UPDATE dzoka.embedding_spaces SET enabled=true WHERE tenant_id='"+str(args.tenant)+"' AND id='"+str(args.space)+"';")
