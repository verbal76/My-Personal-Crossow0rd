# Release convention (Hot Attic Games studio standard)

**Public name:** `My Personal Crossword v<N>`. `N` is a plain sequential integer: v7, v8, v9...
No semantic versions, no codenames (`b9`, `modernized`, `candidate`, `final`) in anything a person has to read.

- **One source of truth:** the repo-root `VERSION` file holds `N`. Gradle turns it into `versionName = "vN"`, the About screen shows it as *Version*, the APK file is named `My-Personal-Crossword-vN.apk`, and CI uses it for the GitHub Release title and tag (`vN`).
- **Bump rule:** every playable build intentionally delivered for testing or release gets the next number. Bump `VERSION` in that commit. Failed CI runs and developer-only builds do not consume numbers. Never reuse a number for a different binary: CI refuses to touch a release that already exists.
- **Release:** pushing a tag `build-vN` (after bumping `VERSION` and pushing the commit) runs CI once and creates the release `vN` from the exact APK it built, marked **Latest**, with the SHA-256 and technical provenance in the notes. A manual run with `build_apk` + `publish_release` does the same. Ordinary pushes cost no Actions minutes. The title is only the name and version.
- **Engineering metadata stays available** but is not the version: Android `versionCode` (`1000 + CI run number`, must only go up), source commit, package, CI run, Kotlin/target SDK. About and Copy diagnostics show them next to the product version.
- **History:** no releases existed before v7. Counting every build actually handed over for testing gives v1 = `0f78713` ... v6 = `80f93ab` (Build 1057); the v7 label starts with the first build carrying this convention. Those older builds were never published, so there are no old releases or tags to rename.
- **Next version after v7 is v8.**

## Build log (public version: what changed)
- v7: first build with this convention. v8: Hot Attic Games studio splash. v9: game splash art, new launcher icon. v10: 25 categorized backgrounds, category picker, tile depth.

## Provenance and source rules
- **Blocking release-source gate** (`tools/verify_release_source.py`): the checked-out commit equals the recorded SHA; a `build-v<N>` tag peels to exactly that commit and matches `VERSION`; the tagged commit (not the branch tip) is reachable from `origin/<AUTHORITATIVE_BRANCH>`; the manifest agrees with the source and the artifact. Reviewed historical branches do not affect it.
- **Manifest:** each build writes `release-manifest.json` (commit, ref, version, platform, toolchain, timestamp, artifact SHA-256), attached to the Release and the Actions artifact. A coordinated multi-platform release lists one commit for all platform artifacts.
- **Tests:** CI verifies every test class declared in `app/src/test` ran with its declared count, none skipped or failed (`tools/verify_test_results.py`); currently 21 classes / 160 tests, of which 17 classes / 148 tests are the pure-Kotlin core suite that `tools/validate_local.sh` runs locally.
- **Branch inventory** (`tools/branch_audit.py`, `docs/BRANCH_LEDGER.md`) is informational. `main` is behind the product; syncing it is an owner decision after device testing and release qualification.

## Desktop artifacts and the studio naming standard
The studio standard names files `Game Title Build NN APK.apk` / `... EXE.zip` / `... Linux.tar.gz`. Desktop packages already use it
(`My Personal Crossword Build <N> Linux.tar.gz`, `... EXE.zip`, plus `build-manifest-<platform>.json` and `checksums-<platform>.sha256`).
Android and its existing Releases keep the owner-directed `v<N>` identity (`My-Personal-Crossword-v<N>.apk`, release/tag `v<N>`); changing that
is an owner decision (see the overnight report). `desktop.yml` attaches desktop files to the existing `v<N>` release only when asked
(`destination=github_releases`); it never creates a release.
