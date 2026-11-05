package com.example.sharedmusic.domain

import kotlin.math.abs

/**
 * Генератор розового градиента-заглушки для треков без обложки.
 *
 * Градиент детерминирован: один и тот же трек всегда получает один и тот же
 * оттенок, поэтому лента обложек не «мигает» при перерисовке.
 */
object ArtworkGenerator {

    /** Базовый акцентный цвет интерфейса */
    const val ACCENT_RED = 0xFD
    const val ACCENT_GREEN = 0x0C
    const val ACCENT_BLUE = 0x7D

    /**
     * Возвращает пару цветов градиента для трека.
     * Первый цвет — исходный акцент, второй — слегка сдвинутый по яркости и тону.
     */
    fun gradientFor(seed: String): Pair<Int, Int> {
        val hash = seed.hashCode()

        // Сдвигаем тон в пределах «розовой» зоны, чтобы интерфейс оставался единым
        val hueShift = (hash % 40) - 20
        val lightShift = ((abs(hash) / 40) % 35) - 15

        val start = toColor(
            red = (ACCENT_RED + lightShift).coerceIn(180, 255),
            green = (ACCENT_GREEN + hueShift / 2).coerceIn(0, 120),
            blue = (ACCENT_BLUE + hueShift).coerceIn(60, 160)
        )

        val end = toColor(
            red = (ACCENT_RED - 60 + lightShift).coerceIn(120, 220),
            green = (ACCENT_GREEN + hueShift).coerceIn(0, 90),
            blue = (ACCENT_BLUE - 10 + hueShift).coerceIn(40, 140)
        )

        return start to end
    }

    private fun toColor(red: Int, green: Int, blue: Int): Int =
        (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
}
