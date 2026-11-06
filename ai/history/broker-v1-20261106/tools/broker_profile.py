"""Disabled SQL only; registration/enablement and activation require operator authority."""
import argparse
import json
from pathlib import Path
import sys
import uuid
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'ai'))
from broker_profiles import profiles
if __name__=='__main__':
    registry=profiles();parser=argparse.ArgumentParser();parser.add_argument('--tenant',type=uuid.UUID,required=True);parser.add_argument('--profile',type=uuid.UUID,required=True);parser.add_argument('--profile-name',choices=registry,required=True);args=parser.parse_args();selected=registry[args.profile_name]
    quote=lambda s:"'"+str(s).replace("'","''")+"'"
    if selected['family']=='text':
        s=selected['space'];columns=['provider','model','artifactRevision','artifactSha256','tokenizerSha256','preprocessing','dimensions','metric','normalization','configSha256','spaceKey']
        values=[quote(args.tenant),quote(args.profile),*[str(s[k]) if k=='dimensions' else quote(s[k]) for k in columns],'false']
        print('INSERT INTO dzoka.embedding_spaces(tenant_id,id,provider,model,artifact_revision,artifact_sha256,tokenizer_sha256,preprocessing,dimensions,metric,normalization,config_sha256,space_key,enabled) VALUES('+','.join(values)+');')
    else:
        print('INSERT INTO dzoka.perception_profiles(tenant_id,id,task,profile_key,manifest,enabled) VALUES('+','.join([quote(args.tenant),quote(args.profile),quote(selected['manifest']['task']),quote(selected['key']),quote(json.dumps(selected['manifest'],sort_keys=True,separators=(',',':')))+'::jsonb','false'])+');')
