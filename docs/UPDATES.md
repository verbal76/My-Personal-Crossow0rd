# Update strategy

- **Android:** no OTA, by owner decision (2026-10-03). Native Kotlin/Compose: every change needs a new APK.
  Updates install over the previous build (same package id and signing; `versionCode = 1000 + CI run number`
  only goes up). Rollback = reinstall a previous Release APK. Saves live in app data and survive updates.
- **Windows / Linux:** no custom updater. Steam delivers files through SteamPipe once the app exists
  (not set up; needs the owner's Steamworks account). Until then a new package replaces the old folder;
  saves are outside the install folder (`%APPDATA%` / `~/.local/share/HotAtticGames/MyPersonalCrossword`).
- **Content boundary:** the word list is compiled into every build; it seeds the Daily (word-list fingerprint),
  so it must never be updated separately from the code.
- **Offline:** neither platform makes network calls to check for updates, so nothing can block play.
- **Publishing separation:** a GitHub Release, an OTA, Steam and Google Play are separate authorizations.
  `build.yml` publishes only when asked (`destination=github_releases`) and only after every selected platform passed; no push, tag or merge publishes anything.
