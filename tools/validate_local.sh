#!/usr/bin/env bash
# Local validation that costs zero GitHub Actions minutes:
#  1. the pure-Kotlin core compiles and its unit tests pass (plain JVM, no Android SDK);
#  2. workflow YAML parses;
#  3. the VERSION file is a positive integer;
#  4. branch accountability (tools/branch_audit.py): every ref is on, or ledgered against, the
#     authoritative branch, and release tags are on that line.
# What this cannot prove: Android compilation, lint and APK packaging (those need the
# Android SDK, which cloud sessions often cannot download). Batch those into one CI build.
set -euo pipefail
cd "$(dirname "$0")/.."
GRADLE="${GRADLE:-$(command -v gradle || echo /opt/gradle/bin/gradle)}"
(cd tools/core-jvm && "$GRADLE" test -q "$@")
# every core test class declared in source ran, none skipped (same check CI runs for all classes)
python3 tools/verify_test_results.py --core-only --results tools/core-jvm/build/test-results/test
# passing and deliberately failing cases for the release-source gate, manifest check and test verifier
python3 tools/test_release_source.py
python3 tools/branch_audit.py
python3 - <<'PY'
import yaml, glob, re, sys
for f in glob.glob('.github/workflows/*.yml'):
    yaml.safe_load(open(f)); print('yaml ok:', f)
v = open('VERSION').read().strip()
assert re.fullmatch(r'[1-9][0-9]*', v), 'VERSION must be a positive integer'
print('VERSION', v)
PY
echo "local validation passed"
