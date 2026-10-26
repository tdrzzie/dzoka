"""Print a disabled reviewed profile INSERT; never connects or enables a provider."""
import argparse
import json
from pathlib import Path
import sys
import uuid
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'ai'))
from perception import profiles,TASKS
if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--tenant',type=uuid.UUID,required=True);parser.add_argument('--profile',type=uuid.UUID,required=True);parser.add_argument('--task',choices=TASKS,required=True);args=parser.parse_args()
    profile=profiles()[args.task];manifest=json.dumps(profile['manifest'],sort_keys=True,separators=(',',':')).replace("'","''")
    print("INSERT INTO dzoka.perception_profiles(tenant_id,id,task,profile_key,manifest,enabled) VALUES ('"+str(args.tenant)+"','"+str(args.profile)+"','"+args.task+"','"+profile['key']+"','"+manifest+"'::jsonb,false);")
