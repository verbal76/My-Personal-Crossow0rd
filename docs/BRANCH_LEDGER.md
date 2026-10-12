# Branch inventory and recovery ledger (audited 2026-10-12)

Policy: one authoritative history; branches are temporary workspaces. The machine-readable
version of this ledger is `docs/branch_ledger.json`, enforced by `tools/branch_audit.py`
(`tools/validate_local.sh` runs it; tag builds run it as a warning). Nothing here authorizes
deleting a branch.

**Authoritative line:** `claude/native-stack-modernization` (see `AUTHORITATIVE_BRANCH`).
Audited at `efaa76d`.

## Inventory

| Ref | Tip | Commits not on the authoritative line | Role |
|---|---|---|---|
| `claude/native-stack-modernization` | `efaa76d` | 0 | **Authoritative.** Carries v7 to v10 (tags `v7`, `v8`, `v9`, `v10`, all ancestors of it). |
| `claude/game-polish-review-r2mlq7` | `7653f4a` | 0 | Rollback/playtest checkpoint (owner-protected). PR #1 (draft, base `main`). Strict ancestor. |
| `claude/infra-about-hold` | `b993bd7` | 0 | Infrastructure checkpoint, strict ancestor. |
| `claude/core-audit-fixes` (local, worktree) | `e31247d` | 0 | Agent worktree branch; fully merged into the line. |
| `worktree-agent-abda7fd4894ca9c7e` (local, worktree) | `b42576d` | 0 | Agent worktree branch; fully merged. |
| `worktree-agent-a84bcf8c82fabcbac` (local) | `a1390e2` | 0 | Points at the old `main` base. |
| `origin/claude/final-polish-pass-AgdnQ` | `adf650f` | 0 | The April line the polish pass started from; fully contained. |
| `origin/claude/review-crossword-repro-5mjbG` | `0960350` | **10** | Old parallel April line. Fully dispositioned below. |
| `origin/main` | `f7775d6` | **2** | Stale April baseline plus two owner asset uploads (integrated, below). Default branch. |

No stashes, no dangling commits, no revert commits anywhere in the history. Release
tags `v7` to `v10` were built from commits on the authoritative line.

## Ledger of work outside the authoritative line

### `origin/claude/review-crossword-repro-5mjbG` (10 commits, 2026-04-26)
Already analysed commit by commit in `docs/BRANCHES.md`; re-verified against today's source.

| Commit | Change | Classification | Evidence today |
|---|---|---|---|
| `f7c9368` | Central Firebase cleanup | Integrated (equivalent) | `closeGame`/`abandonGame`, listener removed in `finally` |
| `906717e` | Local inputs win over remote | Superseded | `core.mergeRemoteInputs` (correct letters only, never erases) |
| `001dfb1` | CSV off the main thread | Integrated (ported) | startup load on `Dispatchers.IO` |
| `9621bcb` | Defer anonymous sign-in | Integrated (ported) | `signInAnonymously` only from Host/Join |
| `f9d2584` | Pause music on background | Integrated (equivalent) | `ON_PAUSE`/`ON_RESUME` handling |
| `65a2fb1` | `deletePlayer` really deletes | Integrated (equivalent, safer) | `core/PlayerKeys` exact ownership |
| `1fe7d32` | Credit combined words to real categories | Integrated (ported) | `PlacedWord.category` |
| `89ddb05` | Vindictive stat records | Integrated (equivalent) | `saveStat` handles modes and ties |
| `8a72099` | Untrack `google-services.json` | Intentionally not adopted | kept tracked so CI can build (`SECURITY.md`, `docs/EXTERNAL_ACTIONS.md`) |
| `0960350` | CLAUDE.md refresh | Superseded | CLAUDE.md rewritten since |

Disposition: nothing to recover. Keep the branch as history.

### `origin/main` (2 commits, 2026-10-04, "Add files via upload")
Owner uploads of `Hot_Attic_Games_Master_Logo_ALPHA_FINAL.png`,
`my_personal_crossword_app_icon.png`, `my_personal_crossword_splash_1080x2280.png`.
Integrated: copied byte-for-byte into `branding/` (git blob hashes match) and shipped in v8
to v9. The root-level copies on `main` stay as they are.

### Files that exist on older lines but not on the authoritative line
- `app/src/main/res/drawable/ic_launcher_background.xml`, `ic_launcher_foreground.xml`:
  intentionally removed with the new launcher icon (v9).
- `app/src/test/.../ExampleUnitTest.kt`: Android Studio template placeholder, intentionally removed.

## Open items needing the owner
1. `main` (the default branch) is 52 commits behind the product. Synchronizing it (fast-forward to
   the authoritative line, or merging PR #1) is an owner-controlled decision to be made after
   device testing and release qualification. It is not done automatically.
2. Old agent worktrees under `.claude/worktrees/` can be removed whenever the owner likes;
   nothing in them is unmerged.
