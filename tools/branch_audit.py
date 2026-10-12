#!/usr/bin/env python3
"""Read-only branch accountability audit (zero Actions minutes; also run by tag builds).

Fails (exit 1) when:
  * a branch/ref has commits that are not reachable from the authoritative branch and is
    not accounted for in docs/branch_ledger.json with a matching tip, or
  * a release tag (v*, build-*) points at a commit that is not on the authoritative line.
A ledger entry with "active_feature": true is an in-progress branch that strictly descends from the
authoritative branch (its tip is not pinned, so ordinary commits on it don't fail the audit); if it ever
diverges from the authoritative line it fails like any other unaccounted branch.
Never deletes, merges or rewrites anything.
"""
import json, os, re, subprocess, sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')

def git(*a, root=ROOT):
    return subprocess.run(['git', *a], cwd=root, capture_output=True, text=True, check=True).stdout.strip()

def exists(ref, root=ROOT):
    return subprocess.run(['git', 'rev-parse', '--verify', '-q', ref], cwd=root, capture_output=True).returncode == 0

def audit(root=ROOT):
    """Returns (problems, info_lines, auth_name, auth_short)."""
    auth_name = open(os.path.join(root, 'AUTHORITATIVE_BRANCH')).read().strip()
    ledger = json.load(open(os.path.join(root, 'docs', 'branch_ledger.json')))
    assert ledger['authoritative_branch'] == auth_name, 'AUTHORITATIVE_BRANCH and docs/branch_ledger.json disagree'
    auth = next((r for r in (f'origin/{auth_name}', auth_name) if exists(r, root)), None)
    if auth is None:
        return [f'FAIL: authoritative branch {auth_name} not found'], [], auth_name, ''
    entries = {e['ref']: e for e in ledger['accounted']}
    problems, info = [], []
    refs = git('for-each-ref', '--format=%(refname:short)', 'refs/heads', 'refs/remotes', root=root).split('\n')
    for r in refs:
        if not r or r.endswith('/HEAD') or r in (auth, auth_name, f'origin/{auth_name}'):
            continue
        ahead = int(git('rev-list', '--count', f'{auth}..{r}', root=root))
        if ahead == 0:
            continue
        tip = git('rev-parse', r, root=root)
        e = entries.get(r)
        if e is None:
            problems.append(f'UNACCOUNTED: {r} has {ahead} commit(s) not on {auth_name} and no ledger entry')
        elif e.get('active_feature'):
            if subprocess.run(['git', 'merge-base', '--is-ancestor', auth, r], cwd=root).returncode != 0:
                problems.append(f'DIVERGED: active branch {r} no longer contains {auth_name}; rebase/merge it or re-audit')
            else:
                info.append(f'active feature branch: {r} ({ahead} commit(s) ahead of {auth_name}, descends from it)')
        elif not tip.startswith(e['tip'][:7]):
            problems.append(f'CHANGED: {r} moved to {tip[:7]} since it was audited at {e["tip"][:7]}; re-audit and update the ledger')
        else:
            info.append(f'accounted: {r} ({ahead} commit(s) outside {auth_name})')
    for t in git('tag', '-l', root=root).split('\n'):
        if t and re.match(r'^(v\d+|build-.*)$', t):
            if subprocess.run(['git', 'merge-base', '--is-ancestor', t, auth], cwd=root).returncode != 0:
                problems.append(f'OFF-LINE RELEASE TAG: {t} is not an ancestor of {auth_name}')
    return problems, info, auth_name, git('rev-parse', '--short', auth, root=root)

def main():
    problems, info, auth_name, short = audit()
    print('\n'.join(info))
    if problems:
        print('\n'.join(problems)); print('branch audit FAILED'); return 1
    print(f'branch audit passed: every ref is on, or accounted for against, {auth_name} @ {short}')
    return 0

if __name__ == '__main__':
    sys.exit(main())
