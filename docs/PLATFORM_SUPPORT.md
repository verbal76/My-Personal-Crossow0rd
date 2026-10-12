# Platform support (studio standard: one source line, three targets)

Authoritative source line: `AUTHORITATIVE_BRANCH`. Shared: everything in `core/` (rules, generation, Daily,
economy, save codec, input engine). Adapters: Android UI/audio/Firebase in `app/`; desktop Swing UI, paths,
gamepad in `desktop/`. No platform-specific gameplay branches.

| Target | Output | Built by | Status (evidence) |
|---|---|---|---|
| Android | `My Personal Crossword Build <N> APK.apk` (v10 and earlier: `My-Personal-Crossword-v<N>.apk`; AAB workflow dormant) | `build.yml` (manual dispatch) | v10 built in CI; awaiting owner physical-device test |
| Windows x64 | `My Personal Crossword Build <N> EXE.zip` (jpackage app-image, bundled Java) | `build.yml` (manual, Windows runner) | defined only; never built or launched |
| Linux x86-64 | `My Personal Crossword Build <N> Linux.tar.gz` (+ `steam-launch.sh`) | `build.yml` or `desktop/scripts/package.sh linux` | built, unpacked, launch-tested in sandbox (Xvfb); no hardware test |

Coordinated interface: ONE manual workflow, `build.yml` (`platform` = android|windows|linux|all, `source_sha`, `build_number`,
`destination` = local_only|github_releases); see `docs/RELEASES.md`. Windows has never been built or launched.
Desktop scope (Solo, Daily, local Team, local Vindictive, sound effects; no online, no music) and the verification labels: `docs/DESKTOP.md`
and `docs/DESKTOP_PARITY.md`.
