"""Integrity manifest for an operator-staged, access-controlled backup checkpoint.

Does not connect to production, discover secrets, claim cross-service consistency,
or encrypt data. Keep the manifest digest in independent trusted storage.
"""
import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath
import uuid

KINDS = {'postgres', 'objects', 'configuration', 'tombstones'}

def digest(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()

def path_in(root, name):
    if not isinstance(name, str) or '\\' in name or ':' in name:
        raise ValueError('Invalid backup entry')
    parts = PurePosixPath(name)
    if parts.is_absolute() or '..' in parts.parts or not parts.parts or name != parts.as_posix():
        raise ValueError('Invalid backup entry')
    current = root
    for part in parts.parts:
        current = current/part
        if current.is_symlink():
            raise ValueError('Backup links are prohibited')
    target = current.resolve()
    if not target.is_relative_to(root) or not target.is_file():
        raise ValueError('Backup entry unavailable')
    return target

def canonical(value):
    return json.dumps(value, sort_keys=True, separators=(',', ':'), ensure_ascii=True).encode()

def seal(root, inventory, checkpoint_sha, secret_escrow_sha):
    root = Path(root).resolve()
    for sha in (checkpoint_sha, secret_escrow_sha):
        if len(sha) != 64 or any(c not in '0123456789abcdef' for c in sha):
            raise ValueError('Checkpoint and separate key-escrow digests required')
    if not 4 <= len(inventory) <= 100000 or {e['kind'] for e in inventory} != KINDS:
        raise ValueError('All backup classes must be explicitly represented')
    names = set()
    entries = []
    for item in inventory:
        name = item['name']
        if name in names or name == 'bundle-manifest.json':
            raise ValueError('Repeated or reserved backup entry')
        names.add(name)
        path = path_in(root, name)
        entries.append({'name': name, 'kind': item['kind'], 'size': path.stat().st_size, 'sha256': digest(path)})
    manifest = {'schema': 1, 'id': str(uuid.uuid4()), 'checkpointSha': checkpoint_sha,
                'secretEscrowSha': secret_escrow_sha, 'entries': sorted(entries, key=lambda e: e['name'])}
    content = canonical(manifest)
    with (root/'bundle-manifest.json').open('xb') as output:
        output.write(content)
    return hashlib.sha256(content).hexdigest()

def verify(root, expected_sha):
    root = Path(root).resolve()
    manifest = path_in(root, 'bundle-manifest.json')
    if manifest.stat().st_size > 32*1024*1024 or digest(manifest) != expected_sha:
        raise ValueError('Untrusted backup manifest')
    value = json.loads(manifest.read_bytes())
    if value['schema'] != 1 or {e['kind'] for e in value['entries']} != KINDS:
        raise ValueError('Incomplete backup manifest')
    names = set()
    for entry in value['entries']:
        if entry['name'] in names:
            raise ValueError('Repeated backup entry')
        names.add(entry['name'])
        path = path_in(root, entry['name'])
        if path.stat().st_size != entry['size'] or digest(path) != entry['sha256']:
            raise ValueError('Backup integrity failure')
    return {'schema': 1, 'verifiedEntries': len(names), 'manifestSha': expected_sha}

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['seal', 'verify'])
    parser.add_argument('root', type=Path)
    parser.add_argument('--inventory', type=Path)
    parser.add_argument('--checkpoint-sha')
    parser.add_argument('--secret-escrow-sha')
    parser.add_argument('--expected-sha')
    args = parser.parse_args()
    if args.action == 'seal':
        print(json.dumps({'manifestSha': seal(args.root, json.loads(args.inventory.read_text()), args.checkpoint_sha, args.secret_escrow_sha)}))
    else:
        print(json.dumps(verify(args.root, args.expected_sha)))
