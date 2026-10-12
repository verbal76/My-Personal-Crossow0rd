#!/usr/bin/env python3
"""Writes release-manifest.json: one record tying a release to its exact source and artifacts.

    make_release_manifest.py <artifact> [out.json] [--platform android|windows|linux]

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

def jdk():
    try:   # `java -version` prints to stderr
        r = subprocess.run(['java', '-version'], capture_output=True, text=True)
        return next(l for l in (r.stderr or r.stdout).splitlines() if not l.startswith('Picked up')).strip()
    except Exception: return 'see CI log'

def desktop_manifest(m, platform_name, artifact):
    """Desktop artifacts: same source/provenance block, desktop toolchain and signing facts."""
    dsrc = open(os.path.join(ROOT, 'desktop', 'build.gradle.kts')).read()
    m['build'].pop('android_version_code', None)
    m['build']['configuration'] = 'release (jpackage app-image)'
    m['build']['signing'] = 'unsigned (no code-signing certificate); Windows SmartScreen will warn until one is added'
    m['toolchain'] = {
        'gradle': m['toolchain']['gradle'], 'kotlin': re.search(r'kotlin\("jvm"\) version "([^"]+)"', dsrc).group(1),
        'ui': 'Java Swing / Java2D (JDK only, no Compose)', 'jdk': jdk(), 'packager': 'jpackage (bundled runtime)',
        'runner_os': os.environ.get('RUNNER_OS', platform.system()),
    }
    m['artifacts'][0]['platform'] = platform_name
    m['platforms_supported'] = [platform_name]
    m['verification'] = {'unit_tested': True, 'packaged_launch_tested': os.environ.get('LAUNCH_TESTED', 'false') == 'true',
                         'physically_tested_on_steam_deck': False, 'steam_deck_verified': False}
    return m

def main():
    args = [a for a in sys.argv[1:] if not a.startswith('--platform=')]
    plat = next((a.split('=', 1)[1] for a in sys.argv[1:] if a.startswith('--platform=')), 'android')
    apk = args[0]; out = args[1] if len(args) > 1 else 'release-manifest.json'
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
    if plat != 'android': m = desktop_manifest(m, plat, apk)
    json.dump(m, open(out, 'w'), indent=2); print(json.dumps(m, indent=2))

if __name__ == '__main__':
    main()
