package com.example.sharedmusic.data.local

import com.example.sharedmusic.data.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

/**
 * Хранилище локальной библиотеки поверх Room.
 * UI получает готовые модели Track, а не сущности базы.
 */
class TrackStore(private val database: SharyDatabase) {

    fun observeTracks(): Flow<List<Track>> =
        database.trackDao().observeAll().map { entities -> entities.map { it.toTrack() } }

    fun observeLikedTracks(): Flow<List<Track>> =
        database.trackDao().observeLiked().map { entities -> entities.map { it.toTrack() } }

    suspend fun getAll(): List<Track> =
        database.trackDao().getAll().map { it.toTrack() }

    suspend fun findById(id: String): Track? =
        database.trackDao().findById(id)?.toTrack()

    suspend fun insert(track: Track) {
        database.trackDao().insert(track.toEntity())
    }

    suspend fun setLiked(id: String, liked: Boolean) {
        database.trackDao().setLiked(id, liked)
    }

    suspend fun setArtwork(id: String, artworkPath: String?) {
        database.trackDao().setArtwork(id, artworkPath)
    }

    /** Удаляет запись из базы и сам файл с устройства */
    suspend fun delete(track: Track) {
        File(track.filePath).delete()
        track.artworkPath?.let { path -> File(path).delete() }
        database.trackDao().deleteById(track.id)
    }

    suspend fun count(): Int = database.trackDao().count()
}

private fun TrackEntity.toTrack(): Track = Track(
    id = id,
    title = title,
    artist = artist,
    fileName = fileName,
    filePath = filePath,
    durationMs = durationMs,
    sizeBytes = sizeBytes,
    artworkPath = artworkPath,
    isLiked = isLiked,
    addedAt = addedAt,
    isWeakId = isWeakId
)

private fun Track.toEntity(): TrackEntity = TrackEntity(
    id = id,
    title = title,
    artist = artist,
    fileName = fileName,
    filePath = filePath,
    durationMs = durationMs,
    sizeBytes = sizeBytes,
    artworkPath = artworkPath,
    isLiked = isLiked,
    addedAt = addedAt,
    isWeakId = isWeakId
)
