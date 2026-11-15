"""Compare two pinned profiles on one predeclared private reference set."""

import argparse
from collections import Counter
from datetime import datetime, timezone
import json
from pathlib import Path
import re
import sys

sys.path.insert(0, str(Path(__file__).resolve().parent))
from journal_archive import durable_new
from model_evaluation import file_bytes
from profile_archive import audit_records, protected
from quality_protocols import measure, parse, sha
from service_host import plain


ROLES = ('baseline', 'candidate')
STATES = ('AVAILABLE', 'TRUNCATED', 'UNAVAILABLE')
GROUPS = ('inputSha256', 'entityGroup', 'institutionGroup', 'timeGroup')
METRICS = ('exact', 'wer', 'cer', 'labels', 'ranking', 'boxes', 'mask')


def fields(value, names):
    if not isinstance(value, dict) or set(value) != set(names.split()):
        raise ValueError('Unexpected paired comparison fields')


def identity(value):
    if not isinstance(value, str) or not re.fullmatch('[a-f0-9]{64}', value):
        raise ValueError('Content identity required')


def bounded_name(value):
    if not isinstance(value, str) or not 1 <= len(value) <= 128:
        raise ValueError('Bounded private metadata required')


def bound_file(root, descriptor, limit):
    fields(descriptor, 'path sha256')
    identity(descriptor['sha256'])
    return file_bytes(root, descriptor['path'], descriptor['sha256'], limit)


