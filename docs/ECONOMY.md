# Economy audit

All score-moving constants now live in `core/Economy.kt` and are pinned by
`EconomyTest`. This document records what the economy **was** on the
authoritative branch, what was **defective** (implementation contradicting
the in-game rules text), what this pass **changed** and why, and what was
deliberately **not** changed.

## 1. What exists

| Thing | Where it lives | What it's used for |
|---|---|---|
| Lifetime score (`score_<name>`) | SharedPreferences, per profile | Displayed on home header, top bar, profile list, stats. **Gates hints** (a hint needs a positive balance). Nothing else reads it — no unlocks, shop, levels or leaderboards. |
| Puzzles completed (`completed_<name>`) | per profile | Display only. |
| Best stat record | per profile + category + difficulty (+ mode, see below) | Stats screen. Keeps the best score → fewest hints → fastest time. |
| Answer streak | in memory, reset on every new puzzle | Milestone banners (3/5/10/15/every 5 after) and a clap sound. No score effect. |
| Vindictive match score | in-match | Decides the winner; banked into lifetime score at the end (floored at 0). |

### Sources

| Event | Points |
|---|---|
| Solve Easy / Medium / Hard / Expert / Genius | 1 / 2 / 4 / 8 / 20 |
| Solve the Daily Puzzle | 20 |
| Team solve | full difficulty reward to **both** players |
| Vindictive: own clue right / wrong | +1 / −1 (match score) |
| Vindictive: assigned clue right / wrong / pass | +1 / −2 / −1 (match score) |
| Vindictive end | each player's match score banked, floored at 0 |

### Sinks

| Event | Points |
|---|---|
| Reveal one letter (hint) | −1 lifetime, charged immediately |

## 2. Defects found

These are places where the code contradicted the rules the game itself shows
the player (tutorials, dialogs, results card).

| # | Defect | Rules text it contradicted | Effect on players |
|---|---|---|---|
| E1 | **Hints charged twice**: −1 when revealed, then subtracted again from the completion reward. | "Reveals one letter… Costs 1 point." Results card: "+N points earned … (−H pts)". | Every hint actually cost 2. The card said the puzzle netted `reward − H`; the profile really moved by `reward − 2H` (or `−H` when the reward was clamped at 0). |
| E2 | **Daily reward farmable**: replaying the Daily paid 20 every time. | "A brand-new puzzle every day". | Unlimited 20-point payouts — the largest single source in the game. |
| E3 | **Online Team double credit**: both devices credited both players, so each player's points were also written onto a shadow local profile of the *other* player on each phone. | Team: "both players share the reward". | Remote friend appeared as a local profile with points they never earned on that device. |
| E4 | **Vindictive timeout took −2 automatically**; the Answer/Pass choice never appeared. | Tutorial: "⏭ Pass: −1 point"; dialog: "Answer for +1 pt, or pass for −1 pt". | Every timeout cost 2 instead of offering the documented −1 pass. |
| E5 | **Stat records ignored game mode**: a Team or Vindictive result could overwrite the solo best for the same category and difficulty. | Stats cards are phrased per mode. | Solo personal bests silently replaced. |

## 3. Changes made (and their consequences)

| # | Change | Consequence |
|---|---|---|
| E1 | Hints are charged **once**, at the moment of use. The completion award is the full difficulty reward, so a puzzle's lifetime delta is exactly `reward − hints`, the number on the results card. The stat record still stores the net (`reward − hints`, floored at 0) so old and new records compare fairly. | Hint users now keep `H` more points per puzzle than before. The only consumer of the balance is the hint gate, so the practical effect is that players can afford slightly more hints. Existing balances are not recalculated. |
| E2 | The Daily pays out **once per UTC day** per profile, tracked by date key. The Daily card shows "Solved today" and a countdown instead of relaunching. | Removes the farm. Points already farmed are kept. |
| E3 | Online games credit **only the local player** on each device. | Each player earns exactly once, on their own phone. No shadow profiles. |
| E4 | Timeout now opens the documented **Answer It / Pass (−1)** choice on the answering player's device. | Timeouts cost −1 (pass) or ±1/−2 (answer), as the tutorial describes. |
| E5 | Team and Vindictive stats are stored under **mode-namespaced keys**. Solo keys are unchanged, so existing solo records remain. | Solo bests can no longer be overwritten by other modes. Legacy mixed records stay readable. |

No reward values, costs, or multipliers were changed. `EconomyTest` pins
1 / 2 / 4 / 8 / 20, Daily 20, hint 1, and Vindictive +1 / −1 / +1 / −2 / −1.

## 4. Considered but **not** changed

These are proposals only. Each would alter progression for existing players and
needs a design decision first.

| Proposal | What it would affect |
|---|---|
| Time bonus / par time per difficulty | Would make `elapsedSeconds` matter to score. Rewards fast solvers and punishes players who leave a puzzle open. Needs par values per difficulty and word count. |
| Stars (e.g. 3★ = no hints, under par) | Pure presentation if stars don't pay out. If they do, it's a new source. Would need persistence per puzzle, which today only stores the best record. |
| Streak score multiplier | Streaks are currently only celebratory. A multiplier would favor careful players and make the Vindictive −1/−2 penalties relatively harsher. |
| Scaling hint cost with difficulty | Genius hints would get cheaper relative to the 20-point reward, and Easy hints (1 point, the whole reward) relatively expensive. |
| Level / unlock progression (backgrounds, colours) | Would give the score balance a purpose beyond hints. Colour and background pickers are currently free; locking them would take options away from existing players. |
| Per-player hint billing in local Team | Today the logged-in player pays for every hint, even Player 2's. Per-player hint counts are now tracked for stats, but billing is unchanged. |
