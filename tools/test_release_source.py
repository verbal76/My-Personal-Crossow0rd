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
        self.assertTrue(any('not a build-<N>' in b for b in self.check(self.c1, tag='random-tag')))
    def test_fail_missing_authoritative_marker(self):
        os.remove(os.path.join(self.d, 'AUTHORITATIVE_BRANCH')); git(self.d, 'add', '-A'); git(self.d, 'commit', '-q', '-m', 'rm')
        c = git(self.d, 'rev-parse', 'HEAD'); git(self.d, 'update-ref', 'refs/remotes/origin/auth', c)
        self.assertTrue(any('AUTHORITATIVE_BRANCH is missing' in b for b in self.check(c)))
    def test_branch_ref_without_tag_still_requires_the_line(self):
        self.assertEqual(self.check(self.c2), [])
        self.assertTrue(any('not reachable' in b for b in self.check(self.side)))

class LocalOnlyFeatureBranch(unittest.TestCase):
    """local_only may build an in-progress ledgered branch; anything that publishes may not."""
    def setUp(self):
        self.d = tempfile.mkdtemp(); git(self.d, 'init', '-q', '-b', 'auth')
        os.makedirs(os.path.join(self.d, 'docs'))
        self.base = commit(self.d, '10', 'base')
        git(self.d, 'update-ref', 'refs/remotes/origin/auth', self.base)
        git(self.d, 'checkout', '-q', '-b', 'feat')
        self.ledger([{'ref': 'origin/feat', 'active_feature': True}])
        self.tip = commit(self.d, '11', 'feature')
        git(self.d, 'update-ref', 'refs/remotes/origin/feat', self.tip)
    def ledger(self, entries):
        json.dump({'authoritative_branch': 'auth', 'accounted': entries}, open(os.path.join(self.d, 'docs', 'branch_ledger.json'), 'w'))
    def check(self, dest, sha=None):
        sha = sha or self.tip
        git(self.d, 'checkout', '-q', '--detach', sha)
        e = {'GITHUB_SHA': sha, 'BUILD_SOURCE_SHA': sha, 'GITHUB_REF': 'refs/heads/feat', 'GITHUB_REF_NAME': 'feat'}
        if dest: e['DESTINATION'] = dest
        return V.verify_source(self.d, e)
    def test_local_only_accepts_a_ledgered_active_feature_commit(self): self.assertEqual(self.check('local_only'), [])
    def test_publishing_destination_is_strict(self):
        self.assertTrue(any('not reachable' in b for b in self.check('github_releases')))
    def test_unset_destination_is_strict(self):
        self.assertTrue(any('not reachable' in b for b in self.check(None)))
    def test_local_only_rejects_a_branch_with_no_active_feature_entry(self):
        git(self.d, 'checkout', '-q', 'feat'); self.ledger([]); git(self.d, 'add', '-A'); git(self.d, 'commit', '-q', '-m', 'unledger')
        tip = git(self.d, 'rev-parse', 'HEAD'); git(self.d, 'update-ref', 'refs/remotes/origin/feat', tip)
        self.assertTrue(any('not reachable' in b for b in self.check('local_only', tip)))
    def test_local_only_rejects_a_branch_that_no_longer_contains_the_authoritative_tip(self):
        git(self.d, 'checkout', '-q', 'auth'); moved = commit(self.d, '10', 'auth moved')
        git(self.d, 'update-ref', 'refs/remotes/origin/auth', moved)
        self.assertTrue(any('not reachable' in b for b in self.check('local_only')))
    def test_local_only_rejects_a_commit_not_on_the_feature_branch(self):
        git(self.d, 'checkout', '-q', '-b', 'stray', self.base); stray = commit(self.d, '10', 'stray')
        self.assertTrue(any('not reachable' in b for b in self.check('local_only', stray)))
    def test_new_tag_name_is_accepted(self):
        git(self.d, 'update-ref', 'refs/remotes/origin/auth', self.tip)
        git(self.d, 'tag', 'build-11', self.tip)
        git(self.d, 'checkout', '-q', '--detach', self.tip)
        e = {'GITHUB_SHA': self.tip, 'BUILD_SOURCE_SHA': self.tip, 'GITHUB_REF': 'refs/tags/build-11', 'GITHUB_REF_NAME': 'build-11'}
        self.assertEqual(V.verify_source(self.d, e), [])

