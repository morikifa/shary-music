package com.example.sharedmusic.domain

/**
 * Длительность прослушивания, разложенная на части.
 *
 * Домен не знает слов «мин» и «ч»: это текст интерфейса, он живёт в
 * strings.xml. Экран выбирает подходящую строку по варианту и подставляет
 * числа — так история переводится вместе с остальным приложением.
 */
sealed interface ListenDuration {

    /** Меньше минуты прослушивания */
    data object LessThanMinute : ListenDuration

    /** От минуты до часа */
    data class Minutes(val minutes: Long) : ListenDuration

    /** Час и больше */
    data class Hours(val hours: Long, val minutes: Long) : ListenDuration

    companion object {

        /** Раскладывает миллисекунды на части, отбрасывая неполные минуты */
        fun of(milliseconds: Long): ListenDuration {
            val totalMinutes = milliseconds.coerceAtLeast(0L) / 60_000L

            return when {
                totalMinutes < 1L -> LessThanMinute
                totalMinutes < 60L -> Minutes(totalMinutes)
                else -> Hours(
                    hours = totalMinutes / 60L,
                    minutes = totalMinutes % 60L
                )
            }
        }
    }
}
