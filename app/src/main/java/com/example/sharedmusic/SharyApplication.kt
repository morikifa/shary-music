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
import com.example.sharedmusic.data.player.EqualizerController
import com.example.sharedmusic.data.player.MusicPlayer
import com.example.sharedmusic.data.player.PlayerContainer
import com.example.sharedmusic.data.remote.RoomRemoteSource
import com.example.sharedmusic.data.session.SharySession
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    /** Эквалайзер поверх аудиосессии плеера */
    lateinit var equalizer: EqualizerController
        private set

    /**
     * Общая область корутин для задач, не привязанных к экрану.
     *
     * По умолчанию это главный поток: всё, что рядом с ExoPlayer и
     * Equalizer, обязано исполняться там же, где они созданы. Тяжёлые
     * операции сами уходят в `withContext(Dispatchers.IO)`.
     */
    val applicationScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

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

        // Эквалайзер подключается к аудиосессии и повторяет сохранённые настройки.
        // Аудиосессия ExoPlayer доступна только с потока плеера, поэтому
        // подписка живёт в области приложения с главным потоком по умолчанию.
        equalizer = EqualizerController()
        applicationScope.launch {
            val player = PlayerContainer.player(this@SharyApplication)
            equalizer.attach(player.audioSessionId)
            settingsStore.settings.collect { equalizer.apply(it) }
        }
    }

    /**
     * Подключает эквалайзер к актуальной аудиосессии и применяет настройки.
     * Сession id становится известен только после старта воспроизведения,
     * поэтому вызывается не только при запуске процесса.
     */
    suspend fun syncEqualizer() = withContext(Dispatchers.Main.immediate) {
        val sessionId = PlayerContainer.player(this@SharyApplication).audioSessionId
        if (sessionId <= 0) return@withContext

        equalizer.attach(sessionId)
        equalizer.apply(settingsStore.settings.first())
    }

    companion object {
        /** Контекст приложения для мест, где его нет под рукой */
        lateinit var appContext: Context
    }
}
