# Testing

## Automated tests

### Unit tests (JVM, no device needed)

`app/src/test/java/com/example/sharedmusic/domain/DomainRulesTest.kt`

| Area | What is checked |
|---|---|
| `RoomCode` | length 6, alphabet without `0/O/1/I`, normalisation, invalid short code |
| `ChatRules` | blank text, control characters removed, newline kept, length limit |
| `SyncPolicy` | paused position does not grow, elapsed server time added, thresholds 150/800 ms |
| `VinylGeometry` | position ↔ degrees round trip, clamping to track duration |
| `PlaybackQueue` | next/previous wrap-around, repeat-one, shuffle never returns current |
| `TrackIdentity` | stable hash, different inputs give different ids, hex length 64 |
| `TimeFormat` | mm:ss, h:mm:ss, negative values, hours rounding |
| `ListenDuration` | below a minute, minutes only, hours + minutes, negative input |

Run:

```bash
./gradlew :app:testDebugUnitTest
```

### Instrumented / UI tests

**Not written yet.** Planned: onboarding → player, adding a track, room
creation and join, chat validation.

## Manual checks

These must be repeated on a real device after UI changes.

### Player

1. Add 3 tracks with long and short titles via the "+" button.
2. Check that a long title scrolls right-to-left and fades on both edges.
3. Press play: the vinyl rotates, the play icon becomes pause, controls turn pink.
4. Press pause: rotation stops at the current angle, not at zero.
5. Drag the vinyl clockwise: position grows, the time label updates, and the
   position is applied when the finger is released.
5a. Drag the thin bar under the controls: it turns pink, the left time label
   follows the finger, on release playback jumps to that position.
6. Enable shuffle and repeat-one: icons turn pink, only one is active at a time.
7. Like a track: the heart fills and stays filled after an app restart.
8. Restart the app: the library and liked tracks are still there.
9. Open the list ("Все треки" / "Понравившиеся"): the liked filter shows only
   liked tracks, and an empty result shows "Пока нет понравившихся треков".
10. Add a broken file: the snackbar names the file ("Не удалось добавить файл …"),
    the rest of the selection is still imported.

### Room

1. Create a room of 2 people with a password; the code has 6 characters.
2. Join from a second device with a wrong password → "Неверный пароль комнаты".
3. Join with the correct password → both members appear, host is ringed.
4. Host taps own avatar → room settings, changes the password.
5. Host taps the trash icon on a member → the member is removed.
6. Host plays a track → the guest follows with a drift below 150 ms.
7. Guest pauses the network → status shows "Догоняю…"/offline message, without crashing.
8. Guest without the file sees "Трек недоступен на вашем устройстве" and
   can download the track with the download button.
9. Chat: send an empty message → validation error; a 600-character message is
   cut at 500; messages arrive on both devices.
10. Host leaves the room → the guest returns to local playback with a message.

### Settings and profile

1. Equalizer: enabling changes the sound on a device that supports it; on a
   device without support nothing breaks.
2. Wi-Fi only: the switch survives an app restart. With the switch on and the
   phone on mobile data, downloading a room track shows "Загрузка начнётся
   только по Wi-Fi…" and does not start; on Wi-Fi the same action downloads.
3. Theme: dark / light / system applied immediately and after restart.
4. Profile: change the name and the avatar; the new avatar appears in a room.
5. History: entries appear grouped by day, "Очистить историю" empties the list.

### Messages

1. Every ViewModel message appears as a snackbar and disappears once
   (`MessageRes` → `context.getString`). A missing format argument shows up as
   a raw `%1$d` in the text.
2. Copy the room code: "Код скопирован" appears; the share sheet contains the
   room code in the text.
3. Guest tries to control playback when the host restricted it:
   "Управляет хост комнаты".

## Known gaps

- No Firebase security-rules tests (needs the Emulator).
- No screenshot tests.
- Background playback is not implemented yet, so it is not tested.
