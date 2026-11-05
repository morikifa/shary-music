package com.example.sharedmusic.data.local

import android.content.Context
import java.io.File

/**
 * Каталоги приложения.
 * Аудио и обложки лежат в filesDir, чтобы переживать перезапуск приложения,
 * а временные файлы загрузки — в cacheDir.
 */
object AppFiles {

    private const val LIBRARY_DIR = "library"
    private const val ARTWORK_DIR = "artwork"
    private const val AVATAR_DIR = "avatars"
    private const val TRANSFER_DIR = "transfers"

    fun libraryDir(context: Context): File =
        ensureDir(File(context.filesDir, LIBRARY_DIR))

    fun artworkDir(context: Context): File =
        ensureDir(File(context.filesDir, ARTWORK_DIR))

    fun avatarDir(context: Context): File =
        ensureDir(File(context.filesDir, AVATAR_DIR))

    fun transferDir(context: Context): File =
        ensureDir(File(context.cacheDir, TRANSFER_DIR))

    fun newAudioFile(context: Context, extension: String): File {
        val safeExtension = if (extension.isBlank()) ".audio" else ".$extension"
        return File(libraryDir(context), "track_${System.currentTimeMillis()}_${counter()}$safeExtension")
    }

    fun newArtworkFile(context: Context): File =
        File(artworkDir(context), "art_${System.currentTimeMillis()}_${counter()}.jpg")

    fun newAvatarFile(context: Context): File =
        File(avatarDir(context), "avatar_${System.currentTimeMillis()}_${counter()}.jpg")

    private var sequence = 0

    private fun counter(): Int {
        sequence += 1
        return sequence
    }

    private fun ensureDir(dir: File): File {
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
}
