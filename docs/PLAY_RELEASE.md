# Google Play release checklist

Every item must be re-checked on the day of release against official sources
(play.google.com/console, developer.android.com, firebase.google.com).
Numbers below are **not** claimed as current requirements unless a check date
is written next to them.

## Blockers before upload

| # | Item | Status |
|---|---|---|
| 1 | `applicationId` is `com.example.sharedmusic` — **not publishable** | ☐ must change (needs new Firebase app + new `google-services.json`) |
| 2 | Realtime Database security rules deployed | ☐ not deployed (see `docs/FIREBASE_SETUP.md`) |
| 3 | Storage security rules deployed | ☐ not deployed |
| 4 | Room password hash readable by participants | ☐ accepted risk, document in data safety |
| 5 | Audio of users stored on Firebase Storage | ☐ must be declared in Play Data safety |
| 6 | Account deletion: Firebase anonymous account only | ☐ sign-out implemented, full deletion needs a decision |
| 7 | Privacy policy page published | ☐ not published |
| 8 | Release build + AAB signed with an owner-owned keystore | ☐ keystore must be created locally by the owner, never committed |

## Technical

- **Format:** Android App Bundle (AAB), not APK — verify current requirement.
- **Signing:** upload key created by the owner locally, Play App Signing enabled.
  Never commit `*.jks` or `keystore.properties` (already in `.gitignore`).
- **targetSdk:** currently 34 in `gradle/libs.versions.toml`.
  Play requires a recent target API level; check the current rule and date on
  developer.android.com before release.
- **Edge-to-edge:** `enableEdgeToEdge()` + `safeDrawingPadding()` are in place;
  verify on Android 15+.
- **Permissions requested:** `INTERNET`, `ACCESS_NETWORK_STATE`,
  `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`,
  `POST_NOTIFICATIONS`, `WAKE_LOCK`. No broad storage permission — the system
  document picker is used for music files.
- **Background playback:** `MediaSessionService`, foreground service and media
  notification are **not implemented yet** (stage 2). Without them the app may be
  killed in the background; declare this honestly or finish stage 2 first.
- **Native libraries / 16 KB page size:** re-check once Media3 and Firebase
  versions are updated.
- **Copyrighted audio:** the app plays the user's own local files and transfers
  them only between participants of the same room on explicit action.
  The store listing must describe this accurately; no copyrighted audio may be
  shipped inside the app.

## Store listing

- App name: **SHARY**
- Icon: generated from `docs/design/logo-shary.jpg`
  (`res/mipmap-*`, adaptive + monochrome variant for Android 13+).
- Screenshots: player (dark), track list, room, chat, settings.
- Content rating questionnaire: no user-generated public content, chat is
  limited to a private room; answer honestly.
- Data safety form must declare: anonymous identifier, display name, audio files
  uploaded to storage, messages in a room, approximate usage statistics (none yet).
- Closed testing requirements depend on account type and region — check the
  current rule in Play Console Help for this specific project.

## Device testing before upload

- Two or more real devices with different Android versions.
- The full room scenario: create, join, wrong password, kick, chat, host exit.
- Playback: headphones unplugged (`handleAudioBecomingNoisy`), incoming call,
  screen lock, low battery mode (animations may be disabled).
