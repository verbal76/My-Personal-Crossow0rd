# Desktop feature parity with Android v10

Assessed from the Android source (`MainActivity.kt` and `core/`), not from feature names. "Shared core" = rules in
`core/` used by both. Status uses the desktop build as of this commit. Parity is claimed only where it is demonstrated
by tests and the real-window smoke test (`desktop/scripts/smoke.sh`); real Deck hardware is untested.

| Feature | Android behaviour | Shared core | Desktop now | External services | Effort left | Effect on identity | Release without it |
|---|---|---|---|---|---|---|---|
| **Team (local)** | Two players alternate; filling a word is one attempt (right or wrong), turn always passes; wrong attempt clears the word; "Pass the Phone!" message between turns; hints are charged to the player whose turn it is; both players earn the full reward; results show solved counts and MVP | `TeamRules`, `TeamState`, `Economy`, `SessionCodec` (team fields) | **Done** (Pass the Deck, fixed names Player 1/2, Player 2 cannot buy hints: no profile score, as on Android for a profile-less name) | none | none | high: a core mode | n/a |
| **Vindictive (local)** | Pick-own, assign-clue (Give), opponent answers a forced clue against a 15/30/60 s clock (+1/-2, timeout lets them answer or pass for -1), taunts on wrong answers, last letter cannot be hinted, match score banked floored at 0, clock restored on resume | `VindictiveRules`, `VindState`, `Economy`, `vindictiveTaunt`, `TAUNT_COUNT`, codec fields | **Done** (taunts kept verbatim) | none | none | very high: the mode's personality | n/a |
| **Online Team/Vindictive** | Firebase Realtime Database: anonymous sign-in, create/join by code in transactions, one device publishes the puzzle, letters merged with `mergeRemoteInputs`, presence with server `onDisconnect`, turn gating, shared Vindictive timer | `mergeRemoteInputs`, rules above | **Not ported** | Firebase Auth + Realtime Database (Google, free tier today; the project config is in the repo) | Large: the Android SDK is not usable on desktop; REST + streaming can do auth, reads, writes and conditional writes but has **no server-side `onDisconnect`**, so the presence/abandon logic would need a different design; needs its own test rig and a second machine | medium: the only network feature | Not materially incomplete for a first playtest; incomplete versus Android. See recommendation |
| **Sound effects** | `SoundSynth` effects (click, key, correct chime rising per streak, wrong, clap, celebration, tick) with a Sound switch | `SoundSynth` (whole synthesis) | **Done** (same samples through javax.sound; switch in the menu; silent if no audio device) | none | none | medium | n/a |
| **Music** | 14 ambient MP3s streamed with audio focus, fades, volume | none (assets) | **Not ported** | none | Medium once licensing is settled: the JDK cannot decode MP3, so it needs either a small pure-Java decoder dependency or re-encoding to a JDK-readable format (WAV is too large; OGG also needs a library) | medium (atmosphere) | Playable, but different feel; **blocked** anyway: no licence evidence for the tracks (`docs/THIRD_PARTY_ASSETS.md`) |
| Streak chime/milestone ("On Fire!") | yes | `checkStreakMilestone` | Done (message + clap sound) | none | none | low | n/a |
| Hints | cost 1, need the points, never below zero, reveal selected/first open square | `pickHintCell`, `Economy` | Done (fixed this round: desktop used to allow hints at 0 points) | none | none | high | n/a |
| Daily | UTC-day deterministic board, pays 20 once | `DailyPuzzle`, golden tests | Done, identical board (tested against the core generator) | none | none | high | n/a |
| Profiles / login, per-profile colours | named profiles, colours, Stats entry | `PlayerKeys`, `sanitizeName` | **Not ported** (one implicit profile) | none | Medium; needs text entry (Steam keyboard API) | low-medium | Acceptable for desktop v1 |
| Stats screen, personal bests, used-word history, completion count | yes | `StatRecord`, `Ledger` | **Not ported** (score, Daily set and payout ledger are kept) | none | Medium | medium | Acceptable for v1; players lose bests |
| 25-picture background picker with categories | picker screen | `BackgroundCatalog` | Partly: Background row cycles all 25 | none | Small | low | Acceptable |
| Tutorials ("how to play"), About/Build details, haptics | yes | n/a | Not ported (haptics n/a) | none | Small-medium | low | Add a short help screen before a public release |

## Findings from the inspection
- The turn orchestration of Team and Vindictive (what happens after an attempt, dialogs, forced word, clocks, banking at completion) is written inside Android's screen code, not in `core/`. Desktop now implements the same sequence on top of the shared rules, so there are two copies of the sequencing. **Recommendation (RED, needs a native build and device test):** extract it into a `core` turn engine with tests, then make both front ends use it. Until then, a change to Team/Vindictive flow on Android must be mirrored in `desktop/.../GameController.kt` and `ModesTest.kt`.
- Desktop had one real parity bug: hints were allowed without points and could drive the score negative. Fixed and tested.
- Android pauses the puzzle clock in the background; desktop now does too (window focus).

## Recommendations
1. **Online play:** do not build it for the first desktop/Steam playtest. If wanted later, choose between (a) Firebase REST with a redesigned presence (heartbeat field + timeout, no `onDisconnect`) or (b) Steam lobbies/networking after a Steamworks app exists (no paid service, but needs Steamworks credentials). Owner decision; (b) fits a Steam release better.
2. **Music:** settle licensing first. Then ship a small set as OGG or similar with an approved decoder library.
3. **Stats/profiles:** port only if players ask; they need name entry and a Stats screen.
