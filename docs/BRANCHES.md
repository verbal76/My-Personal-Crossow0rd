# Branch authority

This document records how the authoritative line of development was chosen
for the "professional polish" pass (branch `claude/game-polish-review-r2mlq7`).

## Candidates

All three branches share a single merge base: `a1390e2` ("Add GitHub Actions
workflow for debug APK builds"), which is the tip of `main`. Neither side
branch had been merged or opened as a pull request.

| Branch | Commits over `main` | Last commit | CI build |
|---|---|---|---|
| `main` | — | 2026-04-26 `a1390e2` | green (run #1) |
| `claude/final-polish-pass-AgdnQ` | 11 | 2026-05-01 `adf650f` | green on every push, incl. head (run #12) |
| `claude/review-crossword-repro-5mjbG` | 10 | 2026-04-26 `0960350` | never built (workflow only ran on `main` at the time) |

## Why `claude/final-polish-pass-AgdnQ` is authoritative

1. **It is a strict superset of `main`.** It forks from `main`'s tip and only
   adds commits; nothing from `main` is lost.
2. **It is the only side branch proven to build.** Every one of its 11 pushes
   produced a green `assembleDebug` and an installable APK (it also added a
   committed debug keystore so successive APKs install over each other,
   which is why it is almost certainly the build on the player's phone).
3. **It contains the most player-facing work:** mode-first home screen with
   snap carousel, Material 3 theme/typography/palette wiring, edge-to-edge,
   system Back handling, three tiers of audit fixes (online cleanup, win-flow
   scoring for Vindictive/Team, sanitised names, real `deletePlayer`, atomic
   stat writes, lifecycle-aware music, UTC daily seed, generator coverage for
   long answers, dark-mode-safe colours, scrollable dialogs).
4. **It already contains 5 of the 10 fixes on the repro branch** (see below),
   usually in a more complete form.

## `claude/review-crossword-repro-5mjbG` — commit-by-commit disposition

| Commit | Change | Present on polish? | Disposition |
|---|---|---|---|
| `f7c9368` | Central `endOnlineGame()` cleanup | Yes — `cleanupOnlineSession()` wired into 8 exit paths | Skipped (overlap) |
| `906717e` | Local inputs win over remote on merge (`rawInputs + userInputs`) | **No** — polish still had `userInputs + rawInputs` | **Superseded** by `core.mergeRemoteInputs`: remote letters are accepted only if they are the correct solution letter, override a wrong local letter, and never erase anything. A blanket "local wins" would keep a player's wrong in-progress letter over the partner's solved word once letter-by-letter input exists. |
| `001dfb1` | Load CSV on `Dispatchers.IO` | **No** | **Ported** |
| `9621bcb` | Defer anonymous Firebase sign-in to Host/Join | **No** | **Ported** (plus failure reporting) |
| `f9d2584` | Pause/resume music on lifecycle | Yes — polish also releases on `ON_DESTROY` | Skipped (overlap) |
| `65a2fb1` | `deletePlayer` sweeps all per-player keys | Yes — equivalent sweep | Skipped (overlap) |
| `1fe7d32` | Credit combined-category words to their real categories | **No** | **Ported** |
| `89ddb05` | Vindictive stat records | Yes — polish also banks score and handles ties | Skipped (overlap) |
| `8a72099` | Untrack `google-services.json`, add template | Polish deliberately keeps it tracked so CI can build; documents risk in `SECURITY.md` | Skipped — see `docs/EXTERNAL_ACTIONS.md` |
| `0960350` | CLAUDE.md refresh | Superseded by the rewrite at the end of this pass | Skipped |

Ported fixes were re-implemented on top of the polish code (not cherry-picked,
since the surrounding code differs): `001dfb1` (CSV off the main thread, now with
validation), `9621bcb` (deferred sign-in, now with failure reporting) and
`1fe7d32` (combined words credited to their real categories, now via the
`category` carried on each placed word).
