package com.example.sharedmusic.domain

/** Форматирование времени для подписей плеера и истории */
object TimeFormat {

    /** 0:00 — минуты:секунды, часы добавляются только при необходимости */
    fun clock(milliseconds: Long): String {
        val totalSeconds = milliseconds.coerceAtLeast(0L) / 1_000L
        val hours = totalSeconds / 3_600L
        val minutes = (totalSeconds % 3_600L) / 60L
        val seconds = totalSeconds % 60L

        return if (hours > 0L) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%d:%02d".format(minutes, seconds)
        }
    }

    /** «12 мин» для статистики профиля */
    fun shortDuration(milliseconds: Long): String {
        val totalMinutes = milliseconds.coerceAtLeast(0L) / 60_000L

        return when {
            totalMinutes < 1L -> "меньше минуты"
            totalMinutes < 60L -> "$totalMinutes мин"
            else -> {
                val hours = totalMinutes / 60L
                val minutes = totalMinutes % 60L
                if (minutes == 0L) "$hours ч" else "$hours ч $minutes мин"
            }
        }
    }

    /** Часы прослушивания в комнатах для профиля, с одним знаком после запятой */
    fun hours(totalMillis: Long): String {
        val hours = totalMillis.toDouble() / 3_600_000.0

        return "%.1f".format(hours)
    }
}
