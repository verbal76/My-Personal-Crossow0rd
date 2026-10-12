#!/usr/bin/env python3
"""Combines the per-platform manifests of ONE coordinated build into build-manifest.json and
writes checksums.sha256 for exactly the artifacts they list. Refuses anything inconsistent:

    merge_build_manifest.py --dir DIR [--expect android,linux,windows] [--out build-manifest.json]

Every manifest must name the same source commit, build number and version; every listed artifact
must exist in DIR with the recorded SHA-256 and size; the expected platforms must all be present
exactly once. A partial or mixed build therefore cannot become a release.
"""
import argparse, glob, hashlib, json, os, sys

def merge(directory, expect=None):
    problems, mans = [], []
    for f in sorted(glob.glob(os.path.join(directory, 'build-manifest-*.json'))):
        mans.append(json.load(open(f)))
    if not mans: return None, ['no per-platform manifests found']
    first = mans[0]
    for m in mans[1:]:
        for key, get in (('source commit', lambda x: x['source']['commit']), ('build number', lambda x: x['build_number']),
                         ('version', lambda x: x['public_version']), ('lineage', lambda x: x['source'].get('lineage'))):
            if get(m) != get(first): problems.append(f'manifests disagree on {key}: {get(first)} vs {get(m)}')
    arts = [a for m in mans for a in m['artifacts']]
    plats = [a['platform'] for a in arts]
    for p in set(plats):
        if plats.count(p) != 1: problems.append(f'platform {p} appears {plats.count(p)} times')
    if expect is not None:
        missing = sorted(set(expect) - set(plats)); extra = sorted(set(plats) - set(expect))
        if missing: problems.append(f'missing platform(s): {", ".join(missing)} (a partial build is not a release)')
        if extra: problems.append(f'unexpected platform(s): {", ".join(extra)}')
    for a in arts:
        path = os.path.join(directory, a['file'])
        if not os.path.isfile(path): problems.append(f'{a["file"]} listed but not present'); continue
        data = open(path, 'rb').read()
        if hashlib.sha256(data).hexdigest() != a['sha256']: problems.append(f'{a["file"]}: SHA-256 differs from its manifest')
        if len(data) != a['bytes']: problems.append(f'{a["file"]}: size differs from its manifest')
    if problems: return None, problems
    merged = {k: first[k] for k in ('product', 'public_version', 'build_number', 'release_title', 'destination', 'source') if k in first}
    merged['artifacts'] = arts
    merged['platforms_supported'] = sorted(plats)
    merged['platform_manifests'] = {m['artifacts'][0]['platform']: {k: v for k, v in m.items() if k not in ('artifacts', 'source')} for m in mans}
    return merged, []

def main(argv=None):
    ap = argparse.ArgumentParser(); ap.add_argument('--dir', required=True); ap.add_argument('--expect'); ap.add_argument('--out', default='build-manifest.json')
    a = ap.parse_args(argv)
    merged, problems = merge(a.dir, a.expect.split(',') if a.expect else None)
    if problems:
        print('\n'.join('::error::' + p for p in problems)); print('build manifest merge FAILED'); return 1
    json.dump(merged, open(os.path.join(a.dir, a.out), 'w'), indent=2)
    with open(os.path.join(a.dir, 'checksums.sha256'), 'w') as c:
        for art in merged['artifacts']: c.write(f'{art["sha256"]}  {art["file"]}\n')
    print(f'merged {len(merged["artifacts"])} artifact(s) for Build {merged["build_number"]} @ {merged["source"]["commit"][:12]}: {", ".join(merged["platforms_supported"])}')
    return 0

if __name__ == '__main__':
    sys.exit(main())
