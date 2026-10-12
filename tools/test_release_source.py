#!/usr/bin/env python3
"""Passing and deliberately failing cases for the release-source gate and test verifier.
Run: python3 tools/test_release_source.py   (also run by tools/validate_local.sh; zero CI minutes)"""
import hashlib, json, os, subprocess, sys, tempfile, unittest

HERE = os.path.dirname(os.path.abspath(__file__)); sys.path.insert(0, HERE)
import verify_release_source as V
import verify_test_results as T

def git(d, *a):
    return subprocess.run(['git', *a], cwd=d, capture_output=True, text=True, check=True,
                          env={**os.environ, 'GIT_AUTHOR_NAME': 't', 'GIT_AUTHOR_EMAIL': 't@t', 'GIT_COMMITTER_NAME': 't', 'GIT_COMMITTER_EMAIL': 't@t'}).stdout.strip()

def commit(d, version='10', msg='c'):
    open(os.path.join(d, 'VERSION'), 'w').write(version + '\n')
    open(os.path.join(d, 'AUTHORITATIVE_BRANCH'), 'w').write('auth\n')
    open(os.path.join(d, 'f.txt'), 'a').write(msg)
    git(d, 'add', '-A'); git(d, 'commit', '-q', '-m', msg)
    return git(d, 'rev-parse', 'HEAD')

class Source(unittest.TestCase):
    def setUp(self):
        self.d = tempfile.mkdtemp(); git(self.d, 'init', '-q', '-b', 'auth')
        self.c1 = commit(self.d, '10', 'one')            # older commit on the authoritative line
        self.c2 = commit(self.d, '11', 'two')            # the tip
        git(self.d, 'update-ref', 'refs/remotes/origin/auth', self.c2)
        git(self.d, 'checkout', '-q', '-b', 'side', self.c1)
        self.side = commit(self.d, '10', 'side-only')    # a commit that is NOT on the line
        git(self.d, 'checkout', '-q', 'auth')
    def env(self, sha, tag=None, branch=None):
        e = {'GITHUB_SHA': sha, 'BUILD_SOURCE_SHA': sha}
        if tag: e.update(GITHUB_REF=f'refs/tags/{tag}', GITHUB_REF_NAME=tag)
        else:   e.update(GITHUB_REF=f'refs/heads/{branch or "auth"}', GITHUB_REF_NAME=branch or 'auth')
        return e
    def check(self, sha, **kw):
        git(self.d, 'checkout', '-q', '--detach', sha)
        return V.verify_source(self.d, self.env(sha, **kw))

    def test_pass_tagged_commit_that_is_not_the_branch_tip(self):
        git(self.d, 'tag', 'build-v10', self.c1)                      # older than tip: still valid
        self.assertEqual(self.check(self.c1, tag='build-v10'), [])
    def test_pass_tip(self):
        git(self.d, 'tag', '-a', 'build-v11', '-m', 'annotated', self.c2)
        self.assertEqual(self.check(self.c2, tag='build-v11'), [])
    def test_unrelated_historical_branches_do_not_block(self):
        git(self.d, 'branch', 'old-reviewed-branch', self.side); git(self.d, 'tag', 'build-v10', self.c1)
        self.assertEqual(self.check(self.c1, tag='build-v10'), [])
    def test_fail_commit_not_on_authoritative_line(self):
        git(self.d, 'tag', 'build-v10', self.side)
        self.assertTrue(any('not reachable' in b for b in self.check(self.side, tag='build-v10')))
    def test_fail_recorded_sha_differs_from_checked_out_source(self):
        git(self.d, 'checkout', '-q', '--detach', self.c1)
        e = self.env(self.c2); bad = V.verify_source(self.d, e)
        self.assertTrue(any('GITHUB_SHA' in b for b in bad)); self.assertTrue(any('BUILD_SOURCE_SHA' in b for b in bad))
    def test_fail_tag_points_elsewhere(self):
        git(self.d, 'tag', 'build-v10', self.c2)                       # tag is on c2 but c1 is checked out
        self.assertTrue(any('points at' in b for b in self.check(self.c1, tag='build-v10')))
    def test_fail_tag_version_mismatch(self):
        git(self.d, 'tag', 'build-v99', self.c1)
        self.assertTrue(any('does not match VERSION' in b for b in self.check(self.c1, tag='build-v99')))
    def test_fail_not_a_release_tag_name(self):
        git(self.d, 'tag', 'random-tag', self.c1)
        self.assertTrue(any('not a build-v<N>' in b for b in self.check(self.c1, tag='random-tag')))
    def test_fail_missing_authoritative_marker(self):
        os.remove(os.path.join(self.d, 'AUTHORITATIVE_BRANCH')); git(self.d, 'add', '-A'); git(self.d, 'commit', '-q', '-m', 'rm')
        c = git(self.d, 'rev-parse', 'HEAD'); git(self.d, 'update-ref', 'refs/remotes/origin/auth', c)
        self.assertTrue(any('AUTHORITATIVE_BRANCH is missing' in b for b in self.check(c)))
    def test_branch_ref_without_tag_still_requires_the_line(self):
        self.assertEqual(self.check(self.c2), [])
        self.assertTrue(any('not reachable' in b for b in self.check(self.side)))

