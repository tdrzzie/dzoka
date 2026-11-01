"""Supervised profile checks and explicitly scoped orphan reconciliation."""
import argparse
import json
from pathlib import Path
import subprocess
import sys
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'ai'));sys.path.insert(0,str(ROOT/'tools'))
from shona_supervised import profiles
from ai_supervisor import verify_sources
from docker_lifecycle import preflight,reconcile
if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('operation',choices=['check','profiles','reconcile']);parser.add_argument('--state');args=parser.parse_args()
    try:
        verify_sources();registry=profiles()
        if registry!=json.loads((ROOT/'ai/shona-supervised-profiles.json').read_bytes()):raise ValueError('Registry changed')
        if args.operation=='profiles':print(json.dumps(registry,indent=2))
        elif args.operation=='check':subprocess.run([sys.executable,str(ROOT/'tools/shona_cpu_runtime.py'),'check'],cwd=ROOT,check=True);print('PASS: five appended supervised profiles; earlier identities unchanged')
        else:
            if not args.state or not Path(args.state).is_absolute():raise ValueError('Explicit state path required')
            preflight();result=reconcile(Path(args.state));print(json.dumps(result));raise SystemExit(1 if result['unresolved'] else 0)
    except Exception:print('Supervised Docker runtime unavailable; inspect private state/configuration',file=sys.stderr);raise SystemExit(1)
