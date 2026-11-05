package com.example.sharedmusic.data.player

import android.media.audiofx.Equalizer
import com.example.sharedmusic.data.model.UserSettings

/**
 * Эквалайзер поверх аудиосессии ExoPlayer.
 *
 * Четыре полосы интерфейса раскладываются по реальным полосам устройства:
 * низкие, средние, высокие и голос (центр ~1 кГц).
 * Если устройство не поддерживает эквалайзер, все методы безопасно ничего не делают.
 */
class EqualizerController {

    private var equalizer: Equalizer? = null
    private var sessionId: Int = -1

    /** Подключает эквалайзер к аудиосессии плеера */
    fun attach(sessionId: Int) {
        if (this.sessionId == sessionId && equalizer != null) return

        release()
        this.sessionId = sessionId

        equalizer = runCatching { Equalizer(0, sessionId) }.getOrNull()
    }

    /** Применяет состояние эквалайзера из настроек */
    fun apply(settings: UserSettings) {
        val effect = equalizer ?: return

        runCatching {
            effect.enabled = settings.equalizerEnabled
            if (!settings.equalizerEnabled) return@runCatching

            val range = effect.bandLevelRange
            val minLevel = range[0]
            val maxLevel = range[1]

            for (band in 0 until effect.numberOfBands) {
                val centerFreq = effect.getCenterFreq(band.toShort())
                val target = targetBand(centerFreq, settings.equalizerBands)
                val level = (target * 100f).toInt().coerceIn(
                    minLevel.toInt(),
                    maxLevel.toInt()
                )
                effect.setBandLevel(band.toShort(), level.toShort())
            }
        }
    }

    /** Раскладывает частоту полосы устройства по четырём полосам интерфейса */
    private fun targetBand(centerFreqMilliHz: Int, bands: List<Float>): Float {
        val frequency = centerFreqMilliHz / 1000

        val index = when {
            frequency < 250 -> 0
            frequency < 1500 -> 3
            frequency < 4000 -> 1
            else -> 2
        }

        return bands.getOrElse(index) { 0f }.coerceIn(-12f, 12f) / 12f
    }

    fun release() {
        runCatching {
            equalizer?.enabled = false
            equalizer?.release()
        }
        equalizer = null
        sessionId = -1
    }
}
