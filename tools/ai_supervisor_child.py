"""Wait for process-tree ownership before invoking a fixed one-job worker."""
import subprocess
import sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parent))
from ai_supervisor import FAMILIES,ROOT,verify_sources
if __name__=='__main__':
    try:
        if len(sys.argv)!=3 or sys.argv[1] not in FAMILIES or sys.stdin.buffer.read(1)!=b'1':raise ValueError('Bootstrap denied')
        verify_sources(sys.argv[2])
        raise SystemExit(subprocess.call([sys.executable,'-I',str(ROOT/'tools'/FAMILIES[sys.argv[1]][0]),'--once'],stdin=subprocess.DEVNULL))
    except Exception:raise SystemExit(125)
