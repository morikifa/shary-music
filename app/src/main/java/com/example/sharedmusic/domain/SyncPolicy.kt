package com.example.sharedmusic.domain

/**
 * Пороги рассинхронизации комнаты.
 * Значения — начальные настройки для проверки, а не обещание точности.
 */
object SyncPolicy {

    /** Дрейф меньше этого значения не корректируется */
    const val TOLERATED_DRIFT_MS = 150L

    /** До этого значения делается мягкая коррекция: подстройка позиции без слышимого рывка */
    const val SOFT_CORRECTION_LIMIT_MS = 800L

    /** Мягкая коррекция выполняется шагом не более этого значения */
    const val SOFT_CORRECTION_STEP_MS = 120L

    /**
     * Ожидаемая позиция трека на сервере.
     * Если воспроизведение на паузе, позиция не растёт.
     */
    fun expectedPositionMs(
        positionMs: Long,
        isPlaying: Boolean,
        updatedAtServerMs: Long,
        currentServerMs: Long
    ): Long {
        if (!isPlaying || updatedAtServerMs <= 0L) return positionMs.coerceAtLeast(0L)

        val elapsed = (currentServerMs - updatedAtServerMs).coerceAtLeast(0L)

        return (positionMs + elapsed).coerceAtLeast(0L)
    }

    /** Возвращает абсолютную разницу между локальной и ожидаемой позицией */
    fun driftMs(localPositionMs: Long, expectedPositionMs: Long): Long =
        kotlin.math.abs(localPositionMs - expectedPositionMs)

    /** Решение по коррекции на основе дрейфа */
    fun decide(driftMs: Long): SyncDecision = when {
        driftMs < TOLERATED_DRIFT_MS -> SyncDecision.IGNORE
        driftMs < SOFT_CORRECTION_LIMIT_MS -> SyncDecision.SOFT
        else -> SyncDecision.HARD
    }
}

/** Что делать при обнаруженном дрейфе */
enum class SyncDecision {
    /** Дрейф в пределах нормы — ничего не делаем */
    IGNORE,

    /** Плавно подтягиваем позицию небольшим шагом */
    SOFT,

    /** Перескакиваем сразу к ожидаемой позиции */
    HARD
}
