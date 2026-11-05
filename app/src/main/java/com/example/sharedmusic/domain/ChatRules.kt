package com.example.sharedmusic.domain

import com.example.sharedmusic.core.SharyConfig

/** Результат проверки текста сообщения чата */
sealed interface ChatValidation {
    data class Valid(val text: String) : ChatValidation
    data object Empty : ChatValidation
    data class TooLong(val maxLength: Int) : ChatValidation
}

/**
 * Правила сообщения чата: обрезка пробелов, запрет управляющих символов,
 * ограничение длины. Переводы строк разрешены.
 */
object ChatRules {

    fun sanitize(raw: String): String =
        raw
            .replace("\r\n", "\n")
            .filter { char ->
                char == '\n' || (!char.isISOControl() && char != '\u0000')
            }
            .trim()

    fun validate(raw: String): ChatValidation {
        val text = sanitize(raw)

        return when {
            text.isEmpty() -> ChatValidation.Empty
            text.length > SharyConfig.MAX_CHAT_LENGTH ->
                ChatValidation.TooLong(SharyConfig.MAX_CHAT_LENGTH)
            else -> ChatValidation.Valid(text)
        }
    }
}
