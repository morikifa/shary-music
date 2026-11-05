package com.example.sharedmusic.domain

/**
 * Разбор технических сообщений Firebase на понятные категории.
 *
 * Раньше любая неудача записи в Realtime Database превращалась в одно
 * сообщение «Нет соединения с сервером», хотя причин три: нет сети, неверный
 * адрес базы и запрет правил. Здесь чистые правила сравнения, поэтому их
 * можно проверить обычными unit-тестами без Android.
 */
object RemoteErrorClassifier {

    /** Запрет правил безопасности базы: лечится в консоли Firebase */
    fun isPermissionDenied(message: String, isDatabaseException: Boolean): Boolean {
        if (isDatabaseException) return true

        return message.contains("permission denied", ignoreCase = true) ||
                message.contains("permission_denied", ignoreCase = true)
    }

    /** Сети нет: запрос вообще не дошёл до сервера */
    fun isOffline(message: String): Boolean =
        message.contains("network", ignoreCase = true) ||
                message.contains("offline", ignoreCase = true) ||
                message.contains("unable to resolve host", ignoreCase = true) ||
                message.contains("failed to connect", ignoreCase = true) ||
                message.contains("timeout", ignoreCase = true)

    /**
     * Короткая подпись причины для экрана.
     *
     * Сообщение Firebase приходит по-английски и бывает длинным; для строки
     * под основной ошибкой достаточно первых осмысленных слов.
     */
    fun shortReason(message: String, limit: Int = 160): String {
        val normalized = message.replace('\n', ' ').trim()
        return if (normalized.length <= limit) normalized else normalized.take(limit - 1) + "…"
    }
}
