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

Consequences:

- every new message costs one line in `res/values/strings.xml`;
- a unit test cannot assert the visible text — it asserts the resource id;
- the number of format arguments is checked by hand (no lint run in this
  sandbox).
