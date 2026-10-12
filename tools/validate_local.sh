#!/usr/bin/env bash
# Local validation that costs zero GitHub Actions minutes:
#  1. the pure-Kotlin core compiles and its unit tests pass (plain JVM, no Android SDK);
#  2. workflow YAML parses;
#  3. the VERSION file is a positive integer;
#  4. branch accountability (tools/branch_audit.py): every ref is on, or ledgered against, the
#     authoritative branch, and release tags are on that line.
#  5. the desktop build (Windows / Linux / Steam Deck front end, desktop/): shared core tests +
#     desktop tests on a plain JVM. Set SKIP_DESKTOP=1 to skip. The packaged launch test needs
#     Xvfb and runs separately: desktop/scripts/package.sh && desktop/scripts/smoke.sh <launcher>.
# What this cannot prove: Android compilation, lint and APK packaging (those need the
# Android SDK, which cloud sessions often cannot download). Batch those into one CI build.
set -euo pipefail
cd "$(dirname "$0")/.."
GRADLE="${GRADLE:-$(command -v gradle || echo /opt/gradle/bin/gradle)}"
# PR_MODE=1 (pull-request CI) skips the standalone core run: the desktop build below compiles and runs
# the very same core sources and tests, and the inventory check still demands every suite.
if [ -z "${PR_MODE:-}" ]; then
  (cd tools/core-jvm && "$GRADLE" test -q "$@")
  # every core test class declared in source ran, none skipped (same check CI runs for all classes)
  python3 tools/verify_test_results.py --core-only --results tools/core-jvm/build/test-results/test
fi
if [ -z "${SKIP_DESKTOP:-}" ]; then
  (cd desktop && "$GRADLE" test -q "$@")
  python3 tools/verify_test_results.py --desktop
fi
# passing and deliberately failing cases for the release-source gate, manifest check and test verifier
python3 -W ignore tools/test_release_source.py
if [ -n "${PR_MODE:-}" ]; then
  python3 tools/branch_audit.py || echo "::warning::branch audit reported unaccounted refs; see docs/BRANCH_LEDGER.md"
else
  python3 tools/branch_audit.py
fi
python3 - <<'PY'
import yaml, glob, re, sys
for f in glob.glob('.github/workflows/*.yml'):
    yaml.safe_load(open(f)); print('yaml ok:', f)
v = open('VERSION').read().strip()
assert re.fullmatch(r'[1-9][0-9]*', v), 'VERSION must be a positive integer'
print('VERSION', v)
PY
echo "local validation passed"
