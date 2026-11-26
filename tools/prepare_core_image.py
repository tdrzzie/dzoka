"""Extract only the project's packaged classes and locked dependencies for its image."""
from pathlib import Path, PurePosixPath
import zipfile

ROOT = Path(__file__).resolve().parents[1]

def prepare(archive, destination):
    destination = Path(destination).resolve()
    if destination.exists():
        raise ValueError('Use a fresh layer directory; existing files are not overwritten')
    with zipfile.ZipFile(archive) as package:
        entries = [i for i in package.infolist() if i.filename.startswith(('BOOT-INF/classes/', 'BOOT-INF/lib/')) and not i.is_dir()]
        if not entries or sum(i.file_size for i in entries) > 256 * 1024 * 1024:
            raise ValueError('Invalid application package')
        for entry in entries:
            path = PurePosixPath(entry.filename)
            if '..' in path.parts or '\\' in entry.filename or path.is_absolute():
                raise ValueError('Invalid package path')
        destination.mkdir(parents=True)
        for entry in entries:
            target = destination.joinpath(*PurePosixPath(entry.filename).parts)
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(package.read(entry))
    return len(entries)

if __name__ == '__main__':
    print('Packaged image entries:', prepare(ROOT/'target/dzoka-core-0.1.0-SNAPSHOT.jar', ROOT/'target/core-layer'))
