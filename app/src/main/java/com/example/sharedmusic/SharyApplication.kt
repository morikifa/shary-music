package com.example.sharedmusic

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.example.sharedmusic.core.SharyConfig
import com.example.sharedmusic.data.local.HistoryStore
import com.example.sharedmusic.data.local.ProfileStore
import com.example.sharedmusic.data.local.SettingsStore
import com.example.sharedmusic.data.local.SharyDatabase
import com.example.sharedmusic.data.local.TrackImporter
import com.example.sharedmusic.data.local.TrackStore
import com.example.sharedmusic.data.player.MusicPlayer
import com.example.sharedmusic.data.player.PlayerContainer
import com.example.sharedmusic.data.remote.RoomRemoteSource
import com.example.sharedmusic.data.session.SharySession
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Точка сборки зависимостей.
 *
 * Проект сознательно обходится без DI-фреймворка: для MVP достаточно
 * одного простого контейнера, который создаётся при старте процесса.
 * Все ViewModel получают зависимости отсюда.
 */
class SharyApplication : Application() {

    lateinit var trackStore: TrackStore
        private set

    lateinit var historyStore: HistoryStore
        private set

    lateinit var profileStore: ProfileStore
        private set

    lateinit var settingsStore: SettingsStore
        private set

    lateinit var trackImporter: TrackImporter
        private set

    lateinit var roomRemoteSource: RoomRemoteSource
        private set

    /** Текущая комната и права пользователя в ней */
    lateinit var sharySession: SharySession
        private set

    /** Общая область корутин для задач, не привязанных к экрану */
    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val musicPlayer: MusicPlayer
        get() = PlayerContainer.player(this)

    override fun onCreate() {
        super.onCreate()

        appContext = applicationContext

        // Инициализация Firebase из google-services.json.
        // Если конфигурации нет, приложение продолжает работать локально.
        runCatching { FirebaseApp.initializeApp(this) }

        val database = Room.databaseBuilder(
            this,
            SharyDatabase::class.java,
            SharyDatabase.NAME
        ).build()

        trackStore = TrackStore(database)
        historyStore = HistoryStore(database)
        profileStore = ProfileStore(this)
        settingsStore = SettingsStore(this)
        trackImporter = TrackImporter(this, trackStore)
        roomRemoteSource = RoomRemoteSource(
            databaseUrl = SharyConfig.DATABASE_URL,
            scope = applicationScope
        )
        sharySession = SharySession(roomRemoteSource, applicationScope)

        // Плеер создаётся сразу: звук не должен обрываться при переходах между экранами
        PlayerContainer.player(this)
    }

    companion object {
        /** Контекст приложения для мест, где его нет под рукой */
        lateinit var appContext: Context
    }
}
