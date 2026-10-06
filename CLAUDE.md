# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Release naming (read first)
Public versions are `My Personal Crossword v<N>`, sequential, from the root `VERSION` file. Bump it for every playable build delivered to the owner; never use codenames or build/SHA in names the owner reads. Full rules: `docs/RELEASES.md`.

## GitHub Actions budget policy (standing owner directive)
GitHub-hosted Actions minutes are shared across the owner's projects and the monthly budget is small. Treat them as scarce.
- Before starting any workflow ask: "Does this need GitHub Actions, or can I prove it locally?" Run `tools/validate_local.sh` first (core compile + all core tests on a plain JVM + workflow YAML + VERSION check, zero minutes).
- `.github/workflows/build.yml` does **not** run on ordinary pushes or pull requests. It runs only on a pushed `build-*` tag (a delivered build: tests + lint + APK + Release) or a manual Run workflow (tests + lint; tick `build_apk` / `publish_release` for more). Docs, comments, research and bookkeeping commits therefore cost nothing.
- Only the Android compile, lint and APK packaging need CI (cloud sessions often cannot download the Android SDK). Batch Android-affecting changes, prove the core logic locally, then spend one build for a candidate that is actually approaching physical testing or release.
- Do not rebuild the same SHA; reuse an existing verified artifact. Do not re-run just to see whether a flaky result passes. No Windows/other-platform builds unless asked. The concurrency group cancels a superseded run on the same ref.
- Release safety is not negotiable: never bypass signing checks, the APK inspection (16 KB, permissions, signer), runtime compatibility checks or rollback protections to save minutes.
- To deliver a build: bump `VERSION`, commit and push (free), then `git tag build-v<N> && git push origin build-v<N>`.

## Build Commands

```bash
./gradlew assembleDebug          # debug APK (signed with the committed debug.keystore)
./gradlew assembleRelease        # release APK (unsigned config, not minified)
./gradlew testDebugUnitTest      # JVM unit tests (core engine + shipped word list)
./gradlew lintDebug              # Android lint
./gradlew connectedAndroidTest   # instrumented tests (device/emulator; template only)
./gradlew clean
```

**Build configuration:** Gradle 9.6.1, AGP 9.4.0, Kotlin 2.3.21, compileSdk 36.1 / targetSdk 36, minSdk 24, Java 11 bytecode (build runs on JDK 17+), Compose BOM 2026.06.01, lifecycle 2.10.0 (runtime + viewmodel-compose), activity-compose 1.13.0, core-ktx 1.18.0, Firebase BoM 34.12.0 (Auth, Realtime Database only; no analytics or ads SDKs). Compose 1.12 / lifecycle 2.11 / core 1.19 and newer require compileSdk 37, so they are held back until compileSdk is deliberately raised; see `docs/STACK.md`.

**CI** (`.github/workflows/build.yml`) is deliberately opt-in (see the budget policy above): it runs on a pushed `build-*` tag or a manual run, never on ordinary pushes or PRs. A build runs unit tests → `assembleDebug` (APK renamed `My-Personal-Crossword-v<N>.apk`, uploaded as the `My-Personal-Crossword-v<N>-<commit>` artifact, 14-day retention) → an APK inspection step (package, permissions, native libraries, 16 KB alignment, signer, SHA-256 in the log) → on a tag or `publish_release`, a GitHub Release `v<N>` created once per `VERSION` and marked Latest → `lintDebug`. `<N>` comes from the root `VERSION` file and becomes `versionName` `v<N>`; the build also carries its commit, build number and build time as `BuildConfig` fields, shown in Settings → Build details. In CI `versionCode` is `1000 + GITHUB_RUN_NUMBER` (monotonic across branches, so every APK installs over the last; local builds fall back to the commit count). When the `RELEASE_KEYSTORE_BASE64` secret exists, an APK build also produces a signed release APK (`app-release-apk-<commit>`); see `docs/EXTERNAL_ACTIONS.md`. Test and lint reports are uploaded as the `reports` artifact only when a run fails.

