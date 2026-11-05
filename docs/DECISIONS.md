# Decisions

Significant product and technical decisions with reasons.

---

## D-001 — New project created inside `morikifa/shary-music`

**Date:** 2026-11-05
**Status:** accepted by product owner

The repository contained only `LICENSE`. The product owner instructed to build
the whole Android project in this repository, taking the logic direction from
`amc3y/SharedMusic` (the partner's repository) and the interface from the design
references.

**Consequence:** the app package is `com.example.sharedmusic` for now because
that package is registered in the provided `google-services.json`.
This must be changed before a Play Store release (see D-008).

---

## D-002 — Audio transfer only on explicit user action

**Date:** 2026-11-05
**Status:** accepted by product owner

The master brief forbids uploading audio to a server, the partner's code
uploaded every shared track to Supabase. The product owner decided to keep
server transfer available.

**Resolution:**

- Tracks are **not** uploaded automatically when a track plays.
- A file is uploaded only when the user taps "add track to room" (explicit action).
- A participant who does not have the file sees "Трек недоступен на вашем устройстве"
  and an explicit download button.
- Storage is Firebase Storage of the same Firebase project as the database,
  so no third-party service and no extra credentials are needed.

**Risk:** audio of users passes through the server. Storage rules must restrict
access to authenticated users, and objects should be deleted after all
participants confirmed the download. This is tracked in `docs/PLAY_RELEASE.md`.

---

## D-003 — Accent colour taken from the logo

**Date:** 2026-11-05

Measured from `docs/design/logo-shary.jpg`: dominant accent `#FD0C7D`
(average over pink pixels `#EF0E77`, background `#0D0D12`).
The muted `#C56C80` visible on the mock-up screenshots is JPEG compression,
not a design decision.

**Palette:** accent `#FD0C7D`, dark background `#101014`, dark surface `#1A1A20`,
light background `#FBF8FA`.

---

## D-004 — Room capacity 2–5, room code 6 characters

**Date:** 2026-11-05

Product brief: "комната с прослушиванием 2 до 5 человек".
Master brief: room code of 6 characters without easily confused symbols.
Alphabet: `ABCDEFGHJKLMNPQRSTUVWXYZ23456789` (32 symbols, no 0/O/1/I).

---

## D-005 — Firebase Realtime Database, not Firestore

**Date:** 2026-11-05
**Status:** inherited from the partner project

Realtime Database provides `onDisconnect()` presence handling and
`/.info/serverTimeOffset` for clock synchronisation, which the room
synchronisation depends on.

---

## D-006 — Track identity is a weak SHA-256 fingerprint

**Date:** 2026-11-05

`SHA-256(displayName|sizeBytes|durationMs)` with the `isWeakId` flag.
Reading file bytes would be exact but expensive for large files.
Collision is possible for different files with identical metadata.

**Consequence:** downloads from a room are stored with the exact `trackId`
that came from the room, so a downloaded track cannot be confused with a
locally imported one.

---

## D-007 — Single player instance per process

**Date:** 2026-11-05

`PlayerContainer` holds one `ExoPlayer` for the whole process, so playback does
not stop when the user navigates between screens, and `ViewModel` recreation
does not interrupt audio.

**Not done yet:** `MediaSessionService`, foreground service and notification
(see `docs/PLAY_RELEASE.md`, stage 2). Until then playback may stop when the
system kills the process in background.

---

## D-008 — `applicationId` must change before release

**Date:** 2026-11-05
**Status:** open

Current `applicationId = com.example.sharedmusic` comes from the supplied
`google-services.json`. Google Play does not accept `com.example.*`.
Changing it requires a new Firebase app registration and a new
`google-services.json`. Tracked in `docs/PLAY_RELEASE.md`.

---

## D-009 — Build versions are a conservative, verified-by-hand combination

**Date:** 2026-11-05
**Status:** needs verification from official sources

AGP 8.5.2 / Kotlin 2.0.20 / Compose BOM 2024.09.00 / compileSdk 34.
The sandbox used for development has **no access** to `dl.google.com` and
`repo1.maven.org`, therefore no build was executed and no version was checked
against official release notes. Before updating versions, verify compatibility
on developer.android.com and firebase.google.com and record the result here.

---

## D-010 — Strings live in `strings.xml`; ViewModels emit resource ids

**Date:** 2026-11-05
**Status:** accepted

Earlier iterations produced user-facing Russian text inside `PlayerViewModel`
and `RoomViewModel` ("Управляет хост комнаты", "Добавлено треков: …"). That
breaks translation, breaks previews, and hides formatting mistakes.

Decision: ViewModels never receive `Context` for messages. They publish
`MessageRes(@StringRes id, args: List<Any>)` and the navigation layer resolves
it with `context.getString(id, *args)` at the moment of showing. Same rule for
model-level labels: `HistoryEntry.modeLabel` and `Track.artistOrUnknown` were
removed and turned into `stringResource` calls in Compose.

Same for the data layer: `FirebaseGate.AuthResult.Failed.reason`,
`RoomRemoteSource.RoomResult.Failure.reason` and `TrackImporter.ImportResult.Failure.reason`
now carry technical codes (`auth_error`, `no_free_code`, `empty_file`), never
sentences. Firebase messages arrive in English anyway, so the user-facing text
is chosen from `kind`/failure type in the ViewModel.

Consequences:

- every new message costs one line in `res/values/strings.xml`;
- a unit test cannot assert the visible text — it asserts the resource id;
- the number of format arguments is checked by hand (no lint run in this
  sandbox).

---

## D-011 — Wi-Fi only blocks room downloads; equalizer is wired to the session

**Date:** 2026-11-05
**Status:** accepted

Two settings were persisted but had no effect on behaviour. Both are now
connected:

- `UserSettings.downloadOverWifiOnly` is checked in `RoomViewModel.downloadTrack`
  and `PlayerViewModel.downloadCurrentRoomTrack` through
  `data/local/NetworkStatus.isUnmetered()`. In a mobile network the download is
  not started and the user sees "Загрузка начнётся только по Wi-Fi…".
- `EqualizerController` is created in `SharyApplication`, attached to the
  ExoPlayer audio session and re-applied on every settings change
  (`SharyApplication.syncEqualizer()` is also called after the first track
  starts, because the session id is only known then).

Sharing a track from the room now opens the library (`+` in the vertical rail
and the pink corner stay where they are): `TrackListScreen.onShareTrack` is
non-null only inside a room, so the same screen serves both entry points
without a second list.

Not implemented on purpose: `MediaSessionService` and background playback
(see D-007), and a 10-band editor — the equalizer exposes four bands mapped
onto real device bands.

---

## D-012 — The player lives on the main thread; everything touching it follows

**Date:** 2026-11-05
**Status:** accepted

The first launch on a device crashed a second after the splash screen. The
cause was not the build: `PlaybackController` started its 200 ms position
ticker in `SharyApplication.applicationScope`, which was
`SupervisorJob() + Dispatchers.Default`. ExoPlayer checks the thread on every
access and throws `IllegalStateException: Player is accessed on the wrong
thread` — the ticker read `currentPosition` from a background thread
immediately, before the first frame was drawn.

The rule now is explicit and applied in three places:

- `SharyApplication.applicationScope` defaults to `Dispatchers.Main.immediate`.
  Heavy work still moves itself to `Dispatchers.IO`.
- `PlaybackController.startTicker()` launches with `Dispatchers.Main.immediate`
  even though the scope it receives may change later.
- `MusicPlayer` owns one thread — the main looper. Every command goes through
  `callOnPlayerThread`, which runs inline on the main thread and posts to the
  main looper from any other. Position, duration and `isPlaying` are read
  through `readOnPlayerThread`, which returns `null` on a foreign thread
  instead of crashing; the wrappers turn that into `0L` / `false`.
  `MusicPlayer` also asserts it is constructed on the main thread.

Consequences:

- the 200 ms tick costs one cheap call on the main thread, not a thread hop;
- a background caller (a download finishing in `RoomViewModel`, for example)
  can call `play()`/`seekTo()` safely — the command is queued, not rejected;
- `kotlinx-coroutines-android` is now an explicit dependency instead of a
  transitive one, because `Dispatchers.Main` is load-bearing.

---

## D-013 — One message per failure; the real reason is shown and logged

**Date:** 2026-11-05
**Status:** accepted

Creating a room failed with "Нет соединения с сервером" while the network was
fine. Every failed Realtime Database write was caught by `runCatching` and
turned into a single `FailureKind.NO_CONNECTION`; the original exception was
dropped, so neither the screen nor logcat said what happened.

Changes:

- `FailureKind` gained `PERMISSION_DENIED` (the database refused the write,
  fixed in the Firebase console) and `NO_FREE_CODE` (all generated codes were
  taken). `NO_CONNECTION` now means the request never reached the server.
- `RemoteErrorClassifier` (domain, pure functions) maps an error message to a
  category: permission keywords, network keywords, otherwise unknown. Unit
  tested in `RemoteErrorClassifierTest`.
- `RoomRemoteSource.failureOf()` logs the original message with `Log.e` under
  the tag `SharyRoom` and passes a shortened copy to the ViewModel.
- `RoomViewModel.noticeDetails` exposes that technical line; `RoomEntryScreen`
  prints it under the main message with `room_error_details`.
- `createRoom` no longer treats a failed code-existence check as "code taken":
  it reports the failure instead of burning all eight attempts.

Consequences:

- the user-facing sentence still comes from `strings.xml` (rule D-010);
- the English Firebase text appears only as a secondary, labelled detail;
- the cause of a room failure is identifiable in one `adb logcat` grep.
