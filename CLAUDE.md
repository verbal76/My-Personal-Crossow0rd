# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Lint check
./gradlew lint

# Clean build
./gradlew clean
```

**Build configuration:** Gradle 9.3.1, AGP 9.1.0, Kotlin 2.2.10, targetSdk 36, minSdk 24, Java 11, Compose BOM 2026.02.01.

## Architecture

**Single-file, single-activity Jetpack Compose app.** Nearly all logic lives in `app/src/main/java/com/example/mypersonalcrossword/MainActivity.kt` (~4,700 lines). There is no ViewModel, no navigation library, and no Room database — state lives entirely in Compose state containers.

### App Navigation (AppMode enum)
Four screens managed by a top-level `when(appMode)` branch in `MainActivity`:
- `LOGIN` — player profile selection/creation
- `STATS` — per-player statistics
- `CATEGORY_SELECT` — category and game mode picker
- `DASHBOARD` — active puzzle gameplay

### Game Modes (GameMode enum)
- `SINGLE` — solo puzzle
- `TEAM` — two players, collaborative; category picker shown inside the setup popup (same pattern as VINDICTIVE)
- `VINDICTIVE` — competitive; each player picks words and assigns clues to the opponent
- `DAILY` — time-seeded daily puzzle across all categories; launched via a yellow "Daily Puzzle" category button

### Difficulty (Difficulty enum)
`EASY` (8 words) → `MEDIUM` (12) → `HARD` (15) → `EXPERT` (25) → `GENIUS` (50). Each level carries a point multiplier.

### Key Data Classes
```kotlin
data class RawEntry(val answer: String, val clue: String, val category: String)
data class PlacedWord(val word: String, val clue: String, val startX: Int, val startY: Int,
                      val isHorizontal: Boolean, val number: Int = 0)
data class GridCell(val x: Int, val y: Int, val char: Char, val number: Int? = null)
data class PuzzleSave(val words: List<PlacedWord>, val inputs: Map<Pair<Int,Int>, Char>,
                      val bgArgb: Int, val bgImageName: String)
data class StatRecord(val category: String, val diffName: String, val gameMode: String,
                      val hintsUsed: Int, val timeSeconds: Long, val score: Int,
                      val partner: String, val partnerScore: Int, val won: Boolean)
