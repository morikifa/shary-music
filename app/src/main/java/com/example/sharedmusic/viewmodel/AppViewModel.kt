package com.example.sharedmusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sharedmusic.SharyApplication
import com.example.sharedmusic.data.local.AppFiles
import com.example.sharedmusic.data.model.HistoryEntry
import com.example.sharedmusic.data.model.ThemeMode
import com.example.sharedmusic.data.model.UserProfile
import com.example.sharedmusic.data.model.UserSettings
import com.example.sharedmusic.data.remote.FirebaseGate
import com.example.sharedmusic.domain.TimeFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Состояние, которое нужно всему приложению: профиль, настройки, история.
 * Здесь же — действия профиля, чтобы экраны оставались «тонкими».
 */
class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SharyApplication

    /** Активная комната: используется шапкой плеера и переходами */
    val roomCode: StateFlow<String?> = app.sharySession.roomCode

    /** Роль пользователя в комнате */
    val role: StateFlow<com.example.sharedmusic.data.model.RoomRole> = app.sharySession.role

    val profile: StateFlow<UserProfile> = app.profileStore.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserProfile())

    /** Прочитан ли профиль: до первого значения нельзя выбирать стартовый экран */
    private val _profileLoaded = MutableStateFlow(false)
    val profileLoaded: StateFlow<Boolean> = _profileLoaded.asStateFlow()

    val settings: StateFlow<UserSettings> = app.settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettings())

    val history: StateFlow<List<HistoryEntry>> = app.historyStore.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _trackCount = MutableStateFlow(0)
    val trackCount: StateFlow<Int> = _trackCount.asStateFlow()

    private val _likedCount = MutableStateFlow(0)
    val likedCount: StateFlow<Int> = _likedCount.asStateFlow()

    private val _roomHours = MutableStateFlow("0.0")
    val roomHours: StateFlow<String> = _roomHours.asStateFlow()

    init {
        viewModelScope.launch {
            app.profileStore.profile.collect { _profileLoaded.value = true }
        }

        viewModelScope.launch {
            app.trackStore.observeTracks().collect { tracks ->
                _trackCount.value = tracks.size
                _likedCount.value = tracks.count { it.isLiked }
            }
        }

        viewModelScope.launch {
            val totalMs = runCatching { app.historyStore.totalRoomListenMs() }.getOrDefault(0L)
            _roomHours.value = TimeFormat.hours(totalMs)
        }

        // Анонимный вход выполняется сразу, чтобы uid был готов к моменту создания комнаты
        viewModelScope.launch {
            val result = FirebaseGate.ensureSignedIn()

            if (result is FirebaseGate.AuthResult.Ready) {
                app.profileStore.saveUid(result.uid)
            }
        }
    }

    /** Сохраняет имя профиля */
    fun saveDisplayName(name: String) {
        if (name.isBlank()) return

        viewModelScope.launch {
            app.profileStore.saveDisplayName(name)
        }
    }

    /**
     * Сохраняет аватар. Пикер отдаёт content:// URI, а он может потерять
     * доступ после перезапуска, поэтому копируем изображение в filesDir.
     */
    fun saveAvatar(sourceUri: String?) {
        viewModelScope.launch {
            if (sourceUri == null) {
                app.profileStore.saveAvatar(null)
                return@launch
            }

            val savedPath = withContext(Dispatchers.IO) {
                runCatching {
                    val uri = android.net.Uri.parse(sourceUri)
                    val destination = AppFiles.newAvatarFile(app)

                    app.contentResolver.openInputStream(uri)?.use { input ->
                        destination.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    } ?: return@runCatching null

                    if (destination.length() == 0L) {
                        destination.delete()
                        null
                    } else {
                        destination.absolutePath
                    }
                }.getOrNull()
            }

            if (savedPath != null) {
                // Старый файл аватара больше не нужен
                profile.value.avatarPath?.let { oldPath -> File(oldPath).delete() }
                app.profileStore.saveAvatar(savedPath)
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { app.settingsStore.setThemeMode(mode) }
    }

    fun setWifiOnly(enabled: Boolean) {
        viewModelScope.launch { app.settingsStore.setWifiOnly(enabled) }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        viewModelScope.launch { app.settingsStore.setEqualizerEnabled(enabled) }
    }

    fun setEqualizerBand(index: Int, decibels: Float) {
        viewModelScope.launch { app.settingsStore.setEqualizerBand(index, decibels) }
    }

    fun clearHistory() {
        viewModelScope.launch { app.historyStore.clear() }
    }

    /** Выход из профиля: анонимный аккаунт сбрасывается, библиотека остаётся */
    fun signOut() {
        viewModelScope.launch {
            FirebaseGate.signOut()
            app.profileStore.clear()

            val result = FirebaseGate.ensureSignedIn()
            if (result is FirebaseGate.AuthResult.Ready) {
                app.profileStore.saveUid(result.uid)
            }
        }
    }
}
