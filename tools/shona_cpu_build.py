"""Offline build from a reviewed cache; no package resolution or implicit downloads."""
import hashlib,json,shutil,subprocess,tarfile,tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def prepare():
    review=ROOT/'.audit-cache/shona-production-review';context=Path(tempfile.mkdtemp(prefix='shona-cpu-build-',dir=ROOT/'.audit-cache'))
    lock=json.loads((ROOT/'ai/shona-cpu-build.lock.json').read_bytes())
    for entry in lock['sources']:
        archive=review/(entry['name']+'.tar.gz')
        if hashlib.file_digest(archive.open('rb'),'sha256').hexdigest()!=entry['sha256']:raise ValueError('Source archive changed')
        output=context/('extract-'+entry['name']);output.mkdir(exist_ok=True)
        with tarfile.open(archive) as bundle:bundle.extractall(output,filter='data')
        source=next(output.iterdir());target=context/'source' if entry['name']=='CTranslate2' else context/'source/third_party'/entry['name'];shutil.copytree(source,target,dirs_exist_ok=True)
    for name in ('debs','runtime-debs','notices'):(context/name).mkdir(exist_ok=True)
    expected=set()
    for entry in lock['packages']:
        path=review/'packages'/entry['file']
        if hashlib.file_digest(path.open('rb'),'sha256').hexdigest()!=entry['sha256']:raise ValueError('Debian archive changed')
        shutil.copyfile(path,context/'debs'/entry['file']);expected.add(entry['file'])
        if entry['name'] in ('libopenblas0-pthread','libgfortran5','libquadmath0','libc6','libc-bin'):shutil.copyfile(path,context/'runtime-debs'/entry['file'])
    if {p.name for p in (context/'debs').glob('*.deb')}!=expected:raise ValueError('Unreviewed archive in build context')
    runtime={e['file'] for e in lock['packages'] if e['name'] in ('libopenblas0-pthread','libgfortran5','libquadmath0','libc6','libc-bin')}
    if {p.name for p in (context/'runtime-debs').glob('*.deb')}!=runtime:raise ValueError('Unreviewed runtime archive')
    for path in (ROOT/'docs/dependencies/notices/shona-cpu').iterdir():shutil.copyfile(path,context/'notices'/path.name)
    for path in (ROOT/'ai/shona_cpu_native').iterdir():shutil.copyfile(path,context/path.name)
    return context
if __name__=='__main__':
    subprocess.run(['docker','build','--network','none','--platform','linux/amd64','--tag','dzoka-shona-cpu:reviewed',str(prepare())],check=True,timeout=900)
    print('Image built. New builds require explicit image/runtime/profile re-pinning and verification.')
