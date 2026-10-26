"""Launch private perception with a scrubbed service-only environment and hard process deadline."""
import os
from pathlib import Path
import subprocess
import sys
ROOT=Path(__file__).resolve().parents[1]
if __name__=='__main__':
    allowed={'PATH','SYSTEMROOT','WINDIR','TEMP','TMP','HOME','LANG','LC_ALL','SSL_CERT_FILE','SSL_CERT_DIR'}
    env={k:v for k,v in os.environ.items() if k in allowed or k.startswith('DZOKA_AI_')}
    env['PYTHONNOUSERSITE']='1';env['PYTHONUTF8']='1'
    try:
        raise SystemExit(subprocess.run([str(ROOT/'.audit-cache/ai-venv/Scripts/python.exe'),'-I',str(ROOT/'ai/perception_worker.py'),*sys.argv[1:]],cwd=ROOT,env=env,timeout=110).returncode)
    except subprocess.TimeoutExpired:
        print('Perception worker deadline exceeded; lease remains recoverable',file=sys.stderr);raise SystemExit(1)
