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
          ".write": "auth != null && (!data.exists() || data.parent().parent().child('members/' + auth.uid).exists() && data.parent().child('hostUid').val() === auth.uid)",
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
