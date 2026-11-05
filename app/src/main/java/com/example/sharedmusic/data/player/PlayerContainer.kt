package com.example.sharedmusic.data.player

import android.content.Context

/**
 * Держит единственный экземпляр плеера на процесс.
 * Благодаря этому звук не обрывается при смене экрана,
 * а ViewModel остаётся пересоздаваемой без потери позиции.
 */
object PlayerContainer {

    @Volatile
    private var instance: MusicPlayer? = null

    fun player(context: Context): MusicPlayer {
        val existing = instance
        if (existing != null) return existing

        return synchronized(this) {
            val created = instance ?: MusicPlayer(context.applicationContext).also {
                instance = it
            }
            created
        }
    }
}
