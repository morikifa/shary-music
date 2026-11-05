package com.example.sharedmusic.domain

import kotlin.random.Random

/**
 * Код комнаты: 6 символов без легко путаемых 0/O/1/I.
 */
object RoomCode {

    const val LENGTH = 6

    /** Алфавит без 0, O, 1, I — их путают при диктовке */
    const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    /** Приводит ввод пользователя к каноническому виду */
    fun normalize(input: String): String =
        input.uppercase()
            .filter { it in ALPHABET }
            .take(LENGTH)

    /** Проверяет, что код полный и состоит только из допустимых символов */
    fun isValid(code: String): Boolean =
        code.length == LENGTH && code.all { it in ALPHABET }

    /** Генерирует случайный код; random передаётся для воспроизводимых тестов */
    fun generate(random: Random = Random.Default): String =
        buildString(LENGTH) {
            repeat(LENGTH) {
                append(ALPHABET[random.nextInt(ALPHABET.length)])
            }
        }
}
