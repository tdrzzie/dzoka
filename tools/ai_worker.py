"""Run the private inference client with only its required service identity, never DB/S3 credentials."""
import os
from pathlib import Path
import subprocess
import sys
ROOT=Path(__file__).resolve().parents[1]
if __name__=='__main__':
    binary=ROOT/'.audit-cache/ai-venv'/('Scripts/python.exe' if os.name=='nt' else 'bin/python')
    allowed={'PATH','SYSTEMROOT','WINDIR','TEMP','TMP','HOME','LANG','LC_ALL','SSL_CERT_FILE','SSL_CERT_DIR'}
    env={k:v for k,v in os.environ.items() if k in allowed or k.startswith('DZOKA_AI_')}
    env['PYTHONNOUSERSITE']='1';env['PYTHONUTF8']='1'
    raise SystemExit(subprocess.run([str(binary),'-I',str(ROOT/'ai/worker.py'),*sys.argv[1:]],cwd=ROOT,env=env).returncode)
