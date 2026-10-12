# Third-party components (status as of this commit)

Status words: **verified** = license text or publisher statement seen; **identified** = license known from the
component's public terms but not independently checked here; **missing evidence** = no provenance in the repo.

| Component | Used in | License | Status |
|---|---|---|---|
| Kotlin stdlib, kotlinx | all | Apache-2.0 | identified |
| AndroidX / Jetpack Compose | Android | Apache-2.0 | identified |
| Firebase Android SDK (Auth, Realtime Database) | Android | Apache-2.0 + Firebase terms | identified |
| OpenJDK runtime bundled by jpackage | Windows / Linux builds | GPLv2 with Classpath Exception (legal notices ship in `lib/runtime/legal`) | identified |
| JUnit 4 | tests only | EPL-1.0 | identified |
| Controller/keyboard glyphs ("Input Prompts" by JulioCacko, 20 images in `desktop/src/main/resources/glyphs`) | desktop | CC0 1.0 per the publisher's page (owner-supplied screenshot) | identified, see `glyphs/NOTICE.txt` |
| Ambient music, `app/src/main/assets/Music/*.mp3` (14 tracks, Android only) | Android | unknown | **missing evidence** |
| Word list and clues, `app/src/main/assets/test.csv` | all | unknown authorship/provenance | **missing evidence** |
| 25 backgrounds (`tools/make_backgrounds.py`, procedural) | all | original, generated in this repo | verified (own work) |
| Older background art, `assets/images/*` | Android | unknown | **missing evidence** |
| Hot Attic Games logo, game splash, icon (`branding/`) | all | owner-supplied | owner's own |

The desktop build does not ship the music or the older background art. Before any commercial/Steam release the
owner must supply provenance and licenses for every "missing evidence" row.
