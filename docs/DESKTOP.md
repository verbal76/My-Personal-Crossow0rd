# Desktop build (Windows x64 and Linux x86-64 / Steam Deck)

One source tree, three targets. Android is untouched and remains the reference. The desktop
front end lives in `desktop/` (its own Gradle build) and compiles **the same `core` sources
Android compiles** (`srcDir` onto `app/src/main/java/.../core`, no copy). Nothing under `app/` changed.

## Why Swing, not Compose Multiplatform
Compose Desktop needs `androidx.*` artifacts from Google Maven, which cloud sandboxes block, and
sharing Android's Compose UI would have meant a major Android rewrite (explicitly not allowed).
The desktop UI is therefore Java Swing/Java2D (JDK only, plus Maven Central for Kotlin/JUnit).
Trade-off: the screens are a second UI implementation; **all rules, generation, Daily, economy,
save format and input logic are shared** (`GameController` only calls core: `InputEngine`,
`Economy`, `pickHintCell`, `DailyPuzzle`, `generateCrossword`, `SessionCodec`, `puzzleFingerprint`).

## Layout
| File | Role |
|---|---|
| `GameController.kt` | puzzle state + rules over the shared core (no AWT) |
| `AppModel.kt` | screen flow (studio logo, game splash, menu, puzzle, pause, results) + input routing |
| `Actions.kt`, `Gamepad.kt` | keyboard map; joydev decoder, Xbox-layout mapper, `/dev/input/js*` reader |
| `Layout.kt` | pure layout maths (1280x800 reference, scales; on-screen keyboard geometry) |
| `GamePanel.kt`, `Main.kt` | Java2D painting, window, fullscreen, clean shutdown |
| `Persistence.kt`, `Platform.kt` | file-backed profile (atomic writes, shared `SessionCodec`), per-OS data dir |
| `Smoke.kt` | launch test driven against the real window |

## Scope (be honest about it)
Desktop plays **SINGLE and DAILY** (all difficulties, categories, hints, saves, Continue, results,
score/ledger, the 25 backgrounds, Hot Attic and game splashes). **Not ported:** Team, Vindictive,
online play, sound/music, per-profile colours, stats screen, used-word history. These are not
desktop gameplay branches; they are missing UI/adapters on top of already-shared rules.

## Controls
- Keyboard: letters, arrows, Tab/Shift+Tab next/previous clue, Space flip, Backspace, F1 hint,
  Esc menu, F11 / Alt+Enter fullscreen.
- Mouse / touch (Deck touchscreen arrives as mouse): click squares, clue list, header buttons
  (Keys / Hint / Menu), menu rows (left third of an option row = back, rest = forward), on-screen keys.
- Controller (Steam Input Xbox-style / any xpad): D-pad or sticks move, A = select / open the on-screen
  keyboard, B = back/close, X = flip, Y = hint, LB/RB = previous/next clue, Start = menu, Back = keyboard.
  The on-screen keyboard is fully D-pad navigable (every A-Z key reachable, tested). Steam's own
  keyboard (Steam+X) also works because it delivers ordinary key events.

## Save locations
Windows `%APPDATA%\HotAtticGames\MyPersonalCrossword`; Linux `$XDG_DATA_HOME` or
`~/.local/share/HotAtticGames/MyPersonalCrossword` (Deck: `/home/deck/.local/share/...`);
`MPC_DATA_DIR` overrides. Contents: `profile.properties`, `saves/*.save` (identical text format to
Android's `SessionCodec` v2). Desktop saves are **not** shared with Android (no sync exists).

## Build, package, test
```
./gradlew -p desktop test                      # shared core + desktop tests (212)
desktop/scripts/package.sh linux|windows       # jpackage app-image + tar.gz / zip (own OS only)
desktop/scripts/smoke.sh <unpacked launcher>   # packaged launch test (Xvfb on headless Linux)
tools/validate_local.sh                        # everything that needs no CI minutes
```
CI: `.github/workflows/desktop.yml` is `workflow_dispatch` only (inputs `platform`, `source_sha`, `build_number`,
`destination` local_only|github_releases), one immutable SHA for every job, release-source gate, tests, package, packaged launch
test, per-platform provenance manifest, artifacts only (no Release, no Steam, no signing). The gate
requires the commit to be on the authoritative line, so the desktop branch must first be approved
onto `AUTHORITATIVE_BRANCH`.

## Steam / Deck
Launch: `steam-launch.sh [--fullscreen]` (bundled Java 21, no install). Layout 1280x800 / 16:10 is the
design reference and scales up/down (tested 800x500 to 4K). No Steamworks SDK is used or needed to run.
Controller prompts use the studio glyph library (`resources/glyphs/NOTICE.txt`); the menu has Button prompts: Auto / Controller / Keyboard.
Not done / needs the owner: Steamworks app + depots, store assets, Steam Input config upload,
code signing for Windows, Steam Cloud, Deck Verified review.

## Verification status (labels)
| Item | Status |
|---|---|
| Rules, saves, Daily, layout, input mapping, gamepad decoding (simulated), persistence | **Unit-tested** (204 tests incl. shared core) |
| Window flow, real key events (Robot), resize, fullscreen toggle, save on close, restore after restart | **Integration-tested** under Xvfb (dev jar) |
| Linux app-image/tarball: unpack, launch, full play-through, partial+resume | **Packaged and launch-tested** (sandbox, Xvfb, 1280x800) |
| Windows package | **Not yet verified** (defined in CI only; never built or launched) |
| Real gamepad via `/dev/input/js*`, Steam Input, Gamescope, Deck touch, Deck performance, battery | **Not yet verified** (no hardware) |
| Steam Deck Verified | **Not claimed** |