```

### Persistence (SaveManager)
`SaveManager` wraps **SharedPreferences** for all persistence — no SQLite or file I/O. It stores player profiles, scores, used-word sets (to prevent repeats), in-progress puzzle saves, stat records, and per-player settings (cell color, button color, music volume).

Serialization uses custom delimiters: `§` between fields within a record, `|` between records for words, `;` between entries for inputs.

**Key SaveManager behaviors:**
- `saveStat()` keeps the **best record only** — overwrites only if `score` improves, then `hintsUsed` (lower better), then `timeSeconds` (lower better). Does not unconditionally overwrite.
- `clearPuzzle()` is **atomic** — removes all five keys (`_words`, `_inputs`, `_bg`, `_bgimg`, `_time`) plus the save-set entry in a single `prefs.edit {}` block. Always include `_bgimg` removal or the background image key leaks.
- First-use tutorial flags: `isFirstSingle()` / `markSingleSeen()`, `isFirstTeam()` / `markTeamSeen()`, `isFirstVindictive()` / `markVindictiveSeen()` — Boolean flags in SharedPreferences.

### Tutorial / First-Use Flow
Every game mode shows a tutorial dialog the first time it is selected. The dialog is triggered inside `onGameModeChange`:
- **SINGLE** → `showSingleTutorial` (no second step; goes straight to category select after dismiss)
- **TEAM** → `showTeamTutorial` (dismiss triggers `showPlayer2SetupDialog` with category picker)
- **VINDICTIVE** → `showVindictiveTutorial` (dismiss triggers `showPlayer2SetupDialog` with category picker)

After the first use, `showPlayer2SetupDialog` is shown directly without a tutorial.

### Daily Puzzle Flow
- On first navigation to `CATEGORY_SELECT` each login session, `showDailyPrompt = true` fires via `LaunchedEffect(appMode)` gated by `dailyPromptShown`.
- The prompt asks "Play today's Daily Puzzle?" — **Yes** switches to `showDailyInstructions`, then launches; **No** dismisses and notes that the yellow button is available.
- `dailyPromptShown` resets to `false` in the `onChangeUser` block so the prompt fires again on the next login.
- In `CategoryScreen`, the Daily Puzzle appears as a styled yellow-gold gradient `Box` button at the top, visually matching the regular category buttons (64dp height, `RoundedCornerShape(12.dp)`, bevel highlight).

### % Explored Label
Category buttons display `"• X% explored"` only when `used > 0` (words completed in past puzzles for that category). The label is hidden when `used == 0` to avoid a confusing "0% explored" on fresh installs. `used` comes from `saveManager.getUsedWords(playerName, category).size` and `total` from the raw entry count for that category.

### Puzzle Generation
`generateCrossword()` runs a **BFS-based placement algorithm** with 50 iterations, returning the highest-scoring layout:
1. `buildWordIndex()` — 3D index: `length → position → char → List<RawEntry>` for O(1) constraint lookup.
2. `isValidFast()` — validates each placement against three rules: empty end-caps, valid perpendicular intersections, no parallel adjacency.
3. BFS from crossing points — tries perpendicular words at every cell of each placed word.
4. Scoring favors inner placements, bridge intersections (word crosses multiple existing words), and longer words.
5. **Crash guard:** `if (topWords.isEmpty()) return@repeat` before `topWords[Random.nextInt(topWords.size)]` — prevents index-out-of-bounds when the word pool is exhausted.

### Audio
- **`SoundPlayer` (object)** — synthesizes sound effects (correct/wrong/clap/celebration) via `AudioTrack` using raw PCM math.
- **`AmbientMusicPlayer` (object)** — streams MP3s from `assets/Music/` via `MediaPlayer`, shuffles and auto-advances the playlist, pauses on app background.
  - `currentTrackName` is declared as `var currentTrackName by mutableStateOf("")` — **must stay Compose state** so the skip-button track name display recomposes when the track changes. A plain `var` will not trigger recomposition from a singleton object.

### Key Composable Functions
Major screen-level composables (all in `MainActivity.kt`):
- `CrosswordApp()` — root composable; owns all top-level state and the `when(appMode)` navigation branch
- `CategoryScreen()` — renders the category/difficulty picker (`CATEGORY_SELECT` mode)
- `PuzzleScreenReference()` — active puzzle grid and clue panel (`DASHBOARD` mode)
- `StatsScreen()` — per-player stat records (`STATS` mode)
- `BgPickerScreen()` — background image picker (launched as an overlay from the puzzle screen)
- `ColorPickerSheet()` — cell/button color customization sheet
- `ConfettiOverlay()` — celebratory animation drawn on top of the puzzle on completion

### State Management Patterns
- `rememberSaveable` + custom `listSaver` — used for enums and collections that must survive config changes.
- `derivedStateOf` — used for computed values like `isWinner`.
- `LaunchedEffect` — drives the game timer, cell animations, music lifecycle, and the daily-prompt trigger.
- State written inside `onClick` lambdas will trigger false-positive **"assigned value is never read"** lint warnings in Android Studio. This is a Compose-specific lint blind spot — the lint engine does not trace reads through the recomposition model. Do not suppress these individually with `@Suppress`; they are harmless noise.

## Assets
- **Word list:** `app/src/main/assets/test.csv` — three columns: `Answer,Clue,Category`. This is the sole data source for puzzles.
- **Backgrounds:** `app/src/main/assets/images/` — WebP/PNG/JPG at `drawable-xxhdpi` and `drawable-xxxhdpi` densities (portrait and landscape variants).
- **Music:** `app/src/main/assets/Music/` — MP3 files discovered at runtime.

## Important Constraints
- **Release builds are not minified** — `isMinifyEnabled = false`; ProGuard/R8 is configured but disabled. Do not enable it without testing, as asset loading and reflection paths may break.
- **Portrait-only** — orientation locked in manifest; the `LaunchedEffect(Unit)` that sets orientation carries `@Suppress("SourceLockedOrientationActivity")`.
- **No networking** — fully offline; no HTTP clients or cloud sync.
- **Single activity** — no Fragments; all screens are Composables switched by `appMode`.
- `test.csv` uses a plain CSV format without quoting; entries must not contain commas or the `§`/`|`/`;` delimiter characters.
- **`@SuppressLint("StaticFieldLeak")`** for `AmbientMusicPlayer`'s `context` field must be placed on the **object declaration**, not the field itself, and requires `import android.annotation.SuppressLint` (short form). Using the fully-qualified `@android.annotation.SuppressLint` triggers a separate "redundant qualifier" warning.
- When using a fully-qualified class name inline (e.g., `android.graphics.BitmapFactory.decodeStream(...)`), the corresponding `import` is flagged as unused. Remove the package prefix from the call site so the import is visibly used.
