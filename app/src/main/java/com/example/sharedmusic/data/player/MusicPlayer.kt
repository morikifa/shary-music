package com.example.sharedmusic.data.player

import android.content.Context
import android.net.Uri
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

    val player: ExoPlayer = ExoPlayer.Builder(context.applicationContext)
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
        get() = player.audioSessionId

    init {
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

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    val currentPositionMs: Long
        get() = player.currentPosition.coerceAtLeast(0L)

    val durationMs: Long
        get() {
            val duration = player.duration
            return if (duration == C.TIME_UNSET || duration < 0L) 0L else duration
        }

    val isPlaying: Boolean
        get() = player.isPlaying

    val currentMediaId: String?
        get() = player.currentMediaItem?.mediaId

    /** Загружает трек и запускает воспроизведение с указанной позиции */
    fun play(
        trackId: String,
        uri: Uri,
        title: String,
        artist: String,
        startPositionMs: Long = 0L
    ) {
        if (currentMediaId != trackId) {
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
    ) {
        if (currentMediaId != trackId) {
            player.setMediaItem(buildItem(trackId, uri, title, artist))
            player.prepare()
        }

        player.pause()
        player.seekTo(positionMs.coerceAtLeast(0L))
    }

    fun pause() {
        player.pause()
    }

    fun resume() {
        if (player.mediaItemCount == 0) return

        if (player.playbackState == Player.STATE_ENDED) {
            player.seekTo(0L)
        }

        player.play()
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceAtLeast(0L))
        listener?.onPositionChanged(currentPositionMs, durationMs)
    }

    fun setVolume(volume: Float) {
        player.volume = volume.coerceIn(0f, 1f)
    }

    fun stop() {
        player.pause()
        player.seekTo(0L)
    }

    fun release() {
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