class Manifest(unittest.TestCase):
    def setUp(self):
        self.d = tempfile.mkdtemp(); git(self.d, 'init', '-q', '-b', 'auth'); self.c = commit(self.d, '10')
        git(self.d, 'update-ref', 'refs/remotes/origin/auth', self.c)
        self.apk = os.path.join(self.d, 'My-Personal-Crossword-v10.apk'); open(self.apk, 'wb').write(b'apk-bytes')
        self.env = {'GITHUB_REF_NAME': 'build-v10'}
        self.m = {'public_version': 'v10', 'source': {'commit': self.c, 'ref': 'build-v10', 'on_authoritative_line': True},
                  'artifacts': [{'file': os.path.basename(self.apk), 'sha256': hashlib.sha256(b'apk-bytes').hexdigest(), 'bytes': 9}]}
    def run_check(self, m):
        p = os.path.join(self.d, 'm.json'); json.dump(m, open(p, 'w')); return V.verify_manifest(self.d, self.env, p, self.apk)
    def test_consistent_manifest_passes(self): self.assertEqual(self.run_check(self.m), [])
    def test_fail_wrong_commit(self): self.m['source']['commit'] = 'a' * 40; self.assertTrue(any('commit' in b for b in self.run_check(self.m)))
    def test_fail_wrong_version(self): self.m['public_version'] = 'v9'; self.assertTrue(any('version' in b for b in self.run_check(self.m)))
    def test_fail_wrong_hash(self): self.m['artifacts'][0]['sha256'] = '0' * 64; self.assertTrue(any('hash' in b for b in self.run_check(self.m)))
    def test_fail_wrong_ref(self): self.m['source']['ref'] = 'main'; self.assertTrue(any('ref' in b for b in self.run_check(self.m)))
    def test_fail_artifact_missing(self): self.m['artifacts'] = []; self.assertTrue(any('does not list' in b for b in self.run_check(self.m)))

class TestInventory(unittest.TestCase):
    def results(self, classes):
        d = tempfile.mkdtemp()
        for cls, (t, s, f) in classes.items():
            open(os.path.join(d, f'TEST-{cls}.xml'), 'w').write(f'<testsuite name="{cls}" tests="{t}" skipped="{s}" failures="{f}" errors="0"/>')
        return d
    def test_real_results_for_every_declared_suite_pass(self):
        inv = T.expected(False); d = self.results({c: (n, 0, 0) for c, n in inv.items()})
        self.assertEqual(T.main(['--results', d]), 0)
    def test_missing_suite_fails(self):
        inv = T.expected(False); inv.pop(next(iter(inv))); d = self.results({c: (n, 0, 0) for c, n in inv.items()})
        self.assertEqual(T.main(['--results', d]), 1)
    def test_skipped_test_fails(self):
        inv = T.expected(False); first = next(iter(inv)); r = {c: (n, 0, 0) for c, n in inv.items()}; r[first] = (inv[first], 1, 0)
        self.assertEqual(T.main(['--results', self.results(r)]), 1)
    def test_count_mismatch_fails(self):
        inv = T.expected(False); first = next(iter(inv)); r = {c: (n, 0, 0) for c, n in inv.items()}; r[first] = (inv[first] - 1, 0, 0)
        self.assertEqual(T.main(['--results', self.results(r)]), 1)
    def test_failure_fails(self):
        inv = T.expected(False); first = next(iter(inv)); r = {c: (n, 0, 0) for c, n in inv.items()}; r[first] = (inv[first], 0, 1)
        self.assertEqual(T.main(['--results', self.results(r)]), 1)

if __name__ == '__main__':
    unittest.main(verbosity=1)
