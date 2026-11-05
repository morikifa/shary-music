package com.example.sharedmusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sharedmusic.R
import com.example.sharedmusic.SharyApplication
import com.example.sharedmusic.core.SharyConfig
import com.example.sharedmusic.data.local.TrackImporter
import com.example.sharedmusic.data.model.HistoryEntry
import com.example.sharedmusic.data.model.RoomPlayback
import com.example.sharedmusic.data.model.RoomRole
import com.example.sharedmusic.data.model.RoomSyncStatus
import com.example.sharedmusic.data.model.Track
import com.example.sharedmusic.data.model.UserProfile
import com.example.sharedmusic.data.remote.FirebaseGate
import com.example.sharedmusic.domain.PlaybackQueue
import com.example.sharedmusic.domain.SyncDecision
import com.example.sharedmusic.domain.VinylGeometry
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/**
 * Экран «Сейчас играет»: единственное место, где собирается состояние плеера
 * из библиотеки, ExoPlayer и комнаты.
 */
class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SharyApplication
    private val session = app.sharySession
    private val remote = app.roomRemoteSource

    // Плеер общий для всех экранов, поэтому цикл обновления живёт в области приложения
    private val playback = PlaybackController(app.musicPlayer, app.applicationScope)

    val tracks: StateFlow<List<Track>> = app.trackStore
        .observeTracks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _currentTrackId = MutableStateFlow<String?>(null)
    val currentTrackId: StateFlow<String?> = _currentTrackId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    /** Треки с учётом поиска: используется лентой обложек и списком */
    val visibleTracks: StateFlow<List<Track>> = combine(
        tracks,
        _searchQuery,
        _isSearchActive
    ) { allTracks, query, searchActive ->
        val trimmed = query.trim()

        if (trimmed.isEmpty()) {
            allTracks
        } else {
            val lower = trimmed.lowercase()
            allTracks.filter { track ->
                track.title.lowercase().contains(lower) ||
                        track.artist.lowercase().contains(lower)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _shuffle = MutableStateFlow(false)
    val shuffle: StateFlow<Boolean> = _shuffle.asStateFlow()

    private val _repeatOne = MutableStateFlow(false)
    val repeatOne: StateFlow<Boolean> = _repeatOne.asStateFlow()

    private val _syncStatus = MutableStateFlow<RoomSyncStatus>(RoomSyncStatus.Synced)
    val syncStatus: StateFlow<RoomSyncStatus> = _syncStatus.asStateFlow()

    /** Сообщение пользователю: идентификатор строкового ресурса и аргументы */
    private val _message = MutableStateFlow<MessageRes?>(null)
    val message: StateFlow<MessageRes?> = _message.asStateFlow()

    private val _isTransferring = MutableStateFlow(false)
    val isTransferring: StateFlow<Boolean> = _isTransferring.asStateFlow()

    /** Идёт импорт файлов из пикера: показываем подпись «Добавляем файлы…» */
    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    val profile: StateFlow<UserProfile> = app.profileStore.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserProfile())

    val isPlaying: StateFlow<Boolean> = playback.isPlaying
    val positionMs: StateFlow<Long> = playback.positionMs
    val durationMs: StateFlow<Long> = playback.durationMs

    /** Накопленный пользователем угол перемотки пластинки */
    private var seekDegrees = 0f

    /** Позиция во время перемотки — для подписи времени */
    private val _scrubPositionMs = MutableStateFlow<Long?>(null)
    val scrubPositionMs: StateFlow<Long?> = _scrubPositionMs.asStateFlow()

    private var syncJob: Job? = null
    private var heartbeatJob: Job? = null
    private var lastPublishedKey: String = ""

    /** Признак того, что гость уже показывал статус «Трек недоступен» */
    private var missingNotified = false

    /** Время начала текущего прослушивания для записи в историю */
    private var listenStartedAtMs: Long = 0L

    init {
        playback.onTrackCompleted = { handleTrackCompleted() }
        playback.onError = { _message.value = MessageRes(R.string.error_player) }

        playback.onPublishState = { trackId, positionMs, isPlaying ->
            publishToRoom(trackId, positionMs, isPlaying)
        }

        // Следим за удалением текущего трека из библиотеки
        viewModelScope.launch {
            app.trackStore.observeTracks().collect { list ->
                val currentId = _currentTrackId.value ?: return@collect
                if (list.none { it.id == currentId }) {
                    playback.stop()
                    _currentTrackId.value = null
                }
            }
        }

        startRoomSync()
    }

    // ---------- Управление воспроизведением ----------

    /** Запускает трек из ленты обложек или списка */
    fun playTrack(track: Track) {
        if (!File(track.filePath).exists()) {
            _message.value = MessageRes(R.string.error_player_file_missing)
            return
        }

        if (!session.canControl) {
            _message.value = MessageRes(R.string.sync_guest_locked)
            return
        }

        _currentTrackId.value = track.id
        playback.play(
            trackId = track.id,
            filePath = track.filePath,
            title = track.title,
            artist = track.artist
        )

        lastPublishedKey = ""
        recordListenStart(track)
    }

    fun togglePlayPause() {
        val current = currentTrack()

        if (current == null) {
            tracks.value.firstOrNull()?.let { playTrack(it) }
            return
        }

        if (!session.canControl) {
            _message.value = MessageRes(R.string.sync_guest_locked)
            return
        }

        val wasPlaying = playback.isPlaying.value
        playback.toggle()

        if (wasPlaying) {
            closeListenRecord()
        } else {
            // Возврат с паузы: продолжаем тем же треком без новой записи старта
            if (listenStartedAtMs <= 0L) {
                listenStartedAtMs = System.currentTimeMillis()
            }
        }
    }

    fun playNext() = playNextInternal(auto = false)

    fun playPrevious() {
        val list = queueTracks()
        if (list.isEmpty()) return

        if (!session.canControl) {
            _message.value = MessageRes(R.string.sync_guest_locked)
            return
        }

        val previousId = PlaybackQueue.previous(
            tracks = list.map { it.id },
            currentId = _currentTrackId.value,
            shuffle = _shuffle.value
        )

        list.firstOrNull { it.id == previousId }?.let { playTrack(it) }
    }

    /** Перемотка ползунком и вертикальной шкалой */
    fun seekTo(positionMs: Long) {
        if (!session.canControl) {
            _message.value = MessageRes(R.string.sync_guest_locked)
            return
        }

        playback.seekTo(positionMs)
    }

    /** Начало перемотки вращением пластинки */
    fun onVinylDragStart() {
        seekDegrees = 0f
        _scrubPositionMs.value = positionMs.value
    }

    /** Поворот пластинки: положительная дельта — вперёд */
    fun onVinylDrag(deltaDegrees: Float) {
        val duration = durationMs.value
        if (duration <= 0L) return

        if (!session.canControl) return

        seekDegrees = (seekDegrees + deltaDegrees)
            .coerceIn(0f, VinylGeometry.degreesPerTrack())

        val target = VinylGeometry.degreesToPosition(seekDegrees, duration)
        _scrubPositionMs.value = target
    }

    /** Конец перемотки: позиция применяется к плееру */
    fun onVinylDragEnd() {
        val target = _scrubPositionMs.value
        _scrubPositionMs.value = null
        seekDegrees = 0f

        if (target != null) {
            playback.seekTo(target)
        }
    }

    fun toggleShuffle() {
        _shuffle.value = !_shuffle.value
        if (_shuffle.value) _repeatOne.value = false
    }

    fun toggleRepeatOne() {
        _repeatOne.value = !_repeatOne.value
        if (_repeatOne.value) _shuffle.value = false
    }

    fun toggleLike(track: Track) {
        viewModelScope.launch {
            app.trackStore.setLiked(track.id, !track.isLiked)
        }
    }

    // ---------- Поиск ----------

    fun openSearch() {
        _isSearchActive.value = true
    }

    fun closeSearch() {
        _isSearchActive.value = false
        _searchQuery.value = ""
    }

    fun updateSearch(query: String) {
        _searchQuery.value = query.take(SharyConfig.MAX_TITLE_LENGTH)
    }

    fun currentTrack(): Track? =
        tracks.value.firstOrNull { it.id == _currentTrackId.value }

    /** Очередь воспроизведения: плейлист комнаты или вся библиотека */
    fun queueTracks(): List<Track> {
        val all = tracks.value

        if (!session.isInRoom) return all

        val roomTracks = remote.tracks.value
        if (roomTracks.isEmpty()) return all

        val byId = all.associateBy { it.id }
        return roomTracks.mapNotNull { byId[it.trackId] }
    }

    /** Добавляет файлы из системного пикера */
    fun addTracks(uris: List<android.net.Uri>) {
        if (uris.isEmpty()) return

        viewModelScope.launch {
            _isImporting.value = true
            val results = app.trackImporter.import(uris)
            _isImporting.value = false

            val added = results.filterIsInstance<TrackImporter.ImportResult.Success>()
            val failed = results.filterIsInstance<TrackImporter.ImportResult.Failure>()

            when {
                // Один файл — называем его, несколько — сообщаем количество
                failed.size == 1 && added.isEmpty() ->
                    _message.value = MessageRes(
                        R.string.library_import_failed,
                        listOf(failed.first().fileName)
                    )
                failed.isNotEmpty() ->
                    _message.value = MessageRes(R.string.library_failed_count, listOf(failed.size))
                added.isNotEmpty() ->
                    _message.value = MessageRes(R.string.library_added_count, listOf(added.size))
                else -> _message.value = MessageRes(R.string.library_duplicates_only)
            }
        }
    }

    /** Скачивает текущий трек комнаты, которого нет на устройстве */
    fun downloadCurrentRoomTrack() {
        val roomTrack = remote.tracks.value.firstOrNull {
            it.trackId == _currentTrackId.value
        } ?: return

        if (roomTrack.downloadUrl.isBlank() || _isTransferring.value) return

        viewModelScope.launch {
            _isTransferring.value = true

            val destination = File(app.cacheDir, "room_${roomTrack.trackId}.audio")

            val downloaded = remote.downloadTrack(roomTrack, destination) { }

            if (!downloaded) {
                _isTransferring.value = false
                _message.value = MessageRes(R.string.room_track_download_failed)
                return@launch
            }

            val imported = app.trackImporter.importDownloadedFile(
                file = destination,
                trackId = roomTrack.trackId,
                title = roomTrack.title,
                artist = roomTrack.artist,
                durationMs = 0L
            )

            destination.delete()
            _isTransferring.value = false

            _message.value = if (imported != null) {
                missingNotified = false
                MessageRes(R.string.track_added_to_library)
            } else {
                MessageRes(R.string.track_add_failed)
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    /** Удаляет трек из библиотеки вместе с файлом */
    fun deleteTrack(track: Track) {
        viewModelScope.launch {
            runCatching { app.trackStore.delete(track) }
                .onFailure { _message.value = MessageRes(R.string.track_delete_failed) }
        }
    }

    // ---------- Синхронизация комнаты ----------

    private fun startRoomSync() {
        syncJob?.cancel()
        heartbeatJob?.cancel()

        syncJob = viewModelScope.launch {
            while (isActive) {
                val code = session.roomCode.value

                if (code == null) {
                    if (_syncStatus.value !is RoomSyncStatus.Synced) {
                        _syncStatus.value = RoomSyncStatus.Synced
                    }
                    delay(SharyConfig.SYNC_CHECK_PERIOD_MS)
                    continue
                }

                if (!session.isHost) {
                    applyRoomState()
                } else {
                    publishIfChanged()
                }

                delay(SharyConfig.SYNC_CHECK_PERIOD_MS)
            }
        }

        // Хост раз в две секунды отправляет контрольную позицию
        heartbeatJob = viewModelScope.launch {
            while (isActive) {
                delay(SharyConfig.HEARTBEAT_PERIOD_MS)

                if (session.isHost && playback.isPlaying.value) {
                    val track = currentTrack() ?: continue
                    publishToRoom(track.id, positionMs.value, true)
                }
            }
        }
    }

    /** Гость подстраивается под состояние хоста */
    private suspend fun applyRoomState() {
        val state: RoomPlayback = remote.playback.value

        if (state.trackId.isBlank()) return

        val localTrack = tracks.value.firstOrNull { it.id == state.trackId }

        if (localTrack == null) {
            _currentTrackId.value = state.trackId
            _syncStatus.value = RoomSyncStatus.TrackMissing

            if (!missingNotified) {
                missingNotified = true
                _message.value = MessageRes(R.string.sync_track_missing)
            }
            return
        }

        val decision = playback.applyRemoteState(
            state = state,
            currentServerMs = remote.serverNowMs(),
            localFilePath = localTrack.filePath,
            localTitle = localTrack.title,
            localArtist = localTrack.artist
        )

        _currentTrackId.value = state.trackId

        _syncStatus.value = when {
            !state.isPlaying -> RoomSyncStatus.Synced
            decision == null -> RoomSyncStatus.Synced
            decision == SyncDecision.IGNORE -> RoomSyncStatus.Synced
            else -> RoomSyncStatus.CatchingUp
        }
    }

    private fun publishIfChanged() {
        val track = currentTrack() ?: return
        val key = "${track.id}:${playback.isPlaying.value}:${_syncStatus.value}"

        if (key == lastPublishedKey) return
        lastPublishedKey = key

        publishToRoom(track.id, positionMs.value, playback.isPlaying.value)
    }

    private fun publishToRoom(trackId: String, positionMs: Long, isPlaying: Boolean) {
        if (!session.isHost) return
        if (session.roomCode.value == null) return

        val uid = FirebaseGate.currentUid ?: return

        viewModelScope.launch {
            remote.publishPlayback(
                trackId = trackId,
                title = currentTrack()?.title.orEmpty(),
                positionMs = positionMs,
                isPlaying = isPlaying,
                uid = uid
            )
        }
    }

    private fun handleTrackCompleted() {
        val list = queueTracks()

        if (list.isEmpty()) {
            playback.stop()
            return
        }

        // Гость не выбирает трек сам — он ждёт команды хоста
        if (!session.canControl) return

        val nextId = PlaybackQueue.next(
            tracks = list.map { it.id },
            currentId = _currentTrackId.value,
            repeatOne = _repeatOne.value,
            shuffle = _shuffle.value
        )

        list.firstOrNull { it.id == nextId }?.let { playTrack(it) }
    }

    private fun playNextInternal(auto: Boolean) {
        val list = queueTracks()
        if (list.isEmpty()) return

        if (!auto && !session.canControl) {
            _message.value = MessageRes(R.string.sync_guest_locked)
            return
        }

        val nextId = PlaybackQueue.next(
            tracks = list.map { it.id },
            currentId = _currentTrackId.value,
            repeatOne = _repeatOne.value,
            shuffle = _shuffle.value
        )

        list.firstOrNull { it.id == nextId }?.let { playTrack(it) }
    }

    /**
     * Начинает новое прослушивание: закрывает предыдущую запись истории
     * с фактической длительностью и запоминает время старта.
     */
    private fun recordListenStart(track: Track) {
        closeListenRecord()

        listenStartedAtMs = System.currentTimeMillis()

        viewModelScope.launch {
            app.historyStore.add(
                HistoryEntry(
                    trackId = track.id,
                    trackTitle = track.title,
                    trackArtist = track.artist,
                    startedAtMs = listenStartedAtMs,
                    listenedMs = 0L,
                    roomCode = session.roomCode.value
                )
            )
        }
    }

    /** Фиксирует накопленное время прослушивания при паузе или уходе */
    private fun closeListenRecord() {
        val started = listenStartedAtMs
        if (started <= 0L) return

        val listened = System.currentTimeMillis() - started
        listenStartedAtMs = 0L

        if (listened < 1_000L) return

        val track = currentTrack() ?: return

        viewModelScope.launch {
            app.historyStore.add(
                HistoryEntry(
                    trackId = track.id,
                    trackTitle = track.title,
                    trackArtist = track.artist,
                    startedAtMs = started,
                    listenedMs = listened,
                    roomCode = session.roomCode.value
                )
            )
        }
    }

    /** Роль пользователя в комнате для подписей интерфейса */
    fun currentRole(): RoomRole = session.role.value

    override fun onCleared() {
        closeListenRecord()
        syncJob?.cancel()
        heartbeatJob?.cancel()
        playback.release()
        super.onCleared()
    }
}
