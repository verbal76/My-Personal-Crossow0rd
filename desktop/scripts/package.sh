#!/usr/bin/env bash
# Builds the distributable for the OS it runs on (jpackage bundles its own Java runtime, so the
# player installs nothing). Run from anywhere:  desktop/scripts/package.sh [linux|windows]
#   linux   -> dist/MyPersonalCrossword-linux-x86_64-v<N>.tar.gz   (Steam / Steam Deck layout)
#   windows -> dist/MyPersonalCrossword-windows-x64-v<N>.zip       (app-image: MyPersonalCrossword.exe)
# jpackage cannot cross-compile, so each target must be built on its own OS (see desktop.yml).
set -euo pipefail
cd "$(dirname "$0")/.."
PLATFORM="${1:-$(case "$(uname -s)" in Linux*) echo linux;; MINGW*|MSYS*|CYGWIN*) echo windows;; *) echo unsupported;; esac)}"
VERSION="$(tr -d '[:space:]' < ../VERSION)"
APP=MyPersonalCrossword
GRADLE=(../gradlew); [ -x ../gradlew ] || GRADLE=(gradle)
"${GRADLE[@]}" --no-daemon -q jar
rm -rf build/jpackage dist; mkdir -p build/jpackage dist

COMMON=(--type app-image --name "$APP" --input build/libs --main-jar MyPersonalCrossword.jar
        --main-class com.hag.mypersonalcrossword.desktop.MainKt --app-version "$VERSION.0.0"
        --vendor "Hot Attic Games" --description "My Personal Crossword" --dest build/jpackage
        --add-modules java.base,java.desktop,java.logging,java.prefs,java.xml
        --java-options "-Xmx512m" --java-options "-Dsun.java2d.opengl=false")

case "$PLATFORM" in
  linux)
    jpackage "${COMMON[@]}" --icon packaging/icon.png
    ROOT="build/jpackage/$APP"
    cp packaging/linux/steam-launch.sh packaging/linux/MyPersonalCrossword.desktop packaging/icon.png "$ROOT/"
    cp packaging/linux/README-STEAM.txt "$ROOT/README.txt"
    chmod +x "$ROOT/steam-launch.sh"
    OUT="dist/$APP-linux-x86_64-v$VERSION.tar.gz"
    tar -C build/jpackage -czf "$OUT" "$APP" ;;
  windows)
    jpackage "${COMMON[@]}" --icon packaging/icon.ico
    OUT="dist/$APP-windows-x64-v$VERSION.zip"
    (cd build/jpackage && powershell -NoProfile -Command "Compress-Archive -Path $APP -DestinationPath ../../$OUT -Force" 2>/dev/null) \
      || (cd build/jpackage && zip -qr "../../$OUT" "$APP") ;;
  *) echo "unsupported platform: $PLATFORM" >&2; exit 2 ;;
esac
echo "$OUT"