**Cloud sessions:** Claude Code on the web may block `dl.google.com`, which serves the Android SDK, AGP and AndroidX. In that case the Android build only runs in CI. The pure-Kotlin `core` package and its tests compile locally on a plain JVM: run `tools/validate_local.sh` (project in `tools/core-jvm`, source sets pointing at `app/src/main/java/.../core` and `app/src/test/java/.../core`, `crossword.assets` set for you). Maven Central occasionally rate-limits (HTTP 429); retry after a few seconds.

## Package / layout

The Kotlin package is **`com.hag.mypersonalcrossword`**, but the source folders are still `com/example/mypersonalcrossword/`. Kotlin allows the mismatch; don't "fix" one side without the other.

| Path | What lives there |
|---|---|
| `core/` (package `…core`) | **Pure Kotlin, no Android imports.** Everything testable: model, generator, word-list parsing, Daily, economy, Team/Vindictive rules, input engine, save sessions, sound synthesis, background names. |
| `MainActivity.kt` | Activity, `SaveManager` (SharedPreferences), `SoundPlayer` (SoundPool), `AmbientMusicPlayer` (MediaPlayer + audio focus), `Haptics`, `FirebaseGameManager`, and the `CrosswordApp()` root composable with every screen and dialog. Still large (~6k lines). |
| `PuzzleViewModel.kt` | Owns all puzzle state and mirrors it into `SavedStateHandle`. |
| `CrosswordBoardUi.kt` | `CrosswordGrid` (Canvas), `ClueBar`, `CrosswordKeyboard`. |
| `ui/theme/` | Material 3 colour scheme, typography ramp, theme. |
| `app/src/test/.../core/` | JVM unit tests (~140), including a golden Daily snapshot. |
| `firebase/database.rules.json` | Realtime Database rules, deployed from the Firebase Console (see `docs/EXTERNAL_ACTIONS.md`). |
| `docs/` | `BRANCHES.md` (why this line of history is authoritative), `ECONOMY.md` (scoring audit), `EXTERNAL_ACTIONS.md` (console tasks). |

## Architecture

**Single activity, Compose, no navigation library, no Room.** `CrosswordApp()` switches screens with `when (appMode)`:

- `LOGIN`: pick or create a profile. Profile cards continue straight home; a separate icon opens Stats.
- **Startup sequence:** Android shows a plain black system splash, then `StudioSplash` (the Hot Attic Games logo, `STUDIO_SPLASH_MS`), then `OpeningCard` (the My Personal Crossword splash art, at least `OPENING_CARD_MIN_MS` and until startup loading finishes, capped), then home or login. Both images are byte-identical copies of the files in `branding/` (`StudioSplashTest` enforces this). The old purple "Welcome" title card is retired; `TitleMark` now only appears on the login screen. The Daily prompt waits for the splash, because dialogs draw above it.
- `STATS`: summary tiles (score, puzzles, Daily streak), in-progress saves, personal bests grouped by mode.
- `CATEGORY_SELECT`: the home hub. Header (with the Settings gear), Continue card, Daily card, play-mode picker with START, and the category carousel.
- `ONLINE_LOBBY`: host shows the code; guest waits.
- `DASHBOARD`: the puzzle itself.

