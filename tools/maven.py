"""Pinned workspace-local Maven launcher. Python 3.11+ and a reviewed JDK required."""
import hashlib
import os
from pathlib import Path
import subprocess
import sys
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[1]
VERSION = '3.9.16'
SHA512 = 'ed41650d42485cfc243fad22158caf9cbb5dc408ce7a09ddb94dd42a019de929ca43065bfa450612cf12bf78b5cafa3884b96c090de326ff590448c933454af3'
URL = f'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/{VERSION}/apache-maven-{VERSION}-bin.zip'

def main():
    cache = ROOT / '.audit-cache'
    distribution = cache / f'apache-maven-{VERSION}'
    launcher = distribution / 'bin' / ('mvn.cmd' if os.name == 'nt' else 'mvn')
    if not launcher.exists():
        cache.mkdir(exist_ok=True)
        raw = urllib.request.urlopen(URL, timeout=120).read()
        if hashlib.sha512(raw).hexdigest() != SHA512:
            raise SystemExit('Maven distribution checksum mismatch')
        archive = cache / f'apache-maven-{VERSION}-bin.zip'
        archive.write_bytes(raw)
        with zipfile.ZipFile(archive) as bundle:
            for name in bundle.namelist():
                target = (cache / name).resolve()
                if not target.is_relative_to(distribution.resolve()):
                    raise SystemExit('Unsafe Maven archive member')
            bundle.extractall(cache)
        if os.name != 'nt': launcher.chmod(0o755)
    return subprocess.run([str(launcher), '-B', '-ntp',
                           f'-Dmaven.repo.local={cache / "m2"}', *sys.argv[1:]], cwd=ROOT).returncode

if __name__ == '__main__':
    raise SystemExit(main())
