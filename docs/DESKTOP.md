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
Desktop plays **Solo and Daily, local Team (pass the Deck) and local Vindictive**, with sound effects, all difficulties and categories,
hints (they cost a point and need a point to buy, as on Android), saves, Continue, results, score/ledger banking, the 25 backgrounds, and the Hot Attic
and game splashes. **Not ported:** online play (Firebase), background music, profiles/login and per-profile colours, the Stats screen,
used-word history, tutorials, About/diagnostics. Details, reasons and recommendations: `docs/DESKTOP_PARITY.md`.
Team and Vindictive reuse the shared rules (`TeamRules`, `VindictiveRules`, `Economy`, `vindictiveTaunt`) but the turn sequencing is written
again in `GameController` (Android's lives inside its screen code); keep the two in step until it is extracted into `core/`.
Player names are fixed ("Player 1", "Player 2") so no text entry is ever needed on a handheld.

## Controls
- Keyboard: letters, arrows, Tab/Shift+Tab next/previous clue, Space flip, Backspace, F1 hint,
  Esc menu, F11 / Alt+Enter fullscreen.
- Mouse / touch (Deck touchscreen arrives as mouse): click squares, clue list, header buttons
  (Keys / Hint / Menu), menu rows (left third of an option row = back, rest = forward), on-screen keys.
- Controller (Steam Input Xbox-style / any xpad): D-pad or sticks move, A = select / open the on-screen
  keyboard, B = back/close, X = flip, Y = hint, LB/RB = previous/next clue, Start = menu, Back = keyboard.
  The on-screen keyboard is fully D-pad navigable (every A-Z key reachable, tested). Steam's own
  keyboard (Steam+X) also works because it delivers ordinary key events.

## Save compatibility and Steam Cloud
- Same text format as Android (`SessionCodec` v2, shared code) but a **different store**: Android keeps saves in SharedPreferences, desktop in files. Nothing syncs and no save is converted; do not assume a desktop save can be copied to a phone.
- Desktop files: `profile.properties` (score, Daily completions, payout ledger, settings) and `saves/*.save` (one per slot). Writes are atomic (temp file then move). The previous good save is kept as `.bak`; a file that cannot be read is moved to `corrupt/` (never deleted), the `.bak` is tried, and the player sees a notice on the menu.
- Local-only settings: window/fullscreen, background, prompt style, sound. These must stay out of Steam Cloud.
- Steam Cloud (not integrated, needs a Steamworks app): sync `profile.properties` (without the local settings, split them into their own file first) and `saves/` from the per-OS data directory; resolve conflicts per slot by newest `elapsed`/modified time and keep the loser as `.bak`; never sync `corrupt/`. The ledger and Daily set must merge as unions.

## Save locations
Windows `%APPDATA%\HotAtticGames\MyPersonalCrossword`; Linux `$XDG_DATA_HOME` or
`~/.local/share/HotAtticGames/MyPersonalCrossword` (Deck: `/home/deck/.local/share/...`);
`MPC_DATA_DIR` overrides. Contents: `profile.properties`, `saves/*.save` (identical text format to
Android's `SessionCodec` v2). Desktop saves are **not** shared with Android (no sync exists).

## Build, package, test
```
./gradlew -p desktop test                      # shared core + desktop tests (258)
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

## Performance note
The window repaints only when something visible changed (input, a time-driven change such as the puzzle clock or splash fade). Measured in the sandbox (Xvfb, software rendering, so only the ratio means anything): idle menu CPU 14.3% of a core before, 1.6% after. Real Deck battery/thermals are not measured.

## Verification status (labels)
| Item | Status |
|---|---|
| Rules, saves, Daily, layout, input mapping, gamepad decoding (simulated), persistence | **Unit-tested** (204 tests incl. shared core) |
| Window flow, real key events (Robot), resize, fullscreen toggle, save on close, restore after restart | **Integration-tested** under Xvfb (dev jar) |
| Linux app-image/tarball: unpack, launch, full play-through, partial+resume | **Packaged and launch-tested** (sandbox, Xvfb, 1280x800) |
| Windows package | **Not yet verified** (defined in CI only; never built or launched) |
| Real gamepad via `/dev/input/js*`, Steam Input, Gamescope, Deck touch, Deck performance, battery | **Not yet verified** (no hardware) |
| Steam Deck Verified | **Not claimed** |