class BranchAudit(unittest.TestCase):
    def setUp(self):
        import branch_audit as B; self.B = B
        self.d = tempfile.mkdtemp(); git(self.d, 'init', '-q', '-b', 'auth')
        open(os.path.join(self.d, '.git', 'info', 'exclude'), 'a').write('docs/\n')   # the ledger stays a working-tree file
        commit(self.d, '10', 'one')
        os.makedirs(os.path.join(self.d, 'docs'))
        self.write_ledger([])
        git(self.d, 'checkout', '-q', '-b', 'feature')
        commit(self.d, '10', 'feature work')
        git(self.d, 'checkout', '-q', 'auth')
    def write_ledger(self, entries):
        json.dump({'authoritative_branch': 'auth', 'note': '', 'accounted': entries}, open(os.path.join(self.d, 'docs', 'branch_ledger.json'), 'w'))
    def test_unledgered_branch_fails(self):
        self.assertTrue(any('UNACCOUNTED' in p for p in self.B.audit(self.d)[0]))
    def test_active_feature_branch_passes_without_a_pinned_tip(self):
        self.write_ledger([{'ref': 'feature', 'active_feature': True}])
        self.assertEqual(self.B.audit(self.d)[0], [])
        git(self.d, 'checkout', '-q', 'feature'); commit(self.d, '10', 'more work'); git(self.d, 'checkout', '-q', 'auth')
        self.assertEqual(self.B.audit(self.d)[0], [])          # new commits do not need a ledger edit
    def test_active_feature_branch_that_diverges_fails(self):
        self.write_ledger([{'ref': 'feature', 'active_feature': True}])
        commit(self.d, '10', 'auth moved on')                  # authoritative branch gets a commit feature lacks
        self.assertTrue(any('DIVERGED' in p for p in self.B.audit(self.d)[0]))
    def test_pinned_entry_still_fails_when_the_tip_moves(self):
        tip = git(self.d, 'rev-parse', 'feature')
        self.write_ledger([{'ref': 'feature', 'tip': tip}]); self.assertEqual(self.B.audit(self.d)[0], [])
        git(self.d, 'checkout', '-q', 'feature'); commit(self.d, '10', 'moved'); git(self.d, 'checkout', '-q', 'auth')
        self.assertTrue(any('CHANGED' in p for p in self.B.audit(self.d)[0]))

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
    def test_feature_lineage_manifest_only_passes_for_local_only(self):
        self.m['source']['on_authoritative_line'] = False; self.m['source']['lineage'] = 'active_feature:origin/x'
        self.assertTrue(any('authoritative' in b for b in self.run_check(self.m)))
        self.env['DESTINATION'] = 'local_only'; self.assertEqual(self.run_check(self.m), [])
        self.env['DESTINATION'] = 'github_releases'; self.assertTrue(any('authoritative' in b for b in self.run_check(self.m)))
    def test_fail_artifact_missing(self): self.m['artifacts'] = []; self.assertTrue(any('does not list' in b for b in self.run_check(self.m)))

import glob, re
import merge_build_manifest as M

