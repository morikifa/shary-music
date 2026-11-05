# UI contract

State and events of every screen. A screen is a function of its state:
data flows down, events flow up.

---

## 1. Player screen (`ui/screens/PlayerScreen.kt`)

### State (parameters)

| Parameter | Type | Meaning |
|---|---|---|
| `tracks` | `List<Track>` | Library tracks, already filtered by the search query |
| `currentTrack` | `Track?` | Track that is loaded in the player, `null` when nothing is selected |
| `isPlaying` | `Boolean` | Vinyl rotates, play button shows pause, controls are highlighted |
| `positionMs` | `Long` | Current playback position |
| `durationMs` | `Long` | Duration of the current track, `0` when unknown |
| `shuffle` | `Boolean` | Random order is on (icon is pink) |
| `repeatOne` | `Boolean` | Repeat-one is on (icon is pink) |
| `syncStatus` | `RoomSyncStatus` | `Synced` / `CatchingUp` / `TrackMissing` / `Offline` |
| `roomCode` | `String?` | Non-null in room mode; also drives the "Комната" chip |
| `userRole` | `RoomRole` | `HOST` or `MEMBER`; a member cannot control playback when the host restricted it |
| `searchQuery` | `String` | Text in the search field |
| `isSearchActive` | `Boolean` | Search field is expanded |

### Events

| Event | Parameters | Meaning |
|---|---|---|
| `onPlayPause` | — | Toggle play/pause |
| `onNext` / `onPrevious` | — | Move through the queue |
| `onToggleShuffle` / `onToggleRepeat` | — | Toggle playback modes |
| `onToggleLike` | `Track` | Add/remove from liked |
| `onSeek` | `Long` | Seek to position (slider, vertical bar) |
| `onVinylDragStart` / `onVinylDrag` / `onVinylDragEnd` | `Float` delta degrees | Circular scrub on the vinyl |
| `onSelectTrack` | `Track` | Play a track from the cover carousel or list |
| `onAddTracks` | `List<Uri>` | Files chosen in the system picker |
| `onSearchChange` / `onSearchOpen` / `onSearchClose` | — | Search field lifecycle |
| `onOpenList` | — | Pink corner → track list |
| `onOpenRoom` / `onOpenChat` | — | Room entry or room chat |
| `onOpenSettings` / `onOpenProfile` | — | Settings / profile |
| `onDownloadTrack` | — | Download the current room track missing on this device |

`isImporting: Boolean` (state) disables the "+" button and shows
"Добавляем файлы…" while the picker results are copied and read.

### States

- **Empty library:** the "no tracks" text is shown instead of the carousel.
- **Track missing (room):** status row shows "Трек недоступен на вашем
  устройстве" and a download button; playback controls stay disabled.
- **Guest without rights:** controls are disabled, the title says who controls playback.
- **Loading:** not represented on this screen (local data is instant).

---

## 2. Track list (`ui/screens/TrackListScreen.kt`)

| Parameter | Meaning |
|---|---|
| `tracks` | Filtered library |
| `currentTrackId` | Highlighted row |
| `isPlaying` | Animated bars next to the current row |
| `searchQuery`, `isSearchActive` | Search state |
| `canDelete` | Whether the delete action is available |

The screen keeps a local filter chip row: **Все треки** / **Понравившиеся**.
The filter is applied on top of the search query; an empty result shows a
dedicated message instead of the generic "no tracks" text.

Events: `onTrackClick`, `onToggleLike`, `onDelete`, `onSearchChange`,
`onSearchOpen`, `onSearchClose`, `onBack`, and the optional `onShareTrack`.

`onShareTrack` is non-null only when the list was opened from an active room:
each row then shows a send icon that uploads that track to the room and returns
to the room screen.

States: empty library, no search results, delete confirmation dialog.

---

## 3. Room entry (`ui/screens/RoomEntryScreen.kt`)

| Parameter | Meaning |
|---|---|
| `isLoading` | Server request in progress |
| `notice` | Human-readable error or confirmation |

Events: `onCreateRoom(capacity, password)`, `onJoinRoom(code, password)`, `onBack`.

States: create form, join form, error text, disabled primary button while the
code is incomplete.

---

## 4. Room (`ui/screens/RoomScreen.kt`)

| Parameter | Meaning |
|---|---|
| `roomCode` | Shown in the header and copied / shared |
| `members` | Participants; host is ringed and slightly larger |
| `roomTracks` | Room playlist with `isAvailableLocally` and `addedByMe` |
| `currentTrack`, `currentTrackId`, `currentTrackTitle` | Track being played |
| `isPlaying`, `positionMs`, `durationMs` | Playback state |
| `shuffle`, `repeatOne` | Playback modes |
| `syncStatus` | Same semantics as the player screen |
| `currentUid` | To mark "Вы" and to distinguish the host |
| `isHost`, `canControl` | Rights for this device |
| `transferProgress` | `null` or upload/download percent |

Events: `onLeaveRoom`, `onCopyCode`, `onShareCode`, `onOpenRoomSettings`,
`onKickMember`, `onSelectTrack`, `onDownloadTrack`, `onPlayPause`, `onNext`,
`onPrevious`, `onToggleShuffle`, `onToggleRepeat`, `onToggleLike`, `onSeek`,
`onOpenChat`, `onOpenLibraryToShare`.

States: host / member, track missing, transfer in progress, kick confirmation,
room closed by host.

---

## 5. Chat (`ui/screens/ChatSheet.kt`)

| Parameter | Meaning |
|---|---|
| `messages` | Sorted by time, own messages on the right |
| `input` | Current text (max 500 characters) |
| `error` | Validation or send error |
| `currentUid` | Distinguishes own messages |
| `isSending` | Disables the send button while a message is in flight |

Events: `onInputChange`, `onSend`, `onClose`.
States: empty chat, error, offline.

---

## 6. Settings, profile, history, onboarding

- **Settings** — `onToggleEqualizer(Boolean)`, `onChangeEqualizerBand(Int, Float)`,
  `onToggleWifiOnly(Boolean)`, `onChangeTheme(ThemeMode)`.
- **Profile** — `onSaveName(String)`, `onSaveAvatar(String?)`, `onOpenHistory`,
  `onSignOut`; state: profile, track count, liked count, hours in rooms.
- **History** — `onClear`, `onBack`; entries grouped by day.
- **Onboarding** — `onContinue(String)`; only a display name is requested.

---

## Text resources

No user-facing text is hardcoded in Kotlin. ViewModels never touch `Context`;
they emit `MessageRes(id: Int, args: List<Any>)` (see
`viewmodel/MessageRes.kt`), and the navigation layer resolves it through
`context.getString(id, *args)` before showing a snackbar, an inline notice or a
validation error. This keeps every string in `res/values/strings.xml`
translatable and survives configuration changes.

---

## Data contracts expected from the data layer

| Value | Source | Notes |
|---|---|---|
| `Track.artworkPath` | extracted from audio tags on import | `null` means gradient placeholder |
| `Track.durationMs` | `MediaMetadataRetriever` on import | `0` when unknown |
| `RoomPlayback.updatedAtServerMs` | `ServerValue.TIMESTAMP` | must be server time, not client time |
| `ChatMessage.sentAtServerMs` | `ServerValue.TIMESTAMP` | client time is only a fallback |
| `serverNowMs()` | `/.info/serverTimeOffset` | used to compensate clock skew |
