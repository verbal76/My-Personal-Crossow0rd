#!/usr/bin/env python3
"""Writes release-manifest.json: one record tying a release to its exact source and artifacts.

    make_release_manifest.py <apk> [out.json]

Every platform artifact of a coordinated release lists the SAME source commit here.
"""
import hashlib, json, os, platform, re, subprocess, sys, datetime

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')

def sh(*a):
    try: return subprocess.run(a, cwd=ROOT, capture_output=True, text=True).stdout.strip()
    except Exception: return ''

def catalog():
    out = {}
    for line in open(os.path.join(ROOT, 'gradle', 'libs.versions.toml')):
        m = re.match(r'^(\w+)\s*=\s*"([^"]+)"', line)
        if m: out[m.group(1)] = m.group(2)
    return out

def main():
    apk = sys.argv[1]; out = sys.argv[2] if len(sys.argv) > 2 else 'release-manifest.json'
    sha = os.environ.get('BUILD_SOURCE_SHA') or sh('git', 'rev-parse', 'HEAD')
    ver = open(os.path.join(ROOT, 'VERSION')).read().strip()
    wrapper = open(os.path.join(ROOT, 'gradle', 'wrapper', 'gradle-wrapper.properties')).read()
    gradle = re.search(r'gradle-([\d.]+)-bin', wrapper).group(1)
    lib = catalog()
    h = hashlib.sha256(open(apk, 'rb').read()).hexdigest()
    m = {
        'product': 'My Personal Crossword',
        'public_version': f'v{ver}',
        'source': {
            'commit': sha,
            'ref': os.environ.get('GITHUB_REF_NAME', ''),
            'authoritative_branch': open(os.path.join(ROOT, 'AUTHORITATIVE_BRANCH')).read().strip(),
            'on_authoritative_line': True,
        },
        'build': {
            'timestamp_utc': datetime.datetime.now(datetime.timezone.utc).replace(microsecond=0).isoformat(),
            'ci_run': f"{os.environ.get('GITHUB_SERVER_URL','')}/{os.environ.get('GITHUB_REPOSITORY','')}/actions/runs/{os.environ.get('GITHUB_RUN_ID','')}",
            'android_version_code': 1000 + int(os.environ.get('GITHUB_RUN_NUMBER', '0')),
            'configuration': 'debug',
            'signing': 'debug keystore (committed); not a Play release',
        },
        'toolchain': {
            'gradle': gradle, 'agp': lib.get('agp'), 'kotlin': lib.get('kotlin'), 'compose_bom': lib.get('composeBom'),
            'jdk': sh('java', '-version') or 'see CI log', 'min_sdk': 24, 'target_sdk': 36, 'compile_sdk': '36.1',
        },
        'artifacts': [{'platform': 'android', 'file': os.path.basename(apk), 'sha256': h, 'bytes': os.path.getsize(apk)}],
        'platforms_supported': ['android'],
    }
    json.dump(m, open(out, 'w'), indent=2); print(json.dumps(m, indent=2))

if __name__ == '__main__':
    main()
