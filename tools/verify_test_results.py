#!/usr/bin/env python3
"""Verifies that every unit-test class in the repository actually executed and passed.

Instead of trusting an arbitrary minimum count, the expected inventory is derived from the
source: every class under app/src/test that declares @Test methods must have a result file
with exactly that many tests, none skipped, none failed. A missing class (a suite that was
silently not run), a skipped test, or a count mismatch fails.

    verify_test_results.py [--results DIR] [--core-only | --desktop]

  --results DIR   where JUnit XML lives (default app/build/test-results/testDebugUnitTest)
  --core-only     only classes in the pure-Kotlin ...core package (local JVM harness)
  --desktop       the desktop build's suite: the shared core tests plus desktop/src/test
                  (results default to desktop/build/test-results/test)
"""
import argparse, glob, os, re, sys
import xml.etree.ElementTree as ET

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')

def expected(core_only, desktop=False):
    inv = {}
    files = glob.glob(os.path.join(ROOT, 'app/src/test/**/*.kt'), recursive=True)
    if desktop:
        core_only = True      # the desktop build compiles and runs only the shared core tests from app/
        files += glob.glob(os.path.join(ROOT, 'desktop/src/test/**/*.kt'), recursive=True)
    for f in files:
        src = open(f, encoding='utf-8').read()
        n = len(re.findall(r'^\s*@Test\b', src, re.M))
        if n == 0:
            continue
        pkg = re.search(r'^package\s+([\w.]+)', src, re.M).group(1)
        cls = re.search(r'^(?:internal\s+|private\s+)?class\s+(\w+)', src, re.M).group(1)
        if '@Ignore' in src:
            sys.exit(f'{f}: @Ignore is not allowed; a suite that does not run must be fixed or removed')
        in_desktop = '/desktop/src/test/' in f.replace(os.sep, '/')
        if core_only and not pkg.endswith('.core') and not in_desktop:
            continue
        inv[f'{pkg}.{cls}'] = n
    return inv

def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument('--results', default=None)
    ap.add_argument('--core-only', action='store_true')
    ap.add_argument('--desktop', action='store_true')
    a = ap.parse_args(argv)
    if a.results is None:
        a.results = os.path.join(ROOT, 'desktop/build/test-results/test' if a.desktop else 'app/build/test-results/testDebugUnitTest')
    inv = expected(a.core_only, a.desktop)
    problems = []; total = 0
    for cls, n in sorted(inv.items()):
        path = os.path.join(a.results, f'TEST-{cls}.xml')
        if not os.path.exists(path):
            problems.append(f'NOT RUN: {cls} ({n} tests expected) has no result file'); continue
        r = ET.parse(path).getroot()
        t, s, fl, er = (int(r.get(k, 0)) for k in ('tests', 'skipped', 'failures', 'errors'))
        total += t
        if t != n: problems.append(f'COUNT MISMATCH: {cls} ran {t}, source declares {n}')
        if s:      problems.append(f'SKIPPED: {cls} skipped {s}')
        if fl or er: problems.append(f'FAILED: {cls} failures={fl} errors={er}')
    print(f'test inventory from source: {len(inv)} classes, {sum(inv.values())} tests; executed {total}')
    if problems:
        print('\n'.join(problems)); print('test verification FAILED'); return 1
    print('test verification passed: every declared suite ran, none skipped or failed')
    return 0

if __name__ == '__main__':
    sys.exit(main())
