# Platform support (studio standard: one source line, three targets)

Authoritative source line: `AUTHORITATIVE_BRANCH`. Shared: everything in `core/` (rules, generation, Daily,
economy, save codec, input engine). Adapters: Android UI/audio/Firebase in `app/`; desktop Swing UI, paths,
gamepad in `desktop/`. No platform-specific gameplay branches.

| Target | Output | Built by | Status (evidence) |
|---|---|---|---|
| Android | `My-Personal-Crossword-v<N>.apk` (AAB workflow dormant) | `build.yml` (tag/manual) | v10 built in CI; awaiting owner physical-device test |
| Windows x64 | `My Personal Crossword Build <N> EXE.zip` (jpackage app-image, bundled Java) | `desktop.yml` (manual, Windows runner) | defined only; never built or launched |
| Linux x86-64 | `My Personal Crossword Build <N> Linux.tar.gz` (+ `steam-launch.sh`) | `desktop.yml` or `desktop/scripts/package.sh linux` | built, unpacked, launch-tested in sandbox (Xvfb); no hardware test |

Coordinated interface today: `build.yml` (Android) and `desktop.yml` (`platform`, `source_sha`, `build_number`,
`destination`). A single `platform=all` workflow that includes Android is not implemented (owner decision, see
`docs/DESKTOP.md`). Desktop scope (SINGLE + DAILY only) and verification labels: `docs/DESKTOP.md`.
