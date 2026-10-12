#!/usr/bin/env python3
"""Read-only branch accountability audit (zero Actions minutes; also run by tag builds).

Fails (exit 1) when:
  * a branch/ref has commits that are not reachable from the authoritative branch and is
    not accounted for in docs/branch_ledger.json with a matching tip, or
  * a release tag (v*, build-*) points at a commit that is not on the authoritative line.
Never deletes, merges or rewrites anything.
"""
import json, os, re, subprocess, sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')

def git(*a):
    return subprocess.run(['git', *a], cwd=ROOT, capture_output=True, text=True, check=True).stdout.strip()

def exists(ref):
    return subprocess.run(['git', 'rev-parse', '--verify', '-q', ref], cwd=ROOT, capture_output=True).returncode == 0

def main():
    auth_name = open(os.path.join(ROOT, 'AUTHORITATIVE_BRANCH')).read().strip()
    ledger = json.load(open(os.path.join(ROOT, 'docs', 'branch_ledger.json')))
    assert ledger['authoritative_branch'] == auth_name, 'AUTHORITATIVE_BRANCH and docs/branch_ledger.json disagree'
    auth = next((r for r in (f'origin/{auth_name}', auth_name) if exists(r)), None)
    if auth is None:
        print(f'FAIL: authoritative branch {auth_name} not found'); return 1
    accounted = {e['ref']: e['tip'] for e in ledger['accounted']}
    problems = []
    refs = git('for-each-ref', '--format=%(refname:short)', 'refs/heads', 'refs/remotes').split('\n')
    for r in refs:
        if not r or r.endswith('/HEAD') or r in (auth, auth_name, f'origin/{auth_name}'):
            continue
        ahead = int(git('rev-list', '--count', f'{auth}..{r}'))
        if ahead == 0:
            continue
        tip = git('rev-parse', r)
        want = accounted.get(r)
        if want is None:
            problems.append(f'UNACCOUNTED: {r} has {ahead} commit(s) not on {auth_name} and no ledger entry')
        elif not tip.startswith(want[:7]):
            problems.append(f'CHANGED: {r} moved to {tip[:7]} since it was audited at {want[:7]}; re-audit and update the ledger')
        else:
            print(f'accounted: {r} ({ahead} commit(s) outside {auth_name})')
    for t in git('tag', '-l').split('\n'):
        if t and re.match(r'^(v\d+|build-.*)$', t):
            if subprocess.run(['git', 'merge-base', '--is-ancestor', t, auth], cwd=ROOT).returncode != 0:
                problems.append(f'OFF-LINE RELEASE TAG: {t} is not an ancestor of {auth_name}')
    if problems:
        print('\n'.join(problems)); print('branch audit FAILED'); return 1
    print(f'branch audit passed: every ref is on, or accounted for against, {auth_name} @ {git("rev-parse", "--short", auth)}')
    return 0

if __name__ == '__main__':
    sys.exit(main())
