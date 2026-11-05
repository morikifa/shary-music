package com.example.sharedmusic.viewmodel

import androidx.annotation.StringRes

/**
 * Сообщение пользователю в виде ссылки на строковый ресурс.
 *
 * ViewModel не работает с Context, поэтому сам текст собирает UI:
 * так строки живут в strings.xml, а логика остаётся переводимой.
 *
 * @param id идентификатор строкового ресурса
 * @param args аргументы форматирования в порядке вхождения в строку
 */
data class MessageRes(
    @StringRes val id: Int,
    val args: List<Any> = emptyList()
)
