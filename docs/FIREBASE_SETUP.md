# Firebase setup and security rules

## Project

| Parameter | Value |
|---|---|
| Firebase project | `shary-f5c42` |
| Project number | `94008973913` |
| Client config | `app/google-services.json` (public client ids only) |
| Realtime Database URL | configured in `core/SharyConfig.kt` |
| Storage bucket | `shary-f5c42.firebasestorage.app` |

`google-services.json` does not contain the Realtime Database URL, therefore the
URL is set explicitly in `SharyConfig.DATABASE_URL`. If the database was created
in a different region, replace that constant.

## Console checklist

1. **Authentication** → Sign-in method → **Anonymous** must be enabled.
2. **Realtime Database** → create database → region `europe-west1`.
3. **Storage** → create bucket (default location).
4. Apply the rules below in the console (Rules tab) or via `firebase deploy`.

## Realtime Database rules

These rules are the minimum for the current data model. They are a starting
point and must be reviewed and tested with the Firebase Emulator before release.
They are **not verified by automated tests yet**.

```json
{
  "rules": {
    "rooms": {
      "$code": {
        ".read": "auth != null",
        "info": {
          ".write": "auth != null && (!data.exists() || (root.child('rooms').child($code).child('members').child(auth.uid).exists() && data.parent().child('hostUid').val() === auth.uid))",
          "passwordHash": {
            ".read": "auth != null"
          }
        },
        "members": {
          "$uid": {
            ".write": "auth != null && auth.uid === $uid"
          }
        },
        "tracks": {
          ".write": "auth != null && root.child('rooms').child($code).child('members').child(auth.uid).exists()"
        },
        "playback": {
          ".write": "auth != null && root.child('rooms').child($code).child('members').child(auth.uid).exists() && (!root.child('rooms').child($code).child('info').child('hostOnlyControl').val() || root.child('rooms').child($code).child('info').child('hostUid').val() === auth.uid)"
        },
        "messages": {
          "$messageId": {
            ".write": "auth != null && root.child('rooms').child($code).child('members').child(auth.uid).exists() && newData.child('senderUid').val() === auth.uid && newData.child('text').isString() && newData.child('text').val().length <= 500"
          }
        }
      }
    }
  }
}
```

### Why the old rule broke room creation

The first published version of these rules used `data.parent().parent()`. In
Realtime Database rules `data.parent()` means the **parent node** of the data
being written, and `data.parent().parent()` means the grandparent — `info` has
only one parent (`rooms/$code`), so the second `.parent()` evaluates to null and
the condition is never true.

Two consequences, both seen in the app:

- `!data.exists()` is checked first, so creating a room worked on an empty node;
  but any later write to `info` (password change, `isClosed`, `hostOnlyControl`)
  was rejected with **Permission denied** — and the UI showed
  "Нет соединения с сервером", because every database error was mapped to one
  message.
- after the room was created once, the same rule made re-joining the host
  impossible.

The rule now walks from the root explicitly. `root` is the whole database, so
`root.child('rooms').child($code).child('members').child(auth.uid).exists()` is
the correct membership check.

### Deploying the rules

Rules are not part of the APK: until they are published, the app keeps using
whatever is in the console.

Option A — copy `database.rules.json` into the console (Realtime Database →
Rules → paste → **Publish**).

Option B — deploy from the command line:

```bash
firebase deploy --only database --project shary-f5c42
```

`firebase.json` in the repository root points at `database.rules.json`, so the
file in Git and the rules in the console stay the same.

### Known limitations

- Reading a room is open to any authenticated user who knows the code.
  With a 6-character code from a 32-symbol alphabet this is acceptable for MVP,
  but rate limiting is not implemented.
- `passwordHash` is readable by authenticated users, so a room password
  protects against casual guessing only. The hash is salted with the room code.
- Writing `info` is allowed to any member when the room already exists, which
  is broader than strictly necessary. Tighten it: only the host may change
  `passwordHash`, `hostOnlyControl`, `capacity`.

## Storage rules

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /rooms/{code}/tracks/{file} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.resource.size < 100 * 1024 * 1024;
    }
  }
}
```

### Known limitations

- Files are not deleted automatically after all participants downloaded them.
  Cleanup must be added later (Cloud Function or client-side delete by the
  uploader after confirmation).
- Upload size is limited to 100 MB; typical music files are below that.

## Emulator testing

```bash
firebase emulators:start --only database,storage,auth
```

Rule tests are **not written yet**. Until they exist, the phrase "rules are
verified" must not be used anywhere.