### State ownership
- **`PuzzleViewModel`** holds the puzzle: words, grid cells, letters, hint reveals, selection, timer, mode, category/combined/difficulty, Daily date key, Player 2, Team and Vindictive state, and background. It serialises a `PuzzleSession` into `SavedStateHandle` (debounced), so rotation, theme change and process death restore the grid in place. The online session (`isOnline`, `onlineRole`, `onlineCode`) also lives here so an Activity recreation doesn't drop a live game, but online games are never written to the handle. The manifest also handles `uiMode`, `fontScale`, `density` and `locale`, so most of those changes don't recreate the Activity at all.
- `goHome()` unloads the puzzle after saving it. A solved grid must never stay loaded: completion would re-run and pay out again after a recreation.
- The session is encoded into the `SavedStateHandle` by a saved-state provider at the moment the system saves state (never a debounced mirror, which could miss the last letter and repay a finished puzzle). A process restored from an online game returns home with a notice.
- Puzzle generation carries a request id (`generationId`); leaving or starting another puzzle invalidates it, so a late board is discarded.
- `CrosswordApp` binds to it with `var placedWords by vm::placedWords`. Assigning to these locals writes the ViewModel.
- UI-only state (dialogs, overlays, animations, online plumbing) is `remember`/`rememberSaveable` inside `CrosswordApp`.
- **Scoring and turn rules never live in UI code.** Always call `TeamRules`, `VindictiveRules` and `Economy`. Hints are charged to the player whose turn it is; in Vindictive a hint can't fill a word's last letter.
- **Colour:** the player's button colour goes through `readableUnderWhiteText` (core `Contrast.kt`, WCAG 4.5:1) because every button/header/tile puts white text on it; accent text on other surfaces uses `readableTextColor`.

### Game modes (`core.GameMode`)
- `SINGLE`: solo.
- `DAILY`: one deterministic puzzle per UTC day. Always solo, whatever mode was selected on home (`modeBeforeDaily` restores the selection afterwards).
- `TEAM`: two players alternate. Completely filling a word is one attempt, and the turn passes whether it was right or wrong. Local play uses "Pass the Phone" dialogs; online play gates typing to the current turn.
- `VINDICTIVE`: `PICK_OWN` (answer any clue, +1/−1) → `ASSIGN_CLUE` (pick a clue, tap 🎯 Give) → `OPPONENT_WAIT` (the opponent answers against a 15/30/60s timer, +1/−2; on timeout they may still answer, or pass for −1). The answerer then assigns next. The taunts are part of the mode's personality; keep them.

### Crossword input (`core/Input.kt` + `CrosswordBoardUi.kt`)
- Tap a square to select it. Tap it again to flip Across/Down.
- Typing uses the in-app keyboard. Physical keyboards also work: Tab/Enter moves to the next clue, Space flips direction.
- The cursor advances to the next empty square. Backspace erases, or steps back and erases.
- Locked cells (solved words and hint reveals) can't be edited and are skipped.
- A keystroke returns every word it completed; the game judges the **active** word as the attempt. Solo keeps wrong letters for correction; Team and Vindictive clear them.
- Hints reveal the selected square, or the first open square in the word, and are judged like typed letters.
- `CrosswordGrid` draws everything on one Canvas. Pan and zoom state is read only during draw, so gestures never recompose. Scale 1 fits the whole board; small cells open zoomed in.

### Puzzle generation (`core/Generator.kt`)
- Placement over a `length → position → char` index, up to `GENERATOR_ATTEMPTS` (50) boards, keeping the best-scoring one. Growth picks a random frontier cell, preferring the one nearest the anchor (`TOURNAMENT`); the board score counts words, crossings, bounding-box area and repeats, and it stops early only once a full board has extra crossings. Strict first-in-first-out growth made comb-shaped boards around one long spine; `boardsBranchInsteadOfFormingACombAroundOneSpine` guards this.
- **All randomness comes from the `rng` parameter.** A seeded `Random` gives an identical board. Maps are iterated in insertion order.
- Used words are deprioritised (per placement and per board), never banned. Each `PlacedWord` carries its source `category`.
- `numberBoard()` normalises the board to (0,0) and numbers clues in reading order.

