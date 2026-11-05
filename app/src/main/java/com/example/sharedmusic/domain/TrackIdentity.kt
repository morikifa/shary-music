package com.example.sharedmusic.domain

import java.security.MessageDigest
import java.util.Locale

/**
 * Идентичность локального трека без отправки аудио на сервер.
 *
 * Предпочтительный вариант — читать байты файла, но для больших файлов это дорого,
 * поэтому используется слабый идентификатор: SHA-256 от имени, размера и длительности.
 * Совпадение возможно у разных файлов с одинаковыми метаданными, поэтому
 * результат помечается флагом isWeak.
 */
object TrackIdentity {

    fun weakId(
        displayName: String,
        sizeBytes: Long,
        durationMs: Long
    ): String {
        val source = buildString {
            append(displayName.lowercase(Locale.ROOT).trim())
            append('|')
            append(sizeBytes)
            append('|')
            append(durationMs)
        }

        return sha256Hex(source)
    }

    fun sha256Hex(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(value.toByteArray(Charsets.UTF_8))

        return bytes.joinToString("") { byte ->
            "%02x".format(byte)
        }
    }
}
