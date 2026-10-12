# Release convention (Hot Attic Games studio standard)

## Names
- **Studio build number `<N>`:** a plain sequential integer, the root `VERSION` file. Bump it in the commit that prepares a build delivered to the owner. A `local_only` validation run does not publish and does not consume a published number; a number is never reused for different published content.
- **Release title:** `My Personal Crossword Build <N>`. **Tag:** `build-<N>`. **Files:**
  `My Personal Crossword Build <N> APK.apk`, `… EXE.zip` (Windows), `… Linux.tar.gz`, `build-manifest.json`, `checksums.sha256`.
- **History kept as it is:** releases/tags `v7`–`v10` (files `My-Personal-Crossword-v<N>.apk`) are never renamed, moved or deleted. v1–v6 were never published. The next number is **11**.
- **Android identifiers are separate on purpose.** `versionName` = `v<N>` (from VERSION, shown in About and Build details). `versionCode` = `1000 + GitHub run number`: an installer-only counter that must only ever go up, so a new APK always installs over the last. It is not the build number and never needs to match it (v10 shipped with versionCode 1066; Build 11 will get whatever run number it gets, which is higher). Package id and signing identity do not change.

## How a build is made (one manual workflow, `build.yml`)
Inputs: `platform` = android | windows | linux | all, `source_sha` (the exact approved 40-character commit), `build_number` (must equal VERSION at that commit), `destination` = `local_only` (default) | `github_releases`.
Dispatch with `ref` = the branch holding the SHA (API or `gh`-style dispatch; the Actions form still shows `main`'s stale copy until `main` is synchronized).

1. `plan` validates the request and the source: SHA format, clean checkout of exactly that SHA, VERSION == build_number, lineage (below), and for `github_releases` that no tag or release named `build-<N>`, `build-v<N>` or `v<N>` exists. It also runs the release-policy tests.
2. Tests: shared core + desktop tests (`desktop-tests`), Android unit tests inside the `android` job; every declared suite must have run (`verify_test_results.py`).
3. Build/package each selected platform from that one SHA; each writes a per-platform manifest (commit, version, build number, toolchain, timestamp, SHA-256, size, lineage) that is checked against the actual file.
4. `publish` runs only for `github_releases` and only if **every** selected platform succeeded (no partial release). It merges the manifests (`tools/merge_build_manifest.py` refuses mixed commits/numbers, missing or altered files), writes `checksums.sha256`, re-verifies, and creates the release and tag at the SHA. It never replaces an existing release or tag and never uses `--clobber`.
5. `local_only` stops after step 3: artifacts stay in the run for 14 days. Nothing is published. Steam, Google Play and OTA are never touched by this workflow.

Automatic triggers: none for builds. Pull requests run only `pr-check.yml` (tests and policy checks, no packaging, no secrets). Signing secrets exist only in the `android` job's signed-release steps; the signed release APK is a run artifact and is never attached to a release.

## Source rules (blocking gate, `tools/verify_release_source.py`)
The checked-out commit equals the recorded SHA; any tag peels to exactly it and matches VERSION; and the commit is reachable from `origin/<AUTHORITATIVE_BRANCH>`. Exception for `local_only` only: a commit on a ledgered `active_feature` branch that still contains the authoritative tip (so an unmerged branch can be validated without being releasable). Reviewed historical branches never matter. `tools/test_release_source.py` holds the passing and deliberately failing cases, including a check of the real workflow files.
To publish desktop work the owner must first accept it onto the authoritative line (fast-forward `AUTHORITATIVE_BRANCH`); `main` is separate and not touched.

## Build log (what changed)
v7: first sequential name. v8: studio splash. v9: game splash, new icon. v10: 25 categorized backgrounds, category picker, tile depth. Build 11 (proposed): first coordinated build with the desktop packages.
