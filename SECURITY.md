# SECURITY.md

## Firebase API key exposure

The file `app/google-services.json` was committed to this repo at the
initial commit and contains the live Firebase project's web API key plus
project number. Anyone with read access to the repo (or to the public
GitHub clone) can extract these values.

Combined with the current Firebase Realtime Database security model
(anonymous auth allowed, default open rules), this means **any person
with the repo URL can sign in anonymously and read/write any node under
`games/`** in the Realtime Database. They can:

- Enumerate active game codes
- Inject puzzle data into games in progress
- Manipulate scores and turn state on live games
- Create arbitrary game nodes that consume database storage

### Mitigation steps (must be done manually)

1. **Rotate the API key.** Open Firebase Console for the project →
   Project settings → General → "Web API Key" → restrict / regenerate.
   While you're there, also add SHA-1 fingerprint restrictions to the
   Android app entry so the key is bound to your debug keystore.

2. **Tighten Realtime Database rules.** In the Firebase Console →
   Realtime Database → Rules. Replace whatever's there with something
   like:

   ```json
   {
     "rules": {
       "games": {
         "$code": {
           ".read":  "auth != null",
           ".write": "auth != null"
         }
       }
     }
   }
   ```

   This still allows any anonymous-authed user to read/write game
   nodes, but at least requires authentication. For a tighter model,
   constrain writes by `hostName`/`guestName` matching the auth uid.

3. **Stop committing `google-services.json`.** This file is now in
   `.gitignore`, but the existing tracked copy still updates if anyone
   `git add`s a new version. To fully remove from version control:

   ```sh
   git rm --cached app/google-services.json
   git commit -m "Stop tracking Firebase config"
   ```

   For CI to keep building after that, add a GitHub Actions secret
   named `GOOGLE_SERVICES_JSON` containing the file's contents and add
   a workflow step:

   ```yaml
   - name: Restore google-services.json
     run: |
       echo "$GOOGLE_SERVICES_JSON" > app/google-services.json
     env:
       GOOGLE_SERVICES_JSON: ${{ secrets.GOOGLE_SERVICES_JSON }}
   ```

   Do step 3 *after* step 1 — otherwise you're committing the
   untracked but already-leaked key.

4. **Note:** rewriting git history (`git filter-repo` / BFG) to scrub
   the old key from past commits does not actually un-leak it — anyone
   with a clone or a GitHub mirror still has the key. **Treat the
   current key as compromised regardless** and rotate.

## Other notes

- The 6-letter game codes are not collision-resistant for high-traffic
  use (32^6 ≈ 1B codes, but `createGame` uses `setValue` and would
  silently overwrite a colliding active game). At single-user scale
  this is fine.
- Closed/abandoned games are never deleted from the DB — storage grows
  unbounded over time. Add a Firebase Cloud Function on a schedule to
  prune nodes with `status == "complete"` older than 24h if this ever
  matters.