### Daily Puzzle (`core/Daily.kt`)
- The date key is the UTC `yyyy-MM-dd` computed from epoch millis, always in ASCII digits (a locale-formatted key gave Arabic/Persian/… devices a different Daily and a second payout). Stored keys go through `DailyPuzzle.normalizeKey`; `epochDayOf` accepts only canonical keys.
- The seed is FNV-1a of (algorithm version, date key) XOR the word-list fingerprint. Randomness is the in-house `SplitMix64` with `fisherYates` (`core/Rng.kt`), not `kotlin.random`, whose seeded algorithm may change between Kotlin versions. `DailyGoldenTest` pins real boards; it fails if generation changes.
- The pool is sorted canonically before generation, and player history is never used. Same date and same word list give the same grid on every device.
- Rewards are paid once per UTC day per profile (`dailydone_<name>`). Daily words never enter per-category used-word history.
- Each day builds from its own seeded `DAILY_POOL_SIZE` (400) sample of the list; over the whole list the scoring always placed the same longest answers (four were in every Daily). `dailyAnswersVaryFromDayToDay` guards this.
- Bump `ALGORITHM_VERSION` (now 4) and re-pin `DailyGoldenTest` if generation changes on purpose.

### Persistence (`SaveManager`, SharedPreferences `CrosswordSaves`)
- **Saves:** one `PuzzleSession` per `SaveSlot`, stored under `psession_<name>_<slotId>` with an index in `saves_<name>`.
  - SINGLE slot ids keep the legacy `CATEGORY__DIFF` form. Legacy `puzzle_…` keys are still read and are removed on clear.
  - TEAM, VINDICTIVE and DAILY slots are namespaced (`TEAM::…`, `DAILY::2026-09-27__EXPERT`), so modes can't overwrite each other.
  - Online games are never saved. The puzzle autosaves on `ON_PAUSE`, back, and Save & Quit.
- **Stats:** best record per player + mode + category + difficulty. Solo keys are legacy (`stat_<p>_<cat>_<DIFF>`); other modes are namespaced. Better means higher score, then fewer hints, then faster time.
- **Settings:** sound, music, vibration and volume are device-wide and persisted. Cell and button colours are per profile.
- `deletePlayer` removes exactly the keys `core/PlayerKeys` says the profile owns (longest matching profile name wins), so deleting "Kev" can't touch "Kev_2". Add any new per-player key prefix to `PlayerKeys` and `PlayerKeysTest`.
- **Combined puzzles** save under `SaveSlot.forPuzzle(...)`: the sorted category list (`CITIES+FOOD__HARD`), not the display label (which was "4 Categories" for every 4+ mix). Older label-keyed saves move to the canonical slot when resumed.
- **Payout ledger** (`paid_<name>`, `core/Ledger.kt`): each finished board's fingerprint; a board pays out once even if a stale saved copy is restored.
- Unfinished Dailies older than a week are pruned from Continue.
- **Serialisation:** `SessionCodec` is versioned (`v2`) and skips malformed segments. Names are sanitised (`sanitizeName` strips `§ | ;` and newlines). Clues can't contain those characters (the CSV validator enforces it).

### Economy (`core/Economy.kt`, audit in `docs/ECONOMY.md`)
Easy/Medium/Hard/Expert/Genius pay 1/2/4/8/20. The Daily pays 20, once per day. A hint costs 1, **charged once when used**; the completion award is the full reward, so a puzzle nets reward − hints. Vindictive match scores are banked floored at 0. `EconomyTest` pins every value. **Update the audit before changing any number.**

### Audio / haptics
- `SoundSynth` (core) renders every effect, normalised to a 0.85 peak with click-free envelopes. `SoundPlayer` caches them as WAVs in `cacheDir` and plays them through one `SoundPool`. The correct-answer chime rises a semitone per streak step.
- `AmbientMusicPlayer` streams `assets/Music/*.mp3`:
  - Requests audio focus: pauses for calls, ducks for notifications.
  - Fades on start, stop and skip, and uses `prepareAsync`.
  - Stops after every track has failed instead of recursing.
  - `currentTrackName` and `isPlaying` **must stay Compose state**.
- `Haptics` uses system presets on API 29+ and respects the Vibration setting.

