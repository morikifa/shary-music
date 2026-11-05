package com.example.sharedmusic.domain

/**
 * Геометрия виниловой пластинки.
 *
 * Пользователь может перематывать трек вращением пластинки.
 * Полный оборот вокруг центра переводит длительность трека в градусы,
 * чтобы перемотка была предсказуемой для любого трека.
 */
object VinylGeometry {

    /** Сколько полных оборотов пальца соответствует всей длине трека */
    const val TURNS_PER_TRACK = 3f

    private const val DEGREES_IN_CIRCLE = 360f

    /** Сколько градусов соответствует всей длительности трека */
    fun degreesPerTrack(): Float = DEGREES_IN_CIRCLE * TURNS_PER_TRACK

    /** Переводит позицию в угол поворота пластинки в градусах */
    fun positionToDegrees(positionMs: Long, durationMs: Long): Float {
        if (durationMs <= 0L) return 0f

        val fraction = (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)

        return fraction * degreesPerTrack()
    }

    /**
     * Переводит угол поворота обратно в позицию.
     * Допускается перемотка только вперёд: новый угол не меньше текущего.
     */
    fun degreesToPosition(degrees: Float, durationMs: Long): Long {
        if (durationMs <= 0L) return 0L

        val clamped = degrees.coerceIn(0f, degreesPerTrack())

        return (clamped / degreesPerTrack() * durationMs.toFloat()).toLong()
    }

    /** Скорость вращения пластинки: градусов в секунду при воспроизведении */
    fun rotationDegreesPerSecond(): Float = DEGREES_IN_CIRCLE / 3f
}
