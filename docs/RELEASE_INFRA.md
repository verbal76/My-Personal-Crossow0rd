# Release infrastructure (Hot Attic Games directive, 2026-10-03)

Facts below were read from the repository on branch `claude/infra-about-hold` (based on `7653f4a`).

## A. Project / Android / Play identity
| Item | Value |
|---|---|
| App name | My Personal Crossword |
| Package ID | `com.hag.mypersonalcrossword` (unchanged since 2026-04-25; do not rename) |
| versionName / versionCode | `1.0.<code>` / `1000 + GitHub run number` in CI, commit count locally |
| minSdk / targetSdk / compileSdk | 24 / 36 / 36.1 |
| Play requirement | New apps and updates must target API 36 from 2026-08-31 (developer.android.com, read 2026-10-03). Stored as `BuildConfig.PLAY_REQUIRED_TARGET_API`; re-verify when Google changes it. |
| Play API compliant | YES (targetSdk 36). No migration needed (remediation class: none). |
| APK | Debug APK built by CI on every push. Signed release APK only when `RELEASE_KEYSTORE_BASE64` exists. |
| AAB | `.github/workflows/release-aab.yml`, manual only, fails hard without release secrets. PREPARED, never run (no key). |
| Signing | Debug builds use the committed `debug.keystore`. No release/upload key exists in this repo. |
| Permissions | `VIBRATE` plus Firebase-merged `INTERNET`/network state. |
| Data | Firebase Auth (anonymous) and Realtime Database for online games only; no analytics, ads, or crash reporting. Saves stay on device. |
| Privacy policy / Data Safety / content rating | Not written. Owner action. |
| Owner actions | Create upload key and add secrets; Play Console app, listing, privacy policy, Data Safety, content rating; deploy `firebase/database.rules.json`. |

## B. OTA architecture
There is NO OTA, deliberately. This is a native Kotlin/Compose app: code, layouts and assets ship inside the APK, so nothing can be swapped without a new APK/AAB. The one data-like asset, `assets/test.csv`, feeds the Daily puzzle seed (word-list fingerprint), so shipping it over the air would give devices different Dailies. Everything is therefore NATIVE-BUILD-REQUIRED. Tester delivery is by CI APK now; Firebase App Distribution or Play internal testing is the recommended update path (owner credentials needed). Because no OTA ever activates, the "Please wait, applying update" modal is not applicable.

## C. Settings → About
Settings gear → "Build details" (`BuildInfoDialog`, `BuildInfo` in `MainActivity.kt`). Fields: application, package, version, versionCode, source commit, build type, signing state, build time and origin, install/update times, Android/API, device, locale, target SDK, Play required target and compliance, update model. "Copy diagnostics" copies a labelled plain-text block with a capture time. It contains no saves, names or secrets.

## D. Hot Attic Games studio splash
NOT IMPLEMENTED: the canonical asset `branding/Hot_Attic_Games_Master_Logo.png` does not exist in this repository (no `branding/` directory in any branch). Supply it and the card can be added after the opening card without touching the existing startup sequence. Native build required.