### Online play (`FirebaseGameManager`)
- Anonymous sign-in happens only at Host/Join, and failures are reported. Creating a game and claiming the guest seat are **transactions**.
- **Presence:** `trackPresence` keeps `hostOnline`/`guestOnline` true while connected; the server sets it false on a drop, and the hook is re-armed on every reconnect. The other phone waits 30 s before ending the game (a brief Wi-Fi → mobile switch used to end it at once). A flag that is absent (older build) counts as connected. Normal ends call `closeGame`; leaving mid-game calls `abandonGame`.
- Join codes are filtered/validated to the code alphabet (Firebase paths reject `. # $ [ ]`). Host/Join answers that arrive after a cancel are discarded. Failed writes are logged and shown (`onWriteRefused`).
- The listener is owned by `LaunchedEffect(onlineCode, isOnlineGame)` and removed in `finally`.
- The host publishes the puzzle exactly once, right after generating it.
- Each device writes only its own score field.
- Remote letters go through `mergeRemoteInputs`: only correct letters are accepted, and nothing is erased.
- The Vindictive timer runs only on the answerer's device (`VindictiveRules.ownsTimer`).
- Each device credits only its own player at completion.
- Game node fields must match `firebase/database.rules.json`. **Update the rules when adding a field**, because unknown fields are rejected.

## Assets
- **Word list:** `app/src/main/assets/test.csv` has columns `Answer,Clue,Category`, plain CSV with no quoting.
  - Answers: A–Z only, 3–30 letters. Multi-word answers are concatenated and their clue ends with "(no spaces)".
  - Clues: no commas and no `§ | ;`, and must not contain their own answer.
  - `WordDataTest` enforces all of this, so a bad edit fails CI.
- **Backgrounds:** two sets.
  - **The 25-picture collection** (SPACE, SKY, WOODS, MOTION CITY, OCEAN, five each) lives in `assets/backgrounds/<category>_<n>_<name>.webp` (portrait 1200x2600). `core/BackgroundCatalog.kt` lists them with titles and a per-picture dimming `scrim` laid over the image behind the board; `BackgroundCatalogTest` enforces 5x5, unique names and that the files ship. The art is original and procedurally generated by `tools/make_backgrounds.py` (deterministic; no photos, text or logos). To swap in better art, replace the webp with the same file name. The picker (`BgPickerScreen`) has a chip per category and shows its five as thumbnails; a CLASSIC chip holds the older art. Backgrounds are free personalization: no currency, unlocks or progression. New games start on a random collection picture.
  - **Older art:** `assets/images/drawable-xxhdpi` and `drawable-xxxhdpi` hold the same filenames (portrait only).
    - Titles come from `core/Backgrounds.kt`. **Add a curated title for new art**; `BackgroundsTest` fails otherwise.
    - Images are decoded downsampled and LRU-cached.
- **Music:** `assets/Music/*.mp3`, discovered at runtime.
- **Settings gear:** `res/drawable-nodpi/ic_settings_gear.png` (144×144, transparent), drawn by `SettingsGearButton` at 24 dp in a 48 dp touch target and never tinted. Use it for every Settings entry point (home header, puzzle top bar).

## Important constraints
- Release builds are **not minified** (`isMinifyEnabled = false`). Asset loading and Firebase would need keep rules before enabling R8. Release signing reads `RELEASE_KEYSTORE_PATH`/`_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` and is skipped when any is missing.
- Backup (`res/xml/*_rules.xml`) includes only `CrosswordSaves.xml`; Firebase auth state never leaves the device.
- **Portrait-only** relies on `PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY` on large screens under targetSdk 36. It stops working at targetSdk 37, so landscape/large-screen layouts are needed before that bump.
- **Portrait-only.** The runtime orientation lock carries `@Suppress("SourceLockedOrientationActivity")`.
- `@SuppressLint("StaticFieldLeak")` for `AmbientMusicPlayer` goes on the **object**, with the short-form import.
- When a fully-qualified name is used inline, remove the matching unused import (and vice versa) to keep lint clean.
- State written inside `onClick` lambdas produces "assigned value is never read" IDE warnings. They are a Compose false positive; don't suppress them individually.
- Local functions inside `CrosswordApp` capture `val`s from the composition that created them. For anything that must be live (for example locked cells while typing fast), read state or recompute inside the function (`lockedNow()`), rather than using a captured snapshot.
