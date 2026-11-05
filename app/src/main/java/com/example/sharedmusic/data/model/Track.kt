package com.example.sharedmusic.data.model

/**
 * Локальный трек пользователя.
 *
 * @param id детерминированный идентификатор (см. domain/TrackIdentity)
 * @param filePath полный путь к аудиофайлу в filesDir/library
 * @param artworkPath путь к сохранённой обложке или null
 * @param isWeakId true, если id вычислен по метаданным, а не по содержимому файла
 */
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val fileName: String,
    val filePath: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val artworkPath: String? = null,
    val isLiked: Boolean = false,
    val addedAt: Long = 0L,
    val isWeakId: Boolean = false
)
