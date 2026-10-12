#!/usr/bin/env python3
"""Release-critical source verification. Blocks a delivered build when the source is wrong.

It checks the ACTUAL checked-out and tagged commit (not the tip of any branch):
  1. HEAD == GITHUB_SHA == BUILD_SOURCE_SHA (the recorded SHA is the source being built);
  2. if the ref is a tag: the tag peels to exactly HEAD, and a `build-v<N>` tag matches the
     VERSION file at that commit (a tag cannot ship a different version);
  3. HEAD is reachable from the approved authoritative line (origin/<AUTHORITATIVE_BRANCH>).
     Exception, local_only builds ONLY (env DESTINATION=local_only): nothing is published, so HEAD may instead be
     reachable from an in-progress branch that docs/branch_ledger.json (at HEAD) marks active_feature AND that still
     contains the authoritative tip. Any destination that publishes (github_releases, or unset) is strict.
Reviewed historical branches are deliberately NOT considered here (see branch_audit.py).

    verify_release_source.py                       # source checks
    verify_release_source.py --manifest M --apk A  # also: manifest consistent with source + artifact
Environment: GITHUB_SHA, BUILD_SOURCE_SHA, GITHUB_REF (refs/tags/NAME or refs/heads/NAME), GITHUB_REF_NAME.
"""
import argparse, hashlib, json, os, re, subprocess, sys

def git(repo, *a):
    r = subprocess.run(['git', *a], cwd=repo, capture_output=True, text=True)
    return r.returncode, r.stdout.strip()

def verify_source(repo, env):
    bad = []
    rc, head = git(repo, 'rev-parse', 'HEAD')
    if rc: return ['cannot read HEAD of the checked-out source']
    for k in ('GITHUB_SHA', 'BUILD_SOURCE_SHA'):
        v = env.get(k, '')
        if not v: bad.append(f'{k} is not set')
        elif v != head: bad.append(f'{k}={v[:12]} does not match the checked-out source HEAD={head[:12]}')
    ref = env.get('GITHUB_REF', ''); name = env.get('GITHUB_REF_NAME', '')
    if ref.startswith('refs/tags/'):
        rc, peeled = git(repo, 'rev-parse', '--verify', '-q', f'refs/tags/{name}^{{commit}}')
        if rc or not peeled: bad.append(f'tag {name} does not exist in the checkout')
        elif peeled != head: bad.append(f'tag {name} points at {peeled[:12]} but the checked-out source is {head[:12]}')
        m = re.fullmatch(r'build-v?(\d+)', name)
        if not m: bad.append(f'tag {name} is not a build-<N> (or legacy build-v<N>) release tag')
        else:
            rc, ver = git(repo, 'show', f'{head}:VERSION')
            if rc or ver.strip() != m.group(1): bad.append(f'tag {name} does not match VERSION ({ver.strip() or "unreadable"}) at that commit')
    rc, auth = git(repo, 'show', f'{head}:AUTHORITATIVE_BRANCH')
    auth = auth.strip()
    if rc or not auth: bad.append('AUTHORITATIVE_BRANCH is missing at the source commit')
    else:
        rc, _ = git(repo, 'rev-parse', '--verify', '-q', f'refs/remotes/origin/{auth}')
        if rc: bad.append(f'authoritative branch origin/{auth} is not available to verify against')
        elif git(repo, 'merge-base', '--is-ancestor', head, f'refs/remotes/origin/{auth}')[0] != 0:
            on_feature = env.get('DESTINATION') == 'local_only' and active_feature_branch(repo, head, auth)
            if on_feature: print(f'note: local_only build from active feature branch {on_feature}; this commit cannot be published')
            else: bad.append(f'commit {head[:12]} is not reachable from the authoritative line origin/{auth}')
    return bad

def active_feature_branch(repo, head, auth):
    """Name of a ledgered active_feature branch that contains HEAD and still descends from the authoritative tip, else None."""
    rc, raw = git(repo, 'show', f'{head}:docs/branch_ledger.json')
    if rc: return None
    try: entries = json.loads(raw).get('accounted', [])
    except ValueError: return None
    for e in entries:
        ref = e.get('ref', '')
        if not e.get('active_feature') or not ref.startswith('origin/'): continue
        full = f'refs/remotes/{ref}'
        if git(repo, 'rev-parse', '--verify', '-q', full)[0]: continue
        if git(repo, 'merge-base', '--is-ancestor', head, full)[0] == 0 and \
           git(repo, 'merge-base', '--is-ancestor', f'refs/remotes/origin/{auth}', full)[0] == 0:
            return ref
    return None

def verify_manifest(repo, env, manifest, apk):
    bad = []; m = json.load(open(manifest))
    _, head = git(repo, 'rev-parse', 'HEAD')
    ver = open(os.path.join(repo, 'VERSION')).read().strip()
    if m['source']['commit'] != head: bad.append(f"manifest commit {m['source']['commit'][:12]} != source {head[:12]}")
    if env.get('GITHUB_REF_NAME') and m['source']['ref'] != env['GITHUB_REF_NAME']: bad.append('manifest ref does not match the build ref')
    if m['public_version'] != f'v{ver}': bad.append(f"manifest version {m['public_version']} != VERSION v{ver}")
    if m['source'].get('on_authoritative_line') is not True: bad.append('manifest does not assert the authoritative line')
    art = [x for x in m['artifacts'] if x['file'] == os.path.basename(apk)]
    if not art: bad.append('manifest does not list the built artifact')
    else:
        h = hashlib.sha256(open(apk, 'rb').read()).hexdigest()
        if art[0]['sha256'] != h: bad.append('manifest artifact hash does not match the built file')
        if art[0]['bytes'] != os.path.getsize(apk): bad.append('manifest artifact size does not match the built file')
    return bad

def main(argv=None, repo=None, env=None):
    ap = argparse.ArgumentParser(); ap.add_argument('--manifest'); ap.add_argument('--apk')
    a = ap.parse_args(argv)
    repo = repo or os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
    env = env if env is not None else os.environ
    bad = verify_source(repo, env)
    if a.manifest: bad += verify_manifest(repo, env, a.manifest, a.apk)
    if bad:
        print('\n'.join('::error::' + b for b in bad)); print('release source verification FAILED'); return 1
    print('release source verification passed: source, tag and' + (' manifest' if a.manifest else ' authoritative line') + ' agree')
    return 0

if __name__ == '__main__':
    sys.exit(main())
