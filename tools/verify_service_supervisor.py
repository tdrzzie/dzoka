"""Test entry point: actual supervised jobs inside the service's host boundary.

This bypasses neither the production service preflight nor its SCM protocol:
it is a finite integration-test command, not an installed-service launcher.
"""
from pathlib import Path
import sys
sys.path.insert(0,str(Path(__file__).resolve().parent))
from service_host import HostLimits
from worker_service import verify_release
from ai_supervisor import main
if __name__=='__main__':
    if len(sys.argv)!=5 or sys.argv[1]!='--plan' or sys.argv[3:]!=['--rounds','1']:raise SystemExit('One explicit test plan/pass required')
    verify_release()
    limits=HostLimits()
    raise SystemExit(main())
