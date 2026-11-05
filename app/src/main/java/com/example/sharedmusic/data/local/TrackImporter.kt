package com.example.sharedmusic.data.local

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.sharedmusic.data.model.Track
import com.example.sharedmusic.domain.TrackIdentity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Импорт музыки из системного пикера.
 *
 * Файл копируется в filesDir/library, метаданные читаются из тегов,
 * обложка сохраняется отдельно. Исходный URI не сохраняется:
 * разрешение на него может быть отозвано, а данные из WhatsApp/Telegram
 * живут в чужих кэшах.
 */
class TrackImporter(
    private val context: Context,
    private val store: TrackStore
) {

    /** Результат импорта одного файла */
    sealed interface ImportResult {
        data class Success(val track: Track) : ImportResult
        data class Failure(val fileName: String, val reason: String) : ImportResult
        data object Duplicate : ImportResult
    }

    /** Импортирует набор файлов, возвращая результат по каждому */
    suspend fun import(uris: List<Uri>): List<ImportResult> = withContext(Dispatchers.IO) {
        uris.map { uri -> importOne(uri) }
    }

    private suspend fun importOne(uri: Uri): ImportResult {
        val displayName = queryDisplayName(uri)

        return try {
            val extension = guessExtension(displayName, uri)
            val destination = AppFiles.newAudioFile(context, extension)

            copyToFile(uri, destination)

            if (destination.length() == 0L) {
                destination.delete()
                return ImportResult.Failure(displayName, "Файл пуст")
            }

            val metadata = AudioMetadataReader.read(
                context = context,
                file = destination,
                fallbackTitle = displayName.substringBeforeLast('.').ifBlank { "Трек" }
            )

            val trackId = TrackIdentity.weakId(
                displayName = displayName,
                sizeBytes = destination.length(),
                durationMs = metadata.durationMs
            )

            val existing = store.findById(trackId)
            if (existing != null) {
                destination.delete()
                return ImportResult.Duplicate
            }

            val track = Track(
                id = trackId,
                title = metadata.title,
                artist = metadata.artist,
                fileName = displayName,
                filePath = destination.absolutePath,
                durationMs = metadata.durationMs,
                sizeBytes = destination.length(),
                artworkPath = metadata.artworkPath,
                addedAt = System.currentTimeMillis(),
                isWeakId = true
            )

            store.insert(track)

            ImportResult.Success(track)
        } catch (error: Exception) {
            ImportResult.Failure(displayName, error.message ?: "Неизвестная ошибка")
        }
    }

    /** Импортирует уже скачанный файл (трек, полученный из комнаты) */
    suspend fun importDownloadedFile(
        file: File,
        trackId: String,
        title: String,
        artist: String,
        durationMs: Long
    ): Track? = withContext(Dispatchers.IO) {
        try {
            if (!file.exists() || file.length() == 0L) return@withContext null

            val destination = AppFiles.newAudioFile(context, file.extension)

            file.copyTo(destination, overwrite = true)

            val metadata = AudioMetadataReader.read(
                context = context,
                file = destination,
                fallbackTitle = title.ifBlank { file.nameWithoutExtension }
            )

            val track = Track(
                id = trackId,
                title = metadata.title.ifBlank { title },
                artist = if (metadata.artist.isBlank()) artist else metadata.artist,
                fileName = file.name,
                filePath = destination.absolutePath,
                durationMs = if (durationMs > 0L) durationMs else metadata.durationMs,
                sizeBytes = destination.length(),
                artworkPath = metadata.artworkPath,
                addedAt = System.currentTimeMillis(),
                isWeakId = false
            )

            store.insert(track)

            track
        } catch (_: Exception) {
            null
        }
    }

    private fun copyToFile(uri: Uri, destination: File) {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Нет доступа к файлу")

        input.use { source ->
            destination.outputStream().use { output ->
                source.copyTo(output)
            }
        }
    }

    private fun queryDisplayName(uri: Uri): String {
        val cursor = context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    it.getString(index)?.takeIf { name -> name.isNotBlank() }?.let { name ->
                        return name
                    }
                }
            }
        }

        return uri.lastPathSegment?.substringAfterLast('/') ?: "track.audio"
    }

    private fun guessExtension(name: String, uri: Uri): String {
        val fromName = name.substringAfterLast('.', "")
            .lowercase()
            .takeIf { it.length in 1..4 && it.all { char -> char.isLetterOrDigit() } }

        if (fromName != null) return fromName

        val mime = runCatching { context.contentResolver.getType(uri) }.getOrNull()

        return when (mime) {
            "audio/mpeg" -> "mp3"
            "audio/mp4", "audio/aac", "audio/x-m4a" -> "m4a"
            "audio/flac" -> "flac"
            "audio/ogg", "audio/opus", "application/ogg" -> "ogg"
            "audio/wav", "audio/x-wav" -> "wav"
            else -> "audio"
        }
    }
}
