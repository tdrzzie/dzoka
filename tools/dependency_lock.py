"""Record/check resolved Maven dependency bytes. No network, stdlib only."""
import argparse
import hashlib
import json
from pathlib import Path
import sys
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]
NS = {'m': 'http://maven.apache.org/POM/4.0.0'}
LICENSE_OPTIONS = {
    'apache license, version 2.0':'Apache-2.0',
    'the apache software license, version 2.0':'Apache-2.0',
    'the apache license, version 2.0':'Apache-2.0',
    'apache-2.0':'Apache-2.0', 'mit':'MIT', 'the mit license':'MIT',
    'bsd-2-clause':'BSD-2-Clause', 'mit-0':'MIT-0', 'epl-2.0':'EPL-2.0', 'epl 2.0':'EPL-2.0',
    'eclipse public license v2.0':'EPL-2.0',
}

def selected_license(declarations):
    options=[LICENSE_OPTIONS[d.lower()] for d in declarations if isinstance(d,str) and d.lower() in LICENSE_OPTIONS]
    if not options: raise ValueError('Unreviewed license requires adoption decision')
    return options[0]

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def licenses(repository, group, artifact, version, seen=()):
    relative = f'{group.replace(".", "/")}/{artifact}/{version}/{artifact}-{version}.pom'
    path = repository / relative
    if relative in seen or not path.is_file(): raise ValueError('Missing/cyclic license evidence: '+relative)
    tree = ET.parse(path)
    declarations = [x.findtext('m:name', namespaces=NS) for x in tree.findall('m:licenses/m:license', NS)]
    if declarations:
        return declarations, {'path': relative, 'sha256': sha(path)}
    parent = tree.find('m:parent', NS)
    if parent is None: raise ValueError('Unresolved license: '+relative)
    coordinates = [parent.findtext('m:'+x, namespaces=NS) for x in ['groupId','artifactId','version']]
    return licenses(repository, *coordinates, seen=seen+(relative,))

def generate(root):
    repository = root / '.audit-cache/m2'
    tree = json.loads((root/'.audit-cache/dependency-tree.json').read_text())
    entries = []
    def walk(node):
        if node.get('artifactId') != 'dzoka-core':
            g,a,v = (node[k] for k in ('groupId','artifactId','version'))
            relative = f'{g.replace(".", "/")}/{a}/{v}/{a}-{v}'
            declarations,evidence = licenses(repository,g,a,v)
            jar = repository / (relative+'.jar')
            with zipfile.ZipFile(jar) as bundle:
                notices = {n:hashlib.sha256(bundle.read(n)).hexdigest() for n in bundle.namelist()
                           if n.startswith('META-INF/') and ('LICENSE' in n.upper() or 'NOTICE' in n.upper())
                           and not n.endswith('/')}
            entries.append({'coordinate':f'{g}:{a}:{v}', 'scope':node['scope'],
                'jar':relative+'.jar', 'jarSha256':sha(jar), 'pom':relative+'.pom',
                'pomSha256':sha(repository/(relative+'.pom')), 'licenses':declarations,
                'selectedLicense':selected_license(declarations),
                'licenseEvidence':evidence, 'embeddedNotices':notices})
        for child in node.get('children',[]): walk(child)
    walk(tree)
    return {'schemaVersion':1, 'review':'docs/dependencies/README.md',
            'pomSha256':sha(root/'pom.xml'), 'maven':'3.9.16',
            'dependencies':sorted(entries,key=lambda e:e['coordinate'])}

def check(root):
    lock = json.loads((root/'dependencies.lock.json').read_text())
    if lock['schemaVersion'] != 1 or sha(root/'pom.xml') != lock['pomSha256']:
        raise ValueError('Dependency declaration changed; review and regenerate lock')
    repo = (root/'.audit-cache/m2').resolve()
    coordinates=set()
    for entry in lock['dependencies']:
        if entry['coordinate'] in coordinates or not entry['licenses']:
            raise ValueError('Duplicate coordinate or absent license')
        if entry['selectedLicense'] != selected_license(entry['licenses']):
            raise ValueError('Unreviewed license selection')
        coordinates.add(entry['coordinate'])
        for field, digest in [('jar','jarSha256'),('pom','pomSha256')]:
            path = (repo/entry[field]).resolve()
            if not path.is_relative_to(repo) or not path.is_file() or sha(path)!=entry[digest]:
                raise ValueError('Missing/altered dependency: '+entry['coordinate'])
        evidence=entry['licenseEvidence']; path=(repo/evidence['path']).resolve()
        if not path.is_relative_to(repo) or not path.is_file() or sha(path)!=evidence['sha256']:
            raise ValueError('Missing/altered license evidence: '+entry['coordinate'])
    if not coordinates: raise ValueError('Empty dependency lock')
    return len(coordinates)

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--write',action='store_true');args=parser.parse_args()
    try:
        if args.write:
            data=generate(ROOT)
            (ROOT/'dependencies.lock.json').write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
        count=check(ROOT)
    except (ValueError,KeyError,OSError,ET.ParseError,zipfile.BadZipFile) as error:
        print('FAIL: '+str(error),file=sys.stderr);return 1
    print(f'PASS: {count} pinned dependency artifacts, POMs and license declarations');return 0

if __name__=='__main__':raise SystemExit(main())
