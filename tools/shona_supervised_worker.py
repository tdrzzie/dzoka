"""One supervised CPU Shona invocation; guardian must pre-exist outside worker ownership."""
import os
from pathlib import Path
import subprocess
import sys
ROOT=Path(__file__).resolve().parents[1]
if __name__=='__main__':
    # This fixed launcher is entered only through the owned bootstrap; standalone callers use a supervisor plan.
    if not os.environ.get('DZOKA_AI_DOCKER_KEY') or not os.environ.get('DZOKA_AI_DOCKER_PORT'):raise SystemExit(1)
    allowed={'PATH','SYSTEMROOT','WINDIR','TEMP','TMP','HOME','LANG','LC_ALL','SSL_CERT_FILE','SSL_CERT_DIR'}
    env={k:v for k,v in os.environ.items() if k in allowed or k.startswith('DZOKA_AI_')};env.update(PYTHONNOUSERSITE='1',PYTHONUTF8='1')
    try:raise SystemExit(subprocess.run([str(ROOT/'.audit-cache/generative-venv/Scripts/python.exe'),'-I',str(ROOT/'ai/shona_supervised_worker.py'),*sys.argv[1:]],cwd=ROOT,env=env,timeout=290).returncode)
    except subprocess.TimeoutExpired:raise SystemExit(1)
