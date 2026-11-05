package com.example.sharedmusic.viewmodel

import com.example.sharedmusic.data.model.RoomPlayback
import com.example.sharedmusic.data.player.MusicPlayer
import com.example.sharedmusic.domain.SyncPolicy
import com.example.sharedmusic.domain.SyncDecision
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Состояние воспроизведения, общее для всех экранов.
 *
 * Здесь живут позиция, длительность и флаг воспроизведения,
 * а также подписка на состояние комнаты. Хост публикует своё состояние,
 * гость подстраивается по порогам SyncPolicy.
 */
class PlaybackController(
    private val player: MusicPlayer,
    private val scope: CoroutineScope
) {

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _currentTrackId = MutableStateFlow<String?>(null)
    val currentTrackId: StateFlow<String?> = _currentTrackId.asStateFlow()

    /** Слушатель завершения трека: следующая очередь выбирается во ViewModel */
    var onTrackCompleted: (() -> Unit)? = null

    /** Ошибка плеера: сообщение формируется кодом ресурса в PlayerViewModel */
    var onError: (() -> Unit)? = null

    /** Публикация состояния хоста в комнату */
    var onPublishState: ((trackId: String, positionMs: Long, isPlaying: Boolean) -> Unit)? = null

    private var tickerJob: Job? = null

    init {
        player.setListener(object : MusicPlayer.Listener {
            override fun onPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                publishIfNeeded()
            }

            override fun onPositionChanged(positionMs: Long, durationMs: Long) {
                _positionMs.value = positionMs
                if (durationMs > 0L) _durationMs.value = durationMs
            }

            override fun onTrackCompleted() {
                _isPlaying.value = false
                onTrackCompleted?.invoke()
            }

            override fun onPlayerError(message: String) {
                _isPlaying.value = false
                onError?.invoke()
            }

            override fun onMediaChanged(mediaId: String?) {
                _currentTrackId.value = mediaId
            }
        })

        startTicker()
    }

    /** Запускает воспроизведение трека */
    fun play(
        trackId: String,
        filePath: String,
        title: String,
        artist: String,
        startPositionMs: Long = 0L
    ) {
        player.play(
            trackId = trackId,
            uri = android.net.Uri.fromFile(java.io.File(filePath)),
            title = title,
            artist = artist,
            startPositionMs = startPositionMs
        )
        _currentTrackId.value = trackId
        _positionMs.value = startPositionMs
    }

    /** Переключает паузу и воспроизведение */
    fun toggle() {
        if (_isPlaying.value) {
            player.pause()
            _isPlaying.value = false
        } else {
            player.resume()
            _isPlaying.value = player.isPlaying
        }

        publishIfNeeded()
    }

    /** Перематывает на позицию */
    fun seekTo(positionMs: Long) {
        val target = if (_durationMs.value > 0L) {
            positionMs.coerceIn(0L, _durationMs.value)
        } else {
            positionMs.coerceAtLeast(0L)
        }

        player.seekTo(target)
        _positionMs.value = target
        publishIfNeeded()
    }

    /** Останавливает воспроизведение */
    fun stop() {
        player.stop()
        _isPlaying.value = false
        _positionMs.value = 0L
        publishIfNeeded()
    }

    /**
     * Применяет состояние комнаты к плееру.
     * @return true, если трек найден и состояние применено
     */
    fun applyRemoteState(
        state: RoomPlayback,
        currentServerMs: Long,
        localFilePath: String?,
        localTitle: String,
        localArtist: String
    ): SyncDecision? {
        if (state.trackId.isBlank()) return null

        val expected = SyncPolicy.expectedPositionMs(
            positionMs = state.positionMs,
            isPlaying = state.isPlaying,
            updatedAtServerMs = state.updatedAtServerMs,
            currentServerMs = currentServerMs
        )

        // Трека нет на устройстве — воспроизведение невозможно, но комната остаётся
        if (localFilePath == null) return null

        val isSameTrack = _currentTrackId.value == state.trackId

        if (!isSameTrack) {
            play(
                trackId = state.trackId,
                filePath = localFilePath,
                title = localTitle,
                artist = localArtist,
                startPositionMs = expected
            )

            if (!state.isPlaying) {
                player.pause()
                _isPlaying.value = false
            }

            return SyncDecision.HARD
        }

        if (!state.isPlaying) {
            if (_isPlaying.value) {
                player.pause()
                _isPlaying.value = false
            }

            val drift = SyncPolicy.driftMs(_positionMs.value, expected)
            val decision = SyncPolicy.decide(drift)

            if (decision != SyncDecision.IGNORE) {
                seekTo(expected)
            }

            return decision
        }

        if (!_isPlaying.value) {
            player.resume()
            _isPlaying.value = player.isPlaying
        }

        val drift = SyncPolicy.driftMs(_positionMs.value, expected)

        return when (SyncPolicy.decide(drift)) {
            SyncDecision.IGNORE -> SyncDecision.IGNORE
            SyncDecision.SOFT -> {
                // Мягкая коррекция: подтягиваем позицию небольшим шагом,
                // чтобы не было слышимого рывка
                val step = if (_positionMs.value < expected) {
                    SyncPolicy.SOFT_CORRECTION_STEP_MS
                } else {
                    -SyncPolicy.SOFT_CORRECTION_STEP_MS
                }
                player.seekTo((_positionMs.value + step).coerceAtLeast(0L))
                SyncDecision.SOFT
            }
            SyncDecision.HARD -> {
                seekTo(expected)
                SyncDecision.HARD
            }
        }
    }

    /** Останавливает публикацию состояния, когда комната неактивна */
    private fun publishIfNeeded() {
        val trackId = _currentTrackId.value ?: return
        onPublishState?.invoke(trackId, _positionMs.value, _isPlaying.value)
    }

    private fun startTicker() {
        tickerJob?.cancel()

        tickerJob = scope.launch {
            while (isActive) {
                _positionMs.value = player.currentPositionMs

                val duration = player.durationMs
                if (duration > 0L) {
                    _durationMs.value = duration
                }

                delay(com.example.sharedmusic.core.SharyConfig.PLAYBACK_TICK_MS)
            }
        }
    }

    /** Освобождает ресурсы при закрытии процесса */
    fun release() {
        tickerJob?.cancel()
        player.setListener(null)
        onTrackCompleted = null
        onPublishState = null
    }
}
