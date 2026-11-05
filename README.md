# SHARY

Android-приложение для совместного прослушивания музыки, которая уже есть на
телефоне. Двое и больше человек входят в одну комнату, слушают один трек
синхронно и переписываются в чате.

- Kotlin + Jetpack Compose + Material 3
- Media3 ExoPlayer, Room, DataStore, Coil
- Firebase Auth (анонимно) + Realtime Database + Storage

## Сборка

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

Требуется JDK 17 и Android SDK (compileSdk 34).

## Структура

```
app/src/main/java/com/example/sharedmusic/
├── core/        константы приложения
├── domain/      чистая логика: код комнаты, чат, синхронизация, время
├── data/        Room, DataStore, ExoPlayer, Firebase
├── viewmodel/   состояние экранов
└── ui/          Compose-экраны, компоненты, тема, навигация
```

Подробности: `AGENTS.md`, `docs/`.
