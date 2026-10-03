# Native stack (modernised 2026-10-03)

| Component | Before (7653f4a / b993bd7) | Now |
|---|---|---|
| Gradle | 9.3.1 | 9.6.1 (AGP 9.4 needs at least 9.6.0). `distributionSha256Sum` is not pinned yet: the checksum host is unreachable from cloud sessions. Pin it from a machine that can fetch `gradle-9.6.1-bin.zip.sha256`. |
| Android Gradle Plugin | 9.1.1 | 9.4.0 |
| Kotlin (+ Compose plugin) | 2.2.10 | 2.3.21 |
| JDK (CI) | 17 | 17 (AGP 9.4 minimum is 17; proven by CI) |
| Compose BOM | 2026.02.01 | 2026.06.01 |
| lifecycle | 2.9.4 | 2.10.0 |
| activity-compose | 1.11.0 | 1.13.0 |
| core-ktx | 1.17.0 | 1.18.0 |
| Firebase BoM / google-services | 34.12.0 / 4.4.4 | unchanged (newest versions could not be checked from the cloud) |
| minSdk / targetSdk / compileSdk | 24 / 36 / 36.1 | unchanged |

Why not newer: Compose 1.12.1 (BOM 2026.09.00), lifecycle 2.11.0 and core 1.19.1 declare `minCompileSdk 37`. Raising compileSdk is a separate decision (targetSdk can stay 36). Tartaria's versions were not copied: its Expo/React Native stack differs, and Crossword was already newer on Gradle, AGP and Kotlin.

CI inspects the built APK on every run: package and versionCode, merged permissions, native libraries, ELF `LOAD` alignment (at least 0x4000) and `zipalign -c -P 16`.
