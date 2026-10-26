"""Print a disabled reviewed profile INSERT; no connection, model enablement or implicit direction."""
import argparse
import json
from pathlib import Path
import sys
import uuid
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'ai'))
from shona import profiles
if __name__=='__main__':
    registry=profiles();parser=argparse.ArgumentParser();parser.add_argument('--tenant',type=uuid.UUID,required=True);parser.add_argument('--profile',type=uuid.UUID,required=True);parser.add_argument('--profile-name',choices=registry,required=True);args=parser.parse_args()
    p=registry[args.profile_name];manifest=json.dumps(p['manifest'],sort_keys=True,separators=(',',':')).replace("'","''")
    print("INSERT INTO dzoka.perception_profiles(tenant_id,id,task,profile_key,manifest,enabled) VALUES ('"+str(args.tenant)+"','"+str(args.profile)+"','"+p['manifest']['task']+"','"+p['key']+"','"+manifest+"'::jsonb,false);")
