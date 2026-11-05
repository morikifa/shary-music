package com.example.sharedmusic.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import com.example.sharedmusic.core.SharyConfig
import java.io.File
import java.io.FileOutputStream

/**
 * Извлечение метаданных и обложки из аудиофайла.
 * Все операции блокирующие — вызывать только из корутины ввода-вывода.
 */
object AudioMetadataReader {

    /** Метаданные трека, прочитанные из файла */
    data class AudioMetadata(
        val title: String,
        val artist: String,
        val durationMs: Long,
        val artworkPath: String?
    )

    /**
     * Читает метаданные. Если тег отсутствует, используется запасное название.
     * Обложка сохраняется в filesDir/artwork и возвращается путём.
     */
    fun read(
        context: Context,
        file: File,
        fallbackTitle: String
    ): AudioMetadata {
        val retriever = MediaMetadataRetriever()

        return try {
            retriever.setDataSource(file.absolutePath)

            val title = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: fallbackTitle

            val artist = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.trim()
                .orEmpty()

            val duration = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?: 0L

            val artworkPath = extractArtwork(context, retriever)

            AudioMetadata(
                title = title.take(SharyConfig.MAX_TITLE_LENGTH),
                artist = artist.take(SharyConfig.MAX_TITLE_LENGTH),
                durationMs = duration.coerceAtLeast(0L),
                artworkPath = artworkPath
            )
        } catch (_: Exception) {
            AudioMetadata(
                title = fallbackTitle.take(SharyConfig.MAX_TITLE_LENGTH),
                artist = "",
                durationMs = 0L,
                artworkPath = null
            )
        } finally {
            runCatching { retriever.release() }
        }
    }

    /** Достаёт встроенную обложку и сохраняет уменьшенную копию */
    private fun extractArtwork(
        context: Context,
        retriever: MediaMetadataRetriever
    ): String? {
        val bytes = retriever.embeddedPicture ?: return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight)
        }

        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            ?: return null

        return try {
            saveArtwork(context, bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    /** Уменьшает обложку до квадрата и сохраняет в JPEG */
    private fun saveArtwork(context: Context, source: Bitmap): String? {
        val target = SharyConfig.ARTWORK_SIZE_PX
        val side = minOf(source.width, source.height)
        val offsetX = (source.width - side) / 2
        val offsetY = (source.height - side) / 2

        val square = Bitmap.createBitmap(source, offsetX, offsetY, side, side)

        val scaled = if (side > target) {
            Bitmap.createScaledBitmap(square, target, target, true).also {
                if (it !== square) square.recycle()
            }
        } else {
            square
        }

        val file = AppFiles.newArtworkFile(context)

        return try {
            FileOutputStream(file).use { output ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 88, output)
            }
            file.absolutePath
        } catch (_: Exception) {
            file.delete()
            null
        } finally {
            scaled.recycle()
        }
    }

    private fun calculateSampleSize(width: Int, height: Int): Int {
        val target = SharyConfig.ARTWORK_SIZE_PX
        var sample = 1

        while (width / sample > target * 2 || height / sample > target * 2) {
            sample *= 2
        }

        return sample
    }
}
