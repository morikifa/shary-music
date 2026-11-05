package com.example.sharedmusic.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Разбор ошибок Firebase.
 *
 * Проверяем ровно тот случай, из-за которого создание комнаты показывало
 * «Нет соединения с сервером» при запрете правил: отказ правил и отсутствие
 * сети — разные причины и разные сообщения для пользователя.
 */
class RemoteErrorClassifierTest {

    /** Правила базы: DatabaseException всегда означает запрет, а не сеть */
    @Test
    fun databaseExceptionIsPermissionDenied() {
        assertTrue(
            RemoteErrorClassifier.isPermissionDenied("whatever", isDatabaseException = true)
        )
    }

    /** Текст отказа правил распознаётся независимо от регистра */
    @Test
    fun permissionTextIsRecognized() {
        assertTrue(
            RemoteErrorClassifier.isPermissionDenied("Permission denied", false)
        )
        assertTrue(
            RemoteErrorClassifier.isPermissionDenied("PERMISSION_DENIED: no rule", false)
        )
        assertFalse(
            RemoteErrorClassifier.isPermissionDenied("Client is offline", false)
        )
    }

    /** Отсутствие сети — отдельная категория */
    @Test
    fun offlineIsSeparateFromDenied() {
        assertTrue(RemoteErrorClassifier.isOffline("Client is offline"))
        assertTrue(RemoteErrorClassifier.isOffline("Unable to resolve host: no address"))
        assertFalse(RemoteErrorClassifier.isOffline("Permission denied"))
    }

    /** Запрет правил не попадает в категорию «нет сети» */
    @Test
    fun permissionDeniedIsNotTreatedAsOffline() {
        assertFalse(RemoteErrorClassifier.isOffline("Permission denied"))
    }

    /** Длинный текст обрезается, короткий не меняется */
    @Test
    fun shortReasonLimitsLength() {
        val long = "x".repeat(300)
        val result = RemoteErrorClassifier.shortReason(long)

        assertTrue(result.length <= 160)
        assertTrue(result.endsWith("…"))
        assertEquals("short", RemoteErrorClassifier.shortReason("short"))
        assertEquals("a b", RemoteErrorClassifier.shortReason("a\nb"))
    }
}