class WorkflowPolicy(unittest.TestCase):
    """The authorization rules, enforced on the REAL workflow files (release authorization checks)."""
    WF = os.path.join(HERE, '..', '.github', 'workflows')
    @classmethod
    def setUpClass(cls):
        import yaml
        cls.files = {os.path.basename(f): (yaml.safe_load(open(f)), open(f).read()) for f in glob.glob(os.path.join(cls.WF, '*.yml'))}
    @staticmethod
    def triggers(doc): return doc.get(True) or doc.get('on') or {}
    def test_workflows_exist(self):
        self.assertTrue({'build.yml', 'pr-check.yml'} <= set(self.files))
    def test_only_dispatch_and_pull_request_triggers_exist(self):
        for name, (doc, _) in self.files.items():
            trig = set(self.triggers(doc))
            self.assertTrue(trig <= {'workflow_dispatch', 'pull_request'}, f'{name} has forbidden triggers {trig}')
            if name != 'pr-check.yml': self.assertEqual(trig, {'workflow_dispatch'}, f'{name} must be manual-only')
    def test_no_push_tag_schedule_or_chained_triggers_anywhere(self):
        for name, (_, text) in self.files.items():
            for bad in ('push:', 'tags:', 'schedule:', 'workflow_run', 'pull_request_target', 'workflow_call'):
                self.assertNotRegex(text, r'(?m)^\s{0,4}' + re.escape(bad), f'{name} uses {bad}')
    def test_pr_check_has_no_secrets_no_write_and_publishes_nothing(self):
        doc, text = self.files['pr-check.yml']
        self.assertEqual(doc['permissions'], {'contents': 'read'})
        code = '\n'.join(l for l in text.splitlines() if not l.lstrip().startswith('#'))
        for forbidden in ('secrets.', 'gh release', 'upload-artifact', 'assembleDebug', 'assembleRelease', 'package.sh', 'jpackage', 'contents: write'):
            self.assertNotIn(forbidden, code, f'pr-check.yml must not contain {forbidden}')
        for job in doc['jobs'].values(): self.assertNotIn('permissions', job)
    def test_publishing_lives_only_in_the_publish_job_behind_the_destination_input(self):
        doc, text = self.files['build.yml']
        for jname, job in doc['jobs'].items():
            body = json.dumps(job)
            publishes = 'gh release create' in body or 'gh release upload' in body
            self.assertEqual(publishes, jname == 'publish', f'only publish may create releases (found in {jname})')
            writes = (job.get('permissions') or {}).get('contents') == 'write'
            self.assertEqual(writes, jname == 'publish', f'contents: write belongs to publish only ({jname})')
        self.assertIn("inputs.destination == 'github_releases'", doc['jobs']['publish']['if'])
        self.assertEqual(doc['permissions'], {'contents': 'read'})
    def test_publish_requires_every_selected_platform_and_never_clobbers(self):
        doc, text = self.files['build.yml']
        self.assertEqual(set(doc['jobs']['publish']['needs']), {'plan', 'desktop-tests', 'android', 'desktop'})
        self.assertNotIn('--clobber', text)
        self.assertIn('no partial release', text)
    def test_signing_secrets_are_confined_to_the_android_job(self):
        for name, (doc, text) in self.files.items():
            for jname, job in doc['jobs'].items():
                body = json.dumps(job)
                if 'RELEASE_KEYSTORE' in body or 'RELEASE_KEY_' in body:
                    self.assertIn((name, jname), {('build.yml', 'android'), ('release-aab.yml', 'aab')}, f'{name}:{jname} touches signing secrets')
    def test_build_workflow_requires_sha_number_and_destination_defaulting_to_local_only(self):
        inp = self.triggers(self.files['build.yml'][0])['workflow_dispatch']['inputs']
        self.assertEqual(set(inp), {'platform', 'source_sha', 'build_number', 'destination'})
        self.assertEqual(inp['destination']['default'], 'local_only')
        self.assertEqual(set(inp['platform']['options']), {'android', 'windows', 'linux', 'all'})
        self.assertTrue(inp['source_sha']['required'] and inp['build_number']['required'])
    def test_every_checkout_in_the_build_workflow_uses_the_input_sha(self):
        doc, _ = self.files['build.yml']
        n = 0
        for job in doc['jobs'].values():
            for st in job['steps']:
                if str(st.get('uses', '')).startswith('actions/checkout'):
                    n += 1; self.assertIn('inputs.source_sha', st['with']['ref'])
        self.assertGreaterEqual(n, 5)
    def test_release_names_follow_the_studio_standard(self):
        text = self.files['build.yml'][1]
        self.assertIn('My Personal Crossword Build $N', text)
        self.assertIn('My Personal Crossword Build ${N} APK.apk', text)