def evaluate(path, expected_plan):
    identity(expected_plan)
    path = plain(path)
    root = protected(path.parent)
    audit_records(root)
    with path.open('rb') as source:
        manifest_raw = source.read(262145)
    if len(manifest_raw) > 262144:
        raise ValueError('Observation manifest exceeds bound')
    manifest = parse(manifest_raw)
    fields(manifest, 'plan observations')
    fields(manifest['plan'], 'path sha256')
    if manifest['plan']['sha256'] != expected_plan:
        raise ValueError('Comparison plan differs from its independently retained digest')
    plan = parse(bound_file(root, manifest['plan'], 262144))
    fields(plan, 'schemaVersion scope evidence profiles cases')
    if type(plan['schemaVersion']) is not int or plan['schemaVersion'] != 1:
        raise ValueError('Unsupported comparison plan')
    if plan['scope'] not in ('AUTHORED_DIAGNOSTIC', 'REFERENCE_EVALUATION'):
        raise ValueError('Explicit evaluation scope required')
    bound_file(root, plan['evidence'], 32768)
    profiles = plan['profiles']
    fields(profiles, 'baseline candidate')
    for profile in profiles.values():
        fields(profile, 'profileKey projectionSourceSha256')
        for value in profile.values():
            identity(value)
    if profiles['baseline']['profileKey'] == profiles['candidate']['profileKey']:
        raise ValueError('Distinct pinned profiles required')

    cases, observations = plan['cases'], manifest['observations']
    if not isinstance(cases, list) or not 1 <= len(cases) <= 128 or not isinstance(observations, dict):
        raise ValueError('Bounded comparison cases required')
    prepared = []
    seen = set()
    groups = {field: {} for field in GROUPS}
    total_bytes = 0

    def artifact(descriptor):
        nonlocal total_bytes
        content = bound_file(root, descriptor, 2 * 1024 * 1024)
        total_bytes += len(content)
        if total_bytes > 32 * 1024 * 1024:
            raise ValueError('Comparison artifact budget exceeded')
        return content

    for case in cases:
        fields(case, 'id split slice inputSha256 entityGroup institutionGroup timeGroup reference metric parameters')
        case_id = case['id']
        for name in ('id', 'slice', 'entityGroup', 'institutionGroup', 'timeGroup'):
            bounded_name(case[name])
        identity(case['inputSha256'])
        if case_id in seen or case['split'] not in ('TRAIN', 'VALIDATION', 'TEST'):
            raise ValueError('Duplicate case or invalid split')
        seen.add(case_id)
        for name, owners in groups.items():
            if owners.setdefault(case[name], case['split']) != case['split']:
                raise ValueError('Cross-split leakage')
        if case['metric'] not in METRICS:
            raise ValueError('A common non-vector reference protocol is required')
        pair = observations.get(case_id)
        fields(pair, 'baseline candidate')
        reference = artifact(case['reference'])
        outputs = {}
        for role in ROLES:
            entry = pair[role]
            fields(entry, 'profileKey inputSha256 state artifact')
            if entry['profileKey'] != profiles[role]['profileKey'] or entry['inputSha256'] != case['inputSha256']:
                raise ValueError('Observation profile or input differs from plan')
            state = entry['state']
            if state not in STATES or (state == 'UNAVAILABLE') != (entry['artifact'] is None):
                raise ValueError('Observation availability does not match its artifact')
            outputs[role] = None if state == 'UNAVAILABLE' else artifact(entry['artifact'])
        prepared.append((case, pair, reference, outputs))
    if set(observations) != seen:
        raise ValueError('Unexpected observations outside the plan')

    rows = []
    coverage = {}
    for ordinal, (case, pair, reference, outputs) in enumerate(prepared):
        slice_hash = sha(case['slice'].encode())
        key = (case['split'], slice_hash, case['metric'])
        summary = coverage.setdefault(key, {**{role: Counter(dict.fromkeys(STATES, 0)) for role in ROLES},
                                            'pairedComplete': 0})
        expected = parse(reference)
        if all(outputs[role] is None for role in ROLES):
            measure(case['metric'], expected, expected, case['parameters'])
        row = {'case': ordinal, 'split': case['split'], 'sliceSha256': slice_hash, 'metric': case['metric'],
               'inputSha256': case['inputSha256'], 'referenceSha256': case['reference']['sha256']}
        for role in ROLES:
            state, content = pair[role]['state'], outputs[role]
            summary[role][state] += 1
            result = {'state': state}
            if content is not None:
                result['artifactSha256'] = pair[role]['artifact']['sha256']
                result['metrics'] = measure(case['metric'], parse(content), expected, case['parameters'])
            row[role] = result
        row['pairedComplete'] = all(pair[role]['state'] == 'AVAILABLE' for role in ROLES)
        summary['pairedComplete'] += int(row['pairedComplete'])
        rows.append(row)

    return {
        'schemaVersion': 1, 'at': datetime.now(timezone.utc).isoformat(), 'scope': plan['scope'],
        'approval': 'NOT_ESTABLISHED', 'leakageCheck': 'DECLARED_GROUPS_DISJOINT',
        'planSha256': manifest['plan']['sha256'], 'observationManifestSha256': sha(manifest_raw),
        'evidenceSha256': plan['evidence']['sha256'], 'profiles': profiles,
        'comparisonSourceSha256': sha(Path(__file__).read_bytes()),
        'protocolSourceSha256': sha(Path(__file__).with_name('quality_protocols.py').read_bytes()),
        'textProtocolSourceSha256': sha(Path(__file__).with_name('model_evaluation.py').read_bytes()),
        'coverage': [{'split': split, 'sliceSha256': slice_hash, 'metric': metric, **summary}
                     for (split, slice_hash, metric), summary in sorted(coverage.items())],
        'rows': rows,
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--dataset', required=True)
    parser.add_argument('--plan-sha256', required=True)
    parser.add_argument('--output', required=True)
    args = parser.parse_args()
    try:
        target = Path(args.output)
        protected(target.parent)
        if ':' in target.name or target.exists():
            raise ValueError('New ordinary result path required')
        result = evaluate(args.dataset, args.plan_sha256)
        durable_new(target, (json.dumps(result, allow_nan=False, indent=2) + '\n').encode())
        print('Private paired evaluation recorded; approval NOT_ESTABLISHED')
    except Exception:
        print('Paired evaluation unavailable; retain private evidence', file=sys.stderr)
        return 1
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
