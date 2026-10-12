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

# The two annotation spellings JUnit 4 tests use here. Anything else that looks like a test
# annotation (kotlin.test, JUnit 5 ...) is rejected outright rather than silently not counted.
TEST_ANNOTATION = re.compile(r'^\s*@(?:org\.junit\.)?Test\b', re.M)
ANY_TEST_LIKE   = re.compile(r'^\s*@([\w.]*\.)?Test\b', re.M)
IGNORE          = re.compile(r'@(?:org\.junit\.)?Ignore\b')
TOP_CLASS       = re.compile(r'^(?:(?:internal|private|open|abstract|public)\s+)*class\s+(\w+)', re.M)

def scan_source(src, name='<source>'):
    """{package.Class: declared @Test count} for the TOP-LEVEL classes of one Kotlin test file.
    Counts both `@Test` and `@org.junit.Test`; each class gets only the tests declared in its own body."""
    if IGNORE.search(src):
        sys.exit(f'{name}: @Ignore is not allowed; a suite that does not run must be fixed or removed')
    for m in ANY_TEST_LIKE.finditer(src):
        if m.group(0).strip() not in ('@Test', '@org.junit.Test') and not m.group(0).strip().startswith(('@Test(', '@org.junit.Test(')):
            sys.exit(f'{name}: unsupported test annotation {m.group(0).strip()!r}; use @Test or @org.junit.Test so the suite is counted')
    pkg = re.search(r'^package\s+([\w.]+)', src, re.M)
    pkg = pkg.group(1) if pkg else ''
    spans = [(m.group(1), m.start()) for m in TOP_CLASS.finditer(src)]
    out = {}
    for i, (cls, start) in enumerate(spans):
        end = spans[i + 1][1] if i + 1 < len(spans) else len(src)
        n = len(TEST_ANNOTATION.findall(src[start:end]))
        if n: out[f'{pkg}.{cls}'] = n
    declared = len(TEST_ANNOTATION.findall(src))
    if declared != sum(out.values()):
        sys.exit(f'{name}: {declared} @Test annotations but {sum(out.values())} attributed to top-level classes; tests outside a class are not counted')
    return out

def expected(core_only, desktop=False):
    inv = {}
    files = glob.glob(os.path.join(ROOT, 'app/src/test/**/*.kt'), recursive=True)
    if desktop:
        core_only = True      # the desktop build compiles and runs only the shared core tests from app/
        files += glob.glob(os.path.join(ROOT, 'desktop/src/test/**/*.kt'), recursive=True)
    for f in files:
        in_desktop = '/desktop/src/test/' in f.replace(os.sep, '/')
        for cls, n in scan_source(open(f, encoding='utf-8').read(), f).items():
            if core_only and not cls.rsplit('.', 1)[0].endswith('.core') and not in_desktop:
                continue
            inv[cls] = n
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
