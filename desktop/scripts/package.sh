#!/usr/bin/env bash
# Builds the distributable for the OS it runs on (jpackage bundles its own Java runtime, so the
# player installs nothing). Run from anywhere:  desktop/scripts/package.sh [linux|windows]
# Names follow the studio standard ("Game Title Build NN Linux.tar.gz" / "... EXE.zip"), N from VERSION.
#   linux   -> dist/My Personal Crossword Build <N> Linux.tar.gz  (+ checksums-linux.sha256; Steam / Steam Deck layout)
#   windows -> dist/My Personal Crossword Build <N> EXE.zip       (+ checksums-windows.sha256; app-image: MyPersonalCrossword.exe)
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
    cp packaging/linux/README-STEAM.txt "$ROOT/README.txt"; cp ../docs/THIRD_PARTY_ASSETS.md "$ROOT/"; cp -r packaging/licenses "$ROOT/licenses"
    chmod +x "$ROOT/steam-launch.sh"
    OUT="dist/My Personal Crossword Build $VERSION Linux.tar.gz"
    tar -C build/jpackage -czf "$OUT" "$APP" ;;
  windows)
    jpackage "${COMMON[@]}" --icon packaging/icon.ico
    cp ../docs/THIRD_PARTY_ASSETS.md "build/jpackage/$APP/"; cp -r packaging/licenses "build/jpackage/$APP/licenses"
    OUT="dist/My Personal Crossword Build $VERSION EXE.zip"
    (cd build/jpackage && powershell -NoProfile -Command "Compress-Archive -Path $APP -DestinationPath ../../$OUT -Force" 2>/dev/null) \
      || (cd build/jpackage && zip -qr "../../$OUT" "$APP") ;;
  *) echo "unsupported platform: $PLATFORM" >&2; exit 2 ;;
esac
(cd dist && sha256sum "$(basename "$OUT")" > "checksums-$PLATFORM.sha256")   # checksum next to the artifact
echo "$OUT"