class MergeManifests(unittest.TestCase):
    def setUp(self):
        self.d = tempfile.mkdtemp()
        self.write('android', 'My Personal Crossword Build 11 APK.apk', b'apk')
        self.write('linux', 'My Personal Crossword Build 11 Linux.tar.gz', b'tgz')
    def write(self, plat, name, data, commit='a' * 40, number=11, version='v11'):
        open(os.path.join(self.d, name), 'wb').write(data)
        json.dump({'product': 'P', 'public_version': version, 'build_number': number, 'release_title': 'T', 'destination': 'local_only',
                   'source': {'commit': commit, 'lineage': 'authoritative'},
                   'artifacts': [{'platform': plat, 'file': name, 'sha256': hashlib.sha256(data).hexdigest(), 'bytes': len(data)}]},
                  open(os.path.join(self.d, f'build-manifest-{plat}.json'), 'w'))
    def test_consistent_build_merges_and_writes_checksums(self):
        m, p = M.merge(self.d, ['android', 'linux']); self.assertEqual(p, [])
        self.assertEqual(m['platforms_supported'], ['android', 'linux'])
        self.assertEqual(M.main(['--dir', self.d, '--expect', 'android,linux']), 0)
        lines = open(os.path.join(self.d, 'checksums.sha256')).read().splitlines(); self.assertEqual(len(lines), 2)
    def test_partial_build_is_refused(self):
        self.assertTrue(any('missing platform' in x for x in M.merge(self.d, ['android', 'linux', 'windows'])[1]))
    def test_mixed_source_commits_are_refused(self):
        self.write('linux', 'My Personal Crossword Build 11 Linux.tar.gz', b'tgz', commit='b' * 40)
        self.assertTrue(any('source commit' in x for x in M.merge(self.d)[1]))
    def test_mixed_build_numbers_are_refused(self):
        self.write('linux', 'My Personal Crossword Build 11 Linux.tar.gz', b'tgz', number=12)
        self.assertTrue(any('build number' in x for x in M.merge(self.d)[1]))
    def test_tampered_artifact_is_refused(self):
        open(os.path.join(self.d, 'My Personal Crossword Build 11 Linux.tar.gz'), 'wb').write(b'other')
        self.assertTrue(any('SHA-256' in x for x in M.merge(self.d)[1]))
    def test_missing_artifact_is_refused(self):
        os.remove(os.path.join(self.d, 'My Personal Crossword Build 11 APK.apk'))
        self.assertTrue(any('not present' in x for x in M.merge(self.d)[1]))
    def test_unexpected_platform_is_refused(self):
        self.assertTrue(any('unexpected' in x for x in M.merge(self.d, ['android'])[1]))
    def test_no_manifests_is_refused(self):
        self.assertTrue(M.merge(tempfile.mkdtemp())[1])

class VerifierScan(unittest.TestCase):
    PKG = 'package a.b\n'
    def test_counts_plain_and_fully_qualified_annotations(self):
        src = self.PKG + 'class X {\n  @Test fun a() {}\n  @org.junit.Test fun b() {}\n  @Test(timeout = 5) fun c() {}\n}\n'
        self.assertEqual(T.scan_source(src), {'a.b.X': 3})
    def test_fully_qualified_only_file_is_not_silently_omitted(self):
        self.assertEqual(T.scan_source(self.PKG + 'class Y {\n  @org.junit.Test fun a() {}\n}\n'), {'a.b.Y': 1})
    def test_each_top_level_class_gets_its_own_count(self):
        src = self.PKG + 'class A {\n @Test fun a() {}\n}\nclass B {\n @Test fun b() {}\n @Test fun c() {}\n}\n'
        self.assertEqual(T.scan_source(src), {'a.b.A': 1, 'a.b.B': 2})
    def test_nested_helper_classes_do_not_split_the_count(self):
        src = self.PKG + 'class A {\n  class Helper\n  @Test fun a() {}\n}\n'
        self.assertEqual(T.scan_source(src), {'a.b.A': 1})
    def test_unsupported_annotation_form_fails_loudly(self):
        for ann in ('@kotlin.test.Test', '@org.junit.jupiter.api.Test'):
            with self.assertRaises(SystemExit): T.scan_source(self.PKG + f'class Z {{\n  {ann} fun a() {{}}\n}}\n')
    def test_ignore_is_rejected_in_both_spellings(self):
        for ig in ('@Ignore', '@org.junit.Ignore'):
            with self.assertRaises(SystemExit): T.scan_source(self.PKG + f'class Z {{\n {ig}\n @Test fun a() {{}}\n}}\n')
    def test_helper_files_without_tests_are_not_suites(self):
        self.assertEqual(T.scan_source(self.PKG + 'object Support { fun x() {} }\n'), {})
    def test_real_repository_inventory_includes_every_test_file(self):
        inv = T.expected(core_only=False, desktop=False)
        self.assertGreater(len(inv), 15)
        import glob
        for f in glob.glob(os.path.join(HERE, '..', 'app/src/test/**/*.kt'), recursive=True):
            src = open(f, encoding='utf-8').read()
            self.assertEqual(sum(T.scan_source(src, f).values()), len(T.TEST_ANNOTATION.findall(src)), f)

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
