package com.example.sharedmusic.data.player

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * Обёртка над ExoPlayer.
 *
 * Экземпляр один на всё приложение: он создаётся в PlayerContainer,
 * поэтому воспроизведение не прерывается при переходах между экранами.
 *
 * ExoPlayer строго проверяет поток: любое обращение к нему должно идти
 * с того потока, где объект создан. Здесь это главный поток, поэтому
 * сам плеер и все его методы завернуты в `callOnPlayerThread`.
 */
class MusicPlayer(context: Context) {

    /** События плеера для слоя ViewModel */
    interface Listener {
        fun onPlayingChanged(isPlaying: Boolean)
        fun onPositionChanged(positionMs: Long, durationMs: Long)
        fun onTrackCompleted()
        fun onPlayerError(message: String)
        fun onMediaChanged(mediaId: String?)
    }

    private var listener: Listener? = null

    /** Поток, на котором создан плеер: только с него к нему можно обращаться */
    private val playerLooper: Looper = Looper.getMainLooper()

    private val playerHandler = Handler(playerLooper)

    private val player: ExoPlayer = ExoPlayer.Builder(context.applicationContext)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            true
        )
        .setHandleAudioBecomingNoisy(true)
        .build()

    /** Усиление звука эквалайзером, привязывается к аудиосессии плеера */
    val audioSessionId: Int
        get() = readOnPlayerThread { player.audioSessionId } ?: 0

    init {
        require(Looper.myLooper() == playerLooper) {
            "MusicPlayer must be created on the main thread"
        }

        player.addListener(object : Player.Listener {

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                listener?.onPlayingChanged(isPlaying)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    listener?.onTrackCompleted()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                // Текст для пользователя формирует ViewModel; здесь только причина
                listener?.onPlayerError(error.message ?: "playback_error")
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                listener?.onMediaChanged(mediaItem?.mediaId)
            }
        })
    }

    /**
     * Выполняет блок на потоке плеера.
     *
     * С главного потока блок исполняется сразу и результат возвращается
     * вызывающему. С фоновой корутины (загрузка из комнаты, например)
     * команда встаёт в очередь главного потока — плеер не падает с
     * «Player is accessed on the wrong thread».
     */
    private fun callOnPlayerThread(block: () -> Unit) {
        if (Looper.myLooper() == playerLooper) {
            block()
        } else {
            playerHandler.post(block)
        }
    }

    /**
     * Читает значение с потока плеера.
     *
     * ExoPlayer не разрешает читать позицию и длительность из другого потока,
     * поэтому с чужого потока возвращается null, а не исключение: интерфейс
     * не должен падать из-за того, что опрос попал не на тот поток.
     */
    private fun <T> readOnPlayerThread(block: () -> T): T? {
        if (Looper.myLooper() != playerLooper) return null
        return block()
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    val currentPositionMs: Long
        get() = readOnPlayerThread { player.currentPosition.coerceAtLeast(0L) } ?: 0L

    val durationMs: Long
        get() = readOnPlayerThread {
            val duration = player.duration
            if (duration == C.TIME_UNSET || duration < 0L) 0L else duration
        } ?: 0L

    val isPlaying: Boolean
        get() = readOnPlayerThread { player.isPlaying } ?: false

    val currentMediaId: String?
        get() = readOnPlayerThread { player.currentMediaItem?.mediaId }

    /** Загружает трек и запускает воспроизведение с указанной позиции */
    fun play(
        trackId: String,
        uri: Uri,
        title: String,
        artist: String,
        startPositionMs: Long = 0L
    ) = callOnPlayerThread {
        if (player.currentMediaItem?.mediaId != trackId) {
            player.setMediaItem(buildItem(trackId, uri, title, artist))
            player.prepare()
        }

        player.seekTo(startPositionMs.coerceAtLeast(0L))
        player.play()
    }

    /** Загружает трек в состоянии паузы */
    fun preparePaused(
        trackId: String,
        uri: Uri,
        title: String,
        artist: String,
        positionMs: Long = 0L
    ) = callOnPlayerThread {
        if (player.currentMediaItem?.mediaId != trackId) {
            player.setMediaItem(buildItem(trackId, uri, title, artist))
            player.prepare()
        }

        player.pause()
        player.seekTo(positionMs.coerceAtLeast(0L))
    }

    fun pause() = callOnPlayerThread {
        player.pause()
    }

    fun resume() = callOnPlayerThread {
        if (player.mediaItemCount == 0) return@callOnPlayerThread

        if (player.playbackState == Player.STATE_ENDED) {
            player.seekTo(0L)
        }

        player.play()
    }

    fun seekTo(positionMs: Long) {
        // Позиция для UI снимается сразу после команды, поэтому сообщаем её
        // изнутри того же блока: чтение с чужого потока вернуло бы null
        callOnPlayerThread {
            player.seekTo(positionMs.coerceAtLeast(0L))
            listener?.onPositionChanged(
                player.currentPosition.coerceAtLeast(0L),
                player.duration.let { if (it == C.TIME_UNSET || it < 0L) 0L else it }
            )
        }
    }

    fun setVolume(volume: Float) = callOnPlayerThread {
        player.volume = volume.coerceIn(0f, 1f)
    }

    fun stop() = callOnPlayerThread {
        player.pause()
        player.seekTo(0L)
    }

    fun release() = callOnPlayerThread {
        player.release()
    }

    private fun buildItem(
        trackId: String,
        uri: Uri,
        title: String,
        artist: String
    ): MediaItem = MediaItem.Builder()
        .setMediaId(trackId)
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .build()
        )
        .build()
}
