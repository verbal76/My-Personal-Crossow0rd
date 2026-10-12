#!/usr/bin/env bash
# Launch smoke test of a PACKAGED build (the unpacked app-image / tarball), not the dev jar.
#   desktop/scripts/smoke.sh <path-to-launcher> [shots-dir]
# Needs a display: on a headless Linux box it re-runs itself under xvfb-run at 1280x800 (the Deck's screen).
# Phases: full -> modes (Team + Vindictive) -> partial -> resume (the last two share a data dir to prove save-on-close + restore after restart).
set -euo pipefail
LAUNCHER="$(cd "$(dirname "$1")" && pwd)/$(basename "$1")"
SHOTS="${2:-$(pwd)/smoke-shots}"; mkdir -p "$SHOTS"
if [ -z "${DISPLAY:-}" ] && [ "$(uname -s)" = Linux ] && [ -z "${SMOKE_UNDER_XVFB:-}" ]; then
  exec env SMOKE_UNDER_XVFB=1 xvfb-run -a -s "-screen 0 1280x800x24" "$0" "$@"
fi
WORK="$(mktemp -d)"; trap 'rm -rf "$WORK"' EXIT
fail=0
run() { # phase extra-args...
  local phase="$1"; shift
  echo "=== smoke: $phase"
  rm -f "$SHOTS/smoke-$phase.log"
  timeout 240 "$LAUNCHER" --smoke="$phase" --data-dir="$WORK/data" --shots="$SHOTS" "$@" >/dev/null 2>&1 || fail=1
  # The launcher writes its report to a file (a packaged Windows GUI exe has no console).
  cat "$SHOTS/smoke-$phase.log" 2>/dev/null || { echo "no report written"; fail=1; }
  grep -q "SMOKE-RESULT phase=$phase ok=true" "$SHOTS/smoke-$phase.log" 2>/dev/null || fail=1
}
mkdir -p "$WORK/data"
run full --size=1280x800
rm -rf "$WORK/data"; mkdir -p "$WORK/data"
run modes --size=1280x800
rm -rf "$WORK/data"; mkdir -p "$WORK/data"
run partial --skip-splash
run resume --skip-splash
[ $fail -eq 0 ] && echo "PACKAGED SMOKE PASSED" || { echo "PACKAGED SMOKE FAILED"; exit 1; }
