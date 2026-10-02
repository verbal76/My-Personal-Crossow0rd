# SECURITY.md

## Threat model

The app is offline-first. The only networked feature is two-device
**online Team and Vindictive play** over Firebase Realtime Database, with
anonymous Firebase Auth. There are no accounts, no payments and no personal
data beyond the display names players type in. The app includes no analytics
or advertising SDKs (Firebase Analytics was removed), so nothing is sent on
launch.

Android backup and device transfer copy only the game's own preferences file
(`CrosswordSaves`). Firebase sign-in state is never restored onto another
phone.

## The Firebase API key

`app/google-services.json` contains the project's Android API key. Firebase
Android keys are **identifiers, not secrets**: they ship inside every APK and
anyone can extract them. What keeps the data safe is:

1. **Realtime Database security rules**, in `firebase/database.rules.json`.
   These are deployed from the console; see `docs/EXTERNAL_ACTIONS.md` §1.
2. **API key restrictions** to the app's package and signing SHA-1
   (§4 of the same file). These only bind the key to *this* app once it is
   signed with a private release key (§3).
3. Optionally, **App Check** (§5), once the app is on Play.

Until the rules in §1 are deployed, assume any signed-in client can read and
write any game node.

## Signing

Every APK built so far is a debug build signed with the committed
`debug.keystore`, so its signature proves nothing about who built it. The
build signs release APKs with a private key supplied through CI secrets
(`docs/EXTERNAL_ACTIONS.md` §3); that key doesn't exist yet. The key and its
passwords must never be committed.

## Online play: what the client enforces

- The game code is claimed with a transaction, so a new game can't overwrite
  a live one.
- The guest seat is claimed with a transaction, so two phones can't both take
  it.
- Each game records `hostUid`, `guestUid` and `createdAt`, so the rules can
  bind writes to the two seated players and expire lobbies after 15 minutes.
- Each player's connection is tracked (`hostOnline`/`guestOnline`, set false
  by the server when it drops). After 30 seconds offline the other phone ends
  the game and the player is told.
- Each device writes only its own score field. Letters received from the other
  device are accepted only if they are the correct solution letter, so a
  tampered remote client can't plant wrong letters or erase yours.

## Game codes

Codes are 6 characters from a 32-symbol alphabet (no 0/O/1/I): 32⁶ ≈ 1.07
billion codes. With the rules deployed:
- Guessing a code only exposes a game that is **still waiting** in its lobby.
  It shows the host's display name and mode, never the puzzle once play
  starts.
- Lobbies expire after 15 minutes.
- At a sustained 100 guesses per second, finding one of 10 open lobbies would
  take about 12 days, far longer than any lobby exists.
- The worst outcome of a successful guess is joining a stranger's lobby.

Longer codes would cost usability (typing them in) for little gain at this
scale.

## Reporting

This is a personal project. Report problems to the repository owner.
