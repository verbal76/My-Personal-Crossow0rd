# Actions outside the repository

Everything below needs the Firebase Console, the Google Cloud Console or the
GitHub repository settings. None of it can be done from source, and none of it
is done yet. Items are in priority order.

The Firebase project is **`mypersonalcrossword`** (Realtime Database at
`https://mypersonalcrossword-default-rtdb.firebaseio.com/`).

---

## 1. Deploy the Realtime Database security rules — **required**

**Why:** the database's current rules are unknown from the repo and are
probably the permissive defaults. With them, anyone who signs in
anonymously can read and write *every* game: inject puzzles, change scores,
steal the guest seat, or fill the database. The app now stores `hostUid`,
`guestUid` and `createdAt` on each game so rules can lock this down.

**What the shipped rules (`firebase/database.rules.json`) do:**
- Nothing outside `games/` is readable or writable.
- A game can be created only by a signed-in user who names themselves as host.
- After that, only the host and the seated guest can write to it.
- The guest seat can be claimed exactly once, by one user, while the game is
  `waiting` and less than 15 minutes old. Old lobby codes expire.
- A waiting game can be read by anyone signed in (needed to join by code).
  Once it starts, only its two players can read it.
- Every field is type- and range-checked. Unknown fields are rejected.
- `hostUid` and `createdAt` can't be changed after creation.

**How:**
1. Firebase Console → *Build → Realtime Database → Rules*.
2. Replace the rules with the contents of `firebase/database.rules.json`.
3. *Publish*.
   - With the Firebase CLI instead: `firebase deploy --only database`, with
     `"database": {"rules": "firebase/database.rules.json"}` in a `firebase.json`.

**Verify:**
- In *Rules → Rules Playground*, simulate an authenticated `set` on
  `/games/ABCDEF` with a body whose `hostUid` is a different uid. It must be
  **denied**.
- Play one online Team game and one online Vindictive game between two phones.
  Both must work end to end: host, join, play, finish, then "Done".
- On a third phone, try to join the same code after the guest has joined. It
  must say the game isn't available.

## 2. Make sure Anonymous sign-in is enabled — **required for online play**

**Why:** Host and Join sign in anonymously first. If the provider is off,
the app now shows "Couldn't reach the game server…" instead of failing
silently.

**How:** Firebase Console → *Build → Authentication → Sign-in method* →
**Anonymous** → *Enable*.

**Verify:** tap *Host Game* on a fresh install. A 6-character code appears and
no error dialog is shown.

## 3. Restrict the Android API key — **recommended**

**Why:** the key in `app/google-services.json` is *not a secret*. Google
designs Firebase Android keys to ship inside the APK, and anyone can pull it
from the app. `SECURITY.md` previously called it "compromised". The real
protection is the rules in step 1. Restricting the key also stops it being
reused against other Google APIs.

**How:**
1. Google Cloud Console → *APIs & Services → Credentials* → the Android key
   for this project.
2. *Application restrictions* → **Android apps** → add package
   `com.hag.mypersonalcrossword` with the SHA-1 of every keystore you ship
   with:
   - The committed `debug.keystore`: run
     `keytool -list -v -keystore debug.keystore -storepass android` to get it.
   - Your release key, when you have one.
3. *API restrictions* → restrict to the Firebase APIs the app uses (Identity
   Toolkit, Firebase Realtime Database, Firebase Installations, Token Service).

**Verify:** the CI debug APK can still host and join games. A `curl` to the
Identity Toolkit API using the key from a non-Android client gets a 403.

## 4. Turn on Firebase App Check — **optional hardening**

**Why:** the rules limit what a signed-in client can do. App Check limits who
can be a client at all: only a genuine copy of the app from Play gets through.
It needs a Play Console listing and an SDK dependency, so it isn't wired up
in code yet.

**How:** Firebase Console → *App Check* → register the Android app with
**Play Integrity** → watch the metrics for a week → *Enforce* for Realtime
Database. Then add `firebase-appcheck-playintegrity` and initialise it in
`MainActivity.onCreate`.

## 5. Clean up finished games — **optional**

**Why:** games are marked `complete` or `abandoned` but never deleted, so the
database grows slowly. Each game stores `createdAt`, so a cleanup job is
simple.

**How:** a scheduled Cloud Function (Blaze plan) that deletes
`games/*` where `createdAt < now − 24h`. Or occasionally delete the `games`
node by hand in the console when nobody is playing.

## 6. Decide on `google-services.json` in git — **optional**

It is still tracked so CI can build. Given step 3, that's acceptable. To stop
tracking it anyway:
1. Add a GitHub Actions secret `GOOGLE_SERVICES_JSON` containing the file.
2. Add a workflow step that writes it to `app/google-services.json` before
   building.
3. Run `git rm --cached app/google-services.json`.

The file is already listed in `.gitignore`.

---

## Local build environment (Claude Code on the web)

This session couldn't build the Android app locally: the environment's
network policy blocks `dl.google.com`, which serves the Android SDK, AGP and
AndroidX. Every Android build and lint check ran in GitHub Actions instead.
To build locally in a future session, allow `dl.google.com` in the cloud
environment's *Network access* settings.
