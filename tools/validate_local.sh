#!/usr/bin/env bash
# Local validation that costs zero GitHub Actions minutes:
#  1. the pure-Kotlin core compiles and its unit tests pass (plain JVM, no Android SDK);
#  2. workflow YAML parses;
#  3. the VERSION file is a positive integer.
# What this cannot prove: Android compilation, lint and APK packaging (those need the
# Android SDK, which cloud sessions often cannot download). Batch those into one CI build.
set -euo pipefail
cd "$(dirname "$0")/.."
GRADLE="${GRADLE:-$(command -v gradle || echo /opt/gradle/bin/gradle)}"
(cd tools/core-jvm && "$GRADLE" test -q "$@")
python3 - <<'PY'
import yaml, glob, re, sys
for f in glob.glob('.github/workflows/*.yml'):
    yaml.safe_load(open(f)); print('yaml ok:', f)
v = open('VERSION').read().strip()
assert re.fullmatch(r'[1-9][0-9]*', v), 'VERSION must be a positive integer'
print('VERSION', v)
PY
echo "local validation passed"
